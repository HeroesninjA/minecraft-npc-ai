# Kotlin Paper Packaging si Smoke Test

Status: checklist operational pentru artefactele curente.
Actualizat: 2026-07-17.

Build-ul tinteste Java 21 si produce artefacte cu roluri diferite. Un build Gradle verde nu confirma singur classloading-ul Paper.

## Contract de packaging

| Artefact | Continut asteptat | Nu este |
| --- | --- | --- |
| `ainpc-api-<version>.jar` | clasele API Kotlin/JVM | plugin Paper; nu are `plugin.yml` |
| `ainpc-core-plugin-<version>.jar` | `plugin.yml`, API, Kotlin stdlib si dependinte runtime core | addon tematic |
| `ainpc-scenario-medieval-<version>.jar` | `plugin.yml`, clase addon, `config-template.yml`, resurse `packs/` | fat JAR cu API sau Kotlin stdlib |
| `ainpc-mcp-service.jar` | aplicatia Spring Boot MCP | plugin Paper |

`ainpc-scenario-medieval` foloseste `compileOnly` pentru API si Paper. `plugin.yml` declara `depend: [AINPCPlugin]`, astfel incat core-ul trebuie sa fie prezent si incarcat inaintea addonului.

## Resurse medievale

- toate fisierele din `src/main/resources/packs/` intra in JAR;
- numai `medieval.yml`, `social.yml` si `medieval_quest.yml` sunt copiate automat de lista `MANAGED_PACKS`;
- celelalte pack-uri ambalate sunt mostre disponibile, nu continut instalat automat;
- `config-template.yml` este actualizat la startup, iar `config.yml` existent este pastrat.

## Verificare Gradle

Pe Windows:

```powershell
.\gradlew.bat :ainpc-api:test :ainpc-core-plugin:test :ainpc-scenario-medieval:test
.\gradlew.bat :ainpc-api:jar :ainpc-core-plugin:jar :ainpc-scenario-medieval:jar
```

Mediul trebuie sa aiba un JDK compatibil si `JAVA_HOME` valid. Nu documenta un build ca reusit pe baza unui JAR vechi din `deploy/`.

## Verificare continut JAR

- API JAR: `AINPCPlatformApi.class`, fara `plugin.yml`;
- core JAR: `plugin.yml`, `AINPCPlatformApi.class`, clase `kotlin/` si dependintele runtime necesare;
- addon JAR: `plugin.yml`, clasa main, `config-template.yml` si resursele `packs/`;
- verifica versiunea expandata in `plugin.yml` si absenta semnaturilor duplicate invalide;
- verifica faptul ca addon JAR nu dubleaza clasele `ro.ainpc.api`.

## Smoke Paper

Gate-ul automat canonic este:

```powershell
.\scripts\smoke-paper-addon-lifecycle.ps1 -PaperJar "C:\cale\paper.jar" -AcceptEula
```

`-PaperJar` poate fi omis daca `PAPER_JAR` este setat sau exista un `paper-data/data/paper-*.jar`. Java este rezolvat din `-JavaExecutable`, `JAVA_HOME` sau `PATH`. Scriptul construieste implicit JAR-urile; `-SkipBuild` este permis numai cand artefactele curente exista deja.

Pornirea necesita acum `-AcceptEula`, furnizat numai dupa acceptarea EULA de catre operator; lipsa lui opreste fluxul inainte de build, copiere sau pornire Java. Nu este acceptata automat de o comanda de pregatire. Verifica separat [cerintele Java ale versiunii Paper tinta](https://docs.papermc.io/paper/getting-started/#requirements).

Pentru pregatire fara pornire:

```powershell
.\scripts\smoke-paper-addon-lifecycle.ps1 -PaperJar "C:\cale\paper.jar" -SkipBuild -PrepareOnly
```

Modul creeaza numai un director nou in `.ai/tmp/paper-addon-lifecycle-*`, copiaza core-ul in `plugins`, scrie `eula=false` si configureaza bind pe `127.0.0.1`, port automat si RCON oprit. Nu copiaza lumi sau DB-uri existente, nu lanseaza procesul Paper si nu executa fazele runtime. Fara `-SkipBuild`, pregatirea poate rula build-ul Gradle folosind Java. `-PrepareOnly` pastreaza limitele de pornire si EULA chiar daca este combinat cu `-AcceptEula`. Directorul pregatit este pastrat fara a necesita `-KeepServer`.

Raportul implicit de pregatire este separat: `.ai/release-reports/paper-addon-lifecycle-preparation.json`. Succesul pregatirii are exit code 0, `status=prepared_not_run`, `preparation_ok=true`, `ok=false`, `smoke_attempted=false` si `phases=[]`; o eroare de pregatire are exit code nenul. Hash-urile identifica fisierele alese, nu compatibilitatea sau executarea lor. Raportul smoke anterior nu este suprascris implicit. Pregatirea nu implementeaza reluarea celor trei faze in directorul pastrat: o invocare runtime ulterioara creeaza alt director izolat si verifica din nou intrarile.

Runnerul creeaza un server temporar izolat si executa fara RCON trei faze:

1. core-only: confirma load, enable, comanda `plugins`, shutdown controlat si absenta resurselor medievale;
2. core-with-addon: confirma ordinea core/addon, config-ul per-addon, cele trei pack-uri gestionate si cleanup-ul lor la shutdown;
3. core-after-addon-removal: elimina JAR-ul addonului, reporneste core-ul si confirma ca addonul si folderul gestionat nu reapar.

Gate-ul esueaza la erori Paper, classloading, dependinta, descriptor respins, timeout, oprire fortata ori resurse ramase. Raportul JSON si cele trei loguri sunt scrise implicit in `.ai/release-reports/`. Serverul temporar este eliminat numai dupa succes si verificarea caii; la esec este pastrat pentru diagnostic. `-KeepServer` il pastreaza si dupa succes.

Smoke-urile functionale mapping, quest, NPC si AI raman verificari separate; acest gate demonstreaza numai packaging-ul, classloading-ul si lifecycle-ul resurselor addonului.

## Legaturi

- `reference/kotlin-interop-api-addonuri.md`
- `reference/addon-developer-guide.md`
- `reference/addon-config-template.md`
- `operations/debugging-si-testare.md`
