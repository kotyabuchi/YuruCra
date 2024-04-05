package com.github.kotyabuchi.YuruCra.Mastering.Skill

import com.github.kotyabuchi.YuruCra.Player.PlayerStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.persistence.PersistentDataType

interface ToggleSkill: Skill {

    fun isEnabledSkill(player: PlayerStatus): Boolean = player.persistentDataContainer.has(getSkillKey(), PersistentDataType.INTEGER)

    fun setSkillLevel(player: PlayerStatus, level: Int) {
        player.persistentDataContainer.set(getSkillKey(), PersistentDataType.INTEGER, level)
    }

    fun removeSkillLevel(player: PlayerStatus) {
        player.persistentDataContainer.remove(getSkillKey())
    }

    fun getSkillLevel(player: PlayerStatus): Int? {
        return player.persistentDataContainer.get(getSkillKey(), PersistentDataType.INTEGER)
    }

    override fun enableAction(player: PlayerStatus, level: Int) {
        player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        player.sendActionBar(Component.text("$displayName On", NamedTextColor.GREEN))
    }

    fun disableAction(player: PlayerStatus) {
        player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        player.sendActionBar(Component.text("$displayName Off", NamedTextColor.RED))
    }

    fun toggleSkill(player: PlayerStatus, level: Int) {
        if (isEnabledSkill(player)) {
            disableSkill(player)
        } else {
            enableSkill(player, level)
        }
    }

    override fun enableSkill(player: PlayerStatus, level: Int) {
        if (needLevel > level) {
            sendErrorMessage(player, Component.text("$displayName: Not enough levels (Need Lv.$needLevel)").color(NamedTextColor.RED))
        } else if (!isEnabledSkill(player)) {
            enableAction(player, level)
            setSkillLevel(player, level)
        }
    }

    fun disableSkill(player: PlayerStatus) {
        disableAction(player)
        removeSkillLevel(player)
    }
}