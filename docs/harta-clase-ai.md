# Harta claselor pentru AI

Actualizat: 2026-06-21

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste numai subsistemul `AI`: generarea de prompturi, orchestrarea apelurilor, dialog, context narativ si impactul asupra progresiei.

Nu este un inventar complet al tuturor claselor din AI. Este o harta de lucru pentru nodurile care decid cum curge contextul catre model si cum se intoarce rezultatul in joc.

## Noduri principale

- `OpenAIService` -> transport, diag, fallback si istoric al interactiunilor
- `OpenAIPromptSnapshotFactory` -> construieste snapshot-ul folosit pentru prompt
- `DialogManager` -> fluxul complet de dialog, relatii, emotii si persistenta
- `AIOrchestrationService` -> politica de orchestrare si decizia intre cai
- `StoryContextService` -> context narativ, semnale si ancore pentru AI
- `ProgressionService` -> progresie, ancore, obiective si snapshot-uri pentru flow-uri AI-asistate
- `PromptSnapshot` -> imaginea compacta a contextului de prompt
- `DialogHistory` -> istoricul recent de dialog
- `NPCRelationship` -> relatiile care influenteaza raspunsul
- `NPCEmotions` -> starea emotionala a NPC-ului

## Flux principal

`StoryContextService` -> `OpenAIPromptSnapshotFactory` -> `PromptSnapshot`

`DialogManager` -> `OpenAIService` -> raspuns sau fallback

`DialogManager` -> `NPCRelationship` / `NPCEmotions` / `DialogHistory`

`AIOrchestrationService` decide ce cale de executie primeste cererea.

`ProgressionService` furnizeaza contextul pentru mecanici cu progres care pot alimenta AI-ul.

## Relatii utile

- `OpenAIService` retine configuratia, diagnosticul si limitarile offline.
- `OpenAIPromptSnapshotFactory` este stratul de compunere a snapshot-ului pentru prompt.
- `DialogManager` este stratul care leaga istoricul de dialog, relatiile, emotiile si persistenta.
- `StoryContextService` alimenteaza AI-ul cu semnale de lume, quest si story.
- `ProgressionService` este util cand AI-ul trebuie sa vada progresul curent, nu doar conversatia.

## Cum se citeste

1. Incepe cu `OpenAIService` pentru transport si fallback.
2. Continua cu `OpenAIPromptSnapshotFactory` si `PromptSnapshot` pentru contextul transmis modelului.
3. Treci la `DialogManager` pentru ciclul complet de conversatie.
4. Foloseste `StoryContextService` si `ProgressionService` cand vrei context narativ si progres.
5. Foloseste `AIOrchestrationService` cand vrei sa intelegi decizia de executie.

## Nota

Daca vrei doar traseul intre module si pachete, foloseste harta de pachete. Daca vrei relatiile intre clasele AI, acesta este documentul potrivit.
