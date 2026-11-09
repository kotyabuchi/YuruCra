package com.github.kotyabuchi.yurucra.feature

import com.github.kotyabuchi.yurucra.util.sendError
import com.github.kotyabuchi.yurucra.util.sendSuccess
import org.bukkit.Sound
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import java.util.UUID

/**
 * インベントリの中身を種類順に並べ、同じアイテムを1つのスタックにまとめる。
 *
 * - 開いているインベントリの空きスロットをダブルクリック（カーソルが空のとき）するとソートする。
 *   プレイヤー自身のインベントリはホットバー以外を並べる。
 *   （旧版の「中クリック」は、サバイバルのクライアントがスロットの中クリックをサーバーへ送らないので使えない）
 * - `/yc sort` のあとにコンテナを右クリックすると、開かずにソートする。右クリック以外の操作で取り消し
 *
 * メニュー画面のクリックは MenuService が HIGH で取り消すので、ignoreCancelled で対象外になる。
 */
class ChestSort : Listener {

    private val waiting = mutableSetOf<UUID>()

    fun startSortMode(player: Player) {
        waiting += player.uniqueId
        player.sendPlainMessage("ソートしたいコンテナを右クリックしてください")
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (player.uniqueId !in waiting) return
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            waiting -= player.uniqueId
            player.sendError("ソートを取り消しました")
            player.playSound(player.eyeLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1.5f)
            return
        }
        // 保護プラグインなどで開けないコンテナは触らない
        if (event.useInteractedBlock() == Event.Result.DENY) return
        val block = event.clickedBlock ?: return
        // 大きいチェストは Chest#getInventory が両方を合わせたものを返す
        val container = block.getState(false) as? Container ?: return
        event.isCancelled = true
        waiting -= player.uniqueId
        sort(container.inventory)
        player.sendSuccess("ソートしました")
        player.playSound(block.location.toCenterLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onClick(event: InventoryClickEvent) {
        if (event.click != ClickType.DOUBLE_CLICK) return
        if (event.currentItem?.isEmpty == false || !event.cursor.isEmpty) return
        val player = event.whoClicked as? Player ?: return
        val inventory = event.clickedInventory ?: return
        if (inventory.type !in SORTABLE_TYPES) return

        event.isCancelled = true
        sort(inventory)
        player.playSound(player.eyeLocation, Sound.UI_BUTTON_CLICK, 1f, 1f)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        waiting -= event.player.uniqueId
    }

    private fun sort(inventory: Inventory) {
        val contents = inventory.storageContents
        // プレイヤーのインベントリは 0〜8 がホットバー。並べるのは 9 以降だけ
        val range = if (inventory is PlayerInventory) HOTBAR_SIZE until contents.size else contents.indices
        val sorted = sortItems(contents.slice(range))
        for ((offset, index) in range.withIndex()) {
            contents[index] = sorted.getOrNull(offset)
        }
        inventory.storageContents = contents
    }

    companion object {
        private const val HOTBAR_SIZE = 9

        private val SORTABLE_TYPES = setOf(
            InventoryType.PLAYER, InventoryType.CHEST, InventoryType.ENDER_CHEST, InventoryType.BARREL,
            InventoryType.SHULKER_BOX, InventoryType.DISPENSER, InventoryType.DROPPER,
        )

        /**
         * 同じアイテム（個数以外が同じもの）を合計してスタック上限ごとに分け直し、種類順・個数の多い順に並べる。
         * 種類が同じでもエンチャントなどが違うものは、元の並びで先にあった方が前になる。
         */
        fun sortItems(items: List<ItemStack?>): List<ItemStack> {
            val totals = LinkedHashMap<ItemStack, Int>()
            for (item in items) {
                if (item == null || item.isEmpty) continue
                totals.merge(item.asOne(), item.amount, Int::plus)
            }
            val stacks = ArrayList<ItemStack>(items.size)
            for ((one, total) in totals) {
                var remaining = total
                val max = one.maxStackSize
                while (remaining > 0) {
                    val amount = minOf(max, remaining)
                    stacks += one.asQuantity(amount)
                    remaining -= amount
                }
            }
            stacks.sortWith(compareBy<ItemStack> { it.type }.thenByDescending { it.amount })
            return stacks
        }
    }
}
