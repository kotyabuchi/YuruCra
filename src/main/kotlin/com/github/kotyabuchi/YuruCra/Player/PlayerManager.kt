package com.github.kotyabuchi.YuruCra.Player

import com.github.kotyabuchi.MCRPG.append
import com.github.kotyabuchi.MCRPG.transactionWithLogger
import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

object PlayerManager: Listener {

    private val main: Main = Main.instance

    init {
        transactionWithLogger {
            SchemaUtils.create(TPlayerStatus)
            SchemaUtils.create(TPlayerHomes)

            main.server.onlinePlayers.forEach {
                loadPlayerData(it.getStatus())
            }
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player.getStatus()

        transactionWithLogger {
            loadPlayerData(player)

            var msg = Component.text("はろわ！", NamedTextColor.GREEN)
            val lastLogin = player.lastLogin
            val dtf = if (lastLogin.year == LocalDateTime.now().year) {
                DateTimeFormatter.ofPattern("MM/dd HH:mm:ss")
            } else {
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            }
            val playTimeSec = player.playTime / 1000
            val playTimeMin = playTimeSec / 60
            val playTimeHour = playTimeMin / 60
            var playTimeMessage = ""
            if (playTimeHour > 0) playTimeMessage += "${playTimeHour % 24}時間"
            if (playTimeMin > 0) playTimeMessage += "${playTimeMin % 60}分"
            if (playTimeSec > 0) playTimeMessage += "${playTimeSec % 60}秒"
            msg = msg
                .append("\n最終ログイン: ", NamedTextColor.WHITE)
                .append("${lastLogin.format(dtf)}\n", NamedTextColor.GREEN)
                .append("総プレイ時間: ", NamedTextColor.WHITE)
                .append(playTimeMessage, NamedTextColor.GOLD)

            event.joinMessage(msg)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player.getStatus()
        transactionWithLogger {
            savePlayerData(player)
            PlayerStatus.removeCache(player)
        }
    }

    private fun loadPlayerData(player: PlayerStatus) {
        val uuidStr = player.uniqueId.toString()
        TPlayerStatus.select { TPlayerStatus.uuid eq uuidStr }.singleOrNull()?.let {
            player.playTime = it[TPlayerStatus.playTime]
            player.lastLogin = it[TPlayerStatus.lastLoginDate]
            player.lastPlayVersion = it[TPlayerStatus.lastPlayVersion]
        }
        TPlayerHomes.selectAll().andWhere { TPlayerHomes.uuid eq uuidStr }.forEach {
            main.server.getWorld(UUID.fromString(it[TPlayerHomes.world]))?.let { world ->
                player.homes.add(
                    HomeInfo(
                        id = it[TPlayerHomes.id].value,
                        name = it[TPlayerHomes.name],
                        icon = Material.valueOf(it[TPlayerHomes.icon]),
                        location = Location(
                            world,
                            it[TPlayerHomes.x],
                            it[TPlayerHomes.y],
                            it[TPlayerHomes.z],
                            it[TPlayerHomes.yaw],
                            0f
                        )
                    )
                )
            }
        }
    }

    fun savePlayerData(player: PlayerStatus) {
        val uuidStr = player.uniqueId.toString()
        val currentPlayTime = System.currentTimeMillis() - (player.loginTime.toEpochSecond(ZoneOffset.ofHours(9)) * 1000)
        val existsData = TPlayerStatus.select { TPlayerStatus.uuid eq uuidStr }.singleOrNull()
        if (existsData == null) {
            TPlayerStatus.insert {
                it[uuid] = uuidStr
                it[playTime] = currentPlayTime
                it[lastLoginDate] = player.loginTime
                it[lastPlayVersion] = main.server.version.toDoubleOrNull() ?: 0.0
            }
        } else {
            TPlayerStatus.update({ TPlayerStatus.uuid eq uuidStr }) {
                it[playTime] = player.playTime + currentPlayTime
                it[lastLoginDate] = player.loginTime
                it[lastPlayVersion] = main.server.version.toDoubleOrNull() ?: 0.0
            }
        }
    }
}