package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.IOException
import java.util.Locale

object OpenAITextSupport {
    @JvmStatic
    fun formatLevel(value: Double): String {
        if (value > 0.8) return "foarte ridicat"
        if (value > 0.6) return "ridicat"
        if (value > 0.4) return "mediu"
        if (value > 0.2) return "scazut"
        return "foarte scazut"
    }

    @JvmStatic
    fun containsAny(text: String, vararg values: String): Boolean {
        for (value in values) {
            if (text.contains(value)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun abbreviate(text: String?, maxLength: Int): String {
        if (text == null) {
            return ""
        }

        val normalized = text.replace('\n', ' ').replace('\r', ' ').trim()
        if (normalized.length <= maxLength) {
            return normalized
        }
        return normalized.substring(0, maxOf(0, maxLength - 3)) + "..."
    }

    @JvmStatic
    fun valueOrUnknown(value: String?): String {
        return if (value.isNullOrBlank()) "necunoscut" else value
    }

    @JvmStatic
    fun capitalizeSentence(text: String?): String {
        if (text.isNullOrBlank()) {
            return ""
        }

        val trimmed = text.trim()
        return Character.toUpperCase(trimmed[0]) + trimmed.substring(1)
    }

    @JvmStatic
    fun joinTraits(traitIds: List<String>?): String {
        return if (traitIds.isNullOrEmpty()) "niciun trait persistent" else traitIds.joinToString(", ")
    }

    @JvmStatic
    fun describeMemoryImpact(value: Double): String {
        if (value >= 0.6) {
            return "foarte pozitiv"
        }
        if (value >= 0.2) {
            return "pozitiv"
        }
        if (value <= -0.6) {
            return "foarte negativ"
        }
        if (value <= -0.2) {
            return "negativ"
        }
        return "neutru"
    }

    @JvmStatic
    fun stripSpeakerPrefix(response: String, expectedSpeakerName: String?): String {
        val colonIndex = response.indexOf(':')
        if (colonIndex <= 0 || colonIndex > 40) {
            return response
        }

        val prefix = response.substring(0, colonIndex)
            .replace("\"", "")
            .replace("[", "")
            .replace("]", "")
            .trim()

        if (!expectedSpeakerName.isNullOrBlank() && prefix.equals(expectedSpeakerName, ignoreCase = true)) {
            return response.substring(colonIndex + 1).trim()
        }

        if (prefix.equals("npc", ignoreCase = true) || prefix.equals("villager", ignoreCase = true)) {
            return response.substring(colonIndex + 1).trim()
        }

        return response
    }

    @JvmStatic
    fun analyzeSentimentFast(message: String?): String {
        val normalized = message?.lowercase()?.trim().orEmpty()
        if (normalized.isBlank()) {
            return "neutral"
        }

        if (containsAny(normalized, "te omor", "te bat", "mori", "kill", "distrug", "iti rup")) {
            return "threat"
        }

        if (containsAny(normalized, "prost", "idiot", "tampit", "urat", "fraier")) {
            return "insult"
        }

        if (containsAny(normalized, "multumesc", "mersi", "apreciez", "respect")) {
            return "compliment"
        }

        if (containsAny(normalized, "salut", "buna", "hei", "servus", "noroc")) {
            return "greeting"
        }

        if (containsAny(normalized, "bravo", "super", "grozav", "minunat", "frumos", "bun")) {
            return "positive"
        }

        if (containsAny(normalized, "rau", "nasol", "groaznic", "suparat", "trist", "urat rau")) {
            return "negative"
        }

        if (normalized.endsWith("?") || containsAny(normalized, "ce ", "cum ", "unde ", "cand ", "de ce", "cine ")) {
            return "question"
        }

        return "neutral"
    }

    @JvmStatic
    fun pickResponse(npcUuid: java.util.UUID, messageKey: String?, responses: Array<String>): String {
        val key = messageKey ?: ""
        val hash = 31 * npcUuid.hashCode() + key.hashCode()
        val index = Math.floorMod(hash, responses.size)
        return responses[index]
    }

    @JvmStatic
    fun nanosToMillis(startedAt: Long): Long {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
    }

    @JvmStatic
    fun sanitizeBaseUrl(configuredBaseUrl: String?): String {
        val envBaseUrl = System.getenv("OPENAI_BASE_URL")
        var value = if (configuredBaseUrl.isNullOrBlank()) {
            if (envBaseUrl.isNullOrBlank()) "https://api.openai.com/v1" else envBaseUrl.trim()
        } else {
            configuredBaseUrl.trim()
        }

        if (!value.contains("://")) {
            value = "https://$value"
        }

        while (value.endsWith("/")) {
            value = value.substring(0, value.length - 1)
        }

        return value
    }

    @JvmStatic
    fun sanitizeSecret(configuredValue: String?, envValue: String?): String {
        val value = if (configuredValue.isNullOrBlank()) envValue else configuredValue
        return value?.trim().orEmpty()
    }

    @JvmStatic
    fun cleanResponse(response: String, expectedSpeakerName: String?): String {
        var cleaned = response.replace(Regex("^\"|\"$"), "").trim()
        cleaned = stripSpeakerPrefix(cleaned, expectedSpeakerName)

        if (cleaned.length > 200) {
            val lastPeriod = cleaned.lastIndexOf(".", 200)
            cleaned = if (lastPeriod > 50) {
                cleaned.substring(0, lastPeriod + 1)
            } else {
                cleaned.substring(0, 200) + "..."
            }
        }

        return cleaned
    }

    @JvmStatic
    fun compactThrowableMessage(throwable: Throwable?): String {
        if (throwable == null) {
            return "necunoscut"
        }
        val message = throwable.message
        if (message.isNullOrBlank()) {
            return throwable.javaClass.simpleName
        }
        return throwable.javaClass.simpleName + ": " + message.trim()
    }

    @JvmStatic
    fun compactExceptionMessage(throwable: Throwable?): String {
        var current = throwable
        val parts = ArrayList<String>()
        var depth = 0
        while (current != null && depth < 4) {
            parts.add(compactThrowableMessage(current))
            current = current.cause
            depth++
        }
        return if (parts.isEmpty()) "necunoscut" else parts.joinToString(" -> ")
    }

    @JvmStatic
    fun isReadTimeout(throwable: Throwable?): Boolean {
        var current = throwable
        while (current != null) {
            if (current is java.net.SocketTimeoutException) {
                return true
            }
            current = current.cause
        }
        return false
    }

    @JvmStatic
    fun shouldEnterOfflineBackoff(throwable: Throwable?): Boolean {
        var current = throwable
        while (current != null) {
            if (current is java.net.ConnectException
                || current is java.net.NoRouteToHostException
                || current is java.net.UnknownHostException
                || current is java.net.PortUnreachableException
                || current is java.net.SocketException
                || current is java.net.SocketTimeoutException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    @JvmStatic
    fun safeJsonString(element: JsonElement?): String {
        if (element == null || element.isJsonNull) {
            return ""
        }
        return try {
            element.asString
        } catch (_: Exception) {
            element.toString()
        }
    }

    @JvmStatic
    fun extractOpenAIErrorMessage(gson: Gson, responseBody: String?): String {
        if (responseBody.isNullOrBlank()) {
            return ""
        }

        return try {
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            if (json == null) {
                abbreviate(responseBody, 200)
            } else {
                val errorElement = json.get("error")
                if (errorElement == null || !errorElement.isJsonObject) {
                    abbreviate(responseBody, 200)
                } else {
                    val error = errorElement.asJsonObject
                    val code = safeJsonString(error.get("code"))
                    val type = safeJsonString(error.get("type"))
                    val message = safeJsonString(error.get("message"))
                    val parts = ArrayList<String>()
                    if (code.isNotBlank()) {
                        parts.add(code)
                    } else if (type.isNotBlank()) {
                        parts.add(type)
                    }
                    if (message.isNotBlank()) {
                        parts.add(message)
                    }
                    if (parts.isEmpty()) abbreviate(responseBody, 200) else parts.joinToString(": ")
                }
            }
        } catch (_: Exception) {
            abbreviate(responseBody, 200)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun extractGeneratedText(gson: Gson, responseBody: String?, expectedSpeakerName: String?): String {
        if (responseBody.isNullOrBlank()) {
            throw IOException("OpenAI a returnat un raspuns gol.")
        }

        val jsonResponse = try {
            gson.fromJson(responseBody, JsonObject::class.java)
        } catch (e: Exception) {
            throw IOException("OpenAI a returnat JSON invalid: " + compactExceptionMessage(e), e)
        } ?: throw IOException("OpenAI a returnat un JSON gol.")

        val errorElement = jsonResponse.get("error")
        if (errorElement != null && !errorElement.isJsonNull) {
            throw IOException("OpenAI a raportat eroare: " + extractOpenAIErrorMessage(gson, responseBody))
        }

        val outputTextElement = jsonResponse.get("output_text")
        if (outputTextElement != null && outputTextElement.isJsonPrimitive) {
            val directText = cleanResponse(outputTextElement.asString, expectedSpeakerName)
            if (directText.isNotBlank()) {
                return directText
            }
        }

        val combined = StringBuilder()
        val outputElement = jsonResponse.get("output")
        if (outputElement != null && outputElement.isJsonArray) {
            val outputs: JsonArray = outputElement.asJsonArray
            for (itemElement in outputs) {
                if (!itemElement.isJsonObject) {
                    continue
                }

                val item = itemElement.asJsonObject
                if (!"message".equals(safeJsonString(item.get("type")), ignoreCase = true)) {
                    continue
                }

                val contentElement = item.get("content")
                if (contentElement == null || !contentElement.isJsonArray) {
                    continue
                }

                for (contentItemElement in contentElement.asJsonArray) {
                    if (!contentItemElement.isJsonObject) {
                        continue
                    }

                    val contentItem = contentItemElement.asJsonObject
                    val type = safeJsonString(contentItem.get("type"))
                    if (!"output_text".equals(type, ignoreCase = true) && !"text".equals(type, ignoreCase = true)) {
                        continue
                    }

                    val text = safeJsonString(contentItem.get("text")).trim()
                    if (text.isBlank()) {
                        continue
                    }

                    if (combined.isNotEmpty()) {
                        combined.append('\n')
                    }
                    combined.append(text)
                }
            }
        }

        val generatedText = cleanResponse(combined.toString(), expectedSpeakerName)
        if (generatedText.isBlank()) {
            throw IOException("OpenAI nu a returnat text util in campul output.")
        }

        return generatedText
    }

    @JvmStatic
    fun generateFallbackResponse(snapshot: PromptSnapshot): String {
        val normalized = snapshot.playerMessage().lowercase(Locale.ROOT).trim()
        val occupation = if (snapshot.occupation().isNotBlank()) snapshot.occupation() else "locuitor"
        val factResponse = NpcFactResolver.resolve(snapshot.playerMessage(), NpcFactResolver.NpcFacts(
            snapshot.npcName(),
            snapshot.occupation(),
            snapshot.emotionShortDescription(),
            snapshot.currentState(),
            snapshot.currentActivity(),
            snapshot.locationDescription()
        )).orElse(null)

        if (factResponse != null) {
            return factResponse
        }

        if (containsAny(normalized, "salut", "buna", "hei", "servus", "noroc")) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Salut. Eu sunt ${snapshot.npcName()}.",
                "Buna. Cu ce te pot ajuta?",
                "Salut, calatorule. Ce vrei sa afli?"
            ))
        }

        if (containsAny(normalized, "multumesc", "mersi", "apreciez")) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Cu placere.",
                "N-ai pentru ce.",
                "Ma bucur ca ti-am fost de folos."
            ))
        }

        if (containsAny(normalized, "prost", "idiot", "tampit", "urat", "fraier")) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Vorbeste cu respect daca vrei raspunsuri.",
                "Nu-mi place tonul tau.",
                "Daca vii cu insulte, n-avem ce discuta."
            ))
        }

        if (containsAny(normalized, "cumpara", "vinde", "pret", "marfa", "schimb")) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Poate te pot ajuta, depinde ce cauti.",
                "Daca e vorba de $occupation, stiu cate ceva.",
                "Spune clar ce iti trebuie si vedem."
            ))
        }

        if (!snapshot.directAddress() && containsAny(normalized, "hmm", "ok", "bine", "da", "nu")) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Te aud.",
                "Daca imi vorbesti mie, spune mai clar.",
                "Sunt aici, daca voiai un raspuns."
            ))
        }

        if (normalized.endsWith("?") || containsAny(normalized, "ce ", "cum ", "unde ", "cand ", "de ce", "cine ")) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Nu stiu tot, dar pot incerca sa te ajut.",
                "Din cate stiu eu, raspunsul tine de meseria mea de $occupation.",
                "Intrebare buna. Spune-mi mai exact ce te intereseaza."
            ))
        }

        val responses = when (snapshot.dominantEmotion()) {
            "happiness" -> arrayOf(
                "Ce bucurie sa te vad!",
                "Ma bucur sa stam de vorba.",
                "Hm, suna interesant ce spui."
            )
            "sadness" -> arrayOf(
                "Nu e cea mai buna zi pentru mine.",
                "Ma simt cam trist astazi.",
                "Imi pare rau, nu sunt in apele mele."
            )
            "anger" -> arrayOf(
                "Nu am chef de vorba acum.",
                "Spune repede ce vrei.",
                "Lasa-ma sa-mi vad de treaba."
            )
            "fear" -> arrayOf(
                "M-ai speriat putin.",
                "Nu stiu daca e bine sa vorbim aici.",
                "Vorbeste mai incet. Nu-mi place locul asta."
            )
            else -> arrayOf(
                "Hmm, inteleg.",
                "Asa e.",
                "Te ascult."
            )
        }

        return pickResponse(snapshot.npcUuid(), normalized, responses)
    }
}
