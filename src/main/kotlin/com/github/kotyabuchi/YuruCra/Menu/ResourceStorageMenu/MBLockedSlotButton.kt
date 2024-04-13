package com.github.kotyabuchi.YuruCra.Menu.ResourceStorageMenu

import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material

object MBLockedSlotButton: MenuButton() {
    override val material: Material = Material.IRON_BARS
    override val displayName: TextComponent = Component.text("Locked")
}