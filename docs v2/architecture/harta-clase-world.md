# Harta claselor pentru world

Status: harta verificata in cod.
Actualizat: 2026-07-18.

Subsistemul world separa mapping-ul semantic, proiectiile read-only si binding-urile NPC de orice constructie fizica a lumii.

## Mapping si persistenta

- `WorldAdminService` detine in runtime `Region`, `Place` si `Node`;
- `/ainpc world save` persista explicit modificarile de mapping;
- `WorldContextSnapshotBuilder` si `WorldMappingSemanticIndex` proiecteaza context read-only;
- `NpcWorldBindingService` persista separat ancorele NPC.

## Scanare si import vanilla

- `VanillaVillageScanner` descrie sesiunea/cursorul, iar `VanillaVillageScanService` citeste incremental blocurile pe schedulerul sincron Bukkit si produce raportul complet;
- `SemanticVillageMapper` transforma semnalele in mapping semantic;
- `AutoSettlementGenerator` consuma raportul complet si adauga planificarea `HouseAllocation` dupa import;
- niciuna dintre aceste clase nu construieste blocuri.

## Analiza si completare

- `VillageGapAnalyzer` detecteaza lipsuri in mapping;
- `VillagePatchPlanner` ordoneaza candidatii si verifica capabilitati;
- `VillagePatchApplier` creeaza node-uri si, in apeluri interne permise explicit, place-uri semantice;
- `BuildingAutoPlaceService` creeaza tot mapping semantic, in pofida numelui de auto-place.

## Structuri exterioare si fixture

- `ExteriorStructureBlueprintCatalog`, `ExteriorStructurePlanner` si `ExteriorStructureAnalyzer` descriu sau valideaza mapping existent;
- `ControlledTestWorldFixturePlanner` produce un plan hardcodat;
- `ControlledTestWorldFixtureApplier` creeaza mapping de test;
- `ControlledTestWorldFixturePopulator` este un flux separat care poate spawna NPC-uri de test.

## Limite

- scanarea vanilla nu paraseste main thread-ul Bukkit, dar toate cererile impart bugetul global configurabil de blocuri per tick; bugetul nu garanteaza un prag de timp daca accesul la un bloc incarca date costisitoare;
- importul, auto-place-ul, patch apply si fixture apply compenseaza creatiile partiale la esec, pastrand obiectele preexistente;
- nu exista tranzactie comuna intre mapping, spawn, binding si persistenta;
- nu exista adaptor WorldEdit sau executor de blocuri in codul de productie.

## Legaturi

- `architecture/mapping.md`
- `architecture/harta-clase-settlement.md`
- `architecture/structuri-exterioare-satului.md`
- `architecture/worldedit-integration-contract.md`
- `reference/harta-clase-index.md`
