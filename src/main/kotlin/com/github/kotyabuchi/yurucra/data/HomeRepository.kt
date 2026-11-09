package com.github.kotyabuchi.yurucra.data

import com.github.kotyabuchi.yurucra.player.Home
import org.bukkit.Material
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.util.UUID

/** homes テーブルの読み書き。DBスレッド（Database.submit の中）からだけ呼ぶこと */
object HomeRepository {

    fun loadPublic(conn: Connection): List<Home> =
        conn.prepareStatement("SELECT * FROM homes WHERE is_public = 1").use { pstmt ->
            pstmt.executeQuery().use { it.readHomes() }
        }

    fun loadOwnedBy(conn: Connection, owner: UUID): List<Home> =
        conn.prepareStatement("SELECT * FROM homes WHERE uuid = ?").use { pstmt ->
            pstmt.setString(1, owner.toString())
            pstmt.executeQuery().use { it.readHomes() }
        }

    /** @return 追加したホームのID */
    fun insert(conn: Connection, owner: UUID, home: Home): Int =
        conn.prepareStatement(
            "INSERT INTO homes(uuid, home_name, world, x, y, z, yaw, icon) VALUES (?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use { pstmt ->
            pstmt.setString(1, owner.toString())
            pstmt.setString(2, home.name)
            pstmt.setString(3, home.worldName)
            pstmt.setDouble(4, home.x)
            pstmt.setDouble(5, home.y)
            pstmt.setDouble(6, home.z)
            pstmt.setFloat(7, home.yaw)
            pstmt.setString(8, home.icon.name)
            pstmt.executeUpdate()
            pstmt.generatedKeys.use { keys ->
                check(keys.next()) { "ホームIDを取得できませんでした" }
                keys.getInt(1)
            }
        }

    fun updateName(conn: Connection, id: Int, name: String) = update(conn, "home_name", id) { it.setString(1, name) }

    fun updatePublic(conn: Connection, id: Int, isPublic: Boolean) = update(conn, "is_public", id) { it.setInt(1, if (isPublic) 1 else 0) }

    fun updateIcon(conn: Connection, id: Int, icon: Material) = update(conn, "icon", id) { it.setString(1, icon.name) }

    fun delete(conn: Connection, id: Int) {
        conn.prepareStatement("DELETE FROM homes WHERE home_id = ?").use { pstmt ->
            pstmt.setInt(1, id)
            pstmt.executeUpdate()
        }
    }

    private inline fun update(conn: Connection, column: String, id: Int, bind: (java.sql.PreparedStatement) -> Unit) {
        conn.prepareStatement("UPDATE homes SET $column = ? WHERE home_id = ?").use { pstmt ->
            bind(pstmt)
            pstmt.setInt(2, id)
            pstmt.executeUpdate()
        }
    }

    private fun ResultSet.readHomes(): List<Home> = buildList {
        while (next()) {
            add(
                Home(
                    id = getInt("home_id"),
                    name = getString("home_name"),
                    worldName = getString("world"),
                    x = getDouble("x"),
                    y = getDouble("y"),
                    z = getDouble("z"),
                    yaw = getFloat("yaw"),
                    icon = getString("icon")?.let { Material.getMaterial(it) } ?: Material.ENDER_PEARL,
                    creator = UUID.fromString(getString("uuid")),
                    isPublic = getInt("is_public") == 1,
                )
            )
        }
    }
}
