package com.github.kotyabuchi.yurucra.skill.gathering

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.block.BreakSource
import com.github.kotyabuchi.yurucra.skill.ToggleSkill
import com.github.kotyabuchi.yurucra.util.ToolType
import com.github.kotyabuchi.yurucra.util.isOre
import com.github.kotyabuchi.yurucra.util.neighbors27
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import kotlin.math.ceil
import kotlin.math.max

/** 鉱石をツルハシで壊すと、つながった鉱石もまとめて掘る */
class MineAssist(plugin: YuruCra) : ToggleSkill(plugin) {
    override val id = "MINE_ASSIST"
    override val displayName = "Mine Assist"
    override val needLevel = 0
    override val description = "鉱石を破壊した際に繋がった鉱石もまとめて採掘する"

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        val context = plugin.breakContext.current(player)
        if (context?.isMineAssist == true) return
        if (!isEnabled(player)) return
        val block = event.block
        if (!ToolType.PICKAXE.includes(player.inventory.itemInMainHand)) return
        if (!block.type.isOre()) return

        val ores = findConnectedOres(block)
        ores.remove(block)
        val source = BreakSource(block, isMultiBreak = context?.isMultiBreak ?: false, isMineAssist = true)
        plugin.breakContext.tagOrigin(block, source)
        val broken = plugin.breakContext.breakBlocks(player, source, ores, originEvent = event)
        player.foodLevel = max(0, player.foodLevel - ceil(broken / 10.0).toInt())
    }

    /** start からつながっている鉱石を幅優先で探す（斜めも含む3x3x3） */
    private fun findConnectedOres(start: Block): LinkedHashSet<Block> {
        val found = linkedSetOf(start)
        val queue = ArrayDeque(listOf(start))
        val checked = hashSetOf(start)
        while (queue.isNotEmpty() && checked.size < SEARCH_LIMIT) {
            val current = queue.removeFirst()
            for (next in current.neighbors27()) {
                if (!checked.add(next)) continue
                if (!next.type.isOre()) continue
                found += next
                queue += next
            }
        }
        return found
    }

    companion object {
        private const val SEARCH_LIMIT = 496
    }
}
