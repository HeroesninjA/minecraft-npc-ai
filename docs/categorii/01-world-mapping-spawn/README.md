# World Mapping si Spawn

Actualizat: 2026-05-07

Aceasta categorie acopera fundatia semantica a lumii: regiuni, places, nodes, spawn order, case si bindings.

## Documente

| Document | Rol |
|---|---|
| `../../mapping.md` | Starea actuala, regulile de consum si evolutia sistemului `WorldRegion -> WorldPlace -> WorldNode` |
| `../../lucru-alternat-quest-mapping-progression.md` | Protocol pentru a verifica mapping-ul prin questuri/contracte mici inainte de extractii mari de runtime |
| `../../mapping-harti-manuale.md` | Ghid pentru harti construite manual, etichetare semantica si limitele detectiei automate |
| `../../mapping-pentru-implementari-ulterioare.md` | Redirect istoric catre `../../mapping.md` |
| `../../npc-world-bindings.md` | Tabela dedicata pentru legaturi NPC -> home/work/social places si nodes |
| `../../ordine-spawn-npc-cladiri-region-node.md` | v2 pentru spawn order, household, generator si rollback |
| `../../settlement-plan.md` | Contract pentru planul complet de regiune inainte de mapping/populatie/spawn |
| `../../generare-populatie-narativa.md` | Contract pentru nume, roluri, familii si distributie home/work/social inainte de spawn |
| `../../households-persistente.md` | Contract pentru household-uri persistente si rezidenti, peste mapping si `npc_world_bindings` |
| `../../gui-interfete.md` | Directie pentru World GUI: whereami, regions, places, nodes, household/settlement plan si audit vizual |
| `../../prevenire-duplicare-npc.md` | Reguli si runbook pentru evitarea duplicarii NPC la spawn, chunk load si retry |
| `../../rutine-npc-si-timeline.md` | Rutine NPC peste ancore home/work/social |
| `../../generare-sate-fara-worldedit.md` | Scanner, mapper semantic si completare sate vanilla |

## Status scurt

- Mapping-ul exista si are lookup pentru region/place/node.
- `WorldContextSnapshot` este legat initial in `NPCContext`.
- `visit_place`, `inspect_node` si `QuestAnchorResolver` exista initial.
- Persistenta dedicata `quest_anchor_bindings` exista initial.
- Auditul/comanda admin pentru `quest_anchor_bindings` exista initial.
- Bind-ul initial NPC -> home/work/social places exista prin `/ainpc world bind npc ...`.
- Plannerul initial casa -> `HouseAllocation` exista prin `/ainpc world household plan ...`.
- Spawn-ul initial household din mapping exista prin `/ainpc world household spawn ...`.
- Plannerul initial regiune -> lista de `HouseAllocation` exista prin `/ainpc world settlement plan ...`.
- Spawn-ul initial pe regiune exista prin `/ainpc world settlement spawn ...`.
- Rollback-ul global practic pentru `settlement spawn` exista la nivel de NPC-uri create anterior.
- Persistenta dedicata `npc_world_bindings` exista initial; bind-ul curent pastreaza si fallback-ul `profile_data` plus metadata pe place.
- Inspectia read-only pentru `npc_world_bindings` exista prin `/ainpc world bindings ...`.
- Debugdump-ul `world/all` exporta `npc-world-bindings.json`.
- Protectiile anti-duplicare exista initial in `NPCManager`, dar lipsesc inca delete-by-id, batch idempotent si marker persistent pe entitate.

## Fazele urmatoare

1. Smoke test Paper pentru `world demo create -> settlement plan -> settlement spawn -> audit -> save -> reload`.
2. Backfill matur pentru `npc_world_bindings` si household-uri persistente.
3. Generator narativ de populatie pe regiune: nume, roluri, familii si distributie pe case/work/social.
4. Hardening pentru spawn pe regiune: tranzactie DB completa sau compensare documentata, debugdump si test de rollback.
5. Quest slice peste mapping: 3-5 questuri medievale cu `visit_place`, `inspect_node`, quest anchors si story events.

## Urmatoarele documente utile

- Document dedicat pentru tranzactie DB completa pe spawn de regiune.
- Document dedicat pentru migration/backfill mapping.
