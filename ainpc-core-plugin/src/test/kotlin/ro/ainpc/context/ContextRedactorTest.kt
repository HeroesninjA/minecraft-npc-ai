package ro.ainpc.context

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContextRedactorTest {

    @Test
    fun redactsApiKey() {
        val input = "Foloseste cheia sk-abc123def456ghi789jkl pentru OpenAI."
        val result = ContextRedactor.redact(input)
        assertFalse(result.contains("sk-abc123def456ghi789jkl"))
        assertTrue(result.contains("[REDACTED]"))
    }

    @Test
    fun redactsLongApiKey() {
        val input = "sk-" + "A".repeat(40)
        val result = ContextRedactor.redact(input)
        assertTrue(result.contains("[REDACTED]"))
    }

    @Test
    fun redactsUuid() {
        val input = "Player 550e8400-e29b-41d4-a716-446655440000 a facut o actiune."
        val result = ContextRedactor.redact(input)
        assertTrue(result.contains("[REDACTED]"))
        assertFalse(result.contains("550e8400-e29b-41d4-a716-446655440000"))
    }

    @Test
    fun redactsEmail() {
        val input = "Contact: admin@example.com"
        val result = ContextRedactor.redact(input)
        assertFalse(result.contains("admin@example.com"))
        assertTrue(result.contains("[REDACTED]"))
    }

    @Test
    fun redactsIpv4() {
        val input = "Server IP: 192.168.1.1"
        val result = ContextRedactor.redact(input)
        assertFalse(result.contains("192.168.1.1"))
        assertTrue(result.contains("[REDACTED]"))
    }

    @Test
    fun cleansTextWithoutSensitiveData() {
        val input = "NPC Ion merge la piata."
        val result = ContextRedactor.redact(input)
        assertEquals(input, result)
    }

    @Test
    fun redactsMultiplePatterns() {
        val input = "API: sk-test12345678901234567890, UUID: 550e8400-e29b-41d4-a716-446655440000, email: test@test.com, IP: 10.0.0.1"
        val result = ContextRedactor.redact(input)
        assertTrue(result.contains("[REDACTED]"))
        assertFalse(result.contains("550e8400-e29b-41d4-a716-446655440000"))
        assertFalse(result.contains("10.0.0.1"))
    }

    @Test
    fun containsApiKeyDetection() {
        assertTrue(ContextRedactor.containsApiKey("sk-abc123def456ghi789jkl"))
        assertFalse(ContextRedactor.containsApiKey("Text normal"))
    }

    @Test
    fun countRedactions() {
        val input = "sk-" + "A".repeat(25) + " si 550e8400-e29b-41d4-a716-446655440000 si admin@test.com"
        assertEquals(3, ContextRedactor.countRedactions(input))
    }

    @Test
    fun emptyTextNoChanges() {
        assertEquals("", ContextRedactor.redact(""))
    }
}
