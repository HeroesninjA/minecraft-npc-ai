# Generare Populatie Narativa

Actualizat: 2026-05-04

Status: document canonic initial pentru generatorul narativ de populatie pe regiune. Acest document descrie designul si contractele recomandate; nu inseamna ca generatorul este implementat complet in cod.

## Scop

Generatorul actual poate produce initial `HouseAllocation` determinist din mapping. Urmatorul pas este ca populatia generata sa nu fie doar o lista de NPC-uri valide tehnic, ci o comunitate credibila:

- nume potrivite temei
- roluri sociale clare
- profesii legate de locurile de munca mapate
- familii simple si coerente
- rezidenti distribuiti corect pe case
- legaturi home/work/social stabile
- motive pentru questuri si story locale

Obiectivul acestui document este sa defineasca stratul dintre mapping/spawn si simularea de sat:

```text
WorldRegion + WorldPlace + WorldNode
-> PopulationPlan
-> HouseholdPlan
-> HouseAllocation
-> NpcSpawnPlan
```

Generatorul narativ trebuie sa poata rula in dry-run si sa explice de ce a ales fiecare NPC, casa, munca si familie.

## Ce problema rezolva

Plannerul curent este util pentru validare tehnica, dar populatia rezultata este inca prea mecanica:

- numele pot fi generice
- rolurile nu au mereu sens social
- familiile sunt minime
- meseriile sunt alese mai mult din place type decat din nevoile satului
- nu exista un plan narativ inspectabil inainte de spawn

Fara generator narativ, satul poate fi valid in audit, dar slab ca gameplay. Jucatorul vede NPC-uri, dar nu vede o comunitate.

## Principii

### 1. Mapping-ul este sursa de adevar

Generatorul nu inventeaza case sau locuri de munca care nu exista in mapping.

Poate propune lipsuri:

```text
nu exista taverna pentru rolul hangiu
nu exista destule case pentru 8 rezidenti
nu exista social place clar
```

Dar nu trebuie sa le creeze direct. Completarea lumii tine de `VillagePatchPlanner` sau de generare worldgen.

### 2. Plan inainte de spawn

Generatorul produce intai un plan inspectabil:

```text
PopulationPlan
-> validation
-> dry-run
-> eventual commit/spawn in faza separata
```

Nu se creeaza NPC-uri in momentul in care se aleg numele si familiile.

### 3. Determinism controlat

Pentru acelasi input si seed, planul trebuie sa fie acelasi.

Surse de seed recomandate:

- `regionId`
- pack/theme id
- optional seed din comanda admin
- versiunea regulilor de generatie

Nu folosi timestamp-uri pentru ID-uri narative stabile.

### 4. Compatibilitate cu spawn order

Generatorul narativ nu sare peste `HouseAllocation`.

Fluxul ramane:

```text
PopulationPlan
-> HouseholdPlan
-> HouseAllocation
-> NpcSpawnPlan
-> spawn
```

Astfel, validarea de spawn, familia si bind-urile raman compatibile cu ce exista deja.

### 5. AI optional, nu autoritate

AI-ul poate propune nume, backstory sau descrieri, dar regulile locale valideaza tot:

- casa exista
- locul de munca exista
- profesia este permisa
- familia are structura valida
- numele nu produce duplicat critic
- planul nu depaseste capacitatea casei

Pentru MVP, generatorul trebuie sa functioneze fara AI.

## Input-uri

`PopulationPlan` poate fi generat direct pentru o regiune sau ca parte dintr-un `SettlementPlan`. Pentru planul complet de sat, vezi `settlement-plan.md`.

### Input minim

```text
regionId
targetPopulation optional
themeId optional
seed optional
```

Daca `targetPopulation` lipseste, se calculeaza din mapping:

```text
targetPopulation = min(totalBedCapacity, workplaceCapacity + householdFallback)
```

Pentru demo, se poate folosi o limita mica:

```text
targetPopulation = 6-10
```

### Date citite din mapping

Generatorul citeste:

- regiunea
- casele
- bed/home/spawn/entrance nodes
- work places
- work nodes
- social places
- social nodes
- tag-uri de tema
- metadata de capacitate, daca exista

### Date citite din content pack

Pack-ul poate furniza:

- liste de nume
- nume de familie
- roluri disponibile
- reguli de familie
- distributie de profesii
- restrictii de gen/varsta, daca proiectul decide sa le modeleze
- backstory snippets
- preferinte de locuri sociale

## Output-uri

### PopulationPlan

Model conceptual:

```text
PopulationPlan
  planId
  regionId
  themeId
  seed
  generatedAt
  targetPopulation
  households
  unassignedWorkplaces
  warnings
  validationReport
```

### HouseholdPlan

```text
HouseholdPlan
  householdKey
  familyId
  homePlaceId
  capacity
  primaryOwnerNpcKey
  residents
  tags
```

### ResidentPlan

```text
ResidentPlan
  npcKey
  displayName
  familyName
  relationRole
  socialRole
  profession
  ageGroup
  homePlaceId
  bedNodeId
  spawnNodeId
  workPlaceId
  workNodeId
  socialPlaceId
  socialNodeId
  routineProfile
  questRole
  personalitySeed
  backstorySeed
```

`npcKey` trebuie sa fie stabil in plan:

```text
demo_sat:popescu:ion
demo_sat:fierar_matei
demo_sat:gardian_ana
```

Nu trebuie sa fie ID DB final. ID-ul DB apare abia dupa spawn.

## Reguli de generatie

### Capacitate case

Ordine recomandata:

1. citeste `maxResidents` din metadata, daca exista
2. altfel calculeaza din numarul de bed nodes
3. altfel foloseste fallback dupa `PlaceType`

Fallback:

```text
house small: 1-2
house medium: 2-3
house large/shared: 3-5
```

Reguli:

- nu depasi capacitatea casei
- nu pune familie de 4 intr-o casa cu un singur pat
- nu crea household fara homePlaceId
- nu crea resident fara spawnNodeId sau fallback explicit

### Distributie profesii

Profesii initiale dupa places:

```text
FORGE -> fierar
FARM -> fermier
MARKET -> negustor
SHOP -> negustor / mestesugar
TAVERN -> hangiu
CAMP -> gardian / soldat
CUSTOM + tags -> dupa tag-uri
```

Reguli:

- fiecare workplace important primeste cel putin un NPC, daca exista populatie disponibila
- profesiile reale trebuie sa aiba `workPlaceId`
- copiii, batranii sau vizitatorii pot sa nu aiba work place
- nu atribui doi owneri principali aceluiasi loc de munca fara metadata care permite asta

### Structuri de familie

Pentru MVP, familiile trebuie sa fie simple.

Tipuri recomandate:

```text
single_adult
couple
parent_child
two_parents_child
siblings
elder_with_adult_child
unrelated_roommates
```

Reguli:

- nu crea arbori genealogici adanci in prima faza
- nu crea relatii ambigue fara `relationRole`
- pentru case cu 2+ rezidenti, prefera o relatie explicita
- `familyId` trebuie sa fie stabil in plan

Exemplu:

```text
household: demo_sat:casa_01
familyId: family_popescu_01
residents:
  - Ion Popescu, father, fierar
  - Ana Popescu, mother, negustor
  - Mara Popescu, child
```

### Roluri sociale

Rolul social este diferit de profesie.

Exemple:

```text
profession=fierar, socialRole=craftsman
profession=fermier, socialRole=provider
profession=guard, socialRole=protector
profession=hangiu, socialRole=gossip_hub
profession=preot, socialRole=moral_authority
```

Rolurile sociale ajuta:

- dialogul
- reputatia
- quest availability
- evenimentele sociale
- StoryContext

### Quest roles

Un NPC poate avea rol special pentru questuri:

```text
none
quest_giver
quest_target
informant
witness
merchant_reward
trainer
```

Reguli:

- nu face toti NPC-ii `quest_giver`
- pentru demo, 2-3 quest givers sunt suficienti
- quest role trebuie sa aiba sens cu profesia si locatia

Exemple:

- fierarul -> `quest_giver` pentru lipsa de fier
- hangiul -> `informant`
- gardianul -> `quest_giver` pentru pericol
- fermierul -> `quest_target` sau `quest_giver` pentru hrana

## Validari obligatorii

Planul trebuie sa refuze commit/spawn daca are erori critice.

Erori:

- `regionId` lipseste
- nu exista case valide
- rezident fara `homePlaceId`
- rezident fara spawn node sau fallback
- profesie reala fara work place
- `residents > capacity`
- doua household-uri folosesc aceeasi casa exclusiv
- doua NPC-uri au acelasi `npcKey`
- `workPlaceId` nu exista
- `socialPlaceId` nu exista cand este obligatoriu

Warning-uri:

- prea putine locuri de munca
- prea putine social places
- nume similar cu NPC existent
- household fara familie explicita
- prea multi NPC fara profesie
- quest giver fara node de quest/social apropiat

## Integrare cu HouseAllocation

`HouseAllocation` ramane contractul de intrare pentru spawn-ul curent.

Conversia recomandata:

```text
HouseholdPlan.homePlaceId -> HouseAllocation.placeId
HouseholdPlan.familyId -> HouseAllocation.familyId
ResidentPlan -> HouseAllocation.ResidentPlan
ResidentPlan.profession -> occupation
ResidentPlan.relationRole -> family role
ResidentPlan.workPlaceId/workNodeId -> work anchor
ResidentPlan.socialPlaceId/socialNodeId -> social anchor
```

Regula:

- `PopulationPlan` poate contine informatie narativa bogata
- `HouseAllocation` contine doar ce trebuie pentru spawn si bind
- informatia narativa se pastreaza pentru profil/backstory/states in faza separata

## Integrare cu simularea de sat

Generatorul narativ este primul pas pentru `SettlementSimulation`.

Pentru persistenta household-urilor rezultate, vezi `households-persistente.md`.

El trebuie sa produca date care pot alimenta:

- `PopulationState`
- `HouseholdState`
- `WorkState`
- reputatie initiala
- resurse initiale
- evenimente posibile

Exemple:

```text
Sat cu 1 ferma si 6 rezidenti
-> food production mica
-> risc de food_shortage daca populatia creste
```

```text
Sat fara gardian
-> securityLevel mai scazut
-> quest/event de banditi devine posibil
```

## Integrare cu story si questuri

Planul poate expune ancore narative:

```text
blacksmith_npc
tavern_informant
guard_post_owner
farm_family
market_merchant
```

Acestea nu inlocuiesc `quest_anchor_bindings`. Ele ajuta la alegerea template-urilor si la validarea questurilor.

Exemplu:

```text
Q_IRON_SHORTAGE necesita:
- NPC cu profession=fierar
- workPlace type=forge
- node workstation/inspect

Generatorul marcheaza:
- Matei Fieraru -> blacksmith_npc
- demo_sat:fierarie -> blacksmith_workplace
```

## Comenzi recomandate

Pentru faza dry-run:

```text
/ainpc population plan <regionId> [targetPopulation] [seed]
/ainpc population inspect <planId>
/ainpc population validate <planId>
/ainpc population discard <planId>
```

Pentru faza de integrare cu spawn:

```text
/ainpc population dryrun <planId>
/ainpc population commit <planId>
```

`commit` nu trebuie sa creeze cladiri. In faza initiala poate doar sa converteasca planul in `HouseAllocation`/spawn plan sau sa pregateasca date persistente, in functie de implementare.

## Exemplu de plan

```yaml
population_plans:
  demo_sat_pop_01:
    region_id: demo_sat
    theme_id: medieval
    seed: demo_sat:medieval:01
    target_population: 7
    households:
      demo_sat:casa_01:
        family_id: family_popescu_01
        capacity: 3
        residents:
          - npc_key: demo_sat:ion_popescu
            name: Ion Popescu
            relation_role: father
            social_role: craftsman
            profession: fierar
            home_place_id: demo_sat:casa_01
            work_place_id: demo_sat:fierarie
            social_place_id: demo_sat:piata
            quest_role: quest_giver
          - npc_key: demo_sat:ana_popescu
            name: Ana Popescu
            relation_role: mother
            social_role: merchant
            profession: negustor
            home_place_id: demo_sat:casa_01
            work_place_id: demo_sat:piata
            social_place_id: demo_sat:taverna
          - npc_key: demo_sat:mara_popescu
            name: Mara Popescu
            relation_role: daughter
            social_role: child
            profession: none
            home_place_id: demo_sat:casa_01
            social_place_id: demo_sat:piata
```

## Faze mici

### P0: Contract documentat

Livrabile:

- documentul de fata
- campuri minime pentru `PopulationPlan`
- reguli de validare

Nu necesita server Paper.

### P1: Generator dry-run determinist

Livrabile:

- genereaza `PopulationPlan` in memorie
- nu scrie DB
- nu spawneaza NPC-uri
- afiseaza summary si warnings

### P2: Conversie catre HouseAllocation

Livrabile:

- `PopulationPlan -> List<HouseAllocation>`
- validare comuna cu spawn planner
- comparatie intre plan narativ si plan tehnic

### P3: Plan serializat

Livrabile:

- salvare temporara a planului
- inspect/validate/discard
- ID-uri stabile

### P4: Integrare profil NPC

Livrabile:

- backstory seed
- social role
- quest role
- routine profile
- metadata de profil controlata

### P5: Integrare simulare

Livrabile:

- initializare `PopulationState`
- initializare `HouseholdState`
- initializare `WorkState`
- semnale pentru resurse si evenimente

## Ce trebuie evitat

- Nu genera populatie fara mapping.
- Nu spawna direct din generatorul narativ.
- Nu crea familii complexe inainte de household persistent.
- Nu ignora contractul din `households-persistente.md` cand planul devine date persistente.
- Nu amesteca generarea de cladiri cu generarea de NPC-uri.
- Nu lasa AI-ul sa aleaga ID-uri finale fara validare.
- Nu folosi nume duplicate fara strategie clara.
- Nu transforma `HouseAllocation` in container pentru tot story-ul NPC-ului.

## Definitia de gata pentru MVP

Generatorul narativ este suficient pentru MVP cand:

- poate produce un plan de 6-10 NPC-uri pentru `demo_sat`
- fiecare NPC are nume, rol social, home si social place
- fiecare profesie reala are work place valid
- casele nu depasesc capacitatea
- familiile simple sunt coerente
- planul poate fi convertit in `HouseAllocation`
- planul poate fi inspectat fara spawn

Abia dupa acest punct merita legate resursele, reputatia si evenimentele peste populatia generata.
