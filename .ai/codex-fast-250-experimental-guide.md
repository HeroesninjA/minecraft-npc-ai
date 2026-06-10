# Codex Fast 250 Experimental Guide

Data: 2026-05-26
Backlog: `.ai/codex-fast-250-experimental-backlog.md`
Scope: F001-F250

FAST_PROGRAMMING_GUIDE=true
EXPERIMENTAL_ONLY=true
NOT_RELEASE_READY=true

## Scop

Seria F001-F250 este un experiment pentru avans rapid in cod, cu verificari mai putine si feedback mai scurt pe fiecare sesiune. Nu este o promisiune de calitate release si nu inlocuieste gate-urile Paper, audit, debugdump sau restart.

Obiectivul este sa produca repede variante interne, helper-e, validatoare, rapoarte si imbunatatiri mici care pot fi curatate ulterior.

## Cum Se Citeste Backlog-ul

| Coloana | Sens |
| --- | --- |
| ID | task fast, de la F001 la F250 |
| Area | zona tehnica: commands, world, quest, debugdump etc. |
| Mode | `fast-code` pentru cod sau `fast-doc` pentru documentatie/raportare |
| Risk | `LOW`, `MED`, `HIGH` |
| Scope | schimbarea propusa |
| Min check | verificarea minima obligatorie |

## Reguli De Fast Programming

- Nu se lucreaza direct pe release public din seria fast.
- Nu exista task cu verificare zero.
- `force code length` inseamna ca o sesiune poate livra mai mult cod, nu ca poate ignora compilarea.
- Taskurile `HIGH` nu se inchid cu `doc marker` simplu.
- Work-ul distructiv ramane interzis fara cerere explicita.
- Orice schimbare care atinge persistenta, debugdump, quest reward, release sau restart cere test tintit, script run, smoke command sau review note.
- Dupa o sesiune fast, raportul trebuie sa spuna ce a ramas netestat.

## Marimi Recomandate De Sesiune

| Tip sesiune | Numar taskuri | Verificare minima |
| --- | --- | --- |
| Micro fast | 3-5 | compile sau test tintit |
| Standard fast | 6-10 | test tintit pe zona schimbata |
| Large fast | 11-20 | test tintit + raport batch |
| Tracking only | pana la 50 | fara implementare masiva, doar triage/index |

Nu se recomanda implementarea reala a 250 taskuri intr-o singura sesiune. Cele 250 sunt backlog interpretabil, nu batch de apply automat.

## Flux Recomandat Pentru O Sesiune

1. Alege 3-10 taskuri apropiate ca zona.
2. Evita sa amesteci `HIGH` cu release sau restart in aceeasi sesiune mare.
3. Scrie codul rapid, dar tine schimbarile intr-un modul clar.
4. Ruleaza verificarea minima din coloana `Min check`.
5. Scrie un raport batch in `.ai/`.
6. Noteaza explicit testele sarite si gate-urile Paper ramase.

## Promovare Din Fast In Cod Stabil

Un task fast poate fi promovat doar daca:

- build-ul trece;
- exista test tintit sau smoke Paper pentru comportament live;
- nu expune secrete;
- nu creeaza API public accidental;
- documentatia afectata este actualizata;
- decizia de release este `KEEP_INTERNAL`, `REMOVE` sau `PROMOTE`.

## Cand Se Sterge Codul Fast

Sterge sau izoleaza codul fast daca:

- dubleaza logica existenta fara castig clar;
- a fost generat doar pentru experiment;
- nu are test si atinge date persistente;
- adauga comenzi publice instabile;
- face output prea mare sau greu de mentinut;
- nu poate trece prin cleanup rezonabil.

## Comenzi De Verificare

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.CodexDemoBacklogTest
.\gradlew.bat test assemble
```

Pentru evidence Paper:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\validate-demo-paper-evidence.ps1 -EvidenceFile ".ai\codex-250-demo-paper-evidence-template.md" -AllowPending
```

## Raport Minim Dupa Sesiune

```text
Fast batch:
Taskuri:
Fisiere schimbate:
Verificari rulate:
Verificari sarite:
Riscuri ramase:
Paper gates ramase:
Decizie: KEEP_INTERNAL
```

## Stop Conditions

Opreste sesiunea fast daca:

- build-ul nu mai compileaza;
- testul de backlog pica;
- apare posibil secret in debugdump/log/evidence;
- o comanda devine distructiva implicit;
- codul experimental ajunge in API public fara marker;
- un task `HIGH` ramane fara verificare tehnica.
