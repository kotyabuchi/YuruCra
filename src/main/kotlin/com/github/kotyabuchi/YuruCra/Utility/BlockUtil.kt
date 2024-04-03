package com.github.kotyabuchi.YuruCra.Utility

import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Leaves

object BlockUtil  {
    fun searchTreeStructure(checkBlock: Block, baseBlock: Block, baseTreeType: TreeType, treeBlocks: MutableList<Block>, checkedList: MutableSet<Block> = mutableSetOf()) {
        val maxSearchCount = 2000
        if (checkedList.size > maxSearchCount) return
        if (baseBlock.y > checkBlock.y) return
        if (!checkedList.add(checkBlock)) return

        val material = checkBlock.type
        if (TreeType.fromMaterial(checkBlock.type) != baseTreeType) return
        if (material.isLog()) {
            treeBlocks.add(checkBlock)
        } else if (material.isLeaves()) {
            if (treeBlocks.contains(checkBlock)) return
            searchLeaves(checkBlock, 0, baseTreeType, treeBlocks)
            return
        } else {
            return
        }

        checkBlock.getFullAroundBlocks().forEach {
            searchTreeStructure(it, baseBlock, baseTreeType, treeBlocks, checkedList)
        }
    }

    fun searchLeaves(checkBlock: Block, prevDistance: Int, baseTreeType: TreeType, leavesBlocks: MutableList<Block>) {
        if (leavesBlocks.contains(checkBlock)) return

        val material = checkBlock.type
        if (!material.isLeaves()) return
        if (TreeType.fromMaterial(material) != baseTreeType) return

        val leaves = checkBlock.blockData as? Leaves ?: return
        if (leaves.isPersistent) return
        val distance = leaves.distance
        if (distance <= prevDistance) return
        leavesBlocks.add(checkBlock)

        checkBlock.getAroundBlocks().forEach {
            searchLeaves(it, distance, baseTreeType, leavesBlocks)
        }
    }
}


fun Block.getFullAroundBlocks(): List<Block> {
    val faces = listOf(
        BlockFace.NORTH,
        BlockFace.NORTH_EAST,
        BlockFace.EAST,
        BlockFace.SOUTH_EAST,
        BlockFace.SOUTH,
        BlockFace.SOUTH_WEST,
        BlockFace.WEST,
        BlockFace.NORTH_WEST
    )
    val result = mutableListOf<Block>()
    val upBlock = this.getRelative(BlockFace.UP)
    val downBlock = this.getRelative(BlockFace.DOWN)
    result.add(upBlock)
    faces.forEach {
        result.add(upBlock.getRelative(it))
    }
    faces.forEach {
        result.add(this.getRelative(it))
    }
    result.add(downBlock)
    faces.forEach {
        result.add(downBlock.getRelative(it))
    }

    return result
}

fun Block.getAroundBlocks(): List<Block> {
    val faces = listOf(
        BlockFace.UP,
        BlockFace.NORTH,
        BlockFace.NORTH_EAST,
        BlockFace.EAST,
        BlockFace.SOUTH_EAST,
        BlockFace.SOUTH,
        BlockFace.SOUTH_WEST,
        BlockFace.WEST,
        BlockFace.NORTH_WEST,
        BlockFace.DOWN,
    )
    val result = mutableListOf<Block>()
    faces.forEach {
        result.add(this.getRelative(it))
    }

    return result
}