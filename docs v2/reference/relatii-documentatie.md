# Relatii Documentatie

Acesta este indexul canonic pentru relatiile dintre documentele de proiect.

## Reguli de baza

- fiecare zona are un document canonic;
- documentele de categorie sunt indexuri, nu surse primare;
- documentele istorice raman doar pentru context;
- daca doua documente spun acelasi lucru, unul trebuie marcat drept derivat sau istoric.

## Tipuri de relatie

- `canonic` - stabileste regula sau contractul principal;
- `derivat` - explica sau descompune un document canonic;
- `istoric` - versiune veche pastrata pentru context;
- `index` - document de navigare pentru un grup de pagini;
- `operational` - runbook, checklist sau ghid de executie;
- `dependent` - document care se bazeaza pe altul pentru ordine sau continut.

## Harta principala

- `canonical/constitutie-proiect.md` este baza pentru directie si criterii;
- `canonical/implementat-deja.md` este sursa pentru statusul confirmat in cod;
- `architecture/mapping.md` ramane baza pentru `WorldRegion -> WorldPlace -> WorldNode`;
- `reference/simulation-stack.md`, `reference/gui-stack.md`, `reference/story-context-quest-ai-stack.md` si `planning/quest-evolution-stack.md` sunt puncte de intrare pe zone;
- `operations/audit-constitutie-proiect.md`, `operations/release-checklist.md` si `operations/debugging-si-testare.md` sunt documente operationale dependente.

## Concluzie

- foloseste acest index ca filtru de navigare;
- pastreaza un singur document canonic pe contract;
- muta in arhiva ceea ce a devenit strict istoric.
