package com.github.kotyabuchi.YuruCra.Item

import org.bukkit.Material

enum class ToolType(override val materialCost: Int?): EquipmentType {
    PICKAXE(3) {
        override fun includes(item: Material): Boolean {
            return item == Material.WOODEN_PICKAXE ||
                    item == Material.STONE_PICKAXE ||
                    item == Material.IRON_PICKAXE ||
                    item == Material.GOLDEN_PICKAXE ||
                    item == Material.DIAMOND_PICKAXE ||
                    item == Material.NETHERITE_PICKAXE
        }
    },
    AXE(3) {
        override fun includes(item: Material): Boolean {
            return item == Material.WOODEN_AXE ||
                    item == Material.STONE_AXE ||
                    item == Material.IRON_AXE ||
                    item == Material.GOLDEN_AXE ||
                    item == Material.DIAMOND_AXE ||
                    item == Material.NETHERITE_AXE
        }
    },
    SHOVEL(1) {
        override fun includes(item: Material): Boolean {
            return item == Material.WOODEN_SHOVEL ||
                    item == Material.STONE_SHOVEL ||
                    item == Material.IRON_SHOVEL ||
                    item == Material.GOLDEN_SHOVEL ||
                    item == Material.DIAMOND_SHOVEL ||
                    item == Material.NETHERITE_SHOVEL
        }
    },
    HOE(2) {
        override fun includes(item: Material): Boolean {
            return item == Material.WOODEN_HOE ||
                    item == Material.STONE_HOE ||
                    item == Material.IRON_HOE ||
                    item == Material.GOLDEN_HOE ||
                    item == Material.DIAMOND_HOE ||
                    item == Material.NETHERITE_HOE
        }
    },
    SWORD(2) {
        override fun includes(item: Material): Boolean {
            return item == Material.WOODEN_SWORD ||
                    item == Material.STONE_SWORD ||
                    item == Material.IRON_SWORD ||
                    item == Material.GOLDEN_SWORD ||
                    item == Material.DIAMOND_SWORD ||
                    item == Material.NETHERITE_SWORD
        }
    },
    BATTLEAXE(3) {
        override fun includes(item: Material): Boolean {
            return item == Material.WOODEN_AXE ||
                    item == Material.STONE_AXE ||
                    item == Material.IRON_AXE ||
                    item == Material.GOLDEN_AXE ||
                    item == Material.DIAMOND_AXE ||
                    item == Material.NETHERITE_AXE
        }
    },
    BOW(3) {
        override fun includes(item: Material): Boolean {
            return item == Material.BOW ||
                    item == Material.CROSSBOW
        }
    },
    TRIDENT(null) {
        override fun includes(item: Material): Boolean {
            return item == Material.TRIDENT
        }
    },
    SHIELD(2) {
        override fun includes(item: Material): Boolean {
            return item == Material.SHIELD
        }
    },
    FISHING_ROD(2) {
        override fun includes(item: Material): Boolean {
            return item == Material.FISHING_ROD
        }
    };

    override fun getEquipmentName(): String = name
}