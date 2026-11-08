package com.github.kotyabuchi.realisticsurvival.block

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.isSkillBreakable
import com.github.kotyabuchi.realisticsurvival.util.runNextTick
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import java.util.UUID

/**
 * スキルによる一括破壊の情報。
 * @property mainBlock プレイヤーが実際に壊したブロック。まとめて壊したブロックのドロップはここに集める
 */
data class BreakSource(
    val mainBlock: Block,
    val isMultiBreak: Boolean = false,
    val isMineAssist: Boolean = false,
)

/**
 * スキルでブロックをまとめて壊すための仕組み。
 *
 * Paper の [Player.breakBlock] で1つずつ壊すので、BlockBreakEvent / BlockDropItemEvent は通常どおり発生する
 * （保護プラグイン・ツールの耐久値・経験値の付与がそのまま効く）。
 * 壊している最中は [current] で「どのスキルによる破壊か」を参照でき、
 * 各スキルはこれを見て自分自身の連鎖を防ぐ。
 */
class BreakContext(private val plugin: RealisticSurvival) : Listener {

    private val active = HashMap<UUID, BreakSource>()
    // スキルの起点になったブロック。そのブロック自身のドロップ時にも参照できるよう1tickだけ残す
    private val origins = HashMap<Block, BreakSource>()

    /** 今まとめて壊している最中なら、その情報 */
    fun current(player: Player): BreakSource? = active[player.uniqueId]

    /** block の破壊がスキルによるものか（起点のブロックも含む） */
    fun sourceOf(player: Player, block: Block): BreakSource? = active[player.uniqueId] ?: origins[block]

    fun tagOrigin(block: Block, source: BreakSource) {
        origins[block] = source
        plugin.runNextTick { origins.remove(block) }
    }

    /**
     * blocks をプレイヤーが壊したことにして順番に壊す。
     * 途中で道具が壊れる（持ち替わる）と、そこで止める。
     * @return 実際に壊せた数
     */
    fun breakBlocks(player: Player, source: BreakSource, blocks: Collection<Block>): Int {
        val uuid = player.uniqueId
        val previous = active[uuid]
        active[uuid] = source
        val toolType = player.inventory.itemInMainHand.type
        var count = 0
        try {
            for (block in blocks) {
                if (player.inventory.itemInMainHand.type != toolType) break
                if (!block.isSkillBreakable()) continue
                val data = block.blockData
                if (player.breakBlock(block)) {
                    count++
                    playBreakEffect(block, data)
                }
            }
        } finally {
            if (previous == null) active.remove(uuid) else active[uuid] = previous
        }
        return count
    }

    /** まとめて壊したブロックのドロップを、実際に壊したブロックの位置に集める */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDrop(event: BlockDropItemEvent) {
        val source = active[event.player.uniqueId] ?: return
        if (event.block == source.mainBlock || event.items.isEmpty()) return
        val location = source.mainBlock.location.toCenterLocation()
        val stacks = event.items.map { it.itemStack }
        event.items.clear()
        stacks.forEach { location.world.dropItem(location, it) }
    }

    // 本人が壊したブロック以外は本人の画面に破壊エフェクトが出ないので、自分で出す
    private fun playBreakEffect(block: Block, data: BlockData) {
        val center = block.location.toCenterLocation()
        block.world.playSound(center, data.soundGroup.breakSound, 1f, 0.75f)
        block.world.spawnParticle(Particle.BLOCK, center, 20, 0.3, 0.3, 0.3, 0.0, data)
    }
}
