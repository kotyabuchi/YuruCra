package com.github.kotyabuchi.YuruCra.Event

import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class GatheringItemDropEvent(val baseMaterial: Material, val items: List<Item>, player: Player): PlayerEvent(player) {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }

    override fun getHandlers(): HandlerList = handlerList
}