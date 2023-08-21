package com.github.kotyabuchi.MCRPG

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor

fun TextComponent.append(text: String, color: TextColor? = null): TextComponent {
    return this.append(Component.text(text, color))
}