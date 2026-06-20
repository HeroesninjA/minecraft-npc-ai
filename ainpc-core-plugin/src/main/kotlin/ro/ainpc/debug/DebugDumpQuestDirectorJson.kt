package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.QuestAuthoringService
import ro.ainpc.engine.QuestAuthoringSnapshot
import ro.ainpc.engine.QuestDirectorRequest
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot

object DebugDumpQuestDirectorJson {
    @JvmStatic
    fun buildQuestDirectorSnapshotJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        root.addProperty("source_type", "quest_director")

        val featurePackLoader = runCatching { plugin.featurePackLoader }.getOrNull()
        if (featurePackLoader == null) {
            root.addProperty("available", false)
            root.addProperty("error", "FeaturePackLoader indisponibil")
            return root
        }

        val definitions = featurePackLoader.getAllScenarios()
            .filter(ProgressionDefinition::isProgressionCandidate)
            .map(ProgressionDefinition::fromScenarioDefinition)
            .sortedBy { it.progressionId() }

        root.addProperty("available", true)
        root.addProperty("definition_count", definitions.size)

        val definitionsArray = JsonArray()
        for (def in definitions.take(50)) {
            val dJson = JsonObject()
            dJson.addProperty("progression_id", def.progressionId())
            dJson.addProperty("pack_id", def.packId())
            dJson.addProperty("mechanic_id", def.mechanicId())
            dJson.addProperty("kind", def.kind())
            dJson.addProperty("definition_id", def.definitionId())
            dJson.addProperty("template_id", def.templateId())
            dJson.addProperty("code", def.code())
            dJson.addProperty("enabled", def.enabled())
            dJson.addProperty("objective_count", def.objectiveCount())
            definitionsArray.add(dJson)
        }
        root.add("definitions", definitionsArray)

        val authoring = QuestAuthoringService()
        val decisionsArray = JsonArray()
        val emptyContext = StoryContextSnapshot.empty()
        for (def in definitions.take(20)) {
            val request = QuestDirectorRequest(
                emptyContext, definitions, "", false, emptyList()
            )
            val snapshot = authoring.analyze(
                emptyContext, definitions, "", "",
                null, null, false, emptyList()
            )
            decisionsArray.add(toDecisionJson(snapshot, def))
        }
        root.add("decisions", decisionsArray)

        return root
    }

    private fun toDecisionJson(snapshot: QuestAuthoringSnapshot, definition: ProgressionDefinition): JsonObject {
        val json = JsonObject()
        json.addProperty("progression_id", definition.progressionId())
        json.addProperty("decision_status", snapshot.decisionStatus())
        json.addProperty("decision_reason", snapshot.decisionReason())
        json.addProperty("runtime_executable", snapshot.decisionRuntimeExecutable())
        json.addProperty("selected_progression", snapshot.selectedProgressionId())

        val matchedSignals = snapshot.decisionMatchedSignals()
        val signals = JsonArray()
        matchedSignals.forEach { signals.add(it) }
        json.add("matched_signals", signals)

        val candidates = snapshot.decisionCandidateTemplateIds()
        val cArray = JsonArray()
        candidates.forEach { cArray.add(it) }
        json.add("candidate_templates", cArray)

        val reasons = snapshot.decisionBlockedReasons()
        val rArray = JsonArray()
        reasons.forEach { rArray.add(it) }
        json.add("blocked_reasons", rArray)

        val warnings = snapshot.decisionWarnings()
        val wArray = JsonArray()
        warnings.forEach { wArray.add(it) }
        json.add("warnings", wArray)

        return json
    }
}
