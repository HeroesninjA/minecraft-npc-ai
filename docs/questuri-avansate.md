# Questuri Avansate - Ghid de evolutie

Actualizat: 2026-05-04

## Scop

Acest document explica cum se avanseaza sistemul de questuri din AINPC de la questuri simple de tip "adu iteme" la questuri mai variate: explorare, social, lupta, livrare, investigatie, crafting, story state si questuri pe etape.

Obiectivul nu este o rescriere brusca. Obiectivul este o evolutie controlata:

- pastrezi Q01-Q05 functionale;
- adaugi tipuri noi de obiective doar cand au listener, validare si persistenta;
- folosesti mapping semantic pentru regiuni, places si nodes;
- separi treptat quest runtime-ul din `ScenarioEngine`;
- ajungi la questuri multi-etapa, cu mai multe questuri active si progres persistent clar.

## Starea curenta

Fisiere relevante:

- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/FeaturePackLoader.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/QuestScenarioContract.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/QuestAnchorResolver.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/listeners/QuestObjectiveListener.java`
- `ainpc-core-plugin/src/main/resources/quests.yml`
- `ainpc-scenario-medieval/src/main/resources/packs/medieval_quest.yml`
- `docs/quest-anchor-bindings.md`
- `docs/questuri-faza-1-stabilizare.md`

Ce exista deja:

- questurile sunt definite in YAML sub `scenarios`, cu `base_type: "QUEST"`;
- fiecare quest are `quest.code`, `giver_profession`, `objectives`, `rewards`;
- exista contract runtime prin `QuestScenarioContract`: `kind`, `acceptance_mode`, `completion_mode`, `tracking_mode`, `tags`;
- exista progres persistent in `player_quests`;
- exista `quest_anchor_bindings` pentru obiective semantice legate de `region`, `place`, `node` si `npc`;
- exista `QuestAnchorResolver` pentru rezolvarea ancorelor inainte de runtime;
- exista `StoryStateService` si actiuni de reward controlate: `set_story_state`, `record_story_event`;
- `/ainpc audit quest` valideaza initial template-uri, binding-uri, materiale, entitati, profesii si prerequisite-uri;
- `scripts/smoke-paper-quests.ps1` pregateste smoke test-ul Paper pentru questuri.

Tipuri de obiective suportate acum in runtime:

- `collect_item`
- `deliver_to_npc`
- `talk_to_npc`
- `visit_region`
- `visit_place`
- `inspect_node`
- `kill_mob`

Tipuri de reward suportate acum:

- `item`
- `set_story_state`
- `record_story_event`

Limitari importante:

- `ScenarioEngine` tine progres curent per jucator si template, iar `quest status`/`quest track` pot selecta explicit `quest_code` sau `template_id`;
- `phases` exista in YAML, dar sunt inca informative, nu etape runtime reale;
- obiectivele sunt plate, nu grupate pe stage;
- `objective_key` este generat din tip, item si index, nu vine inca dintr-un `objective_id` explicit;
- parserul `FeaturePackLoader` nu incarca inca `quest.stages`;
- nu exista branching real, conditii pe obiective sau reward resolver general.

## Directia corecta

Un quest avansat trebuie sa aiba cinci straturi clare:

| Strat | Rol |
|---|---|
| Definitie | YAML-ul static: cod, nume, giver, tip, obiective, recompense, dialoguri |
| Rezolvare | Alegerea ancorelor reale: NPC, region, place, node |
| Progres | Starea jucatorului: status, etapa, obiective, variabile |
| Reactie | Dialog, tracking, mesaje, story state, reputatie |
| Audit | Validare template, validare binding, debug si smoke test |

Regula de evolutie:

- defineste intai questul ca date;
- valideaza datele la load si prin `/ainpc audit quest`;
- rezolva semantic tintele inainte sa pornesti questul;
- scrie progresul in DB;
- actualizeaza progresul prin evenimente controlate;
- finalizeaza questul printr-un singur punct de runtime.

## Catalog de questuri variate

Pentru mai multa varietate, nu inventa doar alte iteme. Variaza intentia, locatia, NPC-urile, riscul, timpul si efectul asupra lumii.

| Tip quest | `kind` recomandat | Obiective potrivite | Exemplu medieval |
|---|---|---|---|
| Fetch | `fetch` | `collect_item`, `deliver_to_npc` | Fierarul cere lingouri si scanduri |
| Hunt | `hunt` | `kill_mob`, `kill_mob_in_region` | Garda cere curatarea drumului |
| Exploration | `exploration` | `visit_region`, `visit_place`, `inspect_node` | Preotul trimite jucatorul la o cripta |
| Social | `social` | `talk_to_npc`, `talk_to_role`, `choose_dialogue_branch` | Hangiul cere informatii de la calatori |
| Delivery | `delivery` | `collect_item`, `visit_place`, `deliver_to_npc` | Negustorul trimite un pachet la han |
| Crafting | `crafting` | `collect_item`, `craft_item`, `smelt_item` | Fierarul cere o piesa prelucrata |
| Investigation | `investigation` | `talk_to_npc`, `inspect_node`, `visit_place` | Fermierul cere aflarea cauzei unei boli |
| Escort | `escort` | `escort_npc`, `visit_place`, `survive_duration` | Garda insoteste un martor la poarta |
| Defense | `defense` | `stay_in_region`, `kill_mob_in_region`, `survive_duration` | Satul este atacat noaptea |
| Reputation | `reputation` | mix de obiective, reward de reputatie | Satul acorda incredere dupa ajutor repetat |
| Story | `story` | `trigger_story_flag`, `set_story_state` ca reward | Cripta ramane purificata dupa quest |

## Matrice pentru varietate

Cand adaugi questuri noi, combina cel putin doua axe din lista de mai jos.

| Axa | Variante |
|---|---|
| Sursa | fierar, fermier, garda, hangiu, vindecator, preot, negustor |
| Locatie | sat, han, fierarie, camp, poarta, padure, cripta, drum |
| Actiune | aduna, livreaza, vorbeste, inspecteaza, vaneaza, apara, craftuieste |
| Tensiune | timp limitat, pericol, alegere morala, prerequisite, cooldown |
| Efect | item, story state, reputatie, relatie NPC, unlock quest |

Exemple rapide:

- Fetch simplu: "Adu 8 grau fermierului."
- Fetch cu locatie: "Adu grau la hambar, nu direct la NPC."
- Delivery social: "Ia mesajul de la hangiu si du-l garzii."
- Explorare: "Gaseste forja veche si inspecteaza nicovala crapata."
- Investigatie: "Vorbeste cu fermierul, inspecteaza campul, apoi raporteaza vindecatorului."
- Lupta contextualizata: "Ucide zombie doar langa drumul de nord."
- Story quest: "Dupa inspectarea criptei, seteaza regiunea in starea `crypt_disturbed`."

## Reguli pentru questuri hibride si flexibile

Questurile nu trebuie sa fie limitate la un singur tip rigid. Un quest bun poate combina mai multe mecanici, de exemplu:

- aduna iteme si viziteaza un loc;
- vorbeste cu un NPC si inspecteaza un node;
- ucide mobi intr-o zona si raporteaza garzii;
- craftuieste un item si livreaza-l la un NPC;
- viziteaza un loc, inspecteaza un obiect si scrie story state.

Regula principala: `kind` descrie intentia dominanta a questului, iar obiectivele si `tags` descriu mecanicile combinate.

Exemplu:

```yml
kind: "delivery"
tags: ["fetch", "visit_place", "blacksmith", "mapping"]
```

In acest caz, questul este in principal o livrare, dar foloseste si adunare de iteme si vizitare de loc.

### Cum alegi combinatia

Foloseste acest tabel cand decizi forma unui quest.

| Intrebare | Alegere recomandata |
|---|---|
| Care este scopul principal pentru jucator? | pune asta in `kind` |
| Ce mecanici secundare apar? | pune-le in `tags` si obiective |
| Obiectivele pot fi facute in orice ordine? | foloseste obiective plate in `quest.objectives` |
| Obiectivele trebuie facute intr-o ordine stricta? | foloseste `quest.stages` cand runtime-ul le suporta |
| Questul trebuie reutilizat in mai multe sate? | foloseste `tag:` si `type:`, nu ID-uri fixe |
| Questul este de poveste fixa? | foloseste `region:`, `place:` sau `node:` exacte |
| Questul are o alegere reala? | foloseste branching doar dupa stages |
| Questul are efect asupra lumii? | foloseste reward `set_story_state` sau `record_story_event` |

### Retete de questuri combinate

| Combinatie | Obiective | Exemplu |
|---|---|---|
| Fetch + locatie | `collect_item`, `visit_place`, `deliver_to_npc` | aduna lemn si du-l la forja |
| Delivery + social | `collect_item`, `talk_to_npc`, `deliver_to_npc` | ia scrisoarea si cauta gardianul |
| Investigatie | `talk_to_npc`, `visit_place`, `inspect_node` | afla zvonul, mergi la camp, inspecteaza urmele |
| Hunt + mapping | `visit_region`, `kill_mob` | mergi la drumul de nord si curata zona |
| Crafting + delivery | `collect_item`, `craft_item`, `deliver_to_npc` | fa bandaje si du-le vindecatorului |
| Story + explorare | `visit_place`, `inspect_node`, reward story | inspecteaza cripta si marcheaza starea locului |
| Reputatie + social | `talk_to_npc`, reward `reputation` | impaca doua NPC-uri si castiga incredere |

### Reguli de compozitie

1. Alege un singur `kind` principal.
2. Pune mecanicile secundare in `tags`.
3. Nu crea un `objective.type` nou daca poti combina tipuri existente.
4. Fiecare obiectiv are o cheie YAML stabila: `collect_wood`, `visit_forge`, `deliver_supplies`.
5. Pentru continut reutilizabil, prefera `tag:blacksmith`, `tag:farm`, `type:inspect_node`.
6. Pentru poveste fixa, foloseste referinte exacte: `place:satul_central:fierarie`.
7. Daca ordinea nu conteaza, pastreaza obiectivele plate.
8. Daca ordinea conteaza, muta questul spre `quest.stages`.
9. Reward-ul final se aplica o singura data, indiferent cate mecanici are questul.
10. Daca obiectivul este optional, marcheaza-l explicit doar cand runtime-ul suporta obiective optionale; pana atunci fa-l quest separat sau stage separat.

### Limite recomandate

Pentru questuri mici:

- 2-3 obiective;
- maximum 2 mecanici diferite;
- un singur NPC tinta;
- reward simplu.

Pentru questuri medii:

- 3-5 obiective;
- 2-3 mecanici;
- mapping semantic;
- story event optional.

Pentru questuri mari:

- foloseste `quest.stages`;
- foloseste branch rules;
- foloseste failure rules;
- testeaza cu smoke test dedicat.

### Exemplu: item plus vizitare loc in acelasi quest

Acest exemplu este compatibil cu directia runtime curenta: are obiective plate, dar combina `collect_item`, `visit_place` si `deliver_to_npc`.

```yml
scenarios:
  Q11:
    name: "Provizii La Forja"
    description: "Fierarul cere materiale si vrea ca jucatorul sa le aduca direct la forja."
    base_type: "QUEST"
    requires_player: true
    hint: "Fierarul are nevoie de lemn la forja."
    phases:
      INTRODUCTION: "Fierarul explica lipsa de provizii."
      GATHERING: "Jucatorul strange materialele."
      DELIVERY: "Jucatorul merge la forja."
      COMPLETION: "Fierarul primeste proviziile."
    quest:
      code: "Q11"
      giver_profession: "blacksmith"
      kind: "delivery"
      acceptance_mode: "explicit"
      completion_mode: "return_to_giver"
      tracking_mode: "next_objective"
      tags: ["fetch", "delivery", "visit_place", "blacksmith", "mapping"]
      objectives:
        collect_planks:
          type: "collect_item"
          item: "OAK_PLANKS"
          amount: 8
          description: "Strange 8 scanduri de stejar."
        visit_forge:
          type: "visit_place"
          item: "tag:blacksmith"
          amount: 1
          description: "Mergi la forja."
        deliver_planks:
          type: "deliver_to_npc"
          item: "OAK_PLANKS"
          amount: 8
          description: "Preda scandurile fierarului."
      rewards:
        emerald:
          type: "item"
          item: "EMERALD"
          amount: 2
          description: "Primesti 2 smaralde."
        forge_supplied_event:
          type: "record_story_event"
          item: "forge_supplied"
          amount: 1
          description: "Satul retine ca forja a primit provizii."
          scope: "place"
          target: "current_place"
          event_type: "quest_completed"
          event_key: "q11_forge_supplied"
          title: "Forja a primit provizii"
          payload:
            quest: "Q11"
```

Observatii:

- `collect_planks` verifica itemele;
- `visit_forge` obliga jucatorul sa ajunga la locul semantic potrivit;
- `deliver_planks` pastreaza finalizarea naturala la NPC;
- `kind: delivery` ramane clar, chiar daca questul contine si fetch si mapping;
- `tags` permit filtrare, audit si recomandari viitoare prin `QuestDirector`.

### Exemplu: acelasi quest reutilizabil in mai multe scenarii

Pentru a aplica acelasi model in sate diferite, nu lega questul de coordonate sau ID-uri fixe.

Varianta reutilizabila:

```yml
objectives:
  visit_workplace:
    type: "visit_place"
    item: "tag:blacksmith"
    amount: 1
    description: "Mergi la locul de munca al fierarului."
```

Varianta fixa, doar pentru poveste:

```yml
objectives:
  visit_old_forge:
    type: "visit_place"
    item: "place:satul_central:forja_veche"
    amount: 1
    description: "Mergi la forja veche din satul central."
```

Regula: foloseste varianta reutilizabila pentru questuri generice si varianta fixa pentru questuri de campanie sau lore.

### Cand separi in doua questuri

Nu combina totul intr-un singur quest. Separa in questuri diferite daca:

- fiecare parte are reward propriu;
- fiecare parte poate fi abandonata separat;
- obiectivele apartin unor NPC-uri diferite fara legatura clara;
- questul devine prea lung pentru un flow simplu;
- o parte trebuie sa fie repeatable si alta nu;
- o parte schimba story state major.

Exemplu:

- Q11A: aduna materialele pentru fierar;
- Q11B: investigheaza de ce forja consuma prea multe materiale.

Prima este delivery/fetch. A doua este investigation/story.

## Evenimente intermediare in questuri

Evenimentele intermediare trebuie sa treaca prin `QuestEngine`, nu sa fie executate direct din listener, comanda sau AI.

Flux recomandat:

```text
Paper event
-> QuestObjectiveListener
-> QuestEngine
-> ObjectiveHandler
-> update objective_progress
-> emit QuestEvent
-> run intermediate actions
-> save progress / story event
```

Exemplu concret pentru un quest cu `collect_item`, `visit_place` si `inspect_node`:

```text
1. Jucatorul strange itemul
   -> OBJECTIVE_PROGRESS collect_planks
   -> optional: mesaj "Ai strans materialele"

2. Jucatorul ajunge la forja
   -> OBJECTIVE_COMPLETED visit_forge
   -> event intermediar: record_story_event q11_reached_forge

3. Jucatorul inspecteaza nicovala
   -> OBJECTIVE_COMPLETED inspect_anvil
   -> event intermediar: seteaza quest variable anvil_checked=true

4. Jucatorul revine la NPC
   -> QUEST_COMPLETED
   -> reward final
```

Regula importanta: evenimentele intermediare nu sunt reward-uri finale. Ele sunt actiuni controlate pe obiectiv sau stage.

Schema pentru obiectiv:

```yml
objectives:
  visit_forge:
    type: "visit_place"
    item: "tag:blacksmith"
    amount: 1
    description: "Mergi la forja."
    on_complete:
      - type: "record_story_event"
        event_key: "q11_reached_forge"
        scope: "place"
        target: "current_place"
```

Schema pentru stages, cand runtime-ul suporta etape reale:

```yml
stages:
  gather:
    objectives:
      collect_planks:
        type: "collect_item"
        item: "OAK_PLANKS"
        amount: 8
    on_complete:
      - type: "record_story_event"
        event_key: "q11_materials_ready"

  deliver:
    objectives:
      visit_forge:
        type: "visit_place"
        item: "tag:blacksmith"
        amount: 1
    on_start:
      - type: "message"
        text_key: "quest.q11.go_to_forge"
```

Hook-uri recomandate:

| Hook | Cand ruleaza |
|---|---|
| `on_start` | cand questul sau stage-ul incepe |
| `on_objective_progress` | cand un obiectiv progreseaza |
| `on_objective_complete` | cand un obiectiv ajunge la amount necesar |
| `on_stage_complete` | cand stage-ul curent este complet |
| `on_fail` | cand questul esueaza |
| `on_abandon` | cand jucatorul abandoneaza |
| `on_complete` | cand questul se finalizeaza |

Reguli:

- hook-urile sunt validate in `/ainpc audit quest`;
- fiecare actiune intermediara are `idempotency_key` sau cheia derivata din `quest_id + hook + objective_id`;
- o actiune intermediara nu ruleaza de doua ori dupa restart;
- hook-urile folosesc registry de actiuni cunoscute;
- actiunile intermediare sunt vizibile in debug/event log;
- reward-ul final ramane separat de actiunile intermediare.

Tipuri sigure pentru actiuni intermediare:

| Actiune | Utilizare |
|---|---|
| `message` | mesaj scurt catre jucator |
| `record_story_event` | noteaza un eveniment narativ |
| `set_quest_variable` | seteaza progres narativ local questului |
| `set_story_state` | schimba stare de lume, doar cand este validat |
| `track_next_objective` | schimba obiectivul afisat |
| `unlock_hint` | face vizibil un hint |

Ce trebuie evitat:

- reward-uri finale in `on_objective_complete`;
- comenzi server arbitrare din YAML;
- actiuni generate liber de AI;
- schimbari de branch fara stage si audit;
- story state permanent fara scope si target clar.

## Story layer necesar pentru questuri

Pentru questuri avansate este nevoie de un story layer, dar nu de un story engine liber si generativ la inceput. Minimul necesar este un serviciu determinist si auditabil care poate salva evenimente si stari de lume.

Flux minim:

```text
Quest complete / objective complete / stage complete
-> record_story_event
-> set_story_state
-> StoryContextService citeste starea
-> NPC/AI/debug foloseste context read-only
```

Ce trebuie implementat initial:

```text
StoryStateService
- setRegionState(...)
- setPlaceState(...)
- recordStoryEvent(...)
- getStoryContextForPlayer(...)
```

Actiuni de quest:

```yml
rewards:
  forge_event:
    type: "record_story_event"
    event_key: "forge_supplied"
    scope: "place"
    target: "current_place"

  forge_state:
    type: "set_story_state"
    scope: "place"
    target: "current_place"
    state: "supplied"
```

Aceleasi actiuni pot fi folosite si intermediar, daca sunt validate:

```yml
on_stage_complete:
  - type: "record_story_event"
    event_key: "q11_reached_forge"
    scope: "place"
    target: "current_place"
```

Ce nu trebuie facut la inceput:

- story engine generativ care decide singur ce se intampla;
- branching liber condus de AI;
- story state modificat direct din dialog;
- lore rescris fara audit;
- reward-uri sau consecinte aplicate din text liber.

Ordine recomandata:

1. stabilizeaza `QuestEngine`;
2. implementeaza `StoryStateService` simplu;
3. pastreaza actiunile `record_story_event` si `set_story_state` in registry;
4. adauga `StoryContextService` read-only pentru NPC/AI/debug;
5. introdu hook-uri intermediare;
6. abia apoi adauga branching, campanii si story arcs.

Reguli:

- questurile modifica story state doar prin actiuni validate;
- AI-ul poate citi story context, dar nu modifica progresul direct;
- fiecare story event are `event_key`, `scope`, `target` si `payload` clar;
- story state-ul persistent trebuie sa fie inspectabil prin comenzi admin;
- auditul detecteaza story actions incomplete.

## Environment context si EnvironmentEngine

`EnvironmentEngine` nu este necesar acum ca motor separat. Pentru questuri avansate de baza este suficient:

Document canonic separat: `environment-context-si-engine.md`.

```text
QuestEngine
StoryStateService
World/Mapping context: region, place, node
```

Ce este necesar acum este un strat read-only de context, nu un engine complet care genereaza evenimente si questuri.

### Ce trebuie implementat acum

Recomandarea este un `EnvironmentContextService` minimal.

Responsabilitati:

```text
EnvironmentContextService
- current_world
- current_region
- current_place
- current_node_nearby
- current_time
- current_weather
- active_world_events
- story_state_snapshot
- settlement_snapshot optional
```

Rolul lui este sa raspunda la intrebari simple pentru questuri, story context si AI:

```text
Unde este jucatorul?
In ce regiune/place se afla?
Ce node este aproape?
Este noapte sau zi?
Exista world event activ?
Care este story state-ul locului?
```

Acest serviciu trebuie sa fie read-only. El nu decide questuri, nu modifica DB si nu aplica reward-uri.

Flux:

```text
Player location / world state
-> EnvironmentContextService
-> QuestEngine verifica obiective visit_region / visit_place / inspect_node
-> StoryContextService poate include snapshot-ul in prompt/debug
```

### Cand nu ai nevoie de EnvironmentEngine

Nu ai nevoie de `EnvironmentEngine` complet pentru questuri de tip:

- aduna iteme;
- livreaza iteme la NPC;
- vorbeste cu NPC;
- viziteaza un loc semantic;
- inspecteaza un node;
- omoara un mob simplu;
- seteaza story state la completare.

Exemplu:

```text
Adu 8 OAK_PLANKS si mergi la forja.
```

Acest quest merge cu:

```text
QuestEngine
Objective handlers
QuestAnchorResolver
WorldAdminApi / mapping
```

### Cand devine util EnvironmentEngine

`EnvironmentEngine` devine util mai tarziu, cand questurile trebuie generate sau activate din starea dinamica a lumii.

Exemple:

| Stare de mediu | Quest generat sau activat |
|---|---|
| satul are hrana sub prag | fermierul cere provizii |
| drumul de nord are multe atacuri | garda cere curatarea zonei |
| este noapte | apar questuri de aparare |
| este toamna | festivalul recoltei activeaza questuri |
| ploua de mult timp | fermierul cere verificarea campului |
| NPC important lipseste | apare quest de cautare |
| reputatia satului este mica | apar questuri de incredere |
| story state indica pericol | se activeaza quest de investigatie |

Aceste cazuri cer un motor care observa lumea, produce semnale si le expune catre `QuestDirector`.

Flux viitor:

```text
World state / simulation / weather / resources
-> EnvironmentEngine
-> EnvironmentSignal
-> QuestDirector
-> Quest recommendation sau QuestDraft
-> audit / validation
-> QuestEngine
```

### Diferenta dintre context si engine

| Componenta | Acum sau viitor | Rol |
|---|---|---|
| `EnvironmentContextService` | acum | citeste contextul curent read-only |
| `StoryContextService` | acum | citeste story state si evenimente pentru prompt/debug |
| `QuestEngine` | acum | ruleaza questuri si progres |
| `QuestDirector` | mai tarziu | recomanda questuri potrivite |
| `EnvironmentEngine` | viitor | observa lumea si produce semnale sistemice |

Regula: daca doar verifici unde este jucatorul, foloseste context. Daca generezi sau activezi questuri pe baza starii lumii, ai nevoie de engine.

### EnvironmentSignal

Cand `EnvironmentEngine` va exista, el nu trebuie sa creeze direct questuri active. Trebuie sa produca semnale.

Model:

```json
{
  "signal_type": "settlement_resource_low",
  "world_id": "overworld",
  "settlement_id": "satul_central",
  "severity": "moderate",
  "key": "food",
  "value": 18,
  "threshold": 20,
  "expires_at": 1760000000000
}
```

Semnalul este apoi consumat de `QuestDirector`, care poate recomanda un quest sau crea un draft validat.

Reguli:

- semnalul nu modifica progresul;
- semnalul nu acorda reward;
- semnalul are cooldown;
- semnalul are severitate;
- semnalul poate expira;
- semnalul este vizibil in debug.

### Conditii de quest bazate pe mediu

Chiar fara `EnvironmentEngine`, poti avea conditii simple bazate pe context, daca sunt read-only.

Exemplu:

```yml
conditions:
  can_start:
    - type: "environment:time_of_day"
      value: "night"
    - type: "story_state"
      scope: "region"
      target: "current_region"
      key: "road_danger"
      value: "active"
```

Reguli:

- conditiile de mediu se evalueaza prin `QuestRuleEngine`;
- ele nu modifica lumea;
- ele trebuie sa poata explica de ce nu trec;
- conditiile instabile, cum ar fi vremea, trebuie folosite cu grija;
- pentru questuri importante, evita ferestre prea scurte.

### Ordine recomandata de implementare

1. `QuestEngine`;
2. objective handlers;
3. `StoryStateService`;
4. hook-uri intermediare;
5. `EnvironmentContextService` read-only;
6. conditii read-only simple: timp, regiune, place, story state;
7. `QuestDirector`;
8. `EnvironmentSignal`;
9. `EnvironmentEngine` complet;
10. questuri sistemice generate din template-uri validate.

### Ce trebuie evitat

- sa implementezi `EnvironmentEngine` mare inainte de `QuestEngine`;
- sa generezi questuri direct din weather/time fara audit;
- sa lasi AI-ul sa modifice environment state;
- sa legi questuri importante de vreme temporara fara fallback;
- sa scanezi lumea constant pe main thread;
- sa creezi semnale fara cooldown;
- sa creezi questuri sistemice fara template validat.

### Criterii de acceptare pentru faza curenta

Pentru faza curenta este suficient daca:

- questurile pot afla regiunea/place-ul curent;
- `visit_region`, `visit_place` si `inspect_node` folosesc mapping semantic;
- story context poate include locul si starea lui;
- conditiile simple pot fi evaluate read-only;
- nu exista generatie automata de questuri din mediu.

### Criterii de acceptare pentru EnvironmentEngine viitor

`EnvironmentEngine` este gata doar cand:

- produce `EnvironmentSignal`, nu questuri active direct;
- semnalele au cooldown si expirare;
- semnalele sunt vizibile in debug;
- `QuestDirector` decide ce face cu semnalele;
- questurile generate folosesc template-uri validate;
- auditul poate explica ce semnal a activat recomandarea;
- sistemul nu scaneaza lumea excesiv;
- restartul pastreaza semnalele importante sau le reface determinist.

## Schema YAML curenta

Aceasta este schema sigura pentru runtime-ul actual. Foloseste-o pentru questurile care trebuie sa mearga acum pe server.

```yml
scenarios:
  Q06:
    name: "Urme La Forja"
    description: "Fierarul cere verificarea unui punct suspect din fierarie."
    base_type: "QUEST"
    requires_player: true
    hint: "Fierarul arata spre urme negre langa nicovala."
    preferred_topologies:
      - "village_center"
      - "interior"
    phases:
      INTRODUCTION: "Fierarul explica problema."
      INVESTIGATION: "Jucatorul inspecteaza fieraria."
      RETURN: "Jucatorul revine la fierar."
      COMPLETION: "Fierarul intelege ce s-a intamplat."
    quest:
      code: "Q06"
      giver_profession: "blacksmith"
      kind: "exploration"
      acceptance_mode: "explicit"
      completion_mode: "return_to_giver"
      tracking_mode: "next_objective"
      tags: ["mapping", "investigation", "forge"]
      objectives:
        visit_forge:
          type: "visit_place"
          item: "tag:blacksmith"
          amount: 1
          description: "Mergi la fierarie."
        inspect_anvil:
          type: "inspect_node"
          item: "type:inspect_node"
          amount: 1
          description: "Inspecteaza punctul suspect din fierarie."
      rewards:
        story_event:
          type: "record_story_event"
          item: "forge_investigated"
          amount: 1
          description: "Satul retine investigatia."
          scope: "place"
          target: "current_place"
          event_type: "quest_progress"
          event_key: "q06_forge_investigated"
          title: "Forja a fost investigata"
          payload:
            quest: "Q06"
        emerald:
          type: "item"
          item: "EMERALD"
          amount: 1
          description: "Primesti 1 smarald."
    roles:
      QUEST_GIVER:
        description: "Fierarul care cere investigatia."
        required_professions:
          - "blacksmith"
      HERO:
        description: "Jucatorul care verifica fieraria."
        player_role: true
```

Observatii:

- `visit_place` si `inspect_node` cer mapping functional;
- `item` la obiective semantice este o referinta, nu material Minecraft;
- foloseste prefixe clare: `tag:`, `type:`, `place:`, `node:`, `region:`, `npc:`;
- reward-urile story pot avea campuri extra, iar `FeaturePackLoader` le pastreaza in metadata/payload;
- foloseste chei stabile in YAML, de exemplu `visit_forge`, `inspect_anvil`, chiar daca runtime-ul nu le foloseste inca drept `objective_key` final.

## Referinte semantice recomandate

| Referinta | Cand se foloseste | Exemplu |
|---|---|---|
| `region:<id>` | Tinta este o regiune exacta | `region:satul_central` |
| `place:<id>` | Tinta este un loc exact | `place:satul_central:fierarie` |
| `node:<id>` | Tinta este un node exact | `node:satul_central:fierarie:anvil` |
| `tag:<tag>` | Vrei quest reutilizabil | `tag:blacksmith` |
| `type:<type>` | Vrei orice loc/node de un tip | `type:inspect_node` |
| `npc:<name_or_uuid>` | Vrei NPC exact sau giver curent | `npc:Ion` |

Pentru continut reutilizabil, prefera `tag:` sau `type:`. Pentru questuri de poveste fixe, poti folosi `region:`, `place:` sau `node:`.

## Tipuri de obiective: curente si propuse

### Suportate acum

| Tip | Progres | Necesita mapping | Note |
|---|---|---:|---|
| `collect_item` | inventar | nu | material Minecraft valid |
| `deliver_to_npc` | inventar plus interactiune | nu pentru item, da pentru NPC semantic | consuma itemele la finalizare |
| `talk_to_npc` | dialog/interactiune NPC | optional | poate rezolva giver-ul curent |
| `visit_region` | pozitia jucatorului | da | ancora `region` |
| `visit_place` | pozitia jucatorului | da | ancora `place` |
| `inspect_node` | apropiere/interactiune cu node | da | ancora `node` |
| `kill_mob` | EntityDeathEvent | nu | entity Minecraft valid |

### Urmatorul val recomandat

Acestea sunt cele mai utile tipuri noi, in ordinea recomandata.

| Prioritate | Tip nou | Motiv | Eveniment/runtime necesar |
|---:|---|---|---|
| 1 | `craft_item` | adauga varietate fara sistem nou mare | `CraftItemEvent` |
| 2 | `interact_block` | bun pentru puzzle-uri simple | `PlayerInteractEvent` |
| 3 | `talk_to_role` | face questurile reutilizabile | resolver NPC dupa profesie/rol |
| 4 | `kill_mob_in_region` | lupta contextualizata | death event plus `WorldAdminApi.findRegion` |
| 5 | `bring_item_to_place` | delivery real catre loc, nu doar NPC | inventar plus `findPlace` |
| 6 | `wait_until_time` | ritm si rutina | scheduler/tick controlat |
| 7 | `choose_dialogue_branch` | branching social | integrare cu dialog manager |
| 8 | `trigger_story_flag` | questuri dependente de lume | `StoryStateService` |
| 9 | `escort_npc` | gameplay avansat | tracking NPC plus timeout/fail |
| 10 | `survive_duration` | defense/siege | scheduler, regiune si combat state |

Regula pentru orice tip nou:

1. adauga normalizare in contract/audit;
2. incarca metadata necesara din YAML;
3. valideaza template-ul in `/ainpc audit quest`;
4. creeaza sau extinde listener-ul;
5. actualizeaza `PlayerQuestProgress.objectiveProgress`;
6. salveaza in `player_quests`;
7. adauga test de contract pentru YAML;
8. adauga smoke test pe server daca depinde de Paper.

## Cum adaugi un tip nou de obiectiv

Exemplu: `craft_item`.

### 1. Contract YAML

```yml
objectives:
  craft_bandage:
    type: "craft_item"
    item: "PAPER"
    amount: 2
    description: "Craftuieste 2 bandaje simple."
```

### 2. Parser

`FeaturePackLoader` incarca deja `type`, `item`, `amount`, `description` si metadata extra. Pentru tipuri simple, nu ai nevoie imediat de schema noua.

Pentru tipuri complexe, foloseste metadata explicita:

```yml
objectives:
  interact_altar:
    type: "interact_block"
    item: "CHISELED_STONE_BRICKS"
    amount: 1
    description: "Activeaza altarul vechi."
    action: "RIGHT_CLICK_BLOCK"
    place: "tag:chapel"
```

### 3. Audit

Extinde validarea din `AINPCCommand.validateQuestObjectiveReference(...)`:

- `craft_item` valideaza `Material`;
- `interact_block` valideaza `Material` si metadata obligatorie;
- `kill_mob_in_region` valideaza `EntityType` si referinta de regiune;
- `talk_to_role` valideaza ca profesia/rolul exista in feature packs.

### 4. Runtime

Listener-ul nu trebuie sa contina toata logica. El doar transforma evenimentul Paper intr-un eveniment de quest.

Model recomandat:

```text
Paper event -> QuestObjectiveListener -> QuestEngine/ScenarioEngine -> updateObjectiveProgress -> saveProgress
```

### 5. Progres

Progresul trebuie indexat dupa `objective_id` stabil. Pana cand exista suport complet, pastreaza cheia YAML stabila si pregateste migrarea de la cheia generata.

Format tinta:

```json
{
  "craft_bandage": 2,
  "interact_altar": 1
}
```

## Etape reale

`phases` din YAML sunt utile pentru naratiune, dar nu sunt suficiente pentru questuri avansate. Pentru runtime matur trebuie introdus `quest.stages`.

Schema tinta:

```yml
quest:
  code: "Q10"
  giver_profession: "priest"
  kind: "investigation"
  stages:
    start:
      display_name: "Zvonul"
      completion_mode: "MANUAL_TURN_IN"
      objectives:
        talk_priest:
          type: "talk_to_npc"
          item: "profession:priest"
          amount: 1
          description: "Vorbeste cu preotul."
      next_stage: "investigate"

    investigate:
      display_name: "Urmele"
      completion_mode: "ALL_OBJECTIVES"
      objectives:
        visit_chapel:
          type: "visit_place"
          item: "tag:chapel"
          amount: 1
          description: "Mergi la capela."
        inspect_altar:
          type: "inspect_node"
          item: "type:altar"
          amount: 1
          description: "Inspecteaza altarul."
      next_stage: "choice"

    choice:
      display_name: "Alegerea"
      completion_mode: "ANY_OBJECTIVE"
      objectives:
        report_priest:
          type: "talk_to_npc"
          item: "profession:priest"
          amount: 1
          description: "Spune adevarul preotului."
        report_merchant:
          type: "talk_to_npc"
          item: "profession:merchant"
          amount: 1
          description: "Vinde informatia negustorului."
      branch_rules:
        report_priest: "complete_honorable"
        report_merchant: "complete_profit"
```

Reguli de implementare:

- verifica doar obiectivele din etapa activa;
- salveaza `current_stage_id` in `player_quests`;
- nu consuma reward-uri intermediare prin acelasi cod ca reward-ul final;
- permite `ALL_OBJECTIVES`, `ANY_OBJECTIVE`, `MANUAL_TURN_IN`;
- fiecare stage trebuie sa aiba `id`, obiective si tranzitie explicita.

## Branching

Branching-ul trebuie introdus dupa etape reale, nu inainte.

Minimul necesar:

- `questVariables` pentru alegeri;
- `branch_rules` pe stage;
- `next_stage` dinamic;
- audit pentru destinatii inexistente;
- istoric in progres pentru debugging.

Exemplu de variabile:

```json
{
  "choice.reported_to": "merchant",
  "helped_blacksmith": "true",
  "crypt_result": "sealed"
}
```

Nu folosi branching generat liber de AI in runtime. AI-ul poate propune drafturi, dar runtime-ul trebuie sa execute doar branch-uri validate din YAML.

## Mai multe questuri active

Pentru questuri variate, un singur quest activ per jucator devine blocant. Runtime-ul a fost migrat initial spre:

```java
Map<UUID, Map<String, PlayerQuestProgress>> activePlayerQuests;
```

sau, mai curat:

```text
QuestProgressRepository
QuestRuntimeIndex
QuestEngine
```

Reguli:

- cheia principala este `player_uuid + template_id`;
- `quest.code` este cod de design, nu inlocuitor complet pentru `template_id`;
- un jucator poate avea simultan quest principal, quest secundar, quest repetabil si quest de reputatie;
- contractul suporta categorii `main`, `side` si `repeatable`, cu limite initiale in `quest.max_active`;
- UI-ul si comenzile trebuie sa ceara explicit questul cand exista mai multe active;
- tracking-ul poate alege "quest tracked", nu "singurul quest activ";
- questul urmarit este persistat prin `player_quests.tracked`.

## Recompense mai variate

Extinde reward-urile in doua faze.

### Faza sigura

Pastreaza reward-urile suportate acum:

- `item`
- `set_story_state`
- `record_story_event`

Adauga questuri care combina item plus story action. Asta deja face lumea sa para ca evolueaza.

### Faza avansata

Introdu `RewardResolver` si tipuri noi:

| Reward | Efect |
|---|---|
| `xp` | experienta Minecraft |
| `money` | economie/plugin extern, optional |
| `reputation` | reputatie pe sat/regiune/factiune |
| `relationship` | relatie cu NPC sau familie |
| `unlock_quest` | deblocheaza quest urmator |
| `unlock_place` | marcheaza un loc ca accesibil/cunoscut |
| `run_action` | actiune controlata prin registry |

Regula: reward-ul nu trebuie sa execute cod arbitrar din YAML. Trebuie sa fie un tip cunoscut, validat si auditat.

## Progres si persistenta

`player_quests` trebuie sa ramana sursa de adevar pentru statusul questului.

Campuri tinta:

- `player_uuid`
- `template_id`
- `quest_code`
- `status`
- `current_stage_id`
- `objective_progress`
- `quest_variables`
- `tracked`
- `assigned_npc_ids`
- `started_at`
- `updated_at`
- `completed_at`
- `failed_at`
- `history`

`quest_anchor_bindings` ramane sursa pentru ancore rezolvate:

- `objective_key`
- `objective_type`
- `reference`
- `anchor_type`
- `anchor_id`
- `anchor_label`

Recomandare importanta:

- `questVariables` este cache runtime si context narativ;
- `quest_anchor_bindings` este auditabil si stabil;
- `objective_progress` trebuie sa treaca treptat la chei explicite de obiectiv;
- nu schimba ID-uri de mapping pe server live fara migration/backfill.

## Ordine recomandata de implementare

### Q2.1 - Stabilizare continut jucabil

- smoke test Q01-Q05 pe server Paper;
- 3-5 questuri medievale noi care folosesc `visit_place` si `inspect_node`;
- exemple clare in `medieval_quest.yml`;
- `/ainpc audit quest` fara erori pentru pack-ul medieval.

### Q2.2 - Objective ID stabil

- foloseste cheia YAML ca `entry_id`;
- schimba `QuestAnchorResolver.buildObjectiveKey(...)` sa prefere `metadata.entry_id`;
- migreaza progresul nou spre `objective_id`;
- pastreaza fallback pentru progres vechi.

### Q2.3 - Tipuri noi usoare

- `craft_item`;
- `interact_block`;
- `talk_to_role`;
- `kill_mob_in_region`;
- audit si teste pentru fiecare.

### Q2.4 - Multi-quest

- runtime-ul foloseste deja progres curent per `player_uuid + template_id`;
- comenzile `quest status` si `quest track` accepta deja `quest_code` sau `template_id`;
- comanda `quest abandon` accepta deja `tracked`, `quest_code` sau `template_id`;
- comanda admin `quest debug` poate inspecta progresul si variabilele persistente pentru `tracked`, `quest_code` sau `template_id`;
- `debugdump quest` exporta `quest-audit-report.txt`, `loaded-quest-definitions.json`, `player-quest-progress.json`, `quest-anchor-bindings.json` si `story-events.json`;
- `quest log` are filtre initiale: `active`, `current`, `tracked`, `main`, `side`, `repeatable`, `completed`, `failed`, `archived`, `all`;
- `quest log` sorteaza questurile curente cu tracked primul, apoi `main`, `side`, `repeatable`, si arata sumar pe status/categorii;
- `quest log` afiseaza actiuni rapide cu selectorul corect pentru status, tracking, abandon si debug admin;
- `quest log` grupeaza vizual questurile curente in tracked, main, side, repeatable si template lipsa;
- contractul are deja categorie `main`, `side`, `repeatable`, iar availability-ul aplica `quest.max_active`;
- questul tracked este persistat si restaurat la load;
- `/ainpc audit quest` valideaza ca un jucator nu are mai multe questuri tracked si ca tracked indica doar questuri active;
- maturizeaza mai departe `quest log` cu indicator tracked si filtre;
- pastreaza selectia explicita prin `template_id` sau `quest_code`;
- pastreaza `nearest` pentru flux simplu.

### Q2.5 - Stages runtime

- initial implementat: obiectivele pot declara `phase`/`stage`, iar runtime-ul verifica si actualizeaza doar etapa activa;
- `current_stage_id` este persistat in `player_quests`, tinut sincronizat initial cu `current_phase` si backfilled la upgrade, pastrand compatibilitatea cu progresul existent;
- Q06, Q07 si Q08 folosesc metadata `phase` pentru obiective etapizate;
- ramas de facut: adauga model `QuestStageDefinition`;
- ramas de facut: extinde parserul pentru `quest.stages`;
- ramas de facut: separa semantic `current_stage_id` de `current_phase` daca apar faze narative care nu sunt stages runtime;
- adauga `ALL_OBJECTIVES`, `ANY_OBJECTIVE`, `MANUAL_TURN_IN`.

### Q2.6 - Branching si reward resolver

- introdu `branch_rules`;
- salveaza alegerile in `questVariables`;
- introdu `RewardResolver`;
- adauga `reputation`, `unlock_quest`, `relationship`;
- extinde auditul pentru destinatii si conditii.

## Partea 2 - plan executabil pentru Q2

Aceasta parte transforma directia de mai sus intr-un backlog tehnic. Ordinea este importanta: intai progres stabil si obiective auditabile, apoi continut, apoi UI/comenzi, apoi multi-quest si stages.

### Q2.0 - Preconditii

Inainte de implementare:

- Q01-Q05 trebuie sa treaca prin `/ainpc audit quest` fara erori critice;
- `scripts/smoke-paper-quests.ps1` trebuie sa poata pregati serverul de test;
- `quest_anchor_bindings` trebuie sa fie populat pentru questurile care folosesc mapping;
- `StoryStateService` trebuie sa ramana optional pentru questurile fara story actions;
- niciun quest nou nu trebuie sa ceara `quest.stages` pana cand parserul nu il suporta explicit.

Rezultat asteptat: faza Q2 porneste de la runtime-ul existent, nu de la o schema viitoare neimplementata.

### Q2.1 - Progres persistent si objective id stabil

Primul pas tehnic este stabilizarea cheilor de progres. Fara asta, orice tip nou de obiectiv devine greu de migrat.

Model tinta:

```text
player_quests.objective_progress = {
  "collect_iron": 3,
  "visit_forge": 1,
  "inspect_anvil": 1
}
```

Reguli:

- cheia YAML a obiectivului devine `objective_id`;
- daca lipseste cheia YAML, se pastreaza fallback-ul generat actual;
- `QuestAnchorResolver` foloseste `objective_id` si in `quest_anchor_bindings.objective_key`;
- progresul vechi ramane citibil pana la o migrare explicita;
- `objective_id` nu se schimba dupa ce questul a ajuns pe server live.

Schimbari recomandate in cod:

| Fisier | Schimbare |
|---|---|
| `FeaturePackLoader.java` | pune cheia YAML in metadata, de exemplu `entry_id` sau `objective_id` |
| `QuestAnchorResolver.java` | prefera `objective_id` in locul cheii generate din tip/item/index |
| `ScenarioEngine.java` | citeste si scrie progres dupa `objective_id` |
| `AINPCCommand.java` | audit pentru obiective fara ID stabil sau duplicate |
| `QuestScenarioContractTest.java` | test pentru pastrarea cheilor YAML |

Migrare DB recomandata, daca tabela nu are deja coloanele tinta:

```sql
ALTER TABLE player_quests ADD COLUMN current_stage_id TEXT;
ALTER TABLE player_quests ADD COLUMN objective_progress TEXT NOT NULL DEFAULT '{}';
ALTER TABLE player_quests ADD COLUMN quest_variables TEXT NOT NULL DEFAULT '{}';
ALTER TABLE player_quests ADD COLUMN failed_at BIGINT;
```

Compatibilitate:

- daca exista deja coloane similare, nu se creeaza duplicate;
- valorile lipsa se trateaza ca `{}` si `null`;
- questurile active vechi pot continua cu cheia generata, dar questurile noi scriu `objective_id`.

Criterii de acceptare:

- Q01-Q05 se finalizeaza ca inainte;
- `/ainpc quest status` afiseaza progresul corect pentru obiectivele existente;
- `/ainpc quest anchors` arata chei stabile pentru `visit_place` si `inspect_node`;
- testele unitare acopera cel putin un obiectiv cu cheia YAML `visit_forge`.

### Q2.2 - Objective handlers

Dupa ce progresul are chei stabile, obiectivele trebuie mutate spre handlere clare. Listener-ele Paper nu trebuie sa decida singure progresul complet; ele doar trimit semnalul catre runtime.

Flux recomandat:

```text
Paper/Bukkit event
-> QuestObjectiveListener
-> ScenarioEngine sau QuestObjectiveTracker
-> ObjectiveHandler
-> PlayerQuestProgress
-> save player_quests
```

Handlere minime pentru runtime-ul curent:

| Handler | Obiective | Sursa evenimentului | Validare |
|---|---|---|---|
| `CollectItemObjectiveHandler` | `collect_item` | inventar/status/turn-in | `Material` valid, amount > 0 |
| `DeliverToNpcObjectiveHandler` | `deliver_to_npc` | interactiune NPC | item valid, NPC tinta sau giver curent |
| `TalkToNpcObjectiveHandler` | `talk_to_npc` | chat/interactiune NPC | profesie, rol sau NPC rezolvat |
| `VisitRegionObjectiveHandler` | `visit_region` | miscare/player location | region anchor existent |
| `VisitPlaceObjectiveHandler` | `visit_place` | miscare/player location | place anchor existent |
| `InspectNodeObjectiveHandler` | `inspect_node` | interactiune/apropiere node | node anchor existent |
| `KillMobObjectiveHandler` | `kill_mob` | `EntityDeathEvent` | `EntityType` valid |

Handlere Q2.3, dupa stabilizarea celor curente:

| Handler | Obiective | Motiv |
|---|---|---|
| `CraftItemObjectiveHandler` | `craft_item` | varietate simpla, fara mapping nou |
| `InteractBlockObjectiveHandler` | `interact_block` | investigatii si puzzle-uri usoare |
| `TalkToRoleObjectiveHandler` | `talk_to_role` | continut reutilizabil pe profesii |
| `KillMobInRegionObjectiveHandler` | `kill_mob_in_region` | combat contextualizat |

Reguli de implementare:

- fiecare handler primeste definitia obiectivului, progresul curent si contextul evenimentului;
- handlerul returneaza un rezultat explicit: `NO_MATCH`, `PROGRESSED`, `COMPLETED`;
- niciun handler nu da reward-uri direct;
- finalizarea questului ramane intr-un singur punct din runtime;
- progresul este salvat numai daca valoarea s-a schimbat.

Criterii de acceptare:

- un eveniment fara quest activ nu modifica DB;
- un eveniment pentru alt obiectiv nu modifica progresul curent;
- progresul nu depaseste `amount`;
- la restart, progresul ramane corect;
- auditul poate spune ce handler lipseste pentru un `objective.type`.

### Q2.3 - Quest log si tracking pentru jucator

Questurile avansate devin greu de testat fara vizibilitate buna. UI-ul poate ramane simplu la inceput; partea importanta este contractul comenzilor.

Comenzi recomandate:

```text
/ainpc quest log [player] [filter]
/ainpc quest status <questCode|templateId|nearest> [player]
/ainpc quest track [start|stop] [questCode|templateId] [player]
/ainpc quest abandon <quest|tracked> [player]
/ainpc quest debug <quest|tracked> [player]
```

Informatii minime in `quest log`:

- cod quest;
- nume quest;
- status;
- giver;
- etapa curenta, daca exista;
- urmatorul obiectiv;
- indicator daca este tracked.
- filtre pentru status, tracked si categorie.
- sumar pentru status curent si categorii.
- actiuni rapide cu `quest_code` sau `template_id`.
- grupare vizuala pe tracked si categorie.

Informatii minime in `quest status`:

- toate obiectivele active;
- progres numeric `current/required`;
- tinta semantica rezolvata, daca exista;
- mesaj clar daca ancora lipseste;
- reward-urile finale.

Reguli pentru tracking:

- un singur quest tracked per jucator;
- tracking-ul este optional si nu afecteaza progresul;
- `nearest` ramane shortcut pentru test manual;
- cand exista mai multe questuri active, comenzile cer `quest_code` sau `template_id`.
- abandonul prin `tracked` sau `quest_code` nu cere NPC in raza.

Criterii de acceptare:

- adminul poate vedea starea unui jucator fara acces la DB;
- adminul poate inspecta progresul brut si variabilele prin `quest debug`;
- jucatorul poate intelege urmatorul pas fara sa citeasca YAML-ul;
- questurile cu ancore lipsa afiseaza problema, nu doar "0/1";
- tracking-ul nu porneste automat un quest.

### Q2.4 - Continut medieval jucabil

Dupa Q2.1 si Q2.2, merita adaugate questuri noi in pack. Nu adauga multe questuri inainte de smoke test; 3-5 questuri bune sunt suficiente.

Set recomandat:

| Cod | Giver | Tip | Obiective | Scop tehnic |
|---|---|---|---|---|
| `Q06` | `blacksmith` | `exploration` | `visit_place`, `inspect_node` | verifica mapping pentru fierarie |
| `Q07` | `innkeeper` | `delivery` | `collect_item`, `talk_to_npc`, `deliver_to_npc` | verifica livrare sociala |
| `Q08` | `guard` | `hunt` | `visit_region`, `kill_mob`, `talk_to_npc` | verifica combat contextual si story event regional |
| `Q09` | `healer` | `investigation` | `talk_to_npc`, `visit_place`, `inspect_node` | combina social, mapping si story |
| `Q10` | `priest` | `story` | doar dupa stages | pregateste branching viitor |

Reguli de continut:

- Q06-Q09 trebuie sa foloseasca doar tipuri suportate acum;
- Q10 ramane documentat sau dezactivat pana exista stages;
- fiecare quest are `kind`, `tags`, `acceptance_mode`, `completion_mode`, `tracking_mode`;
- fiecare quest are dialoguri pentru `offer`, `accepted`, `active`, `ready`, `completed`;
- fiecare quest care foloseste mapping are referinte `tag:` sau `type:`, nu coordonate brute.

Criterii de acceptare:

- `MedievalQuestPackTest` incarca Q01-Q09;
- `/ainpc audit quest` raporteaza zero erori pentru pack-ul medieval;
- smoke test-ul include cel putin Q06 si Q09;
- reward-urile story sunt vizibile in `/ainpc story events`.

### Q2.5 - Multi-quest fara stages

Multi-quest trebuie introdus dupa ce log/status/tracking sunt clare. Altfel adminul nu poate vedea ce s-a stricat.

Model simplu:

```java
Map<UUID, Map<String, PlayerQuestProgress>> activePlayerQuests;
```

Model recomandat pe termen mediu:

```text
QuestProgressRepository
QuestRuntimeIndex
TrackedQuestService
```

Reguli de runtime:

- jucatorul poate avea un quest principal activ;
- jucatorul poate avea 2-3 side quests active;
- questurile repeatable au cooldown obligatoriu;
- `quest.code` poate fi afisat, dar cheia interna ramane `template_id`;
- daca un NPC ofera mai multe questuri, comanda trebuie sa aleaga explicit unul.

Coloane utile:

```text
quest_slot: main | side | repeatable | reputation
tracked: boolean
cooldown_until: timestamp
```

Comportament pentru compatibilitate:

- daca exista un singur quest activ, comenzile vechi functioneaza la fel;
- daca exista mai multe, comenzile ambigue returneaza lista de optiuni;
- `nearest` poate alege questul oferit de NPC-ul apropiat, dar nu questul activ arbitrar.

Criterii de acceptare:

- un jucator poate avea Q06 si Q08 active simultan;
- progresul la `kill_mob` nu modifica Q06;
- `quest log` arata ambele questuri;
- `quest track Q08` nu schimba statusul Q06;
- abandonul unui side quest nu sterge questul principal.

### Q2.6 - Stages runtime

Stages se implementeaza dupa multi-quest sau intr-o ramura separata, dar nu inainte de `objective_id` stabil.

Model minim:

```text
QuestStageDefinition
- id
- display_name
- completion_mode
- objectives
- next_stage
- branch_rules
```

Persistenta:

```text
player_quests.current_stage_id
player_quests.objective_progress
player_quests.quest_variables
```

Reguli:

- runtime-ul verifica doar obiectivele din `current_stage_id`;
- obiectivele din stages au tot chei stabile;
- `next_stage` trebuie sa pointeze catre un stage existent;
- stage-ul final nu trebuie sa aiba `next_stage`;
- reward-ul final se da o singura data, la completarea questului;
- actiunile intermediare trebuie separate de reward-urile finale.

Audit:

- stage fara obiective;
- `next_stage` lipsa sau invalid;
- `branch_rules` catre stage inexistent;
- obiectiv duplicat intre stages fara namespace clar;
- `completion_mode` necunoscut.

Criterii de acceptare:

- un quest cu doua stages avanseaza dupa completarea primului stage;
- obiectivele din stage-ul doi nu pot progresa inainte de activarea lui;
- restartul pastreaza stage-ul curent;
- `quest status` afiseaza stage-ul activ.

### Q2.7 - Branching si reward resolver

Branching-ul si reward-urile avansate sunt ultima parte din Q2. Ele depind de stages, variabile si audit bun.

Branch minim:

```yml
branch_rules:
  report_priest: "complete_honorable"
  report_merchant: "complete_profit"
```

Variabile recomandate:

```json
{
  "branch.selected": "report_priest",
  "reported_to": "priest",
  "reward_path": "honorable"
}
```

Reward resolver:

| Tip | Implementare initiala |
|---|---|
| `item` | existent |
| `set_story_state` | existent |
| `record_story_event` | existent |
| `xp` | Paper API direct |
| `reputation` | tabela separata sau amanat cu audit warning |
| `relationship` | legatura cu `MemoryManager`/NPC relationship, dupa contract |
| `unlock_quest` | flag persistent pentru prerequisite |

Reguli:

- reward-ul necunoscut blocheaza auditul;
- reward-ul optional poate fi marcat explicit cu `optional: true`;
- integrarea cu economie sau plugin extern ramane optionala;
- AI-ul nu decide branch-ul in runtime; AI-ul poate doar genera propuneri validate.

Criterii de acceptare:

- doua alegeri diferite duc la stage-uri diferite;
- reward-ul final se aplica o singura data;
- story event-ul pastreaza branch-ul ales in payload;
- auditul detecteaza branch catre stage inexistent.

## Backlog tehnic Q2

Ordinea recomandata pentru pull request-uri mici:

1. adauga `objective_id` din cheia YAML si test de parser;
2. actualizeaza `QuestAnchorResolver` sa foloseasca `objective_id`;
3. normalizeaza `objective_progress` in `PlayerQuestProgress`;
4. adauga fallback pentru progres vechi;
5. extinde `/ainpc audit quest` pentru handler lipsa si ID duplicat;
6. extrage handlerele curente din `ScenarioEngine` sau grupeaza-le intern;
7. adauga `quest log/status/track` mai explicit;
8. adauga Q06-Q09 in `medieval_quest.yml`;
9. extinde smoke test-ul pentru Q06 si Q09;
10. implementeaza multi-quest cu compatibilitate pentru cazul unui singur quest;
11. adauga `QuestStageDefinition` si parser pentru `quest.stages`;
12. implementeaza stage runtime pentru un quest simplu cu doua stages;
13. adauga branch rules;
14. extrage `RewardResolver`;
15. abia apoi planifica `QuestEngine` separat.

## Riscuri Q2

| Risc | Efect | Mitigare |
|---|---|---|
| Schimbarea cheilor de progres | questuri active stricate | fallback pentru cheia veche si migrare controlata |
| Multi-quest prea devreme | comenzi ambigue si buguri greu de vazut | intai `quest log` si tracking |
| Stages in YAML fara runtime | authoring fals, audit incomplet | audit warning sau blocare pana exista parser |
| Reward-uri prea flexibile | executie nesigura din YAML | registry strict de reward types |
| Branching prin AI | comportament nedeterminist | AI doar authoring, runtime doar YAML validat |

## Definitia de gata pentru Q2

Q2 este considerata gata cand:

- Q01-Q05 raman completabile;
- Q06-Q09 sunt in pack si trec auditul;
- obiectivele noi folosesc `objective_id` stabil;
- `player_quests` persista `objective_progress` si `quest_variables`;
- `quest log/status/track` ajuta testarea fara acces direct la DB;
- smoke test-ul Paper acopera cel putin un quest cu mapping si unul cu combat;
- documentatia mentioneaza clar ce este runtime curent si ce este schema tinta;
- stages si branching nu sunt prezentate ca suportate pana cand exista parser, runtime si audit.

## Partea 3 - maturizare dupa Q2

Partea 3 incepe dupa ce Q2 are progres stabil, objective handlers, log/tracking, cateva questuri medievale jucabile si baza pentru stages. Scopul Q3 nu este doar sa mai adauge tipuri de obiective, ci sa transforme questurile intr-un subsistem matur, usor de extins prin addonuri si sigur de operat pe server.

Directia Q3:

- extrage treptat runtime-ul de quest din `ScenarioEngine`;
- separa definitiile, progresul, tracking-ul, reward-urile si validarea;
- organizeaza continutul in quest packs mai clare;
- adauga pipeline de authoring si validare pentru questuri noi;
- leaga questurile de story context, reputatie si consecinte persistente;
- adauga debug, export si smoke tests pentru operare reala.

### Q3.0 - Preconditii

Nu incepe Q3 daca lipsesc aceste lucruri:

- Q2 are `objective_id` stabil;
- `player_quests.objective_progress` si `quest_variables` sunt persistente;
- `/ainpc quest log`, `/ainpc quest status` si `/ainpc quest track` sunt suficient de clare;
- Q06-Q09 sunt completabile sau macar testabile cap-coada;
- auditul poate separa erori critice de warnings;
- stages nu mai sunt doar schema tinta, ci au macar un runtime minim validat.

Regula: Q3 trebuie sa reduca complexitatea din runtime, nu sa o ascunda sub inca un strat de YAML.

### Q3.1 - Extrage QuestEngine din ScenarioEngine

`ScenarioEngine` poate ramane responsabil pentru scenarii emergente si decizii narative, dar questurile au nevoie de un motor dedicat.

Structura recomandata:

```text
ro.ainpc.quest
- QuestEngine
- QuestDefinition
- QuestStageDefinition
- QuestObjectiveDefinition
- QuestRewardDefinition
- QuestProgress
- QuestProgressRepository
- QuestObjectiveRegistry
- QuestRewardRegistry
- QuestTrackingService
- QuestAuditService
```

Responsabilitati:

| Componenta | Responsabilitate |
|---|---|
| `QuestEngine` | acceptare, abandon, progres, completare, esec |
| `QuestProgressRepository` | incarcare/salvare DB pentru `player_quests` |
| `QuestObjectiveRegistry` | mapare `objective.type` catre handler |
| `QuestRewardRegistry` | mapare `reward.type` catre handler |
| `QuestTrackingService` | quest tracked, next objective, mesaje de stare |
| `QuestAuditService` | validare template, progres, bindings si rewards |
| `ScenarioEngine` | consumator/adapter pana la migrarea completa |

Ordine sigura de extractie:

1. creeaza modele read-only pentru definitii;
2. muta citirea progresului in `QuestProgressRepository`;
3. muta update-ul de obiective in `QuestEngine`;
4. lasa comenzile sa apeleze `QuestEngine`, nu metode private din `ScenarioEngine`;
5. pastreaza adapter pentru fluxurile vechi pana trec testele;
6. elimina duplicarea numai dupa ce smoke test-ul trece.

Criterii de acceptare:

- `ScenarioEngine` nu mai contine logica principala de obiective;
- completarea unui quest trece prin `QuestEngine.completeQuest(...)`;
- testele de quest pot instantia `QuestEngine` fara server Paper complet;
- comenzile admin nu acceseaza direct structuri interne de progres.

### Q3.2 - Quest packs modulare

Un singur fisier mare `medieval_quest.yml` devine greu de intretinut. Q3 trebuie sa permita organizare pe pachete si fisiere.

Structura tinta:

```text
packs/
  medieval_quest/
    pack.yml
    professions.yml
    rewards.yml
    quests/
      blacksmith.yml
      guard.yml
      innkeeper.yml
      healer.yml
      priest.yml
    dialogs/
      blacksmith.yml
      guard.yml
    story/
      flags.yml
      events.yml
```

Reguli:

- `pack.yml` contine id, nume, versiune, dependinte si namespace;
- fisierele din `quests/` contin doar quest definitions;
- dialogurile mari pot fi separate, dar trebuie referite explicit;
- `rewards.yml` poate defini preseturi, dar reward-urile finale raman expandate si auditate;
- namespace-ul pack-ului previne coliziuni intre `quest.code` si `objective_id`.

Exemplu de namespace:

```text
medieval:blacksmith:Q06
medieval:blacksmith:Q06:visit_forge
medieval:blacksmith:Q06:inspect_anvil
```

Compatibilitate:

- loaderul vechi pentru `medieval_quest.yml` ramane suportat;
- pack-urile noi pot fi incarcate din folder;
- auditul afiseaza sursa exacta: fisier, quest, obiectiv;
- ID-urile vechi Q01-Q09 nu se redenumesc pe server live.

Criterii de acceptare:

- pack-ul medieval poate fi impartit pe fisiere fara schimbarea comportamentului;
- auditul gaseste questuri duplicate intre fisiere;
- eroarea de YAML indica fisierul si linia cand parserul permite;
- testul de pack incarca atat formatul vechi, cat si formatul nou.

### Q3.3 - Validator si contract schema

Pe masura ce YAML-ul creste, auditul trebuie sa devina validator de contract, nu doar lista de verificari ad-hoc.

Niveluri de severitate:

| Severitate | Cand apare | Efect |
|---|---|---|
| `ERROR` | questul nu poate rula sigur | blocheaza load sau marcheaza questul invalid |
| `WARNING` | questul poate rula, dar are risc | apare in audit, nu blocheaza |
| `INFO` | observatie de authoring | apare doar in audit detaliat |

Validari Q3:

- `quest.code` unic in namespace;
- `objective_id` unic in quest;
- `stage.id` unic in quest;
- `next_stage` si `branch_rules` catre stages existente;
- `objective.type` are handler in registry;
- `reward.type` are handler in registry;
- referintele `tag:`, `type:`, `region:`, `place:`, `node:` sunt rezolvabile sau marcate lazy;
- `repeatable` are cooldown;
- `prerequisites` indica questuri existente;
- dialogurile referite exista;
- story actions au scope, target si key valid.

Comenzi recomandate:

```text
/ainpc audit quest
/ainpc audit quest pack <pack_id>
/ainpc audit quest code <quest_code>
/ainpc audit quest strict
/ainpc quest inspect <quest_code>
```

`quest inspect` trebuie sa arate definitia dupa parsare, nu YAML-ul brut.

Criterii de acceptare:

- un quest invalid nu apare ca ofertabil;
- auditul raporteaza cate questuri sunt valide, invalide si dezactivate;
- fiecare eroare indica pack-ul si questul;
- testele acopera cel putin un reward invalid, un objective invalid si un branch invalid.

### Q3.4 - Authoring asistat, dar validat

AI-ul poate ajuta la scrierea questurilor, dar nu trebuie sa fie autoritate de runtime. In Q3, authoring-ul trebuie tratat ca pipeline controlat.

Flux recomandat:

```text
idee quest
-> QuestDraft
-> validare schema
-> rezolvare ancore semantice
-> audit strict
-> smoke test
-> activare in pack
```

`QuestDraft` minim:

```json
{
  "pack_id": "medieval",
  "quest_code": "Q11",
  "kind": "investigation",
  "giver_profession": "healer",
  "summary": "Vindecatorul cere verificarea fantanii.",
  "objectives": [],
  "rewards": [],
  "story_effects": []
}
```

Reguli:

- AI-ul poate propune text, obiective si reward-uri;
- sistemul valideaza obiectivele impotriva registry-ului real;
- sistemul nu scrie direct in pack-ul activ fara audit;
- questurile generate primesc status `draft`, `validated`, `disabled` sau `active`;
- orice referinta semantica nerezolvata ramane warning pana la mapping.

Comenzi utile:

```text
/ainpc quest draft create
/ainpc quest draft validate <draft_id>
/ainpc quest draft inspect <draft_id>
/ainpc quest draft activate <draft_id>
```

Criterii de acceptare:

- un draft invalid nu poate fi activat;
- un draft activat produce aceeasi structura ca un quest scris manual;
- auditul arata diferenta dintre draft si quest activ;
- AI-ul nu poate introduce reward-uri necunoscute sau comenzi arbitrare.

### Q3.5 - Reputatie si consecinte persistente

Reputatia trebuie introdusa dupa reward resolver si story state, nu inainte. Ea trebuie sa fie explicita si auditabila.

Model minim:

```text
player_reputation
- player_uuid
- scope_type
- scope_id
- reputation_key
- value
- updated_at
```

Scope-uri:

| Scope | Exemplu |
|---|---|
| `region` | reputatie in sat |
| `place` | incredere la han |
| `profession` | reputatie cu fierarii |
| `faction` | reputatie cu garda |
| `npc` | relatie cu un NPC concret |

Reward YAML:

```yml
rewards:
  village_trust:
    type: "reputation"
    scope: "region"
    target: "current_region"
    key: "village_trust"
    amount: 5
    description: "Satul are mai multa incredere in tine."
```

Reguli:

- reputatia este reward sau consecinta validata, nu text liber;
- scaderile de reputatie sunt permise, dar trebuie vizibile in audit;
- dialogul AI poate citi reputatia, dar nu o modifica direct;
- reputatia poate deveni prerequisite pentru questuri viitoare.

Criterii de acceptare:

- completarea unui quest modifica reputatia o singura data;
- abandonul poate aplica penalizare doar daca questul o declara explicit;
- `/ainpc story context` sau o comanda dedicata poate afisa reputatia;
- prerequisite-ul `min_reputation` blocheaza corect un quest.

### Q3.6 - Failure rules si cleanup

Questurile avansate trebuie sa poata esua sau expira controlat.

Tipuri de failure:

| Tip | Exemplu | Necesita |
|---|---|---|
| `timeout` | livreaza in 10 minute | scheduler |
| `npc_dead_or_missing` | escorta esueaza daca NPC-ul dispare | NPC lifecycle |
| `left_region` | apararea esueaza daca pleci din zona | mapping |
| `item_lost` | obiectul unic este pierdut | inventory tracking |
| `story_state_changed` | alt eveniment a inchis oportunitatea | StoryStateService |

Schema:

```yml
failure_rules:
  timeout:
    type: "timeout"
    seconds: 600
    on_fail:
      - type: "record_story_event"
        event_key: "q11_failed_timeout"
```

Reguli:

- failure rule necunoscuta este eroare de audit;
- esecul seteaza `failed_at` si status `FAILED`;
- cleanup-ul ruleaza o singura data;
- abandonul voluntar si esecul automat sunt stari diferite;
- reward-urile finale nu ruleaza la esec.

Criterii de acceptare:

- questul expirat nu mai poate fi completat;
- cleanup-ul nu ruleaza de doua ori dupa restart;
- `quest log` arata motivul esecului;
- story event-ul de esec se poate vedea in audit/debug.

### Q3.7 - Debug, export si replay

Pe server live, bugurile de quest sunt greu de reparat fara export bun. Q3 trebuie sa ofere inspectie completa.

Export minim:

```text
debug-dump/
  quests/
    loaded-quest-definitions.json
    player-quest-progress.json
    quest-anchor-bindings.json
    quest-audit-report.txt
    story-events.json
```

Comenzi:

```text
/ainpc debug dump quests
/ainpc quest debug <player> <quest>
/ainpc quest repair anchors <player> <quest>
/ainpc quest replay events <player> <quest>
```

Reguli:

- debug dump nu modifica starea;
- repair trebuie sa fie explicit si auditat;
- replay ruleaza doar in mod admin/test, nu automat pe server live;
- exportul nu include date sensibile inutile.

Criterii de acceptare:

- poti diagnostica un quest blocat fara sa deschizi DB manual;
- exportul include definitia questului si progresul jucatorului;
- anchor repair poate reface binding-uri lipsa daca mapping-ul exista;
- replay poate reproduce progresul pentru un test simplu.

### Q3.8 - Balans si economie de questuri

Questurile repetabile si reward-urile trebuie controlate, altfel distrug economia serverului.

Reguli de balans:

- fiecare quest repeatable are `cooldown_seconds`;
- reward-urile mari cer prerequisite sau limitare;
- questurile scurte nu dau iteme rare;
- reputatia si story unlock-urile nu trebuie farmate infinit;
- abandonul repetat poate avea cooldown.

Campuri utile:

```yml
quest:
  repeatable: true
  cooldown_seconds: 86400
  max_completions_per_player: 3
  reward_tier: "minor"
  quest_slot: "side"
```

Audit Q3:

- repeatable fara cooldown;
- reward rar pe quest foarte scurt;
- `max_completions_per_player` lipsa pentru reputatie mare;
- quest principal marcat repeatable;
- reward duplicat suspect.

Criterii de acceptare:

- un quest repeatable nu poate fi reluat in cooldown;
- completarea maxima per jucator este respectata;
- auditul avertizeaza pentru reward-uri disproportionate;
- adminul poate vedea cooldown-ul ramas.

### Q3.9 - Integrare cu story context si AI

Q3 trebuie sa lege questurile de contextul narativ fara sa faca AI-ul nedeterminist.

Ce poate citi AI-ul:

- quest activ si stage activ;
- urmatorul obiectiv;
- reputatie relevanta;
- story state pentru region/place;
- evenimente recente din `story_events`;
- relatia NPC-jucator.

Ce nu poate decide AI-ul direct:

- completarea unui obiectiv;
- aplicarea reward-urilor;
- schimbarea branch-ului fara actiune validata;
- modificarea DB prin text liber;
- pornirea unui quest invalid.

Contract pentru prompt:

```text
QuestContext
- active_quests
- tracked_quest
- available_quests
- completed_quest_codes
- relevant_story_state
- relevant_reputation
```

Criterii de acceptare:

- dialogul NPC mentioneaza corect questul tracked;
- AI-ul nu ofera questuri blocate de prerequisite;
- story context include efectele questurilor completate;
- promptul nu contine YAML brut cand este suficient un snapshot.

### Q3.10 - Operare pe server

Pentru productie, adminul are nevoie de reguli clare de activare, rollback si reparare.

Runbook minim:

```text
1. ruleaza testele Maven;
2. ruleaza `/ainpc audit quest strict` pe server de staging;
3. ruleaza smoke test pentru questurile schimbate;
4. fa backup la DB;
5. porneste pack-ul nou cu questurile noi dezactivate;
6. activeaza questurile pe rand;
7. verifica `/ainpc debug dump quests`;
8. monitorizeaza completari, abandonuri si erori.
```

Configurare utila:

```yml
quest:
  max_active:
    main: 1
    side: 3
    repeatable: 2
  strict_audit_on_startup: true
  disable_invalid_quests: true
  debug_dump_on_audit_error: true
```

Criterii de acceptare:

- un quest invalid nu opreste tot pluginul daca `disable_invalid_quests` este true;
- auditul strict poate bloca pornirea in staging;
- adminul poate dezactiva un quest fara sa editeze cod;
- rollback-ul de pack nu sterge progresul vechi.

## Roadmap Q3 recomandat

Ordinea recomandata pentru Q3:

1. extrage `QuestProgressRepository`;
2. extrage `QuestObjectiveRegistry` si `QuestRewardRegistry`;
3. muta completarea questurilor in `QuestEngine`;
4. adauga `QuestAuditService` cu severitati;
5. imparte pack-ul medieval pe fisiere sau pregateste loaderul pentru asta;
6. adauga `quest inspect`;
7. introdu reputatie ca reward validat;
8. adauga failure rules simple: `timeout`;
9. adauga debug dump complet pentru questuri;
10. integreaza `QuestContext` in story/AI prompt;
11. adauga authoring draft numai dupa validator;
12. documenteaza runbook-ul de operare.

## Definitia de gata pentru Q3

Q3 este gata cand:

- `QuestEngine` exista si detine fluxul principal de runtime;
- `ScenarioEngine` nu mai este locul principal pentru logica de quest;
- quest definitions, progress, objective handlers si rewards sunt separate;
- auditul are severitati si poate dezactiva questuri invalide;
- pack-urile pot fi organizate modular sau loaderul este pregatit pentru asta;
- reputatia sau un alt sistem de consecinte persistente este functional;
- failure rules minime exista si sunt persistente;
- debug dump-ul poate explica un quest blocat;
- AI-ul foloseste `QuestContext` read-only, nu modifica progresul direct;
- exista runbook pentru activare, smoke test, rollback si repair.

## Partea 4 - ecosistem live si campanii

Partea 4 incepe dupa ce Q3 a separat runtime-ul de quest, a introdus audit matur, debug dump, reward registry si un minim de reputatie/failure rules. Scopul Q4 este sa faca sistemul potrivit pentru servere live cu continut in crestere: campanii, evenimente sezoniere, addonuri externe, editor, migrari sigure si observabilitate.

Q4 nu trebuie sa schimbe modelul de baza. Trebuie sa il stabilizeze ca platforma.

Directia Q4:

- questurile simple devin parte din campanii si story arcs;
- pack-urile pot avea versiuni, migrari si activare controlata;
- addonurile pot adauga obiective, reward-uri si conditii prin API;
- adminii pot testa, activa, dezactiva si repara continut live;
- AI-ul poate ajuta la authoring si localizare, dar runtime-ul ramane determinist;
- serverul poate masura completari, blocaje, abandonuri si reward economy.

### Q4.0 - Preconditii

Nu incepe Q4 daca:

- `QuestEngine` nu este extras sau macar izolat clar;
- `QuestAuditService` nu are severitati;
- `QuestObjectiveRegistry` si `QuestRewardRegistry` nu exista;
- progresul nu are `objective_id`, `current_stage_id` si `quest_variables`;
- debug dump-ul nu poate exporta definitii, progres si bindings;
- pack-urile invalide pot ajunge ofertabile;
- nu exista smoke test pentru cel putin un quest cu mapping si unul cu story action.

Regula: Q4 adauga scalare si operare, nu repara fundatia. Daca fundatia nu este gata, Q4 va amplifica bugurile.

### Q4.1 - Campanii si story arcs

Questurile individuale trebuie grupate in campanii. O campanie este un fir narativ cu progres propriu, dar nu inlocuieste progresul pe quest.

Model tinta:

```text
CampaignDefinition
- id
- namespace
- display_name
- description
- entry_quests
- required_quests
- stages
- unlock_rules
- completion_rules
- metadata
```

Exemplu YAML:

```yml
campaigns:
  medieval:forge_crisis:
    display_name: "Criza Forjei"
    description: "Satul investigheaza problemele aparute in jurul fierariei."
    entry_quests:
      - "medieval:blacksmith:Q06"
    required_quests:
      - "medieval:blacksmith:Q06"
      - "medieval:guard:Q08"
    completion_rules:
      all_quests_completed:
        type: "all_completed"
        quests:
          - "medieval:blacksmith:Q06"
          - "medieval:healer:Q09"
```

Reguli:

- campania nu duplica obiectivele questurilor;
- campania controleaza unlock-uri, ordine si story milestones;
- campania are progres separat de `player_quests`;
- questurile pot apartine unei singure campanii principale si mai multor categorii secundare;
- completarea campaniei poate aplica reward-uri globale, dar auditate.

Persistenta minima:

```text
player_campaigns
- player_uuid
- campaign_id
- status
- current_arc_id
- campaign_variables
- started_at
- updated_at
- completed_at
```

Criterii de acceptare:

- un jucator poate incepe campania printr-un quest de intrare;
- completarea questurilor actualizeaza progresul campaniei;
- `quest log` poate grupa questurile dupa campanie;
- auditul detecteaza campanii care refera questuri inexistente.

### Q4.2 - Evenimente globale si calendar

Pe server live, unele questuri trebuie sa apara doar in anumite conditii: noapte, sezon, eveniment de sat, raid, sarbatoare sau patch de continut.

Model:

```text
WorldEventDefinition
- id
- scope
- trigger
- starts_at
- ends_at
- affected_regions
- unlock_quests
- lock_quests
- story_effects
```

Exemplu:

```yml
world_events:
  harvest_festival:
    scope: "region"
    region: "tag:village"
    starts_at: "season:autumn"
    duration_days: 3
    unlock_quests:
      - "medieval:farmer:Q21"
      - "medieval:innkeeper:Q22"
```

Tipuri de trigger:

| Trigger | Exemplu | Necesita |
|---|---|---|
| `time_window` | weekend event | calendar server |
| `minecraft_time` | doar noaptea | world time |
| `story_state` | dupa purificarea criptei | StoryStateService |
| `admin_start` | eveniment pornit manual | comanda admin |
| `population_state` | sat sub prag de hrana | simulare sat |

Reguli:

- evenimentele nu sterg progresul questurilor deja active;
- un quest blocat de eveniment ramane in `AVAILABLE_LATER`, nu dispare fara explicatie;
- adminul poate vedea de ce un quest este disponibil sau blocat;
- evenimentele cu timp real trebuie sa foloseasca timestamps persistente.

Criterii de acceptare:

- un quest sezonier apare doar cand evenimentul este activ;
- questurile active pot fi finalizate dupa inchiderea evenimentului daca schema permite;
- `/ainpc event status` arata evenimentele active;
- auditul detecteaza `unlock_quests` inexistente.

### Q4.3 - Multi-settlement si multi-world

Daca serverul are mai multe sate sau lumi, questurile nu pot presupune o singura regiune activa.

Reguli de identificare:

```text
world_id
region_id
settlement_id
place_id
node_id
```

Chei recomandate:

```text
world:overworld/region:satul_central/place:fierarie
world:overworld/settlement:satul_central/quest:Q06
```

Model runtime:

| Context | Utilizare |
|---|---|
| `world_id` | separa progresul si mapping-ul intre lumi |
| `settlement_id` | limiteaza reputatia si questurile locale |
| `region_id` | story state regional |
| `place_id` | obiective de vizita si delivery |
| `node_id` | interactiuni fine |

Reguli:

- acelasi quest template poate fi instantiat in sate diferite;
- progresul jucatorului trebuie sa stie instanta reala, nu doar template-ul;
- ancorele se rezolva in settlement-ul curent sau declarat;
- reputatia locala nu se aplica automat global;
- transferul intre lumi nu trebuie sa completeze obiective gresite.

Persistenta recomandata:

```text
player_quests
- template_id
- quest_instance_id
- world_id
- settlement_id
- region_id
```

Criterii de acceptare:

- Q06 poate exista in doua sate fara coliziune de ancore;
- `quest_anchor_bindings` include contextul world/settlement;
- `quest log` arata satul sau regiunea questului;
- auditul detecteaza questuri cu referinte ambigue.

### Q4.4 - API public pentru addonuri de quest

Addonurile trebuie sa poata adauga obiective, reward-uri, conditii si pack-uri fara sa modifice core-ul.

API tinta:

```java
public interface QuestAddonApi {
    void registerObjectiveHandler(String type, QuestObjectiveHandler handler);
    void registerRewardHandler(String type, QuestRewardHandler handler);
    void registerConditionHandler(String type, QuestConditionHandler handler);
    void registerQuestPack(QuestPackDescriptor descriptor);
}
```

Contract handler:

```java
public interface QuestObjectiveHandler {
    QuestObjectiveResult handle(QuestObjectiveContext context);
    QuestObjectiveValidationResult validate(QuestObjectiveDefinition definition);
}
```

Reguli:

- tipurile din addon au namespace, de exemplu `economy:pay_money`;
- core-ul poate porni fara addonuri optionale;
- un quest care depinde de addon lipsa devine invalid sau dezactivat;
- addonul nu primeste acces direct la structurile interne mutable;
- fiecare handler trebuie sa aiba validare.

Descriptor:

```yml
quest_addon:
  id: "ainpc-economy"
  provides:
    reward_types:
      - "economy:money"
    condition_types:
      - "economy:min_balance"
  soft_dependencies:
    - "Vault"
```

Criterii de acceptare:

- un addon poate inregistra un reward nou fara schimbare in core;
- auditul raporteaza handler lipsa cu numele addonului asteptat;
- dezactivarea addonului nu corupe progresul existent;
- testele pot folosi un fake addon handler.

### Q4.5 - Conditii si prerequisites avansate

Prerequisite-urile simple pe quest completat nu sunt suficiente pentru campanii.

Tipuri de conditii:

| Conditie | Exemplu |
|---|---|
| `quest_completed` | Q06 completat |
| `campaign_stage` | campania este in arc-ul 2 |
| `min_reputation` | reputatie cu garda >= 10 |
| `story_state` | cripta este `sealed` |
| `world_event_active` | festival activ |
| `player_level` | nivel minim |
| `permission` | admin/tester only |
| `cooldown_expired` | repeatable disponibil |

Schema:

```yml
conditions:
  can_start:
    - type: "quest_completed"
      quest: "medieval:blacksmith:Q06"
    - type: "min_reputation"
      scope: "region"
      key: "village_trust"
      amount: 5
```

Reguli:

- conditiile se evalueaza read-only;
- o conditie necunoscuta este eroare;
- auditul trebuie sa poata explica de ce conditia nu trece;
- conditiile nu modifica progresul.

Criterii de acceptare:

- questurile blocate afiseaza motivul in inspect;
- `quest nearest` nu ofera questuri care nu trec conditiile;
- adminul poate rula debug pentru o conditie;
- testele acopera cel putin `quest_completed`, `min_reputation` si `story_state`.

### Q4.6 - Editor si import/export de continut

Editorul vine dupa validator. Altfel editorul doar produce fisiere invalide mai repede.

Niveluri posibile:

| Nivel | Forma | Cand merita |
|---|---|---|
| 1 | `quest inspect` + YAML manual | Q3/Q4 early |
| 2 | comenzi pentru drafturi | dupa validator stabil |
| 3 | GUI in-game simplu | dupa schema stabila |
| 4 | editor extern web/desktop | dupa import/export robust |

Import/export:

```text
quest-pack-export.zip
- pack.yml
- quests/
- dialogs/
- story/
- audit-report.json
- manifest.json
```

Manifest:

```json
{
  "pack_id": "medieval",
  "pack_version": "1.4.0",
  "schema_version": "4",
  "exported_at": 1760000000000,
  "requires_core": ">=1.4.0"
}
```

Reguli:

- importul ruleaza audit strict inainte de activare;
- exportul include versiunea de schema;
- editorul nu poate activa continut cu `ERROR`;
- modificarile live trebuie sa creeze backup.

Criterii de acceptare:

- un pack exportat poate fi importat pe server de staging;
- importul refuza schema incompatibila;
- editorul arata erorile de audit pe quest si obiectiv;
- activarea pack-ului este separata de import.

### Q4.7 - Versionare si migrari

Cand questurile ajung pe server live, ID-urile si schema devin contracte.

Reguli de versionare:

```text
major: schimbare incompatibila de schema sau ID-uri
minor: questuri noi, obiective noi compatibile
patch: texte, reward balancing, bugfix-uri
```

Campuri:

```yml
pack:
  id: "medieval"
  version: "1.4.0"
  schema_version: 4
  migrations:
    - "1.3.0_to_1.4.0"
```

Tipuri de migrare:

| Migrare | Exemplu |
|---|---|
| `rename_objective` | `inspect_spot` -> `inspect_anvil` |
| `split_quest` | Q09 devine Q09A + Q09B |
| `retire_quest` | quest vechi nu mai este ofertabil |
| `backfill_anchor` | reface binding-uri pentru obiectiv nou |
| `convert_reward` | `money` vechi -> `economy:money` |

Reguli:

- migrarea este idempotenta;
- migrarea se testeaza pe backup;
- progresul vechi nu se sterge fara arhivare;
- questurile retrase raman vizibile in istoric.

Criterii de acceptare:

- serverul porneste cu pack nou si ruleaza migrarile o singura data;
- rollback-ul nu lasa DB intr-o stare necunoscuta;
- auditul raporteaza migrari lipsa;
- testele acopera cel putin rename de objective id.

### Q4.8 - Observabilitate si analytics

Adminul trebuie sa stie ce questuri sunt bune, blocate sau abuzate.

Metrici utile:

| Metrica | Intrebare raspunsa |
|---|---|
| `quest_started_total` | cate questuri sunt pornite |
| `quest_completed_total` | cate sunt finalizate |
| `quest_abandoned_total` | unde renunta jucatorii |
| `quest_failed_total` | ce failure rules lovesc |
| `quest_time_to_complete` | cat dureaza un quest |
| `quest_objective_stuck_count` | unde se blocheaza progresul |
| `quest_reward_issued_total` | impact pe economie |

Evenimente interne:

```text
AINPCQuestStartedEvent
AINPCQuestObjectiveProgressEvent
AINPCQuestStageCompletedEvent
AINPCQuestCompletedEvent
AINPCQuestFailedEvent
AINPCQuestRewardIssuedEvent
```

Reguli:

- analytics nu trebuie sa blocheze runtime-ul;
- evenimentele nu contin date inutile;
- debug-ul detaliat poate fi activat pe player/quest;
- metricile se pot exporta in fisier daca nu exista sistem extern.

Criterii de acceptare:

- adminul poate vedea cele mai abandonate questuri;
- un quest blocat la un obiectiv apare in raport;
- reward-urile emise pot fi numarate;
- logging-ul detaliat poate fi dezactivat.

### Q4.9 - Performanta si bugete

Quest runtime-ul nu trebuie sa scaneze toate questurile la fiecare tick.

Bugete recomandate:

| Operatie | Buget |
|---|---|
| evaluare obiectiv pe event | doar questurile active relevante |
| salvare progres | doar la schimbare |
| audit strict | startup/staging/admin command |
| anchor resolving | la acceptare sau repair, nu constant |
| story context pentru prompt | snapshot cache-uit |

Indexuri runtime:

```text
player_uuid -> active quest ids
objective_type -> active quest ids
region_id -> active visit objectives
node_id -> active inspect objectives
npc_id -> active talk/deliver objectives
```

Reguli:

- `PlayerMoveEvent` trebuie throttled sau filtrat pe schimbare de regiune/place;
- DB writes trebuie grupate sau limitate;
- auditul complet nu ruleaza pe main tick;
- prompt context nu reconstruieste tot istoricul.

Criterii de acceptare:

- 50 de jucatori cu questuri active nu produc lag vizibil din move events;
- progresul este salvat fara spam DB;
- auditul strict poate fi rulat manual fara sa blocheze serverul live prea mult;
- debug mode are avertisment de cost.

### Q4.10 - Securitate si permisiuni

Questurile pot da reward-uri, reputatie si unlock-uri, deci comenzile admin trebuie controlate.

Permisiuni recomandate:

```yml
ainpc.quest.player:
  description: "Permite folosirea comenzilor de quest pentru jucator"
ainpc.quest.admin:
  description: "Permite inspectie si modificari admin"
ainpc.quest.audit:
  description: "Permite rularea auditului"
ainpc.quest.repair:
  description: "Permite repair de progres si anchors"
ainpc.quest.import:
  description: "Permite import de pack-uri"
```

Reguli:

- jucatorii nu pot rula `complete`, `repair`, `import`, `activate`;
- comenzile admin care modifica progresul se logheaza;
- importul de pack nu executa comenzi arbitrare;
- reward-urile de tip extern sunt allowlistate;
- debug dump nu expune date sensibile inutile.

Criterii de acceptare:

- un player fara permisiune nu poate modifica progresul;
- repair-ul apare in audit log;
- pack importat cu reward necunoscut este refuzat;
- comenzile destructive cer argument explicit, nu default.

### Q4.11 - Experienta jucatorului

Q4 trebuie sa faca questurile clare pentru jucator, nu doar pentru admin.

UX minim:

- `/quest log` sau alias catre `/ainpc quest log`;
- quest tracked vizibil in actionbar/bossbar optional;
- mesaj clar la progres;
- mesaj clar la blocare;
- mesaj clar la cooldown;
- jurnal simplu pentru questuri completate.

Mesaje recomandate:

```text
Quest actualizat: Urme La Forja - Inspecteaza punctul suspect din fierarie. (1/1)
Quest blocat: ai nevoie de reputatie 5 cu satul.
Quest disponibil mai tarziu: Festivalul Recoltei incepe in 2 zile.
```

Reguli:

- UI-ul nu trebuie sa dezvaluie obiective ascunse;
- mesajele de progres se rate-limiteaza;
- tracking-ul este per jucator;
- completarea si esecul trebuie sa fie evidente.

Criterii de acceptare:

- un jucator poate urma Q06 fara admin;
- un quest blocat explica motivul principal;
- cooldown-ul este vizibil;
- mesajele nu fac spam la fiecare miscare.

### Q4.12 - Compatibilitate si integrare externa

AINPC trebuie sa ramana functional fara dependinte obligatorii de quest/economy/NPC plugins externe.

Strategie:

| Integrare | Regula |
|---|---|
| Vault/economie | optional, prin reward handler extern |
| Citizens | optional, prin adapter NPC |
| BetonQuest/Quests | inspiratie/import limitat, nu dependinta hard |
| WorldEdit | doar mapping/build adapter, nu runtime quest |
| PlaceholderAPI | optional pentru afisare |

Reguli:

- core-ul porneste fara pluginuri externe;
- integrarea externa se declara in addon descriptor;
- lipsa dependintei dezactiveaza doar tipurile afectate;
- importul din alte pluginuri produce drafturi, nu questuri active direct.

Criterii de acceptare:

- serverul porneste fara Vault/Citizens;
- questurile care folosesc `economy:money` sunt dezactivate daca handlerul lipseste;
- auditul explica dependinta lipsa;
- importul extern trece prin validator.

### Q4.13 - Test matrix pentru productie

Q4 are nevoie de teste mai late decat unit tests.

Matrice:

| Nivel | Ce testeaza |
|---|---|
| Unit | handlers, rewards, conditions |
| Contract | YAML schema, audit, pack loading |
| Integration | DB repository, migrations, story state |
| Paper smoke | comenzi, NPC interaction, movement, death events |
| Regression | Q01-Q09 raman completabile |
| Load smoke | multi-player cu questuri active |

Scenarii obligatorii:

- acceptare quest;
- progres partial;
- restart server;
- completare;
- abandon;
- failure timeout;
- reward story;
- quest blocat de conditie;
- quest repeatable in cooldown;
- pack invalid dezactivat.

Criterii de acceptare:

- fiecare release ruleaza testele contract;
- smoke test-ul acopera cel putin un flow complet;
- migrarile au test de rollback sau backup restore;
- bugurile de quest primesc test de regresie.

### Q4.14 - Politica de continut

Pe termen lung, nu orice quest generat sau scris manual trebuie acceptat in pack.

Reguli editoriale:

- questul trebuie sa aiba scop clar;
- obiectivele trebuie sa foloseasca sisteme existente;
- reward-ul trebuie sa fie proportional;
- story effect-ul trebuie sa fie reversibil sau intentionat permanent;
- questurile principale trebuie testate manual;
- questurile repeatable trebuie sa aiba cooldown si recompensa mica;
- textele trebuie sa fie scurte si clare in UI.

Checklist de review:

- exista motiv narativ;
- exista motiv tehnic pentru fiecare obiectiv;
- nu exista coordonate brute;
- nu exista dependency ascuns;
- reward-ul este auditat;
- failure/abandon este definit daca questul poate ramane blocat;
- questul poate fi reparat sau dezactivat.

Criterii de acceptare:

- pack-ul nu creste cu questuri duplicate ca gameplay;
- questurile importante au test manual documentat;
- fiecare quest nou are owner sau categorie;
- continutul dezactivat ramane in arhiva, nu in runtime activ.

## Roadmap Q4 recomandat

Ordinea recomandata pentru Q4:

1. adauga `CampaignDefinition` si `player_campaigns`;
2. grupeaza Q06-Q09 intr-o campanie mica;
3. adauga conditii avansate read-only;
4. introdu world events pornite manual de admin;
5. extinde contextul pentru multi-settlement;
6. stabilizeaza API-ul pentru objective/reward/condition handlers;
7. adauga import/export de quest pack;
8. adauga versionare si migrari pentru pack-uri;
9. adauga analytics si rapoarte de quest;
10. adauga bugete de performanta si indexuri runtime;
11. intareste permisiunile si audit log-ul admin;
12. documenteaza test matrix si politica de continut.

## Definitia de gata pentru Q4

Q4 este gata cand:

- campaniile exista si pot grupa questuri reale;
- world events pot bloca/debloca questuri controlat;
- acelasi quest template poate rula sigur in mai multe sate sau regiuni;
- addonurile pot inregistra tipuri noi prin API validat;
- conditiile avansate pot explica de ce un quest este blocat;
- pack-urile au versiune, schema si migrari;
- import/export ruleaza prin audit strict;
- analytics arata completari, abandonuri si blocaje;
- runtime-ul are indexuri si nu scaneaza inutil la fiecare event;
- permisiunile separa clar player, admin, audit, repair si import;
- experienta jucatorului este clara fara acces la comenzi admin;
- test matrix-ul acopera restart, migrari, failure si pack invalid.

## Partea 5 - platforma de continut si quest director

Partea 5 este etapa in care sistemul de questuri nu mai este doar un runtime bun pentru pack-uri, ci devine o platforma de continut. Asta inseamna selectie dinamica de questuri, generare controlata, campanii live, questuri cooperative, localizare, marketplace de pack-uri, reguli anti-abuz si pipeline CI pentru continut.

Q5 trebuie tratat ca nivel avansat. Nu este prioritar inainte de Q2-Q4. Daca este implementat prea devreme, creeaza un sistem mare peste o fundatie instabila.

Directia Q5:

- un `QuestDirector` alege ce questuri sunt potrivite pentru jucator si lume;
- generarea procedurala produce drafturi validate, nu questuri active direct;
- campaniile pot avea sezoane, episoade si live events;
- questurile cooperative au progres partajat si contributie individuala;
- pack-urile externe pot fi certificate, versionate si retrase;
- continutul are localizare, chei de text si reguli editoriale;
- economia questurilor are ledger, anti-farm si detectie de exploit;
- release-ul de continut trece prin CI, audit strict si teste automate.

### Q5.0 - Preconditii

Nu incepe Q5 daca:

- campaniile Q4 nu exista sau nu sunt persistente;
- `QuestConditionHandler` nu exista;
- pack-urile nu au versiune si schema;
- import/export nu ruleaza audit strict;
- runtime-ul nu are indexuri de performanta;
- nu exista audit log pentru actiuni admin;
- nu exista analytics minime pentru completari, abandonuri si blocaje.

Regula: Q5 se bazeaza pe date bune. Fara analytics, directorul de questuri ar lua decizii oarbe.

### Q5.1 - QuestDirector

`QuestDirector` este serviciul care decide ce questuri sunt potrivite pentru un jucator intr-un anumit context. El nu completeaza obiective si nu acorda reward-uri. El doar selecteaza, prioritizeaza si explica disponibilitatea.

Responsabilitati:

```text
QuestDirector
- gaseste questuri disponibile
- prioritizeaza questuri relevante
- evita repetitia excesiva
- respecta campanii, world events si reputatie
- sugereaza quest tracked
- explica de ce un quest este blocat
```

Input:

```text
PlayerQuestProfile
- player_uuid
- active_quests
- completed_quests
- failed_quests
- reputation_snapshot
- current_region
- current_settlement
- recent_activity
- preferred_playstyle
```

Output:

```text
QuestRecommendation
- quest_id
- reason
- priority
- source
- expires_at
- blocked_reasons
```

Surse de recomandare:

| Sursa | Exemplu |
|---|---|
| `campaign` | urmatorul quest din Criza Forjei |
| `nearby_npc` | NPC-ul apropiat are quest relevant |
| `world_event` | festivalul a pornit questuri temporare |
| `reputation` | garda ofera questuri dupa incredere |
| `recovery` | quest blocat are repair sau retry |
| `routine` | NPC-ul este disponibil doar la lucru |

Reguli:

- directorul este read-only fata de progres;
- recomandarile sunt cache-uite pe termen scurt;
- recomandarile nu ocolesc conditiile;
- `nearest` poate cere directorului o sugestie, dar acceptarea ramane explicita;
- adminul poate inspecta motivul recomandarii.

Criterii de acceptare:

- doua questuri disponibile sunt ordonate explicabil;
- un quest blocat are motive concrete;
- directorul nu recomanda questuri invalide;
- recomandarile se schimba cand reputatia sau story state-ul se schimba.

### Q5.2 - Generare procedurala controlata

Generarea procedurala trebuie sa produca `QuestDraft`, nu quest activ. Runtime-ul ramane determinist.

Pipeline:

```text
QuestSeed
-> QuestDraft
-> schema validation
-> anchor dry-run
-> reward balance check
-> audit strict
-> smoke test candidate
-> manual/admin approval
-> active quest pack
```

`QuestSeed`:

```json
{
  "theme": "forge_sabotage",
  "region": "satul_central",
  "giver_profession": "blacksmith",
  "desired_kind": "investigation",
  "difficulty": "minor",
  "max_objectives": 3
}
```

Reguli de generare:

- foloseste numai `objective.type` existente in registry;
- foloseste numai reward-uri validate;
- referintele de mapping se aleg din `WorldContextSnapshot`;
- nu inventa NPC-uri permanente fara pipeline de spawn;
- nu creeaza story state permanent fara confirmare;
- nu activeaza questuri cu audit `ERROR`.

Template procedural:

```text
InvestigationTemplate
- talk_to_giver
- visit_place by tag
- inspect_node by type
- return_to_giver
- record_story_event
```

Reguli pentru AI:

- AI-ul completeaza texte si propune combinatii;
- validatorul decide daca draftul este valid;
- adminul decide activarea;
- promptul nu primeste secrete sau date inutile;
- raspunsul AI este tratat ca input nesigur.

Criterii de acceptare:

- un draft generat invalid este respins;
- un draft valid poate fi inspectat ca YAML normalizat;
- anchor dry-run arata daca tintele exista;
- reward balance check avertizeaza pentru recompense prea mari.

### Q5.3 - Personalizare fara nedeterminism

Personalizarea trebuie sa ajusteze selectia, nu regulile de runtime.

Profiluri utile:

| Profil | Semnal |
|---|---|
| `explorer` | finalizeaza obiective de mapping |
| `fighter` | finalizeaza hunt/defense |
| `social` | interactioneaza mult cu NPC-uri |
| `builder` | prefera crafting/delivery |
| `story` | urmareste campanii si lore |

Reguli:

- profilul influenteaza recomandari, nu reward-uri critice;
- playerul poate ignora recomandarile;
- personalizarea nu blocheaza continut principal;
- nu ascunde questuri necesare campaniei;
- adminul poate dezactiva personalizarea.

Exemplu:

```yml
quest_director:
  personalization:
    enabled: true
    max_priority_bonus: 15
    never_hide_campaign_quests: true
```

Criterii de acceptare:

- jucatorul orientat combat primeste mai des hunt quests recomandate;
- campaniile principale raman vizibile;
- `quest inspect availability` arata bonusul de personalizare;
- dezactivarea personalizarii produce ordine neutra.

### Q5.4 - Questuri cooperative

Questurile cooperative au progres partajat, dar contributie individuala. Ele sunt utile pentru aparare, raid, constructie sau investigatii mari.

Model:

```text
GroupQuestProgress
- group_id
- quest_id
- status
- shared_objective_progress
- participant_progress
- started_at
- completed_at
```

Tipuri de grup:

| Grup | Exemplu |
|---|---|
| `party` | 2-5 jucatori |
| `settlement` | toti jucatorii care ajuta satul |
| `server_event` | event global |
| `guild/faction` | daca exista addon |

Schema:

```yml
quest:
  code: "Q30"
  kind: "defense"
  group_mode: "party"
  min_players: 2
  max_players: 5
  contribution_required: true
```

Reguli:

- progresul partajat nu inseamna reward gratuit pentru toti;
- fiecare participant are contributie minima;
- abandonul unui participant nu strica progresul grupului automat;
- reward-urile pot fi per participant sau per grup;
- griefing-ul trebuie limitat prin owner/party leader.

Criterii de acceptare:

- doi jucatori pot progresa acelasi obiectiv;
- un jucator fara contributie minima nu primeste reward complet;
- restartul pastreaza progresul grupului;
- `quest log` arata participantii si progresul partajat.

### Q5.5 - Sezoane, episoade si continut live

Sezoanele organizeaza continutul pe perioade mari. Ele pot activa campanii, modifica recomandari si retrage questuri temporare.

Model:

```text
SeasonDefinition
- id
- display_name
- starts_at
- ends_at
- active_campaigns
- active_world_events
- retired_quests
- catchup_rules
```

Exemplu:

```yml
seasons:
  s1_forge_and_road:
    display_name: "Forja si Drumul de Nord"
    active_campaigns:
      - "medieval:forge_crisis"
      - "medieval:north_road"
    catchup_rules:
      allow_late_completion: true
```

Reguli:

- sezoanele nu sterg progresul;
- questurile retrase pot ramane in istoric;
- catch-up-ul trebuie explicit;
- reward-urile sezoniere sunt auditate separat;
- activarea sezonului se face prin staging si audit strict.

Criterii de acceptare:

- un sezon activeaza campaniile declarate;
- questurile sezoniere nu mai sunt ofertabile dupa final;
- jucatorii pot vedea ce questuri sezoniere au completat;
- rollback-ul de sezon nu sterge progresul.

### Q5.6 - Marketplace si certificare pack-uri

Daca pack-urile devin externe, sistemul are nevoie de certificare si allowlist.

Stari pentru pack:

| Status | Sens |
|---|---|
| `draft` | in lucru |
| `validated` | trece auditul local |
| `certified` | aprobat pentru server |
| `active` | ruleaza |
| `deprecated` | nu se mai recomanda |
| `retired` | nu mai este ofertabil |
| `blocked` | refuzat din motive de siguranta |

Manifest extins:

```json
{
  "pack_id": "medieval",
  "version": "1.5.0",
  "schema_version": 5,
  "author": "ainpc-core",
  "license": "internal",
  "requires_core": ">=1.5.0",
  "capabilities": ["quests", "campaigns", "story_events"],
  "signature": "optional"
}
```

Reguli:

- serverul poate incarca doar pack-uri allowlistate;
- pack-urile externe ruleaza audit strict;
- semnatura este optionala initial, dar designul o lasa posibila;
- pack-urile cu reward-uri externe declara dependinte;
- pack-urile blocate nu pot fi activate prin comanda simpla.

Criterii de acceptare:

- un pack neallowlistat nu se activeaza automat;
- auditul afiseaza autor, versiune si dependinte;
- un pack retras ramane in istoric;
- activarea pack-ului este logata.

### Q5.7 - Localizare si text keys

Textele inline in YAML devin greu de tradus si mentinut. Q5 trebuie sa introduca text keys.

Format:

```yml
quest:
  code: "Q06"
  title_key: "quest.medieval.q06.title"
  description_key: "quest.medieval.q06.description"
```

Fisier de limba:

```yml
ro_ro:
  quest.medieval.q06.title: "Urme La Forja"
  quest.medieval.q06.description: "Fierarul cere verificarea unui punct suspect."
```

Reguli:

- textul inline ramane fallback;
- cheile lipsa sunt warning, nu eroare, in pack intern;
- cheile lipsa pot fi eroare in audit strict;
- variabilele in text sunt allowlistate;
- AI-ul poate propune traduceri, dar validatorul verifica placeholder-ele.

Placeholder:

```text
{player_name}
{npc_name}
{place_name}
{amount}
```

Criterii de acceptare:

- UI-ul poate afisa questul in limba configurata;
- placeholder-ele lipsa sunt detectate;
- fallback-ul functioneaza daca lipseste traducerea;
- auditul listeaza textele fara chei.

### Q5.8 - Anti-abuz si reward ledger

Pe server live, questurile pot fi exploatate. Reward-urile trebuie inregistrate intr-un ledger.

Ledger:

```text
quest_reward_ledger
- id
- player_uuid
- quest_id
- reward_type
- reward_key
- amount
- issued_at
- source
- idempotency_key
```

Reguli:

- fiecare reward are `idempotency_key`;
- completarea repetata nu acorda reward dublu accidental;
- admin repair poate marca reward ca reaplicat sau compensat;
- reward-urile rare au rate limits;
- completari suspecte apar in raport.

Detectii simple:

| Semnal | Posibil abuz |
|---|---|
| completari foarte rapide | exploit obiectiv |
| multe abandonuri | farming/reset abuse |
| reward duplicat | bug/idempotenta lipsa |
| progres imposibil | handler gresit sau cheat |
| acelasi quest repeatable spam | cooldown bypass |

Criterii de acceptare:

- reward-ul final nu se dubleaza dupa restart;
- adminul poate vedea reward-urile acordate;
- auditul gaseste reward-uri fara idempotency;
- rapoartele arata completari suspecte.

### Q5.9 - Politica de date si arhivare

Pe termen lung, DB-ul de questuri creste. Trebuie stabilit ce ramane activ, ce ramane istoric si ce se arhiveaza.

Categorii:

| Date | Retentie recomandata |
|---|---|
| quest activ | pana la finalizare/abandon |
| quest completat | istoric persistent |
| objective history detaliat | arhivabil dupa perioada |
| debug dumps | stergere dupa rotatie |
| analytics agregate | persistente pe termen lung |
| drafts neactivate | stergere/arhivare dupa review |

Reguli:

- progresul activ nu se sterge automat;
- istoricul de completare ramane pentru prerequisites;
- debug dump-urile au rotatie configurabila;
- arhivarea nu rupe campaniile;
- exportul trebuie sa poata exclude debug verbose.

Configurare:

```yml
quests:
  retention:
    debug_dump_days: 14
    draft_days: 30
    detailed_history_days: 90
    keep_completion_history: true
```

Criterii de acceptare:

- debug dump-urile vechi pot fi curatate;
- prerequisites continua sa functioneze dupa arhivare;
- adminul poate exporta istoricul unui jucator;
- arhivarea este logata.

### Q5.10 - CI pentru continut

Quest packs trebuie testate ca un produs, nu doar incarcate manual.

Pipeline:

```text
format check
-> schema validation
-> audit strict
-> pack dependency check
-> migration test
-> reward balance lint
-> generated snapshot
-> Paper smoke candidate
```

Fisiere generate:

```text
build/quest-audit-report.json
build/quest-pack-snapshot.json
build/quest-risk-report.txt
build/quest-localization-report.txt
```

Lint rules:

- quest fara owner;
- objective fara ID stabil;
- reward prea mare pentru `reward_tier`;
- text lipsa;
- stage fara iesire;
- condition necunoscuta;
- quest repeatable fara limite.

Criterii de acceptare:

- un pack invalid pica pipeline-ul;
- raportul de audit este atasat la release;
- snapshot-ul detecteaza schimbari neasteptate;
- smoke test-ul poate fi rulat pe subset de questuri schimbate.

### Q5.11 - Compatibilitate API si deprecari

Cand addonurile folosesc API-ul, schimbarea brusca rupe ecosistemul.

Reguli:

- API-ul public are versiune;
- tipurile de objective/reward au namespace;
- deprecarea are perioada de tranzitie;
- auditul avertizeaza pentru tipuri deprecated;
- adapterele vechi sunt eliminate doar la major version.

Exemplu:

```text
objective type `talk_to_npc` -> activ
objective type `talk_npc` -> deprecated alias
objective type `old_delivery` -> removed in schema 6
```

Criterii de acceptare:

- addonul poate verifica versiunea API;
- auditul arata tipuri deprecated;
- schema veche poate fi migrata;
- release notes listeaza breaking changes.

### Q5.12 - Disaster recovery pentru questuri

Pentru server live, trebuie documentat cum repari continut stricat.

Scenarii:

| Scenariu | Actiune |
|---|---|
| pack invalid activat | dezactiveaza pack, ruleaza rollback |
| migration stricata | restore backup, ruleaza dry-run |
| reward duplicat | foloseste ledger si compensare manuala |
| quest blocat masiv | disable quest, repair anchors, mark retry |
| handler addon lipsa | dezactiveaza questurile dependente |

Comenzi posibile:

```text
/ainpc quest pack disable <pack_id>
/ainpc quest migrate dry-run <pack_id>
/ainpc quest repair player <player> <quest>
/ainpc quest reward ledger <player>
/ainpc quest incident report
```

Reguli:

- toate actiunile de repair sunt logate;
- repair-ul poate rula dry-run;
- compensarea reward-urilor nu sterge ledger-ul;
- incident report include pack, versiune si questuri afectate.

Criterii de acceptare:

- adminul poate dezactiva rapid un pack stricat;
- dry-run arata ce ar modifica repair-ul;
- ledger-ul ajuta la investigarea reward-urilor;
- incident report poate fi atasat la bug report.

### Q5.13 - Questuri sistemice

Questurile sistemice sunt generate sau activate din starea lumii: economie, populatie, pericole, lipsuri, rutine NPC. Ele sunt utile, dar trebuie limitate.

Exemple:

| Stare lume | Quest sistemic |
|---|---|
| hambar gol | fermierul cere provizii |
| drum periculos | garda cere curatare |
| reputatie scazuta | satul cere dovada de incredere |
| NPC bolnav | healer cere ingrediente |
| constructie neterminata | builder cere materiale |

Reguli:

- questul sistemic foloseste template-uri validate;
- instanta generata are `quest_instance_id`;
- cauza este salvata in `quest_variables`;
- frecventa este limitata;
- nu suprascrie campaniile principale.

Schema:

```yml
systemic_templates:
  low_food_delivery:
    trigger:
      type: "settlement_resource_below"
      resource: "food"
      amount: 20
    quest_template: "medieval:farmer:food_delivery"
    cooldown_seconds: 86400
```

Criterii de acceptare:

- starea lumii poate genera o instanta de quest;
- cooldown-ul previne spam-ul;
- auditul arata template-ul si triggerul;
- instanta se poate dezactiva fara sa stergi template-ul.

### Q5.14 - Governance pentru continut

Pe masura ce exista multe questuri, trebuie reguli despre cine poate adauga, aproba si activa continut.

Roluri:

| Rol | Permisiuni |
|---|---|
| `author` | creeaza drafturi |
| `reviewer` | ruleaza audit si comenteaza |
| `tester` | activeaza pe staging |
| `release_manager` | activeaza pe live |
| `admin` | repair si emergency disable |

Flux:

```text
draft
-> review
-> staging
-> smoke tested
-> approved
-> scheduled
-> live
-> monitored
```

Reguli:

- autorul nu activeaza direct pe live;
- fiecare pack are changelog;
- fiecare release are audit report;
- emergency disable poate fi facut rapid;
- continutul retras ramane trasabil.

Criterii de acceptare:

- exista status pentru pack si quest;
- activarea live este logata cu actor;
- changelog-ul poate fi exportat;
- emergency disable nu sterge progresul.

## Roadmap Q5 recomandat

Ordinea recomandata pentru Q5:

1. adauga `QuestDirector` read-only pentru recomandari;
2. adauga `QuestRecommendation` si inspectie pentru motive;
3. introdu `QuestSeed` si pipeline de draft procedural;
4. adauga localizare prin text keys si fallback;
5. implementeaza reward ledger cu idempotency;
6. adauga cooperative quest model pentru party mic;
7. adauga sezoane ca strat peste campanii;
8. adauga CI pentru quest packs;
9. stabileste reguli de API versioning si deprecari;
10. adauga disaster recovery commands;
11. introdu systemic quest templates limitate;
12. documenteaza governance pentru continut live.

## Definitia de gata pentru Q5

Q5 este gata cand:

- `QuestDirector` recomanda questuri explicabil si read-only;
- drafturile procedurale trec prin validare, audit si aprobare;
- personalizarea influenteaza prioritizarea, nu regulile de completare;
- questurile cooperative au progres partajat si contributie individuala;
- sezoanele pot activa campanii si evenimente fara sa stearga progres;
- pack-urile au status, certificare/allowlist si istoric;
- textele pot folosi localization keys cu fallback;
- reward ledger previne dublarea reward-urilor;
- politica de retentie si arhivare este configurabila;
- CI-ul de continut poate bloca pack-uri invalide;
- API-ul public are versiune si politica de deprecari;
- exista proceduri clare pentru incident, repair si rollback;
- questurile sistemice sunt generate doar din template-uri validate;
- continutul live are roluri, review si changelog.

## Partea 6 - standard de platforma si operare pe termen lung

Partea 6 este etapa in care sistemul de questuri devine un standard intern pentru continut live, nu doar o colectie de servicii. Aici intra contracte stabile, compatibilitate pe termen lung, rule testing, replay determinist, cross-server, data warehouse, release trains, hardening de securitate si mentenanta pentru ani de continut.

Q6 nu adauga neaparat mai multe mecanici vizibile pentru jucator. Q6 face ca mecanicile existente sa poata fi operate, masurate, migrate si reparate fara riscuri mari.

Directia Q6:

- schema de quest devine contract versionat si documentat;
- runtime-ul poate face replay determinist pentru debugging;
- regulile de selectie, conditii si reward pot fi testate izolat;
- continutul poate fi lansat pe release trains;
- datele de quest pot fi agregate pentru analytics si balans;
- serverele multiple pot partaja pack-uri si campanii fara coliziuni;
- incidentele pot fi investigate cu trail complet;
- API-ul public are compatibilitate declarata si teste de contract.

### Q6.0 - Preconditii

Nu incepe Q6 daca:

- Q5 nu are `QuestDirector` read-only;
- reward ledger-ul nu exista;
- pack-urile nu au status si versionare;
- CI-ul de continut nu blocheaza pack-uri invalide;
- nu exista proceduri de incident si rollback;
- questurile sistemice nu sunt limitate prin template-uri validate;
- analytics nu poate raporta completari, abandonuri si reward-uri.

Regula: Q6 nu compenseaza lipsa de disciplina din Q5. Q6 presupune ca platforma exista si o transforma in sistem guvernat.

### Q6.1 - Specificatie formala de schema

Schema YAML trebuie documentata ca un contract. Asta inseamna versiuni, campuri obligatorii, campuri optionale, tipuri permise si reguli de compatibilitate.

Artefacte recomandate:

```text
docs/schema/
- quest-schema-v1.md
- quest-schema-v2.md
- quest-pack-schema.md
- campaign-schema.md
- reward-schema.md
- condition-schema.md
- migration-schema.md
```

Contract minim:

```text
QuestDefinition
- id: namespaced string
- code: stable string
- schema_version: integer
- lifecycle_status: draft | active | deprecated | retired
- quest: object
- roles: object
- dialogs: object or references
- metadata: object
```

Reguli:

- fiecare pack declara `schema_version`;
- fiecare breaking change creste versiunea majora;
- campurile necunoscute sunt warning sau error in functie de strict mode;
- validatorul produce raport de compatibilitate;
- documentatia include exemple valide si invalide.

Exemplu de raport:

```json
{
  "schema_version": 6,
  "compatible": true,
  "warnings": [
    "Quest medieval:blacksmith:Q06 uses inline description fallback."
  ],
  "errors": []
}
```

Criterii de acceptare:

- un pack poate fi validat fara pornirea serverului Paper;
- schema veche este acceptata doar daca exista compat layer;
- auditul raporteaza schema si compatibilitatea;
- fiecare camp nou are documentatie si test de contract.

### Q6.2 - Rule engine determinist

Conditiile, branch rules si reward eligibility trebuie evaluate printr-un motor determinist. Nu trebuie sa fie raspandite in metode diferite.

Model:

```text
QuestRuleEngine
- evaluateCondition(...)
- evaluateBranch(...)
- evaluateRewardEligibility(...)
- explain(...)
```

Input:

```text
RuleContext
- player_snapshot
- quest_progress
- campaign_progress
- story_snapshot
- reputation_snapshot
- world_event_snapshot
- time_snapshot
```

Output:

```text
RuleResult
- passed
- reason_code
- human_message_key
- debug_details
```

Reguli:

- evaluarea este read-only;
- aceleasi inputuri produc acelasi rezultat;
- rezultatul poate fi explicat;
- regulile nu scriu DB;
- timpul curent este injectat ca snapshot, nu citit direct peste tot.

Criterii de acceptare:

- o conditie poate fi testata cu fixture JSON;
- `quest inspect availability` foloseste acelasi engine ca runtime-ul;
- branch-ul ales poate fi reprodus din snapshot;
- time-based rules pot fi testate fara server live.

### Q6.3 - Replay determinist si event sourcing partial

Pentru buguri complexe, debug dump-ul nu este suficient. Ai nevoie de replay pentru evenimentele de quest.

Evenimente stocate:

```text
QuestEventLog
- event_id
- player_uuid
- quest_id
- event_type
- payload
- occurred_at
- source
- idempotency_key
```

Evenimente utile:

| Eveniment | Cand apare |
|---|---|
| `QUEST_OFFERED` | questul a fost oferit |
| `QUEST_ACCEPTED` | jucatorul a acceptat |
| `OBJECTIVE_PROGRESS` | un obiectiv a progresat |
| `STAGE_COMPLETED` | stage complet |
| `BRANCH_SELECTED` | branch decis |
| `REWARD_ISSUED` | reward aplicat |
| `QUEST_COMPLETED` | quest complet |
| `QUEST_FAILED` | quest esuat |
| `QUEST_REPAIRED` | admin repair |

Reguli:

- event log-ul nu inlocuieste tabelele de stare curenta;
- evenimentele sunt append-only;
- replay-ul ruleaza in mod test/debug;
- repair-ul produce eveniment nou, nu sterge evenimentul vechi;
- event payload-ul este versionat.

Comenzi:

```text
/ainpc quest eventlog <player> <quest>
/ainpc quest replay <player> <quest> dry-run
/ainpc quest replay <player> <quest> export
```

Criterii de acceptare:

- un quest completat poate fi reconstituit din event log;
- reward-ul nu se reaplica in replay dry-run;
- un repair este vizibil in istoric;
- replay-ul detecteaza divergenta intre event log si stare curenta.

### Q6.4 - Contract tests pentru addonuri

Addonurile care extind questurile trebuie verificate impotriva API-ului public.

Kit de teste:

```text
ainpc-quest-addon-testkit
- fake QuestEngine
- fake QuestProgressRepository
- sample QuestObjectiveContext
- sample QuestRewardContext
- validation fixtures
```

Ce trebuie testat:

| Tip addon | Test obligatoriu |
|---|---|
| objective handler | validate + handle + no-match |
| reward handler | validate + idempotency + failure |
| condition handler | passed + failed + explain |
| quest pack provider | load + audit + unload |

Reguli:

- addonul nu acceseaza direct DB-ul core;
- addonul declara versiunea API suportata;
- handlerul trebuie sa fie deterministic pentru acelasi context;
- handlerul nu blocheaza main thread cu operatii lente.

Criterii de acceptare:

- un addon incompatibil este respins la startup;
- testkit-ul poate rula fara server Paper complet;
- auditul raporteaza handlerul si addonul sursa;
- dezinstalarea addonului dezactiveaza doar continutul dependent.

### Q6.5 - Cross-server si retea de servere

Daca AINPC ruleaza pe mai multe servere, progresul si pack-urile trebuie sa aiba reguli clare.

Modele posibile:

| Model | Descriere |
|---|---|
| `single_server` | progres local, cel mai simplu |
| `shared_pack_local_progress` | pack comun, progres per server |
| `shared_progress` | progres comun intre servere |
| `hub_campaign` | campanie globala, questuri locale |

Reguli pentru `shared_progress`:

- `server_id` este inclus in event log;
- questurile locale au `world_id` si `settlement_id`;
- conflictele se rezolva prin idempotency keys;
- reward-urile cross-server folosesc ledger global sau sync controlat;
- downtime-ul unui server nu corupe progresul global.

Persistenta:

```text
server_id
cluster_id
quest_instance_id
progress_scope: local | cluster
```

Criterii de acceptare:

- acelasi player nu primeste reward dublu pe doua servere;
- un quest local nu progreseaza pe server gresit;
- pack version mismatch este detectat;
- sync failure nu blocheaza complet serverul local daca modul permite.

### Q6.6 - Data warehouse pentru analytics

Analytics-ul operational este diferit de datele runtime. Q6 trebuie sa separe datele agregate de tabelele de joc.

Export recomandat:

```text
analytics/
- quest_events.ndjson
- quest_rewards.ndjson
- quest_sessions.ndjson
- quest_pack_versions.ndjson
```

Dimensiuni:

| Dimensiune | Exemplu |
|---|---|
| quest_id | `medieval:blacksmith:Q06` |
| pack_version | `1.5.0` |
| server_id | `survival-01` |
| player_segment | `new`, `returning`, `veteran` |
| region_type | `village`, `road`, `crypt` |
| objective_type | `visit_place`, `kill_mob` |

Metrici agregate:

- rata de acceptare;
- rata de completare;
- timp median pana la completare;
- obiectivul unde se blocheaza jucatorii;
- reward total emis;
- questuri recomandate dar ignorate;
- incident rate per pack version.

Reguli:

- analytics export este async;
- datele personale inutile se anonimizeaza sau se evita;
- exportul nu afecteaza tick-ul serverului;
- rapoartele folosesc pack version pentru comparatii corecte.

Criterii de acceptare:

- se poate compara Q06 intre doua versiuni de pack;
- adminul vede questuri cu abandon mare;
- reward economy poate fi agregata pe saptamana;
- exportul poate fi dezactivat.

### Q6.7 - A/B testing controlat

A/B testing-ul poate ajuta balansul, dar este riscant daca schimba progresul critic.

Ce poate fi testat:

- texte de dialog;
- prioritate de recomandare;
- reward minor;
- cooldown;
- numar de obiective pentru questuri repeatable;
- UI message timing.

Ce nu trebuie testat fara migrare:

- `objective_id`;
- structura de stages;
- branch rules principale;
- reward-uri permanente mari;
- story state ireversibil.

Schema:

```yml
experiments:
  q06_reward_minor:
    status: "active"
    variants:
      A:
        reward_override:
          emerald: 1
      B:
        reward_override:
          emerald: 2
    guardrails:
      max_reward_tier: "minor"
```

Reguli:

- experimentul este optional si auditat;
- playerul ramane in acelasi variant;
- variantul este salvat in `quest_variables` sau profil;
- guardrails blocheaza modificari periculoase;
- rezultatele se masoara prin analytics.

Criterii de acceptare:

- doi jucatori pot primi variante diferite controlat;
- acelasi jucator nu schimba varianta dupa restart;
- auditul respinge experiment cu reward major;
- raportul arata completare pe variant.

### Q6.8 - Release trains si canary rollout

Continutul live trebuie lansat etapizat.

Release train:

```text
draft
-> content CI
-> staging
-> canary
-> partial rollout
-> full rollout
-> monitored
-> archived
```

Canary:

```yml
rollout:
  pack_id: "medieval"
  version: "1.6.0"
  stages:
    - percent: 5
      duration_minutes: 60
    - percent: 25
      duration_minutes: 180
    - percent: 100
```

Reguli:

- rollout-ul poate fi oprit;
- questurile active nu sunt mutate brusc intre variante;
- rollback-ul pastreaza progresul compatibil;
- canary-ul nu se aplica questurilor principale fara test manual;
- incident threshold opreste rollout-ul automat daca exista suport.

Criterii de acceptare:

- pack-ul poate fi activat doar pentru subset de jucatori;
- incidentul opreste rollout-ul;
- progresul canary ramane valid dupa full rollout;
- release report listeaza schimbari si audit status.

### Q6.9 - Hardening de securitate pentru continut

Pack-urile si addonurile sunt suprafata de atac. Q6 trebuie sa trateze continutul ca input nesigur.

Riscuri:

| Risc | Mitigare |
|---|---|
| reward arbitrar | reward registry allowlist |
| comenzi server din YAML | interzis sau handler explicit securizat |
| YAML prea mare | limite de marime |
| recursive includes | adancime maxima |
| path traversal la import | normalizare si sandbox folder |
| addon malitios | API limitat si permisiuni |
| spam de evenimente | rate limits |

Limite recomandate:

```yml
quests:
  limits:
    max_quests_per_pack: 500
    max_objectives_per_quest: 25
    max_stages_per_quest: 20
    max_dialog_lines_per_quest: 200
    max_import_size_mb: 10
```

Reguli:

- importul nu scrie in afara folderului de pack-uri;
- include-urile sunt relative la pack root;
- comenzi arbitrare sunt dezactivate implicit;
- addonurile nu primesc obiecte mutable interne;
- orice bypass de audit este logat si cere permisiune inalta.

Criterii de acceptare:

- importul cu path traversal este refuzat;
- pack-ul prea mare este refuzat sau avertizat;
- reward necunoscut nu ruleaza;
- bypass-ul de audit apare in audit log.

### Q6.10 - Compatibilitate cu lumi vechi

Serverele live au lumi si DB-uri vechi. Q6 trebuie sa defineasca suportul pentru legacy.

Politici:

| Politica | Sens |
|---|---|
| `support` | merge normal |
| `compat` | merge cu warnings |
| `migrate` | cere migrare |
| `read_only` | se poate inspecta, nu modifica |
| `unsupported` | blocat clar |

Legacy cases:

- questuri fara `objective_id`;
- progres fara `current_stage_id`;
- pack fara namespace;
- reward vechi de tip `money`;
- bindings fara `settlement_id`;
- story events fara schema version.

Reguli:

- compat layer-ul are termen de expirare;
- migrarile au dry-run;
- auditul listeaza toate datele legacy;
- datele unsupported nu sunt modificate automat.

Criterii de acceptare:

- o DB veche poate fi inspectata;
- migrarea poate fi simulata;
- questurile legacy active pot fi finalizate sau retrase controlat;
- release notes mentioneaza suportul legacy.

### Q6.11 - Formalizare lifecycle pentru questuri

Fiecare quest trebuie sa aiba lifecycle explicit.

Statusuri:

```text
draft
validated
staging
active
deprecated
retired
archived
blocked
```

Tranzitii permise:

```text
draft -> validated -> staging -> active
active -> deprecated -> retired -> archived
active -> blocked
blocked -> active
retired -> archived
```

Reguli:

- `blocked` este pentru probleme de siguranta;
- `deprecated` inseamna nu mai este recomandat, dar poate exista;
- `retired` inseamna nu mai este ofertabil;
- `archived` ramane doar pentru istoric;
- tranzitiile sunt logate.

Criterii de acceptare:

- un quest retired nu mai este oferit;
- un quest deprecated apare cu warning in audit;
- un quest blocked nu poate fi activat fara permisiune inalta;
- istoricul completarii ramane pentru prerequisites.

### Q6.12 - QA automat pentru continut narativ

Pe langa schema, textele si flow-ul trebuie verificate.

Verificari automate:

- dialog lipsa pentru stari principale;
- text prea lung pentru UI;
- placeholder necunoscut;
- quest fara hint;
- obiectiv cu descriere neclara;
- reward fara descriere;
- story event fara titlu;
- acelasi text duplicat in multe questuri.

Raport:

```text
Quest Narrative QA
- missing_dialog: 3
- long_lines: 8
- unknown_placeholders: 1
- duplicate_objective_text: 4
```

Reguli:

- QA narativ produce warnings initial;
- strict mode poate transforma warnings in errors pentru release;
- textele generate de AI trec prin aceleasi reguli;
- localizarea pastreaza placeholder-ele.

Criterii de acceptare:

- un dialog lipsa apare in raport;
- textul prea lung este detectat;
- placeholder-ul lipsa blocheaza strict mode;
- raportul poate fi inclus in CI.

### Q6.13 - Mod de mentenanta quest runtime

Adminul are nevoie de o stare controlata cand face migrari sau reparatii.

Moduri:

| Mod | Comportament |
|---|---|
| `normal` | questuri active normal |
| `read_only` | jucatorii pot vedea, nu progresa |
| `no_new_quests` | progresul continua, oferte noi blocate |
| `maintenance` | doar admin/debug |
| `disabled` | sistemul de questuri oprit |

Configurare:

```yml
quests:
  runtime_mode: "normal"
```

Reguli:

- `read_only` nu modifica progresul;
- `no_new_quests` permite finalizarea celor active;
- `maintenance` blocheaza jucatorii, dar permite repair;
- schimbarea modului este logata;
- UI-ul explica jucatorilor de ce questurile sunt indisponibile.

Criterii de acceptare:

- adminul poate opri ofertele noi fara sa strice questuri active;
- maintenance mode permite repair;
- jucatorul primeste mesaj clar;
- modul curent apare in audit/debug.

### Q6.14 - Standard operational pentru incidente

Q6 trebuie sa defineasca un format de incident.

Incident report:

```json
{
  "incident_id": "quest-2026-05-04-001",
  "severity": "major",
  "pack_id": "medieval",
  "pack_version": "1.6.0",
  "affected_quests": ["medieval:blacksmith:Q06"],
  "symptom": "Quest cannot complete after inspect_node.",
  "mitigation": "Disabled Q06 and ran anchor repair.",
  "status": "resolved"
}
```

Severitati:

| Severitate | Exemplu |
|---|---|
| `minor` | text gresit |
| `moderate` | quest secundar blocat |
| `major` | campanie principala blocata |
| `critical` | reward exploit sau corupere progres |

Reguli:

- incidentul are owner;
- masura temporara este separata de fix permanent;
- reward exploit cere analiza ledger;
- incidentul inchis are test de regresie sau checklist manual;
- postmortem-ul actualizeaza audit/lint daca se poate.

Criterii de acceptare:

- incident report poate fi generat din comanda admin;
- raportul include pack version si quest ids;
- incidentul poate lista jucatori afectati;
- fixul permanent este legat de test sau migrare.

### Q6.15 - Documentatie pentru autori si admini

La nivel Q6, documentatia trebuie impartita pe public tinta.

Set recomandat:

```text
docs/questuri/
- authoring-guide.md
- admin-runbook.md
- schema-reference.md
- migration-guide.md
- addon-api.md
- troubleshooting.md
- release-process.md
- narrative-style-guide.md
```

Roluri:

| Document | Public |
|---|---|
| `authoring-guide.md` | autori de questuri |
| `admin-runbook.md` | operatori server |
| `schema-reference.md` | dezvoltatori si autori avansati |
| `migration-guide.md` | release manager |
| `addon-api.md` | dezvoltatori addon |
| `troubleshooting.md` | suport |
| `narrative-style-guide.md` | autori texte |

Reguli:

- documentatia de schema este versionata;
- runbook-ul are comenzi concrete;
- troubleshooting-ul porneste de la simptome;
- ghidul de authoring contine exemple valide complete;
- fiecare release actualizeaza documentatia afectata.

Criterii de acceptare:

- un autor poate scrie un quest fara sa citeasca cod Java;
- un admin poate repara un quest blocat urmand runbook-ul;
- un developer addon are API contract si testkit;
- documentatia mentioneaza diferentele intre Q2-Q6.

## Roadmap Q6 recomandat

Ordinea recomandata pentru Q6:

1. scrie specificatia formala pentru schema curenta;
2. introdu `QuestRuleEngine` read-only si explicabil;
3. adauga `QuestEventLog` pentru replay partial;
4. creeaza testkit pentru addonuri;
5. adauga campurile `server_id`, `cluster_id` si `progress_scope`;
6. construieste exportul analytics NDJSON;
7. adauga A/B testing doar pentru campuri safe;
8. implementeaza canary rollout pentru pack-uri;
9. adauga limite de securitate pentru import si pack size;
10. documenteaza compatibilitatea legacy si migrarile;
11. formalizeaza lifecycle-ul questurilor;
12. adauga narrative QA in CI;
13. adauga runtime maintenance modes;
14. standardizeaza incident reports;
15. imparte documentatia pe autori, admini si addon developers.

## Definitia de gata pentru Q6

Q6 este gata cand:

- schema de quest este documentata si versionata;
- regulile sunt evaluate printr-un engine determinist si explicabil;
- questurile au event log suficient pentru replay/debug;
- addonurile pot rula contract tests;
- cross-server are campuri si reguli clare;
- analytics poate fi exportat fara impact pe runtime;
- A/B testing este limitat la schimbari safe;
- rollout-ul de pack poate fi canary si poate fi oprit;
- importul si pack-urile au limite de securitate;
- datele legacy pot fi inspectate si migrate dry-run;
- lifecycle-ul questurilor este explicit si auditat;
- QA narativ ruleaza in pipeline;
- maintenance mode permite operare fara corupere de progres;
- incident report-ul poate fi generat si urmarit;
- documentatia este impartita pe authoring, administrare, schema, migrari si addon API.

## Partea 7 - standard deschis, autonomie controlata si laborator de questuri

Partea 7 este nivelul cel mai avansat al sistemului. Aici AINPC trateaza questurile ca un ecosistem complet: continut versionat, reguli verificabile, recomandari explicabile, generare controlata, instrumente pentru autori, compatibilitate publica si laborator de replay pentru testarea scenariilor complexe.

Q7 nu este necesar pentru MVP si nu trebuie implementat inainte de Q2-Q6. Este directia pentru momentul in care questurile devin una dintre infrastructurile centrale ale serverului.

Directia Q7:

- questurile, campaniile, NPC-urile, locurile si story state-ul sunt legate intr-un graph semantic;
- regulile critice sunt verificate prin invarianti si teste de model;
- directorul de questuri poate propune continut autonom, dar doar prin guardrails;
- fairness-ul, accesibilitatea si anti-abuzul sunt tratate ca reguli de platforma;
- authoring-ul are vizualizare de dependinte, diff semantic si review asistat;
- replay lab-ul poate reproduce incidente si simula schimbari de pack;
- SDK-ul public permite addonuri stabile si compatibile;
- standardul de continut poate fi publicat pentru pack-uri externe.

### Q7.0 - Preconditii

Nu incepe Q7 daca:

- schema nu este formalizata si versionata;
- `QuestRuleEngine` nu exista;
- `QuestEventLog` nu poate face replay partial;
- addonurile nu au contract tests;
- analytics nu poate compara pack versions;
- canary rollout nu exista;
- lifecycle-ul questurilor nu este explicit;
- incident reports nu pot fi generate.

Regula: Q7 este despre incredere si autonomie controlata. Fara observabilitate si replay, autonomia nu este verificabila.

### Q7.1 - Quest Knowledge Graph

Pe masura ce continutul creste, listele de questuri nu mai sunt suficiente. Ai nevoie de un graph semantic care arata dependentele dintre questuri, campanii, NPC-uri, locuri, reputatie si story state.

Noduri:

| Nod | Exemplu |
|---|---|
| `Quest` | `medieval:blacksmith:Q06` |
| `Campaign` | `medieval:forge_crisis` |
| `NPC` | `npc:blacksmith:ion` |
| `Profession` | `blacksmith` |
| `Region` | `satul_central` |
| `Place` | `satul_central:fierarie` |
| `StoryState` | `place:fierarie:investigated` |
| `ReputationKey` | `region:village_trust` |
| `WorldEvent` | `harvest_festival` |

Muchii:

| Muchie | Sens |
|---|---|
| `requires` | questul cere alt quest sau conditie |
| `unlocks` | completarea deblocheaza continut |
| `uses_place` | questul foloseste un loc |
| `uses_npc` | questul foloseste un NPC sau rol |
| `sets_state` | questul modifica story state |
| `changes_reputation` | questul schimba reputatie |
| `belongs_to` | questul apartine unei campanii |
| `conflicts_with` | questuri incompatibile |

Export:

```json
{
  "nodes": [
    {"id": "medieval:blacksmith:Q06", "type": "Quest"},
    {"id": "satul_central:fierarie", "type": "Place"}
  ],
  "edges": [
    {"from": "medieval:blacksmith:Q06", "to": "satul_central:fierarie", "type": "uses_place"}
  ]
}
```

Reguli:

- graph-ul este derivat din definitii validate, nu scris manual;
- fiecare edge are sursa: pack, fisier, quest, camp;
- graph-ul poate fi exportat pentru debug si editor;
- dependentele lipsa sunt audit errors;
- dependentele optionale sunt marcate explicit.

Criterii de acceptare:

- adminul poate vedea ce questuri depind de Q06;
- editorul poate afisa ce locuri sunt folosite de o campanie;
- auditul detecteaza cicluri de prerequisite;
- retragerea unui quest arata continutul afectat.

### Q7.2 - Invarianti formali de quest

In Q7, anumite reguli trebuie sa fie invarianti: nu pot fi incalcate de pack-uri, addonuri sau generare procedurala.

Invarianti recomandati:

```text
Un quest COMPLETED nu poate reveni la ACTIVE fara repair explicit.
Un reward final nu se emite de doua ori pentru aceeasi idempotency_key.
Un objective nu progreseaza peste amount.
Un quest retired nu poate fi oferit.
Un branch ales nu se schimba fara repair explicit.
Un stage inactiv nu primeste progress.
Un quest invalid nu poate fi activat.
```

Model de test:

```text
initial state
-> event sequence
-> expected state
-> invariants hold
```

Exemplu fixture:

```json
{
  "name": "reward_is_idempotent_after_restart",
  "events": [
    {"type": "QUEST_ACCEPTED"},
    {"type": "OBJECTIVE_PROGRESS", "objective_id": "visit_forge", "value": 1},
    {"type": "QUEST_COMPLETED"},
    {"type": "SERVER_RESTART"},
    {"type": "QUEST_COMPLETED"}
  ],
  "assertions": [
    "reward_issued_once",
    "status_completed"
  ]
}
```

Reguli:

- invarianti ruleaza in CI;
- invarianti critici ruleaza si in audit strict;
- fiecare incident major trebuie sa produca invariant nou sau test de regresie;
- addonurile nu pot dezactiva invarianti core.

Criterii de acceptare:

- CI pica daca un reward se dubleaza;
- replay-ul poate verifica invarianti;
- un handler addon este testat impotriva invariantilor core;
- raportul de audit listeaza invariantii verificati.

### Q7.3 - Director autonom cu guardrails

`QuestDirector` poate deveni mai inteligent, dar autonomia trebuie limitata. El poate propune, programa si prioritiza. Nu poate modifica progres, reward sau story state fara runtime validat.

Actiuni permise:

| Actiune | Conditie |
|---|---|
| recomanda quest | toate conditiile trec |
| propune draft | draftul ramane in review |
| activeaza world event minor | daca policy permite |
| ajusteaza prioritate | in limite configurate |
| sugereaza repair | doar ca recomandare admin |

Actiuni interzise:

- completeaza quest;
- aplica reward;
- modifica DB direct;
- activeaza pack nevalidat;
- schimba branch ales;
- rescrie quest activ fara migrare.

Guardrails:

```yml
quest_director:
  autonomy:
    enabled: true
    max_actions_per_hour: 20
    allow_minor_world_events: false
    allow_auto_activate_drafts: false
    require_audit_clean: true
    max_reward_tier: "minor"
```

Reguli:

- fiecare actiune autonoma are event log;
- directorul poate fi pus in `recommend_only`;
- adminul poate cere explicatie pentru orice actiune;
- guardrails sunt evaluate de `QuestRuleEngine`;
- un incident poate dezactiva autonomia automat.

Criterii de acceptare:

- directorul poate recomanda, dar nu poate finaliza questuri;
- o actiune blocata are motiv clar;
- actiunile autonome apar in audit log;
- modul `recommend_only` opreste toate actiunile cu efect.

### Q7.4 - Fairness si accesibilitate de progres

Questurile avansate pot favoriza anumite stiluri de joc. Q7 introduce reguli de fairness ca sa nu blocheze continutul important pentru jucatori care nu joaca intr-un singur fel.

Dimensiuni:

| Dimensiune | Risc |
|---|---|
| combat | jucatorii non-combat raman blocati |
| social | jucatorii nu gasesc NPC-ul potrivit |
| exploration | locurile nu sunt descoperite |
| time window | jucatorii din alte fusuri orare pierd eventul |
| group quests | jucatorii solo sunt exclusi |
| economy | reward-urile creeaza gap mare |

Reguli:

- campaniile principale au cale alternativa sau difficulty fallback;
- questurile de grup nu blocheaza campania principala fara alternativa;
- eventurile temporare au catch-up sau rerun;
- obiectivele ascunse au hint suficient dupa blocaj lung;
- directorul detecteaza player stuck si recomanda ajutor.

Metrici:

```text
stuck_time_by_objective
completion_rate_by_playstyle
solo_blocked_quests
time_window_missed_count
group_quest_participation
```

Criterii de acceptare:

- un quest principal cu obiectiv combat are alternativa sau justificare explicita;
- analytics poate arata jucatori blocati peste prag;
- directorul recomanda hint dupa blocaj;
- eventurile sezoniere au politica de catch-up.

### Q7.5 - Continuitate narativa verificabila

Pe termen lung, story state-ul poate deveni inconsistent: un NPC vorbeste ca si cum forja e salvata, dar questul spune invers. Q7 cere reguli de continuitate.

Surse de adevar:

| Sursa | Rol |
|---|---|
| `player_quests` | progres individual |
| `player_campaigns` | progres narativ mare |
| `story_events` | istoric narativ |
| `region_story_state` | stare regionala curenta |
| `place_story_state` | stare locala |
| `reputation` | perceptie sociala |

Reguli:

- dialogul AI primeste snapshot consistent;
- story state contradictoriu este warning sau error;
- completarea questurilor principale poate seta state canonical;
- drafturile AI trebuie sa declare ce story state citesc si scriu;
- schimbarea retroactiva de lore cere migrare sau event nou.

Exemplu de conflict:

```text
Q06 completed -> forge_investigated=true
Q14 requires forge_investigated=false
campaign forge_crisis stage=after_investigation
```

Criterii de acceptare:

- auditul detecteaza conditii contradictorii intre campanie si quest;
- `story context` arata state canonical;
- dialogul nu primeste doua valori opuse pentru aceeasi stare;
- migrarile de lore sunt documentate.

### Q7.6 - Simulare controlata de economie si populatie

Questurile sistemice pot lega economia si populatia satului de gameplay, dar trebuie evitat feedback-ul instabil.

Exemple de legaturi:

| Sistem | Influenta asupra questurilor |
|---|---|
| resurse sat | delivery/crafting quests |
| pericol drum | guard/hunt quests |
| populatie | rescue/social quests |
| reputatie | acces la questuri |
| rutina NPC | disponibilitate giver |
| constructii | repair/build support quests |

Reguli:

- simularea produce semnale, nu questuri active direct;
- `QuestDirector` transforma semnalele in recomandari sau drafturi;
- fiecare semnal are cooldown;
- semnalele contradictorii sunt priorizate;
- questurile principale au prioritate peste generarea sistemica.

Semnal:

```json
{
  "signal_type": "settlement_resource_low",
  "settlement_id": "satul_central",
  "resource": "food",
  "severity": "moderate",
  "expires_at": 1760000000000
}
```

Criterii de acceptare:

- un semnal de resursa scazuta poate produce recomandare;
- cooldown-ul previne spam-ul de questuri similare;
- semnalele apar in debug;
- dezactivarea questurilor sistemice nu opreste simularea.

### Q7.7 - Diff semantic pentru quest packs

Un diff text pe YAML nu este suficient. Autorii si adminii trebuie sa vada ce s-a schimbat semantic.

Categorii de diff:

| Schimbare | Risc |
|---|---|
| text only | scazut |
| reward amount | mediu |
| objective amount | mediu |
| objective_id rename | ridicat |
| stage structure | ridicat |
| branch rules | ridicat |
| story state write | ridicat |
| prerequisite remove | mediu/ridicat |

Raport:

```text
Quest Pack Semantic Diff
- Q06: text changed
- Q08: reward changed ARROW 8 -> 16
- Q09: added story state write place:field:healed
- Q10: renamed objective inspect_crypt -> inspect_altar [migration required]
```

Reguli:

- diff-ul semantic ruleaza in CI;
- schimbari ridicate cer review explicit;
- rename de ID cere migration;
- story state write nou cere audit strict;
- reward increase mare cere balance check.

Criterii de acceptare:

- PR-ul de continut arata riscul schimbarilor;
- rename fara migrare este blocat;
- text-only diff nu cere smoke test complet;
- schimbari de branch cer test de flow.

### Q7.8 - Visual quest graph si author tooling

Pentru autori, graph-ul trebuie vizualizat.

Vizualizari utile:

- graph campanie;
- dependente prerequisite;
- flow de stages;
- branch outcomes;
- harta region/place/node folosita;
- reward economy pe pack;
- story state reads/writes;
- quest lifecycle status.

Functionalitati:

```text
open quest
-> vezi stages
-> vezi obiective
-> vezi conditii
-> vezi story effects
-> ruleaza audit pe quest
-> genereaza smoke checklist
```

Reguli:

- editorul vizual foloseste snapshot validat;
- modificarile se salveaza ca draft;
- activarea trece prin audit;
- graph-ul nu este sursa de adevar, YAML/modelul validat este.

Criterii de acceptare:

- autorul poate vedea campania fara sa citeasca tot YAML-ul;
- nodurile invalide sunt marcate;
- editorul poate genera checklist de test;
- modificarile vizuale produc diff semantic.

### Q7.9 - Replay lab

Replay lab este mediul unde rulezi evenimente istorice si simulari fara server live.

Capabilitati:

```text
load pack version
load player snapshot
load event log
run replay
compare expected state
run migration dry-run
export divergence report
```

Utilizari:

- investigare incident;
- testare migration;
- verificare reward idempotency;
- comparare doua versiuni de pack;
- reproducere bug din production;
- testare QuestDirector pe date istorice.

Reguli:

- replay lab nu scrie in DB live;
- datele sensibile sunt reduse;
- rezultatele pot fi atasate la incident;
- replay-ul foloseste acelasi `QuestRuleEngine` ca runtime-ul.

Criterii de acceptare:

- un incident poate fi reprodus din export;
- migration dry-run poate fi comparat cu starea asteptata;
- divergentele sunt raportate pe quest/objective;
- replay lab ruleaza in CI pentru fixtures critice.

### Q7.10 - SDK public si exemple de referinta

Daca API-ul devine public, trebuie SDK si exemple.

Structura:

```text
ainpc-quest-sdk/
- api/
- testkit/
- examples/
  - custom-objective/
  - custom-reward/
  - custom-condition/
  - quest-pack-provider/
- docs/
```

Exemple obligatorii:

- objective simplu `interact_block`;
- reward simplu `xp`;
- condition `min_reputation`;
- pack provider care incarca questuri;
- test de contract pentru fiecare.

Reguli:

- exemplele compileaza in CI;
- SDK-ul nu expune clase interne mutable;
- API-ul are semantic versioning;
- breaking changes au migration guide.

Criterii de acceptare:

- un developer poate crea handler nou urmand exemplul;
- testkit-ul valideaza handlerul;
- docs arata lifecycle de addon;
- exemplele sunt actualizate la fiecare major version.

### Q7.11 - Politica de compatibilitate publica

Un standard public are nevoie de reguli clare.

Clase de compatibilitate:

| Suprafata | Politica |
|---|---|
| quest schema | backward compat pe major version |
| addon API | semantic versioning |
| event payload | versionat si migrabil |
| analytics export | campuri noi additive |
| commands | alias-uri deprecated inainte de remove |
| config | migrari automate sau warning |

Reguli:

- deprecarea are data sau versiune tinta;
- documentatia listeaza alternative;
- auditul avertizeaza pentru folosirea vechiului contract;
- removals apar doar in major version;
- compat layers au teste.

Criterii de acceptare:

- un pack vechi are mesaj clar de compatibilitate;
- addonul vede versiunea API;
- event payload vechi poate fi citit;
- release notes includ deprecari si removals.

### Q7.12 - Sunset si arhivare de continut

Continutul vechi trebuie retras fara sa rupa istoricul.

Stari finale:

| Stare | Sens |
|---|---|
| `deprecated` | inca ruleaza, dar nu se mai recomanda |
| `retired` | nu mai este oferit |
| `archived` | pastrat doar pentru istoric |
| `blocked` | oprit din motiv de siguranta |

Reguli:

- questurile archived pot fi citite pentru istoric;
- prerequisites catre questuri archived raman rezolvabile;
- campaniile vechi pot fi marcate legacy;
- reward ledger ramane disponibil;
- textele pot fi arhivate cu pack-ul.

Comenzi:

```text
/ainpc quest retire <quest_id>
/ainpc quest archive <quest_id>
/ainpc quest history <player> <quest_id>
```

Criterii de acceptare:

- un quest archived nu este ofertabil;
- istoricul jucatorului ramane vizibil;
- campania veche nu blocheaza campanii noi;
- auditul detecteaza prerequisite catre continut blocat.

### Q7.13 - Certificare de calitate pentru pack

Q7 poate introduce un scor de certificare pentru pack-uri.

Niveluri:

| Nivel | Cerinte |
|---|---|
| `bronze` | schema valida, audit fara errors |
| `silver` | smoke tests si localization checks |
| `gold` | replay fixtures, analytics tags, migration tests |
| `platinum` | invariants, semantic diff, incident runbook |

Scor:

```text
pack_certification:
  level: "gold"
  audit_errors: 0
  audit_warnings: 3
  smoke_tests: 12
  replay_fixtures: 5
  localization_coverage: 0.95
```

Reguli:

- certificarea nu inlocuieste review-ul;
- nivelurile sunt recalculabile;
- pack-urile externe pot cere nivel minim;
- serverul poate refuza pack-uri sub nivel configurat.

Criterii de acceptare:

- CI produce scor de certificare;
- adminul poate seta nivel minim;
- certificarea arata motivele pentru nivel;
- pack-ul scade nivelul daca apar warnings critice.

### Q7.14 - Standard de productie pentru AINPC Quest Platform

La final, sistemul poate avea un standard intern de productie.

Standard minim:

```text
AINPC Quest Platform Standard
- schema versioned
- audit strict
- rule engine deterministic
- event log replayable
- reward ledger idempotent
- pack lifecycle explicit
- semantic diff required
- CI content gates
- incident process
- addon contract tests
- documentation versioned
```

Reguli:

- niciun pack nu intra pe live fara audit;
- niciun reward nou fara handler si test;
- niciun objective nou fara handler, validate si no-match behavior;
- niciun rename de ID fara migration;
- niciun incident major fara postmortem si regresie;
- niciun addon public fara testkit.

Criterii de acceptare:

- release manager poate verifica standardul intr-un raport;
- toate exceptiile sunt explicite si logate;
- pack-urile live au status si versiune;
- documentatia reflecta standardul curent.

## Roadmap Q7 recomandat

Ordinea recomandata pentru Q7:

1. genereaza `Quest Knowledge Graph` din pack-urile validate;
2. adauga verificari pentru cicluri si dependente lipsa;
3. defineste invariantii core si ruleaza-i in CI;
4. extinde `QuestDirector` cu guardrails de autonomie;
5. adauga metrici de fairness si stuck detection;
6. adauga verificari de continuitate narativa;
7. conecteaza semnale de simulare la recomandari, nu direct la progres;
8. implementeaza semantic diff pentru quest packs;
9. construieste un visual graph minim pentru autori;
10. creeaza replay lab pentru incidente si migrari;
11. publica SDK/testkit cu exemple;
12. documenteaza politica publica de compatibilitate;
13. implementeaza sunset/archive pentru continut vechi;
14. adauga certificare de pack;
15. publica standardul intern de productie.

## Definitia de gata pentru Q7

Q7 este gata cand:

- questurile si campaniile pot fi vizualizate ca graph semantic;
- auditul detecteaza cicluri, dependente lipsa si conflicte de story;
- invariantii core ruleaza in CI si replay;
- directorul autonom este limitat de guardrails si poate fi explicat;
- fairness-ul si blocajele sunt masurate;
- continuitatea narativa are verificari automate;
- semnalele de simulare produc recomandari controlate;
- semantic diff clasifica riscul schimbarilor de pack;
- autorii au visual graph sau export consumabil de editor;
- replay lab poate reproduce incidente din event log;
- SDK-ul public are testkit si exemple compilabile;
- compatibilitatea publica are reguli de deprecari;
- continutul vechi poate fi retired, archived sau blocked fara pierdere de istoric;
- pack-urile pot primi nivel de certificare;
- exista standard de productie pentru AINPC Quest Platform.

## Checklist pentru un quest nou

Inainte sa adaugi un quest in pack:

- are `base_type: "QUEST"`;
- are `requires_player: true`;
- are `quest.code` unic;
- are `quest.giver_profession` existenta in pack;
- are `kind`, `acceptance_mode`, `completion_mode`, `tracking_mode`;
- are `tags` utile pentru audit si viitor filtering;
- are cel putin un obiectiv suportat de runtime;
- obiectivele au chei YAML stabile;
- materialele si entitatile sunt valide Minecraft;
- referintele de mapping folosesc `tag:`, `type:`, `region:`, `place:` sau `node:`;
- questurile cu `repeatable: true` au `cooldown_seconds`;
- prerequisite-urile indica questuri existente;
- reward-urile sunt de tip suportat;
- dialogurile acopera macar `offer`, `accepted`, `active`, `ready`, `completed`;
- `/ainpc audit quest` nu raporteaza erori;
- exista o comanda de smoke test sau pasi manuali clari.

## Exemple de questuri noi pentru varietate

### Q06 - Explorare mapping

- giver: `blacksmith`;
- obiective: `visit_place tag:blacksmith`, `inspect_node type:inspect_node`;
- reward: `record_story_event` plus item mic;
- scop: valideaza mapping-ul pentru fierarie.

### Q07 - Livrare sociala

- giver: `innkeeper`;
- obiective: `collect_item PAPER`, `talk_to_npc profession:guard`, `deliver_to_npc BREAD`;
- reward: `record_story_event` plus `EMERALD`;
- scop: quest fara lupta, cu NPC tinta.

### Q08 - Vanatoare contextualizata

- giver: `guard`;
- obiective: `visit_region type:settlement`, `kill_mob ZOMBIE`, `talk_to_npc profession:guard`;
- reward: `record_story_event` plus `ARROW`;
- scop: combina mapping regional, combat si raportare la quest giver.

### Q09 - Investigatie

- giver: `healer`;
- obiective: `talk_to_npc farmer`, `visit_place tag:farm`, `inspect_node type:crop`;
- reward: `set_story_state` pe `place`;
- scop: combina social, mapping si story.

### Q10 - Alegere morala

- giver: `priest`;
- etapa 1: inspecteaza cripta;
- etapa 2: raporteaza preotului sau negustorului;
- reward diferit dupa branch;
- scop: se implementeaza doar dupa stages si branching.

## Ce sa eviti

- questuri hardcodate direct in comenzi;
- tipuri noi in YAML fara listener si audit;
- obiective dependente de nume fragile de NPC cand exista profesii sau roluri;
- coordonate brute in questuri;
- reward-uri care executa cod arbitrar;
- branching implementat doar prin text AI;
- schimbarea ID-urilor de `region`, `place` si `node` pe server live fara migration;
- introducerea stages in YAML inainte ca parserul si runtime-ul sa le ignore/valideze explicit.

## Concluzie

Pentru questuri mai multe si mai variate, pasul urmator nu este sa adaugi zeci de fetch questuri. Pasul corect este:

1. adauga questuri noi care folosesc tipurile deja suportate: `visit_place`, `inspect_node`, `talk_to_npc`, `kill_mob`;
2. stabilizeaza `objective_id` explicit;
3. adauga cateva tipuri noi usoare: `craft_item`, `interact_block`, `talk_to_role`;
4. permite mai multe questuri active;
5. introdu `quest.stages`;
6. abia apoi adauga branching si reward-uri avansate.

Asa sistemul ramane testabil, compatibil cu pack-urile existente si suficient de flexibil pentru scenarii mai mari.
