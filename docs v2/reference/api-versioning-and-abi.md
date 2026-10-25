# Versionare API si ABI

Status: politica publica si gate operational canonic.
Actualizat: 2026-10-25.

`ainpc-api` are versiune proprie, baseline JVM determinist si verificare obligatorie la build/release. Codul si baseline-ul comis raman autoritatea pentru ABI-ul public curent.

## Sursa versiunii

- `apiVersion` din `gradle.properties` este versiunea publica a modulului `ainpc-api`;
- `projectVersion` ramane versiunea distributiei core/addon si poate evolua independent;
- JAR-ul API este produs ca `ainpc-api-<apiVersion>.jar`;
- manifestul JAR expune `Implementation-Version` si `AINPC-API-Version` cu aceeasi valoare;
- addonurile compileaza impotriva unei versiuni API explicite, nu impotriva JAR-ului core.

## Politica SemVer

- **patch**: corectii de comportament, documentatie sau implementare interna care nu schimba ABI-ul JVM public;
- **minor**: extensii publice compatibile intentionat, precum tipuri, metode, constructori sau membri noi;
- **major**: eliminari, redenumiri, schimbari de descriptor JVM, mostenire, vizibilitate ori alte schimbari care pot rupe consumatorii compilati;
- schimbarea contractelor de nullability sau a semanticii publice poate necesita bump chiar daca snapshot-ul JVM nu se modifica;
- orice diferenta ABI trebuie clasificata si revizuita; scriptul detecteaza diferenta, dar nu decide singur nivelul SemVer.

## Baseline si verificare

Baseline-ul comis este `ainpc-api/abi/ainpc-api-abi-baseline.json`. `scripts/api-abi-check.ps1` ruleaza `javap -protected -s -constants`, normalizeaza clasele publice, exclude clasele sintetice cunoscute si compara hash-uri per clasa plus hash-ul agregat.

Verificarea exacta detecteaza atat schimbari incompatibile, cat si extensii compatibile. Orice mismatch blocheaza task-ul `check` si raportul de freeze rulat cu `-FailOnWarnings` pana la o decizie explicita.

```powershell
./gradlew.bat :ainpc-api:verifyApiAbi
./gradlew.bat :ainpc-api:updateApiAbiBaseline
./scripts/release-api-addon-freeze.ps1 -ProjectRoot . -ReleaseId "<release-id>" -FailOnWarnings
```

## Flux pentru schimbari API

1. modifica suprafata publica si adauga teste de consum Kotlin/Java unde este relevant;
2. clasifica schimbarea SemVer si actualizeaza `apiVersion`;
3. ruleaza `:ainpc-api:updateApiAbiBaseline` numai pentru schimbarea intentionata;
4. revizuieste diff-ul JSON si snapshot-ul text din `ainpc-api/build/reports/abi`;
5. ruleaza `:ainpc-api:check`, testele proiectului si freeze-ul cu `-FailOnWarnings`;
6. pastreaza in dovada de release versiunea API, hash-ul ABI si raportul freeze.

Actualizarea baseline-ului refuza regresia de versiune si refuza un ABI nou cu acelasi `apiVersion`. Exceptia este schimbarea identificatorului generatorului, folosita numai pentru refresh de tooling si revizuita ca atare.

## Revizuire API 1.1.0 - 2026-10-25

`apiVersion=1.1.0` clasifica drept extensii minor doua schimbari publice deja existente in surse, fara modificarea `projectVersion=1.0.0` sau a implementarii cognitive:

- `PermissionNode.PROFILE` adauga permisiunea `ainpc.profile`, cu nivel `PLAY` si valoare implicita `true`; constantele si metodele anterioare sunt pastrate;
- `WorldRegionInfo` adauga constructorul cu `storyDescriptions` si metoda `storyDescriptions()`, pastrand descriptorul constructorului anterior cu 14 argumente si valorile implicite pentru argumentele nullable;
- testele Java verifica apelurile constructorilor, accesul la membri si cautarea permisiunilor; testele Kotlin verifica apelurile cu argumente numite, inclusiv constructorul anterior;
- baseline-ul regenerat intentionat pastreaza generatorul `javap-protected-descriptors-v2` si cele 166 de clase; numai cele doua clase de mai sus au semnaturi schimbate fata de baseline-ul 1.0.0.

Hash-ul ABI 1.1.0 este `E4944F1E9440FFDB285D49A935A42C0E2AACC510FA1A8F9AD3EA6A0925AACD39`. Adaugarea constantei enum nu garanteaza stabilitatea ordinalelor sau a logicii exhaustive scrise de consumatori; foloseste identificatorul permisiunii, nu pozitia sa in enum, si trateaza explicit valorile noi. Clasificarea si testele de consum nu inlocuiesc verificarea addonurilor si smoke-ul Paper.

## Limite

- snapshot-ul acopera semnaturile JVM publice/protected, constantele si constructorii vizibili, nu compatibilitatea comportamentala;
- adnotarile sursa precum `@Deprecated`, mesajul si nivelul lor nu sunt garantate de hash-ul descriptorilor si cer teste de contract plus documentatie separata;
- metodele default Kotlin, overload-urile, descriptorii, clasele companion si tipurile sintetice publice relevante pot afecta consumatorii Java;
- nullability, reguli de lifecycle, ordine de callback si semantica DTO necesita teste si documentatie separate;
- un hash identic nu demonstreaza compatibilitate cu Paper, startup reusit sau comportament runtime corect.

## Legaturi

- `reference/documentatie-api.md`
- `reference/kotlin-interop-api-addonuri.md`
- `reference/addon-developer-guide.md`
- `operations/release-checklist.md`
- `planning/stabilizare-api-si-addonuri.md`
