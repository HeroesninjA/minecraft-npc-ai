package ro.ainpc.database

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.sql.SQLException

class DatabaseDialectSqlTest {
    @Test
    fun parsesSupportedDatabaseTypes() {
        assertEquals(DatabaseDialect.SQLITE, DatabaseDialect.fromConfig(null))
        assertEquals(DatabaseDialect.SQLITE, DatabaseDialect.fromConfig("sqlite"))
        assertEquals(DatabaseDialect.MYSQL, DatabaseDialect.fromConfig("mysql"))
        assertEquals(DatabaseDialect.MYSQL, DatabaseDialect.fromConfig("mariadb"))
    }

    @Test
    fun rejectsUnknownDatabaseType() {
        assertThrows(SQLException::class.java) {
            DatabaseDialect.fromConfig("postgres")
        }
    }

    @Test
    fun rendersAutoIncrementPrimaryKeyByDialect() {
        assertEquals(
            "INTEGER PRIMARY KEY AUTOINCREMENT",
            DatabaseDialectSql.autoIncrementPrimaryKey(DatabaseDialect.SQLITE)
        )
        assertEquals(
            "INTEGER PRIMARY KEY AUTO_INCREMENT",
            DatabaseDialectSql.autoIncrementPrimaryKey(DatabaseDialect.MYSQL)
        )
    }

    @Test
    fun rendersTextColumnTypesByDialect() {
        assertEquals("TEXT", DatabaseDialectSql.shortText(DatabaseDialect.SQLITE))
        assertEquals("VARCHAR(191)", DatabaseDialectSql.shortText(DatabaseDialect.MYSQL))
        assertEquals("VARCHAR(64)", DatabaseDialectSql.shortText(DatabaseDialect.MYSQL, 64))
        assertEquals("TEXT", DatabaseDialectSql.longText(DatabaseDialect.SQLITE))
        assertEquals("LONGTEXT", DatabaseDialectSql.longText(DatabaseDialect.MYSQL))
    }

    @Test
    fun translatesLegacySqliteInsertOrDmlForMysqlOnly() {
        val insertIgnore = "INSERT OR IGNORE INTO npc_traits (npc_id, trait_id) VALUES (?, ?)"
        val insertReplace = "INSERT OR REPLACE INTO npc_emotions (npc_id) VALUES (?)"

        assertEquals(insertIgnore, DatabaseDialectSql.translateDml(insertIgnore, DatabaseDialect.SQLITE))
        assertEquals(
            "INSERT IGNORE INTO npc_traits (npc_id, trait_id) VALUES (?, ?)",
            DatabaseDialectSql.translateDml(insertIgnore, DatabaseDialect.MYSQL)
        )
        assertEquals(
            "REPLACE INTO npc_emotions (npc_id) VALUES (?)",
            DatabaseDialectSql.translateDml(insertReplace, DatabaseDialect.MYSQL)
        )
    }

    @Test
    fun translatesSimpleSqliteOnConflictUpsertForMysqlOnly() {
        val sqliteUpsert = """
            INSERT INTO npc_world_bindings (
                npc_id, npc_uuid, npc_name, updated_at
            ) VALUES (?, ?, ?, ?)
            ON CONFLICT(npc_id) DO UPDATE SET
                npc_uuid = excluded.npc_uuid,
                npc_name = excluded.npc_name,
                updated_at = excluded.updated_at
        """.trimIndent()

        assertEquals(sqliteUpsert, DatabaseDialectSql.translateDml(sqliteUpsert, DatabaseDialect.SQLITE))
        assertEquals(
            """
            INSERT INTO npc_world_bindings (
                npc_id, npc_uuid, npc_name, updated_at
            ) VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            npc_uuid = VALUES(npc_uuid),
                npc_name = VALUES(npc_name),
                updated_at = VALUES(updated_at)
            """.trimIndent(),
            DatabaseDialectSql.translateDml(sqliteUpsert, DatabaseDialect.MYSQL)
        )
    }

    @Test
    fun translatesSqliteDatetimeExpressionsForMysqlOnly() {
        val insertWithParameterizedExpiry = """
            INSERT INTO npc_memories (expires_at)
            VALUES (datetime('now', '+' || ? || ' days'))
        """.trimIndent()
        val insertWithFixedExpiry = """
            INSERT INTO npc_memories (expires_at)
            VALUES (datetime('now', '+90 days'))
        """.trimIndent()
        val deleteExpired = """
            DELETE FROM npc_memories
            WHERE expires_at IS NOT NULL AND expires_at < datetime('now')
        """.trimIndent()

        assertEquals(
            insertWithParameterizedExpiry,
            DatabaseDialectSql.translateDml(insertWithParameterizedExpiry, DatabaseDialect.SQLITE)
        )
        assertEquals(
            """
            INSERT INTO npc_memories (expires_at)
            VALUES (DATE_ADD(UTC_TIMESTAMP(), INTERVAL ? DAY))
            """.trimIndent(),
            DatabaseDialectSql.translateDml(insertWithParameterizedExpiry, DatabaseDialect.MYSQL)
        )
        assertEquals(
            """
            INSERT INTO npc_memories (expires_at)
            VALUES (DATE_ADD(UTC_TIMESTAMP(), INTERVAL 90 DAY))
            """.trimIndent(),
            DatabaseDialectSql.translateDml(insertWithFixedExpiry, DatabaseDialect.MYSQL)
        )
        assertEquals(
            """
            DELETE FROM npc_memories
            WHERE expires_at IS NOT NULL AND expires_at < UTC_TIMESTAMP()
            """.trimIndent(),
            DatabaseDialectSql.translateDml(deleteExpired, DatabaseDialect.MYSQL)
        )
    }

    @Test
    fun translatesSqliteLimitMaxForMysqlOnly() {
        val deleteExcess = """
            DELETE FROM npc_memories
            WHERE id IN (
                SELECT id FROM npc_memories
                WHERE npc_id = ? AND player_uuid = ?
                ORDER BY importance ASC, created_at ASC
                LIMIT MAX(0, (
                    SELECT COUNT(*) FROM npc_memories
                    WHERE npc_id = ? AND player_uuid = ?
                ) - ?)
            )
        """.trimIndent()

        assertEquals(deleteExcess, DatabaseDialectSql.translateDml(deleteExcess, DatabaseDialect.SQLITE))
        assertEquals(
            """
            DELETE FROM npc_memories
            WHERE id IN (
                SELECT id FROM npc_memories
                WHERE npc_id = ? AND player_uuid = ?
                ORDER BY importance ASC, created_at ASC
                LIMIT GREATEST(0, (SELECT COUNT(*) FROM npc_memories
                    WHERE npc_id = ? AND player_uuid = ?) - ?)
            )
            """.trimIndent(),
            DatabaseDialectSql.translateDml(deleteExcess, DatabaseDialect.MYSQL)
        )
    }

    @Test
    fun translatesSqliteScalarMinMaxForMysqlOnly() {
        val relationshipUpsert = """
            ON CONFLICT(npc_id, player_uuid) DO UPDATE SET
                affection = MIN(1.0, MAX(-1.0, affection + ?)),
                familiarity = MIN(1.0, familiarity + 0.01),
                max_residents = MAX(households.max_residents, excluded.max_residents)
        """.trimIndent()

        assertEquals(relationshipUpsert, DatabaseDialectSql.translateDml(relationshipUpsert, DatabaseDialect.SQLITE))
        assertEquals(
            """
            ON DUPLICATE KEY UPDATE
            affection = LEAST(1.0, GREATEST(-1.0, affection + ?)),
                familiarity = LEAST(1.0, familiarity + 0.01),
                max_residents = GREATEST(households.max_residents, VALUES(max_residents))
            """.trimIndent(),
            DatabaseDialectSql.translateDml(relationshipUpsert, DatabaseDialect.MYSQL)
        )
    }

    @Test
    fun removesCreateIndexIfNotExistsForMysqlOnly() {
        val sql = "CREATE INDEX IF NOT EXISTS idx_demo ON demo_table(name)"
        val uniqueSql = "CREATE UNIQUE INDEX IF NOT EXISTS idx_demo_unique ON demo_table(name)"

        assertEquals(sql, DatabaseDialectSql.translateSchema(sql, DatabaseDialect.SQLITE))
        assertEquals(
            "CREATE INDEX idx_demo ON demo_table(name)",
            DatabaseDialectSql.translateSchema(sql, DatabaseDialect.MYSQL)
        )
        assertEquals(
            "CREATE UNIQUE INDEX idx_demo_unique ON demo_table(name)",
            DatabaseDialectSql.translateSchema(uniqueSql, DatabaseDialect.MYSQL)
        )
    }
}
