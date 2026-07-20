package ro.ainpc.util

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import ro.ainpc.api.PermissionNode

class PermissionAudit(private val plugin: JavaPlugin) {

    fun runStartupAudit() {
        val declared = loadDeclaredPermissions()
        val undeclaredInEnum = PermissionNode.undeclaredNodes(declared)
        val enumNodes = PermissionNode.allNodes().toSet()
        val missingFromEnum = declared - enumNodes

        if (undeclaredInEnum.isEmpty() && missingFromEnum.isEmpty()) {
            plugin.logger.info("PermissionNode audit OK — toate ${PermissionNode.entries.size} nodurile sunt declarate in plugin.yml.")
            return
        }

        plugin.logger.warning("=== Audit noduri de permisiuni ===")
        if (undeclaredInEnum.isNotEmpty()) {
            plugin.logger.warning("  Noduri in enum dar NEdeclarate in plugin.yml:")
            undeclaredInEnum.forEach { node ->
                plugin.logger.warning("    - ${node.node} (${node.level}): ${node.description}")
            }
        }
        if (missingFromEnum.isNotEmpty()) {
            plugin.logger.warning("  Noduri declarate in plugin.yml dar LIPSESC din PermissionNode enum:")
            missingFromEnum.sorted().forEach { node ->
                plugin.logger.warning("    - $node")
            }
        }
    }

    fun report(): String {
        val declared = loadDeclaredPermissions()
        val enumNodes = PermissionNode.allNodes().toSet()
        val inEnum = declared.intersect(enumNodes).sorted()
        val onlyInEnum = (enumNodes - declared).sorted()
        val onlyInYml = (declared - enumNodes).sorted()

        val sb = StringBuilder()
        sb.appendLine("=== PermissionNode Audit ===")
        sb.appendLine("Noduri in plugin.yml: ${declared.size}")
        sb.appendLine("Noduri in PermissionNode enum: ${enumNodes.size}")
        sb.appendLine("Match: ${inEnum.size}")
        sb.appendLine("Doar in enum: ${onlyInEnum.size}")
        if (onlyInEnum.isNotEmpty()) onlyInEnum.forEach { sb.appendLine("  + $it") }
        sb.appendLine("Doar in plugin.yml: ${onlyInYml.size}")
        if (onlyInYml.isNotEmpty()) onlyInYml.forEach { sb.appendLine("  - $it") }
        sb.appendLine("---")
        PermissionNode.entries.groupBy { it.level }.forEach { (level, nodes) ->
            sb.appendLine("${level.name}: ${nodes.size} noduri")
            nodes.forEach { sb.appendLine("  ${it.node}: ${it.description}") }
        }
        return sb.toString()
    }

    private fun loadDeclaredPermissions(): Set<String> {
        val resourceStream = plugin.javaClass.classLoader.getResourceAsStream("plugin.yml") ?: return emptySet()
        return try {
            val config = YamlConfiguration.loadConfiguration(resourceStream.reader())
            val permissionsSection = config.getConfigurationSection("permissions") ?: return emptySet()
            permissionsSection.getKeys(false).toSet()
        } finally {
            resourceStream.close()
        }
    }

    companion object {
        @JvmStatic
        fun auditRegisteredCommands(plugin: JavaPlugin) {
            val cmd = plugin.getCommand("ainpc") ?: return
            val perm = cmd.permission
            if (perm != null && perm.startsWith("ainpc.") && perm !in PermissionNode.allNodes().toSet()) {
                plugin.logger.warning("[PermAudit] Comanda 'ainpc' are permisiunea '$perm' nedeclarata in PermissionNode.")
            }
        }
    }
}
