package com.github.kotyabuchi.MCRPG

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object TPlayerStatus: Table("Player_Status") {
    val uuid = varchar("uuid", 36)
    val playTime = long("play_time")
    val lastLoginDate = datetime("last_login_date")
    val lastPlayVersion = double("last_play_version")
}