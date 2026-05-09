# Questuri Avansate V2 - Faze, diversitate si mecanici

Actualizat: 2026-05-08

Status: document canonic pentru directia V2. Versiunea veche este pastrata pentru istoric in `arhiva/questuri-avansate-v1.md`.

## Scop

V2 organizeaza evolutia questurilor avansate in faze mici, verificabile si jucabile. Directia nu este sa existe doar mai multe questuri, ci sa existe continut variat care foloseste aceeasi infrastructura: mapping semantic, story state, audit, debugdump si `ProgressionService`.

Obiectivul practic:

- questurile clasice raman functionale prin fatada existenta de quest;
- continutul nou poate fi quest, contract, datorie, event local, bounty, ritual, tutorial sau alta mecanica cu progres;
- fiecare mecanica foloseste aceleasi reguli de persistenta, selectie, audit si observabilitate;
- AI-ul poate propune drafturi, dar runtime-ul executa doar definitii validate;
- fiecare slice nou trebuie sa fie demonstrabil pe Paper sau prin smoke test dedicat.

## Regula de baza

Nu se adauga un nou tip de quest sau mecanica daca nu exista toate piesele minime:

- definitie YAML stabila;
- listener sau trigger determinist;
- persistenta prin quest runtime sau `ProgressionService`;
- audit pentru configuratie invalida;
- debugdump pentru inspectie;
- test automat sau smoke script;
- documentatie scurta pentru autorul de continut.

## Stare de pornire V2

Exista deja baza necesara pentru V2:

- Q01-Q05 sunt questuri de baza functionale;
- Q06-Q08 folosesc stages liniare, obiective mapate si story events;
- C02 foloseste mecanica `village_contracts` ca progres non-quest, cu `base_type: TRADE_DEAL`;
- D01 foloseste mecanica `npc_duties` ca sarcina NPC non-quest, cu `base_type: DUTY`;
- B01 si B02 folosesc mecanica `local_bounties` ca bounty-uri locale non-quest, cu `base_type: BOUNTY`;
- E01 foloseste mecanica `village_events` ca eveniment local non-quest, cu `base_type: WORLD_EVENT`;
- T01 foloseste mecanica `onboarding` ca tutorial non-quest, cu `base_type: TUTORIAL`;
- R01 foloseste mecanica `village_rituals` ca ritual non-quest, cu `base_type: RITUAL`;
- `QuestScenarioContract` are tipuri de scenariu si metadata pentru selectie;
- `ProgressionService` este directia pentru progres generic peste questuri si mecanici;
- `ProgressionFilter` permite selectii explicite precum `scenario:investigation`, `base:TRADE_DEAL`, `mechanic:village_contracts`, `kind:bounty`, `kind:tutorial`, `kind:ritual`, `category:side`;
- `semantic_index` din world mapping permite audit pentru `visit_region`, `visit_place` si `inspect_node`;
- `/ainpc audit quest` si `/ainpc debugdump quest` sunt suprafetele principale de verificare.

## Faza V2.0 - Arhivare si compatibilitate

Scop: separa documentatia istorica de planul curent, fara schimbari runtime.

Livrabile:

- documentul vechi este pastrat in `docs/arhiva/questuri-avansate-v1.md`;
- documentul canonic devine `docs/questuri-avansate-v2.md`;
- indexurile si documentele active trimit catre V2;
- V1 ramane doar referinta istorica, nu specificatie activa.

Gate:

- cautarea dupa numele istoric al documentului nu mai gaseste referinte active in documentatia canonica, cu exceptia arhivei daca este mentionata explicit.

## Faza V2.1 - Diversitate controlata de questuri

Scop: adauga varietate de questuri fara sa fragmentezi runtime-ul.

Tipuri recomandate pentru primul val:

| Tip | Rol jucabil | Obiective probabile | Dependinte minime |
|---|---|---|---|
| `main_story` | traseu principal pentru demo | `talk_to_npc`, `visit_place`, `inspect_node`, `deliver_to_npc` | story state, stages |
| `side_story` | continut optional cu recompensa | `collect_item`, `deliver_to_npc`, `talk_to_npc` | quest log, rewards |
| `investigation` | cautare de indicii | `visit_place`, `inspect_node`, `talk_to_npc` | mapping semantic, audit anchors |
| `exploration` | descoperire de locuri | `visit_region`, `visit_place`, `inspect_node` | mapping si world admin |
| `delivery` | transport intre NPC-uri/locuri | `collect_item`, `deliver_to_npc`, `visit_place` | inventory checks |
| `social` | dialog si relatie locala | `talk_to_npc`, story action | dialogue context, story events |
| `hunt` | pericol local controlat | `visit_region`, `kill_mob`, `talk_to_npc` | entity validation, region anchor |
| `trade` | schimb, aprovizionare, piata | `collect_item`, `deliver_to_npc`, `inspect_node` | economie light sau item rewards |
| `tutorial` | invatare mecanici | pasi mici, explicit trackable | GUI/help text, no branching complex |
| `event` | episod local temporar | mix scurt de obiective | cooldown, start/end clar |

Regula de diversitate:

- nu adauga doua questuri consecutive cu acelasi tip dominant;
- fiecare quest nou trebuie sa foloseasca cel putin o ancora semantica sau un story event daca este parte din demo;
- fiecare grup de 3 questuri trebuie sa includa cel putin doua familii de obiective diferite;
- questurile de investigatie si explorare trebuie sa aiba audit strict pe mapping;
- questurile de lupta trebuie sa aiba fallback daca entitatea sau zona nu este disponibila.

## Faza V2.2 - Mapping semantic ca infrastructura de gameplay

Scop: questurile nu mai folosesc coordonate fragile ca sursa principala de adevar.

Reguli:

- `visit_region` trebuie sa tina de `RegionDefinition`;
- `visit_place` trebuie sa tina de place ID, tag sau metadata acceptata de `semantic_index`;
- `inspect_node` trebuie sa tina de node ID, tag sau metadata auditabila;
- `quest_anchor_bindings` ramane legatura persistenta intre template si mapping concret;
- story events trebuie sa poata indica regiunea, place-ul sau node-ul relevant.

Livrabile:

- audit mai clar pentru ancore lipsa sau ambigue;
- debugdump care arata candidatii de mapping folositi de questuri;
- exemple YAML pentru `tag:market`, node semantic si regiune;
- smoke test Paper pentru un quest care cere deplasare reala in place/node.

Gate:

- un autor de content poate vedea din audit de ce un obiectiv mapat nu este jucabil;
- un admin poate confirma din debugdump ce mapping a fost folosit.

## Faza V2.3 - Mecanici non-quest peste acelasi progres

Scop: foloseste infrastructura de quest pentru alte mecanici, fara sa le fortezi pe toate sa apara ca questuri clasice.

Mecanici tinta:

| Mecanica | `base_type` sugerat | `mechanic` | Exemplu |
|---|---|---|---|
| contracte de sat | `TRADE_DEAL` sau `CONTRACT` | `village_contracts` | verificare avizier, livrare, piata |
| datorii/roluri | `DUTY` | `npc_duties` | strajerul cere patrula, fierarul cere materiale |
| bounty local | `BOUNTY` | `local_bounties` | curata zona de mobs si raporteaza |
| event de sat | `WORLD_EVENT` | `village_events` | fantana stricata, targ, alarma |
| ritual/ceremonie | `RITUAL` | `village_rituals` | strange obiecte si activeaza loc semantic |
| tutorial | `TUTORIAL` | `onboarding` | invata quest log, inspectie, tracking |
| reputatie light | `RELATION` | `reputation_steps` | ajuta un NPC si inregistreaza story event |
| mentenanta lume | `TASK` | `settlement_tasks` | repara node, marcheaza lipsa, inchide gap |

Reguli:

- o mecanica non-quest poate avea progres, status si reward fara sa fie afisata automat in quest log principal;
- comenzile si GUI-ul trebuie sa poata filtra dupa `base_type`, `mechanic`, `category` si `scenarioKind`;
- persistenta trebuie sa fie comuna cu progresul generic;
- fiecare mecanica trebuie sa declare explicit daca este `repeatable`, `tracked`, `hidden`, `timed` sau `admin_only`;
- un addon nu primeste runtime propriu daca poate folosi `ProgressionService`.

## Faza V2.4 - Extractie reala in ProgressionService

Scop: Quest runtime-ul ramane compatibil, dar progresul comun se muta treptat intr-un serviciu generic.

Ordine recomandata:

1. pastreaza `Quest` ca fatada pentru comenzile si logica existente;
2. muta definitiile comune in modele de progres generic;
3. ruleaza obiectivele printr-un evaluator comun;
4. ruleaza rewards/actions printr-un registry controlat;
5. expune selectii read-only pentru GUI, debug si addonuri;
6. abia apoi reduci duplicarea din `ScenarioEngine`.

Nu muta totul intr-un singur PR mare. Fiecare extractie trebuie sa lase Q01-Q08, C02, D01, B01/B02, E01, T01 si R01 functionale.

Gate:

- `quest status`, `quest track`, `progression definitions` si `progression stored` arata aceeasi stare logica;
- progress export include `category`, `scenarioKind`, `baseType` si `mechanic`;
- testele vechi de quest trec fara schimbari de comportament vizibil.

## Faza V2.5 - Story, reputatie, economie si conditii

Scop: questurile si mecanicile devin sensibile la lume, dar raman deterministe.

Straturi permise:

- `StoryStateService` pentru stari persistente pe regiune/place;
- `record_story_event` pentru istoric verificabil;
- conditii simple pe story state si progress status;
- reputatie light pe NPC, familie, sat sau factiune;
- economie light prin iteme sau moneda controlata;
- cooldown si repeat rules pentru contracte si events.

Ce nu se face inca:

- branching generativ liber;
- reward-uri decise de AI;
- conditii ascunse fara audit;
- reputatie globala mare inainte de demo-ul jucabil;
- economie complexa inainte sa existe smoke test pentru rewards.

Gate:

- orice conditie care blocheaza un quest apare in audit/debugdump;
- orice story action are tip, tinta si payload validat;
- un quest ramane recuperabil dupa restart daca este in mijlocul unei etape.

## Faza V2.6 - UX, comenzi si observabilitate

Scop: continutul variat trebuie sa fie usor de operat de admin si clar pentru jucator.

Suprafete necesare:

- quest log cu grupare dupa `main`, `side`, `contract`, `event`, `tutorial` si `ritual`;
- status scurt pentru progresul activ;
- filtre admin pentru `mechanic`, `base_type`, `scenarioKind`, `status`, `tracked`;
- debugdump cu definitii, progres, anchors, semantic index si story events relevante;
- audit cu severitati clare: error pentru imposibil, warning pentru fragil, info pentru recomandari;
- smoke script pentru scenariul medieval jucabil.

Reguli de UX:

- jucatorul nu trebuie sa vada termeni tehnici precum `base_type` sau `semantic_index`;
- adminul trebuie sa ii vada in comenzi si debugdump;
- progresul non-quest poate fi ascuns din quest log, dar nu din exporturile admin;
- mecanicile temporare trebuie sa aiba timp, cooldown sau stare finala clara.

## Faza V2.7 - Demo Paper jucabil

Scop: valideaza sistemul prin gameplay, nu doar prin structura de cod.

Scenariul minim:

- un sat medieval cu mapping semantic inspectabil;
- 3-5 questuri variate, nu doar delivery;
- cel putin un contract non-quest;
- cel putin un quest de investigatie;
- cel putin un tutorial/onboarding scurt;
- cel putin o mecanica ritual/event care foloseste un node semantic dedicat;
- cel putin un obiectiv legat de place/node;
- cel putin un story event vizibil in context;
- save, reload si continuare progres;
- audit curat pentru continutul demo;
- debugdump suficient pentru diagnostic.

Gate final:

- un tester poate porni serverul, accepta continutul, finaliza macar doua fluxuri diferite si confirma progresul dupa restart;
- adminul poate rula audit/debugdump si intelege ce lipseste daca un obiectiv nu merge;
- documentatia indica exact ce este implementat si ce ramane backlog.

## Matrice pentru questuri noi

Fiecare intrare noua de content trebuie descrisa inainte de implementare:

| Camp | Intrebare |
|---|---|
| `code` | Care este ID-ul stabil pentru progres si smoke test? |
| `kind` | Ce tip dominant are: `investigation`, `social`, `hunt`, `trade`, `event`? |
| `base_type` | Este quest clasic sau mecanica separata? |
| `mechanic` | In ce familie intra pentru GUI/debug/filter? |
| `mapping` | Ce regiune/place/node foloseste? |
| `objectives` | Ce listener sau trigger actualizeaza progresul? |
| `story` | Ce stare sau eveniment lasa in urma? |
| `reward` | Ce primeste jucatorul si cum se valideaza? |
| `repeat` | Este one-shot, repeatable, cooldown sau event temporar? |
| `tests` | Ce test sau smoke acopera fluxul? |

## Exemple de backlog V2

Aceste exemple sunt directii de continut, nu promisiuni ca sunt implementate deja.

| Cod | Tip | Mecanica | Descriere scurta |
|---|---|---|---|
| Q09 | `exploration` | quest clasic | jucatorul descopera un place si inspecteaza un node vechi |
| Q10 | `social` | quest clasic | jucatorul discuta cu doi martori si intoarce concluzia la giver |
| Q11 | `hunt` | quest clasic | patrula intr-o regiune, eliminare mob, raportare |
| C03 | `investigation` | `village_contracts` | contract de piata legat de avizier si negustor |
| D02 | `duty` | `npc_duties` | urmatoarea sarcina de rol data de strajer sau fierar |
| B03 | `hunt` | `local_bounties` | urmatorul bounty local cu alt giver, cooldown si reward separat |
| E02 | `event` | `village_events` | urmatorul eveniment temporar de sat cu start/end explicit |
| T02 | `tutorial` | `onboarding` | urmatorul tutorial pentru tracking, inspectie si raportare |
| R02 | `ritual` | `village_rituals` | urmatorul ritual care foloseste iteme si node semantic pentru ceremonie |

## Slice standard de lucru

Pentru fiecare pas nou se lucreaza in ordinea:

1. mapping sau ancora semantica necesara;
2. definitie YAML pentru quest/mecanica;
3. contract runtime si progres;
4. audit si debugdump;
5. test automat;
6. smoke Paper daca fluxul atinge serverul real;
7. documentatie si changelog.

Acest protocol tine questurile, mapping-ul si `ProgressionService` sincronizate si evita un backlog mare de continut neverificat.

## Interdictii temporare

Pana cand V2.7 este validat pe Paper:

- nu se introduce branching complex cu multe conditii;
- nu se lasa AI-ul sa acorde reward-uri sau sa schimbe progres direct;
- nu se creeaza engine separat pentru fiecare mecanica;
- nu se adauga economie sau reputatie globala ca dependinta obligatorie;
- nu se adauga questuri care cer mapping neauditabil;
- nu se considera un quest "gata" fara persistenta si debugdump.

## Legaturi active

- `pregatire-questuri-avansate.md` pentru pregatirile deja facute pe Q06-Q08;
- `questuri-faza-1-stabilizare.md` pentru stabilizarea questurilor de baza;
- `quest-anchor-bindings.md` pentru persistenta ancorelor semantice;
- `progression-service.md` pentru extractia progresului generic;
- `lucru-alternat-quest-mapping-progression.md` pentru protocolul de lucru alternat;
- `mapping.md` pentru modelul de lume consumat de questuri;
- `story-context-service.md` si `story-si-context-ai.md` pentru story state si context AI;
- `ai-orchestrare-si-mecanici.md` pentru regula ca AI-ul propune, iar serviciile deterministe executa.
