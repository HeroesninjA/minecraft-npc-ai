# Ordine spawn NPC, cladiri, regiuni si node-uri

Actualizat: 2026-04-29

## Scop

Acest document stabileste ordinea recomandata pentru generarea unei regiuni, a cladirilor si a NPC-urilor astfel incat:

- NPC-urile sa fie spawnate in locul corect
- fiecare NPC sa primeasca o cladire de locuit
- mai multi NPC sa poata fi atasati aceleiasi case pentru a simula o familie
- mapping-ul `Region -> Place -> Node` sa ramana sursa semantica pentru lume
- scriptul de familie, care este inca incomplet, sa nu creeze date greu de corectat ulterior

In proiect, termenul corect este `node`, nu `noodle`.

## Principiul de baza

Nu spawna NPC-ul inainte sa existe mapping-ul semantic al locului in care trebuie sa traiasca.

Ordinea sanatoasa este:

1. planifica regiunea, cladirile, node-urile si familiile
2. construieste sau rezerva fizic cladirile
3. creeaza `WorldRegion`
4. creeaza `WorldPlace` pentru fiecare cladire importanta
5. creeaza `WorldNode` pentru spawn, pat, intrare, lucru si social
6. aloca NPC-urile la case si locuri de munca
7. spawneaza NPC-urile in node-urile lor
8. salveaza profilurile NPC
9. leaga familiile intre NPC-urile reale
10. ruleaza audit si debug dump

Daca NPC-ul este spawnat prea devreme, `NPCManager.ensureSimulationAnchors(...)` poate folosi fallback-ul la locatia curenta sau la cel mai apropiat pat, nu casa planificata.

## Starea curenta din cod

Sistemul existent are deja conceptele necesare:

- `WorldRegion` = zona mare, de exemplu un sat
- `WorldPlace` = cladire sau loc semantic, de exemplu casa, fierarie, piata
- `WorldNode` = punct exact, de exemplu spawn NPC, pat, intrare, masa de lucru
- `AINPC.OwnedLocation` = ancora salvata pe NPC pentru `home`, `work` si `social`
- `FamilyManager` = relatii de familie in `npc_family`

Limitari importante:

- `WorldPlace` are doar un singur `owner_npc_id`
- NPC-ul nu are inca `homePlaceId`, `workPlaceId` sau `socialPlaceId`
- locuitorii multipli ai unei case trebuie tinuti momentan in `metadata`
- `FamilyManager.generateFamily(...)` poate genera rude textuale, nu neaparat NPC-uri spawnate fizic
- pentru familie fizica in aceeasi casa trebuie creati mai multi NPC reali si legati manual prin `linkNPCsAsFamily(...)`

## Cum alege codul ancora de casa

Pentru casa, `NPCManager` cauta in aceasta ordine:

1. `WorldPlace` mapat ca locuinta
2. pat fizic apropiat
3. fallback la locatia curenta a NPC-ului

Un `WorldPlace` este recunoscut ca locuinta daca are:

- `type: house`
- tag `home`
- tag `house`
- `metadata.role: home`
- `metadata.purpose: home`

Un place mapat este preferat daca:

- `owner_npc_id` se potriveste cu UUID-ul, database id-ul, `npc_<id>` sau numele normalizat al NPC-ului
- NPC-ul este spawnat in interiorul acelui place
- place-ul este la cel mult 32 blocuri

Consecinta practica: pentru spawn determinist, NPC-ul trebuie spawnat in interiorul casei asignate sau casa trebuie sa aiba `owner_npc_id` setat corect inainte de spawn/profilare.

## Ordinea recomandata pentru un sat generat

### 1. Planifica totul in memorie

Inainte de orice spawn sau paste de structura, creeaza un plan intern:

```text
VillagePlan
  regionId
  bounds
  buildingPlans
  nodePlans
  npcPlans
  houseAllocations
```

Planul trebuie sa contina:

- cate case exista
- cate paturi are fiecare casa
- ce NPC-uri vor locui in fiecare casa
- care NPC este proprietarul principal
- ce NPC-uri formeaza o familie
- unde este locul de munca pentru fiecare meserie
- unde se afla node-ul de spawn pentru fiecare NPC

Regula: in faza de planificare nu se spawneaza nimic.

### 2. Construieste sau rezerva cladirile

Executa builderul nativ sau WorldEdit numai dupa ce planul a trecut validarile de teren.

Cladirile minime pentru un sat coerent:

- case
- cel putin un punct social, de exemplu piata sau fantana
- locuri de munca pentru meseriile generate
- drumuri sau acces catre intrari

Pentru case, fiecare template trebuie sa declare:

- bounds
- intrare
- interior/spawn
- paturi
- `max_residents`

### 3. Creeaza `WorldRegion`

Regiunea trebuie creata inaintea cladirilor:

```yml
world_admin:
  regions:
    sat_noodle:
      name: "Satul Noodle"
      world: "world"
      type: "settlement"
      min: { x: 100, y: 60, z: 100 }
      max: { x: 220, y: 90, z: 220 }
      tags: [village, generated]
```

Toate cladirile si node-urile trebuie sa fie in bounds-ul acestei regiuni.

### 4. Creeaza `WorldPlace` pentru cladiri

Fiecare cladire cu gameplay devine `WorldPlace`.

Pentru case:

```yml
places:
  casa_popescu:
    name: "Casa familiei Popescu"
    type: "house"
    min: { x: 120, y: 64, z: 130 }
    max: { x: 128, y: 72, z: 138 }
    tags: [home, house, private]
    owner_npc_id: "npc_ion"
    public_access: false
    metadata:
      role: "home"
      family_id: "fam_popescu_001"
      residents: "npc_ion,npc_maria,npc_ana"
      max_residents: "3"
```

Reguli:

- `owner_npc_id` este proprietarul principal, nu lista completa de locuitori
- `metadata.residents` tine lista temporara de locuitori pana apare model dedicat
- `metadata.family_id` leaga casa de familia simulata
- `metadata.max_residents` trebuie sa fie cel putin numarul de NPC-uri asignate

Pentru locuri de munca:

```yml
fierarie:
  name: "Fieraria"
  type: "forge"
  min: { x: 145, y: 64, z: 132 }
  max: { x: 156, y: 73, z: 142 }
  tags: [workplace, blacksmith]
  owner_npc_id: "npc_ion"
  public_access: true
  metadata:
    role: "work"
    profession: "fierar"
```

### 5. Creeaza `WorldNode` pentru spawn si ancore

Node-urile trebuie create dupa place-uri, pentru ca pot fi scoped sub place.

Pentru o casa cu familie:

```yml
nodes:
  spawn_ion:
    type: "npc_spawn"
    x: 123.5
    y: 65
    z: 134.5
    radius: 1.5
    metadata:
      role: "resident_spawn"
      npc_ref: "npc_ion"
      family_role: "father"

  spawn_maria:
    type: "npc_spawn"
    x: 125.5
    y: 65
    z: 134.5
    radius: 1.5
    metadata:
      role: "resident_spawn"
      npc_ref: "npc_maria"
      family_role: "mother"

  bed_1:
    type: "interaction"
    x: 122.5
    y: 65
    z: 136.5
    radius: 1.0
    metadata:
      role: "home"
      anchor: "bed"
      resident: "npc_ion"

  bed_2:
    type: "interaction"
    x: 124.5
    y: 65
    z: 136.5
    radius: 1.0
    metadata:
      role: "home"
      anchor: "bed"
      resident: "npc_maria"
```

Stare implementata initial:

- `ensureSimulationAnchors(...)` cauta mai intai `WorldPlace` potrivit pentru `home`, `work` sau `social`
- daca place-ul are node semantic potrivit, ancora se pune pe node, nu pe centrul cladirii
- daca nu exista node potrivit, ancora cade inapoi pe centrul place-ului
- daca nu exista mapping, sistemul foloseste fallback vanilla: pat apropiat, workstation apropiat sau clopot apropiat

Tipuri de node acceptate pentru ancore:

```text
home: bed, home, npc_spawn, entrance, interaction
work: workstation, work, npc_spawn, interaction
social: social, meeting_point, interaction, npc_spawn
```

Node-urile pot fi identificate si prin metadata, de exemplu `role`, `purpose`, `anchor`, `type` sau `kind`.

## Alocarea mai multor NPC-uri la aceeasi casa

Pentru a simula o familie, generatorul trebuie sa creeze o alocare explicita:

```text
HouseAllocation
  placeId: sat_noodle:casa_popescu
  familyId: fam_popescu_001
  primaryOwner: npc_ion
  residents:
    npc_ion: father
    npc_maria: mother
    npc_ana: daughter
```

Reguli:

- o casa poate avea un singur `owner_npc_id`
- toti locuitorii se pun in `metadata.residents`
- fiecare locuitor trebuie sa aiba node de spawn propriu sau pat propriu
- toti locuitorii trebuie spawnati in interiorul aceluiasi `WorldPlace`
- familia reala se leaga dupa ce NPC-urile au `databaseId`

Nu folosi `owner_npc_id` pentru toti membrii. Campul nu suporta lista.

## Ordinea de spawn pentru NPC-uri

Pentru fiecare casa:

1. citeste `HouseAllocation`
2. spawneaza intai adultii/proprietarii
3. salveaza fiecare NPC ca sa primeasca `databaseId`
4. spawneaza copiii sau rudele care trebuie sa existe fizic
5. salveaza fiecare NPC
6. creeaza relatiile din `npc_family`
7. verifica `homeAnchor` si `workAnchor`

Ordine recomandata intr-o familie:

```text
owner/adult principal
spouse/adult secundar
children
siblings/parents spawnati fizic, daca exista
relatii textuale fara NPC fizic
```

Motivul este simplu: adultul principal poate fi folosit ca `owner_npc_id`, iar restul membrilor se leaga de el dupa ce exista in DB.

## Integrare cu `FamilyManager`

Pentru familie fizica, nu te baza doar pe `generateFamily(...)`.

Flux recomandat:

1. creeaza toate NPC-urile reale ale familiei
2. salveaza-le, ca sa aiba `databaseId`
3. foloseste `linkNPCsAsFamily(npc1, npc2, relation1to2, relation2to1)`
4. foloseste `addFamilyMember(...)` doar pentru rude care nu sunt NPC-uri fizice

Exemplu conceptual:

```java
familyManager.linkNPCsAsFamily(ion, maria, "spouse", "spouse");
familyManager.linkNPCsAsFamily(ion, ana, "daughter", "father");
familyManager.linkNPCsAsFamily(maria, ana, "daughter", "mother");
```

Pentru generatorul de sate, este mai sigur ca familia automata sa fie controlata de generator. Altfel `family.auto_generate` poate crea rude textuale in plus care nu locuiesc efectiv in casa.

## Reguli pentru atasarea corecta la cladire

Un NPC este atasat corect la casa daca:

- are `homeAnchor` in `npc_profiles.profile_data.owned_locations.home`
- ancora este in interiorul `WorldPlace` de tip `house`
- casa are `metadata.residents` care include NPC-ul
- casa are `owner_npc_id` pentru proprietarul principal
- NPC-ul a fost spawnat initial in interiorul casei sau langa patul lui

Un NPC este atasat corect la munca daca:

- are `workAnchor` in `npc_profiles.profile_data.owned_locations.work`
- locul de munca are `type` compatibil cu ocupatia sau tag `workplace`
- locul de munca are `metadata.role: work`
- exista cel putin un node de tip `interaction`

## Validari obligatorii inainte de spawn

Inainte de spawn:

- regiunea exista si contine toate cladirile
- fiecare casa are `type: house` sau tag `home`
- fiecare casa are `max_residents`
- numarul de locuitori nu depaseste `max_residents`
- fiecare NPC are un node `npc_spawn` in casa lui
- fiecare loc de munca are `type` sau tag compatibil
- nu exista doua case cu acelasi `owner_npc_id`
- nu exista doua NPC-uri cu acelasi nume logic
- toate node-urile sunt in bounds-ul place-ului sau regiunii

Daca una dintre aceste verificari esueaza, generatorul trebuie sa opreasca spawn-ul si sa raporteze eroarea.

## Validari dupa spawn

Dupa generare, ruleaza:

```text
/ainpc audit world
/ainpc audit npc
/ainpc debugdump all
/ainpc world places <regionId>
/ainpc world place info <placeId>
/ainpc info <npc>
```

Verifica:

- mapping-ul nu este gol
- fiecare casa are rezidenti
- fiecare NPC are `homeAnchor`
- fiecare NPC cu meserie are `workAnchor`
- familia are randuri reciproce in `npc_family`
- NPC-urile sunt in chunk-uri incarcate
- dupa restart, NPC-urile se restaureaza fara duplicate

## Anti-patterns

Evita urmatoarele:

- spawn NPC inainte de `WorldPlace`
- case fara tag `home`
- locuri de munca fara tag `workplace` sau `metadata.role: work`
- mai multi proprietari pusi in `owner_npc_id`
- familie fizica reprezentata doar prin nume textuale in `npc_family`
- reliance pe cel mai apropiat pat cand exista mai multe case apropiate
- update de mapping dupa spawn fara recalcularea ancorelor

## Faze de implementare

Implementarea trebuie facuta incremental. Nu incerca sa rezolvi generator complet, familie completa, rutine si timeline in acelasi pas.

### Faza 0: Node-anchor binding

Stare: implementata initial.

Ce exista:

- `WorldNodeType` are tipuri semantice pentru `bed`, `workstation`, `home`, `work`, `social`, `meeting_point`, `entrance`
- `NPCManager.ensureSimulationAnchors(...)` cauta intai mapping `WorldPlace`
- daca place-ul are node semantic compatibil, ancora NPC-ului se pune pe node
- fallback-ul ramane compatibil cu vanilla: pat, workstation sau clopot apropiat

Scop:

- NPC-ul nu mai este legat doar de centrul cladirii cand exista node-uri corecte
- mapping-ul `Region -> Place -> Node` incepe sa controleze locatia reala a NPC-ului

Ce lipseste dupa aceasta faza:

- casa nu are inca rezidenti multipli modelati structural
- NPC-ul nu are inca `homePlaceId/workPlaceId/socialPlaceId`
- generatorul nu produce inca automat `HouseAllocation`

### Faza 1: HouseAllocation

Aceasta este urmatoarea faza obligatorie.

Scop:

- o casa poate primi mai multi NPC
- casa poate simula o familie sau un household
- fiecare rezident are pat/spawn node propriu

Model minim:

```text
HouseAllocation
  placeId
  familyId
  maxResidents
  residentPlans
```

```text
ResidentPlan
  npcKey
  relationRole
  occupation
  homeNodeId
  bedNodeId
  spawnNodeId
```

Metadata temporara pe `WorldPlace`:

```yaml
metadata:
  role: "home"
  family_id: "family_popescu_001"
  max_residents: "4"
  residents: "npc_ion,npc_maria,npc_andrei"
```

Reguli:

- `owner_npc_id` ramane un singur proprietar principal
- toti locuitorii se pun in `metadata.residents`
- fiecare rezident trebuie sa aiba `bed` sau `npc_spawn`
- daca nu exista destule paturi/node-uri, nu se spawneaza familia incompleta

Rezultat asteptat:

- o casa poate gazdui 2-4 NPC fara sa fie confundata cu mai multe case
- familia are baza fizica in lume inainte de relatii sociale

### Faza 2: SpawnPlan si NpcSpawnOrchestrator

Stare: implementata initial.

Scop:

- NPC-urile nu mai sunt create direct din comanda/generator fara context
- spawn-ul se face numai dupa ce exista regiune, place-uri si node-uri

Pipeline:

```text
SettlementPlan
-> BuildingPlan
-> WorldRegion
-> WorldPlace
-> WorldNode
-> HouseAllocation
-> NpcSpawnPlan
-> NPC spawn
-> save NPC
-> family bind
-> audit
```

Model minim:

```text
NpcSpawnPlan
  npcKey
  name
  age
  gender
  occupation
  homePlaceId
  workPlaceId
  socialPlaceId
  spawnNodeId
  homeNodeId
  workNodeId
  familyId
```

Reguli:

- `NpcSpawnPlan` se valideaza inainte de spawn
- spawn-ul foloseste `spawnNodeId` daca exista
- `homeAnchor`, `workAnchor`, `socialAnchor` se seteaza din node-uri, nu prin cautare random
- dupa spawn si save, NPC-ul primeste `databaseId`, abia apoi se fac relatii de familie

Clase implementate initial:

```text
ro.ainpc.spawn.NpcSpawnPlan
ro.ainpc.spawn.ResolvedNpcSpawnPlan
ro.ainpc.spawn.NpcSpawnResult
ro.ainpc.spawn.NpcSpawnOrchestrator
```

Integrare curenta:

- `AINPCPlugin` initializeaza `NpcSpawnOrchestrator`
- `NPCManager.createNPCFromPlan(...)` creeaza NPC-ul dintr-un plan rezolvat
- orchestratorul valideaza `spawnNodeId`, `homeNodeId/homePlaceId`, `workNodeId/workPlaceId` si `socialNodeId/socialPlaceId`
- pentru ocupatii reale, work anchor este obligatoriu
- daca lipseste node semantic dar exista place, ancora poate cadea pe centrul place-ului cu warning
- daca lipseste lumea node-ului, spawn-ul este refuzat

Exemplu conceptual:

```java
NpcSpawnPlan plan = NpcSpawnPlan.builder("npc_ion", "Ion")
    .occupation("fierar")
    .age(42)
    .gender("male")
    .homePlaceId("sat_01:casa_popescu")
    .workPlaceId("sat_01:fierarie")
    .spawnNodeId("sat_01:casa_popescu:spawn_ion")
    .homeNodeId("sat_01:casa_popescu:bed_ion")
    .workNodeId("sat_01:fierarie:anvil")
    .familyId("family_popescu_001")
    .build();

NpcSpawnResult result = plugin.getNpcSpawnOrchestrator().spawn(plan);
```

Rezultat asteptat:

- nu mai apar NPC-uri spawnate inainte sa existe casa si node-uri
- dupa restart, NPC-ul se restaureaza pe baza de date persistenta, nu pe presupuneri

Ce mai lipseste dupa implementarea initiala:

- generatorul real nu produce inca automat `NpcSpawnPlan`
- `familyId` este transportat in plan, dar relatiile reale se fac in Faza 3
- `homePlaceId/workPlaceId/socialPlaceId` nu sunt inca persistate intr-un tabel dedicat
- nu exista inca o comanda admin dedicata pentru testarea manuala a unui `NpcSpawnPlan`

### Faza 3: Family binding dupa spawn

Stare: implementata initial.

Scop:

- familia se leaga dupa ce toti membrii au fost creati si salvati
- `npc_family.related_npc_id` poate fi completat corect

Ordine:

```text
spawn tata
save tata -> databaseId
spawn mama
save mama -> databaseId
spawn copil
save copil -> databaseId
creeaza relatii reciproce in npc_family
salveaza profilurile cu family context
```

Reguli:

- nu crea familie completa doar ca text in backstory
- nu lega relatii catre NPC-uri care nu au `databaseId`
- fiecare relatie importanta trebuie sa fie reciproca
- nu duplica aceeasi relatie daca binding-ul este rulat de doua ori

Clase implementate initial:

```text
ro.ainpc.spawn.FamilyBindingPlan
ro.ainpc.spawn.FamilyBindingResult
FamilyManager.bindSpawnedFamily(...)
NpcSpawnOrchestrator.bindFamily(...)
```

Flux implementat:

```text
NpcSpawnOrchestrator.spawn(plan_tata) -> NPC cu databaseId
NpcSpawnOrchestrator.spawn(plan_mama) -> NPC cu databaseId
NpcSpawnOrchestrator.spawn(plan_copil) -> NPC cu databaseId
NpcSpawnOrchestrator.bindFamily(familyPlan)
```

Exemplu conceptual:

```java
FamilyBindingPlan familyPlan = new FamilyBindingPlan(
    "family_popescu_001",
    List.of(
        FamilyBindingPlan.member("npc_ion", ionNpc, "father"),
        FamilyBindingPlan.member("npc_maria", mariaNpc, "mother"),
        FamilyBindingPlan.member("npc_andrei", andreiNpc, "son")
    )
);

FamilyBindingResult result = plugin.getNpcSpawnOrchestrator().bindFamily(familyPlan);
```

Relatii inferate automat:

```text
father + mother -> spouse / spouse
father + son -> son / father
mother + son -> son / mother
son + daughter -> sister / brother
brother + sister -> sister / brother
rol necunoscut -> relative / relative cu warning
```

Rezultat asteptat:

- familia devine interogabila din DB
- AI-ul poate explica familia fara sa inventeze date
- rutinele pot folosi `familyId/householdId`

Ce mai lipseste dupa implementarea initiala:

- generatorul real trebuie sa construiasca automat `FamilyBindingPlan` din `HouseAllocation`
- `familyId` nu este inca persistat intr-un tabel dedicat
- nu exista inca model `household_id`
- auditul initial exista in Faza 4, dar nu exista inca un `AuditService` dedicat si reutilizabil

### Faza 4: Audit pentru spawn order

Stare: implementata initial.

Scop:

- server admin-ul trebuie sa poata vedea rapid ce este gresit
- generatorul trebuie sa opreasca spawn-ul daca datele sunt invalide

Validari implementate:

- fiecare `house` are `max_residents` sau `capacity` valid cand este folosit pentru familie
- `residents <= max_residents`
- fiecare resident listat in metadata poate fi rezolvat ca NPC incarcat
- fiecare resident are `homeAnchor` in interiorul casei asignate
- fiecare casa cu rezidenti are cel putin un node semantic `bed`, `home` sau `npc_spawn`
- fiecare NPC cu ocupatie are `workAnchor`
- fiecare `workAnchor` este in `mappedWorkplaceId`, daca NPC-ul are loc de munca mapat
- `homeAnchor` nu este legat accidental de un place non-house
- relatiile din familia DB sunt reciproce
- randurile duplicate exacte din `npc_family` sunt raportate
- `debugdump all` include verificari de baza pentru case, rezidenti si `homeAnchor`

Comenzi afectate:

```text
/ainpc audit spawn
/ainpc audit all
/ainpc audit world
/ainpc audit npc
/ainpc debugdump all
```

Rezultat asteptat:

- problemele de ordine sunt vizibile imediat
- nu trebuie verificat manual in DB daca un NPC are casa corecta

Ce mai lipseste dupa implementarea initiala:

- extragerea logicii duplicate intr-un `SpawnOrderAuditService`
- audit pentru viitorul model persistent `households/household_residents`
- comanda de smoke test pentru `NpcSpawnPlan` si `FamilyBindingPlan` cu `dry_run`
- audit tranzactional care poate spune exact ce trebuie curatat daca spawn-ul esueaza la mijloc

### Faza 5: VanillaVillageScanner si SemanticMapper

Stare: implementata initial.

Scop:

- nu refacem worldgen-ul Minecraft de la zero
- detectam satul vanilla existent si il transformam in mapping AINPC

Pipeline:

```text
VanillaVillageScanner
-> SemanticMapper
-> VillageGapAnalyzer
-> VillagePatchPlanner
-> NativePatchBuilder
-> NpcBinder
```

Ce detectam:

- clopot
- paturi
- workstation-uri
- case candidate
- usi pentru `entrance`
- puncte sociale
- ferme

Implementare initiala:

- `VanillaVillageScanner` scaneaza zona din jurul adminului
- `SemanticVillageMapper` transforma semnalele vanilla in `WorldRegion`, `WorldPlace` si `WorldNode`
- comanda ruleaza dry-run implicit, fara sa modifice lumea
- importul este explicit si creeaza mapping runtime in `WorldAdminService`
- persistenta se face separat prin `/ainpc world save`

Ce produce:

- `WorldRegion` pentru sat
- `WorldPlace` pentru case si locuri de munca
- `WorldPlace` pentru ferme detectate cand exista destul farmland
- `WorldNode` pentru paturi, `home`, intrari, workstation-uri, `work` si puncte sociale
- lista de lipsuri pentru patch-uri mici

Comenzi:

```text
/ainpc world scan village [radius]
/ainpc world scan village [radius] import [regionId]
/ainpc world save
```

Reguli:

- scannerul nu construieste cladiri
- scannerul nu inlocuieste sate vanilla
- mapperul creeaza doar mapping AINPC peste satul existent
- importul nu spawneaza NPC-uri automat
- daca nu exista clopot, se creeaza un `meeting_point` fallback si se raporteaza warning
- daca nu exista paturi, nu se creeaza case

Rezultat asteptat:

- AINPC foloseste satul vanilla ca baza
- construieste doar ce lipseste, nu inlocuieste tot satul
- spawn-ul NPC poate folosi ulterior casele si nodurile detectate

Ce mai lipseste dupa implementarea initiala:

- detectie mai buna a conturului real al caselor, nu doar cluster pe paturi
- detectie drumuri si separare mai buna pentru piata/taverna
- `VillageGapAnalyzer` care spune explicit ce lipseste in sat
- `VillagePatchPlanner` si `NativePatchBuilder` pentru patch-uri mici, nu regenerare completa
- integrare directa cu `HouseAllocation` si `NpcSpawnPlan`

### Faza 6: Routine integration

Stare: implementata initial.

Scop:

- NPC-ul nu sta aleatoriu
- `home/work/social` devin input pentru rutina zilnica

Ordine recomandata:

```text
TimeService
-> RoutineEngine
-> RoutineAssignment
-> Household/Family binding
-> Quest hooks
-> Timeline overrides
```

Reguli:

- rutina foloseste `homeAnchor`, `workAnchor`, `socialAnchor`
- seara NPC-ul revine la casa lui
- ziua NPC-ul merge la munca
- evenimentele sociale folosesc `socialAnchor` si node-uri `meeting_point`
- timeline-ul poate modifica temporar rutina, dar nu o inlocuieste

Implementare initiala:

- `RoutineEngine` decide slotul curent: `HOME`, `WORK`, `SOCIAL` sau `IDLE`
- `RoutineService` ruleaza periodic rutina pentru NPC-urile spawnate
- rutina seteaza `plannedRoutineActivity`, `currentGoal` si `NPCState`
- noaptea NPC-ul este trimis la `homeAnchor`
- ziua NPC-ul este trimis la `workAnchor`, daca exista
- daca nevoia sociala este scazuta si nu este noapte, NPC-ul poate merge la `socialAnchor`
- daca lipseste `workAnchor`, rutina face fallback la `socialAnchor` sau `homeAnchor`
- NPC-urile aflate in interactiuni importante nu sunt intrerupte de rutina
- mutarea este configurabila prin `routine.teleport_enabled`

Comenzi:

```text
/ainpc routine tick
/ainpc routine status [numeNpc]
```

Configuratie:

```yaml
routine:
  enabled: true
  tick_seconds: 60
  teleport_enabled: true
  arrival_radius: 5.5
  min_teleport_distance: 8.0
```

Rezultat asteptat:

- NPC-urile au motiv clar pentru locatia lor
- evenimentele ambientale se pot produce controlat, cu sansa si cooldown

Ce mai lipseste dupa implementarea initiala:

- pathfinding real sau pasi intermediari, nu doar teleport controlat
- evenimente ambientale probabilistice intre NPC-uri cand ajung la acelasi `socialAnchor`
- rutina pe roluri/familie: copil, parinte, sot/sotie, batran
- override temporar din quest/timeline
- persistenta dedicata pentru `RoutineAssignment` daca trebuie audit istoric

### Faza 7: Model persistent dedicat

Scop:

- metadata string pe `WorldPlace` este suficienta pentru MVP, dar nu pentru sistem matur
- household si rezidentii trebuie mutate in model dedicat

Tabele sau modele recomandate:

```text
households
household_residents
npc_world_bindings
```

`npc_world_bindings` poate contine:

```text
npc_id
home_place_id
work_place_id
social_place_id
home_node_id
work_node_id
social_node_id
household_id
family_id
```

Rezultat asteptat:

- cautarile dupa place/node nu se repeta inutil
- familia, casa si rutina au date persistente clare
- mapping-ul poate fi refacut sau migrat fara sa pierzi legaturile NPC

## Verificare faze suplimentare

Pentru MVP-ul de spawn order, fazele 0-7 sunt suficiente. Ele acopera ordinea corecta, ancorele semantice, spawn-ul controlat, legarea familiei, auditul initial, scanarea vanilla, rutina si modelul persistent recomandat.

Pentru productie, mai sunt recomandate faze suplimentare. Acestea nu sunt obligatorii ca sa pornesti sistemul, dar reduc riscul de date corupte si fac debugging-ul mai simplu.

### Faza 8: Smoke test si dry-run admin

Scop:

- admin-ul poate valida un plan fara sa spawneze entitati reale
- generatorul poate testa rapid `NpcSpawnPlan` si `FamilyBindingPlan`
- erorile de mapping apar inainte sa se modifice lumea sau DB-ul

Comenzi recomandate:

```text
/ainpc spawnplan validate <plan_id>
/ainpc spawnplan dryrun <plan_id>
/ainpc family validate <family_id>
```

Rezultat asteptat:

- planurile invalide sunt blocate inainte de spawn
- testarea satelor devine repetabila

### Faza 9: Migration si backfill pentru date existente

Scop:

- NPC-urile si casele deja create trebuie convertite catre modelul persistent nou
- `metadata.residents` trebuie mutat in `household_residents`
- `owner_npc_id`, `mappedHomePlaceId` si ancorele trebuie mutate in `npc_world_bindings`

Rezultat asteptat:

- update-ul pluginului nu rupe satele existente
- datele vechi pot fi curatate treptat, fara reset complet

### Faza 10: Rollback si tranzactii de spawn

Scop:

- daca generarea casei, spawn-ul NPC sau family bind esueaza, sistemul stie ce trebuie curatat
- entitatile spawnate partial nu raman in lume fara casa, familie sau rutina

Regula:

```text
orice spawn batch trebuie sa aiba create -> verify -> commit sau rollback
```

Rezultat asteptat:

- satul nu ramane intr-o stare partiala
- bug-urile de spawn sunt mai usor de reparat

## Concluzie

Ordinea corecta este `plan -> constructie -> region -> place -> node -> alocare -> spawn NPC -> save -> family bind -> audit`.

Urmatorul pas concret ramane finalizarea `HouseAllocation` si conectarea lui la `NpcSpawnPlan`. Pentru MVP, fazele 0-7 sunt suficiente. Pentru productie, adauga fazele 8-10: dry-run/smoke test, migration/backfill si rollback tranzactional.
