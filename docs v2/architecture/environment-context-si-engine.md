# EnvironmentContext si EnvironmentEngine

Status: contract canonic verificat in cod.
Actualizat: 2026-10-12.

Acest subsistem descrie mediul unei lumi. Nu rezolva playerul, regiunea, place-ul sau node-ul curent.

## Model

`EnvironmentContext` este un snapshot cu:

- numele lumii;
- intervalul zilei si tick-ul zilei;
- vremea;
- sezonul si numarul zilei;
- nivelul de lumina;
- temperatura;
- evenimente speciale pastrate in cache.

El poate produce descrieri si blocuri de prompt, dar nu persista date si nu aplica redactare.

## Engine

`EnvironmentEngine` tine cache-uri in memorie pe numele lumii:

- `getContext(...)` construieste snapshot-ul la primul acces si apoi returneaza cache-ul;
- `getContextForLocation(...)` ajusteaza temperatura dupa biome si poate clasifica ploaia drept ninsoare in biom rece;
- `tick()` actualizeaza timpul, vremea, sezonul si lumina pentru toate lumile; `SchedulerCoordinator` il porneste dupa 30 de secunde si apoi o data pe minut;
- `registerSpecialEvent(...)` si metodele de clear modifica registrul separat de evenimente; urmatorul context cerut include lista curenta;
- `onWorldUnload(...)` elimina intrarile lumii.

## Consumatori confirmati

- `NPCContext` si promptul de dialog;
- `EnvironmentFactResolver` pentru raspunsurile factuale despre mediul curent in dialog si fallback;
- `StoryContextService` pentru semnale environment asociate mapping-ului;
- `RandomWorldEventService` si `SeasonalBehaviorService` pentru sezon;
- GUI-urile world, mapping si story;
- comanda de inspectie environment.

## Limite confirmate

- inaintea fiecarei decizii de rutina, `RoutineService` recalculeaza contextul local din snapshot-ul lumii si biomul de la locatia NPC-ului;
- `getDayNumber(...)` foloseste un cache actualizat de `tick()`;
- snapshot-ul initial seteaza lumina la 15, iar actualizarea ulterioara ar esantiona blocul global `(0, 64, 0)`, nu locatia consumatorului;
- engine-ul nu detine mapping semantic si nu poate afla singur `Region`, `Place` sau `Node`;
- evenimentele speciale si cache-ul nu sunt persistate.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/environment/EnvironmentContext.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/environment/EnvironmentEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NPCContext.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryContextService.kt`

## Legaturi

- `architecture/harta-clase-context.md`
- `architecture/story-context-service.md`
- `architecture/mapping.md`
