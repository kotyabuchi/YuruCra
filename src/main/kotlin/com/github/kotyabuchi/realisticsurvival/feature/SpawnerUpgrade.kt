package com.github.kotyabuchi.realisticsurvival.feature

import com.github.kotyabuchi.realisticsurvival.util.floorDigits
import com.github.kotyabuchi.realisticsurvival.util.sendSuccess
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.CreatureSpawner
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

/**
 * スポナーを右クリックして強化する。
 * - ダイヤ1個: スポーン間隔を短くする（最短0.5秒、最長2秒ずつ短縮。最短は1秒まで）
 * - アメジストの欠片10個: 周囲の最大モンスター数を+2（50体まで）
 */
class SpawnerUpgrade : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val spawner = block.state as? CreatureSpawner ?: return
        val hand = event.hand ?: return
        val player = event.player
        val item = player.inventory.getItem(hand)

        when (item.type) {
            Material.DIAMOND -> {
                if (spawner.minSpawnDelay <= MIN_DELAY_LIMIT) return
                item.amount--
                spawner.maxSpawnDelay -= 40
                spawner.minSpawnDelay -= 10
                player.sendSuccess("最長スポーン時間 -2秒(${spawner.maxSpawnDelay / 20}秒)")
                player.sendSuccess("最短スポーン時間 -0.5秒(${(spawner.minSpawnDelay / 20.0).floorDigits(1)}秒)")
            }
            Material.AMETHYST_SHARD -> {
                if (spawner.maxNearbyEntities >= MAX_NEARBY_LIMIT) return
                if (item.amount < AMETHYST_COST) return
                item.amount -= AMETHYST_COST
                spawner.maxNearbyEntities += 2
                player.sendSuccess("最大モンスター数 +2体(${spawner.maxNearbyEntities}体)")
            }
            else -> return
        }
        event.isCancelled = true
        spawner.update()
        block.world.playSound(block.location, Sound.BLOCK_ANVIL_USE, 0.8f, 0.7f)
    }

    companion object {
        private const val MIN_DELAY_LIMIT = 20
        private const val MAX_NEARBY_LIMIT = 50
        private const val AMETHYST_COST = 10
    }
}
