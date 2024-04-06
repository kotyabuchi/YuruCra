package com.github.kotyabuchi.YuruCra.Mastering.Gathering

import com.github.kotyabuchi.YuruCra.Mastering.GatheringMastering
import com.github.kotyabuchi.YuruCra.Utility.isShovel
import org.bukkit.Material

object Excavator: GatheringMastering("EXCAVATOR") {

    override val canGetExpWithHand: Boolean = true
    private val dirtSet = setOf(
        Material.DIRT, Material.SAND, Material.GRASS_BLOCK, Material.GRAVEL, Material.MUD,
        Material.DIRT_PATH, Material.COARSE_DIRT, Material.PODZOL, Material.RED_SAND, Material.SOUL_SAND, Material.SOUL_SOIL,
    )

    init {
        Material.values().forEach {
            if (it.isShovel()) addTool(it)
        }
        dirtSet.forEach {
            addExpMap(1, it)
        }
        addExpMap(2, Material.CLAY)
    }
}