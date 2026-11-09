package com.github.kotyabuchi.yurucra.skill.gathering

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.block.BreakSource
import com.github.kotyabuchi.yurucra.job.Job
import com.github.kotyabuchi.yurucra.job.MultiBreakEvent
import com.github.kotyabuchi.yurucra.skill.ToggleSkill
import com.github.kotyabuchi.yurucra.util.ToolType
import com.github.kotyabuchi.yurucra.util.canHarvestWith
import com.github.kotyabuchi.yurucra.util.isSkillBreakable
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent

/**
 * 見ている面に沿って、壊したブロックの周囲もまとめて掘る。
 * 範囲はジョブレベル50で3x3、以降150レベルごとに一回り広がる。スニーク中は無効。
 */
class MultiBreak(
    plugin: YuruCra,
    private val owner: Job,
    override val id: String,
    private val toolType: ToolType,
) : ToggleSkill(plugin) {
    override val displayName = "Multi Break"
    override val needLevel = 50
    override val description = "周囲のブロックをまとめて採掘する"

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        if (plugin.breakContext.current(player) != null) return
        if (player.isSneaking) return
        if (!isEnabled(player)) return
        val item = player.inventory.itemInMainHand
        if (!toolType.includes(item)) return
        val block = event.block
        if (!block.isPreferredTool(item)) return

        val face = player.getTargetBlockFace(6) ?: return
        val radius = radius(owner.level(player))
        val targets = area(block, face, radius).filter { it != block && it.canHarvestWith(item) && it.isSkillBreakable() }

        plugin.breakContext.breakBlocks(player, BreakSource(block, isMultiBreak = true), targets, originEvent = event)
        plugin.server.pluginManager.callEvent(MultiBreakEvent(player, block, face, owner))
    }

    companion object {
        fun radius(level: Int): Int = if (level <= 50) 1 else (level - 50) / 150 + 1

        /**
         * 見ている面に垂直な板状の範囲。
         * 横向きの面なら足元1段下から上方向に広がる（立ったまま掘り進めやすいように）。
         */
        fun area(center: Block, face: BlockFace, radius: Int, yBottom: Int = -1, yTopExclusive: Int = radius * 2): List<Block> {
            val horizontal = -radius..radius
            val (xs, ys, zs) = when (face) {
                BlockFace.UP, BlockFace.DOWN -> Triple(horizontal, 0..0, horizontal)
                BlockFace.EAST, BlockFace.WEST -> Triple(0..0, yBottom until yTopExclusive, horizontal)
                BlockFace.NORTH, BlockFace.SOUTH -> Triple(horizontal, yBottom until yTopExclusive, 0..0)
                else -> return emptyList()
            }
            return buildList {
                for (x in xs) for (y in ys) for (z in zs) add(center.getRelative(x, y, z))
            }
        }
    }
}
