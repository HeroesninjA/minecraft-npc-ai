package ro.ainpc.ai.orchestration;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIOrchestrationServiceTest {

    @Test
    void questDraftPolicyRequiresValidationAndCannotExecuteRuntime() {
        AIOrchestrationPolicy policy = AIOrchestrationPolicy.forUseCase(AIUseCase.QUEST_DRAFT);

        assertEquals(AIOutputType.DRAFT, policy.outputType());
        assertTrue(policy.validationRequired());
        assertTrue(policy.fallbackRequired());
        assertFalse(policy.runtimeExecutable());
    }

    @Test
    void disabledServiceReturnsDeterministicFallback() {
        AIOrchestrationService service = new AIOrchestrationService(null);
        AIOrchestrationRequest request = new AIOrchestrationRequest(
            AIUseCase.STORY_DRAFT,
            "npc-1",
            "Hero",
            "story",
            Map.of("region", "spawn")
        );

        AIOrchestrationResult result = service.orchestrate(request);

        assertEquals(AIResultStatus.DISABLED, result.status());
        assertEquals(AIOutputType.DRAFT, result.outputType());
        assertTrue(result.fallbackUsed());
        assertFalse(result.runtimeExecutable());
        assertEquals("ai_orchestration_disabled", result.errorCode());
    }

    @Test
    void nullRequestFailsValidation() {
        AIOrchestrationService service = new AIOrchestrationService(null);

        AIOrchestrationResult result = service.orchestrate(null);

        assertEquals(AIResultStatus.VALIDATION_FAILED, result.status());
        assertEquals("invalid_request", result.errorCode());
        assertFalse(result.runtimeExecutable());
    }

    @Test
    void requestSanitizesContextAndKeepsItImmutable() {
        Map<String, String> context = new LinkedHashMap<>();
        context.put(" region ", " spawn ");
        context.put("", "ignored");
        context.put(null, "ignored");
        context.put("emptyValue", null);

        AIOrchestrationRequest request = new AIOrchestrationRequest(
            AIUseCase.REACTION_TEXT,
            " npc-1 ",
            " Hero ",
            " quest ",
            context
        );

        assertEquals("npc-1", request.actorId());
        assertEquals("Hero", request.playerName());
        assertEquals("quest", request.mechanicId());
        assertEquals("spawn", request.context().get("region"));
        assertEquals("", request.context().get("emptyValue"));
        assertFalse(request.context().containsKey(""));
        assertThrows(UnsupportedOperationException.class, () -> request.context().put("new", "value"));
    }
}
