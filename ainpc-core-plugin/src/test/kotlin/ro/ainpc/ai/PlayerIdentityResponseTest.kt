package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerIdentityResponseTest {
    @Test
    fun `first meeting does not become familiarity because of regional reputation`() {
        val response = PlayerIdentityResponse.resolve(
            "Mă cunoști?", "Ana", PlayerRecognition.from(null, 0, 0, 50), null
        )

        assertEquals("Nu cred ca ne-am mai intalnit, Ana.", response)
    }

    @Test
    fun `known player is recognized without invented memories`() {
        val response = PlayerIdentityResponse.resolve(
            "Ce știi despre mine?", "Ana", PlayerRecognition.from(null, 1, 0), null
        )

        assertEquals("Te recunosc, Ana.", response)
    }

    @Test
    fun `persisted friendship is described without adding an invented event`() {
        val recognition = PlayerRecognition.from(
            NPCRelationship(interactionCount = 3, relationshipType = "friend"), 1, 0
        )

        assertEquals(
            "Ana, te cunosc drept prieten.",
            PlayerIdentityResponse.resolve("Cine sunt eu?", "Ana", recognition, null)
        )
    }

    @Test
    fun `profile remains a declaration and does not erase guarded relationship`() {
        val recognition = PlayerRecognition.from(
            NPCRelationship(trust = -0.5, interactionCount = 2), 1, 0
        )
        val response = PlayerIdentityResponse.resolve(
            "Îți amintești de mine?", "Ana", recognition,
            PlayerProfile(PlayerRole.EXPLORATOR, PlayerStyle.PRIETENOS, PlayerTrait.CURIOS)
        )

        assertTrue(response!!.contains("relatia noastra este tensionata"))
        assertTrue(response.contains("te prezinti drept explorator"))
        assertTrue(response.contains("spui ca esti curios"))
    }

    @Test
    fun `unrelated question is not intercepted`() {
        assertNull(PlayerIdentityResponse.resolve(
            "Unde este magazinul?", "Ana", PlayerRecognition.from(null, 0, 0), null
        ))
    }

    @Test
    fun `personal statement is recalled only when asked`() {
        val recognition = PlayerRecognition.from(null, 1, 1)
        val statements = listOf("locuiesc in Valea Noua")

        val recalled = PlayerIdentityResponse.resolve(
            "Ce tii minte despre mine?", "Ana", recognition, null, statements
        )
        val greeting = PlayerIdentityResponse.resolve(
            "Ma cunosti?", "Ana", recognition, null, statements
        )

        assertTrue(recalled!!.contains("Mi-ai cerut sa tin minte ca locuiesc in Valea Noua."))
        assertEquals("Te recunosc, Ana.", greeting)
    }

    @Test
    fun `recall lists recent statements without claiming to verify them`() {
        val response = PlayerIdentityResponse.resolve(
            "Ce tii minte despre mine?",
            "Ana",
            PlayerRecognition.from(null, 1, 4),
            null,
            listOf("imi place fieraria", "locuiesc in sat", "caut un cal", "port o harta"),
        )

        assertTrue(response!!.contains("Mi-ai cerut sa tin minte, printre altele:"))
        assertTrue(response.contains("imi place fieraria; locuiesc in sat; caut un cal"))
        assertFalse(response.contains("port o harta"))
    }
}
