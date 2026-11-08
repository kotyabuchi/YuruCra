package com.github.kotyabuchi.realisticsurvival.util

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeightedTableTest {

    @Test
    fun `空の表からは何も出ない`() {
        assertNull(WeightedTable<String>().pick())
    }

    @Test
    fun `重み0の項目は出ない`() {
        val table = WeightedTable("a" to 0, "b" to 1)
        repeat(100) { assertEquals("b", table.pick()) }
    }

    @Test
    fun `重みに比例して出る`() {
        val table = WeightedTable("common" to 9, "rare" to 1)
        val random = Random(42)
        val rare = (1..10_000).count { table.pick(random) == "rare" }
        // 期待値1,000回。乱数のぶれを見込んで広めに取る
        assertTrue(rare in 800..1200, "rare = $rare")
    }
}
