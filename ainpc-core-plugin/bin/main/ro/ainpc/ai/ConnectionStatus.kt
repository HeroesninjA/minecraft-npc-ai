package ro.ainpc.ai

class ConnectionStatus private constructor(
    val reachable: Boolean,
    private val configuredModel: String,
    val triedUrls: List<String>,
    val respondingUrl: String?,
    val modelAvailable: Boolean,
    val availableModels: List<String>,
    val errors: List<String>
) {
    fun isReachable(): Boolean = reachable

    fun isModelAvailable(): Boolean = modelAvailable

    fun getSummary(): String = summary()

    companion object {
        @JvmStatic
        fun unchecked(configuredModel: String, triedUrls: List<String>): ConnectionStatus {
            return ConnectionStatus(false, configuredModel, triedUrls.toList(), null, false, emptyList(), emptyList())
        }

        @JvmStatic
        fun reachable(
            configuredModel: String,
            triedUrls: List<String>,
            respondingUrl: String?,
            modelAvailable: Boolean,
            availableModels: List<String>,
            errors: List<String>
        ): ConnectionStatus {
            return ConnectionStatus(
                true,
                configuredModel,
                triedUrls.toList(),
                respondingUrl,
                modelAvailable,
                availableModels.toList(),
                errors.toList()
            )
        }

        @JvmStatic
        fun unreachable(
            configuredModel: String,
            triedUrls: List<String>,
            respondingUrl: String?,
            availableModels: List<String>,
            errors: List<String>
        ): ConnectionStatus {
            return ConnectionStatus(
                false,
                configuredModel,
                triedUrls.toList(),
                respondingUrl,
                false,
                availableModels.toList(),
                errors.toList()
            )
        }
    }

    fun summary(): String {
        if (reachable && modelAvailable) {
            return "OpenAI raspunde pe $respondingUrl si modelul \"$configuredModel\" este disponibil."
        }

        if (reachable) {
            val details = if (errors.isEmpty()) "<fara detalii suplimentare>" else errors.joinToString(" | ")
            return "OpenAI raspunde pe $respondingUrl, dar modelul \"$configuredModel\" nu este disponibil. Detalii: $details"
        }

        return if (errors.isEmpty()) {
            "Nu am putut contacta OpenAI API."
        } else {
            "Nu am putut contacta OpenAI API. Probe: ${errors.joinToString(" | ")}."
        }
    }
}
