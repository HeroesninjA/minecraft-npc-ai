package ro.ainpc.engine

internal object ScenarioActorExpiration {
    enum class RecoveryStatus {
        PENDING,
        EXPIRED,
        INCONSISTENT,
    }

    fun deadline(now: Long, durationSeconds: Long?): Long? {
        if (durationSeconds == null || durationSeconds <= 0) return null
        return runCatching { Math.addExact(now, Math.multiplyExact(durationSeconds, 1_000L)) }
            .getOrNull()?.takeIf { it > now }
    }

    fun recoveryStatus(now: Long, startedAt: Long, durationSeconds: Long?, expiresAt: Long?): RecoveryStatus {
        if (durationSeconds == null || durationSeconds <= 0 || expiresAt == null || startedAt > now) {
            return RecoveryStatus.INCONSISTENT
        }
        val durationMillis = runCatching { Math.multiplyExact(durationSeconds, 1_000L) }
            .getOrNull() ?: return RecoveryStatus.INCONSISTENT
        val spawnedAt = runCatching { Math.subtractExact(expiresAt, durationMillis) }
            .getOrNull() ?: return RecoveryStatus.INCONSISTENT
        if (spawnedAt < startedAt || spawnedAt > now) return RecoveryStatus.INCONSISTENT
        return if (expiresAt <= now) RecoveryStatus.EXPIRED else RecoveryStatus.PENDING
    }

    fun recoveryDelayTicks(now: Long, startedAt: Long, durationSeconds: Long?, expiresAt: Long?): Long? {
        val deadline = expiresAt ?: return null
        if (recoveryStatus(now, startedAt, durationSeconds, expiresAt) != RecoveryStatus.PENDING) return null
        val remainingMillis = runCatching { Math.subtractExact(deadline, now) }.getOrNull() ?: return null
        return remainingMillis / 50L + if (remainingMillis % 50L == 0L) 0L else 1L
    }
}
