package com.github.kotyabuchi.yurucra.data

import com.github.kotyabuchi.yurucra.job.JobType
import com.github.kotyabuchi.yurucra.player.PlayerSnapshot
import org.bukkit.Material
import java.sql.Connection
import java.util.UUID

/** DBから読み込んだままのプレイヤーデータ（ホームは HomeRepository で別に読む） */
data class StoredPlayerData(
    val jobTotalExp: Map<JobType, Double>,
    val storageSlotSize: Int?,
    val storedResources: Map<Material, Int>,
)

/** ジョブ経験値とリソースストレージの読み書き。DBスレッドからだけ呼ぶこと */
object PlayerDataRepository {

    fun load(conn: Connection, uuid: UUID): StoredPlayerData {
        val id = uuid.toString()

        val jobExp = conn.prepareStatement(
            "SELECT jobs.job_name, player_job_status.job_total_exp FROM player_job_status " +
                "INNER JOIN jobs ON player_job_status.job_id = jobs.job_id WHERE player_job_status.uuid = ?"
        ).use { pstmt ->
            pstmt.setString(1, id)
            pstmt.executeQuery().use { rs ->
                buildMap {
                    while (rs.next()) {
                        val type = JobType.entries.find { it.name == rs.getString("job_name") } ?: continue
                        put(type, rs.getDouble("job_total_exp"))
                    }
                }
            }
        }

        val slotSize = conn.prepareStatement("SELECT slot_size FROM resource_storage_size WHERE uuid = ?").use { pstmt ->
            pstmt.setString(1, id)
            pstmt.executeQuery().use { rs -> if (rs.next()) rs.getInt("slot_size") else null }
        }

        val resources = conn.prepareStatement("SELECT material, amount FROM resource_storage WHERE uuid = ?").use { pstmt ->
            pstmt.setString(1, id)
            pstmt.executeQuery().use { rs ->
                buildMap {
                    while (rs.next()) {
                        // 新しいバージョンで名前が変わった素材は読み飛ばす
                        val material = Material.getMaterial(rs.getString("material")) ?: continue
                        put(material, rs.getInt("amount"))
                    }
                }
            }
        }

        return StoredPlayerData(jobExp, slotSize, resources)
    }

    fun save(conn: Connection, snapshots: Collection<PlayerSnapshot>) {
        if (snapshots.isEmpty()) return
        conn.transaction {
            val jobIds = conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT job_id, job_name FROM jobs").use { rs ->
                    buildMap {
                        while (rs.next()) put(rs.getString("job_name"), rs.getInt("job_id"))
                    }
                }
            }

            conn.prepareStatement("REPLACE INTO player_job_status VALUES (?, ?, ?)").use { pstmt ->
                snapshots.forEach { snapshot ->
                    snapshot.jobTotalExp.forEach { (type, exp) ->
                        val jobId = jobIds[type.name] ?: return@forEach
                        pstmt.setString(1, snapshot.uuid.toString())
                        pstmt.setInt(2, jobId)
                        pstmt.setDouble(3, exp)
                        pstmt.addBatch()
                    }
                }
                pstmt.executeBatch()
            }

            conn.prepareStatement("DELETE FROM resource_storage WHERE uuid = ?").use { pstmt ->
                snapshots.forEach {
                    pstmt.setString(1, it.uuid.toString())
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
            }

            conn.prepareStatement("INSERT INTO resource_storage(uuid, material, amount) VALUES (?, ?, ?)").use { pstmt ->
                snapshots.forEach { snapshot ->
                    snapshot.storedResources.forEach { (material, amount) ->
                        pstmt.setString(1, snapshot.uuid.toString())
                        pstmt.setString(2, material.name)
                        pstmt.setInt(3, amount)
                        pstmt.addBatch()
                    }
                }
                pstmt.executeBatch()
            }

            conn.prepareStatement("REPLACE INTO resource_storage_size VALUES (?, ?)").use { pstmt ->
                snapshots.forEach {
                    pstmt.setString(1, it.uuid.toString())
                    pstmt.setInt(2, it.storageSlotSize)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
            }
        }
    }
}
