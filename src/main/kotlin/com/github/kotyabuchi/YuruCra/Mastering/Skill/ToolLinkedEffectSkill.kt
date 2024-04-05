package com.github.kotyabuchi.YuruCra.Mastering.Skill

import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

abstract class ToolLinkedEffectSkill: PassiveSkill {
    abstract val targetToolType: Set<Material>

    abstract fun applyEffect(player: Player)

    fun startCycle() {
        object : BukkitRunnable() {
            override fun run() {
                main.server.onlinePlayers.map { it.getStatus() }.forEach { player ->
                    val item = player.inventory.itemInMainHand
                    if (isEnabledSkill(player) && targetToolType.contains(item.type)) applyEffect(player)
                }
            }
        }.runTaskTimer(main, 0, 20)
    }
}