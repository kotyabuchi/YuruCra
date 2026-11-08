package com.github.kotyabuchi.realisticsurvival.job

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.skill.TimedToggleSkill
import com.github.kotyabuchi.realisticsurvival.skill.ToolLinkedEffectSkill
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.EnumMap

/** 全ジョブのインスタンスを持つ。スキルのイベント登録もここで行う */
class JobRegistry(private val plugin: RealisticSurvival) {

    private val jobs: Map<JobType, Job> = EnumMap<JobType, Job>(JobType::class.java).apply {
        put(JobType.MINER, Miner(plugin))
        put(JobType.EXCAVATOR, Excavator(plugin))
        put(JobType.LUMBERJACK, Lumberjack(plugin))
        put(JobType.FARMER, Farmer(plugin))
    }

    init {
        check(jobs.keys == JobType.entries.toSet()) { "JobType に対応するジョブクラスが足りません" }
    }

    operator fun get(type: JobType): Job = jobs.getValue(type)

    val all: Collection<Job> get() = jobs.values

    /** その道具を使うジョブ */
    fun byTool(material: Material): Job? = jobs.values.firstOrNull { it.isJobTool(material) }

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
