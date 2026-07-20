# Story, context, quest si AI: ordine de citire

Status: index derivat.
Actualizat: 2026-07-15.

## Ordine

1. `architecture/story-state-service.md` - persistenta region/place si evenimente.
2. `architecture/story-context-service.md` - proiectia read-only si limitele de redactare.
3. `architecture/story-si-context-ai.md` - consumul real in dialog.
4. `architecture/generare-automata-questuri-ai.md` - authoring si drafturi quest.
5. `planning/questuri-avansate-v2.md` - lucrul ramas.

## Regula

- story state nu are model global in serviciul curent;
- `toPromptBlock()` nu inseamna redactare;
- scaffold-ul orchestration nu este pipeline activ;
- acest document doar stabileste ordinea de citire.
