package ro.ainpc.world

enum class WorldNodeType(
    val id: String,
    val displayName: String,
    val description: String
) {
    NPC_SPAWN("npc_spawn", "Spawn NPC", "Punctul unde NPC-ul apare initial in lume."),
    ENTRANCE("entrance", "Intrare", "Un punct de acces intr-o cladire, pestera sau zona."),
    BED("bed", "Pat", "Un loc de dormit pentru NPC, adesea in case."),
    WORKSTATION("workstation", "Post de lucru", "Un loc de munca fix, cum ar fi o masa de lucru sau un banc."),
    HOME("home", "Acasa", "Ancora principala unde NPC-ul locuieste si doarme."),
    WORK("work", "Munca", "Ancora unde NPC-ul isi desfasoara activitatea profesionala."),
    SOCIAL("social", "Social", "Un loc de intalnire sociala pentru NPC-uri."),
    MEETING_POINT("meeting_point", "Punct de intalnire", "Un loc unde NPC-urile sau jucatorii se pot intalni."),
    QUEST_TRIGGER("quest_trigger", "Trigger de quest", "Un punct care activeaza sau avanseaza un quest."),
    BOSS("boss", "Boss", "Un punct asociat cu un inamic puternic sau un eveniment special."),
    INTERACTION("interaction", "Interactiune", "Un nod care permite interactiuni speciale (deschide, citeste, activeaza)."),
    PROGRESSION("progression", "Progresie", "Un nod care marcheaza progresul intr-un quest sau eveniment."),
    CUSTOM("custom", "Personalizat", "Un tip de nod definit de admin sau printr-un addon.");

    companion object {
        @JvmStatic
        fun fromId(value: String?): WorldNodeType {
            if (value.isNullOrBlank()) {
                return CUSTOM
            }

            for (type in entries) {
                if (type.id.equals(value, ignoreCase = true) || type.name.equals(value, ignoreCase = true)) {
                    return type
                }
            }

            return CUSTOM
        }
    }
}
