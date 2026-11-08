package com.github.kotyabuchi.realisticsurvival.skill

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.floorDigits
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.util.UUID

/** 詠唱コマンド（L=左クリック, R=右クリック） */
enum class SkillCommand { RRR, RRL, RLR, LRR, LLL, LLR, LRL, RLL }

/**
 * すべてのスキルの基本。発動するとレベル不足とクールタイムを確認してから onActivate を呼ぶ。
 * スキル自身が Listener なので、必要なイベントはサブクラスに @EventHandler で書く。
 */
abstract class Skill(protected val plugin: RealisticSurvival) : Listener {

    /** 保存用のID。プレイヤーのPDCのキーになるので、変えると習得・オン状態が消える */
    abstract val id: String
    abstract val displayName: String
    abstract val needLevel: Int
    abstract val description: String
    open val coolTimeMillis: Long = 0

    private val lastUse = HashMap<UUID, Long>()

    // NamespacedKey は小文字に変換される（旧版と同じキー "realisticsurvival:mine_assist" など）
    val key: NamespacedKey by lazy { NamespacedKey(plugin, id) }

    fun remainingCoolTime(uuid: UUID): Long =
        lastUse[uuid]?.let { coolTimeMillis - (System.currentTimeMillis() - it) } ?: 0

    fun isReady(uuid: UUID): Boolean = remainingCoolTime(uuid) <= 0

    protected fun markUsed(uuid: UUID) {
        lastUse[uuid] = System.currentTimeMillis()
    }

    open fun activate(player: Player, level: Int) {
        if (!checkUsable(player, level)) return
        onActivate(player, level)
        markUsed(player.uniqueId)
    }

    protected open fun onActivate(player: Player, level: Int) {
        player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        player.sendActionBar(Component.text(displayName, NamedTextColor.GREEN))
    }

    /** レベル不足・クールタイム中ならメッセージを出して false */
    protected fun checkUsable(player: Player, level: Int): Boolean = when {
        level < needLevel -> {
            fail(player, "$displayName: Not enough levels (Need Lv.$needLevel)")
            false
        }
        !isReady(player.uniqueId) -> {
            fail(player, "$displayName: Not yet (${(remainingCoolTime(player.uniqueId) / 1000.0).floorDigits(1)}s)")
            false
        }
        else -> true
    }

    protected fun fail(player: Player, message: String) {
        player.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f)
        player.sendActionBar(Component.text(message, NamedTextColor.RED))
    }
}
