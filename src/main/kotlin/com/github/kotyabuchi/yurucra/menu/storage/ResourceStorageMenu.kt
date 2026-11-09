package com.github.kotyabuchi.yurucra.menu.storage

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.menu.BlankButton
import com.github.kotyabuchi.yurucra.menu.Menu
import com.github.kotyabuchi.yurucra.menu.MenuButton
import com.github.kotyabuchi.yurucra.player.ResourceStorage
import com.github.kotyabuchi.yurucra.util.ItemModel
import com.github.kotyabuchi.yurucra.util.buttonItem
import com.github.kotyabuchi.yurucra.util.withModel
import com.github.kotyabuchi.yurucra.util.giveOrDrop
import com.github.kotyabuchi.yurucra.util.loreLine
import com.github.kotyabuchi.yurucra.util.sendError
import com.github.kotyabuchi.yurucra.util.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import kotlin.math.min

/**
 * リソースストレージ。
 * - 自分のインベントリ側をクリック: 左=1スタック、右=5個しまう
 * - 倉庫のアイテムをクリック: 左=1個、右=5個、シフト=1スタック取り出す
 * - 鍵のスロットは経験値レベルを払って解放する
 */
class ResourceStorageMenu(plugin: YuruCra, private val player: Player) : Menu(
    plugin,
    text("Resource Storage"),
    plugin.players.status(player).resourceStorage.slotSize,
    Frame.TOP,
) {
    private val storage: ResourceStorage get() = plugin.players.status(player).resourceStorage

    override fun build() {
        storage.stored.forEach { (material, amount) -> put(StoredResourceButton(material, amount)) }
        if (storage.slotSize < ResourceStorage.MAX_SLOTS) put(UnlockSlotButton(storage), 0, size - 5)
        // 未解放のスロットは鉄格子でふさぐ
        put(BlankButton(Material.IRON_BARS), 0, (9 + storage.slotSize) until (size - 9))
    }

    override fun onPlayerInventoryClick(player: Player, event: InventoryClickEvent, page: Int) {
        val item = event.currentItem ?: return
        if (item.isEmpty || item.maxStackSize == 1) return
        val amount = when {
            event.isLeftClick -> min(item.maxStackSize, item.amount)
            event.isRightClick -> min(5, item.amount)
            else -> return
        }
        val stored = storage.store(item.asQuantity(amount))
        if (stored == 0) return
        event.currentItem = if (item.amount - stored <= 0) null else item.asQuantity(item.amount - stored)
        plugin.menus.reopen(player, this)
    }

    private class StoredResourceButton(val material: Material, private val total: Int) : MenuButton(
        buttonItem(
            material,
            name = null,
            lore = listOf(
                loreLine("Unit: ", "${total / material.maxStackSize}stacks + ${total % material.maxStackSize}items"),
                loreLine("Items: ", "$total"),
                Component.empty(),
                text("Restore"),
                loreLine("Left Click: ", "1 item"),
                loreLine("Right Click: ", "5 items"),
                loreLine("Shift Click: ", "1 stack"),
            ),
        )
    ) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) = restore(player, event, menu)
        override fun onRightClick(player: Player, event: InventoryClickEvent, menu: Menu) = restore(player, event, menu)

        private fun restore(player: Player, event: InventoryClickEvent, menu: Menu) {
            val amount = when {
                event.isShiftClick -> min(material.maxStackSize, total)
                event.isRightClick -> min(5, total)
                else -> 1
            }
            val storage = menu.plugin.players.status(player).resourceStorage
            val restored = storage.take(material, amount) ?: return
            player.giveOrDrop(restored)
            menu.plugin.menus.reopen(player, menu)
        }
    }

    private class UnlockSlotButton(storage: ResourceStorage) : MenuButton(
        buttonItem(Material.ENDER_EYE, text("Unlock Slot", NamedTextColor.GREEN), listOf(loreLine("Cost: ", "${cost(storage)} Lv")))
            .withModel(ItemModel.PLUS)
    ) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            val storage = menu.plugin.players.status(player).resourceStorage
            if (storage.slotSize >= ResourceStorage.MAX_SLOTS) return
            val cost = cost(storage)
            if (player.level < cost) {
                player.sendError("レベルが足りません。")
                return
            }
            player.level -= cost
            storage.slotSize++
            // スロット数でメニューの大きさが変わるので作り直す
            val newMenu = ResourceStorageMenu(menu.plugin, player)
            newMenu.setPrevMenu(menu.prevMenu)
            menu.plugin.menus.open(player, newMenu, asBack = true)
        }

        companion object {
            fun cost(storage: ResourceStorage): Int = (storage.slotSize * 1.5 + 5).toInt()
        }
    }
}
