package ro.ainpc.commands

data class AuthoringCommandRequest(
    val mode: Mode,
    val questSelector: String?,
    val mechanicId: String?
) {
    enum class Mode {
        OPEN,
        DUMP,
        CLEAR,
        PREV,
        NEXT
    }
}

data class AuthoringCommandPlan(
    val requiresPlayer: Boolean,
    val mode: AuthoringCommandRequest.Mode
)

data class AuthoringResolvedSelection(
    val questSelector: String?,
    val mechanicId: String?
)

object AuthoringCommandSupport {
    private val primaryAliases = listOf("next", "prev", "clear", "dump")
    private val secondaryAliases = listOf("next" to "forward", "prev" to "previous", "clear" to "reset")
    private val modeAliases = listOf("next", "forward", "prev", "previous", "clear", "reset", "dump")

    @JvmStatic
    fun parse(args: Array<String>): AuthoringCommandRequest {
        val action = args.getOrNull(1)?.trim()?.lowercase().orEmpty()
        return when (action) {
            "dump" -> AuthoringCommandRequest(
                AuthoringCommandRequest.Mode.DUMP,
                args.getOrNull(2)?.takeIf { it.isNotBlank() },
                args.getOrNull(3)?.takeIf { it.isNotBlank() }
            )
            "clear", "reset" -> AuthoringCommandRequest(
                AuthoringCommandRequest.Mode.CLEAR,
                null,
                null
            )
            "prev", "previous" -> AuthoringCommandRequest(
                AuthoringCommandRequest.Mode.PREV,
                null,
                null
            )
            "next", "forward" -> AuthoringCommandRequest(
                AuthoringCommandRequest.Mode.NEXT,
                null,
                null
            )
            else -> AuthoringCommandRequest(
                AuthoringCommandRequest.Mode.OPEN,
                args.getOrNull(1)?.takeIf { it.isNotBlank() },
                args.getOrNull(2)?.takeIf { it.isNotBlank() }
            )
        }
    }

    @JvmStatic
    fun modeSuggestions(prefix: String): List<String> {
        if (prefix.isEmpty()) {
            return modeAliases.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
        val seen = mutableSetOf<AuthoringCommandRequest.Mode>()
        return modeAliases
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .filter { value -> value.lowercase().startsWith(prefix.lowercase()) }
            .filter { value ->
                val mode = when (value) {
                    "next", "forward" -> AuthoringCommandRequest.Mode.NEXT
                    "prev", "previous" -> AuthoringCommandRequest.Mode.PREV
                    "clear", "reset" -> AuthoringCommandRequest.Mode.CLEAR
                    "dump" -> AuthoringCommandRequest.Mode.DUMP
                    else -> null
                }
                mode == null || seen.add(mode)
            }
    }

    @JvmStatic
    fun plan(request: AuthoringCommandRequest): AuthoringCommandPlan {
        return when (request.mode) {
            AuthoringCommandRequest.Mode.DUMP -> AuthoringCommandPlan(false, request.mode)
            AuthoringCommandRequest.Mode.OPEN,
            AuthoringCommandRequest.Mode.CLEAR,
            AuthoringCommandRequest.Mode.PREV,
            AuthoringCommandRequest.Mode.NEXT -> AuthoringCommandPlan(true, request.mode)
        }
    }

    @JvmStatic
    fun resolveDumpSelection(
        request: AuthoringCommandRequest,
        currentQuestSelector: String?,
        currentMechanicId: String?
    ): AuthoringResolvedSelection {
        val questSelector = request.questSelector?.takeIf { it.isNotBlank() } ?: currentQuestSelector?.takeIf { it.isNotBlank() }
        val mechanicId = request.mechanicId?.takeIf { it.isNotBlank() } ?: currentMechanicId?.takeIf { it.isNotBlank() }
        return AuthoringResolvedSelection(questSelector, mechanicId)
    }
}
