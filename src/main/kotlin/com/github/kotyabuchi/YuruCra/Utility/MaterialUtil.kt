package com.github.kotyabuchi.YuruCra.Utility

import org.bukkit.Material

fun Material.isLog(): Boolean {
    return this.name.endsWith("_LOG")
            || this.name.endsWith("_WOOD")
            || this.name.endsWith("_ROOTS")
            || this.name.endsWith("_STEM")
            || this.name.endsWith("_HYPHAE")
}

fun Material.isLeaves(): Boolean {
    return this.name.endsWith("_LEAVES")
}

fun Material.isOre(): Boolean {
    return this == Material.COAL_ORE ||
            this == Material.DEEPSLATE_COAL_ORE ||
            this == Material.IRON_ORE ||
            this == Material.DEEPSLATE_IRON_ORE ||
            this == Material.COPPER_ORE ||
            this == Material.DEEPSLATE_COPPER_ORE ||
            this == Material.GOLD_ORE ||
            this == Material.DEEPSLATE_GOLD_ORE ||
            this == Material.REDSTONE_ORE ||
            this == Material.DEEPSLATE_REDSTONE_ORE ||
            this == Material.LAPIS_ORE ||
            this == Material.DEEPSLATE_LAPIS_ORE ||
            this == Material.DIAMOND_ORE ||
            this == Material.DEEPSLATE_DIAMOND_ORE ||
            this == Material.EMERALD_ORE ||
            this == Material.DEEPSLATE_EMERALD_ORE ||
            this == Material.NETHER_GOLD_ORE ||
            this == Material.GLOWSTONE ||
            this == Material.NETHER_QUARTZ_ORE ||
            this == Material.GILDED_BLACKSTONE ||
            this == Material.ANCIENT_DEBRIS
}