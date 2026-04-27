# Strategie Plugin Modular si Scenarii Programabile

Actualizat: 2026-04-26

## Scop

Acest document descrie strategia prin care proiectul poate evolua dintr-un plugin mare, cu logica concentrata in core, intr-o platforma modulara capabila sa suporte scenarii practic nelimitat programabile.

Obiectivul real nu este "infinit" in sens matematic, ci:

- core stabil si reutilizabil
- continut mutat in pachete si addonuri
- scenarii extensibile fara a modifica mereu nucleul
- suport pentru lumi, teme si reguli foarte diferite

## Ideea centrala

Pluginul trebuie gandit ca o platforma, nu ca un singur gamemode.

Separarea buna este:

- `ainpc-api` = contract public
- `ainpc-core-plugin` = runtime, persistenta, manageri, executie
- `ainpc-scenario-*` = addonuri tematice sau de gameplay
- `packs/*.yml` = continut declarativ
- extensii de cod = actiuni, conditii, trigger-e, integrari speciale

Cu aceasta separare:

- core-ul nu trebuie sa stie detalii despre fiecare lume
- un scenariu nou nu cere automat schimbari in `ScenarioEngine`
- poti avea mai multe teme sau universuri fara fork de proiect

## Ce exista deja in proiect

Fundatia exista deja partial:

- proiectul este deja multi-modul Maven
- exista `ainpc-api`
- exista `AINPCPlatformApi`
- exista `AddonRegistry`
- exista `FeaturePackLoader`
- exista addon separat `ainpc-scenario-medieval`
- exista incarcare de pachete YAML pentru traits, professions, dialogues, topologies si scenarios

Deci directia buna este deja prezenta.

Limita actuala este alta:

- scenariile sunt inca dependente de logica hardcodata in core
- multe actiuni si obiective sunt finite si cunoscute dinainte
- `ScenarioEngine` risca sa devina prea mare daca fiecare comportament nou intra direct acolo

## Ce inseamna "scenarii nelimitat programabile"

In practica, asta inseamna:

- un autor de continut poate descrie scenarii noi in YAML
- un addon poate introduce tipuri noi de actiuni si conditii
- un scenariu poate lega questuri, NPC-uri, locuri, reactii si evenimente fara editarea nucleului
- un pachet nou poate schimba complet tonul lumii fara sa rescrie platforma

Formula buna este:

- continutul uzual = declarativ
- comportamentul special = extensii de cod
- integrari complexe = addonuri dedicate

Nu vrei "totul in Java", dar nici "totul in YAML".
Vrei un model hibrid.

## Principii de arhitectura

### 1. Core-ul trebuie sa fie generic

Core-ul trebuie sa detina doar:

- ciclul de viata
- registri si servicii
- persistenta
- runtime de scenarii
- validare
- hook-uri de executie

Core-ul nu ar trebui sa contina:

- reguli specifice unui singur univers
- if-uri pentru fiecare tema
- liste hardcodate de scenarii sau questuri

### 2. API-ul trebuie sa fie stabil

`ainpc-api` trebuie sa expuna doar contractele necesare pentru addonuri:

- registri
- tipuri publice
- context de executie
- descriptor de addon
- acces controlat la platforma

Addonurile trebuie sa depinda de API, nu de clase interne din core.

### 3. Continutul trebuie namespaced

Orice element extensibil ar trebui sa aiba namespace stabil:

- `medieval:blacksmith_intro`
- `roman_empire:market_conflict`
- `mystic:spirit_path`

Asta previne coliziuni intre pack-uri si addonuri.

### 4. Scenariile trebuie sa fie compuse din piese mici

Nu vrei un singur tip de scenariu rigid.
Vrei:

- trigger-e
- conditii
- stari sau etape
- tranzitii
- actiuni
- efecte
- cleanup

Asta transforma scenariul intr-un graf de executie, nu intr-o singura lista lineara.

### 5. Extensibilitatea trebuie sa vina prin registri, nu prin `if`-uri

Cand apare o actiune noua, nu vrei sa modifici de fiecare data:

- parserul
- motorul
- executorul central

Vrei un registru de handler-e:

- tipul este declarat in date
- handler-ul este inregistrat de core sau addon
- motorul doar rezolva si executa

## Arhitectura recomandata pe straturi

### Strat 1: Platforma si runtime

Acesta este `ainpc-core-plugin`.

Responsabilitati:

- bootstrap plugin
- servicii principale
- manageri NPC / quest / memorie / lume
- incarcare addonuri si pack-uri
- executie scenarii
- persistenta stare

### Strat 2: API public

Acesta este `ainpc-api`.

Responsabilitati:

- contracte stabile
- obiecte de context
- descrierea capabilitatilor
- interfete pentru extensie

Exemple de contracte utile pe viitor:

- `ScenarioActionHandler`
- `ScenarioConditionHandler`
- `ScenarioTriggerProvider`
- `ScenarioVariableProvider`
- `ScenarioRewardHandler`
- `ScenarioPackInstaller`

### Strat 3: Addonuri de scenariu

Acestea sunt pluginuri separate, de forma:

- `ainpc-scenario-medieval`
- `ainpc-scenario-modern`
- `ainpc-scenario-darkfantasy`

Responsabilitati:

- livreaza continut
- inregistreaza capabilitati noi
- instaleaza pack-uri sau resurse
- adauga comportamente tematice

### Strat 4: Feature packs declarative

Acestea sunt fisierele YAML din `packs/`.

Rolul lor este sa defineasca:

- traits
- professions
- dialogues
- topologies
- scenarios
- quest entries

Pe viitor, aici trebuie mutate si:

- triggers
- conditions
- actions
- transitions
- variables
- spawn rules
- scene hooks

### Strat 5: Extensii programabile

Acesta este stratul pentru cazurile in care YAML-ul nu ajunge.

Aici intra:

- action handlers custom
- condition handlers custom
- integrari cu alte pluginuri
- logica speciala de selectie NPC
- eventual un strat de scripting controlat

## Modelul corect pentru scenarii

Scenariul nu ar trebui tratat doar ca "quest cu cateva objective".

Modelul mai bun este:

- `ScenarioDefinition`
- `ScenarioStage`
- `ScenarioTransition`
- `ScenarioTrigger`
- `ScenarioCondition`
- `ScenarioAction`
- `ScenarioReward`
- `ScenarioCleanupRule`

Executia devine:

1. trigger-ul porneste scenariul
2. se construieste contextul
3. se evalueaza conditiile
4. se intra intr-o etapa
5. se executa actiuni
6. se asteapta evenimente sau conditii de tranzitie
7. se schimba etapa
8. se finalizeaza si se aplica cleanup

## Ce trebuie sa poata extinde un addon

Pentru scenarii cu adevarat programabile, un addon trebuie sa poata inregistra:

- actiuni noi
- conditii noi
- trigger-e noi
- selectori de NPC
- rezolvatori de locatii
- tipuri de recompense
- validatori de pack
- migrari de date pentru versiuni noi

Exemple:

- `spawn_temporary_npc`
- `teleport_npc_to_place`
- `start_cutscene`
- `require_reputation_at_least`
- `if_time_between`
- `bind_scenario_to_region`
- `grant_custom_currency`

## Interfete recomandate

Mai jos este directia buna pentru API. Numele pot fi ajustate, dar ideea trebuie pastrata.

```java
public interface ScenarioActionHandler {
    String getActionType();
    void execute(ScenarioExecutionContext context, ScenarioActionDefinition action);
}

public interface ScenarioConditionHandler {
    String getConditionType();
    boolean evaluate(ScenarioExecutionContext context, ScenarioConditionDefinition condition);
}

public interface ScenarioTriggerProvider {
    String getTriggerType();
    void bind(ScenarioRuntime runtime, ScenarioTriggerDefinition trigger);
}
```

Avantajul este simplu:

- `ScenarioEngine` nu mai cunoaste fiecare tip concret
- addonurile isi aduc propriile capabilitati
- continutul doar refera `type`

## Context de executie unificat

Scenariile programabile au nevoie de un context comun.

`ScenarioExecutionContext` ar trebui sa poata expune:

- jucatorul implicat
- NPC-urile implicate
- regiunea, place-ul si node-ul curent
- variabile de scenariu
- starea questului
- runtime mode / world mode
- acces la servicii prin API

Fara un context unificat, fiecare extensie incepe sa caute singura prin plugin si apare coupling inutil.

## Variabile si state

Pentru a evita codul hardcodat, scenariile trebuie sa poata folosi variabile.

Exemple:

- `player_reputation`
- `current_region`
- `active_place`
- `quest_stage`
- `npc_owner`
- `time_of_day`
- `has_item.iron_ore`

Sursele acestor variabile pot fi:

- runtime intern
- world admin
- quest engine
- NPC context
- addonuri externe

De aceea este util un `ScenarioVariableProviderRegistry`.

## YAML bun pentru scenarii programabile

Exemplu conceptual:

```yml
scenarios:
  blacksmith_emergency:
    type: "scriptable"
    namespace: "medieval"
    trigger:
      type: "player_enter_place"
      place: "fierarie"
    conditions:
      - type: "time_between"
        start: 6000
        end: 12000
      - type: "quest_not_completed"
        quest: "Q_FORGE_01"
    stages:
      intro:
        actions:
          - type: "spawn_temporary_npc"
            npc_template: "forge_apprentice"
            place: "fierarie"
          - type: "dialogue"
            speaker: "blacksmith"
            profile: "urgent_intro"
        transitions:
          - to: "delivery"
            when:
              type: "dialogue_finished"
      delivery:
        actions:
          - type: "start_quest"
            quest: "Q_FORGE_01"
        transitions:
          - to: "cleanup"
            when:
              type: "quest_completed"
              quest: "Q_FORGE_01"
      cleanup:
        actions:
          - type: "despawn_npc"
            npc: "forge_apprentice"
```

Observatia importanta:

- YAML-ul descrie structura
- tipurile concrete sunt rezolvate de handler-e

## Modul corect de a trata addonurile

Un addon de scenariu ar trebui sa poata face trei lucruri distincte:

### 1. Sa se descrie

Prin `AddonDescriptor`:

- id
- versiune
- tip
- capabilitati
- dependinte
- runtime modes suportate

### 2. Sa livreze continut

Prin:

- pack-uri YAML
- resurse
- template-uri
- dialoguri
- skin-uri

### 3. Sa livreze logica

Prin:

- handler-e noi
- validatori
- instalatori de resurse
- integrari cu alte servicii

## De ce nu trebuie sa bagi tot in `ScenarioEngine`

Daca `ScenarioEngine` ajunge sa contina:

- parser
- selectie NPC
- evaluare conditii
- actiuni
- recompense
- dialoguri
- spawn logic
- cleanup
- integrari externe

atunci devine un punct unic de blocaj.

Problemele apar imediat:

- greu de extins
- greu de testat
- greu de mentinut
- orice scenariu nou cere schimbare in core

Modelul bun este:

- `ScenarioDefinitionParser`
- `ScenarioValidator`
- `ScenarioRuntime`
- `ScenarioActionRegistry`
- `ScenarioConditionRegistry`
- `ScenarioTriggerRegistry`

`ScenarioEngine` ramane orchestration layer, nu "god class".

## Compatibilitate si versionare

Pentru modularitate reala, trebuie sa existe reguli de compatibilitate:

- `api_version`
- `pack_version`
- `required_capabilities`
- `dependencies`
- `migration_notes`

Cand un addon cere o capabilitate lipsa, core-ul trebuie sa:

- refuze clar incarcarea
  sau
- dezactiveze doar continutul incompatibil

Nu trebuie sa pice tot pluginul din cauza unui singur pack invalid.

## Validare la incarcare

Un sistem bun nu asteapta runtime-ul ca sa descopere problemele.

La `reloadContent()` trebuie validate:

- id-uri duplicate
- tipuri necunoscute de actiuni
- tipuri necunoscute de conditii
- dependinte lipsa
- referinte invalide la NPC, place, region, quest
- tranzitii spre etape inexistente
- scenarii fara cleanup minim

Asta iti permite un ecosistem modular fara haos.

## Cat de "nelimitat" trebuie sa fie sistemul

Trebuie evitate doua extreme:

- prea inchis: doar cateva tipuri hardcodate
- prea liber: orice executa orice fara contract

Directia buna este:

- extensibil arbitrar
- dar prin contracte controlate

Adica:

- poti adauga actiuni noi fara sa rupi core-ul
- poti crea scenarii foarte diferite
- dar totul trece prin API, validare si lifecycle comun

## Optional: strat de scripting

Daca pe viitor vrei flexibilitate si mai mare, poti adauga un strat de scripting.

Exemple:

- JavaScript
- Kotlin script
- DSL intern

Dar acesta ar trebui introdus doar dupa ce:

- registrii exista
- handler-ele sunt stabile
- modelul declarativ este matur

Altfel vei sari direct la scripting pentru probleme care puteau fi rezolvate mai simplu prin date si extensii mici.

## Ce sa eviti

- sa hardcodezi scenarii tematice in core
- sa faci `ScenarioEngine` responsabil pentru toate tipurile concrete
- sa permiti addonurilor sa consume direct clase interne instabile
- sa folosesti id-uri fara namespace
- sa tratezi scenariul doar ca lista de objective
- sa pui logica de lume sau quest direct in fisiere fara validare
- sa sari direct la scripting inainte de registri si contracte curate

## Ordine buna de implementare

### Faza 1

Stabilizeaza fundatia modulara deja existenta.

De facut:

- clarifica responsabilitatile `ainpc-api` vs `ainpc-core-plugin`
- evita dependintele addonurilor pe internals din core
- defineste capabilitatile standard

### Faza 2

Sparge executia de scenarii in componente.

De facut:

- parser separat
- validator separat
- runtime separat
- registri pentru actiuni, conditii si trigger-e

### Faza 3

Fa scenariile cu adevarat extensibile.

De facut:

- handler-e publice in API
- inregistrare din addon
- YAML cu `type` liber pentru actiuni si conditii

### Faza 4

Leaga scenariile de restul platformei.

De facut:

- questuri avansate
- mapping semantic `region/place/node`
- NPC-uri temporare
- reactii NPC-jucator

### Faza 5

Adauga unelte de ecosistem.

De facut:

- validare mai buna
- mesaje clare de eroare
- exemplu de addon template
- eventual scripting sandboxed

## MVP recomandat pentru proiectul tau

Cel mai bun pas urmator nu este un sistem "infinite scripting".

Cel mai bun MVP este:

1. `ScenarioActionRegistry`
2. `ScenarioConditionRegistry`
3. `ScenarioTriggerRegistry`
4. `ScenarioExecutionContext`
5. extinderea YAML-ului pentru `actions`, `conditions`, `transitions`
6. posibilitatea ca `ainpc-scenario-medieval` sa inregistreze macar un handler nou

Daca asta functioneaza, arhitectura devine deja cu adevarat modulara.

## Concluzie

Strategia corecta este sa transformi pluginul intr-o platforma in straturi:

- core generic
- API stabil
- addonuri tematice
- pack-uri declarative
- extensii programabile prin registri

Scenariile "nelimitat programabile" nu vin dintr-un fisier urias sau dintr-un `ScenarioEngine` gigant.
Ele vin din separarea clara dintre:

- runtime
- continut
- contracte
- extensii

Aceasta este directia care iti permite sa adaugi teme noi, lumi noi si logica noua fara sa rescrii proiectul de fiecare data.
