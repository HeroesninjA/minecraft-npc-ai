package ro.ainpc.engine

import ro.ainpc.engine.FeaturePackLoader.ProgressionMechanicDefinition
import ro.ainpc.engine.ScenarioEngine.ScenarioTemplate

fun resolveProgressionMechanicDefinition(
    loader: FeaturePackLoader?,
    template: ScenarioTemplate?,
): ProgressionMechanicDefinition? {
    if (template == null || template.progressionMechanicId.isBlank() || loader == null) return null
    return loader.findProgressionMechanicDefinition(template.sourcePackId, template.progressionMechanicId)
}

fun resolveProgressionMechanicKey(
    loader: FeaturePackLoader?,
    template: ScenarioTemplate?,
): String {
    if (template == null || template.progressionMechanicId.isBlank()) return ""
    val mechanic = resolveProgressionMechanicDefinition(loader, template)
    if (mechanic != null) {
        return normalizeReference(mechanic.packId) + ":" + normalizeReference(mechanic.id)
    }
    return normalizeReference(template.sourcePackId) + ":" + normalizeReference(template.progressionMechanicId)
}

fun resolveProgressionMechanicDisplay(
    loader: FeaturePackLoader?,
    template: ScenarioTemplate?,
): String {
    if (template == null) return "mecanica de progres"
    if (!template.progressionLabel.isBlank()) return template.progressionLabel
    val mechanic = resolveProgressionMechanicDefinition(loader, template)
    if (mechanic != null && !mechanic.label.isBlank()) return mechanic.label
    return if (template.progressionMechanicId.isBlank()) "mecanica de progres" else template.progressionMechanicId
}

fun resolveProgressionPluralLabel(
    loader: FeaturePackLoader?,
    template: ScenarioTemplate?,
): String {
    if (template == null) return "progresii"
    if (!template.progressionPluralLabel.isBlank()) return template.progressionPluralLabel
    val mechanic = resolveProgressionMechanicDefinition(loader, template)
    if (mechanic != null && !mechanic.pluralLabel.isBlank()) return mechanic.pluralLabel
    return "progresii"
}

fun resolveProgressionSingularLabel(
    loader: FeaturePackLoader?,
    template: ScenarioTemplate?,
): String {
    if (template == null) return "progresie"
    if (!template.progressionSingularLabel.isBlank()) return template.progressionSingularLabel
    val mechanic = resolveProgressionMechanicDefinition(loader, template)
    if (mechanic != null && !mechanic.singularLabel.isBlank()) return mechanic.singularLabel
    return "progresie"
}

fun progressionKindMatches(
    loader: FeaturePackLoader?,
    template: ScenarioTemplate?,
    expectedKind: String,
): Boolean {
    return QuestTemplateSelector.matchesProgressionKind(
        template,
        expectedKind,
        resolveProgressionMechanicDisplay(loader, template),
    )
}

fun resolveProgressionMechanicSortKey(
    loader: FeaturePackLoader?,
    template: ScenarioTemplate?,
): String {
    val mechanicKey = resolveProgressionMechanicKey(loader, template)
    if (mechanicKey.isNotBlank()) return mechanicKey
    return if (template != null) normalizeReference(resolveProgressionMechanicDisplay(loader, template)) else ""
}
