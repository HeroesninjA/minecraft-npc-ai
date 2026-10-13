package ro.ainpc.ai

import ro.ainpc.npc.NpcInteractionProfile
import java.util.UUID

internal class NpcInteractionProfileResponses(
    private val maxConversations: Int = 512,
) {
    private data class ConversationKey(
        val npcUuid: UUID,
        val playerUuid: UUID,
        val profile: NpcInteractionProfile,
    )

    private val responseIndices = object : LinkedHashMap<ConversationKey, Int>(maxConversations, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ConversationKey, Int>?): Boolean =
            size > maxConversations
    }

    init {
        require(maxConversations > 0)
    }

    fun resolve(
        profile: NpcInteractionProfile,
        npcUuid: UUID,
        playerUuid: UUID,
        playerName: String,
        questAvailable: Boolean,
        shopAvailable: Boolean,
        shopAccessDenied: Boolean = false,
    ): String? {
        val templates = when (profile) {
            NpcInteractionProfile.FULL_AI -> return null
            NpcInteractionProfile.QUEST_ONLY -> if (questAvailable) return null else listOf(
                "Nu am o misiune pentru tine acum, {player}.",
                "Despre misiuni putem vorbi cand am ceva de oferit.",
                "Revino mai tarziu; acum nu am nicio sarcina pentru tine.",
            )
            NpcInteractionProfile.SHOP_ONLY -> when {
                shopAvailable -> return null
                shopAccessDenied -> listOf(
                    "Nu iti pot vinde marfa din acest magazin, {player}.",
                    "Comertul de aici necesita acces special.",
                    "Nu ai permisiunea pentru ofertele mele, {player}.",
                )
                else -> listOf(
                    "Nu am marfa pregatita acum, {player}.",
                    "Nu pot face negot in clipa asta.",
                    "Revino mai tarziu, cand am ceva de vandut.",
                )
            }
            NpcInteractionProfile.SCENE_ONLY -> listOf(
                "Nu ma pot opri la vorba in clipa asta.",
                "Vorbim dupa ce se termina treaba de aici.",
                "Nu acum, {player}. Trebuie sa fiu atent la ce se intampla.",
            )
            NpcInteractionProfile.MINIMAL -> listOf(
                "Nu pot vorbi acum, {player}.",
                "Mai tarziu. Acum trebuie sa-mi vad de treaba.",
                "Nu am timp de vorba in clipa asta.",
            )
        }

        val key = ConversationKey(npcUuid, playerUuid, profile)
        val selected = synchronized(responseIndices) {
            val previous = responseIndices[key]
            val index = if (previous == null) {
                Math.floorMod(key.hashCode(), templates.size)
            } else {
                (previous + 1) % templates.size
            }
            responseIndices[key] = index
            templates[index]
        }
        return selected.replace("{player}", playerName.ifBlank { "calatorule" })
    }

    internal fun trackedConversationCount(): Int = synchronized(responseIndices) { responseIndices.size }
}
