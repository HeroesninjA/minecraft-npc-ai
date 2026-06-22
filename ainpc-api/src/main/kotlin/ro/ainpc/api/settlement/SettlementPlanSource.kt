package ro.ainpc.api.settlement

enum class SettlementPlanSource(val id: String) {
    MANUAL("manual"),
    VANILLA_SCAN("vanilla_scan"),
    AI_DRAFT("ai_draft"),
    DEMO("demo"),
    MIGRATION("migration");

    companion object {
        @JvmStatic
        fun fromId(value: String?): SettlementPlanSource {
            if (value.isNullOrBlank()) return MANUAL
            for (source in entries) {
                if (source.id.equals(value, ignoreCase = true) || source.name.equals(value, ignoreCase = true)) {
                    return source
                }
            }
            return MANUAL
        }
    }
}
