package com.github.kotyabuchi.YuruCra.System

import com.github.kotyabuchi.YuruCra.Menu.FrameType
import com.github.kotyabuchi.YuruCra.Menu.TestMenu
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

object Debug: Listener {

    @EventHandler
    fun onClick(event: PlayerInteractEvent) {
        val player = event.player.getStatus()
        if (player.isSneaking) {
            player.openMenu(TestMenu(player.name, setOf()))
        } else if (event.action.name.startsWith("RIGHT") && event.hand == EquipmentSlot.HAND) {
            player.openMenu(TestMenu(player.name, setOf(FrameType.TOP)))
        } else if (event.action.name.startsWith("LEFT")) {
            player.openMenu(TestMenu(player.name, setOf(FrameType.TOP, FrameType.SIDE)))
        }
    }
}