package com.github.kotyabuchi.realisticsurvival.menu

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.runNextTick
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/**
 * メニューの開閉とクリック処理。
 * クリックイベントの中で直接インベントリを開き直すと不具合が出ることがあるので、開閉は次のtickで行う。
 */
class MenuService(private val plugin: RealisticSurvival) : Listener {

    fun currentPage(player: Player): MenuPage? = player.openInventory.topInventory.getHolder(false) as? MenuPage

    /**
     * @param asBack 「戻る」やページ送り・再表示のときは true（戻り先を上書きしない）
     */
    fun open(player: Player, menu: Menu, page: Int = 0, asBack: Boolean = false) {
        val current = currentPage(player)?.menu
        if (!asBack && current != null && current !== menu) menu.setPrevMenu(current)
        plugin.runNextTick {
            if (player.isOnline) player.openInventory(menu.inventory(page))
        }
    }

    /** 今のページを作り直して開き直す */
    fun reopen(player: Player, menu: Menu) {
        val page = currentPage(player)?.takeIf { it.menu === menu }?.index ?: 0
        menu.refresh()
        open(player, menu, page, asBack = true)
    }

    fun close(player: Player, then: () -> Unit = {}) {
        plugin.runNextTick {
            player.closeInventory()
            then()
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onClick(event: InventoryClickEvent) {
        val top = event.view.topInventory
        val page = top.getHolder(false) as? MenuPage ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        val clicked = event.clickedInventory ?: return
        val menu = page.menu
        if (clicked == top) {
            menu.buttonAt(event.rawSlot, page.index)?.let { menu.onButtonClick(player, it, event, page.index) }
        } else {
            menu.onPlayerInventoryClick(player, event, page.index)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.getHolder(false) is MenuPage) event.isCancelled = true
    }
}
