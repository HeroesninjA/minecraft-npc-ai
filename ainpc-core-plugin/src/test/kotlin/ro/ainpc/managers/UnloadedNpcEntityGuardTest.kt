package ro.ainpc.managers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.DriverManager

class UnloadedNpcEntityGuardTest {
    private val connection = DriverManager.getConnection("jdbc:sqlite::memory:").also { database ->
        database.createStatement().use { statement ->
            statement.execute("CREATE TABLE npcs (id INTEGER PRIMARY KEY)")
            statement.execute("INSERT INTO npcs (id) VALUES (42)")
        }
    }
    private val loadedIds = mutableSetOf<Int>()
    private val guard = UnloadedNpcEntityGuard(loadedIds::contains, connection::prepareStatement)

    @AfterEach
    fun tearDown() {
        connection.close()
    }

    @Test
    fun `unloaded persisted rows stay protected until their actual deletion`() {
        assertEquals(UnloadedNpcEntityGuard.Status.PERSISTED, guard.inspect(42))
        assertTrue(guard.inspect(42).preserveEntity)
        assertEquals(UnloadedNpcEntityGuard.Status.MISSING, guard.inspect(43))

        connection.createStatement().use { it.executeUpdate("DELETE FROM npcs WHERE id = 42") }

        assertEquals(UnloadedNpcEntityGuard.Status.MISSING, guard.inspect(42))
        assertFalse(guard.inspect(42).preserveEntity)
    }

    @Test
    fun `database failures never authorize removal or adoption`() {
        connection.createStatement().use { it.executeUpdate("DROP TABLE npcs") }

        assertEquals(UnloadedNpcEntityGuard.Status.UNVERIFIED, guard.inspect(42))
        assertTrue(guard.inspect(42).preserveEntity)
    }

    @Test
    fun `loaded and unmarked entities do not need a database query`() {
        loadedIds.add(42)
        connection.close()

        assertEquals(UnloadedNpcEntityGuard.Status.LOADED, guard.inspect(42))
        assertEquals(UnloadedNpcEntityGuard.Status.UNMANAGED, guard.inspect(0))
        assertEquals(UnloadedNpcEntityGuard.Status.UNMANAGED, guard.inspect(-1))
        assertEquals(UnloadedNpcEntityGuard.Status.UNVERIFIED, guard.inspect(43))
    }
}
