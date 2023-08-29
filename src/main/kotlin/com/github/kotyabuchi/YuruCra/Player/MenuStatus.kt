package com.github.kotyabuchi.YuruCra.Player

import com.github.kotyabuchi.YuruCra.Menu.Menu

class MenuStatus {
    var openingMenu: Menu? = null
    var openingPage: Int = 0

    fun openMenu(menu: Menu) {

    }

    fun closeMenu() {
        openingMenu = null
        openingPage = 0
    }
}