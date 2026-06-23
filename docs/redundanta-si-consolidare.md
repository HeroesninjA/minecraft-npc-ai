# Redundanta si Consolidare

Actualizat: 2026-06-23

Scopul acestui document este sa identifice zonele de documentatie cu suprapunere mare si sa indice unde merita consolidare, fara a rupe lanturile canonice deja folosite in `docs/relatii-documentatie.md`.

## Criteriu

Un document merita consolidare cand:

- descrie aproape acelasi contract ca alt document;
- repeta aceleasi reguli, dar cu nivel de detaliu diferit;
- nu mai aduce o perspectiva operationala separata;
- poate fi absorbit fara sa piarda un pas important de citire.

## Zone cu suprapunere mare

### 1. Interactiuni, dialog si reactii NPC

Posibil lant de consolidare:

- `interactiuni.md`
- `dialog-si-conversatii.md`
- `reactie-npc-jucator.md`

Observatie:

- documentele sunt deja legate logic, dar contin o parte din acelasi flux player -> NPC -> context -> reactie.
- daca echipa vrea mai putine documente, aici exista cel mai bun candidat pentru unificare intr-un singur contract mare, cu subsectiuni clare.

### 2. AI orchestration si runtime intern

Posibil lant de consolidare:

- `ai-orchestrare-si-mecanici.md`
- `spring-ai-mcp-serviciu-intern.md`

Observatie:

- primul descrie politica si mecanicile AI;
- al doilea descrie runtime-ul intern si sidecar-ul;
- daca se urmareste un singur document de arhitectura AI, aceste doua pot fi reunite intr-un contract AI/sidecar mai mare.

### 3. Story context si generare AI

Posibil lant de consolidare:

- `story-context-service.md`
- `story-si-context-ai.md`
- `generare-automata-questuri-ai.md`

Observatie:

- cele trei doc-uri se ating in zona de context, prompt, quest draft si validare;
- daca apar multe modificari de contract in aceeasi zona, merita o sectiune comuna de "Context -> Draft -> Validator" si restul ca derivari scurte.

### 4. Mapping semantic si ghiduri de completare

Posibil lant de consolidare:

- `mapping.md`
- `mapping-harti-manuale.md`
- `mapping-pentru-implementari-ulterioare.md`

Observatie:

- `mapping-pentru-implementari-ulterioare.md` este deja redirect istoric;
- `mapping-harti-manuale.md` este un ghid operational peste `mapping.md`;
- daca apar inca multe reguli comune, se poate face un singur document canonic de mapping cu anexe operationale scurte.

### 5. Questurile pe etape

Posibil lant de consolidare:

- `questuri-faza-1-stabilizare.md`
- `pregatire-questuri-avansate.md`
- `questuri-avansate-v2.md`

Observatie:

- aici exista deja un lant de evolutie, nu un duplicat accidental;
- totusi, daca documentele continua sa creasca in paralel, merita un index canonic unic cu subsectiuni pe faze in locul a trei documente mari.

### 6. GUI de progres si admin

Posibil lant de consolidare:

- `gui-interfete.md`
- `gui-admin-mapping-quest.md`

Observatie:

- acum exista separare buna intre UI public si admin;
- consolidarea nu este obligatorie, dar daca apar prea multe reguli comune, un ghid UI central cu anexe "public" si "admin" poate reduce duplicarea.

### 7. Simulare NPC

Posibil lant de consolidare:

- `simulation-service.md`
- `simulation-service-partea-2.md`
- `simulation-service-partea-3.md`
- `simulation-service-partea-4.md`

Observatie:

- acestea par mai degraba serie decat duplicare;
- totusi, daca fazele devin stabile, merita transformate intr-un singur contract cu anexe istorice.

## Documente care merita pastrate separate

- `quest-anchor-bindings.md` - contract DB specific, clar separat de story.
- `npc-world-bindings.md` - contract operational specific NPC -> world.
- `settlement-plan.md` - plan de regiune, nu doar mapping.
- `playable-village-ux.md` - criterii de experienta si jucabilitate.
- `documentatie-lipsa.md` - index de lacune, nu contract functional.

## Recomandare practica

Daca se doreste reducerea numarului de documente, ordinea sigura este:

1. unificare `interactiuni.md` + `dialog-si-conversatii.md` + `reactie-npc-jucator.md`
2. unificare `ai-orchestrare-si-mecanici.md` + `spring-ai-mcp-serviciu-intern.md`
3. consolidare `story-context-service.md` + `story-si-context-ai.md` + `generare-automata-questuri-ai.md`
4. revizie pentru `mapping.md` + ghidurile sale operationale

Nu recomand consolidarea agresiva a `questuri-avansate-v2.md` sau `gui-interfete.md` pana nu se stabilizeaza complet contractele lor.
