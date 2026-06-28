package ro.ainpc.mcp.bridge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RedactingSnapshotFilterTest {
    private final RedactingSnapshotFilter filter = new RedactingSnapshotFilter("");

    @Test
    void redactsDefaultBlocklistFields() {
        var sample = new RuntimeSnapshot.NpcSample();
        sample.setNpcId(1);
        sample.setName("test");
        sample.setProfession("fierar");

        var npc = new RuntimeSnapshot.NpcSnapshot();
        npc.setTotalCount(1);
        npc.setSamples(java.util.List.of(sample));

        var snapshot = new RuntimeSnapshot();
        snapshot.setSchemaVersion(1);
        snapshot.setTimestamp("2026-01-01T00:00:00Z");
        snapshot.setNpc(npc);

        RuntimeSnapshot redacted = filter.redact(snapshot);
        assertNotNull(redacted);
        assertEquals(1, redacted.getSchemaVersion());
        assertEquals("test", redacted.getNpc().getSamples().get(0).getName());
    }

    @Test
    void handlesNullSnapshotGracefully() {
        assertNull(filter.redact(null));
    }

    @Test
    void redactsSkApiKeysInName() {
        var sample = new RuntimeSnapshot.NpcSample();
        sample.setNpcId(1);
        sample.setName("sk-proj-test12345abcdefghijklmnop");
        sample.setProfession("admin");

        var npc = new RuntimeSnapshot.NpcSnapshot();
        npc.setTotalCount(1);
        npc.setSamples(java.util.List.of(sample));

        var snapshot = new RuntimeSnapshot();
        snapshot.setSchemaVersion(1);
        snapshot.setTimestamp("2026-01-01T00:00:00Z");
        snapshot.setNpc(npc);

        RuntimeSnapshot redacted = filter.redact(snapshot);
        assertEquals("***redacted***", redacted.getNpc().getSamples().get(0).getName());
    }

    @Test
    void redactsQuestPlayerUuid() {
        var quest = new RuntimeSnapshot.QuestSample();
        quest.setTemplateId("test:Q01");
        quest.setPlayerUuid("sk-proj-abcdef1234567890abcdef12");
        quest.setStatus("active");

        var quests = new RuntimeSnapshot.QuestSnapshot();
        quests.setActivePlayerQuests(1);
        quests.setSamples(java.util.List.of(quest));

        var snapshot = new RuntimeSnapshot();
        snapshot.setSchemaVersion(1);
        snapshot.setTimestamp("2026-01-01T00:00:00Z");
        snapshot.setQuests(quests);
        snapshot.setNpc(new RuntimeSnapshot.NpcSnapshot());

        RuntimeSnapshot redacted = filter.redact(snapshot);
        assertEquals("***redacted***", redacted.getQuests().getSamples().get(0).getPlayerUuid());
    }

    @Test
    void preservesNonSensitiveFields() {
        var sample = new RuntimeSnapshot.NpcSample();
        sample.setNpcId(42);
        sample.setName("Vasile");
        sample.setProfession("Pescar");
        sample.setRegionId("delta");
        sample.setWorldName("lumea1");

        var npc = new RuntimeSnapshot.NpcSnapshot();
        npc.setTotalCount(1);
        npc.setSamples(java.util.List.of(sample));

        var snapshot = new RuntimeSnapshot();
        snapshot.setSchemaVersion(1);
        snapshot.setTimestamp("2026-01-01T00:00:00Z");
        snapshot.setNpc(npc);

        RuntimeSnapshot redacted = filter.redact(snapshot);
        assertEquals("Vasile", redacted.getNpc().getSamples().get(0).getName());
        assertEquals("Pescar", redacted.getNpc().getSamples().get(0).getProfession());
        assertEquals("delta", redacted.getNpc().getSamples().get(0).getRegionId());
    }
}
