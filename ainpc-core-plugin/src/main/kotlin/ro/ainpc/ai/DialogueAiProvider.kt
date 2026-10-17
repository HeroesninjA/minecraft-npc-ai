package ro.ainpc.ai

internal enum class DialogueAiProvider {
    OPENAI,
    OLLAMA;

    fun canGenerate(
        featureEnabled: Boolean,
        openAiReady: () -> Boolean,
        ollamaReady: () -> Boolean,
    ): Boolean = featureEnabled && when (this) {
        OPENAI -> openAiReady()
        OLLAMA -> ollamaReady()
    }

    companion object {
        fun fromConfig(value: String?): DialogueAiProvider =
            if (value.equals("ollama", ignoreCase = true)) OLLAMA else OPENAI
    }
}
