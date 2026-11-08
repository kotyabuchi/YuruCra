package com.github.kotyabuchi.realisticsurvival.skill.gathering

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.block.BreakSource
import com.github.kotyabuchi.realisticsurvival.job.Job
import com.github.kotyabuchi.realisticsurvival.skill.ToolLinkedSkill
import com.github.kotyabuchi.realisticsurvival.util.AROUND_FACES
import com.github.kotyabuchi.realisticsurvival.util.ToolType
import com.github.kotyabuchi.realisticsurvival.util.WoodType
import com.github.kotyabuchi.realisticsurvival.util.isLeaves
import com.github.kotyabuchi.realisticsurvival.util.isTreeSoil
import com.github.kotyabuchi.realisticsurvival.util.isWood
import com.github.kotyabuchi.realisticsurvival.util.runLater
import com.github.kotyabuchi.realisticsurvival.util.woodType
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
 * 発動後6秒間、原木を斧で壊すと木全体をまとめて伐採する。葉は上から順に落ちてくる。
 * 斧を持ち替えるとオフになる。
 */
class TreeAssist(plugin: RealisticSurvival, private val owner: Job) : ToolLinkedSkill(plugin) {
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
        val root = findRoot(block, woodType, HashSet()) ?: return
        val woods = mutableListOf<Block>()
        val leaves = LinkedHashSet<Block>()
        collectTree(root, woodType, root, woods, leaves, HashSet())
        woods.remove(block)

        val broken = plugin.breakContext.breakBlocks(player, BreakSource(block), woods)
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

    private fun dropLeaves(leaves: Collection<Block>) {
        if (leaves.isEmpty()) return
        val lowest = leaves.minOf { it.y }
        leaves.forEach { leaf ->
            plugin.runLater((leaf.y - lowest).toLong()) {
                if (!leaf.type.isLeaves()) return@runLater
                val falling = leaf.world.spawnFallingBlock(leaf.location.toCenterLocation(), leaf.blockData)
                falling.dropItem = false
                falling.isPersistent = false
                falling.persistentDataContainer.set(fallingLeafKey, PersistentDataType.BYTE, 1)
                leaf.setType(Material.AIR, false)
            }
        }
    }

    /** 下にたどって、土の上に立っている根元の原木を探す */
    private fun findRoot(check: Block, woodType: WoodType, checked: MutableSet<Block>): Block? {
        if (!checked.add(check) || checked.size > SEARCH_LIMIT) return null
        val material = check.type
        if (!material.isWood() || material.woodType() != woodType) return null
        val below = check.getRelative(BlockFace.DOWN)
        if (below.type.isTreeSoil()) return check
        for (face in AROUND_FACES) findRoot(below.getRelative(face), woodType, checked)?.let { return it }
        for (face in AROUND_FACES) findRoot(check.getRelative(face), woodType, checked)?.let { return it }
        return null
    }

    /** 根元から上につながる原木と、その木に付いている葉を集める */
    private fun collectTree(
        root: Block,
        woodType: WoodType,
        check: Block,
        woods: MutableList<Block>,
        leaves: MutableSet<Block>,
        checked: MutableSet<Block>,
    ) {
        if (!checked.add(check)) return
        if (check.y < root.y) return
        val material = check.type
        if (!material.isWood()) {
            if (material.isLeaves()) collectLeaves(check, check, material.woodType(), leaves)
            return
        }
        if (material.woodType() != woodType) return
        woods += check
        if (checked.size > SEARCH_LIMIT) return
        for (dy in intArrayOf(1, 0, -1)) {
            val layer = check.getRelative(0, dy, 0)
            for (face in AROUND_FACES) collectTree(root, woodType, layer.getRelative(face), woods, leaves, checked)
        }
    }

    /** 原木から遠ざかる方向（distance が増える方向）にだけ葉をたどる。プレイヤーが置いた葉は対象外 */
    private fun collectLeaves(before: Block, check: Block, woodType: WoodType, leaves: MutableSet<Block>) {
        if (check in leaves || leaves.size > SEARCH_LIMIT) return
        val material = check.type
        if (!material.isLeaves() || material.woodType() != woodType) return
        val data = check.blockData as? Leaves ?: return
        if (data.isPersistent) return
        val beforeData = before.blockData
        if (before != check && beforeData is Leaves && beforeData.distance >= data.distance) return
        leaves += check
        for (dy in intArrayOf(1, 0, -1)) {
            val layer = check.getRelative(0, dy, 0)
            for (face in AROUND_FACES) collectLeaves(check, layer.getRelative(face), woodType, leaves)
        }
    }

    companion object {
        private const val SEARCH_LIMIT = 496
    }
}
