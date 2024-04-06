package com.github.kotyabuchi.YuruCra.Mastering

import com.github.kotyabuchi.YuruCra.Event.*
import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.PersistantDataType.PersistentDataTypeUUID
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import kotlin.math.floor
import kotlin.random.Random

open class GatheringMastering(masteringName: String): Mastering(masteringName) {
    private val expMap = mutableMapOf<Material, Int>()
    private val brokenBlockSet = mutableSetOf<Block>()
    open val canGetExpWithHand = true
    private val placedBlock = mutableMapOf<Block, BukkitTask>()

    companion object {
        val minerKey = NamespacedKey(Main.instance, "miner")
    }

    fun addExpMap(exp: Int, vararg materials: Material) {
        materials.forEach {
            expMap[it] = exp
        }
    }

    fun isTargetBlock(block: Block): Boolean {
        return isTargetMaterial(block.type)
    }

    fun isTargetMaterial(material: Material): Boolean {
        return expMap.containsKey(material)
    }

    fun addBrokenBlockSet(block: Block) {
        brokenBlockSet.add(block)
    }

    fun containsBrokenBlockSet(block: Block): Boolean {
        return brokenBlockSet.contains(block)
    }

    fun addPlacedBlock(block: Block) {
        removePlacedBlock(block)
        placedBlock[block] = object : BukkitRunnable() {
            override fun run() {
                placedBlock.remove(block)
            }
        }.runTaskLater(main, 20 * 60L)
    }

    fun removePlacedBlock(block: Block) {
        placedBlock[block]?.cancel()
        placedBlock.remove(block)
    }

    open fun afterDropAction(event: BlockDropItemEvent) {}

    @EventHandler(priority = EventPriority.HIGH)
    fun onDropItemFromBlock(event: BlockDropItemEvent) {
        val block = event.block

        if (containsBrokenBlockSet(block)) {
            brokenBlockSet.remove(block)
            if (placedBlock.containsKey(block)) {
                removePlacedBlock(block)
            } else {
                CustomEventCaller.callEvent(GatheringItemDropEvent(event.blockState.type, event.items, event.player))
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onDropItemFromFallingBlock(event: EntityDropItemsEvent) {
        val entity = event.entity as? FallingBlock ?: return
        val blockState = entity.blockState
        if (!isTargetMaterial(blockState.type)) return
        val pdc = entity.persistentDataContainer
        val minerUUID = pdc.get(minerKey, PersistentDataTypeUUID) ?: return
        val player = main.server.getPlayer(minerUUID) ?: return

        CustomEventCaller.callEvent(GatheringItemDropEvent(blockState.type, event.dropItems, player))
    }

    @EventHandler
    fun onGatheringItemDrop(event: GatheringItemDropEvent) {
        val player = event.player
        val masteringManager = player.getStatus().masteringManager

        var exp = 0.0
        val doubleDropChance = masteringManager.getMasteringStatus(this).getLevel() / 3
        var multiDropAmount = 1 + floor(doubleDropChance / 100.0).toInt()
        if (Random.nextInt(100) < doubleDropChance % 100) {
            multiDropAmount++
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.0f)
        }
        val itemExp = expMap[event.baseMaterial] ?: 1
        event.items.forEach {
            val item = it.itemStack
            item.amount *= multiDropAmount
            exp += (itemExp * item.amount)
        }

        masteringManager.addExp(this, exp, multiDropAmount)
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        if (event.isCancelled) return
        addPlacedBlock(event.block)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockMine(event: BlockMineEvent) {
        if (event.isCancelled) return
        val player = event.player
        val block = event.block
        val item = player.inventory.itemInMainHand
        val toolType= item.type
        if (!isValidTool(toolType) && !canGetExpWithHand) return
        if (!isTargetBlock(block)) return
        addBrokenBlockSet(block)
        CustomEventCaller.callEvent(GatheringEvent(player, this, block))
    }
}