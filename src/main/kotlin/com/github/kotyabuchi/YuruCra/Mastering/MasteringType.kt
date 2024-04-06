package com.github.kotyabuchi.YuruCra.Mastering

import com.github.kotyabuchi.YuruCra.Mastering.Gathering.*
import com.github.kotyabuchi.YuruCra.Utility.upperCamelCase
import org.bukkit.Material

enum class MasteringType(val id: Int, val masteringClass: Mastering, private val icon: Material, val masteringCategory: MasteringCategory, val regularName: String = masteringClass.masteringName.upperCamelCase()) {
    MINER(1, Miner, Material.STONE_PICKAXE, MasteringCategory.GATHERING),
    EXCAVATOR(2, Excavator, Material.STONE_SHOVEL, MasteringCategory.GATHERING),
    LUMBERJACK(3, Lumberjack, Material.STONE_AXE, MasteringCategory.GATHERING),
    FARMER(4, Farmer, Material.STONE_HOE, MasteringCategory.GATHERING),
    FISHING(5, Fishing, Material.FISHING_ROD, MasteringCategory.GATHERING),
    ;
    fun getIcon(): Material = icon

    companion object {
        fun getMastering(id: Int): MasteringType? {
            return values().find { it.id == id }
        }
    }
}