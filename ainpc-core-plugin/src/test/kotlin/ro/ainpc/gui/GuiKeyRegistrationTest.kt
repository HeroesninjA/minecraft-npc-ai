package ro.ainpc.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GuiKeyRegistrationTest {

    @Test
    fun allKeysResolveFromId() {
        for (key in GuiKey.values()) {
            when (key) {
                GuiKey.MAIN -> assertEquals(GuiKey.MAIN, GuiKey.fromId("hub")!!)
                GuiKey.QUEST -> assertEquals(GuiKey.QUEST, GuiKey.fromId("quest")!!)
                GuiKey.STORY -> assertEquals(GuiKey.STORY, GuiKey.fromId("story")!!)
                GuiKey.WORLD -> assertEquals(GuiKey.WORLD, GuiKey.fromId("world")!!)
                GuiKey.ADMIN_MAPPING -> assertEquals(GuiKey.ADMIN_MAPPING, GuiKey.fromId("admin_mapping")!!)
                GuiKey.ADMIN_QUEST -> assertEquals(GuiKey.ADMIN_QUEST, GuiKey.fromId("admin_quest")!!)
                GuiKey.QUEST_MAP -> assertEquals(GuiKey.QUEST_MAP, GuiKey.fromId("quest_map")!!)
                GuiKey.CONFIRM -> assertEquals(GuiKey.CONFIRM, GuiKey.fromId("confirm")!!)
                else -> {} // Other keys use default id
            }
        }
    }

    @Test
    fun fromIdReturnsMainForNull() {
        assertEquals(GuiKey.MAIN, GuiKey.fromId(null)!!)
    }

    @Test
    fun fromIdReturnsMainForBlank() {
        assertEquals(GuiKey.MAIN, GuiKey.fromId("")!!)
    }

    @Test
    fun fromIdReturnsNullForUnknown() {
        assertNull(GuiKey.fromId("nonexistent_key_xyz"))
    }

    @Test
    fun eachKeyHasUniqueId() {
        val ids = GuiKey.values().map { it.id() }
        assertEquals(ids.size, ids.distinct().size, "Fiecare GuiKey trebuie sa aiba un id unic")
    }

    @Test
    fun eachKeyHasDisplayName() {
        for (key in GuiKey.values()) {
            assertFalse(key.displayName().isBlank(), "GuiKey ${key.name} trebuie sa aiba displayName")
        }
    }

    @Test
    fun questMapAliases() {
        assertEquals(GuiKey.QUEST_MAP, GuiKey.fromId("questmap")!!)
        assertEquals(GuiKey.QUEST_MAP, GuiKey.fromId("quest_map")!!)
        assertEquals(GuiKey.QUEST_MAP, GuiKey.fromId("quest_mapping")!!)
    }
}
