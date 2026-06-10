package ro.ainpc.ai

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCState
import ro.ainpc.topology.TopologyCategory
import java.text.Normalizer
import java.util.EnumMap
import java.util.Locale
import java.util.Optional

/**
 * Rezolva intrebarile factuale despre NPC fara a depinde de modelul AI.
 */
object NpcFactResolver {
    @JvmStatic
    fun resolve(playerMessage: String?, facts: NpcFacts?): Optional<String> {
        if (playerMessage.isNullOrBlank() || facts == null) {
            return Optional.empty()
        }

        val intents = detectIntents(playerMessage)
        if (intents.isEmpty()) {
            return Optional.empty()
        }

        val answers = mutableListOf<String>()
        for (intent in intents) {
            val answer = resolveIntent(intent, facts)
            if (!answer.isNullOrBlank()) {
                answers.add(answer)
            }
        }

        if (answers.isEmpty()) {
            return Optional.of("Nu stiu sigur.")
        }

        return Optional.of(joinNaturally(answers))
    }

    @JvmStatic
    fun describeCurrentActivity(occupation: String?, state: NPCState?): String {
        val safeOccupation = occupation?.trim() ?: ""

        if (state == null) {
            return if (safeOccupation.isBlank()) {
                "astept putin"
            } else {
                "imi vad de treburile mele de $safeOccupation"
            }
        }

        return when (state) {
            NPCState.IDLE, NPCState.WAITING -> if (safeOccupation.isBlank()) {
                "astept putin"
            } else {
                "imi vad de treburile mele de $safeOccupation"
            }
            NPCState.WALKING -> "merg"
            NPCState.RUNNING -> "alerg"
            NPCState.TALKING -> "vorbesc"
            NPCState.LISTENING -> "ascult"
            NPCState.TRADING -> "fac schimburi"
            NPCState.WORKING, NPCState.CRAFTING -> if (safeOccupation.isBlank()) {
                "lucrez"
            } else {
                "lucrez la treburile mele de $safeOccupation"
            }
            NPCState.FARMING -> "lucrez pamantul"
            NPCState.MINING -> "minez"
            NPCState.FISHING -> "pescuiesc"
            NPCState.SOCIALIZING -> "socializez"
            NPCState.CELEBRATING -> "sarbatoresc"
            NPCState.MOURNING -> "jelesc"
            NPCState.ARGUING -> "ma cert"
            NPCState.COMBAT -> "lupt"
            NPCState.FLEEING -> "fug"
            NPCState.GUARDING -> "pazesc zona"
            NPCState.PATROLLING -> "patrulez prin zona"
            NPCState.SLEEPING -> "dorm"
            NPCState.RESTING -> "ma odihnesc"
            NPCState.EATING -> "mananc"
            NPCState.DRINKING -> "beau"
            NPCState.PANICKING -> "incerc sa nu intru in panica"
            NPCState.CURIOUS -> "incerc sa aflu ce se intampla"
            NPCState.HIDING -> "ma ascund"
            NPCState.PRAYING -> "ma rog"
            NPCState.QUEST_GIVING -> "caut pe cineva care sa ma ajute"
            NPCState.FOLLOWING -> "urmez pe cineva"
        }
    }

    @JvmStatic
    fun describeLocation(npc: AINPC?, context: NPCContext?): String {
        if (context != null) {
            val topologyCategory = context.topologyCategory
            if (topologyCategory != null && topologyCategory != TopologyCategory.UNKNOWN) {
                val topology = topologyCategory.displayName.lowercase(Locale.ROOT)
                if (topologyCategory == TopologyCategory.INTERIOR) {
                    return "interior"
                }
                return "zona de $topology"
            }
        }

        if (npc != null && !npc.worldName.isNullOrBlank()) {
            return npc.worldName ?: "necunoscut"
        }

        return ""
    }

    private fun detectIntents(playerMessage: String): List<FactIntent> {
        val normalizedMessage = normalize(playerMessage)
        val matches = EnumMap<FactIntent, Int>(FactIntent::class.java)

        registerMatch(matches, FactIntent.NAME, normalizedMessage,
            "cine esti", "cum te cheama", "cum te numesti", "numele tau", "ce nume ai")
        registerMatch(matches, FactIntent.PROFESSION, normalizedMessage,
            "ce meserie ai", "ce profesie ai", "care e meseria ta", "care e profesia ta",
            "ce ocupatie ai", "cu ce te ocupi")
        registerMatch(matches, FactIntent.STATE, normalizedMessage,
            "cum te simti", "ce stare ai", "cum iti este")
        registerMatch(matches, FactIntent.ACTIVITY, normalizedMessage,
            "ce faci", "ce faci acum", "ce lucrezi", "la ce lucrezi", "ce muncesti", "cu ce te ocupi acum")
        registerMatch(matches, FactIntent.LOCATION, normalizedMessage,
            "unde esti", "unde te afli", "in ce loc esti", "unde te gasesc")

        return matches.entries
            .sortedBy { it.value }
            .map { it.key }
    }

    private fun registerMatch(
        matches: MutableMap<FactIntent, Int>,
        intent: FactIntent,
        message: String,
        vararg patterns: String
    ) {
        var earliestIndex = Int.MAX_VALUE
        for (pattern in patterns) {
            val index = message.indexOf(pattern)
            if (index >= 0 && index < earliestIndex) {
                earliestIndex = index
            }
        }

        if (earliestIndex != Int.MAX_VALUE) {
            matches.merge(intent, earliestIndex, ::minOf)
        }
    }

    private fun resolveIntent(intent: FactIntent, facts: NpcFacts): String? {
        return when (intent) {
            FactIntent.NAME -> if (facts.npcName().isBlank()) null else "Sunt ${facts.npcName()}."
            FactIntent.PROFESSION -> if (facts.occupation().isBlank()) null else "Sunt ${facts.occupation()}."
            FactIntent.STATE -> buildStateAnswer(facts)
            FactIntent.ACTIVITY -> if (facts.currentActivity().isBlank()) null else capitalizeSentence(facts.currentActivity()) + "."
            FactIntent.LOCATION -> if (facts.locationDescription().isBlank()) null else "Sunt in ${facts.locationDescription()}."
        }
    }

    private fun buildStateAnswer(facts: NpcFacts): String? {
        if (facts.emotionalState().isNotBlank()) {
            return "Ma simt ${facts.emotionalState()}."
        }

        if (facts.currentState().isNotBlank()) {
            return "Acum sunt ${facts.currentState().lowercase(Locale.ROOT)}."
        }

        return null
    }

    private fun joinNaturally(answers: List<String>): String {
        val cleanAnswers = answers.map { stripTrailingPeriod(it) }

        if (cleanAnswers.size == 1) {
            return cleanAnswers[0] + "."
        }

        if (cleanAnswers.size == 2) {
            return cleanAnswers[0] + " si " + lowerCaseFirst(cleanAnswers[1]) + "."
        }

        val last = cleanAnswers.last()
        val prefix = cleanAnswers.dropLast(1).joinToString(", ")
        return prefix + " si " + lowerCaseFirst(last) + "."
    }

    private fun stripTrailingPeriod(text: String?): String {
        if (text == null) {
            return ""
        }

        var trimmed = text.trim()
        while (trimmed.endsWith(".")) {
            trimmed = trimmed.substring(0, trimmed.length - 1).trim()
        }
        return trimmed
    }

    private fun lowerCaseFirst(text: String?): String {
        if (text.isNullOrBlank()) {
            return ""
        }

        return text[0].lowercaseChar() + text.substring(1)
    }

    private fun capitalizeSentence(text: String?): String {
        if (text.isNullOrBlank()) {
            return ""
        }

        val trimmed = text.trim()
        return trimmed[0].uppercaseChar() + trimmed.substring(1)
    }

    private fun normalize(value: String): String {
        val normalized = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{M}+"), "")
    }

    private enum class FactIntent {
        NAME,
        PROFESSION,
        STATE,
        ACTIVITY,
        LOCATION
    }

    data class NpcFacts(
        val npcName: String,
        val occupation: String,
        val emotionalState: String,
        val currentState: String,
        val currentActivity: String,
        val locationDescription: String
    ) {
        fun npcName(): String = npcName
        fun occupation(): String = occupation
        fun emotionalState(): String = emotionalState
        fun currentState(): String = currentState
        fun currentActivity(): String = currentActivity
        fun locationDescription(): String = locationDescription
    }
}
