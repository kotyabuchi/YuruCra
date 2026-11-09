package com.github.kotyabuchi.yurucra.block

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.util.runTimer
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent

/**
 * 置いたばかりのブロックを覚えておく。
 * 「置いてすぐ壊す」を繰り返してジョブ経験値を稼げないようにするため。
 */
class PlacedBlockTracker(private val plugin: YuruCra) : Listener {

    private val placedAt = HashMap<Block, Long>()

    init {
        plugin.runTimer(20 * 60L, 20 * 60L) {
            val now = System.currentTimeMillis()
            placedAt.values.removeIf { it <= now }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        // 経験値の対象でないブロックは記録しない（建築が多いと記録がたまるため）
        if (!plugin.jobs.isGatheringTarget(event.block.type)) return
        placedAt[event.block] = System.currentTimeMillis() + PROTECT_MILLIS
    }

    /** 最近置かれたブロックなら true を返し、記録を消す */
    fun consumeRecentlyPlaced(block: Block): Boolean {
        val until = placedAt.remove(block) ?: return false
        return until > System.currentTimeMillis()
    }

    companion object {
        private const val PROTECT_MILLIS = 60_000L
    }
}
