package com.github.kotyabuchi.realisticsurvival.player

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import java.util.UUID

/**
 * ホーム地点。id が null のものは「ワールドスポーン」「リスポーン地点」のような一時的な表示用。
 * ワールドは名前で持ち、テレポート時に解決する（未読み込みのワールドでもデータを失わないため）。
 */
class Home(
    val id: Int?,
    var name: String,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    var icon: Material,
    val creator: UUID? = null,
    var isPublic: Boolean = false,
) {
    val world: World? get() = Bukkit.getWorld(worldName)

    fun location(): Location? = world?.let { Location(it, x, y, z, yaw, 0f) }

    companion object {
        /** 保存しない表示用のホーム */
        fun temporary(name: String, location: Location, icon: Material): Home =
            Home(null, name, location.world.name, location.x, location.y, location.z, location.yaw, icon)
    }
}
