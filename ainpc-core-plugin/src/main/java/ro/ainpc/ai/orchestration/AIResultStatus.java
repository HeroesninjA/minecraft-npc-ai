package ro.ainpc.ai.orchestration;

public enum AIResultStatus {
    SUCCESS,
    VALIDATION_FAILED,
    PROVIDER_FAILED,
    TIMEOUT,
    RATE_LIMITED,
    FALLBACK_USED,
    DISABLED
}
