package com.github.kotyabuchi.yurucra.job

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.util.ToolType
import org.bukkit.attribute.Attribute
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import kotlin.math.sqrt

/**
 * 戦闘系ジョブの共通処理。攻撃した相手が強いほど（体力・攻撃力・防御力などが高いほど）経験値が多い。
 * 1回の攻撃で数えるダメージは相手の残り体力まで（とどめの大ダメージで稼げないように）。
 */
abstract class CombatJob(plugin: YuruCra, type: JobType) : Job(plugin, type) {
    protected fun expFor(target: LivingEntity, damage: Double): Double = combatExp(target, damage)
    protected fun isExpTarget(entity: LivingEntity): Boolean = isCombatExpTarget(entity)
}

/** 戦闘で得る経験値。テイマーの狼のレベルでも使う */
fun combatExp(target: LivingEntity, damage: Double): Double {
    val counted = minOf(damage, target.health)
    if (counted <= 0) return 0.0
    fun attr(attribute: Attribute) = target.getAttribute(attribute)?.value ?: 0.0
    val strength = attr(Attribute.ARMOR) +
        attr(Attribute.ARMOR_TOUGHNESS) * 2 +
        attr(Attribute.ATTACK_DAMAGE) * 10 +
        attr(Attribute.ATTACK_KNOCKBACK) +
        attr(Attribute.KNOCKBACK_RESISTANCE) +
        attr(Attribute.MAX_HEALTH) +
        attr(Attribute.MOVEMENT_SPEED) * 10
    return sqrt(counted) * strength / 10
}

/** 経験値の対象になる相手か（防具立ては殴っても稼げないようにする） */
fun isCombatExpTarget(entity: LivingEntity): Boolean = entity !is ArmorStand

/**
 * 武器で近接攻撃すると経験値が入る戦闘系ジョブ。レベルが上がるほど、その武器で与えるダメージが増える。
 * 斧は木こりの道具でもあるので、スキル詠唱に使う「ジョブの道具」には登録しない。
 */
abstract class WeaponCombatJob(plugin: YuruCra, type: JobType, private val weapon: ToolType) : CombatJob(plugin, type) {

    /** 適した武器で殴ると、レベルに応じてダメージが増える（(レベル−1)×0.5%） */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBoost(event: EntityDamageByEntityEvent) {
        val player = weaponAttacker(event) ?: return
        val bonus = (level(player) - 1) * DAMAGE_BONUS_PER_LEVEL
        if (bonus > 0) event.damage *= 1 + bonus
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onHit(event: EntityDamageByEntityEvent) {
        val player = weaponAttacker(event) ?: return
        val target = event.entity as? LivingEntity ?: return
        if (!isExpTarget(target)) return
        val exp = expFor(target, event.damage)
        if (exp > 0) plugin.progress.addExp(player, this, exp)
    }

    /** このジョブの武器で直接殴ったプレイヤー（なぎ払いを含む） */
    private fun weaponAttacker(event: EntityDamageByEntityEvent): Player? {
        val cause = event.cause
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK && cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return null
        if (event.entity !is LivingEntity) return null
        val player = event.damager as? Player ?: return null
        return player.takeIf { weapon.includes(it.inventory.itemInMainHand) }
    }

    private companion object {
        const val DAMAGE_BONUS_PER_LEVEL = 0.005
    }
}

class SwordMaster(plugin: YuruCra) : WeaponCombatJob(plugin, JobType.SWORD_MASTER, ToolType.SWORD)

class BattleAxe(plugin: YuruCra) : WeaponCombatJob(plugin, JobType.BATTLE_AXE, ToolType.AXE)
