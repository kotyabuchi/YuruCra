package com.github.kotyabuchi.YuruCra.Menu.ResourceStorageMenu

import com.github.kotyabuchi.YuruCra.Menu.FrameType
import com.github.kotyabuchi.YuruCra.Menu.Menu
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import com.github.kotyabuchi.YuruCra.System.ResourceStorage.ResourceStorage
import com.github.kotyabuchi.YuruCra.Utility.ceilToInt
import com.github.kotyabuchi.YuruCra.Utility.findAll
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.PlayerInventory
import kotlin.math.min

class ResourceStorageMenu(private val resourceStorage: ResourceStorage): Menu(
    title = Component.text("Resource Storage"),
    menuRow = ((resourceStorage.slotSize + 1) / 9.0).ceilToInt(),
    frames = setOf(FrameType.TOP)
) {

    init {
        createMenu()
    }

    override fun createMenu() {
        resourceStorage.getStoredResources().forEach { (itemStack, amount) ->
            addButton(MBStoredResource(itemStack, amount))
        }
        repeat(resourceStorage.slotSize - resourceStorage.getStoredResources().size) {
            addButton(MBEmptySlot)
        }
        if (resourceStorage.maxSlotSize > resourceStorage.slotSize) addButton(MBUnlockSlot(this, resourceStorage))
        fillButton(getLastPageNum(), MBLockedSlotButton)
    }

    override fun doItemClickAction(event: InventoryClickEvent) {
        val clickedInventory = event.clickedInventory as? PlayerInventory ?: return
        val player = event.whoClicked as? Player ?: return
        val currentItem = event.currentItem ?: return
        var itemStacks = listOf(currentItem)
        if (currentItem.maxStackSize == 1) return

        val amount = if (event.isShiftClick) {
            itemStacks = clickedInventory.findAll(currentItem).map { it.itemStack }
            itemStacks.sumOf { it.amount }
        } else if (event.isLeftClick) {
            min(currentItem.maxStackSize, currentItem.amount)
        } else if (event.isRightClick) {
            min(5, currentItem.amount)
        } else {
            return
        }
        var storedAmount = resourceStorage.storeResource(currentItem.asQuantity(amount))
        itemStacks.forEach { itemStack ->
            val storeAmount = min(itemStack.amount, storedAmount)
            itemStack.amount -= storeAmount
            storedAmount -= storeAmount
        }
        refresh()
        player.getStatus().openMenu(this)
    }
}