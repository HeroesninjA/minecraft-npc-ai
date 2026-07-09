# Implementat Deja

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acest document descrie ce exista deja in cod si nu trebuie reconstruit de la zero.

## Module existente

- `ainpc-core-plugin` contine majoritatea runtime-ului curent.
- `ainpc-api` expune contractele publice pentru addonuri.
- `ainpc-scenario-medieval` ofera un exemplu de continut si reguli tematice.

## Capacitatile curente

### NPC si interactiune

- NPC-uri persistente, bind-uri si metadata.
- Dialog si interactiune de baza.
- Rutine si integrare cu contextul NPC.

### Quest si progression

- Questuri definite in scenarii si fallback-uri locale.
- Acceptare, refuz, abandon, status, reset si completare.
- Persistenta progresului si view-uri read-only pentru audit.
- Selectori de progres si snapshot-uri pentru GUI/debug.

### Story si context

- Story state si event tracking.
- Context narativ read-only pentru quest anchors.
- Briefing si rezumate de progres.

### GUI si admin

- GUI pentru inspectie si operatii curente.
- Actiuni rapide pentru status, tracking, abandon si debug.
- Comenzi read-only de audit si debugdump.

### Mapping si world

- Regiuni, places, nodes si ancore semantice.
- Inspectie pentru world mapping si bindings.
- Fluxuri de spawn si validare de baza.

### AI si orchestration

- Orchestrare initiala asistata.
- AI folosita ca strat de draft, rezumat si asistenta.
- Separare intre logica determinista si asistenta AI.

### Date si audit

- Stocare persistenta pentru starea curenta.
- Debugdump-uri si rapoarte de audit pentru diagnostic.
- Scripturi de smoke si verificare pentru cazuri cheie.

## Ce este partial

- Modularizarea publica este in curs.
- Runtime-ul extensibil pentru scenarii mai complexe este in curs.
- Mapping-ul si world simulation-ul au baza, dar mai au nevoie de consolidare.
- API-ul si addon modelul au infrastructura, dar nu sunt inca finalizate ca suprafata stabila completa.

## Ce trebuie evitat

- Reimplementarea unor servicii deja existente fara motiv clar.
- Dublarea logicii dintre quest, progression si story.
- Folosirea documentelor vechi ca sursa unica de adevar cand exista v2.

## Regula de lucru

Daca o functionalitate exista deja, documentul v2 trebuie sa o trateze ca punct de plecare, nu ca backlog nou.
