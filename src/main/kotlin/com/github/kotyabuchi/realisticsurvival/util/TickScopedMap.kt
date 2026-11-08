package com.github.kotyabuchi.realisticsurvival.util

import org.bukkit.Server

/**
 * 同じtickの間だけ値を覚えておく Map。tick が変わってから最初に触ったときに中身を捨てる。
 *
 * 「BlockBreakEvent で印を付けて、同じ破壊処理の中の BlockDropItemEvent で使う」ような用途向け。
 * 印ごとに後始末のタスクを予約しなくて済むので、大量にブロックが壊れても負荷が増えない。
 * メインスレッドからだけ使うこと。
 */
class TickScopedMap<K : Any, V : Any>(private val server: Server) {

    private val map = HashMap<K, V>()
    private var tick = Int.MIN_VALUE

    operator fun get(key: K): V? = current()[key]

    operator fun set(key: K, value: V) {
        current()[key] = value
    }

    fun remove(key: K): V? = current().remove(key)

    private fun current(): HashMap<K, V> {
        val now = server.currentTick
        if (now != tick) {
            map.clear()
            tick = now
        }
        return map
    }
}
