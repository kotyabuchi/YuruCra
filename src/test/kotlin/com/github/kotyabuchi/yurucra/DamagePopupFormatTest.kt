package com.github.kotyabuchi.yurucra

import com.github.kotyabuchi.yurucra.feature.DamagePopup
import kotlin.test.Test
import kotlin.test.assertEquals

class DamagePopupFormatTest {

    @Test
    fun `整数は小数点を付けない`() {
        assertEquals("4", DamagePopup.format(4.0))
        assertEquals("20", DamagePopup.format(20.0))
    }

    @Test
    fun `小数第2位で切り捨てて末尾の0を省く`() {
        assertEquals("3.5", DamagePopup.format(3.5))
        assertEquals("1.33", DamagePopup.format(1.3333))
        assertEquals("0.99", DamagePopup.format(0.999))
    }
}
