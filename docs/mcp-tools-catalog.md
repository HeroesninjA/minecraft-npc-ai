# MCP Tools Catalog — Actual și Propus

Actualizat: 2026-06-28

## Tool-uri Existente (toate în `ainpc-mcp-service`)

Acestea sunt clasele `@McpTool` din `ro.ainpc.mcp.tools.*`.

| # | Nume tool | Clasă | Status | Conținut |
|---|---|---|---|---|
| 1 | `ainpc.ping` | `AinpcPingTools` | ✅ static | Ping hardcodat `status=ok` |
| 2 | `ainpc.feature.state` | `AinpcFeatureStateTools` | ✅ static | Hardcoded, `runtimeBridge.enabled=false` |
| 3 | `ainpc.debug.health` | `AinpcDebugHealthTools` | ✅ static | Hardcoded `status=UP` |
| 4 | `ainpc.semantic.context` | `AinpcSemanticContextTools` | ✅ schema-only | 4 blocuri WORLD (fără date reale) |
| 5 | `ainpc.semantic.context.summary` | `AinpcSemanticContextTools` | ✅ schema-only | Rezumat WORLD |
| 6 | `ainpc.quest.semantic.context` | `AinpcSemanticContextTools` | ✅ schema-only | 3 blocuri QUEST |
| 7 | `ainpc.quest.semantic.context.summary` | `AinpcSemanticContextTools` | ✅ schema-only | Rezumat QUEST |
| 8 | `ainpc.quest.authoring.context.summary` | `AinpcSemanticContextTools` | ✅ schema-only | Rezumat QUEST_AUTHORING |
| 9 | `ainpc.mapping.semantic.context` | `AinpcSemanticContextTools` | ✅ schema-only | 3 blocuri MAPPING |
| 10 | `ainpc.mapping.semantic.context.summary` | `AinpcSemanticContextTools` | ✅ schema-only | Rezumat MAPPING |
| 11 | `ainpc.story.semantic.context` | `AinpcSemanticContextTools` | ✅ schema-only | 3 blocuri STORY |
| 12 | `ainpc.story.semantic.context.summary` | `AinpcSemanticContextTools` | ✅ schema-only | Rezumat STORY |
| 13 | `ainpc.routing.semantic.context` | `AinpcSemanticContextTools` | ✅ schema-only | 5 blocuri ROUTING |
| 14 | `ainpc.routing.semantic.context.summary` | `AinpcSemanticContextTools` | ✅ schema-only | Rezumat ROUTING |
| 15 | `ainpc.semantic.routing.summary` | `AinpcSemanticContextTools` | ✅ schema-only | Rezumat global routing |

**Total: 15 tool-uri**, dintre care:
- 3 cu date statice goto / diagnostic
- 12 cu doar scheme / contracte, fără date reale

## Tool-uri Propuse (Faza 3 + 4)

| # | Nume tool | Prioritate | Conținut | Sursă date |
|---|---|---|---|---|
| 16 | `ainpc.server.snapshot` | P1 | Rezumat server: version, NPC count, addon count, world counts | Snapshot file |
| 17 | `ainpc.npc.list` | P1 | Listă NPC-uri cu filtre (regiune, profesie, max 20) | Snapshot file |
| 18 | `ainpc.npc.context` | P1 | Context 1 NPC: identitate, profesie, rutină, relații | Snapshot file |
| 19 | `ainpc.quest.summary` | P1 | Progres curent pentru selector (player/template) | Snapshot file |
| 20 | `ainpc.world.mapping.summary` | P1 | Rezumat region/place/node, semantic index | Snapshot file |
| 21 | `ainpc.dialog.context` | P1 | Snapshot dialog NPC ↔ player (context complet) | Snapshot file |
| 22 | `ainpc.story.summary` | P2 | Rezumat story state regiuni + evenimente recente | Snapshot file |
| 23 | `ainpc.reputation.summary` | P2 | Rezumat reputație per regiune | Snapshot file |
| 24 | `ainpc.debug.bridge` | P2 | Status bridge: file age, cache hit/miss, errors | SnapshotReader |

## Consolidare Propusă (Pasul 5)

Un tool unificat (backend) care înlocuiește tool-urile 4–15 (12 tool-uri devin 1):

```
ainpc.semantic.context(domain?, summary?)
```

- `domain` (string, optional): `world` | `quest` | `quest_authoring` | `mapping` | `story` | `routing` / default: toate
- `summary` (boolean, optional): `true` = rezumat / `false` = full context

### Backward compat

Tool-urile individuale rămân active ca aliasuri până la următorul release major. Codul shared e în `SemanticContextService` care e chemat și de tool-ul unificat.

## Mapping Actual → Propus

| Actual (15 tool-uri) | Propus (cu consolidare) | Status |
|---|---|---|
| `ainpc.ping` | `ainpc.ping` | Păstrat |
| `ainpc.feature.state` | `ainpc.feature.state` | Păstrat |
| `ainpc.debug.health` | `ainpc.debug.health` | De îmbunătățit (Pasul 4) |
| 12 semantic + routing | `ainpc.semantic.context(domain, summary)` | De consolidat (Pasul 5) |
| `ainpc.server.snapshot` | `ainpc.server.snapshot` | ✅ Live (Pasul 2) |
| `ainpc.npc.list` | `ainpc.npc.list` | ✅ Live (Pasul 2) |
| `ainpc.npc.context` | `ainpc.npc.context` | ✅ Live (Pasul 2) |
| `ainpc.quest.summary` | `ainpc.quest.summary` | ✅ Live (Pasul 2) |
| `ainpc.world.mapping.summary` | `ainpc.world.mapping.summary` | ✅ Live (Pasul 2) |
| `ainpc.dialog.context` | `ainpc.dialog.context` | ✅ Live (Pasul 2) |
| 16 | `ainpc.server.snapshot` | `AinpcServerSnapshotTools` | ✅ live | Rezumat server (version, NPC count, features, mapping) |
| 17 | `ainpc.npc.list` | `AinpcNpcTools` | ✅ live | Listă NPC-uri cu mostre și byRegion |
| 18 | `ainpc.npc.context` | `AinpcNpcTools` | ✅ live | Context 1 NPC după nume |
| 19 | `ainpc.quest.summary` | `AinpcQuestSnapshotTools` | ✅ live | Rezumat questuri active |
| 20 | `ainpc.world.mapping.summary` | `AinpcWorldMappingTools` | ✅ live | Rezumat region/place/node |
| 21 | `ainpc.dialog.context` | `AinpcQuestSnapshotTools` | ✅ live | Snapshot dialog NPC+player |
| — | `ainpc.story.summary` | Nou (P2) |
| — | `ainpc.reputation.summary` | Nou (P2) |
| — | `ainpc.debug.bridge` | Nou (P2) |

**Total propus: ~10 tool-uri** (față de 15 actuale), toate cu date reale și clasificate.
