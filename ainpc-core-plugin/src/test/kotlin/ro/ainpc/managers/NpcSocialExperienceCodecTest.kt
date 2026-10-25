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
import ro.ainpc.npc.NpcSocialExperience
import java.math.BigDecimal
import java.util.UUID

class NpcSocialExperienceCodecTest {
    private val receipt = NpcSocialExperience.Receipt(
        UUID(0L, 1L), UUID.fromString("01234567-89ab-cdef-8123-456789abcdef"),
        "Helper", "minecraft:apple", 1, 10, 26, 1_700_000_000_001L,
    )

    @Test
    fun `round trip preserves exact helper UUIDs despite identical names and exact long timestamps`() {
        val receipts = listOf(
            receipt,
            receipt.copy(
                id = UUID(0L, 2L),
                helperUuid = UUID.fromString("fedcba98-7654-3210-8123-456789abcdef"),
                recordedAt = 9_007_199_254_740_993L,
            ),
        )
        val memory = NpcSocialExperience().apply { restore(receipts) }
        val serialized = NpcSocialExperienceCodec.write(memory)
        val reparsed = Gson().fromJson(Gson().toJson(serialized), JsonObject::class.java)

        assertEquals(1, serialized.get("version").asInt)
        assertEquals(receipts, NpcSocialExperienceCodec.read(reparsed))
        assertEquals(receipts.map { it.helperUuid }, NpcSocialExperienceCodec.read(reparsed).map { it.helperUuid })
        assertEquals(receipts, memory.snapshot())
    }

    @Test
    fun `missing wrong type and unsupported version restore no receipts`() {
        val invalid = listOf(
            null,
            JsonNull.INSTANCE,
            JsonArray(),
            JsonPrimitive("memory"),
            JsonObject(),
            envelope(receiptJson()).apply { addProperty("version", "1") },
            envelope(receiptJson()).apply { addProperty("version", true) },
            envelope(receiptJson()).apply { addProperty("version", 1.5) },
            envelope(receiptJson()).apply { addProperty("version", 2) },
            envelope(receiptJson()).apply { add("receipts", JsonObject()) },
            envelope(receiptJson()).apply { remove("receipts") },
        )

        invalid.forEach { value ->
            assertTrue(NpcSocialExperienceCodec.read(value).isEmpty(), value.toString())
        }
    }

    @Test
    fun `malformed entries cannot create help evidence or prevent valid receipts from loading`() {
        val invalid = listOf(
            JsonNull.INSTANCE,
            JsonArray(),
            JsonPrimitive("entry"),
            receiptJson().apply { remove("id") },
            receiptJson().apply { addProperty("id", "1-1-1-1-1") },
            receiptJson().apply { addProperty("id", "not-a-uuid") },
            receiptJson().apply { addProperty("helper_uuid", 1) },
            receiptJson().apply { addProperty("helper_uuid", "1-1-1-1-1") },
            receiptJson().apply { addProperty("helper_uuid", "not-a-uuid") },
            receiptJson().apply { addProperty("helper_name", true) },
            receiptJson().apply { addProperty("helper_name", " ") },
            receiptJson().apply { addProperty("helper_name", "a\nb") },
            receiptJson().apply { addProperty("helper_name", "x".repeat(65)) },
            receiptJson().apply { addProperty("item_key", "minecraft:stone") },
            receiptJson().apply { addProperty("item_key", false) },
            receiptJson().apply { addProperty("amount", "1") },
            receiptJson().apply { addProperty("amount", true) },
            receiptJson().apply { addProperty("amount", 1.5) },
            receiptJson().apply { addProperty("amount", 0) },
            receiptJson().apply { addProperty("amount", 2) },
            receiptJson().apply { addProperty("amount", BigDecimal("4294967297")) },
            receiptJson().apply { addProperty("amount", Double.NaN) },
            receiptJson().apply { addProperty("before_hunger", "10") },
            receiptJson().apply { addProperty("before_hunger", 10.5) },
            receiptJson().apply { addProperty("before_hunger", -1) },
            receiptJson().apply { addProperty("before_hunger", 35) },
            receiptJson().apply { addProperty("after_hunger", "26") },
            receiptJson().apply { addProperty("after_hunger", 26.5) },
            receiptJson().apply { addProperty("after_hunger", 10) },
            receiptJson().apply { addProperty("after_hunger", 9) },
            receiptJson().apply { addProperty("after_hunger", 101) },
            receiptJson().apply { addProperty("recorded_at", "1700000000001") },
            receiptJson().apply { addProperty("recorded_at", false) },
            receiptJson().apply { addProperty("recorded_at", BigDecimal("1700000000001.5")) },
            receiptJson().apply { addProperty("recorded_at", BigDecimal("9223372036854775808")) },
            receiptJson().apply { addProperty("recorded_at", Double.POSITIVE_INFINITY) },
            receiptJson().apply { addProperty("recorded_at", 0) },
            receiptJson().apply { addProperty("recorded_at", -1) },
        )

        invalid.forEach { value ->
            assertEquals(listOf(receipt), NpcSocialExperienceCodec.read(envelope(value, receiptJson())), value.toString())
        }
    }

    @Test
    fun `canonical UUID case and integral decimal tokens preserve their values`() {
        val serialized = receiptJson().apply {
            addProperty("helper_uuid", receipt.helperUuid.toString().uppercase())
            addProperty("amount", BigDecimal("1.0"))
            addProperty("before_hunger", BigDecimal("10.0"))
            addProperty("after_hunger", BigDecimal("26.0"))
            addProperty("recorded_at", BigDecimal("1700000000001.0"))
        }

        assertEquals(listOf(receipt), NpcSocialExperienceCodec.read(envelope(serialized)))
    }

    @Test
    fun `duplicate receipt IDs retain the newest valid receipt`() {
        val newer = receipt.copy(recordedAt = receipt.recordedAt + 1L, afterHunger = 27)
        val invalidNewer = receiptJson(newer).apply { addProperty("after_hunger", 10) }

        assertEquals(
            listOf(newer),
            NpcSocialExperienceCodec.read(envelope(receiptJson(newer), receiptJson(), invalidNewer)),
        )
    }

    @Test
    fun `reads are bounded to the newest 32 array entries`() {
        val entries = (1L..48L).map { timestamp ->
            receiptJson(receipt.copy(id = UUID(0L, timestamp), recordedAt = timestamp))
        }

        val loaded = NpcSocialExperienceCodec.read(envelope(*entries.toTypedArray()))

        assertEquals((17L..48L).toList(), loaded.map { it.recordedAt })
        assertEquals((17L..48L).map { UUID(0L, it) }, loaded.map { it.id })
    }

    @Test
    fun `profile rebuild retains social receipts alongside action experience needs narrative and anchors`() {
        val gson = Gson()
        val actionObservation = NpcActionExperience.Observation(NPCAction.EAT, "home:world", 8, 8.0, receipt.recordedAt)
        val npc = AINPC(null).apply {
            hungerLevel = 26
            currentGoal = "sa ajung acasa"
            homeAnchor = AINPC.OwnedLocation("home", "Casa", "world", 1.0, 64.0, 2.0)
            profileDataJson = "{\"narrative\":{\"existing\":\"yes\"}}"
            actionExperience.restore(listOf(actionObservation))
            socialExperience.restore(listOf(receipt))
        }

        val profile = gson.fromJson(buildProfileData(npc, gson), JsonObject::class.java)
        val simulation = profile.getAsJsonObject("simulation")
        val restored = AINPC(null).apply {
            socialExperience.restore(NpcSocialExperienceCodec.read(simulation.get("social_experience")))
        }
        val rebuilt = gson.fromJson(buildProfileData(restored, gson), JsonObject::class.java)

        assertEquals(26, simulation.get("hunger_level").asInt)
        assertEquals("sa ajung acasa", simulation.get("current_goal").asString)
        assertEquals("yes", profile.getAsJsonObject("narrative").get("existing").asString)
        assertEquals("Casa", profile.getAsJsonObject("owned_locations").getAsJsonObject("home").get("label").asString)
        assertEquals(listOf(actionObservation), NpcActionExperienceCodec.read(simulation.get("action_experience")))
        assertEquals(listOf(receipt), restored.socialExperience.snapshot())
        assertEquals(
            listOf(receipt),
            NpcSocialExperienceCodec.read(rebuilt.getAsJsonObject("simulation").get("social_experience")),
        )
    }

    private fun envelope(vararg receipts: JsonElement): JsonObject = JsonObject().apply {
        addProperty("version", 1)
        add("receipts", JsonArray().apply { receipts.forEach(::add) })
    }

    private fun receiptJson(receipt: NpcSocialExperience.Receipt = this.receipt): JsonObject = JsonObject().apply {
        addProperty("id", receipt.id.toString())
        addProperty("helper_uuid", receipt.helperUuid.toString())
        addProperty("helper_name", receipt.helperName)
        addProperty("item_key", receipt.itemKey)
        addProperty("amount", receipt.amount)
        addProperty("before_hunger", receipt.beforeHunger)
        addProperty("after_hunger", receipt.afterHunger)
        addProperty("recorded_at", receipt.recordedAt)
    }
}
