package ro.ainpc.engine

object ScopeRegistry {

    private val VALID_SCOPES = setOf("region", "place")
    private val SCOPE_ALIASES = mapOf(
        "world_region" to "region",
        "village" to "region",
        "settlement" to "region",
        "world_place" to "place",
        "location" to "place",
    )

    fun normalize(scope: String?): String {
        if (scope.isNullOrBlank()) return ""
        val normalized = normalizeReference(scope).replace('-', '_')
        return SCOPE_ALIASES[normalized] ?: normalized
    }

    fun isValid(scope: String): Boolean = scope in VALID_SCOPES

    fun isValidOrBlank(scope: String): Boolean = scope.isBlank() || scope in VALID_SCOPES

    fun supportedScopes(): Set<String> = VALID_SCOPES
}
