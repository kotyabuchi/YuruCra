package com.github.kotyabuchi.YuruCra.Player.Command

import com.github.kotyabuchi.YuruCra.Menu.HomeMenu.HomeMenu
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object HomeCommand: CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val player = (sender as? Player)?.getStatus() ?: let {
            sender.sendMessage("プレイヤーのみ使用可能なコマンドです。")
            return true
        }

        player.openMenu(HomeMenu(player))
        return true
    }
}