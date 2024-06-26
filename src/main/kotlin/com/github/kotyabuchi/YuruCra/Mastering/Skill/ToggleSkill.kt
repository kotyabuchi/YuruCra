package com.github.kotyabuchi.YuruCra.Mastering.Skill

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

interface ToggleSkill: Skill {

    fun isEnabledSkill(player: Player): Boolean = player.persistentDataContainer.has(getSkillKey(), PersistentDataType.INTEGER)

    fun setSkillLevel(player: Player, level: Int) {
        player.persistentDataContainer.set(getSkillKey(), PersistentDataType.INTEGER, level)
    }

    fun removeSkillLevel(player: Player) {
        player.persistentDataContainer.remove(getSkillKey())
    }

    fun getSkillLevel(player: Player): Int? {
        return player.persistentDataContainer.get(getSkillKey(), PersistentDataType.INTEGER)
    }

    override fun enableAction(player: Player, level: Int) {
        player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        player.sendActionBar(Component.text("$displayName On", NamedTextColor.GREEN))
    }

    fun disableAction(player: Player) {
        player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        player.sendActionBar(Component.text("$displayName Off", NamedTextColor.RED))
    }

    fun toggleSkill(player: Player, level: Int) {
        if (isEnabledSkill(player)) {
            disableSkill(player)
        } else {
            enableSkill(player, level)
        }
    }

    override fun enableSkill(player: Player, level: Int) {
        if (needLevel > level) {
            sendErrorMessage(player, Component.text("$displayName: Not enough levels (Need Lv.$needLevel)").color(NamedTextColor.RED))
        } else if (!isEnabledSkill(player)) {
            enableAction(player, level)
            setSkillLevel(player, level)
        }
    }

    fun disableSkill(player: Player) {
        disableAction(player)
        removeSkillLevel(player)
    }
}