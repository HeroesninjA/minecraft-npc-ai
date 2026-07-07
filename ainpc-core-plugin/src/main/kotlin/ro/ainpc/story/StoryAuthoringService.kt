package ro.ainpc.story

import com.google.gson.Gson
import ro.ainpc.AINPCPlugin
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.sql.SQLException
import java.util.logging.Level

class StoryAuthoringService(private val plugin: AINPCPlugin) {
    private val storyStateService: StoryStateService get() = plugin.storyStateService
    private val worldAdmin get() = plugin.platform.worldAdmin
    private val reactionService: StoryReactionService get() = plugin.storyReactionService

    data class StoryEventDraft(
        val scopeType: String,
        val scopeId: String,
        val regionId: String = "",
        val placeId: String = "",
        val eventType: String,
        val eventKey: String = "",
        val title: String = "",
        val description: String = "",
        val payload: Map<String, String> = emptyMap(),
        val actorType: String = "system",
        val actorId: String = "authoring",
        val playerUuid: String = "",
        val npcId: String = ""
    )

    data class StoryEventInfo(
        val id: Long,
        val scopeType: String,
        val scopeId: String,
        val eventType: String,
        val title: String,
        val description: String,
        val createdAt: Long
    )

    fun recordEvent(draft: StoryEventDraft): Boolean {
        return try {
            val sql = """
                INSERT INTO story_events 
                (scope_type, scope_id, region_id, place_id, event_type, event_key, 
                 title, description, payload, actor_type, actor_id, 
                 player_uuid, npc_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, draft.scopeType)
                stmt.setString(2, draft.scopeId)
                stmt.setString(3, draft.regionId)
                stmt.setString(4, draft.placeId)
                stmt.setString(5, draft.eventType)
                stmt.setString(6, draft.eventKey)
                stmt.setString(7, draft.title)
                stmt.setString(8, draft.description)
                stmt.setString(9, Gson().toJson(draft.payload))
                stmt.setString(10, draft.actorType)
                stmt.setString(11, draft.actorId)
                stmt.setString(12, draft.playerUuid)
                stmt.setString(13, draft.npcId)
                stmt.setLong(14, System.currentTimeMillis())
                stmt.executeUpdate()
            }
            reactionService.reactToEvent(draft.eventType, draft.scopeId, draft.regionId)
            plugin.platform.addonRegistry.dispatchStoryEvent(draft.eventType, draft.scopeId, draft.title)
            true
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la salvarea evenimentului story", e)
            false
        }
    }

    fun listRecentEvents(scopeType: String, scopeId: String, limit: Int = 20): List<StoryEventInfo> {
        return try {
            val sql = """
                SELECT id, scope_type, scope_id, event_type, title, description, created_at
                FROM story_events
                WHERE scope_type = ? AND scope_id = ?
                ORDER BY created_at DESC
                LIMIT ?
            """.trimIndent()
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, scopeType)
                stmt.setString(2, scopeId)
                stmt.setInt(3, limit.coerceIn(1, 100))
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<StoryEventInfo>()
                    while (rs.next()) {
                        results.add(StoryEventInfo(
                            id = rs.getLong("id"),
                            scopeType = rs.getString("scope_type"),
                            scopeId = rs.getString("scope_id"),
                            eventType = rs.getString("event_type"),
                            title = rs.getString("title") ?: "",
                            description = rs.getString("description") ?: "",
                            createdAt = rs.getLong("created_at")
                        ))
                    }
                    results
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la listarea evenimentelor story", e)
            emptyList()
        }
    }

    fun listAllRegions(): Collection<WorldRegionInfo> = worldAdmin.regions

    fun listPlacesInRegion(regionId: String): Collection<WorldPlaceInfo> = worldAdmin.getPlaces(regionId)

    fun setRegionStoryMode(regionId: String, mode: String): Boolean {
        return try {
            val storyMode = StoryMode.fromId(mode)
            storyStateService.saveRegionState(
                regionId = regionId,
                storyMode = storyMode,
                stateKey = null,
                storyPool = null,
                variables = null,
                updatedBy = "authoring",
                source = "StoryAuthoringService"
            )
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Eroare la setarea modului story pentru regiunea $regionId", e)
            false
        }
    }

    fun getRegionStoryState(regionId: String): RegionStoryState? {
        return try {
            storyStateService.getRegionState(regionId)
        } catch (e: Exception) {
            null
        }
    }

    fun getAvailableEventTypes(): List<String> = listOf(
        "story_progress", "story_turn", "story_event",
        "npc_event", "quest_event", "world_event",
        "player_action", "environmental", "ritual",
        "celebration", "disaster", "discovery",
        "trade", "diplomacy", "conflict", "resolution"
    )

    fun getAvailableModes(): List<String> = listOf(
        "evolutive", "static", "rotative"
    )

    data class StoryTemplate(
        val id: String,
        val name: String,
        val description: String,
        val eventType: String,
        val suggestedTitle: String,
        val suggestedDescription: String,
        val payload: Map<String, String> = emptyMap()
    )

    fun getTemplates(): List<StoryTemplate> = listOf(
        StoryTemplate("village_celebration", "Sărbătoare în Sat",
            "O sărbătoare are loc în sat. NPC-urile se adună și sărbătoresc.",
            "celebration", "Sărbătoare", "Satul sărbătorește un eveniment important.",
            mapOf("mood" to "festive", "participants" to "all")),
        StoryTemplate("merchant_arrival", "Sosire Negustor",
            "Un negustor important sosește în sat cu marfă nouă.",
            "trade", "Negustorul a sosit", "Un negustor cu mărfuri rare a ajuns în sat.",
            mapOf("trader" to "itinerant", "goods" to "rare")),
        StoryTemplate("raider_attack", "Atac al Jefuitorilor",
            "Jefuitorii atacă satul. NPC-urile intră în panică.",
            "conflict", "Jefuitorii atacă!", "Jefuitorii au fost zăriți la marginea satului!",
            mapOf("threat" to "high", "type" to "raid")),
        StoryTemplate("natural_disaster", "Dezastru Natural",
            "O furtună puternică sau un cutremur lovește zona.",
            "disaster", "Dezastru natural", "Forțele naturii se dezlănțuie asupra regiunii.",
            mapOf("severity" to "high", "evacuation" to "recommended")),
        StoryTemplate("ritual_ceremony", "Ceremonie Rituală",
            "Un ritual străvechi este celebrat de săteni.",
            "ritual", "Ceremonie la altar", "Bătrânii satului conduc o ceremonie străveche.",
            mapOf("type" to "ancient", "participants" to "elders")),
        StoryTemplate("diplomatic_visit", "Vizită Diplomatică",
            "O delegație dintr-un alt sat/facțiune vizitează zona.",
            "diplomacy", "Sosirea delegației", "O delegație străină sosește pentru tratative.",
            mapOf("faction" to "unknown", "intent" to "negotiate")),
        StoryTemplate("discovery", "Descoperire",
            "Ceva valoros sau interesant a fost descoperit în zonă.",
            "discovery", "Descoperire importantă", "O descoperire remarcabilă a fost făcută în regiune.",
            mapOf("type" to "artifact", "value" to "high")),
        StoryTemplate("seasonal_festival", "Festival Sezonier",
            "Festivalul specific anotimpului curent are loc.",
            "celebration", "Festival de anotimp", "Satul celebrează schimbarea anotimpului.",
            mapOf("seasonal" to "true", "type" to "festival")),
        StoryTemplate("hero_return", "Întoarcerea Eroului",
            "Un erou local se întoarce dintr-o misiune importantă.",
            "story_progress", "Întoarcerea eroului", "Un erou al satului se întoarce după o lungă călătorie.",
            mapOf("honor" to "high", "celebration" to "auto")),
        StoryTemplate("dark_omen", "Prevestire Întunecată",
            "Semne misterioase apar în zonă, prevestind evenimente sumbre.",
            "story_event", "Prevestire întunecată", "Semne ciudate apar pe cer și în păduri.",
            mapOf("mood" to "ominous", "type" to "supernatural")),
    )

    fun applyTemplate(templateId: String, scopeId: String, overrides: Map<String, String> = emptyMap()): Boolean {
        val template = getTemplates().find { it.id == templateId } ?: return false
        val scopeType = if (plugin.platform.worldAdmin.getRegion(scopeId) != null) "region" else "place"
        val resolvedTitle = overrides["title"] ?: "${template.suggestedTitle} @ $scopeId"
        val resolvedDesc = overrides["description"] ?: template.suggestedDescription
        val mergedPayload = template.payload + overrides.filterKeys { it != "title" && it != "description" }

        val draft = StoryEventDraft(
            scopeType = scopeType,
            scopeId = scopeId,
            regionId = if (scopeType == "region") scopeId else "",
            placeId = if (scopeType == "place") scopeId else "",
            eventType = template.eventType,
            eventKey = "template_${templateId}_${System.currentTimeMillis()}",
            title = resolvedTitle,
            description = resolvedDesc,
            payload = mergedPayload,
            actorType = "system",
            actorId = "template:$templateId"
        )
        return recordEvent(draft)
    }
}
