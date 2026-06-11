package ro.ainpc.engine

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCEmotions
import ro.ainpc.npc.NPCPersonality
import ro.ainpc.npc.NPCState
import java.util.EnumMap
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class DecisionEngine(private val plugin: AINPCPlugin) {
    private val scoreCache: MutableMap<UUID, MutableMap<NPCAction, Int>> = ConcurrentHashMap()
    private val lastDecisionTime: MutableMap<UUID, Long> = ConcurrentHashMap()

    fun decideAction(npc: AINPC, context: NPCContext): NPCAction {
        val now = System.currentTimeMillis()
        val lastTime = lastDecisionTime[npc.uuid]
        if (lastTime != null && now - lastTime < DECISION_COOLDOWN) {
            val cached = scoreCache[npc.uuid]
            if (!cached.isNullOrEmpty()) {
                return getHighestScoringAction(cached)
            }
            return NPCAction.DO_NOTHING
        }

        lastDecisionTime[npc.uuid] = now
        val scores = calculateAllScores(npc, context)
        scoreCache[npc.uuid] = scores
        val bestAction = getHighestScoringAction(scores)
        plugin.debug("NPC ${npc.name} decide: ${bestAction.displayName} (scor: ${scores[bestAction]})")
        return bestAction
    }

    fun runLifeSimulationTick(npc: AINPC?) {
        if (npc == null) return
        val location = LocationWrapper.fromNpc(npc) ?: return

        val context = npc.context
        context.updateFromWorld(location.world, location.location)
        val scheduledActivity = resolveScheduledActivity(npc, context)
        npc.plannedRoutineActivity = scheduledActivity ?: ""
        updateNeeds(npc, context)
        context.syncSimulationState(location.location)

        val action = decideAction(npc, context)
        applySimulationOutcome(npc, context, action)
        context.syncSimulationState(location.location)
    }

    private fun calculateAllScores(npc: AINPC, context: NPCContext): MutableMap<NPCAction, Int> {
        val scores: MutableMap<NPCAction, Int> = EnumMap(NPCAction::class.java)
        val possibleActions = filterActionsByState(npc.currentState)
        for (action in possibleActions) {
            scores[action] = calculateActionScore(npc, context, action)
        }
        return scores
    }

    private fun calculateActionScore(npc: AINPC, context: NPCContext, action: NPCAction): Int {
        var score = action.baseScore
        score += getTraitModifier(npc.personality, action)
        score += getEmotionModifier(npc.emotions, action)
        score += getMemoryModifier(npc, context, action)
        score += getContextModifier(context, action)
        score += getNeedModifier(context, action)
        score += getRoutineModifier(npc, context, action)
        score += ThreadLocalRandom.current().nextInt(11)
        return max(0, score)
    }

    private fun getTraitModifier(personality: NPCPersonality, action: NPCAction): Int {
        var modifier = 0
        if (action.isSocialState() || action == NPCAction.GREET || action == NPCAction.SOCIALIZE) {
            modifier += ((personality.extraversion - 0.5) * 30).toInt()
        }
        if (action.isFriendly()) modifier += ((personality.agreeableness - 0.5) * 25).toInt()
        if (action.isAggressive()) modifier -= (personality.agreeableness * 20).toInt()
        if (action.getCategory() == NPCAction.ActionCategory.WORK) {
            modifier += ((personality.conscientiousness - 0.5) * 30).toInt()
        }
        if (action == NPCAction.FLEE || action == NPCAction.HIDE) modifier += (personality.neuroticism * 20).toInt()
        if (action == NPCAction.ATTACK || action == NPCAction.THREATEN) modifier -= (personality.neuroticism * 10).toInt()
        if (action == NPCAction.INVESTIGATE || action == NPCAction.OBSERVE) {
            modifier += ((personality.openness - 0.5) * 25).toInt()
        }
        return modifier
    }

    private fun getEmotionModifier(emotions: NPCEmotions, action: NPCAction): Int {
        var modifier = 0
        val happiness = emotions.happiness
        if (action.isFriendly() || action == NPCAction.CELEBRATE || action == NPCAction.LAUGH) modifier += (happiness * 20).toInt()

        val sadness = emotions.sadness
        if (action == NPCAction.CRY || action == NPCAction.MOURN) modifier += (sadness * 25).toInt()
        if (action.isSocialState()) modifier -= (sadness * 15).toInt()

        val anger = emotions.anger
        if (action.isAggressive() || action == NPCAction.ARGUE) modifier += (anger * 30).toInt()
        if (action.isFriendly()) modifier -= (anger * 20).toInt()

        val fear = emotions.fear
        if (action == NPCAction.FLEE || action == NPCAction.HIDE || action == NPCAction.CALL_HELP) modifier += (fear * 35).toInt()
        if (action == NPCAction.ATTACK || action == NPCAction.INVESTIGATE) modifier -= (fear * 25).toInt()

        val surprise = emotions.surprise
        if (action == NPCAction.INVESTIGATE || action == NPCAction.OBSERVE) modifier += (surprise * 20).toInt()

        val disgust = emotions.disgust
        if (action == NPCAction.INSULT || action == NPCAction.ARGUE) modifier += (disgust * 15).toInt()

        return modifier
    }

    private fun getMemoryModifier(npc: AINPC, context: NPCContext, action: NPCAction): Int {
        var modifier = 0
        if (context.interactingPlayer == null) return modifier

        val relationshipLevel = context.relationshipLevel
        if (action.isFriendly()) modifier += relationshipLevel / 5
        if (action.isAggressive()) {
            modifier += if (relationshipLevel < -25) abs(relationshipLevel) / 3 else -relationshipLevel / 3
        }

        when (context.relationshipStatus) {
            "ENEMY" -> {
                if (action == NPCAction.ATTACK || action == NPCAction.THREATEN) modifier += 30
                if (action == NPCAction.FLEE) modifier += 20
                if (action.isFriendly()) modifier -= 40
            }
            "CLOSE_FRIEND" -> {
                if (action.isFriendly()) modifier += 25
                if (action == NPCAction.TELL_STORY || action == NPCAction.SHARE_NEWS) modifier += 20
                if (action.isAggressive()) modifier -= 50
            }
            "FAMILY", "SPOUSE" -> {
                if (action.isFriendly()) modifier += 35
                if (action == NPCAction.DEFEND) modifier += 40
                if (action.isAggressive()) modifier -= 60
            }
        }
        return modifier
    }

    private fun getContextModifier(context: NPCContext, action: NPCAction): Int {
        var modifier = 0
        if (context.isInDanger) {
            if (action == NPCAction.FLEE || action == NPCAction.CALL_HELP) modifier += 50
            if (action == NPCAction.DEFEND || action == NPCAction.ATTACK) modifier += 30
            if (action.getCategory() == NPCAction.ActionCategory.SOCIAL) modifier -= 30
            if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier -= 40
        }
        if ("NIGHT" == context.timeOfDay) {
            if (action == NPCAction.SLEEP || action == NPCAction.REST) modifier += 35
            if (action.isWorkState()) modifier -= 20
        }
        if ("MORNING" == context.timeOfDay) {
            if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier += 20
            if (action == NPCAction.GREET) modifier += 15
        }
        if ("RAIN" == context.weather || "THUNDER" == context.weather) {
            if (!context.isIndoors) {
                if (action == NPCAction.HIDE) modifier += 25
                if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier -= 15
            }
        }
        if (context.hungerLevel < 30 && action == NPCAction.EAT) modifier += 40
        if (context.healthPercent < 50) {
            if (action == NPCAction.REST || action == NPCAction.HEAL) modifier += 30
            if (action == NPCAction.FLEE) modifier += 20
        }
        if (context.isFamilyNearby && (action == NPCAction.SOCIALIZE || action == NPCAction.TALK)) modifier += 25
        if (context.isAtWork && action.getCategory() == NPCAction.ActionCategory.WORK) modifier += 30
        if (context.isAtHome && (action == NPCAction.REST || action == NPCAction.EAT)) modifier += 15
        if (context.interactingPlayer != null) {
            if (action == NPCAction.TALK || action == NPCAction.LISTEN) modifier += 40
            if (action == NPCAction.GREET && context.lastInteractionTime < 5000) modifier += 30
        }
        return modifier
    }

    private fun getNeedModifier(context: NPCContext, action: NPCAction): Int {
        var modifier = 0
        if (context.hungerLevel < 35) {
            if (action == NPCAction.EAT) modifier += 45
            if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier -= 12
        }
        if (context.energyLevel < 35) {
            if (action == NPCAction.SLEEP) modifier += 50
            if (action == NPCAction.REST) modifier += 35
            if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier -= 18
            if (action == NPCAction.INVESTIGATE) modifier -= 10
        }
        if (context.socialNeedLevel < 40) {
            if (action == NPCAction.SOCIALIZE || action == NPCAction.GREET || action == NPCAction.TALK) modifier += 28
        }
        if (context.comfortLevel < 40) {
            if (action == NPCAction.REST || action == NPCAction.SLEEP) modifier += 22
            if (action == NPCAction.HIDE) modifier += 14
        }
        if (context.safetyLevel < 40) {
            if (action == NPCAction.FLEE || action == NPCAction.HIDE || action == NPCAction.CALL_HELP) modifier += 30
            if (action.isAggressive()) modifier -= 10
        }
        return modifier
    }

    private fun getRoutineModifier(npc: AINPC, context: NPCContext, action: NPCAction): Int {
        val focus = inferRoutineFocus(context.plannedRoutineActivity)
        return when (focus) {
            RoutineFocus.WORK -> if (action.getCategory() == NPCAction.ActionCategory.WORK) 26 else 0
            RoutineFocus.REST -> if (action == NPCAction.SLEEP || action == NPCAction.REST || action == NPCAction.EAT) 28 else 0
            RoutineFocus.SOCIAL -> if (action == NPCAction.SOCIALIZE || action == NPCAction.TALK || action == NPCAction.GREET || action == NPCAction.SHARE_NEWS) 22 else 0
            RoutineFocus.GUARD -> if (action == NPCAction.OBSERVE || action == NPCAction.WARN || action == NPCAction.DEFEND || action == NPCAction.CALL_HELP) 24 else 0
            RoutineFocus.OBSERVE -> if (action == NPCAction.OBSERVE || action == NPCAction.INVESTIGATE) 18 else 0
            RoutineFocus.IDLE -> if (action == NPCAction.WALK_RANDOM || action == NPCAction.OBSERVE) 8 else 0
        }
    }

    private fun filterActionsByState(state: NPCState): List<NPCAction> {
        val actions = mutableListOf(NPCAction.DO_NOTHING)
        when (state) {
            NPCState.IDLE -> actions.addAll(listOf(
                NPCAction.WALK_RANDOM, NPCAction.OBSERVE, NPCAction.GREET,
                NPCAction.START_WORK, NPCAction.SOCIALIZE, NPCAction.REST, NPCAction.INVESTIGATE
            ))
            NPCState.TALKING -> actions.addAll(listOf(
                NPCAction.TALK, NPCAction.LISTEN, NPCAction.THANK,
                NPCAction.TELL_STORY, NPCAction.SHARE_NEWS, NPCAction.GOSSIP,
                NPCAction.COMPLIMENT, NPCAction.ARGUE, NPCAction.APOLOGIZE
            ))
            NPCState.WORKING -> actions.addAll(listOf(NPCAction.CONTINUE_WORK, NPCAction.FINISH_WORK, NPCAction.CRAFT, NPCAction.REST))
            NPCState.COMBAT -> actions.addAll(listOf(NPCAction.ATTACK, NPCAction.DEFEND, NPCAction.FLEE, NPCAction.CALL_HELP, NPCAction.SURRENDER))
            NPCState.FLEEING -> actions.addAll(listOf(NPCAction.FLEE, NPCAction.HIDE, NPCAction.CALL_HELP))
            else -> actions.addAll(listOf(NPCAction.WALK_RANDOM, NPCAction.OBSERVE, NPCAction.TALK, NPCAction.REST))
        }
        return actions
    }

    private fun getHighestScoringAction(scores: Map<NPCAction, Int>): NPCAction =
        scores.maxByOrNull { it.value }?.key ?: NPCAction.DO_NOTHING

    fun getTopActions(npc: AINPC, context: NPCContext, count: Int): List<NPCAction> =
        calculateAllScores(npc, context).entries
            .sortedByDescending { it.value }
            .take(count)
            .map { it.key }

    fun getActionScore(npc: AINPC, context: NPCContext, action: NPCAction): Int =
        calculateActionScore(npc, context, action)

    private fun updateNeeds(npc: AINPC, context: NPCContext) {
        val now = System.currentTimeMillis()
        val previousTick = npc.lastSimulationTickAt
        npc.lastSimulationTickAt = now
        if (previousTick <= 0L) return

        val tickFactor = max(0.5, min(4.0, (now - previousTick) / 30000.0))
        val hungerDecay = plugin.config.getDouble("simulation.needs.hunger_decay", 1.5) * tickFactor
        val energyDecay = plugin.config.getDouble("simulation.needs.energy_decay", 1.2) * tickFactor
        val socialDecay = plugin.config.getDouble("simulation.needs.social_decay", 0.8) * tickFactor
        val comfortDecay = plugin.config.getDouble("simulation.needs.comfort_decay", 0.6) * tickFactor
        val safetyRecovery = plugin.config.getDouble("simulation.needs.safety_recovery", 1.0) * tickFactor

        npc.hungerLevel = npc.hungerLevel - (hungerDecay + if (npc.currentState.isWorkState()) 0.7 else 0.0).roundToInt()

        if (npc.currentState == NPCState.SLEEPING) {
            npc.energyLevel = npc.energyLevel + (3.5 * tickFactor).roundToInt()
        } else if (npc.currentState == NPCState.RESTING || context.isAtHome) {
            npc.energyLevel = npc.energyLevel + (1.6 * tickFactor).roundToInt()
        } else {
            npc.energyLevel = npc.energyLevel - (energyDecay + if (context.isAtWork) 0.5 else 0.0).roundToInt()
        }

        if (context.interactingPlayer != null || context.isFriendsNearby || npc.currentState.isSocialState()) {
            npc.socialNeedLevel = npc.socialNeedLevel + (2.4 * tickFactor).roundToInt()
        } else {
            npc.socialNeedLevel = npc.socialNeedLevel - socialDecay.roundToInt()
        }

        if (context.isAtHome || context.isIndoors) {
            npc.comfortLevel = npc.comfortLevel + (1.8 * tickFactor).roundToInt()
        } else {
            val weatherPenalty = if ("RAIN" == context.weather || "THUNDER" == context.weather) 1.2 else 0.0
            npc.comfortLevel = npc.comfortLevel - (comfortDecay + weatherPenalty).roundToInt()
        }

        if (context.isInDanger) {
            npc.safetyLevel = npc.safetyLevel - (4.0 * tickFactor).roundToInt()
        } else if (context.isAtHome) {
            npc.safetyLevel = npc.safetyLevel + (1.8 * tickFactor).roundToInt()
        } else {
            npc.safetyLevel = npc.safetyLevel + safetyRecovery.roundToInt()
        }
    }

    private fun applySimulationOutcome(npc: AINPC, context: NPCContext, action: NPCAction) {
        val targetState = mapActionToState(action, context)
        npc.currentState = targetState
        npc.currentGoal = resolveGoal(action, npc, context)
    }

    private fun mapActionToState(action: NPCAction, context: NPCContext): NPCState = when (action) {
        NPCAction.START_WORK, NPCAction.CONTINUE_WORK -> if (context.isAtWork) NPCState.WORKING else NPCState.WALKING
        NPCAction.CRAFT -> if (context.isAtWork) NPCState.CRAFTING else NPCState.WALKING
        NPCAction.FARM -> if (context.isAtWork) NPCState.FARMING else NPCState.WALKING
        NPCAction.MINE -> if (context.isAtWork) NPCState.MINING else NPCState.WALKING
        NPCAction.FISH -> if (context.isAtWork) NPCState.FISHING else NPCState.WALKING
        NPCAction.EAT -> if (context.isAtHome) NPCState.EATING else NPCState.WALKING
        NPCAction.SLEEP -> if (context.isAtHome) NPCState.SLEEPING else NPCState.WALKING
        NPCAction.REST -> if (context.isAtHome) NPCState.RESTING else NPCState.WALKING
        NPCAction.SOCIALIZE -> if (context.isAtSocialSpot) NPCState.SOCIALIZING else NPCState.WALKING
        NPCAction.TALK, NPCAction.LISTEN, NPCAction.GREET, NPCAction.SHARE_NEWS, NPCAction.TELL_STORY, NPCAction.GOSSIP -> NPCState.TALKING
        NPCAction.FLEE -> NPCState.FLEEING
        NPCAction.HIDE -> NPCState.HIDING
        NPCAction.ATTACK, NPCAction.DEFEND -> NPCState.COMBAT
        NPCAction.CALL_HELP, NPCAction.WARN -> NPCState.GUARDING
        NPCAction.OBSERVE, NPCAction.INVESTIGATE -> NPCState.CURIOUS
        NPCAction.WALK_RANDOM, NPCAction.WALK_TO_TARGET -> NPCState.WALKING
        NPCAction.RUN_TO_TARGET -> NPCState.RUNNING
        NPCAction.PRAY -> NPCState.PRAYING
        else -> NPCState.IDLE
    }

    private fun resolveGoal(action: NPCAction, npc: AINPC, context: NPCContext): String = when (action) {
        NPCAction.START_WORK, NPCAction.CONTINUE_WORK, NPCAction.CRAFT, NPCAction.FARM, NPCAction.MINE, NPCAction.FISH ->
            if (context.isAtWork) defaultGoal(context.plannedRoutineActivity, "sa isi faca treaba")
            else "ajunga la ${describeAnchor(npc.workAnchor, "locul de munca")}"
        NPCAction.EAT -> if (context.isAtHome) "isi recapete fortele la masa" else "ajunga acasa pentru a manca"
        NPCAction.SLEEP -> if (context.isAtHome) "se odihneasca in siguranta" else "se intoarca acasa pentru somn"
        NPCAction.REST -> if (context.isAtHome) "isi revina dupa efort" else "gaseasca un loc linistit de odihna"
        NPCAction.SOCIALIZE, NPCAction.TALK, NPCAction.GREET, NPCAction.SHARE_NEWS, NPCAction.TELL_STORY, NPCAction.GOSSIP ->
            if (context.isAtSocialSpot) "petreaca timp cu ceilalti localnici"
            else "ajunga la ${describeAnchor(npc.socialAnchor, "locul de intalnire")}"
        NPCAction.FLEE, NPCAction.HIDE -> "gaseasca adapost si sa evite pericolul"
        NPCAction.DEFEND, NPCAction.CALL_HELP, NPCAction.WARN -> "protejeze zona si sa ramana atent"
        NPCAction.OBSERVE, NPCAction.INVESTIGATE -> "inteleaga ce se petrece in jur"
        NPCAction.WALK_RANDOM -> defaultGoal(context.plannedRoutineActivity, "mai vada ce se intampla prin sat")
        else -> defaultGoal(context.plannedRoutineActivity, "isi urmeze rutina")
    }

    private fun resolveScheduledActivity(npc: AINPC, context: NPCContext): String {
        val loader = plugin.featurePackLoader
        if (loader != null) {
            val profession = loader.findPrimaryScenarioProfession(npc.occupation)
            if (profession != null) {
                val activity = profession.schedule[context.timeOfDay]
                if (!activity.isNullOrBlank()) return activity
            }
        }
        return when (context.timeOfDay) {
            "MORNING" -> "se pregateste pentru zi si isi verifica treburile"
            "AFTERNOON" -> "lucreaza sau isi rezolva datoriile"
            "EVENING" -> "inchide treburile zilei si cauta companie"
            "NIGHT" -> "doarme si se tine departe de pericole"
            else -> "isi urmeaza rutina obisnuita"
        }
    }

    private fun inferRoutineFocus(activity: String?): RoutineFocus {
        val normalized = activity?.lowercase(Locale.ROOT) ?: ""
        if (containsAny(normalized, "doarme", "somn", "odih", "masa", "mananca", "bea", "inchide")) return RoutineFocus.REST
        if (containsAny(normalized, "patrule", "paz", "poarta", "garda")) return RoutineFocus.GUARD
        if (containsAny(normalized, "social", "vorbeste", "barfa", "piata", "companie", "saluta")) return RoutineFocus.SOCIAL
        if (containsAny(normalized, "observ", "cauta", "verifica", "inspect")) return RoutineFocus.OBSERVE
        if (containsAny(normalized, "lucreaza", "forjeaza", "atelier", "camp", "recolta", "hraneste", "mestesug", "patruleaza")) return RoutineFocus.WORK
        return RoutineFocus.IDLE
    }

    private fun containsAny(text: String, vararg fragments: String): Boolean = fragments.any { text.contains(it) }

    private fun describeAnchor(anchor: AINPC.OwnedLocation?, fallback: String): String =
        if (anchor == null || anchor.label().isNullOrBlank()) fallback else anchor.label()

    private fun defaultGoal(plannedActivity: String?, fallback: String): String =
        if (plannedActivity.isNullOrBlank()) fallback else plannedActivity.lowercase(Locale.ROOT)

    private data class LocationWrapper(val world: org.bukkit.World, val location: org.bukkit.Location) {
        companion object {
            fun fromNpc(npc: AINPC): LocationWrapper? {
                val location = npc.location ?: return null
                val world = location.world ?: return null
                return LocationWrapper(world, location)
            }
        }
    }

    private enum class RoutineFocus { WORK, REST, SOCIAL, GUARD, OBSERVE, IDLE }

    companion object {
        private const val DECISION_COOLDOWN: Long = 1000
    }
}
