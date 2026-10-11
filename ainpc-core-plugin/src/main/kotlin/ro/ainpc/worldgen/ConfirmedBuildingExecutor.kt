package ro.ainpc.worldgen

import java.util.UUID

class ConfirmedBuildingExecutor(
    private val executor: BlockExecutor,
    private val primaryThread: () -> Boolean,
    private val authorize: (String, List<BlockOperation>) -> Boolean,
    private val validate: (List<BlockOperation>) -> List<String>,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Pending(val actor: String, val operations: List<BlockOperation>, val createdAt: Long, val sourceUnchanged: () -> Boolean)
    private val pending = linkedMapOf<UUID, Pending>()

    @Synchronized
    fun prepare(actor: String, operations: List<BlockOperation>, sourceUnchanged: () -> Boolean = { true }): UUID {
        check(primaryThread()) { "Preparation requires the server primary thread" }
        require(actor.isNotBlank() && operations.isNotEmpty() && operations.size <= 10000) { "Invalid build request" }
        val now = clock()
        pending.entries.removeIf { now - it.value.createdAt >= 60000 }
        require(pending.size < 32) { "Too many pending builds" }
        val copy = operations.map { it.copy(blockData = it.blockData?.clone(), metadata = it.metadata.toMap()) }
        check(authorize(actor, copy)) { "Building authorization denied" }
        val errors = validate(copy)
        require(errors.isEmpty()) { errors.joinToString("; ") }
        check(sourceUnchanged()) { "Building source changed during preparation" }
        val token = UUID.randomUUID()
        pending[token] = Pending(actor, copy, now, sourceUnchanged)
        return token
    }

    @Synchronized
    fun confirm(actor: String, token: UUID): ExecutionResult {
        if (!primaryThread()) return ExecutionResult.failure(listOf("Confirmation requires the server primary thread"))
        val plan = pending[token] ?: return ExecutionResult.failure(listOf("Unknown or consumed build token"))
        if (plan.actor != actor) return ExecutionResult.failure(listOf("Build token belongs to another actor"))
        pending.remove(token)
        if (clock() - plan.createdAt !in 0 until 60000) return ExecutionResult.failure(listOf("Build token expired"))
        return try {
            if (!plan.sourceUnchanged()) return ExecutionResult.failure(listOf("Building template or mapping changed; prepare a new preview"))
            if (!authorize(actor, plan.operations)) return ExecutionResult.failure(listOf("Building authorization denied"))
            val errors = validate(plan.operations)
            if (errors.isNotEmpty()) return ExecutionResult.failure(errors)
            executor.execute(plan.operations)
        } catch (exception: Exception) {
            ExecutionResult.failure(listOf("Confirmed building failed: ${exception.message}"))
        }
    }
}
