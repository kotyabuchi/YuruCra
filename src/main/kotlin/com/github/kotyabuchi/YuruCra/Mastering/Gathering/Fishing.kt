package com.github.kotyabuchi.YuruCra.Mastering.Gathering

import com.github.kotyabuchi.YuruCra.Mastering.Mastering
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerFishEvent
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

object Fishing: Mastering("FISHING") {
    private val expMap: Map<Material, Int> = mapOf(Material.COD to 10, Material.SALMON to 10, Material.TROPICAL_FISH to 50, Material.PUFFERFISH to 20)

    @EventHandler
    fun onFishing(event: PlayerFishEvent) {
        val player = event.player
        val masteringManager = player.getStatus().masteringManager
        val level = masteringManager.getMasteringStatus(this).getLevel()

        when (event.state) {
            PlayerFishEvent.State.FISHING -> {
                val fishHook = event.hook
                val minLureTimePerLevel = .1
                val maxLureTimePerLevel = .2
                val minWaitTimePerLevel = .1
                val maxWaitTimePerLevel = .4
                fishHook.minLureTime = max(1.0, fishHook.minLureTime - (minLureTimePerLevel * level)).roundToInt()
                fishHook.maxLureTime = max(1.0, fishHook.maxLureTime - (maxLureTimePerLevel * level)).roundToInt()
                fishHook.minWaitTime = max(1.0, fishHook.minWaitTime - (minWaitTimePerLevel * level)).roundToInt()
                fishHook.maxWaitTime = max(1.0, fishHook.maxWaitTime - (maxWaitTimePerLevel * level)).roundToInt()
            }
            PlayerFishEvent.State.CAUGHT_FISH -> {
                event.expToDrop = 0
                val item = (event.caught as? Item)?.itemStack ?: return

                var exp = 0.0
                val doubleDropChance = level / 3
                var multiDropAmount = 1 + floor(doubleDropChance / 100.0).toInt()
                if (Random.nextInt(100) < doubleDropChance % 100) {
                    multiDropAmount++
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.0f)
                }
                val itemExp = expMap[item.type] ?: 20
                item.amount *= multiDropAmount
                exp += (itemExp * item.amount)
                masteringManager.addExp(this, exp)
            }
            else -> {}
        }
    }
}