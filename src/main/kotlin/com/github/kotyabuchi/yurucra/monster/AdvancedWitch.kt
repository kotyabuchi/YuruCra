package com.github.kotyabuchi.yurucra.monster

import com.destroystokyo.paper.event.entity.WitchReadyPotionEvent
import com.destroystokyo.paper.event.entity.WitchThrowPotionEvent
import com.github.kotyabuchi.yurucra.util.runLater
import com.github.kotyabuchi.yurucra.util.runTimer
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.Tag
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Creeper
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Monster
import org.bukkit.entity.Witch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ウィッチの戦闘行動。
 *
 * - 戦闘中、ときどき次のどれかを行う: 近くのクリーパーを帯電させる / 近くのモンスター（最大3体）と自分に移動速度上昇・攻撃力上昇を付ける / 標的に雷を落とす
 * - 自分が使うポーションを状況に合わせて選び直す（悪い効果を受けたら牛乳、体力に余裕があれば再生や耐性、少なければ透明化）
 * - アンデッドが標的のときは、効く方のポーション（治癒）を投げる
 * - ウィッチが攻撃されると、近くの手の空いたウィッチも攻撃した相手を狙う
 */
class AdvancedWitch(private val plugin: Plugin, private val fights: FightTracker) : FightBehavior, Listener {

    override val types = setOf(EntityType.WITCH)

    override val actions = listOf(
        FightAction(intervalTicks = 20, chance = 0.1) { mob, target -> castSpell(mob, target) },
    )

    private enum class Spell { CHARGE_CREEPERS, SPEED, STRENGTH, THUNDER }

    private fun castSpell(witch: Mob, target: LivingEntity): Boolean {
        val creepers = witch.location.getNearbyEntitiesByType(Creeper::class.java, 10.0, 5.0, 10.0).filter { !it.isPowered }
        val spells = Spell.entries.filter { it != Spell.CHARGE_CREEPERS || creepers.isNotEmpty() }
        when (spells.random()) {
            Spell.CHARGE_CREEPERS -> chargeCreepers(creepers)
            Spell.SPEED -> buffNearbyMonsters(witch, target, PotionEffect(PotionEffectType.SPEED, 20 * 30, 1), Color.AQUA)
            Spell.STRENGTH -> buffNearbyMonsters(witch, target, PotionEffect(PotionEffectType.STRENGTH, 20 * 30, 1), Color.RED)
            Spell.THUNDER -> {
                target.world.strikeLightning(target.location)
                // モンスター同士の戦いなら、雷を落とされた側がウィッチを狙う
                (target as? Mob)?.target = witch
            }
        }
        return true
    }

    /** 0.5秒おきに1体ずつ雷の演出を落として帯電させる */
    private fun chargeCreepers(creepers: List<Creeper>) {
        creepers.forEachIndexed { index, creeper ->
            plugin.runLater(index * 10L) {
                if (!creeper.isValid) return@runLater
                creeper.world.strikeLightningEffect(creeper.location)
                creeper.world.playSound(creeper.location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1f)
                creeper.isPowered = true
            }
        }
    }

    /** 足元に輪とらせんの粒子を約1秒出してから効果を付ける */
    private fun buffNearbyMonsters(witch: Mob, target: LivingEntity, effect: PotionEffect, color: Color) {
        val receivers = witch.location.getNearbyEntitiesByType(Monster::class.java, 10.0, 5.0, 10.0)
            .asSequence()
            .filter { it != witch && it != target && it.target != witch && !it.hasPotionEffect(effect.type) }
            .take(MAX_BUFF_TARGETS)
            .toMutableList<LivingEntity>()
        receivers += witch
        val dust = Particle.DustOptions(color, 1f)
        var step = 0
        plugin.runTimer(0, 1) { task ->
            if (step >= RING_POINTS) {
                receivers.forEach { if (it.isValid) it.addPotionEffect(effect) }
                task.cancel()
                return@runTimer
            }
            for (monster in receivers) {
                if (!monster.isValid) continue
                val base = monster.location
                // 足元の輪は3tickごと（粒子の量を抑える）。らせんは毎tick少しずつ上る
                if (step % 3 == 0) for (i in 0 until RING_POINTS) spawnDust(base, i, 0.0, dust)
                for (i in 0 until 3) spawnDust(base, step * 2 + i, step * 0.115, dust)
            }
            step++
        }
    }

    private fun spawnDust(base: Location, point: Int, height: Double, dust: Particle.DustOptions) {
        val angle = 2 * PI / RING_POINTS * point
        base.world.spawnParticle(
            Particle.DUST, base.x + cos(angle), base.y + height, base.z - sin(angle), 1, 0.0, 0.0, 0.0, 0.0, dust,
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onWitchAttacked(event: EntityDamageByEntityEvent) {
        if (event.entityType != EntityType.WITCH) return
        val attacker = event.damager as? LivingEntity ?: return
        for (witch in event.entity.location.getNearbyEntitiesByType(Witch::class.java, 10.0)) {
            if (witch.target != null || witch == attacker) continue
            witch.target = attacker
            fights.start(witch)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onReadyPotion(event: WitchReadyPotionEvent) {
        val witch = event.entity
        if (BAD_EFFECTS.any { witch.hasPotionEffect(it) }) {
            // 牛乳には効果がないので、飲み終わるころに自分で効果を消す
            event.potion = ItemStack(Material.MILK_BUCKET)
            plugin.runLater(MILK_DRINK_TICKS) {
                if (witch.isValid) witch.activePotionEffects.forEach { witch.removePotionEffect(it.type) }
            }
            return
        }
        val potion = event.potion ?: return
        if (potion.potionType() !in HEALING_TYPES) return
        val maxHealth = witch.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        event.potion = when {
            witch.health >= maxHealth / 2 && !witch.hasPotionEffect(PotionEffectType.REGENERATION) ->
                potionOf(PotionType.REGENERATION)
            witch.health >= maxHealth / 2 && !witch.hasPotionEffect(PotionEffectType.RESISTANCE) ->
                ItemStack(Material.POTION).apply {
                    editMeta(PotionMeta::class.java) {
                        it.color = Color.GRAY
                        it.addCustomEffect(PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, 1), true)
                    }
                }
            witch.health < maxHealth / 2 && !witch.hasPotionEffect(PotionEffectType.INVISIBILITY) ->
                potionOf(PotionType.INVISIBILITY)
            else -> return
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onThrowPotion(event: WitchThrowPotionEvent) {
        if (!Tag.ENTITY_TYPES_UNDEAD.isTagged(event.target.type)) return
        val potion = event.potion ?: return
        // アンデッドには毒が効かず、負傷のポーションで回復してしまうので、治癒のポーションでダメージを与える
        if (potion.potionType() in POISON_OR_HARMING_TYPES) {
            event.potion = ItemStack(potion.type).apply {
                editMeta(PotionMeta::class.java) { it.basePotionType = PotionType.HEALING }
            }
        }
    }

    private fun ItemStack.potionType(): PotionType? = (itemMeta as? PotionMeta)?.basePotionType

    private fun potionOf(type: PotionType): ItemStack =
        ItemStack(Material.POTION).apply { editMeta(PotionMeta::class.java) { it.basePotionType = type } }

    companion object {
        private const val MAX_BUFF_TARGETS = 3
        private const val RING_POINTS = 18
        private const val MILK_DRINK_TICKS = 30L

        private val BAD_EFFECTS = listOf(
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS, PotionEffectType.LEVITATION,
        )
        private val HEALING_TYPES = setOf(PotionType.HEALING, PotionType.STRONG_HEALING)
        private val POISON_OR_HARMING_TYPES = setOf(
            PotionType.POISON, PotionType.LONG_POISON, PotionType.STRONG_POISON,
            PotionType.HARMING, PotionType.STRONG_HARMING,
        )
    }
}
