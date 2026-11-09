package com.github.kotyabuchi.yurucra

import com.github.kotyabuchi.yurucra.block.BreakContext
import com.github.kotyabuchi.yurucra.block.PlacedBlockTracker
import com.github.kotyabuchi.yurucra.block.SafeCropAndReplant
import com.github.kotyabuchi.yurucra.command.YcCommand
import com.github.kotyabuchi.yurucra.data.Database
import com.github.kotyabuchi.yurucra.feature.ChatSound
import com.github.kotyabuchi.yurucra.feature.ChestSort
import com.github.kotyabuchi.yurucra.feature.DamagePopup
import com.github.kotyabuchi.yurucra.feature.Elevator
import com.github.kotyabuchi.yurucra.feature.FarmlandCare
import com.github.kotyabuchi.yurucra.feature.ResourcePackService
import com.github.kotyabuchi.yurucra.feature.SpawnerUpgrade
import com.github.kotyabuchi.yurucra.feature.StarterItems
import com.github.kotyabuchi.yurucra.feature.StoneGenerator
import com.github.kotyabuchi.yurucra.feature.UtilityRecipes
import com.github.kotyabuchi.yurucra.feature.WorldProtection
import com.github.kotyabuchi.yurucra.item.ItemIdentity
import com.github.kotyabuchi.yurucra.job.JobProgress
import com.github.kotyabuchi.yurucra.job.JobRegistry
import com.github.kotyabuchi.yurucra.job.JobType
import com.github.kotyabuchi.yurucra.job.SkillCaster
import com.github.kotyabuchi.yurucra.menu.MenuCompass
import com.github.kotyabuchi.yurucra.menu.MenuService
import com.github.kotyabuchi.yurucra.monster.AdvancedSkeleton
import com.github.kotyabuchi.yurucra.monster.AdvancedSpider
import com.github.kotyabuchi.yurucra.monster.AdvancedWitch
import com.github.kotyabuchi.yurucra.monster.FightTracker
import com.github.kotyabuchi.yurucra.monster.HelmetMobs
import com.github.kotyabuchi.yurucra.monster.MonsterExplosion
import com.github.kotyabuchi.yurucra.player.HomeService
import com.github.kotyabuchi.yurucra.player.PlayerManager
import com.github.kotyabuchi.yurucra.player.ResourceStorageService
import com.github.kotyabuchi.yurucra.tombstone.TombStoneService
import com.github.kotyabuchi.yurucra.util.runTimer
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Level

/**
 * プラグイン本体。各機能はここで作られ、コンストラクタで plugin を受け取って互いを参照する。
 * 初期化の順番に依存関係があるので、onEnable の順序を変えるときは注意すること。
 */
class YuruCra : JavaPlugin() {

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
    lateinit var tombStones: TombStoneService
        private set
    lateinit var resourcePacks: ResourcePackService
        private set
    lateinit var chestSort: ChestSort
        private set
    lateinit var damagePopup: DamagePopup
        private set
    private lateinit var fights: FightTracker
    private lateinit var skeletons: AdvancedSkeleton
    private lateinit var spiders: AdvancedSpider
    private lateinit var explosions: MonsterExplosion

    override fun onEnable() {
        dataFolder.mkdirs()
        saveDefaultConfig()
        database = Database(File(dataFolder, "YuruCra.db"), logger)
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
        tombStones = TombStoneService(this).also { it.load() }
        resourcePacks = ResourcePackService(this)
        chestSort = ChestSort()
        damagePopup = DamagePopup(this)
        val utilityRecipes = UtilityRecipes(this).also { it.register() }
        // 強化モンスター
        fights = FightTracker(this)
        skeletons = AdvancedSkeleton(this)
        spiders = AdvancedSpider(this)
        explosions = MonsterExplosion(this)
        val witches = AdvancedWitch(this, fights)
        listOf(skeletons, spiders, witches).forEach { fights.register(it) }

        registerListeners(
            players, resourceStorage, breakContext, placedBlocks, safeCrop, menus, tombStones, resourcePacks,
            chestSort, damagePopup, utilityRecipes,
            fights, skeletons, spiders, witches, explosions, HelmetMobs(),
            ItemIdentity(this),
            SkillCaster(this), MenuCompass(this),
            // 便利機能
            Elevator(), WorldProtection(), StoneGenerator(this), SpawnerUpgrade(),
            FarmlandCare(this), ChatSound(this), StarterItems(),
        )
        jobs.registerListeners()
        YcCommand.register(this)

        // /reload などで既にプレイヤーがいる場合
        players.loadOnlinePlayers()

        // ハッシュの計算（ダウンロード）は非同期。終わり次第、参加中の人にも送る
        resourcePacks.reload { result -> result.onFailure { logger.warning("リソースパックを配布しません: ${it.message}") } }

        runTimer(AUTO_SAVE_TICKS, AUTO_SAVE_TICKS) {
            players.saveAll().exceptionally { e -> logger.log(Level.SEVERE, "自動保存に失敗しました", e) }
        }
    }

    override fun onDisable() {
        if (::damagePopup.isInitialized) damagePopup.clear()
        // ワールドに一時的な物（骨・クモの巣・飛んでいるブロック）を残さない
        if (::fights.isInitialized) fights.clear()
        if (::skeletons.isInitialized) skeletons.clear()
        if (::spiders.isInitialized) spiders.clear()
        if (::explosions.isInitialized) explosions.restoreAll()
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
