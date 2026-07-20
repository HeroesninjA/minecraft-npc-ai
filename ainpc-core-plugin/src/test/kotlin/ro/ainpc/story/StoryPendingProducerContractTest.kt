package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText

class StoryPendingProducerContractTest {
    @Test
    fun schedulerReviewModeIsExplicitAndBackwardCompatible() {
        val configKeys = Path("src/main/kotlin/ro/ainpc/utils/ConfigKeys.kt").readText()
        val config = Path("src/main/resources/config.yml").readText()
        val authoring = Path("src/main/kotlin/ro/ainpc/story/StoryAuthoringService.kt").readText()
        val scheduler = Path("src/main/kotlin/ro/ainpc/story/RandomWorldEventService.kt").readText()

        assertTrue(configKeys.contains("STORY_RANDOM_REVIEW_REQUIRED = \"story.random_events_require_review\""))
        assertTrue(config.contains("random_events_enabled: false"))
        assertTrue(config.contains("random_events_require_review: false"))
        assertTrue(authoring.contains("fun queueTemplate("))
        assertTrue(authoring.contains("actorType.isBlank() || actorId.isBlank()"))
        assertTrue(scheduler.contains("getBoolean(ConfigKeys.STORY_RANDOM_REVIEW_REQUIRED, false)"))
        assertTrue(scheduler.contains("hasPendingEvents(\"region\", regionId)"))
        assertTrue(scheduler.contains("authoring.queueTemplate("))
        assertTrue(scheduler.contains("authoring.applyTemplate("))
        assertTrue(scheduler.contains("actorType = \"scheduler\""))
        assertTrue(scheduler.contains("actorId = \"random-world-events\""))
    }

    @Test
    fun adminCommandsExposeThePendingReviewLifecycle() {
        val command = Path("src/main/kotlin/ro/ainpc/commands/AINPCCommandStory.kt").readText()
        val usage = Path("src/main/kotlin/ro/ainpc/commands/AINPCCommandDisplay.kt").readText()
        val tabCompleter = Path("src/main/kotlin/ro/ainpc/commands/AINPCTabCompleter.kt").readText()

        assertTrue(command.contains("\"pending\" -> handleStoryPending"))
        assertTrue(command.contains("\"publish\" -> handleStoryPublish"))
        assertTrue(command.contains("\"discard\" -> handleStoryDiscard"))
        assertTrue(command.contains("storyAuthoringService.publishPendingEvent(pendingId)"))
        assertTrue(command.contains("storyAuthoringService.discardPendingEvent(pendingId)"))
        assertTrue(usage.contains("/ainpc story pending <regionId|placeId> [limit]"))
        assertTrue(usage.contains("/ainpc story publish <pendingId>"))
        assertTrue(usage.contains("/ainpc story discard <pendingId>"))
        assertTrue(tabCompleter.contains("\"author\", \"pending\", \"publish\", \"discard\""))
    }
}
