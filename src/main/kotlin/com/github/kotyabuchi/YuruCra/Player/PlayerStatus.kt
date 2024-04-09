package com.github.kotyabuchi.YuruCra.Player

import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.Menu.Menu
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.time.LocalDateTime

class PlayerStatus(private val player: Player): Player by player {

    companion object {
        private val players = mutableMapOf<Player, PlayerStatus>()
        fun Player.getStatus(): PlayerStatus = players.getOrPut(this) { PlayerStatus(this) }
        fun removeCache(player: Player) {
            players.remove(player)
        }
    }

    private val main: Main = Main.instance
    var playTime: Long = 0
    var lastLogin: LocalDateTime = LocalDateTime.now()
    val loginTime: LocalDateTime = LocalDateTime.now()
    var lastPlayVersion: Double = 0.0
    private val debugModeKey = NamespacedKey(main, "DebugMode")
    private val pdc = player.persistentDataContainer
    var isDebugMode: Boolean
        get() {
            return pdc.has(debugModeKey)
        }
        set(value) {
            if (value) {
                pdc.set(debugModeKey, PersistentDataType.BOOLEAN, true)
            } else {
                pdc.remove(debugModeKey)
            }
        }

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
    val masteringManager: MasteringManager = MasteringManager(player)
}