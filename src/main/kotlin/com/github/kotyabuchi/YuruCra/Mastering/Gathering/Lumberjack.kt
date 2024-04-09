package com.github.kotyabuchi.YuruCra.Mastering.Gathering

import com.github.kotyabuchi.YuruCra.Mastering.GatheringMastering
import com.github.kotyabuchi.YuruCra.Mastering.Skill.Gathering.FallenTree
import com.github.kotyabuchi.YuruCra.Utility.isAxe
import com.github.kotyabuchi.YuruCra.Utility.isLog
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.world.StructureGrowEvent

object Lumberjack: GatheringMastering("LUMBERJACK") {

    init {
        Material.values().forEach {
            if (it.isAxe()) addTool(it)
            if (it.isLog()) addExpMap(1, it)
        }

        registerPassiveSkill(FallenTree)
    }

    @EventHandler
    fun onFade(event: BlockFadeEvent) {
        if (!event.isCancelled && isTargetBlock(event.block)) removePlacedBlock(event.block)
    }

    @EventHandler
    fun onGrow(event: BlockGrowEvent) {
        if (!event.isCancelled && isTargetBlock(event.block)) removePlacedBlock(event.block)
    }

    @EventHandler
    fun onStructureGrow(event: StructureGrowEvent) {
        if (!event.isCancelled) {
            event.blocks.forEach {
                if (isTargetMaterial(it.type)) removePlacedBlock(it.block)
            }
        }
    }
}