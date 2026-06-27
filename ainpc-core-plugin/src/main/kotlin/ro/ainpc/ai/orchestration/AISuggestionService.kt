package ro.ainpc.ai.orchestration

import ro.ainpc.AINPCPlugin

class AISuggestionService(private val plugin: AINPCPlugin?) {

    companion object {
        const val MIN_CONFIDENCE_FOR_SAFE = 0.7
        const val MIN_CONFIDENCE_FOR_CAUTION = 0.4
        const val MAX_REWORK_ITERATIONS = 5
    }

    private val lifecycleMap = mutableMapOf<String, AISuggestionLifecycle>()

    fun buildExplanationBundle(
        suggestionId: String,
        changeSummary: String,
        reason: String,
        risks: List<String>,
        dependencies: List<String>,
        affectedNodes: List<String>
    ): AISuggestionExplanationBundle {
        return AISuggestionExplanationBundle(
            changeSummary = changeSummary,
            reason = reason,
            risks = risks,
            dependencies = dependencies,
            affectedNodes = affectedNodes
        )
    }

    fun determineSafetyLabel(request: AIOrchestrationRequest, result: AIOrchestrationResult): AISafetyLabel {
        val context = request.context()
        val confidenceStr = context["confidence"] ?: context["score"]
        val confidence = confidenceStr?.toDoubleOrNull() ?: 1.0

        if (result.status() == AIResultStatus.VALIDATION_FAILED) return AISafetyLabel.BLOCKED
        if (result.fallbackUsed()) return AISafetyLabel.CAUTION

        return when {
            confidence >= MIN_CONFIDENCE_FOR_SAFE -> AISafetyLabel.SAFE
            confidence >= MIN_CONFIDENCE_FOR_CAUTION -> AISafetyLabel.CAUTION
            else -> AISafetyLabel.RISKY
        }
    }

    fun detectMismatch(
        request: AIOrchestrationRequest,
        activeBranch: String?,
        activeRegion: String?,
        activeQuestChain: String?
    ): List<String> {
        val issues = mutableListOf<String>()
        val context = request.context()

        val targetBranch = context["branch"]
        if (targetBranch != null && activeBranch != null && targetBranch != activeBranch) {
            issues.add("Mismatch de branch: sugestia vizează '$targetBranch' dar branch-ul activ este '$activeBranch'")
        }

        val targetRegion = context["region"]
        if (targetRegion != null && activeRegion != null && targetRegion != activeRegion) {
            issues.add("Mismatch de regiune: sugestia vizează '$targetRegion' dar regiunea activă este '$activeRegion'")
        }

        val targetQuest = context["quest_chain"]
        if (targetQuest != null && activeQuestChain != null && targetQuest != activeQuestChain) {
            issues.add("Mismatch de quest chain: sugestia vizează '$targetQuest' dar lanțul activ este '$activeQuestChain'")
        }

        return issues
    }

    fun detectMergeConflict(
        suggestionId: String,
        targetBranch: String,
        existingSuggestions: Map<String, String>
    ): List<String> {
        val conflicts = mutableListOf<String>()
        for ((otherId, otherBranch) in existingSuggestions) {
            if (otherId != suggestionId && otherBranch == targetBranch) {
                conflicts.add("Conflict între sugestia $suggestionId și $otherId pe branch-ul $targetBranch")
            }
        }
        return conflicts
    }

    fun buildDependencyGraph(
        nodeId: String,
        nodeType: String,
        dependsOn: List<String>,
        blockedBy: List<String>
    ): AISuggestionDependency {
        return AISuggestionDependency(
            nodeId = nodeId,
            nodeType = nodeType,
            dependsOn = dependsOn,
            blockedBy = blockedBy
        )
    }

    fun recordAuditEntry(
        suggestionId: String,
        reviewer: String,
        action: String,
        reason: String,
        changesRequested: List<String>
    ): AISuggestionAuditEntry {
        return AISuggestionAuditEntry(
            reviewer = reviewer,
            action = action,
            reason = reason,
            changesRequested = changesRequested
        )
    }

    fun rejectWithTaxonomy(
        result: AIOrchestrationResult,
        useCase: AIUseCase
    ): Pair<String, String> {
        val message = result.message().lowercase()
        val errorCode = result.errorCode().lowercase()

        return when {
            errorCode.contains("lore") || message.contains("lore") -> "lore_conflict" to "Sugestia contrazice lore-ul existent"
            errorCode.contains("route") || message.contains("route") -> "route_invalid" to "Ruta sugerată nu este validă"
            errorCode.contains("spoiler") || message.contains("spoiler") -> "spoiler" to "Sugestia conține spoilere"
            errorCode.contains("permission") || message.contains("permisiune") -> "permission_mismatch" to "Nu ai permisiunea pentru această sugestie"
            errorCode.contains("quality") || message.contains("calitate") || message.contains("calit") -> "quality_below_threshold" to "Calitatea sugestiei este sub prag"
            errorCode.contains("duplicate") || message.contains("duplicat") -> "duplicate_content" to "Conținutul există deja"
            errorCode.contains("scope") || message.contains("scope") -> "scope_violation" to "Sugestia depășește scopul declarat"
            else -> "generic_rejection" to "Sugestia nu îndeplinește criteriile de acceptare"
        }
    }

    fun trackRework(
        suggestionId: String,
        iteration: Int,
        reworkHistory: MutableList<Pair<String, String>>
    ): String {
        if (iteration > MAX_REWORK_ITERATIONS) {
            return "Sugestia $suggestionId a depășit numărul maxim de iterații ($MAX_REWORK_ITERATIONS)"
        }
        val lastReason = reworkHistory.lastOrNull()?.second ?: "necunoscut"
        return "Rework $iteration/$MAX_REWORK_ITERATIONS pentru sugestia $suggestionId: motivul anterior '$lastReason'"
    }

    fun checkPublishGate(
        result: AIOrchestrationResult,
        safetyLabel: AISafetyLabel,
        audits: List<AISuggestionAuditEntry>,
        rollbackInfo: AISuggestionRollbackInfo
    ): List<String> {
        val gates = mutableListOf<String>()

        if (result.status() != AIResultStatus.SUCCESS) {
            gates.add("Gate eșuat: statusul nu este SUCCESS (${result.status()})")
        }
        if (safetyLabel == AISafetyLabel.BLOCKED || safetyLabel == AISafetyLabel.QUARANTINE) {
            gates.add("Gate eșuat: safety label-ul este $safetyLabel")
        }
        if (audits.isEmpty()) {
            gates.add("Gate eșuat: lipsește audit trail-ul de aprobare")
        }
        if (!rollbackInfo.canRollback) {
            gates.add("Gate eșuat: rollback-ul nu este pregătit")
        }

        if (gates.isEmpty()) {
            gates.add("Toate gate-urile sunt bifate. Publicarea este permisă.")
        }
        return gates
    }

    fun buildQuarantineReport(
        suggestionId: String,
        reason: String,
        owner: String,
        dependencies: List<AISuggestionDependency>,
        recommendedAction: String
    ): String {
        val depSummary = dependencies.joinToString("; ") { "${it.nodeId} (${it.nodeType})" }
        return """
            |Raport carantină - Sugestia $suggestionId
            |Motiv: $reason
            |Owner: $owner
            |Dependențe: $depSummary
            |Acțiune recomandată: $recommendedAction
        """.trimMargin()
    }

    fun exportProvenance(
        suggestionId: String,
        provenance: AISuggestionProvenance
    ): String {
        return """
            |Proveniență sugestie $suggestionId
            |Seed: ${provenance.seed}
            |Prompt version: ${provenance.promptVersion}
            |Model version: ${provenance.modelVersion}
            |Reviewer: ${provenance.reviewer}
            |Content version: ${provenance.contentVersion}
            |Generated at: ${provenance.generationTimestamp}
        """.trimMargin()
    }

    fun getLifecycle(suggestionId: String): AISuggestionLifecycle {
        return lifecycleMap[suggestionId] ?: AISuggestionLifecycle.GENERATED
    }

    fun transitionLifecycle(suggestionId: String, target: AISuggestionLifecycle): AISuggestionLifecycle {
        val current = getLifecycle(suggestionId)
        val next = current.transitionTo(target)
        lifecycleMap[suggestionId] = next
        return next
    }

    fun cleanupOrphanedLifecycles(activeSuggestionIds: Set<String>): Int {
        val before = lifecycleMap.size
        lifecycleMap.keys.removeAll { it !in activeSuggestionIds }
        return before - lifecycleMap.size
    }

    fun generateReleaseNotes(
        suggestionIds: List<String>,
        changeSummaries: Map<String, String>,
        risks: Map<String, List<String>>,
        rollbackPlan: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("## Release Notes - AI Content")
        sb.appendLine()
        for (id in suggestionIds) {
            val summary = changeSummaries[id] ?: "N/A"
            val riskList = risks[id] ?: emptyList()
            sb.appendLine("- $id: $summary")
            if (riskList.isNotEmpty()) {
                sb.appendLine("  Riscuri: ${riskList.joinToString(", ")}")
            }
        }
        sb.appendLine()
        sb.appendLine("### Rollback")
        sb.appendLine(rollbackPlan)
        return sb.toString()
    }

    fun moderateForMap(issues: List<String>): List<String> {
        val moderated = mutableListOf<String>()
        moderated.addAll(issues)
        moderated.addAll(
            listOf(
                "[Map] Verificare lore localizare",
                "[Map] Verificare rută accesibilă",
                "[Map] Verificare marker clutter"
            )
        )
        return moderated
    }

    fun moderateForQuest(issues: List<String>): List<String> {
        val moderated = mutableListOf<String>()
        moderated.addAll(issues)
        moderated.addAll(
            listOf(
                "[Quest] Verificare lanț quest",
                "[Quest] Verificare recompense",
                "[Quest] Verificare cleanup"
            )
        )
        return moderated
    }

    fun moderateForStory(issues: List<String>): List<String> {
        val moderated = mutableListOf<String>()
        moderated.addAll(issues)
        moderated.addAll(
            listOf(
                "[Story] Verificare canon",
                "[Story] Verificare spoiler",
                "[Story] Verificare branch coherence"
            )
        )
        return moderated
    }

    fun filterByVisibility(
        suggestionIds: List<String>,
        userRole: String,
        branchPermissions: Set<String>,
        suggestionsByBranch: Map<String, List<String>>
    ): List<String> {
        return suggestionIds.filter { id ->
            val branch = suggestionsByBranch.entries.firstOrNull { it.value.contains(id) }?.key
            branch == null || branch in branchPermissions
        }
    }

    fun replayReview(suggestionId: String, auditEntries: List<AISuggestionAuditEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("Replay review pentru sugestia $suggestionId")
        for ((index, entry) in auditEntries.withIndex()) {
            sb.appendLine("  Pas ${index + 1}: ${entry.action} de ${entry.reviewer}")
            sb.appendLine("    Motiv: ${entry.reason}")
            if (entry.changesRequested.isNotEmpty()) {
                sb.appendLine("    Schimbări cerute: ${entry.changesRequested.joinToString(", ")}")
            }
        }
        return sb.toString()
    }

    fun buildTrainingFeedback(
        suggestionId: String,
        auditEntries: List<AISuggestionAuditEntry>,
        finalVerdict: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Feedback training pentru sugestia $suggestionId")
        sb.appendLine("Verdict final: $finalVerdict")
        val rejectReasons = auditEntries
            .filter { it.action == "reject" || it.action == "changes_requested" }
            .map { it.reason }
        if (rejectReasons.isNotEmpty()) {
            sb.appendLine("Motive respingere: ${rejectReasons.joinToString("; ")}")
        }
        return sb.toString()
    }

    fun buildPolicyPack(): String {
        return """
            |Politici AI: Map, Quest, Story
            |- Map: lore, route, accessibility, marker clutter
            |- Quest: route, reward, cleanup, branch compatibility
            |- Story: canon, spoiler, pacing, branch coherence
            |- Generale: confidence, safety, provenance, rollback
        """.trimMargin()
    }

    fun detectPolicyDrift(
        documentedRules: Set<String>,
        appliedRules: Set<String>
    ): List<String> {
        val drift = mutableListOf<String>()
        val missing = documentedRules - appliedRules
        val extra = appliedRules - documentedRules
        if (missing.isNotEmpty()) {
            drift.add("Reguli documentate dar neaplicate: ${missing.joinToString(", ")}")
        }
        if (extra.isNotEmpty()) {
            drift.add("Reguli aplicate dar nedoCumentate: ${extra.joinToString(", ")}")
        }
        return drift
    }

    fun buildRolloutChecklist(
        validationPassed: Boolean,
        moderationPassed: Boolean,
        cacheWarmed: Boolean,
        rollbackReady: Boolean,
        docsUpdated: Boolean
    ): List<String> {
        val checklist = mutableListOf<String>()
        checklist.add(if (validationPassed) "[x]" else "[ ]" + " Validare trecută")
        checklist.add(if (moderationPassed) "[x]" else "[ ]" + " Moderare trecută")
        checklist.add(if (cacheWarmed) "[x]" else "[ ]" + " Cache pregătit")
        checklist.add(if (rollbackReady) "[x]" else "[ ]" + " Rollback gata")
        checklist.add(if (docsUpdated) "[x]" else "[ ]" + " Documentație actualizată")
        return checklist
    }
}
