package com.github.kotyabuchi.YuruCra.Menu

import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ButtonClickInfo(
    val player: Player,
    val button: MenuButton,
    val menu: Menu,
    val cursorItem: ItemStack?
) {
}