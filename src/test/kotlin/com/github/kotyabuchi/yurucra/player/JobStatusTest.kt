package com.github.kotyabuchi.yurucra.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JobStatusTest {

    @Test
    fun `必要経験値は旧RealisticSurvivalと同じ式で計算される`() {
        // round((lv^2 + 10lv) * (1 + lv/500))
        assertEquals(11, JobStatus.requiredExp(1))
        assertEquals(24, JobStatus.requiredExp(2))
        assertEquals(204, JobStatus.requiredExp(10))
        assertEquals(3300, JobStatus.requiredExp(50))
    }

    @Test
    fun `累計経験値からレベルとレベル内の経験値を戻せる`() {
        val status = JobStatus()
        status.setTotalExp(11.0 + 24.0 + 5.0)
        assertEquals(3, status.level)
        assertEquals(5.0, status.exp, 1e-9)
        assertEquals(40.0, status.totalExp, 1e-9)
    }

    @Test
    fun `負の累計経験値はレベル1の0として扱う`() {
        val status = JobStatus()
        status.setTotalExp(-10.0)
        assertEquals(1, status.level)
        assertEquals(0.0, status.exp, 1e-9)
    }

    @Test
    fun `経験値を得るとコンボ倍率がかかる`() {
        val status = JobStatus()
        status.addExp(5.0, increaseCombo = 10)
        // 10コンボで x1.02
        assertEquals(5.1, status.recentAddedExp, 1e-9)
        assertEquals(5.1, status.totalExp, 1e-9)
    }

    @Test
    fun `一度に複数レベル上がる`() {
        val status = JobStatus()
        val leveledUp = status.addExp(11.0 + 24.0 + 1.0, increaseCombo = 0)
        assertTrue(leveledUp)
        assertEquals(3, status.level)
        assertEquals(1.0, status.exp, 1e-9)
    }

    @Test
    fun `レベルが上がらなければ false`() {
        val status = JobStatus()
        assertFalse(status.addExp(1.0, increaseCombo = 0))
        assertEquals(1, status.level)
    }

    @Test
    fun `レベルを直接変えると累計経験値も合わせて変わる`() {
        val status = JobStatus()
        status.addExp(3.0, increaseCombo = 0)
        status.setLevel(3)
        assertEquals(3, status.level)
        assertEquals(11.0 + 24.0 + 3.0, status.totalExp, 1e-9)

        // 保存して読み直しても同じ状態に戻る
        val reloaded = JobStatus().apply { setTotalExp(status.totalExp) }
        assertEquals(status.level, reloaded.level)
        assertEquals(status.exp, reloaded.exp, 1e-9)
    }

    @Test
    fun `レベル内の経験値を指定するとレベルが上がることもある`() {
        val status = JobStatus()
        status.setExpInLevel(15.0)
        assertEquals(2, status.level)
        assertEquals(4.0, status.exp, 1e-9)
    }

    @Test
    fun `コンボはリセットできる`() {
        val status = JobStatus()
        status.addExp(1.0, increaseCombo = 5)
        status.resetCombo()
        assertEquals(0, status.combo)
        assertEquals(1.0, status.comboMultiplier, 1e-9)
    }
}
