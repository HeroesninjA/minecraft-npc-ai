# Directii din BetonQuest care se potrivesc pentru AINPC

Actualizat: 2026-04-27

## Scop

Acest document extrage doar caracteristicile din analiza BetonQuest care au sens pentru arhitectura actuala AINPC.

Scopul nu este sa clonam BetonQuest, ci sa identificam ce model de extensibilitate merita preluat pentru:

- questuri
- scenarii
- conversatii
- world mapping
- addonuri si pack-uri

## Concluzie scurta

Da, exista mai multe idei din BetonQuest care se potrivesc foarte bine cu AINPC.

Cele mai bune de preluat sunt:

- modelul declarativ cu building blocks mici
- registrii pentru extensie
- separarea intre API public si core
- persistenta explicita a progresului pe obiective
- event bus pentru schimbari de stare
- separarea dintre logica de conversatie si UI / prezentare
- schedules / delayed actions
- cleanup / cancel rules

Ce nu merita copiat direct:

- un API legacy static si greu de controlat
- un DSL prea mare din prima iteratie
- o suprafata foarte mare de hooks inainte sa existe runtime-ul stabil

## De ce se potriveste cu codul actual

AINPC are deja fundatia potrivita:

- proiect multi-modul in `pom.xml`
- API public in `ainpc-api`
- serviciu Bukkit expus prin `AINPCPlatformApi`
- `FeaturePackLoader` pentru continut YAML
- `ScenarioEngine` pentru questuri si scenarii
- `WorldAdminService` pentru `regions`, `places` si `nodes`
- `DialogManager` si `DialogueEngine` pentru conversatie
- `AddonRegistry` pentru extensii

Cu alte cuvinte, directia generala exista deja. Ce lipseste este runtime-ul extensibil pe bucati mici.

## Caracteristici care se potrivesc direct

### 1. Building blocks declarative

Asta este cea mai buna idee de preluat.

BetonQuest trateaza questul ca o compozitie din:

- actions
- conditions
- objectives
- conversations
- schedules
- journal / notificari

La tine, ideea se potriveste natural peste:

- `FeaturePackLoader.java`
- `ScenarioEngine.java`

Ce inseamna pentru AINPC:

- `scenarios` nu trebuie sa fie doar sabloane hardcodate
- fiecare scenariu ar trebui sa fie compus din piese mici
- YAML-ul trebuie sa descrie fluxul, nu doar cateva campuri de quest

Adaptare recomandata:

- `ScenarioActionDefinition`
- `ScenarioConditionDefinition`
- `ScenarioTriggerDefinition`
- `ScenarioStageDefinition`
- `ScenarioTransitionDefinition`

### 2. Registrii pentru extensie

A doua caracteristica foarte potrivita este modelul de registri pentru tipuri de quest.

BetonQuest foloseste registre pentru `Condition`, `Action`, `Objective`, `Placeholder`.
La tine, asta se potriveste foarte bine cu ideea deja documentata in:

- `docs/strategie-plugin-modular-si-scenarii-programabile.md`
- `docs/documentatie-api.md`

Ce trebuie introdus in AINPC:

- `ScenarioActionRegistry`
- `ScenarioConditionRegistry`
- `ScenarioObjectiveRegistry`
- `ScenarioVariableRegistry`
- `ScenarioTriggerRegistry`

Beneficiu:

- core-ul nu mai creste prin `if/switch`
- addonurile pot adauga tipuri noi fara modificari in `ScenarioEngine`

### 3. Separarea API public vs core intern

Asta deja se potriveste foarte bine cu structura ta:

- `ainpc-api`
- `ainpc-core-plugin`
- `ainpc-scenario-medieval`

Din modelul BetonQuest merita preluata disciplina, nu neaparat forma exacta.

Regula buna pentru AINPC:

- addonurile depind de `ainpc-api`
- runtime-ul si clasele interne raman in core
- serviciile publice se obtin prin Bukkit `ServicesManager`

Pe scurt: da, directia BetonQuest confirma ca separarea ta actuala este corecta.

### 4. Persistenta explicita pentru progresul obiectivelor

Asta se potriveste direct cu ce ai deja in `ScenarioEngine`.

In momentul de fata exista deja:

- progres persistent per jucator
- statusuri `OFFERED`, `ACTIVE`, `COMPLETED`, `FAILED`
- progres pe obiective

Tipurile suportate acum sunt apropiate de modelul de baza BetonQuest:

- `collect_item`
- `deliver_to_npc`
- `talk_to_npc`
- `visit_region`
- `kill_mob`

Ce merita preluat mai departe din modelul BetonQuest:

- progres per etapa, nu doar per quest
- `ObjectiveData` mai clar separat de definitie
- variabile de quest persistente
- istoric de tranzitii / audit minim

### 5. Event bus pentru schimbari de stare

Asta lipseste acum si este foarte valoros.

BetonQuest publica evenimente pentru:

- progres obiectiv
- jurnal
- tag-uri
- conversatii
- incarcare date

Pentru AINPC, o versiune buna ar fi:

- `AINPCQuestOfferedEvent`
- `AINPCQuestAcceptedEvent`
- `AINPCQuestObjectiveProgressEvent`
- `AINPCQuestCompletedEvent`
- `AINPCQuestFailedEvent`
- `AINPCRegionEnteredEvent`
- `AINPCPlaceVisitedEvent`
- `AINPCConversationStartedEvent`

Beneficiu:

- alte addonuri pot reactiona fara sa cunoasca intern `ScenarioEngine`

### 6. Separarea logica de conversatie de prezentare

BetonQuest separa `ConversationIO` de logica efectiva.
Pentru AINPC, asta se potriveste foarte bine peste:

- `DialogManager.java`
- `DialogueEngine.java`

Momentan ai logica de raspuns si istoric de dialog, dar nu ai inca un runtime de conversatie ramificata cu UI separabil.

Modelul bun pentru AINPC:

- logica: noduri, optiuni, conditii, tranzitii
- prezentare: chat simplu, clickable chat, inventory GUI, eventual alte UI-uri

Clase utile:

- `ConversationDefinition`
- `ConversationNode`
- `ConversationOption`
- `ConversationCondition`
- `ConversationRenderer`
- `ConversationSession`

### 7. Schedules si delayed actions

Asta se potriveste bine cu natura AI / narativa a proiectului.

BetonQuest foloseste schedules pentru pornire intarziata, repetitii si trigger-e temporale.
La tine ar fi util pentru:

- evenimente narative
- respawn / reset de scene
- NPC-uri temporare
- questuri cu timeout
- tranzitii de faza

Nu trebuie copiat tot sistemul de cron din prima.
Pentru AINPC sunt suficiente initial:

- `delay`
- `repeat`
- `expire_at`
- `cooldown`

### 8. Cleanup / cancel rules

Asta este foarte potrivit pentru AINPC, mai ales fiindca ai scenarii si questuri care pot esua sau fi abandonate.

Din modelul BetonQuest merita preluata ideea ca abandonul nu este doar "schimba statusul", ci poate face cleanup real:

- scoate tag-uri
- reseteaza variabile
- despawneaza NPC temporar
- sterge markere de lume
- schimba story state

Clase utile:

- `ScenarioCleanupRule`
- `QuestFailureRule`
- `QuestCancelAction`

## Caracteristici potrivite dupa refactor

### 1. Placeholders / variables de runtime

Asta se potriveste, dar dupa ce exista registri si context de executie.

Exemple bune pentru AINPC:

- `player_name`
- `npc_name`
- `region_id`
- `place_id`
- `story_state`
- `quest_stage`
- `npc_home_place`

Acestea sunt utile in:

- dialog
- conditions
- rewards
- prompts AI

### 2. Journal / quest log

Foarte util, dar nu este primul pas.

La tine, un `journal` ar trebui sa fie calculat din:

- quest activ
- etapa curenta
- obiective lipsa
- locuri relevante din mapping
- NPC-ul asociat

Asta se leaga direct de:

- `ScenarioEngine`
- `WorldAdminApi`
- viitorul `visit_place`

### 3. Menu-driven quest UI

Se potriveste, dar doar dupa ce exista model de conversatie / stage mai clar.

Altfel vei construi un GUI peste un runtime inca rigid.

### 4. Hook-uri de compatibilitate

BetonQuest are foarte multe integrari externe.
La tine, ideea este buna, dar doar dupa stabilizarea API-ului public.

Ordinea buna este:

1. API public stabil
2. registri
3. evenimente
4. hook-uri externe

Nu invers.

## Caracteristici de evitat sau copiate partial

### 1. API legacy static

Asta nu merita copiat.

AINPC ar trebui sa evite un model de tip:

- singleton global
- `registerX(...)` static din clasa principala
- dependenta addonurilor de clase concrete din core

Mai bine:

- servicii Bukkit
- interfete in `ainpc-api`
- registri expusi controlat

### 2. DSL foarte mare din prima

BetonQuest are un DSL bogat pentru ca este matur si vechi.

Daca incerci sa copiezi imediat:

- actions
- conditions
- objectives
- menus
- journal
- cancelers
- schedules
- placeholders
- compat hooks

vei obtine prea mult cod fragil prea devreme.

Pentru AINPC, DSL-ul trebuie crescut incremental.

### 3. Suprafata masiva de feature-uri inaintea modelului semantic

BetonQuest poate trai mai usor cu multe target-uri pentru ca are deja motorul de questing ca produs principal.

La tine, totul trebuie sa stea pe fundatia:

- `regions`
- `places`
- `nodes`
- roluri NPC
- context semantic

Fara asta, vei avea doar obiective multe, dar nu si lume narativa coerenta.

## Mapping direct pe codul actual

### Ce ai deja si este compatibil cu directia BetonQuest

- `AINPCPlatformApi` confirma separarea API / core
- `FeaturePackLoader` confirma modelul declarativ cu pack-uri YAML
- `ScenarioEngine` confirma runtime-ul de quest si progres persistent
- `WorldAdminService` confirma directia semantica pentru `regions`, `places`, `nodes`
- `DialogManager` si `DialogueEngine` confirma directia spre conversatii bogate
- `AddonRegistry` confirma suportul pentru extensii

### Ce lipseste ca sa devina echivalentul bun pentru AINPC

- registri publici pentru actions / conditions / objectives / variables / triggers
- etape reale de quest
- `visit_place` si obiective bazate pe `node`
- event bus public
- cleanup / cancel rules
- presentation layer pentru conversatii
- schedules declarative
- reward handlers dincolo de iteme

## Ordinea buna de implementare

### Faza 1

- `ScenarioActionRegistry`
- `ScenarioConditionRegistry`
- `ScenarioTriggerRegistry`
- `ScenarioExecutionContext`

### Faza 2

- `QuestStageDefinition`
- `QuestVariableBag`
- `QuestRewardHandler`
- `QuestFailureRule`

### Faza 3

- `visit_place`
- `inspect_node`
- `talk_to_npc_at_place`
- `bring_item_to_place`

### Faza 4

- event bus Bukkit pentru stare de quest si mapping
- `ConversationDefinition` + renderer separat

### Faza 5

- schedules
- journal
- meniuri
- integrari externe

## Directie recomandata

Formula buna pentru AINPC, inspirata de BetonQuest dar adaptata corect, este:

- model semantic al lumii propriu
- quest runtime modular
- continut declarativ in pack-uri
- extensii programabile prin registri
- UI de conversatie separat de logica

Nu trebuie sa devii "un alt BetonQuest".
Trebuie sa folosesti exact piesele care se suprapun cu identitatea proiectului tau:

- NPC-uri AI
- scenarii narative
- mapping semantic
- questuri extensibile
- addonuri tematice

## Rezumat final

Cele mai potrivite idei din BetonQuest pentru codul tau sunt:

- building blocks declarative
- registri pentru extensie
- persistenta de obiective si variabile
- event bus
- conversatii separate de UI
- schedules
- cleanup rules

Cea mai importanta adaptare pentru AINPC este insa aceasta:

BetonQuest are ca centru questing-ul.
AINPC trebuie sa aiba ca centru relatia dintre:

- NPC
- jucator
- loc
- scenariu
- stare narativa

De aceea, ideile BetonQuest trebuie absorbite prin filtrul `world mapping + NPC context`, nu copiate mecanic.
