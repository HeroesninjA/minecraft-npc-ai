# Reducere Marime JAR

Status: nota operationala derivata.
Actualizat: 2026-07-15.

Acesta este ghidul pentru reducerea dimensiunii artefactului final al core-ului. Contractul de packaging ramane `reference/kotlin-paper-packaging-si-smoke.md`.

## Concluzie

- marimea JAR-ului este dominata de dependinte impachetate, nu de codul propriu AINPC;
- `sqlite-jdbc` este cauza principala a volumului;
- Kotlin stdlib ramane necesar pentru codul Kotlin;
- optimizarea reala vine din controlul runtime classpath si a shading-ului.

## Directie

- masoara artifactele locale;
- identifica dependintele care umfla jar-ul;
- aplica refactorizare si shading doar cand reduce efectiv livrabilul.

Nu elimina `sqlite-jdbc` sau Kotlin stdlib numai pentru a micsora JAR-ul fara un plan runtime si un smoke Paper care demonstreaza classloading-ul.

## Legaturi

- `reference/kotlin-paper-packaging-si-smoke.md`
- `operations/release-checklist.md`
