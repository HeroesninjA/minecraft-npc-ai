package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcActionExperienceTest {
    private val tickAt = 100_000L
    private val observedAt = tickAt + 30_000L
    private val contextKey = "home:world:0:64:0"
    private val eating = NpcCognitiveState(hungerLevel = 20, currentState = NPCState.EATING)
    private val sleeping = NpcCognitiveState(energyLevel = 20, currentState = NPCState.SLEEPING)

    @Test
    fun `an applied eating period records its satiety result and biases later eligible choices`() {
        val experience = begun()

        val result = observe(experience)

        assertEquals(NpcActionExperience.Observation(NPCAction.EAT, contextKey, 8, 8.0, observedAt), result)
        assertEquals(listOf(result), experience.snapshot())
        assertEquals(3, score(experience))
        assertEquals(
            "In timpul mesei de acasa, m-am simtit mai satul.",
            experience.latestDescription(observedAt),
        )
        assertNull(observe(experience))
    }

    @Test
    fun `sleep subtracts ambient home recovery from bias but reports the raw energy change`() {
        val experience = begun(NPCAction.SLEEP, sleeping)

        val result = observe(experience, before = sleeping, after = sleeping.copy(energyLevel = 24))

        assertEquals(NpcActionExperience.Observation(NPCAction.SLEEP, contextKey, 4, 2.0, observedAt), result)
        assertEquals(1, score(experience, action = NPCAction.SLEEP, state = sleeping))
        assertEquals(
            "In timpul somnului de acasa, m-am simtit mai odihnit.",
            experience.latestDescription(observedAt),
        )
    }

    @Test
    fun `elapsed normalization follows the same clamped tick factor as needs updates`() {
        val shortEating = begun()
        val longEating = begun()
        val shortSleeping = begun(NPCAction.SLEEP, sleeping)

        assertEquals(
            8.0,
            observe(shortEating, after = eating.copy(hungerLevel = 24), now = tickAt + 1_000L)?.effect,
        )
        assertEquals(
            8.0,
            observe(longEating, after = eating.copy(hungerLevel = 52), now = tickAt + 120_000L)?.effect,
        )
        assertEquals(
            2.0,
            observe(shortSleeping, before = sleeping, after = sleeping.copy(energyLevel = 22), now = tickAt + 15_000L)?.effect,
        )
    }

    @Test
    fun `rest and selections which have not entered the expected care state create no experience`() {
        val resting = NpcCognitiveState(energyLevel = 20, currentState = NPCState.RESTING)
        val experience = begun(NPCAction.REST, resting)

        assertNull(observe(experience, before = resting, after = resting.copy(energyLevel = 22)))
        assertEquals(0, score(experience, action = NPCAction.REST, state = resting))
        assertNull(observe(begun(state = eating.copy(currentState = NPCState.WALKING))))
        assertNull(observe(NpcActionExperience()))
    }

    @Test
    fun `begin requires safe home care with an unmet target need and valid provenance`() {
        val invalidBeginnings = listOf(
            begun(isAtHome = false),
            begun(state = eating.copy(isInDanger = true)),
            begun(state = eating.copy(safetyLevel = 34)),
            begun(state = eating.copy(hasInteractingPlayer = true)),
            begun(state = eating.copy(hungerLevel = 60)),
            begun(state = eating.copy(hungerLevel = 100)),
            begun(state = eating.copy(hungerLevel = -1)),
            begun(key = " "),
            begun(key = "x".repeat(257)),
            begun(revision = -1L),
            begun(timestamp = 0L),
        )

        invalidBeginnings.forEach { experience -> assertNull(observe(experience)) }
    }

    @Test
    fun `observation rejects interrupted changed or unsafe circumstances and consumes the pending period`() {
        val invalidObservations: List<(NpcActionExperience) -> NpcActionExperience.Observation?> = listOf(
            { observe(it, before = eating.copy(currentState = NPCState.IDLE)) },
            { observe(it, after = eating.copy(currentState = NPCState.SLEEPING)) },
            { observe(it, revision = 3L) },
            { observe(it, key = "another-home") },
            { observe(it, isAtHome = false) },
            { observe(it, before = eating.copy(isInDanger = true)) },
            { observe(it, after = eating.copy(isInDanger = true)) },
            { observe(it, before = eating.copy(safetyLevel = 34)) },
            { observe(it, after = eating.copy(hasInteractingPlayer = true)) },
            { observe(it, before = eating.copy(hungerLevel = 21)) },
            { observe(it, after = eating.copy(hungerLevel = 101)) },
            { observe(it, previous = tickAt - 1L) },
            { observe(it, now = tickAt) },
            { observe(it, now = tickAt - 1L) },
            { observe(it, now = tickAt + 120_001L) },
        )

        invalidObservations.forEachIndexed { index, invalidObservation ->
            val experience = begun()
            assertNull(invalidObservation(experience), "guard $index")
            assertNull(observe(experience), "consumed guard $index")
            assertTrue(experience.snapshot().isEmpty(), "stored guard $index")
        }
    }

    @Test
    fun `unrelated need changes do not change the observed eating result`() {
        val experience = begun()
        val after = eating.copy(
            hungerLevel = 28, energyLevel = 90, comfortLevel = 10, socialNeedLevel = 80,
        )

        assertEquals(8, observe(experience, after = after)?.needDelta)
        assertEquals(8.0, experience.snapshot().single().effect)
    }

    @Test
    fun `ambient sleep improvement alone creates no positive preference`() {
        val experience = begun(NPCAction.SLEEP, sleeping)

        val result = observe(experience, before = sleeping, after = sleeping.copy(energyLevel = 22))

        assertEquals(2, result?.needDelta)
        assertEquals(0.0, result?.effect)
        assertEquals(0, score(experience, action = NPCAction.SLEEP, state = sleeping))
    }

    @Test
    fun `bias requires a current eligible need in the same safe home context`() {
        val experience = begun().also { observe(it) }

        assertEquals(0, score(experience, state = eating.copy(hungerLevel = 35, currentState = NPCState.IDLE)))
        assertEquals(3, score(experience, state = eating.copy(hungerLevel = 59)))
        assertEquals(0, score(experience, state = eating.copy(hungerLevel = 60)))
        assertEquals(0, score(experience, state = eating.copy(hungerLevel = 100)))
        assertEquals(0, score(experience, state = eating.copy(hungerLevel = -1)))
        assertEquals(0, score(experience, isAtHome = false))
        assertEquals(0, score(experience, key = "another-home"))
        assertEquals(0, score(experience, state = eating.copy(isInDanger = true)))
        assertEquals(0, score(experience, state = eating.copy(safetyLevel = 34)))
        assertEquals(0, score(experience, state = eating.copy(hasInteractingPlayer = true)))
        listOf(
            NPCState.TALKING, NPCState.LISTENING, NPCState.TRADING,
            NPCState.COMBAT, NPCState.FLEEING, NPCState.PANICKING,
        ).forEach { currentState ->
            assertEquals(0, score(experience, state = eating.copy(currentState = currentState)), currentState.name)
        }
        assertEquals(0, score(experience, action = NPCAction.SLEEP, state = sleeping))
    }

    @Test
    fun `stale and future samples cannot score or describe and reads leave stored evidence intact`() {
        val experience = NpcActionExperience()
        val week = 7 * 24 * 60 * 60 * 1_000L
        val current = week + observedAt + 10L
        val oldestValid = observation(timestamp = current - week)
        val expired = observation(timestamp = current - week - 1L, effect = -100.0)
        val future = observation(timestamp = current + 1L, effect = -100.0)
        experience.restore(listOf(expired, oldestValid, future))
        val stored = experience.snapshot()

        assertEquals(3, score(experience, now = current))
        assertEquals(
            "In timpul mesei de acasa, m-am simtit mai satul.",
            experience.latestDescription(current),
        )
        assertEquals(stored, experience.snapshot())
        experience.restore(listOf(expired, future))
        assertEquals(0, score(experience, now = current))
        assertEquals("", experience.latestDescription(current))
        assertEquals(2, experience.snapshot().size)
    }

    @Test
    fun `bias is bounded for both favorable and adverse evidence`() {
        val experience = NpcActionExperience()
        experience.restore(listOf(observation(effect = 200.0)))
        assertEquals(12, score(experience))
        experience.restore(listOf(observation(delta = -20, effect = -200.0)))
        assertEquals(-12, score(experience))
        assertTrue(experience.latestDescription(observedAt).contains("foamea mea a crescut"))
        experience.restore(listOf(observation(delta = 0, effect = 0.0)))
        assertTrue(experience.latestDescription(observedAt).contains("nu am simtit ca foamea s-a redus"))
    }

    @Test
    fun `restore rejects unsupported malformed and unbounded records and cancels pending observations`() {
        val valid = observation()
        val invalid = listOf(
            valid.copy(action = NPCAction.REST),
            valid.copy(action = NPCAction.START_WORK),
            valid.copy(contextKey = ""),
            valid.copy(contextKey = " "),
            valid.copy(contextKey = "x".repeat(257)),
            valid.copy(needDelta = 101),
            valid.copy(needDelta = -101),
            valid.copy(effect = Double.NaN),
            valid.copy(effect = Double.POSITIVE_INFINITY),
            valid.copy(effect = Double.NEGATIVE_INFINITY),
            valid.copy(effect = 200.1),
            valid.copy(effect = -200.1),
            valid.copy(observedAt = 0L),
            valid.copy(observedAt = -1L),
        )
        val experience = begun()

        experience.restore(invalid + valid)

        assertEquals(listOf(valid), experience.snapshot())
        assertNull(observe(experience))
    }

    @Test
    fun `restore and live observations retain the newest 32 records in chronological order`() {
        val experience = NpcActionExperience()
        val restored = (1L..40L).map { observation(timestamp = it) }.reversed().toMutableList()
        experience.restore(restored)
        val beforeNewObservation = experience.snapshot()
        restored.clear()

        assertEquals((9L..40L).toList(), beforeNewObservation.map { it.observedAt })
        experience.begin(NPCAction.EAT, eating, contextKey, true, 2L, tickAt)
        assertNotNull(observe(experience))
        assertEquals(32, experience.snapshot().size)
        assertEquals((10L..40L).toList() + observedAt, experience.snapshot().map { it.observedAt })
        assertEquals((9L..40L).toList(), beforeNewObservation.map { it.observedAt })
    }

    @Test
    fun `cancel and replacement stop attribution without erasing observed evidence`() {
        val experience = begun().also { observe(it) }
        val stored = experience.snapshot()
        experience.begin(NPCAction.EAT, eating, contextKey, true, 2L, tickAt)
        experience.cancelPending()
        assertNull(observe(experience))
        assertEquals(stored, experience.snapshot())
        experience.begin(NPCAction.EAT, eating, contextKey, true, 2L, tickAt)
        experience.begin(NPCAction.REST, sleeping, contextKey, true, 2L, tickAt)
        assertNull(observe(experience))
        assertEquals(stored, experience.snapshot())
    }

    private fun begun(
        action: NPCAction = NPCAction.EAT,
        state: NpcCognitiveState = eating,
        key: String = contextKey,
        isAtHome: Boolean = true,
        revision: Long = 2L,
        timestamp: Long = tickAt,
    ): NpcActionExperience = NpcActionExperience().apply {
        begin(action, state, key, isAtHome, revision, timestamp)
    }

    private fun observe(
        experience: NpcActionExperience,
        before: NpcCognitiveState = eating,
        after: NpcCognitiveState = eating.copy(hungerLevel = 28),
        key: String = contextKey,
        isAtHome: Boolean = true,
        revision: Long = 2L,
        previous: Long = tickAt,
        now: Long = observedAt,
    ): NpcActionExperience.Observation? = experience.observe(
        before, after, key, isAtHome, revision, previous, now,
    )

    private fun score(
        experience: NpcActionExperience,
        action: NPCAction = NPCAction.EAT,
        state: NpcCognitiveState = eating,
        key: String = contextKey,
        isAtHome: Boolean = true,
        now: Long = observedAt,
    ): Int = experience.scoreModifier(action, state, key, isAtHome, now)

    private fun observation(
        delta: Int = 8,
        effect: Double = 8.0,
        timestamp: Long = observedAt,
    ): NpcActionExperience.Observation = NpcActionExperience.Observation(
        NPCAction.EAT, contextKey, delta, effect, timestamp,
    )
}
