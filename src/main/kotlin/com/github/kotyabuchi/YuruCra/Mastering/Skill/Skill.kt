package com.github.kotyabuchi.YuruCra.Mastering.Skill

import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus
import com.github.kotyabuchi.YuruCra.Utility.floor1Digits
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.event.Listener
import java.util.*

interface Skill: Listener {

    val main: Main

    val skillName: String
    val displayName: String
    val cost: Int
    val needLevel: Int
    val description: String
    val coolTime: Long
    val lastUseTime: MutableMap<UUID, Long>
    fun getSkillKey(): NamespacedKey = NamespacedKey(main, skillName)

    fun enableSkill(player: PlayerStatus, level: Int) {
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
            else -> {
                enableAction(player, level)
                setLastUseTime(uuid)
            }
//            playerStatus.decreaseMana(cost) -> {
//                enableAction(player, level)
//                setLastUseTime(uuid)
//            }
//            else -> {
//                sendErrorMessage(player, Component.text("$displayName: Not enough mana ").color(NamedTextColor.RED)
//                    .append(Component.text("(").color(NamedTextColor.WHITE))
//                    .append(Component.text("${Emoji.DIAMOND}$cost").color(NamedTextColor.AQUA))
//                    .append(Component.text(")").color(NamedTextColor.WHITE)))
//            }
        }
    }

    fun enableAction(player: PlayerStatus, level: Int) {
        player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        player.sendActionBar(Component.text(displayName, NamedTextColor.GREEN))
    }

    fun setLastUseTime(uuid: UUID) {
        lastUseTime[uuid] = System.currentTimeMillis()
    }

    fun getRemainingCoolTime(uuid: UUID): Long {
        return lastUseTime[uuid]?.let { coolTime - (System.currentTimeMillis() - it) } ?: 0
    }

    fun isReadySkill(uuid: UUID): Boolean {
        return getRemainingCoolTime(uuid) <= 0L
    }

    fun sendErrorMessage(player: PlayerStatus, message: Component) {
        player.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f)
        player.sendActionBar(message)
    }
}