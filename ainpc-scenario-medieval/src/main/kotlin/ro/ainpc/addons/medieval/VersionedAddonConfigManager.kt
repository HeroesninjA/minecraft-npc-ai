package ro.ainpc.addons.medieval

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale

internal class VersionedAddonConfigManager(
    private val addonId: String,
    private val currentVersion: Int,
) {
    data class Result(
        val config: YamlConfiguration?,
        val sourceVersion: Int,
        val migrated: Boolean,
        val backupPath: Path?,
        val warnings: List<String>,
        val errors: List<String>,
    ) {
        val valid: Boolean
            get() = config != null && errors.isEmpty()
    }

    fun prepare(configPath: Path, templatePath: Path): Result {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val template = loadStrict(templatePath, "template", errors)
            ?: return result(null, 0, false, null, warnings, errors)
        val config = loadStrict(configPath, "config", errors)
            ?: return result(null, 0, false, null, warnings, errors)

        validateTemplate(template, errors)
        validateAddonId(config, errors)
        validateValueTypes(config, template, errors)
        val sourceVersion = readVersion(config, "config", allowMissing = true, warnings, errors)
        val mode = readMigrationMode(config, errors)

        if (sourceVersion > currentVersion) {
            errors += "addon.config_version=$sourceVersion este mai nou decat versiunea suportata $currentVersion"
        }
        if (errors.isNotEmpty() || mode == null) {
            return result(null, sourceVersion, false, null, warnings, errors)
        }

        val missingPaths = missingLeafPaths(config, template)
        if (sourceVersion == currentVersion && missingPaths.isEmpty()) {
            return result(config, sourceVersion, false, null, warnings, errors)
        }

        if (mode == MigrationMode.VALIDATE_ONLY) {
            if (sourceVersion < currentVersion) {
                warnings += "config versiunea $sourceVersion necesita migrare la $currentVersion"
            }
            if (missingPaths.isNotEmpty()) {
                warnings += "config are ${missingPaths.size} chei lipsa fata de template"
            }
            return result(config, sourceVersion, false, null, warnings, errors)
        }

        val backupPath = configPath.resolveSibling("${configPath.fileName}.v$sourceVersion.bak")
        return try {
            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING)
            mergeMissingValues(config, template)
            config.set(CONFIG_VERSION_PATH, currentVersion)
            saveAtomically(config, configPath)

            val migratedConfig = loadStrict(configPath, "config migrat", errors)
            if (migratedConfig != null) {
                validateAddonId(migratedConfig, errors)
                validateValueTypes(migratedConfig, template, errors)
            }
            if (errors.isNotEmpty() || migratedConfig == null) {
                Files.copy(backupPath, configPath, StandardCopyOption.REPLACE_EXISTING)
                errors += "migrarea a esuat; config-ul initial a fost restaurat"
                result(null, sourceVersion, false, backupPath, warnings, errors)
            } else {
                warnings += if (sourceVersion < currentVersion) {
                    "config migrat de la versiunea $sourceVersion la $currentVersion"
                } else {
                    "config completat cu ${missingPaths.size} chei lipsa"
                }
                result(migratedConfig, sourceVersion, true, backupPath, warnings, errors)
            }
        } catch (exception: IOException) {
            errors += "nu s-a putut migra config-ul: ${exception.message}"
            result(null, sourceVersion, false, backupPath.takeIf(Files::exists), warnings, errors)
        }
    }

    private fun validateTemplate(template: YamlConfiguration, errors: MutableList<String>) {
        val templateId = template.getString(ADDON_ID_PATH, "")?.trim().orEmpty()
        if (templateId != addonId) {
            errors += "template addon.id trebuie sa fie '$addonId', gasit '$templateId'"
        }
        val templateVersion = readVersion(
            template,
            "template",
            allowMissing = false,
            warnings = mutableListOf(),
            errors = errors,
        )
        if (templateVersion != currentVersion) {
            errors += "template addon.config_version=$templateVersion, asteptat $currentVersion"
        }
    }

    private fun validateAddonId(config: YamlConfiguration, errors: MutableList<String>) {
        val configuredId = config.getString(ADDON_ID_PATH, "")?.trim().orEmpty()
        if (configuredId != addonId) {
            errors += "config addon.id trebuie sa fie '$addonId', gasit '$configuredId'"
        }
    }

    private fun validateValueTypes(
        config: YamlConfiguration,
        template: YamlConfiguration,
        errors: MutableList<String>,
    ) {
        for (path in template.getKeys(true).sorted()) {
            if (!config.contains(path)) {
                continue
            }
            if (template.isConfigurationSection(path)) {
                if (!config.isConfigurationSection(path)) {
                    errors += "$path trebuie sa fie sectiune"
                }
                continue
            }
            val expected = template.get(path) ?: continue
            val actual = config.get(path) ?: continue
            if (!hasCompatibleType(expected, actual)) {
                errors += "$path are tip ${actual.javaClass.simpleName}, asteptat ${expected.javaClass.simpleName}"
            }
        }
    }

    private fun hasCompatibleType(expected: Any, actual: Any): Boolean {
        return when (expected) {
            is Boolean -> actual is Boolean
            is Byte, is Short, is Int, is Long ->
                actual is Number && actual.toDouble().isFinite() && actual.toDouble() % 1.0 == 0.0
            is Float, is Double -> actual is Number
            is String -> actual is String
            is List<*> -> actual is List<*>
            else -> expected.javaClass.isInstance(actual)
        }
    }

    private fun missingLeafPaths(config: YamlConfiguration, template: YamlConfiguration): List<String> {
        return template.getKeys(true)
            .asSequence()
            .filterNot(template::isConfigurationSection)
            .filterNot(config::contains)
            .sorted()
            .toList()
    }

    private fun mergeMissingValues(config: YamlConfiguration, template: YamlConfiguration) {
        for (path in missingLeafPaths(config, template)) {
            config.set(path, template.get(path))
        }
    }

    private fun readVersion(
        config: YamlConfiguration,
        source: String,
        allowMissing: Boolean,
        warnings: MutableList<String>,
        errors: MutableList<String>,
    ): Int {
        val value = config.get(CONFIG_VERSION_PATH)
        if (value == null && allowMissing) {
            warnings += "$source fara addon.config_version; este tratat ca versiunea 0"
            return 0
        }
        if (value !is Number || value.toDouble() % 1.0 != 0.0) {
            errors += "$source addon.config_version trebuie sa fie numar intreg"
            return -1
        }
        val version = value.toInt()
        if (version < 0) {
            errors += "$source addon.config_version nu poate fi negativ"
        }
        return version
    }

    private fun readMigrationMode(config: YamlConfiguration, errors: MutableList<String>): MigrationMode? {
        val rawMode = config.getString(MIGRATION_MODE_PATH, MigrationMode.AUTO.id)
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        val mode = MigrationMode.entries.find { it.id == rawMode }
        if (mode == null) {
            errors += "$MIGRATION_MODE_PATH trebuie sa fie auto sau validate_only"
        }
        return mode
    }

    private fun loadStrict(path: Path, label: String, errors: MutableList<String>): YamlConfiguration? {
        val configuration = YamlConfiguration()
        return try {
            configuration.load(path.toFile())
            configuration
        } catch (exception: IOException) {
            errors += "$label nu poate fi citit: ${exception.message}"
            null
        } catch (exception: InvalidConfigurationException) {
            errors += "$label are YAML invalid: ${exception.message}"
            null
        }
    }

    private fun saveAtomically(config: YamlConfiguration, path: Path) {
        val temporaryPath = path.resolveSibling("${path.fileName}.tmp")
        try {
            config.save(temporaryPath.toFile())
            try {
                Files.move(
                    temporaryPath,
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporaryPath)
        }
    }

    private fun result(
        config: YamlConfiguration?,
        sourceVersion: Int,
        migrated: Boolean,
        backupPath: Path?,
        warnings: List<String>,
        errors: List<String>,
    ): Result {
        return Result(
            config,
            sourceVersion,
            migrated,
            backupPath,
            warnings.toList(),
            errors.toList(),
        )
    }

    private enum class MigrationMode(val id: String) {
        AUTO("auto"),
        VALIDATE_ONLY("validate_only"),
    }

    companion object {
        private const val ADDON_ID_PATH = "addon.id"
        private const val CONFIG_VERSION_PATH = "addon.config_version"
        private const val MIGRATION_MODE_PATH = "addon.config_migration"
    }
}
