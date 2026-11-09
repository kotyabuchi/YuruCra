package com.github.kotyabuchi.yurucra.feature

import com.github.kotyabuchi.yurucra.util.floorDigits
import com.github.kotyabuchi.yurucra.util.runTimer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.attribute.Attribute
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Display
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.plugin.Plugin
import java.math.BigDecimal
import kotlin.random.Random

/**
 * 生き物がダメージを受けたら赤、回復したら緑で、頭の近くに数値を浮かべる。
 *
 * 数値は保存されない TextDisplay。1つのタイマーで、出た次のtickに上へ動かし（teleportDuration で滑らかに動く）、
 * 寿命が来たら消す。誰からも見えていない生き物の分は出さない。
 */
class DamagePopup(private val plugin: Plugin) : Listener {

    private class Popup(val display: TextDisplay, val removeAt: Int)

    /** 出したばかりで、次のtickに上へ動かすもの */
    private val rising = ArrayList<Popup>()

    /** 寿命が同じなので、出した順に並べておけば先頭から消せる */
    private val alive = ArrayDeque<Popup>()

    init {
        plugin.runTimer(1, 1) { tick() }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        val amount = event.finalDamage
        if (amount <= MIN_AMOUNT) return
        val entity = event.entity as? LivingEntity ?: return
        popup(entity, amount, NamedTextColor.RED)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onRegain(event: EntityRegainHealthEvent) {
        val entity = event.entity as? LivingEntity ?: return
        // 体力が満タンでもイベントは来るので、実際に回復する分（最大体力まで）だけを出す
        val maxHealth = entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        val amount = minOf(event.amount, maxHealth - entity.health)
        if (amount <= MIN_AMOUNT) return
        popup(entity, amount, NamedTextColor.GREEN)
    }

    /** 停止時に残っている数値を消す（/reload などでワールドに残らないように） */
    fun clear() {
        rising.forEach { it.display.remove() }
        alive.forEach { it.display.remove() }
        rising.clear()
        alive.clear()
    }

    private fun popup(entity: LivingEntity, amount: Double, color: NamedTextColor) {
        if (entity is ArmorStand) return
        // 周りに誰もいない生き物の分は出さない。プレイヤー自身は自分の数値を見られるように出す
        if (entity !is Player && entity.trackedBy.isEmpty()) return

        val eyeHeight = entity.eyeHeight
        val location = entity.location.add(
            Random.nextDouble(-SPREAD, SPREAD),
            eyeHeight + Random.nextDouble(0.0, eyeHeight / 4 + 0.01),
            Random.nextDouble(-SPREAD, SPREAD),
        )
        val display = entity.world.spawn(location, TextDisplay::class.java) {
            it.isPersistent = false
            it.billboard = Display.Billboard.CENTER
            it.text(Component.text(format(amount), color))
            it.isShadowed = true
            it.backgroundColor = Color.fromARGB(0, 0, 0, 0)
            it.teleportDuration = LIFETIME_TICKS
        }
        rising += Popup(display, plugin.server.currentTick + LIFETIME_TICKS)
    }

    private fun tick() {
        if (rising.isNotEmpty()) {
            for (popup in rising) {
                val display = popup.display
                if (display.isValid) display.teleport(display.location.add(0.0, RISE, 0.0))
                alive.addLast(popup)
            }
            rising.clear()
        }
        val now = plugin.server.currentTick
        while (alive.isNotEmpty() && alive.first().removeAt <= now) {
            alive.removeFirst().display.remove()
        }
    }

    companion object {
        private const val MIN_AMOUNT = 0.01
        private const val LIFETIME_TICKS = 30
        private const val SPREAD = 0.75
        private const val RISE = 0.6

        /** 小数第2位で切り捨て、末尾の0は省く（4.0 → "4"、3.50 → "3.5"） */
        fun format(amount: Double): String =
            BigDecimal.valueOf(amount.floorDigits(2)).stripTrailingZeros().toPlainString()
    }
}
