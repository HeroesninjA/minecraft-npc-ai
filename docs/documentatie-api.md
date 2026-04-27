# Documentatie API

Actualizat: 2026-04-26

## Scop

Acest document descrie contractul public curent al modulului `ainpc-api` si modul in care ar trebui folosit de addonuri sau pluginuri externe.

Scopul lui nu este sa descrie toate clasele din core, ci doar suprafata publica pe care alte module ar trebui sa se bazeze.

## Context

Proiectul are deja separarea:

- `ainpc-api`
- `ainpc-core-plugin`
- `ainpc-scenario-medieval`

In acest model:

- `ainpc-api` trebuie tratat ca punct oficial de integrare
- `ainpc-core-plugin` ramane implementarea
- addonurile trebuie sa depinda de API, nu de clase interne din core

## Statusul actual al API-ului

API-ul exista si este deja utilizabil pentru:

- descoperirea platformei prin Bukkit Services
- citirea profilului de runtime
- acces la registry-ul de addonuri
- acces la world admin la nivel de baza
- cererea unui `reloadContent()`

API-ul actual este util, dar inca mic.

Ce inseamna asta:

- este bun pentru faza initiala a addonurilor
- nu este inca suficient pentru scenarii complet extensibile
- va trebui extins cand apar registrii de actiuni, conditii si trigger-e

## Modulul public

Modulul public este `ainpc-api`.

Rolul lui este:

- sa defineasca interfetele stabile
- sa defineasca enum-urile de runtime si lume
- sa defineasca descriptorii si lifecycle-ul addonurilor

Acest modul ar trebui sa poata fi adaugat ca dependinta de orice addon AINPC fara a trage dupa el tot core-ul.

## Punctul principal de intrare

Intrarea publica in platforma este:

- `ro.ainpc.api.AINPCPlatformApi`

Aceasta interfata expune:

- `getRuntimeMode()`
- `getWorldMode()`
- `getDefaultStoryMode()`
- `getAddonRegistry()`
- `getWorldAdmin()`
- `getDataDirectory()`
- `getPackDirectory()`
- `reloadContent()`

## Ce inseamna fiecare metoda

### `getRuntimeMode()`

Returneaza profilul de runtime al platformei.

Util pentru:

- activare conditionala a unei capabilitati
- validarea dependintelor unui addon
- adaptarea comportamentului daca exista sau nu servicii externe

### `getWorldMode()`

Spune ce model de lume foloseste platforma:

- fixa
- semi-dinamica
- dinamica deschisa

Util pentru:

- scenarii dependente de tipul lumii
- limitarea unor addonuri la anumite moduri

### `getDefaultStoryMode()`

Spune modul narativ de baza al lumii.

Util pentru:

- selectie de scenarii
- comportamente tematice
- decizii de pacing narativ

### `getAddonRegistry()`

Ofera acces la registrul public de addonuri.

Aici se fac:

- inregistrarea addonului
- listarea addonurilor
- gasirea scenariului principal

### `getWorldAdmin()`

Ofera acces la contractul public pentru world admin.

Atentie:
- in prezent API-ul public de world admin este minimal
- ofera date agregate, nu acces complet la modelul intern

### `getDataDirectory()`

Returneaza directorul de date al pluginului core.

Util pentru:

- logica de instalare addon
- fisiere auxiliare
- artefacte generate

### `getPackDirectory()`

Returneaza directorul in care core-ul asteapta pack-uri.

Acesta este punctul corect pentru addonuri care instaleaza:

- pack-uri YAML
- resurse gestionate

### `reloadContent()`

Cere core-ului sa reincarce continutul.

Util pentru:

- addonuri care adauga sau elimina pack-uri
- dezvoltare
- sincronizare dupa schimbari controlate

Atentie:
- nu ar trebui apelat abuziv
- trebuie folosit doar dupa modificari reale de continut

## Cum este expus API-ul

Core-ul publica `AINPCPlatformApi` ca serviciu Bukkit.

Asta inseamna ca un plugin extern il poate obtine fara sa cunoasca implementarea concreta.

Modelul corect este:

```java
RegisteredServiceProvider<AINPCPlatformApi> provider = getServer()
    .getServicesManager()
    .getRegistration(AINPCPlatformApi.class);

AINPCPlatformApi platform = provider != null ? provider.getProvider() : null;
```

Acesta este si modelul folosit deja de addonul medieval separat.

## Exemplu minim de consum

```java
public final class MyAddonPlugin extends JavaPlugin {

    private AINPCPlatformApi platform;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<AINPCPlatformApi> provider = getServer()
            .getServicesManager()
            .getRegistration(AINPCPlatformApi.class);

        platform = provider != null ? provider.getProvider() : null;
        if (platform == null) {
            getLogger().severe("AINPC API nu este disponibil.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Runtime mode: " + platform.getRuntimeMode().getId());
        getLogger().info("Pack directory: " + platform.getPackDirectory());
    }
}
```

## Sistemul de addonuri

Addonurile sunt descrise prin:

- `AINPCAddon`
- `AddonDescriptor`
- `AddonRegistryApi`
- `AddonType`

## `AINPCAddon`

Interfata addonului este simpla:

- `getDescriptor()`
- `onLoad(AINPCPlatformApi api)`
- `onEnable(AINPCPlatformApi api)`
- `onDisable(AINPCPlatformApi api)`

### Rolul lifecycle-ului

`onLoad(...)`
- pregatire logica
- citire context
- initializare usoara

`onEnable(...)`
- activare efectiva
- inregistrare de capabilitati
- instalare de continut

`onDisable(...)`
- cleanup
- stergere de artefacte gestionate
- dezactivare controlata

## `AddonDescriptor`

Descriptorul este contractul public de identitate si capabilitati pentru un addon.

Campuri importante:

- `origin`
- `id`
- `name`
- `version`
- `description`
- `type`
- `primaryScenario`
- `supportedRuntimeModes`
- `capabilities`
- `dependencies`

### `origin`

Valori curente:

- `core`
- `feature-pack`
- `plugin-addon`

### `type`

Valorile principale sunt:

- `CORE`
- `SCENARIO`
- `FEATURE`
- `INTEGRATION`

### `primaryScenario`

Marcheaza scenariul principal al unei instalari.

Este util pentru:

- selectie implicita
- admin UI
- fallback de continut

### `supportedRuntimeModes`

Spune in ce moduri de runtime poate functiona addonul.

### `capabilities`

Lista libera de capabilitati declarate.

Exemple de capabilitati bune:

- `scenario-pack`
- `pack-installer`
- `traits`
- `dialogues`
- `topology`

Pe viitor, aici pot intra si:

- `scenario-actions`
- `scenario-conditions`
- `world-place-support`

### `dependencies`

Lista addonurilor sau capabilitatilor necesare.

Acest camp trebuie folosit pentru:

- detectie de configuratii incomplete
- validare la incarcare
- documentarea ecosistemului modular

## `AddonRegistryApi`

Acest registry este punctul public pentru managementul addonurilor.

Metode:

- `registerDescriptor(AddonDescriptor descriptor)`
- `registerAddon(AINPCAddon addon)`
- `unregisterAddon(String addonId)`
- `removeByOrigin(String origin)`
- `getDescriptors()`
- `getDescriptors(AddonType type)`
- `getDescriptor(String id)`
- `getPrimaryScenario()`
- `size()`

### Recomandari de folosire

`registerAddon(...)`
- cand ai un addon real cu lifecycle

`registerDescriptor(...)`
- cand vrei sa publici doar metadate de continut

`unregisterAddon(...)`
- la shutdown sau cleanup controlat

`removeByOrigin(...)`
- util pentru curatarea descriptorilor generati dintr-o sursa specifica

`getPrimaryScenario()`
- util pentru interfete admin si bootstrapping de continut

## World admin API

Interfata publica actuala este:

- `isEnabled()`
- `getWorldMode()`
- `getRegionCount()`
- `getNodeCount()`

Aceasta suprafata este intentionat mica.

Ce inseamna asta:

- addonurile pot afla daca world admin este activ
- pot face validari simple
- pot afisa statistici
- nu se pot baza inca pe acces semantic complet la regiuni, locuri si noduri

Pe viitor, daca vrei addonuri de scenariu serioase, `WorldAdminApi` va trebui extins.

## Enum-uri publice

## `RuntimeMode`

Valori:

- `STANDALONE`
- `HYBRID`
- `ADVANCED`

Semnificatie:

- `STANDALONE` = fara servicii externe obligatorii
- `HYBRID` = servicii externe optionale
- `ADVANCED` = servicii externe si sincronizare dedicate

Metode utile:

- `usesExternalAi()`
- `usesExternalDatabase()`
- `usesDistributedSync()`
- `fromId(...)`
- `fromIds(...)`

## `WorldMode`

Valori:

- `STATIC`
- `FINITE_DYNAMIC`
- `OPEN_DYNAMIC`

Folosire:

- adaptarea scenariilor la tipul lumii
- limitarea unor addonuri la anumite medii

## `StoryMode`

Valori:

- `STATIC`
- `EVOLUTIVE`
- `ROTATIVE`

Folosire:

- alegerea stilului narativ
- filtrare de scenarii
- control asupra ritmului de evolutie

## Ce este sigur sa foloseasca un addon

Un addon ar trebui sa se bazeze doar pe:

- clase din `ainpc-api`
- serviciul `AINPCPlatformApi`
- descriptorii si registry-ul public
- directoarele returnate de API

## Ce nu ar trebui folosit direct

Un addon nu ar trebui sa depinda de:

- `AINPCPlugin`
- `ScenarioEngine`
- `NPCManager`
- `WorldAdminService`
- orice clasa din core care nu este expusa in `ainpc-api`

Motivul este simplu:

- acele clase se pot schimba mult mai des
- creeaza coupling puternic
- rup modularitatea reala

## Contracte lipsa in acest moment

Pentru faza actuala, API-ul este suficient doar partial.

Pentru faza 2 si 3 vor trebui adaugate contracte noi, de exemplu:

- `ScenarioActionHandler`
- `ScenarioConditionHandler`
- `ScenarioTriggerProvider`
- `ScenarioExecutionContext`
- `ScenarioVariableProvider`
- `ScenarioValidationReport`
- `Place`-level world API

Fara acestea:

- addonurile pot instala continut
- dar nu pot extinde curat runtime-ul de scenarii

## Reguli de compatibilitate recomandate

Cand API-ul incepe sa fie folosit extern, trebuie introduse reguli clare:

- versiune de API
- capabilitati declarate
- dependinte minime
- deprecari explicite

Model recomandat:

- schimbarile breaking intra doar intr-o versiune noua de API
- contractele vechi raman o perioada marcate ca deprecated
- addonurile verifica versiunea minima suportata

## Recomandari pentru autorii de addonuri

- foloseste `AINPCPlatformApi` ca singur punct de intrare
- trateaza `reloadContent()` ca operatie scumpa
- declara corect `capabilities` si `dependencies`
- nu scrie direct in fisierele interne ale core-ului in afara directoarelor destinate pack-urilor
- nu consuma internals din core doar pentru ca sunt la indemana

## MVP bun pentru documentatia API

Pentru stadiul actual al proiectului, aceasta documentatie este suficienta daca este completata cu:

1. un exemplu de addon minimal
2. o conventie pentru `capabilities`
3. o conventie pentru `dependencies`
4. reguli de versionare API
5. ulterior, documentatie pentru registrii de scenarii

## Concluzie

API-ul actual este o baza buna pentru inceputul fazei de modularitate.

El permite deja:

- descoperirea platformei
- instalarea si descrierea addonurilor
- sincronizarea pack-urilor
- consultarea profilului de runtime si lume

Dar pentru un ecosistem real de scenarii extensibile, API-ul trebuie extins mai ales in zona:

- runtime de scenarii
- validare
- world semantics
- contracte pentru extensii programabile
