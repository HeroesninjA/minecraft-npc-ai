# Documentatie API

Status: referinta canonica pentru suprafata publica verificata.
Actualizat: 2026-07-18.

Acest document descrie ce poate consuma astazi un plugin addon din modulul `ainpc-api`. Codul ramane autoritatea finala.

## Punct de intrare

- addonul Paper declara `depend: [AINPCPlugin]` in `plugin.yml`;
- dupa activarea core-ului, obtine `AINPCPlatformApi` din Bukkit `ServicesManager`;
- addonul compileaza impotriva `ainpc-api`, nu impotriva claselor din `ainpc-core-plugin`;
- `AINPCPlatformApi` este inregistrat de core in faza de startup gestionata de `ServiceRegistry`.

```kotlin
val registration = server.servicesManager.getRegistration(AINPCPlatformApi::class.java)
val platform = registration?.provider
    ?: error("AINPCPlatformApi nu este disponibil")
```

## Suprafata utilizabila

| Contract | Stare runtime | Observatie |
| --- | --- | --- |
| `AddonRegistryApi` | implementat | descriptor, lifecycle tranzactional, cascada dependentilor si selectie scenariu |
| `IntegrationRegistryApi` | implementat | registru pentru integrari cu pluginuri externe |
| `WorldAdminApi` | implementat | citire mapping semantic si mutatii limitate de binding NPC |
| `ReputationApi` | implementat | citire si modificare reputatie |
| `PlayerProgressionApi` | implementat | acces direct la serviciul runtime de progresie al jucatorului |
| `RelationshipApi` | implementat | citire relatii NPC-NPC |
| `NpcEconomyApi` | implementat | acces direct la serviciul runtime de economie NPC |
| `StoryAuthoringApi` | implementat | ID-uri de template, evenimente recente si verificare scoped a drafturilor din coada persistenta `pending` |
| profilele `runtimeMode`, `worldMode`, `defaultStoryMode` | implementate | provin din configuratia platformei |
| `dataDirectory`, `packDirectory`, `getAddonConfigDirectory` | implementate | cai calculate de core |
| `reloadContent()` | implementat | reincarca pack-uri, template-uri si cache-uri dependente |
| `registerObjectiveHandler(...)` | implementat | tip functie pentru Kotlin si overload `ObjectiveProgressHandler` pentru Java |
| lookup NPC si sold jucator | implementat | `getNPCName`, `getNPCProfession`, `getPlayerBalance` |

## Clasificarea exhaustiva a interfetelor

Cele 12 interfete publice top-level din `ainpc-api` au rol explicit:

- provideri runtime core: `AINPCPlatformApi`, `AddonRegistryApi`, `IntegrationRegistryApi`, `WorldAdminApi`, `ReputationApi`, `PlayerProgressionApi`, `RelationshipApi`, `NpcEconomyApi` si `StoryAuthoringApi`;
- puncte de extensie implementate de consumatori: `AINPCAddon` si `ExternalPluginIntegration`;
- contract rezervat: `DependencyResolver`, folosit intern prin `AddonDependencyResolver`, dar neexpus de platforma.

`AINPCPlatformApi.ObjectiveProgressHandler` este interfata SAM imbricata pentru consumatorii Java. `AddonDependencyGraph` si DTO-urile settlement sunt modele de date, nu servicii. `PublicApiClosureContractTest` inventariaza sursele API si esueaza daca apare o interfata top-level neclasificata, un provider nu mai implementeaza contractul, platforma expune accidental resolverul/settlement ori reapar accessori-placeholder.

## Contracte partiale sau neconectate

- `StoryAuthoringApi.hasPendingEvents` interogheaza exact `scopeType`/`scopeId` in `story_pending_events`; istoricul `story_events` nu este prezentat drept pending;
- fatada publica expune numai verificarea booleana; queue, list, publish si discard raman operatii core in `StoryAuthoringService`/`StoryStateService`;
- schedulerul core poate scrie template-uri built-in in pending numai cu `story.random_events_enabled=true` si `story.random_events_require_review=true`; implicit, cheia de review este `false`, iar publicarea directa ramane compatibila;
- review-ul operational foloseste `/ainpc story pending`, `/ainpc story publish` si `/ainpc story discard`; aceste comenzi admin nu extind `StoryAuthoringApi` public;
- fluxul AI nu scrie pending deoarece raspunsul `STORY_DRAFT` este inca text liber, nu un DTO validat;
- `DependencyResolver` si `AddonDependencyGraph` sunt contracte publice rezervate pentru analiza de graf, fara provider in `AINPCPlatformApi`; addonurile declara metadata, iar `AddonRegistry` detine validarea runtime;
- DTO-urile de settlement sunt contracte de date; agregatul `SettlementPlan` este `@Deprecated(WARNING)`, pastrat pentru compatibilitate si fara `ReplaceWith`, iar existenta celorlalte tipuri nu implica un executor public complet;
- generarea AI a drafturilor story si un eventual executor settlement raman backlog de domeniu in `planning/generare-ai-si-constructie-automata.md` si `planning/generare-sate-worldedit-si-npc.md`; nu sunt goluri de provider ascunse in API;
- stabilizarea providerilor din `planning/stabilizare-api-si-addonuri.md` este inchisa, iar lipsa providerului `DependencyResolver` ramane deliberata.

## API de addon

- `AINPCAddon` defineste `getDescriptor`, `onLoad`, `onEnable`, `onDisable` si callback-uri optionale;
- `AddonDescriptor` declara originea, ID-ul, versiunea, tipul, runtime modes, capabilitatile si dependintele;
- `AddonDescriptor` expune constructori Java prin `@JvmOverloads`; forma cu 4 argumente foloseste implicit tipul `FEATURE`;
- cu validare stricta, `AddonRegistryApi.registerAddon` verifica mai intai graful candidatului, apoi ruleaza `onLoad`, publica descriptorul si ruleaza `onEnable`; la esec curata instanta partiala si restaureaza inregistrarea anterioara;
- reinregistrarea aceleiasi instante este idempotenta, iar inlocuirea unei alte instante cu acelasi ID ruleaza lifecycle-ul complet;
- cu validare stricta, `unregisterAddon` dezactiveaza mai intai dependentii de cod tranzitivi in ordine inversa dependintelor, apoi tinta; continua cleanup-ul dupa erori si propaga prima exceptie cu celelalte suppressed;
- inlocuirea aceluiasi ID nu cascadeaza dependentii, iar reload-ul feature pack reconciliaza dependintele numai dupa reinregistrarea descriptorilor;
- `removeByOrigin` dezactiveaza instantele de cod, iar shutdown-ul continua best-effort dupa exceptii normale;
- dispatch-ul callback-urilor `AINPCAddon` izoleaza si logheaza esecul fiecarui addon, continua cu urmatorul ID si tolereaza dezregistrarea reentranta;
- `onNpcStateChange` este produs de tranzitiile runtime `AINPC`; starile sunt numele enum stabile, iar rehidratarea persistenta nu produce notificari;
- un candidat de cod a carui inchidere tranzitiva contine dependinte lipsa ori cicluri, sau care introduce un conflict de scenariu primar, este respins inainte de lifecycle, fara sa inlocuiasca instanta anterioara;
- `registerDescriptor` ramane declarativ: pastreaza descriptorii cu probleme de graf, le emite ca warning si mentine selectia determinista; `addons.load_order` ordoneaza descriptorii, nu pluginurile Paper.

## API de evenimente

Evenimentele Bukkit publice din `ro.ainpc.api.events` sunt separate de callback-urile `AINPCAddon`. Lista producerilor si golurile de publicare sunt in `reference/api-events-listeners-triggers.md`.

## Reguli de consum

- foloseste numai tipuri din `ainpc-api` si API-uri Paper/Bukkit publice;
- nu face cast la `AINPCPlatform` si nu importa servicii core;
- obtine calea de configurare cu `getAddonConfigDirectory`, fara hardcode;
- trateaza `reloadContent()` ca operatie globala si apeleaz-o numai dupa o schimbare reala de pack;
- nu presupune ca orice interfata aflata in JAR are si provider runtime;
- respecta `apiVersion` si politica SemVer; baseline-ul ABI blocheaza diferentele nerevizuite, dar o versiune majora noua poate rupe consumatorii compilati.

## Surse in cod

- `ainpc-api/src/main/kotlin/ro/ainpc/api/AINPCPlatformApi.kt`
- `ainpc-api/src/main/kotlin/ro/ainpc/api/AddonRegistryApi.kt`
- `ainpc-api/src/main/kotlin/ro/ainpc/addons/`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/platform/AINPCPlatform.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/ServiceRegistry.kt`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/platform/PublicApiClosureContractTest.kt`

## Legaturi

- `reference/addon-developer-guide.md`
- `reference/api-versioning-and-abi.md`
- `architecture/harta-clase-addons.md`
- `reference/kotlin-interop-api-addonuri.md`
- `reference/api-events-listeners-triggers.md`
- `planning/stabilizare-api-si-addonuri.md`
