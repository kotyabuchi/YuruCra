package com.github.kotyabuchi.realisticsurvival.feature

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack

/** 初めてサーバーに参加したプレイヤーに、木の道具とリンゴを渡す */
class StarterItems : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onFirstJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPlayedBefore()) return
        val inventory = player.inventory
        ITEMS.forEach { (slot, item) -> inventory.setItem(slot, item.clone()) }
    }

    companion object {
        private val ITEMS = mapOf(
            0 to ItemStack(Material.WOODEN_SWORD),
            1 to ItemStack(Material.WOODEN_PICKAXE),
            2 to ItemStack(Material.WOODEN_SHOVEL),
            3 to ItemStack(Material.WOODEN_AXE),
            7 to ItemStack(Material.APPLE, 8),
        )
    }
}
