# Configurare story pentru lume și NPC

Verificat în cod: 2026-10-05. Configurare și limite runtime, fără validare pe server live.
Flux complet: [inițiere lume, hartă, structuri, NPC și story](../guides/initiere-lume-harta-structuri-npc-story.md).

## 1. Story, quest, lore și memorie

| Concept | Rol | Configurare/stocare |
|---|---|---|
| Story în mapping | Modul, starea și pool-ul declarate pentru regiune | `world_admin.regions.<id>.story` |
| Story persistent | Starea și variabilele dinamice ale regiunii/place-ului | DB prin `StoryStateService` |
| Eveniment story | Ce s-a întâmplat, unde, cine a participat și payload | DB; publicare directă sau coadă pending |
| Quest/scenariu | Obiective, condiții, faze, recompense și progres | Definiții în pack-uri/questuri; progres separat |
| Lore/backstory | Contextul narativ al NPC-ului sau conținutului | Profiluri și definiții de conținut |
| Memorie NPC | Interacțiuni/amintiri, inclusiv legătura cu jucătorul | `MemoryManager` și DB |
| Context AI | Snapshot al datelor relevante pentru dialog | Compus din mapping, NPC, questuri și story |

Un eveniment story poate exista fără quest. Un quest poate funcționa fără să modifice o stare story. Un text generat despre o clădire sau despre meseria unui NPC nu creează clădirea, binding-ul sau obiectivele questului.

Nu există un model unic `world_story` sau un `NpcStoryState` configurabil complet printr-un YAML general. Stările persistente dedicate din acest subsistem sunt de regiune și de place; contextul NPC combină profilul, ancorele, memoria și evenimentele relevante.

## 2. Configurația globală

| Cheie în `config.yml` | Implicit livrat | Efect/limită |
|---|---|---|
| `features.story` | `true` | Verificat la intrarea comenzii story; nu este un gate universal pentru toate serviciile story |
| `platform.default_story_mode` | `evolutive` | Mod implicit pentru mapping: `static`, `evolutive`, `rotative` |
| `story.npc_reactions_enabled` | `true` | Controlează reacțiile prin `StoryReactionService` |
| `story.random_events_enabled` | `false` | Activează schedulerul și este verificat și în `RandomWorldEventService.tick()` |
| `story.random_events_interval_seconds` | `600` | Interval de evaluare; minimum runtime `120` secunde |
| `story.random_events_require_review` | `false` | Pentru scheduler: adaugă draft pending în loc de publicare directă |
| `features.ai` | `true` | Gate AI; nu produce de la sine un provider configurat și nu este necesar pentru stocarea unui eveniment |
| `quest.ai_draft_enabled` | `false` | Draft de quest prin AI; separat de evenimentele random story |

Exemplu de fragment pentru evenimente random revizuibile:

```yaml
features:
  story: true
story:
  npc_reactions_enabled: true
  random_events_enabled: true
  random_events_interval_seconds: 600
  random_events_require_review: true
```

Integrează fragmentul în secțiunile existente. Pentru activarea/intervalul schedulerului repornește controlat serverul: reload-ul nu reconstruiește task-urile. Dacă ai modificat mapping-ul, [salvează și sincronizează overlay-ul](configurare-lume-harta-generare.md) înainte de restart.

Intervalul `600` înseamnă o evaluare la zece minute, nu un eveniment garantat la fiecare zece minute. Alegerea depinde de probabilități, condiții și cooldown. `features.story: false` singur nu dezactivează schedulerul random; setează explicit și `story.random_events_enabled: false` când acesta trebuie oprit.

## 3. Proprietăți story de regiune în mapping

Fragment de inserat în regiunea existentă, sub `world_admin.regions.sat_demo`:

```yaml
story:
  mode: evolutive
  state: peaceful
  pool:
    - peaceful
    - trade
    - daily_life
  descriptions:
    peaceful: "Localnicii își desfășoară activitățile în liniște."
    trade: "Piața este plină, negustorii primesc clienți."
```

| Câmp | Semnificație | Fallback loader |
|---|---|---|
| `mode` | Profil narativ al regiunii | `platform.default_story_mode` |
| `state` | Cheie text pentru starea declarată | `default` |
| `pool` | Lista cheilor narative disponibile | Listă goală |
| `descriptions` | Texte pentru stările regiunii, indexate după cheia exactă a stării | Hartă goală |

Aceste date sunt proprietăți ale mapping-ului. Nu produc automat rânduri în `region_story_state`. Nici `evolutive`, nici `rotative` nu trebuie interpretate ca promisiunea că un motor schimbă automat toate stările doar fiindcă valoarea a fost setată.

**Două straturi pot coexista:** `StoryContextService` expune semnale `region_story_state` din mapping și `persistent_region_state` din DB. Dacă valorile diferă, inspectează și corectează stratul dorit; editarea YAML-ului nu este un reset implicit al DB.

## 4. Proprietăți story ale unei structuri

`StoryStructureSignalResolver` citește din metadata place-ului cheile `story_state`, `state`, `tension`, `danger`, `danger_level`, `event`, `conflict`, `quest_hook`.

Exemplu de fragment în `world_admin.regions.sat_demo.places.fierarie`:

```yaml
metadata:
  profession: fierar
  story_state: shortage
  tension: low
  danger_level: low
  quest_hook: supply_delivery
```

`profession` servește alocării NPC, iar celelalte valori produc semnale de context. `quest_hook: supply_delivery` nu creează automat un quest cu acest nume. `story_state: shortage` în metadata nu actualizează `place_story_state` din DB.

Tipul place-ului, tag-urile și node-urile relevante contribuie și ele la semnalele despre structură. Vizitarea place-urilor și interacțiunea cu anumite node-uri au căi reale de înregistrare a evenimentelor în `ScenarioEngine`.

Un `quest_hook` nevid în metadata unei ancore o face eligibilă pentru lista `relevant_nodes` din contextul story, inclusiv când tipul ei este `npc_spawn`. Exemplu de fragment în `profiles.house_small.anchors` din `building_templates.yml`:

```yaml
arrival:
  node_type: npc_spawn
  offset: {x: 2, y: 1, z: 2}
  metadata:
    quest_hook: household_welcome
```

Offset-ul trebuie să încapă în dimensiunile template-ului. După generare, valoarea rămâne în mapping la salvare/reîncărcare. Contextul enumeră cel mult cinci ID-uri de noduri relevante: folosește nodurile apropiate și recitește nodurile place-ului curent, astfel încât limita generică de 12 noduri apropiate să nu elimine un hook local. Au prioritate primele noduri ale place-ului curent pentru fiecare `quest_hook` distinct, apoi nodurile cu hook repetat, celelalte noduri ale place-ului curent și, la urmă, cele ale altor structuri; ordinea inițială se păstrează în fiecare grup. Adaugă `node_quest_hook=<valoare>` pentru nodurile selectate care aparțin place-ului curent. Valorile identice sunt deduplicate; hook-urile altor clădiri și cele goale sunt ignorate. Acest semnal poate participa la selecția cererii narative. La planificarea unei interacțiuni, hook-ul nodului devine cheia evenimentului și valoarea `node_quest_hook` din payload. Configurarea lui nu declanșează singură o interacțiune la spawn și nu creează definiția quest-ului.

Fierăria (`forge`) și piața (`market`) au categoria story `profession`, aceeași categorie folosită de evenimentele lor de vizitare. Fierăria furnizează hook-urile `forge_order`, `repair_tool`, `metal_supply`, iar piața `market_supply`, `community_event`, `trade`. Acestea sunt semnale pentru selecția narativă; disponibilitatea quest-urilor depinde în continuare de scenariile configurate.

Categoria story a place-ului este stabilită de aceeași regulă atât pentru context, cât și pentru evenimentele de vizitare/interacțiune. `castle_room` este `public`, iar `cave_room` și `camp` sunt `exterior`; evenimentul folosește categoria raportată în context.

Hook-urile implicite ale tipului de place și cele provenite din tag-uri folosesc același catalog în context și în payload-ul `quest_hooks` al evenimentului. Pentru `castle_room` sunt `civic_request`, `public_event`, `lost_record`; pentru `cave_room` sunt `exploration`, `danger`, `recover_artifact`; pentru `camp` sunt `exploration`, `route_report`, `danger`. Metadata explicită a place-ului apare separat în context ca `place_quest_hook`; la planificare, metadata place-ului sau a nodului poate completa lista `quest_hooks` din payload.

Grădina `leisure_garden` păstrează tipul de mapping `custom`. Tag-ul explicit `leisure` atribuie structurilor `custom` categoria story `leisure`: planificatorul produce `structure_visit_leisure` la vizitare și `structure_node_social` pentru interacțiunea cu ancora socială, cu categoria `leisure` în payload. Tag-urile `social`/`public` ale grădinii furnizează hook-ul `community_event`. Tag-ul `leisure` nu schimbă categoria tipurilor consacrate (de exemplu, taverna rămâne `profession`); un place `custom` fără acest tag păstrează categoria `unknown`.

Pentru un template `place_type: custom`, `metadata.story_category` poate stabili explicit categoria story: `residential`, `profession`, `utility`, `leisure`, `public`, `exterior` sau `quest`. Valoarea explicită are prioritate față de tag-ul `leisure`; valorile necunoscute sau folosirea cheii pe alte tipuri sunt respinse la încărcarea template-ului și la auto-place programatic. Exemplu pentru un atelier personalizat:

```yaml
place_type: custom
tags: [work]
metadata: {max_workers: 1, story_category: profession, quest_hooks: "repair_tool, metal_supply"}
anchors:
  bench:
    node_type: workstation
    offset: {x: 2, y: 1, z: 2}
```

Categoria story influențează contextul și tipul evenimentului; nu creează automat geometria fizică, meseria NPC, un post de muncă sau un quest. Pentru muncă rămân necesare tag-ul `work`, `max_workers` și o ancoră de lucru; configurația completă cere și `footprint` valid.

`metadata.quest_hooks` acceptă ID-uri `snake_case` separate prin virgulă, de exemplu `"repair_tool, metal_supply"`; spațiile de lângă virgule sunt ignorate, iar duplicatele sunt eliminate păstrând prima apariție. Lista este verificată la încărcarea template-ului și la auto-place programatic. Hook-urile apar ca semnale `quest_hook` în context și în `quest_hooks` din payload-ul evenimentului. Primul hook configurat devine cheia evenimentului dacă nu există `quest_hook`, `event`, `conflict` sau altă cheie explicită cu prioritate. Câmpul singular `metadata.quest_hook` rămâne disponibil și are prioritate la cheia evenimentului; niciuna dintre aceste proprietăți nu creează definiții de quest.

Template-ul livrat `workshop` folosește `place_type: custom`, `story_category: profession` și `quest_hooks: "craft_order, repair_order, material_supply"`. Astfel, un atelier personalizat poate avea semantică story de muncă fără a pretinde un nou tip `PlaceType`.

Template-ul livrat `bakery` folosește aceeași categorie `profession` și hook-urile `bread_order`, `grain_supply`, `delivery`. Ele sunt semnale pentru selecția story, nu definiții de quest și nu declanșează automat vânzări sau fabricarea pâinii.

Template-ul livrat `library` are simultan tag-urile `work`, `social` și `leisure`, dar `story_category: profession` explicit păstrează categoria de vizitare profesională. Ancora `reading` produce un eveniment de interacțiune socială, iar `quest_hooks: "lost_book, research_request, book_delivery"` oferă semnale narative, nu împrumuturi sau quest-uri implementate automat.

Cooldown-ul structural este calculat din evenimentele recente încărcate pentru locul curent. Resolverul verifică durata fiecărui eveniment și expune cooldown-ul activ cu timpul rămas cel mai mare; un eveniment expirat nu ascunde unul încă activ. Evenimentele altor locuri și timestamp-urile din viitor nu activează acest semnal. La atingerea duratei, cooldown-ul expiră.

La decizia de generare, `QuestScoringResolver` ignoră un marker cu `quest_generation_cooldown_place_id` diferit de `place_id` sau cu o categorie nerelevantă cererii. Dacă markerul nu blochează cererea, verifică separat evenimentele recente din locul curent, chiar dacă lipsește rezumatul cooldown-ului. Fiecare eveniment folosește propria categorie, durată și relevanță; durata din rezumat nu se aplică tuturor evenimentelor. Verificarea păstrează limita strictă de expirare și ignoră timestamp-urile viitoare. Astfel, un cooldown exterior nerelevant nu ascunde un cooldown profesional încă activ.

Duratele sunt constante în politica comună `StructureQuestCooldown`: `residential` 10 s, `profession` 22 s, `utility` 12 s, `leisure` 6 s, `public` 14 s, `exterior` 30 s și `quest` 45 s; celelalte categorii folosesc 15 s. Evenimentele `structure_node_work`, `structure_node_social` și `structure_node_home` sunt mapate explicit la `profession`, `leisure`, respectiv `residential`. Astfel, atât vizita în grădină, cât și interacțiunea socială folosesc 6 s, iar `structure_node_quest` păstrează toate cele 45 s. Resolverul de context și verificarea alternativă a deciziei folosesc aceeași politică. `QuestDirector` verifică relevanța semnalului pentru cererea narativă înainte de a amâna generarea.

## 5. Proprietățile stării persistente și ale evenimentelor

| Model | Câmpuri principale |
|---|---|
| `RegionStoryState` | `regionId`, `storyMode`, `stateKey`, `storyPool`, `variables`, `createdAt`, `updatedAt`, `updatedBy`, `source` |
| `PlaceStoryState` | `placeId`, `regionId`, `stateKey`, `variables`, `createdAt`, `updatedAt`, `updatedBy`, `source` |
| `StoryEventDraft` | `scopeType`, `scopeId`, `regionId`, `placeId`, `eventType`, `eventKey`, `title`, `description`, `payload`, `actorType`, `actorId`, `playerUuid`, `npcId` |

Stările/variabilele DB se scriu prin serviciile `saveRegionState(...)` și `savePlaceState(...)`. În comenzile story verificate, `region` și `place` sunt comenzi de **inspecție**; nu inventa sintaxe precum `/ainpc story region set ...`.

Pentru o integrare care inițializează direct DB, trebuie implementat un apel valid la serviciul adecvat, păstrând câmpurile care nu trebuie resetate. Aceste metode interne nu sunt prezentate aici ca un API public stabil pentru addonuri.

Limitare: `StoryAuthoringService.setRegionStoryMode()` transmite `null` pentru state/pool/variables, ceea ce salvează `default` și colecții goale. Nu îl folosi drept operație care garantează păstrarea restului stării existente.

### Descrieri pentru starea story curentă

Descrierile opționale oferă o explicație narativă pentru o stare exactă. NPC-ul le folosește în răspunsurile despre starea curentă, în locul ID-ului tehnic. Aceeași descriere ajunge în contextul AI și în răspunsul fără furnizor AI. Pentru o regiune din mapping, configurează `world_admin.regions.<id>.story.descriptions.<stateKey>` ca în exemplul de mai sus. Se folosește numai descrierea cheii `story.state` active; lista `pool` nu schimbă singură starea.

Pentru starea unei clădiri din mapping, adaugă chei plate în metadata place-ului existent:

```yaml
metadata:
  story_state: shortage
  story_description_shortage: "Lipsesc materialele, așteptăm livrările."
  story_description_open: "Atelierul primește comenzi."
```

La întrebarea „Care este starea clădirii?”, prima descriere explică starea `shortage`. Descrierea `open` rămâne inactivă până când starea din mapping devine exact `open`. Dacă există un rând persistent valid pentru acest place, **starea și descrierea vin numai din acel rând**, nu din metadata YAML.

Pentru regiuni și place-uri persistente, adaugă aceleași chei în harta `variables` trimisă către `saveRegionState(...)` sau `savePlaceState(...)`. Exemplu Kotlin de variabile de îmbinat cu cele existente, păstrând celelalte câmpuri ale stării:

```kotlin
val variables = existingState.variables() + mapOf(
    "story_description_shortage" to "Lipsesc proviziile, așteptăm livrările.",
    "story_description_peaceful" to "Localnicii își desfășoară activitățile în liniște.",
)
```

Fragmentul nu salvează singur în DB. Pentru regiunea din mapping se folosește `story.descriptions`, iar pentru starea persistentă a regiunii se folosesc cheile `story_description_<stateKey>` din `variables`. Un rând persistent valid are prioritate peste descrierea din YAML. Configurarea YAML a regiunii supraviețuiește ciclului de salvare și reîncărcare a mapping-ului.

Contractul comun:
- cheia include exact `stateKey`, cu aceeași capitalizare; schimbarea stării nu reutilizează descrierea veche;
- un rând persistent pentru altă locație nu poate furniza nici starea, nici descrierea;
- un rând persistent fără descriere nu preia textul din mapping, chiar dacă starea are același nume;
- textul efectiv este redus la o singură linie și cel mult 200 de caractere; lipsa textului, spațiile și `<none>` păstrează răspunsul anterior bazat pe cheia stării;
- descrierea nu creează o stare, un eveniment, un quest sau o regulă de rutină; regulile continuă să folosească `stateKey`;
- întrebările despre istoric folosesc în continuare istoria locului și titlurile evenimentelor, nu această descriere curentă.

### Proprietăți narative NPC

Proprietățile narative ale NPC-ului — `backstory`, `backstorySeed`, `archetype`, `socialRole`, `questRole` — aparțin profilului/planului descris în [referința NPC](configurare-npc-asociere-structuri.md). Nu există un bloc YAML `npc.story` consumat universal de aceste servicii.

La crearea dintr-un plan, `socialRole` și `questRole` sunt păstrate în `profile_data.narrative` ca `social_role` și `quest_role`. Contextul story al acelui NPC expune numai rolurile valide ca `npc_social_role=<id>` și `npc_quest_role=<id>`; ele pot influența punctarea candidaților de quest. Lipsa profilului sau un JSON invalid nu adaugă semnale. `backstory` și seed-urile nu sunt transformate în semnale de cerere narativă, iar simpla prezență a unui rol nu creează un quest.

Ancorele NPC `home`, `work` și `social` sunt rezolvate prin coordonate în mapping, când indică un place valid. Contextul adaugă `npc_home_place_id`, `npc_work_place_id` și `npc_social_place_id` pentru clădirile găsite; ancorele fără place, cu lume greșită sau coordonate invalide nu produc acest semnal. ID-urile oferă context narativ, dar nu sunt folosite singure ca cerere de quest și nu modifică binding-ul persistent.

## 6. Comenzi pentru inspecție și inițiere de evenimente

Necesită administrator și feature-ul story activ. Folosește ID-uri existente de regiune/place, nu denumiri aproximative.

```text
/ainpc story region sat_demo
/ainpc story place sat_demo:casa_01
/ainpc story events sat_demo 10
/ainpc story pending sat_demo 10
/ainpc story context NumeJucator nearest
```

Înlocuiește `NumeJucator` cu un jucător online. `nearest` necesită un NPC activ suficient de aproape de acel jucător. Inspecția arată separat datele mapping și datele persistente atunci când există.

Listează template-urile disponibile pentru regiune:

```text
/ainpc story author sat_demo
```

Publică un eveniment folosind un template încorporat:

```text
/ainpc story author sat_demo village_celebration
/ainpc story events sat_demo 10
```

Sau publică un tip explicit, cu un titlu de un singur token și o descriere:

```text
/ainpc story author sat_demo trade Sosire_negustor Un negustor a ajuns in sat.
```

`author` publică direct; `story.random_events_require_review` se aplică schedulerului și nu transformă această comandă într-un preview. Comanda nu cere `--confirm` în handlerul verificat; nu adăuga acest token la descriere.

Pentru un draft existent creat de scheduler, folosește ID-ul returnat de `pending`:

```text
/ainpc story publish <pendingId>
/ainpc story discard <pendingId>
```

Acestea sunt alternative: publici **sau** respingi draftul. Publicarea înregistrează evenimentul și notifică reacțiile asociate. Povestea unei sosiri de negustor sau a unui atac nu dovedește că au fost spawnate automat entitățile descrise.

## 7. Template-uri, proprietăți de generare și pack-uri

`StoryAuthoringService.getTemplates()` livrează din cod: `village_celebration`, `merchant_arrival`, `raider_attack`, `natural_disaster`, `ritual_ceremony`, `diplomatic_visit`, `discovery`, `seasonal_festival`, `hero_return`, `dark_omen`.

`RandomWorldEventService` selectează doar subsetul definit în `eventConfigs`. Proprietățile `templateId`, intervalele în ore, probabilitatea, anotimpul, momentul zilei și numărul minim de NPC-uri sunt definite în cod; nu există loader YAML pentru aceste reguli per template.

În implementarea actuală, cooldown-ul regional folosește intervalul minim global din configurațiile interne (24 ore), iar `lastEventTime` este în memorie. Intervalele maxime declarate în model nu trebuie tratate ca un mecanism complet de programare. În modul review, un pending existent pentru regiune amână generarea unui alt draft.

Pentru conținut de scenariu se folosesc feature pack-uri, conform [schemei pack-urilor](scenario-pack-schema.md). Loader-ul citește și `story_defaults`, de forma:

```yaml
story_defaults:
  settlement:
    default_state: festive
    pool: [festive, celebration, preparation]
```

Acesta este un fragment de pack, nu de `config.yml`. Loader-ul cere `default_state` și un pool nevid pentru a reține definiția. Nu este un import automat în DB al tuturor regiunilor de acel tip. Exemplu real: [festival.yml](../../ainpc-scenario-medieval/src/main/resources/packs/festival.yml).

### Limita acțiunilor story declarative

În codul verificat, `ScenarioEngine.applyQuestStoryActions()` publică `StoryActionAppliedEvent` prin `QuestStoryActionEventPublisher`. Această publicare nu scrie singură starea/payload-ul custom în DB. Handler-ele runtime `set_story_state` și `record_story_event` verificate doar loghează.

Prin urmare, nu considera o acțiune YAML acceptată de schemă drept dovadă că starea s-a modificat persistent. Verifică efectul prin `story region/place/events`. Există separat căi reale `recordQuestStoryEvent()` și `recordStructureStoryEvent()` pentru evenimentele de quest și structură; acestea nu trebuie confundate cu acțiunile declarative incomplet conectate.

## 8. Context, reload și verificare

`StoryContextService` combină mapping-ul din jurul jucătorului/NPC-ului, semantica structurilor, ancorele questurilor active și evenimentele recente. Limitele de 12 ancore active și 5 evenimente recente sunt constante în cod; nu chei documentate de tip `context.max_events`.

`reloadContent()` recreează `StoryStateService` și `StoryContextService`, dar datele lor persistente rămân în DB. Reload-ul pack-urilor nu trebuie folosit ca metodă de resetare a story-ului. `/ainpc world save` salvează mapping-ul; nu este operația care persistă evenimentele deja trimise serviciului story.

Verificare recomandată:

1. Inspectează regiunea/place-ul și confirmă ID-urile mapping.
2. Publică un eveniment de test explicit și verifică-l prin `story events`.
3. Inspectează `story context` cu jucătorul și NPC-ul în zona respectivă.
4. Dacă folosești scheduler review, inspectează pending-ul și publică/respinge un ID real.
5. După salvarea și sincronizarea mapping-ului, repornește și verifică separat mapping-ul, evenimentele și stările DB.

## Surse

- [StoryStateService](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryStateService.kt), [RegionStoryState](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/RegionStoryState.kt), [PlaceStoryState](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/PlaceStoryState.kt).
- [StoryAuthoringService](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryAuthoringService.kt), [RandomWorldEventService](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/RandomWorldEventService.kt), [comenzi story](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandStory.kt).
- [StoryContextService](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryContextService.kt), [StoryStructureSignalResolver](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryStructureSignalResolver.kt).
- [QuestStoryActionEventPublisher](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestStoryActionEventPublisher.kt), [SetStoryStateAction](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/runtime/actions/SetStoryStateAction.kt), [RecordStoryEventAction](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/runtime/actions/RecordStoryEventAction.kt).
- [Arhitectură story state](../architecture/story-state-service.md), [story/context/quest/AI](story-context-quest-ai-stack.md).
