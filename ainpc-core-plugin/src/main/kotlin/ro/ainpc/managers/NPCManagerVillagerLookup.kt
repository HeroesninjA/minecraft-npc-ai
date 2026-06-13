@file:JvmName("NPCManagerVillagerLookup")

package ro.ainpc.managers

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Villager
import org.bukkit.persistence.PersistentDataType
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import java.util.UUID

lateinit var villagerLookupPlugin: AINPCPlugin

fun initVillagerLookupPlugin(plugin: AINPCPlugin) {
    villagerLookupPlugin = plugin
}

fun isChunkLoaded(npc: AINPC): Boolean {
    val worldName = npc.worldName ?: return false
    val world = villagerLookupPlugin.server.getWorld(worldName) ?: return false
    val chunkX = floorToBlock(npc.x) shr 4
    val chunkZ = floorToBlock(npc.z) shr 4
    return world.isChunkLoaded(chunkX, chunkZ)
}

fun findVillagerByUuid(uuid: UUID): Villager? {
    for (world in villagerLookupPlugin.server.worlds) {
        for (villager in world.getEntitiesByClass(Villager::class.java)) {
            if (villager.uniqueId == uuid) {
                return villager
            }
        }
    }
    return null
}

fun persistentKey(key: String): NamespacedKey {
    return NamespacedKey(villagerLookupPlugin, key)
}

fun readPersistentString(villager: Villager?, key: String?): String {
    if (villager == null || key == null || key.isBlank()) {
        return ""
    }
    val value = villager.persistentDataContainer[persistentKey(key), PersistentDataType.STRING]
    return value?.trim() ?: ""
}

fun readPersistentNpcId(villager: Villager?): Int {
    if (villager == null) {
        return 0
    }
    val npcId = villager.persistentDataContainer[persistentKey(AINPC.PDC_DATABASE_ID_KEY), PersistentDataType.INTEGER]
    return npcId ?: 0
}

fun isMarkedAinpcVillager(villager: Villager?): Boolean {
    if (villager == null) {
        return false
    }
    val managed = villager.persistentDataContainer[persistentKey(AINPC.PDC_MANAGED_KEY), PersistentDataType.INTEGER]
    return managed != null && managed == 1
}

fun isPendingManagedVillager(villager: Villager?): Boolean {
    return isMarkedAinpcVillager(villager) && readPersistentNpcId(villager) <= 0
}

fun matchesPersistentIdentity(villager: Villager?, npc: AINPC?): Boolean {
    if (villager == null || npc == null) {
        return false
    }

    val storedNpcId = readPersistentNpcId(villager)
    if (storedNpcId > 0 && storedNpcId == npc.databaseId) {
        return true
    }

    val storedUuid = readPersistentString(villager, AINPC.PDC_UUID_KEY)
    if (storedUuid.equals(npc.uuid.toString(), ignoreCase = true)) {
        return true
    }

    val storedSourceKey = readPersistentString(villager, AINPC.PDC_SOURCE_KEY)
    return storedSourceKey.isNotBlank() && storedSourceKey.equals(npc.sourceKey, ignoreCase = true)
}

fun isLegacyPluginVillager(villager: Villager?): Boolean {
    if (villager == null) return false
    return isMarkedAinpcVillager(villager)
        || !villager.hasAI()
        || villager.isInvulnerable
        || villager.isSilent
}

fun choosePreferredVillager(npc: AINPC, currentVillager: Villager, incomingVillager: Villager): Villager {
    val storedUuid = npc.uuid
    if (incomingVillager.uniqueId == storedUuid) {
        return incomingVillager
    }
    if (currentVillager.uniqueId == storedUuid) {
        return currentVillager
    }

    val npcId = npc.databaseId
    val incomingNpcId = readPersistentNpcId(incomingVillager)
    val currentNpcId = readPersistentNpcId(currentVillager)
    if (npcId > 0 && incomingNpcId == npcId && currentNpcId != npcId) {
        return incomingVillager
    }
    return currentVillager
}

fun findVillagerByPersistentIdentity(npc: AINPC?): Villager? {
    if (npc == null) return null
    for (world in villagerLookupPlugin.server.worlds) {
        for (villager in world.getEntitiesByClass(Villager::class.java)) {
            if (matchesPersistentIdentity(villager, npc)) {
                return villager
            }
        }
    }
    return null
}

fun findVillagerByPersistentIdentity(chunk: Chunk?, npc: AINPC?): Villager? {
    if (chunk == null || npc == null) return null
    for (entity in chunk.entities) {
        if (entity is Villager && matchesPersistentIdentity(entity, npc)) {
            return entity
        }
    }
    return null
}

fun findVillagerByUuid(chunk: Chunk?, uuid: UUID): Villager? {
    if (chunk == null) return null
    for (entity in chunk.entities) {
        if (entity is Villager && entity.uniqueId == uuid) {
            return entity
        }
    }
    return null
}

fun findLegacyVillager(npc: AINPC?): Villager? {
    val expectedLocation = npc?.location ?: return null
    val world = expectedLocation.world ?: return null
    for (villager in world.getEntitiesByClass(Villager::class.java)) {
        if (!isLegacyPluginVillager(villager)) continue
        if (!isSameNpcLocation(expectedLocation, villager.location)) continue
        val villagerName = getVillagerDisplayName(villager)
        if (namesMatch(npc.displayName, villagerName) || namesMatch(npc.name, villagerName)) {
            return villager
        }
    }
    return null
}

fun findLegacyVillager(npc: AINPC?, chunk: Chunk?): Villager? {
    val expectedLocation = npc?.location ?: return null
    if (chunk == null) return null
    for (entity in chunk.entities) {
        if (entity !is Villager) continue
        if (!isLegacyPluginVillager(entity)) continue
        if (!isSameNpcLocation(expectedLocation, entity.location)) continue
        val villagerName = getVillagerDisplayName(entity)
        if (namesMatch(npc.displayName, villagerName) || namesMatch(npc.name, villagerName)) {
            return entity
        }
    }
    return null
}

fun findVillagerForNPC(npc: AINPC, preferredChunk: Chunk?): Villager? {
    val exactMatch = if (preferredChunk == null) {
        findVillagerByUuid(npc.uuid)
    } else {
        findVillagerByUuid(preferredChunk, npc.uuid)
    }
    if (exactMatch != null) return exactMatch

    val persistentMatch = if (preferredChunk == null) {
        findVillagerByPersistentIdentity(npc)
    } else {
        findVillagerByPersistentIdentity(preferredChunk, npc)
    }
    if (persistentMatch != null) return persistentMatch

    return if (preferredChunk == null) {
        findLegacyVillager(npc)
    } else {
        findLegacyVillager(npc, preferredChunk)
    }
}
