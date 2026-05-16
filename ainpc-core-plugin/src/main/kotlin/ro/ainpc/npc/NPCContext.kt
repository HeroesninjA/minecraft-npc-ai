package ro.ainpc.npc

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.topology.TopologyCategory
import ro.ainpc.world.WorldContextSnapshot
import ro.ainpc.world.WorldContextSnapshotBuilder
import java.util.ArrayList
import java.util.Locale
import kotlin.math.min

/**
 * Contextul curent al unui NPC - informatii despre mediu si situatie
 * Folosit pentru luarea deciziilor si generarea dialogului
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
    var nearbyPlayers: MutableList<Player> = ArrayList()
        private set
    var nearbyNPCs: MutableList<AINPC> = ArrayList()
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

    // Evenimente recente
    var recentEvents: MutableList<String> = ArrayList()
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
    var sharedMemories: MutableList<String> = ArrayList()
    var plannedRoutineActivity: String = ""
    var currentGoal: String = ""
    var worldContextSnapshot: WorldContextSnapshot = WorldContextSnapshot.empty()
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
        nearbyPlayers.clear()
        nearbyNPCs.clear()
        nearbyHostileMobs = 0
        nearbyPassiveMobs = 0
        pruneStaleInteraction()

        val range = 20.0
        location.world.getNearbyEntities(location, range, range, range).forEach { entity ->
            if (entity is Player) {
                nearbyPlayers.add(entity)
            } else if (isHostileMob(entity.type.name)) {
                nearbyHostileMobs++
            } else if (isPassiveMob(entity.type.name)) {
                nearbyPassiveMobs++
            } else {
                val nearbyNpc = npc.plugin.npcManager.getNPCByEntity(entity)
                if (nearbyNpc != null && nearbyNpc.uuid != npc.uuid) {
                    nearbyNPCs.add(nearbyNpc)
                }
            }
        }

        isFriendsNearby = nearbyNPCs.isNotEmpty()
        isInDanger = nearbyHostileMobs > 0 || isHurt || safetyLevel < 35
    }

    private fun updateWorldContextSnapshot(npcLocation: Location?) {
        if (npcLocation == null || npc.plugin == null || npc.plugin.platform == null) {
            worldContextSnapshot = WorldContextSnapshot.empty()
            return
        }

        worldContextSnapshot = WorldContextSnapshotBuilder(
            npc.plugin.platform.worldAdminService
        ).build(npcLocation, npc, nearbyNPCs)
    }

    fun syncSimulationState(npcLocation: Location?) {
        hungerLevel = npc.hungerLevel
        energyLevel = npc.energyLevel
        socialNeedLevel = npc.socialNeedLevel
        comfortLevel = npc.comfortLevel
        safetyLevel = npc.safetyLevel
        plannedRoutineActivity = npc.plannedRoutineActivity
        currentGoal = npc.currentGoal
        isAtHome = npc.homeAnchor != null && npc.homeAnchor.isNear(npcLocation, 5.5)
        isAtWork = npc.workAnchor != null && npc.workAnchor.isNear(npcLocation, 6.5)
        isAtSocialSpot = npc.socialAnchor != null && npc.socialAnchor.isNear(npcLocation, 6.5)
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
    fun generateContextDescription(): String {
        val sb = StringBuilder()
        sb.append("Este ").append(getTimeDescription()).append(". ")
        sb.append("Vremea: ").append(getWeatherDescription()).append(".\n")

        if (isIndoors) {
            sb.append("Sunt in interior, intr-un mediu de tip ").append(getTopologyDescription()).append(".\n")
        } else {
            sb.append("Sunt afara, intr-o zona de tip ").append(getTopologyDescription())
                .append(" (").append(getBiomeDescription()).append(").\n")
        }

        if (!worldContextSnapshot.isEmpty()) {
            sb.append(worldContextSnapshot.toPromptBlock()).append("\n")
        }

        if (interactingPlayer != null && npc.plugin != null && npc.plugin.storyContextService != null) {
            val storyContext: StoryContextSnapshot = npc.plugin.storyContextService.buildForNpc(npc, interactingPlayer)
            if (!storyContext.isEmpty()) {
                sb.append(storyContext.toPromptBlock()).append("\n")
            }
        }

        if (nearbyPlayers.isNotEmpty()) {
            sb.append("Vad ").append(nearbyPlayers.size).append(" persoane in apropiere.\n")
        }
        if (nearbyHostileMobs > 0) {
            sb.append("ATENTIE: Sunt ").append(nearbyHostileMobs).append(" creaturi periculoase aproape!\n")
        }
        if (healthPercent < 50) sb.append("Ma simt slabita/slabit (sanatate scazuta).\n")
        if (hungerLevel < 30) sb.append("Mi-e foame.\n")
        if (energyLevel < 35) sb.append("Sunt obosit(a) si am nevoie de odihna.\n")
        if (comfortLevel < 35) sb.append("Nu ma simt prea confortabil in locul acesta.\n")
        if (safetyLevel < 35) sb.append("Ma simt nesigur(a) si prudenta mea este ridicata.\n")

        if (isFamilyNearby) sb.append("Familia mea e aproape.\n")
        if (isAtWork) sb.append("Sunt la munca.\n")
        else if (isAtHome) sb.append("Sunt acasa.\n")
        else if (isAtSocialSpot) sb.append("Ma aflu intr-un loc social cunoscut.\n")

        if (plannedRoutineActivity.isNotBlank()) {
            sb.append("Conform rutinei mele, acum ar trebui sa: ").append(plannedRoutineActivity).append(".\n")
        }
        if (currentGoal.isNotBlank()) {
            sb.append("Obiectivul meu imediat este sa ").append(currentGoal).append(".\n")
        }

        val player = interactingPlayer
        if (player != null) {
            sb.append("\nVorbesc cu: ").append(player.name).append("\n")
            sb.append("Relatia noastra: ").append(getRelationshipDescription()).append("\n")
            if (sharedMemories.isNotEmpty()) {
                sb.append("Amintiri comune: ")
                    .append(sharedMemories.subList(0, min(3, sharedMemories.size)).joinToString(", "))
                    .append("\n")
            }
        }

        if (lastSignificantEvent != null) {
            sb.append("Recent: ").append(lastSignificantEvent).append("\n")
        }
        return sb.toString()
    }

    private fun getTimeDescription(): String {
        return when (timeOfDay) {
            "MORNING" -> "dimineata"
            "AFTERNOON" -> "dupa-amiaza"
            "EVENING" -> "seara"
            "NIGHT" -> "noapte"
            else -> "zi"
        }
    }

    private fun getWeatherDescription(): String {
        return when (weather) {
            "RAIN" -> "ploua"
            "THUNDER" -> "furtuna cu tunete"
            "SNOW" -> "ninge"
            else -> "senin"
        }
    }

    private fun getBiomeDescription(): String {
        val currentBiome = biome ?: return "loc necunoscut"
        return when (topologyCategory) {
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

    private fun getTopologyDescription(): String {
        val category = topologyCategory
        if (category == null || category == TopologyCategory.UNKNOWN) {
            return "topologie necunoscuta"
        }
        return category.displayName.lowercase(Locale.ROOT)
    }

    private fun getRelationshipDescription(): String {
        return when (relationshipStatus) {
            "STRANGER" -> "strain/straina - nu il/o cunosc"
            "ACQUAINTANCE" -> "cunoscut/cunoscuta - am mai vorbit"
            "FRIEND" -> "prieten/prietena - ne intelegem bine"
            "CLOSE_FRIEND" -> "prieten apropiat - am incredere totala"
            "ENEMY" -> "dusman - nu vreau sa am de-a face"
            "FAMILY" -> "familie - rudenie de sange"
            "SPOUSE" -> "sot/sotie - partener de viata"
            else -> "necunoscut"
        }
    }
}
