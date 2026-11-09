package com.github.kotyabuchi.yurucra.skill.passive

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.job.GatheringJob
import com.github.kotyabuchi.yurucra.job.MultiBreakEvent
import com.github.kotyabuchi.yurucra.skill.PassiveSkill
import com.github.kotyabuchi.yurucra.skill.gathering.MultiBreak
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack

/**
 * Mine Assist で掘った鉱石の跡を石で埋め、ドロップは直接手元に送る。
 * 埋める石はリソースストレージ → インベントリの順に消費する。Multi Break 中は働かない。
 */
class StoneReplacer(plugin: YuruCra, job: GatheringJob) : PassiveSkill(plugin, job) {
    override val id = "STONE_REPLACER"
    override val displayName = "Stone Replacer"
    override val needLevel = 0
    override val description = "[Mine Assist]使用時に鉱石を石に置き換える"

    // 経験値・ドロップ増加（NORMAL）が済んでから、ドロップを集める処理（HIGHEST）より先に動く
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDrop(event: BlockDropItemEvent) {
        val player = event.player
        val source = plugin.breakContext.sourceOf(player, event.block) ?: return
        if (!source.isMineAssist || source.isMultiBreak) return
        if (!isEnabled(player)) return

        val fill = plugin.resourceStorage.consumeFillBlock(player, event.block) ?: return
        event.items.map { it.itemStack }.forEach { plugin.resourceStorage.storeOrGive(player, it) }
        event.items.clear()
        event.block.type = fill
    }
}

/**
 * Multi Break で掘ったとき、掘った範囲の外周と奥の空洞・液体・砂利などを石で埋めて、
 * トンネル状にする（溶岩や水が流れ込まないように）。
 */
class TunnelAssist(plugin: YuruCra, job: GatheringJob) : PassiveSkill(plugin, job) {
    override val id = "TUNNEL_ASSIST"
    override val displayName = "Tunnel Assist"
    override val needLevel = 100
    // 旧RealisticSurvivalの説明文は「[Mine Assist]使用時」だったが、実際は Multi Break で動くので直した
    override val description = "[Multi Break]使用時にトンネル状に外壁を作成する"

    @EventHandler
    fun onMultiBreak(event: MultiBreakEvent) {
        val player = event.player
        if (event.job != owner) return
        if (!isEnabled(player)) return
        if (!owner.isJobTool(player.inventory.itemInMainHand.type)) return

        // 掘った範囲より一回り大きい範囲の、外周と奥を埋める
        val radius = MultiBreak.radius(owner.level(player)) + 1
        val area = MultiBreak.area(event.centerBlock, event.face, radius, yBottom = -2, yTopExclusive = radius * 2 - 1)
        if (area.isEmpty()) return
        val back = event.face.oppositeFace
        val minX = area.minOf { it.x }; val maxX = area.maxOf { it.x }
        val minY = area.minOf { it.y }; val maxY = area.maxOf { it.y }
        val minZ = area.minOf { it.z }; val maxZ = area.maxOf { it.z }

        for (block in area) {
            val isWall = (minX != maxX && (block.x == minX || block.x == maxX)) ||
                (minY != maxY && (block.y == minY || block.y == maxY)) ||
                (minZ != maxZ && (block.z == minZ || block.z == maxZ))
            fill(player, if (isWall) block else block.getRelative(back))
        }
    }

    private fun fill(player: Player, block: Block) {
        val material = block.type
        if (!material.isAir && !block.isLiquid && !material.hasGravity()) return
        val fill = plugin.resourceStorage.consumeFillBlock(player, block) ?: return
        // 砂利や砂は消さずに手元に回収する
        if (material.hasGravity()) plugin.resourceStorage.storeOrGive(player, ItemStack(material))
        block.type = fill
    }
}
