package ro.ainpc.mcp.bridge

import ro.ainpc.bootstrap.RuntimeHealthSnapshot

/**
 * Runtimesnapshot.
 */
data class RuntimeSnapshot(
    val schemaVersion: Int = 3,
    val timestamp: String,
    val plugin: PluginSnapshot,
    val features: FeatureSnapshot,
    val npc: NpcSnapshot,
    val world: WorldSnapshot,
    val quests: QuestSnapshot,
    val buildMode: BuildModeSnapshot,
    val health: RuntimeHealthSnapshot,
)

/**
 * Pluginsnapshot.
 */
data class PluginSnapshot(
    val version: String,
    val serverType: String,
    val onlinePlayers: Int,
    val uptimeMinutes: Long
)

/**
 * Featuresnapshot.
 */
data class FeatureSnapshot(
    val ai: Boolean,
    val mcp: Boolean,
    val aiOrchestration: Boolean
)

/**
 * Npcsnapshot.
 */
data class NpcSnapshot(
    val totalCount: Int,
    val byRegion: Map<String, Int>,
    val samples: List<NpcSample>,
    val relationshipCount: Int = 0,
    val economyNpcCount: Int = 0,
    val economyTotalValue: Int = 0,
    val socialGatherings: Int = 0
)

/**
 * Npcsample.
 */
data class NpcSample(
    val npcId: Int,
    val name: String,
    val profession: String?,
    val regionId: String?,
    val worldName: String?
)

/**
 * Worldsnapshot.
 */
data class WorldSnapshot(
    val regionCount: Int,
    val placeCount: Int,
    val nodeCount: Int,
    val regionNames: List<String>,
    val placeNames: List<String>
)

/**
 * Questsnapshot.
 */
data class QuestSnapshot(
    val activePlayerQuests: Int,
    val activeGlobalQuests: Int,
    val samples: List<QuestSample>,
    val storyEventCount: Int = 0,
    val recentStoryEvents: List<String> = emptyList()
)

/**
 * Buildmodesnapshot.
 */
data class BuildModeSnapshot(
    val activePlayers: Int,
    val byStyle: Map<String, Int>,
    val byTarget: Map<String, Int>,
    val players: List<BuildModePlayerSnapshot>,
    val history: List<BuildModeHistorySnapshot>
)

/**
 * Buildmodeplayersnapshot.
 */
data class BuildModePlayerSnapshot(
    val name: String,
    val style: String,
    val target: String
)

/**
 * Buildmodehistorysnapshot.
 */
data class BuildModeHistorySnapshot(
    val playerName: String,
    val timestampMillis: Long,
    val action: String,
    val style: String?,
    val target: String?
)

/**
 * Questsample.
 */
data class QuestSample(
    val templateId: String?,
    val mechanic: String?,
    val playerUuid: String?,
    val status: String?
)
