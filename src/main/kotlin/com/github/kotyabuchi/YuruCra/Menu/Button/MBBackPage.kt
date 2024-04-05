package com.github.kotyabuchi.YuruCra.Menu.Button

import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material

class MBBackPage(page: Int, totalPage: Int): MenuButton() {
    override val material: Material = Material.ARROW
    override val displayName: TextComponent = Component.text("Back page $page / $totalPage")

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val player = info.player.getStatus()
        player.openMenu(info.menu, player.menuStatus.openingPage - 1)
    }
}