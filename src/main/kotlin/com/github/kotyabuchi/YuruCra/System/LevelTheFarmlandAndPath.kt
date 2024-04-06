package com.github.kotyabuchi.YuruCra.System

import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Utility.isShovel
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.scheduler.BukkitRunnable

object LevelTheFarmlandAndPath: Listener {

    private val main: Main = Main.instance

    @EventHandler
    fun onClick(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        val block = event.clickedBlock ?: return
        val type = block.type

        if (!player.isSneaking) return
        if (!item.type.isShovel()) return
        if (type != Material.FARMLAND && type != Material.DIRT_PATH) return
        object : BukkitRunnable() {
            override fun run() {
                if (event.hand == EquipmentSlot.HAND) {
                    player.swingMainHand()
                } else {
                    player.swingOffHand()
                }
                block.type = Material.DIRT
                block.world.playSound(block.location.toCenterLocation(), Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1f, 1f)
            }
        }.runTaskLater(main, 1)
    }
}