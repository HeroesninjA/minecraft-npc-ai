package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.util.UUID

class ScenarioSessionSnapshotStoreTest {
    @Test
    fun `saves updates and deletes snapshots by scenario id`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    "CREATE TABLE scenario_sessions (scenario_id TEXT PRIMARY KEY, template_id TEXT NOT NULL, " +
                        "snapshot_version INTEGER NOT NULL, snapshot_data TEXT NOT NULL, updated_at INTEGER NOT NULL)"
                )
            }
            val store = ScenarioSessionSnapshotStore()
            val initial = snapshot()

            assertEquals(0L, store.count(connection))
            store.save(connection, initial)
            assertEquals(1L, store.count(connection))
            assertEquals(initial, store.find(connection, initial.scenarioId))

            val updated = initial.copy(phase = "FINISH")
            store.save(connection, updated)
            assertEquals(1L, store.count(connection))
            assertEquals(updated, store.find(connection, initial.scenarioId))

            store.delete(connection, initial.scenarioId)
            assertEquals(0L, store.count(connection))
            assertNull(store.find(connection, initial.scenarioId))
        }
    }

    @Test
    fun `rejects a row whose metadata differs from its snapshot`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    "CREATE TABLE scenario_sessions (scenario_id TEXT PRIMARY KEY, template_id TEXT NOT NULL, " +
                        "snapshot_version INTEGER NOT NULL, snapshot_data TEXT NOT NULL, updated_at INTEGER NOT NULL)"
                )
            }
            val store = ScenarioSessionSnapshotStore()
            val snapshot = snapshot()
            store.save(connection, snapshot)
            connection.prepareStatement("UPDATE scenario_sessions SET template_id = ? WHERE scenario_id = ?").use { statement ->
                statement.setString(1, "another-template")
                statement.setString(2, snapshot.scenarioId.toString())
                statement.executeUpdate()
            }

            assertNull(store.find(connection, snapshot.scenarioId))
            assertEquals(1L, store.count(connection))
            assertEquals("template-ul randului difera de snapshot", store.loadPage(connection, 10, 0).single().issue)
        }
    }

    @Test
    fun `pages stored snapshots without hiding invalid payloads`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    "CREATE TABLE scenario_sessions (scenario_id TEXT PRIMARY KEY, template_id TEXT NOT NULL, " +
                        "snapshot_version INTEGER NOT NULL, snapshot_data TEXT NOT NULL, updated_at INTEGER NOT NULL)"
                )
            }
            val store = ScenarioSessionSnapshotStore()
            val valid = snapshot()
            store.save(connection, valid)
            connection.prepareStatement(
                "INSERT INTO scenario_sessions (scenario_id, template_id, snapshot_version, snapshot_data, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?)"
            ).use { statement ->
                statement.setString(1, UUID.randomUUID().toString())
                statement.setString(2, "pack:scenario")
                statement.setInt(3, 1)
                statement.setString(4, "not-json")
                statement.setLong(5, System.currentTimeMillis() + 1_000)
                statement.executeUpdate()
            }

            assertEquals(2L, store.count(connection))
            assertEquals(2L, store.countByTemplate(connection, "PACK:SCENARIO"))
            assertEquals("payload invalid sau versiune nesuportata", store.loadPage(connection, 1, 0).single().issue)
            assertEquals(valid, store.loadPage(connection, 1, 1).single().snapshot)
        }
    }

    private fun snapshot(): ScenarioSessionSnapshot = ScenarioSessionSnapshot(
        ScenarioSessionSnapshotCodec.VERSION,
        UUID.randomUUID(),
        "pack:scenario",
        "START",
        100L,
        ScenarioSessionSnapshot.Anchor("world", 1.0, 64.0, 2.0, 0f, 0f),
        emptyMap(),
        emptyMap(),
        emptyMap(),
    )
}
