package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StoryActionValidatorTest {

    private fun entry(type: String, metadata: Map<String, String> = emptyMap(), payload: Map<String, String> = emptyMap(), variables: Map<String, String> = emptyMap()): FeaturePackLoader.QuestEntryDefinition {
        return FeaturePackLoader.QuestEntryDefinition(type, null, 1, null, metadata, variables, payload)
    }

    @Test
    fun validSetStoryStatePasses() {
        val result = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("scope" to "region", "target" to "region:spawn", "state" to "blacksmith_helped"),
            variables = mapOf("quest" to "Q01")
        ))
        assertTrue(result.valid, "Errors: ${result.errors}")
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validRecordStoryEventPasses() {
        val result = StoryActionValidator.validate(entry(
            type = "record_story_event",
            metadata = mapOf("scope" to "place", "target" to "place:market", "event_type" to "quest_completed", "event_key" to "q01_done"),
            payload = mapOf("quest" to "Q01", "outcome" to "success")
        ))
        assertTrue(result.valid, "Errors: ${result.errors}")
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun rejectsUnknownType() {
        val result = StoryActionValidator.validate(entry(type = "invalid_type"))
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("necunoscut") })
    }

    @Test
    fun rejectsMissingScope() {
        val result = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("target" to "region:spawn", "state" to "helped")
        ))
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("scope") })
    }

    @Test
    fun rejectsInvalidScope() {
        val result = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("scope" to "invalid_scope", "target" to "region:spawn", "state" to "helped")
        ))
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("scope") })
    }

    @Test
    fun acceptsNormalizedScopeAliases() {
        val regionResult = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("scope" to "village", "target" to "region:spawn", "state" to "helped")
        ))
        assertTrue(regionResult.valid, "Errors: ${regionResult.errors}")

        val placeResult = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("scope" to "location", "target" to "place:market", "state" to "helped")
        ))
        assertTrue(placeResult.valid, "Errors: ${placeResult.errors}")
    }

    @Test
    fun rejectsMissingTarget() {
        val result = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("scope" to "region", "state" to "helped")
        ))
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("target") })
    }

    @Test
    fun rejectsMissingStateForSetStoryState() {
        val result = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("scope" to "region", "target" to "region:spawn")
        ))
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("state") })
    }

    @Test
    fun warnsWhenVariablesEmptyForSetStoryState() {
        val result = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("scope" to "region", "target" to "region:spawn", "state" to "helped")
        ))
        assertTrue(result.valid, "Errors: ${result.errors}")
        assertTrue(result.warnings.any { it.contains("variables") })
    }

    @Test
    fun rejectsMissingEventTypeForRecordStoryEvent() {
        val result = StoryActionValidator.validate(entry(
            type = "record_story_event",
            metadata = mapOf("scope" to "region", "target" to "region:spawn", "event_key" to "test_event"),
            payload = mapOf("quest" to "Q01")
        ))
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("event_type") })
    }

    @Test
    fun rejectsMissingEventKeyForRecordStoryEvent() {
        val result = StoryActionValidator.validate(entry(
            type = "record_story_event",
            metadata = mapOf("scope" to "region", "target" to "region:spawn", "event_type" to "quest_completed"),
            payload = mapOf("quest" to "Q01")
        ))
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("event_key") })
    }

    @Test
    fun rejectsEmptyPayloadForRecordStoryEvent() {
        val result = StoryActionValidator.validate(entry(
            type = "record_story_event",
            metadata = mapOf("scope" to "region", "target" to "region:spawn", "event_type" to "quest_completed", "event_key" to "test")
        ))
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("payload") })
    }

    @Test
    fun warnsForNonSemanticPayload() {
        val result = StoryActionValidator.validate(entry(
            type = "record_story_event",
            metadata = mapOf("scope" to "region", "target" to "region:spawn", "event_type" to "quest_completed", "event_key" to "test"),
            payload = mapOf("random_key" to "value")
        ))
        assertTrue(result.valid, "Errors: ${result.errors}")
        assertTrue(result.warnings.any { it.contains("payload") })
    }

    @Test
    fun validatesTypeOnly() {
        val validResult = StoryActionValidator.validateType("set_story_state")
        assertTrue(validResult.valid)

        val aliasResult = StoryActionValidator.validateType("set-flag")
        assertTrue(aliasResult.valid)

        val invalidResult = StoryActionValidator.validateType("invalid")
        assertFalse(invalidResult.valid)

        val blankResult = StoryActionValidator.validateType("")
        assertFalse(blankResult.valid)

        val nullResult = StoryActionValidator.validateType(null)
        assertFalse(nullResult.valid)
    }

    @Test
    fun acceptsTargetInAnyMetadataKey() {
        val targetKeys = listOf("scope_id", "target_id", "id", "place_id", "region_id", "target_place", "target_region", "place", "region")
        for (key in targetKeys) {
            val result = StoryActionValidator.validate(entry(
                type = "set_story_state",
                metadata = mapOf("scope" to "region", key to "some_value", "state" to "helped"),
                variables = mapOf("quest" to "Q01")
            ))
            assertTrue(result.valid, "Failed for key '$key': Errors: ${result.errors}")
        }
    }

    @Test
    fun validatesAliasTypes() {
        assertTrue(StoryActionValidator.validateType("set-flag").valid)
        assertTrue(StoryActionValidator.validateType("story_state").valid)
        assertTrue(StoryActionValidator.validateType("set_flag").valid)
        assertTrue(StoryActionValidator.validateType("setstate").valid)
        assertTrue(StoryActionValidator.validateType("record event").valid)
        assertTrue(StoryActionValidator.validateType("event").valid)
        assertTrue(StoryActionValidator.validateType("recordstoryevent").valid)
    }

    @Test
    fun acceptsScopeAliases() {
        val villageResult = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("scope" to "village", "target" to "region:spawn", "state" to "helped"),
            variables = mapOf("quest" to "Q01")
        ))
        assertTrue(villageResult.valid, "Errors: ${villageResult.errors}")

        val worldResult = StoryActionValidator.validate(entry(
            type = "set_story_state",
            metadata = mapOf("scope" to "world_region", "target" to "region:spawn", "state" to "helped"),
            variables = mapOf("quest" to "Q01")
        ))
        assertTrue(worldResult.valid, "Errors: ${worldResult.errors}")

        val settlementResult = StoryActionValidator.validate(entry(
            type = "record_story_event",
            metadata = mapOf("scope" to "settlement", "target" to "region:spawn", "event_type" to "test", "event_key" to "test"),
            payload = mapOf("quest" to "Q01")
        ))
        assertTrue(settlementResult.valid, "Errors: ${settlementResult.errors}")
    }

    @Test
    fun acceptsStateInAnyMetadataKey() {
        val stateKeys = listOf("state_key", "flag", "value", "item")
        for (key in stateKeys) {
            val result = StoryActionValidator.validate(entry(
                type = "set_story_state",
                metadata = mapOf("scope" to "region", "target" to "region:spawn", key to "some_value"),
                variables = mapOf("quest" to "Q01")
            ))
            assertTrue(result.valid, "Failed for key '$key': Errors: ${result.errors}")
        }
    }
}
