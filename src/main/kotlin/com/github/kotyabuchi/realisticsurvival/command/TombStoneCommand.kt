package com.github.kotyabuchi.realisticsurvival.command

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.tombstone.TombStone
import com.github.kotyabuchi.realisticsurvival.tombstone.TombStoneListMenu
import com.github.kotyabuchi.realisticsurvival.tombstone.TombStoneViewMenu
import com.github.kotyabuchi.realisticsurvival.util.thenOnMain
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.Date

/**
 * /rs tombstone list <対象>          対象の墓石の一覧（OP。プレイヤーはGUIで開く。左クリックで中身、右クリックでテレポート・削除）
 * /rs tombstone show <対象> <ID>     墓石の中身を見る（取り出しはできない。OP・プレイヤーのみ）
 * /rs tombstone remove <対象> <ID>   墓石を中身ごと消す（OP）
 */
object TombStoneCommand {

    fun build(plugin: RealisticSurvival): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal("tombstone")
            .requires { it.sender.isOp }
            .then(
                Commands.literal("list").then(
                    Commands.argument("target", ArgumentTypes.player()).executes { ctx -> list(plugin, ctx) }
                )
            )
            .then(
                Commands.literal("show").requires { it.executor is Player }.then(
                    Commands.argument("target", ArgumentTypes.player()).then(
                        Commands.argument("id", IntegerArgumentType.integer(1)).executes { ctx -> show(plugin, ctx) }
                    )
                )
            )
            .then(
                Commands.literal("remove").then(
                    Commands.argument("target", ArgumentTypes.player()).then(
                        Commands.argument("id", IntegerArgumentType.integer(1)).executes { ctx -> remove(plugin, ctx) }
                    )
                )
            )

    private fun list(plugin: RealisticSurvival, ctx: CommandContext<CommandSourceStack>): Int {
        val sender = ctx.source.sender
        val target = target(ctx) ?: return notFound(ctx)
        val stones = plugin.tombStones.tombStonesOf(target.uniqueId)
        if (stones.isEmpty()) {
            sender.sendPlainMessage("${target.name} の墓石はありません")
            return Command.SINGLE_SUCCESS
        }
        // プレイヤーならGUIで開く（クリックで中身を表示）。コンソールは文字で一覧を出す
        val viewer = ctx.source.executor as? Player
        if (viewer != null) {
            plugin.menus.open(viewer, TombStoneListMenu(plugin, target))
            return Command.SINGLE_SUCCESS
        }
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm")
        sender.sendPlainMessage("=== ${target.name} の墓石 (${stones.size}件) ===")
        stones.forEach {
            sender.sendPlainMessage("#${it.id}  ${format.format(Date(it.createdAt))}  ${it.worldName} (${it.x.toInt()}, ${it.y.toInt()}, ${it.z.toInt()})")
        }
        return Command.SINGLE_SUCCESS
    }

    private fun show(plugin: RealisticSurvival, ctx: CommandContext<CommandSourceStack>): Int {
        val viewer = ctx.source.executor as? Player ?: return 0
        val target = target(ctx) ?: return notFound(ctx)
        val stone = stone(plugin, ctx, target) ?: return 0
        plugin.tombStones.loadItems(stone).thenOnMain(plugin) { items ->
            if (items == null) {
                viewer.sendPlainMessage("墓石 #${stone.id} の中身が見つかりません")
                return@thenOnMain
            }
            plugin.menus.open(viewer, TombStoneViewMenu(plugin, "${target.name}'s TombStone #${stone.id}", items))
        }
        return Command.SINGLE_SUCCESS
    }

    private fun remove(plugin: RealisticSurvival, ctx: CommandContext<CommandSourceStack>): Int {
        val sender = ctx.source.sender
        val target = target(ctx) ?: return notFound(ctx)
        val stone = stone(plugin, ctx, target) ?: return 0
        plugin.tombStones.delete(stone) { sender.sendPlainMessage("墓石 #${stone.id} を削除しました") }
        return Command.SINGLE_SUCCESS
    }

    private fun target(ctx: CommandContext<CommandSourceStack>): Player? =
        ctx.getArgument("target", PlayerSelectorArgumentResolver::class.java).resolve(ctx.source).firstOrNull()

    private fun stone(plugin: RealisticSurvival, ctx: CommandContext<CommandSourceStack>, target: Player): TombStone? {
        val id = IntegerArgumentType.getInteger(ctx, "id")
        return plugin.tombStones.find(target.uniqueId, id)
            ?: null.also { ctx.source.sender.sendPlainMessage("${target.name} の墓石 #$id は見つかりません") }
    }

    private fun notFound(ctx: CommandContext<CommandSourceStack>): Int {
        ctx.source.sender.sendPlainMessage("対象のプレイヤーが見つかりません")
        return 0
    }
}
