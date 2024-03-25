package com.github.kotyabuchi.YuruCra.Menu.HomeMenu

import com.github.kotyabuchi.YuruCra.Menu.FrameType
import com.github.kotyabuchi.YuruCra.Menu.Menu
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus
import net.kyori.adventure.text.Component
import kotlin.math.ceil

class HomeMenu(private val player: PlayerStatus): Menu(
    title = Component.text("${player.name}'s Home"),
    menuRow = ceil(player.homes.size / 7.0).toInt(),
    frames = setOf(FrameType.TOP, FrameType.SIDE)
) {

    init {
        createMenu()
    }

    override fun createFooter(pageNum: Int) {
        super.createFooter(pageNum)
        setButton(pageNum, invSize - 5, MBCreateHome)
    }

    override fun createMenu() {
        var page = 0
        player.homes.forEach { home ->
            while (!addButton(page, MBHome(home))) {
                page++
            }
        }
    }
}