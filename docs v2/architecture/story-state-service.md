# StoryStateService

Status: contract canonic verificat in cod.
Actualizat: 2026-07-17.

`StoryStateService` persista starea narativa pentru regiuni si places si inregistreaza evenimente narative. Nu exista un al treilea model de stare globala.

## Persistenta

- `saveRegionState(...)` cere `regionId` nenul si face upsert in `region_story_state`;
- modul implicit este `EVOLUTIVE`, iar cheia implicita este `default`;
- `savePlaceState(...)` cere `placeId` nenul si face upsert in `place_story_state`;
- `regionId` este optional la salvarea unui place;
- ambele scrieri publica un eveniment public `StoryStateChanged`.

## Evenimente

- `recordEvent(...)` cere numai `scopeType`, `scopeId` si `eventType` nenule;
- regiunea, place-ul, titlul, descrierea, payload-ul si actorii sunt campuri optionale;
- `queueEvent(...)` normalizeaza acelasi draft, dar il scrie in `story_pending_events`, nu in istoricul publicat;
- `queueTemplate(...)` construieste un draft numai din catalogul built-in si cere provenance explicita `actorType` + `actorId`;
- `hasPendingEvents(...)` si `listPendingEvents(...)` filtreaza exact dupa `scope_type` si `scope_id`;
- `publishPendingEvent(id)` insereaza evenimentul in `story_events` si sterge draftul pending in aceeasi tranzactie; la esec rollback-ul pastreaza draftul;
- `discardPendingEvent(id)` elimina draftul fara a produce un eveniment publicat;
- `listRecentEvents(regionId, placeId, limit)` necesita cel putin un filtru; cu ambele filtre goale intoarce lista goala;
- interogarea include potriviri prin `region_id`/`place_id` sau prin perechea `scope_type` + `scope_id` pentru `region` si `place`;
- pentru alte scope-uri, `StoryAuthoringService.listRecentEvents(...)` foloseste filtrare exacta dupa scope.

## Producator scheduler si review

- `story.random_events_enabled=false` mentine schedulerul dezactivat implicit;
- `story.random_events_require_review=false` pastreaza comportamentul anterior: un eveniment random acceptat este publicat direct prin `applyTemplate(...)`;
- cand ambele chei sunt active, `RandomWorldEventService` adauga template-ul in pending cu provenance `scheduler:random-world-events`;
- schedulerul nu mai adauga un draft daca scope-ul are deja un element pending, inclusiv dupa restart;
- drafturile pot fi inspectate cu `/ainpc story pending <regionId|placeId> [limit]`, publicate cu `/ainpc story publish <pendingId>` si eliminate cu `/ainpc story discard <pendingId>`;
- publicarea prin comanda trece prin `StoryAuthoringService`, deci notificarile NPC si callback-urile addon sunt emise numai dupa commit.

## Ce nu valideaza serviciul

- nu verifica existenta regiunii sau a place-ului in mapping;
- nu verifica daca place-ul apartine regiunii primite;
- nu aplica un graf de tranzitii intre cheile de stare;
- nu ofera o tabela sau un API pentru `global story state`;
- nu transforma textul liber produs de AI intr-un draft; un producator AI ramane neconectat pana exista un output structurat si validat;
- schedulerul foloseste exclusiv template-uri built-in si scrie pending numai prin opt-in explicit;
- nu autorizeaza apelantul si nu transforma un eveniment in efect de gameplay.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryStateService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryAuthoringService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/RegionStoryState.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/PlaceStoryState.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryEvent.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryPendingEvent.kt`

## Legaturi

- `architecture/story-context-service.md`
- `architecture/progression-service.md`
- `planning/questuri-avansate-v2.md`
