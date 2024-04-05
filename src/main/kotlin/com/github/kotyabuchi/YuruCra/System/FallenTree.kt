package com.github.kotyabuchi.YuruCra.System

import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Utility.BlockUtil
import com.github.kotyabuchi.YuruCra.Utility.TreeType
import com.github.kotyabuchi.YuruCra.Utility.isLog
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object FallenTree: Listener {

    private val main: Main = Main.instance
    private val fallenTreeKey = NamespacedKey(main, "fallenTree")

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        val toolItemStack = player.inventory.itemInMainHand
        val block = event.block
        val material = block.type

        if (material.isLog()) {
            val world = block.world
            val treeBlocks = mutableListOf<Block>()
            val treeType = TreeType.fromMaterial(material) ?: return

            BlockUtil.searchTreeStructure(block, block, treeType, treeBlocks)
            treeBlocks.remove(block)

            treeBlocks.forEach {
                world.spawn(it.location.toCenterLocation().subtract(.0, .5, .0), FallingBlock::class.java) { fallingSand ->
                    val dropItems = it.getDrops(toolItemStack).map { it.serializeAsBytes() }
                    fallingSand.blockData = it.blockData
                    fallingSand.blockState = it.state.copy()
                    fallingSand.persistentDataContainer.set(fallenTreeKey, PersistentDataType.LIST.byteArrays(), dropItems)
                    if ((it.blockData as? Waterlogged)?.isWaterlogged == true) {
                        it.type = Material.WATER
                    } else {
                        it.type = Material.AIR
                    }
                }
            }
        }
    }

    @EventHandler
    fun onLanding(event: EntityChangeBlockEvent) {
        if (dropItem(event.entity)) event.isCancelled = true
    }

    @EventHandler
    fun onDropItem(event: EntityDropItemEvent) {
        if (dropItem(event.entity)) event.isCancelled = true
    }

    private fun dropItem(entity: Entity): Boolean {
        if (entity !is FallingBlock) return false
        val dropItemByteArray = entity.persistentDataContainer.get(fallenTreeKey, PersistentDataType.LIST.byteArrays()) ?: return false

        dropItemByteArray.forEach {
            val dropItem = ItemStack.deserializeBytes(it)
            entity.world.dropItemNaturally(entity.location, dropItem)
        }

        entity.remove()
        return true
    }
}