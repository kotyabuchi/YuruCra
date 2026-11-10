package com.github.kotyabuchi.yurucra.util

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData
import org.bukkit.inventory.ItemStack

/**
 * リソースパック（resourcepack/）の見た目を切り替える目印。
 *
 * アイテムの custom_model_data に文字列を1つ付けると、パックの assets/minecraft/items 以下の定義が
 * 対応するモデルに切り替える。パックを入れていない人には元のアイテムの見た目のまま表示される。
 * ここの文字列とパック側の "when" は必ず一致させること。
 */
enum class ItemModel(val id: String) {
    /** 矢印（ARROW）に付ける */
    ARROW_LEFT("pumpking:arrow_left"),
    ARROW_RIGHT("pumpking:arrow_right"),
    RETURN("pumpking:return"),

    /** エンダーアイ（ENDER_EYE）に付ける */
    PLUS("pumpking:plus"),

    /** オークの看板（OAK_SIGN）に付ける */
    TOMBSTONE("pumpking:tombstone"),

    /** 深層岩の丸石だけで作った石の道具（STONE_PICKAXE など）に付ける */
    DEEPSLATE_PICKAXE("pumpking:deepslate_pickaxe"),
    DEEPSLATE_AXE("pumpking:deepslate_axe"),
    DEEPSLATE_SHOVEL("pumpking:deepslate_shovel"),
    DEEPSLATE_HOE("pumpking:deepslate_hoe"),
    DEEPSLATE_SWORD("pumpking:deepslate_sword"),

    /** 材料別の狼の鎧（WOLF_ARMOR）に付ける */
    COPPER_WOLF_ARMOR("pumpking:copper_wolf_armor"),
    IRON_WOLF_ARMOR("pumpking:iron_wolf_armor"),
    GOLDEN_WOLF_ARMOR("pumpking:golden_wolf_armor"),
    DIAMOND_WOLF_ARMOR("pumpking:diamond_wolf_armor"),
    NETHERITE_WOLF_ARMOR("pumpking:netherite_wolf_armor"),
}

fun ItemStack.withModel(model: ItemModel): ItemStack = apply {
    setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(model.id).build())
}
