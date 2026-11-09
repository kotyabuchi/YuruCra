package com.github.kotyabuchi.yurucra.skill.passive

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.job.GatheringEvent
import com.github.kotyabuchi.yurucra.job.GatheringJob
import com.github.kotyabuchi.yurucra.job.Job
import com.github.kotyabuchi.yurucra.skill.PassiveSkill
import com.github.kotyabuchi.yurucra.skill.ToolLinkedEffectSkill
import com.github.kotyabuchi.yurucra.util.WeightedTable
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.min
import kotlin.random.Random

/** 土や砂を掘ると、低確率で珍しいアイテムが出る（最大5%） */
class Archaeologist(plugin: YuruCra, private val job: GatheringJob) : PassiveSkill(plugin, job) {
    override val id = "ARCHAEOLOGIST"
    override val displayName = "Archaeologist"
    override val needLevel = 20
    override val description = "土や砂を破壊した際に低確率で希少なアイテムがドロップする"

    private val drops = WeightedTable(
        Material.BONE to 10,
        Material.NAME_TAG to 3,
        Material.REDSTONE to 3,
        Material.GLOWSTONE to 2,
        Material.DIAMOND to 1,
    )

    @EventHandler
    fun onGather(event: GatheringEvent) {
        if (event.job != job) return
        val player = event.player
        if (!isEnabled(player)) return
        val chancePerMille = min(50.0, job.level(player) * 0.5)
        if (Random.nextInt(1000) >= chancePerMille) return
        val material = drops.pick() ?: return
        val location = event.block.location.toCenterLocation()
        location.world.dropItem(location, ItemStack(material))
    }
}

/** 石を掘ると、ごく低確率で鉱石が出る（レベル100で0.1%） */
class GemCollector(plugin: YuruCra, private val job: GatheringJob) : PassiveSkill(plugin, job) {
    override val id = "GEM_COLLECTOR"
    override val displayName = "Gem Collector"
    override val needLevel = 0
    override val description = "石を破壊した際に低確率で鉱石がドロップする"

    private val drops = WeightedTable(
        Material.COAL to 40,
        Material.RAW_COPPER to 20,
        Material.RAW_IRON to 15,
        Material.RAW_GOLD to 5,
        Material.REDSTONE to 5,
        Material.LAPIS_LAZULI to 3,
        Material.DIAMOND to 1,
        Material.EMERALD to 1,
    )

    @EventHandler
    fun onGather(event: GatheringEvent) {
        if (event.job != job) return
        if (event.block.type != Material.STONE) return
        val player = event.player
        if (!isEnabled(player)) return
        val chancePerMille = min(100.0, job.level(player) / 100.0)
        if (Random.nextInt(1000) >= chancePerMille) return
        val material = drops.pick() ?: return
        val location = event.block.location.toCenterLocation()
        location.world.dropItem(location, ItemStack(material))
    }
}

/**
 * ジョブの道具を持っている間、採掘速度上昇がかかる。
 * レベル20で採掘速度上昇II、以降20レベルごとに1段階上がる。
 * 採掘士と掘削士の両方にあるので、IDにジョブ名を含めてオン・オフを別々にする。
 */
class HadesBlessing(plugin: YuruCra, owner: Job) : ToolLinkedEffectSkill(plugin, owner) {
    override val id = "HADES_BLESSING_${owner.type.name}"
    override val displayName = "Hades Blessing"
    override val needLevel = 20
    override val description = "採掘速度が上昇する"

    override fun applyEffect(player: Player) {
        val amplifier = (owner.level(player) - 20) / 20 + 1
        player.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 30, amplifier, true, false, false))
    }
}
