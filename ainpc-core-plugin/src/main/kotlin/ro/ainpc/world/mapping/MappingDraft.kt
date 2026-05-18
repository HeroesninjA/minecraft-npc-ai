package ro.ainpc.world.mapping

import java.util.UUID

class MappingDraft(
    private val playerId: UUID?,
    private val kind: MappingDraftKind,
    description: String?,
    localId: String?,
    qualifiedId: String?,
    displayName: String?,
    typeId: String?,
    regionId: String?,
    placeId: String?,
    worldName: String?,
    private val minX: Int,
    private val minY: Int,
    private val minZ: Int,
    private val maxX: Int,
    private val maxY: Int,
    private val maxZ: Int,
    private val x: Double,
    private val y: Double,
    private val z: Double,
    private val radius: Double,
    tags: List<String>?,
    metadata: Map<String, String>?,
    warnings: List<String>?,
    confirmationCommand: String?
) {
    private val description: String = description?.trim().orEmpty()
    private val localId: String = localId?.trim().orEmpty()
    private val qualifiedId: String = if (qualifiedId == null) this.localId else qualifiedId.trim()
    private val displayName: String = if (displayName.isNullOrBlank()) this.qualifiedId else displayName.trim()
    private val typeId: String = if (typeId.isNullOrBlank()) "custom" else typeId.trim()
    private val regionId: String = regionId?.trim().orEmpty()
    private val placeId: String = placeId?.trim().orEmpty()
    private val worldName: String = worldName?.trim().orEmpty()
    private val tags: List<String> = (tags ?: emptyList()).toList()
    private val metadata: Map<String, String> = (metadata ?: emptyMap()).toMap()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()
    private val confirmationCommand: String = confirmationCommand?.trim().orEmpty()

    fun playerId(): UUID? = playerId
    fun kind(): MappingDraftKind = kind
    fun description(): String = description
    fun localId(): String = localId
    fun qualifiedId(): String = qualifiedId
    fun displayName(): String = displayName
    fun typeId(): String = typeId
    fun regionId(): String = regionId
    fun placeId(): String = placeId
    fun worldName(): String = worldName
    fun minX(): Int = minX
    fun minY(): Int = minY
    fun minZ(): Int = minZ
    fun maxX(): Int = maxX
    fun maxY(): Int = maxY
    fun maxZ(): Int = maxZ
    fun x(): Double = x
    fun y(): Double = y
    fun z(): Double = z
    fun radius(): Double = radius
    fun tags(): List<String> = tags
    fun metadata(): Map<String, String> = metadata
    fun warnings(): List<String> = warnings
    fun confirmationCommand(): String = confirmationCommand

    fun isNode(): Boolean = kind == MappingDraftKind.NODE

    fun isBox(): Boolean = kind == MappingDraftKind.REGION || kind == MappingDraftKind.PLACE

    fun isNpcBind(): Boolean = kind == MappingDraftKind.NPC_BIND

    fun isQuestAnchor(): Boolean = kind == MappingDraftKind.QUEST_ANCHOR
}
