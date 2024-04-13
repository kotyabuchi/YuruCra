package com.github.kotyabuchi.YuruCra.Menu.ResourceStorageMenu

import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material

object MBEmptySlot: MenuButton() {
    override val material: Material = Material.LIGHT_GRAY_STAINED_GLASS_PANE
    override val displayName: TextComponent = Component.text("Empty Slot")
}