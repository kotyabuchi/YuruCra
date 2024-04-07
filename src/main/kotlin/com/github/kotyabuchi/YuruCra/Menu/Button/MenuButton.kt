package com.github.kotyabuchi.YuruCra.Menu.Button

import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

abstract class MenuButton {
    abstract val material: Material
    open val displayName: TextComponent? = null
    open val amount: Int = 1
    open val lore: List<TextComponent> = listOf()
    open val modelData: Int? = null
    val menuItem: ItemStack by lazy {
        ItemStack(material, amount).apply {
            editMeta { meta ->
                displayName?.let {
                    meta.displayName(it.decoration(TextDecoration.ITALIC, false))
                }
                meta.lore(
                    this@MenuButton.lore.map {
                        it.decoration(TextDecoration.ITALIC, false)
                    }
                )
                meta.setCustomModelData(modelData)
                ItemFlag.values().forEach {
                    meta.addItemFlags(it)
                }
            }
        }
    }

    open val clickSound: ClickSound? = ClickSound(Sound.UI_BUTTON_CLICK, 1f, 1f)

    open fun doShiftClickAction(info: ButtonClickInfo) {}
    open fun doLeftClickAction(info: ButtonClickInfo) {}
    open fun doRightClickAction(info: ButtonClickInfo) {}
}