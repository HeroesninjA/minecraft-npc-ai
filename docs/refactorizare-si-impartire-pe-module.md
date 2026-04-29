# Refactorizare si Impartire pe Module

Actualizat: 2026-04-28

## Scop

Acest document defineste cum trebuie facuta refactorizarea proiectului astfel incat:

- codul legacy din `src/src` sa fie mutat in modulele Maven reale
- responsabilitatile sa fie separate clar intre API, core si addonuri
- clasele mari din core sa fie sparte in componente mici si testabile
- scenariile si continutul sa poata evolua fara modificari frecvente in nucleu

Documentul este orientat pe structura reala existenta in repo, nu pe o arhitectura teoretica noua.

## Status implementare

Faza 1 a fost aplicata pe 2026-04-28.

Faza 2 a fost inceputa pe 2026-04-28.

Schimbari aplicate:

- sursele Java core au fost copiate in `ainpc-core-plugin/src/main/java`
- resursele core au fost copiate in `ainpc-core-plugin/src/main/resources`
- `ainpc-core-plugin/pom.xml` nu mai foloseste `../src/src/main/java`
- `ainpc-core-plugin/pom.xml` nu mai foloseste `../src/src/main/resources`
- folderul legacy `src/src` a fost eliminat dupa migrare
- task-urile programate au fost extrase din `AINPCPlugin` in `ro.ainpc.bootstrap.SchedulerCoordinator`
- `scripts/debug-openai.ps1` citeste implicit config-ul din noua locatie
- `mvn clean test` trece dupa schimbare
- `mvn package -DskipTests` trece dupa schimbare

Ramas pentru fazele urmatoare:

- continuarea subtierii lui `AINPCPlugin` prin extragerea bootstrap-ului si reload-ului
- spargerea lui `ScenarioEngine`
- spargerea lui `FeaturePackLoader`
- spargerea lui `NPCManager`
- eliminarea mentiunilor istorice despre `src/src` din documentele care descriu stari vechi, daca mai incurca navigarea

## Starea actuala

Structura Maven exista deja:

- `ainpc-api`
- `ainpc-core-plugin`
- `ainpc-scenario-medieval`

Problema initiala era ca `ainpc-core-plugin` nu detinea propriile surse de productie. Inainte de Faza 1, `ainpc-core-plugin/pom.xml` folosea `../src/src/main/java` si `../src/src/main/resources`.

Status dupa Faza 1:

- `ainpc-core-plugin` compileaza codul din propriul `src/main/java`
- `ainpc-core-plugin` incarca resursele din propriul `src/main/resources`
- folderul legacy `src/src` a fost eliminat
- modularizarea fizica a modulului core este finalizata la nivel de layout Maven

Suprafata actuala a codului arata unde sunt punctele de risc:

- `engine` are aproximativ `5240` linii
- `managers` are aproximativ `2681` linii
- `npc` are aproximativ `2071` linii
- `commands` are aproximativ `1716` linii
- `ai` are aproximativ `1690` linii

Clasele cele mai mari sunt:

- `engine/ScenarioEngine.java` aproximativ `2907` linii
- `managers/NPCManager.java` aproximativ `1720` linii
- `commands/AINPCCommand.java` aproximativ `1438` linii
- `engine/FeaturePackLoader.java` aproximativ `1119` linii
- `ai/OpenAIService.java` aproximativ `1058` linii
- `world/WorldAdminService.java` aproximativ `753` linii

Concluzia este simpla: prima nevoie nu este inca un modul nou, ci finalizarea modularizarii existente si spargerea claselor de orchestrare care au devenit prea mari.

## Probleme structurale care trebuie rezolvate

### 1. Modulul core nu isi contine codul

Inainte de Faza 1, faptul ca `ainpc-core-plugin` compila din `../src/src` facea repo-ul greu de inteles si greu de refactorizat.

### 2. `AINPCPlugin` este prea incarcat

`AINPCPlugin` face bootstrap, wiring, lifecycle, config reload, scheduled tasks si expune o lista lunga de dependinte. Clasa trebuie sa ramana punctul de intrare Paper, nu containerul tuturor responsabilitatilor.

### 3. `ScenarioEngine` este o god class

`ScenarioEngine` combina:

- template loading
- addon overrides
- quest runtime
- persistenta progresului
- validare implicita
- interactiuni NPC-jucator
- lifecycle de scenariu

Aceasta este principala tinta de refactorizare.

### 4. `FeaturePackLoader` si `NPCManager` au responsabilitati prea late

Aceste clase au crescut ca puncte centrale pentru prea multe decizii si prea multe efecte laterale.

### 5. Limitele dintre API si internals nu sunt inca destul de ferme

`ainpc-api` exista si este directia buna, dar trebuie pastrat strict ca strat de contracte, DTO-uri si extensie stabila. Addonurile trebuie sa depinda de API, nu de clase interne din core.

## Principii de refactorizare

Regulile de mai jos sunt obligatorii pentru toate mutarile si extragerile:

- Nu se schimba comportamentul public in aceeasi faza in care se muta fisierele.
- Intai se muta fizic codul in modulele corecte, apoi se sparg clasele.
- `ainpc-api` nu depinde de Bukkit intern, de `AINPCPlugin` sau de managerii concreti.
- `ainpc-core-plugin` poate depinde de `ainpc-api`, niciodata invers.
- `ainpc-scenario-*` depind doar de `ainpc-api` si de Paper API.
- Fiecare componenta noua trebuie sa aiba un singur rol clar.
- Orice clasa care depaseste frecvent 300-500 linii trebuie evaluata pentru split.
- Persistenta, parsing-ul si runtime-ul nu se amesteca in aceeasi clasa.

## Impartirea recomandata pe module

### Modul 1: `ainpc-api`

Rol:

- contract public stabil
- descriptori si capabilitati pentru addonuri
- DTO-uri pentru world admin si scenarii
- interfete pentru registri si servicii publice

Ce trebuie sa ramana aici:

- `AINPCPlatformApi`
- `AddonRegistryApi`
- `WorldAdminApi`
- `AddonDescriptor`
- `AddonType`
- `AINPCAddon`
- modelele info de tip `WorldRegionInfo`, `WorldPlaceInfo`, `WorldNodeInfo`
- viitoarele contracte de scenarii: `ScenarioActionHandler`, `ScenarioConditionHandler`, `ScenarioTriggerHandler`, `ScenarioExecutionContext`, `ScenarioValidationReport`

Ce nu trebuie sa ajunga aici:

- `AINPCPlugin`
- clase Bukkit concrete
- acces direct la config intern
- manageri concreti
- cod de persistenta

### Modul 2: `ainpc-core-plugin`

Rol:

- pluginul Paper principal
- bootstrap si lifecycle
- manageri si servicii interne
- runtime de scenarii
- persistenta
- incarcare pack-uri
- integrarea dintre AI, NPC, world admin si questuri

Acest modul a primit efectiv sursele din legacy `src/src/main/java` si `src/src/main/resources` in Faza 1.

### Modul 3: `ainpc-scenario-medieval`

Rol:

- addon de continut
- inregistrare descriptor si capabilitati
- instalare pack propriu
- eventuala logica specifica medievala, dar doar prin API

### Module viitoare

Nu se recomanda adaugarea imediata de module Maven noi doar pentru a obtine o structura aparent mai curata.

Mai intai trebuie finalizate cele 3 module existente. Dupa aceea pot fi adaugate doar daca apare o nevoie reala:

- `ainpc-scenario-social`
- `ainpc-scenario-modern`
- `ainpc-integration-betonquest`
- `ainpc-integration-economy`

## Impartirea recomandata in interiorul `ainpc-core-plugin`

In afara modulelor Maven, core-ul trebuie impartit intern pe pachete mai clare.

Structura tinta recomandata:

```text
ainpc-core-plugin/
  src/main/java/ro/ainpc/core/bootstrap/
  src/main/java/ro/ainpc/core/config/
  src/main/java/ro/ainpc/core/addons/
  src/main/java/ro/ainpc/core/platform/
  src/main/java/ro/ainpc/core/persistence/
  src/main/java/ro/ainpc/core/world/
  src/main/java/ro/ainpc/core/npc/
  src/main/java/ro/ainpc/core/memory/
  src/main/java/ro/ainpc/core/dialog/
  src/main/java/ro/ainpc/core/ai/
  src/main/java/ro/ainpc/core/scenario/
  src/main/java/ro/ainpc/core/scenario/model/
  src/main/java/ro/ainpc/core/scenario/parser/
  src/main/java/ro/ainpc/core/scenario/runtime/
  src/main/java/ro/ainpc/core/scenario/persistence/
  src/main/java/ro/ainpc/core/scenario/quest/
  src/main/java/ro/ainpc/core/commands/
  src/main/java/ro/ainpc/core/listeners/
  src/main/java/ro/ainpc/core/util/
```

Observatie importanta:

- `core` aici descrie organizarea interna a pluginului, nu un al patrulea modul Maven.

## Maparea codului existent catre structura tinta

### Bootstrap si lifecycle

Cod actual:

- `AINPCPlugin`

Tinta:

- `ro.ainpc.core.bootstrap.AINPCPlugin`
- `ro.ainpc.core.bootstrap.PluginBootstrap`
- `ro.ainpc.core.bootstrap.ServiceContainer`
- `ro.ainpc.core.bootstrap.SchedulerCoordinator`
- `ro.ainpc.core.bootstrap.ReloadCoordinator`

Regula:

- `AINPCPlugin` trebuie sa ramana subtire: `onEnable`, `onDisable`, delegare catre servicii.

### Platforma si addonuri

Cod actual:

- `platform/AINPCPlatform`
- `platform/PlatformProfile`
- `addons/AddonRegistry`

Tinta:

- `ro.ainpc.core.platform.AINPCPlatform`
- `ro.ainpc.core.platform.PlatformProfile`
- `ro.ainpc.core.addons.AddonRegistry`
- `ro.ainpc.core.addons.AddonCapabilityValidator`
- `ro.ainpc.core.addons.AddonLifecycleCoordinator`

### Persistenta

Cod actual:

- `database/DatabaseManager`

Tinta:

- `ro.ainpc.core.persistence.DatabaseManager`
- `ro.ainpc.core.persistence.DatabaseExecutor`
- `ro.ainpc.core.persistence.SqliteConnectionFactory`
- `ro.ainpc.core.persistence.migrations.*`

Regula:

- executia SQL, schema setup si task-urile async nu trebuie sa stea toate in aceeasi clasa.

### World admin

Cod actual:

- pachetul `world`

Tinta:

- `ro.ainpc.core.world.service.WorldAdminService`
- `ro.ainpc.core.world.model.*`
- `ro.ainpc.core.world.config.WorldConfigReader`
- `ro.ainpc.core.world.config.WorldConfigWriter`
- `ro.ainpc.core.world.validation.WorldTopologyValidator`

Observatie:

- `WorldAdminService` este deja relativ bine separat si poate fi refactorizat incremental prin extract class.

### NPC, memorie, emotii, familie

Cod actual:

- `managers/NPCManager`
- `managers/MemoryManager`
- `managers/EmotionManager`
- `managers/FamilyManager`
- `managers/ConversationSessionManager`
- `npc/*`

Tinta:

- `ro.ainpc.core.npc.service.NPCManager`
- `ro.ainpc.core.npc.repository.NPCRepository`
- `ro.ainpc.core.npc.lifecycle.NPCSpawnService`
- `ro.ainpc.core.npc.lifecycle.NPCRestoreService`
- `ro.ainpc.core.npc.simulation.NPCLifeSimulationService`
- `ro.ainpc.core.npc.profile.NPCProfileBackfillService`
- `ro.ainpc.core.memory.MemoryManager`
- `ro.ainpc.core.emotion.EmotionManager`
- `ro.ainpc.core.family.FamilyManager`
- `ro.ainpc.core.dialog.session.ConversationSessionManager`
- `ro.ainpc.core.npc.model.*`

Regula:

- `NPCManager` trebuie sa devina orchestrator, nu depozit pentru spawn, restore, sync, save si simulation in aceeasi clasa.

### AI si dialog

Cod actual:

- `ai/DialogManager`
- `ai/OpenAIService`
- `ai/NpcFactResolver`
- `engine/DialogueEngine`
- `engine/DecisionEngine`

Tinta:

- `ro.ainpc.core.ai.OpenAIService`
- `ro.ainpc.core.ai.prompt.*`
- `ro.ainpc.core.ai.history.*`
- `ro.ainpc.core.ai.diagnostics.*`
- `ro.ainpc.core.dialog.DialogManager`
- `ro.ainpc.core.dialog.DialogueEngine`
- `ro.ainpc.core.decision.DecisionEngine`
- `ro.ainpc.core.dialog.fact.NpcFactResolver`

Regula:

- clientul OpenAI, constructia prompturilor, fallback-urile si diagnosticele trebuie separate.

### Scenarii si questuri

Cod actual:

- `engine/ScenarioEngine`
- parti din `engine/FeaturePackLoader`

Tinta:

- `ro.ainpc.core.scenario.ScenarioEngine`
- `ro.ainpc.core.scenario.model.*`
- `ro.ainpc.core.scenario.registry.ScenarioActionRegistry`
- `ro.ainpc.core.scenario.registry.ScenarioConditionRegistry`
- `ro.ainpc.core.scenario.registry.ScenarioTriggerRegistry`
- `ro.ainpc.core.scenario.parser.ScenarioDefinitionParser`
- `ro.ainpc.core.scenario.validation.ScenarioValidator`
- `ro.ainpc.core.scenario.runtime.ScenarioRuntime`
- `ro.ainpc.core.scenario.runtime.ScenarioExecutionContext`
- `ro.ainpc.core.scenario.quest.QuestRuntimeService`
- `ro.ainpc.core.scenario.quest.QuestProgressRepository`
- `ro.ainpc.core.scenario.quest.QuestInteractionService`
- `ro.ainpc.core.scenario.template.ScenarioTemplateRepository`

Regula:

- `ScenarioEngine` trebuie sa ramana doar orchestration layer.
- modelul de quest nu trebuie sa ramana amestecat cu incarcarea template-urilor si cu persistenta.

### Feature packs

Cod actual:

- `engine/FeaturePackLoader`

Tinta:

- `ro.ainpc.core.pack.FeaturePackLoader`
- `ro.ainpc.core.pack.FeaturePackRepository`
- `ro.ainpc.core.pack.FeaturePackParser`
- `ro.ainpc.core.pack.FeaturePackValidator`
- `ro.ainpc.core.pack.install.PackInstallService`

Regula:

- citirea YAML, modelarea datelor, validarea si politica de override trebuie separate.

### Comenzi si listenere

Cod actual:

- `commands/AINPCCommand`
- `commands/AINPCTabCompleter`
- pachetul `listeners`

Tinta:

- `ro.ainpc.core.commands.AINPCCommand`
- `ro.ainpc.core.commands.handler.*`
- `ro.ainpc.core.commands.completion.*`
- `ro.ainpc.core.listeners.*`
- `ro.ainpc.core.listeners.registry.ListenerRegistry`

Regula:

- `AINPCCommand` trebuie spart pe subcomenzi, nu extins inca o data in aceeasi clasa.

## Faze de implementare

### Faza 0: Stabilizare inainte de mutari

Obiectiv:

- sa existe o baza minima de verificare inainte de mutarea codului

De facut:

- pastrarea testelor existente pentru `WorldAdminService`
- adaugarea de smoke tests pentru pornirea pluginului
- adaugarea de smoke tests pentru incarcarea feature packs
- adaugarea de smoke tests pentru inregistrarea addonurilor
- adaugarea de smoke tests pentru `reloadContent()`
- definirea unei reguli temporare: in fazele de mutare nu se adauga functionalitate noua in clasele mari

Rezultat asteptat:

- exista o plasa minima de siguranta pentru mutari mecanice

### Faza 1: Migrare fizica a surselor in `ainpc-core-plugin`

Obiectiv:

- modulul `ainpc-core-plugin` sa detina efectiv sursele si resursele sale

De facut:

1. copierea continutului din `src/src/main/java` in `ainpc-core-plugin/src/main/java`
2. copierea resurselor din `src/src/main/resources` in `ainpc-core-plugin/src/main/resources`
3. actualizarea `ainpc-core-plugin/pom.xml` pentru a folosi sursele standard locale
4. compilare si reparare importuri
5. rularea testelor

Regula:

- in aceasta faza nu se schimba numele claselor si nu se redeseneaza arhitectura; se face doar mutarea fizica si stabilizarea build-ului

Rezultat asteptat:

- folderul legacy `src/src` nu mai exista dupa migrare

### Faza 2: Subtierea lui `AINPCPlugin`

Obiectiv:

- intrarea Paper sa devina minima si previzibila

De facut:

- extragerea wiring-ului intr-un `PluginBootstrap`
- extragerea task-urilor programate intr-un `SchedulerCoordinator`
- extragerea logicii de reload intr-un `ReloadCoordinator`
- reducerea listei de getter-e brute catre servicii orientate pe intentie

Rezultat asteptat:

- `AINPCPlugin` devine o clasa de lifecycle, nu un service locator masiv

### Faza 3: Spargerea `ScenarioEngine`

Obiectiv:

- separarea clara intre model, parser, runtime, persistenta si questuri

Ordine recomandata:

1. extragerea modelului intern in `scenario/model`
2. extragerea incarcarii template-urilor in `ScenarioTemplateRepository`
3. extragerea persistentei progresului in `QuestProgressRepository`
4. extragerea logicii de interactiune in `QuestInteractionService`
5. introducerea registrelor pentru `actions`, `conditions`, `triggers`
6. reducerea `ScenarioEngine` la orchestration

Rezultat asteptat:

- scenariile pot fi extinse fara crestere continua a clasei centrale

### Faza 4: Spargerea `FeaturePackLoader`

Obiectiv:

- continutul declarativ sa fie tratat ca pipeline clar: read -> parse -> validate -> register

De facut:

- `FeaturePackParser`
- `FeaturePackValidator`
- `FeaturePackRepository`
- reguli explicite pentru pack principal, override si coliziuni

Rezultat asteptat:

- pack-urile devin usor de validat si usor de extins din addonuri

### Faza 5: Spargerea `NPCManager` si a zonei AI

Obiectiv:

- separarea operatiei pe NPC-uri de logica AI si de persistenta

De facut:

- extragerea serviciilor de spawn si restore
- extragerea simularii de viata
- extragerea backfill-ului de profile
- reducerea coupling-ului dintre `NPCManager`, `OpenAIService`, `DialogManager` si `DialogueEngine`

Rezultat asteptat:

- fluxurile NPC devin testabile independent

### Faza 6: Stabilizarea contractelor publice pentru addonuri

Obiectiv:

- addonurile sa consume doar API-ul public

De facut:

- mutarea contractelor de scenarii in `ainpc-api`
- adaugarea validarii de capabilitati si dependinte
- documentarea unui addon template minim
- verificarea addonului `ainpc-scenario-medieval` sa nu depinda de internals

Rezultat asteptat:

- un addon nou se poate scrie fara acces la clase instabile din core

### Faza 7: Curatenie finala

Obiectiv:

- eliminarea ramasitelor de monolit

De facut:

- stergerea folderului legacy `src/` dupa confirmarea build-ului
- actualizarea documentatiei
- actualizarea `TODO.md`
- stabilirea conventiilor de package si naming

Rezultat asteptat:

- repo-ul reflecta clar arhitectura modulara reala

## Ce nu trebuie facut in timpul refactorizarii

- nu se adauga module Maven noi inainte de finalizarea migrarii din `src/src`
- nu se muta cod si se schimba comportament in acelasi commit mare
- nu se lasa addonurile sa importe clase concrete din `ainpc-core-plugin`
- nu se extinde `ScenarioEngine`, `AINPCCommand` sau `NPCManager` cu functionalitate noua pana nu sunt sparte
- nu se muta totul direct in YAML fara registri si validare

## Ordinea de prioritate

Prioritatea corecta este:

1. migrarea fizica a surselor in modulul core
2. subtierea bootstrap-ului
3. spargerea `ScenarioEngine`
4. spargerea `FeaturePackLoader`
5. spargerea `NPCManager`
6. intarirea contractelor din `ainpc-api`

Aceasta ordine minimizeaza riscul si evita sa investesti in design nou peste o structura fizica inca ambigua.

## Definitia de gata

Refactorizarea este considerata terminata doar cand toate conditiile de mai jos sunt adevarate:

- `ainpc-core-plugin` compileaza exclusiv din propriul `src/main/java`
- `ainpc-core-plugin` foloseste propriul `src/main/resources`
- folderul legacy `src/src` nu mai este folosit in build
- `AINPCPlugin` este redus la responsabilitati de lifecycle
- `ScenarioEngine` nu mai contine parsing, persistenta si quest runtime in aceeasi clasa
- `FeaturePackLoader` nu mai face singur parse, validate si register
- addonurile depind doar de `ainpc-api`
- exista teste pentru zonele critice mutate

## Recomandare finala

Pentru acest repo, modularizarea corecta nu inseamna inca mai multe module Maven. Inseamna:

- finalizarea celor 3 module deja existente
- mutarea fizica a codului in `ainpc-core-plugin`
- separarea claselor mari in servicii mici
- intarirea granitelor dintre `ainpc-api`, `ainpc-core-plugin` si addonuri

Abia dupa asta are sens sa decizi daca mai este nevoie de module suplimentare.
