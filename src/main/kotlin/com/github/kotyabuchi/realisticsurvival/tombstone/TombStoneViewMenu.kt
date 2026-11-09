package com.github.kotyabuchi.realisticsurvival.tombstone

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.menu.Menu
import com.github.kotyabuchi.realisticsurvival.menu.MenuButton
import com.github.kotyabuchi.realisticsurvival.util.text
import org.bukkit.inventory.ItemStack

/**
 * 墓石の中身を見るだけの画面（管理コマンド用）。メニューなので、アイテムは取り出せない。
 * 1段目: 頭・胴・脚・足・オフハンド、2〜5段目: 持ち物（ホットバーは5段目）
 */
class TombStoneViewMenu(plugin: RealisticSurvival, title: String, private val items: Array<ItemStack>) :
    Menu(plugin, text(title), 9 * 5) {

    override fun build() {
        listOf(TombStoneSlots.HEAD, TombStoneSlots.CHEST, TombStoneSlots.LEGS, TombStoneSlots.FEET, TombStoneSlots.OFF_HAND)
            .forEachIndexed { slot, index -> show(index, slot) }
        // 持ち物 9〜35 を2〜4段目、ホットバー 0〜8 を5段目に（プレイヤーのインベントリ画面と同じ並び）
        for (index in 9 until TombStoneSlots.FEET) show(index, index)
        for (index in 0 until 9) show(index, 36 + index)
    }

    private fun show(index: Int, slot: Int) {
        val item = items.getOrNull(index)?.takeUnless { it.isEmpty } ?: return
        put(MenuButton(item, clickSound = null), 0, slot)
    }
}
