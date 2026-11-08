package com.github.kotyabuchi.realisticsurvival.job

import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/** 採集系ジョブの対象ブロックを壊したとき（パッシブスキルのフック用） */
class GatheringEvent(player: Player, val job: GatheringJob, val block: Block) : PlayerEvent(player) {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/** Multi Break でまとめて壊した直後（Tunnel Assist のフック用） */
class MultiBreakEvent(
    player: Player,
    val centerBlock: Block,
    val face: BlockFace,
    val job: Job,
) : PlayerEvent(player) {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
