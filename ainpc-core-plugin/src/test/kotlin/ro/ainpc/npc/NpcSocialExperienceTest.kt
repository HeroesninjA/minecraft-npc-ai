package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class NpcSocialExperienceTest {
    private val helper = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val stranger = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val now = 100_000L
    private val hungry = NpcCognitiveState(hungerLevel = 20)

    @Test
    fun `confirmed aid biases only that helper and only the intended social actions`() {
        val experience = NpcSocialExperience()
        assertTrue(experience.record(receipt()))

        listOf(NPCAction.GREET, NPCAction.TALK, NPCAction.THANK).forEach { action ->
            assertEquals(5, experience.scoreModifier(helper, action, hungry, now), action.name)
            assertEquals(0, experience.scoreModifier(stranger, action, hungry, now), action.name)
        }
        listOf(NPCAction.CALL_HELP, NPCAction.ATTACK, NPCAction.EAT, NPCAction.SOCIALIZE).forEach { action ->
            assertEquals(0, experience.scoreModifier(helper, action, hungry, now), action.name)
        }
        assertEquals(receipt(), experience.latestFor(helper, now))
        assertNull(experience.latestFor(stranger, now))
    }

    @Test
    fun `dialogue claims and player recognition alone provide no food help evidence`() {
        val experience = NpcSocialExperience()
        val state = hungry.copy(hasInteractingPlayer = true, currentGoal = "Jucatorul spune ca mi-a dat mancare")

        assertEquals(0, experience.scoreModifier(helper, NPCAction.TALK, state, now))
        assertEquals("", experience.descriptionFor(helper, now))
        assertTrue(experience.snapshot().isEmpty())
    }

    @Test
    fun `same receipt cannot be credited twice while additional actual receipts have a bounded effect`() {
        val experience = NpcSocialExperience()
        val first = receipt()
        assertTrue(experience.record(first))
        assertFalse(experience.record(first))
        assertFalse(experience.record(first.copy(helperUuid = stranger)))
        assertEquals(1, experience.snapshot().size)

        (2L..8L).forEach { sequence ->
            assertTrue(experience.record(receipt(sequence)))
            assertEquals((4 + sequence.toInt()).coerceAtMost(8), experience.scoreModifier(helper, NPCAction.GREET, hungry, now))
        }
    }

    @Test
    fun `food help preference cannot override safety interaction restrictions or a satisfied need`() {
        val experience = NpcSocialExperience().apply { record(receipt()) }
        val restricted = listOf(
            hungry.copy(hungerLevel = 35),
            hungry.copy(hungerLevel = 100),
            hungry.copy(hungerLevel = -1),
            hungry.copy(isInDanger = true),
            hungry.copy(safetyLevel = 34),
            hungry.copy(currentState = NPCState.COMBAT),
            hungry.copy(currentState = NPCState.FLEEING),
            hungry.copy(currentState = NPCState.PANICKING),
            hungry.copy(currentState = NPCState.HIDING),
            hungry.copy(currentState = NPCState.SLEEPING),
        )

        restricted.forEach { state ->
            assertEquals(0, experience.scoreModifier(helper, NPCAction.TALK, state, now), state.toString())
        }
        assertEquals(5, experience.scoreModifier(helper, NPCAction.TALK, hungry.copy(currentState = NPCState.TALKING), now))
    }

    @Test
    fun `stale and future receipts remain stored without affecting current decisions or descriptions`() {
        val week = 7 * 24 * 60 * 60 * 1_000L
        val current = now + week
        val expired = receipt(1L).copy(recordedAt = now - 1L)
        val oldestValid = receipt(2L)
        val future = receipt(3L).copy(recordedAt = current + 1L)
        val experience = NpcSocialExperience().apply { restore(listOf(expired, oldestValid, future)) }
        val snapshot = experience.snapshot()

        assertEquals(5, experience.scoreModifier(helper, NPCAction.GREET, hungry, current))
        assertEquals(oldestValid, experience.latestFor(helper, current))
        assertTrue(experience.descriptionFor(helper, current).isNotBlank())
        assertEquals(snapshot, experience.snapshot())
        experience.restore(listOf(expired, future))
        assertEquals(0, experience.scoreModifier(helper, NPCAction.GREET, hungry, current))
        assertEquals("", experience.descriptionFor(helper, current))
        assertNull(experience.latestFor(helper, current))
        assertEquals(2, experience.snapshot().size)
    }

    @Test
    fun `descriptions address the proven helper without exposing stored identifiers or names`() {
        val experience = NpcSocialExperience().apply { record(receipt()) }
        val description = experience.descriptionFor(helper, now)

        assertEquals("Mi-ai oferit mancare cand imi era foame si m-ai ajutat sa ma simt mai satul.", description)
        assertFalse(description.contains(helper.toString()))
        assertFalse(description.contains("Helper"))
        assertEquals("", experience.descriptionFor(stranger, now))
    }

    @Test
    fun `receipt validation rejects unobserved gains unsupported food and malformed helper metadata`() {
        val valid = receipt()
        val invalid = listOf(
            valid.copy(helperName = ""),
            valid.copy(helperName = " "),
            valid.copy(helperName = "x".repeat(65)),
            valid.copy(helperName = "Helper\nName"),
            valid.copy(helperName = "Helper\u007fName"),
            valid.copy(itemKey = "minecraft:poisonous_potato"),
            valid.copy(itemKey = "bread"),
            valid.copy(amount = 0),
            valid.copy(amount = 2),
            valid.copy(beforeHunger = -1),
            valid.copy(beforeHunger = 35),
            valid.copy(afterHunger = valid.beforeHunger),
            valid.copy(afterHunger = valid.beforeHunger - 1),
            valid.copy(afterHunger = 101),
            valid.copy(recordedAt = 0L),
            valid.copy(recordedAt = -1L),
        )
        val experience = NpcSocialExperience()

        invalid.forEach { invalidReceipt -> assertFalse(experience.record(invalidReceipt), invalidReceipt.toString()) }
        experience.restore(invalid + valid)
        assertEquals(listOf(valid), experience.snapshot())
    }

    @Test
    fun `restore deduplicates newest receipt identities then retains the latest 32 detached records`() {
        val experience = NpcSocialExperience()
        val restored = (1L..40L).map { receipt(it).copy(recordedAt = now + it) }.reversed().toMutableList()
        val replacement = receipt(40L).copy(recordedAt = now + 50L, itemKey = "minecraft:apple", afterHunger = 32)
        restored.add(replacement)
        experience.restore(restored)
        val snapshot = experience.snapshot()
        restored.clear()

        assertEquals(32, snapshot.size)
        assertEquals((9L..40L).map { UUID(0L, it) }, snapshot.map { it.id })
        assertEquals(replacement, snapshot.last())
        assertTrue(experience.record(receipt(41L).copy(recordedAt = now + 51L)))
        assertEquals(32, experience.snapshot().size)
        assertEquals(UUID(0L, 9L), snapshot.first().id)
        assertEquals(UUID(0L, 10L), experience.snapshot().first().id)
    }

    @Test
    fun `restore treats later input as newer when receipt timestamps are equal`() {
        val experience = NpcSocialExperience()
        val receipts = (1L..40L).map { receipt(it) }
        experience.restore(receipts)

        assertEquals((9L..40L).map { UUID(0L, it) }, experience.snapshot().map { it.id })
        val replaced = receipt(40L).copy(itemKey = "minecraft:apple", afterHunger = 32)
        experience.restore(receipts + replaced)
        assertEquals(replaced, experience.snapshot().last())
    }

    private fun receipt(sequence: Long = 1L): NpcSocialExperience.Receipt = NpcSocialExperience.Receipt(
        UUID(0L, sequence), helper, "Helper", "minecraft:bread", 1, 20, 40, now,
    )
}
