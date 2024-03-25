package com.github.kotyabuchi.YuruCra.Menu

import com.github.kotyabuchi.YuruCra.Menu.Button.MBBlank
import net.kyori.adventure.text.Component
import org.bukkit.Material

class TestMenu(playerName: String, frames: Set<FrameType>): Menu(
    title = Component.text("Test for $playerName"),
    menuRow = 6,
    frames = frames
    ) {

    override fun createFooter(pageNum: Int) {
        super.createFooter(pageNum)
        setButton(pageNum, invSize - 5, MBBlank(Material.ENDER_EYE))
    }
}