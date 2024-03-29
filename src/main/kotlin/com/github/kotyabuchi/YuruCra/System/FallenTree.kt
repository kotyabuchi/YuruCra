package com.github.kotyabuchi.YuruCra.System

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.TreeType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import java.util.Random

object FallenTree: Listener {

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
        val material = event.itemInHand.type
        player.sendMessage(Component.text(material.name))
        if (!material.name.contains("_SAPLING") || material.name.contains("POTTED_")) return
        event.isCancelled = true
        block.type = Material.GLASS
        val treeTypeName = material.name.removePrefix("POTTED_").removeSuffix("_SAPLING")
        player.sendMessage(treeTypeName)
        val treeType = if (TreeType.values().map { it.name }.contains(treeTypeName)) TreeType.valueOf(treeTypeName) else TreeType.TREE
        player.sendMessage(block.world.generateTree(block.location, Random(), treeType).toString())
    }
}