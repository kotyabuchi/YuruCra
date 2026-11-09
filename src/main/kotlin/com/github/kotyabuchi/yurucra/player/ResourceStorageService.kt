package com.github.kotyabuchi.yurucra.player

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.util.consumeOne
import com.github.kotyabuchi.yurucra.util.fillBlockCandidates
import com.github.kotyabuchi.yurucra.util.giveOrDrop
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.event.player.PlayerAttemptPickupItemEvent

class ResourceStorageService(private val plugin: YuruCra) : Listener {

    /** 既に倉庫にある素材を拾ったら、自動で倉庫にしまう */
    @EventHandler(ignoreCancelled = true)
    fun onPickup(event: PlayerAttemptPickupItemEvent) {
        val player = event.player
        val storage = plugin.players.status(player).resourceStorage
        if (!storage.autoStore || storage.stored.isEmpty()) return
        val item = event.item
        val stack = item.itemStack
        if (!storage.accepts(stack)) return
        stack.amount -= storage.store(stack)
        if (stack.amount <= 0) {
            event.isCancelled = true
            item.remove()
            player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f)
        } else {
            item.itemStack = stack
        }
    }

    /** 倉庫に入る分は倉庫へ、残りはインベントリへ（入らなければ足元に落とす） */
    fun storeOrGive(player: Player, stack: ItemStack) {
        val storage = plugin.players.status(player).resourceStorage
        val rest = stack.clone()
        if (storage.autoStore && storage.accepts(rest)) rest.amount -= storage.store(rest)
        if (rest.amount > 0) player.giveOrDrop(rest)
    }

    /**
     * 穴埋め用のブロックを1つ消費する。倉庫（最後の1個は残す）→ インベントリの順に探す。
     * @return 消費したブロック。見つからなければ null
     */
    fun consumeFillBlock(player: Player, at: Block): Material? {
        val candidates = fillBlockCandidates(at)
        val storage = plugin.players.status(player).resourceStorage
        candidates.firstOrNull { storage.amountOf(it) > 1 }?.let {
            storage.take(it, 1)
            return it
        }
        return candidates.firstOrNull { player.inventory.consumeOne(it) }
    }
}
