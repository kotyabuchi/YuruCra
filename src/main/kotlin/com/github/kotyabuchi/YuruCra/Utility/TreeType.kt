package com.github.kotyabuchi.YuruCra.Utility

import org.bukkit.Material

enum class TreeType {
    DARK_OAK,
    OAK,
    SPRUCE,
    BIRCH,
    JUNGLE,
    ACACIA,
    MANGROVE,
    CHERRY,
    CRIMSON,
    WARPED,
    ;

    companion object {
        fun fromMaterial(material: Material): TreeType? {
            values().forEach {
                if (material.name.contains(it.name)) return it
            }
            return null
        }
    }
}