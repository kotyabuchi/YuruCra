package com.github.kotyabuchi.YuruCra.Menu.Button

import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Menu.Menu
import net.kyori.adventure.text.Component
import org.bukkit.Material

class MBPrevMenu(prev: Menu): MenuButton() {
    override val material: Material = Material.ARROW
    override val displayName: Component = Component.text("Back to ").append(prev.title)

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val player = info.player
        info.menu.prevMenu?.let {
            player.openMenu(it)
        }
    }
}