# Analiza Erori si Plan de Rezolvare

Actualizat: 2026-04-28

## Scop

Acest document centralizeaza erorile si riscurile confirmate in proiect si descrie cum trebuie rezolvate.

Important:

- documentul nu presupune modificari de cod facute acum
- analiza separa erorile reale de problemele structurale
- rezultatele sunt bazate pe rulare locala si pe logurile existente in repo

## Ce a fost verificat

Au fost rulate urmatoarele verificari:

- `mvn test`
- `mvn clean test`
- inspectarea logurilor din `debug-logs/`
- inspectarea scriptului `scripts/debug-openai.ps1`
- inspectarea codului din `OpenAIService` si `FeaturePackLoader`

Data verificarii:

- 2026-04-28

## Rezumat executiv

La momentul analizei, proiectul nu are erori de compilare sau test care sa blocheze build-ul.

Rezultatul real este:

- build Maven: `SUCCESS`
- compilare curata de la zero: `SUCCESS`
- teste automate existente: `SUCCESS`

Erorile confirmate in repo sunt in acest moment de doua tipuri:

1. erori istorice in logurile de diagnostic pentru Ollama
2. probleme structurale sau de observabilitate care nu rup build-ul acum, dar pot produce defecte mai greu de depistat

## Rezultatele build-ului

### Status actual

`mvn clean test` a trecut complet pentru:

- `ainpc-api`
- `ainpc-core-plugin`
- `ainpc-scenario-medieval`

Nu au fost reproduse:

- erori de compilare
- erori de test
- erori de packaging in faza test

### Concluzie

Nu exista in acest moment o eroare Java confirmata care sa blocheze build-ul proiectului.

Prin urmare, orice documentatie de remediere trebuie sa porneasca de la ideea corecta:

- problema actuala nu este una de compilare
- problemele reale sunt de diagnostic runtime, logging si structura de build

## Erori confirmate

### Eroarea 1: diagnostic HTTP Ollama cu acces invalid la proprietatea `Response`

Sursa:

- `debug-logs/ollama-debug-20260423-195901.log`

Mesaj observat:

- `The property 'Response' cannot be found on this object. Verify that the property exists.`

Context:

- eroarea apare la probele HTTP pentru `/api/version` si `/api/tags`
- logul indica faptul ca procesul `ollama` era pornit si asculta pe portul `11434`
- comanda `ollama list` functiona

Interpretare:

- problema nu pare sa fie ca Ollama nu ruleaza
- problema pare sa fie in scriptul de diagnostic, mai exact in tratarea rezultatului unei exceptii HTTP sau a unui obiect returnat diferit fata de ce asteapta scriptul

Impact:

- diagnosticarea locala devine nesigura
- logul poate sugera fals ca serverul e problema, desi defectul este in script

Plan de rezolvare:

1. scriptul de diagnostic trebuie sa foloseasca un wrapper uniform pentru raspunsurile HTTP
2. accesarea directa a unor proprietati conditionale precum `Response` trebuie eliminata
3. orice eroare HTTP trebuie serializata intr-o structura fixa:
   - `Success`
   - `StatusCode`
   - `Body`
   - `Error`
4. logurile trebuie sa afiseze separat:
   - eroarea de transport
   - codul HTTP, daca exista
   - preview-ul body-ului, daca exista

Observatie:

- scriptul actual `scripts/debug-openai.ps1` este deja mult mai aproape de acest model, ceea ce sugereaza ca logurile din `debug-logs/ollama-debug-*.log` provin dintr-o varianta mai veche sau dintr-un script separat care nu mai este in repo

### Eroarea 2: `Object reference not set to an instance of an object` in diagnosticul Ollama

Sursa:

- `debug-logs/ollama-debug-20260423-195921.log`

Mesaj observat:

- `Request esuat: Object reference not set to an instance of an object.`

Context:

- eroarea apare din nou la probele HTTP
- procesul si portul Ollama erau active
- `ollama list` functiona

Interpretare:

- aceasta este tot o eroare de scripting sau de tratare a raspunsului
- cel mai probabil exista o referinta nula in ramura de diagnostic HTTP
- nu exista date care sa indice o exceptie Java in plugin

Impact:

- investigarea incidentelor AI locale este ingreunata
- se pierde claritatea intre:
  - server indisponibil
  - endpoint gresit
  - raspuns neasteptat
  - bug in script

Plan de rezolvare:

1. orice acces la obiecte returnate din HTTP trebuie protejat prin verificari explicite de null
2. codul de diagnostic trebuie sa trateze distinct:
   - esec de conexiune
   - timeout
   - HTTP non-2xx
   - raspuns cu body invalid
3. trebuie adaugat un mesaj final de sumar care spune explicit una dintre variante:
   - serverul nu raspunde
   - serverul raspunde dar endpoint-ul e incompatibil
   - scriptul a esuat intern
   - modelul nu este disponibil

## Probleme structurale confirmate

Acestea nu sunt erori de build acum, dar sunt surse reale de defecte si confuzie.

### Problema 1: `ainpc-core-plugin` compila din folderul legacy `src/src`

Confirmare:

- inainte de refactorizarea din 2026-04-28, `ainpc-core-plugin/pom.xml` folosea:
  - `${project.basedir}/../src/src/main/java`
  - `${project.basedir}/../src/src/main/resources`

Status:

- rezolvat pentru build-ul Maven
- sursele si resursele core sunt acum in `ainpc-core-plugin/src/main`
- folderul legacy `src/src` a fost eliminat dupa migrare
- `mvn clean test` trece dupa schimbare

Impact:

- inainte de remediere, codul sursa real al modulului nu era localizat in modul
- IDE-ul, build-ul si refactorizarile puteau deveni greu de urmarit
- era usor sa apara divergente intre structura declarata si structura reala

Plan de rezolvare:

1. finalizat: mutarea fizica a surselor in `ainpc-core-plugin/src/main/java`
2. finalizat: mutarea resurselor in `ainpc-core-plugin/src/main/resources`
3. finalizat: revenirea la layout Maven standard in `pom.xml`
4. finalizat: rularea `mvn clean test` dupa migrare
5. finalizat: stergerea folderului legacy dupa validarea build-ului
6. ramas: curatarea mentiunilor istorice din documentatie doar unde incurca navigarea curenta

Prioritate:

- rezolvata pentru build; curatarea completa ramane prioritate medie

### Problema 2: logging slab la incarcarea feature pack-urilor

Confirmare:

- `FeaturePackLoader.loadPack()` logheaza doar:
  - `Eroare la incarcarea feature pack: <fisier>`
- apoi apeleaza `e.printStackTrace()`

Impact:

- mesajele nu sunt structurate
- lipsesc date utile precum:
  - pack id
  - sectiunea YAML problematica
  - cauza functionala
- in productie, stack trace-ul brut este mai greu de filtrat si corelat

Plan de rezolvare:

1. inlocuirea `printStackTrace()` cu logging prin `plugin.getLogger()`
2. includerea contextului minim:
   - fisier
   - pack id
   - sectiune
   - cauza exceptionala
3. introducerea unui raport de validare pentru pack-uri
4. diferentierea intre:
   - eroare de parse
   - eroare de schema
   - eroare de dependinte
   - override invalid

Prioritate:

- medie spre mare

### Problema 3: acoperire de teste foarte mica pentru zonele critice

Confirmare:

- in `ainpc-core-plugin` exista doar testul `WorldAdminServiceTest`
- nu exista teste pentru:
  - `FeaturePackLoader`
  - `OpenAIService`
  - `AddonRegistry`
  - `ScenarioEngine`
  - `AINPCPlugin.reloadContent()`

Impact:

- defectele de runtime trec usor de build
- erorile de configurare si integrare nu sunt prinse devreme

Plan de rezolvare:

1. adaugarea de teste pentru incarcarea pack-urilor valide si invalide
2. adaugarea de teste pentru fallback-ul din `OpenAIService`
3. adaugarea de teste pentru `AddonRegistry`
4. adaugarea de smoke tests pentru lifecycle:
   - startup
   - reload
   - load addon

Prioritate:

- mare

## Probleme care nu sunt erori confirmate acum

Acestea trebuie tratate corect ca riscuri, nu ca defecte demonstrate:

- `ScenarioEngine` este foarte mare si greu de intretinut
- `NPCManager` este foarte mare si probabil prea cuplat
- `AINPCPlugin` concentreaza prea mult wiring

Acestea nu au generat o eroare de build in analiza curenta, dar cresc probabilitatea de:

- regresii la refactorizare
- bug-uri greu de izolat
- timp mare de depanare

## Ordinea recomandata de rezolvare

1. Clarificarea zonei de diagnostic AI.
   - Se decide daca scriptul vechi pentru Ollama mai este suportat.
   - Daca da, se aduce la acelasi standard cu `scripts/debug-openai.ps1`.

2. Standardizarea logurilor de eroare pentru feature packs si AI probes.
   - Fara `printStackTrace()`.
   - Cu context functional clar.

3. Curatarea mentiunilor istorice ramase catre `src/src`, doar unde incurca navigarea curenta.
   - Build-ul foloseste deja `ainpc-core-plugin/src/main`, iar folderul legacy a fost eliminat.

4. Cresterea acoperirii de teste in zonele de integrare.
   - Prioritar pentru pack loading, addon lifecycle si AI fallback.

5. Abia dupa aceea refactorizarea claselor mari.
   - `ScenarioEngine`
   - `NPCManager`
   - `AINPCPlugin`

## Definitia de rezolvare

Analiza de fata poate fi considerata inchisa doar cand sunt indeplinite toate conditiile de mai jos:

- scriptul de diagnostic nu mai produce erori interne de tip `Response` lipsa sau `Object reference not set`
- logurile disting clar intre eroare de script, eroare de retea si eroare de API
- `FeaturePackLoader` nu mai foloseste `printStackTrace()`
- `ainpc-core-plugin` compileaza din propriul `src/main/java`
- exista teste automate pentru cel putin:
  - pack loading
  - addon registry
  - AI fallback
  - reload content

## Concluzie

Starea actuala a proiectului este mai buna decat sugereaza logurile istorice:

- build-ul este verde
- testul existent trece
- nu exista o eroare Java de compilare confirmata acum

Problemele reale identificate sunt:

- diagnostic runtime fragil pentru zona AI/Ollama
- logging insuficient pentru erori de configurare
- mentiuni istorice ramase catre folderul legacy `src/src`
- acoperire de teste insuficienta pentru zonele cele mai riscante

Acestea sunt punctele care trebuie rezolvate primele.
