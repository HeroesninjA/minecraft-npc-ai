# World Mapping si Spawn

Actualizat: 2026-05-03

Aceasta categorie acopera fundatia semantica a lumii: regiuni, places, nodes, spawn order, case si bindings.

## Documente

| Document | Rol |
|---|---|
| `../../mapping.md` | Starea actuala, regulile de consum si evolutia sistemului `WorldRegion -> WorldPlace -> WorldNode` |
| `../../mapping-harti-manuale.md` | Ghid pentru harti construite manual, etichetare semantica si limitele detectiei automate |
| `../../mapping-pentru-implementari-ulterioare.md` | Redirect istoric catre `../../mapping.md` |
| `../../ordine-spawn-npc-cladiri-region-node.md` | v2 pentru spawn order, household, generator si rollback |
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
- Persistenta dedicata `npc_world_bindings` lipseste inca; bind-ul curent foloseste `profile_data` si metadata pe place.
- Protectiile anti-duplicare exista initial in `NPCManager`, dar lipsesc inca delete-by-id, batch idempotent si marker persistent pe entitate.

## Urmatoarele documente utile

- Document dedicat pentru `npc_world_bindings`.
- Document dedicat pentru tranzactie DB completa pe spawn de regiune.
- Document dedicat pentru generarea narativa a populatiei.
- Document dedicat pentru migration/backfill mapping.
