package com.github.kotyabuchi.YuruCra.Menu

import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.PlayerInventory

object MenuController: Listener {

    @EventHandler
    fun onButtonClick(event: InventoryClickEvent) {
        val player = (event.whoClicked as? Player)?.getStatus() ?: return
        val menuStatus = player.menuStatus
        val menu = menuStatus.openingMenu ?: return
        val menuInventory = menu.getInventory(menuStatus.openingPage)

        event.isCancelled = true

        val clickInventory = event.clickedInventory
        if (clickInventory is PlayerInventory) {
            menu.doItemClickAction(event)
        } else if (clickInventory == menuInventory) {
            menu.getButton(menuStatus.openingPage, event.slot)?.let { button ->
                button.clickSound?.let { clickSound ->
                    player.playSound(player.eyeLocation, clickSound.sound, clickSound.volume, clickSound.pitch)
                }
                val info = ButtonClickInfo(player, button, menu, event.cursor)
                if (event.isShiftClick) {
                    button.doShiftClickAction(info)
                } else if (event.isRightClick) {
                    button.doRightClickAction(info)
                } else if (event.isLeftClick) {
                    button.doLeftClickAction(info)
                }
            }
        }
    }

    @EventHandler
    fun onCloseMenu(event: InventoryCloseEvent) {
        val player = (event.player as? Player)?.getStatus() ?: return
        val menuStatus = player.menuStatus
        menuStatus.openingMenu?.doCloseMenuAction(event)
        if (event.reason != InventoryCloseEvent.Reason.OPEN_NEW) menuStatus.closeMenu()
    }
}