package com.github.kotyabuchi.yurucra.item

import com.github.kotyabuchi.yurucra.util.ItemModel
import com.github.kotyabuchi.yurucra.util.loreLine
import com.github.kotyabuchi.yurucra.util.runNextTick
import com.github.kotyabuchi.yurucra.util.text
import com.github.kotyabuchi.yurucra.util.upperCamelCase
import com.github.kotyabuchi.yurucra.util.withModel
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareGrindstoneEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerItemMendEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import kotlin.math.roundToInt

/**
 * 道具・防具の耐久値。
 *
 * - 耐久力エンチャントは「確率で減らない」の代わりに、最大耐久値そのものを増やす（道具は 1+レベル 倍、防具は 1.25+0.09×レベル 倍）
 * - 深層岩の丸石だけで作った石の道具は、最大耐久値が1.5倍になり、名前が「深層岩のツルハシ」などになり、見た目も深層岩の色になる
 * - 説明文に「残り / 最大」を出し、道具が減るたびに画面下にも出す（防具は画面下には出さない）
 * - 1回で大きく減って壊れそうなとき（残り2以上）は、残り1で止める
 *
 * 最大耐久値はアイテムの max_damage を直接変えるので、減り方・修理・修繕・壊れる処理はバニラのまま動く。
 * 最大耐久値は作ったとき・エンチャントや金床などで変わったとき・初めて減ったときに合わせ直す（拾った物や交易品も、使えば合う）。
 */
class ItemDurability(private val plugin: Plugin) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDamage(event: PlayerItemDamageEvent) {
        // バニラから渡されるのは実物のアイテムなので、書き換えるとそのまま反映される
        val item = event.item
        if (!isTarget(item)) return
        sync(item)
        val max = item.getData(DataComponentTypes.MAX_DAMAGE) ?: return
        val remaining = max - (item.getData(DataComponentTypes.DAMAGE) ?: 0)
        // 耐久力の「確率で減らない」効果は使わない（最大値を増やしているため）
        var damage = event.originalDamage
        if (damage > 1 && remaining > 1 && damage >= remaining) damage = remaining - 1
        event.damage = damage
        val after = (remaining - damage).coerceAtLeast(0)
        updateLore(item, after, max)
        if (item.type.equipmentSlot.isArmor) return
        event.player.sendActionBar(text("$after / $max", durabilityColor(after, max)))
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onMend(event: PlayerItemMendEvent) {
        val item = event.item
        if (!isTarget(item)) return
        sync(item)
        val max = item.getData(DataComponentTypes.MAX_DAMAGE) ?: return
        val damage = item.getData(DataComponentTypes.DAMAGE) ?: 0
        updateLore(item, max - damage + minOf(event.repairAmount, damage), max)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCraft(event: PrepareItemCraftEvent) {
        val inventory = event.inventory
        val result = inventory.result ?: return
        if (!isTarget(result)) return
        if (!event.isRepair && isAllDeepslateStoneTool(result.type, inventory.matrix)) {
            result.editPersistentDataContainer { it.set(reinforcedKey, PersistentDataType.BOOLEAN, true) }
            result.setData(DataComponentTypes.ITEM_NAME, deepslateToolName(result.type))
            DEEPSLATE_TOOL_MODELS[result.type]?.let { result.withModel(it) }
        }
        sync(result)
        inventory.result = result
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onAnvil(event: PrepareAnvilEvent) {
        event.result = event.result?.let(::synced)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSmithing(event: PrepareSmithingEvent) {
        event.result = event.result?.let(::synced)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onGrindstone(event: PrepareGrindstoneEvent) {
        event.result = event.result?.let(::synced)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEnchant(event: EnchantItemEvent) {
        // エンチャントはこのイベントのあとに付くので、次のtickに台の上のアイテムを合わせる
        val inventory = event.inventory
        plugin.runNextTick {
            val item = inventory.getItem(0) ?: return@runNextTick
            if (isTarget(item)) {
                sync(item)
                inventory.setItem(0, item)
            }
        }
    }

    /** 最大耐久値と説明文を合わせ直す（耐久値のない物はそのまま）。耐久値を直接書き換えたあとに呼ぶ */
    fun synced(item: ItemStack): ItemStack {
        if (isTarget(item)) sync(item)
        return item
    }

    /** 最大耐久値をエンチャントなどに合わせ直し（残りの割合は保つ）、説明文を更新する */
    private fun sync(item: ItemStack) {
        val base = item.type.maxDurability.toInt()
        val expected = expectedMax(item, base)
        val current = item.getData(DataComponentTypes.MAX_DAMAGE) ?: base
        var damage = item.getData(DataComponentTypes.DAMAGE) ?: 0
        if (current != expected) {
            val ratio = (current - damage).toDouble() / current
            damage = (expected - (expected * ratio).roundToInt()).coerceIn(0, expected - 1)
            if (expected == base) item.resetData(DataComponentTypes.MAX_DAMAGE) else item.setData(DataComponentTypes.MAX_DAMAGE, expected)
            item.setData(DataComponentTypes.DAMAGE, damage)
        }
        updateLore(item, expected - damage, expected)
    }

    private fun expectedMax(item: ItemStack, base: Int): Int {
        var multiplier = 1.0
        if (item.persistentDataContainer.has(reinforcedKey)) multiplier *= REINFORCED_MULTIPLIER
        val level = item.getEnchantmentLevel(Enchantment.UNBREAKING)
        if (level > 0) multiplier *= if (item.type.equipmentSlot.isArmor) 1.25 + 0.09 * level else 1.0 + level
        return (base * multiplier).roundToInt().coerceAtLeast(1)
    }

    /** 説明文の最後にある耐久値の行を書き換える（ほかの行はそのまま） */
    private fun updateLore(item: ItemStack, remaining: Int, max: Int) {
        val lore = item.lore()?.toMutableList() ?: mutableListOf()
        if (lore.isNotEmpty() && isDurabilityLine(lore.last())) {
            lore.removeLast()
            if (lore.isNotEmpty() && plain(lore.last()).isEmpty()) lore.removeLast()
        }
        lore += Component.empty()
        lore += loreLine(DURABILITY_LABEL, "$remaining / $max", durabilityColor(remaining, max))
        item.lore(lore)
    }

    private fun isDurabilityLine(line: Component): Boolean = DURABILITY_LINE.matches(plain(line))

    private fun plain(component: Component): String = PlainTextComponentSerializer.plainText().serialize(component)

    private fun isAllDeepslateStoneTool(type: Material, matrix: Array<out ItemStack?>): Boolean {
        if (type !in DEEPSLATE_TOOL_MODELS) return false
        val materials = matrix.filterNotNull().filterNot { it.isEmpty || it.type == Material.STICK }
        return materials.isNotEmpty() && materials.all { it.type == Material.COBBLED_DEEPSLATE }
    }

    /**
     * 「深層岩のツルハシ」など。翻訳はリソースパック（en_us・ja_jp）にあり、ほかの言語では英語になる。
     * リソースパックがないときも英語で出るよう、英語の名前を予備に持たせる
     */
    private fun deepslateToolName(type: Material): Component {
        val tool = type.name.removePrefix("STONE_")
        return Component.translatable("item.yurucra.deepslate_${tool.lowercase()}", "Deepslate ${tool.upperCamelCase()}")
    }

    private fun durabilityColor(remaining: Int, max: Int): NamedTextColor {
        val percent = remaining.toDouble() / max
        return when {
            percent > 0.7 -> NamedTextColor.GREEN
            percent > 0.3 -> NamedTextColor.GOLD
            percent > 0.1 -> NamedTextColor.RED
            else -> NamedTextColor.DARK_RED
        }
    }

    companion object {
        private const val DURABILITY_LABEL = "Durability: "
        private val DURABILITY_LINE = Regex("""^Durability: \d+ / \d+$""")
        private const val REINFORCED_MULTIPLIER = 1.5

        /** 深層岩の丸石だけで作った石の道具の印 */
        private val reinforcedKey = NamespacedKey("yurucra", "reinforced")

        /** 深層岩で作れる石の道具 → 見た目 */
        private val DEEPSLATE_TOOL_MODELS = mapOf(
            Material.STONE_PICKAXE to ItemModel.DEEPSLATE_PICKAXE,
            Material.STONE_AXE to ItemModel.DEEPSLATE_AXE,
            Material.STONE_SHOVEL to ItemModel.DEEPSLATE_SHOVEL,
            Material.STONE_HOE to ItemModel.DEEPSLATE_HOE,
            Material.STONE_SWORD to ItemModel.DEEPSLATE_SWORD,
        )

        private fun isTarget(item: ItemStack): Boolean =
            !item.isEmpty && item.type.maxDurability > 0 && !item.hasData(DataComponentTypes.UNBREAKABLE)
    }
}
