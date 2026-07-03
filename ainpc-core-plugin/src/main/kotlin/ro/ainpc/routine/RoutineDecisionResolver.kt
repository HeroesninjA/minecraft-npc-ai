package ro.ainpc.routine

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import ro.ainpc.topology.TopologyCategory

class RoutineDecisionResolver {
    fun assign(
        npc: AINPC?,
        profile: BehaviorProfile?,
        defaultProfile: BehaviorProfile?,
        routineTime: Long
    ): RoutineAssignment {
        if (npc == null) {
            return idle(null, null, "nu exista NPC valid")
        }

        if (profile != null && profile.schedule.isNotEmpty()) {
            for (entry in profile.schedule) {
                if (routineTime >= entry.startTick && routineTime < entry.endTick) {
                    val slot = when (entry.slot.uppercase()) {
                        "HOME" -> RoutineSlot.HOME
                        "WORK" -> RoutineSlot.WORK
                        "SOCIAL" -> RoutineSlot.SOCIAL
                        else -> RoutineSlot.IDLE
                    }
                    val anchor = when (slot) {
                        RoutineSlot.HOME -> npc.homeAnchor
                        RoutineSlot.WORK -> npc.workAnchor
                        RoutineSlot.SOCIAL -> npc.socialAnchor
                        RoutineSlot.IDLE -> null
                    }
                    val stateName = entry.state.ifBlank { slotState(profile, defaultProfile, slot.name, defaultStateForSlot(slot)) }
                    return RoutineAssignment(
                        slot,
                        entry.activity,
                        entry.target.ifBlank { entry.label },
                        when (slot) {
                            RoutineSlot.HOME -> parseNpcState(stateName, NPCState.RESTING)
                            RoutineSlot.WORK -> workStateForNpc(npc, profile, defaultProfile, stateName)
                            RoutineSlot.SOCIAL -> parseNpcState(stateName, NPCState.SOCIALIZING)
                            RoutineSlot.IDLE -> parseNpcState(stateName, NPCState.IDLE)
                        },
                        anchor
                    )
                }
            }
        }

        resolveFallbackAssignment(npc, profile, defaultProfile, routineTime)?.let { return it }

        return idle(profile, defaultProfile, routineText(profile, defaultProfile, "idle", "isi urmeaza rutina obisnuita"))
    }

    private fun zoneWorkState(npc: AINPC): NPCState {
        return zoneWorkState(npc, null, null)
    }

    private fun zoneWorkState(npc: AINPC, profile: BehaviorProfile?, defaultProfile: BehaviorProfile?): NPCState {
        val zone = npc.context.topologyCategory
        if (zone == null) return NPCState.WORKING
        val stateName = zoneStateName(profile, defaultProfile, zone)
        return parseNpcState(stateName, NPCState.WORKING)
    }

    private fun zoneWorkActivity(npc: AINPC, base: String): String {
        return zoneWorkActivity(npc, base, null, null)
    }

    private fun zoneWorkActivity(npc: AINPC, base: String, profile: BehaviorProfile?, defaultProfile: BehaviorProfile?): String {
        val zone = npc.context.topologyCategory
        if (zone == null) return base
        val suffix = zoneActivitySuffix(profile, defaultProfile, zone)
        return if (suffix.isBlank()) base else "$base $suffix"
    }

    private fun home(npc: AINPC, profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, activity: String, stateName: String? = null): RoutineAssignment {
        val state = parseNpcState(stateName ?: slotState(profile, defaultProfile, "HOME", "RESTING"), NPCState.RESTING)
        return RoutineAssignment(
            RoutineSlot.HOME,
            activity,
            routineGoal(profile, "home", "sa fie acasa"),
            state,
            npc.homeAnchor
        )
    }

    private fun work(npc: AINPC, profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, activity: String, stateName: String? = null): RoutineAssignment {
        return RoutineAssignment(
            RoutineSlot.WORK,
            activity,
            routineGoal(profile, "work", "sa lucreze la " + describeAnchor(npc.workAnchor, "locul de munca")),
            workStateForNpc(npc, profile, defaultProfile, stateName),
            npc.workAnchor
        )
    }

    private fun social(npc: AINPC, profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, activity: String, stateName: String? = null): RoutineAssignment {
        return RoutineAssignment(
            RoutineSlot.SOCIAL,
            activity,
            routineGoal(profile, "social", "sa socializeze la " + describeAnchor(npc.socialAnchor, "punctul social")),
            parseNpcState(stateName ?: slotState(profile, defaultProfile, "SOCIAL", "SOCIALIZING"), NPCState.SOCIALIZING),
            npc.socialAnchor
        )
    }

    private fun idle(profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, reason: String, stateName: String? = null): RoutineAssignment {
        return RoutineAssignment(
            RoutineSlot.IDLE,
            reason,
            routineGoal(profile, "idle", "sa astepte pana exista o ancora utila"),
            parseNpcState(stateName ?: slotState(profile, defaultProfile, "IDLE", "IDLE"), NPCState.IDLE),
            null
        )
    }

    private fun workStateForNpc(npc: AINPC, profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, stateName: String? = null): NPCState {
        if (!stateName.isNullOrBlank()) {
            return parseNpcState(stateName, zoneWorkState(npc, profile, defaultProfile))
        }
        return zoneWorkState(npc, profile, defaultProfile)
    }

    private fun hasAnchor(anchor: AINPC.OwnedLocation?): Boolean {
        return anchor != null && !anchor.worldName().isNullOrBlank()
    }

    private fun describeAnchor(anchor: AINPC.OwnedLocation?, fallback: String): String {
        val label = anchor?.label()
        if (label.isNullOrBlank()) {
            return fallback
        }
        return label
    }

    private fun routineGoal(profile: BehaviorProfile?, key: String, fallback: String): String {
        return profile?.routineGoals?.get(key)?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun phaseTick(profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, key: String, fallback: Long): Long {
        return profile?.phaseTicks?.get(key) ?: defaultProfile?.phaseTicks?.get(key) ?: fallback
    }

    private fun routineText(profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, key: String, fallback: String): String {
        return profile?.routineTexts?.get(key)?.takeIf { it.isNotBlank() }
            ?: defaultProfile?.routineTexts?.get(key)?.takeIf { it.isNotBlank() }
            ?: fallback
    }

    private fun resolveFallbackAssignment(
        npc: AINPC,
        profile: BehaviorProfile?,
        defaultProfile: BehaviorProfile?,
        routineTime: Long
    ): RoutineAssignment? {
        val rules = fallbackRules(profile, defaultProfile)
        for (rule in rules) {
            if (!matchesFallbackRule(rule.condition, npc, routineTime, profile, defaultProfile)) {
                continue
            }
            val activity = routineText(profile, defaultProfile, rule.activityKey, rule.activityKey)
            return when (rule.slot.uppercase()) {
                "HOME" -> home(npc, profile, defaultProfile, activity, ruleState(rule, profile, defaultProfile))
                "WORK" -> work(npc, profile, defaultProfile, zoneWorkActivity(npc, activity, profile, defaultProfile), ruleState(rule, profile, defaultProfile))
                "SOCIAL" -> social(npc, profile, defaultProfile, activity, ruleState(rule, profile, defaultProfile))
                "IDLE" -> idle(profile, defaultProfile, activity, ruleState(rule, profile, defaultProfile))
                else -> null
            }
        }
        return null
    }

    private fun fallbackRules(profile: BehaviorProfile?, defaultProfile: BehaviorProfile?): List<BehaviorProfile.FallbackRule> {
        return profile?.fallbackRules?.takeIf { it.isNotEmpty() }
            ?: defaultProfile?.fallbackRules
            ?: emptyList()
    }

    private fun matchesFallbackRule(
        condition: String,
        npc: AINPC,
        routineTime: Long,
        profile: BehaviorProfile?,
        defaultProfile: BehaviorProfile?
    ): Boolean {
        val nightStart = phaseTick(null, defaultProfile, "night_start", 18000L)
        val wakeStart = phaseTick(null, defaultProfile, "wake_start", 2000L)
        val socialStart = phaseTick(null, defaultProfile, "social_start", 8000L)
        val socialEnd = phaseTick(null, defaultProfile, "social_end", 18000L)
        val workStart = phaseTick(null, defaultProfile, "work_start", 2000L)
        val middayStart = phaseTick(null, defaultProfile, "midday_start", 12000L)
        val eveningStart = phaseTick(null, defaultProfile, "evening_start", 16000L)
        val energyLow = threshold(profile, defaultProfile, "energy_low", 25)
        val hungerLow = threshold(profile, defaultProfile, "hunger_low", 25)
        val safetyLow = threshold(profile, defaultProfile, "safety_low", 35)
        val socialNeedLow = threshold(profile, defaultProfile, "social_need_low", 35)

        return when (condition.lowercase()) {
            "night" -> routineTime >= nightStart || routineTime < wakeStart
            "low_needs" -> npc.energyLevel < energyLow || npc.hungerLevel < hungerLow || npc.safetyLevel < safetyLow
            "social_need" -> npc.socialNeedLevel < socialNeedLow && hasAnchor(npc.socialAnchor) && routineTime >= socialStart && routineTime < socialEnd
            "work_morning" -> hasAnchor(npc.workAnchor) && routineTime >= workStart && routineTime < middayStart
            "work_afternoon" -> hasAnchor(npc.workAnchor) && routineTime >= middayStart && routineTime < eveningStart
            "social_evening" -> hasAnchor(npc.socialAnchor) && routineTime >= eveningStart && routineTime < socialEnd
            "default" -> true
            else -> false
        }
    }

    private fun threshold(profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, key: String, fallback: Int): Int {
        return profile?.thresholds?.get(key) ?: defaultProfile?.thresholds?.get(key) ?: fallback
    }

    private fun slotState(profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, key: String, fallback: String): String {
        val normalizedKey = key.lowercase()
        return profile?.slotStates?.get(normalizedKey)
            ?: defaultProfile?.slotStates?.get(normalizedKey)
            ?: defaultProfile?.slotStates?.get("default")
            ?: fallback
    }

    private fun defaultStateForSlot(slot: RoutineSlot): String {
        return defaultStateForSlotName(slot.name)
    }

    private fun defaultStateForSlotName(slotName: String): String {
        return when (slotName.uppercase()) {
            "HOME" -> "RESTING"
            "WORK" -> "WORKING"
            "SOCIAL" -> "SOCIALIZING"
            "IDLE" -> "IDLE"
            else -> "IDLE"
        }
    }

    private fun ruleState(rule: BehaviorProfile.FallbackRule, profile: BehaviorProfile?, defaultProfile: BehaviorProfile?): String? {
        if (rule.state.isNotBlank()) {
            return rule.state
        }
        return slotState(profile, defaultProfile, rule.slot, defaultStateForSlotName(rule.slot))
    }

    private fun zoneStateName(profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, zone: TopologyCategory): String {
        val key = zone.name.lowercase()
        return profile?.zoneStates?.get(key)
            ?: defaultProfile?.zoneStates?.get(key)
            ?: defaultProfile?.zoneStates?.get("default")
            ?: "WORKING"
    }

    private fun zoneActivitySuffix(profile: BehaviorProfile?, defaultProfile: BehaviorProfile?, zone: TopologyCategory): String {
        val key = zone.name.lowercase()
        return profile?.zoneActivitySuffixes?.get(key)
            ?: defaultProfile?.zoneActivitySuffixes?.get(key)
            ?: defaultProfile?.zoneActivitySuffixes?.get("default")
            ?: ""
    }

    private fun parseNpcState(value: String, fallback: NPCState): NPCState {
        return runCatching { NPCState.valueOf(value.uppercase()) }.getOrDefault(fallback)
    }
}
