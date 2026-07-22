package ro.ainpc.npc

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Zombie
import org.bukkit.entity.Villager

interface NpcEntityAdapter {
    val entityKind: NpcEntityKind

    fun spawn(npc: AINPC, location: Location): Entity?

    fun attach(npc: AINPC, entity: Entity)

    fun configure(npc: AINPC, entity: Entity) {
        entity.setCustomName(npc.displayName ?: npc.name)
        entity.setCustomNameVisible(true)
    }
}

object NpcEntityAdapters {
    private val villagerAdapter = VillagerNpcEntityAdapter()
    private val zombieAdapter = ZombieNpcEntityAdapter()
    private val armorStandAdapter = ArmorStandNpcEntityAdapter()
    private val noopAdapter = NoopNpcEntityAdapter()

    fun resolve(npc: AINPC): NpcEntityAdapter {
        npc.entityAdapter?.let { return it }
        return when (npc.entityKind) {
            NpcEntityKind.VILLAGER,
            NpcEntityKind.HUMANOID -> villagerAdapter
            NpcEntityKind.MONSTER -> zombieAdapter
            NpcEntityKind.SPIRIT,
            NpcEntityKind.MARKER -> armorStandAdapter
            NpcEntityKind.ANIMAL,
            NpcEntityKind.NONE -> noopAdapter
        }
    }

    private val professionMapping: Map<String, Villager.Profession> = mapOf(
        "farmer" to Villager.Profession.FARMER,
        "fermier" to Villager.Profession.FARMER,
        "librarian" to Villager.Profession.LIBRARIAN,
        "bibliotecar" to Villager.Profession.LIBRARIAN,
        "priest" to Villager.Profession.CLERIC,
        "preot" to Villager.Profession.CLERIC,
        "blacksmith" to Villager.Profession.ARMORER,
        "fierar" to Villager.Profession.ARMORER,
        "butcher" to Villager.Profession.BUTCHER,
        "macelar" to Villager.Profession.BUTCHER,
        "cartographer" to Villager.Profession.CARTOGRAPHER,
        "cartograf" to Villager.Profession.CARTOGRAPHER,
        "cleric" to Villager.Profession.CLERIC,
        "armorer" to Villager.Profession.ARMORER,
        "weaponsmith" to Villager.Profession.WEAPONSMITH,
        "armurier" to Villager.Profession.WEAPONSMITH,
        "toolsmith" to Villager.Profession.TOOLSMITH,
        "fletcher" to Villager.Profession.FLETCHER,
        "sagetar" to Villager.Profession.FLETCHER,
        "leatherworker" to Villager.Profession.LEATHERWORKER,
        "piragar" to Villager.Profession.LEATHERWORKER,
        "mason" to Villager.Profession.MASON,
        "zidar" to Villager.Profession.MASON,
        "shepherd" to Villager.Profession.SHEPHERD,
        "cioban" to Villager.Profession.SHEPHERD,
        "nitwit" to Villager.Profession.NITWIT,
        "none" to Villager.Profession.NONE
    )

    fun resolveProfession(occupation: String?): Villager.Profession {
        if (occupation.isNullOrBlank()) return Villager.Profession.NONE
        val key = occupation.trim().lowercase()
        return professionMapping[key] ?: Villager.Profession.NONE
    }

    private val villagerTypeMapping: Map<String, Villager.Type> = mapOf(
        "desert" to Villager.Type.DESERT,
        "jungle" to Villager.Type.JUNGLE,
        "plains" to Villager.Type.PLAINS,
        "savanna" to Villager.Type.SAVANNA,
        "snow" to Villager.Type.SNOW,
        "swamp" to Villager.Type.SWAMP,
        "taiga" to Villager.Type.TAIGA
    )

    fun resolveVillagerType(biome: String?): Villager.Type {
        if (biome.isNullOrBlank()) return Villager.Type.PLAINS
        val key = biome.trim().lowercase()
        for ((biomeKey, type) in villagerTypeMapping) {
            if (key.contains(biomeKey)) return type
        }
        return Villager.Type.PLAINS
    }
}

class ZombieNpcEntityAdapter : NpcEntityAdapter {
    override val entityKind: NpcEntityKind = NpcEntityKind.MONSTER

    override fun spawn(npc: AINPC, location: Location): Entity? {
        val world = location.world ?: return null
        return world.spawn(location, Zombie::class.java) { spawnedZombie ->
            spawnedZombie.setSilent(true)
            spawnedZombie.setBaby(false)
            spawnedZombie.isPersistent = true
            spawnedZombie.setRemoveWhenFarAway(false)
            configure(npc, spawnedZombie)
        }
    }

    override fun attach(npc: AINPC, entity: Entity) {
        npc.attachToEntity(entity)
    }
}

class ArmorStandNpcEntityAdapter : NpcEntityAdapter {
    override val entityKind: NpcEntityKind = NpcEntityKind.SPIRIT

    override fun spawn(npc: AINPC, location: Location): Entity? {
        val world = location.world ?: return null
        return world.spawn(location, ArmorStand::class.java) { stand ->
            stand.setInvisible(true)
            stand.setMarker(true)
            stand.setSmall(true)
            stand.setBasePlate(false)
            stand.setArms(false)
            stand.setSilent(true)
            stand.isInvulnerable = true
            stand.isPersistent = true
            stand.setRemoveWhenFarAway(false)
            configure(npc, stand)
        }
    }

    override fun attach(npc: AINPC, entity: Entity) {
        npc.attachToEntity(entity)
    }
}

class VillagerNpcEntityAdapter : NpcEntityAdapter {
    override val entityKind: NpcEntityKind = NpcEntityKind.VILLAGER

    override fun spawn(npc: AINPC, location: Location): Entity? {
        val world = location.world ?: return null
        val npcLoc = npc.location
        return world.spawn(location, Villager::class.java) { spawnedVillager ->
            val profession = NpcEntityAdapters.resolveProfession(npc.occupation)
            spawnedVillager.profession = profession
            if (npcLoc != null && npcLoc.world != null) {
                val biomeType = npcLoc.world.getBiome(npcLoc.blockX, npcLoc.blockY, npcLoc.blockZ)
                spawnedVillager.villagerType = NpcEntityAdapters.resolveVillagerType(biomeType.key.asString())
            }
            configure(npc, spawnedVillager)
        }
    }

    override fun attach(npc: AINPC, entity: Entity) {
        npc.attachToVillager(entity as? Villager)
    }

    override fun configure(npc: AINPC, entity: Entity) {
        entity.setCustomName(npc.displayName ?: npc.name)
        entity.setCustomNameVisible(true)
        if (entity is Villager && npc.age in 0..120) {
            when {
                npc.age < 18 -> entity.setBaby()
                else -> entity.setAdult()
            }
        }
    }
}

class NoopNpcEntityAdapter : NpcEntityAdapter {
    override val entityKind: NpcEntityKind = NpcEntityKind.NONE

    override fun spawn(npc: AINPC, location: Location): Entity? = null

    override fun attach(npc: AINPC, entity: Entity) {
        npc.attachToEntity(entity)
    }
}
