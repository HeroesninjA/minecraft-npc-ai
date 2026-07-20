package ro.ainpc.engine

object ObjectiveTypeAliasRegistry {
    private val ALIASES = mapOf(
        "collect_item" to listOf("item", "collect", "collectitem", "fetch", "gather"),
        "deliver_to_npc" to listOf("deliver", "deliveritem", "deliver_item", "turnin", "turn_in"),
        "talk_to_npc" to listOf("talk", "speak", "conversation", "talk_npc", "talk_nlc", "speak_to_npc"),
        "visit_region" to listOf("visit", "travel", "go_to", "enter_region"),
        "visit_place" to listOf("visitplace", "enterplace", "enter_place", "go_to_place"),
        "inspect_node" to listOf("inspect", "inspectnode", "interact_node", "interact_nkde"),
        "kill_mob" to listOf("kill", "slay", "defeat"),
        "place_block" to listOf("placeblock", "build", "construct"),
        "break_block" to listOf("break", "breakblock", "mine", "dig", "excavate"),
        "craft_item" to listOf("craft", "craftitem", "make", "create_item", "fabricate"),
        "use_item" to listOf("use", "consume", "drink", "eat", "activate", "utilize"),
        "equip_item" to listOf("equip", "wear", "don", "put_on"),
    )

    private val ALL_ALIASES: Map<String, String> = ALIASES.entries.flatMap { (canonical, aliases) ->
        aliases.map { it to canonical } + (canonical to canonical)
    }.toMap()

    private val DEPRECATED_ALIASES = setOf(
        "talk_nlc", "interact_nkde", "turnin", "gather", "slay", "construct", "fabricate"
    )

    private val SUPPORTED_TYPES: Set<String> = ALIASES.keys

    fun normalize(type: String?): String {
        if (type.isNullOrBlank()) return "collect_item"
        val normalized = normalizeReference(type)
        return ALL_ALIASES[normalized] ?: normalized
    }

    fun isSupported(type: String?): Boolean {
        if (type.isNullOrBlank()) return false
        return normalize(type) in SUPPORTED_TYPES
    }

    fun supportedTypes(): Set<String> = SUPPORTED_TYPES

    fun aliasesFor(canonicalType: String): List<String> =
        ALIASES[canonicalType]?.toList() ?: emptyList()

    fun requiredFields(type: String?): List<String> {
        val normalized = normalize(type)
        return when (normalized) {
            "collect_item" -> listOf("item")
            "deliver_to_npc" -> listOf("item", "npc_target")
            "talk_to_npc" -> listOf("npc_target")
            "visit_region" -> listOf("item")
            "visit_place" -> listOf("item")
            "inspect_node" -> listOf("item")
            "kill_mob" -> emptyList()
            "place_block" -> listOf("item")
            "break_block" -> listOf("item")
            "craft_item" -> listOf("item")
            "use_item" -> listOf("item")
            "equip_item" -> listOf("item")
            else -> emptyList()
        }
    }

    fun isDeprecated(alias: String?): Boolean {
        if (alias.isNullOrBlank()) return false
        val normalized = normalizeReference(alias)
        return normalized in DEPRECATED_ALIASES
    }

    fun recommendedType(alias: String?): String? {
        if (alias.isNullOrBlank()) return null
        val normalized = normalizeReference(alias)
        if (normalized in DEPRECATED_ALIASES) {
            return ALL_ALIASES[normalized]
        }
        return null
    }

    fun suggestCorrection(typo: String?): String? {
        if (typo.isNullOrBlank()) return null
        val normalized = normalizeReference(typo)
        val knownTypos = mapOf(
            "talknlc" to "talk_nlc",
            "interactnkde" to "interact_nkde",
            "collect_item" to "collect_item",
            "deliveritem" to "deliver_item",
            "visitplace" to "visit_place",
            "inspectnode" to "inspect_node",
            "placeblock" to "place_block",
            "breakblock" to "break_block",
            "craftitem" to "craft_item",
            "useitem" to "use_item",
            "equipitem" to "equip_item",
            "talkto" to "talk_to_npc",
            "kill" to "kill_mob",
            "slay" to "kill_mob",
            "fetch" to "collect_item",
            "gather" to "collect_item",
            "build" to "place_block",
            "mine" to "break_block",
            "dig" to "break_block",
            "craft" to "craft_item",
            "make" to "craft_item",
            "consume" to "use_item",
            "wear" to "equip_item",
        )
        val match = knownTypos[normalized]
        if (match != null && ALL_ALIASES[match] != normalized) {
            val canonical = ALL_ALIASES[match] ?: return null
            if (canonical != normalized) return canonical
        }
        val similar = ALL_ALIASES.keys
            .map { it to levenshtein(normalized, it) }
            .filter { it.second <= 3 && it.second > 0 }
            .minByOrNull { it.second }
        if (similar != null) {
            val canonical = ALL_ALIASES[similar.first] ?: similar.first
            if (canonical != normalized) return canonical
        }
        return null
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }
}
