package com.github.kotyabuchi.YuruCra

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

fun Boolean.toInt(): Int {
    return if (this) 1 else 0
}

fun String.toComponent(color: NamedTextColor = NamedTextColor.WHITE): TextComponent {
    return Component.text(this, color).decoration(TextDecoration.ITALIC, false)
}