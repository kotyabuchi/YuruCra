package com.github.kotyabuchi.YuruCra.Event

import com.github.kotyabuchi.YuruCra.Event.MasteringActionEvent.MasteringActionEvent
import com.github.kotyabuchi.YuruCra.Mastering.GatheringMastering
import org.bukkit.block.Block
import org.bukkit.entity.Player

class GatheringEvent(player: Player, gatheringMastering: GatheringMastering, val block: Block): MasteringActionEvent(player, gatheringMastering) {
}