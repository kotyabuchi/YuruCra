package com.github.kotyabuchi.realisticsurvival.util

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

/** バニラのタグで判定するので、銅ツールなど新しい道具にも自動で対応する */
enum class ToolType(private val tag: Tag<Material>) {
    PICKAXE(Tag.ITEMS_PICKAXES),
    AXE(Tag.ITEMS_AXES),
    SHOVEL(Tag.ITEMS_SHOVELS),
    HOE(Tag.ITEMS_HOES),
    SWORD(Tag.ITEMS_SWORDS),
    ;

    fun includes(material: Material): Boolean = tag.isTagged(material)
    fun includes(item: ItemStack): Boolean = includes(item.type)
    val materials: Set<Material> get() = tag.values
}

/** メニューのボタン用アイテム */
fun buttonItem(
    material: Material,
    name: Component? = Component.empty(),
    lore: List<Component> = emptyList(),
): ItemStack = ItemStack(material).apply {
    editMeta { meta ->
        if (name != null) meta.displayName(name)
        meta.lore(lore)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
    }
}

/** インベントリに入れて、入りきらない分は足元に落とす */
fun Player.giveOrDrop(vararg items: ItemStack) {
    inventory.addItem(*items).values.forEach { world.dropItem(location, it) }
}

/** 名前やエンチャントのない素のアイテムを1つ消費する */
fun Inventory.consumeOne(material: Material): Boolean = removeItem(ItemStack(material)).isEmpty()

/** トンネル補助・鉱石置き換えで穴埋めに使うブロックの候補（優先順） */
fun fillBlockCandidates(block: Block): List<Material> = buildList {
    when (block.world.environment) {
        World.Environment.NETHER -> add(Material.NETHERRACK)
        World.Environment.THE_END -> add(Material.END_STONE)
        else -> {}
    }
    val ground = listOf(Material.STONE, Material.COBBLESTONE)
    val underground = listOf(Material.DEEPSLATE, Material.COBBLED_DEEPSLATE)
    if (block.y > 0) {
        addAll(ground); addAll(underground)
    } else {
        addAll(underground); addAll(ground)
    }
}
