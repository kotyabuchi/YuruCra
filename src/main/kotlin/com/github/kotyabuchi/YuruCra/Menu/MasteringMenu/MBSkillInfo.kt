package com.github.kotyabuchi.YuruCra.Menu.MasteringMenu

import com.github.kotyabuchi.MCRPG.normalize
import com.github.kotyabuchi.YuruCra.Mastering.Skill.Skill
import com.github.kotyabuchi.YuruCra.Mastering.Skill.SkillCommand
import com.github.kotyabuchi.YuruCra.Menu.Button.ClickSound
import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class MBSkillInfo(command: SkillCommand, skill: Skill): MenuButton() {
    override val material: Material = Material.WRITABLE_BOOK
    override val displayName: TextComponent = Component.text(skill.displayName).normalize()
    override val lore: List<TextComponent> = mutableListOf<TextComponent>().apply {
        add(Component.text("NeedLevel: ", NamedTextColor.GRAY).append(Component.text(skill.needLevel, NamedTextColor.GOLD)))
        add(Component.text("Cost: ", NamedTextColor.GRAY).append(Component.text(skill.cost, NamedTextColor.AQUA)))
        if (skill.coolTime != 0L) add(Component.text("CoolTime: ", NamedTextColor.GRAY).append(Component.text("${skill.coolTime / 1000}s", NamedTextColor.WHITE)))
        add(Component.text("Command: ", NamedTextColor.GRAY).append(Component.text(command.name, NamedTextColor.WHITE)))
        add(Component.text("===============", NamedTextColor.GRAY))
        add(Component.text(skill.description, NamedTextColor.WHITE))
    }
    override val clickSound: ClickSound? = null
}