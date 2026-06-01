package ro.ainpc.npc

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.persistence.PersistentDataType
import ro.ainpc.AINPCPlugin
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AINPC(val plugin: AINPCPlugin?) {
    var databaseId = 0
    var uuid: UUID = UUID.randomUUID()
    var name: String = ""
    var displayName: String? = null

    var worldName: String? = null
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var yaw = 0f
    var pitch = 0f

    var skinTexture: String? = null
    var skinSignature: String? = null

    var backstory: String? = null
    var occupation: String? = null
    var age = 30
    var gender = "male"
    var profileSource = "manual"
    var profileSummary = ""
    var profileDataJson = "{}"
    var profileVersion = 1
    var profileCreated = false
    var sourceKey = ""

    var hungerLevel = 82
    var energyLevel = 78
    var socialNeedLevel = 72
    var comfortLevel = 70
    var safetyLevel = 84
    var currentGoal = ""
    var plannedRoutineActivity = ""
    var lastSimulationTickAt = 0L
    var homeAnchor: OwnedLocation? = null
    var workAnchor: OwnedLocation? = null
    var socialAnchor: OwnedLocation? = null

    var personality = NPCPersonality()
    var emotions = NPCEmotions()
    var context = NPCContext(this)
    var currentState = NPCState.IDLE
    var traits: MutableList<String> = ArrayList()
    var bukkitEntity: Entity? = null
    var spawned = false

    fun spawn(): Boolean {
        val worldId = worldName
        val world = if (worldId.isNullOrBlank()) null else Bukkit.getWorld(worldId)
        if (world == null) {
            plugin?.logger?.warning("Lumea '$worldName' nu a fost gasita pentru NPC: $name")
            return false
        }
        val location = Location(world, x, y, z, yaw, pitch)
        val villager = world.spawn(location, Villager::class.java) { spawnedVillager ->
            applyPersistentIdentity(spawnedVillager)
            spawnedVillager.profession = villagerProfession
            spawnedVillager.villagerType = Villager.Type.PLAINS
            applyControlledVillagerDefaults(spawnedVillager)
        }
        attachToVillager(villager)
        plugin?.debug("NPC '$name' spawnat la $location")
        return true
    }

    fun attachToVillager(villager: Villager?) {
        if (villager == null) return
        bukkitEntity = villager
        spawned = villager.isValid
        uuid = villager.uniqueId
        syncLocation(villager.location)
        villager.customName(coloredDisplayNameComponent)
        villager.isCustomNameVisible = true
        applyControlledVillagerDefaults(villager)
        if (!occupation.isNullOrBlank() && shouldApplyProfessionToVillager(villager)) {
            villager.profession = villagerProfession
        }
        applyPersistentIdentity(villager)
    }

    fun applyPersistentIdentity() {
        applyPersistentIdentity(bukkitEntity)
    }

    fun applyPersistentIdentity(entity: Entity?) {
        if (entity == null) return
        val data = entity.persistentDataContainer
        val ownerPlugin = plugin ?: return
        data.set(persistentKey(ownerPlugin, PDC_MANAGED_KEY), PersistentDataType.INTEGER, 1)
        if (databaseId > 0) data.set(persistentKey(ownerPlugin, PDC_DATABASE_ID_KEY), PersistentDataType.INTEGER, databaseId) else data.remove(persistentKey(ownerPlugin, PDC_DATABASE_ID_KEY))
        if (databaseId > 0) data.set(persistentKey(ownerPlugin, PDC_UUID_KEY), PersistentDataType.STRING, uuid.toString()) else data.remove(persistentKey(ownerPlugin, PDC_UUID_KEY))
        if (!name.isBlank()) data.set(persistentKey(ownerPlugin, PDC_NAME_KEY), PersistentDataType.STRING, name) else data.remove(persistentKey(ownerPlugin, PDC_NAME_KEY))
        if (sourceKey.isNotBlank()) data.set(persistentKey(ownerPlugin, PDC_SOURCE_KEY), PersistentDataType.STRING, sourceKey) else data.remove(persistentKey(ownerPlugin, PDC_SOURCE_KEY))
    }

    private fun persistentKey(plugin: AINPCPlugin, key: String): NamespacedKey = NamespacedKey(plugin, key)

    private fun applyControlledVillagerDefaults(villager: Villager?) {
        if (villager == null) return
        villager.setAI(configBoolean("npc.natural_movement", true))
        villager.setGravity(configBoolean("npc.gravity", true))
        villager.isInvulnerable = configBoolean("npc.invulnerable", true)
        villager.isSilent = configBoolean("npc.silent", false)
        villager.isCollidable = configBoolean("npc.collidable", true)
        villager.isPersistent = true
        villager.setRemoveWhenFarAway(false)
    }

    fun applyControlledEntitySettings(): Boolean {
        if (bukkitEntity !is Villager) return false
        val villager = bukkitEntity as Villager
        if (!villager.isValid) return false
        val expectedAi = configBoolean("npc.natural_movement", true)
        val expectedGravity = configBoolean("npc.gravity", true)
        val expectedInvulnerable = configBoolean("npc.invulnerable", true)
        val expectedSilent = configBoolean("npc.silent", false)
        val expectedCollidable = configBoolean("npc.collidable", true)
        val changed =
            villager.hasAI() != expectedAi ||
                villager.hasGravity() != expectedGravity ||
                villager.isInvulnerable != expectedInvulnerable ||
                villager.isSilent != expectedSilent ||
                villager.isCollidable != expectedCollidable ||
                !villager.isPersistent ||
                villager.getRemoveWhenFarAway()
        applyControlledVillagerDefaults(villager)
        return changed
    }

    private fun configBoolean(path: String, fallback: Boolean): Boolean = plugin?.config?.getBoolean(path, fallback) ?: fallback

    fun despawn() {
        if (bukkitEntity != null && bukkitEntity!!.isValid) {
            bukkitEntity!!.remove()
        }
        bukkitEntity = null
        spawned = false
        plugin?.debug("NPC '$name' despawnat.")
    }

    fun markEntityUnavailable() {
        if (bukkitEntity != null && bukkitEntity!!.isValid) syncLocation(bukkitEntity!!.location)
        bukkitEntity = null
        spawned = false
    }

    fun teleport(location: Location) {
        syncLocation(location)
        if (spawned && bukkitEntity != null) bukkitEntity!!.teleport(location)
    }

    fun lookAt(player: Player) {
        if (!isSpawned()) return
        val npcLoc = bukkitEntity!!.location
        val playerLoc = player.location
        val dx = playerLoc.x - npcLoc.x
        val dy = playerLoc.y - npcLoc.y
        val dz = playerLoc.z - npcLoc.z
        val distXZ = sqrt(dx * dx + dz * dz)
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = Math.toDegrees(-atan2(dy, distXZ)).toFloat()
        val newLoc = npcLoc.clone()
        newLoc.yaw = yaw
        newLoc.pitch = pitch
        bukkitEntity!!.teleport(newLoc)
        syncLocation(newLoc)
    }

    fun isInRange(player: Player): Boolean {
        if (!isSpawned()) return false
        val maxDistance = plugin?.config?.getDouble("npc.interaction_distance", 5.0) ?: 5.0
        return bukkitEntity!!.location.distanceSquared(player.location) <= maxDistance * maxDistance
    }

    fun getLocation(): Location? {
        if (isSpawned()) {
            syncLocationFromEntity()
            return bukkitEntity!!.location
        }
        val worldId = worldName ?: return null
        val world = Bukkit.getWorld(worldId) ?: return null
        return Location(world, x, y, z, yaw, pitch)
    }

    private val villagerProfession: Villager.Profession
        get() {
            val value = occupation?.lowercase()?.trim() ?: return Villager.Profession.NONE
            val minecraftProfession = value
                .removePrefix("minecraft:")
                .removePrefix("villager:")
                .takeIf { value.startsWith("minecraft:") || value.startsWith("villager:") }
                ?: return Villager.Profession.NONE
            return Registry.VILLAGER_PROFESSION
                .get(NamespacedKey.minecraft(minecraftProfession.lowercase()))
                ?: Villager.Profession.NONE
        }

    private fun shouldApplyProfessionToVillager(villager: Villager?): Boolean {
        if (villager == null) return false
        if ("auto".equals(profileSource, ignoreCase = true) && villager.hasAI()) return false
        return true
    }

    fun getColoredDisplayName(): String {
        val emotionColor = emotions.dominantEmotionColor
        val nameToShow = displayName ?: name
        return emotionColor + nameToShow
    }

    val coloredDisplayNameComponent: Component
        get() = LEGACY_SERIALIZER.deserialize(getColoredDisplayName())

    fun updateDisplayName() {
        if (isSpawned()) bukkitEntity!!.customName(coloredDisplayNameComponent)
    }

    fun syncLocationFromEntity() {
        if (bukkitEntity != null && bukkitEntity!!.isValid) {
            syncLocation(bukkitEntity!!.location)
            spawned = true
            applyControlledEntitySettings()
        } else if (bukkitEntity != null) {
            markEntityUnavailable()
        }
    }

    fun changeState(newState: NPCState): Boolean {
        if (currentState.getPriority() > newState.getPriority() && currentState.getPriority() >= 60) {
            plugin?.debug("NPC $name nu poate trece din $currentState la $newState")
            return false
        }
        val oldState = currentState
        currentState = newState
        plugin?.debug("NPC $name stare: $oldState -> $newState")
        return true
    }

    fun updateContext() {
        if (isSpawned()) context.updateFromWorld(bukkitEntity!!.world, bukkitEntity!!.location)
    }

    fun addTrait(traitId: String) {
        if (!traits.contains(traitId)) traits.add(traitId)
    }

    fun removeTrait(traitId: String) {
        traits.remove(traitId)
    }

    fun hasTrait(traitId: String): Boolean = traits.contains(traitId)

    fun generateContextDescription(): String {
        val sb = StringBuilder()
        sb.append("Nume: ").append(name).append("\n")
        sb.append("Varsta: ").append(age).append(" ani\n")
        sb.append("Gen: ").append(if (gender == "male") "Barbat" else "Femeie").append("\n")
        if (!occupation.isNullOrEmpty()) sb.append("Ocupatie: ").append(occupation).append("\n")
        if (!backstory.isNullOrEmpty()) sb.append("Poveste: ").append(backstory).append("\n")
        sb.append("\nPersonalitate:\n").append(personality.getDescription()).append("\n")
        sb.append("\nStare emotionala curenta:\n").append(emotions.getDescription()).append("\n")
        sb.append("\nStare curenta: ").append(currentState.displayName).append("\n")
        if (plannedRoutineActivity.isNotBlank()) sb.append("Rutina actuala: ").append(plannedRoutineActivity).append("\n")
        if (currentGoal.isNotBlank()) sb.append("Obiectiv imediat: ").append(currentGoal).append("\n")
        sb.append("Nevoi: satietate ").append(hungerLevel)
            .append("/100, energie ").append(energyLevel)
            .append("/100, social ").append(socialNeedLevel)
            .append("/100, confort ").append(comfortLevel)
            .append("/100, siguranta ").append(safetyLevel)
            .append("/100\n")
        appendOwnedLocation(sb, "Casa", homeAnchor)
        appendOwnedLocation(sb, "Loc de munca", workAnchor)
        appendOwnedLocation(sb, "Loc social", socialAnchor)
        if (traits.isNotEmpty()) sb.append("\nTrasaturi speciale: ").append(traits.joinToString(", ")).append("\n")
        return sb.toString()
    }

    private fun appendOwnedLocation(sb: StringBuilder, label: String, anchor: OwnedLocation?) {
        if (anchor == null) return
        sb.append(label).append(": ").append(anchor.label()).append(" (")
            .append(anchor.worldName()).append(" ")
            .append(floor(anchor.x()).toInt()).append(",")
            .append(floor(anchor.y()).toInt()).append(",")
            .append(floor(anchor.z()).toInt()).append(")\n")
    }

    fun isSpawned(): Boolean {
        if (!spawned) return false
        if (bukkitEntity == null || !bukkitEntity!!.isValid) {
            markEntityUnavailable()
            return false
        }
        return true
    }

    fun isProfileCreated(): Boolean = profileCreated

    @get:JvmName("getLocationProperty")
    val location: Location?
        get() = getLocation()

    @get:JvmName("isSpawnedProperty")
    val isSpawnedProperty: Boolean
        get() = isSpawned()

    fun setLocation(worldName: String?, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        this.worldName = worldName
        this.x = x
        this.y = y
        this.z = z
        this.yaw = yaw
        this.pitch = pitch
    }

    private fun syncLocation(location: Location?) {
        if (location == null || location.world == null) return
        worldName = location.world.name
        x = location.x
        y = location.y
        z = location.z
        yaw = location.yaw
        pitch = location.pitch
    }

    private fun clampNeed(value: Int): Int = max(0, min(100, value))

    class OwnedLocation(
        private val type: String,
        private val label: String,
        private val worldName: String,
        private val x: Double,
        private val y: Double,
        private val z: Double
    ) {
        fun type(): String = type
        fun label(): String = label
        fun worldName(): String = worldName
        fun x(): Double = x
        fun y(): Double = y
        fun z(): Double = z

        fun toLocation(): Location? {
            val world: World = Bukkit.getWorld(worldName) ?: return null
            return Location(world, x, y, z)
        }

        fun isNear(location: Location?, radius: Double): Boolean {
            if (location == null || location.world == null || worldName.isBlank()) return false
            if (location.world.name != worldName) return false
            val anchor = toLocation() ?: return false
            return anchor.distanceSquared(location) <= radius * radius
        }
    }

    companion object {
        private val LEGACY_SERIALIZER: LegacyComponentSerializer = LegacyComponentSerializer.legacySection()

        @JvmField
        val PDC_MANAGED_KEY: String = "npc_managed"

        @JvmField
        val PDC_DATABASE_ID_KEY: String = "npc_database_id"

        @JvmField
        val PDC_UUID_KEY: String = "npc_uuid"

        @JvmField
        val PDC_NAME_KEY: String = "npc_name"

        @JvmField
        val PDC_SOURCE_KEY: String = "npc_source_key"
    }
}
