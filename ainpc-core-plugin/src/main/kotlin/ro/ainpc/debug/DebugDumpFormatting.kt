package ro.ainpc.debug

import org.bukkit.configuration.file.FileConfiguration
import ro.ainpc.ai.OpenAIDebugSnapshot
import java.time.Instant

object DebugDumpFormatting {
    @JvmStatic
    fun normalizeScope(scope: String?): String {
        if (scope == null || scope.isBlank()) {
            return "all"
        }

        return when (val normalized = scope.trim().lowercase()) {
            "all", "npc", "world", "quest", "story", "openai" -> normalized
            else -> "all"
        }
    }

    @JvmStatic
    fun sanitizeConfig(config: FileConfiguration?): String {
        if (config == null) {
            return "# config indisponibil\n"
        }

        val raw = config.saveToString()
        return DebugDumpSecrets.redactText(raw)
    }

    @JvmStatic
    fun buildOpenAiInfo(config: FileConfiguration): String {
        return buildOpenAiInfo(config, null)
    }

    @JvmStatic
    fun buildOpenAiInfo(config: FileConfiguration, runtime: OpenAIDebugSnapshot?): String {
        val sb = StringBuilder()
        sb.append("OpenAI diagnostics snapshot\n")
        sb.append("[config]\n")
        sb.append("base_url: ").append(config.getString("openai.base_url", "")).append("\n")
        sb.append("model: ").append(config.getString("openai.model", "")).append("\n")
        sb.append("api_key: <redacted>\n")
        sb.append("connect_timeout_seconds: ").append(config.getInt("openai.connect_timeout", 10)).append("\n")
        sb.append("read_timeout_seconds: ").append(config.getInt("openai.read_timeout", 120)).append("\n")
        sb.append("write_timeout_seconds: ").append(config.getInt("openai.write_timeout", 30)).append("\n")
        sb.append("max_output_tokens: ").append(config.getInt("openai.max_output_tokens", 150)).append("\n")
        sb.append("temperature: ").append(config.getDouble("openai.temperature", 0.7)).append("\n")
        sb.append("store: ").append(config.getBoolean("openai.store", false)).append("\n")
        sb.append("offline_retry_seconds: ").append(config.getLong("openai.offline_retry_seconds", 15L)).append("\n")
        sb.append("diagnostics.enabled: ").append(config.getBoolean("openai.diagnostics.enabled", false)).append("\n")
        sb.append("diagnostics.check_on_startup: ")
            .append(config.getBoolean("openai.diagnostics.check_on_startup", false)).append("\n")
        sb.append("diagnostics.log_prompt_summary: ")
            .append(config.getBoolean("openai.diagnostics.log_prompt_summary", true)).append("\n")
        sb.append("diagnostics.log_response_preview: ")
            .append(config.getBoolean("openai.diagnostics.log_response_preview", true)).append("\n")

        sb.append("\n[runtime]\n")
        if (runtime == null) {
            sb.append("status: unavailable\n")
        } else {
            sb.append("base_url_runtime: ").append(runtime.baseUrl).append("\n")
            sb.append("model_runtime: ").append(runtime.model).append("\n")
            sb.append("api_key_present: ").append(runtime.apiKeyPresent).append("\n")
            sb.append("offline_mode: ").append(runtime.offlineMode).append("\n")
            sb.append("backoff_active: ").append(runtime.backoffActive).append("\n")
            sb.append("backoff_remaining_seconds: ").append(runtime.backoffRemainingSeconds).append("\n")
            sb.append("offline_retry_after: ").append(formatEpoch(runtime.offlineRetryAfterMillis)).append("\n")
            sb.append("last_request_at: ").append(formatEpoch(runtime.lastRequestAtMillis)).append("\n")
            sb.append("last_prompt_chars: ").append(runtime.lastPromptChars).append("\n")
            sb.append("last_prompt_preview: ").append(inline(runtime.lastPromptPreview)).append("\n")
            sb.append("last_response_at: ").append(formatEpoch(runtime.lastResponseAtMillis)).append("\n")
            sb.append("last_response_chars: ").append(runtime.lastResponseChars).append("\n")
            sb.append("last_response_preview: ").append(inline(runtime.lastResponsePreview)).append("\n")
            sb.append("last_failure_at: ").append(formatEpoch(runtime.lastFailureAtMillis)).append("\n")
            sb.append("last_failure_message: ").append(inline(runtime.lastFailureMessage)).append("\n")
            sb.append("last_fallback_at: ").append(formatEpoch(runtime.lastFallbackAtMillis)).append("\n")
            sb.append("last_fallback_reason: ").append(inline(runtime.lastFallbackReason)).append("\n")
        }

        sb.append("\nNote: network probe is not run by debugdump. Use /ainpc test or scripts/debug-openai.ps1.\n")
        return DebugDumpSecrets.redactText(sb.toString())
    }

    private fun inline(value: String?): String {
        if (value == null || value.isBlank()) {
            return "<none>"
        }
        return value.replace(Regex("\\s+"), " ").trim()
    }

    private fun formatEpoch(epochMillis: Long): String {
        if (epochMillis <= 0L) {
            return "<never>"
        }
        return "$epochMillis (${Instant.ofEpochMilli(epochMillis)})"
    }
}
