package ro.ainpc.engine.runtime.actions

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import ro.ainpc.engine.runtime.ScenarioActionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class PlaySoundAction : ScenarioActionHandler {
    override fun type(): String = "play_sound"

    override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {
        val soundName = action.parameter("sound").ifBlank { return }
        val volume = (action.parameter("volume").toFloatOrNull() ?: 1.0f).coerceIn(0f, 2f)
        val pitch = (action.parameter("pitch").toFloatOrNull() ?: 1.0f).coerceIn(0f, 2f)
        val key = NamespacedKey.minecraft(soundName.lowercase())
        val sound = Registry.SOUND_EVENT.get(key) ?: return
        val player = Bukkit.getPlayer(context.playerUuid()) ?: return
        player.playSound(player.location, sound, volume, pitch)
    }
}
