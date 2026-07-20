# Patch Planner

Status: contract curent si gap-uri verificate in cod.
Actualizat: 2026-07-18.

Patch Planner analizeaza si completeaza mapping-ul semantic al unei regiuni. Nu este un sistem activ de constructie a satului.

## Flux

`VillageGapAnalyzer -> GapReport -> VillagePatchPlanner -> PatchPlan -> VillagePatchApplier`

- `analyze` detecteaza capacitate lipsa, node-uri lipsa, profesii fara workplace si lipsa hub-urilor;
- `plan` ordoneaza candidatii dupa prioritate, cost, risc si ID;
- `validate` afiseaza acelasi rezultat cu accent pe validare;
- `apply` regenereaza planurile si aplica planul selectat daca nu are erori.

## Capabilitati reale

`PatchPlannerOptions` permite implicit numai `semantic-place-mapping`.

- `ADD_NODE` foloseste `SEMANTIC_ONLY` si poate crea node-uri;
- `ADD_HOUSE`, `ADD_WORKPLACE` si `ADD_SOCIAL_PLACE` cer si `native-block-build`;
- comanda nu ofera aceasta capabilitate, deci planurile respective sunt `BLOCKED` si contin erori;
- `WORLDEDIT_TEMPLATE` nu este produs de planner-ul curent si nu are executor.

## Ambiguitatea `NATIVE_PATCH`

Clasa `VillagePatchApplier` poate primi intern un plan `NATIVE_PATCH` autorizat manual. Chiar si atunci, ea creeaza doar limite de `Place` si node-uri la offseturi sintetice. Runtime-ul afiseaza aceeasi limita prin mesajul comun atat la plan/validate, cat si la apply, iar succesul este numit `patch de mapping semantic`. Nu modifica blocuri Minecraft.

Prin urmare, `native-block-build` si numele `NATIVE_PATCH` nu corespund executiei curente si nu trebuie prezentate drept constructie fizica.

## Persistenta si rollback

- `apply` muta obiectele din `WorldAdminService`;
- salvarea ramane separata prin `/ainpc world save`;
- la eroare, place-urile si node-urile create de apply-ul curent sunt compensate; nu exista undo dupa un apply reusit;
- un plan WorldEdit produce numai warning;
- protectia read-only este centralizata pentru `patch apply` si celelalte rute de mapping mutabile; analiza, planificarea si validarea raman disponibile.

## Backlog valid

1. redenumeste modurile/capabilitatile ca semantice sau implementeaza un constructor real;
2. [x] compenseaza place-urile si node-urile create partial de apply-ul curent;
3. separa planul de identificatorul regenerat la momentul `apply`;
4. [x] afiseaza si impune salvarea/confirmarea consecvent si clarifica faptul ca native patch nu construieste blocuri;
5. pastreaza WorldEdit dezactivat pana exista adaptor si undo testat.

## Legaturi

- `architecture/generare-sate-fara-worldedit.md`
- `architecture/worldedit-integration-contract.md`
- `architecture/settlement-plan.md`
