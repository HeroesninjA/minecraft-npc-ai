# Codex Fast 250 Experimental Backlog

Data: 2026-05-26
Scope: F001-F250

FAST_PROGRAMMING_EXPERIMENT=true
FAST_EXPERIMENTAL_NOT_PUBLIC_API=true
FORCE_CODE_LENGTH_ALLOWED=true
MIN_CHECK_REQUIRED=true
RELEASE_GATE_REQUIRED=true

## Reguli

- Scopul este avans rapid in cod experimental, nu release public.
- Verificari mai putine inseamna gate scurt, nu zero verificari.
- Fiecare task trebuie sa lase macar una dintre: compile, test tintit, smoke command, doc marker sau rollback note.
- Codul mare generat in fast mode ramane intern pana trece prin cleanup si review.
- Taskurile cu risc HIGH nu se promoveaza fara test sau Paper smoke.

## Taskuri

| ID | Area | Mode | Risk | Scope | Min check |
| --- | --- | --- | --- | --- | --- |
| F001 | architecture | fast-code | MED | extrage helper comun pentru texte demo | compile |
| F002 | architecture | fast-code | MED | separa modele command output | compile |
| F003 | architecture | fast-code | MED | compacteaza DTO-uri command routing | compile |
| F004 | architecture | fast-code | MED | elimina duplicare string usage | test tintit |
| F005 | architecture | fast-code | LOW | adauga constants pentru demo aliases | compile |
| F006 | architecture | fast-code | MED | creeaza registry intern pentru demo modes | test tintit |
| F007 | architecture | fast-code | LOW | normalizeaza helper pentru player argument | compile |
| F008 | architecture | fast-code | MED | separa command validation de execution | test tintit |
| F009 | architecture | fast-code | LOW | documenteaza limite fast mode | doc marker |
| F010 | architecture | fast-code | HIGH | muta cod experimental sub gate intern | compile |
| F011 | commands | fast-code | LOW | adauga alias scurt pentru demo status | test tintit |
| F012 | commands | fast-code | MED | adauga subcomanda demo fast summary | test tintit |
| F013 | commands | fast-code | MED | adauga subcomanda demo fast next | test tintit |
| F014 | commands | fast-code | MED | adauga subcomanda demo fast risks | test tintit |
| F015 | commands | fast-code | LOW | imbunatateste mesaj usage demo | test tintit |
| F016 | commands | fast-code | MED | valideaza argumente prea multe | test tintit |
| F017 | commands | fast-code | LOW | tab-complete pentru fast modes | test tintit |
| F018 | commands | fast-code | MED | mesaj fallback pentru region lipsa | test tintit |
| F019 | commands | fast-code | LOW | mesaj fallback pentru player lipsa | test tintit |
| F020 | commands | fast-code | HIGH | gate intern pentru comenzi experimentale | test tintit |
| F021 | world | fast-code | MED | sumar rapid places pe regiune | test tintit |
| F022 | world | fast-code | MED | sumar rapid nodes pe regiune | test tintit |
| F023 | world | fast-code | MED | helper pentru house/work/social counts | test tintit |
| F024 | world | fast-code | LOW | etichete coerente pentru demo places | test tintit |
| F025 | world | fast-code | MED | detectie place fara tags | test tintit |
| F026 | world | fast-code | MED | detectie node fara place valid | test tintit |
| F027 | world | fast-code | MED | raport compact semantic index | test tintit |
| F028 | world | fast-code | HIGH | repair dryrun pentru metadata lipsa | smoke command |
| F029 | world | fast-code | HIGH | nu modifica blocuri in demo mapping fast | test tintit |
| F030 | world | fast-code | MED | documenteaza world fast assumptions | doc marker |
| F031 | settlement | fast-code | MED | planner rapid pentru subset case | test tintit |
| F032 | settlement | fast-code | MED | warning cand max houses limiteaza planul | test tintit |
| F033 | settlement | fast-code | MED | sumar resident capacity | test tintit |
| F034 | settlement | fast-code | MED | detectie casa fara bed node | test tintit |
| F035 | settlement | fast-code | MED | detectie casa fara home node | test tintit |
| F036 | settlement | fast-code | MED | detectie work anchor lipsa | test tintit |
| F037 | settlement | fast-code | MED | detectie social anchor lipsa | test tintit |
| F038 | settlement | fast-code | HIGH | plan spawn fara apply implicit | smoke command |
| F039 | settlement | fast-code | HIGH | rollback note pentru spawn experimental | review note |
| F040 | settlement | fast-code | LOW | raport compact settlement readiness | test tintit |
| F041 | npc | fast-code | MED | profile defaults quick summary | test tintit |
| F042 | npc | fast-code | MED | nearest NPC output mai clar | test tintit |
| F043 | npc | fast-code | MED | fallback cand nearest nu exista | test tintit |
| F044 | npc | fast-code | LOW | include npc key in info compact | compile |
| F045 | npc | fast-code | LOW | include role/profession in info compact | compile |
| F046 | npc | fast-code | MED | warning NPC fara world binding | test tintit |
| F047 | npc | fast-code | MED | warning NPC fara routine anchor | test tintit |
| F048 | npc | fast-code | HIGH | dryrun repair duplicate NPC summary | smoke command |
| F049 | npc | fast-code | HIGH | no destructive NPC delete in fast mode | test tintit |
| F050 | npc | fast-code | LOW | doc marker pentru NPC fast mode | doc marker |
| F051 | routine | fast-code | MED | sumar rutina nearest | test tintit |
| F052 | routine | fast-code | MED | normalizeaza time negative | test tintit |
| F053 | routine | fast-code | MED | fallback work anchor lipsa | test tintit |
| F054 | routine | fast-code | MED | fallback social anchor lipsa | test tintit |
| F055 | routine | fast-code | MED | fallback home anchor lipsa | test tintit |
| F056 | routine | fast-code | LOW | reduce output routine spam | test tintit |
| F057 | routine | fast-code | MED | status compact pentru unsafe needs | test tintit |
| F058 | routine | fast-code | MED | status compact pentru current activity | test tintit |
| F059 | routine | fast-code | HIGH | fast mode nu teleporteaza NPC implicit | test tintit |
| F060 | routine | fast-code | LOW | doc marker routine fast | doc marker |
| F061 | dialogue | fast-code | MED | fallback raspuns AI gol | test tintit |
| F062 | dialogue | fast-code | MED | fallback timeout AI | test tintit |
| F063 | dialogue | fast-code | MED | context compact NPC anchors | test tintit |
| F064 | dialogue | fast-code | LOW | etichete mood in prompt local | compile |
| F065 | dialogue | fast-code | MED | guard secret in AI debug text | test tintit |
| F066 | dialogue | fast-code | HIGH | nu trimite secrete in prompt debug | test tintit |
| F067 | dialogue | fast-code | MED | quick summary pentru last interaction | compile |
| F068 | dialogue | fast-code | LOW | mesaj clar cand OpenAI lipseste | test tintit |
| F069 | dialogue | fast-code | HIGH | fallback local fara crash | test tintit |
| F070 | dialogue | fast-code | LOW | doc marker dialogue fast | doc marker |
| F071 | quest | fast-code | MED | quest status compact nearest | test tintit |
| F072 | quest | fast-code | MED | quest offer fallback fara NPC | test tintit |
| F073 | quest | fast-code | MED | quest accept mesaj clar | test tintit |
| F074 | quest | fast-code | MED | quest track output compact | test tintit |
| F075 | quest | fast-code | MED | quest turn-in failure readable | test tintit |
| F076 | quest | fast-code | HIGH | nu acorda reward duplicat | test tintit |
| F077 | quest | fast-code | HIGH | nu finalizeaza quest invalid | test tintit |
| F078 | quest | fast-code | MED | audit quest quick summary | test tintit |
| F079 | quest | fast-code | HIGH | Paper smoke pentru Q01 fast | smoke command |
| F080 | quest | fast-code | LOW | doc marker quest fast | doc marker |
| F081 | progression | fast-code | MED | progress stored compact | test tintit |
| F082 | progression | fast-code | MED | progress missing player fallback | test tintit |
| F083 | progression | fast-code | MED | progress status filter compact | test tintit |
| F084 | progression | fast-code | MED | counters pentru active/completed/failed | test tintit |
| F085 | progression | fast-code | HIGH | nu pierde progres la reload local | test tintit |
| F086 | progression | fast-code | HIGH | restart gate pentru progres live | smoke command |
| F087 | progression | fast-code | MED | JSON export progression sanity | test tintit |
| F088 | progression | fast-code | MED | audit progression quick summary | test tintit |
| F089 | progression | fast-code | LOW | doc marker progression fast | doc marker |
| F090 | progression | fast-code | MED | normalizeaza player arg in commands | test tintit |
| F091 | story | fast-code | MED | story events compact list | test tintit |
| F092 | story | fast-code | MED | story state compact summary | test tintit |
| F093 | story | fast-code | MED | fallback region fara story | test tintit |
| F094 | story | fast-code | MED | counters story events by type | test tintit |
| F095 | story | fast-code | MED | JSON export story sanity | test tintit |
| F096 | story | fast-code | HIGH | nu dubla story event la retry | test tintit |
| F097 | story | fast-code | HIGH | restart gate story live | smoke command |
| F098 | story | fast-code | LOW | doc marker story fast | doc marker |
| F099 | story | fast-code | MED | audit story quick summary | test tintit |
| F100 | story | fast-code | MED | link story progression compact | test tintit |
| F101 | debugdump | fast-code | MED | validator fisiere declarate | test tintit |
| F102 | debugdump | fast-code | HIGH | redactare sk key | test tintit |
| F103 | debugdump | fast-code | HIGH | redactare bearer token | test tintit |
| F104 | debugdump | fast-code | HIGH | redactare password/token/secret | test tintit |
| F105 | debugdump | fast-code | MED | openai info sanitized | test tintit |
| F106 | debugdump | fast-code | MED | recent server log sanitized | test tintit |
| F107 | debugdump | fast-code | MED | export names stable | test tintit |
| F108 | debugdump | fast-code | HIGH | debugdump live before/after checklist | smoke command |
| F109 | debugdump | fast-code | LOW | doc marker debugdump fast | doc marker |
| F110 | debugdump | fast-code | MED | evidence validator JSON output | script run |
| F111 | audit | fast-code | MED | audit all compact status | test tintit |
| F112 | audit | fast-code | MED | audit npc compact status | test tintit |
| F113 | audit | fast-code | MED | audit quest compact status | test tintit |
| F114 | audit | fast-code | MED | audit mapping compact status | test tintit |
| F115 | audit | fast-code | HIGH | fail pe critic in strict gate | test tintit |
| F116 | audit | fast-code | LOW | warnings neblocante listate | test tintit |
| F117 | audit | fast-code | MED | audit summary in evidence | doc marker |
| F118 | audit | fast-code | HIGH | Paper audit all live gate | smoke command |
| F119 | audit | fast-code | LOW | doc marker audit fast | doc marker |
| F120 | audit | fast-code | MED | quick parser audit output | test tintit |
| F121 | gui | fast-code | LOW | buton routine nearest stabil | test tintit |
| F122 | gui | fast-code | LOW | buton info nearest stabil | test tintit |
| F123 | gui | fast-code | MED | quest GUI fallback fara quest | test tintit |
| F124 | gui | fast-code | MED | progression GUI filter labels | test tintit |
| F125 | gui | fast-code | LOW | text compact pentru status cards | compile |
| F126 | gui | fast-code | MED | no command typo in GUI actions | test tintit |
| F127 | gui | fast-code | LOW | doc marker GUI fast | doc marker |
| F128 | gui | fast-code | MED | inventory title length guard | test tintit |
| F129 | gui | fast-code | MED | pagination bounds guard | test tintit |
| F130 | gui | fast-code | HIGH | GUI commands no destructive default | test tintit |
| F131 | persistence | fast-code | MED | DB table existence quick check | test tintit |
| F132 | persistence | fast-code | MED | binding persistence summary | test tintit |
| F133 | persistence | fast-code | MED | household persistence summary | test tintit |
| F134 | persistence | fast-code | HIGH | no write without transaction note | test tintit |
| F135 | persistence | fast-code | HIGH | restart persistence live gate | smoke command |
| F136 | persistence | fast-code | MED | migration sanity quick output | test tintit |
| F137 | persistence | fast-code | MED | backup note before repair apply | doc marker |
| F138 | persistence | fast-code | LOW | doc marker persistence fast | doc marker |
| F139 | persistence | fast-code | MED | export DB counters in debugdump | test tintit |
| F140 | persistence | fast-code | HIGH | repair commands dryrun default | test tintit |
| F141 | performance | fast-code | LOW | avoid repeated lowercase in loop | compile |
| F142 | performance | fast-code | LOW | cache demo mode aliases | test tintit |
| F143 | performance | fast-code | MED | limit debugdump recent log read | test tintit |
| F144 | performance | fast-code | MED | cap list command output | test tintit |
| F145 | performance | fast-code | MED | cap nearest scan result output | test tintit |
| F146 | performance | fast-code | MED | avoid repeated file scans in tests | test tintit |
| F147 | performance | fast-code | LOW | quick timing note for heavy commands | doc marker |
| F148 | performance | fast-code | HIGH | no async Bukkit unsafe access | compile |
| F149 | performance | fast-code | MED | batch output pagination helper | test tintit |
| F150 | performance | fast-code | LOW | doc marker performance fast | doc marker |
| F151 | security | fast-code | HIGH | secret scan evidence validator | script run |
| F152 | security | fast-code | HIGH | no API key in debugdump | test tintit |
| F153 | security | fast-code | HIGH | no bearer in logs | test tintit |
| F154 | security | fast-code | MED | mask RCON password in docs examples | doc marker |
| F155 | security | fast-code | MED | mark backups as sensitive | doc marker |
| F156 | security | fast-code | HIGH | no broad host bind change | review note |
| F157 | security | fast-code | MED | gate experimental public API | test tintit |
| F158 | security | fast-code | MED | evidence no secrets final gate | script run |
| F159 | security | fast-code | LOW | doc marker security fast | doc marker |
| F160 | security | fast-code | HIGH | fail strict validator on secrets | script run |
| F161 | scripts | fast-code | MED | helper validate evidence template | script run |
| F162 | scripts | fast-code | MED | helper summarize fast backlog | script run |
| F163 | scripts | fast-code | MED | helper list high risk fast tasks | script run |
| F164 | scripts | fast-code | MED | helper generate session slice | script run |
| F165 | scripts | fast-code | HIGH | scripts never store secrets | script run |
| F166 | scripts | fast-code | LOW | README examples for validators | doc marker |
| F167 | scripts | fast-code | LOW | JSON output for fast validation | script run |
| F168 | scripts | fast-code | MED | nonzero exit on strict failure | script run |
| F169 | scripts | fast-code | MED | allow pending only for templates | script run |
| F170 | scripts | fast-code | LOW | doc marker scripts fast | doc marker |
| F171 | tests | fast-code | MED | backlog shape test for F001-F250 | test tintit |
| F172 | tests | fast-code | MED | report index includes fast backlog | test tintit |
| F173 | tests | fast-code | MED | high risk tasks require non-doc check | test tintit |
| F174 | tests | fast-code | MED | no zero-check task allowed | test tintit |
| F175 | tests | fast-code | MED | fast markers exist | test tintit |
| F176 | tests | fast-code | LOW | compile-only tasks counted | test tintit |
| F177 | tests | fast-code | LOW | smoke-command tasks counted | test tintit |
| F178 | tests | fast-code | LOW | script-run tasks counted | test tintit |
| F179 | tests | fast-code | MED | release gate marker required | test tintit |
| F180 | tests | fast-code | LOW | doc marker tests fast | doc marker |
| F181 | docs | fast-doc | LOW | fast programming rule summary | doc marker |
| F182 | docs | fast-doc | LOW | explain fewer checks policy | doc marker |
| F183 | docs | fast-doc | LOW | explain release gate required | doc marker |
| F184 | docs | fast-doc | LOW | explain high risk cleanup | doc marker |
| F185 | docs | fast-doc | LOW | explain force code length caution | doc marker |
| F186 | docs | fast-doc | LOW | command list for fast demo | doc marker |
| F187 | docs | fast-doc | LOW | Paper live proof references | doc marker |
| F188 | docs | fast-doc | LOW | evidence validator references | doc marker |
| F189 | docs | fast-doc | LOW | changelog discipline fast | doc marker |
| F190 | docs | fast-doc | LOW | docs fast marker complete | doc marker |
| F191 | release | fast-code | HIGH | release checklist blocks experimental | test tintit |
| F192 | release | fast-code | HIGH | fail if API promoted without marker | test tintit |
| F193 | release | fast-code | MED | freeze report mentions fast backlog | doc marker |
| F194 | release | fast-code | HIGH | build before any release decision | script run |
| F195 | release | fast-code | HIGH | Paper smoke before release decision | smoke command |
| F196 | release | fast-code | MED | keep internal decision path | doc marker |
| F197 | release | fast-code | MED | remove decision path | doc marker |
| F198 | release | fast-code | HIGH | promote decision requires tests | test tintit |
| F199 | release | fast-code | LOW | doc marker release fast | doc marker |
| F200 | release | fast-code | HIGH | no release from fast backlog alone | review note |
| F201 | integration | fast-code | MED | demo status includes fast notice | test tintit |
| F202 | integration | fast-code | MED | demo summary references live checklist | test tintit |
| F203 | integration | fast-code | MED | command help references experimental | test tintit |
| F204 | integration | fast-code | HIGH | Paper boot smoke after command changes | smoke command |
| F205 | integration | fast-code | HIGH | Paper debugdump smoke after debug changes | smoke command |
| F206 | integration | fast-code | HIGH | Paper quest smoke after quest changes | smoke command |
| F207 | integration | fast-code | HIGH | Paper restart smoke after persistence changes | smoke command |
| F208 | integration | fast-code | MED | report all fast changes by batch | doc marker |
| F209 | integration | fast-code | LOW | index all fast reports | test tintit |
| F210 | integration | fast-code | MED | integration marker complete | test tintit |
| F211 | cleanup | fast-code | MED | identify generated code duplication | review note |
| F212 | cleanup | fast-code | MED | compact large helper functions | compile |
| F213 | cleanup | fast-code | MED | remove unused fast constants | compile |
| F214 | cleanup | fast-code | MED | move big static text to resource if needed | compile |
| F215 | cleanup | fast-code | LOW | cleanup docs for stale commands | doc marker |
| F216 | cleanup | fast-code | MED | delete abandoned experimental modes only with approval | review note |
| F217 | cleanup | fast-code | HIGH | no destructive cleanup without explicit request | review note |
| F218 | cleanup | fast-code | MED | rerun targeted tests after cleanup | test tintit |
| F219 | cleanup | fast-code | MED | rerun full build after cleanup | script run |
| F220 | cleanup | fast-code | LOW | cleanup marker complete | doc marker |
| F221 | batch-planning | fast-doc | LOW | split implementation in 10-task slices | doc marker |
| F222 | batch-planning | fast-doc | LOW | split risky work separate from docs | doc marker |
| F223 | batch-planning | fast-doc | MED | limit HIGH tasks per slice to 3 | doc marker |
| F224 | batch-planning | fast-doc | LOW | prefer compile/test after each slice | doc marker |
| F225 | batch-planning | fast-doc | LOW | record skipped checks explicitly | doc marker |
| F226 | batch-planning | fast-doc | MED | do not mix release with experimental slice | doc marker |
| F227 | batch-planning | fast-doc | LOW | keep user-facing summary short | doc marker |
| F228 | batch-planning | fast-doc | MED | create batch report per slice | doc marker |
| F229 | batch-planning | fast-doc | LOW | mark nonexecuted Paper gates | doc marker |
| F230 | batch-planning | fast-doc | LOW | planning marker complete | doc marker |
| F231 | fast-session | fast-code | LOW | session 01 architecture commands | test tintit |
| F232 | fast-session | fast-code | LOW | session 02 world settlement | test tintit |
| F233 | fast-session | fast-code | LOW | session 03 npc routine gui | test tintit |
| F234 | fast-session | fast-code | MED | session 04 dialogue quest | test tintit |
| F235 | fast-session | fast-code | MED | session 05 progression story | test tintit |
| F236 | fast-session | fast-code | MED | session 06 audit debugdump | test tintit |
| F237 | fast-session | fast-code | MED | session 07 scripts evidence | script run |
| F238 | fast-session | fast-code | HIGH | session 08 persistence restart gates | smoke command |
| F239 | fast-session | fast-code | HIGH | session 09 release discipline | test tintit |
| F240 | fast-session | fast-code | MED | session 10 cleanup optimization | script run |
| F241 | report | fast-doc | LOW | report each fast slice | doc marker |
| F242 | report | fast-doc | LOW | list files changed per slice | doc marker |
| F243 | report | fast-doc | LOW | list tests skipped per slice | doc marker |
| F244 | report | fast-doc | MED | list high risk tasks per slice | doc marker |
| F245 | report | fast-doc | MED | list Paper gates remaining | doc marker |
| F246 | report | fast-doc | LOW | update report index | test tintit |
| F247 | report | fast-doc | LOW | update changelog memory | changelog |
| F248 | report | fast-doc | MED | final fast report T001 equivalent | test tintit |
| F249 | report | fast-doc | HIGH | final release blocker report | review note |
| F250 | report | fast-doc | HIGH | final decision: remove keep internal promote | review note |
