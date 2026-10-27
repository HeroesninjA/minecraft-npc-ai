package ro.ainpc.ai

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCPersonality
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcPerceptionSnapshot
import ro.ainpc.topology.TopologyCategory
import java.text.Normalizer
import java.util.EnumMap
import java.util.Locale
import java.util.Optional
import kotlin.math.abs

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
            return Optional.of(buildFallbackResponse(facts))
        }

        return Optional.of(joinNaturally(answers))
    }

    @JvmStatic
    fun buildFallbackResponse(facts: NpcFacts?): String {
        if (facts == null) {
            return "Nu pot sa raspund acum."
        }
        val name = facts.npcName().ifBlank { null }
        val occ = facts.occupation().ifBlank { null }
        val loc = facts.locationDescription().ifBlank { null }
        return when {
            name != null && occ != null -> "Scuze, $name nu poate raspunde acum. Ca $occ, sunt concentrat pe alte lucruri."
            name != null -> "$name nu poate raspunde acum."
            occ != null -> "Ca $occ, nu am un raspuns pentru asta."
            loc != null -> "Nu pot raspunde chiar acum, fiind in $loc."
            else -> "Nu am un raspuns clar pentru tine."
        }
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
    fun activityAnswer(facts: NpcFacts): String? {
        val currentActivity = facts.currentActivity().trim()
        val routine = safeRoutine(facts.plannedRoutineActivity())
        if (currentActivity.isBlank()) return routine?.let { "Acum am in program: $it." }
        if (routine != null && isConversationActivity(currentActivity)) {
            return "Vorbesc cu tine acum; in program am: $routine."
        }
        return capitalizeSentence(currentActivity) + "."
    }

    @JvmStatic
    fun routineAnswer(facts: NpcFacts): String = safeRoutine(facts.plannedRoutineActivity())
        ?.let { "Acum am in program: $it." }
        ?: "Nu am un program stabilit acum."

    @JvmStatic
    fun describeLocation(npc: AINPC?, context: NPCContext?): String =
        describeLocationValues(npc?.worldName, context?.topologyCategory)

    internal fun describeCapturedLocation(perception: NpcPerceptionSnapshot): String =
        describeLocationValues(perception.worldName, perception.topologyCategory)

    private fun describeLocationValues(worldName: String?, topologyCategory: TopologyCategory?): String {
        if (topologyCategory != null && topologyCategory != TopologyCategory.UNKNOWN) {
            val topology = topologyCategory.displayName.lowercase(Locale.ROOT)
            if (topologyCategory == TopologyCategory.INTERIOR) {
                return "interior"
            }
            return "zona de $topology"
        }

        if (!worldName.isNullOrBlank()) {
            return worldName
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
        registerMatch(matches, FactIntent.ROUTINE, normalizedMessage,
            "care este programul tau", "care e programul tau", "ce ai in program",
            "care este rutina ta", "care e rutina ta", "ce urmeaza sa faci", "ce ai de facut")
        registerMatch(matches, FactIntent.LOCATION, normalizedMessage,
            "unde esti", "unde te afli", "in ce loc esti", "unde te gasesc")
        registerMatch(matches, FactIntent.PERSONALITY, normalizedMessage,
            "ce fel de persoana esti", "ce fel de om esti", "cum esti ca persoana",
            "cum e firea ta", "cum ti-ai descrie personalitatea", "ce personalitate ai",
            "cum e caracterul tau", "cum te-ai descrie")

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
            FactIntent.ACTIVITY -> activityAnswer(facts)
            FactIntent.ROUTINE -> routineAnswer(facts)
            FactIntent.LOCATION -> if (facts.locationDescription().isBlank()) null else "Sunt in ${facts.locationDescription()}."
            FactIntent.PERSONALITY -> facts.personalityDescription().ifBlank { null }
        }
    }

    fun describePersonality(personality: NPCPersonality?): String {
        if (personality == null) return ""
        val tendencies = listOfNotNull(
            tendency(personality.openness, "imi plac ideile noi", "prefer traditiile"),
            tendency(personality.conscientiousness, "imi place sa planific", "prefer sa improvizez"),
            tendency(personality.extraversion, "imi place sa socializez", "prefer linistea"),
            tendency(personality.agreeableness, "imi place sa colaborez", "sunt precaut cu ceilalti"),
            tendency(personality.neuroticism, "ma ingrijorez usor", "imi pastrez calmul"),
        ).sortedByDescending { it.first }.take(2).map { it.second }
        return if (tendencies.isEmpty()) "De obicei, ma adaptez la situatie."
        else "De obicei, ${tendencies.joinToString(" si ")}."
    }

    private fun tendency(value: Double, high: String, low: String): Pair<Double, String>? = when {
        value > 0.65 -> abs(value - 0.5) to high
        value < 0.35 -> abs(value - 0.5) to low
        else -> null
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

    internal fun safeRoutine(value: String): String? {
        val routine = value.trim().replace(Regex("\\s+"), " ").trimEnd('.', '!', '?')
        if (routine.isBlank() || routine.length > 120) return null
        val normalized = normalize(routine)
        if (normalized.startsWith("ancora ") || normalized.contains(" lipseste") ||
            normalized.contains(" invalida") || normalized.contains(" indisponibila")) return null
        return when (normalized) {
            "work" -> "sa lucrez"
            "home" -> "sa ma odihnesc acasa"
            "social" -> "sa ma intalnesc cu ceilalti"
            "idle" -> "o pauza"
            else -> routine
        }
    }

    private fun isConversationActivity(value: String): Boolean {
        val normalized = normalize(value)
        return normalized.startsWith("vorb") || normalized.startsWith("ascult") || normalized == "sta de vorba"
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
        ROUTINE,
        LOCATION,
        PERSONALITY
    }

    data class NpcFacts(
        val npcName: String,
        val occupation: String,
        val emotionalState: String,
        val currentState: String,
        val currentActivity: String,
        val locationDescription: String,
        val plannedRoutineActivity: String = "",
        val personalityDescription: String = "",
    ) {
        fun npcName(): String = npcName
        fun occupation(): String = occupation
        fun emotionalState(): String = emotionalState
        fun currentState(): String = currentState
        fun currentActivity(): String = currentActivity
        fun locationDescription(): String = locationDescription
        fun plannedRoutineActivity(): String = plannedRoutineActivity
        fun personalityDescription(): String = personalityDescription
    }
}
