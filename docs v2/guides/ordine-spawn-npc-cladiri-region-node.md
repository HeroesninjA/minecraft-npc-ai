# Ordinea reala pentru mapping si spawn NPC

Status: ghid operational verificat in cod.
Actualizat: 2026-10-05 (confirmare, persistenta si limitele generarii).

Spawn-ul activ porneste din mapping semantic existent. Nu exista o etapa automata de constructie a cladirilor.

## Flux recomandat

1. creeaza sau importa `Region -> Place -> Node`;
2. ruleaza `/ainpc world save` dupa mutatiile de mapping;
3. ruleaza `/ainpc world settlement plan <regionId> [maxHouses]`;
4. corecteaza erorile de `HouseAllocationValidator`;
5. ruleaza `/ainpc world settlement spawn <regionId> [maxHouses] --confirm`;
6. verifica household-urile si binding-urile;
7. ruleaza din nou `/ainpc world save` si apoi `/ainpc audit spawn`;
8. inainte de reload/restart, sincronizeaza sectiunea completa `world_admin` salvata in overlay-ul prioritar, apoi verifica persistenta dupa reincarcare.

`plan` este dry-run; nu creeaza NPC-uri si nu cere `--confirm`. Pentru spawn, `--confirm` trebuie sa fie ultimul argument. `maxHouses` limiteaza numarul de case, nu numarul total de NPC-uri.

`world save` scrie in `config.yml`, dar nu actualizeaza overlay-ul. La reload, primul overlay recunoscut inlocuieste intreaga sectiune `world_admin`. Urmeaza [procedura de salvare si sincronizare](../reference/configurare-lume-harta-generare.md) pentru a nu reincarca un mapping vechi.

## Import optional

- `/ainpc world scan village [radius]` este preview;
- scanarea este pusa intr-o coada incrementala comuna si afiseaza numarul de blocuri, bugetul per tick si pozitia in coada; rezultatul apare dupa finalizare;
- pentru import foloseste `/ainpc world scan village <radius> import <regionId> --confirm`;
- `/ainpc world settlement auto <regionId> [maxHouses] --confirm` combina importul cu planificarea alocarilor, dar nu salveaza, nu construieste si nu spawneaza;
- pentru `settlement auto`, poti folosi un ID nou sau o regiune existenta care contine centrul scanarii; regiunea existenta este pastrata, iar importul adauga numai place-uri si node-uri noi;
- importul separat prin `world scan village ... import` ramane strict si refuza o regiune deja existenta.

## Plan si spawn

`HouseAllocationPlanner` selecteaza casele si node-urile, apoi `NpcSpawnOrchestrator` executa dry-run sau spawn.

- NPC-urile existente compatibile pot fi reutilizate;
- rollback-ul sterge numai NPC-urile create in batch-ul curent;
- salvarea household cu eroare SQL sau cu un numar incomplet de rezidenti este raportata ca esec de persistenta, nu ca spawn complet reusit;
- la un esec de persistenta, NPC-urile si relatiile deja create/reutilizate sunt pastrate pentru recuperare; settlement-ul se opreste fara rollback global sau stergerea household-urilor existente;
- binding-ul in mapping se face dupa orchestrator si nu face parte din aceeasi tranzactie;
- `PostSpawnBindingResult` raporteaza separat household-urile/NPC-urile complet legate, scrierile mapping aplicate, randurile `npc_world_bindings` salvate si erorile;
- starea binding-ului este `COMPLETE`, `PARTIAL`, `FAILED` sau `NOT_APPLICABLE`;
- pentru o stare incompleta, comanda spune explicit `Succes partial`, ca NPC-urile raman spawnate si ca nu s-a executat rollback.

## Ce nu intra in flux

- `SettlementPlan` din API nu este consumat;
- `PopulationPlan` narativ poate fi persistat, inspectat si selectat dupa `planId`, dar nu este executat de comanda de spawn;
- `BuildingAutoPlaceService` si Patch Planner creeaza mapping, nu cladiri fizice;
- exista infrastructura interna `WorldGenService` si executori native/WorldEdit, dar comenzile publice de mai sus nu sunt conectate la construirea fizica prin acel serviciu;
- `building_templates.yml` este incarcat la startup/reload pentru auto-place semantic, cu ancore si metadata configurabile; `settlements.yml` nu este un plan executat automat de spawn. Vezi [schema implementata](../reference/template-config-runtime.md).

## Verificari minime

- fiecare casa are node-uri `npc_spawn` si `bed/home` valide;
- node-urile apartin place-ului corect si nu sunt alocate incompatibil;
- workplace-urile si locurile sociale exista in aceeasi semantica;
- dupa spawn, verifica household persistence, `npc_world_bindings`, proprietarul casei, batch status si auditul;
- dupa sincronizarea overlay-ului si restart, verifica aceleasi ID-uri si relatii; nu repeta spawn-ul ca substitut pentru inspectarea unui rezultat partial.

## Surse pentru fluxul actual

- [Politica de confirmare](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandMutationPolicy.kt).
- [Finalizarea persistentei household](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseholdSpawnPersistence.kt).
- [Tratarea esecurilor settlement](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/SettlementSpawnFailureHandler.kt).

## Legaturi

- [Ghid complet de initiere](initiere-lume-harta-structuri-npc-story.md).
- [Exemplu complet de sat](exemplu-complet-sat-npc-story.md).
- [Configurare NPC si asocieri](../reference/configurare-npc-asociere-structuri.md).
- [Settlement plan](../architecture/settlement-plan.md).
- [Households persistente](../architecture/households-persistente.md).
- [NPC world bindings](../architecture/npc-world-bindings.md).
- [Harta claselor spawn](../architecture/harta-clase-spawn.md).
