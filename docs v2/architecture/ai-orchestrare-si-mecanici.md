# AIOrchestrationService

Status: pipeline opt-in pentru sumar administrativ si clasificarea intentiei de dialog; nu executa actiuni de gameplay.
Actualizat: 2026-10-12.

`AIOrchestrationService` este construit la pornirea pluginului. `/ainpc debugdump ai summary` este primul consumator runtime si cere permisiunea `ainpc.admin`. Configuratia `ai.orchestration.enabled` ramane `false` implicit.

## Ce implementeaza

- use case-uri pentru dialog, clasificare intentie, quest/story/build-plan draft, reactie si sumar admin;
- selectie intre `OpenAIService` si `OllamaService` prin `ai_provider`;
- fallback determinist cand feature-ul, fereastra de lucru sau providerul nu permit generarea;
- retry asincron cu backoff pentru raspuns gol sau erori temporare;
- prag optional de confidence pentru cele trei tipuri de draft;
- redactarea promptului cu `ContextRedactor` si validarea raspunsului cu `AIResponseValidator`;
- marcaj `[DRAFT]` pentru sugestii; rezultatele generate nu sunt marcate drept executate;
- utilitare pentru lifecycle, safety label, publish gate si mismatch.

## Primul flux interconectat

`/ainpc debugdump ai summary` captureaza pe thread-ul principal numai numere si indicatori din lume (regiuni si locuri), NPC, magazine, scenarii, story feature flag si istoricul AI. Trimite instantaneul catre `orchestrateAsync(ADMIN_DEBUG_SUMMARY)`, apoi publica raspunsul validat inapoi pe thread-ul principal. Nu trimite nume de jucatori, conversatii, stari brute sau chei si nu modifica aceste sisteme. Daca providerul nu este disponibil, comanda recomanda debugdump-ul determinist.

## Clasificarea intentiei de dialog

Cu `features.ai=true`, `ai.orchestration.enabled=true` si `ai.orchestration.intent_classification.enabled=true`, calea `DialogManager -> DialogueEngine` poate clasifica asincron numai mesaje ambigue de 5-80 caractere pentru care raspunsul contextual AI nu ar fi fost cerut. Raspunsurile factuale si intentiile deja recunoscute local au prioritate. Modelul poate propune numai `TRADE`, `WORK_TALK` sau `NONE`; un parser strict accepta `TRADE` numai daca jucatorul are un magazin asociat accesibil cu marfa configurata si `WORK_TALK` numai daca NPC-ul are ocupatie. Eroarea, un label necunoscut sau un output invalid revin la intentia determinista. `TRADE` raspunde cu un catalog factual din ofertele validate si accesibile; daca acest catalog dispare intre verificari, revine la intentia locala. `WORK_TALK` raspunde din profesia, locul de munca mapat si rutina planificata capturate inainte de pasul asincron; nu prezinta planul drept actiune curenta. Aceeasi replica factuala este folosita si cand intentia `WORK_TALK` este recunoscuta local, chiar daca clasificarea AI este dezactivata. Clasificarea nu cumpara, nu ofera questuri, nu publica story si nu declanseaza un al doilea apel AI contextual.

Pentru un mesaj ambiguu eligibil, `DialogManager` verifica anticipat asocierea magazinului si permisiunile si capteaza profesia, rutina si cladirea de munca mapata pe thread-ul principal, inclusiv la NPC-urile `FULL_AI`; aceasta verificare suplimentara cere toate cele trei flaguri active. Separat, intrebarile comerciale si intentia `TRADE` recunoscuta local cauta magazine accesibile chiar fara AI, iar `WORK_TALK` local capteaza datele de munca. Lipsa unui magazin accesibil blocheaza eticheta AI `TRADE`, fara sa ascunda alte raspunsuri. Lipsa profesiei blocheaza eticheta AI `WORK_TALK`.

Promptul catre provider include mesajul scurt al jucatorului si doua indicatoare booleene, fara numele jucatorului sau catalogul magazinului; `ContextRedactor` elimina datele sensibile recunoscute inainte de trimitere.

## Limite confirmate

- confidence lipsa sau neparsabil trece verificarea;
- API-ul sincron `orchestrate(...)` ramane pentru compatibilitate, dar returneaza fallback pe thread-ul principal; consumatorii noi trebuie sa foloseasca `orchestrateAsync(...)`;
- lifecycle-ul este pastrat doar in memoria procesului, iar `recordAuditEntry(...)` construieste un obiect fara sa-l persiste;
- `markExecuted(...)` este un utilitar explicit, nu este folosit de generarea AI; marcajul nu demonstreaza executia unui efect;
- serviciul nu routeaza efecte catre quest, story, world sau DB.

## Flux activ separat

Dialogul obisnuit trece prin `DialogManager` -> `DialogueEngine` -> `OpenAIService` si fallback-uri deterministe. Clasificatorul de mai sus este o ramura opt-in separata; nu inlocuieste generarea contextuală a replicii.

## Conditie pentru activare reala

Pentru efecte de gameplay, un consumator nou trebuie sa adauge validare de domeniu, aprobarea si apelul explicit al serviciului determinist responsabil. Pana atunci, drafturile si tool calls nu sunt functionalitati de executie livrate.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIOrchestrationService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIOrchestrationPolicy.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIResponseValidator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AISuggestionService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/DialogueAiIntentClassification.kt`
- `ainpc-core-plugin/src/main/resources/config.yml`

## Legaturi

- `architecture/story-si-context-ai.md`
- `architecture/spring-ai-mcp-serviciu-intern.md`
- `reference/prompt-safety-guide.md`
