# Roadmap pentru testare automata cu bot

Status: propunere activa compatibila, neimplementata.
Actualizat: 2026-07-15.

Un client bot poate automatiza scenarii de player pe Paper. In repository nu exista in prezent proiect Node/Mineflayer, `package.json`, runner sau rapoarte produse de un asemenea bot. Ideea ramane activa deoarece nu a fost inlocuita de o alternativa implementata echivalenta.

## Scope propus

- conexiune cu cont dedicat si reconnect controlat;
- executie de chat/comenzi fara dependenta de GUI;
- deplasare si interactiune NPC pentru scenarii selectate;
- verificari quest, progression, story si persistenta;
- capturarea output-ului, timpilor si motivului PASS/FAIL;
- separarea pregatirii fixture-ului de asertiunile scenariului.

## Componente posibile

- client si management de sesiune;
- executor de pasi si asteptari;
- navigator/pathfinding;
- registru declarativ de scenarii;
- colector de loguri si artefacte;
- reporter JSON/Markdown cu redaction.

Numele sau biblioteca exacta nu sunt contract pana la un spike verificat; Mineflayer este o optiune, nu o dependinta actuala.

## Cerinte de siguranta

- cont fara privilegii administrative implicite;
- secrete furnizate extern, fara parole in repo sau rapoarte;
- timeout, cleanup si oprire controlata;
- server/lume izolata si backup restaurabil;
- allowlist de comenzi si interdictie pentru mutatii destructive necerute;
- sanitizarea numelor, chatului, prompturilor AI si endpoint-urilor.

## Criterii pentru mutarea in operations

- proiect si lockfile urmarite;
- cel putin un scenariu determinist cu FAIL real la raspuns invalid;
- raport machine-readable si exit code corect;
- test de reconnect/timeout si cleanup;
- documentarea versiunilor si a limitarilor protocolului;
- integrare optionala in pipeline fara credentiale hardcodate.

## Legaturi

- `operations/demo-server-verification.md`
- `operations/test-fixtures-and-demo-world.md`
- `planning/testare-si-deploy-remote.md`

