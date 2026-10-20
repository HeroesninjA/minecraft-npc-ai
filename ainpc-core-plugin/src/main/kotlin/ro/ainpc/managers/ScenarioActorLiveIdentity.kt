package ro.ainpc.managers

import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Villager
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcEntityKind

internal object ScenarioActorLiveIdentity {
    fun matches(npc: AINPC, entity: Entity?, plugin: Plugin): Boolean {
        if (npc.databaseId <= 0 || npc.sourceKey.isBlank()) return false
        if (npc.entityKind != NpcEntityKind.VILLAGER && npc.entityKind != NpcEntityKind.HUMANOID) return false
        if (entity !is Villager || !entity.isValid || entity.isDead) return false
        if (entity.uniqueId != npc.uuid || entity.world.name != npc.worldName) return false
        val data = entity.persistentDataContainer
        return data.get(NamespacedKey(plugin, AINPC.PDC_MANAGED_KEY), PersistentDataType.INTEGER) == 1 &&
            data.get(NamespacedKey(plugin, AINPC.PDC_DATABASE_ID_KEY), PersistentDataType.INTEGER) == npc.databaseId &&
            data.get(NamespacedKey(plugin, AINPC.PDC_UUID_KEY), PersistentDataType.STRING) == npc.uuid.toString() &&
            data.get(NamespacedKey(plugin, AINPC.PDC_SOURCE_KEY), PersistentDataType.STRING) == npc.sourceKey
    }
}
