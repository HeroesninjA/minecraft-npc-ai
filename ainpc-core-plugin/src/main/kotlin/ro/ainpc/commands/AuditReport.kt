package ro.ainpc.commands

import ro.ainpc.database.DatabaseSchemaInventory

enum class AuditSeverity(val chatPrefix: String) {
    INFO("&7"),
    WARN("&e"),
    ERROR("&c")
}

data class AuditFinding(
    val severity: AuditSeverity,
    val message: String,
)

class AuditReport(retainedMessageLimit: Int = Int.MAX_VALUE) {
    private val retainedLimit = retainedMessageLimit.coerceAtLeast(0)
    private val sectionItems = linkedMapOf<String, MutableList<AuditFinding>>()
    private val sectionItemCounts = linkedMapOf<String, Int>()
    private var currentSection = ""
    private var errorCountValue = 0
    private var warningCountValue = 0
    private var infoCountValue = 0
    private var databaseSchemaInventoryValue: DatabaseSchemaInventory? = null
    private var spawnHistoryAuditValue: SpawnHistoryAuditSummary? = null

    val errors: MutableList<String> = ArrayList()

    val warnings: MutableList<String> = ArrayList()

    val infos: MutableList<String> = ArrayList()

    fun addSection(name: String) {
        currentSection = name.ifBlank { "General" }
        sectionItems.getOrPut(currentSection) { mutableListOf() }
        sectionItemCounts.putIfAbsent(currentSection, 0)
    }

    fun addNote(note: String) {
        info(note)
    }

    fun addWarning(warning: String) {
        warn(warning)
    }

    fun addError(error: String) {
        error(error)
    }

    fun error(message: String) {
        errorCountValue++
        retain(errors, message)
        retainSection(AuditSeverity.ERROR, message)
    }

    fun warn(message: String) {
        warningCountValue++
        retain(warnings, message)
        retainSection(AuditSeverity.WARN, message)
    }

    fun info(message: String) {
        infoCountValue++
        retain(infos, message)
        retainSection(AuditSeverity.INFO, message)
    }

    fun errorCount(): Int = errorCountValue

    fun warningCount(): Int = warningCountValue

    fun infoCount(): Int = infoCountValue

    fun sectionItemCount(section: String): Int = sectionItemCounts[section] ?: 0

    fun sections(): Map<String, List<String>> = sectionItems.mapValues { entry ->
        entry.value.map { finding -> finding.severity.chatPrefix + finding.message }
    }

    fun findingSections(): Map<String, List<AuditFinding>> =
        sectionItems.mapValues { entry -> entry.value.toList() }

    fun retainedMessageLimit(): Int = retainedLimit

    fun recordDatabaseSchemaInventory(inventory: DatabaseSchemaInventory) {
        databaseSchemaInventoryValue = inventory
    }

    fun databaseSchemaInventory(): DatabaseSchemaInventory? = databaseSchemaInventoryValue

    fun recordSpawnHistoryAudit(summary: SpawnHistoryAuditSummary) {
        spawnHistoryAuditValue = summary
    }

    fun spawnHistoryAudit(): SpawnHistoryAuditSummary? = spawnHistoryAuditValue

    private fun retain(messages: MutableList<String>, message: String) {
        if (messages.size < retainedLimit) {
            messages.add(message)
        }
    }

    private fun retainSection(severity: AuditSeverity, message: String) {
        if (currentSection.isBlank()) {
            addSection("General")
        }
        sectionItemCounts[currentSection] = sectionItemCounts.getValue(currentSection) + 1
        val items = sectionItems.getValue(currentSection)
        if (items.size < retainedLimit) {
            items.add(AuditFinding(severity, message))
        }
    }
}
