package com.github.kotyabuchi.realisticsurvival.job

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.player.JobStatus
import com.github.kotyabuchi.realisticsurvival.skill.ToggleSkill
import com.github.kotyabuchi.realisticsurvival.util.floorDigits
import com.github.kotyabuchi.realisticsurvival.util.runLater
import com.github.kotyabuchi.realisticsurvival.util.sendSuccess
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.EnumMap
import java.util.UUID

/** ジョブ経験値の付与・レベルアップ通知・経験値バーの表示 */
class JobProgress(private val plugin: RealisticSurvival) {

    private class ShownBar(val bar: BossBar) {
        var hideTask: BukkitTask? = null
    }

    private val bars = HashMap<UUID, EnumMap<JobType, ShownBar>>()

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
        bars.remove(player.uniqueId)?.values?.forEach {
            it.hideTask?.cancel()
            player.hideBossBar(it.bar)
        }
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
            ShownBar(BossBar.bossBar(title, progress, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10))
        }
        shown.bar.name(title)
        shown.bar.progress(progress)
        player.showBossBar(shown.bar)

        // しばらく経験値を得なければ、バーを消してコンボをリセットする
        shown.hideTask?.cancel()
        shown.hideTask = plugin.runLater(BAR_HIDE_TICKS) {
            status.resetCombo()
            player.hideBossBar(shown.bar)
        }
    }

    companion object {
        private const val BAR_HIDE_TICKS = 20 * 6L
    }
}
