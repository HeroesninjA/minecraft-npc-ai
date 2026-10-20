package ro.ainpc.managers

import java.sql.PreparedStatement
import java.sql.SQLException

internal class UnloadedNpcEntityGuard(
    private val isLoaded: (Int) -> Boolean,
    private val prepareStatement: (String) -> PreparedStatement,
) {
    enum class Status(val preserveEntity: Boolean = false) {
        UNMANAGED,
        LOADED,
        PERSISTED(true),
        MISSING,
        UNVERIFIED(true),
    }

    fun inspect(databaseId: Int): Status {
        if (databaseId <= 0) return Status.UNMANAGED
        if (isLoaded(databaseId)) return Status.LOADED
        return try {
            prepareStatement("SELECT 1 FROM npcs WHERE id = ?").use { statement ->
                statement.setInt(1, databaseId)
                statement.executeQuery().use { rows ->
                    if (rows.next()) Status.PERSISTED else Status.MISSING
                }
            }
        } catch (_: SQLException) {
            Status.UNVERIFIED
        }
    }
}
