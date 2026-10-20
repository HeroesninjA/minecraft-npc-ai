package ro.ainpc.managers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcEntityKind
import ro.ainpc.npc.NpcInteractionProfile
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcSimulationMode
import java.util.UUID

class ScenarioActorProfileDataTest {
    private val gson = Gson()

    @Test
    fun `scenario actor metadata survives profile serialization`() {
        val actor = AINPC(null).apply {
            profileSource = "scenario_actor"
            scenarioSessionId = UUID.randomUUID()
            lifecycleType = NpcLifecycleType.PERMANENT
            persistenceMode = NpcPersistenceMode.LIGHT
            simulationMode = NpcSimulationMode.NONE
            interactionProfile = NpcInteractionProfile.QUEST_ONLY
            entityKind = NpcEntityKind.SPIRIT
            entityArchetype = "specter"
            ownerScenarioId = "village_story"
            ownerQuestId = "find_relic"
            spawnSource = "quest"
            despawnRule = "on_stage_end"
            durationSeconds = 90
            temporaryTags.addAll(listOf("story", "specter"))
        }

        val serialized = buildProfileData(actor, gson)
        val metadata = ScenarioActorProfileData.read(serialized, gson)!!
        val reloaded = AINPC(null)
        metadata.applyTo(reloaded)

        assertEquals(NpcLifecycleType.PERMANENT, reloaded.lifecycleType)
        assertEquals(NpcPersistenceMode.LIGHT, reloaded.persistenceMode)
        assertEquals(NpcSimulationMode.NONE, reloaded.simulationMode)
        assertEquals(NpcInteractionProfile.QUEST_ONLY, reloaded.interactionProfile)
        assertEquals(NpcEntityKind.SPIRIT, reloaded.entityKind)
        assertEquals("specter", reloaded.entityArchetype)
        assertEquals("village_story", reloaded.ownerScenarioId)
        assertEquals(actor.scenarioSessionId, reloaded.scenarioSessionId)
        assertEquals("find_relic", reloaded.ownerQuestId)
        assertEquals("quest", reloaded.spawnSource)
        assertEquals("on_stage_end", reloaded.despawnRule)
        assertEquals(90L, reloaded.durationSeconds)
        assertEquals(setOf("story", "specter"), reloaded.temporaryTags)
        assertEquals(
            ScenarioActorProfileData.RestartDisposition.LOAD,
            ScenarioActorProfileData.restartDisposition(actor.profileSource, serialized, gson),
        )
    }

    @Test
    fun `transient and legacy scenario actors remain in database but not runtime after restart`() {
        val actor = AINPC(null).apply {
            profileSource = "scenario_actor"
            lifecycleType = NpcLifecycleType.TEMPORARY
        }
        val serialized = buildProfileData(actor, gson)

        assertEquals(
            ScenarioActorProfileData.RestartDisposition.QUARANTINE,
            ScenarioActorProfileData.restartDisposition(actor.profileSource, serialized, gson),
        )
        assertEquals(
            ScenarioActorProfileData.RestartDisposition.QUARANTINE,
            ScenarioActorProfileData.restartDisposition("scenario_actor", "{}", gson),
        )
        assertEquals(
            ScenarioActorProfileData.RestartDisposition.QUARANTINE,
            ScenarioActorProfileData.restartDisposition("scenario_actor", "not-json", gson),
        )
        actor.lifecycleType = NpcLifecycleType.PERMANENT
        actor.persistenceMode = NpcPersistenceMode.RUNTIME_ONLY
        assertEquals(
            ScenarioActorProfileData.RestartDisposition.QUARANTINE,
            ScenarioActorProfileData.restartDisposition(actor.profileSource, buildProfileData(actor, gson), gson),
        )
    }

    @Test
    fun `older permanent actor metadata remains loadable without a session marker`() {
        val actor = AINPC(null).apply {
            profileSource = "scenario_actor"
            lifecycleType = NpcLifecycleType.PERMANENT
            persistenceMode = NpcPersistenceMode.LIGHT
        }
        val serialized = buildProfileData(actor, gson)

        assertNull(ScenarioActorProfileData.read(serialized, gson)?.scenarioSessionId)
        assertEquals(
            ScenarioActorProfileData.RestartDisposition.LOAD,
            ScenarioActorProfileData.restartDisposition(actor.profileSource, serialized, gson),
        )
    }

    @Test
    fun `malformed metadata cannot promote a scenario actor to permanent`() {
        val profile = gson.fromJson(
            buildProfileData(AINPC(null).apply {
                profileSource = "scenario_actor"
                lifecycleType = NpcLifecycleType.PERMANENT
            }, gson),
            JsonObject::class.java,
        )
        profile.getAsJsonObject("scenario_actor").addProperty("lifecycle_type", "UNKNOWN")

        assertNull(ScenarioActorProfileData.read(profile))
        assertEquals(
            ScenarioActorProfileData.RestartDisposition.QUARANTINE,
            ScenarioActorProfileData.restartDisposition("scenario_actor", gson.toJson(profile), gson),
        )
        profile.getAsJsonObject("scenario_actor").addProperty("lifecycle_type", "PERMANENT")
        profile.getAsJsonObject("scenario_actor").addProperty("version", 2)
        assertEquals(
            ScenarioActorProfileData.RestartDisposition.QUARANTINE,
            ScenarioActorProfileData.restartDisposition("scenario_actor", gson.toJson(profile), gson),
        )
        profile.getAsJsonObject("scenario_actor").addProperty("version", 1)
        profile.getAsJsonObject("scenario_actor").addProperty("scenario_session_id", "not-a-uuid")
        assertNull(ScenarioActorProfileData.read(profile))
    }

    @Test
    fun `ordinary npc profiles are unchanged`() {
        val profile = gson.fromJson(buildProfileData(AINPC(null), gson), JsonObject::class.java)

        assertFalse(profile.has("scenario_actor"))
        assertTrue(ScenarioActorProfileData.read(profile) == null)
        assertEquals(
            ScenarioActorProfileData.RestartDisposition.LOAD,
            ScenarioActorProfileData.restartDisposition("manual", "{}", gson),
        )
    }
}
