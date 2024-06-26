package com.github.kotyabuchi.YuruCra

import com.github.kotyabuchi.MCRPG.DBConnector
import com.github.kotyabuchi.MCRPG.transactionWithLogger
import com.github.kotyabuchi.YuruCra.Event.CustomEventCaller
import com.github.kotyabuchi.YuruCra.Item.ItemExtensionCommand
import com.github.kotyabuchi.YuruCra.Item.ItemExtensionManager
import com.github.kotyabuchi.YuruCra.Mastering.MasteringType
import com.github.kotyabuchi.YuruCra.Menu.HomeMenu.MBCreateHome
import com.github.kotyabuchi.YuruCra.Menu.MainMenu.MainMenuCompass
import com.github.kotyabuchi.YuruCra.Menu.MenuController
import com.github.kotyabuchi.YuruCra.Player.Command.HomeCommand
import com.github.kotyabuchi.YuruCra.Player.Command.MainMenuCommand
import com.github.kotyabuchi.YuruCra.Player.Command.PlayerManageCommand
import com.github.kotyabuchi.YuruCra.Player.PlayerManager
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import com.github.kotyabuchi.YuruCra.System.*
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Main: JavaPlugin() {

    private val dbFile = File(dataFolder, "YuruCra.sqlite")

    companion object {
        lateinit var instance: Main
    }

    private fun registerEvents() {
        val events: List<Listener> = listOf(
            // Event
            CustomEventCaller,
            // Item
            ItemExtensionManager,
            // Menu
            MenuController,
            MainMenuCompass,
            // Home
            MBCreateHome,
            // Player
            PlayerManager,
            // System
            ChatSound,
            Debug,
            KeepCropAndReplant,
            KeepFarmland,
            LevelTheFarmlandAndPath,
            StarterItem,
            UpgradeSpawner,
        )

        MasteringType.values().forEach {
            registerEvent(it.masteringClass)
        }

        events.forEach {
            registerEvent(it)
        }
    }

    fun registerEvent(event: Listener) {
        server.pluginManager.registerEvents(event, this)
        logger.info("イベントリスナーを登録 - ${event.javaClass.simpleName}")
    }

    private fun registerCommands() {
        getCommand("menu")?.setExecutor(MainMenuCommand)
        getCommand("home")?.setExecutor(HomeCommand)
        getCommand("itemedit")?.setExecutor(ItemExtensionCommand)
        getCommand("playermanager")?.setExecutor(PlayerManageCommand)
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
                it.getStatus().masteringManager.clearExpBar()
                PlayerManager.savePlayerData(it.getStatus())
            }
        }
    }
}