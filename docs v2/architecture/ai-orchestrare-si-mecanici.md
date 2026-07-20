# AIOrchestrationService

Status: inventar canonic al scaffold-ului existent; nu este pipeline runtime activ.
Actualizat: 2026-07-15.

`AIOrchestrationService` este construit la pornirea pluginului, dar nu are apelant in `src/main`. Configuratia `ai.orchestration.enabled` este `false` implicit.

## Ce implementeaza

- use case-uri pentru dialog, clasificare intentie, quest/story/build-plan draft, reactie si sumar admin;
- selectie intre `OpenAIService` si `OllamaService` prin `ai_provider`;
- fallback determinist cand feature-ul, fereastra de lucru sau providerul nu permit generarea;
- retry cu backoff pentru unele erori;
- prag optional de confidence pentru cele trei tipuri de draft;
- marcaje text `[DRAFT]` si `[EXECUTED]`;
- utilitare pentru lifecycle, safety label, publish gate si mismatch.

## Limite confirmate

- `orchestrate(...)` nu apeleaza `AIResponseValidator`, desi toate politicile declara `validationRequired=true`;
- confidence lipsa sau neparsabil trece verificarea;
- `buildPrompt(...)` concateneaza contextul, actorul si playerul fara redactare;
- apelul providerului foloseste `.get()`, iar backoff-ul foloseste `Thread.sleep`; metoda nu trebuie apelata pe main thread;
- succesul foloseste codul `ai_provider_openai` inclusiv cand providerul selectat este Ollama;
- `markDraft(...)` si `markExecuted(...)` reconstruiesc rezultatul fara lifecycle, safety label si provenance, deci valorile calculate anterior se pierd;
- lifecycle-ul este pastrat doar in memoria procesului, iar `recordAuditEntry(...)` construieste un obiect fara sa-l persiste;
- marcajul `[EXECUTED]` si `runtimeExecutable=true` nu demonstreaza executia unui efect;
- serviciul nu routeaza efecte catre quest, story, world sau DB.

## Flux activ separat

Dialogul real trece prin `DialogManager` -> `DialogueEngine` -> `OpenAIService` si fallback-uri deterministe. Nu presupune ca schimbarea `AIOrchestrationService` modifica automat dialogul.

## Conditie pentru activare reala

Un consumator de productie trebuie sa apeleze explicit orchestratorul, sa aplice redactarea, `AIResponseValidator`, validarea de domeniu, aprobarea si serviciul determinist responsabil. Pana atunci, documentele nu vor descrie drafturile sau tool calls drept functionalitate livrata.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIOrchestrationService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIOrchestrationPolicy.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIResponseValidator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AISuggestionService.kt`
- `ainpc-core-plugin/src/main/resources/config.yml`

## Legaturi

- `architecture/story-si-context-ai.md`
- `architecture/spring-ai-mcp-serviciu-intern.md`
- `reference/prompt-safety-guide.md`
