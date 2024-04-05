package com.github.kotyabuchi.YuruCra.Mastering.Skill

import com.github.kotyabuchi.YuruCra.Mastering.Mastering
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus

interface PassiveSkill: ToggleSkill {

    val ownerMastering: Mastering

    override fun enableAction(player: PlayerStatus, level: Int) {}
    override fun disableAction(player: PlayerStatus) {}
}