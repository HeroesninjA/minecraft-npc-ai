package ro.ainpc.engine

import org.bukkit.Location
import org.bukkit.World
import ro.ainpc.npc.AINPC
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class ScenarioSessionRegistry {
    val activeScenarios = ConcurrentHashMap<UUID, ActiveScenario>()

    @Synchronized
    fun start(template: ScenarioTemplate, anchorLocation: Location): ActiveScenario? {
        if (template.templateId.isBlank()) return null
        val initialPhase = template.phases.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        if (activeScenarios.values.any { it.templateId.equals(template.templateId, ignoreCase = true) }) return null

        val scenario = ActiveScenario(UUID.randomUUID(), template)
        scenario.currentPhase = initialPhase
        scenario.anchorLocation = anchorLocation.clone()
        activeScenarios[scenario.id] = scenario
        return scenario
    }

    @Synchronized
    fun restore(
        template: ScenarioTemplate,
        snapshot: ScenarioSessionSnapshot,
        world: World,
        transientActorReady: (AINPC) -> Boolean = { false },
        actorById: (Int) -> AINPC? = { null },
    ): ActiveScenario? {
        if (!ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
                snapshot, template, actorById, transientActorReady = transientActorReady,
            )) return null
        if (world.name != snapshot.anchor.world) return null
        if (activeScenarios.containsKey(snapshot.scenarioId) ||
            activeScenarios.values.any { it.templateId.equals(snapshot.templateId, ignoreCase = true) }
        ) return null

        val scenario = ActiveScenario(snapshot.scenarioId, template, snapshot.startedAt)
        scenario.currentPhase = snapshot.phase
        scenario.anchorLocation = Location(world, snapshot.anchor.x, snapshot.anchor.y, snapshot.anchor.z,
            snapshot.anchor.yaw, snapshot.anchor.pitch)
        snapshot.actors.forEach { (actorId, actor) -> scenario.assignActor(actorId, actor.uuid, actor.expiresAt) }
        scenario.retiredActorIds.addAll(snapshot.retiredActorIds)
        activeScenarios[scenario.id] = scenario
        return scenario
    }
}
