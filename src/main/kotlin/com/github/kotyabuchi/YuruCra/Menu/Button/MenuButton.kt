package com.github.kotyabuchi.YuruCra.Menu.Button

import com.github.kotyabuchi.YuruCra.Menu.ButtonClickInfo
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

abstract class MenuButton {
    abstract val material: Material
    open val displayName: Component? = null
    open val amount: Int = 1
    open val lore: List<Component> = listOf()
    open val modelData: Int? = null
    val menuItem: ItemStack by lazy {
        ItemStack(material, amount).apply {
            editMeta { meta ->
                if (displayName != null) meta.displayName(displayName)
                meta.lore(this@MenuButton.lore)
                meta.setCustomModelData(modelData)
            }
        }
    }

    open val clickSound: ClickSound? = ClickSound(Sound.UI_BUTTON_CLICK, 1f, 1f)

    open fun doShiftClickAction(info: ButtonClickInfo) {}
    open fun doLeftClickAction(info: ButtonClickInfo) {}
    open fun doRightClickAction(info: ButtonClickInfo) {}
}