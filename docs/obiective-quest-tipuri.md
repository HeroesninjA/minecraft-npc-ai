# Obiective Quest - Tipuri Suportate

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## Stare curentÄƒ (2026-06-19)

| Tip obiectiv | ID configuraÈ›ie | Status | DetecÈ›ie |
|---|---|---|---|
| ViziteazÄƒ regiune | `visit_region` | âœ… Implementat | AutomatÄƒ la miÈ™care (`PlayerMoveEvent`) |
| ViziteazÄƒ loc | `visit_place` | âœ… Implementat | AutomatÄƒ la miÈ™care (`PlayerMoveEvent`) |
| InspecteazÄƒ nod | `inspect_node` | âœ… Implementat | AutomatÄƒ la miÈ™care (`PlayerMoveEvent`) |
| VorbeÈ™te cu NPC | `talk_to_npc` | âœ… Implementat | La right-click pe NPC (`handleQuestInteraction`) |
| ColecteazÄƒ obiect | `collect_item` | âœ… Implementat | La pickup (`EntityPickupItemEvent`) + inventory check |
| OmorÄƒ mob | `kill_mob` | âœ… Implementat | La moarte mob (`EntityDeathEvent`) |
| LivreazÄƒ la NPC | `deliver_to_npc` | âœ… Implementat | La right-click pe NPC cu obiectul Ã®n inventar |
| PlaseazÄƒ bloc | `place_block` | âŒ **LipseÈ™te** | Neimplementat |

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
- **Listener**: `NPCInteractionListener` â†’ `ScenarioEngine.handleQuestInteraction()`
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
- **Listener**: `NPCInteractionListener` â†’ `ScenarioEngine.handleQuestInteraction()`
- **Engine**: Verificare inventar + consumare obiect la predare
- **Config**: `item: "WHEAT"`, `amount: 5`

## Obiective viitoare (neimplementate)

### place_block
- NecesitÄƒ:
  - Listener pentru `BlockPlaceEvent`
  - Verificare tip bloc (`item: "OAK_PLANKS"`)
  - Verificare opÈ›ionalÄƒ locaÈ›ie (region/place/node)
  - Logica `incrementObjectiveProgress()` similarÄƒ cu `collect_item`

### break_block
- Similar cu `place_block` dar cu `BlockBreakEvent`

### ride_entity / tame_entity
- Pentru questuri de cÄƒlÄƒrie / Ã®mblÃ¢nzire

### craft_item
- Pentru questuri de crafting

## Cum se adaugÄƒ un tip nou de obiectiv

1. AdaugÄƒ tipul Ã®n `normalizeQuestObjectiveType()` Ã®n `AINPCCommandText.kt`
2. AdaugÄƒ Ã®n `isSupportedQuestObjectiveType()` Ã®n `AINPCCommandText.kt`
3. AdaugÄƒ listener pentru evenimentul Bukkit corespunzÄƒtor
4. AdaugÄƒ logica de progres Ã®n `ScenarioEngine`
5. AdaugÄƒ formatare afiÈ™are Ã®n `ScenarioEngineText.kt`
6. AdaugÄƒ Ã®n `QuestDraftValidator.kt` pentru validare drafturi
7. AdaugÄƒ Ã®n tab completion (`AINPCTabCompleter.kt`)

