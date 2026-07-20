# Tipuri canonice de obiective

Status: referinta canonica.
Verificat in cod: 2026-07-15.

`ObjectiveTypeAliasRegistry` recunoaste exact 12 tipuri canonice. Pack-urile noi trebuie sa foloseasca numele canonice, chiar daca runtime-ul normalizeaza si aliasuri de compatibilitate.

## Catalog

| Tip canonic | Eveniment urmarit | Referinta uzuala in `item` |
| --- | --- | --- |
| `collect_item` | inventar | material, de exemplu `OAK_LOG` |
| `deliver_to_npc` | inventar plus turn-in la NPC | materialul livrat; vezi limita de mai jos |
| `talk_to_npc` | interactiune NPC | nume, profesie, UUID sau selector NPC |
| `visit_region` | intrare in mapping | ID, tip sau tag de regiune |
| `visit_place` | intrare in mapping | ID, tip sau tag de place |
| `inspect_node` | prezenta sau interactiune la node | ID, tip sau metadata de node |
| `kill_mob` | moarte entitate | tipul mobului; tinta poate fi omisa de registry |
| `place_block` | plasare bloc | material de bloc |
| `break_block` | spargere bloc | material de bloc |
| `craft_item` | crafting | material rezultat |
| `use_item` | folosire/consum | material folosit |
| `equip_item` | echipare | material echipat |

Toate cele 12 tipuri au handler inregistrat de `ScenarioEngine`. Filtrarea concreta ramane responsabilitatea traseului de eveniment si trebuie verificata prin test runtime, nu dedusa doar din existenta handler-ului.

## Campuri

- cheia obiectivului din YAML trebuie sa fie unica si stabila;
- `type` trebuie sa fie unul dintre numele canonice;
- `amount` trebuie sa descrie pragul urmarit;
- registry-ul cere `item` pentru toate tipurile in afara de `kill_mob`;
- campul `item` este o referinta supraincarcata: material, selector semantic sau tinta, in functie de tip;
- un `type` gol nu este suportat; nu te baza pe fallback-ul intern la `collect_item`.

## Aliasuri tolerate

- `collect_item`: `item`, `collect`, `collectitem`, `fetch`, `gather`;
- `deliver_to_npc`: `deliver`, `deliveritem`, `deliver_item`, `turnin`, `turn_in`;
- `talk_to_npc`: `talk`, `speak`, `conversation`, `talk_npc`, `talk_nlc`, `speak_to_npc`;
- `visit_region`: `visit`, `travel`, `go_to`, `enter_region`;
- `visit_place`: `visitplace`, `enterplace`, `enter_place`, `go_to_place`;
- `inspect_node`: `inspect`, `inspectnode`, `interact_node`, `interact_nkde`;
- `kill_mob`: `kill`, `slay`, `defeat`;
- `place_block`: `placeblock`, `build`, `construct`;
- `break_block`: `break`, `breakblock`, `mine`, `dig`, `excavate`;
- `craft_item`: `craft`, `craftitem`, `make`, `create_item`, `fabricate`;
- `use_item`: `use`, `consume`, `drink`, `eat`, `activate`, `utilize`;
- `equip_item`: `equip`, `wear`, `don`, `put_on`.

Aliasurile `talk_nlc`, `interact_nkde`, `turnin`, `gather`, `slay`, `construct` si `fabricate` sunt marcate deprecated. Migreaza-le la tipul canonic recomandat.

## Ambiguitate cunoscuta la delivery

Pentru `deliver_to_npc`, progresul de inventar trateaza `item` ca material livrat, dar `QuestAnchorResolver` incearca sa interpreteze acelasi camp si ca referinta NPC. Pana cand schema separa materialul de destinatar, nu considera binding-ul NPC al acestui tip un contract stabil. Valideaza turn-in-ul in joc si urmareste remedierea in `planning/questuri-avansate-v2.md`.

## Surse tehnice

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ObjectiveTypeAliasRegistry.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/runtime/objectivehandlers/`
- `ainpc-scenario-medieval/src/main/resources/packs/sample_objectives_quest.yml`

## Documente dependente

- `reference/objective-examples.md`
- `guides/quest-authoring-tutorial.md`
- `reference/quest-anchor-bindings.md`
