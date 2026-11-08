package com.github.kotyabuchi.realisticsurvival.util

import org.bukkit.Server
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TickScopedMapTest {

    private var tick = 0

    // currentTick だけを返す Server の代役
    private val server = Proxy.newProxyInstance(Server::class.java.classLoader, arrayOf(Server::class.java)) { _, method, _ ->
        if (method.name == "getCurrentTick") tick else throw UnsupportedOperationException(method.name)
    } as Server

    @Test
    fun `同じtickの間は値が残る`() {
        val map = TickScopedMap<String, Int>(server)
        map["a"] = 1
        assertEquals(1, map["a"])
        assertEquals(1, map.remove("a"))
        assertNull(map["a"])
    }

    @Test
    fun `tickが変わると値が消える`() {
        val map = TickScopedMap<String, Int>(server)
        map["a"] = 1
        tick++
        assertNull(map["a"])
    }

    @Test
    fun `tickが変わった後に入れた値は残る`() {
        val map = TickScopedMap<String, Int>(server)
        map["a"] = 1
        tick++
        map["b"] = 2
        assertNull(map["a"])
        assertEquals(2, map["b"])
    }
}
