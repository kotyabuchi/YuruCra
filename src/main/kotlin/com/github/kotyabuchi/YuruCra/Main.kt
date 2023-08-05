package com.github.kotyabuchi.YuruCra

import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class Main: JavaPlugin() {

    companion object {
        lateinit var instance: Main
    }

    private fun registerEvents() {
        val pm = server.pluginManager
        val events: List<Listener> = listOf()

        events.forEach { it ->
            pm.registerEvents(it, this)
            logger.info("イベントリスナーを登録 - ${it.javaClass.name}")
        }
    }

    override fun onEnable() {
        logger.info("初期化中...")

        instance = this

        registerEvents()

        logger.info("プラグインを有効化しました。")
    }
}