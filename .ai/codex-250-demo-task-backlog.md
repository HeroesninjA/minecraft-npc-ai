i# Codex 250 Demo Task Backlog

Status: experimental
Generated: 2026-05-26
Purpose: backlog compact pentru Codex, usor de filtrat, impartit si executat incremental.

Format:
`ID | Area | Type | Priority | Scope | Acceptance`

## Build Config

T001 | build | verify | P0 | ruleaza `clean test assemble` local | build verde sau eroare notata
T002 | build | inspect | P1 | verifica JAR core in `ainpc-core-plugin/build/libs` | artifact exista dupa assemble
T003 | build | inspect | P1 | verifica JAR addon medieval in `ainpc-scenario-medieval/build/libs` | artifact exista dupa assemble
T004 | build | inspect | P1 | verifica JAR API in `ainpc-api/build/libs` | artifact exista dupa assemble
T005 | build | docs | P2 | documenteaza comanda standard de build | docs contin comanda exacta
T006 | config | inspect | P0 | verifica `world_admin.enabled` in config template | valoare documentata
T007 | config | inspect | P1 | verifica `routine.enabled` in config template | valoare documentata
T008 | config | inspect | P1 | verifica `simulation.enabled` in config template | valoare documentata
T009 | config | inspect | P1 | verifica fallback fara `OPENAI_API_KEY` | flux notat ca suportat
T010 | config | docs | P2 | noteaza configuratia minima Paper | checklist complet

## Demo Commands

T011 | demo-command | verify | P0 | `/ainpc demo definition` | criterii demo afisate
T012 | demo-command | verify | P0 | `/ainpc demo status demo_sat` | raport PASS/WARN/FAIL
T013 | demo-command | verify | P0 | `/ainpc demo next demo_sat` | blocaje si next steps afisate
T014 | demo-command | verify | P1 | `/ainpc demo script demo_sat <player>` | pasi procedural afisati
T015 | demo-command | verify | P1 | `/ainpc demo phases demo_sat <player>` | D0-D9 afisate
T016 | demo-command | verify | P1 | `/ainpc demo evidence demo_sat <player>` | dovezi necesare listate
T017 | demo-command | verify | P1 | `/ainpc demo runbook demo_sat <player>` | ghid operational afisat
T018 | demo-command | verify | P1 | `/ainpc demo smoke demo_sat <player>` | checklist rapid afisat
T019 | demo-command | verify | P1 | `/ainpc demo summary demo_sat <player>` | sumar operational afisat
T020 | demo-command | verify | P1 | `/ainpc demo commands demo_sat <player>` | lista compacta afisata
T021 | demo-command | verify | P1 | `/ainpc demo restart demo_sat` | restart gate afisat
T022 | demo-command | verify | P2 | `/ainpc demo experimental demo_sat <player>` | output marcat experimental
T023 | demo-command | verify | P2 | `/ainpc demo experimental5 demo_sat <player>` | 5 task-uri afisate
T024 | demo-command | verify | P2 | `/ainpc demo experimental25 demo_sat <player>` | 25 task-uri afisate
T025 | demo-command | verify | P2 | `/ainpc demo experimental25deep demo_sat <player>` | 25 task-uri deep afisate
T026 | demo-command | verify | P2 | `/ainpc demo experimental25ops demo_sat <player>` | 25 task-uri ops afisate
T027 | demo-command | harden | P1 | invalideaza arg extra la `definition` | usage afisat corect
T028 | demo-command | harden | P1 | invalideaza arg extra la `restart` | usage afisat corect
T029 | demo-command | test | P1 | acopera tab-completion demo aliases | test verde
T030 | demo-command | docs | P1 | actualizeaza docs pentru comenzi demo | docs sincronizate

## World Mapping

T031 | world | verify | P0 | `/ainpc world places demo_sat` | regiunea apare
T032 | world | verify | P0 | `/ainpc world demo create demo_sat` | mapping demo creat
T033 | world | verify | P1 | verifica minim 5 places | readiness trece places
T034 | world | verify | P1 | verifica minim 10 nodes | readiness trece nodes
T035 | world | verify | P1 | verifica minim 3 case | house count valid
T036 | world | verify | P1 | verifica minim 1 workplace | work count valid
T037 | world | verify | P1 | verifica minim 1 social hub | social count valid
T038 | world | inspect | P2 | verifica tags pentru case demo | tags coerente
T039 | world | inspect | P2 | verifica tags pentru forge | tags coerente
T040 | world | inspect | P2 | verifica tags pentru farm | tags coerente
T041 | world | inspect | P2 | verifica tags pentru market | tags coerente
T042 | world | inspect | P2 | verifica tags pentru tavern | tags coerente
T043 | world | inspect | P2 | verifica tags pentru altar | tags coerente
T044 | world | harden | P1 | previne creare demo pe lume nepotrivita | warning clar
T045 | world | docs | P2 | noteaza ca demo mapping nu construieste blocuri | docs clare
T046 | world | verify | P1 | `/ainpc world whereami` in demo | locatie explicata
T047 | world | verify | P1 | `/ainpc world save` dupa mapping | save fara eroare
T048 | world | restart | P0 | places persist dupa restart | aceleasi place IDs
T049 | world | restart | P0 | nodes persist dupa restart | aceleasi node IDs
T050 | world | audit | P1 | `/ainpc audit world` dupa create | fara eroare critica

## Settlement Spawn

T051 | settlement | verify | P0 | `/ainpc world settlement plan demo_sat 5` | plan citibil
T052 | settlement | verify | P0 | `/ainpc world settlement spawn demo_sat 5` | spawn executat
T053 | settlement | inspect | P1 | verifica 3-5 NPC-uri demo | count valid
T054 | settlement | inspect | P1 | verifica NPC-uri legate de places demo | binding in regiune
T055 | settlement | inspect | P1 | verifica home place pentru fiecare NPC | home neblank
T056 | settlement | inspect | P1 | verifica work place pentru fiecare NPC | work neblank
T057 | settlement | inspect | P1 | verifica social place pentru fiecare NPC | social neblank
T058 | settlement | harden | P1 | plan cu mai putine case valide | mesaj clar
T059 | settlement | harden | P1 | spawn esuat partial | rollback sau nota clara
T060 | settlement | audit | P1 | `/ainpc audit spawn` dupa spawn | fara duplicate critice
T061 | settlement | repair | P2 | dry-run repair npc-bindings | raport fara apply
T062 | settlement | repair | P2 | dry-run repair mapping-metadata | raport fara apply
T063 | settlement | inspect | P2 | verifica spawn batch recent | batch identificabil
T064 | settlement | restart | P0 | NPC-urile raman dupa restart | list valid
T065 | settlement | restart | P0 | bindings raman dupa restart | bindings list valid
T066 | settlement | docs | P2 | noteaza fallback manual create/bind | docs clare
T067 | settlement | test | P2 | test planner cu mapping demo | test verde
T068 | settlement | test | P2 | test rollback settlement spawn | test verde
T069 | settlement | inspect | P2 | verifica family/household metadata | metadata coerenta
T070 | settlement | inspect | P2 | verifica roluri NPC generate | roluri distincte

## NPC Runtime

T071 | npc | verify | P0 | `/ainpc list` | NPC-uri vizibile
T072 | npc | verify | P1 | `/ainpc info nearest` | profil afisat
T073 | npc | verify | P1 | nearest langa NPC demo | tinta corecta
T074 | npc | inspect | P1 | verifica databaseId pozitiv | id valid
T075 | npc | inspect | P1 | verifica uuid prezent | uuid valid
T076 | npc | inspect | P1 | verifica spawned true pentru demo | spawned valid
T077 | npc | harden | P1 | nearest fara NPC in raza | mesaj clar
T078 | npc | harden | P1 | NPC lipsa dupa restart | repair path documentat
T079 | npc | audit | P1 | `/ainpc audit npc` | fara critic
T080 | npc | repair | P2 | `/ainpc repair duplicates dryrun` | raport clar
T081 | npc | ux | P2 | nume NPC demo lizibile | rol intuitiv
T082 | npc | ux | P2 | ocupatii NPC demo distincte | diversitate vizibila
T083 | npc | ux | P2 | GUI interact arata actiuni | actiuni clare
T084 | npc | docs | P2 | documenteaza alegerea NPC nearest | docs clare
T085 | npc | test | P2 | test duplicate detection | test verde
T086 | npc | test | P2 | test info nearest fallback | test verde
T087 | npc | inspect | P2 | verifica emotii default | valori rezonabile
T088 | npc | inspect | P2 | verifica relatie player initiala | valori rezonabile
T089 | npc | restart | P1 | profil NPC persistat | info stabil
T090 | npc | debug | P2 | debugdump NPC include demo | export valid

## Routine UX

T091 | routine | verify | P0 | `/ainpc routine status nearest` | status clar
T092 | routine | verify | P1 | `/ainpc routine tick` | fara crash
T093 | routine | inspect | P1 | home anchor folosit | home prezent
T094 | routine | inspect | P1 | work anchor folosit | work prezent
T095 | routine | inspect | P1 | social anchor folosit | social prezent
T096 | routine | harden | P1 | binding lipsa | mesaj clar
T097 | routine | harden | P1 | NPC prea departe | mesaj clar
T098 | routine | harden | P1 | teleport/movement haotic | cooldown verificat
T099 | routine | ux | P2 | GUI routine arata slot | slot clar
T100 | routine | ux | P2 | actionbar/mesaj nu spammeaza | UX acceptabil
T101 | routine | audit | P2 | routine status in debugdump | export valid
T102 | routine | restart | P1 | rutina dupa restart | status valid
T103 | routine | docs | P2 | noteaza limita rutinei MVP | docs clare
T104 | routine | test | P2 | test routine assignment | test verde
T105 | routine | test | P2 | test missing binding fallback | test verde
T106 | routine | inspect | P2 | verifica distanta home/work | valori rezonabile
T107 | routine | inspect | P2 | verifica slot timp curent | slot coerent
T108 | routine | inspect | P2 | verifica natural movement config | config citit
T109 | routine | harden | P2 | world/place lipsa | fallback sigur
T110 | routine | verify | P1 | rutina nu modifica DB direct | read-only inspectabil

## Dialogue AI

T111 | dialogue | verify | P1 | click dreapta pe NPC | dialog porneste
T112 | dialogue | verify | P1 | mesaj scurt in chat | raspuns primit
T113 | dialogue | verify | P1 | fara `OPENAI_API_KEY` | fallback stabil
T114 | dialogue | verify | P2 | cu `OPENAI_API_KEY` | rclar
T115 | dialogue | harden | P1 | AI timeout sau raspuns gol | fallback clar
T116 | dialogue | harden | P1 | AI eroare HTTP | fallback clar
T117 | dialogue | safety | P0 | cheia API nu apare in log | secret protejat
T118 | dialogue | safety | P0 | debugdump nu expune secret | secret protejat
T119 | dialogue | inspect | P2 | prompt include world context | context prezent
T120 | dialogue | inspect | P2 | prompt include story context | context prezent
T121 | dialogue | inspect | P2 | prompt include relation/memory | context prezent
T122 | dialogue | harden | P1 | NPC fara context | mesaj clar
T123 | dialogue | ux | P2 | fallback text acceptabil | text clar
T124 | dialogue | docs | P2 | documenteaza AI optional pentru demo | docs clare
T125 | dialogue | test | P2 | test fallback deterministic | test verde
T126 | dialogue | test | P2 | test no secret in debug snapshot | test verde
T127 | dialogue | inspect | P2 | debug OpenAI snapshot | export valid
T128 | dialogue | verify | P1 | dialog nu executa progres direct | runtime safe
T129 | dialogue | verify | P1 | distanta interactiune respectata | limitare valida
T130 | dialogue | restart | P2 | conversatie dupa restart | fallback stabil

## Quest Flow

T131 | quest | verify | P0 | `/ainpc audit quest` | fara critic
T132 | quest | verify | P0 | `/ainpc quest log` | log afisat
T133 | quest | verify | P0 | `/ainpc quest nearest` | oferta sau mesaj clar
T134 | quest | verify | P0 | `/ainpc quest accept nearest` | quest activ
T135 | quest | verify | P1 | `/ainpc quest status nearest` | status afisat
T136 | quest | verify | P1 | `/ainpc quest track start` | tracking porneste
T137 | quest | verify | P1 | `/ainpc quest anchors` | anchors afisate
T138 | quest | inspect | P1 | Q06 disponibil | definition valida
T139 | quest | inspect | P1 | Q07 disponibil | definition valida
T140 | quest | inspect | P1 | Q08 disponibil | definition valida
T141 | quest | harden | P1 | nearest fara quest | mesaj clar
T142 | quest | harden | P1 | accept duplicat | mesaj clar
T143 | quest | harden | P1 | selector invalid | mesaj clar
T144 | quest | audit | P1 | semantic references valide | audit verde
T145 | quest | restart | P0 | quest activ dupa restart | status valid
T146 | quest | debug | P1 | `/ainpc quest debug tracked` | debug citibil
T147 | quest | docs | P2 | documenteaza quest demo minim | docs clare
T148 | quest | test | P2 | test quest selector | test verde
T149 | quest | test | P2 | test quest anchors audit | test verde
T150 | quest | test | P2 | test quest log filters | test verde

## Progression Non Quest

T151 | progression | verify | P0 | `/ainpc progression definitions` | definitions afisate
T152 | progression | verify | P0 | `/ainpc progression stored <player>` | stored afisat
T153 | progression | verify | P1 | `/ainpc contract definitions` | contracte afisate
T154 | progression | verify | P1 | `/ainpc duty definitions` | duties afisate
T155 | progression | verify | P1 | `/ainpc bounty definitions` | bounties afisate
T156 | progression | verify | P1 | `/ainpc event definitions` | events afisate
T157 | progression | verify | P1 | `/ainpc tutorial definitions` | tutorials afisate
T158 | progression | verify | P1 | `/ainpc ritual definitions` | rituals afisate
T159 | progression | inspect | P1 | C02 mapat la market | semantic valid
T160 | progression | inspect | P1 | D01 disponibil | definition valida
T161 | progression | inspect | P1 | B01 disponibil | definition valida
T162 | progression | inspect | P1 | B02 disponibil | definition valida
T163 | progression | inspect | P1 | E01 disponibil | definition valida
T164 | progression | inspect | P1 | T01 disponibil | definition valida
T165 | progression | inspect | P1 | R01 disponibil | definition valida
T166 | progression | harden | P1 | stored fara player | usage clar
T167 | progression | harden | P1 | filter invalid | mesaj clar
T168 | progression | restart | P0 | stored progress dupa restart | progres vizibil
T169 | progression | debug | P1 | debugdump progression | export valid
T170 | progression | test | P2 | test progression filters | test verde

## Story Context

T171 | story | verify | P0 | `/ainpc story context` | context afisat
T172 | story | verify | P0 | `/ainpc story context <player> nearest` | context NPC/player
T173 | story | verify | P1 | `/ainpc story region demo_sat` | state regiune
T174 | story | verify | P1 | `/ainpc story events demo_sat` | events afisate
T175 | story | inspect | P1 | story vede mapping demo | region/place corect
T176 | story | inspect | P1 | story vede quest anchors | anchors incluse
T177 | story | harden | P1 | region fara story | WARN clar
T178 | story | harden | P1 | place invalid | mesaj clar
T179 | story | restart | P0 | story events dupa restart | events persistate
T180 | story | debug | P1 | `/ainpc debugdump story` | export valid
T181 | story | safety | P0 | story read-only pentru AI | scrieri controlate
T182 | story | docs | P2 | documenteaza story minim demo | docs clare
T183 | story | test | P2 | test story event persistence | test verde
T184 | story | test | P2 | test story context snapshot | test verde
T185 | story | audit | P1 | audit quest story actions | fara critic
T186 | story | inspect | P2 | region state JSON | format valid
T187 | story | inspect | P2 | place state JSON | format valid
T188 | story | inspect | P2 | story events JSON | format valid
T189 | story | verify | P1 | story dupa quest completion | event creat
T190 | story | verify | P1 | story fara quest | WARN acceptat

## Audit Debugdump

T191 | audit | verify | P0 | `/ainpc audit all` | fara critic
T192 | audit | verify | P0 | `/ainpc audit db` | fara critic
T193 | audit | verify | P1 | `/ainpc audit npc` | raport valid
T194 | audit | verify | P1 | `/ainpc audit world` | raport valid
T195 | audit | verify | P1 | `/ainpc audit spawn` | raport valid
T196 | audit | verify | P1 | `/ainpc audit quest` | raport valid
T197 | debugdump | verify | P0 | `/ainpc debugdump all` | export creat
T198 | debugdump | verify | P1 | `/ainpc debugdump npc` | export creat
T199 | debugdump | verify | P1 | `/ainpc debugdump world` | export creat
T200 | debugdump | verify | P1 | `/ainpc debugdump quest` | export creat
T201 | debugdump | verify | P1 | `/ainpc debugdump story` | export creat
T202 | debugdump | safety | P0 | cauta chei API in export | zero secrete
T203 | debugdump | inspect | P1 | `world-mapping.json` include semantic_index | index prezent
T204 | debugdump | inspect | P1 | `npc-world-bindings.json` include bindings | bindings prezente
T205 | debugdump | inspect | P1 | `player-progressions.json` include progres | progres prezent
T206 | debugdump | inspect | P1 | `story-events.json` include events | events prezente
T207 | debugdump | inspect | P1 | `quest-audit-report.txt` citibil | raport prezent
T208 | audit | harden | P1 | audit strict quest | fara critic
T209 | debugdump | docs | P2 | documenteaza folder export | docs clare
T210 | audit | test | P2 | test audit output smoke | test verde

## Restart Persistence

T211 | restart | precheck | P0 | `/ainpc demo restart demo_sat` | checklist afisat
T212 | restart | precheck | P0 | `/ainpc audit all` inainte | fara critic
T213 | restart | precheck | P0 | `/ainpc debugdump all` inainte | export creat
T214 | restart | precheck | P0 | `/ainpc world save` | save reusit
T215 | restart | manual | P0 | opreste Paper normal | shutdown curat
T216 | restart | manual | P0 | porneste Paper | plugin enabled
T217 | restart | verify | P0 | `/ainpc world places demo_sat` dupa | mapping persistat
T218 | restart | verify | P0 | `/ainpc list` dupa | NPC persistati
T219 | restart | verify | P0 | `/ainpc world bindings list 20` dupa | bindings persistate
T220 | restart | verify | P0 | `/ainpc progression stored <player>` dupa | progres persistat
T221 | restart | verify | P1 | `/ainpc story events demo_sat` dupa | story persistat
T222 | restart | verify | P0 | `/ainpc demo status demo_sat` dupa | readiness final
T223 | restart | harden | P1 | NPC lipsa dupa restart | recovery plan
T224 | restart | harden | P1 | bindings lipsa dupa restart | repair dryrun
T225 | restart | harden | P1 | progress lipsa dupa restart | DB inspectabil
T226 | restart | docs | P2 | documenteaza restart gate | docs clare
T227 | restart | report | P1 | compara debugdump before/after | diferente notate
T228 | restart | test | P2 | test persistence service | test verde
T229 | restart | safety | P0 | nu folosi kill fortat | procedura clara
T230 | restart | release | P1 | gate doua rulari consecutive | ambele verzi

## Codex Workflow

T231 | codex | hygiene | P0 | marcheaza experimental in final | user informat
T232 | codex | hygiene | P0 | nu promova experimental ca API | risc redus
T233 | codex | hygiene | P1 | ruleaza `clean test assemble` dupa bulk patch | build verde
T234 | codex | hygiene | P1 | pastreaza task-uri in `.ai` | runtime neafectat
T235 | codex | hygiene | P1 | separa docs de cod runtime | diffs clare
T236 | codex | cleanup | P1 | inainte de release sterge moduri experimentale inutile | suprafata redusa
T237 | codex | cleanup | P1 | compacteaza comenzi experimentale pastrate | cod mentenabil
T238 | codex | cleanup | P1 | adauga teste pentru comenzi pastrate | coverage real
T239 | codex | cleanup | P1 | muta task packs in resurse daca raman mari | cod mai curat
T240 | codex | cleanup | P1 | extrage model comun pentru task rows | duplicare redusa
T241 | codex | report | P2 | genereaza raport dupa fiecare experiment | trasabilitate
T242 | codex | report | P2 | noteaza warnings neblocante | context clar
T243 | codex | report | P2 | noteaza fisiere modificate | review usor
T244 | codex | report | P2 | noteaza comenzi rulate | reproducibil
T245 | codex | report | P2 | noteaza ce nu a fost testat pe Paper | risc clar
T246 | codex | planning | P2 | grupeaza task-uri pe P0/P1/P2 | prioritizare clara
T247 | codex | planning | P2 | transforma task-urile in milestones | roadmap clar
T248 | codex | planning | P2 | identifica blocaje reale dupa demo Paper | backlog curatat
T249 | codex | planning | P2 | arhiveaza experimentele nereusite | repo curat
T250 | codex | release | P0 | freeze API inainte de publicare | release controlat
