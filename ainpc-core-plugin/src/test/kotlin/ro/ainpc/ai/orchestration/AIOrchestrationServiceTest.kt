package ro.ainpc.ai.orchestration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.LinkedHashMap

class AIOrchestrationServiceTest {
    @Test
    fun questDraftPolicyRequiresValidationAndCannotExecuteRuntime() {
        val policy = AIOrchestrationPolicy.forUseCase(AIUseCase.QUEST_DRAFT)

        assertEquals(AIOutputType.DRAFT, policy.outputType())
        assertTrue(policy.validationRequired())
        assertTrue(policy.fallbackRequired())
        assertFalse(policy.runtimeExecutable())
    }

    @Test
    fun disabledServiceReturnsDeterministicFallback() {
        val service = AIOrchestrationService(null)
        val request = AIOrchestrationRequest(
            AIUseCase.STORY_DRAFT,
            "npc-1",
            "Hero",
            "story",
            mapOf("region" to "spawn")
        )

        val result = service.orchestrate(request)

        assertEquals(AIResultStatus.DISABLED, result.status())
        assertEquals(AIOutputType.DRAFT, result.outputType())
        assertTrue(result.fallbackUsed())
        assertFalse(result.runtimeExecutable())
        assertEquals("ai_orchestration_disabled", result.errorCode())
    }

    @Test
    fun nullRequestFailsValidation() {
        val service = AIOrchestrationService(null)

        val result = service.orchestrate(null)

        assertEquals(AIResultStatus.VALIDATION_FAILED, result.status())
        assertEquals("invalid_request", result.errorCode())
        assertFalse(result.runtimeExecutable())
    }

    @Test
    fun resultStatusTransitions() {
        assertEquals(AIResultStatus.SUCCESS, AIResultStatus.SUCCESS)
        assertEquals(AIResultStatus.VALIDATION_FAILED, AIResultStatus.VALIDATION_FAILED)
        assertEquals(AIResultStatus.PROVIDER_FAILED, AIResultStatus.PROVIDER_FAILED)
        assertEquals(AIResultStatus.TIMEOUT, AIResultStatus.TIMEOUT)
        assertEquals(AIResultStatus.RATE_LIMITED, AIResultStatus.RATE_LIMITED)
        assertEquals(AIResultStatus.FALLBACK_USED, AIResultStatus.FALLBACK_USED)
        assertEquals(AIResultStatus.DISABLED, AIResultStatus.DISABLED)
    }

    @Test
    fun successResultIsValidAndExecutable() {
        val result = AIOrchestrationResult(
            AIUseCase.DIALOGUE_REPLY, AIResultStatus.SUCCESS, AIOutputType.MESSAGE,
            "Salut!", false, true, "", emptyList()
        )
        assertEquals(AIResultStatus.SUCCESS, result.status())
        assertFalse(result.fallbackUsed())
        assertTrue(result.runtimeExecutable())
        assertTrue(result.message().isNotBlank())
    }

    @Test
    fun fallbackResultIsNotExecutable() {
        val result = AIOrchestrationResult(
            AIUseCase.DIALOGUE_REPLY, AIResultStatus.FALLBACK_USED, AIOutputType.MESSAGE,
            "Fallback text", true, false, "ai_provider_not_available", listOf("AI unavailable.")
        )
        assertEquals(AIResultStatus.FALLBACK_USED, result.status())
        assertTrue(result.fallbackUsed())
        assertFalse(result.runtimeExecutable())
        assertEquals("ai_provider_not_available", result.errorCode())
    }

    @Test
    fun validationFailedResultHasErrors() {
        val result = AIOrchestrationResult(
            AIUseCase.QUEST_DRAFT, AIResultStatus.VALIDATION_FAILED, AIOutputType.MESSAGE,
            "Invalid request", true, false, "invalid_request", listOf("Request is null.")
        )
        assertEquals(AIResultStatus.VALIDATION_FAILED, result.status())
        assertFalse(result.runtimeExecutable())
        assertTrue(result.validationMessages().isNotEmpty())
    }

    @Test
    fun providerFailedAndTimeoutAndRateLimitedAreDistinct() {
        val providerFailed = AIResultStatus.PROVIDER_FAILED
        val timeout = AIResultStatus.TIMEOUT
        val rateLimited = AIResultStatus.RATE_LIMITED
        assertNotEquals(providerFailed, timeout)
        assertNotEquals(timeout, rateLimited)
        assertNotEquals(providerFailed, rateLimited)
    }

    @Test
    fun successToFallbackAndDegradedTransitions() {
        val success = AIOrchestrationResult(
            AIUseCase.DIALOGUE_REPLY, AIResultStatus.SUCCESS, AIOutputType.MESSAGE,
            "ok", false, true, "", emptyList()
        )
        assertTrue(success.runtimeExecutable())
        assertFalse(success.fallbackUsed())

        val degraded = AIOrchestrationResult(
            AIUseCase.DIALOGUE_REPLY, AIResultStatus.PROVIDER_FAILED, AIOutputType.MESSAGE,
            "degraded", true, false, "provider_degraded", listOf("Provider responding slowly.")
        )
        assertFalse(degraded.runtimeExecutable())
        assertTrue(degraded.fallbackUsed())

        val fallback = AIOrchestrationResult(
            AIUseCase.DIALOGUE_REPLY, AIResultStatus.FALLBACK_USED, AIOutputType.MESSAGE,
            "fallback", true, false, "ai_response_empty", listOf("AI returned empty response.")
        )
        assertFalse(fallback.runtimeExecutable())
        assertTrue(fallback.fallbackUsed())
        assertEquals("ai_response_empty", fallback.errorCode())
    }

    @Test
    fun disabledResultCannotExecute() {
        val result = AIOrchestrationResult(
            AIUseCase.DIALOGUE_REPLY, AIResultStatus.DISABLED, AIOutputType.MESSAGE,
            "AI disabled", true, false, "ai_orchestration_disabled", emptyList()
        )
        assertEquals(AIResultStatus.DISABLED, result.status())
        assertFalse(result.runtimeExecutable())
        assertTrue(result.fallbackUsed())
    }

    @Test
    fun requestSanitizesContextAndKeepsItImmutable() {
        val context = LinkedHashMap<String?, String?>()
        context[" region "] = " spawn "
        context[""] = "ignored"
        context[null] = "ignored"
        context["emptyValue"] = null

        val request = AIOrchestrationRequest(
            AIUseCase.REACTION_TEXT,
            " npc-1 ",
            " Hero ",
            " quest ",
            context
        )

        assertEquals("npc-1", request.actorId())
        assertEquals("Hero", request.playerName())
        assertEquals("quest", request.mechanicId())
        assertEquals("spawn", request.context()["region"])
        assertEquals("", request.context()["emptyValue"])
        assertFalse(request.context().containsKey(""))
        assertThrows(UnsupportedOperationException::class.java) {
            (request.context() as MutableMap<String, String>)["new"] = "value"
        }
    }
}
