package com.github.kotyabuchi.YuruCra.Mastering.Skill.Gathering

import com.github.kotyabuchi.YuruCra.Event.BlockMineEvent
import com.github.kotyabuchi.YuruCra.Event.CustomEventCaller
import com.github.kotyabuchi.YuruCra.Event.EntityDropItemsEvent
import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Mastering.Gathering.Lumberjack
import com.github.kotyabuchi.YuruCra.Mastering.GatheringMastering
import com.github.kotyabuchi.YuruCra.Mastering.Mastering
import com.github.kotyabuchi.YuruCra.Mastering.Skill.PassiveSkill
import com.github.kotyabuchi.YuruCra.PersistantDataType.PersistentDataTypeUUID
import com.github.kotyabuchi.YuruCra.Utility.BlockUtil
import com.github.kotyabuchi.YuruCra.Utility.TreeType
import com.github.kotyabuchi.YuruCra.Utility.isLog
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

object FallenTree: PassiveSkill {
    override val ownerMastering: Mastering = Lumberjack
    override val main: Main = Main.instance
    override val skillName: String = "FALLEN_TREE"
    override val displayName: String = "Fallen Tree"
    override val cost: Int = 0
    override val needLevel: Int = 0
    override var description: String = "破壊したブロックより上の木を崩壊させる"
    override val coolTime: Long = 0
    override val lastUseTime: MutableMap<UUID, Long> = mutableMapOf()

    private val fallenTreeKey = NamespacedKey(main, "fallenTree")

    @EventHandler(priority = EventPriority.HIGH)
    fun onBreak(event: BlockMineEvent) {
        if (event.isCancelled) return
        val player = event.player
        val toolItemStack = player.inventory.itemInMainHand
        val block = event.block
        val material = block.type

        if (!player.isSneaking && material.isLog()) {
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
                    fallingSand.persistentDataContainer.set(GatheringMastering.minerKey, PersistentDataTypeUUID, player.uniqueId)
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
        val dropItems = mutableListOf<Item>()

        dropItemByteArray.forEach {
            val dropItem = ItemStack.deserializeBytes(it)
            dropItems.add(entity.world.dropItemNaturally(entity.location, dropItem))
        }
        CustomEventCaller.callEvent(EntityDropItemsEvent(entity, dropItems))

        entity.remove()
        return true
    }
}