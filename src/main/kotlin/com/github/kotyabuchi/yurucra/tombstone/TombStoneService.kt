package com.github.kotyabuchi.yurucra.tombstone

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.data.TombStoneRepository
import com.github.kotyabuchi.yurucra.util.ItemModel
import com.github.kotyabuchi.yurucra.util.giveOrDrop
import com.github.kotyabuchi.yurucra.util.runTimer
import com.github.kotyabuchi.yurucra.util.text
import com.github.kotyabuchi.yurucra.util.thenOnMain
import com.github.kotyabuchi.yurucra.util.withModel
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

/**
 * 墓石。死亡すると、その場に墓石（防具立て）が立ち、持ち物と装備が保管される。
 * 本人が右クリックか攻撃すると、元のスロットに戻る（そのスロットにあった物は空きへ移すか足元に落とす）。
 *
 * 中身は死亡した直後にDBへ書き込む。メモリには位置などの情報だけを持ち、アイテムは回収時に読む。
 */
class TombStoneService(private val plugin: YuruCra) : Listener {

    // 墓石の防具立ての目印（"yurucra:tombstone"）。値は持ち主のUUID
    private val key = NamespacedKey(plugin, "tombstone")

    private val byOwner = HashMap<UUID, MutableList<TombStone>>()
    private val byEntity = HashMap<UUID, TombStone>()
    // DBへの書き込みが終わっていない墓石（この間に回収されると記録が見つからないため）
    private val saving = HashSet<UUID>()
    // 回収の演出中の墓石（二重に回収されないように）
    private val retrieving = HashSet<UUID>()

    /** 起動時に、全墓石の位置情報を読み込む */
    fun load() {
        plugin.database.submit { TombStoneRepository.loadAll(it) }.join().forEach { index(it) }
    }

    /** 古い順 */
    fun tombStonesOf(owner: UUID): List<TombStone> = byOwner[owner].orEmpty().sortedBy { it.createdAt }

    fun latest(owner: UUID): TombStone? = byOwner[owner]?.maxByOrNull { it.createdAt }

    fun find(owner: UUID, id: Int): TombStone? = byOwner[owner]?.find { it.id == id }

    /** 中身を読む。記録が消えていれば null */
    fun loadItems(stone: TombStone): CompletableFuture<Array<ItemStack>?> =
        plugin.database.submit { TombStoneRepository.loadItems(it, stone.id) }
            .thenApply<Array<ItemStack>?> { bytes -> bytes?.let { ItemStack.deserializeItemsFromBytes(it) } }

    /** 中身ごと墓石を消す（管理コマンド用）。防具立ては、チャンクが読み込まれていれば消す */
    fun delete(stone: TombStone, onDone: () -> Unit = {}) {
        plugin.database.submit { TombStoneRepository.delete(it, stone.id) }.thenOnMain(plugin) {
            unindex(stone)
            plugin.server.getEntity(stone.entityId)?.let { removeEntity(it) }
            onDone()
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        // 持ち物が残る設定なら墓石は要らない（作るとアイテムが増える）
        if (event.keepInventory) return
        val player = event.player
        val keep = event.itemsToKeep
        val contents = player.inventory.contents
        val stored = Array(TombStoneSlots.SIZE) { index ->
            val item = contents.getOrNull(index)
            if (item == null || item.isEmpty) return@Array ItemStack.empty()
            // 手元に残る物（メニュー用コンパスなど）と、死ぬと消える物（消滅の呪い）は入れない
            if (keep.any { it.isSimilar(item) } || item.containsEnchantment(Enchantment.VANISHING_CURSE)) ItemStack.empty() else item.clone()
        }
        if (stored.all { it.isEmpty }) return

        // 保管した物は地面に落とさない（他のプラグインが足したドロップは残す）
        stored.filterNot { it.isEmpty }.forEach { item -> event.drops.remove(item) }

        val location = placeLocation(player.location)
        val stand = spawnStand(location, player)
        val stone = TombStone(
            id = 0,
            owner = player.uniqueId,
            entityId = stand.uniqueId,
            worldName = location.world.name,
            x = location.x, y = location.y, z = location.z,
            createdAt = System.currentTimeMillis(),
        )
        val bytes = ItemStack.serializeItemsAsBytes(stored)
        saving += stand.uniqueId
        plugin.database.submit { TombStoneRepository.insert(it, stone, bytes) }
            .thenOnMain(plugin, onError = { saving -= stand.uniqueId }) { id ->
                saving -= stand.uniqueId
                index(stone.copy(id = id))
            }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractAtEntityEvent) {
        val stand = event.rightClicked as? ArmorStand ?: return
        if (!isTombStone(stand)) return
        event.isCancelled = true
        tryRetrieve(event.player, stand)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onManipulate(event: PlayerArmorStandManipulateEvent) {
        if (isTombStone(event.rightClicked)) event.isCancelled = true
    }

    /** 攻撃（左クリック）でも回収できる。墓石自体は壊れない */
    @EventHandler(priority = EventPriority.HIGH)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val stand = event.entity as? ArmorStand ?: return
        if (!isTombStone(stand)) return
        event.isCancelled = true
        val player = event.damager as? Player ?: return
        tryRetrieve(player, stand)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onDamage(event: EntityDamageEvent) {
        val stand = event.entity as? ArmorStand ?: return
        if (isTombStone(stand)) event.isCancelled = true
    }

    private fun isTombStone(entity: Entity): Boolean = entity.persistentDataContainer.has(key, PersistentDataType.STRING)

    private fun tryRetrieve(player: Player, stand: ArmorStand) {
        val ownerId = stand.persistentDataContainer.get(key, PersistentDataType.STRING) ?: return
        if (ownerId != player.uniqueId.toString()) {
            player.showTitle(
                Title.title(Component.empty(), Component.text("他人の墓荒らしは良くないよ", NamedTextColor.RED, TextDecoration.BOLD))
            )
            return
        }
        if (stand.uniqueId in saving || stand.uniqueId in retrieving) return
        val stone = byEntity[stand.uniqueId]
        if (stone == null) {
            // 記録が消えた墓石（管理コマンドで消したが、チャンクが読み込まれていなかった等）は片付ける
            removeEntity(stand)
            return
        }
        retrieving += stand.uniqueId
        loadItems(stone).thenOnMain(plugin, onError = { retrieving -= stand.uniqueId }) { items ->
            if (items == null) {
                retrieving -= stand.uniqueId
                return@thenOnMain
            }
            playRetrieveEffect(player, stand) { completed ->
                if (completed) giveBack(player, items, stone, stand) else retrieving -= stand.uniqueId
            }
        }
    }

    /** 砂利を掘る音を3回鳴らしてから返す。途中で死んだら中止する */
    private fun playRetrieveEffect(player: Player, stand: ArmorStand, then: (completed: Boolean) -> Unit) {
        var count = 0
        plugin.runTimer(0, 10) { task ->
            if (player.isDead || !player.isOnline) {
                task.cancel()
                then(false)
                return@runTimer
            }
            stand.world.playSound(stand.location, Sound.BLOCK_GRAVEL_BREAK, 1.5f, 0.5f)
            if (count++ >= 2) {
                task.cancel()
                then(true)
            }
        }
    }

    private fun giveBack(player: Player, items: Array<ItemStack>, stone: TombStone, stand: ArmorStand) {
        val inventory = player.inventory
        val displaced = mutableListOf<ItemStack>()
        items.forEachIndexed { index, item ->
            if (item.isEmpty || index >= TombStoneSlots.SIZE) return@forEachIndexed
            inventory.getItem(index)?.takeUnless { it.isEmpty }?.let { displaced += it }
            inventory.setItem(index, item)
        }
        if (displaced.isNotEmpty()) player.giveOrDrop(*displaced.toTypedArray())

        // 返してから記録を消す（逆にすると、途中でサーバーが落ちたときにアイテムが消える）
        unindex(stone)
        removeEntity(stand)
        retrieving -= stand.uniqueId
        plugin.database.submit { TombStoneRepository.delete(it, stone.id) }
            .exceptionally { e -> plugin.logger.log(Level.SEVERE, "墓石 #${stone.id} の削除に失敗しました", e) }
    }

    private fun spawnStand(location: Location, player: Player): ArmorStand =
        location.world.spawn(location, ArmorStand::class.java) { stand ->
            stand.isVisible = false
            stand.setGravity(false)
            // 無敵にすると攻撃イベント自体が来なくなり、左クリックで回収できないので、ダメージは onDamage で防ぐ
            stand.isSilent = true
            stand.setBasePlate(false)
            // リソースパックがあれば墓石の3Dモデル、なければ看板に見える
            stand.equipment.setHelmet(ItemStack(Material.OAK_SIGN).withModel(ItemModel.TOMBSTONE), true)
            stand.addDisabledSlots(*EquipmentSlot.entries.toTypedArray())
            stand.customName(text("R.I.P [${player.name}]").decoration(TextDecoration.BOLD, true))
            stand.isCustomNameVisible = true
            stand.persistentDataContainer.set(key, PersistentDataType.STRING, player.uniqueId.toString())
        }

    /** 死んだブロックの中心。奈落や高すぎる所で死んだときは、ワールドの範囲内に収める */
    private fun placeLocation(death: Location): Location {
        val world = death.world
        val y = death.y.coerceIn(world.minHeight + 1.0, world.maxHeight - 2.0)
        return death.block.location.toCenterLocation().apply { this.y = y }
    }

    private fun removeEntity(entity: Entity) {
        entity.remove()
    }

    private fun index(stone: TombStone) {
        byOwner.getOrPut(stone.owner) { mutableListOf() } += stone
        byEntity[stone.entityId] = stone
    }

    private fun unindex(stone: TombStone) {
        byOwner[stone.owner]?.removeIf { it.id == stone.id }
        byEntity.remove(stone.entityId)
    }
}
