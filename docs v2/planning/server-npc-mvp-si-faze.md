# Server NPC MVP si Faze Ulterioare

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este planul pentru un server NPC minim functional si fazele ulterioare.

## MVP

- pluginul porneste fara erori repetate;
- DB se initializeaza si supravietuieste restartului;
- exista mapping minim;
- NPC-urile pot fi create sau spawnate si inspectate;
- rutina este vizibila;
- dialogul are fallback;
- un quest sau progression minim este jucabil;
- adminul poate rula audit, debugdump, backup si rollback operational.

## Faze ulterioare

- playable village hardening;
- populatie narativa;
- questuri si progression mai mature;
- runtime extensibil pentru scenarii;
- story, reputatie si reactii;
- economie si reward-uri extinse;
- generare si patch planning;
- scalare si performanta;
- release public controlat.

## Regula

- MVP-ul este un loop mic, verificabil si repetabil;
- nu se blocheaza pe sisteme mari inainte de baza stabila.

## Legaturi

- `planning/prim-demo-functionalitate-minima-diversa.md`
- `planning/roadmap-orientativ.md`
- `operations/server-admin-runbook.md`
