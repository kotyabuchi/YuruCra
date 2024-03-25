package com.github.kotyabuchi.YuruCra.System

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.sound.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object ChatSound: Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onChat(event: AsyncChatEvent) {
        if (event.isCancelled) return
        event.viewers().forEach {
            it.playSound(Sound.sound(org.bukkit.Sound.ENTITY_CHICKEN_EGG.key, Sound.Source.MASTER, .6f, 1.5f), Sound.Emitter.self())
        }
    }
}