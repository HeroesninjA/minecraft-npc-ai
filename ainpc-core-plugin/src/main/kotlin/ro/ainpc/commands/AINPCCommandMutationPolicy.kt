package ro.ainpc.commands

import java.util.Locale

internal data class AINPCCommandMutation(
    val operation: String,
    val requiresConfirmation: Boolean = true,
    val worldSaveReminder: Boolean = true,
)

internal object AINPCCommandMutationPolicy {
    private const val CONFIRMATION_FLAG = "--confirm"
    private val WORLD_MAPPING_ACTIONS = setOf("create", "edit", "remove", "delete")

    fun mutation(route: AINPCSubcommandRoute, args: Array<String>): AINPCCommandMutation? = when (route) {
        AINPCSubcommandRoute.WORLD -> worldMutation(args)
        AINPCSubcommandRoute.PATCH ->
            if (token(args, 1) == "apply") AINPCCommandMutation("ainpc patch apply") else null
        AINPCSubcommandRoute.MAP ->
            if (token(args, 1) in setOf("confirm", "confirma")) {
                AINPCCommandMutation("ainpc map confirm", requiresConfirmation = false)
            } else {
                null
            }
        AINPCSubcommandRoute.BUILDING ->
            if (token(args, 1) == "auto-place") AINPCCommandMutation("ainpc building auto-place") else null
        else -> null
    }

    fun blockedOperation(route: AINPCSubcommandRoute, args: Array<String>): String? =
        mutation(route, args)?.operation

    fun hasExplicitConfirmation(args: Array<String>): Boolean = token(args, args.lastIndex) == CONFIRMATION_FLAG

    fun executionArgs(args: Array<String>): Array<String> =
        if (hasExplicitConfirmation(args)) args.dropLast(1).toTypedArray() else args

    fun confirmationCommand(args: Array<String>): String = confirmedCommand("/ainpc ${args.joinToString(" ")}")

    fun requiresConfirmation(command: String): Boolean {
        val parsed = parseCommand(command) ?: return false
        val mutation = mutation(parsed.first, parsed.second) ?: return false
        return mutation.requiresConfirmation && !hasExplicitConfirmation(parsed.second)
    }

    fun confirmedCommand(command: String): String {
        val normalized = command.trim()
        if (normalized.isEmpty() || normalized.endsWith(" $CONFIRMATION_FLAG", ignoreCase = true)) {
            return normalized
        }
        val parsed = parseCommand(normalized) ?: return normalized
        if (mutation(parsed.first, parsed.second) == null) {
            return normalized
        }
        return "$normalized $CONFIRMATION_FLAG"
    }

    private fun worldMutation(args: Array<String>): AINPCCommandMutation? {
        val mode = token(args, 1)
        val action = token(args, 2)
        return when (mode) {
            "fixture" -> when (action) {
                "apply" -> AINPCCommandMutation("ainpc world fixture apply")
                "populate" -> if (token(args, 3) == "--dry-run") {
                    null
                } else {
                    AINPCCommandMutation("ainpc world fixture populate")
                }
                else -> null
            }
            "region", "place", "node" ->
                if (action in WORLD_MAPPING_ACTIONS) AINPCCommandMutation("ainpc world $mode $action") else null
            "scan" ->
                if (action == "village" && token(args, 4) == "import") {
                    AINPCCommandMutation(
                        operation = "ainpc world scan village import",
                        worldSaveReminder = false,
                    )
                } else {
                    null
                }
            "demo" -> if (action == "create") AINPCCommandMutation("ainpc world demo create") else null
            "bind" -> if (action == "npc") AINPCCommandMutation("ainpc world bind npc") else null
            "household" -> if (action == "spawn") AINPCCommandMutation("ainpc world household spawn") else null
            "settlement" -> when (action) {
                "auto" -> AINPCCommandMutation(
                    operation = "ainpc world settlement auto",
                    worldSaveReminder = false,
                )
                "spawn" -> AINPCCommandMutation("ainpc world settlement spawn")
                else -> null
            }
            "save" -> AINPCCommandMutation(
                operation = "ainpc world save",
                requiresConfirmation = false,
                worldSaveReminder = false,
            )
            else -> null
        }
    }

    private fun parseCommand(command: String): Pair<AINPCSubcommandRoute, Array<String>>? {
        val tokens = command.trim().removePrefix("/").split(Regex("\\s+")).filter(String::isNotBlank)
        if (tokens.size < 2 || !tokens[0].equals("ainpc", ignoreCase = true)) {
            return null
        }
        val args = tokens.drop(1).toTypedArray()
        val route = AINPCCommandCatalog.resolveSubcommand(args[0]) ?: return null
        return route to args
    }

    private fun token(args: Array<String>, index: Int): String? =
        args.getOrNull(index)?.trim()?.lowercase(Locale.ROOT)
}
