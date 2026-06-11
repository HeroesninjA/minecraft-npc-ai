package ro.ainpc.managers

class DuplicateRepairResult(
    private val applied: Boolean,
    private val duplicateDbRows: Int,
    private val deletedDbRows: Int,
    private val duplicateEntities: Int,
    private val removedEntities: Int,
    private val reassociatedEntities: Int,
    private val sourceKeyIndexIssues: Int,
    private val reindexedSourceKeys: Int,
    actions: List<String>?,
    warnings: List<String>?,
    errors: List<String>?,
) {
    private val actions: List<String> = actions?.toList() ?: emptyList()
    private val warnings: List<String> = warnings?.toList() ?: emptyList()
    private val errors: List<String> = errors?.toList() ?: emptyList()

    fun applied(): Boolean = applied

    fun duplicateDbRows(): Int = duplicateDbRows

    fun deletedDbRows(): Int = deletedDbRows

    fun duplicateEntities(): Int = duplicateEntities

    fun removedEntities(): Int = removedEntities

    fun reassociatedEntities(): Int = reassociatedEntities

    fun sourceKeyIndexIssues(): Int = sourceKeyIndexIssues

    fun reindexedSourceKeys(): Int = reindexedSourceKeys

    fun actions(): List<String> = actions

    fun warnings(): List<String> = warnings

    fun errors(): List<String> = errors
}
