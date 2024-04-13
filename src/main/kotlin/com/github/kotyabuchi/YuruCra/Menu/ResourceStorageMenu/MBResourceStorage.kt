package com.github.kotyabuchi.YuruCra.Menu.ResourceStorageMenu

import com.github.kotyabuchi.YuruCra.Menu.Button.ClickSound
import com.github.kotyabuchi.YuruCra.Menu.Button.MenuButton
import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.Sound

object MBResourceStorage: MenuButton() {
    override val material: Material = Material.ENDER_CHEST
    override val displayName: TextComponent = Component.text("Resource Storage")
    override val clickSound: ClickSound = ClickSound(Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f)

    override fun doLeftClickAction(info: ButtonClickInfo) {
        val status = info.player.getStatus()
        status.openMenu(ResourceStorageMenu(status.resourceStorage))
    }
}