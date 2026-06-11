package ro.ainpc.managers

class NpcRepairCounters {
    @JvmField
    var duplicateDbRows: Int = 0

    @JvmField
    var deletedDbRows: Int = 0

    @JvmField
    var duplicateEntities: Int = 0

    @JvmField
    var removedEntities: Int = 0

    @JvmField
    var reassociatedEntities: Int = 0

    @JvmField
    var sourceKeyIndexIssues: Int = 0

    @JvmField
    var reindexedSourceKeys: Int = 0
}
