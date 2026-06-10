package ro.ainpc.world.mapping

import ro.ainpc.world.PlaceType
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldPlace
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegion
import ro.ainpc.world.WorldRegionInfo
import java.util.LinkedHashMap
import java.util.Locale
import java.util.UUID
import java.util.function.Predicate

class MappingDraftFactory {
    fun createDraft(
        playerId: UUID?,
        kind: MappingDraftKind,
        selection: MappingWandSelection?,
        description: String?,
        worldAdmin: WorldAdminService?
    ): MappingDraft {
        if (worldAdmin == null || !worldAdmin.isEnabled) {
            throw IllegalArgumentException("World admin este dezactivat.")
        }
        val safeSelection = selection ?: MappingWandSelection.empty()
        val suggestion = MappingIntentParser.suggest(kind, description)
        return when (kind) {
            MappingDraftKind.REGION -> createRegionDraft(playerId, safeSelection, description, worldAdmin, suggestion)
            MappingDraftKind.PLACE -> createPlaceDraft(playerId, safeSelection, description, worldAdmin, suggestion)
            MappingDraftKind.NODE -> createNodeDraft(playerId, safeSelection, description, worldAdmin, suggestion)
            MappingDraftKind.NPC_BIND -> createNpcBindDraft(playerId, safeSelection, description, worldAdmin, suggestion)
            MappingDraftKind.QUEST_ANCHOR -> createQuestAnchorDraft(playerId, safeSelection, description, worldAdmin, suggestion)
        }
    }

    fun apply(draft: MappingDraft?, worldAdmin: WorldAdminService?): MappingDraftApplyResult {
        if (draft == null) {
            throw IllegalArgumentException("Nu exista draft de confirmat.")
        }
        if (worldAdmin == null || !worldAdmin.isEnabled) {
            throw IllegalArgumentException("World admin este dezactivat.")
        }

        return when (draft.kind()) {
            MappingDraftKind.REGION -> {
                val region: WorldRegion = worldAdmin.createRegion(
                    draft.localId(),
                    draft.displayName(),
                    draft.worldName(),
                    RegionType.fromId(draft.typeId()),
                    draft.minX(),
                    draft.minY(),
                    draft.minZ(),
                    draft.maxX(),
                    draft.maxY(),
                    draft.maxZ()
                )
                region.setTags(draft.tags())
                MappingDraftApplyResult(draft.kind(), region.id, "Regiune creata")
            }

            MappingDraftKind.PLACE -> {
                val place: WorldPlace = worldAdmin.createPlace(
                    draft.regionId(),
                    draft.localId(),
                    draft.displayName(),
                    draft.worldName(),
                    PlaceType.fromId(draft.typeId()),
                    draft.minX(),
                    draft.minY(),
                    draft.minZ(),
                    draft.maxX(),
                    draft.maxY(),
                    draft.maxZ()
                )
                place.setTags(draft.tags())
                val metadata = LinkedHashMap(draft.metadata())
                val publicAccessHint = metadata.remove("public_access_hint")
                if (!publicAccessHint.isNullOrBlank()) {
                    place.isPublicAccess = publicAccessHint.toBoolean()
                }
                metadata.forEach { (key, value) -> place.putMetadata(key, value) }
                MappingDraftApplyResult(draft.kind(), place.id, "Place creat")
            }

            MappingDraftKind.NODE -> {
                val node: WorldNode = worldAdmin.createNode(
                    draft.regionId(),
                    draft.placeId(),
                    draft.localId(),
                    WorldNodeType.fromId(draft.typeId()),
                    draft.worldName(),
                    draft.x(),
                    draft.y(),
                    draft.z(),
                    draft.radius()
                )
                draft.metadata().forEach { (key, value) -> node.putMetadata(key, value) }
                MappingDraftApplyResult(draft.kind(), node.id, "Node creat")
            }

            MappingDraftKind.NPC_BIND -> throw IllegalArgumentException(
                "NPC bind se confirma prin comanda, ca sa actualizeze si profilul NPC."
            )

            MappingDraftKind.QUEST_ANCHOR -> throw IllegalArgumentException(
                "Quest anchor se confirma prin comanda, ca sa scrie in quest_anchor_bindings."
            )
        }
    }

    private fun createRegionDraft(
        playerId: UUID?,
        selection: MappingWandSelection,
        description: String?,
        worldAdmin: WorldAdminService,
        suggestion: MappingDraftSuggestion
    ): MappingDraft {
        val bounds = selection.bounds().orElseThrow {
            IllegalArgumentException("Selecteaza pos1 si pos2 cu /ainpc wand inainte de draft region.")
        }
        val localId = uniqueRegionId(worldAdmin, suggestion.localId())
        val command = "/ainpc world region create " + localId + " " + suggestion.typeId() + " " +
            bounds.minX() + " " + bounds.minY() + " " + bounds.minZ() + " " +
            bounds.maxX() + " " + bounds.maxY() + " " + bounds.maxZ()
        return MappingDraft(
            playerId,
            MappingDraftKind.REGION,
            description,
            localId,
            localId,
            suggestion.displayName(),
            suggestion.typeId(),
            localId,
            "",
            bounds.worldName(),
            bounds.minX(),
            bounds.minY(),
            bounds.minZ(),
            bounds.maxX(),
            bounds.maxY(),
            bounds.maxZ(),
            0.0,
            0.0,
            0.0,
            suggestion.radius(),
            suggestion.tags(),
            suggestion.metadata(),
            suggestion.warnings(),
            command
        )
    }

    private fun createPlaceDraft(
        playerId: UUID?,
        selection: MappingWandSelection,
        description: String?,
        worldAdmin: WorldAdminService,
        suggestion: MappingDraftSuggestion
    ): MappingDraft {
        val bounds = selection.bounds().orElseThrow {
            IllegalArgumentException("Selecteaza pos1 si pos2 cu /ainpc wand inainte de draft place.")
        }
        val region = worldAdmin.findRegion(bounds.worldName(), bounds.centerX(), bounds.centerY(), bounds.centerZ())
            ?: throw IllegalArgumentException(
                "Selectia place nu are o regiune parinte la centru. Creeaza intai regiunea."
            )

        val warnings = ArrayList(suggestion.warnings())
        if (!region.contains(bounds.worldName(), bounds.minX(), bounds.minY(), bounds.minZ()) ||
            !region.contains(bounds.worldName(), bounds.maxX(), bounds.maxY(), bounds.maxZ())
        ) {
            warnings.add("Selectia place pare sa iasa din regiunea " + region.id() + "; confirmarea poate fi respinsa.")
        }

        val localId = uniquePlaceId(worldAdmin, region.id(), suggestion.localId())
        val qualifiedId = region.id() + ":" + localId
        val command = "/ainpc world place create " + region.id() + " " + localId + " " + suggestion.typeId() + " " +
            bounds.minX() + " " + bounds.minY() + " " + bounds.minZ() + " " +
            bounds.maxX() + " " + bounds.maxY() + " " + bounds.maxZ()
        return MappingDraft(
            playerId,
            MappingDraftKind.PLACE,
            description,
            localId,
            qualifiedId,
            suggestion.displayName(),
            suggestion.typeId(),
            region.id(),
            "",
            region.worldName(),
            bounds.minX(),
            bounds.minY(),
            bounds.minZ(),
            bounds.maxX(),
            bounds.maxY(),
            bounds.maxZ(),
            0.0,
            0.0,
            0.0,
            suggestion.radius(),
            suggestion.tags(),
            suggestion.metadata(),
            warnings,
            command
        )
    }

    private fun createNodeDraft(
        playerId: UUID?,
        selection: MappingWandSelection,
        description: String?,
        worldAdmin: WorldAdminService,
        suggestion: MappingDraftSuggestion
    ): MappingDraft {
        if (!selection.hasPoint()) {
            throw IllegalArgumentException("Selecteaza un punct cu /ainpc wand point sau click cu wand-ul in modul node.")
        }
        val point = requireNotNull(selection.point())
        val region = worldAdmin.findRegion(point.worldName(), point.x(), point.y(), point.z())
            ?: throw IllegalArgumentException("Punctul node nu are o regiune parinte. Creeaza intai regiunea.")
        val place = worldAdmin.findPlace(point.worldName(), point.x(), point.y(), point.z())
        val placeId = if (place != null && place.regionId().equals(region.id(), ignoreCase = true)) place.id() else ""
        val localId = uniqueNodeId(worldAdmin, region.id(), placeId, suggestion.localId())
        val qualifiedId = if (placeId.isBlank()) region.id() + ":" + localId else placeId + ":" + localId
        val placeSelector = if (placeId.isBlank()) "-" else placeId
        val command = "/ainpc world node create " + region.id() + " " + placeSelector + " " +
            localId + " " + suggestion.typeId() + " " +
            point.x() + " " + point.y() + " " + point.z() + " " +
            String.format(Locale.ROOT, "%.1f", suggestion.radius())
        return MappingDraft(
            playerId,
            MappingDraftKind.NODE,
            description,
            localId,
            qualifiedId,
            suggestion.displayName(),
            suggestion.typeId(),
            region.id(),
            placeId,
            region.worldName(),
            0,
            0,
            0,
            0,
            0,
            0,
            point.x().toDouble(),
            point.y().toDouble(),
            point.z().toDouble(),
            suggestion.radius(),
            suggestion.tags(),
            suggestion.metadata(),
            suggestion.warnings(),
            command
        )
    }

    private fun createNpcBindDraft(
        playerId: UUID?,
        selection: MappingWandSelection,
        description: String?,
        worldAdmin: WorldAdminService,
        suggestion: MappingDraftSuggestion
    ): MappingDraft {
        if (!selection.hasPoint()) {
            throw IllegalArgumentException("Selecteaza punctul din place cu /ainpc wand point sau click in modul npc_bind.")
        }
        val point = requireNotNull(selection.point())
        val region = worldAdmin.findRegion(point.worldName(), point.x(), point.y(), point.z())
            ?: throw IllegalArgumentException("Punctul de bind NPC nu are o regiune parinte.")
        val place = worldAdmin.findPlace(point.worldName(), point.x(), point.y(), point.z())
        if (place == null || !place.regionId().equals(region.id(), ignoreCase = true)) {
            throw IllegalArgumentException("Punctul de bind NPC trebuie sa fie intr-un place mapat.")
        }

        val intent = parseNpcBindIntent(description)
        val warnings = ArrayList(suggestion.warnings())
        warnings.add("Confirmarea actualizeaza profilul NPC, metadata mapping si npc_world_bindings.")
        if (!roleMatchesPlace(intent.role, place)) {
            warnings.add("Place-ul " + place.id() + " nu pare potrivit pentru rolul " + intent.role + ".")
        }

        val metadata = LinkedHashMap(suggestion.metadata())
        metadata["npc_selector"] = intent.npcSelector
        metadata["bind_role"] = intent.role
        metadata["place_id"] = place.id()
        metadata["source"] = "wand_prompt"

        val qualifiedId = intent.npcSelector + "@" + place.id() + ":" + intent.role
        return MappingDraft(
            playerId,
            MappingDraftKind.NPC_BIND,
            description,
            intent.npcSelector,
            qualifiedId,
            "NPC Bind " + intent.role,
            intent.role,
            region.id(),
            place.id(),
            region.worldName(),
            0,
            0,
            0,
            0,
            0,
            0,
            point.x().toDouble(),
            point.y().toDouble(),
            point.z().toDouble(),
            suggestion.radius(),
            suggestion.tags(),
            metadata,
            warnings,
            "/ainpc map confirm"
        )
    }

    private fun createQuestAnchorDraft(
        playerId: UUID?,
        selection: MappingWandSelection,
        description: String?,
        worldAdmin: WorldAdminService,
        suggestion: MappingDraftSuggestion
    ): MappingDraft {
        if (!selection.hasPoint()) {
            throw IllegalArgumentException("Selecteaza punctul de quest anchor cu /ainpc wand point sau click in modul quest_anchor.")
        }
        val point = requireNotNull(selection.point())
        val region = worldAdmin.findRegion(point.worldName(), point.x(), point.y(), point.z())
            ?: throw IllegalArgumentException("Punctul quest anchor nu are o regiune parinte.")

        val node = worldAdmin.findNode(point.worldName(), point.x(), point.y(), point.z())
        var place = worldAdmin.findPlace(point.worldName(), point.x(), point.y(), point.z())
        if (place != null && !place.regionId().equals(region.id(), ignoreCase = true)) {
            place = null
        }

        val anchorType: String
        val anchorId: String
        val anchorLabel: String
        if (node != null) {
            anchorType = "node"
            anchorId = node.id()
            anchorLabel = node.typeId()
        } else if (place != null) {
            anchorType = "place"
            anchorId = place.id()
            anchorLabel = place.displayName()
        } else {
            anchorType = "region"
            anchorId = region.id()
            anchorLabel = region.name()
        }

        val intent = parseQuestAnchorIntent(
            description,
            defaultObjectiveTypeForAnchor(anchorType),
            anchorId
        )
        val warnings = ArrayList(suggestion.warnings())
        warnings.add("Confirmarea scrie/actualizeaza quest_anchor_bindings pentru obiectivul ales.")
        if (!questAnchorTypeCompatible(intent.objectiveType, anchorType)) {
            warnings.add("Tipul obiectivului " + intent.objectiveType + " nu pare compatibil cu ancora " + anchorType + ".")
        }

        val metadata = LinkedHashMap(suggestion.metadata())
        metadata["player_selector"] = intent.playerSelector
        metadata["progression_selector"] = intent.progressionSelector
        metadata["objective_key"] = intent.objectiveKey
        metadata["objective_type"] = intent.objectiveType
        metadata["reference"] = intent.reference
        metadata["anchor_type"] = anchorType
        metadata["anchor_id"] = anchorId
        metadata["anchor_label"] = anchorLabel
        metadata["source"] = "wand_prompt"

        val localId = MappingIntentParser.slugOrFallback(intent.objectiveKey, "quest_anchor")
        val qualifiedId = intent.progressionSelector + ":" + intent.objectiveKey + "@" + anchorType + ":" + anchorId
        return MappingDraft(
            playerId,
            MappingDraftKind.QUEST_ANCHOR,
            description,
            localId,
            qualifiedId,
            "Quest Anchor " + intent.objectiveKey,
            intent.objectiveType,
            region.id(),
            place?.id() ?: "",
            region.worldName(),
            0,
            0,
            0,
            0,
            0,
            0,
            point.x().toDouble(),
            point.y().toDouble(),
            point.z().toDouble(),
            suggestion.radius(),
            suggestion.tags(),
            metadata,
            warnings,
            "/ainpc map confirm"
        )
    }

    private fun uniqueRegionId(worldAdmin: WorldAdminService, baseId: String): String =
        uniqueId(baseId) { candidate -> worldAdmin.getRegion(candidate) == null }

    private fun uniquePlaceId(worldAdmin: WorldAdminService, regionId: String, baseId: String): String =
        uniqueId(baseId) { candidate -> worldAdmin.getPlace("$regionId:$candidate") == null }

    private fun uniqueNodeId(worldAdmin: WorldAdminService, regionId: String, placeId: String?, baseId: String): String {
        val scope = if (placeId.isNullOrBlank()) regionId else placeId
        return uniqueId(baseId) { candidate ->
            val existing: WorldNodeInfo? = worldAdmin.getNode("$scope:$candidate")
            existing == null
        }
    }

    private fun uniqueId(baseId: String, available: Predicate<String>): String {
        val safeBase = MappingIntentParser.slugOrFallback(baseId, "mapping_draft")
        if (available.test(safeBase)) {
            return safeBase
        }
        var index = 2
        while (!available.test("${safeBase}_$index")) {
            index++
        }
        return "${safeBase}_$index"
    }

    private fun parseNpcBindIntent(description: String?): NpcBindIntent {
        val normalized = MappingIntentParser.normalize(description)
        val tokens = if (normalized.isBlank()) emptyList() else normalized.split(Regex("\\s+"))
        var role = "home"
        var npcSelector = ""
        for (token in tokens) {
            val resolvedRole = resolveBindRole(token)
            if (resolvedRole.isNotBlank()) {
                role = resolvedRole
                continue
            }
            if (isNpcBindFiller(token)) {
                continue
            }
            if (npcSelector.isBlank()) {
                npcSelector = token
            }
        }
        if (npcSelector.isBlank()) {
            npcSelector = "nearest"
        }
        return NpcBindIntent(npcSelector, role)
    }

    private fun resolveBindRole(token: String?): String {
        return when (token?.lowercase(Locale.ROOT) ?: "") {
            "home", "casa", "acasa", "locuinta", "resident", "residence" -> "home"
            "work", "lucru", "munca", "job", "workplace", "atelier" -> "work"
            "social", "piata", "taverna", "meeting", "adunare" -> "social"
            else -> ""
        }
    }

    private fun isNpcBindFiller(token: String?): Boolean {
        return when (token?.lowercase(Locale.ROOT) ?: "") {
            "npc", "bind", "leaga", "legare", "la", "in", "pe", "pentru", "place", "loc" -> true
            else -> false
        }
    }

    private fun roleMatchesPlace(role: String, place: WorldPlaceInfo): Boolean {
        return when (role) {
            "home" -> "house".equals(place.placeType().id, ignoreCase = true) || place.hasTag("home")
            "work" -> place.hasTag("work") || place.hasTag("workplace") ||
                when (place.placeType()) {
                    PlaceType.FORGE, PlaceType.SHOP, PlaceType.FARM, PlaceType.TAVERN, PlaceType.CAMP -> true
                    else -> false
                }

            "social" -> place.hasTag("social") || place.hasTag("public") ||
                when (place.placeType()) {
                    PlaceType.MARKET, PlaceType.TAVERN, PlaceType.CAMP -> true
                    else -> false
                }

            else -> true
        }
    }

    private fun parseQuestAnchorIntent(
        description: String?,
        defaultObjectiveType: String,
        defaultReference: String?
    ): QuestAnchorIntent {
        val safeDescription = description?.trim().orEmpty()
        val tokens = if (safeDescription.isBlank()) emptyList() else safeDescription.split(Regex("\\s+"))
        var index = 0
        var playerSelector = "self"
        if (tokens.isNotEmpty()) {
            val playerToken = stripKeyPrefix(tokens[0], "player", "jucator")
            if (playerToken.isNotBlank()) {
                playerSelector = playerToken
                index = 1
            }
        }

        if (index >= tokens.size) {
            throw IllegalArgumentException(
                "Utilizare: /ainpc map quest_anchor [player:<jucator|uuid>] <tracked|current|templateId|questCode> <objective_id> [objective_type] [reference]"
            )
        }
        val progressionSelector = stripKeyPrefix(tokens[index++], "progression", "quest", "template")
        if (progressionSelector.isBlank()) {
            throw IllegalArgumentException("Specifica progresia pentru quest_anchor.")
        }
        if (index >= tokens.size) {
            throw IllegalArgumentException("Specifica objective_id pentru quest_anchor.")
        }

        val objectiveKey = stripKeyPrefix(tokens[index++], "objective", "objective_id", "obiectiv")
        if (objectiveKey.isBlank()) {
            throw IllegalArgumentException("objective_id pentru quest_anchor este gol.")
        }

        var objectiveType = defaultObjectiveTypeForAnchor(defaultObjectiveType)
        var reference = defaultReference?.trim().orEmpty()
        if (index < tokens.size) {
            val maybeType = normalizeQuestAnchorObjectiveType(tokens[index])
            if (maybeType.isNotBlank()) {
                objectiveType = maybeType
                index++
            }
        }
        if (index < tokens.size) {
            reference = joinTokens(tokens, index)
        }

        return QuestAnchorIntent(
            playerSelector,
            progressionSelector,
            objectiveKey,
            objectiveType,
            reference
        )
    }

    private fun stripKeyPrefix(token: String?, vararg prefixes: String): String {
        if (token.isNullOrBlank()) {
            return ""
        }
        val trimmed = token.trim()
        var separator = trimmed.indexOf(':')
        if (separator <= 0 || separator >= trimmed.length - 1) {
            separator = trimmed.indexOf('=')
        }
        if (separator <= 0 || separator >= trimmed.length - 1) {
            return trimmed
        }
        val prefix = trimmed.substring(0, separator).lowercase(Locale.ROOT)
        for (expected in prefixes) {
            if (prefix == expected.lowercase(Locale.ROOT)) {
                return trimmed.substring(separator + 1).trim()
            }
        }
        return trimmed
    }

    private fun normalizeQuestAnchorObjectiveType(token: String?): String {
        val normalized = MappingIntentParser.normalize(token).replace('-', '_')
        return when (normalized) {
            "visit_region", "region", "enter_region" -> "visit_region"
            "visit_place", "place", "enter_place", "go_to_place" -> "visit_place"
            "inspect_node", "node", "inspect", "interact_node" -> "inspect_node"
            "talk_to_npc", "npc", "talk", "speak_to_npc" -> "talk_to_npc"
            else -> ""
        }
    }

    private fun defaultObjectiveTypeForAnchor(anchorType: String?): String {
        return when (anchorType?.lowercase(Locale.ROOT) ?: "") {
            "region", "visit_region" -> "visit_region"
            "place", "visit_place" -> "visit_place"
            "node", "inspect_node" -> "inspect_node"
            "npc", "talk_to_npc" -> "talk_to_npc"
            else -> "inspect_node"
        }
    }

    private fun questAnchorTypeCompatible(objectiveType: String?, anchorType: String?): Boolean {
        val objective = normalizeQuestAnchorObjectiveType(objectiveType)
        val anchor = anchorType?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (objective.isBlank() || anchor.isBlank()) {
            return true
        }
        return when (objective) {
            "visit_region" -> "region" == anchor
            "visit_place" -> "place" == anchor
            "inspect_node" -> "node" == anchor
            "talk_to_npc" -> "npc" == anchor
            else -> true
        }
    }

    private fun joinTokens(tokens: List<String>, startIndex: Int): String {
        val safeStart = startIndex.coerceAtLeast(0)
        return if (safeStart >= tokens.size) "" else tokens.subList(safeStart, tokens.size).joinToString(" ")
    }

    private data class NpcBindIntent(val npcSelector: String, val role: String)

    private data class QuestAnchorIntent(
        val playerSelector: String,
        val progressionSelector: String,
        val objectiveKey: String,
        val objectiveType: String,
        val reference: String
    )
}
