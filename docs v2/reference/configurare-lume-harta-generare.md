# Configurare lume, hartă și generare

Verificat în cod: 2026-10-05. Document de operare; fără validare pe server live.
Punct de intrare: [inițiere lume, structuri, NPC și story](../guides/initiere-lume-harta-structuri-npc-story.md).

## 1. Cele patru niveluri

| Nivel | Ce reprezintă | Ce nu face automat |
|---|---|---|
| Lume Minecraft | Terenul, blocurile și entitățile dintr-o lume încărcată de server | Nu este creată prin `platform.world_mode` |
| `WorldRegion` | O zonă semantică, de exemplu sat, castel sau peșteră | Nu generează teren |
| `WorldPlace` | O structură/zonă din regiune: casă, fierărie, piață | Nu plasează blocurile clădirii |
| `WorldNode` | Punct utilizabil: spawn, pat, lucru, social, interacțiune | Nu creează automat NPC-ul, patul fizic sau questul |

Fluxul public de populație folosește mapping-ul existent. Construcția fizică are infrastructură internă, dar nu este conectată complet la comenzile de mapping; vezi secțiunea 6.

## 2. Configurația globală

Valorile din tabel sunt cele livrate în [config.yml](../../ainpc-core-plugin/src/main/resources/config.yml), înainte de aplicarea unui overlay.

| Cheie | Implicit | Rol |
|---|---|---|
| `platform.runtime_mode` | `standalone` | Profil de runtime; valori `standalone`, `hybrid`, `advanced` |
| `platform.world_mode` | `finite_dynamic` | Profil semantic; valori `static`, `finite_dynamic`, `open_dynamic` |
| `platform.default_story_mode` | `evolutive` | Mod implicit când regiunea nu definește altul: `static`, `evolutive`, `rotative` |
| `features.mapping` | `true` | Flag de feature; nu înlocuiește configurarea WorldAdmin și permisiunile |
| `features.generation` | `true` | Flag declarat pentru generare; nu dovedește executarea construcției fizice |
| `world_admin.enabled` | `true` | Activarea mapping-ului |
| `world_admin.auto_index.enabled` | `true` | Index pe chunk-uri pentru căutări; fără index se poate căuta liniar |
| `world_admin.scan.blocks_per_tick` | `4096` | Buget comun scanărilor vanilla; runtime îl limitează la `256..65536` |

Exemplu de fragment în `config.yml`; combină cheile cu secțiunile existente, fără a dubla rădăcinile YAML:

```yaml
platform:
  runtime_mode: standalone
  world_mode: finite_dynamic
  default_story_mode: evolutive
world_admin:
  enabled: true
  auto_index:
    enabled: true
  scan:
    blocks_per_tick: 4096
```

Seed-ul, tipul terenului și generarea structurilor vanilla aparțin configurării lumii Minecraft/serverului. Nu există aici chei AINPC confirmate precum `world_seed` sau `generate_terrain`.

## 3. Overlay, salvare și reload

`AINPCPlatform` caută în directorul pluginului **primul** fișier existent din această ordine:

1. `world-admin.json`
2. `world-admin.yml`
3. `world-admin.yaml`
4. `world_admin.json`
5. `world_admin.yml`
6. `world_admin.yaml`
7. `castel-world-admin.yml`

Din fișier acceptă secțiunea `world_admin`, alternativ `world-admin` sau `mapping`; dacă niciuna nu există, folosește rădăcina fișierului. Dacă nu există fișier, încearcă resursa încorporată `castel-world-admin.yml`.

**Limitare actuală:** `ScriptConfigurationLoader.mergeSection()` înlocuiește întreaga secțiune `world_admin`; nu combină individual regiunile cu cele din `config.yml`. `/ainpc world save` scrie mapping-ul în `config.yml`, nu sincronizează overlay-ul. Prin urmare, un overlay vechi sau minimal poate elimina mapping-ul salvat din starea încărcată după reload/restart.

Resursa `castel-world-admin.yml`, copiată la prima pornire, conține `enabled: true` și `auto_index.enabled: false`. Nu presupune că valoarea `true` din config câștigă la indexare. Loader-ul are și un fallback programatic `castel` atunci când secțiunea de regiuni există, dar acea regiune lipsește, în afara testelor.

Procedură pentru păstrarea mapping-ului:

1. După modificările din joc, rulează `/ainpc world save`.
2. Identifică overlay-ul efectiv din ordinea de mai sus.
3. Copiază **secțiunea completă** `world_admin` salvată din `config.yml` în overlay-ul pe care îl vei utiliza. Pentru YAML, un `world-admin.yml` explicit este ușor de inspectat, dar nu are prioritate peste un `world-admin.json` existent.
4. Păstrează în secțiune și opțiunile necesare, nu numai `regions`.
5. Abia apoi execută reload/restart și verifică regiuni, places și nodes.

Aceasta este o măsură operațională pentru comportamentul actual, nu o sincronizare implementată de plugin. Nu edita overlay-ul și mapping-ul din joc în paralel fără să alegi care copie este autoritară.

Surse: [AINPCPlatform](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/platform/AINPCPlatform.kt), [ScriptConfigurationLoader](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ScriptConfigurationLoader.kt), [WorldAdminService](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/WorldAdminService.kt).

## 4. Proprietățile hărții

| Obiect | Câmpuri YAML | Observații |
|---|---|---|
| Regiune | cheia ID, `name`, `world`, `type`, `min.x/y/z`, `max.x/y/z`, `tags` | Bounds întregi; lumea trebuie să corespundă lumii încărcate |
| Story de regiune | `story.mode`, `story.state`, `story.pool` | Metadate mapping, distincte de DB story |
| Place | cheia ID, `name`, `world`, `type`, `min`, `max`, `tags` | Se declară în `regions.<id>.places`; lumea implicită este a regiunii |
| Acces și proprietar | `owner_npc_id`, `public_access` | Owner implicit gol, acces public implicit `true`; nu reprezintă lista tuturor rezidenților |
| Metadata place | `metadata.<cheie>` | Valori text; au efect doar dacă există consumator pentru cheie |
| Node | cheia ID, `type`, `world`, `x`, `y`, `z`, `radius`, `metadata` | Sub `nodes` al place-ului sau direct al regiunii; radius implicit `2.5` |

Tipuri de regiune: `settlement`, `castle`, `dungeon`, `cave`, `wilderness`, `custom`. Pentru sat folosește `settlement`; `village` nu este un ID din enum-ul `RegionType`.

Tipuri de node: `npc_spawn`, `entrance`, `bed`, `workstation`, `home`, `work`, `social`, `meeting_point`, `quest_trigger`, `boss`, `interaction`, `progression`, `custom`.

Pentru NPC, casa are nevoie de puncte `npc_spawn` și `bed/home`. Setarea unui `quest_trigger` descrie punctul; condițiile și obiectivele questului trebuie configurate separat.

### Exemplu minimal de overlay

Exemplul este pentru o casă cu un rezident. Creează în prealabil clădirea fizică și adaptează lumea, înălțimea și coordonatele la teren. Exemplul definește o regiune nouă; la folosirea pe un server existent păstrează și celelalte regiuni în overlay.

```yaml
world_admin:
  enabled: true
  auto_index:
    enabled: true
  scan:
    blocks_per_tick: 4096
  regions:
    sat_demo:
      name: Sat Demo
      world: world
      type: settlement
      min: {x: 0, y: 60, z: 0}
      max: {x: 60, y: 90, z: 60}
      tags: [demo]
      story:
        mode: evolutive
        state: peaceful
        pool: [peaceful, trade, daily_life]
      places:
        casa_01:
          name: Casa 01
          type: house
          min: {x: 5, y: 64, z: 5}
          max: {x: 13, y: 72, z: 13}
          public_access: false
          metadata:
            max_residents: "1"
          nodes:
            spawn_01:
              type: npc_spawn
              x: 8.0
              y: 65.0
              z: 8.0
              radius: 1.0
            pat_01:
              type: bed
              x: 10.0
              y: 65.0
              z: 10.0
              radius: 1.0
```

Folosește ierarhia `min: {x: ..., y: ..., z: ...}`. Chei precum `min_x` sau `story_state_key` nu sunt schema citită aici. Place-ul va fi identificat ca `sat_demo:casa_01`; inspectează ID-urile node-urilor înainte de a le referi în alte definiții.

## 5. Scanare, import și amplasare semantică

Comenzile administrative cer permisiunile și feature-urile potrivite. `--confirm` trebuie să fie ultimul argument al mutațiilor care îl cer.

```text
/ainpc world scan village 32
/ainpc world scan village 32 import sat_demo --confirm
/ainpc world settlement auto sat_demo --confirm
/ainpc building templates
/ainpc building auto-place house_small sat_demo --confirm
/ainpc audit world
/ainpc world save
```

Acestea sunt **alternative de lucru**, nu o secvență de rulat integral peste exemplul YAML anterior:

- `scan village 32` inspectează incremental blocurile din jurul jucătorului; rezultatul este asincron față de răspunsul inițial al comenzii.
- `scan ... import` creează mapping din construcțiile găsite; importul separat refuză un ID de regiune deja existent.
- `settlement auto` combină scanarea/importul și planificarea; nu construiește clădiri și nu execută spawn. Poate completa o regiune existentă compatibilă cu centrul scanării.
- `building auto-place` creează mapping pentru template-ul ales; nu este o comandă de construire fizică.
- După modificări, salvează și sincronizează overlay-ul conform secțiunii 3.

Pentru introducere manuală și editare cu wand: [mapping hărți manuale](../guides/mapping-harti-manuale.md).

## 6. Configurația și limitele generării

### `settlements.yml`

Resursa definește `settlements.<id>` cu `world`, `center.x/y/z`, `radius`, `profile`, `region_type`, `theme`, `display_name` și `tags`. Exemplul livrat este vizibil prin:

```text
/ainpc world settlement definitions
```

Fișierul este citit pentru listarea definițiilor. Fluxul `settlement auto/plan/spawn` nu îl consumă ca un plan executabil complet. Schimbarea `radius` sau `theme` în acest fișier nu generează automat un sat. Exemplul livrat folosește `region_type: village`; această valoare nu trebuie copiată ca `type` în schema `WorldRegion`.

### `building_templates.yml`

Fișierul este încărcat și validat la startup și prin `/ainpc reload`. `BuildingTemplateRegistry` păstrează catalogul anterior când reîncărcarea eșuează. Comenzile folosesc profilele configurate, inclusiv dimensiunile, ancorele, tag-urile și metadata. Catalogul implicit din JAR conține `house_small`, `house_family`, `forge`, `farm`, `shop`, `tavern`, `market`, `leisure_garden`, `community_hall`, `workshop`, `workshop_shared`, `bakery` și `library`; loader-ul nu rescrie fișierul personalizat.

Pentru preview/construire fizică, profilele livrate `house_small` și `house_family` declară explicit `metadata.geometry: house_small`; celelalte locuințe rămân doar mapping semantic. Intrarea, ancorele de dormit și spawn-urile trebuie să fie poziții distincte la nivelul `1` deasupra fundației. Casa familială are două perechi de ancore și `max_residents: "2"`. Catalogul și mapping-ul deja existente nu sunt mutate automat după actualizarea profilului.

Schema activă, configurația pentru familie și limitele sunt în [template-uri configurabile runtime](template-config-runtime.md). Auto-place rămâne semantic și execută numai orientarea `0`; încărcarea rotațiilor declarate nu activează construcția fizică.

### `worldgen` în `config.yml`

| Cheie | Implicit livrat | Conectare confirmată în cod |
|---|---|---|
| `enabled` | `false` | Nu este citită ca gate al inițializării serviciului |
| `executor_preference` | `[worldedit, native]` | Citită în `ServiceRegistry.phase9Starters()` la alegerea executorului |
| `auto_build_on_patch` | `true` | Nu este citită de fluxul public verificat |
| `auto_build_on_building_auto_place` | `true` | Nu este citită de fluxul public verificat |
| `templates_dir` | `templates` | Cheie declarată; providerul folosește propriul director în cod |
| `schematics_dir` | `schematics` | Cheie declarată, fără citire de configurare identificată |
| `max_blocks_per_operation` | `50000` | Nu trebuie tratată ca limită aplicată de runtime |
| `enable_preview` | `true` | Nu este un gate de config conectat |
| `enable_undo` | `true` | Nu este un gate de config conectat |
| `persist_schematics` | `true` | Nu este un gate de config conectat |

Există `WorldGenService` și executori native/WorldEdit, inițializați la pornire. Comenzile publice verificate nu apelează metodele sale de construire fizică; patch-urile folosesc `VillagePatchApplier`, iar auto-place folosește `BuildingAutoPlaceService`.

Nu deduce că `worldgen.enabled: true` activează de la sine o generare completă. În implementarea internă, ramura WorldEdit a patch-ului are fallback nativ. Executorul nativ păstrează acum snapshot-uri `BlockState` pentru undo, nu șterge prin AIR. Preview-ul nu creează o sesiune de undo executabilă; restaurările eșuate pot fi reîncercate. Snapshot-urile sunt numai în memorie, dispar la restart și nu detectează modificările ulterioare ale jucătorilor. Backend-ul WorldEdit nu este schimbat de această corecție. Nu folosi undo ca backup persistent sau mecanism de recuperare fără verificarea zonei.

Surse: [config.yml](../../ainpc-core-plugin/src/main/resources/config.yml), [settlements.yml](../../ainpc-core-plugin/src/main/resources/settlements.yml), [BuildingTemplateRegistry](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingTemplateRegistry.kt), [WorldGenService](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/worldgen/WorldGenService.kt), [VillagePatchApplier](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch/VillagePatchApplier.kt), [BuildingAutoPlaceService](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingAutoPlaceService.kt).

## 7. Pasul următor

După mapping valid și persistent continuă cu [generarea NPC și asocierea structurilor](configurare-npc-asociere-structuri.md), apoi cu [story](configurare-story-lume-npc.md). Pentru fiecare casă, verifică planul înainte de spawn; un fișier YAML valid sintactic nu dovedește că lumea fizică și alocările sunt valide.
