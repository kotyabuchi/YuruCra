package com.github.kotyabuchi.YuruCra

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class RandomTable<T> {
    private val items = mutableListOf<T>()

    fun addItem(item: T, probability: Double, asPossible: Boolean = true): Boolean {
        val itemAmount = min(floor(max(probability, 0.0) * 10).toInt(), 1000)
        if (!asPossible && 1000 < itemAmount + items.size) return false
        repeat(min(itemAmount, 1000 - items.size)) {
            items.add(item)
        }
        return true
    }

    fun fillRemaining(item: T) {
        repeat(1000 - items.size) {
            items.add(item)
        }
    }

    fun getRandomItem(): T {
        return items.random()
    }

    fun remainingAmount(): Double {
        return (1000 - items.size) / 10.0
    }
}