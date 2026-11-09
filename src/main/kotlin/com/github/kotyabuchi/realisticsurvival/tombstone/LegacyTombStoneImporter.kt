package com.github.kotyabuchi.realisticsurvival.tombstone

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.data.TombStoneRepository
import com.github.kotyabuchi.realisticsurvival.data.transaction
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.Base64
import java.util.UUID
import java.util.logging.Level

/**
 * 旧版の TombStones.json を、DB（tombstones テーブル）へ一度だけ取り込む。
 * 取り込んだら TombStones.json.imported に名前を変えるので、次回以降は何もしない。
 *
 * 旧版の形式:
 * { 持ち主UUID: { 防具立てUUID: { "Location": {"World": ワールドUUID, "X", "Y", "Z"},
 *                                 "Equipment": {装備スロット名: Base64}, "Storage": {スロット番号: Base64} } } }
 * 作成日時は保存されていないので、ファイル内の順番（後ろほど新しい）で作る。
 */
class LegacyTombStoneImporter(private val plugin: RealisticSurvival) {

    private val file = File(plugin.dataFolder, "TombStones.json")

    fun importIfExists() {
        if (!file.exists()) return
        val stones = try {
            parse(JsonParser.parseString(file.readText()).asJsonObject)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "旧版の TombStones.json を読み込めませんでした。取り込みを中止します", e)
            return
        }
        // 前回の取り込み後にファイル名の変更だけ失敗していた場合に備え、取り込み済みの墓石は飛ばす
        val imported = plugin.database.submit { conn ->
            conn.transaction {
                stones.count { (stone, items) ->
                    if (TombStoneRepository.existsEntity(conn, stone.entityId)) return@count false
                    TombStoneRepository.insert(conn, stone, items)
                    true
                }
            }
        }.join()
        if (!file.renameTo(File(plugin.dataFolder, "TombStones.json.imported"))) {
            plugin.logger.warning("TombStones.json の名前を変更できませんでした（次回起動時にもう一度確認します）")
        }
        plugin.logger.info("旧版の墓石を $imported 件取り込みました")
    }

    private fun parse(root: JsonObject): List<Pair<TombStone, ByteArray>> {
        val result = mutableListOf<Pair<TombStone, ByteArray>>()
        val baseTime = file.lastModified()
        var order = 0L
        root.entrySet().forEach { (ownerId, stonesJson) ->
            stonesJson.asJsonObject.entrySet().forEach { (entityId, stoneJson) ->
                parseStone(UUID.fromString(ownerId), UUID.fromString(entityId), stoneJson.asJsonObject, baseTime + order++)
                    ?.let { result += it }
            }
        }
        return result
    }

    private fun parseStone(owner: UUID, entityId: UUID, json: JsonObject, createdAt: Long): Pair<TombStone, ByteArray>? {
        val location = json.getAsJsonObject("Location") ?: return null
        val world = plugin.server.getWorld(UUID.fromString(location.get("World").asString))
        if (world == null) {
            plugin.logger.warning("墓石 $entityId のワールドが見つからないため、取り込みません")
            return null
        }

        val items = Array(TombStoneSlots.SIZE) { ItemStack.empty() }
        json.getAsJsonObject("Equipment")?.entrySet()?.forEach { (slotName, value) ->
            val index = when (slotName) {
                "HEAD" -> TombStoneSlots.HEAD
                "CHEST" -> TombStoneSlots.CHEST
                "LEGS" -> TombStoneSlots.LEGS
                "FEET" -> TombStoneSlots.FEET
                "OFF_HAND" -> TombStoneSlots.OFF_HAND
                else -> return@forEach
            }
            items[index] = decode(value.asString)
        }
        json.getAsJsonObject("Storage")?.entrySet()?.forEach { (slot, value) ->
            val index = slot.toIntOrNull()?.takeIf { it in 0 until TombStoneSlots.FEET } ?: return@forEach
            items[index] = decode(value.asString)
        }

        val stone = TombStone(
            id = 0,
            owner = owner,
            entityId = entityId,
            worldName = world.name,
            x = location.get("X").asDouble,
            y = location.get("Y").asDouble,
            z = location.get("Z").asDouble,
            createdAt = createdAt,
        )
        return stone to ItemStack.serializeItemsAsBytes(items)
    }

    // 旧バージョン（1.20.1）で保存したアイテムも、サーバーが今のバージョンの形式に変換して読める
    private fun decode(base64: String): ItemStack = ItemStack.deserializeBytes(Base64.getDecoder().decode(base64))
}
