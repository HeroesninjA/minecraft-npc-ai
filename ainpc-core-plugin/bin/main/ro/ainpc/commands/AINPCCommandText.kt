@file:JvmName("AINPCCommandText")

package ro.ainpc.commands

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import org.bukkit.Location
import ro.ainpc.npc.AINPC
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.progression.StoredProgression
import ro.ainpc.story.StoryEvent
import ro.ainpc.world.patch.PatchCandidate
import ro.ainpc.world.patch.PatchPlan
import ro.ainpc.world.patch.VillageGap
import ro.ainpc.world.mapping.MappingDraftKind
import ro.ainpc.world.mapping.MappingPoint
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldRegionInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.PlaceType
import ro.ainpc.world.NpcWorldBinding
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.ScenarioEngine
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.HashSet
import java.util.Locale

private val STORY_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun jsonString(element: JsonElement?, key: String?): String {
    if (element == null || !element.isJsonObject || key.isNullOrBlank()) {
        return ""
    }
    val value = element.asJsonObject.get(key)
    return if (value != null && !value.isJsonNull) value.asString.trim() else ""
}

fun routeDirectCommandToQuest(args: Array<String>): Array<String> {
    val routedArgs = Array(args.size + 1) { "" }
    routedArgs[0] = "quest"
    System.arraycopy(args, 0, routedArgs, 1, args.size)
    return routedArgs
}

fun routeSubcommandToQuest(args: Array<String>): Array<String> {
    val routedArgs = args.clone()
    routedArgs[0] = "quest"
    return routedArgs
}

fun compactUuid(uuid: String?): String {
    if (uuid.isNullOrBlank()) {
        return "<nesetat>"
    }
    val safeUuid = uuid.trim()
    return if (safeUuid.length > 8) safeUuid.substring(0, 8) else safeUuid
}

fun safeAuditValue(value: String?): String = value?.trim() ?: ""

fun normalizeAuditKey(value: String?): String =
    value?.trim()?.lowercase(Locale.getDefault())?.replace(' ', '_') ?: ""

fun questReferencePrefix(reference: String?): String {
    if (reference.isNullOrBlank()) {
        return ""
    }

    val trimmed = reference.trim()
    val separator = trimmed.indexOf(':')
    if (separator <= 0) {
        return ""
    }
    return normalizeAuditKey(trimmed.substring(0, separator))
}

fun normalizeStoryActionScope(scope: String?): String =
    when (val normalized = normalizeAuditKey(scope).replace('-', '_')) {
        "region", "world_region", "village", "settlement" -> "region"
        "place", "world_place", "location" -> "place"
        else -> normalized
    }

fun hasAnyMetadata(metadata: Map<String, String>?, vararg keys: String?): Boolean =
    hasAnyMapEntry(metadata, *keys)

fun hasAnyMapEntry(values: Map<String, String>?, vararg keys: String?): Boolean {
    if (values.isNullOrEmpty()) {
        return false
    }
    for (key in keys) {
        if (key != null && !values.getOrDefault(key, "").isBlank()) {
            return true
        }
    }
    return false
}

fun equalsIgnoreCase(left: String?, right: String?): Boolean =
    left != null && right != null && left.trim().equals(right.trim(), ignoreCase = true)

fun sameNonBlankIgnoreCase(left: String?, right: String?): Boolean =
    left != null &&
        right != null &&
        left.isNotBlank() &&
        right.isNotBlank() &&
        left.trim().equals(right.trim(), ignoreCase = true)

fun sameOptionalId(left: String?, right: String?): Boolean =
    normalizeAuditKey(left) == normalizeAuditKey(right)

fun isNoneSelector(value: String?): Boolean =
    value.isNullOrBlank() ||
        value == "-" ||
        value.equals("none", ignoreCase = true) ||
        value.equals("null", ignoreCase = true)

fun firstNonBlankFromMap(values: Map<String, String>, vararg keys: String): String {
    for (key in keys) {
        val value = values[key]
        if (!value.isNullOrBlank()) {
            return value
        }
    }
    return ""
}

fun firstNonBlank(vararg values: String?): String {
    for (value in values) {
        if (!value.isNullOrBlank()) {
            return value
        }
    }
    return ""
}

fun formatBounds(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): String =
    "$minX,$minY,$minZ -> $maxX,$maxY,$maxZ"

fun formatList(values: Collection<String>?): String =
    if (values.isNullOrEmpty()) "<gol>" else values.joinToString(", ")

fun formatMap(values: Map<String, String>?): String {
    if (values.isNullOrEmpty()) {
        return "<gol>"
    }

    return values.entries
        .sortedBy { it.key }
        .joinToString(", ") { "${it.key}=${it.value}" }
        .ifBlank { "<gol>" }
}

fun formatCountMap(values: Map<String, Int>?): String {
    if (values.isNullOrEmpty()) {
        return "<gol>"
    }

    return values.entries
        .sortedBy { it.key }
        .joinToString(", ") { "${it.key}=${it.value}" }
        .ifBlank { "<gol>" }
}

fun formatOptional(value: String?): String =
    if (value.isNullOrBlank()) "<nesetat>" else value

fun formatOnOff(enabled: Boolean): String =
    if (enabled) "pornit" else "oprit"

fun formatLocation(loc: Location?): String {
    if (loc == null) {
        return "necunoscuta"
    }
    return String.format("%s (%.1f, %.1f, %.1f)", loc.world.name, loc.x, loc.y, loc.z)
}

fun formatDistance(current: Location?, target: Location?): String {
    if (current == null || target == null || current.world == null || target.world == null) {
        return "necunoscuta"
    }
    if (current.world != target.world) {
        return "alta lume"
    }
    return String.format("%.1f blocuri", Math.sqrt(current.distanceSquared(target)))
}

fun formatMappingPoint(point: MappingPoint?): String =
    point?.format() ?: "<nesetat>"

fun joinArgs(args: Array<String>, startIndex: Int): String =
    args.drop(maxOf(0, startIndex)).joinToString(" ")

fun parsePatchProfessionList(rawValue: String?): List<String> {
    if (rawValue.isNullOrBlank() || rawValue == "-") {
        return emptyList()
    }
    return rawValue.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toList()
}

fun formatListOrNone(values: List<String>?): String =
    if (values.isNullOrEmpty()) "-" else values.joinToString(", ")

fun formatVillageGap(gap: VillageGap): String =
    gap.type().toString() +
        " x" + gap.amount() +
        (if (gap.targetPlaceId().isBlank()) "" else " place=" + gap.targetPlaceId()) +
        (if (gap.reference().isBlank()) "" else " ref=" + gap.reference()) +
        " severity=" + gap.severity() +
        " - " + gap.reason()

fun formatPatchCandidate(candidate: PatchCandidate): String =
    candidate.candidateId() +
        " " + candidate.patchType().id() +
        " priority=" + candidate.priority() +
        " cost=" + candidate.cost() +
        " risk=" + candidate.risk()

fun formatPatchPlan(plan: PatchPlan): String =
    plan.patchId() +
        " " + plan.type().id() +
        " mode=" + plan.buildMode().id() +
        " status=" + plan.validationStatus() +
        " places=" + formatListOrNone(plan.plannedPlaces()) +
        " nodes=" + formatListOrNone(plan.plannedNodes()) +
        (if (plan.errors().isEmpty()) "" else " errors=" + plan.errors().joinToString("; "))

fun formatStoryEvent(event: StoryEvent): String {
    val eventKey = if (event.eventKey().isBlank()) "<no-key>" else event.eventKey()
    val title = if (event.title().isBlank()) "" else " - " + event.title()
    val actor = if (event.actorType().isBlank() && event.actorId().isBlank()) {
        ""
    } else {
        " actor=" + formatOptional(event.actorType()) + ":" + formatOptional(event.actorId())
    }
    return "#" + event.id() +
        " " + formatStoryTime(event.createdAt()) +
        " " + event.scopeType() + ":" + event.scopeId() +
        " " + event.eventType() + "/" + eventKey +
        title +
        actor
}

fun formatStoryMetadata(metadata: Map<String, String>?): String {
    if (metadata.isNullOrEmpty()) {
        return "<gol>"
    }

    val storyMetadata = mutableMapOf<String, String>()
    for ((rawKey, value) in metadata) {
        val key = rawKey.lowercase(Locale.getDefault())
        if (
            key.contains("story") ||
            key.contains("event") ||
            key.contains("state") ||
            key.contains("tension") ||
            key.contains("danger") ||
            key.contains("conflict")
        ) {
            storyMetadata[rawKey] = value
        }
    }
    return formatMap(storyMetadata)
}

fun inferRegionIdFromPlaceId(placeId: String?): String {
    if (placeId.isNullOrBlank() || !placeId.contains(":")) {
        return ""
    }
    return placeId.substring(0, placeId.indexOf(':'))
}

fun formatStoryTime(epochMillis: Long): String {
    if (epochMillis <= 0L) {
        return "<necunoscut>"
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        .format(STORY_TIME_FORMAT)
}

fun auditNpcLabel(npc: AINPC): String {
    val name = if (npc.name.isBlank()) "<fara nume>" else npc.name
    return "NPC $name (id=${npc.databaseId})"
}

fun sanitizeForChat(message: String?): String {
    if (message.isNullOrBlank()) {
        return "<gol>"
    }
    return message
        .replace("&", "")
        .replace('\n', ' ')
        .replace('\r', ' ')
        .trim()
}

fun formatWandSelectionPart(rawPart: String): String =
    when (rawPart.lowercase(Locale.ROOT)) {
        "pos1" -> "pos1"
        "pos2" -> "pos2"
        "punct" -> "point"
        else -> "point"
    }

fun formatMappingDraftKind(kind: MappingDraftKind?): String =
    kind?.id() ?: "<nesetat>"

fun isAuditOptionSupported(mode: String, option: String?): Boolean =
    option.isNullOrBlank() || ((mode == "quest" || mode == "all") && isStrictQuestAuditOption(option))

fun isStrictQuestAuditOption(option: String): Boolean =
    option == "strict" || option == "full" || option == "offline"

fun auditModeLabel(mode: String, option: String?): String =
    if (option.isNullOrBlank()) mode else "$mode $option"

fun progressionAliasLogFilter(filter: String?, kind: String): String =
    when (normalizeQuestLogFilter(filter)) {
        "current" -> kind + "_current"
        "active" -> kind + "_active"
        "offered" -> kind + "_offered"
        "tracked" -> kind + "_tracked"
        "completed" -> kind + "_completed"
        "failed" -> kind + "_failed"
        "archived" -> kind + "_archived"
        else -> kind
    }

fun isHelpMode(value: String?): Boolean =
    when (value?.trim()?.lowercase(Locale.ROOT)) {
        "help", "usage", "ajutor", "?" -> true
        else -> false
    }

fun isQuestLogFilter(value: String?): Boolean =
    normalizeQuestLogFilter(value).isNotBlank()

fun normalizeQuestLogFilter(value: String?): String =
    when (value?.trim()?.lowercase(Locale.getDefault()) ?: "") {
        "all", "toate" -> "all"
        "current", "curent", "curente" -> "current"
        "active", "activ", "activeaza" -> "active"
        "offered", "oferit", "oferite" -> "offered"
        "tracked", "urmarit" -> "tracked"
        "quest", "questuri" -> "quest"
        "contract", "contracts", "contracte" -> "contract"
        "duty", "duties", "sarcina", "sarcini" -> "duty"
        "bounty", "bounties", "recompensa", "recompense" -> "bounty"
        "event", "events", "eveniment", "evenimente" -> "event"
        "tutorial", "tutorials", "onboarding", "indrumare" -> "tutorial"
        "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> "ritual"
        "contract_current", "contract_curent", "contracte_curente" -> "contract_current"
        "contract_active", "contract_activ", "contracte_active" -> "contract_active"
        "contract_offered", "contract_oferit", "contracte_oferite" -> "contract_offered"
        "contract_tracked", "contract_urmarit", "contracte_urmarite" -> "contract_tracked"
        "contract_completed", "contract_completat", "contracte_completate" -> "contract_completed"
        "contract_failed", "contract_esuat", "contracte_esuate" -> "contract_failed"
        "contract_archived", "contract_arhivat", "contracte_arhivate" -> "contract_archived"
        "duty_current", "duty_curent", "sarcini_curente" -> "duty_current"
        "duty_active", "duty_activ", "sarcini_active" -> "duty_active"
        "duty_offered", "duty_oferit", "sarcini_oferite" -> "duty_offered"
        "duty_tracked", "duty_urmarit", "sarcini_urmarite" -> "duty_tracked"
        "duty_completed", "duty_completat", "sarcini_completate" -> "duty_completed"
        "duty_failed", "duty_esuat", "sarcini_esuate" -> "duty_failed"
        "duty_archived", "duty_arhivat", "sarcini_arhivate" -> "duty_archived"
        "bounty_current", "bounty_curent", "recompense_curente" -> "bounty_current"
        "bounty_active", "bounty_activ", "recompense_active" -> "bounty_active"
        "bounty_offered", "bounty_oferit", "recompense_oferite" -> "bounty_offered"
        "bounty_tracked", "bounty_urmarit", "recompense_urmarite" -> "bounty_tracked"
        "bounty_completed", "bounty_completat", "recompense_completate" -> "bounty_completed"
        "bounty_failed", "bounty_esuat", "recompense_esuate" -> "bounty_failed"
        "bounty_archived", "bounty_arhivat", "recompense_arhivate" -> "bounty_archived"
        "event_current", "event_curent", "evenimente_curente" -> "event_current"
        "event_active", "event_activ", "evenimente_active" -> "event_active"
        "event_offered", "event_oferit", "evenimente_oferite" -> "event_offered"
        "event_tracked", "event_urmarit", "evenimente_urmarite" -> "event_tracked"
        "event_completed", "event_completat", "evenimente_completate" -> "event_completed"
        "event_failed", "event_esuat", "evenimente_esuate" -> "event_failed"
        "event_archived", "event_arhivat", "evenimente_arhivate" -> "event_archived"
        "tutorial_current", "tutorial_curent", "tutoriale_curente" -> "tutorial_current"
        "tutorial_active", "tutorial_activ", "tutoriale_active" -> "tutorial_active"
        "tutorial_offered", "tutorial_oferit", "tutoriale_oferite" -> "tutorial_offered"
        "tutorial_tracked", "tutorial_urmarit", "tutoriale_urmarite" -> "tutorial_tracked"
        "tutorial_completed", "tutorial_completat", "tutoriale_completate" -> "tutorial_completed"
        "tutorial_failed", "tutorial_esuat", "tutoriale_esuate" -> "tutorial_failed"
        "tutorial_archived", "tutorial_arhivat", "tutoriale_arhivate" -> "tutorial_archived"
        "ritual_current", "ritual_curent", "ritualuri_curente" -> "ritual_current"
        "ritual_active", "ritual_activ", "ritualuri_active" -> "ritual_active"
        "ritual_offered", "ritual_oferit", "ritualuri_oferite" -> "ritual_offered"
        "ritual_tracked", "ritual_urmarit", "ritualuri_urmarite" -> "ritual_tracked"
        "ritual_completed", "ritual_completat", "ritualuri_completate" -> "ritual_completed"
        "ritual_failed", "ritual_esuat", "ritualuri_esuate" -> "ritual_failed"
        "ritual_archived", "ritual_arhivat", "ritualuri_arhivate" -> "ritual_archived"
        "main", "principal" -> "main"
        "side", "secundar", "secundare" -> "side"
        "repeatable", "repetabil", "repetabile" -> "repeatable"
        "completed", "complete", "completat", "finalizat", "finalizate" -> "completed"
        "failed", "esuat", "abandonat", "abandonate" -> "failed"
        "archived", "archive", "arhivat", "arhivate" -> "archived"
        else -> ""
    }

fun isQuestAcceptMode(mode: String?): Boolean =
    when (mode?.lowercase(Locale.ROOT) ?: "") {
        "accept", "yes", "y", "da", "ok", "confirm" -> true
        else -> false
    }

fun isQuestDeclineMode(mode: String?): Boolean =
    when (mode?.lowercase(Locale.ROOT) ?: "") {
        "decline", "deny", "reject", "no", "n", "nu", "refuz" -> true
        else -> false
    }

fun commandLabelForKind(progressionKind: String?): String =
    when (normalizeProgressionKind(progressionKind)) {
        "contract" -> "contract"
        "duty" -> "duty"
        "bounty" -> "bounty"
        "event" -> "event"
        "tutorial" -> "tutorial"
        "ritual" -> "ritual"
        else -> "quest"
    }

fun normalizeProgressionKind(progressionKind: String?): String =
    progressionKind?.trim()?.lowercase(Locale.ROOT) ?: ""

fun formatBatchNpcIdList(rawValue: String?): String {
    if (rawValue.isNullOrBlank()) {
        return "-"
    }
    val tokens = rawValue.split(',')
    val values = tokens
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(3)
    if (values.isEmpty()) {
        return "-"
    }
    var formatted = values.joinToString(",")
    val total = tokens.count { it.isNotBlank() }
    if (total > values.size) {
        formatted += ",+" + (total - values.size)
    }
    return shortenBatchValue(formatted, 72)
}

fun sameNearbyLocation(left: Location?, right: Location?, maxDistanceSquared: Double): Boolean {
    if (left == null || right == null || left.world == null || right.world == null) {
        return false
    }
    return left.world == right.world && left.distanceSquared(right) <= maxDistanceSquared
}

fun formatNpcIdentities(npcs: List<AINPC>): String =
    npcs.map { formatNpcIdentity(it) }.toString()

fun formatNpcIdentity(npc: AINPC): String =
    npc.name + "#" + npc.databaseId

fun nodeLabel(node: WorldNodeInfo, fallbackLabel: String?): String {
    val explicitLabel = firstNonBlank(
        node.metadata()["label"],
        node.metadata()["name"],
        node.metadata()["display_name"]
    )
    if (explicitLabel.isNotBlank()) {
        return explicitLabel
    }
    return if (fallbackLabel.isNullOrBlank()) node.id() else fallbackLabel
}

fun npcBindingId(npc: AINPC): String {
    if (npc.databaseId > 0) {
        return "npc_" + npc.databaseId
    }
    return npc.uuid.toString()
}

fun distanceSquaredToPlaceCenter(place: WorldPlaceInfo, node: WorldNodeInfo): Double =
    distanceSquared(placeCenterX(place), placeAnchorY(place), placeCenterZ(place), node.x(), node.y(), node.z())

fun distanceSquared(
    leftX: Double,
    leftY: Double,
    leftZ: Double,
    rightX: Double,
    rightY: Double,
    rightZ: Double
): Double {
    val dx = leftX - rightX
    val dy = leftY - rightY
    val dz = leftZ - rightZ
    return dx * dx + dy * dy + dz * dz
}

fun placeCenterX(place: WorldPlaceInfo): Double =
    (place.minX() + place.maxX()) / 2.0

fun placeAnchorY(place: WorldPlaceInfo): Double =
    kotlin.math.min(place.maxY().toDouble(), place.minY() + 1.0)

fun placeCenterZ(place: WorldPlaceInfo): Double =
    (place.minZ() + place.maxZ()) / 2.0

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
        entry.variables["phase"]
    )
}

fun normalizeQuestStageCompletionMode(completionMode: String?): String =
    when (val normalized = normalizeQuestStageReference(completionMode)) {
        "", "all", "all_objective", "all_objectives", "allobjective", "allobjectives" -> "all_objectives"
        "any", "any_objective", "any_objectives", "anyobjective", "anyobjectives" -> "any_objective"
        "manual", "manual_turn_in", "manualturnin", "turn_in", "turnin" -> "manual_turn_in"
        else -> normalized
    }

fun isSupportedQuestStageCompletionMode(completionMode: String?): Boolean =
    completionMode in setOf("all_objectives", "any_objective", "manual_turn_in")

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

fun semanticAnchorTypeForObjective(objectiveType: String?): String =
    when (normalizeQuestObjectiveType(objectiveType)) {
        "visit_region" -> "region"
        "visit_place" -> "place"
        "inspect_node" -> "node"
        "talk_to_npc" -> "npc"
        else -> ""
    }

fun questEntryId(entry: FeaturePackLoader.QuestEntryDefinition?): String {
    if (entry == null) {
        return "<null>"
    }
    val entryId = entry.metadata.getOrDefault("entry_id", "")
    if (entryId.isNotBlank()) {
        return entryId
    }
    return entry.type + ":" + entry.itemId
}

fun isLegacyObjectiveProgressKey(key: String?): Boolean {
    if (key.isNullOrBlank()) {
        return false
    }
    val parts = key.split(':')
    if (parts.size < 3) {
        return false
    }
    val type = normalizeQuestObjectiveType(parts[0])
    if (!isSupportedQuestObjectiveType(type)) {
        return false
    }
    return try {
        parts[parts.size - 1].toInt() >= 0
    } catch (_: NumberFormatException) {
        false
    }
}

fun isSupportedQuestObjectiveType(type: String?): Boolean =
    when (normalizeQuestObjectiveType(type)) {
        "collect_item", "deliver_to_npc", "talk_to_npc", "visit_region", "visit_place", "inspect_node", "kill_mob" -> true
        else -> false
    }

fun normalizeQuestObjectiveType(type: String?): String =
    when (val normalized = normalizeAuditKey(type)) {
        "", "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item"
        "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc"
        "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc"
        "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region"
        "visitplace", "visit_place", "enterplace", "enter_place", "go_to_place", "place" -> "visit_place"
        "inspect", "inspectnode", "inspect_node", "interact_node", "node" -> "inspect_node"
        "kill", "slay", "defeat", "kill_mob" -> "kill_mob"
        else -> normalized
    }

fun normalizeQuestRewardType(type: String?): String =
    when (val normalized = normalizeAuditKey(type)) {
        "", "item", "reward_item" -> "item"
        "set_story_state", "record_story_event" -> normalized
        else -> normalized
    }

fun formatObjectiveCandidates(candidates: List<String>?): String =
    if (candidates.isNullOrEmpty()) "." else ". Obiective valide: " + candidates.joinToString(", ") + "."

fun lastSelectorSegment(selector: String?): String {
    val safeSelector = selector?.trim() ?: ""
    val separator = safeSelector.lastIndexOf(':')
    if (separator < 0 || separator >= safeSelector.length - 1) {
        return safeSelector
    }
    return safeSelector.substring(separator + 1)
}

fun isWorkplace(place: WorldPlaceInfo): Boolean =
    place.hasTag("work") ||
        place.hasTag("workplace") ||
        "work".equals(place.metadata()["role"], ignoreCase = true) ||
        "work".equals(place.metadata()["purpose"], ignoreCase = true) ||
        place.placeType() == PlaceType.FORGE ||
        place.placeType() == PlaceType.SHOP ||
        place.placeType() == PlaceType.FARM ||
        place.placeType() == PlaceType.MARKET ||
        place.placeType() == PlaceType.TAVERN

fun isSocialPlace(place: WorldPlaceInfo): Boolean =
    place.hasTag("social") ||
        place.hasTag("public") ||
        "social".equals(place.metadata()["role"], ignoreCase = true) ||
        "social".equals(place.metadata()["purpose"], ignoreCase = true) ||
        place.placeType() == PlaceType.MARKET ||
        place.placeType() == PlaceType.TAVERN ||
        place.placeType() == PlaceType.CAMP

fun hasPendingOwner(place: WorldPlaceInfo): Boolean {
    val ownerStatus = place.metadata().getOrDefault("owner_status", "")
    val ownerPending = place.metadata().getOrDefault("owner_pending", "")
    return "pending".equals(ownerStatus, ignoreCase = true) ||
        "true".equals(ownerPending, ignoreCase = true) ||
        ("demo_mapping".equals(place.metadata().getOrDefault("source", ""), ignoreCase = true) && place.hasTag("demo"))
}

fun placeInsideRegion(place: WorldPlaceInfo, region: WorldRegionInfo): Boolean =
    place.worldName().equals(region.worldName(), ignoreCase = true) &&
        place.minX() >= region.minX() &&
        place.maxX() <= region.maxX() &&
        place.minY() >= region.minY() &&
        place.maxY() <= region.maxY() &&
        place.minZ() >= region.minZ() &&
        place.maxZ() <= region.maxZ()

fun pointInsidePlace(node: WorldNodeInfo, place: WorldPlaceInfo): Boolean =
    node.worldName().equals(place.worldName(), ignoreCase = true) &&
        node.x() >= place.minX() &&
        node.x() <= place.maxX() &&
        node.y() >= place.minY() &&
        node.y() <= place.maxY() &&
        node.z() >= place.minZ() &&
        node.z() <= place.maxZ()

fun pointInsideRegion(node: WorldNodeInfo, region: WorldRegionInfo): Boolean =
    node.worldName().equals(region.worldName(), ignoreCase = true) &&
        node.x() >= region.minX() &&
        node.x() <= region.maxX() &&
        node.y() >= region.minY() &&
        node.y() <= region.maxY() &&
        node.z() >= region.minZ() &&
        node.z() <= region.maxZ()

fun placesIntersect(left: WorldPlaceInfo, right: WorldPlaceInfo): Boolean =
    left.worldName().equals(right.worldName(), ignoreCase = true) &&
        overlaps(left.minX(), left.maxX(), right.minX(), right.maxX()) &&
        overlaps(left.minY(), left.maxY(), right.minY(), right.maxY()) &&
        overlaps(left.minZ(), left.maxZ(), right.minZ(), right.maxZ())

fun isHousePlace(place: WorldPlaceInfo): Boolean =
    place.placeType() == PlaceType.HOUSE ||
        place.hasTag("home") ||
        place.hasTag("house") ||
        "home".equals(place.metadata()["role"], ignoreCase = true) ||
        "home".equals(place.metadata()["purpose"], ignoreCase = true)

fun parseResidents(place: WorldPlaceInfo): List<String> {
    val rawResidents = firstNonBlank(
        place.metadata()["residents"],
        place.metadata()["resident_npc_ids"],
        place.metadata()["resident_ids"]
    )
    if (rawResidents.isBlank()) {
        return emptyList()
    }
    return rawResidents.split(Regex("[,;]"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

fun parsePositiveIntMetadata(place: WorldPlaceInfo, vararg keys: String): Int? {
    val rawValue = firstNonBlankFromMap(place.metadata(), *keys)
    if (rawValue.isBlank()) {
        return null
    }
    return try {
        val value = rawValue.trim().toInt()
        if (value > 0) value else null
    } catch (_: NumberFormatException) {
        null
    }
}

fun hasAnySemanticNode(nodes: Collection<WorldNodeInfo>, vararg expectedTokens: String): Boolean =
    nodes.any { nodeMatchesAny(it, *expectedTokens) }

fun nodeMatchesAny(node: WorldNodeInfo, vararg expectedTokens: String): Boolean {
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

fun formatQuestAnchorBinding(row: QuestAnchorBindingRow): String =
    row.playerUuid() + " | " + row.templateId() +
        " | " + row.objectiveKey() +
        " | " + row.objectiveType() +
        " -> " + row.anchorType() + ":" + row.anchorId() +
        " (" + formatOptional(row.anchorLabel()) + ")" +
        " | status=" + formatOptional(row.status())

fun generatedQuestObjectiveKey(objective: FeaturePackLoader.QuestEntryDefinition, index: Int): String {
    val entryId = objective.entryId
    if (!entryId.isNullOrBlank()) {
        return entryId
    }
    val type = normalizeQuestStageReference(firstNonBlank(objective.type, "objective"))
    val itemId = normalizeQuestStageReference(firstNonBlank(objective.itemId, "entry"))
    return "$type:$itemId:$index"
}

fun displayQuestObjectiveKey(objective: FeaturePackLoader.QuestEntryDefinition): String {
    val entryId = objective.entryId
    return if (!entryId.isNullOrBlank()) entryId else questEntryId(objective)
}

fun normalizeQuestObjectiveLookupKey(value: String?): String {
    if (value.isNullOrBlank()) {
        return ""
    }
    return value.trim()
        .lowercase(Locale.ROOT)
        .replace("minecraft:", "")
        .replace(Regex("[^\\p{L}\\p{Nd}:]+"), "_")
        .replace(Regex("^_+|_+$"), "")
        .replace(Regex("_+"), "_")
}

fun storedProgressionMatchesDefinition(
    progression: StoredProgression,
    definition: ProgressionDefinition
): Boolean =
    sameNonBlankIgnoreCase(progression.templateId(), definition.templateId()) ||
        sameNonBlankIgnoreCase(progression.code(), definition.code()) ||
        sameNonBlankIgnoreCase(progression.progressionId(), definition.progressionId()) ||
        (sameNonBlankIgnoreCase(progression.packId(), definition.packId()) &&
            sameNonBlankIgnoreCase(progression.definitionId(), definition.definitionId())) ||
        sameNonBlankIgnoreCase(progression.definitionId(), definition.definitionId())

fun storedProgressionMatchesSelector(row: StoredProgression, selector: String?): Boolean {
    val normalized = selector?.trim() ?: ""
    return equalsIgnoreCase(row.templateId(), normalized) ||
        equalsIgnoreCase(row.code(), normalized) ||
        equalsIgnoreCase(row.progressionId(), normalized) ||
        equalsIgnoreCase(row.definitionId(), normalized) ||
        equalsIgnoreCase(row.mechanicId() + ":" + row.definitionId(), normalized) ||
        equalsIgnoreCase(row.packId() + ":" + row.definitionId(), normalized)
}

fun parseNpcIdSelector(selector: String?): Int? {
    if (selector.isNullOrBlank()) {
        return null
    }
    var normalized = normalizeAuditKey(selector)
    if (normalized.startsWith("npc_")) {
        normalized = normalized.substring("npc_".length)
    }
    return parseIntegerStrict(normalized)
}

fun bindingReferencesAnyPlace(binding: NpcWorldBinding?, placeIds: Set<String?>?): Boolean {
    if (binding == null || placeIds.isNullOrEmpty()) {
        return false
    }
    for (placeId in placeIds) {
        if (placeId.isNullOrBlank()) {
            continue
        }
        if (placeId.equals(binding.homePlaceId(), ignoreCase = true) ||
            placeId.equals(binding.workPlaceId(), ignoreCase = true) ||
            placeId.equals(binding.socialPlaceId(), ignoreCase = true)
        ) {
            return true
        }
    }
    return false
}

fun sameMappingBinding(left: NpcWorldBinding, right: NpcWorldBinding): Boolean =
    sameOptionalId(left.homePlaceId(), right.homePlaceId()) &&
        sameOptionalId(left.workPlaceId(), right.workPlaceId()) &&
        sameOptionalId(left.socialPlaceId(), right.socialPlaceId()) &&
        sameOptionalId(left.homeNodeId(), right.homeNodeId()) &&
        sameOptionalId(left.workNodeId(), right.workNodeId()) &&
        sameOptionalId(left.socialNodeId(), right.socialNodeId())

fun formatNpcWorldBindingPlaces(binding: NpcWorldBinding): String =
    "home=" + formatOptional(binding.homePlaceId()) +
        " work=" + formatOptional(binding.workPlaceId()) +
        " social=" + formatOptional(binding.socialPlaceId()) +
        " nodes=" + formatOptional(binding.homeNodeId()) +
        "/" + formatOptional(binding.workNodeId()) +
        "/" + formatOptional(binding.socialNodeId())

fun metadataListContains(place: WorldPlaceInfo, key: String, expected: String?): Boolean {
    val normalizedExpected = normalizeAuditKey(expected)
    if (normalizedExpected.isBlank()) {
        return false
    }
    val raw = place.metadata().getOrDefault(key, "")
    if (raw.isBlank()) {
        return false
    }
    for (part in raw.split(Regex("[,;]"))) {
        if (normalizeAuditKey(part) == normalizedExpected) {
            return true
        }
    }
    return false
}

fun formatOwnedLocation(location: AINPC.OwnedLocation?): String {
    if (location == null) {
        return "<nesetat>"
    }
    return location.label() + " [" + location.type() + "] " +
        location.worldName() + " " +
        String.format("%.1f, %.1f, %.1f", location.x(), location.y(), location.z())
}

fun hasStoryEventProgressionKey(keys: Set<String>, playerUuid: String?, templateId: String?, questCode: String?): Boolean =
    keys.contains(storyEventProgressionKey(playerUuid, templateId)) ||
        keys.contains(storyEventProgressionKey(playerUuid, questCode)) ||
        keys.contains(storyEventProgressionKey("", templateId)) ||
        keys.contains(storyEventProgressionKey("", questCode))

fun addStoryEventProgressionKey(keys: MutableSet<String>, playerUuid: String?, selector: String?) {
    val key = storyEventProgressionKey(playerUuid, selector)
    if (key.isNotBlank()) {
        keys.add(key)
    }
}

fun storyEventProgressionKey(playerUuid: String?, selector: String?): String {
    val normalizedSelector = normalizeQuestObjectiveLookupKey(selector)
    if (normalizedSelector.isBlank()) {
        return ""
    }
    return safeAuditValue(playerUuid) + "|" + normalizedSelector
}

fun hasRecordStoryEventAction(scenario: FeaturePackLoader.ScenarioDefinition?): Boolean {
    if (scenario == null) {
        return false
    }
    for (reward in scenario.rewards) {
        if ("record_story_event" == normalizeQuestRewardType(reward.type)) {
            return true
        }
    }
    return false
}

fun isQuestAuditCandidate(scenario: FeaturePackLoader.ScenarioDefinition?): Boolean =
    scenario != null &&
        (scenario.baseType == ScenarioEngine.ScenarioType.QUEST ||
            (scenario.isProgressionEnabled &&
                (scenario.questCode.isNotBlank() ||
                    scenario.objectives.isNotEmpty() ||
                    scenario.rewards.isNotEmpty())))

fun collectKnownQuestReferences(quests: List<FeaturePackLoader.ScenarioDefinition>): Set<String> {
    val references = HashSet<String>()
    for (quest in quests) {
        addQuestReference(references, quest.id)
        addQuestReference(references, quest.packId + ":" + quest.id)
        addQuestReference(references, quest.questCode)
    }
    return references
}

fun addQuestReference(references: MutableSet<String>, value: String?) {
    val normalized = normalizeAuditKey(value)
    if (normalized.isNotBlank()) {
        references.add(normalized)
    }
}

fun isQuestAnchorTypeCompatible(objectiveType: String?, anchorType: String?): Boolean {
    val objective = normalizeAuditKey(objectiveType)
    val anchor = normalizeAuditKey(anchorType)
    if (objective.isBlank() || anchor.isBlank()) {
        return true
    }
    return when (objective) {
        "visit_region" -> anchor == "region"
        "visit_place" -> anchor == "place"
        "inspect_node" -> anchor == "node"
        "talk_to_npc" -> anchor == "npc"
        else -> true
    }
}

fun ownedLocationInsidePlace(location: AINPC.OwnedLocation?, place: WorldPlaceInfo): Boolean =
    location != null &&
        place.worldName().equals(location.worldName(), ignoreCase = true) &&
        location.x() >= place.minX() &&
        location.x() <= place.maxX() &&
        location.y() >= place.minY() &&
        location.y() <= place.maxY() &&
        location.z() >= place.minZ() &&
        location.z() <= place.maxZ()

fun isNpcBindingRepairTarget(value: String?): Boolean =
    "npc-bindings".equals(value, ignoreCase = true) ||
        "npc_bindings".equals(value, ignoreCase = true) ||
        "world-bindings".equals(value, ignoreCase = true) ||
        "world_bindings".equals(value, ignoreCase = true)

fun isMappingMetadataRepairTarget(value: String?): Boolean =
    "mapping-metadata".equals(value, ignoreCase = true) ||
        "mapping_metadata".equals(value, ignoreCase = true) ||
        "metadata-mapping".equals(value, ignoreCase = true) ||
        "metadata_mapping".equals(value, ignoreCase = true)

fun isRepairBatchTarget(value: String?): Boolean =
    "batch".equals(value, ignoreCase = true) ||
        "spawn-batch".equals(value, ignoreCase = true) ||
        "spawn_batch".equals(value, ignoreCase = true)

fun isRepairBatchListAction(value: String?): Boolean =
    "list".equals(value, ignoreCase = true) ||
        "recent".equals(value, ignoreCase = true) ||
        "history".equals(value, ignoreCase = true)

fun isQuestRuntimeStage(quest: FeaturePackLoader.ScenarioDefinition?, normalizedStageId: String?): Boolean {
    if (quest == null || normalizedStageId.isNullOrBlank()) {
        return false
    }
    for (stage in quest.questStages) {
        if (normalizeAuditKey(stage.id) != normalizedStageId) {
            continue
        }
        if (stage.objectiveIds.isNotEmpty()) {
            return true
        }
        return quest.objectives
            .map { questEntryStage(it) }
            .any { objectiveStage -> normalizeAuditKey(objectiveStage) == normalizedStageId }
    }
    return false
}

fun collectQuestObjectiveReferences(objectives: List<FeaturePackLoader.QuestEntryDefinition>): Set<String> {
    val references = HashSet<String>()
    for (objective in objectives) {
        references.add(normalizeQuestStageReference(objective.entryId))
        references.add(normalizeQuestStageReference(objective.itemId))
    }
    references.remove("")
    return references
}

fun questStageReferencesObjective(
    quest: FeaturePackLoader.ScenarioDefinition?,
    objective: FeaturePackLoader.QuestEntryDefinition?
): Boolean {
    if (quest == null || objective == null || quest.questStages.isEmpty()) {
        return false
    }
    for (stage in quest.questStages) {
        if (stageReferencesObjective(stage, objective)) {
            return true
        }
    }
    return false
}

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
        if (normalizedObjective.isNotBlank() && (normalizedObjective == entryId || normalizedObjective == itemId)) {
            return true
        }
    }
    return false
}

fun extractSourceKeyFromProfileData(profileData: String?): String {
    if (profileData.isNullOrBlank()) {
        return ""
    }
    return try {
        val parsed = JsonParser.parseString(profileData)
        if (parsed == null || !parsed.isJsonObject) {
            return ""
        }
        val sourceKey = parsed.asJsonObject.get("source_key")
        if (sourceKey != null && sourceKey.isJsonPrimitive) sourceKey.asString.trim() else ""
    } catch (_: JsonSyntaxException) {
        ""
    }
}

fun nodePriorityForAnchor(node: WorldNodeInfo, anchorRole: String?): Int =
    when (normalizeAuditKey(anchorRole)) {
        "home" -> {
            when {
                nodeMatchesAny(node, "home", "house", "bed", "sleep", "pat") -> 0
                nodeMatchesAny(node, "npc_spawn", "spawn") -> 1
                nodeMatchesAny(node, "entrance", "door", "inside", "intrare", "usa") -> 2
                nodeMatchesAny(node, "interaction") -> 3
                else -> -1
            }
        }
        "work" -> {
            when {
                nodeMatchesAny(node, "work", "workplace", "workstation", "job", "munca", "lucru") -> 0
                nodeMatchesAny(node, "npc_spawn", "spawn") -> 1
                nodeMatchesAny(node, "interaction", "counter", "desk") -> 2
                else -> -1
            }
        }
        "social" -> {
            when {
                nodeMatchesAny(node, "social", "meeting_point", "meeting", "market", "well", "tavern", "piata", "fantana") -> 0
                nodeMatchesAny(node, "interaction") -> 1
                nodeMatchesAny(node, "npc_spawn", "spawn") -> 2
                else -> -1
            }
        }
        else -> -1
    }

fun featureDisabledMessages(configPath: String, label: String): List<String> =
    listOf(
        "&cFunctia $label este dezactivata in configuratie.",
        "&7Activeaza &f$configPath=true &7in config.yml si ruleaza /ainpc reload."
    )

fun parseInt(s: String?, defaultValue: Int): Int =
    try {
        Integer.parseInt(s)
    } catch (_: NumberFormatException) {
        defaultValue
    }

fun parseDouble(s: String?, defaultValue: Double): Double =
    try {
        java.lang.Double.parseDouble(s)
    } catch (_: NumberFormatException) {
        defaultValue
    }

fun parseIntegerStrict(value: String?): Int? =
    try {
        Integer.valueOf(value)
    } catch (_: NumberFormatException) {
        null
    }

fun parseDoubleStrict(value: String?): Double? =
    try {
        java.lang.Double.valueOf(value)
    } catch (_: NumberFormatException) {
        null
    }

fun overlaps(leftMin: Int, leftMax: Int, rightMin: Int, rightMax: Int): Boolean =
    leftMax >= rightMin && rightMax >= leftMin

fun validBounds(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): Boolean =
    minX <= maxX && minY <= maxY && minZ <= maxZ

fun matchesAnyToken(rawValue: String?, vararg expectedTokens: String?): Boolean {
    val value = normalizeAuditKey(rawValue).replace('-', '_')
    if (value.isBlank()) {
        return false
    }
    for (expectedToken in expectedTokens) {
        if (value == normalizeAuditKey(expectedToken).replace('-', '_')) {
            return true
        }
    }
    return false
}

fun requiresWorkAnchor(occupation: String?): Boolean {
    val normalized = normalizeAuditKey(occupation)
    return normalized.isNotBlank() &&
        normalized != "locuitor" &&
        normalized != "localnic" &&
        normalized != "villager" &&
        normalized != "resident"
}

fun shortenBatchValue(value: String?, maxLength: Int): String {
    if (value.isNullOrBlank()) {
        return "-"
    }
    val safeMax = maxOf(8, maxLength)
    val cleanValue = value.trim()
    return if (cleanValue.length <= safeMax) {
        cleanValue
    } else {
        cleanValue.substring(0, safeMax - 3) + "..."
    }
}

fun valueOrDash(value: String?): String =
    if (value.isNullOrBlank()) "-" else value
