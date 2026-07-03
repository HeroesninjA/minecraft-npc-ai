package ro.ainpc.engine

import ro.ainpc.npc.AINPC
import java.util.UUID

class QuestTemplateSelectionService(
    private val questTemplates: Map<String, ScenarioTemplate>,
    private val scenarioTemplates: Map<ScenarioType, ScenarioTemplate>,
    private val currentQuestProgressProvider: (UUID, String) -> PlayerQuestProgress?
) {
    fun findQuestTemplateForNpc(npc: AINPC): ScenarioTemplate {
        return findQuestTemplateForNpc(npc, UUID(0, 0))
    }

    fun findQuestTemplateForNpc(npc: AINPC, playerId: UUID): ScenarioTemplate {
        val occupation = npc.occupation ?: ""
        for (template in questTemplates.values) {
            if (occupation.isNotBlank() && template.questGiverProfession.equals(occupation, ignoreCase = true)) {
                return template
            }
        }
        return questTemplates.values.firstOrNull() ?: scenarioTemplates[ScenarioType.QUEST]
            ?: error("No QUEST template found in scenarioTemplates.")
    }

    fun findQuestTemplateForNpc(npc: AINPC, playerId: UUID, templateId: String): ScenarioTemplate {
        if (templateId.isBlank()) return findQuestTemplateForNpc(npc, playerId)
        for (template in questTemplates.values) {
            if (template.templateId.equals(templateId, ignoreCase = true) || template.questCode.equals(templateId, ignoreCase = true)) {
                return template
            }
        }
        return findQuestTemplateForNpc(npc, playerId)
    }

    fun resolveCurrentQuestTemplateForNpc(npc: AINPC, playerId: UUID): ScenarioTemplate {
        val progress = currentQuestProgressProvider(playerId, "")
        if (progress != null) {
            val template = questTemplates[progress.templateId()]
            if (template != null) return template
        }
        return findQuestTemplateForNpc(npc, playerId)
    }

    fun resolveCurrentQuestTemplateForNpc(npc: AINPC, playerId: UUID, templateId: String): ScenarioTemplate {
        if (templateId.isNotBlank()) {
            for (template in questTemplates.values) {
                if (template.templateId.equals(templateId, ignoreCase = true) || template.questCode.equals(templateId, ignoreCase = true)) {
                    return template
                }
            }
        }
        return resolveCurrentQuestTemplateForNpc(npc, playerId)
    }

    fun resolveCurrentQuestTemplateForNpc(npc: AINPC, progress: PlayerQuestProgress): ScenarioTemplate {
        val template = questTemplates[progress.templateId()]
        if (template != null) return template
        return findQuestTemplateForNpc(npc)
    }

    fun buildQuestBriefingMessages(template: ScenarioTemplate): List<String> {
        return template.description.let { if (it.isNotBlank()) listOf(it) else emptyList() }
    }
}
