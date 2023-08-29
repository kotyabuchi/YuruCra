package com.github.kotyabuchi.YuruCra.Menu.Button

import net.kyori.adventure.text.Component
import org.bukkit.Material

class MBBlank(override val material: Material = Material.GRAY_STAINED_GLASS_PANE): MenuButton() {
    override val displayName: Component = Component.text("")
}