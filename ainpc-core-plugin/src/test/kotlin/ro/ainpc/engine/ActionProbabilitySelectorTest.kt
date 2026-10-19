package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState
import java.util.UUID

class ActionProbabilitySelectorTest {
    @Test
    fun `only nearby useful actions enter the distribution`() {
        val probabilities = ActionProbabilitySelector.probabilities(mapOf(
            NPCAction.DO_NOTHING to 0,
            NPCAction.START_WORK to 50,
            NPCAction.OBSERVE to 48,
            NPCAction.REST to 47,
            NPCAction.WALK_RANDOM to 32,
        ))

        assertEquals(listOf(NPCAction.START_WORK, NPCAction.OBSERVE, NPCAction.REST), probabilities.keys.toList())
        assertEquals(1.0, probabilities.values.sum(), 1e-9)
        assertTrue(probabilities.getValue(NPCAction.START_WORK) > probabilities.getValue(NPCAction.OBSERVE))
        assertTrue(probabilities.getValue(NPCAction.OBSERVE) > probabilities.getValue(NPCAction.REST))
    }

    @Test
    fun `optional action weights change probabilities without changing eligibility`() {
        val probabilities = ActionProbabilitySelector.probabilities(
            mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40),
            settings = ActionProbabilitySettings(
                actionWeights = mapOf(NPCAction.WALK_RANDOM to 2.0),
            ),
        )

        assertEquals(listOf(NPCAction.WALK_RANDOM, NPCAction.OBSERVE), probabilities.keys.toList())
        assertEquals(2.0 / 3.0, probabilities.getValue(NPCAction.WALK_RANDOM), 1e-9)
        assertEquals(1.0 / 3.0, probabilities.getValue(NPCAction.OBSERVE), 1e-9)
        assertEquals(1.0, probabilities.values.sum(), 1e-9)
    }

    @Test
    fun `optional idle weight adds a deliberate pause without replacing useful actions`() {
        val probabilities = ActionProbabilitySelector.probabilities(
            mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40),
            settings = ActionProbabilitySettings(idleWeight = 0.1),
        )

        assertEquals(listOf(NPCAction.WALK_RANDOM, NPCAction.OBSERVE, NPCAction.DO_NOTHING), probabilities.keys.toList())
        assertEquals(1.0, probabilities.values.sum(), 1e-9)
        assertTrue(probabilities.getValue(NPCAction.DO_NOTHING) > 0.0)
        assertEquals(NPCAction.DO_NOTHING, ActionProbabilitySelector.select(probabilities, 0.999999))
    }

    @Test
    fun `action weight can tune the optional idle probability`() {
        val base = ActionProbabilitySelector.probabilities(
            mapOf(NPCAction.WALK_RANDOM to 40),
            settings = ActionProbabilitySettings(idleWeight = 0.1),
        )
        val boosted = ActionProbabilitySelector.probabilities(
            mapOf(NPCAction.WALK_RANDOM to 40),
            settings = ActionProbabilitySettings(
                idleWeight = 0.1,
                actionWeights = mapOf(NPCAction.DO_NOTHING to 2.0),
            ),
        )

        assertEquals(0.1 / 1.1, base.getValue(NPCAction.DO_NOTHING), 1e-9)
        assertEquals(0.2 / 1.2, boosted.getValue(NPCAction.DO_NOTHING), 1e-9)
    }

    @Test
    fun `idle weight consumes one configured choice slot`() {
        val probabilities = ActionProbabilitySelector.probabilities(
            mapOf(
                NPCAction.WALK_RANDOM to 40,
                NPCAction.OBSERVE to 39,
                NPCAction.REST to 38,
            ),
            settings = ActionProbabilitySettings(maxChoices = 2, idleWeight = 0.1),
        )

        assertEquals(listOf(NPCAction.WALK_RANDOM, NPCAction.DO_NOTHING), probabilities.keys.toList())
        assertEquals(1.0, probabilities.values.sum(), 1e-9)
    }

    @Test
    fun `one random draw selects an action from cumulative probabilities`() {
        val probabilities = ActionProbabilitySelector.probabilities(mapOf(
            NPCAction.START_WORK to 50,
            NPCAction.OBSERVE to 48,
            NPCAction.REST to 47,
        ))
        val firstBoundary = probabilities.getValue(NPCAction.START_WORK)

        assertEquals(NPCAction.START_WORK, ActionProbabilitySelector.select(probabilities, 0.0))
        assertEquals(NPCAction.OBSERVE, ActionProbabilitySelector.select(probabilities, firstBoundary))
        assertEquals(NPCAction.REST, ActionProbabilitySelector.select(probabilities, 0.999999))
    }

    @Test
    fun `empty or nonpositive scores fall back to doing nothing`() {
        for (scores in listOf(emptyMap(), mapOf(NPCAction.OBSERVE to 0, NPCAction.REST to -3))) {
            assertEquals(mapOf(NPCAction.DO_NOTHING to 1.0), ActionProbabilitySelector.probabilities(scores))
        }
    }

    @Test
    fun `equal scores use stable order and at most three candidates`() {
        val probabilities = ActionProbabilitySelector.probabilities(mapOf(
            NPCAction.OBSERVE to 40,
            NPCAction.REST to 40,
            NPCAction.START_WORK to 40,
            NPCAction.WALK_RANDOM to 40,
        ))

        assertEquals(listOf(NPCAction.WALK_RANDOM, NPCAction.START_WORK, NPCAction.REST), probabilities.keys.toList())
        assertEquals(1.0 / 3.0, probabilities.getValue(NPCAction.WALK_RANDOM), 1e-9)
    }

    @Test
    fun `recent exploration receives lower weight without becoming forbidden`() {
        val scores = mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40)
        val probabilities = ActionProbabilitySelector.probabilities(scores, NPCAction.WALK_RANDOM)

        assertEquals(1.0 / 3.0, probabilities.getValue(NPCAction.WALK_RANDOM), 1e-9)
        assertEquals(2.0 / 3.0, probabilities.getValue(NPCAction.OBSERVE), 1e-9)
        assertEquals(NPCAction.WALK_RANDOM, ActionProbabilitySelector.select(probabilities, 0.0))
    }

    @Test
    fun `configured repetition weight changes only recent exploratory actions`() {
        val scores = mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40)
        val reduced = ActionProbabilitySelector.probabilities(
            scores, NPCAction.WALK_RANDOM, ActionProbabilitySettings(repeatWeight = 0.25),
        )
        val unpenalized = ActionProbabilitySelector.probabilities(
            scores, NPCAction.WALK_RANDOM, ActionProbabilitySettings(repeatWeight = 1.0),
        )

        assertEquals(0.2, reduced.getValue(NPCAction.WALK_RANDOM), 1e-9)
        assertEquals(0.5, unpenalized.getValue(NPCAction.WALK_RANDOM), 1e-9)
        assertEquals(1.0, reduced.values.sum(), 1e-9)
    }

    @Test
    fun `configured repetition window has clear inclusive and exclusive boundaries`() {
        val settings = ActionProbabilitySettings(repeatWindowSeconds = 90)

        assertTrue(settings.includesPreviousAction(0L))
        assertTrue(settings.includesPreviousAction(89_999L))
        assertFalse(settings.includesPreviousAction(90_000L))
        assertFalse(settings.includesPreviousAction(-1L))
        assertFalse(settings.copy(repeatWindowSeconds = 0).includesPreviousAction(0L))
    }

    @Test
    fun `repetition penalty fades smoothly until its window expires`() {
        val scores = mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40)
        val settings = ActionProbabilitySettings(repeatWeight = 0.5, repeatWindowSeconds = 90)
        fun chance(ageMillis: Long): Double = ActionProbabilitySelector.probabilities(
            scores, NPCAction.WALK_RANDOM, settings, ageMillis,
        ).getValue(NPCAction.WALK_RANDOM)

        assertEquals(1.0 / 3.0, chance(0L), 1e-9)
        assertEquals(3.0 / 7.0, chance(45_000L), 1e-9)
        assertTrue(chance(89_999L) < 0.5)
        assertEquals(0.5, chance(90_000L), 1e-9)
        assertEquals(0.5, chance(-1L), 1e-9)
        assertEquals(0.5, ActionProbabilitySelector.probabilities(
            scores, NPCAction.WALK_RANDOM, settings.copy(repeatWindowSeconds = 0),
        ).getValue(NPCAction.WALK_RANDOM), 1e-9)
    }

    @Test
    fun `diagnostic reports only an effective repeat adjustment`() {
        val settings = ActionProbabilitySettings(repeatWindowSeconds = 90)
        val recent = RecentExploration(NPCAction.WALK_RANDOM, 1_000L)
        val scores = mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40)
        val probabilities = ActionProbabilitySelector.probabilities(scores, recent.action, settings, 45_000L)

        assertEquals(RepeatAdjustment(NPCAction.WALK_RANDOM, 0.75, 45_000L),
            ActionProbabilitySelector.repeatAdjustment(probabilities, recent, 46_000L, settings))
        assertEquals(null, ActionProbabilitySelector.repeatAdjustment(
            mapOf(NPCAction.WALK_RANDOM to 1.0), recent, 46_000L, settings))
        assertEquals(null, ActionProbabilitySelector.repeatAdjustment(
            mapOf(NPCAction.OBSERVE to 0.5, NPCAction.REST to 0.5), recent, 46_000L, settings))
        assertEquals(null, ActionProbabilitySelector.repeatAdjustment(probabilities, recent, 91_000L, settings))
        assertEquals(null, ActionProbabilitySelector.repeatAdjustment(
            probabilities, recent, 46_000L, settings.copy(repeatWeight = 1.0)))
    }

    @Test
    fun `recent exploration survives other actions until its own window expires`() {
        val settings = ActionProbabilitySettings(repeatWindowSeconds = 90)
        val exploration = ActionProbabilitySelector.recentExplorationAfter(null, NPCAction.WALK_RANDOM, 1_000L, settings)
        val afterRest = ActionProbabilitySelector.recentExplorationAfter(exploration, NPCAction.REST, 60_000L, settings)
        val scores = mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40)

        assertEquals(exploration, afterRest)
        val repeatedChance = ActionProbabilitySelector.probabilities(
            scores, afterRest?.action, settings, 60_000L - requireNotNull(afterRest).timestamp,
        ).getValue(NPCAction.WALK_RANDOM)
        assertTrue(repeatedChance > 1.0 / 3.0 && repeatedChance < 0.5)
        assertEquals(null, ActionProbabilitySelector.recentExplorationAfter(afterRest, NPCAction.REST, 91_000L, settings))
    }

    @Test
    fun `expanded exploration history penalizes each recent exploration`() {
        val settings = ActionProbabilitySettings(repeatHistorySize = 3)
        val afterWalk = ActionProbabilitySelector.recentExplorationsAfter(
            emptyList(), NPCAction.WALK_RANDOM, 1_000L, settings,
        )
        val afterObserve = ActionProbabilitySelector.recentExplorationsAfter(
            afterWalk, NPCAction.OBSERVE, 2_000L, settings,
        )
        val scores = mapOf(
            NPCAction.WALK_RANDOM to 40,
            NPCAction.OBSERVE to 40,
            NPCAction.REST to 40,
        )
        val probabilities = ActionProbabilitySelector.probabilities(
            scores,
            settings = settings,
            recentExplorationAges = afterObserve.associate { it.action to (2_000L - it.timestamp) },
        )

        assertEquals(listOf(NPCAction.OBSERVE, NPCAction.WALK_RANDOM), afterObserve.map { it.action })
        val walkWeight = settings.repeatMultiplier(1_000L)
        val observeWeight = settings.repeatMultiplier(0L)
        val totalWeight = walkWeight + observeWeight + 1.0
        assertEquals(walkWeight / totalWeight, probabilities.getValue(NPCAction.WALK_RANDOM), 1e-9)
        assertEquals(observeWeight / totalWeight, probabilities.getValue(NPCAction.OBSERVE), 1e-9)
        assertEquals(1.0 / totalWeight, probabilities.getValue(NPCAction.REST), 1e-9)
    }

    @Test
    fun `active action cooldown removes an action when an alternative exists`() {
        val settings = ActionProbabilitySettings(
            actionCooldowns = mapOf(NPCAction.WALK_RANDOM to 60),
        )
        val probabilities = ActionProbabilitySelector.probabilities(
            mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40),
            settings = settings,
            actionCooldownAges = mapOf(NPCAction.WALK_RANDOM to 1_000L),
        )

        assertEquals(mapOf(NPCAction.OBSERVE to 1.0), probabilities)
        assertEquals(
            listOf(ActionCooldown(NPCAction.WALK_RANDOM, 59_000L)),
            ActionProbabilitySelector.activeCooldowns(
                setOf(NPCAction.WALK_RANDOM, NPCAction.OBSERVE),
                mapOf(NPCAction.WALK_RANDOM to 1_000L),
                settings,
            ),
        )
    }

    @Test
    fun `cooldown only promotes alternatives within the original score window`() {
        val scores = mapOf(
            NPCAction.WALK_RANDOM to 50,
            NPCAction.OBSERVE to 47,
            NPCAction.REST to 30,
        )
        val settings = ActionProbabilitySettings(
            scoreWindow = 5,
            actionCooldowns = mapOf(NPCAction.WALK_RANDOM to 60, NPCAction.OBSERVE to 60),
        )

        assertEquals(
            mapOf(NPCAction.OBSERVE to 1.0),
            ActionProbabilitySelector.probabilities(
                scores, settings = settings,
                actionCooldownAges = mapOf(NPCAction.WALK_RANDOM to 1_000L),
            ),
        )
        assertEquals(
            mapOf(NPCAction.WALK_RANDOM to 1.0),
            ActionProbabilitySelector.probabilities(
                scores, settings = settings,
                actionCooldownAges = mapOf(
                    NPCAction.WALK_RANDOM to 1_000L,
                    NPCAction.OBSERVE to 1_000L,
                ),
            ),
        )
    }

    @Test
    fun `diagnostic reports active cooldown as the exclusion reason`() {
        val exclusions = ActionProbabilitySelector.excludedByRanking(
            mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40, NPCAction.REST to 10),
            selected = setOf(NPCAction.OBSERVE),
            probabilistic = true,
            cooldownActions = setOf(NPCAction.WALK_RANDOM, NPCAction.REST),
        )

        assertEquals(ActionRankingExclusion.ACTION_COOLDOWN, exclusions[NPCAction.WALK_RANDOM])
        assertEquals(ActionRankingExclusion.OUTSIDE_SCORE_WINDOW, exclusions[NPCAction.REST])
    }

    @Test
    fun `cooldown fallback keeps the best action when every candidate is blocked`() {
        val settings = ActionProbabilitySettings(
            actionCooldowns = mapOf(NPCAction.WALK_RANDOM to 60, NPCAction.OBSERVE to 60),
        )

        val probabilities = ActionProbabilitySelector.probabilities(
            mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 35),
            settings = settings,
            actionCooldownAges = mapOf(
                NPCAction.WALK_RANDOM to 1_000L,
                NPCAction.OBSERVE to 1_000L,
            ),
        )

        assertEquals(mapOf(NPCAction.WALK_RANDOM to 1.0), probabilities)
    }

    @Test
    fun `idle cooldown removes voluntary pause but keeps fallback when no useful action exists`() {
        val settings = ActionProbabilitySettings(
            maxChoices = 2,
            idleWeight = 0.5,
            actionCooldowns = mapOf(NPCAction.DO_NOTHING to 60),
        )
        val cooldownAges = mapOf(NPCAction.DO_NOTHING to 1_000L)

        assertEquals(
            listOf(NPCAction.WALK_RANDOM, NPCAction.OBSERVE),
            ActionProbabilitySelector.probabilities(
                mapOf(NPCAction.WALK_RANDOM to 40, NPCAction.OBSERVE to 40),
                settings = settings, actionCooldownAges = cooldownAges,
            ).keys.toList(),
        )
        assertEquals(
            mapOf(NPCAction.DO_NOTHING to 1.0),
            ActionProbabilitySelector.probabilities(
                emptyMap(), settings = settings, actionCooldownAges = cooldownAges,
            ),
        )
    }

    @Test
    fun `recent action cooldown timestamps retain only active configured actions`() {
        val settings = ActionProbabilitySettings(
            actionCooldowns = mapOf(NPCAction.GREET to 60),
        )
        val first = ActionProbabilitySelector.recentActionTimestampsAfter(
            emptyMap(), NPCAction.GREET, 1_000L, settings,
        )
        val retained = ActionProbabilitySelector.recentActionTimestampsAfter(
            first, NPCAction.OBSERVE, 2_000L, settings,
        )
        val expired = ActionProbabilitySelector.recentActionTimestampsAfter(
            retained, NPCAction.OBSERVE, 61_001L, settings,
        )

        assertEquals(mapOf(NPCAction.GREET to 1_000L), first)
        assertEquals(first, retained)
        assertEquals(emptyMap<NPCAction, Long>(), expired)
    }

    @Test
    fun `new exploration replaces old one and disabled window clears history`() {
        val settings = ActionProbabilitySettings()
        val previous = RecentExploration(NPCAction.WALK_RANDOM, 1_000L)

        assertEquals(RecentExploration(NPCAction.OBSERVE, 2_000L),
            ActionProbabilitySelector.recentExplorationAfter(previous, NPCAction.OBSERVE, 2_000L, settings))
        assertEquals(null, ActionProbabilitySelector.recentExplorationAfter(
            previous, NPCAction.REST, 2_000L, settings.copy(repeatWindowSeconds = 0)))
    }

    @Test
    fun `repetition does not weaken work or remove the only useful action`() {
        val workScores = mapOf(NPCAction.CONTINUE_WORK to 40, NPCAction.REST to 40)
        val workProbabilities = ActionProbabilitySelector.probabilities(workScores, NPCAction.CONTINUE_WORK)
        val onlyAction = ActionProbabilitySelector.probabilities(
            mapOf(NPCAction.OBSERVE to 40),
            NPCAction.OBSERVE,
        )

        assertEquals(0.5, workProbabilities.getValue(NPCAction.CONTINUE_WORK), 1e-9)
        assertEquals(mapOf(NPCAction.OBSERVE to 1.0), onlyAction)
    }

    @Test
    fun `configured choice limit score window and temperature shape the distribution`() {
        val scores = mapOf(
            NPCAction.WALK_RANDOM to 50,
            NPCAction.OBSERVE to 45,
            NPCAction.REST to 40,
        )
        val settings = ActionProbabilitySettings(maxChoices = 2, scoreWindow = 5, temperature = 20.0)
        val probabilities = ActionProbabilitySelector.probabilities(scores, settings = settings)
        val windowOnly = ActionProbabilitySelector.probabilities(
            scores,
            settings = settings.copy(maxChoices = 3),
        )
        val choiceLimitOnly = ActionProbabilitySelector.probabilities(
            scores,
            settings = settings.copy(scoreWindow = 15),
        )

        assertEquals(listOf(NPCAction.WALK_RANDOM, NPCAction.OBSERVE), probabilities.keys.toList())
        assertEquals(listOf(NPCAction.WALK_RANDOM, NPCAction.OBSERVE), windowOnly.keys.toList())
        assertEquals(listOf(NPCAction.WALK_RANDOM, NPCAction.OBSERVE), choiceLimitOnly.keys.toList())
        assertTrue(probabilities.getValue(NPCAction.OBSERVE) > 0.4)
        assertEquals(1.0, probabilities.values.sum(), 1e-9)
    }

    @Test
    fun `configured random seed produces stable per NPC rolls`() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val first = DecisionEngine.deterministicDecisionRoll(12345L, uuid, 1L)
        val same = DecisionEngine.deterministicDecisionRoll(12345L, uuid, 1L)
        val nextDecision = DecisionEngine.deterministicDecisionRoll(12345L, uuid, 2L)
        val otherSeed = DecisionEngine.deterministicDecisionRoll(54321L, uuid, 1L)

        assertEquals(first, same)
        assertTrue(first >= 0.0 && first < 1.0)
        assertNotEquals(first, nextDecision)
        assertNotEquals(first, otherSeed)
    }

    @Test
    fun `diagnostic distinguishes choice limit from score window without changing selection`() {
        val scores = mapOf(
            NPCAction.START_WORK to 50,
            NPCAction.OBSERVE to 48,
            NPCAction.REST to 47,
            NPCAction.WALK_RANDOM to 34,
            NPCAction.DO_NOTHING to 0,
        )
        val settings = ActionProbabilitySettings(maxChoices = 2, scoreWindow = 15)
        val probabilities = ActionProbabilitySelector.probabilities(scores, settings = settings)
        val exclusions = ActionProbabilitySelector.excludedByRanking(scores, probabilities.keys, true, settings)

        assertEquals(listOf(NPCAction.START_WORK, NPCAction.OBSERVE), probabilities.keys.toList())
        assertEquals(ActionRankingExclusion.CHOICE_LIMIT, exclusions[NPCAction.REST])
        assertEquals(ActionRankingExclusion.OUTSIDE_SCORE_WINDOW, exclusions[NPCAction.WALK_RANDOM])
        assertFalse(NPCAction.DO_NOTHING in exclusions)
        assertTrue(exclusions.keys.none { it in probabilities })
    }

    @Test
    fun `diagnostic explains deterministic and nonpositive alternatives`() {
        val deterministicScores = mapOf(NPCAction.FLEE to 60, NPCAction.CALL_HELP to 55)
        val deterministic = ActionProbabilitySelector.excludedByRanking(
            deterministicScores, setOf(NPCAction.FLEE), false,
        )
        val nonpositive = ActionProbabilitySelector.excludedByRanking(
            mapOf(NPCAction.DO_NOTHING to 0, NPCAction.OBSERVE to 0), setOf(NPCAction.DO_NOTHING), true,
        )

        assertEquals(ActionRankingExclusion.DETERMINISTIC_SELECTION, deterministic[NPCAction.CALL_HELP])
        assertEquals(ActionRankingExclusion.NON_POSITIVE_SCORE, nonpositive[NPCAction.OBSERVE])
    }

    @Test
    fun `invalid configuration uses defaults and excessive values are bounded`() {
        val config = YamlConfiguration()
        config.set("simulation.decision.max_choices", 0)
        config.set("simulation.decision.score_window", -1)
        config.set("simulation.decision.temperature", 0.0)
        config.set("simulation.decision.repeat_weight", 0.0)
        config.set("simulation.decision.repeat_window_seconds", -1)
        config.set("simulation.decision.repeat_history_size", 0)
        config.set("simulation.decision.idle_weight", -1.0)
        config.set("simulation.decision.statistics_window", 0)

        assertEquals(ActionProbabilitySettings(), ActionProbabilitySettings.fromConfig(config))

        config.set("simulation.decision.max_choices", 100)
        config.set("simulation.decision.score_window", 1000)
        config.set("simulation.decision.temperature", Double.POSITIVE_INFINITY)
        config.set("simulation.decision.repeat_weight", Double.NaN)
        config.set("simulation.decision.repeat_window_seconds", 100_000)
        config.set("simulation.decision.repeat_history_size", 100_000)
        config.set("simulation.decision.idle_weight", 100.0)
        config.set("simulation.decision.statistics_window", 100_000)
        config.set("simulation.decision.action_weights.OBSERVE", 100.0)
        config.set("simulation.decision.action_weights.REST", 0.01)
        config.set("simulation.decision.action_weights.DO_NOTHING", 2.0)
        config.set("simulation.decision.action_weights.UNKNOWN_ACTION", 2.0)
        config.set("simulation.decision.action_cooldowns.GREET", 30)
        config.set("simulation.decision.action_cooldowns.SHARE_NEWS", 100_000)
        config.set("simulation.decision.action_cooldowns.ARGUE", 0)
        config.set("simulation.decision.action_cooldowns.UNKNOWN_ACTION", 30)
        assertEquals(
            ActionProbabilitySettings(
                8, 100, 8.0, 0.5, 3600, 1.0, 256,
                mapOf(
                    NPCAction.OBSERVE to 10.0,
                    NPCAction.REST to 0.1,
                    NPCAction.DO_NOTHING to 2.0,
                ),
                8,
                null,
                mapOf(NPCAction.GREET to 30, NPCAction.SHARE_NEWS to 3600),
            ),
            ActionProbabilitySettings.fromConfig(config),
        )

        config.set("simulation.decision.repeat_weight", 2.0)
        config.set("simulation.decision.repeat_window_seconds", 0)
        config.set("simulation.decision.repeat_history_size", 1)
        config.set("simulation.decision.idle_weight", 0.0)
        config.set("simulation.decision.statistics_window", 1)
        config.set("simulation.decision.action_cooldowns", null)
        assertEquals(
            ActionProbabilitySettings(
                8, 100, 8.0, 1.0, 0, 0.0, 1,
                mapOf(
                    NPCAction.OBSERVE to 10.0,
                    NPCAction.REST to 0.1,
                    NPCAction.DO_NOTHING to 2.0,
                ),
                1,
            ),
            ActionProbabilitySettings.fromConfig(config),
        )
    }

    @Test
    fun `optional random seed accepts numeric strings and ignores invalid values`() {
        val config = YamlConfiguration()
        config.set("simulation.decision.random_seed", "12345")

        assertEquals(12345L, ActionProbabilitySettings.fromConfig(config).randomSeed)

        config.set("simulation.decision.random_seed", "not-a-number")
        assertEquals(null, ActionProbabilitySettings.fromConfig(config).randomSeed)
    }

    @Test
    fun `occupation weights override global action weights case insensitively`() {
        val config = YamlConfiguration()
        config.set("simulation.decision.action_weights.OBSERVE", 2.0)
        config.set("simulation.decision.action_weights.REST", 3.0)
        config.set("simulation.decision.action_weights_by_occupation.Farmer.OBSERVE", 4.0)
        config.set("simulation.decision.action_weights_by_occupation.Farmer.FARM", 1.5)
        config.set("simulation.decision.action_weights_by_time.NIGHT.OBSERVE", 0.5)
        config.set("simulation.decision.action_weights_by_time.NIGHT.SLEEP", 2.0)
        config.set("simulation.decision.action_weights_by_weather.RAIN.OBSERVE", 1.25)
        config.set("simulation.decision.action_weights_by_weather.RAIN.HIDE", 1.75)

        val farmer = ActionProbabilitySettings.fromConfig(config, "farmer", "night", "rain")
        val miner = ActionProbabilitySettings.fromConfig(config, "miner")

        assertEquals(
            mapOf(
                NPCAction.OBSERVE to 1.25,
                NPCAction.REST to 3.0,
                NPCAction.FARM to 1.5,
                NPCAction.SLEEP to 2.0,
                NPCAction.HIDE to 1.75,
            ),
            farmer.actionWeights,
        )
        assertEquals(
            mapOf(NPCAction.OBSERVE to 2.0, NPCAction.REST to 3.0),
            miner.actionWeights,
        )
    }

    @Test
    fun `danger and conversational states keep deterministic selection`() {
        assertTrue(ActionProbabilitySelector.isEnabledFor(NPCState.IDLE, false, true))
        assertTrue(ActionProbabilitySelector.isEnabledFor(NPCState.WORKING, false, true))
        assertFalse(ActionProbabilitySelector.isEnabledFor(NPCState.IDLE, true, true))
        assertFalse(ActionProbabilitySelector.isEnabledFor(NPCState.COMBAT, false, true))
        assertFalse(ActionProbabilitySelector.isEnabledFor(NPCState.FLEEING, false, true))
        assertFalse(ActionProbabilitySelector.isEnabledFor(NPCState.TALKING, false, true))
        assertFalse(ActionProbabilitySelector.isEnabledFor(NPCState.IDLE, false, false))
    }

    @Test
    fun `danger makes escape and help eligible even from ordinary states`() {
        val ordinary = DecisionEngine.candidateActions(NPCState.IDLE, false)
        val threatened = DecisionEngine.candidateActions(NPCState.IDLE, true)
        val interruptedConversation = DecisionEngine.candidateActions(NPCState.TALKING, true)

        assertFalse(NPCAction.FLEE in ordinary)
        assertFalse(NPCAction.CALL_HELP in ordinary)
        assertTrue(NPCAction.FLEE in threatened)
        assertTrue(NPCAction.CALL_HELP in threatened)
        assertTrue(NPCAction.FLEE in interruptedConversation)
        assertEquals(1, DecisionEngine.candidateActions(NPCState.COMBAT, true).count { it == NPCAction.FLEE })
    }
}
