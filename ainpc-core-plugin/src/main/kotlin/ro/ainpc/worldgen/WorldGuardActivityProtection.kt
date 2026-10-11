package ro.ainpc.worldgen

import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player

class WorldGuardActivityProtection internal constructor(
    private val queryFactory: (String, String) -> ((BlockOperation) -> Boolean)?,
) {
    fun allows(actor: String, operations: List<BlockOperation>): Boolean {
        if (!actor.startsWith("player:") || operations.isEmpty() || operations.size > 10000) return false
        val worldName = operations.first().worldName
        if (operations.any { it.worldName != worldName }) return false
        return try {
            val query = queryFactory(actor, worldName) ?: return false
            operations.all(query)
        } catch (exception: Exception) {
            false
        } catch (error: LinkageError) {
            false
        }
    }

    companion object {
        fun bukkit(): WorldGuardActivityProtection = WorldGuardActivityProtection { actor, worldName ->
            val plugin = Bukkit.getPluginManager().getPlugin("WorldGuard")
            val playerId = runCatching { UUID.fromString(actor.removePrefix("player:")) }.getOrNull()
            val player = playerId?.let { Bukkit.getPlayer(it) }
            val world = Bukkit.getWorld(worldName)
            if (plugin == null || !plugin.isEnabled || player == null || !player.isOnline || world == null) {
                null
            } else {
                createQuery(plugin.javaClass.classLoader, player, world)
            }
        }

        private fun createQuery(loader: ClassLoader, player: Player, world: World): ((BlockOperation) -> Boolean)? {
            fun type(name: String) = Class.forName(name, true, loader)
            val guardType = type("com.sk89q.worldguard.WorldGuard")
            val guard = guardType.getMethod("getInstance").invoke(null)
            val platform = guardType.getMethod("getPlatform").invoke(guard)
            val container = type("com.sk89q.worldguard.internal.platform.WorldGuardPlatform")
                .getMethod("getRegionContainer").invoke(platform)
            val adapter = type("com.sk89q.worldedit.bukkit.BukkitAdapter")
            val adaptedWorld = adapter.getMethod("adapt", World::class.java).invoke(null, world)
            val manager = type("com.sk89q.worldguard.protection.regions.RegionContainer")
                .getMethod("get", type("com.sk89q.worldedit.world.World")).invoke(container, adaptedWorld) ?: return null
            val pluginType = type("com.sk89q.worldguard.bukkit.WorldGuardPlugin")
            val localPlayer = pluginType.getMethod("wrapPlayer", Player::class.java)
                .invoke(pluginType.getMethod("inst").invoke(null), player)
            val vectorType = type("com.sk89q.worldedit.math.BlockVector3")
            val vectorAt = vectorType.getMethod("at", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            val applicable = type("com.sk89q.worldguard.protection.managers.RegionManager")
                .getMethod("getApplicableRegions", vectorType)
            val setType = type("com.sk89q.worldguard.protection.ApplicableRegionSet")
            val virtual = setType.getMethod("isVirtual")
            val flagType = type("com.sk89q.worldguard.protection.flags.StateFlag")
            val flags = java.lang.reflect.Array.newInstance(flagType, 3)
            val flagRegistry = type("com.sk89q.worldguard.protection.flags.Flags")
            for ((index, name) in listOf("BUILD", "BLOCK_PLACE", "BLOCK_BREAK").withIndex()) {
                java.lang.reflect.Array.set(flags, index, flagRegistry.getField(name).get(null))
            }
            val testState = setType.getMethod("testState", type("com.sk89q.worldguard.protection.association.RegionAssociable"), flags.javaClass)
            return { operation ->
                val vector = vectorAt.invoke(null, operation.x, operation.y, operation.z)
                val regions = applicable.invoke(manager, vector)
                virtual.invoke(regions) == false && testState.invoke(regions, localPlayer, flags) == true
            }
        }
    }
}
