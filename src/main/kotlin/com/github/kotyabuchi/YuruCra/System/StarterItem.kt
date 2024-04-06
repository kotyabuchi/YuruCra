package com.github.kotyabuchi.YuruCra.System

import com.github.kotyabuchi.YuruCra.Main
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.inventory.ItemStack

object StarterItem: Listener {

    private val main: Main = Main.instance

    @EventHandler
    fun onLogin(event: PlayerLoginEvent) {
        val player = event.player

        if (main.server.offlinePlayers.contains(player)) return

        val inventory = player.inventory
        inventory.setItem(0, ItemStack(Material.WOODEN_SWORD))
        inventory.setItem(1, ItemStack(Material.WOODEN_PICKAXE))
        inventory.setItem(2, ItemStack(Material.WOODEN_SHOVEL))
        inventory.setItem(3, ItemStack(Material.WOODEN_AXE))
        inventory.setItem(7, ItemStack(Material.APPLE, 8))
    }
}