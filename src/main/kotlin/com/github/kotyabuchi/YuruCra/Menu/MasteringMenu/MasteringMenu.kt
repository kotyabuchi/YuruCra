package com.github.kotyabuchi.YuruCra.Menu.MasteringMenu

import com.github.kotyabuchi.YuruCra.Mastering.MasteringType
import com.github.kotyabuchi.YuruCra.Menu.FrameType
import com.github.kotyabuchi.YuruCra.Menu.Menu
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import kotlin.math.ceil

class MasteringMenu(val player: Player): Menu(
    title = Component.text("Mastering"),
    menuRow = ceil(MasteringType.values().size / 7.0).toInt(),
    frames =  setOf(FrameType.TOP, FrameType.SIDE)
) {

    init {
        createMenu()
    }

    override fun createMenu() {
        MasteringType.values().forEach {
            addButton(MBMasteringInfo(it, player))
        }
    }
}