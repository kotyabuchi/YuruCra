package com.github.kotyabuchi.YuruCra.Item

import com.destroystokyo.paper.event.inventory.PrepareResultEvent
import com.github.kotyabuchi.YuruCra.Event.CustomEventCaller
import com.github.kotyabuchi.YuruCra.Event.PrepareItemExtensionCraftEvent
import com.github.kotyabuchi.YuruCra.Utility.hasDurability
import com.github.kotyabuchi.YuruCra.Utility.isArmors
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerItemMendEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.GrindstoneInventory
import kotlin.math.round

object ItemExtensionManager: Listener {

    @EventHandler
    fun onDamage(event: PlayerItemDamageEvent) {
        if (event.isCancelled) return
        event.isCancelled = true
        val itemStack = event.item
        val itemExtension = ItemExtension(itemStack)
        val beforeDurability = itemExtension.durability
        val damage = event.damage

        if (itemStack.type.isArmors()) {
            itemExtension.damage(damage)
        } else {
            itemExtension.damage(damage, event.player)
        }
        itemExtension.applyDurability().applySetting()

        val player = event.player
        if (itemExtension.durability == 1 && itemStack.type == Material.ELYTRA) {
            player.world.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
            return
        }
        if (itemExtension.durability > 0) return
        if (beforeDurability > 1 && damage > 1) {
            itemExtension.mending(1, player).applyDurability().applySetting()
            return
        }

        val itemBreakEvent = PlayerItemBreakEvent(player, itemStack)
        CustomEventCaller.callEvent(itemBreakEvent)
        player.world.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
        itemStack.subtract(Int.MAX_VALUE)
    }

    @EventHandler
    fun onCraft(event: PrepareItemCraftEvent) {
        if (event is PrepareItemExtensionCraftEvent) return
        val inv = event.inventory
        val result = if (event.isRepair) {
            inv.result
        } else {
            val recipe = event.recipe ?: return
            recipe.result
        } ?: return
        if (!result.type.hasDurability()) return
        val extensionItemResult = ItemExtension(result).applySetting()
        val prepareItemExtensionCraftEvent = PrepareItemExtensionCraftEvent(extensionItemResult, inv, event.view, event.isRepair)
        CustomEventCaller.callEvent(prepareItemExtensionCraftEvent)
        inv.result = prepareItemExtensionCraftEvent.result.itemStack
    }

    @EventHandler
    fun onCombined(event: PrepareAnvilEvent) {
        val result = event.result ?: return
        if (!result.type.hasDurability()) return
        val resultExtension = ItemExtension(result)
        val inv = event.inventory as? AnvilInventory ?: return
        val firstItem = inv.firstItem ?: return
        val secondItem = inv.secondItem ?: return
        resultExtension.applyUnbreaking()
        val mendAmount = when {
            firstItem.type == secondItem.type -> {
                ItemExtension(secondItem).durability
            }
            secondItem.canRepair(firstItem) -> {
                val mendPerItem = resultExtension.baseMaxDurability * (1.0 / resultExtension.repairCost)
                round(secondItem.amount * mendPerItem).toInt()
            }
            else -> return
        }
        resultExtension.mending(mendAmount).applyDurability().applySetting()
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onMendItem(event: PlayerItemMendEvent) {
        ItemExtension(event.item).mending(event.repairAmount, event.player).applyDurability().applySetting()
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onEnchant(event: EnchantItemEvent) {
        val level = event.enchantsToAdd[Enchantment.DURABILITY] ?: return
        ItemExtension(event.item).applyUnbreaking(level).applyDurability().applySetting()
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onSmithing(event: PrepareSmithingEvent) {
        val result = event.result ?: return
        event.result = ItemExtension(result).applyUnbreaking().applyDurability().applySetting().itemStack
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onGrind(event: PrepareResultEvent) {
        if (event.inventory !is GrindstoneInventory) return
        val result = event.result ?: return
        event.result = ItemExtension(result).applyUnbreaking().applyDurability().applySetting().itemStack
    }
}