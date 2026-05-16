# WorldEdit Integration Contract

Actualizat: 2026-05-04

Status: contract de proiectare. Integrarea WorldEdit nu este implementata in core.

## Scop

Documentul defineste cum poate fi adaugat WorldEdit ca executant optional de constructii fara sa transforme core-ul Ainpc intr-un plugin dependent de WorldEdit.

WorldEdit trebuie tratat ca adapter de executie pentru template-uri si patch-uri deja planificate. El nu decide satul, economia, NPC-urile, questurile sau legaturile semantice. Aceste decizii raman in `SettlementPlan`, `PatchPlan`, template-uri si sistemele de simulare.

## Problema pe care o rezolva

Fara un contract clar, integrarea WorldEdit risca sa devina un shortcut care:

- lipeste cladiri fara legatura cu `region`, `node`, `place` si `household`;
- creeaza dependinta hard intre plugin si WorldEdit;
- sare peste validarea de coliziuni, drumuri, acces si limite de regiune;
- produce cladiri vizuale fara markere pentru NPC, joburi si questuri;
- face rollback-ul imposibil sau nesigur.

Contractul separa planificarea de executie.

## Principii

1. WorldEdit este optional.
2. Core-ul defineste contractele si capabilitatile.
3. Adapterul WorldEdit executa, nu planifica gameplay.
4. Fiecare build returneaza rezultat masurabil: bounds, markere transformate, avertizari si erori.
5. Dry-run este obligatoriu inainte de commit pentru patch-uri automate.
6. Rollback-ul este explicit: suportat, nesuportat sau partial, niciodata presupus.
7. Cand WorldEdit lipseste, sistemul trebuie sa ramana functional semantic si cu builder nativ unde exista.

## Module

### `ainpc-core-plugin`

Core-ul:

- defineste interfetele de build;
- incarca `SettlementPlan` si `PatchPlan`;
- valideaza capabilitati;
- decide ordinea de aplicare a patch-urilor;
- creeaza `region`, `node`, `place`, `household`, `job_site` si legaturi NPC;
- expune comenzile administrative.

Core-ul nu importa direct clase WorldEdit.

### `ainpc-api`

API-ul public trebuie sa ramana curat. Poate expune DTO-uri stabile pentru:

- cereri de build;
- rezultat de build;
- capabilitati disponibile;
- handle de rollback;
- markere transformate.

Nu trebuie sa expuna tipuri WorldEdit in semnaturile publice.

### `ainpc-integration-worldedit`

Modulul optional:

- detecteaza WorldEdit;
- implementeaza contractele definite de core/API;
- incarca schematic-uri;
- aplica rotatie, mirror si palette replacement;
- executa paste-ul in lume;
- raporteaza blocurile schimbate, bounds si markerele rezultate;
- ofera rollback doar daca poate garanta datele necesare.

Numele poate fi schimbat, dar responsabilitatea trebuie sa ramana separata de core.

## Capabilitati

Adapterul trebuie sa publice un raport de capabilitati. Exemple:

| Capability | Sens |
| --- | --- |
| `worldedit-execution` | Poate executa paste prin WorldEdit. |
| `structure-templates` | Poate incarca template-uri structurale. |
| `schematic-validation` | Poate valida existenta si metadata template-ului. |
| `clipboard-rotation` | Poate roti structura inainte de paste. |
| `clipboard-mirror` | Poate oglindi structura, daca template-ul permite. |
| `palette-substitution` | Poate inlocui materiale dupa palette. |
| `marker-transform` | Poate transforma marker nodes in coordonate finale. |
| `build-rollback` | Poate reveni la starea anterioara pentru build-ul executat. |
| `dry-run` | Poate valida fara sa modifice lumea. |

Plannerul nu trebuie sa programeze un patch care cere o capabilitate indisponibila.

## Contracte conceptuale

### `StructureBuildService`

Serviciul principal pentru executie:

```text
validate(request) -> BuildValidationResult
dryRun(request) -> StructureBuildResult
commit(request) -> StructureBuildResult
rollback(handle) -> RollbackResult
capabilities() -> BuildCapabilityReport
```

### `StructureBuildRequest`

Cererea trimisa adapterului trebuie sa contina suficient context pentru executie, nu pentru decizie de gameplay:

| Camp | Descriere |
| --- | --- |
| `planId` | Planul de asezare din care vine build-ul. |
| `patchId` | Patch-ul care cere constructia, daca exista. |
| `structureId` | ID-ul structurii din plan. |
| `templateId` | Template-ul structural selectat de planner. |
| `worldName` | Lumea tinta. |
| `origin` | Punctul de plasare. |
| `rotation` | Rotatia calculata de planner. |
| `mirror` | Oglindire optionala. |
| `paletteId` | Paleta de materiale aprobata. |
| `expectedBounds` | Volumul asteptat pentru validare. |
| `requiredCapabilities` | Capabilitati fara care cererea nu poate continua. |
| `markerNodes` | Markerele semantice citite din template. |
| `dryRun` | Flag explicit pentru validare fara modificari. |

### `StructureBuildResult`

Rezultatul trebuie sa fie suficient pentru sincronizarea lumii vizuale cu modelul semantic:

| Camp | Descriere |
| --- | --- |
| `status` | `success`, `failed`, `partial` sau `skipped`. |
| `dryRun` | Indica daca lumea a fost modificata. |
| `actualBounds` | Volumul final ocupat dupa transformari. |
| `changedBlockCount` | Numarul de blocuri modificate. |
| `transformedMarkers` | Markerele template-ului in coordonate finale. |
| `createdPlaceRefs` | Referinte semantice sugerate pentru locuri create. |
| `createdNodeRefs` | Referinte semantice sugerate pentru nodes create. |
| `rollbackHandle` | Handle pentru rollback, daca exista. |
| `warnings` | Probleme non-fatale. |
| `errors` | Probleme fatale sau partiale. |

### `BuildRollbackHandle`

Handle-ul de rollback trebuie sa fie serializabil si verificabil:

| Camp | Descriere |
| --- | --- |
| `buildId` | ID unic pentru build. |
| `worldName` | Lumea modificata. |
| `bounds` | Zona afectata. |
| `strategy` | `snapshot`, `worldedit-history`, `none` sau `partial`. |
| `createdAt` | Timpul executiei. |
| `expiresAt` | Momentul dupa care rollback-ul nu mai este garantat. |

## Ce are voie sa faca adapterul

Adapterul WorldEdit poate:

- incarca fisiere `.schem` sau formatul ales de repository;
- verifica metadata template-ului;
- aplica rotatie si mirror;
- aplica substitutii de materiale;
- lipi structura in lume;
- transforma markerele declarate in template;
- raporta bounds, blocuri schimbate si warnings;
- salva datele necesare pentru rollback, daca strategia permite.

## Ce nu are voie sa faca adapterul

Adapterul WorldEdit nu trebuie sa:

- aleaga unde apare satul;
- aleaga populatia, familiile sau joburile;
- creeze NPC-uri;
- decida ce cladire este publica sau privata;
- scrie progres de quest;
- modifice reputatie, economie sau memoria NPC;
- creeze `region`, `node` sau `place` fara aprobarea core-ului;
- ignore `SettlementPlan`, `PatchPlan` sau limitarile de regiune;
- execute build-uri automate fara dry-run cand planul cere validare.

## Flux de executie

1. Plannerul genereaza sau incarca `SettlementPlan`.
2. `PatchPlan` selecteaza structurile care trebuie construite.
3. Core-ul verifica daca adapterul are capabilitatile cerute.
4. Core-ul trimite `validate`.
5. Core-ul trimite `dryRun`.
6. Daca rezultatul este acceptabil, adminul sau politica serverului aproba `commit`.
7. Adapterul executa paste-ul.
8. Adapterul returneaza bounds, markere si rollback handle.
9. Core-ul creeaza sau actualizeaza legaturile semantice.
10. Sistemele NPC folosesc legaturile finale pentru rutina, job si quest.

## Validare

`validate` si `dryRun` trebuie sa verifice cel putin:

- WorldEdit este prezent si compatibil;
- lumea tinta exista si este incarcata;
- template-ul exista;
- metadata template-ului este valida;
- capabilitatile cerute sunt disponibile;
- `expectedBounds` nu depaseste limitele permise;
- structura nu intra in zone protejate;
- structura nu blocheaza drumuri obligatorii;
- marker nodes obligatorii exista;
- dimensiunea paste-ului respecta bugetul de performanta.

Validarea de gameplay ramane in core. Adapterul poate raporta coliziuni brute, dar nu decide daca un magazin este util economic sau daca un NPC are nevoie de el.

## Rollback

Rollback-ul trebuie tratat ca feature separata, nu ca promisiune implicita.

Strategii acceptate:

| Strategie | Sens |
| --- | --- |
| `snapshot` | Adapterul salveaza blocurile afectate inainte de paste. |
| `worldedit-history` | Adapterul foloseste istoricul WorldEdit, daca este accesibil sigur. |
| `partial` | Adapterul poate reveni doar o parte din schimbari. |
| `none` | Nu exista rollback garantat. |

Daca rollback-ul nu este garantat, `StructureBuildResult` trebuie sa spuna explicit acest lucru.

## Comenzi administrative

Comenzi propuse:

```text
/ainpc build capabilities
/ainpc build validate <planId|patchId>
/ainpc build dryrun <planId|patchId>
/ainpc build commit <planId|patchId>
/ainpc build rollback <buildId>
```

Comenzile trebuie sa afiseze:

- capabilitati disponibile;
- numarul de structuri afectate;
- bounds estimate si bounds finale;
- warnings critice;
- rollback disponibil sau indisponibil;
- link catre planul sau patch-ul care a generat build-ul.

## Fallback cand WorldEdit lipseste

Cand WorldEdit nu este instalat:

- `worldedit-execution` este indisponibil;
- template-urile cu `buildMode = worldedit_template` sunt marcate `skipped`;
- plannerul poate propune alternativa nativa daca exista;
- modelul semantic al satului poate fi creat fara structura fizica;
- adminul primeste avertizare clara, nu eroare ascunsa.

Aceasta permite dezvoltarea simularii reale chiar si inainte ca executia vizuala sa fie gata.

## Integrare cu planurile existente

Legaturi directe:

- `SettlementPlan` decide asezarea, structurile si markerele asteptate.
- `PatchPlan` decide ce build-uri se aplica incremental.
- `Template Cladiri si Marker Nodes` defineste metadata pe care adapterul trebuie sa o respecte.
- `NPC World Bindings` foloseste markerele finale pentru `place`, `node`, rutina si quest.

WorldEdit este doar executantul final al unei decizii deja validate.

## Faze mici

### WE0 - Contracte fara dependinta

- Adauga DTO-uri si interfete in core/API.
- Returneaza capabilitati goale cand nu exista adapter.
- Nu atinge lumea.

### WE1 - Detectie optionala

- Modul separat detecteaza WorldEdit.
- `/ainpc build capabilities` raporteaza prezenta pluginului.
- Nu executa paste.

### WE2 - Validare template

- Adapterul incarca metadata si verifica fisierul.
- `validate` si `dryRun` returneaza erori clare.
- Nu modifica lumea.

### WE3 - Paste controlat

- `commit` executa o singura structura aprobata.
- Returneaza `actualBounds`, `changedBlockCount` si markere transformate.
- Nu creeaza NPC-uri.

### WE4 - Sincronizare semantica

- Core-ul foloseste rezultatul pentru `place`, `node` si `job_site`.
- Rutinele NPC pot folosi locurile construite.
- Questurile pot referi locurile prin IDs stabile.

### WE5 - Rollback si patch-uri multiple

- Build-urile primesc `buildId`.
- Rollback-ul devine disponibil unde strategia permite.
- Patch-urile pot grupa mai multe structuri cu raport complet.

## De evitat

- Importuri WorldEdit in core.
- Paste direct din comenzi fara `SettlementPlan` sau `PatchPlan`.
- Template-uri fara marker nodes.
- Cladiri generate vizual fara `place` semantic.
- Rollback prezentat ca disponibil fara handle valid.
- Decizii de NPC/job/quest in adapter.
- Build-uri automate mari fara dry-run si buget de performanta.

## Definitie MVP

Integrarea WorldEdit este MVP cand:

- core-ul porneste fara WorldEdit instalat;
- capabilitatile sunt raportate corect;
- un template poate fi validat fara modificari de lume;
- un build aprobat poate lipi o structura;
- rezultatul include bounds si markere transformate;
- core-ul poate crea legaturi semantice dupa build;
- lipsa rollback-ului este raportata explicit daca nu exista suport.

