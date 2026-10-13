package ro.ainpc.story

import ro.ainpc.environment.EnvironmentContext
import ro.ainpc.npc.NPCPersonality

internal object StoryCharacteristicsSignals {
    private val eventLabelPattern = Regex("""[\p{L}\p{N}_ .:-]{1,64}""")

    fun environment(context: EnvironmentContext?): List<String> {
        if (context == null || context.worldName.isBlank()) return emptyList()
        return buildList {
            add("environment_time=${context.timeOfDay.id}")
            add("environment_weather=${context.weather.id}")
            add("environment_season=${context.season.id}")
            add("environment_temperature=${context.temperature.id}")
            add("environment_light=${context.lightLevel.coerceIn(0, 15)}")
            val events = context.specialEvents.map(String::trim)
                .filter(eventLabelPattern::matches).distinct().take(5)
            if (events.isNotEmpty()) add("environment_events=${events.joinToString(",")}")
        }
    }

    fun personality(personality: NPCPersonality?): List<String> {
        if (personality == null) return emptyList()
        return listOf(
            "npc_openness=${level(personality.openness)}",
            "npc_conscientiousness=${level(personality.conscientiousness)}",
            "npc_extraversion=${level(personality.extraversion)}",
            "npc_agreeableness=${level(personality.agreeableness)}",
            "npc_neuroticism=${level(personality.neuroticism)}",
        )
    }

    private fun level(value: Double): String = when {
        value < 0.35 -> "low"
        value > 0.65 -> "high"
        else -> "medium"
    }
}
