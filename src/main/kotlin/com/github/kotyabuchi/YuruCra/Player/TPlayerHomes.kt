package com.github.kotyabuchi.YuruCra.Player

import org.jetbrains.exposed.dao.id.IntIdTable

object TPlayerHomes: IntIdTable("Player_Homes") {
    val uuid = varchar("uuid", 36)
    val name = varchar("name", 36)
    val icon = varchar("icon", 36)
    val world = varchar("world", 36)
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val yaw = float("yaw")
}