package ro.ainpc.addons.medieval

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class VersionedAddonConfigManagerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun autoMigrationBacksUpAndPreservesOperatorValues() {
        val templatePath = copyTemplate()
        val configPath = writeConfig(
            """
            addon:
              id: "ainpc-scenario-medieval"
              config_version: 1
              enabled: true
            content:
              install_pack: false
              playable_content: true
            progression:
              side_quests:
                max_active: 7
            custom:
              operator_note: "keep"
            """.trimIndent(),
        )

        val result = manager().prepare(configPath, templatePath)

        assertTrue(result.valid, result.errors.joinToString())
        assertTrue(result.migrated)
        assertEquals(1, result.sourceVersion)
        assertNotNull(result.backupPath)
        assertTrue(Files.exists(result.backupPath))

        val migrated = loadStrict(configPath)
        assertEquals(2, migrated.getInt("addon.config_version"))
        assertEquals("auto", migrated.getString("addon.config_migration"))
        assertFalse(migrated.getBoolean("content.install_pack"))
        assertEquals(7, migrated.getInt("progression.side_quests.max_active"))
        assertEquals("medieval", migrated.getString("dialogue.tone"))
        assertEquals("keep", migrated.getString("custom.operator_note"))

        val backup = loadStrict(requireNotNull(result.backupPath))
        assertEquals(1, backup.getInt("addon.config_version"))
        assertFalse(backup.contains("addon.config_migration"))
    }

    @Test
    fun validateOnlyReportsMigrationWithoutWriting() {
        val templatePath = copyTemplate()
        val configPath = writeConfig(
            """
            addon:
              id: "ainpc-scenario-medieval"
              config_version: 1
              config_migration: "validate_only"
              enabled: true
            content:
              install_pack: true
            """.trimIndent(),
        )
        val original = Files.readString(configPath)

        val result = manager().prepare(configPath, templatePath)

        assertTrue(result.valid, result.errors.joinToString())
        assertFalse(result.migrated)
        assertNull(result.backupPath)
        assertTrue(result.warnings.any { warning -> warning.contains("necesita migrare") })
        assertTrue(result.warnings.any { warning -> warning.contains("chei lipsa") })
        assertEquals(original, Files.readString(configPath))
    }

    @Test
    fun rejectsFutureConfigVersionWithoutDowngrade() {
        val templatePath = copyTemplate()
        val configPath = writeConfig(
            """
            addon:
              id: "ainpc-scenario-medieval"
              config_version: 99
              config_migration: "auto"
            """.trimIndent(),
        )

        val result = manager().prepare(configPath, templatePath)

        assertFalse(result.valid)
        assertFalse(result.migrated)
        assertNull(result.backupPath)
        assertTrue(result.errors.any { error -> error.contains("mai nou") })
        assertEquals(99, loadStrict(configPath).getInt("addon.config_version"))
    }

    @Test
    fun rejectsConfiguredValueWithWrongTemplateType() {
        val templatePath = copyTemplate()
        val configPath = writeConfig(
            """
            addon:
              id: "ainpc-scenario-medieval"
              config_version: 2
              config_migration: "auto"
            content:
              install_pack: "yes"
            progression:
              side_quests:
                max_active: 2.5
            """.trimIndent(),
        )

        val result = manager().prepare(configPath, templatePath)

        assertFalse(result.valid)
        assertFalse(result.migrated)
        assertTrue(result.errors.any { error -> error.contains("content.install_pack") })
        assertTrue(result.errors.any { error -> error.contains("progression.side_quests.max_active") })
    }

    private fun manager(): VersionedAddonConfigManager {
        return VersionedAddonConfigManager("ainpc-scenario-medieval", 2)
    }

    private fun copyTemplate(): Path {
        val resource = requireNotNull(javaClass.classLoader.getResource("config-template.yml")) {
            "Missing medieval config template"
        }
        val target = tempDir.resolve("config-template.yml")
        resource.openStream().use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
        return target
    }

    private fun writeConfig(content: String): Path {
        val path = tempDir.resolve("config.yml")
        Files.writeString(path, content)
        return path
    }

    private fun loadStrict(path: Path): YamlConfiguration {
        return YamlConfiguration().apply { load(path.toFile()) }
    }
}
