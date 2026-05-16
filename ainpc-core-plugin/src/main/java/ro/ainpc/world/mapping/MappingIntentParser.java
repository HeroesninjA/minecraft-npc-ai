package ro.ainpc.world.mapping;

import ro.ainpc.world.PlaceType;
import ro.ainpc.world.RegionType;
import ro.ainpc.world.WorldNodeType;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MappingIntentParser {

    private MappingIntentParser() {
    }

    public static MappingDraftSuggestion suggest(MappingDraftKind kind, String description) {
        String clean = cleanDescription(description);
        String normalized = normalize(clean);
        return switch (kind) {
            case REGION -> suggestRegion(clean, normalized);
            case PLACE -> suggestPlace(clean, normalized);
            case NODE -> suggestNode(clean, normalized);
            case NPC_BIND -> new MappingDraftSuggestion(
                slugOrFallback(clean, "npc_bind"),
                displayName(clean, "NPC Bind"),
                "npc_bind",
                List.of(),
                Map.of("draft_only", "true"),
                2.5D,
                List.of("Confirmarea aplica bind-ul NPC pe profil, metadata mapping si npc_world_bindings.")
            );
            case QUEST_ANCHOR -> new MappingDraftSuggestion(
                slugOrFallback(clean, "quest_anchor"),
                displayName(clean, "Quest Anchor"),
                "quest_anchor",
                List.of(),
                Map.of("draft_only", "true"),
                2.5D,
                List.of("Confirmarea scrie direct in quest_anchor_bindings pentru progresia aleasa.")
            );
        };
    }

    public static String slugOrFallback(String value, String fallback) {
        String normalized = normalize(value)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "")
            .replaceAll("_+", "_");
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.length() > 42) {
            return normalized.substring(0, 42).replaceAll("_+$", "");
        }
        return normalized;
    }

    public static String cleanDescription(String description) {
        String value = description == null ? "" : description.trim();
        value = value.replaceAll("(?i)\\b(aici|asta|acesta|aceasta|este|va fi|e|un|o|the|this)\\b", " ");
        value = value.replaceAll("\\s+", " ").trim();
        return value.isBlank() ? "mapping draft" : value;
    }

    public static String normalize(String value) {
        String raw = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        String ascii = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return ascii
            .replace('\u0103', 'a')
            .replace('\u00e2', 'a')
            .replace('\u00ee', 'i')
            .replace('\u0219', 's')
            .replace('\u0163', 't')
            .replace('\u021b', 't');
    }

    private static MappingDraftSuggestion suggestRegion(String clean, String normalized) {
        RegionType type = RegionType.CUSTOM;
        Set<String> tags = new LinkedHashSet<>();
        if (containsAny(normalized, "sat", "village", "asezare")) {
            type = RegionType.SETTLEMENT;
            tags.addAll(List.of("settlement", "village"));
        } else if (containsAny(normalized, "castel", "castle", "cetate")) {
            type = RegionType.CASTLE;
            tags.addAll(List.of("castle", "fortified"));
        } else if (containsAny(normalized, "dungeon", "temnita", "cripta")) {
            type = RegionType.DUNGEON;
            tags.addAll(List.of("dungeon", "danger"));
        } else if (containsAny(normalized, "pestera", "cave", "mina")) {
            type = RegionType.CAVE;
            tags.addAll(List.of("cave", "underground"));
        } else if (containsAny(normalized, "padure", "drum", "camp", "wilderness")) {
            type = RegionType.WILDERNESS;
            tags.addAll(List.of("wilderness"));
        }

        tags.add("manual");
        return new MappingDraftSuggestion(
            preferredId(clean, normalized, type.getId()),
            displayName(clean, "Regiune"),
            type.getId(),
            List.copyOf(tags),
            Map.of("source", "wand_prompt"),
            2.5D,
            List.of()
        );
    }

    private static MappingDraftSuggestion suggestPlace(String clean, String normalized) {
        PlaceType type = PlaceType.CUSTOM;
        Set<String> tags = new LinkedHashSet<>();
        Map<String, String> metadata = new LinkedHashMap<>();
        boolean publicAccess = true;

        if (containsAny(normalized, "casa")) {
            type = PlaceType.HOUSE;
            tags.addAll(List.of("home", "residential"));
            publicAccess = false;
            if (containsAny(normalized, "fierar", "blacksmith")) {
                tags.add("blacksmith");
                metadata.put("profession", "blacksmith");
            }
        } else if (containsAny(normalized, "fierarie", "forja", "fierar", "blacksmith", "forge")) {
            type = PlaceType.FORGE;
            tags.addAll(List.of("workplace", "blacksmith", "shop"));
            metadata.put("profession", "blacksmith");
            metadata.put("role", "work");
        } else if (containsAny(normalized, "taverna", "han", "inn", "tavern")) {
            type = PlaceType.TAVERN;
            tags.addAll(List.of("social", "public", "food"));
        } else if (containsAny(normalized, "piata", "market", "targ")) {
            type = PlaceType.MARKET;
            tags.addAll(List.of("public", "social", "trade"));
            metadata.put("role", "social");
        } else if (containsAny(normalized, "ferma", "farm", "lan")) {
            type = PlaceType.FARM;
            tags.addAll(List.of("farm", "workplace", "food"));
            metadata.put("role", "work");
        } else if (containsAny(normalized, "magazin", "shop", "negustor")) {
            type = PlaceType.SHOP;
            tags.addAll(List.of("shop", "trade", "public"));
        } else if (containsAny(normalized, "tabara", "camp")) {
            type = PlaceType.CAMP;
            tags.addAll(List.of("camp"));
        } else if (containsAny(normalized, "camera", "sala")) {
            type = PlaceType.CASTLE_ROOM;
            tags.addAll(List.of("room"));
        } else if (containsAny(normalized, "cripta", "pestera", "grota")) {
            type = PlaceType.CAVE_ROOM;
            tags.addAll(List.of("cave", "danger"));
        }

        tags.add("manual");
        metadata.put("source", "wand_prompt");
        metadata.put("public_access_hint", publicAccess ? "true" : "false");
        return new MappingDraftSuggestion(
            preferredId(clean, normalized, type.getId()),
            displayName(clean, "Place"),
            type.getId(),
            List.copyOf(tags),
            metadata,
            2.5D,
            List.of()
        );
    }

    private static MappingDraftSuggestion suggestNode(String clean, String normalized) {
        WorldNodeType type = WorldNodeType.CUSTOM;
        Set<String> metadataTags = new LinkedHashSet<>();
        Map<String, String> metadata = new LinkedHashMap<>();
        String preferredId = "";
        double radius = 2.5D;

        if (containsAny(normalized, "avizier", "panou", "board", "quest board", "quest", "ancora")) {
            type = WorldNodeType.QUEST_TRIGGER;
            preferredId = containsAny(normalized, "avizier", "panou", "board") ? "quest_board" : "quest_anchor";
            metadata.put("semantic", preferredId);
            metadata.put("role", "quest_anchor");
            metadataTags.addAll(List.of("board", "public", "quests"));
            radius = 2.0D;
        } else if (containsAny(normalized, "ritual", "cerc", "altar")) {
            type = WorldNodeType.QUEST_TRIGGER;
            preferredId = "ritual_circle";
            metadata.put("semantic", "ritual_circle");
            metadata.put("role", "ritual_start");
            metadataTags.addAll(List.of("ritual", "quest_anchor"));
            radius = 3.0D;
        } else if (containsAny(normalized, "intrare", "usa", "poarta", "entrance")) {
            type = WorldNodeType.ENTRANCE;
            preferredId = "entrance";
            metadata.put("semantic", "entrance");
            radius = 2.0D;
        } else if (containsAny(normalized, "pat", "bed")) {
            type = WorldNodeType.BED;
            preferredId = "bed";
            metadata.put("semantic", "bed");
            radius = 1.5D;
        } else if (containsAny(normalized, "munca", "lucru", "work", "nicovala", "workstation")) {
            type = WorldNodeType.WORKSTATION;
            preferredId = containsAny(normalized, "nicovala") ? "anvil" : "workstation";
            metadata.put("semantic", preferredId);
            metadata.put("role", "work");
            radius = 2.0D;
        } else if (containsAny(normalized, "aduna", "intalnire", "meeting", "social")) {
            type = WorldNodeType.MEETING_POINT;
            preferredId = "meeting_point";
            metadata.put("semantic", "meeting_point");
            metadata.put("role", "social");
            radius = 4.0D;
        } else if (containsAny(normalized, "spawn", "aparitie")) {
            type = WorldNodeType.NPC_SPAWN;
            preferredId = "npc_spawn";
            metadata.put("semantic", "npc_spawn");
            radius = 2.0D;
        }

        metadata.put("source", "wand_prompt");
        if (!metadataTags.isEmpty()) {
            metadata.put("tags", String.join(",", metadataTags));
        }
        String id = preferredId.isBlank() ? preferredId(clean, normalized, type.getId()) : preferredId;
        return new MappingDraftSuggestion(
            id,
            displayName(clean, "Node"),
            type.getId(),
            List.of(),
            metadata,
            radius,
            List.of()
        );
    }

    private static boolean containsAny(String normalized, String... tokens) {
        for (String token : tokens) {
            if (normalized.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private static String preferredId(String clean, String normalized, String fallback) {
        List<String> specific = new ArrayList<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() <= 2 || isFiller(token)) {
                continue;
            }
            specific.add(token);
        }
        String source = specific.isEmpty() ? clean : String.join("_", specific);
        return slugOrFallback(source, fallback);
    }

    private static boolean isFiller(String token) {
        return switch (token) {
            case "aici", "este", "asta", "acesta", "aceasta", "pentru", "lui", "din", "de", "la", "si", "cu" -> true;
            default -> false;
        };
    }

    private static String displayName(String clean, String fallback) {
        String value = clean == null || clean.isBlank() ? fallback : clean.trim();
        String[] words = value.split("\\s+");
        List<String> formatted = new ArrayList<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (word.length() == 1) {
                formatted.add(word.toUpperCase(Locale.ROOT));
            } else {
                formatted.add(word.substring(0, 1).toUpperCase(Locale.ROOT)
                    + word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return formatted.isEmpty() ? fallback : String.join(" ", formatted);
    }
}
