# AI Orchestration si Mecanici Runtime

Status: canonical in `docs v2`.
Actualizat: 2026-05-08.

Acest document rezuma rolul AI-ului: asistenta, nu autoritate finala.

## Regula de baza

AI-ul propune, formuleaza si explica. Runtime-ul valideaza, executa si persista.

## Ce coordoneaza AI-ul

- dialog contextual;
- quest drafturi;
- story drafturi;
- explicatii pentru admin;
- reactii NPC;
- rutine explicate;
- constructie sau generare asistata;
- tool calls validate.

## Ce nu face AI-ul

- nu acorda reward-uri direct;
- nu modifica DB sau world state fara serviciu determinist;
- nu decide progresul questurilor;
- nu trateaza promptul ca sursa de adevar pentru mapping sau story;
- nu executa actiuni riscante fara validare si confirmare.

## Arhitectura recomandata

- `AIOrchestrationService` construieste contextul si alege capabilitatea;
- `AIIntentRouter` trimite intentia catre serviciul potrivit;
- `AIResponseValidator` verifica raspunsul;
- serviciile specializate executa efectele validate;
- contextul vine din mapping, quest, story, NPC si admin tools.

## Stare curenta

- AI este deja folosit ca strat transversal pentru asistenta;
- exista separare intre draft si executie;
- prompturile si output-urile trebuie tratate ca artefacte validate;
- business logic ramane in runtime, nu in model.

## Legaturi

- `canonical/implementat-deja.md`
- `architecture/mapping.md`
- `planning/questuri-avansate-v2.md`
- `guides/gui-interfete.md`
