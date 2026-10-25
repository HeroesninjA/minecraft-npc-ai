package ro.ainpc.managers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NpcActionExperience
import java.math.BigDecimal

internal object NpcActionExperienceCodec {
    private const val VERSION = 1L
    private const val MAX_OBSERVATIONS = 32

    fun write(memory: NpcActionExperience): JsonObject = JsonObject().apply {
        addProperty("version", VERSION)
        val observations = JsonArray()
        memory.snapshot().takeLast(MAX_OBSERVATIONS).forEach { observation ->
            observations.add(JsonObject().apply {
                addProperty("action", observation.action.name)
                addProperty("context_key", observation.contextKey)
                addProperty("need_delta", observation.needDelta)
                addProperty("effect", observation.effect)
                addProperty("observed_at", observation.observedAt)
            })
        }
        add("observations", observations)
    }

    fun read(value: JsonElement?): List<NpcActionExperience.Observation> {
        val memory = value?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()
        if (exactLong(memory.get("version")) != VERSION) return emptyList()
        val observations = memory.get("observations")?.takeIf { it.isJsonArray }?.asJsonArray
            ?: return emptyList()
        val parsed = buildList {
            for (index in maxOf(0, observations.size() - MAX_OBSERVATIONS) until observations.size()) {
                readObservation(observations[index])?.let(::add)
            }
        }
        return NpcActionExperience().apply { restore(parsed) }.snapshot()
    }

    private fun readObservation(value: JsonElement): NpcActionExperience.Observation? {
        val observation = value.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val actionName = stringField(observation.get("action")) ?: return null
        val action = NPCAction.entries.firstOrNull { it.name == actionName } ?: return null
        val contextKey = stringField(observation.get("context_key")) ?: return null
        val needDelta = exactLong(observation.get("need_delta"))
            ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt() ?: return null
        val effect = numberField(observation.get("effect")) ?: return null
        val observedAt = exactLong(observation.get("observed_at")) ?: return null
        return NpcActionExperience.Observation(action, contextKey, needDelta, effect, observedAt)
    }

    private fun stringField(value: JsonElement?): String? =
        value?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun exactLong(value: JsonElement?): Long? {
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) return null
        return try {
            BigDecimal(value.asString).longValueExact()
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun numberField(value: JsonElement?): Double? {
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) return null
        return try {
            value.asDouble.takeIf { it.isFinite() }
        } catch (_: RuntimeException) {
            null
        }
    }
}
