package com.github.kotyabuchi.YuruCra.Menu

import com.github.kotyabuchi.YuruCra.Menu.Button.*
import com.github.kotyabuchi.YuruCra.toInt
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import kotlin.math.max
import kotlin.math.min

abstract class Menu(
    val title: Component,
    private var menuRow: Int = 1,
    frames: Set<FrameType> = setOf(),
) {
    private val pages: MutableList<Inventory> = mutableListOf()
    private val hasTopFrame: Boolean = frames.contains(FrameType.TOP)
    private val hasSideFrame: Boolean = frames.contains(FrameType.SIDE)
    private val invRow: Int
    val invSize: Int
    var prevMenu: Menu? = null
        set(value) {
            if (value == null) {
                (0 until pages.size).forEach { page ->
                    setButton(page, invSize - 8, MBBlank())
                }
            } else {
                (0 until pages.size).forEach { page ->
                    setButton(page, invSize - 8, MBPrevMenu(value))
                }
            }
            field = value
        }
    private val buttons = mutableListOf(mutableMapOf<Int, MenuButton>())

    init {
        val maxHeight = 5 - hasTopFrame.toInt()
        menuRow = max(1, min(maxHeight, menuRow))
        invRow = hasTopFrame.toInt() + menuRow + 1
        invSize = invRow * 9

        createNewPage()
    }

    open fun createMenu() {}

    fun createNewPage() {
        val newPage = Bukkit.createInventory(null, (hasTopFrame.toInt() + menuRow + 1) * 9, title)
        pages.add(newPage)
        buttons.add(mutableMapOf())
        createFrame()
        createFooter()
    }

    fun createNewPageIfNeed(pageNum: Int) {
        repeat(max(0, pageNum - pages.size + 1)) {
            createNewPage()
        }
    }

    fun setButton(pageNum: Int, slot: Int, button: MenuButton) {
        createNewPageIfNeed(pageNum)
        buttons[pageNum][slot] = button
        pages[pageNum].setItem(slot, button.menuItem)
    }

    fun getButton(pageNum: Int, slot: Int): MenuButton? {
        createNewPageIfNeed(pageNum)
        return buttons[pageNum][slot]
    }

    open fun createFooter(pageNum: Int = pages.size - 1) {
        val startSlot = invSize - 9
        (0 until 9).forEach {
            setButton(pageNum, startSlot + it, MBBlank())
        }

        val totalPage = pages.size
        if (pageNum > 0) {
            setButton(pageNum, startSlot + 1, MBBackPage(pageNum, totalPage))
        } else if (totalPage > pageNum + 1) {
            setButton(pageNum, invSize - 2, MBNextPage(pageNum + 2, totalPage))
        }
    }

    open fun createFrame(pageNum: Int = pages.size - 1) {
        if (hasTopFrame) {
            (0 until 9).forEach {
                setButton(pageNum, it, MBBlank())
            }
        }
        if (hasSideFrame) {
            (0 until invRow).forEach {
                setButton(pageNum, it * 9, MBBlank())
                setButton(pageNum, it * 9 + 8, MBBlank())
            }
        }
    }

    fun getInventory(page: Int): Inventory {
        if (pages.size <= page) createNewPage()
        return pages[page]
    }

    open fun doCloseMenuAction(event: InventoryCloseEvent) {}

    open fun doItemClickAction(event: InventoryClickEvent) {}
}