package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActivityBuildWritePolicyTest {
    @Test
    fun requiresExplicitBooleanWritableState() {
        assertTrue(activityBuildWriteAllowed("""{"tools":{"readOnly":false}}"""))
        for (content in listOf(null, "", "{", "null", "[]", "{}", """{"tools":{}}""",
            """{"tools":{"readOnly":true}}""", """{"tools":{"readOnly":"false"}}""",
            """{"tools":{"readOnly":0}}""", """{"tools":{"readOnly":null}}""")) {
            assertFalse(activityBuildWriteAllowed(content), "Unexpected writable state: $content")
        }
    }
}
