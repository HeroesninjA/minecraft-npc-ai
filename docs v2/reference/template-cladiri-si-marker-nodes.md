# Template-uri de cladiri si marker nodes

Status: referinta verificata in cod.
Actualizat: 2026-07-18.

Template-urile curente descriu dreptunghiuri de mapping si ancore semantice. Ele nu contin blocuri, schematic-uri sau instructiuni de constructie.

## Modelul API

`BuildingTemplateDefinition` poate declara:

- `templateId`, nume si `placeType`;
- latime, adancime si inaltime;
- lista de `BuildingAnchorDefinition`;
- variante, rotatii, capabilitati, tags si metadata.

Runtime-ul de auto-place foloseste numai dimensiunile, tipul de place si ancorele.

## Registrul activ

`BuildingTemplateRegistry.loadDefaults()` inregistreaza hardcodat:

- `house_small`: node-uri `bed`, `entrance`, `npc_spawn`;
- `forge`: node-uri `workstation`, `entrance`, `npc_spawn`;
- `farm`: node-uri `workstation`, `entrance`, `npc_spawn`.

Nu exista loader YAML pentru aceste template-uri.

## Auto-place

`/ainpc building auto-place <templateId> <regionId>`:

- cauta un dreptunghi neocupat in limitele regiunii;
- creeaza un `Place` cu tags `auto_placed` si ID-ul template-ului;
- creeaza node-urile la offseturile declarate;
- muta numai mapping-ul din `WorldAdminService`.

Comanda afiseaza explicit ca produce numai place-uri si node-uri de mapping semantic si nu construieste sau modifica blocuri fizice. Nu roteste template-ul, nu alege variante si nu salveaza automat mapping-ul. Daca un node esueaza dupa crearea place-ului, creatiile acelui auto-place sunt compensate si rezultatul raporteaza eroarea.

## Regula

- foloseste termenul `template semantic`, nu `schematic`;
- un marker node este o ancora de gameplay, nu dovada ca exista o constructie fizica;
- dupa auto-place, verifica mapping-ul si ruleaza `/ainpc world save` daca rezultatul este acceptat.

## Surse in cod

- `ainpc-api/src/main/kotlin/ro/ainpc/api/settlement/BuildingTemplateDefinition.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingTemplateRegistry.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingAutoPlaceService.kt`

## Legaturi

- `architecture/harta-clase-settlement.md`
- `architecture/generare-sate-fara-worldedit.md`
- `planning/patch-planner.md`
