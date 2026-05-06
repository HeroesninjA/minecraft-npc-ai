# Pregatire pentru Questuri Avansate

Actualizat: 2026-05-06

## Scop

Acest document defineste pregatirile minime inainte de implementarea questurilor avansate in AINPC.

Obiectivul nu este sa rescriem sistemul de questuri. Obiectivul este sa stabilizam contractul curent astfel incat sa putem adauga questuri mai variate fara sa rupem Q01-Q05, progresul persistent sau smoke testele Paper.

Questuri avansate inseamna, in prima etapa:

- obiective stabile, identificabile prin ID explicit;
- obiective combinate in acelasi quest;
- tinte semantice prin `region`, `place`, `node` si `npc`;
- reward-uri controlate pentru iteme si story state;
- audit clar pentru pack-uri invalide;
- smoke test predictibil pe server.

Nu inseamna inca:

- branching complex;
- auto-start sau QuestDirector autonom;
- conditii dinamice pe obiective;
- actiuni AI care modifica direct progresul sau reward-urile.

## Verdict de pornire

Codul actual este suficient pentru questuri avansate de baza si pentru stages liniare initiale, dar nu este suficient pentru branching complex sau QuestDirector autonom.

Se poate incepe imediat cu questuri care combina tipurile deja suportate:

- `collect_item`
- `deliver_to_npc`
- `talk_to_npc`
- `visit_region`
- `visit_place`
- `inspect_node`
- `kill_mob`

Pentru questuri cu etape reale exista deja baza initiala:

- `objective_id` explicit si stabil prin cheia YAML;
- reguli de compatibilitate pentru progresul existent;
- `quest.stages` incarcat in `QuestStageDefinition`;
- runtime care progreseaza doar etapa activa;
- `next_stage` explicit pentru tranzitii liniare intre stages;
- persistenta `player_quests.current_stage_id`;
- `quest_variables` retine stage-ul curent, stage-ul anterior si stage-urile completate pentru debug/hook-uri viitoare;
- audit/debugdump pentru stage IDs, objective IDs, `completion_mode` si `next_stage`.

Ramane de pregatit pentru questuri avansate complete:

- smoke test Paper extins pentru mapping, ancore si stages;
- separare semantica intre `current_stage_id` si `current_phase`, daca fazele narative se separa de stages runtime;
- hook-uri intermediare pe obiectiv/stage;
- branching si reward resolver general;
- separare treptata a runtime-ului de quest din `ScenarioEngine`.

## Starea curenta relevanta

Fisiere principale:

- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/FeaturePackLoader.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/QuestScenarioContract.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/QuestAnchorResolver.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/listeners/QuestObjectiveListener.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/database/DatabaseManager.java`
- `ainpc-scenario-medieval/src/main/resources/packs/medieval_quest.yml`
- `scripts/smoke-paper-quests.ps1`

Ce exista deja:

- questuri definite in YAML sub `scenarios`;
- contract runtime prin `QuestScenarioContract`;
- progres persistent in `player_quests`;
- ancore semantice persistente in `quest_anchor_bindings`;
- audit pentru quest templates si binding-uri;
- listener pentru inventory, miscare, discutie cu NPC si kill;
- tracking pentru urmatorul obiectiv;
- story actions finale: `set_story_state` si `record_story_event`.

Limitari care conteaza:

- exista runtime initial pentru mai multe questuri curente per jucator, iar `quest status` si `quest track` pot folosi selector explicit de quest;
- exista categorii `main`, `side`, `repeatable` cu limite initiale prin `quest.max_active`;
- questul urmarit este persistat in `player_quests.tracked` si restaurat la load;
- `phases` pot functiona initial ca stage IDs runtime pentru questuri etapizate;
- obiectivele pot fi plate sau grupate prin metadata `phase`/`stage` si `quest.stages.<stage>.objectives`;
- `objective_key` nou prefera cheia YAML stabila, cu fallback pentru cheia legacy `type:item:index`;
- `FeaturePackLoader` incarca `quest.stages`, inclusiv `next_stage`;
- tranzitiile de stage sunt reflectate in `quest_variables` pentru observabilitate si pregatirea hook-urilor;
- nu exista hook-uri intermediare pe obiectiv sau stage;
- reward-ul final nu are inca ledger/idempotency dedicat.

## Principiu de implementare

Fiecare pas nou trebuie sa respecte aceasta ordine:

```text
schema YAML
-> loader
-> audit
-> runtime deterministic
-> persistenta
-> comanda/debug
-> test local
-> smoke Paper
```

Nu se adauga un tip nou de obiectiv doar in YAML. Un obiectiv nou este acceptabil numai daca are:

- parser in `FeaturePackLoader`;
- validare in `/ainpc audit quest`;
- handler runtime sau comportament explicit de no-op blocat;
- progres persistent;
- test sau smoke checklist.

## Pregatirea 1 - Objective ID stabil

Status: implementat initial in runtime pe 2026-05-05.

Problema actuala:

```text
objective_key = type:item:index
```

Aceasta cheie se schimba daca autorul:

- reordoneaza obiectivele;
- schimba `item`;
- transforma `collect_item` in `deliver_to_npc`;
- introduce un obiectiv nou intre doua obiective existente.

Directia corecta:

```yml
objectives:
  collect_iron:
    type: "collect_item"
    item: "IRON_INGOT"
    amount: 3
```

Cheia YAML `collect_iron` devine `objective_id`.

Reguli:

- `objective_id` este cheia YAML a obiectivului;
- trebuie sa fie unic in quest;
- trebuie sa fie stabil intre versiuni;
- nu se redenumeste pe server live fara migrare;
- progresul nou foloseste `objective_id`;
- progresul vechi pe `type:item:index` ramane citibil ca fallback.

Compatibilitate recomandata:

```text
read progress by objective_id
if missing:
  read legacy key type:item:index
  optionally persist migrated key on next update
```

Criterii de acceptare:

- Q01-Q05 continua sa se completeze;
- `quest_anchor_bindings.objective_key` foloseste ID-ul stabil pentru progres nou;
- `/ainpc quest anchors` arata ID-uri lizibile;
- `/ainpc audit quest` detecteaza duplicate sau ID-uri goale;
- testele Maven raman verzi.

Implementarea initiala acopera:

- `FeaturePackLoader.QuestEntryDefinition#getEntryId()`;
- `ScenarioEngine` foloseste `entry_id` ca `objective_key` principal;
- citirea progresului foloseste fallback pe cheia legacy `type:item:index`;
- progresul de inventory migreaza spre cheia stabila la urmatorul snapshot;
- progresul de `talk_to_npc`, `visit_*`, `inspect_node` si `kill_mob` copiaza valoarea legacy cand actualizeaza obiectivul;
- `QuestAnchorResolver` foloseste `entry_id` pentru `quest_anchor_bindings` noi;
- tracking-ul cauta ancore atat dupa cheia stabila, cat si dupa cheia legacy.
- `/ainpc audit quest` avertizeaza cand gaseste `objective_progress` sau `quest_anchor_bindings.objective_key` in format legacy.

Ramane pentru faza urmatoare:

- migrare explicita DB pentru `player_quests.objective_progress` si `quest_anchor_bindings.objective_key`;
- test unitar dedicat pentru fallback-ul de progres legacy din `ScenarioEngine`;
- repair/migration command explicit pentru datele legacy persistate.

## Pregatirea 2 - Audit strict pentru continut avansat

Status: implementat initial in `/ainpc audit quest` pe 2026-05-05.

Auditul trebuie sa blocheze sau sa marcheze clar continutul care nu poate rula.

Validari necesare:

| Zona | Regula |
|---|---|
| Objective ID | unic, stabil, fara caractere fragile |
| Tip obiectiv | trebuie sa fie suportat de runtime |
| Material | valid pentru `collect_item` si `deliver_to_npc` |
| Entity | valid pentru `kill_mob` |
| Mapping reference | `tag:`, `type:`, `region:`, `place:`, `node:` sau valoare directa |
| Story action | `scope`, `target`, `state` sau `event_type` clar |
| Repeatable | cere `cooldown_seconds` |
| Prerequisite | indica quest existent |
| Dialog | cel putin `offer`, `accepted`, `active`, `ready`, `completed` |

Regula importanta:

Un pack cu obiective necunoscute poate fi incarcat pentru authoring doar daca runtime-ul refuza pornirea questului si auditul raporteaza problema clar.

Criterii de acceptare:

- `/ainpc audit quest` distinge intre warning si error;
- obiectivele necunoscute apar cu quest, objective_id si type;
- reward-urile necunoscute nu sunt tratate automat ca item;
- story actions incomplete sunt error pentru questuri active.

Implementarea initiala acopera:

- ID-uri goale sau fragile pentru obiective si recompense;
- tipuri de obiectiv nesuportate ca erori;
- tipuri de recompensa nesuportate ca erori;
- separarea dintre normalizarea obiectivelor si normalizarea reward-urilor;
- referinte semantice cu prefix neobisnuit ca warning;
- story actions fara `scope`, `target`, `state` sau `event_type` ca erori.

Ramane pentru faza urmatoare:

- test unitar dedicat pentru raportul de audit;
- audit strict configurabil pe mod server, de exemplu `warn` in authoring si `error` in production;
- blocare runtime explicita pentru questuri cu tipuri necunoscute, nu doar raport de audit.

## Pregatirea 3 - Primele questuri avansate cu stages liniare

Status: implementare initiala pe 2026-05-06 cu Q06, Q07 si Q08 in pack-ul medieval.

Primele questuri avansate folosesc obiective combinate si stages liniare. Obiectivele raman declarate in `quest.objectives`, iar `quest.stages` grupeaza cheile YAML stabile ale obiectivelor.

Exemplu Q06:

```yml
Q06:
  name: "Urmele De La Forja"
  base_type: "QUEST"
  requires_player: true
  quest:
    code: "Q06"
    giver_profession: "blacksmith"
    kind: "exploration"
    acceptance_mode: "explicit"
    completion_mode: "return_to_giver"
    tracking_mode: "next_objective"
    tags: ["exploration", "blacksmith", "mapping", "story"]
    prerequisites:
      - "Q01"
    stages:
      INVESTIGATION:
        description: "Jucatorul merge la forja si inspecteaza punctul suspect."
        completion_mode: "all_objectives"
        objectives:
          - "visit_forge"
          - "inspect_anvil"
      RETURN:
        description: "Jucatorul revine la fierar."
        completion_mode: "manual_turn_in"
        objectives:
          - "report_to_blacksmith"
    objectives:
      visit_forge:
        type: "visit_place"
        item: "tag:blacksmith"
        amount: 1
        phase: "INVESTIGATION"
        description: "Mergi la forja."
      inspect_anvil:
        type: "inspect_node"
        item: "type:anvil"
        amount: 1
        phase: "INVESTIGATION"
        description: "Inspecteaza nicovala."
      report_to_blacksmith:
        type: "talk_to_npc"
        item: "profession:blacksmith"
        amount: 1
        phase: "RETURN"
        description: "Raporteaza fierarului ce ai gasit."
    rewards:
      forge_event:
        type: "record_story_event"
        item: "forge_inspected"
        amount: 1
        scope: "place"
        target: "anchor:visit_forge"
        event_type: "quest_completed"
        event_key: "q06_forge_inspected"
        description: "Satul retine ca forja a fost inspectata."
```

Observatii:

- `visit_forge` si `inspect_anvil` cer mapping valid;
- `report_to_blacksmith` foloseste NPC/profesie, nu nume fragil;
- reward-ul story ruleaza doar la final;
- ordinea este controlata de etapa activa: obiectivele din `RETURN` nu progreseaza inainte de etapa de raportare;
- tracking-ul poate indica urmatorul obiectiv incomplet.

Questuri recomandate pentru prima runda:

| Quest | Intentie | Obiective |
|---|---|---|
| Q06 | explorare/mapping | `visit_place`, `inspect_node`, `talk_to_npc` |
| Q07 | livrare sociala | `collect_item`, `talk_to_npc`, `deliver_to_npc` |
| Q08 | hunt contextual simplu | `visit_region`, `kill_mob`, `talk_to_npc` |

Implementarea initiala acopera:

- Q06 `Urme La Forja` in `ainpc-scenario-medieval`;
- Q07 `Mesaj Pentru Straja` in `ainpc-scenario-medieval`;
- Q08 `Patrula De Hotar` in `ainpc-scenario-medieval`;
- Q06-Q08 au metadata `phase` pe obiective si `quest.stages` explicite;
- stage-urile Q06-Q08 folosesc `all_objectives` pentru etapa de lucru si `manual_turn_in` pentru etapa de raportare;
- etapa de lucru din Q06-Q08 declara `next_stage: RETURN`, iar runtime-ul respecta tranzitia explicita cand etapa este completa;
- runtime-ul scrie variabile de stage precum `stage.current`, `stage.previous`, `stage.completed.<stage>` si `stage.last_completed`;
- runtime-ul progreseaza doar etapa curenta, folosind atat metadata `phase`/`stage`, cat si lista `quest.stages.<stage>.objectives`;
- prerequisite `Q01` pentru Q06, `Q03` + `Q04` pentru Q07 si `Q03` pentru Q08;
- obiective plate combinate: `visit_place`, `visit_region`, `inspect_node`, `talk_to_npc`, `collect_item`, `deliver_to_npc`, `kill_mob`;
- reward `record_story_event` pe ancora `visit_forge`, pe regiunea curenta si pe ancora `patrol_region`;
- reward item mic pentru feedback imediat;
- selectie quest per jucator, ca giverii cu mai multe questuri sa poata avansa la urmatorul quest disponibil;
- interactiuni `talk_to_npc` cu NPC secundar pentru questul activ, cu finalizare blocata pana la revenirea la quest giver;
- `/ainpc audit quest` si `debugdump quest` valideaza stages, objective IDs, `completion_mode` si `next_stage`;
- testul `MedievalQuestPackTest` valideaza stages pentru Q06-Q08;
- smoke checklist extins pentru test Paper Q06, Q07 si Q08.

Ramane pentru faza urmatoare:

- test Paper real cu mapping demo;
- audit/export mai bun pentru story event-ul generat de Q06.

## Pregatirea 4 - Smoke test pentru questuri avansate

Smoke testul existent trebuie extins dupa Q06, Q07 si Q08.

Comenzi minime:

```text
ainpc audit quest
ainpc world whereami <player>
ainpc world places
ainpc quest nearest <player>
ainpc quest accept nearest <player>
ainpc quest track start <player>
ainpc quest status Q06 <player>
ainpc quest track start Q06 <player>
ainpc quest anchors <player>
ainpc quest status nearest <player>
ainpc story events
```

Pentru questuri cu mapping:

- jucatorul trebuie sa stea langa NPC-ul giver;
- regiunea trebuie sa existe;
- place-ul tinta trebuie sa aiba tag/type potrivit;
- node-ul tinta trebuie sa existe si sa fie detectabil;
- `/ainpc quest anchors` trebuie sa arate binding-uri pentru obiectivele semantice.

Criterii de acceptare Q06:

- questul este oferit doar daca mapping-ul necesar exista;
- daca lipseste mapping-ul, mesajul spune ce ancora lipseste;
- `visit_place` progreseaza cand jucatorul intra in place;
- `inspect_node` progreseaza cand jucatorul ajunge la node;
- `talk_to_npc` progreseaza la interactiunea cu NPC-ul potrivit;
- reward-ul `record_story_event` scrie un event verificabil.

Criterii de acceptare Q07:

- innkeeper-ul ofera Q07 dupa ce Q03 si Q04 sunt completate;
- daca Q04 repetabil este deja completat, nu blocheaza Q07 doar pentru ca are acelasi giver;
- `talk_to_npc` progreseaza la interactiunea cu garda, chiar daca garda nu este quest giver;
- finalizarea se face la hangiu, nu la NPC-ul secundar;
- reward-ul `record_story_event` regional este verificabil cand exista regiune curenta.

Criterii de acceptare Q08:

- garda ofera Q08 dupa ce Q03 este completat;
- `visit_region` se leaga de o regiune `type:settlement` si progreseaza in mapping demo;
- `kill_mob` progreseaza doar pentru mobii configurati;
- `talk_to_npc` inchide raportarea la garda;
- reward-ul `record_story_event` regional foloseste ancora `patrol_region`.

## Pregatirea 5 - Separare treptata din ScenarioEngine

`ScenarioEngine` poate ramane orchestratorul initial, dar logica de quest trebuie extrasa gradual.

Ordine recomandata:

1. `QuestRuntimeService` pentru acceptare, status, completare si abandon.
2. `QuestProgressStore` pentru `player_quests` si `quest_anchor_bindings`.
3. `ObjectiveProgressService` pentru update-uri din listener.
4. `QuestRewardService` pentru item rewards si story actions.
5. `QuestAuditService` pentru validare reutilizabila.

Regula:

Nu se face refactor mare inainte de Q06-Q08. Prima data stabilizam comportamentul, apoi extragem clase.

## Ce nu implementam inca

Se amana pana dupa Q06-Q08:

- branching;
- objective optional;
- failure rules;
- time limit;
- reward ledger dedicat;
- custom objective API public;
- QuestDirector autonom;
- authoring AI care activeaza continut automat.

Motivul este simplu: toate acestea schimba contractul de progres. Daca le adaugam inainte de ID-uri stabile si smoke tests, vom face migrarea mai riscanta.

## Ordine de lucru recomandata

### Pasul A - Stabilizare ID obiective

- adauga `entry_id`/`objective_id` in modelul `QuestEntryDefinition`;
- foloseste cheia YAML ca ID;
- inlocuieste `buildObjectiveKey` cu ID stabil;
- pastreaza fallback pentru cheile legacy;
- actualizeaza `QuestAnchorResolver`;
- actualizeaza auditul.

### Pasul B - Teste locale

- test pentru loader: cheia YAML devine ID;
- test pentru cheie legacy citita ca fallback;
- test pentru anchor resolver cu ID stabil;
- test pentru audit duplicate objective IDs;
- `mvn test`.

### Pasul C - Q06 in pack medieval

- adauga Q06 dupa Q01-Q05;
- prerequisite recomandat: `Q01`;
- obiective: `visit_place`, `inspect_node`, `talk_to_npc`;
- reward: `record_story_event` plus item mic optional;
- dialoguri complete;
- audit curat.

### Pasul C2 - Q07 in pack medieval

- adauga Q07 dupa Q06;
- prerequisites recomandate: `Q03` si `Q04`;
- obiective: `collect_item`, `talk_to_npc`, `deliver_to_npc`;
- reward: `record_story_event` regional plus item mic optional;
- selectorul de quest trebuie sa aleaga urmatorul quest disponibil pentru acelasi giver;
- interactiunea cu NPC secundar trebuie sa progreseze obiectivul social fara sa finalizeze questul.

### Pasul C3 - Q08 in pack medieval

- adauga Q08 dupa Q07;
- prerequisite recomandat: `Q03`;
- obiective: `visit_region`, `kill_mob`, `talk_to_npc`;
- reward: `record_story_event` regional plus item mic optional;
- `visit_region` trebuie sa foloseasca o referinta mapabila pe demo, de exemplu `type:settlement`;
- selectorul de quest trebuie sa treaca de Q03 completat si sa ofere Q08.

### Pasul D - Smoke Paper

- ruleaza scriptul de smoke;
- porneste serverul cu core si addon medieval;
- ruleaza `/ainpc audit quest`;
- testeaza Q01 pentru regresie;
- testeaza Q06 cu mapping demo sau mapping real;
- testeaza Q07 cu innkeeper + guard;
- testeaza Q08 cu guard + regiune settlement + combat.

## Definitia de gata

Pregatirea este gata cand:

- `mvn test` trece;
- Q01-Q05 continua sa functioneze;
- cheile de obiectiv sunt stabile si lizibile;
- `/ainpc audit quest` prinde obiective/reward-uri invalide;
- Q06-Q08 pot fi adaugate fara schimbari majore de runtime;
- mapping-ul lipsa blocheaza questul cu mesaj explicit;
- `quest_anchor_bindings` foloseste ID-uri stabile;
- exista smoke checklist pentru Q06, Q07 si Q08.

## Legatura cu documentele existente

- `questuri-faza-1-stabilizare.md` ramane documentul pentru Q01-Q05.
- `questuri-avansate.md` ramane ghidul complet de evolutie.
- `quest-anchor-bindings.md` ramane contractul pentru ancore persistente.
- Acest document este puntea practica dintre stabilizarea Q1 si implementarea Q06-Q08.
