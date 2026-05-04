# NPC, Rutine si Simulare

Actualizat: 2026-05-04

Aceasta categorie acopera comportamentul NPC-urilor, rutina, reactiile sociale, NPC-urile temporare si trecerea spre simulare de sat/lume.

## Documente

| Document | Rol |
|---|---|
| `../../simulare-sat-si-lume.md` | Contract de ansamblu pentru simulare de comunitate, resurse, reputatie, evenimente si consecinte |
| `../../environment-context-si-engine.md` | Context read-only de mediu acum si EnvironmentEngine viitor pentru semnale sistemice |
| `../../households-persistente.md` | Model persistent pentru case, rezidenti, familie si rutina home |
| `../../rutine-npc-si-timeline.md` | Rutine zilnice, timeline si hook-uri |
| `../../reactie-npc-jucator.md` | Reactii bazate pe istoric, emotii, relatie si context |
| `../../dialog-si-conversatii.md` | Dialog contextual peste rutina, relatie, memorie, quest si story |
| `../../npc-uri-temporare-si-episodice.md` | NPC-uri temporare, episodice si non-villager |
| `../../implementat-deja.md` | Status confirmat pentru NPC, familie, emotii si simulare |
| `../../ordine-spawn-npc-cladiri-region-node.md` | Legatura dintre spawn, household si rutina |

## Zone neacoperite complet

- Model persistent complet pentru household-uri, nu doar `homePlaceId`, `workPlaceId`, `socialPlaceId`.
- Economie, resurse, reputatie si evenimente regionale ca mecanici runtime.
- `EnvironmentEngine` complet pentru semnale sistemice; pentru faza curenta ajunge context read-only.
- Politici clare pentru cleanup NPC temporari.
- Reguli pentru cand rutina poate fi intrerupta de quest/story.
