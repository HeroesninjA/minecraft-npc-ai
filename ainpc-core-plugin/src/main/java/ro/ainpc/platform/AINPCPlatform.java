package ro.ainpc.platform;

import ro.ainpc.AINPCPlugin;
import ro.ainpc.addons.AddonDescriptor;
import ro.ainpc.addons.AddonRegistry;
import ro.ainpc.addons.AddonType;
import ro.ainpc.api.AINPCPlatformApi;
import ro.ainpc.api.AddonRegistryApi;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.world.StoryMode;
import ro.ainpc.world.WorldAdminService;
import ro.ainpc.world.WorldMode;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public class AINPCPlatform implements AINPCPlatformApi {

    private final AINPCPlugin plugin;
    private final AddonRegistry addonRegistry;
    private final WorldAdminService worldAdminService;
    private PlatformProfile profile;

    public AINPCPlatform(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.addonRegistry = new AddonRegistry(this);
        this.worldAdminService = new WorldAdminService(plugin);
        this.profile = PlatformProfile.fromConfig(plugin.getConfig());
    }

    public void initialize() {
        reloadFromConfig();
        registerCoreDescriptor();
    }

    public void reloadFromConfig() {
        this.profile = PlatformProfile.fromConfig(plugin.getConfig());
        addonRegistry.configure(
            plugin.getConfig().getBoolean("addons.enabled", true),
            plugin.getConfig().getBoolean("addons.strict_validation", true),
            plugin.getConfig().getStringList("addons.disabled"),
            plugin.getConfig().getStringList("addons.load_order")
        );
        worldAdminService.reloadFromConfig(plugin.getConfig(), profile);
    }

    private void registerCoreDescriptor() {
        addonRegistry.registerDescriptor(new AddonDescriptor(
            AddonDescriptor.ORIGIN_CORE,
            "ainpc-core",
            "AINPC Core",
            plugin.getPluginMeta().getVersion(),
            "Nucleul universal al platformei NPC AI",
            AddonType.CORE,
            false,
            EnumSet.allOf(RuntimeMode.class),
            List.of("ai-engine", "context-system", "dialog-system", "world-admin-api", "world-place-api", "addon-api"),
            List.of()
        ));
    }

    @Override
    public RuntimeMode getRuntimeMode() {
        return profile.getRuntimeMode();
    }

    @Override
    public WorldMode getWorldMode() {
        return profile.getWorldMode();
    }

    @Override
    public StoryMode getDefaultStoryMode() {
        return profile.getDefaultStoryMode();
    }

    @Override
    public AddonRegistryApi getAddonRegistry() {
        return addonRegistry;
    }

    @Override
    public WorldAdminApi getWorldAdmin() {
        return worldAdminService;
    }

    @Override
    public Path getDataDirectory() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public Path getPackDirectory() {
        return getDataDirectory().resolve("packs");
    }

    @Override
    public Path getAddonConfigDirectory(String addonId) {
        return getDataDirectory()
            .resolve(sanitizeRelativeDirectory(plugin.getConfig().getString("addons.config_directory", "addons")))
            .resolve(sanitizePathSegment(addonId, "unknown-addon"));
    }

    @Override
    public void reloadContent() {
        plugin.reloadContent();
    }

    public WorldAdminService getWorldAdminService() {
        return worldAdminService;
    }

    public PlatformProfile getProfile() {
        return profile;
    }

    public void shutdown() {
        addonRegistry.shutdown();
    }

    private String sanitizeRelativeDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return "addons";
        }
        String normalized = directory.trim().replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":") || normalized.contains("..")) {
            return "addons";
        }
        return normalized;
    }

    private String sanitizePathSegment(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        String sanitized = normalized.replaceAll("[^a-z0-9._-]", "-");
        return sanitized.isBlank() ? fallback : sanitized;
    }
}
