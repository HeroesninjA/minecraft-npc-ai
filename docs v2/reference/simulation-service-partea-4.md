# Simulation Service - Partea 4

Acesta este runbook-ul de implementare treptata pentru `SimulationService`.

## Scop

- livreaza refactorul in pasi mici, verificabili;
- separa observabilitatea, auditul, semnalele si persistenta optionala;
- evita combinarea refactorului de baza cu gameplay nou in acelasi pas.

## Structura

- baseline;
- refactor fara schimbare functionala;
- observabilitate si debug;
- semnale read-only;
- persistenta optionala;
- consumatori controlati;
- rollout si rollback.
