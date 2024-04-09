package com.github.kotyabuchi.YuruCra.Mastering.Skill.Gathering

import com.github.kotyabuchi.YuruCra.Event.BlockMineEvent
import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Mastering.Gathering.Miner
import com.github.kotyabuchi.YuruCra.Mastering.Mastering
import com.github.kotyabuchi.YuruCra.Mastering.Skill.PassiveSkill
import com.github.kotyabuchi.YuruCra.Utility.*
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

object MineAssist: PassiveSkill {
    override val ownerMastering: Mastering = Miner
    override val main: Main = Main.instance
    override val skillName: String = "MINE_ASSIST"
    override val displayName: String = "Mine Assist"
    override val cost: Int = 0
    override val needLevel: Int = 10
    override var description: String = "鉱石を破壊した際に繋がった鉱石もまとめて採掘する"
    override val coolTime: Long = 0
    override val lastUseTime: MutableMap<UUID, Long> = mutableMapOf()

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockMineEvent) {
        if (event.isCancelled) return
        if (event.isMineAssist) return

        val player = event.player

        if (!isEnabledSkill(player)) return

        val block = event.block
        val itemStack = player.inventory.itemInMainHand
        if (!itemStack.type.isPickAxe()) return
        if (!block.type.isOre()) return
        event.isCancelled = true

        val ores: MutableList<Block> = mutableListOf()
        searchOres(block, ores)
        ores.remove(block)

        block.miningWithEvent(main, player, itemStack, block, true, event.isMultiBreak, true)
        ores.forEach {
            it.miningWithEvent(main, player, itemStack, block, false, isMineAssist = true)
        }
        player.foodLevel = max(0, player.foodLevel - ceil(ores.size / 10.0).toInt())
        itemStack.damage(player, ores.size)
    }

    private fun searchOres(checkBlock: Block, oreList: MutableList<Block>, checkedList: MutableSet<Block> = mutableSetOf()) {
        if (!checkedList.add(checkBlock)) return
        if (!checkBlock.type.isOre()) return
        oreList.add(checkBlock)
        if (checkedList.size > 1000) return

        checkBlock.getAroundBlocks().forEach {
            searchOres(it, oreList, checkedList)
        }
    }
}