package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException
import java.io.StringReader

internal object OpenAIJsonEnvelope {
    private const val INVALID_JSON_MESSAGE = "OpenAI a returnat JSON invalid."

    fun parseObject(gson: Gson, responseBody: String): JsonObject? = try {
        validateUniqueMembers(responseBody)
        gson.newBuilder().setStrictness(Strictness.STRICT).create()
            .fromJson(responseBody, JsonObject::class.java)
    } catch (_: JsonParseException) {
        throw IOException(INVALID_JSON_MESSAGE)
    } catch (_: IOException) {
        throw IOException(INVALID_JSON_MESSAGE)
    }

    private fun validateUniqueMembers(responseBody: String) {
        JsonReader(StringReader(responseBody)).use { reader ->
            reader.strictness = Strictness.STRICT
            val objectMembers = ArrayDeque<MutableSet<String>>()
            while (true) {
                when (reader.peek()) {
                    JsonToken.BEGIN_OBJECT -> {
                        reader.beginObject()
                        objectMembers.addLast(HashSet())
                    }
                    JsonToken.END_OBJECT -> {
                        reader.endObject()
                        objectMembers.removeLast()
                    }
                    JsonToken.BEGIN_ARRAY -> reader.beginArray()
                    JsonToken.END_ARRAY -> reader.endArray()
                    JsonToken.NAME -> {
                        if (!objectMembers.last().add(reader.nextName())) {
                            throw IOException(INVALID_JSON_MESSAGE)
                        }
                    }
                    JsonToken.END_DOCUMENT -> return
                    else -> reader.skipValue()
                }
            }
        }
    }
}
