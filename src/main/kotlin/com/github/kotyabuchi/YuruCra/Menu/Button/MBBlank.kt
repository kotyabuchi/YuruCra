package com.github.kotyabuchi.YuruCra.Menu.Button

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material

class MBBlank(override val material: Material = Material.GRAY_STAINED_GLASS_PANE): MenuButton() {
    override val displayName: TextComponent = Component.text("")
}