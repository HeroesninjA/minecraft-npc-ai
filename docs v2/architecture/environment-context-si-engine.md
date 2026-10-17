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
- `getContextForLocation(...)` construieste snapshot-ul local intr-un singur loc: ajusteaza temperatura dupa biome, poate clasifica ploaia drept ninsoare in biom rece si, cand consumatorul furnizeaza lumina observata, o limiteaza la intervalul `0..15`;
- `tick()` actualizeaza timpul, vremea, sezonul si lumina pentru toate lumile; `SchedulerCoordinator` il porneste dupa 30 de secunde si apoi o data pe minut;
- `registerSpecialEvent(...)` si metodele de clear modifica registrul separat de evenimente; urmatorul context cerut include lista curenta;
- `onWorldUnload(...)` elimina intrarile lumii.

## Consumatori confirmati

- `NPCContext`, raspunsurile factuale si promptul de dialog;
- `EnvironmentFactResolver` pentru raspunsurile factuale despre mediul curent in dialog si fallback;
- `StoryContextService` pentru semnale environment asociate mapping-ului;
- `RandomWorldEventService` si `SeasonalBehaviorService` pentru sezon;
- GUI-urile world, mapping si story;
- comanda de inspectie environment.

## Limite confirmate

- inaintea fiecarei decizii de rutina, `RoutineService` recalculeaza contextul local din snapshot-ul lumii, biomul si lumina de la locatia NPC-ului;
- `getDayNumber(...)` foloseste un cache actualizat de `tick()`;
- snapshot-ul global initial seteaza lumina la 15, iar actualizarea globala esantioneaza blocul `(0, 64, 0)`; consumatorii dependenti de locatie (`NPCContext`, story si inspectia unui player) transmit separat lumina blocului local si nu folosesc aceasta valoare globala drept lumina NPC-ului;
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
