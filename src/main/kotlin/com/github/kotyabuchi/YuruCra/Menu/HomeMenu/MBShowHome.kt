package com.github.kotyabuchi.YuruCra.Menu.HomeMenu

import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.event.Listener

object MBShowHome: MenuButton(), Listener {
    override val material: Material = Material.ENDER_EYE
    override val displayName: TextComponent = Component.text("Home")

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val player = info.player.getStatus()
        player.openMenu(HomeMenu(player))
    }
}