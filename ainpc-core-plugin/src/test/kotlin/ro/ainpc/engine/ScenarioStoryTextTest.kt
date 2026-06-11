package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScenarioStoryTextTest {
    @Test
    fun storyActionTypesNormalizeKnownAliasesOnly() {
        assertEquals("set_story_state", normalizeStoryActionType(entry(type = "set-flag")))
        assertEquals("set_story_state", normalizeStoryActionType(entry(type = "story_state")))
        assertEquals("record_story_event", normalizeStoryActionType(entry(type = "record event")))
        assertEquals("record_story_event", normalizeStoryActionType(entry(type = "event")))
        assertEquals("", normalizeStoryActionType(entry(type = "collect_item")))
        assertEquals("", normalizeStoryActionType(null))

        assertEquals(true, isQuestStoryAction(entry(type = "record_story_event")))
        assertEquals(false, isQuestStoryAction(entry(type = "collect_item")))
    }

    @Test
    fun questEntryMetadataPrefersDirectKeysThenNormalizedKeys() {
        val entry = entry(
            metadata = mapOf(
                "event_type" to "direct_event",
                "Actor Type" to "player",
                "blank_value" to " ",
            ),
        )

        assertEquals("", getQuestEntryMetadata(null, "event_type"))
        assertEquals("", getQuestEntryMetadata(entry))
        assertEquals("", getQuestEntryMetadata(entry, null, " "))
        assertEquals("direct_event", getQuestEntryMetadata(entry, "event_type"))
        assertEquals("player", getQuestEntryMetadata(entry, "actor_type"))
        assertEquals("", getQuestEntryMetadata(entry, "blank_value"))
        assertEquals("", getQuestEntryMetadata(entry, "missing"))
    }

    @Test
    fun normalizeReferenceRemovesMinecraftPrefixAndPunctuation() {
        assertEquals("", normalizeReference(null))
        assertEquals("", normalizeReference(" "))
        assertEquals("oak_log", normalizeReference("MINECRAFT:OAK_LOG"))
        assertEquals("npc_name_42", normalizeReference(" NPC Name #42 "))
        assertEquals("piata_centrala", normalizeReference("Piata---Centrala"))
    }

    @Test
    fun stripObjectivePrefixRemovesKnownQuestPrefixesOnly() {
        assertEquals("", stripObjectivePrefix(null))
        assertEquals("", stripObjectivePrefix(" "))
        assertEquals("Bob", stripObjectivePrefix("npc:Bob"))
        assertEquals("zombie", stripObjectivePrefix("entity:zombie"))
        assertEquals("minecraft:oak_log", stripObjectivePrefix("minecraft:oak_log"))
        assertEquals("quest:item", stripObjectivePrefix("quest:item"))
    }

    @Test
    fun storyScopeHelpersNormalizeAliasesAndCleanScopedIds() {
        assertEquals("region", normalizeStoryScope(" regional "))
        assertEquals("place", normalizeStoryScope("location"))
        assertEquals("", normalizeStoryScope("node"))

        assertEquals("region", detectStoryTargetScope("region:spawn"))
        assertEquals("place", detectStoryTargetScope("PLACE:market"))
        assertEquals("", detectStoryTargetScope("node:bell"))

        assertEquals("spawn", cleanStoryId(" region: spawn "))
        assertEquals("market", cleanStoryId("place:market"))
        assertEquals("node:bell", cleanStoryId(" node:bell "))
    }

    @Test
    fun parseStoryListTrimsAndSkipsBlankParts() {
        assertEquals(emptyList<String>(), parseStoryList(null))
        assertEquals(emptyList<String>(), parseStoryList(" "))
        assertEquals(listOf("alpha", "beta", "gamma"), parseStoryList(" alpha, , beta,gamma "))
    }

    @Test
    fun firstNonBlankReturnsOriginalFirstNonBlankValue() {
        assertEquals("", firstNonBlank())
        assertEquals("", firstNonBlank(null, " "))
        assertEquals(" beta ", firstNonBlank(null, " ", " beta ", "gamma"))
    }

    private fun entry(
        type: String? = "record_story_event",
        metadata: Map<String, String> = emptyMap(),
    ): FeaturePackLoader.QuestEntryDefinition =
        FeaturePackLoader.QuestEntryDefinition(
            type,
            "",
            1,
            "",
            metadata,
            emptyMap(),
            emptyMap(),
        )
}
