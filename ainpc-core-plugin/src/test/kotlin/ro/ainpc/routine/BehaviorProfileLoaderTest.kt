package ro.ainpc.routine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BehaviorProfileLoaderTest {

    @Test
    fun parseMinimalProfile() {
        val loader = BehaviorProfileLoader(null)
        val yaml = """
            profiles:
              default_villager:
                occupation: "villager"
        """.trimIndent()
        val results = loader.parseYamlString(yaml)
        assertEquals(1, results.size)
        val p = results[0]
        assertEquals("default_villager", p.profileId)
        assertEquals("villager", p.occupation)
        assertEquals(0.6, p.movementSpeed)
        assertTrue(p.homeReturn)
    }

    @Test
    fun parseFullProfile() {
        val loader = BehaviorProfileLoader(null)
        val yaml = """
            profiles:
              farmer:
                occupation: "fermier"
                display_name: "Fermier"
                movement_speed: 0.5
                wander_radius: 12.0
                home_return: true
                socialize_chance: 0.2
                weather_reactions: true
                night_return: true
                danger_avoidance: false
                schedule:
                  morning:
                    label: "Dimineata"
                    start_tick: 0
                    end_tick: 6000
                    slot: "WORK"
                    activity: "cultivă"
                    target: "farm_01"
                  evening:
                    label: "Seara"
                    start_tick: 12000
                    end_tick: 13800
                    slot: "HOME"
                    activity: "odihnă"
                metadata:
                  skill: "farming"
        """.trimIndent()
        val results = loader.parseYamlString(yaml)
        assertEquals(1, results.size)
        val p = results[0]
        assertEquals("farmer", p.profileId)
        assertEquals("fermier", p.occupation)
        assertEquals("Fermier", p.displayName)
        assertEquals(0.5, p.movementSpeed)
        assertEquals(2, p.schedule.size)
        assertEquals("Dimineata", p.schedule[0].label)
        assertEquals("WORK", p.schedule[0].slot)
        assertEquals("cultivă", p.schedule[0].activity)
        assertEquals("farm_01", p.schedule[0].target)
        assertEquals("HOME", p.schedule[1].slot)
        assertEquals("odihnă", p.schedule[1].activity)
        assertTrue(p.metadata.containsKey("skill"))
    }

    @Test
    fun parseMultipleProfiles() {
        val loader = BehaviorProfileLoader(null)
        val yaml = """
            profiles:
              guard:
                occupation: "paznic"
                movement_speed: 0.7
                danger_avoidance: true
              trader:
                occupation: "negustor"
                socialize_chance: 0.6
        """.trimIndent()
        val results = loader.parseYamlString(yaml)
        assertEquals(2, results.size)
        assertTrue(results.any { it.profileId == "guard" })
        assertTrue(results.any { it.profileId == "trader" })
    }

    @Test
    fun emptyYamlReturnsEmpty() {
        val loader = BehaviorProfileLoader(null)
        val results = loader.parseYamlString("profiles:")
        assertTrue(results.isEmpty())
    }

    @Test
    fun getProfileById() {
        val loader = BehaviorProfileLoader(null)
        loader.parseYamlString("""
            profiles:
              test: { occupation: "test" }
        """.trimIndent())
        val p = loader.getProfile("test")
        assertTrue(p != null)
        assertEquals("test", p!!.profileId)
    }

    @Test
    fun findProfileForOccupation() {
        val loader = BehaviorProfileLoader(null)
        loader.parseYamlString("""
            profiles:
              farmer: { occupation: "fermier", movement_speed: 0.5 }
              guard: { occupation: "paznic", movement_speed: 0.7 }
        """.trimIndent())
        val farmer = loader.findProfileForOccupation("fermier")
        assertTrue(farmer != null)
        assertEquals("farmer", farmer!!.profileId)
        val guard = loader.findProfileForOccupation("paznic")
        assertTrue(guard != null)
        assertEquals(0.7, guard!!.movementSpeed)
        val unknown = loader.findProfileForOccupation("alchimist")
        assertEquals(null, unknown)
    }

    @Test
    fun getAllProfiles() {
        val loader = BehaviorProfileLoader(null)
        loader.parseYamlString("""
            profiles:
              a: { occupation: "x" }
              b: { occupation: "y" }
        """.trimIndent())
        assertEquals(2, loader.getAllProfiles().size)
    }

    @Test
    fun validateDetectsMissingSchedule() {
        val loader = BehaviorProfileLoader(null)
        val profile = BehaviorProfile(profileId = "test", occupation = "fermier")
        val issues = loader.validate(profile)
        assertTrue(issues.any { it.contains("nu are schedule") })
    }

    @Test
    fun validateDetectsInvalidSlot() {
        val loader = BehaviorProfileLoader(null)
        val profile = BehaviorProfile(
            profileId = "test", occupation = "test",
            schedule = listOf(BehaviorProfile.ScheduleEntry("bad", 0, 6000, "INVALID"))
        )
        val issues = loader.validate(profile)
        assertTrue(issues.any { it.contains("slot") && it.contains("INVALID") })
    }

    @Test
    fun validateDetectsInvalidTimeRange() {
        val loader = BehaviorProfileLoader(null)
        val profile = BehaviorProfile(
            profileId = "test",
            schedule = listOf(BehaviorProfile.ScheduleEntry("bad", 6000, 3000, "HOME"))
        )
        val issues = loader.validate(profile)
        assertTrue(issues.any { it.contains("interval") })
    }

    @Test
    fun validateDetectsOverlappingEntries() {
        val loader = BehaviorProfileLoader(null)
        val profile = BehaviorProfile(
            profileId = "test",
            schedule = listOf(
                BehaviorProfile.ScheduleEntry("a", 0, 12000, "WORK"),
                BehaviorProfile.ScheduleEntry("b", 6000, 24000, "HOME")
            )
        )
        val issues = loader.validate(profile)
        assertTrue(issues.any { it.contains("suprapun") })
    }

    @Test
    fun validatePassesValidProfile() {
        val loader = BehaviorProfileLoader(null)
        val profile = BehaviorProfile(
            profileId = "test", occupation = "fermier",
            schedule = listOf(
                BehaviorProfile.ScheduleEntry("morning", 0, 6000, "WORK", "cultiva"),
                BehaviorProfile.ScheduleEntry("evening", 12000, 13800, "HOME", "odihna")
            )
        )
        val issues = loader.validate(profile)
        assertTrue(issues.isEmpty())
    }
}
