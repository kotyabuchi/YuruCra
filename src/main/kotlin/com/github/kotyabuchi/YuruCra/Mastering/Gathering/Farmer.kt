package com.github.kotyabuchi.YuruCra.Mastering.Gathering

import com.github.kotyabuchi.YuruCra.Mastering.GatheringMastering
import com.github.kotyabuchi.YuruCra.Utility.isHoe
import org.bukkit.Material

object Farmer: GatheringMastering("FARMER") {

    init {
        Material.values().forEach {
            if (it.isHoe()) addTool(it)
        }
        addExpMap(1, Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.SWEET_BERRIES, Material.GLOW_BERRIES)
        addExpMap(2, Material.MELON, Material.CHORUS_FRUIT)
        addExpMap(10, Material.PUMPKIN)
    }
}