package com.github.kotyabuchi.YuruCra.Menu.ResourceStorageMenu

import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Menu.Menu
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import com.github.kotyabuchi.YuruCra.Utility.addItemOrDrop
import com.github.kotyabuchi.YuruCra.Utility.floorToInt
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player

class MBStoredResource(resource: Material, totalAmount: Int): MenuButton() {
    private val maxStackSize = resource.maxStackSize
    private val unit = (totalAmount / maxStackSize.toDouble()).floorToInt()
    override val material: Material = resource
    override val lore: List<TextComponent> = mutableListOf<TextComponent>().apply {
        add(Component.text("${unit}stacks + ${totalAmount % maxStackSize}items", NamedTextColor.WHITE))
        add(Component.text("TotalAmount: ", NamedTextColor.GRAY).append(Component.text(totalAmount, NamedTextColor.WHITE)))
        add(Component.empty())
        add(Component.text("Restore", NamedTextColor.WHITE))
        add(Component.text("Left Click: ", NamedTextColor.GRAY).append(Component.text("1 item", NamedTextColor.WHITE)))
        add(Component.text("Right Click: ", NamedTextColor.GRAY).append(Component.text("5 items", NamedTextColor.WHITE)))
        add(Component.text("Shift Click: ", NamedTextColor.GRAY).append(Component.text("1 stack", NamedTextColor.WHITE)))
    }

    override fun doLeftClickAction(info: ButtonClickInfo) {
        restoreResource(info.player, info.menu, info.button.material, 1)
    }

    override fun doRightClickAction(info: ButtonClickInfo) {
        restoreResource(info.player, info.menu, info.button.material, 5)
    }

    override fun doShiftClickAction(info: ButtonClickInfo) {
        restoreResource(info.player, info.menu, info.button.material, maxStackSize)
    }

    private fun restoreResource(player: Player, menu: Menu, resource: Material, amount: Int) {
        val restoredItem = player.getStatus().resourceStorage.restoreResource(resource, amount) ?: return
        player.inventory.addItemOrDrop(player, restoredItem)
        menu.refresh()
        player.getStatus().openMenu(menu)
    }
}