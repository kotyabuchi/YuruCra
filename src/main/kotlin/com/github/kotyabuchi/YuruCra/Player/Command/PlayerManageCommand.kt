package com.github.kotyabuchi.YuruCra.Player.Command

import com.github.kotyabuchi.YuruCra.CommandBaseFrame
import com.github.kotyabuchi.YuruCra.Mastering.MasteringType
import com.github.kotyabuchi.YuruCra.Player.PlayerStatus.Companion.getStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.round

object PlayerManageCommand: CommandBaseFrame() {

    private val server = main.server

    private val args1List = listOf("mastering", "debug")
    private val masteringArgs2List = listOf("set", "increase", "decrease")
    private val masteringArgs3List = listOf("xp", "level")

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        val result = mutableListOf<String>()
        if (!sender.isOp) return result
        when (args.size) {
            1 -> {
                args1List.forEach {
                    if (it.contains(args[0], true)) result.add(it)
                }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "mastering" -> {
                        masteringArgs2List.forEach {
                            if (it.contains(args[1], true)) result.add(it)
                        }
                    }
                    "debug" -> {
                        server.onlinePlayers.forEach {
                            result.add(it.name)
                        }
                    }
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "mastering" -> {
                        masteringArgs3List.forEach {
                            if (it.contains(args[2], true)) result.add(it)
                        }
                    }
                }
            }
            5 -> {
                when (args[0].lowercase()) {
                    "mastering" -> {
                        MasteringType.values().forEach {
                            if (it.name.contains(args[4], true)) result.add(it.regularName)
                        }
                    }
                }
            }
            6 -> {
                when (args[0].lowercase()) {
                    "mastering" -> {
                        server.onlinePlayers.forEach {
                            result.add(it.name)
                        }
                    }
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
        if (args.isEmpty()) return true
        if (!sender.isOp) return true
        when (args[0].lowercase()) {
            "debug" -> {
                val target = if (args.size == 1) {
                    (sender as? Player)?.getStatus() ?: run {
                        sender.sendMessage(Component.text("ゲーム内から実行するか、プレイヤーを指定してください", NamedTextColor.RED))
                        return true
                    }
                } else {
                    server.getPlayer(args[1])?.getStatus() ?: run {
                        sender.sendMessage(Component.text("対象のプレイヤーが見つかりません", NamedTextColor.RED))
                        return true
                    }
                }
                target.isDebugMode = !target.isDebugMode
                sender.sendMessage(Component.text("デバッグモードを[${target.isDebugMode}]に変更しました", NamedTextColor.GREEN))
            }
            "mastering" -> {
                if (args.size < 5) return true
                val type = args[2]
                val doubleAmount = args[3].toDoubleOrNull() ?: return true
                val intAmount = round(doubleAmount).toInt()
                val target = if (args.size < 6) {
                    sender as? Player ?: return true
                } else {
                    main.server.getPlayer(args[5]) ?: return true
                }
                val targetJob = MasteringType.valueOf(args[4].uppercase()).masteringClass
                val playerStatus = target.getStatus()
                val jobStatus = playerStatus.masteringManager.getMasteringStatus(targetJob)
                jobStatus.resetCombo()
                when (args[1].lowercase()) {
                    "set" -> {
                        when (type.lowercase()) {
                            "xp" -> {
                                jobStatus.setExp(doubleAmount, true)
                            }
                            "level" -> {
                                jobStatus.setLevel(intAmount)
                            }
                        }
                    }
                    "increase" -> {
                        when (type.lowercase()) {
                            "xp" -> {
                                jobStatus.addExp(doubleAmount, 0)
                            }
                            "level" -> {
                                jobStatus.addLevel(intAmount)
                            }
                        }
                    }
                    "decrease" -> {
                        when (type.lowercase()) {
                            "xp" -> {
                                jobStatus.setTotalExp(jobStatus.getTotalExp() - doubleAmount)
                            }
                            "level" -> {
                                jobStatus.setLevel(jobStatus.getLevel() - intAmount)
                            }
                        }
                    }
                }
            }
        }
        return true
    }
}