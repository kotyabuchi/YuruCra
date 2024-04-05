package com.github.kotyabuchi.YuruCra.Utility

import org.bukkit.Material

object MaterialUtil {
    val canNotItems: MutableList<Material> = mutableListOf()
}

fun Material.isLog(): Boolean {
    return this.name.endsWith("_LOG")
}

fun Material.isLeaves(): Boolean {
    return this.name.endsWith("_LEAVES")
}

fun Material.canNotItem(): Boolean {
    return MaterialUtil.canNotItems.contains(this)
}