package ro.ainpc.debug

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.progression.ProgressionDefinition
import java.sql.ResultSet
import java.sql.SQLException

object DebugDumpStoryEventJson {
    @JvmStatic
    fun buildStoryEventsJson(plugin: AINPCPlugin, gson: Gson): JsonObject {
        val root = JsonObject()
        root.addProperty("source_table", "story_events")
        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            root.addProperty("available", false)
            root.addProperty("error", "DatabaseManager indisponibil")
            root.addProperty("row_count", 0)
            root.add("rows", JsonArray())
            return root
        }

        root.addProperty("available", true)
        val rows = JsonArray()
        val byEventType = LinkedHashMap<String, Int>()
        val byScopeType = LinkedHashMap<String, Int>()
        val byQuestTemplate = LinkedHashMap<String, Int>()
        val byQuestCode = LinkedHashMap<String, Int>()
        val byProgressionLink = LinkedHashMap<String, Int>()
        val linkIndex = buildStoryProgressionLinkIndex(plugin)

        val sql = """
            SELECT id, scope_type, scope_id, region_id, place_id, event_type, event_key,
                   title, description, payload, actor_type, actor_id, player_uuid,
                   npc_id, created_at
            FROM story_events
            ORDER BY created_at DESC, id DESC
        """.trimIndent()

        try {
            databaseManager.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val payload = resultSet.getString("payload")
                        val playerUuid = resultSet.getString("player_uuid")
                        val eventKey = resultSet.getString("event_key")
                        val row = storyEventRowJson(resultSet, payload)
                        val progressionMatch = findStoryProgressionMatch(
                            linkIndex,
                            playerUuid,
                            payload,
                            eventKey,
                        )
                        if (progressionMatch != null) {
                            row.add("progression_link", storyProgressionLinkJson(progressionMatch, eventKey, gson))
                            DebugDumpSupport.incrementCount(byProgressionLink, "linked")
                        } else {
                            DebugDumpSupport.incrementCount(byProgressionLink, "unlinked")
                        }
                        rows.add(row)
                        DebugDumpSupport.incrementCount(byEventType, resultSet.getString("event_type"))
                        DebugDumpSupport.incrementCount(byScopeType, resultSet.getString("scope_type"))
                        DebugDumpSupport.incrementCountIfPresent(
                            byQuestTemplate,
                            DebugDumpSupport.storedJsonProperty(payload, "quest_template"),
                        )
                        DebugDumpSupport.incrementCountIfPresent(
                            byQuestCode,
                            DebugDumpSupport.storedJsonProperty(payload, "quest_code"),
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message)
        }

        root.addProperty("row_count", rows.size())
        root.addProperty("progression_cross_link_available", linkIndex.available())
        root.addProperty("progression_cross_link_source_rows", linkIndex.sourceRows())
        if (linkIndex.error().isNotBlank()) {
            root.addProperty("progression_cross_link_error", linkIndex.error())
        }
        root.add("by_event_type", DebugDumpSupport.countMapJson(byEventType))
        root.add("by_scope_type", DebugDumpSupport.countMapJson(byScopeType))
        root.add("by_quest_template", DebugDumpSupport.countMapJson(byQuestTemplate))
        root.add("by_quest_code", DebugDumpSupport.countMapJson(byQuestCode))
        root.add("by_progression_link", DebugDumpSupport.countMapJson(byProgressionLink))
        root.add("rows", rows)
        return root
    }

    private fun storyEventRowJson(resultSet: ResultSet, payload: String?): JsonObject {
        val json = JsonObject()
        json.addProperty("id", resultSet.getLong("id"))
        json.addProperty("scope_type", DebugDumpSupport.valueOrEmpty(resultSet.getString("scope_type")))
        json.addProperty("scope_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("scope_id")))
        json.addProperty("region_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("region_id")))
        json.addProperty("place_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("place_id")))
        json.addProperty("event_type", DebugDumpSupport.valueOrEmpty(resultSet.getString("event_type")))
        json.addProperty("event_key", DebugDumpSupport.valueOrEmpty(resultSet.getString("event_key")))
        json.addProperty("title", DebugDumpSupport.valueOrEmpty(resultSet.getString("title")))
        json.addProperty("description", DebugDumpSupport.valueOrEmpty(resultSet.getString("description")))
        json.addProperty("actor_type", DebugDumpSupport.valueOrEmpty(resultSet.getString("actor_type")))
        json.addProperty("actor_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("actor_id")))
        json.addProperty("player_uuid", DebugDumpSupport.valueOrEmpty(resultSet.getString("player_uuid")))
        json.addProperty("npc_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("npc_id")))
        json.addProperty("created_at", resultSet.getLong("created_at"))
        DebugDumpSupport.addStoredJson(json, "payload", payload)
        return json
    }

    private fun buildStoryProgressionLinkIndex(plugin: AINPCPlugin): StoryProgressionLinkIndex {
        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
            ?: return StoryProgressionLinkIndex(false, "DatabaseManager indisponibil", 0, emptyMap())

        val scenariosBySelector = buildProgressionScenarioLookup(plugin)
        val linksBySelector = LinkedHashMap<String, MutableList<StoryProgressionLink>>()
        var sourceRows = 0
        val sql = """
            SELECT player_uuid, template_id, quest_code, status, started_at, completed_at,
                   updated_at, tracked
            FROM player_quests
            ORDER BY updated_at DESC, completed_at DESC, started_at DESC
            LIMIT 2000
        """.trimIndent()

        try {
            databaseManager.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        sourceRows++
                        val playerUuid = DebugDumpSupport.valueOrEmpty(resultSet.getString("player_uuid"))
                        val templateId = DebugDumpSupport.valueOrEmpty(resultSet.getString("template_id"))
                        val questCode = DebugDumpSupport.valueOrEmpty(resultSet.getString("quest_code"))
                        val scenario = DebugDumpSupport.findScenarioForProgressionRow(
                            templateId,
                            questCode,
                            scenariosBySelector,
                        )
                        val link = StoryProgressionLink(
                            playerUuid,
                            templateId,
                            questCode,
                            DebugDumpSupport.valueOrEmpty(resultSet.getString("status")),
                            resultSet.getLong("started_at"),
                            resultSet.getLong("completed_at"),
                            resultSet.getLong("updated_at"),
                            resultSet.getInt("tracked") != 0,
                            scenario,
                        )

                        addStoryProgressionLink(linksBySelector, playerUuid, templateId, link)
                        addStoryProgressionLink(linksBySelector, playerUuid, questCode, link)
                        addStoryProgressionLink(
                            linksBySelector,
                            playerUuid,
                            DebugDumpSupport.lastSelectorSegment(templateId),
                            link,
                        )
                        addStoryProgressionLink(linksBySelector, "", templateId, link)
                        addStoryProgressionLink(linksBySelector, "", questCode, link)
                        addStoryProgressionLink(
                            linksBySelector,
                            "",
                            DebugDumpSupport.lastSelectorSegment(templateId),
                            link,
                        )
                    }
                }
            }
            return StoryProgressionLinkIndex(true, "", sourceRows, linksBySelector)
        } catch (exception: SQLException) {
            return StoryProgressionLinkIndex(false, exception.message, sourceRows, linksBySelector)
        }
    }

    private fun addStoryProgressionLink(
        linksBySelector: MutableMap<String, MutableList<StoryProgressionLink>>,
        playerUuid: String?,
        selector: String?,
        link: StoryProgressionLink,
    ) {
        val key = DebugDumpSupport.storyEventProgressionKey(playerUuid, selector)
        if (key.isBlank()) {
            return
        }
        val links = linksBySelector.getOrPut(key) { ArrayList() }
        if (!links.contains(link)) {
            links.add(link)
        }
    }

    private fun findStoryProgressionMatch(
        index: StoryProgressionLinkIndex?,
        eventPlayerUuid: String?,
        rawPayload: String?,
        eventKey: String?,
    ): StoryProgressionMatch? {
        if (index == null || index.linksBySelector().isEmpty()) {
            return null
        }

        val payload = DebugDumpSupport.parseStoredJsonObject(rawPayload)
        val effectivePlayerUuid = DebugDumpSupport.firstNonBlank(
            DebugDumpSupport.valueOrEmpty(eventPlayerUuid),
            DebugDumpSupport.jsonString(payload, "player_uuid"),
        )
        val selectors = ArrayList<StoryProgressionSelector>()
        addStoryProgressionSelector(
            selectors,
            "payload.quest_template",
            DebugDumpSupport.jsonString(payload, "quest_template"),
        )
        addStoryProgressionSelector(
            selectors,
            "payload.quest_code",
            DebugDumpSupport.jsonString(payload, "quest_code"),
        )
        addStoryProgressionSelector(selectors, "event_key", eventKey)

        for (selector in selectors) {
            val exactMatch = findUniqueStoryProgressionMatch(index, effectivePlayerUuid, selector, false)
            if (exactMatch != null) {
                return exactMatch
            }
        }

        if (effectivePlayerUuid.isNotBlank()) {
            return null
        }

        for (selector in selectors) {
            val selectorOnlyMatch = findUniqueStoryProgressionMatch(index, "", selector, true)
            if (selectorOnlyMatch != null) {
                return selectorOnlyMatch
            }
        }
        return null
    }

    private fun addStoryProgressionSelector(
        selectors: MutableList<StoryProgressionSelector>,
        source: String,
        selector: String?,
    ) {
        val normalizedSelector = DebugDumpSupport.normalizeKey(selector)
        if (normalizedSelector.isBlank()) {
            return
        }
        for (existing in selectors) {
            if (DebugDumpSupport.normalizeKey(existing.selector()) == normalizedSelector) {
                return
            }
        }
        selectors.add(StoryProgressionSelector(source, selector))
    }

    private fun findUniqueStoryProgressionMatch(
        index: StoryProgressionLinkIndex,
        playerUuid: String?,
        selector: StoryProgressionSelector,
        selectorOnly: Boolean,
    ): StoryProgressionMatch? {
        val key = DebugDumpSupport.storyEventProgressionKey(playerUuid, selector.selector())
        val links = index.linksBySelector()[key]
        if (links == null || links.size != 1) {
            return null
        }
        return StoryProgressionMatch(
            links[0],
            selector.source(),
            selector.selector(),
            selectorOnly,
            links.size,
        )
    }

    private fun storyProgressionLinkJson(match: StoryProgressionMatch, eventKey: String?, gson: Gson): JsonObject {
        val json = JsonObject()
        val link = match.link()
        json.addProperty("match_source", match.matchSource())
        json.addProperty("match_selector", DebugDumpSupport.valueOrEmpty(match.matchSelector()))
        json.addProperty("selector_only_match", match.selectorOnlyMatch())
        json.addProperty("candidate_count", match.candidateCount())
        json.addProperty("player_uuid", DebugDumpSupport.valueOrEmpty(link.playerUuid()))
        json.addProperty("template_id", DebugDumpSupport.valueOrEmpty(link.templateId()))
        json.addProperty("quest_code", DebugDumpSupport.valueOrEmpty(link.questCode()))
        json.addProperty("status", DebugDumpSupport.valueOrEmpty(link.status()))
        json.addProperty("started_at", link.startedAt())
        json.addProperty("completed_at", link.completedAt())
        json.addProperty("updated_at", link.updatedAt())
        json.addProperty("tracked", link.tracked())

        val scenario = link.scenario()
        if (scenario != null) {
            json.addProperty("scenario_pack_id", DebugDumpSupport.valueOrEmpty(scenario.packId))
            json.addProperty("scenario_id", DebugDumpSupport.valueOrEmpty(scenario.id))
            json.addProperty("scenario_name", DebugDumpSupport.valueOrEmpty(scenario.name))
            json.addProperty("scenario_base_type", scenario.baseType.name)
            json.addProperty("scenario_mechanic_id", DebugDumpSupport.valueOrEmpty(scenario.progressionMechanicId))
            json.addProperty("scenario_kind", DebugDumpSupport.valueOrEmpty(scenario.questScenarioKind))
            val action = DebugDumpSupport.findRecordStoryEventAction(scenario, eventKey)
            if (action != null) {
                json.add("story_action", storyActionJson(action, link, gson))
            }
        }
        return json
    }

    private fun storyActionJson(
        action: FeaturePackLoader.QuestEntryDefinition,
        link: StoryProgressionLink,
        gson: Gson,
    ): JsonObject {
        val json = JsonObject()
        json.addProperty("type", DebugDumpSupport.normalizeQuestRewardType(action.type))
        json.addProperty("entry_id", DebugDumpSupport.valueOrEmpty(action.entryId))
        json.addProperty("item_id", DebugDumpSupport.valueOrEmpty(action.itemId))
        json.addProperty("description", DebugDumpSupport.valueOrEmpty(action.description))
        json.addProperty("scope", DebugDumpSupport.questEntryMetadata(action, "scope", "scope_type"))
        json.addProperty(
            "target",
            DebugDumpSupport.questEntryMetadata(
                action,
                "target",
                "scope_id",
                "target_id",
                "id",
                "place_id",
                "region_id",
                "target_place",
                "target_region",
                "place",
                "region",
            ),
        )
        json.addProperty("event_type", DebugDumpSupport.questEntryMetadata(action, "event_type", "type_id"))
        json.addProperty(
            "event_key",
            DebugDumpSupport.firstNonBlank(
                DebugDumpSupport.questEntryMetadata(action, "event_key", "key"),
                link.questCode(),
            ),
        )
        json.add("metadata", gson.toJsonTree(action.metadata))
        json.add("payload", gson.toJsonTree(action.payload))
        return json
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
            DebugDumpSupport.addScenarioLookupKey(
                lookup,
                definition.packId() + ":" + definition.definitionId(),
                scenario
            )
            DebugDumpSupport.addScenarioLookupKey(
                lookup,
                definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId(),
                scenario,
            )
        }
        return lookup
    }
}
