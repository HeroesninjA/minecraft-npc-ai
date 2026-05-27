# Audit Conformitate cu Constitutia Proiectului

Actualizat: 2026-05-27

Importanta: ridicata. Acest document compara codul curent cu `constitutie-proiect.md` si listeaza zonele care nu respecta complet regulile constitutionale sau care sunt doar partial conforme.

## Scop

Auditul verifica in special:

- core neutru si compatibil cu orice addon valid;
- continut demo/fallback dezactivabil;
- tipuri de addon: scenariu, story, resursa/textura, datapack;
- feature flags pentru caracteristici majore;
- storage SQLite/MySQL/HikariCP;
- AI ca sistem asistiv, nu autoritate finala;
- generare harti/structuri ca plan validat, nu executie directa necontrolata.

Acesta este un audit static peste fisierele sursa si resurse. Nu inlocuieste testele, smoke testul pe Paper sau auditul runtime.

## Rezumat executiv

Proiectul respecta partial constitutia: exista config pentru multe sisteme (`simulation.enabled`, `routine.enabled`, `world_admin.enabled`, `ai.orchestration.enabled`, `addons.disabled`), addonul medieval separat are config propriu si exista validare de metadata pentru feature packs.

Neconformitatile principale sunt:

1. `ainpc-core-plugin` inca livreaza si auto-copiaza pack-uri tematice (`medieval`, `modern`, `social`) din core.
2. Core-ul are fallback medieval hardcodat in cod, nu doar in continut demo configurabil.
3. Nu exista un switch constitutional clar de tip `demo.enabled=false` care sa opreasca tot demo-ul core.
4. API-ul de addon nu contine tipurile constitutionale noi: story, resursa/textura, datapack.
5. Addonul medieval separat este inregistrat ca `FEATURE`, desi functional este scenariu.
6. `database.type` exista in config, dar codul initializeaza doar SQLite direct prin `DriverManager`; nu exista MySQL/HikariCP.
7. GUI/story/quest nu au inca feature flags globale echivalente cu regula "orice caracteristica majora trebuie sa poata fi dezactivata".

## Conformitati Confirmate

| Regula constitutionala | Stare in cod | Dovezi |
|---|---|---|
| Addon registry dezactivabil | Conform partial | `ainpc-core-plugin/src/main/resources/config.yml` are `addons.enabled`, `addons.disabled`, `addons.strict_validation`; `AddonRegistry.isAddonEnabled` respinge addonuri dezactivate |
| Feature packs cu metadata | Conform partial | `FeaturePackMetadataValidator` valideaza `addon.type`, `runtime_modes`, `capabilities`, `dependencies` |
| Addon medieval separat | Conform partial | `ainpc-scenario-medieval` are plugin separat, config template si pack instalat in `packs/addons/ainpc-scenario-medieval` |
| AI orchestration dezactivata implicit | Conform | `config.yml` are `ai.orchestration.enabled: false` |
| Rutina si simulare dezactivabile | Conform partial | `config.yml` are `simulation.enabled` si `routine.enabled`; schedulerul verifica aceste flag-uri |
| World admin dezactivabil | Conform partial | `config.yml` are `world_admin.enabled`; debugdump raporteaza disabled cand service-ul lipseste/dezactivat |
| Generare/patch planner controlat | Conform partial | patch planner si mapping sunt orientate pe analyze/plan/validate; nu apare executie directa larga in auditul static |

## Neconformitati si Riscuri

### C-001: Core-ul livreaza pack-uri tematice in `ainpc-core-plugin`

Severitate: ridicata  
Reguli afectate: Core neutru; continut demo dezactivabil; addonurile ofera tema si poveste.

Dovezi:

- `ainpc-core-plugin/src/main/resources/packs/medieval.yml` contine `id: medieval`, `addon.type: scenario`, `primary_scenario: true` si continut medieval.
- `ainpc-core-plugin/src/main/resources/packs/modern.yml` contine pachet de scenariu modern/post-apocaliptic.
- `ainpc-core-plugin/src/main/resources/packs/social.yml` contine pachet de trasaturi sociale.
- `FeaturePackLoader.saveDefaultPacks()` copiaza neconditionat `packs/medieval.yml`, `packs/modern.yml`, `packs/social.yml` in folderul de date.

Problema:

Constitutia spune ca `core` trebuie sa ofere infrastructura, iar addonurile continutul. Pack-urile tematice din core fac ca `ainpc-core-plugin` sa livreze implicit continut de scenariu, inclusiv un primary scenario medieval.

Recomandare:

- Mutare `medieval.yml`, `modern.yml`, `social.yml` in addonuri separate sau intr-un folder de demo explicit.
- Introducere config:

```yaml
demo:
  enabled: false
  install_core_packs: false
feature_packs:
  install_defaults: false
  disabled: []
```

- Daca pack-urile raman temporar in core, toate ID-urile sa fie namespaced: `core_demo:medieval`, `core_demo:modern`, `core_demo:social`.

### C-002: Fallback medieval hardcodat in cod

Severitate: ridicata  
Reguli afectate: Core neutru; demo/fallback dezactivabil.

Dovezi:

- `FeaturePackLoader.loadAllPacks()` apeleaza `FeaturePackDefaults.loadDefaultMedievalPack(...)` cand `loadedPacks` este gol.
- `FeaturePackDefaults.loadDefaultMedievalPack` creeaza direct `FeaturePack("medieval", "Medieval", ...)`, traits si profesii ca `blacksmith`, `farmer`, `guard`, cu texte medievale.

Problema:

Acesta nu este doar continut YAML, ci continut tematic compilat in core. Daca adminul sterge pack-urile sau dezactiveaza addonurile, core-ul poate reveni tot la fallback medieval.

Recomandare:

- Inlocuire cu fallback neutru minim: `core_minimal`, `generic_worker`, `generic_guard`, `generic_social`.
- Adaugare `demo.enabled` si `feature_packs.allow_builtin_fallbacks`.
- Fallback-ul medieval sa fie mutat in addonul medieval sau intr-un addon demo separat.

### C-003: Nu exista un switch unic pentru oprirea demo-ului core

Severitate: medie-ridicata  
Reguli afectate: Dezactivare completa; continut demo dezactivabil cand se activeaza alt addon.

Dovezi:

- `quests.yml` are `simple_for_all_npcs: true`.
- `FeaturePackLoader.saveDefaultPacks()` instaleaza pack-uri default fara sa citeasca `demo.enabled`.
- `AINPCPlugin.loadQuestConfig()` salveaza `quests.yml` daca lipseste.
- Configul are `addons.disabled`, dar nu exista `demo.enabled`, `core_demo.enabled` sau `feature_packs.install_defaults`.

Problema:

Exista mecanisme individuale, dar nu exista o regula operationala unica pentru "core demo off". Cand se introduce alt addon, adminul trebuie sa ghiceasca ce pack-uri, quest fallback-uri si demo mapping trebuie dezactivate.

Recomandare:

- Introducere `demo.enabled=false` ca switch global.
- Legare `simple_for_all_npcs`, default packs si demo mapping de acest switch.
- Cand un addon `SCENARIO` activ este primary, core demo trebuie sa se opreasca automat sau sa afiseze avertizare explicita.

### C-004: Tipurile constitutionale de addon lipsesc din API

Severitate: ridicata  
Reguli afectate: tipuri addon scenariu, story, resursa/textura, datapack.

Dovezi:

- `AddonType` contine doar `CORE`, `SCENARIO`, `FEATURE`, `INTEGRATION`.
- `FeaturePackMetadataValidator.isKnownAddonType` accepta doar valorile din `AddonType`.

Problema:

Constitutia cere tipuri planificate: scenariu, story, resursa/textura si compatibilitate datapack. Codul nu poate declara sau valida in mod nativ `story`, `resource`, `texture`, `resource_texture`, `datapack`.

Recomandare:

- Extindere `AddonType` cu:

```kotlin
STORY("story", "Story addon")
RESOURCE("resource", "Addon de resurse")
TEXTURE("texture", "Addon de textura")
DATAPACK("datapack", "Compatibilitate datapack")
```

- Stabilire aliases pentru `resource_texture`, `resources`, `textures`.
- Actualizare `AddonRegistry` ca fiecare tip sa aiba capabilitati minime validate.

### C-005: Addonul medieval separat este declarat ca `FEATURE`, nu `SCENARIO`

Severitate: medie-ridicata  
Reguli afectate: addonurile trebuie sa declare tipul si capabilitatile corect.

Dovezi:

- `MedievalScenarioAddon` foloseste `AddonType.FEATURE`.
- `medieval_quest.yml` declara `addon.type: "scenario"` si `primary_scenario: true`.
- Pluginul se numeste `AINPCScenarioMedieval`, iar descrierea spune ca livreaza scenariul medieval.

Problema:

Registrul vede pluginul addon ca feature, dar pack-ul pe care il instaleaza este scenariu. Aceasta diferenta poate afecta selectie primary scenario, audit, reguli de dezactivare si viitoare compatibilitate cu alte scenarii.

Recomandare:

- Schimbare descriptor addon medieval la `AddonType.SCENARIO`.
- Capabilities minime: `scenarios`, `scenario-pack`, `pack-installer`, `addon-config-template`.
- Adaugare test care confirma ca addonul medieval apare in `getDescriptors(AddonType.SCENARIO)`.

### C-006: `database.type` nu este respectat; lipseste MySQL/HikariCP

Severitate: medie  
Reguli afectate: directia de productie include MySQL cu HikariCP; configurabilitate explicita.

Dovezi:

- `config.yml` are `database.type: "sqlite"`.
- `DatabaseManager.initialize()` ignora `database.type`, incarca `org.sqlite.JDBC` si deschide `jdbc:sqlite:...`.
- `ainpc-core-plugin/build.gradle` include `sqlite-jdbc`, dar nu include MySQL driver sau HikariCP.

Problema:

Configuratia sugereaza un tip de baza de date, dar implementarea suporta efectiv doar SQLite. MySQL/HikariCP este directie viitoare in constitutie, deci acesta este un gap de roadmap, nu neaparat bug runtime.

Recomandare:

- Introducere `StorageProvider`/`DataSourceFactory`.
- Config:

```yaml
database:
  type: "sqlite" # sqlite | mysql
  sqlite:
    filename: "ainpc_data.db"
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "ainpc"
    username: ""
    password_env: "AINPC_MYSQL_PASSWORD"
    pool:
      maximum_pool_size: 10
      connection_timeout_ms: 30000
```

- HikariCP doar pentru MySQL sau pentru orice JDBC pool configurabil.

### C-007: Nu toate caracteristicile majore au feature flags globale

Severitate: medie  
Reguli afectate: orice caracteristica majora trebuie sa poata fi dezactivata.

Dovezi:

- Configul are flags pentru `simulation`, `routine`, `world_admin`, `addons`, `ai.orchestration`.
- Nu exista sectiuni globale `gui.enabled`, `story.enabled`, `quest.enabled`, `progression.enabled`, `dialog.enabled` ca switch-uri complete.
- `AINPCPlugin.onEnable()` initializeaza `scenarioEngine`, `progressionService`, `storyStateService`, `storyContextService`, `guiService`, `mappingWandService` neconditionat.

Problema:

Unele functii pot fi limitate prin permisiuni sau sub-config, dar constitutia cere dezactivare clara pentru caracteristici majore. In starea curenta, un admin nu poate opri complet story/progression/GUI/quest runtime prin config unic.

Recomandare:

- Adaugare `features:` central:

```yaml
features:
  gui: true
  quest: true
  progression: true
  story: true
  mapping: true
  routine: true
  simulation: true
  ai: false
  generation: false
```

- Serviciile pot fi initializate in modul disabled/read-only, dar comenzile si GUI-urile trebuie sa raporteze explicit functia dezactivata.

### C-008: Configul default activeaza sisteme automate cu impact in lume

Severitate: medie  
Reguli afectate: date validate inainte de automatizare; playability inainte de complexitate.

Dovezi:

- `villagers.auto_repopulate.enabled: true`.
- `family.auto_generate: true`.
- `dialog.passive_listen_enabled: true`.
- `simulation.enabled: true`, `routine.enabled: true`.

Problema:

Aceste defaults pot fi bune pentru demo, dar pentru "core neutru si flexibil" valorile implicite ar trebui sa fie conservative, mai ales cand se instaleaza un addon nou sau cand serverul are date reale.

Recomandare:

- Profiluri explicite: `profile: demo | production | minimal`.
- In `minimal` si `production`, sistemele automate sa fie off pana la configurare.
- In `demo`, ele pot ramane on, dar sub `demo.enabled`.

## Zone Care Par Conforme sau Acceptabile

### AI

AI-ul este in mare parte conform:

- `ai.orchestration.enabled` este false implicit.
- `AIOrchestrationPolicy` pare sa marcheze cazuri ca draft/validation.
- `OpenAIService` are fallback local si diagnostice configurabile.

Riscul ramas este operational: `OpenAIService` se initializeaza mereu si ruleaza diagnostice async la startup/reload. Configul `openai.diagnostics.check_on_startup: false` reduce riscul, dar o politica `features.ai=false` ar face intentia mai clara.

### Generare harti si structuri

Auditul static nu a gasit executie larga necontrolata de world generation. Zonele de `patch`, `scan`, `mapping` par orientate pe analiza, draft si plan. Ramane necesar un audit separat cand se introduce generare reala de structuri.

### Addon medieval separat

Addonul medieval separat este o directie buna:

- are config propriu;
- poate fi dezactivat prin `addons.disabled` sau `addon.enabled`;
- instaleaza pack-ul in folder gestionat;
- curata pack-ul gestionat la disable.

Problema ramane ca exista in paralel continut medieval si in core.

## Plan Recomandat de Remediere

### Faza 1: Neutralizare core demo

1. Adauga `demo.enabled` si `feature_packs.install_defaults`.
2. Opreste `saveDefaultPacks()` cand demo/default packs sunt disabled.
3. Mutare `medieval.yml`, `modern.yml`, `social.yml` in addonuri sau `packs/demo/`.
4. Inlocuire fallback medieval din `FeaturePackDefaults` cu fallback generic neutru.

### Faza 2: API addon constitutional

1. Extinde `AddonType` cu `STORY`, `RESOURCE`, `TEXTURE`, `DATAPACK`.
2. Actualizeaza validatorul si registry-ul pentru capabilitati minime per tip.
3. Schimba `MedievalScenarioAddon` la `AddonType.SCENARIO`.
4. Adauga teste pentru tipuri, aliases, dezactivare si selectie primary scenario.

### Faza 3: Feature flags globale

1. Introduce `features.*` central.
2. Leaga `quest`, `story`, `gui`, `progression`, `mapping`, `generation`, `ai` la initializare/comenzi/GUI.
3. Cand o functie este disabled, comenzile trebuie sa raspunda clar, nu sa cada partial.

### Faza 4: Storage provider

1. Extrage interfata de conexiune din `DatabaseManager`.
2. Respecta `database.type`.
3. Adauga MySQL + HikariCP ca optiune, nu ca dependinta operationala obligatorie.
4. Pastreaza SQLite ca default pentru dev/demo.

## Verdict

Status: partial conform.

Codul are directia corecta pentru addonuri si configurabilitate, dar inca poarta continut tematic in core si fallback medieval hardcodat. Cele mai importante remedieri sunt neutralizarea core-ului, adaugarea unui switch global pentru demo si extinderea API-ului de addon la tipurile constitutionale.

Pana la remediere, `ainpc-core-plugin` trebuie tratat ca "core cu demo incorporat", nu ca "core complet neutru".
