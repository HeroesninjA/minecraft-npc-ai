# Coding Automation Stack Linux VS Code DeepSeek MCP

Actualizat: 2026-06-04

## Scop

Acest document defineste un stack complet de coding automation pentru server Linux accesat prin SSH si VS Code Server.

Stack-ul este pentru dezvoltare, build, testare, analiza, refactor, documentatie, deploy si mentenanta proiectului AINPC. Nu este providerul AI runtime al pluginului Minecraft.

Ideea principala:

```text
VS Code Server editeaza.
CLI-ul de automation orchestreaza.
LLM-urile propun si explica.
MCP-urile aduc context si tools.
Gradle/Git/Docker/Paper valideaza realitatea.
```

## Obiective

| Obiectiv | Rezultat dorit |
|---|---|
| Coding AI pe server | AI-ul ruleaza langa repo, logs, build si Docker, nu doar local pe Windows. |
| Multi-LLM | DeepSeek poate fi provider principal, dar pot exista fallback-uri sau modele specializate. |
| MCP extensibil | Serena, Context7, Superpowers, Chrome si alte MCP-uri pot fi activate/dezactivate separat. |
| Context local | Codul, docs, logs si deciziile proiectului sunt indexate pentru cautare hibrida. |
| Embeddings si reranking | Contextul trimis la LLM este redus si relevant, nu dump mare de fisiere. |
| CLI-first | Orice task se poate rula fara IntelliJ: build, test, package, deploy, backup. |
| Componente usor de adaugat | Fiecare provider/MCP/workflow este declarat ca o componenta optionala. |
| Componente usor de eliminat | Daca un MCP sau LLM nu mai este dorit, se dezactiveaza fara sa rupa CLI-ul. |

## Non-Obiective

- Nu muta logica pluginului Minecraft in stack-ul de coding automation.
- Nu pune chei API in repo.
- Nu expune MCP-uri de editare/refactor pe internet.
- Nu da agentului drept sa ruleze comenzi destructive fara confirmare explicita.
- Nu inlocuieste testele Gradle, Paper smoke sau review-ul de cod.

## Arhitectura

```text
SSH / VS Code Server
  -> /opt/ainpc/project
  -> ainpc-ai CLI
  -> LLM Gateway
       -> DeepSeek
       -> OpenAI optional
       -> local model optional
       -> fallback/noop
  -> MCP Hub
       -> Serena MCP
       -> Context7 MCP
       -> Superpowers MCP
       -> Chrome DevTools MCP optional
       -> Project Memory MCP
  -> Retrieval
       -> keyword index
       -> embeddings
       -> vector DB
       -> reranker
  -> Execution
       -> git
       -> gradle
       -> docker compose
       -> paper deploy scripts
       -> backup/restore scripts
```

Regula:

```text
AI-ul poate propune patch-uri.
CLI-ul aplica doar operatii controlate.
Build/test/deploy confirma rezultatul.
```

## Structura Pe Server

```text
/opt/ainpc/
  project/
    .git/
    ainpc-core-plugin/
    ainpc-scenario-medieval/
    docs/
  automation/
    ainpc-ai
    config/
      stack.yml
      providers.yml
      mcp-servers.yml
      retrieval.yml
      workflows.yml
    components/
      llm/
      mcp/
      retrieval/
      workflows/
      deploy/
    scripts/
    logs/
  context/
    chroma/
    keyword-index.json
    memory/
    changelog/
  paper/
    paper.jar
    plugins/
    logs/
  backups/
  .env
```

`project/` ramane repo-ul curat. `automation/`, `context/`, `paper/`, `backups/` si `.env` sunt infrastructura locala de server.

## Varianta Cu Doua VPS-uri

Pentru separare curata, stack-ul poate rula pe doua VPS-uri:

```text
VPS 1: Coding / Automation
  -> VS Code Server
  -> repo Git
  -> DeepSeek / multi-LLM
  -> MCP-uri: Serena, Context7, Superpowers, Project Memory
  -> embeddings / reranking
  -> build/test/package
  -> genereaza artefacte JAR

VPS 2: Live Test / Paper
  -> Java 25+
  -> Paper server
  -> plugins/
  -> config si DB runtime
  -> smoke test
  -> logs live
  -> backup/restore
```

Aceasta este varianta recomandata cand serverul Minecraft trebuie sa ramana curat si stabil.

Beneficii:

| Separare | Beneficiu |
|---|---|
| Coding VPS | Poate avea multe tool-uri AI/MCP fara sa afecteze tick-ul sau memoria serverului Paper. |
| Test VPS | Ruleaza doar runtime-ul Minecraft, JAR-urile, configul si datele de test. |
| Securitate | Cheia DeepSeek si MCP-urile de refactor raman pe VPS 1, nu pe serverul Paper. |
| Deploy controlat | Build-ul se face pe VPS 1, apoi se trimit doar artefactele spre VPS 2. |
| Rollback simplu | VPS 2 pastreaza backup de JAR/config/date inainte de deploy. |
| Performanta | Embeddings, reranking si build nu concureaza cu serverul Minecraft. |

Structura recomandata:

```text
VPS 1 /opt/ainpc/
  project/
  automation/
  context/
  artifacts/
  backups/
  .env

VPS 2 /opt/ainpc/
  paper/
    paper.jar
    plugins/
    logs/
  incoming/
  backups/
  data/
```

Workflow de deploy:

```text
1. code pe VPS 1 prin VS Code Server
2. ainpc-ai test
3. ainpc-ai package
4. ainpc-ai deploy-test
5. VPS 2 face backup JAR/config/date
6. VPS 2 instaleaza JAR-urile din incoming/
7. VPS 2 restarteaza Paper controlat
8. ainpc-ai smoke-test verifica logs si plugin load
9. daca pica, ainpc-ai rollback-test revine la backup
```

Comenzi propuse pe VPS 1:

```bash
ainpc-ai build
ainpc-ai test
ainpc-ai package
ainpc-ai deploy-test
ainpc-ai smoke-test
ainpc-ai rollback-test
ainpc-ai logs-test
```

Transfer artefacte:

```bash
rsync -avz \
  /opt/ainpc/project/ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar \
  /opt/ainpc/project/ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-1.0.0.jar \
  ainpc-test@VPS2:/opt/ainpc/incoming/
```

Deploy pe VPS 2:

```bash
sudo systemctl stop paper
cp /opt/ainpc/incoming/ainpc-core-plugin-1.0.0.jar /opt/ainpc/paper/plugins/
cp /opt/ainpc/incoming/ainpc-scenario-medieval-1.0.0.jar /opt/ainpc/paper/plugins/
sudo systemctl start paper
journalctl -u paper -n 200 --no-pager
```

Reguli:

- VPS 1 poate avea AI, MCP, embeddings, Docker si tool-uri grele.
- VPS 2 nu are nevoie de DeepSeek sau MCP-uri de coding.
- VPS 2 trebuie sa aiba Java 25+ pentru Paper 26.1+.
- Deploy-ul spre VPS 2 se face prin user dedicat, de exemplu `ainpc-test`.
- `sudo` pe VPS 2 trebuie limitat la `systemctl restart/status paper` si scripturile aprobate.
- Paper smoke trebuie sa confirme `AINPCPlugin` si addonurile in logs.
- Rollback-ul trebuie testat inainte de deploy-uri repetate.

Gate pentru varianta doua VPS-uri:

- VPS 1 poate face build/test fara acces root pe VPS 2.
- VPS 2 poate porni Paper fara tool-uri AI instalate.
- `deploy-test` nu trimite `.env` sau secrete.
- `smoke-test` detecteaza `UnknownDependencyException`, `NoClassDefFoundError` si lipsa `AINPCPlugin`.
- `rollback-test` restaureaza JAR-urile anterioare si reporneste Paper.

## CLI Principal

Comanda propusa:

```bash
ainpc-ai <command>
```

Comenzi MVP:

| Comanda | Rol |
|---|---|
| `ainpc-ai doctor` | Verifica OS, Java, Gradle, Docker, Git, env, MCP-uri si LLM. |
| `ainpc-ai status` | Rezumat stack, repo, servicii, procese si ultimele erori. |
| `ainpc-ai index` | Reindexeaza repo/docs/loguri in keyword + vector DB. |
| `ainpc-ai ask "<intrebare>"` | Raspuns cu context local, fara editari. |
| `ainpc-ai plan "<task>"` | Plan de implementare cu fisiere probabile si riscuri. |
| `ainpc-ai fix "<task>"` | Flux controlat de patch + test, cu confirmare. |
| `ainpc-ai test` | Ruleaza testele Gradle relevante sau toate. |
| `ainpc-ai package` | Construieste JAR-urile pluginului. |
| `ainpc-ai deploy-paper` | Copiaza JAR-urile in serverul Paper local dupa build. |
| `ainpc-ai logs` | Arata logs relevante Paper/build/MCP/automation. |
| `ainpc-ai backup` | Backup repo state/config/context/paper data dupa politica. |
| `ainpc-ai component list` | Listeaza componente disponibile si active. |
| `ainpc-ai component add <id>` | Adauga o componenta noua declarativ. |
| `ainpc-ai component enable <id>` | Activeaza o componenta. |
| `ainpc-ai component disable <id>` | Dezactiveaza o componenta. |
| `ainpc-ai component health <id>` | Verifica o componenta. |
| `ainpc-ai component remove <id>` | Elimina config-ul unei componente. |

## LLM Gateway

DeepSeek poate fi provider principal pentru coding automation.

Exemplu `.env`:

```env
CODING_AI_PROVIDER=deepseek
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat

CODING_AI_FALLBACK_PROVIDER=none
```

Exemplu `providers.yml`:

```yaml
default: deepseek

providers:
  deepseek:
    type: openai-compatible
    base_url_env: DEEPSEEK_BASE_URL
    api_key_env: DEEPSEEK_API_KEY
    model_env: DEEPSEEK_MODEL
    roles:
      - planning
      - coding
      - explanation
  openai:
    type: openai-compatible
    enabled: false
    base_url: "https://api.openai.com/v1"
    api_key_env: OPENAI_API_KEY
    model: "gpt-5.4"
  local:
    type: openai-compatible
    enabled: false
    base_url: "http://127.0.0.1:11434/v1"
    api_key: "local"
    model: "local-model"
```

Reguli:

- providerul este configurabil, nu hardcodat;
- cheile sunt doar in `.env`;
- fiecare provider are health check;
- fallback-ul poate fi `none` ca sa evite costuri neasteptate;
- taskurile riscante cer confirmare chiar daca modelul propune patch.

## MCP Hub

MCP-urile sunt componente optionale.

| Componenta | Rol | Prioritate |
|---|---|---|
| Serena MCP | symbol search, references, diagnostics, refactor semantic | mare |
| Context7 MCP | documentatie actuala pentru librarii si framework-uri | mare |
| Superpowers MCP | workflow-uri de planning, TDD, debugging, review | medie |
| Chrome DevTools MCP | browser automation, UI/web test | mica pentru plugin; util ulterior pentru dashboards |
| Project Memory MCP | memorie, changelog, reguli, vector search, context pack | mare |

Regula de securitate:

```text
MCP-urile cu tools de editare raman local-only.
Nu se expun pe internet fara auth strict.
```

## Component Model

Fiecare componenta se descrie declarativ.

Exemplu `components/mcp/context7.yml`:

```yaml
id: context7
type: mcp
enabled: true
description: "Documentatie actuala pentru librarii si framework-uri."
transport: stdio
command: npx
args:
  - -y
  - "@upstash/context7-mcp"
health:
  command: ainpc-ai mcp ping context7
depends_on:
  - node
removable: true
```

Exemplu `components/llm/deepseek.yml`:

```yaml
id: deepseek
type: llm-provider
enabled: true
api: openai-compatible
base_url_env: DEEPSEEK_BASE_URL
api_key_env: DEEPSEEK_API_KEY
model_env: DEEPSEEK_MODEL
health:
  command: ainpc-ai llm ping deepseek
removable: true
```

Campuri obligatorii:

| Camp | Rol |
|---|---|
| `id` | Nume stabil. |
| `type` | `llm-provider`, `mcp`, `retrieval`, `workflow`, `deploy`. |
| `enabled` | Activeaza/dezactiveaza fara stergere. |
| `health` | Comanda de verificare. |
| `depends_on` | Dependinte de sistem sau alte componente. |
| `removable` | Daca poate fi eliminata fara migration. |

## Adaugare Componenta Noua

Flux:

```text
ainpc-ai component add <id>
-> creeaza fisier componenta
-> verifica dependinte
-> adauga in stack.yml daca este activata
-> ruleaza health check
-> scrie intrare in changelog local
```

Checklist:

1. Componenta are `id` unic.
2. Nu contine secrete in YAML.
3. Are `enabled: false` daca este experimentala.
4. Are `health.command`.
5. Are `remove` clar.
6. Are timeout.
7. Nu blocheaza `ainpc-ai doctor` daca este disabled.
8. Nu schimba workflow-ul principal fara opt-in.

## Eliminare Componenta

Flux:

```text
ainpc-ai component disable <id>
ainpc-ai doctor
ainpc-ai component remove <id>
```

Reguli:

- intai disable, apoi remove;
- stergerea unei componente nu sterge date persistente fara confirmare;
- daca o componenta este dependency pentru alta, remove este blocat sau cere `--force`;
- CLI-ul trebuie sa functioneze cu doar provider LLM + Git + Gradle.

Gate:

- `ainpc-ai doctor` trece dupa eliminare;
- build/test Gradle inca ruleaza;
- config-ul nu contine referinte la componenta stearsa;
- nu exista systemd service/container orfan.

## Retrieval, Embeddings Si Reranking

Scopul retrieval-ului este sa trimita LLM-ului context mic si relevant.

Pipeline:

```text
query task
-> keyword search
-> vector search
-> merge/hybrid scoring
-> reranking
-> context pack compact
-> LLM
```

Componente:

| Componenta | Rol |
|---|---|
| keyword index | rapid, exact pentru simboluri, comenzi si fisiere. |
| embeddings | gaseste concepte similare in cod/docs/loguri. |
| vector DB | persistenta pentru embeddings. |
| reranker | reordoneaza rezultatele inainte de prompt. |
| context pack | format final trimis la LLM. |

Reguli:

- `build`, `target`, `.gradle`, `.kotlin`, `node_modules` nu se indexeaza implicit;
- logs mari sunt tail-limited;
- secretele sunt redactate;
- schimbarea modelului de embeddings cere reindex controlat;
- reranking trebuie sa fie optional.

## Workflow Coding

Flux standard pentru task de cod:

```text
1. classify task
2. retrieve context
3. optional Context7 docs
4. optional Serena symbol lookup
5. rerank context
6. LLM plan
7. patch small slice
8. Gradle test/build
9. review result
10. changelog/memory
```

Taskurile mari se sparg in slice-uri mici.

Nu se face deploy Paper fara:

- build JAR trecut;
- backup config/date cand este cazul;
- server Paper oprit sau deploy hot-safe explicit;
- smoke log verificat.

## Workflows Propuse

| Workflow | Scop |
|---|---|
| `build-test` | ruleaza Gradle test/build. |
| `kotlin-conversion-slice` | converteste un slice Java -> Kotlin si ruleaza teste relevante. |
| `paper-smoke` | porneste Paper, verifica plugin load, opreste serverul. |
| `code-review` | review automat pe diff. |
| `dependency-audit` | verifica dependinte, shading, JAR contents. |
| `docs-update` | actualizeaza docs si indexuri. |
| `deploy-paper` | package + copy jars + smoke. |
| `backup-restore-check` | backup si test restore controlat. |

## Faze

### Faza 0 - Baseline Server

Scop:

- user Linux dedicat;
- SSH key;
- VS Code Server;
- Git;
- Java compatibil;
- Gradle wrapper functional;
- Docker optional.

Gate:

- `git status` ruleaza in repo;
- `./gradlew test` poate porni;
- Java version este documentat.

### Faza 1 - CLI Minim

Scop:

- `ainpc-ai doctor`;
- `ainpc-ai test`;
- `ainpc-ai package`;
- `ainpc-ai logs`.

Gate:

- CLI-ul functioneaza fara niciun LLM;
- toate comenzile au timeout si exit code clar.

### Faza 2 - DeepSeek Provider

Scop:

- configurare DeepSeek pentru coding AI;
- health check;
- `ainpc-ai ask`;
- `ainpc-ai plan`.

Gate:

- cheia sta in `.env`;
- health check nu logheaza cheia;
- taskurile read-only functioneaza.

### Faza 3 - Project Context

Scop:

- keyword index;
- embeddings;
- vector DB;
- context pack;
- reranking optional.

Gate:

- indexul exclude foldere generate;
- query simplu gaseste fisiere relevante;
- reranking disabled nu rupe CLI-ul.

### Faza 4 - MCP Hub

Scop:

- Serena;
- Context7;
- Project Memory MCP;
- Superpowers optional;
- Chrome optional disabled.

Gate:

- fiecare MCP are health separat;
- dezactivarea unui MCP nu rupe `ainpc-ai doctor`;
- Serena nu este expusa public.

### Faza 5 - Patch Automation

Scop:

- `ainpc-ai fix`;
- patch mic;
- teste relevante;
- review diff;
- changelog.

Gate:

- nu ruleaza comenzi destructive fara confirmare;
- nu modifica fisiere generate inutil;
- testele relevante trec sau esecul este raportat clar.

### Faza 6 - Deploy Si Paper Smoke

Scop:

- build JAR;
- copy in Paper server;
- smoke start;
- verificare plugin load;
- rollback.

Gate:

- backup inainte de deploy;
- smoke confirma `AINPCPlugin` load;
- serverul nu ramane orfan dupa smoke.

## Definitia De Gata Pentru MVP

MVP-ul coding automation este gata cand:

| Zona | Criteriu |
|---|---|
| Server | VS Code Server si SSH functioneaza stabil. |
| Repo | proiectul curent este importat in `/opt/ainpc/project`. |
| CLI | `ainpc-ai doctor`, `test`, `package`, `ask`, `plan` functioneaza. |
| LLM | DeepSeek este configurat prin `.env` si health check trece. |
| Context | keyword search si context pack functioneaza. |
| MCP | Serena si Context7 pot fi activate/dezactivate separat. |
| Extensibilitate | `component add/enable/disable/health/remove` exista pentru config. |
| Securitate | secretele nu sunt in repo; MCP-urile sunt local-only. |
| Fallback | CLI-ul poate rula build/test fara LLM si fara MCP-uri. |

## Securitate

Reguli:

- `.env` nu se comite;
- cheile API nu se afiseaza in logs;
- SSH user dedicat;
- `sudo` limitat sau explicit;
- MCP-uri cu editare/refactor doar pe `127.0.0.1`;
- backup inainte de deploy sau migration;
- comenzi destructive cer confirmare;
- audit compact pentru actiuni automate.

## Relatie Cu Pluginul AINPC

Acest stack este pentru dezvoltare.

Nu schimba:

- providerul AI runtime al pluginului;
- `features.ai`;
- config-ul Paper live;
- logica gameplay.

Poate ajuta la:

- implementare mecanici;
- conversie Java -> Kotlin;
- debugging `features.ai`;
- packaging JAR;
- smoke Paper;
- documentatie;
- deploy controlat pe server.

## Recomandare De Implementare

Ordinea recomandata:

1. Creeaza structura `/opt/ainpc`.
2. Cloneaza/importa proiectul in `/opt/ainpc/project`.
3. Instaleaza Java, Git, Docker si dependinte minime.
4. Adauga `ainpc-ai doctor/test/package/logs`.
5. Configureaza DeepSeek pentru `ask/plan`.
6. Adauga index keyword si context pack.
7. Adauga Serena MCP.
8. Adauga Context7 MCP.
9. Adauga embeddings si reranking.
10. Adauga `fix` doar dupa ce test/build sunt stabile.
11. Adauga deploy Paper si smoke doar dupa backup/rollback.
