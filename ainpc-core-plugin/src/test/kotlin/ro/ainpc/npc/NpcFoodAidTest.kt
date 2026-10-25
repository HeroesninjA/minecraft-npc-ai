package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class NpcFoodAidTest {
    private val helper = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val now = 100_000L

    @Test
    fun `supported food uses the explicit simulation balance and all other items are refused`() {
        assertEquals(20, NpcFoodAid.foodGain("minecraft:bread"))
        assertEquals(12, NpcFoodAid.foodGain("minecraft:apple"))
        assertEquals(20, NpcFoodAid.foodGain("minecraft:baked_potato"))
        assertEquals(30, NpcFoodAid.foodGain("minecraft:cooked_beef"))
        assertEquals(0, NpcFoodAid.foodGain("minecraft:poisonous_potato"))
        assertEquals(0, NpcFoodAid.foodGain("bread"))
    }

    @Test
    fun `one real food transfer changes both hunger fields and creates one addressed receipt`() {
        val npc = hungryNpc()
        npc.context.hungerLevel = 7
        var inventoryAmount = 3
        var consumes = 0
        var rollbacks = 0

        val result = offer(npc, consumeOne = { consumes++; inventoryAmount--; true }, rollback = { rollbacks++ })

        assertEquals(NpcFoodAid.Status.ACCEPTED, result.status)
        assertEquals(1, consumes)
        assertEquals(0, rollbacks)
        assertEquals(2, inventoryAmount)
        assertEquals(40, npc.hungerLevel)
        assertEquals(40, npc.context.hungerLevel)
        val receipt = result.receipt
        assertNotNull(receipt)
        assertEquals(helper, receipt?.helperUuid)
        assertEquals(1, receipt?.amount)
        assertEquals(20, receipt?.beforeHunger)
        assertEquals(40, receipt?.afterHunger)
        assertEquals(listOf(receipt), npc.socialExperience.snapshot())
        assertTrue(npc.socialExperience.descriptionFor(helper, now).startsWith("Mi-ai oferit"))
    }

    @Test
    fun `rejected items modes states needs and metadata never attempt inventory transfer`() {
        val cases = listOf(
            Triple(hungryNpc(), "minecraft:poisonous_potato", NpcFoodAid.Status.UNSUPPORTED),
            Triple(hungryNpc().apply { simulationMode = NpcSimulationMode.LIGHT }, "minecraft:bread", NpcFoodAid.Status.UNAVAILABLE),
            Triple(hungryNpc().apply { simulationMode = NpcSimulationMode.NONE }, "minecraft:bread", NpcFoodAid.Status.UNAVAILABLE),
            Triple(hungryNpc().apply { context.isInDanger = true }, "minecraft:bread", NpcFoodAid.Status.UNAVAILABLE),
            Triple(hungryNpc().apply { safetyLevel = 34 }, "minecraft:bread", NpcFoodAid.Status.UNAVAILABLE),
            Triple(hungryNpc().apply { changeStateFromSimulation(NPCState.COMBAT) }, "minecraft:bread", NpcFoodAid.Status.UNAVAILABLE),
            Triple(hungryNpc().apply { changeStateFromSimulation(NPCState.FLEEING) }, "minecraft:bread", NpcFoodAid.Status.UNAVAILABLE),
            Triple(hungryNpc().apply { changeStateFromSimulation(NPCState.PANICKING) }, "minecraft:bread", NpcFoodAid.Status.UNAVAILABLE),
            Triple(hungryNpc().apply { changeStateFromSimulation(NPCState.SLEEPING) }, "minecraft:bread", NpcFoodAid.Status.UNAVAILABLE),
            Triple(hungryNpc().apply { hungerLevel = -1 }, "minecraft:bread", NpcFoodAid.Status.UNAVAILABLE),
            Triple(hungryNpc().apply { hungerLevel = 35 }, "minecraft:bread", NpcFoodAid.Status.NOT_HUNGRY),
            Triple(hungryNpc().apply { hungerLevel = 100 }, "minecraft:bread", NpcFoodAid.Status.NOT_HUNGRY),
        )

        cases.forEach { (npc, itemKey, expectedStatus) ->
            val before = npc.hungerLevel
            val contextBefore = npc.context.hungerLevel
            val result = offer(npc, itemKey = itemKey, consumeOne = { error("rejected transfer") }, rollback = { error("rejected rollback") })
            assertEquals(expectedStatus, result.status)
            assertNull(result.receipt)
            assertEquals(before, npc.hungerLevel)
            assertEquals(contextBefore, npc.context.hungerLevel)
            assertTrue(npc.socialExperience.snapshot().isEmpty())
        }
        listOf("", " ", "x".repeat(65), "Helper\nName").forEach { helperName ->
            assertEquals(
                NpcFoodAid.Status.UNAVAILABLE,
                offer(hungryNpc(), helperName = helperName, consumeOne = { error("invalid helper") }).status,
            )
        }
        assertEquals(NpcFoodAid.Status.UNAVAILABLE, offer(hungryNpc(), timestamp = 0L, consumeOne = { error("invalid time") }).status)
    }

    @Test
    fun `canceled transfer changes no needs or memories and does not invoke rollback`() {
        val npc = hungryNpc()
        npc.context.hungerLevel = 7
        var consumes = 0
        var rollbacks = 0

        val result = offer(npc, consumeOne = { consumes++; false }, rollback = { rollbacks++ })

        assertEquals(NpcFoodAid.Status.TRANSFER_FAILED, result.status)
        assertEquals(1, consumes)
        assertEquals(0, rollbacks)
        assertEquals(20, npc.hungerLevel)
        assertEquals(7, npc.context.hungerLevel)
        assertNull(result.receipt)
        assertTrue(npc.socialExperience.snapshot().isEmpty())
    }

    @Test
    fun `partially failed inventory consumption is rolled back without changing needs or evidence`() {
        val npc = hungryNpc()
        npc.context.hungerLevel = 7
        var inventoryAmount = 3
        var rollbacks = 0

        val result = offer(
            npc,
            consumeOne = { inventoryAmount--; throw IllegalStateException("partial inventory failure") },
            rollback = { inventoryAmount = 3; rollbacks++ },
        )

        assertEquals(NpcFoodAid.Status.TRANSFER_FAILED, result.status)
        assertEquals(3, inventoryAmount)
        assertEquals(1, rollbacks)
        assertEquals(20, npc.hungerLevel)
        assertEquals(7, npc.context.hungerLevel)
        assertTrue(npc.socialExperience.snapshot().isEmpty())
    }

    @Test
    fun `rollback failure propagates after both hunger fields are restored`() {
        val npc = hungryNpc()
        npc.context.hungerLevel = 7
        val failure = IllegalArgumentException("rollback failed")

        val caught = assertThrows(IllegalArgumentException::class.java) {
            offer(
                npc,
                consumeOne = { throw IllegalStateException("partial inventory failure") },
                rollback = { throw failure },
            )
        }

        assertEquals(failure, caught)
        assertEquals(20, npc.hungerLevel)
        assertEquals(7, npc.context.hungerLevel)
        assertTrue(npc.socialExperience.snapshot().isEmpty())
    }

    @Test
    fun `external food aid cannot be credited to a pending autonomous eating action`() {
        val npc = hungryNpc().apply { changeStateFromSimulation(NPCState.EATING) }
        val before = NpcCognitiveState.capture(npc)
        npc.actionExperience.begin(NPCAction.EAT, before, "home", true, npc.stateRevision, now)

        assertEquals(NpcFoodAid.Status.ACCEPTED, offer(npc).status)

        assertNull(
            npc.actionExperience.observe(
                before, NpcCognitiveState.capture(npc), "home", true, npc.stateRevision, now, now + 30_000L,
            ),
        )
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(1, npc.socialExperience.snapshot().size)
    }

    @Test
    fun `separate actual units can help until hunger is satisfied without a dialogue cooldown`() {
        val npc = hungryNpc().apply { hungerLevel = 0 }
        var consumed = 0

        repeat(3) { sequence ->
            assertEquals(
                NpcFoodAid.Status.ACCEPTED,
                offer(npc, itemKey = "minecraft:apple", timestamp = now + sequence, consumeOne = { consumed++; true }).status,
            )
        }
        assertEquals(NpcFoodAid.Status.NOT_HUNGRY, offer(npc, consumeOne = { error("satisfied transfer") }).status)
        assertEquals(3, consumed)
        assertEquals(36, npc.hungerLevel)
        assertEquals(3, npc.socialExperience.snapshot().size)
    }

    @Test
    fun `replayed deterministic receipt cannot consume another inventory unit`() {
        val npc = hungryNpc()
        val first = offer(npc)
        npc.hungerLevel = 20
        npc.context.hungerLevel = 20

        val replay = offer(npc, consumeOne = { error("duplicate transfer") }, rollback = { error("duplicate rollback") })

        assertEquals(NpcFoodAid.Status.TRANSFER_FAILED, replay.status)
        assertEquals(listOf(first.receipt), npc.socialExperience.snapshot())
        assertEquals(20, npc.hungerLevel)
    }

    private fun hungryNpc(): AINPC = AINPC(null).apply {
        hungerLevel = 20
        context.hungerLevel = 20
    }

    private fun offer(
        npc: AINPC,
        helperName: String = "Helper",
        itemKey: String = "minecraft:bread",
        timestamp: Long = now,
        consumeOne: () -> Boolean = { true },
        rollback: () -> Unit = {},
    ): NpcFoodAid.Result = NpcFoodAid.offer(npc, helper, helperName, itemKey, timestamp, consumeOne, rollback)
}
