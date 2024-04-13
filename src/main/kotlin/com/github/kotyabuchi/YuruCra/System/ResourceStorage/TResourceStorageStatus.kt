package com.github.kotyabuchi.YuruCra.System.ResourceStorage

import org.jetbrains.exposed.sql.Table

object TResourceStorageStatus: Table("resource_storage_status") {
    val uuid = varchar("uuid", 36)
    val unlockedSlotSize = integer("unlocked_slot_size")
    val autoStore = bool("auto_store")
}