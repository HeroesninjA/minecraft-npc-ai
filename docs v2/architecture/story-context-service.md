# StoryContextService

Status: contract canonic verificat in cod.
Actualizat: 2026-07-15.

`StoryContextService` construieste o proiectie read-only pentru NPC, player si locatie. Snapshot-ul este folosit de GUI, debug, quest views si prompturile de dialog.

## Constructie

- locatia NPC-ului are prioritate; locatia playerului este fallback;
- `buildForPlayer(player)` delega la `buildForNpc(null, player)`;
- mapping-ul furnizeaza regiunea, place-ul, node-urile si NPC-urile apropiate;
- pentru player sunt incarcate ancorele quest active;
- `StoryStateService` furnizeaza starea persistenta de regiune/place si evenimentele recente;
- serviciul produce `storySignals` si warnings cand lipsesc lumea, playerul sau datele persistente;
- construirea publica un eveniment `StoryContextBuilt`.

## Continutul snapshot-ului

- nume si rol NPC;
- nume player;
- `WorldContextSnapshot`;
- stare persistenta de regiune si place;
- evenimente story recente;
- ancore quest active;
- semnale story si warnings.

## Limite de siguranta

- `StoryContextSnapshot.toPromptBlock()` serializeaza valorile direct;
- metoda nu apeleaza `ContextRedactor` si nu garanteaza redactarea datelor;
- snapshot-ul nu este un boundary de incredere pentru prompturi sau loguri;
- consumatorul trebuie sa limiteze, sa redacteze si sa valideze textul la iesirea finala;
- serviciul nu scrie story state si nu decide progresul questurilor.

## Consumatori confirmati

- `OpenAISemanticWorldContextBuilder` si `OpenAIPromptBuilder`;
- `NPCContext`;
- `QuestAuthoringGui`, `QuestLogGui`, `QuestDetailGui` si `StoryGui`;
- comenzile story si debugdump-urile.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryContextService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryContextSnapshot.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAISemanticWorldContextBuilder.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAIPromptBuilder.kt`

## Legaturi

- `architecture/story-state-service.md`
- `architecture/mapping.md`
- `architecture/story-si-context-ai.md`
- `reference/prompt-safety-guide.md`
