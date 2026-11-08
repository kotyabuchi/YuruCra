package com.github.kotyabuchi.realisticsurvival.block

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.TickScopedMap
import com.github.kotyabuchi.realisticsurvival.util.isSkillBreakable
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
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

    private class ExpBatch {
        var exp = 0
    }

    private val active = HashMap<UUID, BreakSource>()
    // 一括破壊中に出た経験値の合計（入れ子になっても一番外側の1つだけを使う）
    private val expBatches = HashMap<UUID, ExpBatch>()
    // 起点ブロック → その破壊イベントの最後で出す経験値（同じtick内で使い切る）
    private val pendingOriginExp = TickScopedMap<Block, ExpBatch>(plugin.server)
    // スキルの起点になったブロック。そのブロック自身のドロップ（同じtick内）でも参照できるようにする
    private val origins = TickScopedMap<Block, BreakSource>(plugin.server)

    /** 今まとめて壊している最中なら、その情報 */
    fun current(player: Player): BreakSource? = active[player.uniqueId]

    /** block の破壊がスキルによるものか（起点のブロックも含む） */
    fun sourceOf(player: Player, block: Block): BreakSource? = active[player.uniqueId] ?: origins[block]

    fun tagOrigin(block: Block, source: BreakSource) {
        origins[block] = source
    }

    /**
     * blocks をプレイヤーが壊したことにして順番に壊す。
     * 途中で道具が壊れる（持ち替わる）と、そこで止める。
     *
     * 壊したブロックの経験値は、起点のブロック（originEvent）の分も含めて合計し、
     * mainBlock の位置に経験値オーブ1つとして出す。
     * スキルの中でさらに一括破壊が起きた（入れ子になった）ときは、一番外側でまとめて1つにする。
     *
     * @param originEvent プレイヤーが実際に壊したブロックの BlockBreakEvent（その経験値もまとめる）
     * @return 実際に壊せた数
     */
    fun breakBlocks(player: Player, source: BreakSource, blocks: Collection<Block>, originEvent: BlockBreakEvent? = null): Int {
        val uuid = player.uniqueId
        val previous = active[uuid]
        active[uuid] = source
        val isOutermost = uuid !in expBatches
        if (isOutermost) expBatches[uuid] = ExpBatch()
        val toolType = player.inventory.itemInMainHand.type
        var count = 0
        var firstSound: Pair<Block, BlockData>? = null
        try {
            for (block in blocks) {
                if (player.inventory.itemInMainHand.type != toolType) break
                if (!block.isSkillBreakable()) continue
                val data = block.blockData
                if (player.breakBlock(block)) {
                    count++
                    block.world.spawnParticle(Particle.BLOCK, block.location.toCenterLocation(), 20, 0.3, 0.3, 0.3, 0.0, data)
                    if (firstSound == null) firstSound = block to data
                }
            }
        } finally {
            if (previous == null) active.remove(uuid) else active[uuid] = previous
            if (isOutermost) {
                val exp = expBatches.remove(uuid)?.exp ?: 0
                if (originEvent != null) {
                    // 同じブロックの破壊で複数のスキル（Multi Break と Mine Assist など）が動くことがあるので、
                    // 起点ブロックの破壊イベントの最後（onBreak）でまとめて1つにする
                    val pending = pendingOriginExp[originEvent.block]
                    if (pending == null) pendingOriginExp[originEvent.block] = ExpBatch().also { it.exp = exp } else pending.exp += exp
                } else {
                    spawnExpOrb(source.mainBlock, exp)
                }
            }
        }
        // 本人が壊したブロック以外は本人の画面に破壊エフェクトが出ないので自分で出す。
        // 音はまとめて1回にする（数十ブロック分を同時に鳴らすと、周りのプレイヤーにもうるさく負荷もかかるため）
        firstSound?.let { (block, data) ->
            block.world.playSound(block.location.toCenterLocation(), data.soundGroup.breakSound, 1f, 0.75f)
        }
        return count
    }

    /**
     * - まとめて壊しているブロック: 経験値をその場に出さず合計へ回す
     * - スキルの起点になったブロック: 合計した経験値に自分の分を足して、オーブ1つにして出す
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBreak(event: BlockBreakEvent) {
        val uuid = event.player.uniqueId
        if (uuid in active) {
            if (event.isCancelled) return
            val batch = expBatches[uuid] ?: return
            batch.exp += event.expToDrop
            event.expToDrop = 0
            return
        }
        val pending = pendingOriginExp.remove(event.block) ?: return
        var exp = pending.exp
        // 起点の破壊が他のプラグインに取り消されても、まとめて壊した分は壊れているので、その経験値は出す
        if (!event.isCancelled) {
            exp += event.expToDrop
            event.expToDrop = 0
        }
        spawnExpOrb(event.block, exp)
    }

    private fun spawnExpOrb(at: Block, exp: Int) {
        if (exp <= 0) return
        at.world.spawn(at.location.toCenterLocation(), ExperienceOrb::class.java) { it.experience = exp }
    }

    /**
     * まとめて壊したブロックのドロップを、実際に壊したブロックの位置に集める。
     * ドロップを書き換える他の処理（経験値・Stone Replacer）が済んだ後に動く。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDrop(event: BlockDropItemEvent) {
        val source = active[event.player.uniqueId] ?: return
        if (event.block == source.mainBlock || event.items.isEmpty()) return
        val location = source.mainBlock.location.toCenterLocation()
        val stacks = event.items.map { it.itemStack }
        event.items.clear()
        stacks.forEach { location.world.dropItem(location, it) }
    }
}
