# Ordinea reala pentru mapping si spawn NPC

Status: ghid operational verificat in cod.
Actualizat: 2026-07-15.

Spawn-ul activ porneste din mapping semantic existent. Nu exista o etapa automata de constructie a cladirilor.

## Flux recomandat

1. creeaza sau importa `Region -> Place -> Node`;
2. ruleaza `/ainpc world save` dupa mutatiile de mapping;
3. ruleaza `/ainpc world settlement plan <regionId> [maxHouses]`;
4. corecteaza erorile de `HouseAllocationValidator`;
5. ruleaza `/ainpc world settlement spawn <regionId> [maxHouses]`;
6. verifica household-urile si binding-urile;
7. ruleaza din nou `/ainpc world save` si apoi `/ainpc audit spawn`.

## Import optional

- `/ainpc world scan village [radius]` este preview;
- scanarea este pusa intr-o coada incrementala comuna si afiseaza numarul de blocuri, bugetul per tick si pozitia in coada; rezultatul apare dupa finalizare;
- adauga `import [regionId]` pentru a crea mapping;
- `/ainpc world settlement auto <regionId> [maxHouses]` combina importul cu planificarea alocarilor, dar nu salveaza, nu construieste si nu spawneaza;
- pentru `settlement auto`, poti folosi un ID nou sau o regiune existenta care contine centrul scanarii; regiunea existenta este pastrata, iar importul adauga numai place-uri si node-uri noi;
- importul separat prin `world scan village ... import` ramane strict si refuza o regiune deja existenta.

## Plan si spawn

`HouseAllocationPlanner` selecteaza casele si node-urile, apoi `NpcSpawnOrchestrator` executa dry-run sau spawn.

- NPC-urile existente compatibile pot fi reutilizate;
- rollback-ul sterge numai NPC-urile create in batch-ul curent;
- o eroare de persistenta household poate ramane warning, iar spawn-ul poate fi raportat reusit;
- binding-ul in mapping se face dupa orchestrator si nu face parte din aceeasi tranzactie;
- `PostSpawnBindingResult` raporteaza separat household-urile/NPC-urile complet legate, scrierile mapping aplicate, randurile `npc_world_bindings` salvate si erorile;
- starea binding-ului este `COMPLETE`, `PARTIAL`, `FAILED` sau `NOT_APPLICABLE`;
- pentru o stare incompleta, comanda spune explicit `Succes partial`, ca NPC-urile raman spawnate si ca nu s-a executat rollback.

## Ce nu intra in flux

- `SettlementPlan` din API nu este consumat;
- `PopulationPlan` narativ poate fi persistat, inspectat si selectat dupa `planId`, dar nu este executat de comanda de spawn;
- `BuildingAutoPlaceService` si Patch Planner creeaza mapping, nu cladiri fizice;
- WorldEdit nu are adaptor activ.

## Verificari minime

- fiecare casa are node-uri `npc_spawn` si `bed/home` valide;
- node-urile apartin place-ului corect si nu sunt alocate incompatibil;
- workplace-urile si locurile sociale exista in aceeasi semantica;
- dupa spawn, verifica household persistence, `npc_world_bindings`, batch status si auditul.

## Legaturi

- `architecture/settlement-plan.md`
- `architecture/households-persistente.md`
- `architecture/npc-world-bindings.md`
- `architecture/harta-clase-spawn.md`
