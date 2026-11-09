package com.github.kotyabuchi.yurucra.item

import com.github.kotyabuchi.yurucra.util.runNextTick
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * 1つずつしか持てないアイテム（道具・防具など）に、1つずつ違うUUIDを付けて見分けられるようにする。
 * 付けるのは、まだ付いていないものに一度だけ。地面に出たときとクラフトしたときに付ける。
 *
 * 一度付いたUUIDは変わらないが、コピーされたアイテム（クリエイティブでの複製など）は同じUUIDを持つ。
 */
class ItemIdentity(private val plugin: Plugin) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onSpawn(event: ItemSpawnEvent) {
        val entity = event.entity
        val item = entity.itemStack
        if (assign(item)) entity.itemStack = item
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCraft(event: CraftItemEvent) {
        if (event.recipe.result.maxStackSize > 1) return
        val player = event.whoClicked as? Player ?: return
        // シフトクリックでまとめて作ったものもあるので、クラフトが終わった次のtickに手持ちをまとめて確認する
        plugin.runNextTick {
            if (!player.isOnline) return@runNextTick
            val inventory = player.inventory
            for ((slot, item) in inventory.contents.withIndex()) {
                if (item != null && assign(item)) inventory.setItem(slot, item)
            }
            val cursor = player.itemOnCursor
            if (assign(cursor)) player.setItemOnCursor(cursor)
        }
    }

    /** UUIDを付けたら true（呼び出し側で元の場所に戻す） */
    private fun assign(item: ItemStack): Boolean {
        if (!isTarget(item) || item.persistentDataContainer.has(key)) return false
        val uuid = UUID.randomUUID()
        item.editPersistentDataContainer {
            it.set(key, PersistentDataType.LONG_ARRAY, longArrayOf(uuid.mostSignificantBits, uuid.leastSignificantBits))
        }
        return true
    }

    companion object {
        private val key = NamespacedKey("yurucra", "item_uuid")

        /** 付いていなければ null */
        fun uuidOf(item: ItemStack): UUID? {
            if (item.isEmpty) return null
            val bits = item.persistentDataContainer.get(key, PersistentDataType.LONG_ARRAY) ?: return null
            if (bits.size != 2) return null
            return UUID(bits[0], bits[1])
        }

        private fun isTarget(item: ItemStack): Boolean = !item.isEmpty && item.maxStackSize == 1
    }
}
