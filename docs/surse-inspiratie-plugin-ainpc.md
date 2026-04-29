# Surse de inspiratie pentru AINPC

Actualizat: 2026-04-29

## Scop

Acest document strange surse publice gasite online care pot inspira directia AINPC.

AINPC nu trebuie sa copieze aceste pluginuri si nu trebuie sa devina dependent obligatoriu de ele. Directia buna este sa invete din ideile mature ale acestor proiecte si sa le adapteze la modelul propriu:

- NPC-uri persistente si controlabile
- dialog AI contextual
- questuri cu progres clar
- mapping semantic `region/place/node`
- generare de sate si cladiri
- addonuri si scenarii modulare
- debugging bun pentru server admins

## Regula de arhitectura

Sursele de mai jos sunt referinte de design, nu cerinte de implementare.

- Nu clonam gameplay-ul, API-ul, fisierele de configurare sau structura interna a altor pluginuri.
- Nu facem dependinte obligatorii in `ainpc-core-plugin` pentru Citizens, Denizen, BetonQuest, WorldEdit, Towny sau alte pluginuri similare.
- Core-ul AINPC trebuie sa porneasca si sa functioneze singur pe Paper, cu propriul model de NPC, quest, mapping si familie.
- Daca o integrare devine utila, ea trebuie implementata ca adapter optional, modul separat sau addon cu `softdepend`, nu ca baza a sistemului.
- Inspiratia inseamna sa extragem principii: lifecycle clar, validare buna, event-driven runtime, debugging, authoring modular si separare intre core si extensii.

Consecinta practica: documentul recomanda concepte, nu pachete de copiat. Orice referinta externa trebuie tradusa in concepte AINPC native.

## Criterii de selectie

Am cautat surse care au una dintre urmatoarele valori pentru proiect:

- sistem matur de NPC-uri
- sistem matur de questuri
- scripting sau event/action runtime
- editor de dialog/quest
- AI conversation cu memorie si context
- integrare cu regiuni, world editing sau comunitati de jucatori
- idee de arhitectura care poate fi adaptata in AINPC

## Surse principale

### 1. Citizens

Surse:

- [Citizens - site oficial](https://citizensnpcs.co/)
- [Citizens Wiki](https://wiki.citizensnpcs.co/Main_Page)
- [Citizens Commands](https://wiki.citizensnpcs.co/Commands)

Ce merita observat:

- Citizens trateaza NPC-ul ca infrastructura de baza, nu ca quest direct.
- Are model mental clar: NPC, command, trait, pathing, shopkeeper, guard, scriptable NPC.
- Wiki-ul prezinta Citizens ca plugin pentru NPC-uri cu gameplay variat: statui, shopkeepers, guards, NPC-uri scriptabile.
- Comenzile expun control granular asupra NPC-ului.

Inspiratie pentru AINPC:

- Pastreaza `NPCManager` ca strat stabil de lifecycle, spawn, despawn, restore si persistenta.
- Nu incarca `NPCManager` cu quest logic, world generation sau AI prompt logic.
- Introdu pe viitor un concept de `Trait`/`Capability` pe NPC, de exemplu `quest_giver`, `merchant`, `guard`, `resident`, `family_member`.
- Fa comenzi de inspectie bune pentru NPC-uri: locatie, casa, munca, familie, quest disponibil, quest activ, memorie, profil.

Ce sa nu copiezi direct:

- Nu transforma AINPC intr-un wrapper Citizens. AINPC are deja baza proprie pe `Villager` si integrare AI.
- Nu lega tot comportamentul de comenzi admin. Runtime-ul trebuie sa fie bazat pe servicii si evenimente.

### 2. Denizen

Surse:

- [Denizen links directory](https://denizenscript.com/)
- [Denizen Meta Documentation - Commands](https://meta.denizenscript.com/Docs/Commands)

Ce merita observat:

- Denizen este foarte puternic pentru scripting: comenzi, evenimente, flags, cooldown-uri, NPC actions.
- Documentatia are categorii clare pentru comenzi: core, entity, item, npc, player, server, world.
- Are comanda/idee de `custom event`, ceea ce ajuta sistemele extensibile.
- Are `flags` pe obiecte, util pentru progres, stari temporare si date persistente atasate entitatilor.

Inspiratie pentru AINPC:

- Introdu un runtime de actiuni si conditii, nu doar metode hardcodate in `ScenarioEngine`.
- Directie recomandata:
  - `ScenarioActionRegistry`
  - `ScenarioConditionRegistry`
  - `ScenarioTriggerRegistry`
  - `QuestObjectiveRegistry`
  - `QuestRewardRegistry`
- Foloseste `quest_variables` si `story_flags` ca sistem controlat, nu ca JSON arbitrar fara validare.
- Adauga evenimente interne AINPC, de exemplu:
  - `AINPCQuestOfferedEvent`
  - `AINPCQuestAcceptedEvent`
  - `AINPCQuestObjectiveProgressEvent`
  - `AINPCPlaceEnteredEvent`
  - `AINPCNodeInspectedEvent`

Ce sa nu copiezi direct:

- Nu introduce un limbaj complet de scripting prea devreme. Pentru AINPC este suficient initial un registry de actiuni/conditii in Java plus YAML validat.

### 3. BetonQuest

Surse:

- [BetonQuest - Quest Packages](https://betonquest.org/2.1/Tutorials/Syntax/Quest-Packages/)
- [BetonQuest Spigot page](https://www.spigotmc.org/resources/betonquest-all-your-adventure-supplies-versatile-quests-in-depth-conversations.2117/)
- [BetonQuest integration list](https://betonquest.org/2.0/Documentation/Scripting/Building-Blocks/Integration-List/)

Ce merita observat:

- BetonQuest pune accent pe organizarea questurilor in pachete.
- Un quest poate fi impartit in fisiere separate pentru events, conditions, objectives, conversations si variables.
- Sistemul de package/subpackage este foarte util pentru questuri mari.
- Integrarile sunt tratate ca parte importanta a ecosistemului.

Inspiratie pentru AINPC:

- Introdu `quest packs` separate de feature packs generale.
- Structura recomandata pentru AINPC:

```text
plugins/AINPC/packs/
  medieval_quest/
    pack.yml
    professions.yml
    quests/
      q01_blacksmith.yml
      q02_missing_child.yml
    dialogues/
      blacksmith.yml
      healer.yml
    rewards.yml
    conditions.yml
```

- Nu tine toate questurile intr-un singur `medieval_quest.yml` pe termen lung.
- Adauga validare de pack:
  - quest id duplicat
  - objective id duplicat
  - material invalid
  - region/place/node inexistent
  - profession inexistenta
  - reward invalid

Ce sa nu copiezi direct:

- BetonQuest este foarte configurabil, dar poate deveni greu de inteles. AINPC trebuie sa pastreze un format mai mic, explicit si usor de citit.

### 4. Quests

Surse:

- [Quests documentation](https://quests.leonardobishop.com/)
- [Quests Spigot page](https://www.spigotmc.org/resources/quests.3711/)

Ce merita observat:

- Quests este orientat pe sistem extins de task-uri si questing clasic.
- Este popular pentru obiective directe, task-uri multiple si configurare de server.
- Documentatia este dedicata versiunii recente si mentinuta langa repo.

Inspiratie pentru AINPC:

- AINPC are nevoie de task/objective registry clar:
  - `collect_item`
  - `deliver_to_npc`
  - `talk_to_npc`
  - `visit_region`
  - `visit_place`
  - `inspect_node`
  - `kill_mob`
  - `craft_item`
  - `interact_block`
- Pentru fiecare tip de obiectiv trebuie sa existe:
  - validator
  - tracker
  - formatter pentru status
  - serializer pentru progres
  - teste automate

Ce sa nu copiezi direct:

- Nu fa questurile doar lista de task-uri generice. Avantajul AINPC este contextul NPC, familie, regiune, memorie si dialog AI.

### 5. ScriptedQuests

Sursa:

- [TeamMonumenta ScriptedQuests GitHub](https://github.com/TeamMonumenta/scripted-quests)

Ce merita observat:

- ScriptedQuests este prezentat ca plugin Paper/Spigot JSON-driven pentru questuri.
- Ideea importanta este sa creezi mecanici complexe prin fisiere de configurare si editor, fara sa scrii cod nou pentru fiecare quest.
- Include concepte utile precum NPC interaction, compass objectives, login/death logic, traders si rules.

Inspiratie pentru AINPC:

- AINPC poate avea pe termen mediu un editor extern sau in-game pentru questuri.
- Persistenta in JSON/YAML trebuie sa fie portabila si usor de backup.
- Merita introdus un `quest inspect` care arata cum vede runtime-ul questul dupa parsare.

Ce sa nu copiezi direct:

- Nu sari direct la editor. Mai intai stabilizeaza modelul de quest si validarea.

### 6. DialogueQuestEditor

Sursa:

- [DialogueQuestEditor pe Modrinth](https://modrinth.com/plugin/dialoguequesteditor)

Ce merita observat:

- Accentul este pe editor vizual, dialogue trees, branching choices si memorie de progres per jucator.
- Este o directie buna pentru server admins care nu vor scripting complicat.

Inspiratie pentru AINPC:

- Pe termen lung, AINPC ar trebui sa aiba un editor simplu pentru dialog:
  - noduri de dialog
  - optiuni de raspuns
  - conditii
  - actiuni
  - legatura cu quest stage
- Pentru MVP, e suficient un format YAML mai clar pentru dialoguri si quest hooks.

Ce sa nu copiezi direct:

- Nu incepe cu UI/editor inainte ca runtime-ul sa fie stabil. UI-ul va bloca designul daca modelul se schimba des.

### 7. VillagerGPT si Speaking Villagers

Surse:

- [VillagerGPT pe Modrinth](https://modrinth.com/plugin/villagergpt)
- [Speaking Villagers pe CurseForge](https://www.curseforge.com/minecraft/mc-mods/speaking-villagers)

Ce merita observat:

- VillagerGPT foloseste conversatii cu villageri, reputatie, personalitate dupa profesie si context din lume.
- Speaking Villagers merge pe AI conversation, friendship, quests procedurale, context awareness si multe combinatii de personalitate.
- Ambele confirma ca valoarea mare pentru AI NPC vine din context: locatie, vreme, timp, reputatie, iteme, profesie, memorie, relatii.

Inspiratie pentru AINPC:

- Pastreaza `PromptSnapshot` ca sursa controlata de adevar pentru AI.
- Extinde contextul cu:
  - `currentRegionId`
  - `currentPlaceId`
  - `homePlaceId`
  - `workPlaceId`
  - familie apropiata
  - quest activ
  - reputatie locala
  - evenimente recente din sat
- AI-ul nu trebuie sa decida direct recompense sau sa modifice DB. AI-ul poate propune intentii, iar runtime-ul valideaza.

Ce sa nu copiezi direct:

- Nu face AI-ul autoritatea finala pentru questuri. Questurile trebuie sa ramana deterministe si auditable.
- Evita dependinta de client modded daca tinta este server Paper/Spigot.

### 8. MCPDial

Sursa:

- [MCPDial: A Minecraft Persona-driven Dialogue Dataset](https://arxiv.org/abs/2410.21627)

Ce merita observat:

- Lucrarea descrie conversatii persona-driven intre jucator si NPC in Minecraft.
- Conversatiile includ descrieri bogate de personaj si pot include apeluri canonice de functie intre replici.

Inspiratie pentru AINPC:

- Fiecare NPC trebuie sa aiba profil stabil:
  - nume
  - varsta
  - ocupatie
  - trasaturi
  - familie
  - locuinta
  - loc de munca
  - obiective curente
- Promptul trebuie sa fie structurat, nu doar text liber.
- Tool/function calls pot deveni pe termen lung:
  - `find_place`
  - `get_quest_status`
  - `offer_quest`
  - `remember_fact`
  - `inspect_nearby_npcs`

Ce sa nu copiezi direct:

- Nu genera conversatii lungi fara control. In Minecraft, raspunsurile trebuie sa fie scurte, utile si legate de gameplay.

### 9. Paper Event API

Sursa:

- [PaperMC Event API](https://docs.papermc.io/paper/dev/api/event-api/)

Ce merita observat:

- Paper documenteaza listenere, custom events, handler lists si chat events.
- Pentru un plugin complex, evenimentele interne curate sunt mai usor de extins decat apelurile directe intre clase mari.

Inspiratie pentru AINPC:

- Listener-ele Bukkit trebuie sa fie subtiri.
- `QuestObjectiveListener` ar trebui sa publice evenimente catre un serviciu, nu sa contina logica de quest.
- Pe termen mediu:
  - `QuestEventRouter`
  - `ScenarioEventBus`
  - `NpcLifecycleEvent`
  - `WorldMappingEvent`

Ce sa nu copiezi direct:

- Nu emite evenimente pentru orice schimbare minora. Evenimentele trebuie sa reprezinte schimbari semnificative.

### 10. WorldEdit API

Sursa:

- [WorldEdit Developer API](https://worldedit.enginehub.org/en/7.1.0/api/)

Ce merita observat:

- WorldEdit separa `worldedit-core` de implementari pe platforme, inclusiv `worldedit-bukkit`.
- API-ul oficial recomanda dependinte diferite in functie de ce strat folosesti.

Inspiratie pentru AINPC:

- Integrarea WorldEdit trebuie sa fie modul separat:
  - `ainpc-integration-worldedit`
  - `worldedit-execution`
  - fallback nativ in core
- Core-ul trebuie sa defineasca doar contracte:
  - `StructureBuildService`
  - `StructurePlan`
  - `BuildResult`
  - `SemanticMapper`

Ce sa nu copiezi direct:

- Nu lega `ainpc-core-plugin` direct de WorldEdit ca dependinta obligatorie.

### 11. Towny

Sursa:

- [Towny GitHub portal](https://towny.github.io/)

Ce merita observat:

- Towny are ierarhie clara: resident, town, nation.
- Foloseste grid/chunk ownership si reguli de protectie.
- Are concept de comunitate persistenta, nu doar locatii.

Inspiratie pentru AINPC:

- Pentru satele generate, AINPC poate avea model:
  - `Resident`
  - `Household`
  - `Settlement`
  - `Region`
  - `Faction`
- `WorldRegion` poate deveni fundatia pentru story state si reputatie locala.
- Casele cu mai multi NPC trebuie tratate ca `Household`, nu doar `owner_npc_id`.

Ce sa nu copiezi direct:

- Nu implementa protectie de teren completa acum. Pentru AINPC conteaza semantica si gameplay-ul, nu managementul economic de claim-uri.

### 12. Deriving Quests from Open World Mechanics

Sursa:

- [Deriving Quests from Open World Mechanics](https://arxiv.org/abs/1705.00341)

Ce merita observat:

- Lucrarea foloseste Minecraft ca mediu pentru a analiza mecanici open-world si dependinte.
- Ideea utila este sa derive obiective din ordinea logica a mecanicilor, nu doar din liste arbitrare.

Inspiratie pentru AINPC:

- Questurile generate ar trebui sa respecte lanturi logice:
  - strange lemn
  - construieste un tool
  - mineaza fier
  - du fierul la fierar
  - primeste sabie
- Pentru generare automata, construieste un grafic de dependinte:
  - material
  - locatie
  - NPC rol
  - recompensa
  - risc
  - etapa de poveste

Ce sa nu copiezi direct:

- Nu incepe cu generare academica completa. Foloseste ideea simpla de dependency graph pentru questuri generate controlat.

## Matrice de inspiratie aplicata

| Zona AINPC | Surse utile | Directie recomandata |
| --- | --- | --- |
| NPC lifecycle | Citizens, Denizen | `NPCManager` stabil, traits/capabilities, comenzi de inspectie |
| Dialog AI | VillagerGPT, Speaking Villagers, MCPDial | context structurat, memorie, reputatie, familie, tool calls validate |
| Quest runtime | BetonQuest, Quests, ScriptedQuests | `QuestEngine`, objective registry, progress repository |
| Quest authoring | BetonQuest, DialogueQuestEditor, ScriptedQuests | quest packs, YAML validat, editor abia dupa stabilizarea runtime-ului |
| World mapping | Towny, Paper events, WorldEdit | regiuni/places/nodes ca sursa semantica pentru questuri si NPC-uri |
| Generare sate | WorldEdit, Towny, GDMC directie generala | builder optional, `StructurePlan`, `SemanticMapper`, households |
| Extensibilitate | Denizen, BetonQuest integrations, Paper custom events | registries pentru actiuni, conditii, trigger-e si rewards |

## Idei concrete pentru backlog

### Prioritate 1: QuestEngine separat

Extrage treptat din `ScenarioEngine`:

- `QuestInteractionService`
- `QuestProgressRepository`
- `QuestObjectiveTracker`
- `QuestRewardService`
- `QuestDefinitionParser`

Motiv: toate sursele mature separa definitia, runtime-ul si integrarea.

### Prioritate 2: Quest packs

In loc de un YAML mare, foloseste pachete:

```text
packs/medieval_quest/
  pack.yml
  quests/
  dialogues/
  rewards.yml
  conditions.yml
```

Motiv: inspirat din BetonQuest si ScriptedQuests; scaleaza mai bine decat un singur fisier.

### Prioritate 3: Obiective cu ID stabil

Fiecare obiectiv trebuie sa aiba ID explicit:

```yml
objectives:
  collect_iron:
    type: "collect_item"
    target_ref: "IRON_INGOT"
    amount: 3
    consume_on_complete: true
```

Motiv: progresul nu trebuie sa depinda de chei generate din `type:item:index`.

### Prioritate 4: Integrare cu `place` si `node`

Adauga obiective:

- `visit_place`
- `inspect_node`
- `talk_to_npc_at_place`
- `bring_item_to_place`

Motiv: AINPC are deja mapping semantic. Aceasta este diferenta fata de quest pluginurile generice.

### Prioritate 5: Household si familii reale

Adauga model:

- `Household`
- `residentIds`
- `familyId`
- `homePlaceId`

Motiv: Towny inspira ierarhia sociala, iar AINPC are deja `FamilyManager`, dar casa comuna este inca reprezentata temporar in metadata.

### Prioritate 6: Dialog scurt, contextual, auditable

Raspunsurile AI trebuie sa foloseasca:

- profil NPC
- relatie cu jucatorul
- familie
- quest activ
- loc semantic
- evenimente recente

Dar modificarile de stare trebuie facute de runtime, nu direct de model.

### Prioritate 7: Editor doar dupa runtime stabil

Un editor in-game sau web este util, dar numai dupa:

- modelul de quest este stabil
- validarea functioneaza
- pack-urile sunt clare
- reload-ul este sigur

## Directie de produs pentru AINPC

Pozitionarea buna nu este:

```text
inca un plugin de questuri
```

Pozitionarea mai buna este:

```text
un runtime de sate vii, NPC-uri AI, familii, questuri contextuale si world mapping semantic
```

Avantajul competitiv al AINPC ar trebui sa fie combinatia:

- NPC-uri cu memorie si personalitate
- familii si locuinte
- questuri legate de locuri reale
- sate generate si mapate semantic
- dialog AI controlat de date persistente
- scenarii/addonuri configurabile

## Ce trebuie evitat

- Questuri complet hardcodate in Java.
- AI care inventeaza progres, recompense sau modificari DB.
- Un fisier YAML urias pentru tot continutul.
- Dependinte obligatorii grele in core, precum WorldEdit.
- Editor UI inainte de runtime stabil.
- Obiective fara ID stabil.
- NPC-uri fara `homePlaceId`/`workPlaceId` pe termen lung.
- Listener-e Bukkit care contin logica de business.

## Roadmap inspirat din surse

### Etapa 1: Stabilizare

- teste automate pentru quest lifecycle
- validare YAML
- debug pentru `player_quests`
- comenzi de inspectie pentru quest progress

### Etapa 2: Separare arhitecturala

- extrage `QuestEngine`
- extrage `QuestProgressRepository`
- extrage `QuestObjectiveTracker`
- extrage `QuestRewardService`

### Etapa 3: Quest packs

- suport folder-based packs
- questuri impartite pe fisiere
- validator pentru dependinte si referinte

### Etapa 4: World-semantic quests

- `visit_place`
- `inspect_node`
- `bring_item_to_place`
- `talk_to_role_at_place`

### Etapa 5: Social simulation

- household model
- family placement
- reputatie per settlement
- story state pe regiune

### Etapa 6: AI orchestration

- tool calls validate
- intentii AI transformate in actiuni runtime
- memorie compacta
- raspunsuri scurte si contextuale

### Etapa 7: Editor

- editor simplu pentru dialog/quest
- export/import YAML sau JSON
- preview de validare

## Concluzie

Sursele analizate arata aceeasi directie: sistemele bune separa clar NPC-ul, questul, dialogul, world mapping-ul si extensiile.

Pentru AINPC, cea mai buna strategie este:

1. pastreaza NPC-urile si AI-ul ca diferentiator principal
2. extrage questurile din `ScenarioEngine`
3. foloseste `Region/Place/Node` ca avantaj fata de quest pluginurile generice
4. trateaza familiile si gospodariile ca model social, nu doar text in backstory
5. introdu extensibilitate prin registries si events, nu printr-un limbaj de scripting complet de la inceput
