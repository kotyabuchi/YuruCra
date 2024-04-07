package com.github.kotyabuchi.YuruCra.Menu.MasteringMenu

import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.event.Listener

object MBShowMastering: MenuButton(), Listener {
    override val material: Material = Material.DIAMOND_PICKAXE
    override val displayName: TextComponent = Component.text("Mastering")

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val player = info.player.getStatus()
        player.openMenu(MasteringMenu(player))
    }
}