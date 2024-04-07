package com.github.kotyabuchi.YuruCra.Menu.MasteringMenu

import com.github.kotyabuchi.YuruCra.Mastering.Mastering
import com.github.kotyabuchi.YuruCra.Menu.FrameType
import com.github.kotyabuchi.YuruCra.Menu.Menu
import com.github.kotyabuchi.YuruCra.Utility.upperCamelCase
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class SkillInfoMenu(private val player: Player, private val mastering: Mastering, private val isPassiveSkill: Boolean = false): Menu(
    title = if (isPassiveSkill) {
        Component.text("${mastering.masteringName.upperCamelCase()} Passive skills")
    } else {
        Component.text("${mastering.masteringName.upperCamelCase()} Active skills")
    },
    menuRow = if (isPassiveSkill) mastering.getPassiveSkills().size else mastering.getSkills().values.size,
    frames = setOf(FrameType.TOP, FrameType.SIDE)) {

    init {
        createMenu()
    }

    override fun createMenu() {
        if (isPassiveSkill) {
            mastering.getPassiveSkills().forEach { skill ->
                addButton(MBPassiveSkillInfo(player, skill, this))
            }
        } else {
            mastering.getSkills().forEach { (command, skill) ->
                addButton(MBSkillInfo(command, skill))
            }
        }
    }
}