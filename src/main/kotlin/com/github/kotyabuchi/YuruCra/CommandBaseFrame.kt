package com.github.kotyabuchi.YuruCra

import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter

abstract class CommandBaseFrame: CommandExecutor, TabCompleter {

    protected val main: Main = Main.instance
}