package com.github.kotyabuchi.realisticsurvival.menu.home

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.menu.BlankButton
import com.github.kotyabuchi.realisticsurvival.menu.ConfirmMenu
import com.github.kotyabuchi.realisticsurvival.menu.Menu
import com.github.kotyabuchi.realisticsurvival.menu.MenuButton
import com.github.kotyabuchi.realisticsurvival.menu.TextPrompt
import com.github.kotyabuchi.realisticsurvival.player.Home
import com.github.kotyabuchi.realisticsurvival.util.ItemModel
import com.github.kotyabuchi.realisticsurvival.util.buttonItem
import com.github.kotyabuchi.realisticsurvival.util.withModel
import com.github.kotyabuchi.realisticsurvival.util.consumeOne
import com.github.kotyabuchi.realisticsurvival.util.loreLine
import com.github.kotyabuchi.realisticsurvival.util.runNextTick
import com.github.kotyabuchi.realisticsurvival.util.sendError
import com.github.kotyabuchi.realisticsurvival.util.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/** 自分のホーム一覧。先頭にワールドスポーン・リスポーン地点・直近の墓石が並ぶ */
class HomeMenu(
    plugin: RealisticSurvival,
    private val player: Player,
    private val respawn: Location? = player.getRespawnLocation(false),
    private val tombStone: Location? = plugin.tombStones.latest(player.uniqueId)?.location(),
) : Menu(
    plugin,
    text("${player.name}'s Homes"),
    1 + (if (respawn != null) 1 else 0) + (if (tombStone != null) 1 else 0) + plugin.players.status(player).homes.size,
    Frame.TOP, Frame.SIDE,
) {
    override fun build() {
        put(HomeButton(player, Home.temporary("World Spawn", player.world.spawnLocation.apply { yaw = 0f }, Material.GRASS_BLOCK)))
        respawn?.let { put(HomeButton(player, Home.temporary("Respawn Point", it.clone().apply { yaw = 0f }, Material.RED_BED))) }
        tombStone?.let { put(HomeButton(player, Home.temporary("Recent TombStone", it, Material.OAK_SIGN), ItemModel.TOMBSTONE)) }

        var page = 0
        plugin.players.status(player).homes.forEach { home ->
            if (firstEmptySlot(page) == null) page++
            put(HomeButton(player, home), page)
        }
        repeat(page + 1) { put(CreateHomeButton(), it, size - 5) }
    }
}

/** 全員の公開ホーム一覧 */
class PublicHomeMenu(plugin: RealisticSurvival, private val player: Player) :
    Menu(plugin, text("Public Homes"), plugin.homes.publicHomes().size, Frame.TOP, Frame.SIDE) {

    override fun build() {
        var page = 0
        plugin.homes.publicHomes().forEach { home ->
            if (firstEmptySlot(page) == null) page++
            put(HomeButton(player, home), page)
        }
    }
}

/** ホームの設定（アイコン・名前・公開設定・削除） */
class HomeSettingMenu(plugin: RealisticSurvival, private val home: Home) :
    Menu(plugin, text("Home Setting"), 7, Frame.TOP, Frame.SIDE) {

    override fun build() {
        put(ChangeIconButton(home))
        put(RenameButton(home))
        put(ChangePublicButton(home))
        put(BlankButton())
        put(BlankButton())
        put(RemoveButton(home))
        put(BlankButton())
    }
}

/** アイコンに使うアイテムを選ぶ */
class ChangeHomeIconMenu(plugin: RealisticSurvival, private val home: Home) :
    Menu(plugin, text("Change Icon"), Int.MAX_VALUE) {

    override fun build() {
        val perPage = size - 9
        ICONS.chunked(perPage).forEachIndexed { page, icons ->
            icons.forEachIndexed { index, (material, icon) -> put(IconButton(home, material, icon), page, index) }
        }
    }

    private class IconButton(private val home: Home, private val material: Material, icon: ItemStack) :
        MenuButton(icon) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            menu.plugin.homes.changeIcon(player, home, material) {
                menu.prevMenu?.let { prev -> menu.plugin.menus.reopen(player, prev) }
            }
        }
    }

    companion object {
        // 約1,400種類あるので、ボタン用のアイテムは最初に一度だけ作って使い回す
        private val ICONS: List<Pair<Material, ItemStack>> by lazy {
            Material.entries
                .filter { it.isItem && !it.isAir && !it.name.startsWith("LEGACY_") }
                .map { it to buttonItem(it, name = null) }
        }
    }
}

private class HomeButton(viewer: Player, private val home: Home, model: ItemModel? = null) :
    MenuButton(createIcon(viewer, home).also { icon -> model?.let { icon.withModel(it) } }) {

    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        val destination = home.location()
        if (destination == null) {
            player.sendError("ワールド[${home.worldName}]が読み込まれていないため、テレポートできません。")
            return
        }
        val plugin = menu.plugin
        plugin.menus.close(player) {
            player.inventory.consumeOne(Material.ENDER_PEARL)
            playTeleportEffect(player.location)
            player.teleportAsync(destination).thenAccept { success ->
                plugin.runNextTick {
                    if (success) playTeleportEffect(destination) else player.sendError("テレポートに失敗しました")
                }
            }
        }
    }

    override fun onRightClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        if (home.id == null || !menu.plugin.homes.canEdit(player, home)) return
        menu.plugin.menus.open(player, HomeSettingMenu(menu.plugin, home))
    }

    companion object {
        fun createIcon(viewer: Player, home: Home) = run {
            val world = home.world
            val worldColor = if (world == null || world != viewer.world) NamedTextColor.RED else NamedTextColor.WHITE
            val lore = mutableListOf(
                loreLine("World: ", home.worldName, worldColor),
                text("X: ${home.x}"),
                text("Y: ${home.y}"),
                text("Z: ${home.z}"),
                text("Yaw: ${home.yaw}"),
            )
            if (home.isPublic) {
                if (home.creator == viewer.uniqueId) {
                    lore += text("Public", NamedTextColor.GREEN)
                } else {
                    home.creator?.let { Bukkit.getOfflinePlayer(it).name }?.let { lore += text("CreateBy: $it") }
                }
            }
            lore += Component.empty()
            lore += text("Left Click: Teleport to location", NamedTextColor.GOLD)
            if (home.id != null && (viewer.isOp || viewer.uniqueId == home.creator)) {
                lore += text("Right Click: Edit setting", NamedTextColor.GREEN)
            }
            buttonItem(home.icon, text(home.name), lore)
        }

        fun playTeleportEffect(location: Location) {
            val center = location.clone().add(0.0, 1.0, 0.0)
            location.world.spawnParticle(Particle.PORTAL, center, 200, 0.4, 0.8, 0.4, 0.5)
            location.world.spawnParticle(Particle.REVERSE_PORTAL, center, 60, 0.4, 0.8, 0.4, 0.05)
            location.world.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.6f)
        }
    }
}

private class CreateHomeButton :
    MenuButton(buttonItem(Material.ENDER_EYE, text("Create Home", NamedTextColor.GREEN)).withModel(ItemModel.PLUS)) {
    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        val plugin = menu.plugin
        plugin.menus.close(player) {
            TextPrompt.open(
                plugin, player,
                title = "Create Home", label = "ホームの名前",
                onSubmit = { name -> plugin.homes.create(player, name, player.location) },
                onCancel = { player.sendError("ホームポイントの作成をキャンセルしました。") },
            )
        }
    }
}

private class ChangeIconButton(private val home: Home) :
    MenuButton(buttonItem(Material.WRITABLE_BOOK, text("Change Icon"))) {
    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        menu.plugin.menus.open(player, ChangeHomeIconMenu(menu.plugin, home))
    }
}

private class RenameButton(private val home: Home) : MenuButton(buttonItem(Material.NAME_TAG, text("Rename"))) {
    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        val plugin = menu.plugin
        plugin.menus.close(player) {
            TextPrompt.open(
                plugin, player,
                title = "Rename Home", label = "新しい名前", initial = home.name,
                onSubmit = { name -> plugin.homes.rename(player, home, name) },
                onCancel = { player.sendError("ホームポイントのリネームをキャンセルしました。") },
            )
        }
    }
}

private class ChangePublicButton(private val home: Home) :
    MenuButton(buttonItem(Material.FILLED_MAP, text("Change to " + if (home.isPublic) "private" else "public"))) {
    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        menu.plugin.homes.setPublic(player, home, !home.isPublic) { menu.plugin.menus.reopen(player, menu) }
    }
}

private class RemoveButton(private val home: Home) : MenuButton(buttonItem(Material.BARRIER, text("Remove"))) {
    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        val plugin = menu.plugin
        plugin.menus.open(player, ConfirmMenu(plugin, "Remove Home", listOf("[${home.name}]を削除します"), confirmLabel = "削除する") {
            plugin.homes.remove(player, home) {
                // 設定画面のひとつ前（ホーム一覧）に戻る
                menu.prevMenu?.let { plugin.menus.reopen(player, it) }
            }
        })
    }
}
