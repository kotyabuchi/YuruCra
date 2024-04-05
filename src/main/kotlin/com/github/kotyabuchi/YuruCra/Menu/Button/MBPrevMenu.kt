package com.github.kotyabuchi.YuruCra.Menu.Button

import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Menu.Menu
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material

class MBPrevMenu(prev: Menu): MenuButton() {
    override val material: Material = Material.ARROW
    override val displayName: TextComponent = Component.text("Back to ").append(prev.title)

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val player = info.player.getStatus()
        info.menu.prevMenu?.let {
            player.openMenu(it)
        }
    }
}