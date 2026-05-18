package ro.ainpc.ai.orchestration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
