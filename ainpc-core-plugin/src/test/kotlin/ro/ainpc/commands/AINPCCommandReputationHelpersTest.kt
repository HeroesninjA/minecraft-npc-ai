package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class AINPCCommandReputationHelpersTest {

    @Test
    fun `tryParseUuidOrNullReputation accepts valid UUID`() {
        val validUuid = "12345678-1234-1234-1234-123456789012"
        val parsed = tryParseUuidOrNullReputation(validUuid)
        assertEquals(validUuid, parsed)
        assertEquals(validUuid, UUID.fromString(parsed!!).toString())
    }

    @Test
    fun `tryParseUuidOrNullReputation rejects invalid UUID`() {
        assertEquals(null, tryParseUuidOrNullReputation("not-a-uuid"))
        assertEquals(null, tryParseUuidOrNullReputation(""))
        assertEquals(null, tryParseUuidOrNullReputation("12345"))
    }

    @Test
    fun `tryParseUuidOrNullReputation accepts player name as non-uuid`() {
        assertEquals(null, tryParseUuidOrNullReputation("Steve"))
        assertEquals(null, tryParseUuidOrNullReputation("Player123"))
    }

    @Test
    fun `tryParseUuidOrNullReputation rejects special characters in non-uuid strings`() {
        assertEquals(null, tryParseUuidOrNullReputation("abc-def"))
        assertEquals(null, tryParseUuidOrNullReputation("a b c"))
    }
}
