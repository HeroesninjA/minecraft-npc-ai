# Server NPC MVP si Faze Ulterioare

Actualizat: 2026-05-20

## Scop

Acest document defineste pasii necesari pentru o versiune minim functionala a serverului NPC AINPC si ordinea fazelor ulterioare.

Prin "server NPC minim functional" intelegem un server Paper local/de test pe care:

- pluginul AINPC porneste fara erori repetate;
- baza de date se initializeaza si supravietuieste restartului;
- exista mapping minim pentru o zona jucabila;
- NPC-urile pot fi create sau spawnate, legate de locuri si inspectate;
- rutina NPC este vizibila si nu produce miscare haotica;
- dialogul functioneaza cu fallback sigur cand AI-ul extern nu raspunde;
- cel putin un quest/progression poate fi oferit, acceptat, progresat si verificat;
- adminul poate rula audit, debugdump, backup si rollback operational.

MVP-ul nu inseamna toate mecanicile visate. Inseamna un loop jucabil mic, verificabil, care poate fi pornit de mai multe ori fara pierdere de date si fara cleanup manual riscant.

## Definitia De Gata Pentru MVP

MVP-ul este gata doar daca toate punctele de mai jos sunt adevarate pe un server Paper curat:

| Zona | Criteriu minim |
|---|---|
| Build | JAR-urile core si addon demo se construiesc si se copiaza in `plugins/` |
| Startup | `/plugins` arata AINPC incarcat, fara stacktrace repetat |
| Config | `plugins/AINPCPlugin/config.yml`, `quests.yml` si `ainpc_data.db` exista |
| Audit | `/ainpc audit`, `/ainpc audit db`, `/ainpc audit world`, `/ainpc audit npc`, `/ainpc audit quest` nu raporteaza erori critice |
| Mapping | exista o regiune demo sau scanata cu places/nodes inspectabile |
| NPC | minim 3 NPC-uri exista, au profil, home/work/social unde este posibil si apar in `/ainpc list` |
| Rutina | `/ainpc routine status <npc|nearest>` arata stare coerenta si nu forteaza teleport haotic |
| Dialog | click dreapta sau chat porneste conversatie si are fallback controlat |
| Quest/progression | un quest sau tutorial poate fi acceptat, urmarit si inspectat |
| Persistenta | dupa restart, NPC-urile, mapping-ul, legaturile si progresul minim raman disponibile |
| Debug | `/ainpc debugdump all` produce artefacte utile fara secrete expuse |
| Rollback | exista backup pentru `plugins/AINPCPlugin/` si JAR-urile folosite |

## Non-Obiective Pentru MVP

Nu bloca MVP-ul pe aceste lucruri:

- economie completa;
- reputatie/factiuni avansate;
- generare completa de sate;
- runtime generic perfect pentru toate scenariile;
- WorldEdit obligatoriu;
- AI care modifica direct lumea;
- sute de NPC-uri;
- GUI-uri finale pentru toate fluxurile;
- productie publica fara operator tehnic.

Acestea intra in fazele ulterioare.

## Faza 0: Pregatire Si Baseline

Scop: serverul poate fi construit, pornit si verificat fara sa ghicim starea.

Pasi:

1. Ruleaza build local:

```powershell
.\gradlew.bat test
.\gradlew.bat :ainpc-core-plugin:jar :ainpc-scenario-medieval:jar
```

2. Confirma artefactele:

```text
ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar
ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-1.0.0.jar
```

3. Pregateste un server Paper separat de lumea reala.

4. Copiaza JAR-urile in `plugins/`.

5. Porneste o data serverul ca sa genereze config.

6. Opreste serverul si salveaza backup initial:

```text
plugins/AINPCPlugin/
plugins/ainpc-core-plugin-1.0.0.jar
plugins/ainpc-scenario-medieval-1.0.0.jar
```

Gate:

- serverul porneste;
- AINPC apare in `/plugins`;
- configul si DB-ul sunt create;
- nu exista exceptii repetate la startup.

## Faza 1: Config Minim Si Pornire Stabilizata

Scop: pluginul ruleaza cu setari sigure, inclusiv fara cheie OpenAI.

Config minim:

```yaml
openai:
  base_url: "https://api.openai.com/v1"
  api_key: ""
  model: "gpt-5.4-nano"
  diagnostics:
    enabled: true
    check_on_startup: true

world_admin:
  enabled: true
  auto_index:
    enabled: true

simulation:
  enabled: true

routine:
  enabled: true
  teleport_enabled: true
  natural_movement:
    enabled: true

debug: false
```

Reguli:

- cheia OpenAI sta in `OPENAI_API_KEY`, nu in config commit-uit sau trimis public;
- `debug: false` pe server public;
- debug doar temporar pentru investigatii.

Comenzi de verificare:

```text
/ainpc
/ainpc audit
/ainpc audit db
/ainpc audit world
```

Gate:

- comanda `/ainpc` raspunde;
- auditul DB nu raporteaza initializare esuata;
- fallback-ul AI este clar cand providerul extern lipseste.

## Faza 2: Mapping Minim Al Lumii

Scop: NPC-urile si questurile au locuri semantice reale, nu doar coordonate brute.

Varianta rapida pentru demo:

```text
/ainpc world demo create demo_sat
/ainpc world whereami
/ainpc world places demo_sat
/ainpc audit world
/ainpc world save
```

Varianta pe sat vanilla existent:

```text
/ainpc world scan village 64
/ainpc world scan village 64 import demo_sat
/ainpc audit world
/ainpc world save
```

Dupa restart:

```text
/ainpc audit world
/ainpc world places demo_sat
```

Gate:

- exista regiune;
- exista places pentru case/work/social/piata unde este posibil;
- mapping-ul ramane dupa restart;
- auditul world nu crapa si nu raporteaza lipsuri critice.

## Faza 3: NPC Lifecycle Minim

Scop: NPC-urile pot fi create, gasite, salvate si inspectate.

Flux manual:

```text
/ainpc create TestNpc1
/ainpc create TestNpc2
/ainpc create TestNpc3
/ainpc list
/ainpc info TestNpc1
/ainpc audit npc
```

Flux cu mapping:

```text
/ainpc world bind npc TestNpc1 <homePlaceId> [workPlaceId|-] [socialPlaceId|-]
/ainpc world bindings TestNpc1
/ainpc routine status TestNpc1
/ainpc audit spawn
```

Flux household/settlement:

```text
/ainpc world household plan <homePlaceId> [count]
/ainpc world household spawn <homePlaceId> [count]
/ainpc world settlement plan demo_sat [maxHouses]
/ainpc world settlement spawn demo_sat [maxHouses]
/ainpc audit spawn
```

Gate:

- minim 3 NPC-uri exista;
- fiecare are profil si entitate valida;
- nu apar duplicate dupa restart;
- legaturile NPC -> home/work/social pot fi inspectate;
- rollback-ul settlement spawn nu lasa date partiale evidente.

## Faza 4: Rutina Si Simulare De Baza

Scop: NPC-urile par vii, dar controlat.

Comenzi:

```text
/ainpc routine status nearest
/ainpc routine tick
/ainpc audit npc
/ainpc audit spawn
```

Verificari:

- NPC-ul are home/work/social cand mapping-ul exista;
- statusul rutinei este lizibil;
- NPC-ul nu sare continuu intre ancore;
- pathfinding-ul este preferat fata de teleport cand exista drum valid;
- teleportul ramane fallback, nu comportament principal.

Gate:

- rutina poate fi inspectata de admin;
- nu apar erori repetate in consola;
- dupa restart, rutina poate continua peste aceleasi ancore.

## Faza 5: Dialog Si AI Fallback

Scop: jucatorul poate vorbi cu NPC-ul fara ca serverul sa depinda de AI extern pentru stabilitate.

Verificari:

```text
/ainpc info nearest
```

In joc:

- click dreapta pe NPC;
- trimite mesaj scurt;
- inchide conversatia cu `pa`;
- testeaza si fara `OPENAI_API_KEY`, ca fallback-ul sa fie controlat.

Gate:

- conversatia porneste;
- distanta de interactiune este respectata;
- NPC-ul raspunde sau cade pe fallback clar;
- erorile AI nu opresc serverul;
- logul nu expune cheia API.

## Faza 6: Quest Sau Progression Minim Jucabil

Scop: exista un loop simplu: oferta, acceptare, progres, status, debug.

Smoke minim:

```text
/ainpc audit quest
/ainpc quest log
/ainpc quest nearest
/ainpc quest accept nearest
/ainpc quest status nearest
/ainpc quest track start
/ainpc quest anchors
```

Pentru mecanici generice deja pregatite:

```text
/ainpc progression definitions
/ainpc progression stored <player|uuid|all>
/ainpc tutorial definitions
/ainpc duty definitions
/ainpc bounty definitions
```

Gate:

- un quest/tutorial/progression poate fi vazut;
- poate fi acceptat;
- statusul nu crapa;
- tracking-ul este inspectabil;
- debug-ul quest/progression arata variabile si ancore coerente;
- dupa restart, progresul ramane.

## Faza 7: Operare Admin, Audit Si Debugdump

Scop: cand ceva nu merge, adminul poate colecta informatii fara sa editeze DB live.

Comenzi obligatorii:

```text
/ainpc audit all
/ainpc debugdump all
```

Artefacte asteptate:

```text
plugins/AINPCPlugin/debug-dumps/debug-dump-YYYYMMDD-HHMMSS/
  audit.txt
  npcs.json
  world-mapping.json
  npc-world-bindings.json
  player-progressions.json
  player-quest-progress.json
  quest-anchor-bindings.json
  story-states.json
  story-events.json
  recent-server-log.txt
```

Gate:

- debugdump se genereaza;
- configul din dump este sanitizat;
- dump-ul contine NPC/world/quest/progression suficient pentru investigatie;
- nu se editeaza `ainpc_data.db` cat timp serverul ruleaza.

## Faza 8: Pachet MVP Si Smoke Final

Scop: MVP-ul poate fi reprodus de la zero.

Checklist final:

1. Opreste serverul.
2. Fa backup la `plugins/AINPCPlugin/`.
3. Noteaza versiunea JAR-urilor si hash-ul lor.
4. Porneste serverul.
5. Ruleaza audit complet.
6. Ruleaza mapping smoke.
7. Ruleaza NPC/routine smoke.
8. Ruleaza quest/progression smoke.
9. Ruleaza debugdump.
10. Opreste si reporneste serverul.
11. Repeta auditul de baza.

Comenzi utile:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 -ServerDir "C:\Minecraft\paper-test"
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 -ServerDir "C:\Minecraft\paper-test" -PlayerName "NumeleTau"
```

MVP-ul este acceptat cand acelasi flux trece de doua ori: inainte si dupa restart.

## Faze Ulterioare

### Faza U1: Playable Village Hardening

Scop: satul demo devine lizibil si placut de testat.

Prioritati:

- teren plat si spatiere intre case;
- case/work/social clare;
- NPC-uri fara fuga haotica;
- rutina vizibila in GUI si comenzi;
- `/ainpc patch analyze|plan|validate` pe `demo_sat`;
- smoke complet wand: `region`, `place`, `node`, `npc_bind`, `quest_anchor`.

### Faza U2: Populatie Narativa

Scop: satul poate genera familii si roluri coerente.

Prioritati:

- `PopulationPlan` dry-run;
- nume, roluri, familii si ocupatii;
- distributie pe case/work/social;
- validare inainte de spawn;
- spawn batch cu rollback si audit.

### Faza U3: Questuri Si Progression Mai Mature

Scop: progresul nu ramane doar quest clasic.

Prioritati:

- smoke Q01-Q08;
- smoke C02, D01, B01, B02, E01, T01, R01;
- validator mai strict pentru pack-uri;
- story fara quest, quest fara story si quest cu `record_story_event`;
- UX mai clar pentru track/abandon/status/debug.

### Faza U4: Runtime Extensibil Pentru Scenarii

Scop: contractele deja existente devin runtime principal gradual.

Prioritati:

- `ScenarioActionRegistry` consumat real de `ScenarioEngine`;
- `ScenarioConditionRegistry` si `ScenarioTriggerRegistry` integrate treptat;
- validare dependencies/capabilities/runtime mode pentru addonuri;
- mesaje clare la load pentru incompatibilitati;
- fallback compatibil pentru questurile existente.

### Faza U5: Story, Reputatie Si Reactii

Scop: lumea tine minte evenimente si NPC-urile reactioneaza coerent.

Prioritati:

- story context read-only mai bogat;
- reputatie simpla pe player/regiune;
- reactii NPC la evenimente;
- dialog care consuma context validat, nu toata lumea;
- audit/debugdump pentru story/reputatie.

### Faza U6: Economie Si Reward-uri Extinse

Scop: recompensele devin sistemice, nu doar iteme simple.

Prioritati:

- moneda sau credit local;
- reputatie ca reward;
- afiliere regionala;
- shop real in loc de placeholder;
- audit pentru reward-uri necunoscute sau invalide.

### Faza U7: Generare Si Patch Planning

Scop: serverul poate propune sau completa sate, fara executie oarba.

Prioritati:

- `GapReport` si `PatchPlan` mai bune;
- template-uri de cladiri cu marker nodes;
- builder optional fara WorldEdit;
- integrare WorldEdit optionala;
- AI doar pentru drafturi validate de admin.

### Faza U8: Scalare Si Performanta

Scop: serverul ramane stabil cu mai multe sate si NPC-uri.

Prioritati:

- tick budget pentru simulare;
- limite pe NPC-uri active;
- lazy loading pentru zone;
- audit performanta;
- debugdump cu timpi si numar de entitati;
- teste pe restart si chunk unload/load.

### Faza U9: Release Public Controlat

Scop: pachetul poate fi folosit de alt admin fara interventie directa in cod.

Prioritati:

- release checklist complet;
- migration si backup documentate;
- config template curat;
- ghid de instalare scurt;
- rollback testat;
- smoke Paper obligatoriu inainte de publicare.

## Ordine Recomandata De Executie

Ordinea practica pentru urmatoarea perioada:

1. Stabilizeaza MVP-ul Paper pe `demo_sat`.
2. Ruleaza smoke mapping/spawn dupa restart.
3. Ruleaza smoke Q01-Q08 si cate un smoke pentru mecanicile generice.
4. Muta populatia narativa in dry-run inspectabil.
5. Leaga runtime-ul extensibil doar dupa ce smoke-urile existente sunt stabile.
6. Abia apoi extinde economie, reputatie, story reactii si generare lume.

## Comenzi De Control Rapid

Build:

```powershell
.\gradlew.bat test
.\gradlew.bat :ainpc-core-plugin:jar :ainpc-scenario-medieval:jar
```

Smoke mapping:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 -ServerDir "C:\Minecraft\paper-test"
```

Smoke quest:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 -ServerDir "C:\Minecraft\paper-test" -PlayerName "NumeleTau"
```

Audit in joc:

```text
/ainpc audit all
/ainpc debugdump all
```

## Reguli De Siguranta

- Nu edita `ainpc_data.db` cat timp serverul ruleaza.
- Nu spawna settlement fara plan inspectat.
- Nu lasa AI-ul sa execute direct schimbari de lume sau progres.
- Nu considera un feature gata fara restart test.
- Nu trece la mecanici mari daca mapping/NPC/routine nu sunt stabile.
- Nu publica JAR fara smoke Paper si backup.

