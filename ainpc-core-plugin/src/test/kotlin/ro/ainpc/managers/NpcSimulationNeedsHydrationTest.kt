package ro.ainpc.managers

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.DecisionEngine
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NpcSocialExperience
import java.math.BigDecimal
import java.util.UUID

class NpcSimulationNeedsHydrationTest {
    private val gson = Gson()
    private val defaults = needs(AINPC(null))
    private val stored = linkedMapOf(
        "hunger_level" to 31,
        "energy_level" to 45,
        "social_need_level" to 58,
        "comfort_level" to 67,
        "safety_level" to 90,
    )
    private val home = AINPC.OwnedLocation("home", "Casa", "world", 1.0, 64.0, 2.0)
    private val receipt = NpcSocialExperience.Receipt(
        UUID(0L, 1L), UUID(0L, 2L), "Helper", "minecraft:apple", 1, 10, 26, 1_700_000_000_001L,
    )
    private val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        field.get(null) as sun.misc.Unsafe
    }
    private val plugin = (unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin).apply {
        JavaPlugin::class.java.getDeclaredField("newConfig").apply { isAccessible = true }
            .set(this, YamlConfiguration())
    }

    @Test
    fun `out of range fractional and overflowing needs preserve the previous level without narrowing`() {
        val invalidNumbers = listOf("-1", "101", "20.5", "4294967316", "-4294967276",
            "9223372036854775828", "1e10000", "1e-10000")
        for (key in stored.keys) {
            for (number in invalidNumbers) {
                val profile = profile()
                profile.getAsJsonObject("simulation").addProperty(key, BigDecimal(number))

                val restored = hydrate(profile)

                assertEquals(stored + (key to defaults.getValue(key)), needs(restored), "$key=$number")
                assertEquals("sa ajung acasa", restored.currentGoal)
                assertHome(restored)
            }
        }
    }

    @Test
    fun `wrong need types do not abort valid needs goals or anchors`() {
        val invalidValues: List<JsonElement> = listOf(JsonNull.INSTANCE, JsonPrimitive(true),
            JsonPrimitive(false), JsonPrimitive("20"), JsonPrimitive("invalid"), JsonArray(), JsonObject())
        for (key in stored.keys) {
            for (value in invalidValues) {
                val profile = profile()
                profile.getAsJsonObject("simulation").add(key, value)

                val restored = hydrate(profile)

                assertEquals(stored + (key to defaults.getValue(key)), needs(restored), "$key=$value")
                assertEquals("sa ajung acasa", restored.currentGoal)
                assertHome(restored)
            }
        }
    }

    @Test
    fun `missing need fields and absent or wrong simulation envelopes keep prior needs`() {
        for (key in stored.keys) {
            val profile = profile()
            profile.getAsJsonObject("simulation").remove(key)
            assertEquals(stored + (key to defaults.getValue(key)), needs(hydrate(profile)), key)
        }
        val envelopes: List<JsonElement?> = listOf(null, JsonNull.INSTANCE, JsonArray(),
            JsonPrimitive("simulation"), JsonPrimitive(false), JsonObject())
        for (envelope in envelopes) {
            val profile = profile()
            if (envelope == null) profile.remove("simulation") else profile.add("simulation", envelope)
            assertEquals(defaults, needs(hydrate(profile)), envelope.toString())
        }
    }

    @Test
    fun `valid bounds and exact integral decimal or exponent tokens remain compatible`() {
        val values = listOf("0", "1", "34", "35", "59", "60", "99", "100", "20.0", "2e1")
        for (value in values) {
            val number = BigDecimal(value)
            val profile = profile()
            for (key in stored.keys) profile.getAsJsonObject("simulation").addProperty(key, number)

            val restored = hydrate(profile)

            assertEquals(stored.mapValues { number.intValueExact() }, needs(restored), value)
            assertHome(restored)
        }
    }

    @Test
    fun `invalid tick numbers preserve the previous timestamp without narrowing or truncation`() {
        val numbers = listOf("-1", "-9223372036854775808", "1.5", "9223372036854775808",
            "18446744073709551617", "-18446744073709551615", "1e10000", "1e-10000")
        for (number in numbers) {
            val profile = profile()
            profile.getAsJsonObject("simulation").addProperty("last_simulation_tick_at", BigDecimal(number))

            val restored = hydrate(profile, previousTickAt = 37L)

            assertEquals(37L, restored.lastSimulationTickAt, number)
            assertEquals(stored, needs(restored))
            assertEquals("sa ajung acasa", restored.currentGoal)
            assertEquals("odihna", restored.plannedRoutineActivity)
            assertRuntimeTail(restored)
        }
    }

    @Test
    fun `wrong or missing tick types preserve prior time and do not discard memory or anchors`() {
        val values: List<JsonElement?> = listOf(null, JsonNull.INSTANCE, JsonPrimitive(true),
            JsonPrimitive(false), JsonPrimitive("1"), JsonPrimitive("invalid"), JsonArray(), JsonObject())
        for (previousTick in listOf(0L, 37L)) {
            for (value in values) {
                val profile = profile()
                val simulation = profile.getAsJsonObject("simulation")
                if (value == null) simulation.remove("last_simulation_tick_at")
                else simulation.add("last_simulation_tick_at", value)

                val restored = hydrate(profile, previousTickAt = previousTick)

                assertEquals(previousTick, restored.lastSimulationTickAt, value.toString())
                assertRuntimeTail(restored)
            }
        }
    }

    @Test
    fun `exact nonnegative tick values preserve zero large integers decimals and future timestamps`() {
        val numbers = listOf("0", "1", "1700000000000.0", "1.7e12", "9007199254740993",
            "9223372036854775807", "9223372036854775807.0")
        for (number in numbers) {
            val profile = profile()
            profile.getAsJsonObject("simulation").addProperty("last_simulation_tick_at", BigDecimal(number))

            val restored = hydrate(profile, previousTickAt = 37L)

            assertEquals(BigDecimal(number).longValueExact(), restored.lastSimulationTickAt, number)
            assertRuntimeTail(restored)
        }
    }

    @Test
    fun `invalid restored clock cannot invent elapsed needs on the first observed update`() {
        val values = listOf(JsonPrimitive(BigDecimal("18446744073709551617")),
            JsonPrimitive(BigDecimal("1.5")), JsonPrimitive("1"))
        val engine = DecisionEngine(plugin)
        for (value in values) {
            val profile = profile()
            profile.getAsJsonObject("simulation").add("last_simulation_tick_at", value)
            val restored = hydrate(profile)

            updateNeeds(engine, restored)

            assertEquals(stored, needs(restored), value.toString())
            assertTrue(restored.lastSimulationTickAt > 0L)
            assertRuntimeTail(restored)
        }

        val profile = profile()
        profile.getAsJsonObject("simulation").addProperty("last_simulation_tick_at", 1L)
        val validPast = hydrate(profile)
        updateNeeds(engine, validPast)
        assertEquals(stored.getValue("hunger_level") - 6, validPast.hungerLevel)
        assertRuntimeTail(validPast)
    }

    @Test
    fun `wrong or missing intention fields preserve prior text without coercing other primitives`() {
        val values: List<JsonElement?> = listOf(null, JsonNull.INSTANCE, JsonPrimitive(true),
            JsonPrimitive(false), JsonPrimitive(20), JsonPrimitive(BigDecimal("1e10000")),
            JsonArray(), JsonObject())
        for (key in listOf("current_goal", "planned_routine_activity")) {
            for (value in values) {
                val profile = profile()
                val simulation = profile.getAsJsonObject("simulation")
                if (value == null) simulation.remove(key) else simulation.add(key, value)

                val restored = hydrate(profile, previousGoal = "obiectiv anterior", previousRoutine = "rutina anterioara")

                assertEquals(if (key == "current_goal") "obiectiv anterior" else "sa ajung acasa", restored.currentGoal)
                assertEquals(if (key == "planned_routine_activity") "rutina anterioara" else "odihna",
                    restored.plannedRoutineActivity)
                assertEquals(stored, needs(restored))
                assertRuntimeTail(restored)
            }
        }
    }

    @Test
    fun `string intentions round trip exactly including empty and whitespace values`() {
        for (value in listOf("", "  ", "sa ajung acasa", "\u00eemi ajut vecinul\nmaine")) {
            val profile = profile()
            profile.getAsJsonObject("simulation").apply {
                addProperty("current_goal", value)
                addProperty("planned_routine_activity", value)
            }

            val restored = hydrate(profile, previousGoal = "anterior", previousRoutine = "anterioara")

            assertEquals(value, restored.currentGoal)
            assertEquals(value, restored.plannedRoutineActivity)
            assertRuntimeTail(restored)
        }
    }

    @Test
    fun `a valid future profile timestamp rebases without changing needs at the first update`() {
        val profile = profile()
        profile.getAsJsonObject("simulation").addProperty("last_simulation_tick_at", Long.MAX_VALUE)
        val restored = hydrate(profile)
        assertEquals(Long.MAX_VALUE, restored.lastSimulationTickAt)

        updateNeeds(DecisionEngine(plugin), restored)

        assertEquals(stored, needs(restored))
        assertTrue(restored.lastSimulationTickAt in 1 until Long.MAX_VALUE)
        assertRuntimeTail(restored)
    }

    private fun profile(): JsonObject {
        val source = AINPC(null).apply {
            hungerLevel = stored.getValue("hunger_level")
            energyLevel = stored.getValue("energy_level")
            socialNeedLevel = stored.getValue("social_need_level")
            comfortLevel = stored.getValue("comfort_level")
            safetyLevel = stored.getValue("safety_level")
            currentGoal = "sa ajung acasa"
            plannedRoutineActivity = "odihna"
            socialExperience.record(receipt)
            homeAnchor = home
        }
        return gson.fromJson(buildProfileData(source, gson), JsonObject::class.java)
    }

    private fun hydrate(
        profile: JsonObject, previousTickAt: Long = 0L, previousGoal: String = "", previousRoutine: String = "",
    ): AINPC {
        val restored = AINPC(null).apply {
            profileDataJson = gson.toJson(profile)
            lastSimulationTickAt = previousTickAt
            currentGoal = previousGoal
            plannedRoutineActivity = previousRoutine
        }
        val manager = unsafe.allocateInstance(NPCManager::class.java) as NPCManager
        NPCManager::class.java.getDeclaredField("plugin").apply { isAccessible = true }.set(manager, plugin)
        NPCManager::class.java.getDeclaredField("gson").apply { isAccessible = true }.set(manager, gson)
        NPCManager::class.java.getDeclaredMethod("hydrateProfileRuntimeData", AINPC::class.java)
            .apply { isAccessible = true }.invoke(manager, restored)
        return restored
    }

    private fun updateNeeds(engine: DecisionEngine, npc: AINPC) {
        DecisionEngine::class.java.getDeclaredMethod("updateNeeds", AINPC::class.java, NPCContext::class.java)
            .apply { isAccessible = true }.invoke(engine, npc, npc.context)
    }

    private fun assertRuntimeTail(npc: AINPC) {
        assertEquals(listOf(receipt), npc.socialExperience.snapshot())
        assertHome(npc)
    }

    private fun assertHome(npc: AINPC) {
        val restored = requireNotNull(npc.homeAnchor)
        assertEquals(home.type(), restored.type())
        assertEquals(home.label(), restored.label())
        assertEquals(home.worldName(), restored.worldName())
        assertEquals(home.x(), restored.x())
        assertEquals(home.y(), restored.y())
        assertEquals(home.z(), restored.z())
    }

    private fun needs(npc: AINPC): Map<String, Int> = linkedMapOf(
        "hunger_level" to npc.hungerLevel,
        "energy_level" to npc.energyLevel,
        "social_need_level" to npc.socialNeedLevel,
        "comfort_level" to npc.comfortLevel,
        "safety_level" to npc.safetyLevel,
    )
}
