package com.github.kotyabuchi.realisticsurvival.player

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.math.min

/**
 * 素材専用の倉庫。1スロットに1種類の素材を個数無制限で入れられる。
 * 名前やエンチャントの付いたアイテムは入れられない。
 */
class ResourceStorage(slotSize: Int = DEFAULT_SLOTS) {

    var slotSize: Int = slotSize.coerceIn(0, MAX_SLOTS)
    var autoStore: Boolean = true

    private val resources = LinkedHashMap<Material, Int>()
    val stored: Map<Material, Int> get() = resources

    fun amountOf(material: Material): Int = resources[material] ?: 0

    /**
     * 既に入っている素材なら true（自動収納の判定に使う）。
     * アイテムを拾おうとするたび（ほぼ毎tick）に呼ばれるので、軽い判定を先にする。
     */
    fun accepts(item: ItemStack): Boolean = item.type in resources && isPlain(item)

    /** @return 実際にしまった個数 */
    fun store(item: ItemStack): Int {
        if (!isPlain(item)) return 0
        val material = item.type
        val current = resources[material]
        return when {
            current != null -> {
                val amount = min(Int.MAX_VALUE - current, item.amount)
                resources[material] = current + amount
                amount
            }
            resources.size < slotSize -> {
                resources[material] = item.amount
                item.amount
            }
            else -> 0
        }
    }

    /** 取り出す。0個になったスロットは空く */
    fun take(material: Material, amount: Int): ItemStack? {
        val current = resources[material] ?: return null
        val taken = min(amount, current)
        val left = current - taken
        if (left == 0) resources.remove(material) else resources[material] = left
        return ItemStack(material, taken)
    }

    /** DBからの読み込み用（スロット数の制限をかけない） */
    fun restore(material: Material, amount: Int) {
        resources[material] = amount
    }

    private fun isPlain(item: ItemStack): Boolean = !item.type.isAir && ItemStack(item.type).isSimilar(item)

    companion object {
        const val MAX_SLOTS = 4 * 9
        const val DEFAULT_SLOTS = 2
    }
}
