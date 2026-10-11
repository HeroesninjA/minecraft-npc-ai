package ro.ainpc.ai

import ro.ainpc.database.DatabaseManager
import java.util.Locale
import java.util.UUID

enum class PlayerRole(val label: String) {
    CALATOR("calator"),
    COMERCIANT("comerciant"),
    MESTESUGAR("mestesugar"),
    EXPLORATOR("explorator");

    companion object {
        fun parse(value: String): PlayerRole? = entries.firstOrNull { it.label == value.lowercase(Locale.ROOT) }
    }
}

enum class PlayerStyle(val label: String) {
    DIRECT("direct"),
    PRIETENOS("prietenos"),
    FORMAL("formal");

    companion object {
        fun parse(value: String): PlayerStyle? = entries.firstOrNull { it.label == value.lowercase(Locale.ROOT) }
    }
}

enum class PlayerTrait(val label: String) {
    CURIOS("curios"),
    PRUDENT("prudent"),
    GENEROS("generos");

    companion object {
        fun parse(value: String): PlayerTrait? = entries.firstOrNull { it.label == value.lowercase(Locale.ROOT) }
    }
}

data class PlayerProfile(
    val role: PlayerRole? = null,
    val style: PlayerStyle? = null,
    val trait: PlayerTrait? = null,
)

class PlayerProfileRepository(private val database: DatabaseManager) {
    fun find(playerUuid: UUID): PlayerProfile? {
        database.prepareStatement(
            "SELECT role, style, trait FROM player_profiles WHERE player_uuid = ?"
        ).use { statement ->
            statement.setString(1, playerUuid.toString())
            statement.executeQuery().use { result ->
                if (!result.next()) return null
                return PlayerProfile(
                    result.getString("role")?.let(PlayerRole::parse),
                    result.getString("style")?.let(PlayerStyle::parse),
                    result.getString("trait")?.let(PlayerTrait::parse),
                )
            }
        }
    }

    fun save(playerUuid: UUID, profile: PlayerProfile) {
        database.prepareStatement(
            """
            INSERT INTO player_profiles (player_uuid, role, style, trait)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                role = excluded.role, style = excluded.style, trait = excluded.trait
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, playerUuid.toString())
            statement.setString(2, profile.role?.label)
            statement.setString(3, profile.style?.label)
            statement.setString(4, profile.trait?.label)
            statement.executeUpdate()
        }
    }

    fun delete(playerUuid: UUID) {
        database.prepareStatement("DELETE FROM player_profiles WHERE player_uuid = ?").use { statement ->
            statement.setString(1, playerUuid.toString())
            statement.executeUpdate()
        }
    }
}
