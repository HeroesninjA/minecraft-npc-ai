package ro.ainpc.story

import com.google.gson.JsonParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseManager
import ro.ainpc.world.StoryMode
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.logging.Logger

class StoryStateServiceTest {
    private var connection: Connection? = null
    private lateinit var service: StoryStateService

    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        createStoryTables(connection!!)
        service = StoryStateService(TestDatabaseManager(connection!!), Logger.getLogger("test"))
    }

    @AfterEach
    @Throws(Exception::class)
    fun tearDown() {
        connection?.close()
    }

    @Test
    @Throws(Exception::class)
    fun recordsLocalPlaceStoryWithoutNpcTarget() {
        val event = service.recordEvent(
            "place",
            "demo_sat:piata",
            "demo_sat",
            "demo_sat:piata",
            "local_story",
            "market_bell_rang",
            "Clopotul pietei a sunat",
            "Un eveniment local exista fara NPC tinta si fara progresie.",
            mapOf("state" to "market_alert"),
            "system",
            "story_lane",
            "",
            ""
        )

        assertTrue(event.id() > 0)
        assertEquals("", event.npcId())
        assertEquals("", event.playerUuid())

        val events = service.listRecentEvents("", "demo_sat:piata", 10)
        assertEquals(1, events.size)
        assertEquals("place", events.first().scopeType())
        assertEquals("market_bell_rang", events.first().eventKey())
    }

    @Test
    @Throws(Exception::class)
    fun recordsRegionalStoryVisibleFromRegionScope() {
        val state = service.saveRegionState(
            "demo_sat",
            StoryMode.EVOLUTIVE,
            "regional_unrest",
            listOf("market_alarm", "guard_patrol"),
            mapOf("tension" to "high"),
            "test",
            "story_lane_test"
        )
        service.recordEvent(
            "region",
            "demo_sat",
            "demo_sat",
            "",
            "regional_story",
            "council_warned_village",
            "Sfatul satului a avertizat regiunea",
            "Evenimentul regional este vizibil fara place tinta.",
            mapOf("outcome" to "regional_warning"),
            "system",
            "story_lane",
            "",
            ""
        )

        assertEquals("regional_unrest", state.stateKey())
        assertEquals("high", service.getRegionState("demo_sat")!!.variables()["tension"])

        val events = service.listRecentEvents("demo_sat", "", 10)
        assertEquals(1, events.size)
        assertEquals("region", events.first().scopeType())
        assertEquals("demo_sat", events.first().regionId())
        assertEquals("council_warned_village", events.first().eventKey())
    }

    @Test
    fun readsOnlyRequestedRegionStatesInOneBoundedQuery() {
        service.saveRegionState("village", StoryMode.STATIC, "peace", emptyList(),
            emptyMap(), "test", "test")
        service.saveRegionState("keep", StoryMode.EVOLUTIVE, "siege", emptyList(),
            emptyMap(), "test", "test")

        val states = service.getRegionStates(listOf("village", "keep", "village", "missing"))

        assertEquals(setOf("village", "keep"), states.keys)
        assertEquals("peace", states.getValue("village").stateKey())
        assertEquals("siege", states.getValue("keep").stateKey())
        assertTrue(service.getRegionStates(emptyList()).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            service.getRegionStates((1..65).map { "region$it" })
        }
    }

    @Test
    fun createsRegionStateOnlyOnceWithoutOverwritingExistingStory() {
        val created = service.createRegionStateIfAbsent("keep", StoryMode.STATIC, "peace",
            listOf("peace"), mapOf("story_description_peace" to "Prima poveste"),
            "admin", "world_blueprint:world:hash")
        val second = service.createRegionStateIfAbsent("keep", StoryMode.EVOLUTIVE, "siege",
            listOf("siege"), mapOf("story_description_siege" to "Alta poveste"),
            "other", "world_blueprint:world:other")

        assertNotNull(created)
        assertNull(second)
        assertEquals("Prima poveste", service.getRegionState("keep")!!.variables()["story_description_peace"])
        assertEquals(StoryMode.STATIC, service.getRegionState("keep")!!.storyMode())
        assertEquals("world_blueprint:world:hash", service.getRegionState("keep")!!.source())
        assertEquals("Prima poveste", StoryStateViewResolver.region(null, service.getRegionState("keep"))!!.description)
    }

    @Test
    fun createsMultipleRegionStatesInOneTransaction() {
        val created = service.createRegionStatesIfAbsent(listOf(
            creation("keep"), creation("village"),
        ))

        assertEquals(listOf("keep", "village"), created!!.map { it.regionId() })
        assertEquals("Povestea keep", service.getRegionState("keep")!!.variables()["story_description_default"])
        assertEquals("Povestea village", service.getRegionState("village")!!.variables()["story_description_default"])
    }

    @Test
    fun duplicateDuringBatchRollsBackEarlierInsertAndPreservesExistingState() {
        service.createRegionStateIfAbsent("village", StoryMode.STATIC, "peace", emptyList(),
            mapOf("story_description_peace" to "Poveste existenta"), "admin", "manual")

        val created = service.createRegionStatesIfAbsent(listOf(
            creation("keep"), creation("village"),
        ))

        assertNull(created)
        assertNull(service.getRegionState("keep"))
        assertEquals("Poveste existenta", service.getRegionState("village")!!.variables()["story_description_peace"])
        assertEquals("manual", service.getRegionState("village")!!.source())
    }

    @Test
    fun databaseFailureDuringBatchRollsBackEveryInsert() {
        connection!!.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_village_story BEFORE INSERT ON region_story_state
                WHEN NEW.region_id = 'village'
                BEGIN SELECT RAISE(FAIL, 'insertion rejected'); END
            """.trimIndent())
        }

        assertThrows(SQLException::class.java) {
            service.createRegionStatesIfAbsent(listOf(creation("keep"), creation("village")))
        }
        assertNull(service.getRegionState("keep"))
        assertNull(service.getRegionState("village"))
    }

    @Test
    fun invalidBatchIsRejectedBeforeAnyInsert() {
        assertThrows(IllegalArgumentException::class.java) {
            service.createRegionStatesIfAbsent(listOf(creation("keep"), creation(" keep ")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            service.createRegionStatesIfAbsent(emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            service.createRegionStatesIfAbsent((1..65).map { creation("region$it") })
        }
        assertNull(service.getRegionState("keep"))
    }

    @Test
    fun blueprintBatchWritesQueryableJournalInTheSameTransaction() {
        val fingerprint = "a".repeat(64)
        val created = service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", fingerprint)

        assertEquals(2, created!!.size)
        val entries = service.listBlueprintStoryApplications("world")
        assertEquals(setOf("keep", "village"), entries.map { it.regionId }.toSet())
        assertEquals(1, entries.map { it.batchId }.distinct().size)
        assertTrue(entries.all { it.blueprintFingerprint == fingerprint && it.stateFingerprint.length == 64 })
        assertTrue(entries.all { it.appliedBy == "admin" })
        assertEquals(listOf("keep"), service.listBlueprintStoryApplications("world", "keep").map { it.regionId })
        assertEquals(1, service.listBlueprintStoryApplications("world", limit = 1).size)
        assertTrue(service.listBlueprintStoryApplications("nether").isEmpty())
    }

    @Test
    fun blueprintJournalIsAbsentWhenStateInsertConflicts() {
        service.createRegionStateIfAbsent("village", StoryMode.STATIC, "peace", emptyList(),
            mapOf("story_description_peace" to "Poveste existenta"), "admin", "manual")

        val created = service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))

        assertNull(created)
        assertNull(service.getRegionState("keep"))
        assertTrue(service.listBlueprintStoryApplications("world").isEmpty())
    }

    @Test
    fun journalInsertFailureRollsBackStoryStates() {
        connection!!.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_blueprint_journal BEFORE INSERT ON blueprint_story_applications
                WHEN NEW.region_id = 'village'
                BEGIN SELECT RAISE(FAIL, 'journal rejected'); END
            """.trimIndent())
        }

        assertThrows(SQLException::class.java) {
            service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
                "world", "a".repeat(64))
        }
        assertNull(service.getRegionState("keep"))
        assertNull(service.getRegionState("village"))
        assertTrue(service.listBlueprintStoryApplications("world").isEmpty())
    }

    @Test
    fun blueprintJournalRejectsMismatchedSourceBeforeWriting() {
        assertThrows(IllegalArgumentException::class.java) {
            service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")), "nether", "a".repeat(64))
        }
        assertThrows(IllegalArgumentException::class.java) {
            service.listBlueprintStoryApplications("world", limit = 51)
        }
        assertNull(service.getRegionState("keep"))
        assertTrue(service.listBlueprintStoryApplications("world").isEmpty())
    }

    @Test
    fun verifiesLatestBlueprintApplicationWithoutChangingStoryState() {
        assertEquals(BlueprintStoryApplicationVerificationStatus.NO_JOURNAL,
            service.verifyBlueprintStoryApplication("world", "keep").status)
        service.saveRegionState("manual", StoryMode.STATIC, "peace", emptyList(),
            emptyMap(), "admin", "manual")
        assertEquals(BlueprintStoryApplicationVerificationStatus.NO_JOURNAL,
            service.verifyBlueprintStoryApplication("world", "manual").status)
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")), "world", "a".repeat(64))

        val matching = service.verifyBlueprintStoryApplication("world", "keep")
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH, matching.status)
        assertNotNull(matching.entry)
        assertEquals("keep", matching.state?.regionId())
        assertNull(service.verifyBlueprintStoryApplication("nether", "keep").state)
        assertEquals(BlueprintStoryApplicationVerificationStatus.NO_JOURNAL,
            service.verifyBlueprintStoryApplication("nether", "keep").status)

        service.saveRegionState("keep", StoryMode.EVOLUTIVE, "default", emptyList(),
            mapOf("story_description_default" to "Poveste modificata"), "admin", "manual")
        assertEquals(BlueprintStoryApplicationVerificationStatus.STATE_CHANGED,
            service.verifyBlueprintStoryApplication("world", "keep").status)
        connection!!.prepareStatement("DELETE FROM region_story_state WHERE region_id = ?").use { statement ->
            statement.setString(1, "keep")
            statement.executeUpdate()
        }
        assertEquals(BlueprintStoryApplicationVerificationStatus.STATE_MISSING,
            service.verifyBlueprintStoryApplication("world", "keep").status)
        assertNull(service.verifyBlueprintStoryApplication("world", "keep").state)
        assertEquals(1, service.listBlueprintStoryApplications("world").size)
        assertThrows(IllegalArgumentException::class.java) {
            service.verifyBlueprintStoryApplication("world", " ")
        }
    }

    @Test
    fun verifiesSelectedBlueprintApplicationsTogetherAndRejectsInvalidSelections() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))
        val appliedAt = service.listBlueprintStoryApplications("world", "keep", 1).single().appliedAt
        insertStoryEvent("region", "keep", "keep", appliedAt)
        service.saveRegionState("village", StoryMode.EVOLUTIVE, "default", emptyList(),
            mapOf("story_description_default" to "Alta poveste"), "admin", "manual")

        val verifications = service.verifyBlueprintStoryApplications("world",
            listOf("keep", "village", "unknown"))

        assertEquals(listOf(BlueprintStoryApplicationVerificationStatus.MATCH_WITH_EVENTS,
            BlueprintStoryApplicationVerificationStatus.STATE_CHANGED,
            BlueprintStoryApplicationVerificationStatus.NO_JOURNAL), verifications.map { it.status })
        assertEquals(listOf("keep", "village", null), verifications.map { it.entry?.regionId })
        assertEquals("keep", verifications.first().state?.regionId())
        assertNull(verifications.last().state)
        assertEquals(2, service.listBlueprintStoryApplications("world").size)
        listOf(emptyList(), listOf("keep", " keep "), (1..65).map { "region$it" },
            listOf("keep", " ")).forEach { selection ->
            assertThrows(IllegalArgumentException::class.java) {
                service.verifyBlueprintStoryApplications("world", selection)
            }
        }
    }

    @Test
    fun auditsHistoricalBatchByApplicationIdAndMarksReappliedRegionsAsSuperseded() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))
        val originalEntries = service.listBlueprintStoryApplications("world")
        val applicationId = originalEntries.single { it.regionId == "keep" }.id

        val initial = service.verifyBlueprintStoryApplicationBatch("world", applicationId)
        assertEquals(listOf("keep", "village"), initial.map { it.entry?.regionId })
        assertTrue(initial.all { it.status == BlueprintStoryApplicationVerificationStatus.MATCH })
        assertTrue(service.verifyBlueprintStoryApplicationBatch("nether", applicationId).isEmpty())
        assertTrue(service.verifyBlueprintStoryApplicationBatch("world", Long.MAX_VALUE).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            service.verifyBlueprintStoryApplicationBatch("world", 0)
        }

        service.saveRegionState("village", StoryMode.EVOLUTIVE, "default", emptyList(),
            mapOf("story_description_default" to "Alta poveste"), "admin", "manual")
        connection!!.prepareStatement("DELETE FROM region_story_state WHERE region_id = ?").use { statement ->
            statement.setString(1, "keep")
            statement.executeUpdate()
        }
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")), "world", "a".repeat(64))

        val historical = service.verifyBlueprintStoryApplicationBatch("world", applicationId)
        assertEquals(listOf(BlueprintStoryApplicationVerificationStatus.SUPERSEDED,
            BlueprintStoryApplicationVerificationStatus.STATE_CHANGED), historical.map { it.status })
        assertNull(historical.first().state)
        assertEquals(applicationId, historical.first().entry?.id)
        val newApplicationId = service.listBlueprintStoryApplications("world", "keep", 1).single().id
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH,
            service.verifyBlueprintStoryApplicationBatch("world", newApplicationId).single().status)

        connection!!.prepareStatement("DELETE FROM region_story_state WHERE region_id = ?").use { statement ->
            statement.setString(1, "keep")
            statement.executeUpdate()
        }
        val netherCreation = creation("keep").copy(source = "world_blueprint:nether:${"a".repeat(64)}")
        service.createBlueprintRegionStatesIfAbsent(listOf(netherCreation), "nether", "a".repeat(64))
        assertEquals(BlueprintStoryApplicationVerificationStatus.SUPERSEDED,
            service.verifyBlueprintStoryApplicationBatch("world", newApplicationId).single().status)

        connection!!.prepareStatement("""
            UPDATE blueprint_story_applications SET blueprint_fingerprint = ? WHERE id = ?
        """.trimIndent()).use { statement ->
            statement.setString(1, "other")
            statement.setLong(2, originalEntries.single { it.regionId == "village" }.id)
            statement.executeUpdate()
        }
        assertThrows(SQLException::class.java) {
            service.verifyBlueprintStoryApplicationBatch("world", applicationId)
        }
    }

    @Test
    fun verificationFlagsOnlyRecordedEventsSinceTheAppliedSnapshot() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")), "world", "a".repeat(64))
        val appliedAt = service.listBlueprintStoryApplications("world", "keep", 1).single().appliedAt
        insertStoryEvent("region", "keep", "keep", appliedAt - 1)
        insertStoryEvent("region", "other", "other", appliedAt + 1)
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH,
            service.verifyBlueprintStoryApplication("world", "keep").status)

        insertStoryEvent("region", "keep", "", appliedAt)
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH_WITH_EVENTS,
            service.verifyBlueprintStoryApplication("world", "keep").status)

        connection!!.createStatement().use { statement -> statement.executeUpdate("DELETE FROM story_events") }
        insertStoryEvent("place", "keep:forge", "keep", appliedAt + 1)
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH_WITH_EVENTS,
            service.verifyBlueprintStoryApplication("world", "keep").status)

        service.saveRegionState("keep", StoryMode.EVOLUTIVE, "default", emptyList(),
            mapOf("story_description_default" to "Poveste modificata"), "admin", "manual")
        assertEquals(BlueprintStoryApplicationVerificationStatus.STATE_CHANGED,
            service.verifyBlueprintStoryApplication("world", "keep").status)
    }

    @Test
    @Throws(Exception::class)
    fun pendingEventsAreScopedAndSeparateFromPublishedHistory() {
        val pending = queuePendingEvent("festival_review")

        assertTrue(pending.id > 0L)
        assertTrue(service.hasPendingEvents("REGION", "demo_sat"))
        assertFalse(service.hasPendingEvents("region", "other_region"))
        assertFalse(service.hasPendingEvents("", "demo_sat"))
        assertTrue(service.listRecentEvents("demo_sat", "", 10).isEmpty())

        val queued = service.listPendingEvents("region", "demo_sat", 10)
        assertEquals(1, queued.size)
        assertEquals("festival_review", queued.first().eventKey)
        assertEquals("review", queued.first().payload["state"])
    }

    @Test
    @Throws(Exception::class)
    fun schedulerDraftKeepsProvenanceUntilExplicitPublication() {
        val pending = queuePendingEvent(
            "scheduled_festival",
            actorType = "scheduler",
            actorId = "random-world-events",
        )

        assertEquals("scheduler", pending.actorType)
        assertEquals("random-world-events", pending.actorId)
        assertTrue(service.listRecentEvents("demo_sat", "", 10).isEmpty())

        val published = service.publishPendingEvent(pending.id)

        assertNotNull(published)
        assertEquals("scheduler", published!!.actorType())
        assertEquals("random-world-events", published.actorId())
        assertFalse(service.hasPendingEvents("region", "demo_sat"))
    }

    @Test
    @Throws(Exception::class)
    fun publishesPendingEventAndRemovesItFromQueueAtomically() {
        val pending = queuePendingEvent("festival_publish")

        val published = service.publishPendingEvent(pending.id)

        assertNotNull(published)
        assertEquals("festival_publish", published!!.eventKey())
        assertFalse(service.hasPendingEvents("region", "demo_sat"))
        assertTrue(service.listPendingEvents("region", "demo_sat", 10).isEmpty())
        assertEquals(
            listOf("festival_publish"),
            service.listRecentEvents("demo_sat", "", 10).map { it.eventKey() },
        )
        assertNull(service.publishPendingEvent(pending.id))
    }

    @Test
    @Throws(Exception::class)
    fun failedPublicationRollsBackAndKeepsPendingEvent() {
        val pending = queuePendingEvent("festival_retry")
        connection!!.createStatement().use { it.execute("DROP TABLE story_events") }

        assertThrows(SQLException::class.java) {
            service.publishPendingEvent(pending.id)
        }

        assertTrue(service.hasPendingEvents("region", "demo_sat"))
        assertEquals("festival_retry", service.listPendingEvents("region", "demo_sat", 10).single().eventKey)
    }

    @Test
    @Throws(Exception::class)
    fun discardsPendingEventWithoutPublishingIt() {
        val pending = queuePendingEvent("festival_discard")

        assertTrue(service.discardPendingEvent(pending.id))
        assertFalse(service.discardPendingEvent(pending.id))
        assertFalse(service.hasPendingEvents("region", "demo_sat"))
        assertTrue(service.listRecentEvents("demo_sat", "", 10).isEmpty())
    }

    @Throws(SQLException::class)
    private fun queuePendingEvent(
        eventKey: String,
        actorType: String = "system",
        actorId: String = "story_test",
    ): StoryPendingEvent {
        return service.queueEvent(
            "region",
            "demo_sat",
            "demo_sat",
            "",
            "story_event",
            eventKey,
            "Festival pending",
            "Draft care asteapta publicarea.",
            mapOf("state" to "review"),
            actorType,
            actorId,
            "",
            "",
        )
    }

    @Test
    fun controlledUpdateArchivesPreviousStateAndJournalsTheReplacement() {
        val original = service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")),
            "world", "a".repeat(64))!!.single()
        val entry = service.listBlueprintStoryApplications("world").single()

        insertStoryEvent("region", "keep", "keep", entry.appliedAt - 1)
        insertStoryEvent("region", "other", "other", entry.appliedAt)
        val updated = service.updateBlueprintRegionStory("world", "keep", entry.id,
            "b".repeat(64), "Poveste noua", "editor")

        assertEquals(original.createdAt(), updated.createdAt())
        assertEquals(original.storyMode(), updated.storyMode())
        assertEquals(original.stateKey(), updated.stateKey())
        assertEquals(original.storyPool(), updated.storyPool())
        assertEquals("Poveste noua", updated.variables()["story_description_default"])
        val current = service.verifyBlueprintStoryApplication("world", "keep")
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH, current.status)
        assertEquals("b".repeat(64), current.entry?.blueprintFingerprint)
        assertEquals(1, current.entry?.operationSize)
        assertTrue(current.entry?.operationId?.isNotBlank() == true)
        assertEquals(1, service.verifyBlueprintStoryApplicationOperation("world", current.entry!!.id).size)
        assertEquals(BlueprintStoryApplicationVerificationStatus.SUPERSEDED,
            service.verifyBlueprintStoryApplicationBatch("world", entry.id).single().status)
        connection!!.prepareStatement("SELECT * FROM blueprint_story_revisions").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(entry.id, result.getLong("previous_application_id"))
                assertEquals(current.entry.batchId, result.getString("batch_id"))
                val snapshot = JsonParser.parseString(result.getString("previous_state")).asJsonObject
                assertEquals(original.source(), snapshot["source"].asString)
                assertEquals(original.createdAt(), snapshot["createdAt"].asLong)
                assertEquals(original.variables()["story_description_default"],
                    snapshot["variables"].asJsonObject["story_description_default"].asString)
                assertFalse(result.next())
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            service.updateBlueprintRegionStory("world", "keep", entry.id, "c".repeat(64), "Alta poveste", "editor")
        }
        assertEquals(2, service.listBlueprintStoryApplications("world").size)
    }

    @Test
    fun controlledUpdateRechecksStateEventsPendingEventsAndNoOpRequests() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("changed")),
            "world", "a".repeat(64))
        val entry = service.listBlueprintStoryApplications("world", "keep").single()
        fun update() = service.updateBlueprintRegionStory("world", "keep", entry.id,
            "b".repeat(64), "Poveste noua", "editor")
        assertThrows(IllegalArgumentException::class.java) {
            service.updateBlueprintRegionStory("world", "keep", entry.id,
                "b".repeat(64), "Povestea keep", "editor")
        }
        insertStoryEvent("place", "keep:forge", "keep", entry.appliedAt)
        assertThrows(IllegalArgumentException::class.java) { update() }
        connection!!.createStatement().use { it.executeUpdate("DELETE FROM story_events") }
        listOf(Triple("region", "keep", ""), Triple("place", "keep:forge", "keep")).forEach { (scope, scopeId, regionId) ->
            val pending = service.queueEvent(scope, scopeId, regionId, "", "story_event", "pending", "", "",
                emptyMap(), "system", "test", "", "")
            assertThrows(IllegalArgumentException::class.java) { update() }
            service.discardPendingEvent(pending.id)
        }
        val changedEntry = service.listBlueprintStoryApplications("world", "changed").single()
        service.saveRegionState("changed", StoryMode.STATIC, "war", emptyList(), emptyMap(), "admin", "manual")
        assertThrows(IllegalArgumentException::class.java) {
            service.updateBlueprintRegionStory("world", "changed", changedEntry.id,
                "b".repeat(64), "Poveste noua", "editor")
        }
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH,
            service.verifyBlueprintStoryApplication("world", "keep").status)
        assertEquals(2, service.listBlueprintStoryApplications("world").size)
    }

    @Test
    fun failedUpdateArchiveOrJournalRollsBackBothStateAndRecoverySnapshot() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")), "world", "a".repeat(64))
        val entry = service.listBlueprintStoryApplications("world").single()
        listOf("blueprint_story_revisions", "blueprint_story_applications").forEach { table ->
            connection!!.createStatement().use { statement ->
                statement.execute("""
                    CREATE TRIGGER reject_story_update BEFORE INSERT ON $table
                    BEGIN SELECT RAISE(FAIL, 'write rejected'); END
                """.trimIndent())
            }
            assertThrows(SQLException::class.java) {
                service.updateBlueprintRegionStory("world", "keep", entry.id,
                    "b".repeat(64), "Poveste noua", "editor")
            }
            assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH,
                service.verifyBlueprintStoryApplication("world", "keep").status)
            assertEquals(1, service.listBlueprintStoryApplications("world").size)
            connection!!.prepareStatement("SELECT COUNT(*) FROM blueprint_story_revisions").use { statement ->
                statement.executeQuery().use { result ->
                    assertTrue(result.next())
                    assertEquals(0, result.getInt(1))
                }
            }
            connection!!.createStatement().use { statement ->
                statement.execute("DROP TRIGGER reject_story_update")
            }
        }
    }

    @Test
    fun controlledBatchUpdateArchivesAndJournalsEachRegionAtomically() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))
        val originalEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        assertTrue(originalEntries.values.all { it.operationId.isEmpty() && it.operationSize == 0 })
        val selected = linkedMapOf("keep" to originalEntries.getValue("keep").id,
            "village" to originalEntries.getValue("village").id)

        val updated = service.updateBlueprintRegionStories("world", selected,
            "b".repeat(64), "Poveste comuna", "editor")

        assertEquals(listOf("keep", "village"), updated.map { it.regionId() })
        updated.forEach { state ->
            assertEquals("Poveste comuna", state.variables()["story_description_default"])
            val verification = service.verifyBlueprintStoryApplication("world", state.regionId())
            assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH, verification.status)
            assertEquals("b".repeat(64), verification.entry?.blueprintFingerprint)
            val archive = service.getBlueprintStoryRevision("world", verification.entry!!.id)!!
            assertEquals(originalEntries.getValue(state.regionId()).id, archive.previousApplication.id)
            assertEquals("Povestea ${state.regionId()}",
                archive.previousState.variables()["story_description_default"])
        }
        assertEquals(4, service.listBlueprintStoryApplications("world", limit = 10).size)
        val batchIds = updated.map { state ->
            service.listBlueprintStoryApplications("world", state.regionId(), 1).single().batchId
        }
        assertEquals(2, batchIds.distinct().size)
        val updatedEntries = updated.map { state ->
            service.listBlueprintStoryApplications("world", state.regionId(), 1).single()
        }
        assertEquals(1, updatedEntries.map { it.operationId }.distinct().size)
        assertTrue(updatedEntries.all { it.operationId.isNotBlank() && it.operationSize == 2 })
        assertEquals(listOf("keep", "village"),
            service.verifyBlueprintStoryApplicationOperation("world", updatedEntries.first().id)
                .map { it.entry?.regionId })
        assertTrue(service.verifyBlueprintStoryApplicationOperation("world", originalEntries.getValue("keep").id)
            .isEmpty())
    }

    @Test
    fun controlledBatchUpdateRejectsOneEvolvedRegionWithoutTouchingOthers() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))
        val originalEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        val selected = linkedMapOf("keep" to originalEntries.getValue("keep").id,
            "village" to originalEntries.getValue("village").id)
        insertStoryEvent("region", "village", "village", originalEntries.getValue("village").appliedAt)

        assertThrows(IllegalArgumentException::class.java) {
            service.updateBlueprintRegionStories("world", selected, "b".repeat(64), "Poveste comuna", "editor")
        }
        selected.keys.forEach { regionId ->
            assertEquals("Povestea $regionId",
                service.getRegionState(regionId)?.variables()?.get("story_description_default"))
        }
        assertEquals(2, service.listBlueprintStoryApplications("world", limit = 10).size)
        connection!!.prepareStatement("SELECT COUNT(*) FROM blueprint_story_revisions").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(0, result.getInt(1))
            }
        }
    }

    @Test
    fun failedSecondBatchJournalInsertRollsBackAllRegionsAndArchives() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))
        val originalEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        val selected = linkedMapOf("keep" to originalEntries.getValue("keep").id,
            "village" to originalEntries.getValue("village").id)
        connection!!.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_second_story_update BEFORE INSERT ON blueprint_story_applications
                WHEN NEW.region_id = 'village' AND NEW.blueprint_fingerprint = '${"b".repeat(64)}'
                BEGIN SELECT RAISE(FAIL, 'second journal rejected'); END
            """.trimIndent())
        }

        assertThrows(SQLException::class.java) {
            service.updateBlueprintRegionStories("world", selected, "b".repeat(64), "Poveste comuna", "editor")
        }
        selected.keys.forEach { regionId ->
            assertEquals("Povestea $regionId",
                service.getRegionState(regionId)?.variables()?.get("story_description_default"))
        }
        assertEquals(2, service.listBlueprintStoryApplications("world", limit = 10).size)
        connection!!.prepareStatement("SELECT COUNT(*) FROM blueprint_story_revisions").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(0, result.getInt(1))
            }
        }
    }

    @Test
    fun restoresArchivedStoryAndKeepsTheReplacedVersionAvailable() {
        val original = service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")),
            "world", "a".repeat(64))!!.single()
        val originalEntry = service.listBlueprintStoryApplications("world").single()
        assertNull(service.getBlueprintStoryRevision("world", originalEntry.id))
        service.updateBlueprintRegionStory("world", "keep", originalEntry.id,
            "b".repeat(64), "Poveste noua", "editor")
        val updatedEntry = service.listBlueprintStoryApplications("world", "keep", 1).single()
        val archived = service.getBlueprintStoryRevision("world", updatedEntry.id)!!
        assertEquals(originalEntry.id, archived.previousApplication.id)
        assertEquals(original.variables(), archived.previousState.variables())

        val restored = service.restoreBlueprintRegionStory("world", "keep", updatedEntry.id,
            originalEntry.id, "admin")

        assertEquals(original.variables(), restored.variables())
        assertEquals(original.source(), restored.source())
        assertEquals(original.updatedAt(), restored.updatedAt())
        val latest = service.verifyBlueprintStoryApplication("world", "keep")
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH, latest.status)
        assertEquals(originalEntry.blueprintFingerprint, latest.entry?.blueprintFingerprint)
        val replacementArchive = service.getBlueprintStoryRevision("world", latest.entry!!.id)!!
        assertEquals(updatedEntry.id, replacementArchive.previousApplication.id)
        assertEquals("Poveste noua", replacementArchive.previousState.variables()["story_description_default"])
        val history = service.getBlueprintStoryRevisionHistory("world", latest.entry.id)!!
        assertEquals(listOf(updatedEntry.id, originalEntry.id),
            history.revisions.map { it.previousApplication.id })
        assertFalse(history.hasMore)
        val firstPage = service.getBlueprintStoryRevisionHistory("world", latest.entry.id, 1)!!
        assertEquals(1, firstPage.revisions.size)
        assertTrue(firstPage.hasMore)
        assertTrue(service.getBlueprintStoryRevisionHistory("world", originalEntry.id)!!.revisions.isEmpty())
        assertNull(service.getBlueprintStoryRevisionHistory("other", latest.entry.id))
        assertThrows(IllegalArgumentException::class.java) {
            service.restoreBlueprintRegionStory("world", "keep", updatedEntry.id, originalEntry.id, "admin")
        }
        val returned = service.restoreBlueprintRegionStory("world", "keep", latest.entry.id,
            updatedEntry.id, "admin")
        assertEquals("Poveste noua", returned.variables()["story_description_default"])
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH,
            service.verifyBlueprintStoryApplication("world", "keep").status)
    }

    @Test
    fun archiveHistoryRejectsAValidSnapshotThatSkipsAnApplication() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")), "world", "a".repeat(64))
        val originalEntry = service.listBlueprintStoryApplications("world").single()
        service.updateBlueprintRegionStory("world", "keep", originalEntry.id,
            "b".repeat(64), "Poveste noua", "editor")
        val updatedEntry = service.listBlueprintStoryApplications("world", "keep", 1).single()
        service.restoreBlueprintRegionStory("world", "keep", updatedEntry.id, originalEntry.id, "admin")
        val restoredEntry = service.listBlueprintStoryApplications("world", "keep", 1).single()
        val originalSnapshot = connection!!.prepareStatement("""
            SELECT previous_state FROM blueprint_story_revisions WHERE batch_id = ?
        """.trimIndent()).use { statement ->
            statement.setString(1, updatedEntry.batchId)
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                result.getString(1)
            }
        }
        connection!!.prepareStatement("""
            UPDATE blueprint_story_revisions SET previous_application_id = ?, previous_state = ?
            WHERE batch_id = ?
        """.trimIndent()).use { statement ->
            statement.setLong(1, originalEntry.id)
            statement.setString(2, originalSnapshot)
            statement.setString(3, restoredEntry.batchId)
            statement.executeUpdate()
        }

        assertThrows(SQLException::class.java) {
            service.getBlueprintStoryRevision("world", restoredEntry.id)
        }
        assertThrows(SQLException::class.java) {
            service.getBlueprintStoryRevisionHistory("world", restoredEntry.id)
        }
        assertThrows(SQLException::class.java) {
            service.restoreBlueprintRegionStory("world", "keep", restoredEntry.id, originalEntry.id, "admin")
        }
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH,
            service.verifyBlueprintStoryApplication("world", "keep").status)
    }

    @Test
    fun restorationRejectsEventsPendingWorkAndCorruptArchive() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")), "world", "a".repeat(64))
        val originalEntry = service.listBlueprintStoryApplications("world").single()
        service.updateBlueprintRegionStory("world", "keep", originalEntry.id,
            "b".repeat(64), "Poveste noua", "editor")
        val updatedEntry = service.listBlueprintStoryApplications("world", "keep", 1).single()
        fun restore() = service.restoreBlueprintRegionStory("world", "keep", updatedEntry.id,
            originalEntry.id, "admin")

        insertStoryEvent("place", "keep:forge", "keep", updatedEntry.appliedAt)
        assertThrows(IllegalArgumentException::class.java) { restore() }
        connection!!.createStatement().use { statement -> statement.executeUpdate("DELETE FROM story_events") }
        val pending = service.queueEvent("region", "keep", "", "", "story_event", "pending", "", "",
            emptyMap(), "system", "test", "", "")
        assertThrows(IllegalArgumentException::class.java) { restore() }
        service.discardPendingEvent(pending.id)
        assertThrows(IllegalArgumentException::class.java) {
            service.restoreBlueprintRegionStory("world", "keep", updatedEntry.id, 999L, "admin")
        }

        val archivedSnapshot = connection!!.prepareStatement("""
            SELECT previous_state FROM blueprint_story_revisions WHERE batch_id = ?
        """.trimIndent()).use { statement ->
            statement.setString(1, updatedEntry.batchId)
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                result.getString(1)
            }
        }
        val tamperedSnapshot = JsonParser.parseString(archivedSnapshot).asJsonObject
        tamperedSnapshot["variables"].asJsonObject.addProperty("story_description_default", "Poveste schimbata")
        connection!!.prepareStatement("""
            UPDATE blueprint_story_revisions SET previous_state = ? WHERE batch_id = ?
        """.trimIndent()).use { statement ->
            statement.setString(1, tamperedSnapshot.toString())
            statement.setString(2, updatedEntry.batchId)
            statement.executeUpdate()
        }
        assertThrows(SQLException::class.java) { service.getBlueprintStoryRevision("world", updatedEntry.id) }

        connection!!.prepareStatement("""
            UPDATE blueprint_story_revisions SET previous_state = ? WHERE batch_id = ?
        """.trimIndent()).use { statement ->
            statement.setString(1, "{\"schemaVersion\":1}")
            statement.setString(2, updatedEntry.batchId)
            statement.executeUpdate()
        }
        assertThrows(SQLException::class.java) {
            service.getBlueprintStoryRevision("world", updatedEntry.id)
        }
        assertThrows(SQLException::class.java) { restore() }
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH,
            service.verifyBlueprintStoryApplication("world", "keep").status)
        assertEquals(2, service.listBlueprintStoryApplications("world").size)
    }

    @Test
    fun failedRestorationJournalInsertRollsBackStateAndArchive() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")), "world", "a".repeat(64))
        val originalEntry = service.listBlueprintStoryApplications("world").single()
        service.updateBlueprintRegionStory("world", "keep", originalEntry.id,
            "b".repeat(64), "Poveste noua", "editor")
        val updatedEntry = service.listBlueprintStoryApplications("world", "keep", 1).single()
        connection!!.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_story_restore BEFORE INSERT ON blueprint_story_applications
                BEGIN SELECT RAISE(FAIL, 'journal rejected'); END
            """.trimIndent())
        }
        assertThrows(SQLException::class.java) {
            service.restoreBlueprintRegionStory("world", "keep", updatedEntry.id, originalEntry.id, "admin")
        }
        val current = service.verifyBlueprintStoryApplication("world", "keep")
        assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH, current.status)
        assertEquals(updatedEntry.id, current.entry?.id)
        assertEquals("Poveste noua", current.state?.variables()?.get("story_description_default"))
        connection!!.prepareStatement("SELECT COUNT(*) FROM blueprint_story_revisions").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(1, result.getInt(1))
            }
        }
    }

    @Test
    fun controlledBatchRestoreArchivesAndJournalsEveryRegion() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))
        val originalEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        service.updateBlueprintRegionStories("world", linkedMapOf(
            "keep" to originalEntries.getValue("keep").id,
            "village" to originalEntries.getValue("village").id),
            "b".repeat(64), "Poveste comuna", "editor")
        val updatedEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        val selected = linkedMapOf(
            "keep" to (updatedEntries.getValue("keep").id to originalEntries.getValue("keep").id),
            "village" to (updatedEntries.getValue("village").id to originalEntries.getValue("village").id))

        val restored = service.restoreBlueprintRegionStories("world", selected, "admin")

        assertEquals(listOf("keep", "village"), restored.map { it.regionId() })
        restored.forEach { state ->
            assertEquals("Povestea ${state.regionId()}", state.variables()["story_description_default"])
            val verification = service.verifyBlueprintStoryApplication("world", state.regionId())
            assertEquals(BlueprintStoryApplicationVerificationStatus.MATCH, verification.status)
            assertEquals("a".repeat(64), verification.entry?.blueprintFingerprint)
            val archive = service.getBlueprintStoryRevision("world", verification.entry!!.id)!!
            assertEquals(updatedEntries.getValue(state.regionId()).id, archive.previousApplication.id)
            assertEquals("Poveste comuna", archive.previousState.variables()["story_description_default"])
        }
        assertEquals(6, service.listBlueprintStoryApplications("world", limit = 10).size)
        val batchIds = restored.map { state ->
            service.listBlueprintStoryApplications("world", state.regionId(), 1).single().batchId
        }
        assertEquals(2, batchIds.distinct().size)
        val restoredEntries = restored.map { state ->
            service.listBlueprintStoryApplications("world", state.regionId(), 1).single()
        }
        assertEquals(1, restoredEntries.map { it.operationId }.distinct().size)
        assertTrue(restoredEntries.all { it.operationId.isNotBlank() && it.operationSize == 2 })
        assertTrue(service.verifyBlueprintStoryApplicationOperation("world", restoredEntries.first().id)
            .all { it.status == BlueprintStoryApplicationVerificationStatus.MATCH })
        assertTrue(service.verifyBlueprintStoryApplicationOperation("world", updatedEntries.getValue("keep").id)
            .all { it.status == BlueprintStoryApplicationVerificationStatus.SUPERSEDED })
    }

    @Test
    fun operationAuditRejectsMissingJournalMembers() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))
        val originalEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        service.updateBlueprintRegionStories("world", linkedMapOf(
            "keep" to originalEntries.getValue("keep").id,
            "village" to originalEntries.getValue("village").id),
            "b".repeat(64), "Poveste comuna", "editor")
        val updatedEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        connection!!.prepareStatement("DELETE FROM blueprint_story_applications WHERE id = ?").use { statement ->
            statement.setLong(1, updatedEntries.getValue("village").id)
            statement.executeUpdate()
        }

        assertThrows(SQLException::class.java) {
            service.verifyBlueprintStoryApplicationOperation("world", updatedEntries.getValue("keep").id)
        }
    }

    @Test
    fun operationAuditRejectsMissingArchiveForSupersededApplication() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep")),
            "world", "a".repeat(64))
        val original = service.listBlueprintStoryApplications("world", "keep", 1).single()
        service.updateBlueprintRegionStory("world", "keep", original.id,
            "b".repeat(64), "Poveste noua", "editor")
        val updated = service.listBlueprintStoryApplications("world", "keep", 1).single()
        service.restoreBlueprintRegionStory("world", "keep", updated.id, original.id, "editor")
        connection!!.prepareStatement("DELETE FROM blueprint_story_revisions WHERE batch_id = ?").use { statement ->
            statement.setString(1, updated.batchId)
            statement.executeUpdate()
        }

        assertThrows(SQLException::class.java) {
            service.verifyBlueprintStoryApplicationOperation("world", updated.id)
        }
    }

    @Test
    fun controlledBatchRestoreRejectsAnEvolvedRegionWithoutTouchingOthers() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))
        val originalEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        service.updateBlueprintRegionStories("world", linkedMapOf(
            "keep" to originalEntries.getValue("keep").id,
            "village" to originalEntries.getValue("village").id),
            "b".repeat(64), "Poveste comuna", "editor")
        val updatedEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        val selected = linkedMapOf(
            "keep" to (updatedEntries.getValue("keep").id to originalEntries.getValue("keep").id),
            "village" to (updatedEntries.getValue("village").id to originalEntries.getValue("village").id))
        insertStoryEvent("region", "village", "village", updatedEntries.getValue("village").appliedAt)

        assertThrows(IllegalArgumentException::class.java) {
            service.restoreBlueprintRegionStories("world", selected, "admin")
        }
        selected.keys.forEach { regionId ->
            assertEquals("Poveste comuna",
                service.getRegionState(regionId)?.variables()?.get("story_description_default"))
        }
        assertEquals(4, service.listBlueprintStoryApplications("world", limit = 10).size)
        connection!!.prepareStatement("SELECT COUNT(*) FROM blueprint_story_revisions").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(2, result.getInt(1))
            }
        }
    }

    @Test
    fun failedSecondBatchRestoreJournalInsertRollsBackAllRegionsAndArchives() {
        service.createBlueprintRegionStatesIfAbsent(listOf(creation("keep"), creation("village")),
            "world", "a".repeat(64))
        val originalEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        service.updateBlueprintRegionStories("world", linkedMapOf(
            "keep" to originalEntries.getValue("keep").id,
            "village" to originalEntries.getValue("village").id),
            "b".repeat(64), "Poveste comuna", "editor")
        val updatedEntries = service.listBlueprintStoryApplications("world", limit = 2)
            .associateBy { it.regionId }
        val selected = linkedMapOf(
            "keep" to (updatedEntries.getValue("keep").id to originalEntries.getValue("keep").id),
            "village" to (updatedEntries.getValue("village").id to originalEntries.getValue("village").id))
        connection!!.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_second_story_restore BEFORE INSERT ON blueprint_story_applications
                WHEN NEW.region_id = 'village' AND NEW.blueprint_fingerprint = '${"a".repeat(64)}'
                BEGIN SELECT RAISE(FAIL, 'second restore journal rejected'); END
            """.trimIndent())
        }

        assertThrows(SQLException::class.java) {
            service.restoreBlueprintRegionStories("world", selected, "admin")
        }
        selected.keys.forEach { regionId ->
            assertEquals("Poveste comuna",
                service.getRegionState(regionId)?.variables()?.get("story_description_default"))
            assertEquals(updatedEntries.getValue(regionId).id,
                service.verifyBlueprintStoryApplication("world", regionId).entry?.id)
        }
        assertEquals(4, service.listBlueprintStoryApplications("world", limit = 10).size)
        connection!!.prepareStatement("SELECT COUNT(*) FROM blueprint_story_revisions").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(2, result.getInt(1))
            }
        }
    }

    private fun creation(regionId: String) = RegionStoryStateCreation(regionId, StoryMode.EVOLUTIVE,
        "default", emptyList(), mapOf("story_description_default" to "Povestea ${regionId.trim()}"),
        "admin", "world_blueprint:world:${"a".repeat(64)}")

    private fun insertStoryEvent(scopeType: String, scopeId: String, regionId: String, createdAt: Long) {
        connection!!.prepareStatement("""
            INSERT INTO story_events (scope_type, scope_id, region_id, event_type, created_at)
            VALUES (?, ?, ?, 'story_event', ?)
        """.trimIndent()).use { statement ->
            statement.setString(1, scopeType)
            statement.setString(2, scopeId)
            statement.setString(3, regionId)
            statement.setLong(4, createdAt)
            statement.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun createStoryTables(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TABLE blueprint_story_revisions (
                    batch_id TEXT PRIMARY KEY, world_name TEXT NOT NULL, region_id TEXT NOT NULL,
                    previous_application_id INTEGER NOT NULL, previous_state TEXT NOT NULL,
                    updated_at INTEGER NOT NULL, updated_by TEXT NOT NULL
                )
            """.trimIndent())
            statement.execute(
                """
                CREATE TABLE region_story_state (
                    region_id TEXT PRIMARY KEY,
                    story_mode TEXT NOT NULL DEFAULT 'evolutive',
                    state_key TEXT NOT NULL DEFAULT 'default',
                    story_pool TEXT NOT NULL DEFAULT '[]',
                    variables TEXT NOT NULL DEFAULT '{}',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    updated_by TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT ''
                )
                """
            )
            statement.execute(
                """
                CREATE TABLE place_story_state (
                    place_id TEXT PRIMARY KEY,
                    region_id TEXT NOT NULL DEFAULT '',
                    state_key TEXT NOT NULL DEFAULT 'default',
                    variables TEXT NOT NULL DEFAULT '{}',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    updated_by TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT ''
                )
                """
            )
            statement.execute(
                """
                CREATE TABLE blueprint_story_applications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    batch_id TEXT NOT NULL,
                    world_name TEXT NOT NULL,
                    region_id TEXT NOT NULL,
                    blueprint_fingerprint TEXT NOT NULL,
                    state_fingerprint TEXT NOT NULL,
                    applied_at INTEGER NOT NULL,
                    applied_by TEXT NOT NULL,
                    operation_id TEXT NOT NULL DEFAULT '',
                    operation_size INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            statement.execute(
                """
                CREATE TABLE story_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scope_type TEXT NOT NULL,
                    scope_id TEXT NOT NULL,
                    region_id TEXT NOT NULL DEFAULT '',
                    place_id TEXT NOT NULL DEFAULT '',
                    event_type TEXT NOT NULL,
                    event_key TEXT NOT NULL DEFAULT '',
                    title TEXT NOT NULL DEFAULT '',
                    description TEXT NOT NULL DEFAULT '',
                    payload TEXT NOT NULL DEFAULT '{}',
                    actor_type TEXT NOT NULL DEFAULT '',
                    actor_id TEXT NOT NULL DEFAULT '',
                    player_uuid TEXT NOT NULL DEFAULT '',
                    npc_id TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL
                )
                """
            )
            statement.execute(
                """
                CREATE TABLE story_pending_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scope_type TEXT NOT NULL,
                    scope_id TEXT NOT NULL,
                    region_id TEXT NOT NULL DEFAULT '',
                    place_id TEXT NOT NULL DEFAULT '',
                    event_type TEXT NOT NULL,
                    event_key TEXT NOT NULL DEFAULT '',
                    title TEXT NOT NULL DEFAULT '',
                    description TEXT NOT NULL DEFAULT '',
                    payload TEXT NOT NULL DEFAULT '{}',
                    actor_type TEXT NOT NULL DEFAULT '',
                    actor_id TEXT NOT NULL DEFAULT '',
                    player_uuid TEXT NOT NULL DEFAULT '',
                    npc_id TEXT NOT NULL DEFAULT '',
                    queued_at INTEGER NOT NULL
                )
                """
            )
        }
    }

    private class TestDatabaseManager(private val connection: Connection) : DatabaseManager(null) {
        @Throws(SQLException::class)
        override fun prepareStatement(sql: String): PreparedStatement {
            return connection.prepareStatement(sql)
        }

        @Throws(SQLException::class)
        override fun prepareStatement(sql: String, autoGeneratedKeys: Int): PreparedStatement {
            return connection.prepareStatement(sql, autoGeneratedKeys)
        }

        override fun executeTransaction(block: (Connection) -> Unit) {
            val autoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                block(connection)
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = autoCommit
            }
        }
    }
}
