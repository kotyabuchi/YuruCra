package com.github.kotyabuchi.yurucra.feature

import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent

/** 火による被害を防ぐ。ブロックは燃えず、火は燃え広がらず、落ちているアイテムは燃えない */
class WorldProtection : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onFireSpread(event: BlockSpreadEvent) {
        if (event.source.type == Material.FIRE || event.source.type == Material.SOUL_FIRE) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onItemCombust(event: EntityCombustEvent) {
        if (event.entity is Item) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onItemBurn(event: EntityDamageEvent) {
        if (event.entity !is Item) return
        if (event.cause == EntityDamageEvent.DamageCause.FIRE || event.cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            event.isCancelled = true
        }
    }
}
