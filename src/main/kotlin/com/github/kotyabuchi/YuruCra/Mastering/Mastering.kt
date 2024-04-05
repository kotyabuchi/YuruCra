package com.github.kotyabuchi.YuruCra.Mastering

import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Mastering.Skill.PassiveSkill
import com.github.kotyabuchi.YuruCra.Mastering.Skill.Skill
import com.github.kotyabuchi.YuruCra.Mastering.Skill.SkillCommand
import com.github.kotyabuchi.YuruCra.Mastering.Skill.ToggleSkill
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import java.time.Duration

abstract class Mastering(val masteringName: String): Listener {

    private val main: Main = Main.instance
    private val targetTool: MutableList<Material> = mutableListOf()
    private val castingPlayerList: MutableList<Player> = mutableListOf()
    private val castingCommandMap: MutableMap<Player, String> = mutableMapOf()
    private val skillMap: MutableMap<SkillCommand, Skill> = mutableMapOf()
    private val passiveSkillSet: MutableSet<PassiveSkill> = mutableSetOf()

    private val commandTitleTime = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)

    @EventHandler
    fun modeChange(event: PlayerSwapHandItemsEvent) {
        val player = event.player.getStatus()
        if (player.isSneaking) return
        if (!targetTool.contains(event.offHandItem.type)) return
        event.isCancelled = true
        if (castingPlayerList.contains(player)) {
            activeSkill(player)
            castingPlayerList.remove(player)
            castingCommandMap.remove(player)
        } else {
            castingPlayerList.add(player)
            castingCommandMap[player] = ""
            player.sendActionBar(Component.text("SpellCast Enabled", NamedTextColor.GREEN))
            player.showTitle(Title.title(Component.empty(), Component.text("- - -"), commandTitleTime))
            player.world.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onClick(event: PlayerInteractEvent) {
//        if (event is PlayerInteractBlockEvent) return
        val player = event.player.getStatus()
        if (!castingPlayerList.contains(player)) return
        event.isCancelled = true
        if (event.hand != EquipmentSlot.HAND) return
        val action = event.action
        if (action == Action.PHYSICAL) return
        castingCommandMap[player]?.let {
            val thisTimeAction = if (action.name.startsWith("LEFT_CLICK")) {
                player.playSound(player.eyeLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.3f)
                "L"
            } else {
                player.playSound(player.eyeLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 0.7f)
                "R"
            }
            val newActionStr = it + thisTimeAction
            castingCommandMap[player] = newActionStr
            var subTitle = Component.text("")
            val actionLength = newActionStr.length

            repeat(3) { repeatTime ->
                subTitle = if (actionLength > repeatTime) {
                    val actionChar = newActionStr[repeatTime]
                    subTitle.append(Component.text(actionChar, if (actionChar == 'L') NamedTextColor.GREEN else NamedTextColor.RED))
                } else {
                    subTitle.append(Component.text("-"))
                }
                if (repeatTime < 2) {
                    subTitle = subTitle.append(Component.text(" "))
                }
            }

            player.showTitle(Title.title(Component.empty(), subTitle, commandTitleTime))
            if (actionLength == 3) {
                activeSkill(player)
                castingPlayerList.remove(player)
                castingCommandMap.remove(player)
            }
        }
    }

    @EventHandler
    fun onClick(event: PlayerItemHeldEvent) {
        val player = event.player
        if (!castingPlayerList.contains(player)) return
        castingPlayerList.remove(player)
        castingCommandMap.remove(player)
        player.playSound(player.eyeLocation, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f)
        player.sendActionBar(Component.text("SpellCast canceled", NamedTextColor.RED))
    }

    fun getTool(): List<Material> {
        return targetTool
    }

    protected fun addTool(tool: Material): Mastering {
        targetTool.add(tool)
        return this
    }

    fun isValidTool(tool: Material): Boolean {
        return targetTool.contains(tool)
    }

    protected fun registerSkill(skillCommand: SkillCommand, skill: Skill) {
        skillMap[skillCommand] = skill
        main.registerEvent(skill)
    }

    protected fun registerPassiveSkill(skill: PassiveSkill) {
        passiveSkillSet.add(skill)
        main.registerEvent(skill)
    }

    fun getSkills(): Map<SkillCommand, Skill> {
        return skillMap
    }

    fun getPassiveSkills(): Set<PassiveSkill> {
        return passiveSkillSet
    }

    private fun activeSkill(player: PlayerStatus) {
        val castingAction = castingCommandMap[player] ?: return
        try {
            val skillCommand = SkillCommand.valueOf(castingAction)
            val skill = skillMap[skillCommand]
            if (skill == null) {
                notRegisterActionNotice(player)
            } else {
                val level = player.masteringManager.getLevel(this)
                if (skill is ToggleSkill) {
                    skill.toggleSkill(player, level)
                } else {
                    skill.enableSkill(player, level)
                }
            }
        } catch (e: IllegalArgumentException) {
            notRegisterActionNotice(player)
        }
    }

    private fun notRegisterActionNotice(player: Player) {
        player.playSound(player.eyeLocation, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f)
        player.sendActionBar(Component.text("Not registered skill", NamedTextColor.RED))
    }

    open fun levelUpEvent(player: Player) {}
}