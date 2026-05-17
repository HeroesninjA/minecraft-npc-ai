package ro.ainpc.spawn

import java.util.Locale

data class NpcSpawnPlan(
    val npcKey: String,
    val name: String,
    val occupation: String,
    val backstory: String,
    val age: Int,
    val gender: String,
    val archetype: String,
    val homePlaceId: String,
    val workPlaceId: String,
    val socialPlaceId: String,
    val spawnNodeId: String,
    val homeNodeId: String,
    val workNodeId: String,
    val socialNodeId: String,
    val familyId: String
) {
    private val cleanedNpcKey = clean(npcKey)
    private val cleanedName = clean(name)
    private val cleanedOccupation = clean(occupation)
    private val cleanedBackstory = clean(backstory)
    private val cleanedAge = if (age > 0) age else 30
    private val cleanedGender = clean(gender).let { if (it.isBlank()) "male" else it.lowercase(Locale.getDefault()) }
    private val cleanedArchetype = clean(archetype)
    private val cleanedHomePlaceId = clean(homePlaceId)
    private val cleanedWorkPlaceId = clean(workPlaceId)
    private val cleanedSocialPlaceId = clean(socialPlaceId)
    private val cleanedSpawnNodeId = clean(spawnNodeId)
    private val cleanedHomeNodeId = clean(homeNodeId)
    private val cleanedWorkNodeId = clean(workNodeId)
    private val cleanedSocialNodeId = clean(socialNodeId)
    private val cleanedFamilyId = clean(familyId)

    fun npcKey(): String = cleanedNpcKey
    fun name(): String = cleanedName
    fun occupation(): String = cleanedOccupation
    fun backstory(): String = cleanedBackstory
    fun age(): Int = cleanedAge
    fun gender(): String = cleanedGender
    fun archetype(): String = cleanedArchetype
    fun homePlaceId(): String = cleanedHomePlaceId
    fun workPlaceId(): String = cleanedWorkPlaceId
    fun socialPlaceId(): String = cleanedSocialPlaceId
    fun spawnNodeId(): String = cleanedSpawnNodeId
    fun homeNodeId(): String = cleanedHomeNodeId
    fun workNodeId(): String = cleanedWorkNodeId
    fun socialNodeId(): String = cleanedSocialNodeId
    fun familyId(): String = cleanedFamilyId

    fun sourceKey(): String {
        val scope = firstNonBlank(cleanedFamilyId, cleanedHomePlaceId, cleanedSpawnNodeId, "global")
        val key = firstNonBlank(cleanedNpcKey, cleanedName, "npc")
        return "spawn_plan:${normalizeToken(scope)}:${normalizeToken(key)}"
    }

    class Builder private constructor(
        private val npcKey: String,
        private val name: String
    ) {
        private var occupation = ""
        private var backstory = ""
        private var age = 30
        private var gender = "male"
        private var archetype = ""
        private var homePlaceId = ""
        private var workPlaceId = ""
        private var socialPlaceId = ""
        private var spawnNodeId = ""
        private var homeNodeId = ""
        private var workNodeId = ""
        private var socialNodeId = ""
        private var familyId = ""

        fun occupation(occupation: String): Builder {
            this.occupation = occupation
            return this
        }

        fun backstory(backstory: String): Builder {
            this.backstory = backstory
            return this
        }

        fun age(age: Int): Builder {
            this.age = age
            return this
        }

        fun gender(gender: String): Builder {
            this.gender = gender
            return this
        }

        fun archetype(archetype: String): Builder {
            this.archetype = archetype
            return this
        }

        fun homePlaceId(homePlaceId: String): Builder {
            this.homePlaceId = homePlaceId
            return this
        }

        fun workPlaceId(workPlaceId: String): Builder {
            this.workPlaceId = workPlaceId
            return this
        }

        fun socialPlaceId(socialPlaceId: String): Builder {
            this.socialPlaceId = socialPlaceId
            return this
        }

        fun spawnNodeId(spawnNodeId: String): Builder {
            this.spawnNodeId = spawnNodeId
            return this
        }

        fun homeNodeId(homeNodeId: String): Builder {
            this.homeNodeId = homeNodeId
            return this
        }

        fun workNodeId(workNodeId: String): Builder {
            this.workNodeId = workNodeId
            return this
        }

        fun socialNodeId(socialNodeId: String): Builder {
            this.socialNodeId = socialNodeId
            return this
        }

        fun familyId(familyId: String): Builder {
            this.familyId = familyId
            return this
        }

        fun build(): NpcSpawnPlan {
            return NpcSpawnPlan(
                npcKey,
                name,
                occupation,
                backstory,
                age,
                gender,
                archetype,
                homePlaceId,
                workPlaceId,
                socialPlaceId,
                spawnNodeId,
                homeNodeId,
                workNodeId,
                socialNodeId,
                familyId
            )
        }

        companion object {
            @JvmStatic
            fun create(npcKey: String, name: String): Builder = Builder(npcKey, name)
        }
    }

    companion object {
        @JvmStatic
        fun builder(npcKey: String, name: String): Builder = Builder.create(npcKey, name)

        private fun clean(value: String?): String = value?.trim() ?: ""

        private fun firstNonBlank(vararg values: String?): String {
            for (value in values) {
                val cleanValue = clean(value)
                if (cleanValue.isNotBlank()) {
                    return cleanValue
                }
            }
            return ""
        }

        private fun normalizeToken(value: String?): String {
            val cleaned = clean(value).lowercase(Locale.ROOT)
            if (cleaned.isBlank()) {
                return "unknown"
            }
            return cleaned.replace(Regex("[^a-z0-9_.:-]+"), "_")
        }
    }
}
