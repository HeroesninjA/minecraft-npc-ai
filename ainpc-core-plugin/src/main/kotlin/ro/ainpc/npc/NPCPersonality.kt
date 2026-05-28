package ro.ainpc.npc

import java.util.Locale
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * Personalitatea NPC-ului bazata pe modelul Big Five (OCEAN)
 */
class NPCPersonality {
    // Big Five Personality Traits (0.0 - 1.0)
    var openness: Double = 0.5 // Deschidere catre experiente noi
        set(value) {
            field = clamp(value)
        }
    var conscientiousness: Double = 0.5 // Constiinciozitate, organizare
        set(value) {
            field = clamp(value)
        }
    var extraversion: Double = 0.5 // Extraversie, sociabilitate
        set(value) {
            field = clamp(value)
        }
    var agreeableness: Double = 0.5 // Agreabilitate, amabilitate
        set(value) {
            field = clamp(value)
        }
    var neuroticism: Double = 0.5 // Neuroticism, instabilitate emotionala
        set(value) {
            field = clamp(value)
        }

    constructor()

    constructor(
        openness: Double,
        conscientiousness: Double,
        extraversion: Double,
        agreeableness: Double,
        neuroticism: Double
    ) {
        this.openness = clamp(openness)
        this.conscientiousness = clamp(conscientiousness)
        this.extraversion = clamp(extraversion)
        this.agreeableness = clamp(agreeableness)
        this.neuroticism = clamp(neuroticism)
    }

    /**
     * Obtine descrierea personalitatii pentru contextul AI
     */
    fun getDescription(): String {
        val sb = StringBuilder()

        // Openness
        when {
            openness > 0.7 -> sb.append("- Foarte curios si deschis la idei noi, iubeste aventura si creativitatea\n")
            openness > 0.5 -> sb.append("- Moderat deschis la experiente noi\n")
            openness > 0.3 -> sb.append("- Preferi rutina si traditia\n")
            else -> sb.append("- Foarte conservator, rezistent la schimbare\n")
        }

        // Conscientiousness
        when {
            conscientiousness > 0.7 -> sb.append("- Foarte organizat, disciplinat si de incredere\n")
            conscientiousness > 0.5 -> sb.append("- Echilibrat intre spontaneitate si organizare\n")
            conscientiousness > 0.3 -> sb.append("- Flexibil dar uneori dezorganizat\n")
            else -> sb.append("- Impulsiv si dezorganizat\n")
        }

        // Extraversion
        when {
            extraversion > 0.7 -> sb.append("- Foarte sociabil, vorbaret si energic\n")
            extraversion > 0.5 -> sb.append("- Sociabil dar apreciaza si singuritatea\n")
            extraversion > 0.3 -> sb.append("- Rezervat, prefera grupuri mici\n")
            else -> sb.append("- Foarte introvertit, evita interactiunile sociale\n")
        }

        // Agreeableness
        when {
            agreeableness > 0.7 -> sb.append("- Foarte bland, empatic si cooperant\n")
            agreeableness > 0.5 -> sb.append("- In general amabil dar poate fi ferm cand e nevoie\n")
            agreeableness > 0.3 -> sb.append("- Pragmatic, poate parea rece uneori\n")
            else -> sb.append("- Sceptic, competitiv si uneori ostil\n")
        }

        // Neuroticism
        when {
            neuroticism > 0.7 -> sb.append("- Emotional instabil, se streseaza usor\n")
            neuroticism > 0.5 -> sb.append("- Sensibil emotional dar gestioneaza stresul\n")
            neuroticism > 0.3 -> sb.append("- In general calm si stabil emotional\n")
            else -> sb.append("- Foarte calm, rar afectat de stres\n")
        }

        return sb.toString()
    }

    /**
     * Obtine o lista de trasaturi dominante
     */
    fun getDominantTraits(): String {
        val sb = StringBuilder()

        if (openness > 0.6) sb.append("curios, ")
        if (openness < 0.4) sb.append("traditional, ")
        if (conscientiousness > 0.6) sb.append("organizat, ")
        if (conscientiousness < 0.4) sb.append("spontan, ")
        if (extraversion > 0.6) sb.append("sociabil, ")
        if (extraversion < 0.4) sb.append("introvertit, ")
        if (agreeableness > 0.6) sb.append("amabil, ")
        if (agreeableness < 0.4) sb.append("competitiv, ")
        if (neuroticism > 0.6) sb.append("emotional, ")
        if (neuroticism < 0.4) sb.append("calm, ")

        var result = sb.toString()
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length - 2)
        }
        return if (result.isEmpty()) "echilibrat" else result
    }

    /**
     * Calculeaza cat de bine se potriveste NPC-ul cu un anumit tip de conversatie
     */
    fun getConversationAffinity(topic: String): Double {
        return when (topic.lowercase(Locale.ROOT)) {
            "adventure", "aventura" -> (openness + extraversion) / 2
            "philosophy", "filosofie" -> openness * 0.7 + conscientiousness * 0.3
            "gossip", "barfa" -> extraversion * 0.6 + (1 - conscientiousness) * 0.4
            "work", "munca" -> conscientiousness
            "feelings", "sentimente" -> (agreeableness + neuroticism) / 2
            "conflict" -> (1 - agreeableness) * 0.6 + neuroticism * 0.4
            "humor", "umor" -> extraversion * 0.5 + openness * 0.5
            else -> 0.5
        }
    }

    companion object {
        private val random = Random()

        /**
         * Genereaza o personalitate aleatorie realista
         */
        @JvmStatic
        fun generateRandom(): NPCPersonality {
            val personality = NPCPersonality()
            // Genereaza valori cu distributie normala centrata pe 0.5
            personality.openness = generateTrait()
            personality.conscientiousness = generateTrait()
            personality.extraversion = generateTrait()
            personality.agreeableness = generateTrait()
            personality.neuroticism = generateTrait()
            return personality
        }

        /**
         * Creeaza o personalitate bazata pe un arhetip
         */
        @JvmStatic
        fun fromArchetype(archetype: String): NPCPersonality {
            return when (archetype.lowercase(Locale.ROOT)) {
                "hero", "erou" -> NPCPersonality(0.7, 0.8, 0.7, 0.8, 0.2)
                "villain", "raufacator" -> NPCPersonality(0.5, 0.6, 0.4, 0.1, 0.7)
                "sage", "intelept" -> NPCPersonality(0.9, 0.7, 0.4, 0.7, 0.3)
                "jester", "bufon" -> NPCPersonality(0.8, 0.3, 0.9, 0.7, 0.4)
                "caregiver", "ingrijitor" -> NPCPersonality(0.5, 0.7, 0.6, 0.9, 0.4)
                "explorer", "explorator" -> NPCPersonality(0.9, 0.4, 0.6, 0.5, 0.3)
                "rebel" -> NPCPersonality(0.7, 0.3, 0.5, 0.3, 0.6)
                "lover", "romantic" -> NPCPersonality(0.7, 0.5, 0.7, 0.8, 0.5)
                "creator" -> NPCPersonality(0.9, 0.6, 0.5, 0.6, 0.4)
                "ruler", "conducator" -> NPCPersonality(0.5, 0.9, 0.6, 0.4, 0.3)
                "magician" -> NPCPersonality(0.9, 0.5, 0.5, 0.5, 0.4)
                "innocent", "inocent" -> NPCPersonality(0.4, 0.5, 0.6, 0.9, 0.2)
                "orphan", "orfan" -> NPCPersonality(0.4, 0.5, 0.4, 0.6, 0.7)
                "warrior", "razboinic" -> NPCPersonality(0.4, 0.8, 0.5, 0.3, 0.4)
                else -> generateRandom()
            }
        }

        /**
         * Genereaza un trait cu distributie normala
         */
        private fun generateTrait(): Double {
            // Media 0.5, deviatie standard 0.2
            val value = random.nextGaussian() * 0.2 + 0.5
            return clamp(value)
        }

        private fun clamp(value: Double): Double = max(0.0, min(1.0, value))
    }
}
