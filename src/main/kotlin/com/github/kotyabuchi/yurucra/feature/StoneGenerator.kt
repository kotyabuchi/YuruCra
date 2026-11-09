package com.github.kotyabuchi.yurucra.feature

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.util.runNextTick
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent

/**
 * 溶岩と水でできる丸石を、真下のブロックに合わせた石に変える。
 * Y=15以下では常に深層岩になる。真下が対象外のブロックなら丸石のまま。
 */
class StoneGenerator(private val plugin: YuruCra) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onFlow(event: BlockFromToEvent) {
        if (event.block.type != Material.LAVA) return
        val target = event.toBlock
        // 丸石ができるのは流れた後なので、次のtickで確認する
        plugin.runNextTick {
            if (target.type != Material.COBBLESTONE) return@runNextTick
            val stone = if (target.y <= DEEPSLATE_MAX_Y) {
                Material.DEEPSLATE
            } else {
                target.getRelative(BlockFace.DOWN).type.takeIf { it in COPYABLE } ?: return@runNextTick
            }
            target.type = stone
        }
    }

    companion object {
        private const val DEEPSLATE_MAX_Y = 15
        private val COPYABLE = setOf(
            Material.DEEPSLATE, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.CALCITE, Material.TUFF,
        )
    }
}
