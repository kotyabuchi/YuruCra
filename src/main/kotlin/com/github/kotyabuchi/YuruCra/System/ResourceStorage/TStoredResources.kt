package com.github.kotyabuchi.YuruCra.System.ResourceStorage

import org.jetbrains.exposed.sql.Table

object TStoredResources: Table("stored_resources") {
    val uuid = varchar("uuid", 36)
    val resourceName = varchar("resource_name", 36)
    val amount = integer("amount")
}