package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TryParseUuidOrNullReputationTest {

    @Test
    fun `valid UUID parses correctly`() {
        val validUuid = "12345678-1234-1234-1234-123456789012"
        assertEquals(validUuid, tryParseUuidOrNullReputation(validUuid))
    }

    @Test
    fun `non-UUID strings return null`() {
        assertEquals(null, tryParseUuidOrNullReputation("Steve"))
        assertEquals(null, tryParseUuidOrNullReputation(""))
        assertEquals(null, tryParseUuidOrNullReputation("not-a-uuid"))
    }

    @Test
    fun `short UUID-like strings return null`() {
        assertEquals(null, tryParseUuidOrNullReputation("12345"))
    }
}
