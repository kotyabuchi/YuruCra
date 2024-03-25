package com.github.kotyabuchi.YuruCra

import com.github.kotyabuchi.MCRPG.DBConnector
import com.github.kotyabuchi.MCRPG.transactionWithLogger
import com.github.kotyabuchi.YuruCra.Menu.HomeMenu.MBCreateHome
import com.github.kotyabuchi.YuruCra.Menu.MenuController
import com.github.kotyabuchi.YuruCra.Player.HomeCommand
import com.github.kotyabuchi.YuruCra.Player.PlayerManager
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import com.github.kotyabuchi.YuruCra.System.Debug
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Main: JavaPlugin() {

    private val dbFile = File(dataFolder, "YuruCra.sqlite")

    companion object {
        lateinit var instance: Main
    }

    private fun registerEvents() {
        val pm = server.pluginManager
        val events: List<Listener> = listOf(
            // Menu
            MenuController,
                // Home
            MBCreateHome,
            // Player
            PlayerManager,
            // System
            Debug,
        )

        events.forEach {
            pm.registerEvents(it, this)
            logger.info("イベントリスナーを登録 - ${it.javaClass.simpleName}")
        }
    }

    private fun registerCommands() {
        getCommand("home")?.setExecutor(HomeCommand)
    }

    override fun onEnable() {
        logger.info("初期化中...")

        instance = this

        if (!dataFolder.exists()) dataFolder.mkdirs()

        DBConnector.registerDBFile(dbFile.path).connect()

        registerEvents()
        registerCommands()

        logger.info("プラグインを有効化しました。")
    }

    override fun onDisable() {
        logger.info("終了中...")

        transactionWithLogger {
            server.onlinePlayers.forEach {
                PlayerManager.savePlayerData(it.getStatus())
            }
        }
    }
}