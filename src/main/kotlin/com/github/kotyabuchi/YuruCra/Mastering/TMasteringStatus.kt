package com.github.kotyabuchi.YuruCra.Mastering

import org.jetbrains.exposed.sql.Table

object TMasteringStatus: Table("mastering_status") {
    val masteringId = integer("mastering_id")
    val uuid = varchar("uuid", 36)
    val totalExp = double("total_exp")
}