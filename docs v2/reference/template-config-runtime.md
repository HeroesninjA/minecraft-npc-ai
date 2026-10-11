# Template-uri configurabile: primul lot de implementare

Actualizat: 2026-10-11.
Status: implementat local pentru catalog, mapping semantic și transmiterea proprietăților către planner/context. Construcția fizică și bootstrap-ul complet al satului rămân etape separate.

## 1. Ce funcționează acum

`plugins/AINPC/building_templates.yml` este citit la pornire și prin `/ainpc reload`. `BuildingTemplateConfigLoader` validează întregul catalog înainte ca `BuildingTemplateRegistry` să înlocuiască definițiile configurate.

- Comenzile `building templates` și `building auto-place` folosesc catalogul încărcat; nu mai suprascriu configurația cu definiții hardcodate.
- Resursa YAML din JAR este sursa valorilor implicite. Fișierul de pe server este instalat numai când lipsește; loader-ul nu îl rescrie.
- La reload invalid, catalogul anterior rămâne activ și comanda raportează eroarea. Aceasta este o garanție pentru catalog, nu o tranzacție pentru toate serviciile din `/ainpc reload`.
- La startup, o configurație invalidă propagă eroarea de inițializare; pluginul nu pornește cu un catalog parțial.
- `profiles: {}` reprezintă un catalog configurat gol. O definiție eliminată din YAML dispare din catalogul configurat după reload.
- Template-urile înregistrate programatic sunt păstrate la reload și au prioritate pentru același ID. Catalogul static este curățat la inițializarea core-ului.

## 2. Schema activă

| Cale | Tip și validare | Efect |
|---|---|---|
| `profiles.<id>` | Obiect; ID local `[a-z0-9][a-z0-9_-]*` | Definește un template |
| `display_name` | Text nevid, implicit ID-ul | Numele place-ului generat |
| `place_type` | ID canonic `PlaceType`, implicit `house` | Tipul structurii semantice |
| `footprint.width/depth/height` | Întregi pozitivi; aria trebuie să încapă într-un `Int` | Extinderea limitelor de mapping |
| `rotations` | Listă nevidă, fără duplicate, din `0/90/180/270`; implicit `[0]` | Orientări declarate; auto-place execută în prezent numai `0` |
| `tags` | Listă de texte nevide | Tag-uri copiate în place |
| `metadata` | Mapă de valori scalare text/număr/boolean | Valori copiate ca text în place |
| `anchors.<id>.node_type` | ID canonic `WorldNodeType` | Tip de node |
| `anchors.<id>.offset.x/y/z` | Întregi de la zero până la dimensiunea corespunzătoare, inclusiv | Poziție relativă la originea place-ului |
| `anchors.<id>.radius` | Număr finit pozitiv, implicit `2.0` | Raza node-ului; valori sub `1.0` sunt păstrate |
| `anchors.<id>.role` | Text, implicit gol | Metadata node `anchor_role` |
| `anchors.<id>.tags` | Listă de texte nevide | Metadata node `tags`, separată prin virgulă |
| `anchors.<id>.metadata` | Mapă scalară | Metadata suplimentară a node-ului |

Cheile necunoscute sunt respinse, pentru a nu ignora erori de scriere. `source`, `template_id`, `anchor_role` și `tags` sunt rezervate în obiectele `metadata`; folosește câmpurile dedicate pentru rol/tag-uri. `variants`, `required_capabilities`, seed-uri și fișiere schematic nu fac parte din această schemă YAML implementată.

Pentru `house`, dacă declari `metadata.max_residents`, acesta trebuie să fie un întreg pozitiv care nu depășește numărul perechilor de ancore `npc_spawn` și `bed/home`. Metadata generică rămâne extensibilă; numai cheile cu consumator în runtime au efect de gameplay.

`metadata.max_workers` trebuie să fie un întreg nenegativ: implicit `1`, iar `0` exclude structura din alocarea locurilor de muncă. Planner-ul scade ID-urile distincte ale angajaților existenți și rezervă capacitatea între casele planificate în același apel. Capacitatea este per structură, nu per node; mai mulți lucrători pot folosi aceeași ancoră. Rezervările preview-ului nu sunt persistente; comenzile independente de planificare a unei case nu împart aceste rezervări.

`metadata.max_social_associations` este opțional și trebuie să fie un întreg nenegativ. Când lipsește, nu există o limită numerică pentru asocierile sociale; `0` oprește asocierile noi. Planner-ul și generatorul narativ scad ID-urile distincte din `social_npc_ids` și rezervările din planul curent. Binding-ul social verifică din nou limita și permite același ID deja asociat, inclusiv după reducerea capacității. Nu este o limită a vizitatorilor prezenți fizic și nu se aplică posturilor de muncă.

Amprenta folosește convenția serviciului existent: maximul coordonatei este originea plus dimensiunea configurată. Nu este o declarație a numărului de blocuri fizice construite. Verificarea suprapunerilor respectă limitele inclusive din WorldAdmin.

## 3. Legătura cu NPC și story

La aplicarea binding-ului work, `WorldAdminService.bindNpcToWorkPlace` verifică din nou `max_workers` față de ID-urile distincte din `worker_npc_ids`. Refuză un NPC nou dacă structura este plină sau dezactivată, înainte de orice modificare a metadata. Reasocierea aceluiași ID este permisă inclusiv după reducerea capacității; nu sunt eliminați automat lucrătorii existenți. ID-urile sunt comparate fără diferențiere între majuscule/minuscule, iar listele acceptă separatorul virgulă sau punct și virgulă.

La aplicarea binding-ului social, `WorldAdminService.bindNpcToSocialPlace` verifică `max_social_associations` dacă este configurat; un NPC nou este refuzat fără modificarea metadata când limita este atinsă. Validarea grupată a destinațiilor se face înainte de a scrie home/work/social, dar nu constituie o tranzacție comună cu DB-ul.

Verificarea și scrierea binding-urilor work sunt sincronizate pe instanța serviciului. Aceasta protejează apelurile simultane ale acelei metode, nu reprezintă o tranzacție între mapping, spawn și DB. Ocuparea este păstrată prin salvarea/reîncărcarea mapping-ului; autosave-ul și overlay-ul au în continuare limitările documentate. Un plan preliminar poate fi refuzat la binding dacă între timp postul este ocupat. Un astfel de eșec nu șterge automat NPC-urile deja create.

| Configurație | Consumator | Rezultat |
|---|---|---|
| Tipul `house`, ancorele și `metadata.max_residents` | `HouseAllocationPlanner` | Capacitate și puncte pentru rezidenți |
| Workplace `metadata.profession` | `HouseAllocationPlanner` | Ocupația NPC-ului alocat |
| Loc social `metadata.max_social_associations` | Planner, generator narativ și binding | Număr maxim de NPC-uri distincte asociate |
| Node-uri `workstation` și `social` | Rezolvarea ancorelor | Work și social în planul NPC |
| Metadata `story_state`, `quest_hook` și celelalte chei story recunoscute | `StoryStructureSignalResolver` | Semnale pentru context |

Resursa livrată include `house_small` cu `max_residents: "1"` și `house_family` cu `max_residents: "2"`; ambele aleg `geometry: house_small`. Geometria fizică a casei este opt-in pentru template-urile `house`: cere tag `home`, ancore distincte `entrance`, `bed`/`home` și `npc_spawn` la nivelul `1`, plus capacitate pozitivă; reperul de dormit din lână este decorativ, nu pat funcțional. `community_hall` declară `geometry: community_hall`, `max_workers: 0`, `max_social_associations: 2`, două ancore sociale și categoria story `public`; plannerul distribuie cei doi rezidenți între ancore, iar rutina folosește ancora atribuită. `forge` și `farm` nu impun profesii tematice; acestea pot fi configurate pe server, de exemplu `profession: blacksmith` sau `profession: farmer`. `workshop` și `bakery` includ profesii configurate; `workshop_shared` are două posturi și `max_workers: 2`, iar plannerul distribuie muncitorii între ancore. `library` folosește ocupația neutră `lucrator` și `max_social_associations: 2`. Configurațiile existente nu sunt completate automat pe disc.

Această transmitere de proprietăți nu creează un quest și nu scrie o stare dinamică `PlaceStoryState` în DB. Pentru aceste diferențe consultă [configurarea story](configurare-story-lume-npc.md).

## 4. Exemplu utilizabil

Pentru catalogul extins de magazin, fierărie, tavernă, piață și recreere, vezi [clădiri pentru muncă, comerț și socializare](../guides/cladiri-munca-comert-socializare.md).

Catalogul [building_templates.yml pentru familia demo](../examples/familie-documentatie/building_templates.yml) definește o casă cu doi rezidenți, fierărie, fermă și piață. Acest exemplu mai vechi definește `house_family` numai semantic (fără `geometry` și `entrance`); pentru preview fizic folosește profilul nou din catalogul livrat și reface mapping-ul corespunzător.

Integrează profilele dorite în fișierul serverului. Fișierul de exemplu este un catalog complet pentru exercițiu; înlocuirea fișierului existent elimină din catalog profilele care nu mai sunt declarate.

După editare, cu mapping-ul existent salvat și [overlay-ul sincronizat](configurare-lume-harta-generare.md), rulează:

```text
/ainpc reload
/ainpc building templates
```

Pe o lume de test încărcată, alege o zonă liberă și un ID nou. Următoarele coordonate sunt un exemplu pentru o regiune goală:

```text
/ainpc world region create sat_configurat settlement 600 64 600 680 90 680 --confirm
/ainpc building auto-place house_family sat_configurat --confirm
/ainpc building auto-place forge sat_configurat --confirm
/ainpc building auto-place farm sat_configurat --confirm
/ainpc building auto-place market sat_configurat --confirm
/ainpc audit world
/ainpc world settlement plan sat_configurat 1
```

Aceste operații creează mapping. Auto-place folosește în continuare `region.minY + 1` ca origine verticală și nu analizează terenul. Înainte de spawn trebuie să existe construcțiile, paturile și căile accesibile la pozițiile mapate. Dacă harta este deja construită, [mapping-ul manual](../guides/mapping-harti-manuale.md) poate fi intrarea mai potrivită.

După verificarea lumii fizice și a planului:

```text
/ainpc world settlement spawn sat_configurat 1 --confirm
/ainpc world household list 20
/ainpc world bindings list 20
/ainpc audit spawn
/ainpc world save
```

`1` limitează numărul caselor; casa din exemplu are capacitate pentru doi rezidenți. Folosește ID-urile returnate de comenzi pentru inspecții ulterioare. Sincronizează overlay-ul înainte de restart. Repetarea `auto-place` creează o amplasare suplimentară; nu reprezintă reluarea idempotentă a unei inițializări.

## 5. Ce urmează în implementare

| Etapă | Stare |
|---|---|
| Catalog YAML validat, reload, metadata și ancore propagate în mapping | Implementat în acest lot |
| Salvarea coerentă config/overlay a lumii | Limitarea existentă rămâne; sincronizare manuală documentată |
| Capacitate `max_workers` și rezervarea posturilor între casele unui plan regional | Implementat; rezervările persistente și identitățile de familie unice rămân de consolidat |
| Geometrie fizică legată de aceeași definiție, teren, rotații și compensare | De implementat/consolidat |
| Inițializare opt-in, etapizată, persistentă și reluabilă | De implementat |
| Aplicarea efectivă în DB a tuturor acțiunilor story declarative | De implementat/consolidat |

Ordinea completă și criteriile de acceptare rămân în [planul de remediere](../planning/plan-remediere-generare-sat-si-rutine.md). Nu este încă un generator complet de lume sau un bootstrap automat de sat.

## 6. Categoria story pentru template-uri custom

`metadata.story_category` este acceptată numai pentru `place_type: custom`. Valorile permise sunt `residential`, `profession`, `utility`, `leisure`, `public`, `exterior` și `quest`. Valoarea configurează semantica story a place-ului generat, nu atribuirea NPC, capacitatea de muncă sau geometria clădirii. Vezi [configurarea story](configurare-story-lume-npc.md) pentru exemplu și regulile de prioritate.

`metadata.quest_hooks` acceptă o listă scalară de ID-uri separate prin virgulă, de exemplu `"repair_tool, metal_supply"`. Loader-ul și auto-place validează fiecare ID înainte de plasare; hook-urile sunt deduplicate în context și în evenimente. `metadata.quest_hook` singular rămâne compatibil. Lista oferă semnale story, nu creează quest-uri.

## 7. Validare și surse

Testele acoperă schema, catalogul din JAR, eroarea la reload fără pierderea catalogului anterior, eliminarea profilelor, prioritatea înregistrărilor programatice, propagarea metadata, alocarea NPC și round-trip-ul mapping-ului. Testul de integrare folosește WorldAdmin în memorie; nu demonstrează existența blocurilor ori spawn-ul fizic pe Paper.

- [BuildingTemplateConfigLoader](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingTemplateConfigLoader.kt).
- [BuildingTemplateRegistry](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingTemplateRegistry.kt).
- [BuildingAutoPlaceService](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingAutoPlaceService.kt).
- [Testul fluxului template → mapping → planner/story](../../ainpc-core-plugin/src/test/kotlin/ro/ainpc/settlement/BuildingAutoPlaceServiceTest.kt).
