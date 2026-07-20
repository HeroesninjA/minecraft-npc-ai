# Consolidari ramase pentru satul semantic

Status: roadmap verificat fata de cod; toate consolidarile din prioritatea ridicata sunt inchise.
Actualizat: 2026-07-18.

Mapping-ul, binding-urile persistente, household-urile si rutina exista deja. Backlog-ul valid este integrarea si siguranta dintre aceste piese, nu recrearea lor.

## Prioritate ridicata

- [x] aplica protectia runtime read-only tuturor comenzilor care muta mapping sau spawneaza fixture-uri; politica centrala acopera `map confirm`, `patch apply`, `building auto-place`, CRUD-ul world, importul, demo/bind/save, fixture apply/populate, household spawn si settlement auto/spawn, pastrand planurile si dry-run-urile disponibile;
- [x] adauga tranzactie sau compensare pentru import, patch, auto-place si fixture apply; `WorldMappingCompensator` elimina in ordine inversa numai ID-urile create de operatia curenta, iar rezultatul pastreaza si raporteaza orice element care nu a putut fi retras;
- [x] cere confirmare si afiseaza consecvent `/ainpc world save` dupa orice mutatie; `AINPCCommandMutationPolicy` cere `--confirm` pentru mutatiile CLI, trateaza `map confirm` drept confirmare explicita si `world save` drept pas sigur separat, iar dispatcher-ul afiseaza un singur reminder cand mapping-ul ramane nesalvat; GUI-ul adauga flag-ul dupa dialog si deschide confirmare generica pentru butoanele mutante directe;
- [x] clarifica mesajele `settlement auto`, `building auto-place` si `native patch`, care creeaza semantica, nu blocuri; formatterul comun `semanticMappingOnlyNotice` numeste artefactele produse si spune explicit ca operatia nu construieste sau modifica blocuri fizice, iar mesajele de succes folosesc `mapping semantic` in loc de `sat/patch generat` ambiguu;
- [x] corecteaza `settlement auto`: generatorul activeaza reutilizarea opt-in a regiunii cerute, mapper-ul valideaza lumea si centrul scanarii, pastreaza regiunea si tag-urile existente si compenseaza numai place-urile/node-urile create de import; importul standard continua sa respinga duplicatele;
- [x] raporteaza explicit succesul partial al binding-ului post-spawn: `PostSpawnBindingResult` agrega household-uri/NPC-uri complete, scrieri mapping, binding-uri persistente si erori, iar comenzile `household spawn` si `settlement spawn` disting `COMPLETE`, `PARTIAL`, `FAILED` si `NOT_APPLICABLE`; la rezultat incomplet spun explicit ca NPC-urile raman spawnate si ca nu s-a executat rollback.

## Planuri si populatie

- [x] pastreaza DTO-ul API `SettlementPlan` drept scaffold compatibil si marcheaza agregatul `@Deprecated(WARNING)`; nu exista executor/provider/repository public, `ReplaceWith` sau termen de eliminare, iar runtime-ul continua sa foloseasca intern `HouseAllocation`;
- [x] persista `PopulationPlan` in fisiere JSON schema v1 cu replace atomic si timestamp-uri, permite `list`, `inspect <planId>` si selectie persistenta per regiune prin `select <planId>`; selectia ramane deliberat neconectata la spawn;
- [x] pastreaza `socialRole`, `ageGroup`, `routineProfile`, `questRole` si `backstorySeed` prin `PopulationPlan -> HouseAllocation -> NpcSpawnPlan`, include-le in hash-ul de confirmare si salveaza-le ca metadata `npc_profiles.profile_data.narrative`; rutina si quest-ul raman neexecutate;
- [x] elimina tema `medieval` hardcodata din preview-ul core: rezolva metadata din tagul regional `theme:<id>` si foloseste `generic` cu avertisment cand tagul lipseste sau este invalid; tema ramane descriptiva si nu activeaza feature-pack-uri.

## World si performanta

- [x] muta scanarea vanilla intr-o coada FIFO incrementala pe schedulerul sincron Bukkit; toate cererile impart bugetul global configurabil `world_admin.scan.blocks_per_tick`, cursorul reia volumul fara duplicate, iar importul sau planificarea pornesc numai dupa raportul complet, fara acces Bukkit off-thread;
- valideaza coordonatele sintetice ale patch-urilor si template-urilor fata de teren;
- adauga cleanup/undo pentru fixture-ul controlat;
- separa fixture-ul hardcodat de runtime-ul de productie cand exista un addon demo stabil.

## Constructie fizica

- nu declara `native-block-build` disponibil pana cand exista un executor de blocuri;
- nu activa WorldEdit pana cand adaptorul are preview, undo si persistenta rezultatului;
- mapping-ul trebuie creat dupa succesul constructiei sau compensat atomic.

## Criteriu de inchidere

Un flux de sat este complet numai cand planul selectat, mapping-ul, spawn-ul, binding-ul si persistenta au un rezultat comun, inspectabil si compensabil. Runtime-ul curent nu indeplineste inca aceasta conditie end-to-end.

## Legaturi

- `architecture/settlement-plan.md`
- `architecture/generare-sate-fara-worldedit.md`
- `architecture/generare-populatie-narativa.md`
- `architecture/worldedit-integration-contract.md`
- `planning/generare-sate-worldedit-si-npc.md`
- `planning/schema-authoring-structuri-world.md`
