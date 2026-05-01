# World Mapping si Spawn

Actualizat: 2026-04-30

Aceasta categorie acopera fundatia semantica a lumii: regiuni, places, nodes, spawn order, case si bindings.

## Documente

| Document | Rol |
|---|---|
| `../../mapping.md` | Starea actuala, regulile de consum si evolutia sistemului `WorldRegion -> WorldPlace -> WorldNode` |
| `../../mapping-harti-manuale.md` | Ghid pentru harti construite manual, etichetare semantica si limitele detectiei automate |
| `../../mapping-pentru-implementari-ulterioare.md` | Redirect istoric catre `../../mapping.md` |
| `../../ordine-spawn-npc-cladiri-region-node.md` | v2 pentru spawn order, household, generator si rollback |
| `../../rutine-npc-si-timeline.md` | Rutine NPC peste ancore home/work/social |
| `../../generare-sate-fara-worldedit.md` | Scanner, mapper semantic si completare sate vanilla |

## Status scurt

- Mapping-ul exista si are lookup pentru region/place/node.
- `WorldContextSnapshot` este legat initial in `NPCContext`.
- `visit_place`, `inspect_node` si `QuestAnchorResolver` exista initial.
- Persistenta dedicata `quest_anchor_bindings` exista initial.
- Auditul/comanda admin pentru `quest_anchor_bindings` exista initial.
- Persistenta dedicata `npc_world_bindings` lipseste inca.

## Urmatoarele documente utile

- Document dedicat pentru `npc_world_bindings`.
- Document dedicat pentru migration/backfill mapping.
