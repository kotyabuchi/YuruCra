package com.github.kotyabuchi.YuruCra.Menu.MainMenu

import com.github.kotyabuchi.MCRPG.normalize
import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import com.github.kotyabuchi.YuruCra.Utility.addItemOrDrop
import com.github.kotyabuchi.YuruCra.Utility.removeSimilar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.scheduler.BukkitRunnable

object MainMenuCompass: Listener {

    private val main: Main = Main.instance
    private val menuCompass = ItemStack(Material.COMPASS).apply {
        this.editMeta {
            it.displayName(Component.text("Menu", NamedTextColor.GOLD).normalize())
        }
    }

    @EventHandler
    fun onLogin(event: PlayerLoginEvent) {
        object : BukkitRunnable() {
            override fun run() {
                val player = event.player
                val inv = player.inventory
                inv.getItem(8)?.let {
                    if (!it.isSimilar(menuCompass)) inv.addItemOrDrop(player, it)
                }
                inv.removeSimilar(menuCompass)
                event.player.inventory.setItem(8, menuCompass)
            }
        }.runTaskLater(main, 0)
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val compasses = mutableListOf<ItemStack>()
        event.drops.forEach {
            if (it.isSimilar(menuCompass)) {
                event.itemsToKeep.add(it)
                compasses.add(it)
            }
        }
        event.drops.removeAll(compasses)
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        if (event.itemDrop.itemStack.isSimilar(menuCompass)) event.isCancelled = true
    }

    @EventHandler
    fun onClickCompass(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (!event.action.name.startsWith("RIGHT_CLICK")) return
        val player = event.player
        val item = player.inventory.getItem(EquipmentSlot.HAND)
        if (!item.isSimilar(menuCompass)) return
        event.isCancelled = true
        player.getStatus().openMenu(MainMenu)
    }

    @EventHandler
    fun onClickInvCompass(event: InventoryClickEvent) {
        if (event.isCancelled) return
        val player = event.whoClicked as? Player ?: return
        val status = player.getStatus()
        if (event.click == ClickType.NUMBER_KEY) {
            val cache = event.currentItem?.clone()
            object : BukkitRunnable() {
                override fun run() {
                    val item = event.currentItem ?: return
                    if (!item.isSimilar(menuCompass)) return
                    event.currentItem = cache
                    player.inventory.setItem(8, menuCompass)
                    status.openMenu(MainMenu)
                }
            }.runTaskLater(main, 0)
        } else {
            val item = event.currentItem ?: return
            val clickedInventory = event.clickedInventory ?: return
            if (clickedInventory !is PlayerInventory) return
            if (!item.isSimilar(menuCompass)) return
            event.isCancelled = true
            event.cursor?.let {
                player.inventory.addItemOrDrop(player, it)
                event.setCursor(null)
            }
            status.openMenu(MainMenu)
        }
    }
}