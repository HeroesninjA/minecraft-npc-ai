package ro.ainpc.api.settlement

enum class SettlementPlanStatus(val id: String) {
    DRAFT("draft"),
    VALIDATED("validated"),
    COMMITTED("committed"),
    DISCARDED("discarded"),
    FAILED("failed");

    companion object {
        @JvmStatic
        fun fromId(value: String?): SettlementPlanStatus {
            if (value.isNullOrBlank()) return DRAFT
            for (status in entries) {
                if (status.id.equals(value, ignoreCase = true) || status.name.equals(value, ignoreCase = true)) {
                    return status
                }
            }
            return DRAFT
        }
    }
}
