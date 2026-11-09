package com.github.kotyabuchi.yurucra.item

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ExperienceOrb
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.PlayerInventory

/**
 * 修繕が付いた道具は、身に着けていなくてもインベントリにあれば経験値で直る。
 *
 * 経験値1で耐久値2を直す（バニラと同じ）。手に持った物 → オフハンド → 防具 → そのほかの順に直し、
 * 余った経験値はバニラどおりプレイヤーに入る。
 */
class GlobalMending(private val durability: ItemDurability) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPickupOrb(event: PlayerPickupExperienceEvent) {
        val orb = event.experienceOrb
        val original = orb.experience
        var experience = original
        if (experience <= 0) return
        val player = event.player
        val inventory = player.inventory
        for (slot in mendingOrder(inventory)) {
            val item = inventory.getItem(slot) ?: continue
            // 拾うたびに全スロットを見るので、軽い判定から行う
            if (item.isEmpty || item.type.maxDurability <= 0) continue
            val damage = item.getData(DataComponentTypes.DAMAGE) ?: 0
            if (damage <= 0 || !item.containsEnchantment(Enchantment.MENDING)) continue
            val repair = minOf(damage, experience * DURABILITY_PER_EXPERIENCE)
            item.setData(DataComponentTypes.DAMAGE, damage - repair)
            inventory.setItem(slot, durability.synced(item))
            experience -= (repair + DURABILITY_PER_EXPERIENCE - 1) / DURABILITY_PER_EXPERIENCE
            if (experience <= 0) break
        }
        experience = experience.coerceAtLeast(0)
        if (experience == original) return
        if (orb.count <= 1) {
            // 直すのに使った分だけ減らす。残りはバニラの処理（身に着けた物の修繕・経験値の獲得）に回る
            orb.experience = experience
            return
        }
        // 重なったオーブは全部が同じ量を持つので、量を減らすと残りのオーブまで減ってしまう。
        // 1つ分だけ数を減らして、余った経験値は別のオーブにして渡す
        orb.count -= 1
        event.isCancelled = true
        if (experience > 0) player.world.spawn(player.location, ExperienceOrb::class.java) { it.experience = experience }
    }

    private fun mendingOrder(inventory: PlayerInventory): Sequence<Int> {
        val held = inventory.heldItemSlot
        return sequenceOf(held, OFF_HAND_SLOT) + ARMOR_SLOTS.asSequence() + (0 until STORAGE_SIZE).asSequence().filter { it != held }
    }

    companion object {
        private const val DURABILITY_PER_EXPERIENCE = 2
        private const val STORAGE_SIZE = 36
        private val ARMOR_SLOTS = 36..39
        private const val OFF_HAND_SLOT = 40
    }
}
