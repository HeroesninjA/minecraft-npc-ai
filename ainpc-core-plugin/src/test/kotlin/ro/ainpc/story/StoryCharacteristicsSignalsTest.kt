package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.environment.EnvironmentContext
import ro.ainpc.npc.NPCPersonality

class StoryCharacteristicsSignalsTest {
    @Test
    fun locationEnvironmentBecomesTypedStorySignalsWithoutMapping() {
        val context = EnvironmentContext.empty("sat").copy(
            weather = EnvironmentContext.Weather.SNOW,
            temperature = EnvironmentContext.Temperature.FREEZING,
            lightLevel = 3,
            specialEvents = listOf("bad\nsignal", "festival", "pana", "raid", "targ", "concert", "extra"),
        )

        assertEquals(
            listOf(
                "environment_time=morning",
                "environment_weather=snow",
                "environment_season=spring",
                "environment_temperature=freezing",
                "environment_light=3",
                "environment_events=festival,pana,raid,targ,concert",
            ),
            StoryCharacteristicsSignals.environment(context),
        )
        assertEquals(emptyList<String>(), StoryCharacteristicsSignals.environment(EnvironmentContext.empty()))
    }

    @Test
    fun npcPersonalityBecomesStableLowMediumHighSignals() {
        val personality = NPCPersonality(0.8, 0.2, 0.5, 0.7, 0.3)

        assertEquals(
            listOf(
                "npc_openness=high",
                "npc_conscientiousness=low",
                "npc_extraversion=medium",
                "npc_agreeableness=high",
                "npc_neuroticism=low",
            ),
            StoryCharacteristicsSignals.personality(personality),
        )
        assertEquals(emptyList<String>(), StoryCharacteristicsSignals.personality(null))
    }
}
