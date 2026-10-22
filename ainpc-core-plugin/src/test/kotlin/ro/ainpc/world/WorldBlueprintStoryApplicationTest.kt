package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.BlueprintStoryApplicationEntry
import ro.ainpc.story.BlueprintStoryApplicationVerification
import ro.ainpc.story.BlueprintStoryApplicationVerificationStatus

class WorldBlueprintStoryApplicationTest {
    private val fingerprint = "a".repeat(64)
    private val draft = WorldBlueprint("world", "Povestea completa a lumii",
        mapOf("clima" to "rece"), 4, mapOf("fierar" to WorldProfessionBlueprint(2, "atelier")),
        "Piatra si lemn", mapOf("story_region_ids" to "keep"),
        WorldBlueprintApproval(fingerprint, "admin", 1L))
    private val region = WorldRegionInfo("keep", "Cetate", "world", RegionType.CASTLE.id,
        0, 64, 0, 10, 80, 10, emptyList(), StoryMode.STATIC, "peace", listOf("peace", "siege"), emptyMap())

    @Test
    fun `controlled update requires confirmed changed content and an unchanged mapped application`() {
        val state = RegionStoryState("keep", StoryMode.STATIC, "peace", listOf("peace", "siege"),
            mapOf("story_description_peace" to "Poveste veche"), 1L, 1L, "admin", "blueprint")
        val entry = BlueprintStoryApplicationEntry(7L, "batch", "world", "keep", "b".repeat(64),
            "c".repeat(64), 1L, "admin")
        val verification = BlueprintStoryApplicationVerification(BlueprintStoryApplicationVerificationStatus.MATCH,
            entry, state)
        WorldBlueprintStoryApplication.validateUpdate(draft, "keep", 7L, fingerprint, fingerprint,
            listOf(region), verification)

        listOf(draft.copy(approval = null), draft.copy(story = ""), draft.copy(story = "Poveste veche"),
            draft.copy(mapRules = mapOf("story_region_ids" to "other"))).forEach { candidate ->
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintStoryApplication.validateUpdate(candidate, "keep", 7L, fingerprint, fingerprint,
                    listOf(region), verification)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.validateUpdate(draft, "keep", 8L, fingerprint, fingerprint,
                listOf(region), verification)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.validateUpdate(draft, "keep", 7L, "d".repeat(64), fingerprint,
                listOf(region), verification)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.validateUpdate(draft, "keep", 7L, fingerprint, fingerprint,
                listOf(region), verification.copy(status = BlueprintStoryApplicationVerificationStatus.MATCH_WITH_EVENTS))
        }
        listOf(null, emptyList(), listOf(WorldRegionInfo("keep", "Cetate", "nether", RegionType.CASTLE.id,
            0, 64, 0, 10, 80, 10, emptyList(), StoryMode.STATIC, "peace", listOf("peace", "siege"))))
            .forEach { mapping ->
                assertThrows(IllegalArgumentException::class.java) {
                    WorldBlueprintStoryApplication.validateUpdate(draft, "keep", 7L, fingerprint, fingerprint,
                        mapping, verification)
                }
            }
    }

    @Test
    fun `applied mapping inspection distinguishes unavailable missing moved ambiguous and changed configuration`() {
        val state = RegionStoryState("keep", StoryMode.STATIC, "peace", listOf("peace", "siege"),
            mapOf("story_description_peace" to draft.story), 1L, 1L, "admin", "blueprint")
        fun mapped(worldName: String = "world", mode: StoryMode = StoryMode.STATIC,
                   stateKey: String = "peace", pool: List<String> = listOf("peace", "siege"),
                   descriptions: Map<String, String> = emptyMap()) =
            WorldRegionInfo("keep", "Cetate", worldName, RegionType.CASTLE.id,
                0, 64, 0, 10, 80, 10, emptyList(), mode, stateKey, pool, descriptions)
        fun inspect(regions: Collection<WorldRegionInfo>?) =
            WorldBlueprintStoryApplication.inspectAppliedMapping("world", "keep", state, regions)

        assertEquals(WorldBlueprintStoryMappingStatus.MAPPING_UNAVAILABLE, inspect(null))
        assertEquals(WorldBlueprintStoryMappingStatus.REGION_MISSING, inspect(emptyList()))
        assertEquals(WorldBlueprintStoryMappingStatus.WRONG_WORLD, inspect(listOf(mapped(worldName = "nether"))))
        assertEquals(WorldBlueprintStoryMappingStatus.AMBIGUOUS, inspect(listOf(region, region)))
        assertEquals(WorldBlueprintStoryMappingStatus.STORY_CONFIG_CHANGED,
            inspect(listOf(mapped(mode = StoryMode.EVOLUTIVE))))
        assertEquals(WorldBlueprintStoryMappingStatus.STORY_CONFIG_CHANGED,
            inspect(listOf(mapped(stateKey = "siege"))))
        assertEquals(WorldBlueprintStoryMappingStatus.STORY_CONFIG_CHANGED,
            inspect(listOf(mapped(pool = listOf("peace")))))
        assertEquals(WorldBlueprintStoryMappingStatus.DESCRIPTION_PRESENT,
            inspect(listOf(mapped(descriptions = mapOf("peace" to "Alta poveste")))))
        assertEquals(WorldBlueprintStoryMappingStatus.MATCH,
            inspect(listOf(mapped(descriptions = mapOf("peace" to "<none>")))))
    }

    @Test
    fun `confirmed revision prepares one mapped region without changing its mode or pool`() {
        val request = WorldBlueprintStoryApplication.prepare(draft, "keep", fingerprint, fingerprint,
            listOf(region), emptyMap())

        assertEquals("keep", request.regionId)
        assertEquals(StoryMode.STATIC, request.storyMode)
        assertEquals("peace", request.stateKey)
        assertEquals(listOf("peace", "siege"), request.storyPool)
        assertEquals(mapOf("story_description_peace" to draft.story), request.variables)
        assertEquals("world_blueprint:world:$fingerprint", request.source)
    }

    @Test
    fun `unapproved changed or incomplete drafts are rejected`() {
        val candidates = listOf(
            draft.copy(approval = null),
            draft.copy(story = ""),
            draft.copy(mapRules = emptyMap()),
        )
        candidates.forEach { candidate ->
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintStoryApplication.prepare(candidate, "keep", fingerprint, fingerprint,
                    listOf(region), emptyMap())
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepare(draft, "keep", "b".repeat(64), fingerprint,
                listOf(region), emptyMap())
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepare(draft, "keep", fingerprint, "b".repeat(64),
                listOf(region), emptyMap())
        }
    }

    @Test
    fun `undeclared missing foreign or existing story targets are rejected`() {
        val existing = RegionStoryState("keep", StoryMode.EVOLUTIVE, "default", emptyList(),
            emptyMap(), 1L, 2L, "admin", "test")
        val mappedDescription = WorldRegionInfo("keep", "Cetate", "world", RegionType.CASTLE.id,
            0, 64, 0, 10, 80, 10, emptyList(), StoryMode.STATIC, "peace", emptyList(),
            mapOf("siege" to "Descriere existenta"))
        val foreign = WorldRegionInfo("keep", "Cetate", "nether", RegionType.CASTLE.id,
            0, 64, 0, 10, 80, 10, emptyList(), StoryMode.STATIC, "peace", emptyList(), emptyMap())

        val inputs = listOf(
            Triple("other", listOf(region), emptyMap()),
            Triple("keep", emptyList(), emptyMap()),
            Triple("keep", listOf(foreign), emptyMap()),
            Triple("keep", listOf(region), mapOf("keep" to existing)),
            Triple("keep", listOf(mappedDescription), emptyMap()),
        )
        inputs.forEach { (regionId, regions, states) ->
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintStoryApplication.prepare(draft, regionId, fingerprint, fingerprint,
                    regions, states)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepare(draft, "keep", fingerprint, fingerprint,
                listOf(region), null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepare(draft, "keep", fingerprint, fingerprint,
                null, null)
        }
    }

    @Test
    fun `batch prepares all declared regions only when every target is clear`() {
        val batchDraft = draft.copy(mapRules = mapOf("story_region_ids" to "keep,village,keep"))
        val village = WorldRegionInfo("village", "Sat", "world", RegionType.SETTLEMENT.id,
            0, 64, 0, 10, 80, 10, emptyList(), StoryMode.EVOLUTIVE, "default", emptyList(), emptyMap())

        val requests = WorldBlueprintStoryApplication.prepareAll(batchDraft, fingerprint, fingerprint,
            listOf(region, village), emptyMap())

        assertEquals(listOf("keep", "village"), requests.map { it.regionId })
        assertEquals(listOf("world_blueprint:world:$fingerprint", "world_blueprint:world:$fingerprint"),
            requests.map { it.source })
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepareAll(batchDraft, fingerprint, fingerprint,
                listOf(region), emptyMap())
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepareAll(batchDraft, fingerprint, fingerprint,
                listOf(region, village), mapOf("village" to RegionStoryState("village",
                    StoryMode.EVOLUTIVE, "default", emptyList(), emptyMap(), 1L, 2L, "admin", "manual")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepareAll(batchDraft, "b".repeat(64), fingerprint,
                listOf(region, village), emptyMap())
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepareAll(draft.copy(mapRules = mapOf("story_region_ids" to "keep,")),
                fingerprint, fingerprint, listOf(region, village), emptyMap())
        }
    }

    @Test
    fun `selected batch validates only chosen declared regions`() {
        val batchDraft = draft.copy(mapRules = mapOf("story_region_ids" to "keep,village,ruins"))
        val village = WorldRegionInfo("village", "Sat", "world", RegionType.SETTLEMENT.id,
            0, 64, 0, 10, 80, 10, emptyList(), StoryMode.EVOLUTIVE, "default", emptyList(), emptyMap())
        val existingKeep = RegionStoryState("keep", StoryMode.STATIC, "peace", emptyList(),
            emptyMap(), 1L, 2L, "admin", "manual")
        val selected = WorldBlueprintStoryApplication.parseRegionSelection(" village ")

        val requests = WorldBlueprintStoryApplication.prepareSelected(batchDraft, selected,
            fingerprint, fingerprint, listOf(region, village), mapOf("keep" to existingKeep))

        assertEquals(listOf("village"), requests.map { it.regionId })
        assertEquals(StoryMode.EVOLUTIVE, requests.single().storyMode)
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepareSelected(batchDraft, listOf("village", "keep"),
                fingerprint, fingerprint, listOf(region, village), mapOf("keep" to existingKeep))
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepareSelected(batchDraft, listOf("other"),
                fingerprint, fingerprint, listOf(region, village), emptyMap())
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStoryApplication.prepareSelected(batchDraft, listOf("village"),
                "b".repeat(64), fingerprint, listOf(region, village), emptyMap())
        }
    }

    @Test
    fun `selected batch rejects empty duplicate and oversized region lists`() {
        assertEquals(listOf("keep", "village"),
            WorldBlueprintStoryApplication.parseRegionSelection(" keep , village "))
        listOf("", "keep,", ",keep", "keep,,village", "keep,keep",
            (1..65).joinToString(",") { "region$it" }).forEach { raw ->
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintStoryApplication.parseRegionSelection(raw)
            }
        }
    }

    @Test
    fun `update batch selection requires distinct regions and positive application ids`() {
        assertEquals(mapOf("keep" to 7L, "village" to 11L),
            WorldBlueprintStoryApplication.parseRegionApplicationSelection(" keep:7 , village:11 "))
        listOf("", "keep", "keep:", ":7", "keep:0", "keep:-1", "keep:nope",
            "keep:7,keep:8", "keep:7, keep:8", "keep:7,",
            (1..65).joinToString(",") { "region$it:$it" }).forEach { raw ->
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintStoryApplication.parseRegionApplicationSelection(raw)
            }
        }
    }

    @Test
    fun `restore batch selection requires distinct regions and two different positive ids`() {
        assertEquals(mapOf("keep" to (8L to 3L), "village" to (12L to 5L)),
            WorldBlueprintStoryApplication.parseRegionRestorationSelection(" keep:8:3 , village:12:5 "))
        listOf("", "keep", "keep:8", "keep:8:", ":8:3", "keep:0:3", "keep:8:0",
            "keep:8:8", "keep:foo:3", "keep:8:bar", "keep:8:3,keep:9:4",
            "keep:8:3,", (1..65).joinToString(",") { "region$it:${it + 1}:$it" }).forEach { raw ->
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintStoryApplication.parseRegionRestorationSelection(raw)
            }
        }
    }
}
