package com.github.kotyabuchi.yurucra.job

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.player.JobStatus
import com.github.kotyabuchi.yurucra.skill.ToggleSkill
import com.github.kotyabuchi.yurucra.util.floorDigits
import com.github.kotyabuchi.yurucra.util.runTimer
import com.github.kotyabuchi.yurucra.util.sendSuccess
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.Duration
import java.util.EnumMap
import java.util.UUID

/**
 * ジョブ経験値の付与・レベルアップ通知・経験値バーの表示。
 *
 * 経験値バーは「最後に経験値を得てから6秒」で消え、同時にコンボがリセットされる。
 * 経験値を得るたびにタスクを作り直すと、一括破壊×人数分のタスクが生まれるので、
 * 消す時刻だけを記録して、1つのタイマーでまとめて確認する。
 */
class JobProgress(private val plugin: YuruCra) {

    private class ShownBar(val bar: BossBar, val status: JobStatus) {
        var hideAtTick: Int = 0
    }

    private val bars = HashMap<UUID, EnumMap<JobType, ShownBar>>()

    init {
        plugin.runTimer(CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS) { hideExpiredBars() }
    }

    fun addExp(player: Player, job: Job, point: Double, increaseCombo: Int = 1) {
        val status = plugin.players.status(player).job(job.type)
        val levelBefore = status.level
        if (status.addExp(point, increaseCombo)) onLevelUp(player, job, levelBefore, status.level)
        showBar(player, job, status)
    }

    /** 経験値バーを全員から消す（プラグイン停止時） */
    fun clearAll() {
        plugin.server.onlinePlayers.forEach { clear(it) }
    }

    fun clear(player: Player) {
        bars.remove(player.uniqueId)?.values?.forEach { player.hideBossBar(it.bar) }
    }

    private fun onLevelUp(player: Player, job: Job, from: Int, to: Int) {
        job.onLevelUp(player)
        player.showTitle(
            Title.title(
                Component.text("Level Up!"),
                Component.text("${job.type.displayName} Lv. $to"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500)),
            )
        )
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 1.0f)

        // 一度に複数レベル上がっても、間のレベルで習得するスキルを取りこぼさない
        job.allSkills.filter { it.needLevel in (from + 1)..to }.forEach { skill ->
            if (skill is ToggleSkill) skill.activate(player, to)
            player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
            player.sendSuccess("[${skill.displayName}]を習得しました")
        }
    }

    private fun showBar(player: Player, job: Job, status: JobStatus) {
        val title = Component.text("${job.type.displayName} Lv.${status.level} ${status.exp.floorDigits(2)}/${status.nextLevelExp} ")
            .append(Component.text("+${status.recentAddedExp.floorDigits(2)} ", NamedTextColor.GREEN))
            .append(Component.text("${status.combo}Combo(x${status.comboMultiplier.floorDigits(3)})", NamedTextColor.GOLD))
        val progress = (status.exp / status.nextLevelExp).toFloat().coerceIn(0f, 1f)

        val shown = bars.getOrPut(player.uniqueId) { EnumMap(JobType::class.java) }.getOrPut(job.type) {
            ShownBar(BossBar.bossBar(title, progress, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10), status)
        }
        shown.bar.name(title)
        shown.bar.progress(progress)
        player.showBossBar(shown.bar)
        shown.hideAtTick = plugin.server.currentTick + BAR_HIDE_TICKS
    }

    private fun hideExpiredBars() {
        val now = plugin.server.currentTick
        val emptyPlayers = mutableListOf<UUID>()
        bars.forEach { (uuid, jobBars) ->
            val player = plugin.server.getPlayer(uuid)
            jobBars.values.removeIf { shown ->
                if (player != null && shown.hideAtTick > now) return@removeIf false
                shown.status.resetCombo()
                player?.hideBossBar(shown.bar)
                true
            }
            if (jobBars.isEmpty()) emptyPlayers += uuid
        }
        emptyPlayers.forEach { bars.remove(it) }
    }

    companion object {
        private const val BAR_HIDE_TICKS = 20 * 6
        private const val CHECK_INTERVAL_TICKS = 10L
    }
}
