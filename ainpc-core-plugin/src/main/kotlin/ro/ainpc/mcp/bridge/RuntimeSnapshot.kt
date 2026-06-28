package ro.ainpc.mcp.bridge

data class RuntimeSnapshot(
    val schemaVersion: Int = 1,
    val timestamp: String,
    val plugin: PluginSnapshot,
    val features: FeatureSnapshot,
    val npc: NpcSnapshot,
    val world: WorldSnapshot,
    val quests: QuestSnapshot
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

data class QuestSample(
    val templateId: String?,
    val mechanic: String?,
    val playerUuid: String?,
    val status: String?
)
