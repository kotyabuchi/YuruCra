package com.github.kotyabuchi.yurucra.feature

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent

/**
 * 重量感圧板（鉄）のエレベーター。
 * - 乗り場: ガラスか鉄ブロックの上に重量感圧板を置く
 * - 上の階の床はガラスにする（ジャンプで上、スニークで下の階へ移動）
 * 最大50ブロック先まで探す。
 */
class Elevator : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onJump(event: PlayerJumpEvent) {
        val player = event.player
        val plate = player.location.block
        if (!isStation(plate, FLOOR_ANY)) return
        // 上へたどって、最初にぶつかるブロックがガラスの床で、その上に乗り場があれば移動する
        val floor = firstBlocking(plate, BlockFace.UP) ?: return
        if (floor.type != Material.GLASS) return
        val target = floor.getRelative(BlockFace.UP)
        if (target.type == PLATE) teleport(player, target)
    }

    @EventHandler(ignoreCancelled = true)
    fun onSneak(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        val player = event.player
        val plate = player.location.block
        // 下の階へ行けるのは、ガラスの床の乗り場からだけ
        if (!isStation(plate, setOf(Material.GLASS))) return
        val glass = plate.getRelative(BlockFace.DOWN)
        val target = firstBlocking(glass, BlockFace.DOWN, stopAtPlate = true) ?: return
        if (isStation(target, FLOOR_ANY)) teleport(player, target)
    }

    private fun isStation(block: Block, floors: Set<Material>): Boolean =
        block.type == PLATE && block.getRelative(BlockFace.DOWN).type in floors

    /**
     * start の隣から direction へたどり、通り抜けられない最初のブロックを返す。
     * stopAtPlate が true なら、感圧板（通り抜けられる）も見つけた時点で返す。
     */
    private fun firstBlocking(start: Block, direction: BlockFace, stopAtPlate: Boolean = false): Block? {
        var check = start
        repeat(SEARCH_LIMIT) {
            check = check.getRelative(direction)
            if (stopAtPlate && check.type == PLATE) return check
            if (!check.isPassable) return check
        }
        return null
    }

    private fun teleport(player: Player, plate: Block) {
        val destination = player.location.clone().apply { y = plate.y.toDouble() }
        player.world.playSound(player.location, Sound.BLOCK_PISTON_EXTEND, 1f, 1.5f)
        player.teleport(destination)
        player.world.playSound(destination, Sound.BLOCK_PISTON_EXTEND, 1f, 1.5f)
    }

    companion object {
        private val PLATE = Material.HEAVY_WEIGHTED_PRESSURE_PLATE
        private val FLOOR_ANY = setOf(Material.GLASS, Material.IRON_BLOCK)
        private const val SEARCH_LIMIT = 50
    }
}
