package ro.ainpc.addons.medieval

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin
import ro.ainpc.api.AINPCPlatformApi
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator
import java.util.stream.Stream

class AINPCScenarioMedievalPlugin : JavaPlugin() {
    private var platform: AINPCPlatformApi? = null
    private var addon: MedievalScenarioAddon? = null
    private var addonRegistered = false

    override fun onEnable() {
        platform = resolvePlatform()
        if (platform == null) {
            logger.severe("AINPC core nu a expus serviciul API. Addonul se opreste.")
            server.pluginManager.disablePlugin(this)
            return
        }

        if (!platform!!.addonRegistry.isAddonEnabled(ADDON_ID)) {
            logger.info("Addonul medieval este dezactivat in AINPC config.yml.")
            try {
                removeManagedPack()
                platform!!.reloadContent()
            } catch (exception: IOException) {
                logger.warning("Nu s-a putut curata pack-ul medieval dezactivat: ${exception.message}")
            }
            return
        }

        val addonConfig: YamlConfiguration
        try {
            val addonConfigPath = syncAddonConfig()
            addonConfig = YamlConfiguration.loadConfiguration(addonConfigPath.toFile())
            if (addonConfig.getBoolean("debug.log_config_path", false)) {
                logger.info("Config addon medieval: ${addonConfigPath.toAbsolutePath()}")
            }
        } catch (exception: IOException) {
            logger.severe("Nu s-a putut pregati config-ul addonului medieval: ${exception.message}")
            server.pluginManager.disablePlugin(this)
            return
        }

        if (!addonConfig.getBoolean("addon.enabled", true)) {
            logger.info("Addonul medieval este dezactivat in config-ul sau.")
            try {
                removeManagedPack()
                platform!!.reloadContent()
            } catch (exception: IOException) {
                logger.warning("Nu s-a putul curata pack-ul medieval dezactivat: ${exception.message}")
            }
            return
        }

        addon = MedievalScenarioAddon(pluginMeta.version)
        platform!!.addonRegistry.registerAddon(addon)
        addonRegistered = platform!!.addonRegistry.getDescriptor(ADDON_ID) != null
        if (!addonRegistered) {
            logger.severe("Addonul medieval a fost respins de registrul AINPC.")
            server.pluginManager.disablePlugin(this)
            return
        }

        try {
            if (shouldInstallManagedPack(addonConfig)) {
                syncManagedPacks()
            } else {
                removeManagedPack()
                logger.info("Pack-ul medieval nu este instalat prin config-ul addonului.")
            }
            platform!!.reloadContent()
        } catch (exception: IOException) {
            logger.severe("Nu s-au putut instala resursele addonului medieval: ${exception.message}")
            unregisterAddon()
            server.pluginManager.disablePlugin(this)
            return
        }

        logger.info("Scenariul medieval este conectat la AINPC Core.")
    }

    override fun onDisable() {
        if (platform == null) {
            return
        }

        try {
            removeManagedPack()
            if (server.pluginManager.isPluginEnabled(CORE_PLUGIN_NAME)) {
                platform!!.reloadContent()
            }
        } catch (exception: IOException) {
            logger.warning("Nu s-a putut curata pack-ul medieval: ${exception.message}")
        }

        unregisterAddon()
    }

    private fun resolvePlatform(): AINPCPlatformApi? {
        val provider: RegisteredServiceProvider<AINPCPlatformApi>? =
            server.servicesManager.getRegistration(AINPCPlatformApi::class.java)
        return provider?.provider
    }

    private fun shouldInstallManagedPack(addonConfig: YamlConfiguration): Boolean {
        return addonConfig.getBoolean("content.install_pack", true)
            && addonConfig.getBoolean("content.playable_content", true)
    }

    @Throws(IOException::class)
    private fun syncAddonConfig(): Path {
        val addonDirectory = managedConfigDirectory
        Files.createDirectories(addonDirectory)

        val template = addonDirectory.resolve(CONFIG_TEMPLATE_FILE_NAME)
        copyResource(CONFIG_RESOURCE_PATH, template, StandardCopyOption.REPLACE_EXISTING)

        val config = addonDirectory.resolve(CONFIG_FILE_NAME)
        if (!Files.exists(config)) {
            copyResource(CONFIG_RESOURCE_PATH, config)
            logger.info("Config addon medieval creat: ${config.toAbsolutePath()}")
        }
        return config
    }

    @Throws(IOException::class)
    private fun syncManagedPacks() {
        val currentPlatform = platform ?: return
        val packDirectory = currentPlatform.packDirectory
        Files.createDirectories(packDirectory)

        val addonDirectory = managedPackDirectory
        Files.createDirectories(addonDirectory)

        for (pack in MANAGED_PACKS) {
            val legacyPack = packDirectory.resolve(pack.fileName)
            val managedPack = addonDirectory.resolve(pack.fileName)
            if (Files.exists(legacyPack)) {
                Files.deleteIfExists(managedPack)
                logger.info("Folosesc pack-ul existent din folderul principal packs/: ${pack.fileName}")
                continue
            }
            copyResource(pack.resourcePath, managedPack, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    @Throws(IOException::class)
    private fun removeManagedPack() {
        val addonDirectory = managedPackDirectory
        if (!Files.exists(addonDirectory)) {
            return
        }

        Files.walk(addonDirectory).use { pathStream: Stream<Path> ->
            for (path in pathStream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path)
            }
        }
    }

    private val managedPackDirectory: Path
        get() = platform!!.packDirectory.resolve(MANAGED_PACK_FOLDER)

    private val managedConfigDirectory: Path
        get() = platform!!.getAddonConfigDirectory(ADDON_ID)

    @Throws(IOException::class)
    private fun copyResource(resourcePath: String, target: Path, vararg options: StandardCopyOption) {
        val inputStream: InputStream = getResource(resourcePath)
            ?: throw IOException("Resursa lipsa: $resourcePath")
        inputStream.use {
            Files.copy(it, target, *options)
        }
    }

    private fun unregisterAddon() {
        if (!addonRegistered || addon == null || platform == null) {
            return
        }

        platform!!.addonRegistry.unregisterAddon(addon!!.getDescriptor().id)
        addonRegistered = false
    }

    companion object {
        private const val CORE_PLUGIN_NAME = "AINPCPlugin"
        private const val ADDON_ID = "ainpc-scenario-medieval"
        private const val CONFIG_TEMPLATE_FILE_NAME = "config-template.yml"
        private const val CONFIG_FILE_NAME = "config.yml"
        private const val CONFIG_RESOURCE_PATH = CONFIG_TEMPLATE_FILE_NAME
        private const val MANAGED_PACK_FOLDER = "addons/$ADDON_ID"
        private val MANAGED_PACKS = listOf(
            ManagedPack("medieval.yml", "packs/medieval.yml"),
            ManagedPack("social.yml", "packs/social.yml"),
            ManagedPack("medieval_quest.yml", "packs/medieval_quest.yml"),
        )
    }

    private data class ManagedPack(val fileName: String, val resourcePath: String)
}
