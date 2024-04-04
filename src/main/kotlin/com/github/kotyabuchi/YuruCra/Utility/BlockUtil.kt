package com.github.kotyabuchi.YuruCra.Utility

import com.github.kotyabuchi.YuruCra.Event.BlockMineEvent
import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.data.type.Leaves
import org.bukkit.entity.Item
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack

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

fun Block.miningWithEvent(main: Main, player: PlayerStatus, itemStack: ItemStack, mainBlock: Block = this, damage: Boolean = true, isMultiBreak: Boolean = false, isMineAssist: Boolean = false, dropItemCallBack: (BlockDropItemEvent) -> Unit = {}, blockCallBack: (Block) -> Unit = {}) {
    val isMainBlock = this == mainBlock
    val mineEvent = BlockMineEvent(this, player, itemStack, isMainBlock, isMultiBreak, isMineAssist)
    main.server.pluginManager.callEvent(mineEvent)
    if (!mineEvent.isCancelled) {
        this.breakBlock(main, player, itemStack, mainBlock, damage, dropItemCallBack, blockCallBack)
    }
}

fun Block.breakBlock(main: Main, player: PlayerStatus, itemStack: ItemStack, mainBlock: Block, damage: Boolean, dropItemCallBack: (BlockDropItemEvent) -> Unit = {}, blockCallBack: (Block) -> Unit = {}) {
    val dropItems = mutableListOf<Item>()
    if (player.isDebugMode || player.gameMode != GameMode.CREATIVE) {
        this.getDrops(itemStack, player.player).forEach { item ->
            if (!item.type.isAir) {
                val dropItem = mainBlock.world.dropItem(mainBlock.location.toCenterLocation(), item)
                dropItems.add(dropItem)
            }
        }
        if (damage && itemStack.type.hasDurability()) itemStack.damage(player, 1)
    }
    val state = this.state
    if (this != mainBlock) {
        this.world.playSound(this.location.add(.5, .5, .5), this.blockSoundGroup.breakSound, 1f, .75f)
        this.world.spawnParticle(Particle.BLOCK_CRACK, this.location.add(0.5, 0.5, 0.5), 20, .3, .3, .3, .0, this.blockData)
    }
    if (state is Container && !this.type.name.endsWith("SHULKER_BOX")) {
        val inventory = if (state is Chest) {
            state.blockInventory
        } else {
            state.inventory
        }
        inventory.viewers.forEach {
            it.closeInventory()
        }
        inventory.storageContents.forEach {
            it?.let { mainBlock.world.dropItem(mainBlock.location.toCenterLocation(), it) }
        }
    }
    this.type = Material.AIR

    if (dropItems.isNotEmpty()) {
        val dropEvent = BlockDropItemEvent(this, state, player, dropItems)
        main.server.pluginManager.callEvent(dropEvent)
        dropItemCallBack(dropEvent)
        if (dropEvent.items.isEmpty()) {
            dropItems.forEach { item ->
                item.remove()
            }
        }
    }
    blockCallBack(this)
}

fun Block.destroyWithEffect(playSound: Boolean = true) {
    val effectMaterial = this.type
    if (playSound) this.world.playSound(this.location, Sound.BLOCK_STONE_BREAK, .8f, .75f)
    this.world.spawnParticle(Particle.BLOCK_CRACK, this.location.add(.5, .5, .5), 20, .3, .3, .3, 2.0, effectMaterial.createBlockData())
    this.type = Material.AIR
}