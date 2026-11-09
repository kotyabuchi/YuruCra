package com.github.kotyabuchi.yurucra.player

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.data.HomeRepository
import com.github.kotyabuchi.yurucra.util.sendError
import com.github.kotyabuchi.yurucra.util.sendSuccess
import com.github.kotyabuchi.yurucra.util.thenOnMain
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * ホームの作成・変更・削除。DB書き込みは非同期で行い、完了後に onDone をメインスレッドで呼ぶ。
 * 公開ホームは全プレイヤー共通なので、ここで1つのインスタンスとして持つ。
 */
class HomeService(private val plugin: YuruCra) {

    // ログイン前スレッドからも読むので ConcurrentHashMap
    private val publicHomes = ConcurrentHashMap<Int, Home>()

    fun loadPublicHomes() {
        val homes = plugin.database.submit { HomeRepository.loadPublic(it) }.join()
        homes.forEach { home -> home.id?.let { publicHomes[it] = home } }
    }

    fun publicHomes(): List<Home> = publicHomes.values.sortedBy { it.id }

    /** 公開ホームと同じIDなら、そのインスタンスを返す（名前変更などが両方に反映されるように） */
    fun canonical(home: Home): Home = home.id?.let { publicHomes[it] } ?: home

    fun canEdit(player: Player, home: Home): Boolean = player.isOp || home.creator == player.uniqueId

    fun create(player: Player, name: String, location: Location, onDone: () -> Unit = {}) {
        val home = Home(
            id = null,
            name = name,
            worldName = location.world.name,
            x = location.blockX + 0.5,
            y = location.y,
            z = location.blockZ + 0.5,
            yaw = location.yaw,
            icon = Material.ENDER_PEARL,
            creator = player.uniqueId,
        )
        plugin.database.submit { HomeRepository.insert(it, player.uniqueId, home) }
            .thenOnMain(plugin, onError = { player.sendError("ホームの作成に失敗しました。") }) { id ->
                val saved = Home(id, home.name, home.worldName, home.x, home.y, home.z, home.yaw, home.icon, home.creator)
                if (player.isOnline) plugin.players.status(player).homes += saved
                player.sendSuccess("ホームポイント[${name}]を作成しました。")
                onDone()
            }
    }

    fun rename(player: Player, home: Home, newName: String, onDone: () -> Unit = {}) {
        val id = home.id ?: return
        plugin.database.submit { HomeRepository.updateName(it, id, newName) }
            .thenOnMain(plugin, onError = { player.sendError("名前の変更に失敗しました。") }) {
                home.name = newName
                player.sendSuccess("名前を変更しました。")
                onDone()
            }
    }

    fun setPublic(player: Player, home: Home, isPublic: Boolean, onDone: () -> Unit = {}) {
        val id = home.id ?: return
        if (!canEdit(player, home)) {
            player.sendError("作成者のみ公開設定を変更できます。")
            return
        }
        plugin.database.submit { HomeRepository.updatePublic(it, id, isPublic) }
            .thenOnMain(plugin, onError = { player.sendError("公開設定の変更に失敗しました。") }) {
                home.isPublic = isPublic
                if (isPublic) publicHomes[id] = home else publicHomes.remove(id)
                player.sendSuccess("[${home.name}]を" + (if (isPublic) "公開" else "非公開") + "に変更しました。")
                onDone()
            }
    }

    fun changeIcon(player: Player, home: Home, icon: Material, onDone: () -> Unit = {}) {
        val id = home.id ?: return
        plugin.database.submit { HomeRepository.updateIcon(it, id, icon) }
            .thenOnMain(plugin, onError = { player.sendError("アイコンの変更に失敗しました。") }) {
                home.icon = icon
                onDone()
            }
    }

    fun remove(player: Player, home: Home, onDone: () -> Unit = {}) {
        val id = home.id ?: return
        if (!canEdit(player, home)) return
        plugin.database.submit { HomeRepository.delete(it, id) }
            .thenOnMain(plugin, onError = { player.sendError("ホームの削除に失敗しました。") }) {
                publicHomes.remove(id)
                plugin.server.onlinePlayers.forEach { online ->
                    plugin.players.status(online).homes.removeIf { it.id == id }
                }
                player.sendSuccess("[${home.name}]を削除しました。")
                onDone()
            }
    }
}
