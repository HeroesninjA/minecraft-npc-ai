# Dialog si conversatii

Status: contract canonic verificat in cod pentru formularea si post-procesarea replicii NPC.
Actualizat: 2026-10-11.

Calea activa este `DialogManager -> DialogueEngine -> OpenAIService`, cu raspunsuri locale inaintea sau in locul providerului.

## Context incarcat

`DialogManager.processMessage(...)` aplica cooldown si citeste asincron:

- ultimele 5 perechi din `dialog_history`, readuse in ordine cronologica;
- pana la 5 randuri din `npc_memories` pentru NPC si player;
- relatia player-NPC din `npc_relationships`;
- numarul total de memorii si impactul emotional agregat.

Metoda `MemoryManager.getRelevantMemories(...)` nu foloseste in prezent textul `context`; selectia este doar dupa importanta si data. Numele metodei nu confirma cautare semantica.

## Alegerea raspunsului

`DialogueEngine` foloseste urmatoarea ordine:

1. `NpcFactResolver` pentru fapte despre nume, profesie, emotie, activitate si locatie;
2. `OpenAISemanticWorldContextBuilder.resolveProfessionFact(...)` pentru un fapt profesional semantic;
3. intentie si template local procesat;
4. AI contextual numai daca pragurile de intentie, mesaj, istoric, memorii, relatie sau conversatie explicita il cer.

Saluturile locale folosesc acum recunoasterea derivata din relatia persistata si istoricul perechii NPC-jucator, nu starea globala `NPCContext.relationshipStatus`. Prietenii primesc un salut cald, rivalii unul prudent, iar un jucator deja cunoscut nu mai este tratat ca strain. Cand providerul AI nu este configurat, replica locala ramane activa.

## Recunoasterea jucatorului

- Identitatea persistenta este UUID-ul jucatorului folosit pentru relatie, istoric, amintiri si reputatie; numele afisat poate varia si nu este folosit drept cheie de recunoastere.
- `PlayerRecognition` separa prima intalnire personala de un jucator cunoscut, prieten sau relatie tensionata. Istoricul/amintirile confirma recunoasterea chiar daca randul de relatie lipseste.
- Reputatia din regiunea NPC-ului este un semnal comunitar separat: o reputatie mare nu inseamna ca NPC-ul l-a intalnit personal.
- Obiectul vizibil din mana jucatorului este capturat pe threadul Paper pentru prompt, dar nu devine automat profesie, personalitate sau fapta memorata.
- Jucatorul isi poate declara separat un rol, un stil de comunicare si o trasatura prin `/ainpc profile`. Optiunile sunt controlate, persistate dupa UUID in `player_profiles` si citite in acelasi context asincron ca relatia; un NPC le trateaza drept auto-descriere, nu drept profesie verificata, dovada de incredere sau memorie comuna.
- `/ainpc profile show` afiseaza profilul; `/ainpc profile rol <calator|comerciant|mestesugar|explorator>`, `/ainpc profile stil <direct|prietenos|formal>` si `/ainpc profile trasatura <curios|prudent|generos>` modifica doar profilul propriu; `/ainpc profile reset` il sterge. Este necesara permisiunea `ainpc.profile` (implicit disponibila jucatorilor). Consola nu poate edita profiluri.
- Pentru saluturile locale, stilurile `formal` si `direct` pot schimba formularea; o relatie tensionata are prioritate si pastreaza raspunsul prudent. Rolul si trasatura sunt folosite drept context declarat in promptul AI si in raspunsul local despre identitatea jucatorului, dar nu modifica reputatia sau mecanica de quest.
- La intrebari precum „ma cunosti?” sau „ce stii despre mine?”, raspunsul local foloseste doar numele, istoricul/relatia confirmata si rolul/trasatura declarate. Chiar fara provider AI, NPC-ul distinge prima intalnire de o relatie cunoscuta sau tensionata, fara sa inventeze amintiri.
- La „despre ce am vorbit?” sau „ce am discutat?”, NPC-ul poate rezuma doar subiectul ultimului schimb stabil disponibil dintre el si acel jucator (maximum cinci schimburi citite). Nu reda textul brut al mesajelor si nu pretinde ca stie intreaga istorie sau data unei conversatii anterioare. Intrebarile de recapitulare repetate sunt ignorate la alegerea subiectului.
- Promptul cere continuitate cu replicile recente si interzice amintiri inventate. Relatia per-jucator vine din DB; descrierea generica a `NPCContext` nu mai pretinde o relatie personala neconfirmata.

`OpenAIService.isAvailable` verifica doar fereastra de offline backoff. Flagul `features.ai` si cheia API sunt verificate ulterior in `generateResponse(...)`:

- AI dezactivat sau cheie lipsa: `OpenAITextSupport.generateFallbackResponse(...)`;
- backoff detectat inainte de ramura AI: template-ul local al `DialogueEngine`;
- eroare de prompt sau request: fallback local construit din `PromptSnapshot`;
- raspuns gol sau exceptie propagata spre engine: template-ul local.

Prin urmare, un apel al `OpenAIService` nu inseamna automat request extern.

## Mediu, rutina si MCP

`OpenAIPromptSnapshotFactory` captureaza pe threadul Paper descrierea mediului, regiunea curenta, activitatea planificata si obiectivul imediat. Promptul distinge explicit activitatea curenta de plan si nu trebuie sa prezinte un plan drept fapt realizat. `NPCContext` descrie vremea locala, biomul, anotimpul, temperatura si lumina de la pozitia NPC-ului, plus locurile si legaturile cunoscute din world mapping.

Cu `features.mcp` si `ai.orchestration.enabled` active, `OpenAIService` cere `ainpc.dialog.context` pentru regiunea NPC-ului. Sidecar-ul citeste doar snapshotul runtime si poate furniza numarul NPC-urilor inregistrate in acea regiune si maximum cinci exemple din esantionul exportat. Exemplele nu sunt lista completa. Contextul MCP nu este folosit daca regiunea lipseste, snapshotul este invechit sau sidecar-ul nu raspunde; dialogul ramane functional cu datele capturate local. Serverul MCP de proiect de pe portul `3000` nu participa la aceasta cale runtime.

Pentru comportament efectiv de rutina activeaza `routine.enabled` (implicit `false`); pentru nevoi periodice activeaza separat `simulation.enabled` (implicit `false`). Completarea dialogului prin MCP necesita si `mcp.enabled`, sidecar-ul in profil `local-bridge` cu snapshot comun si un provider AI configurat; `ai.orchestration.enabled` este implicit `false`.

## Post-procesare

Pentru un raspuns nevid, `DialogManager`:

1. clasifica local sentimentul mesajului prin `OpenAITextSupport.analyzeSentimentFast(...)`;
2. salveaza perechea player-replica in `dialog_history`;
3. inregistreaza o decizie de branch generica;
4. actualizeaza direct relatia player-NPC din `npc_relationships`;
5. creeaza o memorie numai pentru sentimente cu importanta de cel putin 2;
6. cere `EmotionManager` sa aplice efectul emotional si sa persiste emotiile asincron.

Multiplicatorul emotional foloseste relatia citita inaintea actualizarii curente, nu randul reincarcat dupa scriere. Erorile individuale SQL sunt in mare parte logate si absorbite, astfel replica poate fi livrata chiar daca o mutatie auxiliara esueaza.

## Limite de siguranta

- raspunsul providerului nu trece prin `AIResponseValidator`;
- `ContextRedactor` nu este conectat la promptul activ;
- promptul si preview-ul raspunsului pot fi logate conform configuratiei de diagnostic;
- `DialogAIRequestBuiltEvent` poate fi emis si pentru ramuri care vor folosi doar fallback local;
- o replica de quest deja produsa de `ScenarioEngine` nu intra obligatoriu in acest pipeline.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/DialogManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/DialogueEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAIService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAIPromptSnapshotFactory.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/MemoryManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/EmotionManager.kt`

## Legaturi

- `architecture/interactiuni.md`
- `architecture/reactie-npc-jucator.md`
- `architecture/story-si-context-ai.md`
- `reference/prompt-safety-guide.md`
