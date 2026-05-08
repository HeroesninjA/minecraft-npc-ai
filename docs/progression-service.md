# ProgressionService

Actualizat: 2026-05-07

## Scop

`ProgressionService` este directia de refactorizare pentru partea de quest runtime. Scopul este ca sistemul actual de questuri sa devina un motor generic de progres pentru mai multe mecanici de gameplay, nu doar pentru scenarii numite explicit "quest".

Un addon trebuie sa poata folosi acelasi runtime pentru:

- questuri principale;
- questuri secundare;
- contracte de livrare;
- datorii de garda;
- bounty-uri si hunt contracts;
- taskuri zilnice;
- evenimente locale;
- ritualuri;
- investigatii;
- tutoriale;
- orice alta mecanica bazata pe obiective, stari si recompense.

Regula centrala: "quest" trebuie sa devina o fatada peste `ProgressionService`, nu numele intern obligatoriu al sistemului.

## Problema curenta

In codul actual, runtime-ul matur exista in mare parte in `ScenarioEngine` si este orientat semantic spre questuri:

- progresul este salvat in `player_quests`;
- comenzile principale sunt `/ainpc quest ...`;
- obiectivele, stages, tracking-ul si rewards sunt incarcate din definitii de quest;
- `QuestScenarioContract` descrie categoria, tipul si modul de acceptare/completare.

Acest lucru functioneaza pentru questuri, dar limiteaza addonurile care vor mecanici similare cu alte nume. De exemplu, un addon medieval poate avea simultan `contracte`, `datorii`, `evenimente de sat` si `questuri principale`, toate bazate pe obiective si progres persistent.

## Directie

Intern, progresul trebuie modelat generic:

```text
Progression
-> definition id
-> mechanic id
-> kind
-> display labels
-> status
-> stages
-> objectives
-> rewards/actions
-> variables
-> anchors
-> tracking
-> player progress
```

`Quest` ramane doar o fatada compatibila:

```text
/ainpc quest log
/ainpc quest status
/ainpc quest progress
/ainpc quest track
```

In viitor, acelasi runtime poate fi expus prin comenzi sau GUI-uri diferite:

```text
/ainpc contract log
/ainpc duty progress
/ainpc event status
/ainpc progression debug
```

Implementare initiala compatibila:

```text
/ainpc progression log
/ainpc progression stored [jucator|uuid|all] [filter] [limit]
/ainpc progression status <selector>
/ainpc progression progress <selector>
/ainpc progression track start <selector>
/ainpc progression abandon <selector>

/ainpc contract log
/ainpc contract log active
/ainpc contract log completed
/ainpc contract stored [jucator|uuid|all] [filter] [limit]
/ainpc contract status C01
/ainpc contract progress C01
/ainpc contract track start C01
/ainpc contract abandon C01

/progression log
/contract log
```

`/ainpc contract ...` este o fatada scurta peste runtime-ul comun: pentru log aplica filtrul `contract`, iar pentru selectori simpli prefixeaza mecanica `contract:` unde este sigur sa o faca.

## Model YAML recomandat

Un addon ar trebui sa poata declara mai multe mecanici de progres:

```yaml
mechanics:
  main_quests:
    kind: quest
    label: "Questuri principale"
    singular_label: "quest"
    plural_label: "questuri"
    progress: true
    max_active: 1

  delivery_contracts:
    kind: contract
    label: "Contracte de livrare"
    singular_label: "contract"
    plural_label: "contracte"
    progress: true
    max_active: 3

  guard_duties:
    kind: duty
    label: "Datorii de garda"
    singular_label: "datorie"
    plural_label: "datorii"
    progress: true
    max_active: 1

  village_events:
    kind: event
    label: "Evenimente locale"
    singular_label: "eveniment"
    plural_label: "evenimente"
    progress: true
    max_active: 2
```

Scenariile din acelasi addon pot folosi mecanici diferite:

```yaml
scenarios:
  - id: bring_supplies
    base_type: trade_deal
    mechanic: delivery_contracts
    name: "Hartie pentru negustor"
    objectives:
      - id: collect_paper
        type: collect_item
        item: PAPER
        amount: 6
    rewards:
      - type: item
        item: EMERALD
        amount: 2

  - id: night_watch
    base_type: emergency
    mechanic: guard_duties
    name: "Straja de noapte"
    objectives:
      - id: visit_gate
        type: visit_place
        target: tag:gate

  - id: missing_child
    base_type: conflict
    mechanic: village_events
    name: "Copil disparut"
    objectives:
      - id: talk_to_parent
        type: talk_to_npc
        target: role:PARENT
```

## Scenarii fara progres

Nu toate scenariile trebuie sa creeze progres pentru jucator.

Un scenariu fara `mechanic`, fara `progress: true` sau fara obiective active trebuie sa ramana scenariu narativ/sistemic:

- NPC-urile reactioneaza;
- story state-ul poate evolua prin servicii dedicate;
- dialogul poate primi context;
- simularile pot produce semnale;
- nu apare in quest log;
- nu scrie in progresul jucatorului.

Aceasta regula previne transformarea fiecarui eveniment de lume intr-un quest ascuns.

## Identificare si namespace

Fiecare progres activ trebuie sa aiba o cheie stabila care include addonul si mecanica:

```text
<pack_id>:<mechanic_id>:<scenario_id>
```

Exemple:

```text
medieval:main_quests:blacksmith_q01
medieval:delivery_contracts:bring_supplies
medieval:guard_duties:night_watch
medieval:village_events:missing_child
```

`quest_code` poate ramane compatibil pentru questurile existente, dar pentru mecanici noi este mai corect un `progression_code` sau `definition_code`.

Selectorii acceptati initial de runtime-ul compatibil:

```text
<quest_code>
<template_id>
<definition_id>
<mechanic_id>:<quest_code>
<mechanic_id>:<definition_id>
<kind>:<quest_code>
<pack_id>:<mechanic_id>:<quest_code>
<pack_id>:<mechanic_id>:<definition_id>
```

Exemple pentru addonul medieval:

```text
Q01
main_quests:Q01
side_quests:Q07
contract:C01
village_contracts:C01
medieval_quest:village_contracts:C01
```

## Contract minim pentru ProgressionService

Serviciul ar trebui sa detina operatiile deterministe:

```java
ProgressionOfferResult offer(Player player, ProgressionDefinition definition, ProgressionActor actor);
ProgressionAcceptResult accept(Player player, ProgressionSelector selector);
ProgressionStatusSnapshot status(Player player, ProgressionSelector selector);
ProgressionProgressSnapshot progress(Player player, ProgressionSelector selector);
ProgressionTrackResult track(Player player, ProgressionSelector selector);
ProgressionAbandonResult abandon(Player player, ProgressionSelector selector);
ProgressionCompletionResult complete(Player player, ProgressionSelector selector, ProgressionActor actor);
ObjectiveProgressResult recordObjectiveEvent(Player player, ObjectiveEvent event);
```

`ScenarioEngine`, listener-ele, comenzile si AI-ul nu trebuie sa scrie direct progresul. Ele trebuie sa routeze intentii sau evenimente catre `ProgressionService`.

Implementare initiala in repo:

- `ProgressionService` exista in `ro.ainpc.progression` si este initializat de `AINPCPlugin`;
- serviciul preia initial log/status/progress/debug/track/abandon si snapshot-ul GUI, dar deleaga inca executia reala catre `ScenarioEngine`;
- `ProgressionSelector` normalizeaza selectori goi, `tracked/current/curent/urmarit`, selectori simpli, `mechanic:definition` si `pack:mechanic:definition`;
- `ProgressionDefinition` modeleaza read-only definitiile jucabile din feature packs;
- `ProgressionStatusSnapshot`, `ProgressionProgressSnapshot`, `ProgressionGuiSnapshot`, `ProgressionGuiEntry` si `ProgressionStageSnapshot` exista initial ca snapshot-uri compatibile peste datele produse inca de `ScenarioEngine`;
- `ProgressionRepository` citeste read-only tabela compatibila `player_quests` si intoarce `StoredProgression`, cu metadata de definitie rezolvata din `ProgressionDefinition`;
- `ProgressionService.getStoredProgressions()` expune repository-ul generic pentru audit/debugdump si pentru viitoarea migrare;
- `StoredProgressionSummary` agrega read-only randurile persistate dupa jucator, status, pack, template, mecanica si kind;
- `/ainpc progression definitions [filter]` listeaza definitiile generice de progres;
- `/ainpc contract definitions [filter]` listeaza aceeasi sursa, filtrata implicit pe contracte cand nu primeste filtru;
- `/ainpc progression stored [jucator|uuid|all] [filter] [limit]` listeaza progresul persistent citit prin `ProgressionRepository`;
- `/ainpc contract stored ...` foloseste acelasi view, filtrat implicit pe contracte;
- `/ainpc audit quest` foloseste `StoredProgressionSummary` pentru sumar si avertizari despre progresii persistate fara definitie/mecanica/status;
- `/ainpc debugdump quest` include `progression_definitions` in `loaded-quest-definitions.json`;
- `/ainpc contract ...` foloseste parserul pentru a transforma selectorii scurti in forma `contract:<selector>`.

## Persistenta

Pe termen scurt, tabela `player_quests` poate ramane pentru compatibilitate.

Pe termen mediu, directia mai curata este o tabela generica:

```text
player_progressions
```

Campuri recomandate:

```text
player_uuid
progression_id
pack_id
mechanic_id
definition_id
kind
display_name
status
current_stage_id
objective_progress
variables
tracked
started_at
completed_at
updated_at
```

Pentru compatibilitate:

- `player_quests` poate fi citit ca view logic peste `player_progressions` unde `kind = quest`;
- comenzile `/ainpc quest ...` pot filtra doar mecanicile `kind=quest`;
- debugdump-ul vechi ramane prin `player-quest-progress.json`;
- implementare initiala: `ProgressionRepository` citeste `player_quests` ca `StoredProgression`, `StoredProgressionSummary` agrega randurile, iar `/ainpc debugdump quest` scrie `player-progressions.json` ca view generic cu `progression_id`, `pack_id`, `definition_id`, `mechanic_id`, `kind` si label-uri rezolvate din feature packs.

## Obiective si stari

Starea obiectivului este derivata din progresul numeric, statusul progresiei si stage-ul curent:

| Stare | Sens |
|---|---|
| `pending` | obiectivul nu este inca activ sau progresia nu a pornit |
| `started` | obiectivul este activ, dar nu are progres numeric |
| `in_progress` | obiectivul este activ si are progres partial |
| `completed` | obiectivul a atins valoarea ceruta sau progresia este completata |
| `failed` | progresia a esuat sau a fost abandonata |

Aceeasi logica trebuie folosita pentru questuri, contracte, datorii si evenimente.

## Mai multe mecanici in acelasi addon

Un addon poate declara simultan mai multe mecanici bazate pe progres. Regulile recomandate:

- fiecare mecanica are `id` propriu;
- fiecare mecanica are `kind` si label-uri proprii pentru UI;
- fiecare mecanica are limite proprii, de exemplu `max_active`;
- progresul se salveaza separat pe `mechanic_id`;
- selectorii trebuie sa accepte fie codul definitiei, fie cheia completa `<pack>:<mechanic>:<definition>`;
- GUI-ul trebuie sa grupeze progresiile dupa mecanica, nu doar dupa status.

Exemplu practic intr-un addon medieval:

| Mechanic | Rol | Limita |
|---|---|---:|
| `main_quests` | poveste principala | 1 activ |
| `side_quests` | questuri secundare | 3 active |
| `delivery_contracts` | contracte de negustor | 3 active |
| `guard_duties` | sarcini de garda | 1 activ |
| `village_events` | evenimente locale temporare | 2 active |
| `daily_tasks` | treburi repetabile | 5 active |

Implementare initiala in repo:

- `FeaturePackLoader` incarca `mechanics` din feature packs si completeaza scenariile cu `kind`, label-uri si `max_active` din mecanica referita;
- `ScenarioEngine` include in runtime si scenarii non-`QUEST`, daca au progres activ si briefing/obiective/rewards;
- availability-ul verifica acum si limita `max_active` a mecanicii, pe langa limitele legacy `quest.max_active`;
- addonul medieval foloseste simultan `main_quests`, `side_quests` si `village_contracts`;
- `C01` este primul exemplu concret de progres non-`QUEST`: un scenariu `TRADE_DEAL` expus ca `contract` prin `village_contracts`.
- log/status/progress afiseaza metadata mecanicii, iar `quest log` poate filtra initial `quest` si `contract`.
- selectorii pentru status/progress/track/abandon accepta forme cu mecanica, de exemplu `village_contracts:C01`.
- exista fatade initiale `/ainpc progression ...`, `/progression ...`, `/ainpc contract ...` si `/contract ...`, toate rutate catre acelasi runtime.
- comenzile admin `/ainpc progression stored ...` si `/ainpc contract stored ...` inspecteaza progresiile persistate fara sa modifice DB-ul.
- `/ainpc debugdump quest` exporta initial si `player-progressions.json`, pentru inspectie generica fara dependenta semantica de numele `quest`.

## Migrare incrementala recomandata

1. Pastreaza comenzile `/ainpc quest ...` si DB-ul curent.
2. Adauga metadata de progres in loader: `mechanic`, `progress.enabled`, `progress.kind`, `progress.labels`. Implementat initial: `FeaturePackLoader` incarca `mechanics`, scenariile pot seta `mechanic` si `progress/progression`, iar metadata lipsa se completeaza din mecanica referita.
3. Permite progres si pentru scenarii non-`QUEST`, daca au mecanica de progres activa. Implementat initial: runtime-ul actual include definitii non-`QUEST` cu progres activ si obiective/rewards, inclusiv contractul medieval `C01`.
4. Introdu un model intern `ProgressionDefinition` mapat din quest/scenario definitions existente.
   Implementat initial: `ProgressionDefinition` exista read-only peste feature packs si este exportat in debugdump.
5. Extrage treptat metodele de progres din `ScenarioEngine` in `ProgressionService`. Implementat initial: exista `ProgressionService` ca strat de rutare/delegare pentru comenzile si GUI-urile bazate pe selector.
6. Adauga snapshot-uri generic numite: `ProgressionStatusSnapshot`, `ProgressionProgressSnapshot`, `ProgressionGuiSnapshot`, `ProgressionGuiEntry`.
   Implementat initial: `ProgressionStatusSnapshot`, `ProgressionProgressSnapshot`, `ProgressionGuiSnapshot`, `ProgressionGuiEntry` si `ProgressionStageSnapshot` exista ca wrapper-e compatibile peste status/progress/GUI, iar Quest GUI consuma deja snapshot-ul generic expus de `ProgressionService`.
7. Adauga exporturi generic numite pentru observabilitate. Implementat initial: `ProgressionRepository`, `StoredProgressionSummary` si `player-progressions.json` peste `player_quests`.
8. Abia apoi planifica migrari DB din `player_quests` catre `player_progressions`.

## Ce nu trebuie facut

- Nu duplica runtime separat pentru contracte, duty si events.
- Nu forta toate scenariile sa apara in quest log.
- Nu lasa AI-ul sa seteze direct status, obiective sau rewards.
- Nu lega progresul de numele vizibil "quest".
- Nu face migrare DB mare inainte ca modelul generic sa fie stabil in cod.

## Legaturi cu documentele existente

- `questuri-avansate.md` ramane ghidul pentru evolutia questurilor concrete.
- `lucru-alternat-quest-mapping-progression.md` stabileste cadenta de lucru: mapping concret, quest/contract mic, smoke test, observabilitate si apoi extractie mica spre `ProgressionService`.
- `quest-anchor-bindings.md` ramane contractul pentru ancorele semantice existente.
- `strategie-plugin-modular-si-scenarii-programabile.md` descrie directia pentru addonuri si scenarii.
- `ai-orchestrare-si-mecanici.md` trebuie sa trateze `ProgressionService` ca serviciu determinist, nu ca tool AI direct.
- `gui-interfete.md` trebuie sa evolueze Quest GUI spre un Progression GUI capabil sa grupeze mecanici.
