# Fixture controlat pentru sat si structuri exterioare

Status: runbook verificat in cod.
Actualizat: 2026-07-18.

Fixture-ul este definit hardcodat in core pentru smoke si demo. Planificarea este read-only, dar comenzile `apply` si `populate` fac mutatii reale si nu sunt un profil izolat automat. Ambele sunt blocate cand runtime-ul MCP raporteaza `read_only`; varianta `populate --dry-run` ramane disponibila.

## Comenzi

- `/ainpc world fixture plan [prefix]` afiseaza planul;
- `/ainpc world fixture validate [prefix]` compara planul cu mapping-ul curent;
- `/ainpc world fixture apply [prefix]` creeaza mapping-ul;
- `/ainpc world fixture populate --dry-run [prefix]` listeaza rolurile NPC;
- `/ainpc world fixture populate [prefix]` spawneaza rolurile lipsa;
- `/ainpc world fixture context [prefix]` afiseaza contextul semantic derivat.

Prefixul implicit este `test_`.

## Ce aplica

`ControlledTestWorldFixtureApplier` foloseste in prezent valori fixe:

- lumea `world`;
- centrul `2000, 2000`;
- inaltimea semantica de baza `Y=64`;
- un sat central si sapte regiuni exterioare;
- place-uri si node-uri sintetice, fara blocuri Minecraft.

Planul declara `mappingOnly=true`, `usesWorldEdit=false` si toate politicile automate de build/spawn/progress ca false. Aceste valori descriu planul si `apply`; comanda separata `populate` poate totusi spawna NPC-uri.

## Populare

Populatorul are 12 roluri hardcodate. El:

- cauta home place-ul si primul node de spawn;
- foloseste centrul casei ca fallback;
- evita duplicarea dupa numele NPC-ului;
- creeaza NPC-ul direct prin `NPCManager`.

Nu creeaza `npc_world_bindings` pentru home/work/social, nu foloseste `NpcSpawnOrchestrator`, nu persista household-uri si nu are rollback de batch.

## Riscuri operationale

- handlerul nu verifica un flag dedicat de profil test/demo;
- `apply` compenseaza automat regiunile, places si nodes create de executia curenta daca apare o eroare;
- nu exista comanda de cleanup sau undo;
- mapping-ul nu este salvat automat;
- `populate` poate lasa un subset de NPC-uri daca apar erori;
- valorile fixe pot coliziona cu o lume reala.

## Procedura sigura

1. foloseste numai o lume de test sau un backup verificat;
2. ruleaza `plan` si `validate`;
3. verifica manual zona din jurul `2000, 64, 2000`;
4. ruleaza `apply`, inspecteaza mapping-ul si apoi `/ainpc world save`;
5. foloseste `populate --dry-run` inainte de `populate`;
6. ruleaza auditul si pastreaza o lista a ID-urilor create pentru cleanup manual.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/fixture/ControlledTestWorldFixturePlanner.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/fixture/ControlledTestWorldFixtureApplier.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/fixture/ControlledTestWorldFixturePopulator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandWorld.kt`

## Legaturi

- `operations/test-fixtures-and-demo-world.md`
- `architecture/structuri-exterioare-satului.md`
- `archive/controlled-test-fixture-schema-concept.md`
