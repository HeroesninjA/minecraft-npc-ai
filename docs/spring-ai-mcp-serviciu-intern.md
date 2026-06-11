# Spring AI MCP Ca Serviciu Intern

Actualizat: 2026-06-04

## Scop

Acest document defineste directia pentru adaugarea unui serviciu intern Spring AI MCP langa pluginul AINPC.

Scopul functional principal este cresterea contextului folosit in dialogurile NPC, astfel incat raspunsurile sa tina cont de NPC, player, questuri, story state, mapping semantic, rutine, memorie si evenimente recente.

Scopul secundar este pregatirea unei baze curate pentru features AI viitoare: authoring de questuri, story drafts, recomandari admin, debug explicabil, reactii sociale si generare asistata.

Scopul tehnic nu este sa incarcam Spring Boot direct in JAR-ul Paper, ci sa avem un serviciu sidecar local care expune tool-uri MCP pentru AI, iar pluginul Paper sa consume rezultatele printr-un contract controlat.

Ideea principala:

```text
Paper plugin ramane runtime-ul de joc.
Spring AI MCP sidecar expune capabilitati AI/tooling.
Dialogurile NPC primesc context mai bogat, dar validat.
Serviciile deterministe din core valideaza si executa.
```

## Context

Spring AI MCP ofera suport pentru servere si clienti MCP, cu tool-uri, resources, prompts si transporturi precum STDIO, SSE, Streamable HTTP si stateless HTTP.

Referinte oficiale:

- Spring AI MCP overview: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
- Spring AI MCP Server Boot Starter: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html
- Spring AI MCP Annotations: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-overview.html

## Beneficii

| Beneficiu | Explicatie |
|---|---|
| Separare de Paper | Spring Boot si dependintele AI nu incarca classloader-ul Bukkit/Paper. |
| Restart izolat | Sidecar-ul MCP poate fi restartat fara restart complet de server Minecraft. |
| Tool-uri AI standard | AI-ul poate consuma tool-uri prin MCP in loc de apeluri custom imprastiate. |
| Dialog NPC mai contextual | Raspunsurile pot folosi snapshot-uri despre NPC, player, questuri, story si mapping fara sa trimita toata baza de date in prompt. |
| Baza pentru features viitoare | Aceeasi structura poate sustine ulterior story drafts, quest authoring, recomandari admin si debug explicabil. |
| Testare mai simpla | Serviciul MCP poate avea teste Spring normale, fara server Paper live. |
| Extensibilitate | Se pot adauga tool-uri pentru NPC, quest, world mapping, debug si story fara sa marim API-ul public Paper prea repede. |
| Control operational | Transportul ramane pe `127.0.0.1`, cu timeout, token local si feature flag. |

## De Ce Sidecar, Nu Spring Boot In Plugin

Nu recomand includerea Spring Boot/Spring AI direct in `ainpc-core-plugin`.

Motive:

- JAR-ul Paper devine mult mai mare.
- Riscul de conflicte de dependinte creste.
- Lifecycle-ul Spring Boot si lifecycle-ul Paper nu sunt identice.
- Reload-ul Paper poate lasa thread-uri, porturi sau contexte Spring intr-o stare incerta.
- Un web server intern pornit din plugin poate bloca shutdown-ul sau poate conflictua cu alte servicii.
- Debugging-ul devine greu: o eroare AI/MCP poate opri pluginul de gameplay.

Modelul recomandat:

```text
ainpc-core-plugin
  -> McpRuntimeClient
  -> http://127.0.0.1:<port>/mcp
  -> ainpc-mcp-service
  -> Spring AI MCP tools/resources/prompts
```

## Arhitectura Propusa

```text
Minecraft / Paper
  -> AINPCPlugin
  -> AIOrchestrationService
  -> McpRuntimeClient
  -> Local Spring AI MCP Service
  -> Tool-uri MCP
  -> raspuns structurat
  -> validator AINPC
  -> serviciu determinist: Quest, Story, World, NPC, Debug
```

Regula de baza:

```text
AI-ul propune si explica.
MCP-ul expune tool-uri.
Core-ul valideaza si executa.
```

## Obiective Functionale Pentru Context Si Dialog

Primul consumator real al serviciului MCP trebuie sa fie dialogul NPC, nu write tools sau generarea automata.

Obiective:

| Obiectiv | Rezultat dorit |
|---|---|
| Context NPC compact | Dialogul primeste identitate, profesie, stare, emotii, familie, rutina si relatii relevante. |
| Context player limitat | Dialogul poate tine cont de questuri active, reputatie viitoare, progres si interactiuni recente. |
| Context de lume | NPC-ul poate mentiona regiuni, places, nodes, probleme locale si story state fara coordonate brute inutile. |
| Context de scenariu | Raspunsurile pot respecta pack-ul/addon-ul activ si tonul scenariului medieval sau al altui addon. |
| Context temporal | Dialogul poate tine cont de rutina, ora jocului, evenimente recente si cooldown-uri. |
| Explicabilitate admin | Adminul poate vedea ce context a fost folosit si de ce un raspuns a fost ales. |
| Extensie viitoare | Acelasi mecanism poate alimenta quest drafts, story drafts, recomandari si debug AI. |

Regula:

```text
MCP aduna si structureaza context.
OpenAIService sau providerul AI genereaza raspunsul.
Core-ul decide ce este permis si ce se persista.
```

Snapshot-ul pentru dialog trebuie sa fie mic, redactat si versionat. Nu se trimite DB complet, nu se trimit secrete si nu se trimit liste nelimitate de NPC-uri sau evenimente.

## Module Propuse

| Modul | Rol |
|---|---|
| `ainpc-core-plugin` | Plugin Paper, runtime gameplay, validare, persistenta si comenzi. |
| `ainpc-api` | Contract Java-friendly pentru addonuri si integrari stabile. |
| `ainpc-scenario-medieval` | Addon de continut si scenarii. |
| `ainpc-mcp-service` | Serviciu Spring Boot + Spring AI MCP, pornit separat. |
| `ainpc-mcp-contract` optional | DTO-uri comune daca apare nevoie reala de contract partajat. |

Initial, evita `ainpc-mcp-contract` daca DTO-urile pot ramane JSON simple si versionate.

## Structura Usor De Eliminat

Integrarea Spring AI MCP trebuie proiectata ca o extensie optionala, nu ca o dependinta structurala a core-ului.

Obiectiv:

```text
Daca MCP nu mai este dorit, stergem modulul sidecar, oprim feature flag-ul si core-ul ramane functional.
```

Reguli de izolare:

| Regula | Motiv |
|---|---|
| `features.mcp=false` implicit | Pluginul trebuie sa porneasca si sa functioneze fara MCP. |
| `ainpc-core-plugin` nu depinde de Spring Boot sau Spring AI | Eliminarea MCP nu trebuie sa schimbe classpath-ul Paper. |
| Toate clasele de client stau intr-un pachet izolat, de exemplu `ro.ainpc.mcp` | Stergerea este mecanica si usor de verificat. |
| `AIOrchestrationService` vede doar o interfata mica, nu implementarea MCP | Se poate inlocui cu `NoopMcpRuntimeClient`. |
| DTO-urile raman JSON simple sau contracte interne mici | Nu expune tipuri Spring AI in API-ul public AINPC. |
| Nu se adauga tabele DB obligatorii pentru MVP | Dezinstalarea nu cere migration de rollback. |
| Nu se adauga comenzi obligatorii care depind de MCP | Comenzile existente trebuie sa aiba fallback local. |
| Nu se adauga dependinte MCP in `ainpc-api` | Addonurile nu trebuie recompilate daca MCP este scos. |
| Sidecar-ul ruleaza separat si nu este copiat in `plugins/` | Serverul Paper ramane instalabil fara serviciul MCP. |

Interfata minima recomandata in core:

```text
McpRuntimeClient
  health()
  callTool(toolName, payload)

NoopMcpRuntimeClient
  returneaza indisponibil/fallback

HttpMcpRuntimeClient
  exista doar cand features.mcp=true
```

Checklist pentru eliminare:

1. Seteaza `features.mcp=false`.
2. Sterge modulul `ainpc-mcp-service` din `settings.gradle` si build.
3. Sterge pachetul izolat `ro.ainpc.mcp`, daca nu mai este folosit.
4. Pastreaza sau inlocuieste interfata cu `NoopMcpRuntimeClient`, daca `AIOrchestrationService` o foloseste.
5. Sterge cheile `mcp.*` din config doar dupa ce serverul a pornit o data fara MCP.
6. Ruleaza testele de config si Paper smoke cu sidecar oprit.
7. Confirma ca `/ainpc audit all` nu raporteaza dependency lipsa pentru MCP.

Gate de acceptare:

- core-ul compileaza fara modulul `ainpc-mcp-service`;
- JAR-ul Paper nu contine clase Spring Boot/Spring AI;
- serverul porneste cu `features.mcp=false`;
- AI-ul existent foloseste fallback local sau providerul direct;
- niciun addon nu depinde de MCP.

## Config Propus

In `config.yml`:

```yaml
features:
  ai: true
  mcp: false

mcp:
  enabled: false
  base_url: "http://127.0.0.1:39841/mcp"
  token: ""
  auto_start: false
  connect_timeout_seconds: 5
  read_timeout_seconds: 20
  fail_open: true
```

Semantica:

| Cheie | Rol |
|---|---|
| `features.ai` | Activeaza capabilitatile AI existente. |
| `features.mcp` | Activeaza integrarea runtime cu MCP. |
| `mcp.enabled` | Permite clientului intern sa cheme serviciul MCP. |
| `mcp.base_url` | Endpoint local pentru serviciul Spring AI MCP. |
| `mcp.token` | Token local optional pentru requesturi interne. Nu se logheaza. |
| `mcp.auto_start` | Pornire automata a sidecar-ului. Amanat pana dupa MVP. |
| `mcp.fail_open` | Daca MCP cade, pluginul foloseste fallback local si nu opreste serverul. |

## Tool-uri MCP Initiale

MVP-ul trebuie sa inceapa cu tool-uri read-only si usor de validat.

| Tool | Tip | Scop |
|---|---|---|
| `ainpc.feature.state` | read-only | Raporteaza feature flags si status AI/MCP. |
| `ainpc.server.snapshot` | read-only | Rezumat server: plugin version, NPC count, addon count, world mapping counts. |
| `ainpc.npc.list` | read-only | Lista limitata de NPC-uri, cu filtre sigure. |
| `ainpc.npc.context` | read-only | Context compact pentru un NPC. |
| `ainpc.dialog.context` | read-only | Snapshot contextual pentru dialog: NPC, player, quest/story/mapping/rutina, limitat si redactat. |
| `ainpc.dialog.explain_context` | read-only | Explica adminului ce surse de context au fost folosite intr-un raspuns. |
| `ainpc.quest.summary` | read-only | Rezumat quest/progression pentru un player sau selector. |
| `ainpc.world.mapping.summary` | read-only | Rezumat regiuni, places, nodes. |
| `ainpc.debug.health` | read-only | Health check pentru AI, MCP, DB si addons. |

Tool-uri care pot modifica stare raman faze ulterioare si necesita validare explicita.

## Tool-uri Amanate

| Tool | Motiv de amanare |
|---|---|
| `ainpc.quest.create` | Necesita validator complet si review admin. |
| `ainpc.story.apply` | Poate modifica story state; trebuie audit si rollback. |
| `ainpc.dialog.force_memory` | Poate scrie memorie sau schimba comportament; trebuie reguli de persistenta si audit. |
| `ainpc.world.patch.apply` | Poate modifica lumea; trebuie planner, dry-run si confirmare. |
| `ainpc.npc.spawn` | Poate duplica NPC-uri; trebuie integrare cu spawn order si audit. |
| `ainpc.config.update` | Risc operational; trebuie permisiuni si backup. |

## Reguli De Threading Paper

Serviciul MCP nu atinge direct API-ul Bukkit/Paper.

Reguli:

- orice acces la lume, entitati, playeri sau comenzi Bukkit trece prin scheduler-ul Paper pe main thread;
- serviciul Spring AI MCP lucreaza cu snapshot-uri JSON sau DTO-uri, nu cu obiecte Bukkit;
- requesturile MCP au timeout strict;
- raspunsurile AI/MCP sunt validate in core inainte de executie;
- fallback-ul local ramane obligatoriu cand MCP este indisponibil.

## Securitate

MVP-ul trebuie sa respecte aceste reguli:

- bind doar pe `127.0.0.1`;
- fara port public;
- token local optional in header, daca serviciul va fi consumat de alt proces;
- nu loga `OPENAI_API_KEY`, token MCP sau prompt complet cu date sensibile;
- tool-urile write sunt dezactivate implicit;
- toate requesturile write viitoare trebuie sa produca audit compact;
- payload-urile au limite de dimensiune;
- prompturile primesc context minim, nu dump complet de DB sau lume.

## Faze

### Faza 0 - Decizie Si Contract

Scop:

- stabilirea ca Spring AI MCP ruleaza ca sidecar, nu in JAR-ul Paper;
- stabilirea ca primul use-case este cresterea contextului pentru dialogurile NPC;
- alegerea portului local si a cheilor de config;
- definirea tool-urilor read-only initiale pentru context si diagnostic;
- documentarea relatiei cu `AIOrchestrationService`.

Livrabile:

- acest document;
- config design pentru `features.mcp` si `mcp.*`;
- lista de tool-uri MVP;
- decizie explicita ca write tools sunt amanate.

Gate:

- documentul este legat in `docs/README.md`;
- nu exista schimbari runtime in Paper in aceasta faza.

### Faza 1 - Modul Spring Boot Minimal

Scop:

- adaugarea modulului `ainpc-mcp-service`;
- pornire standalone pe Java compatibil;
- endpoint MCP local;
- health endpoint simplu.

Livrabile:

- `ainpc-mcp-service/build.gradle`;
- aplicatie Spring Boot minima;
- configuratie `application.yml`;
- endpoint local pe `127.0.0.1`;
- tool MCP demonstrativ `ainpc.ping`.

Gate:

- `.\gradlew.bat :ainpc-mcp-service:test` trece;
- serviciul porneste si raspunde local;
- nu este necesar server Paper live.

### Faza 2 - Client Intern In Plugin

Scop:

- adaugarea unui client HTTP/MCP in `ainpc-core-plugin`;
- feature flag `features.mcp`;
- fallback cand serviciul nu ruleaza.

Livrabile:

- `McpRuntimeClient`;
- `McpRuntimeConfig`;
- health check prin `/ainpc debug` sau `/ainpc audit`;
- timeout si mesaje clare pentru admin.

Gate:

- daca `features.mcp=false`, pluginul se comporta identic cu starea curenta;
- daca `features.mcp=true` si serviciul lipseste, pluginul nu pica;
- testele de config si resolver trec.

### Faza 3 - Tool-uri Read-only AINPC

Scop:

- expunerea informatiilor runtime prin tool-uri sigure, cu prioritate pe contextul de dialog.

Livrabile:

- `ainpc.feature.state`;
- `ainpc.server.snapshot`;
- `ainpc.dialog.context`;
- `ainpc.dialog.explain_context`;
- `ainpc.debug.health`;
- `ainpc.world.mapping.summary`;
- contract JSON versionat.

Gate:

- tool-urile nu modifica DB sau lume;
- datele sunt limitate si redactate;
- exista teste pentru payload-uri si fallback.

### Faza 4 - Integrare Cu AIOrchestrationService

Scop:

- folosirea MCP ca sursa de context pentru dialogurile NPC;
- extinderea treptata spre tool calls si context pentru alte features AI;
- pastrarea validarii in core.

Livrabile:

- integrare controlata in `AIOrchestrationService`;
- consum initial in fluxul de dialog NPC;
- selectie intre provider direct si MCP;
- loguri de diagnostic fara secrete;
- fallback local.

Gate:

- `features.ai=true` si `features.mcp=true` sunt necesare pentru calea MCP;
- raspunsurile MCP sunt validate inainte de consum;
- Paper smoke porneste cu si fara sidecar.

### Faza 5 - Tool-uri De Authoring Cu Review Admin

Scop:

- permiterea generarii de drafturi, nu executie directa.

Livrabile:

- `ainpc.quest.draft`;
- `ainpc.story.draft`;
- `ainpc.dialog.suggest`;
- export sau preview pentru admin;
- audit compact pentru drafturi.

Gate:

- niciun draft nu ajunge live fara validare;
- adminul vede diferentele si poate respinge;
- exista rollback sau non-persistenta implicita.

### Faza 6 - Write Tools Controlate

Scop:

- adaugarea limitata a tool-urilor care modifica stare, doar dupa ce read-only si authoring sunt stabile.

Livrabile posibile:

- `ainpc.story.apply` cu allowlist;
- `ainpc.quest.install_draft` dupa validare;
- `ainpc.world.patch.plan` si eventual `apply` doar cu confirmare.

Gate:

- backup operational documentat;
- audit runtime;
- permisiuni admin;
- dry-run obligatoriu;
- teste Paper smoke si rollback.

## Definitia De Gata Pentru MVP

MVP-ul Spring AI MCP este gata cand:

| Zona | Criteriu |
|---|---|
| Modul | `ainpc-mcp-service` exista si porneste standalone. |
| Transport | endpoint MCP local ruleaza pe `127.0.0.1`. |
| Config | `features.mcp` si `mcp.*` exista in config, implicit off. |
| Plugin | `ainpc-core-plugin` poate verifica health MCP fara sa pice. |
| Tool | exista cel putin `ainpc.feature.state` read-only. |
| Dialog | exista cel putin un snapshot `ainpc.dialog.context` consumabil de dialog sau inspectabil in debug. |
| Fallback | pluginul functioneaza normal cand sidecar-ul este oprit. |
| Securitate | nu exista bind public si nu se logheaza secrete. |
| Teste | Gradle tests si Paper smoke trec. |

## Riscuri

| Risc | Control |
|---|---|
| Classloader conflict Paper | Spring AI ramane in sidecar. |
| Port local ocupat | Config port explicit si health check. |
| AI/MCP blocheaza tick-ul serverului | Timeout strict si executie asincrona. |
| Acces Bukkit din thread gresit | Snapshot-uri si scheduler Paper pe main thread. |
| Tool-uri prea puternice | MVP read-only; write tools amanate. |
| Date sensibile in prompt/log | redactare, limite payload si fara chei in audit. |
| Dependinta operationala noua | `features.mcp=false` implicit pana la MVP stabil. |

## Relatie Cu Serverul MCP De Context

Acest serviciu nu inlocuieste serverul MCP local de context al proiectului.

Diferenta:

| Server | Rol |
|---|---|
| MCP context server din Docker | Ajuta Codex/JetBrains cu memorie, Chroma, changelog si context de cod. |
| Spring AI MCP runtime sidecar | Ajuta pluginul AINPC la runtime cu tool-uri AI si context de gameplay. |

Regula:

```text
MCP context server este pentru dezvoltare.
Spring AI MCP sidecar este pentru runtime/plugin.
```

## Recomandare De Implementare

Ordinea recomandata:

1. Pastreaza `features.ai=true` doar pentru AI runtime existent.
2. Adauga `features.mcp=false` implicit.
3. Creeaza `ainpc-mcp-service` cu `ainpc.ping`.
4. Adauga client health read-only in plugin.
5. Adauga `ainpc.feature.state`.
6. Ruleaza Paper smoke cu sidecar oprit si pornit.
7. Abia dupa aceea leaga `AIOrchestrationService` de MCP.
