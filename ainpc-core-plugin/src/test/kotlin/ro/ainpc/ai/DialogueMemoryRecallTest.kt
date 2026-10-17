package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.managers.MemoryRecallCandidate
import ro.ainpc.managers.MemoryRecallSelector

class DialogueMemoryRecallTest {
    private val swordQuestion = "Ce stii despre sabia pierduta?"
    private val swordExchange = DialogHistory(swordQuestion, "Vrei sa vorbim despre aur?", 1L)
    private val swordMemory = MemoryRecallCandidate("Jucatorul a pierdut sabia.", "game_event")
    private val breadMemory = MemoryRecallCandidate("Jucatorul mi-a oferit paine.", "game_event")

    @Test
    fun `continuations recall the player topic instead of unrelated higher priority memories`() {
        for (message in listOf("Continuă!", "De ce?", "Spune-mi mai mult, te rog.", "Nu mi-ai răspuns, explică mai clar.")) {
            val query = PlayerConversationContinuation.memoryQuery(message, listOf(swordExchange))

            assertEquals(swordQuestion, query, message)
            assertEquals(listOf(swordMemory), MemoryRecallSelector.select(query, listOf(breadMemory, swordMemory), 1), message)
        }
    }

    @Test
    fun `explicit new questions take priority over the previous topic`() {
        for (message in listOf("Unde gasesc paine?", "Continua, dar unde gasesc paine?", "Da, unde gasesc paine?")) {
            val query = PlayerConversationContinuation.memoryQuery(message, listOf(swordExchange))

            assertEquals(message, query)
            assertEquals(listOf(breadMemory), MemoryRecallSelector.select(query, listOf(swordMemory, breadMemory), 1))
        }
        val repairQuery = PlayerConversationContinuation.memoryQuery("Nu mi-ai raspuns, unde gasesc paine?", listOf(swordExchange))
        assertEquals("unde gasesc paine?", repairQuery)
        assertEquals(listOf(breadMemory), MemoryRecallSelector.select(repairQuery, listOf(swordMemory, breadMemory), 1))
    }

    @Test
    fun `follow up chains retain the latest explicit correction in chronological order`() {
        val history = listOf(
            swordExchange,
            DialogHistory("Nu mi-ai raspuns, unde gasesc paine?", "Cauta in sat.", 2L),
            DialogHistory("Continua", "Nu am alte detalii.", 3L),
            DialogHistory("Repeta ultima replica", "Nu am alte detalii.", 4L),
            DialogHistory("De ce?", "Nu am informatii noi.", 5L),
        )

        assertEquals("unde gasesc paine?", PlayerConversationContinuation.memoryQuery("Continua", history.reversed()))
    }

    @Test
    fun `recap interruptions and greetings prevent reuse of an older topic`() {
        for (interruption in listOf("Ce am discutat despre vreme?", "Despre ce am vorbit?", "Salut!")) {
            val history = listOf(swordExchange, DialogHistory(interruption, "Nu am detalii.", 2L))

            assertEquals("Continua", PlayerConversationContinuation.memoryQuery("Continua", history), interruption)
        }
    }

    @Test
    fun `failed proposed and incomplete exchanges cannot supply a memory topic`() {
        for (status in listOf(DialogHistory.BranchStatus.FAILED, DialogHistory.BranchStatus.REJECTED, DialogHistory.BranchStatus.PROPOSED)) {
            assertEquals("Continua", PlayerConversationContinuation.memoryQuery("Continua", listOf(swordExchange.copy(branchStatus = status))))
        }
        assertEquals("Continua", PlayerConversationContinuation.memoryQuery("Continua", listOf(swordExchange.copy(npcResponse = " "))))
        assertEquals("Continua", PlayerConversationContinuation.memoryQuery("Continua", emptyList()))

        val failedChange = DialogHistory("Unde gasesc paine?", "In sat.", 2L, DialogHistory.BranchStatus.FAILED)
        assertEquals(swordQuestion, PlayerConversationContinuation.memoryQuery("Continua", listOf(swordExchange, failedChange)))
    }

    @Test
    fun `short answers inherit a topic only when the latest stable NPC reply is a question`() {
        for (message in listOf("Da", "Nu", "A doua")) {
            assertEquals(swordQuestion, PlayerConversationContinuation.memoryQuery(message, listOf(swordExchange)), message)
            assertEquals(message, PlayerConversationContinuation.memoryQuery(message, listOf(swordExchange.copy(npcResponse = "Nu am detalii."))), message)
        }
        assertEquals("Da?", PlayerConversationContinuation.memoryQuery("Da?", listOf(swordExchange)))
    }

    @Test
    fun `topics outside the last five stable exchanges are not reintroduced`() {
        val history = listOf(swordExchange) + (2L..6L).map { timestamp -> DialogHistory("Continua", "Nu am alte detalii.", timestamp) }

        assertEquals("Continua", PlayerConversationContinuation.memoryQuery("Continua", history))
    }

    @Test
    fun `memory queries do not carry topics between supplied conversation histories`() {
        val breadHistory = listOf(DialogHistory("Unde gasesc paine?", "Cauta in sat.", 1L))

        assertEquals(swordQuestion, PlayerConversationContinuation.memoryQuery("Continua", listOf(swordExchange)))
        assertEquals("Unde gasesc paine?", PlayerConversationContinuation.memoryQuery("Continua", breadHistory))
        assertEquals("Continua", PlayerConversationContinuation.memoryQuery("Continua", emptyList()))
    }
}
