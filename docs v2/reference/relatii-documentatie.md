# Relatii documentatie

Status: index canonic al relatiilor dintre pagini.
Actualizat: 2026-07-16.

## Tipuri

- `canonic` - stabileste contractul principal;
- `derivat` - explica un contract fara sa-l redefineasca;
- `istoric` - continut retras din uz;
- `index` - traseu de navigare;
- `operational` - runbook, checklist sau ghid;
- `roadmap` - idee compatibila sau lucru ramas, fara afirmatie de implementare.

## Harta principala

- `canonical/constitutie-proiect.md` detine regulile proiectului;
- `canonical/implementat-deja.md` detine statusul confirmat;
- `architecture/mapping.md` detine modelul `Region -> Place -> Node`, iar `reference/mapping-stack.md` este index;
- `architecture/progression-service.md` detine progresia, `reference/objective-types-reference.md` catalogul si `reference/quest-anchor-bindings.md` persistenta ancorelor;
- `architecture/story-state-service.md` detine scrierile story, iar `architecture/story-context-service.md` proiectia read-only;
- `architecture/interactiuni.md` detine inputul si sesiunea, `architecture/dialog-si-conversatii.md` replica si post-procesarea, iar `architecture/reactie-npc-jucator.md` separa mutatiile de dialog, story si NPC-NPC;
- `architecture/interactiune-dialog-reactie-stack.md` este doar indexul acestor fluxuri;
- `architecture/environment-context-si-engine.md` detine snapshot-ul environment per lume, nu mapping-ul semantic;
- `architecture/simulation-service.md` detine cele doua fluxuri runtime verificate, iar `reference/simulation-stack.md` este numai index;
- `architecture/comportament-natural-npc-rutine-alocari.md` detine atribuirea si miscarea, iar `architecture/evita-comportamentul-robotic-prin-rutine-staggered.md` numai variatia temporala implementata;
- `architecture/npc-world-bindings.md` detine ancorele persistente, iar `architecture/households-persistente.md` rezultatul persistent al alocarii de spawn; niciunul nu redefineste mapping-ul;
- `architecture/settlement-plan.md` delimiteaza DTO-ul API, `HouseAllocation`, `PopulationPlan` si `PatchPlan`, iar `guides/ordine-spawn-npc-cladiri-region-node.md` detine procedura operationala;
- `architecture/generare-populatie-narativa.md` detine preview-ul partial, nu spawn-ul activ;
- `architecture/generare-sate-fara-worldedit.md` detine scanarea si mutatiile semantice, iar `planning/patch-planner.md` capabilitatile exacte ale patch-urilor;
- `reference/template-cladiri-si-marker-nodes.md` detine template-urile semantice, iar `architecture/structuri-exterioare-satului.md` catalogul si validarea exteriorului;
- `architecture/worldedit-integration-contract.md` este contract de roadmap neimplementat, nu descriere de runtime;
- `planning/generare-sate-worldedit-si-npc.md` pastreaza pipeline-ul fizic propus, iar `planning/schema-authoring-structuri-world.md` schema viitoare de authoring;
- `planning/generare-ai-si-constructie-automata.md` pastreaza AI build draft ca propunere cu executor determinist obligatoriu;
- `planning/quest-blastemul-castelului.md` este concept de continut, nu definitie runtime;
- `operations/mediu-test-controlat-sat-si-structuri-exterioare.md` detine fixture-ul runtime, iar `operations/test-fixtures-and-demo-world.md` este index;
- `architecture/npc-uri-temporare-si-episodice.md` detine limitele lifecycle-ului de scenariu;
- `architecture/simulare-sat-si-lume.md` si `planning/npc-population-world-stack.md` sunt vederi derivate, iar `planning/rutine-npc-si-timeline.md` este roadmap;
- `architecture/story-si-context-ai.md` descrie fluxul activ de consum, nu o noua autoritate;
- `architecture/ai-orchestrare-si-mecanici.md` documenteaza scaffold-ul, nu pipeline-ul de dialog;
- `architecture/spring-ai-mcp-serviciu-intern.md` detine limita sidecar-ului, iar `architecture/mcp-runtime-bridge-design.md` transportul;
- `reference/mcp-tools-catalog.md` detine lista tool-urilor, iar `reference/mcp-runtime-gap-checklist.md` singurul backlog MCP runtime;
- `reference/documentatie-api.md` detine suprafata publica verificata, `reference/api-versioning-and-abi.md` politica SemVer si gate-ul ABI, iar `architecture/harta-clase-addons.md` lifecycle-ul cu rollback, cascada dependentilor si fluxurile code-addon/feature-pack;
- `reference/addon-developer-guide.md`, `reference/addon-config-template.md` si `reference/kotlin-paper-packaging-si-smoke.md` sunt ghiduri derivate;
- `reference/scenario-pack-schema.md` detine schema feature pack, iar `reference/json-yaml-contract.md` delimiteaza doar normalizarea formatelor;
- `reference/kotlin-interop-api-addonuri.md` detine ergonomia Kotlin/Java curenta, iar `planning/stabilizare-api-si-addonuri.md` golurile compatibile neimplementate;
- `architecture/harta-clase-platform-db.md` este harta structurala, `reference/storage-runtime.md` detine persistenta verificata, iar `planning/storage-provider-roadmap.md` lucrul compatibil ramas;
- `operations/migration-si-backup.md` detine procedura de backup, restore si backfill, fara a redefini schema;
- `operations/server-admin-runbook.md` si `operations/release-checklist.md` consuma contractele de storage si packaging;
- `planning/testare-si-deploy-remote.md` ramane roadmap pana la existenta unui pipeline portabil aprobat;
- `operations/audit.md` detine limitele auditului runtime, `operations/observability-and-logs.md` suprafetele de diagnostic, iar `architecture/harta-clase-debug.md` ownership-ul claselor;
- `operations/debugging-si-testare.md` detine triage-ul, iar `planning/diagnostic-si-observabilitate-roadmap.md` imbunatatirile compatibile ramase;
- `operations/demo-server-verification.md` este gate-ul unic D1-D9, iar `planning/testare-automata-bot.md` propunerea de automatizare fara implementare curenta;
- `planning/performance-hardening.md` detine numai riscurile si masuratorile propuse, nu rezultate de benchmark;
- `archive/remediere-deploy-2026-07-05.md` si `archive/remediere-final-2026-07-05.md` sunt snapshot-uri istorice inchise;
- `reference/gui-stack.md` detine contractul infrastructurii GUI, iar `architecture/harta-clase-gui.md` ownership-ul claselor;
- `guides/gui-interfete.md`, `guides/tutorial-gui-creator.md` si `guides/gui-admin-mapping-quest.md` sunt ghiduri derivate;
- `planning/gui-ux-hardening.md` detine imbunatatirile GUI compatibile ramase;
- `reference/sistem-permisiuni-compatibilitate-pluginuri.md` detine autorizarea Bukkit si matricea integrarilor verificate;
- `planning/authorization-and-plugin-compatibility-hardening.md` detine numai hardening-ul de permisiuni, protectie world si integrari ramas;
- `planning/player-onboarding-initiere.md` este roadmap pentru profil/initiere si nu redefineste scenariul quest `onboarding:T01`;
- `reference/story-context-quest-ai-stack.md`, `reference/ai-orchestrare-mcp-stack.md` si `planning/quest-evolution-stack.md` sunt indexuri;
- `operations/audit-constitutie-proiect.md`, `operations/release-checklist.md` si `operations/debugging-si-testare.md` sunt documente operationale dependente.

## Regula

Documentele istorice si roadmap-urile nu pot suprascrie contractele verificate in cod. O idee compatibila nu devine istorica doar pentru ca nu este implementata.
