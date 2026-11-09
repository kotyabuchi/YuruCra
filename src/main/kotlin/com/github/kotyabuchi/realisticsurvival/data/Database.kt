package com.github.kotyabuchi.realisticsurvival.data

import java.io.File
import java.sql.Connection
import java.util.Properties
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * SQLite へのアクセスはすべて専用スレッド1本で順番に実行する。
 * こうすると「ログアウト時の保存」と「直後の再ログイン時の読み込み」の順番が必ず守られる。
 * メインスレッドから直接 SQL を実行しないこと（サーバーが止まる）。
 */
class Database(private val file: File, private val logger: Logger) {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "RealisticSurvival-DB").apply { isDaemon = true }
    }

    // DBスレッドからしか触らない
    private var connection: Connection? = null

    fun <T> submit(task: (Connection) -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync({ task(connection()) }, executor)

    /** 起動時にテーブルを作る（旧版と同じ構成なので、旧版の .db ファイルをそのまま使える） */
    fun initSchema(jobNames: List<String>) {
        submit { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_job_status (uuid TEXT NOT NULL, job_id INTEGER NOT NULL, job_total_exp REAL NOT NULL, UNIQUE(uuid, job_id))")
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS jobs (job_id INTEGER PRIMARY KEY AUTOINCREMENT, job_name TEXT UNIQUE NOT NULL)")
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS homes (home_id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT NOT NULL, home_name TEXT NOT NULL, world TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, yaw REAL NOT NULL, icon TEXT, is_public INTEGER DEFAULT 0)")
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS resource_storage_size (uuid TEXT NOT NULL PRIMARY KEY, slot_size INTEGER NOT NULL DEFAULT 2)")
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS resource_storage (uuid TEXT NOT NULL, material TEXT NOT NULL, amount INTEGER NOT NULL)")
                // 新版で追加（旧版は TombStones.json に保存していた）。items はインベントリ41スロット分をまとめたバイト列
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tombstones (tombstone_id INTEGER PRIMARY KEY AUTOINCREMENT, owner TEXT NOT NULL, entity_uuid TEXT NOT NULL UNIQUE, world TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, created_at INTEGER NOT NULL, items BLOB NOT NULL)")
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS tombstones_owner ON tombstones(owner)")
            }
            conn.prepareStatement("INSERT OR IGNORE INTO jobs(job_name) VALUES (?)").use { pstmt ->
                jobNames.forEach {
                    pstmt.setString(1, it)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
            }
        }.join()
    }

    /** 残っている処理を待ってから接続を閉じる */
    fun close() {
        try {
            submit { conn ->
                conn.close()
                connection = null
            }.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.severe("データベースを正常に閉じられませんでした: ${e.message}")
        }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
    }

    private fun connection(): Connection {
        connection?.let { return it }
        // plugin.yml の libraries で読み込んだドライバは DriverManager から見えないことがあるので直接使う
        val conn = org.sqlite.JDBC().connect("jdbc:sqlite:${file.absolutePath}", Properties())
            ?: error("SQLite に接続できませんでした: ${file.absolutePath}")
        conn.createStatement().use { it.execute("PRAGMA journal_mode=WAL") }
        connection = conn
        return conn
    }
}

/** 複数のSQLをまとめて実行し、途中で失敗したら全部取り消す */
inline fun <T> Connection.transaction(block: (Connection) -> T): T {
    val previous = autoCommit
    autoCommit = false
    try {
        val result = block(this)
        commit()
        return result
    } catch (e: Exception) {
        rollback()
        throw e
    } finally {
        autoCommit = previous
    }
}
