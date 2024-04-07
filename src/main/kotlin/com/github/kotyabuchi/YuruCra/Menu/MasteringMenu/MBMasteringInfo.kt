package com.github.kotyabuchi.YuruCra.Menu.MasteringMenu

import com.github.kotyabuchi.YuruCra.Mastering.MasteringType
import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import com.github.kotyabuchi.YuruCra.Utility.floor2Digits
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player

class MBMasteringInfo(private val masteringType: MasteringType, player: Player): MenuButton() {
    override val material: Material = masteringType.getIcon()
    override val displayName: TextComponent = Component.text(masteringType.regularName)
    override val lore: List<TextComponent> = mutableListOf<TextComponent>().apply {
        val masteringStatus = player.getStatus().masteringManager.getMasteringStatus(masteringType.masteringClass)
        add(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(masteringStatus.getLevel(), NamedTextColor.WHITE)))
        add(Component.text("Need Exp: ", NamedTextColor.GRAY).append(Component.text(masteringStatus.getNextLevelExp(), NamedTextColor.WHITE)))
        add(Component.text("Total Exp: ", NamedTextColor.GRAY).append(Component.text(masteringStatus.getTotalExp().floor2Digits(), NamedTextColor.WHITE)))
        add(Component.empty())
        add(Component.text("Left Click: ", NamedTextColor.GRAY).append(Component.text("Show active skills", NamedTextColor.WHITE)))
        add(Component.text("Right Click: ", NamedTextColor.GRAY).append(Component.text("Show passive skills", NamedTextColor.WHITE)))
    }

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val player = info.player
        player.getStatus().openMenu(SkillInfoMenu(player, masteringType.masteringClass))
    }

    override fun doRightClickAction(info: ButtonClickInfo) {
        val player = info.player
        player.getStatus().openMenu(SkillInfoMenu(player, masteringType.masteringClass, true))
    }
}