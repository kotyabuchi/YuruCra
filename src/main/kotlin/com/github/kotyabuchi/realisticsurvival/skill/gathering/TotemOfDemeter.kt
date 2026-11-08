package com.github.kotyabuchi.realisticsurvival.skill.gathering

import com.destroystokyo.paper.ParticleBuilder
import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.block.BreakSource
import com.github.kotyabuchi.realisticsurvival.skill.Skill
import com.github.kotyabuchi.realisticsurvival.util.runTimer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * その場にトーテムを置き、60秒間、半径4ブロック内の作物を育てる。
 * 育ちきった作物は自動で収穫し、植え直す。
 */
class TotemOfDemeter(plugin: RealisticSurvival) : Skill(plugin) {
    override val id = "TOTEM_OF_DEMETER"
    override val displayName = "Totem of Demeter"
    override val needLevel = 20
    override val description = "周囲の植物の成長を促進するトーテムをその場に設置する"
    override val coolTimeMillis = 1000L * 60 * 5

    override fun onActivate(player: Player, level: Int) {
        player.world.playSound(player.eyeLocation, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.3f)
        player.sendActionBar(Component.text(displayName, NamedTextColor.GREEN))

        val base = player.location.block
        val center = base.location.toCenterLocation()
        val crops = buildList {
            for (x in -RADIUS..RADIUS) for (y in -1..1) for (z in -RADIUS..RADIUS) {
                val block = base.getRelative(x, y, z)
                if (block.blockData is Ageable && center.distance(block.location.toCenterLocation()) <= RADIUS + 0.1) add(block)
            }
        }

        // 目印の浮いたアイテム。チャンクに保存しないので、サーバーが止まっても残らない
        val totem = player.world.dropItem(center, ItemStack(Material.WATER_BUCKET)) {
            it.setGravity(false)
            it.setCanPlayerPickup(false)
            it.setCanMobPickup(false)
            it.velocity = Vector()
            it.isPersistent = false
        }

        val ring = (0 until RING_POINTS).map { i ->
            val angle = 2 * PI * i / RING_POINTS
            center.clone().add((RADIUS + 0.2) * cos(angle), 0.0, (RADIUS + 0.2) * -sin(angle))
        }
        val ringParticle = ParticleBuilder(Particle.COMPOSTER).offset(0.0, 0.5, 0.0).speed(0.0).receivers(30).count(5)
        plugin.runTimer(0, 5) { task ->
            if (task.isCancelled || !totem.isValid) {
                task.cancel()
                return@runTimer
            }
            ring.forEach { ringParticle.location(it).spawn() }
        }

        val growParticle = ParticleBuilder(Particle.TOTEM_OF_UNDYING).offset(0.0, 0.2, 0.0).speed(0.15).receivers(30).count(20)
        var count = 0
        plugin.runTimer(0, 2) { task ->
            if (count++ > DURATION_TICKS / 2 || !player.isOnline) {
                task.cancel()
                totem.remove()
                return@runTimer
            }
            val target = crops.randomOrNull() ?: return@runTimer
            growOrHarvest(player, target)
            growParticle.location(target.location.toCenterLocation()).spawn()
        }
    }

    private fun growOrHarvest(player: Player, block: Block) {
        val data = block.blockData as? Ageable ?: return
        if (data.age >= data.maximumAge) {
            plugin.safeCrop.markHarvest(block)
            plugin.breakContext.breakBlocks(player, BreakSource(block), listOf(block))
        } else {
            data.age++
            block.blockData = data
        }
    }

    companion object {
        private const val RADIUS = 4
        private const val RING_POINTS = 76
        private const val DURATION_TICKS = 20 * 60
    }
}
