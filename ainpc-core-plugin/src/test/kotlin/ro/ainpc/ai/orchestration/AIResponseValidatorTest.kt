package ro.ainpc.ai.orchestration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AIResponseValidatorTest {

    @Test
    fun acceptsValidResponse() {
        val result = AIResponseValidator.validate("Salut, ce pot face pentru tine?")
        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun rejectsNullResponse() {
        val result = AIResponseValidator.validate(null)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("gol") })
    }

    @Test
    fun rejectsBlankResponse() {
        val result = AIResponseValidator.validate("   ")
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("gol") })
    }

    @Test
    fun rejectsEmptyResponse() {
        val result = AIResponseValidator.validate("")
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("gol") })
    }

    @Test
    fun rejectsTooLongResponse() {
        val longText = "A".repeat(2500)
        val result = AIResponseValidator.validate(longText)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("2000") })
    }

    @Test
    fun warnsOnManyLines() {
        val multiLine = (1..150).joinToString("\n") { "Line $it" }
        val result = AIResponseValidator.validate(multiLine)
        assertTrue(result.valid, "Errors: ${result.errors}")
        assertTrue(result.warnings.any { it.contains("linii") })
    }

    @Test
    fun rejectsForbiddenTemplatePattern() {
        val result = AIResponseValidator.validate("Ceva cu {{template}} in text.")
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("interzis") })
    }

    @Test
    fun rejectsScriptTag() {
        val result = AIResponseValidator.validate("<script>alert('xss')</script>")
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("interzis") })
    }

    @Test
    fun sanitizeRemovesForbiddenPatterns() {
        val sanitized = AIResponseValidator.sanitize("Text {{template}} si <script>xss</script>.")
        assertFalse(sanitized.contains("{{"))
        assertFalse(sanitized.contains("<script>"))
        assertTrue(sanitized.contains("Text "))
    }

    @Test
    fun validatesWithUseCase() {
        val result = AIResponseValidator.validate("Buna ziua!", AIUseCase.DIALOGUE_REPLY)
        assertTrue(result.valid)
    }

    @Test
    fun lengthCheckWithinLimit() {
        assertEquals(null, AIResponseValidator.validateLength("Short text"))
    }

    @Test
    fun lengthCheckExceedsLimit() {
        val longText = "A".repeat(2001)
        val result = AIResponseValidator.validateLength(longText)
        assertTrue(result != null && result.contains("2000"))
    }

    @Test
    fun lineCountWithinLimit() {
        assertEquals(null, AIResponseValidator.validateLineCount("Line 1\nLine 2"))
    }

    @Test
    fun lineCountExceedsLimit() {
        val manyLines = (1..101).joinToString("\n") { "Line $it" }
        val result = AIResponseValidator.validateLineCount(manyLines)
        assertTrue(result != null && result.contains("101"))
    }

    @Test
    fun forbiddenPatternDetection() {
        val issues = AIResponseValidator.validateForbiddenPatterns("Text {{template}} si normal.")
        assertTrue(issues.any { it.contains("interzis") })
    }

    @Test
    fun cleanTextHasNoForbiddenIssues() {
        val issues = AIResponseValidator.validateForbiddenPatterns("Text normal si curat.")
        assertTrue(issues.isEmpty())
    }

    @Test
    fun validationResultFactoryMethods() {
        val valid = AIValidationResult.valid("ok")
        assertTrue(valid.valid)
        assertEquals("ok", valid.sanitizedText)

        val rejected = AIValidationResult.rejected(listOf("eroare"))
        assertFalse(rejected.valid)
        assertEquals(1, rejected.errors.size)

        val warned = AIValidationResult.withWarnings("text", listOf("atentie"))
        assertTrue(warned.valid)
        assertEquals(1, warned.warnings.size)
    }

    @Test
    fun rejectionReasonIsSetOnInvalidResponse() {
        val result = AIResponseValidator.validate("")
        assertFalse(result.valid)
        assertEquals("raspuns_gol", result.rejectionReason)
    }

    @Test
    fun rejectionReasonOnTruncatedResponse() {
        val result = AIResponseValidator.validate("Raspunsul continua...")
        assertFalse(result.valid)
        assertEquals("raspuns_trunchiat", result.rejectionReason)
    }

    @Test
    fun rejectionReasonOnForbiddenPattern() {
        val result = AIResponseValidator.validate("Text cu {{template}} periculos.")
        assertFalse(result.valid)
        assertEquals("pattern_interzis", result.rejectionReason)
    }

    @Test
    fun rejectedWithReasonFactory() {
        val result = AIValidationResult.rejectedWithReason(listOf("eroare"), "test_reason")
        assertFalse(result.valid)
        assertEquals("test_reason", result.rejectionReason)
    }

    @Test
    fun validatesFormatForIntentClassification() {
        val issues = AIResponseValidator.validateFormat("single_line", AIUseCase.INTENT_CLASSIFICATION)
        assertTrue(issues.isEmpty())
    }

    @Test
    fun validatesFormatForIntentClassificationRejectsMultiline() {
        val issues = AIResponseValidator.validateFormat("line1\nline2", AIUseCase.INTENT_CLASSIFICATION)
        assertTrue(issues.any { it.contains("linii multiple") })
    }

    @Test
    fun validatesFormatForDraftWithJson() {
        val jsonResponse = """{"type": "quest", "name": "test"}"""
        val issues = AIResponseValidator.validateFormat(jsonResponse, AIUseCase.QUEST_DRAFT)
        assertTrue(issues.isEmpty())
    }

    @Test
    fun validatesTruncatedResponse() {
        val result = AIResponseValidator.validateComplete("Raspuns terminat cu ...")
        assertTrue(result != null && result.contains("trunchiat"))
    }

    @Test
    fun completeResponsePassesTruncationCheck() {
        val result = AIResponseValidator.validateComplete("Raspuns complet si valid.")
        assertEquals(null, result)
    }

    @Test
    fun validatesUnexpectedTypeInDialogueReply() {
        val issues = AIResponseValidator.validateUnexpectedType(
            """{"type": "json", "content": "test"}""", AIUseCase.DIALOGUE_REPLY
        )
        assertTrue(issues.any { it.contains("Tip neasteptat") })
    }

    @Test
    fun validatesConnectionStatusSummary() {
        val summary = "connected"
        val degraded = "degraded"
        val failed = "failed"
        assertNotEquals(summary, failed)
        assertNotEquals(degraded, failed)
    }

    @Test
    fun intentClassificationUseCaseExists() {
        assertEquals(AIUseCase.INTENT_CLASSIFICATION, AIUseCase.valueOf("INTENT_CLASSIFICATION"))
    }

    @Test
    fun buildPlanDraftUseCaseExists() {
        assertEquals(AIUseCase.BUILD_PLAN_DRAFT, AIUseCase.valueOf("BUILD_PLAN_DRAFT"))
    }

    @Test
    fun policyForIntentClassification() {
        val policy = AIOrchestrationPolicy.forUseCase(AIUseCase.INTENT_CLASSIFICATION)
        assertEquals(AIOutputType.INTENT, policy.outputType())
        assertFalse(policy.runtimeExecutable())
        assertTrue(policy.validationRequired())
        assertTrue(policy.fallbackRequired())
    }

    @Test
    fun policyForBuildPlanDraft() {
        val policy = AIOrchestrationPolicy.forUseCase(AIUseCase.BUILD_PLAN_DRAFT)
        assertEquals(AIOutputType.DRAFT, policy.outputType())
        assertFalse(policy.runtimeExecutable())
    }
}
