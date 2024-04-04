package com.github.kotyabuchi.YuruCra.Event

import com.github.kotyabuchi.YuruCra.Player.PlayerStatus
import org.bukkit.Statistic
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack

class BlockMineEvent(block: Block, player: PlayerStatus, val itemStack: ItemStack, val isMainBlock: Boolean, val isMultiBreak: Boolean = false, val isMineAssist: Boolean = false): BlockBreakEvent(block, player) {

    private var cancelled = false

    override fun setCancelled(cancel: Boolean) {
        super.setCancelled(cancel)
        if (cancel) {
            if (!cancelled) {
                cancelled = true
                try {
                    player.decrementStatistic(Statistic.MINE_BLOCK, block.type)
                } catch (e: Exception) {}
            }
        } else {
            if (cancelled) {
                cancelled = false
                try {
                    player.incrementStatistic(Statistic.MINE_BLOCK, block.type)
                } catch (e: Exception) {}
            }
        }
    }

    init {
        try {
            player.incrementStatistic(Statistic.MINE_BLOCK, block.type)
        } catch (e: Exception) {}
    }
}