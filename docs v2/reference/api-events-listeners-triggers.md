# API events, listeners si triggers

Status: contract de orientare verificat in `ainpc-api`; nu este catalog exhaustiv al ordinii runtime.
Actualizat: 2026-07-18.

Evenimentele publice transporta snapshot-uri si metadata pentru integrare. Serviciile deterministe si tabelele persistente raman sursa de adevar.

## Familii publice

- `events.context` - world, NPC, player si semnale de context;
- `events.dialog` - sesiune, mesaj, intentie, request construit, raspuns si inchidere;
- `events.npc` - interactiune, lifecycle, emotie, memorie si rutina;
- `events.quest` - oferta, lifecycle, stage, objective, tracking si anchor binding;
- `events.story` - state, event, action, context si semnale.

## Evenimente cancellable

In API sunt declarate explicit cancellable:

- `DialogSessionStartedEvent`;
- `DialogMessageReceivedEvent`;
- `AINPCSpawnedEvent`.

Pentru fiecare trebuie verificat emitatorul. Interfata `Cancellable` nu garanteaza singura ca nicio stare nu a fost schimbata anterior.

In fluxul de dialog:

- anularea `DialogSessionStartedEvent` opreste pornirea sesiunii in calea listenerului;
- anularea `DialogMessageReceivedEvent` opreste trimiterea mesajului la `DialogManager`;
- `DialogAIRequestBuiltEvent` este informativ si poate aparea chiar daca urmeaza fallback local, nu request extern.

In fluxul de rutina:

- `AINPCRoutineChangedEvent` este emis numai cand `plannedRoutineActivity` se schimba si `events.public_api_enabled=true`;
- evenimentul apare dupa actualizarea activitatii, goal-ului si starii NPC;
- callback-ul addon `onNpcStateChange` este livrat in timpul schimbarii efective de stare, inaintea eventualului `AINPCRoutineChangedEvent`, si nu este eveniment Bukkit anulabil;
- evenimentul este informativ si core-ul nu are consumator quest/story pentru el.

## Captura diagnostica

`RecentPublicEventListener` inregistreaza explicit la prioritatea `MONITOR` toate cele 33 de clase concrete `*Event` din modulul API. Captura nu anuleaza si nu modifica evenimentele; retine in `RecentEventsBuffer` numai timestamp, numele clasei, starea async si starea cancellable observata.

`RecentPublicEventListenerTest` compara catalogul listenerului cu declaratiile concrete din `ainpc-api`, astfel incat o clasa publica noua nu ramane neobservata accidental. `events.public_api_enabled=false` opreste emitatorii existenti, iar `events.debug_recent_event_buffer` controleaza bufferul in intervalul bounded `10..1000`, inclusiv dupa reload.

Exportul diagnostic foloseste `recent-api-events.txt` pentru aceasta captura in-memory. `recent-story-events.txt` este o sursa separata, citita din tabela `story_events`, si nu trebuie interpretata drept eveniment Bukkit API.

## Limite

- payload-urile sunt snapshot-uri, nu obiecte de comanda si nu tranzactii;
- un eveniment emis dupa persistenta nu poate fi folosit pentru rollback implicit;
- captura diagnostica este volatila, fara payload si nu stabileste o ordine totala intre thread-uri sau intre listener-ele cu aceeasi prioritate;
- testele de sursa existente confirma prezenta unor emitatori, nu toate garantiile de ordine, thread si persistenta;
- lista concreta de clase din `ainpc-api/src/main/kotlin/ro/ainpc/api/events/` are prioritate fata de rezumate vechi.

## Surse in cod

- `ainpc-api/src/main/kotlin/ro/ainpc/api/events/`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners/NPCInteractionListener.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners/NPCChatListener.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/DialogManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineService.kt`

## Legaturi

- `reference/addon-developer-guide.md`
- `reference/documentatie-api.md`
- `architecture/harta-clase-listeners.md`
- `architecture/interactiuni.md`
- `architecture/comportament-natural-npc-rutine-alocari.md`
