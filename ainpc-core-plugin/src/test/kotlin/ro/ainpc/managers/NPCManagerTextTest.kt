package ro.ainpc.managers

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Villager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCPersonality
import ro.ainpc.utils.NPCNameGenerator
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.lang.reflect.Proxy
import java.util.Collections
import java.util.Random
import java.util.UUID
import java.util.function.Predicate

class NPCManagerTextTest {
    @Test
    fun nodeMatchingNormalizesTypeAndMetadataTokens() {
        val node = nodeInfo(
            typeId = "npc-spawn",
            metadata = mapOf("display_name" to "Patul mare", "role" to "Home")
        )

        assertTrue(nodeMatchesAny(node, "npc_spawn"))
        assertTrue(nodeMatchesAny(node, "home"))
        assertFalse(nodeMatchesAny(node, "work"))
    }

    @Test
    fun nodeLabelPrefersMetadataThenFallbackThenId() {
        assertEquals("Patul mare", nodeLabel(nodeInfo(metadata = mapOf("display_name" to "Patul mare")), "fallback"))
        assertEquals("fallback", nodeLabel(nodeInfo(), "fallback"))
        assertEquals("node_1", nodeLabel(nodeInfo(), ""))
    }

    @Test
    fun nodePriorityKeepsAnchorRoleOrdering() {
        assertEquals(0, nodePriority(nodeInfo(typeId = "bed"), "home"))
        assertEquals(1, nodePriority(nodeInfo(typeId = "npc-spawn"), "home"))
        assertEquals(0, nodePriority(nodeInfo(metadata = mapOf("role" to "workstation")), "work"))
        assertEquals(2, nodePriority(nodeInfo(metadata = mapOf("kind" to "counter")), "work"))
        assertEquals(0, nodePriority(nodeInfo(typeId = "meeting-point"), "social"))
        assertEquals(-1, nodePriority(nodeInfo(typeId = "unknown"), "home"))
    }

    @Test
    fun placeClassificationKeepsHomeWorkAndSocialRules() {
        assertTrue(isHomePlace(placeInfo(placeType = PlaceType.HOUSE)))
        assertTrue(isHomePlace(placeInfo(placeType = PlaceType.CUSTOM, metadata = mapOf("role" to "HOME"))))

        assertFalse(isWorkPlace(placeInfo(placeType = PlaceType.HOUSE, tags = listOf("work")), "fierar"))
        assertTrue(isWorkPlace(placeInfo(placeType = PlaceType.FORGE), "fierar"))
        assertTrue(
            isWorkPlace(
                placeInfo(placeType = PlaceType.CUSTOM, metadata = mapOf("purpose" to "work")),
                "fierar"
            )
        )

        assertTrue(isSocialPlace(placeInfo(placeType = PlaceType.MARKET)))
        assertTrue(isSocialPlace(placeInfo(placeType = PlaceType.CUSTOM, metadata = mapOf("anchor" to "social"))))
        assertFalse(isSocialPlace(placeInfo(placeType = PlaceType.HOUSE)))
    }

    @Test
    fun placeCenterDistanceUsesHorizontalCenterAndAnchorY() {
        val place = placeInfo()
        val node = nodeInfo(typeId = "custom").let {
            WorldNodeInfo(
                it.id(),
                it.regionId(),
                it.placeId(),
                it.typeId(),
                it.worldName(),
                8.0,
                63.0,
                9.0,
                it.radius(),
                it.metadata(),
            )
        }

        assertEquals(5.0, placeCenterX(place))
        assertEquals(61.0, placeAnchorY(place))
        assertEquals(5.0, placeCenterZ(place))
        assertEquals(29.0, distanceSquaredToPlaceCenter(place, node))
        assertEquals(29.0, distanceSquaredToPlaceCenter(place, Location(null, 8.0, 63.0, 9.0)))
        assertEquals(50.0, distanceSquared(0.0, 0.0, 0.0, 3.0, 4.0, 5.0))
    }

    @Test
    fun ownedLocationConversionUsesPlaceCenterOrPreferredNode() {
        val place = placeInfo()
        val node = nodeInfo(
            typeId = "npc-spawn",
            metadata = mapOf("display_name" to "Patul Mare")
        )

        val placeAnchor = toOwnedLocation("home", place, null)
        assertEquals("home", placeAnchor.type())
        assertEquals("Place", placeAnchor.label())
        assertEquals("world", placeAnchor.worldName())
        assertEquals(5.0, placeAnchor.x())
        assertEquals(61.0, placeAnchor.y())
        assertEquals(5.0, placeAnchor.z())

        val nodeAnchor = toOwnedLocation("home", place, node)
        assertEquals("home", nodeAnchor.type())
        assertEquals("Patul Mare", nodeAnchor.label())
        assertEquals("world", nodeAnchor.worldName())
        assertEquals(1.0, nodeAnchor.x())
        assertEquals(2.0, nodeAnchor.y())
        assertEquals(3.0, nodeAnchor.z())

        val directNodeAnchor = toOwnedLocation("social", nodeInfo(), "fallback")
        assertEquals("fallback", directNodeAnchor.label())
    }

    @Test
    fun ownershipMatchesUuidDatabaseIdAndNormalizedName() {
        val uuid = UUID.fromString("11111111-2222-3333-4444-555555555555")
        val npc = AINPC(null).apply {
            this.uuid = uuid
            databaseId = 42
            name = "Mara Fierar"
        }

        assertTrue(isOwnedByNpc(placeInfo(ownerNpcId = uuid.toString()), npc))
        assertTrue(isOwnedByNpc(placeInfo(ownerNpcId = "npc_42"), npc))
        assertTrue(isOwnedByNpc(placeInfo(ownerNpcId = "mara_fierar"), npc))
        assertFalse(isOwnedByNpc(placeInfo(ownerNpcId = ""), npc))
        assertFalse(isOwnedByNpc(placeInfo(ownerNpcId = "alt_npc"), npc))
        assertFalse(isOwnedByNpc(placeInfo(ownerNpcId = "mara_fierar"), null))
        assertEquals("mara_fierar", normalizeOwnerKey(" Mara Fierar "))
    }

    @Test
    fun ownedLocationJsonRoundTripsAndRejectsBlankWorld() {
        val root = JsonObject()
        val anchor = AINPC.OwnedLocation("home", "Casa", "world", 1.5, 64.0, 2.5)

        writeOwnedLocation(root, "home", anchor)
        writeOwnedLocation(root, "missing", null)

        val read = readOwnedLocation(root, "home")!!
        assertEquals("home", read.type())
        assertEquals("Casa", read.label())
        assertEquals("world", read.worldName())
        assertEquals(1.5, read.x())
        assertEquals(64.0, read.y())
        assertEquals(2.5, read.z())
        assertFalse(root.has("missing"))
        assertEquals(null, readOwnedLocation(root, "missing"))

        val blankWorld = JsonObject().apply {
            add(
                "work",
                JsonObject().apply {
                    addProperty("world", "")
                    addProperty("x", 1.0)
                },
            )
        }
        assertEquals(null, readOwnedLocation(blankWorld, "work"))
    }

    @Test
    fun genericOccupationRecognizesOnlyFallbackRoles() {
        assertTrue(isGenericOccupation(null))
        assertTrue(isGenericOccupation(""))
        assertTrue(isGenericOccupation(" Locuitor "))
        assertTrue(isGenericOccupation("villager"))
        assertTrue(isGenericOccupation("resident"))
        assertTrue(isGenericOccupation("localnic"))
        assertFalse(isGenericOccupation("fierar"))
        assertFalse(isGenericOccupation("minecraft:farmer"))
    }

    @Test
    fun environmentOccupationPreferenceOnlyOverridesGenericOrSameMappedOccupation() {
        assertFalse(shouldPreferEnvironmentOccupation(null, "fierar", null))
        assertFalse(shouldPreferEnvironmentOccupation(null, "fierar", ""))
        assertTrue(shouldPreferEnvironmentOccupation(null, "resident", "fierar"))
        assertTrue(shouldPreferEnvironmentOccupation(null, "minecraft:farmer", "MINECRAFT:FARMER"))
        assertFalse(shouldPreferEnvironmentOccupation(null, "minecraft:farmer", "fierar"))
    }

    @Test
    fun occupationChoicePreservesMappedInferredAndThemedPriority() {
        assertEquals("fierar", resolveOccupationChoice(null, "fierar", null, "brutar"))
        assertEquals("MINECRAFT:FARMER", resolveOccupationChoice(null, "minecraft:farmer", "MINECRAFT:FARMER", null))
        assertEquals("fierar", resolveOccupationChoice(null, "resident", "fierar", "brutar"))
        assertEquals("brutar", resolveOccupationChoice(null, "resident", null, "brutar"))
        assertEquals("resident", resolveOccupationChoice(null, "resident", null, null))
        assertEquals("resident", resolveOccupationChoice(null, null, null, null))
    }

    @Test
    fun villagerSeededRandomUsesUuidBitsAndEnvironmentInferenceStaysDisabled() {
        val villagerUuid = uuid("00000000-0000-0001-0000-000000000010")
        val expected = Random(villagerUuid.mostSignificantBits xor villagerUuid.leastSignificantBits)
        val actual = createVillagerSeededRandom(villager(null, uniqueId = villagerUuid))

        assertEquals(expected.nextInt(), actual.nextInt())
        assertEquals(expected.nextInt(1000), actual.nextInt(1000))
        assertEquals(null, inferOccupationFromEnvironment(villager(null, uniqueId = villagerUuid)))
        assertEquals(null, inferOccupationFromEnvironment(null))
    }

    @Test
    fun uniquifyNpcNameTrimsFallbacksAndAppendsFirstFreeSuffix() {
        assertEquals("Mara", uniquifyNpcName(" Mara ", Predicate { false }))
        assertEquals("NPC", uniquifyNpcName(" ", Predicate { false }))

        val takenNames = setOf("Mara", "Mara 2", "Mara 3")
        assertEquals("Mara 4", uniquifyNpcName("Mara", Predicate { it in takenNames }))
    }

    @Test
    fun generatedAutoNameUsesFirstFreeShuffledPredefinedName() {
        val expectedCandidates = ArrayList(NPCNameGenerator.predefinedNames("female"))
        Collections.shuffle(expectedCandidates, Random(17))
        val takenFirst = expectedCandidates.first()
        val expected = expectedCandidates[1]

        assertEquals(
            expected,
            generateUniqueAutoName("female", Random(17), Predicate { it == takenFirst }),
        )
    }

    @Test
    fun npcNameTakenIsBlankSafeTrimmedAndCaseInsensitive() {
        val existing = listOf(
            AINPC(null).apply { name = " Mara " },
        )

        assertFalse(isNpcNameTaken(null, existing))
        assertFalse(isNpcNameTaken(" ", existing))
        assertTrue(isNpcNameTaken("mara", existing))
        assertTrue(isNpcNameTaken(" MARA ", existing))
        assertFalse(isNpcNameTaken("Dorin", existing))
    }

    @Test
    fun workstationHelpersKeepMaterialRulesAndLabelFormatting() {
        assertTrue(isWorkstation(Material.COMPOSTER))
        assertTrue(isWorkstation(Material.SMITHING_TABLE))
        assertTrue(isWorkstation(Material.BELL))
        assertFalse(isWorkstation(Material.DIRT))

        assertTrue(matchesOccupationWorkstation("fierar", Material.ANVIL))
        assertFalse(matchesOccupationWorkstation("fierar", Material.DIRT))
        assertEquals("smithing table", describeWorkAnchor("fierar", Material.SMITHING_TABLE))
    }

    @Test
    fun genderResolverKeepsFemaleOnlySpecialCase() {
        assertEquals("female", resolveGender("female"))
        assertEquals("female", resolveGender("FEMALE"))
        assertEquals("male", resolveGender("male"))
        assertEquals("male", resolveGender(""))
        assertEquals("male", resolveGender(null))
    }

    @Test
    fun floorToBlockKeepsMathFloorSemantics() {
        assertEquals(12, floorToBlock(12.9))
        assertEquals(12, floorToBlock(12.0))
        assertEquals(-13, floorToBlock(-12.1))
    }

    @Test
    fun safeNpcNameFallsBackForMissingNames() {
        assertEquals("NPC", safeNpcName(null))
        assertEquals(
            "NPC",
            safeNpcName(
                AINPC(null).apply {
                    name = " "
                },
            ),
        )
        assertEquals(
            "Mara",
            safeNpcName(
                AINPC(null).apply {
                    name = "Mara"
                },
            ),
        )
    }

    @Test
    fun fallbackAnchorsUseNpcNameOccupationAndCenterCoordinates() {
        val center = Location(world("sat"), 12.5, 64.0, -3.5)
        val namedNpc = AINPC(null).apply {
            name = "Mara"
            occupation = "fierar"
        }

        val home = createFallbackHomeAnchor(namedNpc, center)
        assertEquals("home", home.type())
        assertEquals("casa lui Mara", home.label())
        assertEquals("sat", home.worldName())
        assertEquals(12.5, home.x())
        assertEquals(64.0, home.y())
        assertEquals(-3.5, home.z())

        val work = createFallbackWorkAnchor(namedNpc, center)
        assertEquals("work", work.type())
        assertEquals("locul de munca de fierar", work.label())
        assertEquals("sat", work.worldName())
        assertEquals(12.5, work.x())
        assertEquals(64.0, work.y())
        assertEquals(-3.5, work.z())

        val genericNpc = AINPC(null).apply {
            name = "Dorin"
            occupation = "resident"
        }
        assertEquals("locul de munca al lui Dorin", createFallbackWorkAnchor(genericNpc, center).label())
    }

    @Test
    fun sourceKeyOwnerReplacementKeepsCanonicalRules() {
        assertFalse(shouldReplacePersistedSourceKeyOwner(0, 10, currentOwnerExists = true))
        assertTrue(shouldReplacePersistedSourceKeyOwner(7, 0, currentOwnerExists = false))
        assertTrue(shouldReplacePersistedSourceKeyOwner(7, 10, currentOwnerExists = false))
        assertTrue(shouldReplacePersistedSourceKeyOwner(7, 10, currentOwnerExists = true))
        assertFalse(shouldReplacePersistedSourceKeyOwner(12, 10, currentOwnerExists = true))
    }

    @Test
    fun sourceKeyCandidatePreferenceUsesDatabaseIdThenUuid() {
        val lowId = npcRecord(databaseId = 7, uuid = uuid("00000000-0000-0000-0000-000000000009"))
        val highId = npcRecord(databaseId = 10, uuid = uuid("00000000-0000-0000-0000-000000000001"))
        val noIdLowUuid = npcRecord(uuid = uuid("00000000-0000-0000-0000-000000000001"))
        val noIdHighUuid = npcRecord(uuid = uuid("00000000-0000-0000-0000-000000000002"))

        assertFalse(isPreferredSourceKeyCandidate(null, lowId))
        assertTrue(isPreferredSourceKeyCandidate(lowId, null))
        assertTrue(isPreferredSourceKeyCandidate(lowId, highId))
        assertFalse(isPreferredSourceKeyCandidate(highId, lowId))
        assertTrue(isPreferredSourceKeyCandidate(lowId, noIdLowUuid))
        assertFalse(isPreferredSourceKeyCandidate(noIdLowUuid, lowId))
        assertTrue(isPreferredSourceKeyCandidate(noIdLowUuid, noIdHighUuid))
        assertFalse(isPreferredSourceKeyCandidate(noIdHighUuid, noIdLowUuid))
    }

    @Test
    fun sameNpcRecordUsesIdentityDatabaseIdThenUuid() {
        val first = npcRecord(databaseId = 7, uuid = uuid("00000000-0000-0000-0000-000000000001"))
        val sameDatabaseId = npcRecord(databaseId = 7, uuid = uuid("00000000-0000-0000-0000-000000000002"))
        val sameUuid = npcRecord(databaseId = 0, uuid = first.uuid)
        val different = npcRecord(databaseId = 8, uuid = uuid("00000000-0000-0000-0000-000000000003"))

        assertTrue(isSameNpcRecord(first, first))
        assertFalse(isSameNpcRecord(first, null))
        assertTrue(isSameNpcRecord(first, sameDatabaseId))
        assertTrue(isSameNpcRecord(first, sameUuid))
        assertFalse(isSameNpcRecord(first, different))
    }

    @Test
    fun generatedBackstoryKeepsOccupationFallbackText() {
        assertEquals(
            "Mara are rolul fierar si participa la viata comunitatii.",
            generateBackstory("Mara", "fierar", null),
        )
        assertEquals(
            "Mara are rolul resident si participa la viata comunitatii.",
            generateBackstory("Mara", " ", null),
        )
        assertEquals(
            "null are rolul resident si participa la viata comunitatii.",
            generateBackstory(null, null, null),
        )
    }

    @Test
    fun namesMatchIsCaseInsensitiveAndNullSafe() {
        assertTrue(namesMatch("Mara", "mara"))
        assertFalse(namesMatch("Mara", "Dorin"))
        assertFalse(namesMatch(null, "Mara"))
        assertFalse(namesMatch("Mara", null))
    }

    @Test
    fun npcPlannedForDeletionRequiresPositiveIdInSet() {
        assertTrue(isNpcPlannedForDeletion(npcRecord(databaseId = 7), setOf(7, 10)))
        assertFalse(isNpcPlannedForDeletion(npcRecord(databaseId = 0), setOf(0)))
        assertFalse(isNpcPlannedForDeletion(npcRecord(databaseId = 9), setOf(7, 10)))
        assertFalse(isNpcPlannedForDeletion(null, setOf(7)))
    }

    @Test
    fun locationHelpersKeepWorldDistanceAndFloorRules() {
        val world = world("sat")
        val otherWorld = world("alt")

        assertTrue(isSameNpcLocation(Location(world, 1.0, 64.0, 1.0), Location(world, 2.0, 64.0, 2.0)))
        assertFalse(isSameNpcLocation(Location(world, 1.0, 64.0, 1.0), Location(world, 4.0, 64.0, 1.0)))
        assertFalse(isSameNpcLocation(Location(world, 1.0, 64.0, 1.0), Location(otherWorld, 1.0, 64.0, 1.0)))
        assertFalse(isSameNpcLocation(Location(null, 1.0, 64.0, 1.0), Location(world, 1.0, 64.0, 1.0)))

        assertEquals("sat 12,64,-13", formatLocation(Location(world, 12.9, 64.0, -12.1)))
        assertEquals("<locatie necunoscuta>", formatLocation(Location(null, 0.0, 0.0, 0.0)))
        assertEquals("sat:2:-2", buildVillageKey(Location(world, 64.0, 64.0, -33.0)))
        assertEquals("unknown", buildVillageKey(null))
    }

    @Test
    fun profileSummaryKeepsFallbacksTraitsAndBackstoryTruncation() {
        assertEquals("Acest NPC este locuitor, 30 ani, barbat.", buildProfileSummary(AINPC(null)))

        val longBackstory = "A".repeat(220)
        val npc = AINPC(null).apply {
            name = "Mara"
            occupation = "fierar"
            age = 32
            gender = "female"
            personality = NPCPersonality(0.8, 0.5, 0.5, 0.5, 0.5)
            backstory = longBackstory
        }

        val summary = buildProfileSummary(npc)
        assertTrue(summary.startsWith("Mara este fierar, 32 ani, femeie, trasaturi dominante: curios. "))
        assertTrue(summary.endsWith("..."))
        assertTrue(summary.length < longBackstory.length + 80)
    }

    @Test
    fun villagerDisplayNameSerializesAndTrimsCustomName() {
        assertEquals("Mara", getVillagerDisplayName(villager(Component.text(" Mara "))))
        assertEquals(null, getVillagerDisplayName(villager(null)))
    }

    @Test
    fun spawnStateJsonKeepsRuntimeCoordinatesAndChunkKeys() {
        val npc = npcRecord(
            databaseId = 42,
            uuid = uuid("00000000-0000-0000-0000-000000000042"),
        ).apply {
            spawned = true
            sourceKey = "sat:mara"
            worldName = "world"
            x = 33.9
            y = 64.0
            z = -1.1
            yaw = 90.0f
            pitch = 10.0f
        }

        val before = System.currentTimeMillis()
        val state = buildSpawnState(npc)
        val after = System.currentTimeMillis()

        assertTrue(state.get("spawned").asBoolean)
        assertEquals("00000000-0000-0000-0000-000000000042", state.get("entity_uuid").asString)
        assertEquals(42, state.get("database_id").asInt)
        assertEquals("sat:mara", state.get("source_key").asString)
        assertEquals("world", state.get("world").asString)
        assertEquals(33.9, state.get("x").asDouble)
        assertEquals(64.0, state.get("y").asDouble)
        assertEquals(-1.1, state.get("z").asDouble)
        assertEquals(90.0f, state.get("yaw").asFloat)
        assertEquals(10.0f, state.get("pitch").asFloat)
        assertEquals(2, state.get("chunk_x").asInt)
        assertEquals(-1, state.get("chunk_z").asInt)
        assertTrue(state.get("restorable").asBoolean)
        assertTrue(state.get("updated_at").asLong in before..after)
    }

    @Test
    fun profileDataJsonKeepsNestedRuntimeAndProfileFields() {
        val gson = Gson()
        val npc = npcRecord(
            databaseId = 77,
            uuid = uuid("00000000-0000-0000-0000-000000000077"),
        ).apply {
            name = "Mara"
            displayName = "Mara Fierar"
            profileSource = "auto"
            profileVersion = 3
            sourceKey = "sat:mara"
            worldName = "world"
            x = 33.9
            y = 64.0
            z = -1.1
            yaw = 90.0f
            pitch = 10.0f
            occupation = "fierar"
            backstory = "Mara lucreaza la fierarie."
            age = 32
            gender = "female"
            spawned = true
            traits.add("curios")
            traits.add("")
            hungerLevel = 41
            currentGoal = "lucreaza"
            homeAnchor = AINPC.OwnedLocation("home", "Casa", "world", 1.0, 64.0, 2.0)
        }

        val profile = gson.fromJson(buildProfileData(npc, gson), JsonObject::class.java)

        assertEquals(77, profile.get("npc_id").asInt)
        assertEquals("00000000-0000-0000-0000-000000000077", profile.get("uuid").asString)
        assertEquals("Mara", profile.get("name").asString)
        assertEquals("Mara Fierar", profile.get("display_name").asString)
        assertEquals("auto", profile.get("profile_source").asString)
        assertEquals(3, profile.get("profile_version").asInt)
        assertEquals("sat:mara", profile.get("source_key").asString)
        assertEquals("world", profile.get("world").asString)
        assertEquals("fierar", profile.get("occupation").asString)
        assertEquals("female", profile.get("gender").asString)
        assertTrue(profile.get("profile_summary").asString.startsWith("Mara este fierar, 32 ani, femeie."))
        assertEquals(1, profile.getAsJsonArray("traits").size())
        assertEquals("curios", profile.getAsJsonArray("traits")[0].asString)
        assertEquals(2, profile.getAsJsonObject("spawn_state").get("chunk_x").asInt)
        assertEquals(-1, profile.getAsJsonObject("spawn_state").get("chunk_z").asInt)
        assertEquals(41, profile.getAsJsonObject("simulation").get("hunger_level").asInt)
        assertEquals("lucreaza", profile.getAsJsonObject("simulation").get("current_goal").asString)
        assertEquals(0.5, profile.getAsJsonObject("personality").get("openness").asDouble)
        assertTrue(profile.getAsJsonObject("emotions").has("short_description"))
        assertEquals("Casa", profile.getAsJsonObject("owned_locations").getAsJsonObject("home").get("label").asString)
        assertFalse(profile.getAsJsonObject("owned_locations").has("work"))
    }

    @Test
    fun generatedPersonalityHelpersReturnClampedTraits() {
        assertPersonalityTraitsAreClamped(generatePersonalityForProfession(null))
        assertPersonalityTraitsAreClamped(generatePersonalityForOccupation(null, null))
        assertPersonalityTraitsAreClamped(generatePersonalityForOccupation("fierar", null))
    }

    @Test
    fun repairCandidateHelpersSortAndMarkByCanonicalIds() {
        val highId = npcRecord(databaseId = 10, uuid = uuid("00000000-0000-0000-0000-000000000001"))
        val lowIdHighUuid = npcRecord(databaseId = 7, uuid = uuid("00000000-0000-0000-0000-000000000003"))
        val lowIdLowUuid = npcRecord(databaseId = 7, uuid = uuid("00000000-0000-0000-0000-000000000002"))
        val noId = npcRecord(databaseId = 0, uuid = uuid("00000000-0000-0000-0000-000000000004"))

        assertEquals(
            listOf(noId, lowIdLowUuid, lowIdHighUuid, highId),
            sortRepairCandidates(listOf(highId, lowIdHighUuid, noId, lowIdLowUuid)),
        )

        val plannedIds = mutableSetOf<Int>()
        markNpcPlannedForDeletion(highId, plannedIds)
        markNpcPlannedForDeletion(noId, plannedIds)
        markNpcPlannedForDeletion(null, plannedIds)
        assertEquals(setOf(10), plannedIds)
    }

    @Test
    fun jsonReadersUseFallbackForMissingOrNonPrimitiveValues() {
        val json = JsonObject().apply {
            addProperty("count", 7)
            addProperty("label", "sat")
            add("object", JsonObject())
        }

        assertEquals(7, readInt(json, "count", 1))
        assertEquals("sat", readString(json, "label", "fallback"))
        assertEquals("fallback", readString(json, "object", "fallback"))
        assertEquals(5L, readLong(json, "missing", 5L))
    }

    private fun nodeInfo(
        typeId: String = "custom",
        metadata: Map<String, String> = emptyMap(),
    ): WorldNodeInfo =
        WorldNodeInfo(
            "node_1",
            "region_1",
            "place_1",
            typeId,
            "world",
            1.0,
            2.0,
            3.0,
            0.5,
            metadata,
        )

    private fun placeInfo(
        placeType: PlaceType = PlaceType.CUSTOM,
        tags: List<String> = emptyList(),
        ownerNpcId: String = "",
        metadata: Map<String, String> = emptyMap(),
    ): WorldPlaceInfo =
        WorldPlaceInfo(
            "place_1",
            "region_1",
            "Place",
            "world",
            placeType,
            0,
            60,
            0,
            10,
            70,
            10,
            tags,
            ownerNpcId,
            true,
            metadata,
        )

    private fun npcRecord(
        databaseId: Int = 0,
        uuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    ): AINPC =
        AINPC(null).apply {
            this.databaseId = databaseId
            this.uuid = uuid
        }

    private fun uuid(value: String): UUID =
        UUID.fromString(value)

    private fun assertPersonalityTraitsAreClamped(personality: NPCPersonality) {
        assertTrue(personality.openness in 0.0..1.0)
        assertTrue(personality.conscientiousness in 0.0..1.0)
        assertTrue(personality.extraversion in 0.0..1.0)
        assertTrue(personality.agreeableness in 0.0..1.0)
        assertTrue(personality.neuroticism in 0.0..1.0)
    }

    private fun world(name: String): World =
        Proxy.newProxyInstance(
            World::class.java.classLoader,
            arrayOf(World::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "getName" -> name
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "World($name)"
                else -> throw UnsupportedOperationException("World.${method.name}")
            }
        } as World

    private fun villager(
        customName: Component?,
        uniqueId: UUID = uuid("00000000-0000-0000-0000-000000000001"),
    ): Villager =
        Proxy.newProxyInstance(
            Villager::class.java.classLoader,
            arrayOf(Villager::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "customName" -> if (args == null || args.isEmpty()) customName else Unit
                "getUniqueId" -> uniqueId
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "Villager($customName)"
                else -> throw UnsupportedOperationException("Villager.${method.name}")
            }
        } as Villager
}
