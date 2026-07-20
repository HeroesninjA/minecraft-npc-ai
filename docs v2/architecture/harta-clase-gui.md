# Harta claselor pentru GUI

Status: harta structurala verificata in cod.
Verificat in cod: 2026-07-16.

Aceasta pagina descrie ownership-ul claselor GUI. Contractul de comportament ramane in `reference/gui-stack.md`.

## Intrare si rutare

- `AINPCCommandMisc.handleGui` rezolva rolul, aliasul, filtrul quest si `resume`;
- `AINPCCommandQuest.handleQuestGui` deschide quest log-ul optional filtrat;
- `GuiRole` rezolva prioritatea `ADMIN -> CREATOR -> PLAYER`;
- `GuiKey` detine ID-urile, numele si aliasurile ecranelor;
- `GuiService.canOpen` detine matricea tehnica de permisiuni pe cheie.

## Infrastructura

- `GuiService` detine registry-ul, deschiderea, selectiile per player, confirmarea, inputul text si dispatch-ul comenzilor;
- `GuiScreen` este contractul `key/title/size/render`;
- `GuiRenderContext` scrie iteme si butoane numai in sloturi valide;
- `GuiButton` separa iconul, actiunea si starea enabled;
- `GuiAction` executa callback-ul cu `GuiClickContext`;
- `GuiItemFactory` construieste itemele Adventure/Bukkit;
- `GuiNavigation` adauga controalele standard pentru ecranele de 54 de sloturi care il folosesc.

## Sesiune si evenimente

- `GuiSessionManager` pastreaza indexul sesiune-player si permite o singura sesiune activa per player;
- `GuiSession` retine cheia, momentul deschiderii si snapshot-ul butoanelor;
- `AINPCGuiHolder` identifica inventarul prin UUID si `GuiKey`;
- `GuiInventoryListener` anuleaza click/drag, ruteaza click-ul si curata sesiunea la close;
- `GuiService.handleClick` verifica ownership-ul sesiunii si reaplica `canOpen` pentru cheia activa;
- `PlayerQuitEvent` curata intreaga stare tranzitorie prin `GuiService.clearPlayerState`;
- `NPCChatListener` consuma `TextInputRequest`, valideaza inputul si redeschide ecranul.

## Hub-uri

- `MainHubGui` este suprafata generala si afiseaza numai butoanele permise sau variante disabled;
- `PlayerHubGui` grupeaza progresia si actiunile uzuale;
- `AdminHubGui` grupeaza operarea, mapping-ul si diagnosticul;
- `CreatorHubGui` grupeaza quest si mapping authoring si aplica suplimentar rolul admin/creator.

Hub-urile nu definesc persistenta si nu sunt o autoritate universala de permisiuni.

## Familii de ecrane

- progresie: `QuestLogGui`, `QuestDetailGui`, `QuestOfferGui`, `QuestOfferNpcGui`;
- quest authoring: `QuestAuthoringGui`, `QuestCreatorGui`, `QuestCreatorDefinitionsGui`, `QuestCreatorTestGui`, `QuestCreateGui`, `QuestEditGui`, `QuickQuestGui`, `QuestMapGui`;
- world: `WorldHubGui`, `WorldRegionGui`, `WorldPlaceGui`, `AdminMappingGui`, `MappingCreatorGui` si cele trei formulare de creare;
- NPC: `NpcInteractionGui`, `NpcManagerGui`, `NpcMemoryGui`, `RoutineGui`, `RelationshipGui`, `ShopGui`;
- story si status: `StoryGui`, `StoryAuthoringGui`, `StatsGui`;
- operare: `AdminQuestGui`, `AdminMcpGui`, `AuditGui`, `DebugGui`;
- protectie: `ConfirmActionGui`.

## Fluxuri

### Inventar

`handler -> GuiService.open -> GuiScreen.render -> GuiSession -> AINPCGuiHolder -> GuiInventoryListener -> GuiAction`

### Comanda

`GuiAction -> GuiService.runCommand -> close inventory -> Bukkit dispatch ca player -> command validation`

### Formular chat

`GuiAction -> openTextInput -> close inventory -> AsyncChatEvent anulat -> validare sync -> creatorFormValues -> reopen`

## Ownership de stare

- sesiunea inventarului: `GuiSessionManager`;
- selectiile si drafturile GUI: `GuiService`, numai in memorie;
- mapping: `WorldAdminApi` si serviciile world;
- quest/progression: `ProgressionService` si repository-urile sale;
- story: serviciile story;
- rutina, relatii, memorie si shop: serviciile domeniului NPC;
- permisiuni declarate: `plugin.yml`;
- feature flags si auto-confirm: `config.yml`.

## Limite structurale

- `GuiService` concentreaza multe harti de stare si politici diferite;
- cheia este reautorizata la click, dar targetul si mutatia finala raman responsabilitatea ecranului, comenzii sau serviciului;
- listenerul de chat este dependinta formularului creator si a dialogului NPC;
- navigarea standard revine la `MAIN`, fara arbore/stiva comuna;
- ecranele aleg individual confirmarea, paginarea si validarea la click.

## Legaturi

- `reference/gui-stack.md`
- `guides/gui-interfete.md`
- `planning/gui-ux-hardening.md`
- `architecture/interactiuni.md`
- `reference/harta-clase-cod.md`
