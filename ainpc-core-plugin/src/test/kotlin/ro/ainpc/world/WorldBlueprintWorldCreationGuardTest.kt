package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class WorldBlueprintWorldCreationGuardTest {
    @TempDir
    lateinit var directory: Path

    private val complete = WorldBlueprint(
        "new_world", "Poveste", mapOf("clima" to "Rece"), 1,
        mapOf("fierar" to WorldProfessionBlueprint(2, "Atelier")),
        "Case din piatra", mapOf("drumuri" to "Leaga satele"),
    )
    private val fingerprint = "a".repeat(64)
    private val confirmed = complete.copy(approval = WorldBlueprintApproval(fingerprint, "admin", 123L))

    private fun issue(
        blueprint: WorldBlueprint? = confirmed,
        actual: String? = fingerprint,
        expected: String = fingerprint,
        loaded: Boolean = false,
        status: WorldBlueprintWorldDirectoryStatus = WorldBlueprintWorldDirectoryStatus.AVAILABLE,
    ): String? = WorldBlueprintWorldCreationGuard.issue(blueprint, actual, expected, loaded, status)

    @Test
    fun `allows only confirmed exact revision for a new empty directory`() {
        assertNull(issue())
        assertEquals("Nu exista draft pentru aceasta lume.", issue(blueprint = null, actual = null))
        assertEquals("Lumea este deja incarcata; comanda nu reincarca lumi.", issue(loaded = true))
        assertEquals("Draftul curent nu este confirmat; ruleaza review si confirm.", issue(blueprint = complete))
        assertEquals("Draftul curent nu este confirmat; ruleaza review si confirm.", issue(actual = "b".repeat(64)))
        assertEquals("Amprenta nu corespunde reviziei confirmate; ruleaza review din nou.", issue(expected = "b".repeat(64)))
        assertEquals("Draft incomplet: povestea lumii lipseste", issue(blueprint = confirmed.copy(story = "")))
    }

    @Test
    fun `rejects directory collisions and uncertainty`() {
        for (status in WorldBlueprintWorldDirectoryStatus.entries - WorldBlueprintWorldDirectoryStatus.AVAILABLE) {
            assertNotNull(issue(status = status), status.name)
        }
    }

    @Test
    fun `rejects invalid world seed even when the draft was confirmed`() {
        val invalid = complete.copy(mapRules = complete.mapRules +
            (WorldBlueprintWorldSeedRule.KEY to "9223372036854775808"))
        val store = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        store.save(invalid)
        val reviewed = store.fingerprint(invalid)
        store.confirm(invalid.worldName, reviewed, "admin")
        val persisted = store.get(invalid.worldName)

        assertEquals("Regula world_seed este invalida; foloseste un intreg zecimal pe 64 de biti.",
            WorldBlueprintWorldCreationGuard.issue(persisted, persisted?.let(store::fingerprint), reviewed,
                false, WorldBlueprintWorldDirectoryStatus.AVAILABLE))
    }

    @Test
    fun `rejects invalid world generation rules before creation`() {
        val invalid = confirmed.copy(mapRules = confirmed.mapRules + mapOf(
            WorldBlueprintWorldGenerationRules.TYPE_KEY to "custom",
            WorldBlueprintWorldGenerationRules.ENVIRONMENT_KEY to "custom",
            WorldBlueprintWorldGenerationRules.STRUCTURES_KEY to "yes",
        ))
        assertEquals("Reguli de generare invalide: world_type, world_environment, world_generate_structures; consulta plan.",
            issue(blueprint = invalid))
    }

    @Test
    fun `rejects world type combined with non normal environment`() {
        val invalid = confirmed.copy(mapRules = confirmed.mapRules + mapOf(
            WorldBlueprintWorldGenerationRules.TYPE_KEY to "flat",
            WorldBlueprintWorldGenerationRules.ENVIRONMENT_KEY to "nether",
        ))
        assertEquals("Reguli de generare invalide: world_type; consulta plan.", issue(blueprint = invalid))
    }

    @Test
    fun `accepts persisted confirmation and rejects a later draft edit`() {
        val store = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        store.save(complete)
        val reviewed = store.fingerprint(complete)
        store.confirm(complete.worldName, reviewed, "admin")
        val persisted = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        val draft = persisted.get(complete.worldName)
        assertNull(WorldBlueprintWorldCreationGuard.issue(draft, draft?.let(persisted::fingerprint), reviewed,
            false, WorldBlueprintWorldDirectoryStatus.AVAILABLE))

        store.save(complete.copy(story = "Poveste schimbata"))
        val changed = persisted.get(complete.worldName)
        assertNotNull(WorldBlueprintWorldCreationGuard.issue(changed, changed?.let(persisted::fingerprint), reviewed,
            false, WorldBlueprintWorldDirectoryStatus.AVAILABLE))
    }
}
