package com.github.kotyabuchi.realisticsurvival.menu

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.buttonItem
import com.github.kotyabuchi.realisticsurvival.util.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * 元に戻せない操作の前に出す確認画面。
 * 「実行」で onConfirm を呼び、「キャンセル」で元の画面に戻る。
 *
 * 使い方:
 * ```
 * plugin.menus.open(player, ConfirmMenu(plugin, "ホームを削除", listOf("[家]を削除します")) { player ->
 *     // 実行したときの処理
 * })
 * ```
 * onConfirm の中で次の画面を開かない場合、確認画面は閉じる。
 */
class ConfirmMenu(
    plugin: RealisticSurvival,
    title: String,
    private val description: List<String> = emptyList(),
    private val confirmLabel: String = "実行",
    private val onConfirm: (Player) -> Unit,
) : Menu(plugin, text(title), 7, Frame.TOP, Frame.SIDE) {

    override fun build() {
        val lore = description.map { text(it, NamedTextColor.GRAY) } + Component.empty() + text("元に戻せません", NamedTextColor.RED)
        // 押し間違えにくいよう、ボタンの間を空ける
        put(BlankButton(), 0, 10)
        put(ConfirmButton(confirmLabel, lore, onConfirm), 0, 11)
        put(BlankButton(), 0, 12)
        put(BlankButton(), 0, 13)
        put(BlankButton(), 0, 14)
        put(CancelButton(), 0, 15)
        put(BlankButton(), 0, 16)
    }

    private class ConfirmButton(label: String, lore: List<Component>, private val onConfirm: (Player) -> Unit) :
        MenuButton(buttonItem(Material.LIME_CONCRETE, text(label, NamedTextColor.GREEN), lore)) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            val plugin = menu.plugin
            // 先に閉じておき、onConfirm が次の画面を開けばそちらが表示される
            plugin.menus.close(player)
            onConfirm(player)
        }
    }

    private class CancelButton : MenuButton(buttonItem(Material.RED_CONCRETE, text("キャンセル", NamedTextColor.RED))) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            val plugin = menu.plugin
            val prev = menu.prevMenu
            if (prev != null) plugin.menus.reopen(player, prev) else plugin.menus.close(player)
        }
    }
}
