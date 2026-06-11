package ro.ainpc.spawn

import ro.ainpc.npc.AINPC
import java.util.LinkedHashMap
import java.util.Locale

data class HouseAllocation(
    val placeId: String,
    val familyId: String,
    val primaryOwnerNpcKey: String,
    val maxResidents: Int,
    val residentPlans: List<ResidentPlan>
) {
    private val cleanedPlaceId = clean(placeId)
    private val cleanedFamilyId = clean(familyId)
    private val cleanedPrimaryOwnerNpcKey = clean(primaryOwnerNpcKey)
    private val cleanedResidentPlans = residentPlans.toList()
    private val effectiveMaxResidents = if (maxResidents > 0) maxResidents else cleanedResidentPlans.size

    fun placeId(): String = cleanedPlaceId
    fun familyId(): String = cleanedFamilyId
    fun primaryOwnerNpcKey(): String = cleanedPrimaryOwnerNpcKey
    fun maxResidents(): Int = effectiveMaxResidents
    fun residentPlans(): List<ResidentPlan> = cleanedResidentPlans.toList()

    fun toNpcSpawnPlans(): List<NpcSpawnPlan> {
        val plans = mutableListOf<NpcSpawnPlan>()
        for (resident in cleanedResidentPlans) {
            plans.add(resident.toNpcSpawnPlan(cleanedPlaceId, cleanedFamilyId))
        }
        return plans.toList()
    }

    fun householdId(): String {
        if (cleanedFamilyId.isNotBlank()) {
            return cleanedFamilyId
        }
        val normalizedPlaceId = normalizeKey(cleanedPlaceId)
        return if (normalizedPlaceId.isBlank()) "" else "household_$normalizedPlaceId"
    }

    fun toPlaceMetadata(): Map<String, String> {
        val metadata = LinkedHashMap<String, String>()
        metadata["role"] = "home"
        if (cleanedFamilyId.isNotBlank()) {
            metadata["family_id"] = cleanedFamilyId
        }
        metadata["max_residents"] = effectiveMaxResidents.toString()
        metadata["residents"] = residentNpcKeys().joinToString(",")
        return metadata.toMap()
    }

    fun toFamilyBindingPlan(spawnedNpcsByKey: Map<String, AINPC>?): FamilyBindingPlan {
        val members = mutableListOf<FamilyBindingPlan.Member>()
        val resolvedNpcs = spawnedNpcsByKey ?: mapOf()

        for (resident in cleanedResidentPlans) {
            var npc = resolvedNpcs[normalizeKey(resident.npcKey())]
            if (npc == null) {
                npc = resolvedNpcs[resident.npcKey()]
            }
            members.add(FamilyBindingPlan.member(resident.npcKey(), npc, resident.relationRole()))
        }

        return FamilyBindingPlan(cleanedFamilyId, members)
    }

    fun residentNpcKeys(): List<String> {
        return cleanedResidentPlans
            .map { it.npcKey() }
            .filter { key -> key.isNotBlank() }
    }

    class Builder private constructor(private val placeId: String) {
        private var familyId = ""
        private var primaryOwnerNpcKey = ""
        private var maxResidents = 0
        private val residentPlans = mutableListOf<ResidentPlan>()

        fun familyId(familyId: String): Builder {
            this.familyId = familyId
            return this
        }

        fun primaryOwnerNpcKey(primaryOwnerNpcKey: String): Builder {
            this.primaryOwnerNpcKey = primaryOwnerNpcKey
            return this
        }

        fun maxResidents(maxResidents: Int): Builder {
            this.maxResidents = maxResidents
            return this
        }

        fun addResident(residentPlan: ResidentPlan?): Builder {
            if (residentPlan != null) {
                residentPlans.add(residentPlan)
            }
            return this
        }

        fun residents(residentPlans: List<ResidentPlan>?): Builder {
            this.residentPlans.clear()
            if (residentPlans != null) {
                this.residentPlans.addAll(residentPlans)
            }
            return this
        }

        fun build(): HouseAllocation {
            return HouseAllocation(placeId, familyId, primaryOwnerNpcKey, maxResidents, residentPlans)
        }

        companion object {
            @JvmStatic
            fun create(placeId: String): Builder = Builder(placeId)
        }
    }

    data class ResidentPlan(
        val npcKey: String,
        val name: String,
        val relationRole: String,
        val occupation: String,
        val backstory: String,
        val age: Int,
        val gender: String,
        val archetype: String,
        val spawnNodeId: String,
        val homeNodeId: String,
        val bedNodeId: String,
        val workPlaceId: String,
        val workNodeId: String,
        val socialPlaceId: String,
        val socialNodeId: String
    ) {
        private val cleanedNpcKey = clean(npcKey)
        private val cleanedName = clean(name)
        private val cleanedRelationRole = clean(relationRole)
        private val cleanedOccupation = clean(occupation)
        private val cleanedBackstory = clean(backstory)
        private val cleanedAge = if (age > 0) age else 30
        private val cleanedGender = clean(gender)
        private val cleanedArchetype = clean(archetype)
        private val cleanedSpawnNodeId = clean(spawnNodeId)
        private val cleanedHomeNodeId = clean(homeNodeId)
        private val cleanedBedNodeId = clean(bedNodeId)
        private val cleanedWorkPlaceId = clean(workPlaceId)
        private val cleanedWorkNodeId = clean(workNodeId)
        private val cleanedSocialPlaceId = clean(socialPlaceId)
        private val cleanedSocialNodeId = clean(socialNodeId)

        fun npcKey(): String = cleanedNpcKey
        fun name(): String = cleanedName
        fun relationRole(): String = cleanedRelationRole
        fun occupation(): String = cleanedOccupation
        fun backstory(): String = cleanedBackstory
        fun age(): Int = cleanedAge
        fun gender(): String = cleanedGender
        fun archetype(): String = cleanedArchetype
        fun spawnNodeId(): String = cleanedSpawnNodeId
        fun homeNodeId(): String = cleanedHomeNodeId
        fun bedNodeId(): String = cleanedBedNodeId
        fun workPlaceId(): String = cleanedWorkPlaceId
        fun workNodeId(): String = cleanedWorkNodeId
        fun socialPlaceId(): String = cleanedSocialPlaceId
        fun socialNodeId(): String = cleanedSocialNodeId

        fun effectiveHomeNodeId(): String = if (cleanedHomeNodeId.isNotBlank()) cleanedHomeNodeId else cleanedBedNodeId

        fun toNpcSpawnPlan(homePlaceId: String, familyId: String): NpcSpawnPlan {
            return NpcSpawnPlan.builder(cleanedNpcKey, if (cleanedName.isNotBlank()) cleanedName else humanizeId(cleanedNpcKey))
                .occupation(cleanedOccupation)
                .backstory(cleanedBackstory)
                .age(cleanedAge)
                .gender(cleanedGender)
                .archetype(cleanedArchetype)
                .homePlaceId(homePlaceId)
                .workPlaceId(cleanedWorkPlaceId)
                .socialPlaceId(cleanedSocialPlaceId)
                .spawnNodeId(cleanedSpawnNodeId)
                .homeNodeId(effectiveHomeNodeId())
                .workNodeId(cleanedWorkNodeId)
                .socialNodeId(cleanedSocialNodeId)
                .familyId(familyId)
                .build()
        }

        companion object {
            @JvmStatic
            fun builder(npcKey: String, name: String): ResidentBuilder = ResidentBuilder.create(npcKey, name)
        }
    }

    class ResidentBuilder private constructor(
        private val npcKey: String,
        private val name: String
    ) {
        private var relationRole = ""
        private var occupation = ""
        private var backstory = ""
        private var age = 30
        private var gender = "male"
        private var archetype = ""
        private var spawnNodeId = ""
        private var homeNodeId = ""
        private var bedNodeId = ""
        private var workPlaceId = ""
        private var workNodeId = ""
        private var socialPlaceId = ""
        private var socialNodeId = ""

        fun relationRole(relationRole: String): ResidentBuilder {
            this.relationRole = relationRole
            return this
        }

        fun occupation(occupation: String): ResidentBuilder {
            this.occupation = occupation
            return this
        }

        fun backstory(backstory: String): ResidentBuilder {
            this.backstory = backstory
            return this
        }

        fun age(age: Int): ResidentBuilder {
            this.age = age
            return this
        }

        fun gender(gender: String): ResidentBuilder {
            this.gender = gender
            return this
        }

        fun archetype(archetype: String): ResidentBuilder {
            this.archetype = archetype
            return this
        }

        fun spawnNodeId(spawnNodeId: String): ResidentBuilder {
            this.spawnNodeId = spawnNodeId
            return this
        }

        fun homeNodeId(homeNodeId: String): ResidentBuilder {
            this.homeNodeId = homeNodeId
            return this
        }

        fun bedNodeId(bedNodeId: String): ResidentBuilder {
            this.bedNodeId = bedNodeId
            return this
        }

        fun workPlaceId(workPlaceId: String): ResidentBuilder {
            this.workPlaceId = workPlaceId
            return this
        }

        fun workNodeId(workNodeId: String): ResidentBuilder {
            this.workNodeId = workNodeId
            return this
        }

        fun socialPlaceId(socialPlaceId: String): ResidentBuilder {
            this.socialPlaceId = socialPlaceId
            return this
        }

        fun socialNodeId(socialNodeId: String): ResidentBuilder {
            this.socialNodeId = socialNodeId
            return this
        }

        fun build(): ResidentPlan {
            return ResidentPlan(
                npcKey,
                name,
                relationRole,
                occupation,
                backstory,
                age,
                gender,
                archetype,
                spawnNodeId,
                homeNodeId,
                bedNodeId,
                workPlaceId,
                workNodeId,
                socialPlaceId,
                socialNodeId
            )
        }

        companion object {
            @JvmStatic
            fun create(npcKey: String, name: String): ResidentBuilder = ResidentBuilder(npcKey, name)
        }
    }

    companion object {
        @JvmStatic
        fun builder(placeId: String): Builder = Builder.create(placeId)

        private fun clean(value: String?): String = value?.trim() ?: ""

        private fun normalizeKey(value: String): String {
            return clean(value).lowercase(Locale.ROOT).replace(' ', '_').replace('-', '_')
        }

        private fun humanizeId(rawId: String): String {
            var value = clean(rawId)
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
            return if (builder.isNotEmpty()) builder.toString() else value
        }
    }
}
