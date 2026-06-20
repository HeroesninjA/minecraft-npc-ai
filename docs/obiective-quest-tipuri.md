# Obiective Quest - Tipuri Suportate

## Stare curentă (2026-06-19)

| Tip obiectiv | ID configurație | Status | Detecție |
|---|---|---|---|
| Vizitează regiune | `visit_region` | ✅ Implementat | Automată la mișcare (`PlayerMoveEvent`) |
| Vizitează loc | `visit_place` | ✅ Implementat | Automată la mișcare (`PlayerMoveEvent`) |
| Inspectează nod | `inspect_node` | ✅ Implementat | Automată la mișcare (`PlayerMoveEvent`) |
| Vorbește cu NPC | `talk_to_npc` | ✅ Implementat | La right-click pe NPC (`handleQuestInteraction`) |
| Colectează obiect | `collect_item` | ✅ Implementat | La pickup (`EntityPickupItemEvent`) + inventory check |
| Omoră mob | `kill_mob` | ✅ Implementat | La moarte mob (`EntityDeathEvent`) |
| Livrează la NPC | `deliver_to_npc` | ✅ Implementat | La right-click pe NPC cu obiectul în inventar |
| Plasează bloc | `place_block` | ❌ **Lipsește** | Neimplementat |

## Detalii implementare

### visit_region
- **Listener**: `QuestObjectiveListener.onPlayerMove()`
- **Engine**: `ScenarioEngine.recordRegionVisit()`
- **Matching**: Compare `objectives[].item` cu `region.id` prin `matchesRegionObjective()`
- **Config**: `item: "region_id"`

### visit_place
- **Listener**: `QuestObjectiveListener.onPlayerMove()`
- **Engine**: `ScenarioEngine.recordRegionVisit()`
- **Matching**: Compare `objectives[].item` cu `place.id` prin `matchesPlaceObjective()`
- **Config**: `item: "region:place_id"` (ex: `castel:poarta_castel`)

### inspect_node
- **Listener**: `QuestObjectiveListener.onPlayerMove()`
- **Engine**: `ScenarioEngine.recordRegionVisit()`
- **Matching**: Compare `objectives[].item` cu `node.id` prin `matchesNodeObjective()`
- **Config**: `item: "region:place:node_id"` (ex: `castel:curte_castel:cufar`)

### talk_to_npc
- **Listener**: `NPCInteractionListener` → `ScenarioEngine.handleQuestInteraction()`
- **Engine**: `ScenarioEngine.trackNpcObjectiveProgress()` + `markNpcTalkObjective()`
- **Matching**: Compare `objectives[].item` cu `profession:npc_profession`
- **Config**: `item: "profession:garda"` (sau NPC name)

### collect_item
- **Listener**: `QuestObjectiveListener.onEntityPickupItem()`, `onInventoryClick()`, `onPlayerDropItem()`
- **Engine**: `ScenarioEngine.recordInventoryChange()`
- **Matching**: Inventory scan pentru materialul specificat
- **Config**: `item: "POPPY"` (material name Bukkit), `amount: 3`

### kill_mob
- **Listener**: `QuestObjectiveListener.onEntityDeath()`
- **Engine**: `ScenarioEngine.recordMobKill()`
- **Config**: `item: "ZOMBIE"` (entity type), `amount: 2`

### deliver_to_npc
- **Listener**: `NPCInteractionListener` → `ScenarioEngine.handleQuestInteraction()`
- **Engine**: Verificare inventar + consumare obiect la predare
- **Config**: `item: "WHEAT"`, `amount: 5`

## Obiective viitoare (neimplementate)

### place_block
- Necesită:
  - Listener pentru `BlockPlaceEvent`
  - Verificare tip bloc (`item: "OAK_PLANKS"`)
  - Verificare opțională locație (region/place/node)
  - Logica `incrementObjectiveProgress()` similară cu `collect_item`

### break_block
- Similar cu `place_block` dar cu `BlockBreakEvent`

### ride_entity / tame_entity
- Pentru questuri de călărie / îmblânzire

### craft_item
- Pentru questuri de crafting

## Cum se adaugă un tip nou de obiectiv

1. Adaugă tipul în `normalizeQuestObjectiveType()` în `AINPCCommandText.kt`
2. Adaugă în `isSupportedQuestObjectiveType()` în `AINPCCommandText.kt`
3. Adaugă listener pentru evenimentul Bukkit corespunzător
4. Adaugă logica de progres în `ScenarioEngine`
5. Adaugă formatare afișare în `ScenarioEngineText.kt`
6. Adaugă în `QuestDraftValidator.kt` pentru validare drafturi
7. Adaugă în tab completion (`AINPCTabCompleter.kt`)
