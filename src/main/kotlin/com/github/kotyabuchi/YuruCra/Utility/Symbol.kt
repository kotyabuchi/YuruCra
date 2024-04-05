package com.github.kotyabuchi.YuruCra.Utility

enum class Symbol(val decimalValue: Int) {
    I(1),
    IV(4),
    V(5),
    IX(9),
    X(10),
    XL(40),
    L(50),
    XC(90),
    C(100),
    CD(400),
    D(500),
    CM(900),
    M(1000);

    companion object {
        fun closestBelow(value: Int) =
            values()
                .sortedByDescending { it.decimalValue }
                .firstOrNull { value >= it.decimalValue }

        fun highestStartingSymbol(value: String) =
            values()
                .sortedByDescending { it.decimalValue }
                .firstOrNull { value.startsWith(it.name) }
    }
}