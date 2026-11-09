package com.github.kotyabuchi.yurucra.monster

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta

/**
 * ゾンビ・スケルトンの仲間に、体の色に合わせた革の帽子をかぶせて出す。昼でも燃えずに襲ってくる。
 * 帽子は落とさない。元から頭に何か付けているものはそのまま。
 */
class HelmetMobs : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSpawn(event: EntitySpawnEvent) {
        val color = HELMET_COLORS[event.entityType] ?: return
        val equipment = (event.entity as? Mob)?.equipment ?: return
        if (!equipment.helmet.isEmpty) return
        equipment.setHelmet(ItemStack(Material.LEATHER_HELMET).apply {
            editMeta(LeatherArmorMeta::class.java) { it.setColor(color) }
        })
        equipment.helmetDropChance = 0f
    }

    companion object {
        private val ZOMBIE = Color.fromRGB(78, 123, 54)

        private val HELMET_COLORS = mapOf(
            EntityType.ZOMBIE to ZOMBIE,
            EntityType.ZOMBIE_VILLAGER to ZOMBIE,
            EntityType.DROWNED to Color.fromRGB(96, 152, 146),
            EntityType.SKELETON to Color.fromRGB(236, 240, 241),
            EntityType.STRAY to Color.fromRGB(106, 129, 131),
        )
    }
}
