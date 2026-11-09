package com.github.kotyabuchi.yurucra.tombstone

import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.UUID

/**
 * 墓石の情報（中身のアイテムは含まない。DBから回収時に読む）。
 * @property entityId 墓石として置いた防具立てのUUID
 */
data class TombStone(
    val id: Int,
    val owner: UUID,
    val entityId: UUID,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val createdAt: Long,
) {
    fun location(): Location? = Bukkit.getWorld(worldName)?.let { Location(it, x, y, z) }
}

/**
 * 墓石に入れるインベントリの並び。PlayerInventory#getContents と同じ41スロット
 * （0〜35: 持ち物、36〜39: 足・脚・胴・頭の防具、40: オフハンド）。
 */
object TombStoneSlots {
    const val SIZE = 41
    const val FEET = 36
    const val LEGS = 37
    const val CHEST = 38
    const val HEAD = 39
    const val OFF_HAND = 40
}
