package ro.ainpc.mcp.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class RedactingSnapshotFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RedactingSnapshotFilter.class);

    private static final Set<String> DEFAULT_BLOCKLIST = Set.of(
        "token", "apiKey", "password", "secret", "credential",
        "OPENAI_API_KEY", "ANTHROPIC_API_KEY", "mcp.token"
    );

    private static final String REDACTED = "***redacted***";

    private final Set<String> blocklist;

    public RedactingSnapshotFilter(
        @Value("${mcp.snapshot.blocklist-fields:}") String blocklistCsv
    ) {
        Set<String> custom = parseBlocklist(blocklistCsv);
        this.blocklist = custom.isEmpty() ? DEFAULT_BLOCKLIST : custom;
        LOG.info("RedactingSnapshotFilter activ cu {} campuri in blocklist", this.blocklist.size());
    }

    public RuntimeSnapshot redact(RuntimeSnapshot snapshot) {
        if (snapshot == null) return null;
        RuntimeSnapshot copy = new RuntimeSnapshot();
        copy.setSchemaVersion(snapshot.getSchemaVersion());
        copy.setTimestamp(snapshot.getTimestamp());
        copy.setPlugin(snapshot.getPlugin());
        copy.setFeatures(snapshot.getFeatures());
        copy.setNpc(redactNpc(snapshot.getNpc()));
        copy.setWorld(snapshot.getWorld());
        copy.setQuests(redactQuests(snapshot.getQuests()));
        copy.setBuildMode(snapshot.getBuildMode());
        copy.setHealth(snapshot.getHealth());
        return copy;
    }

    private RuntimeSnapshot.NpcSnapshot redactNpc(RuntimeSnapshot.NpcSnapshot npc) {
        if (npc == null) return null;
        RuntimeSnapshot.NpcSnapshot copy = new RuntimeSnapshot.NpcSnapshot();
        copy.setTotalCount(npc.getTotalCount());
        copy.setByRegion(npc.getByRegion());
        copy.setSamples(redactNpcSamples(npc.getSamples()));
        copy.setRelationshipCount(npc.getRelationshipCount());
        copy.setEconomyNpcCount(npc.getEconomyNpcCount());
        copy.setEconomyTotalValue(npc.getEconomyTotalValue());
        copy.setSocialGatherings(npc.getSocialGatherings());
        return copy;
    }

    private List<RuntimeSnapshot.NpcSample> redactNpcSamples(List<RuntimeSnapshot.NpcSample> samples) {
        if (samples == null) return null;
        return samples.stream()
            .map(s -> {
                RuntimeSnapshot.NpcSample copy = new RuntimeSnapshot.NpcSample();
                copy.setNpcId(s.getNpcId());
                copy.setName(redactIfSensitive(s.getName()));
                copy.setProfession(redactIfSensitive(s.getProfession()));
                copy.setRegionId(redactIfSensitive(s.getRegionId()));
                copy.setWorldName(redactIfSensitive(s.getWorldName()));
                return copy;
            })
            .collect(Collectors.toList());
    }

    private RuntimeSnapshot.QuestSnapshot redactQuests(RuntimeSnapshot.QuestSnapshot quests) {
        if (quests == null) return null;
        RuntimeSnapshot.QuestSnapshot copy = new RuntimeSnapshot.QuestSnapshot();
        copy.setActivePlayerQuests(quests.getActivePlayerQuests());
        copy.setActiveGlobalQuests(quests.getActiveGlobalQuests());
        copy.setSamples(redactQuestSamples(quests.getSamples()));
        copy.setStoryEventCount(quests.getStoryEventCount());
        copy.setRecentStoryEvents(quests.getRecentStoryEvents());
        return copy;
    }

    private List<RuntimeSnapshot.QuestSample> redactQuestSamples(List<RuntimeSnapshot.QuestSample> samples) {
        if (samples == null) return null;
        return samples.stream()
            .map(s -> {
                RuntimeSnapshot.QuestSample copy = new RuntimeSnapshot.QuestSample();
                copy.setTemplateId(s.getTemplateId());
                copy.setMechanic(s.getMechanic());
                copy.setPlayerUuid(redactIfSensitive(s.getPlayerUuid()));
                copy.setStatus(s.getStatus());
                return copy;
            })
            .collect(Collectors.toList());
    }

    private String redactIfSensitive(String value) {
        if (value == null) return null;
        if (isInBlocklist(value)) return REDACTED;
        if (matchesApiKeyPattern(value)) return REDACTED;
        return value;
    }

    private boolean isInBlocklist(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        for (String blocked : blocklist) {
            if (lower.contains(blocked.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesApiKeyPattern(String value) {
        return value.matches("(?i).*(sk-[a-zA-Z0-9]{10,}|pk-[a-zA-Z0-9]{10,}|sk-proj-[a-zA-Z0-9_-]{20,}).*");
    }

    private static Set<String> parseBlocklist(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Stream.of(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }
}
