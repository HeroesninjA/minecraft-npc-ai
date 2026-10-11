# Exemplu: familie, proprietar și doi rezidenți

Verificat static: 2026-10-05. Continuare a [exemplului cu un rezident](exemplu-complet-sat-npc-story.md); configurația nu a fost aplicată unui server.

## 1. Ce adaugă acest exemplu

O casă cu doi rezidenți, două puncte de spawn și două paturi; o fierărie și o fermă pentru muncă; o piață comună. Exemplul separă proprietatea casei de rezidență și verifică două binding-uri distincte.

Configurație completă: [world-admin.json pentru familia demo](../examples/familie-documentatie/world-admin.json).

Regiunea este `sat_familie_demo`, casa este `sat_familie_demo:casa_familiei_demo`. Coordonatele diferă de primul exemplu: `x=400..480`, `z=400..460`, puncte la `y=65`.

Este un exemplu separat, nu o migrare a casei deja populate din primul ghid. Dacă le păstrezi pe amândouă, integrează ambele regiuni în același overlay autoritar. Loader-ul nu combină automat fișierele exemplu.

## 2. Proprietăți configurabile și proprietăți calculate

| Proprietate | Unde se stabilește | Valoare în exemplu |
|---|---|---|
| Capacitatea casei | `metadata.max_residents` | Textul `"2"` |
| Capacitatea efectivă | Numărul perechilor spawn/home | Două spawn-uri și două paturi |
| Numărul cerut | Argumentul `count` al comenzii household | `2` |
| Ocupația la fierărie | Metadata `profession` | `fierar` |
| Ocupația la fermă | Metadata `profession` | `fermier` |
| Ordinea workplace-urilor | `SpawnSemanticRules` din cod | `FORGE`, apoi `FARM`, apoi `MARKET` |
| Proprietarul principal | `HouseAllocationPlanner.primaryOwnerNpcKey` | Primul rezident planificat |
| Familie | Planner pentru mai mult de un rezident | `family_casa_familiei_demo` |
| Roluri familiale | Planner, după index | `father`, `mother` |
| Vârste și genuri inițiale | Planner, după index | `36/male`, `35/female` |

Rolurile `father/mother` sunt convenția curentă a planner-ului chiar și pentru un household fără copil. Nu sunt alegeri făcute de AI și nu sunt chei YAML configurabile prin acest exemplu.

`family.auto_generate` controlează o altă cale de generare. Dacă îl păstrezi `false`, planificarea explicită a unei familii prin household rămâne posibilă; orchestratorul trimite membrii către `FamilyManager.bindSpawnedFamily()`.

Numele NPC și ID-urile DB se citesc din rezultatul efectiv. Nu introduce ID-uri numerice inventate în `owner_npc_id`. Pentru familii noi, folosește ID-uri locale de casă distincte: formula actuală a `familyId` derivă din ID-ul local al casei, nu include ID-ul regiunii.

## 3. Încarcă și validează mapping-ul

Pregătește construcțiile fizice și adaptează coordonatele. Exemplul nu plasează blocuri. Menține inițial rutina, simularea și repopularea automată dezactivate, ca în [primul exemplu](exemplu-complet-sat-npc-story.md).

Integrează regiunea în overlay-ul folosit de server. Nu suprascrie regiunile existente. Înainte de reload, salvează eventualele modificări runtime și respectă [procedura overlay](../reference/configurare-lume-harta-generare.md).

```text
/ainpc reload
/ainpc world region info sat_familie_demo
/ainpc world place info sat_familie_demo:casa_familiei_demo
/ainpc audit world
/ainpc world household plan sat_familie_demo:casa_familiei_demo 2
```

Verifică următoarea alocare în planul rezultat:

| Rol în plan | Spawn | Home | Work | Social |
|---|---|---|---|---|
| Primul rezident | `spawn_01` | `pat_01` | Fierărie, `lucru_01` | Piață, `intalnire_01` |
| Al doilea rezident | `spawn_02` | `pat_02` | Fermă, `lucru_01` | Piață, `intalnire_01` |

ID-urile scurte ale node-urilor sunt calificate cu regiunea și place-ul. Cele două node-uri `lucru_01` aparțin unor places diferite și au ID-uri complete diferite. Punctele spawn/home sunt individuale; locul social este comun.

Dacă planul raportează un singur rezident, verifică perechile de node-uri, nu doar `max_residents`. O valoare mai mare în metadata nu creează punctele lipsă.

## 4. Execută spawn-ul și inspectează separat rezidenții

```text
/ainpc world household spawn sat_familie_demo:casa_familiei_demo 2 --confirm
/ainpc world household place sat_familie_demo:casa_familiei_demo
/ainpc world bindings place sat_familie_demo:casa_familiei_demo 20
/ainpc world place info sat_familie_demo:casa_familiei_demo
/ainpc audit spawn
```

Notează cele două ID-uri numerice reale, apoi inspectează fiecare rezident:

```text
/ainpc world household resident <idNumeric>
/ainpc world bindings npc <idNumeric>
```

Repetă inspecția pentru fiecare ID. Parantezele marchează valori de înlocuit, nu argumente literale. Aceste comenzi de inspecție acceptă ID numeric; nu presupune că și `world bind npc` are aceeași sintaxă.

### Proprietarul nu se schimbă la fiecare rezident

Proprietarul principal este rezolvat din cheia planului, iar `HouseholdOwnerBinding` îl transformă în `npc_<databaseId>`. Atribuirea are loc după binding-urile mapping și persistente ale tuturor rezidenților. Al doilea rezident locuiește în casă fără să înlocuiască proprietarul.

Dacă locuința are deja alt proprietar, conflictul poate produce rezultat parțial. Nici reexecutarea spawn-ului, nici asocierea unui nou rezident nu sunt comenzi implicite de transfer al proprietății.

`familyId`, rândurile household și relațiile din `npc_family` sunt straturi diferite. Verifică și rezultatul legării familiei; simpla prezență a textului `familyId` într-un binding nu dovedește că toate relațiile au fost persistate.

## 5. Story comun și contexte individuale

```text
/ainpc story author sat_familie_demo celebration Ziua_recoltei Familia participa la sarbatoarea satului.
/ainpc story events sat_familie_demo 10
```

Evenimentul aparține regiunii; nu definește două stări story individuale ale NPC-urilor. Pentru a vedea contribuția fiecărui NPC la context, stai lângă el și rulează:

```text
/ainpc story context NumeJucator nearest
```

Repetă lângă celălalt NPC și verifică identitatea afișată. Home poate fi comun, iar work diferă. Evenimentul narativ nu declanșează singur o rutină sincronizată de familie și nu demonstrează deplasarea simultană la sărbătoare.

## 6. Persistență și schimbarea ulterioară a capacității

După rezultate complete, rulează `/ainpc world save`, sincronizează întreaga secțiune salvată `world_admin` în overlay și verifică după restart același household, cele două identități, proprietarul și binding-urile. Nu recopia fișierul exemplu inițial peste mapping-ul de după spawn.

Creșterea `max_residents` de la `2` la `3` nu este o operație automată de adăugare a unui copil. Sunt necesare puncte suplimentare, un nou plan și verificarea impactului asupra populației existente. La fel, reducerea la `1` nu trebuie folosită ca o comandă de ștergere a celui de-al doilea NPC.

Replanificarea poate schimba proprietățile calculate, inclusiv rolurile și familia. De exemplu, planner-ul nu atribuie `familyId` pentru un singur rezident. Acest ghid documentează crearea inițială; nu prezintă reexecutarea cu alt `count` ca migrare sigură a unui household existent.

## 7. Checklist de rezultat

- Mapping: o regiune exemplu, patru places, șapte nodes valide.
- Plan: doi rezidenți, două perechi spawn/home distincte, două workplaces.
- Spawn: două identități create/reutilizate inspectabile, fără erori ignorate.
- Proprietate: un proprietar principal al casei, doi rezidenți.
- Persistență: household, binding-uri, relații și mapping verificate separat.
- Story: eveniment regional vizibil și context verificat pentru fiecare NPC.
- Restart: aceleași identități și asocieri; testul live rămâne de executat pe serverul țintă.

Surse: [HouseAllocationPlanner](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocationPlanner.kt), [NpcSpawnOrchestrator](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/NpcSpawnOrchestrator.kt), [HouseholdOwnerBinding](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/HouseholdOwnerBinding.kt), [contractul binding-urilor](../architecture/npc-world-bindings.md).
