package ro.ainpc.managers

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCEmotions

/**
 * Manager pentru sistemul de emotii al NPC-urilor
 */
class EmotionManager(private val plugin: AINPCPlugin) {
    /**
     * Aplica o emotie unui NPC
     */
    fun applyEmotion(npc: AINPC, emotion: String, intensity: Double) {
        runEmotionUpdate {
            npc.emotions.applyEmotion(emotion, intensity)
            npc.updateDisplayName()

            if (plugin.config.getBoolean("emotions.show_particles", true)) {
                showEmotionParticles(npc, emotion, intensity)
            }

            persistEmotionsAsync(npc)
            plugin.debug("Emotie aplicata pentru " + npc.name + ": " + emotion + " (" + intensity + ")")
        }
    }

    /**
     * Decrementeaza emotiile tuturor NPC-urilor (revenire la starea neutra)
     */
    fun decayEmotions() {
        val decayRate = plugin.config.getDouble("emotions.change_rate", 0.1)

        for (npc in plugin.npcManager.allNPCs) {
            npc.emotions.decay(decayRate)
            npc.updateDisplayName()
        }

        plugin.debug("Decay emotii aplicat pentru toate NPC-urile.")
    }

    fun applyInteractionEffect(npc: AINPC, interactionType: String, multiplier: Double) {
        runEmotionUpdate {
            npc.emotions.applyInteractionEffect(interactionType, multiplier)
            npc.updateDisplayName()
            persistEmotionsAsync(npc)
        }
    }

    /**
     * Afiseaza particule vizuale pentru emotie
     */
    fun showEmotionParticles(npc: AINPC, emotion: String, intensity: Double) {
        if (!npc.isSpawned()) return

        val loc = npc.location ?: return
        val world: World = loc.world ?: return

        // Locatia deasupra capului NPC-ului
        val particleLoc = loc.clone().add(0.0, 2.2, 0.0)

        val count = (5 + intensity * 10).toInt()

        when (emotion.lowercase()) {
            "happiness", "bucurie" -> {
                // Inimi verzi/galbene
                world.spawnParticle(Particle.HEART, particleLoc, count, 0.3, 0.3, 0.3)
            }
            "sadness", "tristete" -> {
                // Particule de ploaie/lacrimi
                world.spawnParticle(Particle.FALLING_WATER, particleLoc, count * 2, 0.2, 0.1, 0.2)
            }
            "anger", "furie" -> {
                // Fum rosu/foc
                world.spawnParticle(Particle.SMOKE, particleLoc, count, 0.2, 0.2, 0.2, 0.02)
                // Adauga si flacari mici
                if (intensity > 0.5) {
                    world.spawnParticle(Particle.FLAME, particleLoc, count / 2, 0.2, 0.2, 0.2, 0.01)
                }
            }
            "fear", "frica" -> {
                // Particule de panica
                world.spawnParticle(Particle.WITCH, particleLoc, count, 0.3, 0.3, 0.3)
            }
            "surprise", "surpriza" -> {
                // Stele/scantei
                world.spawnParticle(Particle.END_ROD, particleLoc, count, 0.3, 0.3, 0.3, 0.05)
            }
            "disgust", "dezgust" -> {
                // Fum verde
                world.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, count, 0.3, 0.2, 0.3)
            }
            "trust", "incredere" -> {
                // Particule aurii
                world.spawnParticle(Particle.ENCHANT, particleLoc.add(0.0, 0.5, 0.0), count * 2, 0.5, 0.5, 0.5, 0.5)
            }
            "anticipation", "anticipare" -> {
                // Particule de portal
                world.spawnParticle(Particle.PORTAL, particleLoc, count, 0.3, 0.3, 0.3, 0.1)
            }
        }
    }

    /**
     * Obtine descrierea emotiei in romana
     */
    fun getEmotionDescription(npc: AINPC): String {
        val emotions = npc.emotions
        val dominant = emotions.dominantEmotion
        val intensity = emotions.getEmotionValue(dominant)

        val emotionName = getEmotionNameRomanian(dominant)
        val intensityDesc = getIntensityDescription(intensity)

        return npc.name + " se simte " + intensityDesc + " " + emotionName
    }

    /**
     * Obtine starea emotionala completa
     */
    fun getFullEmotionReport(npc: AINPC): String {
        val sb = StringBuilder()
        val emotions = npc.emotions

        sb.append("&6=== Stare Emotionala: ").append(npc.name).append(" ===\n\n")

        // Emotie dominanta
        val dominant = emotions.dominantEmotion
        sb.append("&eEmotie dominanta: &f").append(getEmotionNameRomanian(dominant)).append("\n\n")

        // Toate emotiile
        sb.append("&eDetalii:\n")
        appendEmotionBar(sb, "Bucurie", emotions.happiness, "&a")
        appendEmotionBar(sb, "Tristete", emotions.sadness, "&9")
        appendEmotionBar(sb, "Furie", emotions.anger, "&c")
        appendEmotionBar(sb, "Frica", emotions.fear, "&5")
        appendEmotionBar(sb, "Surpriza", emotions.surprise, "&e")
        appendEmotionBar(sb, "Dezgust", emotions.disgust, "&2")
        appendEmotionBar(sb, "Incredere", emotions.trust, "&b")
        appendEmotionBar(sb, "Anticipare", emotions.anticipation, "&6")

        return sb.toString()
    }

    private fun appendEmotionBar(sb: StringBuilder, name: String, value: Double, color: String) {
        sb.append("&7").append(String.format("%-12s", name)).append(": ")
        sb.append(color)
        val filled = (value * 10).toInt()
        sb.append("â–ˆ".repeat(filled))
        sb.append("&8")
        sb.append("â–‘".repeat(10 - filled))
        sb.append(" &f").append(String.format("%.0f%%", value * 100)).append("\n")
    }

    /**
     * Calculeaza raspunsul emotional la un eveniment
     */
    fun processEvent(npc: AINPC, eventType: String, intensity: Double) {
        when (eventType.lowercase()) {
            "player_approach" -> {
                // Jucator se apropie
                val extraversion = npc.personality.extraversion
                if (extraversion > 0.5) {
                    applyEmotion(npc, "happiness", 0.1 * intensity)
                } else {
                    applyEmotion(npc, "anticipation", 0.1 * intensity)
                }
            }
            "player_leave" -> {
                // Jucator pleaca
                val agreeableness = npc.personality.agreeableness
                if (agreeableness > 0.6) {
                    applyEmotion(npc, "sadness", 0.05 * intensity)
                }
            }
            "combat_nearby" -> {
                // Lupta in apropiere
                val neuroticism = npc.personality.neuroticism
                applyEmotion(npc, "fear", 0.2 * neuroticism * intensity)
            }
            "weather_storm" -> {
                // Furtuna
                val neuroticism = npc.personality.neuroticism
                if (neuroticism > 0.5) {
                    applyEmotion(npc, "fear", 0.1 * intensity)
                }
            }
            "daytime" -> {
                // Zi
                applyEmotion(npc, "happiness", 0.05 * intensity)
            }
            "nighttime" -> {
                // Noapte
                val neuroticism = npc.personality.neuroticism
                if (neuroticism > 0.6) {
                    applyEmotion(npc, "fear", 0.05 * intensity)
                }
            }
        }
    }

    /**
     * Seteaza emotia dominanta direct
     */
    fun setMood(npc: AINPC, emotion: String, intensity: Double) {
        runEmotionUpdate {
            val emotions: NPCEmotions = npc.emotions
            emotions.happiness = if (emotion == "happiness") intensity else 0.3
            emotions.sadness = if (emotion == "sadness") intensity else 0.0
            emotions.anger = if (emotion == "anger") intensity else 0.0
            emotions.fear = if (emotion == "fear") intensity else 0.0
            emotions.surprise = if (emotion == "surprise") intensity else 0.0
            emotions.disgust = if (emotion == "disgust") intensity else 0.0
            emotions.trust = if (emotion == "trust") intensity else 0.5
            emotions.anticipation = if (emotion == "anticipation") intensity else 0.3

            npc.updateDisplayName()
            showEmotionParticles(npc, emotion, intensity)
            persistEmotionsAsync(npc)
        }
    }

    // Helper methods

    private fun runEmotionUpdate(task: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            task.run()
            return
        }

        plugin.server.scheduler.runTask(plugin, task)
    }

    private fun persistEmotionsAsync(npc: AINPC) {
        plugin.databaseManager.runAsync { plugin.npcManager.saveEmotions(npc) }
    }

    private fun getEmotionNameRomanian(emotion: String): String {
        return when (emotion.lowercase()) {
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
        if (value > 0.8) return "extrem de"
        if (value > 0.6) return "foarte"
        if (value > 0.4) return "destul de"
        if (value > 0.2) return "putin"
        return ""
    }
}
