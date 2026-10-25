package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

class WorldBlueprintWizardPersistenceTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `own saved revision advances subsequent answers`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)

        val story = store.advanceWizard("world", WorldBlueprintWizardStep.STORY, "Doua sate rivale", null)
        assertNotNull(story.fingerprint)
        assertEquals(WorldBlueprintWizardStep.PROPERTY, story.update.nextStep)
        assertEquals(story.update.blueprint, WorldBlueprintStore(file).get("world"))
        assertEquals(store.fingerprint(story.update.blueprint), story.fingerprint)

        val property = store.advanceWizard("world", story.update.nextStep, "clima: Ierni lungi", story.fingerprint)

        assertNotEquals(story.fingerprint, property.fingerprint)
        assertEquals("Doua sate rivale", property.update.blueprint.story)
        assertEquals("Ierni lungi", property.update.blueprint.properties["clima"])
        assertEquals(property.update.blueprint, WorldBlueprintStore(file).get("world"))
        assertEquals(store.fingerprint(property.update.blueprint), property.fingerprint)
    }

    @Test
    fun `another editor invalidates edits and gata before parsing the answer`() {
        val file = directory.resolve("world-blueprints.json")
        val editor = WorldBlueprintStore(file)
        val otherEditor = WorldBlueprintStore(file)
        val original = completeBlueprint()
        editor.save(original)
        val expectedFingerprint = editor.fingerprint(original)
        val updated = original.copy(properties = original.properties + ("pericol" to "Banditi la nord"))
        otherEditor.save(updated)
        val before = Files.readString(file)
        val modifiedAt = freezeModificationTime(file)

        assertThrows(WorldBlueprintRevisionConflictException::class.java) {
            editor.advanceWizard("world", WorldBlueprintWizardStep.STORY, "Poveste concurenta", expectedFingerprint)
        }
        assertThrows(WorldBlueprintRevisionConflictException::class.java) {
            editor.advanceWizard("world", WorldBlueprintWizardStep.PROPERTY, "gata", expectedFingerprint)
        }
        assertThrows(WorldBlueprintRevisionConflictException::class.java) {
            editor.advanceWizard("world", WorldBlueprintWizardStep.PROPERTY, " ", expectedFingerprint)
        }

        assertEquals(updated, otherEditor.get("world"))
        assertEquals(before, Files.readString(file))
        assertEquals(modifiedAt, Files.getLastModifiedTime(file))
    }

    @Test
    fun `absent revision cannot overwrite a draft created by another editor`() {
        val file = directory.resolve("world-blueprints.json")
        val editor = WorldBlueprintStore(file)
        val otherEditor = WorldBlueprintStore(file)
        assertNull(editor.get("world"))
        otherEditor.save(WorldBlueprint("world"))
        val before = Files.readString(file)
        val modifiedAt = freezeModificationTime(file)

        assertThrows(WorldBlueprintRevisionConflictException::class.java) {
            editor.advanceWizard("world", WorldBlueprintWizardStep.STORY, "Poveste noua", null)
        }
        assertThrows(WorldBlueprintRevisionConflictException::class.java) {
            editor.advanceWizard("world", WorldBlueprintWizardStep.PROFESSION_LIMIT, "16", null)
        }

        assertEquals(WorldBlueprint("world"), otherEditor.get("world"))
        assertEquals(before, Files.readString(file))
        assertEquals(modifiedAt, Files.getLastModifiedTime(file))
    }

    @Test
    fun `deleted persisted draft is not recreated by a stale wizard`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val original = completeBlueprint()
        store.save(original)
        val expectedFingerprint = store.fingerprint(original)
        Files.delete(file)

        assertThrows(WorldBlueprintRevisionConflictException::class.java) {
            store.advanceWizard("world", WorldBlueprintWizardStep.STORY, "Poveste noua", expectedFingerprint)
        }

        assertFalse(Files.exists(file))
        assertNull(WorldBlueprintStore(file).get("world"))
    }

    @Test
    fun `invalid answer preserves the revision for a corrected retry`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val original = completeBlueprint()
        store.save(original)
        val expectedFingerprint = store.fingerprint(original)
        val before = Files.readString(file)
        val modifiedAt = freezeModificationTime(file)

        assertThrows(IllegalArgumentException::class.java) {
            store.advanceWizard("world", WorldBlueprintWizardStep.PROFESSION_LIMIT, "65", expectedFingerprint)
        }

        assertEquals(original, WorldBlueprintStore(file).get("world"))
        assertEquals(before, Files.readString(file))
        assertEquals(modifiedAt, Files.getLastModifiedTime(file))

        val corrected = store.advanceWizard("world", WorldBlueprintWizardStep.PROFESSION_LIMIT, "3", expectedFingerprint)

        assertEquals(3, corrected.update.blueprint.professionTypeLimit)
        assertNotEquals(expectedFingerprint, corrected.fingerprint)
        assertEquals(corrected.update.blueprint, WorldBlueprintStore(file).get("world"))
    }

    @Test
    fun `approval changes allow no-op and gata while actual edits revoke approval`() {
        val file = directory.resolve("world-blueprints.json")
        val editor = WorldBlueprintStore(file)
        val otherEditor = WorldBlueprintStore(file)
        val original = completeBlueprint()
        editor.save(original)
        val expectedFingerprint = editor.fingerprint(original)
        val approval = otherEditor.confirm("world", expectedFingerprint, "admin")
        val before = Files.readString(file)
        val modifiedAt = freezeModificationTime(file)

        val unchanged = editor.advanceWizard("world", WorldBlueprintWizardStep.STORY, original.story, expectedFingerprint)
        val continued = editor.advanceWizard("world", WorldBlueprintWizardStep.PROPERTY, "gata", unchanged.fingerprint)

        assertEquals(expectedFingerprint, unchanged.fingerprint)
        assertEquals(expectedFingerprint, continued.fingerprint)
        assertEquals(approval, unchanged.update.blueprint.approval)
        assertEquals(approval, continued.update.blueprint.approval)
        assertEquals(WorldBlueprintWizardStep.PROFESSION_LIMIT, continued.update.nextStep)
        assertEquals(before, Files.readString(file))
        assertEquals(modifiedAt, Files.getLastModifiedTime(file))

        val edited = editor.advanceWizard("world", WorldBlueprintWizardStep.PROPERTY, "clima: Cald", continued.fingerprint)

        assertNotEquals(expectedFingerprint, edited.fingerprint)
        assertEquals("Cald", edited.update.blueprint.properties["clima"])
        assertNull(edited.update.blueprint.approval)
        assertEquals(edited.update.blueprint, otherEditor.get("world"))
    }

    @Test
    fun `no-op on an absent draft retains null revision and does not create a file`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)

        val unchanged = store.advanceWizard("world", WorldBlueprintWizardStep.PROFESSION_LIMIT, "16", null)

        assertNull(unchanged.fingerprint)
        assertEquals(WorldBlueprint("world"), unchanged.update.blueprint)
        assertEquals(WorldBlueprintWizardStep.PROFESSION, unchanged.update.nextStep)
        assertFalse(Files.exists(file))

        val created = store.advanceWizard("world", unchanged.update.nextStep, "fierar 2 Repara unelte", unchanged.fingerprint)

        assertNotNull(created.fingerprint)
        assertEquals(2, created.update.blueprint.professions["fierar"]?.maxNpcs)
        assertEquals(created.update.blueprint, WorldBlueprintStore(file).get("world"))
    }

    @Test
    fun `edits to another world do not invalidate or disappear after a wizard answer`() {
        val file = directory.resolve("world-blueprints.json")
        val editor = WorldBlueprintStore(file)
        val otherEditor = WorldBlueprintStore(file)
        val original = completeBlueprint()
        editor.save(original)
        val expectedFingerprint = editor.fingerprint(original)
        val otherWorld = WorldBlueprint("nether", story = "Taram vulcanic")
        otherEditor.save(otherWorld)

        val progress = editor.advanceWizard("world", WorldBlueprintWizardStep.STORY, "Poveste noua", expectedFingerprint)

        assertEquals("Poveste noua", otherEditor.get("world")?.story)
        assertEquals(progress.update.blueprint, otherEditor.get("world"))
        assertEquals(otherWorld, otherEditor.get("nether"))
    }

    private fun completeBlueprint() = WorldBlueprint(
        "world", "Poveste", mapOf("clima" to "Rece"), 2,
        mapOf("fierar" to WorldProfessionBlueprint(2, "Atelier")),
        "Case de piatra", mapOf("drumuri" to "Leaga satele"),
    )

    private fun freezeModificationTime(file: Path): FileTime {
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_600_000_000_000L))
        return Files.getLastModifiedTime(file)
    }
}
