package com.github.kotyabuchi.yurucra.command

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.job.JobType
import com.github.kotyabuchi.yurucra.menu.MainMenu
import com.github.kotyabuchi.yurucra.util.floorDigits
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Player

/**
 * /yc menu                                              メインメニューを開く
 * /yc save                                              全員のデータを保存する（OP）
 * /yc job <set|add|remove> <exp|level> <job> <量> [対象]  ジョブの経験値・レベルを変える（OP）
 * /yc tombstone ...                                     墓石の管理（OP。TombStoneCommand を参照）
 * /yc resourcepack reload                               リソースパックを読み込み直して全員に送り直す（OP）
 */
object YcCommand {

    private enum class Operation { SET, ADD, REMOVE }
    private enum class ValueKind { EXP, LEVEL }

    fun register(plugin: YuruCra) {
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(build(plugin), "YuruCra のコマンド", listOf("yurucra"))
        }
    }

    private fun build(plugin: YuruCra): LiteralCommandNode<CommandSourceStack> {
        val job = Commands.literal("job").requires { it.sender.isOp }
        for (operation in Operation.entries) {
            val operationNode = Commands.literal(operation.name.lowercase())
            for (unit in ValueKind.entries) {
                val unitNode = Commands.literal(unit.name.lowercase())
                for (type in JobType.entries) {
                    unitNode.then(
                        Commands.literal(type.name.lowercase()).then(
                            Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                .executes { ctx -> changeJob(plugin, ctx, operation, unit, type, targetFromArgument = false) }
                                .then(
                                    Commands.argument("target", ArgumentTypes.player())
                                        .executes { ctx -> changeJob(plugin, ctx, operation, unit, type, targetFromArgument = true) }
                                )
                        )
                    )
                }
                operationNode.then(unitNode)
            }
            job.then(operationNode)
        }

        return Commands.literal("yc")
            .then(
                Commands.literal("menu")
                    .requires { it.executor is Player }
                    .executes { ctx ->
                        val player = ctx.source.executor as Player
                        plugin.menus.open(player, MainMenu(plugin))
                        Command.SINGLE_SUCCESS
                    }
            )
            .then(
                Commands.literal("save")
                    .requires { it.sender.isOp }
                    .executes { ctx ->
                        val sender = ctx.source.sender
                        plugin.players.saveAll().whenComplete { _, error ->
                            if (error == null) sender.sendPlainMessage("プレイヤーデータを保存しました")
                            else sender.sendPlainMessage("保存に失敗しました: ${error.message}")
                        }
                        Command.SINGLE_SUCCESS
                    }
            )
            .then(job)
            .then(TombStoneCommand.build(plugin))
            .then(
                Commands.literal("resourcepack").requires { it.sender.isOp }.then(
                    Commands.literal("reload").executes { ctx ->
                        val sender = ctx.source.sender
                        sender.sendPlainMessage("リソースパックを読み込み直しています...")
                        plugin.resourcePacks.reload { result ->
                            result
                                .onSuccess { sender.sendPlainMessage("参加中の全員に送り直しました (sha1=$it)") }
                                .onFailure { sender.sendPlainMessage("配布できませんでした: ${it.message}") }
                        }
                        Command.SINGLE_SUCCESS
                    }
                )
            )
            .build()
    }

    private fun changeJob(
        plugin: YuruCra,
        ctx: CommandContext<CommandSourceStack>,
        operation: Operation,
        unit: ValueKind,
        type: JobType,
        targetFromArgument: Boolean,
    ): Int {
        val sender = ctx.source.sender
        val target = if (targetFromArgument) {
            ctx.getArgument("target", PlayerSelectorArgumentResolver::class.java).resolve(ctx.source).firstOrNull()
        } else {
            ctx.source.executor as? Player
        }
        if (target == null) {
            sender.sendPlainMessage("対象のプレイヤーが見つかりません")
            return 0
        }
        val amount = DoubleArgumentType.getDouble(ctx, "amount")
        val levels = amount.toInt()
        val job = plugin.jobs[type]
        val status = plugin.players.status(target).job(type)
        status.resetCombo()

        when (operation to unit) {
            Operation.SET to ValueKind.EXP -> status.setExpInLevel(amount)
            Operation.SET to ValueKind.LEVEL -> status.setLevel(levels)
            // 経験値の追加は通常の獲得と同じ流れにする（レベルアップ通知・スキル習得あり）
            Operation.ADD to ValueKind.EXP -> plugin.progress.addExp(target, job, amount, increaseCombo = 0)
            Operation.ADD to ValueKind.LEVEL -> status.addLevel(levels)
            Operation.REMOVE to ValueKind.EXP -> status.setTotalExp(status.totalExp - amount)
            Operation.REMOVE to ValueKind.LEVEL -> status.setLevel(status.level - levels)
        }
        sender.sendPlainMessage("${target.name} の ${type.displayName}: Lv.${status.level} (${status.exp.floorDigits(2)}/${status.nextLevelExp})")
        return Command.SINGLE_SUCCESS
    }
}
