# Analiza Stare Dezvoltare AINPC v2

**Data:** 2026-07-07 (v2.1 — actualizată)
**Stadiu General Estimat:** ~95% complet

---

## Sumar Executiv

| Modul | Stare | Linii cod | Fișiere | Prioritate |
|-------|-------|-----------|---------|------------|
| NPC System | 97% | ~300k | 58 | Minor |
| Quest System | 96% | ~652k | 97 | Minor |
| World Mapping | 95% | ~330k | 52 | Minor |
| Economy & Shops | 92% | ~26k | 10 | Minor |
| AI / OpenAI | 94% | ~190k | 30 | Minor |
| Progression | 92% | ~135k | 17 | Minor |
| Story System | 95% | ~95k | 11 | Minor |
| MCP Integration | 90% | ~140k | 42 | Minor |
| GUI System | 96% | ~538k | 55 | Minor |
| Addon System | 92% | ~19k | 3 | Minor |
| Debug & Audit | 96% | ~280k | 36 | Minor |
| Commands | 96% | ~845k | 17 | Minor |
| **Total** | **~95%** | **~3.55MB** | **~428** | |

---

## 1. NPC System — 97% (58 fișiere, ~300KB)

### Arhitectură
```
NPCManager (79KB) → gestiune CRUD, UUID, source keys, entități
  ├── NPCManagerDB → persistare SQL
  ├── NPCManagerAnchors → ancore home/work/social
  ├── NPCManagerText → formatare ieșire
  ├── NPCManagerVillagerLookup → lookup villager
  └── FamilyManager (17KB) → relații de familie
AINPC (17KB) → model NPC principal
  ├── NPCState (110 stări cu priorități)
  ├── NPCEmotions (10KB) → Plutchik, dominant
  ├── NPCPersonality (7.5KB) → Big Five
  ├── NPCAction (40+ tipuri)
  └── NPCContext (17KB) → context runtime
EmotionManager (10.7KB) → decay, aplicare, particule
MemoryManager (15KB) → creare/recall/search/forget
RoutineService (7.7KB) → tick rutine zilnice
RoutineCoordinator (5KB) → orchestrator rutine + grupuri + adunări sociale
SocialCoordinator (3.2KB) → grupuri sociale NPC
RelationshipService (9.5KB) → relații NPC-NPC cu DB
NpcEconomyService (8KB) → conturi bancare NPC + salarii
NpcEntityAdapter (8KB) → adaptoare entități cu profesii + biomes + vârstă
NPCNameGenerator (8KB) → 160+100 nume + 110 nume de familie
```

### Implementat complet
- CRUD NPC cu DB persistence (NPCManager 79KB)
- Personalitate Big Five, emoții Plutchik, 5 nevoi
- 40+ tipuri de acțiuni NPC
- Rutine zilnice complete (RoutineEngine, RoutineService, RoutineCoordinator)
- Coordonare socială (SocialCoordinator)
- Relații NPC-NPC (RelationshipService cu DB, decay, progresie) ✅ **NOU**
- Group activities — detectare adunări sociale + comandă `/ainpc routine gatherings` ✅ **NOU**
- Economie NPC — conturi bancare, salarii pe ocupație + comandă `/ainpc economy npc` ✅ **NOU**
- Memorie cu tip, impact, expirare configurabil per tip ✅ **NOU**
- Familie (FamilyManager 17KB)
- Spawning orchestrat (NpcSpawnOrchestrator 29KB)
- Auto-generare settlement (AutoSettlementGenerator)
- NPC entități cu profesii, biomes, vârstă, nume vizibil ✅ **NOU**
- Generator nume expandat: 160 masculine + 100 feminine + 110 nume de familie ✅ **NOU**
- ~28,600 combinații nume unice
- 3 fișiere test noi

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | NPC relationship GUI dedicată | Minor | 1 zi |

---

## 2. Quest System — 96% (97 fișiere, 652KB)

### Arhitectură
```
ScenarioEngine (151KB) → engine central quest
  ├── 12 Objective Handler-e
  ├── 5 Runtime Trigger-e
  ├── 7 Runtime Action-e
  ├── 4 Runtime Condition-e
  ├── 5 Runtime Registry-uri
  ├── QuestDraftValidator + Exporter
  ├── QuestAuthoringService
  ├── QuestProgressPersistenceService
  ├── QuestAnchorResolver (20KB)
  └── QuestDirector (2.8KB)
ProgressionService (24KB) → definiții, cache, interogări
```

### Implementat complet
- Lifecycle complet: offer → accept → progress → complete → fail → abandon
- 12 tipuri obiective: BreakBlock, CollectItem, CraftItem, DeliverToNpc, EquipItem, InspectNode, KillMob, PlaceBlock, TalkToNpc, UseItem, VisitPlace, VisitRegion
- Runtime triggers: PlayerEntersRegion/Place/Node, PlayerTalksToNpc, PlayerUsesItem
- Runtime actions: GiveItem, TeleportPlayer, SendMessage, PlaySound, ExecuteCommand, SetStoryState, RecordStoryEvent
- Runtime conditions: HasCompletedQuest, MechanicLimit, QuestCooldown, QuestPrerequisite
- Stage progression cu phase/stage
- Multi-quest per jucător
- Quest tracking, anchoring, audit
- Progression: definiții, filtre, selectoare, snapshot-uri, GUI
- 6 mecanici: side_quests, quest, village_contracts, npc_duties, local_bounties, village_events, onboarding, village_rituals

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | Template selection edge cases | Minor | 0.5 zi |
| 🟢 | NPC quest offering prioritization | Minor | 1 zi |

---

## 3. World Mapping — 95% (52 fișiere, 330KB)

### Arhitectură
```
WorldAdminService (60KB) → CRUD regiuni/places/nodes
  ├── MappingIndex → index spațial pe chunk-uri
  ├── RegionIdentityProvider
  ├── NpcWorldBindingService (7.8KB)
  └── reloadFromConfig()
MappingWandService (17KB) → wand în joc
MappingDraftFactory (27KB) → draft-uri din selecții
SemanticVillageMapper (22KB) → scanare sate vanilla
VanillaVillageScanner (4KB) → detectare features
VillagePatchPlanner (9KB) → planificare modificări
ExteriorStructureAnalyzer (19KB) → structuri exterioare
ControlledTestWorldFixture (32KB) → fixture test
```

### Implementat complet
- CRUD regiuni/places/nodes cu cache și dirty tracking
- Index spațial O(1) pe chunk-uri
- Wand în joc cu pos1/pos2, draft creation, particule
- Scanare sate vanilla cu import semantic
- Village patch planning și aplicare
- Structuri exterioare: analyzer, planner, blueprint catalog
- Auto-indexare la WorldLoad/WorldUnload
- 52 fișiere mapping
- 28+ fișiere test

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | Dynamic world discovery (new worlds) | Minor | 0.5 zi |
| 🟢 | Building template auto-placement | Minor | 2 zile |

---

## 4. Economy & Shops — 92% (10 fișiere, 26KB)

### Implementat
- EconomyService (3.8KB) — balanțe jucători, DB persistence (SQLite/MySQL) ✅ **MIGRAT JSON→DB**
- ShopService (6.3KB) — înregistrare magazine, canAfford, executePurchase/sell
- ShopOffer, NpcShopDefinition, ShopCurrency
- VaultEconomyHook (3.2KB) — integrare Vault prin Proxy reflection ✅ **NOU**
- NpcEconomyService (8KB) — conturi bancare NPC, salarii pe ocupație, plată la muncă ✅ **NOU**
- Comenzi: balance, pay, set, top, npc ✅ **NOU**
- Shop GUI

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | Banking/investment mechanics | Minor | 2 zile |
| 🟢 | Item value rating (economy item pricing) | Minor | 1 zi |

---

## 5. AI / OpenAI — 92% (29 fișiere, 186KB)

### Implementat
- OpenAIService (25.5KB) — client Responses API, configurable, timeout, retry, offline fallback
- OpenAIPromptBuilder (10.4KB) — build prompt-uri cu context NPC/lume/istoric
- AIOrchestrationService (12KB) — orchestrator cu retry, fallback, freeze detection
- AIResponseValidator (11KB) — validare cu safety labels
- OllamaService (5.2KB) — suport modele locale Ollama
- DialogManager (21KB) — dialog jucător-NPC complet cu cooldown
- DialogueEngine (19KB) — engine dialog cu 30 de intent-uri
- PromptSnapshot, DebugSnapshot, ConnectionProbe

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | Streaming response | Minor | 1 zi |
| 🟢 | Multi-model routing per use case | Minor | 1-2 zile |
| 🟢 | AI suggestion cooldown per NPC | Minor | 0.5 zi |

---

## 6. Progression — 92% (17 fișiere, 135KB)

### Implementat
- ProgressionService (24KB) — definiții, cache, snapshot-uri
- PlayerProgressionService (15KB) — level, XP, skills
- ProgressionRepository (22KB) — DB persistence
- ProgressionDefinition, filter, selector, gui entries
- StoredProgression, StoredProgressionSummary

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | Definiții standalone (acum din feature packs) | Minor | 1 zi |
| 🟢 | Progression rewards cu efecte vizuale | Minor | 1 zi |

---

## 7. Story System — 95% (11 fișiere, 95KB)

### Implementat
- StoryStateService (22KB) — stare persistentă regiuni/places, variabile, story pools
- StoryContextService (21KB) — build context snapshot-uri
- StoryAuthoringService (8KB) — creare manuală evenimente story (16 tipuri) + 10 template-uri ✅ **NOU**
- StoryReactionService (10KB) — NPC-uri reacționează la evenimente story (emoții + stare) ✅ **NOU**
- StoryEvent, RegionStoryState, PlaceStoryState
- StructureStoryEventPlanner (14KB)
- StoryStructureSignalResolver (10KB)
- Comenzi: /ainpc story region, place, events, context, author (cu template-uri)

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | Story authoring GUI | Minor | 1 zi |

---

## 8. MCP Integration — 90% (42 fișiere, ~140KB)

### Plugin Side (16 fișiere, ~45KB)
- HttpMcpRuntimeClient (9.6KB) — client HTTP JSON-RPC, circuit breaker, session management
- McpCommandQueue (12KB) — coadă comenzi write prin fișiere JSON (7 tipuri) ✅ **NOU**
- McpDialogContextProvider (2.2KB) — context dialog
- RuntimeSnapshotProducer (6.6KB) — snapshot periodic
- AdminMcpGui (12KB)

### MCP Server (26 Java fișiere, ~100KB)
- 19 tool-uri read-only pe 8 clase
- **7 tool-uri write** (AinpcWriteTools) ✅ **NOU**
  - ainpc.npc.say, ainpc.npc.setState, ainpc.broadcast, ainpc.executeCommand
  - ainpc.quest.progress, ainpc.quest.complete, ainpc.quest.list
- Bridge: SnapshotReader, McpSnapshotService
- Health monitoring (writeToolsEnabled: true)

### 8 tool-uri apelate din plugin
ainpc.dialog.context, ainpc.feature.state, ainpc.debug.health, ainpc.server.snapshot, ainpc.build.mode.status, ainpc.build.mode.history, ainpc.build.mode.export, ainpc.npc.list

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | NPC management tools via MCP (spawn, delete) | Minor | 2-3 zile |
| 🟢 | SSE transport pe lângă polling | Minor | 2 zile |

---

## 9. GUI System — 96% (55 fișiere, 537KB)

### 36 Ecrane + 18 Suport + 1 Listener
- Hub-uri: Main, Player, Admin, Creator
- Quest: Log, Detail, Offer, Authoring, Map, Edit, Create, Quick, Creator, Test
- World: Hub, Place, Region, Mapping, Creator, Node
- NPC: Interact, Manager, Routine, Shop
- Story, Stats, Audit, Debug, MCP, Confirm
- Suport: GuiService (31KB), GuiItemFactory, GuiNavigation, GuiAccessHelper

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | Relationship GUI | Minor | 1 zi |
| 🟢 | Story authoring GUI | Minor | 1 zi |

---

## 10. Addon System — 90% (2 fișiere, 17KB)

### Implementat
- AddonRegistry (11KB) — gestiune descriptor/addon, validare
- AddonDependencyResolver (6.3KB) — dependințe, detectare cicluri, sortare topologică
- FeaturePackLoader (42KB) — încărcare YAML, traits/profesii/dialoguri/scenarii
- PackFileWatcher (3.2KB) — hot-reload cu polling

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | Addon store/download | Minor | 3-4 zile |
| 🟢 | Per-pack selective reload | Minor | 1 zi |

---

## 11. Debug & Audit — 96% (36 fișiere, 280KB)

### Implementat
- DebugDumpService — orchestrează 36 module de debug dump
- 36 module: NPC, World, Mapping, Quest, Story, Progression, Economy, Authoring, Server, Config, Audit, Secrets, Routing, RecentEvents
- RecentEventsBuffer
- AuditGui, DebugGui
- Health check în debug dumps

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟢 | Endpoint health check dedicat | Minor | 0.5 zi |

---

## 12. Test Coverage — 194 fișiere, 771KB

### Distribuție
- NPC/spawn: ~45 fișiere
- Quest/engine: ~35 fișiere
- World/mapping: ~28 fișiere
- GUI: ~18 fișiere
- AI: ~12 fișiere
- Economy: ~8 fișiere
- Database: ~6 fișiere
- Comenzi: ~15 fișiere
- Diverse: ~27 fișiere

### Ce mai lipsește
| # | Componenta | Impact | Efort |
|---|-----------|--------|-------|
| 🟡 | Integration tests pentru servicii noi | Mediu | 3-4 zile |
| 🟢 | Smoke tests pentru rutare comandă completă | Minor | 2 zile |

---

## Top 5 Priorități de Implementat

| # | Ce | Modul | Efort | Impact | Prioritate |
|---|----|-------|-------|--------|------------|
| 1 | **Relationship GUI** (ecran dedicat relații NPC) | GUI | 1 zi | Vizualizare relații în interfață | 🟢 |
| 2 | **Story authoring GUI** | GUI | 2 zile | Creare evenimente story vizual | 🟢 |
| 3 | **Integration tests** pentru servicii noi | Testare | 3-4 zile | Stabilitate servicii noi | 🟡 |
| 4 | **Banking/investment mechanics** | Economy | 2 zile | Economie avansată | 🟢 |
| 5 | **Building template auto-placement** | Mapping | 2 zile | Generare automată clădiri | 🟢 |

---

## Statistici Globale

| Metrică | Valoare |
|---------|---------|
| Total fișiere cod sursă | ~428 (main) |
| Total linii cod (estimat) | ~95,000 |
| Total fișiere test | ~207 |
| Module | 5 |
| Pachete feature | 8 (medieval, medieval_quest, social, festival, wilderness, tutorial_demo, sample_objectives, compat) |
| Total bytes cod sursă | ~3.55MB |
| Total bytes test | ~780KB |
| Comenzi implementate | 17 fișiere, ~845KB |
| Subcomenzi disponibile | 40+ |
| Ecrane GUI | 36 |
| Tool-uri MCP (server) | 26 (19 read-only + 7 write) |
| Tool-uri MCP apelate (plugin) | 8 |
| Template-uri story | 10 |
| Nume NPC posibile | ~28,600 combinații |
| NPC-uri cu rutine, relații, economie | Da |
| Reacții NPC la evenimente story | 14 tipuri |
| Adunări sociale detectate automat | Da |
| Hot-reload feature packs | Da |
| Multi-model AI (OpenAI + Ollama) | Da |
| Integration Vault economy | Da |
| MCP write tools | 7 |
| Teste adăugate în sesiune | 3 noi (12 metode) |
