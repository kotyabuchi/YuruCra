package com.github.kotyabuchi.YuruCra.Utility

import com.github.kotyabuchi.YuruCra.Event.CustomEventCaller
import com.github.kotyabuchi.YuruCra.Item.ArmorType
import com.github.kotyabuchi.YuruCra.Item.EquipmentType
import com.github.kotyabuchi.YuruCra.Item.ItemExtension
import com.github.kotyabuchi.YuruCra.Item.ToolType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack

fun Material.hasDurability(): Boolean {
    return this.isTools() || this.isWeapons() || this.isArmors() || this.isShield()
}

fun Material.isSword(): Boolean {
    return ToolType.SWORD.includes(this)
}

fun Material.isPickAxe(): Boolean {
    return ToolType.PICKAXE.includes(this)
}

fun Material.isAxe(): Boolean {
    return ToolType.AXE.includes(this)
}

fun Material.isShovel(): Boolean {
    return ToolType.SHOVEL.includes(this)
}

fun Material.isHoe(): Boolean {
    return ToolType.HOE.includes(this)
}

fun Material.isShield(): Boolean {
    return ToolType.SHIELD.includes(this)
}

fun Material.isTools(): Boolean {
    return this == Material.SHIELD ||
            this.isPickAxe() ||
            this.isShovel() ||
            this.isHoe() ||
            this.isAxe() ||
            this == Material.FISHING_ROD ||
            this == Material.SHEARS ||
            this == Material.FLINT_AND_STEEL
}

fun Material.isWeapons(): Boolean {
    return this == Material.BOW ||
            this == Material.CROSSBOW ||
            this.isSword() ||
            this.isAxe()
}

fun Material.isHelmet(): Boolean {
    return ArmorType.HELMET.includes(this)
}

fun Material.isChestplate(): Boolean {
    return ArmorType.CHESTPLATE.includes(this)
}

fun Material.isLeggings(): Boolean {
    return ArmorType.LEGGINGS.includes(this)
}

fun Material.isBoots(): Boolean {
    return ArmorType.BOOTS.includes(this)
}

fun Material.isArmors(): Boolean {
    return this.isHelmet() ||
            this.isChestplate() ||
            this.isLeggings() ||
            this.isBoots() ||
            this == Material.ELYTRA
}

fun Material.getEquipmentType(): EquipmentType? {
    ToolType.values().forEach {
        if (it.includes(this)) return it
    }
    ArmorType.values().forEach {
        if (it.includes(this)) return it
    }
    return null
}

fun ItemStack.damage(player: Player, amount: Int) {
    if (this.type.hasDurability() && amount > 0) CustomEventCaller.callEvent(PlayerItemDamageEvent(player, this, amount, amount))
}

fun ItemStack.damage(amount: Int): Boolean {
    val itemExtension = ItemExtension(this)
    itemExtension.damage(amount)
    itemExtension.applyDurability().applySetting()
    if (itemExtension.durability > 0) return false
    this.subtract(Int.MAX_VALUE)
    return true
}

fun ItemStack.damageWithBreakSound(amount: Int, location: Location): Boolean {
    val broken = this.damage(amount)
    if (broken) location.world.playSound(location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
    return broken
}