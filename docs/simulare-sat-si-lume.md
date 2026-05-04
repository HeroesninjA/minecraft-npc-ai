# Simulare Sat si Lume

Actualizat: 2026-05-04

Status: document canonic initial pentru trecerea de la NPC-uri individuale la o simulare de comunitate si lume. Acest document este design si ordine de lucru, nu dovada ca mecanicile sunt implementate complet in cod.

## Scop

AINPC nu trebuie sa ramana doar un sistem de NPC-uri care vorbesc si se teleporteaza intre ancore. Tinta este o lume in care NPC-urile sunt parte dintr-o comunitate:

- locuiesc in gospodarii
- au roluri sociale si economice
- produc, consuma si reactioneaza la lipsuri
- raspund la reputatia jucatorului si la starea satului
- sunt afectati de evenimente, questuri si schimbari de story
- pastreaza consecinte dupa reload

Acest document stabileste mecanicile lipsa si ordinea in care trebuie legate. Documentele existente acopera bine parti separate: mapping, spawn, rutine, questuri si story. Lipsa principala era un contract de ansamblu pentru simularea satului.

## Ce nu este

Acest document nu inlocuieste:

- `mapping.md`, pentru `WorldRegion -> WorldPlace -> WorldNode`
- `ordine-spawn-npc-cladiri-region-node.md`, pentru spawn order si household planning
- `rutine-npc-si-timeline.md`, pentru rutina zilnica si timeline
- `questuri-avansate.md`, pentru quest runtime
- `story-si-context-ai.md`, pentru context AI si story drafts

Regula: documentele de specialitate definesc contractele locale. Acest document defineste cum se combina intr-o simulare coerenta.

## Problema curenta

Starea actuala este buna pentru un MVP tehnic:

- exista NPC-uri persistente, memorie, emotii si relatie cu jucatorul
- exista mapping semantic initial
- exista spawn household/settlement initial
- exista rutina zilnica initiala peste `home/work/social anchors`
- exista story state si story events initiale
- questurile pot folosi `visit_place`, `inspect_node` si ancore persistente

Dar asta inca nu este simulare de lume.

Limitari importante:

- rutina este inca orientata pe NPC individual, nu pe comunitate
- gospodariile nu sunt inca model persistent matur
- economia, reputatia si factiunile sunt backlog, nu mecanica
- timeline-ul nu schimba inca lumea pe mai multe zile
- story state-ul este scris controlat, dar nu conduce inca sisteme de simulare
- AI-ul coloreaza dialogul, dar nu trebuie sa conduca direct runtime-ul

## Principiul de baza

Simularea reala trebuie sa porneasca de la starea lumii, nu de la textul generat de AI.

Formula corecta:

```text
mapping semantic
-> populatie si gospodarii
-> rutine si roluri
-> resurse si stare de regiune
-> evenimente si consecinte
-> quest/story hooks
-> dialog AI contextual
```

Formula gresita:

```text
AI inventeaza eveniment
-> NPC spune text
-> lumea ramane neschimbata
```

NPC-ul trebuie sa fie participant in simulare, nu singura sursa de adevar.

## Model conceptual

### SettlementSimulation

Componenta centrala viitoare poate fi gandita ca `SettlementSimulation`.

Responsabilitati:

- citeste starea regiunii, places, nodes si NPC bindings
- grupeaza NPC-urile pe household-uri, roluri si locuri de munca
- calculeaza indicatori simpli pentru sat
- aplica evenimente de simulare la intervale controlate
- expune snapshot-uri pentru audit, questuri, story si AI

Nu trebuie sa modifice direct blocuri si nu trebuie sa genereze dialog. Ea produce stare si intentii validate.

Model conceptual:

```text
SettlementSimulation
  regionId
  populationState
  householdState
  workState
  resourceState
  safetyState
  reputationState
  activeEvents
  warnings
```

### SettlementState

`SettlementState` este starea agregata a unei regiuni.

Campuri recomandate:

```text
regionId
populationCount
householdCount
availableBeds
workplaceCount
unassignedNpcCount
foodLevel
materialLevel
securityLevel
prosperityLevel
tensionLevel
healthLevel
storyFlags
updatedAt
```

Aceste valori nu trebuie sa fie perfecte economic. Pentru MVP este suficient sa fie stabile, persistente si inspectabile.

### PopulationState

Populatia nu trebuie sa fie doar o lista de NPC-uri.

Model minim:

```text
npcId
regionId
householdId
familyId
role
profession
ageGroup
simulationMode
homePlaceId
workPlaceId
socialPlaceId
currentRoutineSlot
availability
```

Roluri initiale utile:

- `worker`
- `merchant`
- `guard`
- `farmer`
- `craftsman`
- `quest_giver`
- `elder`
- `child`
- `visitor`

### HouseholdState

Household-ul este unitatea sociala de baza. Fara el, familia si rutina raman informatii imprastiate.

Model minim:

```text
householdId
regionId
homePlaceId
familyId
residentNpcIds
capacity
bedNodeIds
primaryProviderNpcId
householdMood
foodNeed
safetyNeed
wealthLevel
status
```

Reguli:

- un NPC permanent are un household principal
- o casa poate avea mai multi rezidenti
- familia poate coincide cu household-ul, dar nu este obligatoriu
- household-ul poate avea nevoie de hrana, siguranta sau reparatii
- questurile pot porni din nevoi reale ale household-ului

### WorkState

Locurile de munca trebuie sa conteze pentru sat, nu doar pentru rutina.

Model minim:

```text
workPlaceId
profession
assignedNpcIds
requiredNpcCount
productionType
productionRate
resourceInput
resourceOutput
status
```

Exemple:

- ferma produce `food`
- fieraria consuma `ore` si produce `tools`
- taverna consuma `food` si creste `social_stability`
- garda creste `security`

Pentru MVP, aceste valori pot fi abstracte. Nu trebuie sa existe inventare fizice complete din prima.

## Sisteme necesare

### 1. TimeService si calendar

Starea lumii are nevoie de timp comun.

Minim:

- zi curenta
- faza zilei
- saptamana sau ciclu local
- evenimente programate

Extensie ulterioara:

- anotimp
- sarbatori locale
- perioade de criza
- cooldown-uri regionale

### 2. RoutineEngine legat de comunitate

Rutina curenta trebuie sa consume mai mult decat ancorele din profil.

Extensii necesare:

- citire din `npc_world_bindings`
- citire din household persistent
- target node specific pentru pat, usa, workstation si social point
- reguli pentru cand NPC-ul poate fi intrerupt
- motiv inspectabil pentru alegerea rutinei

Rezultat dorit:

```text
NPC-ul merge la munca fiindca:
- este MORNING_WORK
- are workPlaceId valid
- forge_01 are nevoie de fierar
- nu exista quest/timeline override activ
```

### 3. ResourceState

Resursele trebuie sa fie abstracte la inceput.

Resurse MVP:

- `food`
- `materials`
- `tools`
- `medicine`
- `coin`
- `security`
- `morale`

Reguli:

- resursele sunt pe regiune sau household
- NPC-urile si locurile de munca pot modifica resursele
- questurile pot consuma sau restaura resurse
- story events pot explica schimbarile

Nu implementa economie granulara cu fiecare item Minecraft in prima faza.

### 4. EconomyService

Economia initiala trebuie sa fie mica si determinista.

Responsabilitati:

- calculeaza productie pe faza/zi
- calculeaza consum pe populatie/household
- aplica lipsuri si surplusuri
- produce evenimente cand pragurile sunt atinse

Exemple:

```text
food < 25
-> status regional: food_shortage
-> fermierii devin mai ingrijorati
-> quest de ajutor la ferma devine disponibil
```

```text
tools < 20 si forge exista
-> fierarul cere minereu
-> preturile cresc
-> story event: "uneltele sunt tot mai rare"
```

### 5. ReputationService

Reputatia trebuie separata de relatia individuala NPC-jucator.

Niveluri recomandate:

- relatie individuala: NPC <-> jucator
- reputatie pe household
- reputatie pe sat/regiune
- reputatie pe factiune sau rol

Model minim:

```text
playerUuid
scopeType: npc | household | region | faction
scopeId
score
tags
updatedAt
```

Surse de modificare:

- completare quest
- abandon quest
- furt sau atac
- ajutor in criza regionala
- aparare sat
- donatii sau comert

### 6. Event si Timeline

Evenimentele trebuie sa schimbe stare, nu doar sa afiseze text.

Tipuri MVP:

- `resource_shortage`
- `family_conflict`
- `market_day`
- `bandit_threat`
- `visitor_arrival`
- `workplace_problem`
- `illness`
- `festival`

Model minim:

```text
eventId
regionId
type
status
startDay
endDay
severity
affectedNpcIds
affectedPlaceIds
payload
createdBy
```

Reguli:

- evenimentul are durata
- evenimentul este persistat
- evenimentul poate aplica override-uri de rutina
- evenimentul poate activa questuri
- evenimentul trebuie sa fie inspectabil

### 7. Quest si story hooks

Questurile trebuie sa citeasca starea simularii si sa scrie consecinte controlate.

Exemple:

- questul de fierar porneste doar daca exista `tools_low`
- questul de hrana porneste daca `foodLevel` este scazut
- dupa completare, `foodLevel` creste si se inregistreaza `StoryEvent`
- daca jucatorul ignora criza, `tensionLevel` creste

Tipuri de conditii recomandate:

```text
region_state_at_least
region_state_below
resource_below
resource_above
reputation_at_least
event_active
household_has_need
workplace_status
```

Tipuri de actiuni recomandate:

```text
adjust_region_resource
adjust_household_need
adjust_reputation
start_timeline_event
resolve_timeline_event
set_workplace_status
record_story_event
```

### 8. AI ca prezentare, nu autoritate

AI-ul poate:

- formula dialogul
- rezuma starea satului
- propune drafturi de quest/story
- varia tonul social

AI-ul nu trebuie sa:

- scrie direct in DB
- creeze resurse sau reputatie fara validare
- modifice timeline-ul direct
- inventeze locuri sau NPC-uri executabile
- decida singur completarea questurilor

Runtime-ul decide. AI-ul explica si coloreaza.

## Persistenta recomandata

Tabele sau modele viitoare:

```text
settlement_state
settlement_resources
households
household_residents
workplace_state
player_reputation
simulation_events
simulation_event_targets
timeline_events
active_timeline_overrides
```

Ordine recomandata:

1. `households` si `household_residents`
2. `settlement_state` si `settlement_resources`
3. `player_reputation`
4. `simulation_events`
5. `timeline_events`

Nu introduce toate tabelele simultan fara migration si debugdump.

## Comenzi de inspectie recomandate

Pentru ca simularea sa fie operabila, fiecare sistem trebuie inspectabil.

Comenzi MVP:

```text
/ainpc sim region <regionId>
/ainpc sim resources <regionId>
/ainpc sim households <regionId>
/ainpc sim household <householdId>
/ainpc sim events <regionId>
/ainpc sim tick <regionId>
```

Comenzi ulterioare:

```text
/ainpc reputation <player> <regionId>
/ainpc economy region <regionId>
/ainpc timeline list <regionId>
/ainpc timeline inspect <eventId>
```

## Faze mici fara test Paper imediat

### S0: Documentatie si contracte

Livrabile:

- documentul de fata
- `generare-populatie-narativa.md`
- contract minim pentru households persistente
- lista de comenzi de inspectie

Nu necesita server Paper.

### S1: Snapshot read-only de simulare

Livrabile:

- model `SettlementSimulationSnapshot`
- agregare read-only din mapping, NPC-uri si `npc_world_bindings`
- comanda inspectie fara scrieri

Scop:

- sa vedem ce sat poate fi simulat inainte sa modificam DB-ul.

### S2: Household persistent minim

Document:

- `households-persistente.md`

Livrabile:

- `households`
- `household_residents`
- backfill din metadata si `npc_world_bindings`
- audit read-only

Scop:

- familia, rutina si spawn-ul sa aiba aceeasi sursa de adevar.

### S3: ResourceState abstract

Livrabile:

- resurse regionale simple
- citire/scriere controlata
- debugdump
- reguli fara economie complexa

Scop:

- questurile si story-ul pot avea consecinte numerice simple.

### S4: Reputation MVP

Livrabile:

- reputatie pe regiune
- modificari prin quest completion/abandon
- afisare in context NPC

Scop:

- satul reactioneaza la istoria jucatorului, nu doar NPC-ul individual.

### S5: Event MVP

Livrabile:

- evenimente persistente pe regiune
- start/resolve manual
- impact mic asupra rutinei sau quest availability

Scop:

- lumea poate avea situatii active, nu doar flags statice.

## Ordinea recomandata acum

Pentru proiectul curent, ordinea pragmatica este:

1. documentatie si contracte pentru simulare
2. inspectie read-only peste datele existente
3. model persistent pentru household; vezi `households-persistente.md`
4. generator narativ de populatie dry-run
5. resurse regionale abstracte
6. reputatie regionala MVP
7. evenimente regionale MVP
8. integrare in quest/story

Smoke test-ul Paper pentru mapping ramane important, dar nu blocheaza aceste faze de documentare si modele read-only.

## Ce trebuie evitat

- Nu construi economie completa inainte de household-uri persistente.
- Nu introduce reputatie globala pe factiuni inainte de reputatie regionala simpla.
- Nu face timeline complex inainte sa existe evenimente persistente inspectabile.
- Nu lasa AI-ul sa modifice direct starea simularii.
- Nu dubla sursele de adevar intre metadata, profile data si DB fara reguli clare.
- Nu porni de la questuri complexe daca satul nu are stare care sa produca motive reale.

## Definitia de "simulare reala" pentru MVP

Pentru primul nivel credibil, simularea este suficienta daca:

- fiecare NPC permanent are household, home, work si social place clare
- satul are resurse abstracte inspectabile
- questurile pot modifica resurse sau reputatie
- cel putin un eveniment regional schimba rutina sau disponibilitatea unui quest
- story events explica modificarile importante
- dupa reload, starea ramane coerenta

Nu este nevoie inca de economie completa, pathfinding perfect sau factiuni mari. Este nevoie de consecinte persistente, vizibile si conectate intre sisteme.
