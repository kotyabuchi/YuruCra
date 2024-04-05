package com.github.kotyabuchi.YuruCra.Player

import com.github.kotyabuchi.YuruCra.Menu.Menu
import org.bukkit.entity.Player
import java.time.LocalDateTime

class PlayerStatus(player: Player): Player by player {

    companion object {
        private val players = mutableMapOf<Player, PlayerStatus>()
        fun Player.getStatus(): PlayerStatus = players.getOrPut(this) { PlayerStatus(this) }
        fun removeCache(player: Player) {
            players.remove(player)
        }
    }

    var playTime: Long = 0
    var lastLogin: LocalDateTime = LocalDateTime.now()
    val loginTime: LocalDateTime = LocalDateTime.now()
    var lastPlayVersion: Double = 0.0
    var isDebugMode: Boolean = false

    // MenuSystem
    val menuStatus: MenuStatus = MenuStatus()

    fun openMenu(menu: Menu, page: Int = 0) {
        if (menuStatus.openingMenu?.prevMenu != menu) {
            if (menuStatus.openingMenu != menu) {
                menu.prevMenu = menuStatus.openingMenu
            }
        }
        menuStatus.openingMenu = menu
        menuStatus.openingPage = page
        openInventory(menu.getInventory(page))
    }

    fun closeMenu() {
        menuStatus.openingMenu = null
        menuStatus.openingPage = 0
        closeInventory()
    }

    // HomeSystem
    val homes: MutableList<HomeInfo> = mutableListOf()

    // MasteringSystem
    val masteringManager: MasteringManager = MasteringManager(this)
}