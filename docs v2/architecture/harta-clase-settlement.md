# Harta claselor pentru settlement

Status: harta verificata in cod.
Actualizat: 2026-07-18.

Subsistemul settlement este o colectie de fluxuri separate. Nu exista un serviciu unic care incarca o definitie, construieste satul si spawneaza populatia.

## Definitii declarative

- `SettlementConfigLoader` citeste `settlements.yml` din data folder;
- produce `SettlementDefinition` si raporteaza erori sau warning-uri;
- singurul consumer de productie este `/ainpc world settlement definitions`;
- definitiile nu sunt consumate de scanner, planner, spawn sau un generator de blocuri.

## Template-uri semantice

- `BuildingTemplateRegistry` inregistreaza in cod `house_small`, `forge` si `farm`;
- `BuildingAutoPlaceService` cauta un dreptunghi liber in regiune;
- serviciul creeaza un `Place` si node-urile template-ului in mapping;
- nu plaseaza blocuri, nu foloseste rotatia si nu executa variante de cladire.

## Scanare si import

- `VanillaVillageScanner` creeaza o sesiune cu cursor reluabil, iar `VanillaVillageScanService` consuma o coada FIFO cu buget global pe schedulerul sincron Bukkit;
- `SemanticVillageMapper` importa imediat `Region`, `Place` si `Node` in memoria `WorldAdminService`;
- `AutoSettlementGenerator` primeste raportul complet, combina importul semantic cu `HouseAllocationPlanner` si permite mapper-ului sa reutilizeze explicit regiunea ceruta;
- reutilizarea pastreaza regiunea existenta si adauga numai place-uri/node-uri, in timp ce importul standard continua sa respinga duplicatele;
- auto-generatorul nu construieste blocuri si nu spawneaza NPC-uri.

## Populatie si spawn

- `HouseAllocationPlanner` si `HouseAllocationValidator` produc intrarea reala de spawn;
- `NpcSpawnOrchestrator` executa dry-run, spawn, persistenta household si compensarea NPC-urilor noi;
- `NarrativeGenerator` produce un preview separat, neconectat la comanda de spawn;
- DTO-ul API `SettlementPlan` nu participa la aceste fluxuri.

## Gap-uri si patch-uri

- `VillageGapAnalyzer` inspecteaza mapping-ul;
- `VillagePatchPlanner` produce `PatchPlan`;
- `VillagePatchApplier` poate crea elemente semantice;
- modurile `NATIVE_PATCH` si `WORLDEDIT_TEMPLATE` nu reprezinta un constructor de blocuri activ.

## Limite comune

- mutatiile bulk de mapping compenseaza creatiile proprii la esec, dar nu sunt tranzactionale impreuna cu spawn-ul, binding-ul sau persistenta;
- salvarea mapping-ului este separata prin `/ainpc world save`;
- unele comenzi mutatoare nu afiseaza pasul de salvare; protectia read-only este aplicata central rutelor de mapping si spawn administrativ;
- numele `auto-place`, `native patch` si `settlement auto` descriu mai mult decat executa in prezent.

## Legaturi

- `architecture/settlement-plan.md`
- `architecture/generare-sate-fara-worldedit.md`
- `architecture/harta-clase-spawn.md`
- `planning/patch-planner.md`
