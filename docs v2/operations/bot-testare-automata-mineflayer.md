# Bot Testare Automata Mineflayer

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Plan pentru un bot automat de test care ruleaza scenarii prin chat si pathfinding.

## Rol

- executa comenzi text in locul GUI-ului;
- valideaza quest flow si interactiuni NPC;
- raporteaza pass/fail si timpi;
- poate rula smoke tests si scenarii punctuale.

## Structura

- `client.js` pentru conexiune si reconnect;
- `commander.js` pentru comenzi si raspunsuri;
- `navigator.js` pentru deplasare;
- `reporter.js` pentru raportare;
- `registry.js` pentru incarcarea quest-urilor.

## Folosire

- bun pentru verificari automate si regresii simple;
- util cand vrei testare fara interventie manuala;
- nu inlocuieste testele de integrare din server.

## Legaturi

- `operations/test-fixtures-and-demo-world.md`
- `guides/quest-authoring-tutorial.md`
