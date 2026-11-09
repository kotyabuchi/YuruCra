package com.github.kotyabuchi.yurucra.feature

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.CraftingRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.recipe.CraftingBookCategory
import org.bukkit.plugin.Plugin

/**
 * バニラにない便利レシピ。参加時にレシピ本へ追加する。
 * 旧版にあった名札のレシピは、バニラに安いレシピ（紙＋金属ナゲット）ができたので外した。
 */
class UtilityRecipes(private val plugin: Plugin) : Listener {

    private val recipes: List<CraftingRecipe> = listOf(
        // 原木8個でチェスト4個（バニラは板材8個で1個）
        shaped("chest", ItemStack(Material.CHEST, 4), CraftingBookCategory.MISC, "AAA", "A A", "AAA") {
            setIngredient('A', RecipeChoice.MaterialChoice(Tag.LOGS))
        },
        horseArmor("iron_horse_armor", Material.IRON_HORSE_ARMOR, Material.IRON_INGOT),
        horseArmor("golden_horse_armor", Material.GOLDEN_HORSE_ARMOR, Material.GOLD_INGOT),
        horseArmor("diamond_horse_armor", Material.DIAMOND_HORSE_ARMOR, Material.DIAMOND),
        // ブレイズロッドを使わないエンドロッド
        shaped("end_rod", ItemStack(Material.END_ROD, 2), CraftingBookCategory.MISC, "A", "B", "C") {
            setIngredient('A', Material.IRON_INGOT)
            setIngredient('B', Material.GLOWSTONE_DUST)
            setIngredient('C', Material.PURPUR_BLOCK)
        },
        // レッドストーントーチの代わりにレッドストーンと棒で作るリピーター
        shaped("repeater", ItemStack(Material.REPEATER), CraftingBookCategory.REDSTONE, "A A", "BAB", "CCC") {
            setIngredient('A', Material.REDSTONE)
            setIngredient('B', Material.STICK)
            setIngredient('C', Material.STONE)
        },
        // ドロッパーに弓の材料（棒と糸）を足して作るディスペンサー
        shaped("dispenser", ItemStack(Material.DISPENSER), CraftingBookCategory.REDSTONE, " AB", "ACB", " AB") {
            setIngredient('A', Material.STICK)
            setIngredient('B', Material.STRING)
            setIngredient('C', Material.DROPPER)
        },
        ShapelessRecipe(key("wheat_seeds"), ItemStack(Material.WHEAT_SEEDS, 2)).apply {
            category = CraftingBookCategory.MISC
            addIngredient(Material.WHEAT)
        },
    )

    private val keys: List<NamespacedKey> = recipes.map { it.key }

    fun register() {
        val server = plugin.server
        // /reload などで残っている同じキーのレシピがあると追加に失敗するので先に消す。どちらもまとめて1回で反映される
        server.removeRecipes(keys)
        if (!server.addRecipes(recipes)) plugin.logger.warning("追加レシピの一部を登録できませんでした")
        server.onlinePlayers.forEach { it.discoverRecipes(keys) }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        // 覚えていないものだけ追加される
        event.player.discoverRecipes(keys)
    }

    private fun key(name: String) = NamespacedKey(plugin, name)

    private fun shaped(
        name: String,
        result: ItemStack,
        category: CraftingBookCategory,
        vararg shape: String,
        ingredients: ShapedRecipe.() -> Unit,
    ): ShapedRecipe = ShapedRecipe(key(name), result).apply {
        this.category = category
        shape(*shape)
        ingredients()
    }

    private fun horseArmor(name: String, result: Material, material: Material): ShapedRecipe =
        shaped(name, ItemStack(result), CraftingBookCategory.EQUIPMENT, "AAA", "ABA") {
            setIngredient('A', material)
            setIngredient('B', Material.TRIPWIRE_HOOK)
        }
}
