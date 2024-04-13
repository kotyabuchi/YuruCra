package com.github.kotyabuchi.YuruCra.System.ResourceStorage

import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAttemptPickupItemEvent

object ResourceStorageManager: Listener {

    @EventHandler
    fun onPickup(event: PlayerAttemptPickupItemEvent) {
        if (event.isCancelled) return
        val player = event.player
        val resourceStorage = player.getStatus().resourceStorage
        val item = event.item
        val itemStack = item.itemStack
        if (!resourceStorage.autoStore || !resourceStorage.existsMaterial(itemStack)) return

        val storedAmount = resourceStorage.storeResource(itemStack)
        itemStack.amount -= storedAmount
        item.itemStack = itemStack
        if (itemStack.amount == 0) {
            player.world.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, .5f, 1.5f)
            event.isCancelled = true
        }
    }
}