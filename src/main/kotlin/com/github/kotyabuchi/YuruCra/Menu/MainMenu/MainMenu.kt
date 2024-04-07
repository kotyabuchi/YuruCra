package com.github.kotyabuchi.YuruCra.Menu.MainMenu

import com.github.kotyabuchi.YuruCra.Menu.FrameType
import com.github.kotyabuchi.YuruCra.Menu.HomeMenu.MBShowHome
import com.github.kotyabuchi.YuruCra.Menu.Menu
import net.kyori.adventure.text.Component

object MainMenu: Menu(
    title = Component.text("Menu"),
    frames = setOf(FrameType.TOP, FrameType.SIDE)
) {
    init {
        createMenu()
    }

    override fun createMenu() {
        addButton(MBShowHome)
    }
}