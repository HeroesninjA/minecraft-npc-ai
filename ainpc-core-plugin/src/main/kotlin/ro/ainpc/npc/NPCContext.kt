@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.npc

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.context.NPCContextUpdatedEvent
import ro.ainpc.api.events.context.NPCContextUpdatedEventPayload
import ro.ainpc.api.events.context.WorldContextBuiltEvent
import ro.ainpc.api.events.context.WorldContextBuiltEventPayload
import ro.ainpc.environment.EnvironmentContext
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.topology.TopologyCategory
import ro.ainpc.world.WorldContextSnapshot
import ro.ainpc.world.WorldContextSnapshotBuilder
import java.util.Locale
import java.util.UUID

/**
 * Contextul curent al unui NPC - informatii despre mediu si situatie
 * Folosit pentru luarea deciziilor si generarea dialogului
 */
/**
 * Npccontext.
 */
class NPCContext(
    // NPC-ul asociat
    val npc: AINPC
) {
    // Timp si locatie
    var worldTime: Long = 0L
        private set
    var timeOfDay: String? = null // MORNING, AFTERNOON, EVENING, NIGHT
        private set
    var weather: String? = null // CLEAR, RAIN, THUNDER, SNOW
        private set
    var biome: String? = null
        private set
    var topologyCategory: TopologyCategory? = TopologyCategory.UNKNOWN
        private set
    var isIndoors: Boolean = false
        private set

    // Entitati din apropiere
    var nearbyPlayers: MutableList<Player> = mutableListOf()
        private set
    var nearbyNPCs: MutableList<AINPC> = mutableListOf()
        private set
    var nearbyHostileMobs: Int = 0
        private set
    var nearbyPassiveMobs: Int = 0
        private set

    // Interactiune curenta
    var interactingPlayer: Player? = null
        private set
    var interactingNPC: AINPC? = null
    var lastPlayerMessage: String? = null
    var lastInteractionTime: Long = 0L
        private set

    // Stare fizica
    var healthPercent: Double = 100.0
    var hungerLevel: Int = npc.hungerLevel
    var energyLevel: Int = npc.energyLevel
    var socialNeedLevel: Int = npc.socialNeedLevel
    var comfortLevel: Int = npc.comfortLevel
    var safetyLevel: Int = npc.safetyLevel
    var isHurt: Boolean = false
    var isInDanger: Boolean = false

    // Stare sociala
    var isAtHome: Boolean = false
    var isAtWork: Boolean = false
    var isAtSocialSpot: Boolean = false
    var isFamilyNearby: Boolean = false
    var isFriendsNearby: Boolean = false

    private var lastNearbyUpdateTick: Int = -1

    // Evenimente recente
    var recentEvents: MutableList<String> = mutableListOf()
        private set
    var lastSignificantEvent: String? = null
        private set

    // Informatii despre relatia cu jucatorul curent
    var relationshipLevel: Int = 0
        set(value) {
            field = value
            relationshipStatus = when {
                value < -50 -> "ENEMY"
                value < 0 -> "STRANGER"
                value < 25 -> "ACQUAINTANCE"
                value < 75 -> "FRIEND"
                else -> "CLOSE_FRIEND"
            }
        }
    var relationshipStatus: String = "STRANGER" // STRANGER, ACQUAINTANCE, FRIEND, CLOSE_FRIEND, ENEMY
    var sharedMemories: MutableList<String> = mutableListOf()
    var plannedRoutineActivity: String = ""
    var currentGoal: String = ""
    var worldContextSnapshot: WorldContextSnapshot = WorldContextSnapshot.empty()
        private set
    var effectiveRegionStoryState: String? = null
        internal set
    var effectiveRegionStoryMode: String? = null
        internal set
    var effectivePlaceStoryState: String? = null
        internal set
    var environmentContext: EnvironmentContext = EnvironmentContext.empty()
        private set

    /**
     * Actualizeaza contextul bazat pe lumea curenta
     */
    fun updateFromWorld(world: World?, npcLocation: Location?) {
        if (world == null || npcLocation == null) return

        worldTime = world.time
        timeOfDay = calculateTimeOfDay(worldTime)

        weather = if (world.hasStorm()) {
            if (world.isThundering) "THUNDER" else "RAIN"
        } else {
            "CLEAR"
        }

        biome = npcLocation.block.biome.toString().uppercase(Locale.ROOT)
        isIndoors = npcLocation.block.lightFromSky < 10
        topologyCategory = TopologyCategory.fromBiome(biome, isIndoors)
        syncSimulationState(npcLocation)
        updateNearbyEntities(npcLocation)
        updateWorldContextSnapshot(npcLocation)
        refreshEnvironmentContext(npcLocation)
        publishNpcContextUpdated("update_from_world")
    }

    /**
     * Calculeaza momentul zilei bazat pe tick-uri
     */
    private fun calculateTimeOfDay(time: Long): String {
        // Minecraft: 0 = rasarit, 6000 = amiaza, 12000 = apus, 18000 = miezul noptii
        if (time >= 0 && time < 6000) return "MORNING"
        if (time >= 6000 && time < 12000) return "AFTERNOON"
        if (time >= 12000 && time < 18000) return "EVENING"
        return "NIGHT"
    }

    /**
     * Actualizeaza lista entitatilor din apropiere
     */
    private fun updateNearbyEntities(location: Location) {
        val currentTick = Bukkit.getCurrentTick()
        if (currentTick == lastNearbyUpdateTick) return
        lastNearbyUpdateTick = currentTick

        nearbyPlayers.clear()
        nearbyNPCs.clear()
        nearbyHostileMobs = 0
        nearbyPassiveMobs = 0
        pruneStaleInteraction()

        location.world.getNearbyEntities(location, NEARBY_SCAN_RANGE, NEARBY_SCAN_RANGE, NEARBY_SCAN_RANGE).forEach { entity ->
            if (entity is Player) {
                nearbyPlayers.add(entity)
            } else if (isHostileMob(entity.type.name)) {
                nearbyHostileMobs++
            } else if (isPassiveMob(entity.type.name)) {
                nearbyPassiveMobs++
            } else {
                val nearbyNpc = npc.plugin?.npcManager?.getNPCByEntity(entity)
                if (nearbyNpc != null && nearbyNpc.uuid != npc.uuid) {
                    nearbyNPCs.add(nearbyNpc)
                }
            }
        }

        isFriendsNearby = nearbyNPCs.isNotEmpty()
        isInDanger = nearbyHostileMobs > 0 || isHurt || safetyLevel < 35
    }

    private fun updateWorldContextSnapshot(npcLocation: Location?) {
        updateWorldContextSnapshot(npcLocation, true)
    }

    internal fun refreshWorldContextSnapshot(npcLocation: Location?) {
        updateWorldContextSnapshot(npcLocation, false)
    }

    private fun updateWorldContextSnapshot(npcLocation: Location?, publishEvents: Boolean) {
        effectiveRegionStoryState = null
        effectiveRegionStoryMode = null
        effectivePlaceStoryState = null
        if (npcLocation == null || npc.plugin == null || npc.plugin.platform == null) {
            worldContextSnapshot = WorldContextSnapshot.empty()
            return
        }

        val snapshot = WorldContextSnapshotBuilder(
            npc.plugin.platform.worldAdmin
        ).build(npcLocation, npc, nearbyNPCs)
        worldContextSnapshot = snapshot
        if (publishEvents && !snapshot.isEmpty()) {
            publishWorldContextBuilt(snapshot)
        }
    }

    fun syncSimulationState(npcLocation: Location?) {
        hungerLevel = npc.hungerLevel
        energyLevel = npc.energyLevel
        socialNeedLevel = npc.socialNeedLevel
        comfortLevel = npc.comfortLevel
        safetyLevel = npc.safetyLevel
        plannedRoutineActivity = npc.plannedRoutineActivity
        currentGoal = npc.currentGoal
        val homeAnchor = npc.homeAnchor
        val workAnchor = npc.workAnchor
        val socialAnchor = npc.socialAnchor
        isAtHome = homeAnchor != null && homeAnchor.isNear(npcLocation, 5.5)
        isAtWork = workAnchor != null && workAnchor.isNear(npcLocation, 6.5)
        isAtSocialSpot = socialAnchor != null && socialAnchor.isNear(npcLocation, 6.5)
        if (npcLocation != null && npcLocation.world != null) {
            healthPercent = 100.0
        }
        isInDanger = nearbyHostileMobs > 0 || isHurt || safetyLevel < 35
    }

    private fun pruneStaleInteraction() {
        val player = interactingPlayer ?: return
        val expired = System.currentTimeMillis() - lastInteractionTime > 120_000L
        if (expired || !player.isOnline) {
            interactingPlayer = null
            lastPlayerMessage = null
        }
    }

    private fun isHostileMob(type: String): Boolean {
        return when (type) {
            "ZOMBIE", "SKELETON", "CREEPER", "SPIDER", "ENDERMAN",
            "WITCH", "PILLAGER", "VINDICATOR", "RAVAGER", "WARDEN" -> true
            else -> false
        }
    }

    private fun isPassiveMob(type: String): Boolean {
        return when (type) {
            "COW", "SHEEP", "PIG", "CHICKEN", "HORSE", "DONKEY",
            "CAT", "DOG", "WOLF", "RABBIT", "FOX" -> true
            else -> false
        }
    }

    /**
     * Seteaza jucatorul cu care NPC-ul interactioneaza
     */
    fun setInteractingPlayer(player: Player?) {
        interactingPlayer = player
        lastInteractionTime = System.currentTimeMillis()
    }

    /**
     * Adauga un eveniment recent
     */
    internal fun refreshEnvironmentContext(npcLocation: Location?) {
        if (npcLocation == null || npc.plugin == null) {
            environmentContext = EnvironmentContext.empty()
            return
        }
        environmentContext = npc.plugin.environmentEngine.getContextForLocation(
            npcLocation.world.name,
            npcLocation.block.biome.key.asString(),
            npcLocation.block.lightLevel.toInt(),
        )
    }

    fun addRecentEvent(event: String) {
        recentEvents.add(0, event)
        if (recentEvents.size > 10) {
            recentEvents.removeAt(recentEvents.size - 1)
        }
        lastSignificantEvent = event
    }

    /**
     * Genereaza o descriere textuala a contextului pentru AI
     */
    @JvmOverloads
    fun generateContextDescription(player: Player? = interactingPlayer): String =
        generateContextDescription(player, null)

    fun generateContextDescription(player: Player?, preloadedStory: StoryContextSnapshot?): String =
        generateContextDescription(player, preloadedStory, NpcCognitiveState.capture(npc, this))

    internal fun generateContextDescription(
        player: Player?, preloadedStory: StoryContextSnapshot?, cognition: NpcCognitiveState,
    ): String = generateContextDescription(player, preloadedStory, cognition, NpcPerceptionSnapshot.capture(this))

    internal fun generateContextDescription(
        player: Player?, preloadedStory: StoryContextSnapshot?, cognition: NpcCognitiveState,
        perception: NpcPerceptionSnapshot,
    ): String {
        val sb = StringBuilder()
        sb.append("Este ").append(getTimeDescription(perception)).append(". ")
        sb.append("Vremea: ").append(getWeatherDescription(perception)).append(".\n")

        if (perception.isIndoors) {
            sb.append("Sunt in interior, intr-un mediu de tip ").append(getTopologyDescription(perception)).append(".\n")
        } else {
            sb.append("Sunt afara, intr-o zona de tip ").append(getTopologyDescription(perception))
                .append(" (").append(getBiomeDescription(perception)).append(").\n")
        }

        val env = perception.environmentContext
        if (env.worldName.isNotBlank()) {
            sb.append("Anotimp: ").append(env.season.displayName).append(". ")
            sb.append("Temperatura: ").append(env.temperature.displayName).append(".\n")
            sb.append("Lumina locala: ").append(
                when {
                    env.lightLevel < 5 -> "slaba"
                    env.lightLevel < 11 -> "moderata"
                    else -> "puternica"
                }
            ).append(".\n")
        }
        if (env.specialEvents.isNotEmpty()) {
            sb.append("Evenimente active: ").append(env.specialEvents.joinToString(", ")).append(".\n")
        }

        if (!worldContextSnapshot.isEmpty()) {
            sb.append(worldContextSnapshot.toPromptBlock()).append("\n")
        }

        if (player != null) {
            val storyContext = preloadedStory ?: npc.plugin?.storyContextService?.buildForNpc(npc, player)
            if (storyContext != null && !storyContext.isEmpty()) {
                sb.append(storyContext.toPromptBlock()).append("\n")
            }
        }

        if (perception.nearbyPlayerCount > 0) {
            sb.append("Vad ").append(perception.nearbyPlayerCount).append(" persoane in apropiere.\n")
        }
        if (perception.nearbyHostileMobs > 0) {
            sb.append("ATENTIE: Sunt ").append(perception.nearbyHostileMobs).append(" creaturi periculoase aproape!\n")
        }
        if (perception.healthPercent < 50) sb.append("Ma simt slabita/slabit (sanatate scazuta).\n")
        if (cognition.hungerLevel < 30) sb.append("Mi-e foame.\n")
        if (cognition.energyLevel < 35) sb.append("Sunt obosit(a) si am nevoie de odihna.\n")
        if (cognition.comfortLevel < 35) sb.append("Nu ma simt prea confortabil in locul acesta.\n")
        if (cognition.safetyLevel < 35) sb.append("Ma simt nesigur(a) si prudenta mea este ridicata.\n")

        if (perception.isFamilyNearby) sb.append("Familia mea e aproape.\n")
        if (perception.isAtWork) sb.append("Sunt la munca.\n")
        else if (perception.isAtHome) sb.append("Sunt acasa.\n")
        else if (perception.isAtSocialSpot) sb.append("Ma aflu intr-un loc social cunoscut.\n")

        if (cognition.plannedRoutineActivity.isNotBlank()) {
            sb.append("Conform rutinei mele, acum ar trebui sa: ").append(cognition.plannedRoutineActivity).append(".\n")
        }
        if (cognition.currentGoal.isNotBlank()) {
            sb.append("Obiectivul meu imediat este sa ").append(cognition.currentGoal).append(".\n")
        }

        if (player != null) {
            sb.append("\nVorbesc cu: ").append(player.name).append("\n")
        }

        if (perception.lastSignificantEvent != null) {
            sb.append("Recent: ").append(perception.lastSignificantEvent).append("\n")
        }
        return sb.toString()
    }

    private fun getTimeDescription(perception: NpcPerceptionSnapshot): String {
        return when (perception.timeOfDay) {
            "MORNING" -> "dimineata"
            "AFTERNOON" -> "dupa-amiaza"
            "EVENING" -> "seara"
            "NIGHT" -> "noapte"
            else -> "zi"
        }
    }

    private fun getWeatherDescription(perception: NpcPerceptionSnapshot): String {
        val environment = perception.environmentContext
        val localWeather = if (environment.worldName.isNotBlank()) environment.weather.name else perception.weather
        return when (localWeather) {
            "RAIN" -> "ploua"
            "THUNDER" -> "furtuna cu tunete"
            "SNOW" -> "ninge"
            else -> "senin"
        }
    }

    private fun getBiomeDescription(perception: NpcPerceptionSnapshot): String {
        val currentBiome = perception.biome ?: return "loc necunoscut"
        return when (perception.topologyCategory) {
            TopologyCategory.INTERIOR -> "spatiu interior"
            TopologyCategory.PLAINS -> "camp deschis"
            TopologyCategory.FOREST -> "padure"
            TopologyCategory.DARK_FOREST -> "padure intunecata"
            TopologyCategory.DESERT -> "zona arida"
            TopologyCategory.MOUNTAIN -> "munte"
            TopologyCategory.SWAMP -> "mlastina"
            TopologyCategory.TAIGA -> "taiga"
            TopologyCategory.SNOW -> "tinut rece"
            TopologyCategory.JUNGLE -> "jungla"
            TopologyCategory.COAST -> "coasta"
            TopologyCategory.RIVER -> "mal de rau"
            TopologyCategory.OCEAN -> "margine de ocean"
            TopologyCategory.UNDERGROUND -> "zona subterana"
            TopologyCategory.NETHER -> "nether"
            TopologyCategory.END -> "end"
            TopologyCategory.UNKNOWN, null -> currentBiome.lowercase(Locale.ROOT).replace("_", " ")
        }
    }

    private fun getTopologyDescription(perception: NpcPerceptionSnapshot): String {
        val category = perception.topologyCategory
        if (category == null || category == TopologyCategory.UNKNOWN) {
            return "topologie necunoscuta"
        }
        return category.displayName.lowercase(Locale.ROOT)
    }

    private fun publishNpcContextUpdated(reason: String) {
        val pl = npc.plugin ?: return
        if (!pl.config.getBoolean("events.context_events_enabled", false)) return
        if (!pl.config.getBoolean("events.public_api_enabled", true)) return
        val event = NPCContextUpdatedEvent(
            NPCContextUpdatedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.SYSTEM,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                timeOfDay,
                weather,
                isIndoors,
                isAtHome,
                isAtWork,
                isAtSocialSpot,
                nearbyPlayers.size,
                nearbyNPCs.size,
                interactingPlayer != null,
                reason,
                mapOf("source" to "NPCContext")
            )
        )
        pl.server.pluginManager.callEvent(event)
    }

    private fun publishWorldContextBuilt(snapshot: WorldContextSnapshot) {
        val pl = npc.plugin ?: return
        if (!pl.config.getBoolean("events.context_events_enabled", false)) return
        if (!pl.config.getBoolean("events.public_api_enabled", true)) return
        val region = snapshot.currentRegion()
        val place = snapshot.currentPlace()
        val worldName = npc.location?.world?.name ?: npc.worldName ?: ""
        val event = WorldContextBuiltEvent(
            WorldContextBuiltEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.SYSTEM,
                worldName,
                region?.id(),
                place?.id(),
                snapshot.nearbyPlaces().size,
                snapshot.nearbyNodes().size,
                npc.databaseId.toString(),
                npc.name,
                mapOf("source" to "NPCContext")
            )
        )
        pl.server.pluginManager.callEvent(event)
    }

    companion object {
        private const val NEARBY_SCAN_RANGE = 20.0
    }
}
