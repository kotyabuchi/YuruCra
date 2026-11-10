package com.github.kotyabuchi.yurucra.job

import com.github.kotyabuchi.yurucra.player.JobStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.entity.Wolf
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * 飼っている狼のレベル。狼が攻撃すると経験値がたまり、レベルに応じて体力・攻撃力・防御力・移動速度と自然回復が上がる。
 * レベルと経験値は狼自身に保存する。名前の前に「Lv.5」と出し、名札で付けた名前は別に覚えておく。
 */
class WolfLevels(plugin: Plugin) {

    private val levelKey = NamespacedKey(plugin, "pet_level")
    private val expKey = NamespacedKey(plugin, "pet_exp")
    private val nameKey = NamespacedKey(plugin, "pet_name")

    private val modifierKey = NamespacedKey(plugin, "pet_level_bonus")

    fun level(wolf: Wolf): Int = wolf.persistentDataContainer.get(levelKey, PersistentDataType.INTEGER) ?: 0

    /** 経験値を足す。初めてなら Lv.1 から始める */
    fun addExp(wolf: Wolf, exp: Double) {
        val pdc = wolf.persistentDataContainer
        var level = level(wolf)
        if (level == 0) {
            level = 1
            setLevel(wolf, level)
        }
        if (level >= MAX_LEVEL) return
        var current = (pdc.get(expKey, PersistentDataType.DOUBLE) ?: 0.0) + exp
        val before = level
        while (level < MAX_LEVEL && current >= JobStatus.requiredExp(level)) {
            current -= JobStatus.requiredExp(level)
            level++
        }
        pdc.set(expKey, PersistentDataType.DOUBLE, if (level >= MAX_LEVEL) 0.0 else current)
        if (level == before) return
        setLevel(wolf, level)
        wolf.world.spawnParticle(Particle.HAPPY_VILLAGER, wolf.location.add(0.0, 0.8, 0.0), 8, 0.4, 0.3, 0.4)
        wolf.world.playSound(wolf.location, Sound.ENTITY_WOLF_AMBIENT, 1f, 1.4f)
        (wolf.owner as? Player)?.sendActionBar(
            Component.text("${baseName(wolf) ?: "Wolf"} → Lv.$level", NamedTextColor.GREEN)
        )
    }

    /** 5秒ごとに呼ぶ。レベルの付いた狼を 0.5 + レベル×0.05 回復する（瀕死の狼は除く） */
    fun regenerate(wolves: Collection<Wolf>, isDying: (Wolf) -> Boolean) {
        for (wolf in wolves) {
            val level = level(wolf)
            if (level == 0 || !wolf.isValid || isDying(wolf)) continue
            val max = wolf.getAttribute(Attribute.MAX_HEALTH)?.value ?: continue
            if (wolf.health >= max) continue
            // 回復のイベントを起こして、ダメージ表示に回復量を出す
            wolf.heal(minOf(max - wolf.health, REGEN_BASE + level * REGEN_PER_LEVEL), EntityRegainHealthEvent.RegainReason.REGEN)
        }
    }

    /** 名札で名前を付けたとき。付けた名前を覚えて、レベル付きの名前を返す */
    fun rename(wolf: Wolf, name: Component): Component {
        wolf.persistentDataContainer.set(nameKey, PersistentDataType.STRING, PlainTextComponentSerializer.plainText().serialize(name))
        return displayName(level(wolf).coerceAtLeast(1), baseName(wolf))
    }

    private fun setLevel(wolf: Wolf, level: Int) {
        val pdc = wolf.persistentDataContainer
        // レベル表示を付ける前に、名札の名前を覚えておく
        if (!pdc.has(levelKey)) {
            wolf.customName()?.let { pdc.set(nameKey, PersistentDataType.STRING, PlainTextComponentSerializer.plainText().serialize(it)) }
        }
        pdc.set(levelKey, PersistentDataType.INTEGER, level)
        applyStats(wolf, level)
        wolf.customName(displayName(level, baseName(wolf)))
    }

    private fun applyStats(wolf: Wolf, level: Int) {
        val bonus = level - 1
        setBonus(wolf, Attribute.MAX_HEALTH, bonus * HEALTH_PER_LEVEL, AttributeModifier.Operation.ADD_NUMBER)
        setBonus(wolf, Attribute.ATTACK_DAMAGE, bonus * ATTACK_PER_LEVEL, AttributeModifier.Operation.ADD_NUMBER)
        setBonus(wolf, Attribute.ARMOR, bonus * ARMOR_PER_LEVEL, AttributeModifier.Operation.ADD_NUMBER)
        setBonus(wolf, Attribute.MOVEMENT_SPEED, bonus * SPEED_PER_LEVEL, AttributeModifier.Operation.ADD_SCALAR)
    }

    private fun setBonus(wolf: Wolf, attribute: Attribute, amount: Double, operation: AttributeModifier.Operation) {
        val instance = wolf.getAttribute(attribute) ?: return
        instance.removeModifier(modifierKey)
        if (amount > 0) instance.addModifier(AttributeModifier(modifierKey, amount, operation))
    }

    private fun baseName(wolf: Wolf): String? = wolf.persistentDataContainer.get(nameKey, PersistentDataType.STRING)

    private fun displayName(level: Int, name: String?): Component {
        val label = Component.text("Lv.$level", NamedTextColor.GRAY)
        return if (name.isNullOrEmpty()) label else label.append(Component.text(" $name", NamedTextColor.WHITE))
    }

    private companion object {
        const val MAX_LEVEL = 50
        const val HEALTH_PER_LEVEL = 1.0
        const val ATTACK_PER_LEVEL = 0.1
        const val ARMOR_PER_LEVEL = 0.2
        const val SPEED_PER_LEVEL = 0.005
        const val REGEN_BASE = 0.5
        const val REGEN_PER_LEVEL = 0.05
    }
}
