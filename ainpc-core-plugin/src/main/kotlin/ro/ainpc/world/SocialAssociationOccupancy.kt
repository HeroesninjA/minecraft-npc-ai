package ro.ainpc.world

internal object SocialAssociationOccupancy {
    fun capacity(metadata: Map<String, String>): Int? {
        val configured = metadata["max_social_associations"] ?: return null
        val capacity = configured.trim().toIntOrNull()
        require(capacity != null && capacity >= 0) {
            "max_social_associations invalid; este necesar un intreg nenegativ."
        }
        return capacity
    }

    fun associatedIds(metadata: Map<String, String>): Set<String> = mappedNpcIds(metadata, "social_npc_ids")

    fun hasAvailableSlot(capacity: Int?, assignedCount: Int): Boolean =
        capacity == null || assignedCount < capacity
}
