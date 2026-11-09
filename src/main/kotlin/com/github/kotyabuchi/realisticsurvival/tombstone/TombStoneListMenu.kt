package com.github.kotyabuchi.realisticsurvival.tombstone

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.menu.ConfirmMenu
import com.github.kotyabuchi.realisticsurvival.menu.Menu
import com.github.kotyabuchi.realisticsurvival.menu.MenuButton
import com.github.kotyabuchi.realisticsurvival.util.ItemModel
import com.github.kotyabuchi.realisticsurvival.util.buttonItem
import com.github.kotyabuchi.realisticsurvival.util.withModel
import com.github.kotyabuchi.realisticsurvival.util.loreLine
import com.github.kotyabuchi.realisticsurvival.util.runNextTick
import com.github.kotyabuchi.realisticsurvival.util.sendError
import com.github.kotyabuchi.realisticsurvival.util.sendSuccess
import com.github.kotyabuchi.realisticsurvival.util.text
import com.github.kotyabuchi.realisticsurvival.util.thenOnMain
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import java.text.SimpleDateFormat
import java.util.Date

/** あるプレイヤーの墓石の一覧（管理コマンド用）。クリックで中身を表示する。新しい順 */
class TombStoneListMenu(plugin: RealisticSurvival, private val owner: OfflinePlayer) : Menu(
    plugin,
    text("${owner.name}'s TombStones"),
    plugin.tombStones.tombStonesOf(owner.uniqueId).size,
    Frame.TOP, Frame.SIDE,
) {
    override fun build() {
        var page = 0
        plugin.tombStones.tombStonesOf(owner.uniqueId).asReversed().forEach { stone ->
            if (firstEmptySlot(page) == null) page++
            put(TombStoneButton(owner, stone), page)
        }
    }

    private class TombStoneButton(private val owner: OfflinePlayer, private val stone: TombStone) : MenuButton(
        buttonItem(
            Material.OAK_SIGN,
            text("TombStone #${stone.id}"),
            listOf(
                loreLine("Date: ", DATE_FORMAT.format(Date(stone.createdAt))),
                loreLine("World: ", stone.worldName),
                loreLine("Location: ", "${stone.x.toInt()}, ${stone.y.toInt()}, ${stone.z.toInt()}"),
                Component.empty(),
                text("Left Click: Show items", NamedTextColor.GOLD),
                text("Right Click: Teleport / Remove", NamedTextColor.GREEN),
            ),
        ).withModel(ItemModel.TOMBSTONE)
    ) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) = show(player, menu)

        override fun onRightClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            menu.plugin.menus.open(player, TombStoneActionMenu(menu.plugin, owner, stone))
        }

        private fun show(player: Player, menu: Menu) {
            val plugin = menu.plugin
            plugin.tombStones.loadItems(stone).thenOnMain(plugin) { items ->
                if (items == null) {
                    // 他の人が回収・削除した直後など
                    player.sendError("墓石 #${stone.id} の中身が見つかりません")
                    plugin.menus.reopen(player, menu)
                    return@thenOnMain
                }
                plugin.menus.open(player, TombStoneViewMenu(plugin, "${owner.name}'s TombStone #${stone.id}", items))
            }
        }
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd HH:mm")
    }
}

/** 墓石の一覧で右クリックしたときのサブメニュー（テレポート・削除） */
class TombStoneActionMenu(plugin: RealisticSurvival, owner: OfflinePlayer, private val stone: TombStone) :
    Menu(plugin, text("${owner.name}'s TombStone #${stone.id}"), 7, Frame.TOP, Frame.SIDE) {

    override fun build() {
        put(TeleportButton(stone))
        put(RemoveButton(stone))
    }

    private class TeleportButton(private val stone: TombStone) :
        MenuButton(buttonItem(Material.ENDER_PEARL, text("Teleport"), listOf(loreLine("World: ", stone.worldName)))) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            val destination = stone.location()
            if (destination == null) {
                player.sendError("ワールド[${stone.worldName}]が読み込まれていないため、テレポートできません。")
                return
            }
            val plugin = menu.plugin
            plugin.menus.close(player) {
                player.teleportAsync(destination).thenAccept { success ->
                    if (!success) plugin.runNextTick { player.sendError("テレポートに失敗しました") }
                }
            }
        }
    }

    private class RemoveButton(private val stone: TombStone) : MenuButton(
        buttonItem(Material.BARRIER, text("Remove", NamedTextColor.RED), listOf(text("中身ごと削除します", NamedTextColor.GRAY)))
    ) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            val plugin = menu.plugin
            plugin.menus.open(player, ConfirmMenu(plugin, "Remove TombStone", listOf("墓石 #${stone.id} を中身ごと削除します"), confirmLabel = "削除する") {
                plugin.tombStones.delete(stone) {
                    player.sendSuccess("墓石 #${stone.id} を削除しました")
                    // サブメニューのひとつ前（一覧）に、削除した墓石が消えた状態で戻る
                    menu.prevMenu?.let { plugin.menus.reopen(player, it) }
                }
            })
        }
    }
}
