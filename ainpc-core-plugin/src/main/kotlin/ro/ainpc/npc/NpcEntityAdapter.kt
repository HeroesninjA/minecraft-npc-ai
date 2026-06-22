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
        return world.spawn(location, Villager::class.java) { spawnedVillager ->
            spawnedVillager.profession = Villager.Profession.NONE
            spawnedVillager.villagerType = Villager.Type.PLAINS
        }
    }

    override fun attach(npc: AINPC, entity: Entity) {
        npc.attachToVillager(entity as? Villager)
    }
}

class NoopNpcEntityAdapter : NpcEntityAdapter {
    override val entityKind: NpcEntityKind = NpcEntityKind.NONE

    override fun spawn(npc: AINPC, location: Location): Entity? = null

    override fun attach(npc: AINPC, entity: Entity) {
        npc.attachToEntity(entity)
    }
}
