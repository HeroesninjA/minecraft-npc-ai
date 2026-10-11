# Exemplu complet: sat, fierar și story

Verificat static: 2026-10-05. Exemplu de documentație; nu a fost aplicat unui server.
Prerechizite și explicații: [ghidul de inițiere](initiere-lume-harta-structuri-npc-story.md).

## 1. Rezultatul urmărit

O regiune `sat_documentatie` conține o casă, o fierărie și o piață. Planner-ul generează un rezident, îi atribuie ocupația `fierar`, casa ca home, fierăria ca work și piața ca social. Un eveniment comercial oferă apoi context narativ satului.

| Obiect | ID | Rezultat urmărit |
|---|---|---|
| Regiune | `sat_documentatie` | Tip `settlement`, story de mapping `peaceful` |
| Casă | `sat_documentatie:casa_fierarului` | Un rezident, un spawn, un pat |
| Atelier | `sat_documentatie:fierarie` | Tip `forge`, metadata `profession: fierar`, un workstation |
| Piață | `sat_documentatie:piata` | Tip `market`, un node social |
| NPC | ID și nume returnate la spawn | Identitate generată de planner; nu se presupune un nume fix |
| Eveniment | ID returnat/listat de story | Tip `trade`, publicat explicit |

Fișierul complet este [world-admin.json](../examples/sat-documentatie/world-admin.json). JSON este acceptat de loader-ul de mapping și permite folosirea exemplului fără transcrierea unui bloc din ghid.

## 2. Pregătește terenul și configurația

Lumea `world` trebuie să existe și să fie încărcată. Exemplul folosește o zonă la `x=200..264`, `z=200..264`, cu podele la `y=64` și puncte utilizabile la `y=65`. Adaptează coordonatele la terenul tău înainte de folosire.

Construiește fizic casa, atelierul și piața în volumele din exemplu. Punctele NPC trebuie să aibă suport și spațiu liber; punctul home trebuie să corespundă unei zone de dormit accesibile. Fișierul definește numai semantica și nu plasează blocurile sau patul.

În `config.yml`, verifică:

| Cheie | Valoare pentru acest exercițiu |
|---|---|
| `features.mapping` | `true` |
| `features.generation` | `true` |
| `features.story` | `true` |
| `routine.enabled` | `false` pentru prima verificare |
| `simulation.enabled` | `false` pentru prima verificare |
| `villagers.auto_repopulate.enabled` | `false` pentru a evita popularea suplimentară automată |
| `story.random_events_enabled` | `false`; evenimentul va fi publicat manual |

Nu este necesar să activezi WorldGen sau un provider AI pentru acest exercițiu. Un jucător administrator este necesar pentru operațiile și verificările din joc.

## 3. Încarcă mapping-ul

Pe un server de test fără mapping de păstrat, fișierul exemplu se folosește ca `plugins/AINPC/world-admin.json`. Pe un server cu mapping existent, integrează regiunea `sat_documentatie` în secțiunea `world_admin.regions` a overlay-ului autoritar, păstrând toate regiunile și opțiunile existente.

**Nu înlocui un overlay existent cu exemplul ca metodă de adăugare a unei singure regiuni.** `world-admin.json` are prioritate peste variantele YAML și înlocuiește întreaga secțiune `world_admin` la încărcare. Exemplul nu conține mapping-ul serverului tău.

Înainte de reload salvează orice modificare runtime pe care trebuie să o păstrezi și sincronizeaz-o în overlay conform [procedurii de persistență](../reference/configurare-lume-harta-generare.md). Apoi:

```text
/ainpc reload
/ainpc world region info sat_documentatie
/ainpc world place info sat_documentatie:casa_fierarului
/ainpc world place info sat_documentatie:fierarie
/ainpc world place info sat_documentatie:piata
/ainpc audit world
```

Verifică existența celor trei places și a celor patru nodes în regiunea exemplu. Nu presupune că acestea sunt singurele obiecte globale: există mapping preexistent și un fallback `castel` în loader.

## 4. Planifică și generează rezidentul

```text
/ainpc world household plan sat_documentatie:casa_fierarului 1
```

Rezultatul trebuie să conțină un singur rezident planificat. Alegerea fierăriei ca workplace se bazează pe prioritatea `FORGE` în `SpawnSemanticRules`; piața are prioritate pentru social. Textul ocupației vine din `metadata.profession`, nu doar din tipul `forge`.

Dacă planul are erori, corectează mapping-ul/terenul înainte de spawn. Când planul este valid:

```text
/ainpc world household spawn sat_documentatie:casa_fierarului 1 --confirm
/ainpc world household place sat_documentatie:casa_fierarului
/ainpc world bindings place sat_documentatie:casa_fierarului 20
/ainpc audit spawn
```

Notează numele și ID-ul numeric returnate. Nu executa și `/ainpc create` pentru același rol: acest exercițiu folosește calea household, care creează sau reutilizează NPC-ul din plan.

### Ce trebuie verificat după spawn

| Proprietate | Așteptare |
|---|---|
| Ocupație | `fierar` |
| Home place | `sat_documentatie:casa_fierarului` |
| Work place | `sat_documentatie:fierarie` |
| Social place | `sat_documentatie:piata` |
| Home node | Node-ul patului din casă |
| Work node | `sat_documentatie:fierarie:lucru_01` |
| Social node | `sat_documentatie:piata:intalnire_01` |
| Proprietarul casei | NPC-ul rezolvat din `primaryOwnerNpcKey`, în cazul binding-ului complet |
| Familie | Nu presupune un `familyId` nenul pentru un singur rezident |

Un mesaj de spawn reușit nu substituie verificarea binding-ului. Dacă apare „Succes parțial”, păstrează ID-urile și inspectează datele existente înainte de o nouă încercare.

## 5. Inițiază story-ul

```text
/ainpc story region sat_documentatie
/ainpc story author sat_documentatie trade Sosire_negustor Un negustor a adus metal pentru atelier.
/ainpc story events sat_documentatie 10
/ainpc story place sat_documentatie:fierarie
```

Evenimentul este publicat direct în DB. Descrierea nu transferă automat metal în inventar, nu creează un negustor și nu finalizează vreun quest. Metadata fierăriei `story_state: shortage` rămâne un semnal declarat până este modificată explicit; publicarea textului nu o schimbă singură.

Inspectează contextul cu un jucător online aflat lângă NPC:

```text
/ainpc story context NumeJucator nearest
```

Înlocuiește `NumeJucator`. Contextul trebuie inspectat pentru regiune, NPC și evenimentul recent; lipsa unui rând `RegionStoryState` sau `PlaceStoryState` nu este echivalentă cu lipsa mapping-ului ori a evenimentului.

`quest_hook: supply_delivery` din metadata este un semnal de context, nu un quest deja instalat. Pentru a transforma situația într-un quest, urmează [tutorialul de authoring](quest-authoring-tutorial.md) și verifică separat obiectivele și recompensele.

## 6. Salvează și verifică persistența

```text
/ainpc world save
```

Această comandă scrie mapping-ul actual în `config.yml`. **Actualizează apoi overlay-ul JSON cu întreaga secțiune `world_admin` salvată**, păstrând noile asocieri, owner și metadata. Nu recopia exemplul inițial peste starea de după spawn: exemplul nu conține identitățile NPC generate.

Dacă vrei să activezi rutina, setează `features.routine: true` și `routine.enabled: true` în config. Pentru simulare folosește separat `features.simulation: true` și `simulation.enabled: true`. Consultă [valorile și intervalele](../reference/configurare-npc-asociere-structuri.md), apoi repornește controlat serverul.

După restart:

```text
/ainpc world region info sat_documentatie
/ainpc world household place sat_documentatie:casa_fierarului
/ainpc world bindings place sat_documentatie:casa_fierarului 20
/ainpc story events sat_documentatie 10
/ainpc audit spawn
```

Compară cu ID-urile notate anterior. Rezultatul urmărit este același NPC logic, aceleași asocieri și același eveniment persistent. Dacă ai activat rutina, urmărește deplasarea între ținte în funcție de slotul orar; nu presupune că începe imediat cu deplasarea la muncă.

## 7. Probleme și diagnostic

| Simptom | Verifică întâi |
|---|---|
| Regiunea nu apare | Numele fișierului prioritar, rădăcina `world_admin`, logul de reload |
| Mapping-ul dispare după restart | Overlay nesincronizat cu `config.yml` salvat |
| Zero rezidenți/plan invalid | Tipul casei, node `npc_spawn`, node `bed/home`, capacitatea și geometria |
| Ocupația este `Locuitor` | Metadata `profession` a workplace-ului și workplace-ul ales |
| NPC fără work/social | Existența și tipul fierăriei/pieței, node-urile și rezultatul binding-ului |
| Număr neașteptat de NPC-uri | Entități preexistente descoperite, batch-uri anterioare, repopulare activă |
| NPC nu merge la țintă | Activarea rutinei la startup, slotul orar, pathfinding și accesibilitatea punctului |
| Story apare în events, nu ca state DB | Evenimentul, metadata place și starea persistentă sunt straturi diferite |

Acest exemplu validează static forma datelor și oferă pași pentru testul live. Reușita pe server se stabilește din rezultatele comenzilor, inspecția lumii și verificarea după restart.

Surse: [loader mapping](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/WorldAdminService.kt), [HouseAllocationPlanner](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocationPlanner.kt), [SpawnSemanticRules](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/SpawnSemanticRules.kt), [comenzi story](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandStory.kt).
