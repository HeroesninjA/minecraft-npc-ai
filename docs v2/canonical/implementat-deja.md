# Implementat Deja

Status: canonical in `docs v2`.
Actualizat: 2026-07-18.

Acest document este sursa canonica pentru ce exista deja in cod si nu trebuie reconstruit de la zero.

## Module existente

- `ainpc-core-plugin` contine majoritatea runtime-ului curent.
- `ainpc-api` expune contractele publice pentru addonuri.
- `ainpc-scenario-medieval` este addon Paper separat si livreaza config plus pack-uri tematice.
- `ainpc-mcp-service` este sidecar-ul Spring Boot separat de runtime-ul Paper.

## Capacitatile curente

### NPC si interactiune

- NPC-uri persistente, source keys, profile data si `npc_world_bindings`.
- Households si rezidenti persistenti salvati de fluxul de spawn, cu migration si repair administrative.
- Click dreapta, sesiune de conversatie in memorie si chat directionat catre NPC.
- Ascultare pasiva configurabila, dezactivata implicit.
- Dialog factual/template cu ramura OpenAI optionala si fallback local.
- Istoric, memorii si relatii player-NPC persistate dupa dialogul normal.
- Relatii NPC-NPC persistate separat si alimentate de simularea sociala.
- Simulare individuala pentru nevoi/stare si rutina separata pentru atribuire/miscare.
- Rutina cu profile pe ocupatie, offset stabil, pathfinding Paper si teleport fallback.
- Actori de scenariu cu adaptoare Villager, Zombie si ArmorStand, spawn policy si durata optionala.

### Quest si progression

- Questuri definite in scenarii si fallback-uri locale.
- Acceptare, refuz, abandon, status, reset si completare.
- Persistenta progresului si view-uri read-only pentru audit.
- Selectori de progres si snapshot-uri pentru GUI/debug.

### Story si context

- Story state si event tracking.
- Context narativ read-only pentru quest anchors.
- Briefing si rezumate de progres.

### GUI si admin

- Registry de ecrane, holder propriu, o sesiune de inventar per player si rutare de click/drag/close.
- Hub-uri pe rol, matrice de acces pe cheie, confirmari explicite si formulare creator prin chat.
- GUI pentru inspectie si operatii curente, cu actiuni directe si comenzi dispatch-uite ca player.
- Actiuni rapide pentru status, tracking, abandon si debug.
- Comenzi read-only de audit si debugdump.

### Mapping si world

- Regiuni, places, nodes si ancore semantice.
- Inspectie pentru world mapping si bindings.
- Scanare vanilla cu preview si import in mapping semantic.
- Planificare `HouseAllocation`, dry-run si spawn batch cu persistenta household.
- Gap analysis si patch planning; aplicarea normala poate completa node-uri semantice.
- Trei template-uri hardcodate care pot crea places si marker nodes, fara blocuri.
- Catalog, plan read-only si validare pentru structuri exterioare.
- Fixture administrativ hardcodat pentru mapping si populare de test.
- Politica centrala runtime read-only pentru comenzile care confirma sau aplica mapping, importa sate, ruleaza auto-place, modifica world mapping ori spawneaza fixture/household/settlement.
- Confirmare CLI centrala cu `--confirm` pentru acele mutatii, bridge automat prin dialogurile GUI si reminder unic `/ainpc world save` cand mapping-ul ramane nesalvat; `map confirm` si `world save` sunt actiuni deja explicite.
- Mesaj comun pentru `settlement auto`, `building auto-place` si native patch care numeste artefactele semantice si exclude explicit constructia sau modificarea blocurilor fizice.
- Reutilizare opt-in a unei regiuni existente pentru `settlement auto`, cu validarea lumii si centrului, pastrarea metadatelor regiunii si compensarea limitata la artefactele nou-create.
- Rezultat structurat pentru binding-ul post-spawn al household-urilor si settlement-urilor, cu stari complet/partial/esuat/fara tinte, contoare separate pentru mapping si persistenta si mesaj explicit ca un succes partial nu face rollback NPC-urilor.
- Compensare comuna pentru creatiile partiale din import, patch apply, building auto-place si fixture apply; rollback-ul elimina numai ID-urile jurnalizate de operatia curenta.
- Statistici de populatie si repopulare vanilla pe baza paturilor, aceasta din urma dezactivata implicit.
- Scanare vanilla incrementala prin `VanillaVillageScanService`: comenzile `world scan village` si `settlement auto` folosesc aceeasi coada FIFO pe schedulerul sincron Bukkit si impart un buget global de blocuri per tick configurabil; importul si planificarea ruleaza dupa finalizarea raportului.

### AI si orchestration

- Dialog determinist cu folosire optionala a providerului prin `OpenAIService`; serviciul poate returna fallback local fara request extern.
- Context semantic world/story inclus in promptul de dialog.
- `AIOrchestrationService`, validatorul si lifecycle-ul de sugestii exista ca scaffold dezactivat implicit, fara apelant de productie.
- Sidecar-ul Spring MCP si bridge-ul prin snapshot exista ca artefacte, dar integrarea end-to-end este blocata de schema v2/v1.

### API si addonuri

- Core-ul publica `AINPCPlatformApi` prin Bukkit `ServicesManager`.
- `ainpc-api` foloseste `apiVersion` separat, il publica in numele/manifestul JAR si are baseline JVM determinist verificat de `:ainpc-api:check` si freeze-ul de release.
- Fatada publica expune serviciile runtime `playerProgression`, `npcEconomy` si un adaptor stabil `storyAuthoring`.
- `StoryAuthoringApi.hasPendingEvents` consulta coada persistenta `story_pending_events`; publicarea muta tranzactional draftul in `story_events`, iar discard-ul il elimina fara publicare.
- `registerObjectiveHandler` are overload-ul SAM `ObjectiveProgressHandler`, acoperit de un test consumer Java.
- `AddonDescriptor` are overload-uri Java, iar un fixture consumer verifica descriptorul, registry-ul si lifecycle-ul `onLoad`/`onEnable`/`onDisable`.
- `AddonRegistry` implementeaza gate de dependinte la register, lifecycle cu cleanup si rollback la inlocuire, cascada tranzitiva a dependentilor la unregister/reconfigurare, reload feature-pack reconciliat, `removeByOrigin`, shutdown best-effort si selectie de scenariu.
- `IntegrationRegistry` comite adaptoarele externe dupa activare, ruleaza lifecycle-ul la inlocuire, restaureaza adaptorul anterior la esec si curata best-effort la shutdown.
- `FeaturePackLoader` incarca recursiv pack-uri, valideaza metadata si inregistreaza descriptori `feature-pack`.
- Addonul medieval isi gestioneaza config-ul si instaleaza automat `medieval`, `social` si `medieval_quest`.
- Callback-urile story, relationship, salary, season si NPC state au producatori core; tranzitiile NPC identice, respinse sau restaurate din persistenta nu emit notificare runtime.
- Dispatcherul addon izoleaza si logheaza erorile per callback si continua sigur daca un addon se dezregistreaza in timpul livrarii.

### Permisiuni si compatibilitate pluginuri

- Runtime-ul foloseste `hasPermission` Bukkit/Paper si nu stocheaza grupuri sau permisiuni de player.
- `plugin.yml` declara nodurile GUI folosite, iar comenzile mixte aplica gate-ul in handlerul subcomenzii.
- Quest log-ul public este separat de formularele creator si quest map prin `ainpc.creator` si noduri specializate.
- `GuiService.handleClick` reaplica `canOpen` pentru cheia sesiunii inainte de actiune.
- LuckPerms este compatibil indirect prin Bukkit; nu exista integrare API LuckPerms.
- Vault este `softdepend` si primeste un provider Economy reflexiv pentru soldurile AINPC; bank, fractionar si mutatii offline nu sunt implementate.
- Nu exista adaptoare runtime WorldEdit, WorldGuard, PlaceholderAPI, Citizens sau ProtocolLib.

### Date si audit

- SQLite este backend implicit, cu foreign keys, busy timeout, WAL si synchronous NORMAL.
- MySQL/MariaDB selecteaza dialectul MySQL, Connector/J si Hikari; suportul este initial si pastreaza o singura conexiune serializata.
- Startup-ul creeaza schema curenta si adauga un set limitat de coloane compatibile; nu exista registru versionat de migrari.
- `/ainpc migration households` executa un backfill de business, nu o migrare de schema sau backend.
- Scriptul de release arhiveaza fisiere, manifest si hash-uri si poate face restore-check prin extractie.
- `/ainpc audit` produce rapoarte read-only text sau JSON schema v1; sectiunea DB descopera tabelele prin metadata JDBC si compara cele 28 de tabele active cu un catalog pe noua domenii. `all full/offline` parcurge detaliat tot istoricul spawn prin paginare keyset de 200 si publica sumarul in `spawn_history`. Verdicturile au coduri stabile `PASS=0`, `WARN=1`, `FAIL=2`, iar wrapperul RCON valideaza contractul si propaga rezultatul ca exit code de proces.
- `/ainpc debugdump all|npc` creeaza exporturi in mod `standard` sau `privacy-safe`; redaction-ul comun este aplicat dupa serializare, fiecare artefact este limitat la 4 MiB, iar `latest.log` este citit ca tail de maximum 250 linii/512 KiB. Exporturile gestionate au retentie configurabila dupa varsta, numar si bytes, protejeaza exportul curent si includ un manifest versionat de confidentialitate.
- `RecentPublicEventListener` conecteaza explicit toate cele 33 de evenimente Bukkit concrete din API la un `RecentEventsBuffer` bounded; exportul separa metadata runtime in `recent-api-events.txt` de extractul persistent `story_events` din `recent-story-events.txt`.
- Scripturile de smoke si raportare acopera cazuri cheie, dar unele sunt numai executori/checklisturi si nu dovedesc semantic PASS-ul.

## Ce este partial

- Modularizarea publica este in curs.
- Runtime-ul extensibil pentru scenarii mai complexe este in curs.
- DTO-urile API `SettlementPlan` si `SettlementGenerationPlan` nu au consumer sau executor de productie; agregatul `SettlementPlan` este pastrat compatibil, dar marcat `@Deprecated(WARNING)` ca scaffold neexecutabil, fara `ReplaceWith` sau termen de eliminare.
- `PopulationPlan` este preview persistent JSON, regasit si selectat explicit dupa `planId` per regiune; campurile sale narrative supravietuiesc conversiei in `HouseAllocation`/`NpcSpawnPlan` si pot fi persistate in `npc_profiles.profile_data.narrative`, iar tema descriptiva vine din tagul regional `theme:<id>` sau foloseste fallback-ul `generic`, dar comanda de spawn regenereaza alte alocari si nu executa selectia.
- Nu exista constructor de blocuri, adaptor WorldEdit, schematic paste sau undo pentru worldgen.
- Importul, auto-place-ul, patch apply si fixture apply nu au tranzactie persistenta sau undo dupa succes; compensarea lor acopera numai esecul operatiei curente.
- Fixture-ul runtime foloseste in continuare lume si coordonate hardcodate.
- Simularea si rutina sunt schedulere separate, ambele dezactivate implicit prin flagurile locale; nu exista o clasa `SimulationService` sau un motor unificat de sat.
- `RoutineCoordinator` nu aplica inca pause/custom override, iar evenimentul de rutina nu are consumator core pentru quest sau story.
- Rutina foloseste pathfinding direct spre ancora, nu reteaua semantica de drumuri; household-ul persistent nu este sursa de decizie pentru rutina.
- `NpcSimulationMode` si `NpcInteractionProfile` nu sunt aplicate de consumatori, iar modurile `FULL/LIGHT` nu au politici distincte de persistenta.
- Metadata de lifecycle a actorilor de scenariu nu este rehidratata dupa restart; `TEMPORARY` nu este inclus in cleanup-ul de unregister.
- Salvarea `npc_world_bindings` nu valideaza semantic place/node; validarile extinse sunt in comenzile de audit si repair.
- Coada story pending are operatii core explicite, dar nu are inca producator automat din AI sau scheduler; fluxurile `recordEvent` si `applyTemplate` publica direct.
- `registerDescriptor` pastreaza conflictele globale intre scenarii primare ca declaratii diagnostice si emite warning; selectia ramane determinista, spre deosebire de gate-ul `registerAddon`, care respinge candidatul de cod.
- `addons.load_order` sorteaza descriptorii si nu controleaza ordinea de activare Paper.
- Politica de permisiuni ramane distribuita intre handler, cheia GUI si gate-ul final; nu exista inca un catalog unic pentru toate operatiile.
- Vault nu are smoke cu server real, selectie multiprovider sau suport de mutatii offline.
- Mutatiile world nu consulta automat un provider extern de protectie precum WorldGuard.
- API-ul este Kotlin-first; verificarea ABI automata exista, dar ergonomia Java completa si compatibilitatea comportamentala necesita in continuare teste dedicate.
- `NPCChatListener` anuleaza in prezent si mesajele fara tinta NPC, iar sesiunile active nu revalideaza distanta la fiecare mesaj.
- Sesiunea de conversatie si playerul din `NPCContext` au cleanup separat.
- Reactiile de dialog, story si relatiile NPC-NPC nu formeaza un motor unic; reactia story nu persista explicit toate mutatiile aplicate obiectului.
- Cache-ul `EnvironmentEngine` nu are un apel periodic conectat in `src/main` si poate ramane stale.
- Orchestration-ul AI nu este conectat la dialog, authoring sau executori deterministi.
- Bridge-ul MCP runtime are schema read-only aliniata pana la v3; smoke-ul producer-reader, tool ownership si hardening-ul write tools raman deschise.
- MySQL/MariaDB nu are o suita de integrare pe server real, iar auditul static nu demonstreaza productie.
- Nu exista upgrade/downgrade versionat al schemei sau migrare automata SQLite -> MySQL.
- Scriptul de backup nu opreste Paper, nu valideaza SQLite, nu face restore Paper si nu captureaza MySQL.
- Scripturile de deploy remote sunt specifice mediului si nu formeaza un pipeline portabil aprobat.
- Exista un registru bounded de metrici, health machine-readable si raport audit JSON; nu exista tracing, Prometheus sau istoric persistent, iar cleanup-ul dump-urilor ruleaza la export, nu periodic.
- `RecentEventsBuffer` este volatil, nu pastreaza payload-uri si se goleste la restart, iar modul `privacy-safe` ramane best-effort pentru nume offline necunoscute si text liber nestructurat.
- Nu exista in repository un bot Mineflayer sau un runner echivalent de scenarii player.
- Accesul GUI este impartit intre `canOpen`, hub-uri si comenzile finale; click-ul reaplica cheia, dar nu revalideaza automat targetul ori starea de business.
- `GuiService.open` nu impune singur `features.gui`, iar navigarea nu are stiva de parinti; confirmarea centrala acopera comenzile clasificate de mapping, nu orice actiune de business din toate ecranele.
- Formularele GUI prin chat nu au timeout, iar onboarding-ul dedicat de profil/starter kit nu este implementat.

## Ce trebuie evitat

- Reimplementarea unor servicii deja existente fara motiv clar.
- Dublarea logicii dintre quest, progression si story.
- Folosirea documentelor vechi ca sursa unica de adevar cand exista v2.

## Regula de lucru

- daca o functionalitate exista deja, documentatia trebuie sa o trateze ca punct de plecare, nu ca backlog nou;
- analizele si sumarurile derivate trimit aici si nu redefinesc separat starea proiectului.
