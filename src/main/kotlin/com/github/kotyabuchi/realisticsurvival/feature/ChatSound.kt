package com.github.kotyabuchi.realisticsurvival.feature

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.runNextTick
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.sound.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

/** チャットが送られたら、受け取る人全員に小さな通知音を鳴らす */
class ChatSound(private val plugin: RealisticSurvival) : Listener {

    private val sound = Sound.sound(org.bukkit.Sound.ENTITY_CHICKEN_EGG, Sound.Source.MASTER, 0.5f, 1.5f)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val viewers = event.viewers().toList()
        // チャットイベントは非同期スレッドで来るので、メインスレッドで鳴らす
        plugin.runNextTick { viewers.forEach { it.playSound(sound) } }
    }
}
