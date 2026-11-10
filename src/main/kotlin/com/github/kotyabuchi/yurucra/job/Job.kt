package com.github.kotyabuchi.yurucra.job

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.skill.PassiveSkill
import com.github.kotyabuchi.yurucra.skill.Skill
import com.github.kotyabuchi.yurucra.skill.SkillCommand
import com.github.kotyabuchi.yurucra.util.upperCamelCase
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Listener

enum class JobCategory { GATHERING, COMBAT }

/**
 * ジョブの種類。名前はDBの jobs.job_name と一致させる（変えると保存データと紐付かなくなる）。
 */
enum class JobType(val icon: Material, val category: JobCategory) {
    MINER(Material.STONE_PICKAXE, JobCategory.GATHERING),
    EXCAVATOR(Material.STONE_SHOVEL, JobCategory.GATHERING),
    LUMBERJACK(Material.STONE_AXE, JobCategory.GATHERING),
    FARMER(Material.STONE_HOE, JobCategory.GATHERING),
    SWORD_MASTER(Material.IRON_SWORD, JobCategory.COMBAT),
    BATTLE_AXE(Material.IRON_AXE, JobCategory.COMBAT),
    TAMER(Material.BONE, JobCategory.COMBAT),
    ;

    val displayName: String = name.upperCamelCase()
}

abstract class Job(protected val plugin: YuruCra, val type: JobType) : Listener {

    private val tools = mutableSetOf<Material>()
    private val _activeSkills = linkedMapOf<SkillCommand, Skill>()
    private val _passiveSkills = mutableListOf<PassiveSkill>()

    /** 詠唱コマンド（左右クリック3回）で発動するスキル */
    val activeSkills: Map<SkillCommand, Skill> get() = _activeSkills
    /** レベルで習得し、メニューからオン・オフするスキル */
    val passiveSkills: List<PassiveSkill> get() = _passiveSkills
    val allSkills: List<Skill> get() = _activeSkills.values + _passiveSkills

    fun isJobTool(material: Material): Boolean = material in tools

    protected fun addTools(materials: Collection<Material>) {
        tools += materials
    }

    protected fun registerSkill(command: SkillCommand, skill: Skill) {
        _activeSkills[command] = skill
    }

    protected fun registerPassiveSkill(skill: PassiveSkill) {
        _passiveSkills += skill
    }

    fun level(player: Player): Int = plugin.players.status(player).job(type).level

    open fun onLevelUp(player: Player) {}
}
