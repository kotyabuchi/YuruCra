package com.github.kotyabuchi.YuruCra.Utility

import org.bukkit.Material

fun Material.isLog(): Boolean {
    return this.name.endsWith("_LOG")
}

fun Material.isLeaves(): Boolean {
    return this.name.endsWith("_LEAVES")
}