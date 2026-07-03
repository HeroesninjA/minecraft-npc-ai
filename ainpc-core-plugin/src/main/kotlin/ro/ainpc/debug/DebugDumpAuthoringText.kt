package ro.ainpc.debug

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.QuestAuthoringSnapshot
import ro.ainpc.progression.ProgressionGuiSnapshot
import ro.ainpc.story.StoryContextSnapshot
import java.time.LocalDateTime

object DebugDumpAuthoringText {
    @JvmStatic
    fun buildAuthoringText(
        plugin: AINPCPlugin,
        player: Player?,
        preferredQuestSelector: String? = null,
        preferredMechanicId: String? = null
    ): String {
        val storyContext = if (player != null) {
            plugin.storyContextService.buildForPlayer(player)
        } else {
            StoryContextSnapshot.empty()
        }
        val adminView = player?.hasPermission("ainpc.admin") == true
        val progressionSnapshot = if (player != null) {
            plugin.progressionService.getProgressionGuiSnapshot(player, "all", adminView)
        } else {
            ProgressionGuiSnapshot.empty()
        }
        val selectedEntry = progressionSnapshot.currentEntries().firstOrNull()
        val authoringSnapshot = plugin.authoringService.analyze(
            storyContext,
            plugin.progressionService.getDefinitions(),
            preferredQuestSelector ?: selectedEntry?.selector(),
            preferredMechanicId ?: selectedEntry?.mechanicId(),
            storyContext.worldContext().currentRegion()?.id(),
            storyContext.worldContext().currentPlace()?.id(),
            true,
            emptyList()
        )

        return buildText(authoringSnapshot, storyContext, progressionSnapshot)
    }

    @JvmStatic
    fun buildText(
        snapshot: QuestAuthoringSnapshot,
        storyContext: StoryContextSnapshot,
        progressionSnapshot: ProgressionGuiSnapshot
    ): String {
        val sb = StringBuilder()
        sb.append("AINPC Quest Authoring Dump\n")
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n")
        sb.append("Handled: ").append(snapshot.handled).append("\n")
        sb.append("Player: ").append(valueOrUnknown(snapshot.playerName)).append("\n")
        sb.append("Requested selector: ").append(valueOrUnknown(snapshot.requestedQuestSelector)).append("\n")
        sb.append("Requested mechanic: ").append(valueOrUnknown(snapshot.requestedMechanicId)).append("\n")
        sb.append("Decision: ").append(snapshot.decisionStatus()).append(" / ").append(snapshot.decisionReason()).append("\n")
        sb.append("Decision runtime executable: ").append(snapshot.decisionRuntimeExecutable()).append("\n")
        sb.append("Selected template: ").append(valueOrUnknown(snapshot.selectedTemplateId())).append("\n")
        sb.append("Selected progression: ").append(valueOrUnknown(snapshot.selectedProgressionId())).append("\n")
        sb.append("Selected mechanic: ").append(valueOrUnknown(snapshot.selectedMechanicId())).append("\n")
        sb.append("Selected definition: ").append(valueOrUnknown(snapshot.selectedDefinitionId())).append("\n")
        sb.append("Matched signals: ").append(snapshot.decisionMatchedSignals()).append("\n")
        sb.append("Candidate templates: ").append(snapshot.decisionCandidateTemplateIds()).append("\n")
        sb.append("Blocked reasons: ").append(snapshot.decisionBlockedReasons()).append("\n")
        sb.append("Seed region: ").append(valueOrUnknown(snapshot.seedRegionId())).append("\n")
        sb.append("Seed place: ").append(valueOrUnknown(snapshot.seedPlaceId())).append("\n")
        sb.append("Seed mechanic: ").append(valueOrUnknown(snapshot.seedMechanicId())).append("\n")
        sb.append("Seed kind: ").append(valueOrUnknown(snapshot.seedKind())).append("\n")
        sb.append("Seed story mode: ").append(valueOrUnknown(snapshot.seedStoryMode())).append("\n")
        sb.append("Seed theme: ").append(valueOrUnknown(snapshot.seedTheme())).append("\n")
        sb.append("Progression entries: ").append(progressionSnapshot.allEntries().size).append("\n")
        sb.append("Story signals: ").append(storyContext.storySignals().size).append("\n")
        sb.append("Warnings: ").append(snapshot.warnings.size).append("\n")
        appendQuestSemanticSection(sb, "QUEST_AUTHORING_SUMMARY", buildQuestAuthoringSummaryLines(snapshot))
        appendQuestSemanticSection(sb, "QUEST_LORE", buildQuestLoreLines(snapshot))
        appendQuestSemanticSection(sb, "QUEST_HISTORY", buildQuestHistoryLines(snapshot, storyContext))
        appendQuestSemanticSection(sb, "QUEST_SIGNALS", buildQuestSignalsLines(snapshot))
        appendSection(sb, "Summary lines", snapshot.summaryLines)
        appendSection(sb, "Story warnings", storyContext.warnings())
        appendSection(sb, "Snapshot warnings", snapshot.warnings)
        appendSection(sb, "Seed limits", snapshot.seed?.limits().orEmpty())
        return DebugDumpSecrets.redactText(sb.toString())
    }

    @JvmStatic
    fun buildSummaryText(
        snapshot: QuestAuthoringSnapshot,
        storyContext: StoryContextSnapshot
    ): String {
        val sb = StringBuilder()
        sb.append("AINPC Quest Authoring Summary\n")
        sb.append("Decision: ").append(snapshot.decisionStatus()).append(" / ").append(snapshot.decisionReason()).append("\n")
        sb.append("Selector: ").append(valueOrUnknown(snapshot.requestedQuestSelector)).append("\n")
        sb.append("Mechanic: ").append(valueOrUnknown(snapshot.requestedMechanicId)).append("\n")
        sb.append("Seed: ").append(valueOrUnknown(snapshot.seedRegionId()))
            .append("/").append(valueOrUnknown(snapshot.seedPlaceId()))
            .append("/").append(valueOrUnknown(snapshot.seedStoryMode())).append("\n")
        sb.append("Template: ").append(valueOrUnknown(snapshot.selectedTemplateId())).append("\n")
        sb.append("Definition: ").append(valueOrUnknown(snapshot.selectedDefinitionId())).append("\n")
        sb.append("Quest generation: ")
            .append(if (snapshot.decisionReason() == "structure_quest_generation_cooldown") "cooldown" else "active")
            .append("\n")
        sb.append("Matched signals: ").append(snapshot.decisionMatchedSignals().joinToString(", ")).append("\n")
        sb.append("Blocked reasons: ").append(snapshot.decisionBlockedReasons().joinToString(", ")).append("\n")
        sb.append("Story signals: ").append(storyContext.storySignals().joinToString(", ")).append("\n")
        sb.append("Warnings: ").append(snapshot.warnings.size).append("\n")
        return DebugDumpSecrets.redactText(sb.toString())
    }

    private fun appendSection(sb: StringBuilder, title: String, lines: List<String>) {
        sb.append("\n[").append(title).append("]\n")
        if (lines.isEmpty()) {
            sb.append("<none>\n")
            return
        }
        for (line in lines) {
            sb.append("- ").append(line).append("\n")
        }
    }

    private fun appendQuestSemanticSection(sb: StringBuilder, title: String, lines: List<String>) {
        sb.append("\n[").append(title).append("]\n")
        if (lines.isEmpty()) {
            sb.append("<none>\n")
            return
        }
        for (line in lines) {
            sb.append("- ").append(line).append("\n")
        }
    }

    private fun buildQuestLoreLines(snapshot: QuestAuthoringSnapshot): List<String> {
        val lines = ArrayList<String>()
        lines.add("selector=${valueOrUnknown(snapshot.requestedQuestSelector)}")
        lines.add("mechanic=${valueOrUnknown(snapshot.requestedMechanicId)}")
        lines.add("seed_region=${valueOrUnknown(snapshot.seedRegionId())}")
        lines.add("seed_place=${valueOrUnknown(snapshot.seedPlaceId())}")
        lines.add("seed_kind=${valueOrUnknown(snapshot.seedKind())}")
        lines.add("seed_theme=${valueOrUnknown(snapshot.seedTheme())}")
        lines.add("selected_template=${valueOrUnknown(snapshot.selectedTemplateId())}")
        lines.add("selected_progression=${valueOrUnknown(snapshot.selectedProgressionId())}")
        lines.add("selected_definition=${valueOrUnknown(snapshot.selectedDefinitionId())}")
        return lines
    }

    private fun buildQuestAuthoringSummaryLines(snapshot: QuestAuthoringSnapshot): List<String> {
        return listOf(
            "decision=${snapshot.decisionStatus()}:${snapshot.decisionReason()}",
            "selector=${valueOrUnknown(snapshot.requestedQuestSelector)}",
            "mechanic=${valueOrUnknown(snapshot.requestedMechanicId)}",
            "seed=${valueOrUnknown(snapshot.seedRegionId())}/${valueOrUnknown(snapshot.seedPlaceId())}/${valueOrUnknown(snapshot.seedStoryMode())}",
            "template=${valueOrUnknown(snapshot.selectedTemplateId())}",
            "definition=${valueOrUnknown(snapshot.selectedDefinitionId())}"
        )
    }

    private fun buildQuestHistoryLines(
        snapshot: QuestAuthoringSnapshot,
        storyContext: StoryContextSnapshot
    ): List<String> {
        val lines = ArrayList<String>()
        lines.add("decision=${snapshot.decisionStatus()}:${snapshot.decisionReason()}")
        lines.add("runtime_executable=${snapshot.decisionRuntimeExecutable()}")
        if (snapshot.decisionMatchedSignals().isNotEmpty()) {
            lines.add("matched_signals=" + snapshot.decisionMatchedSignals().joinToString(","))
        }
        if (snapshot.decisionCandidateTemplateIds().isNotEmpty()) {
            lines.add("candidate_templates=" + snapshot.decisionCandidateTemplateIds().joinToString(","))
        }
        if (snapshot.decisionBlockedReasons().isNotEmpty()) {
            lines.add("blocked_reasons=" + snapshot.decisionBlockedReasons().joinToString(","))
        }
        if (snapshot.decisionWarnings().isNotEmpty()) {
            lines.add("decision_warnings=" + snapshot.decisionWarnings().joinToString(","))
        }
        if (storyContext.warnings().isNotEmpty()) {
            lines.add("story_warnings=" + storyContext.warnings().joinToString(","))
        }
        return lines
    }

    private fun buildQuestSignalsLines(snapshot: QuestAuthoringSnapshot): List<String> {
        val lines = ArrayList<String>()
        lines.add("story_mode=${valueOrUnknown(snapshot.seedStoryMode())}")
        lines.add("story_signals=" + snapshot.seed?.storySignals().orEmpty().joinToString(","))
        lines.add("allowed_objectives=" + snapshot.seed?.allowedObjectiveTypes().orEmpty().joinToString(","))
        lines.add("allowed_rewards=" + snapshot.seed?.allowedRewardTypes().orEmpty().joinToString(","))
        lines.add("limits=" + snapshot.seed?.limits().orEmpty().joinToString(","))
        return lines
    }

    private fun valueOrUnknown(value: String): String = value.ifBlank { "unknown" }
}
