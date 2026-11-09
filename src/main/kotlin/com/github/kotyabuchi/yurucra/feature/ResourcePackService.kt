package com.github.kotyabuchi.yurucra.feature

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.util.runNextTick
import com.github.kotyabuchi.yurucra.util.text
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * 参加したプレイヤーにリソースパックを送る（config.yml の resource-pack）。
 *
 * SHA-1 を設定に書かなければ、起動時にURLからパックをダウンロードして計算する。
 * GitHub Releases の「最新版」リンクを使えば、リリースを更新して /yc resourcepack reload するだけで配り直せる。
 * ハッシュが分かるまでの間に参加した人には、分かった時点で送る。
 */
class ResourcePackService(private val plugin: YuruCra) : Listener {

    private val http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    // 準備できたパックの情報（URLが空なら null のまま）
    private var request: ResourcePackRequest? = null

    /** 設定を読み直してハッシュを用意し、参加中の全員に送り直す。onReady は完了時（失敗時も）にメインスレッドで呼ぶ */
    fun reload(onReady: (Result<String>) -> Unit = {}) {
        plugin.reloadConfig()
        val config = plugin.config
        val url = config.getString("resource-pack.url").orEmpty().trim()
        request = null
        if (url.isEmpty()) {
            onReady(Result.failure(IllegalStateException("resource-pack.url が空なので配布しません")))
            return
        }
        val uri = runCatching { URI.create(url) }.getOrElse {
            onReady(Result.failure(IllegalArgumentException("resource-pack.url の形式が正しくありません: $url")))
            return
        }
        val configured = config.getString("resource-pack.sha1").orEmpty().trim().lowercase()
        val hashFuture = if (configured.isNotEmpty()) CompletableFuture.completedFuture(configured) else downloadSha1(uri)

        // ダウンロードの失敗（リリースがない・通信できない）は想定内なので、スタックトレースは出さず呼び出し側に返す
        hashFuture.whenComplete { hash, error ->
            if (!plugin.isEnabled) return@whenComplete
            plugin.runNextTick {
                if (error != null) onReady(Result.failure((error as? CompletionException)?.cause ?: error))
                else apply(uri, hash, onReady)
            }
        }
    }

    private fun apply(uri: URI, hash: String, onReady: (Result<String>) -> Unit) {
        val config = plugin.config
        request = ResourcePackRequest.resourcePackRequest()
            .packs(ResourcePackInfo.resourcePackInfo(PACK_ID, uri, hash))
            .required(config.getBoolean("resource-pack.required"))
            .prompt(text(config.getString("resource-pack.prompt").orEmpty()))
            // 他のプラグインのパックは残したまま、このパックだけ入れ替える
            .replace(false)
            .build()
        plugin.server.onlinePlayers.forEach { send(it) }
        plugin.logger.info("リソースパックを配布します: $uri (sha1=$hash)")
        onReady(Result.success(hash))
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) = send(event.player)

    private fun send(player: Player) {
        request?.let { player.sendResourcePacks(it) }
    }

    private fun downloadSha1(uri: URI): CompletableFuture<String> {
        val httpRequest = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "YuruCra-Plugin")
            .GET()
            .build()
        return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray()).thenApply { response ->
            check(response.statusCode() == 200) { "リソースパックをダウンロードできませんでした (HTTP ${response.statusCode()}): $uri" }
            MessageDigest.getInstance("SHA-1").digest(response.body()).joinToString("") { "%02x".format(it) }
        }
    }

    companion object {
        // パックの識別子。同じIDのままハッシュが変われば、クライアントは新しいパックに入れ替える
        private val PACK_ID: UUID = UUID.nameUUIDFromBytes("yurucra:pumpking_pack".toByteArray())
    }
}
