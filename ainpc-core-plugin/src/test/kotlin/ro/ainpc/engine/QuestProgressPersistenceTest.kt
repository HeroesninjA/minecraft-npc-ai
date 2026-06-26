package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class QuestProgressPersistenceTest {

    @Test
    fun questStatusStorageRoundtrip() {
        for (status in QuestStatus.entries) {
            val stored = status.storageValue()
            val restored = QuestStatus.fromStorage(stored)
            assertEquals(status, restored,
                "Status $status -> storage '$stored' -> restore $restored")
        }
    }

    @Test
    fun questProgressSerializableFields() {
        val templateId = "quest_test_001"
        val questCode = "qc_001"
        val playerId = UUID.randomUUID()
        val now = System.currentTimeMillis()
        val objectiveProgress = mapOf("obj1" to 3, "obj2" to 1)
        val questVariables = mapOf("key1" to "val1", "anchor.obj1.type" to "place")

        val progress = PlayerQuestProgress(
            templateId = templateId,
            questCode = questCode,
            status = QuestStatus.ACTIVE,
            startedAt = now,
            completedAt = 0L,
            updatedAt = now,
            currentPhase = "EXECUTION",
            objectiveProgress = objectiveProgress,
            questVariables = questVariables,
        )

        assertEquals(templateId, progress.templateId())
        assertEquals(questCode, progress.questCode())
        assertEquals(QuestStatus.ACTIVE, progress.status())
        assertEquals(now, progress.startedAt())
        assertEquals(0L, progress.completedAt())
        assertEquals(now, progress.updatedAt())
        assertEquals("EXECUTION", progress.currentPhase())
        assertEquals(3, progress.objectiveProgress()["obj1"])
        assertEquals("val1", progress.questVariables()["key1"])
        assertTrue(progress.isActive())
    }

    @Test
    fun questStatusArchivedCorrectly() {
        assertTrue(QuestStatus.COMPLETED.isArchived())
        assertTrue(QuestStatus.FAILED.isArchived())
    }

    @Test
    fun questStatusNotArchivedForActive() {
        assertTrue(!QuestStatus.ACTIVE.isArchived())
        assertTrue(!QuestStatus.OFFERED.isArchived())
        assertTrue(!QuestStatus.NOT_STARTED.isArchived())
    }

    @Test
    fun questProgressCurrentOnlyForActiveOrOffered() {
        val progress = PlayerQuestProgress("t1", "qc", QuestStatus.COMPLETED,
            0, 0, 0, "", emptyMap(), emptyMap())
        assertTrue(!progress.isCurrent())

        val activeProgress = PlayerQuestProgress("t1", "qc", QuestStatus.ACTIVE,
            0, 0, 0, "", emptyMap(), emptyMap())
        assertTrue(activeProgress.isCurrent())
    }

    @Test
    fun questStatusFromNullReturnsNotStarted() {
        assertEquals(QuestStatus.NOT_STARTED, QuestStatus.fromStorage(null))
        assertEquals(QuestStatus.NOT_STARTED, QuestStatus.fromStorage(""))
        assertEquals(QuestStatus.NOT_STARTED, QuestStatus.fromStorage("   "))
    }

    @Test
    fun questStatusFromUnknownReturnsNotStarted() {
        assertEquals(QuestStatus.NOT_STARTED, QuestStatus.fromStorage("UNKNOWN"))
    }

    @Test
    fun objectiveTypeIsSupported() {
        assertTrue(ObjectiveTypeAliasRegistry.isSupported("collect_item"))
        assertTrue(ObjectiveTypeAliasRegistry.isSupported("talk_to_npc"))
        assertTrue(ObjectiveTypeAliasRegistry.isSupported("kill_mob"))
        assertFalse(ObjectiveTypeAliasRegistry.isSupported("unknown_type"))
        assertFalse(ObjectiveTypeAliasRegistry.isSupported(null))
    }

    @Test
    fun scopeRegistryNormalizesCorrectly() {
        assertEquals("region", ScopeRegistry.normalize("village"))
        assertEquals("region", ScopeRegistry.normalize("settlement"))
        assertEquals("place", ScopeRegistry.normalize("location"))
        assertEquals("", ScopeRegistry.normalize(null))
        assertEquals("", ScopeRegistry.normalize(""))
    }

    @Test
    fun featurePackSchemaVersionDefaults() {
        val pack = FeaturePackLoader.FeaturePack("test", "Test", "Test pack")
        assertEquals(1, pack.schemaVersion)
    }

    @Test
    fun questEntryDefinitionDefaults() {
        val def = FeaturePackLoader.QuestEntryDefinition("type", "item", 5, "desc")
        assertEquals("type", def.type)
        assertEquals("item", def.itemId)
        assertEquals(5, def.amount)
        assertEquals("desc", def.description)
    }
}
