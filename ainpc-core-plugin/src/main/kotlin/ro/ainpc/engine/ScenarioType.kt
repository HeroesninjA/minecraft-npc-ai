package ro.ainpc.engine

import java.util.Locale

enum class ScenarioType(val displayName: String) {
    THEFT("Furt"),
    CONFLICT("Conflict"),
    CELEBRATION("Sarbatoare"),
    EMERGENCY("Urgenta"),
    ROMANCE("Romantism"),
    TRADE_DEAL("Afacere"),
    DUTY("Sarcina"),
    BOUNTY("Bounty local"),
    WORLD_EVENT("Eveniment local"),
    TUTORIAL("Tutorial"),
    RITUAL("Ritual"),
    QUEST("Misiune"),
    GOSSIP_SPREAD("Raspandirea zvonurilor");

    companion object {
        @JvmStatic
        fun fromId(value: String?): ScenarioType {
            if (value.isNullOrBlank()) return QUEST
            for (type in entries) {
                if (type.name.equals(value, ignoreCase = true) || type.displayName.equals(value, ignoreCase = true)) {
                    return type
                }
            }
            val normalized = value.trim().lowercase(Locale.ROOT).replace('-', '_').replace(' ', '_')
            return when (normalized) {
                "datorie", "sarcina", "npc_duty" -> DUTY
                "bounty", "bounties", "local_bounty", "local_bounties", "recompensa", "recompense" -> BOUNTY
                "event", "events", "eveniment", "evenimente", "world_event", "local_event", "village_event", "village_events" -> WORLD_EVENT
                "tutorial", "tutorials", "onboarding", "indrumare" -> TUTORIAL
                "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii", "village_ritual", "village_rituals" -> RITUAL
                else -> QUEST
            }
        }
    }
}
