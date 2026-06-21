# API Events, Listeners si Triggers

Actualizat: 2026-06-18

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## Scop

Acest document defineste ce evenimente, listener-e si trigger-e sunt necesare pentru API-ul public AINPC peste sistemele:

- quest si progresie
- story state si story events
- dialog si intentii de conversatie
- NPC lifecycle, interactiune, emotii si memorie
- context read-only pentru world, story, NPC si player

Documentul separa explicit:

- ce exista deja in core
- ce trebuie expus ca API public stabil
- unde trebuie declansate evenimentele
- cine are voie sa le asculte

## Status curent

| Zona | Exista acum | Lipseste pentru API public |
|---|---|---|
| Bukkit listeners | `ListenerRegistry` inregistreaza listener-ele runtime principale | evenimente custom publice in `ainpc-api` |
| Quest/progression | `QuestObjectiveListener`, `ScenarioEngine`, `ProgressionService`; lifecycle initial `accepted/declined/abandoned/completed`, `objective progress`, `offer`, `stage change`, `tracking` si `anchor binding` sunt emise prin `callEvent` | niciunul pentru quest/progression de baza |
| Story | `StoryStateService.recordEvent`, `StoryContextService` | evenimente publice pentru story state changed, story event recorded, context built si signal collected |
| Dialog | `NPCInteractionListener`, `NPCChatListener`, `DialogManager` | evenimente publice pentru session start/end, message si response |
| NPC | `NPCManager`, `VillagerLifecycleListener`, `NPCContext` | evenimente publice pentru NPC discovered/spawned/profile/emotion/memory |
| Context | `WorldContextSnapshot`, `StoryContextSnapshot`, `NPCContext` | hook-uri read-only pentru context built/signal collected |
| Addonuri | `AINPCPlatformApi`, `AddonRegistryApi`, `WorldAdminApi` | contract documentat pentru event package si payload-uri stabile |

Observatie importanta: in cod exista initial `callEvent` pentru lifecycle-ul minim de progression: accepted, declined, abandoned si completed. Restul listener-elor actuale asculta evenimente Paper/Bukkit si cheama servicii interne.

## Principii obligatorii

1. Evenimentele publice sunt contract de integrare, nu sursa de adevar.
2. Serviciile deterministe raman responsabile pentru progres, persistenta, reward-uri si validare.
3. Dialogul formuleaza raspunsuri si intentii; nu acorda direct progres sau reward-uri fara serviciile quest/progression.
4. Contextul expus prin evenimente este read-only.
5. Evenimentele `Before*` pot fi cancellable doar daca nu s-a modificat inca starea.
6. Evenimentele `After*`, `*Changed`, `*Recorded`, `*Completed` nu trebuie sa fie cancellable.
7. Listener-ele addonurilor nu trebuie sa scrie direct in tabelele core.
8. Evenimentele care pot porni AI sau I/O extern trebuie limitate prin cooldown, scheduler si timeouts.
9. Tipurile din `ainpc-api` trebuie sa ramana prietenoase pentru Java, conform `kotlin-interop-api-addonuri.md`.

## Pachete recomandate

| Pachet | Rol |
|---|---|
| `ro.ainpc.api.events` | baza comuna pentru evenimente publice |
| `ro.ainpc.api.events.quest` | quest/progression events |
| `ro.ainpc.api.events.story` | story state si story event events |
| `ro.ainpc.api.events.dialog` | dialog/session/intent events |
| `ro.ainpc.api.events.npc` | NPC lifecycle si social events |
| `ro.ainpc.api.events.context` | world/story/NPC context read-only events |

Recomandare practica: evenimentele publice pot extinde `org.bukkit.event.Event` si pot locui in `ainpc-api`, iar core-ul le declanseaza prin `Bukkit.getPluginManager().callEvent(event)`.

## Model de baza pentru event public

Fiecare event public trebuie sa expuna minim:

- `eventId`: UUID/string pentru corelare in logs/debugdump
- `createdAtMillis`: timp unix millis
- `source`: `PLAYER`, `NPC`, `COMMAND`, `GUI`, `SCHEDULER`, `ADDON`, `SYSTEM`
- `playerUuid`: optional
- `npcId`: optional, ID intern stabil cand exista
- `regionId`: optional
- `placeId`: optional
- `progressionSelector`: optional pentru quest/progression
- `metadata`: map read-only cu valori string, limitata si sanitizata

Nu include in event:

- obiecte mutable interne din core
- conexiuni DB
- fisiere
- config complet
- prompt complet AI cu date sensibile
- referinte care permit addonului sa ocoleasca serviciile core

## Listenere existente in core

| Listener | Eveniment Bukkit ascultat | Trigger intern actual | Sistem afectat |
|---|---|---|---|
| `NPCInteractionListener` | `PlayerInteractEntityEvent` | click dreapta pe villager gestionat ca AINPC | dialog, quest, NPC context |
| `NPCChatListener` | `AsyncChatEvent` | mesaj chat cu sesiune activa, nume NPC sau passive listen | dialog, quest intent |
| `NPCChatListener` | `PlayerQuitEvent` | player iese de pe server | curatare sesiune dialog |
| `QuestObjectiveListener` | `PlayerMoveEvent` | schimbare de block | `recordRegionVisit` |
| `QuestObjectiveListener` | `EntityDeathEvent` | kill facut de player | `recordMobKill` |
| `QuestObjectiveListener` | `EntityPickupItemEvent` | item pickup | refresh inventar quest next tick |
| `QuestObjectiveListener` | `PlayerDropItemEvent` | item drop | refresh inventar quest next tick |
| `QuestObjectiveListener` | `InventoryClickEvent` | click inventar | refresh inventar quest next tick |
| `QuestObjectiveListener` | `InventoryDragEvent` | drag inventar | refresh inventar quest next tick |
| `PlayerJoinListener` | `PlayerJoinEvent` | player intra pe server | recunoastere NPC/memorie |
| `VillagerLifecycleListener` | `CreatureSpawnEvent` | villager spawn | `ensureVillagerIsNPC`, profile refresh, rebalance |
| `VillagerLifecycleListener` | `ChunkLoadEvent` | chunk incarcat | restaurare NPC, refresh profile, rebalance |
| `VillagerLifecycleListener` | `VillagerCareerChangeEvent` | profesie villager schimbata | refresh profile |
| `VillagerLifecycleListener` | `EntityDeathEvent` | villager mort | `handleEntityDeath` |
| `MappingWandListener` | `PlayerInteractEvent` | click cu wand admin | selectie mapping/anchor/NPC bind |
| `GuiInventoryListener` | `InventoryClickEvent` | click in inventar AINPC GUI | `GuiService.handleClick` |
| `GuiInventoryListener` | `InventoryDragEvent` | drag in AINPC GUI | blocare mutare iteme |
| `GuiInventoryListener` | `InventoryCloseEvent` | inchidere GUI | inchidere sesiune GUI |
| `GuiInventoryListener` | `PlayerQuitEvent` | player iese | curatare stare GUI |

## Evenimente necesare pentru quest si progresie

| Event API propus | Cand se declanseaza | Cancellable | Listener tipic | Trigger actual / viitor |
|---|---|---:|---|---|
| `ProgressionOfferEvent` | un NPC sau sistem ofera un quest/progression | da, inainte de offer vizibil | addon de reguli, reputatie, gating | `ScenarioEngine.handleQuestInteraction` |
| `ProgressionAcceptedEvent` | player accepta o progresie | nu | addon de analytics, tutorial, story | `ScenarioEngine.acceptQuest`; implementat initial |
| `ProgressionDeclinedEvent` | player refuza oferta | nu | story/social reaction | `ScenarioEngine.declineQuest`; implementat initial |
| `ProgressionAbandonedEvent` | player abandoneaza progresia | nu | cleanup, reputatie, story event | `ScenarioEngine.abandonQuest`, `ProgressionService.abandon`; implementat initial |
| `ProgressionObjectiveProgressEvent` | un obiectiv primeste progres | nu | UI, debug, analytics, story signal | `recordRegionVisit`, `recordMobKill`; implementat initial |
| `ProgressionOfferEvent` | questul este oferit jucatorului | nu | UI, story, analytics, addon de gating | `handleQuestInteraction`, `startQuestManually`; implementat initial |
| `ProgressionStageChangedEvent` | faza questului se schimba | nu | UI, story, analytics, addon de pacing | `updateTrackedQuestProgress`, `setCurrentQuestProgress`; implementat acum |
| `ProgressionStageChangedEvent` | progresia trece la alt stage | nu | story pacing, NPC hints | `ScenarioEngine`; implementat acum |
| `ProgressionCompletedEvent` | progresia este finalizata | nu | story state, reward addon, achievement | `markQuestCompleted`; implementat initial |
| `ProgressionFailedEvent` | progresia esueaza | nu | story consequence, cleanup | `markQuestFailed`; implementat acum |
| `ProgressionTrackingChangedEvent` | player porneste/opreste tracking | nu | UI, minimap, particles addon | `startQuestTracking`, `stopQuestTracking`; implementat acum |
| `ProgressionAnchorBoundEvent` | progresia se leaga la region/place/node/NPC | nu | mapping audit, story context | `persistQuestAnchors`, anchor binding; implementat acum |

Payload minim:

- `progressionSelector`
- `progressionId`
- `progressionKind`
- `mechanicId`
- `stageId`
- `objectiveId`
- `previousValue`
- `newValue`
- `requiredValue`
- `npcId`
- `playerUuid`
- `anchorIds`

Regula de trigger:

- evenimentul se declanseaza dupa ce serviciul a validat tranzitia
- pentru `Offer`, un `Before/Offer` cancellable poate rula inainte de mesajul catre player
- completarea nu trebuie facuta direct din listener; se cere prin API/service, nu prin mutare manuala de state

## Evenimente necesare pentru story

| Event API propus | Cand se declanseaza | Cancellable | Listener tipic | Trigger actual / viitor |
|---|---|---:|---|---|
| `StoryStateChangedEvent` | se schimba state persistent pe region/place | nu | quest generator, UI, audit | `StoryStateService.saveRegionState`, `savePlaceState`; implementat acum |
| `StoryEventRecordedEvent` | se insereaza un rand in `story_events` | nu | context cache, analytics, addon narativ | `StoryStateService.recordEvent`; implementat acum |
| `StoryActionAppliedEvent` | quest action `set_story_state` sau `record_story_event` a fost aplicata | nu | validator/debug/story timeline | `ScenarioEngine.applyQuestStoryActions`; implementat acum |
| `StoryContextBuiltEvent` | se construieste snapshot narativ pentru player/NPC | nu, read-only | AI orchestration, debug, addon de inspectie | `StoryContextService.buildForPlayer`, `buildForNpc`; implementat acum |
| `StorySignalCollectedEvent` | se detecteaza un semnal semantic important | nu, optional/dezactivat default | debug si authoring | `StoryContextService.collectStorySignals`; implementat acum |

Payload minim:

- `scopeType`
- `scopeId`
- `regionId`
- `placeId`
- `eventType`
- `eventKey`
- `actorType`
- `actorId`
- `playerUuid`
- `npcId`
- `storySignals`
- `changedKeys`

Regula de trigger:

- `StoryEventRecordedEvent` se declanseaza dupa insert reusit si contine `StoryEvent.id`
- `StoryContextBuiltEvent` nu trebuie declansat pe fiecare tick; doar la cereri explicite de dialog, quest, GUI, debug sau authoring
- listener-ele nu au voie sa modifice `StoryContextSnapshot`

## Evenimente necesare pentru dialog

| Event API propus | Cand se declanseaza | Cancellable | Listener tipic | Trigger actual / viitor |
|---|---|---:|---|---|
| `DialogSessionStartedEvent` | player incepe conversatie cu NPC | da, inainte de sesiune | addon de blocare, tutorial, reputation gate | `NPCInteractionListener.startConversation`, quest-open, passive listen; implementat acum |
| `DialogMessageReceivedEvent` | mesaj player este routat la NPC | da, inainte de AI | intent resolver addon, profanity filter | `NPCChatListener.handleResolvedMessage`; implementat acum |
| `DialogIntentResolvedEvent` | textul devine intentie de quest/dialog | nu | quest addon, analytics, debug | `QuestDecisionIntentResolver`, resolver viitor |
| `DialogAIRequestBuiltEvent` | prompt/request este pregatit | nu, doar observare (gate de `dialog_prompt_events_enabled`) | AI policy addon | `DialogManager.processMessage`; implementat acum |
| `DialogResponseGeneratedEvent` | AI/fallback produce raspuns | nu | subtitles, memory, moderation post-check | `NPCChatListener.handleResolvedMessage`; implementat acum |
| `DialogSessionEndedEvent` | conversatia se inchide | nu | memory, emotions, cleanup | goodbye, quit, timeout; implementat acum |

Payload minim:

- `conversationId`
- `playerUuid`
- `npcId`
- `message`
- `directAddress`
- `explicitConversation`
- `triggerReason`
- `nearbyNpcCount`
- `distanceToNpc`
- `intent`
- `responseStatus`
- `responsePreview`

Regula de trigger:

- evenimentele de dialog care ating Bukkit state trebuie declansate pe main thread
- `AsyncChatEvent` ramane doar sursa; core-ul trebuie sa mute procesarea pe sync inainte de event public
- payload-ul pentru prompt AI trebuie sa fie sanitizat; nu publica cheia OpenAI sau config complet

## Evenimente necesare pentru NPC

| Event API propus | Cand se declanseaza | Cancellable | Listener tipic | Trigger actual / viitor |
|---|---|---:|---|---|
| `AINPCDiscoveredEvent` | villager existent este asociat ca AINPC | nu | addon de profilare, mapping | `NPCManager.createAutoProfile`; implementat acum |
| `AINPCSpawnedEvent` | NPC nou este creat/spawned | da, inainte de spawn | addon de populatie, spawn rules | `NPCManager.createNPC`, `createNPCFromPlan`; implementat acum |
| `AINPCProfileRefreshedEvent` | profilul runtime se regenereaza/refresh | nu | UI, audit, addon de profesii | `refreshVillagerProfile`; implementat acum |
| `AINPCInteractedEvent` | player interaction valid cu NPC | nu | social, quest, tutorial | `NPCInteractionListener` dupa range check; implementat acum |
| `AINPCEmotionChangedEvent` | emotie NPC se schimba | nu | dialog tone, story reaction | `EmotionManager.applyEmotion`, `setMood`; implementat acum |
| `AINPCMemoryRecordedEvent` | se salveaza memorie/dialog important | nu | relationship addon, analytics | `MemoryManager.createMemory`; implementat acum |
| `AINPCRoutineChangedEvent` | rutina/activitatea planificata se schimba | nu | simulation, UI | `RoutineService.runRoutineTick`; implementat acum |
| `AINPCDeathEvent` | NPC/villager gestionat moare | nu | cleanup, story consequence | `NPCManager.handleEntityDeath`; implementat acum |

Payload minim:

- `npcId`
- `npcUuid`
- `entityUuid`
- `displayName`
- `profession`
- `sourceKey`
- `regionId`
- `placeId`
- `nodeId`
- `playerUuid`
- `emotion`
- `relationshipDelta`

Regula de trigger:

- evenimentele NPC nu trebuie sa expuna direct obiectul intern `AINPC` ca mutable API stabil
- daca se expune un handle, acesta trebuie sa fie read-only sau o interfata publica dedicata
- `BeforeSpawn` poate fi cancellable; `Spawned/Discovered/ProfileRefreshed` nu

## Evenimente necesare pentru context

| Event API propus | Cand se declanseaza | Cancellable | Listener tipic | Trigger actual / viitor |
|---|---|---:|---|---|
| `WorldContextBuiltEvent` | snapshot de lume este construit | nu | AI, quest authoring, debug | `NPCContext.updateWorldContextSnapshot`; implementat acum |
| `StoryContextBuiltEvent` | snapshot story este construit | nu | AI, quest director, authoring | `StoryContextService`; implementat acum in story slice |
| `NPCContextUpdatedEvent` | `NPCContext` se actualizeaza din lume/simulare | nu | debug, simulation signals | `NPCContext.updateFromWorld`; implementat acum (gate `context_events_enabled`) |
| `ContextSignalCollectedEvent` | un semnal semantic este colectat | nu, optional | authoring/debug | context services; clasa exista, necablat |
| `PlayerContextChangedEvent` | player intra in alta regiune/place relevant | nu | tutorial, quest hints | `QuestObjectiveListener.onPlayerMove`; implementat acum (gate `context_events_enabled`) |

Payload minim:

- `contextType`
- `playerUuid`
- `npcId`
- `regionId`
- `placeId`
- `nodeIds`
- `signals`
- `reason`
- `requestSource`

Regula de trigger:

- context events sunt pentru observare si extensie, nu pentru control de flow
- nu se declanseaza in bucle dese fara throttle
- pentru AI, contextul trebuie redus la semnale si sumar, nu la harta completa

## Matrice trigger -> servicii -> eveniment public

| Trigger sursa | Serviciu core | Eveniment public necesar | Consumatori |
|---|---|---|---|
| Click dreapta pe NPC | `NPCInteractionListener`, `ScenarioEngine`, `DialogManager` | `AINPCInteractedEvent`, `ProgressionOfferEvent`, `DialogSessionStartedEvent` | dialog, quest, tutorial, addon social; `AINPCInteractedEvent` implementat |
| Mesaj chat catre NPC | `NPCChatListener`, `DialogManager` | `DialogMessageReceivedEvent`, `DialogIntentResolvedEvent`, `DialogResponseGeneratedEvent` | quest intent, AI, memorie |
| Offer/accept/decline/abandon din chat/comanda/GUI | `ScenarioEngine`, `ProgressionService` | `ProgressionOfferEvent`, `ProgressionAcceptedEvent`, `ProgressionDeclinedEvent`, `ProgressionAbandonedEvent` | story, GUI, analytics; lifecycle initial implementat |
| Region/mob objective progress | `ScenarioEngine` | `ProgressionObjectiveProgressEvent` | UI, debug, analytics; initial implementat |
| Stage change after objective update | `ScenarioEngine` | `ProgressionStageChangedEvent` | pacing, HUD, analytics; initial implementat |
| Player se misca intre blocks | `QuestObjectiveListener` | `PlayerContextChangedEvent`, `ProgressionObjectiveProgressEvent` | quest, context, hints |
| Mob kill | `QuestObjectiveListener` | `ProgressionObjectiveProgressEvent` | quest, bounty, story |
| Inventar schimbat | `QuestObjectiveListener` | `ProgressionObjectiveProgressEvent` | collect/deliver objectives |
| Quest complete/fail | `ScenarioEngine` | `ProgressionCompletedEvent`, `ProgressionFailedEvent`, `StoryActionAppliedEvent` | story state, rewards, notifications; completed initial implementat |
| Story action aplicata | `ScenarioEngine`, `StoryStateService` | `StoryStateChangedEvent`, `StoryEventRecordedEvent` | context, timeline, AI |
| Villager spawn/chunk load | `VillagerLifecycleListener`, `NPCManager` | `AINPCDiscoveredEvent`, `AINPCProfileRefreshedEvent` | NPC addons, mapping, simulation; implementate acum |
| Dialog produce memorie | `DialogManager`, `MemoryManager` | `AINPCMemoryRecordedEvent` | relationship, story, analytics; implementat acum |
| Emotion manager modifica stare | `EmotionManager` | `AINPCEmotionChangedEvent` | dialog tone, UI, simulation; implementat acum |
| NPC creat/spawned | `NPCManager` | `AINPCSpawnedEvent` | population, spawn rules; implementat acum |
| NPC moare | `VillagerLifecycleListener`, `NPCManager` | `AINPCDeathEvent` | cleanup, story; implementat acum |
| Rutina se schimba | `RoutineService` | `AINPCRoutineChangedEvent` | simulation, UI; implementat acum |
| GUI click | `GuiInventoryListener`, `GuiService` | de obicei niciun event public direct; se emit evenimentele serviciului rezultat | quest GUI, authoring, debug |
| Mapping wand | `MappingWandListener`, `MappingWandService` | `ContextSignalCollectedEvent` sau `MappingDraftChangedEvent` viitor | authoring, world admin |
| NPC context update | `NPCContext` | `NPCContextUpdatedEvent`, `WorldContextBuiltEvent` | AI, debug, simulation; implementate acum |
| Player movement intre regiuni | `QuestObjectiveListener` | `PlayerContextChangedEvent` | quest, context, hints; implementat acum |

## Ce trebuie sa asculte addonurile

| Scop addon | Evenimente recomandate | Ce sa nu faca |
|---|---|---|
| Addon de quest nou | `ProgressionOfferEvent`, `ProgressionObjectiveProgressEvent`, `ProgressionCompletedEvent` | sa modifice direct `activePlayerQuests` |
| Addon story/narativ | `StoryEventRecordedEvent`, `StoryStateChangedEvent`, `ProgressionCompletedEvent` | sa scrie direct in `story_events` |
| Addon dialog | `DialogSessionStartedEvent`, `DialogMessageReceivedEvent`, `DialogIntentResolvedEvent`, `DialogResponseGeneratedEvent`, `DialogSessionEndedEvent` | sa apeleze AI extern blocant pe main thread |
| Addon social/reputatie | `AINPCInteractedEvent`, `DialogSessionEndedEvent`, `AINPCMemoryRecordedEvent`, `AINPCEmotionChangedEvent` | sa schimbe emotii/memorii fara serviciu dedicat |
| Addon world/context | `WorldContextBuiltEvent`, `StoryContextBuiltEvent`, `PlayerContextChangedEvent` | sa consume harta completa la fiecare tick |
| Addon UI/HUD | `ProgressionTrackingChangedEvent`, `ProgressionStageChangedEvent`, `ProgressionCompletedEvent` | sa trateze GUI intern ca API public |

## Ce trebuie sa declanseze core-ul

| Componenta core | Responsabilitate de event |
|---|---|
| `ScenarioEngine` | quest/progression lifecycle, objective progress, stage change, story actions |
| `ProgressionService` | fatada generica peste tracking, status, abandon si snapshot-uri; tracking si anchor binding emit event public |
| `StoryStateService` | state changed si event recorded dupa persistenta reusita |
| `StoryContextService` | context built si signals, doar la cereri explicite |
| `DialogManager` | request built, response generated, memory candidate |
| `NPCInteractionListener` | interactiune NPC validata si session start |
| `NPCChatListener` | dialog message, response, session end |
| `NPCManager` | discovered, spawned, profile refreshed, death cleanup |
| `EmotionManager` | emotion changed |
| `MemoryManager` | memory recorded |
| `GuiService` | nu emite evenimente GUI generice; lasa serviciul apelat sa emita eventul domeniului |

## Ordine recomandata de implementare

1. ~~Adauga pachetul `ro.ainpc.api.events` in `ainpc-api` cu base event si DTO-uri read-only.~~ **GATA**
2. ~~Continua emiterea evenimentelor din `ScenarioEngine` dupa tranzitii validate.~~ **GATA**
3. ~~Adauga test Java de consum pentru evenimentele din `ainpc-api`.~~ **GATA**
4. ~~Adauga NPC events (toate 8: discovered, spawned, profile, interacted, emotion, memory, routine, death).~~ **GATA**
5. ~~Adauga `DialogAIRequestBuiltEvent` si `ProgressionFailedEvent`.~~ **GATA**
6. ~~Adauga context events (WorldContextBuilt, NPCContextUpdated, PlayerContextChanged, ContextSignalCollected).~~ **GATA** (WorldContextBuilt cablat si din StoryContextService; NPCContextUpdated din NPCContext; PlayerContextChanged din QuestObjectiveListener; ContextSignalCollected clasa in context pachet)
7. ~~Adauga teste Java interop si Kotlin trigger pentru toate evenimentele.~~ **GATA**
9. ~~Extinde `/ainpc debugdump` si auditul ca sa arate ultimele evenimente publice emise.~~ **GATA** (scrie `recent-public-events.txt` cu ultimele 50 de `story_events` din DB)

## Flags si configurare

Recomandare pentru `config.yml`:

```yaml
events:
  public_api_enabled: true
  context_events_enabled: false
  dialog_prompt_events_enabled: false
  debug_recent_event_buffer: 100
  context_event_min_interval_ticks: 20
```

Reguli:

- `public_api_enabled` poate opri emiterea evenimentelor custom pentru diagnostic.
- `context_events_enabled` trebuie sa fie `false` implicit pana exista throttle si teste de performanta.
- `dialog_prompt_events_enabled` nu trebuie sa publice prompt complet; doar sumar sau metadata.
- bufferul de debug nu trebuie sa contina date sensibile.

## Compatibilitate API

Pentru fiecare event public:

- constructorul trebuie sa fie stabil pentru Java
- getter-ele trebuie sa foloseasca nume simple
- colectiile trebuie sa fie immutable/copy
- campurile optionale trebuie documentate
- adaugarea unui camp nou trebuie sa fie backward compatible
- eliminarea/redenumerirea unui event cere versiune API noua

## Definitie de gata

Un event este considerat gata cand:

1. tipul exista in `ainpc-api`
2. are test Java de consum
3. core-ul il declanseaza intr-un singur loc clar
4. exista test pentru trigger-ul principal
5. payload-ul nu expune obiecte mutable interne
6. documentatia listeaza cand se declanseaza si daca este cancellable
7. debug/audit poate confirma emiterea fara date sensibile

