# Server Admin Runbook

Actualizat: 2026-05-04

Status: ghid operational initial pentru administrarea unui server Paper cu AINPC. Nu inlocuieste `release-checklist.md`, `debugging-si-testare.md`, `audit.md` sau documentele de migration/backup care urmeaza.

## Scop

Acest runbook raspunde la intrebarea:

```text
Ce face un admin ca sa instaleze, porneasca, verifice si depaneze AINPC pe un server real?
```

Documentul este orientat spre operare. Nu descrie design intern, nu promite features viitoare si nu cere teste automate ca preconditie pentru citire.

## Cand il folosesti

Foloseste acest document cand:

- instalezi pluginul pe un server Paper local sau de test;
- verifici daca pluginul a pornit corect;
- configurezi OpenAI si fallback-ul local;
- verifici mapping-ul world admin;
- pregatesti un smoke test manual;
- colectezi debug dump pentru o problema;
- trebuie sa faci cleanup sigur dupa un incident.

## Cerinte minime

| Componenta | Cerinta |
| --- | --- |
| Server | Paper compatibil cu `api-version: 1.21` |
| Permisiuni | OP sau `ainpc.admin` pentru comenzi administrative |
| Plugin core | `ainpc-core-plugin-1.0.0.jar` |
| Addon demo | `ainpc-scenario-medieval-1.0.0.jar`, optional dar util pentru quest smoke |
| Config | `plugins/AINPC/config.yml` generat la prima pornire |
| DB | `plugins/AINPC/ainpc_data.db` pentru SQLite |

Comenzile se ruleaza cu `/` in joc. In consola Paper se ruleaza fara `/`.

## Fisiere importante

```text
plugins/
  ainpc-core-plugin-1.0.0.jar
  ainpc-scenario-medieval-1.0.0.jar

plugins/AINPC/
  config.yml
  quests.yml
  ainpc_data.db
  debug-dumps/
```

Regula: nu edita `ainpc_data.db` cat timp serverul este pornit.

## Instalare curata

1. Opreste serverul Paper.
2. Copiaza JAR-ul core in `plugins/`.
3. Copiaza addonul medieval in `plugins/`, daca vrei quest demo.
4. Porneste serverul o data ca sa genereze `plugins/AINPC/config.yml`.
5. Opreste serverul.
6. Editeaza configuratia minima.
7. Porneste serverul.
8. Ruleaza verificarea de baza.

Build local, daca JAR-urile nu exista:

```powershell
mvn package -DskipTests
```

JAR-uri asteptate:

```text
ainpc-core-plugin/target/ainpc-core-plugin-1.0.0.jar
ainpc-scenario-medieval/target/ainpc-scenario-medieval-1.0.0.jar
```

## Configuratie minima

Fisier runtime:

```text
plugins/AINPC/config.yml
```

Setari care conteaza la prima pornire:

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

debug: false
```

Recomandare operationala:

- pastreaza `openai.api_key` gol;
- seteaza cheia in mediul serverului ca `OPENAI_API_KEY`;
- pastreaza `debug: false` pe server public;
- activeaza debug doar temporar cand colectezi date.

## Verificare dupa pornire

Ruleaza:

```text
/plugins
/ainpc
/ainpc audit
/ainpc audit world
/ainpc audit db
/ainpc list
```

Semne bune:

- pluginul apare in `/plugins`;
- `/ainpc` afiseaza help, nu eroare;
- `/ainpc audit db` nu raporteaza initializare DB esuata;
- `/ainpc audit world` nu raporteaza crash sau WorldAdmin indisponibil;
- logul nu contine exceptii repetate la startup.

Observatie: `World mapping: 0 regiuni, 0 places, 0 nodes` nu este neaparat bug pe server nou. Inseamna ca mapping-ul nu a fost creat sau importat inca.

## Flux minim pentru mapping

Pentru o harta de test:

```text
/ainpc world demo create demo_sat
/ainpc world whereami
/ainpc world places demo_sat
/ainpc audit world
/ainpc world save
```

Pentru un sat vanilla existent:

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

Mapping-ul trebuie verificat inainte sa fie folosit pentru spawn, rutine, questuri sau story context.

## Flux minim pentru NPC

Comenzi de baza:

```text
/ainpc create <nume>
/ainpc list
/ainpc info <nume>
/ainpc tp <nume>
/ainpc audit npc
```

Daca exista mapping:

```text
/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]
/ainpc routine status <numeNpc>
/ainpc audit spawn
```

Reguli:

- nu folosi nume duplicate pentru NPC-uri de test;
- dupa spawn sau bind, ruleaza audit;
- dupa restart, verifica din nou `list`, `info` si `audit npc`.

## Flux minim pentru household si settlement

Pentru plan fara spawn direct:

```text
/ainpc world household plan <homePlaceId> [count]
/ainpc world settlement plan <regionId> [maxHouses]
```

Pentru spawn controlat:

```text
/ainpc world household spawn <homePlaceId> [count]
/ainpc world settlement spawn <regionId> [maxHouses]
/ainpc audit spawn
```

Atentie: spawn-ul trebuie facut doar dupa ce mapping-ul este verificat. Daca regiunea are putine case sau nodes lipsa, foloseste planul ca diagnostic, nu forta spawn-ul.

## Flux minim pentru quest

Comenzi utile:

```text
/ainpc quest log
/ainpc quest nearest
/ainpc quest accept nearest
/ainpc quest status nearest
/ainpc quest track start
/ainpc quest anchors
/ainpc audit quest
```

Aliasuri disponibile:

```text
/npcquest
/npcq
/quest
/quests
```

Pentru quest smoke cu addonul medieval, vezi `scripts/smoke-paper-quests.ps1` si `debugging-si-testare.md`.

## OpenAI si fallback

Verificari:

- `OPENAI_API_KEY` exista in mediul procesului serverului;
- `openai.base_url` este `https://api.openai.com/v1`, daca folosesti configuratia standard;
- `openai.model` este setat la modelul dorit;
- `openai.diagnostics.check_on_startup` este activ in mediul de test;
- serverul are acces la internet.

Script util:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\debug-openai.ps1
```

Semne de problema:

- NPC-ul raspunde doar cu fallback local;
- logul arata autentificare esuata;
- logul arata model invalid;
- request-urile OpenAI au timeout.

In productie, nu pune cheia API in documentatie, screenshot-uri, debug dumps trimise public sau commit-uri.

## Audit si debug dump

Audit rapid:

```text
/ainpc audit
/ainpc audit all
/ainpc audit npc
/ainpc audit world
/ainpc audit db
/ainpc audit spawn
/ainpc audit quest
```

Debug dump:

```text
/ainpc debugdump
/ainpc debugdump all
/ainpc debugdump npc
/ainpc debugdump world
/ainpc debugdump quest
/ainpc debugdump story
```

Artefacte asteptate:

```text
plugins/AINPC/debug-dumps/debug-dump-YYYYMMDD-HHMMSS/
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

Reguli:

- auditul este read-only;
- debugdump este pentru investigatie offline;
- verifica manual sa nu trimiti secrete sau date sensibile;
- dupa colectare, dezactiveaza debug daca l-ai activat temporar.

## Smoke test manual asistat

Pentru mapping/spawn:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

Pentru questuri:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau"
```

Scripturile:

- pot construi JAR-urile;
- copiaza pluginurile in `plugins/`;
- genereaza fisiere cu comenzi de rulat in Paper;
- genereaza raport cu hash-uri si verificari de baza.

Pentru documentatia completa, vezi `scripts/README.md`.

## Backup rapid inainte de operatii riscante

Inainte de cleanup, migration manuala, upgrade sau test pe date reale:

1. Opreste serverul.
2. Copiaza folderul `plugins/AINPC/`.
3. Copiaza lumea sau regiunea afectata, daca operatia poate modifica lumea.
4. Noteaza versiunea JAR-urilor.
5. Porneste serverul doar dupa ce backup-ul este confirmat.

Minimum de copiat:

```text
plugins/AINPC/config.yml
plugins/AINPC/quests.yml
plugins/AINPC/ainpc_data.db
plugins/AINPC/debug-dumps/
```

Documentul dedicat `migration-si-backup.md` ramane de creat pentru politici complete.

## Upgrade controlat

Pentru un release complet, foloseste `release-checklist.md`. Fluxul de mai jos este varianta scurta pentru upgrade operational.

Flux recomandat:

1. Citeste changelog-ul intern sau commit-ul care aduce noul JAR.
2. Opreste serverul.
3. Fa backup la `plugins/AINPC/` si la lumea de test.
4. Inlocuieste JAR-urile in `plugins/`.
5. Porneste serverul.
6. Ruleaza `/ainpc audit all`.
7. Ruleaza smoke-ul relevant: mapping, NPC, quest.
8. Pastreaza JAR-ul vechi pana cand noua versiune este verificata.

Nu combina upgrade de JAR, editare DB si schimbare de config in acelasi pas daca poti evita.

## Troubleshooting rapid

| Simptom | Verificare | Actiune |
| --- | --- | --- |
| `/ainpc` nu exista | `/plugins`, log startup | Verifica daca JAR-ul core este in `plugins/` si serverul este Paper compatibil |
| Pluginul porneste, dar AI nu raspunde | OpenAI diagnostics, env var | Verifica `OPENAI_API_KEY`, `base_url`, model si internet |
| Audit world arata 0 regiuni | `world_admin.enabled`, comenzi world | Creeaza demo mapping sau importa sat vanilla |
| NPC-ul nu are rutina coerenta | `info`, `routine status`, anchors | Leaga home/work/social si ruleaza `audit spawn` |
| Questul nu porneste | addon medieval, `audit quest`, anchors | Verifica daca addonul este incarcat si exista NPC/anchor potrivit |
| NPC duplicat | `audit npc`, `debugdump npc` | Urmeaza `prevenire-duplicare-npc.md`; nu sterge DB live |
| DB pare corupt sau blocat | server oprit, backup | Nu repara live; colecteaza debugdump si restaureaza din backup daca este necesar |
| Lag dupa activare | tick-uri simulation/routine, log | Mareste intervalele sau dezactiveaza temporar subsistemul afectat |

## Reguli de siguranta

- Nu edita DB-ul cu serverul pornit.
- Nu folosi `/ainpc delete <nume>` cand exista nume duplicate.
- Nu sterge global villageri ca metoda de cleanup.
- Nu lasa `debug: true` permanent pe server public.
- Nu publica debug dumps fara verificare manuala.
- Nu trata un plan generat ca efect aplicat in lume.
- Nu rula spawn de household/settlement fara mapping verificat.
- Nu introduce WorldEdit ca dependinta obligatorie pentru operarea curenta.

## Cand escaladezi catre documente specializate

| Caz | Document |
| --- | --- |
| Audit si debugdump | `audit.md`, `debugging-si-testare.md` |
| NPC duplicati | `prevenire-duplicare-npc.md` |
| Mapping manual | `mapping.md`, `mapping-harti-manuale.md` |
| Spawn order si rollback | `ordine-spawn-npc-cladiri-region-node.md` |
| Quest anchors | `quest-anchor-bindings.md` |
| Release build/test | `release-checklist.md` |
| JAR mare sau shade | `reducere-marime-jar.md` |
| WorldEdit optional | `worldedit-integration-contract.md` |

## Definitie de gata pentru server test

Un server de test este intr-o stare buna cand:

- `/plugins` arata core-ul AINPC incarcat;
- `/ainpc audit all` nu raporteaza erori critice;
- `plugins/AINPC/config.yml` este configurat pentru mediul curent;
- `plugins/AINPC/ainpc_data.db` exista;
- mapping-ul minim este creat sau se intelege explicit ca lipseste;
- un NPC de test poate fi creat, listat si inspectat;
- debugdump poate fi generat la nevoie;
- backup-ul folderului `plugins/AINPC/` este facut inainte de operatii riscante.
