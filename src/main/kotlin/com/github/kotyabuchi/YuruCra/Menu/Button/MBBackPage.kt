package com.github.kotyabuchi.YuruCra.Menu.Button

import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import net.kyori.adventure.text.Component
import org.bukkit.Material

class MBBackPage(page: Int, totalPage: Int): MenuButton() {
    override val material: Material = Material.ARROW
    override val displayName: Component = Component.text("Back page $page / $totalPage")

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val player = info.player
        player.openMenu(info.menu, player.menuStatus.openingPage - 1)
    }
}