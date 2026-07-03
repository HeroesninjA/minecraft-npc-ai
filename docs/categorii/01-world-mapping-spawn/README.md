# World Mapping si Spawn

Actualizat: 2026-07-01

Aceasta categorie acopera fundatia semantica a lumii: regiuni, places, nodes, spawn order, case si bindings.

## Documente

| Document | Rol |
|---|---|
| `../../mapping-stack.md` | Punct de intrare scurt pentru documentele de mapping |
| `../../npc-population-world-stack.md` | Punct de intrare scurt pentru planul de regiune, binding-uri NPC, household-uri si spawn |
| `../../mapping.md` | Starea actuala, regulile de consum si evolutia sistemului `WorldRegion -> WorldPlace -> WorldNode` |
| `../../build-mode-tutorial.md` | Tutorial scurt pentru mapping asistat AI/intent: `world create ai preview`, `map edit`, confirmare si save |
| `../../build-mode-region-place-node.md` | Contract extins pentru build mode, preview, editor si integrare `Quest`/`Progression` |
| `../../playable-village-ux.md` | Criterii de playability pentru sat: spatiere, teren, NPC stabili, rutina si interactiuni clare |
| `../../gui-stack.md` | Punct de intrare scurt pentru documentele UI relevante pentru mapping si world hub |
| `../../lucru-alternat-quest-mapping-progression.md` | Protocol pentru a verifica mapping-ul prin questuri/contracte mici si GUI peste snapshot-uri inainte de extractii mari de runtime |
| `../../mapping-harti-manuale.md` | Ghid pentru harti construite manual, etichetare semantica, wand + prompturi naturale si limitele detectiei automate |
| `../../mapping-pentru-implementari-ulterioare.md` | Redirect istoric catre `../../mapping.md` |
| `../../npc-world-bindings.md` | Tabela dedicata pentru legaturi NPC -> home/work/social places si nodes |
| `../../ordine-spawn-npc-cladiri-region-node.md` | v2 pentru spawn order, household, generator si rollback |
| `../../harta-clase-spawn.md` | Harta doar-documentatie pentru relatiile dintre clasele cheie ale subsistemului spawn |
| `../../settlement-plan.md` | Contract pentru planul complet de regiune inainte de mapping/populatie/spawn |
| `../../structuri-exterioare-satului.md` | Definitii semantice pentru structuri din afara satului: castel, padure, fantana, casa izolata, mini-sat, sat de barbari, dungeon si extensii |
| `../../mediu-test-controlat-sat-si-structuri-exterioare.md` | Fixture demo/test temporar pentru sat predefinit, structuri exterioare si ancore stabile de validare; nu trebuie sa ramana hardcodat in core |
| `../../schema-scenariu-predefinit-testare.md` | Schema pentru scenariu de test cu cladiri controlate, NPC-uri controlate, relatii, context semantic si quest/story peste fixture |
| `../../generare-populatie-narativa.md` | Contract pentru nume, roluri, familii si distributie home/work/social inainte de spawn |
| `../../households-persistente.md` | Contract pentru household-uri persistente si rezidenti, peste mapping si `npc_world_bindings` |
| `../../gui-interfete.md` | Directie pentru World GUI: whereami, regions, places, nodes, household/settlement plan si audit vizual |
| `../../prevenire-duplicare-npc.md` | Reguli si runbook pentru evitarea duplicarii NPC la spawn, chunk load si retry |
| `../../rutine-npc-si-timeline.md` | Rutine NPC peste ancore home/work/social |
| `../../generare-sate-fara-worldedit.md` | Scanner, mapper semantic si completare sate vanilla |

## Status scurt

- Mapping-ul exista si are lookup pentru region/place/node.
- Primul pass de playable village este documentat: demo semantic mai spatios, teren plat recomandat si criterii pentru sat lizibil.
- Mapping wand + prompt natural exista initial pentru `region`, `place`, `node`, `npc_bind` si `quest_anchor`, cu draft, preview vizual, editare GUI si confirmare explicita.
- `/ainpc world create ai [preview|dryrun|inspect] [region|place|node] ...` creeaza draft AI/intent fara scriere directa; `/ainpc map edit/open/gui` redeschide editorul pentru draftul curent.
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
- Protectiile anti-duplicare au index DB `npc_source_keys`, marker persistent pe entitate, `/ainpc duplicates`, `/ainpc delete-id` si `/ainpc repair duplicates`.
- Ramane de acoperit batch-ul persistent complet pentru retry/rollback settlement la nivel de `spawn_batches`.

## Fazele urmatoare

1. Pass Paper pe playable village: teren plat, case distantate, NPC-uri stabile si rutine inspectabile.
2. Anti-duplicare NPC dupa restart: `/ainpc duplicates`, `/ainpc repair duplicates dryrun`, restart, chunk reload si verificare `npc_source_keys`.
3. Smoke test Paper pentru `world demo create -> settlement plan -> settlement spawn -> audit -> save -> reload`, plus flux wand/AI draft.
4. Backfill matur pentru `npc_world_bindings` si household-uri persistente.
5. Generator narativ de populatie pe regiune: nume, roluri, familii si distributie pe case/work/social.
6. Hardening pentru spawn pe regiune: `spawn_batches`, retry idempotent, compensare documentata, debugdump si test de rollback.
7. Quest slice peste mapping: 3-5 questuri medievale cu `visit_place`, `inspect_node`, quest anchors si story events.

## Urmatoarele documente utile

- Document dedicat pentru tranzactie DB completa pe spawn de regiune.
- Document dedicat pentru migration/backfill mapping.
- Document dedicat pentru `spawn_batches` si retry idempotent settlement/household.

## Relatii

Vezi ../../relatii-documentatie.md pentru catalogul pe fisiere si lanturile de citire aferente acestei categorii.
Pentru harta de cod, foloseste ../../harta-pachetelor-cod-scurta.md pentru orientare rapida si ../../harta-pachetelor-cod.md pentru detaliu complet.
Pentru harta de clase pe world, foloseste ../../harta-clase-world.md.
Pentru harta de clase pe spawn, foloseste ../../harta-clase-spawn.md.
