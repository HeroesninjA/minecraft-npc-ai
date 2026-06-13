@file:JvmName("AINPCCommandText")

package ro.ainpc.commands

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import org.bukkit.Material
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import ro.ainpc.npc.AINPC
import ro.ainpc.debug.WorldMappingSemanticIndex
import ro.ainpc.managers.ManagedVillagerAuditIssue
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.progression.ProgressionService
import ro.ainpc.progression.StoredProgression
import ro.ainpc.story.StoryEvent
import ro.ainpc.world.patch.PatchCandidate
import ro.ainpc.world.patch.PatchPlan
import ro.ainpc.world.patch.VillageGap
import ro.ainpc.world.mapping.MappingDraftKind
import ro.ainpc.world.mapping.MappingPoint
import ro.ainpc.world.mapping.MappingWandService
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldRegionInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.PlaceType
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldNode
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldPlace
import ro.ainpc.world.WorldRegion
import ro.ainpc.world.NpcWorldBinding
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.ScenarioEngine
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.HashSet
import java.util.Locale
import java.util.UUID
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.math.floor

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

fun routeProgressionAlias(
    args: Array<String>,
    kind: String,
    selectorMapper: BiFunction<String, String, String>
): Array<String> {
    val routedArgs = routeSubcommandToQuest(args)
    val normalizedKind = normalizeProgressionKind(kind)
    if (routedArgs.size == 1) {
        return arrayOf("quest", "log", normalizedKind)
    }

    val mode = routedArgs[1].lowercase(Locale.ROOT)
    if (mode == "log") {
        return routeProgressionAliasLogArgs(routedArgs, normalizedKind)
    }
    if (mode == "gui") {
        return routeProgressionAliasGuiArgs(routedArgs, normalizedKind)
    }

    return routeProgressionAliasSelectorArgs(routedArgs, mode, normalizedKind, selectorMapper)
}

private fun routeProgressionAliasSelectorArgs(
    args: Array<String>,
    mode: String,
    kind: String,
    selectorMapper: BiFunction<String, String, String>
): Array<String> {
    val routedArgs = args.clone()
    when (mode) {
        "gui", "nearest", "accept", "yes", "y", "da", "ok", "confirm",
        "decline", "deny", "reject", "no", "n", "nu", "refuz",
        "reset", "complete", "anchors" -> return routedArgs

        "status", "progress", "progres", "debug", "abandon" -> {
            if (routedArgs.size > 2) {
                routedArgs[2] = selectorMapper.apply(routedArgs[2], kind)
            }
        }

        "track", "current" -> {
            val rawAction = if (routedArgs.size > 2) routedArgs[2].lowercase(Locale.ROOT) else ""
            val action = if (rawAction == "start" || rawAction == "stop") rawAction else ""
            val selectorIndex = if (action.isBlank()) 2 else 3
            if (action != "stop" && routedArgs.size > selectorIndex) {
                routedArgs[selectorIndex] = selectorMapper.apply(routedArgs[selectorIndex], kind)
            }
        }

        else -> {
            if (routedArgs.size == 2) {
                return arrayOf("quest", "status", selectorMapper.apply(routedArgs[1], kind))
            }
            if (routedArgs.size == 3) {
                return arrayOf("quest", "status", selectorMapper.apply(routedArgs[1], kind), routedArgs[2])
            }
        }
    }
    return routedArgs
}

fun routeProgressionAliasGuiArgs(args: Array<String>, kind: String): Array<String> {
    if (args.size == 2) {
        return arrayOf("quest", "gui", kind)
    }
    if (args.size == 3) {
        return arrayOf("quest", "gui", progressionAliasLogFilter(args[2], kind))
    }
    return args
}

fun routeProgressionAliasLogArgs(args: Array<String>, kind: String): Array<String> {
    if (args.size == 2) {
        return arrayOf("quest", "log", kind)
    }

    if (args.size == 3) {
        return if (isQuestLogFilter(args[2])) {
            arrayOf("quest", "log", progressionAliasLogFilter(args[2], kind))
        } else {
            arrayOf("quest", "log", args[2], kind)
        }
    }

    if (args.size == 4) {
        val firstIsFilter = isQuestLogFilter(args[2])
        val secondIsFilter = isQuestLogFilter(args[3])
        if (firstIsFilter && !secondIsFilter) {
            return arrayOf("quest", "log", progressionAliasLogFilter(args[2], kind), args[3])
        }
        if (!firstIsFilter && secondIsFilter) {
            return arrayOf("quest", "log", args[2], progressionAliasLogFilter(args[3], kind))
        }
    }

    return args
}

fun progressionAliasSelector(
    selector: String?,
    kind: String,
    progressionService: ProgressionService?,
    onlinePlayerResolver: Function<String, Player?>
): String {
    if (selector.isNullOrBlank() ||
        selector.contains(":") ||
        selector.equals("nearest", ignoreCase = true) ||
        isTrackedQuestSelector(selector) ||
        onlinePlayerResolver.apply(selector) != null
    ) {
        return selector.orEmpty()
    }

    return progressionService?.kindSelector(selector, kind) ?: selector
}

fun shouldTreatQuestDecisionArgumentAsPlayer(
    argument: String?,
    npcResolver: Function<String, AINPC?>,
    onlinePlayerResolver: Function<String, Player?>
): Boolean {
    if (argument.isNullOrBlank() || argument.equals("nearest", ignoreCase = true)) {
        return false
    }
    if (npcResolver.apply(argument) != null) {
        return false
    }

    return onlinePlayerResolver.apply(argument) != null
}

fun shouldHandleAbandonAsQuestSelector(
    selector: String?,
    npcResolver: Function<String, AINPC?>
): Boolean {
    if (selector.isNullOrBlank() || selector.equals("nearest", ignoreCase = true)) {
        return false
    }
    if (isTrackedQuestSelector(selector)) {
        return true
    }
    return npcResolver.apply(selector) == null
}

fun resolveProgressionStoredPlayerUuid(
    selector: String?,
    onlinePlayerResolver: Function<String, Player?>
): String? {
    if (selector.isNullOrBlank() || selector.equals("all", ignoreCase = true)) {
        return ""
    }

    try {
        return UUID.fromString(selector).toString()
    } catch (_: IllegalArgumentException) {
        // Not a UUID; fall back to online player lookup.
    }

    return onlinePlayerResolver.apply(selector)?.uniqueId?.toString()
}

fun pointFromPlayer(player: Player): MappingPoint {
    val location = player.location
    return MappingPoint(
        player.world.name,
        location.blockX,
        location.blockY,
        location.blockZ
    )
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

fun findNodesAtLocation(worldAdmin: WorldAdminApi, location: Location): List<WorldNodeInfo> =
    worldAdmin.nodes
        .filter { node -> node.worldName().equals(location.world.name, ignoreCase = true) }
        .filter { node ->
            val dx = node.x() - location.x
            val dy = node.y() - location.y
            val dz = node.z() - location.z
            val radius = node.radius().coerceAtLeast(0.0)
            dx * dx + dy * dy + dz * dz <= radius * radius
        }
        .sortedBy { it.id() }

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

fun auditNpcs(
    report: AuditReport,
    npcs: Collection<AINPC>,
    loadedWorldNames: Set<String>,
    managedVillagerIssues: Collection<ManagedVillagerAuditIssue>,
    persistentSourceKeyIssues: Collection<ManagedVillagerAuditIssue>
) {
    report.info("NPC-uri incarcate: ${npcs.size}")
    if (npcs.isEmpty()) {
        report.warn("Nu exista NPC-uri incarcate in manager.")
        return
    }

    val npcsByName = mutableMapOf<String, MutableList<AINPC>>()
    val npcsByDatabaseId = mutableMapOf<Int, MutableList<AINPC>>()
    val npcsBySourceKey = mutableMapOf<String, MutableList<AINPC>>()

    for (npc in npcs) {
        val label = auditNpcLabel(npc)
        if (npc.databaseId <= 0) {
            report.error("$label nu are ID valid in baza de date.")
        } else {
            npcsByDatabaseId.getOrPut(npc.databaseId) { mutableListOf() }.add(npc)
        }

        if (npc.name.isNullOrBlank()) {
            report.error("$label nu are nume.")
        } else {
            npcsByName.getOrPut(normalizeAuditKey(npc.name)) { mutableListOf() }.add(npc)
        }

        val worldName = npc.worldName
        if (worldName.isNullOrBlank()) {
            report.error("$label nu are lume setata.")
        } else if (loadedWorldNames.none { it.equals(worldName, ignoreCase = true) }) {
            report.warn("$label refera o lume neincarcata: $worldName.")
        }

        if (npc.occupation.isNullOrBlank()) {
            report.warn("$label nu are ocupatie.")
        }

        if (!npc.isProfileCreated()) {
            report.warn("$label nu are profil persistent creat.")
        }

        if (npc.profileSource.isNullOrBlank()) {
            report.warn("$label nu are profile_source.")
        }
        if (!npc.sourceKey.isNullOrBlank()) {
            npcsBySourceKey.getOrPut(normalizeAuditKey(npc.sourceKey)) { mutableListOf() }.add(npc)
        }

        validateProfileJson(report, label, npc.profileDataJson)

        val homeAnchor = npc.homeAnchor
        if (homeAnchor == null) {
            report.warn("$label nu are casa/homeAnchor.")
        } else {
            validateOwnedLocation(report, "$label homeAnchor", homeAnchor, loadedWorldNames)
        }

        val workAnchor = npc.workAnchor
        if (workAnchor == null) {
            report.warn("$label nu are loc de munca/workAnchor.")
        } else {
            validateOwnedLocation(report, "$label workAnchor", workAnchor, loadedWorldNames)
        }
    }

    for ((normalizedName, duplicates) in npcsByName) {
        if (duplicates.size > 1) {
            val duplicateIds = duplicateNpcIds(duplicates)
            report.warn(
                "Exista mai multi NPC cu acelasi nume normalizat: " +
                    "$normalizedName (${duplicates.size}): $duplicateIds. " +
                    "Comenzile dupa nume pot selecta NPC-ul gresit."
            )
        }
    }

    for ((databaseId, duplicates) in npcsByDatabaseId) {
        if (duplicates.size > 1) {
            report.error("Exista mai multi NPC cu acelasi database_id: $databaseId.")
        }
    }

    for ((sourceKey, duplicates) in npcsBySourceKey) {
        if (duplicates.size > 1) {
            report.error(
                "Exista mai multi NPC cu acelasi source_key: " +
                    "$sourceKey (${duplicates.size}): ${duplicateNpcIds(duplicates)}."
            )
        }
    }

    appendManagedVillagerIssues(report, managedVillagerIssues)
    appendManagedVillagerIssues(report, persistentSourceKeyIssues)
}

private fun duplicateNpcIds(npcs: Collection<AINPC>): String =
    npcs.joinToString(prefix = "[", postfix = "]") { npc -> "${npc.name}#${npc.databaseId}" }

private fun appendManagedVillagerIssues(report: AuditReport, issues: Collection<ManagedVillagerAuditIssue>) {
    for (issue in issues) {
        if (issue.error()) {
            report.error(issue.message())
        } else {
            report.warn(issue.message())
        }
    }
}

fun auditMappingWandDrafts(
    report: AuditReport,
    service: MappingWandService?,
    worldAdmin: WorldAdminApi?,
    loadedNpcs: Collection<AINPC>,
    previewLimit: Int
) {
    if (service == null) {
        report.warn("MappingWandService este indisponibil; nu pot audita draft-urile wand recente.")
        return
    }

    val entries = service.recentConfirmedDrafts()
    report.info("Wand draft-uri confirmate recent: ${entries.size}.")
    if (entries.isEmpty()) {
        return
    }

    val canValidateWorld = worldAdmin != null && worldAdmin.isEnabled
    val limit = minOf(previewLimit, entries.size)
    for (entry in entries.take(limit)) {
        report.info(
            "Wand ${formatStoryTime(entry.confirmedAt())}" +
                " player=${entry.playerName()}" +
                " kind=${formatMappingDraftKind(entry.kind())}" +
                " draft=${formatOptional(entry.qualifiedId())}" +
                " result=${formatOptional(entry.resultId())}" +
                " (${formatOptional(entry.resultMessage())})."
        )
        if (canValidateWorld) {
            validateMappingWandAuditEntry(report, entry, worldAdmin, loadedNpcs)
        }
    }

    if (entries.size > limit) {
        report.info("Wand audit afiseaza ultimele $limit din ${entries.size} confirmari pastrate in memorie.")
    }
    if (!canValidateWorld) {
        report.warn("World admin este dezactivat sau indisponibil; auditul wand nu poate valida tintele din mapping.")
    }
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

fun isTrackedQuestSelector(selector: String?): Boolean =
    when (selector?.trim()?.lowercase(Locale.ROOT) ?: "") {
        "tracked", "current", "curent", "urmarit" -> true
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

fun collectSourceKeyDuplicateFindings(npcs: List<AINPC>, findings: MutableList<String>) {
    val bySourceKey = linkedMapOf<String, MutableList<AINPC>>()
    for (npc in npcs) {
        val sourceKey = npc.sourceKey
        if (sourceKey.isNullOrBlank()) {
            continue
        }
        bySourceKey.computeIfAbsent(normalizeAuditKey(sourceKey)) { mutableListOf() }.add(npc)
    }

    for ((sourceKey, duplicates) in bySourceKey) {
        if (duplicates.size <= 1) {
            continue
        }
        val sorted = duplicates.sortedBy { it.databaseId }
        findings.add(
            "&csource_key duplicat &f$sourceKey" +
                " &7canonic=&f${formatNpcIdentity(sorted[0])}" +
                " &7duplicate=&f${formatNpcIdentities(sorted.subList(1, sorted.size))}"
        )
    }
}

fun collectNearbyNameDuplicateFindings(npcs: List<AINPC>, findings: MutableList<String>) {
    val seenPairs = HashSet<String>()
    for (leftIndex in npcs.indices) {
        val left = npcs[leftIndex]
        for (rightIndex in leftIndex + 1 until npcs.size) {
            val right = npcs[rightIndex]
            if (normalizeAuditKey(left.name) != normalizeAuditKey(right.name)) {
                continue
            }
            if (!sameNearbyLocation(left.location, right.location, 2.25)) {
                continue
            }

            val pairKey = "${minOf(left.databaseId, right.databaseId)}:${maxOf(left.databaseId, right.databaseId)}"
            if (seenPairs.add(pairKey)) {
                findings.add(
                    "&eNume si locatie aproape identice: &f" +
                        "${formatNpcIdentity(left)} &7<-> &f${formatNpcIdentity(right)}" +
                        " &7la &f${formatLocation(left.location)}"
                )
            }
        }
    }
}

fun ownerMatchesLoadedNpc(ownerNpcId: String?, loadedNpcs: Collection<AINPC>): Boolean {
    if (ownerNpcId.isNullOrBlank()) {
        return false
    }

    val owner = normalizeAuditKey(ownerNpcId)
    return findLoadedNpcBySelector(loadedNpcs, owner) != null
}

fun findLoadedNpcBySelector(loadedNpcs: Collection<AINPC>, selector: String?): AINPC? {
    val normalizedSelector = normalizeAuditKey(selector)
    if (normalizedSelector.isBlank()) {
        return null
    }

    for (npc in loadedNpcs) {
        if (npc.uuid.toString().equals(normalizedSelector, ignoreCase = true)) {
            return npc
        }
        if (npc.databaseId > 0) {
            val id = npc.databaseId.toString()
            if (normalizedSelector == id || normalizedSelector == "npc_$id") {
                return npc
            }
        }
        val npcName = normalizeAuditKey(npc.name)
        if (npcName.isNotBlank() && (normalizedSelector == npcName || normalizedSelector == "npc_$npcName")) {
            return npc
        }
    }

    return null
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

fun findScenarioForProgression(
    loader: FeaturePackLoader?,
    progression: StoredProgression?
): FeaturePackLoader.ScenarioDefinition? {
    if (loader == null || progression == null) {
        return null
    }
    for (scenario in loader.getAllScenarios()) {
        if (!ProgressionDefinition.isProgressionCandidate(scenario)) {
            continue
        }
        val definition = ProgressionDefinition.fromScenarioDefinition(scenario)
        if (storedProgressionMatchesDefinition(progression, definition)) {
            return scenario
        }
    }
    return null
}

fun collectObjectiveKeyLookup(
    scenario: FeaturePackLoader.ScenarioDefinition
): Map<String, FeaturePackLoader.QuestEntryDefinition> {
    val lookup = LinkedHashMap<String, FeaturePackLoader.QuestEntryDefinition>()
    val objectives = scenario.objectives
    for (index in objectives.indices) {
        val objective = objectives[index]
        addObjectiveLookupKey(lookup, objective.entryId, objective)
        addObjectiveLookupKey(lookup, questEntryId(objective), objective)
        addObjectiveLookupKey(lookup, objective.itemId, objective)
        addObjectiveLookupKey(lookup, generatedQuestObjectiveKey(objective, index), objective)
    }
    return lookup
}

fun buildProgressionScenarioLookup(
    loader: FeaturePackLoader?
): Map<String, FeaturePackLoader.ScenarioDefinition> {
    if (loader == null) {
        return emptyMap()
    }

    val lookup = LinkedHashMap<String, FeaturePackLoader.ScenarioDefinition>()
    for (scenario in loader.getAllScenarios()) {
        if (!ProgressionDefinition.isProgressionCandidate(scenario)) {
            continue
        }
        val definition = ProgressionDefinition.fromScenarioDefinition(scenario)
        addScenarioLookupKey(lookup, definition.templateId(), scenario)
        addScenarioLookupKey(lookup, definition.progressionId(), scenario)
        addScenarioLookupKey(lookup, definition.definitionId(), scenario)
        addScenarioLookupKey(lookup, definition.code(), scenario)
        addScenarioLookupKey(lookup, definition.packId() + ":" + definition.definitionId(), scenario)
        addScenarioLookupKey(
            lookup,
            definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId(),
            scenario
        )
    }
    return lookup
}

fun findScenarioForQuestAnchorRow(
    row: QuestAnchorBindingRow,
    scenariosBySelector: Map<String, FeaturePackLoader.ScenarioDefinition>?
): FeaturePackLoader.ScenarioDefinition? {
    if (scenariosBySelector.isNullOrEmpty()) {
        return null
    }

    var scenario = scenariosBySelector[normalizeQuestObjectiveLookupKey(row.templateId())]
    if (scenario != null) {
        return scenario
    }
    scenario = scenariosBySelector[normalizeQuestObjectiveLookupKey(row.questCode())]
    return scenario ?: scenariosBySelector[normalizeQuestObjectiveLookupKey(lastSelectorSegment(row.templateId()))]
}

fun validateQuestAnchorObjectiveDefinition(
    report: AuditReport,
    label: String,
    row: QuestAnchorBindingRow,
    scenariosBySelector: Map<String, FeaturePackLoader.ScenarioDefinition>
) {
    if (row.objectiveKey().isNullOrBlank()) {
        return
    }

    val scenario = findScenarioForQuestAnchorRow(row, scenariosBySelector)
    if (scenario == null) {
        report.warn("$label nu poate valida objective_key; definitia progresiei nu este incarcata.")
        return
    }

    val objectives = collectObjectiveKeyLookup(scenario)
    val objective = objectives[normalizeQuestObjectiveLookupKey(row.objectiveKey())]
    if (objective == null) {
        val candidates = objectives.values
            .distinct()
            .map { displayQuestObjectiveKey(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(8)
        report.error("$label are objective_key inexistent in definitie: ${row.objectiveKey()}${formatObjectiveCandidates(candidates)}")
        return
    }

    val expectedType = normalizeQuestObjectiveType(objective.type)
    val actualType = normalizeQuestObjectiveType(row.objectiveType())
    if (expectedType.isNotBlank() && actualType.isNotBlank() && expectedType != actualType) {
        report.error(
            "$label are objective_type diferit de definitie: binding=${row.objectiveType()}, definitie=$expectedType."
        )
    }
}

fun validateQuestAnchorTarget(
    report: AuditReport,
    label: String,
    row: QuestAnchorBindingRow,
    worldAdmin: WorldAdminApi?,
    regionsById: Map<String, WorldRegionInfo>,
    placesById: Map<String, WorldPlaceInfo>,
    nodesById: Map<String, WorldNodeInfo>,
    loadedNpcs: Collection<AINPC>
) {
    val anchorType = normalizeAuditKey(row.anchorType())
    val anchorId = row.anchorId() ?: ""
    if (anchorId.isBlank()) {
        return
    }

    when (anchorType) {
        "region" -> {
            if (worldAdmin == null || !worldAdmin.isEnabled) {
                report.warn("$label nu poate valida region anchor: World admin este dezactivat.")
            } else if (!regionsById.containsKey(anchorId)) {
                report.error("$label refera regiunea inexistenta $anchorId.")
            }
        }
        "place" -> {
            if (worldAdmin == null || !worldAdmin.isEnabled) {
                report.warn("$label nu poate valida place anchor: World admin este dezactivat.")
            } else if (!placesById.containsKey(anchorId)) {
                report.error("$label refera place-ul inexistent $anchorId.")
            }
        }
        "node" -> {
            if (worldAdmin == null || !worldAdmin.isEnabled) {
                report.warn("$label nu poate valida node anchor: World admin este dezactivat.")
            } else if (!nodesById.containsKey(anchorId)) {
                report.error("$label refera node-ul inexistent $anchorId.")
            }
        }
        "npc" -> {
            if (findLoadedNpcBySelector(loadedNpcs, anchorId) == null) {
                report.warn("$label refera NPC care nu este incarcat acum: $anchorId.")
            }
        }
        else -> report.error("$label are anchor_type necunoscut: ${row.anchorType()}.")
    }
}

@JvmOverloads
fun validateQuestEntries(
    report: AuditReport,
    label: String,
    entryKind: String,
    entries: List<FeaturePackLoader.QuestEntryDefinition>,
    objectives: Boolean,
    worldSemanticIndex: WorldMappingSemanticIndex? = null
) {
    if (entries.isEmpty()) {
        if (objectives) {
            report.error("$label nu are obiective.")
        } else {
            report.warn("$label nu are recompense.")
        }
        return
    }

    val entryIds = HashSet<String>()
    for (entry in entries) {
        val entryLabel = "$label $entryKind ${questEntryId(entry)}"
        val rawEntryId = entry.metadata.getOrDefault("entry_id", "")
        val entryId = normalizeAuditKey(rawEntryId)
        validateQuestEntryId(report, label, entryKind, rawEntryId)
        if (entryId.isNotBlank() && !entryIds.add(entryId)) {
            report.error("$label are $entryKind duplicat: $rawEntryId.")
        }
        if (entry.amount <= 0) {
            report.error("$entryLabel are amount invalid: ${entry.amount}.")
        }

        if (objectives) {
            validateQuestObjectiveEntry(report, entryLabel, entry, worldSemanticIndex)
        } else {
            validateQuestRewardEntry(report, entryLabel, entry)
        }
    }
}

fun validateQuestEntryId(report: AuditReport, label: String, entryKind: String, entryId: String?) {
    if (entryId.isNullOrBlank()) {
        report.error("$label are $entryKind fara ID stabil.")
        return
    }

    if (!Regex("[A-Za-z0-9][A-Za-z0-9_.-]*").matches(entryId)) {
        report.error(
            "$label are $entryKind cu ID fragil: $entryId. Foloseste doar litere ASCII, cifre, '_', '-' sau '.'."
        )
    }
}

@JvmOverloads
fun validateQuestObjectiveEntry(
    report: AuditReport,
    entryLabel: String,
    entry: FeaturePackLoader.QuestEntryDefinition,
    worldSemanticIndex: WorldMappingSemanticIndex? = null
) {
    when (val type = normalizeQuestObjectiveType(entry.type)) {
        "collect_item", "deliver_to_npc" -> validateMaterialReference(report, entryLabel, entry.itemId)
        "kill_mob" -> validateEntityReference(report, entryLabel, entry.itemId)
        "talk_to_npc", "visit_region", "visit_place", "inspect_node" -> {
            if (entry.itemId.isBlank()) {
                report.warn("$entryLabel nu are item/reference; resolverul va folosi contextul curent daca poate.")
            } else {
                validateQuestSemanticReference(report, entryLabel, type, entry.itemId)
                validateQuestSemanticReferenceExists(report, entryLabel, type, entry.itemId, worldSemanticIndex)
            }
        }
        else -> report.error("$entryLabel are tip de obiectiv nesuportat de runtime: ${entry.type}.")
    }
}

fun validateQuestSemanticReferenceExists(
    report: AuditReport,
    label: String,
    objectiveType: String?,
    reference: String,
    worldSemanticIndex: WorldMappingSemanticIndex?
) {
    if (worldSemanticIndex == null) {
        return
    }

    val anchorType = semanticAnchorTypeForObjective(objectiveType)
    if (anchorType.isBlank() || anchorType == "npc") {
        return
    }

    if (!worldSemanticIndex.hasReference(anchorType, reference)) {
        report.warn(
            "$label refera $objectiveType `$reference`, dar tokenul nu apare in world mapping semantic_index pentru ancora $anchorType" +
                ". Verifica /ainpc debugdump world sau mapping-ul demo."
        )
    }
}

fun validateQuestRewardEntry(
    report: AuditReport,
    entryLabel: String,
    entry: FeaturePackLoader.QuestEntryDefinition
) {
    when (val type = normalizeQuestRewardType(entry.type)) {
        "item" -> validateMaterialReference(report, entryLabel, entry.itemId)
        "set_story_state", "record_story_event" -> validateQuestStoryAction(report, entryLabel, type, entry)
        else -> report.error("$entryLabel are tip de recompensa nesuportat de runtime: ${entry.type}.")
    }
}

fun validateMaterialReference(report: AuditReport, label: String, materialId: String?) {
    if (materialId.isNullOrBlank()) {
        report.error("$label nu are item/material.")
        return
    }
    if (Material.matchMaterial(materialId) == null) {
        report.error("$label refera material Minecraft invalid: $materialId.")
    }
}

fun validateEntityReference(report: AuditReport, label: String, entityId: String?) {
    if (entityId.isNullOrBlank()) {
        report.error("$label nu are mob/entity.")
        return
    }
    try {
        EntityType.valueOf(entityId.trim().uppercase(Locale.ROOT))
    } catch (_: IllegalArgumentException) {
        report.error("$label refera entity Minecraft invalid: $entityId.")
    }
}

fun validateQuestSemanticReference(
    report: AuditReport,
    label: String,
    objectiveType: String,
    reference: String
) {
    val prefix = questReferencePrefix(reference)
    if (prefix.isBlank()) {
        return
    }

    val allowedPrefixes = when (objectiveType) {
        "talk_to_npc" -> setOf("npc", "name", "profession")
        "visit_region" -> setOf("region", "tag", "type")
        "visit_place" -> setOf("place", "region", "tag", "type")
        "inspect_node" -> setOf("node", "place", "tag", "type")
        else -> emptySet()
    }

    if (allowedPrefixes.isNotEmpty() && prefix !in allowedPrefixes) {
        report.warn("$label foloseste prefix de referinta neobisnuit pentru $objectiveType: $reference.")
    }
}

fun validateQuestStoryAction(
    report: AuditReport,
    label: String,
    type: String,
    entry: FeaturePackLoader.QuestEntryDefinition
) {
    val metadata = entry.metadata
    val scope = normalizeStoryActionScope(metadata.getOrDefault("scope", ""))
    if (scope.isBlank()) {
        report.error("$label nu are metadata.scope pentru story action.")
    } else if (scope !in setOf("region", "place")) {
        report.error(
            "$label are metadata.scope invalid pentru story action: " +
                "${metadata.getOrDefault("scope", "")}. Valori acceptate: region, place."
        )
    }
    if (!hasAnyMetadata(
            metadata,
            "target", "scope_id", "target_id", "id",
            "place_id", "region_id", "target_place", "target_region", "place", "region"
        )
    ) {
        report.error("$label nu are metadata.target pentru story action.")
    }
    if (type == "set_story_state" && !hasAnyMetadata(metadata, "state_key", "state", "flag", "value", "item")) {
        report.error("$label nu are metadata.state pentru set_story_state.")
    }
    if (type == "set_story_state" && entry.variables.isEmpty()) {
        report.warn("$label nu are variables pentru set_story_state; va scrie doar state_key.")
    }
    if (type == "record_story_event") {
        if (!hasAnyMetadata(metadata, "event_type", "type_id")) {
            report.error("$label nu are metadata.event_type pentru record_story_event.")
        }
        if (!hasAnyMetadata(metadata, "event_key", "key")) {
            report.error("$label nu are metadata.event_key pentru record_story_event.")
        }
        if (entry.payload.isEmpty()) {
            report.error("$label nu are payload minim pentru record_story_event.")
        } else if (!hasAnyMapEntry(
                entry.payload,
                "quest", "outcome", "result", "reason", "state", "mechanic", "tag", "quest_template", "quest_code"
            )
        ) {
            report.warn(
                "$label are payload record_story_event, dar fara cheie semantica uzuala " +
                    "(quest/outcome/result/reason/state/mechanic/tag)."
            )
        }
    }
}

private fun addObjectiveLookupKey(
    lookup: MutableMap<String, FeaturePackLoader.QuestEntryDefinition>,
    key: String?,
    objective: FeaturePackLoader.QuestEntryDefinition
) {
    val normalized = normalizeQuestObjectiveLookupKey(key)
    if (normalized.isNotBlank()) {
        lookup.putIfAbsent(normalized, objective)
    }
}

private fun addScenarioLookupKey(
    lookup: MutableMap<String, FeaturePackLoader.ScenarioDefinition>,
    key: String?,
    scenario: FeaturePackLoader.ScenarioDefinition
) {
    val normalized = normalizeQuestObjectiveLookupKey(key)
    if (normalized.isNotBlank()) {
        lookup.putIfAbsent(normalized, scenario)
    }
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

fun warnNpcBindingFieldDivergence(
    report: AuditReport,
    label: String,
    field: String,
    storedValue: String?,
    profileValue: String?
) {
    if (!sameOptionalId(storedValue, profileValue)) {
        report.warn(
            "$label difera de profil pentru $field: " +
                "binding=${formatOptional(storedValue)}, profil=${formatOptional(profileValue)}."
        )
    }
}

fun validateHouseholdPlaceReference(
    report: AuditReport,
    label: String,
    role: String,
    placeId: String?,
    placesById: Map<String, WorldPlaceInfo>
) {
    if (placeId.isNullOrBlank()) {
        return
    }

    val place = placesById[normalizeAuditKey(placeId)]
    if (place == null) {
        report.error("$label refera ${role}_place_id inexistent in mapping: $placeId.")
    }
}

fun validateHouseholdNodeReference(
    report: AuditReport,
    label: String,
    role: String,
    nodeId: String?,
    expectedPlaceId: String?,
    nodesById: Map<String, WorldNodeInfo>
) {
    if (nodeId.isNullOrBlank()) {
        return
    }

    val node = nodesById[normalizeAuditKey(nodeId)]
    if (node == null) {
        report.error("$label refera ${role}_node_id inexistent in mapping: $nodeId.")
        return
    }
    if (!expectedPlaceId.isNullOrBlank() &&
        node.placeId().isNotBlank() &&
        !node.placeId().equals(expectedPlaceId, ignoreCase = true)
    ) {
        report.error("$label are ${role}_node_id=$nodeId in alt place decat $expectedPlaceId.")
    }
}

fun validateNpcWorldPlaceBinding(
    report: AuditReport,
    label: String,
    role: String,
    placeId: String?,
    placesById: Map<String, WorldPlaceInfo>
) {
    if (placeId.isNullOrBlank()) {
        return
    }

    val place = placesById[placeId]
    if (place == null) {
        report.error("$label refera ${role}_place_id inexistent: $placeId.")
        return
    }

    if (role == "home" && !isHousePlace(place)) {
        report.warn("$label are home_place_id care nu este casa/home: $placeId.")
    } else if (role == "work" && !isWorkplace(place)) {
        report.error("$label are work_place_id care nu este workplace: $placeId.")
    } else if (role == "social" && !isSocialPlace(place)) {
        report.warn("$label are social_place_id care nu este loc social clar: $placeId.")
    }
}

fun validateNpcWorldNodeBinding(
    report: AuditReport,
    label: String,
    role: String,
    nodeId: String?,
    expectedPlaceId: String?,
    nodesById: Map<String, WorldNodeInfo>
) {
    if (nodeId.isNullOrBlank()) {
        return
    }

    val node = nodesById[nodeId]
    if (node == null) {
        report.error("$label refera ${role}_node_id inexistent: $nodeId.")
        return
    }
    if (!expectedPlaceId.isNullOrBlank() &&
        node.placeId().isNotBlank() &&
        !node.placeId().equals(expectedPlaceId, ignoreCase = true)
    ) {
        report.error("$label are ${role}_node_id=$nodeId in alt place decat $expectedPlaceId.")
    }
}

fun auditNpcProfileBindingDivergence(
    report: AuditReport,
    label: String,
    npc: AINPC?,
    binding: NpcWorldBinding,
    worldAdmin: WorldAdminApi?
) {
    if (npc == null || worldAdmin == null || !worldAdmin.isEnabled) {
        return
    }

    val inferred = inferNpcWorldBindingFromProfile(npc, worldAdmin, "audit_profile")
    if (inferred == null || !inferred.hasAnyPlaceBinding()) {
        if (binding.hasAnyPlaceBinding()) {
            report.warn("$label are rand persistent, dar profilul NPC nu mai indica ancore mapabile.")
        }
        return
    }

    warnNpcBindingFieldDivergence(report, label, "home_place_id", binding.homePlaceId(), inferred.homePlaceId())
    warnNpcBindingFieldDivergence(report, label, "work_place_id", binding.workPlaceId(), inferred.workPlaceId())
    warnNpcBindingFieldDivergence(report, label, "social_place_id", binding.socialPlaceId(), inferred.socialPlaceId())
    warnNpcBindingFieldDivergence(report, label, "home_node_id", binding.homeNodeId(), inferred.homeNodeId())
    warnNpcBindingFieldDivergence(report, label, "work_node_id", binding.workNodeId(), inferred.workNodeId())
    warnNpcBindingFieldDivergence(report, label, "social_node_id", binding.socialNodeId(), inferred.socialNodeId())
}

fun auditWorld(
    report: AuditReport,
    worldAdmin: WorldAdminApi?,
    loadedWorldNames: Set<String>,
    loadedNpcs: Collection<AINPC>
) {
    if (worldAdmin == null || !worldAdmin.isEnabled) {
        report.warn("World admin este dezactivat sau indisponibil.")
        return
    }

    val regions = worldAdmin.regions.toList()
    val places = worldAdmin.places.toList()
    val nodes = worldAdmin.nodes.toList()
    report.info("World mapping: ${regions.size} regiuni, ${places.size} places, ${nodes.size} nodes.")
    if (regions.isEmpty()) {
        report.warn(
            "World admin este activ, dar mapping-ul nu are nicio regiune. " +
                "NPC-urile si questurile folosesc doar fallback-uri de coordonate."
        )
    } else if (places.isEmpty()) {
        report.warn(
            "World admin are regiuni, dar nu are places. " +
                "Casele, locurile de munca si contextul semantic pentru NPC-uri sunt incomplete."
        )
    }

    val regionsById = mutableMapOf<String, WorldRegionInfo>()
    for (region in regions) {
        regionsById[region.id()] = region
        if (loadedWorldNames.none { it.equals(region.worldName(), ignoreCase = true) }) {
            report.warn("Regiunea ${region.id()} refera o lume neincarcata: ${region.worldName()}.")
        }
        if (!validBounds(region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ())) {
            report.error("Regiunea ${region.id()} are bounds invalide.")
        }
    }

    val placesById = mutableMapOf<String, WorldPlaceInfo>()
    val ownerWarnings = HashSet<String>()
    for (place in places) {
        placesById[place.id()] = place
        val region = regionsById[place.regionId()]
        if (region == null) {
            report.error("Place-ul ${place.id()} refera regiunea inexistenta ${place.regionId()}.")
        } else if (!placeInsideRegion(place, region)) {
            report.error("Place-ul ${place.id()} nu este complet in regiunea ${region.id()}.")
        }

        if (loadedWorldNames.none { it.equals(place.worldName(), ignoreCase = true) }) {
            report.warn("Place-ul ${place.id()} refera o lume neincarcata: ${place.worldName()}.")
        }
        if (!validBounds(place.minX(), place.minY(), place.minZ(), place.maxX(), place.maxY(), place.maxZ())) {
            report.error("Place-ul ${place.id()} are bounds invalide.")
        }
        if (place.placeType() == PlaceType.HOUSE && place.ownerNpcId().isBlank() && !hasPendingOwner(place)) {
            report.warn("Casa ${place.id()} nu are owner_npc_id.")
        }
        if (!place.publicAccess() && place.ownerNpcId().isBlank() && !hasPendingOwner(place)) {
            report.warn("Place-ul privat ${place.id()} nu are owner_npc_id.")
        }
        if (place.ownerNpcId().isNotBlank() && !ownerMatchesLoadedNpc(place.ownerNpcId(), loadedNpcs)) {
            val key = "${place.ownerNpcId()}@${place.id()}"
            if (ownerWarnings.add(key)) {
                report.warn("Place-ul ${place.id()} are owner_npc_id fara NPC incarcat potrivit: ${place.ownerNpcId()}.")
            }
        }
        if (isWorkplace(place) && worldAdmin.getNodesForPlace(place.id()).isEmpty()) {
            report.warn("Locul de munca ${place.id()} nu are nodes de interactiune.")
        }
    }

    auditWorldReadiness(report, worldAdmin, places, nodes)

    for (i in places.indices) {
        for (j in i + 1 until places.size) {
            val left = places[i]
            val right = places[j]
            if (left.regionId().equals(right.regionId(), ignoreCase = true) && placesIntersect(left, right)) {
                report.warn("Place-uri suprapuse in ${left.regionId()}: ${left.id()} si ${right.id()}.")
            }
        }
    }

    for (node in nodes) {
        val region = regionsById[node.regionId()]
        if (region == null) {
            report.error("Node-ul ${node.id()} refera regiunea inexistenta ${node.regionId()}.")
        }

        val place = if (node.placeId().isNotBlank()) {
            val mappedPlace = placesById[node.placeId()]
            if (mappedPlace == null) {
                report.error("Node-ul ${node.id()} refera place-ul inexistent ${node.placeId()}.")
            } else if (!mappedPlace.regionId().equals(node.regionId(), ignoreCase = true)) {
                report.error("Node-ul ${node.id()} refera un place din alta regiune: ${node.placeId()}.")
            }
            mappedPlace
        } else {
            null
        }

        if (node.radius() <= 0) {
            report.error("Node-ul ${node.id()} are raza invalida: ${node.radius()}.")
        }
        if (loadedWorldNames.none { it.equals(node.worldName(), ignoreCase = true) }) {
            report.warn("Node-ul ${node.id()} refera o lume neincarcata: ${node.worldName()}.")
        }

        if (place != null && !pointInsidePlace(node, place)) {
            report.error("Node-ul ${node.id()} nu este in interiorul place-ului ${place.id()}.")
        } else if (place == null && region != null && !pointInsideRegion(node, region)) {
            report.error("Node-ul ${node.id()} nu este in interiorul regiunii ${region.id()}.")
        }
    }
}

fun auditWorldReadiness(
    report: AuditReport,
    worldAdmin: WorldAdminApi,
    places: List<WorldPlaceInfo>,
    nodes: List<WorldNodeInfo>
) {
    if (places.isEmpty()) {
        return
    }

    val houseCount = places.count { isHousePlace(it) }
    val workplaceCount = places.count { isWorkplace(it) }
    val socialPlaceCount = places.count { isSocialPlace(it) }
    val questNodeCount = nodes.count { node ->
        nodeMatchesAny(node, "quest_trigger", "quest_board", "inspect_node", "interaction")
    }
    val bedNodeCount = nodes.count { node -> nodeMatchesAny(node, "bed") }
    val workNodeCount = nodes.count { node -> nodeMatchesAny(node, "work", "workstation", "work_anchor") }

    report.info(
        "Mapping readiness: $houseCount case, $workplaceCount locuri de munca, " +
            "$socialPlaceCount locuri sociale, $questNodeCount quest/interaction nodes."
    )

    if (houseCount == 0) {
        report.warn("Mapping-ul nu are nicio casa. Spawn-ul household si rutina home vor cadea pe fallback.")
    }
    if (bedNodeCount == 0) {
        report.warn("Mapping-ul nu are node-uri de tip bed. Casele nu pot fi validate bine pentru rezidenti.")
    }
    if (workplaceCount == 0 || workNodeCount == 0) {
        report.warn("Mapping-ul nu are locuri de munca cu node-uri work/workstation.")
    }
    if (socialPlaceCount == 0) {
        report.warn("Mapping-ul nu are loc social public pentru rutina sociala.")
    }
    if (questNodeCount == 0) {
        report.warn("Mapping-ul nu are quest/interaction nodes pentru obiective inspect_node.")
    }

    for (house in places.filter { isHousePlace(it) }) {
        val houseNodes = worldAdmin.getNodesForPlace(house.id())
        if (!hasAnySemanticNode(houseNodes, "bed", "home", "npc_spawn", "spawn")) {
            report.warn("Casa ${house.id()} nu are node bed/home/npc_spawn.")
        }
    }
}

fun auditNpcSpawnBindings(report: AuditReport, places: List<WorldPlaceInfo>, npc: AINPC) {
    val label = auditNpcLabel(npc)
    if (requiresWorkAnchor(npc.occupation) && npc.workAnchor == null) {
        report.warn("$label are ocupatie '${npc.occupation}', dar nu are workAnchor.")
    }

    if (npc.homeAnchor != null) {
        val homePlace = findPlaceContainingOwnedLocation(places, npc.homeAnchor)
        if (homePlace != null && !isHousePlace(homePlace)) {
            report.warn("$label are homeAnchor intr-un place care nu este casa: ${homePlace.id()}.")
        }
    }

    if (npc.workAnchor != null) {
        val workPlace = findPlaceContainingOwnedLocation(places, npc.workAnchor)
        if (workPlace == null) {
            report.warn("$label are workAnchor in afara oricarui mapped place.")
        } else if (!isWorkplace(workPlace)) {
            report.error("$label are workAnchor in ${workPlace.id()}, dar place-ul nu este workplace compatibil.")
        }
    }
}

fun auditHouseSpawnOrder(
    report: AuditReport,
    worldAdmin: WorldAdminApi,
    house: WorldPlaceInfo,
    loadedNpcs: Collection<AINPC>
) {
    val residents = parseResidents(house)
    val maxResidents = parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity")

    if (maxResidents == null) {
        report.warn("Casa ${house.id()} nu are metadata.max_residents/capacity.")
    } else if (residents.isNotEmpty() && residents.size > maxResidents) {
        report.error("Casa ${house.id()} are ${residents.size} rezidenti, peste max_residents=$maxResidents.")
    }

    if (residents.isNotEmpty() &&
        !hasAnySemanticNode(worldAdmin.getNodesForPlace(house.id()), "bed", "home", "npc_spawn", "spawn")
    ) {
        report.error("Casa ${house.id()} are rezidenti, dar nu are node bed/home/npc_spawn.")
    }

    val seenResidentIds = HashSet<Int>()
    for (residentSelector in residents) {
        val resident = findLoadedNpcBySelector(loadedNpcs, residentSelector)
        if (resident == null) {
            report.error("Casa ${house.id()} contine resident necunoscut in metadata.residents: $residentSelector.")
            continue
        }

        if (!seenResidentIds.add(resident.databaseId)) {
            report.warn("Casa ${house.id()} contine resident duplicat: ${resident.name}#${resident.databaseId}.")
        }

        if (resident.homeAnchor == null) {
            report.error("${auditNpcLabel(resident)} este listed resident in ${house.id()}, dar nu are homeAnchor.")
        } else if (!ownedLocationInsidePlace(resident.homeAnchor, house)) {
            report.error(
                "${auditNpcLabel(resident)} este listed resident in ${house.id()}, " +
                    "dar homeAnchor nu este in interiorul casei."
            )
        }
    }
}

fun validateMappingWandAuditEntry(
    report: AuditReport,
    entry: MappingWandService.MappingWandAuditEntry,
    worldAdmin: WorldAdminApi,
    loadedNpcs: Collection<AINPC>
) {
    val label = "Wand draft recent ${formatOptional(entry.qualifiedId())}"
    val kind = entry.kind()
    if (kind == null) {
        report.warn("$label nu are kind setat.")
        return
    }

    when (kind) {
        MappingDraftKind.REGION -> {
            if (entry.resultId().isBlank() || worldAdmin.getRegion(entry.resultId()) == null) {
                report.error("$label a confirmat o regiune care nu mai exista: ${formatOptional(entry.resultId())}.")
            }
        }
        MappingDraftKind.PLACE -> {
            if (entry.resultId().isBlank() || worldAdmin.getPlace(entry.resultId()) == null) {
                report.error("$label a confirmat un place care nu mai exista: ${formatOptional(entry.resultId())}.")
            }
        }
        MappingDraftKind.NODE -> {
            if (entry.resultId().isBlank() || worldAdmin.getNode(entry.resultId()) == null) {
                report.error("$label a confirmat un node care nu mai exista: ${formatOptional(entry.resultId())}.")
            }
        }
        MappingDraftKind.NPC_BIND -> {
            if (entry.placeId().isBlank() || worldAdmin.getPlace(entry.placeId()) == null) {
                report.error("$label a confirmat un npc_bind catre place inexistent: ${formatOptional(entry.placeId())}.")
            }
        }
        MappingDraftKind.QUEST_ANCHOR -> {
            val anchorType = entry.metadata().getOrDefault("anchor_type", "")
            val anchorId = entry.metadata().getOrDefault("anchor_id", "")
            if (anchorType.isBlank() || anchorId.isBlank()) {
                report.warn("$label nu are anchor_type/anchor_id in metadata.")
            } else if (!questAnchorTargetExists(anchorType, anchorId, worldAdmin, loadedNpcs)) {
                report.error("$label a confirmat quest_anchor catre tinta inexistenta: $anchorType:$anchorId.")
            }
        }
    }
}

fun questAnchorTargetExists(
    anchorType: String?,
    anchorId: String?,
    worldAdmin: WorldAdminApi,
    loadedNpcs: Collection<AINPC>
): Boolean =
    worldAdmin.isEnabled && when (normalizeAuditKey(anchorType)) {
        "region" -> worldAdmin.getRegion(anchorId) != null
        "place" -> worldAdmin.getPlace(anchorId) != null
        "node" -> worldAdmin.getNode(anchorId) != null
        "npc" -> findLoadedNpcBySelector(loadedNpcs, anchorId) != null
        else -> false
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

fun findScenarioForProgressionRow(
    templateId: String?,
    questCode: String?,
    scenariosBySelector: Map<String, FeaturePackLoader.ScenarioDefinition>?
): FeaturePackLoader.ScenarioDefinition? {
    if (scenariosBySelector.isNullOrEmpty()) {
        return null
    }

    scenariosBySelector[normalizeQuestObjectiveLookupKey(templateId)]?.let { return it }
    scenariosBySelector[normalizeQuestObjectiveLookupKey(questCode)]?.let { return it }
    return scenariosBySelector[normalizeQuestObjectiveLookupKey(lastSelectorSegment(templateId))]
}

fun parseStoredJsonObject(rawValue: String?): JsonElement? {
    val safeRawValue = if (rawValue.isNullOrBlank()) "{}" else rawValue
    return try {
        val parsed = JsonParser.parseString(safeRawValue)
        if (parsed != null && parsed.isJsonObject) parsed else null
    } catch (_: JsonSyntaxException) {
        null
    }
}

fun auditStoryJsonColumn(
    report: AuditReport,
    column: String,
    label: String,
    rawValue: String?,
    arrayExpected: Boolean
) {
    val safeRawValue = if (rawValue.isNullOrBlank()) {
        if (arrayExpected) "[]" else "{}"
    } else {
        rawValue
    }

    try {
        val parsed = JsonParser.parseString(safeRawValue)
        if (arrayExpected && !parsed.isJsonArray) {
            report.warn("$column nu este array JSON pentru $label.")
        } else if (!arrayExpected && !parsed.isJsonObject) {
            report.warn("$column nu este obiect JSON pentru $label.")
        }
    } catch (exception: JsonSyntaxException) {
        report.warn("$column are JSON invalid pentru $label: ${exception.message}")
    }
}

fun validateProfileJson(report: AuditReport, label: String, profileData: String?) {
    if (profileData.isNullOrBlank()) {
        report.warn("$label are profile_data gol.")
        return
    }

    try {
        JsonParser.parseString(profileData)
    } catch (exception: JsonSyntaxException) {
        report.error("$label are profile_data JSON invalid: ${exception.message}")
    }
}

@Throws(SQLException::class)
fun describeCurrentRow(rs: ResultSet): String {
    val columnCount = rs.metaData.columnCount
    val parts = ArrayList<String>()
    for (i in 1..columnCount) {
        parts.add("${rs.metaData.getColumnLabel(i)}=${rs.getString(i)}")
    }
    return parts.joinToString(", ")
}

fun isQuestAuditCandidate(scenario: FeaturePackLoader.ScenarioDefinition?): Boolean =
    scenario != null &&
        (scenario.baseType == ScenarioEngine.ScenarioType.QUEST ||
            (scenario.isProgressionEnabled &&
                (scenario.questCode.isNotBlank() ||
                    scenario.objectives.isNotEmpty() ||
                    scenario.rewards.isNotEmpty())))

fun validateProgressionMechanicDefinitions(
    report: AuditReport,
    mechanics: Collection<FeaturePackLoader.ProgressionMechanicDefinition>?
) {
    if (mechanics.isNullOrEmpty()) {
        report.warn("Nu exista mechanics/progression definite explicit; questurile legacy folosesc fallback intern.")
        return
    }

    val keys = HashSet<String>()
    for (mechanic in mechanics) {
        val label = "Progression mechanic ${mechanic.packId}:${mechanic.id}"
        val key = normalizeAuditKey("${mechanic.packId}:${mechanic.id}")
        if (!keys.add(key)) {
            report.error("$label este duplicat.")
        }
        if (mechanic.id.isBlank()) {
            report.error("$label nu are ID.")
        }
        if (mechanic.kind.isBlank()) {
            report.warn("$label nu are kind; UI-ul va folosi ID-ul ca fallback.")
        }
        if (mechanic.label.isBlank()) {
            report.warn("$label nu are label vizibil.")
        }
        if (mechanic.isProgressEnabled && mechanic.maxActive == 0) {
            report.info("$label nu are max_active; se aplica limitele globale/config existente.")
        }
    }
}

fun validateQuestTemplate(
    report: AuditReport,
    featurePackLoader: FeaturePackLoader,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition,
    knownQuestReferences: Set<String>,
    questCodes: MutableMap<String, String>,
    worldSemanticIndex: WorldMappingSemanticIndex?
) {
    if (quest.questCode.isBlank()) {
        report.warn("$label nu are quest.code; runtime-ul foloseste template_id ca fallback.")
    } else {
        val normalizedCode = normalizeAuditKey(quest.questCode)
        val previous = questCodes[normalizedCode]
        if (previous == null) {
            questCodes[normalizedCode] = label
        } else {
            report.error("$label are quest.code duplicat cu $previous: ${quest.questCode}.")
        }
    }

    if (!quest.isRequiresPlayer) {
        report.warn("$label nu are requires_player=true; questurile jucabile ar trebui sa ceara player.")
    }
    if (quest.questGiverProfession.isBlank()) {
        report.warn("$label nu are quest.giver_profession; poate cadea pe fallback de NPC.")
    } else if (featurePackLoader.findProfessionDefinition(quest.questGiverProfession) == null) {
        report.warn("$label foloseste giver_profession necunoscuta: ${quest.questGiverProfession}.")
    }

    validateQuestPrerequisites(report, label, quest, knownQuestReferences)
    validateQuestRepeatability(report, label, quest)
    validateQuestPhases(report, label, quest)
    validateQuestDialogues(report, label, quest)
    validateQuestGiverRole(report, label, quest)
    validateQuestProgressionMetadata(report, featurePackLoader, label, quest)
    validateQuestEntries(report, label, "obiectiv", quest.objectives, true, worldSemanticIndex)
    validateQuestEntries(report, label, "recompensa", quest.rewards, false)
    validateQuestObjectiveStages(report, label, quest)
}

fun validateQuestProgressionMetadata(
    report: AuditReport,
    featurePackLoader: FeaturePackLoader,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition
) {
    if (!quest.isProgressionEnabled) {
        report.warn("$label are progression/progress disabled; nu va fi candidat bun pentru runtime generic.")
        return
    }

    val mechanicId = quest.progressionMechanicId
    if (mechanicId.isBlank()) {
        report.warn("$label nu are mechanic/progression.mechanic; quest runtime foloseste fallback.")
        return
    }

    val mechanic = featurePackLoader.findProgressionMechanicDefinition(quest.packId, mechanicId)
    if (mechanic == null) {
        report.warn("$label refera progression mechanic necunoscuta: $mechanicId.")
    } else if (!mechanic.isProgressEnabled) {
        report.warn("$label foloseste mechanic cu progress=false: ${mechanic.packId}:${mechanic.id}.")
    }
}

fun validateQuestPrerequisites(
    report: AuditReport,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition,
    knownQuestReferences: Set<String>
) {
    for (prerequisite in quest.questPrerequisites) {
        val normalizedPrerequisite = normalizeAuditKey(prerequisite)
        if (normalizedPrerequisite.isBlank()) {
            report.warn("$label are prerequisite gol.")
        } else if (!knownQuestReferences.contains(normalizedPrerequisite)) {
            report.error("$label cere prerequisite necunoscut: $prerequisite.")
        }
    }
}

fun validateQuestRepeatability(
    report: AuditReport,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition
) {
    if (quest.isQuestRepeatable && quest.questCooldownSeconds <= 0) {
        report.warn("$label este repeatable, dar nu are cooldown_seconds.")
    }
    if (!quest.isQuestRepeatable && quest.questCooldownSeconds > 0) {
        report.warn("$label are cooldown_seconds, dar repeatable=false.")
    }
}

fun validateQuestPhases(
    report: AuditReport,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition
) {
    if (quest.phases.isEmpty()) {
        report.warn("$label nu are phases; statusul va fi mai greu de urmarit.")
        return
    }

    val phases = quest.phases
        .map { normalizeAuditKey(it) }
        .toHashSet()
    for (requiredPhase in listOf("introduction", "acceptance", "return", "completion")) {
        if (!phases.contains(requiredPhase)) {
            report.warn("$label nu are faza $requiredPhase.")
        }
    }
}

fun validateQuestObjectiveStages(
    report: AuditReport,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition
) {
    if (quest.objectives.isEmpty()) {
        return
    }

    val knownPhases = quest.phases
        .map { normalizeAuditKey(it) }
        .filter { it.isNotBlank() }
        .toHashSet()
    var hasStagedObjective = false
    var hasUnstagedObjective = false

    for (objective in quest.objectives) {
        val stage = questEntryStage(objective)
        if (stage.isBlank()) {
            if (questStageReferencesObjective(quest, objective)) {
                hasStagedObjective = true
            } else {
                hasUnstagedObjective = true
            }
            continue
        }

        hasStagedObjective = true
        val normalizedStage = normalizeAuditKey(stage)
        if (!knownPhases.contains(normalizedStage)) {
            report.error("$label are obiectiv ${questEntryId(objective)} cu phase/stage necunoscut: $stage.")
        }
    }

    if (hasStagedObjective && hasUnstagedObjective) {
        report.warn("$label combina obiective cu phase/stage si obiective fara etapa explicita.")
    }

    validateQuestStageDefinitions(report, label, quest, knownPhases)
}

fun validateQuestStageDefinitions(
    report: AuditReport,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition,
    knownPhases: Set<String>
) {
    if (quest.questStages.isEmpty()) {
        return
    }

    val objectiveReferences = collectQuestObjectiveReferences(quest.objectives)
    for (stage in quest.questStages) {
        if (stage.id.isBlank()) {
            report.error("$label are quest stage fara ID.")
            continue
        }

        val normalizedStageId = normalizeAuditKey(stage.id)
        if (!knownPhases.contains(normalizedStageId)) {
            report.error("$label are quest stage care nu exista in phases: ${stage.id}.")
        }

        val completionMode = normalizeQuestStageCompletionMode(stage.completionMode)
        if (!isSupportedQuestStageCompletionMode(completionMode)) {
            report.error("$label stage ${stage.id} are completion_mode necunoscut: ${stage.completionMode}.")
        }

        validateQuestStageNextStage(report, label, quest, stage, knownPhases, normalizedStageId)

        val stageHasObjectiveMetadata = quest.objectives
            .map { questEntryStage(it) }
            .any { objectiveStage -> normalizeAuditKey(objectiveStage) == normalizedStageId }
        if (stage.objectiveIds.isEmpty()) {
            if (!"phases".equals(stage.metadata.getOrDefault("source", ""), ignoreCase = true) &&
                !stageHasObjectiveMetadata
            ) {
                report.warn("$label stage ${stage.id} nu listeaza objectives si nu are obiective cu phase/stage aferent.")
            }
            continue
        }

        val seenStageObjectives = HashSet<String>()
        for (objectiveId in stage.objectiveIds) {
            val normalizedObjective = normalizeQuestStageReference(objectiveId)
            if (normalizedObjective.isBlank()) {
                report.warn("$label stage ${stage.id} are objective ID gol.")
                continue
            }
            if (!seenStageObjectives.add(normalizedObjective)) {
                report.warn("$label stage ${stage.id} listeaza objective duplicat: $objectiveId.")
            }
            if (!objectiveReferences.contains(normalizedObjective)) {
                report.error("$label stage ${stage.id} refera objective necunoscut: $objectiveId.")
            }
        }
    }
}

fun validateQuestStageNextStage(
    report: AuditReport,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition,
    stage: FeaturePackLoader.QuestStageDefinition,
    knownPhases: Set<String>,
    normalizedStageId: String
) {
    val nextStage = stage.getNextStageId()
    if (nextStage.isBlank()) {
        return
    }

    val normalizedNextStage = normalizeAuditKey(nextStage)
    if (normalizedNextStage.isBlank()) {
        report.warn("$label stage ${stage.id} are next_stage gol.")
        return
    }
    if (normalizedNextStage == normalizedStageId) {
        report.error("$label stage ${stage.id} are next_stage catre sine.")
    }
    if (!knownPhases.contains(normalizedNextStage)) {
        report.error("$label stage ${stage.id} are next_stage necunoscut: $nextStage.")
    } else if (!isQuestRuntimeStage(quest, normalizedNextStage)) {
        report.warn("$label stage ${stage.id} are next_stage catre o faza fara obiective runtime: $nextStage.")
    }
}

fun validateQuestDialogues(
    report: AuditReport,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition
) {
    val dialogues = quest.questDialogues
    for (requiredDialogue in listOf("offer", "offered", "accepted", "active", "ready", "completed")) {
        val lines = dialogues[requiredDialogue]
        if (lines.isNullOrEmpty()) {
            report.warn("$label nu are quest.dialogues.$requiredDialogue.")
        }
    }
    val hasAvailabilityGate = quest.questPrerequisites.isNotEmpty() || quest.isQuestRepeatable
    val unavailableLines = dialogues["unavailable"]
    if (hasAvailabilityGate && unavailableLines.isNullOrEmpty()) {
        report.warn("$label are prerequisite/repeatable, dar nu are quest.dialogues.unavailable.")
    }
}

fun validateQuestGiverRole(
    report: AuditReport,
    label: String,
    quest: FeaturePackLoader.ScenarioDefinition
) {
    val role = quest.roles["QUEST_GIVER"]
    if (role == null) {
        report.warn("$label nu defineste rolul QUEST_GIVER.")
        return
    }

    val giverProfession = normalizeAuditKey(quest.questGiverProfession)
    if (giverProfession.isBlank() || role.requiredProfessions.isEmpty()) {
        return
    }

    val roleMatchesGiver = role.requiredProfessions
        .map { normalizeAuditKey(it) }
        .any { giverProfession == it }
    if (!roleMatchesGiver) {
        report.warn("$label are QUEST_GIVER.required_professions diferit de quest.giver_profession.")
    }
}

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

fun validateOwnedLocation(
    report: AuditReport,
    label: String,
    location: AINPC.OwnedLocation?,
    loadedWorldNames: Set<String>
) {
    val worldName = location?.worldName()
    if (worldName.isNullOrBlank()) {
        report.error("$label nu are lume.")
        return
    }
    if (loadedWorldNames.none { it.equals(worldName, ignoreCase = true) }) {
        report.warn("$label refera o lume neincarcata: $worldName.")
    }
    if (location.label().isNullOrBlank()) {
        report.warn("$label nu are label.")
    }
}

fun findPlaceContainingOwnedLocation(
    places: List<WorldPlaceInfo>,
    location: AINPC.OwnedLocation?
): WorldPlaceInfo? {
    if (location == null) {
        return null
    }

    return places.firstOrNull { place -> ownedLocationInsidePlace(location, place) }
}

fun inferProfileAnchorPlace(worldAdmin: WorldAdminApi?, anchor: AINPC.OwnedLocation?): WorldPlaceInfo? {
    if (worldAdmin == null || anchor == null || anchor.worldName().isBlank()) {
        return null
    }
    return worldAdmin.findPlace(
        anchor.worldName(),
        floor(anchor.x()).toInt(),
        floor(anchor.y()).toInt(),
        floor(anchor.z()).toInt()
    )
}

fun inferProfileAnchorNode(
    worldAdmin: WorldAdminApi?,
    anchor: AINPC.OwnedLocation?,
    place: WorldPlaceInfo?
): WorldNodeInfo? {
    if (worldAdmin == null || anchor == null || anchor.worldName().isBlank()) {
        return null
    }
    return worldAdmin.findNodesNear(anchor.worldName(), anchor.x(), anchor.y(), anchor.z(), 2.5, 5)
        .firstOrNull { node -> place == null || node.placeId().isBlank() || node.placeId().equals(place.id(), ignoreCase = true) }
}

fun preserveBindingMetadata(proposed: NpcWorldBinding, existing: NpcWorldBinding?, source: String): NpcWorldBinding {
    if (existing == null) {
        return proposed
    }
    return NpcWorldBinding(
        proposed.npcId(),
        firstNonBlank(proposed.npcUuid(), existing.npcUuid()),
        firstNonBlank(proposed.npcName(), existing.npcName()),
        proposed.homePlaceId(),
        proposed.workPlaceId(),
        proposed.socialPlaceId(),
        proposed.homeNodeId(),
        proposed.workNodeId(),
        proposed.socialNodeId(),
        existing.familyId(),
        source,
        existing.createdAt(),
        0L
    )
}

fun inferNpcWorldBindingFromProfile(npc: AINPC?, worldAdmin: WorldAdminApi?, source: String): NpcWorldBinding? {
    if (npc == null || worldAdmin == null || !worldAdmin.isEnabled) {
        return null
    }
    val homePlace = inferProfileAnchorPlace(worldAdmin, npc.homeAnchor)
    val workPlace = inferProfileAnchorPlace(worldAdmin, npc.workAnchor)
    val socialPlace = inferProfileAnchorPlace(worldAdmin, npc.socialAnchor)
    if (homePlace == null && workPlace == null && socialPlace == null) {
        return null
    }

    val homeNode = inferProfileAnchorNode(worldAdmin, npc.homeAnchor, homePlace)
    val workNode = inferProfileAnchorNode(worldAdmin, npc.workAnchor, workPlace)
    val socialNode = inferProfileAnchorNode(worldAdmin, npc.socialAnchor, socialPlace)
    return NpcWorldBinding(
        npc.databaseId,
        npc.uuid.toString(),
        npc.name,
        homePlace?.id() ?: "",
        workPlace?.id() ?: "",
        socialPlace?.id() ?: "",
        homeNode?.id() ?: "",
        workNode?.id() ?: "",
        socialNode?.id() ?: "",
        "",
        source,
        0L,
        0L
    )
}

fun collectMappingMetadataRepairActions(
    worldAdmin: WorldAdminApi,
    actions: MutableList<String>,
    warnings: MutableList<String>,
    binding: NpcWorldBinding,
    role: String,
    placeId: String?,
    npcSelector: String?
): Int {
    if (placeId.isNullOrBlank()) {
        return 0
    }
    val place = worldAdmin.getPlace(placeId)
    if (place == null) {
        warnings.add("npc_world_bindings npc_id=${binding.npcId()} refera ${role}_place_id inexistent: $placeId.")
        return 0
    }

    var candidates = 0
    when (role) {
        "home" -> {
            if (!sameOptionalId(place.ownerNpcId(), npcSelector)) {
                actions.add("Ar seta owner_npc_id pentru ${place.id()} la $npcSelector din binding npc_id=${binding.npcId()}.")
                candidates++
            }
            if (!metadataListContains(place, "resident_npc_ids", npcSelector)) {
                actions.add("Ar adauga $npcSelector in resident_npc_ids pentru ${place.id()}.")
                candidates++
            }
        }
        "work" -> {
            if (!metadataListContains(place, "worker_npc_ids", npcSelector)) {
                actions.add("Ar adauga $npcSelector in worker_npc_ids pentru ${place.id()}.")
                candidates++
            }
        }
        "social" -> {
            if (!metadataListContains(place, "social_npc_ids", npcSelector)) {
                actions.add("Ar adauga $npcSelector in social_npc_ids pentru ${place.id()}.")
                candidates++
            }
        }
        else -> {
            // Rolurile validate in Java sunt cele persistate in npc_world_bindings.
        }
    }
    return candidates
}

fun createOwnedLocationFromPlace(
    worldAdmin: WorldAdminApi,
    place: WorldPlaceInfo,
    anchorRole: String
): AINPC.OwnedLocation {
    val node = findBestAnchorNodeForPlace(worldAdmin, place, anchorRole)
    if (node != null) {
        return AINPC.OwnedLocation(
            anchorRole,
            nodeLabel(node, place.displayName()),
            node.worldName(),
            node.x(),
            node.y(),
            node.z()
        )
    }

    return AINPC.OwnedLocation(
        anchorRole,
        place.displayName(),
        place.worldName(),
        placeCenterX(place),
        placeAnchorY(place),
        placeCenterZ(place)
    )
}

fun findBestAnchorNodeForPlace(
    worldAdmin: WorldAdminApi,
    place: WorldPlaceInfo,
    anchorRole: String?
): WorldNodeInfo? {
    var bestNode: WorldNodeInfo? = null
    var bestScore = Double.MAX_VALUE
    for (node in worldAdmin.getNodesForPlace(place.id())) {
        val priority = nodePriorityForAnchor(node, anchorRole)
        if (priority < 0) {
            continue
        }

        val score = priority * 100_000.0 + distanceSquaredToPlaceCenter(place, node)
        if (score < bestScore) {
            bestScore = score
            bestNode = node
        }
    }
    return bestNode
}

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

fun findRegionMatches(worldAdmin: WorldAdminApi, selector: String?): List<WorldRegionInfo> {
    if (selector.isNullOrBlank()) {
        return emptyList()
    }

    val normalizedSelector = selector.trim()
    return worldAdmin.regions
        .filter { region ->
            region.id().equals(normalizedSelector, ignoreCase = true)
                || region.name().equals(normalizedSelector, ignoreCase = true)
        }
        .sortedBy { it.id() }
}

fun findPlaceMatches(worldAdmin: WorldAdminApi, selector: String?): List<WorldPlaceInfo> {
    if (selector.isNullOrBlank()) {
        return emptyList()
    }

    val normalizedSelector = selector.trim()
    val normalizedSuffix = ":${normalizedSelector}".lowercase(Locale.getDefault())
    return worldAdmin.places
        .filter { place ->
            place.id().equals(normalizedSelector, ignoreCase = true)
                || place.displayName().equals(normalizedSelector, ignoreCase = true)
                || place.id().lowercase(Locale.getDefault()).endsWith(normalizedSuffix)
        }
        .sortedBy { it.id() }
}

fun findPlaceMatches(worldAdmin: WorldAdminApi, regionId: String, selector: String?): List<WorldPlaceInfo> =
    findPlaceMatches(worldAdmin, selector)
        .filter { place -> place.regionId().equals(regionId, ignoreCase = true) }
        .sortedBy { it.id() }

fun findPlaceById(worldAdmin: WorldAdminApi?, placeId: String?): WorldPlaceInfo? {
    if (worldAdmin == null || placeId.isNullOrBlank()) {
        return null
    }

    return worldAdmin.places.firstOrNull { place -> place.id().equals(placeId, ignoreCase = true) }
}

fun hasAmbiguousRegionMatch(worldAdmin: WorldAdminApi?, selector: String?): Boolean =
    worldAdmin != null && findRegionMatches(worldAdmin, selector).size > 1

fun hasAmbiguousPlaceMatch(worldAdmin: WorldAdminApi?, selector: String?): Boolean =
    worldAdmin != null && findPlaceMatches(worldAdmin, selector).size > 1

fun findNodeById(worldAdmin: WorldAdminApi?, nodeId: String?): WorldNodeInfo? {
    if (worldAdmin == null || nodeId.isNullOrBlank()) {
        return null
    }

    return worldAdmin.nodes.firstOrNull { node -> node.id().equals(nodeId, ignoreCase = true) }
}

fun parseRegionTypeStrict(value: String?): RegionType? {
    val type = RegionType.fromId(value)
    return if (type == RegionType.CUSTOM && !value.equals("custom", ignoreCase = true)) {
        null
    } else {
        type
    }
}

fun parsePlaceTypeStrict(value: String?): PlaceType? {
    val type = PlaceType.fromId(value)
    return if (type == PlaceType.CUSTOM && !value.equals("custom", ignoreCase = true)) {
        null
    } else {
        type
    }
}

fun parseNodeTypeStrict(value: String?): WorldNodeType? {
    val type = WorldNodeType.fromId(value)
    return if (type == WorldNodeType.CUSTOM && !value.equals("custom", ignoreCase = true)) {
        null
    } else {
        type
    }
}

fun toRegionInfo(region: WorldRegion): WorldRegionInfo =
    WorldRegionInfo(
        region.id,
        region.name,
        region.worldName,
        region.type.id,
        region.minX,
        region.minY,
        region.minZ,
        region.maxX,
        region.maxY,
        region.maxZ,
        region.getTags(),
        region.storyState.mode,
        region.storyState.stateKey,
        region.storyState.getStoryPool()
    )

fun toPlaceInfo(place: WorldPlace): WorldPlaceInfo =
    WorldPlaceInfo(
        place.id,
        place.regionId,
        place.displayName,
        place.worldName,
        place.placeType,
        place.minX,
        place.minY,
        place.minZ,
        place.maxX,
        place.maxY,
        place.maxZ,
        place.getTags(),
        place.getOwnerNpcId(),
        place.isPublicAccess,
        place.getMetadata()
    )

fun toNodeInfo(node: WorldNode): WorldNodeInfo =
    WorldNodeInfo(
        node.id,
        node.regionId,
        node.placeId,
        node.type.id,
        node.worldName,
        node.x,
        node.y,
        node.z,
        node.radius,
        node.getMetadata()
    )

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

fun readQuestAnchorBindingRow(rs: ResultSet): QuestAnchorBindingRow =
    QuestAnchorBindingRow(
        rs.getString("player_uuid"),
        rs.getString("template_id"),
        rs.getString("objective_key"),
        rs.getString("quest_code"),
        rs.getString("objective_type"),
        rs.getString("reference"),
        rs.getString("anchor_type"),
        rs.getString("anchor_id"),
        rs.getString("anchor_label"),
        rs.getLong("created_at"),
        rs.getLong("updated_at"),
        rs.getString("status"),
    )

fun resetWandSelectionPart(
    service: MappingWandService,
    player: Player,
    rawPart: String,
): MappingWandService.MappingWandSession? {
    val part = rawPart.lowercase(Locale.ROOT)
    return when (part) {
        "pos1" -> service.resetPos1(player)
        "pos2" -> service.resetPos2(player)
        "point", "punct" -> service.resetPoint(player)
        else -> null
    }
}
