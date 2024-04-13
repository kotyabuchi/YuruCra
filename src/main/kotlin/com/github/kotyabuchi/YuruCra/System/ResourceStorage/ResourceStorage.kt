package com.github.kotyabuchi.YuruCra.System.ResourceStorage

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.math.min

class ResourceStorage {
    val maxSlotSize = 9 * 4
    var slotSize = 2
    var autoStore = true
    private val storedResources = mutableMapOf<Material, Int>()

    fun getStoredResources(): Map<Material, Int> {
        return storedResources
    }

    fun getStoredResourceAmount(material: Material): Int {
        return storedResources[material] ?: 0
    }

    fun existsMaterial(itemStack: ItemStack): Boolean {
        return storedResources.any{ ItemStack(it.key).isSimilar(itemStack) }
    }

    fun storeResource(_itemStack: ItemStack): Int {
        val itemStack = _itemStack.asOne()
        var storeAmount = 0
        val material = itemStack.type
        if (!ItemStack(material).isSimilar(itemStack)) return 0
        if (existsMaterial(itemStack)) {
            storedResources[material]?.let {
                storeAmount = min(Int.MAX_VALUE - it, _itemStack.amount)
                storedResources[material] = it + storeAmount
            }
        } else {
            if (storedResources.size < slotSize) {
                storeAmount = _itemStack.amount
                storedResources[material] = storeAmount
            }
        }
        return storeAmount
    }

    fun restoreResource(material: Material, amount: Int): ItemStack? {
        storedResources[material]?.let { resource ->
            val restoreAmount = min(amount, resource)
            val restoredAmount = resource - restoreAmount
            storedResources[material] = restoredAmount
            if (restoredAmount == 0) storedResources.remove(material)
            return ItemStack(material).asQuantity(restoreAmount)
        }
        return null
    }
}