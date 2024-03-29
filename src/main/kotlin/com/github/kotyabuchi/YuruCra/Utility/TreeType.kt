package com.github.kotyabuchi.YuruCra.Utility

import org.bukkit.Material

enum class TreeType {
    OAK,
    SPRUCE,
    BIRCH,
    JUNGLE,
    ACACIA,
    DARK_OAK,
    MANGROVE,
    CHERRY,
    CRIMSON,
    WARPED,
    ;

    companion object {
        fun fromMaterial(material: Material): TreeType? {
            return when (material.name) {
                else -> null
            }
        }
    }
}