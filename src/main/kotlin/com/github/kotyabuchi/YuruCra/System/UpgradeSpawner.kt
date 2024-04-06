package com.github.kotyabuchi.YuruCra.System

import com.github.kotyabuchi.YuruCra.Utility.floor1Digits
import com.github.kotyabuchi.YuruCra.Utility.sendErrorMessage
import com.github.kotyabuchi.YuruCra.Utility.sendSuccessMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.CreatureSpawner
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

object UpgradeSpawner: Listener {

    @EventHandler
    fun onUpgrade(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock ?: return
        val spawner = block.state as? CreatureSpawner ?: return
        val hand = event.hand ?: return
        val item = player.inventory.getItem(hand)

        when (item.type) {
            Material.DIAMOND -> {
                if (spawner.minSpawnDelay <= 20) {
                    player.sendErrorMessage("""
                    既に最大まで強化されています。
                    最長スポーン時間 ${(spawner.maxSpawnDelay / 20.0).floor1Digits()}秒
                    最短スポーン時間 ${(spawner.minSpawnDelay / 20.0).floor1Digits()}秒
                    """.trimIndent())
                    return
                }
                item.amount--
                spawner.maxSpawnDelay -= 40
                spawner.minSpawnDelay -= 10
                player.sendSuccessMessage("""
                    スポナーを強化しました。
                    最長スポーン時間 -2秒(${(spawner.maxSpawnDelay / 20.0).floor1Digits()}秒)
                    最短スポーン時間 -0.5秒(${(spawner.minSpawnDelay / 20.0).floor1Digits()}秒)
                """.trimIndent())
            }
            Material.GOLD_INGOT -> {
                if (spawner.spawnCount >= 10) {
                    player.sendErrorMessage("既に最大まで強化されています。(${spawner.spawnCount}体)")
                    return
                }
                if (item.amount < 10) {
                    player.sendErrorMessage("強化には10個必要です。")
                    return
                }
                spawner.spawnCount += 1
                item.amount -= 10
                player.sendSuccessMessage("""
                    スポナーを強化しました。
                    同時スポーン数 +1体(${spawner.spawnCount}体)
                    """.trimIndent())
            }
            Material.AMETHYST_SHARD -> {
                if (spawner.maxNearbyEntities >= 50) {
                    player.sendErrorMessage("既に最大まで強化されています。(${spawner.maxNearbyEntities}体)")
                    return
                }
                if (item.amount < 10)  {
                    player.sendErrorMessage("強化には10個必要です。")
                    return
                }
                spawner.maxNearbyEntities += 2
                item.amount -= 10
                player.sendSuccessMessage("""
                    スポナーを強化しました。
                    最大モンスター数 +2体(${spawner.maxNearbyEntities}体)
                    """.trimIndent())
            }
            Material.NETHER_STAR -> {
                spawner.requiredPlayerRange += 1
                item.amount--
                player.sendSuccessMessage("""
                    スポナーを強化しました。
                    プレイヤー検知範囲 +1m(${spawner.requiredPlayerRange}m)
                    """.trimIndent())
            }
            else -> return
        }
        spawner.update()
        block.world.playSound(block.location, Sound.BLOCK_ANVIL_USE,.8f, .7f)
    }
}