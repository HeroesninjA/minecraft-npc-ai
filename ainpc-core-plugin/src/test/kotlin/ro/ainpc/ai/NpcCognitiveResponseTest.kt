package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NpcSelfCareGoal
import ro.ainpc.npc.NpcFoodRequestPreferences

class NpcCognitiveResponseTest {
    private val hungry = NpcCognitiveState(hungerLevel = 10)

    @Test
    fun `missing progress is communicated from captured evidence without naming a blocked path`() {
        val progress = NpcSelfCareGoal.Snapshot(
            NPCAction.EAT, NpcSelfCareGoal.Stage.RETURNING_HOME, 10, 10, "world:home", false,
            1_000L, withoutProgressMillis = 180_000L,
        )
        val state = hungry.copy(selfCareProgress = progress)
        val response = NpcCognitiveResponse.resolve("Cum merge obiectivul de hrana?", state)!!

        assertEquals(progress.description(), response)
        assertTrue(response.contains("cauza nu este stabilita"))
        assertTrue(!response.contains("ruta blocata"))
        assertEquals(progress, state.selfCareProgress)
    }

    @Test
    fun `Romanian thought questions use current needs despite diacritics and punctuation`() {
        for (message in listOf("La ce te gândești?", "CE AI ÎN MINTE?", "Ce gânduri ai acum?")) {
            assertTrue(NpcCognitiveResponse.resolve(message, hungry)!!.contains("foame"))
        }
    }

    @Test
    fun `needs questions are grounded without promising food or inventory actions`() {
        val response = NpcCognitiveResponse.resolve("Ți-e foame?", hungry)!!

        assertTrue(response.contains("foame"))
        assertEquals(response, NpcCognitiveResponse.resolve("De ce ai nevoie?", hungry))
        assertEquals(response, NpcCognitiveResponse.resolve("Ai nevoie de ceva?", hungry))
    }

    @Test
    fun `multiple questions retain requested order without repeating the same answer`() {
        val message = "Ce nevoi ai și la ce te gândești? Ce gânduri ai?"

        assertEquals(
            hungry.needsDescription() + " " + hungry.thoughtDescription(),
            NpcCognitiveResponse.resolve(message, hungry),
        )
    }

    @Test
    fun `intention questions use the real current goal`() {
        val state = NpcCognitiveState(currentGoal = "sa ajung acasa")

        assertTrue(NpcCognitiveResponse.resolve("Care este obiectivul tău?", state)!!.contains("sa ajung acasa"))
    }

    @Test
    fun `questions about why a particular object or person is needed stay in contextual dialogue`() {
        assertNull(NpcCognitiveResponse.resolve("De ce ai nevoie de mine?", hungry))
        assertNull(NpcCognitiveResponse.resolve("De ce ai nevoie de sabia asta?", hungry))
        assertEquals(
            hungry.needsDescription(),
            NpcCognitiveResponse.resolve("De ce ai nevoie de mine? Si de ce ai nevoie?", hungry),
        )
    }

    @Test
    fun `learning questions use captured experience and acknowledge absent evidence`() {
        val unknown = NpcCognitiveResponse.resolve("Ce ai învățat până acum?", hungry)!!
        assertTrue(unknown.contains("Nu am inca"))
        val observation = "In timpul mesei de acasa, m-am simtit mai satul."
        val experienced = hungry.copy(experienceDescription = observation)

        assertEquals(observation, NpcCognitiveResponse.resolve("Ce ai învățat din experiență?", experienced))
        assertEquals(
            observation + " " + experienced.needsDescription(),
            NpcCognitiveResponse.resolve("Ce ai invatat? Si de ce ai nevoie?", experienced),
        )
        assertNull(NpcCognitiveResponse.resolve("Ce ai invatat despre mine?", experienced))
        assertNull(NpcCognitiveResponse.resolve("Ce ai invatat din povestea mea?", experienced))
    }

    @Test
    fun `known help changes requests only for safe hungry states`() {
        val confirmed = "Mi-ai oferit mancare cand imi era foame."
        val request = NpcCognitiveResponse.resolve("De ce ai nevoie?", hungry, confirmed)!!
        assertTrue(request.contains("ma poti ajuta din nou"))
        for (state in listOf(
            hungry.copy(isInDanger = true), hungry.copy(safetyLevel = 34), hungry.copy(hungerLevel = 35),
            hungry.copy(currentState = NPCState.COMBAT), hungry.copy(currentState = NPCState.SLEEPING),
            hungry.copy(currentState = NPCState.FLEEING), hungry.copy(currentState = NPCState.PANICKING),
        )) {
            assertTrue(!NpcCognitiveResponse.resolve("De ce ai nevoie?", state, confirmed)!!.contains("ma poti ajuta din nou"))
        }
        assertTrue(!NpcCognitiveResponse.resolve("De ce ai nevoie?", hungry)!!.contains("ma poti ajuta din nou"))
    }

    @Test
    fun `typed pauses explain waiting without a new request or invented help`() {
        for (reply in NpcFoodRequestPreferences.Reply.entries) {
            val response = NpcCognitiveResponse.resolve("De ce ai nevoie?", hungry, "", true, reply)!!
            assertEquals(hungry.needsDescription() + " " + reply.waitingDescription, response)
            assertFalse(response.contains("ma poti ajuta din nou"))
            assertTrue(NpcCognitiveResponse.resolve("Ce ai invatat?", hungry, "", false, reply)!!.contains("Nu am inca"))
            assertTrue(NpcCognitiveResponse.resolve("Te-am ajutat cu mancare?", hungry, "", false, reply)!!
                .contains("Nu am o amintire confirmata"))
        }
        assertEquals(hungry.needsDescription(), NpcCognitiveResponse.resolve(
            "De ce ai nevoie?", hungry, "Mi-ai oferit mancare.", allowFoodRequest = false,
        ))
    }

    @Test
    fun `waiting explanations stay scoped to safe food needs and the actual question`() {
        val reply = NpcFoodRequestPreferences.Reply.PROMISE
        for (state in listOf(
            hungry.copy(hungerLevel = 35), hungry.copy(hungerLevel = -1), hungry.copy(isInDanger = true),
            hungry.copy(safetyLevel = 34), hungry.copy(currentState = NPCState.COMBAT),
            hungry.copy(currentState = NPCState.SLEEPING),
        )) {
            assertEquals(state.needsDescription(), NpcCognitiveResponse.resolve("De ce ai nevoie?", state, "", false, reply))
        }
        assertEquals(hungry.intentionDescription(), NpcCognitiveResponse.resolve("Ce vrei sa faci?", hungry, "", false, reply))
        assertNull(NpcCognitiveResponse.resolve("De ce ai nevoie de mine?", hungry, "", false, reply))
    }

    @Test
    fun `the public four argument response method remains available to Java callers`() {
        val method = NpcCognitiveResponse::class.java.getMethod(
            "resolve", String::class.java, NpcCognitiveState::class.java, String::class.java, java.lang.Boolean.TYPE,
        )
        assertEquals(hungry.needsDescription(), method.invoke(NpcCognitiveResponse, "De ce ai nevoie?", hungry, "", false))
    }

    @Test
    fun `decision questions use captured historical evidence and acknowledge its absence`() {
        val explanation = "La ultima decizie, am ales sa ma ocup de hrana."
        val state = hungry.copy(decisionDescription = explanation)
        for (message in listOf(
            "De ce ai ales asta?", "De ce ai decis asta?", "De ce faci asta?",
            "Cum ai luat decizia?", "Care a fost ultima ta decizie?", "Ce ai decis?",
        )) {
            assertEquals(explanation, NpcCognitiveResponse.resolve(message, state), message)
        }
        assertTrue(NpcCognitiveResponse.resolve("Ce ai decis?", hungry)!!.contains("Nu am o decizie recenta"))
        assertEquals(
            explanation + " " + state.needsDescription(),
            NpcCognitiveResponse.resolve("De ce ai ales asta si de ce ai nevoie?", state),
        )
        assertEquals(
            state.needsDescription() + " " + explanation,
            NpcCognitiveResponse.resolve(
                "Maria a zis \"ce ai decis\" si a plecat; dar ce nevoi ai si de ce ai ales asta?", state,
            ),
        )
        for (message in listOf(
            "Ce ai decis despre mine?", "De ce ai ales sabia asta?", "De ce a ales asta Maria?",
            "Maria a zis de ce ai ales asta", "Explica expresia \"de ce ai ales asta\".",
        )) {
            assertNull(NpcCognitiveResponse.resolve(message, state), message)
        }
    }

    @Test
    fun `care progress questions distinguish the recorded objective from unrelated or absent goals`() {
        val progress = NpcSelfCareGoal.Snapshot(
            NPCAction.EAT, NpcSelfCareGoal.Stage.RETURNING_HOME, 10, 10, "world:home", false,
            System.currentTimeMillis(),
        )
        val state = hungry.copy(selfCareProgress = progress)
        for (message in listOf("Cum merge îngrijirea ta?", "Cum merge obiectivul de hrană?", "Ai reusit sa te refaci?")) {
            assertEquals(progress.description(), NpcCognitiveResponse.resolve(message, state), message)
        }
        assertTrue(NpcCognitiveResponse.resolve("Cum merge obiectivul de odihna?", state)!!.contains("Nu am un obiectiv recent"))
        assertTrue(NpcCognitiveResponse.resolve("Cum merge ingrijirea ta?", hungry)!!.contains("Nu am un obiectiv recent"))
        assertEquals(
            state.needsDescription() + " " + progress.description(),
            NpcCognitiveResponse.resolve(
                "Maria a zis \"cum merge ingrijirea ta\"; dar ce nevoi ai si cum merge obiectivul de hrana?", state,
            ),
        )
        for (message in listOf(
            "Cum merge obiectivul de hrana al Mariei?", "Cum merge questul tau?",
            "Maria a zis cum merge ingrijirea ta", "Explica expresia \"cum merge ingrijirea ta\".",
        )) {
            assertNull(NpcCognitiveResponse.resolve(message, state), message)
        }
    }

    @Test
    fun `other people and words sharing a prefix do not trigger introspection`() {
        for (message in listOf("La ce mă gândesc?", "Ce are în minte Radu?", "Ești obositor.", "Salut", "")) {
            assertNull(NpcCognitiveResponse.resolve(message, hungry), message)
        }
        assertNull(NpcCognitiveResponse.resolve(null, hungry))
    }
}
