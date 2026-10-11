# Clădiri pentru muncă, comerț și timp liber

## Ce este implementat

Catalogul `building_templates.yml` include acum treisprezece template-uri. `building auto-place` creează **mapping semantic**: limitele structurii, proprietăți și puncte de activitate. Nu construiește pereți, mobilier, tejghele sau vegetație. Clădirile fizice trebuie pregătite separat; nu porni spawn-ul pe un teren gol doar pentru că mapping-ul este valid.

| Template | Utilizare | Ancore principale | Posturi implicite |
|---|---|---|---|
| `house_small` | Locuință | `bed`, `npc_spawn`, `entrance` | Nu este workplace |
| `house_family` | Locuință pentru doi rezidenți | `bed_01/02`, `spawn_01/02`, `entrance` | Nu este workplace |
| `forge` | Atelier/fierărie | `workstation`, `entrance`, `npc_spawn` | 1 |
| `farm` | Muncă agricolă și hambar | `workstation`, `entrance`, `npc_spawn` | 1 |
| `shop` | Muncă la tejghea, interacțiune comercială | `counter` de tip workstation; `customer` de tip interaction | 1 |
| `tavern` | Servire și socializare | `counter` pentru muncă; `tables` pentru socializare | 1 |
| `market` | Tarabă și întâlniri publice | `stall` pentru muncă; `customer` pentru interacțiune; `square` pentru întâlniri | 1 |
| `leisure_garden` | Recreere și socializare | `benches` de tip social; `entrance` | 0 |
| `community_hall` | Întâlniri și socializare în interior | `gathering_west/east` de tip social; `entrance` | 0 |
| `workshop` | Tâmplărie și preluare comenzi | `bench` de tip workstation; `customer` de tip interaction; `entrance` | 1 |
| `workshop_shared` | Atelier comun pentru doi lucrători | `bench_west/east` de tip workstation; `customer`; `entrance` | 2 |
| `bakery` | Brutărie și preluare comenzi | `oven` de tip workstation; `customer` de tip interaction; `entrance` | 1 |
| `library` | Muncă la bibliotecă și lectură socială | `desk` de tip workstation; `reading` de tip social; `entrance` | 1 |

Tipurile de activitate nu se exclud: taverna și biblioteca au atât angajat, cât și vizitatori. Grădina și sala comunitară nu au tag `work` și nu sunt selectate ca locuri de muncă. Tag-ul `leisure` atribuie structurilor `custom` categoria story `leisure` numai în lipsa unui `metadata.story_category` explicit; biblioteca declară `profession`, iar sala comunitară `public`. Programul activităților NPC rămâne gestionat de sistemul de rutine.

## Configurare și amplasare

Pe instalări existente, fișierul personalizat nu este suprascris. Integrează profilele noi din [catalogul livrat](../../ainpc-core-plugin/src/main/resources/building_templates.yml) în secțiunea `profiles` a fișierului serverului, apoi rulează `/ainpc reload`. Un catalog invalid este refuzat; cel anterior rămâne activ.

Într-o regiune de test deja creată, înlocuiește `sat_test` cu ID-ul real:

```text
/ainpc building templates
/ainpc building auto-place shop sat_test --confirm
/ainpc building auto-place forge sat_test --confirm
/ainpc building auto-place farm sat_test --confirm
/ainpc building auto-place tavern sat_test --confirm
/ainpc building auto-place market sat_test --confirm
/ainpc building auto-place leisure_garden sat_test --confirm
/ainpc building auto-place community_hall sat_test --confirm
/ainpc building auto-place workshop sat_test --confirm
/ainpc building auto-place workshop_shared sat_test --confirm
/ainpc building auto-place bakery sat_test --confirm
/ainpc building auto-place library sat_test --confirm
/ainpc audit world
/ainpc world settlement plan sat_test
```

Comenzile cer spațiu liber în mapping. Folosește ID-urile returnate, nu presupune un sufix numeric fix: după ștergerea unei clădiri, auto-place sare peste ID-urile încă existente în regiune. Repetarea auto-place adaugă altă structură. Originea verticală rămâne `region.minY + 1`; nu există adaptare automată la teren în acest flux.

`auto-place` verifică dimensiunile template-ului și poziția/raza fiecărei ancore înainte de scriere, inclusiv pentru template-uri înregistrate prin API și nu doar încărcate din YAML. O ancoră în afara volumului clădirii este refuzată fără a crea un place parțial.
Pentru un template cu tag `work`, aceeași verificare cere `max_workers` nenegativ și, când există posturi disponibile, cel puțin o ancoră de lucru (`workstation`, `interaction` sau `semantic: work_anchor`). Un template invalid este refuzat înainte de a crea structura.
Pentru o casă cu `max_residents`, valoarea trebuie să fie pozitivă și să nu depășească numărul de perechi distincte `npc_spawn` și `bed`/`home`. Astfel, capacitatea declarată nu poate reutiliza același pat sau punct de spawn pentru mai mulți rezidenți.

Capacitatea și ocupația sunt configurabile în metadata structurii/template-ului:

```yaml
metadata:
  max_workers: 1
  profession: vanzator
```

Pentru un loc eligibil social, `metadata.max_social_associations` limitează numărul de NPC-uri distincte din `social_npc_ids` care pot fi asociate. Este opțional, întreg nenegativ; fără el asocierea nu are limită numerică, iar `0` oprește asocierile noi. Biblioteca livrată folosește `max_social_associations: 2`. Aceasta nu limitează vizitatorii prezenți fizic și nu rezervă rafturi sau locuri de stat.

Pentru atelier poți folosi `profession: fierar`, pentru tavernă `profession: hangiu`, iar pentru bibliotecă `profession: bibliotecar` în configurația serverului. Acestea sunt texte configurate, nu implementări automate ale meseriilor. Catalogul livrat folosește `profession: lucrator` pentru bibliotecă; profesia poate fi personalizată fără schimbarea codului. `max_workers: 0` oprește atribuirea de noi locuri de muncă, fără să închidă accesul social. Mai multe posturi pot utiliza aceeași ancoră workstation; capacitatea nu creează mobilier suplimentar.

Template-urile `forge` și `farm` declară explicit `max_workers: "1"`, astfel încât planner-ul și binding-ul să folosească aceeași capacitate după reîncărcarea configurației.

Un template cu tag `work` și `max_workers` pozitiv trebuie să aibă o ancoră `workstation` sau `interaction`, ori o ancoră cu `metadata.semantic: work_anchor`. Loader-ul refuză un profil fără punct de lucru; pentru `max_workers: 0`, ancora nu este obligatorie.

## Cum sunt asociate NPC-urile

1. NPC-ul este planificat pornind de la o casă fără rezidenți în mapping și perechile spawn/pat; un node `npc_spawn` din fierărie nu o transformă în locuință. Casele deja populate sunt omise cu avertisment din planul regional, nu realocate automat.
2. Planner-ul caută workplaces în aceeași regiune: fierărie, fermă, piață, tavernă, magazin, apoi celelalte tipuri. La egalitate folosește ID-ul. Nu alege după meseria existentă a NPC-ului și nici după distanță.
3. Ocupația vine din `profession` sau `occupation`; absența lor păstrează fallback-ul generic. Din capacitate se scad angajații existenți din `worker_npc_ids`, apoi se rezervă posturi între casele aceluiași plan regional. Duplicatele de ID nu consumă posturi suplimentare.
4. `workPlaceId` indică structura; `workNodeId` indică punctul de lucru. Se preferă `workstation`/`work` sau `metadata.semantic: work_anchor`, apoi `interaction`, iar `npc_spawn` este fallback. Distanța față de centrul clădirii departajează numai punctele de aceeași prioritate. Planner-ul, spawn-ul și binding-ul manual folosesc aceeași selecție; un spawn mai apropiat nu înlocuiește postul de lucru explicit.
5. Asocierea socială este separată: planner-ul alege locul eligibil cu cele mai puține ID-uri distincte în `social_npc_ids` plus rezervările planului curent, excluzând locurile care au atins `max_social_associations`. La egalitate, piața are prioritate față de tavernă, apoi celelalte tipuri după ordinea semantică și ID. Rezervările se păstrează între casele aceluiași plan regional, nu se resetează pentru fiecare casă. Tag-ul `public` face și magazinul eligibil; aceasta nu înseamnă că vizitatorii cumpără ceva. Se selectează o singură asociere socială per rezident, nu un traseu prin toate clădirile.
6. Binding-ul work verifică din nou `max_workers`, iar binding-ul social verifică `max_social_associations` față de ID-urile distincte persistate. Un plan depășit poate fi refuzat; reasocierea aceluiași NPC rămâne permisă. Nu este încă o rezervare SQL comună tuturor comenzilor.

Exemplu: o fierărie cu `max_workers: 3` și doi angajați distincți existenți oferă un singur post unui plan nou, nu trei. Dacă restul clădirilor nu au posturi libere, ceilalți rezidenți noi rămân `locuitor`. Preview-ul nu modifică listele de angajați sau rezidenți. Repetarea lui pe același mapping produce aceeași alocare, dar nu rezervă postul pentru o comandă viitoare.

Warning-ul planner-ului separă numărul de angajați existenți de posturile rezervate în plan. Folosește această diferență când investighezi un rezultat cu `locuitor`: primul vine din mapping, al doilea numai din ordinea caselor din preview.

`maxHouses` limitează casele disponibile după omiterea celor populate. Planificarea individuală a unei case cu `resident_npc_ids` nevid este refuzată; dacă întreaga regiune este populată, nu se generează un nou plan executabil. Verificarea folosește mapping-ul, nu reconciliază automat DB-ul și nu șterge ID-uri vechi sau asocieri existente.

## Comerț și rutine: limite importante

Cu șase rezidenți, fără limite sociale sau asocieri anterioare și numai piață, tavernă și grădină ca destinații eligibile, planul regional distribuie câte doi rezidenți fiecăreia. Dacă `social_npc_ids` conține deja NPC-uri, distribuția nouă pornește de la aceste asocieri. Cu `max_social_associations`, un loc plin este omis; dacă toate sunt pline, rezidentul rămâne fără `socialPlaceId` și planul emite avertisment. Limita nu măsoară ocuparea în timp real: nu consultă vizitatorii prezenți fizic, nu rezervă mese/bănci și nu coordonează planuri independente. `max_workers` afectează numai munca; grădina cu `max_workers: 0` rămâne eligibilă pentru socializare. Schimbarea numărului caselor sau destinațiilor poate schimba repartizarea unui plan nou, fără a migra automat binding-urile deja persistate.

- `interaction` cu rolul `trade` marchează locul clientului. Nu creează singur un magazin economic, inventar, prețuri, stoc sau tranzacții.
- Comerțul efectiv necesită configurarea separată a serviciului economic și a interacțiunii NPC. Numele `shop` al unui place nu îl leagă automat de o înregistrare economică.
- `socialPlaceId` și ancorele sunt intrări pentru sistemele de rutină. Nu demonstrează pathfinding reușit, animații, conversații sau consum de obiecte.
- Verifică blocurile, căile accesibile, binding-urile și [configurarea rutinelor NPC](../reference/configurare-npc-asociere-structuri.md) înainte de activare. Nu crea chei imaginare de tip `opening_hours` sau `leisure_schedule`: catalogul strict nu le acceptă ca proprietăți de template.
- Salvează mapping-ul și sincronizează overlay-ul conform [configurării lumii](../reference/configurare-lume-harta-generare.md). Actualizarea template-ului nu modifică retroactiv place-urile deja amplasate.

## Verificare

### Geometrie fizică pentru preview

`ActivityBuildingGeometry` generează pentru `shop`, `forge`, `farm` și `tavern` fundație, pereți, acoperiș plat, intrări deschise și mobilier lângă ancorele de muncă. Magazinul primește butoi, fierăria masă de lucru, ferma composter, iar taverna tejghea cu butoi și o masă decorativă lângă fiecare ancoră socială. Ferma și taverna folosesc scânduri de molid. Dimensiunile și pozițiile provin din definiția semantică, nu din vechile fișiere JSON. Interiorul include operații AIR; planul nu trebuie executat peste construcții fără verificarea terenului.

Locuințele `house_small` și `house_family` primesc geometrie fizică numai dacă template-ul de tip `house` declară `metadata.geometry: house_small`. Au podea, pereți, acoperiș plat, intrare deschisă și câte un bloc `WHITE_WOOL` decorativ lângă fiecare ancoră `bed`/`home`; ancorele rămân libere pentru NPC. Blocul decorativ nu este un pat Minecraft funcțional și nu adaugă somn sau proprietate automată. Generatorul cere tag `home`, `max_residents` pozitiv, suficiente ancore de dormit și spawn, poziții distincte și acces de la intrare. `house_family` are capacitate `2`, două perechi de ancore și două repere decorative, dar nu compartimentează camerele. Catalogul livrat mută intrarea și spawn-ul casei mici la nivelul `1`, iar spawn-ul la `(3, 1, 2)`, separat de ancora `bed` `(2, 1, 2)`. Cataloagele și mapping-urile existente nu se migrează automat; actualizează ambele înainte de preview.

Ferma `farm` produce un hambar simplu, cu un bloc `COMPOSTER` lângă ancora de muncă și un balot `HAY_BLOCK` decorativ, fără a ocupa intrarea sau punctele NPC. Nu generează culturi, irigație, animale sau producție agricolă automată. Spawn-ul și intrarea din catalogul livrat sunt acum la nivelul `1` deasupra fundației, ca poziții practicabile pentru NPC.

Taverna cere ancore distincte pentru intrare, muncă și socializare. Mobilierul nu ocupă pozițiile unde stau NPC-urile; generatorul verifică traseul dintre intrare și toate ancorele. Masa este realizată din gard și placă de presiune, nu reprezintă un serviciu automat de servire. Nu sunt generate camere de dormit sau rezervări de mese. Folosește `tavern` în comenzile preview/prepare, pe un place de tip tavern deja mapat.

Grădina `leisure_garden` are geometrie fizică deschisă: fundație din blocuri de iarbă și câte o bancă decorativă din slab de stejar lângă fiecare ancoră socială. Nu are pereți, acoperiș, tejghele sau posturi de lucru. Tipul rămâne `custom`, iar selecția generatorului este explicită prin `metadata.geometry: leisure_garden`; celelalte template-uri custom nu primesc implicit această geometrie.

Sala `community_hall` este un place `custom` social fără locuri de muncă, cu `metadata.geometry: community_hall`: podea, pereți și acoperiș plat din scânduri de molid, intrare deschisă și câte o masă decorativă lângă ancorele `gathering_west` și `gathering_east`. Cere `max_workers: 0`, tag `social`, cel puțin o ancoră socială și `max_social_associations` pozitiv; catalogul livrat limitează asocierea la doi NPC, suficient pentru cei doi rezidenți din `house_family`. Plannerul repartizează rezidenții între ancorele sociale disponibile, inclusiv între case planificate în același sat; rutina folosește ancora atribuită dacă este activă și destinația este accesibilă. Categoria story este `public`, cu hook-urile `meeting_notice` și `festival_preparation`. Nu există rezervare de scaune, program automat de întâlniri sau quest-uri create de aceste hook-uri. Dacă ai deja sala cu vechiul node `gathering`, actualizează template-ul și mapping-ul împreună înainte de preview; această schimbare nu migrează node-urile existente.

Atelierul `workshop` este tot un place `custom`, cu geometrie selectată explicit prin `metadata.geometry: workshop`: pereți și podea din scânduri de molid, intrare deschisă și masă de lucru lângă ancora `bench`. Template-ul livrat declară `profession: tamplar`, `max_workers: 1`, categoria story `profession` și hook-urile `craft_order`, `repair_order`, `material_supply`. Ancora `customer` este un punct de interacțiune, nu un sistem de comenzi sau tranzacții automate. Alte template-uri `custom` nu primesc implicit această geometrie.

`workshop_shared` refolosește aceeași geometrie într-un spațiu mai mare, cu două ancore `bench_west/east` și câte o masă `CRAFTING_TABLE` lângă fiecare. Declară `max_workers: 2` și ocupația `tamplar`. Plannerul repartizează cei doi lucrători între posturi, inclusiv când provin din case diferite planificate în același sat; rutina folosește ancora de muncă atribuită. Binding-ul refuză al treilea NPC după ocuparea capacității. Nu implementează producție, inventar sau împărțirea automată a comenzilor. Folosește `workshop_shared` ca template de preview numai după ce ancorele și dimensiunile din mapping corespund profilului.

Brutăria `bakery` folosește același tip `custom`, cu `metadata.geometry: bakery`: fundație, pereți și acoperiș plat din cărămizi și un bloc `SMOKER` lângă ancora `oven`. NPC-ul stă pe ancora liberă, nu în blocul de mobilier. Template-ul declară `profession: brutar`, `max_workers: 1`, categoria story `profession` și hook-urile `bread_order`, `grain_supply`, `delivery`. Ancora `customer` permite interacțiunea, dar nu configurează automat rețete, stoc, vânzări sau producție.

Biblioteca `library` este un place `custom` cu `metadata.geometry: library`: pereți și podea din scânduri de stejar, pupitru `LECTERN` lângă ancora de muncă `desk` și raft `BOOKSHELF` lângă ancora socială `reading`. Generatorul cere tag-urile `work` și `social`, `max_workers` pozitiv și ancore distincte pentru intrare, muncă și socializare. Template-ul include ocupația neutră `lucrator`, `max_social_associations: 2`, categoria story `profession` și hook-urile `lost_book`, `research_request`, `book_delivery`. Dacă este singurul loc eligibil și are capacitate disponibilă, același NPC poate avea biblioteca atât ca `workPlaceId`, cât și ca `socialPlaceId`, dar cu node-uri diferite. Raftul nu oferă automat împrumuturi, inventar de cărți sau animație de citit.

Generatoarele grădinii și sălii cer `max_workers: 0`, tag `social`, intrare și ancoră socială; refuză node-uri și tag-uri de muncă. Punctele NPC rămân libere și accesibile. Mobilierul nu implementează automat animația de ședere, conversații sau program de recreere. Adaugă metadata nouă în cataloagele existente dacă dorești acest preview; fișierele și mapping-ul vechi nu sunt migrate automat.

`market` are acum geometrie în aer liber: pavaj de cobblestone și câte o tarabă cu butoi, doi stâlpi și copertină de lână lângă fiecare ancoră de muncă. Nu are pereți sau acoperiș peste întreaga piață. Zona de întâlnire și toate pozițiile NPC rămân libere și accesibile. Tarabele nu creează automat stoc, prețuri sau tranzacții.

Piața cere intrare, workplace și ancoră socială. Catalogul livrat adaugă intrarea la `(7, 1, 0)`; fișierele și mapping-urile existente nu sunt migrate automat. Pentru preview fizic, actualizează coerent definiția și node-urile, apoi folosește `market` ca template în comenzile preview/prepare.

Ancorele trebuie să fie la nivelul `1` peste fundație, cu două blocuri libere pentru NPC. Intrările sunt pe margine, nu în colț. Generatorul verifică accesul de la intrare la toate ancorele; refuză dimensiuni și poziții incompatibile. Limita este 10.000 de celule, cu rotație `0`. Nu generează încă uși cu stare, logică automată pentru cuptor sau inventare comerciale.

`WorldGenService.previewBuild` folosește geometria nouă pentru cele cinci tipuri de activitate, locuințele `house` cu `geometry: house_small` și profilele `custom` cu geometrie explicit configurată (`leisure_garden`, `community_hall`, `workshop`, `bakery`, `library`); refuză dimensiuni diferite de mapping. Preview-ul verifică acum lumea încărcată, thread-ul principal, limitele verticale și chunk-urile deja încărcate. Cere aer în întregul volum țintă, inclusiv în celulele care urmează să devină AIR, și sprijin solid, fără lichid, sub fiecare coloană a fundației. Nu încarcă automat chunk-uri și nu nivelează terenul. Solidity este verificată la nivel de material; formele parțiale ale blocurilor nu sunt încă analizate.

`buildPlacePhysically` continuă să refuze aceste tipuri, pentru a nu executa vechiul JSON în locul geometriei afișate. Fluxul intern nou este `prepareActivityBuild(actor, placeId, templateId)` urmat de `confirmActivityBuild(actor, token)`. Token-ul expiră după 60 de secunde, aparține actorului și poate fi consumat o singură dată. Un eșec la confirmare necesită un preview nou; un actor diferit nu poate consuma token-ul. Sunt păstrate maximum 32 de cereri simultane, numai în memorie.

Confirmarea revalidează terenul și refuză orice entitate din volumul clădirii înainte de scriere. Păstrează ID-ul sesiunii executorului nativ la eșec parțial, pentru recuperare; nu aplică automat undo. Nu este o tranzacție comună pentru blocuri, mapping și DB.

Token-ul păstrează și un snapshot al template-ului, place-ului, limitelor regiunii și node-urilor. Modificarea sau ștergerea acestor date după pregătire refuză confirmarea și consumă token-ul, fără scriere de blocuri. Este necesar un preview nou. Comparația este conservatoare: și schimbările de metadata sau proprietar pot invalida planul, chiar dacă geometria vizibilă rămâne aceeași.

Încă de la preview, tipul, dimensiunile și lumea place-ului trebuie să corespundă template-ului; fiecare ancoră trebuie să existe în mapping sub ID-ul `<placeId>:<anchorId>`, cu același tip, poziție și rază. Un catalog modificat nu reconstruiește automat node-urile vechi. Rezolvă diferențele explicit înainte de pregătirea construirii. Reîncărcarea unor valori identice nu invalidează singură planul: se compară datele, nu identitatea obiectelor în memorie.

Execuția este dezactivată implicit prin `worldgen.activity_build.enabled` și cere backend nativ plus protecție configurată explicit. `worldgen.activity_build.protection: deny` refuză tot; `worldguard` selectează adaptorul opțional WorldGuard 7. Pluginul absent/dezactivat, API-ul incompatibil sau datele regionale indisponibile produc refuz, nu fallback permisiv. Celelalte căi legacy nu sunt modernizate de acest lot.

### Comenzi administrative fizice

Autorizarea locală verifică acum, atât la pregătire cât și la confirmare: `worldgen.enabled`, `worldgen.activity_build.enabled`, backend nativ, jucător online cu `ainpc.admin`, starea MCP verificabilă și includerea întregului volum într-o singură regiune din `worldgen.activity_build.allowed_regions`. Regiunile și permisiunile sunt citite din nou la confirmare: eliminarea sau micșorarea regiunii poate invalida token-ul. Un plan care traversează două regiuni nu este permis nici dacă ambele sunt în listă.

Valorile livrate sunt sigure implicit:

```yaml
worldgen:
  enabled: false
  activity_build:
    enabled: false
    allowed_regions: []
    protection: deny
```

Integrează aceste chei în secțiunea existentă, nu crea două secțiuni `worldgen`. Lista goală refuză orice construire. Activarea și completarea listei nu înlocuiesc adaptorul extern de protecție: acesta rămâne obligatoriu și implicit refuză tot. Consola poate solicita preview, dar pregătirea și confirmarea fizică necesită un jucător online; această limită evită lipsa identității de jucător pentru verificările de claims.

### Configurarea opțională WorldGuard

Pe un server de test cu WorldGuard 7 și dependențele sale instalate, configurația opt-in poate fi:

```yaml
worldgen:
  enabled: true
  executor_preference: [native]
  activity_build:
    enabled: true
    allowed_regions: [sat_test]
    protection: worldguard
```

`sat_test` este ID-ul regiunii AINPC, nu un ID WorldGuard. Ambele verificări trebuie să permită întregul volum. Alegerea executorului necesită restart controlat; altfel un executor WorldEdit selectat anterior rămâne neeligibil pentru acest flux. Fișierele serverului nu sunt schimbate automat de implementare.

Adaptorul citește direct managerul WorldGuard și regiunile aplicabile fiecărei celule, fără cache de rezultat între preview și confirmare. Verifică împreună `BUILD`, `BLOCK_PLACE` și `BLOCK_BREAK`, inclusiv pentru operații AIR, și refuză seturile virtuale. Nu aplică bypass-ul WorldGuard al administratorilor: acesta este un control intenționat mai strict decât unele operații manuale. Nu simulează evenimente Bukkit de plasare și nu consultă automat alte pluginuri de claims, blacklist-ul de materiale sau protecții economice. Nu activa această variantă pe un server unde acele sisteme trebuie consultate separat.

Integrarea folosește API-ul WorldGuard prin reflecție și `softdepend`, fără includerea WorldGuard în JAR. Testele locale verifică politica și refuzurile cu un query simulat; compatibilitatea completă cu versiunea WorldGuard instalată trebuie verificată pe serverul de test înainte de producție.

Comenzile necesită `ainpc.admin`. Folosește ID-ul real al unui place deja mapat:

```text
/ainpc building preview sat_test:shop_1 shop
/ainpc building prepare sat_test:shop_1 shop
/ainpc building confirm <token-returnat> --confirm
```

`preview` afișează numărul de celule și limitele, fără scriere și fără token; nu este încă o hologramă în joc. `prepare` verifică autorizarea și creează token-ul pentru același solicitant. `confirm` cere explicit `--confirm`, verifică din nou condițiile și execută planul capturat. Pentru jucători identitatea este UUID-ul, nu numele afișat.

Pregătirea și confirmarea sunt refuzate dacă MCP raportează read-only **sau dacă starea nu poate fi verificată**; numai valoarea booleană explicită `tools.readOnly: false` permite continuarea. Preview-ul rămâne disponibil. Protecția aceasta este suplimentară adaptorului pentru regiuni, nu îl înlocuiește.

La eșec parțial comanda afișează ID-ul sesiunii de recuperare. Nu există încă o comandă publică nouă de undo pentru acest flux: sesiunea este disponibilă serviciului intern, numai până la restart. Nu repeta confirmarea pentru a recupera; token-ul este consumat și zona trebuie inspectată. Nu activa acest flux pe lumea de producție fără adaptorul de protecție și o probă controlată pe Paper.

Template-urile livrate pentru fierărie și fermă mută intrarea și spawn-ul de la nivelul `0` la `1`. Fișierele și mapping-urile existente nu sunt migrate automat; generatorul fizic refuză vechile ancore la nivelul fundației. Actualizează template-ul și node-urile deja mapate în mod coerent înainte de preview fizic. Testele geometriei verifică limitele, spațiul ancorelor, mobilierul, determinismul și translația coordonatelor, fără server Paper.

Testele `BuildingAutoPlaceServiceTest` verifică amplasarea template-urilor de bază, ferma, atelierul de tâmplărie, brutăria și biblioteca, ancorele comerciale, asocierea locurilor de muncă și a celui social din bibliotecă, selecția magazinului/tavernei ca workplace și grădina exclusiv socială. Sunt teste de mapping și planificare, nu teste de construire fizică sau tranzacții comerciale pe server.

### Folosirea destinațiilor în rutina NPC

Programul `SOCIAL` folosește ancora socială deja asociată, inclusiv cea a pieței, tavernei sau grădinii; `WORK` folosește ancora de muncă, iar `HOME` ancora locuinței. Pentru oricare dintre aceste sloturi, o ancoră absentă sau invalidă (lume goală ori coordonate nefinite) produce `IDLE`, fără țintă și cu motiv explicit. Nu sunt inventate locuri de muncă sau destinații alternative.

La execuția programului normal, rutina verifică lumea/chunk-ul și confirmă apropierea reală de destinație înainte de aplicarea stării de activitate. O navigație acceptată produce `WALKING`, nu muncă sau socializare imediată; o destinație indisponibilă ori deplasarea refuzată fără teleport reușit produce `IDLE`. Teleportul trebuie acceptat și verificat prin poziția reală înainte de activitate și cooldown.

Sosirea folosește raza 3D `routine.arrival_radius` (implicit `8.0` în configurația livrată), nu ocuparea exactă a tejghelei/băncii. Override-urile validează și ele destinația, respectă activarea/limita navigației naturale și nu raportează un drum refuzat drept deplasare. Ele nu teleportează și rămân `IDLE` după sosire, fără activitate de muncă/socială inventată din eticheta slotului.

Înainte de mers, rutina verifică traseul calculat: trebuie să fie raportat accesibil de motor și să se termine în raza ancorei, în aceeași lume. Traseele parțiale sau absente nu pornesc navigația. Pentru rutina normală, teleportul rămâne posibil numai conform configurației existente; override-urile nu îl folosesc.

Validarea nu garantează că traseul rămâne liber după calcul, nu implementează tranzacții/animații și nu activează automat rutinele. Simularea independentă rămâne un flux separat. Pentru configurare și limite vezi [destinațiile valide pentru rutine](../reference/configurare-npc-asociere-structuri.md#8-destinații-valide-pentru-rutine).

Pentru diagnostic, compară în GUI `Stare tinta` cu `Stare efectiva`, respectiv `Goal planificat` cu `Obiectiv imediat`. Astfel, un NPC programat la magazin poate fi vizibil în `WALKING` sau `IDLE`, cu motivul de așteptare, fără a fi confundat cu un comerciant deja activ. `/ainpc routine status <nume>` afișează obiectivul curent integral. Obiectivul poate fi schimbat și de simulare; nu este un istoric de erori.

Fallback-ul de teleport verifică acum volumul NPC-ului, chunk-urile necesare, sprijinul sub întreaga bază și coliziunile la țintă. Refuză lichidele și pericolele enumerate în [configurarea rutinelor](../reference/configurare-npc-asociere-structuri.md#verificarea-destinației-înainte-de-teleport). Nu caută automat altă poziție și nu schimbă ancorele. Este o verificare conservatoare înainte de teleport, nu o garanție împotriva redirecționării prin alte pluginuri sau schimbărilor ulterioare ale lumii.
