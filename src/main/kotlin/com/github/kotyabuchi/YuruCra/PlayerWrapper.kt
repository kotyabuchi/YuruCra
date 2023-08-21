package com.github.kotyabuchi.YuruCra

import org.bukkit.entity.Player
import java.time.LocalDateTime

class PlayerWrapper(player: Player): Player by player {

    companion object {
        private val players = mutableMapOf<Player, PlayerWrapper>()
        fun Player.getWrapper(): PlayerWrapper = players.getOrPut(this) { PlayerWrapper(this) }
        fun removeCache(player: Player) {
            players.remove(player)
        }
    }

    var playTime: Long = 0
    var lastLogin: LocalDateTime = LocalDateTime.now()
    val loginTime: LocalDateTime = LocalDateTime.now()
    var lastPlayVersion: Double = 0.0
}