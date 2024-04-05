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