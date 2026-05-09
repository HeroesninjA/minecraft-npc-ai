# Urmatoarele 250 Faze - Partea 2

Actualizat: 2026-05-09

## Scop

Acest document continua backlog-ul operational din `faze-urmatoare-250.md`.

Partea 2 se concentreaza pe:

- generare de sate din config;
- generare de cladiri din template-uri configurabile;
- cresterea contextului folosit de NPC, questuri, story si AI;
- comportament NPC mai realist;
- documentare API si stabilizare contracte publice.

Reguli:

- fiecare faza trebuie facuta incremental, cu audit/debugdump sau smoke test cand schimba runtime-ul;
- config-ul este sursa de intentie, nu executa direct schimbari in lume fara validare;
- AI poate propune drafturi, dar validarea si commit-ul raman locale;
- API-ul public trebuie documentat inainte sa fie folosit de addonuri externe.

## A. Config pentru Sate si Regiuni

1. F251 - `ACUM` - Defineste schema `settlements.yml` pentru sate generate din config.
2. F252 - `ACUM` - Documenteaza campurile minime pentru regiune: id, lume, centru, raza, profil.
3. F253 - `ACUM` - Adauga config pentru densitate case, piata, drumuri si zone de lucru.
4. F254 - `ACUM` - Adauga validator read-only pentru `settlements.yml`.
5. F255 - `ACUM` - Adauga audit pentru settlement config lipsa sau invalid.
6. F256 - `URMATOR` - Creeaza `SettlementConfigLoader` cu erori explicite.
7. F257 - `URMATOR` - Creeaza DTO `SettlementDefinition`.
8. F258 - `URMATOR` - Creeaza DTO `SettlementLayoutProfile`.
9. F259 - `URMATOR` - Creeaza profiluri preset: compact, spacious, rural, fortified.
10. F260 - `URMATOR` - Adauga comanda `/ainpc settlement definitions`.
11. F261 - `URMATOR` - Adauga comanda `/ainpc settlement validate <id>`.
12. F262 - `URMATOR` - Adauga debugdump `settlement-config.json`.
13. F263 - `URMATOR` - Leaga settlement config de WorldAdminService fara commit automat.
14. F264 - `URMATOR` - Adauga warning pentru config care suprascrie regiuni existente.
15. F265 - `URMATOR` - Adauga dry-run pentru regiune generata din config.
16. F266 - `URMATOR` - Adauga preview text pentru numar case, NPC si work places.
17. F267 - `DUPA DEMO MATUR` - Adauga suport pentru mai multe sate intr-un fisier.
18. F268 - `DUPA DEMO MATUR` - Adauga inheritance intre profiluri de settlement.
19. F269 - `DUPA DEMO MATUR` - Adauga config pentru tematica: medieval, modern, rural, magic.
20. F270 - `DUPA DEMO MATUR` - Adauga config pentru stil economic al satului.
21. F271 - `DUPA DEMO MATUR` - Adauga config pentru risc si pericole locale.
22. F272 - `DUPA DEMO MATUR` - Adauga config pentru legaturi intre sate.
23. F273 - `AMANAT` - Generare multi-biome adaptiva.
24. F274 - `AMANAT` - Import config din tool extern.
25. F275 - `AMANAT` - Editor vizual pentru settlement config.

## B. Template-uri de Cladiri din Config

26. F276 - `ACUM` - Defineste schema `building_templates.yml`.
27. F277 - `ACUM` - Documenteaza template minim: id, type, footprint, anchors, blocks.
28. F278 - `ACUM` - Adauga validator pentru dimensiuni minime si maxime.
29. F279 - `ACUM` - Adauga validator pentru marker nodes obligatorii.
30. F280 - `URMATOR` - Creeaza DTO `BuildingTemplateDefinition`.
31. F281 - `URMATOR` - Creeaza DTO `BuildingAnchorDefinition`.
32. F282 - `URMATOR` - Creeaza DTO `BuildingVariantDefinition`.
33. F283 - `URMATOR` - Adauga comanda `/ainpc building templates`.
34. F284 - `URMATOR` - Adauga comanda `/ainpc building validate <id>`.
35. F285 - `URMATOR` - Adauga debugdump `building-templates.json`.
36. F286 - `URMATOR` - Adauga template pentru casa mica medievala.
37. F287 - `URMATOR` - Adauga template pentru casa familie.
38. F288 - `URMATOR` - Adauga template pentru fierarie.
39. F289 - `URMATOR` - Adauga template pentru ferma.
40. F290 - `URMATOR` - Adauga template pentru taverna.
41. F291 - `URMATOR` - Adauga template pentru piata/quest board.
42. F292 - `URMATOR` - Adauga template pentru altar/ritual point.
43. F293 - `DUPA DEMO MATUR` - Adauga variante pe material si biome.
44. F294 - `DUPA DEMO MATUR` - Adauga conditii de plasare pe panta.
45. F295 - `DUPA DEMO MATUR` - Adauga preview bounding box in lume.
46. F296 - `DUPA DEMO MATUR` - Adauga generare marker nodes din template.
47. F297 - `DUPA DEMO MATUR` - Adauga transformari rotate/mirror validate.
48. F298 - `DUPA DEMO MATUR` - Adauga suport optional WorldEdit schematic.
49. F299 - `AMANAT` - Template-uri parametrice pe mai multe etaje.
50. F300 - `AMANAT` - Editor in-game pentru template block palette.

## C. Pipeline Generare Sat si Cladiri

51. F301 - `ACUM` - Defineste pipeline dry-run: config -> plan -> validate -> preview -> commit.
52. F302 - `ACUM` - Creeaza `SettlementGenerationPlan` fara modificari in lume.
53. F303 - `ACUM` - Adauga `BuildingPlacementPlan`.
54. F304 - `ACUM` - Adauga `RoadPlacementPlan`.
55. F305 - `URMATOR` - Adauga `TerrainSuitabilityReport`.
56. F306 - `URMATOR` - Verifica teren plat pentru piata.
57. F307 - `URMATOR` - Verifica spatiu pentru case.
58. F308 - `URMATOR` - Verifica apa/lava/void in footprint.
59. F309 - `URMATOR` - Verifica intersectii intre cladiri planificate.
60. F310 - `URMATOR` - Verifica distanta minima intre intrari.
61. F311 - `URMATOR` - Verifica drumuri intre piata, case si workplace.
62. F312 - `URMATOR` - Adauga comanda `/ainpc settlement plan <id>`.
63. F313 - `URMATOR` - Adauga comanda `/ainpc settlement preview <id>`.
64. F314 - `URMATOR` - Adauga comanda `/ainpc settlement commit <id> confirm`.
65. F315 - `URMATOR` - Scrie `generation_batches` pentru commit-uri.
66. F316 - `URMATOR` - Leaga batch-ul de `spawn_batches`.
67. F317 - `URMATOR` - Leaga cladirile generate de world mapping.
68. F318 - `URMATOR` - Creeaza nodes pentru paturi, usi, workstation, social.
69. F319 - `URMATOR` - Creeaza places pentru case si workplace.
70. F320 - `URMATOR` - Salveaza mapping dupa commit controlat.
71. F321 - `DUPA DEMO MATUR` - Rollback pentru batch de cladiri.
72. F322 - `DUPA DEMO MATUR` - Repair pentru cladire partial generata.
73. F323 - `DUPA DEMO MATUR` - Patch planner pentru completarea unui sat existent.
74. F324 - `AMANAT` - Generare drumuri cu pathfinding pe teren real.
75. F325 - `AMANAT` - Generare decor si vegetatie contextuala.

## D. Context Extins pentru Player, NPC si Lume

76. F326 - `ACUM` - Defineste `ContextSnapshot` comun pentru dialog, quest si GUI.
77. F327 - `ACUM` - Adauga context player: profil, onboarding, progres tracked.
78. F328 - `ACUM` - Adauga context NPC: rutina, familie, household, rol.
79. F329 - `ACUM` - Adauga context place: tip, tag-uri, story state.
80. F330 - `ACUM` - Adauga context region: settlement, pericole, story flags.
81. F331 - `URMATOR` - Creeaza `ContextService` read-only.
82. F332 - `URMATOR` - Creeaza snapshot scurt pentru prompt AI.
83. F333 - `URMATOR` - Creeaza snapshot complet pentru debugdump.
84. F334 - `URMATOR` - Adauga limite de dimensiune pentru context AI.
85. F335 - `URMATOR` - Adauga redactare pentru date sensibile player.
86. F336 - `URMATOR` - Adauga context vreme/timp/biome.
87. F337 - `URMATOR` - Adauga context evenimente recente.
88. F338 - `URMATOR` - Adauga context quest anchors locale.
89. F339 - `URMATOR` - Adauga context economy local.
90. F340 - `URMATOR` - Adauga context safety si comfort settlement.
91. F341 - `DUPA DEMO MATUR` - Context memory summary pentru NPC.
92. F342 - `DUPA DEMO MATUR` - Context relationship summary pentru player.
93. F343 - `DUPA DEMO MATUR` - Context reputation regional.
94. F344 - `DUPA DEMO MATUR` - Context conflict si evenimente active.
95. F345 - `DUPA DEMO MATUR` - Context pentru generare quest draft.
96. F346 - `DUPA DEMO MATUR` - Context pentru generare building suggestions.
97. F347 - `DUPA DEMO MATUR` - Cache context cu invalidare pe event.
98. F348 - `AMANAT` - Context cross-server.
99. F349 - `AMANAT` - Context vectorizat pentru cautare semantica.
100. F350 - `AMANAT` - Context AI multi-agent.

## E. Comportament NPC Mai Realist

101. F351 - `ACUM` - Verifica AI si gravity pentru NPC dupa restart.
102. F352 - `ACUM` - Ajusteaza routine cooldown ca miscarea sa para naturala.
103. F353 - `ACUM` - Adauga pauze scurte intre schimbari de target.
104. F354 - `ACUM` - Adauga look-at natural catre player in conversatie.
105. F355 - `URMATOR` - Adauga idle behavior langa home/work/social.
106. F356 - `URMATOR` - Adauga wandering limitat in jurul ancorei curente.
107. F357 - `URMATOR` - Adauga evitarea teleportului cand playerul priveste NPC-ul.
108. F358 - `URMATOR` - Adauga motiv vizibil pentru teleport fallback.
109. F359 - `URMATOR` - Adauga reactie la vreme: cauta adapost la ploaie.
110. F360 - `URMATOR` - Adauga reactie la noapte: merge acasa.
111. F361 - `URMATOR` - Adauga reactie la pericol: se retrage spre safe node.
112. F362 - `URMATOR` - Adauga reactie la social event: merge in piata.
113. F363 - `URMATOR` - Adauga rutine diferite pe ocupatii.
114. F364 - `URMATOR` - Adauga rutina pentru fermier.
115. F365 - `URMATOR` - Adauga rutina pentru fierar.
116. F366 - `URMATOR` - Adauga rutina pentru negustor.
117. F367 - `URMATOR` - Adauga rutina pentru paznic.
118. F368 - `DUPA DEMO MATUR` - Adauga conversatii NPC-NPC ca eveniment scurt.
119. F369 - `DUPA DEMO MATUR` - Adauga vizite intre membri household.
120. F370 - `DUPA DEMO MATUR` - Adauga emotii care influenteaza rutina.
121. F371 - `DUPA DEMO MATUR` - Adauga oboseala si somn.
122. F372 - `DUPA DEMO MATUR` - Adauga foame si cautare mancare.
123. F373 - `DUPA DEMO MATUR` - Adauga social need si intalniri.
124. F374 - `AMANAT` - Comportament emergent pe economie completa.
125. F375 - `AMANAT` - Planificare autonoma multi-zi.

## F. Config pentru Comportament si Rutine

126. F376 - `ACUM` - Defineste `behavior_profiles.yml`.
127. F377 - `ACUM` - Documenteaza campuri: id, occupation, schedule, movement, reactions.
128. F378 - `URMATOR` - Creeaza loader pentru behavior profiles.
129. F379 - `URMATOR` - Adauga validator pentru schedule invalid.
130. F380 - `URMATOR` - Adauga validator pentru target anchors inexistente.
131. F381 - `URMATOR` - Leaga `behavior_profile` de occupation.
132. F382 - `URMATOR` - Leaga `behavior_profile` de NPC profile.
133. F383 - `URMATOR` - Adauga profile default pentru villager.
134. F384 - `URMATOR` - Adauga profile default pentru worker.
135. F385 - `URMATOR` - Adauga profile default pentru guard.
136. F386 - `URMATOR` - Adauga profile default pentru trader.
137. F387 - `URMATOR` - Adauga profile default pentru child/young NPC viitor.
138. F388 - `URMATOR` - Adauga comanda `/ainpc routine profiles`.
139. F389 - `URMATOR` - Adauga debugdump `behavior-profiles.json`.
140. F390 - `DUPA DEMO MATUR` - Hot reload pentru behavior profiles.
141. F391 - `DUPA DEMO MATUR` - GUI admin pentru inspectare behavior profile.
142. F392 - `DUPA DEMO MATUR` - Override per NPC pentru movement speed.
143. F393 - `DUPA DEMO MATUR` - Override per region pentru program zilnic.
144. F394 - `DUPA DEMO MATUR` - Override pe story state.
145. F395 - `DUPA DEMO MATUR` - Override pe weather.
146. F396 - `DUPA DEMO MATUR` - Behavior profile pentru evenimente.
147. F397 - `DUPA DEMO MATUR` - Behavior profile pentru festival.
148. F398 - `AMANAT` - Behavior profile generat AI si validat.
149. F399 - `AMANAT` - Behavior profile editor vizual.
150. F400 - `AMANAT` - Behavior trees configurabile complet.

## G. API Public si Documentare

151. F401 - `ACUM` - Inventariaza API public existent in `ainpc-api`.
152. F402 - `ACUM` - Documenteaza ce este stabil si ce este intern.
153. F403 - `ACUM` - Creeaza `documentatie-api-v2.md`.
154. F404 - `URMATOR` - Documenteaza `AINPCPlatformApi`.
155. F405 - `URMATOR` - Documenteaza `WorldAdminApi`.
156. F406 - `URMATOR` - Documenteaza snapshot-uri NPC read-only.
157. F407 - `URMATOR` - Documenteaza snapshot-uri progression read-only.
158. F408 - `URMATOR` - Documenteaza evenimente publice recomandate.
159. F409 - `URMATOR` - Documenteaza extension points pentru quest objectives.
160. F410 - `URMATOR` - Documenteaza extension points pentru rewards.
161. F411 - `URMATOR` - Documenteaza extension points pentru GUI panels.
162. F412 - `URMATOR` - Documenteaza contract pentru addon pack YAML.
163. F413 - `URMATOR` - Documenteaza lifecycle addon: load, validate, enable, disable.
164. F414 - `URMATOR` - Documenteaza compatibilitate versiuni.
165. F415 - `URMATOR` - Adauga exemple de addon minimal.
166. F416 - `URMATOR` - Adauga exemple de quest addon.
167. F417 - `URMATOR` - Adauga exemple de mapping addon.
168. F418 - `DUPA DEMO MATUR` - Genereaza JavaDoc pentru API public.
169. F419 - `DUPA DEMO MATUR` - Publica contract de semver pentru API.
170. F420 - `DUPA DEMO MATUR` - Adauga teste de compatibilitate API.
171. F421 - `DUPA DEMO MATUR` - Adauga migration guide API v1 -> v2.
172. F422 - `DUPA DEMO MATUR` - Stabilizeaza DTO-uri fara dependinte Bukkit unde se poate.
173. F423 - `DUPA DEMO MATUR` - Documenteaza thread rules pentru API.
174. F424 - `AMANAT` - Portal docs generat static.
175. F425 - `AMANAT` - SDK extern pentru addonuri.

## H. GUI si Tooling pentru Config/Generare

176. F426 - `ACUM` - Adauga pagina GUI pentru settlement config summary.
177. F427 - `ACUM` - Adauga pagina GUI pentru building templates.
178. F428 - `URMATOR` - GUI pentru validare settlement config.
179. F429 - `URMATOR` - GUI pentru validare building template.
180. F430 - `URMATOR` - GUI pentru preview settlement plan.
181. F431 - `URMATOR` - GUI pentru preview building placement.
182. F432 - `URMATOR` - GUI pentru confirm commit settlement.
183. F433 - `URMATOR` - GUI pentru generation batch history.
184. F434 - `URMATOR` - GUI pentru rollback dry-run.
185. F435 - `URMATOR` - GUI pentru behavior profile summary.
186. F436 - `URMATOR` - GUI pentru context snapshot NPC.
187. F437 - `URMATOR` - GUI pentru context snapshot place.
188. F438 - `URMATOR` - GUI pentru API docs links/help.
189. F439 - `DUPA DEMO MATUR` - Tool in-game pentru selectare region center.
190. F440 - `DUPA DEMO MATUR` - Tool in-game pentru preview footprints.
191. F441 - `DUPA DEMO MATUR` - Tool in-game pentru marker node visualization.
192. F442 - `DUPA DEMO MATUR` - Tool in-game pentru road preview.
193. F443 - `DUPA DEMO MATUR` - Tool in-game pentru config export.
194. F444 - `DUPA DEMO MATUR` - Tool in-game pentru config import dry-run.
195. F445 - `DUPA DEMO MATUR` - Wizard pentru creare settlement config.
196. F446 - `DUPA DEMO MATUR` - Wizard pentru creare building template.
197. F447 - `AMANAT` - Editor GUI complet pentru config.
198. F448 - `AMANAT` - Harta vizuala externa pentru preview.
199. F449 - `AMANAT` - Web UI pentru configurare.
200. F450 - `AMANAT` - Live preview 3D extern.

## I. Testare, Migration si Performanta pentru Generare

201. F451 - `ACUM` - Test pentru parser `settlements.yml`.
202. F452 - `ACUM` - Test pentru parser `building_templates.yml`.
203. F453 - `ACUM` - Test pentru validator settlement.
204. F454 - `ACUM` - Test pentru validator building template.
205. F455 - `URMATOR` - Test pentru plan fara intersectii.
206. F456 - `URMATOR` - Test pentru detectie intersectii.
207. F457 - `URMATOR` - Test pentru terrain suitability.
208. F458 - `URMATOR` - Test pentru road connection plan.
209. F459 - `URMATOR` - Test pentru mapping nodes generate.
210. F460 - `URMATOR` - Test pentru batch generation DB.
211. F461 - `URMATOR` - Test pentru rollback dry-run.
212. F462 - `URMATOR` - Test pentru debugdump generation.
213. F463 - `URMATOR` - Benchmark planificare 10 cladiri.
214. F464 - `URMATOR` - Benchmark planificare 50 cladiri.
215. F465 - `URMATOR` - Benchmark context snapshot 100 NPC.
216. F466 - `URMATOR` - Benchmark behavior tick 100 NPC.
217. F467 - `DUPA DEMO MATUR` - Smoke Paper pentru commit cladire.
218. F468 - `DUPA DEMO MATUR` - Smoke Paper pentru commit sat mic.
219. F469 - `DUPA DEMO MATUR` - Smoke Paper pentru rollback partial.
220. F470 - `DUPA DEMO MATUR` - Smoke Paper pentru reload dupa generare.
221. F471 - `DUPA DEMO MATUR` - Migration pentru config vechi world demo.
222. F472 - `DUPA DEMO MATUR` - Migration pentru templates vechi.
223. F473 - `DUPA DEMO MATUR` - Profilare tick routine dupa generare sat.
224. F474 - `AMANAT` - Load test 500 NPC.
225. F475 - `AMANAT` - Load test 10 sate generate.

## J. AI Drafting Controlat si Continut Dinamic

226. F476 - `DUPA DEMO MATUR` - AI propune settlement config draft, fara commit.
227. F477 - `DUPA DEMO MATUR` - AI propune building template draft, fara commit.
228. F478 - `DUPA DEMO MATUR` - AI propune behavior profile draft, fara commit.
229. F479 - `DUPA DEMO MATUR` - Validator local pentru draft settlement AI.
230. F480 - `DUPA DEMO MATUR` - Validator local pentru draft building AI.
231. F481 - `DUPA DEMO MATUR` - Validator local pentru draft behavior AI.
232. F482 - `DUPA DEMO MATUR` - AI explica de ce un plan de generare este invalid.
233. F483 - `DUPA DEMO MATUR` - AI sugereaza reparatii pentru config invalid.
234. F484 - `DUPA DEMO MATUR` - AI sugereaza backstory pentru settlement.
235. F485 - `DUPA DEMO MATUR` - AI sugereaza roluri NPC pe baza contextului.
236. F486 - `DUPA DEMO MATUR` - AI sugereaza quest hooks pe baza cladirilor.
237. F487 - `DUPA DEMO MATUR` - AI sugereaza evenimente locale pe baza story state.
238. F488 - `DUPA DEMO MATUR` - AI rezuma contextul satului pentru admin.
239. F489 - `DUPA DEMO MATUR` - AI produce documentatie pentru config generat.
240. F490 - `DUPA DEMO MATUR` - AI produce changelog pentru settlement generation.
241. F491 - `AMANAT` - AI genereaza variante de cladiri cu confirmare.
242. F492 - `AMANAT` - AI genereaza economie locala cu validare.
243. F493 - `AMANAT` - AI genereaza lanturi de questuri pe settlement.
244. F494 - `AMANAT` - AI genereaza dialoguri contextuale pe household.
245. F495 - `AMANAT` - AI planifica evolutia satului pe sezoane.
246. F496 - `AMANAT` - AI propune extinderi de oras.
247. F497 - `AMANAT` - AI propune conflicte intre sate.
248. F498 - `AMANAT` - AI propune evenimente globale persistente.
249. F499 - `AMANAT` - AI autorizeaza tool calls doar prin confirmare admin.
250. F500 - `AMANAT` - AI co-designer complet pentru lumi, pastrat in sandbox.
