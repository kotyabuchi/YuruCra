package com.github.kotyabuchi.yurucra.menu

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.util.runNextTick
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.time.Duration

/** Paper の Dialog API で、文字を入力してもらう（旧RealisticSurvivalの AnvilGUI の代わり） */
object TextPrompt {

    private const val INPUT_KEY = "value"
    private const val MAX_LENGTH = 32

    fun open(
        plugin: YuruCra,
        player: Player,
        title: String,
        label: String,
        initial: String = "",
        onSubmit: (String) -> Unit,
        onCancel: () -> Unit,
    ) {
        val options = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(10)).build()
        val submit = DialogAction.customClick({ view, _ ->
            val value = view.getText(INPUT_KEY)?.trim().orEmpty()
            // コールバックがどのスレッドで呼ばれても安全なように、メインスレッドに戻す
            plugin.runNextTick {
                if (!player.isOnline) return@runNextTick
                if (value.isEmpty()) onCancel() else onSubmit(value)
            }
        }, options)
        val cancel = DialogAction.customClick({ _, _ -> plugin.runNextTick { if (player.isOnline) onCancel() } }, options)

        val dialog = Dialog.create { builder ->
            builder.empty()
                .base(
                    DialogBase.builder(Component.text(title))
                        .inputs(
                            listOf(
                                DialogInput.text(INPUT_KEY, Component.text(label))
                                    .initial(initial)
                                    .maxLength(MAX_LENGTH)
                                    .build()
                            )
                        )
                        .build()
                )
                .type(
                    DialogType.confirmation(
                        ActionButton.create(Component.text("決定", NamedTextColor.GREEN), null, 100, submit),
                        ActionButton.create(Component.text("キャンセル", NamedTextColor.RED), null, 100, cancel),
                    )
                )
        }
        player.showDialog(dialog)
    }
}
