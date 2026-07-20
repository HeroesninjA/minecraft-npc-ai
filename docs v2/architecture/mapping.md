# Contractul de mapping semantic

Status: contract canonic.
Verificat in cod: 2026-07-18.

Acest document este sursa de adevar pentru modelul semantic al lumii, regulile de validare si persistenta mapping-ului. Ghidurile descriu doar cum se foloseste contractul.

## Modelul valid

### Region

- reprezinta un volum 3D dintr-o singura lume;
- are ID propriu, tip, bounds, tag-uri si stare narativa;
- este container obligatoriu pentru orice `Place` si `Node`.

### Place

- apartine unei singure regiuni si aceleiasi lumi;
- trebuie sa fie complet in interiorul regiunii;
- nu se poate suprapune cu alt `Place` din aceeasi regiune;
- primeste un ID calificat de forma `region:place`.

### Node

- este un punct semantic cu raza, tip si metadata;
- apartine intotdeauna unei regiuni;
- poate apartine optional unui `Place`;
- daca are `Place`, punctul trebuie sa fie in interiorul lui;
- daca nu are `Place`, punctul trebuie sa fie direct in interiorul regiunii;
- primeste un ID de forma `region:node` sau `region:place:node`.

Prin urmare, `Region -> Place -> Node` este traseul uzual, nu o ierarhie obligatorie pentru toate node-urile. `Region -> Node` este valid pentru ancore regionale explicite.

## Starea confirmata

- `WorldAdminService` incarca, valideaza, modifica si expune mapping-ul runtime;
- `MappingIndex` indexeaza separat regiuni, places si nodes pe chunk-uri;
- lookup-ul foloseste indexul doar cand `world_admin.auto_index.enabled` este activ, altfel foloseste cautare liniara;
- `WorldAdminApi` expune proiectii `WorldRegionInfo`, `WorldPlaceInfo` si `WorldNodeInfo`, plus operatiile publice permise;
- `MappingWandService` pastreaza selectia, draftul, preview-ul si confirmarile recente ale sesiunii de authoring;
- confirmarea unui draft modifica starea runtime si marcheaza mapping-ul ca nesalvat;
- `AINPCCommandMutationPolicy` clasifica central mutatiile de mapping, fixture si spawn semantic; mutatiile CLI cer sufixul final `--confirm`, cu exceptia explicita `map confirm`;
- dispatcher-ul elimina `--confirm` inainte de handler si, daca mapping-ul ramane nesalvat, afiseaza o singura data `/ainpc world save` dupa comanda;
- GUI-ul reutilizeaza aceeasi politica: dialogurile existente adauga confirmarea tehnica, iar comenzile mutante directe deschid un dialog generic;
- persistenta in `config.yml` se face explicit prin `/ainpc world save`.

## Reguli de consum

- foloseste ID-uri, tipuri, tag-uri si metadata pentru semantica;
- foloseste coordonatele pentru validare spatiala si lookup, nu ca identificator stabil;
- questurile si NPC-urile trebuie sa rezolve ancorele prin API sau servicii, nu prin citirea directa a YAML-ului;
- un selector ambiguu trebuie respins; operatiile distructive folosesc ID-ul calificat;
- `--confirm` nu ocoleste permisiunile, validarea, protectia MCP `read_only` sau compensarea operatiei;
- un draft sau preview nu reprezinta date persistate;
- generarea sau AI-ul pot propune intentii, dar `WorldAdminService` ramane autoritatea de validare.

## Surse tehnice

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/WorldAdminService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/MappingIndex.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/WorldRegion.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/WorldPlace.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/WorldNode.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/mapping/MappingWandService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandMutationPolicy.kt`
- `ainpc-api/src/main/kotlin/ro/ainpc/api/WorldAdminApi.kt`

## Documente dependente

- `reference/mapping-stack.md`
- `guides/mapping-harti-manuale.md`
- `guides/build-mode-tutorial.md`
- `guides/build-mode-region-place-node.md`
