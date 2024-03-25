package com.github.kotyabuchi.YuruCra.Menu.HomeMenu

import com.github.kotyabuchi.MCRPG.transactionWithLogger
import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Player.HomeInfo
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import com.github.kotyabuchi.YuruCra.Player.TPlayerHomes
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.exposed.sql.insert

object MBCreateHome: MenuButton(), Listener {
    override val material: Material = Material.ENDER_EYE
    override val displayName: TextComponent = Component.text("create home", NamedTextColor.GREEN)

    private val typingPlayers: MutableSet<PlayerStatus> = mutableSetOf()

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val player = info.player.getStatus()
        typingPlayers.add(info.player)
        player.closeMenu()
        player.sendMessage(
            Component.text("作成するホームポイントの名前を入力してください。", NamedTextColor.GREEN)
                .append(Component.text("\ncancel", NamedTextColor.RED))
                .append(Component.text("と入力することでキャンセル出来ます。", NamedTextColor.WHITE))
        )
    }

    @EventHandler
    fun onEnterChat(event: AsyncChatEvent) {
        val player = event.player.getStatus()
        if (!typingPlayers.contains(player)) return
        event.isCancelled = true
        val location = player.location
        val homeName = (event.message() as? TextComponent)?.content() ?: ""

        if (homeName.isEmpty() || homeName.equals("cancel", true)) {
            player.sendMessage(Component.text("ホームポイントの作成をキャンセルしました。", NamedTextColor.RED).decorate(TextDecoration.BOLD))
            return
        }

        typingPlayers.remove(player)

        transactionWithLogger {
            val results = TPlayerHomes.insert {
                it[uuid] = player.uniqueId.toString()
                it[name] = homeName
                it[icon] = Material.ENDER_EYE.name
                it[world] = location.world.uid.toString()
                it[x] = location.x
                it[y] = location.y
                it[z] = location.z
                it[yaw] = location.yaw
            }.resultedValues

            if (results == null) {
                player.sendMessage(Component.text("ホームポイントの追加に失敗しました。", NamedTextColor.RED).decorate(TextDecoration.BOLD))
            }
            results?.let {
                it.forEach { result ->
                    player.sendMessage(Component.text("ホームポイント[$homeName]を追加しました。", NamedTextColor.GREEN))
                    player.homes.add(
                        HomeInfo(
                            id = result[TPlayerHomes.id].value,
                            name = homeName,
                            icon = Material.ENDER_EYE,
                            location = location
                        )
                    )
                }
            }
        }
    }
}