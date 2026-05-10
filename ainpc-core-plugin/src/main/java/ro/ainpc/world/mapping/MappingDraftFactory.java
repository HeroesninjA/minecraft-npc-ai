package ro.ainpc.world.mapping;

import ro.ainpc.world.PlaceType;
import ro.ainpc.world.RegionType;
import ro.ainpc.world.WorldAdminService;
import ro.ainpc.world.WorldNode;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldNodeType;
import ro.ainpc.world.WorldPlace;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegion;
import ro.ainpc.world.WorldRegionInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MappingDraftFactory {

    public MappingDraft createDraft(UUID playerId,
                                    MappingDraftKind kind,
                                    MappingWandSelection selection,
                                    String description,
                                    WorldAdminService worldAdmin) {
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            throw new IllegalArgumentException("World admin este dezactivat.");
        }
        MappingWandSelection safeSelection = selection != null ? selection : MappingWandSelection.empty();
        MappingDraftSuggestion suggestion = MappingIntentParser.suggest(kind, description);
        return switch (kind) {
            case REGION -> createRegionDraft(playerId, safeSelection, description, worldAdmin, suggestion);
            case PLACE -> createPlaceDraft(playerId, safeSelection, description, worldAdmin, suggestion);
            case NODE -> createNodeDraft(playerId, safeSelection, description, worldAdmin, suggestion);
            case NPC_BIND -> createNpcBindDraft(playerId, safeSelection, description, worldAdmin, suggestion);
            case QUEST_ANCHOR -> createQuestAnchorDraft(playerId, safeSelection, description, worldAdmin, suggestion);
        };
    }

    public MappingDraftApplyResult apply(MappingDraft draft, WorldAdminService worldAdmin) {
        if (draft == null) {
            throw new IllegalArgumentException("Nu exista draft de confirmat.");
        }
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            throw new IllegalArgumentException("World admin este dezactivat.");
        }

        return switch (draft.kind()) {
            case REGION -> {
                WorldRegion region = worldAdmin.createRegion(
                    draft.localId(),
                    draft.displayName(),
                    draft.worldName(),
                    RegionType.fromId(draft.typeId()),
                    draft.minX(),
                    draft.minY(),
                    draft.minZ(),
                    draft.maxX(),
                    draft.maxY(),
                    draft.maxZ()
                );
                region.setTags(draft.tags());
                yield new MappingDraftApplyResult(draft.kind(), region.getId(), "Regiune creata");
            }
            case PLACE -> {
                WorldPlace place = worldAdmin.createPlace(
                    draft.regionId(),
                    draft.localId(),
                    draft.displayName(),
                    draft.worldName(),
                    PlaceType.fromId(draft.typeId()),
                    draft.minX(),
                    draft.minY(),
                    draft.minZ(),
                    draft.maxX(),
                    draft.maxY(),
                    draft.maxZ()
                );
                place.setTags(draft.tags());
                Map<String, String> metadata = new LinkedHashMap<>(draft.metadata());
                String publicAccessHint = metadata.remove("public_access_hint");
                if (publicAccessHint != null && !publicAccessHint.isBlank()) {
                    place.setPublicAccess(Boolean.parseBoolean(publicAccessHint));
                }
                metadata.forEach(place::putMetadata);
                yield new MappingDraftApplyResult(draft.kind(), place.getId(), "Place creat");
            }
            case NODE -> {
                WorldNode node = worldAdmin.createNode(
                    draft.regionId(),
                    draft.placeId(),
                    draft.localId(),
                    WorldNodeType.fromId(draft.typeId()),
                    draft.worldName(),
                    draft.x(),
                    draft.y(),
                    draft.z(),
                    draft.radius()
                );
                draft.metadata().forEach(node::putMetadata);
                yield new MappingDraftApplyResult(draft.kind(), node.getId(), "Node creat");
            }
            case NPC_BIND -> throw new IllegalArgumentException("NPC bind se confirma prin comanda, ca sa actualizeze si profilul NPC.");
            case QUEST_ANCHOR -> throw new IllegalArgumentException("Quest anchor se confirma prin comanda, ca sa scrie in quest_anchor_bindings.");
        };
    }

    private MappingDraft createRegionDraft(UUID playerId,
                                           MappingWandSelection selection,
                                           String description,
                                           WorldAdminService worldAdmin,
                                           MappingDraftSuggestion suggestion) {
        MappingWandSelection.MappingBounds bounds = selection.bounds()
            .orElseThrow(() -> new IllegalArgumentException("Selecteaza pos1 si pos2 cu /ainpc wand inainte de draft region."));
        String localId = uniqueRegionId(worldAdmin, suggestion.localId());
        String command = "/ainpc world region create " + localId + " " + suggestion.typeId() + " "
            + bounds.minX() + " " + bounds.minY() + " " + bounds.minZ() + " "
            + bounds.maxX() + " " + bounds.maxY() + " " + bounds.maxZ();
        return new MappingDraft(
            playerId,
            MappingDraftKind.REGION,
            description,
            localId,
            localId,
            suggestion.displayName(),
            suggestion.typeId(),
            localId,
            "",
            bounds.worldName(),
            bounds.minX(),
            bounds.minY(),
            bounds.minZ(),
            bounds.maxX(),
            bounds.maxY(),
            bounds.maxZ(),
            0,
            0,
            0,
            suggestion.radius(),
            suggestion.tags(),
            suggestion.metadata(),
            suggestion.warnings(),
            command
        );
    }

    private MappingDraft createPlaceDraft(UUID playerId,
                                          MappingWandSelection selection,
                                          String description,
                                          WorldAdminService worldAdmin,
                                          MappingDraftSuggestion suggestion) {
        MappingWandSelection.MappingBounds bounds = selection.bounds()
            .orElseThrow(() -> new IllegalArgumentException("Selecteaza pos1 si pos2 cu /ainpc wand inainte de draft place."));
        WorldRegionInfo region = worldAdmin.findRegion(bounds.worldName(), bounds.centerX(), bounds.centerY(), bounds.centerZ());
        if (region == null) {
            throw new IllegalArgumentException("Selectia place nu are o regiune parinte la centru. Creeaza intai regiunea.");
        }

        List<String> warnings = new ArrayList<>(suggestion.warnings());
        if (!region.contains(bounds.worldName(), bounds.minX(), bounds.minY(), bounds.minZ())
            || !region.contains(bounds.worldName(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            warnings.add("Selectia place pare sa iasa din regiunea " + region.id() + "; confirmarea poate fi respinsa.");
        }

        String localId = uniquePlaceId(worldAdmin, region.id(), suggestion.localId());
        String qualifiedId = region.id() + ":" + localId;
        String command = "/ainpc world place create " + region.id() + " " + localId + " " + suggestion.typeId() + " "
            + bounds.minX() + " " + bounds.minY() + " " + bounds.minZ() + " "
            + bounds.maxX() + " " + bounds.maxY() + " " + bounds.maxZ();
        return new MappingDraft(
            playerId,
            MappingDraftKind.PLACE,
            description,
            localId,
            qualifiedId,
            suggestion.displayName(),
            suggestion.typeId(),
            region.id(),
            "",
            region.worldName(),
            bounds.minX(),
            bounds.minY(),
            bounds.minZ(),
            bounds.maxX(),
            bounds.maxY(),
            bounds.maxZ(),
            0,
            0,
            0,
            suggestion.radius(),
            suggestion.tags(),
            suggestion.metadata(),
            warnings,
            command
        );
    }

    private MappingDraft createNodeDraft(UUID playerId,
                                         MappingWandSelection selection,
                                         String description,
                                         WorldAdminService worldAdmin,
                                         MappingDraftSuggestion suggestion) {
        if (!selection.hasPoint()) {
            throw new IllegalArgumentException("Selecteaza un punct cu /ainpc wand point sau click cu wand-ul in modul node.");
        }
        MappingPoint point = selection.point();
        WorldRegionInfo region = worldAdmin.findRegion(point.worldName(), point.x(), point.y(), point.z());
        if (region == null) {
            throw new IllegalArgumentException("Punctul node nu are o regiune parinte. Creeaza intai regiunea.");
        }
        WorldPlaceInfo place = worldAdmin.findPlace(point.worldName(), point.x(), point.y(), point.z());
        String placeId = place != null && place.regionId().equalsIgnoreCase(region.id()) ? place.id() : "";
        String localId = uniqueNodeId(worldAdmin, region.id(), placeId, suggestion.localId());
        String qualifiedId = placeId.isBlank() ? region.id() + ":" + localId : placeId + ":" + localId;
        String placeSelector = placeId.isBlank() ? "-" : placeId;
        String command = "/ainpc world node create " + region.id() + " " + placeSelector + " "
            + localId + " " + suggestion.typeId() + " "
            + point.x() + " " + point.y() + " " + point.z() + " "
            + String.format(Locale.ROOT, "%.1f", suggestion.radius());
        return new MappingDraft(
            playerId,
            MappingDraftKind.NODE,
            description,
            localId,
            qualifiedId,
            suggestion.displayName(),
            suggestion.typeId(),
            region.id(),
            placeId,
            region.worldName(),
            0,
            0,
            0,
            0,
            0,
            0,
            point.x(),
            point.y(),
            point.z(),
            suggestion.radius(),
            suggestion.tags(),
            suggestion.metadata(),
            suggestion.warnings(),
            command
        );
    }

    private MappingDraft createNpcBindDraft(UUID playerId,
                                            MappingWandSelection selection,
                                            String description,
                                            WorldAdminService worldAdmin,
                                            MappingDraftSuggestion suggestion) {
        if (!selection.hasPoint()) {
            throw new IllegalArgumentException("Selecteaza punctul din place cu /ainpc wand point sau click in modul npc_bind.");
        }
        MappingPoint point = selection.point();
        WorldRegionInfo region = worldAdmin.findRegion(point.worldName(), point.x(), point.y(), point.z());
        if (region == null) {
            throw new IllegalArgumentException("Punctul de bind NPC nu are o regiune parinte.");
        }
        WorldPlaceInfo place = worldAdmin.findPlace(point.worldName(), point.x(), point.y(), point.z());
        if (place == null || !place.regionId().equalsIgnoreCase(region.id())) {
            throw new IllegalArgumentException("Punctul de bind NPC trebuie sa fie intr-un place mapat.");
        }

        NpcBindIntent intent = parseNpcBindIntent(description);
        List<String> warnings = new ArrayList<>(suggestion.warnings());
        warnings.add("Confirmarea actualizeaza profilul NPC, metadata mapping si npc_world_bindings.");
        if (!roleMatchesPlace(intent.role(), place)) {
            warnings.add("Place-ul " + place.id() + " nu pare potrivit pentru rolul " + intent.role() + ".");
        }

        Map<String, String> metadata = new LinkedHashMap<>(suggestion.metadata());
        metadata.put("npc_selector", intent.npcSelector());
        metadata.put("bind_role", intent.role());
        metadata.put("place_id", place.id());
        metadata.put("source", "wand_prompt");

        String qualifiedId = intent.npcSelector() + "@" + place.id() + ":" + intent.role();
        return new MappingDraft(
            playerId,
            MappingDraftKind.NPC_BIND,
            description,
            intent.npcSelector(),
            qualifiedId,
            "NPC Bind " + intent.role(),
            intent.role(),
            region.id(),
            place.id(),
            region.worldName(),
            0,
            0,
            0,
            0,
            0,
            0,
            point.x(),
            point.y(),
            point.z(),
            suggestion.radius(),
            suggestion.tags(),
            metadata,
            warnings,
            "/ainpc map confirm"
        );
    }

    private MappingDraft createQuestAnchorDraft(UUID playerId,
                                                MappingWandSelection selection,
                                                String description,
                                                WorldAdminService worldAdmin,
                                                MappingDraftSuggestion suggestion) {
        if (!selection.hasPoint()) {
            throw new IllegalArgumentException("Selecteaza punctul de quest anchor cu /ainpc wand point sau click in modul quest_anchor.");
        }
        MappingPoint point = selection.point();
        WorldRegionInfo region = worldAdmin.findRegion(point.worldName(), point.x(), point.y(), point.z());
        if (region == null) {
            throw new IllegalArgumentException("Punctul quest anchor nu are o regiune parinte.");
        }

        WorldNodeInfo node = worldAdmin.findNode(point.worldName(), point.x(), point.y(), point.z());
        WorldPlaceInfo place = worldAdmin.findPlace(point.worldName(), point.x(), point.y(), point.z());
        if (place != null && !place.regionId().equalsIgnoreCase(region.id())) {
            place = null;
        }

        String anchorType;
        String anchorId;
        String anchorLabel;
        if (node != null) {
            anchorType = "node";
            anchorId = node.id();
            anchorLabel = node.typeId();
        } else if (place != null) {
            anchorType = "place";
            anchorId = place.id();
            anchorLabel = place.displayName();
        } else {
            anchorType = "region";
            anchorId = region.id();
            anchorLabel = region.name();
        }

        QuestAnchorIntent intent = parseQuestAnchorIntent(
            description,
            defaultObjectiveTypeForAnchor(anchorType),
            anchorId
        );
        List<String> warnings = new ArrayList<>(suggestion.warnings());
        warnings.add("Confirmarea scrie/actualizeaza quest_anchor_bindings pentru obiectivul ales.");
        if (!questAnchorTypeCompatible(intent.objectiveType(), anchorType)) {
            warnings.add("Tipul obiectivului " + intent.objectiveType()
                + " nu pare compatibil cu ancora " + anchorType + ".");
        }

        Map<String, String> metadata = new LinkedHashMap<>(suggestion.metadata());
        metadata.put("player_selector", intent.playerSelector());
        metadata.put("progression_selector", intent.progressionSelector());
        metadata.put("objective_key", intent.objectiveKey());
        metadata.put("objective_type", intent.objectiveType());
        metadata.put("reference", intent.reference());
        metadata.put("anchor_type", anchorType);
        metadata.put("anchor_id", anchorId);
        metadata.put("anchor_label", anchorLabel);
        metadata.put("source", "wand_prompt");

        String localId = MappingIntentParser.slugOrFallback(intent.objectiveKey(), "quest_anchor");
        String qualifiedId = intent.progressionSelector() + ":" + intent.objectiveKey()
            + "@" + anchorType + ":" + anchorId;
        return new MappingDraft(
            playerId,
            MappingDraftKind.QUEST_ANCHOR,
            description,
            localId,
            qualifiedId,
            "Quest Anchor " + intent.objectiveKey(),
            intent.objectiveType(),
            region.id(),
            place != null ? place.id() : "",
            region.worldName(),
            0,
            0,
            0,
            0,
            0,
            0,
            point.x(),
            point.y(),
            point.z(),
            suggestion.radius(),
            suggestion.tags(),
            metadata,
            warnings,
            "/ainpc map confirm"
        );
    }

    private String uniqueRegionId(WorldAdminService worldAdmin, String baseId) {
        return uniqueId(baseId, candidate -> worldAdmin.getRegion(candidate) == null);
    }

    private String uniquePlaceId(WorldAdminService worldAdmin, String regionId, String baseId) {
        return uniqueId(baseId, candidate -> worldAdmin.getPlace(regionId + ":" + candidate) == null);
    }

    private String uniqueNodeId(WorldAdminService worldAdmin, String regionId, String placeId, String baseId) {
        String scope = placeId == null || placeId.isBlank() ? regionId : placeId;
        return uniqueId(baseId, candidate -> {
            WorldNodeInfo existing = worldAdmin.getNode(scope + ":" + candidate);
            return existing == null;
        });
    }

    private String uniqueId(String baseId, java.util.function.Predicate<String> available) {
        String safeBase = MappingIntentParser.slugOrFallback(baseId, "mapping_draft");
        if (available.test(safeBase)) {
            return safeBase;
        }
        int index = 2;
        while (!available.test(safeBase + "_" + index)) {
            index++;
        }
        return safeBase + "_" + index;
    }

    private NpcBindIntent parseNpcBindIntent(String description) {
        String normalized = MappingIntentParser.normalize(description);
        String[] tokens = normalized.isBlank() ? new String[0] : normalized.split("\\s+");
        String role = "home";
        String npcSelector = "";
        for (String token : tokens) {
            String resolvedRole = resolveBindRole(token);
            if (!resolvedRole.isBlank()) {
                role = resolvedRole;
                continue;
            }
            if (isNpcBindFiller(token)) {
                continue;
            }
            if (npcSelector.isBlank()) {
                npcSelector = token;
            }
        }
        if (npcSelector.isBlank()) {
            npcSelector = "nearest";
        }
        return new NpcBindIntent(npcSelector, role);
    }

    private String resolveBindRole(String token) {
        return switch (token == null ? "" : token.toLowerCase(Locale.ROOT)) {
            case "home", "casa", "acasa", "locuinta", "resident", "residence" -> "home";
            case "work", "lucru", "munca", "job", "workplace", "atelier" -> "work";
            case "social", "piata", "taverna", "meeting", "adunare" -> "social";
            default -> "";
        };
    }

    private boolean isNpcBindFiller(String token) {
        return switch (token == null ? "" : token.toLowerCase(Locale.ROOT)) {
            case "npc", "bind", "leaga", "legare", "la", "in", "pe", "pentru", "place", "loc" -> true;
            default -> false;
        };
    }

    private boolean roleMatchesPlace(String role, WorldPlaceInfo place) {
        return switch (role) {
            case "home" -> "house".equalsIgnoreCase(place.placeType().getId()) || place.hasTag("home");
            case "work" -> place.hasTag("work") || place.hasTag("workplace")
                || switch (place.placeType()) {
                    case FORGE, SHOP, FARM, TAVERN, CAMP -> true;
                    default -> false;
                };
            case "social" -> place.hasTag("social") || place.hasTag("public")
                || switch (place.placeType()) {
                    case MARKET, TAVERN, CAMP -> true;
                    default -> false;
                };
            default -> true;
        };
    }

    private QuestAnchorIntent parseQuestAnchorIntent(String description,
                                                     String defaultObjectiveType,
                                                     String defaultReference) {
        String safeDescription = description == null ? "" : description.trim();
        String[] tokens = safeDescription.isBlank() ? new String[0] : safeDescription.split("\\s+");
        int index = 0;
        String playerSelector = "self";
        if (tokens.length > 0) {
            String playerToken = stripKeyPrefix(tokens[0], "player", "jucator");
            if (!playerToken.isBlank()) {
                playerSelector = playerToken;
                index = 1;
            }
        }

        if (index >= tokens.length) {
            throw new IllegalArgumentException("Utilizare: /ainpc map quest_anchor [player:<jucator|uuid>] <tracked|current|templateId|questCode> <objective_id> [objective_type] [reference]");
        }
        String progressionSelector = stripKeyPrefix(tokens[index++], "progression", "quest", "template");
        if (progressionSelector.isBlank()) {
            throw new IllegalArgumentException("Specifica progresia pentru quest_anchor.");
        }
        if (index >= tokens.length) {
            throw new IllegalArgumentException("Specifica objective_id pentru quest_anchor.");
        }

        String objectiveKey = stripKeyPrefix(tokens[index++], "objective", "objective_id", "obiectiv");
        if (objectiveKey.isBlank()) {
            throw new IllegalArgumentException("objective_id pentru quest_anchor este gol.");
        }

        String objectiveType = defaultObjectiveTypeForAnchor(defaultObjectiveType);
        String reference = defaultReference == null ? "" : defaultReference.trim();
        if (index < tokens.length) {
            String maybeType = normalizeQuestAnchorObjectiveType(tokens[index]);
            if (!maybeType.isBlank()) {
                objectiveType = maybeType;
                index++;
            }
        }
        if (index < tokens.length) {
            reference = joinTokens(tokens, index);
        }

        return new QuestAnchorIntent(
            playerSelector,
            progressionSelector,
            objectiveKey,
            objectiveType,
            reference
        );
    }

    private String stripKeyPrefix(String token, String... prefixes) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String trimmed = token.trim();
        int separator = trimmed.indexOf(':');
        if (separator <= 0 || separator >= trimmed.length() - 1) {
            separator = trimmed.indexOf('=');
        }
        if (separator <= 0 || separator >= trimmed.length() - 1) {
            return trimmed;
        }
        String prefix = trimmed.substring(0, separator).toLowerCase(Locale.ROOT);
        for (String expected : prefixes) {
            if (prefix.equals(expected.toLowerCase(Locale.ROOT))) {
                return trimmed.substring(separator + 1).trim();
            }
        }
        return trimmed;
    }

    private String normalizeQuestAnchorObjectiveType(String token) {
        String normalized = MappingIntentParser.normalize(token).replace('-', '_');
        return switch (normalized) {
            case "visit_region", "region", "enter_region" -> "visit_region";
            case "visit_place", "place", "enter_place", "go_to_place" -> "visit_place";
            case "inspect_node", "node", "inspect", "interact_node" -> "inspect_node";
            case "talk_to_npc", "npc", "talk", "speak_to_npc" -> "talk_to_npc";
            default -> "";
        };
    }

    private String defaultObjectiveTypeForAnchor(String anchorType) {
        return switch (anchorType == null ? "" : anchorType.toLowerCase(Locale.ROOT)) {
            case "region", "visit_region" -> "visit_region";
            case "place", "visit_place" -> "visit_place";
            case "node", "inspect_node" -> "inspect_node";
            case "npc", "talk_to_npc" -> "talk_to_npc";
            default -> "inspect_node";
        };
    }

    private boolean questAnchorTypeCompatible(String objectiveType, String anchorType) {
        String objective = normalizeQuestAnchorObjectiveType(objectiveType);
        String anchor = anchorType == null ? "" : anchorType.trim().toLowerCase(Locale.ROOT);
        if (objective.isBlank() || anchor.isBlank()) {
            return true;
        }
        return switch (objective) {
            case "visit_region" -> "region".equals(anchor);
            case "visit_place" -> "place".equals(anchor);
            case "inspect_node" -> "node".equals(anchor);
            case "talk_to_npc" -> "npc".equals(anchor);
            default -> true;
        };
    }

    private String joinTokens(String[] tokens, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int index = Math.max(0, startIndex); index < tokens.length; index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(tokens[index]);
        }
        return builder.toString();
    }

    private record NpcBindIntent(String npcSelector, String role) {
    }

    private record QuestAnchorIntent(
        String playerSelector,
        String progressionSelector,
        String objectiveKey,
        String objectiveType,
        String reference
    ) {
    }
}
