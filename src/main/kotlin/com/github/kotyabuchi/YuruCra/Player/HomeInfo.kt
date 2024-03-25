package com.github.kotyabuchi.YuruCra.Player

import org.bukkit.Location
import org.bukkit.Material

data class HomeInfo(
    val id: Int,
    val name: String,
    val icon: Material,
    val location: Location
) {

    init {
        location.pitch = 0f
    }
}