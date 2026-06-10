package ro.ainpc.npc

import java.util.HashMap
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Sistemul de emotii al NPC-ului bazat pe modelul Plutchik
 */
class NPCEmotions {
    // Emotii primare (0.0 - 1.0)
    var happiness: Double = 0.5 // Bucurie
        set(value) {
            field = clamp(value)
        }
    var sadness: Double = 0.0 // Tristete
        set(value) {
            field = clamp(value)
        }
    var anger: Double = 0.0 // Furie
        set(value) {
            field = clamp(value)
        }
    var fear: Double = 0.0 // Frica
        set(value) {
            field = clamp(value)
        }
    var surprise: Double = 0.0 // Surpriza
        set(value) {
            field = clamp(value)
        }
    var disgust: Double = 0.0 // Dezgust
        set(value) {
            field = clamp(value)
        }
    var trust: Double = 0.5 // Incredere
        set(value) {
            field = clamp(value)
        }
    var anticipation: Double = 0.3 // Anticipare
        set(value) {
            field = clamp(value)
        }

    var lastUpdated: Long = System.currentTimeMillis()

    /**
     * Aplica o emotie cu o anumita intensitate
     */
    fun applyEmotion(emotion: String, intensityInput: Double) {
        val intensity = max(0.0, min(1.0, intensityInput))

        when (emotion.lowercase(Locale.ROOT)) {
            "happiness", "bucurie", "joy" -> {
                happiness = blend(happiness, intensity)
                sadness = decay(sadness, intensity * 0.5)
            }

            "sadness", "tristete", "sorrow" -> {
                sadness = blend(sadness, intensity)
                happiness = decay(happiness, intensity * 0.5)
            }

            "anger", "furie", "rage" -> {
                anger = blend(anger, intensity)
                fear = decay(fear, intensity * 0.3)
            }

            "fear", "frica", "terror" -> {
                fear = blend(fear, intensity)
                anger = decay(anger, intensity * 0.3)
            }

            "surprise", "surpriza", "amazement" -> {
                surprise = blend(surprise, intensity)
            }

            "disgust", "dezgust", "loathing" -> {
                disgust = blend(disgust, intensity)
                trust = decay(trust, intensity * 0.5)
            }

            "trust", "incredere", "admiration" -> {
                trust = blend(trust, intensity)
                disgust = decay(disgust, intensity * 0.5)
                fear = decay(fear, intensity * 0.3)
            }

            "anticipation", "anticipare", "vigilance" -> {
                anticipation = blend(anticipation, intensity)
            }
        }

        lastUpdated = System.currentTimeMillis()
        normalizeEmotions()
    }

    /**
     * Aplica emotii bazate pe tipul de interactiune
     */
    fun applyInteractionEffect(interactionType: String, multiplier: Double) {
        when (interactionType.lowercase(Locale.ROOT)) {
            "greeting", "salut" -> {
                applyEmotion("happiness", 0.1 * multiplier)
                applyEmotion("trust", 0.05 * multiplier)
            }

            "compliment" -> {
                applyEmotion("happiness", 0.2 * multiplier)
                applyEmotion("trust", 0.1 * multiplier)
            }

            "insult", "insulta" -> {
                applyEmotion("anger", 0.3 * multiplier)
                applyEmotion("sadness", 0.1 * multiplier)
            }

            "gift", "cadou" -> {
                applyEmotion("happiness", 0.3 * multiplier)
                applyEmotion("surprise", 0.2 * multiplier)
                applyEmotion("trust", 0.15 * multiplier)
            }

            "threat", "amenintare" -> {
                applyEmotion("fear", 0.4 * multiplier)
                applyEmotion("anger", 0.2 * multiplier)
            }

            "help", "ajutor" -> {
                applyEmotion("happiness", 0.15 * multiplier)
                applyEmotion("trust", 0.2 * multiplier)
            }

            "ignore", "ignorare" -> {
                applyEmotion("sadness", 0.1 * multiplier)
            }

            "joke", "gluma" -> {
                applyEmotion("happiness", 0.15 * multiplier)
                applyEmotion("surprise", 0.1 * multiplier)
            }

            "secret" -> {
                applyEmotion("trust", 0.2 * multiplier)
                applyEmotion("anticipation", 0.15 * multiplier)
            }

            "betrayal", "tradare" -> {
                applyEmotion("anger", 0.4 * multiplier)
                applyEmotion("sadness", 0.3 * multiplier)
                applyEmotion("disgust", 0.2 * multiplier)
            }
        }
    }

    fun adjustHappiness(delta: Double) {
        happiness += delta
        lastUpdated = System.currentTimeMillis()
    }

    fun adjustSadness(delta: Double) {
        sadness += delta
        lastUpdated = System.currentTimeMillis()
    }

    fun adjustAnger(delta: Double) {
        anger += delta
        lastUpdated = System.currentTimeMillis()
    }

    fun adjustFear(delta: Double) {
        fear += delta
        lastUpdated = System.currentTimeMillis()
    }

    /**
     * Decrementeaza emotiile in timp (revenire la neutral)
     */
    fun decay(rate: Double) {
        happiness = decayTowards(happiness, 0.5, rate)
        sadness = decayTowards(sadness, 0.0, rate)
        anger = decayTowards(anger, 0.0, rate)
        fear = decayTowards(fear, 0.0, rate)
        surprise = decayTowards(surprise, 0.0, rate * 2) // Surpriza scade mai repede
        disgust = decayTowards(disgust, 0.0, rate)
        trust = decayTowards(trust, 0.5, rate * 0.5) // Increderea se schimba mai greu
        anticipation = decayTowards(anticipation, 0.3, rate)

        lastUpdated = System.currentTimeMillis()
    }

    /**
     * Obtine emotia dominanta
     */
    val dominantEmotion: String
        get() {
            val emotions = getEmotionMap()
            var dominant = "neutral"
            var maxValue = 0.0

            for ((key, value) in emotions) {
                val threshold = if (key == "happiness" || key == "trust") 0.6 else 0.3
                if (value > threshold && value > maxValue) {
                    maxValue = value
                    dominant = key
                }
            }
            return dominant
        }

    /**
     * Obtine culoarea asociata emotiei dominante
     */
    val dominantEmotionColor: String
        get() = getEmotionColor(dominantEmotion)

    /**
     * Obtine descrierea starii emotionale
     */
    fun getDescription(): String {
        val sb = StringBuilder()
        val dominant = dominantEmotion
        sb.append("Emotie dominanta: ").append(getEmotionNameRomanian(dominant)).append("\n")

        for ((key, value) in getEmotionMap()) {
            if (value > 0.2) {
                sb.append("- ").append(getEmotionNameRomanian(key))
                    .append(": ").append(getIntensityDescription(value)).append("\n")
            }
        }
        return sb.toString()
    }

    /**
     * Obtine descrierea scurta a starii emotionale pentru prompt
     */
    fun getShortDescription(): String {
        val dominant = dominantEmotion
        val intensity = getEmotionValue(dominant)
        val intensityWord = if (intensity > 0.7) "foarte " else if (intensity > 0.4) "" else "putin "
        return intensityWord + getEmotionNameRomanian(dominant)
    }

    /**
     * Obtine valoarea unei emotii dupa nume
     */
    fun getEmotionValue(emotion: String): Double {
        return when (emotion.lowercase(Locale.ROOT)) {
            "happiness" -> happiness
            "sadness" -> sadness
            "anger" -> anger
            "fear" -> fear
            "surprise" -> surprise
            "disgust" -> disgust
            "trust" -> trust
            "anticipation" -> anticipation
            else -> 0.0
        }
    }

    /**
     * Obtine harta emotiilor
     */
    fun getEmotionMap(): Map<String, Double> {
        val map = HashMap<String, Double>()
        map["happiness"] = happiness
        map["sadness"] = sadness
        map["anger"] = anger
        map["fear"] = fear
        map["surprise"] = surprise
        map["disgust"] = disgust
        map["trust"] = trust
        map["anticipation"] = anticipation
        return map
    }

    private fun getEmotionNameRomanian(emotion: String): String {
        return when (emotion.lowercase(Locale.ROOT)) {
            "happiness" -> "fericit"
            "sadness" -> "trist"
            "anger" -> "furios"
            "fear" -> "speriat"
            "surprise" -> "surprins"
            "disgust" -> "dezgustat"
            "trust" -> "increzator"
            "anticipation" -> "nerabdator"
            "neutral" -> "neutru"
            else -> emotion
        }
    }

    private fun getIntensityDescription(value: Double): String {
        if (value > 0.8) return "foarte puternic"
        if (value > 0.6) return "puternic"
        if (value > 0.4) return "moderat"
        if (value > 0.2) return "slab"
        return "foarte slab"
    }

    private fun blend(current: Double, target: Double): Double = current + (target - current) * 0.3

    private fun decay(value: Double, amount: Double): Double = max(0.0, value - amount)

    private fun decayTowards(current: Double, target: Double, rate: Double): Double {
        if (abs(current - target) < 0.01) return target
        return current + (target - current) * rate
    }

    private fun normalizeEmotions() {
        happiness = clamp(happiness)
        sadness = clamp(sadness)
        anger = clamp(anger)
        fear = clamp(fear)
        surprise = clamp(surprise)
        disgust = clamp(disgust)
        trust = clamp(trust)
        anticipation = clamp(anticipation)
    }

    private fun clamp(value: Double): Double = max(0.0, min(1.0, value))

    companion object {
        /**
         * Obtine culoarea pentru o emotie
         */
        @JvmStatic
        fun getEmotionColor(emotion: String): String {
            return when (emotion.lowercase(Locale.ROOT)) {
                "happiness", "bucurie" -> "\u00A7a" // Verde
                "sadness", "tristete" -> "\u00A79" // Albastru
                "anger", "furie" -> "\u00A7c" // Rosu
                "fear", "frica" -> "\u00A75" // Mov
                "surprise", "surpriza" -> "\u00A7e" // Galben
                "disgust", "dezgust" -> "\u00A72" // Verde inchis
                "trust", "incredere" -> "\u00A7b" // Cyan
                "anticipation", "anticipare" -> "\u00A76" // Portocaliu
                else -> "\u00A7f" // Alb (neutral)
            }
        }
    }
}
