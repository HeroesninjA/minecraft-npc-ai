# Interactiuni

Actualizat: 2026-05-08

## Scop

Acest document descrie fluxul de interactiune dintre jucator si NPC: click dreapta, chat privat, ascultare pasiva, intentii de quest, conversatie, relatie, memorie si emotii.

Regula principala:

```text
Interactiunea routeaza intentia jucatorului.
Dialogul formuleaza raspunsul.
Serviciile deterministe decid progresul, reward-ul si efectele persistente.
```

NPC-ul poate reactiona natural, dar nu trebuie sa inventeze progres de quest, reward-uri, story state sau modificari de lume. Orice actiune care schimba starea reala trebuie sa treaca prin runtime-ul dedicat.

## Limita documentului

Acest document acopera:

- cum porneste o conversatie;
- cum este ales NPC-ul tinta;
- cum este separat chat-ul privat de chat-ul public;
- cum sunt detectate intentiile de quest;
- ce date sunt citite sau scrise dupa o interactiune;
- ce trebuie extras ulterior intr-un `InteractionService`.

Nu acopera in detaliu:

- continutul replicilor AI, care ramane in `dialog-si-conversatii.md`;
- structura avansata de quest, care ramane in `questuri-avansate-v2.md`;
- reactiile sociale pe termen lung, care raman in `reactie-npc-jucator.md`;
- rutina si tick-ul NPC, care raman in `simulation-service.md` si documentele urmatoare.

## Ce exista in cod acum

| Componenta | Rol curent |
|---|---|
| `NPCInteractionListener` | Prinde click dreapta pe villager/NPC, verifica distanta, seteaza contextul de interactiune si porneste fluxul de quest sau conversatie |
| `NPCChatListener` | Prinde chat-ul, rezolva NPC-ul tinta, anuleaza chat-ul public cand mesajul este pentru NPC si proceseaza intentii de quest sau dialog liber |
| `ConversationSessionManager` | Pastreaza sesiunea activa `player -> npc`, timestamp-ul ultimei interactiuni si cleanup-ul la timeout/quit |
| `AbstractPluginListener` | Ofera helper-ele comune pentru sesiuni, mesaje si trecerea intre thread-ul async de chat si main thread-ul Bukkit |
| `DialogManager` | Genereaza raspuns, aplica cooldown, incarca istoric/memorii/relatie si salveaza dialogul |
| `DialogueEngine` | Pipeline contextual pentru generarea raspunsului, cand este disponibil |
| `OpenAIService` | Fallback pentru generatie si analiza sentimentului |
| `QuestDecisionIntentResolver` | Normalizeaza mesajele si detecteaza intentii de quest: oferta, acceptare, refuz, abandon, status si completare |
| `ScenarioEngine` | Executa interactiunea de quest, acceptarea, refuzul, abandonul, statusul si inregistrarea conversatiei relevante pentru obiective |
| `NPCContext` | Tine contextul runtime al NPC-ului, inclusiv jucatorul cu care interactioneaza si ultimul mesaj |
| `EmotionManager` | Aplica efecte emotionale pentru apropiere, plecare si continutul mesajului |
| `MemoryManager` | Creeaza memoria primei intalniri si memorii relevante din dialog |

Implementarea este functionala, dar logica este inca distribuita intre listener-e, `DialogManager` si `ScenarioEngine`. Un serviciu explicit de interactiuni ar reduce duplicarea si ar face auditul mai clar.

## Flux click dreapta

Fluxul de baza este:

```text
PlayerInteractEntityEvent
-> NPCInteractionListener
-> villager este mapat la AINPC
-> profilul villagerului este refresh-uit
-> interactiunea default este anulata
-> se verifica npc.interaction_distance
-> NPC-ul se uita la jucator
-> NPCContext primeste interactingPlayer
-> ScenarioEngine.recordNpcConversation(...)
-> ScenarioEngine.handleQuestInteraction(...)
-> daca nu exista interactiune de quest, porneste conversatia normala
```

Comportament important:

- daca entitatea nu este villager sau nu poate fi mapata la `AINPC`, evenimentul este ignorat;
- daca NPC-ul este prea departe, jucatorul primeste mesajul `too_far`;
- click-ul pe NPC deschide sau improspateaza sesiunea privata;
- daca NPC-ul are quest relevant, `ScenarioEngine` poate trimite mesaje de quest inaintea dialogului liber;
- la pornirea conversatiei se salveaza memoria primei intalniri, cand este cazul;
- NPC-ul primeste eveniment emotional `player_approach`.

Click-ul este deci intrarea explicita. El stabileste tinta conversatiei si reduce ambiguitatea fata de ascultarea pasiva.

## Flux chat cu sesiune activa

Cand jucatorul scrie in chat, `NPCChatListener` ruleaza pe eveniment async si muta rezolvarea sensibila la Bukkit pe main thread prin `callSync`.

Fluxul este:

```text
AsyncChatEvent
-> extrage textul plain
-> cauta sesiune activa in ConversationSessionManager
-> verifica timeout-ul de 300 secunde
-> anuleaza chat-ul public
-> executa procesarea pe main thread
-> verifica "pa" / "la revedere" / "bye" / "adio" / "exit" / "quit"
-> seteaza NPCContext.lastPlayerMessage
-> incearca intai intentia de quest
-> daca nu este quest, trimite mesajul in DialogManager
```

Consecinte:

- mesajele catre NPC nu ajung in chat-ul public;
- sesiunea este per jucator, nu globala;
- sesiunea se inchide la `PlayerQuitEvent`;
- `pa` si variantele similare inchid conversatia si declanseaza `player_leave`;
- mesajele normale trec prin cooldown-ul `npc.message_cooldown`.

## Ascultare pasiva

Daca jucatorul nu are sesiune activa, NPC-ul poate raspunde pe baza proximitatii, daca `dialog.passive_listen_enabled` este activ.

Rezolvarea tintei se face in ordinea:

1. NPC apropiat mentionat explicit dupa nume sau display name;
2. un singur NPC in raza `dialog.passive_listen_radius`;
3. cel mai apropiat NPC, daca este in `dialog.auto_engage_radius`;
4. nicio tinta, caz in care chat-ul ramane public.

Valorile implicite din config sunt:

| Config | Default | Rol |
|---|---:|---|
| `dialog.passive_listen_enabled` | `true` | Permite raspunsuri fara click |
| `dialog.passive_listen_radius` | `8` | Raza maxima in care NPC-ul poate auzi mesajul |
| `dialog.auto_engage_radius` | `4` | Raza in care cel mai apropiat NPC poate intra natural in dialog |

Ascultarea pasiva trebuie folosita cu grija. Daca sunt multi NPC aproape si jucatorul nu mentioneaza numele unui NPC, sistemul trebuie sa evite alegerea agresiva a unei tinte gresite.

## Intentii de quest

Inainte ca mesajul sa ajunga la dialogul AI, `NPCChatListener` incearca sa il rezolve ca intentie de quest prin `QuestDecisionIntentResolver`.

Intentiile curente:

| Intentie | Exemple de intrare | Actiune runtime |
|---|---|---|
| `OFFER` | `misiune`, `quest`, `sarcina`, `treaba` | `ScenarioEngine.handleQuestInteraction(...)` |
| `ACCEPT` | `accept`, `da`, `sigur`, `iau quest`, `ma ocup` | `ScenarioEngine.acceptQuest(...)` |
| `DECLINE` | `refuz`, `nu`, `nu acum`, `poate mai tarziu` | `ScenarioEngine.declineQuest(...)` |
| `ABANDON` | `renunt`, `abandonez`, `abandon` | `ScenarioEngine.abandonQuest(...)` |
| `STATUS` | `status`, `progres` | `ScenarioEngine.getQuestStatus(...)` |
| `COMPLETE` | `gata`, `terminat`, `finalizat`, `am adus` | Momentan este detectata ca intentie, dar fluxul curent ajunge prin interactiunea de quest validata |

Reguli importante:

- acceptarile scurte precum `da` sunt luate in calcul doar cand exista oferta de quest activa;
- diacriticele sunt eliminate in normalizare, deci `misiune` si variantele fara diacritice sunt tratate robust;
- NPC-ul folosit pentru accept/refuz poate fi rezolvat prin oferta activa, nu doar prin NPC-ul curent;
- daca intentia de quest nu produce rezultat tratat, mesajul continua catre dialogul normal.

Acest lucru pastreaza AI-ul in rol de formulare. Progresul de quest ramane in `ScenarioEngine`.

## Dialog liber

Daca mesajul nu este consumat de quest runtime, fluxul intra in `DialogManager`.

`DialogManager` face:

- verifica si seteaza cooldown-ul `npc.message_cooldown`;
- incarca ultimele replici din `dialog_history`;
- incarca memorii relevante prin `MemoryManager`;
- incarca relatia `npc_relationships`;
- trimite contextul catre `DialogueEngine` sau `OpenAIService`;
- salveaza dialogul generat;
- actualizeaza relatia pe baza sentimentului;
- creeaza memorie daca mesajul este suficient de important;
- aplica efect emotional prin `EmotionManager`.

Tabele si date afectate:

| Zona | Exemple |
|---|---|
| `dialog_history` | mesaj jucator, raspuns NPC, emotia curenta |
| `npc_relationships` | `affection`, `trust`, `respect`, `familiarity`, `interaction_count`, `last_interaction`, `relationship_type` |
| `npc_memories` | amintiri create pentru complimente, insulte, amenintari sau mesaje importante |
| starea runtime NPC | emotii, context, ultimul mesaj, jucatorul activ |

Nu fiecare salut devine memorie. Importanta este calculata din sentiment, iar memoriile sunt create doar pentru interactiuni cu impact suficient.

## Incheiere si timeout

O conversatie se inchide cand:

- jucatorul scrie `pa`, `la revedere`, `bye`, `adio`, `exit` sau `quit`;
- jucatorul iese de pe server;
- sesiunea depaseste timeout-ul intern de 300 de secunde fara interactiune;
- NPC-ul tinta nu mai este valid sau nu mai este spawned.

La incheiere explicita:

- sesiunea este stearsa din `ConversationSessionManager`;
- NPC-ul trimite o replica de ramas bun;
- jucatorul primeste confirmarea ca discutia s-a incheiat;
- `EmotionManager` primeste evenimentul `player_leave`.

## Config relevant

| Config | Default | Folosit de | Observatie |
|---|---:|---|---|
| `npc.interaction_distance` | `5` | `AINPC.isInRange(...)` | Distanta maxima pentru click/interactiune explicita |
| `npc.message_cooldown` | `2` | `DialogManager` | Cooldown in secunde intre mesaje catre acelasi NPC |
| `dialog.passive_listen_enabled` | `true` | `NPCChatListener` | Activeaza raspunsuri fara click |
| `dialog.passive_listen_radius` | `8` | `NPCChatListener` | Cauta NPC-uri apropiate care pot auzi mesajul |
| `dialog.auto_engage_radius` | `4` | `NPCChatListener` | Permite celui mai apropiat NPC sa raspunda fara mentionare directa |

Timeout-ul conversatiei este hardcodat momentan in `NPCChatListener` la 300 de secunde. Daca interactiunile devin configurabile complet, acesta ar trebui mutat in config.

## Invariante

Pentru a evita buguri greu de reprodus, interactiunile trebuie sa respecte aceste reguli:

- chat-ul async nu trebuie sa acceseze direct API-uri Bukkit care cer main thread;
- orice modificare reala de quest trece prin `ScenarioEngine`;
- AI-ul nu scrie direct progres, reward-uri sau story state;
- o sesiune activa apartine unui singur jucator si unui singur NPC;
- mesajele private catre NPC trebuie anulate din chat-ul public;
- range-ul trebuie verificat pentru interactiunea explicita;
- intentiile scurte de accept/refuz se folosesc doar in context de oferta activa;
- memoria si relatia sunt efecte post-dialog, nu conditii inventate de AI;
- listener-ele trebuie sa ramana subtiri pe masura ce apare `InteractionService`.

## Probleme curente

Zone care merita imbunatatite:

- nu exista inca un `InteractionService` central;
- timeout-ul de conversatie nu este configurabil;
- listener-ele contin atat routing, cat si logica de efecte;
- nu exista comenzi admin dedicate pentru inspectia sesiunilor active;
- `debugdump` nu are inca o sectiune compacta pentru interactiuni active;
- intentia `COMPLETE` este detectata, dar ar trebui conectata explicit la fluxul de turn-in;
- nu exista un jurnal structurat de `InteractionEvent`;
- ascultarea pasiva poate produce surprize daca sunt multi NPC in aceeasi zona.

## Directia recomandata

Urmatorul pas tehnic este extragerea unui serviciu dedicat:

```text
InteractionService
- resolveTarget(player, message, source)
- openSession(player, npc, reason)
- closeSession(player, reason)
- routeIntent(interactionContext)
- buildInteractionResult(...)
- emitInteractionEvent(...)
```

Modele utile:

```text
InteractionContext
- player_id
- npc_id
- source: CLICK | CHAT_ACTIVE | CHAT_PASSIVE | COMMAND | QUEST_RUNTIME
- message
- direct_address
- explicit_session
- trigger_reason
- nearby_npc_count
- distance_to_npc
- active_quest_summary
- relationship_summary
- safety_flags

InteractionIntent
- type: DIALOG | QUEST_OFFER | QUEST_ACCEPT | QUEST_DECLINE | QUEST_ABANDON | QUEST_STATUS | QUEST_TURN_IN | GOODBYE | NONE
- confidence
- resolver
- raw_message
- normalized_message

InteractionResult
- handled
- open_conversation
- close_conversation
- npc_messages
- system_messages
- side_effects
- audit_warnings
```

Separarea recomandata:

| Handler | Responsabilitate |
|---|---|
| `QuestInteractionHandler` | Accept/refuz/status/abandon/turn-in prin `ScenarioEngine` |
| `DialogInteractionHandler` | Mesaje libere prin `DialogManager` |
| `SocialReactionHandler` | Evenimente emotionale, relatie, reputatie si memorii |
| `InteractionAuditService` | Snapshot read-only pentru admin/debugdump |
| `InteractionPolicy` | Range, cooldown, timeout, permisiuni si reguli de siguranta |

Scopul nu este sa se creeze un serviciu mare care decide tot. Scopul este sa existe un punct central de routing si audit, in timp ce questurile, dialogul, memoria si emotiile raman servicii separate.

## Comenzi si debug viitor

Comenzi utile pentru fazele urmatoare:

```text
/ainpc interaction status [player]
/ainpc interaction end <player>
/ainpc interaction target <player>
/ainpc interaction debug nearest
/ainpc interaction passive on|off
```

Export `debugdump` recomandat:

```text
interactions:
  active_sessions: 3
  passive_listen_enabled: true
  passive_listen_radius: 8
  auto_engage_radius: 4
  session_timeout_seconds: 300
  recent_events:
    - player: Hero
      npc: Brann
      source: CHAT_ACTIVE
      intent: QUEST_STATUS
      handled_by: ScenarioEngine
```

Audit recomandat:

- timeout configurat prea mic sau prea mare;
- passive listen activ cu raza mare;
- NPC-uri multiple cu acelasi nume in aceeasi zona;
- quest NPC fara template sau template fara dialog minim;
- intentii de quest detectate, dar netratate;
- mismatch intre sesiune activa si NPC-ul ofertei de quest.

## Smoke test manual

Checklist minim pe server Paper:

1. Porneste serverul cu un NPC AINPC spawned.
2. Click dreapta pe NPC de la distanta valida.
3. Confirma ca apare salutul si instructiunea cu `pa`.
4. Scrie un mesaj normal si verifica raspunsul privat.
5. Scrie din nou imediat si verifica mesajul de cooldown.
6. Scrie `pa` si verifica inchiderea sesiunii.
7. Scrie `misiune` langa un NPC cu quest si verifica oferta.
8. Scrie `da` dupa oferta si verifica acceptarea.
9. Scrie `status` si verifica progresul.
10. Activeaza scenariul cu mai multi NPC apropiati si verifica daca mentionarea numelui alege tinta corecta.
11. Dezactiveaza `dialog.passive_listen_enabled` si confirma ca mesajele fara click raman publice.
12. Asteapta peste 300 de secunde si confirma ca sesiunea expira.

## Definition of Done pentru interactiuni avansate

Interactiunile pot fi considerate stabile pentru faza avansata cand:

- exista `InteractionService` cu API clar;
- listener-ele doar convertesc evenimente Bukkit in cereri de interactiune;
- toate intentiile de quest au handler explicit;
- timeout-ul si passive listen sunt configurabile si auditate;
- `debugdump` exporta sesiuni active si ultimele evenimente;
- testele acopera click, chat activ, chat pasiv, goodbye, cooldown si quest intents;
- AI-ul ramane limitat la formulare si nu poate modifica direct starea de gameplay.
