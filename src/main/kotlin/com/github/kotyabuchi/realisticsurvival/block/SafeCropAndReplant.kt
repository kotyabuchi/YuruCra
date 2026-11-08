package com.github.kotyabuchi.realisticsurvival.block

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.ToolType
import com.github.kotyabuchi.realisticsurvival.util.runNextTick
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent

/**
 * クワで作物を収穫するときの補助。
 * - 育ちきっていない作物はクワでは壊れない
 * - 育ちきった作物を収穫すると、ドロップの種を1つ使って植え直す
 */
class SafeCropAndReplant(private val plugin: RealisticSurvival) : Listener {

    private val harvested = HashSet<Block>()
    private val seeds = mapOf(
        Material.WHEAT to Material.WHEAT_SEEDS,
        Material.CARROTS to Material.CARROT,
        Material.POTATOES to Material.POTATO,
        Material.BEETROOTS to Material.BEETROOT_SEEDS,
        Material.COCOA to Material.COCOA_BEANS,
    )

    /** クワ以外での収穫（デメテルのトーテムなど）でも植え直すようにする */
    fun markHarvest(block: Block) {
        harvested += block
        plugin.runNextTick { harvested.remove(block) }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val data = event.block.blockData as? Ageable ?: return
        if (!ToolType.HOE.includes(event.player.inventory.itemInMainHand)) return
        if (data.age != data.maximumAge) {
            event.isCancelled = true
        } else {
            markHarvest(event.block)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onDrop(event: BlockDropItemEvent) {
        val block = event.block
        if (!harvested.remove(block)) return
        val seed = seeds[event.blockState.type]
        if (seed != null) {
            val seedItem = event.items.firstOrNull { it.itemStack.type == seed } ?: return
            val stack = seedItem.itemStack
            stack.amount--
            if (stack.amount <= 0) event.items.remove(seedItem) else seedItem.itemStack = stack
        }
        replant(block, event.blockState.blockData)
    }

    // 元のブロックデータを使うので、ココアの向きなども保たれる
    private fun replant(block: Block, original: org.bukkit.block.data.BlockData) {
        val data = original.clone()
        if (data is Ageable) data.age = 0
        block.blockData = data
    }
}
