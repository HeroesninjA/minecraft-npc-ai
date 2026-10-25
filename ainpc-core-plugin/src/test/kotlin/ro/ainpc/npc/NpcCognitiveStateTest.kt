package ro.ainpc.npc

import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class NpcCognitiveStateTest {
    @Test
    fun `danger and emergency states override conversations needs and stale goals`() {
        val state = NpcCognitiveState(
            hungerLevel = 0,
            energyLevel = 0,
            hasInteractingPlayer = true,
            currentState = NPCState.TALKING,
            currentGoal = "sa ajung la atelier",
        )

        val emergencyStates = listOf(NPCState.COMBAT, NPCState.FLEEING, NPCState.PANICKING)
            .map { state.copy(currentState = it) } + state.copy(isInDanger = true)

        emergencyStates.forEach { emergency ->
            assertEquals(NpcCognitiveState.Focus.SAFETY, emergency.focus)
            assertTrue(emergency.needsDescription().contains("siguranta"))
            assertTrue(emergency.intentionDescription().contains("loc sigur"))
            assertFalse(emergency.intentionDescription().contains("atelier"))
        }
    }

    @Test
    fun `conversation focus requires a stored interlocutor and an active conversation state`() {
        val state = NpcCognitiveState(hungerLevel = 10)

        listOf(NPCState.TALKING, NPCState.LISTENING).forEach { conversationState ->
            assertEquals(NpcCognitiveState.Focus.HUNGER, state.copy(currentState = conversationState).focus)
            val conversation = state.copy(currentState = conversationState, hasInteractingPlayer = true)
            assertEquals(NpcCognitiveState.Focus.CONVERSATION, conversation.focus)
            assertEquals("Vreau sa urmaresc discutia curenta.", conversation.intentionDescription())
        }
        assertEquals(NpcCognitiveState.Focus.HUNGER, state.copy(hasInteractingPlayer = true).focus)
    }

    @Test
    fun `competing hunger and fatigue select the greater relative deficit with a stable hunger tie`() {
        assertEquals(NpcCognitiveState.Focus.HUNGER, NpcCognitiveState(hungerLevel = 5, energyLevel = 20).focus)
        assertEquals(NpcCognitiveState.Focus.ENERGY, NpcCognitiveState(hungerLevel = 20, energyLevel = 5).focus)
        assertEquals(NpcCognitiveState.Focus.HUNGER, NpcCognitiveState(hungerLevel = 10, energyLevel = 10).focus)
        assertEquals(
            NpcCognitiveState.Focus.ENERGY,
            NpcCognitiveState(hungerLevel = 30, energyLevel = 45, currentState = NPCState.SLEEPING).focus,
        )
        assertEquals(
            NpcCognitiveState.Focus.HUNGER,
            NpcCognitiveState(hungerLevel = 45, energyLevel = 30, currentState = NPCState.EATING).focus,
        )
    }

    @Test
    fun `self care starts below 35 and continues below 60 only for its matching active state`() {
        assertEquals(NpcCognitiveState.Focus.IDLE, NpcCognitiveState(hungerLevel = 35, energyLevel = 35).focus)
        assertEquals(NpcCognitiveState.Focus.HUNGER, NpcCognitiveState(hungerLevel = 34).focus)
        assertEquals(NpcCognitiveState.Focus.ENERGY, NpcCognitiveState(energyLevel = 34).focus)
        assertEquals(
            NpcCognitiveState.Focus.HUNGER,
            NpcCognitiveState(hungerLevel = 59, currentState = NPCState.EATING).focus,
        )
        assertEquals(
            NpcCognitiveState.Focus.ENERGY,
            NpcCognitiveState(energyLevel = 59, currentState = NPCState.SLEEPING).focus,
        )
        assertEquals(NpcCognitiveState.Focus.IDLE, NpcCognitiveState(hungerLevel = 60, currentState = NPCState.EATING).focus)
        assertEquals(NpcCognitiveState.Focus.IDLE, NpcCognitiveState(energyLevel = 60, currentState = NPCState.SLEEPING).focus)
        assertEquals(NpcCognitiveState.Focus.IDLE, NpcCognitiveState(hungerLevel = 59, energyLevel = 59).focus)
    }

    @Test
    fun `secondary needs select the greatest deficit before plans`() {
        val state = NpcCognitiveState(currentGoal = "sa merg la atelier", plannedRoutineActivity = "sa lucrez")

        assertEquals(NpcCognitiveState.Focus.SAFETY, state.copy(safetyLevel = 10, comfortLevel = 20, socialNeedLevel = 30).focus)
        assertEquals(NpcCognitiveState.Focus.COMFORT, state.copy(safetyLevel = 30, comfortLevel = 10, socialNeedLevel = 20).focus)
        assertEquals(NpcCognitiveState.Focus.SOCIAL, state.copy(safetyLevel = 30, comfortLevel = 20, socialNeedLevel = 10).focus)
        assertEquals(NpcCognitiveState.Focus.SAFETY, state.copy(safetyLevel = 20, comfortLevel = 20, socialNeedLevel = 20).focus)
        assertEquals(NpcCognitiveState.Focus.GOAL, state.focus)
        assertEquals(NpcCognitiveState.Focus.ROUTINE, state.copy(currentGoal = "").focus)
        assertEquals(NpcCognitiveState.Focus.IDLE, state.copy(currentGoal = "", plannedRoutineActivity = "").focus)
    }

    @Test
    fun `need descriptions report every unmet need and retain recovery needs`() {
        val state = NpcCognitiveState(
            hungerLevel = 45,
            energyLevel = 20,
            socialNeedLevel = 30,
            comfortLevel = 30,
            safetyLevel = 30,
            currentState = NPCState.EATING,
        )

        assertEquals(
            "Am nevoie de siguranta. Mi-e foame si am nevoie de mancare. Am nevoie de odihna. " +
                "Am nevoie de confort. Am nevoie de companie.",
            state.needsDescription(),
        )
        assertEquals("Nevoile mele de baza sunt in echilibru acum.", NpcCognitiveState().needsDescription())
        assertEquals(
            "Am nevoie de odihna.",
            NpcCognitiveState(energyLevel = 59, currentState = NPCState.SLEEPING).needsDescription(),
        )
    }

    @Test
    fun `thoughts and intentions describe desires without claiming successful work or consumption`() {
        val hungry = NpcCognitiveState(hungerLevel = 10, currentState = NPCState.EATING)
        val tired = NpcCognitiveState(energyLevel = 10, currentState = NPCState.SLEEPING)
        val planned = NpcCognitiveState(plannedRoutineActivity = "sa lucrez la atelier")

        assertEquals("Mi-e foame si ma gandesc sa gasesc ceva de mancare.", hungry.thoughtDescription())
        assertEquals("Vreau sa gasesc ceva de mancare.", hungry.intentionDescription())
        assertEquals("Vreau sa gasesc un loc de odihna.", tired.intentionDescription())
        assertEquals("Ma gandesc la ce am in program: sa lucrez la atelier.", planned.thoughtDescription())
        assertEquals("Vreau sa-mi urmez programul: sa lucrez la atelier.", planned.intentionDescription())
        assertEquals(
            "Obiectivul meu imediat este: sa ajung la atelier.",
            hungry.copy(currentGoal = " sa ajung la atelier. ").intentionDescription(),
        )
    }

    @Test
    fun `combined deficits preserve the existing decision need scores`() {
        val state = NpcCognitiveState(
            hungerLevel = 10, energyLevel = 10, socialNeedLevel = 10, comfortLevel = 10, safetyLevel = 10,
        )
        val expectedScores = mapOf(
            NPCAction.EAT to 45,
            NPCAction.SLEEP to 72,
            NPCAction.REST to 57,
            NPCAction.INVESTIGATE to -10,
            NPCAction.SOCIALIZE to 28,
            NPCAction.GREET to 28,
            NPCAction.TALK to 28,
            NPCAction.FLEE to 30,
            NPCAction.HIDE to 44,
            NPCAction.CALL_HELP to 30,
            NPCAction.ATTACK to -10,
            NPCAction.THREATEN to -10,
            NPCAction.INSULT to -10,
            NPCAction.ARGUE to -10,
            NPCAction.LISTEN to 0,
            NPCAction.DRINK to 0,
        )

        expectedScores.forEach { (action, expectedScore) ->
            assertEquals(expectedScore, state.needModifier(action), action.name)
        }
        NPCAction.entries.filter { it.getCategory() == NPCAction.ActionCategory.WORK }.forEach { action ->
            assertEquals(-30, state.needModifier(action), action.name)
        }
    }

    @Test
    fun `hunger and fatigue retain their distinct work penalties`() {
        val hungry = NpcCognitiveState(hungerLevel = 34)
        val tired = NpcCognitiveState(energyLevel = 34)

        assertEquals(-12, hungry.needModifier(NPCAction.START_WORK))
        assertEquals(-18, tired.needModifier(NPCAction.START_WORK))
        assertEquals(45, hungry.needModifier(NPCAction.EAT))
        assertEquals(0, hungry.needModifier(NPCAction.SLEEP))
        assertEquals(50, tired.needModifier(NPCAction.SLEEP))
        assertEquals(35, tired.needModifier(NPCAction.REST))
        assertEquals(0, tired.needModifier(NPCAction.EAT))
    }

    @Test
    fun `decision modifiers keep start thresholds even while cognition follows recovery hysteresis`() {
        val boundary = NpcCognitiveState(
            hungerLevel = 35, energyLevel = 35, socialNeedLevel = 40, comfortLevel = 40, safetyLevel = 40,
        )

        NPCAction.entries.forEach { action -> assertEquals(0, boundary.needModifier(action), action.name) }
        val recovering = NpcCognitiveState(hungerLevel = 45, currentState = NPCState.EATING)
        assertEquals(NpcCognitiveState.Focus.HUNGER, recovering.focus)
        assertEquals(0, recovering.needModifier(NPCAction.EAT))
        assertEquals(0, recovering.needModifier(NPCAction.START_WORK))
    }

    @Test
    fun `capture copies authoritative NPC needs without reading player APIs and stays detached`() {
        val npc = AINPC(null).apply {
            hungerLevel = 12
            energyLevel = 23
            socialNeedLevel = 34
            comfortLevel = 45
            safetyLevel = 56
            currentGoal = "sa ajung la atelier"
            plannedRoutineActivity = "sa lucrez"
            changeState(NPCState.TALKING)
        }
        npc.context.hungerLevel = 100
        npc.context.energyLevel = 100
        npc.context.isInDanger = true
        npc.context.setInteractingPlayer(Proxy.newProxyInstance(
            Player::class.java.classLoader, arrayOf(Player::class.java),
        ) { _, method, _ -> throw AssertionError("Capture must not call Player.${method.name}") } as Player)

        val snapshot = NpcCognitiveState.capture(npc)
        npc.hungerLevel = 100
        npc.currentGoal = "sa ma intorc acasa"
        npc.context.isInDanger = false
        npc.context.setInteractingPlayer(null)

        assertEquals(
            NpcCognitiveState(
                hungerLevel = 12, energyLevel = 23, socialNeedLevel = 34, comfortLevel = 45, safetyLevel = 56,
                isInDanger = true, currentState = NPCState.TALKING, hasInteractingPlayer = true,
                currentGoal = "sa ajung la atelier", plannedRoutineActivity = "sa lucrez",
            ),
            snapshot,
        )
        assertEquals(NpcCognitiveState.Focus.SAFETY, snapshot.focus)
    }

    @Test
    fun `capture recognizes stored NPC interlocutors without treating nearby entities as conversations`() {
        val npc = AINPC(null).apply { changeState(NPCState.LISTENING) }
        val context = NPCContext(npc)

        assertFalse(NpcCognitiveState.capture(npc, context).hasInteractingPlayer)
        context.interactingNPC = AINPC(null)
        val snapshot = NpcCognitiveState.capture(npc, context)

        assertTrue(snapshot.hasInteractingPlayer)
        assertEquals(NpcCognitiveState.Focus.CONVERSATION, snapshot.focus)
    }
}
