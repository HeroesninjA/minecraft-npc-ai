# Concept istoric: extractie SimulationService

Status: superseded de runtime-ul distribuit implementat.
Arhivat: 2026-07-15.

Acest snapshot consolideaza fostele pagini `Simulation Service - Partea 2`, `Partea 3` si `Partea 4`. Ele descriau un refactor posibil, nu clase sau fluxuri existente.

## Directia propusa

- extragerea unui orchestrator `SimulationService`;
- introducerea `SimulationTickSummary`, `SimulationNpcSnapshot` si `SimulationPolicy`;
- semnale validate pentru quest, story, dialog si debug;
- agregare sau cooldown pe regiune si household;
- rollout gradual: baseline, refactor fara schimbare functionala, observabilitate, semnale, persistenta optionala si consumatori.

## Motivul arhivarii

- simularea si rutina sunt deja implementate ca fluxuri distincte;
- extragerea unui serviciu unic ar concura cu aceasta alternativa pana la o decizie explicita de refactor;
- nu exista consumatori regionali ai unor semnale de simulare;
- seria era prezentata in Reference ca traseu activ si putea fi confundata cu starea implementata.

Directia nu a fost arhivata doar fiindca clasele propuse lipsesc, ci fiindca runtime-ul distribuit este alternativa implementata si documentata canonic.

## Inlocuit de

- `../architecture/simulation-service.md` pentru runtime-ul verificat;
- `../reference/simulation-stack.md` pentru navigare;
- `../planning/rutine-npc-si-timeline.md` pentru munca ramasa.

Continutul arhivat nu este contract de implementare.
