package ro.ainpc.commands

internal enum class PostSpawnBindingStatus {
    COMPLETE,
    PARTIAL,
    FAILED,
    NOT_APPLICABLE
}

internal data class PostSpawnBindingResult(
    val householdsAttempted: Int,
    val householdsCompleted: Int,
    val npcsAttempted: Int,
    val npcsCompleted: Int,
    val mappingWritesApplied: Int,
    val persistentBindingsSaved: Int,
    val failures: List<String>
) {
    init {
        require(householdsAttempted >= 0)
        require(householdsCompleted in 0..householdsAttempted)
        require(npcsAttempted >= 0)
        require(npcsCompleted in 0..npcsAttempted)
        require(mappingWritesApplied >= 0)
        require(persistentBindingsSaved in 0..npcsAttempted)
        require(npcsCompleted <= persistentBindingsSaved)
    }

    val status: PostSpawnBindingStatus
        get() = when {
            householdsAttempted == 0 && npcsAttempted == 0 && failures.isEmpty() ->
                PostSpawnBindingStatus.NOT_APPLICABLE
            householdsAttempted > 0 &&
                npcsAttempted > 0 &&
                householdsCompleted == householdsAttempted &&
                npcsCompleted == npcsAttempted &&
                failures.isEmpty() -> PostSpawnBindingStatus.COMPLETE
            householdsCompleted > 0 ||
                npcsCompleted > 0 ||
                mappingWritesApplied > 0 ||
                persistentBindingsSaved > 0 -> PostSpawnBindingStatus.PARTIAL
            else -> PostSpawnBindingStatus.FAILED
        }

    operator fun plus(other: PostSpawnBindingResult): PostSpawnBindingResult =
        PostSpawnBindingResult(
            householdsAttempted = householdsAttempted + other.householdsAttempted,
            householdsCompleted = householdsCompleted + other.householdsCompleted,
            npcsAttempted = npcsAttempted + other.npcsAttempted,
            npcsCompleted = npcsCompleted + other.npcsCompleted,
            mappingWritesApplied = mappingWritesApplied + other.mappingWritesApplied,
            persistentBindingsSaved = persistentBindingsSaved + other.persistentBindingsSaved,
            failures = failures + other.failures
        )

    companion object {
        val EMPTY = PostSpawnBindingResult(0, 0, 0, 0, 0, 0, emptyList())
    }
}

internal fun postSpawnBindingSummary(result: PostSpawnBindingResult): String {
    val (color, label) = when (result.status) {
        PostSpawnBindingStatus.COMPLETE -> "&a" to "complet"
        PostSpawnBindingStatus.PARTIAL -> "&e" to "partial"
        PostSpawnBindingStatus.FAILED -> "&c" to "esuat"
        PostSpawnBindingStatus.NOT_APPLICABLE -> "&e" to "fara tinte"
    }
    return "${color}Binding post-spawn $label: &f${result.householdsCompleted}/${result.householdsAttempted}" +
        " &7household-uri, &f${result.npcsCompleted}/${result.npcsAttempted} &7NPC-uri, " +
        "&f${result.mappingWritesApplied} &7scrieri mapping, " +
        "&f${result.persistentBindingsSaved} &7binding-uri persistente."
}

internal fun postSpawnBindingCompletionNotice(
    spawnOutcome: String,
    result: PostSpawnBindingResult
): String = when (result.status) {
    PostSpawnBindingStatus.COMPLETE ->
        "&7$spawnOutcome, iar binding-ul post-spawn este complet. Ruleaza &f/ainpc audit spawn &7pentru verificare."
    PostSpawnBindingStatus.PARTIAL ->
        "&eSucces partial: &f$spawnOutcome, dar binding-ul post-spawn este incomplet. " +
            "&eNPC-urile raman spawnate; nu s-a executat rollback. Ruleaza &f/ainpc audit spawn&e."
    PostSpawnBindingStatus.FAILED ->
        "&eSucces partial: &f$spawnOutcome, dar binding-ul post-spawn a esuat. " +
            "&eNPC-urile raman spawnate; nu s-a executat rollback. Ruleaza &f/ainpc audit spawn&e."
    PostSpawnBindingStatus.NOT_APPLICABLE ->
        "&eSucces partial: &f$spawnOutcome, dar binding-ul post-spawn nu a gasit NPC-uri eligibile. " +
            "&eNu s-a executat rollback. Ruleaza &f/ainpc audit spawn&e."
}
