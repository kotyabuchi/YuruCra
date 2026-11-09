package com.github.kotyabuchi.yurucra.monster

import com.github.kotyabuchi.yurucra.util.runTimer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * スケルトン・ストレイの戦闘行動。
 *
 * - 標的が近く（4ブロック以内）で歩いてたどり着けるときは、木の斧に持ち替えて殴りに来る。6ブロック以上離れたら元の武器に戻す。
 *   元の武器はスケルトン自身に保存するので、持ち替え中に読み込みが外れても失われない
 * - ときどき周りに骨を3本回す（骨の盾）。盾がある間は攻撃を1回受けるたびに骨が1本減り、ダメージを受けない
 */
class AdvancedSkeleton(private val plugin: Plugin) : FightBehavior, Listener {

    override val types = setOf(EntityType.SKELETON, EntityType.STRAY)

    override val actions = listOf(
        FightAction(intervalTicks = 5, chance = 1.0) { mob, target -> switchWeapon(mob, target) },
        FightAction(intervalTicks = 10, chance = 0.1, cooldownTicks = 20 * 10) { mob, _ -> summonBoneGuard(mob) },
    )

    private val weaponKey = NamespacedKey(plugin, "backup_weapon")

    /** 骨の盾を持っているスケルトン → 回っている骨 */
    private val boneGuards = mutableMapOf<UUID, BoneGuard>()

    private class BoneGuard(val mob: Mob, val bones: MutableList<ItemDisplay>) {
        var angle = 0.0
    }

    init {
        // 全員の骨を1つのタイマーで回す。teleportDuration で間が補われるので2tickごとで滑らかに見える
        plugin.runTimer(GUARD_PERIOD_TICKS, GUARD_PERIOD_TICKS) { rotateBoneGuards() }
    }

    override fun onFightEnd(mob: Mob) {
        // 読み込みが外れたものは元の武器が保存済みなので触らない（次に戦って離れたときに戻る）
        if (mob.isValid) restoreWeapon(mob)
        removeBoneGuard(mob)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAttacked(event: EntityDamageByEntityEvent) {
        val guard = boneGuards[event.entity.uniqueId] ?: return
        event.isCancelled = true
        guard.bones.removeLastOrNull()?.remove()
        guard.mob.world.playSound(guard.mob.location, Sound.ENTITY_SKELETON_HURT, 0.8f, 0.6f)
        if (guard.bones.isEmpty()) boneGuards.remove(guard.mob.uniqueId)
    }

    @EventHandler
    fun onDeath(event: EntityDeathEvent) {
        if (event.entityType !in types) return
        // 斧に持ち替えたまま倒されたら、元の武器をバニラと同じ確率で落とす
        val weapon = takeBackupWeapon(event.entity) ?: return
        if (!weapon.isEmpty && Random.nextDouble() < WEAPON_DROP_CHANCE) event.drops += weapon
    }

    /** 停止時に、回っている骨を消す */
    fun clear() {
        boneGuards.values.forEach { guard -> guard.bones.forEach { it.remove() } }
        boneGuards.clear()
    }

    private fun switchWeapon(mob: Mob, target: LivingEntity): Boolean {
        val distanceSquared = mob.location.distanceSquared(target.location)
        val switched = mob.persistentDataContainer.has(weaponKey)
        if (!switched) {
            // 経路探索は重いので、距離で絞ってから行う
            if (distanceSquared > MELEE_RANGE * MELEE_RANGE) return false
            if (mob.pathfinder.findPath(target)?.canReachFinalPoint() != true) return false
            val equipment = mob.equipment
            val current = equipment.itemInMainHand
            // 素手は空の配列で表す（空のアイテムは serializeAsBytes できない）
            val bytes = if (current.isEmpty) ByteArray(0) else current.serializeAsBytes()
            mob.persistentDataContainer.set(weaponKey, PersistentDataType.BYTE_ARRAY, bytes)
            equipment.setItemInMainHand(ItemStack(Material.WOODEN_AXE))
            equipment.itemInMainHandDropChance = 0f
            return true
        }
        if (distanceSquared < RANGED_RANGE * RANGED_RANGE) return false
        restoreWeapon(mob)
        return true
    }

    private fun restoreWeapon(mob: LivingEntity) {
        val weapon = takeBackupWeapon(mob) ?: return
        val equipment = mob.equipment ?: return
        equipment.setItemInMainHand(weapon)
        equipment.itemInMainHandDropChance = WEAPON_DROP_CHANCE.toFloat()
    }

    private fun takeBackupWeapon(mob: LivingEntity): ItemStack? {
        val bytes = mob.persistentDataContainer.get(weaponKey, PersistentDataType.BYTE_ARRAY) ?: return null
        mob.persistentDataContainer.remove(weaponKey)
        return if (bytes.isEmpty()) ItemStack.empty() else ItemStack.deserializeBytes(bytes)
    }

    private fun summonBoneGuard(mob: Mob): Boolean {
        if (mob.uniqueId in boneGuards) return false
        val center = mob.location
        val bones = MutableList(BONE_COUNT) {
            mob.world.spawn(center, ItemDisplay::class.java) {
                it.setItemStack(ItemStack(Material.BONE))
                it.isPersistent = false
                it.teleportDuration = GUARD_PERIOD_TICKS.toInt()
            }
        }
        boneGuards[mob.uniqueId] = BoneGuard(mob, bones)
        mob.world.playSound(center, Sound.ENTITY_SKELETON_AMBIENT, 1f, 0.6f)
        return true
    }

    private fun removeBoneGuard(mob: Mob) {
        boneGuards.remove(mob.uniqueId)?.bones?.forEach { it.remove() }
    }

    private fun rotateBoneGuards() {
        if (boneGuards.isEmpty()) return
        val iterator = boneGuards.values.iterator()
        while (iterator.hasNext()) {
            val guard = iterator.next()
            if (!guard.mob.isValid) {
                guard.bones.forEach { it.remove() }
                iterator.remove()
                continue
            }
            guard.angle = (guard.angle + GUARD_ROTATE_PER_UPDATE) % (2 * PI)
            val base = guard.mob.location
            base.pitch = 0f
            val step = 2 * PI / guard.bones.size
            for ((i, bone) in guard.bones.withIndex()) {
                val angle = guard.angle + step * i
                val location = base.clone().add(GUARD_RADIUS * cos(angle), GUARD_HEIGHT, GUARD_RADIUS * sin(angle))
                // 骨の向きを回る方向に合わせる
                location.yaw = Math.toDegrees(-angle).toFloat()
                bone.teleport(location)
            }
        }
    }

    companion object {
        private const val MELEE_RANGE = 4.0
        private const val RANGED_RANGE = 6.0
        private const val WEAPON_DROP_CHANCE = 0.085

        private const val BONE_COUNT = 3
        private const val GUARD_PERIOD_TICKS = 2L
        private const val GUARD_RADIUS = 1.2
        private const val GUARD_HEIGHT = 0.8

        /** 旧版と同じく、約1.8秒（36tick）で1周する */
        private const val GUARD_ROTATE_PER_UPDATE = 2 * PI / 36 * GUARD_PERIOD_TICKS
    }
}
