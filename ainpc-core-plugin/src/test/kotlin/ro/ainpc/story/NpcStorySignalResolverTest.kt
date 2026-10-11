package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.engine.QuestScoringResolver

class NpcStorySignalResolverTest {
    private val resolver = NpcStorySignalResolver()

    @Test
    fun generatedNarrativeRolesBecomeQuestDemandSignals() {
        val signals = resolver.collect("""
            {"narrative":{"quest_role":"informant","social_role":"gossip_hub",
            "backstory_seed":"secret narrative","family_id":"family_1"}}
        """.trimIndent())
        val context = StoryContextSnapshot("Mara", "hangiu", "Player", null,
            null, null, emptyList(), emptyList(), signals, emptyList())

        assertEquals(listOf("npc_quest_role=informant", "npc_social_role=gossip_hub"), signals)
        assertEquals(signals, QuestScoringResolver().storyDemandSignals(context))
    }

    @Test
    fun missingMalformedOrUnsafeProfileDoesNotAddSignals() {
        for (profile in listOf<String?>(null, "", "not-json", "[]", "{}",
            """{"narrative":{"quest_role":42,"social_role":"gossip hub"}}""",
            """{"narrative":{"quest_role":"a=b","social_role":{"value":"merchant"}}}""")) {
            assertEquals(emptyList<String>(), resolver.collect(profile), profile)
        }
    }
}
