package ro.ainpc.managers

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NpcActionExperience
import java.math.BigDecimal

class NpcActionExperienceCodecTest {
    private val observed = NpcActionExperience.Observation(NPCAction.EAT, "hungry:home", 8, 12.5, 1_700_000_000_001L)

    @Test
    fun `round trip preserves action context effect and exact long timestamps`() {
        val observations = listOf(
            observed,
            observed.copy(action = NPCAction.SLEEP, contextKey = "tired:home", observedAt = 9_007_199_254_740_993L),
        )
        val memory = NpcActionExperience().apply { restore(observations) }

        val serialized = NpcActionExperienceCodec.write(memory)
        val reparsed = Gson().fromJson(Gson().toJson(serialized), JsonObject::class.java)

        assertEquals(1, serialized.get("version").asInt)
        assertEquals(observations, NpcActionExperienceCodec.read(reparsed))
        assertEquals(observations, memory.snapshot())
    }

    @Test
    fun `missing wrong type and unsupported version restore no experience`() {
        val invalid = listOf(
            null,
            JsonNull.INSTANCE,
            JsonArray(),
            JsonPrimitive("memory"),
            JsonObject(),
            envelope(observationJson()).apply { addProperty("version", "1") },
            envelope(observationJson()).apply { addProperty("version", true) },
            envelope(observationJson()).apply { addProperty("version", 1.5) },
            envelope(observationJson()).apply { addProperty("version", 2) },
            envelope(observationJson()).apply { add("observations", JsonObject()) },
            envelope(observationJson()).apply { remove("observations") },
        )

        invalid.forEach { value ->
            assertTrue(NpcActionExperienceCodec.read(value).isEmpty(), value.toString())
        }
    }

    @Test
    fun `bad observations are skipped independently without coercing strings fractions or overflow`() {
        val invalid = listOf(
            JsonNull.INSTANCE,
            JsonArray(),
            JsonPrimitive("entry"),
            observationJson().apply { remove("action") },
            observationJson().apply { addProperty("action", true) },
            observationJson().apply { addProperty("action", "UNKNOWN_ACTION") },
            observationJson().apply { addProperty("action", NPCAction.REST.name) },
            observationJson().apply { addProperty("context_key", 42) },
            observationJson().apply { addProperty("context_key", " ") },
            observationJson().apply { addProperty("context_key", "x".repeat(257)) },
            observationJson().apply { addProperty("need_delta", "8") },
            observationJson().apply { addProperty("need_delta", true) },
            observationJson().apply { addProperty("need_delta", 8.5) },
            observationJson().apply { addProperty("need_delta", BigDecimal("4294967304")) },
            observationJson().apply { addProperty("need_delta", 101) },
            observationJson().apply { addProperty("need_delta", -101) },
            observationJson().apply { addProperty("effect", "12.5") },
            observationJson().apply { addProperty("effect", false) },
            observationJson().apply { add("effect", JsonNull.INSTANCE) },
            observationJson().apply { addProperty("effect", Double.NaN) },
            observationJson().apply { addProperty("effect", Double.POSITIVE_INFINITY) },
            observationJson().apply { addProperty("effect", Double.NEGATIVE_INFINITY) },
            observationJson().apply { addProperty("effect", 200.1) },
            observationJson().apply { addProperty("effect", -200.1) },
            observationJson().apply { addProperty("observed_at", "1700000000001") },
            observationJson().apply { addProperty("observed_at", true) },
            observationJson().apply { addProperty("observed_at", BigDecimal("1700000000001.5")) },
            observationJson().apply { addProperty("observed_at", BigDecimal("9223372036854775808")) },
            observationJson().apply { addProperty("observed_at", 0) },
            observationJson().apply { addProperty("observed_at", -1) },
        )

        invalid.forEach { value ->
            assertEquals(listOf(observed), NpcActionExperienceCodec.read(envelope(value, observationJson())), value.toString())
        }
    }

    @Test
    fun `integral numeric decimals retain their exact value`() {
        val observation = observationJson().apply {
            addProperty("need_delta", BigDecimal("8.0"))
            addProperty("observed_at", BigDecimal("1700000000001.0"))
        }

        assertEquals(listOf(observed), NpcActionExperienceCodec.read(envelope(observation)))
    }

    @Test
    fun `reads are bounded to the newest 32 array entries`() {
        val entries = (1L..48L).map { timestamp -> observationJson(observed.copy(observedAt = timestamp)) }

        val loaded = NpcActionExperienceCodec.read(envelope(*entries.toTypedArray()))

        assertEquals((17L..48L).toList(), loaded.map { it.observedAt })
    }

    @Test
    fun `profile rebuild persists experience alongside simulation and narrative data`() {
        val gson = Gson()
        val npc = AINPC(null).apply {
            hungerLevel = 41
            currentGoal = "sa ajung acasa"
            profileDataJson = "{\"narrative\":{\"existing\":\"yes\"}}"
            actionExperience.restore(listOf(observed))
        }

        val profile = gson.fromJson(buildProfileData(npc, gson), JsonObject::class.java)
        val simulation = profile.getAsJsonObject("simulation")
        val restored = AINPC(null).apply {
            actionExperience.restore(NpcActionExperienceCodec.read(simulation.get("action_experience")))
        }
        val rebuilt = gson.fromJson(buildProfileData(restored, gson), JsonObject::class.java)

        assertEquals(41, simulation.get("hunger_level").asInt)
        assertEquals("sa ajung acasa", simulation.get("current_goal").asString)
        assertEquals("yes", profile.getAsJsonObject("narrative").get("existing").asString)
        assertEquals(listOf(observed), restored.actionExperience.snapshot())
        assertEquals(
            listOf(observed),
            NpcActionExperienceCodec.read(rebuilt.getAsJsonObject("simulation").get("action_experience")),
        )
    }

    private fun envelope(vararg observations: JsonElement): JsonObject = JsonObject().apply {
        addProperty("version", 1)
        add("observations", JsonArray().apply { observations.forEach(::add) })
    }

    private fun observationJson(observation: NpcActionExperience.Observation = observed): JsonObject = JsonObject().apply {
        addProperty("action", observation.action.name)
        addProperty("context_key", observation.contextKey)
        addProperty("need_delta", observation.needDelta)
        addProperty("effect", observation.effect)
        addProperty("observed_at", observation.observedAt)
    }
}
