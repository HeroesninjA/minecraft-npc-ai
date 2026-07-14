# Generare automata a questurilor cu AI

Status: contract canonic pentru pipeline-ul de drafturi quest.
Actualizat: 2026-07-14.

AI-ul produce drafturi inspectabile; runtime-ul valideaza, iar adminul aproba publicarea.

## Pipeline

1. selecteaza mapping-ul, story context-ul si constrangerile relevante;
2. genereaza un draft conform schemei;
3. valideaza obiectivele, ancorele si dependentele;
4. prezinta erorile si sugestiile pentru revizie;
5. cere aprobarea adminului;
6. scrie un pack versionat care poate fi incarcat controlat.

## Iesiri

- drafturi de quest;
- schite de dialog;
- sugestii de revizie;
- explicatii de validare;
- outline-uri pentru lanturi de quest.

## Limite

- niciun draft nu devine live direct;
- generatorul nu modifica progresul sau story state-ul;
- schema si mapping-ul existent sunt obligatorii.

## Legaturi

- `architecture/story-context-service.md`
- `architecture/story-si-context-ai.md`
- `reference/scenario-pack-schema.md`
