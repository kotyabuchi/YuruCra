package com.github.kotyabuchi.yurucra.job

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.skill.SkillCommand
import com.github.kotyabuchi.yurucra.skill.tamer.CallWolves
import com.github.kotyabuchi.yurucra.skill.tamer.HealWolves
import com.github.kotyabuchi.yurucra.skill.tamer.SitOrFollow
import com.github.kotyabuchi.yurucra.util.runTimer
import io.papermc.paper.event.player.PlayerNameEntityEvent
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Sittable
import org.bukkit.entity.Tameable
import org.bukkit.entity.Wolf
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

/**
 * テイマー。ジョブの道具は人参付きの棒で、自分の狼に指示するスキルを使える。
 *
 * - 飼っている動物が攻撃すると、飼い主に経験値が入る（ダメージの半分で計算）。狼なら狼自身にも経験値が入り、レベルが上がる（[WolfLevels]）
 * - 飼っている動物は死ぬ代わりに瀕死になる（光って動かなくなり、攻撃されない）。
 *   飼い主がそばで3秒しゃがみ続けるか、回復させると起き上がる。奈落と /kill では瀕死にならずに死ぬ
 * - テイマーのレベルが上がると、飼っている動物が全回復する
 * - レベルの付いた狼は、5秒ごとにレベルに応じて自然回復する
 * - 狼は飼い主やペットを狙う敵を自動で狙い、レベルで覚えた特殊行動を使う（[WolfCombat]）
 */
class Tamer(plugin: YuruCra) : CombatJob(plugin, JobType.TAMER) {

    val wolfLevels = WolfLevels(plugin)

    private val wolfCombat = WolfCombat(plugin, wolfLevels) { isDying(it) }

    private val dyingKey = NamespacedKey(plugin, "dying")

    /** しゃがんで介抱しているプレイヤー → 介抱し終わるtickと、起こす動物 */
    private val nursing = HashMap<UUID, Nursing>()

    private class Nursing(val player: Player, val finishAt: Int, val pets: List<LivingEntity>)

    init {
        addTools(listOf(Material.CARROT_ON_A_STICK))
        registerSkill(SkillCommand.LRL, CallWolves(plugin, this))
        registerSkill(SkillCommand.RLR, SitOrFollow(plugin, this))
        registerSkill(SkillCommand.LLL, HealWolves(plugin, this))
        plugin.runTimer(1, 1) { tickNursing() }
        // 狼の自然回復。5秒に1回、全ワールドの狼をまとめて見る
        plugin.runTimer(WOLF_REGEN_TICKS, WOLF_REGEN_TICKS) {
            plugin.server.worlds.forEach { world -> wolfLevels.regenerate(world.getEntitiesByClass(Wolf::class.java)) { isDying(it) } }
            wolfCombat.cleanup()
        }
        // 狼の自動の狙い・飛びつき。プレイヤーごとに、そばにいる自分の狼だけを見る
        plugin.runTimer(WOLF_COMBAT_TICKS, WOLF_COMBAT_TICKS) {
            plugin.server.onlinePlayers.forEach { wolfCombat.tick(it) }
        }
    }

    /** 自分の狼（瀕死のものは除く） */
    fun ownWolves(player: Player, radius: Double): List<Wolf> =
        player.location.getNearbyEntitiesByType(Wolf::class.java, radius)
            .filter { it.ownerUniqueId == player.uniqueId && !isDying(it) }

    fun isDying(entity: LivingEntity): Boolean = entity.persistentDataContainer.has(dyingKey)

    fun revive(pet: LivingEntity) {
        pet.persistentDataContainer.remove(dyingKey)
        pet.setAI(true)
        pet.removePotionEffect(PotionEffectType.GLOWING)
        if (pet is Sittable) pet.isSitting = false
        val location = pet.location.add(0.0, 1.0, 0.0)
        pet.world.spawnParticle(Particle.HAPPY_VILLAGER, location, 5, 0.5, 0.3, 0.5)
        pet.world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPetAttack(event: EntityDamageByEntityEvent) {
        val pet = event.damager as? Tameable ?: return
        val owner = pet.owner as? Player ?: return
        val target = event.entity as? LivingEntity ?: return
        if (!isExpTarget(target)) return
        val ownerExp = expFor(target, event.damage / 2)
        if (ownerExp > 0) plugin.progress.addExp(owner, this, ownerExp)
        if (pet is Wolf) {
            val petExp = combatExp(target, event.damage)
            if (petExp > 0) wolfLevels.addExp(pet, petExp)
            wolfCombat.onAttack(pet, target)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onRename(event: PlayerNameEntityEvent) {
        val wolf = event.entity as? Wolf ?: return
        if (!wolf.isTamed || wolfLevels.level(wolf) == 0) return
        val name = event.name ?: return
        event.name = wolfLevels.rename(wolf, name)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPetDamaged(event: EntityDamageEvent) {
        val pet = event.entity as? Tameable ?: return
        if (!pet.isTamed || pet.ownerUniqueId == null) return
        val cause = event.cause
        if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.KILL) return
        if (isDying(pet)) {
            event.isCancelled = true
            return
        }
        if (event.finalDamage < pet.health) return
        event.isCancelled = true
        pet.health = 1.0
        startDying(pet)
    }

    @EventHandler(ignoreCancelled = true)
    fun onRegain(event: EntityRegainHealthEvent) {
        val pet = event.entity as? Tameable ?: return
        if (isDying(pet)) revive(pet)
    }

    @EventHandler(ignoreCancelled = true)
    fun onTarget(event: EntityTargetLivingEntityEvent) {
        val pet = event.target as? Tameable ?: return
        if (isDying(pet)) event.isCancelled = true
    }

    @EventHandler
    fun onSneak(event: PlayerToggleSneakEvent) {
        val player = event.player
        if (!event.isSneaking) {
            nursing.remove(player.uniqueId)
            return
        }
        val pets = player.getNearbyEntities(2.0, 1.0, 2.0)
            .filter { it is Tameable && it.ownerUniqueId == player.uniqueId && isDying(it) }
            .filterIsInstance<LivingEntity>()
        if (pets.isEmpty()) return
        nursing[player.uniqueId] = Nursing(player, plugin.server.currentTick + NURSING_TICKS, pets)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        nursing.remove(event.player.uniqueId)
    }

    override fun onLevelUp(player: Player) {
        for (pet in player.world.getEntitiesByClass(Tameable::class.java)) {
            if (pet.ownerUniqueId != player.uniqueId) continue
            val maxHealth = pet.getAttribute(Attribute.MAX_HEALTH)?.value ?: continue
            pet.health = maxHealth
            pet.world.spawnParticle(Particle.HEART, pet.location.add(0.0, 1.0, 0.0), 5, 0.5, 0.3, 0.5)
            if (isDying(pet)) revive(pet)
        }
    }

    private fun tickNursing() {
        if (nursing.isEmpty()) return
        val now = plugin.server.currentTick
        val iterator = nursing.values.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val pets = entry.pets.filter { it.isValid && isDying(it) }
            if (pets.isEmpty() || !entry.player.isOnline) {
                iterator.remove()
                continue
            }
            if (now >= entry.finishAt) {
                pets.forEach { revive(it) }
                iterator.remove()
                continue
            }
            pets.forEach { it.world.spawnParticle(Particle.CLOUD, it.location.add(0.0, it.height / 2, 0.0), 3, 0.5, 0.5, 0.5, 0.0) }
        }
    }

    private fun startDying(pet: LivingEntity) {
        pet.persistentDataContainer.set(dyingKey, PersistentDataType.BOOLEAN, true)
        pet.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0, true, false))
        pet.setAI(false)
        if (pet is Sittable) pet.isSitting = true
        // 瀕死の動物を狙っているモンスターは狙いを外す
        for (mob in pet.location.getNearbyEntitiesByType(Mob::class.java, 30.0, 10.0, 30.0)) {
            if (mob.target == pet) mob.target = null
        }
    }

    private companion object {
        const val NURSING_TICKS = 20 * 3
        const val WOLF_REGEN_TICKS = 20 * 5L
        const val WOLF_COMBAT_TICKS = 10L
    }
}
