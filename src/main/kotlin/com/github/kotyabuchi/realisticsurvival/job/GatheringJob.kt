package com.github.kotyabuchi.realisticsurvival.job

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.TickScopedMap
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import java.util.UUID
import kotlin.random.Random

/**
 * 採集系ジョブの共通処理。対象ブロックを壊してアイテムがドロップしたときに経験値を与える。
 * レベルに応じてドロップが増える（レベル÷3 %の確率で+1個。100%を超えた分は確定で増える）。
 */
abstract class GatheringJob(
    plugin: RealisticSurvival,
    type: JobType,
    /** 素手（ジョブの道具以外）でも経験値を得られるか */
    private val canGetExpWithHand: Boolean = true,
) : Job(plugin, type) {

    private val expMap = HashMap<Material, Int>()
    // 壊されてドロップ待ちのブロック → 壊したプレイヤー（ドロップは同じtick内に発生するので、tickが変われば捨ててよい）
    private val pending = TickScopedMap<Block, UUID>(plugin.server)

    protected fun addExp(exp: Int, materials: Iterable<Material>) {
        materials.forEach { expMap[it] = exp }
    }

    protected fun addExp(exp: Int, vararg materials: Material) = addExp(exp, materials.asIterable())

    fun isTargetMaterial(material: Material): Boolean = material in expMap

    fun isTargetBlock(block: Block): Boolean = isTargetMaterial(block.type)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        if (!canGetExpWithHand && !isJobTool(player.inventory.itemInMainHand.type)) return
        if (!isTargetBlock(event.block)) return
        pending[event.block] = player.uniqueId
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBreakResult(event: BlockBreakEvent) {
        val block = event.block
        if (pending[block] != event.player.uniqueId) return
        if (event.isCancelled) {
            pending.remove(block)
            return
        }
        plugin.server.pluginManager.callEvent(GatheringEvent(event.player, this, block))
    }

    // ドロップを書き換える処理の順番は CLAUDE.md の「BlockDropItemEvent の優先度」を参照
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onDrop(event: BlockDropItemEvent) {
        val player = event.player
        if (pending.remove(event.block) != player.uniqueId) return
        if (plugin.placedBlocks.consumeRecentlyPlaced(event.block)) return

        val chance = level(player) / 3
        var dropMultiplier = 1 + chance / 100
        if (Random.nextInt(100) < chance % 100) {
            dropMultiplier++
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.0f)
        }

        val expPerItem = expMap[event.blockState.type] ?: 1
        var exp = 0.0
        event.items.forEach { item ->
            val stack = item.itemStack
            stack.amount *= dropMultiplier
            item.itemStack = stack
            exp += expPerItem * stack.amount
        }
        plugin.progress.addExp(player, this, exp, dropMultiplier)
    }
}
