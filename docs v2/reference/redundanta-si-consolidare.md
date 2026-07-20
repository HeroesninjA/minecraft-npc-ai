# Redundanta si consolidare

Status: registru derivat al deciziilor de documentatie.
Actualizat: 2026-07-16.

## Scop

- identifica documentele care repeta acelasi contract;
- pastreaza un singur owner canonic pe tema;
- muta in arhiva numai continutul superseded de o alternativa implementata, incompatibil sau inchis istoric;
- pastreaza ideile compatibile, dar neimplementate, ca propuneri in `planning/`.

## Decizii aplicate

- `canonical/constitutie-proiect.md` este unica sursa pentru regulile proiectului;
- `canonical/constitusional.md` este doar alias de compatibilitate;
- `canonical/implementat-deja.md` este unica sursa pentru starea confirmata;
- `reference/analiza-stare-dezvoltare.md` si `reference/sumar-implementare-demo.md` sunt derivate;
- interactiunea, dialogul si reactiile au contracte distincte, iar `architecture/interactiune-dialog-reactie-stack.md` este index;
- reactiile de dialog si story nu sunt prezentate drept motor unic;
- relatia player-NPC din `DialogManager` si relatia NPC-NPC din `RelationshipService` sunt documentate separat;
- `EnvironmentEngine` detine numai contextul environment per lume, nu mapping-ul semantic;
- story state, story context si consumul AI au contracte distincte, iar `reference/story-context-quest-ai-stack.md` este index;
- dialogul AI activ si scaffold-ul `AIOrchestrationService` sunt documentate separat;
- sidecar-ul Spring, bridge-ul runtime si catalogul tool-urilor au owneri distincti, iar `reference/ai-orchestrare-mcp-stack.md` este index;
- `reference/mcp-runtime-gap-checklist.md` este singurul backlog MCP runtime activ;
- cele patru trackere MCP redundante au fost consolidate in `archive/mcp-runtime-gap-tracker-legacy.md`;
- analiza GLM depasita a fost mutata in `archive/mcp-analiza-glm-5.2-legacy.md`;
- AI-builder-ul ramane propunere activa in `planning/generare-ai-si-constructie-automata.md`, separata de planner-ele deterministe implementate;
- pentru mapping, `architecture/mapping.md` detine modelul, ghidul detine procedura, iar `reference/mapping-stack.md` este index;
- aliasul vechi despre mapping ulterior a fost eliminat, iar redirectul trimite la contractul canonic;
- pentru questuri, `planning/questuri-avansate-v2.md` este roadmap-ul tehnic principal, `planning/quest-evolution-stack.md` este index, iar conceptele de continut compatibile pot ramane propuneri separate;
- `reference/objective-types-reference.md` este unicul catalog de tipuri, iar `reference/objective-examples.md` contine exemple;
- ghidul duplicat de quest progression si nota generica de lucru alternat au fost eliminate.
- `architecture/simulation-service.md` documenteaza runtime-ul distribuit; nu exista o clasa `SimulationService` de productie;
- cele trei faze speculative `Simulation Service` au fost consolidate in `archive/simulation-service-extraction-concept.md`;
- `reference/simulation-stack.md` ruteaza doar spre contracte active si roadmap-ul explicit;
- `architecture/simulation-service.md` detine nevoi/stare, iar `architecture/comportament-natural-npc-rutine-alocari.md` detine atribuirea si miscarea;
- world bindings, households si lifecycle-ul actorilor au owneri separati si nu sunt prezentati drept motor unic de sat;
- planul generic pentru Server NPC MVP a fost mutat in `archive/server-npc-mvp-legacy.md`; scope-ul, criteriile si roadmap-ul au owneri activi distincti.
- `architecture/settlement-plan.md` separa cele patru modele de plan si elimina promisiunea unui commit runtime inexistent;
- `architecture/generare-populatie-narativa.md` este marcata preview neconectat la spawn;
- `architecture/generare-sate-fara-worldedit.md` documenteaza mapping semantic, nu constructie de blocuri;
- pipeline-ul WorldEdit ramane propunere activa in `planning/generare-sate-worldedit-si-npc.md`, deoarece nu are o alternativa fizica implementata si este compatibil cu proiectul;
- schema generica de structuri ramane propunere activa in `planning/schema-authoring-structuri-world.md`; registrele hardcodate sunt baseline, nu inlocuitor complet;
- schema declarativa veche de fixture ramane in `archive/controlled-test-fixture-schema-concept.md` deoarece fixture-ul hardcodat este alternativa implementata curenta;
- questul Castelului ramane concept activ in `planning/quest-blastemul-castelului.md`, fara afirmatii de implementare;
- `architecture/structuri-exterioare-satului.md`, `reference/template-cladiri-si-marker-nodes.md` si runbook-ul fixture-ului au owneri activi distincti.
- `reference/documentatie-api.md` detine suprafata publica verificata, iar `reference/addon-developer-guide.md` este doar ghid de consum;
- `architecture/harta-clase-addons.md` detine lifecycle-ul cu rollback, cascada dependentilor, reconcilierea reload-ului si dispatch-ul izolat al addonurilor de cod si le separa de descriptorii feature pack;
- `reference/scenario-pack-schema.md` detine schema citita de loader; documentele JSON/YAML nu o redefiniesc;
- `reference/kotlin-interop-api-addonuri.md` detine starea ABI, iar `reference/kotlin-paper-packaging-si-smoke.md` checklistul operational;
- golurile compatibile ale API-ului sunt pastrate in `planning/stabilizare-api-si-addonuri.md`, nu mutate in arhiva;
- referintele Maven si afirmatia ca `ainpc-api` este Java au fost eliminate deoarece sunt incompatibile cu build-ul Gradle si sursele Kotlin actuale.
- `reference/storage-runtime.md` detine comportamentul implementat, iar `planning/storage-provider-roadmap.md` numai maturizarea neimplementata;
- `operations/migration-si-backup.md` este unicul owner pentru backup, restore si backfill; procedura scurta de demo a fost eliminata;
- planul remote compatibil a fost mutat in `planning/testare-si-deploy-remote.md`, nu arhivat;
- rapoartele de remediere din 2026-07-05 au fost mutate in arhiva deoarece sunt incidente inchise, nu pentru ca ar descrie lucru neimplementat;
- `operations/server-credentials.md` este politica fara secrete si nu mai pretinde ca datele reale apartin documentatiei urmarite.
- cele sapte checklisturi D1-D9 au fost consolidate in `operations/demo-server-verification.md`; gate-urile raman active si nu au fost arhivate;
- planul Mineflayer a fost mutat in `planning/testare-automata-bot.md` deoarece nu exista implementare, dar ideea ramane compatibila;
- nota generica de performanta a devenit roadmap-ul masurabil `planning/performance-hardening.md`, fara afirmatii de benchmark inexistent;
- analiza generica de erori a fost eliminata; triage-ul are owner operational in `operations/debugging-si-testare.md`;
- auditul, observabilitatea si harta de clase separa acum comportamentul implementat de golurile active din `planning/diagnostic-si-observabilitate-roadmap.md`;
- exporterul `DebugDumpService` a fost reconectat la `/ainpc debugdump all|npc`, corectand regresia de rutare fara a elimina sumarurile noi.
- `reference/gui-stack.md` este owner-ul unic pentru sesiuni, acces, click, confirmare, input text si navigare GUI;
- harta GUI a fost mutata in `architecture/harta-clase-gui.md`, iar ghidurile nu mai pretind ownership structural;
- `planning/gui-ux-hardening.md` separa autorizarea, navigarea, formularele si testarea neimplementate de contractul curent;
- onboarding-ul de profil a fost mutat in `planning/player-onboarding-initiere.md`; aliasul quest `onboarding:T01` ramane baseline implementat distinct;
- cleanup-ul GUI la quit include acum si selectia de oferta, selectia shop si ultima cheie de resume.
- `reference/sistem-permisiuni-compatibilitate-pluginuri.md` este owner-ul unic pentru noduri, roluri derivate si compatibilitatea third-party verificata;
- LuckPerms este documentat ca provider indirect Bukkit, Vault ca provider Economy concret, iar WorldEdit/WorldGuard/PlaceholderAPI/Citizens raman explicit fara adaptor verificat;
- hardening-ul compatibil ramas a fost pastrat in `planning/authorization-and-plugin-compatibility-hardening.md`, nu arhivat;
- `ainpc.gui.quest` nu mai deschide suprafete creator sau quest map, iar toate nodurile GUI folosite sunt declarate in descriptor.

## Regula

Cand doua pagini spun acelasi lucru, una devine owner, iar cealalta devine index sau ghid. Arhivarea cere suplimentar o alternativa implementata, incompatibilitate sau caracter istoric inchis.
