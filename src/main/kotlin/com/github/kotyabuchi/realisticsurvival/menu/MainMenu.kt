package com.github.kotyabuchi.realisticsurvival.menu

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.menu.home.HomeMenu
import com.github.kotyabuchi.realisticsurvival.menu.home.PublicHomeMenu
import com.github.kotyabuchi.realisticsurvival.menu.job.JobMenu
import com.github.kotyabuchi.realisticsurvival.menu.storage.ResourceStorageMenu
import com.github.kotyabuchi.realisticsurvival.util.buttonItem
import com.github.kotyabuchi.realisticsurvival.util.loreLine
import com.github.kotyabuchi.realisticsurvival.util.text
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

class MainMenu(plugin: RealisticSurvival) :
    Menu(plugin, text("Menu"), 3, Frame.TOP, Frame.SIDE) {

    override fun build() {
        put(JobMenuButton())
        put(HomeMenuButton())
        put(ResourceStorageButton())
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

    private class ResourceStorageButton : MenuButton(buttonItem(Material.CHEST, text("Resource Storage"))) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
            menu.plugin.menus.open(player, ResourceStorageMenu(menu.plugin, player))
        }
    }
}
