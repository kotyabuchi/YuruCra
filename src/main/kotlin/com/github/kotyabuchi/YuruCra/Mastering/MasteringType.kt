package com.github.kotyabuchi.YuruCra.Mastering

import com.github.kotyabuchi.YuruCra.Mastering.Gathering.*
import com.github.kotyabuchi.YuruCra.Utility.upperCamelCase
import org.bukkit.Material

enum class MasteringType(val masteringClass: Mastering, private val icon: Material, val masteringCategory: MasteringCategory, val regularName: String = masteringClass.masteringName.upperCamelCase()) {
    MINER(Miner, Material.STONE_PICKAXE, MasteringCategory.GATHERING),
    EXCAVATOR(Excavator, Material.STONE_SHOVEL, MasteringCategory.GATHERING),
    LUMBERJACK(Lumberjack, Material.STONE_AXE, MasteringCategory.GATHERING),
    FARMER(Farmer, Material.STONE_HOE, MasteringCategory.GATHERING),
    FISHING(Fishing, Material.FISHING_ROD, MasteringCategory.GATHERING),
    ;
    fun getIcon(): Material = icon
}