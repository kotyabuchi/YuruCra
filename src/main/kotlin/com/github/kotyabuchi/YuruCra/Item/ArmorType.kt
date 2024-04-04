package com.github.kotyabuchi.YuruCra.Item

import org.bukkit.Material

enum class ArmorType(override val materialCost: Int?): EquipmentType {
    HELMET(5) {
        override fun includes(item: Material): Boolean {
            return item == Material.LEATHER_HELMET ||
                    item == Material.IRON_HELMET ||
                    item == Material.CHAINMAIL_HELMET ||
                    item == Material.GOLDEN_HELMET ||
                    item == Material.DIAMOND_HELMET ||
                    item == Material.NETHERITE_HELMET ||
                    item == Material.TURTLE_HELMET
        }
    },
    CHESTPLATE(8) {
        override fun includes(item: Material): Boolean {
            return item == Material.LEATHER_CHESTPLATE ||
                    item == Material.IRON_CHESTPLATE ||
                    item == Material.CHAINMAIL_CHESTPLATE ||
                    item == Material.GOLDEN_CHESTPLATE ||
                    item == Material.DIAMOND_CHESTPLATE ||
                    item == Material.NETHERITE_CHESTPLATE
        }
    },
    LEGGINGS(7) {
        override fun includes(item: Material): Boolean {
            return item == Material.LEATHER_LEGGINGS ||
                    item == Material.IRON_LEGGINGS ||
                    item == Material.CHAINMAIL_LEGGINGS ||
                    item == Material.GOLDEN_LEGGINGS ||
                    item == Material.DIAMOND_LEGGINGS ||
                    item == Material.NETHERITE_LEGGINGS
        }
    },
    BOOTS(4) {
        override fun includes(item: Material): Boolean {
            return item == Material.LEATHER_BOOTS ||
                    item == Material.IRON_BOOTS ||
                    item == Material.CHAINMAIL_BOOTS ||
                    item == Material.GOLDEN_BOOTS ||
                    item == Material.DIAMOND_BOOTS ||
                    item == Material.NETHERITE_BOOTS
        }
    };

    override fun getEquipmentName(): String = name
}