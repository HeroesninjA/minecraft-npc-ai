package ro.ainpc.addons.medieval;

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
    private static final String PACK_FILE_NAME = "medieval_quest.yml";
    private static final String PACK_RESOURCE_PATH = "packs/medieval_quest.yml";
    private static final String MANAGED_PACK_FOLDER = "addons/ainpc-scenario-medieval";

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

        addon = new MedievalScenarioAddon(getPluginMeta().getVersion());
        platform.getAddonRegistry().registerAddon(addon);
        addonRegistered = true;

        try {
            syncManagedPack();
            platform.reloadContent();
        } catch (IOException exception) {
            getLogger().severe("Nu s-a putut instala pack-ul medieval: " + exception.getMessage());
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
        try (InputStream inputStream = getResource(PACK_RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IOException("Resursa lipsa: " + PACK_RESOURCE_PATH);
            }
            Files.copy(inputStream, managedPack, StandardCopyOption.REPLACE_EXISTING);
        }
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

    private void unregisterAddon() {
        if (!addonRegistered || addon == null) {
            return;
        }

        platform.getAddonRegistry().unregisterAddon(addon.getDescriptor().getId());
        addonRegistered = false;
    }
}
