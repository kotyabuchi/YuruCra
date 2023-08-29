package com.github.kotyabuchi.YuruCra

import com.github.kotyabuchi.MCRPG.DBConnector
import com.github.kotyabuchi.YuruCra.Menu.MenuController
import com.github.kotyabuchi.YuruCra.Player.PlayerManager
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
            PlayerManager,
        )

        events.forEach {
            pm.registerEvents(it, this)
            logger.info("イベントリスナーを登録 - ${it.javaClass.name}")
        }
    }

    override fun onEnable() {
        logger.info("初期化中...")

        instance = this

        if (!dataFolder.exists()) dataFolder.mkdirs()

        DBConnector.registerDBFile(dbFile.path).connect()

        registerEvents()

        logger.info("プラグインを有効化しました。")
    }
}