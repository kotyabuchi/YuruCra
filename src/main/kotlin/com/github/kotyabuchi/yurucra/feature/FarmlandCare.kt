package com.github.kotyabuchi.yurucra.feature

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.util.ToolType
import com.github.kotyabuchi.yurucra.util.runLater
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * 農地まわりの補助。
 * - 耕地はプレイヤーやモブが踏んでも荒れない
 * - スニークしながらシャベルで耕地・土の道を右クリックすると、土に戻す
 */
class FarmlandCare(private val plugin: YuruCra) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onTrample(event: EntityChangeBlockEvent) {
        if (event.block.type == Material.FARMLAND && event.to == Material.DIRT) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onLevel(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        if (!player.isSneaking) return
        val item = event.item ?: return
        if (!ToolType.SHOVEL.includes(item)) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.FARMLAND && block.type != Material.DIRT_PATH) return
        val hand = event.hand ?: return

        // 同じtickだと他の右クリック処理と競合するので、1tick後に変える
        plugin.runLater(1) {
            if (block.type != Material.FARMLAND && block.type != Material.DIRT_PATH) return@runLater
            if (hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
            block.type = Material.DIRT
            block.world.playSound(block.location.toCenterLocation(), Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1f, 1f)
        }
    }
}
