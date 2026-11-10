package com.github.kotyabuchi.yurucra.item

import com.github.kotyabuchi.yurucra.util.ItemModel
import com.github.kotyabuchi.yurucra.util.runNextTick
import com.github.kotyabuchi.yurucra.util.withModel
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.event.entity.EntityDamageItemEvent
import io.papermc.paper.datacomponent.item.Repairable
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Wolf
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.inventory.recipe.CraftingBookCategory
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.tag.DamageTypeTags
import kotlin.random.Random

/**
 * 材料別の狼の鎧。中身はバニラの狼の鎧で、名前・見た目・耐久値・修理の素材を変える。
 * 狼の鎧は受けたダメージを耐久値で肩代わりするので、硬さは「1回で減る耐久値の割合（wearRate）」で表す。
 */
enum class WolfArmorTier(
    val material: Material,
    val durability: Int,
    val model: ItemModel,
    val wearRate: Double,
    val knockbackResistance: Double = 0.0,
) {
    COPPER(Material.COPPER_INGOT, 96, ItemModel.COPPER_WOLF_ARMOR, wearRate = 0.9),
    IRON(Material.IRON_INGOT, 160, ItemModel.IRON_WOLF_ARMOR, wearRate = 0.8),
    GOLDEN(Material.GOLD_INGOT, 112, ItemModel.GOLDEN_WOLF_ARMOR, wearRate = 1.0),
    DIAMOND(Material.DIAMOND, 320, ItemModel.DIAMOND_WOLF_ARMOR, wearRate = 0.6, knockbackResistance = 0.2),
    NETHERITE(Material.NETHERITE_INGOT, 480, ItemModel.NETHERITE_WOLF_ARMOR, wearRate = 0.5, knockbackResistance = 0.4),
    ;

    val id: String = name.lowercase()

    companion object {
        private val tierKey = NamespacedKey("yurucra", "wolf_armor")

        fun of(item: ItemStack): WolfArmorTier? {
            if (item.type != Material.WOLF_ARMOR) return null
            val id = item.persistentDataContainer.get(tierKey, PersistentDataType.STRING) ?: return null
            return entries.find { it.id == id }
        }

        fun create(tier: WolfArmorTier): ItemStack = apply(ItemStack(Material.WOLF_ARMOR), tier)

        /** 材料別の鎧の見た目・名前・耐久値・修理の素材を付ける（鍛冶台で上の材料に替えるときにも使う） */
        fun apply(item: ItemStack, tier: WolfArmorTier): ItemStack = item.apply {
            editPersistentDataContainer { it.set(tierKey, PersistentDataType.STRING, tier.id) }
            setData(DataComponentTypes.ITEM_NAME, Component.translatable("item.yurucra.${tier.id}_wolf_armor", "${tier.displayName} Wolf Armor"))
            withModel(tier.model)
            val equippable = ItemStack(Material.WOLF_ARMOR).getData(DataComponentTypes.EQUIPPABLE)
            if (equippable != null) {
                setData(DataComponentTypes.EQUIPPABLE, equippable.toBuilder().assetId(Key.key("pumpking", "${tier.id}_wolf")).build())
            }
            setData(DataComponentTypes.MAX_DAMAGE, tier.durability)
            setData(
                DataComponentTypes.REPAIRABLE,
                Repairable.repairable(RegistrySet.keySet(RegistryKey.ITEM, TypedKey.create(RegistryKey.ITEM, tier.material.key()))),
            )
            // 染めても見た目が変わらないので、色は付けない
            unsetData(DataComponentTypes.DYED_COLOR)
            if (tier.knockbackResistance > 0) setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, attributesOf(tier))
        }

        /** バニラの狼の鎧の防御力に、ノックバック耐性を足す */
        private fun attributesOf(tier: WolfArmorTier): ItemAttributeModifiers {
            val builder = ItemAttributeModifiers.itemAttributes()
            ItemStack(Material.WOLF_ARMOR).getData(DataComponentTypes.ATTRIBUTE_MODIFIERS)?.modifiers()?.forEach {
                builder.addModifier(it.attribute(), it.modifier(), it.getGroup(), it.display())
            }
            builder.addModifier(
                Attribute.KNOCKBACK_RESISTANCE,
                AttributeModifier(NamespacedKey("yurucra", "wolf_armor_knockback"), tier.knockbackResistance, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.BODY),
                EquipmentSlotGroup.BODY,
            )
            return builder.build()
        }

        private val WolfArmorTier.displayName: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/**
 * 材料別の狼の鎧のレシピと、関わる処理。
 * - 銅・鉄・金・ダイヤは、バニラの狼の鎧と同じ形で作る。ネザライトはダイヤの鎧を鍛冶台でアップグレードする
 * - 材料別の鎧は染めたり、作業台で2つを合わせて直したりできない（材料の印が消えるため）。直すのは金床で
 * - 狼が攻撃を受けて鎧が減ったら、説明文の耐久値を次のtickにまとめて更新する
 * - 良い材料ほど、1回で減る耐久値が少ない。ネザライトの鎧を着た狼は、火や溶岩のダメージを受けない
 */
class WolfArmors(private val plugin: Plugin, private val durability: ItemDurability) : Listener {

    private val netheriteKey = NamespacedKey(plugin, "netherite_wolf_armor")

    private val recipes: List<Recipe> = WolfArmorTier.entries.filter { it != WolfArmorTier.NETHERITE }.map { tier ->
        ShapedRecipe(NamespacedKey(plugin, "${tier.id}_wolf_armor"), WolfArmorTier.create(tier)).apply {
            category = CraftingBookCategory.EQUIPMENT
            shape("A  ", "AAA", "A A")
            setIngredient('A', tier.material)
        }
    } + SmithingTransformRecipe(
        netheriteKey,
        WolfArmorTier.create(WolfArmorTier.NETHERITE),
        RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
        // ダイヤの鎧かどうかは onSmithing で確かめる（耐久値や説明文が変わると、完全一致の材料指定では一致しなくなるため）
        RecipeChoice.MaterialChoice(Material.WOLF_ARMOR),
        RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT),
    )

    private val keys = recipes.map { (it as org.bukkit.Keyed).key }

    /** 説明文の耐久値を更新する狼 */
    private val damagedWolves = LinkedHashSet<Wolf>()

    fun register() {
        val server = plugin.server
        server.removeRecipes(keys)
        if (!server.addRecipes(recipes)) plugin.logger.warning("狼の鎧のレシピの一部を登録できませんでした")
        server.onlinePlayers.forEach { it.discoverRecipes(keys) }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        event.player.discoverRecipes(keys)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onSmithing(event: PrepareSmithingEvent) {
        val recipe = event.inventory.recipe as? SmithingTransformRecipe ?: return
        if (recipe.key != netheriteKey) return
        val base = event.inventory.inputEquipment
        val result = event.result
        if (base == null || result == null || WolfArmorTier.of(base) != WolfArmorTier.DIAMOND) {
            event.result = null
            return
        }
        // 元の鎧のエンチャントなどを引き継いだ結果に、ネザライトの見た目と耐久値を付け直す（割合は ItemDurability が保つ）
        event.result = WolfArmorTier.apply(result, WolfArmorTier.NETHERITE)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onCraft(event: PrepareItemCraftEvent) {
        if (event.inventory.matrix.any { it != null && WolfArmorTier.of(it) != null }) event.inventory.result = null
    }

    /** 狼の鎧が減るとき、材料に応じて減る量を少なくする（端数は確率で切り上げ） */
    @EventHandler(ignoreCancelled = true)
    fun onArmorWear(event: EntityDamageItemEvent) {
        if (event.entity !is Wolf) return
        val tier = WolfArmorTier.of(event.item) ?: return
        if (tier.wearRate >= 1.0) return
        val scaled = event.damage * tier.wearRate
        val whole = scaled.toInt()
        event.damage = whole + if (Random.nextDouble() < scaled - whole) 1 else 0
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onWolfBurn(event: EntityDamageEvent) {
        // 火・燃焼・溶岩・マグマブロック・焚き火・火の玉（バニラの「火のダメージ」の分類）
        if (!DamageTypeTags.IS_FIRE.isTagged(event.damageSource.damageType)) return
        val wolf = event.entity as? Wolf ?: return
        if (WolfArmorTier.of(wolf.equipment.getItem(EquipmentSlot.BODY)) == WolfArmorTier.NETHERITE) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onWolfDamaged(event: EntityDamageEvent) {
        val wolf = event.entity as? Wolf ?: return
        if (wolf.equipment.getItem(EquipmentSlot.BODY).type != Material.WOLF_ARMOR) return
        // たくさん殴られても、予約するのは1tickに1回だけ
        if (damagedWolves.isEmpty()) plugin.runNextTick { refreshLore() }
        damagedWolves += wolf
    }

    private fun refreshLore() {
        for (wolf in damagedWolves) {
            if (!wolf.isValid) continue
            val armor = wolf.equipment.getItem(EquipmentSlot.BODY)
            if (armor.type != Material.WOLF_ARMOR) continue
            wolf.equipment.setItem(EquipmentSlot.BODY, durability.synced(armor))
        }
        damagedWolves.clear()
    }
}
