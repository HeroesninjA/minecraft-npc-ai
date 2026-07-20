# Contractul ProgressionService

Status: contract canonic.
Verificat in cod: 2026-07-15.

`ProgressionService` este stratul comun de indexare, interogare si comanda pentru progresii. Nu este o directie viitoare si nu inlocuieste `ScenarioEngine`, care ramane runtime-ul executabil pentru questuri si obiective.

## Rol implementat

- incarca si cache-uieste `ProgressionDefinition` din definitiile de scenariu eligibile;
- expune definitii, selectori, sugestii de objective ID si snapshot-uri pentru log, GUI si debug;
- citeste progresul persistent si sumarizarile prin `ProgressionRepository`;
- expune cautarea, salvarea si stergerea binding-urilor de ancora;
- delega start, stop, abandon, tracking si progres catre runtime-ul existent;
- raporteaza duplicate, referinte nerezolvate si alte rezultate de audit.

## Ce este o progresie

O definitie este eligibila cand scenariul este un quest sau este marcat pentru progresie si expune cel putin cod, obiective ori recompense relevante. Identitatea comuna foloseste pack-ul, mecanica si definitia, de forma `pack:mechanic:definition`.

Campurile generice precum `mechanicId`, `kind`, `templateId` si `definitionId` permit filtrare comuna. Ele nu demonstreaza singure ca orice mecanica are toate regulile de gameplay implementate.

## Limite de responsabilitate

- `ScenarioEngine` valideaza si executa acceptarea, progresul obiectivelor, stage-urile, recompensele si lifecycle-ul;
- `ProgressionRepository` proiecteaza starea persistata din `player_quests` si `quest_anchor_bindings`;
- `ProgressionService` unifica accesul, dar nu mentine un al doilea runtime paralel;
- serviciul nu genereaza continut cu un model AI si nu publica pack-uri;
- un draft de authoring nu devine progresie incarcata doar pentru ca are un `mechanicId` valid.

## Compatibilitate

- questul ramane fatada si traseul executabil compatibil;
- mecanicile non-quest pot folosi proiectia comuna doar dupa verificarea propriilor conditii, evenimente si recompense;
- ID-urile explicite ale obiectivelor trebuie pastrate pentru a evita migrarea accidentala a progresului.

## Limite cunoscute

- binding-urile de ancora nu au inca un namespace global persistent sigur; vezi `reference/quest-anchor-bindings.md`;
- suprafetele de authoring nu expun toate aceleasi subseturi de obiective;
- genericitatea serviciului nu este criteriu suficient pentru a declara o mecanica jucabila cap-coada.

## Surse tehnice

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression/ProgressionService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression/ProgressionDefinition.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression/ProgressionRepository.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt`

## Documente dependente

- `planning/questuri-avansate-v2.md`
- `reference/progression-events-onboarding-stack.md`
- `reference/quest-anchor-bindings.md`
- `guides/quest-authoring-tutorial.md`
