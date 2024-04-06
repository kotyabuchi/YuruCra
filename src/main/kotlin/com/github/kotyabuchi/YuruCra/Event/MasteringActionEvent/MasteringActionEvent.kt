package com.github.kotyabuchi.YuruCra.Event.MasteringActionEvent

import com.github.kotyabuchi.YuruCra.Mastering.Mastering
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

abstract class MasteringActionEvent(player: Player, val mastering: Mastering): PlayerEvent(player) {

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }

    override fun getHandlers(): HandlerList = handlerList
}