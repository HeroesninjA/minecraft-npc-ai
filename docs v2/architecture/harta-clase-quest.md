# Harta claselor pentru quest si progresie

Status: harta derivata din cod.
Verificat in cod: 2026-07-15.

Aceasta pagina arata responsabilitatile principale. Contractele raman in `architecture/progression-service.md`, `reference/objective-types-reference.md` si `reference/quest-anchor-bindings.md`.

## Executie

- `ScenarioEngine` gestioneaza lifecycle-ul questului, evenimentele de progres, stage-urile, tracking-ul si recompensele;
- `ObjectiveHandlerRegistry` inregistreaza handler-ele celor 12 tipuri canonice;
- clasele din `engine/runtime/objectivehandlers/` aplica incrementarea tipului primit;
- filtrele de tinta si contextul evenimentului raman in traseele `ScenarioEngine` si utilitarele asociate.

## Proiectie si persistenta

- `ProgressionService` expune definitii, selectori, snapshot-uri, audit si comenzi comune;
- `ProgressionDefinition` proiecteaza un scenariu eligibil intr-o identitate generica;
- `ProgressionRepository` citeste progresul si persista binding-urile de ancora;
- progresul executabil ramane detinut de runtime-ul quest, nu de o copie paralela in serviciul generic.

## Semantica world

- `QuestAnchorResolver` rezolva referinte spre region, place, node sau NPC;
- `QuestAnchorBindingService` combina binding-urile persistate cu rezolvarea din definitie;
- `WorldAdminApi` este sursa pentru mapping-ul semantic;
- limitele fallback-ului global si ale `deliver_to_npc` sunt documentate separat.

## Authoring si AI

- `QuestCreateGui` construieste si exporta un draft JSON;
- `QuickQuestGui` construieste preview YAML si un export simplificat in chat;
- `QuestAuthoringService` si `QuestSeedFactory` furnizeaza analiza read-only;
- `QuestDirector` poate selecta candidati sau sugera seed-uri, dar nu publica pack-uri.

## Story

- `StoryContextService` proiecteaza context read-only;
- `StoryStateService` detine scrierile narative validate;
- story-ul completeaza progresul, dar nu il inlocuieste.

## Legaturi

- `architecture/progression-service.md`
- `reference/objective-types-reference.md`
- `reference/quest-anchor-bindings.md`
- `architecture/generare-automata-questuri-ai.md`
- `reference/harta-clase-cod.md`
