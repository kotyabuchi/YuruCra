package com.github.kotyabuchi.yurucra.job

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Creeper
import org.bukkit.entity.Enemy
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.entity.Wolf
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID
import kotlin.random.Random

/**
 * 飼っている狼の戦い方。テイマーのタイマー（10tickごと）からプレイヤーごとに [tick]、狼が攻撃したときに [onAttack] が呼ばれる。
 *
 * - 何も狙っていない狼は、飼い主か飼い主のペットを狙っている敵のうち、いちばん近いものを狙う（クリーパーはバニラどおり狙わない）
 * - バニラでは届かないが少し離れた標的にも噛みつく（攻撃の補助）
 * - レベルで特殊行動を覚える
 *   - Lv.1 飛びつき: 標的が1〜6ブロック先にいると、着地するころの標的の位置をめがけて低く跳ぶ（噛める距離なら跳ばない）
 *   - Lv.10 遠吠え: 近くの同じ飼い主の狼全員に、移動速度上昇Iと攻撃力上昇Iを10秒
 *   - Lv.20 足を噛む: 攻撃した相手に、移動速度低下IIを3秒
 *   - Lv.30 威嚇: 近くの敵に、弱体化Iを6秒
 *   - Lv.40 なぎ払い: 噛んだ相手の周りの敵に、狼の攻撃力の半分のダメージ
 */
class WolfCombat(
    private val plugin: Plugin,
    private val levels: WolfLevels,
    private val isDying: (LivingEntity) -> Boolean,
) {
    private class Cooldowns {
        var leap = 0
        var howl = 0
        var intimidate = 0
        /** 次に攻撃の補助をしてよいtick（バニラの攻撃でも更新する） */
        var attack = 0
    }

    private val cooldowns = HashMap<UUID, Cooldowns>()

    /** なぎ払いのダメージで、もう一度特殊行動が起きないようにする */
    private var sweeping = false

    fun tick(player: Player) {
        val wolves = player.location.getNearbyEntitiesByType(Wolf::class.java, RANGE)
            .filter { it.ownerUniqueId == player.uniqueId && !it.isSitting && !isDying(it) }
        if (wolves.isEmpty()) return
        val enemies by lazy { enemiesTargeting(player) }
        val now = plugin.server.currentTick
        for (wolf in wolves) {
            val target = wolf.target?.takeIf { it.isValid && !it.isDead }
            if (target == null) {
                val nearest = enemies.minByOrNull { it.location.distanceSquared(wolf.location) } ?: continue
                wolf.target = nearest
                continue
            }
            val level = levels.level(wolf).coerceAtLeast(1)
            val cooldown = cooldowns.getOrPut(wolf.uniqueId) { Cooldowns() }
            if (tryAssistAttack(wolf, target, cooldown, now)) continue
            if (tryLeap(wolf, target, cooldown, now)) continue
            if (level >= HOWL_LEVEL && cooldown.howl <= now && Random.nextDouble() < HOWL_CHANCE) {
                cooldown.howl = now + HOWL_COOLDOWN_TICKS
                howl(wolf, wolves)
                continue
            }
            if (level >= INTIMIDATE_LEVEL && cooldown.intimidate <= now && Random.nextDouble() < INTIMIDATE_CHANCE) {
                cooldown.intimidate = now + INTIMIDATE_COOLDOWN_TICKS
                intimidate(wolf)
            }
        }
    }

    /** 狼が攻撃したとき */
    fun onAttack(wolf: Wolf, target: LivingEntity) {
        if (sweeping) return
        cooldowns.getOrPut(wolf.uniqueId) { Cooldowns() }.attack = plugin.server.currentTick + ATTACK_INTERVAL_TICKS
        val level = levels.level(wolf)
        if (level >= BITE_LEVEL && Random.nextDouble() < BITE_CHANCE) {
            target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, BITE_TICKS, 1))
        }
        if (level >= SWEEP_LEVEL && Random.nextDouble() < SWEEP_CHANCE) sweep(wolf, target)
    }

    /** 使わなくなった記録の片付け（5秒ごと） */
    fun cleanup() {
        val now = plugin.server.currentTick
        cooldowns.values.removeIf { it.leap <= now && it.howl <= now && it.intimidate <= now && it.attack <= now }
    }

    private fun enemiesTargeting(player: Player): List<Mob> =
        player.location.getNearbyEntitiesByType(Mob::class.java, RANGE).filter { mob ->
            if (mob !is Enemy || mob is Creeper) return@filter false
            val target = mob.target ?: return@filter false
            target == player || (target is Tameable && target.ownerUniqueId == player.uniqueId)
        }

    /**
     * バニラの狼は当たり判定が低く小さいので、少し高い場所の相手などに噛みつけない。
     * バニラでは届かないが少し広げた範囲（横+0.5・縦+1）にいる標的を、1秒に1回まで噛ませる
     */
    private fun tryAssistAttack(wolf: Wolf, target: LivingEntity, cooldown: Cooldowns, now: Int): Boolean {
        if (cooldown.attack > now) return false
        val box = wolf.boundingBox
        val targetBox = target.boundingBox
        if (box.clone().expand(VANILLA_REACH, 0.0, VANILLA_REACH).overlaps(targetBox)) return false
        if (!box.expand(VANILLA_REACH + EXTRA_REACH, EXTRA_REACH_VERTICAL, VANILLA_REACH + EXTRA_REACH).overlaps(targetBox)) return false
        if (!wolf.hasLineOfSight(target)) return false
        // 攻撃すると onAttack で次の攻撃までの間隔が記録される
        wolf.attack(target)
        return true
    }

    private fun tryLeap(wolf: Wolf, target: LivingEntity, cooldown: Cooldowns, now: Int): Boolean {
        if (!wolf.isOnGround || cooldown.leap > now) return false
        val distance = target.location.distance(wolf.location)
        if (distance !in LEAP_MIN_DISTANCE..LEAP_MAX_DISTANCE) return false
        // そのまま噛める距離なら跳ばない（ぎりぎり届かないときの補助として跳ぶ）
        if (wolf.boundingBox.expand(VANILLA_REACH, 0.0, VANILLA_REACH).overlaps(target.boundingBox)) return false
        if (Random.nextDouble() >= LEAP_CHANCE) return false
        val velocity = leapVelocity(wolf, target) ?: return false
        cooldown.leap = now + LEAP_COOLDOWN_TICKS
        wolf.velocity = velocity
        wolf.world.playSound(wolf.location, Sound.ENTITY_WOLF_GROWL, 0.6f, 1.3f)
        return true
    }

    /**
     * 標的の手前まで届く初速を、バニラの動きを1tickずつなぞって求める。
     * 空中では毎tick 縦: (速さ − 0.08) × 0.98、横: × 0.91。跳んだ瞬間の1tickだけ、地面の摩擦で横が × 0.546 になる。
     * 標的が動いていれば、着地するころの位置を狙う。高すぎて届かないなら null
     */
    private fun leapVelocity(wolf: Wolf, target: LivingEntity): Vector? {
        val from = wolf.location.toVector()
        val dy = target.location.y - from.y
        // 高い所へは少し強く跳ぶ
        val vy0 = LEAP_HEIGHT + maxOf(0.0, dy) * 0.12
        var y = 0.0
        var vy = vy0
        var ticks = 0
        var horizontalFactor = 0.0
        var friction = 1.0
        while (ticks < MAX_LEAP_TICKS) {
            horizontalFactor += friction
            friction *= if (ticks == 0) GROUND_FRICTION else AIR_FRICTION
            y += vy
            vy = (vy - GRAVITY) * VERTICAL_DRAG
            ticks++
            if (vy < 0 && y <= dy) break
        }
        if (ticks >= MAX_LEAP_TICKS) return null
        // 着地するころの標的の位置。体の幅の分だけ手前に降りる
        val aim = target.location.toVector().add(target.velocity.clone().multiply(ticks)).subtract(from).setY(0.0)
        val reach = (aim.length() - (wolf.width + target.width) / 2).coerceAtLeast(0.0)
        if (aim.lengthSquared() < 1.0E-6) return null
        val speed = minOf(reach / horizontalFactor, LEAP_MAX_SPEED)
        return aim.normalize().multiply(speed).setY(vy0)
    }

    private fun howl(wolf: Wolf, packWolves: List<Wolf>) {
        wolf.world.playSound(wolf.location, Sound.ENTITY_WOLF_WHINE, 1f, 0.6f)
        for (member in packWolves) {
            if (member.location.distanceSquared(wolf.location) > HOWL_RANGE * HOWL_RANGE) continue
            member.addPotionEffect(PotionEffect(PotionEffectType.SPEED, HOWL_TICKS, 0))
            member.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, HOWL_TICKS, 0))
            member.world.spawnParticle(Particle.NOTE, member.location.add(0.0, 1.0, 0.0), 1)
        }
    }

    private fun intimidate(wolf: Wolf) {
        wolf.world.playSound(wolf.location, Sound.ENTITY_WOLF_GROWL, 1f, 0.6f)
        for (enemy in wolf.location.getNearbyEntitiesByType(Mob::class.java, INTIMIDATE_RANGE)) {
            if (enemy !is Enemy) continue
            enemy.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, INTIMIDATE_TICKS, 0))
            enemy.world.spawnParticle(Particle.ANGRY_VILLAGER, enemy.eyeLocation, 1)
        }
    }

    private fun sweep(wolf: Wolf, target: LivingEntity) {
        val damage = (wolf.getAttribute(Attribute.ATTACK_DAMAGE)?.value ?: return) * SWEEP_DAMAGE_RATE
        val source = DamageSource.builder(DamageType.MOB_ATTACK).withCausingEntity(wolf).withDirectEntity(wolf).build()
        target.world.spawnParticle(Particle.SWEEP_ATTACK, target.location.add(0.0, target.height / 2, 0.0), 1)
        sweeping = true
        try {
            for (enemy in target.location.getNearbyEntitiesByType(Mob::class.java, SWEEP_RANGE)) {
                if (enemy == target || enemy !is Enemy) continue
                enemy.damage(damage, source)
            }
        } finally {
            sweeping = false
        }
    }

    private companion object {
        const val RANGE = 24.0

        /** バニラのモンスターの近接攻撃の届く幅（当たり判定から横に √2.04 − 0.6） */
        const val VANILLA_REACH = 0.828
        const val EXTRA_REACH = 0.5
        const val EXTRA_REACH_VERTICAL = 1.0
        const val ATTACK_INTERVAL_TICKS = 20

        const val LEAP_MIN_DISTANCE = 1.0
        const val LEAP_MAX_DISTANCE = 6.0
        const val LEAP_CHANCE = 0.3
        const val LEAP_COOLDOWN_TICKS = 60
        const val LEAP_MAX_SPEED = 1.3
        const val LEAP_HEIGHT = 0.35
        const val MAX_LEAP_TICKS = 40
        const val GRAVITY = 0.08
        const val VERTICAL_DRAG = 0.98
        const val AIR_FRICTION = 0.91
        const val GROUND_FRICTION = 0.546

        const val HOWL_LEVEL = 10
        const val HOWL_CHANCE = 0.1
        const val HOWL_COOLDOWN_TICKS = 20 * 30
        const val HOWL_RANGE = 12.0
        const val HOWL_TICKS = 20 * 10

        const val BITE_LEVEL = 20
        const val BITE_CHANCE = 0.25
        const val BITE_TICKS = 20 * 3

        const val INTIMIDATE_LEVEL = 30
        const val INTIMIDATE_CHANCE = 0.1
        const val INTIMIDATE_COOLDOWN_TICKS = 20 * 20
        const val INTIMIDATE_RANGE = 6.0
        const val INTIMIDATE_TICKS = 20 * 6

        const val SWEEP_LEVEL = 40
        const val SWEEP_CHANCE = 0.2
        const val SWEEP_RANGE = 2.5
        const val SWEEP_DAMAGE_RATE = 0.5
    }
}
