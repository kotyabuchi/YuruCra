package com.github.kotyabuchi.YuruCra.Event

import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityEvent

class EntityDropItemsEvent(entity: Entity, val dropItems: List<Item>): EntityEvent(entity) {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }

    override fun getHandlers(): HandlerList = handlerList
}