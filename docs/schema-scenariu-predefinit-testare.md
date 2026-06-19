# Schema scenariu predefinit de testare

Actualizat: 2026-06-16

## Scop

Acest document defineste schema pentru un scenariu predefinit de testare peste un sat controlat, cladiri controlate, NPC-uri controlate, structuri exterioare si context narativ. Scopul este sa putem testa questuri cu story intr-un mediu predictibil, fara sa depindem de generare aleatoare sau de o harta reala incompleta.

Documentul completeaza `mediu-test-controlat-sat-si-structuri-exterioare.md`:

- acel document defineste fixture-ul de lume: regiuni, places, nodes si structuri exterioare;
- acest document defineste fixture-ul de scenariu: tip sat, istoric, cladiri, NPC-uri, relatii, story state si questuri de test.

## Avertizare obligatorie

Aceasta schema este pentru test/demo intern.

Regula:

```text
scenariu predefinit de testare
-> cod de test sau fixture
-> nu continut final
-> nu activ implicit
-> nu dependinta permanenta pentru core
-> se muta ulterior in addon demo, test resources sau pack de scenarii separat
```

Nu se accepta ca acest scenariu sa devina:

- lore final hardcodat in core;
- generator permanent de sate;
- sursa unica de adevar pentru NPC-urile finale;
- bypass pentru mapping, audit, validare sau rollback;
- motiv pentru a introduce tema medievala direct in core fara namespacing si profil explicit.

## Reguli constitutionale

Schema trebuie sa respecte regulile proiectului:

- activare explicita prin profil `test`, `demo` sau comanda admin;
- default dezactivat pe servere reale;
- toate ID-urile sunt stabile si namespaced cu `test_`;
- mapping-ul semantic exista inainte de spawn, quest sau story;
- planurile sunt read-only in primul slice;
- creare mapping-only in slice separat, daca este aprobata;
- spawn NPC separat si idempotent, niciodata implicit;
- quest progress si story events apar doar prin interactiuni sau comenzi de test explicite;
- fiecare pas are audit/debugdump;
- scenariul poate fi sters sau mutat fara sa rupa sistemele generale.

## Model mental

Scenariul are cinci straturi:

| Strat | Ce controleaza | Nu controleaza |
|---|---|---|
| `scenario` | identitate, profil, versiune, reguli fixture | gameplay live |
| `world_fixture` | sat, cladiri, structuri exterioare, ancore | build fizic automat |
| `population_fixture` | NPC-uri, roluri, case, locuri de lucru | AI generativ liber |
| `semantic_context` | tip sat, istoric, tensiuni, relatii | lore final de productie |
| `quest_story_tests` | questuri de smoke, story events asteptate | branching complex permanent |

## Schema de baza

Format tinta recomandat pentru authoring:

```yaml
scenario:
  id: test_story_sat_01
  version: 1
  namespace: test_
  profile: test
  status: fixture_only
  enabled_by_default: false
  description: "Scenariu controlat pentru testarea questurilor cu story."

fixture_policy:
  temporary: true
  production_allowed: false
  mapping_only_first: true
  auto_build_blocks: false
  auto_spawn_npcs: false
  auto_spawn_mobs: false
  auto_grant_quest_progress: false
  cleanup_required: true

village_context:
  region_id: test_sat_central
  village_type: frontier_hamlet
  social_profile: small_trade_outpost
  danger_level: low_inside_village
  outside_danger_level: medium
  history_summary: "Sat mic aparut langa un drum vechi si o fantana uitata."
  current_tension: "Provizii lipsa si zvonuri despre miscari langa padure."

world_fixture:
  village_region: test_sat_central
  buildings: []
  exterior_structures: []

population_fixture:
  expected_npc_count: 12
  npcs: []
  relations: []

quest_story_tests:
  quests: []
  expected_story_events: []
```

## Sat controlat

Satul de test trebuie sa fie mic, lizibil si suficient pentru un flux complet de quest.

| Camp | Valoare recomandata | Motiv |
|---|---|---|
| `region_id` | `test_sat_central` | ID stabil pentru comenzi si teste |
| `village_type` | `frontier_hamlet` | Sat mic, bun pentru testare controlata |
| `social_profile` | `small_trade_outpost` | Permite negustor, taverna, ferme si drumuri |
| `history_period` | `old_road_settlement` | Context istoric generic, fara lore final |
| `risk_profile` | `safe_core_dangerous_edges` | Interior sigur, exterior testabil |
| `primary_problem` | `missing_supplies` | Declanseaza questuri simple |
| `secondary_problem` | `forest_rumors` | Leaga padurea, casa izolata si dungeon-ul |

Exemplu:

```yaml
village_context:
  region_id: test_sat_central
  village_type: frontier_hamlet
  history:
    founding_reason: old_road_crossing
    notable_place: test_fantana_uitata
    unresolved_event: old_caravan_disappearance
  current_state:
    primary_problem: missing_supplies
    secondary_problem: forest_rumors
    public_mood: cautious
    trust_in_outsiders: mixed
```

## Cladiri controlate

Pentru primul scenariu, numarul de cladiri trebuie sa fie fix. Daca generatorul produce alt numar, testul trebuie sa esueze sau sa raporteze drift.

| Tip cladire | Numar | ID-uri recomandate | Rol test |
|---|---:|---|---|
| piata/centru | 1 | `test_sat_central:piata` | start, interactiune, quest trigger |
| case simple | 5 | `casa_1` ... `casa_5` | home anchors, household-uri |
| fierarie | 1 | `fierarie` | work anchor, crafting context, quest clue |
| ferma | 1 | `ferma` | work anchor, provizii, resource context |
| taverna | 1 | `taverna` | social anchor, zvonuri, dialog |
| altar/fantana mica in sat | 1 | `altar` | ritual/lore inspect, story context |
| depozit | 1 | `depozit` | missing supplies, inspect node |
| post paza | 1 | `post_paza` | guard routine, road danger |
| drumuri/iesiri | 2 | `drum_nord`, `drum_est` | legaturi spre structuri exterioare |

Total minim controlat: 14 places in sat.

Exemplu de intrare:

```yaml
buildings:
  - id: test_sat_central:casa_1
    type: house
    count_slot: house_1
    required_nodes: [door, bed, home, npc_spawn]
    capacity: 2
    owner_role: village_leader
    quest_anchors: []

  - id: test_sat_central:depozit
    type: storage
    count_slot: storage_1
    required_nodes: [door, storage, inspect, quest_trigger]
    capacity: 0
    owner_role: village_leader
    quest_anchors: [missing_supplies_clue]
```

## Structuri exterioare controlate

Structurile exterioare trebuie sa ramana putine si cu rol clar in story.

| Structura | Numar | Tip semantic | Rol in scenariu |
|---|---:|---|---|
| padure | 1 | `forest` | prima zona de investigatie |
| fantana uitata | 1 | `fountain` | istoric local si clue ritual |
| casa izolata | 1 | `isolated_house` | NPC izolat si informatie ascunsa |
| dungeon/cripta | 1 | `dungeon` | dovada finala pentru quest |
| tabara factiune | 1 | `faction_settlement` | tensiune sociala si negociere |
| turn paza | 1 | `watchtower` | observatie drum si control perimetru |
| ruine | 1 | `ruins` | lore inspect si context istoric |

Exemplu:

```yaml
exterior_structures:
  - id: test_padure_veche
    type: forest
    required_nodes: [entry, trail, resource, danger_marker]
    story_role: first_investigation_area
    linked_quests: [test_q01_missing_supplies]

  - id: test_cripta_lupilor
    type: dungeon
    required_nodes: [entrance, exit, chamber, evidence, loot_marker]
    story_role: proof_location
    linked_quests: [test_q03_crypt_proof]
```

## NPC-uri controlate

NPC-urile trebuie definite prin numar si tip. Nu se foloseste populatie random in scenariul de test initial.

Numar recomandat: 12 NPC-uri.

| Rol NPC | Numar | ID-uri recomandate | Home | Work/Social | Rol story |
|---|---:|---|---|---|---|
| lider sat | 1 | `test_npc_lider` | `casa_1` | `piata` | ofera quest principal |
| fierar | 1 | `test_npc_fierar` | `casa_2` | `fierarie` | confirma lipsa provizii |
| fermieri | 2 | `test_npc_fermier_1`, `test_npc_fermier_2` | `casa_3` | `ferma` | martori si rutina work |
| hangiu | 1 | `test_npc_hangiu` | `casa_4` | `taverna` | zvonuri si dialog social |
| paznic | 1 | `test_npc_paznic` | `post_paza` | `post_paza` | indica drum periculos |
| negustor | 1 | `test_npc_negustor` | `casa_5` | `piata` | legatura cu provizii |
| vindecator/ierbar | 1 | `test_npc_ierbar` | `casa_5` | `altar` | context despre fantana |
| ucenic | 1 | `test_npc_ucenic` | `casa_2` | `fierarie` | martor secundar |
| mesager | 1 | `test_npc_mesager` | `taverna` | `drum_nord` | declanseaza informatie de drum |
| izolat | 1 | `test_npc_pustnic` | `test_casa_izolata` | `test_casa_izolata` | cunoaste istoria veche |
| reprezentant factiune | 1 | `test_npc_emisar` | `test_tabara_factiune` | `test_tabara_factiune` | negociere sau tensiune |

Exemplu:

```yaml
npcs:
  - id: test_npc_lider
    type: village_leader
    display_role: "Liderul satului"
    home_place: test_sat_central:casa_1
    work_place: test_sat_central:piata
    social_place: test_sat_central:taverna
    story_flags: [quest_giver, knows_missing_supplies]

  - id: test_npc_pustnic
    type: hermit
    display_role: "Locuitor izolat"
    home_place: test_casa_izolata
    work_place: test_casa_izolata
    social_place: test_padure_veche
    story_flags: [knows_old_history, unlocks_crypt_context]
```

## Relatii intre NPC-uri

Relatiile trebuie sa fie explicite, nu deduse liber de AI. AI-ul poate formula dialog peste relatii, dar nu le modifica fara actiune validata.

Tipuri recomandate:

| Tip relatie | Scop |
|---|---|
| `household` | locuiesc impreuna |
| `family` | legatura de rudenie generica |
| `work` | colaborare profesionala |
| `trust` | incredere pozitiva |
| `rivalry` | tensiune controlata |
| `debt` | datorie narativa |
| `witness` | un NPC stie ceva despre alt NPC |
| `faction_contact` | legatura cu structura externa |

Exemplu:

```yaml
relations:
  - from: test_npc_fierar
    to: test_npc_ucenic
    type: work
    intensity: high
    reason: "Ucenicul lucreaza zilnic in fierarie."
    visible_to_player: true

  - from: test_npc_negustor
    to: test_npc_emisar
    type: debt
    intensity: medium
    reason: "Negustorul suspecteaza ca proviziile lipsa au trecut prin tabara."
    visible_to_player: false

  - from: test_npc_pustnic
    to: test_npc_lider
    type: trust
    intensity: low
    reason: "Pustnicul cunoaste istoria veche, dar evita satul."
    visible_to_player: true
```

## Semantica de context

Contextul semantic trebuie sa fie scurt, validat si consumabil de story/dialog/quest. Nu se trimite un lore dump complet catre AI.

Campuri recomandate:

| Camp | Exemplu | Consumator |
|---|---|---|
| `village_type` | `frontier_hamlet` | StoryContextService, dialog |
| `history.founding_reason` | `old_road_crossing` | story, inspect_node |
| `history.unresolved_event` | `old_caravan_disappearance` | quest chain |
| `current_state.primary_problem` | `missing_supplies` | QuestDirector |
| `current_state.public_mood` | `cautious` | dialog |
| `social_tensions` | `merchant_vs_faction_contact` | story, branching viitor |
| `known_rumors` | `forest_tracks`, `old_well_whispers` | tavern dialog |
| `locked_knowledge` | `crypt_origin` | quest gated reveal |

Exemplu:

```yaml
semantic_context:
  village_type: frontier_hamlet
  history:
    founding_reason: old_road_crossing
    notable_place: test_fantana_uitata
    unresolved_event: old_caravan_disappearance
  current_state:
    primary_problem: missing_supplies
    public_mood: cautious
    outside_pressure: forest_rumors
  social_tensions:
    - id: merchant_vs_faction_contact
      participants: [test_npc_negustor, test_npc_emisar]
      state: suspected
  known_rumors:
    - id: forest_tracks
      source_npc: test_npc_hangiu
      target_place: test_padure_veche
  locked_knowledge:
    - id: crypt_origin
      unlock_condition: talk_to_npc:test_npc_pustnic
```

## Questuri de test cu story

Primul set trebuie sa verifice fluxul cap-coada, nu complexitatea.

| Quest | Scop | Obiective | Story event asteptat |
|---|---|---|---|
| `test_q01_missing_supplies` | introducere in sat si problema centrala | `talk_to_npc`, `inspect_node`, `visit_place` | `supplies_problem_confirmed` |
| `test_q02_forest_clue` | legatura sat -> exterior | `visit_region`, `inspect_node`, `talk_to_npc` | `forest_clue_found` |
| `test_q03_hermit_history` | relatie NPC + istoric | `visit_place`, `talk_to_npc` | `old_history_revealed` |
| `test_q04_crypt_proof` | dungeon ca locatie de dovada | `visit_region`, `inspect_node`, `talk_to_npc` | `crypt_proof_returned` |
| `test_q05_faction_contact` | tensiune sociala controlata | `talk_to_npc`, `visit_place`, `record_story_event` | `faction_contact_logged` |

Exemplu:

```yaml
quests:
  - id: test_q01_missing_supplies
    type: story_smoke
    starts_at: test_npc_lider
    stages:
      - id: INTRO
        objectives:
          - type: talk_to_npc
            npc_id: test_npc_lider
          - type: inspect_node
            place_id: test_sat_central:depozit
            node_tag: storage
      - id: RETURN
        objectives:
          - type: talk_to_npc
            npc_id: test_npc_lider
    story_actions:
      - type: record_story_event
        event_id: supplies_problem_confirmed
        scope: region
        target_id: test_sat_central
```

## Ordine de testare

Flux recomandat:

1. creeaza sau incarca mapping-ul fixture;
2. valideaza satul si structurile exterioare;
3. valideaza numarul de cladiri si tipurile lor;
4. creeaza planul NPC read-only;
5. valideaza numarul de NPC-uri, rolurile si home/work/social bindings;
6. valideaza relatiile intre NPC-uri;
7. incarca semantic context read-only;
8. ruleaza questurile de smoke;
9. verifica story events si story state;
10. exporta debugdump;
11. sterge sau reseteaza fixture-ul.

Comenzi tinta:

```text
/ainpc world fixture plan
/ainpc world outside validate test_padure_veche
/ainpc world outside validate test_cripta_lupilor
/ainpc audit world
/ainpc world settlement plan test_sat_central
/ainpc world bindings npc test_npc_lider
/ainpc quest anchors
/ainpc audit quest
/ainpc story context test_sat_central
/ainpc story events test_sat_central
/ainpc debugdump world
/ainpc debugdump quest
/ainpc debugdump story
```

Comenzile pot fi diferite in implementare, dar testul trebuie sa poata inspecta aceleasi contracte: mapping, cladiri, NPC-uri, relatii, context semantic, questuri si story events.

## Criterii de acceptare

Schema este acceptabila cand:

- exista un singur `scenario.id` stabil;
- `enabled_by_default=false`;
- toate ID-urile folosesc prefixul `test_`;
- satul are numarul asteptat de cladiri;
- structurile exterioare au numarul asteptat si tip semantic valid;
- NPC-urile au numar fix si roluri validate;
- fiecare NPC are home/work/social unde este cazul;
- relatiile sunt explicite si inspectabile;
- contextul semantic este scurt si validat;
- questurile folosesc obiective existente;
- story events sunt inregistrate doar prin actiuni validate;
- `audit world`, `audit quest` si debugdump pot verifica scenariul;
- resetul fixture-ului nu sterge date reale.

## Ce ramane pentru mai tarziu

Nu se include in primul slice:

- generare AI libera de NPC-uri;
- conversie automata in harta finala;
- WorldEdit build fizic;
- spawn automat de mobi;
- economie functionala;
- reputatie complexa;
- branching narativ avansat;
- relatii dinamice mutate de AI fara validator;
- salvare permanenta in core ca scenariu oficial.

Primul slice trebuie sa livreze documentatia si, eventual, un plan read-only. Implementarea de create/spawn/story live vine doar dupa validare si ramane opt-in.
