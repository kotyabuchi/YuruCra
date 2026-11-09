package com.github.kotyabuchi.yurucra.player

import kotlin.math.round

/** 1つのジョブのレベルと経験値。DBには累計経験値(totalExp)だけを保存し、レベルは計算で戻す */
class JobStatus {
    var level: Int = 1
        private set
    /** 現在のレベル内での経験値 */
    var exp: Double = 0.0
        private set
    var totalExp: Double = 0.0
        private set
    /** 連続で経験値を得た回数。経験値に倍率がかかり、しばらく得ないとリセットされる */
    var combo: Int = 0
        private set
    var recentAddedExp: Double = 0.0
        private set

    val nextLevelExp: Int get() = requiredExp(level)

    val comboMultiplier: Double get() = 1 + combo * COMBO_BONUS

    fun setTotalExp(value: Double) {
        val total = value.coerceAtLeast(0.0)
        var remaining = total
        var newLevel = 1
        while (remaining >= requiredExp(newLevel)) {
            remaining -= requiredExp(newLevel)
            newLevel++
        }
        totalExp = total
        level = newLevel
        exp = remaining
    }

    /** レベル内の経験値を指定値にする（レベルが上がる場合もある） */
    fun setExpInLevel(value: Double) = setTotalExp(totalExp + (value - exp))

    /** レベルを直接変える。レベル内の経験値はそのまま */
    fun setLevel(value: Int) {
        level = value.coerceAtLeast(1)
        totalExp = (1 until level).sumOf { requiredExp(it).toDouble() } + exp
    }

    fun addLevel(amount: Int) = setLevel(level + amount)

    /** @return レベルが上がったら true */
    fun addExp(point: Double, increaseCombo: Int = 1): Boolean {
        combo += increaseCombo
        val gained = point * comboMultiplier
        recentAddedExp = gained
        exp += gained
        totalExp += gained
        var leveledUp = false
        while (exp >= nextLevelExp) {
            exp -= nextLevelExp
            level++
            leveledUp = true
        }
        return leveledUp
    }

    fun resetCombo() {
        combo = 0
    }

    companion object {
        private const val COMBO_BONUS = 0.002

        /** そのレベルから次のレベルに上がるのに必要な経験値 */
        fun requiredExp(level: Int): Int =
            round((level.toDouble() * level + 10.0 * level) * (1 + level / 500.0)).toInt()
    }
}
