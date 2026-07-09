# Taskuri Prioritizate

Status: canonical in `docs v2`.
Actualizat: 2026-07-01.

Acesta este indexul curatat pentru backlog-ul activ. Taskurile finalizate raman separate de lucrul deschis.

## Reguli de lucru

- Nu marca un task ca finalizat fara verificare.
- Pastreaza separat backlog-ul deschis de istoric.
- Orice categorie noua trebuie sa aiba taskuri concrete si verificabile.
- Daca apare contradictie intre stare si continut, continutul si auditul au prioritate.

## Index rapid

- `P0` - fundatie, API si runtime
- `P1` - world, NPC si spawn
- `P2` - quest, AI, story si GUI
- `P3` - debug, testare, release si hardening
- `P4` - modularizare, Kotlin si addonuri
- `P5` - istoric si igiena documentatiei

## Focus curent

### P0 - Fundatie, API si runtime

- traseu clar de bootstrap, reload si shutdown
- persistenta coerenta si schema initiala
- punct principal de intrare in documentatie

### P1 - World, NPC si spawn

- ordinea de spawn: alocare, validare, spawn, bind, persistenta, rollback
- mapping semantic si inspectie pentru duplicate
- rutina dupa spawn si validarea satului semantic

### P2 - Quest, AI, story si GUI

- questuri cap-coada si progression stabil
- story state si context narativ
- GUI si actiuni administrative clare

### P3 - Debug, testare, release si hardening

- smoke, audit, runbook si verificari repetabile
- capturarea erorilor si a regresiilor

### P4 - Modularizare, Kotlin si addonuri

- API public stabil
- compatibilitate addon
- packaging si interop

### P5 - Istoric si igiena documentatiei

- indexuri de navigare
- arhiva si redirecturi
- mutarea documentelor vechi in locuri corecte

## Regula de aliniere

Daca un task intra in conflict cu constitutia proiectului, se ajusteaza taskul, nu constitutia.
