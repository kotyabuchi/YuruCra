package com.github.kotyabuchi.realisticsurvival.util

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
}

fun ItemStack.withModel(model: ItemModel): ItemStack = apply {
    setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(model.id).build())
}
