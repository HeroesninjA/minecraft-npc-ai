# Urmatoarele 250 Faze

Actualizat: 2026-05-09

## Scop

Acest document este backlog operational pe termen lung pentru AINPC. Nu inlocuieste documentele canonice, ci le transforma intr-o lista de faze executabile.

Reguli de folosire:

- lucreaza in slice-uri mici, cu test sau smoke test dupa fiecare schimbare reala;
- cand o faza trece in cod, actualizeaza documentul de specialitate si `implementat-deja.md`;
- nu sari la faze AI/generare publica inainte ca demo-ul Paper sa fie stabil dupa restart;
- pentru date persistente, prefera audit, dry-run, backup si repair controlat.

Legenda status:

| Status | Sens |
|---|---|
| `ACUM` | intra in urmatoarele sesiuni de lucru |
| `URMATOR` | imediat dupa stabilizarea fazelor ACUM |
| `DUPA DEMO MATUR` | util dupa first playable stabil |
| `AMANAT` | nu bloca demo-ul cu aceasta faza |

## A. Operare, Audit si Release Gates

1. F001 - `ACUM` - Ruleaza smoke Paper complet pentru startup, audit, debugdump si shutdown curat.
2. F002 - `ACUM` - Documenteaza rezultatul smoke Paper in `debugging-si-testare.md`.
3. F003 - `ACUM` - Adauga checklist scurt pentru restart dupa update de jar.
4. F004 - `ACUM` - Extinde `/ainpc audit db` cu sumar clar pentru tabelele noi.
5. F005 - `ACUM` - Extinde `/ainpc debugdump all` cu index sumar al fisierelor generate.
6. F006 - `ACUM` - Marcheaza in log fiecare repair aplicat cu motiv si numar de randuri afectate.
7. F007 - `ACUM` - Adauga comanda read-only pentru status rapid de sanatate server AINPC.
8. F008 - `URMATOR` - Creeaza smoke script pentru `world demo -> settlement spawn -> audit -> restart`.
9. F009 - `URMATOR` - Creeaza smoke script pentru quest accept/progress/complete/reload.
10. F010 - `URMATOR` - Adauga raport de versiune schema DB in debugdump.
11. F011 - `URMATOR` - Adauga raport de tabele lipsa sau indexuri lipsa.
12. F012 - `URMATOR` - Adauga verificare de configuratie pentru productie vs test.
13. F013 - `URMATOR` - Documenteaza procedura backup inainte de `/ainpc repair`.
14. F014 - `URMATOR` - Adauga comanda pentru export compact al starii NPC/mapping/quest.
15. F015 - `URMATOR` - Adauga `release-checklist` cu pas explicit pentru duplicate NPC.
16. F016 - `URMATOR` - Creeaza raport de regresii cunoscute dupa fiecare sesiune mare.
17. F017 - `DUPA DEMO MATUR` - Introdu gate de release bazat pe audit fara erori critice.
18. F018 - `DUPA DEMO MATUR` - Introdu gate de release bazat pe Paper smoke automat.
19. F019 - `DUPA DEMO MATUR` - Documenteaza procedura rollback jar plus DB.
20. F020 - `DUPA DEMO MATUR` - Adauga verificare ca debugdump nu include secrete.
21. F021 - `DUPA DEMO MATUR` - Creeaza raport admin pentru performanta tick-urilor AINPC.
22. F022 - `DUPA DEMO MATUR` - Adauga raport de compatibilitate Paper/Java.
23. F023 - `DUPA DEMO MATUR` - Stabileste format de changelog pe categorii.
24. F024 - `AMANAT` - Automatizeaza release complet cu artefacte si checksum.
25. F025 - `AMANAT` - Dashboard extern pentru operare server.

## B. NPC Identity, Duplicate si Lifecycle

26. F026 - `ACUM` - Smoke real dupa restart pentru `npc_source_keys`.
27. F027 - `ACUM` - Verifica vizual ca `npc_database_id` ramane pe entitate dupa reload.
28. F028 - `ACUM` - Adauga audit pentru entitati AINPC fara `npc_uuid`.
29. F029 - `ACUM` - Adauga audit pentru entitati AINPC cu `npc_name` diferit de DB.
30. F030 - `ACUM` - Adauga repair dry-run pentru marker PDC lipsa pe entitate valid atasata.
31. F031 - `ACUM` - Adauga repair apply pentru rescriere marker PDC pe entitati canonice.
32. F032 - `URMATOR` - Adauga raport de entitati AINPC in chunk-uri incarcate, grupate dupa lume.
33. F033 - `URMATOR` - Verifica duplicate dupa `source_key` si distanta in acelasi raport.
34. F034 - `URMATOR` - Persistenta imediata cand un NPC este reatasat la entitate existenta.
35. F035 - `URMATOR` - Audit pentru NPC DB `spawned=true` dar fara entitate live in chunk incarcat.
36. F036 - `URMATOR` - Audit pentru NPC DB `spawned=false` dar cu entitate live marcata.
37. F037 - `URMATOR` - Repair pentru alinierea `spawn_state.spawned` cu realitatea live.
38. F038 - `URMATOR` - Comanda read-only pentru inspectare NPC dupa DB id.
39. F039 - `URMATOR` - Comanda read-only pentru inspectare entitate dupa UUID.
40. F040 - `URMATOR` - Log rate-limit pentru mesaje repetate de duplicate.
41. F041 - `URMATOR` - Test local pentru alegerea canonica dupa source key.
42. F042 - `URMATOR` - Test local pentru stergere duplicate name+location.
43. F043 - `DUPA DEMO MATUR` - Migrare asistata pentru NPC legacy fara source key.
44. F044 - `DUPA DEMO MATUR` - Backfill `source_key` pentru NPC creat manual, daca are binding clar.
45. F045 - `DUPA DEMO MATUR` - Alerta admin pentru UUID DB care nu mai exista in lume dupa N restarturi.
46. F046 - `DUPA DEMO MATUR` - Separare clara intre NPC permanent si NPC temporar.
47. F047 - `DUPA DEMO MATUR` - Policy pentru moarte permanenta vs respawn controlat.
48. F048 - `DUPA DEMO MATUR` - Cleanup periodic pentru NPC inactiv fara binding si fara quest.
49. F049 - `AMANAT` - Identitate cross-server pentru retele multi-world.
50. F050 - `AMANAT` - Sincronizare identitate NPC intre mai multe servere.

## C. Spawn, Household si Settlement Persistence

51. F051 - `ACUM` - Smoke Paper pentru `spawn_batches` dupa settlement spawn.
52. F052 - `ACUM` - Smoke Paper pentru `households` si `household_residents` dupa spawn.
53. F053 - `IMPLEMENTAT INITIAL` - Adauga comanda read-only `/ainpc world household status`.
54. F054 - `IMPLEMENTAT INITIAL` - Adauga listare household dupa place id.
55. F055 - `IMPLEMENTAT INITIAL` - Adauga listare resident dupa NPC id.
56. F056 - `IMPLEMENTAT INITIAL` - Audit pentru household fara `home_place_id`.
57. F057 - `IMPLEMENTAT INITIAL` - Audit pentru household cu `home_place_id` care nu exista in mapping.
58. F058 - `IMPLEMENTAT INITIAL` - Audit pentru resident cu `home_node_id` care nu exista.
59. F059 - `IMPLEMENTAT INITIAL` - Audit pentru resident cu `work_place_id` care nu exista.
60. F060 - `IMPLEMENTAT INITIAL` - Backfill household din `npc_world_bindings.family_id`.
61. F061 - `IMPLEMENTAT INITIAL` - Backfill household resident din `metadata.resident_npc_ids`.
62. F062 - `IMPLEMENTAT INITIAL` - Export `households.json` cu summary pe family/place.
63. F063 - `IMPLEMENTAT INITIAL` - Repair dry-run pentru resident mutat gresit in doua case.
64. F064 - `IMPLEMENTAT INITIAL` - Repair apply pentru un singur household canonic pe NPC.
65. F065 - `IMPLEMENTAT INITIAL` - Persistenta batch pentru spawn household singular, nu doar settlement.
66. F066 - `IMPLEMENTAT INITIAL` - `spawn_batches` pentru dry-run optional, marcat separat.
67. F067 - `IMPLEMENTAT INITIAL` - Legatura explicita intre `spawn_batch_steps` si `household_id`.
68. F068 - `IMPLEMENTAT INITIAL` - Rollback bazat pe batch step, nu doar lista in memorie.
69. F069 - `INCEPUT IN COD` - Tranzactie compensata pentru spawn + DB + mapping; rollback manual, listare batch, inspectie pasi, audit `rollback_pending_steps`, `mark-failed` manual si blocare retry peste batch RUNNING cu pasi creatori implementate initial.
70. F070 - `DUPA DEMO MATUR` - Plan de migrare pentru sate create inainte de household persistence.
71. F071 - `DUPA DEMO MATUR` - Suport pentru mutarea controlata a household-ului in alta casa.
72. F072 - `DUPA DEMO MATUR` - Istoric resident: fost household, data mutarii, motiv.
73. F073 - `DUPA DEMO MATUR` - Capacitate dinamica a casei in functie de paturi/noduri.
74. F074 - `AMANAT` - Household-uri non-familiale: breasla, han, tabara.
75. F075 - `AMANAT` - Migrare household intre regiuni.

## D. Mapping, World Admin si Semantic Index

76. F076 - `ACUM` - Smoke Paper pentru `/ainpc world demo create` si `/ainpc world save`.
77. F077 - `ACUM` - Audit pentru regiune demo fara places sau nodes.
78. F078 - `ACUM` - Audit pentru case fara `max_residents`.
79. F079 - `ACUM` - Audit pentru places fara `role`.
80. F080 - `ACUM` - Audit pentru nodes fara `type`.
81. F081 - `URMATOR` - Comanda read-only pentru summary pe regiune.
82. F082 - `URMATOR` - GUI world cu lista places filtrabila.
83. F083 - `URMATOR` - GUI world cu noduri pentru place selectat.
84. F084 - `URMATOR` - Validare Y variance pentru demo settlement.
85. F085 - `URMATOR` - Validare apa/lava la spawn nodes.
86. F086 - `URMATOR` - Validare obstacole in jurul `npc_spawn`.
87. F087 - `URMATOR` - Validare distanta minima intre case demo.
88. F088 - `URMATOR` - Validare piata centrala cu spatiu suficient.
89. F089 - `URMATOR` - Salvare versiune mapping in config.
90. F090 - `URMATOR` - Diff mapping in debugdump dupa save.
91. F091 - `DUPA DEMO MATUR` - Import mapping din fisiere externe.
92. F092 - `DUPA DEMO MATUR` - Export mapping compact pentru addon demo.
93. F093 - `DUPA DEMO MATUR` - Editor command pentru node add/move/delete.
94. F094 - `DUPA DEMO MATUR` - Editor command pentru place resize.
95. F095 - `DUPA DEMO MATUR` - Wand simplu pentru setare corners.
96. F096 - `DUPA DEMO MATUR` - Validare semantica pentru template-uri cladiri.
97. F097 - `DUPA DEMO MATUR` - Reindex semantic incremental dupa modificare.
98. F098 - `AMANAT` - Auto-detectie avansata pentru cladiri custom.
99. F099 - `AMANAT` - Integrare WorldEdit pentru preview.
100. F100 - `AMANAT` - Editor vizual extern pentru mapping.

## E. Quest si Progression Runtime

101. F101 - `ACUM` - Smoke Paper pentru Q01-Q08 dupa reload.
102. F102 - `ACUM` - Smoke Paper pentru C01/C02 contracte.
103. F103 - `ACUM` - Smoke Paper pentru D01 duty.
104. F104 - `ACUM` - Smoke Paper pentru B01/B02 bounty.
105. F105 - `ACUM` - Smoke Paper pentru E01 event.
106. F106 - `ACUM` - Smoke Paper pentru T01 tutorial.
107. F107 - `ACUM` - Smoke Paper pentru R01 ritual.
108. F108 - `URMATOR` - Design si schema pentru player onboarding, profil first join si starter kit.
109. F109 - `URMATOR` - Audit pentru stage fara `next_stage` valid.
110. F110 - `URMATOR` - Audit pentru reward care nu are validator.
111. F111 - `URMATOR` - Persistenta clara pentru tracked progression.
112. F112 - `URMATOR` - Comanda read-only pentru progression dupa player.
113. F113 - `URMATOR` - Comanda read-only pentru progression dupa template.
114. F114 - `URMATOR` - Debugdump progression cu anchor bindings incluse.
115. F115 - `URMATOR` - Unificare selector quest/contract/duty/bounty.
116. F116 - `URMATOR` - API intern pentru `completeObjective`.
117. F117 - `URMATOR` - API intern pentru `failObjective`.
118. F118 - `URMATOR` - Cooldown pentru repeatable quest.
119. F119 - `URMATOR` - Expiry pentru quest oferit dar neacceptat.
120. F120 - `DUPA DEMO MATUR` - Branching simplu pe doua alegeri.
121. F121 - `DUPA DEMO MATUR` - Conditions pe story state.
122. F122 - `DUPA DEMO MATUR` - Conditions pe simulation signals.
123. F123 - `DUPA DEMO MATUR` - Reward reputation controlat.
124. F124 - `AMANAT` - Quest editor complet in GUI.
125. F125 - `AMANAT` - DSL avansat de quest cu scripturi.

## F. GUI si UX Player/Admin

126. F126 - `ACUM` - Verifica GUI quest log pe server real.
127. F127 - `ACUM` - Verifica GUI world hub pe server real.
128. F128 - `ACUM` - Verifica GUI NPC interaction pe server real.
129. F129 - `ACUM` - Adauga status vizibil pentru NPC fara quest disponibil.
130. F130 - `ACUM` - Adauga buton routine status in NPC GUI.
131. F131 - `URMATOR` - GUI pentru household status.
132. F132 - `URMATOR` - GUI pentru residentii unei case.
133. F133 - `URMATOR` - GUI pentru batch spawn history.
134. F134 - `URMATOR` - GUI pentru audit summary.
135. F135 - `URMATOR` - GUI pentru debugdump quick create.
136. F136 - `URMATOR` - Filtre in quest log dupa kind.
137. F137 - `URMATOR` - Filtre in quest log dupa status.
138. F138 - `URMATOR` - Indicator tracked quest in main hub.
139. F139 - `URMATOR` - Indicator next objective in actionbar.
140. F140 - `URMATOR` - Feedback clar cand accept quest esueaza.
141. F141 - `URMATOR` - Feedback clar cand deliver item esueaza.
142. F142 - `DUPA DEMO MATUR` - GUI pentru story state pe regiune.
143. F143 - `DUPA DEMO MATUR` - GUI pentru place story state.
144. F144 - `DUPA DEMO MATUR` - GUI pentru NPC memories sumar.
145. F145 - `DUPA DEMO MATUR` - GUI pentru familie si relatii.
146. F146 - `DUPA DEMO MATUR` - GUI pentru schedule zilnic.
147. F147 - `DUPA DEMO MATUR` - GUI admin pentru repair dry-run.
148. F148 - `DUPA DEMO MATUR` - GUI admin pentru confirm repair apply.
149. F149 - `AMANAT` - GUI editor complet pentru mapping.
150. F150 - `AMANAT` - GUI editor complet pentru questuri.

## G. Routine, Simulation si Economie

151. F151 - `ACUM` - Smoke Paper pentru rutina cu AI si gravity active.
152. F152 - `ACUM` - Verifica pathfinding natural intre home/work/social.
153. F153 - `ACUM` - Verifica teleport fallback numai cand pathing esueaza.
154. F154 - `URMATOR` - Audit pentru NPC fara home anchor.
155. F155 - `URMATOR` - Audit pentru NPC fara work anchor cand ocupatia o cere.
156. F156 - `URMATOR` - Audit pentru NPC fara social anchor.
157. F157 - `URMATOR` - Debugdump routine snapshot per NPC.
158. F158 - `URMATOR` - Comanda pentru tick manual pe un NPC.
159. F159 - `URMATOR` - Cooldown per activitate rutina.
160. F160 - `URMATOR` - Prioritate intre quest interaction si routine movement.
161. F161 - `URMATOR` - Prioritate intre conversation session si routine movement.
162. F162 - `URMATOR` - Simulare needs read-only in GUI.
163. F163 - `URMATOR` - Persistenta compacta pentru simulation snapshot.
164. F164 - `DUPA DEMO MATUR` - Extractie `SimulationService` fara schimbare de gameplay.
165. F165 - `DUPA DEMO MATUR` - Signals pentru hunger/energy/social.
166. F166 - `DUPA DEMO MATUR` - Signals pentru settlement comfort/safety.
167. F167 - `DUPA DEMO MATUR` - Economy draft pentru munca si consum.
168. F168 - `DUPA DEMO MATUR` - Rutina speciala pentru evenimente.
169. F169 - `DUPA DEMO MATUR` - Rutina speciala pentru vreme/noapte.
170. F170 - `DUPA DEMO MATUR` - Rutina family gathering.
171. F171 - `DUPA DEMO MATUR` - Rutina market day.
172. F172 - `AMANAT` - Economie completa pe resurse.
173. F173 - `AMANAT` - Simulare productie pe cladiri.
174. F174 - `AMANAT` - Simulare migratie populatie.
175. F175 - `AMANAT` - Simulare conflict intre asezari.

## H. Story, Dialog si AI Orchestration

176. F176 - `ACUM` - Smoke Paper pentru story events generate de quest.
177. F177 - `ACUM` - Debugdump story state dupa quest complete.
178. F178 - `URMATOR` - Audit pentru story event cu scope invalid.
179. F179 - `URMATOR` - Audit pentru place story state fara place valid.
180. F180 - `URMATOR` - Audit pentru region story state fara region valid.
181. F181 - `URMATOR` - Context story compact pentru NPC interaction.
182. F182 - `URMATOR` - Context story compact pentru quest explanation.
183. F183 - `URMATOR` - Dialog fallback determinist cand AI lipseste.
184. F184 - `URMATOR` - Log clar pentru AI fallback.
185. F185 - `URMATOR` - Rate limit pentru requesturi AI.
186. F186 - `URMATOR` - Redactare secrete in debug AI.
187. F187 - `DUPA DEMO MATUR` - AI draft pentru replica NPC, cu validator.
188. F188 - `DUPA DEMO MATUR` - AI draft pentru quest flavor text, cu validator.
189. F189 - `DUPA DEMO MATUR` - AI draft pentru story event, fara commit automat.
190. F190 - `DUPA DEMO MATUR` - AI explain audit read-only.
191. F191 - `DUPA DEMO MATUR` - AI propose mapping annotation, dry-run only.
192. F192 - `DUPA DEMO MATUR` - AI propose resident backstory, validator local.
193. F193 - `DUPA DEMO MATUR` - Memory summary generat si validat.
194. F194 - `DUPA DEMO MATUR` - Conversatie privata persistenta pe sesiune.
195. F195 - `DUPA DEMO MATUR` - Reactii NPC pe reputatie/story.
196. F196 - `AMANAT` - Tool calls AI cu confirmare admin.
197. F197 - `AMANAT` - AI authoring multi-step pentru questuri.
198. F198 - `AMANAT` - AI planner pentru settlement.
199. F199 - `AMANAT` - AI builder pentru structuri validate.
200. F200 - `AMANAT` - Memorie AI cross-session autonoma.

## I. API, Addonuri si Modularizare

201. F201 - `URMATOR` - Inventariaza clasele interne folosite ca API accidental.
202. F202 - `URMATOR` - Documenteaza contractul minim pentru addon quest pack.
203. F203 - `URMATOR` - Stabilizeaza modelul de capabilities pentru addon.
204. F204 - `URMATOR` - Stabilizeaza validarea dependencies pentru addon.
205. F205 - `URMATOR` - Test pentru addon medieval load.
206. F206 - `URMATOR` - Test pentru addon medieval quest definitions.
207. F207 - `URMATOR` - Separare API public vs core intern in docs.
208. F208 - `URMATOR` - Export DTO pentru NPC snapshot read-only.
209. F209 - `URMATOR` - Export DTO pentru mapping snapshot read-only.
210. F210 - `URMATOR` - Export DTO pentru progression snapshot read-only.
211. F211 - `DUPA DEMO MATUR` - Modul separat pentru quest runtime generic.
212. F212 - `DUPA DEMO MATUR` - Modul separat pentru progression API.
213. F213 - `DUPA DEMO MATUR` - Modul separat pentru world mapping API.
214. F214 - `DUPA DEMO MATUR` - Modul separat pentru simulation API.
215. F215 - `DUPA DEMO MATUR` - Event bus intern pentru evenimente validate.
216. F216 - `DUPA DEMO MATUR` - Extension point pentru objective validators.
217. F217 - `DUPA DEMO MATUR` - Extension point pentru reward executors.
218. F218 - `DUPA DEMO MATUR` - Extension point pentru GUI panels.
219. F219 - `DUPA DEMO MATUR` - Contract de compatibilitate pentru addon versioning.
220. F220 - `DUPA DEMO MATUR` - Deprecation policy pentru API public.
221. F221 - `AMANAT` - Marketplace local de addonuri.
222. F222 - `AMANAT` - Hot reload complet pentru addonuri.
223. F223 - `AMANAT` - Scripting sandbox pentru addonuri.
224. F224 - `AMANAT` - API public pentru worldgen extern.
225. F225 - `AMANAT` - Compatibilitate multi-plugin avansata.

## J. Testing, Migration, Performance si Security

226. F226 - `ACUM` - Ruleaza `mvn -pl ainpc-core-plugin -am test` dupa fiecare slice.
227. F227 - `ACUM` - Ruleaza `mvn -pl ainpc-core-plugin -am package -DskipTests` dupa schimbari reale.
228. F228 - `ACUM` - Adauga teste pentru fiecare helper determinist nou.
229. F229 - `ACUM` - Adauga test pentru fiecare schema service nou.
230. F230 - `URMATOR` - Test de migrare DB in-memory pentru tabele noi.
231. F231 - `URMATOR` - Test pentru audit DB cu household orfan.
232. F232 - `URMATOR` - Test pentru audit DB cu source key stale.
233. F233 - `URMATOR` - Test pentru repair duplicate dry-run.
234. F234 - `URMATOR` - Test pentru repair duplicate apply pe DB mock.
235. F235 - `URMATOR` - Test pentru debugdump JSON minimal.
236. F236 - `URMATOR` - Benchmark simplu pentru audit pe 500 NPC.
237. F237 - `URMATOR` - Benchmark simplu pentru progression list.
238. F238 - `URMATOR` - Benchmark simplu pentru mapping semantic index.
239. F239 - `URMATOR` - Thread check pentru operatii Bukkit main-thread.
240. F240 - `URMATOR` - Thread check pentru operatii DB async.
241. F241 - `DUPA DEMO MATUR` - Test Paper automat cu server temporar.
242. F242 - `DUPA DEMO MATUR` - Test Paper restart cu DB persistenta.
243. F243 - `DUPA DEMO MATUR` - Test Paper chunk unload/load.
244. F244 - `DUPA DEMO MATUR` - Test Paper player quest cap-coada.
245. F245 - `DUPA DEMO MATUR` - Test Paper settlement spawn cap-coada.
246. F246 - `DUPA DEMO MATUR` - Security audit pentru comenzi admin.
247. F247 - `DUPA DEMO MATUR` - Security audit pentru fisiere generate.
248. F248 - `DUPA DEMO MATUR` - Security audit pentru input din YAML.
249. F249 - `AMANAT` - Fuzzing pentru parser quest YAML.
250. F250 - `AMANAT` - Fuzzing pentru comenzi admin complexe.
