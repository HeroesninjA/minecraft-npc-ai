package ro.ainpc.engine

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import ro.ainpc.ai.NpcFoodRequestReply
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcInteractionProfile
import ro.ainpc.npc.NpcSelfCareThresholds
import ro.ainpc.npc.NpcSimulationMode
import java.util.UUID

internal class NpcFoodRequestService(
    private val config: () -> ConfigurationSection,
    private val isNpcInConversation: (UUID) -> Boolean,
    private val isPlayerBusy: (Player) -> Boolean,
    private val send: (Player, AINPC, String) -> Unit,
    private val isMainThread: () -> Boolean = Bukkit::isPrimaryThread,
    private val throttle: NpcFoodRequestThrottle = NpcFoodRequestThrottle(),
    private val findNpc: (UUID) -> AINPC? = { null },
    private val followUps: NpcFoodRequestFollowUps = NpcFoodRequestFollowUps(),
) {
    fun tryRequest(npc: AINPC, now: Long = System.currentTimeMillis()): Boolean {
        if (!isMainThread() || now <= 0L) return false
        val settings = config()
        if (!settings.getBoolean("simulation.food_requests_enabled", true) ||
            !settings.getBoolean("simulation.food_help_enabled", true) ||
            npc.simulationMode != NpcSimulationMode.FULL ||
            npc.interactionProfile != NpcInteractionProfile.FULL_AI || !npc.isSpawned()
        ) return false

        val entity = npc.bukkitEntity as? Villager ?: return false
        val state = NpcCognitiveState.capture(npc)
        if (entity.isDead || state.hungerLevel !in 0 until NpcSelfCareThresholds.START ||
            state.safetyLevel !in 40..100 || state.focus != NpcCognitiveState.Focus.HUNGER ||
            state.currentState !in AVAILABLE_STATES || state.hasInteractingPlayer ||
            isNpcInConversation(npc.uuid)
        ) return false

        val radius = settings.getDouble("npc.interaction_distance", 5.0)
        if (!radius.isFinite() || radius <= 0.0) return false
        val maxDistanceSquared = radius.coerceAtMost(6.0).let { it * it }
        val candidates = npc.context.nearbyPlayers.mapNotNull { player ->
            if (npc.socialExperience.latestFor(player.uniqueId, now) == null ||
                !npc.foodRequestPreferences.canAsk(player.uniqueId, now) ||
                !followUps.canRecord(player.uniqueId, now) || isPlayerBusy(player)
            ) return@mapNotNull null
            val distanceSquared = visibleDistanceSquared(player, entity, maxDistanceSquared) ?: return@mapNotNull null
            player to distanceSquared
        }.sortedWith(compareBy<Pair<Player, Double>> { it.second }.thenBy { it.first.uniqueId })

        for ((player, _) in candidates) {
            val receipt = npc.socialExperience.latestFor(player.uniqueId, now) ?: continue
            val request = NpcFoodRequestFollowUps.Request(
                npc.uuid, npc.runtimeIdentity, npc.decisionRevision, player.uniqueId, receipt.id, now,
            )
            val delivered = try {
                throttle.deliver(npc.uuid, player.uniqueId, now) {
                    send(player, npc, REQUEST_MESSAGE)
                    followUps.record(request)
                }
            } catch (_: RuntimeException) {
                return false
            }
            if (delivered) return true
        }
        return false
    }

    fun tryReply(
        player: Player, message: String, now: Long = System.currentTimeMillis(),
        beforeAccept: (AINPC) -> Boolean = { true },
    ): Boolean {
        if (!isMainThread() || now <= 0L) return false
        val request = followUps.find(player.uniqueId, now) ?: return false
        val npc = findNpc(request.npcUuid) ?: return false
        val reply = NpcFoodRequestReply.parse(message, listOfNotNull(npc.name, npc.displayName)) ?: return false
        if (!canReply(npc, player, request, now)) return false
        if (!beforeAccept(npc)) return true
        if (!canReply(npc, player, request, now) || !followUps.consume(request, now)) return true
        if (!npc.foodRequestPreferences.defer(player.uniqueId, reply, now)) return true
        try {
            send(player, npc, reply.acknowledgement)
        } catch (_: RuntimeException) {
            return true
        }
        return true
    }

    fun forgetPlayer(playerUuid: UUID) {
        followUps.forgetPlayer(playerUuid)
    }

    private fun canReply(npc: AINPC, player: Player, request: NpcFoodRequestFollowUps.Request, now: Long): Boolean {
        val settings = config()
        if (!settings.getBoolean("simulation.food_requests_enabled", true) ||
            !settings.getBoolean("simulation.food_help_enabled", true) ||
            findNpc(request.npcUuid) !== npc || npc.runtimeIdentity != request.npcRuntimeIdentity ||
            npc.decisionRevision != request.npcRevision || npc.simulationMode != NpcSimulationMode.FULL ||
            npc.interactionProfile != NpcInteractionProfile.FULL_AI || !npc.isSpawned() ||
            isNpcInConversation(npc.uuid) || isPlayerBusy(player) ||
            npc.socialExperience.latestFor(player.uniqueId, now)?.id != request.receiptId ||
            !npc.foodRequestPreferences.canAsk(player.uniqueId, now)
        ) return false
        val state = NpcCognitiveState.capture(npc)
        if (state.isInDanger || state.safetyLevel !in 40..100 ||
            state.hungerLevel !in 0 until NpcSelfCareThresholds.START || state.hasInteractingPlayer ||
            !state.currentState.allowsPlayerInteraction()
        ) return false
        val entity = npc.bukkitEntity as? Villager ?: return false
        if (entity.isDead) return false
        val radius = settings.getDouble("npc.interaction_distance", 5.0)
        if (!radius.isFinite() || radius <= 0.0) return false
        return visibleDistanceSquared(player, entity, radius.coerceAtMost(6.0).let { it * it }) != null
    }

    private fun visibleDistanceSquared(player: Player, entity: Villager, maxDistanceSquared: Double): Double? {
        if (!player.isOnline || !player.isConnected || !player.isValid || player.isDead ||
            player.gameMode == GameMode.SPECTATOR || player.isInvisible ||
            player.getMetadata("vanished").any { it.asBoolean() }
        ) return null
        val location = entity.location
        val world = location.world ?: return null
        val playerLocation = player.location
        if (playerLocation.world != world) return null
        val distanceSquared = location.distanceSquared(playerLocation)
        return distanceSquared.takeIf {
            it.isFinite() && it <= maxDistanceSquared && player.canSee(entity) && entity.hasLineOfSight(player)
        }
    }

    companion object {
        private val AVAILABLE_STATES = setOf(
            NPCState.IDLE, NPCState.WALKING, NPCState.WAITING, NPCState.RESTING, NPCState.CURIOUS,
        )
        private const val REQUEST_MESSAGE =
            "Mi-e foame. M-ai ajutat cu mancare inainte; ma poti ajuta din nou? " +
                "Poti sa-mi oferi paine sau un mar cu Shift + click dreapta, tinand mancarea in mana principala. " +
                "Poti raspunde acum cu 'nu pot acum' sau 'iti aduc mancare'."
    }
}
