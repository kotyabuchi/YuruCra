package com.github.kotyabuchi.YuruCra.System

import com.github.kotyabuchi.YuruCra.Event.BlockMineEvent
import com.github.kotyabuchi.YuruCra.Utility.isHoe
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent

object KeepCropAndReplant: Listener {

    private val harvestBlock = mutableSetOf<Block>()
    private val cropsSeed = mapOf(
        Material.WHEAT to Material.WHEAT_SEEDS,
        Material.CARROTS to Material.CARROT,
        Material.POTATOES to Material.POTATO,
        Material.BEETROOTS to Material.BEETROOT_SEEDS,
        Material.COCOA to Material.COCOA_BEANS
    )

    @EventHandler
    fun onMine(event: BlockMineEvent) {
        val blockData = event.block.blockData
        val item = event.player.inventory.itemInMainHand
        if (item.type.isHoe() && blockData is Ageable) {
            if (blockData.age != blockData.maximumAge) {
                event.isCancelled = true
            } else {
                harvestBlock.add(event.block)
            }
        }
    }

    @EventHandler
    fun onDrop(event: BlockDropItemEvent) {
        val block = event.block
        if (!harvestBlock.contains(block)) return
        harvestBlock.remove(block)
        val seedItem = cropsSeed[block.type]

        if (seedItem == null) {
            block.type = event.blockState.type
        } else {
            for (item in event.items) {
                val dropItem = item.itemStack
                if (dropItem.type == seedItem) {
                    dropItem.amount--
                    block.type = event.blockState.type
                    break
                }
            }
        }
    }
}