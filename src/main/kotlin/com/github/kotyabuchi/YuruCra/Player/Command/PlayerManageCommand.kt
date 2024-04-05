package com.github.kotyabuchi.YuruCra.Player.Command

import com.github.kotyabuchi.YuruCra.CommandBaseFrame
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object PlayerManageCommand: CommandBaseFrame() {

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        val result = mutableListOf<String>()

        when (args.size) {
            1 -> {
                main.server.onlinePlayers.forEach {
                    result.add(it.name)
                }
            }
        }

        return result
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        when (command.name) {
            "debugmode" -> {
                if (sender.isOp) {
                    val target = if (args.isEmpty()) {
                        (sender as? Player)?.getStatus() ?: run {
                            sender.sendMessage(Component.text("ゲーム内から実行するか、プレイヤーを指定してください", NamedTextColor.RED))
                            return true
                        }
                    } else {
                        main.server.getPlayer(args[0])?.getStatus() ?: run {
                            sender.sendMessage(Component.text("対象のプレイヤーが見つかりません", NamedTextColor.RED))
                            return true
                        }
                    }
                    target.isDebugMode = !target.isDebugMode
                    sender.sendMessage(Component.text("デバッグモードを[${target.isDebugMode}]に変更しました", NamedTextColor.GREEN))
                }
            }
        }
        return true
    }
}