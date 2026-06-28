package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class AinpcSemanticContextTools {
    private static final List<Map<String, Object>> BLOCKS = List.of(
        Map.of(
            "name", "WORLD_LORE",
            "purpose", "Locuri, orientare, ce exista in zona.",
            "fields", List.of("region", "current_place", "nearby_places")
        ),
        Map.of(
            "name", "WORLD_HISTORY",
            "purpose", "Trecut, evenimente si evolutia regiunii.",
            "fields", List.of("region_story_mode", "state", "pool", "recent_events")
        ),
        Map.of(
            "name", "NPC_LORE",
            "purpose", "Povestea, rolul si ocupatia NPC-urilor.",
            "fields", List.of("name", "occupation", "work", "home", "lore")
        ),
        Map.of(
            "name", "STORY_SIGNALS",
            "purpose", "Indicii narative si semnale de progresie.",
            "fields", List.of("signals")
        )
    );

    private static final List<Map<String, Object>> QUEST_BLOCKS = List.of(
        Map.of(
            "name", "QUEST_LORE",
            "purpose", "Tema, rolul si identitatea narativa a quest-ului.",
            "fields", List.of("quest_id", "title", "kind", "theme", "mechanic", "giver", "anchor")
        ),
        Map.of(
            "name", "QUEST_HISTORY",
            "purpose", "Istoricul semantic al quest-ului: decizii, blocaje, semnale si seed-uri.",
            "fields", List.of("decision", "matched_signals", "blocked_reasons", "candidate_templates", "seed_story_mode")
        ),
        Map.of(
            "name", "QUEST_SIGNALS",
            "purpose", "Semnale de progresie si cerinte narative pentru quest.",
            "fields", List.of("signals", "allowed_objectives", "allowed_rewards", "limits")
        )
    );

    private static final List<Map<String, Object>> QUEST_AUTHORING_BLOCKS = List.of(
        Map.of(
            "name", "QUEST_AUTHORING_LORE",
            "purpose", "Datele esentiale de authoring: selector, mechanic, seed si definitia aleasa.",
            "fields", List.of("requested_selector", "requested_mechanic", "seed_region", "seed_place", "selected_template", "selected_progression")
        ),
        Map.of(
            "name", "QUEST_AUTHORING_HISTORY",
            "purpose", "Istoricul deciziei de authoring: status, motiv, warnings si semnale potrivite.",
            "fields", List.of("decision_status", "decision_reason", "matched_signals", "candidate_templates", "blocked_reasons", "warnings")
        ),
        Map.of(
            "name", "QUEST_AUTHORING_SIGNALS",
            "purpose", "Semnale si restrictii pentru generarea quest-ului si seed-ul narativ.",
            "fields", List.of("seed_story_mode", "story_signals", "allowed_objectives", "allowed_rewards", "limits")
        )
    );

    private static final List<Map<String, Object>> MAPPING_BLOCKS = List.of(
        Map.of(
            "name", "MAPPING_CONTEXT",
            "purpose", "Structura world mapping: regiuni, places, noduri si sursele overlay.",
            "fields", List.of("world_admin_enabled", "auto_index_enabled", "region_count", "place_count", "node_count", "overlay_sources")
        ),
        Map.of(
            "name", "MAPPING_HISTORY",
            "purpose", "Istoricul semantic al mapping-ului: semantic index, bucket-uri si drift de date.",
            "fields", List.of("semantic_index_summary", "bucket_count", "bucket_keys")
        ),
        Map.of(
            "name", "MAPPING_STORY",
            "purpose", "Legatura dintre mapping si poveste: state-uri, evenimente si progression gaps.",
            "fields", List.of("story_summary", "story_state_available", "story_event_available", "progression_gap_available")
        )
    );

    private static final List<Map<String, Object>> ROUTING_BLOCKS = List.of(
        Map.of(
            "name", "ROUTING_WORLD",
            "purpose", "Punctul de intrare pentru lume: world lore, world history si NPC lore.",
            "fields", List.of("ainpc.semantic.context", "ainpc.semantic.context.summary")
        ),
        Map.of(
            "name", "ROUTING_STORY",
            "purpose", "Punctul de intrare pentru poveste: story context, story history si story signals.",
            "fields", List.of("ainpc.story.semantic.context", "ainpc.story.semantic.context.summary")
        ),
        Map.of(
            "name", "ROUTING_MAPPING",
            "purpose", "Punctul de intrare pentru mapping: semantic index, history si story links.",
            "fields", List.of("ainpc.mapping.semantic.context", "ainpc.mapping.semantic.context.summary")
        ),
        Map.of(
            "name", "ROUTING_QUEST",
            "purpose", "Punctul de intrare pentru quest: lore, history si signals.",
            "fields", List.of("ainpc.quest.semantic.context", "ainpc.quest.semantic.context.summary")
        ),
        Map.of(
            "name", "ROUTING_QUEST_AUTHORING",
            "purpose", "Punctul de intrare pentru quest authoring: lore, history si signals.",
            "fields", List.of("ainpc.quest.authoring.context.summary")
        )
    );

    private static final List<Map<String, Object>> STORY_BLOCKS = List.of(
        Map.of(
            "name", "STORY_CONTEXT",
            "purpose", "Contextul narativ de baza: regiune, place, NPC si actorul jucator.",
            "fields", List.of("subject_npc", "player", "region_story", "place_story")
        ),
        Map.of(
            "name", "STORY_HISTORY",
            "purpose", "Istoricul narativ: state-uri persistente, evenimente recente si warnings.",
            "fields", List.of("persistent_region_story", "persistent_place_story", "recent_story_events", "warnings")
        ),
        Map.of(
            "name", "STORY_SIGNALS",
            "purpose", "Semnale narative si ancore quest active pentru routing semantic.",
            "fields", List.of("story_signals", "active_quest_anchors")
        )
    );

    @McpTool(
        name = "ainpc.semantic.context",
        description = "Read-only semantic context contract for WORLD_LORE, WORLD_HISTORY, NPC_LORE, and STORY_SIGNALS.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> semanticContext() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "blocks", BLOCKS,
            "aliases", Map.of(
                "semantic", List.of("world semantic", "world lore", "world history", "npc lore")
            ),
            "usage", List.of(
                "Citeste blocurile in ordinea WORLD_LORE -> WORLD_HISTORY -> NPC_LORE -> STORY_SIGNALS.",
                "Nu inventa valori care nu apar in blocuri.",
                "Prefera raspunsuri scurte si factuale cand utilizatorul intreaba despre locuri, istoric sau NPC-uri."
            ),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.semantic.context.summary",
        description = "Short read-only semantic context summary for fast model routing.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> semanticContextSummary() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "summary", "WORLD_LORE = locuri si orientare; WORLD_HISTORY = trecut si evenimente; NPC_LORE = poveste NPC; STORY_SIGNALS = indicii narative.",
            "blockCount", BLOCKS.size(),
            "blockNames", BLOCKS.stream().map(block -> (String) block.get("name")).toList(),
            "recommendedOrder", List.of("WORLD_LORE", "WORLD_HISTORY", "NPC_LORE", "STORY_SIGNALS"),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.quest.semantic.context",
        description = "Read-only semantic context contract for QUEST_LORE, QUEST_HISTORY, and QUEST_SIGNALS.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> questSemanticContext() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "blocks", QUEST_BLOCKS,
            "aliases", Map.of(
                "quest", List.of("quest semantic", "quest lore", "quest history", "quest signals")
            ),
            "usage", List.of(
                "Citeste blocurile in ordinea QUEST_LORE -> QUEST_HISTORY -> QUEST_SIGNALS.",
                "QUEST_LORE descrie tema si identitatea quest-ului.",
                "QUEST_HISTORY descrie decizia, semnalele si blocajele din fluxul quest.",
                "QUEST_SIGNALS descrie obiectivele si recompensele permise."
            ),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.quest.semantic.context.summary",
        description = "Short read-only quest semantic summary for fast model routing.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> questSemanticContextSummary() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "summary", "QUEST_LORE = tema si identitate; QUEST_HISTORY = decizii si blocaje; QUEST_SIGNALS = progresie si restrictii.",
            "blockCount", QUEST_BLOCKS.size(),
            "blockNames", QUEST_BLOCKS.stream().map(block -> (String) block.get("name")).toList(),
            "recommendedOrder", List.of("QUEST_LORE", "QUEST_HISTORY", "QUEST_SIGNALS"),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.quest.authoring.context.summary",
        description = "Short read-only quest authoring summary for fast model routing.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> questAuthoringContextSummary() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "summary", "QUEST_AUTHORING_LORE = selector si seed; QUEST_AUTHORING_HISTORY = decizie si warnings; QUEST_AUTHORING_SIGNALS = story mode si restrictii.",
            "blockCount", QUEST_AUTHORING_BLOCKS.size(),
            "blockNames", QUEST_AUTHORING_BLOCKS.stream().map(block -> (String) block.get("name")).toList(),
            "recommendedOrder", List.of("QUEST_AUTHORING_LORE", "QUEST_AUTHORING_HISTORY", "QUEST_AUTHORING_SIGNALS"),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.mapping.semantic.context",
        description = "Read-only semantic context contract for mapping context, history, and story links.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> mappingSemanticContext() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "blocks", MAPPING_BLOCKS,
            "aliases", Map.of(
                "mapping", List.of("mapping semantic", "world mapping", "mapping history", "mapping story")
            ),
            "usage", List.of(
                "Citeste blocurile in ordinea MAPPING_CONTEXT -> MAPPING_HISTORY -> MAPPING_STORY.",
                "MAPPING_CONTEXT descrie topologia si sursele overlay ale world mapping-ului.",
                "MAPPING_HISTORY descrie semantic index si bucket-urile de rezolvare.",
                "MAPPING_STORY descrie legatura dintre mapping, story state si progression gaps."
            ),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.mapping.semantic.context.summary",
        description = "Short read-only mapping semantic summary for fast model routing.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> mappingSemanticContextSummary() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "summary", "MAPPING_CONTEXT = topologie si overlay; MAPPING_HISTORY = semantic index si bucket-uri; MAPPING_STORY = story summary si progression gaps.",
            "blockCount", MAPPING_BLOCKS.size(),
            "blockNames", MAPPING_BLOCKS.stream().map(block -> (String) block.get("name")).toList(),
            "recommendedOrder", List.of("MAPPING_CONTEXT", "MAPPING_HISTORY", "MAPPING_STORY"),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.story.semantic.context",
        description = "Read-only semantic context contract for STORY_CONTEXT, STORY_HISTORY, and STORY_SIGNALS.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> storySemanticContext() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "blocks", STORY_BLOCKS,
            "aliases", Map.of(
                "story", List.of("story semantic", "story context", "story history", "story signals")
            ),
            "usage", List.of(
                "Citeste blocurile in ordinea STORY_CONTEXT -> STORY_HISTORY -> STORY_SIGNALS.",
                "STORY_CONTEXT descrie elementele locale si actorul implicat in context.",
                "STORY_HISTORY descrie state-ul persistent, evenimentele recente si warnings.",
                "STORY_SIGNALS descrie semnalele narative si ancorele quest active."
            ),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.story.semantic.context.summary",
        description = "Short read-only story semantic summary for fast model routing.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> storySemanticContextSummary() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "summary", "STORY_CONTEXT = regiune si NPC; STORY_HISTORY = state-uri si evenimente; STORY_SIGNALS = semnale si ancore quest.",
            "blockCount", STORY_BLOCKS.size(),
            "blockNames", STORY_BLOCKS.stream().map(block -> (String) block.get("name")).toList(),
            "recommendedOrder", List.of("STORY_CONTEXT", "STORY_HISTORY", "STORY_SIGNALS"),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.routing.semantic.context",
        description = "Read-only routing context that summarizes the available semantic domains.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> routingSemanticContext() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "blocks", ROUTING_BLOCKS,
            "aliases", Map.of(
                "routing", List.of("routing semantic", "semantic routing", "mcp routing", "model routing")
            ),
            "usage", List.of(
                "Citeste blocurile in ordinea ROUTING_WORLD -> ROUTING_STORY -> ROUTING_MAPPING -> ROUTING_QUEST -> ROUTING_QUEST_AUTHORING.",
                "ROUTING_WORLD indica tool-urile pentru world lore si world history.",
                "ROUTING_STORY indica tool-urile pentru story context si story history.",
                "ROUTING_MAPPING indica tool-urile pentru mapping si semantic index.",
                "ROUTING_QUEST si ROUTING_QUEST_AUTHORING indica tool-urile pentru quest si authoring."
            ),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.routing.semantic.context.summary",
        description = "Short read-only routing summary for fast model routing.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> routingSemanticContextSummary() {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "summary", "ROUTING_WORLD = world semantic; ROUTING_STORY = story semantic; ROUTING_MAPPING = mapping semantic; ROUTING_QUEST = quest semantic; ROUTING_QUEST_AUTHORING = quest authoring summary.",
            "blockCount", ROUTING_BLOCKS.size(),
            "blockNames", ROUTING_BLOCKS.stream().map(block -> (String) block.get("name")).toList(),
            "recommendedOrder", List.of("ROUTING_WORLD", "ROUTING_STORY", "ROUTING_MAPPING", "ROUTING_QUEST", "ROUTING_QUEST_AUTHORING"),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.semantic.routing.summary",
        description = "Compact routing summary across all semantic domains.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> semanticRoutingSummary() {
        List<Map<String, Object>> domains = List.of(
            Map.of(
                "name", "WORLD",
                "tool", "ainpc.semantic.context.summary",
                "blockNames", List.of("WORLD_LORE", "WORLD_HISTORY", "NPC_LORE", "STORY_SIGNALS"),
                "summary", "WORLD = lore/history/npc"
            ),
            Map.of(
                "name", "STORY",
                "tool", "ainpc.story.semantic.context.summary",
                "blockNames", List.of("STORY_CONTEXT", "STORY_HISTORY", "STORY_SIGNALS"),
                "summary", "STORY = context/history/signals"
            ),
            Map.of(
                "name", "MAPPING",
                "tool", "ainpc.mapping.semantic.context.summary",
                "blockNames", List.of("MAPPING_CONTEXT", "MAPPING_HISTORY", "MAPPING_STORY"),
                "summary", "MAPPING = context/history/story links"
            ),
            Map.of(
                "name", "QUEST",
                "tool", "ainpc.quest.semantic.context.summary",
                "blockNames", List.of("QUEST_LORE", "QUEST_HISTORY", "QUEST_SIGNALS"),
                "summary", "QUEST = lore/history/signals"
            ),
            Map.of(
                "name", "QUEST_AUTHORING",
                "tool", "ainpc.quest.authoring.context.summary",
                "blockNames", List.of("QUEST_AUTHORING_LORE", "QUEST_AUTHORING_HISTORY", "QUEST_AUTHORING_SIGNALS"),
                "summary", "QUEST_AUTHORING = selector/seed/history/signals"
            ),
            Map.of(
                "name", "ROUTING",
                "tool", "ainpc.routing.semantic.context.summary",
                "blockNames", List.of("ROUTING_WORLD", "ROUTING_STORY", "ROUTING_MAPPING", "ROUTING_QUEST", "ROUTING_QUEST_AUTHORING"),
                "summary", "ROUTING = world/story/mapping/quest/authoring entrypoints"
            )
        );

        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "summary", "WORLD = lore/history/npc; STORY = context/history/signals; MAPPING = context/history/story links; QUEST = lore/history/signals; QUEST_AUTHORING = selector/seed/history/signals.",
            "domainCount", domains.size(),
            "domains", domains,
            "recommendedOrder", List.of(
                "ainpc.semantic.context.summary",
                "ainpc.story.semantic.context.summary",
                "ainpc.mapping.semantic.context.summary",
                "ainpc.quest.semantic.context.summary",
                "ainpc.quest.authoring.context.summary",
                "ainpc.routing.semantic.context.summary"
            ),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.semantic.context",
        description = "Read-only semantic context for any domain: world, story, mapping, quest, quest_authoring, or routing. Use 'summary=true' for compact output.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> unifiedSemanticContext(String domain, Boolean summary) {
        String d = domain != null ? domain.trim().toLowerCase() : "all";
        boolean sum = summary != null && summary;

        return switch (d) {
            case "world" -> sum ? semanticContextSummary() : semanticContext();
            case "quest" -> sum ? questSemanticContextSummary() : questSemanticContext();
            case "quest_authoring" -> sum ? questAuthoringContextSummary() : questAuthoringContextSummary();
            case "mapping" -> sum ? mappingSemanticContextSummary() : mappingSemanticContext();
            case "story" -> sum ? storySemanticContextSummary() : storySemanticContext();
            case "routing" -> sum ? routingSemanticContextSummary() : routingSemanticContext();
            default -> {
                Map<String, Object> world = sum ? semanticContextSummary() : semanticContext();
                Map<String, Object> story = sum ? storySemanticContextSummary() : storySemanticContext();
                Map<String, Object> mapping = sum ? mappingSemanticContextSummary() : mappingSemanticContext();
                Map<String, Object> quest = sum ? questSemanticContextSummary() : questSemanticContext();
                Map<String, Object> questAuth = sum ? questAuthoringContextSummary() : questAuthoringContextSummary();
                Map<String, Object> routing = sum ? routingSemanticContextSummary() : routingSemanticContext();
                yield Map.ofEntries(
                    Map.entry("schemaVersion", 1),
                    Map.entry("service", "ainpc-mcp-service"),
                    Map.entry("domain", "all"),
                    Map.entry("summary", sum),
                    Map.entry("world", world),
                    Map.entry("story", story),
                    Map.entry("mapping", mapping),
                    Map.entry("quest", quest),
                    Map.entry("quest_authoring", questAuth),
                    Map.entry("routing", routing),
                    Map.entry("timestamp", Instant.now().toString())
                );
            }
        };
    }
}
