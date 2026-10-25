package ro.ainpc.npc

internal enum class NpcSelfCareContinuity(val label: String) {
    READY("revenirea este eligibila; sosirea nu este confirmata"),
    MODE_NOT_FULL("modul de simulare nu este FULL"),
    FEATURE_DISABLED("oprita prin simulation.decision.goal_continuity_enabled"),
    SIMULATION_DISABLED("simularea periodica este oprita"),
    ROUTINE_DISABLED("rutina periodica este oprita"),
    NO_RECENT_GOAL("nu exista un obiectiv recent de ingrijire"),
    NOT_RETURNING("obiectivul nu este in etapa de revenire acasa"),
    NO_PROGRESS("prioritate eliberata: lipseste progresul confirmat timp de trei minute cumulate"),
    HOME_UNCONFIRMED("casa nu este confirmata in lumea curenta"),
    HOME_CHANGED("casa curenta nu mai corespunde obiectivului"),
    AT_HOME("contextul indica prezenta acasa; revenirea nu mai este necesara"),
    UNSAFE("context nesigur; continuitatea nu are prioritate"),
    INTERLOCUTOR("exista un interlocutor in context"),
    STATE_MISMATCH("starea curenta nu permite continuarea revenirii"),
    INVALID_NEED("valoarea nevoii nu este valida"),
    NEED_SATISFIED("nevoia este deja satisfacuta"),
    ROUTINE_PAUSED("rutina NPC-ului este in pauza"),
    ROUTINE_OVERRIDE("o destinatie sau activitate impusa are prioritate"),
}
