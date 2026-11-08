package com.github.kotyabuchi.realisticsurvival.player

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.data.HomeRepository
import com.github.kotyabuchi.realisticsurvival.data.PlayerDataRepository
import com.github.kotyabuchi.realisticsurvival.util.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * プレイヤーデータの読み込み・保存。
 * ログイン前（非同期スレッド）に読み込んでおき、参加時に受け取る。
 */
class PlayerManager(private val plugin: RealisticSurvival) : Listener {

    // メインスレッドからだけ触る
    private val statuses = HashMap<UUID, PlayerStatus>()
    // ログイン前スレッドで読み込んだデータの受け渡し場所
    private val preloaded = ConcurrentHashMap<UUID, PlayerStatus>()

    /**
     * 参加中のプレイヤーのデータ。通常は参加時に読み込み済み。
     * 読み込み前に呼ばれた場合だけ、その場で（メインスレッドを止めて）読み込む。
     * ログアウト後に呼ばれた場合は、データが残り続けないようキャッシュしない。
     */
    fun status(player: Player): PlayerStatus {
        statuses[player.uniqueId]?.let { return it }
        plugin.logger.warning("${player.name} のデータが未読み込みのため、その場で読み込みます")
        val status = loadBlocking(player.uniqueId)
        if (player.isOnline) statuses[player.uniqueId] = status
        return status
    }

    /** プラグインの再読み込み時など、既にオンラインのプレイヤーを読み込む */
    fun loadOnlinePlayers() {
        plugin.server.onlinePlayers.forEach { player ->
            statuses[player.uniqueId] = loadBlocking(player.uniqueId)
            plugin.jobs.clearTimedSkills(player)
        }
    }

    fun saveAll(): CompletableFuture<Unit> {
        val snapshots = statuses.values.map { it.snapshot() }
        return plugin.database.submit { conn -> PlayerDataRepository.save(conn, snapshots) }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return
        try {
            preloaded[event.uniqueId] = load(event.uniqueId).get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "${event.name} のデータ読み込みに失敗しました", e)
            // 空のデータで参加させると保存時に上書きしてしまうので、参加を断る
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                text("プレイヤーデータの読み込みに失敗しました。しばらくしてから再接続してください。", NamedTextColor.RED),
            )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        statuses[player.uniqueId] = preloaded.remove(player.uniqueId) ?: loadBlocking(player.uniqueId)
        plugin.jobs.clearTimedSkills(player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        plugin.progress.clear(player)
        val status = statuses.remove(player.uniqueId) ?: return
        val snapshot = status.snapshot()
        plugin.database.submit { conn -> PlayerDataRepository.save(conn, listOf(snapshot)) }
            .exceptionally { e ->
                plugin.logger.log(Level.SEVERE, "${player.name} のデータ保存に失敗しました", e)
            }
    }

    private fun load(uuid: UUID): CompletableFuture<PlayerStatus> =
        plugin.database.submit { conn ->
            val data = PlayerDataRepository.load(conn, uuid)
            val homes = HomeRepository.loadOwnedBy(conn, uuid).map { plugin.homes.canonical(it) }
            PlayerStatus(
                uuid = uuid,
                jobTotalExp = data.jobTotalExp,
                resourceStorage = ResourceStorage(data.storageSlotSize ?: ResourceStorage.DEFAULT_SLOTS).apply {
                    data.storedResources.forEach { (material, amount) -> restore(material, amount) }
                },
                homes = homes,
            )
        }

    private fun loadBlocking(uuid: UUID): PlayerStatus = load(uuid).get(10, TimeUnit.SECONDS)
}
