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
            default_profile: "default_villager"
            profiles:
              default_villager:
                occupation: "villager"
        """.trimIndent()
        val results = loader.parseYamlString(yaml)
        assertEquals(1, results.size)
        assertEquals("default_villager", loader.defaultProfileId())
        assertEquals("default_villager", loader.defaultProfile()?.profileId)
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
                routine_bias_ticks: -900
                routine_goals:
                  home: "sa se intoarca la casa si gospodarie"
                  work: "sa lucreze pe camp"
                  social: "sa vorbeasca cu ceilalti fermieri"
                  idle: "sa astepte sa se deschida campul"
                phase_ticks:
                  wake_start: 2000
                  work_start: 3000
                  midday_start: 12000
                  evening_start: 16000
                  social_start: 8000
                  social_end: 18000
                  night_start: 18000
                routine_texts:
                  work_day: "lucreaza pe camp"
                  fallback_idle: "asteapta o ancora"
                fallback_rules:
                  - condition: "night"
                    slot: "HOME"
                    activity_key: "night_sleep"
                    state: "SLEEPING"
                  - condition: "default"
                    slot: "IDLE"
                    activity_key: "fallback_idle"
                    state: "IDLE"
                thresholds:
                  energy_low: 20
                  hunger_low: 22
                  safety_low: 30
                  social_need_low: 33
                slot_states:
                  home: "RESTING"
                  work: "WORKING"
                  social: "SOCIALIZING"
                  idle: "IDLE"
                zone_states:
                  forest: "FARMING"
                  plains: "WORKING"
                  default: "WORKING"
                zone_activity_suffixes:
                  forest: "in padure"
                  plains: "pe camp"
                preview_points:
                  - label: "Noapte"
                    world_time: 19000
                  - label: "Dimineata"
                    world_time: 6000
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
        assertEquals(-900L, p.routineBiasTicks)
        assertEquals("sa lucreze pe camp", p.routineGoals["work"])
        assertEquals("sa se intoarca la casa si gospodarie", p.routineGoals["home"])
        assertEquals(3000L, p.phaseTicks["work_start"])
        assertEquals(18000L, p.phaseTicks["night_start"])
        assertEquals("lucreaza pe camp", p.routineTexts["work_day"])
        assertEquals("asteapta o ancora", p.routineTexts["fallback_idle"])
        assertEquals(2, p.fallbackRules.size)
        assertEquals("night", p.fallbackRules[0].condition)
        assertEquals("default", p.fallbackRules[1].condition)
        assertEquals(20, p.thresholds["energy_low"])
        assertEquals(33, p.thresholds["social_need_low"])
        assertEquals("RESTING", p.slotStates["home"])
        assertEquals("FARMING", p.zoneStates["forest"])
        assertEquals("pe camp", p.zoneActivitySuffixes["plains"])
        assertEquals(2, p.previewPoints.size)
        assertEquals("Noapte", p.previewPoints[0].label)
        assertEquals(19000L, p.previewPoints[0].worldTime)
        assertEquals("SLEEPING", p.fallbackRules[0].state)
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
                routine_bias_ticks: 1200
              trader:
                occupation: "negustor"
                socialize_chance: 0.6
                routine_bias_ticks: 500
        """.trimIndent()
        val results = loader.parseYamlString(yaml)
        assertEquals(2, results.size)
        assertTrue(results.any { it.profileId == "guard" })
        assertTrue(results.any { it.profileId == "trader" })
        assertEquals(1200L, results.first { it.profileId == "guard" }.routineBiasTicks)
        assertEquals(500L, results.first { it.profileId == "trader" }.routineBiasTicks)
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
