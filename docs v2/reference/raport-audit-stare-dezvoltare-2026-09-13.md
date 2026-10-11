# Raport de audit al starii de dezvoltare

Data auditului: 2026-09-13  
Ramura inspectata: `work4`  
Tip: analiza statica, inspectie Git si citire de rapoarte existente

## Domeniu si limita

Acest audit nu modifica codul, configuratia, dependintele, serverul sau artefactele existente. Nu au fost rulate build-uri, teste ori smoke script-uri noi, deoarece acestea ar scrie in directoarele de build/cache si ar contrazice cerinta de analiza fara modificari. Singura scriere realizata este acest raport.

Concluziile se bazeaza pe configuratia Gradle si GitHub Actions, sursa si scripturile relevante, rezultatele JUnit deja prezente si rapoartele de smoke existente. Diagnosticele Serena care nu pot rezolva clasele Paper, Gson si HikariCP au fost excluse din verdictul de compilare: ele produc erori in cascada de classpath, nu dovezi fiabile despre cod.

## Verdict

Proiectul este intr-o faza avansata de integrare si hardening, nu intr-o stare demonstrata pentru release. Arhitectura este substantiala: API public, plugin Paper principal, serviciu MCP Spring Boot si addon-ul medieval. Exista o suita mare de teste si CI, dar validarea operationala si unele gate-uri de calitate au defecte care pot raporta succes in mod fals.

Cele mai urgente corectii sunt: increderea in smoke test-ul VPS, caracterul neblocant al lint-ului CI, izolarea credentialelor implicite din scripturi si actualizarea documentatiei de proiect.

## Dovezi pozitive

- Configuratia Gradle declara patru module in `settings.gradle:13`, `settings.gradle:14`, `settings.gradle:15` si `settings.gradle:16`.
- Codul este majoritar Kotlin (826 fisiere), dar include si 52 fisiere Java: 17 in sursele principale si 35 in teste.
- Rezultatele JUnit locale existente contin 293 suite, 1.493 teste, zero esecuri, zero erori si un test sarit. Cele mai recente XML-uri sunt din 2026-07-26 pentru core; ele nu reprezinta o validare noua a arborelui curent.
- Raportul local `paper-addon-lifecycle-smoke.json` din 2026-07-17 consemneaza trecerea fazelor `core-only`, `core-with-addon` si `core-after-addon-removal`.
- API-ul are un gate de compatibilitate ABI: `ainpc-api/build.gradle:28` defineste `verifyApiAbi`, iar `ainpc-api/build.gradle:80` il leaga de `check`.
- Nu s-au gasit placeholder-e de productie de forma `TODO()`; aparitiile `UnsupportedOperationException` gasite sunt in test fixtures.
- `git diff --check` nu semnaleaza erori de whitespace in modificarile existente.

## Probleme confirmate

### P1 - Smoke-ul VPS poate declara succes dupa un deploy sau restart esuat

`scripts/smoke-vps.ps1:95` ruleaza `scp`, iar `scripts/smoke-vps.ps1:97` marcheaza faza ca reusita fara verificarea lui `$LASTEXITCODE`. Acelasi tipar exista pentru restart la `scripts/smoke-vps.ps1:103` si `scripts/smoke-vps.ps1:106`.

Impact: un JAR poate sa nu fie copiat sau serverul poate sa nu fie repornit, iar raportul final poate ramane aparent verde. Acest lucru reduce valoarea smoke test-ului ca release gate.

Recomandare: verifica explicit codul de iesire dupa fiecare apel `scp`/`ssh`, marcheaza faza drept esuata si opreste rularea cand actiunea critica esueaza.

### P1 - Lint-ul CI nu poate bloca un merge

Jobul `lint` ruleaza `./gradlew kotlinLint --no-daemon || true` in `.github/workflows/build.yml:93-94`. Operatorul `|| true` transforma inclusiv o comanda inexistenta sau un lint cu erori in succes. In fisierele Gradle nu exista configuratie pentru un task `kotlinLint` (nu s-au gasit pluginuri Ktlint/Detekt/Checkstyle).

Impact: badge-ul CI poate fi verde fara ca stilul Kotlin sa fi fost verificat.

Recomandare: instaleaza si configureaza un singur linter ales, ruleaza task-ul real fara `|| true` si face ca orice incalcare sa esueze jobul.

### P1 - Smoke-ul nu pastreaza suficiente dovezi pentru investigarea unui esec

`Run-Check` clasifica rezultatele predominant dupa textul returnat (`scripts/smoke-vps.ps1:35-59`) si logheaza doar comanda (`scripts/smoke-vps.ps1:60`), nu si statusul, codul de iesire sau outputul sanitizat. Logul VPS din 2026-06-29 consemneaza un rezultat de 19 treceri si un esec, dar nu permite identificarea exacta a comenzii esuate; o rulare anterioara a surprins si o exceptie `ToInt32` in lantul RCON.

Impact: regresiile operationale sunt greu de reprodus si de atribuit.

Recomandare: pentru fiecare comanda scrie in raport numele fazei, comanda, `$LASTEXITCODE`, rezultat `PASS`/`FAIL` si o versiune sanitizata, limitata, a outputului.

### P2 - Parserul RCON accepta pachete partial citite

In `scripts/rcon-client.ps1:79-85`, citirea payload-ului se opreste cand stream-ul returneaza zero, dar codul continua sa parseze bufferul alocat. Verificarea de la `scripts/rcon-client.ps1:85` compara dimensiunea bufferului alocat, nu numarul de octeti cititi. Un pachet trunchiat poate fi interpretat ca raspuns valid cu date partiale sau umplute cu zero.

Impact: raspunsuri RCON invalide pot fi raportate ca date aplicative, iar smoke-urile pot deveni instabile.

Recomandare: returneaza imediat `null` cand `$read -lt $len`; adauga teste pentru EOF, timeout si citire partiala. Evita mentinerea implementarii duplicate din `scripts/smoke-rcon-test.ps1`.

### P2 - CI nu ruleaza automat la push pe ramura activa

Ramura activa este `work4`, dar `.github/workflows/build.yml:4-7` declanseaza la push numai pentru `main`, `develop` si `feature/**`. Un pull request spre `main` este acoperit, insa push-urile obisnuite pe `work4` nu primesc feedback CI automat.

Impact: modificarile pot ramane neverificate pana la deschiderea unui PR sau la rularea manuala a workflow-ului.

Recomandare: include ramurile de lucru relevante ori adopta un trigger general pentru push, pastrand protectia de merge la nivel de `main`.

### P2 - Scripturile de operare contin valori implicite sensibile sau dependente de masina

`scripts/smoke-vps.ps1:2-7` contine implicit host, utilizator, cale locala de cheie SSH si parola RCON. Fisierul este versionat. Exista deja un precedent mai sigur: `scripts/deploy-vps.ps1` foloseste variabile de mediu pentru aceleasi tipuri de parametri.

Impact: risc de expunere accidentala, rulare impotriva mediului gresit si imposibilitate de reutilizare in CI sau de catre alti dezvoltatori.

Recomandare: elimina valorile reale/locale din implicite, cere parametri sau citeste doar variabile de mediu/secrete CI; valideaza lipsa lor inainte de conectare.

### P2 - README-ul nu mai corespunde structurii si statisticilor reale

`README.md:28-32` listeaza trei module si omite `ainpc-mcp-service`, desi modulul este inclus in `settings.gradle:15`. `README.md:79-81` afirma „0 fisiere Java” si „100% Kotlin”, dar repository-ul are 17 fisiere Java principale si 35 de teste Java. Statisticile de test sunt de asemenea depasite: artefactele locale curente indica 1.493 teste, nu aproximativ 1.060.

Separat, `README.md:4` afiseaza badge-ul MIT cu tinta `LICENSE`, dar fisierul `LICENSE` nu exista in repository.

Impact: onboarding, evaluarea tehnologica si asteptarile legale ale utilizatorilor sunt induse in eroare.

Recomandare: actualizeaza structura, limbajele si statisticile README-ului, apoi adauga licenta declarata sau elimina badge-ul pana cand decizia legala este stabilita.

## Riscuri si datorie tehnica

### Verificare curenta incompleta

Arborele de lucru avea la inceputul auditului 484 intrari necommit-uite: 481 fisiere modificate si 3 neversionate. Diferenta Kotlin contine 2.277 linii adaugate si zero linii de productie schimbate semantic in esantionul verificat; toate adaugarile Kotlin se potrivesc cu un sweep automat de KDoc. Aceasta nu este o eroare functionala dovedita, dar ramane o stare care necesita commit separat si o verificare dupa integrare.

### Functionalitati de demo si validare live inca deschise

`TODO.md:139` pastreaza ca prioritati smoke test-uri Paper live pentru mapping, settlement spawn, quest/progression si story. `TODO.md:298` marcheaza inca nefinalizate scenariul medieval complet jucabil, NPC-urile cu roluri distincte, questurile cap-coada, smoke/restart/audit/debugdump si instalarea simpla pentru administratori.

Concluzie: functiile de baza sunt dezvoltate, dar demonstratia end-to-end si validarea in server real nu sunt inchise.

### Reproductibilitate a build-ului

`gradle.properties:7` foloseste un Paper API `SNAPSHOT`; nu exista fisiere Gradle de dependency locking sau verification metadata. In plus, `build.gradle:60-63` genereaza un timestamp curent, iar `build.gradle:115-125` il insereaza in resurse la fiecare build.

Impact: acelasi commit poate rezolva dependinte diferite sau poate produce JAR-uri diferite intre rulări. Pentru dependintele care trebuie sa fie stabile, Gradle poate impune lock-uri de rezolvare; o astfel de politica trebuie aleasa constient, mai ales daca Paper SNAPSHOT trebuie sa ramana mobil.

### Complexitate si acoperire

Exista 11 fisiere Kotlin principale mai mari de 50 KiB; cele mai mari sunt `AINPCCommand.kt` (267.492 bytes) si `ScenarioEngine.kt` (151.491 bytes). Aceste zone concentreaza multe responsabilitati si devin costisitor de revizuit sau de testat. Doar patru suite XML sunt disponibile pentru addon-ul medieval, iar `NpcDefinitionsTest.kt:60` este dezactivat din cauza necesitatii MockBukkit/`RegistryAccess`.

`build.gradle:18-22` suprima global doua categorii de warning Kotlin. Impreuna cu lint-ul neblocant si controlul de warnings CI care ruleaza dupa un build deja efectuat (`.github/workflows/build.yml:49-57`), aceasta reduce sansele de a observa regresii de calitate.

### Navigarea documentatiei

Documentatia este in tranzitie intre `docs/` si `docs v2/`; multe fisiere din primul director sunt doar redirect-uri text spre al doilea. Nu este un defect imediat, dar creste riscul ca documentele noi sa fie salvate in locul gresit sau ca un link vechi sa fie interpretat drept continut actual. Acest raport este plasat intentionat in `docs v2/reference/`.

## Prioritati recomandate

1. Repara `scripts/smoke-vps.ps1`: coduri de iesire, log de dovezi si secrete/configuratie din mediu.
2. Fa CI-ul cu adevarat blocant: task de lint configurat, fara `|| true`, avertismente colectate din compilare efectiva si trigger potrivit pentru ramurile active.
3. Testeaza robustetea RCON pentru pachete partiale si elimina implementarea duplicata.
4. Ruleaza, dupa autorizare, build-ul si toate testele curente; apoi un smoke Paper live pentru fluxurile ramase din `TODO.md`.
5. Actualizeaza README-ul si decizia de licentiere; stabileste daca lock-urile de dependinte si build-ul reproductibil sunt cerinte de release.
6. Sparge gradual clasele foarte mari dupa ce gate-urile de verificare sunt de incredere.

## Comenzi recomandate dupa autorizarea verificarilor care scriu artefacte

```powershell
.\gradlew.bat build
.\scripts\run-tests.ps1
.\gradlew.bat :ainpc-api:check
```

Pentru fluxurile Paper, ruleaza smoke-urile numai dupa corectarea raportarii de succes/eroare; altfel un rezultat verde nu este suficient ca dovada de deploy sau restart reusit.
