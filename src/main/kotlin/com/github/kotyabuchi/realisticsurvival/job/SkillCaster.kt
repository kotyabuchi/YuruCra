package com.github.kotyabuchi.realisticsurvival.job

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.skill.SkillCommand
import com.github.kotyabuchi.realisticsurvival.skill.ToggleSkill
import com.github.kotyabuchi.realisticsurvival.util.playErrorSound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import java.time.Duration
import java.util.UUID

/**
 * スキルの詠唱操作。
 * ジョブの道具を持ってオフハンド切替キー(F)で詠唱モードに入り、左右クリックを3回入力すると発動する。
 * 詠唱中にもう一度Fを押すと、そこまでの入力で発動を試みる。
 */
class SkillCaster(private val plugin: RealisticSurvival) : Listener {

    private class CastState(val job: Job) {
        var command = ""
    }

    private val casting = HashMap<UUID, CastState>()
    private val titleTimes = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)

    @EventHandler
    fun onSwap(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        if (player.isSneaking) return
        // offHandItem は「オフハンドに移ろうとしているアイテム」= 今メインハンドに持っている道具
        val job = plugin.jobs.byTool(event.offHandItem.type) ?: return
        event.isCancelled = true

        val state = casting.remove(player.uniqueId)
        if (state != null) {
            activate(player, state)
        } else {
            casting[player.uniqueId] = CastState(job)
            player.sendActionBar(Component.text("Cast Mode Enabled", NamedTextColor.GREEN))
            showCommand(player, "")
            player.world.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val state = casting[player.uniqueId] ?: return
        event.isCancelled = true
        if (event.hand != EquipmentSlot.HAND) return
        val action = event.action
        if (action == Action.PHYSICAL) return

        val input = if (action.isLeftClick) {
            player.playSound(player.eyeLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.3f)
            'L'
        } else {
            player.playSound(player.eyeLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 0.7f)
            'R'
        }
        state.command += input
        showCommand(player, state.command)
        if (state.command.length == COMMAND_LENGTH) {
            casting.remove(player.uniqueId)
            activate(player, state)
        }
    }

    @EventHandler
    fun onChangeSlot(event: PlayerItemHeldEvent) {
        val player = event.player
        if (casting.remove(player.uniqueId) == null) return
        player.playErrorSound()
        player.sendActionBar(Component.text("Cast Mode canceled", NamedTextColor.RED))
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        casting.remove(event.player.uniqueId)
    }

    private fun activate(player: Player, state: CastState) {
        val command = SkillCommand.entries.find { it.name == state.command }
        val skill = command?.let { state.job.activeSkills[it] }
        if (skill == null) {
            player.playErrorSound()
            player.sendActionBar(Component.text("Not registered skill", NamedTextColor.RED))
            return
        }
        val level = state.job.level(player)
        if (skill is ToggleSkill) skill.toggle(player, level) else skill.activate(player, level)
    }

    /** 入力済みのコマンドを「L R -」のようにタイトルで見せる */
    private fun showCommand(player: Player, command: String) {
        var subtitle = Component.empty()
        repeat(COMMAND_LENGTH) { index ->
            val part = command.getOrNull(index)
            subtitle = subtitle.append(
                when (part) {
                    'L' -> Component.text("L", NamedTextColor.GREEN)
                    'R' -> Component.text("R", NamedTextColor.RED)
                    else -> Component.text("-")
                }
            )
            if (index < COMMAND_LENGTH - 1) subtitle = subtitle.append(Component.text(" "))
        }
        player.showTitle(Title.title(Component.empty(), subtitle, titleTimes))
    }

    companion object {
        private const val COMMAND_LENGTH = 3
    }
}
