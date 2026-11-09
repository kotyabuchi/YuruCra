package com.github.kotyabuchi.yurucra.monster

import com.github.kotyabuchi.yurucra.util.runTimer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.util.UUID

/**
 * クモ・洞窟グモの戦闘行動。
 *
 * - ときどき標的に向けてクモの巣を飛ばす。着地した巣は5秒で消える
 * - ときどき標的に向かって飛びかかる
 */
class AdvancedSpider(private val plugin: Plugin) : FightBehavior, Listener {

    override val types = setOf(EntityType.SPIDER, EntityType.CAVE_SPIDER)

    override val actions = listOf(
        FightAction(intervalTicks = 10, chance = 0.3, cooldownTicks = 20 * 5) { mob, target -> shootWeb(mob, target) },
        FightAction(intervalTicks = 10, chance = 0.5, cooldownTicks = 20 * 3) { mob, target -> leap(mob, target) },
    )

    private class PlacedWeb(val location: Location, val removeAt: Int)

    /** 飛んでいる途中の巣 */
    private val flyingWebs = mutableSetOf<UUID>()

    /** 着地した巣。寿命が同じなので、置いた順に並べておけば先頭から消せる */
    private val placedWebs = ArrayDeque<PlacedWeb>()

    init {
        plugin.runTimer(20, 20) { removeExpiredWebs() }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onLand(event: EntityChangeBlockEvent) {
        val falling = event.entity as? FallingBlock ?: return
        if (!flyingWebs.remove(falling.uniqueId)) return
        placedWebs.addLast(PlacedWeb(event.block.location, plugin.server.currentTick + WEB_LIFETIME_TICKS))
    }

    /** 停止時に、残っている巣を消す */
    fun clear() {
        placedWebs.forEach { removeWeb(it.location) }
        placedWebs.clear()
        flyingWebs.clear()
    }

    private fun shootWeb(mob: Mob, target: LivingEntity): Boolean {
        val from = mob.location.toVector()
        val to = target.location.add(0.0, 0.5, 0.0).toVector()
        val direction = to.clone().subtract(from)
        val spawnAt = mob.location.add(direction.clone().normalize().multiply(0.3))
        val web = mob.world.spawn(spawnAt, FallingBlock::class.java) {
            it.blockData = Material.COBWEB.createBlockData()
            it.dropItem = false
            it.isPersistent = false
        }
        web.velocity = direction.multiply(0.2)
        flyingWebs += web.uniqueId
        return true
    }

    private fun leap(mob: Mob, target: LivingEntity): Boolean {
        val from = mob.location
        val to = target.location.add(0.0, target.eyeHeight / 2, 0.0)
        // 遠いほど少し高く跳ぶ
        to.y += from.distance(to) / 40.0
        val velocity = to.toVector().subtract(from.toVector()).multiply(0.3)
        mob.velocity = Vector(
            velocity.x.coerceIn(-MAX_LEAP_SPEED, MAX_LEAP_SPEED),
            velocity.y.coerceIn(-MAX_LEAP_SPEED, MAX_LEAP_SPEED),
            velocity.z.coerceIn(-MAX_LEAP_SPEED, MAX_LEAP_SPEED),
        )
        return true
    }

    private fun removeExpiredWebs() {
        // 着地しないまま消えた巣の印も、ここで少しずつ片付ける
        if (flyingWebs.isNotEmpty()) flyingWebs.removeIf { plugin.server.getEntity(it) == null }
        val now = plugin.server.currentTick
        while (placedWebs.isNotEmpty() && placedWebs.first().removeAt <= now) {
            removeWeb(placedWebs.removeFirst().location)
        }
    }

    private fun removeWeb(location: Location) {
        // 読み込まれていないチャンクは触らない（その巣は残る）
        if (!location.isChunkLoaded) return
        val block = location.block
        if (block.type == Material.COBWEB) block.type = Material.AIR
    }

    companion object {
        private const val WEB_LIFETIME_TICKS = 20 * 5
        private const val MAX_LEAP_SPEED = 4.0
    }
}
