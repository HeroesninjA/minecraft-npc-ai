# Harta claselor pentru AI

Status: harta derivata din cod.
Actualizat: 2026-10-15.

## Calea activa de dialog

- `DialogManager` - cooldown, istoric, memorii, relatie player-NPC si post-procesare;
- `DialogueEngine` - raspuns factual, fapt profesional semantic, template si decizia de a cere AI contextual;
- `DialoguePersonalityPolicy` - selectie determinista a variantelor locale generale dupa extraversia NPC-ului;
- `OpenAIPromptSnapshotFactory` - captura contextului pe thread-ul Paper;
- `OpenAISemanticWorldContextBuilder` - mapping, story si lore NPC;
- `OpenAIPromptBuilder` - compunerea promptului;
- `OpenAIService` - selectia OpenAI/Ollama pentru dialog, request asincron, diagnostic, backoff OpenAI si fallback;
- `OllamaService` - generare cand `ai_provider: ollama`, cu pauza scurta dupa esecuri;
- `OpenAITextSupport` - extractie text, fallback local si clasificare locala de sentiment;
- `DialogHistory`, `NPCRelationship`, `NPCEmotions` si `PromptSnapshot` - date de context.

Providerul nu este prima si nici singura sursa de replica. `DialogueEngine` pastreaza raspunsurile care cer exactitate si poate folosi un template la esec, iar ruta AI poate intoarce fallback factual local fara request extern.

## Mutatii dupa replica

- `DialogManager` scrie istoricul, relatia player-NPC si memoria;
- `EmotionManager` aplica efectul emotional si persista emotiile asincron;
- `RelationshipService` nu participa la aceasta cale: el detine relatiile NPC-NPC;
- raspunsurile quest produse anterior de `ScenarioEngine` pot ocoli aceste mutatii.

## Orchestrare AI

- `AIOrchestrationService` - poate clasifica optional intentii ambigue si ruteaza celelalte use case-uri configurate;
- `AIOrchestrationPolicy` - politici pentru sapte use case-uri;
- `AIResponseValidator` - valideaza rezultatele orchestrarii, nu replica directa a dialogului;
- `AISuggestionService` - lifecycle si analiza in memorie;

## Context adiacent

- `StoryContextService` alimenteaza promptul si GUI-urile;
- `ProgressionService` este autoritate de progres, nu componenta AI;
- `ContextService` si `ContextRedactor` sunt utilitare izolate, fara wiring de productie confirmat;
- `McpDialogContextProvider` poate adauga context read-only, dar nu muta autoritatea la MCP.

## Limite confirmate

- `OpenAIService.isAvailable` inseamna doar ca offline backoff-ul nu este activ;
- `features.ai` este verificat pentru ambele rute; cheia API se aplica numai rutei OpenAI;
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
