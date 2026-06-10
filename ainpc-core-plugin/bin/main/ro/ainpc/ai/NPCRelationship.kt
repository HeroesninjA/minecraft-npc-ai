package ro.ainpc.ai

class NPCRelationship(
    var affection: Double = 0.0,
    var trust: Double = 0.0,
    var respect: Double = 0.0,
    var familiarity: Double = 0.0,
    var interactionCount: Int = 0,
    var relationshipType: String? = null
)
