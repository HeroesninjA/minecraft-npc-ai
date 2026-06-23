package ro.ainpc.listeners

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class NPCChatListenerTextInputRoutingTest {
    @Test
    fun chatListenerRoutesCreatorTextInputBeforeNpcDialogue() {
        val source = File("src/main/kotlin/ro/ainpc/listeners/NPCChatListener.kt").readText()

        assertTrue(source.contains("hasTextInputRequest"))
        assertTrue(source.contains("handleGuiTextInput"))
        assertTrue(source.contains("isValidGuiTextInput"))
        assertTrue(source.contains("sanitizeGuiTextInput"))
        assertTrue(source.contains("requeueTextInput"))
        assertTrue(source.contains("creator_quest_map_target"))
        assertTrue(source.contains("creator_quest_log_filter"))
        assertTrue(source.contains("quest_id"))
        assertTrue(source.contains("quest_obj_count"))
        assertTrue(source.contains("quest_reward_count"))
    }
}
