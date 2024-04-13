package com.github.kotyabuchi.YuruCra.Menu.ResourceStorageMenu

import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import com.github.kotyabuchi.YuruCra.System.ResourceStorage.ResourceStorage
import com.github.kotyabuchi.YuruCra.Utility.floorToInt
import com.github.kotyabuchi.YuruCra.Utility.sendErrorMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class MBUnlockSlot(private val menu: ResourceStorageMenu, private val resourceStorage: ResourceStorage): MenuButton() {
    private val cost = (resourceStorage.slotSize * 1.5 + 5).floorToInt()
    override val material: Material = Material.ENDER_EYE
    override val displayName: TextComponent = Component.text("Unlock Slot")
    override val lore: List<TextComponent> = mutableListOf<TextComponent>().apply {
        add(Component.text("Cost: ", NamedTextColor.GRAY).append(Component.text(cost, NamedTextColor.WHITE)))
    }

    override fun doLeftClickAction(info: ButtonClickInfo) {
        if (resourceStorage.slotSize >= resourceStorage.maxSlotSize) return
        val player = info.player
        val status = player.getStatus()
        if (player.level < cost) {
            player.sendErrorMessage("レベルが足りません。")
            return
        }
        player.level -= cost
        resourceStorage.slotSize++
        val newMenu = ResourceStorageMenu(resourceStorage)
        newMenu.prevMenu = menu.prevMenu
        status.openMenu(newMenu, status.menuStatus.openingPage, forceOpen = true)
//        menu.refresh()
//        player.getStatus().openMenu(menu)
    }
}