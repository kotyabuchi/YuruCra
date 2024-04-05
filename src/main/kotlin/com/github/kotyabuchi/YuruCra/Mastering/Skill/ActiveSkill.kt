package com.github.kotyabuchi.YuruCra.Mastering.Skill

import com.github.kotyabuchi.YuruCra.Player.PlayerStatus
import com.github.kotyabuchi.YuruCra.Utility.floor1Digits
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

interface ActiveSkill: ToggleSkill {
    val hasActiveTime: Boolean
    val activeTimeMap: MutableMap<UUID, BukkitTask>

    fun calcActiveTime(level: Int): Int = 0

    override fun enableSkill(player: PlayerStatus, level: Int) {
        val uuid = player.uniqueId
        when {
            level < needLevel -> {
                player.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f)
                sendErrorMessage(player, Component.text("$displayName: Not enough levels (Need Lv.$needLevel)").color(
                    NamedTextColor.RED))
            }
            !isReadySkill(uuid) -> {
                player.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f)
                sendErrorMessage(player, Component.text("$displayName: Not yet (${(getRemainingCoolTime(uuid) / 1000.0).floor1Digits()}s)").color(NamedTextColor.RED))
            }
//            player.decreaseMana(cost) -> {
//                enableAction(player, level)
//                setLastUseTime(uuid)
//                setSkillLevel(player, level)
//                if (hasActiveTime) restartActiveTime(player, level)
//            }
            else -> {
//                sendErrorMessage(player, Component.text("$displayName: Not enough mana ").color(NamedTextColor.RED)
//                    .append(Component.text("(").color(NamedTextColor.WHITE))
//                    .append(Component.text("${Emoji.DIAMOND}$cost").color(NamedTextColor.AQUA))
//                    .append(Component.text(")").color(NamedTextColor.WHITE)))
            }
        }
    }

    override fun disableSkill(player: PlayerStatus) {
        val uuid = player.uniqueId
        disableAction(player)
        activeTimeMap[uuid]?.cancel()
        activeTimeMap.remove(uuid)
        removeSkillLevel(player)
    }

    fun restartActiveTime(player: PlayerStatus, level: Int) {
        val uuid = player.uniqueId
        activeTimeMap[uuid]?.cancel()
        activeTimeMap[uuid] = object : BukkitRunnable() {
            override fun run() {
                disableSkill(player)
            }
        }.runTaskLater(main, calcActiveTime(level).toLong())
    }
}