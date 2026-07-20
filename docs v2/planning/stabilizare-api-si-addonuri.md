# Stabilizare API si Addonuri

Status: inchis pentru scope-ul curent; redeschiderea cere un contract public nou sau o regresie demonstrata.
Actualizat: 2026-07-17.

Acest document pastreaza deciziile de stabilizare ale suprafetei publice. Backlogul de domeniu story si settlement continua in roadmap-urile dedicate, fara a fi prezentat drept provider API existent.

## Baseline implementat

- module Gradle separate pentru API, core, MCP si addonul medieval;
- `AINPCPlatformApi` publicat prin Bukkit `ServicesManager`;
- lifecycle `AINPCAddon` cu cleanup la activare esuata, rollback la inlocuire si dezactivare best-effort in operatiile bulk;
- callback-uri addon cu producatori core, izolate, raportate si sigure la dezregistrare reentranta;
- registry separat pentru integrari externe, cu commit dupa activare, rollback la inlocuire si cleanup best-effort la shutdown;
- accessorii `playerProgression`, `npcEconomy` si `storyAuthoring` conectati la serviciile runtime, cu adaptor story stabil;
- overload Java SAM `ObjectiveProgressHandler`, verificat printr-un consumer test Java;
- constructori Java `AddonDescriptor` si fixture consumer pentru descriptor, registry si lifecycle complet;
- `apiVersion` separat, manifest versionat, baseline `javap` determinist si gate ABI conectat la `check` si freeze-ul de release;
- coada persistenta `story_pending_events`, cu provider scoped pentru `hasPendingEvents`, publicare tranzactionala si discard explicit;
- producator scheduler opt-in pentru drafturi story din template-uri built-in, cu provenance, deduplicare scoped si comenzi admin de list/publish/discard;
- gate candidat in `registerAddon` pentru dependinte lipsa, cicluri si conflict de scenariu primar, inainte de lifecycle;
- cascada tranzitiva la unregister/reconfigurare pentru dependentii activi, cu ordine inversa, agregarea erorilor si fereastra de reload feature-pack;
- `registerDescriptor` pastreaza declaratiile cu probleme de graf si emite diagnostice, in timp ce `registerAddon` respinge candidatul de cod;
- `DependencyResolver` si `AddonDependencyGraph` sunt contracte rezervate de diagnostic, fara provider in `AINPCPlatformApi`; registrul detine validarea runtime;
- semantica `addons.load_order` documentata ca prioritate de descriptori, nu ordine Paper;
- feature pack loader cu metadata, dependinte intre pack-uri si reload;
- config si pack-uri gestionate de addonul medieval;
- evenimente publice si teste Java pentru o parte din contracte.
- audit exhaustiv `PublicApiClosureContractTest` pentru toate interfetele top-level, providerii core, punctele de extensie, contractul rezervat si absenta accessorilor-placeholder.

## Dependinte si lifecycle

- politica este inchisa: `registerDescriptor` ramane declarativ si diagnostic pentru graf, inclusiv la conflicte globale de scenariu primar; selectia ramane determinista, iar gate-ul strict este rezervat lifecycle-ului `registerAddon`;

## Contracte neconectate

- politica este inchisa: `DependencyResolver` nu este expus prin platforma deoarece nu exista niciun consumator addon; instanta si accessorii redundanti din `AINPCPlatform` au fost eliminati, iar o expunere viitoare cere un use case public si revizuire API;
- politica este inchisa: schedulerul poate adauga drafturi numai cu `story.random_events_require_review=true`, folosind template-uri built-in si cel mult un pending per scope; AI-ul ramane neconectat pana produce un contract structurat validabil;
- politica este inchisa: DTO-urile settlement raman modele de date pana exista un executor public confirmat in roadmap-ul de generare a satelor;
- politica este inchisa: nicio interfata declarata nu este prezentata drept serviciu disponibil fara provider runtime sau clasificare explicita.

## Schema si operare

- politica este inchisa: loaderul citeste si raporteaza cheia top-level `version`, iar cheia inexistenta `schema_version` este respinsa de testul de contract;
- politica este inchisa: `.yml`, `.yaml` si `.json` folosesc acelasi punct de incarcare, iar fixture-ul JSON trece prin metadata, traits, dialogues si scenarios intr-un test de integrare;
- politica este inchisa: fiecare addon isi detine schema; implementarea medievala valideaza strict, blocheaza versiunile viitoare si ofera `auto` cu backup/merge atomic sau `validate_only` fara scriere;
- politica este inchisa: `scripts/smoke-paper-addon-lifecycle.ps1` porneste izolat core-only, core+addon si core dupa eliminarea addonului, apoi blocheaza classloading invalid, shutdown fortat si resurse gestionate ramase.

## Criterii de inchidere

- inchis: niciun accessor public nu contine placeholder de implementare lipsa;
- inchis: toate cele 12 interfete top-level sunt implementate, puncte de extensie sau contract rezervat;
- inchis: dependintele si ordinea au o singura semantica documentata si testata;
- inchis: addonul Kotlin real si fixture-ul Java acopera consumul descriptorului si lifecycle-ul registry;
- inchis: referinta runtime, matricea providerilor si acest roadmap folosesc aceeasi clasificare verificata de test.

## Legaturi

- `reference/documentatie-api.md`
- `reference/api-versioning-and-abi.md`
- `architecture/harta-clase-addons.md`
- `reference/kotlin-interop-api-addonuri.md`
- `architecture/refactorizare-si-impartire-pe-module.md`
