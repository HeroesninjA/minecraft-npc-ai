# StoryContextService

Status: contract canonic verificat in cod.
Actualizat: 2026-10-12.

`StoryContextService` construieste o proiectie read-only pentru NPC, player si locatie. Snapshot-ul este folosit de GUI, debug, quest views si prompturile de dialog.

## Constructie

- locatia NPC-ului are prioritate; locatia playerului este fallback;
- `buildForPlayer(player)` delega la `buildForNpc(null, player)`;
- mapping-ul furnizeaza regiunea, place-ul, node-urile si NPC-urile apropiate;
- pentru player sunt incarcate ancorele quest active;
- `StoryStateService` furnizeaza starea persistenta de regiune/place si evenimentele recente;
- serviciul produce `storySignals` si warnings cand lipsesc lumea, playerul sau datele persistente;
- conditiile locale ale lumii (moment, vreme, anotimp, temperatura din biom si lumina) produc semnale story chiar daca regiunea nu este mapata;
- profilul de personalitate al NPC-ului produce semnale stabile `low`/`medium`/`high` pentru cele cinci trasaturi OCEAN; contextul pentru un jucator fara NPC nu inventeaza trasaturi;
- semnalul `environment_events` include cel mult cinci etichete distincte, scurte si fara caractere de control;
- evenimentele speciale ale lumii sunt pastrate separat de snapshot-ul meteo, astfel pot fi inregistrate inainte de primul tick si apar in contextul NPC/story; se sterg la descarcarea lumii;
- construirea publica un eveniment `StoryContextBuilt`.

## Continutul snapshot-ului

- nume si rol NPC;
- nume player;
- `WorldContextSnapshot`;
- stare persistenta de regiune si place;
- stare story efectiva pentru regiune si place: randul persistent cu acelasi ID are prioritate, inclusiv cand cheia sa este `default`; in lipsa lui se foloseste starea din mapping/metadata;
- evenimente story recente;
- ancore quest active;
- semnale story si warnings.

## Limite de siguranta

- `StoryContextSnapshot.toPromptBlock()` serializeaza valorile direct;
- metoda nu apeleaza `ContextRedactor` si nu garanteaza redactarea datelor;
- snapshot-ul nu este un boundary de incredere pentru prompturi sau loguri;
- consumatorul trebuie sa limiteze, sa redacteze si sa valideze textul la iesirea finala;
- serviciul nu scrie story state si nu decide progresul questurilor.

`StoryContextSnapshot` pastreaza separat sursele brute si expune in prompt valorile `effective_region_story` si `effective_place_story` cu `source`. `WORLD_HISTORY` foloseste aceeasi rezolvare pentru modul, starea si pool-ul regiunii, respectiv starea place-ului. Un rand persistent cu ID diferit de regiunea/place-ul curent nu le poate suprascrie; pentru un place fara stare configurata sau persistenta nu se inventeaza una.

Fallback-ul dialogului foloseste campurile de stare efectiva doar pentru intrebari despre starea curenta; pentru istoric selecteaza numai `place_history.history` si titlurile din `recent_events`. `pool` este o lista de teme configurate, nu o cronologie de evenimente.

`StoryFactResolver` aplica acelasi contract in dialogul direct: primeste mapping-ul capturat inaintea procesarii asincrone, starea persistenta si evenimentele citite pe executorul bazei de date. Selectia starii efective foloseste `StoryStateViewResolver`; nu confunda starea `default` persistenta cu lipsa unei stari.

Descrierea optionala este selectata din aceeasi sursa ca starea: `variables.story_description_<stateKey>` pentru un rand persistent valid, `story.descriptions.<stateKey>` pentru o regiune din mapping, respectiv `metadata.story_description_<stateKey>` pentru un place din mapping. `WorldAdminService` pastreaza descrierile regiunii la salvare si reincarcare si le include in `WorldRegionInfo`. Descrierile altor stari nu sunt reutilizate; un rand persistent fara descriere nu imprumuta textul din mapping. Textul efectiv este redus la o linie de maximum 200 de caractere si apare separat ca `effective_region_story_description` / `effective_place_story_description` in `STORY_CONTEXT`, respectiv `region_story_description` / `place_story_description` in `WORLD_HISTORY`. Separarea pastreaza virgulele din text. Dialogul direct si fallback-ul prefera descrierea doar pentru starea curenta, fara sa o transforme in istoric; fara descriere pastreaza raspunsul bazat pe cheia starii. Vezi [configurarea descrierilor](../reference/configurare-story-lume-npc.md#descrieri-pentru-starea-story-curentă).

`RoutineService` rezolva aceeasi stare efectiva pentru locatia curenta a NPC-ului si pastreaza si modul efectiv al regiunii. Regulile `world_state_rules` din profil pot folosi scope-ul `REGION` sau `PLACE`, cheia story si/sau modul regiunii pentru a alege slotul si activitatea; regulile configurate au prioritate fata de programul orar. Citirile persistente sunt tinute intr-un cache scurt configurabil prin `routine.story_state_cache_seconds`.

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
