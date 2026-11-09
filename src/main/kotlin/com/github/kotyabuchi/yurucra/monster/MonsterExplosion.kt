package com.github.kotyabuchi.yurucra.monster

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.github.kotyabuchi.yurucra.util.runNextTick
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.TileState
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Creeper
import org.bukkit.entity.Enemy
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDropItemEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.util.UUID

/**
 * モンスターの爆発（クリーパー・ガストの火の玉・ウィザーなど）で地形が壊れないようにする。
 * 壊れるはずだったブロックは吹き飛んで、着地したら元の場所に戻る（元の場所が埋まっていたら着地した場所に置かれる）。プレイヤーが起こす爆発（TNTなど）はバニラどおり。
 *
 * ほかに、爆発や火のダメージを受けたクリーパーはすぐ爆発する（誘爆）。
 * モンスターの爆発に巻き込まれた生き物は、爆発の中心から強く吹き飛ばされる。
 */
class MonsterExplosion(private val plugin: Plugin) : Listener {

    private class Backup(val location: Location, val data: BlockData)

    /** 吹き飛んでいるブロック → 元の場所と状態 */
    private val flying = mutableMapOf<UUID, Backup>()

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onExplode(event: EntityExplodeEvent) {
        if (!isMonsterExplosion(event.entity)) return
        val blocks = event.blockList()
        if (blocks.isEmpty()) return
        val center = event.location.toVector()
        // 上のブロックから飛ばす。数が多い爆発は上限までにして、残りは壊さない
        for (block in blocks.sortedByDescending { it.y }.take(MAX_FLYING_BLOCKS)) {
            // チェストや看板などは中身があるので飛ばさずにそのまま残す
            if (block.getState(false) is TileState) continue
            val data = block.blockData
            val spawnAt = block.location.add(0.5, 0.0, 0.5)
            val falling = block.world.spawn(spawnAt, FallingBlock::class.java) {
                it.blockData = data
                // 着地した場所に置けないとき（トーチの上など）はアイテムになる
                it.dropItem = true
                it.setHurtEntities(true)
                it.isPersistent = false
            }
            falling.velocity = spawnAt.toVector().subtract(center).normalizeSafe().add(Vector(0.0, 1.2, 0.0))
            flying[falling.uniqueId] = Backup(block.location, data)
            // 周りのブロック（上のトーチなど）がアイテムになって増えないよう、物理更新なしで消す
            block.setType(Material.AIR, false)
        }
        blocks.clear()
    }

    /** 元の場所が空いていれば戻す。埋まっていれば、着地した場所にそのまま置かれる（バニラどおり） */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onLand(event: EntityChangeBlockEvent) {
        val falling = event.entity as? FallingBlock ?: return
        val backup = flying.remove(falling.uniqueId) ?: return
        if (!tryPlace(backup)) return
        event.isCancelled = true
        falling.remove()
    }

    /** 置けずにアイテムになるときも、元の場所が空いていればそちらに戻す */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDropAsItem(event: EntityDropItemEvent) {
        val falling = event.entity as? FallingBlock ?: return
        val backup = flying.remove(falling.uniqueId) ?: return
        if (tryPlace(backup)) event.isCancelled = true
    }

    /** 着地せずに消えたとき（チャンクの読み込みが外れたなど）も、元に戻すかアイテムにする */
    @EventHandler
    fun onRemove(event: EntityRemoveFromWorldEvent) {
        val falling = event.entity as? FallingBlock ?: return
        flying.remove(falling.uniqueId)?.let { placeOrDrop(it) }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onExplosionDamage(event: EntityDamageByEntityEvent) {
        val cause = event.cause
        if (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return
        val entity = event.entity
        if (entity is Creeper) {
            chainExplode(entity)
            return
        }
        val damager = event.damager
        if (!isMonsterExplosion(damager)) return
        entity.velocity = entity.location.toVector().subtract(damager.location.toVector()).normalizeSafe()
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onFireDamage(event: EntityDamageEvent) {
        val creeper = event.entity as? Creeper ?: return
        val cause = event.cause
        if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) chainExplode(creeper)
    }

    /** 停止時に、吹き飛んでいる途中のブロックを全部元に戻す（戻せないものはアイテムにする） */
    fun restoreAll() {
        // remove() で onRemove が呼ばれて二重にならないよう、先に一覧を空にする
        val pending = flying.toMap()
        flying.clear()
        for ((id, backup) in pending) {
            plugin.server.getEntity(id)?.remove()
            placeOrDrop(backup)
        }
    }

    /** 元の場所が空いていれば（空気か液体）戻す。別のブロックが置かれていたら上書きしない */
    private fun tryPlace(backup: Backup): Boolean {
        val block = backup.location.block
        if (!block.type.isAir && !block.isLiquid) return false
        block.setBlockData(backup.data, false)
        return true
    }

    private fun placeOrDrop(backup: Backup) {
        if (tryPlace(backup)) return
        val item = backup.data.placementMaterial
        if (item.isItem && !item.isAir) backup.location.world.dropItemNaturally(backup.location.toCenterLocation(), ItemStack(item))
    }

    /** 爆発の処理の途中で別の爆発を起こさないよう、次のtickに爆発させる */
    private fun chainExplode(creeper: Creeper) {
        plugin.runNextTick { if (creeper.isValid) creeper.explode() }
    }

    private fun isMonsterExplosion(source: Entity): Boolean =
        source is Enemy || (source is Projectile && source.shooter is Enemy)

    private fun Vector.normalizeSafe(): Vector = if (lengthSquared() < 1.0E-6) Vector(0, 0, 0) else normalize()

    companion object {
        private const val MAX_FLYING_BLOCKS = 64
    }
}
