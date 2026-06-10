# Codex 250 Demo Batch 08: Safe Session Report

Data: 2026-05-26
Interval: T200-T250
Tip: final backlog tracking + safe-session boundary

## Matrice Task-uri Ramase

| ID | Area | Strategy | Session decision | Gate |
| --- | --- | --- | --- | --- |
| T200 | debugdump | LIVE | Pastreaza ca gate Paper | `/ainpc debugdump quest` export creat |
| T201 | debugdump | LIVE | Pastreaza ca gate Paper | `/ainpc debugdump story` export creat |
| T202 | debugdump | MIXED | Verifica regula zero secrete in docs/test ulterior | cauta chei API in export real |
| T203 | debugdump | MIXED | Candidat test JSON export | `world-mapping.json` include semantic_index |
| T204 | debugdump | MIXED | Candidat test JSON export | `npc-world-bindings.json` include bindings |
| T205 | debugdump | MIXED | Candidat test JSON export | `player-progressions.json` include progres |
| T206 | debugdump | MIXED | Candidat test JSON export | `story-events.json` include events |
| T207 | debugdump | MIXED | Candidat test text export | `quest-audit-report.txt` citibil |
| T208 | audit | LIVE | Pastreaza ca gate Paper | audit strict quest fara critic |
| T209 | debugdump | LOCAL | Documentatia trebuie sa indice folderul export | docs clare |
| T210 | audit | LOCAL | Candidat test smoke pentru output audit | test verde ulterior |
| T211 | restart | MIXED | `/ainpc demo restart demo_sat` deja in D8/D9 | checklist afisat live |
| T212 | restart | LIVE | Precheck Paper | `/ainpc audit all` inainte fara critic |
| T213 | restart | LIVE | Precheck Paper | `/ainpc debugdump all` inainte export creat |
| T214 | restart | LIVE | Precheck Paper | `/ainpc world save` reusit |
| T215 | restart | LIVE | Procedura manuala | shutdown curat |
| T216 | restart | LIVE | Procedura manuala | plugin enabled dupa pornire |
| T217 | restart | LIVE | Verificare post-restart | mapping persistat |
| T218 | restart | LIVE | Verificare post-restart | NPC persistati |
| T219 | restart | LIVE | Verificare post-restart | bindings persistate |
| T220 | restart | LIVE | Verificare post-restart | progres persistat |
| T221 | restart | LIVE | Verificare post-restart | story persistat |
| T222 | restart | LIVE | Verificare finala | readiness final |
| T223 | restart | MIXED | Recovery plan in docs | NPC lipsa dupa restart |
| T224 | restart | MIXED | Repair dryrun in docs | bindings lipsa dupa restart |
| T225 | restart | MIXED | DB inspectabil in docs | progress lipsa dupa restart |
| T226 | restart | LOCAL | Restart gate documentat | docs clare |
| T227 | restart | LIVE | Compara exporturi reale | diferente notate |
| T228 | restart | LOCAL | Candidat test persistence service | test verde ulterior |
| T229 | restart | LOCAL | Procedura interzice kill fortat | procedura clara |
| T230 | restart | LIVE | Doua rulari consecutive | ambele verzi |
| T231 | codex | LOCAL | Finalul marcheaza experimental | user informat |
| T232 | codex | LOCAL | Nu promova experimental ca API | risc redus |
| T233 | codex | LOCAL | Ruleaza build dupa bulk patch | build verde |
| T234 | codex | LOCAL | Pastreaza task-uri in `.ai` | runtime neafectat |
| T235 | codex | LOCAL | Separa docs de cod runtime | diffs clare |
| T236 | codex | LOCAL | Cleanup inainte de release | suprafata redusa |
| T237 | codex | LOCAL | Compactare comenzi pastrate | cod mentenabil |
| T238 | codex | LOCAL | Teste pentru comenzi pastrate | coverage real |
| T239 | codex | LOCAL | Task packs mari in resurse daca raman | cod mai curat |
| T240 | codex | LOCAL | Model comun pentru task rows | duplicare redusa |
| T241 | codex | LOCAL | Raport dupa fiecare experiment | trasabilitate |
| T242 | codex | LOCAL | Warnings neblocante notate | context clar |
| T243 | codex | LOCAL | Fisiere modificate notate | review usor |
| T244 | codex | LOCAL | Comenzi rulate notate | reproducibil |
| T245 | codex | LOCAL | Ce nu a fost testat pe Paper notat | risc clar |
| T246 | codex | LOCAL | Grupeaza task-uri pe P0/P1/P2 | prioritizare clara |
| T247 | codex | LOCAL | Transforma task-urile in milestones | roadmap clar |
| T248 | codex | LIVE | Blocaje reale dupa demo Paper | backlog curatat |
| T249 | codex | LOCAL | Arhiveaza experimente nereusite | repo curat |
| T250 | codex | LIVE | Freeze API inainte de publicare | release controlat |

## Concluzie Batch 08

T200-T250 sunt mai grele decat batch-ul anterior pentru ca includ restart, debugdump real si release discipline. Sesiunea poate urmari toate cele 51 task-uri in siguranta, dar executia reala cere Paper live pentru majoritatea P0 restart/debugdump/release gates.
