# Story si context in fluxul AI

Status: document derivat din fluxul runtime activ.
Actualizat: 2026-07-15.

Story context ajunge in dialogul contextual existent, dar nu trece prin `AIOrchestrationService`.

## Flux activ

1. `DialogManager` pregateste istoricul, memoriile si relatia player-NPC.
2. `DialogueEngine` incearca raspunsuri factuale si template-uri deterministe.
3. Cand criteriile cer ramura contextuala, `OpenAIService` captureaza un `PromptSnapshot` pe thread-ul Paper.
4. `NPCContext.generateContextDescription()` poate include `StoryContextService.buildForNpc(npc, playerul activ)`.
5. `OpenAISemanticWorldContextBuilder` include separat un snapshot construit prin `buildForNpc(npc, null)`.
6. `OpenAIPromptBuilder` adauga istoricul, memoriile si relatia, iar `McpDialogContextProvider` poate adauga context MCP read-only.
7. `OpenAIService` foloseste providerul sau un fallback local, in functie de flag, cheie, backoff si erori.

## Consecinte

- snapshot-ul semantic construit cu player `null` nu include ancorele quest active ale playerului;
- blocul din `NPCContext` poate include playerul, inclusiv numele si story context-ul lui;
- acelasi prompt poate contine proiectii story cu scope diferit;
- inchiderea sesiunii nu goleste imediat playerul din `NPCContext`;
- `ContextRedactor` nu este apelat automat;
- raspunsul providerului nu este trecut prin `AIResponseValidator`;
- `OpenAIService.isAvailable` nu confirma flag activ sau cheie configurata.

## Flux neconectat

`AIOrchestrationService` contine use case-uri pentru dialog, story, quest si build-plan drafts, dar nu are apelant in codul de productie. El ramane scaffold dezactivat implicit, nu etapa activa a dialogului sau authoring-ului.

## Regula

- story state-ul persistent ramane autoritatea;
- contextul serializat este continut, nu tranzactie si nu dovada de validare;
- orice integrare viitoare trebuie sa conecteze explicit redactarea, validatorul si serviciul determinist care detine efectul.

## Legaturi

- `architecture/dialog-si-conversatii.md`
- `architecture/story-state-service.md`
- `architecture/story-context-service.md`
- `architecture/harta-clase-context.md`
- `architecture/ai-orchestrare-si-mecanici.md`
- `reference/prompt-safety-guide.md`
