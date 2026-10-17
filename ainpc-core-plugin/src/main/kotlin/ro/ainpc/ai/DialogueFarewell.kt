package ro.ainpc.ai

internal object DialogueFarewell {
    private val phrases = setOf(
        "pa", "pa pa", "la revedere", "adio", "pe curand", "ne vedem", "ne mai vedem",
        "o zi buna", "o seara buna", "noapte buna", "bye", "goodbye", "exit", "quit",
    )
    private val courtesy = setOf("multumesc", "multumesc mult", "mersi")
    private val separators = Regex("[,!.;…]+")

    fun isFarewell(message: String, npcAliases: List<String> = emptyList()): Boolean {
        val normalized = DialogueMessageText.normalize(message).trimEnd('!', '.', '…').trimEnd()
        if ('?' in normalized) return false
        if (isFarewellBody(normalized)) return true

        return npcAliases.map(DialogueMessageText::normalize).filter(String::isNotBlank).any { alias ->
            val leadingAddress = Regex("^${Regex.escape(alias)}\\s*,\\s*")
            val trailingAddress = Regex("(?:,\\s*|\\s+)${Regex.escape(alias)}$")
            val body = trailingAddress.replaceFirst(leadingAddress.replaceFirst(normalized, ""), "").trim()
            body != normalized && isFarewellBody(body)
        }
    }

    private fun isFarewellBody(message: String): Boolean {
        val parts = message.split(separators).map(String::trim).filter(String::isNotBlank)
        return parts.any { it in phrases } && parts.all { it in phrases || it in courtesy }
    }
}
