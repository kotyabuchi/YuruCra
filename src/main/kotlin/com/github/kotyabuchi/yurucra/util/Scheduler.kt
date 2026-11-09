package com.github.kotyabuchi.yurucra.util

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.logging.Level

fun Plugin.runNextTick(task: () -> Unit): BukkitTask =
    server.scheduler.runTask(this, Runnable(task))

fun Plugin.runLater(delayTicks: Long, task: () -> Unit): BukkitTask =
    server.scheduler.runTaskLater(this, Runnable(task), delayTicks)

/** task に自分自身の BukkitTask が渡されるので、その中で cancel() できる */
fun Plugin.runTimer(delayTicks: Long, periodTicks: Long, task: (BukkitTask) -> Unit) {
    server.scheduler.runTaskTimer(this, Consumer<BukkitTask> { task(it) }, delayTicks, periodTicks)
}

/**
 * 非同期処理の結果をメインスレッドで受け取る。
 * 失敗したときはログに出して onError を呼ぶ。プラグイン停止中は何もしない。
 */
fun <T> CompletableFuture<T>.thenOnMain(
    plugin: Plugin,
    onError: (Throwable) -> Unit = {},
    onSuccess: (T) -> Unit,
) {
    whenComplete { value, error ->
        if (!plugin.isEnabled) return@whenComplete
        plugin.runNextTick {
            if (error != null) {
                plugin.logger.log(Level.SEVERE, "非同期処理に失敗しました", error)
                onError(error)
            } else {
                onSuccess(value)
            }
        }
    }
}
