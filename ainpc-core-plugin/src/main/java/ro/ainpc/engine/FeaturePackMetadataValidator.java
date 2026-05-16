package ro.ainpc.engine;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ro.ainpc.addons.AddonType;
import ro.ainpc.platform.RuntimeMode;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class FeaturePackMetadataValidator {

    private FeaturePackMetadataValidator() {
    }

    static ValidationResult validate(YamlConfiguration config, File file, RuntimeMode runtimeMode) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        RuntimeMode effectiveRuntimeMode = runtimeMode != null ? runtimeMode : RuntimeMode.STANDALONE;
        String packId = firstNonBlank(config.getString("id"), fallbackPackId(file));

        if (packId.isBlank()) {
            errors.add("id lipsa pentru feature pack");
        }

        ConfigurationSection addonSection = config.getConfigurationSection("addon");
        if (addonSection == null) {
            return new ValidationResult(packId, errors, warnings);
        }

        validateOptionalScalar(addonSection, "type", errors);
        validateOptionalScalar(addonSection, "version", errors);
        validateStringList(addonSection, "capabilities", errors);
        validateStringList(addonSection, "dependencies", errors);

        String type = addonSection.getString("type", "");
        if (!type.isBlank() && !isKnownAddonType(type)) {
            errors.add("addon.type necunoscut: " + type);
        }

        if (addonSection.getBoolean("primary_scenario", false)
            && !type.isBlank()
            && AddonType.fromId(type) != AddonType.SCENARIO) {
            warnings.add("primary_scenario este ignorat pentru addon.type=" + type);
        }

        Set<RuntimeMode> declaredRuntimeModes = validateRuntimeModes(addonSection, errors);
        if (!declaredRuntimeModes.isEmpty() && !declaredRuntimeModes.contains(effectiveRuntimeMode)) {
            errors.add("runtime-ul curent " + effectiveRuntimeMode.getId()
                + " nu este inclus in addon.runtime_modes");
        }

        return new ValidationResult(packId, errors, warnings);
    }

    private static void validateOptionalScalar(ConfigurationSection section, String key, List<String> errors) {
        if (!section.contains(key)) {
            return;
        }
        String value = section.getString(key, "");
        if (value.isBlank()) {
            errors.add("addon." + key + " este gol");
        }
    }

    private static void validateStringList(ConfigurationSection section, String key, List<String> errors) {
        if (!section.contains(key)) {
            return;
        }
        if (!section.isList(key)) {
            errors.add("addon." + key + " trebuie sa fie lista");
            return;
        }
        for (String value : section.getStringList(key)) {
            if (value == null || value.isBlank()) {
                errors.add("addon." + key + " contine valoare goala");
            }
        }
    }

    private static Set<RuntimeMode> validateRuntimeModes(ConfigurationSection addonSection, List<String> errors) {
        if (!addonSection.contains("runtime_modes")) {
            return EnumSet.allOf(RuntimeMode.class);
        }
        if (!addonSection.isList("runtime_modes")) {
            errors.add("addon.runtime_modes trebuie sa fie lista");
            return EnumSet.noneOf(RuntimeMode.class);
        }

        EnumSet<RuntimeMode> runtimeModes = EnumSet.noneOf(RuntimeMode.class);
        for (String value : addonSection.getStringList("runtime_modes")) {
            if (value == null || value.isBlank()) {
                errors.add("addon.runtime_modes contine valoare goala");
                continue;
            }
            RuntimeMode runtimeMode = findRuntimeMode(value);
            if (runtimeMode == null) {
                errors.add("addon.runtime_modes contine runtime necunoscut: " + value);
                continue;
            }
            runtimeModes.add(runtimeMode);
        }
        return runtimeModes;
    }

    private static RuntimeMode findRuntimeMode(String value) {
        String normalized = normalize(value);
        for (RuntimeMode mode : RuntimeMode.values()) {
            if (mode.getId().equalsIgnoreCase(normalized) || mode.name().equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        return null;
    }

    private static boolean isKnownAddonType(String value) {
        String normalized = normalize(value);
        for (AddonType type : AddonType.values()) {
            if (type.getId().equalsIgnoreCase(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String fallbackPackId(File file) {
        if (file == null || file.getName() == null) {
            return "";
        }
        String name = file.getName();
        String lowerName = name.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".yaml")) {
            return name.substring(0, name.length() - 5);
        }
        if (lowerName.endsWith(".yml")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    record ValidationResult(String packId, List<String> errors, List<String> warnings) {
        boolean valid() {
            return errors.isEmpty();
        }
    }
}
