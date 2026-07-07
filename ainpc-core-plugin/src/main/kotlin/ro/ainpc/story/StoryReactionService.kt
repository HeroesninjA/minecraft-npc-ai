package ro.ainpc.story

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.NPCState
import java.util.logging.Level

class StoryReactionService(private val plugin: AINPCPlugin) {
    private val eventReactions: Map<String, EventReaction> = mapOf(
        "celebration" to EventReaction(
            targetState = NPCState.CELEBRATING,
            messages = listOf(
                "Sărbătoarea a început!",
                "Haideți cu toții la petrecere!",
                "Ce zi minunată pentru o sărbătoare!",
                "Bucurie și veselie în sat!"
            ),
            radius = 64.0,
            emotionalImpact = mapOf("happiness" to 0.3, "trust" to 0.1)
        ),
        "conflict" to EventReaction(
            targetState = NPCState.PANICKING,
            messages = listOf(
                "Pericol! La arme!",
                "Fugiți! Sunt atacați!",
                "Ce este acel zgomot?",
                "Trebuie să ne apărăm satul!"
            ),
            radius = 96.0,
            emotionalImpact = mapOf("fear" to 0.5, "anger" to 0.2, "sadness" to 0.1)
        ),
        "disaster" to EventReaction(
            targetState = NPCState.FLEEING,
            messages = listOf(
                "Fugiți! Dezastru!",
                "Cerul se întunecă!",
                "Trebuie să ne salvăm!",
                "Pământul se cutremură!"
            ),
            radius = 128.0,
            emotionalImpact = mapOf("fear" to 0.6, "sadness" to 0.3)
        ),
        "discovery" to EventReaction(
            targetState = NPCState.CURIOUS,
            messages = listOf(
                "Ce s-a descoperit?",
                "Trebuie să vedem despre ce e vorba!",
                "O descoperire interesantă!",
                "Să mergem să vedem cu toții!"
            ),
            radius = 48.0,
            emotionalImpact = mapOf("surprise" to 0.3, "happiness" to 0.1)
        ),
        "ritual" to EventReaction(
            targetState = NPCState.PRAYING,
            messages = listOf(
                "Este timpul pentru ritual.",
                "Strămoșii ne veghează.",
                "Onorăm tradițiile străvechi.",
                "Lăsați luminile să ne ghideze."
            ),
            radius = 64.0,
            emotionalImpact = mapOf("trust" to 0.2, "anticipation" to 0.2)
        ),
        "trade" to EventReaction(
            targetState = NPCState.SOCIALIZING,
            messages = listOf(
                "Marfă nouă în sat!",
                "Negustorul a sosit cu bunuri!",
                "Haideți la târg!",
                "Oferte bune astăzi!"
            ),
            radius = 48.0,
            emotionalImpact = mapOf("happiness" to 0.2, "anticipation" to 0.2)
        ),
        "diplomacy" to EventReaction(
            targetState = NPCState.IDLE,
            messages = listOf(
                "Avem vizitatori importanți.",
                "O delegație străină a sosit.",
                "Să fim ospitalieri cu oaspeții noștri.",
                "Vremuri interesante pentru sat."
            ),
            radius = 48.0,
            emotionalImpact = mapOf("anticipation" to 0.2, "trust" to 0.1)
        ),
        "story_progress" to EventReaction(
            targetState = NPCState.CURIOUS,
            messages = listOf(
                "Vești noi din lume!",
                "Am auzit că ceva important s-a întâmplat.",
                "Povestea satului nostru continuă.",
                "Ce se mai întâmplă în lume?"
            ),
            radius = 32.0,
            emotionalImpact = mapOf("anticipation" to 0.2)
        ),
        "story_event" to EventReaction(
            targetState = NPCState.IDLE,
            messages = listOf(
                "Ceva se întâmplă în sat...",
                "Simt că aerul s-a schimbat.",
                "Să fim atenți la ce se petrece.",
                "Vremuri interesante vin peste noi."
            ),
            radius = 32.0,
            emotionalImpact = mapOf("anticipation" to 0.15, "surprise" to 0.1)
        ),
        "npc_event" to EventReaction(
            targetState = NPCState.SOCIALIZING,
            messages = listOf(
                "Un eveniment important pentru ai noștri!",
                "Ar trebui să sărbătorim!",
                "Haideți să ne adunăm!",
                "Este o zi specială pentru cineva din sat."
            ),
            radius = 32.0,
            emotionalImpact = mapOf("happiness" to 0.2, "trust" to 0.1)
        ),
        "quest_event" to EventReaction(
            targetState = NPCState.IDLE,
            messages = listOf(
                "Am auzit de o misiune importantă.",
                "Cineva caută ajutor în sat.",
                "O nouă sarcină ne așteaptă.",
                "Țineți urechile deschise pentru vești."
            ),
            radius = 32.0,
            emotionalImpact = mapOf("anticipation" to 0.2)
        ),
        "player_action" to EventReaction(
            targetState = NPCState.CURIOUS,
            messages = listOf(
                "Ce face acest străin?",
                "Un călător ne vizitează satul.",
                "Să fim prietenoși cu vizitatorul.",
                "Poate are vești din alte locuri."
            ),
            radius = 24.0,
            emotionalImpact = mapOf("surprise" to 0.15, "anticipation" to 0.1)
        ),
        "environmental" to EventReaction(
            targetState = NPCState.IDLE,
            messages = listOf(
                "Natura ne vorbește.",
                "Anotimpurile se schimbă în jurul nostru.",
                "Simt că ceva se pregătește în aer.",
                "Pământul și cerul ne spun povești."
            ),
            radius = 48.0,
            emotionalImpact = mapOf("anticipation" to 0.1, "surprise" to 0.05)
        ),
        "resolution" to EventReaction(
            targetState = NPCState.CELEBRATING,
            messages = listOf(
                "Totul s-a rezolvat!",
                "Mulțumim că ne-ați ajutat!",
                "Satul este din nou în pace.",
                "O nouă zi, o nouă speranță."
            ),
            radius = 48.0,
            emotionalImpact = mapOf("happiness" to 0.3, "trust" to 0.2, "sadness" to -0.2)
        )
    )

    data class EventReaction(
        val targetState: NPCState,
        val messages: List<String>,
        val radius: Double,
        val emotionalImpact: Map<String, Double> = emptyMap()
    )

    fun reactToEvent(eventType: String, scopeId: String, regionId: String) {
        val reaction = eventReactions[eventType] ?: return
        if (!plugin.config.getBoolean("story.npc_reactions_enabled", true)) return

        val affectedNpcs = findNpcsInScope(scopeId, regionId, reaction.radius)
        for (npc in affectedNpcs) {
            if (npc.currentState.getPriority() >= reaction.targetState.getPriority()) continue
            npc.changeState(reaction.targetState)
            val message = reaction.messages.random()
            npc.plannedRoutineActivity = message
            applyEmotionalImpact(npc, reaction.emotionalImpact)
        }
        plugin.debug("[StoryReaction] ${affectedNpcs.size} NPC reactonat la $eventType in $scopeId")
    }

    private fun applyEmotionalImpact(npc: ro.ainpc.npc.AINPC, impact: Map<String, Double>) {
        for ((emotion, delta) in impact) {
            when (emotion) {
                "happiness" -> npc.emotions.happiness = (npc.emotions.happiness + delta).coerceIn(0.0, 1.0)
                "sadness" -> npc.emotions.sadness = (npc.emotions.sadness + delta).coerceIn(0.0, 1.0)
                "anger" -> npc.emotions.anger = (npc.emotions.anger + delta).coerceIn(0.0, 1.0)
                "fear" -> npc.emotions.fear = (npc.emotions.fear + delta).coerceIn(0.0, 1.0)
                "surprise" -> npc.emotions.surprise = (npc.emotions.surprise + delta).coerceIn(0.0, 1.0)
                "trust" -> npc.emotions.trust = (npc.emotions.trust + delta).coerceIn(0.0, 1.0)
                "anticipation" -> npc.emotions.anticipation = (npc.emotions.anticipation + delta).coerceIn(0.0, 1.0)
            }
        }
    }

    private fun findNpcsInScope(scopeId: String, regionId: String, radius: Double): List<ro.ainpc.npc.AINPC> {
        val region = plugin.platform.worldAdmin.getRegion(regionId.ifBlank { scopeId })
        val worldName = region?.worldName() ?: return emptyList()
        val world = plugin.server.getWorld(worldName) ?: return emptyList()
        val center = if (region != null) {
            org.bukkit.Location(world,
                ((region.minX() + region.maxX()) / 2.0),
                ((region.minY() + region.maxY()) / 2.0),
                ((region.minZ() + region.maxZ()) / 2.0))
        } else return emptyList()

        return plugin.npcManager.getAllNPCs().filter { npc ->
            if (!npc.isSpawned()) return@filter false
            val loc = npc.location ?: return@filter false
            loc.world?.name?.equals(worldName, ignoreCase = true) == true &&
                loc.distanceSquared(center) <= radius * radius
        }
    }
}
