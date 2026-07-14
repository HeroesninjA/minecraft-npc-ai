# Reducere Marime JAR

Acesta este ghidul pentru reducerea dimensiunii artifactului final al core-ului.

## Concluzie

- marimea JAR-ului este dominata de dependinte impachetate, nu de codul propriu AINPC;
- `sqlite-jdbc` este cauza principala a volumului;
- Kotlin stdlib ramane necesar pentru codul Kotlin;
- optimizarea reala vine din controlul runtime classpath si a shading-ului.

## Directie

- masoara artifactele locale;
- identifica dependintele care umfla jar-ul;
- aplica refactorizare si shading doar cand reduce efectiv livrabilul.
