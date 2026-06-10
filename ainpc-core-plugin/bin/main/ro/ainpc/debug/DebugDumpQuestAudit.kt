package ro.ainpc.debug

import ro.ainpc.AINPCPlugin
import ro.ainpc.database.DatabaseManager
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.progression.ProgressionDefinition
import java.sql.SQLException
import java.time.LocalDateTime

object DebugDumpQuestAudit {
    @JvmStatic
    fun buildQuestAuditReportText(plugin: AINPCPlugin): String {
        val errors = ArrayList<String>()
        val warnings = ArrayList<String>()

        auditLoadedQuestTemplates(plugin, errors, warnings)
        auditQuestPersistence(plugin, errors, warnings)

        val sb = StringBuilder()
        sb.append("AINPC Quest Audit Report\n")
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n")
        sb.append("Errors: ").append(errors.size).append("\n")
        for (error in errors) {
            sb.append("[ERROR] ").append(error).append("\n")
        }
        sb.append("\nWarnings: ").append(warnings.size).append("\n")
        for (warning in warnings) {
            sb.append("[WARN] ").append(warning).append("\n")
        }
        return sb.toString()
    }

    private fun auditLoadedQuestTemplates(
        plugin: AINPCPlugin,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        val featurePackLoader = runCatching { plugin.featurePackLoader }.getOrNull()
        if (featurePackLoader == null) {
            errors.add("FeaturePackLoader indisponibil; nu pot valida quest templates.")
            return
        }

        val worldSemanticIndex = DebugDumpWorldJson.buildWorldMappingSemanticIndexForAudit(plugin)
        var questCount = 0
        for (scenario in featurePackLoader.getAllScenarios()) {
            if (!DebugDumpQuestDefinitionJson.isLoadedQuestDefinitionCandidate(scenario)) {
                continue
            }
            questCount++
            val templateId = DebugDumpSupport.questTemplateId(scenario)
            if (DebugDumpSupport.valueOrEmpty(scenario.questCode).isBlank()) {
                warnings.add("$templateId nu defineste quest.code.")
            }
            if (DebugDumpSupport.valueOrEmpty(scenario.questGiverProfession).isBlank()) {
                warnings.add("$templateId nu defineste quest.giver_profession.")
            }
            auditQuestEntries(
                templateId,
                "objective",
                scenario.objectives,
                DebugDumpSupport.supportedQuestObjectiveTypes(),
                errors,
                warnings,
                worldSemanticIndex,
            )
            auditQuestEntries(
                templateId,
                "reward",
                scenario.rewards,
                DebugDumpSupport.supportedQuestRewardTypes(),
                errors,
                warnings,
            )
            auditQuestObjectiveStages(templateId, scenario, errors, warnings)
        }

        if (questCount == 0) {
            warnings.add("Nu exista definitii jucabile incarcate pentru quest/progression.")
        }
    }

    private fun auditQuestEntries(
        templateId: String,
        entryKind: String,
        entries: List<FeaturePackLoader.QuestEntryDefinition>?,
        supportedTypes: Set<String>,
        errors: MutableList<String>,
        warnings: MutableList<String>,
        worldSemanticIndex: WorldMappingSemanticIndex? = null,
    ) {
        if (entries.isNullOrEmpty()) {
            if (entryKind == "objective") {
                errors.add("$templateId nu are obiective.")
            } else {
                warnings.add("$templateId nu are reward-uri.")
            }
            return
        }

        val entryIds = HashSet<String>()
        for (index in entries.indices) {
            val entry = entries[index]
            val type = if (entryKind == "objective") {
                DebugDumpSupport.normalizeQuestObjectiveType(entry.type)
            } else {
                DebugDumpSupport.normalizeQuestRewardType(entry.type)
            }
            if (!supportedTypes.contains(type)) {
                errors.add("$templateId are $entryKind cu tip nesuportat: ${entry.type}.")
            }
            if (entryKind == "objective") {
                auditQuestSemanticReference(templateId, entry, type, worldSemanticIndex, warnings)
            }
            val entryId = DebugDumpSupport.valueOrEmpty(entry.entryId)
            if (entryId.isBlank()) {
                warnings.add("$templateId are $entryKind fara entry_id stabil la index $index.")
            } else if (!entryIds.add(DebugDumpSupport.normalizeKey(entryId))) {
                errors.add("$templateId are $entryKind duplicat: $entryId.")
            }
        }
    }

    private fun auditQuestSemanticReference(
        templateId: String,
        entry: FeaturePackLoader.QuestEntryDefinition?,
        normalizedObjectiveType: String,
        worldSemanticIndex: WorldMappingSemanticIndex?,
        warnings: MutableList<String>,
    ) {
        if (entry == null || worldSemanticIndex == null) {
            return
        }

        val anchorType = DebugDumpSupport.semanticAnchorTypeForObjective(normalizedObjectiveType)
        val reference = DebugDumpSupport.valueOrEmpty(entry.itemId)
        if (anchorType.isBlank() || anchorType == "npc" || reference.isBlank()) {
            return
        }

        if (!worldSemanticIndex.hasReference(anchorType, reference)) {
            warnings.add(
                templateId + " objective " + DebugDumpSupport.valueOrFallback(entry.entryId, normalizedObjectiveType) +
                    " refera `" + reference + "`, dar tokenul nu apare in world mapping semantic_index pentru ancora " +
                    anchorType + ".",
            )
        }
    }

    private fun auditQuestObjectiveStages(
        templateId: String,
        scenario: FeaturePackLoader.ScenarioDefinition?,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        if (scenario == null || scenario.objectives.isEmpty()) {
            return
        }

        val knownPhases = HashSet<String>()
        for (phase in scenario.phases) {
            val normalizedPhase = DebugDumpSupport.normalizeKey(phase)
            if (normalizedPhase.isNotBlank()) {
                knownPhases.add(normalizedPhase)
            }
        }

        var hasStagedObjective = false
        var hasUnstagedObjective = false
        for (objective in scenario.objectives) {
            val stage = DebugDumpSupport.questEntryStage(objective)
            if (stage.isBlank()) {
                if (DebugDumpSupport.questStageReferencesObjective(scenario, objective)) {
                    hasStagedObjective = true
                } else {
                    hasUnstagedObjective = true
                }
                continue
            }

            hasStagedObjective = true
            if (!knownPhases.contains(DebugDumpSupport.normalizeKey(stage))) {
                errors.add("$templateId are objective phase/stage necunoscut: $stage.")
            }
        }

        if (hasStagedObjective && hasUnstagedObjective) {
            warnings.add("$templateId combina obiective cu phase/stage si obiective fara etapa explicita.")
        }

        auditQuestStageDefinitions(templateId, scenario, knownPhases, errors, warnings)
    }

    private fun auditQuestStageDefinitions(
        templateId: String,
        scenario: FeaturePackLoader.ScenarioDefinition?,
        knownPhases: Set<String>,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        if (scenario == null || scenario.questStages.isEmpty()) {
            return
        }

        val objectiveReferences = DebugDumpSupport.collectQuestObjectiveReferences(scenario.objectives)
        for (stage in scenario.questStages) {
            if (stage.id.isBlank()) {
                errors.add("$templateId are quest stage fara ID.")
                continue
            }

            val normalizedStageId = DebugDumpSupport.normalizeKey(stage.id)
            if (!knownPhases.contains(normalizedStageId)) {
                errors.add("$templateId are quest stage care nu exista in phases: ${stage.id}.")
            }

            val completionMode = DebugDumpSupport.normalizeQuestStageCompletionMode(stage.completionMode)
            if (!DebugDumpSupport.isSupportedQuestStageCompletionMode(completionMode)) {
                errors.add("$templateId stage ${stage.id} are completion_mode necunoscut: ${stage.completionMode}.")
            }

            auditQuestStageNextStage(templateId, scenario, stage, knownPhases, normalizedStageId, errors, warnings)

            var stageHasObjectiveMetadata = false
            for (objective in scenario.objectives) {
                if (DebugDumpSupport.normalizeKey(DebugDumpSupport.questEntryStage(objective)) == normalizedStageId) {
                    stageHasObjectiveMetadata = true
                    break
                }
            }
            if (stage.objectiveIds.isEmpty()) {
                if (!"phases".equals(stage.metadata.getOrDefault("source", ""), ignoreCase = true) &&
                    !stageHasObjectiveMetadata
                ) {
                    warnings.add(
                        "$templateId stage ${stage.id} nu listeaza objectives si nu are obiective cu phase/stage aferent.",
                    )
                }
                continue
            }

            val seenStageObjectives = HashSet<String>()
            for (objectiveId in stage.objectiveIds) {
                val normalizedObjective = DebugDumpSupport.normalizeQuestStageReference(objectiveId)
                if (normalizedObjective.isBlank()) {
                    warnings.add("$templateId stage ${stage.id} are objective ID gol.")
                    continue
                }
                if (!seenStageObjectives.add(normalizedObjective)) {
                    warnings.add("$templateId stage ${stage.id} listeaza objective duplicat: $objectiveId.")
                }
                if (!objectiveReferences.contains(normalizedObjective)) {
                    errors.add("$templateId stage ${stage.id} refera objective necunoscut: $objectiveId.")
                }
            }
        }
    }

    private fun auditQuestStageNextStage(
        templateId: String,
        scenario: FeaturePackLoader.ScenarioDefinition,
        stage: FeaturePackLoader.QuestStageDefinition,
        knownPhases: Set<String>,
        normalizedStageId: String,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        val nextStage = stage.getNextStageId()
        if (nextStage.isBlank()) {
            return
        }

        val normalizedNextStage = DebugDumpSupport.normalizeKey(nextStage)
        if (normalizedNextStage.isBlank()) {
            warnings.add("$templateId stage ${stage.id} are next_stage gol.")
            return
        }
        if (normalizedNextStage == normalizedStageId) {
            errors.add("$templateId stage ${stage.id} are next_stage catre sine.")
        }
        if (!knownPhases.contains(normalizedNextStage)) {
            errors.add("$templateId stage ${stage.id} are next_stage necunoscut: $nextStage.")
        } else if (!DebugDumpSupport.isQuestRuntimeStage(scenario, normalizedNextStage)) {
            warnings.add("$templateId stage ${stage.id} are next_stage catre o faza fara obiective runtime: $nextStage.")
        }
    }

    private fun auditQuestPersistence(
        plugin: AINPCPlugin,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            warnings.add("DatabaseManager indisponibil; nu pot valida player_quests, quest_anchor_bindings sau story_events.")
            return
        }

        auditTrackedQuestPersistence(databaseManager, errors, warnings)
        auditQuestAnchorPersistence(databaseManager, errors, warnings)
        auditStoredQuestJson(databaseManager, warnings)
        auditStoryProgressionConsistency(plugin, databaseManager, warnings)
    }

    private fun auditTrackedQuestPersistence(
        databaseManager: DatabaseManager,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        val duplicateTrackedSql = """
            SELECT player_uuid, COUNT(*) AS tracked_count
            FROM player_quests
            WHERE tracked != 0
            GROUP BY player_uuid
            HAVING COUNT(*) > 1
            ORDER BY tracked_count DESC, player_uuid
            LIMIT 50
        """.trimIndent()
        try {
            databaseManager.prepareStatement(duplicateTrackedSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        errors.add(
                            "player_quests are ${resultSet.getInt("tracked_count")} questuri tracked pentru jucatorul " +
                                "${resultSet.getString("player_uuid")}.",
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("Nu pot valida unicitatea player_quests.tracked: ${exception.message}")
        }

        val inactiveTrackedSql = """
            SELECT player_uuid, template_id, status
            FROM player_quests
            WHERE tracked != 0
              AND LOWER(status) NOT IN ('active', 'offered')
            ORDER BY player_uuid, template_id
            LIMIT 50
        """.trimIndent()
        try {
            databaseManager.prepareStatement(inactiveTrackedSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        errors.add(
                            "player_quests.tracked indica quest inactiv: " +
                                resultSet.getString("player_uuid") + " " + resultSet.getString("template_id") +
                                " status=" + resultSet.getString("status") + ".",
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("Nu pot valida statusul player_quests.tracked: ${exception.message}")
        }
    }

    private fun auditQuestAnchorPersistence(
        databaseManager: DatabaseManager,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        val duplicateAnchorsSql = """
            SELECT player_uuid, template_id, objective_key, COUNT(*) AS duplicate_count
            FROM quest_anchor_bindings
            GROUP BY player_uuid, template_id, objective_key
            HAVING COUNT(*) > 1
            ORDER BY duplicate_count DESC, player_uuid, template_id, objective_key
            LIMIT 50
        """.trimIndent()
        try {
            databaseManager.prepareStatement(duplicateAnchorsSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        errors.add(
                            "quest_anchor_bindings are duplicate pentru " +
                                resultSet.getString("player_uuid") + " " + resultSet.getString("template_id") +
                                " " + resultSet.getString("objective_key") + ": " +
                                resultSet.getInt("duplicate_count") + ".",
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("Nu pot valida duplicatele din quest_anchor_bindings: ${exception.message}")
        }

        val orphanAnchorsSql = """
            SELECT b.player_uuid, b.template_id, b.objective_key
            FROM quest_anchor_bindings b
            LEFT JOIN player_quests p
              ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
            WHERE p.player_uuid IS NULL
            ORDER BY b.player_uuid, b.template_id, b.objective_key
            LIMIT 50
        """.trimIndent()
        try {
            databaseManager.prepareStatement(orphanAnchorsSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        warnings.add(
                            "quest_anchor_bindings orfan fara player_quests parinte: " +
                                resultSet.getString("player_uuid") + " " + resultSet.getString("template_id") +
                                " " + resultSet.getString("objective_key") + ".",
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("Nu pot valida ancorele orfane: ${exception.message}")
        }
    }

    private fun auditStoredQuestJson(databaseManager: DatabaseManager, warnings: MutableList<String>) {
        val playerQuestSql = """
            SELECT player_uuid, template_id, objective_progress, quest_variables
            FROM player_quests
            ORDER BY player_uuid, template_id
            LIMIT 500
        """.trimIndent()
        try {
            databaseManager.prepareStatement(playerQuestSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val label = resultSet.getString("player_uuid") + " " + resultSet.getString("template_id")
                        auditStoredJsonColumn(
                            warnings,
                            "player_quests.objective_progress",
                            label,
                            resultSet.getString("objective_progress"),
                        )
                        auditStoredJsonColumn(
                            warnings,
                            "player_quests.quest_variables",
                            label,
                            resultSet.getString("quest_variables"),
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("Nu pot valida JSON-ul din player_quests: ${exception.message}")
        }

        val regionStoryStateSql = """
            SELECT region_id, story_pool, variables
            FROM region_story_state
            ORDER BY updated_at DESC, region_id
            LIMIT 500
        """.trimIndent()
        try {
            databaseManager.prepareStatement(regionStoryStateSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val label = resultSet.getString("region_id")
                        auditStoredJsonColumn(
                            warnings,
                            "region_story_state.story_pool",
                            label,
                            resultSet.getString("story_pool"),
                        )
                        auditStoredJsonColumn(
                            warnings,
                            "region_story_state.variables",
                            label,
                            resultSet.getString("variables"),
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("Nu pot valida JSON-ul din region_story_state: ${exception.message}")
        }

        val placeStoryStateSql = """
            SELECT place_id, variables
            FROM place_story_state
            ORDER BY updated_at DESC, place_id
            LIMIT 500
        """.trimIndent()
        try {
            databaseManager.prepareStatement(placeStoryStateSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        auditStoredJsonColumn(
                            warnings,
                            "place_story_state.variables",
                            resultSet.getString("place_id"),
                            resultSet.getString("variables"),
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("Nu pot valida JSON-ul din place_story_state: ${exception.message}")
        }

        val storyEventSql = """
            SELECT id, payload
            FROM story_events
            ORDER BY created_at DESC, id DESC
            LIMIT 500
        """.trimIndent()
        try {
            databaseManager.prepareStatement(storyEventSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        auditStoredJsonColumn(
                            warnings,
                            "story_events.payload",
                            "id=" + resultSet.getLong("id"),
                            resultSet.getString("payload"),
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("Nu pot valida JSON-ul din story_events: ${exception.message}")
        }
    }

    private fun auditStoredJsonColumn(
        warnings: MutableList<String>,
        column: String,
        label: String,
        rawValue: String?,
    ) {
        if (DebugDumpSupport.isStoredJsonValid(rawValue)) {
            return
        }
        warnings.add("$column are JSON invalid pentru $label.")
    }

    private fun auditStoryProgressionConsistency(
        plugin: AINPCPlugin,
        databaseManager: DatabaseManager,
        warnings: MutableList<String>,
    ) {
        val scenariosBySelector = buildProgressionScenarioLookup(plugin)
        if (scenariosBySelector.isEmpty()) {
            return
        }

        try {
            val storyEventProgressionKeys = queryStoryEventProgressionKeys(databaseManager, warnings)
            val completedProgressionsSql = """
                SELECT player_uuid, template_id, quest_code, status
                FROM player_quests
                WHERE LOWER(COALESCE(status, '')) IN ('completed', 'complete', 'done')
                ORDER BY completed_at DESC, updated_at DESC
                LIMIT 500
            """.trimIndent()

            databaseManager.prepareStatement(completedProgressionsSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val playerUuid = resultSet.getString("player_uuid")
                        val templateId = resultSet.getString("template_id")
                        val questCode = resultSet.getString("quest_code")
                        val scenario = DebugDumpSupport.findScenarioForProgressionRow(
                            templateId,
                            questCode,
                            scenariosBySelector,
                        )
                        if (scenario == null || !DebugDumpSupport.hasRecordStoryEventAction(scenario)) {
                            continue
                        }
                        if (DebugDumpSupport.hasStoryEventProgressionKey(
                                storyEventProgressionKeys,
                                playerUuid,
                                templateId,
                                questCode,
                            )
                        ) {
                            continue
                        }
                        warnings.add(
                            "Progresie completata cu record_story_event fara story_event asociat detectabil: player_uuid=" +
                                playerUuid + ", template_id=" + templateId + ", quest_code=" + questCode +
                                ". Verifica payload.quest_template/quest_code in story_events.",
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("Nu pot valida consistenta story/progression: ${exception.message}")
        }
    }

    @Throws(SQLException::class)
    private fun queryStoryEventProgressionKeys(
        databaseManager: DatabaseManager,
        warnings: MutableList<String>,
    ): Set<String> {
        val keys = HashSet<String>()
        val storyEventSql = """
            SELECT id, player_uuid, event_key, actor_type, payload
            FROM story_events
            ORDER BY created_at DESC, id DESC
            LIMIT 1000
        """.trimIndent()

        databaseManager.prepareStatement(storyEventSql).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val id = resultSet.getLong("id")
                    val playerUuid = DebugDumpSupport.valueOrEmpty(resultSet.getString("player_uuid"))
                    val actorType = DebugDumpSupport.valueOrEmpty(resultSet.getString("actor_type"))
                    val eventKey = DebugDumpSupport.valueOrEmpty(resultSet.getString("event_key"))
                    val payload = DebugDumpSupport.parseStoredJsonObject(resultSet.getString("payload"))
                    val questTemplate = DebugDumpSupport.jsonString(payload, "quest_template")
                    val questCodeFromPayload = DebugDumpSupport.jsonString(payload, "quest_code")
                    val questCode = DebugDumpSupport.firstNonBlank(questCodeFromPayload, eventKey)
                    val payloadPlayerUuid = DebugDumpSupport.jsonString(payload, "player_uuid")
                    val effectivePlayerUuid = DebugDumpSupport.firstNonBlank(playerUuid, payloadPlayerUuid)

                    DebugDumpSupport.addStoryEventProgressionKey(keys, effectivePlayerUuid, questTemplate)
                    DebugDumpSupport.addStoryEventProgressionKey(keys, effectivePlayerUuid, questCode)

                    val questLikeEvent = "quest".equals(actorType, ignoreCase = true) ||
                        questTemplate.isNotBlank() ||
                        questCodeFromPayload.isNotBlank()
                    if (questLikeEvent && questTemplate.isBlank() && questCode.isBlank()) {
                        warnings.add(
                            "story_events id=$id pare legat de quest, dar nu are payload.quest_template sau payload.quest_code.",
                        )
                    }
                }
            }
        }
        return keys
    }

    private fun buildProgressionScenarioLookup(
        plugin: AINPCPlugin
    ): Map<String, FeaturePackLoader.ScenarioDefinition> {
        val loader = runCatching { plugin.featurePackLoader }.getOrNull() ?: return emptyMap()

        val lookup = LinkedHashMap<String, FeaturePackLoader.ScenarioDefinition>()
        for (scenario in loader.getAllScenarios()) {
            if (!ProgressionDefinition.isProgressionCandidate(scenario)) {
                continue
            }
            val definition = ProgressionDefinition.fromScenarioDefinition(scenario)
            DebugDumpSupport.addScenarioLookupKey(lookup, definition.templateId(), scenario)
            DebugDumpSupport.addScenarioLookupKey(lookup, definition.progressionId(), scenario)
            DebugDumpSupport.addScenarioLookupKey(lookup, definition.definitionId(), scenario)
            DebugDumpSupport.addScenarioLookupKey(lookup, definition.code(), scenario)
            DebugDumpSupport.addScenarioLookupKey(lookup, definition.packId() + ":" + definition.definitionId(), scenario)
            DebugDumpSupport.addScenarioLookupKey(
                lookup,
                definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId(),
                scenario,
            )
        }
        return lookup
    }
}
