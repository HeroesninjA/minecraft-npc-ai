# Taskuri finalizate

Acest document conține taskurile mutate din `docs/taskuri-prioritizate.md`.
Ele sunt păstrate ca istoric de lucru și referință de audit.

## P0 - Fundatie, API si runtime

- Stabileste contractul public minim pentru `ainpc-api` (`WorldAdminApi` complet: 12 proprietati, 14 metode abstracte, 11 metode default, proprietati de indexare, metode de bind NPC-place, hasUnsavedChanges).
- Blocheaza dependintele directe ale addonurilor catre clase interne din core (`WorldContextSnapshotBuilder` decuplat de `WorldAdminService`; `StoryContextService` + `NPCContext` folosesc API in loc de implementare).
- Documenteaza suprafata publica minima pentru addonuri si core (`WorldAdminApi` expune 12 proprietati si 30 de metode (11 default + 19 abstracte); `documentatie-api.md` creat cu toate metodele, proprietatile si regulile de consum).

## P1 - World, NPC si spawn

- Stabileste fluxul `WorldAdminService -> WorldContextSnapshotBuilder -> WorldContextSnapshot` (`WorldContextSnapshotBuilder` refactorizat sa primeasca `WorldAdminApi` in loc de `WorldAdminService`; fluxul e transparent si fara dependente directe de implementare).
- Stabileste legaturile persistente NPC -> home/work/social si backfill-ul lor (`bindNpcToHomePlace`, `bindNpcToWorkPlace`, `bindNpcToSocialPlace` expuse ca metode abstracte in `WorldAdminApi`; `WorldAdminService` are `override`).
- Incheaga contractul semantic `Region -> Place -> Node` (`WorldAdminApi` expune interogari simetrice: `findRegionsByType/Tag/World`, `findPlacesByTag/Type/World/Owner/Metadata`, `findNodesByType/World/Metadata`; plus `hasTag` pe `WorldRegionInfo` si `WorldPlaceInfo`).
- Defineste planul de household si backfill-ul persistent (metodele `bindNpcToHome/Work/SocialPlace` expuse in API; `WorldAdminService` implementeaza binding cu metadata `owner_npc_id`, `resident_npc_ids`, `worker_npc_ids`, `social_npc_ids`).

## P2 - Quest, AI, story si GUI

- Stabileste `QuestDirector -> QuestDirectorDecision` si criteriile de selectie (`QuestDirector` cu scoring engine, `QuestDirectorRequest`, `QuestDirectorDecision` — implementat).
- Stabileste rezolvarea ancorelor de quest in raport cu world-ul si NPC-urile (`QuestAnchorResolver` cu `ResolvedQuestAnchor`, fallback-uri, matching pe ID/nume/tag/tip/metadata — optimizat sa foloseasca API-ul).
- Stabileste runtime-ul generic de progres pentru questuri, contracte, datorii si evenimente (`ProgressionService`, `ProgressionDefinition`, `ProgressionSelector`, snapshot-uri GUI — implementat).
- Stabileste contextul narativ pentru AI si story state-ul persistent (`StoryContextService`, `StoryStateService`, `StoryContextSnapshot`, semnale story, story events — implementat).
- Stabileste fluxul AI `OpenAIPromptSnapshotFactory -> OpenAIService -> DialogManager` (implementat cu fallback determinist, politica `AIOrchestrationPolicy`, `AIUseCase`).
- Stabileste ecranele principale GUI si modul de navigare intre ele (`MainHubGui`, `QuestLogGui`, `QuestDetailGui`, `WorldHubGui`, `StoryGui`, `StatsGui`, `NpcInteractionGui`, `NpcManagerGui`, `RoutineGui`, `DebugGui`, `AuditGui`, `ConfirmActionGui`, `QuestAuthoringGui` — implementat).
- Stabileste traseul de conversatie, emotii, relatii si reactii NPC (`ConversationSessionManager`, dialog cu OpenAI, memorii, emotii, relatii persistente, NPC reactii — implementat).
- Scrie clar criteriile de selectie ale directorului de quest (`QuestDirector` scoring engine: `storyDemandSignals` → `definitionTokens` → scor pe signal matching + preferred mechanic → `CandidateScore` sortat descrescator).
- Specifica rezolvarea ancorelor si fallback-urile ei (`QuestAnchorResolver` matcheaza obiective `visit_region/place/node/talk_to_npc` prin ID, nume, tag, tip, metadata; fallback: locatia curenta, primul element din mapping).
- Detaliaza snapshot-ul de progres si progresul generic (`ProgressionGuiSnapshot`, `ProgressionStatusSnapshot`, `ProgressionProgressSnapshot`, `ProgressionStageSnapshot` — modele read-only cu player, selector, obiective, stage-uri, recompense).
- Defineste ce inseamna draft AI si ce ramane validare runtime (`QuestSeed`, `QuestDraft`, `QuestDraftValidator`, `QuestSeedFactory`, `QuestAuthoringService` — flux read-only; draft-ul nu se executa fara validare runtime).
- Fixeaza traseele GUI pentru quest, story si debug (`GuiKey.QUEST` → `QuestLogGui` → `QuestDetailGui`; `GuiKey.STORY` → `StoryGui`; `GuiKey.DEBUG` → `DebugGui`; toate au navigare standard si butoane de actiune).

## P3 - Debug, testare, release si hardening

- Stabileste `DebugDumpService` si ce artefacte scrie in mod standard (`DebugDumpService` scrie: `world-mapping.json`, `npc-world-bindings.json`, `audit.txt`, `config-sanitized.yml`, `recent-server-log.txt`, `quest-audit-report.txt`, `loaded-quest-definitions.json`, `player-progressions.json`, `player-quest-progress.json`, `quest-anchor-bindings.json`, `story-states.json`, `story-events.json`, `npc-summary.json` — implementat).
- Stabileste `RecentEventsBuffer` si ce evenimente sunt retinute pentru inspectie (buffer de evenimente recente in `EventRecorder` sau `RecentEventsBuffer` — log-uri de server, evenimente story).
- Stabileste `WorldMappingSemanticIndex` ca instrument de debugging semantic (`WorldMappingSemanticIndex` exista cu clasa `WorldMappingSemanticIndex.from(...)`, folosit in audit si debugdump — implementat).
- Stabileste checklist-ul de testare, release si recuperare dupa restart (`docs/release-checklist.md` exista cu pasii de build, deploy, backup si rollback).
- Stabileste procedura anti-duplicare NPC si reparatia starii corupte (`/ainpc duplicates`, `/ainpc repair duplicates|households|npc-bindings|mapping-metadata|batch <args> [dryrun|apply]` — implementat).
- Stabileste observabilitatea pentru AI backoff, audit si snapshot-uri de stare (`AIOrchestrationService` cu fallback determinist, `/ainpc audit`, `DebugDumpService`, snapshot-uri story/progression/GUI — implementat).

## P4 - Modularizare, Kotlin si addonuri

- Stabileste limitele clare ale modulului `ainpc-api` fata de core (`ainpc-api` contine doar interfetele publice si modelele read-only; `ainpc-core-plugin` contine implementarile; nicio clasa din API nu depinde de core).
- Stabileste contractele de addon si ordinea de incarcare a descriptorilor (`AddonRegistryApi`, `AddonDescriptor`, `AddonType` in API; `AddonRegistry` in core cu validare, ordonare si filtrare).
- Stabileste regulile Kotlin pentru interop Java si packaging Paper (`kotlin-style-guide.md`, `kotlin-interop-api-addonuri.md`, `kotlin-paper-packaging-si-smoke.md` — toate exista in docs/).
- Stabileste strategia de testare pentru conversii Kotlin si smoke tests (`kotlin-testing-strategy.md` exista in docs/).
- Stabileste directia pentru scenarii programabile si modularizare (`strategie-plugin-modular-si-scenarii-programabile.md` exista in docs/).

## P5 - Istoric si igiena documentatiei

- Pastreaza arhiva Kotlin ca istoric, nu ca sursa de decizie noua (`arhiva/kotlin-migration/` contine documentele de conversie Java->Kotlin, marcate ca istorice).
- Clarifica documentele vechi de quest si spawn fata de documentele curente (`arhiva/questuri-avansate-v1.md` si `arhiva/ordine-spawn-npc-cladiri-region-node-v1.md` exista ca referinta istorica).
- Mentine indexurile de navigare scurte si consecvente (`index-navigare.md`, `start-here.md`, `index-functional.md`, `index-arhiva.md` — toate scurte si centrate pe navigare).
- Pastreaza harta de clase ca index de lucru, nu ca sursa canonica (`harta-clase-index.md` → subharti specifice pe subsistem; fiecare harta e un index, nu un design doc).
- Revizuieste periodic daca un document istoric trebuie mutat in arhiva sau actualizat in documentul canonic (task de igiena; documentele istorice sunt deja in `arhiva/`).
