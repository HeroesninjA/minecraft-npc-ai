# Generare Automata a Questurilor cu AI

Actualizat: 2026-05-11

Status: document canonic initial pentru generarea asistata de questuri cu AI. In cod exista `AIOrchestrationService` ca fundatie initiala, dar generarea automata de questuri, `QuestDraft`, comenzile admin dedicate, persistenta drafturilor si validatorul complet nu sunt implementate inca.

## Scop

Acest document defineste cum AI-ul poate ajuta la crearea de questuri, contracte, bounty-uri, tutoriale si evenimente locale fara sa preia controlul runtime-ului.

Regula centrala:

```text
AI-ul propune drafturi.
Runtime-ul valideaza, adminul aproba, apoi pack-ul versionat este incarcat.
```

Generarea automata nu inseamna ca modelul poate porni questuri live, acorda reward-uri sau modifica story state. El produce un `QuestDraft` inspectabil, care trece prin validatoare si audit inainte sa ajunga in YAML sau intr-un pack de scenariu.

## Probleme rezolvate

Sistemul are deja runtime pentru questuri si progresii, dar authoring-ul manual devine greu pe masura ce apar:

- questuri principale si secundare;
- contracte locale;
- datorii de rol;
- bounty-uri;
- tutoriale;
- ritualuri;
- evenimente de sat;
- obiective mapate pe `WorldRegion`, `WorldPlace` si `WorldNode`;
- story actions si story events;
- reguli de cooldown, repeatability si categorii.

AI-ul poate accelera authoring-ul prin:

- generarea unui prim draft coerent;
- propunerea de variante pentru acelasi seed;
- completarea dialogurilor si descrierilor;
- alegerea obiectivelor compatibile cu mapping-ul existent;
- explicarea motivului pentru care un draft este invalid;
- transformarea unui brief natural intr-un format verificabil.

## Principii

### 1. Draft, nu executie

AI-ul nu produce efecte live. Output-ul sau ramane una dintre formele:

- `QuestDraft`;
- `QuestRevisionSuggestion`;
- `QuestValidationExplanation`;
- `QuestDialogueDraft`;
- `QuestChainOutline`.

Niciuna nu este executabila direct.

### 2. Schema stricta

Raspunsul AI trebuie sa fie structurat, nu text liber care este interpretat ad-hoc.

Minimul acceptat:

- `draft_id`;
- `title`;
- `mechanic`;
- `base_type`;
- `category`;
- `scenario_kind`;
- `giver_role`;
- `objectives`;
- `rewards`;
- `story_actions`;
- `required_anchors`;
- `validation_notes`.

Textul narativ este payload, nu contract.

### 3. AI foloseste context validat

AI-ul primeste doar snapshot-uri compacte:

- mecanici disponibile;
- obiective suportate;
- reward-uri suportate;
- story actions suportate;
- semantic index din mapping;
- feature pack curent;
- questuri existente, rezumate;
- restrictii de dificultate si durata.

Nu primeste:

- coordonate brute inutile;
- DB complet;
- chei API;
- istoric complet de chat;
- continut privat fara rezumat;
- permisiuni de scriere.

### 4. Validatorul are ultimul cuvant

Un draft este util doar daca poate fi explicat si validat.

Validatorul trebuie sa verifice:

- schema;
- unicitatea ID-urilor;
- obiectivele suportate;
- ancorele semantice;
- reward-urile;
- story actions;
- cooldown/repeat rules;
- compatibilitatea cu `ProgressionService`;
- conflictul cu questuri existente;
- balansul minim;
- restrictiile de siguranta.

### 5. Admin review obligatoriu

Pentru primul MVP, orice draft generat de AI ramane in review.

Adminul decide:

- `approve`;
- `revise`;
- `discard`;
- `export`;
- `run audit`;
- `run smoke checklist`.

## Relatia cu documentele existente

- `ai-orchestrare-si-mecanici.md` defineste orchestratorul AI transversal.
- `questuri-avansate-v2.md` defineste diversitatea mecanicilor si criteriile pentru questuri noi.
- `progression-service.md` defineste directia runtime-ului generic de progres.
- `quest-anchor-bindings.md` defineste legarea obiectivelor de mapping concret.
- `story-context-service.md` defineste contextul narativ read-only.
- `story-si-context-ai.md` defineste cum AI-ul consuma mapping, story si quest context.
- `mapping.md` defineste semantic index-ul folosit de audit si resolver.
- `generare-ai-si-constructie-automata.md` ramane documentul pentru AI in constructii si planuri de lume.

## Flux recomandat

```text
Admin brief sau sistem seed
-> QuestSeed
-> QuestAuthoringContext
-> AIOrchestrationService
-> QuestDraft raw
-> QuestDraftNormalizer
-> QuestDraftValidator
-> QuestDraftReview
-> export YAML draft
-> /ainpc audit quest
-> smoke test Paper
-> activare in feature pack
```

In niciun punct AI-ul nu scrie direct in `player_quests`, `quest_anchor_bindings`, story state sau config live.

## Input: QuestSeed

`QuestSeed` este brief-ul controlat pe care il primeste sistemul.

```yaml
seed_id: "admin:demo:q09"
region_selector: "demo_sat"
theme: "medieval"
goal: "quest de investigatie in piata satului"
mechanic: "main_quests"
base_type: "QUEST"
category: "main"
scenario_kind: "investigation"
target_duration_minutes: 8
difficulty: "easy"
required_objective_families:
  - visit_place
  - inspect_node
  - talk_to_npc
preferred_anchors:
  - "tag:market"
  - "quest_board"
forbidden:
  - "kill_mob"
  - "economy_currency_reward"
notes:
  - "Trebuie sa foloseasca mapping-ul demo."
  - "Nu introduce branching complex."
```

Reguli:

- seed-ul poate fi scris de admin, addon sau un planner determinist;
- seed-ul nu contine coordonate ca sursa principala de adevar;
- seed-ul declara limite, nu doar inspiratie narativa;
- daca seed-ul cere lucruri nesuportate, AI-ul trebuie sa raspunda cu draft invalidabil, nu sa inventeze runtime.

## Context: QuestAuthoringContext

Contextul trimis la AI trebuie sa fie scurt si auditat.

```yaml
available_mechanics:
  - main_quests
  - side_quests
  - village_contracts
  - npc_duties
  - local_bounties
  - village_events
  - onboarding
  - village_rituals
supported_objectives:
  - talk_to_npc
  - visit_region
  - visit_place
  - inspect_node
  - collect_item
  - deliver_to_npc
  - kill_mob
supported_story_actions:
  - record_story_event
  - set_story_state
supported_reward_types:
  - item
semantic_index_excerpt:
  places:
    - id: "demo_sat:market"
      type: "market"
      tags: ["market", "social", "public"]
    - id: "demo_sat:forge"
      type: "forge"
      tags: ["blacksmith", "workplace"]
  nodes:
    - id: "demo_sat:market:quest_board"
      type: "quest_board"
      tags: ["quest_trigger"]
existing_progressions:
  - code: "Q06"
    kind: "investigation"
    summary: "inspectie in sat si raportare"
constraints:
  max_objectives: 4
  max_stages: 3
  require_stable_objective_id: true
  allow_branching: false
```

Contextul este construit de runtime, nu de model.

## Output: QuestDraft

`QuestDraft` este formatul intern recomandat. El poate fi mai strict decat YAML-ul final.

```yaml
draft_id: "draft:q09:market_notice"
status: "draft"
source:
  type: "ai"
  seed_id: "admin:demo:q09"
identity:
  code: "Q09"
  template_id: "medieval:q09_market_notice"
  title: "Umbra de pe Avizier"
  mechanic: "main_quests"
  base_type: "QUEST"
  category: "main"
  scenario_kind: "investigation"
  repeatable: false
  cooldown_seconds: 0
availability:
  giver_role: "merchant"
  required_region: "demo_sat"
  prerequisites: []
briefing:
  short: "Negustorul cere verificarea unui anunt suspect din piata."
  player_goal: "Mergi in piata, inspecteaza avizierul si raporteaza."
stages:
  - stage_id: "INVESTIGATE"
    completion: "all_objectives"
    objectives:
      - objective_id: "visit_market"
        type: "visit_place"
        anchor: "tag:market"
        description: "Mergi in piata satului."
      - objective_id: "inspect_board"
        type: "inspect_node"
        anchor: "quest_board"
        description: "Inspecteaza avizierul."
    next_stage_id: "RETURN"
  - stage_id: "RETURN"
    completion: "manual_turn_in"
    objectives:
      - objective_id: "return_to_merchant"
        type: "talk_to_npc"
        target_role: "merchant"
        description: "Raporteaza ce ai gasit."
rewards:
  - type: "item"
    material: "EMERALD"
    amount: 1
story_actions:
  - type: "record_story_event"
    scope: "place"
    target: "tag:market"
    event_type: "quest_investigation_completed"
    summary: "Jucatorul a verificat avizierul suspect din piata."
required_anchors:
  - "tag:market"
  - "quest_board"
dialogue:
  offer: "Am gasit un anunt ciudat pe avizier. Poti verifica?"
  progress_hint: "Piata e chiar langa punctul de intalnire."
  completion: "Bine ca ai verificat. Satul trebuie sa stie ce se intampla."
validation_notes:
  - "Foloseste doar obiective suportate."
  - "Necesita mapping cu market si quest_board."
```

## Starile unui draft

```text
DRAFT
-> NORMALIZED
-> VALIDATED
-> NEEDS_REVISION
-> APPROVED
-> EXPORTED
-> DISCARDED
```

Reguli:

- `DRAFT` poate contine text AI brut si warning-uri.
- `NORMALIZED` are schema interna completa.
- `VALIDATED` inseamna ca validatorul nu a gasit erori blocante.
- `APPROVED` inseamna aprobare admin, nu activare live.
- `EXPORTED` inseamna ca exista artifact YAML sau patch de pack.
- `DISCARDED` ramane auditat cu motiv.

## Validare

### V1: Schema

Verifica:

- campuri obligatorii;
- tipuri de date;
- ID-uri stabile;
- stage-uri referite corect;
- obiective cu `objective_id` unic;
- reward-uri si story actions cu payload complet.

### V2: Runtime

Verifica:

- `base_type` acceptat;
- `mechanic` existenta;
- `scenario_kind` acceptat de `QuestScenarioContract`;
- obiective suportate de runtime/listeners;
- `completion` suportat pentru stage-uri;
- trigger-ele nu cer functionalitati inexistente.

### V3: Mapping

Verifica:

- `visit_region` are regiune valida;
- `visit_place` foloseste place ID, tag sau metadata gasibila in semantic index;
- `inspect_node` foloseste node ID, tip sau metadata gasibila;
- ancorele nu sunt ambigue fara strategie de rezolvare;
- draftul nu inventeaza coordonate in loc de ancore.

### V4: Story

Verifica:

- `set_story_state` are scope, key si value;
- `record_story_event` are scope, target si summary;
- story actions nu rescriu contradictoriu evenimente permanente;
- efectele story sunt proportionale cu questul.

### V5: Reward si balans

Verifica:

- materialele sunt valide;
- cantitatile sunt in limite;
- questurile usoare nu dau reward-uri mari;
- bounty-urile au recompensa separata de questurile principale;
- AI-ul nu promite reward-uri in dialog care nu exista in `rewards`.

### V6: Continut si siguranta

Verifica:

- textul nu contine instructiuni toxice sau nepotrivite;
- nu cere jucatorului actiuni in afara jocului;
- nu include date private;
- nu incearca sa ocoleasca validatoarele;
- nu introduce promisiuni de progres automat.

## Prompt contract

Promptul de authoring trebuie sa fie explicit:

```text
Returneaza doar JSON sau YAML conform schemei QuestDraft.
Nu crea obiective, reward-uri sau story actions in afara listelor acceptate.
Nu folosi coordonate brute daca exista ancore semantice.
Nu marca questul ca aprobat.
Nu spune ca reward-ul este acordat; doar propune reward-uri candidate.
Nu inventa servicii runtime.
Cand contextul nu ajunge, pune warning in validation_notes.
```

Promptul trebuie sa includa:

- schema output;
- listele acceptate de runtime;
- extrasul de mapping;
- questurile existente relevante;
- limitele de lungime;
- exemple pozitive scurte;
- exemple negative, daca modelul tinde sa inventeze campuri.

## Comenzi propuse

Aceste comenzi nu sunt implementate inca.

```text
/ainpc ai quest seed <regionId> <brief>
/ainpc ai quest draft <seedId>
/ainpc ai quest inspect <draftId>
/ainpc ai quest validate <draftId>
/ainpc ai quest revise <draftId> <brief>
/ainpc ai quest approve <draftId>
/ainpc ai quest export <draftId> [packId]
/ainpc ai quest discard <draftId> <reason>
```

Reguli:

- `seed` si `draft` sunt permise doar adminilor;
- `approve` si `export` cer permisiune separata;
- `export` nu activeaza automat questul live;
- toate comenzile au audit log.

## Persistenta drafturilor

Pentru MVP, exista doua optiuni bune.

### Fisiere in debug-dumps

Simplu pentru inceput:

```text
plugins/AINPC/drafts/quest/
  draft-q09-market-notice.seed.yml
  draft-q09-market-notice.raw.json
  draft-q09-market-notice.normalized.yml
  draft-q09-market-notice.validation.txt
  draft-q09-market-notice.export.yml
```

Avantaje:

- usor de inspectat;
- nu cere migrari DB;
- potrivit pentru authoring offline.

### Tabel dedicat

Pentru productie:

```text
quest_drafts
- id
- draft_id
- seed_id
- status
- source
- author_player_uuid
- region_id
- mechanic
- base_type
- category
- payload_json
- validation_status
- validation_report_json
- created_at
- updated_at
- approved_by
- approved_at
```

Avantaje:

- audit mai bun;
- review in GUI;
- istoric de revizii;
- posibilitatea de rollback al exporturilor.

## Integrare cu feature packs

Exportul trebuie sa produca un fragment YAML compatibil cu loader-ul curent, dar sa fie marcat ca draft.

```yaml
quests:
  q09_market_notice:
    enabled: false
    code: "Q09"
    title: "Umbra de pe Avizier"
    base_type: "QUEST"
    category: "main"
    mechanic: "main_quests"
    scenario_kind: "investigation"
    ai_generated:
      draft_id: "draft:q09:market_notice"
      reviewed: false
```

Reguli:

- exportul initial are `enabled: false`;
- activarea cere review manual sau comanda separata;
- auditul trebuie sa poata raporta ca definitia vine din draft AI;
- pack-ul final trebuie versionat ca orice continut scris manual.

## Integrare cu mapping

AI-ul nu trebuie sa aleaga locatii arbitrare.

Surse permise:

- `semantic_index`;
- `WorldContextSnapshot`;
- `StoryContextSnapshot`;
- rezultatul `/ainpc patch analyze|plan|validate`, daca lipseste infrastructura;
- `quest_anchor_bindings` existente, doar ca referinta read-only.

Cand lipseste mapping-ul, AI-ul trebuie sa aleaga una dintre variante:

- produce draft cu `requires_mapping: true`;
- propune un `PatchPlan` semantic separat;
- cere adminului sa creeze ancora prin wand;
- foloseste obiective non-mapate doar daca mecanica permite.

## Integrare cu story

AI-ul poate propune story hooks, dar story state-ul ramane determinist.

Story-ul poate decide directia unui quest, dar nu trebuie sa porneasca sau sa completeze questul direct.

Fluxul corect:

```text
StoryStateService
-> StoryContextService
-> QuestDirector
-> QuestSeed sau quest candidat existent
-> QuestDraft/QuestDefinition validata
-> ProgressionService
```

Cu alte cuvinte:

- story state-ul spune ce are sens acum in lume;
- `QuestDirector` alege daca exista un quest potrivit sau daca trebuie generat un draft;
- `QuestDraftValidator` si auditul decid daca propunerea este valida;
- `ProgressionService` este singurul strat care ofera, porneste, progreseaza si finalizeaza questul;
- `StoryStateService` ramane sursa de context si efect post-quest, nu runtime de quest.

Exemplu:

```text
region_story_state.market_unrest = true
recent_story_event = "avizierul din piata a fost vandalizat"
semantic_index contine tag:market si node quest_board
-> QuestDirector propune seed de investigatie
-> AI produce QuestDraft
-> validatorul verifica market/quest_board/rewards/story_actions
-> adminul aproba si exporta
```

Interzis:

```text
StoryStateService -> scrie direct in player_quests
AI -> creeaza quest live direct
Dialog -> completeaza quest direct
```

Permis:

- `record_story_event` cu summary scurt;
- `set_story_state` pentru chei simple si scoped;
- dialog care reflecta story state existent;
- sugestii de follow-up quest.

Interzis:

- rescrierea trecutului;
- schimbarea unei factiuni globale fara mecanica dedicata;
- story actions ascunse in text de dialog;
- contradictii fata de questuri principale.

## Integrare cu ProgressionService

Un draft nou trebuie sa declare mecanica in care intra.

Exemple:

| Continut | `base_type` | `mechanic` |
|---|---|---|
| quest principal | `QUEST` | `main_quests` |
| quest secundar | `QUEST` | `side_quests` |
| contract | `TRADE_DEAL` sau `CONTRACT` | `village_contracts` |
| sarcina NPC | `DUTY` | `npc_duties` |
| bounty | `BOUNTY` | `local_bounties` |
| eveniment local | `WORLD_EVENT` | `village_events` |
| tutorial | `TUTORIAL` | `onboarding` |
| ritual | `RITUAL` | `village_rituals` |

Reguli:

- AI-ul nu decide mecanici noi fara registru;
- mecanicile noi cer documentatie, audit si GUI/filter support;
- drafturile pentru mecanici existente trebuie sa foloseasca selectori compatibili cu `ProgressionFilter`.

## UX admin

Adminul trebuie sa vada:

- seed-ul original;
- contextul folosit;
- draftul brut;
- draftul normalizat;
- raportul de validare;
- warning-uri de mapping;
- diferente fata de questuri existente;
- estimare de durata/dificultate;
- fragmentul YAML exportabil.

Mesajele trebuie sa fie orientate pe actiune:

```text
Eroare: obiectivul inspect_node cere ancora quest_board, dar semantic index nu contine acest node.
Actiune: creeaza node cu /ainpc map node ... sau schimba ancora in draft.
```

## Workflow Paper

Pentru un quest generat cu AI, fluxul minim de testare ramane:

```text
1. genereaza draft
2. valideaza draft
3. exporta YAML cu enabled=false
4. ruleaza /ainpc audit quest strict
5. activeaza pe server de test
6. accepta questul cu jucator real
7. parcurge fiecare obiectiv
8. ruleaza /ainpc quest debug <code>
9. ruleaza /ainpc debugdump quest
10. restart server
11. confirma ca progresul si story event-urile raman coerente
```

Un draft AI nu este gata doar pentru ca YAML-ul se incarca.

## Faze mici

### AQ0: Document si contract

Status: documentul de fata.

Livrabile:

- schema `QuestSeed`;
- schema `QuestDraft`;
- reguli de validare;
- flux admin;
- faze de implementare.

### AQ1: Modele interne

Livrabile:

- `QuestSeed`;
- `QuestAuthoringContext`;
- `QuestDraft`;
- `QuestDraftValidationReport`;
- serializare JSON/YAML pentru drafturi.

### AQ2: Validator read-only

Livrabile:

- validator de schema;
- validator de obiective/rewards/story actions;
- validator de mapping peste semantic index;
- raport inspectabil.

### AQ3: Orchestrare AI pentru draft

Livrabile:

- use case `QUEST_DRAFT`;
- prompt versionat;
- output strict;
- fallback determinist cand AI este oprit;
- audit log pentru request/response fara secrete.

### AQ4: Comenzi admin

Livrabile:

- `seed`;
- `draft`;
- `inspect`;
- `validate`;
- `discard`;
- export in fisier, nu activare live.

### AQ5: Review si export pack

Livrabile:

- statusuri de draft;
- `approve`;
- export YAML cu `enabled=false`;
- raport diff fata de pack existent;
- audit quest dupa export.

### AQ6: Evals si dataset intern

Livrabile:

- exemple valide si invalide;
- teste pentru prompt;
- replay de request AI;
- masurare de rata de invalidare;
- categorii de motive de respingere.

## Ce trebuie evitat

- AI care scrie direct config live.
- AI care completeaza sau porneste questuri pentru jucatori.
- AI care acorda reward-uri.
- AI care inventeaza obiective fara listener.
- AI care inventeaza story actions fara validator.
- AI care foloseste coordonate brute in loc de mapping semantic.
- AI care creeaza mecanici noi fara registry, GUI si audit.
- Activare automata pe server live.
- Branching complex inainte de stage-uri liniare stabile.

## Definitia de gata pentru MVP

Generarea automata de questuri cu AI este suficienta pentru MVP cand:

- adminul poate crea un `QuestSeed`;
- AI-ul produce un `QuestDraft` strict;
- validatorul raporteaza erori si warning-uri clare;
- draftul poate fi exportat ca YAML dezactivat;
- `/ainpc audit quest strict` poate verifica exportul;
- exista cel putin un smoke test Paper pentru un draft acceptat;
- toate requesturile AI sunt auditate fara secrete;
- AI-ul poate fi oprit fara sa afecteze runtime-ul questurilor existente.

Pana atunci, AI-ul ramane asistenta de authoring si explicatie, nu sistem de continut live autonom.
