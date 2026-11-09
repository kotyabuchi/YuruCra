package com.github.kotyabuchi.yurucra.skill.gathering

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.block.BreakSource
import com.github.kotyabuchi.yurucra.job.Job
import com.github.kotyabuchi.yurucra.skill.ToolLinkedSkill
import com.github.kotyabuchi.yurucra.util.ToolType
import com.github.kotyabuchi.yurucra.util.WoodType
import com.github.kotyabuchi.yurucra.util.isLeaves
import com.github.kotyabuchi.yurucra.util.isTreeSoil
import com.github.kotyabuchi.yurucra.util.isWood
import com.github.kotyabuchi.yurucra.util.neighbors27
import com.github.kotyabuchi.yurucra.util.runLater
import com.github.kotyabuchi.yurucra.util.woodType
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Leaves
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.persistence.PersistentDataType
import kotlin.math.ceil
import kotlin.math.max

/**
 * 発動後6秒間、原木を斧で壊すと木全体をまとめて伐採する。葉は下から順に落ちてくる。
 * 斧を持ち替えるとオフになる。
 */
class TreeAssist(plugin: YuruCra, private val owner: Job) : ToolLinkedSkill(plugin) {
    override val id = "TREE_ASSIST"
    override val displayName = "Tree Assist"
    override val needLevel = 0
    override val description = "原木を破壊した際に繋がった原木もまとめて伐採する"

    private val fallingLeafKey = NamespacedKey(plugin, "tree_assist_leaf")

    override fun activeTicks(level: Int): Long = 20 * 6L

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        if (plugin.breakContext.current(player) != null) return
        val block = event.block
        if (!block.type.isWood()) return
        if (!ToolType.AXE.includes(player.inventory.itemInMainHand)) return
        if (!isEnabled(player)) return

        restartActiveTime(player, owner.level(player))

        val woodType = block.type.woodType()
        val root = findRoot(block, woodType) ?: return
        val (woods, edgeLeaves) = collectWoods(root, woodType)
        woods.remove(block)
        val leaves = collectLeaves(edgeLeaves)

        val broken = plugin.breakContext.breakBlocks(player, BreakSource(block), woods, originEvent = event)
        player.foodLevel = max(0, player.foodLevel - ceil(broken / 20.0).toInt())
        dropLeaves(leaves)
    }

    /** 落ちてきた葉が着地したら、葉のドロップ（苗木など）を出して消える */
    @EventHandler
    fun onLeafLand(event: EntityChangeBlockEvent) {
        val falling = event.entity as? FallingBlock ?: return
        if (!falling.persistentDataContainer.has(fallingLeafKey, PersistentDataType.BYTE)) return
        event.isCancelled = true
        val block = event.block
        val original = block.blockData
        // 葉として一度置いてからドロップを計算する（素手で壊したときのドロップ）
        block.setBlockData(falling.blockData, false)
        val drops = block.drops
        block.setBlockData(original, false)
        drops.forEach { falling.world.dropItem(falling.location, it) }
        falling.remove()
    }

    /** 葉を高さごとにまとめ、下の段から1tickずつずらして落とす（1段につきタスク1つ） */
    private fun dropLeaves(leaves: Collection<Block>) {
        if (leaves.isEmpty()) return
        val lowest = leaves.minOf { it.y }
        leaves.groupBy { it.y }.forEach { (y, layer) ->
            plugin.runLater((y - lowest).toLong()) {
                layer.forEach { leaf ->
                    if (!leaf.type.isLeaves()) return@forEach
                    val falling = leaf.world.spawnFallingBlock(leaf.location.toCenterLocation(), leaf.blockData)
                    falling.dropItem = false
                    falling.isPersistent = false
                    falling.persistentDataContainer.set(fallingLeafKey, PersistentDataType.BYTE, 1)
                    leaf.setType(Material.AIR, false)
                }
            }
        }
    }

    /** 壊した原木から近い順にたどって、土の上に立っている根元の原木を探す */
    private fun findRoot(start: Block, woodType: WoodType): Block? {
        val queue = ArrayDeque(listOf(start))
        val checked = hashSetOf(start)
        while (queue.isNotEmpty() && checked.size < SEARCH_LIMIT) {
            val current = queue.removeFirst()
            if (current.getRelative(BlockFace.DOWN).type.isTreeSoil()) return current
            for (next in current.neighbors27()) {
                // 根元は下にあるので、上方向へはたどらない
                if (next.y > current.y || !checked.add(next)) continue
                if (next.isWoodOf(woodType)) queue += next
            }
        }
        return null
    }

    /**
     * 根元から上につながる原木を集める。
     * あわせて、原木に接している葉（葉をたどる起点）も返す。
     */
    private fun collectWoods(root: Block, woodType: WoodType): Pair<MutableList<Block>, List<Block>> {
        val woods = mutableListOf(root)
        val edgeLeaves = mutableListOf<Block>()
        val queue = ArrayDeque(listOf(root))
        val checked = hashSetOf(root)
        while (queue.isNotEmpty() && checked.size < SEARCH_LIMIT) {
            val current = queue.removeFirst()
            for (next in current.neighbors27()) {
                if (next.y < root.y || !checked.add(next)) continue
                when {
                    next.isWoodOf(woodType) -> {
                        woods += next
                        queue += next
                    }
                    next.type.isLeaves() -> edgeLeaves += next
                }
            }
        }
        return woods to edgeLeaves
    }

    /**
     * 原木から遠ざかる方向（葉の distance が増える方向）にだけ葉をたどる。
     * プレイヤーが置いた葉（persistent）と、別の種類の木の葉は対象外。
     */
    private fun collectLeaves(edgeLeaves: List<Block>): Set<Block> {
        val leaves = LinkedHashSet<Block>()
        val queue = ArrayDeque<Block>()
        edgeLeaves.filter { it.isNaturalLeaf() }.forEach { if (leaves.add(it)) queue += it }
        while (queue.isNotEmpty() && leaves.size < SEARCH_LIMIT) {
            val current = queue.removeFirst()
            val currentData = current.blockData as Leaves
            val woodType = current.type.woodType()
            for (next in current.neighbors27()) {
                if (next in leaves || !next.isNaturalLeaf() || next.type.woodType() != woodType) continue
                if ((next.blockData as Leaves).distance <= currentData.distance) continue
                leaves += next
                queue += next
            }
        }
        return leaves
    }

    private fun Block.isWoodOf(woodType: WoodType): Boolean = type.isWood() && type.woodType() == woodType

    private fun Block.isNaturalLeaf(): Boolean {
        if (!type.isLeaves()) return false
        val data = blockData as? Leaves ?: return false
        return !data.isPersistent
    }

    companion object {
        private const val SEARCH_LIMIT = 496
    }
}
