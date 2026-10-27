@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.engine

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.util.NumberConversions
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.RuntimeMetricNames
import ro.ainpc.commands.WorldBlueprintWizard
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCEmotions
import ro.ainpc.npc.NPCPersonality
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcAgentSnapshot
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcDecisionExplanation
import ro.ainpc.npc.NpcFoodRequestPreferences
import ro.ainpc.npc.NpcSelfCareGoal
import ro.ainpc.npc.NpcSelfCareContinuity
import ro.ainpc.npc.NpcSimulationMode
import ro.ainpc.npc.NpcSimulationLodPass
import ro.ainpc.npc.NpcLodPosition
import ro.ainpc.npc.NpcPerceptionSource
import ro.ainpc.npc.NpcPerceptionRefreshHistory
import ro.ainpc.npc.NpcPerceptionRefreshSettings
import ro.ainpc.npc.NpcSocialExperience
import java.util.EnumMap
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Decisionengine.
 */
class DecisionEngine internal constructor(
    private val plugin: AINPCPlugin,
    private val refreshWorldContext: (NPCContext, org.bukkit.World, org.bukkit.Location) -> Unit,
    private val needsClock: () -> Long,
    private val planningNanoClock: () -> Long,
) {
    internal constructor(
        plugin: AINPCPlugin,
        refreshWorldContext: (NPCContext, org.bukkit.World, org.bukkit.Location) -> Unit,
        needsClock: () -> Long,
    ) : this(plugin, refreshWorldContext, needsClock, System::nanoTime)

    internal constructor(
        plugin: AINPCPlugin,
        refreshWorldContext: (NPCContext, org.bukkit.World, org.bukkit.Location) -> Unit,
    ) : this(plugin, refreshWorldContext, System::currentTimeMillis)

    constructor(plugin: AINPCPlugin) : this(plugin, { context, world, location ->
        context.updateFromWorld(world, location)
    })

    internal data class ActionScoreBreakdown(
        val base: Int,
        val traits: Int,
        val emotions: Int,
        val memory: Int,
        val context: Int,
        val needs: Int,
        val routine: Int,
        val experience: Int = 0,
        val socialExperience: Int = 0,
    ) {
        val rawTotal: Int
            get() = base + traits + emotions + memory + context + needs + routine + experience + socialExperience
        val score: Int
            get() = max(0, rawTotal)
    }

    internal data class ActionDecisionPreview(
        val scores: Map<NPCAction, Int>,
        val probabilities: Map<NPCAction, Double>,
        val probabilistic: Boolean,
        val cached: Boolean,
        val blockedActions: Map<NPCAction, ActionBlockReason> = emptyMap(),
        val excludedActions: Map<NPCAction, ActionRankingExclusion> = emptyMap(),
        val cooldownActions: List<ActionCooldown> = emptyList(),
        val repeatAdjustment: RepeatAdjustment? = null,
        val repeatAdjustments: List<RepeatAdjustment> = emptyList(),
        val scoreBreakdowns: Map<NPCAction, ActionScoreBreakdown> = emptyMap(),
        val probabilityWeights: Map<NPCAction, Double> = emptyMap(),
        val agentSnapshot: NpcAgentSnapshot? = null,
        val carePlan: NpcCarePlanPreview? = null,
    )

    internal data class CognitiveDiagnostic(
        val capturedAt: Long,
        val state: NpcCognitiveState,
        val continuity: NpcSelfCareContinuity,
        val foodAid: FoodAidDiagnostic = FoodAidDiagnostic(),
    )

    internal data class FoodAidDiagnostic(
        val retainedReceipts: Int = 0,
        val lastReceipt: NpcSocialExperience.Receipt? = null,
        val viewerUuid: UUID? = null,
        val activeReply: NpcFoodRequestPreferences.Reply? = null,
    )

    private data class CachedDecision(
        val action: NPCAction,
        val state: NPCState,
        val inDanger: Boolean,
        val safetyLevel: Int,
        val timestamp: Long,
        val decisionSequence: Long,
        val recentExplorations: List<RecentExploration>,
        val recentActionTimestamps: Map<NPCAction, Long>,
        val explanation: NpcDecisionExplanation,
        val explanationRevision: Long,
        val returningForCare: Boolean,
    )

    private val decisionCache: MutableMap<UUID, CachedDecision> = ConcurrentHashMap()
    private val decisionStatistics: MutableMap<UUID, DecisionStatistics> = ConcurrentHashMap()
    private val foodRequests = NpcFoodRequestService(
        config = { plugin.config },
        isNpcInConversation = { plugin.conversationSessionManager.hasActiveConversation(it) },
        isPlayerBusy = {
            plugin.conversationSessionManager.isInConversation(it) ||
                plugin.guiService.hasTextInputRequest(it) || WorldBlueprintWizard.hasSession(it.uniqueId)
        },
        send = { player, npc, message -> plugin.messageUtils.sendNPCMessage(player, npc.name, message) },
        findNpc = { plugin.npcManager.getNPCByUuid(it) },
    )

    internal fun handleFoodRequestReply(player: Player, message: String, beforeAccept: (AINPC) -> Boolean): Boolean =
        foodRequests.tryReply(player, message, beforeAccept = beforeAccept)

    internal fun forgetFoodRequestPlayer(playerUuid: UUID) {
        foodRequests.forgetPlayer(playerUuid)
    }

    fun decideAction(npc: AINPC, context: NPCContext): NPCAction =
        decideAction(npc, context, actionSituation(npc, context))

    private fun decideAction(npc: AINPC, context: NPCContext, situation: ActionSituation): NPCAction {
        val now = System.currentTimeMillis()
        val cached = decisionCache[npc.uuid]
        if (cached != null && isReusable(cached, npc, context, situation, now)) {
            return cached.action
        }
        if (cached == null) evictIfNeeded(now)

        val agentSnapshot = NpcAgentSnapshot.capture(npc, context, now)
        val scoreBreakdowns = calculateAllScoreBreakdowns(npc, context, situation, agentSnapshot)
        val scores = scoreBreakdowns.mapValuesTo(EnumMap(NPCAction::class.java)) { it.value.score }
        val settings = ActionProbabilitySettings.fromConfig(
            plugin.config, npc.occupation, context.timeOfDay, context.weather,
        )
        val preview = previewForScores(
            npc, context, scores, cached?.recentExplorations.orEmpty(),
            cached?.recentActionTimestamps.orEmpty(), now, settings, scoreBreakdowns,
        )
        val decisionSequence = (cached?.decisionSequence ?: 0L) + 1L
        val action = if (preview.probabilistic) {
            val roll = settings.randomSeed?.let { seed ->
                deterministicDecisionRoll(seed, npc.uuid, decisionSequence)
            } ?: ThreadLocalRandom.current().nextDouble()
            ActionProbabilitySelector.select(preview.probabilities, roll)
        } else {
            preview.probabilities.keys.first()
        }
        val recentExplorations = ActionProbabilitySelector.recentExplorationsAfter(
            cached?.recentExplorations.orEmpty(), action, now, settings,
        )
        val recentActionTimestamps = ActionProbabilitySelector.recentActionTimestampsAfter(
            cached?.recentActionTimestamps.orEmpty(), action, now, settings,
        )
        decisionCache[npc.uuid] = CachedDecision(
            action, npc.currentState, context.isInDanger, context.safetyLevel, now, decisionSequence,
            recentExplorations, recentActionTimestamps,
            decisionExplanation(
                action, now, preview.probabilistic && preview.probabilities.size > 1, scoreBreakdowns[action],
            ).copy(continuingSelfCare = situation.returningForCare && action == NPCAction.WALK_TO_TARGET),
            npc.decisionRevision,
            situation.returningForCare,
        )
        decisionStatistics.computeIfAbsent(npc.uuid) { DecisionStatistics(settings.statisticsWindow) }
            .also { it.resize(settings.statisticsWindow) }
            .record(action, now, preview.probabilities)
        val chancePercent = (preview.probabilities.getValue(action) * 100).roundToInt()
        plugin.debug("NPC ${npc.name} decide: ${action.displayName} (scor: ${scores[action]}, probabilitate: $chancePercent%)")
        return action
    }

    internal fun previewActionDecision(npc: AINPC, context: NPCContext): ActionDecisionPreview {
        val now = System.currentTimeMillis()
        val agentSnapshot = NpcAgentSnapshot.capture(npc, context, now)
        val carePlan = if (plugin.config.getBoolean("simulation.decision.care_planner_shadow_enabled", false)) {
            NpcCarePlanner.plan(NpcCarePlanningInput.capture(npc, agentSnapshot))
        } else null
        val cached = decisionCache[npc.uuid]
        val situation = actionSituation(npc, context, agentSnapshot)
        if (cached != null && isReusable(cached, npc, context, situation, now)) {
            val action = cached.action
            val breakdown = calculateActionBreakdown(npc, context, action, situation, agentSnapshot)
            return ActionDecisionPreview(
                mapOf(action to breakdown.score),
                mapOf(action to 1.0),
                probabilistic = false,
                cached = true,
                scoreBreakdowns = mapOf(action to breakdown),
                agentSnapshot = agentSnapshot,
                carePlan = carePlan,
            )
        }
        val scoreBreakdowns = calculateAllScoreBreakdowns(npc, context, situation, agentSnapshot)
        val scores = scoreBreakdowns.mapValuesTo(EnumMap(NPCAction::class.java)) { it.value.score }
        val settings = ActionProbabilitySettings.fromConfig(
            plugin.config, npc.occupation, context.timeOfDay, context.weather,
        )
        return previewForScores(
            npc, context, scores, cached?.recentExplorations.orEmpty(),
            cached?.recentActionTimestamps.orEmpty(), now, settings, scoreBreakdowns,
            includeExplanations = true,
        ).copy(
            blockedActions = blockedActions(agentSnapshot.cognition.currentState, agentSnapshot.cognition.isInDanger, situation),
            agentSnapshot = agentSnapshot,
            carePlan = carePlan,
        )
    }

    private fun isReusable(
        cached: CachedDecision, npc: AINPC, context: NPCContext, situation: ActionSituation, now: Long,
    ): Boolean = now - cached.timestamp in 0 until DECISION_COOLDOWN &&
        cached.explanationRevision == npc.decisionRevision &&
        cached.returningForCare == situation.returningForCare &&
        cached.state == npc.currentState && cached.inDanger == context.isInDanger &&
        cached.safetyLevel == context.safetyLevel &&
        ActionEligibility.permits(cached.action, situation)

    private fun previewForScores(
        npc: AINPC, context: NPCContext, scores: Map<NPCAction, Int>,
        recentExplorations: List<RecentExploration>, recentActionTimestamps: Map<NPCAction, Long>,
        now: Long, settings: ActionProbabilitySettings,
        scoreBreakdowns: Map<NPCAction, ActionScoreBreakdown> = emptyMap(),
        includeExplanations: Boolean = false,
    ): ActionDecisionPreview {
        val probabilistic = ActionProbabilitySelector.isEnabledFor(
            npc.currentState,
            context.isInDanger,
            plugin.config.getBoolean("simulation.decision.probabilistic_enabled", true),
        )
        val probabilities = if (probabilistic) {
            ActionProbabilitySelector.probabilities(
                scores,
                settings = settings,
                recentExplorationAges = recentExplorations.associate { it.action to (now - it.timestamp) },
                actionCooldownAges = recentActionTimestamps.mapValues { (_, timestamp) -> now - timestamp },
            )
        } else {
            mapOf(getHighestScoringAction(scores) to 1.0)
        }
        val cooldowns = if (probabilistic) {
            ActionProbabilitySelector.activeCooldowns(
                scores.keys,
                recentActionTimestamps.mapValues { (_, timestamp) -> now - timestamp },
                settings,
            )
        } else emptyList()
        val excludedActions = if (includeExplanations) {
            ActionProbabilitySelector.excludedByRanking(
                scores, probabilities.keys, probabilistic, settings,
                cooldownActions = cooldowns.map { it.action }.toSet(),
            )
        } else emptyMap()
        val repeatAdjustments = if (includeExplanations && probabilistic) {
            ActionProbabilitySelector.repeatAdjustments(probabilities, recentExplorations, now, settings)
        } else emptyList()
        val probabilityWeights = if (includeExplanations && probabilistic) {
            probabilities.keys.mapNotNull { action ->
                settings.actionWeights[action]
                    ?.takeIf { it != 1.0 }
                    ?.let { action to it }
            }.toMap()
        } else emptyMap()
        return ActionDecisionPreview(scores, probabilities, probabilistic, cached = false,
            excludedActions = excludedActions, cooldownActions = cooldowns,
            repeatAdjustment = repeatAdjustments.firstOrNull(),
            repeatAdjustments = repeatAdjustments, scoreBreakdowns = scoreBreakdowns,
            probabilityWeights = probabilityWeights)
    }

    private fun evictIfNeeded(now: Long) {
        if (decisionCache.size >= MAX_CACHE_SIZE) {
            val cutoff = now - CACHE_TTL_MS
            val stale = decisionCache.entries.filter { it.value.timestamp < cutoff }
            stale.forEach {
                if (decisionCache.remove(it.key, it.value)) decisionStatistics.remove(it.key)
            }
            if (decisionCache.size >= MAX_CACHE_SIZE) {
                decisionCache.entries.minByOrNull { it.value.timestamp }?.let {
                    if (decisionCache.remove(it.key, it.value)) decisionStatistics.remove(it.key)
                }
            }
        }
    }

    internal fun decisionStatistics(uuid: UUID): DecisionStatistics.Snapshot =
        decisionStatistics[uuid]?.snapshot() ?: DecisionStatistics.Snapshot(0, emptyMap(), null)

    private var activeCarePlanningBatch: NpcCarePlanningBatch? = null
    private var lastCarePlanningActor: UUID? = null
    @Volatile private var lastCarePlanningReport: NpcCarePlanningReport? = null

    internal fun carePlanningReport(): NpcCarePlanningReport? = lastCarePlanningReport

    internal fun runLifeSimulationBatch(npcs: Collection<AINPC>, process: (AINPC) -> Unit) {
        if (activeCarePlanningBatch != null ||
            !plugin.config.getBoolean("simulation.decision.care_planner_execution_enabled", false)
        ) {
            npcs.forEach(process)
            return
        }
        val actors = npcs.toList()
        val previousIndex = actors.indexOfFirst { it.uuid == lastCarePlanningActor }
        val startIndex = if (actors.isEmpty()) 0 else (previousIndex + 1) % actors.size
        val batch = NpcCarePlanningBatch(NpcCarePlanningLimits.fromConfig(plugin.config), planningNanoClock)
        activeCarePlanningBatch = batch
        var completed = false
        try {
            for (offset in actors.indices) process(actors[(startIndex + offset) % actors.size])
            completed = true
        } finally {
            activeCarePlanningBatch = null
            batch.lastActorUuid?.let { lastCarePlanningActor = it }
            lastCarePlanningReport = batch.report(completed, System.currentTimeMillis())
        }
    }

    fun runLifeSimulationTick(npc: AINPC?) {
        if (npc == null || !npc.isSpawned()) return
        runLifeSimulationTick(npc, NpcSimulationLodPass.capture(plugin.config))
    }

    internal fun runLifeSimulationTick(npc: AINPC?, lod: NpcSimulationLodPass) {
        if (npc == null || !npc.isSpawned()) return
        if (lod.enabled && npc.simulationMode == NpcSimulationMode.NONE) return
        if (activeCarePlanningBatch == null &&
            plugin.config.getBoolean("simulation.decision.care_planner_execution_enabled", false)
        ) {
            runLifeSimulationBatch(listOf(npc)) { runLifeSimulationTick(it, lod) }
            return
        }
        val revision = npc.decisionRevision
        val location = LocationWrapper.fromNpc(npc) ?: run {
            npc.actionExperience.cancelPending()
            npc.selfCareGoal.clear()
            return
        }

        val context = npc.context
        if (!refreshSimulationPerception(npc, context, location, lod, revision)) return
        if (lod.enabled && lod.modeFor(npc) == NpcSimulationMode.LIGHT) {
            npc.actionExperience.cancelPending()
            npc.selfCareGoal.clear()
            if (updateNeeds(npc, context) == null) return
            context.syncSimulationState(location.location)
            return
        }
        val scheduledActivity = resolveScheduledActivity(npc, context)
        npc.plannedRoutineActivity = scheduledActivity
        if (!updateNeedsWithExperience(npc, context)) return
        context.syncSimulationState(location.location)
        try {
            if (foodRequests.tryRequest(npc)) {
                plugin.debug("NPC ${npc.name} cere mancare unui jucator care l-a ajutat anterior.")
            }
        } catch (exception: RuntimeException) {
            plugin.debug("Cerere de mancare omisa pentru ${npc.name}: ${exception.javaClass.simpleName}")
        }

        if (tryExecuteCarePlanAtHome(npc, context)) {
            if (npc.isSpawned()) context.syncSimulationState(npc.bukkitEntity?.location)
            return
        }
        val situation = actionSituation(npc, context)
        val action = decideAction(npc, context, situation)
        applySimulationOutcome(npc, context, action, situation)
        if (!npc.isSpawned()) return
        context.syncSimulationState(npc.bukkitEntity?.location)
    }

    private fun refreshSimulationPerception(
        npc: AINPC, context: NPCContext, location: LocationWrapper, lod: NpcSimulationLodPass, revision: Long,
    ): Boolean {
        if (context.npc !== npc) return false
        val settings = if (lod.enabled && plugin.config.getBoolean("simulation.lod.perception.enabled", false)) {
            NpcPerceptionRefreshSettings.fromConfig(plugin.config)
        } else null
        val entity = npc.bukkitEntity
        if (settings == null || !settings.enabled) {
            npc.perceptionRefreshHistory = null
            refreshWorldContext(context, location.world, location.location)
            return npc.isSpawned() && npc.decisionRevision == revision && npc.context === context && npc.bukkitEntity === entity
        }
        check(Bukkit.isPrimaryThread()) { "Perception LOD requires the Paper main thread" }
        val serverTick = Bukkit.getCurrentTick()
        val position = NpcLodPosition.fromLocation(location.location)
        val source = NpcPerceptionSource.capture(npc, context, position)
        val age = if (lod.modeFor(npc) == NpcSimulationMode.LIGHT && npc.simulationMode == NpcSimulationMode.FULL) {
            npc.perceptionRefreshHistory?.reuseAge(settings, source, serverTick)
        } else null
        if (age != null) {
            context.syncSimulationState(location.location)
            if (npc.isSpawned() && npc.decisionRevision == revision && npc.context === context && npc.bukkitEntity === entity &&
                lod.modeFor(npc) == NpcSimulationMode.LIGHT) {
                context.reusePerceptionObservation(age)
                return true
            }
        }
        npc.perceptionRefreshHistory = null
        val observationToken = context.perceptionObservationToken
        refreshWorldContext(context, location.world, location.location)
        if (!npc.isSpawned() || npc.decisionRevision != revision || npc.context !== context || npc.bukkitEntity !== entity) return false
        if (context.perceptionObservationToken === observationToken) context.recordPerceptionObservation()
        val refreshedSource = NpcPerceptionSource.capture(npc, context, position)
        npc.perceptionRefreshHistory = refreshedSource?.let { NpcPerceptionRefreshHistory(settings, it, serverTick) }
        return true
    }

    private fun carePlanExecutionAllowed(npc: AINPC): Boolean =
        plugin.config.getBoolean("simulation.decision.care_planner_execution_enabled", false) &&
            plugin.config.getBoolean("features.simulation", false) &&
            plugin.config.getBoolean("simulation.enabled", false) &&
            plugin.config.getBoolean("features.routine", false) &&
            plugin.config.getBoolean("routine.enabled", false) &&
            npc.simulationMode == NpcSimulationMode.FULL && npc.isSpawned() && Bukkit.isPrimaryThread() &&
            !plugin.routineCoordinator.isRoutinePaused(npc.uuid) && plugin.routineCoordinator.getOverride(npc.uuid) == null

    private fun tryExecuteCarePlanAtHome(npc: AINPC, context: NPCContext): Boolean {
        if (!carePlanExecutionAllowed(npc) || !context.isAtHome) return false
        val batch = activeCarePlanningBatch ?: return false
        val outcome = batch.execute(npc.uuid) {
            val timer = plugin.performanceMonitor.timer(RuntimeMetricNames.CARE_PLANNER)
            timer.begin()
            var success = false
            try {
                val result = executeCarePlanAtHome(npc, context)
                success = true
                result
            } finally {
                timer.end(success, 1)
            }
        }
        if (outcome != null) return outcome.endsTick
        val now = System.currentTimeMillis()
        if (npc.selfCareGoal.snapshot(now)?.stage != NpcSelfCareGoal.Stage.RECOVERING) return false
        return npc.selfCareGoal.canKeepRecovery(
            NpcCognitiveState.capture(npc, context, now), experienceContextKey(npc), context.isAtHome, now,
        )
    }

    private fun executeCarePlanAtHome(npc: AINPC, context: NPCContext): NpcCarePlanExecutor.Outcome {
        val input = NpcCarePlanningInput.capture(npc, NpcAgentSnapshot.capture(npc, context))
        val plan = NpcCarePlanner.plan(input)
        return NpcCarePlanExecutor.executeAtHome(
            plan,
            currentInput = { NpcCarePlanningInput.capture(npc, NpcAgentSnapshot.capture(npc, context)) },
            policyAllows = { carePlanExecutionAllowed(npc) },
            homeAvailable = { careHomeAvailable(npc, input) },
            apply = { action ->
                decisionCache.remove(npc.uuid)
                applySimulationOutcome(npc, context, action, ActionSituation(
                    false, false, false, isAtHome = true,
                    hungerLevel = input.agent.cognition.hungerLevel, energyLevel = input.agent.cognition.energyLevel,
                    currentState = input.agent.cognition.currentState,
                ), plan)
            },
        )
    }

    private fun careHomeAvailable(npc: AINPC, input: NpcCarePlanningInput): Boolean {
        val home = input.home ?: return false
        val position = npc.bukkitEntity?.location ?: return false
        val world = position.world ?: return false
        if (!npc.isSpawned() || !home.isConfirmedIn(npc.worldName) || world.name != home.worldName ||
            !position.x.isFinite() || !position.y.isFinite() || !position.z.isFinite()
        ) return false
        if (!world.isChunkLoaded(NumberConversions.floor(home.x) shr 4, NumberConversions.floor(home.z) shr 4)) {
            return false
        }
        val deltaX = position.x - home.x
        val deltaY = position.y - home.y
        val deltaZ = position.z - home.z
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= 5.5 * 5.5
    }

    private fun carePlanSurvivesTransition(
        npc: AINPC, context: NPCContext, plan: NpcCarePlanPreview, expectedState: NPCState, expectedStateRevision: Long,
    ): Boolean {
        val available = careHomeAvailable(npc, plan.input)
        val current = NpcCarePlanningInput.capture(npc, NpcAgentSnapshot.capture(npc, context))
        if (current.agent.stateRevision != expectedStateRevision || current.agent.cognition.currentState != expectedState) {
            return false
        }
        val normalized = current.copy(agent = current.agent.copy(
            stateRevision = plan.input.agent.stateRevision,
            cognition = current.agent.cognition.copy(currentState = plan.input.agent.cognition.currentState),
        ))
        return available && carePlanExecutionAllowed(npc) && plan.invalidationReason(normalized) == null
    }

    fun updateBasicNeeds(npc: AINPC?) {
        if (npc == null || !npc.isSpawned()) return
        val revision = npc.decisionRevision
        npc.actionExperience.cancelPending()
        npc.selfCareGoal.clear()
        val location = LocationWrapper.fromNpc(npc) ?: return

        val context = npc.context
        refreshWorldContext(context, location.world, location.location)
        if (!npc.isSpawned() || npc.decisionRevision != revision) return
        if (updateNeeds(npc, context) == null) return
        context.syncSimulationState(location.location)
    }

    private fun calculateAllScores(
        npc: AINPC,
        context: NPCContext,
        situation: ActionSituation = actionSituation(npc, context),
    ): MutableMap<NPCAction, Int> {
        return calculateAllScoreBreakdowns(npc, context, situation)
            .mapValuesTo(EnumMap(NPCAction::class.java)) { it.value.score }
    }

    private fun calculateAllScoreBreakdowns(
        npc: AINPC,
        context: NPCContext,
        situation: ActionSituation = actionSituation(npc, context),
        agentSnapshot: NpcAgentSnapshot = NpcAgentSnapshot.capture(npc, context),
    ): MutableMap<NPCAction, ActionScoreBreakdown> {
        val breakdowns: MutableMap<NPCAction, ActionScoreBreakdown> = EnumMap(NPCAction::class.java)
        val possibleActions = eligibleActions(agentSnapshot.cognition.currentState, agentSnapshot.cognition.isInDanger, situation)
        for (action in possibleActions) {
            breakdowns[action] = calculateActionBreakdown(npc, context, action, situation, agentSnapshot)
        }
        return breakdowns
    }

    private fun actionSituation(
        npc: AINPC, context: NPCContext, agentSnapshot: NpcAgentSnapshot = NpcAgentSnapshot.capture(npc, context),
    ): ActionSituation {
        val cognition = agentSnapshot.cognition
        val socialTargets = nearbySocialTargets(npc, context)
        val chunkAvailable = { worldName: String, chunkX: Int, chunkZ: Int ->
            Bukkit.getWorld(worldName)?.isChunkLoaded(chunkX, chunkZ) == true
        }
        return ActionSituation(
            hasSocialTarget = socialTargets.hasSocialTarget,
            hasWorkplace = ActionTargetAvailability.hasUsableAnchor(npc.workAnchor, chunkAvailable),
            hasSocialDestination = ActionTargetAvailability.hasUsableAnchor(npc.socialAnchor, chunkAvailable),
            hasInteractingPlayer = socialTargets.hasInteractingPlayer,
            isAtHome = agentSnapshot.perception.isAtHome,
            hungerLevel = cognition.hungerLevel,
            energyLevel = cognition.energyLevel,
            currentState = cognition.currentState,
            interactingPlayerUuid = context.interactingPlayer?.takeIf { socialTargets.hasInteractingPlayer }?.uniqueId,
            returningForCare = canContinueSelfCare(npc, context),
        )
    }

    private fun canContinueSelfCare(npc: AINPC, context: NPCContext): Boolean =
        selfCareContinuity(npc, context, System.currentTimeMillis()) == NpcSelfCareContinuity.READY

    internal fun cognitiveDiagnostic(
        npc: AINPC, context: NPCContext = npc.context, now: Long = System.currentTimeMillis(),
        viewerUuid: UUID? = null,
    ): CognitiveDiagnostic {
        val state = NpcCognitiveState.capture(npc, context, now)
        val receipts = npc.socialExperience.snapshot()
        val foodAid = FoodAidDiagnostic(receipts.size, receipts.lastOrNull(), viewerUuid,
            viewerUuid?.let { npc.foodRequestPreferences.activeReply(it, now) })
        return CognitiveDiagnostic(now, state, selfCareContinuity(npc, context, now, state), foodAid)
    }

    private fun selfCareContinuity(
        npc: AINPC, context: NPCContext, now: Long, captured: NpcCognitiveState? = null,
    ): NpcSelfCareContinuity {
        if (npc.simulationMode != NpcSimulationMode.FULL) return NpcSelfCareContinuity.MODE_NOT_FULL
        if (!plugin.config.getBoolean("simulation.decision.goal_continuity_enabled", false)) {
            return NpcSelfCareContinuity.FEATURE_DISABLED
        }
        if (!plugin.config.getBoolean("features.simulation", false) ||
            !plugin.config.getBoolean("simulation.enabled", false)
        ) return NpcSelfCareContinuity.SIMULATION_DISABLED
        if (!plugin.config.getBoolean("features.routine", false) ||
            !plugin.config.getBoolean("routine.enabled", false)
        ) return NpcSelfCareContinuity.ROUTINE_DISABLED
        if (context.interactingNPC != null) return NpcSelfCareContinuity.INTERLOCUTOR
        val status = npc.selfCareGoal.returnStatus(
            captured ?: NpcCognitiveState.capture(npc, context, now), experienceContextKey(npc), context.isAtHome, now,
        )
        if (status != NpcSelfCareContinuity.READY) return status
        if (plugin.routineCoordinator.isRoutinePaused(npc.uuid)) return NpcSelfCareContinuity.ROUTINE_PAUSED
        if (plugin.routineCoordinator.getOverride(npc.uuid) != null) return NpcSelfCareContinuity.ROUTINE_OVERRIDE
        return NpcSelfCareContinuity.READY
    }

    private fun nearbySocialTargets(npc: AINPC, context: NPCContext): SocialTargetStatus {
        val interactingPlayer = context.interactingPlayer?.takeIf { it.isOnline }?.location
        val otherTargets = buildList {
            context.interactingNPC?.takeIf { it.uuid != npc.uuid && it.isSpawned() }?.location?.let { add(it) }
            context.nearbyPlayers.filter { it.isOnline }.forEach { add(it.location) }
            context.nearbyNPCs.filter { it.uuid != npc.uuid && it.isSpawned() }
                .forEach { it.location?.let { location -> add(location) } }
        }
        val directRadius = plugin.config.getDouble("npc.interaction_distance", 5.0)
            .takeIf { it.isFinite() && it > 0.0 } ?: 5.0
        val passiveRadius = plugin.config.getDouble("dialog.passive_listen_radius", 8.0)
            .takeIf { it.isFinite() && it > 0.0 } ?: 8.0
        return SocialTargetProximity.evaluate(
            npc.location, interactingPlayer, otherTargets, maxOf(directRadius, passiveRadius),
        )
    }

    private fun calculateActionScore(
        npc: AINPC, context: NPCContext, action: NPCAction, situation: ActionSituation,
    ): Int = calculateActionBreakdown(npc, context, action, situation).score

    private fun calculateActionBreakdown(
        npc: AINPC, context: NPCContext, action: NPCAction, situation: ActionSituation,
    ): ActionScoreBreakdown = calculateActionBreakdown(
        npc, context, action, situation, NpcAgentSnapshot.capture(npc, context),
    )

    private fun calculateActionBreakdown(
        npc: AINPC, context: NPCContext, action: NPCAction, situation: ActionSituation,
        agentSnapshot: NpcAgentSnapshot,
    ): ActionScoreBreakdown = ActionScoreBreakdown(
        base = action.baseScore,
        traits = getTraitModifier(npc.personality, action),
        emotions = getEmotionModifier(npc.emotions, action),
        memory = getMemoryModifier(agentSnapshot.cognition, context, action, situation.hasInteractingPlayer),
        context = getContextModifier(action, situation.hasInteractingPlayer, agentSnapshot),
        needs = agentSnapshot.cognition.needModifier(action),
        routine = getRoutineModifier(agentSnapshot.cognition, action),
        experience = if (experienceEnabled(npc)) npc.actionExperience.scoreModifier(
            action, agentSnapshot.cognition, experienceContextKey(npc),
            agentSnapshot.perception.isAtHome, agentSnapshot.capturedAtMillis,
        ) else 0,
        socialExperience = if (experienceEnabled(npc) && situation.hasInteractingPlayer) {
            situation.interactingPlayerUuid?.let { playerUuid ->
                npc.socialExperience.scoreModifier(
                    playerUuid, action, agentSnapshot.cognition, agentSnapshot.capturedAtMillis,
                )
            } ?: 0
        } else 0,
    )

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

    private fun getMemoryModifier(
        cognition: NpcCognitiveState, context: NPCContext, action: NPCAction, hasInteractingPlayer: Boolean,
    ): Int {
        var modifier = 0
        if (!hasInteractingPlayer) return modifier

        val relationshipLevel = context.relationshipLevel
        val routineFocus = inferRoutineFocus(cognition.plannedRoutineActivity)
        if (action.isFriendly()) modifier += relationshipLevel / 5
        if (action.isAggressive()) {
            modifier += if (relationshipLevel < -25) abs(relationshipLevel) / 3 else -relationshipLevel / 3
        }
        if (routineFocus == RoutineFocus.SOCIAL && action.isFriendly()) modifier += 5
        if (routineFocus == RoutineFocus.WORK && action.getCategory() == NPCAction.ActionCategory.WORK) modifier += 5

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

    private fun getContextModifier(
        action: NPCAction, hasInteractingPlayer: Boolean,
        agentSnapshot: NpcAgentSnapshot,
    ): Int {
        val perception = agentSnapshot.perception
        var modifier = 0
        if (agentSnapshot.cognition.isInDanger) {
            if (action == NPCAction.FLEE || action == NPCAction.CALL_HELP) modifier += 50
            if (action == NPCAction.DEFEND || action == NPCAction.ATTACK) modifier += 30
            if (action.getCategory() == NPCAction.ActionCategory.SOCIAL) modifier -= 30
            if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier -= 40
        }
        if ("NIGHT" == perception.timeOfDay) {
            if (action == NPCAction.SLEEP || action == NPCAction.REST) modifier += 35
            if (action.isWorkState()) modifier -= 20
        }
        if ("MORNING" == perception.timeOfDay) {
            if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier += 20
            if (action == NPCAction.GREET) modifier += 15
        }
        if ("RAIN" == perception.weather || "THUNDER" == perception.weather) {
            if (!perception.isIndoors) {
                if (action == NPCAction.HIDE) modifier += 25
                if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier -= 15
            }
        }
        if (agentSnapshot.cognition.hungerLevel < 30 && action == NPCAction.EAT) modifier += 40
        if (perception.healthPercent < 50) {
            if (action == NPCAction.REST || action == NPCAction.HEAL) modifier += 30
            if (action == NPCAction.FLEE) modifier += 20
        }
        if (perception.isFamilyNearby && (action == NPCAction.SOCIALIZE || action == NPCAction.TALK)) modifier += 25
        if (perception.isAtWork && action.getCategory() == NPCAction.ActionCategory.WORK) modifier += 30
        if (perception.isAtHome && (action == NPCAction.REST || action == NPCAction.EAT)) modifier += 15
        if (hasInteractingPlayer) {
            if (action == NPCAction.TALK || action == NPCAction.LISTEN) modifier += 40
            if (action == NPCAction.GREET && SocialTargetProximity.isRecentInteraction(
                    perception.lastInteractionTime, agentSnapshot.capturedAtMillis,
                )) modifier += 30
        }
        return modifier
    }

    private fun getRoutineModifier(cognition: NpcCognitiveState, action: NPCAction): Int {
        val focus = inferRoutineFocus(cognition.plannedRoutineActivity)
        return when (focus) {
            RoutineFocus.WORK -> if (action.getCategory() == NPCAction.ActionCategory.WORK) 26 else 0
            RoutineFocus.REST -> if (action == NPCAction.SLEEP || action == NPCAction.REST || action == NPCAction.EAT) 28 else 0
            RoutineFocus.SOCIAL -> if (action == NPCAction.SOCIALIZE || action == NPCAction.TALK || action == NPCAction.GREET || action == NPCAction.SHARE_NEWS) 22 else 0
            RoutineFocus.GUARD -> if (action == NPCAction.OBSERVE || action == NPCAction.WARN || action == NPCAction.DEFEND || action == NPCAction.CALL_HELP) 24 else 0
            RoutineFocus.OBSERVE -> if (action == NPCAction.OBSERVE || action == NPCAction.INVESTIGATE) 18 else 0
            RoutineFocus.IDLE -> if (action == NPCAction.WALK_RANDOM || action == NPCAction.OBSERVE) 8 else 0
        }
    }

    private fun getHighestScoringAction(scores: Map<NPCAction, Int>): NPCAction =
        scores.maxByOrNull { it.value }?.key ?: NPCAction.DO_NOTHING

    fun getTopActions(npc: AINPC, context: NPCContext, count: Int): List<NPCAction> =
        calculateAllScores(npc, context).entries
            .sortedByDescending { it.value }
            .take(count)
            .map { it.key }

    fun getActionScore(npc: AINPC, context: NPCContext, action: NPCAction): Int =
        calculateActionScore(npc, context, action, actionSituation(npc, context))

    private fun updateNeedsWithExperience(npc: AINPC, context: NPCContext): Boolean {
        val before = NpcCognitiveState.capture(npc, context)
        val interval = updateNeeds(npc, context) ?: return false
        val after = NpcCognitiveState.capture(npc, context)
        val homeKey = experienceContextKey(npc)
        if (npc.simulationMode == NpcSimulationMode.FULL) {
            npc.selfCareGoal.observe(before, after, homeKey, context.isAtHome,
                interval.previousAtMillis, interval.observedAtMillis)
        } else {
            npc.selfCareGoal.clear()
        }
        if (experienceEnabled(npc)) {
            npc.actionExperience.observe(
                before, after, homeKey,
                context.isAtHome, npc.stateRevision, interval.previousAtMillis, interval.observedAtMillis,
            )
        } else {
            npc.actionExperience.cancelPending()
        }
        return true
    }

    private fun experienceEnabled(npc: AINPC): Boolean =
        npc.simulationMode == NpcSimulationMode.FULL &&
            plugin.config.getBoolean("simulation.decision.experience_enabled", true)

    private fun experienceContextKey(npc: AINPC): String = NpcSelfCareGoal.homeKey(npc)

    private fun updateNeeds(npc: AINPC, context: NPCContext): NpcSimulationInterval? {
        val now = needsClock()
        val progressLease = if (plugin.config.getBoolean("simulation.progress.enabled", false)) {
            npc.simulationProgress.preparePhysical(npc.uuid, npc.lastSimulationTickAt, now) ?: return null
        } else null
        val input = NpcNeedsInput.capture(npc, context, now, progressLease)
        val rates = if (input.interval.advances) NpcNeedsRates(
            plugin.config.getDouble("simulation.needs.hunger_decay", 1.5),
            plugin.config.getDouble("simulation.needs.energy_decay", 1.2),
            plugin.config.getDouble("simulation.needs.social_decay", 0.8),
            plugin.config.getDouble("simulation.needs.comfort_decay", 0.6),
            plugin.config.getDouble("simulation.needs.safety_recovery", 1.0),
        ) else NpcNeedsRates()
        val transition = NpcNeedsTransition.calculate(input, rates)
        if (progressLease != null && !plugin.config.getBoolean("simulation.progress.enabled", false)) return null
        if (input.spawned && !npc.isSpawned()) return null
        return input.interval.takeIf { transition.applyTo(npc, context) }
    }

    private fun applySimulationOutcome(
        npc: AINPC, context: NPCContext, action: NPCAction, situation: ActionSituation,
    ) {
        applySimulationOutcome(npc, context, action, situation, null)
    }

    private fun applySimulationOutcome(
        npc: AINPC, context: NPCContext, action: NPCAction, situation: ActionSituation, carePlan: NpcCarePlanPreview?,
    ): Boolean {
        val revision = npc.decisionRevision
        val decision = decisionCache[npc.uuid]?.takeIf {
            it.action == action && it.explanationRevision == npc.decisionRevision
        }
        val targetState = mapActionToState(action, context, situation)
        val expectedStateRevision = npc.stateRevision + if (npc.currentState == targetState) 0L else 1L
        npc.changeStateFromSimulation(targetState)
        if (npc.decisionRevision != revision || npc.stateRevision != expectedStateRevision ||
            npc.currentState != targetState
        ) return false
        if (carePlan != null && !carePlanSurvivesTransition(npc, context, carePlan, targetState, expectedStateRevision)) {
            return false
        }
        npc.currentGoal = resolveGoal(action, npc, context, situation)
        if (npc.simulationMode == NpcSimulationMode.FULL && npc.currentState == targetState &&
            npc.decisionRevision == revision
        ) {
            if (action == NPCAction.WALK_TO_TARGET && situation.returningForCare) {
                npc.selfCareGoal.continueReturn(
                    NpcCognitiveState.capture(npc, context), experienceContextKey(npc), System.currentTimeMillis(),
                )
            } else {
                npc.selfCareGoal.applied(
                    action, NpcCognitiveState.capture(npc, context), experienceContextKey(npc),
                    context.isAtHome, System.currentTimeMillis(),
                    allowRest = carePlan != null,
                )
            }
        } else {
            npc.selfCareGoal.clear()
        }
        val explanation = if (carePlan != null) {
            NpcDecisionExplanation(action, System.currentTimeMillis(), false, fromCarePlan = true)
        } else decision?.explanation
        npc.rememberDecision(explanation?.takeIf {
            npc.simulationMode == NpcSimulationMode.FULL && npc.currentState == targetState &&
                (if (carePlan != null) revision else decision?.explanationRevision) == npc.decisionRevision
        })
        if (experienceEnabled(npc)) {
            npc.actionExperience.begin(
                action, NpcCognitiveState.capture(npc, context), experienceContextKey(npc),
                context.isAtHome, npc.stateRevision, npc.lastSimulationTickAt,
                allowRest = carePlan != null,
            )
        } else {
            npc.actionExperience.cancelPending()
        }
        return true
    }

    private fun mapActionToState(action: NPCAction, context: NPCContext, situation: ActionSituation): NPCState = when (action) {
        NPCAction.START_WORK, NPCAction.CONTINUE_WORK -> if (context.isAtWork) NPCState.WORKING else NPCState.WALKING
        NPCAction.CRAFT -> if (context.isAtWork) NPCState.CRAFTING else NPCState.WALKING
        NPCAction.FARM -> if (context.isAtWork) NPCState.FARMING else NPCState.WALKING
        NPCAction.MINE -> if (context.isAtWork) NPCState.MINING else NPCState.WALKING
        NPCAction.FISH -> if (context.isAtWork) NPCState.FISHING else NPCState.WALKING
        NPCAction.EAT -> if (context.isAtHome) NPCState.EATING else NPCState.WALKING
        NPCAction.SLEEP -> if (context.isAtHome) NPCState.SLEEPING else NPCState.WALKING
        NPCAction.REST -> if (context.isAtHome) NPCState.RESTING else NPCState.WALKING
        NPCAction.SOCIALIZE -> SocialActionOutcome.state(context.isAtSocialSpot, situation.hasSocialTarget)
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

    private fun resolveGoal(action: NPCAction, npc: AINPC, context: NPCContext, situation: ActionSituation): String = when (action) {
        NPCAction.START_WORK, NPCAction.CONTINUE_WORK, NPCAction.CRAFT, NPCAction.FARM, NPCAction.MINE, NPCAction.FISH ->
            if (context.isAtWork) defaultGoal(context.plannedRoutineActivity, "sa isi faca treaba")
            else "ajunga la ${describeAnchor(npc.workAnchor, "locul de munca")}"
        NPCAction.EAT -> if (context.isAtHome) "isi recapete fortele la masa" else "ajunga acasa pentru a manca"
        NPCAction.SLEEP -> if (context.isAtHome) "se odihneasca in siguranta" else "se intoarca acasa pentru somn"
        NPCAction.REST -> if (context.isAtHome) "isi revina dupa efort" else "gaseasca un loc linistit de odihna"
        NPCAction.SOCIALIZE -> SocialActionOutcome.goal(
            context.isAtSocialSpot, situation.hasSocialTarget, describeAnchor(npc.socialAnchor, "locul de intalnire"),
        )
        NPCAction.TALK, NPCAction.GREET, NPCAction.SHARE_NEWS, NPCAction.TELL_STORY, NPCAction.GOSSIP ->
            "interactioneze cu interlocutorul"
        NPCAction.FLEE, NPCAction.HIDE -> "gaseasca adapost si sa evite pericolul"
        NPCAction.DEFEND, NPCAction.CALL_HELP, NPCAction.WARN -> "protejeze zona si sa ramana atent"
        NPCAction.OBSERVE, NPCAction.INVESTIGATE -> "inteleaga ce se petrece in jur"
        NPCAction.WALK_RANDOM -> defaultGoal(context.plannedRoutineActivity, "mai vada ce se intampla prin sat")
        NPCAction.WALK_TO_TARGET -> if (situation.returningForCare) "revina acasa pentru ingrijire"
            else defaultGoal(context.plannedRoutineActivity, "ajunga la destinatie")
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

    fun removeNpcFromCache(uuid: UUID) {
        decisionCache.remove(uuid)
        decisionStatistics.remove(uuid)
    }

    fun clearCache() {
        decisionCache.clear()
        decisionStatistics.clear()
    }

    private enum class RoutineFocus { WORK, REST, SOCIAL, GUARD, OBSERVE, IDLE }

    companion object {
        internal fun decisionExplanation(
            action: NPCAction, now: Long, variedChoice: Boolean, breakdown: ActionScoreBreakdown?,
        ): NpcDecisionExplanation {
            if (breakdown == null) return NpcDecisionExplanation(action, now, variedChoice)
            val factors = listOf(
                NpcDecisionExplanation.Factor.NEEDS to breakdown.needs,
                NpcDecisionExplanation.Factor.CONTEXT to breakdown.context,
                NpcDecisionExplanation.Factor.ROUTINE to breakdown.routine,
                NpcDecisionExplanation.Factor.TRAITS to breakdown.traits,
                NpcDecisionExplanation.Factor.EMOTIONS to breakdown.emotions,
                NpcDecisionExplanation.Factor.MEMORY to breakdown.memory,
                NpcDecisionExplanation.Factor.EXPERIENCE to breakdown.experience,
                NpcDecisionExplanation.Factor.SOCIAL_HELP to breakdown.socialExperience,
            ).filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
            return NpcDecisionExplanation(action, now, variedChoice, factors.getOrNull(0), factors.getOrNull(1))
        }

        internal fun deterministicDecisionRoll(seed: Long, uuid: UUID, sequence: Long): Double {
            var value = seed xor uuid.mostSignificantBits xor
                java.lang.Long.rotateLeft(uuid.leastSignificantBits, 23) xor
                sequence * SPLIT_MIX_GAMMA
            value = (value xor (value ushr 30)) * SPLIT_MIX_MULTIPLIER_1
            value = (value xor (value ushr 27)) * SPLIT_MIX_MULTIPLIER_2
            value = value xor (value ushr 31)
            return (value ushr 11).toDouble() / 9_007_199_254_740_992.0
        }

        internal fun eligibleActions(state: NPCState, inDanger: Boolean, situation: ActionSituation): List<NPCAction> {
            ongoingCareAction(state, inDanger, situation)?.let { return listOf(it) }
            return candidateActionsWithCare(state, inDanger).filter { ActionEligibility.permits(it, situation) }
        }

        internal fun blockedActions(
            state: NPCState, inDanger: Boolean, situation: ActionSituation,
        ): Map<NPCAction, ActionBlockReason> {
            if (ongoingCareAction(state, inDanger, situation) != null) return emptyMap()
            return candidateActionsWithCare(state, inDanger).mapNotNull { action ->
                ActionEligibility.blockReason(action, situation)?.let { action to it }
            }.toMap(linkedMapOf())
        }

        private fun ongoingCareAction(
            state: NPCState, inDanger: Boolean, situation: ActionSituation,
        ): NPCAction? {
            if (situation.returningForCare && !inDanger && !situation.isAtHome &&
                !situation.hasInteractingPlayer && state == NPCState.WALKING
            ) return NPCAction.WALK_TO_TARGET
            if (!inDanger && situation.isAtHome) {
                val ongoingCare = when (state) {
                    NPCState.EATING -> NPCAction.EAT
                    NPCState.SLEEPING -> NPCAction.SLEEP
                    else -> null
                }
                if (ongoingCare != null && ActionEligibility.permits(ongoingCare, situation)) {
                    return ongoingCare
                }
            }
            return null
        }

        private fun candidateActionsWithCare(state: NPCState, inDanger: Boolean): List<NPCAction> {
            val actions = candidateActions(state, inDanger).toMutableList()
            if (!inDanger && !state.isDangerState() && state != NPCState.TALKING) {
                actions.add(NPCAction.EAT)
                actions.add(NPCAction.SLEEP)
            }
            return actions
        }

        internal fun nextHungerLevel(
            current: Int,
            state: NPCState,
            isAtHome: Boolean,
            hungerDecay: Double,
            tickFactor: Double,
        ): Int = NpcNeedsModel.nextHungerLevel(current, state, isAtHome, hungerDecay, tickFactor)

        internal fun candidateActions(state: NPCState, inDanger: Boolean): List<NPCAction> {
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
            if (inDanger) {
                if (NPCAction.FLEE !in actions) actions.add(NPCAction.FLEE)
                if (NPCAction.CALL_HELP !in actions) actions.add(NPCAction.CALL_HELP)
            }
            return actions
        }

        private const val DECISION_COOLDOWN: Long = 1000
        private const val CACHE_TTL_MS: Long = 300_000
        private const val MAX_CACHE_SIZE: Int = 500
        private const val SPLIT_MIX_GAMMA: Long = -7046029254386353131L
        private const val SPLIT_MIX_MULTIPLIER_1: Long = -4658895280553007687L
        private const val SPLIT_MIX_MULTIPLIER_2: Long = -7723592293110705685L
    }
}
