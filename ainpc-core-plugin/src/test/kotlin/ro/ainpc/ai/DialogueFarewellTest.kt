package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DialogueFarewellTest {
    @Test
    fun `complete farewells accept punctuation diacritics and courtesy`() {
        for (message in listOf(
            "Pa!", "PA...", "La revedere!", "Pe curând.", "Pa, ne vedem!", "Pa pa!",
            "Mulțumesc, la revedere!", "O seară bună!", "Noapte bună.", "Bye!", "exit", "quit.",
        )) {
            assertTrue(DialogueFarewell.isFarewell(message), message)
        }
    }

    @Test
    fun `questions quotations and mixed messages do not end a conversation`() {
        for (message in listOf(
            "Pa?", "Ce inseamna la revedere?", "Nu spun pa.", "El a spus pa.",
            "\"pa\"", "„La revedere!”", "Pa, unde este piata?", "pa am nevoie de ajutor",
            "Pa, mai am o intrebare.", "Ce parere ai?", "Multumesc!", "", "...",
        )) {
            assertFalse(DialogueFarewell.isFarewell(message), message)
        }
    }

    @Test
    fun `known npc names can address a complete farewell`() {
        for (message in listOf("Maria, pa!", "La revedere, Măria!", "Pa Maria", "Maria, pa, Maria!", "Pe curând, Maria Fierarul!")) {
            assertTrue(DialogueFarewell.isFarewell(message, listOf("Măria", "Maria Fierarul")), message)
        }
        assertTrue(DialogueFarewell.isFarewell("La revedere, A.b [smith]!", listOf("A.b [smith]")))
        assertTrue(DialogueFarewell.isFarewell("A.b [smith], pa!", listOf("A.b [smith]")))
    }

    @Test
    fun `other names reported speech and partial aliases do not become a farewell to this npc`() {
        for (message in listOf(
            "Pa, Maria!", "Mariana, pa!", "Bob a spus pa.", "Bob, spune pa!", "Bob, pa, unde mergi?",
            "\"Bob, pa!\"", "Pa, Bob, Maria!",
        )) {
            assertFalse(DialogueFarewell.isFarewell(message, listOf("Bob")), message)
        }
        assertFalse(DialogueFarewell.isFarewell("Pa, Mariana!", listOf("Maria")))
        assertFalse(DialogueFarewell.isFarewell("Pa, AxB smith!", listOf("A.b [smith]")))
    }
}
