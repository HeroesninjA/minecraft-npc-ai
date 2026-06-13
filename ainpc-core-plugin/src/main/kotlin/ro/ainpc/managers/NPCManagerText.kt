@file:JvmName("NPCManagerText")

package ro.ainpc.managers

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Bed
import org.bukkit.entity.Villager
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCPersonality
import ro.ainpc.utils.NPCNameGenerator
import ro.ainpc.world.NpcWorldBinding
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.util.Collections
import java.util.Locale
import java.util.Random
import java.util.function.Predicate
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private val PLAIN_TEXT = PlainTextComponentSerializer.plainText()

fun normalizeSourceKey(sourceKey: String?): String =
    sourceKey?.trim()?.lowercase(Locale.ROOT).orEmpty()

fun valueOrFallback(value: String?, fallback: String): String {
    val safeValue = value?.trim().orEmpty()
    return safeValue.ifBlank { fallback }
}

fun readInt(json: JsonObject?, key: String, fallback: Int): Int {
    val element = json?.get(key)
    return if (element != null && element.isJsonPrimitive) element.asInt else fallback
}

fun readLong(json: JsonObject?, key: String, fallback: Long): Long {
    val element = json?.get(key)
    return if (element != null && element.isJsonPrimitive) element.asLong else fallback
}

fun readDouble(json: JsonObject?, key: String, fallback: Double): Double {
    val element = json?.get(key)
    return if (element != null && element.isJsonPrimitive) element.asDouble else fallback
}

fun readString(json: JsonObject?, key: String, fallback: String): String {
    val element = json?.get(key)
    return if (element != null && element.isJsonPrimitive) element.asString else fallback
}

fun truncateProfileText(text: String?, maxLength: Int): String? {
    if (text == null || text.length <= maxLength) {
        return text
    }

    val truncated = text.substring(0, max(0, maxLength - 3)).trim()
    return if (truncated.endsWith(".")) truncated else "$truncated..."
}

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

fun matchesAnyToken(rawValue: String?, vararg expectedTokens: String): Boolean {
    val value = normalizeAnchorToken(rawValue)
    if (value.isBlank()) {
        return false
    }

    for (expectedToken in expectedTokens) {
        val expected = normalizeAnchorToken(expectedToken)
        if (value == expected) {
            return true
        }
    }

    return false
}

fun normalizeAnchorToken(rawValue: String?): String =
    rawValue?.trim()?.lowercase(Locale.ROOT)?.replace(' ', '_')?.replace('-', '_').orEmpty()

fun nodeLabel(node: WorldNodeInfo, fallbackLabel: String?): String {
    val explicitLabel = firstNonBlank(
        node.metadata()["label"],
        node.metadata()["name"],
        node.metadata()["display_name"],
    )
    if (explicitLabel.isNotBlank()) {
        return explicitLabel
    }

    return fallbackLabel?.takeIf { it.isNotBlank() } ?: node.id()
}

fun firstNonBlank(vararg values: String?): String =
    values.firstOrNull { !it.isNullOrBlank() }.orEmpty()

fun nodePriority(node: WorldNodeInfo?, anchorRole: String?): Int {
    if (node == null || anchorRole == null) {
        return -1
    }

    return when (anchorRole.lowercase(Locale.ROOT)) {
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
                nodeMatchesAny(
                    node,
                    "social",
                    "meeting_point",
                    "meeting",
                    "market",
                    "well",
                    "tavern",
                    "piata",
                    "fantana"
                ) -> 0

                nodeMatchesAny(node, "interaction") -> 1
                nodeMatchesAny(node, "npc_spawn", "spawn") -> 2
                else -> -1
            }
        }

        else -> -1
    }
}

fun isHomePlace(place: WorldPlaceInfo): Boolean =
    place.placeType() == PlaceType.HOUSE ||
            place.hasTag("home") ||
            place.hasTag("house") ||
            metadataEquals(place, "role", "home") ||
            metadataEquals(place, "purpose", "home")

fun isWorkPlace(place: WorldPlaceInfo, occupation: String?): Boolean {
    if (place.placeType() == PlaceType.HOUSE) {
        return false
    }

    return place.hasTag("work") ||
            place.hasTag("workplace") ||
            place.hasTag("job") ||
            metadataEquals(place, "role", "work") ||
            metadataEquals(place, "purpose", "work") ||
            matchesOccupationPlaceType(occupation, place.placeType()) ||
            isGenericWorkPlaceType(place.placeType())
}

fun isSocialPlace(place: WorldPlaceInfo): Boolean =
    place.placeType() == PlaceType.MARKET ||
            place.placeType() == PlaceType.TAVERN ||
            place.hasTag("social") ||
            place.hasTag("meeting") ||
            place.hasTag("meeting_point") ||
            place.hasTag("market") ||
            place.hasTag("well") ||
            metadataEquals(place, "role", "social") ||
            metadataEquals(place, "purpose", "social") ||
            metadataEquals(place, "anchor", "social")

@Suppress("UNUSED_PARAMETER")
fun matchesOccupationPlaceType(occupation: String?, placeType: PlaceType): Boolean = false

fun isGenericWorkPlaceType(placeType: PlaceType): Boolean =
    placeType == PlaceType.FORGE ||
            placeType == PlaceType.SHOP ||
            placeType == PlaceType.FARM ||
            placeType == PlaceType.MARKET ||
            placeType == PlaceType.TAVERN

fun metadataEquals(place: WorldPlaceInfo, key: String, expectedValue: String): Boolean =
    place.metadata()[key]?.equals(expectedValue, ignoreCase = true) == true

fun distanceSquaredToPlaceCenter(place: WorldPlaceInfo, location: Location): Double =
    distanceSquared(
        placeCenterX(place),
        placeAnchorY(place),
        placeCenterZ(place),
        location.x,
        location.y,
        location.z,
    )

fun distanceSquaredToPlaceCenter(place: WorldPlaceInfo, node: WorldNodeInfo): Double =
    distanceSquared(
        placeCenterX(place),
        placeAnchorY(place),
        placeCenterZ(place),
        node.x(),
        node.y(),
        node.z(),
    )

fun distanceSquared(
    leftX: Double,
    leftY: Double,
    leftZ: Double,
    rightX: Double,
    rightY: Double,
    rightZ: Double,
): Double {
    val dx = leftX - rightX
    val dy = leftY - rightY
    val dz = leftZ - rightZ
    return dx * dx + dy * dy + dz * dz
}

fun placeCenterX(place: WorldPlaceInfo): Double =
    (place.minX() + place.maxX()) / 2.0

fun placeAnchorY(place: WorldPlaceInfo): Double =
    min(place.maxY().toDouble(), place.minY() + 1.0)

fun placeCenterZ(place: WorldPlaceInfo): Double =
    (place.minZ() + place.maxZ()) / 2.0

fun toOwnedLocation(type: String, place: WorldPlaceInfo, node: WorldNodeInfo?): AINPC.OwnedLocation {
    if (node != null) {
        return toOwnedLocation(type, node, place.displayName())
    }

    return AINPC.OwnedLocation(
        type,
        place.displayName(),
        place.worldName(),
        placeCenterX(place),
        placeAnchorY(place),
        placeCenterZ(place),
    )
}

fun toOwnedLocation(type: String, node: WorldNodeInfo, fallbackLabel: String?): AINPC.OwnedLocation =
    AINPC.OwnedLocation(
        type,
        nodeLabel(node, fallbackLabel),
        node.worldName(),
        node.x(),
        node.y(),
        node.z(),
    )

fun isOwnedByNpc(place: WorldPlaceInfo, npc: AINPC?): Boolean {
    if (place.ownerNpcId().isBlank() || npc == null) {
        return false
    }

    val owner = normalizeOwnerKey(place.ownerNpcId())
    if (owner.equals(npc.uuid.toString(), ignoreCase = true)) {
        return true
    }
    if (npc.databaseId > 0) {
        val databaseId = npc.databaseId.toString()
        if (owner == databaseId || owner == "npc_$databaseId") {
            return true
        }
    }

    val npcName = normalizeOwnerKey(npc.name)
    return npcName.isNotBlank() && (owner == npcName || owner == "npc_$npcName")
}

fun normalizeOwnerKey(value: String?): String =
    value?.trim()?.lowercase(Locale.ROOT)?.replace(' ', '_').orEmpty()

fun writeOwnedLocation(root: JsonObject, key: String, anchor: AINPC.OwnedLocation?) {
    if (anchor == null) {
        return
    }

    val anchorJson = JsonObject()
    anchorJson.addProperty("type", anchor.type())
    anchorJson.addProperty("label", anchor.label())
    anchorJson.addProperty("world", anchor.worldName())
    anchorJson.addProperty("x", anchor.x())
    anchorJson.addProperty("y", anchor.y())
    anchorJson.addProperty("z", anchor.z())
    root.add(key, anchorJson)
}

fun readOwnedLocation(root: JsonObject?, key: String): AINPC.OwnedLocation? {
    if (root == null || !root.has(key) || !root.get(key).isJsonObject) {
        return null
    }

    val anchorJson = root.getAsJsonObject(key)
    val world = readString(anchorJson, "world", "")
    if (world.isBlank()) {
        return null
    }

    return AINPC.OwnedLocation(
        readString(anchorJson, "type", key),
        readString(anchorJson, "label", key),
        world,
        readDouble(anchorJson, "x", 0.0),
        readDouble(anchorJson, "y", 0.0),
        readDouble(anchorJson, "z", 0.0),
    )
}

fun isGenericOccupation(occupation: String?): Boolean {
    if (occupation.isNullOrBlank()) {
        return true
    }

    return when (occupation.trim().lowercase(Locale.ROOT)) {
        "locuitor", "villager", "resident", "localnic" -> true
        else -> false
    }
}

@Suppress("UNUSED_PARAMETER")
fun shouldPreferEnvironmentOccupation(
    profession: Villager.Profession?,
    mappedOccupation: String?,
    inferredOccupation: String?,
): Boolean {
    if (inferredOccupation.isNullOrBlank()) {
        return false
    }

    if (isGenericOccupation(mappedOccupation)) {
        return true
    }

    if (mappedOccupation.equals(inferredOccupation, ignoreCase = true)) {
        return true
    }

    return false
}

fun resolveOccupationChoice(
    profession: Villager.Profession?,
    mappedOccupation: String?,
    inferredOccupation: String?,
    themedOccupation: String?,
): String {
    if (shouldPreferEnvironmentOccupation(profession, mappedOccupation, inferredOccupation)) {
        return inferredOccupation.orEmpty()
    }

    if (!isGenericOccupation(mappedOccupation)) {
        return mappedOccupation.orEmpty()
    }

    if (!inferredOccupation.isNullOrBlank()) {
        return inferredOccupation
    }

    if (!themedOccupation.isNullOrBlank()) {
        return themedOccupation
    }

    return mappedOccupation ?: "resident"
}

fun createVillagerSeededRandom(villager: Villager): Random =
    Random(villager.uniqueId.mostSignificantBits xor villager.uniqueId.leastSignificantBits)

@Suppress("UNUSED_PARAMETER")
fun inferOccupationFromEnvironment(villager: Villager?): String? = null

fun generateUniqueAutoName(gender: String?, random: Random, isNpcNameTaken: Predicate<String>): String {
    val candidates = ArrayList(NPCNameGenerator.predefinedNames(gender))
    Collections.shuffle(candidates, random)
    for (candidate in candidates) {
        if (!isNpcNameTaken.test(candidate)) {
            return candidate
        }
    }

    return uniquifyNpcName(NPCNameGenerator.randomName(gender, random), isNpcNameTaken)
}

fun isNpcNameTaken(candidateName: String?, existingNpcs: Collection<AINPC>): Boolean {
    if (candidateName.isNullOrBlank()) {
        return false
    }

    val normalizedCandidate = candidateName.trim().lowercase(Locale.ROOT)
    for (existingNpc in existingNpcs) {
        val existingName = existingNpc.name
        if (existingName.trim().lowercase(Locale.ROOT) == normalizedCandidate) {
            return true
        }
    }
    return false
}

fun uniquifyNpcName(baseName: String?, isNpcNameTaken: Predicate<String>): String {
    val base = if (baseName.isNullOrBlank()) "NPC" else baseName.trim()
    var candidate = base
    var suffix = 2
    while (isNpcNameTaken.test(candidate)) {
        candidate = "$base $suffix"
        suffix++
    }
    return candidate
}

@Suppress("UNUSED_PARAMETER")
fun matchesOccupationWorkstation(occupation: String?, material: Material): Boolean =
    isWorkstation(material)

@Suppress("UNUSED_PARAMETER")
fun describeWorkAnchor(occupation: String?, material: Material): String =
    material.name.lowercase(Locale.ROOT).replace('_', ' ')

fun isWorkstation(material: Material): Boolean =
    material == Material.COMPOSTER ||
            material == Material.BLAST_FURNACE ||
            material == Material.SMITHING_TABLE ||
            material == Material.ANVIL ||
            material == Material.CHIPPED_ANVIL ||
            material == Material.DAMAGED_ANVIL ||
            material == Material.GRINDSTONE ||
            material == Material.BARREL ||
            material == Material.SMOKER ||
            material == Material.CAMPFIRE ||
            material == Material.BREWING_STAND ||
            material == Material.CAULDRON ||
            material == Material.LECTERN ||
            material == Material.CARTOGRAPHY_TABLE ||
            material == Material.STONECUTTER ||
            material == Material.FLETCHING_TABLE ||
            material == Material.BELL ||
            material == Material.CHEST

fun resolveGender(gender: String?): String =
    if ("female".equals(gender, ignoreCase = true)) "female" else "male"

fun floorToBlock(coordinate: Double): Int =
    floor(coordinate).toInt()

fun safeNpcName(npc: AINPC?): String {
    val name = npc?.name
    return if (name.isNullOrBlank()) "NPC" else name
}

fun createFallbackHomeAnchor(npc: AINPC?, center: Location): AINPC.OwnedLocation =
    AINPC.OwnedLocation(
        "home",
        "casa lui ${safeNpcName(npc)}",
        center.world.name,
        center.x,
        center.y,
        center.z,
    )

fun createFallbackWorkAnchor(npc: AINPC, center: Location): AINPC.OwnedLocation {
    val occupation = npc.occupation
    val label = if (occupation.isNullOrBlank() || isGenericOccupation(occupation)) {
        "locul de munca al lui ${safeNpcName(npc)}"
    } else {
        "locul de munca de $occupation"
    }

    return AINPC.OwnedLocation(
        "work",
        label,
        center.world.name,
        center.x,
        center.y,
        center.z,
    )
}

fun shouldReplacePersistedSourceKeyOwner(
    candidateNpcId: Int,
    currentOwnerId: Int,
    currentOwnerExists: Boolean,
): Boolean {
    if (candidateNpcId <= 0) {
        return false
    }
    if (currentOwnerId <= 0) {
        return true
    }
    if (!currentOwnerExists) {
        return true
    }
    return candidateNpcId < currentOwnerId
}

fun isPreferredSourceKeyCandidate(candidate: AINPC?, current: AINPC?): Boolean {
    if (candidate == null) {
        return false
    }
    if (current == null) {
        return true
    }

    val candidateId = candidate.databaseId
    val currentId = current.databaseId
    if (candidateId > 0 && currentId > 0) {
        return candidateId < currentId
    }
    if (candidateId > 0) {
        return true
    }
    if (currentId > 0) {
        return false
    }
    return candidate.uuid < current.uuid
}

fun isSameNpcRecord(first: AINPC?, second: AINPC?): Boolean {
    if (first === second) {
        return true
    }
    if (first == null || second == null) {
        return false
    }
    if (first.databaseId > 0 && first.databaseId == second.databaseId) {
        return true
    }
    return first.uuid == second.uuid
}

@Suppress("UNUSED_PARAMETER")
fun generateBackstory(name: String?, occupation: String?, profession: Villager.Profession?): String {
    val safeOccupation = if (occupation.isNullOrBlank()) "resident" else occupation
    return "$name are rolul $safeOccupation si participa la viata comunitatii."
}

fun namesMatch(expected: String?, actual: String?): Boolean {
    if (expected == null || actual == null) {
        return false
    }
    return expected.equals(actual, ignoreCase = true)
}

fun isNpcPlannedForDeletion(npc: AINPC?, plannedDeletedNpcIds: Set<Int>): Boolean =
    npc != null && npc.databaseId > 0 && plannedDeletedNpcIds.contains(npc.databaseId)

fun isSameNpcLocation(first: Location?, second: Location?): Boolean {
    val firstWorld = first?.world ?: return false
    val secondWorld = second?.world ?: return false
    if (firstWorld != secondWorld) {
        return false
    }
    return first.distanceSquared(second) <= 2.25
}

fun formatLocation(location: Location?): String {
    val world = location?.world ?: return "<locatie necunoscuta>"
    return world.name +
            " " +
            floorToBlock(location.x) + "," +
            floorToBlock(location.y) + "," +
            floorToBlock(location.z)
}

fun buildVillageKey(center: Location?): String {
    val world = center?.world ?: return "unknown"
    val coarseX = floorToBlock(center.x) shr 5
    val coarseZ = floorToBlock(center.z) shr 5
    return world.name + ":" + coarseX + ":" + coarseZ
}

fun buildProfileSummary(npc: AINPC): String {
    val parts = mutableListOf<String>()
    val displayName = npc.name.takeIf { it.isNotBlank() } ?: "Acest NPC"
    val occupation = npc.occupation?.takeIf { it.isNotBlank() } ?: "locuitor"

    parts.add("$displayName este $occupation")

    if (npc.age > 0) {
        parts.add("${npc.age} ani")
    }

    if (npc.gender.isNotBlank()) {
        parts.add(if (npc.gender.equals("female", ignoreCase = true)) "femeie" else "barbat")
    }

    val traits = npc.personality.getDominantTraits()
    if (traits.isNotBlank() && !traits.equals("echilibrat", ignoreCase = true)) {
        parts.add("trasaturi dominante: $traits")
    }

    val summary = StringBuilder(parts.joinToString(", ")).append(".")
    if (!npc.backstory.isNullOrBlank()) {
        summary.append(" ").append(truncateProfileText(npc.backstory, 180))
    }

    return summary.toString()
}

fun getVillagerDisplayName(villager: Villager): String? {
    val customName = villager.customName() ?: return null
    return PLAIN_TEXT.serialize(customName).trim()
}

fun buildSpawnState(npc: AINPC): JsonObject {
    val state = JsonObject()
    state.addProperty("spawned", npc.spawned)
    state.addProperty("entity_uuid", npc.uuid.toString())
    state.addProperty("database_id", npc.databaseId)
    state.addProperty("source_key", npc.sourceKey)
    state.addProperty("world", npc.worldName)
    state.addProperty("x", npc.x)
    state.addProperty("y", npc.y)
    state.addProperty("z", npc.z)
    state.addProperty("yaw", npc.yaw)
    state.addProperty("pitch", npc.pitch)
    state.addProperty("chunk_x", floorToBlock(npc.x) shr 4)
    state.addProperty("chunk_z", floorToBlock(npc.z) shr 4)
    state.addProperty("restorable", !npc.worldName.isNullOrBlank())
    state.addProperty("updated_at", System.currentTimeMillis())
    return state
}

fun buildProfileData(npc: AINPC, gson: Gson): String {
    val profile = JsonObject()
    profile.addProperty("npc_id", npc.databaseId)
    profile.addProperty("uuid", npc.uuid.toString())
    profile.addProperty("name", npc.name)
    profile.addProperty("display_name", npc.displayName)
    profile.addProperty("profile_source", npc.profileSource)
    profile.addProperty("profile_version", npc.profileVersion)
    profile.addProperty("source_key", npc.sourceKey)
    profile.addProperty("world", npc.worldName)
    profile.addProperty("x", npc.x)
    profile.addProperty("y", npc.y)
    profile.addProperty("z", npc.z)
    profile.addProperty("yaw", npc.yaw)
    profile.addProperty("pitch", npc.pitch)
    profile.addProperty("occupation", npc.occupation)
    profile.addProperty("backstory", npc.backstory)
    profile.addProperty("age", npc.age)
    profile.addProperty("gender", npc.gender)
    profile.addProperty("current_state", npc.currentState.name)
    profile.addProperty("spawned", npc.spawned)
    profile.add("spawn_state", buildSpawnState(npc))
    profile.addProperty("profile_summary", buildProfileSummary(npc))

    val traitsArray = JsonArray()
    for (traitId in npc.traits) {
        if (traitId.isNotBlank()) {
            traitsArray.add(traitId)
        }
    }
    profile.add("traits", traitsArray)

    val personality = JsonObject()
    personality.addProperty("openness", npc.personality.openness)
    personality.addProperty("conscientiousness", npc.personality.conscientiousness)
    personality.addProperty("extraversion", npc.personality.extraversion)
    personality.addProperty("agreeableness", npc.personality.agreeableness)
    personality.addProperty("neuroticism", npc.personality.neuroticism)
    personality.addProperty("dominant_traits", npc.personality.getDominantTraits())
    profile.add("personality", personality)

    val emotions = JsonObject()
    emotions.addProperty("happiness", npc.emotions.happiness)
    emotions.addProperty("sadness", npc.emotions.sadness)
    emotions.addProperty("anger", npc.emotions.anger)
    emotions.addProperty("fear", npc.emotions.fear)
    emotions.addProperty("surprise", npc.emotions.surprise)
    emotions.addProperty("disgust", npc.emotions.disgust)
    emotions.addProperty("trust", npc.emotions.trust)
    emotions.addProperty("anticipation", npc.emotions.anticipation)
    emotions.addProperty("short_description", npc.emotions.getShortDescription())
    profile.add("emotions", emotions)

    val simulation = JsonObject()
    simulation.addProperty("hunger_level", npc.hungerLevel)
    simulation.addProperty("energy_level", npc.energyLevel)
    simulation.addProperty("social_need_level", npc.socialNeedLevel)
    simulation.addProperty("comfort_level", npc.comfortLevel)
    simulation.addProperty("safety_level", npc.safetyLevel)
    simulation.addProperty("current_goal", npc.currentGoal)
    simulation.addProperty("planned_routine_activity", npc.plannedRoutineActivity)
    simulation.addProperty("last_simulation_tick_at", npc.lastSimulationTickAt)
    profile.add("simulation", simulation)

    val ownedLocations = JsonObject()
    writeOwnedLocation(ownedLocations, "home", npc.homeAnchor)
    writeOwnedLocation(ownedLocations, "work", npc.workAnchor)
    writeOwnedLocation(ownedLocations, "social", npc.socialAnchor)
    profile.add("owned_locations", ownedLocations)

    return gson.toJson(profile)
}

@Suppress("UNUSED_PARAMETER")
fun generatePersonalityForProfession(profession: Villager.Profession?): NPCPersonality =
    NPCPersonality.generateRandom()

@Suppress("UNUSED_PARAMETER")
fun generatePersonalityForOccupation(occupation: String?, profession: Villager.Profession?): NPCPersonality =
    generatePersonalityForProfession(profession)

fun sortRepairCandidates(npcs: List<AINPC>): List<AINPC> =
    npcs.sortedWith(
        compareBy<AINPC> { it.databaseId }
            .thenBy { it.uuid },
    )

fun markNpcPlannedForDeletion(npc: AINPC?, plannedDeletedNpcIds: MutableSet<Int>) {
    if (npc != null && npc.databaseId > 0) {
        plannedDeletedNpcIds.add(npc.databaseId)
    }
}

fun averageLocation(locations: List<Location>): Location {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    val world = locations[0].world

    for (location in locations) {
        x += location.x
        y += location.y
        z += location.z
    }

    val count = locations.size
    return Location(world, x / count, y / count, z / count)
}

fun belongsToChunk(npc: AINPC, chunk: Chunk): Boolean {
    if (npc.worldName == null) {
        return false
    }

    if (npc.worldName != chunk.world.name) {
        return false
    }

    val chunkX = floorToBlock(npc.x) shr 4
    val chunkZ = floorToBlock(npc.z) shr 4
    return chunk.x == chunkX && chunk.z == chunkZ
}

fun inferWorldBindingFromAnchors(npc: AINPC, worldAdmin: WorldAdminApi): NpcWorldBinding? {
    val homePlace = inferPlaceFromAnchor(worldAdmin, npc.homeAnchor)
    val workPlace = inferPlaceFromAnchor(worldAdmin, npc.workAnchor)
    val socialPlace = inferPlaceFromAnchor(worldAdmin, npc.socialAnchor)
    if (homePlace == null && workPlace == null && socialPlace == null) {
        return null
    }

    val homeNode = inferNodeFromAnchor(worldAdmin, npc.homeAnchor, homePlace)
    val workNode = inferNodeFromAnchor(worldAdmin, npc.workAnchor, workPlace)
    val socialNode = inferNodeFromAnchor(worldAdmin, npc.socialAnchor, socialPlace)

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
        "profile_backfill",
        0L,
        0L,
    )
}

fun anchorFromBinding(worldAdmin: WorldAdminApi, placeId: String?, nodeId: String?, role: String): AINPC.OwnedLocation? {
    val place = if (placeId.isNullOrBlank()) null else worldAdmin.getPlace(placeId)
    val node = if (nodeId.isNullOrBlank()) null else worldAdmin.getNode(nodeId)
    if (node != null) {
        val label = place?.displayName() ?: node.id()
        return AINPC.OwnedLocation(
            role,
            nodeLabel(node, label),
            node.worldName(),
            node.x(),
            node.y(),
            node.z(),
        )
    }
    if (place == null) {
        return null
    }

    return AINPC.OwnedLocation(
        role,
        place.displayName(),
        place.worldName(),
        placeCenterX(place),
        placeAnchorY(place),
        placeCenterZ(place),
    )
}

fun inferPlaceFromAnchor(worldAdmin: WorldAdminApi?, anchor: AINPC.OwnedLocation?): WorldPlaceInfo? {
    if (worldAdmin == null || anchor == null || anchor.worldName().isBlank()) {
        return null
    }
    return worldAdmin.findPlace(
        anchor.worldName(),
        floor(anchor.x()).toInt(),
        floor(anchor.y()).toInt(),
        floor(anchor.z()).toInt(),
    )
}

fun inferNodeFromAnchor(worldAdmin: WorldAdminApi?, anchor: AINPC.OwnedLocation?, place: WorldPlaceInfo?): WorldNodeInfo? {
    if (worldAdmin == null || anchor == null || anchor.worldName().isBlank()) {
        return null
    }

    return worldAdmin.findNodesNear(anchor.worldName(), anchor.x(), anchor.y(), anchor.z(), 2.5, 5)
        .stream()
        .filter { node -> place == null || node.placeId().isBlank() || node.placeId().equals(place.id(), ignoreCase = true) }
        .findFirst()
        .orElse(null)
}

fun findBedLocations(center: Location?, radius: Int, verticalRadius: Int): List<Location> {
    if (center == null || center.world == null) return emptyList()
    val seenBeds = HashSet<String>()
    val beds = ArrayList<Location>()
    val centerX = floorToBlock(center.x)
    val centerY = floorToBlock(center.y)
    val centerZ = floorToBlock(center.z)
    for (dx in -radius..radius) {
        for (dy in -verticalRadius..verticalRadius) {
            for (dz in -radius..radius) {
                val block = center.world.getBlockAt(centerX + dx, centerY + dy, centerZ + dz)
                if (!Tag.BEDS.isTagged(block.type)) continue
                val blockData = block.blockData
                if (blockData is Bed && blockData.part != Bed.Part.HEAD) continue
                val key = "${block.x}:${block.y}:${block.z}"
                if (seenBeds.add(key)) {
                    beds.add(block.location)
                }
            }
        }
    }
    return beds
}

fun findVillageSpawnLocation(snapshot: NpcVillageSnapshot?, offset: Int): Location? {
    if (snapshot == null) return null
    val beds = snapshot.bedLocations()
    if (beds.isEmpty()) return null
    val bed = beds[offset % beds.size]
    val world = bed.world ?: return null
    val candidates = arrayOf(
        intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(-1, 0),
        intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(2, 0),
        intArrayOf(-2, 0), intArrayOf(0, 2), intArrayOf(0, -2)
    )
    for (candidate in candidates) {
        val spawn = bed.clone().add(candidate[0] + 0.5, 1.0, candidate[1] + 0.5)
        val feet = world.getBlockAt(floorToBlock(spawn.x), floorToBlock(spawn.y), floorToBlock(spawn.z))
        val head = world.getBlockAt(floorToBlock(spawn.x), floorToBlock(spawn.y) + 1, floorToBlock(spawn.z))
        val ground = world.getBlockAt(floorToBlock(spawn.x), floorToBlock(spawn.y) - 1, floorToBlock(spawn.z))
        if (feet.isPassable && head.isPassable && !ground.isPassable) {
            return spawn
        }
    }
    return snapshot.center().clone().add(0.5, 0.0, 0.5)
}

fun mapProfessionToOccupation(profession: Villager.Profession?): String {
    if (profession == null || profession == Villager.Profession.NONE || profession == Villager.Profession.NITWIT) {
        return "resident"
    }
    val key = org.bukkit.Registry.VILLAGER_PROFESSION.getKey(profession)
    if (key == null) return "resident"
    return "minecraft:${key.key}"
}

fun findNearestBlock(center: Location?, horizontalRadius: Int, verticalRadius: Int, predicate: Predicate<Block>): Block? {
    if (center == null || center.world == null) return null
    var bestBlock: Block? = null
    var bestDistanceSquared = Double.MAX_VALUE
    val centerX = floorToBlock(center.x)
    val centerY = floorToBlock(center.y)
    val centerZ = floorToBlock(center.z)
    for (dx in -horizontalRadius..horizontalRadius) {
        for (dy in -verticalRadius..verticalRadius) {
            for (dz in -horizontalRadius..horizontalRadius) {
                val block = center.world.getBlockAt(centerX + dx, centerY + dy, centerZ + dz)
                if (!predicate.test(block)) continue
                val distanceSquared = block.location.distanceSquared(center)
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared
                    bestBlock = block
                }
            }
        }
    }
    return bestBlock
}

fun findNearestHomeAnchor(center: Location?): AINPC.OwnedLocation? {
    val bed = findNearestBlock(center, 8, 4) { block ->
        if (!Tag.BEDS.isTagged(block.type)) return@findNearestBlock false
        val blockData = block.blockData
        blockData !is Bed || blockData.part == Bed.Part.HEAD
    }
    return bed?.let {
        AINPC.OwnedLocation("home", "casa de langa pat", it.world.name, it.x + 0.5, it.y.toDouble(), it.z + 0.5)
    }
}

fun findNearestWorkAnchor(center: Location?, occupation: String?): AINPC.OwnedLocation? {
    var workstation = findNearestBlock(center, 6, 3) { block -> matchesOccupationWorkstation(occupation, block.type) }
    if (workstation == null) {
        workstation = findNearestBlock(center, 6, 3) { block -> isWorkstation(block.type) }
    }
    return workstation?.let {
        AINPC.OwnedLocation("work", describeWorkAnchor(occupation, it.type), it.world.name, it.x + 0.5, it.y.toDouble(), it.z + 0.5)
    }
}

fun findNearestSocialAnchor(center: Location?): AINPC.OwnedLocation? {
    val socialSpot = findNearestBlock(center, 12, 4) { block -> block.type == Material.BELL }
    return socialSpot?.let {
        AINPC.OwnedLocation("social", "piata satului", it.world.name, it.x + 0.5, it.y.toDouble(), it.z + 0.5)
    }
}

fun findNearbyWorkstation(center: Location?, horizontalRadius: Int, verticalRadius: Int): Material? {
    if (center == null || center.world == null) return null
    val materialWeights = HashMap<Material, Int>()
    val centerX = floorToBlock(center.x)
    val centerY = floorToBlock(center.y)
    val centerZ = floorToBlock(center.z)
    for (dx in -horizontalRadius..horizontalRadius) {
        for (dy in -verticalRadius..verticalRadius) {
            for (dz in -horizontalRadius..horizontalRadius) {
                val block = center.world.getBlockAt(centerX + dx, centerY + dy, centerZ + dz)
                val type = block.type
                if (!isWorkstation(type)) continue
                materialWeights.merge(type, 1, Int::plus)
            }
        }
    }
    return materialWeights.entries.maxByOrNull { it.value }?.key
}
