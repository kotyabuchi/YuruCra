package com.github.kotyabuchi.realisticsurvival.menu

import com.github.kotyabuchi.realisticsurvival.RealisticSurvival
import com.github.kotyabuchi.realisticsurvival.util.buttonItem
import com.github.kotyabuchi.realisticsurvival.util.text
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil

/** メニューの1ページ。インベントリの持ち主にすることで、開いているのがどのメニューか判別する */
class MenuPage(val menu: Menu, val index: Int) : InventoryHolder {
    private lateinit var inv: Inventory

    fun attach(inventory: Inventory) {
        inv = inventory
    }

    override fun getInventory(): Inventory = inv
}

/** メニューのボタン。左右クリックの処理をサブクラスで書く */
open class MenuButton(
    val icon: ItemStack,
    val clickSound: Sound? = Sound.UI_BUTTON_CLICK,
) {
    open fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {}
    open fun onRightClick(player: Player, event: InventoryClickEvent, menu: Menu) {}
}

/**
 * チェスト型のメニュー。中身は build() で並べる（最初に開いたとき・refresh() 時に呼ばれる）。
 *
 * レイアウト: 上段の枠（TOP）・左右の枠（SIDE）・最下段のフッターを除いた部分にボタンを詰めて並べる。
 * 入りきらない分は put(button, page = 1) のように別ページに置くと、ページ送りボタンが自動で付く。
 */
abstract class Menu(
    val plugin: RealisticSurvival,
    val title: Component,
    contentAmount: Int,
    vararg frames: Frame,
) {
    enum class Frame { TOP, SIDE }

    private val hasTop = Frame.TOP in frames
    private val hasSide = Frame.SIDE in frames
    private val rows: Int = run {
        val width = if (hasSide) 7.0 else 9.0
        val maxRows = if (hasTop) 4 else 5
        ceil(contentAmount / width).toInt().coerceIn(0, maxRows)
    }
    val size: Int = (rows + if (hasTop) 2 else 1) * 9

    private val pages = mutableListOf<MenuPage>()
    private val buttons = mutableListOf<MutableMap<Int, MenuButton>>()
    private var built = false

    /** 「戻る」ボタンの戻り先 */
    var prevMenu: Menu? = null
        private set

    protected abstract fun build()

    fun inventory(page: Int = 0): Inventory {
        if (!built) {
            built = true
            ensurePage(0)
            build()
            placePageButtons()
        }
        ensurePage(page)
        return pages[page].inventory
    }

    /** 中身を作り直す（次に inventory() を呼んだときに build() される） */
    fun refresh() {
        pages.clear()
        buttons.clear()
        built = false
    }

    fun setPrevMenu(menu: Menu?) {
        prevMenu = menu
        pages.indices.forEach { placeBackButton(it) }
    }

    fun buttonAt(slot: Int, page: Int): MenuButton? = buttons.getOrNull(page)?.get(slot)

    /** slot を省略すると、そのページの空いている最初のスロットに置く */
    fun put(button: MenuButton, page: Int = 0, slot: Int? = null) {
        ensurePage(page)
        val target = slot ?: firstEmptySlot(page) ?: return
        buttons[page][target] = button
        pages[page].inventory.setItem(target, button.icon)
    }

    fun put(button: MenuButton, page: Int, slots: IntRange) {
        slots.forEach { put(button, page, it) }
    }

    fun firstEmptySlot(page: Int): Int? {
        ensurePage(page)
        val inventory = pages[page].inventory
        return (0 until size).firstOrNull { inventory.getItem(it)?.isEmpty ?: true }
    }

    val pageCount: Int get() = pages.size

    open fun onButtonClick(player: Player, button: MenuButton, event: InventoryClickEvent, page: Int) {
        if (event.isLeftClick) button.onLeftClick(player, event, this)
        if (event.isRightClick) button.onRightClick(player, event, this)
        button.clickSound?.let { player.playSound(player.eyeLocation, it, 0.5f, 1f) }
    }

    /** メニューを開いたまま、自分のインベントリ側をクリックしたとき（クリック自体はキャンセル済み） */
    open fun onPlayerInventoryClick(player: Player, event: InventoryClickEvent, page: Int) {}

    protected fun ensurePage(page: Int) {
        while (pages.size <= page) addPage()
    }

    private fun addPage() {
        val index = pages.size
        val page = MenuPage(this, index)
        page.attach(Bukkit.createInventory(page, size, title))
        pages += page
        buttons += HashMap()
        put(BlankButton(), index, (size - 9) until size)
        if (hasTop) put(BlankButton(), index, 0..8)
        if (hasSide) {
            repeat(size / 9) { row ->
                put(BlankButton(), index, row * 9)
                put(BlankButton(), index, row * 9 + 8)
            }
        }
        placeBackButton(index)
    }

    private fun placeBackButton(page: Int) {
        prevMenu?.let { put(BackToPrevButton(it), page, size - 8) }
    }

    private fun placePageButtons() {
        val total = pages.size
        for (page in 0 until total) {
            if (page > 0) put(PageButton(page - 1, total, isNext = false), page, size - 7)
            if (page < total - 1) put(PageButton(page + 1, total, isNext = true), page, size - 3)
        }
    }
}

/** 枠や空きスロットを埋める何もしないボタン */
class BlankButton(material: Material = Material.BLACK_STAINED_GLASS_PANE) :
    MenuButton(buttonItem(material, Component.empty()), clickSound = null)

class BackToPrevButton(private val prev: Menu) :
    MenuButton(buttonItem(Material.ARROW, text("Back to ").append(prev.title))) {
    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        prev.refresh()
        menu.plugin.menus.open(player, prev, asBack = true)
    }
}

class PageButton(private val targetPage: Int, totalPages: Int, isNext: Boolean) :
    MenuButton(buttonItem(Material.ARROW, text((if (isNext) "Next" else "Back") + " page ${targetPage + 1} / $totalPages"))) {
    override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) {
        menu.plugin.menus.open(player, menu, targetPage, asBack = true)
    }
}
