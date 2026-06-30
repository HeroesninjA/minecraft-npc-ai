package ro.ainpc.mcp.bridge

data class RuntimeSnapshot(
    val schemaVersion: Int = 2,
    val timestamp: String,
    val plugin: PluginSnapshot,
    val features: FeatureSnapshot,
    val npc: NpcSnapshot,
    val world: WorldSnapshot,
    val quests: QuestSnapshot,
    val buildMode: BuildModeSnapshot
)

data class PluginSnapshot(
    val version: String,
    val serverType: String,
    val onlinePlayers: Int,
    val uptimeMinutes: Long
)

data class FeatureSnapshot(
    val ai: Boolean,
    val mcp: Boolean,
    val aiOrchestration: Boolean
)

data class NpcSnapshot(
    val totalCount: Int,
    val byRegion: Map<String, Int>,
    val samples: List<NpcSample>
)

data class NpcSample(
    val npcId: Int,
    val name: String,
    val profession: String?,
    val regionId: String?,
    val worldName: String?
)

data class WorldSnapshot(
    val regionCount: Int,
    val placeCount: Int,
    val nodeCount: Int,
    val regionNames: List<String>,
    val placeNames: List<String>
)

data class QuestSnapshot(
    val activePlayerQuests: Int,
    val activeGlobalQuests: Int,
    val samples: List<QuestSample>
)

data class BuildModeSnapshot(
    val activePlayers: Int,
    val byStyle: Map<String, Int>,
    val byTarget: Map<String, Int>,
    val players: List<BuildModePlayerSnapshot>,
    val history: List<BuildModeHistorySnapshot>
)

data class BuildModePlayerSnapshot(
    val name: String,
    val style: String,
    val target: String
)

data class BuildModeHistorySnapshot(
    val playerName: String,
    val timestampMillis: Long,
    val action: String,
    val style: String?,
    val target: String?
)

data class QuestSample(
    val templateId: String?,
    val mechanic: String?,
    val playerUuid: String?,
    val status: String?
)
