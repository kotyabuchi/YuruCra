package com.github.kotyabuchi.yurucra.job

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.skill.SkillCommand
import com.github.kotyabuchi.yurucra.skill.gathering.MineAssist
import com.github.kotyabuchi.yurucra.skill.gathering.MultiBreak
import com.github.kotyabuchi.yurucra.skill.gathering.TotemOfDemeter
import com.github.kotyabuchi.yurucra.skill.gathering.TreeAssist
import com.github.kotyabuchi.yurucra.skill.passive.Archaeologist
import com.github.kotyabuchi.yurucra.skill.passive.AutoReplantSapling
import com.github.kotyabuchi.yurucra.skill.passive.GemCollector
import com.github.kotyabuchi.yurucra.skill.passive.HadesBlessing
import com.github.kotyabuchi.yurucra.skill.passive.StoneReplacer
import com.github.kotyabuchi.yurucra.skill.passive.TunnelAssist
import com.github.kotyabuchi.yurucra.util.ToolType
import com.github.kotyabuchi.yurucra.util.isWood
import org.bukkit.Material
import org.bukkit.Tag

class Miner(plugin: YuruCra) : GatheringJob(plugin, JobType.MINER, canGetExpWithHand = false) {
    init {
        addTools(ToolType.PICKAXE.materials)

        addExp(1, Tag.TERRACOTTA.values)
        addExp(
            1,
            Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.NETHERRACK,
            Material.SANDSTONE, Material.SMOOTH_SANDSTONE, Material.RED_SANDSTONE,
            Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM, Material.MOSSY_COBBLESTONE, Material.BASALT, Material.BLACKSTONE,
            Material.DEEPSLATE, Material.CALCITE, Material.TUFF,
        )
        addExp(2, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE)
        addExp(3, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.END_STONE, Material.GLOWSTONE)
        addExp(4, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.NETHER_QUARTZ_ORE)
        addExp(5, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.OBSIDIAN, Material.NETHER_GOLD_ORE)
        addExp(7, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE)
        addExp(8, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE)
        addExp(10, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE)

        registerSkill(SkillCommand.LRL, MineAssist(plugin))
        registerSkill(SkillCommand.RLR, MultiBreak(plugin, this, "MULTI_BREAK_MINER", ToolType.PICKAXE))
        registerPassiveSkill(GemCollector(plugin, this))
        registerPassiveSkill(StoneReplacer(plugin, this))
        registerPassiveSkill(TunnelAssist(plugin, this))
        registerPassiveSkill(HadesBlessing(plugin, this))
    }
}

class Excavator(plugin: YuruCra) : GatheringJob(plugin, JobType.EXCAVATOR) {
    init {
        addTools(ToolType.SHOVEL.materials)

        addExp(
            1,
            Material.DIRT, Material.SAND, Material.GRASS_BLOCK, Material.GRAVEL, Material.FARMLAND,
            Material.DIRT_PATH, Material.COARSE_DIRT, Material.PODZOL, Material.RED_SAND, Material.SOUL_SAND, Material.SOUL_SOIL,
        )
        addExp(2, Material.CLAY)

        registerSkill(SkillCommand.RLR, MultiBreak(plugin, this, "MULTI_BREAK_EXCAVATOR", ToolType.SHOVEL))
        registerPassiveSkill(Archaeologist(plugin, this))
        registerPassiveSkill(HadesBlessing(plugin, this))
    }
}

class Lumberjack(plugin: YuruCra) : GatheringJob(plugin, JobType.LUMBERJACK) {
    init {
        addTools(ToolType.AXE.materials)
        addExp(1, Material.entries.filter { it.isWood() })

        registerSkill(SkillCommand.LRL, TreeAssist(plugin, this))
        registerPassiveSkill(AutoReplantSapling(plugin, this))
    }
}

class Farmer(plugin: YuruCra) : GatheringJob(plugin, JobType.FARMER) {
    init {
        addTools(ToolType.HOE.materials)
        addExp(1, Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS)
        addExp(2, Material.MELON)
        addExp(10, Material.PUMPKIN)

        registerSkill(SkillCommand.LLL, TotemOfDemeter(plugin))
    }
}
