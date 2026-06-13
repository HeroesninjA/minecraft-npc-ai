package ro.ainpc.engine

import org.bukkit.entity.Entity
import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition
import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldPlace
import ro.ainpc.world.WorldRegion

fun matchesRegionObjective(objective: QuestEntryDefinition?, region: WorldRegion?): Boolean {
    if (objective == null || region == null) return false
    val reference = objective.itemId
    if (reference.isNullOrBlank()) return true
    val candidates = mutableListOf<String>()
    candidates.add(region.id)
    candidates.add(region.name)
    if (region.type != null) {
        candidates.add(region.type.id)
        candidates.add(region.type.name)
    }
    candidates.addAll(region.getTags())
    return matchesObjectiveReference(reference, *candidates.toTypedArray())
}

fun matchesRegionObjective(
    progress: PlayerQuestProgress?,
    objective: QuestEntryDefinition?,
    index: Int,
    region: WorldRegion?,
): Boolean {
    for (objectiveKey in objectiveKeyCandidates(objective, index)) {
        if (hasBoundAnchor(progress, objectiveKey)) {
            return matchesBoundAnchor(progress, objectiveKey, "region", region?.id ?: "")
        }
    }
    return matchesRegionObjective(objective, region)
}

fun matchesPlaceObjective(objective: QuestEntryDefinition?, place: WorldPlace?): Boolean {
    if (objective == null || place == null) return false
    val reference = objective.itemId
    if (reference.isNullOrBlank()) return true
    val candidates = mutableListOf<String>()
    candidates.add(place.id)
    candidates.add(place.displayName)
    candidates.add(place.regionId)
    if (place.placeType != null) {
        candidates.add(place.placeType.id)
        candidates.add(place.placeType.name)
    }
    candidates.addAll(place.getTags())
    candidates.addAll(place.getMetadata().keys)
    candidates.addAll(place.getMetadata().values)
    return matchesObjectiveReference(reference, *candidates.toTypedArray())
}

fun matchesPlaceObjective(
    progress: PlayerQuestProgress?,
    objective: QuestEntryDefinition?,
    index: Int,
    place: WorldPlace?,
): Boolean {
    for (objectiveKey in objectiveKeyCandidates(objective, index)) {
        if (hasBoundAnchor(progress, objectiveKey)) {
            return matchesBoundAnchor(progress, objectiveKey, "place", place?.id ?: "")
        }
    }
    return matchesPlaceObjective(objective, place)
}

fun matchesNodeObjective(objective: QuestEntryDefinition?, node: WorldNode?): Boolean {
    if (objective == null || node == null) return false
    val reference = objective.itemId
    if (reference.isNullOrBlank()) return true
    val candidates = mutableListOf<String>()
    candidates.add(node.id)
    candidates.add(node.regionId)
    candidates.add(node.placeId.orEmpty())
    if (node.type != null) {
        candidates.add(node.type.id)
        candidates.add(node.type.name)
    }
    candidates.addAll(node.getMetadata().keys)
    candidates.addAll(node.getMetadata().values)
    return matchesObjectiveReference(reference, *candidates.toTypedArray())
}

fun matchesNodeObjective(
    progress: PlayerQuestProgress?,
    objective: QuestEntryDefinition?,
    index: Int,
    node: WorldNode?,
): Boolean {
    for (objectiveKey in objectiveKeyCandidates(objective, index)) {
        if (hasBoundAnchor(progress, objectiveKey)) {
            return matchesBoundAnchor(progress, objectiveKey, "node", node?.id ?: "")
        }
    }
    return matchesNodeObjective(objective, node)
}

fun matchesMobObjective(objective: QuestEntryDefinition?, entity: Entity?): Boolean {
    if (objective == null || entity == null) return false
    val reference = objective.itemId
    if (reference.isNullOrBlank()) return true
    return matchesObjectiveReference(
        reference,
        entity.type.name,
        humanizeItemId(entity.type.name),
    )
}
