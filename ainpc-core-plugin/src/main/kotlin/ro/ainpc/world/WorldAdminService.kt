package ro.ainpc.world

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.platform.PlatformProfile
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import java.util.function.Consumer
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class WorldAdminService(
    private val debugSink: Consumer<String>,
    private val logger: Logger
) : WorldAdminApi {
    constructor(plugin: AINPCPlugin) : this(Consumer { message -> plugin.debug(message) }, plugin.logger)

    private val regionsById: MutableMap<String, WorldRegion> = LinkedHashMap()
    private val placesById: MutableMap<String, WorldPlace> = LinkedHashMap()
    private val placesByRegion: MutableMap<String, MutableList<WorldPlace>> = LinkedHashMap()
    private val nodesById: MutableMap<String, WorldNode> = LinkedHashMap()
    private val nodesByRegion: MutableMap<String, MutableList<WorldNode>> = LinkedHashMap()
    private val nodesByPlace: MutableMap<String, MutableList<WorldNode>> = LinkedHashMap()
    private val mappingIndex = MappingIndex()
    private var enabled = true
    private var autoIndexEnabled = true
    private var currentWorldMode = WorldMode.FINITE_DYNAMIC
    private var dirty = false

    override val isEnabled: Boolean
        get() = enabled

    val isAutoIndexEnabled: Boolean
        get() = autoIndexEnabled

    val indexedRegionChunkCount: Int
        get() = mappingIndex.indexedRegionChunks()

    val indexedPlaceChunkCount: Int
        get() = mappingIndex.indexedPlaceChunks()

    val indexedNodeChunkCount: Int
        get() = mappingIndex.indexedNodeChunks()

    override val worldMode: WorldMode
        get() = currentWorldMode

    override val regions: Collection<WorldRegionInfo>
        get() = Collections.unmodifiableList(regionsById.values.map { region -> toRegionInfo(region)!! })

    override val places: Collection<WorldPlaceInfo>
        get() = Collections.unmodifiableList(placesById.values.map { place -> toPlaceInfo(place)!! })

    override val nodes: Collection<WorldNodeInfo>
        get() = Collections.unmodifiableList(nodesById.values.map { node -> toNodeInfo(node)!! })

    override val regionCount: Int
        get() = regionsById.size

    override val placeCount: Int
        get() = placesById.size

    override val nodeCount: Int
        get() = nodesById.size

    fun reloadFromConfig(config: FileConfiguration, profile: PlatformProfile) {
        regionsById.clear()
        placesById.clear()
        placesByRegion.clear()
        nodesById.clear()
        nodesByRegion.clear()
        nodesByPlace.clear()
        mappingIndex.clear()
        dirty = false

        enabled = config.getBoolean("world_admin.enabled", true)
        autoIndexEnabled = config.getBoolean("world_admin.auto_index.enabled", true)
        currentWorldMode = profile.worldMode
        if (!enabled) {
            debugSink.accept("World admin este dezactivat din configuratie.")
            return
        }

        val regionsSection = config.getConfigurationSection("world_admin.regions")
        if (regionsSection == null) {
            debugSink.accept("World admin nu are regiuni configurate.")
            return
        }

        for (regionId in regionsSection.getKeys(false)) {
            val regionSection = regionsSection.getConfigurationSection(regionId) ?: continue

            val region = WorldRegion(
                regionId,
                regionSection.getString("name", regionId) ?: regionId,
                regionSection.getString("world", "world") ?: "world",
                RegionType.fromId(regionSection.getString("type", "custom")),
                getInt(regionSection, "min.x", 0),
                getInt(regionSection, "min.y", 0),
                getInt(regionSection, "min.z", 0),
                getInt(regionSection, "max.x", 0),
                getInt(regionSection, "max.y", 255),
                getInt(regionSection, "max.z", 0)
            )
            region.setTags(regionSection.getStringList("tags"))
            region.storyState = loadStoryState(regionSection, profile)

            registerRegion(region)
            loadPlaces(region, regionSection)
            loadNodes(region, regionSection)
        }

        logger.info(
            "World admin incarcat: ${regionsById.size} regiuni, " +
                "$placeCount places, $nodeCount noduri." +
                " Auto-index: ${if (autoIndexEnabled) "activ" else "dezactivat"}."
        )
        dirty = false
    }

    private fun loadStoryState(regionSection: ConfigurationSection, profile: PlatformProfile): StoryState {
        val defaultMode = profile.defaultStoryMode
        val storySection = regionSection.getConfigurationSection("story")
            ?: return StoryState(defaultMode, "default")

        val storyState = StoryState(
            StoryMode.fromId(storySection.getString("mode", defaultMode.id)),
            storySection.getString("state", "default") ?: "default"
        )
        storyState.setStoryPool(storySection.getStringList("pool"))
        return storyState
    }

    private fun loadPlaces(region: WorldRegion, regionSection: ConfigurationSection) {
        val placesSection = regionSection.getConfigurationSection("places") ?: return

        for (placeId in placesSection.getKeys(false)) {
            val placeSection = placesSection.getConfigurationSection(placeId) ?: continue
            val qualifiedPlaceId = qualifyChildId(region.id, placeId)

            val place = WorldPlace(
                qualifiedPlaceId,
                region.id,
                placeSection.getString("name", placeId) ?: placeId,
                placeSection.getString("world", region.worldName) ?: region.worldName,
                PlaceType.fromId(placeSection.getString("type", "custom")),
                getInt(placeSection, "min.x", 0),
                getInt(placeSection, "min.y", 0),
                getInt(placeSection, "min.z", 0),
                getInt(placeSection, "max.x", 0),
                getInt(placeSection, "max.y", 255),
                getInt(placeSection, "max.z", 0)
            )
            place.setTags(placeSection.getStringList("tags"))
            place.setOwnerNpcId(placeSection.getString("owner_npc_id", ""))
            place.isPublicAccess = placeSection.getBoolean("public_access", true)

            val metadataSection = placeSection.getConfigurationSection("metadata")
            if (metadataSection != null) {
                for (key in metadataSection.getKeys(false)) {
                    place.putMetadata(key, metadataSection.getString(key, "") ?: "")
                }
            }

            registerPlace(place)
            loadNodes(region, place, placeSection)
        }
    }

    private fun loadNodes(region: WorldRegion, regionSection: ConfigurationSection) {
        loadNodes(region, null, regionSection)
    }

    private fun loadNodes(region: WorldRegion, place: WorldPlace?, parentSection: ConfigurationSection?) {
        if (parentSection == null) {
            return
        }

        val defaultWorldName = place?.worldName ?: region.worldName
        val nodesSection = parentSection.getConfigurationSection("nodes") ?: return

        for (nodeId in nodesSection.getKeys(false)) {
            val nodeSection = nodesSection.getConfigurationSection(nodeId) ?: continue
            val nodeScope = place?.id ?: region.id
            val qualifiedNodeId = qualifyChildId(nodeScope, nodeId)

            val node = WorldNode(
                qualifiedNodeId,
                region.id,
                place?.id,
                WorldNodeType.fromId(nodeSection.getString("type", "custom")),
                nodeSection.getString("world", defaultWorldName) ?: defaultWorldName,
                nodeSection.getDouble("x", 0.0),
                nodeSection.getDouble("y", 0.0),
                nodeSection.getDouble("z", 0.0),
                nodeSection.getDouble("radius", 2.5)
            )

            val metadataSection = nodeSection.getConfigurationSection("metadata")
            if (metadataSection != null) {
                for (key in metadataSection.getKeys(false)) {
                    node.putMetadata(key, metadataSection.getString(key, "") ?: "")
                }
            }

            registerNode(node)
        }
    }

    private fun getInt(section: ConfigurationSection, path: String, fallback: Int): Int {
        return if (section.isInt(path)) section.getInt(path) else fallback
    }

    fun hasUnsavedChanges(): Boolean = dirty

    fun bindNpcToHomePlace(placeId: String?, npcId: String?, npcName: String?): WorldPlaceInfo {
        val place = requireExistingPlace(placeId)
        val normalizedNpcId = normalizeBindingValue(npcId)
        if (normalizedNpcId.isBlank()) {
            throw IllegalArgumentException("NPC id este obligatoriu pentru bind-ul home.")
        }

        place.setOwnerNpcId(normalizedNpcId)
        place.putMetadata("owner_status", "assigned")
        putMetadataIfPresent(place, "owner_name", npcName)
        appendMetadataValue(place, "resident_npc_ids", normalizedNpcId)
        appendMetadataValue(place, "resident_names", npcName)
        place.putMetadata("residents_status", "assigned")
        dirty = true
        return toPlaceInfo(place)!!
    }

    fun bindNpcToWorkPlace(placeId: String?, npcId: String?, npcName: String?): WorldPlaceInfo {
        val place = requireExistingPlace(placeId)
        val normalizedNpcId = normalizeBindingValue(npcId)
        if (normalizedNpcId.isBlank()) {
            throw IllegalArgumentException("NPC id este obligatoriu pentru bind-ul work.")
        }

        appendMetadataValue(place, "worker_npc_ids", normalizedNpcId)
        appendMetadataValue(place, "worker_names", npcName)
        place.putMetadata("worker_status", "assigned")
        dirty = true
        return toPlaceInfo(place)!!
    }

    fun bindNpcToSocialPlace(placeId: String?, npcId: String?, npcName: String?): WorldPlaceInfo {
        val place = requireExistingPlace(placeId)
        val normalizedNpcId = normalizeBindingValue(npcId)
        if (normalizedNpcId.isBlank()) {
            throw IllegalArgumentException("NPC id este obligatoriu pentru bind-ul social.")
        }

        appendMetadataValue(place, "social_npc_ids", normalizedNpcId)
        appendMetadataValue(place, "social_names", npcName)
        place.putMetadata("social_status", "assigned")
        dirty = true
        return toPlaceInfo(place)!!
    }

    fun createDemoSettlement(
        requestedRegionId: String?,
        worldName: String?,
        centerX: Int,
        centerY: Int,
        centerZ: Int,
        worldMinY: Int,
        worldMaxY: Int
    ): DemoMappingResult {
        val regionId = if (requestedRegionId.isNullOrBlank()) {
            nextAvailableDemoRegionId()
        } else {
            normalizeScopeId(requestedRegionId, "regionId")
        }
        val resolvedWorldName = requireWorldName(worldName)
        val safeWorldMinY = min(worldMinY, worldMaxY - 1)
        val safeWorldMaxY = max(worldMinY + 1, worldMaxY - 1)
        val regionMinY = clamp(centerY - 8, safeWorldMinY, safeWorldMaxY)
        val regionMaxY = clamp(centerY + 20, regionMinY + 1, safeWorldMaxY)
        val placeMinY = clamp(centerY - 1, regionMinY, max(regionMinY, regionMaxY - 2))
        val placeMaxY = clamp(centerY + 7, placeMinY + 1, regionMaxY)
        val nodeY = clamp(centerY, placeMinY, placeMaxY).toDouble()

        val createdPlaceIds = ArrayList<String>()
        val createdNodeIds = ArrayList<String>()
        val warnings = ArrayList<String>()

        val region = createRegion(
            regionId,
            "Demo Village",
            resolvedWorldName,
            RegionType.SETTLEMENT,
            centerX - 72,
            regionMinY,
            centerZ - 72,
            centerX + 72,
            regionMaxY,
            centerZ + 72
        )
        region.setTags(listOf("demo", "village", "starter", "first_playable", "spacious"))

        createDemoNode(
            regionId, null, "village_center", WorldNodeType.MEETING_POINT,
            resolvedWorldName, centerX.toDouble(), nodeY, centerZ.toDouble(), 8.0, createdNodeIds, "meeting_point"
        )

        val house1 = createDemoPlace(
            regionId, "house_1", "Casa 1", resolvedWorldName, PlaceType.HOUSE,
            centerX - 56, placeMinY, centerZ - 40, centerX - 40, placeMaxY, centerZ - 24,
            listOf("demo", "house", "home", "residential"), createdPlaceIds
        )
        house1.isPublicAccess = false
        house1.putMetadata("max_residents", "2")
        house1.putMetadata("owner_status", "pending")
        house1.putMetadata("residents_status", "pending")
        addDemoHouseNodes(regionId, house1, resolvedWorldName, nodeY, createdNodeIds)

        val house2 = createDemoPlace(
            regionId, "house_2", "Casa 2", resolvedWorldName, PlaceType.HOUSE,
            centerX - 30, placeMinY, centerZ - 50, centerX - 14, placeMaxY, centerZ - 34,
            listOf("demo", "house", "home", "residential"), createdPlaceIds
        )
        house2.isPublicAccess = false
        house2.putMetadata("max_residents", "2")
        house2.putMetadata("owner_status", "pending")
        house2.putMetadata("residents_status", "pending")
        addDemoHouseNodes(regionId, house2, resolvedWorldName, nodeY, createdNodeIds)

        val house3 = createDemoPlace(
            regionId, "house_3", "Casa 3", resolvedWorldName, PlaceType.HOUSE,
            centerX + 14, placeMinY, centerZ - 50, centerX + 30, placeMaxY, centerZ - 34,
            listOf("demo", "house", "home", "residential"), createdPlaceIds
        )
        house3.isPublicAccess = false
        house3.putMetadata("max_residents", "1")
        house3.putMetadata("owner_status", "pending")
        house3.putMetadata("residents_status", "pending")
        addDemoHouseNodes(regionId, house3, resolvedWorldName, nodeY, createdNodeIds)

        val house4 = createDemoPlace(
            regionId, "house_4", "Casa 4", resolvedWorldName, PlaceType.HOUSE,
            centerX + 40, placeMinY, centerZ - 40, centerX + 56, placeMaxY, centerZ - 24,
            listOf("demo", "house", "home", "residential"), createdPlaceIds
        )
        house4.isPublicAccess = false
        house4.putMetadata("max_residents", "1")
        house4.putMetadata("owner_status", "pending")
        house4.putMetadata("residents_status", "pending")
        addDemoHouseNodes(regionId, house4, resolvedWorldName, nodeY, createdNodeIds)

        val market = createDemoPlace(
            regionId, "piata", "Piata", resolvedWorldName, PlaceType.MARKET,
            centerX - 14, placeMinY, centerZ - 12, centerX + 14, placeMaxY, centerZ + 12,
            listOf("demo", "market", "public", "social"), createdPlaceIds
        )
        market.putMetadata("role", "social")
        market.putMetadata("layout_profile", "spacious_playable")
        createDemoNode(
            regionId, market.id, "meeting_point_1", WorldNodeType.MEETING_POINT,
            resolvedWorldName, centerX.toDouble(), nodeY, centerZ.toDouble(), 7.0, createdNodeIds, "meeting_point"
        )
        createDemoNode(
            regionId, market.id, "social_1", WorldNodeType.SOCIAL,
            resolvedWorldName, (centerX - 8).toDouble(), nodeY, centerZ.toDouble(), 5.0, createdNodeIds, "social_anchor"
        )
        createDemoNode(
            regionId, market.id, "quest_board", WorldNodeType.QUEST_TRIGGER,
            resolvedWorldName, (centerX + 10).toDouble(), nodeY, (centerZ + 3).toDouble(), 3.0, createdNodeIds, "quest_board"
        )

        val forge = createDemoPlace(
            regionId, "fierarie", "Fierarie", resolvedWorldName, PlaceType.FORGE,
            centerX - 60, placeMinY, centerZ + 18, centerX - 38, placeMaxY, centerZ + 40,
            listOf("demo", "forge", "workplace", "shop"), createdPlaceIds
        )
        forge.putMetadata("role", "work")
        createDemoNode(
            regionId, forge.id, "workstation_1", WorldNodeType.WORKSTATION,
            resolvedWorldName, (centerX - 50).toDouble(), nodeY, (centerZ + 28).toDouble(), 3.0, createdNodeIds, "workstation"
        )
        createDemoNode(
            regionId, forge.id, "work_1", WorldNodeType.WORK,
            resolvedWorldName, (centerX - 48).toDouble(), nodeY, (centerZ + 31).toDouble(), 5.0, createdNodeIds, "work_anchor"
        )
        createDemoNode(
            regionId, forge.id, "inspect_1", WorldNodeType.INTERACTION,
            resolvedWorldName, (centerX - 54).toDouble(), nodeY, (centerZ + 25).toDouble(), 3.0, createdNodeIds, "inspect_node"
        )

        val farm = createDemoPlace(
            regionId, "ferma", "Ferma", resolvedWorldName, PlaceType.FARM,
            centerX + 36, placeMinY, centerZ + 18, centerX + 62, placeMaxY, centerZ + 48,
            listOf("demo", "farm", "workplace"), createdPlaceIds
        )
        farm.putMetadata("role", "work")
        createDemoNode(
            regionId, farm.id, "work_1", WorldNodeType.WORK,
            resolvedWorldName, (centerX + 49).toDouble(), nodeY, (centerZ + 31).toDouble(), 6.0, createdNodeIds, "work_anchor"
        )
        createDemoNode(
            regionId, farm.id, "inspect_1", WorldNodeType.QUEST_TRIGGER,
            resolvedWorldName, (centerX + 55).toDouble(), nodeY, (centerZ + 40).toDouble(), 4.0, createdNodeIds, "inspect_node"
        )

        val tavern = createDemoPlace(
            regionId, "taverna", "Taverna", resolvedWorldName, PlaceType.TAVERN,
            centerX - 14, placeMinY, centerZ + 38, centerX + 14, placeMaxY, centerZ + 58,
            listOf("demo", "tavern", "public", "social"), createdPlaceIds
        )
        tavern.putMetadata("role", "social")
        createDemoNode(
            regionId, tavern.id, "entrance_1", WorldNodeType.ENTRANCE,
            resolvedWorldName, centerX.toDouble(), nodeY, (centerZ + 38).toDouble(), 3.0, createdNodeIds, "entrance"
        )
        createDemoNode(
            regionId, tavern.id, "social_1", WorldNodeType.SOCIAL,
            resolvedWorldName, centerX.toDouble(), nodeY, (centerZ + 49).toDouble(), 6.0, createdNodeIds, "social_anchor"
        )
        createDemoNode(
            regionId, tavern.id, "interaction_1", WorldNodeType.INTERACTION,
            resolvedWorldName, (centerX - 6).toDouble(), nodeY, (centerZ + 49).toDouble(), 3.0, createdNodeIds, "dialog_anchor"
        )

        val shrine = createDemoPlace(
            regionId, "altar", "Altarul satului", resolvedWorldName, PlaceType.CUSTOM,
            centerX + 40, placeMinY, centerZ - 10, centerX + 60, placeMaxY, centerZ + 12,
            listOf("demo", "ritual", "altar", "shrine", "sacred", "public"), createdPlaceIds
        )
        shrine.putMetadata("role", "ritual")
        createDemoNode(
            regionId, shrine.id, "ritual_circle", WorldNodeType.PROGRESSION,
            resolvedWorldName, (centerX + 50).toDouble(), nodeY, (centerZ + 1).toDouble(), 5.0, createdNodeIds, "ritual_circle"
        )
        createDemoNode(
            regionId, shrine.id, "altar_1", WorldNodeType.INTERACTION,
            resolvedWorldName, (centerX + 55).toDouble(), nodeY, (centerZ + 5).toDouble(), 3.0, createdNodeIds, "altar"
        )

        warnings.add("Demo-ul marcheaza semantic zona din jurul jucatorului; nu construieste blocuri fizice.")
        warnings.add("Layout-ul demo este spatios pentru gameplay; alege o zona relativ plata inainte de creare.")
        return DemoMappingResult(region.id, createdPlaceIds, createdNodeIds, warnings)
    }

    private fun createDemoPlace(
        regionId: String,
        localId: String,
        displayName: String,
        worldName: String,
        placeType: PlaceType,
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int,
        tags: List<String>,
        createdPlaceIds: MutableList<String>
    ): WorldPlace {
        val place = createPlace(
            regionId, localId, displayName, worldName, placeType,
            minX, minY, minZ, maxX, maxY, maxZ
        )
        place.setTags(tags)
        place.putMetadata("source", "demo_mapping")
        createdPlaceIds.add(place.id)
        return place
    }

    private fun createDemoNode(
        regionId: String,
        placeId: String?,
        localId: String,
        nodeType: WorldNodeType,
        worldName: String,
        x: Double,
        y: Double,
        z: Double,
        radius: Double,
        createdNodeIds: MutableList<String>,
        semantic: String
    ): WorldNode {
        val node = createNode(regionId, placeId, localId, nodeType, worldName, x, y, z, radius)
        node.putMetadata("source", "demo_mapping")
        node.putMetadata("semantic", semantic)
        createdNodeIds.add(node.id)
        return node
    }

    private fun addDemoHouseNodes(
        regionId: String,
        house: WorldPlace,
        worldName: String,
        y: Double,
        createdNodeIds: MutableList<String>
    ) {
        val centerX = (house.minX + house.maxX) / 2.0
        val centerZ = (house.minZ + house.maxZ) / 2.0
        val bedZ = house.minZ + 2.0
        val entranceZ = house.maxZ.toDouble()

        createDemoNode(regionId, house.id, "bed_1", WorldNodeType.BED, worldName, centerX, y, bedZ, 1.5, createdNodeIds, "bed")
        createDemoNode(regionId, house.id, "home_1", WorldNodeType.HOME, worldName, centerX, y, centerZ, 2.5, createdNodeIds, "home_anchor")
        createDemoNode(regionId, house.id, "entrance_1", WorldNodeType.ENTRANCE, worldName, centerX, y, entranceZ, 2.0, createdNodeIds, "entrance")
        createDemoNode(regionId, house.id, "npc_spawn_1", WorldNodeType.NPC_SPAWN, worldName, centerX, y, centerZ + 1.0, 2.0, createdNodeIds, "npc_spawn")
    }

    private fun nextAvailableDemoRegionId(): String {
        val baseId = "demo_sat"
        if (!regionsById.containsKey(baseId)) {
            return baseId
        }
        var index = 2
        while (regionsById.containsKey("${baseId}_$index")) {
            index++
        }
        return "${baseId}_$index"
    }

    fun createRegion(
        regionId: String?,
        name: String?,
        worldName: String?,
        type: RegionType?,
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int
    ): WorldRegion {
        val normalizedRegionId = normalizeScopeId(regionId, "regionId")
        if (regionsById.containsKey(normalizedRegionId)) {
            throw IllegalArgumentException("Exista deja o regiune cu ID-ul $normalizedRegionId.")
        }

        val resolvedWorldName = requireWorldName(worldName)
        val region = WorldRegion(
            normalizedRegionId,
            defaultDisplayName(name, normalizedRegionId),
            resolvedWorldName,
            type ?: RegionType.CUSTOM,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ
        )
        validateRegion(region)
        registerRegion(region)
        dirty = true
        return region
    }

    fun createPlace(
        regionId: String?,
        placeId: String?,
        displayName: String?,
        worldName: String?,
        placeType: PlaceType?,
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int
    ): WorldPlace {
        val normalizedRegionId = normalizeScopeId(regionId, "regionId")
        val region = regionsById[normalizedRegionId]
            ?: throw IllegalArgumentException("Regiunea $normalizedRegionId nu exista.")

        val normalizedPlaceId = normalizeLocalId(placeId, "placeId")
        val qualifiedPlaceId = qualifyChildId(normalizedRegionId, normalizedPlaceId)
        if (placesById.containsKey(qualifiedPlaceId)) {
            throw IllegalArgumentException("Exista deja un place cu ID-ul $qualifiedPlaceId.")
        }

        val resolvedWorldName = requireWorldName(worldName)
        if (!region.worldName.equals(resolvedWorldName, ignoreCase = true)) {
            throw IllegalArgumentException("Place-ul trebuie sa fie in aceeasi lume cu regiunea.")
        }

        val place = WorldPlace(
            qualifiedPlaceId,
            normalizedRegionId,
            defaultDisplayName(displayName, normalizedPlaceId),
            resolvedWorldName,
            placeType ?: PlaceType.CUSTOM,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ
        )
        validatePlace(region, place)
        registerPlace(place)
        dirty = true
        return place
    }

    fun createNode(
        regionId: String?,
        placeId: String?,
        nodeId: String?,
        nodeType: WorldNodeType?,
        worldName: String?,
        x: Double,
        y: Double,
        z: Double,
        radius: Double
    ): WorldNode {
        val normalizedRegionId = normalizeScopeId(regionId, "regionId")
        val region = regionsById[normalizedRegionId]
            ?: throw IllegalArgumentException("Regiunea $normalizedRegionId nu exista.")

        var place: WorldPlace? = null
        if (!placeId.isNullOrBlank()) {
            val normalizedPlaceId = if (placeId.contains(":")) {
                normalizeQualifiedPlaceId(placeId)
            } else {
                qualifyChildId(normalizedRegionId, normalizeLocalId(placeId, "placeId"))
            }
            place = placesById[normalizedPlaceId]
                ?: throw IllegalArgumentException("Place-ul $normalizedPlaceId nu exista.")
            if (!place.regionId.equals(normalizedRegionId, ignoreCase = true)) {
                throw IllegalArgumentException("Place-ul selectat nu apartine regiunii $normalizedRegionId.")
            }
        }

        val normalizedNodeId = normalizeLocalId(nodeId, "nodeId")
        val nodeScope = place?.id ?: normalizedRegionId
        val qualifiedNodeId = qualifyChildId(nodeScope, normalizedNodeId)
        if (nodesById.containsKey(qualifiedNodeId)) {
            throw IllegalArgumentException("Exista deja un node cu ID-ul $qualifiedNodeId.")
        }

        if (radius <= 0) {
            throw IllegalArgumentException("Raza node-ului trebuie sa fie mai mare decat 0.")
        }

        val resolvedWorldName = requireWorldName(worldName)
        val expectedWorld = place?.worldName ?: region.worldName
        if (!expectedWorld.equals(resolvedWorldName, ignoreCase = true)) {
            throw IllegalArgumentException("Node-ul trebuie sa fie in aceeasi lume cu zona care il contine.")
        }

        val node = WorldNode(
            qualifiedNodeId,
            normalizedRegionId,
            place?.id,
            nodeType ?: WorldNodeType.CUSTOM,
            resolvedWorldName,
            x,
            y,
            z,
            radius
        )
        validateNode(region, place, node)
        registerNode(node)
        dirty = true
        return node
    }

    fun saveToConfig(config: FileConfiguration) {
        val worldAdminSection = config.getConfigurationSection("world_admin") ?: config.createSection("world_admin")

        worldAdminSection.set("enabled", enabled)
        worldAdminSection.set("auto_index.enabled", autoIndexEnabled)
        worldAdminSection.set("regions", null)
        val regionsSection = worldAdminSection.createSection("regions")

        val sortedRegions = ArrayList(regionsById.values)
        sortedRegions.sortBy { region -> region.id }

        for (region in sortedRegions) {
            val regionSection = regionsSection.createSection(region.id)
            regionSection.set("name", region.name)
            regionSection.set("world", region.worldName)
            regionSection.set("type", region.type.id)
            writeBounds(
                regionSection,
                region.minX,
                region.minY,
                region.minZ,
                region.maxX,
                region.maxY,
                region.maxZ
            )
            regionSection.set("tags", region.getTags())
            writeStoryState(regionSection, region.storyState)
            writeRegionNodes(regionSection, region.id)
            writePlaces(regionSection, region.id)
        }

        dirty = false
    }

    private fun requireExistingPlace(placeId: String?): WorldPlace {
        val normalizedPlaceId = normalizeBindingValue(placeId)
        return placesById[normalizedPlaceId]
            ?: throw IllegalArgumentException("Place-ul $placeId nu exista in world mapping.")
    }

    private fun putMetadataIfPresent(place: WorldPlace, key: String, value: String?) {
        val normalizedValue = normalizeBindingValue(value)
        if (normalizedValue.isNotBlank()) {
            place.putMetadata(key, normalizedValue)
        }
    }

    private fun appendMetadataValue(place: WorldPlace, key: String, value: String?) {
        val normalizedValue = normalizeBindingValue(value)
        if (normalizedValue.isBlank()) {
            return
        }

        val values = ArrayList<String>()
        val existing = place.getMetadata().getOrDefault(key, "")
        if (existing.isNotBlank()) {
            for (part in existing.split(Regex("[,;]"))) {
                val candidate = normalizeBindingValue(part)
                if (candidate.isNotBlank() && values.none { existingValue -> existingValue.equals(candidate, ignoreCase = true) }) {
                    values.add(candidate)
                }
            }
        }
        if (values.none { existingValue -> existingValue.equals(normalizedValue, ignoreCase = true) }) {
            values.add(normalizedValue)
        }
        place.putMetadata(key, values.joinToString(","))
    }

    private fun normalizeBindingValue(value: String?): String {
        return value?.trim().orEmpty()
    }

    fun registerRegion(region: WorldRegion) {
        regionsById[region.id] = region
        if (autoIndexEnabled) {
            mappingIndex.indexRegion(region)
        }
    }

    fun registerPlace(place: WorldPlace) {
        placesById[place.id] = place
        placesByRegion.getOrPut(place.regionId) { ArrayList() }.add(place)
        if (autoIndexEnabled) {
            mappingIndex.indexPlace(place)
        }
    }

    fun registerNode(node: WorldNode) {
        nodesById[node.id] = node
        nodesByRegion.getOrPut(node.regionId) { ArrayList() }.add(node)
        if (!node.placeId.isNullOrBlank()) {
            nodesByPlace.getOrPut(node.placeId) { ArrayList() }.add(node)
        }
        if (autoIndexEnabled) {
            mappingIndex.indexNode(node)
        }
    }

    fun getRegionModels(): Collection<WorldRegion> {
        return Collections.unmodifiableCollection(regionsById.values)
    }

    fun getPlaceModels(regionId: String?): List<WorldPlace> {
        return Collections.unmodifiableList(regionId?.let { placesByRegion[it] } ?: emptyList())
    }

    fun getNodeModels(regionId: String?): List<WorldNode> {
        return Collections.unmodifiableList(regionId?.let { nodesByRegion[it] } ?: emptyList())
    }

    fun getNodeModelsForPlace(placeId: String?): List<WorldNode> {
        return Collections.unmodifiableList(placeId?.let { nodesByPlace[it] } ?: emptyList())
    }

    fun findRegionAt(worldName: String?, x: Int, y: Int, z: Int): WorldRegion? {
        if (autoIndexEnabled) {
            return mappingIndex.findRegion(worldName, x, y, z)
        }
        return regionsById.values.firstOrNull { region -> region.contains(worldName, x, y, z) }
    }

    fun findPlaceAt(worldName: String?, x: Int, y: Int, z: Int): WorldPlace? {
        if (autoIndexEnabled) {
            return mappingIndex.findPlace(worldName, x, y, z)
        }
        return placesById.values.firstOrNull { place -> place.contains(worldName, x, y, z) }
    }

    fun findNodeAt(worldName: String?, x: Double, y: Double, z: Double): WorldNode? {
        if (autoIndexEnabled) {
            return mappingIndex.findNode(worldName, x, y, z)
        }
        return nodesById.values.firstOrNull { node -> node.contains(worldName, x, y, z) }
    }

    fun findNodeModelsNear(worldName: String?, x: Double, y: Double, z: Double, radius: Double, limit: Int): List<WorldNode> {
        if (worldName.isNullOrBlank() || radius < 0.0 || limit == 0) {
            return emptyList()
        }

        if (autoIndexEnabled) {
            return mappingIndex.findNodesNear(worldName, x, y, z, radius, limit)
        }

        val sequence = nodesById.values.asSequence()
            .filter { node -> node.isNear(worldName, x, y, z, radius + node.radius) }
            .sortedBy { node -> node.distanceSquared(worldName, x, y, z) }
        return (if (limit > 0) sequence.take(limit) else sequence).toList()
    }

    override fun getRegion(regionId: String?): WorldRegionInfo? {
        return toRegionInfo(regionId?.let { regionsById[it] })
    }

    override fun findRegion(worldName: String?, x: Int, y: Int, z: Int): WorldRegionInfo? {
        return toRegionInfo(findRegionAt(worldName, x, y, z))
    }

    override fun getPlaces(regionId: String?): Collection<WorldPlaceInfo> {
        return Collections.unmodifiableList(
            (regionId?.let { placesByRegion[it] } ?: emptyList())
                .map { place -> toPlaceInfo(place)!! }
        )
    }

    override fun getPlace(placeId: String?): WorldPlaceInfo? {
        return toPlaceInfo(placeId?.let { placesById[it] })
    }

    override fun findPlace(worldName: String?, x: Int, y: Int, z: Int): WorldPlaceInfo? {
        return toPlaceInfo(findPlaceAt(worldName, x, y, z))
    }

    override fun findPlacesByTag(regionId: String?, tag: String?): Collection<WorldPlaceInfo> {
        if (tag.isNullOrBlank()) {
            return emptyList()
        }

        return Collections.unmodifiableList(
            placesById.values.asSequence()
                .filter { place -> regionId.isNullOrBlank() || place.regionId.equals(regionId, ignoreCase = true) }
                .filter { place -> place.hasTag(tag) }
                .map { place -> toPlaceInfo(place)!! }
                .toList()
        )
    }

    override fun getNodes(regionId: String?): Collection<WorldNodeInfo> {
        return Collections.unmodifiableList(
            (regionId?.let { nodesByRegion[it] } ?: emptyList())
                .map { node -> toNodeInfo(node)!! }
        )
    }

    override fun getNodesForPlace(placeId: String?): Collection<WorldNodeInfo> {
        return Collections.unmodifiableList(
            (placeId?.let { nodesByPlace[it] } ?: emptyList())
                .map { node -> toNodeInfo(node)!! }
        )
    }

    override fun getNode(nodeId: String?): WorldNodeInfo? {
        return toNodeInfo(nodeId?.let { nodesById[it] })
    }

    override fun findNode(worldName: String?, x: Int, y: Int, z: Int): WorldNodeInfo? {
        return toNodeInfo(findNodeAt(worldName, x.toDouble(), y.toDouble(), z.toDouble()))
    }

    override fun findNodesNear(worldName: String?, x: Double, y: Double, z: Double, radius: Double, limit: Int): Collection<WorldNodeInfo> {
        return Collections.unmodifiableList(
            findNodeModelsNear(worldName, x, y, z, radius, limit)
                .map { node -> toNodeInfo(node)!! }
        )
    }

    private fun toRegionInfo(region: WorldRegion?): WorldRegionInfo? {
        if (region == null) {
            return null
        }

        val storyState = region.storyState
        return WorldRegionInfo(
            region.id,
            region.name,
            region.worldName,
            region.type.id,
            region.minX,
            region.minY,
            region.minZ,
            region.maxX,
            region.maxY,
            region.maxZ,
            region.getTags(),
            storyState.mode,
            storyState.stateKey,
            storyState.getStoryPool()
        )
    }

    private fun toPlaceInfo(place: WorldPlace?): WorldPlaceInfo? {
        if (place == null) {
            return null
        }

        return WorldPlaceInfo(
            place.id,
            place.regionId,
            place.displayName,
            place.worldName,
            place.placeType,
            place.minX,
            place.minY,
            place.minZ,
            place.maxX,
            place.maxY,
            place.maxZ,
            place.getTags(),
            place.getOwnerNpcId(),
            place.isPublicAccess,
            place.getMetadata()
        )
    }

    private fun toNodeInfo(node: WorldNode?): WorldNodeInfo? {
        if (node == null) {
            return null
        }

        return WorldNodeInfo(
            node.id,
            node.regionId,
            node.placeId,
            node.type.id,
            node.worldName,
            node.x,
            node.y,
            node.z,
            node.radius,
            node.getMetadata()
        )
    }

    private fun qualifyChildId(scopeId: String, localId: String?): String {
        if (localId.isNullOrBlank()) {
            return scopeId
        }
        if (localId.contains(":")) {
            return localId
        }
        return "$scopeId:$localId"
    }

    private fun validateRegion(candidate: WorldRegion) {
        for (existing in regionsById.values) {
            if (existing.worldName.equals(candidate.worldName, ignoreCase = true) && intersects(existing, candidate)) {
                throw IllegalArgumentException("Regiunea se suprapune peste regiunea existenta ${existing.id}.")
            }
        }
    }

    private fun validatePlace(region: WorldRegion, candidate: WorldPlace) {
        if (!contains(region, candidate.minX.toDouble(), candidate.minY.toDouble(), candidate.minZ.toDouble()) ||
            !contains(region, candidate.maxX.toDouble(), candidate.maxY.toDouble(), candidate.maxZ.toDouble())
        ) {
            throw IllegalArgumentException("Place-ul trebuie sa fie complet in interiorul regiunii ${region.id}.")
        }

        for (existing in placesByRegion[region.id] ?: emptyList()) {
            if (intersects(existing, candidate)) {
                throw IllegalArgumentException("Place-ul se suprapune peste place-ul existent ${existing.id}.")
            }
        }
    }

    private fun validateNode(region: WorldRegion, place: WorldPlace?, candidate: WorldNode) {
        val insideContainer = if (place != null) {
            contains(place, candidate.x, candidate.y, candidate.z)
        } else {
            contains(region, candidate.x, candidate.y, candidate.z)
        }
        if (!insideContainer) {
            throw IllegalArgumentException(
                if (place != null) {
                    "Node-ul trebuie sa fie in interiorul place-ului ${place.id}."
                } else {
                    "Node-ul trebuie sa fie in interiorul regiunii ${region.id}."
                }
            )
        }
    }

    private fun intersects(left: WorldRegion, right: WorldRegion): Boolean {
        return overlaps(left.minX, left.maxX, right.minX, right.maxX) &&
            overlaps(left.minY, left.maxY, right.minY, right.maxY) &&
            overlaps(left.minZ, left.maxZ, right.minZ, right.maxZ)
    }

    private fun intersects(left: WorldPlace, right: WorldPlace): Boolean {
        return overlaps(left.minX, left.maxX, right.minX, right.maxX) &&
            overlaps(left.minY, left.maxY, right.minY, right.maxY) &&
            overlaps(left.minZ, left.maxZ, right.minZ, right.maxZ)
    }

    private fun overlaps(leftMin: Int, leftMax: Int, rightMin: Int, rightMax: Int): Boolean {
        return leftMax >= rightMin && rightMax >= leftMin
    }

    private fun contains(region: WorldRegion, x: Double, y: Double, z: Double): Boolean {
        return x >= region.minX && x <= region.maxX &&
            y >= region.minY && y <= region.maxY &&
            z >= region.minZ && z <= region.maxZ
    }

    private fun contains(place: WorldPlace, x: Double, y: Double, z: Double): Boolean {
        return x >= place.minX && x <= place.maxX &&
            y >= place.minY && y <= place.maxY &&
            z >= place.minZ && z <= place.maxZ
    }

    private fun writeBounds(
        section: ConfigurationSection,
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int
    ) {
        section.set("min.x", minX)
        section.set("min.y", minY)
        section.set("min.z", minZ)
        section.set("max.x", maxX)
        section.set("max.y", maxY)
        section.set("max.z", maxZ)
    }

    private fun writeStoryState(regionSection: ConfigurationSection, storyState: StoryState?) {
        if (storyState == null) {
            return
        }

        val storySection = regionSection.createSection("story")
        storySection.set("mode", storyState.mode.id)
        storySection.set("state", storyState.stateKey)
        storySection.set("pool", storyState.getStoryPool())
    }

    private fun writePlaces(regionSection: ConfigurationSection, regionId: String) {
        val places = ArrayList(placesByRegion[regionId] ?: emptyList())
        if (places.isEmpty()) {
            return
        }

        places.sortBy { place -> place.id }
        val placesSection = regionSection.createSection("places")
        for (place in places) {
            val placeSection = placesSection.createSection(localChildId(regionId, place.id))
            placeSection.set("name", place.displayName)
            placeSection.set("world", place.worldName)
            placeSection.set("type", place.placeType.id)
            writeBounds(
                placeSection,
                place.minX,
                place.minY,
                place.minZ,
                place.maxX,
                place.maxY,
                place.maxZ
            )
            placeSection.set("tags", place.getTags())
            placeSection.set("owner_npc_id", place.getOwnerNpcId())
            placeSection.set("public_access", place.isPublicAccess)
            val metadata = place.getMetadata()
            if (metadata.isNotEmpty()) {
                val metadataSection = placeSection.createSection("metadata")
                for ((key, value) in metadata) {
                    metadataSection.set(key, value)
                }
            }
            writePlaceNodes(placeSection, place.id)
        }
    }

    private fun writeRegionNodes(regionSection: ConfigurationSection, regionId: String) {
        val regionNodes = ArrayList<WorldNode>()
        for (node in nodesByRegion[regionId] ?: emptyList()) {
            if (node.placeId.isNullOrBlank()) {
                regionNodes.add(node)
            }
        }
        writeNodes(regionSection, regionId, regionNodes)
    }

    private fun writePlaceNodes(placeSection: ConfigurationSection, placeId: String) {
        writeNodes(placeSection, placeId, nodesByPlace[placeId] ?: emptyList())
    }

    private fun writeNodes(parentSection: ConfigurationSection, scopeId: String, sourceNodes: Collection<WorldNode>) {
        if (sourceNodes.isEmpty()) {
            return
        }

        val nodes = ArrayList(sourceNodes)
        nodes.sortBy { node -> node.id }
        val nodesSection = parentSection.createSection("nodes")
        for (node in nodes) {
            val nodeSection = nodesSection.createSection(localChildId(scopeId, node.id))
            nodeSection.set("type", node.type.id)
            nodeSection.set("world", node.worldName)
            nodeSection.set("x", node.x)
            nodeSection.set("y", node.y)
            nodeSection.set("z", node.z)
            nodeSection.set("radius", node.radius)
            val metadata = node.getMetadata()
            if (metadata.isNotEmpty()) {
                val metadataSection = nodeSection.createSection("metadata")
                for ((key, value) in metadata) {
                    metadataSection.set(key, value)
                }
            }
        }
    }

    private fun localChildId(scopeId: String, qualifiedId: String): String {
        val prefix = "$scopeId:"
        if (qualifiedId.startsWith(prefix)) {
            return qualifiedId.substring(prefix.length)
        }
        return qualifiedId
    }

    private fun requireWorldName(worldName: String?): String {
        if (worldName.isNullOrBlank()) {
            throw IllegalArgumentException("Lumea nu poate fi goala.")
        }
        return worldName.trim()
    }

    private fun normalizeScopeId(value: String?, label: String): String {
        return normalizeId(value, label, false)
    }

    private fun normalizeLocalId(value: String?, label: String): String {
        return normalizeId(value, label, false)
    }

    private fun normalizeQualifiedPlaceId(value: String?): String {
        return normalizeId(value, "placeId", true)
    }

    private fun normalizeId(value: String?, label: String, allowScopes: Boolean): String {
        if (value == null) {
            throw IllegalArgumentException("$label nu poate fi null.")
        }

        val normalized = value.trim().lowercase(Locale.ROOT).replace(' ', '_')
        if (normalized.isBlank()) {
            throw IllegalArgumentException("$label nu poate fi gol.")
        }
        val pattern = if (allowScopes) Regex("[a-z0-9_\\-:]+") else Regex("[a-z0-9_\\-]+")
        if (!normalized.matches(pattern)) {
            throw IllegalArgumentException("$label poate contine doar litere mici, cifre, '_' si '-'.")
        }
        if (normalized.startsWith(":") || normalized.endsWith(":") || normalized.contains("::")) {
            throw IllegalArgumentException("$label are un format invalid.")
        }
        return normalized
    }

    private fun defaultDisplayName(explicitName: String?, fallbackId: String): String {
        if (!explicitName.isNullOrBlank()) {
            return explicitName.trim()
        }
        return humanizeId(fallbackId)
    }

    private fun humanizeId(rawId: String): String {
        var value = rawId
        val scopeSeparator = value.lastIndexOf(':')
        if (scopeSeparator >= 0) {
            value = value.substring(scopeSeparator + 1)
        }

        val builder = StringBuilder()
        for (part in value.split(Regex("[_-]+"))) {
            if (part.isBlank()) {
                continue
            }
            if (builder.isNotEmpty()) {
                builder.append(' ')
            }
            builder.append(part[0].uppercaseChar())
            if (part.length > 1) {
                builder.append(part.substring(1))
            }
        }
        return if (builder.isNotEmpty()) builder.toString() else rawId
    }

    private fun clamp(value: Int, min: Int, max: Int): Int {
        if (max < min) {
            return min
        }
        return max(min, min(max, value))
    }

    class DemoMappingResult(
        private val regionId: String,
        createdPlaceIds: List<String>?,
        createdNodeIds: List<String>?,
        warnings: List<String>?
    ) {
        private val createdPlaceIds: List<String> = Collections.unmodifiableList((createdPlaceIds ?: emptyList()).toList())
        private val createdNodeIds: List<String> = Collections.unmodifiableList((createdNodeIds ?: emptyList()).toList())
        private val warnings: List<String> = Collections.unmodifiableList((warnings ?: emptyList()).toList())

        fun regionId(): String = regionId

        fun createdPlaceIds(): List<String> = createdPlaceIds

        fun createdNodeIds(): List<String> = createdNodeIds

        fun warnings(): List<String> = warnings
    }
}
