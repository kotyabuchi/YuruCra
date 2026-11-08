package com.github.kotyabuchi.realisticsurvival

import com.github.kotyabuchi.realisticsurvival.block.BreakContext
import com.github.kotyabuchi.realisticsurvival.block.PlacedBlockTracker
import com.github.kotyabuchi.realisticsurvival.block.SafeCropAndReplant
import com.github.kotyabuchi.realisticsurvival.command.RsCommand
import com.github.kotyabuchi.realisticsurvival.data.Database
import com.github.kotyabuchi.realisticsurvival.job.JobProgress
import com.github.kotyabuchi.realisticsurvival.job.JobRegistry
import com.github.kotyabuchi.realisticsurvival.job.JobType
import com.github.kotyabuchi.realisticsurvival.job.SkillCaster
import com.github.kotyabuchi.realisticsurvival.menu.MenuCompass
import com.github.kotyabuchi.realisticsurvival.menu.MenuService
import com.github.kotyabuchi.realisticsurvival.player.HomeService
import com.github.kotyabuchi.realisticsurvival.player.PlayerManager
import com.github.kotyabuchi.realisticsurvival.player.ResourceStorageService
import com.github.kotyabuchi.realisticsurvival.util.runTimer
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Level

/**
 * プラグイン本体。各機能はここで作られ、コンストラクタで plugin を受け取って互いを参照する。
 * 初期化の順番に依存関係があるので、onEnable の順序を変えるときは注意すること。
 */
class RealisticSurvival : JavaPlugin() {

    lateinit var database: Database
        private set
    lateinit var players: PlayerManager
        private set
    lateinit var homes: HomeService
        private set
    lateinit var resourceStorage: ResourceStorageService
        private set
    lateinit var jobs: JobRegistry
        private set
    lateinit var progress: JobProgress
        private set
    lateinit var breakContext: BreakContext
        private set
    lateinit var placedBlocks: PlacedBlockTracker
        private set
    lateinit var safeCrop: SafeCropAndReplant
        private set
    lateinit var menus: MenuService
        private set

    override fun onEnable() {
        dataFolder.mkdirs()
        database = Database(File(dataFolder, "RealisticSurvival.db"), logger)
        database.initSchema(JobType.entries.map { it.name })

        homes = HomeService(this).also { it.loadPublicHomes() }
        players = PlayerManager(this)
        resourceStorage = ResourceStorageService(this)
        breakContext = BreakContext(this)
        placedBlocks = PlacedBlockTracker(this)
        safeCrop = SafeCropAndReplant(this)
        progress = JobProgress(this)
        jobs = JobRegistry(this)
        menus = MenuService(this)

        registerListeners(
            players, resourceStorage, breakContext, placedBlocks, safeCrop, menus,
            SkillCaster(this), MenuCompass(this),
        )
        jobs.registerListeners()
        RsCommand.register(this)

        // /reload などで既にプレイヤーがいる場合
        players.loadOnlinePlayers()

        runTimer(AUTO_SAVE_TICKS, AUTO_SAVE_TICKS) {
            players.saveAll().exceptionally { e -> logger.log(Level.SEVERE, "自動保存に失敗しました", e) }
        }
    }

    override fun onDisable() {
        if (!::database.isInitialized) return
        if (::progress.isInitialized) progress.clearAll()
        if (::players.isInitialized) {
            try {
                players.saveAll().join()
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "停止時の保存に失敗しました", e)
            }
        }
        database.close()
    }

    private fun registerListeners(vararg listeners: Listener) {
        listeners.forEach { server.pluginManager.registerEvents(it, this) }
    }

    companion object {
        private const val AUTO_SAVE_TICKS = 20 * 60 * 5L
    }
}
