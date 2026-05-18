# Story si Context AI - Mapping, Indexare si Questuri

Actualizat: 2026-05-11

Status: design pentru faze urmatoare, cu Faza A implementata initial, Faza B implementata initial la nivel de quest anchors, `StoryContextService` implementat initial read-only si Faza C implementata initial la nivel de schema, serviciu si actiuni de quest pentru story state.

## Scop

Acest document stabileste cum trebuie legate mapping-ul, indexarea, questurile si story-ul astfel incat AI-ul sa aiba suficient context pentru:

- dialog contextual
- generare de questuri coerente
- povesti locale pe regiuni si sate
- evenimente narative legate de locuri reale din lume
- continut validabil inainte de executie

Ideea principala: AI-ul nu trebuie sa primeasca toata harta si nu trebuie sa inventeze coordonate. Mapping-ul trebuie sa fie sursa de adevar, indexarea trebuie sa gaseasca rapid contextul relevant, iar quest/story runtime-ul trebuie sa valideze ce poate fi folosit.

## Problema curenta

Mapping-ul are deja concepte utile:

- `WorldRegion`
- `WorldPlace`
- `WorldNode`
- auto-indexare interna pentru lookup-uri rapide
- scanner si mapper semantic initial

Questurile au deja baza:

- `ScenarioEngine`
- questuri definite in YAML
- progres persistent in `player_quests`
- obiective precum `visit_region`, `talk_to_npc`, `deliver_to_npc`

Stratul care le leaga este inceput, dar nu este matur complet:

- `NPCContext` include initial `WorldContextSnapshot` cu regiune/place/node-uri apropiate
- promptul AI primeste initial sectiunea `WORLD_CONTEXT`
- questurile suporta initial `visit_place` si `inspect_node`
- `QuestAnchorResolver` rezolva initial ancore pentru obiective semantice
- ancorele de quest sunt persistate initial in `quest_anchor_bindings` si reflectate in `questVariables`
- `StoryContextService` construieste initial `STORY_CONTEXT` din mapping, NPC tinta, jucator si quest anchors active
- story state-ul pe regiune/place nu este persistat ca model dedicat
- AI-ul nu are un contract clar: ce poate propune, ce trebuie validat si ce este interzis

## Principiul de baza

Nu:

```text
AI -> inventeaza quest -> inventeaza locatii -> plugin executa
```

Da:

```text
mapping -> index semantic -> context snapshot -> AI draft
-> resolver -> validator -> quest/story runtime -> persistenta
```

Coordonatele sunt utile pentru detectie si teleport/pathing. Gameplay-ul narativ trebuie sa lucreze cu:

- `regionId`
- `placeId`
- `nodeId`
- `npcId`
- `familyId`
- `householdId`
- `tags`
- `storyState`

## Integrare fara conflict intre contexte

Contextul final pentru AI, dialog, `QuestDirector` sau GUI nu trebuie sa fie un obiect mare in care toate sistemele scriu. Trebuie compus din snapshot-uri mici, fiecare cu proprietar clar.

```text
WorldContext = unde este NPC-ul sau jucatorul
StoryContext = ce se intampla in regiune/place
MemoryContext = ce tine minte NPC-ul despre jucator/evenimente
ProgressionContext = ce questuri/progresii sunt active
```

Agregarea recomandata:

```text
ContextAssembler
-> WorldContextSnapshot
-> StoryContextSnapshot
-> MemorySnapshot
-> ProgressionSnapshot
-> PromptContext / QuestDirectorContext / GuiSnapshot
```

Regula principala: fiecare context are un singur proprietar de scriere.

| Context | Proprietar | Scrie | Citeste |
|---|---|---|---|
| World | `WorldAdminService` | mapping semantic | story, quest, AI, GUI |
| Story | `StoryStateService` | story state si story events | AI, `QuestDirector`, GUI |
| Memory | `MemoryManager`, viitor `MemoryService` | memorii NPC despre jucator/evenimente | AI, dialog, reactii, `QuestDirector` optional |
| Progression | `ProgressionService` | progres jucator, obiective, stages, status | GUI, story actions, AI |

Reguli de evitare a conflictelor:

- `MemoryService` nu schimba story state.
- `StoryStateService` nu scrie memorii NPC.
- `ProgressionService` nu decide directia narativa, ci executa progres validat.
- `QuestDirector` citeste contextul combinat, dar produce doar decizie read-only.
- AI-ul nu scrie direct in DB, story, memorie sau progres; AI propune, serviciile valideaza.

Exemplu:

```text
Jucatorul ajuta fierarul.

ProgressionService completeaza questul.
-> StoryStateService scrie story event: blacksmith_helped
-> MemoryService scrie memorie NPC: "Ion m-a ajutat cu fieraria."
```

Nu este conflict, pentru ca sunt adevaruri diferite:

```text
Story = in sat s-a intamplat evenimentul X
Memory = NPC-ul Y tine minte ceva despre jucatorul Z
```

## Matrice permisiuni story

Story are trei niveluri separate. Orice functie noua trebuie incadrata explicit intr-unul dintre ele.

| Nivel | Cine poate declansa | Operatii permise | Interzis |
|---|---|---|---|
| `read_only` | `ainpc.admin`, `ainpc.gui.story`, debug/admin GUI | citire `region_story_state`, `place_story_state`, `story_events`, context, audit, debugdump | `saveRegionState`, `savePlaceState`, `recordEvent` |
| `write_admin` | admin explicit, viitoare comenzi de repair/migration/seed controlat | scriere manuala cu confirmare, `source` clar si audit dupa operatie | scriere ascunsa din GUI sau dialog |
| `write_runtime` | runtime validat, de exemplu completion prin `ProgressionService`/`ScenarioEngine` | `set_story_state` si `record_story_event` declarate in pack, dupa validare | AI text care scrie direct in DB |

Reguli:

- `/ainpc gui story` este strict `read_only`: poate afisa region state, place state si evenimente recente, dar nu modifica story.
- `/ainpc story region|place|events|context` este inspectie admin; daca in viitor apar comenzi de scriere, ele trebuie separate clar de inspectie.
- actiunile `set_story_state` si `record_story_event` sunt `write_runtime`, nu `write_admin`; ele ruleaza doar ca efect al unei progresii validate.
- pack-urile trebuie validate inainte de runtime: story action are `scope` valid (`region`/`place`), target explicit, `event_key` pentru `record_story_event` si payload semantic minim.
- AI-ul poate propune `QuestSeed`, `QuestDraft` sau un story action candidat, dar nu primeste permisiune directa de scriere.
- orice scriere story trebuie sa lase `source`, `updated_by` sau payload suficient pentru audit/debugdump.

Pentru prompt AI, sectiunile trebuie pastrate separate:

```text
WORLD_CONTEXT:
...

STORY_CONTEXT:
...

MEMORY_CONTEXT:
...

PROGRESSION_CONTEXT:
...
```

Daca doua contexte par sa se contrazica, prioritatea este:

```text
Progression > Story > Memory > AI text
```

Exemplu: daca memoria spune ca jucatorul este prieten, dar progresul curent spune ca jucatorul a tradat o factiune, dialogul respecta progresul/story-ul curent, iar memoria doar nuanteaza reactia NPC-ului.

## Arhitectura recomandata

### 1. WorldSemanticIndex

Indexarea existenta trebuie extinsa conceptual cu query-uri semantice, nu doar lookup geografic.

Query-uri utile:

```text
findRegion(world, x, y, z)
findPlace(world, x, y, z)
findNearbyPlaces(regionId, tags, radius)
findNodesForPlace(placeId)
findNodesByTag(regionId, tag)
findQuestAnchors(regionId, questType)
findStoryAnchors(regionId, npcRole)
findNpcBindings(npcId)
findPlacesByRole(regionId, role)
```

Indexul trebuie sa raspunda la intrebari de gameplay:

- unde este NPC-ul acum?
- ce locuri relevante sunt in jur?
- exista o fierarie reala in regiune?
- exista un node de inspectie?
- exista un loc public unde poate porni un quest?
- exista o ancora pentru conflict, pericol sau recompensa?

### 2. WorldContextSnapshot

AI-ul si quest engine-ul trebuie sa consume un snapshot scurt, nu obiectele brute din world admin.

Model recomandat:

```text
WorldContextSnapshot
  worldName
  currentRegion
  currentPlace
  nearbyPlaces
  nearbyNodes
  npcBindings
  nearbyNpcs
  activeQuestAnchors
  regionStoryState
  placeStoryState
  recentStoryEvents
  warnings
```

Campuri pentru `currentRegion`:

```text
id
displayName
type
tags
summary
storyFlags
dangerLevel
```

Campuri pentru `currentPlace`:

```text
id
displayName
type
tags
ownerNpcId
access
state
summary
```

Campuri pentru `nearbyNodes`:

```text
id
placeId
type
tags
interactionType
summary
```

Snapshot-ul trebuie limitat. Recomandare:

- maxim 1 regiune curenta
- maxim 1 place curent
- maxim 5-10 places apropiate
- maxim 10-15 node-uri relevante
- maxim 5 NPC-uri relevante
- maxim 5 story events recente

### 3. Prompt AI cu sectiune WORLD_CONTEXT

Promptul AI trebuie sa primeasca o sectiune structurata si compacta.

Exemplu:

```text
WORLD_CONTEXT:
- region: satul_central, type=village, tags=[medieval, safe, farming]
- current_place: satul_central:fierarie, type=forge, tags=[workplace, blacksmith, shop]
- relevant_nodes:
  - satul_central:fierarie:anvil, type=workstation, tags=[blacksmith, inspect]
  - satul_central:piata:notice_board, type=quest_board, tags=[quest, public]
- npc_bindings:
  - home=satul_central:casa_fierarului
  - work=satul_central:fierarie
- story_state:
  - region_flag=iron_shortage
  - tension=medium
  - recent_event=caravana de minereu a intarziat
```

Regula: AI-ul poate folosi doar ID-urile si conceptele prezente in context sau poate cere explicit o ancora noua ca draft, dar nu o poate executa direct.

## Legatura cu questurile

Questurile trebuie sa fie generate sau alese pe baza de ancore reale.

### QuestAnchorResolver

Un resolver trebuie sa transforme cerintele unui quest in ID-uri reale:

```text
quest template -> required tags/types -> mapping lookup -> anchor bindings
```

Exemplu YAML:

```yaml
quest:
  code: "Q_IRON_SHORTAGE"
  requires:
    region_tags: ["village"]
    place_tags: ["blacksmith"]
    node_tags: ["quest", "inspect"]
    npc_roles: ["blacksmith"]
  objectives:
    inspect_anvil:
      type: "inspect_node"
      target: "$node:blacksmith.inspect"
    visit_market:
      type: "visit_place"
      target: "$place:market"
```

Rezolvare runtime:

```text
$place:blacksmith -> satul_central:fierarie
$node:blacksmith.inspect -> satul_central:fierarie:anvil
$place:market -> satul_central:piata
```

Rezultatul trebuie persistat in progresul questului:

```text
questId
playerId
regionId
placeBindings
nodeBindings
npcBindings
storyArcId
```

### Obiective necesare

Obiectivele naturale peste mapping sunt:

- `visit_place`
- `enter_place`
- `inspect_node`
- `talk_to_npc_at_place`
- `deliver_to_place`
- `collect_near_node`
- `defend_region`
- `escort_between_places`
- `trigger_place_event`

Acestea trebuie sa foloseasca ID-uri semantice, nu coordonate hardcodate.

### Validare inainte de oferire

Un quest nu trebuie oferit daca:

- regiunea ceruta lipseste
- place-ul cerut lipseste
- node-ul cerut lipseste
- NPC-ul cerut nu exista sau nu are rolul potrivit
- place-ul este privat si questul cere acces public
- story state-ul blocheaza questul
- questul cere o ancora deja folosita exclusiv de alt quest activ

## Legatura cu story-ul

Story-ul trebuie vazut ca stare persistenta si evenimente, nu doar text generat.

Modele recomandate:

```text
StoryArc
  id
  regionId
  title
  status
  priority
  theme
  activeFlags
  createdAt
  updatedAt
```

```text
StoryEvent
  id
  storyArcId
  regionId
  placeId
  nodeId
  npcId
  playerId
  type
  summary
  impact
  createdAt
```

```text
QuestStoryLink
  questId
  storyArcId
  regionId
  contributionType
  status
```

Story-ul trebuie sa raspunda la intrebari clare:

- ce tensiune exista in regiune?
- cine este afectat?
- ce locuri sunt implicate?
- ce questuri pot avansa povestea?
- ce s-a intamplat recent?
- ce nu are voie AI-ul sa schimbe?

## Story-driven quest selection

Story-ul poate decide directia questului, dar nu executa questul.

Regula:

```text
story state decide ce are sens
QuestDirector alege sau genereaza candidatul
validatorul confirma ca este jucabil
ProgressionService ruleaza progresul
```

Responsabilitati:

- `StoryStateService` pastreaza starea narativa si evenimentele persistente.
- `StoryContextService` construieste snapshot-ul read-only pentru decizie.
- `QuestDirector` transforma contextul in selectie de quest existent sau `QuestSeed` pentru draft.
- `QuestAnchorResolver` leaga obiectivele de regiuni, places, nodes si NPC-uri reale.
- `ProgressionService` ofera, porneste, progreseaza si finalizeaza questul.
- AI-ul poate formula motivatie, dialog si `QuestDraft`, dar nu scrie progres.

Flux pentru quest existent:

```text
region_story_state + recent_story_events + semantic_index
-> StoryContextSnapshot
-> QuestDirector
-> quest template candidat
-> QuestAnchorResolver
-> audit/validator
-> ProgressionService.offer/start
```

Flux pentru quest generat:

```text
StoryContextSnapshot
-> QuestSeed
-> AIOrchestrationService
-> QuestDraft
-> QuestDraftValidator
-> admin review/export
-> audit quest
-> ProgressionService dupa activare
```

Interzis:

- `StoryStateService` care scrie direct in `player_quests`;
- AI care creeaza quest live direct;
- dialog care completeaza obiective fara runtime;
- story flag care acorda reward fara validator;
- quest generat fara mapping/audit.

## StoryContextService

Implementat initial read-only:

```text
StoryContextService
  buildForNpc(npc, player)
  buildForPlayer(player)
```

Ce face acum:

- combina `WorldContextSnapshot` cu NPC-ul tinta si jucatorul
- incarca quest anchors active din `quest_anchor_bindings`
- produce sectiunea `STORY_CONTEXT` pentru prompt si debugging
- expune comanda `/ainpc story context [jucator] [numeNpc|nearest]`

Extindere recomandata pentru fazele urmatoare:

```text
StoryContextService
  buildForRegion(regionId)
  buildForQuestDraft(regionId, questCategory)
  recordEvent(event)
  setFlag(scope, key, value)
  validateStoryDraft(draft)
```

El trebuie sa combine:

- mapping semantic
- NPC bindings
- quest progress
- reputatie/relatie, cand exista
- evenimente recente
- story flags persistente

In implementarea actuala, evenimentele recente si story flags persistente exista initial prin `story_events`, `region_story_state` si `place_story_state`. Semnalele story din mapping raman completate din `WorldRegionInfo`, `WorldPlaceInfo.metadata` si `quest_anchor_bindings`.

AI-ul nu trebuie sa scrie direct in DB. AI-ul produce drafturi:

```text
StoryDraft
QuestDraft
NpcDialogueDraft
```

Pluginul valideaza si importa doar ce trece contractele.

## Pipeline pentru generare de quest/story

Flux recomandat:

1. jucatorul interactioneaza cu un NPC sau intra intr-o regiune
2. `WorldContextSnapshot` strange ancorele relevante
3. `StoryContextService` adauga semnale story, quest anchors active si, ulterior, flags/evenimente persistente
4. `QuestAnchorResolver` verifica ce quest templates sunt posibile
5. AI-ul primeste doar contextul validat
6. AI-ul propune text, ton, motivatie sau draft de quest
7. validatorul verifica ancorele, tipurile, limitele si consistenta
8. quest/story runtime-ul persista rezultatul
9. auditul poate explica de ce un quest/story este sau nu disponibil

## Date persistente recomandate

Pentru maturizare, pe langa tabelele actuale, merita introduse:

```text
npc_world_bindings
region_story_state
place_story_state
story_arcs
story_events
quest_story_links
```

`quest_anchor_bindings` exista deja initial si trebuie tratat ca primul model persistent din aceasta lista.

`npc_world_bindings` trebuie sa fie sursa pentru:

- casa NPC-ului
- locul de munca
- locul social
- familie/household
- rol local

`quest_anchor_bindings` trebuie sa fie sursa pentru:

- target place
- target node
- giver NPC
- receiver NPC
- region scope

## Impartire pe faze

### Faza A: Context read-only

Scop:

- `WorldContextSnapshot` peste `WorldAdminService`
- includere context semantic in `NPCContext`
- sectiune `WORLD_CONTEXT` in prompt

Status: implementat initial.

Observatii:

- snapshot-ul este read-only si se construieste din `WorldAdminService`
- `NPCContext` include `currentRegion`, `currentPlace`, places/nodes apropiate, bindings de ancora si NPC-uri apropiate
- nu exista inca persistenta dedicata pentru `homePlaceId/workPlaceId/socialPlaceId`

### Faza B: Questuri legate de mapping

Scop:

- `visit_place`
- `inspect_node`
- `QuestAnchorResolver`
- persistenta pentru anchor bindings

Questurile trebuie sa refuze template-uri fara ancore reale.

Status: implementat initial.

Implementat initial:

- `visit_place`
- `inspect_node`
- `QuestAnchorResolver`
- persistenta `quest_anchor_bindings`
- hidratare fallback din DB in `questVariables` la reload
- `/ainpc quest anchors` si `/ainpc audit quest` pentru inspectie read-only
- lookup indexat pentru node curent si node-uri apropiate

Ramas:

- validare schema mai stricta pentru template-uri si raport admin dedicat
- repair/backfill pentru binding-uri de quest dupa rename-uri de mapping

### Faza B.5: Story context read-only

Scop:

- `StoryContextService`
- `StoryContextSnapshot`
- sectiune `STORY_CONTEXT` in prompt cand NPC-ul interactioneaza cu un jucator
- comanda admin `/ainpc story context [jucator] [numeNpc|nearest]`

Status: implementat initial.

Observatii:

- serviciul este read-only si nu creeaza story state nou
- foloseste mapping-ul curent, NPC-ul tinta, jucatorul si quest anchors active
- include warnings cand mapping-ul, DB-ul sau locatia lipsesc

### Faza C: Story state regional

Scop:

- `region_story_state`
- `place_story_state`
- `story_events`
- actiuni de quest precum `set_story_state` si `record_story_event`

Story-ul incepe sa evolueze dupa questuri si interactiuni.

Status: implementat initial la nivel de persistenta, citire in context si actiuni controlate de quest.

Implementat initial:

- tabele `region_story_state`, `place_story_state` si `story_events`
- `StoryStateService`
- modele `RegionStoryState`, `PlaceStoryState` si `StoryEvent`
- includere in `StoryContextSnapshot` si `STORY_CONTEXT` cand exista mapping curent
- comenzi read-only `/ainpc story region`, `/ainpc story place` si `/ainpc story events`
- actiuni de quest `set_story_state` si `record_story_event`, executate la finalizarea questului
- metadata, `variables` si `payload` citite din YAML pentru intrari de quest non-item

Exemplu YAML minim:

```yml
rewards:
  story_state:
    type: "set_story_state"
    scope: "region"
    target: "current_region"
    state: "blacksmith_helped"
    variables:
      quest: "Q01"
  story_event:
    type: "record_story_event"
    scope: "region"
    target: "current_region"
    event_type: "quest_completed"
    event_key: "q01_completed"
    title: "Fierarul a primit materialele"
    payload:
      quest: "Q01"
```

Ramas:

- audit/debugdump pentru story state
- validator explicit pentru actiunile story din feature packs

### Faza D: Authoring asistat de AI

Scop:

- AI-ul propune quest/story drafts
- validatorul verifica schema si ancorele
- adminul poate importa controlat draftul

AI-ul nu executa direct modificari in lume.

## Audit si debugging

Comenzi utile pentru fazele urmatoare:

```text
/ainpc story context [jucator] [numeNpc|nearest]
/ainpc quest anchors [jucator|uuid|all] [templateId]
/ainpc story region <regionId>
/ainpc story place <placeId>
/ainpc story events <regionId|placeId> [limit]
/ainpc story validate <storyArcId>
/ainpc world context <npcId>
/ainpc ai context <npcId>
```

Primele cinci comenzi exista initial. Restul sunt propuneri pentru fazele cu validare si inspectie AI mai avansata.

Validari utile:

- story arc fara regiune valida
- quest anchor catre place/node lipsa
- node in afara place-ului parinte
- NPC fara `homePlaceId`
- NPC cu `workPlaceId` in regiune inexistenta
- prompt context prea mare
- template de quest care cere tag-uri inexistente
- story flag folosit de quest, dar nedefinit

## Ordine minima recomandata

Pasi 1-9 si actiunile story de baza sunt implementate initial. Urmatorul pas recomandat este audit/debugdump pentru persistenta story si validare explicita pentru actiunile din feature packs.

1. creeaza `WorldContextSnapshot` read-only
2. leaga snapshot-ul la `NPCContext` si prompt AI
3. adauga obiectivul `visit_place`
4. adauga obiectivul `inspect_node`
5. stabilizeaza `QuestAnchorResolver` peste mai multe template-uri reale
6. persista `quest_anchor_bindings`
7. adauga `StoryContextService`
8. persista `region_story_state`, `place_story_state` si `story_events`
9. adauga comenzi read-only pentru story state si events
10. adauga audit/debugdump pentru `region_story_state`, `place_story_state` si `story_events`
11. adauga validatoare pentru actiunile story din feature packs
12. abia apoi adauga generare AI de quest/story drafts

## Avertizari

- Nu da AI-ului toata harta; trimite un rezumat semantic limitat.
- Nu lasa AI-ul sa inventeze coordonate sau ID-uri executabile.
- Nu genera questuri daca ancorele cerute lipsesc.
- Nu lega questurile de nume fixe de NPC cand poti folosi roluri si tag-uri.
- Nu trata story-ul ca text liber; persista stari si evenimente.
- Nu combina generarea de cladiri, spawn NPC, questuri si story intr-un singur pas netestabil.
- Nu considera `metadata` solutie finala pentru bindings critice.

## Rezultat dorit

Cand acest sistem este implementat, AI-ul poate genera continut coerent pentru ca primeste:

- unde se afla NPC-ul
- ce locuri reale exista in jur
- ce node-uri pot fi inspectate sau folosite
- ce roluri au NPC-urile locale
- ce conflicte si evenimente exista in regiune
- ce questuri sunt valide tehnic
- ce story state poate fi schimbat

Astfel, questurile si povestile devin legate de lumea reala a serverului, nu de text inventat care nu poate fi executat.
