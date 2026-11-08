package com.github.kotyabuchi.realisticsurvival.menu

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.giveOrDrop
import com.github.kotyabuchi.realisticsurvival.util.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * ホットバーの一番右（スロット8）に固定される「Menu」コンパス。
 * 右クリック、またはインベントリでクリックするとメインメニューを開く。捨てられず、死んでも残る。
 */
class MenuCompass(private val plugin: RealisticSurvival) : Listener {

    private val key = NamespacedKey(plugin, "menu_compass")

    private fun create(): ItemStack = ItemStack(Material.COMPASS).apply {
        editMeta {
            it.displayName(text("Menu", NamedTextColor.GOLD))
            it.persistentDataContainer.set(key, PersistentDataType.BYTE, 1)
        }
    }

    // インベントリのクリックのたびに呼ばれるので、素材の比較（軽い）を先にしてからPDCを読む
    private fun isCompass(item: ItemStack?): Boolean =
        item != null && item.type == Material.COMPASS && item.persistentDataContainer.has(key, PersistentDataType.BYTE)

    private fun openMainMenu(player: Player) = plugin.menus.open(player, MainMenu(plugin))

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val inventory = event.player.inventory
        // 他の場所にあるコンパスは消して、スロット8に置き直す
        inventory.storageContents.forEachIndexed { slot, item -> if (isCompass(item)) inventory.setItem(slot, null) }
        inventory.getItem(SLOT)?.takeUnless { it.isEmpty }?.let { event.player.giveOrDrop(it) }
        inventory.setItem(SLOT, create())
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val compasses = event.drops.filter { isCompass(it) }
        event.drops.removeAll(compasses)
        event.itemsToKeep.addAll(compasses)
    }

    @EventHandler(ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        if (isCompass(event.itemDrop.itemStack)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onSwapHand(event: PlayerSwapHandItemsEvent) {
        if (isCompass(event.offHandItem)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onUse(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND || !event.action.isRightClick) return
        if (!isCompass(event.item)) return
        event.isCancelled = true
        openMainMenu(event.player)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val swapsCompass = event.click == ClickType.NUMBER_KEY && isCompass(player.inventory.getItem(event.hotbarButton))
        if (!isCompass(event.currentItem) && !swapsCompass) return
        event.isCancelled = true
        // 持ち上げていたアイテムは手元に戻す
        val cursor = event.view.cursor
        if (!cursor.isEmpty) {
            event.view.setCursor(null)
            player.giveOrDrop(cursor)
        }
        openMainMenu(player)
    }

    companion object {
        private const val SLOT = 8
    }
}
