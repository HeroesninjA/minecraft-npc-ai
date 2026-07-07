# Analiza Stare Dezvoltare AINPC

**Data:** 2026-07-05
**Stadiu General:** ~87% complet

---

## 1. NPC System — 90%

### Implementat
- NPCManager complet (77.5KB): CRUD, DB persistare, duplicate detection, NPCs by UUID/world/source key
- AINPC class (16.8KB): Personality Big Five, emotions Plutchik, 5 nevoi (foame/energie/social/confort/siguranta), anchors home/work/social, spawntracking
- AINPC sub-module: NPCContext, NPCState, NPCAction (40 de tipuri)
- EmotionManager (10.5KB): Decay, apply, particle effects, async persistence
- MemoryManager (14.6KB): create/recall/forget/clear/search cu tip, impact, expirare
- FamilyManager (16.6KB): generate/adauga/get/backstory cu probabilitati configurabile
- NPC spawning: NpcSpawnOrchestrator, NpcSpawnPlan, PopulationPlan, SpawnBatchTracker, HouseholdPersistenceService, HouseAllocationPlanner
- NPC profiles: DB schema, auto-generare, profile_source/summary/data
- NPC world bindings: NpcWorldBindingService
- NPC villager lookup si anchors

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🔴 | **RoutineCoordinator** — nu exista deloc | NPC-urile nu au rutine zilnice, stau pe loc |
| 🔴 | **RelationshipService** — `NPCRelationship` e doar data class (10linii) | Fara progresie/decay/management relatii |
| 🟡 | **RoutineService** exista dar e simplu | Functional minimal, fara coordonare avansata |

---

## 2. Quest System — 95%

### Implementat
- ScenarioEngine (147.9KB, 3090linii): Engine central complet — lifecycle (offer, accept, progress, complete, fail, abandon), objective tracking, stage progression, NPC actors, rewards, story events
- 12 Objective Handlers: BreakBlock, CollectItem, CraftItem, DeliverToNpc, EquipItem, InspectNode, KillMob, PlaceBlock, TalkToNpc, UseItem, VisitPlace, VisitRegion
- Runtime triggers: PlayerEntersRegion/Place/Node, PlayerTalksToNpc, PlayerUsesItem
- Runtime actions: GiveItem, TeleportPlayer, SendMessage, PlaySound, ExecuteCommand, SetStoryState, RecordStoryEvent
- Runtime conditions: HasCompletedQuest, MechanicLimit, QuestCooldown, QuestPrerequisite
- Quest persistence: QuestProgressPersistenceService, cleanup, tracking, lifecycle
- Quest authoring: QuestAuthoringService, QuestDraftValidator, QuestDraftExporter
- Quest tracking: marker, metrics, filter, log view, support service

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟢 | Template selection edge cases | Minor, functioneaza corect in cazuri normale |

---

## 3. World Mapping — 90%

### Implementat
- WorldAdminService (57.8KB): CRUD regiuni/places/nodes cu cache, dirty tracking, auto-indexing
- WorldRegion, WorldPlace, WorldNode: Modele complete cu coordonate, tipuri, metadata
- MappingIndex: Index spatial pe chunk-uri (O(1) lookup)
- MappingWandService: Wand in-game cu pos1/pos2, draft creation, particule
- MappingDraftFactory: Creare draft-uri din selectii wand
- SemanticVillageMapper: Scanare sate vanilla, import semantic
- VanillaVillageScanner: Detectare features sat
- VillagePatchSystem: Planner/Applier/GapAnalyzer pentru modificari sat
- ExteriorStructureSystem: Analyzer/Planner/BlueprintCatalog
- ControlledTestWorldFixture: Planner/Populator/Applier/Validator
- BuildModeListener: Sign parsing, point mode, GUI triggers

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟡 | Auto-indexing fara world listeners | Indexul nu se actualizeaza la incarcare/descarcare lumi |
| 🟢 | WorldListener minimal (0.4KB) | Ar putea fi extins |

---

## 4. Economy & Shops — 70%

### Implementat
- EconomyService (2.6KB): Balance management, deposit/withdraw/transfer
- ShopService (5.2KB): Inregistrare magazine pe rol NPC, canAfford, executePurchase
- ShopOffer, NpcShopDefinition: Modele pentru oferte
- Economy commands: balance, pay, set, top
- Shop GUI

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟠 | **JSON persistence** in loc de DB | Balantele se pierd la crash |
| 🟡 | **Vault integration** | Pluginuri externe nu vad balantele |
| 🟢 | Banking/investment mechanics | Necesar doar pentru gameplay avansat |

---

## 5. AI / OpenAI — 90%

### Implementat
- OpenAIService (16.4KB): Client OpenAI Responses API, configurable, timeout, retry, offline fallback
- OpenAIConnectionProbe: Proba conexiune si disponibilitate model
- OpenAIPromptBuilder: Build prompts cu context NPC/lume/istoric
- OpenAISemanticWorldContextBuilder: Context semantic pentru AI
- DialogManager: Dialog jucator-NPC complet cu cooldown
- AIOrchestrationService: Retry, fallback, freeze detection, confidence, policy
- AIResponseValidator: Validare cu safety labels
- AISuggestionService: Sugestii AI pentru quest/story/build
- Debug: OpenAIDebugSnapshot, PromptSnapshot, DebugGui status

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟢 | Un singur model suportat | Major, dar configurabil |
| 🟢 | Fara streaming response | Acceptabil pentru NPC dialog |

---

## 6. Progression — 90%

### Implementat
- ProgressionService (23.6KB): Definitions cache, quest log/snapshots, filters
- PlayerProgressionService: Level, XP, skills, top players
- ProgressionRepository: DB persistence
- ProgressionDefinition: Criteria, rewards, stages
- ProgressionFilter/Selector: Filtrare si selectie definitii
- Progression GUI: Entry, Snapshot, Stage, Status, Objective, Progress

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟢 | Definitii standalone (acum din feature packs) | Functional |

---

## 7. Story System — 85%

### Implementat
- StoryStateService (21.7KB): Stare persistenta regiuni/places, variabile, story pools, moduri, evenimente
- StoryContextService (20.4KB): Build context snapshots, colecteaza story signals
- StoryEvent, StoryContextSnapshot, RegionStoryState, PlaceStoryState
- StructureStoryEventPlanner: Planifica evenimente din structuri
- StoryStructureSignalResolver: Rezolva semnale din structuri
- StoryActionValidator: Validare actiuni

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟡 | **Story authoring tools** | Evenimentele story sunt doar din quest-uri si structuri |
| 🟢 | Mode switching logic basic | Functional dar extensibil |

---

## 8. MCP Integration — 75%

### Implementat
- HttpMcpRuntimeClient (9.4KB): Client HTTP MCP cu circuit breaker
- RuntimeSnapshot + RuntimeSnapshotProducer: Snapshot periodic
- McpDialogContextProvider: Context pentru dialog
- AdminMcpGui: GUI monitorizare
- Modul extern `ainpc-mcp-service`: 30 fisiere Java (Spring Boot + bridge)

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟠 | **MCP tools** — doar 2 implementate | Functional minimal |
| 🟢 | Nu e suportat SSE | Acceptabil pentru arhitectura curenta |

---

## 9. GUI System — 95%

### Implementat
- 37 ecrane, TOATE implementate (0 stub-uri):
  - Hub-uri: Main, Player, Admin, Creator
  - Quest: Log, Detail, Offer, Authoring, Map, Edit, Create, Quick, Creator, Test
  - World: Hub, Place, Region, Mapping, Creator
  - NPC: Interact, Manager, Routine, Shop
  - Story, Stats, Audit, Debug, MCP, Confirm
- GuiService (30.2KB): Session management, navigation, text input, quest editor
- Suport: GuiRenderContext, GuiButton, GuiAction, GuiItemFactory, GuiNavigation

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟢 | Nimic semnificativ | Sistemul e complet |

---

## 10. Addon System — 85%

### Implementat
- AddonRegistry: Gestiune descriptor/addon dupa ID/tip, validare
- AddonDependencyResolver: Rezolvare dependinte, detectare cicluri, sortare topologica
- FeaturePackLoader (~1000linii): Incarcare YAML, parsare traits/profesii/dialoguri/scenarii
- FeaturePackMetadataValidator si DependencyValidator

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟡 | Fara hot-reload (necesita restart sau `/ainpc reload`) | Acceptabil |
| 🟢 | Fara addon store/download | Necesar doar pentru distributie |

---

## 11. Debug & Audit — 95%

### Implementat
- DebugDumpService: Orchestreaza 38 module de debug dump
- 38 module: NPC, World, Mapping, Quest, Story, Progression, Economy, Authoring, Server, Config, Audit, Secrets, Routing, RecentEvents
- RecentEventsBuffer: Filtrare evenimente AINPC
- DebugDumpAudit: Audit NPC complet (homeAnchor, workAnchor, profile, ocupatie)
- AuditGui, DebugGui
- Health check in debug dumps

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟢 | Endpoint health check dedicat | Functional prin comenzi |

---

## 12. Demo Content — 90%

### Implementat
- 17 quest-uri in `medieval_quest.yml` (2149linii):
  - Q01-Q08: questuri principale si secundare complete
  - C01-C02: contracte
  - D01: datorie NPC
  - B01-B02: bounty-uri
  - E01: eveniment mondial
  - T01: tutorial
  - R01: ritual
  - Q100: quest demonstrativ cu toate tipurile de recompense
- Tutorial_Demo.yml: TD01-TD05+ lant complet
- 7 pachete in total: medieval, medieval_quest, social, festival, wilderness, tutorial_demo, sample_objectives_quest
- Profesii, trasaturi, dialoguri, topologii

### Ce lipseste
| # | Componenta | Impact |
|---|-----------|--------|
| 🟡 | Pachetele festival si wilderness nu au fost verificate complet | Continut aditional |
| 🟢 | Fara skin-uri/modele NPC custom | Cosmetic |

---

## Rezumat Final

| Modul | Stare | Linii cod | Prioritati ramase |
|-------|-------|-----------|-------------------|
| NPC System | 90% | ~4000 | 🔴 2 missing-uri majore |
| Quest System | 95% | ~5000 | 🟢 Minor |
| World Mapping | 90% | ~4000 | 🟡 1 missing mediu |
| Economy & Shops | 70% | ~300 | 🟠 1 missing, 🟡 1 missing |
| AI / OpenAI | 90% | ~1500 | 🟢 Minor |
| Progression | 90% | ~2000 | Completa functional |
| Story System | 85% | ~1500 | 🟡 1 missing |
| MCP Integration | 75% | ~30 fisiere | 🟠 Tools limitate |
| GUI System | 95% | ~8000 | Completa |
| Addon System | 85% | ~1500 | 🟡 Fara hot-reload |
| Debug & Audit | 95% | ~2000 | Completa |
| Demo Content | 90% | ~3000 YAML | 🟡 Pachete neverificate |

## Top 3 Prioritati de Implementat

| # | Ce | Efort | Impact |
|---|----|-------|--------|
| 1 | **RoutineCoordinator** — NPC-uri cu rutine zilnice | 2-3 zile | NPC-urile devin dinamice |
| 2 | **RelationshipService** — relatii NPC-NPC | 1-2 zile | NPC-urile interactioneaza intre ele |
| 3 | **Economy DB persistence** — SQLite in loc de JSON | 1 zi | Balante sigure la crash |
