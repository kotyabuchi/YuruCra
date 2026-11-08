package com.github.kotyabuchi.realisticsurvival.menu.job

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.job.Job
import com.github.kotyabuchi.realisticsurvival.job.JobType
import com.github.kotyabuchi.realisticsurvival.menu.Menu
import com.github.kotyabuchi.realisticsurvival.menu.MenuButton
import com.github.kotyabuchi.realisticsurvival.skill.PassiveSkill
import com.github.kotyabuchi.realisticsurvival.skill.Skill
import com.github.kotyabuchi.realisticsurvival.skill.SkillCommand
import com.github.kotyabuchi.realisticsurvival.util.LORE_LABEL
import com.github.kotyabuchi.realisticsurvival.util.buttonItem
import com.github.kotyabuchi.realisticsurvival.util.floorDigits
import com.github.kotyabuchi.realisticsurvival.util.loreLine
import com.github.kotyabuchi.realisticsurvival.util.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/** ジョブ一覧。各ジョブのレベルと経験値を表示する */
class JobMenu(plugin: RealisticSurvival, private val player: Player) :
    Menu(plugin, text("Job"), JobType.entries.size, Frame.TOP, Frame.SIDE) {

    override fun build() {
        JobType.entries.forEach { put(JobInfoButton(plugin, plugin.jobs[it], player)) }
    }
}

private class JobInfoButton(plugin: RealisticSurvival, private val job: Job, player: Player) :
    MenuButton(createIcon(plugin, job, player)) {

    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        menu.plugin.menus.open(player, SkillInfoMenu(menu.plugin, player, job, passive = false))
    }

    override fun onRightClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        menu.plugin.menus.open(player, SkillInfoMenu(menu.plugin, player, job, passive = true))
    }

    companion object {
        fun createIcon(plugin: RealisticSurvival, job: Job, player: Player) = run {
            val status = plugin.players.status(player).job(job.type)
            val exp = status.exp.floorDigits(2)
            val next = status.nextLevelExp
            val percent = (exp / next * 100).floorDigits(1)
            buttonItem(
                job.type.icon,
                text(job.type.displayName),
                listOf(
                    loreLine("Level: ", "${status.level}"),
                    loreLine("Need Exp: ", "$exp/$next [$percent%]"),
                    loreLine("Total Exp: ", "${status.totalExp.floorDigits(2)}"),
                    Component.empty(),
                    loreLine("Left Click: ", "Show active skill"),
                    loreLine("Right Click: ", "Show passive skill"),
                ),
            )
        }
    }
}

/** ジョブのスキル一覧。パッシブスキルはクリックでオン・オフできる */
class SkillInfoMenu(
    plugin: RealisticSurvival,
    private val player: Player,
    private val job: Job,
    private val passive: Boolean,
) : Menu(
    plugin,
    text("${job.type.displayName} skills"),
    if (passive) job.passiveSkills.size else job.activeSkills.size,
    Frame.TOP, Frame.SIDE,
) {
    override fun build() {
        if (passive) {
            job.passiveSkills.forEach { put(PassiveSkillButton(player, it)) }
        } else {
            job.activeSkills.forEach { (command, skill) -> put(ActiveSkillButton(command, skill)) }
        }
    }
}

private fun separator() = Component.text("===============", LORE_LABEL)

private class ActiveSkillButton(command: SkillCommand, skill: Skill) : MenuButton(
    buttonItem(
        Material.WRITABLE_BOOK,
        text(skill.displayName),
        buildList {
            add(loreLine("NeedLevel: ", "${skill.needLevel}"))
            if (skill.coolTimeMillis != 0L) add(loreLine("CoolTime: ", "${skill.coolTimeMillis / 1000}s"))
            add(loreLine("Command: ", command.name))
            add(separator())
            add(Component.text(skill.description, LORE_LABEL))
        },
    ),
    clickSound = null,
)

private class PassiveSkillButton(player: Player, private val skill: PassiveSkill) : MenuButton(
    run {
        val enabled = skill.isEnabled(player)
        buttonItem(
            Material.WRITABLE_BOOK,
            text(skill.displayName),
            listOf(
                if (enabled) loreLine("State: ", "Enabled", NamedTextColor.GREEN) else loreLine("State: ", "Disabled", NamedTextColor.RED),
                loreLine("NeedLevel: ", "${skill.needLevel}"),
                separator(),
                Component.text(skill.description, LORE_LABEL),
                Component.empty(),
                if (enabled) loreLine("Click: ", "Disable skill", NamedTextColor.RED) else loreLine("Click: ", "Enable skill", NamedTextColor.GREEN),
            ),
        )
    }
) {
    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        skill.toggle(player, skill.owner.level(player))
        menu.plugin.menus.reopen(player, menu)
    }
}
