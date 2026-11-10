package com.github.kotyabuchi.yurucra.job

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.skill.TimedToggleSkill
import com.github.kotyabuchi.yurucra.skill.ToolLinkedEffectSkill
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.EnumMap

/** 全ジョブのインスタンスを持つ。スキルのイベント登録もここで行う */
class JobRegistry(private val plugin: YuruCra) {

    private val jobs: Map<JobType, Job> = EnumMap<JobType, Job>(JobType::class.java).apply {
        put(JobType.MINER, Miner(plugin))
        put(JobType.EXCAVATOR, Excavator(plugin))
        put(JobType.LUMBERJACK, Lumberjack(plugin))
        put(JobType.FARMER, Farmer(plugin))
        put(JobType.SWORD_MASTER, SwordMaster(plugin))
        put(JobType.BATTLE_AXE, BattleAxe(plugin))
        put(JobType.TAMER, Tamer(plugin))
    }

    init {
        check(jobs.keys == JobType.entries.toSet()) { "JobType に対応するジョブクラスが足りません" }
    }

    operator fun get(type: JobType): Job = jobs.getValue(type)

    val all: Collection<Job> get() = jobs.values

    /** その道具を使うジョブ */
    fun byTool(material: Material): Job? = jobs.values.firstOrNull { it.isJobTool(material) }

    private val gatheringJobs = jobs.values.filterIsInstance<GatheringJob>()

    /** どれかの採集系ジョブで経験値がもらえる素材か */
    fun isGatheringTarget(material: Material): Boolean = gatheringJobs.any { it.isTargetMaterial(material) }

    fun registerListeners() {
        val pm = plugin.server.pluginManager
        jobs.values.forEach { job ->
            pm.registerEvents(job, plugin)
            job.allSkills.forEach { skill ->
                pm.registerEvents(skill, plugin)
                if (skill is ToolLinkedEffectSkill) skill.start()
            }
        }
    }

    /** 時間制限付きスキルの「発動中」状態を消す（再起動・再ログインで残らないように） */
    fun clearTimedSkills(player: Player) {
        jobs.values.flatMap { it.allSkills }.filterIsInstance<TimedToggleSkill>().forEach { it.forceClear(player) }
    }
}
