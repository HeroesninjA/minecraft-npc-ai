# Questuri Avansate V2

Status: canonical in `docs v2`.
Actualizat: 2026-05-08.

Acesta este planul curatat pentru evolutia questurilor si a mecanicilor de progres.

## Obiectiv

- questurile clasice raman functionale;
- mecanicile noi pot fi quest, contract, duty, bounty, event, tutorial sau ritual;
- toate folosesc aceeasi infrastructura de persistenta, audit si observabilitate;
- fiecare pas nou trebuie sa fie jucabil sau verificabil.

## Faze

### V2.0 - Arhivare si compatibilitate

- documentul vechi ramane doar in arhiva;
- indexurile trimit catre V2.

### V2.1 - Diversitate controlata

- introduce tipuri variate de questuri;
- pastreaza obiective simple si auditable.

### V2.2 - Mapping semantic

- ancorele si locurile semantice devin infrastructura de gameplay;
- coordonatele brute nu mai sunt sursa principala de adevar.

### V2.3 - Mecanici non-quest

- progresul comun acopera si mecanici non-quest;
- comenzile si GUI-ul pot filtra dupa mecanica.

### V2.4 - ProgressionService

- quest runtime ramane compatibil;
- progresul comun se muta treptat intr-un serviciu generic.

### V2.5 - Story, reputatie, economie

- obiectivele pot produce efecte narative sau sociale validate.

### V2.6 - UX, comenzi si observabilitate

- log, status, GUI si debugdump trebuie sa spuna acelasi lucru.

### V2.7 - Demo Paper jucabil

- slice-ul final trebuie sa poata fi demonstrat cap-coada pe server.

## Reguli

- un tip nou necesita definitie, persistenta, audit si test;
- AI propune, runtime valideaza;
- fiecare mecanica trebuie sa fie dezactivabila sau izolabila.

## Legaturi

- `canonical/implementat-deja.md`
- `architecture/mapping.md`
- `architecture/ai-orchestrare-si-mecanici.md`
- `guides/gui-interfete.md`
