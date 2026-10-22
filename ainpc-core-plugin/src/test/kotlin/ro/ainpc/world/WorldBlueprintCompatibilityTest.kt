package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.story.RegionStoryState
import ro.ainpc.world.exterior.ExteriorStructureReport
import ro.ainpc.world.exterior.ExteriorStructureType

class WorldBlueprintCompatibilityTest {
    private val base = WorldBlueprint("world", "Poveste", mapOf("clima" to "Rece"), 4,
        mapOf("fierar" to WorldProfessionBlueprint(3, "Atelier")), "Piatra",
        mapOf("drumuri" to "Leaga satele"))
    private val blacksmith = WorldBlueprintProfessionProfile("blacksmith", "Fierar", listOf("smith"))

    @Test
    fun `catalog names and aliases connect draft professions to canonical workplace metadata`() {
        val places = listOf(
            place("forge", PlaceType.FORGE, "blacksmith"),
            place("smith-workshop", PlaceType.FORGE, "smith"),
            place("foreign", PlaceType.FORGE, "blacksmith", "nether"),
        )
        val plan = WorldBlueprintPlanner.compile(base, places, listOf(blacksmith))

        assertTrue(plan.compatibility.professionCatalogAvailable)
        assertTrue(plan.compatibility.issues.isEmpty())
        assertEquals("blacksmith", plan.professionPlans.single().runtimeProfessionId)
        assertEquals(2L, plan.professionPlans.single().mappedSlots)
        assertEquals(1L, plan.professionPlans.single().capacityGap)
        assertEquals(setOf("fierar"), base.professions.keys)
    }

    @Test
    fun `unknown profession is reported only when the catalog is available`() {
        val places = listOf(place("forge", PlaceType.FORGE, "fierar"))
        val unavailable = WorldBlueprintPlanner.compile(base, places, null)
        val emptyCatalog = WorldBlueprintPlanner.compile(base, places, emptyList())

        assertFalse(unavailable.compatibility.professionCatalogAvailable)
        assertTrue(unavailable.compatibility.issues.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.UNKNOWN_PROFESSION,
            emptyCatalog.compatibility.issues.single().code)
        assertEquals(1L, emptyCatalog.professionPlans.single().mappedSlots)
        assertEquals(1L, unavailable.professionPlans.single().mappedSlots)
    }

    @Test
    fun `ambiguous draft aliases do not assign workplace capacity`() {
        val profiles = listOf(blacksmith, WorldBlueprintProfessionProfile("armorer", "Fierar", listOf("smith")))
        val plan = WorldBlueprintPlanner.compile(base,
            listOf(place("forge", PlaceType.FORGE, "fierar")), profiles)

        assertEquals(WorldBlueprintCompatibilityCode.AMBIGUOUS_PROFESSION,
            plan.compatibility.issues.single().code)
        assertEquals(0L, plan.professionPlans.single().mappedSlots)
        assertTrue(plan.compatibility.resolvedProfessionIds.isEmpty())
    }

    @Test
    fun `canonical ids take priority while ambiguous workplace aliases stay unassigned`() {
        val draft = base.copy(professions = mapOf("blacksmith" to WorldProfessionBlueprint(2, "Atelier")))
        val profiles = listOf(blacksmith,
            WorldBlueprintProfessionProfile("armorer", "Armurier", listOf("blacksmith", "smith")))
        val plan = WorldBlueprintPlanner.compile(draft, listOf(
            place("forge", PlaceType.FORGE, "blacksmith"),
            place("ambiguous", PlaceType.FORGE, "smith"),
        ), profiles)

        assertEquals("blacksmith", plan.professionPlans.single().runtimeProfessionId)
        assertEquals(1L, plan.professionPlans.single().mappedSlots)
        assertTrue(plan.compatibility.issues.isEmpty())
        assertTrue(plan.mappingWarnings.single().contains("ambiguous"))
    }

    @Test
    fun `duplicate draft references to one profile never double count a workplace`() {
        val draft = base.copy(professions = base.professions + ("blacksmith" to WorldProfessionBlueprint(3, "Duplicat")))
        val plan = WorldBlueprintPlanner.compile(draft,
            listOf(place("forge", PlaceType.FORGE, "blacksmith")), listOf(blacksmith))

        assertEquals(WorldBlueprintCompatibilityCode.DUPLICATE_PROFESSION,
            plan.compatibility.issues.single().code)
        assertTrue(plan.professionPlans.all { it.mappedSlots == 0L })
        assertTrue(plan.mappingWarnings.single().contains("ambigua"))
    }

    @Test
    fun `conflicting workplace metadata stays ambiguous even when only one profession is in the draft`() {
        val places = listOf(place("conflicting", PlaceType.FORGE, "blacksmith", occupation = "farmer"))
        val profiles = listOf(blacksmith, WorldBlueprintProfessionProfile("farmer", "Fermier"))
        val plan = WorldBlueprintPlanner.compile(base, places, profiles)

        assertEquals(0L, plan.professionPlans.single().mappedSlots)
        assertTrue(plan.mappingWarnings.single().contains("conflicting"))
    }

    @Test
    fun `required and forbidden types inspect only places in the draft world`() {
        val draft = base.copy(mapRules = mapOf(
            "required_place_types" to "HOUSE, forge,house",
            "forbidden_place_types" to "camp",
            "drumuri" to "Ocoleste lacurile",
        ))
        val plan = WorldBlueprintPlanner.compile(draft, listOf(
            place("forge", PlaceType.FORGE, "fierar"),
            place("camp", PlaceType.CAMP),
            place("foreign-house", PlaceType.HOUSE, world = "nether"),
            place("foreign-camp", PlaceType.CAMP, world = "nether"),
        ))

        assertEquals(2, plan.compatibility.structuredRuleCount)
        assertEquals(1, plan.declarativeRuleCount)
        val missing = plan.compatibility.issues.single { it.code == WorldBlueprintCompatibilityCode.REQUIRED_PLACE_TYPE_MISSING }
        assertEquals("house", missing.subject)
        val forbidden = plan.compatibility.issues.single { it.code == WorldBlueprintCompatibilityCode.FORBIDDEN_PLACE_TYPE_PRESENT }
        assertEquals("camp", forbidden.subject)
        assertTrue(forbidden.message.contains("1 locuri"))
        assertFalse(forbidden.message.contains("foreign"))
    }

    @Test
    fun `contradictions are detected without inventing missing places when mapping is unavailable`() {
        val draft = base.copy(mapRules = mapOf(
            "required_place_types" to "forge,house",
            "forbidden_place_types" to "forge",
        ))
        val plan = WorldBlueprintPlanner.compile(draft, null)

        assertFalse(plan.mappingAvailable)
        assertEquals(WorldBlueprintCompatibilityCode.CONFLICTING_PLACE_TYPE_RULES,
            plan.compatibility.issues.single().code)
        assertEquals("forge", plan.compatibility.issues.single().subject)
    }

    @Test
    fun `suggestion preview reports only newly introduced structured conflicts`() {
        val existing = base.copy(mapRules = mapOf(
            "required_place_types" to "forge",
            "forbidden_place_types" to "forge",
            "required_region_types" to "settlement",
            "allowed_structure_biomes" to "minecraft:plains",
            "architecture_block_palette" to "invalid_existing_palette",
        ))
        val candidate = existing.copy(mapRules = existing.mapRules + mapOf(
            "forbidden_region_types" to "settlement",
            "forbidden_structure_biomes" to "minecraft:plains",
        ))

        val introduced = WorldBlueprintCompatibility.introducedStructuredRuleConflicts(existing, candidate)

        assertEquals(setOf(
            WorldBlueprintCompatibilityCode.CONFLICTING_REGION_TYPE_RULES to "settlement",
            WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_BIOME_RULES to "minecraft:plains",
        ), introduced.map { it.code to it.subject }.toSet())
        assertTrue(WorldBlueprintCompatibility.introducedStructuredRuleConflicts(existing,
            existing.copy(mapRules = existing.mapRules + ("drumuri" to "Evita lacurile"))).isEmpty())
    }

    @Test
    fun `invalid structured rules are rejected as a whole instead of becoming custom places`() {
        listOf("forge,unknown", "forge,", "forge house", "castel", "").forEach { value ->
            val draft = base.copy(mapRules = mapOf("required_place_types" to value))
            val plan = WorldBlueprintPlanner.compile(draft, emptyList())

            assertEquals(WorldBlueprintCompatibilityCode.INVALID_PLACE_TYPE_RULE,
                plan.compatibility.issues.single().code, value)
            assertEquals(1, plan.compatibility.structuredRuleCount)
        }
        val custom = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf("required_place_types" to "custom")),
            listOf(place("custom-place", PlaceType.CUSTOM)))
        assertTrue(custom.compatibility.issues.isEmpty())
    }

    @Test
    fun `free text rules are not interpreted as structured instructions`() {
        val draft = base.copy(mapRules = mapOf("drumuri" to "Evita forge si camp langa case"))
        val plan = WorldBlueprintPlanner.compile(draft, emptyList())

        assertTrue(plan.compatibility.issues.isEmpty())
        assertEquals(0, plan.compatibility.structuredRuleCount)
        assertEquals(1, plan.declarativeRuleCount)
    }

    @Test
    fun `region type rules use only mapped regions in the draft world`() {
        val draft = base.copy(mapRules = mapOf(
            "required_region_types" to "SETTLEMENT,castle,settlement",
            "forbidden_region_types" to "dungeon",
            "drumuri" to "Leaga asezarile",
        ))
        val regions = listOf(
            region("castle", RegionType.CASTLE),
            region("dungeon", RegionType.DUNGEON),
            region("foreign-settlement", RegionType.SETTLEMENT, "nether"),
            region("foreign-dungeon", RegionType.DUNGEON, "nether"),
        )
        val plan = WorldBlueprintPlanner.compile(draft, emptyList(), mappedRegions = regions)

        assertTrue(plan.compatibility.regionMappingAvailable)
        assertEquals(2, plan.compatibility.structuredRuleCount)
        assertEquals(1, plan.declarativeRuleCount)
        assertEquals("settlement", plan.compatibility.issues.single {
            it.code == WorldBlueprintCompatibilityCode.REQUIRED_REGION_TYPE_MISSING
        }.subject)
        val forbidden = plan.compatibility.issues.single {
            it.code == WorldBlueprintCompatibilityCode.FORBIDDEN_REGION_TYPE_PRESENT
        }
        assertEquals("dungeon", forbidden.subject)
        assertTrue(forbidden.message.contains("1 regiuni"))
        assertFalse(forbidden.message.contains("foreign"))
    }

    @Test
    fun `region contradictions are reported without region mapping`() {
        val draft = base.copy(mapRules = mapOf(
            "required_region_types" to "castle,dungeon",
            "forbidden_region_types" to "castle",
        ))
        val plan = WorldBlueprintPlanner.compile(draft, emptyList())

        assertFalse(plan.compatibility.regionMappingAvailable)
        assertEquals(WorldBlueprintCompatibilityCode.CONFLICTING_REGION_TYPE_RULES,
            plan.compatibility.issues.single().code)
        assertEquals("castle", plan.compatibility.issues.single().subject)
    }

    @Test
    fun `invalid region ids reject the whole rule and do not silently become custom`() {
        listOf("castle,unknown", "castle,", "castel", "").forEach { value ->
            val draft = base.copy(mapRules = mapOf("required_region_types" to value))
            val plan = WorldBlueprintPlanner.compile(draft, emptyList(), mappedRegions = emptyList())

            assertEquals(WorldBlueprintCompatibilityCode.INVALID_REGION_TYPE_RULE,
                plan.compatibility.issues.single().code, value)
        }
        val custom = WorldBlueprintPlanner.compile(
            base.copy(mapRules = mapOf("required_region_types" to "custom")),
            emptyList(), mappedRegions = listOf(region("custom-region", RegionType.CUSTOM)),
        )
        assertTrue(custom.compatibility.issues.isEmpty())
    }

    @Test
    fun `a castle room does not satisfy a castle region requirement`() {
        val draft = base.copy(mapRules = mapOf("required_region_types" to "castle"))
        val plan = WorldBlueprintPlanner.compile(draft,
            listOf(place("throne-room", PlaceType.CASTLE_ROOM)), mappedRegions = emptyList())

        assertEquals(WorldBlueprintCompatibilityCode.REQUIRED_REGION_TYPE_MISSING,
            plan.compatibility.issues.single().code)
    }

    @Test
    fun `story region links report mapped configuration without activating story`() {
        val draft = base.copy(mapRules = mapOf("story_region_ids" to "village, village, keep"))
        val regions = listOf(
            WorldRegionInfo("village", "Sat", "world", RegionType.SETTLEMENT.id,
                0, 0, 0, 1, 1, 1, emptyList(), StoryMode.STATIC, "peace",
                listOf("peace", "siege"), mapOf("peace" to "Satul este linistit")),
            region("keep", RegionType.CASTLE),
        )
        val plan = WorldBlueprintPlanner.compile(draft, emptyList(), mappedRegions = regions)

        assertTrue(plan.compatibility.issues.isEmpty())
        assertFalse(plan.compatibility.storyStateAvailable)
        assertEquals(1, plan.compatibility.structuredRuleCount)
        assertEquals(0, plan.declarativeRuleCount)
        assertEquals(listOf("village", "keep"), plan.compatibility.storyRegionLinks.map { it.id })
        val village = plan.compatibility.storyRegionLinks.first()
        assertEquals("peace", village.stateKey)
        assertEquals(StoryMode.STATIC, village.mode)
        assertEquals(2, village.poolSize)
        assertTrue(village.hasStateDescription)
        assertEquals("mapping", village.source)
        assertFalse(plan.compatibility.storyRegionLinks.last().hasStateDescription)
    }

    @Test
    fun `persistent story state takes precedence over mapping in the preview`() {
        val draft = base.copy(mapRules = mapOf("story_region_ids" to "village"))
        val mapped = WorldRegionInfo("village", "Sat", "world", RegionType.SETTLEMENT.id,
            0, 0, 0, 1, 1, 1, emptyList(), StoryMode.STATIC, "peace",
            listOf("peace"), mapOf("peace" to "Liniste"))
        val persistent = RegionStoryState("village", StoryMode.ROTATIVE, "siege",
            listOf("siege", "recovery"), mapOf("story_description_siege" to "Sub asediu"),
            1L, 2L, "admin", "story")
        val plan = WorldBlueprintPlanner.compile(draft, emptyList(), mappedRegions = listOf(mapped),
            persistentStoryStates = mapOf("village" to persistent))

        assertTrue(plan.compatibility.storyStateAvailable)
        val link = plan.compatibility.storyRegionLinks.single()
        assertEquals("persistent", link.source)
        assertEquals("siege", link.stateKey)
        assertEquals(StoryMode.ROTATIVE, link.mode)
        assertEquals(2, link.poolSize)
        assertTrue(link.hasStateDescription)
    }

    @Test
    fun `story region references are scoped to the draft world`() {
        val draft = base.copy(mapRules = mapOf("story_region_ids" to "village,keep"))
        val plan = WorldBlueprintPlanner.compile(draft, emptyList(), mappedRegions = listOf(
            region("village", RegionType.SETTLEMENT, "nether"),
            region("keep", RegionType.CASTLE),
        ))

        assertEquals(listOf("keep"), plan.compatibility.storyRegionLinks.map { it.id })
        assertEquals("village", plan.compatibility.issues.single {
            it.code == WorldBlueprintCompatibilityCode.STORY_REGION_MISSING
        }.subject)
    }

    @Test
    fun `malformed story region references fail without mapping but absent mapping does not imply missing regions`() {
        val invalid = WorldBlueprintPlanner.compile(
            base.copy(mapRules = mapOf("story_region_ids" to "village,")), emptyList())
        val unavailable = WorldBlueprintPlanner.compile(
            base.copy(mapRules = mapOf("story_region_ids" to "village")), emptyList())

        assertEquals(WorldBlueprintCompatibilityCode.INVALID_STORY_REGION_RULE,
            invalid.compatibility.issues.single().code)
        assertTrue(invalid.compatibility.storyRegionLinks.isEmpty())
        assertFalse(unavailable.compatibility.regionMappingAvailable)
        assertTrue(unavailable.compatibility.issues.isEmpty())
        assertTrue(unavailable.compatibility.storyRegionLinks.isEmpty())
    }

    @Test
    fun `story region preview caps references and ignores placeholder descriptions`() {
        val tooMany = (1..65).joinToString(",") { "region$it" }
        val invalid = WorldBlueprintPlanner.compile(
            base.copy(mapRules = mapOf("story_region_ids" to tooMany)), emptyList(), mappedRegions = emptyList())
        val placeholder = WorldRegionInfo("village", "Sat", "world", RegionType.SETTLEMENT.id,
            0, 0, 0, 1, 1, 1, emptyList(), StoryMode.EVOLUTIVE, "default",
            emptyList(), mapOf("default" to "<none>"))
        val valid = WorldBlueprintPlanner.compile(
            base.copy(mapRules = mapOf("story_region_ids" to "village")), emptyList(),
            mappedRegions = listOf(placeholder))

        assertEquals(WorldBlueprintCompatibilityCode.INVALID_STORY_REGION_RULE,
            invalid.compatibility.issues.single().code)
        assertFalse(valid.compatibility.storyRegionLinks.single().hasStateDescription)
    }

    @Test
    fun `exterior rules inspect classified regions only in the draft world`() {
        val draft = base.copy(mapRules = mapOf(
            "required_exterior_types" to "castle,watchtower",
            "forbidden_exterior_types" to "camp",
        ))
        val plan = WorldBlueprintPlanner.compile(draft, emptyList(), mappedRegions = listOf(
            region("keep", RegionType.CASTLE),
            region("camp", RegionType.SETTLEMENT),
            region("foreign-tower", RegionType.CUSTOM, "nether"),
        ), exteriorReports = listOf(
            exteriorReport("keep", ExteriorStructureType.CASTLE, listOf("Lipseste poarta")),
            exteriorReport("camp", ExteriorStructureType.CAMP),
            exteriorReport("foreign-tower", ExteriorStructureType.WATCHTOWER),
        ))

        assertTrue(plan.compatibility.exteriorAnalysisAvailable)
        assertEquals(2, plan.compatibility.structuredRuleCount)
        assertEquals(0, plan.declarativeRuleCount)
        assertEquals(setOf(
            WorldBlueprintCompatibilityCode.REQUIRED_EXTERIOR_TYPE_MISSING,
            WorldBlueprintCompatibilityCode.REQUIRED_EXTERIOR_TYPE_UNREADY,
            WorldBlueprintCompatibilityCode.FORBIDDEN_EXTERIOR_TYPE_PRESENT,
        ), plan.compatibility.issues.map { it.code }.toSet())
        assertEquals("watchtower", plan.compatibility.issues.single {
            it.code == WorldBlueprintCompatibilityCode.REQUIRED_EXTERIOR_TYPE_MISSING
        }.subject)
        assertFalse(plan.compatibility.issues.any { "foreign-tower" in it.message })
    }

    @Test
    fun `exterior contradictions and invalid types are detected without analysis`() {
        val conflicting = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            "required_exterior_types" to "castle",
            "forbidden_exterior_types" to "castle",
        )), emptyList())
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            "required_exterior_types" to "castle,unknown",
        )), emptyList(), mappedRegions = emptyList(), exteriorReports = emptyList())
        val unavailable = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            "required_exterior_types" to "watchtower",
        )), emptyList(), mappedRegions = emptyList())

        assertEquals(WorldBlueprintCompatibilityCode.CONFLICTING_EXTERIOR_TYPE_RULES,
            conflicting.compatibility.issues.single().code)
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_EXTERIOR_TYPE_RULE,
            invalid.compatibility.issues.single().code)
        assertFalse(unavailable.compatibility.exteriorAnalysisAvailable)
        assertTrue(unavailable.compatibility.issues.isEmpty())
    }

    @Test
    fun `structure biome rules are recognized and validated without world samples`() {
        val valid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains,custom:moon_forest",
            WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY to "desert",
        )), emptyList())
        val conflicting = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains",
            WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY to "minecraft:plains",
        )), emptyList())
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains,",
        )), emptyList())

        assertEquals(2, valid.compatibility.structuredRuleCount)
        assertEquals(0, valid.declarativeRuleCount)
        assertTrue(valid.compatibility.issues.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_BIOME_RULES,
            conflicting.compatibility.issues.single().code)
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_BIOME_RULE,
            invalid.compatibility.issues.single().code)
    }

    @Test
    fun `structure surface span is recognized and validated without terrain samples`() {
        val valid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureSurfaceSpanRule.KEY to "12",
        )), emptyList())
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureSurfaceSpanRule.KEY to "-2",
        )), emptyList())

        assertEquals(1, valid.compatibility.structuredRuleCount)
        assertEquals(0, valid.declarativeRuleCount)
        assertTrue(valid.compatibility.issues.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_SURFACE_SPAN_RULE,
            invalid.compatibility.issues.single().code)
    }

    @Test
    fun `structure region positions rule is validated before bounds are chosen`() {
        val valid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureRegionPositionsRule.KEY to "1000000",
        )), emptyList())
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureRegionPositionsRule.KEY to "0",
        )), emptyList())

        assertEquals(1, valid.compatibility.structuredRuleCount)
        assertEquals(0, valid.declarativeRuleCount)
        assertTrue(valid.compatibility.issues.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_REGION_POSITIONS_RULE,
            invalid.compatibility.issues.single().code)
    }

    @Test
    fun `structure non air rule is recognized before bounds are chosen`() {
        val valid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "0",
        )), emptyList())
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "unknown",
        )), emptyList())

        assertEquals(1, valid.compatibility.structuredRuleCount)
        assertEquals(0, valid.declarativeRuleCount)
        assertTrue(valid.compatibility.issues.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_NON_AIR_RULE,
            invalid.compatibility.issues.single().code)
    }

    @Test
    fun `structure fluid rule is recognized before bounds are chosen`() {
        val valid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureFluidRule.KEY to "0",
        )), emptyList())
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintStructureFluidRule.KEY to "unknown",
        )), emptyList())

        assertEquals(1, valid.compatibility.structuredRuleCount)
        assertEquals(0, valid.declarativeRuleCount)
        assertTrue(valid.compatibility.issues.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_FLUID_RULE,
            invalid.compatibility.issues.single().code)
    }

    @Test
    fun `base support rule is recognized before bounds are chosen`() {
        val valid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintBaseSupportRule.KEY to "0",
        )), emptyList())
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintBaseSupportRule.KEY to "6",
        )), emptyList())

        assertEquals(1, valid.compatibility.structuredRuleCount)
        assertEquals(0, valid.declarativeRuleCount)
        assertTrue(valid.compatibility.issues.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_BASE_SUPPORT_RULE,
            invalid.compatibility.issues.single().code)
    }

    @Test
    fun `world seed is a validated structured rule`() {
        val valid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintWorldSeedRule.KEY to Long.MIN_VALUE.toString(),
        )), emptyList())
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintWorldSeedRule.KEY to "9223372036854775808",
        )), emptyList())

        assertEquals(1, valid.compatibility.structuredRuleCount)
        assertEquals(0, valid.declarativeRuleCount)
        assertTrue(valid.compatibility.issues.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_WORLD_SEED_RULE,
            invalid.compatibility.issues.single().code)
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_WORLD_SEED_RULE,
            WorldBlueprintCompatibility.structuredRuleSyntaxIssues(WorldBlueprintWorldSeedRule.KEY, "bad").single().code)
    }

    @Test
    fun `world type and vanilla structures are validated structured rules`() {
        val valid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintWorldGenerationRules.TYPE_KEY to "flat",
            WorldBlueprintWorldGenerationRules.STRUCTURES_KEY to "false",
        )), emptyList())
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintWorldGenerationRules.TYPE_KEY to "custom",
            WorldBlueprintWorldGenerationRules.STRUCTURES_KEY to "no",
        )), emptyList())

        assertEquals(2, valid.compatibility.structuredRuleCount)
        assertTrue(valid.compatibility.issues.isEmpty())
        assertEquals(setOf(WorldBlueprintCompatibilityCode.INVALID_WORLD_TYPE_RULE,
            WorldBlueprintCompatibilityCode.INVALID_WORLD_STRUCTURES_RULE),
            invalid.compatibility.issues.map { it.code }.toSet())
    }

    @Test
    fun `forbidden structure blocks rule is validated without inspecting terrain`() {
        val valid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintForbiddenStructureBlocks.KEY to "chest,stone",
        )), emptyList(), isKnownStructureBlock = { it == "CHEST" || it == "STONE" })
        val invalid = WorldBlueprintPlanner.compile(base.copy(mapRules = mapOf(
            WorldBlueprintForbiddenStructureBlocks.KEY to "stone,not_a_block",
        )), emptyList(), isKnownStructureBlock = { it == "CHEST" || it == "STONE" })

        assertEquals(1, valid.compatibility.structuredRuleCount)
        assertEquals(0, valid.declarativeRuleCount)
        assertTrue(valid.compatibility.issues.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_FORBIDDEN_STRUCTURE_BLOCKS_RULE,
            invalid.compatibility.issues.single().code)
    }

    @Test
    fun `invalid architecture palette is reported as a structured rule`() {
        val draft = base.copy(mapRules = mapOf(
            WorldBlueprintArchitecturePalette.KEY to List(17) { "stone_bricks" }.joinToString(),
        ))
        val plan = WorldBlueprintPlanner.compile(draft, emptyList())

        assertEquals(1, plan.compatibility.structuredRuleCount)
        assertEquals(0, plan.declarativeRuleCount)
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_ARCHITECTURE_PALETTE_RULE,
            plan.compatibility.issues.single().code)
        assertTrue(plan.compatibility.architecturePalette.isEmpty())
    }

    private fun place(
        id: String, type: PlaceType, profession: String? = null, world: String = "world", occupation: String? = null,
    ) =
        WorldPlaceInfo(id, "village", id, world, type, 0, 0, 0, 1, 1, 1, emptyList(), null, true,
            buildMap {
                profession?.let { put("profession", it) }
                occupation?.let { put("occupation", it) }
            })

    private fun region(id: String, type: RegionType, world: String = "world") =
        WorldRegionInfo(id, id, world, type.id, 0, 0, 0, 1, 1, 1,
            emptyList(), StoryMode.EVOLUTIVE, "default", emptyList())

    private fun exteriorReport(id: String, type: ExteriorStructureType, errors: List<String> = emptyList()) =
        ExteriorStructureReport(id, id, type, emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), errors)
}
