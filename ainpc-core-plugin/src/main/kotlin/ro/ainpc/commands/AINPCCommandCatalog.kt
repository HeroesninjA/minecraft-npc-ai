package ro.ainpc.commands

import java.util.Locale

internal enum class AINPCDirectCommandRoute {
    MAIN,
    QUEST,
    PROGRESSION,
    CONTRACT,
    DUTY,
    BOUNTY,
    EVENT,
    TUTORIAL,
    RITUAL,
}

internal enum class AINPCSubcommandRoute {
    CREATE,
    DELETE,
    DELETE_ID,
    DUPLICATES,
    REPAIR,
    INFO,
    GUI,
    AUTHORING,
    VERSION,
    QUEST,
    RESET_OBJECTIVE,
    RESET_REWARD,
    RESET_DIALOG,
    RESET_DRAFT,
    PROGRESSION,
    REPUTATION,
    CONTRACT,
    DUTY,
    BOUNTY,
    EVENT,
    TUTORIAL,
    RITUAL,
    DEMO,
    WORLD,
    PATCH,
    WAND,
    MAP,
    STORY,
    MIGRATION,
    POPULATION,
    AUDIT,
    DEBUG_DUMP,
    DEBUG_DIALOG,
    SCENARIO,
    LIST,
    FAMILY,
    ROUTINE,
    MOOD,
    TELEPORT,
    RELOAD,
    TEST,
    HEALTH,
    OVERVIEW,
    ECONOMY,
    BUILD,
    BUILDING,
    RELATIONSHIP,
    ENVIRONMENT,
}

internal object AINPCCommandCatalog {
    private val COMMAND_TOKEN = Regex("[a-z][a-z-]*")

    private data class DirectSpec(
        val route: AINPCDirectCommandRoute,
        val tokens: Set<String>,
    )

    private data class SubcommandSpec(
        val route: AINPCSubcommandRoute,
        val primary: String,
        val aliases: Set<String> = emptySet(),
        val suggested: Boolean = true,
    ) {
        val tokens: Set<String> = linkedSetOf(primary).apply { addAll(aliases) }
    }

    private val directSpecs = listOf(
        direct(AINPCDirectCommandRoute.MAIN, "ainpc", "ai", "npc"),
        direct(AINPCDirectCommandRoute.QUEST, "npcquest", "npcq", "quest", "quests"),
        direct(AINPCDirectCommandRoute.PROGRESSION, "progression", "progress"),
        direct(AINPCDirectCommandRoute.CONTRACT, "contract", "contracts"),
        direct(AINPCDirectCommandRoute.DUTY, "duty", "duties", "sarcina", "sarcini"),
        direct(AINPCDirectCommandRoute.BOUNTY, "bounty", "bounties"),
        direct(AINPCDirectCommandRoute.EVENT, "event", "events"),
        direct(AINPCDirectCommandRoute.TUTORIAL, "tutorial", "tutorials", "onboarding"),
        direct(AINPCDirectCommandRoute.RITUAL, "ritual", "rituals", "ceremony", "ceremonies"),
    )

    private val subcommandSpecs = listOf(
        subcommand(AINPCSubcommandRoute.CREATE, "create"),
        subcommand(AINPCSubcommandRoute.DELETE, "delete", "remove"),
        subcommand(AINPCSubcommandRoute.DELETE_ID, "delete-id"),
        subcommand(AINPCSubcommandRoute.DUPLICATES, "duplicates"),
        subcommand(AINPCSubcommandRoute.REPAIR, "repair"),
        subcommand(AINPCSubcommandRoute.INFO, "info"),
        subcommand(AINPCSubcommandRoute.GUI, "gui"),
        subcommand(AINPCSubcommandRoute.AUTHORING, "authoring"),
        subcommand(AINPCSubcommandRoute.QUEST, "quest"),
        subcommand(AINPCSubcommandRoute.SCENARIO, "scenario"),
        subcommand(AINPCSubcommandRoute.PROGRESSION, "progression", "progress"),
        subcommand(AINPCSubcommandRoute.CONTRACT, "contract", "contracts"),
        subcommand(AINPCSubcommandRoute.DUTY, "duty", "duties", "sarcina", "sarcini"),
        subcommand(AINPCSubcommandRoute.BOUNTY, "bounty", "bounties"),
        subcommand(AINPCSubcommandRoute.EVENT, "event", "events", "eveniment", "evenimente"),
        subcommand(AINPCSubcommandRoute.TUTORIAL, "tutorial", "tutorials", "onboarding"),
        subcommand(
            AINPCSubcommandRoute.RITUAL,
            "ritual",
            "rituals",
            "ceremony",
            "ceremonies",
            "ceremonie",
            "ceremonii",
        ),
        subcommand(AINPCSubcommandRoute.DEMO, "demo"),
        subcommand(AINPCSubcommandRoute.BUILD, "build"),
        subcommand(AINPCSubcommandRoute.BUILDING, "building"),
        subcommand(AINPCSubcommandRoute.WORLD, "world"),
        subcommand(AINPCSubcommandRoute.PATCH, "patch"),
        subcommand(AINPCSubcommandRoute.WAND, "wand"),
        subcommand(AINPCSubcommandRoute.MAP, "map"),
        subcommand(AINPCSubcommandRoute.STORY, "story"),
        subcommand(AINPCSubcommandRoute.MIGRATION, "migration"),
        subcommand(AINPCSubcommandRoute.POPULATION, "population"),
        subcommand(AINPCSubcommandRoute.AUDIT, "audit"),
        subcommand(AINPCSubcommandRoute.DEBUG_DUMP, "debugdump"),
        subcommand(AINPCSubcommandRoute.LIST, "list"),
        subcommand(AINPCSubcommandRoute.FAMILY, "family"),
        subcommand(AINPCSubcommandRoute.ROUTINE, "routine"),
        subcommand(AINPCSubcommandRoute.MOOD, "mood", "emotion"),
        subcommand(AINPCSubcommandRoute.VERSION, "version"),
        subcommand(AINPCSubcommandRoute.TELEPORT, "tp", "teleport"),
        subcommand(AINPCSubcommandRoute.HEALTH, "health", "status", "healthcheck"),
        subcommand(AINPCSubcommandRoute.RELOAD, "reload"),
        subcommand(AINPCSubcommandRoute.TEST, "test"),
        subcommand(AINPCSubcommandRoute.ECONOMY, "economy"),
        subcommand(AINPCSubcommandRoute.RESET_OBJECTIVE, "reset-objective", "resetobjective", suggested = false),
        subcommand(AINPCSubcommandRoute.RESET_REWARD, "reset-reward", "resetreward", suggested = false),
        subcommand(AINPCSubcommandRoute.RESET_DIALOG, "reset-dialog", "resetdialog", suggested = false),
        subcommand(AINPCSubcommandRoute.RESET_DRAFT, "reset-draft", "resetdraft", suggested = false),
        subcommand(AINPCSubcommandRoute.REPUTATION, "reputation", "reputatie", suggested = false),
        subcommand(AINPCSubcommandRoute.DEBUG_DIALOG, "debugdialog", suggested = false),
        subcommand(AINPCSubcommandRoute.OVERVIEW, "overview", "preview", "summary", suggested = false),
        subcommand(
            AINPCSubcommandRoute.RELATIONSHIP,
            "relationship",
            "relationships",
            "relatii",
            suggested = false,
        ),
        subcommand(
            AINPCSubcommandRoute.ENVIRONMENT,
            "environment",
            "env",
            "time",
            "weather",
            suggested = false,
        ),
    )

    private val directRoutes = indexRoutes(directSpecs.map { spec -> spec.route to spec.tokens })
    private val subcommandRoutes = indexRoutes(subcommandSpecs.map { spec -> spec.route to spec.tokens })

    init {
        require(directRoutes.values.toSet() == AINPCDirectCommandRoute.entries.toSet()) {
            "Catalogul comenzilor directe nu acopera toate rutele."
        }
        require(subcommandRoutes.values.toSet() == AINPCSubcommandRoute.entries.toSet()) {
            "Catalogul subcomenzilor nu acopera toate rutele."
        }
    }

    val suggestedSubcommands: List<String> = subcommandSpecs
        .filter(SubcommandSpec::suggested)
        .map(SubcommandSpec::primary)

    fun resolveDirectCommand(commandName: String): AINPCDirectCommandRoute? =
        directRoutes[normalize(commandName)]

    fun resolveSubcommand(token: String): AINPCSubcommandRoute? =
        subcommandRoutes[normalize(token)]

    private fun direct(route: AINPCDirectCommandRoute, vararg tokens: String): DirectSpec =
        DirectSpec(route, linkedSetOf(*tokens))

    private fun subcommand(
        route: AINPCSubcommandRoute,
        primary: String,
        vararg aliases: String,
        suggested: Boolean = true,
    ): SubcommandSpec = SubcommandSpec(route, primary, linkedSetOf(*aliases), suggested)

    private fun <T : Enum<T>> indexRoutes(specs: List<Pair<T, Set<String>>>): Map<String, T> {
        val routes = linkedMapOf<String, T>()
        for ((route, tokens) in specs) {
            require(tokens.isNotEmpty()) { "Ruta $route nu are niciun token." }
            for (token in tokens) {
                val normalized = normalize(token)
                require(normalized == token && COMMAND_TOKEN.matches(token)) { "Token de comanda invalid: '$token'." }
                require(routes.put(token, route) == null) { "Token de comanda duplicat: '$token'." }
            }
        }
        return routes.toMap()
    }

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)
}
