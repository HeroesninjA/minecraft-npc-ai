# NPC, Rutine si Simulare

Actualizat: 2026-05-06

Aceasta categorie acopera comportamentul NPC-urilor, rutina, reactiile sociale, NPC-urile temporare si trecerea spre simulare de sat/lume.

## Documente

| Document | Rol |
|---|---|
| `../../playable-village-ux.md` | Criterii concrete pentru NPC-uri stabile, rutina lizibila si interactiuni clare in primul sat jucabil |
| `../../simulare-sat-si-lume.md` | Contract de ansamblu pentru simulare de comunitate, resurse, reputatie, evenimente si consecinte |
| `../../simulation-service.md` | Contract tehnic pentru serviciul logic de simulare NPC: tick periodic, nevoi, scoring, stare, rutina si persistenta |
| `../../simulation-service-partea-2.md` | Plan de extractie pentru `SimulationService` real: API, comenzi, audit, debugdump, teste, performanta si integrare cu household-uri |
| `../../simulation-service-partea-3.md` | Design avansat pentru semnale de simulare, agregare pe settlement, consumatori quest/story/AI, audit si rollout controlat |
| `../../simulation-service-partea-4.md` | Runbook de implementare si rollout: PR slicing, feature flags, migration, comenzi, teste, smoke test si rollback |
| `../../environment-context-si-engine.md` | Context read-only de mediu acum si EnvironmentEngine viitor pentru semnale sistemice |
| `../../households-persistente.md` | Model persistent pentru case, rezidenti, familie si rutina home |
| `../../rutine-npc-si-timeline.md` | Rutine zilnice, timeline si hook-uri |
| `../../reactie-npc-jucator.md` | Reactii bazate pe istoric, emotii, relatie si context |
| `../../dialog-si-conversatii.md` | Dialog contextual peste rutina, relatie, memorie, quest si story |
| `../../interactiuni.md` | Fluxul concret player-NPC: click, chat privat, sesiuni, passive listen, intentii si efecte sociale |
| `../../gui-interfete.md` | Directie pentru GUI de interactiune NPC, manager NPC, statistici si shop |
| `../../npc-uri-temporare-si-episodice.md` | NPC-uri temporare, episodice si non-villager |
| `../../implementat-deja.md` | Status confirmat pentru NPC, familie, emotii si simulare |
| `../../ordine-spawn-npc-cladiri-region-node.md` | Legatura dintre spawn, household si rutina |

## Zone neacoperite complet

- Model persistent complet pentru household-uri, nu doar `homePlaceId`, `workPlaceId`, `socialPlaceId`.
- Economie, resurse, reputatie si evenimente regionale ca mecanici runtime.
- `EnvironmentEngine` complet pentru semnale sistemice; pentru faza curenta ajunge context read-only.
- Politici clare pentru cleanup NPC temporari.
- Reguli pentru cand rutina poate fi intrerupta de quest/story.
- GUI si audit pentru explicarea rutinei curente si a ancorelor home/work/social.
