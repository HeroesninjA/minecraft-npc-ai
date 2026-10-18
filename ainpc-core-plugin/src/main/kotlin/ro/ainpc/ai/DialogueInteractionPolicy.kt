package ro.ainpc.ai

internal data class DialogueInteractionEffect(
    val affectionChange: Double = 0.0,
    val trustChange: Double = 0.0,
    val respectChange: Double = 0.0,
    val memoryImportance: Int? = null,
    val emotionType: String? = null,
)

internal object DialogueInteractionPolicy {
    private val reportedSpeech = arrayOf(
        "am auzit", "am spus", "am zis", "a spus", "a zis", "au spus", "au zis",
        "ai spus", "ai zis", "mi-a spus", "mi-a zis", "mi-ai spus", "mi-ai zis",
        "m-a numit", "m-ai numit", "spune ca", "spunea ca", "zice ca", "spui ca", "zici ca",
    )
    private val negatedAddress = Regex("\\bnu\\s+(?:te|iti|esti)\\b")
    private val directThanks = Regex("^(?:multumesc|mersi)\\b|,\\s+(?:multumesc|mersi)\\b|\\b(?:dar|insa|totusi)\\s+(?:multumesc|mersi)\\b|\\b(?:iti multumesc|te apreciez|te respect)\\b")
    private val directInsult = Regex("\\b(?:tu\\s+)?esti\\s+(?:(?:un|foarte|cam)\\s+)*(?:prost|idiot|tampit|urat|fraier)\\b")
    private val leadingVocative = Regex("^([\\p{L}][\\p{L}\\p{N} _-]{0,39}),\\s+(?=(?:tu\\s+)?(?:esti|te|iti)\\b|(?:multumesc|mersi)\\b)")
    private val trailingVocative = Regex(",\\s+([\\p{L}][\\p{L}\\p{N} _-]{0,39})[.!?]*$")
    private val leadingContrast = Regex("^(?:dar|insa|totusi)\\b[\\s,;:!?]*")
    private val quotedSpeechMarks = setOf('"', '“', '”', '„', '«', '»', '\'')

    fun forMessage(
        message: String,
        sentiment: String,
        directAddress: Boolean,
        npcName: String,
        npcDisplayName: String? = null,
        addressClarified: Boolean = false,
    ): DialogueInteractionEffect {
        if (!directAddress) return DialogueInteractionEffect()

        val reportedLead = DialogueMessageText.hasReportedLead(message)
        val quotedLead = DialogueMessageText.hasLeadingQuotedSpan(message)
        val normalized = DialogueMessageText.normalize(message)
        val directText = when {
            reportedLead -> DialogueMessageText.directIntentText(message)
            quotedLead -> leadingContrast.replace(
                DialogueMessageText.unquotedText(message).trimStart { !it.isLetterOrDigit() },
                "",
            )
            else -> normalized
        }
        val effectiveSentiment = if (reportedLead || quotedLead) OpenAITextSupport.analyzeSentimentFast(directText) else sentiment
        val effect = forSentiment(effectiveSentiment)
        if (effect.memoryImportance == null && effect.emotionType == null) return effect

        val vocative = (leadingVocative.find(directText)?.groupValues?.get(1)
            ?: trailingVocative.find(directText)?.groupValues?.get(1))?.trim()
        if (!addressClarified && vocative != null && listOfNotNull(npcName, npcDisplayName)
                .none { DialogueMessageText.normalize(it) == vocative }
        ) {
            return DialogueInteractionEffect()
        }
        if (effect.memoryImportance == null) return effect

        val unsafeQuote = message.any { it in quotedSpeechMarks } &&
            (!reportedLead && !quotedLead || DialogueMessageText.unquotedText(message).any { it in quotedSpeechMarks })
        if (unsafeQuote ||
            DialogueMessageText.matches(directText, *reportedSpeech) ||
            negatedAddress.containsMatchIn(directText)
        ) {
            return DialogueInteractionEffect()
        }

        val bareMessage = directText.trim { !it.isLetterOrDigit() }
        val directAct = when (effectiveSentiment) {
            "compliment" -> directThanks.containsMatchIn(directText) || bareMessage == "respect"
            "insult" -> directInsult.containsMatchIn(directText) ||
                bareMessage in setOf("prost", "idiot", "tampit", "urat", "fraier")
            "threat" -> DialogueMessageText.matches(directText, "te omor", "te bat", "iti rup") ||
                bareMessage in setOf("mori", "kill")
            else -> false
        }
        return if (directAct) effect else DialogueInteractionEffect()
    }

    private fun forSentiment(sentiment: String): DialogueInteractionEffect = when (sentiment) {
        "greeting" -> DialogueInteractionEffect(emotionType = "greeting")
        "compliment" -> DialogueInteractionEffect(
            affectionChange = 0.05,
            trustChange = 0.03,
            memoryImportance = 3,
            emotionType = "compliment",
        )
        "insult" -> DialogueInteractionEffect(
            affectionChange = -0.1,
            trustChange = -0.05,
            respectChange = -0.05,
            memoryImportance = 4,
            emotionType = "insult",
        )
        "threat" -> DialogueInteractionEffect(
            affectionChange = -0.15,
            trustChange = -0.2,
            respectChange = -0.1,
            memoryImportance = 4,
            emotionType = "threat",
        )
        else -> DialogueInteractionEffect()
    }
}
