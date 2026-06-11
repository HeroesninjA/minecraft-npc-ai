package ro.ainpc.npc

/**
 * Starile posibile ale unui NPC - State Machine
 * NPC-ul poate fi intr-o singura stare la un moment dat
 */
enum class NPCState(
    val displayName: String,
    val description: String
) {
    // Stari de baza
    IDLE("Asteapta", "NPC-ul nu face nimic special"),
    WALKING("Merge", "NPC-ul se deplaseaza"),
    RUNNING("Alearga", "NPC-ul se deplaseaza rapid"),

    // Stari de interactiune
    TALKING("Vorbeste", "NPC-ul este in conversatie"),
    LISTENING("Asculta", "NPC-ul asculta pe cineva"),
    TRADING("Negociaza", "NPC-ul face schimburi comerciale"),

    // Stari de munca
    WORKING("Lucreaza", "NPC-ul isi desfasoara meseria"),
    CRAFTING("Creeaza", "NPC-ul creaza obiecte"),
    FARMING("Fermiereaza", "NPC-ul lucreaza pamantul"),
    MINING("Mineaza", "NPC-ul sapa"),
    FISHING("Pescuieste", "NPC-ul pescuieste"),

    // Stari sociale
    SOCIALIZING("Socializeaza", "NPC-ul interactioneaza cu alte NPC-uri"),
    CELEBRATING("Sarbatoreste", "NPC-ul participa la o sarbatoare"),
    MOURNING("Jeleste", "NPC-ul este in doliu"),
    ARGUING("Se cearta", "NPC-ul are un conflict verbal"),

    // Stari de combat
    COMBAT("In lupta", "NPC-ul lupta activ"),
    FLEEING("Fuge", "NPC-ul fuge de pericol"),
    GUARDING("Pazeste", "NPC-ul pazeste o zona"),
    PATROLLING("Patruleaza", "NPC-ul patruleaza o zona"),

    // Stari de odihna
    SLEEPING("Doarme", "NPC-ul doarme"),
    RESTING("Se odihneste", "NPC-ul se odihneste"),
    EATING("Mananca", "NPC-ul mananca"),
    DRINKING("Bea", "NPC-ul bea"),

    // Stari emotionale speciale
    PANICKING("Panicheaza", "NPC-ul este cuprins de panica"),
    CURIOUS("Curios", "NPC-ul investigheaza ceva"),
    HIDING("Se ascunde", "NPC-ul se ascunde"),
    PRAYING("Se roaga", "NPC-ul se roaga"),

    // Stari de scenarii
    QUEST_GIVING("Ofera misiune", "NPC-ul ofera o misiune"),
    FOLLOWING("Urmareste", "NPC-ul urmareste pe cineva"),
    WAITING("Asteapta", "NPC-ul asteapta ceva sau pe cineva");

    /**
     * Verifica daca starea permite interactiunea cu jucatori
     */
    fun allowsPlayerInteraction(): Boolean = when (this) {
        SLEEPING, COMBAT, FLEEING, PANICKING, HIDING -> false
        else -> true
    }

    /**
     * Verifica daca starea permite miscarea
     */
    fun allowsMovement(): Boolean = when (this) {
        SLEEPING, TALKING, TRADING, PRAYING, WORKING, CRAFTING -> false
        else -> true
    }

    /**
     * Verifica daca starea este una de munca
     */
    fun isWorkState(): Boolean = when (this) {
        WORKING, CRAFTING, FARMING, MINING, FISHING, GUARDING, PATROLLING -> true
        else -> false
    }

    /**
     * Verifica daca starea este una sociala
     */
    fun isSocialState(): Boolean = when (this) {
        TALKING, LISTENING, SOCIALIZING, CELEBRATING, ARGUING, TRADING -> true
        else -> false
    }

    /**
     * Verifica daca starea este una de pericol
     */
    fun isDangerState(): Boolean = when (this) {
        COMBAT, FLEEING, PANICKING, HIDING, GUARDING -> true
        else -> false
    }

    /**
     * Obtine prioritatea starii (pentru tranzitii)
     * Cu cat mai mare, cu atat mai greu de intrerupt
     */
    fun getPriority(): Int = when (this) {
        COMBAT, FLEEING, PANICKING -> 100
        SLEEPING -> 80
        TALKING, TRADING -> 60
        WORKING, CRAFTING -> 40
        WALKING, RUNNING, PATROLLING -> 20
        IDLE -> 0
        else -> 30
    }
}
