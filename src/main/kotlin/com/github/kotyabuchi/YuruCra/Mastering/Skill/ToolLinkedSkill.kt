package com.github.kotyabuchi.YuruCra.Mastering.Skill

import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import java.util.*

interface ToolLinkedSkill: ActiveSkill {

    val skillItemBackup: MutableMap<UUID, ItemStack>

    @EventHandler
    fun onClickItem(event: InventoryClickEvent) {
        val player = (event.whoClicked as? Player)?.getStatus() ?: return
        if (!isEnabledSkill(player)) return
        if (event.clickedInventory == null) return
        if (event.slot != player.inventory.heldItemSlot) return
        event.isCancelled = true
        disableSkill(player)
    }

    @EventHandler
    fun onBreakItem(event: PlayerItemBreakEvent) {
        val player = event.player.getStatus()
        if (isEnabledSkill(player)) {
            skillItemBackup.remove(player.uniqueId)
            disableSkill(player)
        }
    }

    @EventHandler
    fun onChangeItemSlot(event: PlayerItemHeldEvent) {
        val player = event.player.getStatus()
        if (isEnabledSkill(player)) {
            disableSkill(player)
        }
    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player.getStatus()
        if (isEnabledSkill(player)) {
            player.inventory.setItemInMainHand(event.itemDrop.itemStack)
            event.itemDrop.remove()
            disableSkill(player)
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.entity.getStatus()
        if (isEnabledSkill(player)) {
            disableSkill(player)
        }
    }

    @EventHandler
    fun onLogout(event: PlayerQuitEvent) {
        val player = event.player.getStatus()
        if (isEnabledSkill(player)) {
            disableSkill(player)
        }
    }
}