package com.github.kotyabuchi.YuruCra.Menu

import net.kyori.adventure.text.Component

class TestMenu(playerName: String, frames: Set<FrameType>): Menu(
    title = Component.text("Test for $playerName"),
    menuRow = 6,
    frames = frames
    ) {
}