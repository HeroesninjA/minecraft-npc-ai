package ro.ainpc.commands

import java.util.Locale

enum class AuditSection {
    NPC,
    WORLD,
    DATABASE,
    SPAWN,
    QUEST,
    WAND
}

enum class AuditMode(val argument: String, val sections: List<AuditSection>) {
    ALL("all", AuditSection.values().toList()),
    NPC("npc", listOf(AuditSection.NPC)),
    WORLD("world", listOf(AuditSection.WORLD)),
    DATABASE("db", listOf(AuditSection.DATABASE)),
    SPAWN("spawn", listOf(AuditSection.SPAWN)),
    QUEST("quest", listOf(AuditSection.QUEST)),
    WAND("wand", listOf(AuditSection.WAND));

    companion object {
        fun fromArgument(value: String?): AuditMode? {
            val normalized = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
            return values().firstOrNull { mode -> mode.argument == normalized }
        }
    }
}

enum class AuditProfile(val argument: String) {
    STANDARD("standard"),
    STRICT("strict"),
    FULL("full"),
    OFFLINE("offline");

    companion object {
        fun fromArgument(value: String?): AuditProfile? {
            if (value.isNullOrBlank()) {
                return STANDARD
            }
            val normalized = value.trim().lowercase(Locale.ROOT)
            return values().firstOrNull { profile -> profile != STANDARD && profile.argument == normalized }
        }
    }
}

data class AuditCommandRequest(
    val mode: AuditMode,
    val profile: AuditProfile,
    val outputFormat: AuditOutputFormat = AuditOutputFormat.TEXT,
) {
    fun displayLabel(): String =
        if (profile == AuditProfile.STANDARD) mode.argument else "${mode.argument} ${profile.argument}"

    fun executionPlan(): AuditExecutionPlan {
        val completePersistentScan = profile == AuditProfile.FULL || profile == AuditProfile.OFFLINE
        val completeSpawnHistory = completePersistentScan && AuditSection.SPAWN in mode.sections
        return AuditExecutionPlan(
            sections = mode.sections,
            liveRuntimeChecks = profile != AuditProfile.OFFLINE,
            failOnWarnings = profile == AuditProfile.STRICT,
            questAnchorLimit = if (completePersistentScan) null else 500,
            spawnBatchLimit = if (profile == AuditProfile.STANDARD) 10 else 50,
            scanCompleteSpawnHistory = completeSpawnHistory,
            spawnHistoryPageSize = if (completeSpawnHistory) AUDIT_SPAWN_HISTORY_PAGE_SIZE else null,
        )
    }
}

data class AuditExecutionPlan(
    val sections: List<AuditSection>,
    val liveRuntimeChecks: Boolean,
    val failOnWarnings: Boolean,
    val questAnchorLimit: Int?,
    val spawnBatchLimit: Int,
    val scanCompleteSpawnHistory: Boolean,
    val spawnHistoryPageSize: Int?,
)

const val AUDIT_SPAWN_HISTORY_PAGE_SIZE = 200

enum class AuditOutputFormat(val argument: String) {
    TEXT("text"),
    JSON("json")
}

enum class AuditVerdict(val exitCode: Int) {
    PASS(0),
    WARN(1),
    FAIL(2)
}

fun parseAuditCommandRequest(arguments: List<String>): AuditCommandRequest? {
    if (arguments.size > 3) {
        return null
    }
    val mode = if (arguments.isEmpty()) AuditMode.ALL else AuditMode.fromArgument(arguments[0]) ?: return null
    var profile = AuditProfile.STANDARD
    var outputFormat = AuditOutputFormat.TEXT
    for (option in arguments.drop(1)) {
        val parsedProfile = AuditProfile.fromArgument(option)
        when {
            parsedProfile != null && profile == AuditProfile.STANDARD -> profile = parsedProfile
            option.equals(AuditOutputFormat.JSON.argument, ignoreCase = true) &&
                outputFormat == AuditOutputFormat.TEXT -> outputFormat = AuditOutputFormat.JSON
            else -> return null
        }
    }
    if (profile != AuditProfile.STANDARD && mode != AuditMode.ALL && mode != AuditMode.QUEST) {
        return null
    }
    return AuditCommandRequest(mode, profile, outputFormat)
}

fun auditVerdict(errorCount: Int, warningCount: Int, failOnWarnings: Boolean): AuditVerdict =
    when {
        errorCount > 0 -> AuditVerdict.FAIL
        warningCount > 0 && failOnWarnings -> AuditVerdict.FAIL
        warningCount > 0 -> AuditVerdict.WARN
        else -> AuditVerdict.PASS
    }
