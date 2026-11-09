package com.github.kotyabuchi.yurucra.monster

import com.github.kotyabuchi.yurucra.util.runTimer
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.plugin.Plugin
import java.util.UUID
import kotlin.random.Random

/**
 * 戦闘中のモンスターが一定間隔で行う行動。
 *
 * @param intervalTicks 何tickごとに行うか判定するか（[FightTracker.PERIOD_TICKS] の倍数にする）
 * @param chance 判定のたびに行う確率
 * @param cooldownTicks 行ったあと、次に行えるまでの時間
 * @param perform 行動。行えなかった（条件が合わなかった）ときは false を返すと、クールダウンに入らない
 */
class FightAction(
    val intervalTicks: Int,
    val chance: Double,
    val cooldownTicks: Int = 0,
    val perform: (mob: Mob, target: LivingEntity) -> Boolean,
)

/** 戦闘中に特別な行動をするモンスターの種類 */
interface FightBehavior {
    val types: Set<EntityType>
    val actions: List<FightAction>

    /** 標的がいなくなったとき・死んだとき・読み込みが外れたときに呼ばれる。後片付けに使う */
    fun onFightEnd(mob: Mob) {}
}

/**
 * 標的を見つけたモンスターを「戦闘中」として覚え、1つのタイマーで行動させる。
 * 標的がいなくなったら戦闘終了。戦闘中でないモンスターには何もしないので、数が多くても負荷は戦闘中の数だけ。
 */
class FightTracker(private val plugin: Plugin) : Listener {

    private class Entry(val behavior: FightBehavior, val action: FightAction)

    private class Fight(val mob: Mob, val entries: List<Entry>, now: Int) {
        val nextCheckAt = IntArray(entries.size) { now + entries[it].action.intervalTicks }
        val cooldownUntil = IntArray(entries.size)
    }

    private val entriesByType = mutableMapOf<EntityType, MutableList<Entry>>()
    private val behaviorsByType = mutableMapOf<EntityType, MutableList<FightBehavior>>()
    private val fights = mutableMapOf<UUID, Fight>()

    init {
        plugin.runTimer(PERIOD_TICKS.toLong(), PERIOD_TICKS.toLong()) { tick() }
    }

    fun register(behavior: FightBehavior) {
        for (type in behavior.types) {
            behaviorsByType.getOrPut(type) { mutableListOf() } += behavior
            behavior.actions.forEach { entriesByType.getOrPut(type) { mutableListOf() } += Entry(behavior, it) }
        }
    }

    /** 戦闘中として扱い始める。既に戦闘中なら何もしない */
    fun start(mob: Mob) {
        if (mob.uniqueId in fights) return
        val entries = entriesByType[mob.type]
        // 行動はなくても、終了時の後片付けが必要な種類がある
        if (entries == null && mob.type !in behaviorsByType) return
        fights[mob.uniqueId] = Fight(mob, entries.orEmpty(), plugin.server.currentTick)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTarget(event: EntityTargetLivingEntityEvent) {
        if (event.target == null) return
        val mob = event.entity as? Mob ?: return
        start(mob)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDeath(event: EntityDeathEvent) {
        val fight = fights.remove(event.entity.uniqueId) ?: return
        end(fight)
    }

    /** 停止時に、戦闘中のモンスターの後片付け（骨の盾を消す・武器を戻すなど）をする */
    fun clear() {
        fights.values.forEach { end(it) }
        fights.clear()
    }

    private fun tick() {
        if (fights.isEmpty()) return
        val now = plugin.server.currentTick
        val iterator = fights.values.iterator()
        while (iterator.hasNext()) {
            val fight = iterator.next()
            val mob = fight.mob
            val target = mob.target
            if (!mob.isValid || target == null || !target.isValid || target.isDead) {
                iterator.remove()
                end(fight)
                continue
            }
            for ((i, entry) in fight.entries.withIndex()) {
                if (now < fight.nextCheckAt[i]) continue
                fight.nextCheckAt[i] = now + entry.action.intervalTicks
                if (now < fight.cooldownUntil[i]) continue
                if (Random.nextDouble() >= entry.action.chance) continue
                if (entry.action.perform(mob, target)) fight.cooldownUntil[i] = now + entry.action.cooldownTicks
                // 行動で自分が死んだり消えたりしたら、残りは行わない
                if (!mob.isValid) break
            }
        }
    }

    private fun end(fight: Fight) {
        behaviorsByType[fight.mob.type]?.forEach { it.onFightEnd(fight.mob) }
    }

    companion object {
        const val PERIOD_TICKS = 5
    }
}
