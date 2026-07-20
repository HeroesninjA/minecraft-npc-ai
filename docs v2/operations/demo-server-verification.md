# Verificare consolidata a serverului demo

Status: checklist operational canonic.
Actualizat: 2026-07-15.

Acest document inlocuieste cele sapte pagini D1-D9 redundante. Continutul lor ramane activ si compatibil; a fost consolidat, nu arhivat.

## Reguli de evidenta

- inregistreaza commitul, versiunile Java/Paper, backend-ul, feature flags, lumea, `regionId` si playerul;
- pastreaza output-ul real, nu doar comanda executata;
- `PASS` necesita rezultat observabil si criteriul indeplinit;
- foloseste `N/A` cu motiv pentru o functie dezactivata sau scoasa explicit din scope;
- orice stacktrace, timeout, raspuns de usage neasteptat sau stare pierduta inseamna `FAIL` pana la diagnostic;
- nu salva secrete in raport.

## D1 - Startup si artefacte

Executa `/plugins`, `/ainpc` si `/ainpc audit db`. Confirma core-ul si addonul optional ca enabled, absenta API JAR din `plugins/`, incarcarea definitiilor asteptate si lipsa erorilor critice de startup/DB.

## D2 - Mapping

Pentru fixture-ul demo curent pot fi folosite, dupa verificarea zonei:

```text
/ainpc world demo create <regionId>
/ainpc world places <regionId>
/ainpc world save
/ainpc audit world
```

Confirma regiunea, places/nodes necesare si persistenta dupa restart. Crearea mapping-ului semantic nu dovedeste constructia fizica a cladirilor.

## D3 - Populatie si bindings

```text
/ainpc world settlement plan <regionId> 5
/ainpc world settlement spawn <regionId> 5
/ainpc list
/ainpc audit npc
/ainpc debugdump npcbound
```

Confirma planul, NPC-urile create si home/work/social bindings. Verifica manual duplicatele sau referintele lipsa; auditul NPC nu acopera singur integritatea completa.

## D4 - Rutina si UX

Din joc, cu functiile activate:

```text
/ainpc routine status nearest
/ainpc routine tick
/ainpc gui
```

Confirma sumarul rutinei, interactiunea click-dreapta si GUI-ul. Comportamentul vizual trebuie observat; un raspuns de comanda nu dovedeste pathfinding corect.

## D5 - Quest si progresie

```text
/ainpc quest nearest
/ainpc quest accept nearest
/ainpc quest status nearest
/ainpc progression definitions
/ainpc progression stored <player>
```

Confirma acceptarea, progresul observabil si persistenta dupa restart. Alege o definitie incarcata real si pastreaza output-ul etapelor.

## D6 - Story context

```text
/ainpc story context
/ainpc story events
/ainpc debugdump story
```

`debugdump story` este sumar in chat/consola, nu export de fisiere. Confirma starea si cel putin un eveniment produs de scenariul testat.

## D7 - Dialog si fallback AI

Executa interactiunea reala cu NPC-ul si verifica raspunsul, fallback-ul si logul fara stacktrace. Foloseste `/ainpc debugdump ai` numai intr-un mediu controlat; output-ul poate contine nume si fragmente de prompt/raspuns.

## D8 - Restart si persistenta

Opreste Paper controlat, pastreaza logul, reporneste si repeta inspectiile mapping, NPC bindings, quest/progression si story. Confirma ca restartul nu pierde stare si nu dubleaza inregistrari.

## D9 - Gate final

```text
/ainpc audit all
/ainpc debugdump all [player]
```

Revizuieste manual folderul `plugins/AINPC/debug-dumps/debug-dump-<timestamp>/`, elimina secretele si ataseaza numai artefactele necesare. Completeaza backup/restore, raportul de release si decizia `release` sau `hold`; auditul verde singur nu este suficient.

## Automatizare

Un executor RCON care trimite comenzile nu confirma gate-urile. `scripts/validate-demo-paper-evidence.ps1` verifica forma raportului, nu adevarul output-ului. Roadmap-ul pentru automatizare bot ramane `planning/testare-automata-bot.md`.

## Legaturi

- `operations/playable-village-runbook.md`
- `operations/debugging-si-testare.md`
- `operations/release-checklist.md`
- `operations/migration-si-backup.md`

