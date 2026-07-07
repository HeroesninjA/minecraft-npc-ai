package ro.ainpc.bootstrap

import ro.ainpc.AINPCPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class PackFileWatcher(private val plugin: AINPCPlugin) {
    private val fileTimestamps: MutableMap<String, Long> = ConcurrentHashMap()
    private var lastScan: MutableMap<String, Long> = ConcurrentHashMap()
    private var dirty = false
    private var debounceStart = 0L
    private var hasChanges = false
    private var enabled = false

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
        snapshotTimestamps()
        plugin.logger.info("[PackWatcher] Urmarirea fisierelor activata pentru: ${packDir.absolutePath}")
    }

    fun stop() {
        enabled = false
        fileTimestamps.clear()
        lastScan.clear()
    }

    fun tick() {
        if (!enabled || !packDir.exists()) return

        val current = snapshotTimestamps()
        val now = System.currentTimeMillis()

        if (hasChanges) {
            if (now - debounceStart >= 5000) {
                triggerReload()
                hasChanges = false
            }
            return
        }

        for ((path, timestamp) in current) {
            val lastTs = fileTimestamps[path]
            if (lastTs != null && lastTs != timestamp) {
                plugin.logger.info("[PackWatcher] Fisier modificat: $path")
                fileTimestamps[path] = timestamp
                hasChanges = true
                debounceStart = now
                return
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
            plugin.reloadContent()
            plugin.logger.info("[PackWatcher] Reincarcare completata.")
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "[PackWatcher] Eroare la reincarcarea pachetelor", e)
        }
    }
}
