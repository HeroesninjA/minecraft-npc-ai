package ro.ainpc.npc

enum class NpcLifecycleType {
    PERMANENT,
    TEMPORARY,
    EPISODIC,
    SCENE_ONLY
}

enum class NpcPersistenceMode {
    FULL,
    LIGHT,
    RUNTIME_ONLY
}

enum class NpcSimulationMode {
    FULL,
    LIGHT,
    NONE
}

enum class NpcInteractionProfile {
    FULL_AI,
    QUEST_ONLY,
    SHOP_ONLY,
    SCENE_ONLY,
    MINIMAL
}

enum class NpcEntityKind {
    VILLAGER,
    HUMANOID,
    MONSTER,
    ANIMAL,
    SPIRIT,
    MARKER,
    NONE
}

enum class NpcSpawnPolicy {
    AUTO,
    PHASE,
    STAGE,
    MANUAL
}

data class NpcScenarioActorDefinition(
    val id: String,
    val name: String,
    val lifecycleType: NpcLifecycleType = NpcLifecycleType.TEMPORARY,
    val persistenceMode: NpcPersistenceMode = NpcPersistenceMode.LIGHT,
    val simulationMode: NpcSimulationMode = NpcSimulationMode.LIGHT,
    val interactionProfile: NpcInteractionProfile = NpcInteractionProfile.MINIMAL,
    val entityKind: NpcEntityKind = NpcEntityKind.VILLAGER,
    val entityArchetype: String = "",
    val ownerScenarioId: String = "",
    val ownerQuestId: String = "",
    val spawnSource: String = "",
    val spawnPhase: String = "",
    val spawnPolicy: NpcSpawnPolicy = NpcSpawnPolicy.AUTO,
    val despawnRule: String = "",
    val durationSeconds: Long? = null,
    val temporaryTags: Set<String> = emptySet()
) {
    fun isRuntimeOnly(): Boolean = persistenceMode == NpcPersistenceMode.RUNTIME_ONLY

    fun hasScenarioOwnership(): Boolean = ownerScenarioId.isNotBlank() || ownerQuestId.isNotBlank()
}
