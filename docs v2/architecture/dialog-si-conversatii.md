# Dialog si conversatii

Status: contract canonic verificat in cod pentru formularea si post-procesarea replicii NPC.
Actualizat: 2026-07-15.

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

`OpenAIService.isAvailable` verifica doar fereastra de offline backoff. Flagul `features.ai` si cheia API sunt verificate ulterior in `generateResponse(...)`:

- AI dezactivat sau cheie lipsa: `OpenAITextSupport.generateFallbackResponse(...)`;
- backoff detectat inainte de ramura AI: template-ul local al `DialogueEngine`;
- eroare de prompt sau request: fallback local construit din `PromptSnapshot`;
- raspuns gol sau exceptie propagata spre engine: template-ul local.

Prin urmare, un apel al `OpenAIService` nu inseamna automat request extern.

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
