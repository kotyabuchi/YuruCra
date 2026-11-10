package com.github.kotyabuchi.yurucra.skill.tamer

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.job.Tamer
import com.github.kotyabuchi.yurucra.skill.Skill
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.entity.Wolf

/** 64ブロック以内の自分の狼を立たせて、そばに呼び寄せる */
class CallWolves(plugin: YuruCra, private val tamer: Tamer) : Skill(plugin) {
    override val id = "CALL_WOLVES"
    override val displayName = "Call Wolves"
    override val needLevel = 0
    override val description = "離れた場所にいる自分の狼を呼び寄せる"
    override val coolTimeMillis = 1000L * 10

    override fun activate(player: Player, level: Int) {
        if (!checkUsable(player, level)) return
        val wolves = tamer.ownWolves(player, RADIUS)
        if (wolves.isEmpty()) {
            fail(player, "$displayName: No wolves nearby")
            return
        }
        onActivate(player, level)
        markUsed(player.uniqueId)
        for (wolf in wolves) {
            wolf.isSitting = false
            wolf.teleport(player.location)
        }
        player.world.playSound(player.location, Sound.ENTITY_WOLF_AMBIENT, 1f, 1.2f)
    }

    private companion object {
        const val RADIUS = 64.0
    }
}

/** 32ブロック以内の自分の狼が1匹でも立っていれば全員座らせ、全員座っていれば全員立たせる */
class SitOrFollow(plugin: YuruCra, private val tamer: Tamer) : Skill(plugin) {
    override val id = "SIT_OR_FOLLOW"
    override val displayName = "Sit / Follow"
    override val needLevel = 0
    override val description = "自分の狼をまとめて座らせる・立たせる"

    override fun activate(player: Player, level: Int) {
        if (!checkUsable(player, level)) return
        val wolves = tamer.ownWolves(player, RADIUS)
        if (wolves.isEmpty()) {
            fail(player, "$displayName: No wolves nearby")
            return
        }
        val sit = wolves.any { !it.isSitting }
        wolves.forEach { it.isSitting = sit }
        player.playSound(player.location, Sound.ENTITY_WOLF_AMBIENT, 0.7f, if (sit) 0.8f else 1.2f)
        player.sendActionBar(Component.text(if (sit) "Sit" else "Follow", NamedTextColor.GREEN))
    }

    private companion object {
        const val RADIUS = 32.0
    }
}

/** 16ブロック以内の自分の狼を回復する（4 + テイマーレベル×0.2）。瀕死の狼も起こす */
class HealWolves(plugin: YuruCra, private val tamer: Tamer) : Skill(plugin) {
    override val id = "HEAL_WOLVES"
    override val displayName = "Heal Wolves"
    override val needLevel = 20
    override val description = "近くにいる自分の狼を回復する（瀕死の狼も起こす）"
    override val coolTimeMillis = 1000L * 60

    override fun activate(player: Player, level: Int) {
        if (!checkUsable(player, level)) return
        // 瀕死の狼も対象にするので、ownWolves ではなく自分で探す
        val wolves = player.location.getNearbyEntitiesByType(Wolf::class.java, RADIUS)
            .filter { it.ownerUniqueId == player.uniqueId }
        if (wolves.isEmpty()) {
            fail(player, "$displayName: No wolves nearby")
            return
        }
        onActivate(player, level)
        markUsed(player.uniqueId)
        val amount = 4 + level * 0.2
        for (wolf in wolves) {
            if (tamer.isDying(wolf)) tamer.revive(wolf)
            val max = wolf.getAttribute(Attribute.MAX_HEALTH)?.value ?: continue
            wolf.health = minOf(max, wolf.health + amount)
            wolf.world.spawnParticle(Particle.HEART, wolf.location.add(0.0, 1.0, 0.0), 5, 0.4, 0.3, 0.4)
        }
    }

    private companion object {
        const val RADIUS = 16.0
    }
}
