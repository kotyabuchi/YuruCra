package com.github.kotyabuchi.yurucra.skill

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.job.Job
import com.github.kotyabuchi.yurucra.util.runLater
import com.github.kotyabuchi.yurucra.util.runTimer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * オン・オフを切り替えるスキル。
 * オン状態は「オンにした時のレベル」としてプレイヤーのPDCに保存されるので、再ログインしても残る。
 */
abstract class ToggleSkill(plugin: YuruCra) : Skill(plugin) {

    fun isEnabled(player: Player): Boolean = player.persistentDataContainer.has(key, PersistentDataType.INTEGER)

    protected fun store(player: Player, level: Int) =
        player.persistentDataContainer.set(key, PersistentDataType.INTEGER, level)

    protected fun clear(player: Player) = player.persistentDataContainer.remove(key)

    override fun activate(player: Player, level: Int) {
        if (level < needLevel) {
            fail(player, "$displayName: Not enough levels (Need Lv.$needLevel)")
        } else if (!isEnabled(player)) {
            onEnable(player, level)
            store(player, level)
        }
    }

    open fun deactivate(player: Player) {
        onDisable(player)
        clear(player)
    }

    fun toggle(player: Player, level: Int) {
        if (isEnabled(player)) deactivate(player) else activate(player, level)
    }

    protected open fun onEnable(player: Player, level: Int) {
        player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        player.sendActionBar(Component.text("$displayName On", NamedTextColor.GREEN))
    }

    protected open fun onDisable(player: Player) {
        player.playSound(player.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.2f, 2.0f)
        player.sendActionBar(Component.text("$displayName Off", NamedTextColor.RED))
    }
}

/** オンにすると一定時間で自動的にオフになるスキル（クールタイムあり） */
abstract class TimedToggleSkill(plugin: YuruCra) : ToggleSkill(plugin) {

    private val activeTasks = HashMap<UUID, BukkitTask>()

    abstract fun activeTicks(level: Int): Long

    override fun activate(player: Player, level: Int) {
        if (!checkUsable(player, level)) return
        onEnable(player, level)
        markUsed(player.uniqueId)
        store(player, level)
        restartActiveTime(player, level)
    }

    override fun deactivate(player: Player) {
        super.deactivate(player)
        activeTasks.remove(player.uniqueId)?.cancel()
    }

    /** 効果時間を最初から数え直す */
    fun restartActiveTime(player: Player, level: Int) {
        activeTasks.remove(player.uniqueId)?.cancel()
        activeTasks[player.uniqueId] = plugin.runLater(activeTicks(level)) {
            activeTasks.remove(player.uniqueId)
            if (player.isOnline) deactivate(player)
        }
    }

    /** 通知を出さずにオン状態を消す（ログイン時の掃除用） */
    fun forceClear(player: Player) {
        activeTasks.remove(player.uniqueId)?.cancel()
        clear(player)
    }
}

/** 道具を持ち替える・捨てる・壊れるとオフになる時間制スキル */
abstract class ToolLinkedSkill(plugin: YuruCra) : TimedToggleSkill(plugin) {

    @EventHandler
    fun onClickHeldItem(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!isEnabled(player)) return
        if (event.clickedInventory != player.inventory || event.slot != player.inventory.heldItemSlot) return
        event.isCancelled = true
        deactivate(player)
    }

    @EventHandler
    fun onBreakItem(event: PlayerItemBreakEvent) {
        if (isEnabled(event.player)) deactivate(event.player)
    }

    @EventHandler
    fun onChangeSlot(event: PlayerItemHeldEvent) {
        if (isEnabled(event.player)) deactivate(event.player)
    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        if (!isEnabled(event.player)) return
        event.isCancelled = true
        deactivate(event.player)
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        if (isEnabled(event.player)) deactivate(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (isEnabled(event.player)) forceClear(event.player)
    }
}

/** ジョブのレベルで習得し、メニューからオン・オフするスキル。切り替え時の通知は出さない */
abstract class PassiveSkill(plugin: YuruCra, val owner: Job) : ToggleSkill(plugin) {
    override fun onEnable(player: Player, level: Int) {}
    override fun onDisable(player: Player) {}
}

/** オンの間、ジョブの道具を持っていると1秒ごとに効果をかけ続けるパッシブスキル */
abstract class ToolLinkedEffectSkill(plugin: YuruCra, owner: Job) : PassiveSkill(plugin, owner) {

    abstract fun applyEffect(player: Player)

    fun start() {
        plugin.runTimer(0, 20) {
            plugin.server.onlinePlayers.forEach { player ->
                if (isEnabled(player) && owner.isJobTool(player.inventory.itemInMainHand.type)) applyEffect(player)
            }
        }
    }
}
