# Preview de populatie narativa

Status: persistenta, selectie si conversie narativa verificate; executia ramane separata.
Actualizat: 2026-07-18.

`NarrativeGenerator` construieste un preview determinist al populatiei peste mapping-ul existent. Preview-ul nu este intrarea folosita de comanda curenta de spawn.

## Intrari si rezultat

`/ainpc population plan <regionId> [targetPopulation] [seed]`:

- selecteaza case, locuri de munca si locuri sociale din regiune;
- ignora casele fara node de spawn sau bed/home;
- foloseste seed-ul primit sau `<region>:narrative:01`;
- rezolva tema descriptiva din primul ID normalizat in ordine lexicografica dintre tagurile valide `theme:<id>` ale regiunii; fara un asemenea tag foloseste `generic` si emite avertisment;
- deriva `planId` determinist din regiune si hash-ul unsigned al seed-ului pe opt cifre hexazecimale;
- produce familii, nume, varste, profesii, roluri sociale, roluri quest si profile de rutina;
- salveaza rezultatul prin `PopulationPlanRepository` in `<dataFolder>/population-plans/<planId>.plan.json`.

Fiecare fisier foloseste un envelope JSON `schema_version: 1`, pastreaza `created_at` la suprascrierea aceluiasi `planId` cu acelasi seed si actualizeaza `updated_at`. Un seed diferit care produce acelasi ID scurt este respins ca o coliziune, nu suprascrie planul existent. Scrierea foloseste un fisier temporar si mutare atomica, cu fallback la replace cand filesystem-ul nu suporta atomic move. Payload-ul contine toate campurile `PopulationPlan`, `HouseholdPlan` si `ResidentNarrativePlan`; planul supravietuieste restartului si reload-ului.

Campul numit `targetPopulation` este completat cu numarul efectiv generat, nu cu tinta ceruta, astfel ca intentia initiala nu ramane distincta in rezultat.

## Selectie si inspectie, nu executie

Comenzile persistente sunt:

- `/ainpc population list [regionId]` listeaza planurile salvate, newest-first;
- `/ainpc population inspect <planId>` incarca exact planul cerut si il converteste in `HouseAllocation` doar pentru afisare;
- `/ainpc population select <planId>` salveaza selectia pentru regiunea planului in `selections.json`; o selectie noua inlocuieste numai selectia acelei regiuni;
- tab-completion-ul ofera ID-urile persistate pentru `inspect` si `select` cand pluginul este disponibil;
- nu exista comanda care trimite aceste alocari catre `NpcSpawnOrchestrator`;
- `/ainpc world settlement spawn <region>` regenereaza alocari distincte prin `HouseAllocationPlanner`.

Selectia este stare operationala persistenta pentru un executor viitor. Ea nu declanseaza spawn si nu modifica fluxul activ de settlement.

## Contractul conversiei

`PopulationPlan.toHouseAllocations()` pastreaza identitatea, familia, varsta, genul, ancorele si campurile `socialRole`, `ageGroup`, `routineProfile`, `questRole` si `backstorySeed` pe `HouseAllocation.ResidentPlan`. `ResidentPlan.toNpcSpawnPlan()` le transmite mai departe, iar un spawn care foloseste acel plan le salveaza sub obiectul `narrative` din `npc_profiles.profile_data`; reconstruirea profilului pastreaza obiectul, iar promptul AI primeste JSON-ul profilului.

Aceste valori sunt metadata descriptiva. `routineProfile` nu selecteaza inca o rutina din `RoutineEngine`, iar `questRole` nu creeaza si nu leaga un quest. Generatorul emite un `backstorySeed` determinist, iar conversia construieste textul generic folosit de `NpcSpawnPlan`; preview-ul nu promite un backstory detaliat. Similar, `themeId` descrie planul, dar nu schimba regulile generatorului si nu activeaza automat un feature-pack. Tagurile `theme:<id>` sunt comparate fara diferenta de litere mari/mici, ID-ul este normalizat la forma `snake_case`, iar fallback-ul fara tag valid este `generic`.

## Regula

- trateaza `PopulationPlan` ca preview experimental;
- pentru spawn-ul activ, foloseste contractul `HouseAllocationPlanner -> NpcSpawnOrchestrator`;
- nu documenta preview-ul ca pipeline complet pana cand are executor explicit;
- nu prezenta metadata `routineProfile` sau `questRole` drept comportament activ pana cand exista integrarea dedicata.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/NarrativeGenerator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/PopulationPlan.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocation.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/NpcSpawnPlan.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/PopulationPlanRepository.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt`

## Legaturi

- `architecture/settlement-plan.md`
- `architecture/harta-clase-spawn.md`
- `guides/ordine-spawn-npc-cladiri-region-node.md`
