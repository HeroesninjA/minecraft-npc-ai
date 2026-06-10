package ro.ainpc.ai.orchestration

enum class AIResultStatus {
    SUCCESS,
    VALIDATION_FAILED,
    PROVIDER_FAILED,
    TIMEOUT,
    RATE_LIMITED,
    FALLBACK_USED,
    DISABLED
}
