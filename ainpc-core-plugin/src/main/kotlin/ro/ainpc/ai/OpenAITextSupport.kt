package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import ro.ainpc.context.SensitiveDataRedactor
import java.io.IOException
import java.util.UUID

object OpenAITextSupport {
    private const val MAX_FALLBACK_ROTATIONS = 512
    private const val MAX_CLEAN_RESPONSE_LENGTH = 200
    private const val MIN_COMPLETE_SENTENCE_LENGTH = 50
    private val promptProfileGson = Gson()
    private val sentenceEnding = Regex("[.!?](?=\\s|$)")
    private val fallbackResponseIndices = object : LinkedHashMap<Pair<UUID, String>, Int>(MAX_FALLBACK_ROTATIONS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<UUID, String>, Int>?): Boolean =
            size > MAX_FALLBACK_ROTATIONS
    }

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

    internal fun profileForPrompt(profileData: String): String = try {
        val profile = promptProfileGson.fromJson(profileData, JsonObject::class.java)
        if (profile == null) {
            "{}"
        } else {
            profile.get("simulation")?.takeIf { it.isJsonObject }?.asJsonObject?.remove("social_experience")
            promptProfileGson.toJson(profile)
        }
    } catch (_: RuntimeException) {
        "{}"
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
        val normalized = DialogueMessageText.normalize(message.orEmpty())
        if (normalized.isBlank()) {
            return "neutral"
        }

        if (DialogueMessageText.matches(normalized, "te omor", "te bat", "mori", "kill", "distrug", "iti rup")) {
            return "threat"
        }

        if (DialogueMessageText.matches(normalized, "prost", "idiot", "tampit", "urat", "fraier")) {
            return "insult"
        }

        if (DialogueMessageText.matches(normalized, "multumesc", "mersi", "apreciez", "respect")) {
            return "compliment"
        }

        if (DialogueMessageText.isSimpleGreeting(normalized)) {
            return "greeting"
        }

        if (DialogueMessageText.matches(normalized, "bravo", "super", "grozav", "minunat", "frumos", "bun")) {
            return "positive"
        }

        if (DialogueMessageText.matches(normalized, "rau", "nasol", "groaznic", "suparat", "trist", "urat rau")) {
            return "negative"
        }

        if (DialogueMessageText.isQuestion(normalized)) {
            return "question"
        }

        return "neutral"
    }

    @JvmStatic
    fun pickResponse(npcUuid: UUID, messageKey: String?, responses: Array<String>): String {
        require(responses.isNotEmpty())
        val key = npcUuid to (messageKey ?: "")
        synchronized(fallbackResponseIndices) {
            val previousIndex = fallbackResponseIndices[key]
            val index = if (previousIndex == null) {
                Math.floorMod(31 * npcUuid.hashCode() + key.second.hashCode(), responses.size)
            } else {
                (previousIndex + 1) % responses.size
            }
            fallbackResponseIndices[key] = index
            return responses[index]
        }
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
    fun redactDiagnosticText(text: String?, apiKey: String? = null): String {
        val secret = apiKey?.trim().orEmpty()
        val masked = if (secret.isEmpty()) text.orEmpty() else text.orEmpty().replace(secret, "<redacted>", ignoreCase = true)
        return SensitiveDataRedactor.redact(masked)
    }

    @JvmStatic
    fun cleanResponse(response: String, expectedSpeakerName: String?): String {
        var cleaned = response.replace(Regex("^\"|\"$"), "").trim()
        cleaned = stripSpeakerPrefix(cleaned, expectedSpeakerName)

        if (cleaned.length > MAX_CLEAN_RESPONSE_LENGTH) {
            val sentenceEnd = sentenceEnding.findAll(cleaned)
                .lastOrNull { it.range.last >= MIN_COMPLETE_SENTENCE_LENGTH &&
                    it.range.last < MAX_CLEAN_RESPONSE_LENGTH }?.range?.last
            cleaned = if (sentenceEnd != null) {
                cleaned.substring(0, sentenceEnd + 1)
            } else {
                val prefix = cleaned.substring(0, MAX_CLEAN_RESPONSE_LENGTH)
                val wordBoundary = prefix.indexOfLast(Char::isWhitespace)
                prefix.substring(0, if (wordBoundary > MIN_COMPLETE_SENTENCE_LENGTH) wordBoundary else MAX_CLEAN_RESPONSE_LENGTH)
                    .trimEnd() + "..."
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
            if (current is java.net.SocketTimeoutException
                || current is java.net.http.HttpTimeoutException
            ) {
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
                || current is java.net.http.HttpTimeoutException
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
        } catch (_: java.lang.IllegalStateException) {
            element.toString()
        }
    }

    private fun errorFieldString(element: JsonElement?): String? =
        element?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    @JvmStatic
    @JvmOverloads
    fun extractOpenAIErrorMessage(gson: Gson, responseBody: String?, apiKey: String? = null): String {
        if (responseBody.isNullOrBlank()) {
            return ""
        }

        fun fallbackExcerpt(): String = abbreviate(redactDiagnosticText(responseBody, apiKey), 200)

        return try {
            val json = OpenAIJsonEnvelope.parseObject(gson, responseBody)
            if (json == null) {
                fallbackExcerpt()
            } else {
                val errorElement = json.get("error")
                if (errorElement == null || !errorElement.isJsonObject) {
                    fallbackExcerpt()
                } else {
                    val error = errorElement.asJsonObject
                    val code = errorFieldString(error.get("code")).orEmpty()
                    val type = errorFieldString(error.get("type")).orEmpty()
                    val message = errorFieldString(error.get("message")).orEmpty()
                    val parts = ArrayList<String>()
                    if (code.isNotBlank()) {
                        parts.add(code)
                    } else if (type.isNotBlank()) {
                        parts.add(type)
                    }
                    if (message.isNotBlank()) {
                        parts.add(message)
                    }
                    if (parts.isEmpty()) fallbackExcerpt() else redactDiagnosticText(parts.joinToString(": "), apiKey)
                }
            }
        } catch (_: IOException) {
            fallbackExcerpt()
        }
    }

    @JvmStatic
    fun extractOpenAIErrorCode(gson: Gson, responseBody: String?): String {
        if (responseBody.isNullOrBlank()) return ""
        return try {
            val error = OpenAIJsonEnvelope.parseObject(gson, responseBody)
                ?.get("error")
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
            val codeElement = error?.get("code")
            val code = errorFieldString(codeElement)
            if (codeElement != null && !codeElement.isJsonNull && code == null) return ""
            if (!code.isNullOrBlank()) code else errorFieldString(error?.get("type")).orEmpty()
        } catch (_: IOException) {
            ""
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun extractGeneratedText(gson: Gson, responseBody: String?, expectedSpeakerName: String?,
                             preserveStructuredJson: Boolean = false): String =
        extractGeneratedText(gson, responseBody, expectedSpeakerName, preserveStructuredJson, null)

    @JvmStatic
    @Throws(IOException::class)
    fun extractGeneratedText(
        gson: Gson, responseBody: String?, expectedSpeakerName: String?, preserveStructuredJson: Boolean, apiKey: String?,
    ): String {
        if (responseBody.isNullOrBlank()) {
            throw IOException("OpenAI a returnat un raspuns gol.")
        }

        val jsonResponse = OpenAIJsonEnvelope.parseObject(gson, responseBody)
            ?: throw IOException("OpenAI a returnat un JSON gol.")

        val errorElement = jsonResponse.get("error")
        if (errorElement != null && !errorElement.isJsonNull) {
            throw IOException("OpenAI a raportat eroare: " + extractOpenAIErrorMessage(gson, responseBody, apiKey))
        }
        val responseStatus = jsonResponse.get("status")
        val responseCompleted = hasJsonStringValue(responseStatus, "completed")
        if ((preserveStructuredJson || responseStatus != null) && !responseCompleted) {
            throw IOException("OpenAI nu a finalizat raspunsul${if (preserveStructuredJson) " JSON structurat" else ""}.")
        }

        val combined = StringBuilder()
        var hasExplicitMessagePhase = false
        val outputElement = jsonResponse.get("output")
        if (outputElement != null) {
            if (!outputElement.isJsonArray) {
                throw IOException("OpenAI a returnat un camp output cu structura invalida.")
            }
            val outputs: JsonArray = outputElement.asJsonArray
            for (itemElement in outputs) {
                if (!itemElement.isJsonObject) {
                    throw IOException("OpenAI a returnat un item output care nu este obiect.")
                }

                val item = itemElement.asJsonObject
                if (!"message".equals(responseItemType(item.get("type")), ignoreCase = true)) {
                    continue
                }

                val messageStatus = item.get("status")
                if (messageStatus != null && !hasJsonStringValue(messageStatus, "completed")) {
                    throw IOException("OpenAI nu a finalizat un mesaj din raspuns.")
                }
                val messageRole = item.get("role")
                if (messageRole != null && !hasJsonStringValue(messageRole, "assistant")) {
                    throw IOException("OpenAI a returnat un mesaj cu rol invalid.")
                }

                val messagePhase = item.get("phase")
                if (messagePhase != null && !messagePhase.isJsonNull) {
                    if (!hasJsonStringValue(messagePhase, "commentary") && !hasJsonStringValue(messagePhase, "final_answer")) {
                        throw IOException("OpenAI a returnat un mesaj cu faza invalida.")
                    }
                    hasExplicitMessagePhase = true
                }
                val isCommentary = hasJsonStringValue(messagePhase, "commentary")

                val contentElement = item.get("content")
                if (contentElement == null || !contentElement.isJsonArray) {
                    throw IOException("OpenAI a returnat un mesaj cu structura content invalida.")
                }

                for (contentItemElement in contentElement.asJsonArray) {
                    if (!contentItemElement.isJsonObject) {
                        throw IOException("OpenAI a returnat un item content care nu este obiect.")
                    }

                    val contentItem = contentItemElement.asJsonObject
                    val type = responseItemType(contentItem.get("type"))
                    if ("refusal".equals(type, ignoreCase = true)) {
                        throw IOException("OpenAI a refuzat generarea raspunsului.")
                    }
                    if (!"output_text".equals(type, ignoreCase = true) && !"text".equals(type, ignoreCase = true)) {
                        continue
                    }

                    val text = generatedJsonText(contentItem.get("text")).trim()
                    if (text.isBlank() || isCommentary) {
                        continue
                    }

                    if (combined.isNotEmpty()) {
                        combined.append('\n')
                    }
                    combined.append(text)
                }
            }
        }

        val outputText = generatedJsonText(jsonResponse.get("output_text"))
        if (!hasExplicitMessagePhase) {
            val directText = if (preserveStructuredJson) outputText.trim() else cleanResponse(outputText, expectedSpeakerName)
            if (directText.isNotBlank()) {
                return directText
            }
        }

        val generatedText = if (preserveStructuredJson) combined.toString().trim()
            else cleanResponse(combined.toString(), expectedSpeakerName)
        if (generatedText.isBlank()) {
            throw IOException("OpenAI nu a returnat text util in campul output.")
        }

        return generatedText
    }

    private fun hasJsonStringValue(element: JsonElement?, expected: String): Boolean =
        element != null && element.isJsonPrimitive && element.asJsonPrimitive.isString && element.asString == expected

    private fun responseItemType(element: JsonElement?): String {
        if (element == null || !element.isJsonPrimitive || !element.asJsonPrimitive.isString || element.asString.isBlank()) {
            throw IOException("OpenAI a returnat un item fara tip string valid.")
        }
        return element.asString
    }

    private fun generatedJsonText(element: JsonElement?): String {
        if (element == null || element.isJsonNull) return ""
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isString) {
            throw IOException("OpenAI a returnat un camp text cu tip invalid.")
        }
        return element.asString
    }

    internal fun generateGroundedFallbackResponse(
        snapshot: PromptSnapshot,
        recentHistory: List<DialogHistory>,
        groundedFacts: String?,
    ): String = groundedFacts?.takeIf(String::isNotBlank) ?: generateFallbackResponse(snapshot, recentHistory)

    @JvmStatic
    @JvmOverloads
    fun generateFallbackResponse(snapshot: PromptSnapshot, recentHistory: List<DialogHistory> = emptyList()): String {
        PlayerConversationRecall.resolveForFallback(snapshot.playerMessage(), recentHistory)?.let { return it }
        val normalized = DialogueMessageText.normalize(snapshot.playerMessage())
        val occupation = if (snapshot.occupation().isNotBlank()) snapshot.occupation() else "locuitor"
        val environmentFact = EnvironmentFactResolver.resolve(snapshot.playerMessage(), snapshot.environmentContext(),
            snapshot.agentSnapshot?.perception?.observation)
        val factResponse = NpcFactResolver.resolve(snapshot.playerMessage(), NpcFactResolver.NpcFacts(
            snapshot.npcName(),
            snapshot.occupation(),
            snapshot.emotionShortDescription(),
            snapshot.currentState(),
            snapshot.currentActivity(),
            snapshot.locationDescription(),
            snapshot.plannedRoutineActivity,
            snapshot.personalityDescription(),
        )).orElse(null)

        if (environmentFact != null) {
            return listOfNotNull(factResponse, environmentFact).joinToString(" ")
        }
        val semanticFact = resolveSemanticFactFallback(snapshot, normalized)
        if (semanticFact != null) {
            return semanticFact
        }

        if (factResponse != null) {
            return factResponse
        }

        val continuationTemplates = PlayerConversationContinuation.templates(snapshot.playerMessage(), recentHistory)
        if (continuationTemplates != null) {
            return pickResponse(snapshot.npcUuid(), normalized, continuationTemplates.toTypedArray())
        }

        if (DialogueMessageText.matches(normalized, "multumesc", "mersi", "apreciez")) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Cu placere.",
                "N-ai pentru ce.",
                "Ma bucur ca ti-am fost de folos."
            ))
        }

        if (DialogueMessageText.matches(normalized, "prost", "idiot", "tampit", "urat", "fraier")) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Vorbeste cu respect daca vrei raspunsuri.",
                "Nu-mi place tonul tau.",
                "Daca vii cu insulte, n-avem ce discuta."
            ))
        }

        if (DialogueMessageText.matches(normalized, "cumpar", "cumpara", "vinde", "pret", "pretul", "preturi", "marfa", "schimb")) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Poate te pot ajuta, depinde ce cauti.",
                "Daca e vorba de $occupation, stiu cate ceva.",
                "Spune clar ce iti trebuie si vedem."
            ))
        }

        if (DialogueMessageText.isSimpleGreeting(normalized)) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Salut. Eu sunt ${snapshot.npcName()}.",
                "Buna. Cu ce te pot ajuta?",
                "Salut, calatorule. Ce vrei sa afli?"
            ))
        }

        if (!snapshot.directAddress() && Regex("(?:hmm|ok|bine|da|nu)[.!?]*").matches(normalized)) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Te aud.",
                "Daca imi vorbesti mie, spune mai clar.",
                "Sunt aici, daca voiai un raspuns."
            ))
        }

        if (DialogueMessageText.isQuestion(normalized)) {
            return pickResponse(snapshot.npcUuid(), normalized, arrayOf(
                "Nu stiu tot, dar pot incerca sa te ajut.",
                "Nu am destule informatii despre asta. Ce detalii mai ai?",
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

    private fun resolveSemanticFactFallback(snapshot: PromptSnapshot, normalizedMessage: String): String? {
        val context = snapshot.semanticWorldContext()
        if (context.isBlank()) {
            return null
        }

        val worldLoreLines = extractSemanticSection(context, "WORLD_LORE:")
        val worldHistoryLines = extractSemanticSection(context, "WORLD_HISTORY:")
        val npcLoreLines = extractSemanticSection(context, "NPC_LORE:")
        val storySignalsLine = extractSemanticLine(context, "STORY_SIGNALS:")
        val storyFacts = StoryFactResolver.fromSemanticLines(worldHistoryLines, worldLoreLines)
        val professionAnswer = NpcProfessionFactResolver.resolve(normalizedMessage, context)
        if (professionAnswer != null) return professionAnswer

        val wantsLore = DialogueMessageText.matches(normalizedMessage, "lore", "poveste", "povestea", "povesti", "povesteste")
        val wantsNpcHistory = DialogueMessageText.matches(
            normalizedMessage, "istorie", "istoria", "istoric", "istoricul", "trecut", "trecutul"
        )
        val wantsPlace = normalizedMessage.trimEnd('?', '!', '.') in setOf("unde sunt", "unde suntem", "unde ne aflam") ||
            DialogueMessageText.matches(normalizedMessage, "unde sunt acum", "unde suntem acum", "unde ne aflam acum", "ce locuri sunt aici", "ce locuri sunt in zona", "care sunt locurile din zona")
        val wantsSignals = DialogueMessageText.matches(normalizedMessage, "semnal", "semnale", "semnalele", "indiciu", "indicii", "indiciile")
        val wantsLocalLore = DialogueMessageText.matches(normalizedMessage, "sat", "satul", "satului", "regiune", "regiunii", "zona", "zonei", "aici", "acestui loc")

        return when {
            wantsSignals -> buildStorySignalsAnswer(storySignalsLine)
            StoryFactResolver.isLookupQuestion(normalizedMessage, storyFacts) -> StoryFactResolver.resolve(normalizedMessage, storyFacts)
            wantsNpcHistory -> buildNpcLoreAnswer(npcLoreLines, normalizedMessage)
            wantsLore -> buildNpcLoreAnswer(npcLoreLines, normalizedMessage)
                ?: if (wantsLocalLore) StoryFactResolver.historyAnswer(storyFacts) ?: buildWorldLoreAnswer(worldLoreLines) else null
            wantsPlace -> buildWorldLoreAnswer(worldLoreLines)
            else -> null
        }
    }

    private fun extractSemanticSection(context: String, header: String): List<String> {
        val lines = context.lineSequence().map { it.trimEnd() }.toList()
        val startIndex = lines.indexOfFirst { it.trim() == header }
        if (startIndex < 0) {
            return emptyList()
        }

        val endIndex = (startIndex + 1 until lines.size).firstOrNull { index ->
            val trimmed = lines[index].trim()
            trimmed == "WORLD_LORE:" ||
                trimmed == "WORLD_HISTORY:" ||
                trimmed == "NPC_LORE:" ||
                trimmed.startsWith("STORY_SIGNALS:") ||
                trimmed.startsWith("Raspuns factual pentru intrebari despre meserii:")
        } ?: lines.size

        return lines.subList(startIndex + 1, endIndex)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun extractSemanticLine(context: String, prefix: String): String? {
        return context.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun buildWorldLoreAnswer(loreLines: List<String>): String? {
        if (loreLines.isEmpty()) {
            return null
        }
        val regionLine = loreLines.firstOrNull { it.startsWith("- region=") }
        val placeLine = loreLines.firstOrNull { it.startsWith("- current_place=") }
        val nearbyPlaces = loreLines.filter { line ->
            line.startsWith("- ") &&
                !line.startsWith("- region=") &&
                !line.startsWith("- current_place=") &&
                !line.startsWith("- nearby_places:")
        }.take(3)

        val regionName = extractField(regionLine, "name")
        val placeName = extractField(placeLine, "name")
        val placeType = extractField(placeLine, "type")

        return buildString {
            if (placeName.isNotBlank() && regionName.isNotBlank()) {
                append("Sunt in ").append(placeName).append(" din ").append(regionName)
                if (placeType.isNotBlank()) {
                    append(" (").append(placeType).append(")")
                }
                append('.')
            } else if (placeName.isNotBlank()) {
                append("Sunt in ").append(placeName)
                if (placeType.isNotBlank()) {
                    append(" (").append(placeType).append(")")
                }
                append('.')
            } else if (regionName.isNotBlank()) {
                append("Sunt in ").append(regionName).append('.')
            }
            if (nearbyPlaces.isNotEmpty()) {
                val summary = nearbyPlaces.mapNotNull { line ->
                    val name = line.substringAfter("- ").substringBefore(", type=").trim()
                    val type = extractField(line, "type")
                    if (name.isBlank()) null else if (type.isBlank()) name else "$name ($type)"
                }
                if (summary.isNotEmpty()) {
                    if (isNotEmpty()) append(' ')
                    append("Locuri din zona: ").append(summary.joinToString("; ")).append('.')
                }
            }
        }.takeIf { it.isNotBlank() }
    }

    private fun buildNpcLoreAnswer(npcLines: List<String>, normalizedMessage: String): String? {
        if (npcLines.isEmpty()) {
            return null
        }
        val selectedLine = npcLines.firstOrNull { line ->
            val name = line.substringAfter("- ").substringBefore(",").trim()
            val occupation = extractField(line, "occupation")
            DialogueMessageText.references(normalizedMessage, name) || DialogueMessageText.references(normalizedMessage, occupation)
        } ?: return null

        val name = selectedLine.substringAfter("- ").substringBefore(",").trim()
        val occupation = extractField(selectedLine, "occupation")
        val work = extractField(selectedLine, "work")
        val lore = extractTailField(selectedLine, "lore")

        return buildString {
            if (name.isNotBlank()) {
                append(name)
            }
            if (occupation.isNotBlank()) {
                if (isNotEmpty()) append(" este ") else append("Este ")
                append(occupation)
            }
            if (work.isNotBlank()) {
                append(" si lucreaza la ").append(work)
            }
            if (lore.isNotBlank()) {
                append(". Lore: ").append(lore)
            } else if (isNotEmpty()) {
                append('.')
            }
        }.takeIf { it.isNotBlank() }
    }

    private fun buildStorySignalsAnswer(signalsLine: String?): String? {
        val signals = signalsLine.orEmpty()
        if (signals.isBlank()) {
            return null
        }
        return "Semnalele povestii sunt: $signals."
    }

    private fun extractField(line: String?, key: String): String {
        if (line.isNullOrBlank()) {
            return ""
        }
        val token = "$key="
        val index = line.indexOf(token)
        if (index < 0) {
            return ""
        }
        val valueStart = index + token.length
        return line.substring(valueStart).substringBefore(",").trim()
    }

    private fun extractTailField(line: String?, key: String): String {
        if (line.isNullOrBlank()) {
            return ""
        }
        val token = "$key="
        val index = line.indexOf(token)
        if (index < 0) {
            return ""
        }
        return line.substring(index + token.length).trim()
    }
}
