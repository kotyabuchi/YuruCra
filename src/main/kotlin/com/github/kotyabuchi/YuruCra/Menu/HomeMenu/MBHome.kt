package com.github.kotyabuchi.YuruCra.Menu.HomeMenu

import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Player.HomeInfo
import com.github.kotyabuchi.YuruCra.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import java.text.DecimalFormat

class MBHome(private val homeInfo: HomeInfo): MenuButton() {
    override val material: Material = homeInfo.icon
    override val displayName: TextComponent = homeInfo.name.toComponent()
    override val lore: List<TextComponent> = mutableListOf<TextComponent>().apply {
        val location = homeInfo.location
        val df = DecimalFormat("#.#")
        add(Component.text("X: ${df.format(location.x)}", NamedTextColor.GRAY))
        add(Component.text("Y: ${df.format(location.y)}", NamedTextColor.GRAY))
        add(Component.text("Z: ${df.format(location.z)}", NamedTextColor.GRAY))
        add(Component.empty())
        add(Component.text("左クリック: テレポート", NamedTextColor.GREEN))
    }

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val player = info.player
        player.teleport(homeInfo.location)
    }

    override fun doRightClickAction(info: ButtonClickInfo) {

    }
}