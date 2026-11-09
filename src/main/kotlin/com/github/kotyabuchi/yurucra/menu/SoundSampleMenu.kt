package com.github.kotyabuchi.yurucra.menu

import com.github.kotyabuchi.yurucra.YuruCra
import com.github.kotyabuchi.yurucra.util.buttonItem
import com.github.kotyabuchi.yurucra.util.loreLine
import com.github.kotyabuchi.yurucra.util.text
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * ゲームの音を一覧にして試し聞きする、OP用のメニュー（効果音を選ぶときに使う）。
 * 左クリックで普通の高さ、右クリックで低く（0.5）、シフトクリックで高く（1.5）鳴らす。
 * 入りきらない分は自動でページ送りになる。
 */
class SoundSampleMenu(plugin: YuruCra) : Menu(plugin, text("Sound Sample"), samples.size) {

    override fun build() {
        samples.forEachIndexed { index, sample -> put(SoundButton(sample), page = index / PAGE_SIZE) }
    }

    private class Sample(val sound: Sound, val name: String, val icon: Material)

    private class SoundButton(private val sample: Sample) : MenuButton(
        buttonItem(
            sample.icon,
            text(sample.name),
            listOf(
                loreLine("Left Click: ", "Pitch 1.0"),
                loreLine("Right Click: ", "Pitch 0.5"),
                loreLine("Shift Click: ", "Pitch 1.5"),
            ),
        ),
        clickSound = null,
    ) {
        override fun onLeftClick(player: Player, event: InventoryClickEvent, menu: Menu) = play(player, event)
        override fun onRightClick(player: Player, event: InventoryClickEvent, menu: Menu) = play(player, event)

        private fun play(player: Player, event: InventoryClickEvent) {
            val pitch = when {
                event.isShiftClick -> 1.5f
                event.isRightClick -> 0.5f
                else -> 1f
            }
            player.playSound(player, sample.sound, 1f, pitch)
        }
    }

    private companion object {
        /** 枠なしのメニューの1ページに並ぶ数（9列×5段） */
        const val PAGE_SIZE = 9 * 5

        /** 音の一覧は変わらないので、最初に開いたときに1度だけ作る */
        val samples: List<Sample> by lazy {
            val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT)
            registry.stream()
                .map { sound ->
                    val key = registry.getKeyOrThrow(sound)
                    val name = if (key.namespace == "minecraft") key.value() else key.asString()
                    Sample(sound, name, iconOf(key.value()))
                }
                .toList()
                .sortedBy { it.name }
        }

        /** 「block.anvil.land」なら金床、「entity.zombie.ambient」ならゾンビのスポーンエッグ。見つからなければ音符ブロック */
        fun iconOf(path: String): Material {
            val parts = path.split('.')
            val name = parts.getOrNull(1)?.uppercase() ?: return Material.NOTE_BLOCK
            val candidates = when (parts[0]) {
                "entity" -> listOf("${name}_SPAWN_EGG", name)
                "block", "item" -> listOf(name)
                else -> emptyList()
            }
            return candidates.firstNotNullOfOrNull { candidate ->
                Material.getMaterial(candidate)?.takeIf { it.isItem && !it.isAir }
            } ?: Material.NOTE_BLOCK
        }
    }
}
