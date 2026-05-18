package ro.ainpc.engine

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ro.ainpc.addons.AddonType
import ro.ainpc.platform.RuntimeMode
import java.io.File
import java.util.EnumSet
import java.util.Locale

object FeaturePackMetadataValidator {
    @JvmStatic
    fun validate(config: YamlConfiguration, file: File?, runtimeMode: RuntimeMode?): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val effectiveRuntimeMode = runtimeMode ?: RuntimeMode.STANDALONE
        val packId = firstNonBlank(config.getString("id"), fallbackPackId(file))

        if (packId.isBlank()) {
            errors.add("id lipsa pentru feature pack")
        }

        val addonSection = config.getConfigurationSection("addon")
            ?: return ValidationResult(packId, errors, warnings)

        validateOptionalScalar(addonSection, "type", errors)
        validateOptionalScalar(addonSection, "version", errors)
        validateStringList(addonSection, "capabilities", errors)
        validateStringList(addonSection, "dependencies", errors)

        val type = addonSection.getString("type", "")
        if (!type.isNullOrBlank() && !isKnownAddonType(type)) {
            errors.add("addon.type necunoscut: $type")
        }

        if (addonSection.getBoolean("primary_scenario", false)
            && !type.isNullOrBlank()
            && AddonType.fromId(type) != AddonType.SCENARIO
        ) {
            warnings.add("primary_scenario este ignorat pentru addon.type=$type")
        }

        val declaredRuntimeModes = validateRuntimeModes(addonSection, errors)
        if (declaredRuntimeModes.isNotEmpty() && !declaredRuntimeModes.contains(effectiveRuntimeMode)) {
            errors.add("runtime-ul curent ${effectiveRuntimeMode.id} nu este inclus in addon.runtime_modes")
        }

        return ValidationResult(packId, errors, warnings)
    }

    private fun validateOptionalScalar(section: ConfigurationSection, key: String, errors: MutableList<String>) {
        if (!section.contains(key)) {
            return
        }
        val value = section.getString(key, "")
        if (value.isNullOrBlank()) {
            errors.add("addon.$key este gol")
        }
    }

    private fun validateStringList(section: ConfigurationSection, key: String, errors: MutableList<String>) {
        if (!section.contains(key)) {
            return
        }
        if (!section.isList(key)) {
            errors.add("addon.$key trebuie sa fie lista")
            return
        }
        for (value in section.getStringList(key)) {
            if (value.isBlank()) {
                errors.add("addon.$key contine valoare goala")
            }
        }
    }

    private fun validateRuntimeModes(addonSection: ConfigurationSection, errors: MutableList<String>): Set<RuntimeMode> {
        if (!addonSection.contains("runtime_modes")) {
            return EnumSet.allOf(RuntimeMode::class.java)
        }
        if (!addonSection.isList("runtime_modes")) {
            errors.add("addon.runtime_modes trebuie sa fie lista")
            return EnumSet.noneOf(RuntimeMode::class.java)
        }

        val runtimeModes = EnumSet.noneOf(RuntimeMode::class.java)
        for (value in addonSection.getStringList("runtime_modes")) {
            if (value.isBlank()) {
                errors.add("addon.runtime_modes contine valoare goala")
                continue
            }
            val runtimeMode = findRuntimeMode(value)
            if (runtimeMode == null) {
                errors.add("addon.runtime_modes contine runtime necunoscut: $value")
                continue
            }
            runtimeModes.add(runtimeMode)
        }
        return runtimeModes
    }

    private fun findRuntimeMode(value: String): RuntimeMode? {
        val normalized = normalize(value)
        for (mode in RuntimeMode.entries) {
            if (mode.id.equals(normalized, ignoreCase = true) || mode.name.equals(normalized, ignoreCase = true)) {
                return mode
            }
        }
        return null
    }

    private fun isKnownAddonType(value: String): Boolean {
        val normalized = normalize(value)
        for (type in AddonType.entries) {
            if (type.id.equals(normalized, ignoreCase = true) || type.name.equals(normalized, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun fallbackPackId(file: File?): String {
        val name = file?.name ?: return ""
        val lowerName = name.lowercase(Locale.ROOT)
        if (lowerName.endsWith(".yaml")) {
            return name.substring(0, name.length - 5)
        }
        if (lowerName.endsWith(".yml")) {
            return name.substring(0, name.length - 4)
        }
        return name
    }

    private fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    private fun normalize(value: String?): String = value?.trim()?.lowercase(Locale.ROOT) ?: ""

    data class ValidationResult(val packId: String, val errors: List<String>, val warnings: List<String>) {
        fun packId(): String = packId
        fun errors(): List<String> = errors
        fun warnings(): List<String> = warnings
        fun valid(): Boolean = errors.isEmpty()
    }
}
