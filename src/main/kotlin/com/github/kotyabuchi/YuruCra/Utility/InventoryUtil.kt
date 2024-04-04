package com.github.kotyabuchi.YuruCra.Utility

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import kotlin.math.min

fun PlayerInventory.addItemOrDrop(player: Player, vararg items: ItemStack) {
    items.forEach { item ->
        val nullSlots = mutableListOf<Int>()
        var storeAmount = item.amount

        for ((index, itemStack) in storageContents.withIndex()) {
            if (itemStack == null) {
                nullSlots.add(index)
            } else {
                if (item.isSimilar(itemStack)) {
                    val canStoreAmount = min(storeAmount, item.maxStackSize - itemStack.amount)
                    itemStack.amount += canStoreAmount
                    storeAmount -= canStoreAmount
                    if (storeAmount <= 0) break
                }
            }
        }
        if (storeAmount > 0) {
            for (nullSlot in nullSlots) {
                val canStoreAmount = min(storeAmount, item.maxStackSize)
                this.setItem(nullSlot, item.asQuantity(canStoreAmount))
                storeAmount -= canStoreAmount
                if (storeAmount <= 0) return
            }
            player.world.dropItem(player.location, item.asQuantity(storeAmount))
        }
        if (item.amount != storeAmount) player.world.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, .5f, 1.5f)
    }
}

fun Inventory.getRemaining(item: ItemStack): Int {
    var remainingAmount = item.amount

    for (itemStack in storageContents) {
        if (itemStack == null) {
            remainingAmount -= min(remainingAmount, item.maxStackSize)
        } else {
            if (item.isSimilar(itemStack)) {
                val canStoreAmount = min(remainingAmount, item.maxStackSize - itemStack.amount)
                remainingAmount -= canStoreAmount
            }
        }
        if (remainingAmount <= 0) break
    }
    return remainingAmount
}

fun Inventory.findFirst(searchItem: ItemStack): FindItemResult? {
    this.storageContents.forEachIndexed { index, itemStack ->
        if (itemStack != null && searchItem.isSimilar(itemStack)) return FindItemResult(index, itemStack)
    }
    return null
}

fun Inventory.findLast(searchItem: ItemStack): FindItemResult? {
    this.storageContents.reversed().forEachIndexed { index, itemStack ->
        if (itemStack != null && searchItem.isSimilar(itemStack)) return FindItemResult(index, itemStack)
    }
    return null
}

fun Inventory.findAll(searchItem: ItemStack): List<FindItemResult> {
    val result = mutableListOf<FindItemResult>()
    this.storageContents.forEachIndexed { index, itemStack ->
        if (itemStack != null && searchItem.isSimilar(itemStack)) result.add(FindItemResult(index, itemStack))
    }
    return result
}

fun Inventory.getFirstItem(): FindItemResult? {
    this.storageContents.forEachIndexed { index, itemStack ->
        if (itemStack != null && !itemStack.type.isAir) return FindItemResult(index, itemStack)
    }
    return null
}

fun Inventory.removeSimilar(removeItem: ItemStack) {
    val removeSlots = mutableListOf<Int>()
    this.storageContents.forEachIndexed { index, itemStack ->
        if (itemStack != null && itemStack.isSimilar(removeItem)) removeSlots.add(index)
    }
    removeSlots.forEach { slot ->
        this.setItem(slot, null)
    }
}

fun Inventory.consume(itemStack: ItemStack, amount: Int = itemStack.amount, reverse: Boolean = false): Boolean {
    var result = false
    val consumeItems = mutableMapOf<Int, Int>()
    if (reverse) contents.reverse()

    var foundAmount = 0
    for ((index, item) in contents.withIndex()) {
        if (item == null) continue
        if (itemStack.isSimilar(item)) {
            val consumeAmount = min(item.amount, amount - foundAmount)
            foundAmount += consumeAmount
            consumeItems[index] = consumeAmount
            if (foundAmount >= amount) {
                result = true
                break
            }
        }
    }
    if (result) {
        consumeItems.forEach { (index, consumedAmount) ->
            contents[index]?.let {
                it.amount -= consumedAmount
            }
        }
    }
    return result
}

fun Inventory.findItemAmount(itemStack: ItemStack): Int {
    var foundAmount = 0
    storageContents.let {
        for (item in it) {
            if (item != null && itemStack.isSimilar(item)) {
                foundAmount += item.amount
            }
        }
    }
    return foundAmount
}

fun Inventory.consumeFillBlock(block: Block): Material? {
    var fillBlock: Material? = null
    val groundFillBlockMaterials = listOf(Material.STONE, Material.COBBLESTONE)
    val underGroundFillBlockMaterials = listOf(Material.DEEPSLATE, Material.COBBLED_DEEPSLATE)
    if (block.y > 0) {
        for (material in groundFillBlockMaterials) {
            if (this.consume(ItemStack(material))) {
                fillBlock = material
                break
            }
        }
        if (fillBlock == null) {
            for (material in underGroundFillBlockMaterials) {
                if (this.consume(ItemStack(material))) {
                    fillBlock = material
                    break
                }
            }
        }
    } else {
        for (material in underGroundFillBlockMaterials) {
            if (this.consume(ItemStack(material))) {
                fillBlock = material
                break
            }
        }
        if (fillBlock == null) {
            for (material in groundFillBlockMaterials) {
                if (this.consume(ItemStack(material))) {
                    fillBlock = material
                    break
                }
            }
        }
    }
    return fillBlock
}

data class FindItemResult(val slot: Int, val itemStack: ItemStack)