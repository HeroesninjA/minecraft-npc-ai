# Documentatie API

Actualizat: 2026-06-21

Acest document descrie suprafata publica a API-ului AINPC pentru addonuri si consumatori externi.

Regula principala: addonurile trebuie sa consume doar clasele din `ainpc-api`. Orice dependenta directa catre `ainpc-core-plugin` (clase interne) este un semn de design gresit si trebuie refactorizata.

## Pachete publice

```
ro.ainpc.api
  AINPCPlatformApi      - punctul principal de intrare pentru platforma
  AddonRegistryApi      - registru de addonuri si descriptorilor
  WorldAdminApi         - API complet pentru world mapping semantic
  AINPCAddon            - interfata pentru addonuri
  
ro.ainpc.addons
  AddonDescriptor       - descriptor de addon (id, nume, versiune, tip, capabilitati)
  AddonType             - tipuri de addon (CORE, FEATURE, SCENARIO, STORY etc.)
  
ro.ainpc.world
  WorldRegionInfo       - informatie read-only despre o regiune
  WorldPlaceInfo        - informatie read-only despre un place (include hasTag, metadata)
  WorldNodeInfo         - informatie read-only despre un node (include metadata)
  WorldMode             - modul de world (FINITE_DYNAMIC etc.)
  PlaceType             - tipuri de place (HOUSE, FORGE, MARKET, TAVERN etc.)
  StoryMode             - modul de story (EVOLUTIVE etc.)

ro.ainpc.platform
  RuntimeMode           - modul de runtime (STANDALONE, ADVANCED etc.)
```

## WorldAdminApi

`WorldAdminApi` este API-ul principal pentru accesarea si manipularea world mapping-ului semantic (regiuni, places, nodes).

### Proprietati

| Proprietate | Tip | Descriere |
|---|---|---|
| `isEnabled` | `Boolean` | World admin este activ |
| `worldMode` | `WorldMode` | Modul curent de world |
| `regions` | `Collection<WorldRegionInfo>` | Toate regiunile |
| `places` | `Collection<WorldPlaceInfo>` | Toate place-urile |
| `nodes` | `Collection<WorldNodeInfo>` | Toate nodurile |
| `regionCount` | `Int` | Numar de regiuni |
| `placeCount` | `Int` | Numar de places |
| `nodeCount` | `Int` | Numar de noduri |
| `isAutoIndexEnabled` | `Boolean` | Indexarea automata este activa |
| `indexedRegionChunkCount` | `Int` | Numar de chunk-uri indexate pentru regiuni |
| `indexedPlaceChunkCount` | `Int` | Numar de chunk-uri indexate pentru places |
| `indexedNodeChunkCount` | `Int` | Numar de chunk-uri indexate pentru noduri |

### Metode de interogare

Toate metodele default pot fi folosite fara a fi suprascrise. Metodele abstracte trebuie implementate de service.

#### Regiuni

| Metoda | Tip | Descriere |
|---|---|---|
| `getRegion(regionId)` | abstract | Gaseste o regiune dupa ID |
| `findRegion(worldName, x, y, z)` | abstract | Gaseste regiunea la coordonate |
| `findRegionsByType(typeId)` | default | Regiuni filtrate dupa tip |
| `findRegionsByTag(tag)` | default | Regiuni filtrate dupa tag |
| `findRegionsByWorld(worldName)` | default | Regiuni filtrate dupa lume |

#### Places

| Metoda | Tip | Descriere |
|---|---|---|
| `getPlaces(regionId)` | abstract | Places dintr-o regiune |
| `getPlace(placeId)` | abstract | Place dupa ID |
| `findPlace(worldName, x, y, z)` | abstract | Place la coordonate |
| `findPlacesByTag(regionId, tag)` | default | Places filtrate dupa tag (si regiune) |
| `findPlacesByType(regionId, placeType)` | default | Places filtrate dupa tip (si regiune) |
| `findPlacesByWorld(worldName)` | default | Places filtrate dupa lume |
| `findPlacesByOwner(npcId)` | default | Places detinute de un NPC |
| `findPlacesByMetadata(regionId, key, value)` | default | Places filtrate dupa metadata |

#### Noduri

| Metoda | Tip | Descriere |
|---|---|---|
| `getNodes(regionId)` | abstract | Noduri dintr-o regiune |
| `getNodesForPlace(placeId)` | abstract | Noduri dintr-un place |
| `getNode(nodeId)` | abstract | Node dupa ID |
| `findNode(worldName, x, y, z)` | abstract | Node la coordonate |
| `findNodesNear(worldName, x, y, z, radius, limit)` | abstract | Noduri in apropiere |
| `findNodesByType(regionId, typeId)` | default | Noduri filtrate dupa tip |
| `findNodesByWorld(worldName)` | default | Noduri filtrate dupa lume |
| `findNodesByMetadata(regionId, key, value)` | default | Noduri filtrate dupa metadata |

#### NPC Bindings

| Metoda | Tip | Descriere |
|---|---|---|
| `bindNpcToHomePlace(placeId, npcId, npcName)` | abstract | Leaga un NPC de casa sa |
| `bindNpcToWorkPlace(placeId, npcId, npcName)` | abstract | Leaga un NPC de locul de munca |
| `bindNpcToSocialPlace(placeId, npcId, npcName)` | abstract | Leaga un NPC de locul social |

#### Utilitare

| Metoda | Tip | Descriere |
|---|---|---|
| `hasUnsavedChanges()` | abstract | Exista modificari nepersistate |

### AINPCPlatformApi

`AINPCPlatformApi` este punctul de intrare pentru platforma.

| Membru | Tip | Descriere |
|---|---|---|
| `runtimeMode` | `RuntimeMode` | Modul curent de runtime |
| `worldMode` | `WorldMode` | Modul curent de world |
| `defaultStoryMode` | `StoryMode` | Modul implicit de story |
| `addonRegistry` | `AddonRegistryApi` | Registrul de addonuri |
| `worldAdmin` | `WorldAdminApi` | API-ul de world admin |
| `dataDirectory` | `Path` | Directorul cu date ale pluginului |
| `packDirectory` | `Path` | Directorul cu pack-uri |
| `getAddonConfigDirectory(addonId)` | default | Directorul deconfig al unui addon (sanitizat) |
| `reloadContent()` | abstract | Reincarca continutul |

Accesul la platforma se face prin:
```kotlin
val platform: AINPCPlatformApi = plugin.platform
val worldAdmin: WorldAdminApi = platform.worldAdmin
```

### AddonRegistryApi

`AddonRegistryApi` gestioneaza inregistrarea si interogarea addonurilor.

| Membru | Tip | Descriere |
|---|---|---|
| `descriptors` | `Collection<AddonDescriptor>` | Toti descriptorii inregistrati |
| `primaryScenario` | `AddonDescriptor?` | Scenariul principal |
| `registerDescriptor(descriptor)` | abstract | Inregistreaza un descriptor |
| `registerAddon(addon)` | abstract | Inregistreaza un addon |
| `unregisterAddon(addonId)` | abstract | Dezinregistreaza un addon |
| `removeByOrigin(origin)` | abstract | Sterge addonurile dupa origine |
| `getDescriptors(type)` | abstract | Descriptorii filtrati dupa tip |
| `getDescriptor(id)` | abstract | Descriptor dupa ID |
| `isAddonEnabled(addonId)` | default | Verifica daca un addon e activ |
| `size()` | abstract | Numarul de descriptorii |

## Reguli de consum

1. Foloseste `platform.worldAdmin` (returneaza `WorldAdminApi`), nu `platform.worldAdminService` (clasa interna).
2. Foloseste metodele default pentru interogari simple; nu implementa propria logica de filtrare.
3. Pentru binding NPC-place, foloseste metodele `bindNpcTo*`. Nu scrie direct in DB.
4. Toate metodele default sunt sigure la null/blank (returneaza colectii goale).
5. `WorldAdminApi` este interfata stabila; metodele abstracte pot fi extinse, metodele default nu trebuie suprascrise.
6. Pentru metadata, foloseste `findPlacesByMetadata` si `findNodesByMetadata` in loc sa filtrezi manual colectiile.
