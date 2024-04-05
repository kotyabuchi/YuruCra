package com.github.kotyabuchi.YuruCra.Mastering.Skill

import com.github.kotyabuchi.YuruCra.Mastering.Mastering
import org.bukkit.entity.Player

interface PassiveSkill: ToggleSkill {

    val ownerMastering: Mastering

    override fun enableAction(player: Player, level: Int) {}
    override fun disableAction(player: Player) {}
}