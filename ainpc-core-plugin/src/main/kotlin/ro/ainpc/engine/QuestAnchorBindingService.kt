package ro.ainpc.engine

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.progression.ProgressionAnchorBinding

class QuestAnchorBindingService(private val plugin: AINPCPlugin) {
    fun resolve(template: ScenarioTemplate, player: Player, npc: AINPC): QuestAnchorResolver.ResolvedQuestAnchors {
        if (template.objectives.isEmpty()) {
            return QuestAnchorResolver.ResolvedQuestAnchors.valid(emptyList())
        }

        val bindings = mutableListOf<QuestAnchorResolver.ResolvedQuestAnchor>()
        runCatching {
            val uuid = player.uniqueId.toString()
            val personal = plugin.progressionService.getAnchorBindings(uuid, template.templateId, 50)
            bindings.addAll(personal.map { binding ->
                QuestAnchorResolver.ResolvedQuestAnchor(
                    binding.objectiveKey(), binding.objectiveType(),
                    binding.reference(), binding.anchorType(),
                    binding.anchorId(), binding.displayLabel()
                )
            })
            val global = plugin.progressionService.getAnchorBindings(
                ProgressionAnchorBinding.GLOBAL_PLAYER_UUID, template.templateId, 50
            )
            for (binding in global) {
                if (bindings.none { it.objectiveKey().equals(binding.objectiveKey(), ignoreCase = true) }) {
                    bindings.add(
                        QuestAnchorResolver.ResolvedQuestAnchor(
                            binding.objectiveKey(), binding.objectiveType(),
                            binding.reference(), binding.anchorType(),
                            binding.anchorId(), binding.displayLabel()
                        )
                    )
                }
            }
        }.onFailure { error ->
            plugin.logger.warning("Failed to resolve quest anchor bindings: ${error.message}")
        }

        val worldAdmin = plugin.platform.worldAdminService
        val resolver = QuestAnchorResolver(worldAdmin, null)
        return resolver.resolve(template, player.location, npc, bindings.ifEmpty { null })
    }
}
