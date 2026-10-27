package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class OpenAIProfilePromptLimitTest {
    private val gson = Gson()

    @Test
    fun `oversized profile is omitted rather than cut into partial JSON facts`() {
        val profile = jsonWithText("PROFILE_PAYLOAD_".repeat(200))
        val snapshot = snapshot(profile)

        val prompt = buildPrompt(snapshot)

        assertEquals("{}", profileLine(prompt))
        assertTrue(prompt.contains("[Profil JSON omis:"))
        assertFalse(prompt.contains("PROFILE_PAYLOAD_"))
        assertEquals(profile, snapshot.profileDataJson)
        for (required in listOf("profil_rezumat: PROFILE_SUMMARY", "npc_name: \"Ion\"", "NPC_DESCRIPTION", "Steve: \"CURRENT_MESSAGE\"")) {
            assertTrue(prompt.contains(required), required)
        }
        assertTrue(prompt.endsWith("Ion: "))
    }

    @Test
    fun `profile at the exact serialized character limit is preserved in full`() {
        val profile = jsonWithText("a".repeat(1200 - jsonWithText("").length))
        assertEquals(1200, profile.length)

        val prompt = buildPrompt(snapshot(profile))

        assertEquals(profile, profileLine(prompt))
        assertEquals(JsonParser.parseString(profile), JsonParser.parseString(profileLine(prompt)))
        assertFalse(prompt.contains("[Profil JSON omis:"))
    }

    @Test
    fun `escaped and Unicode profile text is budgeted after serialization without splitting values`() {
        for (text in listOf("\\".repeat(700), "🌍".repeat(700), "before\n\"quoted\"\tafter".repeat(100))) {
            val profile = jsonWithText(text)
            assertTrue(profile.length > 1200)

            val prompt = buildPrompt(snapshot(profile))

            assertEquals("{}", profileLine(prompt))
            assertTrue(JsonParser.parseString(profileLine(prompt)).isJsonObject)
            assertTrue(prompt.contains("[Profil JSON omis:"))
        }
    }

    @Test
    fun `private social receipts are removed before applying the profile size limit`() {
        val profile = gson.toJson(mapOf(
            "name" to "Ion",
            "simulation" to mapOf(
                "hunger_level" to 20,
                "social_experience" to mapOf("receipts" to "PRIVATE_RECEIPT_".repeat(200)),
            ),
        ))
        assertTrue(profile.length > 1200)

        val prompt = buildPrompt(snapshot(profile))
        val visibleProfile = JsonParser.parseString(profileLine(prompt)).asJsonObject

        assertEquals("Ion", visibleProfile.get("name").asString)
        assertEquals(20, visibleProfile.getAsJsonObject("simulation").get("hunger_level").asInt)
        assertFalse(prompt.contains("PRIVATE_RECEIPT_"))
        assertFalse(prompt.contains("[Profil JSON omis:"))
    }

    @Test
    fun `small valid profile values and request instruction boundaries remain unchanged`() {
        val note = "Nume: Ana\nA spus: \"Salut!\"\tCale: C:\\sat\\casa 🌍"
        val profile = jsonWithText(note)
        val prompt = buildPrompt(snapshot(profile))

        assertEquals(profile, profileLine(prompt))
        assertEquals(note, JsonParser.parseString(profileLine(prompt)).asJsonObject.get("note").asString)
        assertFalse(prompt.contains("[Profil JSON omis:"))
        val request = OpenAIService.buildResponsesRequestBody(
            "gpt-5.4-nano", prompt, 0.7, 150, false, instructions = OpenAIPromptBuilder.dialogueInstructions,
        )
        assertEquals(OpenAIPromptBuilder.dialogueInstructions, request.get("instructions").asString)
        assertEquals(prompt, request.get("instructions").asString + request.get("input").asString)
    }

    private fun jsonWithText(text: String): String = gson.toJson(JsonObject().apply { addProperty("note", text) })

    private fun profileLine(prompt: String): String = prompt.substringAfter("profil_json_db: ").substringBefore('\n')

    private fun buildPrompt(snapshot: PromptSnapshot): String =
        OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)

    private fun snapshot(profile: String): PromptSnapshot = PromptSnapshot(
        npcUuid = UUID.randomUUID(),
        npcName = "Ion",
        npcDescription = "NPC_DESCRIPTION",
        environmentDescription = "",
        topologyConsensusBlock = "",
        semanticWorldContext = "",
        familyMembers = emptyList(),
        profileCreated = true,
        profileSource = "test",
        profileVersion = 1,
        profileSummary = "PROFILE_SUMMARY",
        profileDataJson = profile,
        traitIds = emptyList(),
        playerName = "Steve",
        playerMessage = "CURRENT_MESSAGE",
        occupation = "paznic",
        emotionShortDescription = "calm",
        dominantEmotion = "calm",
        currentState = "idle",
        currentActivity = "sta de vorba",
        locationDescription = "sat",
        directAddress = true,
        explicitConversation = true,
        triggerReason = "direct",
        nearbyNpcCount = 1,
        distanceToNpc = 2.0,
    )
}
