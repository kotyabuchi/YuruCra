package com.github.kotyabuchi.YuruCra.Item

import com.github.kotyabuchi.MCRPG.normalize
import com.github.kotyabuchi.YuruCra.Main
import com.github.kotyabuchi.YuruCra.PersistantDataType.PersistentDataTypeUUID
import com.github.kotyabuchi.YuruCra.Utility.getEquipmentType
import com.github.kotyabuchi.YuruCra.Utility.hasDurability
import com.github.kotyabuchi.YuruCra.Utility.isArmors
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.logging.Level
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class ItemExtension(_itemStack: ItemStack) {

    private val main: Main = Main.instance

    val itemStack: ItemStack = _itemStack
    var uuid: UUID? = null
        private set
    val equipmentType: EquipmentType? = itemStack.type.getEquipmentType()
    val repairCost: Int = equipmentType?.materialCost ?: 0
    var hasDurability: Boolean = false
        private set
    var baseMaxDurability: Int = 0
        private set
    var maxDurability: Int = 0
        private set
    var durability: Int = 0
        private set

    private val uuidKey = NamespacedKey(main, "UUID")
    private val baseMaxDurabilityKey = NamespacedKey(main, "BaseMaxDurability")
    private val maxDurabilityKey = NamespacedKey(main, "MaxDurability")
    private val durabilityKey = NamespacedKey(main, "Durability")

    init {
        itemStack.itemMeta?.let { meta ->
            val pdc = meta.persistentDataContainer
            if (itemStack.maxStackSize == 1) {
                uuid = pdc.get(uuidKey, PersistentDataTypeUUID) ?: run {
                    val newUUID = UUID.randomUUID()
                    pdc.set(uuidKey, PersistentDataTypeUUID, newUUID)
                    newUUID
                }
            }
            if (meta is Damageable) {
                hasDurability = true
                baseMaxDurability = pdc.get(baseMaxDurabilityKey, PersistentDataType.INTEGER) ?: run {
                    val materialDurability = itemStack.type.maxDurability.toInt()
                    pdc.set(baseMaxDurabilityKey, PersistentDataType.INTEGER, materialDurability)
                    materialDurability
                }
                maxDurability = pdc.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, itemStack.type.maxDurability.toInt())
                durability = pdc.getOrDefault(durabilityKey, PersistentDataType.INTEGER, min(maxDurability - meta.damage, maxDurability))
            }
        }
    }

    fun maxDurability(amount: Int): ItemExtension {
        maxDurability = amount
        return this
    }

    fun durability(amount: Int): ItemExtension {
        durability = min(maxDurability, amount)
        return this
    }

    fun damage(amount: Int): ItemExtension {
        durability(max(0, durability - amount))
        return this
    }

    fun damage(amount: Int, player: Player): ItemExtension {
        if (hasDurability) {
            damage(amount)
            notifyDurability(player)
        } else {
            val stackTrace = Throwable().stackTraceToString()
            Bukkit.getLogger().log(
                Level.INFO, """
                ItemExtension Damage call not damageable
                ItemStack: $itemStack
                $stackTrace
                """.trimIndent()
            )
        }
        return this
    }

    fun mending(amount: Int): ItemExtension {
        durability(min(maxDurability, durability + amount))
        return this
    }

    fun mending(amount: Int, player: Player): ItemExtension {
        if (hasDurability) {
            mending(amount)
            notifyDurability(player)
        } else {
            val stackTrace = Throwable().stackTraceToString()
            Bukkit.getLogger().log(
                Level.INFO, """   
                ItemExtension Mending call not damageable
                ItemStack: $itemStack
                $stackTrace
                """.trimIndent()
            )
        }
        return this
    }

    fun applyUnbreaking(_level: Int? = null): ItemExtension {
        val material = itemStack.type
        if (!material.hasDurability()) return this
        val level = _level ?: itemStack.getEnchantmentLevel(Enchantment.DURABILITY)
        val durabilityRatio = durability / maxDurability.toDouble()
        if (level > 0) {
            val multiple = if (material.isArmors()) {
                1.25 + (.09 * level)
            } else {
                1.0 + level
            }
            maxDurability(round(baseMaxDurability * multiple).toInt())
        } else {
            maxDurability(baseMaxDurability)
        }
        durability(round(maxDurability * durabilityRatio).toInt())
        return this
    }

    fun applyDurability(): ItemExtension {
        if (hasDurability) {
            itemStack.editMeta { meta ->
                val percent = durability.toDouble() / maxDurability * 100
                meta as Damageable
                val materialDurability = itemStack.type.maxDurability
                val ratio = materialDurability / 100.0
                meta.damage = materialDurability - min(materialDurability.toInt(), round(ratio * percent).toInt())
                val pdc = meta.persistentDataContainer
                pdc.set(maxDurabilityKey, PersistentDataType.INTEGER, maxDurability)
                pdc.set(durabilityKey, PersistentDataType.INTEGER, durability)
            }
        }
        return this
    }

    fun applySetting(): ItemExtension {
        val lore = mutableListOf<Component>()
        if (hasDurability) {
            lore.add(Component.empty())
            lore.add(Component.text("Durability: ", NamedTextColor.GRAY).normalize().append(Component.text("$durability / $maxDurability", durabilityColor()).normalize()))
        }
        itemStack.editMeta {
            it.lore(lore)
        }
        return this
    }

    private fun durabilityColor(): NamedTextColor {
        val percent = durability / maxDurability.toDouble()
        return when {
            percent > .7 -> NamedTextColor.GREEN
            percent > .3 -> NamedTextColor.GOLD
            percent > .1 -> NamedTextColor.RED
            else -> NamedTextColor.DARK_RED
        }
    }

    fun notifyDurability(player: Player) {
        player.sendActionBar(Component.text("$durability / $maxDurability", durabilityColor()).normalize())
    }
}