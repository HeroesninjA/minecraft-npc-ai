package ro.ainpc.world.fixture

import org.bukkit.Location
import org.bukkit.World
import ro.ainpc.managers.NPCManager
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldNodeType
import java.util.Locale

class ControlledTestWorldFixturePopulateResult(
    npcNames: List<String>?,
    errors: List<String>?,
    warnings: List<String>?
) {
    private val npcNames: List<String> = (npcNames ?: emptyList()).toList()
    private val errors: List<String> = (errors ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()

    fun npcNames(): List<String> = npcNames
    fun npcCount(): Int = npcNames.size
    fun errors(): List<String> = errors
    fun warnings(): List<String> = warnings
    fun success(): Boolean = errors.isEmpty()
}

class ControlledTestWorldFixturePopulator {

    private val DEFAULT_Y = 64

    fun populate(
        npcManager: NPCManager?,
        worldAdminService: WorldAdminService?,
        world: World?,
        plan: ControlledTestWorldFixturePlan,
    ): ControlledTestWorldFixturePopulateResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val createdNpcs = mutableListOf<String>()

        if (npcManager == null) {
            errors.add("NPCManager indisponibil.")
            return result(createdNpcs, errors, warnings)
        }
        if (worldAdminService == null) {
            errors.add("WorldAdminService indisponibil.")
            return result(createdNpcs, errors, warnings)
        }
        if (world == null) {
            errors.add("Lumea nu este disponibila.")
            return result(createdNpcs, errors, warnings)
        }

        val roles = npcRoles(plan)

        for (role in roles) {
            val homePlace = worldAdminService.getPlace(role.homePlaceId)
            if (homePlace == null) {
                warnings.add("Home place ${role.homePlaceId} pentru ${role.npcId} nu exista.")
                continue
            }

            val spawnNodes = worldAdminService.getNodesForPlace(role.homePlaceId)
                .filter { it.typeId() == WorldNodeType.NPC_SPAWN.id || it.metadata().values.any { v -> v.contains("spawn", ignoreCase = true) } }

            val existingNpc = npcManager.getNPCByName(role.name)
            if (existingNpc != null) {
                warnings.add("NPC ${role.name} exista deja (id=${existingNpc.databaseId}), sar peste.")
                createdNpcs.add(existingNpc.name ?: role.name)
                continue
            }

            val spawnLocation: Location = if (spawnNodes.isNotEmpty()) {
                val node = spawnNodes.first()
                Location(world, node.x(), node.y(), node.z())
            } else {
                val cx = (homePlace.minX() + homePlace.maxX()) / 2.0
                val cy = homePlace.minY().toDouble() + 1.0
                val cz = (homePlace.minZ() + homePlace.maxZ()) / 2.0
                Location(world, cx, cy, cz)
            }

            val npc = npcManager.createNPC(role.name, spawnLocation, role.occupation, role.backstory, role.age, role.gender, role.archetype)
            if (npc != null) {
                createdNpcs.add(role.name)
            } else {
                errors.add("Nu am putut crea NPC-ul ${role.name}.")
            }
        }

        return result(createdNpcs, errors, warnings)
    }

    fun npcRoles(plan: ControlledTestWorldFixturePlan): List<FixtureNpcRole> {
        val prefix = plan.prefix()
        return listOf(
            FixtureNpcRole("${prefix}npc_lider", "Nicolae Liderul", "Liderul satului", "merchant",
                "${prefix}sat_central:casa_1", "${prefix}sat_central:piata", "${prefix}sat_central:taverna", "male", 42, "caregiver"),
            FixtureNpcRole("${prefix}npc_fierar", "Gheorghe Fierarul", "Fierar", "blacksmith",
                "${prefix}sat_central:casa_2", "${prefix}sat_central:fierarie", "${prefix}sat_central:taverna", "male", 38, "creator"),
            FixtureNpcRole("${prefix}npc_fermier_1", "Dumitru Gavrila", "Fermier principal", "farmer",
                "${prefix}sat_central:casa_3", "${prefix}sat_central:ferma", "${prefix}sat_central:piata", "male", 35, "creator"),
            FixtureNpcRole("${prefix}npc_fermier_2", "Maria Gavrila", "Fermier ajutor", "farmer",
                "${prefix}sat_central:casa_3", "${prefix}sat_central:ferma", "${prefix}sat_central:piata", "female", 30, "creator"),
            FixtureNpcRole("${prefix}npc_hangiu", "Ion Hangiu", "Hangiu", "innkeeper",
                "${prefix}sat_central:casa_4", "${prefix}sat_central:taverna", "${prefix}sat_central:piata", "male", 45, "caregiver"),
            FixtureNpcRole("${prefix}npc_paznic", "Vlad Paznicul", "Paznic", "guard",
                "${prefix}sat_central:post_paza", "${prefix}sat_central:post_paza", "${prefix}sat_central:piata", "male", 32, "protector"),
            FixtureNpcRole("${prefix}npc_negustor", "Tudor Negustorul", "Negustor", "merchant",
                "${prefix}sat_central:casa_5", "${prefix}sat_central:piata", "${prefix}sat_central:taverna", "male", 40, "caregiver"),
            FixtureNpcRole("${prefix}npc_ierbar", "Elena Ierbarul", "Vindecator", "healer",
                "${prefix}sat_central:casa_5", "${prefix}sat_central:altar", "${prefix}sat_central:piata", "female", 36, "wise"),
            FixtureNpcRole("${prefix}npc_ucenic", "Andrei Ucenicul", "Ucenic fierar", "blacksmith",
                "${prefix}sat_central:casa_2", "${prefix}sat_central:fierarie", "${prefix}sat_central:piata", "male", 18, "learner"),
            FixtureNpcRole("${prefix}npc_mesager", "Ana Mesagerul", "Mesager", "guard",
                "${prefix}sat_central:taverna", "${prefix}sat_central:drum_nord", "${prefix}sat_central:taverna", "female", 25, "explorer",
                "Cunoaste drumurile din zona si a observat miscari suspecte langa padure."),
            FixtureNpcRole("${prefix}npc_emisar", "Stefan Emisarul", "Reprezentant factiune", "merchant",
                "${prefix}tabara_factiune:leader_tent", "${prefix}tabara_factiune:storage", "${prefix}tabara_factiune:campfire", "male", 43, "caregiver"),
            FixtureNpcRole("${prefix}npc_pustnic", "Ilie Pustnicul", "Locuitor izolat", "priest",
                "${prefix}casa_izolata:casa", "${prefix}casa_izolata:casa", "${prefix}padure_veche", "male", 58, "wise",
                "Cunoaste istoria veche a zonei. A vazut caravana disparuta acum ani. Evita satul dar stie adevarul despre cripta lupilor."),
        )
    }

    private fun result(
        npcNames: List<String>,
        errors: List<String>,
        warnings: List<String>
    ): ControlledTestWorldFixturePopulateResult =
        ControlledTestWorldFixturePopulateResult(npcNames, errors, warnings)
}

data class FixtureNpcRole(
    val npcId: String,
    val name: String,
    val displayRole: String,
    val occupation: String,
    val homePlaceId: String,
    val workPlaceId: String? = null,
    val socialPlaceId: String? = null,
    val gender: String = "male",
    val age: Int = 30,
    val archetype: String? = null,
    val backstory: String? = null
)
