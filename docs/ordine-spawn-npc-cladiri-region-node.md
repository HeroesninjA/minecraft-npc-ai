# Ordine spawn NPC, cladiri, regiuni si node-uri - v2

Actualizat: 2026-05-03

Versiune: v2

Arhiva v1:

- `docs/arhiva/ordine-spawn-npc-cladiri-region-node-v1.md`

## Scop v2

Acest document nu mai repeta tot istoricul fazelor 0-10. Versiunea v1 ramane arhivata pentru detalii despre implementarea initiala.

v2 stabileste ce trebuie facut in continuare dupa nucleul MVP de spawn order:

- smoke test Paper pentru mapping demo, household planner si settlement planner
- generator narativ care produce automat `HouseAllocation`
- planuri serializate si comenzi admin pentru validate/dry-run
- model persistent dedicat pentru household si world bindings
- migration/backfill pentru date vechi
- rollback mai sigur peste DB, entitati si viitoare constructii fizice
- integrare cu scannerul vanilla, gap analyzer si patch planner
- rutina/familie/timeline peste date persistente clare

## Status curent scurt

Implementat initial:

- `WorldRegion`, `WorldPlace`, `WorldNode`
- `HouseAllocation` si `HouseAllocation.ResidentPlan`
- conversie `HouseAllocation -> NpcSpawnPlan`
- `HouseAllocationValidator`
- `NpcSpawnOrchestrator.validateHouseAllocation(...)`
- `NpcSpawnOrchestrator.dryRunHouseAllocation(...)`
- `NpcSpawnOrchestrator.spawnHousehold(...)`
- `/ainpc world demo create [regionId]`
- `/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]`
- `/ainpc world household <plan|spawn> <homePlaceId> [count]`
- `/ainpc world settlement <plan|spawn> <regionId> [maxHouses]`
- planner determinist pentru o casa mapata
- planner determinist pentru toate casele dintr-o regiune
- rollback global practic pentru `settlement spawn`, la nivel de NPC-uri create anterior
- `npc_world_bindings` initial pentru home/work/social place si node IDs
- family bind dupa spawn prin `FamilyBindingPlan`
- audit spawn-order pentru case, rezidenti, ancore si familie reciproca
- routine MVP pe `home/work/social anchors`
- scanner vanilla si semantic mapper initial

Nu este complet inca:

- generatorul nu produce inca `SettlementPlan` serializat si populatie narativa complexa
- planurile nu au format serializat stabil
- nu exista comenzi admin dedicate `/ainpc spawnplan validate|dryrun|commit`
- `metadata.residents` este inca solutie temporara
- nu exista tabele `households`, `household_residents` si `spawn_batches`
- `npc_world_bindings` exista initial, dar mai trebuie inspectie/backfill matur
- rollback-ul global practic nu este tranzactie DB completa
- nu exista patch planner/builder complet pentru lipsuri de sat

## Regula de baza ramasa valabila

Ordinea corecta ramane:

```text
plan -> constructie/import -> region -> place -> node -> HouseAllocation
-> NpcSpawnPlan -> spawn NPC -> save -> family bind -> audit
```

Nu schimba aceasta ordine. Daca generatorul sare peste `place`, `node` sau `HouseAllocation`, NPC-urile vor ajunge iar pe fallback-uri random.

## Faza Curenta Salvata: Mapping Demo si Settlement Planner

Starea curenta de lucru:

- mapping demo poate crea regiune, case, locuri de munca, piata si node-uri
- NPC existent poate fi legat manual la home/work/social places
- o casa poate fi planificata si populata prin `world household plan/spawn`
- o regiune poate fi planificata si populata prin `world settlement plan/spawn`
- `settlement spawn` opreste executia la prima eroare si sterge NPC-urile create in household-uri anterioare
- metadata mapping se actualizeaza dupa spawn reusit
- `npc_world_bindings` se actualizeaza dupa bind manual si spawn reusit

Urmatorul test obligatoriu:

```text
/ainpc world demo create demo_sat
/ainpc world settlement plan demo_sat
/ainpc world settlement spawn demo_sat
/ainpc audit world
/ainpc audit spawn
/ainpc world save
```

Dupa reload/restart:

```text
/ainpc audit world
/ainpc audit spawn
/ainpc world places demo_sat
```

Conditia pentru a trece la questuri:

- demo-ul ramane coerent dupa reload
- NPC-urile au casa, ancore si familie valida
- auditul nu raporteaza erori de mapping/spawn order

## Faza 11: SettlementPlan si Generator Real

Scop:

- generatorul produce planul complet in memorie inainte de orice modificare reala
- `HouseAllocation` nu mai este construit manual
- fiecare cladire, node si NPC are ID stabil inainte de spawn

Document:

- `settlement-plan.md`

Modele recomandate:

```text
SettlementPlan
  planId
  regionPlan
  buildingPlans
  nodePlans
  householdPlans
  npcPlans
  validationReport
```

```text
BuildingPlan
  localId
  type
  bounds
  entrance
  interiorSpawnPoints
  bedPoints
  workstationPoints
  maxResidents
  tags
  metadata
```

```text
HouseholdPlan
  housePlaceId
  familyId
  primaryOwnerNpcKey
  residents
```

Rezultat asteptat:

- un sat poate fi planificat complet fara spawn
- toate casele au `maxResidents`
- toate NPC-urile au `spawnNodeId`
- toate ocupatiile reale au `workPlaceId` sau `workNodeId`
- generatorul poate opri procesul inainte de modificari reale

Observatii:

- `SettlementPlan` trebuie sa fie determinist pentru acelasi seed/input.
- ID-urile trebuie sa fie stabile: `sat_01:casa_01:bed_01`, nu ID-uri bazate pe timestamp.
- `HouseAllocation` trebuie generat din `BuildingPlan` si `npcPlans`, nu invers.

Avertizari:

- Nu construi fizic cladiri pana cand planul nu trece validarea.
- Nu genera NPC-uri direct din `BuildingPlan`; treci prin `HouseAllocation`.
- Nu amesteca generarea de questuri in aceasta faza.

## Faza 12: Plan Serializat si Comenzi Admin

Scop:

- adminul poate inspecta, valida si rula un plan controlat
- acelasi plan poate fi testat de mai multe ori
- erorile sunt vizibile inainte de spawn

Comenzi recomandate:

```text
/ainpc spawnplan list
/ainpc spawnplan info <planId>
/ainpc spawnplan validate <planId>
/ainpc spawnplan dryrun <planId>
/ainpc spawnplan commit <planId>
/ainpc spawnplan discard <planId>
```

Format minim recomandat:

```yaml
spawn_plans:
  sat_01_households:
    region_id: "sat_01"
    source: "vanilla_scan"
    households:
      popescu_001:
        place_id: "sat_01:casa_popescu"
        family_id: "family_popescu_001"
        primary_owner: "npc_ion"
        residents:
          - npc_key: "npc_ion"
            name: "Ion"
            relation_role: "father"
            occupation: "fierar"
            spawn_node_id: "sat_01:casa_popescu:spawn_ion"
            bed_node_id: "sat_01:casa_popescu:bed_ion"
            work_place_id: "sat_01:fierarie"
            work_node_id: "sat_01:fierarie:anvil"
```

Validari obligatorii:

- planul are `planId` unic
- regiunea exista sau este declarata in acelasi plan
- toate `placeId` si `nodeId` se pot rezolva dupa import
- `residents <= maxResidents`
- nu exista doua NPC-uri cu acelasi `npcKey`
- nu exista doua household-uri care folosesc aceeasi casa
- work anchor este obligatoriu pentru ocupatii reale

Observatii:

- `dryrun` trebuie sa foloseasca aceeasi validare ca `commit`.
- `commit` trebuie sa refuze planurile care nu au trecut `validate`.
- planul serializat nu trebuie sa fie singura sursa de adevar dupa commit; DB-ul trebuie sa preia binding-urile mature.

Avertizari:

- Nu permite `commit` pe plan partial valid.
- Nu rescrie config-ul principal fara backup sau confirmare.
- Nu sterge planurile rulate pana nu exista audit post-commit.

## Faza 13: Model Persistent Dedicat

Scop:

- eliminarea dependentei de `metadata.residents` pentru date mature
- cautari rapide si clare dupa casa, familie si ancore
- reluare dupa restart fara presupuneri

Document:

- `households-persistente.md`

Tabele recomandate:

```text
households
household_residents
npc_world_bindings
spawn_batches
spawn_batch_steps
```

Structura minima:

```text
households
  id
  household_key
  family_id
  home_place_id
  primary_owner_npc_id
  status
  created_at
  updated_at
```

```text
household_residents
  household_id
  npc_id
  npc_key
  relation_role
  bed_node_id
  spawn_node_id
  status
```

```text
npc_world_bindings
  npc_id
  home_place_id
  work_place_id
  social_place_id
  home_node_id
  work_node_id
  social_node_id
  household_id
  family_id
  source
  updated_at
```

Rezultat asteptat:

- `NPCManager` nu trebuie sa recalculeze mereu home/work/social din apropiere
- auditul poate compara DB bindings cu WorldAdmin mapping
- familia si rutina au date persistente comune

Observatii:

- `metadata.residents` ramane pentru compatibilitate si debug, dar nu trebuie sa fie sursa finala.
- `npc_world_bindings` trebuie sa fie actualizat cand se schimba mapping-ul sau cand NPC-ul este mutat administrativ.
- `household_key` poate ramane stabil chiar daca ID-ul DB se schimba intre medii.

Avertizari:

- Nu adauga tabele fara migration/backfill.
- Nu rupe satele existente care au doar metadata.
- Nu dubla sursele de adevar fara reguli clare de prioritate.

## Faza 14: Migration si Backfill

Scop:

- datele create in MVP sunt migrate catre modelul persistent
- serverele existente nu trebuie resetate
- adminul poate vedea ce s-a convertit si ce nu

Surse de backfill:

- `WorldPlace.metadata.residents`
- `WorldPlace.ownerNpcId`
- `npc_profiles.profile_data.owned_locations`
- `npc_family`
- NPC-uri incarcate in `NPCManager`

Pipeline recomandat:

```text
scan old metadata -> infer households -> infer npc_world_bindings
-> validate inferred data -> dry-run report -> admin confirm -> write DB
```

Comenzi recomandate:

```text
/ainpc migration worldbindings dryrun
/ainpc migration worldbindings apply
/ainpc migration households dryrun
/ainpc migration households apply
```

Observatii:

- migration trebuie sa fie idempotenta.
- fiecare pas trebuie sa poata fi rulat de doua ori fara duplicate.
- raportul trebuie sa listeze case fara rezidenti, rezidenti nerezolvati si ancore in afara casei.

Avertizari:

- Nu rula migration automat la startup fara optiune de disable.
- Nu sterge metadata veche imediat dupa migration.
- Nu presupune ca `owner_npc_id` este mereu database id; poate fi nume logic, `npc_<id>` sau UUID.

## Faza 15: Rollback si Tranzactii de Spawn

Scop:

- spawn-ul batch nu lasa NPC-uri partiale
- DB-ul, entitatile si viitoarele constructii fizice raman coerente
- adminul primeste un raport clar daca ceva esueaza

Model recomandat:

```text
SpawnBatch
  batchId
  planId
  status: PLANNED | RUNNING | COMMITTED | ROLLED_BACK | FAILED
  steps
```

```text
SpawnBatchStep
  stepType: CREATE_REGION | CREATE_PLACE | CREATE_NODE | SPAWN_NPC | BIND_FAMILY | VERIFY
  targetId
  status
  rollbackAction
  error
```

Regula:

```text
preflight -> create -> verify -> commit
preflight -> create -> error -> rollback -> audit
```

Observatii:

- rollback-ul actual sterge NPC-urile create in batch, dar nu este tranzactie completa.
- pentru DB, foloseste tranzactie cand toate scrierile pot fi controlate local.
- pentru entitati Bukkit si constructii fizice, ai nevoie de actiuni compensatorii.

Avertizari:

- Nu promite rollback complet pentru constructii pana cand builderul nu are undo.
- Nu lasa `FAILED` fara raport de cleanup.
- Nu sterge NPC-uri care existau inainte de batch.

## Faza 16: Scanner Vanilla -> Gap Analyzer -> Patch Planner

Scop:

- satul vanilla este baza
- pluginul completeaza doar ce lipseste
- mapping-ul semantic devine suficient pentru spawn controlat

Document:

- `patch-planner.md`

Pipeline recomandat:

```text
VanillaVillageScanner
-> SemanticVillageMapper
-> VillageGapAnalyzer
-> VillagePatchPlanner
-> NativePatchBuilder sau WorldEditAdapter
-> SettlementPlan
-> HouseAllocation
-> spawn batch
```

`VillageGapAnalyzer` trebuie sa raporteze:

- case fara destule paturi
- paturi fara casa clara
- workstation-uri fara workplace
- lipsa clopot/piata/social anchor
- drumuri/intrari lipsa sau neclare
- case fara `maxResidents`
- locuri de munca fara node de interactiune

`VillagePatchPlanner` trebuie sa produca:

- node-uri lipsa
- metadata lipsa
- cladiri mici necesare
- paturi suplimentare
- workstation-uri suplimentare
- punct social fallback

Observatii:

- patch planner trebuie sa produca plan, nu sa modifice lumea direct.
- importul semantic si constructia fizica trebuie sa ramana pasi separati.
- WorldEdit trebuie sa fie adapter optional, nu dependinta obligatorie.

Avertizari:

- Nu inlocui satul vanilla complet daca doar lipsesc cateva noduri.
- Nu construi peste zone nevalidate.
- Nu spawna NPC-uri automat imediat dupa scanare fara `dryrun`.

## Faza 17: Rutina pe Household, Familie si Timeline

Scop:

- rutina foloseste household si bindings persistente
- familia influenteaza rutina si evenimentele sociale
- questurile pot face override temporar fara sa distruga rutina de baza

Extensii recomandate:

- sloturi pe rol: copil, parinte, sot/sotie, batran, gardian
- evenimente sociale probabilistice intre rezidenti ai aceluiasi household
- fallback diferit pentru NPC fara munca, copil sau NPC episodic
- override de quest cu expirare si audit

Date necesare:

- `household_id`
- `family_id`
- `home_node_id`
- `work_node_id`
- `social_node_id`
- `routine_override_until`
- `routine_override_source`

Observatii:

- rutina actuala este MVP si poate ramane activa.
- extinderea trebuie sa consume `npc_world_bindings`, nu sa caute random din nou.
- evenimentele sociale trebuie sa aiba cooldown global si per NPC.

Avertizari:

- Nu folosi teleport pentru toate interactiunile sociale daca pathfinding devine disponibil.
- Nu intrerupe NPC-ul aflat in quest/dialog important.
- Nu transforma timeline-ul intr-un scheduler paralel care contrazice rutina.

## Faza 18: Audit Service si Teste de Productie

Scop:

- auditul de spawn-order devine serviciu reutilizabil
- aceeasi logica este folosita de comenzi, debugdump, dry-run si teste
- exista criterii clare pentru "gata de release"

Servicii recomandate:

```text
SpawnOrderAuditService
WorldBindingAuditService
HouseholdAuditService
SpawnPlanValidationService
```

Teste recomandate:

- `HouseAllocationValidatorTest`
- `NpcSpawnOrchestratorDryRunTest`
- `SpawnOrderAuditServiceTest`
- migration dry-run test
- config round-trip pentru planuri serializate
- smoke test manual pe server Paper

Comenzi recomandate:

```text
/ainpc audit spawn
/ainpc audit worldbindings
/ainpc audit households
/ainpc debugdump all
/ainpc spawnplan dryrun <planId>
```

Observatii:

- testele Maven confirma logica Java, nu comportamentul complet pe server.
- smoke test-ul pe Paper ramane obligatoriu pentru spawn entitati si lumi reale.
- debugdump trebuie sa includa planId, batchId si householdId dupa ce exista.

Avertizari:

- Nu declara productie fara test de restart.
- Nu declara productie fara test de duplicate NPC.
- Nu declara productie fara test de plan invalid refuzat.

## Ordinea recomandata de lucru

1. Smoke test complet pe server Paper pentru `demo_sat`, cu save si reload.
2. Inspectie/backfill matur pentru `npc_world_bindings`.
3. Migration/backfill dry-run din `profile_data` si metadata veche pentru cazuri ambigue.
4. Generator narativ peste regiune: nume, roluri, familii, profesii si distributie pe case/work/social.
5. `SettlementPlan` serializat si comenzi `validate/dryrun/commit`.
6. `households`, `household_residents`, `spawn_batches` si audit post-commit.
7. Rollback tranzactional sau compensare documentata peste spawn, DB, familie si mapping metadata.
8. Gap analyzer si patch planner peste scannerul vanilla.
9. Rutina extinsa pe household/family.
10. Audit services reutilizabile si debugdump complet.

## Criterii de iesire pentru v2

v2 este completa cand:

- exista un plan serializat care poate fi validat, dry-run si commit-uit
- generatorul poate produce automat `HouseAllocation`
- un household poate fi persistat in DB dedicat
- migration dry-run poate converti metadata veche fara scrieri
- rollback-ul raporteaza exact ce a creat si ce a sters
- auditul spune clar daca un NPC are casa, node, familie si rutina coerente

## Anti-patterns de evitat in fazele urmatoare

- spawn direct din comanda/generator fara `HouseAllocation`
- folosirea `owner_npc_id` ca lista de rezidenti
- generare AI care modifica lumea fara plan validat
- migration automata fara backup
- retry orb la spawn dupa eroare de mapping
- stergere de NPC-uri existente in rollback batch
- pathfinding/rutina care ignora `npc_world_bindings`
- extinderea `ScenarioEngine` pentru probleme care tin de world binding

## Concluzie

v1 documenteaza cum s-a ajuns la nucleul MVP. v2 trebuie folosit pentru urmatoarea etapa: transformarea spawn order-ului dintr-un set de modele initiale intr-un pipeline complet, validabil, persistent si recuperabil dupa esec.
