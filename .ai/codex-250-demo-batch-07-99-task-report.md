# Codex 250 Demo Batch 07: 99-Task Prompt Report

Data: 2026-05-26
Interval: T101-T199
Tip: experimental orchestration, local validation first

## Regula testului

Acest batch testeaza limita operationala "99 task-uri per prompt" fara sa pretinda executie live Paper pentru task-uri care cer server, restart sau jucator real. Fiecare rand pastreaza un ID real din backlog si marcheaza strategia curenta:

- `LOCAL`: acoperibil prin teste statice/unitare sau documentatie.
- `LIVE`: necesita server Paper, jucator, entitati sau restart.
- `MIXED`: are parte locala plus verificare live ramasa.

## Matrice 99 Task-uri

| ID | Area | Strategy | Local action | Live gate |
| --- | --- | --- | --- | --- |
| T101 | routine | LOCAL | Verifica prezenta routine/debugdump in suprafete si docs | confirma export live |
| T102 | routine | LIVE | Documenteaza restart gate | ruleaza status dupa restart |
| T103 | routine | LOCAL | Documentatia D4 noteaza limita MVP | review docs |
| T104 | routine | LOCAL | `RoutineEngineTest` acopera assignment-uri | test verde |
| T105 | routine | LOCAL | `RoutineEngineTest` acopera fallback fara ancore | test verde |
| T106 | routine | MIXED | Status afiseaza distanta home/work cand exista locatie | verifica valori live |
| T107 | routine | LOCAL | Preview day si time normalization acopera sloturi | test verde |
| T108 | routine | MIXED | Status afiseaza config natural movement | verifica config live |
| T109 | routine | LOCAL | Fallback fara anchor ramane IDLE cu motiv | test verde |
| T110 | routine | LOCAL | Routine preview/tick tratate ca inspectie, nu DB edit direct | audit live optional |
| T111 | dialogue | LIVE | Pastreaza in script demo click dreapta | dialog porneste live |
| T112 | dialogue | LIVE | Pastreaza smoke mesaj scurt | raspuns primit live |
| T113 | dialogue | MIXED | Contract fallback fara API in docs | test server fara cheie |
| T114 | dialogue | LIVE | Noteaza test cu API optional | raspuns real |
| T115 | dialogue | MIXED | Backlog include timeout/raspuns gol | fallback live |
| T116 | dialogue | MIXED | Backlog include eroare HTTP | fallback live |
| T117 | dialogue | MIXED | Secret safety ramane gate in docs | inspect log live |
| T118 | dialogue | MIXED | Debugdump fara secret ramane gate | inspect export live |
| T119 | dialogue | MIXED | World context mentionat in D7 | verifica prompt/debug |
| T120 | dialogue | MIXED | Story context mentionat in D7 | verifica prompt/debug |
| T121 | dialogue | MIXED | Relation/memory mentionat in D7 | verifica prompt/debug |
| T122 | dialogue | MIXED | NPC fara context trebuie mesaj clar | test live |
| T123 | dialogue | MIXED | Fallback text acceptabil este gate | review live |
| T124 | dialogue | LOCAL | AI optional documentat in D7 | docs clare |
| T125 | dialogue | LOCAL | Candidat pentru test fallback deterministic | adauga test ulterior |
| T126 | dialogue | LOCAL | Candidat pentru test no-secret snapshot | adauga test ulterior |
| T127 | dialogue | MIXED | Debug OpenAI snapshot in docs | export valid live |
| T128 | dialogue | MIXED | Dialog nu executa progres direct in gate D7 | audit runtime |
| T129 | dialogue | LIVE | Distanta interactiune este verificare live | test server |
| T130 | dialogue | LIVE | Conversatie dupa restart ramane live-only | restart server |
| T131 | quest | MIXED | `/ainpc audit quest` in smoke D5 | audit live |
| T132 | quest | MIXED | `/ainpc quest log` in smoke D5 | log live |
| T133 | quest | MIXED | `/ainpc quest nearest` in smoke D5 | oferta/mesaj live |
| T134 | quest | MIXED | `/ainpc quest accept nearest` in smoke D5 | quest activ live |
| T135 | quest | MIXED | `/ainpc quest status nearest` in smoke D5 | status live |
| T136 | quest | MIXED | `/ainpc quest track start` in smoke D5 | tracking live |
| T137 | quest | MIXED | `/ainpc quest anchors` in smoke D5 | anchors live |
| T138 | quest | LOCAL | Q06 disponibil ramane candidat de definitional test | test ulterior |
| T139 | quest | LOCAL | Q07 disponibil ramane candidat de definitional test | test ulterior |
| T140 | quest | LOCAL | Q08 disponibil ramane candidat de definitional test | test ulterior |
| T141 | quest | MIXED | nearest fara quest cere mesaj clar | live nearest |
| T142 | quest | MIXED | accept duplicat cere mesaj clar | live progression |
| T143 | quest | MIXED | selector invalid cere mesaj clar | handler/live |
| T144 | quest | MIXED | semantic references valide prin audit | audit live |
| T145 | quest | LIVE | quest activ dupa restart | restart server |
| T146 | quest | MIXED | debug tracked mentionat ca inspectie | command live |
| T147 | quest | LOCAL | Quest demo minim documentat in D5 | docs clare |
| T148 | quest | LOCAL | Candidat test quest selector | test ulterior |
| T149 | quest | LOCAL | Candidat test quest anchors audit | test ulterior |
| T150 | quest | LOCAL | Candidat test quest log filters | test ulterior |
| T151 | progression | MIXED | definitions listate in D5 | command live |
| T152 | progression | MIXED | stored player listat in D5 | command live |
| T153 | progression | MIXED | contract definitions listat in D5 | command live |
| T154 | progression | MIXED | duty definitions listat in D5 | command live |
| T155 | progression | MIXED | bounty definitions listat in D5 | command live |
| T156 | progression | MIXED | event definitions listat in D5 | command live |
| T157 | progression | MIXED | tutorial definitions listat in D5 | command live |
| T158 | progression | MIXED | ritual definitions listat in D5 | command live |
| T159 | progression | LOCAL | C02 market mapping candidat test semantic | test ulterior |
| T160 | progression | LOCAL | D01 definition candidat test | test ulterior |
| T161 | progression | LOCAL | B01 definition candidat test | test ulterior |
| T162 | progression | LOCAL | B02 definition candidat test | test ulterior |
| T163 | progression | LOCAL | E01 definition candidat test | test ulterior |
| T164 | progression | LOCAL | T01 definition candidat test | test ulterior |
| T165 | progression | LOCAL | R01 definition candidat test | test ulterior |
| T166 | progression | MIXED | stored fara player trebuie usage clar | handler/live |
| T167 | progression | MIXED | filter invalid trebuie mesaj clar | handler/live |
| T168 | progression | LIVE | stored progress dupa restart | restart server |
| T169 | progression | MIXED | debugdump progression in D8 | export live |
| T170 | progression | LOCAL | Candidat test progression filters | test ulterior |
| T171 | story | MIXED | story context in D6 | command live |
| T172 | story | MIXED | story context player nearest in D6 | command live |
| T173 | story | MIXED | story region demo_sat in D6 | command live |
| T174 | story | MIXED | story events demo_sat in D6 | command live |
| T175 | story | MIXED | story vede mapping demo prin D6 gate | inspect live |
| T176 | story | MIXED | story vede quest anchors prin D6 gate | inspect live |
| T177 | story | MIXED | region fara story cere WARN clar | handler/live |
| T178 | story | MIXED | place invalid cere mesaj clar | handler/live |
| T179 | story | LIVE | story events dupa restart | restart server |
| T180 | story | MIXED | debugdump story in D6/D8 | export live |
| T181 | story | MIXED | story read-only pentru AI in D6 gate | audit runtime |
| T182 | story | LOCAL | Story minim demo documentat in D6 | docs clare |
| T183 | story | LOCAL | Candidat test story event persistence | test ulterior |
| T184 | story | LOCAL | Candidat test story context snapshot | test ulterior |
| T185 | story | MIXED | audit quest story actions | audit live |
| T186 | story | LOCAL | Candidat JSON format region state | test ulterior |
| T187 | story | LOCAL | Candidat JSON format place state | test ulterior |
| T188 | story | LOCAL | Candidat JSON format story events | test ulterior |
| T189 | story | LIVE | story dupa quest completion | scenario live |
| T190 | story | MIXED | story fara quest WARN acceptat | command live |
| T191 | audit | MIXED | audit all in D8 | command live |
| T192 | audit | MIXED | audit db in D1/D8 | command live |
| T193 | audit | MIXED | audit npc in D3 | command live |
| T194 | audit | MIXED | audit world in D2 | command live |
| T195 | audit | MIXED | audit spawn in D3 | command live |
| T196 | audit | MIXED | audit quest in D5 | command live |
| T197 | debugdump | MIXED | debugdump all in D8 | export live |
| T198 | debugdump | MIXED | debugdump npc in evidence checklist | export live |
| T199 | debugdump | MIXED | debugdump world in evidence checklist | export live |

## Rezumat numeric

| Strategy | Count |
| --- | ---: |
| LOCAL | 25 |
| MIXED | 60 |
| LIVE | 14 |
| TOTAL | 99 |

## Verificare locala

Acest raport este validat de `CodexDemoBacklogTest.codexDemoBatchSevenTracksExactlyNinetyNineBacklogTasks`.
