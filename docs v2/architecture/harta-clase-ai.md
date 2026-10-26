# Harta claselor pentru AI

Status: harta derivata din cod.
Actualizat: 2026-10-26.

## Calea activa de dialog

- `DialogManager` - cooldown, istoric, memorii, relatie player-NPC si post-procesare;
- `DialogueEngine` - raspuns factual, fapt profesional semantic, template si decizia de a cere AI contextual;
- `DialoguePersonalityPolicy` - selectie determinista a variantelor locale generale dupa extraversia NPC-ului;
- `OpenAIPromptSnapshotFactory` - captura contextului pe thread-ul Paper;
- `OpenAISemanticWorldContextBuilder` - mapping, story si lore NPC;
- `OpenAIPromptBuilder` - compunerea promptului, citarea JSON a textelor conversationale, limite independente pentru contextul optional, profil JSON complet sau omis explicit si preambulul stabil separat pentru `instructions` in dialogul OpenAI;
- `OpenAIService` - selectia OpenAI/Ollama pentru dialog, preflight local al configuratiei de transport, request asincron, diagnostic, backoff OpenAI si fallback;
- `OpenAIConnectionProbe` - proba HTTP cu timeout, validare JSON/identitate de model si status de conexiune cu secrete mascate in datele de diagnostic/cache;
- `OpenAIHttpTransport` - termen per cerere si corp de cel mult 2 MiB, cu anularea schimbului HTTP la expirare/intrerupere sau a subscriptiei la depasirea dimensiunii;
- `OpenAIHttpBackoff` - clasificarea erorilor temporare, pauza exponentiala plafonata cu jitter si respectarea termenului minim `Retry-After`;
- `OpenAIBackoffState` - gate sincronizat cu verificarea admisiei fiecarei incercari Responses, partajat intre clientii recreati la reload pentru aceeasi cheie efectiva si acelasi endpoint;
- `OllamaService` - generare cand `ai_provider: ollama`, cu pauza scurta dupa esecuri;
- `OpenAITextSupport` - parsarea stricta a envelope-ului JSON, validarea structurii output/content, tipurilor de itemi, statusului Responses si al mesajelor, rolurilor, fazelor, refuzurilor si textului, extractie fara commentary, redactare de secrete inaintea extraselor de diagnostic, fallback local si clasificare locala de sentiment;
- `DialogueResponseNovelty` - validarea replicii prin `AIResponseValidator` si retry limitat pentru repetitie sau format invalid, nu pentru exceptii de parser;
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
- `AIResponseValidator` - valideaza replicile de dialog prin `DialogueResponseNovelty`; nu este apelat de `AIOrchestrationService.orchestrate(...)`;
- `AISuggestionService` - lifecycle si analiza in memorie;

## Context adiacent

- `StoryContextService` alimenteaza promptul si GUI-urile;
- `ProgressionService` este autoritate de progres, nu componenta AI;
- `ContextService` si `ContextRedactor` sunt utilitare izolate, fara wiring de productie confirmat;
- `McpDialogContextProvider` poate adauga context read-only, dar nu muta autoritatea la MCP.

## Limite confirmate

- `OpenAIService.isAvailable` cere lipsa suspendarii si expirarea cooldown-ului; nu confirma conectivitatea, cheia sau flag-ul AI;
- `features.ai` este verificat pentru ambele rute; cheia API se aplica numai rutei OpenAI;
- `openai.read_timeout` limiteaza fiecare incercare HTTP, nu durata totala a dialogului, a contextului sau a retry-ului conversational;
- limita de 2 MiB pe corp HTTP OpenAI este verificata inainte de decodare, inclusiv la proba de model; nu se returneaza text partial si nu este un plafon global de memorie;
- un HTTP 2xx invalid la proba confirma doar raspunsul endpoint-ului, nu disponibilitatea modelului; nu activeaza backoff-ul generarii;
- fiecare incercare Responses reverifica gate-ul inainte de transport; cererile deja admise nu sunt anulate si nu exista plafon nou de concurenta;
- `reloadFromConfig()` reseteaza suspendarea administrativa, nu setarile imuabile sau cooldown-ul; schimbarea backend-ului necesita clientul nou construit prin `reloadedFromConfig()`;
- constructorul respinge timeout-uri nepozitive si URL/header neconstruibil inainte de inlocuirea clientului sau resetarea gate-ului, fara HTTP ori secrete in exceptia de configuratie; nu confirma conectivitatea sau autentificarea;
- dialogul valideaza replica prin `AIResponseValidator`, dar `generateAsync` nu primeste automat aceasta validare de domeniu;
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
