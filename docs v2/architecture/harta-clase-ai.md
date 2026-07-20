# Harta claselor pentru AI

Status: harta derivata din cod.
Actualizat: 2026-07-15.

## Calea activa de dialog

- `DialogManager` - cooldown, istoric, memorii, relatie player-NPC si post-procesare;
- `DialogueEngine` - raspuns factual, fapt profesional semantic, template si decizia de a cere AI contextual;
- `OpenAIPromptSnapshotFactory` - captura contextului pe thread-ul Paper;
- `OpenAISemanticWorldContextBuilder` - mapping, story si lore NPC;
- `OpenAIPromptBuilder` - compunerea promptului;
- `OpenAIService` - request asincron, diagnostic, offline backoff si fallback;
- `OpenAITextSupport` - extractie text, fallback local si clasificare locala de sentiment;
- `DialogHistory`, `NPCRelationship`, `NPCEmotions` si `PromptSnapshot` - date de context.

Providerul nu este prima si nici singura sursa de replica. `DialogueEngine` poate inchide fluxul cu un fapt sau template, iar `OpenAIService` poate intoarce fallback local fara request extern.

## Mutatii dupa replica

- `DialogManager` scrie istoricul, relatia player-NPC si memoria;
- `EmotionManager` aplica efectul emotional si persista emotiile asincron;
- `RelationshipService` nu participa la aceasta cale: el detine relatiile NPC-NPC;
- raspunsurile quest produse anterior de `ScenarioEngine` pot ocoli aceste mutatii.

## Calea scaffold

- `AIOrchestrationService` - serviciu construit, dar fara apelant de productie;
- `AIOrchestrationPolicy` - politici pentru sapte use case-uri;
- `AIResponseValidator` - validator separat, neconectat la orchestrator sau dialog;
- `AISuggestionService` - lifecycle si analiza in memorie;
- `OllamaService` - provider selectabil numai de scaffold-ul orchestration in fluxul verificat.

## Context adiacent

- `StoryContextService` alimenteaza promptul si GUI-urile;
- `ProgressionService` este autoritate de progres, nu componenta AI;
- `ContextService` si `ContextRedactor` sunt utilitare izolate, fara wiring de productie confirmat;
- `McpDialogContextProvider` poate adauga context read-only, dar nu muta autoritatea la MCP.

## Limite confirmate

- `OpenAIService.isAvailable` inseamna doar ca offline backoff-ul nu este activ;
- flagul `features.ai` si cheia API sunt verificate in interiorul serviciului;
- raspunsul providerului nu trece prin `AIResponseValidator`;
- promptul activ nu trece prin `ContextRedactor`;
- evenimentul `DialogAIRequestBuiltEvent` nu garanteaza ca s-a facut un request extern.

## Regula

- existenta unei clase nu confirma folosirea ei in runtime;
- un fallback local nu trebuie etichetat drept raspuns al providerului;
- textul generat nu devine autoritate asupra questului, story state-ului sau mapping-ului.

## Legaturi

- `architecture/dialog-si-conversatii.md`
- `architecture/story-context-service.md`
- `architecture/harta-clase-context.md`
- `architecture/harta-clase-npc.md`
- `reference/prompt-safety-guide.md`
