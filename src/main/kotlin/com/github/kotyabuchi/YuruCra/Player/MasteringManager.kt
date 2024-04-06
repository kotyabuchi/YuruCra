package com.github.kotyabuchi.YuruCra.Player

import com.github.kotyabuchi.MCRPG.normalize
import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Mastering.Mastering
import com.github.kotyabuchi.YuruCra.Mastering.Skill.Skill
import com.github.kotyabuchi.YuruCra.Utility.floor2Digits
import com.github.kotyabuchi.YuruCra.Utility.floor3Digits
import com.github.kotyabuchi.YuruCra.Utility.upperCamelCase
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.time.Duration

class MasteringManager(private val player: Player) {
    private val main: Main  = Main.instance
    private val masteringStatusMap = mutableMapOf<Mastering, MasteringStatus>()
    private val expBars: MutableMap<Mastering, BossBar> = mutableMapOf()
    private val expBarRunnableMap = mutableMapOf<Mastering, BukkitTask>()

    fun notifyLevelUp(mastering: Mastering) {
        val jobStatus = getMasteringStatus(mastering)
        mastering.levelUpEvent(player)
        player.showTitle(
            Title.title(
                Component.text("Level Up!"),
                Component.text("${mastering.masteringName.upperCamelCase()} Lv. ${jobStatus.getLevel()}"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
            )
        )
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 1.0f)
    }

    fun notifyLearnedSkill(skill: Skill) {
        player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        player.sendMessage(Component.text("[${skill.displayName}]を習得しました", NamedTextColor.GREEN).normalize())
    }

    fun getLevel(mastering: Mastering): Int {
        return getMasteringStatus(mastering).getLevel()
    }

    fun levelUp(mastering: Mastering, level: Int) {
        notifyLevelUp(mastering)

        mastering.getPassiveSkills().forEach {
            if (level == it.needLevel) {
                it.enableSkill(player, level)
                notifyLearnedSkill(it)
            }
        }
        mastering.getSkills().values.forEach {
            if (level == it.needLevel) {
                it.enableSkill(player, level)
                notifyLearnedSkill(it)
            }
        }
    }

    fun addExp(mastering: Mastering, point: Double, increaseCombo: Int = 1) {
        val masteringStatus = getMasteringStatus(mastering)
        if (masteringStatus.addExp(point, increaseCombo) == MasteringStatus.AddExpResult.LEVEL_UP) levelUp(mastering, masteringStatus.getLevel())
        val addedExp = masteringStatus.getRecentAddedExp()
        val combo = masteringStatus.getCombo()
        val masteringName = mastering.masteringName.upperCamelCase()

        if (expBarRunnableMap.containsKey(mastering)) {
            expBarRunnableMap[mastering]!!.cancel()
            expBarRunnableMap.remove(mastering)
        }
        val exp = masteringStatus.getExp()
        val nextLevelExp = masteringStatus.getNextLevelExp()
        var title = Component.text("$masteringName Lv.${masteringStatus.getLevel()} ${exp.floor2Digits()}/$nextLevelExp").normalize()
        title = if (addedExp > 0) {
            title.append(Component.text(" +${addedExp.floor2Digits()} ", NamedTextColor.GREEN).normalize())
        } else {
            title.append(Component.text(" +${addedExp.floor2Digits()} ", NamedTextColor.GREEN).normalize())
        }
        title = title.append(Component.text(" ${combo}Combo(x${(1 + combo * 0.002).floor3Digits()})", NamedTextColor.GOLD).normalize())

        val progress = (exp / nextLevelExp).toFloat()
        val expBar = expBars[mastering]?.apply {
            name(title)
            progress(progress)
        } ?: run {
            val expBar = BossBar.bossBar(title, progress, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10)
            expBars[mastering] = expBar
            expBar
        }
        player.showBossBar(expBar)
        expBarRunnableMap[mastering] = object : BukkitRunnable() {
            override fun run() {
                masteringStatus.resetCombo()
                player.hideBossBar(expBar)
                expBars.remove(mastering)
                expBarRunnableMap.remove(mastering)
            }
        }.runTaskLater(main, 20 * 6L)
        setMasteringStatus(mastering, masteringStatus)
    }

    fun clearExpBar() {
        player.activeBossBars().forEach {
            player.hideBossBar(it)
        }
    }

    fun setMasteringStatus(mastering: Mastering, masteringStatus: MasteringStatus) {
        masteringStatusMap[mastering] = masteringStatus
    }

    fun getMasteringStatus(mastering: Mastering): MasteringStatus {
        return masteringStatusMap[mastering] ?: MasteringStatus()
    }

    fun getAllMasteringStatus(): List<MasteringStatus> {
        return masteringStatusMap.values.toList()
    }

}