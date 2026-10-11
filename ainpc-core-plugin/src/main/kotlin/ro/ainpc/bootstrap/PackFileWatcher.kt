package ro.ainpc.bootstrap

import ro.ainpc.AINPCPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Packfilewatcher.
 */
class PackFileWatcher(private val plugin: AINPCPlugin) {
    private val fileTimestamps: MutableMap<String, Long> = ConcurrentHashMap()
    private var debounceStart = 0L
    private var hasChanges = false
    @Volatile private var enabled = false

    private val packDir: File
        get() = File(plugin.dataFolder, "packs")

    fun start() {
        enabled = plugin.config.getBoolean("feature_packs.hot_reload", false)
        if (!enabled) {
            plugin.debug("[PackWatcher] Urmarirea fisierelor este dezactivata (feature_packs.hot_reload=false)")
            return
        }
        if (!packDir.exists()) {
            plugin.debug("[PackWatcher] Directorul packs/ nu exista. Se va crea la prima scriere.")
            return
        }
        fileTimestamps.clear()
        fileTimestamps.putAll(snapshotTimestamps())
        hasChanges = false
        plugin.logger.info("[PackWatcher] Urmarirea fisierelor activata pentru: ${packDir.absolutePath}")
    }

    fun stop() {
        enabled = false
        fileTimestamps.clear()
        hasChanges = false
    }

    fun tick() {
        if (!enabled || !packDir.exists()) return

        val current = snapshotTimestamps()
        val now = System.currentTimeMillis()

        if (current != fileTimestamps) {
            val changedPath = (current.keys + fileTimestamps.keys).firstOrNull {
                current[it] != fileTimestamps[it]
            }
            plugin.logger.info("[PackWatcher] Fisier modificat: $changedPath")
            fileTimestamps.clear()
            fileTimestamps.putAll(current)
            hasChanges = true
            debounceStart = now
            return
        }

        if (hasChanges) {
            if (now - debounceStart >= 5000) {
                triggerReload()
                hasChanges = false
            }
        }
    }

    private fun snapshotTimestamps(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        if (!packDir.exists()) return result
        scanFiles(packDir, result)
        return result
    }

    private fun scanFiles(dir: File, result: MutableMap<String, Long>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanFiles(file, result)
            } else if (isPackFile(file)) {
                result[file.absolutePath] = file.lastModified()
            }
        }
    }

    private fun isPackFile(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".json")
    }

    private fun triggerReload() {
        try {
            plugin.logger.info("[PackWatcher] Fisier(e) modificate — reincarc pachetele...")
            plugin.server.scheduler.runTask(plugin, Runnable {
                if (!enabled) return@Runnable
                try {
                    plugin.reloadContent()
                    plugin.logger.info("[PackWatcher] Reincarcare completata.")
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "[PackWatcher] Eroare la reincarcarea pachetelor", e)
                }
            })
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "[PackWatcher] Eroare la reincarcarea pachetelor", e)
        }
    }
}
