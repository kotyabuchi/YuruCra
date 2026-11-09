package com.github.kotyabuchi.realisticsurvival.data

import com.github.kotyabuchi.realisticsurvival.tombstone.TombStone
import java.sql.Connection
import java.sql.Statement
import java.util.UUID

/** tombstones テーブルの読み書き。DBスレッドからだけ呼ぶこと */
object TombStoneRepository {

    /** 位置などの情報だけを読む（中身のアイテムは回収するときに loadItems で読む） */
    fun loadAll(conn: Connection): List<TombStone> =
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT tombstone_id, owner, entity_uuid, world, x, y, z, created_at FROM tombstones").use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            TombStone(
                                id = rs.getInt("tombstone_id"),
                                owner = UUID.fromString(rs.getString("owner")),
                                entityId = UUID.fromString(rs.getString("entity_uuid")),
                                worldName = rs.getString("world"),
                                x = rs.getDouble("x"),
                                y = rs.getDouble("y"),
                                z = rs.getDouble("z"),
                                createdAt = rs.getLong("created_at"),
                            )
                        )
                    }
                }
            }
        }

    fun loadItems(conn: Connection, id: Int): ByteArray? =
        conn.prepareStatement("SELECT items FROM tombstones WHERE tombstone_id = ?").use { pstmt ->
            pstmt.setInt(1, id)
            pstmt.executeQuery().use { rs -> if (rs.next()) rs.getBytes("items") else null }
        }

    /** @return 追加した墓石のID */
    fun insert(conn: Connection, stone: TombStone, items: ByteArray): Int =
        conn.prepareStatement(
            "INSERT INTO tombstones(owner, entity_uuid, world, x, y, z, created_at, items) VALUES (?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use { pstmt ->
            pstmt.setString(1, stone.owner.toString())
            pstmt.setString(2, stone.entityId.toString())
            pstmt.setString(3, stone.worldName)
            pstmt.setDouble(4, stone.x)
            pstmt.setDouble(5, stone.y)
            pstmt.setDouble(6, stone.z)
            pstmt.setLong(7, stone.createdAt)
            pstmt.setBytes(8, items)
            pstmt.executeUpdate()
            pstmt.generatedKeys.use { keys ->
                check(keys.next()) { "墓石IDを取得できませんでした" }
                keys.getInt(1)
            }
        }

    fun existsEntity(conn: Connection, entityId: UUID): Boolean =
        conn.prepareStatement("SELECT 1 FROM tombstones WHERE entity_uuid = ?").use { pstmt ->
            pstmt.setString(1, entityId.toString())
            pstmt.executeQuery().use { it.next() }
        }

    fun delete(conn: Connection, id: Int) {
        conn.prepareStatement("DELETE FROM tombstones WHERE tombstone_id = ?").use { pstmt ->
            pstmt.setInt(1, id)
            pstmt.executeUpdate()
        }
    }
}
