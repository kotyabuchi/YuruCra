package com.github.kotyabuchi.YuruCra.Menu

import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Player.PlayerWrapper
import org.bukkit.inventory.ItemStack

class ButtonClickInfo(
    val player: PlayerWrapper,
    val button: MenuButton,
    val menu: Menu,
    val cursorItem: ItemStack?
) {
}