# Codex Release Freeze Checklist

Data: 2026-05-26
Scope: T231-T250
Marker: EXPERIMENTAL_NOT_PUBLIC_API=true

## Release Rules

- Nu promova modurile experimentale ca API public.
- Nu publica release fara API freeze explicit.
- Nu lasa task packs mari in cod runtime daca raman doar pentru analiza.
- Pastreaza rapoartele in `.ai/` si documentatia in `docs/`.

## Hygiene Gates

| ID | Gate | Expected |
| --- | --- | --- |
| T231 | marcheaza experimental in final | user informat |
| T232 | nu promova experimental ca API | risc redus |
| T233 | ruleaza `clean test assemble` dupa bulk patch | build verde |
| T234 | pastreaza task-uri in `.ai` | runtime neafectat |
| T235 | separa docs de cod runtime | diffs clare |

## Cleanup Gates

| ID | Gate | Expected |
| --- | --- | --- |
| T236 | sterge moduri experimentale inutile inainte de release | suprafata redusa |
| T237 | compacteaza comenzi experimentale pastrate | cod mentenabil |
| T238 | adauga teste pentru comenzi pastrate | coverage real |
| T239 | muta task packs in resurse daca raman mari | cod mai curat |
| T240 | extrage model comun pentru task rows | duplicare redusa |

## Reporting Gates

| ID | Gate | Expected |
| --- | --- | --- |
| T241 | genereaza raport dupa fiecare experiment | trasabilitate |
| T242 | noteaza warnings neblocante | context clar |
| T243 | noteaza fisiere modificate | review usor |
| T244 | noteaza comenzi rulate | reproducibil |
| T245 | noteaza ce nu a fost testat pe Paper | risc clar |

## Planning Gates

| ID | Gate | Expected |
| --- | --- | --- |
| T246 | grupeaza task-uri pe P0/P1/P2 | prioritizare clara |
| T247 | transforma task-urile in milestones | roadmap clar |
| T248 | identifica blocaje reale dupa demo Paper | backlog curatat |
| T249 | arhiveaza experimentele nereusite | repo curat |

## Final Release Gate

| ID | Gate | Expected |
| --- | --- | --- |
| T250 | freeze API inainte de publicare | release controlat |

## Required Commands Before Release

```text
.\gradlew.bat clean test assemble
powershell -ExecutionPolicy Bypass -File .\scripts\release-api-addon-freeze.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\release-report.ps1
```

## Paper Evidence Required

- startup log cu plugin enabled
- `/ainpc demo status demo_sat`
- `/ainpc audit all`
- `/ainpc debugdump all`
- restart gate trecut de doua ori consecutiv

## Cleanup Decision

Inainte de release, fiecare comanda `experimental*` trebuie marcata ca:

- `REMOVE`: nu mai este utila dupa demo intern
- `KEEP_INTERNAL`: ramane ascunsa/documentata ca instabila
- `PROMOTE`: devine comanda stabila doar dupa rename, docs si teste dedicate
