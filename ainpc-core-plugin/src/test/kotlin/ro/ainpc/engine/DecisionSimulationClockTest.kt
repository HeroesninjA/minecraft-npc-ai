package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcActionExperience
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcSelfCareGoal
import java.lang.reflect.InvocationTargetException

class DecisionSimulationClockTest {
    private val plugin = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        (field.get(null) as sun.misc.Unsafe).allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    }.also { instance ->
        JavaPlugin::class.java.getDeclaredField("newConfig").apply {
            isAccessible = true
            set(instance, YamlConfiguration())
        }
    }
    private var now = 1_700_000_100_000L
    private var clockReads = 0
    private val engine = DecisionEngine(plugin, { _, _, _ -> error("Unexpected world scan") }, { clockReads++; now })

    @Test
    fun `tracked progress establishes one baseline then advances and ignores a repeated timestamp`() {
        plugin.config.set("simulation.progress.enabled", true)
        val npc = npc(now - 90_000L)
        val before = needs(npc)

        update(npc)

        assertEquals(before, needs(npc))
        assertEquals(now, npc.lastSimulationTickAt)
        val baseline = npc.simulationProgress.snapshot()!!
        assertEquals(1L, baseline.sequence)
        now += 30_000L
        update(npc)
        assertEquals(listOf(39, 42, 46, 48, 54), needs(npc))
        assertEquals(2L, npc.simulationProgress.snapshot()!!.sequence)
        val applied = needs(npc)
        val checkpoint = npc.simulationProgress.snapshot()
        update(npc)
        assertEquals(applied, needs(npc))
        assertEquals(checkpoint, npc.simulationProgress.snapshot())
    }

    @Test
    fun `tracked failure consumes neither the lease checkpoint nor needs time`() {
        plugin.config.set("simulation.progress.enabled", true)
        val npc = npc(0L)
        update(npc)
        val before = needs(npc)
        val checkpoint = npc.simulationProgress.snapshot()
        val timestamp = npc.lastSimulationTickAt
        now += 30_000L
        plugin.config.set("simulation.needs.social_decay", Double.NaN)

        assertThrows(InvocationTargetException::class.java) { update(npc) }

        assertEquals(before, needs(npc))
        assertEquals(timestamp, npc.lastSimulationTickAt)
        assertEquals(checkpoint, npc.simulationProgress.snapshot())
        plugin.config.set("simulation.needs.social_decay", 0.8)
        update(npc)
        assertEquals(2L, npc.simulationProgress.snapshot()!!.sequence)
        assertEquals(now, npc.lastSimulationTickAt)
    }

    @Test
    fun `tracked lifecycle resume does not convert the detached gap into physical recovery or experience`() {
        plugin.config.set("simulation.progress.enabled", true)
        val npc = npc(0L, NPCState.EATING)
        update(npc)
        armCare(npc, NPCAction.EAT)
        npc.markEntityUnavailable()
        val before = needs(npc)
        now += 900_000L

        update(npc, withExperience = true)

        assertEquals(before, needs(npc))
        assertEquals(now, npc.lastSimulationTickAt)
        assertEquals(2L, npc.simulationProgress.snapshot()!!.sequence)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertNull(npc.selfCareGoal.snapshot(now))
        now += 30_000L
        update(npc)
        assertEquals(49, npc.hungerLevel)
    }

    @Test
    fun `reenabling tracking after legacy updates creates a baseline without erasing its history`() {
        plugin.config.set("simulation.progress.enabled", true)
        val npc = npc(0L)
        update(npc)
        val checkpoint = npc.simulationProgress.snapshot()
        plugin.config.set("simulation.progress.enabled", false)
        now += 30_000L
        update(npc)
        val afterLegacy = needs(npc)
        assertEquals(checkpoint, npc.simulationProgress.snapshot())
        plugin.config.set("simulation.progress.enabled", true)
        now += 30_000L

        update(npc)

        assertEquals(afterLegacy, needs(npc))
        assertEquals(2L, npc.simulationProgress.snapshot()!!.sequence)
        assertEquals(now, npc.lastSimulationTickAt)
    }

    @Test
    fun `needs and observers share one clock sample for each update`() {
        for (withExperience in listOf(false, true)) {
            val npc = npc(now - 30_000L)
            val beforeReads = clockReads
            update(npc, withExperience)
            assertEquals(beforeReads + 1, clockReads)
            assertEquals(now, npc.lastSimulationTickAt)
        }
    }

    @Test
    fun `capped needs recovery does not turn a long real gap into an eligible observation`() {
        val npc = npc(now - 150_000L, NPCState.EATING)
        armCare(npc, NPCAction.EAT)

        update(npc, withExperience = true)

        assertEquals(73, npc.hungerLevel)
        assertEquals(now, npc.lastSimulationTickAt)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertNull(npc.selfCareGoal.snapshot(now))
    }

    @Test
    fun `a failed needs calculation consumes neither time nor partial recovery and a retry observes once`() {
        val npc = npc(now - 30_000L, NPCState.EATING)
        armCare(npc, NPCAction.EAT)
        val before = needs(npc)
        val previousTick = npc.lastSimulationTickAt
        val goal = npc.selfCareGoal.snapshot(previousTick)
        plugin.config.set("simulation.needs.social_decay", Double.NaN)

        val failure = assertThrows(InvocationTargetException::class.java) { update(npc, withExperience = true) }

        assertTrue(failure.cause is IllegalArgumentException)
        assertEquals(before, needs(npc))
        assertEquals(previousTick, npc.lastSimulationTickAt)
        assertEquals(goal, npc.selfCareGoal.snapshot(previousTick))
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        plugin.config.set("simulation.needs.social_decay", 0.8)
        update(npc, withExperience = true)
        assertEquals(now, npc.lastSimulationTickAt)
        assertEquals(49, npc.hungerLevel)
        assertEquals(1, npc.actionExperience.snapshot().size)
        assertTrue(npc.selfCareGoal.snapshot(now)!!.observedRecovery)
    }

    @Test
    fun `uninitialized clocks establish a baseline without updating needs`() {
        for (previousTick in listOf(0L, -1L, Long.MIN_VALUE)) {
            val npc = npc(previousTick)
            val before = needs(npc)

            update(npc)

            assertEquals(before, needs(npc))
            assertEquals(now, npc.lastSimulationTickAt)
        }
    }

    @Test
    fun `backward time rebases without decay or recovery even for a representable far future tick`() {
        for (state in listOf(NPCState.IDLE, NPCState.EATING, NPCState.SLEEPING)) {
            for (previousTick in listOf(now + 1L, now + 30_000L, Long.MAX_VALUE)) {
                val npc = npc(previousTick, state)
                val before = needs(npc)

                update(npc)

                assertEquals(before, needs(npc), "$state previous=$previousTick")
                assertEquals(now, npc.lastSimulationTickAt)
            }
        }
    }

    @Test
    fun `repeated updates in the same millisecond do not invent elapsed needs`() {
        for (state in listOf(NPCState.IDLE, NPCState.EATING, NPCState.SLEEPING)) {
            val npc = npc(now, state)
            val before = needs(npc)

            repeat(3) { update(npc) }

            assertEquals(before, needs(npc), state.name)
            assertEquals(now, npc.lastSimulationTickAt)
        }
    }

    @Test
    fun `positive elapsed intervals retain existing minimum normal and maximum tick factors`() {
        val cases = listOf(
            1L to listOf(40, 42, 47, 49, 54),
            30_000L to listOf(39, 42, 46, 48, 54),
            120_000L to listOf(35, 38, 44, 47, 57),
            900_000L to listOf(35, 38, 44, 47, 57),
        )
        for ((elapsed, expected) in cases) {
            val npc = npc(now - elapsed)

            update(npc)

            assertEquals(expected, needs(npc), "elapsed=$elapsed")
            assertEquals(now, npc.lastSimulationTickAt)
        }
    }

    @Test
    fun `backward and equal time cannot create recovery credit and do not erase learned history`() {
        for (action in listOf(NPCAction.EAT, NPCAction.SLEEP)) {
            for (offset in listOf(0L, 30_000L)) {
                val state = if (action == NPCAction.EAT) NPCState.EATING else NPCState.SLEEPING
                val npc = npc(now + offset, state)
                val historical = NpcActionExperience.Observation(
                    action, NpcSelfCareGoal.homeKey(npc), 4, 4.0, now - 30_000L,
                )
                npc.actionExperience.restore(listOf(historical))
                armCare(npc, action)
                val before = needs(npc)

                update(npc, withExperience = true)

                assertEquals(before, needs(npc), "$action offset=$offset")
                assertEquals(listOf(historical), npc.actionExperience.snapshot())
                assertNull(npc.selfCareGoal.snapshot(now))

                now += 30_000L
                update(npc, withExperience = true)
                assertEquals(listOf(historical), npc.actionExperience.snapshot())
                assertNull(npc.selfCareGoal.snapshot(now))

                armCare(npc, action)
                now += 30_000L
                update(npc, withExperience = true)
                assertEquals(2, npc.actionExperience.snapshot().size)
                assertEquals(historical, npc.actionExperience.snapshot().first())
                assertTrue(npc.actionExperience.snapshot().last().needDelta > 0)
                assertTrue(requireNotNull(npc.selfCareGoal.snapshot(now)).observedRecovery)
            }
        }
    }

    @Test
    fun `a rebased clock resumes once on a positive interval and ignores duplicate observations`() {
        val npc = npc(Long.MAX_VALUE)
        val before = needs(npc)
        update(npc)
        assertEquals(before, needs(npc))

        now += 30_000L
        update(npc)
        val after = needs(npc)
        assertEquals(listOf(39, 42, 46, 48, 54), after)

        repeat(3) { update(npc) }
        assertEquals(after, needs(npc))
    }

    private fun npc(previousTick: Long, state: NPCState = NPCState.IDLE): AINPC = AINPC(null).apply {
        hungerLevel = 41
        energyLevel = 43
        socialNeedLevel = 47
        comfortLevel = 49
        safetyLevel = 53
        lastSimulationTickAt = previousTick
        restorePersistedState(state)
        worldName = "world"
        homeAnchor = AINPC.OwnedLocation("home", "Casa", "world", 0.0, 64.0, 0.0)
        context.isAtHome = state != NPCState.IDLE
    }

    private fun armCare(npc: AINPC, action: NPCAction) {
        val state = NpcCognitiveState.capture(npc, npc.context, npc.lastSimulationTickAt)
        val homeKey = NpcSelfCareGoal.homeKey(npc)
        npc.actionExperience.begin(action, state, homeKey, true, npc.stateRevision, npc.lastSimulationTickAt)
        npc.selfCareGoal.applied(action, state, homeKey, true, npc.lastSimulationTickAt)
    }

    private fun update(npc: AINPC, withExperience: Boolean = false) {
        val method = if (withExperience) "updateNeedsWithExperience" else "updateNeeds"
        DecisionEngine::class.java.getDeclaredMethod(method, AINPC::class.java, NPCContext::class.java)
            .apply { isAccessible = true }.invoke(engine, npc, npc.context)
    }

    private fun needs(npc: AINPC): List<Int> = listOf(
        npc.hungerLevel, npc.energyLevel, npc.socialNeedLevel, npc.comfortLevel, npc.safetyLevel,
    )
}
