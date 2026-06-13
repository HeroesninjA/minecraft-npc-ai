package ro.ainpc.engine

import org.bukkit.entity.Player
import ro.ainpc.npc.AINPC

fun canTriggerScenario(template: ScenarioEngine.ScenarioTemplate?, npcs: List<AINPC>, players: List<Player>): Boolean {
    if (template == null) return false
    if (template.requiresPlayer() && players.isEmpty()) return false
    if (npcs.size < template.minimumNpcCount) return false
    if (!canAssignMandatoryRoles(template, npcs, players)) return false
    return when (template.type) {
        ScenarioEngine.ScenarioType.ROMANCE -> hasMixedGenders(npcs)
        ScenarioEngine.ScenarioType.CONFLICT -> hasConflictingPersonalities(npcs)
        else -> true
    }
}

fun canAssignMandatoryRoles(
    template: ScenarioEngine.ScenarioTemplate?,
    npcs: List<AINPC>,
    players: List<Player>,
): Boolean {
    if (template == null) return false
    val requiredPlayers = template.playerRoles.stream()
        .filter { role: ScenarioEngine.ScenarioRoleRule -> !role.isOptional }
        .count()
    if (players.size < requiredPlayers) return false

    val availableNpcs = npcs.toMutableList()
    for (role in template.npcRoles) {
        if (role.isOptional) continue
        val selected = selectBestNpcForRole(availableNpcs, role)
        if (selected != null) {
            availableNpcs.remove(selected)
            continue
        }
        if (availableNpcs.isEmpty() || role.hasHardRequirements()) return false
        availableNpcs.removeAt(0)
    }
    return true
}

fun hasMixedGenders(npcs: List<AINPC>): Boolean {
    val hasMale = npcs.any { npc -> "male".equals(npc.gender, ignoreCase = true) }
    val hasFemale = npcs.any { npc -> "female".equals(npc.gender, ignoreCase = true) }
    return hasMale && hasFemale
}

fun hasConflictingPersonalities(npcs: List<AINPC>): Boolean {
    for (npc1 in npcs) {
        for (npc2 in npcs) {
            if (npc1 === npc2) continue
            val p1 = npc1.personality
            val p2 = npc2.personality
            if (kotlin.math.abs(p1.agreeableness - p2.agreeableness) > 0.5) return true
            if (p1.neuroticism > 0.7 && p2.neuroticism > 0.7) return true
        }
    }
    return false
}

fun selectBestNpcForRole(candidates: List<AINPC>, role: ScenarioEngine.ScenarioRoleRule): AINPC? {
    if (candidates.isEmpty()) return null
    var bestNpc: AINPC? = null
    var bestScore = Int.MIN_VALUE
    for (npc in candidates) {
        val score = scoreNpcForRole(npc, role)
        if (score > bestScore) {
            bestNpc = npc
            bestScore = score
        }
    }
    return if (bestScore == Int.MIN_VALUE) null else bestNpc
}

fun scoreNpcForRole(npc: AINPC, role: ScenarioEngine.ScenarioRoleRule): Int {
    if (!hasRequiredProfessions(npc, role.requiredProfessions)) return Int.MIN_VALUE
    if (!hasRequiredTraits(npc, role.requiredTraits)) return Int.MIN_VALUE

    var score = baseRoleScore(npc, role.id)

    if (role.preferredProfessions.isNotEmpty()) {
        val loader = npc.plugin?.featurePackLoader
        val professionMatch = role.preferredProfessions.any { reference ->
            loader != null && loader.matchesProfession(npc.occupation, reference)
        }
        score += if (professionMatch) 90 else -15
    }

    for (preferredTrait in role.preferredTraits) {
        if (npc.hasTrait(preferredTrait)) score += 25
    }

    return score
}

fun hasRequiredProfessions(npc: AINPC, requiredProfessions: List<String>?): Boolean {
    if (requiredProfessions.isNullOrEmpty()) return true
    val occupation = npc.occupation
    if (occupation.isNullOrBlank()) return false
    val loader = npc.plugin?.featurePackLoader
    for (requiredProfession in requiredProfessions) {
        if (loader != null && loader.matchesProfession(occupation, requiredProfession)) return true
        if (normalizeScenarioToken(occupation) == normalizeScenarioToken(requiredProfession)) return true
    }
    return false
}

fun hasRequiredTraits(npc: AINPC, requiredTraits: List<String>?): Boolean {
    if (requiredTraits.isNullOrEmpty()) return true
    for (requiredTrait in requiredTraits) {
        if (!npc.hasTrait(requiredTrait)) return false
    }
    return true
}

fun baseRoleScore(npc: AINPC, roleId: String): Int {
    val personality = npc.personality
    val emotions = npc.emotions
    return when (roleId) {
        "THIEF" -> scoreBoolean(personality.conscientiousness < 0.4 && personality.agreeableness < 0.5, 40)
        "RESPONDER" -> scoreBoolean(personality.conscientiousness > 0.5 || emotions.trust > 0.5, 30)
        "AGGRESSOR" -> scoreBoolean(personality.agreeableness < 0.4 || emotions.anger > 0.5, 35)
        "MEDIATOR" -> scoreBoolean(personality.agreeableness > 0.6 && personality.extraversion > 0.5, 35)
        "COWARD" -> scoreBoolean(personality.neuroticism > 0.6 || emotions.fear > 0.5, 35)
        "LEADER" -> scoreBoolean(personality.extraversion > 0.6 && personality.conscientiousness > 0.5, 35)
        "HOST" -> scoreBoolean(personality.extraversion > 0.5 && personality.agreeableness > 0.5, 30)
        "SUITOR" -> scoreBoolean(personality.extraversion > 0.5, 25)
        "ORIGINATOR" -> scoreBoolean(personality.openness > 0.6, 25)
        "QUEST_GIVER" -> scoreBoolean(personality.agreeableness > 0.45 || personality.extraversion > 0.45, 30)
        "HELPER" -> scoreBoolean(personality.agreeableness > 0.5 || emotions.trust > 0.55, 28)
        "ANTAGONIST" -> scoreBoolean(personality.agreeableness < 0.45 || emotions.anger > 0.45, 28)
        "WITNESS" -> scoreBoolean(personality.openness > 0.45 || personality.extraversion > 0.45, 20)
        "SELLER" -> scoreBoolean(personality.extraversion > 0.5 && personality.conscientiousness > 0.45, 25)
        "BUYER" -> 10
        else -> 0
    }
}

fun matchesOccupation(npc: AINPC, vararg references: String?): Boolean {
    if (references.isEmpty()) return false
    val occupation = npc.occupation
    if (occupation.isNullOrBlank()) return false
    val loader = npc.plugin?.featurePackLoader
    for (reference in references) {
        if (reference == null) continue
        if (loader != null && loader.matchesProfession(occupation, reference)) return true
        if (normalizeScenarioToken(occupation) == normalizeScenarioToken(reference)) return true
    }
    return false
}
