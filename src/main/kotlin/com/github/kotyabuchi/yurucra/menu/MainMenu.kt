package com.github.kotyabuchi.yurucra.menu

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.menu.home.HomeMenu
import com.github.kotyabuchi.yurucra.menu.home.PublicHomeMenu
import com.github.kotyabuchi.yurucra.menu.job.JobMenu
import com.github.kotyabuchi.yurucra.menu.storage.ResourceStorageMenu
import com.github.kotyabuchi.yurucra.util.buttonItem
import com.github.kotyabuchi.yurucra.util.loreLine
import com.github.kotyabuchi.yurucra.util.text
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/** @param viewer 開く人。OPならサウンドサンプルのボタンも出す */
class MainMenu(plugin: YuruCra, private val viewer: Player) :
    Menu(plugin, text("Menu"), 3, Frame.TOP, Frame.SIDE) {

    override fun build() {
        put(JobMenuButton())
        put(HomeMenuButton())
        put(ResourceStorageButton())
        if (viewer.isOp) put(SoundSampleButton())
    }

    private class JobMenuButton : MenuButton(buttonItem(Material.DIAMOND_PICKAXE, text("JobInfo"))) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            menu.plugin.menus.open(player, JobMenu(menu.plugin, player))
        }
    }

    private class HomeMenuButton : MenuButton(
        buttonItem(
            Material.ENDER_PEARL,
            text("Homes"),
            listOf(loreLine("Left Click: ", "Show private homes"), loreLine("Right Click: ", "Show public homes")),
        )
    ) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            menu.plugin.menus.open(player, HomeMenu(menu.plugin, player))
        }

        override fun onRightClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            menu.plugin.menus.open(player, PublicHomeMenu(menu.plugin, player))
        }
    }

    private class SoundSampleButton : MenuButton(buttonItem(Material.NOTE_BLOCK, text("Sound Sample"))) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            menu.plugin.menus.open(player, SoundSampleMenu(menu.plugin))
        }
    }

    private class ResourceStorageButton : MenuButton(buttonItem(Material.CHEST, text("Resource Storage"))) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            menu.plugin.menus.open(player, ResourceStorageMenu(menu.plugin, player))
        }
    }
}
