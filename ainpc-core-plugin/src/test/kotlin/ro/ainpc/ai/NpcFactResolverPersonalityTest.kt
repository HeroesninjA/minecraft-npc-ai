package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NPCPersonality

class NpcFactResolverPersonalityTest {
    @Test
    fun `personal question uses the strongest configured traits`() {
        val personality = NPCPersonality(0.95, 0.5, 0.1, 0.5, 0.5)
        val description = NpcFactResolver.describePersonality(personality)
        val facts = npcFacts(description)

        assertEquals("De obicei, imi plac ideile noi si prefer linistea.", description)
        assertEquals(description, NpcFactResolver.resolve("Ce fel de persoană ești?", facts).orElseThrow())
        assertEquals("Ma simt ingrijorat.", NpcFactResolver.resolve("Cum te simti?", facts).orElseThrow())
    }

    @Test
    fun `neutral traits give a stable answer and can combine with the name`() {
        val description = NpcFactResolver.describePersonality(NPCPersonality())
        val response = NpcFactResolver.resolve("Cum te cheama si ce personalitate ai?", npcFacts(description)).orElseThrow()

        assertEquals("De obicei, ma adaptez la situatie.", description)
        assertTrue(response.contains("Sunt Mara"))
        assertTrue(response.contains("ma adaptez la situatie"))
    }

    @Test
    fun `unrelated place question is not treated as a personality request`() {
        assertFalse(NpcFactResolver.resolve("Ce fel de sat este aici?", npcFacts("De obicei, prefer linistea.")).isPresent)
    }

    private fun npcFacts(personalityDescription: String) = NpcFactResolver.NpcFacts(
        "Mara", "fierar", "ingrijorat", "La lucru", "lucrez", "fierarie", "",
        personalityDescription,
    )
}
