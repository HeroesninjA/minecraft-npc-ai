# StoryContextService

Status: canonical in `docs v2`.
Actualizat: 2026-06-29.

Acesta este rezumatul stratului read-only dintre mapping, quest anchors si story state.

## Rol

- construieste context narativ compact pentru NPC, jucator si locatie;
- combina mapping semantic, quest anchors si semnale story;
- expune context pentru AI si GUI fara sa lase AI-ul sa inventeze stare executabila;
- ajuta la briefing si la explicarea starii curente.

## Stare curenta

- serviciul este read-only;
- scrierile controlate trec prin `StoryStateService` si actiuni de quest;
- snapshot-ul include regiune, place, node-uri relevante si evenimente recente;
- exista comenzi admin read-only pentru inspectie si debugdump.

## Reguli

- nu creeaza story state nou;
- nu scrie in DB;
- nu inlocuieste `QuestAnchorResolver`;
- depinde de mapping existent;
- degradeaza cu warnings cand lumea sau datele lipsesc.

## Legaturi

- `architecture/mapping.md`
- `architecture/ai-orchestrare-si-mecanici.md`
- `canonical/implementat-deja.md`
