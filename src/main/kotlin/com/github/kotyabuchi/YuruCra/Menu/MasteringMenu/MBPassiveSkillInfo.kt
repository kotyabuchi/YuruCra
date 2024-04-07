package com.github.kotyabuchi.YuruCra.Menu.MasteringMenu

import com.github.kotyabuchi.MCRPG.normalize
import com.github.kotyabuchi.YuruCra.Mastering.Skill.PassiveSkill
import com.github.kotyabuchi.YuruCra.Menu.Button.ClickSound
import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player

class MBPassiveSkillInfo(private val player: Player, private val skill: PassiveSkill, private val menu: SkillInfoMenu): MenuButton() {
    override val material: Material = Material.WRITABLE_BOOK
    override val displayName: TextComponent = Component.text(skill.displayName).normalize()
    override val lore: List<TextComponent> = mutableListOf<TextComponent>().apply {
        add(Component.text("State: ", NamedTextColor.GRAY).append {
            if (isEnabled) {
                Component.text("Enabled", NamedTextColor.GREEN)
            } else {
                Component.text("Disabled", NamedTextColor.RED)
            }
        })
        add(Component.text("NeedLevel: ", NamedTextColor.GRAY).append(Component.text(skill.needLevel, NamedTextColor.GOLD)))
        add(Component.text("Cost: ", NamedTextColor.GRAY).append(Component.text(skill.cost, NamedTextColor.AQUA)))
        add(Component.text("===============", NamedTextColor.GRAY))
        add(Component.text(skill.description))
        add(Component.empty())
        add(Component.text("Click: ").append {
            if (isEnabled) {
                Component.text("Disable skill", NamedTextColor.RED)
            } else {
                Component.text("Enable skill", NamedTextColor.GREEN)
            }
        })
    }
    override val clickSound: ClickSound? = null
    private val isEnabled: Boolean = skill.isEnabledSkill(player)

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val playerStatus = player.getStatus()
        skill.toggleSkill(player, playerStatus.masteringManager.getLevel(skill.ownerMastering))
        menu.refresh()
        playerStatus.openMenu(menu)
    }
}