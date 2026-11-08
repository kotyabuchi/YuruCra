package com.github.kotyabuchi.realisticsurvival.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.entity.Player
import kotlin.math.floor
import kotlin.math.pow

/** アイテム名・Loreで使う、斜体なしのテキスト */
fun text(content: String, color: TextColor = NamedTextColor.WHITE): Component =
    Component.text(content, color).decoration(TextDecoration.ITALIC, false)

/** Loreの項目名に使うグレーのスタイル */
val LORE_LABEL: Style = Style.style(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)

/** 「項目名: 値」形式のLore行 */
fun loreLine(label: String, value: String, valueColor: TextColor = NamedTextColor.WHITE): Component =
    Component.text(label, LORE_LABEL).append(text(value, valueColor))

/** MINE_ASSIST → "Mine Assist" */
fun String.upperCamelCase(): String =
    split("_").filter { it.isNotEmpty() }.joinToString(" ") { part ->
        part.lowercase().replaceFirstChar { it.uppercase() }
    }

fun Double.floorDigits(digits: Int): Double {
    val factor = 10.0.pow(digits)
    return floor(this * factor) / factor
}

fun Player.sendSuccess(message: String) = sendMessage(text(message, NamedTextColor.GREEN))

fun Player.sendError(message: String) = sendMessage(text(message, NamedTextColor.RED))

fun Player.playErrorSound() = playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f)
