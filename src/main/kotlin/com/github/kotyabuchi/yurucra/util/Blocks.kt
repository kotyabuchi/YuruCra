package com.github.kotyabuchi.yurucra.util

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace

/** 同じ高さの周囲8方向＋自分自身。上下の段と組み合わせて3x3x3の探索に使う */
val AROUND_FACES: List<BlockFace> = listOf(
    BlockFace.SELF, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST,
    BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST,
)

/** block を中心とした3x3x3の範囲（block自身を含む） */
fun Block.neighbors27(): Sequence<Block> = sequence {
    for (dy in -1..1) {
        val layer = getRelative(0, dy, 0)
        for (face in AROUND_FACES) yield(layer.getRelative(face))
    }
}

fun Material.isWood(): Boolean = Tag.LOGS.isTagged(this) || this == Material.MANGROVE_ROOTS

fun Material.isLeaves(): Boolean = Tag.LEAVES.isTagged(this)

/** 木が生える土台になるブロック */
fun Material.isTreeSoil(): Boolean = Tag.DIRT.isTagged(this)

private val ORES: Set<Material> by lazy {
    buildSet {
        listOf(
            Tag.COAL_ORES, Tag.IRON_ORES, Tag.COPPER_ORES, Tag.GOLD_ORES,
            Tag.REDSTONE_ORES, Tag.LAPIS_ORES, Tag.DIAMOND_ORES, Tag.EMERALD_ORES,
        ).forEach { addAll(it.values) }
        addAll(
            listOf(
                Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.GLOWSTONE,
                Material.GILDED_BLACKSTONE, Material.ANCIENT_DEBRIS,
            )
        )
    }
}

fun Material.isOre(): Boolean = this in ORES

/** その道具で壊したときにドロップが出るか（Block#isValidTool の代わり。あちらは非推奨になった） */
fun Block.canHarvestWith(item: org.bukkit.inventory.ItemStack): Boolean =
    blockData.let { !it.requiresCorrectToolForDrops() || it.isPreferredTool(item) }

/** スキルでまとめて壊してよいブロックか（空気・液体・岩盤のような破壊不能ブロックを除く） */
fun Block.isSkillBreakable(): Boolean = !type.isAir && !isLiquid && type.hardness >= 0f

enum class WoodType { OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK, PALE_OAK, MANGROVE, CHERRY, CRIMSON, WARPED }

fun Material.woodType(): WoodType = when {
    "PALE_OAK" in name -> WoodType.PALE_OAK
    "DARK_OAK" in name -> WoodType.DARK_OAK
    "OAK" in name -> WoodType.OAK
    "BIRCH" in name -> WoodType.BIRCH
    "SPRUCE" in name -> WoodType.SPRUCE
    "JUNGLE" in name -> WoodType.JUNGLE
    "ACACIA" in name -> WoodType.ACACIA
    "MANGROVE" in name -> WoodType.MANGROVE
    "CHERRY" in name -> WoodType.CHERRY
    "CRIMSON" in name -> WoodType.CRIMSON
    "WARPED" in name -> WoodType.WARPED
    // ツツジの葉などはオークの木に付くのでオーク扱い
    else -> WoodType.OAK
}
