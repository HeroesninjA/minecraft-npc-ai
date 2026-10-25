package ro.ainpc.managers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import ro.ainpc.npc.NpcSocialExperience
import java.math.BigDecimal
import java.util.UUID

internal object NpcSocialExperienceCodec {
    private const val VERSION = 1L
    private const val MAX_RECEIPTS = 32

    fun write(memory: NpcSocialExperience): JsonObject = JsonObject().apply {
        addProperty("version", VERSION)
        val receipts = JsonArray()
        memory.snapshot().takeLast(MAX_RECEIPTS).forEach { receipt ->
            receipts.add(JsonObject().apply {
                addProperty("id", receipt.id.toString())
                addProperty("helper_uuid", receipt.helperUuid.toString())
                addProperty("helper_name", receipt.helperName)
                addProperty("item_key", receipt.itemKey)
                addProperty("amount", receipt.amount)
                addProperty("before_hunger", receipt.beforeHunger)
                addProperty("after_hunger", receipt.afterHunger)
                addProperty("recorded_at", receipt.recordedAt)
            })
        }
        add("receipts", receipts)
    }

    fun read(value: JsonElement?): List<NpcSocialExperience.Receipt> {
        val memory = value?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()
        if (exactLong(memory.get("version")) != VERSION) return emptyList()
        val receipts = memory.get("receipts")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        val parsed = buildList {
            for (index in maxOf(0, receipts.size() - MAX_RECEIPTS) until receipts.size()) {
                readReceipt(receipts[index])?.let(::add)
            }
        }
        return NpcSocialExperience().apply { restore(parsed) }.snapshot()
    }

    private fun readReceipt(value: JsonElement): NpcSocialExperience.Receipt? {
        val receipt = value.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val id = uuidField(receipt.get("id")) ?: return null
        val helperUuid = uuidField(receipt.get("helper_uuid")) ?: return null
        val helperName = stringField(receipt.get("helper_name")) ?: return null
        val itemKey = stringField(receipt.get("item_key")) ?: return null
        val amount = intField(receipt.get("amount")) ?: return null
        val beforeHunger = intField(receipt.get("before_hunger")) ?: return null
        val afterHunger = intField(receipt.get("after_hunger")) ?: return null
        val recordedAt = exactLong(receipt.get("recorded_at")) ?: return null
        return NpcSocialExperience.Receipt(id, helperUuid, helperName, itemKey, amount, beforeHunger, afterHunger, recordedAt)
    }

    private fun stringField(value: JsonElement?): String? =
        value?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun uuidField(value: JsonElement?): UUID? {
        val raw = stringField(value) ?: return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
            ?.takeIf { it.toString().equals(raw, ignoreCase = true) }
    }

    private fun intField(value: JsonElement?): Int? = exactLong(value)
        ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()

    private fun exactLong(value: JsonElement?): Long? {
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) return null
        return try {
            BigDecimal(value.asString).longValueExact()
        } catch (_: RuntimeException) {
            null
        }
    }
}
