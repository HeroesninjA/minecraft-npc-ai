package ro.ainpc.addons.medieval;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ro.ainpc.api.AINPCPlatformApi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

public class AINPCScenarioMedievalPlugin extends JavaPlugin {

    private static final String CORE_PLUGIN_NAME = "AINPCPlugin";
    private static final String ADDON_ID = "ainpc-scenario-medieval";
    private static final String PACK_FILE_NAME = "medieval_quest.yml";
    private static final String PACK_RESOURCE_PATH = "packs/medieval_quest.yml";
    private static final String CONFIG_TEMPLATE_FILE_NAME = "config-template.yml";
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String CONFIG_RESOURCE_PATH = CONFIG_TEMPLATE_FILE_NAME;
    private static final String MANAGED_PACK_FOLDER = "addons/" + ADDON_ID;

    private AINPCPlatformApi platform;
    private MedievalScenarioAddon addon;
    private boolean addonRegistered;

    @Override
    public void onEnable() {
        platform = resolvePlatform();
        if (platform == null) {
            getLogger().severe("AINPC core nu a expus serviciul API. Addonul se opreste.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!platform.getAddonRegistry().isAddonEnabled(ADDON_ID)) {
            getLogger().info("Addonul medieval este dezactivat in AINPC config.yml.");
            try {
                removeManagedPack();
                platform.reloadContent();
            } catch (IOException exception) {
                getLogger().warning("Nu s-a putut curata pack-ul medieval dezactivat: " + exception.getMessage());
            }
            return;
        }

        YamlConfiguration addonConfig;
        try {
            Path addonConfigPath = syncAddonConfig();
            addonConfig = YamlConfiguration.loadConfiguration(addonConfigPath.toFile());
            if (addonConfig.getBoolean("debug.log_config_path", false)) {
                getLogger().info("Config addon medieval: " + addonConfigPath.toAbsolutePath());
            }
        } catch (IOException exception) {
            getLogger().severe("Nu s-a putut pregati config-ul addonului medieval: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!addonConfig.getBoolean("addon.enabled", true)) {
            getLogger().info("Addonul medieval este dezactivat in config-ul sau.");
            try {
                removeManagedPack();
                platform.reloadContent();
            } catch (IOException exception) {
                getLogger().warning("Nu s-a putut curata pack-ul medieval dezactivat: " + exception.getMessage());
            }
            return;
        }

        addon = new MedievalScenarioAddon(getPluginMeta().getVersion());
        platform.getAddonRegistry().registerAddon(addon);
        addonRegistered = platform.getAddonRegistry().getDescriptor(ADDON_ID) != null;
        if (!addonRegistered) {
            getLogger().severe("Addonul medieval a fost respins de registrul AINPC.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            if (shouldInstallManagedPack(addonConfig)) {
                syncManagedPack();
            } else {
                removeManagedPack();
                getLogger().info("Pack-ul medieval nu este instalat prin config-ul addonului.");
            }
            platform.reloadContent();
        } catch (IOException exception) {
            getLogger().severe("Nu s-au putut instala resursele addonului medieval: " + exception.getMessage());
            unregisterAddon();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Scenariul medieval este conectat la AINPC Core.");
    }

    @Override
    public void onDisable() {
        if (platform == null) {
            return;
        }

        try {
            removeManagedPack();
            if (getServer().getPluginManager().isPluginEnabled(CORE_PLUGIN_NAME)) {
                platform.reloadContent();
            }
        } catch (IOException exception) {
            getLogger().warning("Nu s-a putut curata pack-ul medieval: " + exception.getMessage());
        }

        unregisterAddon();
    }

    private AINPCPlatformApi resolvePlatform() {
        RegisteredServiceProvider<AINPCPlatformApi> provider = getServer()
            .getServicesManager()
            .getRegistration(AINPCPlatformApi.class);
        return provider != null ? provider.getProvider() : null;
    }

    private boolean shouldInstallManagedPack(YamlConfiguration addonConfig) {
        return addonConfig.getBoolean("content.install_pack", true)
            && addonConfig.getBoolean("content.playable_content", true);
    }

    private Path syncAddonConfig() throws IOException {
        Path addonDirectory = getManagedConfigDirectory();
        Files.createDirectories(addonDirectory);

        Path template = addonDirectory.resolve(CONFIG_TEMPLATE_FILE_NAME);
        copyResource(CONFIG_RESOURCE_PATH, template, StandardCopyOption.REPLACE_EXISTING);

        Path config = addonDirectory.resolve(CONFIG_FILE_NAME);
        if (!Files.exists(config)) {
            copyResource(CONFIG_RESOURCE_PATH, config);
            getLogger().info("Config addon medieval creat: " + config.toAbsolutePath());
        }
        return config;
    }

    private void syncManagedPack() throws IOException {
        Path packDirectory = platform.getPackDirectory();
        Files.createDirectories(packDirectory);

        Path legacyPack = packDirectory.resolve(PACK_FILE_NAME);
        if (Files.exists(legacyPack)) {
            removeManagedPack();
            getLogger().info("Folosesc pack-ul medieval existent din folderul principal packs/.");
            return;
        }

        Path addonDirectory = getManagedPackDirectory();
        Files.createDirectories(addonDirectory);

        Path managedPack = addonDirectory.resolve(PACK_FILE_NAME);
        copyResource(PACK_RESOURCE_PATH, managedPack, StandardCopyOption.REPLACE_EXISTING);
    }

    private void removeManagedPack() throws IOException {
        Path addonDirectory = getManagedPackDirectory();
        if (!Files.exists(addonDirectory)) {
            return;
        }

        try (Stream<Path> pathStream = Files.walk(addonDirectory)) {
            for (Path path : pathStream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private Path getManagedPackDirectory() {
        return platform.getPackDirectory().resolve(MANAGED_PACK_FOLDER);
    }

    private Path getManagedConfigDirectory() {
        return platform.getAddonConfigDirectory(ADDON_ID);
    }

    private void copyResource(String resourcePath, Path target, StandardCopyOption... options) throws IOException {
        try (InputStream inputStream = getResource(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resursa lipsa: " + resourcePath);
            }
            Files.copy(inputStream, target, options);
        }
    }

    private void unregisterAddon() {
        if (!addonRegistered || addon == null) {
            return;
        }

        platform.getAddonRegistry().unregisterAddon(addon.getDescriptor().getId());
        addonRegistered = false;
    }
}
