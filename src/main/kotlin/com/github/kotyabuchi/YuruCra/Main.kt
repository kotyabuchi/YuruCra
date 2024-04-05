package com.github.kotyabuchi.YuruCra

import com.github.kotyabuchi.MCRPG.DBConnector
import com.github.kotyabuchi.MCRPG.transactionWithLogger
import com.github.kotyabuchi.YuruCra.Event.CustomEventCaller
import com.github.kotyabuchi.YuruCra.Item.ItemExtensionCommand
import com.github.kotyabuchi.YuruCra.Item.ItemExtensionManager
import com.github.kotyabuchi.YuruCra.Menu.HomeMenu.MBCreateHome
import com.github.kotyabuchi.YuruCra.Menu.MenuController
import com.github.kotyabuchi.YuruCra.Player.Command.HomeCommand
import com.github.kotyabuchi.YuruCra.Player.Command.PlayerManageCommand
import com.github.kotyabuchi.YuruCra.Player.PlayerManager
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import com.github.kotyabuchi.YuruCra.System.ChatSound
import com.github.kotyabuchi.YuruCra.System.Debug
import com.github.kotyabuchi.YuruCra.System.FallenTree
import com.github.kotyabuchi.YuruCra.Utility.MaterialUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File

class Main: JavaPlugin() {

    private val dbFile = File(dataFolder, "YuruCra.sqlite")

    companion object {
        lateinit var instance: Main
    }

    private fun registerEvents() {
        val pm = server.pluginManager
        val events: List<Listener> = listOf(
            // Event
            CustomEventCaller,
            // Item
            ItemExtensionManager,
            // Menu
            MenuController,
            // Home
            MBCreateHome,
            // Player
            PlayerManager,
            // System
            ChatSound,
            Debug,
            FallenTree,
        )

        events.forEach {
            registerEvent(it)
        }
    }

    fun registerEvent(event: Listener) {
        server.pluginManager.registerEvents(event, this)
        logger.info("イベントリスナーを登録 - ${event.javaClass.simpleName}")
    }

    private fun registerCommands() {
        getCommand("home")?.setExecutor(HomeCommand)
        getCommand("itemedit")?.setExecutor(ItemExtensionCommand)
        getCommand("debugmode")?.setExecutor(PlayerManageCommand)
    }

    override fun onEnable() {
        logger.info("初期化中...")

        instance = this

        if (!dataFolder.exists()) dataFolder.mkdirs()

        DBConnector.registerDBFile(dbFile.path).connect()

        registerEvents()
        registerCommands()

        checkCanNotItemMaterials(MaterialUtil.canNotItems)

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

    private fun checkCanNotItemMaterials(resultList: MutableList<Material>) {
        println("Start check can not item materials")
        val inv = Bukkit.createInventory(null, 6 * 9)
        val materials = Material.values().toList().chunked(6 * 9)
        object : BukkitRunnable() {
            var page = 0
            override fun run() {
                println("$page/${materials.size}")
                if (page > 0) {
                    inv.storageContents.forEachIndexed { index, itemStack ->
                        if (itemStack == null) {
                            if (materials[page - 1].size > index) resultList.add(materials[page - 1][index])
                        }
                    }
                }
                if (page >= materials.size) {
                    cancel()
                    println("""
                        ===== Finished =====
                        Result:
                        ${resultList.joinToString { it.name }}
                    """.trimIndent())

                } else {
                    materials[page].forEachIndexed { index, material ->
                        inv.setItem(index, ItemStack(material))
                    }
                }
                page++
            }
        }.runTaskTimer(this, 0, 1)
    }
}