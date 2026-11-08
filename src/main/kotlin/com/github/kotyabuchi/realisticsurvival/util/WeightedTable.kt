package com.github.kotyabuchi.realisticsurvival.util

import kotlin.random.Random

/** 重み付きの抽選表 */
class WeightedTable<T>(vararg entries: Pair<T, Int>) {
    private val entries = entries.filter { it.second > 0 }
    private val total = this.entries.sumOf { it.second }

    fun pick(random: Random = Random): T? {
        if (total <= 0) return null
        var roll = random.nextInt(total)
        for ((value, weight) in entries) {
            roll -= weight
            if (roll < 0) return value
        }
        return null
    }
}
