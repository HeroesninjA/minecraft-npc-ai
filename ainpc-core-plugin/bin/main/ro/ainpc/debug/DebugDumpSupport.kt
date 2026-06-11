package ro.ainpc.debug

import com.google.gson.JsonElement
import com.google.gson.JsonArray
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.npc.AINPC
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Locale

object DebugDumpSupport {
    @JvmStatic
    fun normalizeQuestStageCompletionMode(completionMode: String?): String {
        return when (val normalized = normalizeQuestStageReference(completionMode)) {
            "", "all", "all_objective", "all_objectives", "allobjective", "allobjectives" -> "all_objectives"
            "any", "any_objective", "any_objectives", "anyobjective", "anyobjectives" -> "any_objective"
            "manual", "manual_turn_in", "manualturnin", "turn_in", "turnin" -> "manual_turn_in"
            else -> normalized
        }
    }

    @JvmStatic
    fun isSupportedQuestStageCompletionMode(completionMode: String?): Boolean {
        return completionMode in setOf("all_objectives", "any_objective", "manual_turn_in")
    }

    @JvmStatic
    fun normalizeQuestStageReference(value: String?): String {
        if (value.isNullOrBlank()) {
            return ""
        }
        return value.trim()
            .lowercase(Locale.ROOT)
            .replace("minecraft:", "")
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
            .replace(Regex("^_+|_+$"), "")
            .replace(Regex("_+"), "_")
    }

    @JvmStatic
    fun normalizeQuestObjectiveType(type: String?): String {
        return when (val normalized = normalizeKey(type).replace('-', '_')) {
            "", "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item"
            "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc"
            "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc"
            "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region"
            "visitplace", "visit_place", "enterplace", "enter_place", "go_to_place", "place" -> "visit_place"
            "inspect", "inspectnode", "inspect_node", "interact_node", "node" -> "inspect_node"
            "kill", "slay", "defeat", "kill_mob" -> "kill_mob"
            else -> normalized
        }
    }

    @JvmStatic
    fun normalizeQuestRewardType(type: String?): String {
        return when (val normalized = normalizeKey(type).replace('-', '_')) {
            "", "item", "reward_item" -> "item"
            "story_state", "set_story_flag", "story_flag", "set_flag", "setstorystate" -> "set_story_state"
            "story_event", "record_event", "event", "recordstoryevent" -> "record_story_event"
            else -> normalized
        }
    }

    @JvmStatic
    fun supportedQuestObjectiveTypes(): Set<String> {
        return setOf(
            "collect_item",
            "deliver_to_npc",
            "talk_to_npc",
            "visit_region",
            "visit_place",
            "inspect_node",
            "kill_mob",
        )
    }

    @JvmStatic
    fun supportedQuestRewardTypes(): Set<String> {
        return setOf(
            "item",
            "set_story_state",
            "record_story_event",
        )
    }

    @JvmStatic
    fun parseStoredJsonObject(rawValue: String?): JsonObject? {
        val safeRawValue = if (rawValue.isNullOrBlank()) "{}" else rawValue
        return try {
            val parsed: JsonElement = JsonParser.parseString(safeRawValue)
            if (parsed.isJsonObject) parsed.asJsonObject else null
        } catch (ignored: JsonSyntaxException) {
            null
        }
    }

    @JvmStatic
    fun addStoredJson(root: JsonObject, key: String, rawValue: String?) {
        val safeRawValue = if (rawValue.isNullOrBlank()) "{}" else rawValue
        try {
            val parsed = JsonParser.parseString(safeRawValue)
            root.add(key, parsed)
        } catch (exception: JsonSyntaxException) {
            root.addProperty("${key}_raw", safeRawValue)
            root.addProperty("${key}_parse_error", exception.message)
        }
    }

    @JvmStatic
    fun jsonString(obj: JsonObject?, key: String?): String {
        if (obj == null || key.isNullOrBlank()) {
            return ""
        }
        val value = obj.get(key)
        return if (value != null && !value.isJsonNull) value.asString.trim() else ""
    }

    @JvmStatic
    fun lastSelectorSegment(selector: String?): String {
        if (selector.isNullOrBlank()) {
            return ""
        }
        val separator = selector.lastIndexOf(':')
        return if (separator >= 0 && separator < selector.length - 1) {
            selector.substring(separator + 1)
        } else {
            selector
        }
    }

    @JvmStatic
    fun semanticAnchorTypeForObjective(normalizedObjectiveType: String?): String {
        return when (normalizedObjectiveType) {
            "talk_to_npc" -> "npc"
            "visit_region" -> "region"
            "visit_place" -> "place"
            "inspect_node" -> "node"
            else -> ""
        }
    }

    @JvmStatic
    fun semanticReferencePrefix(reference: String?): String {
        if (reference.isNullOrBlank()) {
            return ""
        }
        val trimmed = reference.trim()
        val prefixSeparator = trimmed.indexOf(':')
        if (prefixSeparator <= 0) {
            return ""
        }
        val prefix = normalizeKey(trimmed.substring(0, prefixSeparator))
        return if (isKnownSemanticReferencePrefix(prefix)) prefix else ""
    }

    @JvmStatic
    fun semanticReferenceValue(reference: String?): String {
        if (reference.isNullOrBlank()) {
            return ""
        }
        val trimmed = reference.trim()
        val prefixSeparator = trimmed.indexOf(':')
        if (prefixSeparator <= 0) {
            return trimmed
        }
        val prefix = normalizeKey(trimmed.substring(0, prefixSeparator))
        return if (!isKnownSemanticReferencePrefix(prefix)) {
            trimmed
        } else {
            trimmed.substring(prefixSeparator + 1).trim()
        }
    }

    @JvmStatic
    fun isKnownSemanticReferencePrefix(prefix: String?): Boolean {
        return prefix in setOf("npc", "name", "profession", "region", "place", "node", "tag", "type", "mob", "entity")
    }

    @JvmStatic
    fun questTemplateId(scenario: FeaturePackLoader.ScenarioDefinition): String {
        val packId = valueOrEmpty(scenario.packId)
        val scenarioId = valueOrEmpty(scenario.id)
        if (packId.isBlank()) {
            return scenarioId
        }
        if (scenarioId.isBlank()) {
            return packId
        }
        return "$packId:$scenarioId"
    }

    @JvmStatic
    fun questEntryMetadata(entry: FeaturePackLoader.QuestEntryDefinition?, vararg keys: String?): String {
        if (entry == null || keys.isEmpty()) {
            return ""
        }
        val metadata = entry.metadata
        for (key in keys) {
            val value = metadata[key]
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return ""
    }

    @JvmStatic
    fun incrementCount(counts: MutableMap<String, Int>, key: String?) {
        var normalizedKey = valueOrEmpty(key)
        if (normalizedKey.isBlank()) {
            normalizedKey = "unknown"
        }
        counts[normalizedKey] = (counts[normalizedKey] ?: 0) + 1
    }

    @JvmStatic
    fun incrementCountIfPresent(counts: MutableMap<String, Int>, key: String?) {
        if (key.isNullOrBlank()) {
            return
        }
        incrementCount(counts, key)
    }

    @JvmStatic
    fun countMapJson(counts: Map<String, Int>): JsonObject {
        val json = JsonObject()
        for ((key, value) in counts) {
            json.addProperty(key, value)
        }
        return json
    }

    @JvmStatic
    fun enumJsonId(value: Enum<*>?): String = if (value != null) normalizeKey(value.name) else ""

    @JvmStatic
    fun storedJsonProperty(rawValue: String?, key: String?): String {
        if (rawValue.isNullOrBlank() || key.isNullOrBlank()) {
            return ""
        }
        return try {
            val parsed = JsonParser.parseString(rawValue)
            if (!parsed.isJsonObject) {
                return ""
            }
            val value = parsed.asJsonObject.get(key)
            if (value == null || value.isJsonNull) {
                ""
            } else if (value.isJsonPrimitive) {
                value.asString
            } else {
                value.toString()
            }
        } catch (exception: JsonSyntaxException) {
            ""
        } catch (exception: IllegalStateException) {
            ""
        }
    }

    @JvmStatic
    fun hasStoryEventProgressionKey(
        keys: Set<String>,
        playerUuid: String?,
        templateId: String?,
        questCode: String?
    ): Boolean {
        return keys.contains(storyEventProgressionKey(playerUuid, templateId)) ||
            keys.contains(storyEventProgressionKey(playerUuid, questCode)) ||
            keys.contains(storyEventProgressionKey("", templateId)) ||
            keys.contains(storyEventProgressionKey("", questCode))
    }

    @JvmStatic
    fun addStoryEventProgressionKey(keys: MutableSet<String>, playerUuid: String?, selector: String?) {
        val key = storyEventProgressionKey(playerUuid, selector)
        if (key.isNotBlank()) {
            keys.add(key)
        }
    }

    @JvmStatic
    fun storyEventProgressionKey(playerUuid: String?, selector: String?): String {
        val normalizedSelector = normalizeKey(selector)
        if (normalizedSelector.isBlank()) {
            return ""
        }
        return "${valueOrEmpty(playerUuid)}|$normalizedSelector"
    }

    @JvmStatic
    fun addScenarioLookupKey(
        lookup: MutableMap<String, FeaturePackLoader.ScenarioDefinition>,
        key: String?,
        scenario: FeaturePackLoader.ScenarioDefinition
    ) {
        val normalized = normalizeKey(key)
        if (normalized.isNotBlank()) {
            lookup.putIfAbsent(normalized, scenario)
        }
    }

    @JvmStatic
    fun findScenarioForProgressionRow(
        templateId: String?,
        questCode: String?,
        scenariosBySelector: Map<String, FeaturePackLoader.ScenarioDefinition>?
    ): FeaturePackLoader.ScenarioDefinition? {
        if (scenariosBySelector.isNullOrEmpty()) {
            return null
        }

        var scenario = scenariosBySelector[normalizeKey(templateId)]
        if (scenario != null) {
            return scenario
        }

        scenario = scenariosBySelector[normalizeKey(questCode)]
        if (scenario != null) {
            return scenario
        }

        return scenariosBySelector[normalizeKey(lastSelectorSegment(templateId))]
    }

    @JvmStatic
    fun hasRecordStoryEventAction(scenario: FeaturePackLoader.ScenarioDefinition?): Boolean {
        if (scenario == null) {
            return false
        }

        for (reward in scenario.rewards) {
            if (normalizeQuestRewardType(reward.type) == "record_story_event") {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun findRecordStoryEventAction(
        scenario: FeaturePackLoader.ScenarioDefinition?,
        eventKey: String?
    ): FeaturePackLoader.QuestEntryDefinition? {
        if (scenario == null) {
            return null
        }

        var fallback: FeaturePackLoader.QuestEntryDefinition? = null
        val normalizedEventKey = normalizeKey(eventKey)
        for (reward in scenario.rewards) {
            if (normalizeQuestRewardType(reward.type) != "record_story_event") {
                continue
            }
            if (fallback == null) {
                fallback = reward
            }
            val actionEventKey = questEntryMetadata(reward, "event_key", "key")
            if (normalizedEventKey.isNotBlank() && normalizeKey(actionEventKey) == normalizedEventKey) {
                return reward
            }
        }
        return fallback
    }

    @JvmStatic
    fun questStagesJson(
        stages: List<FeaturePackLoader.QuestStageDefinition>?,
        gson: Gson
    ): JsonArray {
        val json = JsonArray()
        if (stages.isNullOrEmpty()) {
            return json
        }

        for (stage in stages) {
            json.add(questStageJson(stage, gson))
        }
        return json
    }

    @JvmStatic
    fun questStageJson(
        stage: FeaturePackLoader.QuestStageDefinition?,
        gson: Gson
    ): JsonObject {
        val json = JsonObject()
        if (stage == null) {
            return json
        }

        json.addProperty("id", valueOrEmpty(stage.id))
        json.addProperty("description", valueOrEmpty(stage.description))
        json.addProperty("completion_mode", valueOrEmpty(stage.completionMode))
        json.addProperty("next_stage", valueOrEmpty(stage.getNextStageId()))
        json.add("objective_ids", gson.toJsonTree(stage.objectiveIds))
        json.add("metadata", gson.toJsonTree(stage.metadata))
        return json
    }

    @JvmStatic
    fun questEntryStage(entry: FeaturePackLoader.QuestEntryDefinition?): String {
        if (entry == null) {
            return ""
        }

        return firstNonBlank(
            entry.metadata["stage_id"],
            entry.metadata["stage"],
            entry.metadata["phase"],
            entry.metadata["current_stage_id"],
            entry.metadata["current_phase"],
            entry.variables["stage_id"],
            entry.variables["stage"],
            entry.variables["phase"],
        )
    }

    @JvmStatic
    fun isQuestRuntimeStage(
        scenario: FeaturePackLoader.ScenarioDefinition?,
        normalizedStageId: String?
    ): Boolean {
        if (scenario == null || normalizedStageId.isNullOrBlank()) {
            return false
        }

        for (stage in scenario.questStages) {
            if (normalizeKey(stage.id) != normalizedStageId) {
                continue
            }
            if (stage.objectiveIds.isNotEmpty()) {
                return true
            }
            for (objective in scenario.objectives) {
                if (normalizeKey(questEntryStage(objective)) == normalizedStageId) {
                    return true
                }
            }
            return false
        }
        return false
    }

    @JvmStatic
    fun collectQuestObjectiveReferences(
        objectives: List<FeaturePackLoader.QuestEntryDefinition>?
    ): Set<String> {
        val references = LinkedHashSet<String>()
        if (objectives == null) {
            return references
        }
        for (objective in objectives) {
            references.add(normalizeQuestStageReference(objective.entryId))
            references.add(normalizeQuestStageReference(objective.itemId))
        }
        references.remove("")
        return references
    }

    @JvmStatic
    fun questStageReferencesObjective(
        scenario: FeaturePackLoader.ScenarioDefinition?,
        objective: FeaturePackLoader.QuestEntryDefinition?
    ): Boolean {
        if (scenario == null || objective == null || scenario.questStages.isEmpty()) {
            return false
        }

        for (stage in scenario.questStages) {
            if (stageReferencesObjective(stage, objective)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun stageReferencesObjective(
        stage: FeaturePackLoader.QuestStageDefinition?,
        objective: FeaturePackLoader.QuestEntryDefinition?
    ): Boolean {
        if (stage == null || objective == null || stage.objectiveIds.isEmpty()) {
            return false
        }

        val entryId = normalizeQuestStageReference(objective.entryId)
        val itemId = normalizeQuestStageReference(objective.itemId)
        for (objectiveId in stage.objectiveIds) {
            val normalizedObjective = normalizeQuestStageReference(objectiveId)
            if (normalizedObjective.isNotBlank() &&
                (normalizedObjective == entryId || normalizedObjective == itemId)
            ) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun isStoredJsonValid(rawValue: String?): Boolean {
        val safeRawValue = if (rawValue.isNullOrBlank()) "{}" else rawValue
        return try {
            JsonParser.parseString(safeRawValue)
            true
        } catch (exception: JsonSyntaxException) {
            false
        }
    }

    @JvmStatic
    fun valueOrEmpty(value: String?): String = value ?: ""

    @JvmStatic
    fun valueOrFallback(value: String?, fallback: String?): String =
        if (value.isNullOrBlank()) valueOrEmpty(fallback) else value

    @JvmStatic
    @Throws(SQLException::class)
    fun nullableLong(resultSet: ResultSet, column: String): Long {
        val value = resultSet.getObject(column)
        return if (value == null) 0L else resultSet.getLong(column)
    }

    @JvmStatic
    fun boundsJson(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): JsonObject {
        val json = JsonObject()
        json.addProperty("min_x", minX)
        json.addProperty("min_y", minY)
        json.addProperty("min_z", minZ)
        json.addProperty("max_x", maxX)
        json.addProperty("max_y", maxY)
        json.addProperty("max_z", maxZ)
        return json
    }

    @JvmStatic
    fun toRegionJson(region: WorldRegionInfo): JsonObject {
        val json = JsonObject()
        json.addProperty("id", region.id())
        json.addProperty("name", region.name())
        json.addProperty("world", region.worldName())
        json.addProperty("type", region.typeId())
        json.add("bounds", boundsJson(region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ()))
        json.add("tags", stringArray(region.tags()))
        json.addProperty("story_mode", region.storyMode().id)
        json.addProperty("story_state", region.storyStateKey())
        return json
    }

    @JvmStatic
    fun toPlaceJson(place: WorldPlaceInfo): JsonObject {
        val json = JsonObject()
        json.addProperty("id", place.id())
        json.addProperty("region_id", place.regionId())
        json.addProperty("display_name", place.displayName())
        json.addProperty("world", place.worldName())
        json.addProperty("type", place.placeType().id)
        json.add("bounds", boundsJson(place.minX(), place.minY(), place.minZ(), place.maxX(), place.maxY(), place.maxZ()))
        json.add("tags", stringArray(place.tags()))
        json.addProperty("owner_npc_id", place.ownerNpcId())
        json.addProperty("public_access", place.publicAccess())
        json.add("metadata", stringMapJson(place.metadata()))
        return json
    }

    @JvmStatic
    fun toNodeJson(node: WorldNodeInfo): JsonObject {
        val json = JsonObject()
        json.addProperty("id", node.id())
        json.addProperty("region_id", node.regionId())
        json.addProperty("place_id", node.placeId())
        json.addProperty("type", node.typeId())
        json.addProperty("world", node.worldName())
        json.addProperty("x", node.x())
        json.addProperty("y", node.y())
        json.addProperty("z", node.z())
        json.addProperty("radius", node.radius())
        json.add("metadata", stringMapJson(node.metadata()))
        return json
    }

    @JvmStatic
    fun worldMappingSemanticIndexJson(index: WorldMappingSemanticIndex?): JsonObject {
        val json = JsonObject()
        if (index == null) {
            return json
        }

        val resolverCandidates = JsonObject()
        resolverCandidates.add("regions", semanticIndexMapJson(index.regionCandidates()))
        resolverCandidates.add("places", semanticIndexMapJson(index.placeCandidates()))
        resolverCandidates.add("nodes", semanticIndexMapJson(index.nodeCandidates()))
        json.add("resolver_candidate_tokens", resolverCandidates)
        json.add("place_tags", semanticIndexMapJson(index.placeTags()))
        json.add("place_types", semanticIndexMapJson(index.placeTypes()))
        json.add("node_types", semanticIndexMapJson(index.nodeTypes()))
        json.add("node_metadata_values", semanticIndexMapJson(index.nodeMetadataValues()))
        return json
    }

    @JvmStatic
    fun semanticIndexMapJson(index: Map<String, List<String>>?): JsonObject {
        val json = JsonObject()
        if (index.isNullOrEmpty()) {
            return json
        }
        for ((key, value) in index) {
            json.add(key, stringArray(value))
        }
        return json
    }

    @JvmStatic
    fun addNpcWorldBindingRoleJson(
        root: JsonObject,
        role: String,
        placeId: String?,
        nodeId: String?,
        placesById: Map<String, WorldPlaceInfo>,
        nodesById: Map<String, WorldNodeInfo>,
    ) {
        val json = JsonObject()
        val safePlaceId = valueOrEmpty(placeId)
        val safeNodeId = valueOrEmpty(nodeId)
        val place = placesById[safePlaceId]
        val node = nodesById[safeNodeId]
        json.addProperty("place_id", safePlaceId)
        json.addProperty("node_id", safeNodeId)
        json.addProperty("place_exists", safePlaceId.isBlank() || place != null)
        json.addProperty("node_exists", safeNodeId.isBlank() || node != null)
        json.addProperty(
            "node_place_matches",
            safePlaceId.isBlank() ||
                safeNodeId.isBlank() ||
                node == null ||
                node.placeId().isBlank() ||
                node.placeId().equals(safePlaceId, ignoreCase = true),
        )
        if (place != null) {
            json.addProperty("place_type", valueOrEmpty(place.placeType().id))
            json.addProperty("place_display_name", valueOrEmpty(place.displayName()))
            json.addProperty("region_id", valueOrEmpty(place.regionId()))
        }
        if (node != null) {
            json.addProperty("node_type", valueOrEmpty(node.typeId()))
            json.addProperty("node_world", valueOrEmpty(node.worldName()))
            json.addProperty("node_x", node.x())
            json.addProperty("node_y", node.y())
            json.addProperty("node_z", node.z())
        }
        root.add(role, json)
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun npcWorldMissingPlaceReferenceCount(
        resultSet: ResultSet,
        placesById: Map<String, WorldPlaceInfo>,
    ): Int {
        return missingReferenceCount(
            placesById,
            resultSet.getString("home_place_id"),
            resultSet.getString("work_place_id"),
            resultSet.getString("social_place_id"),
        )
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun npcWorldMissingNodeReferenceCount(
        resultSet: ResultSet,
        nodesById: Map<String, WorldNodeInfo>,
    ): Int {
        return missingReferenceCount(
            nodesById,
            resultSet.getString("home_node_id"),
            resultSet.getString("work_node_id"),
            resultSet.getString("social_node_id"),
        )
    }

    @JvmStatic
    fun missingReferenceCount(knownReferences: Map<String, *>?, vararg references: String?): Int {
        if (knownReferences == null || knownReferences.isEmpty()) {
            return 0
        }
        var missing = 0
        for (reference in references) {
            val safeReference = valueOrEmpty(reference)
            if (safeReference.isNotBlank() && !knownReferences.containsKey(safeReference)) {
                missing++
            }
        }
        return missing
    }

    @JvmStatic
    fun isWorkplace(place: WorldPlaceInfo): Boolean {
        return place.hasTag("work") ||
            place.hasTag("workplace") ||
            "work".equals(place.metadata()["role"], ignoreCase = true) ||
            "work".equals(place.metadata()["purpose"], ignoreCase = true) ||
            when (place.placeType()) {
                PlaceType.FORGE, PlaceType.SHOP, PlaceType.FARM, PlaceType.MARKET, PlaceType.TAVERN -> true
                else -> false
            }
    }

    @JvmStatic
    fun hasPendingOwner(place: WorldPlaceInfo): Boolean {
        val ownerStatus = place.metadata().getOrDefault("owner_status", "")
        val ownerPending = place.metadata().getOrDefault("owner_pending", "")
        return "pending".equals(ownerStatus, ignoreCase = true) ||
            "true".equals(ownerPending, ignoreCase = true) ||
            ("demo_mapping".equals(place.metadata().getOrDefault("source", ""), ignoreCase = true) && place.hasTag("demo"))
    }

    @JvmStatic
    fun isHousePlace(place: WorldPlaceInfo): Boolean {
        return place.placeType().id == "house" ||
            place.hasTag("home") ||
            place.hasTag("house") ||
            "home".equals(place.metadata()["role"], ignoreCase = true) ||
            "home".equals(place.metadata()["purpose"], ignoreCase = true)
    }

    @JvmStatic
    fun parseResidents(place: WorldPlaceInfo): List<String> {
        val rawResidents = firstNonBlank(
            place.metadata()["residents"],
            place.metadata()["resident_npc_ids"],
            place.metadata()["resident_ids"],
        )
        if (rawResidents.isBlank()) {
            return emptyList()
        }
        return rawResidents.split(Regex("[,;]"))
            .map { part -> part.trim() }
            .filter { resident -> resident.isNotBlank() }
    }

    @JvmStatic
    fun parsePositiveIntMetadata(place: WorldPlaceInfo, vararg keys: String?): Int? {
        val rawValue = firstNonBlankFromMap(place.metadata(), *keys)
        if (rawValue.isBlank()) {
            return null
        }
        return rawValue.trim().toIntOrNull()?.takeIf { value -> value > 0 }
    }

    @JvmStatic
    fun firstNonBlankFromMap(values: Map<String, String>?, vararg keys: String?): String {
        if (values == null) {
            return ""
        }
        for (key in keys) {
            val value = values[key]
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return ""
    }

    @JvmStatic
    fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return ""
    }

    @JvmStatic
    fun hasAnySemanticNode(nodes: Iterable<WorldNodeInfo>, vararg expectedTokens: String?): Boolean {
        for (node in nodes) {
            if (nodeMatchesAny(node, *expectedTokens)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun nodeMatchesAny(node: WorldNodeInfo, vararg expectedTokens: String?): Boolean {
        if (matchesAnyToken(node.typeId(), *expectedTokens)) {
            return true
        }
        for ((key, value) in node.metadata()) {
            if (matchesAnyToken(key, *expectedTokens) || matchesAnyToken(value, *expectedTokens)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun matchesAnyToken(rawValue: String?, vararg expectedTokens: String?): Boolean {
        val value = normalizeKey(rawValue).replace('-', '_')
        if (value.isBlank()) {
            return false
        }
        for (expectedToken in expectedTokens) {
            if (value == normalizeKey(expectedToken).replace('-', '_')) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun ownedLocationInsidePlace(location: AINPC.OwnedLocation?, place: WorldPlaceInfo): Boolean {
        return location != null &&
            place.worldName().equals(location.worldName(), ignoreCase = true) &&
            location.x() >= place.minX() &&
            location.x() <= place.maxX() &&
            location.y() >= place.minY() &&
            location.y() <= place.maxY() &&
            location.z() >= place.minZ() &&
            location.z() <= place.maxZ()
    }

    @JvmStatic
    fun normalizeKey(value: String?): String = value?.trim()?.lowercase()?.replace(' ', '_').orEmpty()

    private fun stringArray(values: Iterable<String>?): JsonArray {
        val json = JsonArray()
        if (values != null) {
            for (value in values) {
                json.add(value)
            }
        }
        return json
    }

    private fun stringMapJson(values: Map<String, String>?): JsonObject {
        val json = JsonObject()
        if (values != null) {
            for ((key, value) in values) {
                json.addProperty(key, value)
            }
        }
        return json
    }
}
