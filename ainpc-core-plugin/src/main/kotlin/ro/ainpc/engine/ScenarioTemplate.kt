@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.engine

import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition
import ro.ainpc.engine.FeaturePackLoader.QuestStageDefinition
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.npc.NpcScenarioActorDefinition
import java.util.Locale

class ScenarioTemplate(val type: ScenarioType) {
    val roles: MutableMap<String, ScenarioRoleRule> = LinkedHashMap()
    val phases: MutableList<String> = ArrayList()
    var templateId: String = type.name.lowercase(Locale.ROOT)
    var displayName: String = type.displayName
    var description: String = ""
    var sourcePackId: String = "core"
    var hint: String = ""
    var preferredTopologies: MutableList<String> = ArrayList()
    var narrativeHints: MutableList<String> = ArrayList()
    var progressionEnabled: Boolean = type == ScenarioType.QUEST
    var progressionMechanicId: String = if (type == ScenarioType.QUEST) "quest" else ""
    var progressionKind: String = if (type == ScenarioType.QUEST) "quest" else ""
    var progressionLabel: String = if (type == ScenarioType.QUEST) "Quest" else ""
    var progressionSingularLabel: String = "quest"
    var progressionPluralLabel: String = "questuri"
    var progressionMaxActive: Int = 0
    var questCode: String = ""
    var questGiverProfession: String = ""
    var questPrerequisites: MutableList<String> = ArrayList()
    var questRepeatable: Boolean = false
    var questCooldownSeconds: Long = 0L
    var nextQuest: String = ""
    var questDialogues: MutableMap<String, List<String>> = LinkedHashMap()
    var questActorTriggers: MutableMap<String, MutableSet<String>> = LinkedHashMap()
    var validationWarnings: MutableList<String> = ArrayList()
    var validationWarningDetails: List<FeaturePackLoader.ValidationWarning> = ArrayList()
    var questStages: List<QuestStageDefinition> = ArrayList()
    var questContract: QuestScenarioContract = QuestScenarioContract.defaultContract()
    var actors: MutableMap<String, NpcScenarioActorDefinition> = LinkedHashMap()
    var objectives: List<QuestEntryDefinition> = ArrayList()
    var rewards: List<QuestEntryDefinition> = ArrayList()
    var triggerProbability: Double = 0.05
    var minimumNpcCount: Int = 2
    var requiresPlayer: Boolean = false
    var runtimeConditionDefs: List<ScenarioRuntimeDefinition> = emptyList()
    var runtimeTriggerDefs: List<ScenarioRuntimeDefinition> = emptyList()
    var runtimeActionDefs: List<ScenarioRuntimeDefinition> = emptyList()

    fun addRole(roleId: String, description: String) {
        addRole(ScenarioRoleRule(roleId, description, false, false))
    }

    fun addRole(roleId: String, description: String, optional: Boolean) {
        addRole(ScenarioRoleRule(roleId, description, false, optional))
    }

    fun addPlayerRole(roleId: String, description: String) {
        addRole(ScenarioRoleRule(roleId, description, true, false))
    }

    fun addRole(role: ScenarioRoleRule) {
        roles[role.id] = role
    }

    fun addActor(actorId: String, actor: NpcScenarioActorDefinition?) {
        if (actor != null && actorId.isNotBlank()) {
            actors[actorId] = actor
        }
    }

    fun addQuestActorTrigger(triggerId: String, actorIds: Collection<String>?) {
        val normalizedTriggerId = triggerId.trim()
        if (normalizedTriggerId.isBlank() || actorIds.isNullOrEmpty()) {
            return
        }
        val targetIds = questActorTriggers.getOrPut(normalizedTriggerId) { LinkedHashSet() }
        for (actorId in actorIds) {
            val normalizedActorId = actorId.trim()
            if (normalizedActorId.isNotBlank()) {
                targetIds.add(normalizedActorId)
            }
        }
    }

    fun addPhase(phaseId: String, description: String) {
        phases.add(phaseId)
    }

    fun getNpcRoles(): List<ScenarioRoleRule> = roles.values
        .filter { !it.playerRole }
        .sortedWith(compareBy<ScenarioRoleRule> { it.optional }.thenByDescending { it.hasHardRequirements() }.thenBy { it.id })

    fun getPlayerRoles(): List<ScenarioRoleRule> = roles.values.filter { it.playerRole }

    fun hasQuestBriefing(): Boolean = questCode.isNotBlank() || objectives.isNotEmpty() || rewards.isNotEmpty()

    fun getQuestDialogueLines(key: String): List<String> = questDialogues[normalizeQuestDialogueKey(key)] ?: emptyList()

    fun applyQuestDialogues(dialogues: Map<String, List<String>>?) {
        questDialogues.clear()
        if (dialogues == null) return
        for ((key, lines) in dialogues) {
            val normalizedKey = normalizeQuestDialogueKey(key)
            if (normalizedKey.isNotBlank() && lines != null && lines.isNotEmpty()) {
                questDialogues[normalizedKey] = lines.toList()
            }
        }
    }

    companion object {
        private fun normalizeQuestDialogueKey(key: String?): String {
            return key?.trim()?.lowercase(Locale.ROOT)?.replace('-', '_') ?: ""
        }
    }
}
