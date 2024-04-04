package com.github.kotyabuchi.YuruCra.Item

import com.github.kotyabuchi.YuruCra.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import kotlin.math.roundToInt

object ItemExtensionCommand: CommandExecutor, TabCompleter {

    private val main: Main = Main.instance
    private val args2List: List<String> = listOf("damage")

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
            2 -> {
                args2List.forEach {
                    if (it.contains(args[0], true)) result.add(it)
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
        val commandLength = args.size
        if (commandLength <= 2) {
            return false
        }
        val targetPlayer = main.server.getPlayer(args[0]) ?: run {
            sender.sendMessage(Component.text("対象のプレイヤーが見つかりません", NamedTextColor.RED))
            return true
        }
        val item = ItemExtension(targetPlayer.inventory.itemInMainHand)
        if (item.itemStack.type.isAir) {
            sender.sendMessage(Component.text("手に対象のアイテムを持ってください", NamedTextColor.RED))
            return true
        }
        when (commandLength) {
            3 -> {
                when (args[1]) {
                    "damage" -> {
                        val damageArgs = args[2]
                        val damage = if (damageArgs.endsWith("%")) {
                            val percentage = (damageArgs.substring(0, damageArgs.length - 1).toIntOrNull()?.div(100.0)) ?: run {
                                sender.sendMessage(Component.text("数値または％を使用した割合のみ使用できます", NamedTextColor.RED))
                                return true
                            }
                            (item.maxDurability * percentage).roundToInt()
                        } else {
                            damageArgs.toIntOrNull() ?: return true
                        }
                        item.damage(damage, targetPlayer).applyDurability().applySetting()
                    }
                }
            }
        }
        return true
    }
}