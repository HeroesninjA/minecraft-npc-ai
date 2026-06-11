# Codex Session Safe Capacity

Data: 2026-05-26
Context: test experimental pe backlog-ul Prim Demo

## Rezultat Practic

SAFE_EXECUTION_LIMIT=10-20
SAFE_TRACKING_LIMIT=99
LIVE_GATE_REQUIRED=true

O sesiune poate urmari multe task-uri daca task-urile sunt tratate ca plan verificabil, dar numarul de task-uri executate efectiv in cod trebuie tinut mai jos.

## Praguri Recomandate

| Mod sesiune | Numar sigur | Ce inseamna |
| --- | ---: | --- |
| Implementare reala cu cod si teste | 10-20 | Schimbari concrete, testate local, raport scurt. |
| Audit/triage cu raport machine-readable | 50-100 | Fara promisiune ca toate sunt implementate; fiecare rand are status si gate. |
| Planificare/backlog static | 250+ | Acceptabil daca exista parser/test pentru forma si daca nu se amesteca cu runtime. |
| Paper live/restart/release | 3-8 | Necesita operator, server real, dovezi si rerulare dupa restart. |

## Regula De Siguranta

- Task-urile `LOCAL` pot fi inchise in sesiune daca exista test/build/documentatie.
- Task-urile `MIXED` pot primi acoperire locala, dar raman deschise pana trece gate-ul Paper.
- Task-urile `LIVE` nu se declara finalizate fara server Paper, restart, entitati sau export real.

## Observatie Din Test

Batch-ul de 99 task-uri T101-T199 a fost sigur pentru tracking si validare de forma.
Batch-ul final T200-T250 are doar 51 task-uri, dar riscul real e mai mare pentru ca include debugdump, restart si release gates.

Concluzie: pentru acest repo, limita practica sigura pe sesiune este 10-20 task-uri implementate sau pana la 99 task-uri urmarite in raport verificat automat.
