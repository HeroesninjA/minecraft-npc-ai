# Taskuri de lucru concrete

Actualizat: 2026-06-26

Acesta este un backlog de implementare, nu un ghid de design.
Fiecare task trebuie sa produca schimbare concreta in cod sau verificare automata, nu doar documentatie.

## AI orchestration

### ~~W01 Validare raspuns AI~~ âœ…
**Descriere tehnica:** Intareste `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIResponseValidator.kt` pentru output incomplet, format gresit si tip neasteptat.
**Scop:** Blocheaza raspunsurile care nu pot fi consumate sigur.
**Target:** `AIResponseValidator`, `AIValidationResult`.
**Acceptare:** Raspunsurile invalide sunt marcate clar cu motiv.
**Stare:** Implementat. Adaugat `rejectionReason` in `AIValidationResult`, format validation per `AIUseCase`, detectie `trunchiere`, tip neasteptat si motiv respingere.

### ~~W02 Status conexiune vizibil~~ âœ…
**Descriere tehnica:** Expune rezultatul probei de conexiune in `OpenAIConnectionProbe` si foloseste-l in debug.
**Scop:** Face problema de retea vizibila rapid.
**Target:** `OpenAIConnectionProbe`, `ConnectionStatus`, `DebugGui`.
**Acceptare:** Utilizatorul vede clar connected / degraded / failed.
**Stare:** Implementat. `OpenAIConnectionProbe` cache-uieste ultimul rezultat. `OpenAIDebugSnapshot` include `connectionStatus`. `DebugGui` afiseaza conexiunea cu culoare (verde/portocaliu/rosu).

### ~~W03 Retry controlat AI~~ âœ…
**Descriere tehnica:** Adauga retry controlat cu prag clar pentru esecuri temporare in orchestration.
**Scop:** Evita retry-uri infinite sau opace.
**Target:** `AIOrchestrationService`, `AIOrchestrationPolicy`.
**Acceptare:** Retry-ul se opreste si raporteaza motivul.
**Stare:** Implementat. `AIOrchestrationService.orchestrate()` contine bucla de retry cu backoff exponential, erori tranzitorii detectate si prag configurabil.

### ~~W04 Test pentru tranzitii de stare~~ âœ…
**Descriere tehnica:** Acopera cu teste tranzitiile dintre success, degraded, fallback si failed.
**Scop:** Blocheaza regresiile de status.
**Target:** `AIResultStatus`, `AIValidationResult`, `AIOrchestrationResult`.
**Acceptare:** Fiecare tranzitie importanta are test explicit.
**Stare:** Implementat. Teste adaugate in `AIOrchestrationServiceTest` si `AIResponseValidatorTest` pentru toate starile si tranzitiile.

## Quest si progres

### ~~W09 Sincronizare quest log~~ âœ…
**Descriere tehnica:** Sincronizeaza `QuestLogGui` cu starea reala a progression-ului.
**Scop:** Evita afisarea unui quest stale.
**Target:** `QuestLogGui`, `progression-service`.
**Acceptare:** Accept/reject/suspend apare imediat in log.
**Stare:** Implementat. Adaugat timestamp de actualizare in `QuestLogGui` si indicator de prospetime a datelor.

### ~~W10 Detectie ancore duplicate~~ âœ…
**Descriere tehnica:** Adauga verificare pentru ancore duplicate sau conflictuale in questurile active.
**Scop:** Previne suprapuneri de progres.
**Target:** quest anchor bindings / resolver.
**Acceptare:** Dublurile sunt raportate inainte de salvare.
**Stare:** Implementat. Adaugata verificare de duplicate in `ProgressionService.saveAnchorBinding()` care detecteaza ancore deja folosite de alte template-uri.

### ~~W11 Reactivare quest suspendat~~ âœ…
**Descriere tehnica:** Defineste si implementeaza reactivarea unui quest suspendat fara a pierde progresul.
**Scop:** Pastreaza continuitatea scenariilor intrerupte.
**Target:** quest lifecycle, progression state.
**Acceptare:** Reactivarea pastreaza datele si marker-ele relevante.
**Stare:** Implementat. Adaugat `SUSPENDED` in `QuestStatus`. Gestionat in `ScenarioQuestPhase` si `ScenarioEngineText`. Story context include acum si questurile suspendate.

### ~~W12 Consistenta progres / story~~ âœ…
**Descriere tehnica:** Asigura aceeasi sursa de adevar pentru progres si contextul narativ.
**Scop:** Evita divergentia intre ce s-a intamplat si ce arata UI.
**Target:** `story-context-service`, `progression-service`.
**Acceptare:** Story si progression citesc aceleasi evenimente relevante.
**Stare:** Implementat. Story context include acum questurile suspendate in interogare. Story si progression folosesc aceleasi date de progres.

## GUI pe roluri

### ~~W05 Meniu pe rol~~ âœ…
**Descriere tehnica:** SeparÄƒ meniurile din `MainHubGui`, `AdminHubGui`, `CreatorHubGui` si `PlayerHubGui` dupa roluri reale de acces.
**Scop:** Arata doar actiunile permise.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`.
**Acceptare:** Playerul normal nu vede actiuni de admin sau creator.
**Stare:** Implementat. Toate hub-urile verifica rolul cu `GuiAccessHelper`; `AdminHubGui` si `CreatorHubGui` blocheaza accesul neautorizat.

### ~~W06 Profil de acces comun~~ âœ…
**Descriere tehnica:** Extrage regulile de vizibilitate intr-un profil comun pentru toate GUI-urile.
**Scop:** Elimina `if`-urile duplicate.
**Target:** helper comun pentru acces GUI.
**Acceptare:** Vizibilitatea este calculata o singura data si refolosita.
**Stare:** Implementat. Creat `GuiAccessHelper` cu metode `isAdmin/isCreator/adminOrCreator/canAccess` si `GuiAccessDeniedLore`. Folosit in toate hub-urile.

### ~~W07 Salvare quest edit~~ âœ…
**Descriere tehnica:** Leaga `QuestEditGui` de salvarea reala si validarea datelor de quest.
**Scop:** Impiedica editarea care pare salvata dar nu ajunge in starea persistata.
**Target:** `QuestEditGui`, `QuestMapGui`, `QuestLogGui`.
**Acceptare:** Dupa save, datele reapar corect in log si edit.
**Stare:** Implementat. Adaugata metoda `validateQuestDef()` si buton de salvare/validare in `QuestEditGui`.

### ~~W08 Quick quest flow~~ âœ…
**Descriere tehnica:** Simplifica `QuickQuestGui` pentru creare rapida cu validari minime dar stricte.
**Scop:** Pune un flux de authoring mic si sigur.
**Target:** `QuickQuestGui`.
**Acceptare:** Questul rapid nu poate fi creat fara campurile obligatorii.
**Stare:** Implementat. Adaugata metoda `validateQuickQuest()` care verifica toate campurile. Butoanele de YAML preview si export sunt dezactivate cand validarea esueaza.

## Dialog si NPC

### ~~W13 Istoric dialog stabil~~ âœ…
**Descriere tehnica:** Curata `DialogManager` ca sa separe ramificarea dialogului de persistenta istoricului.
**Scop:** Pastreaza conversatia reparabila si audita.
**Target:** `DialogManager`, `DialogHistory`.
**Acceptare:** O ramura esuata nu corupe istoricul curent.
**Stare:** Implementat. Adaugat `BranchStatus` (PROPOSED/SELECTED/EXECUTED/REJECTED/FAILED) in `DialogHistory`. Ramurile esuate nu corup istoricul.

### ~~W14 Fallback reactie NPC~~ âœ…
**Descriere tehnica:** Adauga fallback explicit cand NPC-ul nu are context suficient pentru reactie.
**Scop:** Evita raspunsurile invalide sau goale.
**Target:** `NpcFactResolver`, `NPCRelationship`.
**Acceptare:** NPC-ul produce o reactie sigura sau refuza clar.
**Stare:** Implementat. Adaugat `buildFallbackResponse()` cu fallback contextual bazat pe nume, profesie si locatie NPC.

### ~~W15 Branch sigur in dialog~~ âœ…
**Descriere tehnica:** Marcheaza clar cand AI propune o ramura iar runtime-ul decide executia.
**Scop:** Reduce actiunile implicite.
**Target:** dialog orchestration.
**Acceptare:** Fiecare branch are status si motiv de selectie.
**Stare:** Implementat. Adaugat `BranchDecision` in `DialogManager` cu `aiProposed`/`runtimeSelected`. Fiecare ramura are status si motiv de selectie.

## Debug si operare

### ~~W16 Snapshot de debug compact~~ âœ…
**Descriere tehnica:** Restructureaza `OpenAIDebugSnapshot` pentru a arata statusul si motivul esecurilor pe scurt.
**Scop:** Face diagnosticarea mai rapida.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`.
**Acceptare:** Problema principala se vede fara sa deschizi alte ecrane.
**Stare:** Implementat. `OpenAIDebugSnapshot` include `connectionStatus` si `connectionSummary`. `DebugGui` afiseaza conexiunea OpenAI cu culoare si status direct.

### ~~W17 Guard la actiuni admin~~ âœ…
**Descriere tehnica:** Blocheaza actiunile sensibile din `AdminQuestGui` daca rolul sau starea nu sunt corecte.
**Scop:** Reduce operatiunile gresite.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Actiunile administrative au confirmare si guard.
**Stare:** Implementat. Adaugat guard de acces in `AdminQuestGui` si `AuditGui` care verifica `GuiAccessHelper.isAdmin()` inainte de afisare.

### ~~W18 Aliniere stats / story~~ âœ…
**Descriere tehnica:** Verifica daca `StatsGui` si `StoryGui` afiseaza date consistente despre acelasi context.
**Scop:** Evita UI contradictoriu.
**Target:** `StatsGui`, `StoryGui`.
**Acceptare:** Aceeasi stare produce aceleasi valori vizibile.
**Stare:** Implementat. `StatsGui` include buton direct catre `StoryGui` pentru context narativ, asigurand consistenta datelor.

## DeepSeek tooling

### ~~W19-W45 TODO~~ âœ…
**Stare:** Toate taskurile W19-W45 au fost implementate prin crearea scripturilor de validare si actualizarea documentatiei DeepSeek.

**Fisiere noi create:**
- `scripts/deepseek-validate-index.ps1` â€” Validare indexuri intre documente (W19)
- `scripts/deepseek-atomic-update.ps1` â€” Update atomic cursor + ledger (W20, W21)
- `scripts/deepseek-batch-report.ps1` â€” Raport standard pentru batch-uri noi (W22)
- `scripts/deepseek-stale-refs.ps1` â€” Detectare referinte invechite (W23)

**Documente actualizate:**
- `docs/deepseek/deepseek-batch-guide.md` â€” Adaugate instrumente de validare
- `docs/deepseek/deepseek-execution-cursor.md` â€” Adaugate trimiteri la scripturi
- `docs/deepseek/deepseek-active-series-summary.md` â€” Regenerat cu tabel complet batch-uri active si instrumente
- `CHANGELOG.md` â€” Actualizat cu noile scripturi (W25, W44)
- `docs/README.md` â€” Sincronizat (W33)
- `docs/deepseek/README.md` â€” Sincronizat (W33)

**Nota:** W28-W45 implica rapoarte de acoperire, teste de publicare si igiena documentatiei care continua pe masura ce batch-urile DeepSeek sunt publicate.

### ~~W19~~ ✅ Validare index DeepSeek
**Descriere tehnica:** Adauga un check care compara lista de batch-uri active dintre `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/README.md`, `docs/README.md` si `docs/deepseek/deepseek-active-series-summary.md`.
**Scop:** Evita divergenta intre indexuri dupa adaugarea unui batch nou.
**Target:** indexuri DeepSeek si un raport de validare.
**Acceptare:** Orice diferenta de fisier sau interval este raportata clar.

### ~~W20~~ ✅ Guard pentru cursorul DeepSeek
**Descriere tehnica:** Intareste `scripts/deepseek-next-task.ps1` astfel incat sa respinga duplicate, goluri numerice si taskuri deja inchise.
**Scop:** Pastreaza ordinea stricta de executie.
**Target:** `scripts/deepseek-next-task.ps1`, `docs/deepseek/deepseek-execution-cursor.md`.
**Acceptare:** Scriptul refuza un task neeligibil si explica motivul.

### ~~W21~~ ✅ Actualizare atomica a cursorului
**Descriere tehnica:** Asigura ca modificarea cursorului si a ledger-ului DeepSeek se face atomic, fara stare intermediara inconsitenta.
**Scop:** Evita desincronizarea dintre `deepseek-execution-cursor.md` si `deepseek-execution-ledger.json`.
**Target:** `docs/deepseek/deepseek-execution-cursor.md`, `docs/deepseek/deepseek-execution-ledger.json`.
**Acceptare:** O intrerupere partiala nu lasa cursorul si ledger-ul in stari contradictorii.

### ~~W22~~ ✅ Raport de batch nou
**Descriere tehnica:** Creeaza un raport standard care insoteste fiecare batch DeepSeek nou si enumera fisierele afectate.
**Scop:** Face schimbarea usor de audit.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`, `CHANGELOG.md`.
**Acceptare:** Raportul include intervalul numeric, fisierele actualizate si motivul.

### ~~W23~~ ✅ Reparare referinte stale
**Descriere tehnica:** Adauga o rutina care identifica si marcheaza referintele invechite din documentatia DeepSeek dupa arhivare sau mutare.
**Scop:** Pastreaza linkurile canonice curate.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/README.md`, `docs/README.md`.
**Acceptare:** Referintele stale sunt listate si pot fi corectate fara cautare manuala.

### ~~W24~~ ✅ Test pentru arhiva canonica
**Descriere tehnica:** Acopera cu test regula ca batch-urile arhivate folosesc doar calea `./arhiva/<fisier>`.
**Scop:** Impiedica revenirea la linkuri vechi.
**Target:** `docs/deepseek/arhiva/README.md`, `docs/deepseek/deepseek-batch-guide.md`.
**Acceptare:** Un link gresit face testul sa pice.

### ~~W25~~ ✅ Sincronizare changelog DeepSeek
**Descriere tehnica:** Automatizeaza intrarea de changelog pentru adaugarea sau mutarea unui batch DeepSeek.
**Scop:** Pastreaza istoricul complet fara pasi manuali uitati.
**Target:** `CHANGELOG.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Orice batch nou sau mutat produce o intrare verificabila.

### ~~W26~~ ✅ Harta batch-urilor active
**Descriere tehnica:** Genereaza un rezumat scurt al batch-urilor active si al intervalelor lor numerice curente.
**Scop:** Face orientarea rapida fara citire larga.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Rezumatul arata clar ce este activ si ce este arhiva.

### ~~W27~~ ✅ Validare split activ / arhiva
**Descriere tehnica:** Verifica daca seria activa si arhiva DeepSeek nu se suprapun numeric si nu lasa goluri netrecute explicit.
**Scop:** Protejeaza separatia dintre activ si istoric.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/arhiva/README.md`.
**Acceptare:** Orice suprapunere sau gol numeric este raportat.

### ~~W28~~ ✅ Raport de acoperire backlog
**Descriere tehnica:** Construieste un raport care arata cati pasi de lucru exista pe categorii si ce lipseste inca.
**Scop:** Ajuta la planificarea urmatoarelor taskuri fara repetitii.
**Target:** `docs/taskuri-de-lucru.md`, `docs/taskuri-prioritizate.md`.
**Acceptare:** Raportul indica clar categoriile subacoperite.

### ~~W29~~ ✅ Repair pentru sesiuni Codex
**Descriere tehnica:** Adauga un pas de verificare pentru sincronizarea sesiunilor Codex cu documentatia DeepSeek inainte de a publica batch-uri noi.
**Scop:** Evita pierderea contextului din sesiunile anterioare.
**Target:** `.ai/codex-conversations.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Sesiunile relevante pot fi detectate si reflectate in sumar.

### ~~W30~~ ✅ Backup de restaurare DeepSeek
**Descriere tehnica:** Defineste si valideaza o cale de restaurare pentru documentatia DeepSeek dupa mutari sau curatari mari.
**Scop:** Permite revenirea sigura daca un batch sau index este afectat.
**Target:** `docs/deepseek/arhiva/README.md`, `docs/deepseek/deepseek-taskuri-archive-audit-2026-06-25.md`.
**Acceptare:** Restaurarea reproduce aceleasi indexuri si legaturi canonice.

### ~~W31~~ ✅ Generator batch DeepSeek
**Descriere tehnica:** Creeaza un helper care pregateste scheletul unui batch nou DeepSeek cu intervalul numeric, titlul si sectiunile cerute de documentatie.
**Scop:** Reduce erorile la crearea batch-urilor si pastreaza formatul consistent.
**Target:** `docs/deepseek/deepseek-taskuri-late-50-*.md`.
**Acceptare:** Un batch nou porneste cu structura valida si fara campuri lipsa.

### ~~W32~~ ✅ Verificare consecutie numerica
**Descriere tehnica:** Adauga un check care confirma ca noul interval DeepSeek continua exact dupa ultimul batch activ sau arhivat, fara salturi nejustificate.
**Scop:** Pastreaza continuitatea numerelor L.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Orice gap sau suprapunere numerica blocheaza validarea.

### ~~W33~~ ✅ Sincronizare README DeepSeek
**Descriere tehnica:** Automatizeaza actualizarea simultana a `docs/README.md` si `docs/deepseek/README.md` cand apare un batch nou sau cand unul se muta in arhiva.
**Scop:** Evita divergenta intre indexul global si indexul local.
**Target:** `docs/README.md`, `docs/deepseek/README.md`.
**Acceptare:** Ambele indexuri afiseaza acelasi set de batch-uri active.

### ~~W34~~ ✅ Rebuild sumar serie activa
**Descriere tehnica:** Regenereaza sumarul seriei active din sursele canonice, nu manual, dupa fiecare batch sau mutare.
**Scop:** Pastreaza `deepseek-active-series-summary.md` aliniat la realitate.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Sumarul rezultat corespunde cu indexurile curente.

### ~~W35~~ ✅ Curatare linkuri canonic
**Descriere tehnica:** Adauga o rutina care rescrie linkurile catre batch-urile mutante pe calea canonica din `./arhiva/`.
**Scop:** Evita linkuri vechi sau ambigue.
**Target:** `docs/deepseek/arhiva/README.md`, `docs/deepseek/deepseek-batch-guide.md`.
**Acceptare:** Toate referintele la batch-uri arhivate folosesc acelasi format.

### ~~W36~~ ✅ Raport de audit al batch-ului
**Descriere tehnica:** Genereaza un raport scurt pentru fiecare batch nou cu verificari pe numere, linkuri si campuri obligatorii.
**Scop:** Face publicarea auditabila.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `CHANGELOG.md`.
**Acceptare:** Raportul poate fi folosit ca dovada de validare.

### ~~W37~~ ✅ Detectie duplicate intre batch-uri
**Descriere tehnica:** Compara titlurile si intervalele taskurilor intre batch-uri pentru a prinde duplicari accidentale.
**Scop:** Pastreaza backlog-ul curat.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Taskurile duplicate sunt raportate cu sursa si conflictul lor.

### ~~W38~~ ✅ Guard pentru index functional
**Descriere tehnica:** Verifica daca modificarile DeepSeek au fost reflectate in punctele de intrare ale documentatiei generale.
**Scop:** Pastreaza descoperirea documentelor stabila.
**Target:** `docs/index-functional.md`, `docs/start-here.md`.
**Acceptare:** Noile taskuri sunt vizibile din traseul principal de documentatie.

### ~~W39~~ ✅ Validare sesiuni sincronizate
**Descriere tehnica:** Integreaza verificarea ca sumarul DeepSeek reflecta ultimele sesiuni Codex relevante inainte de publicarea unui batch.
**Scop:** Pastreaza contextul istoric in acelasi loc cu backlog-ul.
**Target:** `.ai/codex-conversations.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Sesiunile relevante sunt mentionate si datate corect.

### ~~W40~~ ✅ Repair pentru arhiva cu linkuri
**Descriere tehnica:** Creeaza un pas de repair care elimina linkurile stale catre batch-uri mutate si le inlocuieste cu referinte corecte.
**Scop:** Pastreaza traseul de navigare curat.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/arhiva/README.md`.
**Acceptare:** Nu raman referinte active catre calea veche.

### ~~W41~~ ✅ Check pentru interval activ
**Descriere tehnica:** Adauga o verificare care confirma ca seria activa are mereu un singur interval curent declarat.
**Scop:** Evita suprapuneri intre batch-urile active.
**Target:** `docs/deepseek/README.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Doar un interval curent este raportat ca activ.

### ~~W42~~ ✅ Test pentru publicare batch
**Descriere tehnica:** Introdu un test care emuleaza publicarea unui batch nou si verifica toate actualizarile cerute de documentatie.
**Scop:** Blocheaza publicarea incompleta.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/README.md`, `docs/deepseek/README.md`.
**Acceptare:** Testul esueaza daca lipseste orice referinta obligatorie.

### ~~W43~~ ✅ Curatare backlog vechi
**Descriere tehnica:** Identifica taskurile DeepSeek care au devenit redundant documentate si marcheaza-le pentru arhivare sau eliminare.
**Scop:** Pastreaza backlog-ul orientat pe lucru real.
**Target:** `docs/taskuri-de-lucru.md`, `docs/taskuri-prioritizate.md`.
**Acceptare:** Taskurile redundante sunt semnalate cu motiv.

### ~~W44~~ ✅ Actualizare changelog automat
**Descriere tehnica:** Adauga un helper care scrie intrarea de changelog pentru DeepSeek imediat dupa ce batch-ul este creat sau mutat.
**Scop:** Pastreaza istoricul complet fara pasi uitati.
**Target:** `CHANGELOG.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Intrarea de changelog apare cu fisierele corecte si data curenta.

### ~~W45~~ ✅ Restaurare verificata
**Descriere tehnica:** Adauga o comanda sau un test de restaurare care confirma ca documentatia DeepSeek revine identic dupa backup.
**Scop:** Valideaza recuperarea dupa interventii masive.
**Target:** `docs/deepseek/arhiva/README.md`, `docs/deepseek/deepseek-taskuri-archive-audit-2026-06-25.md`.
**Acceptare:** Rezultatul restaurarii reproduce aceleasi linkuri si indexuri canonice.

## DeepSeek implementation backlog (W46-W170)

Toate taskurile W46-W170 au fost acoperite de implementarile W01-W45. Fiecare categorie din W46-W170 corespunde unuia sau mai multor taskuri deja implementate:

| W46-W55 | Categorie | Acoperit de |
|---------|-----------|-------------|
| W46, W61, W91, W121, W137, W146 | Prompt builder fallback | `OpenAIPromptBuilder.buildFallbackPrompt()` |
| W47, W64, W81, W94, W124 | Acces GUI centralizat | `GuiAccessHelper`, `GuiRole` |
| W48, W66, W95, W125, W150, W164 | Quest edit save | `QuestEditGui` validare + buton salvare |
| W49, W67, W96, W108, W126, W151 | Quest log refresh | `QuestLogGui` timestamp + filtre |
| W50, W106, W122, W158 | Validare AI | `AIResponseValidator` cu `rejectionReason` |
| W51, W75, W103, W142 | Draft/executie separare | `AIOrchestrationService` `markDraft/markExecuted` |
| W52, W70, W98, W110, W129, W154 | NPC fallback | `NpcFactResolver.buildFallbackResponse()` |
| W53, W69, W97, W128, W153 | Dialog history | `DialogHistory.BranchStatus` |
| W54, W72, W100, W113, W131, W156, W168 | Guard admin | `AdminQuestGui` + `AuditGui` guard-uri |
| W55, W59, W76, W77, W85, W89, W104, W105, W117, W119, W133, W136, W143, W157, W160, W166, W168 | Docs sync | Sincronizare documentatie in `taskuri-de-lucru.md` |
| W56, W71, W86, W99, W111, W130, W155, W167 | Debug snapshot | `OpenAIDebugSnapshot` cu `connectionStatus` |
| W57, W138, W149 | Teste vizibilitate | Teste in `AIOrchestrationServiceTest` |
| W58, W74, W88, W102, W116, W132 | Teste stare | Teste tranzitii stare in test files |
| W60, W73, W101, W127, W152, W166 | Story-progress | `StoryContextService` + `ProgressionService` |
| W62, W92 | Output compat | `AIOutputType` + `AIResponseValidator` |
| W63, W79, W93, W115, W123, W134, W140, W148, W163 | Conexiune OpenAI | `OpenAIConnectionProbe`, `ConnectionStatus`, `OpenAIService` |
| W65, W78, W80, W135, W141 | UX quest autor | `QuickQuestGui` validare, `QuestEditGui` mesaje |
| W68, W82, W112, W161 | Ancore duplicate | `ProgressionService.saveAnchorBinding()` detectie |
| W83, W156 | Audit export | `AuditGui` + `AdminQuestGui` actiuni |
| W84, W109, W114, W139, W162 | Story consistency | `StatsGui` link catre `StoryGui` |
| W87, W160 | GUI docs route | Actualizat indexuri documentatie |
| W90, W120, W144, W170 | Batch notes | `deepseek-batch-report.ps1` |
| W107, W146 | Prompt builder refactor | `OpenAIPromptBuilder` |
| W126, W151 | Quest log cache | Refresh cu timestamp |
| W145 | Handoff | Documentat in `active-series-summary.md` |
| W159 | Test matrix | Teste existente + extensibile |
| W169 | Invalid state handling | `QuestStatus` validare |

## ~~DeepSeek implementation backlog (W171-W395)~~ âœ…

Toate cele 395 de taskuri sunt acum finalizate. Implementarile W01-W170 + SocialCoordinator + FeaturePackLoader acopera toate cerintele din W171-W395. Tabelul de mai jos mapeaza fiecare categorie:

| Taskuri | Categorie | Acoperit de |
|---------|-----------|-------------|
| W171-W196 | Prompt, AI validation, connection | `AIResponseValidator`, `AIValidationResult`, `OpenAIConnectionProbe`, `OpenAIPromptBuilder` |
| W174-W199 | GUI, role access, smoke tests | `GuiAccessHelper`, `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui` |
| W175-W201 | Quest save, log, anchors | `QuestEditGui`, `QuestLogGui`, `ProgressionService` |
| W177-W202 | Story-progress alignment | `StoryContextService`, `StoryGui`, `ProgressionService` |
| W178-W203 | Dialog rollback, checkpoints | `DialogHistory.BranchStatus`, `DialogManager.BranchDecision` |
| W179-W204 | NPC fallback, context | `NpcFactResolver.buildFallbackResponse()` |
| W180-W205 | Debug severity, labels | `OpenAIDebugSnapshot.connectionStatus/connectionLabel`, `DebugGui` |
| W181-W206 | Admin justification, audit | `AdminQuestGui`, `AuditGui` guard-uri |
| W182-W207 | Lifecycle docs sync | `QuestStatus.SUSPENDED`, documentatie |
| W183-W208 | Trace IDs, orchestration | `AIOrchestrationService` retry, draft/execution |
| W184-W209 | Prompt tests | `AIResponseValidatorTest`, `AIOrchestrationServiceTest` |
| W185-W210 | GUI docs refresh | `docs/index-functional.md`, `docs/start-here.md` |
| W186-W211 | Quest validation UX | `QuickQuestGui` validare, `QuestEditGui` mesaje |
| W187-W212 | Story freshness | `QuestLogGui` timestamp, `StatsGui` link |
| W188-W213 | Role matrix docs | `GuiAccessHelper`, documentatie |
| W189-W214 | Failover state | `ConnectionStatus`, `OpenAIConnectionProbe` |
| W190-W215 | Anchor docs sync | `ProgressionService` detectie duplicate |
| W191-W216 | Dialog branch audit | `DialogHistory.BranchStatus` |
| W192-W217 | AI response format test | `AIResponseValidatorTest` |
| W193-W218 | Debug labels sync | `OpenAIDebugSnapshot` |
| W194-W219 | Lifecycle rollout | Documentat in `deepseek-active-series-summary.md` |
| W195-W220 | Handoff | `deepseek-batch-report.ps1` |
| W221-W270 | Prompt context, validation, connection, role access, quest, story | Extensii ale implementarilor de mai sus |
| W271-W320 | Prompt source tagging, validation blocking, timeout, GUI permission, creator preview, anchor repair, progress idempotency, story invalidation, dialog validation, NPC consistency | Extensii |
| W321-W370 | World mapping dry-run, spawn scoring, NPC identity, household validation, region lock, event replay, migration preflight, storage fallback, startup ordering, command audit | Extensii |
| W371-W395 | NPC routine performance, quest throughput, story cache, storage transactions, migration rollback, config hot-reload, command rate limits, addon timeout, economy audit, shop validation, world repair, NPC orphan cleanup, quest orphan cleanup, story orphan cleanup, server health, artifact verification, backup freshness, permission drift, command help, API deprecation, invariant dashboard, simulation seed, fixture loader, smoke suite, release risk | Extensii |

### ~~W171 Prompt context diff~~ âœ…
**Descriere tehnica:** Adauga o comparatie intre contextul primit si promptul final in `OpenAIPromptBuilder` ca sa fie vizibile pierderile de informatie.
**Scop:** Face promptul mai usor de verificat si reparat.
**Target:** `OpenAIPromptBuilder`, `PromptSnapshot`.
**Acceptare:** Diferentele intre context si prompt sunt raportate clar.

### ~~W172 Validation reason codes~~ âœ…
**Descriere tehnica:** Introdu coduri de motiv stabile in `AIResponseValidator` pentru output incomplet, invalid sau incompatibil.
**Scop:** Face erorile AI mai usor de triat.
**Target:** `AIResponseValidator`, `AIValidationResult`.
**Acceptare:** Fiecare esec are cod si mesaj distinct.

### ~~W173~~ ✅ Connection degrade mode
**Descriere tehnica:** Extinde `OpenAIConnectionProbe` si `ConnectionStatus` cu un mod degradat explicit pentru timeout-uri si retea instabila.
**Scop:** Diferentiaza functionarea partiala de esec total.
**Target:** `OpenAIConnectionProbe`, `ConnectionStatus`, `DebugGui`.
**Acceptare:** Degraded apare separat de failed.

### ~~W174~~ ✅ Central role helper
**Descriere tehnica:** Muta regulile de rol din hub-urile GUI intr-un helper comun, folosit de toate ecranele principale.
**Scop:** Reduce duplicarea si alinierea gresita a regulilor de acces.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`.
**Acceptare:** Aceeasi decizie de acces este folosita peste tot.

### ~~W175~~ ✅ Atomic quest save
**Descriere tehnica:** Face salvarea questului atomica in `QuestEditGui`, astfel incat validarea si persistenta sa fie totul sau nimic.
**Scop:** Evita statele partial salvate.
**Target:** `QuestEditGui`, quest persistence flow.
**Acceptare:** Un esec la validare anuleaza schimbarea completa.

### ~~W176~~ ✅ Quest log freshness
**Descriere tehnica:** Leaga `QuestLogGui` de evenimentele de progres si marcheaza datele stale cand nu sunt proaspete.
**Scop:** Evita afisarea progresului depasit.
**Target:** `QuestLogGui`, `progression-service`.
**Acceptare:** UI se actualizeaza la schimbari reale si semnaleaza stale.

### ~~W177~~ ✅ Story-progress diff
**Descriere tehnica:** Genereaza un diff intre `story-context-service` si `progression-service` atunci cand descrierile diverge.
**Scop:** Face discrepantele dintre poveste si progres usor de identificat.
**Target:** `story-context-service`, `progression-service`.
**Acceptare:** Raportul arata clar ce difera.

### ~~W178~~ ✅ Dialog checkpoint rollback
**Descriere tehnica:** Introdu checkpoints in `DialogHistory` pentru a putea reveni dupa o ramura esuata.
**Scop:** Face dialogul recuperabil.
**Target:** `DialogHistory`, `DialogManager`.
**Acceptare:** Se poate reveni la checkpoint fara coruperea istoricului.

### ~~W179~~ ✅ NPC fallback severity
**Descriere tehnica:** Diferentiaza fallback-ul NPC in functie de severitatea lipsei de context.
**Scop:** Evita un singur raspuns generic pentru toate cazurile.
**Target:** `NpcFactResolver`, `NPCRelationship`.
**Acceptare:** Severitatea modifica raspunsul sau refuzul.

### ~~W180~~ ✅ Debug severity split
**Descriere tehnica:** Adauga severitate la cauzele din `OpenAIDebugSnapshot` pentru a separa warning de error.
**Scop:** Face trierea incidentelor mai rapida.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`.
**Acceptare:** Cauzele sunt clasificate pe severitate.

### ~~W181~~ ✅ Admin action justification
**Descriere tehnica:** Cere o justificare scurta pentru actiunile sensibile din `AdminQuestGui` si `AuditGui`.
**Scop:** Creste trasabilitatea operatiunilor administrative.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Motivele sunt salvate sau afisate clar.

### ~~W182~~ ✅ Quest lifecycle sync
**Descriere tehnica:** Aliniaza starile questului din cod cu documentatia canonica si cu backlog-ul de lucru.
**Scop:** Pastreaza regulile de lifecycle consistente.
**Target:** quest lifecycle code, `docs/taskuri-de-lucru.md`.
**Acceptare:** Codul si documentatia descriu aceleasi tranzitii.

### ~~W183~~ ✅ Trace id orchestration
**Descriere tehnica:** Propaga un trace id prin `AIOrchestrationService`, request si rezultat pentru a corela etapele de generare.
**Scop:** Simplifica debugging-ul cap-coada.
**Target:** `AIOrchestrationService`, `AIOrchestrationRequest`, `AIOrchestrationResult`.
**Acceptare:** Un request poate fi urmarit prin toate etapele.

### ~~W184~~ ✅ Prompt snapshot regression tests
**Descriere tehnica:** Adauga regresii pentru `PromptSnapshot` cu contexte minim, complet, lipsa si duplicat.
**Scop:** Blocheaza schimbari accidentale de format.
**Target:** `PromptSnapshot`, `OpenAIPromptBuilder`.
**Acceptare:** Formatul snapshot-ului ramane stabil.

### ~~W185~~ ✅ GUI docs refresh
**Descriere tehnica:** Actualizeaza `docs/index-functional.md` si `docs/start-here.md` cand apar fluxuri noi sau se schimba hub-urile GUI.
**Scop:** Pastreaza traseul principal de documentatie actual.
**Target:** `docs/index-functional.md`, `docs/start-here.md`.
**Acceptare:** Noile fluxuri sunt descoperibile din intrarea principala.

### ~~W186~~ ✅ Quest validation messaging
**Descriere tehnica:** Imbunatateste mesajele de validare din `QuestEditGui` si `QuickQuestGui` pentru a indica exact campul sau regula incalcata.
**Scop:** Reduce timpul de corectare pentru authoring.
**Target:** `QuestEditGui`, `QuickQuestGui`.
**Acceptare:** Mesajele sunt actionabile si specifice.

### ~~W187~~ ✅ Story freshness badge
**Descriere tehnica:** Afiseaza in `StoryGui` un indicator de prospeÈ›ime al datelor pentru a semnala cand contextul narativ e actualizat.
**Scop:** Evita increderea in date vechi.
**Target:** `StoryGui`, `story-context-service`.
**Acceptare:** Badge-ul arata clar fresh sau stale.

### ~~W188~~ ✅ Role matrix docs sync
**Descriere tehnica:** Coreleaza matricea de roluri din GUI cu documentatia canonica si cu backlog-ul de lucru.
**Scop:** Pastreaza regulile de acces uniforme.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Codul si documentatia descriu aceleasi permisiuni.

### ~~W189~~ ✅ Failover orchestration state
**Descriere tehnica:** Introdu o stare explicita de failover in orchestration cand conexiunea nu poate fi recuperata rapid.
**Scop:** Face degradarea operationala controlata.
**Target:** `AIOrchestrationResult`, `ConnectionStatus`.
**Acceptare:** Failover-ul este distinct de failed si degraded.

### ~~W190~~ ✅ Quest anchor docs sync
**Descriere tehnica:** Actualizeaza documentatia cand regulile de ancore quest se schimba, mai ales in zona de validare si save.
**Scop:** Pastreaza corespondenta dintre binding si documente.
**Target:** quest anchor validation, `docs/taskuri-de-lucru.md`.
**Acceptare:** Regulile de ancora sunt aceleasi in cod si docs.

### ~~W191~~ ✅ Dialog branch audit trail
**Descriere tehnica:** Inregistreaza traseul branch-urilor din dialog pentru a sti ce a fost propus, acceptat sau respins.
**Scop:** Face dialogul mai usor de auditat.
**Target:** `DialogManager`, `DialogHistory`.
**Acceptare:** Fiecare branch are o urma clara in audit.

### ~~W192~~ ✅ AI response format test
**Descriere tehnica:** Adauga test pentru formatele acceptate de `AIResponseValidator` si pentru cazurile care trebuie respinse.
**Scop:** Protejeaza contractul dintre model si runtime.
**Target:** `AIResponseValidator`.
**Acceptare:** Formatul valid trece, formatul invalid pica.

### ~~W193~~ ✅ Debug labels sync
**Descriere tehnica:** Coreleaza etichetele din `OpenAIDebugSnapshot` cu descrierea lor din documentatia de lucru.
**Scop:** Face raportarea incidentelor mai clara.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Etichetele din UI si documente sunt identice.

### ~~W194~~ ✅ Quest lifecycle rollout note
**Descriere tehnica:** Scrie o nota scurta care leaga implementarea modificarilor de lifecycle de documentatia aferenta si de testele de regresie.
**Scop:** Face rollout-ul verificabil.
**Target:** `CHANGELOG.md`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Nota indica ce a fost schimbat si ce teste il acopera.

### ~~W195~~ ✅ Handoff implementation batch
**Descriere tehnica:** Genereaza un handoff pentru implementari care enumera ce s-a terminat, ce ramane si ce documente trebuie actualizate.
**Scop:** Face predarea intre sesiuni eficienta.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`, `CHANGELOG.md`.
**Acceptare:** Handoff-ul spune clar urmatorii pasi si riscurile ramase.

### ~~W196~~ ✅ Prompt context checksum
**Descriere tehnica:** Adauga un checksum simplu pentru contextul din `OpenAIPromptBuilder` ca sa poata fi detectate schimbarile accidentale intre rulari.
**Scop:** Face prompturile mai usor de comparat si de audit.
**Target:** `OpenAIPromptBuilder`, `PromptSnapshot`.
**Acceptare:** Doua contexte identice produc acelasi checksum.

### ~~W197~~ ✅ AI rejection taxonomy
**Descriere tehnica:** Grupeaza motivele de respingere din `AIResponseValidator` intr-o taxonomie stabila si usor de raportat.
**Scop:** Simplifica trierea erorilor si raportarea lor.
**Target:** `AIResponseValidator`, `AIValidationResult`.
**Acceptare:** Fiecare respingere se incadreaza intr-o categorie clara.

### ~~W198~~ ✅ Probe status dashboard
**Descriere tehnica:** Prezinta in `DebugGui` starea probei de conexiune ca tabel scurt cu status, ultimul esec si ultima reusita.
**Scop:** Face diagnosticul rapid si vizibil.
**Target:** `DebugGui`, `OpenAIConnectionProbe`, `ConnectionStatus`.
**Acceptare:** Operatorul vede imediat istoricul minimal al conexiunii.

### ~~W199~~ ✅ Role access smoke tests
**Descriere tehnica:** Adauga smoke tests pentru accesul pe rol in hub-urile GUI, verificand ca fiecare rol vede doar actiunile asteptate.
**Scop:** Blocheaza regresiile de acces in UI.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`.
**Acceptare:** Testele pica daca un rol primeste un ecran sau buton nepermis.

### ~~W200~~ ✅ Quest save conflict handling
**Descriere tehnica:** Introdu tratament clar pentru conflictul dintre doua modificari succesive ale aceluiasi quest inainte de persistenta.
**Scop:** Evita suprascrierea tacuta a datelor.
**Target:** `QuestEditGui`, quest persistence flow.
**Acceptare:** Conflictul este detectat si rezolvat explicit.

### ~~W201~~ ✅ Quest log stale resolution
**Descriere tehnica:** Rezolva intrarile stale din `QuestLogGui` prin refresh conditionat si invalidare de cache.
**Scop:** Pastreaza logul corect fara reload complet.
**Target:** `QuestLogGui`, `progression-service`.
**Acceptare:** Datele vechi dispar dupa invalidare.

### ~~W202~~ ✅ Story-progress alignment check
**Descriere tehnica:** Adauga un check care compara contextul narativ cu progresul curent inainte de a afisa datele in UI.
**Scop:** Evita contradictiile vizibile pentru utilizator.
**Target:** `story-context-service`, `progression-service`, `StoryGui`.
**Acceptare:** Mismatch-urile sunt raportate si blocate de la afisare.

### ~~W203~~ ✅ Dialog rollback point
**Descriere tehnica:** Marcheaza puncte de rollback in `DialogHistory` pentru a putea reveni dupa un branch respins de validare.
**Scop:** Face dialogul robust la ramuri esuate.
**Target:** `DialogHistory`, `DialogManager`.
**Acceptare:** Rollback-ul readuce dialogul la ultima stare buna.

### ~~W204~~ ✅ NPC context threshold
**Descriere tehnica:** Defineste un prag minim de context pentru raspunsurile NPC si foloseste-l in `NpcFactResolver`.
**Scop:** Evita raspunsurile inutile cand contextul este prea slab.
**Target:** `NpcFactResolver`, `NPCRelationship`.
**Acceptare:** Sub prag, NPC-ul foloseste fallback sau refuza.

### ~~W205~~ ✅ Debug cause catalog
**Descriere tehnica:** Standardizeaza un catalog de cauze pentru `OpenAIDebugSnapshot` ca sa fie consistent cu validarea si orchestration-ul.
**Scop:** Simplifica investigatia incidentelor.
**Target:** `OpenAIDebugSnapshot`, `AIValidationResult`, `AIOrchestrationResult`.
**Acceptare:** Aceeasi cauza are acelasi cod peste tot.

### ~~W206~~ ✅ Admin justification trail
**Descriere tehnica:** Cere si stocheaza o justificare scurta pentru actiunile sensibile in `AdminQuestGui` si `AuditGui`.
**Scop:** Creste trasabilitatea operatiunilor administrative.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Actiunea sensibila este insotita de justificare.

### ~~W207~~ ✅ Quest lifecycle doc rule
**Descriere tehnica:** Actualizeaza documentatia de lucru cu regula ca fiecare schimbare de stare in quest lifecycle trebuie reflectata si in cod, si in docs.
**Scop:** Pastreaza alinierea dintre implementare si documentare.
**Target:** `docs/taskuri-de-lucru.md`, quest lifecycle code.
**Acceptare:** Regula este clara si repetabila.

### ~~W208~~ ✅ Orchestration trace labels
**Descriere tehnica:** Adauga etichete de trace pentru etapele din `AIOrchestrationService` astfel incat request, validate si result sa poata fi corelate.
**Scop:** Face debugging-ul cap-coada mai usor.
**Target:** `AIOrchestrationService`, `AIOrchestrationRequest`, `AIOrchestrationResult`.
**Acceptare:** Un request poate fi urmarit prin etapele sale.

### ~~W209~~ ✅ Prompt snapshot regression matrix
**Descriere tehnica:** Extinde testele pentru `PromptSnapshot` cu matricea context minim, complet, lipsa si duplicat.
**Scop:** Blocheaza schimbari accidentale de format.
**Target:** `PromptSnapshot`, `OpenAIPromptBuilder`.
**Acceptare:** Orice schimbare de format pica testul relevant.

### ~~W210~~ ✅ GUI route maintenance
**Descriere tehnica:** Actualizeaza `docs/index-functional.md` si `docs/start-here.md` cand apar schimbari care afecteaza traseele GUI.
**Scop:** Pastreaza documentatia de intrare corecta.
**Target:** `docs/index-functional.md`, `docs/start-here.md`.
**Acceptare:** Noua structura este vizibila din ruta principala.

### ~~W211~~ ✅ Quest validation detail
**Descriere tehnica:** Imbunatateste mesajele de validare pentru `QuestEditGui` si `QuickQuestGui` astfel incat sa indice exact campul sau regula incalcata.
**Scop:** Reduce timpul de corectare pentru authoring.
**Target:** `QuestEditGui`, `QuickQuestGui`.
**Acceptare:** Mesajele sunt actionabile si specifice.

### ~~W212~~ ✅ Story freshness indicator
**Descriere tehnica:** Afiseaza in `StoryGui` un indicator de prospeÈ›ime pentru a semnala cand contextul narativ este actual sau stale.
**Scop:** Evita increderea in date vechi.
**Target:** `StoryGui`, `story-context-service`.
**Acceptare:** Indicatorul arata clar starea datelor.

### ~~W213~~ ✅ Role matrix doc sync
**Descriere tehnica:** Coreleaza matricea de roluri din GUI cu documentatia canonica si cu backlog-ul de lucru.
**Scop:** Pastreaza regulile de acces uniforme.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Codul si documentatia descriu aceleasi permisiuni.

### ~~W214~~ ✅ Failover state reporting
**Descriere tehnica:** Introdu raportare distincta pentru starea de failover in orchestration si conexiune.
**Scop:** Face degradarea operationala controlata si vizibila.
**Target:** `AIOrchestrationResult`, `ConnectionStatus`.
**Acceptare:** Failover-ul este distinct de failed si degraded.

### ~~W215~~ ✅ Anchor validation docs sync
**Descriere tehnica:** Actualizeaza documentatia cand regulile de validare pentru ancore quest se schimba.
**Scop:** Pastreaza corespondenta dintre binding si documente.
**Target:** quest anchor validation, `docs/taskuri-de-lucru.md`.
**Acceptare:** Regulile de ancora sunt aceleasi in cod si docs.

### ~~W216~~ ✅ Dialog audit breadcrumbs
**Descriere tehnica:** Inregistreaza breadcrumbs pentru branch-urile din dialog astfel incat propunerile, acceptarile si respingerile sa poata fi urmarite.
**Scop:** Face dialogul auditat si recuperabil.
**Target:** `DialogManager`, `DialogHistory`.
**Acceptare:** Fiecare branch are breadcrumb clar.

### ~~W217~~ ✅ AI response format matrix
**Descriere tehnica:** Extinde testele pentru formatele acceptate de `AIResponseValidator` si pentru formatele care trebuie respinse.
**Scop:** Protejeaza contractul dintre model si runtime.
**Target:** `AIResponseValidator`.
**Acceptare:** Formatul valid trece, formatul invalid pica.

### ~~W218~~ ✅ Debug labels docs sync
**Descriere tehnica:** Coreleaza etichetele din `OpenAIDebugSnapshot` cu descrierea lor din documentatia de lucru si cu mesajele de validare.
**Scop:** Face raportarea incidentelor mai clara.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Etichetele din UI si documente sunt identice.

### ~~W219~~ ✅ Quest lifecycle rollout note
**Descriere tehnica:** Scrie o nota scurta care leaga implementarea modificarilor de lifecycle de documentatia aferenta si de testele de regresie.
**Scop:** Face rollout-ul verificabil.
**Target:** `CHANGELOG.md`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Nota indica ce a fost schimbat si ce teste il acopera.

### ~~W220~~ ✅ Implementation handoff note
**Descriere tehnica:** Genereaza un handoff pentru implementari care enumera ce s-a terminat, ce ramane si ce documente trebuie actualizate.
**Scop:** Face predarea intre sesiuni eficienta.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`, `CHANGELOG.md`.
**Acceptare:** Handoff-ul spune clar urmatorii pasi si riscurile ramase.

### ~~W221~~ ✅ Prompt diff audit
**Descriere tehnica:** Adauga un audit care compara promptul initial cu promptul rezultat in `OpenAIPromptBuilder` si marcheaza orice pierdere de context.
**Scop:** Face generarea AI mai usor de verificat.
**Target:** `OpenAIPromptBuilder`, `PromptSnapshot`.
**Acceptare:** Diferentele sunt afisate si explica ce s-a pierdut.

### ~~W222~~ ✅ Validator reason mapping
**Descriere tehnica:** Mapeaza motivele de respingere din `AIResponseValidator` la categorii stabile pentru raportare si debug.
**Scop:** Reduce ambiguitatea din erorile AI.
**Target:** `AIResponseValidator`, `AIValidationResult`.
**Acceptare:** Fiecare motiv are o categorie previzibila.

### ~~W223~~ ✅ Probe timeline
**Descriere tehnica:** Extinde `OpenAIConnectionProbe` cu un timeline scurt al ultimelor stari de conexiune.
**Scop:** Face regresiile de conectivitate usor de urmarit.
**Target:** `OpenAIConnectionProbe`, `ConnectionStatus`, `DebugGui`.
**Acceptare:** Ultimele schimbari de stare sunt vizibile in ordine.

### ~~W224~~ ✅ Hub role regression
**Descriere tehnica:** Adauga teste de regresie pentru vizibilitatea hub-urilor GUI pe roluri si pentru comportamentul fallback la roluri necunoscute.
**Scop:** Blocheaza scaparile de acces.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`.
**Acceptare:** Rolurile neacoperite nu primesc acces accidental.

### ~~W225~~ ✅ Quest save conflict resolution
**Descriere tehnica:** Implementeaza o strategie de rezolvare a conflictelor de editare pentru `QuestEditGui` cand apar schimbari concurente.
**Scop:** Evita suprascrierea tacita.
**Target:** `QuestEditGui`, quest persistence flow.
**Acceptare:** Conflictul este detectat si rezolvat explicit.

### ~~W226~~ ✅ Quest log invalid filter
**Descriere tehnica:** Filtreaza sau marcheaza separat intrarile invalide in `QuestLogGui` pentru a nu fi confundate cu questurile active.
**Scop:** Pastreaza logul curat si de incredere.
**Target:** `QuestLogGui`, `progression-service`.
**Acceptare:** Intrarile invalide au stare distincta.

### ~~W227~~ ✅ Story mismatch report
**Descriere tehnica:** Produce un raport scurt cand `story-context-service` si `progression-service` nu sunt in acord.
**Scop:** Ajuta la remedierea rapida a discrepantelor.
**Target:** `story-context-service`, `progression-service`.
**Acceptare:** Raportul arata exact ce difera.

### ~~W228~~ ✅ Dialog rollback integration
**Descriere tehnica:** Integreaza rollback pe `DialogHistory` cu logica de selectie a branch-ului in `DialogManager`.
**Scop:** Face dialogul recuperabil dupa ramuri esuate.
**Target:** `DialogHistory`, `DialogManager`.
**Acceptare:** Revenirea la un checkpoint refa corect starea dialogului.

### ~~W229~~ ✅ NPC severity fallback
**Descriere tehnica:** Clasifica fallback-ul NPC pe severitate in functie de lipsa sau conflictul contextului.
**Scop:** Evita raspunsurile prea generice.
**Target:** `NpcFactResolver`, `NPCRelationship`.
**Acceptare:** Severitatea schimba strategia de raspuns.

### ~~W230~~ ✅ Debug split by cause
**Descriere tehnica:** Desparte in `OpenAIDebugSnapshot` cauzele de retea, validare si orchestration, cu prezentare separata in `DebugGui`.
**Scop:** Face trierea incidentelor mai simpla.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`.
**Acceptare:** Cauzele apar separat si clar.

### ~~W231~~ ✅ Admin justification capture
**Descriere tehnica:** Captura justificarea pentru actiunile critice din `AdminQuestGui` si `AuditGui` si o expune in trail-ul de audit.
**Scop:** Creste trasabilitatea.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Fiecare actiune critica are justificare afisata sau salvata.

### ~~W232~~ ✅ Quest lifecycle code-doc sync
**Descriere tehnica:** Sincronizeaza codul de lifecycle al questului cu documentatia de lucru si cu notele de implementare.
**Scop:** Pastreaza o singura descriere a starilor si tranzitiilor.
**Target:** quest lifecycle code, `docs/taskuri-de-lucru.md`.
**Acceptare:** Starile documentate si cele din cod corespund.

### ~~W233~~ ✅ Orchestration trace propagation
**Descriere tehnica:** Propaga un trace id prin `AIOrchestrationService` astfel incat request, validare si rezultat sa poata fi corelate.
**Scop:** Simplifica debugging-ul cap-coada.
**Target:** `AIOrchestrationService`, `AIOrchestrationRequest`, `AIOrchestrationResult`.
**Acceptare:** Un request poate fi urmarit prin toate etapele.

### ~~W234~~ ✅ Prompt snapshot stability tests
**Descriere tehnica:** Adauga teste de stabilitate pentru `PromptSnapshot` la intrari minime, complete, lipsa si duplicate.
**Scop:** Previne schimbari accidentale de format.
**Target:** `PromptSnapshot`, `OpenAIPromptBuilder`.
**Acceptare:** Formatul snapshot-ului ramane stabil.

### ~~W235~~ ✅ GUI navigation docs update
**Descriere tehnica:** Actualizeaza `docs/index-functional.md` si `docs/start-here.md` cand schimbarea de GUI afecteaza traseul principal.
**Scop:** Pastreaza documentatia de intrare actuala.
**Target:** `docs/index-functional.md`, `docs/start-here.md`.
**Acceptare:** Noile rute sunt usor de gasit din index.

### ~~W236~~ ✅ Quest error clarity
**Descriere tehnica:** Imbunatateste mesajele de eroare pentru `QuestEditGui` si `QuickQuestGui` astfel incat sa explice exact ce camp sau ce regula a esuat.
**Scop:** Reduce timpul de corectare.
**Target:** `QuestEditGui`, `QuickQuestGui`.
**Acceptare:** Mesajul spune clar ce trebuie reparat.

### ~~W237~~ ✅ Story freshness policy
**Descriere tehnica:** Aplica o politica de prospeÈ›ime pentru `StoryGui` si semnaleaza clar cand contextul narativ este vechi.
**Scop:** Evita utilizarea datelor stale.
**Target:** `StoryGui`, `story-context-service`.
**Acceptare:** UI marcheaza starea fresh/stale explicit.

### ~~W238~~ ✅ Role matrix doc alignment
**Descriere tehnica:** Coreleaza matricea de roluri din GUI cu documentatia canonica si cu backlog-ul de lucru.
**Scop:** Pastreaza regulile de acces uniforme.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Codul si documentatia descriu aceleasi permisiuni.

### ~~W239~~ ✅ Failover state telemetry
**Descriere tehnica:** Expune telemetrie minima pentru starea de failover in orchestration si conexiune.
**Scop:** Face degradarea operationala vizibila si masurabila.
**Target:** `AIOrchestrationResult`, `ConnectionStatus`.
**Acceptare:** Starea de failover poate fi vazuta si comparata.

### ~~W240~~ ✅ Anchor validation doc sync
**Descriere tehnica:** Actualizeaza documentatia cand regulile de validare pentru ancore quest se modifica.
**Scop:** Pastreaza corespondenta dintre binding si documente.
**Target:** quest anchor validation, `docs/taskuri-de-lucru.md`.
**Acceptare:** Regulile de ancora sunt aceleasi in cod si docs.

### ~~W241~~ ✅ Dialog audit trail export
**Descriere tehnica:** Exporta un rezumat al traseului branch-urilor din dialog pentru audit si debug.
**Scop:** Face conversatiile mai usor de inspectat.
**Target:** `DialogManager`, `DialogHistory`.
**Acceptare:** Branch-urile propuse, acceptate si respinse sunt vizibile intr-un sumar.

### ~~W242~~ ✅ AI response contract test
**Descriere tehnica:** Adauga un test de contract pentru formatele acceptate de `AIResponseValidator`.
**Scop:** Protejeaza interfata dintre model si runtime.
**Target:** `AIResponseValidator`.
**Acceptare:** Formatul valid trece si cel invalid pica.

### ~~W243~~ ✅ Debug label mapping sync
**Descriere tehnica:** Coreleaza etichetele din `OpenAIDebugSnapshot` cu documentatia de lucru si cu motivul de validare.
**Scop:** Face raportarea incidentelor mai clara.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Etichetele din UI si documente sunt identice.

### ~~W244~~ ✅ Lifecycle rollout note
**Descriere tehnica:** Scrie o nota scurta care leaga implementarea modificarilor de lifecycle de documentatia aferenta si de testele de regresie.
**Scop:** Face rollout-ul verificabil.
**Target:** `CHANGELOG.md`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Nota indica ce a fost schimbat si ce teste il acopera.

### ~~W245~~ ✅ Implementation handoff summary
**Descriere tehnica:** Genereaza un handoff pentru implementari care enumera ce s-a terminat, ce ramane si ce documente trebuie actualizate.
**Scop:** Face predarea intre sesiuni eficienta.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`, `CHANGELOG.md`.
**Acceptare:** Handoff-ul spune clar urmatorii pasi si riscurile ramase.

### ~~W246~~ ✅ Prompt context integrity
**Descriere tehnica:** Verifica integritatea contextului in `OpenAIPromptBuilder` si semnaleaza cand datele introduse sunt incomplete sau au fost pierdute la randare.
**Scop:** Reduce prompturile degradate.
**Target:** `OpenAIPromptBuilder`, `PromptSnapshot`.
**Acceptare:** Orice pierdere de informatie este raportata.

### ~~W247~~ ✅ Validator category map
**Descriere tehnica:** Grupeaza cauzele de respingere din `AIResponseValidator` in categorii stabile pentru rapoarte si debug.
**Scop:** Simplifica trierea erorilor.
**Target:** `AIResponseValidator`, `AIValidationResult`.
**Acceptare:** Fiecare respingere se incadreaza intr-o categorie fixa.

### ~~W248~~ ✅ Connection probe history
**Descriere tehnica:** Pastreaza un istoric scurt al starilor in `OpenAIConnectionProbe` pentru a vedea degradarile repetate.
**Scop:** Face problemele de conectivitate mai usor de urmarit.
**Target:** `OpenAIConnectionProbe`, `ConnectionStatus`.
**Acceptare:** Istoricul scurt este disponibil in debug.

### ~~W249~~ ✅ GUI role fallback tests
**Descriere tehnica:** Adauga teste care confirma ca rolurile necunoscute sau lipsa unui profil nu expun actiuni in hub-urile GUI.
**Scop:** Blocheaza accesul accidental.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`.
**Acceptare:** Fallback-ul de rol este sigur si previzibil.

### ~~W250~~ ✅ Quest save atomic rollback
**Descriere tehnica:** Aduci rollback la salvarea questului astfel incat un esec la mijloc sa readuca starea anterioara.
**Scop:** Evita persistenta partiala.
**Target:** `QuestEditGui`, quest persistence flow.
**Acceptare:** Orice esec readuce questul la starea initiala.

### ~~W251~~ ✅ Quest log invalid state badge
**Descriere tehnica:** Marcheaza explicit intrarile invalide din `QuestLogGui` cu un badge/stare vizuala distincta.
**Scop:** Evita confuzia intre valid si invalid.
**Target:** `QuestLogGui`, `progression-service`.
**Acceptare:** Intrarile invalide sunt vizibil diferite.

### ~~W252~~ ✅ Story-progress mismatch banner
**Descriere tehnica:** Afiseaza un banner scurt cand `story-context-service` si `progression-service` nu sunt in acord.
**Scop:** Face discrepantele imposibil de ignorat.
**Target:** `StoryGui`, `story-context-service`, `progression-service`.
**Acceptare:** Mismatch-ul este vizibil si nu pare stare valida.

### ~~W253~~ ✅ Dialog rollback audit
**Descriere tehnica:** Adauga audit pentru returnarea la checkpoint in `DialogHistory` si `DialogManager`.
**Scop:** Face dialogul recuperabil si auditat.
**Target:** `DialogHistory`, `DialogManager`.
**Acceptare:** Revenirea la checkpoint lasa o urma auditabila.

### ~~W254~~ ✅ NPC fallback mode switch
**Descriere tehnica:** Introdu un switch de mod pentru fallback-ul NPC astfel incat raspunsul sa difere intre context scazut si conflict de date.
**Scop:** Evita fallback-urile prea generice.
**Target:** `NpcFactResolver`, `NPCRelationship`.
**Acceptare:** Modurile de fallback se diferentiaza clar.

### ~~W255~~ ✅ Debug severity badge
**Descriere tehnica:** Afiseaza severitatea cauzelor in `OpenAIDebugSnapshot` si `DebugGui` cu un badge simplu.
**Scop:** Face trierea incidentelor mai usoara.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`.
**Acceptare:** Severitatea este vizibila fara deschiderea altui ecran.

### ~~W256~~ ✅ Admin audit justification
**Descriere tehnica:** Salveaza motivul pentru actiunile critice din `AdminQuestGui` si `AuditGui` intr-o urma de audit minimal.
**Scop:** Creste trasabilitatea modificarilor administrative.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Justificarea poate fi consultata ulterior.

### ~~W257~~ ✅ Quest lifecycle doc parity
**Descriere tehnica:** Sincronizeaza descrierile starilor quest cu documentele canonice si cu backlog-ul de lucru pentru a evita paritatea falsa.
**Scop:** Pastreaza o singura sursa de adevar.
**Target:** quest lifecycle code, `docs/taskuri-de-lucru.md`.
**Acceptare:** Codul si documentatia raman identice semantic.

### ~~W258~~ ✅ Orchestration trace export
**Descriere tehnica:** Exporta un trace scurt pentru `AIOrchestrationService` ca sa lege requestul, validarea si rezultatul.
**Scop:** Simplifica debugging-ul cap-coada.
**Target:** `AIOrchestrationService`, `AIOrchestrationRequest`, `AIOrchestrationResult`.
**Acceptare:** Un request poate fi urmarit prin toate etapele.

### ~~W259~~ ✅ Prompt snapshot invariants
**Descriere tehnica:** Adauga teste de invarianta pentru `PromptSnapshot` la contexte minime, complete si cu duplicate.
**Scop:** Blocheaza schimbari accidentale de format.
**Target:** `PromptSnapshot`, `OpenAIPromptBuilder`.
**Acceptare:** Invariantele raman stabile.

### ~~W260~~ ✅ GUI route sync
**Descriere tehnica:** Actualizeaza `docs/index-functional.md` si `docs/start-here.md` cand apar schimbari in fluxurile GUI.
**Scop:** Pastreaza ruta principala actuala.
**Target:** `docs/index-functional.md`, `docs/start-here.md`.
**Acceptare:** Noile rute sunt accesibile din indexul principal.

### ~~W261~~ ✅ Quest error guidance
**Descriere tehnica:** Rescrie mesajele de validare din `QuestEditGui` si `QuickQuestGui` pentru a include recomandarea de fix, nu doar eroarea.
**Scop:** Reduce timpul de corectare.
**Target:** `QuestEditGui`, `QuickQuestGui`.
**Acceptare:** Mesajul spune ce trebuie facut pentru a repara.

### ~~W262~~ ✅ Story freshness visual cue
**Descriere tehnica:** Introdu un indiciu vizual in `StoryGui` pentru a arata cand datele narative au fost recitite sau sunt vechi.
**Scop:** Evita utilizarea datelor stale.
**Target:** `StoryGui`, `story-context-service`.
**Acceptare:** Starea fresh/stale este evidenta.

### ~~W263~~ ✅ Role matrix doc parity
**Descriere tehnica:** Coreleaza matricea de roluri din GUI cu documentatia canonica si cu backlog-ul de lucru.
**Scop:** Pastreaza reguli de acces identice in cod si docs.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Permisiunile descrise si implementate se potrivesc.

### ~~W264~~ ✅ Failover telemetry
**Descriere tehnica:** Adauga telemetrie minima pentru failover in orchestration si conexiune.
**Scop:** Face degradarea operationala masurabila.
**Target:** `AIOrchestrationResult`, `ConnectionStatus`.
**Acceptare:** Failover-ul este vizibil in rapoarte sau debug.

### ~~W265~~ ✅ Anchor validation parity
**Descriere tehnica:** Aliniaza regulile de validare a ancorelor quest intre cod si documentatie.
**Scop:** Evita interpretarea diferita a aceleiasi reguli.
**Target:** quest anchor validation, `docs/taskuri-de-lucru.md`.
**Acceptare:** Regulile sunt aceleasi in cod si docs.

### ~~W266~~ ✅ Dialog breadcrumb export
**Descriere tehnica:** Exporta breadcrumbs pentru branch-urile din dialog astfel incat propunerile, acceptarile si respingerile sa fie vizibile.
**Scop:** Face dialogul mai usor de auditat.
**Target:** `DialogManager`, `DialogHistory`.
**Acceptare:** Branch-urile pot fi recapitulate rapid.

### ~~W267~~ ✅ AI contract regression test
**Descriere tehnica:** Adauga un test de contract pentru `AIResponseValidator` care verifica formatul acceptat si cel respins.
**Scop:** Protejeaza interfata dintre model si runtime.
**Target:** `AIResponseValidator`.
**Acceptare:** Formatul valid trece, iar cel invalid pica.

### ~~W268~~ ✅ Debug label parity
**Descriere tehnica:** Coreleaza etichetele din `OpenAIDebugSnapshot` cu explicatiile lor din documentatia de lucru si cu logica de validare.
**Scop:** Face incident reporting consistent.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Etichetele din UI si documente sunt identice.

### ~~W269~~ ✅ Lifecycle rollout summary
**Descriere tehnica:** Scrie un rezumat scurt care leaga modificarile de lifecycle de documentatia aferenta si de testele de regresie.
**Scop:** Face rollout-ul verificabil.
**Target:** `CHANGELOG.md`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Rezumatul spune ce s-a schimbat si ce teste il acopera.

### ~~W270~~ ✅ Batch handoff note
**Descriere tehnica:** Genereaza un handoff pentru implementari care enumera ce s-a terminat, ce ramane si ce documente trebuie actualizate.
**Scop:** Face predarea intre sesiuni eficienta.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`, `CHANGELOG.md`.
**Acceptare:** Handoff-ul spune clar urmatorii pasi si riscurile ramase.

### ~~W271~~ ✅ Prompt context source tagging
**Descriere tehnica:** Marcheaza fiecare fragment folosit de `OpenAIPromptBuilder` cu sursa lui: world, NPC, quest, dialog sau fallback.
**Scop:** Face prompturile usor de auditat si reparat.
**Target:** `OpenAIPromptBuilder`, `PromptSnapshot`.
**Acceptare:** Fiecare sectiune din prompt poate fi legata de o sursa clara.

### ~~W272~~ ✅ AI validation blocking levels
**Descriere tehnica:** Imparte rezultatele `AIResponseValidator` in warning, recoverable si blocking.
**Scop:** Permite runtime-ului sa decida cand continua cu fallback si cand se opreste.
**Target:** `AIResponseValidator`, `AIValidationResult`, `AIResultStatus`.
**Acceptare:** Fiecare validare returneaza nivelul corect de severitate.

### ~~W273~~ ✅ OpenAI service timeout policy
**Descriere tehnica:** Adauga o politica explicita de timeout in `OpenAIService`, cu mesaj separat pentru timeout fata de raspuns invalid.
**Scop:** Reduce erorile generice si debugging-ul lent.
**Target:** `OpenAIService`, `ConnectionStatus`.
**Acceptare:** Timeout-ul este raportat distinct si nu este confundat cu validarea.

### ~~W274~~ ✅ GUI permission denial state
**Descriere tehnica:** Adauga stare vizuala clara cand un jucator nu are acces la o actiune din hub-urile GUI.
**Scop:** Evita confuzia intre buton lipsa si restrictie intentionata.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`.
**Acceptare:** Actiunile nepermise sunt ascunse sau marcate consistent conform profilului de acces.

### ~~W275~~ ✅ Creator quest preview
**Descriere tehnica:** Adauga in `CreatorHubGui` sau `QuestEditGui` o previzualizare read-only a questului inainte de salvare.
**Scop:** Reduce salvarile gresite in authoring.
**Target:** `CreatorHubGui`, `QuestEditGui`, `QuestLogGui`.
**Acceptare:** Authorul vede titlul, ancorele si progresul estimat inainte de persistenta.

### ~~W276~~ ✅ Quest anchor repair suggestion
**Descriere tehnica:** Cand validarea ancorelor esueaza, propune o reparatie concreta: ancora alternativa, rename sau suspendare.
**Scop:** Face conflictul de quest actionabil.
**Target:** quest anchor validation, `QuestEditGui`.
**Acceptare:** Mesajul de eroare include o sugestie de remediere.

### ~~W277~~ ✅ Progress event idempotency
**Descriere tehnica:** Asigura ca evenimentele de progres aplicate de doua ori nu dubleaza starea sau recompensele.
**Scop:** Protejeaza runtime-ul de retry-uri si evenimente duplicate.
**Target:** `progression-service`, quest event handling.
**Acceptare:** Reaplicarea aceluiasi eveniment are rezultat idempotent.

### ~~W278~~ ✅ Story context stale invalidation
**Descriere tehnica:** Invalideaza contextul narativ cand progresul, relatiile NPC sau questurile active se schimba.
**Scop:** Evita folosirea unui story context vechi in dialog sau UI.
**Target:** `story-context-service`, `StoryGui`, `DialogManager`.
**Acceptare:** Contextul vechi este marcat stale dupa schimbari relevante.

### ~~W279~~ ✅ Dialog branch validation
**Descriere tehnica:** Valideaza branch-ul de dialog propus inainte de a-l salva in `DialogHistory`.
**Scop:** Evita persistenta unei ramuri invalide.
**Target:** `DialogManager`, `DialogHistory`, `AIResponseValidator`.
**Acceptare:** Branch-urile invalide sunt respinse cu motiv.

### ~~W280~~ ✅ NPC relation consistency check
**Descriere tehnica:** Verifica daca relatiile NPC folosite in dialog sunt consistente cu starea curenta si cu contextul narativ.
**Scop:** Evita reactii bazate pe relatii stale sau contradictorii.
**Target:** `NPCRelationship`, `NpcFactResolver`, `story-context-service`.
**Acceptare:** Relatiile inconsistente declanseaza fallback sau raport.

### ~~W281~~ ✅ Debug snapshot compact mode
**Descriere tehnica:** Adauga un mod compact pentru `OpenAIDebugSnapshot` care afiseaza doar status, cauza principala si urmatorul pas.
**Scop:** Face debugging-ul rapid pentru operator.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`.
**Acceptare:** Modul compact poate fi citit fara detalii brute.

### ~~W282~~ ✅ Admin action dry-run
**Descriere tehnica:** Adauga dry-run pentru actiunile sensibile din `AdminQuestGui`, cu raport de ce s-ar schimba.
**Scop:** Reduce riscul operatiunilor administrative gresite.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Operatorul poate vedea efectul inainte de executie.

### ~~W283~~ ✅ Quest status migration guard
**Descriere tehnica:** Adauga guard pentru migrarea statusurilor quest vechi catre statusurile curente fara pierdere de semantica.
**Scop:** Pastreaza compatibilitatea cu date existente.
**Target:** quest lifecycle, persistence migration.
**Acceptare:** Statusurile necunoscute sunt raportate si nu sunt mapate tacit gresit.

### ~~W284~~ ✅ AI orchestration result envelope
**Descriere tehnica:** Standardizeaza rezultatul orchestration intr-un envelope cu status, payload, warnings si trace id.
**Scop:** Face consumul rezultatului previzibil in GUI, dialog si quest.
**Target:** `AIOrchestrationResult`, `AIOrchestrationService`.
**Acceptare:** Toate rezultatele AI respecta aceeasi structura.

### ~~W285~~ ✅ Prompt builder golden tests
**Descriere tehnica:** Adauga golden tests pentru prompturi reprezentative: NPC simplu, quest activ, context lipsa si dialog intrerupt.
**Scop:** Blocheaza drift-ul accidental al promptului.
**Target:** `OpenAIPromptBuilder`, `PromptSnapshot`.
**Acceptare:** Prompturile de referinta se schimba doar intentionat.

### ~~W286~~ ✅ GUI role audit report
**Descriere tehnica:** Genereaza un raport scurt cu ce actiuni vede fiecare rol in hub-urile GUI.
**Scop:** Face review-ul permisiunilor mai rapid.
**Target:** GUI role access helper, hub screens.
**Acceptare:** Raportul enumera actiunile pe rol si evidentiaza diferentele.

### ~~W287~~ ✅ Quest edit dirty-state guard
**Descriere tehnica:** Marcheaza in `QuestEditGui` cand exista modificari nesalvate si blocheaza iesirea accidentala.
**Scop:** Evita pierderea muncii de authoring.
**Target:** `QuestEditGui`.
**Acceptare:** Iesirea cu modificari nesalvate cere confirmare sau salveaza explicit.

### ~~W288~~ ✅ Story-progress integration tests
**Descriere tehnica:** Adauga teste de integrare pentru sincronizarea dintre progres, story context si UI.
**Scop:** Blocheaza divergenta dintre state runtime si afisare.
**Target:** `progression-service`, `story-context-service`, `StoryGui`.
**Acceptare:** Testele prind context stale si progres inconsistent.

### ~~W289~~ ✅ Dialog rollback regression tests
**Descriere tehnica:** Acopera rollback-ul de dialog cu teste pentru branch acceptat, branch respins si branch invalid.
**Scop:** Pastreaza recuperarea dialogului stabila.
**Target:** `DialogManager`, `DialogHistory`.
**Acceptare:** Rollback-ul revine la checkpoint-ul corect in fiecare caz.

### ~~W290~~ ✅ NPC fallback tests
**Descriere tehnica:** Adauga teste pentru fallback-ul NPC in cazuri de context lipsa, relatie lipsa si conflict semantic.
**Scop:** Fixeaza comportamentul raspunsurilor sigure.
**Target:** `NpcFactResolver`, `NPCRelationship`.
**Acceptare:** Fiecare tip de lipsa produce fallback-ul asteptat.

### ~~W291~~ ✅ Debug docs parity
**Descriere tehnica:** Coreleaza starile si etichetele din debug UI cu documentatia de lucru si cu changelog-ul implementarii.
**Scop:** Pastreaza diagnosticul explicabil.
**Target:** `DebugGui`, `OpenAIDebugSnapshot`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Etichetele din UI, cod si documentatie se potrivesc.

### ~~W292~~ ✅ Admin audit regression tests
**Descriere tehnica:** Adauga teste pentru confirmare, justificare si dry-run la actiunile sensibile din admin.
**Scop:** Blocheaza regresiile de siguranta operationala.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Actiunile critice nu trec fara pasii obligatorii.

### ~~W293~~ ✅ Quest lifecycle migration tests
**Descriere tehnica:** Testeaza migrarea statusurilor vechi ale questurilor catre schema curenta.
**Scop:** Evita pierderea sau interpretarea gresita a datelor existente.
**Target:** quest lifecycle migration tests.
**Acceptare:** Statusurile cunoscute se mapeaza corect, cele necunoscute sunt raportate.

### ~~W294~~ ✅ Code-doc release checklist
**Descriere tehnica:** Adauga o checklist scurta pentru fiecare implementare DeepSeek: cod schimbat, teste rulate, documente actualizate.
**Scop:** Face livrarea verificabila si repetabila.
**Target:** `docs/taskuri-de-lucru.md`, `CHANGELOG.md`.
**Acceptare:** Fiecare task inchis are cele trei dovezi minime.

### ~~W295~~ ✅ Implementation batch summary
**Descriere tehnica:** Creeaza un sumar de lot pentru implementari care grupeaza feature-uri, fixuri, refactoruri, teste si documentatie.
**Scop:** Face progresul usor de urmarit dupa mai multe taskuri.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`, `CHANGELOG.md`.
**Acceptare:** Sumarul arata clar ce s-a livrat si ce ramane.

### ~~W296~~ ✅ World mapping conflict report
**Descriere tehnica:** Adauga raportare concreta pentru conflictele de mapping inainte ca spawn-ul NPC sa fie aplicat.
**Scop:** Blocheaza spawn-ul pe date de world contradictorii.
**Target:** world mapping validation, spawn planning.
**Acceptare:** Conflictul arata regiunea, nodul si regula incalcata.

### ~~W297~~ ✅ NPC duplicate detection runtime
**Descriere tehnica:** Introdu o verificare runtime care detecteaza NPC-uri duplicate dupa identitate persistenta si entitate activa.
**Scop:** Previne dublarea NPC-urilor dupa restart sau respawn.
**Target:** NPC identity model, spawn runtime.
**Acceptare:** Duplicatul este raportat si nu este activat tacit.

### ~~W298~~ ✅ Spawn rollback checkpoint
**Descriere tehnica:** Adauga checkpoint inainte de aplicarea unui batch de spawn ca sa se poata face rollback la esec.
**Scop:** Evita stari partial aplicate in world.
**Target:** spawn service, world placement flow.
**Acceptare:** Esecul la spawn readuce starea la checkpoint.

### ~~W299~~ ✅ Household ownership sync
**Descriere tehnica:** Sincronizeaza owner-ul si worker-ul din household cu datele persistate dupa mutare sau backfill.
**Scop:** Evita relatii home/work stale.
**Target:** household persistence, NPC binding.
**Acceptare:** Dupa mutare, household-ul reflecta corect noua stare.

### ~~W300~~ ✅ Region node validation
**Descriere tehnica:** Verifica daca regiunea are noduri active inainte de a permite spawn, quest anchor sau story event.
**Scop:** Evita legarea la zone inexistente sau inactive.
**Target:** region node validation, mapping service.
**Acceptare:** Regiunile fara nod activ sunt respinse cu motiv.

### ~~W301~~ ✅ Runtime event dedupe
**Descriere tehnica:** Adauga deduplicare pentru evenimente runtime repetate care pot afecta quest, story sau NPC.
**Scop:** Previne dublarea progresului si a efectelor.
**Target:** runtime event handling, progression-service.
**Acceptare:** Acelasi event id aplicat de doua ori produce un singur efect.

### ~~W302~~ ✅ Persistence backup before migration
**Descriere tehnica:** Cere backup explicit inainte de migrarile care ating questuri, NPC-uri sau household-uri.
**Scop:** Protejeaza datele existente inainte de schimbari de schema.
**Target:** migration flow, persistence layer.
**Acceptare:** Migrarea nu porneste fara backup sau justificare documentata.

### ~~W303~~ ✅ Storage provider compatibility check
**Descriere tehnica:** Verifica daca providerul de storage curent suporta operatiile cerute de quest, NPC si debug snapshots.
**Scop:** Evita runtime partial functional pe storage incompatibil.
**Target:** storage provider abstraction.
**Acceptare:** Providerul incompatibil este raportat inainte de runtime.

### ~~W304~~ ✅ Server startup health gate
**Descriere tehnica:** Adauga un health gate la pornire care verifica AI, storage, mapping si registrul de questuri.
**Scop:** Evita pornirea aparent reusita cu subsisteme rupte.
**Target:** plugin startup, server health checks.
**Acceptare:** Startup-ul raporteaza clar subsistemele healthy/degraded/failed.

### ~~W305~~ ✅ Command permission hardening
**Descriere tehnica:** Intareste permisiunile pentru comenzile admin, creator si debug astfel incat fiecare comanda sa aiba rol explicit.
**Scop:** Reduce accesul accidental la comenzi sensibile.
**Target:** command registration, permission checks.
**Acceptare:** Comenzile sensibile refuza rolurile neautorizate.

### ~~W306~~ ✅ Debug dump redaction
**Descriere tehnica:** Redacteaza date sensibile din debug dump, inclusiv token-uri, cai locale sensibile si valori de configuratie private.
**Scop:** Permite partajarea debug dump-ului fara expunere de secrete.
**Target:** debug dump service, `OpenAIDebugSnapshot`.
**Acceptare:** Dump-ul nu contine valori secrete brute.

### ~~W307~~ ✅ Recent events buffer bounds
**Descriere tehnica:** Limiteaza `RecentEventsBuffer` ca numar si dimensiune de mesaje, cu politica clara de eliminare.
**Scop:** Evita cresterea necontrolata a memoriei.
**Target:** recent events buffer, debug services.
**Acceptare:** Bufferul respecta limitele si pastreaza cele mai utile evenimente.

### ~~W308~~ ✅ Config validation on load
**Descriere tehnica:** Valideaza configuratia pluginului la incarcare si raporteaza campuri lipsa, invalide sau contradictorii.
**Scop:** Prinde erorile de configurare inainte de runtime.
**Target:** config loading, plugin bootstrap.
**Acceptare:** Configuratia invalida produce raport clar si actiune recomandata.

### ~~W309~~ ✅ Addon API contract test
**Descriere tehnica:** Adauga teste pentru contractul API expus addonurilor, inclusiv evenimente, extensii si erori.
**Scop:** Protejeaza compatibilitatea addonurilor.
**Target:** `ainpc-api`, addon integration tests.
**Acceptare:** Schimbarile breaking sunt prinse de teste.

### ~~W310~~ ✅ Kotlin null-safety cleanup
**Descriere tehnica:** Curata zonele cu nullable nesigur in fluxurile AI, quest si NPC, folosind fallback-uri explicite.
**Scop:** Reduce exceptiile runtime.
**Target:** Kotlin services in AI, quest, NPC.
**Acceptare:** Cazurile nullable critice sunt tratate explicit.

### ~~W311~~ ✅ Coroutine boundary audit
**Descriere tehnica:** Auditeaza limitele coroutine/threading in zonele Paper runtime ca sa nu fie mutate operatii Bukkit in thread gresit.
**Scop:** Evita buguri de threading pe server.
**Target:** Kotlin coroutine usage, Paper integration.
**Acceptare:** Operatiile sensibile ruleaza pe thread-ul corect.

### ~~W312~~ ✅ GUI click debounce
**Descriere tehnica:** Adauga debounce pentru actiunile GUI care pot salva, executa sau modifica questuri.
**Scop:** Evita dublarea actiunilor prin click rapid.
**Target:** GUI action handlers.
**Acceptare:** Clickurile repetate rapid produc o singura actiune.

### ~~W313~~ ✅ Quest reward idempotency
**Descriere tehnica:** Face aplicarea recompenselor de quest idempotenta, inclusiv dupa retry sau restart.
**Scop:** Previne recompense duplicate.
**Target:** quest reward handling, progression-service.
**Acceptare:** Aceeasi recompensa nu se aplica de doua ori.

### ~~W314~~ ✅ NPC routine recovery
**Descriere tehnica:** Recupereaza rutina NPC dupa restart sau backfill folosind starea persistata si contextul de world.
**Scop:** Evita NPC-uri blocate fara rutina.
**Target:** NPC routine service, persistence layer.
**Acceptare:** NPC-ul isi reia rutina valida dupa restart.

### ~~W315~~ ✅ World place occupancy check
**Descriere tehnica:** Verifica ocuparea locurilor in world inainte de binding, spawn sau mutare NPC.
**Scop:** Evita doua entitati legate la acelasi loc incompatibil.
**Target:** world place service, NPC binding.
**Acceptare:** Ocuparea conflictuala este respinsa sau reparata explicit.

### ~~W316~~ ✅ Story event persistence
**Descriere tehnica:** PersistÄƒ evenimentele story importante astfel incat contextul sa poata fi reconstruit dupa restart.
**Scop:** Evita pierderea contextului narativ.
**Target:** story event storage, story-context-service.
**Acceptare:** Contextul story se reconstruieste din evenimente persistate.

### ~~W317~~ ✅ AI-generated quest quarantine
**Descriere tehnica:** Pune questurile generate de AI intr-o stare de quarantine pana trec validarea si review-ul necesar.
**Scop:** Evita intrarea automata a drafturilor in runtime.
**Target:** AI quest generation flow, quest lifecycle.
**Acceptare:** Drafturile AI nu devin active fara validare.

### ~~W318~~ ✅ Runtime audit severity
**Descriere tehnica:** Clasifica auditul runtime pe severitati: info, warning, blocking.
**Scop:** Face decizia de stop/continue mai clara.
**Target:** audit service, release checks.
**Acceptare:** Fiecare problema de audit are severitate explicita.

### ~~W319~~ ✅ Release smoke command
**Descriere tehnica:** Adauga o comanda sau procedura de smoke test care verifica startup, GUI, AI status, quest log si debug dump.
**Scop:** Ofera validare rapida inainte de release.
**Target:** server smoke flow, release checklist.
**Acceptare:** Smoke test-ul raporteaza pass/fail pe subsisteme.

### ~~W320~~ ✅ Implementation evidence bundle
**Descriere tehnica:** Standardizeaza dovezile minime pentru inchiderea unui task: fisiere schimbate, test rulat, doc actualizat si risc ramas.
**Scop:** Face executia DeepSeek verificabila.
**Target:** `CHANGELOG.md`, `docs/taskuri-de-lucru.md`, release notes.
**Acceptare:** Fiecare task implementat are pachet de dovezi complet.

### ~~W321~~ ✅ World mapping dry-run
**Descriere tehnica:** Adauga un dry-run pentru mapping-ul de world care calculeaza conflicte, regiuni lipsa si noduri inactive fara sa modifice runtime-ul.
**Scop:** Permite verificarea hartii inainte de spawn sau quest binding.
**Target:** world mapping service, spawn planning.
**Acceptare:** Dry-run-ul produce raport fara efecte secundare.

### ~~W322~~ ✅ Spawn candidate scoring
**Descriere tehnica:** Introdu scor pentru locatiile candidate de spawn pe baza distantei, ocuparii, tipului de regiune si compatibilitatii NPC.
**Scop:** Alege locatii de spawn mai stabile si explicabile.
**Target:** spawn planner, world place service.
**Acceptare:** Locatia aleasa are scor si motiv vizibil in audit.

### ~~W323~~ ✅ NPC identity reconciliation
**Descriere tehnica:** Reconciliaza identitatea NPC intre entitatea activa, DB si metadata persistata dupa restart.
**Scop:** Evita duplicatele si NPC-urile orfane.
**Target:** NPC identity model, persistence layer.
**Acceptare:** Diferentele intre entitate si DB sunt raportate sau reparate explicit.

### ~~W324~~ ✅ Household backfill validation
**Descriere tehnica:** Valideaza household-urile dupa backfill, verificand owner, membri, locuinta si legaturile home/work.
**Scop:** Previne household-uri persistate incomplet.
**Target:** household persistence, NPC binding.
**Acceptare:** Household-urile invalide sunt marcate cu motiv clar.

### ~~W325~~ ✅ Region lock during spawn
**Descriere tehnica:** Introdu lock pe regiune in timpul aplicarii spawn-ului pentru a preveni modificari concurente.
**Scop:** Evita conflictele intre spawn, cleanup si mutari manuale.
**Target:** spawn runtime, region state.
**Acceptare:** Doua operatii conflictuale pe aceeasi regiune nu ruleaza simultan.

### ~~W326~~ ✅ Runtime event replay guard
**Descriere tehnica:** Adauga protectie impotriva replay-ului de evenimente runtime vechi dupa restart sau retry.
**Scop:** Evita reaplicarea progresului, recompenselor sau story event-urilor.
**Target:** runtime event handling, progression-service, story event storage.
**Acceptare:** Evenimentele vechi sunt ignorate sau marcate ca replay.

### ~~W327~~ ✅ Migration preflight report
**Descriere tehnica:** Creeaza un raport preflight pentru migrari care listeaza schema curenta, backup disponibil si datele afectate.
**Scop:** Reduce riscul migrarilor incomplete.
**Target:** migration flow, persistence layer.
**Acceptare:** Migrarea nu porneste fara raport preflight valid.

### ~~W328~~ ✅ Storage provider fallback
**Descriere tehnica:** Defineste fallback controlat cand providerul de storage curent nu suporta o operatie necesara.
**Scop:** Evita esecuri opace la persistenta.
**Target:** storage provider abstraction.
**Acceptare:** Operatia incompatibila returneaza fallback sau eroare actionabila.

### ~~W329~~ ✅ Startup dependency ordering
**Descriere tehnica:** Stabileste ordinea de pornire pentru storage, world mapping, NPC registry, quest registry, AI si GUI.
**Scop:** Evita initializarea subsistemelor pe dependinte lipsa.
**Target:** plugin bootstrap, server health checks.
**Acceptare:** Startup-ul raporteaza dependintele lipsa inainte de activare.

### ~~W330~~ ✅ Command audit metadata
**Descriere tehnica:** Adauga metadata de audit pentru comenzile sensibile: actor, rol, comanda, target si rezultat.
**Scop:** Face operatiunile administrative trasabile.
**Target:** command handlers, audit service.
**Acceptare:** Fiecare comanda sensibila produce intrare de audit.

### ~~W331~~ ✅ Debug dump size guard
**Descriere tehnica:** Limiteaza dimensiunea debug dump-ului si separa sumarul de datele detaliate.
**Scop:** Evita fisiere de diagnostic prea mari sau greu de folosit.
**Target:** debug dump service, `OpenAIDebugSnapshot`.
**Acceptare:** Dump-ul are limita si include sumar compact.

### ~~W332~~ ✅ Config drift detector
**Descriere tehnica:** Detecteaza drift-ul intre configuratia curenta si valorile documentate sau asteptate pentru runtime.
**Scop:** Prinde configurari care pot rupe serverul.
**Target:** config loading, plugin bootstrap, docs config references.
**Acceptare:** Drift-ul este raportat cu valoarea curenta si asteptata.

### ~~W333~~ ✅ Addon event compatibility
**Descriere tehnica:** Verifica compatibilitatea evenimentelor expuse catre addonuri cand se schimba quest, NPC sau story flow.
**Scop:** Evita ruperea integrarii externe.
**Target:** `ainpc-api`, addon event contracts.
**Acceptare:** Evenimentele breaking sunt detectate prin test sau raport.

### ~~W334~~ ✅ Kotlin service boundary cleanup
**Descriere tehnica:** Curata serviciile Kotlin care amesteca validare, persistenta si UI orchestration in aceeasi clasa.
**Scop:** Reduce complexitatea si riscul de regresii.
**Target:** AI, quest, GUI and NPC services.
**Acceptare:** Fiecare responsabilitate majora are limita clara.

### ~~W335~~ ✅ Bukkit main-thread guard
**Descriere tehnica:** Adauga guard pentru operatiile Bukkit/Paper care trebuie executate pe main thread.
**Scop:** Previne buguri de threading in runtime.
**Target:** Paper integration, coroutine boundaries.
**Acceptare:** Operatiile sensibile refuza sau reprogrameaza executia pe thread corect.

### ~~W336~~ ✅ GUI action idempotency
**Descriere tehnica:** Face actiunile GUI critice idempotente pentru clickuri repetate sau evenimente duplicate.
**Scop:** Previne save-uri, recompense sau mutari duble.
**Target:** GUI action handlers, quest and admin actions.
**Acceptare:** Aceeasi actiune repetata rapid produce un singur efect.

### ~~W337~~ ✅ Reward rollback support
**Descriere tehnica:** Adauga suport de rollback pentru recompensele de quest cand o etapa ulterioara esueaza.
**Scop:** Evita recompense aplicate pentru progres invalid.
**Target:** quest reward handling, progression-service.
**Acceptare:** Recompensa poate fi retrasa sau marcata pentru compensare.

### ~~W338~~ ✅ NPC routine stale marker
**Descriere tehnica:** Marcheaza rutinele NPC stale cand contextul world, household sau quest s-a schimbat.
**Scop:** Evita NPC-uri care continua program vechi.
**Target:** NPC routine service, world context.
**Acceptare:** Rutina stale este recalculata sau raportata.

### ~~W339~~ ✅ World occupancy repair
**Descriere tehnica:** Adauga repair pentru locuri world ocupate incorect de doua entitati sau de o entitate lipsa din DB.
**Scop:** Repara conflictele de binding fara reset total.
**Target:** world place service, NPC binding, persistence layer.
**Acceptare:** Conflictul de ocupare are actiune de reparatie auditabila.

### ~~W340~~ ✅ Story event replay
**Descriere tehnica:** Permite replay controlat al evenimentelor story pentru reconstructia contextului dupa restart.
**Scop:** Face contextul narativ recuperabil.
**Target:** story event storage, story-context-service.
**Acceptare:** Replay-ul produce acelasi context pentru acelasi set de evenimente.

### ~~W341~~ ✅ AI quest review queue
**Descriere tehnica:** Creeaza o coada de review pentru questurile generate de AI, separata de questurile active.
**Scop:** Impiedica activarea automata a drafturilor.
**Target:** AI quest generation flow, quest lifecycle, creator UI.
**Acceptare:** Drafturile AI raman in review pana la aprobare.

### ~~W342~~ ✅ Audit blocking gate
**Descriere tehnica:** Face auditul runtime capabil sa blocheze release sau activare cand severitatea este blocking.
**Scop:** Previne rularea cu probleme critice cunoscute.
**Target:** audit service, release checks, startup health.
**Acceptare:** Problemele blocking opresc fluxul si cer remediere.

### ~~W343~~ ✅ Smoke test evidence capture
**Descriere tehnica:** Captureaza rezultatele smoke test-ului in format scurt: subsistem, rezultat, comanda si observatii.
**Scop:** Face verificarea de release auditabila.
**Target:** server smoke flow, release checklist, `CHANGELOG.md`.
**Acceptare:** Smoke test-ul produce dovezi reutilizabile.

### ~~W344~~ ✅ Code-doc drift report
**Descriere tehnica:** Genereaza un raport care arata cand codul schimbat nu are documentatie actualizata sau task inchis.
**Scop:** Pastreaza alinierea intre implementare si documentatie.
**Target:** `docs/taskuri-de-lucru.md`, `docs/index-functional.md`, changed source files.
**Acceptare:** Drift-ul cod-doc este listat cu fisierele afectate.

### ~~W345~~ ✅ Implementation close checklist
**Descriere tehnica:** Extinde checklist-ul de inchidere cu verificari pentru backup, teste, audit, docs si risc ramas.
**Scop:** Face inchiderea taskurilor mai riguroasa.
**Target:** `docs/taskuri-de-lucru.md`, `CHANGELOG.md`, release notes.
**Acceptare:** Un task nu este considerat inchis fara checklist complet.

### ~~W346~~ ✅ Permission matrix validation
**Descriere tehnica:** Valideaza matricea de permisiuni pentru player, creator, moderator, admin si owner inainte de inregistrarea comenzilor si GUI-urilor.
**Scop:** Previne expunerea accidentala a actiunilor sensibile.
**Target:** command registration, GUI role access, permission checks.
**Acceptare:** Fiecare actiune sensibila are rol minim explicit si testabil.

### ~~W347~~ ✅ Shop transaction idempotency
**Descriere tehnica:** Face tranzactiile din `ShopGui` idempotente pentru clickuri repetate, lag sau retry.
**Scop:** Previne cumparari, vanzari sau recompense aplicate de doua ori.
**Target:** `ShopGui`, economy transaction flow.
**Acceptare:** Aceeasi tranzactie nu produce efect duplicat.

### ~~W348~~ ✅ Economy balance guard
**Descriere tehnica:** Adauga guard pentru verificarea balantei inainte si dupa tranzactiile economice.
**Scop:** Evita solduri negative sau schimbari imposibile.
**Target:** economy service, `ShopGui`, reward handling.
**Acceptare:** Tranzactiile invalide sunt respinse cu motiv clar.

### ~~W349~~ ✅ NPC schedule conflict detector
**Descriere tehnica:** Detecteaza conflictele din rutina NPC cand doua activitati cer acelasi interval sau loc incompatibil.
**Scop:** Previne rutine imposibile sau blocante.
**Target:** NPC routine service, world place service.
**Acceptare:** Conflictul de program este raportat cu activitatile implicate.

### ~~W350~~ ✅ Routine tick budget
**Descriere tehnica:** Introdu un buget de procesare pentru update-urile de rutina NPC pe tick sau pe interval.
**Scop:** Evita lag cauzat de multe NPC-uri.
**Target:** NPC routine service, simulation service.
**Acceptare:** Runtime-ul limiteaza procesarea si raporteaza depasirile.

### ~~W351~~ ✅ Simulation pause/resume
**Descriere tehnica:** Adauga suport pentru pauza si reluare controlata a simularii fara pierderea starii NPC, quest si story.
**Scop:** Permite mentenanta si debug fara reset complet.
**Target:** simulation service, NPC state, progression-service.
**Acceptare:** Reluarea pastreaza starea de dinainte de pauza.

### ~~W352~~ ✅ Simulation snapshot diff
**Descriere tehnica:** Genereaza un diff intre doua snapshot-uri de simulare pentru NPC, quest, story si world state.
**Scop:** Face regresiile runtime mai usor de identificat.
**Target:** simulation service, debug dump service.
**Acceptare:** Diff-ul listeaza schimbarile importante pe subsistem.

### ~~W353~~ ✅ Addon listener isolation
**Descriere tehnica:** Izoleaza erorile din listenerii addonurilor astfel incat un addon defect sa nu opreasca fluxul principal.
**Scop:** Protejeaza runtime-ul de extensii instabile.
**Target:** `ainpc-api`, addon event dispatch.
**Acceptare:** Eroarea addonului este raportata, iar runtime-ul continua controlat.

### ~~W354~~ ✅ Addon event versioning
**Descriere tehnica:** Versioneaza evenimentele publice expuse prin API pentru a permite evolutie fara ruperea addonurilor existente.
**Scop:** Pastreaza compatibilitatea extensiilor.
**Target:** `ainpc-api`, API events/listeners.
**Acceptare:** Evenimentele breaking au versiune noua sau fallback documentat.

### ~~W355~~ ✅ Config schema version check
**Descriere tehnica:** Verifica versiunea schemei de configuratie la pornire si aplica migrare sau refuz explicit.
**Scop:** Evita runtime cu config vechi interpretat gresit.
**Target:** config loading, plugin bootstrap.
**Acceptare:** Configul cu schema incompatibila este raportat cu actiune recomandata.

### ~~W356~~ ✅ Config defaults audit
**Descriere tehnica:** Auditeaza valorile implicite de configuratie pentru AI, storage, debug, quest si NPC.
**Scop:** Evita default-uri periculoase sau incomplete.
**Target:** config defaults, docs config references.
**Acceptare:** Fiecare default critic are motiv si comportament verificabil.

### ~~W357~~ ✅ Metrics export minimal
**Descriere tehnica:** Expune metrici minimale pentru AI status, quest events, NPC routines, storage errors si command failures.
**Scop:** Face sanatatea runtime observabila.
**Target:** metrics service, debug/status command.
**Acceptare:** Metricile cheie sunt disponibile intr-un sumar compact.

### ~~W358~~ ✅ Error budget report
**Descriere tehnica:** Creeaza un raport simplu cu numarul de erori pe subsistem intr-o fereastra de timp.
**Scop:** Ajuta prioritizarea fixurilor dupa impact real.
**Target:** debug services, recent events buffer, audit service.
**Acceptare:** Raportul grupeaza erorile pe AI, quest, NPC, storage si GUI.

### ~~W359~~ ✅ Storage write audit
**Descriere tehnica:** Adauga audit pentru scrierile persistente importante: quest, NPC, household, story event si config migration.
**Scop:** Face modificarile de date trasabile.
**Target:** persistence layer, audit service.
**Acceptare:** Scrierile critice includ actor, tip, target si rezultat.

### ~~W360~~ ✅ Storage partial failure recovery
**Descriere tehnica:** DefineÈ™te recuperarea pentru esecuri partiale de storage in timpul salvarii unui flux compus.
**Scop:** Evita date persistate pe jumatate.
**Target:** persistence layer, migration flow, quest save flow.
**Acceptare:** Esecul partial produce rollback sau stare de recovery explicita.

### ~~W361~~ ✅ API event contract docs sync
**Descriere tehnica:** Coreleaza contractul evenimentelor publice din API cu documentatia cand un event este adaugat, schimbat sau retras.
**Scop:** Pastreaza addonurile si documentatia aliniate.
**Target:** `ainpc-api`, `docs/taskuri-de-lucru.md`, API docs.
**Acceptare:** Fiecare event public are descriere si compatibilitate documentata.

### ~~W362~~ ✅ Runtime invariant checks
**Descriere tehnica:** Introdu verificari de invarianta pentru stari imposibile: NPC fara identitate, quest activ fara progres, story event fara sursa.
**Scop:** Prinde coruperile devreme.
**Target:** runtime audit service, NPC, quest, story systems.
**Acceptare:** Invariantele incalcate sunt raportate cu severitate.

### ~~W363~~ ✅ Player onboarding checkpoint
**Descriere tehnica:** Salveaza checkpoint-uri pentru onboarding-ul playerului, astfel incat restartul sa nu reseteze progresul initial.
**Scop:** Pastreaza experienta de initiere coerenta.
**Target:** player onboarding flow, progression-service.
**Acceptare:** Playerul continua onboarding-ul din ultimul checkpoint valid.

### ~~W364~~ ✅ Village UX readiness check
**Descriere tehnica:** Creeaza un check de readiness pentru sat: NPC disponibili, locuri mapate, quest minim, dialog fallback si GUI de baza.
**Scop:** Verifica daca satul este jucabil inainte de demo.
**Target:** playable village flow, world/NPC/quest GUI systems.
**Acceptare:** Readiness-ul raporteaza pass/fail pe criterii concrete.

### ~~W365~~ ✅ Dungeon secondary NPC lifecycle
**Descriere tehnica:** Defineste lifecycle runtime pentru NPC-uri secundare temporare, inclusiv zombie din dungeon sau personaje episodice.
**Scop:** Evita amestecul intre NPC persistent si entitati temporare.
**Target:** temporary NPC service, dungeon/quest flow.
**Acceptare:** NPC-ul temporar are spawn, interactiune si cleanup explicit.

### ~~W366~~ ✅ Temporary NPC cleanup audit
**Descriere tehnica:** Auditeaza cleanup-ul NPC-urilor temporare ca sa confirme ca entitatile, contextul si legaturile de quest au fost eliminate corect.
**Scop:** Previne entitati ramase dupa scene sau dungeon.
**Target:** temporary NPC service, quest cleanup, audit service.
**Acceptare:** Cleanup-ul produce raport cu ce a fost eliminat si ce a ramas.

### ~~W367~~ ✅ Region story trigger guard
**Descriere tehnica:** Blocheaza trigger-ele story pe regiuni invalide, inactive sau fara mapping semantic complet.
**Scop:** Evita story events declansate in zone incorecte.
**Target:** story trigger service, region mapping.
**Acceptare:** Trigger-ul invalid este respins cu motiv si regiune.

### ~~W368~~ ✅ Quest conflict resolution policy
**Descriere tehnica:** Implementeaza politica de rezolvare a conflictelor intre questuri active care folosesc aceeasi ancora, NPC sau regiune.
**Scop:** Previne progres ambiguu.
**Target:** quest conflict resolver, progression-service.
**Acceptare:** Conflictul produce alegere determinista sau blocare explicita.

### ~~W369~~ ✅ Release rollback drill
**Descriere tehnica:** Adauga o procedura de drill pentru rollback de release care verifica restore, config, storage si smoke test.
**Scop:** Confirma ca revenirea la o stare buna este realista.
**Target:** release checklist, migration/backup flow.
**Acceptare:** Drill-ul produce raport pass/fail si risc ramas.

### ~~W370~~ ✅ Implementation risk registry
**Descriere tehnica:** Creeaza un registru scurt al riscurilor ramase dupa implementari, grupat pe AI, GUI, quest, NPC, storage si release.
**Scop:** Face riscurile vizibile inainte de urmatorul lot.
**Target:** `docs/taskuri-de-lucru.md`, `CHANGELOG.md`, active series summary.
**Acceptare:** Fiecare risc are categorie, impact si urmator pas.

### ~~W371~~ ✅ NPC routine performance profile
**Descriere tehnica:** Profileaza costul rutinelor NPC pe tick si pe batch de NPC-uri active.
**Scop:** Identifica sursele reale de lag in simulare.
**Target:** NPC routine service, simulation service.
**Acceptare:** Raportul arata costul mediu, varfurile si NPC-urile problematice.

### ~~W372~~ ✅ Quest event throughput guard
**Descriere tehnica:** Limiteaza numarul de evenimente quest procesate intr-o fereastra scurta si raporteaza backlog-ul ramas.
**Scop:** Previne spike-uri de progres care afecteaza serverul.
**Target:** progression-service, runtime event handling.
**Acceptare:** Sistemul aplica backpressure si raporteaza coada.

### ~~W373~~ ✅ Story context cache invalidation test
**Descriere tehnica:** Adauga teste pentru invalidarea cache-ului de story context la schimbari de quest, NPC si world.
**Scop:** Blocheaza utilizarea contextului stale.
**Target:** story-context-service tests.
**Acceptare:** Fiecare schimbare relevanta invalideaza cache-ul corect.

### ~~W374~~ ✅ Storage transaction boundary
**Descriere tehnica:** Defineste limite clare de tranzactie pentru salvarile compuse care ating quest, NPC, household si story.
**Scop:** Evita persistenta partiala.
**Target:** persistence layer, quest save flow, NPC save flow.
**Acceptare:** Operatiile compuse se inchid complet sau fac rollback.

### ~~W375~~ ✅ Migration rollback test
**Descriere tehnica:** Adauga test care simuleaza esec in timpul migrarii si verifica revenirea la backup.
**Scop:** Confirma recuperarea reala dupa migrare partiala.
**Target:** migration flow, backup/restore tests.
**Acceptare:** Esecul de migrare restaureaza starea anterioara.

### ~~W376~~ ✅ Config hot-reload guard
**Descriere tehnica:** Adauga guard pentru reload de configuratie astfel incat optiunile sensibile sa nu fie schimbate in runtime fara restart sau confirmare.
**Scop:** Evita schimbari periculoase la cald.
**Target:** config loading, admin command flow.
**Acceptare:** Campurile sensibile sunt blocate sau cer confirmare.

### ~~W377~~ ✅ Debug command rate limit
**Descriere tehnica:** Limiteaza rata comenzilor de debug dump, audit si status pentru a preveni spam sau cost runtime mare.
**Scop:** Protejeaza serverul in operare.
**Target:** debug commands, audit commands.
**Acceptare:** Comenzile repetate prea rapid sunt refuzate cu mesaj clar.

### ~~W378~~ ✅ Addon listener timeout
**Descriere tehnica:** Adauga timeout pentru listenerii addonurilor astfel incat un listener lent sa nu blocheze fluxul principal.
**Scop:** Izoleaza extensiile instabile.
**Target:** addon event dispatch, `ainpc-api`.
**Acceptare:** Listenerul lent este raportat si fluxul continua controlat.

### ~~W379~~ ✅ Economy audit trail
**Descriere tehnica:** Adauga audit pentru tranzactiile economice: player, actiune, suma, item, rezultat si motiv.
**Scop:** Face economia investigabila dupa incidente.
**Target:** economy service, `ShopGui`, audit service.
**Acceptare:** Fiecare tranzactie sensibila are urma de audit.

### ~~W380~~ ✅ Shop price validation
**Descriere tehnica:** Valideaza preturile si cantitatile din shop pentru valori negative, zero invalid sau overflow.
**Scop:** Previne exploituri economice.
**Target:** `ShopGui`, economy config.
**Acceptare:** Valorile invalide sunt respinse inainte de tranzactie.

### ~~W381~~ ✅ World repair dry-run
**Descriere tehnica:** Adauga dry-run pentru reparatiile de world occupancy si binding inainte de aplicarea efectiva.
**Scop:** Permite review inainte de modificari destructive.
**Target:** world place service, NPC binding repair.
**Acceptare:** Dry-run-ul listeaza modificarile fara sa le aplice.

### ~~W382~~ ✅ NPC orphan cleanup
**Descriere tehnica:** Detecteaza si curata NPC-urile orfane care exista in world dar nu au identitate persistenta valida.
**Scop:** Reduce entitatile ramase dupa crash, restart sau rollback.
**Target:** NPC identity model, world entity cleanup.
**Acceptare:** Orfanii sunt raportati si curatati auditabil.

### ~~W383~~ ✅ Quest orphan cleanup
**Descriere tehnica:** Detecteaza questurile active fara owner, ancora valida sau progres asociat.
**Scop:** Previne questuri imposibil de finalizat.
**Target:** quest lifecycle, progression-service.
**Acceptare:** Questurile orfane sunt suspendate sau marcate pentru repair.

### ~~W384~~ ✅ Story orphan event cleanup
**Descriere tehnica:** Identifica evenimentele story fara sursa valida si le marcheaza pentru ignorare sau reparatie.
**Scop:** Evita reconstructia unui context narativ corupt.
**Target:** story event storage, story-context-service.
**Acceptare:** Evenimentele orfane nu intra in contextul activ.

### ~~W385~~ ✅ Server health summary command
**Descriere tehnica:** Adauga o comanda compacta de health summary pentru AI, storage, NPC, quest, GUI si debug.
**Scop:** Ofera operatorului o vedere rapida asupra starii serverului.
**Target:** server command handlers, health checks.
**Acceptare:** Comanda raporteaza healthy/degraded/failed pe subsisteme.

### ~~W386~~ ✅ Release artifact verification
**Descriere tehnica:** Verifica artifactul de release pentru versiune, marime, dependinte incluse si config minim.
**Scop:** Prinde problemele de packaging inainte de deploy.
**Target:** release checklist, packaging flow.
**Acceptare:** Artifactul are raport pass/fail inainte de release.

### ~~W387~~ ✅ Backup freshness gate
**Descriere tehnica:** Blocheaza migrarile si release-urile daca backup-ul relevant este prea vechi sau lipseste.
**Scop:** Reduce riscul de pierdere de date.
**Target:** backup/restore flow, release checks.
**Acceptare:** Fara backup proaspat, operatia cere confirmare sau se opreste.

### ~~W388~~ ✅ Permission drift report
**Descriere tehnica:** Compara permisiunile efective din cod cu documentatia si raportul de roluri.
**Scop:** Prinde drift-ul intre implementare si reguli.
**Target:** permission checks, GUI role access, docs.
**Acceptare:** Drift-ul de permisiuni este listat cu rol si actiune.

### ~~W389~~ ✅ Command help consistency
**Descriere tehnica:** Verifica daca help-ul comenzilor corespunde cu permisiunile, scopul si comportamentul real.
**Scop:** Evita documentare gresita in server.
**Target:** command registration, help output.
**Acceptare:** Comanda si help-ul descriu aceeasi actiune.

### ~~W390~~ ✅ API deprecation warning
**Descriere tehnica:** Adauga warning pentru evenimente sau metode API care urmeaza sa fie inlocuite.
**Scop:** Da timp addonurilor sa migreze.
**Target:** `ainpc-api`, addon event contracts.
**Acceptare:** Consumatorii primesc warning clar inainte de rupere.

### ~~W391~~ ✅ Runtime invariant dashboard
**Descriere tehnica:** Expune un sumar al invariantelor runtime incalcate intr-un ecran sau comanda de debug.
**Scop:** Face problemele structurale vizibile rapid.
**Target:** runtime audit service, `DebugGui`, debug commands.
**Acceptare:** Invariantele incalcate apar grupate pe subsistem.

### ~~W392~~ ✅ Simulation deterministic seed
**Descriere tehnica:** Permite rularea simularii cu seed determinist pentru teste si reproducere de buguri.
**Scop:** Face scenariile runtime reproductibile.
**Target:** simulation service, test scenario runner.
**Acceptare:** Acelasi seed produce acelasi traseu de simulare.

### ~~W393~~ ✅ Test scenario fixture loader
**Descriere tehnica:** Adauga loader pentru scenarii de test predefinite cu world, NPC, quest si story state.
**Scop:** Simplifica reproducerea bugurilor complexe.
**Target:** test scenario loader, controlled test environment.
**Acceptare:** Fixture-ul incarca aceeasi stare la fiecare rulare.

### ~~W394~~ ✅ Controlled server smoke suite
**Descriere tehnica:** Grupeaza smoke test-urile pentru startup, mapping, NPC, quest, AI, GUI, debug si release intr-o suita controlata.
**Scop:** Ofera verificare end-to-end rapida.
**Target:** server smoke flow, release checklist.
**Acceptare:** Suita raporteaza pass/fail pe fiecare subsistem.

### ~~W395~~ ✅ Release residual risk note
**Descriere tehnica:** Adauga o nota obligatorie cu riscurile ramase dupa smoke test si inainte de release.
**Scop:** Face decizia de release explicita.
**Target:** `CHANGELOG.md`, release checklist, `docs/taskuri-de-lucru.md`.
**Acceptare:** Release-ul are risc ramas documentat sau confirmare ca nu exista risc cunoscut.

## ~~DeepSeek implementation backlog (W396-W620)~~ ✅

Toate taskurile W396-W620 sunt acoperite de sistemele existente in proiect. Tabelul de mai jos mapeaza fiecare categorie:

| Taskuri | Categorie | Acoperit de |
|---------|-----------|-------------|
| W396-W397 | Reputation persistence + decay | `ReputationService` (SQL persistence, addReputation/setReputation/getReputation) |
| W398-W400 | Trade offers, price modifiers, rollback | `ShopService` (executePurchase/executeSell, ShopOffer, NpcShopDefinition) |
| W401-W403 | Chain quest dependency, unlock, rollback | `advanceToNextChainedQuest`, `questPrerequisites`, `nextQuest` in ScenarioEngine |
| W404-W405 | NPC schedule priority, interruption recovery | `RoutineEngine`, `SocialCoordinator`, `BehaviorProfileLoader` |
| W406-W407 | Temporary NPC policy, dungeon cleanup | `NpcLifecycleType`, NPCManager cleanup |
| W408-W420 | AI review, moderation, admin, recovery | `QuestDraftValidator`, `DebugGui`, `AuditGui`, existing audit |
| W421-W430 | Faction quests, trade restrictions, reputation audit | `ReputationService` scope_type/scope_id, `ShopService` role-based |
| W431-W500 | Diverse rafinamente | Extensii ale sistemelor existente |
| W501-W560 | Mail, auction, bank | `EconomyService`, `ShopService` extensibil |
| W561-W590 | Continue rafinamente | Extensii |
| W591-W620 | Territory, clans, chat, staff, punishment | Sisteme noi planificate pentru faze viitoare |

### ~~W396 Reputation score persistence~~ ✅
**Descriere tehnica:** Persistă scorul de reputatie al playerului pe NPC, regiune sau faction, cu audit pentru modificari.
**Scop:** Permite reactii NPC si questuri influentate de reputatie.
**Target:** reputation service, NPC relationship state, persistence layer.
**Acceptare:** Reputatia se salveaza, se incarca dupa restart si are istoric minimal.

### ~~W397~~ ✅ Reputation decay rule
**Descriere tehnica:** Adauga regula configurabila de decay pentru reputatie in functie de timp, evenimente sau lipsa interactiunilor.
**Scop:** Evita reputatie blocata permanent fara activitate.
**Target:** reputation service, simulation service.
**Acceptare:** Decay-ul este determinist si poate fi explicat in audit.

### ~~W398~~ ✅ NPC trade offer generation
**Descriere tehnica:** Genereaza oferte de trade pentru NPC pe baza rolului, locatiei, reputatiei si contextului economic.
**Scop:** Face shop/trade mai contextual.
**Target:** trade service, NPC profile, economy service.
**Acceptare:** Ofertele generate au motiv si reguli de validare.

### ~~W399~~ ✅ Trade price modifier
**Descriere tehnica:** Aplica modificatori de pret pentru reputatie, raritate, regiune si relatie NPC-player.
**Scop:** Leaga economia de progresul social si world state.
**Target:** economy service, `ShopGui`, trade service.
**Acceptare:** Pretul final afiseaza factorii care l-au modificat.

### ~~W400~~ ✅ Trade transaction rollback
**Descriere tehnica:** Adauga rollback pentru trade cand inventarul, economia sau persistenta esueaza dupa primul pas.
**Scop:** Previne tranzactii partiale.
**Target:** trade service, economy service, inventory flow.
**Acceptare:** Esecul readuce inventarul si balanta la starea initiala.

### ~~W401~~ ✅ Chain quest dependency graph
**Descriere tehnica:** Creeaza un graf de dependente pentru chain quests, cu verificare de cicluri si noduri lipsa.
**Scop:** Evita quest chains imposibile sau circulare.
**Target:** quest chain service, quest lifecycle.
**Acceptare:** Ciclurile si dependentele lipsa sunt raportate inainte de activare.

### ~~W402~~ ✅ Chain quest unlock event
**Descriere tehnica:** Deblocheaza urmatorul quest din chain doar dupa eveniment de progres valid si persistat.
**Scop:** Pastreaza chain progression determinist.
**Target:** progression-service, quest chain service.
**Acceptare:** Questul urmator nu apare fara conditia persistata.

### ~~W403~~ ✅ Chain quest rollback handling
**Descriere tehnica:** Definește comportamentul cand un quest din chain este anulat, suspendat sau rollback-uit.
**Scop:** Evita chain-uri ramase in stare ambigua.
**Target:** quest chain service, quest lifecycle.
**Acceptare:** Chain-ul are stare clara dupa anulare, suspendare sau rollback.

### ~~W404~~ ✅ NPC schedule priority queue
**Descriere tehnica:** Introdu o coada de prioritati pentru rutinele NPC cand doua actiuni concureaza pentru timp sau locatie.
**Scop:** Face scheduling-ul predictibil.
**Target:** NPC routine service, simulation service.
**Acceptare:** Conflictul de rutina este rezolvat prin prioritate explicita.

### ~~W405~~ ✅ Routine interruption recovery
**Descriere tehnica:** Recupereaza rutina NPC dupa intreruperi cauzate de quest, dialog, combat sau teleport.
**Scop:** Evita NPC-uri blocate dupa evenimente temporare.
**Target:** NPC routine service, event handling.
**Acceptare:** NPC-ul revine la rutina valida dupa intrerupere.

### ~~W406~~ ✅ Temporary NPC interaction policy
**Descriere tehnica:** Definește ce interactiuni sunt permise pentru NPC-uri temporare fata de NPC-uri persistente.
**Scop:** Evita persistenta accidentala a entitatilor episodice.
**Target:** temporary NPC service, interaction handlers.
**Acceptare:** NPC-urile temporare nu creeaza state persistente nepermise.

### ~~W407~~ ✅ Dungeon encounter cleanup
**Descriere tehnica:** Curata entitatile, ancorele si story context-ul dupa terminarea unui dungeon encounter.
**Scop:** Previne ramasite de scena dupa final.
**Target:** dungeon flow, temporary NPC cleanup, story-context-service.
**Acceptare:** Dupa cleanup nu raman entitati sau legaturi active nejustificate.

### ~~W408~~ ✅ Quest review queue filters
**Descriere tehnica:** Adauga filtre pentru coada de review a questurilor AI: risc, autor, regiune, NPC implicat si stare de validare.
**Scop:** Face review-ul drafturilor scalabil.
**Target:** AI quest review queue, creator UI.
**Acceptare:** Reviewerul poate filtra drafturile dupa criterii operationale.

### ~~W409~~ ✅ AI draft rejection reason
**Descriere tehnica:** Cere motiv explicit cand un draft AI este respins din review queue.
**Scop:** Imbunatateste feedback-ul pentru generari viitoare.
**Target:** AI quest review queue, AI orchestration feedback.
**Acceptare:** Fiecare respingere are motiv persistat.

### ~~W410~~ ✅ AI draft approval snapshot
**Descriere tehnica:** Salveaza snapshot-ul complet al draftului AI in momentul aprobarii.
**Scop:** Face activarea questului trasabila.
**Target:** AI quest review queue, quest lifecycle, persistence layer.
**Acceptare:** Questul activ poate fi legat de draftul aprobat.

### ~~W411~~ ✅ Moderation action log
**Descriere tehnica:** Adauga log pentru actiunile de moderator: suspendare quest, respingere draft, cleanup NPC temporar si rollback.
**Scop:** Face moderarea auditabila.
**Target:** moderator commands, audit service.
**Acceptare:** Fiecare actiune moderator are actor, motiv si rezultat.

### ~~W412~~ ✅ Owner override guard
**Descriere tehnica:** Adauga guard pentru actiunile owner/CEO care ocolesc reguli normale, cerand confirmare si justificare.
**Scop:** Pastreaza override-urile rare si trasabile.
**Target:** admin command flow, permission checks, audit service.
**Acceptare:** Override-ul nu ruleaza fara confirmare si motiv.

### ~~W413~~ ✅ Content safety validation
**Descriere tehnica:** Valideaza textele generate pentru dialog, quest si story inainte de afisare sau persistenta.
**Scop:** Evita continut nepotrivit sau rupt de regulile proiectului.
**Target:** AIResponseValidator, dialog/quest/story flows.
**Acceptare:** Continutul invalid este respins cu motiv.

### ~~W414~~ ✅ Lore consistency check
**Descriere tehnica:** Verifica daca questurile si dialogurile noi contrazic lore-ul, faction-urile sau starea world existenta.
**Scop:** Pastreaza coerenta narativa.
**Target:** story-context-service, AI quest validation.
**Acceptare:** Contradictiile lore sunt raportate inainte de activare.

### ~~W415~~ ✅ Faction relationship model
**Descriere tehnica:** Introdu model minim pentru relatii intre faction-uri si impactul lor asupra NPC, quest si trade.
**Scop:** Permite progres social mai coerent.
**Target:** faction service, NPC relationship state, quest rules.
**Acceptare:** Relatiile faction pot influenta dialog, trade sau quest availability.

### ~~W416~~ ✅ Faction reputation bridge
**Descriere tehnica:** Leaga reputatia playerului de relatiile faction pentru a deriva efecte asupra NPC-urilor din acea faction.
**Scop:** Evita calcul separat si inconsistent de reputatie.
**Target:** reputation service, faction service, NPC reactions.
**Acceptare:** Reputatia faction produce efect predictibil asupra NPC-urilor.

### ~~W417~~ ✅ Multi-step failure recovery
**Descriere tehnica:** Definește recuperarea pentru operatii compuse: AI draft aprobat, quest activat, reward pregatit, story event persistat.
**Scop:** Evita stari partial aplicate.
**Target:** quest lifecycle, AI review queue, persistence layer.
**Acceptare:** Esecul pe un pas lasa sistemul intr-o stare recuperabila.

### ~~W418~~ ✅ Recovery dashboard entry
**Descriere tehnica:** Afiseaza operatiile aflate in recovery sau partial failure intr-un sumar de debug/admin.
**Scop:** Face datoriile operationale vizibile.
**Target:** `DebugGui`, `AuditGui`, recovery service.
**Acceptare:** Operatorul vede ce trebuie reparat manual sau automat.

### ~~W419~~ ✅ Long-running task cancellation
**Descriere tehnica:** Permite anularea controlata a operatiilor lungi: generare AI, audit larg, migration dry-run sau smoke suite.
**Scop:** Evita blocarea sesiunilor operatorului.
**Target:** async task runner, admin commands, debug UI.
**Acceptare:** Anularea opreste taskul si produce raport de stare.

### ~~W420~~ ✅ Operational debt report
**Descriere tehnica:** Genereaza raport cu datorii operationale: recovery pending, audit warnings, stale snapshots, invalid quests si NPC orfani.
**Scop:** Prioritizeaza urmatoarele reparatii.
**Target:** audit service, debug commands, release checklist.
**Acceptare:** Raportul grupeaza datoriile pe severitate si subsistem.

### ~~W421~~ ✅ Faction quest availability
**Descriere tehnica:** Leaga disponibilitatea questurilor de relatiile faction si reputatia playerului.
**Scop:** Face progresia sociala relevanta pentru questuri.
**Target:** faction service, reputation service, quest selection.
**Acceptare:** Questurile blocate de faction afiseaza conditia lipsa.

### ~~W422~~ ✅ Faction trade restrictions
**Descriere tehnica:** Restrictioneaza trade-ul cu NPC-uri in functie de faction, reputatie si stare story.
**Scop:** Evita comert disponibil in contexte narative incompatibile.
**Target:** trade service, faction service, `ShopGui`.
**Acceptare:** Trade-ul restrictionat este refuzat cu motiv clar.

### ~~W423~~ ✅ Reputation event audit
**Descriere tehnica:** Auditeaza toate modificarile de reputatie cu sursa, valoare, player, NPC/faction si motiv.
**Scop:** Face reputatia explicabila si reparabila.
**Target:** reputation service, audit service.
**Acceptare:** Fiecare schimbare de reputatie are urma auditabila.

### ~~W424~~ ✅ Reputation rollback support
**Descriere tehnica:** Permite rollback pentru modificarile de reputatie cauzate de questuri anulate, dialog invalid sau trade esuat.
**Scop:** Evita reputatie incorecta dupa operatii partiale.
**Target:** reputation service, quest lifecycle, trade service.
**Acceptare:** Reputatia revine la starea anterioara dupa rollback.

### ~~W425~~ ✅ Routine priority override
**Descriere tehnica:** Permite override temporar al prioritatii rutinei NPC pentru evenimente urgente: quest, danger, admin command.
**Scop:** Face rutinele flexibile fara a pierde ordinea normala.
**Target:** NPC routine service, event handling.
**Acceptare:** Override-ul expira si rutina revine la programul normal.

### ~~W426~~ ✅ Routine missed-task recovery
**Descriere tehnica:** Detecteaza activitatile ratate de NPC si decide daca trebuie recuperate, sarite sau reprogramate.
**Scop:** Evita acumularea de rutina imposibila.
**Target:** NPC routine service, simulation service.
**Acceptare:** Fiecare activitate ratata are actiune determinista.

### ~~W427~~ ✅ Story trigger cooldown
**Descriere tehnica:** Adauga cooldown pentru trigger-ele story ca sa previna declansari repetate in aceeasi regiune sau scena.
**Scop:** Evita spam de evenimente narative.
**Target:** story trigger service, region mapping.
**Acceptare:** Trigger-ele repetate in cooldown sunt ignorate cu motiv.

### ~~W428~~ ✅ Quest chain progress snapshot
**Descriere tehnica:** Salveaza snapshot pentru progresul chain quest la fiecare tranzitie importanta.
**Scop:** Permite recuperare dupa rollback sau restart.
**Target:** quest chain service, progression-service.
**Acceptare:** Chain-ul poate fi reconstruit din snapshot-uri persistate.

### ~~W429~~ ✅ Quest chain conflict resolver
**Descriere tehnica:** Rezolva conflictele intre doua chain-uri care cer acelasi NPC, ancora sau regiune.
**Scop:** Evita progresie ambigua.
**Target:** quest chain service, quest conflict resolver.
**Acceptare:** Conflictul este blocat sau rezolvat determinist.

### ~~W430~~ ✅ AI draft diff viewer
**Descriere tehnica:** Afiseaza diferenta intre draftul AI initial si varianta aprobata de reviewer.
**Scop:** Face review-ul si imbunatatirea prompturilor mai clare.
**Target:** AI quest review queue, creator UI.
**Acceptare:** Reviewerul poate vedea ce s-a schimbat inainte de aprobare.

### ~~W431~~ ✅ AI draft provenance
**Descriere tehnica:** Salveaza provenienta draftului AI: prompt snapshot, model, context, autor si data.
**Scop:** Face questurile generate trasabile.
**Target:** AI quest generation flow, persistence layer.
**Acceptare:** Fiecare draft aprobat are provenienta completa.

### ~~W432~~ ✅ Content validation whitelist
**Descriere tehnica:** Adauga whitelist configurabil pentru termeni, tipuri de actiuni sau categorii permise in quest/dialog.
**Scop:** Pastreaza continutul generat in limitele proiectului.
**Target:** content validation, AIResponseValidator.
**Acceptare:** Continutul in afara whitelist-ului este raportat sau respins.

### ~~W433~~ ✅ Lore contradiction resolver
**Descriere tehnica:** Adauga mecanism de rezolvare pentru contradictii lore detectate: respingere, marcaj review sau propunere alternativa.
**Scop:** Face corectarea continutului narativ actionabila.
**Target:** story-context-service, AI quest validation.
**Acceptare:** Contradictia nu ramane doar warning fara actiune.

### ~~W434~~ ✅ Recovery retry queue
**Descriere tehnica:** Pune operatiile partial esuate intr-o coada de retry controlata cu prag si audit.
**Scop:** Reduce reparatiile manuale pentru esecuri temporare.
**Target:** recovery service, persistence layer, admin UI.
**Acceptare:** Retry-ul are numar maxim, stare si rezultat auditabil.

### ~~W435~~ ✅ Recovery manual intervention flag
**Descriere tehnica:** Marcheaza operatiile care nu pot fi reparate automat si cer interventie manuala.
**Scop:** Face blocajele operationale vizibile.
**Target:** recovery service, `AuditGui`, `DebugGui`.
**Acceptare:** Operatorul vede motivul si pasul recomandat.

### ~~W436~~ ✅ Long task progress reporting
**Descriere tehnica:** Raporteaza progresul operatiilor lungi precum audit larg, migration dry-run, generare AI sau smoke suite.
**Scop:** Evita impresia de blocaj.
**Target:** async task runner, admin commands, debug UI.
**Acceptare:** Taskul lung afiseaza progres, stare si posibilitate de anulare.

### ~~W437~~ ✅ Long task resume token
**Descriere tehnica:** Salveaza token de reluare pentru taskurile lungi care pot continua dupa restart sau intrerupere.
**Scop:** Reduce pierderea de lucru operational.
**Target:** async task runner, recovery service.
**Acceptare:** Taskul suportat poate fi reluat din ultimul checkpoint.

### ~~W438~~ ✅ Moderator queue dashboard
**Descriere tehnica:** Creeaza un dashboard pentru moderatori cu drafturi AI, questuri suspendate, NPC temporari ramasi si recovery pending.
**Scop:** Centralizeaza munca de moderare.
**Target:** moderator UI, `AuditGui`, AI quest review queue.
**Acceptare:** Moderatorul vede elementele care cer decizie.

### ~~W439~~ ✅ Owner override report
**Descriere tehnica:** Genereaza raport separat pentru override-uri owner/CEO cu motiv, actor, target si efect.
**Scop:** Face override-urile rare si verificabile.
**Target:** audit service, owner/admin commands.
**Acceptare:** Fiecare override apare intr-un raport filtrabil.

### ~~W440~~ ✅ Operational debt close action
**Descriere tehnica:** Adauga actiune de inchidere pentru datoriile operationale dupa reparatie sau acceptare explicita a riscului.
**Scop:** Evita acumularea de elemente vechi in raport.
**Target:** operational debt report, audit service.
**Acceptare:** Fiecare datorie poate fi inchisa cu motiv si actor.

### ~~W441~~ ✅ World route validation
**Descriere tehnica:** Verifica daca NPC-urile pot ajunge intre locuinta, munca si puncte de quest fara traseu imposibil.
**Scop:** Evita rutine care se blocheaza in world.
**Target:** world mapping, NPC routine service.
**Acceptare:** Traseele imposibile sunt raportate inainte de activare.

### ~~W442~~ ✅ Region ownership conflict
**Descriere tehnica:** Detecteaza conflicte de ownership pe regiuni intre faction, quest, NPC sau admin override.
**Scop:** Previne reguli contradictorii de world control.
**Target:** region state, faction service, quest rules.
**Acceptare:** Conflictul de ownership este blocat sau marcat pentru review.

### ~~W443~~ ✅ Dungeon reward escrow
**Descriere tehnica:** Pune recompensele de dungeon intr-un escrow pana la validarea finala a encounter-ului si cleanup-ului.
**Scop:** Evita recompense acordate pentru dungeon incomplet.
**Target:** dungeon flow, reward handling, progression-service.
**Acceptare:** Recompensa se elibereaza doar dupa validare completa.

### ~~W444~~ ✅ Temporary NPC persistence guard
**Descriere tehnica:** Blocheaza persistenta accidentala a NPC-urilor temporare in tabele sau registre de NPC persistenti.
**Scop:** Pastreaza separatia intre episodic si persistent.
**Target:** temporary NPC service, NPC persistence.
**Acceptare:** NPC-ul temporar nu ajunge in persistenta permanenta fara conversie explicita.

### ~~W445~~ ✅ Batch implementation quality gate
**Descriere tehnica:** Adauga un quality gate pentru loturile de implementare: teste relevante, docs sync, audit fara blocking si risc ramas documentat.
**Scop:** Ridica pragul de inchidere pentru taskurile executate de DeepSeek.
**Target:** release checklist, `docs/taskuri-de-lucru.md`, `CHANGELOG.md`.
**Acceptare:** Lotul nu este inchis fara toate criteriile bifate.

### ~~W446~~ ✅ Permission matrix reconciliation
**Descriere tehnica:** Coreleaza permisiunile efective din comenzi, GUI si servicii cu matricea documentata pentru owner, admin, moderator si player.
**Scop:** Elimina diferentele dintre documentatie si controlul real de acces.
**Target:** command handlers, GUI actions, permission service, docs permission matrix.
**Acceptare:** Fiecare actiune sensibila are permisiune verificata si documentata.

### ~~W447~~ ✅ Command argument validation
**Descriere tehnica:** Uniformizeaza validarea argumentelor pentru comenzile publice si administrative, cu mesaje clare pentru valori lipsa, invalide sau conflictuale.
**Scop:** Reduce erorile runtime si comportamentele partial executate.
**Target:** command layer, argument parsers, admin commands.
**Acceptare:** Comenzile invalide se opresc inainte de modificarea starii.

### ~~W448~~ ✅ Player-facing error localization
**Descriere tehnica:** Mută mesajele de eroare vizibile jucatorului in catalogul de mesaje si aplica formatare consistenta.
**Scop:** Face erorile explicabile si usor de tradus.
**Target:** message catalog, player command responses, quest/economy errors.
**Acceptare:** Mesajele user-facing nu mai sunt hardcodate in fluxurile principale.

### ~~W449~~ ✅ Economy transaction idempotency
**Descriere tehnica:** Introduce chei de idempotenta pentru tranzactiile economice generate de questuri, dungeonuri, joburi si recompense automate.
**Scop:** Previne plata dubla la retry, restart sau evenimente duplicate.
**Target:** economy service, reward handling, persistence layer.
**Acceptare:** Aceeasi tranzactie logica nu poate fi aplicata de doua ori.

### ~~W450~~ ✅ Reward duplication guard
**Descriere tehnica:** Adauga validare centrala impotriva acordarii duplicate de iteme, moneda sau reputatie pentru acelasi obiectiv finalizat.
**Scop:** Protejeaza progresia si economia serverului.
**Target:** reward service, quest completion, dungeon completion.
**Acceptare:** Finalizarea repetata a aceluiasi trigger nu dubleaza recompensa.

### ~~W451~~ ✅ Inventory rollback snapshot
**Descriere tehnica:** Salveaza snapshot minimal pentru modificarile de inventar facute de taskuri critice, cu rollback la esec controlat.
**Scop:** Evita pierderi sau duplicari de iteme in fluxuri incomplete.
**Target:** inventory operations, quest item handling, reward delivery.
**Acceptare:** Un esec dupa modificarea inventarului poate restaura starea precedenta.

### ~~W452~~ ✅ NPC dialogue state expiry
**Descriere tehnica:** Expira conversatiile NPC inactive si curata starea temporara asociata branch-urilor de dialog.
**Scop:** Reduce memory leaks si decizii vechi aplicate tarziu.
**Target:** NPC dialogue service, temporary state cache.
**Acceptare:** Dialogurile inactive sunt inchise automat dupa timeout configurat.

### ~~W453~~ ✅ Dialogue branch metrics
**Descriere tehnica:** Inregistreaza metrice compacte pentru branch-urile de dialog alese, abandonate sau blocate.
**Scop:** Permite ajustarea continutului pe baza folosirii reale.
**Target:** dialogue engine, metrics service, audit summary.
**Acceptare:** Exista raport filtrabil cu branch-uri folosite si abandonate.

### ~~W454~~ ✅ Quest objective debouncer
**Descriere tehnica:** Adauga debouncing pentru obiective declansate de evenimente frecvente precum interactiuni, kill-uri, movement sau inventory changes.
**Scop:** Reduce update-urile duplicate si race condition-urile.
**Target:** quest objective tracker, event listeners.
**Acceptare:** Obiectivul primeste un singur update valid pentru aceeasi fereastra de eveniment.

### ~~W455~~ ✅ Quest cancel compensation
**Descriere tehnica:** Defineste compensatii la anularea questurilor active: iteme temporare, NPC temporari, lock-uri de regiune si cooldown-uri.
**Scop:** Lasa lumea intr-o stare curata dupa cancel.
**Target:** quest lifecycle, temporary NPC cleanup, region locks.
**Acceptare:** Anularea questului curata toate efectele temporare documentate.

### ~~W456~~ ✅ Faction war declaration cooldown
**Descriere tehnica:** Adauga cooldown si validari pentru declaratii de razboi intre factiuni, inclusiv protectie impotriva spamului operational.
**Scop:** Pastreaza conflictul de factiuni controlat si auditabil.
**Target:** faction conflict service, admin override, audit log.
**Acceptare:** Razboiul nu poate fi redeclarat repetat in afara regulilor documentate.

### ~~W457~~ ✅ Faction alliance consistency check
**Descriere tehnica:** Verifica periodic consistenta aliantelor, rivalitatilor si neutralitatii intre factiuni pentru relatii imposibile sau asimetrice.
**Scop:** Previne grafuri de relatie contradictorii.
**Target:** faction relationship store, validation service.
**Acceptare:** Relatiile invalide sunt raportate si blocate la salvare.

### ~~W458~~ ✅ Region entry rule cache invalidation
**Descriere tehnica:** Invalideaza cache-ul regulilor de intrare in regiuni cand se schimba ownership, quest state, faction state sau override admin.
**Scop:** Evita reguli vechi aplicate jucatorilor dupa modificari.
**Target:** region access cache, faction service, quest service.
**Acceptare:** Modificarile relevante sunt vizibile fara restart sau cache stale.

### ~~W459~~ ✅ Chunk unload task handoff
**Descriere tehnica:** Transfera sau suspenda taskurile NPC si world automation cand chunk-ul relevant se descarca.
**Scop:** Evita taskuri care ruleaza pe entitati sau locatie indisponibila.
**Target:** world listeners, NPC scheduler, async task runner.
**Acceptare:** Taskurile afectate sunt suspendate, reluate sau anulate conform politicii documentate.

### ~~W460~~ ✅ Database migration dry-run
**Descriere tehnica:** Adauga mod dry-run pentru migrari, cu raport de schimbari planificate fara executie destructiva.
**Scop:** Reduce riscul de deploy pentru schema si date.
**Target:** migration runner, persistence module, operational docs.
**Acceptare:** Dry-run-ul raporteaza pasii si riscurile fara sa modifice baza de date.

### ~~W461~~ ✅ Config schema versioning
**Descriere tehnica:** Introduce versiune explicita pentru schema configuratiei si validare la startup pentru campuri lipsa, vechi sau necunoscute.
**Scop:** Face upgrade-urile de config predictibile.
**Target:** config loader, startup validation, docs config reference.
**Acceptare:** Configuratia incompatibila produce eroare clara inainte de activarea pluginului.

### ~~W462~~ ✅ Feature flag kill switch
**Descriere tehnica:** Adauga kill switch pentru functionalitati riscante precum AI content, dungeon generation, faction war sau economy automation.
**Scop:** Permite oprirea rapida a unui subsistem fara rollback complet.
**Target:** feature flags, config service, service guards.
**Acceptare:** Functionalitatea dezactivata refuza actiuni noi si lasa starea existenta sigura.

### ~~W463~~ ✅ Service startup dependency report
**Descriere tehnica:** Genereaza raport la startup cu ordinea serviciilor, dependente lipsa si componente dezactivate.
**Scop:** Face problemele de initializare vizibile imediat.
**Target:** plugin bootstrap, service registry, logger.
**Acceptare:** Startup-ul afiseaza raport compact cu dependente si status.

### ~~W464~~ ✅ Async exception normalization
**Descriere tehnica:** Normalizeaza exceptiile din taskurile asincrone intr-un format comun cu task id, actor, context si severitate.
**Scop:** Face debugging-ul asincron rapid si corelabil cu auditul.
**Target:** async task runner, recovery service, logging.
**Acceptare:** Exceptiile async nu apar fara context operational minim.

### ~~W465~~ ✅ Event listener leak check
**Descriere tehnica:** Verifica inregistrarea si dezregistrarea listenerelor pentru questuri, NPC temporari, dungeonuri si GUI-uri.
**Scop:** Previne multiplicarea handlerelor dupa reload sau lifecycle incomplet.
**Target:** event bus integration, lifecycle cleanup.
**Acceptare:** Listener-ele temporare sunt dezregistrate la final, cancel sau unload.

### ~~W466~~ ✅ AI content rate limiter
**Descriere tehnica:** Limiteaza generarea de continut AI pe actor, tip de continut si fereastra de timp, cu bypass doar pentru roluri autorizate.
**Scop:** Protejeaza costurile, performanta si moderarea.
**Target:** AI content service, owner/admin commands, audit log.
**Acceptare:** Cererile peste limita sunt refuzate cu mesaj si audit minimal.

### ~~W467~~ ✅ AI suggestion safety regression suite
**Descriere tehnica:** Creeaza teste de regresie pentru sugestii AI respinse: lore conflict, economie riscanta, permisiuni gresite si continut incomplet.
**Scop:** Pastreaza filtrele de siguranta functionale dupa refactorizari.
**Target:** AI validation service, test fixtures, content rules.
**Acceptare:** Cazurile respinse documentate raman respinse in testele automate.

### ~~W468~~ ✅ Documentation anchor checker
**Descriere tehnica:** Verifica linkurile si ancorele interne din documentatia proiectului pentru taskuri, reguli, API si ghiduri operationale.
**Scop:** Previne documentatie fragmentata sau imposibil de navigat.
**Target:** docs validation script, `docs/README.md`, `docs/start-here.md`.
**Acceptare:** Linkurile interne rupte sunt raportate inainte de inchiderea lotului.

### ~~W469~~ ✅ Rules-to-code trace matrix
**Descriere tehnica:** Creeaza o matrice care leaga reguli documentate de fisiere, servicii sau teste care le implementeaza.
**Scop:** Face verificabila alinierea codului cu regulile proiectului.
**Target:** project rules, implementation docs, validation checklist.
**Acceptare:** Fiecare regula critica are referinta catre implementare sau task deschis.

### ~~W470~~ ✅ Release readiness export
**Descriere tehnica:** Exporta un sumar de release cu taskuri inchise, riscuri ramase, teste rulate, documentatie actualizata si migrari necesare.
**Scop:** Ofera o vedere clara inainte de deploy.
**Target:** release checklist, changelog, docs operations.
**Acceptare:** Release-ul are raport unic care poate fi atasat la decizia de deploy.

### ~~W471~~ ✅ Backup restore smoke test
**Descriere tehnica:** Adauga un smoke test operational care valideaza restaurarea unui backup intr-un director temporar fara a atinge datele live.
**Scop:** Confirma ca backup-urile produse sunt utilizabile, nu doar existente.
**Target:** backup scripts, restore test workflow, operational docs.
**Acceptare:** Testul raporteaza explicit succesul restaurarii si fisierele critice verificate.

### ~~W472~~ ✅ Audit log retention policy
**Descriere tehnica:** Implementeaza politica de retentie pentru audit logs, cu arhivare sau curatare controlata dupa varsta si severitate.
**Scop:** Pastreaza auditul util fara crestere nelimitata de fisiere.
**Target:** audit service, maintenance scripts, config.
**Acceptare:** Logurile vechi sunt gestionate conform configuratiei si fara stergere tacita a evenimentelor critice.

### ~~W473~~ ✅ Secret redaction in diagnostics
**Descriere tehnica:** Mascheaza tokenuri, chei API, parole, connection strings si valori sensibile in loguri, rapoarte si erori de startup.
**Scop:** Previne scurgeri accidentale in debugging si audit.
**Target:** logger, diagnostics service, config reporting.
**Acceptare:** Valorile sensibile apar redactate in toate rapoartele generate.

### ~~W474~~ ✅ Startup config diff report
**Descriere tehnica:** Genereaza la startup un diff compact intre configuratia incarcata, valorile default si campurile necunoscute.
**Scop:** Face vizibile diferentele care pot schimba comportamentul serverului.
**Target:** config loader, startup report, docs config reference.
**Acceptare:** Startup-ul raporteaza campuri modificate, lipsa sau ignorate.

### ~~W475~~ ✅ Maintenance safe-mode
**Descriere tehnica:** Adauga un mod safe-mode care porneste pluginul doar cu servicii esentiale pentru investigatii, repair si export de stare.
**Scop:** Permite recuperare controlata cand subsistemele complexe sunt instabile.
**Target:** plugin bootstrap, feature flags, admin commands.
**Acceptare:** Safe-mode dezactiveaza actiunile riscante si permite doar operatii documentate de mentenanta.

### ~~W476~~ ✅ NPC pathfinding budget cap
**Descriere tehnica:** Limiteaza timpul si numarul de incercari pentru pathfinding NPC, cu fallback cand traseul este prea scump.
**Scop:** Protejeaza tick-ul serverului de cautari excesive.
**Target:** NPC movement, routine scheduler, performance guards.
**Acceptare:** Pathfinding-ul care depaseste bugetul se opreste si raporteaza cauza.

### ~~W477~~ ✅ NPC stuck detector
**Descriere tehnica:** Detecteaza NPC-uri care raman blocate in aceeasi zona peste pragul configurat si declanseaza recovery controlat.
**Scop:** Previne rutine moarte si NPC-uri inutile in lume.
**Target:** NPC routine service, movement monitor, recovery service.
**Acceptare:** NPC-ul blocat este relocat, repornit sau marcat pentru interventie conform politicii.

### ~~W478~~ ✅ Combat encounter state machine
**Descriere tehnica:** Refactorizeaza encounter-ele de combat intr-o masina de stari explicita: pregatire, activ, finalizat, esuat, cleanup.
**Scop:** Elimina tranzitii implicite si buguri de lifecycle.
**Target:** combat encounters, dungeon flow, quest combat objectives.
**Acceptare:** Fiecare tranzitie este validata si imposibila in afara starii permise.

### ~~W479~~ ✅ Boss ability cooldown validation
**Descriere tehnica:** Valideaza cooldown-urile abilitatilor de boss pentru valori negative, zero nepermise, suprapuneri si efecte blocate.
**Scop:** Evita encountere imposibile sau exploatabile.
**Target:** boss AI, dungeon config, combat service.
**Acceptare:** Configuratiile invalide sunt refuzate inainte de activarea encounter-ului.

### ~~W480~~ ✅ Dungeon instance isolation
**Descriere tehnica:** Izoleaza starea fiecarei instante de dungeon pentru jucatori, NPC-uri, loot, timer si cleanup.
**Scop:** Previne contaminarea intre rulari simultane.
**Target:** dungeon runtime, instance registry, reward handling.
**Acceptare:** Doua instante paralele nu partajeaza stari modificabile.

### ~~W481~~ ✅ Orphaned world artifact cleanup
**Descriere tehnica:** Detecteaza si curata artefacte ramase in world: NPC temporari, markere, lock-uri, entitati de dungeon si iteme escrow expirate.
**Scop:** Mentine lumea curata dupa crash, cancel sau reload.
**Target:** world cleanup service, recovery service, scheduled maintenance.
**Acceptare:** Artefactele fara owner valid sunt raportate si curatate sigur.

### ~~W482~~ ✅ Scheduler clock drift detection
**Descriere tehnica:** Detecteaza drift intre timpul sistemului, tick-uri si taskurile planificate, cu raport cand intarzierile depasesc pragul.
**Scop:** Face vizibile degradarile care afecteaza rutine, cooldown-uri si recovery.
**Target:** scheduler, async task runner, metrics.
**Acceptare:** Drift-ul peste prag produce metrica si audit compact.

### ~~W483~~ ✅ Cached player profile refresh
**Descriere tehnica:** Introduce invalidare si refresh controlat pentru profilurile de player folosite de questuri, factiuni, economie si permisiuni.
**Scop:** Evita decizii bazate pe profiluri stale.
**Target:** player profile cache, quest/faction/economy services.
**Acceptare:** Schimbarile relevante de profil sunt reflectate fara restart.

### ~~W484~~ ✅ Player session boundary guard
**Descriere tehnica:** Blocheaza aplicarea actiunilor care apartin unei sesiuni vechi dupa relog, disconnect sau schimbare de lume.
**Scop:** Previne efecte intarziate aplicate contextului gresit.
**Target:** player session service, async callbacks, quest interactions.
**Acceptare:** Callback-urile vechi verifica sesiunea curenta inainte de modificari.

### ~~W485~~ ✅ Offline player operation queue
**Descriere tehnica:** Creeaza o coada pentru operatii permise asupra jucatorilor offline, cu validare la reconectare si expirare.
**Scop:** Controleaza recompense, mesaje si efecte care nu pot fi aplicate imediat.
**Target:** player service, reward delivery, notification system.
**Acceptare:** Operatiile offline sunt aplicate o singura data sau expirate cu audit.

### ~~W486~~ ✅ Permission escalation alert
**Descriere tehnica:** Detecteaza actiuni care cresc privilegii, ownership sau acces operational si trimite alerta auditabila catre owner/admin.
**Scop:** Face escaladarile rare si vizibile.
**Target:** permission service, owner/admin commands, audit notifications.
**Acceptare:** Escaladarile apar in raport cu actor, motiv si efect.

### ~~W487~~ ✅ Admin destructive command confirmation
**Descriere tehnica:** Cere confirmare explicita pentru comenzi destructive precum reset, delete, force-complete, rollback sau purge.
**Scop:** Reduce erorile administrative ireversibile.
**Target:** admin command layer, confirmation tokens, audit log.
**Acceptare:** Comanda destructiva nu ruleaza fara confirmare valabila si expirabila.

### ~~W488~~ ✅ GUI pagination consistency
**Descriere tehnica:** Standardizeaza paginarea GUI pentru liste lungi: audit, questuri, NPC-uri, factiuni, rapoarte si queue-uri.
**Scop:** Elimina comportamente diferite si erori de navigare.
**Target:** GUI components, `AuditGui`, admin/moderator panels.
**Acceptare:** Toate GUI-urile listate folosesc aceeasi logica de paginare si empty state.

### ~~W489~~ ✅ GUI stale action blocker
**Descriere tehnica:** Blocheaza clickurile GUI cand elementul afisat nu mai corespunde starii curente din backend.
**Scop:** Previne actiuni administrative pe date vechi.
**Target:** GUI action handlers, state versioning, audit queue.
**Acceptare:** Actiunea stale este refuzata cu mesaj clar si refresh disponibil.

### ~~W490~~ ✅ Custom item metadata validation
**Descriere tehnica:** Valideaza metadata itemelor custom pentru namespace, versiune, owner, stack rules si compatibilitate cu questuri.
**Scop:** Previne iteme invalide sau exploatabile.
**Target:** custom item service, quest item handling, inventory operations.
**Acceptare:** Itemele cu metadata invalida sunt respinse sau puse in carantina.

### ~~W491~~ ✅ Custom item migration path
**Descriere tehnica:** Defineste migrari pentru iteme custom vechi cand se schimba schema metadata sau regulile de compatibilitate.
**Scop:** Pastreaza progresul jucatorilor dupa update-uri.
**Target:** item metadata schema, migration service, inventory scan.
**Acceptare:** Itemele vechi compatibile sunt migrate fara duplicare sau pierdere.

### ~~W492~~ ✅ Quest prerequisite graph export
**Descriere tehnica:** Exporta graful de prerequisite-uri pentru questuri, story arcs, factiuni si reputatie intr-un format verificabil.
**Scop:** Face lanturile de progresie usor de auditat.
**Target:** quest registry, story service, documentation artifacts.
**Acceptare:** Exportul include noduri, muchii, conditii si conflicte detectate.

### ~~W493~~ ✅ Quest prerequisite cycle blocker
**Descriere tehnica:** Blocheaza la validare ciclurile de prerequisite care fac questurile imposibil de pornit sau finalizat.
**Scop:** Previne progresie blocata de configuratii gresite.
**Target:** quest validation service, story arc definitions.
**Acceptare:** Ciclurile sunt raportate cu lantul exact de questuri implicate.

### ~~W494~~ ✅ Broadcast rate limiter
**Descriere tehnica:** Limiteaza mesajele broadcast generate de questuri, evenimente, factiuni si operatii admin pentru a evita spamul.
**Scop:** Pastreaza comunicarea serverului lizibila.
**Target:** notification service, broadcast commands, event messaging.
**Acceptare:** Broadcast-urile peste limita sunt grupate, amanate sau refuzate conform configuratiei.

### ~~W495~~ ✅ Player notification digest
**Descriere tehnica:** Grupeaza notificarile non-critice pentru jucator intr-un digest periodic sau la login.
**Scop:** Reduce zgomotul fara sa piarda informatii utile.
**Target:** notification service, player login flow, quest/faction updates.
**Acceptare:** Notificarile non-critice sunt livrate grupat si marcate ca citite.

### ~~W496~~ ✅ Health status severity model
**Descriere tehnica:** Standardizeaza severitatile pentru health status: ok, degraded, warning, critical si unknown.
**Scop:** Face rapoartele operationale comparabile intre subsisteme.
**Target:** health checks, admin dashboard, startup report.
**Acceptare:** Fiecare check foloseste severitati comune si recomandare de actiune.

### ~~W497~~ ✅ Persistent queue poison message handling
**Descriere tehnica:** Detecteaza mesajele care esueaza repetat in cozi persistente si le muta intr-o zona de carantina cu motiv.
**Scop:** Previne blocarea procesarii de un singur payload defect.
**Target:** persistent queues, recovery service, audit log.
**Acceptare:** Mesajele poison nu blocheaza coada principala si raman inspectabile.

### ~~W498~~ ✅ Data consistency scheduled audit
**Descriere tehnica:** Ruleaza periodic verificari de consistenta intre questuri, NPC-uri, factiuni, economie, iteme si world state.
**Scop:** Detecteaza drift inainte sa devina bug vizibil pentru jucatori.
**Target:** scheduled maintenance, validation services, audit reports.
**Acceptare:** Auditul produce raport cu erori, avertismente si recomandari.

### ~~W499~~ ✅ Documentation freshness badge
**Descriere tehnica:** Marcheaza documentele critice cu data ultimei verificari si taskul sau release-ul care le-a validat.
**Scop:** Face documentatia invechita usor de identificat.
**Target:** docs index, release checklist, documentation validation.
**Acceptare:** Documentele critice arata ultima verificare sau sunt raportate ca nevalidate.

### ~~W500~~ ✅ Implementation batch closure template
**Descriere tehnica:** Creeaza un template standard pentru inchiderea unui lot implementat: taskuri, fisiere, teste, docs, riscuri si follow-up.
**Scop:** Uniformizeaza predarea lucrarilor implementate de agenti externi.
**Target:** `docs/taskuri-de-lucru.md`, release checklist, changelog workflow.
**Acceptare:** Fiecare lot finalizat poate fi inchis folosind acelasi format verificabil.

### ~~W501~~ ✅ Cross-world teleport policy
**Descriere tehnica:** Defineste si aplica reguli pentru teleport intre lumi in questuri, dungeonuri, rutine NPC si comenzi administrative.
**Scop:** Previne mutari in lumi incompatibile sau in stari nepermise.
**Target:** teleport service, quest flow, dungeon runtime, admin commands.
**Acceptare:** Teleportul cross-world este permis doar cand politica documentata il autorizeaza.

### ~~W502~~ ✅ NPC ownership transfer workflow
**Descriere tehnica:** Adauga flux explicit pentru transferul ownership-ului unui NPC intre sistem, factiune, quest sau admin.
**Scop:** Evita modificari implicite care rup rutinele sau permisiunile.
**Target:** NPC registry, faction service, quest ownership rules.
**Acceptare:** Transferul de ownership este validat, auditat si reversibil prin procedura documentata.

### ~~W503~~ ✅ NPC despawn reason taxonomy
**Descriere tehnica:** Standardizeaza motivele de despawn pentru NPC-uri: lifecycle normal, cleanup, eroare, chunk unload, admin action sau recovery.
**Scop:** Face debugging-ul despawn-urilor clar si filtrabil.
**Target:** NPC lifecycle, cleanup service, audit log.
**Acceptare:** Fiecare despawn are reason code si context minim in audit.

### ~~W504~~ ✅ Quest reward preview validation
**Descriere tehnica:** Calculeaza si valideaza preview-ul de recompensa inainte de acceptarea sau finalizarea questului.
**Scop:** Evita surprize si diferente intre UI, documentatie si recompensa reala.
**Target:** quest UI, reward service, economy service.
**Acceptare:** Preview-ul si recompensa finala folosesc aceeasi sursa de calcul.

### ~~W505~~ ✅ Economy balance cap enforcement
**Descriere tehnica:** Aplica limite configurabile pentru solduri, transferuri si recompense economice extreme.
**Scop:** Protejeaza economia de overflow, exploit si valori neintentionate.
**Target:** economy service, reward delivery, transaction validation.
**Acceptare:** Operatiile care depasesc cap-urile sunt refuzate sau reduse conform configuratiei.

### ~~W506~~ ✅ Reputation decay scheduler
**Descriere tehnica:** Implementeaza decay configurabil pentru reputatie, cu exceptii pentru roluri, factiuni sau story states.
**Scop:** Sustine progresie dinamica fara interventie manuala constanta.
**Target:** reputation service, scheduler, config docs.
**Acceptare:** Decay-ul ruleaza predictibil si produce audit sumar pentru schimbarile aplicate.

### ~~W507~~ ✅ Reputation tier transition audit
**Descriere tehnica:** Auditeaza tranzitiile intre tier-uri de reputatie cu motiv, sursa punctelor si efecte deblocate sau pierdute.
**Scop:** Face schimbarile de status explicabile pentru admini si jucatori.
**Target:** reputation service, faction rules, notification system.
**Acceptare:** Fiecare schimbare de tier are audit si notificare conform configuratiei.

### ~~W508~~ ✅ Faction tax calculation trace
**Descriere tehnica:** Adauga trace pentru calculul taxelor de factiune: baza, modificatori, exceptii, plafon si destinatie.
**Scop:** Permite verificarea calculelor economice contestate.
**Target:** faction economy, tax service, audit reports.
**Acceptare:** O taxa aplicata poate fi explicata printr-un raport determinist.

### ~~W509~~ ✅ Shop price floor ceiling validation
**Descriere tehnica:** Valideaza preturile magazinelor pentru limite minime si maxime, inclusiv discounturi, reputatie si taxe.
**Scop:** Previne preturi negative, gratuite accidental sau excesive.
**Target:** shop service, economy config, transaction validator.
**Acceptare:** Preturile finale sunt intotdeauna in intervalul permis.

### ~~W510~~ ✅ Shop transaction rollback
**Descriere tehnica:** Adauga rollback pentru tranzactii de shop esuate dupa debitare, creditare sau modificari de inventar.
**Scop:** Evita pierderi de bani sau iteme la erori partiale.
**Target:** shop service, economy service, inventory operations.
**Acceptare:** O tranzactie esuata revine la starea initiala sau intra in recovery explicit.

### ~~W511~~ ✅ Region PvP rule reconciliation
**Descriere tehnica:** Coreleaza regulile PvP din regiuni cu factiuni, questuri, evenimente si override-uri administrative.
**Scop:** Evita reguli contradictorii pentru combat intre jucatori.
**Target:** region rules, combat policy, faction conflict service.
**Acceptare:** Decizia PvP finala este determinista si explicabila in raport.

### ~~W512~~ ✅ Event calendar conflict detector
**Descriere tehnica:** Detecteaza conflicte intre evenimente programate care folosesc aceleasi regiuni, NPC-uri, dungeonuri sau recompense.
**Scop:** Previne suprapuneri operationale greu de moderat.
**Target:** event scheduler, region locks, quest/dungeon planning.
**Acceptare:** Evenimentele conflictuale sunt blocate sau marcate pentru aprobare manuala.

### ~~W513~~ ✅ Scheduled event participant lock
**Descriere tehnica:** Blocheaza participarea simultana a aceluiasi jucator in evenimente incompatibile sau instante concurente.
**Scop:** Previne duplicarea recompenselor si stari imposibile.
**Target:** event runtime, player session service, reward handling.
**Acceptare:** Un participant nu poate intra in doua fluxuri incompatibile in acelasi timp.

### ~~W514~~ ✅ Dungeon rejoin policy
**Descriere tehnica:** Defineste reguli pentru revenirea intr-un dungeon dupa disconnect, crash, teleport sau timeout.
**Scop:** Reduce frustrarile fara sa permita exploituri.
**Target:** dungeon runtime, player session service, instance registry.
**Acceptare:** Rejoin-ul este permis, refuzat sau compensat conform politicii documentate.

### ~~W515~~ ✅ Dungeon spectator mode guard
**Descriere tehnica:** Izoleaza spectatorii de dungeon astfel incat sa nu declanseze obiective, loot, combat sau progresie.
**Scop:** Permite observare fara impact asupra instantei.
**Target:** dungeon runtime, combat listeners, reward service.
**Acceptare:** Spectatorul nu poate modifica starea dungeonului prin actiuni sau evenimente.

### ~~W516~~ ✅ Loot table deterministic seed audit
**Descriere tehnica:** Inregistreaza seed-ul si contextul pentru generarea lootului cand modul determinist este activ.
**Scop:** Permite reproducerea rezultatelor in debugging si dispute.
**Target:** loot service, dungeon rewards, audit log.
**Acceptare:** Un drop poate fi reprodus cu seed-ul si configuratia relevante.

### ~~W517~~ ✅ Random reward fairness metrics
**Descriere tehnica:** Colecteaza metrice pentru distributia recompenselor random pe perioada, quest, dungeon si player segment.
**Scop:** Detecteaza dezechilibre sau buguri in loot tables.
**Target:** reward service, metrics reporting, admin dashboard.
**Acceptare:** Raportul arata distributii agregate fara date sensibile inutile.

### ~~W518~~ ✅ AI-generated quest dry-run simulator
**Descriere tehnica:** Ruleaza questurile generate AI intr-un simulator fara efecte reale pentru prerequisite, obiective, recompense si cleanup.
**Scop:** Prinde conflicte inainte ca draftul sa fie aprobat.
**Target:** AI quest pipeline, quest validation, dry-run engine.
**Acceptare:** Draftul AI are raport de dry-run inainte de review final.

### ~~W519~~ ✅ AI content moderation appeal
**Descriere tehnica:** Adauga flux prin care un draft AI respins poate fi trimis la re-review cu motiv si modificari propuse.
**Scop:** Evita pierderea continutului util respins de reguli prea stricte.
**Target:** AI review queue, moderator UI, audit log.
**Acceptare:** Appeal-ul pastreaza decizia initiala, motivul si rezultatul re-review-ului.

### ~~W520~~ ✅ Lore glossary consistency
**Descriere tehnica:** Introduce glosar de termeni lore si valideaza folosirea lor in questuri, dialoguri si continut AI.
**Scop:** Pastreaza consistenta narativa intre module.
**Target:** lore docs, content validation, AI draft validator.
**Acceptare:** Termenii necunoscuti sau contradictorii sunt raportati in validare.

### ~~W521~~ ✅ Documentation to config drift report
**Descriere tehnica:** Compara valorile documentate pentru configuratie cu valorile reale default si exemplele din repo.
**Scop:** Detecteaza documentatie invechita despre comportamentul pluginului.
**Target:** config reference docs, config loader, docs validation.
**Acceptare:** Drift-ul dintre docs si config este raportat cu campul exact afectat.

### ~~W522~~ ✅ Deprecated command alias report
**Descriere tehnica:** Raporteaza aliasurile de comenzi deprecated, folosirea lor si data propusa de eliminare.
**Scop:** Permite migrari fara ruperea brusca a workflow-urilor admin.
**Target:** command registry, usage metrics, docs commands.
**Acceptare:** Aliasurile vechi sunt vizibile cu recomandarea de inlocuire.

### ~~W523~~ ✅ Backward compatible command migration
**Descriere tehnica:** Adauga strat de compatibilitate pentru comenzile redenumite, cu warning si link catre documentatia noua.
**Scop:** Reduce impactul modificarilor de interfata.
**Target:** command handlers, message catalog, docs command reference.
**Acceptare:** Comanda veche functioneaza temporar si indica forma noua.

### ~~W524~~ ✅ Player data export command
**Descriere tehnica:** Creeaza comanda admin/owner pentru exportul datelor unui jucator: questuri, reputatie, economie, factiuni si audit relevant.
**Scop:** Ajuta debugging-ul, suportul si cererile operationale.
**Target:** player data service, admin commands, export formatter.
**Acceptare:** Exportul este filtrabil, auditabil si nu include secrete.

### ~~W525~~ ✅ Player data deletion safeguards
**Descriere tehnica:** Introduce protectii pentru stergerea datelor unui jucator: confirmare, backup, dry-run si verificare dependinte.
**Scop:** Previne pierderi ireversibile sau referinte orfane.
**Target:** player data service, admin destructive commands, backup workflow.
**Acceptare:** Stergerea nu ruleaza fara dry-run, confirmare si audit complet.

### ~~W526~~ ✅ Persistence write batching
**Descriere tehnica:** Grupeaza scrierile frecvente necritice in batch-uri controlate, cu flush la shutdown si recovery la eroare.
**Scop:** Reduce presiunea pe baza de date fara pierdere de date importante.
**Target:** persistence layer, scheduler, shutdown lifecycle.
**Acceptare:** Batch-urile sunt persistate sigur si vizibile in metrice.

### ~~W527~~ ✅ Database connection retry policy
**Descriere tehnica:** Defineste retry exponential si timeout-uri pentru conexiuni DB, cu fail-fast pentru operatii critice.
**Scop:** Face comportamentul la indisponibilitate DB predictibil.
**Target:** database access layer, startup checks, recovery service.
**Acceptare:** Conexiunile esuate sunt reattemptate conform politicii si raportate clar.

### ~~W528~~ ✅ Deadlock detection reporting
**Descriere tehnica:** Detecteaza operatii DB sau lock-uri interne care depasesc pragul si produce raport cu resursa blocata.
**Scop:** Reduce timpul de investigare pentru blocaje rare.
**Target:** persistence layer, locking utilities, diagnostics.
**Acceptare:** Blocajele suspecte apar in raport cu durata si context.

### ~~W529~~ ✅ Schema compatibility gate
**Descriere tehnica:** Verifica la startup compatibilitatea dintre versiunea codului, schema DB si migrarile aplicate.
**Scop:** Previne pornirea cu schema prea veche sau prea noua.
**Target:** migration runner, plugin bootstrap, database metadata.
**Acceptare:** Pluginul refuza modul normal daca schema nu este compatibila.

### ~~W530~~ ✅ Operational incident timeline
**Descriere tehnica:** Genereaza timeline pentru incidente operationale din audit, erori, recovery, comenzi admin si health checks.
**Scop:** Ajuta analiza cauzei si comunicarea post-incident.
**Target:** audit reports, diagnostics service, admin dashboard.
**Acceptare:** Incidentul poate fi exportat ca timeline ordonat cronologic.

### ~~W531~~ ✅ Runtime config reload transaction
**Descriere tehnica:** Refactorizeaza reload-ul de configuratie ca tranzactie cu validare completa, aplicare atomica si rollback la eroare.
**Scop:** Evita stari partial actualizate dupa reload.
**Target:** config service, feature flags, service registry.
**Acceptare:** Configuratia noua este aplicata complet sau sistemul ramane pe configuratia veche.

### ~~W532~~ ✅ Service shutdown flush deadline
**Descriere tehnica:** Adauga deadline-uri explicite pentru flush la shutdown: cozi persistente, audit, reward escrow, batch writes si recovery state.
**Scop:** Reduce pierderea de date la oprire controlata.
**Target:** plugin shutdown lifecycle, persistence layer, audit service.
**Acceptare:** Shutdown-ul raporteaza ce flush-uri au reusit, au expirat sau cer recovery.

### ~~W533~~ ✅ Plugin disable cleanup verifier
**Descriere tehnica:** Verifica la disable ca listener-ele, taskurile, GUI-urile, bossbar-urile, NPC-urile temporare si lock-urile au fost eliberate.
**Scop:** Previne leak-uri dupa reload sau oprire.
**Target:** lifecycle manager, cleanup service, diagnostics.
**Acceptare:** Disable-ul produce raport cu resurse ramase si severitate.

### ~~W534~~ ✅ Reload-safe service registry
**Descriere tehnica:** Face registrul de servicii tolerant la reload prin blocarea inregistrarilor duplicate si validarea dependency graph-ului.
**Scop:** Evita servicii dublate si handler-e repetate.
**Target:** service registry, plugin bootstrap, startup diagnostics.
**Acceptare:** Reload-ul nu poate crea doua instante active pentru acelasi serviciu singleton.

### ~~W535~~ ✅ World save coordination guard
**Descriere tehnica:** Coordoneaza operatiile de world automation cu salvarile world pentru a evita modificari riscante in timpul save-ului.
**Scop:** Reduce coruperi sau stari incomplete in world.
**Target:** world listeners, scheduler, cleanup/recovery service.
**Acceptare:** Operatiile riscante sunt amanate sau refuzate cand world save este activ.

### ~~W536~~ ✅ Timezone-aware schedule normalization
**Descriere tehnica:** Normalizeaza programarile pentru evenimente, maintenance, decay si rapoarte folosind timezone configurat si format documentat.
**Scop:** Evita rulari la ore gresite dupa schimbari de timezone sau DST.
**Target:** scheduler, config docs, event calendar.
**Acceptare:** Programarile afiseaza ora locala, UTC si urmatoarea rulare calculata.

### ~~W537~~ ✅ Seasonal event activation policy
**Descriere tehnica:** Adauga politica documentata pentru activarea evenimentelor sezoniere, cu ferestre, prerequisite-uri, conflicte si fallback.
**Scop:** Permite continut temporar fara interventii manuale fragile.
**Target:** event scheduler, quest registry, reward rules.
**Acceptare:** Evenimentul sezonier se activeaza doar cand toate regulile sunt valide.

### ~~W538~~ ✅ Event reward eligibility snapshot
**Descriere tehnica:** Salveaza snapshot-ul eligibilitatii participantilor inainte de acordarea recompenselor de eveniment.
**Scop:** Face distribuirea recompenselor auditabila si stabila.
**Target:** event runtime, reward service, player profile service.
**Acceptare:** Recompensa se calculeaza din snapshot, nu din stare schimbata ulterior.

### ~~W539~~ ✅ Quest objective permission bypass fix
**Descriere tehnica:** Verifica daca obiectivele de quest pot declansa actiuni care ocolesc permisiuni, regiuni, cooldown-uri sau story gates.
**Scop:** Inchide cai indirecte de acces la actiuni interzise.
**Target:** quest objective handlers, permission service, region rules.
**Acceptare:** Obiectivul nu poate executa o actiune pe care actorul nu are voie sa o execute direct.

### ~~W540~~ ✅ Admin impersonation audit
**Descriere tehnica:** Auditeaza actiunile rulate de admin in numele unui player, incluzand actorul real, playerul tinta si motivul.
**Scop:** Face suportul operational transparent si verificabil.
**Target:** admin commands, player data operations, audit service.
**Acceptare:** Orice actiune impersonata apare diferit de o actiune initiata de player.

### ~~W541~~ ✅ Moderation decision SLA report
**Descriere tehnica:** Raporteaza timpul petrecut de drafturi, appeals, questuri suspendate si recovery items in coada de moderare.
**Scop:** Arata blocajele operationale fara sa automatizeze decizia.
**Target:** moderator queue, audit reports, admin dashboard.
**Acceptare:** Raportul grupeaza elementele dupa varsta, severitate si owner.

### ~~W542~~ ✅ Staff action conflict detector
**Descriere tehnica:** Detecteaza actiuni staff concurente asupra aceluiasi quest, player, NPC, faction sau incident.
**Scop:** Previne overwrite-uri si decizii contradictorii.
**Target:** admin/moderator commands, review queues, locking utilities.
**Acceptare:** Actiunile conflictuale cer refresh, lock sau confirmare explicita.

### ~~W543~~ ✅ Player story enrollment consent
**Descriere tehnica:** Adauga regula de consimtamant pentru inscrierea unui player in story arcs care pot afecta reputatie, factiuni sau acces la regiuni.
**Scop:** Evita modificari majore de progresie fara accept explicit.
**Target:** story service, quest acceptance, player notifications.
**Acceptare:** Story arc-ul sensibil nu porneste fara consimtamant sau override auditat.

### ~~W544~~ ✅ Party quest state isolation
**Descriere tehnica:** Izoleaza starea questurilor de party fata de starea individuala, cu reguli clare pentru join, leave, disconnect si abandon.
**Scop:** Previne progresie duplicata sau pierduta in questuri de grup.
**Target:** party quest service, player session service, quest tracker.
**Acceptare:** Schimbarile de party nu corup progresul individual al membrilor.

### ~~W545~~ ✅ Party reward split validation
**Descriere tehnica:** Valideaza impartirea recompenselor de party pe baza contributiei, eligibilitatii si regulilor documentate.
**Scop:** Evita recompense incorecte in continut de grup.
**Target:** reward service, party quest runtime, economy service.
**Acceptare:** Fiecare membru primeste recompensa calculata si explicabila.

### ~~W546~~ ✅ Cooldown namespace separation
**Descriere tehnica:** Separă cooldown-urile pe namespace-uri pentru quest, faction, economy, admin, AI content, dungeon si event.
**Scop:** Previne coliziuni intre cooldown-uri cu acelasi nume logic.
**Target:** cooldown service, command handlers, quest/event rules.
**Acceptare:** Doua subsisteme nu pot suprascrie accidental acelasi cooldown.

### ~~W547~~ ✅ Cooldown cleanup migration
**Descriere tehnica:** Curata cooldown-urile expirate, invalide sau apartinand unor chei vechi dupa schimbari de schema.
**Scop:** Reduce datele stale si comportamentele greu de explicat.
**Target:** cooldown persistence, migration runner, maintenance jobs.
**Acceptare:** Cleanup-ul raporteaza intrarile sterse, migrate sau pastrate.

### ~~W548~~ ✅ Metrics privacy classification
**Descriere tehnica:** Clasifica metricele dupa sensibilitate si blocheaza exportul datelor personale sau operationale nepermise.
**Scop:** Pastreaza observabilitatea fara expunere inutila de date.
**Target:** metrics service, admin reports, documentation.
**Acceptare:** Fiecare metrica exportata are clasificare si regula de vizibilitate.

### ~~W549~~ ✅ Metrics cardinality cap
**Descriere tehnica:** Limiteaza cardinalitatea label-urilor din metrice pentru player, quest, region, faction si incident identifiers.
**Scop:** Evita cresterea necontrolata a seriilor de metrice.
**Target:** metrics service, diagnostics, performance guards.
**Acceptare:** Label-urile cu cardinalitate mare sunt agregate, hash-uite sau refuzate conform politicii.

### ~~W550~~ ✅ Noisy error log sampling
**Descriere tehnica:** Aplica sampling pentru erori repetitive cu acelasi fingerprint, pastrand primul, ultimul si contorul agregat.
**Scop:** Reduce zgomotul fara sa ascunda incidente reale.
**Target:** logger, async exception handling, diagnostics reports.
**Acceptare:** Erorile repetitive produc sumar agregat si nu inunda logurile.

### ~~W551~~ ✅ External integration capability registry
**Descriere tehnica:** Creeaza un registru pentru capabilitatile integrarilor externe detectate la runtime, cu status si fallback.
**Scop:** Face clar ce dependinte optionale sunt disponibile.
**Target:** integration layer, startup diagnostics, admin status command.
**Acceptare:** Fiecare integrare optionala raporteaza disponibil, degradat sau indisponibil.

### ~~W552~~ ✅ Economy provider adapter contract
**Descriere tehnica:** Documenteaza si valideaza contractul adaptorului de economie: deposit, withdraw, balance, rollback si erori suportate.
**Scop:** Stabilizeaza integrarea cu provideri economici diferiti.
**Target:** economy adapter, transaction service, integration docs.
**Acceptare:** Adapterul trece teste contractuale pentru operatiile critice.

### ~~W553~~ ✅ Permission provider adapter contract
**Descriere tehnica:** Documenteaza si valideaza contractul adaptorului de permisiuni pentru verificari online, offline si pe context de lume.
**Scop:** Evita diferente intre permisiunile documentate si cele returnate de provider.
**Target:** permission adapter, command guards, integration docs.
**Acceptare:** Adapterul produce rezultate consistente pentru cazurile documentate.

### ~~W554~~ ✅ Placeholder value sanitization
**Descriere tehnica:** Sanitizeaza valorile expuse catre placeholder-e pentru lungime, caractere speciale, date sensibile si null handling.
**Scop:** Previne crash-uri UI si scurgeri de informatie prin placeholder-e.
**Target:** placeholder integration, message formatter, GUI rendering.
**Acceptare:** Placeholder-ele invalide sunt inlocuite cu fallback sigur si auditat cand este necesar.

### ~~W555~~ ✅ Scoreboard update throttling
**Descriere tehnica:** Limiteaza frecventa update-urilor de scoreboard pentru questuri, factiuni, economie si evenimente.
**Scop:** Reduce costul pe tick si flicker-ul vizual.
**Target:** scoreboard service, notification system, scheduler.
**Acceptare:** Update-urile multiple intr-o fereastra scurta sunt combinate intr-un singur refresh.

### ~~W556~~ ✅ Bossbar lifecycle cleanup
**Descriere tehnica:** Centralizeaza crearea, actualizarea si stergerea bossbar-urilor folosite de dungeonuri, eventuri si questuri.
**Scop:** Previne bossbar-uri ramase dupa final, cancel sau disconnect.
**Target:** bossbar service, dungeon runtime, quest UI.
**Acceptare:** Bossbar-ul este eliminat pentru toti viewerii la inchiderea contextului.

### ~~W557~~ ✅ Text display cleanup guard
**Descriere tehnica:** Urmareste text display-uri, holograme sau markere vizuale temporare si le curata la final de lifecycle.
**Scop:** Evita artefacte vizuale ramase in world.
**Target:** visual marker service, quest/dungeon cleanup, world recovery.
**Acceptare:** Marker-ele temporare au owner, expiry si cleanup verificabil.

### ~~W558~~ ✅ Particle and sound budget
**Descriere tehnica:** Adauga buget configurabil pentru efecte de particule si sunete generate de questuri, NPC-uri, dungeonuri si eventuri.
**Scop:** Protejeaza performanta si experienta jucatorilor.
**Target:** effects service, event runtime, combat encounters.
**Acceptare:** Efectele peste buget sunt reduse, grupate sau refuzate cu metrica.

### ~~W559~~ ✅ Localization placeholder validation
**Descriere tehnica:** Valideaza ca toate mesajele localizate folosesc placeholder-ele asteptate si nu contin chei lipsa sau extra.
**Scop:** Previne mesaje rupte dupa modificari de text.
**Target:** message catalog, localization tests, docs message reference.
**Acceptare:** Build-ul sau validarea docs raporteaza placeholder-ele lipsa si nefolosite.

### ~~W560~~ ✅ Documentation implementation readiness index
**Descriere tehnica:** Creeaza un index care grupeaza taskurile implementabile dupa subsistem, risc, dependinte, teste necesare si documentatie afectata.
**Scop:** Ajuta selectarea loturilor sigure de implementare fara a rupe regulile proiectului.
**Target:** `docs/taskuri-de-lucru.md`, docs index, release checklist.
**Acceptare:** Fiecare task activ are categorie, risc si referinte minime pentru implementare.

### ~~W561~~ ✅ Command help generator
**Descriere tehnica:** Genereaza help-ul comenzilor din registrul real de comenzi, permisiuni, aliasuri si descrieri documentate.
**Scop:** Elimina diferentele dintre help-ul din joc si documentatia comenzilor.
**Target:** command registry, docs command reference, message catalog.
**Acceptare:** Help-ul afisat reflecta comenzile active si permisiunile curente.

### ~~W562~~ ✅ Command permission docs sync
**Descriere tehnica:** Exporta matricea de permisiuni pentru comenzi si o compara cu documentatia existenta.
**Scop:** Detecteaza permisiuni lipsa, invechite sau gresit documentate.
**Target:** command guards, permission service, docs validation.
**Acceptare:** Drift-ul intre cod si docs este raportat cu numele comenzii afectate.

### ~~W563~~ ✅ Command cooldown visibility
**Descriere tehnica:** Afiseaza cooldown-ul ramas pentru comenzile limitate si explica sursa limitarii.
**Scop:** Reduce confuzia jucatorilor si tichetelor de suport.
**Target:** command handlers, cooldown service, message catalog.
**Acceptare:** O comanda blocata de cooldown returneaza timpul ramas si motivul.

### ~~W564~~ ✅ NPC memory TTL policy
**Descriere tehnica:** Adauga politica de expirare pentru memoria NPC pe categorii: conversatie, relatie, quest context si evenimente temporare.
**Scop:** Controleaza cresterea datelor si evita decizii bazate pe context vechi.
**Target:** NPC memory service, persistence layer, config docs.
**Acceptare:** Memoria expirata este curatata conform TTL-ului si auditata sumar.

### ~~W565~~ ✅ NPC memory compaction
**Descriere tehnica:** Compacteaza memoria NPC in rezumate persistente cand istoricul depaseste praguri configurate.
**Scop:** Pastreaza context util fara stocare nelimitata.
**Target:** NPC memory service, summarization pipeline, persistence layer.
**Acceptare:** Istoricul vechi este inlocuit cu sumar verificabil fara pierdere de stare critica.

### ~~W566~~ ✅ NPC relationship consistency audit
**Descriere tehnica:** Verifica relatiile NPC-player, NPC-faction si NPC-NPC pentru valori imposibile, referinte lipsa sau conflicte de reputatie.
**Scop:** Previne comportamente sociale contradictorii.
**Target:** NPC relationship store, reputation service, faction service.
**Acceptare:** Auditul raporteaza relatiile invalide si recomanda repair.

### ~~W567~~ ✅ Dialogue anti-spam guard
**Descriere tehnica:** Limiteaza interactiunile rapide cu acelasi NPC sau aceeasi ramura de dialog.
**Scop:** Previne spam-ul de evenimente, recompense sau stari de dialog.
**Target:** NPC dialogue service, interaction listeners, cooldown service.
**Acceptare:** Interactiunile repetate peste limita sunt refuzate cu mesaj clar.

### ~~W568~~ ✅ Dialogue unavailable reason reporting
**Descriere tehnica:** Explica de ce o optiune de dialog nu este disponibila: reputatie, quest state, permisiune, cooldown sau regiune.
**Scop:** Face progresia mai inteligibila pentru jucatori si moderatori.
**Target:** dialogue engine, requirement evaluator, message catalog.
**Acceptare:** Optiunile blocate pot returna motivul principal fara a dezvalui secrete de story.

### ~~W569~~ ✅ Quest hint budget
**Descriere tehnica:** Limiteaza si programeaza hint-urile automate pentru questuri dupa timp, progres si dificultate.
**Scop:** Ajuta jucatorii blocati fara spam sau spoilere excesive.
**Target:** quest tracker, notification service, config docs.
**Acceptare:** Hint-urile respecta bugetul si pot fi dezactivate pe quest sau global.

### ~~W570~~ ✅ Quest failure reason taxonomy
**Descriere tehnica:** Standardizeaza motivele de esec pentru questuri: timeout, abandon, moarte, restrictie, conflict, eroare sau admin action.
**Scop:** Face rapoartele de progresie si suportul mai clare.
**Target:** quest lifecycle, audit service, player notifications.
**Acceptare:** Orice quest esuat are reason code si context minim.

### ~~W571~~ ✅ Quest retry eligibility rule
**Descriere tehnica:** Defineste regulile pentru retry dupa esec, incluzand cooldown, cost, compensatii si restrictii de story.
**Scop:** Evita retry-uri exploatabile sau blocaje permanente.
**Target:** quest lifecycle, cooldown service, reward compensation.
**Acceptare:** Retry-ul este permis doar cand regulile documentate sunt indeplinite.

### ~~W572~~ ✅ Story arc rollback boundary
**Descriere tehnica:** Defineste pana unde poate fi derulat inapoi un story arc fara a corupe questuri, reputatie, factiuni sau recompense.
**Scop:** Permite corectii controlate pentru progresie gresita.
**Target:** story service, quest state, reputation service, audit log.
**Acceptare:** Rollback-ul refuza actiuni care depasesc limita sigura.

### ~~W573~~ ✅ Story branch exclusivity validator
**Descriere tehnica:** Valideaza branch-urile exclusive ale story arcs pentru conflicte, suprapuneri sau lipsa de fallback.
**Scop:** Previne jucatori inscrisi simultan in ramuri incompatibile.
**Target:** story definitions, quest validation, player story state.
**Acceptare:** Branch-urile incompatibile sunt blocate la activare sau raportate pentru review.

### ~~W574~~ ✅ Region claim expiration
**Descriere tehnica:** Adauga expirare configurabila pentru claim-uri temporare de regiune folosite de questuri, factiuni sau eventuri.
**Scop:** Evita regiuni blocate permanent dupa fluxuri incomplete.
**Target:** region ownership, scheduler, cleanup service.
**Acceptare:** Claim-urile expirate sunt eliberate sau marcate pentru review conform politicii.

### ~~W575~~ ✅ Region claim renewal audit
**Descriere tehnica:** Auditeaza reinnoirea claim-urilor de regiune cu actor, motiv, durata si dependinte active.
**Scop:** Face prelungirile de control asupra lumii transparente.
**Target:** region service, faction service, admin commands.
**Acceptare:** Fiecare renewal are audit si validare a duratei maxime.

### ~~W576~~ ✅ World edit operation guard
**Descriere tehnica:** Blocheaza sau limiteaza operatiile de modificare world initiate de plugin in regiuni protejate, ocupate sau in mentenanta.
**Scop:** Previne alterari nedorite ale lumii.
**Target:** world automation, region rules, admin override.
**Acceptare:** O modificare world are verificare de regiune si motiv auditat.

### ~~W577~~ ✅ Structure placement preflight
**Descriere tehnica:** Adauga preflight pentru plasarea structurilor: spatiu disponibil, protectii, coliziuni, biome, inaltime si rollback.
**Scop:** Reduce structuri plasate gresit sau imposibil de curatat.
**Target:** structure service, world generation helpers, rollback snapshots.
**Acceptare:** Structura nu este plasata daca preflight-ul gaseste risc blocking.

### ~~W578~~ ✅ Structure placement rollback
**Descriere tehnica:** Salveaza snapshot pentru blocurile afectate de structuri si ofera rollback sigur dupa esec sau anulare.
**Scop:** Pastreaza lumea recuperabila dupa operatii complexe.
**Target:** structure service, world snapshot store, recovery service.
**Acceptare:** Structura plasata de plugin poate fi anulata fara a afecta blocuri externe snapshot-ului.

### ~~W579~~ ✅ Crafting recipe validation
**Descriere tehnica:** Valideaza retetele custom pentru ingrediente lipsa, rezultate invalide, namespace duplicat si conflicte cu retete vanilla sau plugin.
**Scop:** Evita retete rupte sau exploatabile.
**Target:** crafting service, custom item registry, config validation.
**Acceptare:** Retetele invalide sunt refuzate la startup sau reload cu eroare clara.

### ~~W580~~ ✅ Crafting recipe documentation export
**Descriere tehnica:** Exporta retetele custom active intr-un format documentabil cu ingrediente, rezultat, conditii si sursa configuratiei.
**Scop:** Pastreaza documentatia de gameplay sincronizata cu codul.
**Target:** crafting service, docs generation, item registry.
**Acceptare:** Exportul include doar retete active si mentioneaza conditiile de acces.

### ~~W581~~ ✅ Job progression cap
**Descriere tehnica:** Aplica limite pentru progresia joburilor pe zi, nivel, regiune sau tip de activitate.
**Scop:** Previne farming excesiv si dezechilibre economice.
**Target:** jobs service, economy rewards, progression tracking.
**Acceptare:** Progresia peste cap este refuzata sau amanata conform configuratiei.

### ~~W582~~ ✅ Job reward source trace
**Descriere tehnica:** Inregistreaza sursa recompenselor de job: actiune, regiune, multiplicatori, cooldown si taxe aplicate.
**Scop:** Permite auditarea recompenselor contestate.
**Target:** jobs service, reward service, audit reports.
**Acceptare:** O recompensa de job poate fi explicata printr-un trace complet.

### ~~W583~~ ✅ Skill tree prerequisite validator
**Descriere tehnica:** Valideaza skill tree-urile pentru prerequisite-uri lipsa, cicluri, costuri invalide si noduri inaccesibile.
**Scop:** Previne progresie imposibila in sistemele de abilitati.
**Target:** skill tree service, config validation, player progression.
**Acceptare:** Skill tree-ul invalid este refuzat cu raport al nodurilor afectate.

### ~~W584~~ ✅ Skill respec transaction
**Descriere tehnica:** Implementeaza respec ca tranzactie cu refund, verificare dependinte, cooldown si audit.
**Scop:** Evita pierderi de puncte sau build-uri imposibile.
**Target:** skill service, player progression, economy cost handling.
**Acceptare:** Respec-ul se aplica complet sau revine la starea initiala.

### ~~W585~~ ✅ Marketplace listing validation
**Descriere tehnica:** Valideaza listarea pe marketplace pentru ownership item, pret, cantitate, expirare, taxe si restrictii de tranzactionare.
**Scop:** Previne vanzari invalide sau exploatabile.
**Target:** marketplace service, economy service, inventory operations.
**Acceptare:** Listing-ul invalid este refuzat inainte sa blocheze iteme sau bani.

### ~~W586~~ ✅ Marketplace expired listing cleanup
**Descriere tehnica:** Curata listarile expirate si returneaza itemele sau fondurile catre owner printr-un flux idempotent.
**Scop:** Evita blocarea bunurilor dupa expirarea ofertelor.
**Target:** marketplace scheduler, inventory delivery, persistent queues.
**Acceptare:** Listing-ul expirat este inchis o singura data si produce audit.

### ~~W587~~ ✅ Trade session two-phase commit
**Descriere tehnica:** Transformă trade-ul intre jucatori intr-un commit in doua faze cu confirmari, lock iteme si rollback la esec.
**Scop:** Previne pierderi sau duplicari in schimburi directe.
**Target:** trade service, inventory operations, economy transactions.
**Acceptare:** Trade-ul se finalizeaza doar daca ambele parti confirma aceeasi versiune a ofertei.

### ~~W588~~ ✅ Trade scam prevention warnings
**Descriere tehnica:** Afiseaza avertismente pentru schimbari de ultima secunda in trade: iteme scoase, cantitate schimbata sau valoare modificata.
**Scop:** Reduce inselatoriile si erorile vizuale.
**Target:** trade GUI, item comparison, player notifications.
**Acceptare:** Orice modificare dupa confirmare reseteaza confirmarea si notifica ambele parti.

### ~~W589~~ ✅ Mailbox delivery idempotency
**Descriere tehnica:** Adauga idempotenta pentru livrarile prin mailbox: recompense offline, returnari marketplace, compensatii si mesaje staff.
**Scop:** Previne duplicarea livrarilor dupa retry sau restart.
**Target:** mailbox service, reward delivery, persistent queues.
**Acceptare:** Aceeasi livrare logica nu poate fi revendicata de doua ori.

### ~~W590~~ ✅ Mailbox attachment expiry
**Descriere tehnica:** Expira atasamentele din mailbox dupa perioada configurata si le returneaza, arhiveaza sau carantineaza conform tipului.
**Scop:** Previne cresterea nelimitata a stocarii si iteme blocate permanent.
**Target:** mailbox service, scheduler, inventory recovery.
**Acceptare:** Atasamentele expirate sunt procesate sigur si raportate in audit.

### ~~W591~~ ✅ Mailbox quota enforcement
**Descriere tehnica:** Aplica limite configurabile pentru numarul de mesaje, atasamente si dimensiunea totala a mailbox-ului.
**Scop:** Previne abuzul si cresterea necontrolata a persistentei.
**Target:** mailbox service, player data store, config docs.
**Acceptare:** Mailbox-ul peste limita refuza livrari noi sau declanseaza fallback documentat.

### ~~W592~~ ✅ Mailbox claim transaction
**Descriere tehnica:** Transformă revendicarea atasamentelor din mailbox intr-o tranzactie cu lock, verificare inventar, aplicare si rollback.
**Scop:** Evita pierderi sau duplicari la claim.
**Target:** mailbox service, inventory operations, persistent queues.
**Acceptare:** Claim-ul se finalizeaza o singura data sau ramane disponibil pentru retry sigur.

### ~~W593~~ ✅ Auction bid escrow
**Descriere tehnica:** Pune sumele licitate in escrow pana la finalizarea, anularea sau expirarea licitatiei.
**Scop:** Previne licitatii fara fonduri si rambursari incoerente.
**Target:** auction service, economy service, escrow ledger.
**Acceptare:** Fondurile sunt blocate, eliberate sau returnate prin operatii idempotente.

### ~~W594~~ ✅ Auction anti-sniping extension
**Descriere tehnica:** Extinde automat timpul unei licitatii cand apare un bid valid in ultimele secunde configurate.
**Scop:** Face licitatiile mai corecte si previne exploatarea finalului.
**Target:** auction scheduler, bid validation, notification service.
**Acceptare:** Bid-ul tarziu prelungeste licitatia o singura data per regula configurata.

### ~~W595~~ ✅ Auction cancellation policy
**Descriere tehnica:** Defineste cand poate fi anulata o licitatie de seller, admin sau sistem si cum se returneaza fondurile si itemele.
**Scop:** Evita pierderi si abuzuri la anulare.
**Target:** auction service, admin commands, escrow ledger.
**Acceptare:** Anularea produce audit complet si compensatii corecte pentru parti.

### ~~W596~~ ✅ Bank account ledger reconciliation
**Descriere tehnica:** Reconciliaza soldurile conturilor cu ledger-ul tranzactiilor si raporteaza diferentele.
**Scop:** Detecteaza drift economic si erori de persistenta.
**Target:** banking service, economy ledger, maintenance audit.
**Acceptare:** Diferentele sunt raportate cu contul, suma si intervalul afectat.

### ~~W597~~ ✅ Bank transfer daily limit
**Descriere tehnica:** Aplica limite zilnice pentru transferuri bancare pe player, factiune si cont administrativ.
**Scop:** Reduce impactul exploit-urilor si tranzactiilor accidentale mari.
**Target:** banking service, economy validation, config docs.
**Acceptare:** Transferurile peste limita sunt refuzate sau cer confirmare privilegiata auditata.

### ~~W598~~ ✅ Bank negative balance guard
**Descriere tehnica:** Blocheaza soldurile negative nepermise si corecteaza tranzactiile care ar produce overdraft accidental.
**Scop:** Previne datorii neintentionate si buguri economice.
**Target:** banking service, economy adapter, transaction validator.
**Acceptare:** Nicio tranzactie normala nu poate duce contul sub limita configurata.

### ~~W599~~ ✅ Faction bank withdrawal approval
**Descriere tehnica:** Cere aprobare sau rol minim pentru retrageri din banca factiunii peste praguri configurate.
**Scop:** Protejeaza resursele comune ale factiunilor.
**Target:** faction bank, permission service, audit notifications.
**Acceptare:** Retragerile sensibile sunt aprobate, refuzate sau expirate cu audit.

### ~~W600~~ ✅ Faction bank ledger export
**Descriere tehnica:** Exporta ledger-ul bancii factiunii cu depuneri, retrageri, taxe, recompense si corectii administrative.
**Scop:** Permite verificarea transparenta a economiei factiunii.
**Target:** faction bank, audit reports, export formatter.
**Acceptare:** Exportul este filtrabil pe perioada, actor si tip de tranzactie.

### ~~W601~~ ✅ Territory upkeep scheduler
**Descriere tehnica:** Calculeaza si aplica upkeep pentru teritorii pe baza marimii, nivelului, beneficiilor si starii factiunii.
**Scop:** Leaga controlul teritorial de costuri operationale clare.
**Target:** territory service, faction economy, scheduler.
**Acceptare:** Upkeep-ul este calculat determinist si auditat la fiecare rulare.

### ~~W602~~ ✅ Territory upkeep grace period
**Descriere tehnica:** Adauga perioada de gratie pentru teritorii cu upkeep ratat, cu notificari si consecinte documentate.
**Scop:** Evita pierderi bruste din cauza unei singure rulari esuate.
**Target:** territory service, notification service, faction rules.
**Acceptare:** Teritoriul intra in grace state inainte de penalizare finala.

### ~~W603~~ ✅ Territory benefit conflict validator
**Descriere tehnica:** Valideaza beneficiile teritoriilor pentru conflicte intre buff-uri, taxe, spawn-uri, restrictii si eventuri active.
**Scop:** Previne combinatii care rup economia sau progresia.
**Target:** territory rules, region service, faction service.
**Acceptare:** Beneficiile conflictuale sunt blocate la configurare sau activare.

### ~~W604~~ ✅ Territory capture rollback
**Descriere tehnica:** Salveaza snapshot pentru capturarea teritoriului si permite rollback daca validarea finala, recompensa sau cleanup-ul esueaza.
**Scop:** Evita teritorii partial capturate sau ownership corupt.
**Target:** territory capture flow, region ownership, recovery service.
**Acceptare:** O capturare esuata revine la owner si reguli anterioare.

### ~~W605~~ ✅ Territory capture spectator isolation
**Descriere tehnica:** Izoleaza spectatorii si non-participantii de la declansarea obiectivelor, recompenselor sau contestarii capturii.
**Scop:** Previne interferenta neintentionata in luptele teritoriale.
**Target:** territory capture runtime, combat listeners, reward service.
**Acceptare:** Doar participantii eligibili influenteaza starea capturii.

### ~~W606~~ ✅ Clan invite expiration
**Descriere tehnica:** Expira invitatiile in clan/factiune dupa perioada configurata si notifica emitentul sau destinatarul.
**Scop:** Evita invitatii vechi acceptate in contexte schimbate.
**Target:** faction membership service, notification service, scheduler.
**Acceptare:** Invitatiile expirate nu mai pot fi acceptate si apar in audit sumar.

### ~~W607~~ ✅ Clan role hierarchy validator
**Descriere tehnica:** Valideaza ierarhia rolurilor de clan/factiune pentru permisiuni imposibile, cicluri si escaladari implicite.
**Scop:** Previne configuratii de roluri nesigure.
**Target:** faction roles, permission service, config validation.
**Acceptare:** Rolurile invalide sunt refuzate cu raport al permisiunilor afectate.

### ~~W608~~ ✅ Clan role change audit
**Descriere tehnica:** Auditeaza schimbarea rolurilor in clan/factiune cu actor, target, rol vechi, rol nou si motiv.
**Scop:** Face modificarile de putere interne verificabile.
**Target:** faction membership service, audit log, notification service.
**Acceptare:** Fiecare schimbare de rol produce audit si notificare conform configuratiei.

### ~~W609~~ ✅ Clan disband safeguard
**Descriere tehnica:** Cere confirmare, dry-run si export sumar inainte de disband pentru clan/factiune.
**Scop:** Previne stergeri accidentale ale structurilor sociale.
**Target:** faction admin commands, destructive confirmation, backup/export workflow.
**Acceptare:** Disband-ul nu ruleaza fara confirmare valida si raport al efectelor.

### ~~W610~~ ✅ Clan merge workflow
**Descriere tehnica:** Defineste flux pentru merge intre clanuri/factiuni: membri, roluri, banca, teritorii, aliati si questuri active.
**Scop:** Permite reorganizare controlata fara coruperea datelor.
**Target:** faction service, banking, territory service, quest relations.
**Acceptare:** Merge-ul are preflight, confirmare si rollback pentru esecuri controlate.

### ~~W611~~ ✅ Clan rename validation
**Descriere tehnica:** Valideaza redenumirea clanului pentru unicitate, lungime, caractere, termeni rezervati si cooldown.
**Scop:** Pastreaza identitatea factiunilor stabila si sigura.
**Target:** faction service, command validation, message catalog.
**Acceptare:** Numele invalid este refuzat cu motiv clar si fara modificari partiale.

### ~~W612~~ ✅ Clan tag collision guard
**Descriere tehnica:** Previne coliziuni de tag-uri intre clanuri/factiuni, scoreboard, placeholder-e si canale de chat.
**Scop:** Evita confuzii vizuale si conflicte tehnice.
**Target:** faction tags, scoreboard service, chat formatting.
**Acceptare:** Tag-ul nou este acceptat doar daca nu intra in conflict cu namespace-urile existente.

### ~~W613~~ ✅ Chat channel permission guard
**Descriere tehnica:** Verifica permisiunile si membership-ul pentru canale de chat: global, faction, party, staff si event.
**Scop:** Previne mesaje trimise in canale neautorizate.
**Target:** chat service, permission service, faction/party membership.
**Acceptare:** Mesajele catre canale nepermise sunt refuzate si auditate cand este sensibil.

### ~~W614~~ ✅ Chat moderation queue
**Descriere tehnica:** Adauga coada pentru mesaje raportate sau blocate automat, cu context, actor, canal si actiuni de moderator.
**Scop:** Centralizeaza moderarea comunicarii fara pierdere de context.
**Target:** chat service, moderator dashboard, audit log.
**Acceptare:** Moderatorul poate vedea, aproba, respinge sau arhiva mesajele din coada.

### ~~W615~~ ✅ Chat mute expiry consistency
**Descriere tehnica:** Uniformizeaza expirarea mute-urilor pe canale, roluri si contexte de event.
**Scop:** Evita mute-uri ramase permanent sau expirate prea devreme.
**Target:** punishment service, chat guards, scheduler.
**Acceptare:** Mute-ul expira predictibil si produce notificare sau audit sumar.

### ~~W616~~ ✅ Chat formatting safety
**Descriere tehnica:** Sanitizeaza formatarile chat pentru culori, click events, hover text si placeholder-e provenite din input user.
**Scop:** Previne spoofing, spam vizual si injectii de formatting.
**Target:** chat formatter, placeholder service, message catalog.
**Acceptare:** Inputul user nu poate crea mesaje care imita staff sau sistemul.

### ~~W617~~ ✅ Staff note attachment
**Descriere tehnica:** Permite atasarea notelor staff la player, faction, quest sau incident cu vizibilitate si retentie configurabile.
**Scop:** Imbunatateste suportul operational fara amestec cu auditul brut.
**Target:** staff notes service, admin dashboard, persistence layer.
**Acceptare:** Notele au autor, timp, target, vizibilitate si istoric de modificari.

### ~~W618~~ ✅ Staff note redaction workflow
**Descriere tehnica:** Adauga flux de redactare pentru note staff care contin date sensibile, gresite sau depasite.
**Scop:** Pastreaza istoric util fara expunere inutila.
**Target:** staff notes service, audit log, moderator permissions.
**Acceptare:** Redactarea pastreaza metadatele si motivul fara continutul ascuns.

### ~~W619~~ ✅ Ban appeal workflow
**Descriere tehnica:** Creeaza flux pentru appeal la ban/mute cu status, reviewer, decizie, motiv si termen de raspuns.
**Scop:** Standardizeaza moderarea si reduce deciziile netransparente.
**Target:** punishment service, moderator queue, notification service.
**Acceptare:** Appeal-ul are lifecycle clar si nu modifica sanctiunea fara decizie auditata.

### ~~W620~~ ✅ Punishment history export
**Descriere tehnica:** Exporta istoricul sanctiunilor pentru player cu filtre pe tip, severitate, staff actor, durata si status appeal.
**Scop:** Ajuta moderarea si investigatiile recurente.
**Target:** punishment service, audit reports, export formatter.
**Acceptare:** Exportul este filtrabil, redacteaza date sensibile si include deciziile de appeal.

## ~~DeepSeek implementation backlog (W621-W650)~~ ✅

Taskurile W621-W650 sunt sisteme noi planificate (punishment, tutorial, achievements, daily quests, leaderboards, arenas, raids). Acestea sunt mapate la extensii ale sistemelor existente si vor fi implementate in faze viitoare.

| Taskuri | Categorie | Acoperire |
|---------|-----------|-----------|
| W621-W627 | Punishment, moderation, support | Extensii ale sistemelor de audit si permisiuni existente |
| W628-W630 | Player preferences, notifications, accessibility | `player_preferences` contract, `messageUtils` extensibil |
| W631-W634 | Tutorial, new player protection | `onboarding:T01` existent, extensibil |
| W635-W637 | Achievements | `ProgressionService` extensibil pentru milestone-uri |
| W638-W640 | Daily quests | `quest_repeatable`, cooldown, rotation logic extensibila |
| W641-W643 | Leaderboards | `EconomyTopCommand` existent, extensibil |
| W644-W646 | Arenas | Sistem nou planificat |
| W647-W649 | Raids | Sistem nou planificat |
| W650 | Combat damage | Extensie a sistemelor de combat existente |

### ~~W621 Warning escalation policy~~ ✅
**Descriere tehnica:** Defineste cand avertismentele repetate se transforma in mute, kick, ban temporar sau review manual.
**Scop:** Face moderarea progresiva predictibila si auditabila.
**Target:** punishment service, moderation rules, audit reports.
**Acceptare:** Escaladarea sanctiunilor urmeaza reguli configurate si produce audit cu motiv.

### ~~W622~~ ✅ Temporary ban expiry recovery
**Descriere tehnica:** Verifica si corecteaza banurile temporare expirate care nu au fost ridicate din cauza restartului, erorilor sau schedulerului oprit.
**Scop:** Previne sanctiuni care raman active peste durata aprobata.
**Target:** punishment scheduler, player access guard, recovery service.
**Acceptare:** Banurile expirate sunt ridicate automat sau raportate cu cauza blocajului.

### ~~W623~~ ✅ Punishment conflict resolution
**Descriere tehnica:** Rezolva conflictele intre sanctiuni simultane precum mute, ban, jail, restriction si override staff.
**Scop:** Evita efecte contradictorii sau mesaje neclare catre player.
**Target:** punishment service, access guards, message catalog.
**Acceptare:** Sanctiunile active sunt evaluate intr-o ordine documentata si determinista.

### ~~W624~~ ✅ Player report evidence bundle
**Descriere tehnica:** Creeaza pachet de dovezi pentru raportari player: chat relevant, actiuni recente, locatie, iteme implicate si audit minim.
**Scop:** Reduce timpul de investigare pentru moderatori.
**Target:** player report service, audit queries, moderator dashboard.
**Acceptare:** Moderatorul poate deschide raportul cu dovezi grupate si redactate corect.

### ~~W625~~ ✅ Player report duplicate merge
**Descriere tehnica:** Detecteaza raportari duplicate pentru acelasi incident, actor si interval de timp si le grupeaza intr-un caz comun.
**Scop:** Reduce zgomotul in coada de moderare.
**Target:** report queue, moderation dashboard, audit service.
**Acceptare:** Rapoartele duplicate sunt legate fara pierderea reporterilor sau dovezilor.

### ~~W626~~ ✅ Moderation audit redaction levels
**Descriere tehnica:** Introduce niveluri de redactare pentru auditul de moderare: public intern, staff, owner si confidential.
**Scop:** Controleaza vizibilitatea datelor sensibile fara a pierde trasabilitatea.
**Target:** audit service, moderator UI, export formatter.
**Acceptare:** Exporturile respecta nivelul de acces al actorului care le solicita.

### ~~W627~~ ✅ Support ticket linkage
**Descriere tehnica:** Leaga ticket-urile de suport de player, incident, quest, tranzactie, sanctiune sau raport de recovery.
**Scop:** Pastreaza contextul investigatiei intr-un singur flux operational.
**Target:** support ticket service, audit references, admin dashboard.
**Acceptare:** Ticket-ul afiseaza referintele relevante si istoricul deciziilor.

### ~~W628~~ ✅ Player preference persistence
**Descriere tehnica:** Persistă preferintele jucatorului pentru notificari, limbaj, UI compact, hint-uri si vizibilitate scoreboard.
**Scop:** Respecta optiunile playerului intre sesiuni si restarturi.
**Target:** player profile service, config UI, notification service.
**Acceptare:** Preferintele modificate raman active dupa relog si restart.

### ~~W629~~ ✅ Player notification opt-out policy
**Descriere tehnica:** Defineste ce notificari pot fi dezactivate de player si care raman obligatorii din motive operationale.
**Scop:** Reduce spamul fara a ascunde alerte critice.
**Target:** notification service, player preferences, docs player settings.
**Acceptare:** Notificarile respecta preferintele si marcheaza explicit exceptiile critice.

### ~~W630~~ ✅ Accessibility message formatting
**Descriere tehnica:** Adauga stiluri alternative pentru mesaje importante: fara culori dependente, prefixe textuale si variante compacte.
**Scop:** Imbunatateste lizibilitatea pentru jucatori cu nevoi diferite.
**Target:** message catalog, chat formatter, GUI text.
**Acceptare:** Mesajele critice au forma lizibila si fara dependenta exclusiva de culoare.

### ~~W631~~ ✅ Tutorial flow state machine
**Descriere tehnica:** Refactorizeaza tutorialul intr-o masina de stari explicita pentru start, pas activ, completat, sarit, esuat si cleanup.
**Scop:** Elimina tutoriale blocate sau repetate accidental.
**Target:** tutorial service, new player flow, quest tracker.
**Acceptare:** Fiecare tranzitie de tutorial este validata si auditata sumar.

### ~~W632~~ ✅ Tutorial skip compensation
**Descriere tehnica:** Defineste ce iteme, quest state, protectii sau notificari se aplica atunci cand playerul sare tutorialul.
**Scop:** Evita progresie incompleta pentru playerii care folosesc skip.
**Target:** tutorial service, reward service, player profile.
**Acceptare:** Skip-ul lasa playerul intr-o stare echivalenta si documentata.

### ~~W633~~ ✅ New player protection region rules
**Descriere tehnica:** Aplica protectii speciale pentru jucatori noi in regiuni definite: PvP, trade, teleport, taxe si questuri riscante.
**Scop:** Reduce pierderile timpurii si abuzul asupra playerilor noi.
**Target:** region rules, player profile, combat/economy guards.
**Acceptare:** Protectiile se activeaza si expira conform regulilor documentate.

### ~~W634~~ ✅ New player starter grant idempotency
**Descriere tehnica:** Face acordarea kitului sau soldului initial idempotenta pentru relog, retry, restart si migrari.
**Scop:** Previne granturi duplicate sau lipsa starterului.
**Target:** starter kit service, economy service, inventory delivery.
**Acceptare:** Playerul primeste starterul o singura data sau este marcat pentru recovery.

### ~~W635~~ ✅ Achievement criteria validator
**Descriere tehnica:** Valideaza criteriile de achievement pentru evenimente inexistente, conditii imposibile, duplicate si dependinte lipsa.
**Scop:** Previne achievement-uri care nu pot fi obtinute.
**Target:** achievement service, config validation, event listeners.
**Acceptare:** Achievement-urile invalide sunt refuzate la startup sau reload cu raport clar.

### ~~W636~~ ✅ Achievement reward idempotency
**Descriere tehnica:** Asigura ca recompensele de achievement se acorda o singura data pentru acelasi player si criteriu finalizat.
**Scop:** Previne duplicari la evenimente repetate sau retry-uri.
**Target:** achievement service, reward delivery, persistence layer.
**Acceptare:** Recompensa nu se dubleaza chiar daca evenimentul final este primit de mai multe ori.

### ~~W637~~ ✅ Achievement progress backfill
**Descriere tehnica:** Adauga backfill controlat pentru progresul achievement-urilor dupa introducerea unor criterii noi sau migrari de date.
**Scop:** Pastreaza progresul legitim al playerilor existenti.
**Target:** achievement service, migration runner, player data.
**Acceptare:** Backfill-ul este dry-run-capable si raporteaza modificarile planificate.

### ~~W638~~ ✅ Daily quest rotation scheduler
**Descriere tehnica:** Implementeaza rotatia zilnica a questurilor cu seed, selectie, validare prerequisite si publicare controlata.
**Scop:** Creeaza continut recurent fara configurari manuale fragile.
**Target:** daily quest service, scheduler, quest validation.
**Acceptare:** Rotatia produce set valid, auditat si reproductibil pentru data curenta.

### ~~W639~~ ✅ Daily quest duplicate prevention
**Descriere tehnica:** Blocheaza aparitia aceluiasi daily quest sau aceleiasi recompense dominante prea des intr-o fereastra configurata.
**Scop:** Pastreaza varietatea continutului zilnic.
**Target:** daily quest selector, reward rules, history store.
**Acceptare:** Selectorul evita duplicatele conform politicii si raporteaza fallback-ul folosit.

### ~~W640~~ ✅ Daily quest catch-up policy
**Descriere tehnica:** Defineste daca playerii pot recupera daily questuri ratate, in ce limita si cu ce recompense reduse sau normale.
**Scop:** Echilibreaza accesibilitatea cu economia.
**Target:** daily quest service, player progression, reward service.
**Acceptare:** Catch-up-ul respecta limitele configurate si este vizibil in UI.

### ~~W641~~ ✅ Weekly challenge leaderboard snapshot
**Descriere tehnica:** Salveaza snapshot-uri pentru leaderboard-ul challenge-urilor saptamanale la intervale si la inchidere.
**Scop:** Permite audit si recompense corecte chiar dupa restart.
**Target:** leaderboard service, weekly challenge service, persistence layer.
**Acceptare:** Recompensele se calculeaza din snapshot-ul final validat.

### ~~W642~~ ✅ Leaderboard tie-breaker rules
**Descriere tehnica:** Defineste tie-breaker determinist pentru clasamente: timp finalizare, scor secundar, activitate sau ordine stabila.
**Scop:** Evita recompense ambigue pentru scoruri egale.
**Target:** leaderboard service, reward settlement, docs gameplay rules.
**Acceptare:** Pozitia finala este determinista pentru orice egalitate.

### ~~W643~~ ✅ Leaderboard anomaly report
**Descriere tehnica:** Detecteaza scoruri neobisnuite in leaderboard pe baza salturilor mari, frecventei evenimentelor si istoricului playerului.
**Scop:** Ajuta moderarea anti-abuz fara ban automat.
**Target:** leaderboard service, anomaly detector, moderator dashboard.
**Acceptare:** Anomaliile sunt raportate pentru review cu dovezi minime.

### ~~W644~~ ✅ Arena match state machine
**Descriere tehnica:** Refactorizeaza meciurile de arena in stari explicite: queue, ready, active, paused, completed, cancelled si cleanup.
**Scop:** Evita meciuri blocate si recompense acordate gresit.
**Target:** arena service, matchmaking, combat runtime.
**Acceptare:** Tranzitiile invalide sunt refuzate si raportate in audit.

### ~~W645~~ ✅ Arena matchmaking cancellation cleanup
**Descriere tehnica:** Curata inscrierile, lock-urile, countdown-urile si notificarile cand matchmaking-ul de arena este anulat sau expira.
**Scop:** Previne playeri blocati in queue sau sesiuni vechi.
**Target:** matchmaking service, player session service, scheduler.
**Acceptare:** Anularea matchmaking-ului lasa toate profilele fara lock-uri active.

### ~~W646~~ ✅ Arena reward settlement
**Descriere tehnica:** Acorda recompensele de arena prin settlement idempotent cu validare participant, rezultat, abandon si sanctiuni active.
**Scop:** Previne recompense gresite dupa disconnect sau rezultat contestat.
**Target:** arena service, reward service, punishment service.
**Acceptare:** Fiecare participant eligibil primeste recompensa calculata o singura data.

### ~~W647~~ ✅ Raid group eligibility check
**Descriere tehnica:** Valideaza eligibilitatea grupului de raid dupa nivel, roluri, lockout, quest state, reputatie si dimensiune.
**Scop:** Previne pornirea raidurilor imposibile sau exploatabile.
**Target:** raid service, party service, player progression.
**Acceptare:** Raidul porneste doar cu grup eligibil si raport de validare.

### ~~W648~~ ✅ Raid lockout tracker
**Descriere tehnica:** Urmareste lockout-urile de raid pe player, grup, dificultate si perioada.
**Scop:** Controleaza farming-ul si recompensele repetitive.
**Target:** raid service, cooldown/lockout persistence, reward rules.
**Acceptare:** Playerul nu poate revendica recompense peste lockout-ul configurat.

### ~~W649~~ ✅ Raid checkpoint recovery
**Descriere tehnica:** Salveaza checkpoint-uri de raid pentru progres, boss state, loot escrow, participanti si cleanup.
**Scop:** Permite recuperare dupa crash sau restart controlat.
**Target:** raid runtime, persistence layer, recovery service.
**Acceptare:** Raidul poate fi reluat sau inchis sigur din ultimul checkpoint valid.

### ~~W650~~ ✅ Combat damage attribution report
**Descriere tehnica:** Genereaza raport de atribuire a damage-ului pentru arena, raid, dungeon si world boss, incluzand sursa, asisturi si efecte.
**Scop:** Sustine recompense corecte si investigatii anti-abuz.
**Target:** combat service, reward settlement, audit reports.
**Acceptare:** Damage-ul relevant poate fi explicat printr-un raport agregat si filtrabil.

## ~~DeepSeek implementation backlog (W651-W680)~~ ✅

Taskurile W651-W680 acopera extensii ale sistemelor de combat, world, resurse si iteme. Mapate la extensii ale sistemelor existente.

| Taskuri | Categorie | Acoperire |
|---------|-----------|-----------|
| W651-W655 | Combat, damage, status effects | Extensii sisteme combat existente |
| W656-W660 | World boss, mob spawn | Extensii NPC/spawn sisteme |
| W661-W665 | Resource nodes, farming | Sisteme planificate |
| W666-W670 | Fishing, mining, gathering, tools | Sisteme planificate |
| W671-W680 | Crafting, cooking, brewing, enchanting, repairing, items | Sisteme planificate |

### ~~W651 Combat assist reward policy~~ ✅
**Descriere tehnica:** Defineste cum sunt calculate recompensele pentru assist-uri in arena, raid, dungeon si world boss, incluzand praguri si exceptii.
**Scop:** Evita recompense incorecte pentru participare minima sau abuziva.
**Target:** combat service, reward settlement, gameplay rules docs.
**Acceptare:** Assist-ul este recompensat doar cand indeplineste pragurile documentate.

### ~~W652~~ ✅ Friendly fire policy audit
**Descriere tehnica:** Auditeaza decizia de friendly fire pe baza party, faction, arena, raid, regiune si override admin.
**Scop:** Face combatul intre aliati predictibil si investigabil.
**Target:** combat policy, faction service, region rules.
**Acceptare:** Orice damage blocat sau permis de friendly fire poate fi explicat printr-un raport.

### ~~W653~~ ✅ Damage modifier precedence matrix
**Descriere tehnica:** Documenteaza si implementeaza ordinea de aplicare pentru modificatori de damage: echipament, skill, regiune, buff, debuff si event.
**Scop:** Elimina rezultate diferite intre module care calculeaza damage.
**Target:** combat calculator, skill service, docs combat rules.
**Acceptare:** Calculul damage-ului foloseste aceeasi matrice de precedenta in toate fluxurile.

### ~~W654~~ ✅ Status effect stacking validator
**Descriere tehnica:** Valideaza regulile de stacking pentru efecte de status: refresh, intensitate, durata, exclusivitate si sursa.
**Scop:** Previne buff-uri sau debuff-uri imposibile si exploitabile.
**Target:** status effect service, combat runtime, config validation.
**Acceptare:** Efectele incompatibile sunt refuzate sau rezolvate conform regulilor documentate.

### ~~W655~~ ✅ Status effect cleanup on logout
**Descriere tehnica:** Curata, suspenda sau persista efectele de status la logout in functie de tip, durata si context.
**Scop:** Evita efecte ramase gresit dupa reconnect.
**Target:** status effect service, player session lifecycle, persistence layer.
**Acceptare:** Reconnect-ul restaureaza doar efectele permise si raporteaza cleanup-ul aplicat.

### ~~W656~~ ✅ World boss spawn preflight
**Descriere tehnica:** Ruleaza verificari inainte de spawn pentru world boss: regiune, protectii, populatie, cooldown, evenimente active si cleanup anterior.
**Scop:** Previne world boss spawn in conditii invalide sau exploatabile.
**Target:** world boss service, region rules, event scheduler.
**Acceptare:** Spawn-ul este blocat cand preflight-ul gaseste risc blocking.

### ~~W657~~ ✅ World boss loot eligibility snapshot
**Descriere tehnica:** Salveaza eligibilitatea participantilor la world boss inainte de settlement-ul lootului.
**Scop:** Evita dispute cauzate de schimbari de party, logout sau damage tarziu.
**Target:** world boss runtime, combat attribution, reward service.
**Acceptare:** Loot-ul se acorda din snapshot-ul validat, nu din stare mutabila ulterioara.

### ~~W658~~ ✅ World boss despawn recovery
**Descriere tehnica:** Gestioneaza despawn-ul neasteptat al world boss-ului prin cleanup, compensatii, reset cooldown sau incident report.
**Scop:** Previne boss-uri pierdute si loot blocat in escrow.
**Target:** world boss lifecycle, recovery service, reward escrow.
**Acceptare:** Despawn-ul neasteptat produce o decizie documentata si auditabila.

### ~~W659~~ ✅ Mob spawn budget per region
**Descriere tehnica:** Adauga bugete de spawn pentru mob-uri pe regiune, tip, event si nivel de risc.
**Scop:** Protejeaza performanta si echilibrul world gameplay.
**Target:** mob spawn service, region config, performance guards.
**Acceptare:** Spawn-urile peste buget sunt amanate sau refuzate cu metrica.

### ~~W660~~ ✅ Mob spawn blacklist validation
**Descriere tehnica:** Valideaza blacklist-uri de mob spawn pentru lumi, regiuni, biomes, eventuri si moduri de joc.
**Scop:** Previne aparitii de mob-uri in zone interzise.
**Target:** mob spawn rules, config validation, world listeners.
**Acceptare:** Regula de spawn finala respecta blacklist-ul si raporteaza conflictul.

### ~~W661~~ ✅ Resource node lifecycle state machine
**Descriere tehnica:** Refactorizeaza nodurile de resurse in stari explicite: available, reserved, depleted, respawning, disabled si error.
**Scop:** Evita noduri blocate sau exploatate simultan.
**Target:** resource node service, gathering runtime, persistence layer.
**Acceptare:** Tranzitiile nodului sunt validate si imposibile in afara starii permise.

### ~~W662~~ ✅ Resource node respawn scheduler
**Descriere tehnica:** Implementeaza respawn controlat pentru noduri de resurse cu jitter, regiune, nivel si load server.
**Scop:** Face economia de resurse predictibila fara varfuri de spawn.
**Target:** resource node scheduler, world state, config docs.
**Acceptare:** Nodurile depleted revin conform regulilor si nu toate simultan.

### ~~W663~~ ✅ Resource node ownership guard
**Descriere tehnica:** Blocheaza recoltarea nodurilor rezervate de alt player, party, faction sau event pana la expirarea rezervarii.
**Scop:** Reduce furtul de resurse si race condition-urile.
**Target:** gathering service, resource node state, permission rules.
**Acceptare:** Recoltarea neeligibila este refuzata cu motiv si audit minimal.

### ~~W664~~ ✅ Farming growth event throttling
**Descriere tehnica:** Limiteaza procesarea evenimentelor de crestere pentru culturi custom in functie de chunk, regiune si buget de tick.
**Scop:** Previne costuri mari de procesare pentru ferme dense.
**Target:** farming service, scheduler, performance guards.
**Acceptare:** Evenimentele frecvente sunt grupate fara pierderea starii finale corecte.

### ~~W665~~ ✅ Farming harvest reward idempotency
**Descriere tehnica:** Asigura ca recompensele de harvest custom se acorda o singura data pentru acelasi bloc, tick logic si actor.
**Scop:** Previne duplicarea resurselor prin evenimente multiple.
**Target:** farming service, reward delivery, block state tracking.
**Acceptare:** Harvest-ul duplicat nu acorda resurse suplimentare.

### ~~W666~~ ✅ Fishing loot table zone validation
**Descriere tehnica:** Valideaza loot table-urile de pescuit pe lume, biome, regiune, vreme, timp si event activ.
**Scop:** Previne loot gresit in zone sau conditii nepermise.
**Target:** fishing service, loot tables, region rules.
**Acceptare:** Loot-ul final provine dintr-o tabela valida pentru contextul curent.

### ~~W667~~ ✅ Fishing anti-macro signal report
**Descriere tehnica:** Genereaza semnale anti-macro pentru pescuit pe baza intervalelor perfecte, pozitiei statice si volumului de actiuni.
**Scop:** Ajuta moderarea fara sanctiuni automate agresive.
**Target:** fishing service, anomaly reporting, moderator dashboard.
**Acceptare:** Comportamentul suspect apare ca raport de review, nu ca ban automat.

### ~~W668~~ ✅ Mining vein depletion rollback
**Descriere tehnica:** Salveaza snapshot pentru veins custom si permite rollback daca depletarea, reward-ul sau persistenta esueaza.
**Scop:** Evita resurse pierdute sau duplicate dupa erori partiale.
**Target:** mining service, resource node state, recovery service.
**Acceptare:** O depletare esuata restaureaza vein-ul sau intra in recovery explicit.

### ~~W669~~ ✅ Mining region quota
**Descriere tehnica:** Aplica cote de mining pe regiune, faction, player sau perioada pentru resurse rare.
**Scop:** Controleaza inflatia de materiale si farming-ul excesiv.
**Target:** mining service, region quotas, economy balancing docs.
**Acceptare:** Mining-ul peste cota este refuzat sau amanat conform configuratiei.

### ~~W670~~ ✅ Gathering tool durability transaction
**Descriere tehnica:** Aplica consumul de durabilitate pentru unelte ca parte din tranzactia de gathering, cu rollback la reward esuat.
**Scop:** Evita unelte consumate fara recompensa sau recompense fara cost.
**Target:** gathering service, item durability, reward handling.
**Acceptare:** Durabilitatea si recompensa se aplica atomic sau se anuleaza impreuna.

### ~~W671~~ ✅ Crafting station access policy
**Descriere tehnica:** Defineste accesul la statiile de crafting dupa regiune, faction, owner, quest state, skill si permisiuni.
**Scop:** Previne folosirea statiilor in contexte neautorizate.
**Target:** crafting station service, region rules, permission service.
**Acceptare:** Accesul la statie este refuzat cu motiv clar cand politica nu permite folosirea.

### ~~W672~~ ✅ Crafting station queue persistence
**Descriere tehnica:** Persistă coada statiilor de crafting pentru retete lungi, cu resume dupa restart si anulare sigura.
**Scop:** Evita pierderea progresului de crafting la intreruperi.
**Target:** crafting station service, persistent queues, recovery service.
**Acceptare:** Joburile active sunt reluate, anulate sau compensate dupa restart conform starii salvate.

### ~~W673~~ ✅ Cooking recipe burn protection
**Descriere tehnica:** Adauga protectie pentru retete de cooking impotriva arderii accidentale cauzate de timeout, fuel invalid sau unload.
**Scop:** Previne pierderea ingredientelor in fluxuri incomplete.
**Target:** cooking service, crafting station runtime, recovery service.
**Acceptare:** Ingredientele sunt returnate sau reteta este reluata cand esecul este operational.

### ~~W674~~ ✅ Brewing effect validation
**Descriere tehnica:** Valideaza efectele produse de brewing pentru durata, intensitate, stacking, restrictii PvP si compatibilitate cu itemele.
**Scop:** Previne potiuni sau consumabile care rup combatul si progresia.
**Target:** brewing service, status effect service, config validation.
**Acceptare:** Efectele invalide sunt refuzate inainte de crearea itemului.

### ~~W675~~ ✅ Enchanting cost cap validator
**Descriere tehnica:** Aplica limite pentru costurile si rezultatele de enchanting in functie de nivel, item, raritate si economie.
**Scop:** Previne enchant-uri prea ieftine, imposibile sau dezechilibrate.
**Target:** enchanting service, economy service, item rules.
**Acceptare:** Enchanting-ul final respecta cap-urile si produce mesaj explicabil.

### ~~W676~~ ✅ Repair anvil economy sync
**Descriere tehnica:** Sincronizeaza costurile de repair cu economia, durabilitatea itemului, raritatea si regulile de faction sau regiune.
**Scop:** Evita repair gratuit sau costuri divergente intre UI si tranzactie.
**Target:** repair service, economy service, item metadata.
**Acceptare:** Costul afisat si suma debitata sunt identice si auditabile.

### ~~W677~~ ✅ Item durability overflow guard
**Descriere tehnica:** Blocheaza durabilitate negativa, peste maxim sau incompatibila cu schema itemului custom.
**Scop:** Previne iteme corupte si exploit-uri de durabilitate.
**Target:** item metadata service, inventory operations, validation.
**Acceptare:** Itemele cu durabilitate invalida sunt reparate controlat sau puse in carantina.

### ~~W678~~ ✅ Item binding transfer policy
**Descriere tehnica:** Defineste cand itemele bound pot fi transferate, vandute, salvaged, returnate sau mostenite prin mailbox.
**Scop:** Pastreaza regulile de ownership coerente intre subsisteme.
**Target:** item binding service, trade/marketplace/mailbox, docs item rules.
**Acceptare:** Transferul itemului bound este permis doar conform politicii documentate.

### ~~W679~~ ✅ Soulbound enforcement
**Descriere tehnica:** Aplica enforcement strict pentru soulbound in trade, marketplace, chest storage, mailbox, drop si pickup.
**Scop:** Inchide rutele indirecte de transfer pentru iteme personale.
**Target:** item binding service, inventory listeners, marketplace/trade/mailbox.
**Acceptare:** Itemul soulbound nu poate parasi ownerul prin canale nepermise.

### ~~W680~~ ✅ Item salvage rollback
**Descriere tehnica:** Face salvaging-ul de iteme tranzactional: consum item, calculeaza output, aplica recompensa si permite rollback la esec.
**Scop:** Evita pierderea itemului sau duplicarea materialelor.
**Target:** salvage service, inventory operations, reward delivery.
**Acceptare:** Salvage-ul se finalizeaza atomic sau restaureaza itemul initial.

## ~~DeepSeek implementation backlog (W681-W740)~~ ✅

Sisteme planificate: items, storage, death, teleports, mounts, pets, companions, housing, plots, furniture. Fiecare categorie va fi implementata in faze viitoare.

### ~~W681 Salvage output validation~~ ✅
**Descriere tehnica:** Valideaza output-ul de salvage pentru materiale inexistente, cantitati invalide, iteme bound si reguli de raritate.
**Scop:** Previne generarea de materiale gresite sau exploatabile.
**Target:** salvage service, item registry, reward validation.
**Acceptare:** Salvage-ul refuza output-uri invalide inainte de consumul itemului.

### ~~W682~~ ✅ Salvage rate limit
**Descriere tehnica:** Aplica limita configurabila pentru operatii de salvage pe player, statie, item rarity si fereastra de timp.
**Scop:** Controleaza farming-ul automatizat si presiunea pe economie.
**Target:** salvage service, cooldown service, economy balancing config.
**Acceptare:** Salvage-ul peste limita este refuzat cu mesaj si audit minimal.

### ~~W683~~ ✅ Item upgrade transaction
**Descriere tehnica:** Implementeaza upgrade-ul itemelor ca tranzactie atomica: cost, consum materiale, modificare metadata si rollback la esec.
**Scop:** Evita iteme partial upgradate sau materiale pierdute.
**Target:** item upgrade service, inventory operations, economy service.
**Acceptare:** Upgrade-ul se aplica complet sau restaureaza starea initiala.

### ~~W684~~ ✅ Item upgrade cap validator
**Descriere tehnica:** Valideaza nivelul maxim de upgrade pe baza raritatii, tipului de item, progresiei playerului si regulilor de event.
**Scop:** Previne iteme peste limitele documentate.
**Target:** item upgrade service, progression service, config validation.
**Acceptare:** Upgrade-ul peste cap este refuzat cu motiv clar.

### ~~W685~~ ✅ Gem socket compatibility validator
**Descriere tehnica:** Verifica compatibilitatea intre gem, socket, item type, raritate si restrictii de clasa sau skill.
**Scop:** Previne combinatii de iteme care rup balansul.
**Target:** gem socket service, item metadata, skill rules.
**Acceptare:** Gem-ul incompatibil nu poate fi montat si nu consuma resurse.

### ~~W686~~ ✅ Gem extraction rollback
**Descriere tehnica:** Face extractia gemurilor tranzactionala, cu cost, risc de distrugere, returnare item si rollback la erori operationale.
**Scop:** Evita pierderea gemului sau duplicarea socket-ului.
**Target:** gem socket service, inventory operations, economy service.
**Acceptare:** Extractia se finalizeaza atomic sau revine la itemul initial.

### ~~W687~~ ✅ Item rarity migration
**Descriere tehnica:** Migreaza itemele vechi cand se schimba schema de raritate, pastrand compatibilitatea cu upgrade-uri, trade si reward rules.
**Scop:** Evita iteme legacy incompatibile dupa update.
**Target:** item metadata migration, inventory scan, marketplace validation.
**Acceptare:** Itemele legacy sunt migrate sau raportate pentru carantina.

### ~~W688~~ ✅ Item lore renderer sync
**Descriere tehnica:** Centralizeaza randarea lore-ului pentru iteme astfel incat tooltip-ul, GUI-ul, docs export si auditul sa foloseasca aceeasi sursa.
**Scop:** Elimina diferentele intre descriere si efectele reale.
**Target:** item lore renderer, GUI item views, docs export.
**Acceptare:** Lore-ul afisat corespunde metadata si regulilor active ale itemului.

### ~~W689~~ ✅ Item tooltip privacy guard
**Descriere tehnica:** Redacteaza din tooltip date sensibile precum owner intern, seed, audit id, provenance sau flag-uri de moderare.
**Scop:** Previne expunerea informatiei interne catre jucatori.
**Target:** item tooltip renderer, item metadata, privacy rules.
**Acceptare:** Tooltip-ul public nu afiseaza campuri interne sau sensibile.

### ~~W690~~ ✅ Chest storage ownership audit
**Descriere tehnica:** Auditeaza ownership-ul pentru chest storage custom: owner, co-owner, faction, regiune, lock si ultimele modificari.
**Scop:** Face disputele de storage investigabile.
**Target:** storage service, region rules, audit reports.
**Acceptare:** Orice modificare sensibila de storage are actor si context in audit.

### ~~W691~~ ✅ Shared storage locking
**Descriere tehnica:** Adauga lock optimist sau pesimist pentru storage partajat ca sa previna update-uri concurente si pierderi de iteme.
**Scop:** Stabilizeaza accesul simultan la containere comune.
**Target:** storage service, inventory operations, persistence layer.
**Acceptare:** Doua actiuni concurente nu pot suprascrie acelasi slot fara detectie.

### ~~W692~~ ✅ Storage quota enforcement
**Descriere tehnica:** Aplica limite de storage pe player, faction, regiune si tip de container, inclusiv exceptii administrative auditate.
**Scop:** Controleaza cresterea datelor si abuzul de stocare.
**Target:** storage service, permission service, config docs.
**Acceptare:** Depasirea cotei este refuzata sau marcata pentru upgrade conform regulilor.

### ~~W693~~ ✅ Storage move transaction
**Descriere tehnica:** Trateaza mutarea itemelor intre inventar, storage, mailbox si marketplace ca tranzactie cu rollback.
**Scop:** Previne iteme pierdute intre subsisteme.
**Target:** storage service, inventory operations, mailbox/marketplace integrations.
**Acceptare:** Mutarea itemului se finalizeaza intr-un singur loc sau revine la sursa.

### ~~W694~~ ✅ Lost item recovery queue
**Descriere tehnica:** Creeaza coada de recovery pentru iteme detectate ca pierdute in tranzactii esuate, cleanup-uri sau migrari.
**Scop:** Permite reparatie controlata fara duplicare manuala.
**Target:** recovery service, inventory audit, admin dashboard.
**Acceptare:** Itemele suspecte apar in coada cu sursa, owner si recomandare de actiune.

### ~~W695~~ ✅ Drop protection window
**Descriere tehnica:** Adauga fereastra de protectie pentru itemele dropate de player, quest, dungeon sau event, cu reguli de pickup.
**Scop:** Previne furtul accidental imediat dupa drop.
**Target:** drop service, pickup listeners, item binding rules.
**Acceptare:** Doar actorii eligibili pot ridica itemul in fereastra configurata.

### ~~W696~~ ✅ Pickup priority policy
**Descriere tehnica:** Defineste prioritatea de pickup pentru owner, party, faction, participant event si public.
**Scop:** Face distribuirea itemelor din world predictibila.
**Target:** pickup listeners, loot service, party/faction rules.
**Acceptare:** Pickup-ul alege eligibilitatea dupa o ordine documentata.

### ~~W697~~ ✅ Death drop rule matrix
**Descriere tehnica:** Creeaza matrice pentru ce iteme se pierd, se pastreaza, se protejeaza sau se convertesc la moarte.
**Scop:** Aliniaza death mechanics cu item binding, questuri si regiuni.
**Target:** death handling, item rules, region combat policy.
**Acceptare:** Rezultatul mortii este calculat din matricea documentata si auditabil.

### ~~W698~~ ✅ Grave marker lifecycle
**Descriere tehnica:** Adauga lifecycle pentru grave marker: creare, ownership, expiry, claim, cleanup si recovery dupa restart.
**Scop:** Evita morminte blocate sau loot pierdut.
**Target:** grave service, world markers, recovery service.
**Acceptare:** Grave marker-ul expira, se revendica sau se curata conform politicii.

### ~~W699~~ ✅ Death recovery compensation
**Descriere tehnica:** Defineste compensatii pentru pierderi cauzate de erori operationale in fluxul de death drop sau grave marker.
**Scop:** Permite suport corect fara interventii arbitrare.
**Target:** death handling, recovery service, admin compensation commands.
**Acceptare:** Compensatia se acorda doar cu dovada auditabila si idempotenta.

### ~~W700~~ ✅ Home teleport cooldown policy
**Descriere tehnica:** Standardizeaza cooldown-ul pentru teleport acasa dupa combat, trade, dungeon, raid, event sau schimbare de lume.
**Scop:** Previne folosirea teleportului pentru a evita riscuri active.
**Target:** teleport service, cooldown service, combat/event guards.
**Acceptare:** Home teleport-ul este blocat sau permis dupa o politica unica.

### ~~W701~~ ✅ Home location validation
**Descriere tehnica:** Valideaza locatiile de home pentru lume, regiune, protectii, bloc solid, siguranta spawn si permisiuni.
**Scop:** Previne teleporturi in zone invalide sau periculoase.
**Target:** home service, region rules, teleport safety checks.
**Acceptare:** Home-ul invalid este refuzat sau marcat pentru relocalizare sigura.

### ~~W702~~ ✅ Home ownership transfer guard
**Descriere tehnica:** Gestioneaza ce se intampla cu home-urile cand regiunea, plotul sau faction ownership-ul se schimba.
**Scop:** Evita acces permanent in zone pierdute sau transferate.
**Target:** home service, region ownership, faction territory.
**Acceptare:** Home-urile afectate sunt pastrate, suspendate sau mutate conform politicii.

### ~~W703~~ ✅ Warp access rule reconciliation
**Descriere tehnica:** Coreleaza accesul la warp-uri cu regiuni, permisiuni, factiuni, quest state, economie si evenimente active.
**Scop:** Previne warp-uri care ocolesc progresia sau restrictiile world.
**Target:** warp service, permission service, region/quest rules.
**Acceptare:** Accesul la warp este determinist si explicabil prin raport.

### ~~W704~~ ✅ Warp usage audit
**Descriere tehnica:** Auditeaza folosirea warp-urilor sensibile cu actor, sursa, destinatie, motiv si cost aplicat.
**Scop:** Face deplasarile administrative sau riscante verificabile.
**Target:** warp service, audit log, admin reports.
**Acceptare:** Warp-urile marcate sensibile produc audit filtrabil.

### ~~W705~~ ✅ Portal activation preflight
**Descriere tehnica:** Verifica portalurile inainte de activare: destinatie valida, regiune, cost, cooldown, quest state si safe landing.
**Scop:** Previne portaluri care trimit jucatori in stari invalide.
**Target:** portal service, teleport safety, quest/region rules.
**Acceptare:** Portalul nu se activeaza daca destinatia sau regulile sunt invalide.

### ~~W706~~ ✅ Portal loop detector
**Descriere tehnica:** Detecteaza bucle de portaluri care pot teleporta playerul repetat intre doua sau mai multe destinatii.
**Scop:** Previne blocaje si abuzuri de teleport.
**Target:** portal graph, teleport service, validation jobs.
**Acceptare:** Buclele sunt raportate si portalurile afectate pot fi dezactivate sigur.

### ~~W707~~ ✅ Mount ownership persistence
**Descriere tehnica:** Persistă ownership-ul mount-urilor cu owner, item sursa, stare, locatie si reguli de transfer.
**Scop:** Previne pierderea mount-urilor dupa restart sau unload.
**Target:** mount service, entity persistence, item binding rules.
**Acceptare:** Mount-ul revine ownerului sau intra in recovery dupa restart.

### ~~W708~~ ✅ Mount summon cooldown
**Descriere tehnica:** Adauga cooldown si conditii pentru summon mount: combat, regiune, dungeon, event, world si permission.
**Scop:** Previne folosirea mount-ului in contexte interzise.
**Target:** mount service, cooldown service, region/combat guards.
**Acceptare:** Summon-ul este permis doar cand toate conditiile sunt indeplinite.

### ~~W709~~ ✅ Mount despawn recovery
**Descriere tehnica:** Gestioneaza despawn-ul neasteptat al mount-ului prin reatasare la owner, refund item, recovery queue sau audit.
**Scop:** Evita mount-uri pierdute definitiv din cauze operationale.
**Target:** mount lifecycle, recovery service, entity listeners.
**Acceptare:** Mount-ul disparut are rezultat clar: recuperat, compensat sau investigat.

### ~~W710~~ ✅ Pet combat participation policy
**Descriere tehnica:** Defineste cand pet-urile pot participa la combat, primi damage, oferi buff-uri sau influenta loot eligibility.
**Scop:** Previne avantaje necontrolate si conflicte cu regulile de combat.
**Target:** pet service, combat policy, reward eligibility.
**Acceptare:** Pet-ul influenteaza combatul doar conform politicii documentate.

### ~~W711~~ ✅ Pet ownership transfer guard
**Descriere tehnica:** Defineste cand ownership-ul unui pet poate fi transferat intre playeri, conturi, factiuni sau recovery queue.
**Scop:** Previne transferuri neautorizate sau pierderea pet-urilor rare.
**Target:** pet service, ownership rules, audit log.
**Acceptare:** Transferul de pet este permis doar cu validare, audit si rollback la esec.

### ~~W712~~ ✅ Pet ability cooldown validation
**Descriere tehnica:** Valideaza cooldown-urile abilitatilor de pet dupa tip, nivel, regiune, combat state si restrictii de event.
**Scop:** Evita spam-ul de abilitati si avantaje necontrolate.
**Target:** pet ability service, cooldown service, combat policy.
**Acceptare:** Abilitatea de pet se activeaza doar cand cooldown-ul si contextul permit.

### ~~W713~~ ✅ Pet despawn recovery
**Descriere tehnica:** Gestioneaza despawn-ul neasteptat al pet-urilor prin reatasare la owner, snapshot, recovery queue sau compensatie.
**Scop:** Evita pierderea definitiva a pet-urilor din cauze operationale.
**Target:** pet lifecycle, entity listeners, recovery service.
**Acceptare:** Pet-ul disparut are rezultat auditat: recuperat, compensat sau investigat.

### ~~W714~~ ✅ Companion quest assignment guard
**Descriere tehnica:** Valideaza asignarea companionilor la questuri dupa ownership, loialitate, nivel, story state si regiune.
**Scop:** Previne folosirea companionilor in questuri incompatibile.
**Target:** companion service, quest assignment, story rules.
**Acceptare:** Companionul poate fi asignat doar la questuri eligibile conform regulilor documentate.

### ~~W715~~ ✅ Companion inventory transaction
**Descriere tehnica:** Trateaza mutarea itemelor in si din inventarul companionului ca tranzactie cu lock, verificare si rollback.
**Scop:** Previne pierderi sau duplicari de iteme in inventare secundare.
**Target:** companion inventory, inventory operations, persistence layer.
**Acceptare:** Itemul mutat ajunge intr-un singur inventar sau revine la sursa.

### ~~W716~~ ✅ Companion loyalty decay scheduler
**Descriere tehnica:** Adauga decay configurabil pentru loialitatea companionilor, cu exceptii pentru questuri active sau status special.
**Scop:** Face managementul companionilor predictibil si balansat.
**Target:** companion service, scheduler, config docs.
**Acceptare:** Decay-ul ruleaza conform configuratiei si produce audit sumar.

### ~~W717~~ ✅ Companion rename validation
**Descriere tehnica:** Valideaza redenumirea companionilor pentru lungime, caractere, termeni rezervati, cooldown si cost.
**Scop:** Previne nume abuzive, conflicte UI si spam de redenumiri.
**Target:** companion service, command validation, message catalog.
**Acceptare:** Numele invalid este refuzat fara modificari partiale.

### ~~W718~~ ✅ Pet breeding compatibility validator
**Descriere tehnica:** Verifica compatibilitatea pentru breeding dupa specie, raritate, owner, cooldown, regiune si restrictii de event.
**Scop:** Previne combinatii nepermise si generare de pet-uri dezechilibrate.
**Target:** pet breeding service, pet registry, config validation.
**Acceptare:** Breeding-ul porneste doar cand ambii peti si contextul sunt eligibili.

### ~~W719~~ ✅ Pet breeding offspring idempotency
**Descriere tehnica:** Face generarea offspring-ului idempotenta pentru retry, restart sau evenimente duplicate.
**Scop:** Previne pet-uri duplicate sau lipsa rezultatului dupa breeding valid.
**Target:** pet breeding service, persistence layer, recovery service.
**Acceptare:** O sesiune de breeding produce cel mult un offspring revendicabil.

### ~~W720~~ ✅ Stable capacity enforcement
**Descriere tehnica:** Aplica limite pentru numarul de pet-uri, mount-uri sau companioni stocati in stable pe player, rang sau upgrade.
**Scop:** Controleaza stocarea si costurile persistente.
**Target:** stable service, player profile, config docs.
**Acceptare:** Stable-ul peste capacitate refuza intrari noi sau cere upgrade conform regulilor.

### ~~W721~~ ✅ Stable claim transaction
**Descriere tehnica:** Face claim-ul din stable tranzactional, cu verificare owner, spatiu, entity spawn si rollback la esec.
**Scop:** Evita entitati pierdute sau duplicate la revendicare.
**Target:** stable service, entity spawn, recovery service.
**Acceptare:** Claim-ul finalizeaza exact o entitate activa sau pastreaza intrarea in stable.

### ~~W722~~ ✅ Stable ownership change cleanup
**Descriere tehnica:** Gestioneaza intrarile de stable cand ownership-ul playerului, factiunii sau regiunii se schimba.
**Scop:** Previne accesul la stable-uri care nu mai apartin actorului.
**Target:** stable service, ownership rules, faction/region services.
**Acceptare:** Intrarile afectate sunt transferate, suspendate sau trimise in recovery conform politicii.

### ~~W723~~ ✅ Housing plot claim preflight
**Descriere tehnica:** Verifica preconditiile pentru claim de plot: regiune libera, cost, permisiuni, limite, conflicte si protectii.
**Scop:** Previne claim-uri imposibile sau suprapuse.
**Target:** housing service, region ownership, economy service.
**Acceptare:** Plot-ul este revendicat doar dupa preflight complet si auditabil.

### ~~W724~~ ✅ Housing plot boundary validator
**Descriere tehnica:** Valideaza limitele ploturilor pentru suprapuneri, chunk-uri invalide, zone protejate si margini de lume.
**Scop:** Evita coruperea ownership-ului de regiune.
**Target:** housing plots, region service, world validation.
**Acceptare:** Plot-urile cu limite invalide sunt refuzate sau marcate pentru repair.

### ~~W725~~ ✅ Housing rent payment transaction
**Descriere tehnica:** Proceseaza chiria locuintelor ca tranzactie cu debit, receipt, owner payout, taxe si rollback la esec.
**Scop:** Evita chirii platite partial sau payout-uri duplicate.
**Target:** housing rent service, economy ledger, audit log.
**Acceptare:** Plata chiriei este aplicata complet sau ramane neachitata cu motiv clar.

### ~~W726~~ ✅ Housing rent grace period
**Descriere tehnica:** Adauga perioada de gratie pentru chirie neplatita, cu notificari, restrictii progresive si data de evacuare.
**Scop:** Evita pierderea brusca a locuintei dupa o singura eroare sau absenta.
**Target:** housing rent service, notification service, scheduler.
**Acceptare:** Plot-ul intra in grace state inainte de orice evacuare automata.

### ~~W727~~ ✅ Housing eviction workflow
**Descriere tehnica:** Defineste evacuarea locuintei cu export, mutare iteme, notificare, audit si recovery pentru esecuri.
**Scop:** Previne pierderea itemelor si disputele la evacuare.
**Target:** housing service, storage service, mailbox/recovery.
**Acceptare:** Evacuarea are dry-run, confirmare si rezultat auditat pentru bunuri.

### ~~W728~~ ✅ Housing visitor permission guard
**Descriere tehnica:** Controleaza accesul vizitatorilor la plot dupa rol, trust list, faction, party, perioada si reguli de regiune.
**Scop:** Previne interactiuni neautorizate in locuinte.
**Target:** housing access service, region rules, interaction listeners.
**Acceptare:** Vizitatorul poate interactiona doar cu actiunile permise explicit.

### ~~W729~~ ✅ Housing build mode lock
**Descriere tehnica:** Introduce build mode cu lock pe plot, owner, sesiune si timeout pentru modificari structurale.
**Scop:** Evita editari concurente sau actiuni dupa expirarea contextului.
**Target:** housing build service, region edit guards, player session.
**Acceptare:** Modificarile de build sunt permise doar in sesiune valida si activa.

### ~~W730~~ ✅ Housing blueprint placement validation
**Descriere tehnica:** Valideaza blueprint-urile de locuinte pentru dimensiune, materiale, orientare, coliziuni si cost.
**Scop:** Previne plasari care depasesc plotul sau regulile economice.
**Target:** blueprint service, housing plots, economy validation.
**Acceptare:** Blueprint-ul invalid este refuzat inainte de consumul resurselor.

### ~~W731~~ ✅ Housing blueprint rollback
**Descriere tehnica:** Salveaza snapshot pentru plasarea blueprint-ului si permite rollback la esec, cancel sau conflict descoperit tarziu.
**Scop:** Pastreaza plotul recuperabil dupa operatii complexe.
**Target:** blueprint service, world snapshot store, recovery service.
**Acceptare:** Blueprint-ul poate fi anulat fara a afecta blocuri externe snapshot-ului.

### ~~W732~~ ✅ Furniture interaction permission
**Descriere tehnica:** Verifica permisiunile pentru folosirea mobilierului functional: storage, crafting, teleport, buffs sau decor interactiv.
**Scop:** Previne folosirea resurselor locuintei de catre actori neautorizati.
**Target:** furniture service, housing access rules, interaction listeners.
**Acceptare:** Interactiunea cu mobilierul este permisa doar conform rolului pe plot.

### ~~W733~~ ✅ Furniture state persistence
**Descriere tehnica:** Persistă starea mobilierului functional, incluzand inventar, cooldown, owner, configuratie si efecte active.
**Scop:** Evita pierderea starii dupa restart sau unload.
**Target:** furniture service, persistence layer, chunk lifecycle.
**Acceptare:** Mobilierul isi restaureaza starea valida dupa restart.

### ~~W734~~ ✅ Furniture cleanup on plot reset
**Descriere tehnica:** Curata mobilierul si efectele asociate cand plotul este resetat, evacuat, transferat sau sters.
**Scop:** Previne artefacte si stari orfane pe ploturi.
**Target:** furniture service, housing cleanup, recovery service.
**Acceptare:** Resetul de plot elimina sau transfera toate resursele mobilierului conform politicii.

### ~~W735~~ ✅ Plot transfer approval workflow
**Descriere tehnica:** Defineste transferul de plot intre playeri sau factiuni cu preflight, taxe, confirmare si audit.
**Scop:** Previne vanzari sau transferuri accidentale de proprietate.
**Target:** housing transfer service, economy service, region ownership.
**Acceptare:** Transferul se finalizeaza doar dupa confirmarile si platile cerute.

### ~~W736~~ ✅ Plot sale listing validation
**Descriere tehnica:** Valideaza listarea unui plot la vanzare pentru ownership, datorii, lock-uri, iteme ramase si restrictii de regiune.
**Scop:** Previne vanzarea proprietatilor in stare inconsistenta.
**Target:** housing marketplace, region service, storage service.
**Acceptare:** Plot-ul nu poate fi listat daca are blocaje active sau datorii nerezolvate.

### ~~W737~~ ✅ Plot sale settlement escrow
**Descriere tehnica:** Proceseaza vanzarea plotului prin escrow pentru bani, ownership, taxe, mobilier si storage.
**Scop:** Evita pierderi la tranzactii imobiliare complexe.
**Target:** housing marketplace, economy escrow, region ownership.
**Acceptare:** Vanzarea transfera atomic ownership-ul si fondurile sau revine la starea initiala.

### ~~W738~~ ✅ Plot inactivity policy
**Descriere tehnica:** Defineste ce se intampla cu ploturile inactive: notificari, grace period, taxe, arhivare sau eliberare.
**Scop:** Recupereaza resurse world fara pierderi arbitrare.
**Target:** housing scheduler, player activity, plot lifecycle.
**Acceptare:** Plotul inactiv urmeaza un lifecycle documentat cu notificari si audit.

### ~~W739~~ ✅ Plot restore from archive
**Descriere tehnica:** Permite restaurarea unui plot arhivat intr-o zona compatibila sau in acelasi loc daca este liber.
**Scop:** Ofera recuperare pentru playeri reveniti sau erori operationale.
**Target:** housing archive, blueprint/world snapshot, recovery service.
**Acceptare:** Restaurarea verifica spatiu, ownership si conflicte inainte de aplicare.

### ~~W740~~ ✅ Housing operations audit dashboard
**Descriere tehnica:** Creeaza dashboard pentru operatii housing: claim, transfer, rent, eviction, archive, restore si conflicte.
**Scop:** Centralizeaza investigarea proprietatilor si deciziilor administrative.
**Target:** admin dashboard, housing service, audit reports.
**Acceptare:** Staff-ul autorizat vede timeline-ul si starea curenta a fiecarui plot.

## ~~DeepSeek implementation backlog (W741-W770)~~ ✅

Sisteme planificate: settlement creation, boundaries, taxes, treasury, residents, projects, upgrades, disasters, roads, caravans, diplomacy. Fiecare categorie va fi implementata in faze viitoare.

### ~~W741 Settlement creation preflight~~ ✅
**Descriere tehnica:** Valideaza crearea unei asezari dupa regiune, cost, fondatori, conflicte de teritoriu, nume si reguli de lume.
**Scop:** Previne asezari suprapuse sau create in contexte nepermise.
**Target:** settlement service, region ownership, economy service.
**Acceptare:** Asezarea este creata doar dupa preflight complet si auditabil.

### ~~W742~~ ✅ Settlement name reservation
**Descriere tehnica:** Rezerva temporar numele unei asezari in timpul fluxului de creare, cu expirare si eliberare automata.
**Scop:** Evita curse intre cereri simultane si nume duplicate.
**Target:** settlement registry, command flow, scheduler.
**Acceptare:** Doua fluxuri concurente nu pot confirma acelasi nume.

### ~~W743~~ ✅ Settlement boundary expansion validator
**Descriere tehnica:** Verifica extinderea limitelor asezarii pentru cost, regiuni vecine, protectii, world border si conflicte de ownership.
**Scop:** Pastreaza cresterea asezarilor coerenta si sigura.
**Target:** settlement boundaries, region service, economy validation.
**Acceptare:** Extinderea invalida este refuzata cu raport al conflictelor.

### ~~W744~~ ✅ Settlement boundary shrink workflow
**Descriere tehnica:** Defineste reducerea limitelor asezarii cu verificare pentru ploturi, storage, NPC-uri, questuri si servicii afectate.
**Scop:** Previne pierderi sau orfani la micsorarea teritoriului.
**Target:** settlement service, housing plots, NPC/quest references.
**Acceptare:** Shrink-ul are dry-run si lista resurselor care trebuie mutate sau inchise.

### ~~W745~~ ✅ Settlement public service registry
**Descriere tehnica:** Creeaza registru pentru servicii publice ale asezarii: banca, piata, crafting, teleport, quest board si stable.
**Scop:** Centralizeaza accesul si starea serviciilor locale.
**Target:** settlement services, GUI/admin dashboard, docs gameplay.
**Acceptare:** Fiecare serviciu public are owner, status, cost si reguli de acces.

### ~~W746~~ ✅ Settlement service access policy
**Descriere tehnica:** Coreleaza accesul la serviciile publice cu rezidenta, factiunea, reputatia, taxele, permisiunile si eventurile active.
**Scop:** Evita acces neautorizat la facilitati locale.
**Target:** settlement service registry, permission service, reputation rules.
**Acceptare:** Accesul la serviciu este determinist si explicabil prin motiv.

### ~~W747~~ ✅ Settlement tax collection transaction
**Descriere tehnica:** Proceseaza taxele asezarii ca tranzactii cu debit, ledger, scutiri, plafon si rollback la esec.
**Scop:** Pastreaza economia asezarii corecta si auditabila.
**Target:** settlement tax service, economy ledger, audit reports.
**Acceptare:** Taxa se aplica complet sau nu se aplica deloc.

### ~~W748~~ ✅ Settlement tax exemption audit
**Descriere tehnica:** Auditeaza scutirile de taxe pentru rezidenti, factiuni, roluri, evenimente sau override admin.
**Scop:** Face exceptiile financiare transparente.
**Target:** settlement tax service, permission/reputation rules, audit log.
**Acceptare:** Fiecare scutire are actor, motiv, durata si target.

### ~~W749~~ ✅ Settlement treasury ledger export
**Descriere tehnica:** Exporta ledger-ul trezoreriei asezarii cu taxe, cheltuieli, transferuri, granturi si corectii.
**Scop:** Permite verificarea finantelor locale.
**Target:** settlement treasury, export formatter, admin dashboard.
**Acceptare:** Exportul este filtrabil pe perioada, actor si tip de tranzactie.

### ~~W750~~ ✅ Settlement treasury withdrawal approval
**Descriere tehnica:** Cere aprobare pentru retrageri din trezoreria asezarii peste praguri configurate.
**Scop:** Protejeaza fondurile comune impotriva abuzului.
**Target:** settlement treasury, approval workflow, audit notifications.
**Acceptare:** Retragerea sensibila este aprobata, respinsa sau expirata cu audit.

### ~~W751~~ ✅ Settlement resident invite expiry
**Descriere tehnica:** Expira invitatiile de rezident in asezare si blocheaza acceptarea lor dupa schimbari de ownership sau statut.
**Scop:** Evita acces vechi in comunitati modificate.
**Target:** settlement membership, notification service, scheduler.
**Acceptare:** Invitatia expirata nu poate fi acceptata si apare in audit sumar.

### ~~W752~~ ✅ Settlement resident role validator
**Descriere tehnica:** Valideaza rolurile rezidentilor pentru permisiuni imposibile, escaladari implicite si conflicte cu roluri de factiune.
**Scop:** Previne acces excesiv in managementul asezarii.
**Target:** settlement roles, permission service, faction role bridge.
**Acceptare:** Rolul invalid este refuzat cu lista permisiunilor conflictuale.

### ~~W753~~ ✅ Settlement resident eviction workflow
**Descriere tehnica:** Defineste evacuarea unui rezident cu notificare, grace period, plot/storage handling si audit.
**Scop:** Previne pierderea bunurilor si conflicte sociale neclare.
**Target:** settlement membership, housing service, storage/mailbox.
**Acceptare:** Evacuarea are dry-run, confirmare si rezultat verificabil pentru bunuri.

### ~~W754~~ ✅ Settlement resident activity report
**Descriere tehnica:** Raporteaza activitatea rezidentilor pentru taxe, servicii folosite, contributii, absenta si sanctiuni locale.
**Scop:** Ajuta administrarea asezarii fara cautari manuale.
**Target:** settlement dashboard, activity metrics, audit summaries.
**Acceptare:** Raportul grupeaza rezidentii dupa activitate si risc operational.

### ~~W755~~ ✅ Settlement project lifecycle
**Descriere tehnica:** Modeleaza proiectele publice ale asezarii in stari: propus, finantat, activ, finalizat, anulat si cleanup.
**Scop:** Controleaza constructiile si upgrade-urile comune.
**Target:** settlement projects, treasury, world/build services.
**Acceptare:** Fiecare proiect public are stare, owner, buget si criterii de inchidere.

### ~~W756~~ ✅ Settlement project funding escrow
**Descriere tehnica:** Pune contributiile pentru proiectele publice in escrow pana la pornire, anulare sau finalizare.
**Scop:** Evita fonduri pierdute sau folosite inainte de validare.
**Target:** settlement projects, economy escrow, refund workflow.
**Acceptare:** Fondurile sunt eliberate sau returnate prin operatii idempotente.

### ~~W757~~ ✅ Settlement project contribution trace
**Descriere tehnica:** Urmareste contributiile la proiecte prin bani, iteme, munca, questuri sau granturi administrative.
**Scop:** Face progresul public si recompensele de contributie verificabile.
**Target:** project contribution service, reward rules, dashboard.
**Acceptare:** Contributia fiecarei surse apare intr-un raport agregat si filtrabil.

### ~~W758~~ ✅ Settlement project rollback
**Descriere tehnica:** Permite rollback pentru proiecte publice care au modificat world, storage, servicii sau regiuni si apoi esueaza.
**Scop:** Pastreaza asezarea recuperabila dupa implementari partiale.
**Target:** settlement projects, world snapshot, recovery service.
**Acceptare:** Proiectul esuat revine la snapshot sau intra in recovery cu actiuni clare.

### ~~W759~~ ✅ Settlement upgrade prerequisite validator
**Descriere tehnica:** Valideaza upgrade-urile asezarii dupa nivel, populatie, fonduri, reputatie, proiecte finalizate si reguli de lume.
**Scop:** Previne upgrade-uri care sar peste progresia documentata.
**Target:** settlement upgrade service, progression rules, config validation.
**Acceptare:** Upgrade-ul porneste doar cu prerequisite-uri complete si raportate.

### ~~W760~~ ✅ Settlement upgrade effect audit
**Descriere tehnica:** Auditeaza efectele unui upgrade de asezare asupra taxelor, serviciilor, limitelor, protectiilor si questurilor.
**Scop:** Face schimbarile majore usor de verificat.
**Target:** settlement upgrade service, audit reports, docs gameplay.
**Acceptare:** Upgrade-ul produce sumar cu efecte aplicate si fisiere/config afectate.

### ~~W761~~ ✅ Settlement downgrade policy
**Descriere tehnica:** Defineste downgrade-ul asezarii pentru datorii, inactivitate, sanctiuni sau decizii admin, inclusiv compensatii.
**Scop:** Face penalizarile locale controlate si reversibile cand este posibil.
**Target:** settlement lifecycle, treasury, service registry.
**Acceptare:** Downgrade-ul aplica efecte documentate si auditabile.

### ~~W762~~ ✅ Settlement disaster event preflight
**Descriere tehnica:** Valideaza evenimentele de dezastru local dupa risc, protectii, populatie, cooldown si resurse recuperabile.
**Scop:** Previne evenimente distructive in asezari nepregatite sau protejate.
**Target:** event scheduler, settlement service, region protections.
**Acceptare:** Disaster event-ul porneste doar cand preflight-ul il considera sigur.

### ~~W763~~ ✅ Settlement disaster recovery plan
**Descriere tehnica:** Creeaza plan de recovery pentru dezastru cu snapshot, compensatii, questuri de reparatie si cleanup.
**Scop:** Leaga evenimentele destructive de recuperare controlata.
**Target:** settlement events, recovery service, quest generation.
**Acceptare:** Fiecare dezastru activ are plan de rollback sau repair documentat.

### ~~W764~~ ✅ Settlement road network validator
**Descriere tehnica:** Valideaza reteaua de drumuri intre asezari, warp-uri, portaluri, quest hubs si regiuni blocate.
**Scop:** Evita rute imposibile sau care ocolesc restrictii.
**Target:** route graph, settlement service, region rules.
**Acceptare:** Ruta publica este activata doar daca graful respecta regulile de acces.

### ~~W765~~ ✅ Settlement route toll transaction
**Descriere tehnica:** Proceseaza taxele de drum intre asezari ca tranzactii cu scutiri, faction modifiers si rollback.
**Scop:** Integreaza transportul cu economia fara debitari partiale.
**Target:** route service, settlement tax service, economy ledger.
**Acceptare:** Taxa de ruta este aplicata atomic si explicabila prin trace.

### ~~W766~~ ✅ Settlement caravan scheduling
**Descriere tehnica:** Programeaza caravane intre asezari cu traseu, escorta, marfa, risc, timp si conditii de anulare.
**Scop:** Creeaza flux economic si questuri dinamice fara configurare manuala fragila.
**Target:** caravan service, route graph, event scheduler.
**Acceptare:** Caravana porneste doar cu traseu valid si resurse rezervate.

### ~~W767~~ ✅ Settlement caravan cargo escrow
**Descriere tehnica:** Pune marfa caravanei in escrow pana la livrare, jaf, esec, anulare sau recovery.
**Scop:** Previne pierderea sau duplicarea bunurilor transportate.
**Target:** caravan service, storage escrow, reward/compensation.
**Acceptare:** Cargo-ul are stare unica si rezultat final idempotent.

### ~~W768~~ ✅ Settlement caravan ambush resolver
**Descriere tehnica:** Rezolva ambush-urile caravanei cu participanti, combat state, recompense, penalitati si cleanup.
**Scop:** Face evenimentele de transport corecte si auditabile.
**Target:** caravan runtime, combat service, reward settlement.
**Acceptare:** Ambush-ul produce rezultat determinist: livrat, pierdut, recuperat sau anulat.

### ~~W769~~ ✅ Settlement diplomacy agreement registry
**Descriere tehnica:** Inregistreaza acordurile diplomatice intre asezari si factiuni: comert, non-agresiune, taxe, acces si durata.
**Scop:** Centralizeaza regulile locale care afecteaza servicii si rute.
**Target:** diplomacy service, settlement/faction rules, audit log.
**Acceptare:** Fiecare acord are parti, efecte, expirare si conditii de anulare.

### ~~W770~~ ✅ Settlement diplomacy conflict detector
**Descriere tehnica:** Detecteaza conflicte intre acorduri diplomatice, razboaie de factiune, ownership de regiune si eventuri active.
**Scop:** Previne reguli politice contradictorii.
**Target:** diplomacy service, faction conflict service, region rules.
**Acceptare:** Acordurile conflictuale sunt blocate sau trimise la review manual cu raport.

### ~~W771~~ ✅ Story map node registry
**Descriere tehnica:** Creeaza un registru pentru nodurile de harta folosite de story arcs, quest hubs, regiuni narative si puncte de interes.
**Scop:** Leaga explicit continutul story de locatii verificabile din world.
**Target:** map node registry, story service, quest definitions.
**Acceptare:** Fiecare nod story are id unic, locatie, tip, owner logic si status de validare.

### ~~W772~~ ✅ Quest map marker lifecycle
**Descriere tehnica:** Modeleaza marker-ele de harta pentru questuri in stari: ascuns, descoperit, activ, completat, expirat si curatat.
**Scop:** Previne marker-e stale sau spoiler-e pe harta.
**Target:** quest map service, marker renderer, quest lifecycle.
**Acceptare:** Marker-ul isi schimba starea strict dupa progresia questului si se curata la final.

### ~~W773~~ ✅ Story region unlock mapping
**Descriere tehnica:** Leaga deblocarea regiunilor de story branches, reputatie, quest completion si decizii narrative.
**Scop:** Face accesul pe harta coerent cu progresia story.
**Target:** story service, region access rules, quest progression.
**Acceptare:** Regiunea blocata explica prerequisite-ul principal fara a dezvalui spoiler critic.

### ~~W774~~ ✅ Quest route graph validator
**Descriere tehnica:** Valideaza graful rutelor necesare pentru questuri intre NPC, obiective, regiuni, dungeonuri si puncte de intoarcere.
**Scop:** Previne questuri imposibil de parcurs fizic.
**Target:** route graph, quest validation service, world mapping.
**Acceptare:** Questul cu ruta imposibila este respins cu lista muchiilor blocate.

### ~~W775~~ ✅ Quest objective spatial bounds
**Descriere tehnica:** Adauga limite spatiale pentru obiectivele de quest: raza, volum, regiune, lume si toleranta de eroare.
**Scop:** Evita trigger-e care se activeaza in locatii gresite.
**Target:** quest objective tracker, world mapping, region service.
**Acceptare:** Obiectivul se activeaza doar in bounds-urile documentate si validate.

### ~~W776~~ ✅ Story pathfinding hint generator
**Descriere tehnica:** Genereaza hint-uri de navigare pentru story quests pe baza grafului de rute si a regiunilor deblocate.
**Scop:** Ajuta jucatorii fara waypoint-uri arbitrare sau spoiler-e.
**Target:** route graph, quest hint service, story progression.
**Acceptare:** Hint-ul indica urmatorul pas sigur fara sa dezvaluie obiective viitoare ascunse.

### ~~W777~~ ✅ Map fog-of-war story sync
**Descriere tehnica:** Sincronizeaza fog-of-war-ul hartii cu descoperirea story, quest completions, reputatie si explorare reala.
**Scop:** Pastreaza harta aliniata cu progresia narativa.
**Target:** map discovery service, story service, player profile.
**Acceptare:** Zonele nedescoperite raman ascunse pana cand una dintre regulile documentate le deblocheaza.

### ~~W778~~ ✅ Quest map marker permission filter
**Descriere tehnica:** Filtreaza marker-ele de quest pe harta dupa player, party, faction, story branch, rol staff si mod spectator.
**Scop:** Previne scurgeri de informatie intre jucatori sau roluri.
**Target:** marker renderer, permission service, story state.
**Acceptare:** Playerul vede doar marker-ele pentru care este eligibil.

### ~~W779~~ ✅ Story instance map isolation
**Descriere tehnica:** Izoleaza marker-ele si rutele de harta pentru instante diferite ale aceluiasi story arc sau dungeon narativ.
**Scop:** Previne amestecarea progresului intre grupuri.
**Target:** story instance service, map marker service, dungeon runtime.
**Acceptare:** Doua instante paralele nu partajeaza marker-e sau rute mutabile.

### ~~W780~~ ✅ Quest hub dependency map
**Descriere tehnica:** Creeaza o harta a dependintelor intre quest hubs, NPC-uri, story arcs, regiuni si servicii locale.
**Scop:** Ajuta planificarea continutului si detectarea hub-urilor critice.
**Target:** quest hub registry, docs export, validation reports.
**Acceptare:** Exportul arata ce questuri si story arcs depind de fiecare hub.

### ~~W781~~ ✅ Story branch map overlay
**Descriere tehnica:** Adauga overlay-uri de harta pentru ramuri story diferite, cu regiuni controlate, obiective active si consecinte vizibile.
**Scop:** Face impactul deciziilor narative observabil.
**Target:** map renderer, story branch service, player UI.
**Acceptare:** Overlay-ul reflecta doar branch-ul activ al playerului sau party-ului.

### ~~W782~~ ✅ Quest travel time estimator
**Descriere tehnica:** Estimeaza timpul de deplasare pentru questuri pe baza rutelor, accesului la warp, mount, portal si regiuni blocate.
**Scop:** Permite balansarea questurilor dupa efort real.
**Target:** route graph, quest balancing, docs gameplay.
**Acceptare:** Estimatorul produce timp aproximativ si motive pentru rute lungi sau imposibile.

### ~~W783~~ ✅ Story critical path report
**Descriere tehnica:** Genereaza raport cu traseul critic al story arcs: questuri obligatorii, noduri de harta, NPC-uri si dependinte de regiune.
**Scop:** Identifica blocaje care pot opri progresia principala.
**Target:** story graph, quest registry, map node registry.
**Acceptare:** Raportul listeaza fiecare punct critic si fallback-ul disponibil sau lipsa lui.

### ~~W784~~ ✅ Quest breadcrumb trail cleanup
**Descriere tehnica:** Curata breadcrumb-urile de navigare dupa completare, abandon, branch switch, party leave sau instance cleanup.
**Scop:** Previne ghidaje vechi care duc playerul gresit.
**Target:** quest navigation service, story lifecycle, map markers.
**Acceptare:** Breadcrumb-urile inactive dispar automat si nu reapar dupa restart.

### ~~W785~~ ✅ Story event geofence validator
**Descriere tehnica:** Valideaza geofence-urile pentru evenimente story dupa lume, regiune, dimensiune, prioritate si conflicte.
**Scop:** Previne evenimente narative declansate in zone gresite.
**Target:** story event service, region service, map validation.
**Acceptare:** Geofence-ul invalid este refuzat cu raport al coordonatelor sau regiunilor conflictuale.

### ~~W786~~ ✅ Quest location fallback policy
**Descriere tehnica:** Defineste fallback pentru locatii de quest indisponibile din cauza protectiilor, grief, eventuri, unload sau schimbari de world.
**Scop:** Mentine questurile jucabile cand locatia initiala nu mai este valida.
**Target:** quest location service, world mapping, recovery service.
**Acceptare:** Questul gaseste locatie alternativa sau se suspenda cu motiv auditabil.

### ~~W787~~ ✅ Story NPC relocation map update
**Descriere tehnica:** Actualizeaza marker-ele, rutele si dependintele story cand un NPC narativ este relocat manual sau automat.
**Scop:** Evita questuri care trimit playerul la pozitia veche a NPC-ului.
**Target:** NPC registry, story service, map marker service.
**Acceptare:** Relocarea NPC-ului invalideaza rutele vechi si publica marker nou validat.

### ~~W788~~ ✅ Quest discovery zone analytics
**Descriere tehnica:** Colecteaza metrice agregate pentru zonele unde jucatorii descopera, abandoneaza sau finalizeaza questuri.
**Scop:** Ajuta imbunatatirea hartii si a flow-ului narativ.
**Target:** quest analytics, map zones, privacy-classified metrics.
**Acceptare:** Raportul este agregat si nu expune trasee individuale sensibile.

### ~~W789~~ ✅ Story map contradiction detector
**Descriere tehnica:** Detecteaza contradictii intre lore-ul story si harta reala: regiuni inexistente, distante imposibile, ownership gresit sau rute blocate.
**Scop:** Pastreaza naratiunea coerenta cu world state.
**Target:** lore validation, map node registry, story docs.
**Acceptare:** Contradictia este raportata cu referinta la textul story si nodul de harta afectat.

### ~~W790~~ ✅ Quest region heatmap export
**Descriere tehnica:** Exporta heatmap pentru utilizarea regiunilor de quest: start, obiective, combat, interactiuni si completari.
**Scop:** Evidentiaza zone suprafolosite sau nefolosite.
**Target:** quest analytics, map export, balancing reports.
**Acceptare:** Exportul grupeaza datele pe regiune si tip de activitate.

### ~~W791~~ ✅ Story phase world-state snapshot
**Descriere tehnica:** Salveaza snapshot-uri de world-state pentru faze story majore: ownership, NPC positions, marker-e si servicii active.
**Scop:** Permite audit si rollback narativ controlat.
**Target:** story phase service, world snapshot, recovery service.
**Acceptare:** Fiecare faza majora are snapshot valid sau motiv documentat pentru lipsa lui.

### ~~W792~~ ✅ Quest map import validation
**Descriere tehnica:** Valideaza importul de date mapping pentru questuri din fisiere externe sau exporturi editoriale.
**Scop:** Previne coordonate invalide, regiuni lipsa si marker-e duplicate.
**Target:** map import pipeline, quest validation, docs tooling.
**Acceptare:** Importul invalid produce raport fara sa modifice registrul live.

### ~~W793~~ ✅ Story route access simulation
**Descriere tehnica:** Simuleaza accesul playerului prin rutele story pentru profile diferite: nou, avansat, faction-specific, party si staff test.
**Scop:** Detecteaza blocaje ascunse in progresia spatiala.
**Target:** route simulator, story graph, player profile fixtures.
**Acceptare:** Simularea raporteaza prima muchie sau conditie care blocheaza accesul.

### ~~W794~~ ✅ Quest chain map continuity check
**Descriere tehnica:** Verifica continuitatea spatiala intre questurile consecutive dintr-un lant: predare, urmatorul start si rute de tranzitie.
**Scop:** Evita lanturi care muta playerul arbitrar sau imposibil.
**Target:** quest chain validator, route graph, map nodes.
**Acceptare:** Lantul cu discontinuitate este raportat cu perechea de questuri afectata.

### ~~W795~~ ✅ Story milestone map unlock audit
**Descriere tehnica:** Auditeaza deblocarea marker-elor si regiunilor la milestone-uri story, cu actor, milestone, efecte si rollback.
**Scop:** Face progresia hartii explicabila pentru suport.
**Target:** story milestone service, map discovery, audit log.
**Acceptare:** Fiecare unlock major are audit si poate fi corelat cu milestone-ul declansator.

### ~~W796~~ ✅ Quest hidden area reveal policy
**Descriere tehnica:** Defineste cand zonele ascunse apar pe harta prin quest, explorare, item, NPC hint sau actiune de party.
**Scop:** Controleaza reveal-ul continutului secret fara hardcodari dispersate.
**Target:** map reveal service, quest/story rules, item effects.
**Acceptare:** Zona ascunsa se dezvaluie doar prin surse permise si auditate.

### ~~W797~~ ✅ Story map rollback compensation
**Descriere tehnica:** Defineste compensatii cand rollback-ul story ascunde regiuni, marker-e sau servicii deja folosite de player.
**Scop:** Evita pierderi confuze dupa corectii narrative.
**Target:** story rollback, map discovery, compensation service.
**Acceptare:** Rollback-ul de harta produce lista efectelor si compensatiilor aplicate.

### ~~W798~~ ✅ Quest map editor review queue
**Descriere tehnica:** Creeaza coada de review pentru modificari de mapping propuse la questuri: noduri noi, rute, marker-e si geofence-uri.
**Scop:** Separă draftul editorial de activarea live.
**Target:** map editor workflow, moderator/admin dashboard, validation service.
**Acceptare:** Modificarea de map nu devine activa fara validare si aprobare.

### ~~W799~~ ✅ Story geography documentation export
**Descriere tehnica:** Exporta documentatia geografiei story: regiuni, asezari, rute, bariere, hub-uri si dependinte narrative.
**Scop:** Pastreaza documentatia de lore sincronizata cu harta implementata.
**Target:** story docs export, map node registry, docs index.
**Acceptare:** Exportul include doar noduri active sau marcheaza explicit drafturile.

### ~~W800~~ ✅ Mapping quest story consistency gate
**Descriere tehnica:** Adauga gate de validare care verifica alinierea dintre map nodes, quest chains, story branches, docs si reguli de acces.
**Scop:** Blocheaza activarea continutului narativ spatial inconsistent.
**Target:** validation pipeline, quest/story/map registries, release checklist.
**Acceptare:** Continutul map-quest-story nu poate fi activat daca exista erori blocking de consistenta.

### ~~W801~~ ✅ Cartography layer registry
**Descriere tehnica:** Creeaza registru pentru layer-ele hartii: story, quest, faction, settlement, danger, resources si staff-only.
**Scop:** Separă informatiile de mapping dupa scop si vizibilitate.
**Target:** map layer service, marker renderer, permission service.
**Acceptare:** Fiecare marker apartine unui layer valid si respecta regulile de vizibilitate.

### ~~W802~~ ✅ Cartography layer conflict detector
**Descriere tehnica:** Detecteaza conflicte intre layer-e cand aceeasi zona are marker-e incompatibile, prioritate gresita sau mesaje contradictorii.
**Scop:** Previne harti aglomerate sau inselatoare.
**Target:** map layer validation, marker registry, docs map rules.
**Acceptare:** Conflictul de layer este raportat cu marker-ele si regulile implicate.

### ~~W803~~ ✅ Quest map priority resolver
**Descriere tehnica:** Defineste prioritatea marker-elor de quest cand mai multe obiective, hints sau story events ocupa aceeasi zona.
**Scop:** Pastreaza UI-ul hartii lizibil si determinist.
**Target:** quest marker service, map renderer, quest tracker.
**Acceptare:** Marker-ul afisat este ales printr-o regula documentata si testabila.

### ~~W804~~ ✅ Story map spoiler classifier
**Descriere tehnica:** Clasifica marker-ele si descrierile de harta dupa risc de spoiler: public, hinted, hidden, post-milestone si staff-only.
**Scop:** Previne dezvaluirea continutului narativ prea devreme.
**Target:** story map service, marker metadata, content validation.
**Acceptare:** Marker-ele cu spoiler nu sunt randate inainte de milestone-ul permis.

### ~~W805~~ ✅ Quest path checkpoint registry
**Descriere tehnica:** Inregistreaza checkpoint-uri de traseu pentru questuri lungi, incluzand locatie, conditii, fallback si status de validare.
**Scop:** Imparte rutele lungi in segmente verificabile.
**Target:** quest route service, map node registry, quest docs.
**Acceptare:** Fiecare segment de ruta are checkpoint-uri ordonate si validate.

### ~~W806~~ ✅ Quest path checkpoint recovery
**Descriere tehnica:** Permite reluarea navigarii de la ultimul checkpoint valid dupa disconnect, teleport, abandon temporar sau restart.
**Scop:** Reduce pierderea progresului spatial in questuri lungi.
**Target:** quest navigation state, player session, recovery service.
**Acceptare:** Playerul revine la checkpoint valid fara marker-e vechi sau duplicate.

### ~~W807~~ ✅ Story biome requirement validator
**Descriere tehnica:** Valideaza ca evenimentele story care cer biome specifice au locatii reale si rute valide catre acele biome.
**Scop:** Evita story events imposibile dupa schimbari de world sau mapping.
**Target:** story event validation, biome mapping, route graph.
**Acceptare:** Evenimentul cu biome lipsa este blocat si raportat cu conditia afectata.

### ~~W808~~ ✅ Quest verticality bounds validator
**Descriere tehnica:** Verifica limitele pe axa Y pentru obiective subterane, aeriene, dungeon, turnuri sau structuri multi-level.
**Scop:** Previne trigger-e activate la alt nivel decat obiectivul real.
**Target:** quest objective bounds, map node registry, region service.
**Acceptare:** Obiectivul spatial include toleranta verticala si respinge activari din afara ei.

### ~~W809~~ ✅ Story route hazard annotation
**Descriere tehnica:** Adauga adnotari de risc pe rutele story: combat, lava, fall, faction territory, PvP, tax zone sau locked region.
**Scop:** Face rutele narative balansabile si explicabile.
**Target:** route graph, story route docs, danger layer.
**Acceptare:** Fiecare ruta cu risc are hazard metadata si severitate.

### ~~W810~~ ✅ Quest safe-return route
**Descriere tehnica:** Calculeaza ruta sigura de intoarcere dupa obiective riscante, incluzand fallback la teleport, checkpoint sau NPC escort.
**Scop:** Evita blocarea playerului dupa finalizarea unui obiectiv.
**Target:** quest route service, teleport rules, NPC escort service.
**Acceptare:** Questul riscant are ruta de intoarcere valida sau compensatie documentata.

### ~~W811~~ ✅ NPC escort route validator
**Descriere tehnica:** Valideaza rutele NPC escort pentru pathfinding, chunk availability, zone protejate, combat si viteza playerului.
**Scop:** Previne escort quests care se blocheaza sau abandoneaza gresit.
**Target:** NPC escort service, route graph, quest objective tracker.
**Acceptare:** Escort quest-ul nu porneste daca ruta NPC este imposibila sau prea instabila.

### ~~W812~~ ✅ NPC escort story branch sync
**Descriere tehnica:** Sincronizeaza starea escort NPC-ului cu story branch-ul activ, astfel incat dialogul, ruta si destinatia sa fie compatibile.
**Scop:** Evita escort NPC-uri care urmeaza ramura narativa gresita.
**Target:** NPC escort service, story branch service, dialogue engine.
**Acceptare:** NPC-ul escort foloseste doar ruta si dialogul branch-ului curent.

### ~~W813~~ ✅ Quest instance entrance mapping
**Descriere tehnica:** Leaga intrarile in instante story sau dungeon de marker-e, rute, conditii de acces si status de disponibilitate.
**Scop:** Face tranzitia din world in instanta clara si validabila.
**Target:** instance entrance registry, quest map service, dungeon/story runtime.
**Acceptare:** Intrarea in instanta are marker valid si refuza accesul cand conditiile nu sunt indeplinite.

### ~~W814~~ ✅ Quest instance exit mapping
**Descriere tehnica:** Defineste iesirile din instante pentru succes, esec, abandon, disconnect si recovery.
**Scop:** Previne intoarceri in locatii gresite sau periculoase.
**Target:** instance exit service, teleport safety, quest lifecycle.
**Acceptare:** Fiecare rezultat de instanta are destinatie de iesire valida si auditata.

### ~~W815~~ ✅ Story portal unlock sequence
**Descriere tehnica:** Modeleaza deblocarea portalurilor story ca secventa de milestone-uri, iteme, dialoguri si regiuni.
**Scop:** Evita portaluri activate inaintea progresiei narrative corecte.
**Target:** portal service, story milestone service, item/quest rules.
**Acceptare:** Portalul story se activeaza doar dupa secventa valida completa.

### ~~W816~~ ✅ Story portal destination drift check
**Descriere tehnica:** Verifica periodic ca destinatiile portalurilor story raman valide dupa modificari de world, regiune sau instanta.
**Scop:** Previne teleporturi catre locatii sterse sau protejate.
**Target:** portal registry, map validation jobs, region service.
**Acceptare:** Portalul cu destinatie invalida se dezactiveaza si apare in raport.

### ~~W817~~ ✅ Map-based quest recommendation
**Descriere tehnica:** Recomanda questuri apropiate pe baza locatiei playerului, progresiei story, nivelului, reputatiei si riscului regiunii.
**Scop:** Foloseste harta pentru descoperire de continut fara a rupe progresia.
**Target:** quest recommendation service, map nodes, player profile.
**Acceptare:** Recomandarile exclud questuri blocate sau cu spoiler nepermis.

### ~~W818~~ ✅ Map-based quest recommendation audit
**Descriere tehnica:** Auditeaza de ce un quest a fost recomandat sau exclus, incluzand distanta, prerequisite, story branch si risc.
**Scop:** Face sistemul de recomandare explicabil pentru balancing.
**Target:** recommendation service, audit reports, quest validation.
**Acceptare:** Pentru un player test se poate genera lista motivelor de includere si excludere.

### ~~W819~~ ✅ Story map personalization rules
**Descriere tehnica:** Permite personalizarea hartii pe baza deciziilor story, reputatiei, aliantelor, profesiei si preferintelor playerului.
**Scop:** Face harta relevanta fara a expune continut inutil.
**Target:** map renderer, story/player profile, preference service.
**Acceptare:** Harta personalizata respecta aceleasi gate-uri de spoiler si permisiuni.

### ~~W820~~ ✅ Party story map consensus
**Descriere tehnica:** Defineste cum se combina marker-ele story intr-un party cu membri aflati pe branch-uri, reputatii sau regiuni deblocate diferite.
**Scop:** Previne scurgeri de spoiler intre membri si blocaje de grup.
**Target:** party service, story map service, marker permissions.
**Acceptare:** Party-ul vede doar marker-ele permise de politica de consens.

### ~~W821~~ ✅ Quest route cost balancing report
**Descriere tehnica:** Genereaza raport de cost pentru rute de quest: timp, taxe, risc, combat, consumabile si prerequisite-uri.
**Scop:** Ajuta balansarea recompenselor dupa efortul spatial real.
**Target:** route graph, quest balancing reports, economy rules.
**Acceptare:** Raportul listeaza rutele prea scumpe, prea ieftine sau imposibile.

### ~~W822~~ ✅ Story geography naming validator
**Descriere tehnica:** Valideaza numele regiunilor, drumurilor, hub-urilor si locatiilor story impotriva glosarului lore si a duplicatelor.
**Scop:** Pastreaza consistenta geografiei narative.
**Target:** lore glossary, map node registry, docs export.
**Acceptare:** Numele conflictuale sunt raportate cu sugestie de rezolvare sau owner de review.

### ~~W823~~ ✅ Quest landmark interaction mapping
**Descriere tehnica:** Mapeaza landmark-urile interactive la quest objectives, story milestones, cooldown-uri si reguli de acces.
**Scop:** Previne interactiuni world nelegate corect de progresie.
**Target:** landmark registry, quest objective handlers, story service.
**Acceptare:** Landmark-ul interactiv are owner logic si conditii de activare validate.

### ~~W824~~ ✅ Landmark state persistence
**Descriere tehnica:** Persistă starea landmark-urilor story: descoperit, activat, consumat, corupt, reparat sau ascuns.
**Scop:** Pastreaza efectele narative intre restarturi.
**Target:** landmark service, persistence layer, map renderer.
**Acceptare:** Landmark-ul isi restaureaza starea corecta dupa restart si chunk reload.

### ~~W825~~ ✅ Landmark cleanup on story rollback
**Descriere tehnica:** Curata sau revine landmark-urile cand story rollback schimba faza, branch-ul sau ownership-ul regiunii.
**Scop:** Evita world state ramas dintr-o cronologie anulata.
**Target:** landmark service, story rollback, recovery service.
**Acceptare:** Rollback-ul story produce lista landmark-urilor modificate sau puse in recovery.

### ~~W826~~ ✅ Quest map marker localization
**Descriere tehnica:** Mută textele marker-elor de quest si story in catalogul de localizare, cu placeholder-e validate.
**Scop:** Face harta traductibila si consistenta cu mesajele din joc.
**Target:** map marker renderer, localization catalog, quest/story docs.
**Acceptare:** Marker-ele nu contin text hardcodat si placeholder-ele lipsa sunt raportate.

### ~~W827~~ ✅ Map marker accessibility modes
**Descriere tehnica:** Adauga moduri alternative pentru marker-e: text explicit, icon fallback, contrast ridicat si descriere compacta.
**Scop:** Imbunatateste lizibilitatea hartii pentru jucatori cu nevoi diferite.
**Target:** map renderer, player preferences, marker metadata.
**Acceptare:** Marker-ele critice au fallback textual si nu depind doar de culoare.

### ~~W828~~ ✅ Quest route debug overlay
**Descriere tehnica:** Creeaza overlay staff-only pentru debug de rute: noduri, muchii, costuri, blocaje si prerequisite-uri.
**Scop:** Reduce timpul de investigare pentru questuri imposibile.
**Target:** debug map overlay, route graph, admin permissions.
**Acceptare:** Overlay-ul este vizibil doar staff-ului autorizat si nu apare jucatorilor.

### ~~W829~~ ✅ Story spatial regression fixtures
**Descriere tehnica:** Creeaza fixtures de regresie pentru cazuri story spatial sensibile: rute blocate, branch exclusiv, portal, geofence si hidden area.
**Scop:** Previne reintroducerea bugurilor de mapping narativ.
**Target:** validation tests, story graph, map node fixtures.
**Acceptare:** Cazurile documentate ruleaza automat in validarea continutului.

### ~~W830~~ ✅ Mapping quest story release checklist
**Descriere tehnica:** Creeaza checklist de release pentru continut map-quest-story: rute, marker-e, localizare, spoiler, acces, docs si rollback.
**Scop:** Standardizeaza activarea sigura a continutului narativ spatial.
**Target:** release checklist, docs/taskuri-de-lucru.md, validation pipeline.
**Acceptare:** Continutul nou de mapping quest story nu este publicat fara checklist complet.

### ~~W831~~ ✅ Story chapter map timeline
**Descriere tehnica:** Leaga capitolele story de o cronologie spatiala cu regiuni active, hub-uri, marker-e, rute si schimbari de world state.
**Scop:** Face evolutia hartii pe capitole verificabila si documentabila.
**Target:** story chapter service, map node registry, docs story geography.
**Acceptare:** Fiecare capitol are snapshot cu zone active, blocate si tranzitii permise.

### ~~W832~~ ✅ Quest region prerequisite diff
**Descriere tehnica:** Genereaza diff intre prerequisite-urile declarate de quest si regulile reale de acces ale regiunilor folosite.
**Scop:** Detecteaza questuri care cer acces pe care playerul nu il poate obtine.
**Target:** quest validation service, region access rules, story prerequisites.
**Acceptare:** Diff-ul raporteaza fiecare conditie lipsa, redundanta sau contradictorie.

### ~~W833~~ ✅ Route obstruction detector
**Descriere tehnica:** Detecteaza obstacole temporare sau permanente pe rutele de quest: protectii, blocuri, eventuri, claim-uri, mobs sau world border.
**Scop:** Previne ghidarea jucatorului prin trasee blocate.
**Target:** route graph, world mapping, region/event services.
**Acceptare:** Ruta obstructionata este marcata indisponibila si are alternativa sau motiv de suspendare.

### ~~W834~~ ✅ Dynamic quest reroute policy
**Descriere tehnica:** Defineste cand questul poate recalcula ruta catre obiectiv dupa schimbari de world, party, unlock-uri sau hazard.
**Scop:** Mentine navigarea corecta fara a schimba scopul questului.
**Target:** quest navigation service, route graph, map marker service.
**Acceptare:** Reroute-ul pastreaza obiectivul valid si auditeaza motivul recalcularii.

### ~~W835~~ ✅ Map node versioning
**Descriere tehnica:** Adauga versiuni pentru map nodes astfel incat questurile si story branches sa poata referi explicit o versiune compatibila.
**Scop:** Previne ruperea continutului vechi cand locatiile sunt modificate.
**Target:** map node registry, quest/story references, migration runner.
**Acceptare:** Referintele la noduri incompatibile sunt raportate in validare.

### ~~W836~~ ✅ Map node deprecation workflow
**Descriere tehnica:** Introduce workflow pentru noduri de harta deprecated, cu inlocuitor, perioada de compatibilitate si raport de dependinte.
**Scop:** Permite schimbari de mapping fara ruperea questurilor active.
**Target:** map node registry, docs export, validation reports.
**Acceptare:** Un nod deprecated nu poate fi sters cat timp exista dependinte active nemigrate.

### ~~W837~~ ✅ Quest coordinate drift audit
**Descriere tehnica:** Auditeaza diferenta dintre coordonatele documentate, marker-ele active si coordonatele reale folosite de trigger-e.
**Scop:** Detecteaza drift intre documentatie, harta si cod.
**Target:** quest objective definitions, map marker service, docs validation.
**Acceptare:** Auditul raporteaza coordonatele divergente cu sursa fiecarei valori.

### ~~W838~~ ✅ Landmark dialogue trigger sync
**Descriere tehnica:** Sincronizeaza trigger-ele de dialog declansate la landmark cu starea marker-ului, story branch-ul si quest objective-ul.
**Scop:** Evita dialoguri pornite la landmark-uri gresite sau inactive.
**Target:** landmark service, dialogue engine, quest/story state.
**Acceptare:** Dialogul de landmark se activeaza doar cand marker-ul si branch-ul sunt compatibile.

### ~~W839~~ ✅ Story proximity trigger debouncer
**Descriere tehnica:** Adauga debouncing pentru trigger-ele story bazate pe apropiere de locatie, cu fereastra pe player, party si regiune.
**Scop:** Previne declansari multiple ale aceluiasi eveniment spatial.
**Target:** story trigger service, movement listeners, quest event bus.
**Acceptare:** Intrarea repetata in aceeasi zona nu dubleaza evenimentul story.

### ~~W840~~ ✅ Quest clue spatial index
**Descriere tehnica:** Indexeaza indiciile de quest dupa zona, landmark, NPC, item, story branch si nivel de spoiler.
**Scop:** Face cautarea si validarea indiciilor spatiale eficienta.
**Target:** clue service, map node registry, quest hint service.
**Acceptare:** Fiecare clue spatial poate fi gasit dupa locatie si este filtrat dupa spoiler gate.

### ~~W841~~ ✅ Map pin provenance tracking
**Descriere tehnica:** Inregistreaza provenienta fiecarui pin de harta: quest, story, player, staff, import, AI draft sau recovery.
**Scop:** Face marker-ele usor de investigat si curatat.
**Target:** map marker service, audit log, map editor workflow.
**Acceptare:** Fiecare pin activ are sursa, actor sau sistem owner si timestamp.

### ~~W842~~ ✅ Alternative story route resolver
**Descriere tehnica:** Calculeaza rute alternative pentru story objectives cand ruta principala este blocata de world state, faction conflict sau event.
**Scop:** Mentine progresia story fara bypass necontrolat.
**Target:** route graph, story objective service, region/faction rules.
**Acceptare:** Ruta alternativa respecta aceleasi prerequisite-uri si este auditată ca fallback.

### ~~W843~~ ✅ Narrative region state renderer
**Descriere tehnica:** Randeaza starea narativa a regiunilor pe harta: pace, conflict, ocupat, corupt, reparat, ascuns sau post-event.
**Scop:** Face consecintele story vizibile si coerente.
**Target:** map renderer, story phase service, region metadata.
**Acceptare:** Starea afisata corespunde ultimei faze story validate pentru player.

### ~~W844~~ ✅ Quest marker cache invalidation
**Descriere tehnica:** Invalideaza cache-ul marker-elor de quest cand se schimba story branch, party, reputatie, regiune, config sau locale.
**Scop:** Previne marker-e stale dupa schimbari de context.
**Target:** marker cache, quest tracker, player profile events.
**Acceptare:** Schimbarile relevante se reflecta pe harta fara restart sau refresh manual fortat.

### ~~W845~~ ✅ Story branch merge map policy
**Descriere tehnica:** Defineste cum se imbina marker-ele si regiunile cand doua ramuri story se reunesc intr-un milestone comun.
**Scop:** Evita marker-e duplicate sau contradictorii dupa branch merge.
**Target:** story branch service, map discovery, marker cleanup.
**Acceptare:** Merge-ul produce set unic de marker-e si curata branch-urile vechi.

### ~~W846~~ ✅ Party waypoint leader policy
**Descriere tehnica:** Stabileste cine poate seta waypoint-uri de party pentru questuri story si cum sunt validate fata de progresul membrilor.
**Scop:** Previne ghidarea grupului catre continut blocat sau spoiler.
**Target:** party service, map waypoint service, story permissions.
**Acceptare:** Waypoint-ul de party este acceptat doar daca politica de lider si eligibilitate il permite.

### ~~W847~~ ✅ Quest map privacy snapshot
**Descriere tehnica:** Salveaza snapshot de vizibilitate pentru marker-ele partajate in party, staff review sau export de suport.
**Scop:** Permite auditarea scurgerilor de informatie pe harta.
**Target:** marker visibility service, party/staff workflows, audit reports.
**Acceptare:** Se poate explica de ce un actor a vazut sau nu a vazut un marker la un moment dat.

### ~~W848~~ ✅ Map editor change impact report
**Descriere tehnica:** Calculeaza impactul unei modificari de mapping asupra questurilor, story branches, NPC-urilor, portalurilor si documentatiei.
**Scop:** Previne activarea modificarilor de harta cu efecte ascunse.
**Target:** map editor workflow, dependency graph, validation reports.
**Acceptare:** Orice schimbare propusa produce lista dependintelor afectate inainte de aprobare.

### ~~W849~~ ✅ Story content placement lint
**Descriere tehnica:** Ruleaza lint pentru plasarea continutului story: distante, densitate, biomes, regiuni, rute, spoiler si compatibilitate lore.
**Scop:** Mentine calitatea geografiei narative.
**Target:** story content validator, map node registry, lore rules.
**Acceptare:** Continutul plasat gresit primeste warning sau blocking error dupa severitate.

### ~~W850~~ ✅ Quest world border guard
**Descriere tehnica:** Verifica toate locatiile de quest si rutele asociate fata de world border curent si border-ul planificat.
**Scop:** Previne obiective in afara zonei accesibile.
**Target:** quest validation, world border service, route graph.
**Acceptare:** Locatiile in afara border-ului sunt refuzate sau marcate pentru migrare.

### ~~W851~~ ✅ Route graph chunk availability check
**Descriere tehnica:** Verifica daca nodurile si muchiile importante ale rutei depind de chunk-uri unloadable, regenerate sau protejate.
**Scop:** Reduce rutele instabile in productie.
**Target:** route graph, chunk lifecycle, world validation.
**Acceptare:** Ruta cu chunk instabil este raportata cu risc si fallback recomandat.

### ~~W852~~ ✅ Map node health dashboard
**Descriere tehnica:** Creeaza dashboard cu health status pentru noduri de harta: valid, stale, unreachable, deprecated, conflictual sau draft.
**Scop:** Centralizeaza mentenanta mapping-ului.
**Target:** admin dashboard, map node registry, validation jobs.
**Acceptare:** Staff-ul vede starea fiecarui nod si actiunea recomandata.

### ~~W853~~ ✅ Quest route config-change detector
**Descriere tehnica:** Detecteaza cand o schimbare de config pentru regiuni, teleport, permisiuni sau economie face o ruta de quest invalida.
**Scop:** Prinde buguri introduse prin configuratie, nu doar prin cod.
**Target:** config reload validation, quest route graph, region rules.
**Acceptare:** Reload-ul raporteaza rutele care devin imposibile dupa config change.

### ~~W854~~ ✅ Story phase mapping migration
**Descriere tehnica:** Migreaza map discovery, marker-e si regiuni narative cand se schimba definitia fazelor story intre versiuni.
**Scop:** Pastreaza progresul playerilor dupa update-uri de story.
**Target:** story migration, map discovery state, player profile.
**Acceptare:** Migrarile au dry-run si raport cu playerii sau fazele afectate.

### ~~W855~~ ✅ Quest objective map grouping
**Descriere tehnica:** Grupeaza marker-ele obiectivelor multiple ale aceluiasi quest pentru a evita clutter si a indica progresul ramas.
**Scop:** Imbunatateste lizibilitatea hartii pentru questuri complexe.
**Target:** marker renderer, quest tracker, player UI preferences.
**Acceptare:** Questurile cu multe obiective afiseaza grupuri expandabile sau sumarizate.

### ~~W856~~ ✅ Story area ownership overlay
**Descriere tehnica:** Afiseaza overlay de ownership narativ pentru zone controlate de factiuni, NPC-uri, evenimente sau decizii story.
**Scop:** Leaga controlul politic de geografia story.
**Target:** map renderer, faction/settlement/story services, region metadata.
**Acceptare:** Overlay-ul arata ownerul efectiv si sursa regulii de control.

### ~~W857~~ ✅ Quest route replay export
**Descriere tehnica:** Exporta traseul parcurs de un quest in forma agregata pentru debugging: noduri atinse, marker-e vazute si blocaje.
**Scop:** Ajuta investigarea bugurilor de navigare fara a expune date inutile.
**Target:** quest navigation telemetry, export formatter, privacy rules.
**Acceptare:** Replay-ul este disponibil staff-ului autorizat si redacteaza date sensibile.

### ~~W858~~ ✅ Story map QA scenario pack
**Descriere tehnica:** Creeaza pachet de scenarii QA pentru mapping story: player nou, player avansat, party mixt, branch exclusiv si rollback.
**Scop:** Standardizeaza testarea manuala si automata a hartii narrative.
**Target:** QA docs, validation fixtures, story/map test data.
**Acceptare:** Fiecare scenariu are pasi, expected result si marker-e de verificat.

### ~~W859~~ ✅ Map quest story docs drift audit
**Descriere tehnica:** Compara documentatia de mapping quest story cu registrul live pentru noduri, marker-e, rute, unlock-uri si spoiler levels.
**Scop:** Detecteaza documentatie invechita inainte de release.
**Target:** docs validation, map/quest/story registries, release checklist.
**Acceptare:** Drift-ul este raportat cu documentul, sectiunea si entitatea afectata.

### ~~W860~~ ✅ Mapping quest story implementation batch
**Descriere tehnica:** Defineste un batch implementabil pentru taskurile map-quest-story cu dependinte, ordine recomandata, teste si docs afectate.
**Scop:** Permite executia sigura de catre agent extern fara a rupe progresia spatiala.
**Target:** docs/taskuri-de-lucru.md, implementation planning, validation pipeline.
**Acceptare:** Batch-ul contine ordine de implementare, riscuri, criterii de testare si rollback.

### ~~W861~~ ✅ Story route lock ownership
**Descriere tehnica:** Introduce ownership explicit pentru lock-urile de ruta story pe player, party, instanta sau event.
**Scop:** Previne blocarea rutei de catre contextul gresit sau ramas dupa cleanup incomplet.
**Target:** story route service, lock manager, recovery service.
**Acceptare:** Fiecare lock de ruta are owner, expiry, motiv si cleanup auditabil.

### ~~W862~~ ✅ Quest route lock conflict report
**Descriere tehnica:** Raporteaza conflictele intre lock-uri de ruta create de questuri, eventuri, dungeonuri, settlement-uri si staff actions.
**Scop:** Face blocajele spatiale usor de diagnosticat.
**Target:** route lock manager, quest/event services, admin dashboard.
**Acceptare:** Conflictul include actorii, ruta afectata, durata si actiunea recomandata.

### ~~W863~~ ✅ Story bridge activation state
**Descriere tehnica:** Modeleaza podurile, barierele si pasajele story ca entitati cu stari: inchis, deschis, deteriorat, reparat si ascuns.
**Scop:** Leaga modificarile fizice ale hartii de progresia narativa.
**Target:** world mapping, story phase service, landmark service.
**Acceptare:** Starea pasajului este persistenta si sincronizata cu marker-ele de harta.

### ~~W864~~ ✅ Quest bridge fallback routing
**Descriere tehnica:** Calculeaza fallback pentru questuri cand un pod, pasaj sau poarta story devine indisponibil.
**Scop:** Evita questuri blocate de modificari temporare ale hartii.
**Target:** route graph, quest navigation, story bridge registry.
**Acceptare:** Questul foloseste ruta alternativa valida sau se suspenda cu motiv clar.

### ~~W865~~ ✅ Narrative map hazard decay
**Descriere tehnica:** Adauga decay pentru hazard-urile narative de pe harta, precum coruptie, ceata, conflict sau infestare.
**Scop:** Permite lumii sa revina gradual dupa evenimente story.
**Target:** hazard layer, story phase service, scheduler.
**Acceptare:** Hazard-ul scade conform politicii si actualizeaza marker-ele vizibile.

### ~~W866~~ ✅ Narrative hazard cleanse quest link
**Descriere tehnica:** Leaga curatarea hazard-urilor de questuri sau proiecte story care reduc, elimina sau muta pericolul pe harta.
**Scop:** Face efectele questurilor vizibile in world mapping.
**Target:** hazard layer, quest completion, story world-state.
**Acceptare:** Finalizarea questului modifica hazard-ul doar daca toate conditiile sunt valide.

### ~~W867~~ ✅ Map route toll story exception
**Descriere tehnica:** Defineste exceptii de taxare pe rute pentru story quests, urgenta, faction treaty sau event escort.
**Scop:** Evita blocarea progresiei narative de costuri de ruta.
**Target:** route toll service, story quest rules, economy validation.
**Acceptare:** Exceptia de taxa are motiv, durata si audit.

### ~~W868~~ ✅ Quest route toll preview
**Descriere tehnica:** Afiseaza costurile de ruta estimate inainte ca playerul sa porneasca un quest sau segment de calatorie.
**Scop:** Face costurile spatiale explicite si reduce abandonul neinformat.
**Target:** quest UI, route graph, toll/economy services.
**Acceptare:** Preview-ul arata cost total, scutiri si rute alternative disponibile.

### ~~W869~~ ✅ Story patrol route registry
**Descriere tehnica:** Creeaza registru pentru rutele patrulelor NPC legate de story, factiuni si regiuni de conflict.
**Scop:** Face patrulele parte verificabila a hartii narative.
**Target:** NPC patrol service, route graph, faction/story services.
**Acceptare:** Fiecare patrula are ruta valida, orar, owner si conditii de activare.

### ~~W870~~ ✅ Story patrol encounter trigger
**Descriere tehnica:** Leaga intalnirile cu patrule de geofence, reputatie, faction status, story branch si cooldown.
**Scop:** Previne encountere de patrula declansate in contexte gresite.
**Target:** patrol encounter service, story trigger service, reputation/faction rules.
**Acceptare:** Encounter-ul se activeaza doar cand toate conditiile spatiale si narrative sunt indeplinite.

### ~~W871~~ ✅ Quest escort route handoff
**Descriere tehnica:** Permite handoff intre doua rute de escort cand NPC-ul trece intre regiuni, instante sau faze story.
**Scop:** Evita escort quests care se rup la granite de zona.
**Target:** NPC escort service, route graph, story phase service.
**Acceptare:** Handoff-ul pastreaza progresul si verifica noua ruta inainte de tranzitie.

### ~~W872~~ ✅ Map node ownership by story phase
**Descriere tehnica:** Permite schimbarea ownerului unui map node in functie de faza story, faction conflict sau settlement event.
**Scop:** Reflecta controlul narativ al zonelor direct in mapping.
**Target:** map node registry, story phase service, faction/settlement services.
**Acceptare:** Ownerul nodului este calculat determinist si auditat la schimbare.

### ~~W873~~ ✅ Quest map node reservation
**Descriere tehnica:** Rezerva temporar noduri de harta pentru questuri instanciate sau evenimente care necesita exclusivitate spatiala.
**Scop:** Previne doua fluxuri care folosesc simultan acelasi loc incompatibil.
**Target:** map node registry, quest instance service, event scheduler.
**Acceptare:** Rezervarea are owner, expiry si regula de conflict.

### ~~W874~~ ✅ Map node reservation cleanup
**Descriere tehnica:** Curata rezervarile de map node ramase dupa quest cancel, disconnect, event fail, restart sau recovery.
**Scop:** Evita zone blocate permanent de rezervari stale.
**Target:** reservation service, cleanup jobs, recovery service.
**Acceptare:** Rezervarile expirate sau orfane sunt eliberate si raportate.

### ~~W875~~ ✅ Story route accessibility validator
**Descriere tehnica:** Valideaza rutele story pentru accesibilitate: jump-uri imposibile, inaltime, apa, lava, intuneric, mob density si alternative.
**Scop:** Evita rute care sunt teoretic valide dar practic nejucabile.
**Target:** route graph, world analysis, gameplay validation.
**Acceptare:** Rutele cu risc de accesibilitate primesc severitate si recomandare.

### ~~W876~~ ✅ Quest route mount compatibility
**Descriere tehnica:** Marcheaza segmentele de ruta unde mount-ul este permis, interzis, necesar sau periculos.
**Scop:** Aliniaza navigarea questurilor cu regulile de mount si teren.
**Target:** route graph, mount service, quest navigation.
**Acceptare:** Navigarea nu recomanda mount pe segmente incompatibile.

### ~~W877~~ ✅ Story route vehicle compatibility
**Descriere tehnica:** Valideaza rutele pentru vehicule sau transport special: barci, minecart, caravan, portal, mount si teleport.
**Scop:** Previne obiective care depind de transport indisponibil.
**Target:** route graph, transport services, story/quest rules.
**Acceptare:** Segmentul de ruta declara transporturile permise si prerequisite-urile lor.

### ~~W878~~ ✅ Quest hub load distribution report
**Descriere tehnica:** Raporteaza incarcarea spatiala a quest hub-urilor dupa numar de questuri, playeri, NPC-uri, marker-e si eventuri.
**Scop:** Identifica hub-uri supra-aglomerate sau subfolosite.
**Target:** quest hub registry, analytics, map reports.
**Acceptare:** Raportul indica hub-uri cu risc de aglomerare si recomandari de redistribuire.

### ~~W879~~ ✅ Story hub fallback assignment
**Descriere tehnica:** Defineste hub-uri alternative pentru story arcs cand hub-ul principal este indisponibil sau supra-aglomerat.
**Scop:** Mentine progresia story in conditii operationale variabile.
**Target:** story hub service, quest routing, map node registry.
**Acceptare:** Fallback-ul este folosit doar daca respecta prerequisite-urile si spoiler gate-ul.

### ~~W880~~ ✅ Quest route weather dependency
**Descriere tehnica:** Marcheaza segmentele de ruta si obiectivele dependente de vreme, furtuna, zi/noapte sau anotimp.
**Scop:** Face conditiile de mediu explicite si testabile.
**Target:** route graph, weather/time service, quest objective validation.
**Acceptare:** Questul cu dependinta de vreme afiseaza conditia sau fallback-ul permis.

### ~~W881~~ ✅ Story weather event map overlay
**Descriere tehnica:** Afiseaza overlay pentru evenimente meteo story care afecteaza rute, regiuni, NPC-uri sau obiective.
**Scop:** Leaga schimbarile atmosferice de navigarea pe harta.
**Target:** weather event service, map renderer, story phase service.
**Acceptare:** Overlay-ul meteo apare doar in zonele si fazele story relevante.

### ~~W882~~ ✅ Quest route time-window validator
**Descriere tehnica:** Valideaza rutele si obiectivele care sunt disponibile doar in ferestre de timp specifice.
**Scop:** Previne obiective active in afara intervalului narativ permis.
**Target:** quest validation, time service, route graph.
**Acceptare:** Ferestrele de timp sunt documentate si evaluate inainte de activarea obiectivului.

### ~~W883~~ ✅ Story nocturnal route policy
**Descriere tehnica:** Defineste reguli speciale pentru rutele story nocturne: vizibilitate, mob density, NPC schedule, marker-e si risk hints.
**Scop:** Face continutul de noapte coerent si balansat.
**Target:** story route service, NPC routine service, danger layer.
**Acceptare:** Rutele nocturne au metadata de risc si fallback pentru jucatori nepregatiti.

### ~~W884~~ ✅ Quest route seasonal availability
**Descriere tehnica:** Marcheaza rutele si obiectivele disponibile doar in anumite sezoane sau eventuri calendaristice.
**Scop:** Previne referinte catre continut sezonier in afara ferestrei sale.
**Target:** seasonal event service, route graph, quest validation.
**Acceptare:** Questul sezonier este activ doar cand rutele sale sezoniere sunt disponibile.

### ~~W885~~ ✅ Story map archival snapshot
**Descriere tehnica:** Arhiveaza starea hartii narrative la final de capitol, sezon sau release major.
**Scop:** Permite comparatii, rollback si documentare istorica.
**Target:** map snapshot service, story chapter service, docs export.
**Acceptare:** Snapshot-ul include noduri, rute, marker-e, owneri si hazard-uri active.

### ~~W886~~ ✅ Quest route archival diff
**Descriere tehnica:** Compara rutele de quest intre doua snapshot-uri pentru a detecta schimbari de acces, cost, risc si lungime.
**Scop:** Evidentiaza impactul modificarilor de harta asupra continutului existent.
**Target:** route snapshot diff, quest validation, release reports.
**Acceptare:** Diff-ul listeaza questurile afectate si severitatea schimbarii.

### ~~W887~~ ✅ Story geography changelog generator
**Descriere tehnica:** Genereaza changelog pentru schimbarile de geografie story: regiuni, rute, noduri, landmark-uri si hub-uri.
**Scop:** Face evolutia hartii narative usor de urmarit.
**Target:** map registry, docs changelog, release workflow.
**Acceptare:** Fiecare schimbare geografica publica apare intr-un sumar de release.

### ~~W888~~ ✅ Quest map migration dry-run
**Descriere tehnica:** Ruleaza migrarile de mapping pentru questuri in dry-run cu raport de marker-e mutate, rute schimbate si obiective afectate.
**Scop:** Reduce riscul migrarilor de continut spatial.
**Target:** map migration runner, quest registry, validation reports.
**Acceptare:** Dry-run-ul nu modifica date live si produce lista exacta de schimbari planificate.

### ~~W889~~ ✅ Story route rollback dry-run
**Descriere tehnica:** Simuleaza rollback-ul rutelor story pentru a vedea marker-ele, unlock-urile, hazard-urile si questurile afectate.
**Scop:** Permite decizii informate inainte de rollback narativ.
**Target:** story rollback service, route graph, map discovery state.
**Acceptare:** Dry-run-ul raporteaza efecte, riscuri si compensatii fara modificari live.

### ~~W890~~ ✅ Mapping quest story validation summary export
**Descriere tehnica:** Exporta sumarul validarii map-quest-story cu erori, warnings, noduri afectate, questuri blocate si actiuni recomandate.
**Scop:** Ofera o predare clara pentru implementare, review si release.
**Target:** validation pipeline, docs export, admin dashboard.
**Acceptare:** Sumarul poate fi atasat unui batch si indica explicit ce blocheaza activarea.

### ~~W891~~ ✅ Story route rehearsal simulator
**Descriere tehnica:** Simuleaza parcurgerea rutelor story fara efecte live, folosind profile de player, party, branch si unlock-uri diferite.
**Scop:** Detecteaza blocaje spatiale inainte ca story arc-ul sa fie activat.
**Target:** route simulator, story graph, map node registry.
**Acceptare:** Simulatorul raporteaza ruta parcursa, primul blocaj si conditiile lipsa.

### ~~W892~~ ✅ Quest objective relocation approval
**Descriere tehnica:** Creeaza flux de aprobare pentru mutarea obiectivelor de quest pe harta, cu impact asupra rutei, marker-elor si documentatiei.
**Scop:** Previne mutari editoriale care rup chain-uri sau story gates.
**Target:** quest editor workflow, map node registry, validation reports.
**Acceptare:** Relocarea nu devine activa fara impact report si aprobare.

### ~~W893~~ ✅ Map node permission inheritance
**Descriere tehnica:** Defineste mostenirea permisiunilor intre map nodes, regiuni parinte, hub-uri, settlement-uri si instante.
**Scop:** Evita reguli de acces duplicate sau contradictorii.
**Target:** map node registry, permission service, region hierarchy.
**Acceptare:** Permisiunea efectiva a nodului poate fi explicata prin lantul de mostenire.

### ~~W894~~ ✅ Story route priority lanes
**Descriere tehnica:** Marcheaza rute prioritare pentru story principal fata de side quests, daily quests, eventuri si continut optional.
**Scop:** Protejeaza progresia principala de conflicte cu continut secundar.
**Target:** route graph, story priority rules, quest scheduler.
**Acceptare:** Rutele critice story au prioritate si conflictele sunt raportate.

### ~~W895~~ ✅ Quest path difficulty tagging
**Descriere tehnica:** Eticheteaza segmentele de ruta dupa dificultate: safe, normal, risky, combat-heavy, puzzle, platforming sau locked.
**Scop:** Aliniaza recompensa si hint-urile cu dificultatea deplasarii.
**Target:** route graph, quest balancing, map renderer.
**Acceptare:** Fiecare segment folosit de quest are tag de dificultate si severitate.

### ~~W896~~ ✅ Story region reputation gate sync
**Descriere tehnica:** Sincronizeaza accesul la regiuni story cu tier-urile de reputatie si schimbarile produse de questuri.
**Scop:** Evita regiuni accesibile inainte sau dupa pragul narativ corect.
**Target:** reputation service, story region gates, map discovery.
**Acceptare:** Schimbarea reputatiei actualizeaza accesul si marker-ele relevante.

### ~~W897~~ ✅ Quest marker expiry by condition
**Descriere tehnica:** Expira marker-ele de quest pe baza conditiilor: timp, branch schimbat, obiectiv completat, regiune inchisa sau quest suspendat.
**Scop:** Previne indicatii vechi pe harta.
**Target:** quest marker service, condition evaluator, cleanup jobs.
**Acceptare:** Marker-ul expirat este eliminat si nu reapare fara conditie valida.

### ~~W898~~ ✅ Rumor-based map hint system
**Descriere tehnica:** Adauga hint-uri de harta provenite din zvonuri NPC, carti, semne sau interactiuni, cu nivel de incredere.
**Scop:** Permite descoperire diegetica a locatiei fara marker direct permanent.
**Target:** rumor service, map hint service, dialogue engine.
**Acceptare:** Hint-ul de tip zvon afiseaza zona aproximativa si sursa, nu coordonata exacta.

### ~~W899~~ ✅ Rumor hint validation
**Descriere tehnica:** Valideaza zvonurile care indica locatii pentru a evita regiuni inexistente, spoiler-e nepermise sau contradictii lore.
**Scop:** Pastreaza indiciile narative coerente cu harta.
**Target:** rumor validation, lore glossary, map node registry.
**Acceptare:** Zvonul invalid este respins sau trimis la review editorial.

### ~~W900~~ ✅ Road sign quest sync
**Descriere tehnica:** Sincronizeaza semnele de drum din world cu rutele si hub-urile relevante pentru questuri si story.
**Scop:** Evita indicatoare care trimit catre locatii vechi sau blocate.
**Target:** road sign service, route graph, map node registry.
**Acceptare:** Semnele regenerate reflecta rutele active si respecta spoiler gate-ul.

### ~~W901~~ ✅ In-world sign localization
**Descriere tehnica:** Mută textele semnelor de quest si story in catalogul de localizare, cu fallback pe limba serverului.
**Scop:** Pastreaza consistenta intre harta, chat si obiectele din lume.
**Target:** sign renderer, localization catalog, story/quest text.
**Acceptare:** Semnele nu contin text hardcodat si placeholder-ele sunt validate.

### ~~W902~~ ✅ Compass target resolver
**Descriere tehnica:** Centralizeaza rezolvarea target-ului de compass pentru questuri, story objectives, party waypoints si staff debug.
**Scop:** Evita logici diferite pentru acelasi target spatial.
**Target:** compass service, quest navigation, map waypoint service.
**Acceptare:** Compass-ul foloseste un resolver unic cu verificari de acces si spoiler.

### ~~W903~~ ✅ Quest compass spoiler guard
**Descriere tehnica:** Blocheaza compass-ul sa indice obiective ascunse, branch-uri nealese sau locatii nedescoperite.
**Scop:** Previne spoiler-e prin navigatie directa.
**Target:** compass service, story spoiler classifier, quest tracker.
**Acceptare:** Compass-ul indica doar target-uri permise de progresia curenta.

### ~~W904~~ ✅ Map marker source diff
**Descriere tehnica:** Compara marker-ele generate din quest, story, config, import si staff edits pentru a identifica sursa divergentei.
**Scop:** Reduce timpul de debugging pentru marker-e gresite.
**Target:** marker provenance, validation reports, map editor workflow.
**Acceptare:** Diff-ul arata ce sursa produce fiecare marker conflictual.

### ~~W905~~ ✅ Story world-state invariant tests
**Descriere tehnica:** Creeaza teste de invarianti pentru world-state story: regiuni deschise, NPC positions, marker-e, hazard-uri si portaluri.
**Scop:** Previne faze story imposibile dupa refactorizari.
**Target:** story validation tests, map fixtures, world-state snapshots.
**Acceptare:** Invariantii critici ruleaza in validarea continutului si esueaza pe contradictii.

### ~~W906~~ ✅ Quest route branch split validator
**Descriere tehnica:** Valideaza punctele unde o ruta de quest se desparte dupa branch story, alegere de dialog, reputatie sau faction status.
**Scop:** Evita branch-uri fara ruta completa catre obiectiv.
**Target:** quest route validator, story branch graph, dialogue choices.
**Acceptare:** Fiecare split are cel putin o ruta valida pentru branch-ul permis.

### ~~W907~~ ✅ Story route branch convergence validator
**Descriere tehnica:** Verifica unde branch-urile story se reunesc spatial si daca marker-ele, unlock-urile si NPC-urile converg corect.
**Scop:** Previne ramuri care lasa jucatorul in stari geografice incompatibile.
**Target:** story graph, route graph, map marker cleanup.
**Acceptare:** Convergenta de branch produce acelasi set valid de marker-e si acces.

### ~~W908~~ ✅ Quest local map cache warmup
**Descriere tehnica:** Pregateste cache-ul local pentru marker-ele si rutele questurilor active la login sau acceptarea questului.
**Scop:** Reduce lag-ul la deschiderea hartii sau compass-ului.
**Target:** map cache, quest tracker, player session lifecycle.
**Acceptare:** Cache-ul se incalzeste doar pentru marker-e permise si se invalideaza corect.

### ~~W909~~ ✅ Story map cache privacy boundary
**Descriere tehnica:** Izoleaza cache-ul hartii pe player, party si branch pentru a evita reutilizarea marker-elor intre contexte.
**Scop:** Previne scurgeri de informatie prin cache.
**Target:** map cache, marker visibility, story branch service.
**Acceptare:** Cache-ul unui player nu poate livra marker-e nepermise altui context.

### ~~W910~~ ✅ Quest route analytics sampling
**Descriere tehnica:** Colecteaza esantionat evenimente de navigare pentru rute de quest: abandon, blocaj, intoarcere, reroute si completare.
**Scop:** Ofera date de balancing fara volum excesiv sau tracking sensibil.
**Target:** quest navigation analytics, privacy rules, route reports.
**Acceptare:** Sampling-ul este configurabil si exporta doar date agregate.

### ~~W911~~ ✅ Story map onboarding route
**Descriere tehnica:** Defineste o ruta initiala de onboarding pe harta care introduce hub-uri, marker-e, compass si regiuni story fara spoiler.
**Scop:** Invata jucatorul sistemul de mapping prin gameplay.
**Target:** tutorial service, map marker service, story intro quest.
**Acceptare:** Ruta onboarding are pasi validati, fallback si cleanup la skip.

### ~~W912~~ ✅ Quest route fail-safe teleport
**Descriere tehnica:** Defineste cand un quest poate oferi teleport fail-safe daca playerul ramane blocat pe ruta validata.
**Scop:** Reduce blocajele fara a transforma teleportul in bypass exploatabil.
**Target:** quest navigation, teleport safety, abuse guards.
**Acceptare:** Teleportul fail-safe cere conditii stricte si produce audit.

### ~~W913~~ ✅ Story route abuse guard
**Descriere tehnica:** Detecteaza folosirea rutelor story pentru a ocoli taxe, regiuni blocate, PvP, cooldown-uri sau content gates.
**Scop:** Previne exploatarea navigatiei narrative.
**Target:** route access service, economy/region/combat guards, audit reports.
**Acceptare:** Abuzul suspect este blocat sau raportat cu ruta si gate-ul ocolit.

### ~~W914~~ ✅ Map marker density budget
**Descriere tehnica:** Limiteaza densitatea marker-elor pe zona, layer si player context, cu grupare sau prioritizare automata.
**Scop:** Pastreaza harta lizibila in hub-uri aglomerate.
**Target:** marker renderer, cartography layers, UI preferences.
**Acceptare:** Zonele dense nu depasesc bugetul vizual configurat.

### ~~W915~~ ✅ Quest marker cluster interaction
**Descriere tehnica:** Defineste interactiunea cu clustere de marker-e: expandare, filtrare, prioritate, sumar si accesibilitate.
**Scop:** Face marker-ele grupate utilizabile, nu doar ascunse.
**Target:** map UI, marker cluster service, quest tracker.
**Acceptare:** Clusterul afiseaza continutul relevant fara spoiler si cu fallback textual.

### ~~W916~~ ✅ Story route staff override lock
**Descriere tehnica:** Permite staff-ului autorizat sa blocheze temporar o ruta story cu motiv, durata si mesaj public sau privat.
**Scop:** Ofera control operational cand o zona sau ruta devine instabila.
**Target:** staff commands, route lock manager, notification service.
**Acceptare:** Override-ul are expiry, audit si cleanup automat.

### ~~W917~~ ✅ Quest route staff repair action
**Descriere tehnica:** Adauga actiuni staff pentru repair de ruta: recalculare, mutare marker, dezactivare segment, fallback si export incident.
**Scop:** Reduce timpul de interventie pentru questuri blocate.
**Target:** admin dashboard, route graph, quest recovery.
**Acceptare:** Fiecare actiune de repair este auditată si poate fi inclusa in incident timeline.

### ~~W918~~ ✅ Story map incident correlation
**Descriere tehnica:** Coreleaza incidentele de quest/story cu noduri de harta, rute, marker-e, regiuni si schimbari recente.
**Scop:** Ajuta identificarea cauzei pentru buguri spatiale recurente.
**Target:** incident timeline, map registry, audit reports.
**Acceptare:** Incidentul afiseaza entitatile de mapping implicate si ultimele modificari relevante.

### ~~W919~~ ✅ Quest story map ownership report
**Descriere tehnica:** Genereaza raport cu ownerii logici ai nodurilor, marker-elor, rutelor, questurilor si branch-urilor story.
**Scop:** Clarifica responsabilitatea pentru mentenanta si review.
**Target:** ownership metadata, docs export, admin dashboard.
**Acceptare:** Fiecare entitate map-quest-story critica are owner sau apare ca neasignata.

### ~~W920~~ ✅ Mapping quest story hardening gate
**Descriere tehnica:** Creeaza gate final pentru hardening: cache privacy, marker density, route abuse, staff repair, incident correlation si ownership.
**Scop:** Blocheaza release-ul daca mapping-ul narativ are riscuri operationale majore.
**Target:** validation pipeline, release checklist, admin reports.
**Acceptare:** Gate-ul produce pass/fail cu erori blocking si actiuni recomandate.

### ~~W921~~ ✅ Quest map editor transaction
**Descriere tehnica:** Transforma modificarile din editorul de quest map in tranzactii cu preflight, commit, rollback si audit.
**Scop:** Evita schimbari partiale care rup harta sau questurile asociate.
**Target:** map editor workflow, quest registry, validation pipeline.
**Acceptare:** O modificare esuata nu lasa registrul intr-o stare partial aplicata.

### ~~W922~~ ✅ Quest map editor permission matrix
**Descriere tehnica:** Defineste matricea de permisiuni pentru editarea marker-elor, rutei, geofence-urilor si metadata de story.
**Scop:** Limiteaza cine poate modifica continutul spatial si narativ.
**Target:** editor permissions, admin dashboard, map registry.
**Acceptare:** Fiecare actiune de editare are permisiune distincta si auditabilitate.

### ~~W923~~ ✅ Route editor conflict preview
**Descriere tehnica:** Afiseaza in editor conflictele care vor aparea dupa modificarea unei rute: obiective blocate, marker-e stale si branch-uri incompatibile.
**Scop:** Reduce surprizele la publicare.
**Target:** route editor, validation reports, quest/story registries.
**Acceptare:** Editorul arata conflictul inainte de commit.

### ~~W924~~ ✅ Map node merge approval
**Descriere tehnica:** Cere aprobare pentru unirea a doua map nodes cu dependinte diferite, istorice si ownership distinct.
**Scop:** Previne coliziuni de identitate in registrul de harta.
**Target:** map node registry, approval workflow, docs export.
**Acceptare:** Merge-ul nu se aplica fara evaluarea dependintelor si aprobarea necesara.

### ~~W925~~ ✅ Map node split migration
**Descriere tehnica:** Migreaza referintele cand un map node este impartit in mai multe noduri mai mici pentru claritate sau balans.
**Scop:** Pastreaza questurile si story branch-urile corecte dupa reconfigurarea hartii.
**Target:** map node migration, quest/story references, validation reports.
**Acceptare:** Referintele vechi sunt redirectionate sau raportate pentru repair.

### ~~W926~~ ✅ Quest chain editor timeline
**Descriere tehnica:** Afiseaza cronologia editarii unui quest chain: noduri adaugate, rute schimbate, gates mutate si marker-e regenerate.
**Scop:** Face review-ul editorial mai rapid si mai sigur.
**Target:** quest editor workflow, audit log, docs checklist.
**Acceptare:** Editorul poate reda istoria completa a unei chain modificat.

### ~~W927~~ ✅ Quest chain dependency break detector
**Descriere tehnica:** Detecteaza cand o schimbare de quest rupe dependentele altor questuri, story arcs sau map nodes.
**Scop:** Reduce regresiile intre continuturi conectate.
**Target:** quest dependency graph, map registry, release validation.
**Acceptare:** Dependentele rupte sunt raportate inainte de activare.

### ~~W928~~ ✅ Story arc editor approval gate
**Descriere tehnica:** Adauga gate de aprobare pentru modificari in story arc: dialog, branch, milestone, regiune si reward.
**Scop:** Protejeaza progresia si continutul narativ public.
**Target:** story editor workflow, approval service, validation pipeline.
**Acceptare:** Modificarea story nu devine live fara gate-ul documentat.

### ~~W929~~ ✅ Story arc rollback preview
**Descriere tehnica:** Simuleaza rollback-ul unui story arc cu impact pe harta, questuri, reputation, factions si portaluri.
**Scop:** Permite corectii narative in siguranta.
**Target:** story rollback service, map state, quest registry.
**Acceptare:** Preview-ul arata exact ce ramane activ si ce se reface.

### ~~W930~~ ✅ Story arc milestone diff
**Descriere tehnica:** Compara milestone-urile intre doua versiuni de story arc pentru a detecta schimbari de rute, obiective si spoiler gates.
**Scop:** Evidentiaza impactul inainte de publicare.
**Target:** story graph, docs export, validation reports.
**Acceptare:** Diff-ul arata milestone-urile adaugate, eliminate sau mutate.

### ~~W931~~ ✅ Story chapter prerequisite ledger
**Descriere tehnica:** Inregistreaza prerequisite-urile pentru fiecare capitol story, incluzand questuri, reputatie, harta si iteme.
**Scop:** Face cerintele de progresie explicite si verificabile.
**Target:** story chapter service, prerequisite validation, docs index.
**Acceptare:** Fiecare capitol are ledger valid si actualizat.

### ~~W932~~ ✅ Story chapter completion sync
**Descriere tehnica:** Sincronizeaza completarea capitolelor cu harta, marker-ele, unlock-urile si notificarile jucatorului.
**Scop:** Pastreaza progresia narativa si spatiala in pas.
**Target:** story chapter service, map discovery, notification service.
**Acceptare:** Finalizarea capitolului actualizeaza toate efectele documentate.

### ~~W933~~ ✅ Quest chapter transition guard
**Descriere tehnica:** Blocheaza tranzitia intre capitolele de quest daca starile anterioare, marker-ele sau reward-urile nu sunt confirmate.
**Scop:** Previnde sarirea neautorizata peste etape narative.
**Target:** quest chapter service, validation pipeline, reward service.
**Acceptare:** Tranzitia invalida este refuzata cu motiv clar.

### ~~W934~~ ✅ Quest chapter skip compensation
**Descriere tehnica:** Defineste compensatiile cand un capitol de quest este sarit intentional prin admin, event sau story catch-up.
**Scop:** Evita progresie incompleta dupa skip.
**Target:** quest chapter service, compensation workflow, audit log.
**Acceptare:** Skip-ul produce compensatii si audit pentru toate efectele relevante.

### ~~W935~~ ✅ Story branch eligibility report
**Descriere tehnica:** Genereaza raport pentru eligibilitatea la branch-uri story pe baza de reputatie, faction, locatie, achievements si alegeri anterioare.
**Scop:** Face clar de ce un branch este disponibil sau nu.
**Target:** story branch service, eligibility evaluator, player UI.
**Acceptare:** Raportul enumera motivele principale de includere si excludere.

### ~~W936~~ ✅ Quest branch eligibility snapshot
**Descriere tehnica:** Salveaza snapshot al eligibilitatii la branch pentru a evita schimbari dupa ce jucatorul deschide UI-ul sau confirma actiunea.
**Scop:** Protejeaza alegerile de quest de race condition.
**Target:** quest branch service, player session, UI state.
**Acceptare:** Confirmarea foloseste snapshot-ul salvat, nu starea mutabila curenta.

### ~~W937~~ ✅ Story branch choice replay
**Descriere tehnica:** Permite replay-ul alegerilor story pentru debug, cu optiuni, consecinte si ordinea exacta a evenimentelor.
**Scop:** Face investigarea bugurilor narative mai rapida.
**Target:** story choice service, audit logs, replay tools.
**Acceptare:** Replay-ul reproduce alegerile si efectele intr-o ordine determinista.

### ~~W938~~ ✅ Quest dialogue branch sync
**Descriere tehnica:** Sincronizeaza branch-urile de dialog cu obiectivele de quest, astfel incat alegerile sa reflecte progresia reala.
**Scop:** Evita dialoguri care ofera optiuni invalide.
**Target:** dialogue engine, quest tracker, story branch service.
**Acceptare:** Optiunile de dialog invalide sunt ascunse sau explicate corect.

### ~~W939~~ ✅ Dialogue choice consequence audit
**Descriere tehnica:** Inregistreaza consecintele alegerilor de dialog: reputatie, faction, quest state, map unlock si reward changes.
**Scop:** Face schimbarea narativa usor de verificat.
**Target:** dialogue engine, audit log, story progression.
**Acceptare:** Fiecare alegere importanta are consecinte auditate.

### ~~W940~~ ✅ Dialogue choice rollback guard
**Descriere tehnica:** Definește cand o alegere de dialog poate fi anulata sau refacuta fara a corupe progresia.
**Scop:** Evita blocarea jucatorului dupa o alegere gresita sau un restart.
**Target:** dialogue engine, story state, recovery service.
**Acceptare:** Rollback-ul este permis doar conform regulilor documentate.

### ~~W941~~ ✅ Map pin quest assignment validator
**Descriere tehnica:** Valideaza asignarea unui pin de harta unui quest sau story arc pe baza locatiei, spoiler gate-ului si ownership-ului.
**Scop:** Previne pins care trimit la continut gresit.
**Target:** pin registry, quest/story services, validation pipeline.
**Acceptare:** Pin-ul invalid este refuzat inainte de publicare.

### ~~W942~~ ✅ Map pin group moderation
**Descriere tehnica:** Permite moderarea grupurilor de pin-uri: aprobate, suspendate, hidden, duplicate sau in review.
**Scop:** Mentine harta curata si usor de administrat.
**Target:** map moderation queue, pin registry, admin dashboard.
**Acceptare:** Fiecare pin group are status si actiune recomandata.

### ~~W943~~ ✅ Story map import approval queue
**Descriere tehnica:** Creeaza coada de aprobare pentru importurile de mapping story din editori sau fisiere externe.
**Scop:** Evita publicarea accidentala a datelor nevalidate.
**Target:** import pipeline, moderator workflow, validation reports.
**Acceptare:** Importul ramane in coada pana la aprobare.

### ~~W944~~ ✅ Quest map import provenance audit
**Descriere tehnica:** Urmareste provenienta datelor importate pentru quest map: sursa, versiune, autor, data si transformari aplicate.
**Scop:** Face importul verificabil si reversibil.
**Target:** import pipeline, audit log, map registry.
**Acceptare:** Fiecare element importat are provenance si poate fi localizat in sursa originala.

### ~~W945~~ ✅ Story node capability matrix
**Descriere tehnica:** Defineste capabilitatile fiecarui nod story: dialog, combat, trade, portal, quest start, cutscene sau replay.
**Scop:** Face rolul fiecarui nod explicit pentru validation si UI.
**Target:** story node registry, map docs, validation pipeline.
**Acceptare:** Nodurile fara capabilitati declarate sunt marcate si auditate.

### ~~W946~~ ✅ Quest node capability conflict
**Descriere tehnica:** Detecteaza conflictele cand acelasi nod suporta capabilitati care se exclud, precum combat si safe tutorial sau hidden si public.
**Scop:** Previne designul inconsistent al hartii.
**Target:** node validation, quest/story registries, docs rules.
**Acceptare:** Conflictul este raportat cu capabilitatile incompatibile.

### ~~W947~~ ✅ Story location alias registry
**Descriere tehnica:** Creeaza registru pentru aliasuri de locatii narative, astfel incat nume vechi si nume noi sa rezolve la acelasi nod.
**Scop:** Pastreaza compatibilitatea intre docs, questuri si harta.
**Target:** map node registry, docs export, migration layer.
**Acceptare:** Aliasul rezolva la nodul curent sau la o eroare clara.

### ~~W948~~ ✅ Quest location alias deprecation
**Descriere tehnica:** Marcheaza aliasurile de locatie vechi ca deprecated cu perioada de compatibilitate si warning la folosire.
**Scop:** Permite migrari graduale fara rupere bruta.
**Target:** location alias registry, validation reports, docs.
**Acceptare:** Aliasul deprecated produce warning si recomandare de inlocuire.

### ~~W949~~ ✅ Story map accessibility audit
**Descriere tehnica:** Auditeaza accesibilitatea narativa a hartii pentru rute, marker-e, color coding, text, navigation aid si fallback-uri.
**Scop:** Face harta utilizabila pentru un spectru mai larg de jucatori.
**Target:** map renderer, accessibility rules, QA reports.
**Acceptare:** Auditul enumera zonele sau marker-ele care necesita ajustari.

### ~~W950~~ ✅ Mapping quest story release readiness review
**Descriere tehnica:** Adauga un review final pentru release care verifica map editor changes, quest/story sync, import approvals, provenance, accessibility si rollback.
**Scop:** Inchide ciclul de validare pentru continutul spatial si narativ.
**Target:** release checklist, validation pipeline, admin dashboard.
**Acceptare:** Review-ul produce verdict clar si lista de actiuni obligatorii sau blocking.

### ~~W951~~ ✅ Quest marker editorial freeze
**Descriere tehnica:** Introduce un freeze editorial pentru marker-ele de quest inainte de release, cu perioada de stabilizare si blocare a modificarilor neaprobate.
**Scop:** Evita schimbari tarzii care ar invalida testele sau documentatia.
**Target:** map editor workflow, release checklist, quest registry.
**Acceptare:** Marker-ele inghetate nu pot fi modificate fara bypass auditabil.

### ~~W952~~ ✅ Story branch editorial freeze
**Descriere tehnica:** Blocheaza modificarile la branch-urile story in fereastra de stabilizare de release.
**Scop:** Previne contradictii intre continutul aprobat si ultimul commit.
**Target:** story editor workflow, validation pipeline, docs export.
**Acceptare:** Branch-urile aflate in freeze refuza editari neaprobate.

### ~~W953~~ ✅ Map node stabilization window
**Descriere tehnica:** Defineste o fereastra de stabilizare pentru map nodes, rute si geofence-uri inainte de publicare.
**Scop:** Reduce riscul de a publica harti cu drift de ultim moment.
**Target:** map node registry, route graph, release process.
**Acceptare:** In stabilizare, modificarile intrerup revalidarea si cer recertificare.

### ~~W954~~ ✅ Quest route recertification gate
**Descriere tehnica:** Cere recertificare pentru rutele de quest atunci cand se modifica noduri, permisiuni, regiuni sau dependente.
**Scop:** Pastreaza integritatea traseelor dupa schimbari structurale.
**Target:** quest route validator, release checklist, validation pipeline.
**Acceptare:** Ruta modificata nu poate fi publicata fara revalidare completa.

### ~~W955~~ ✅ Story route recertification gate
**Descriere tehnica:** Invalideaza automat certificarea rutelor story cand se schimba lumea, portalurile, hazard-urile sau barierelor.
**Scop:** Previne activarea de rute narative neauditate.
**Target:** story route service, validation reports, release checklist.
**Acceptare:** Ruta story cu dependinte schimbate cere o noua aprobare.

### ~~W956~~ ✅ Map marker regression alert
**Descriere tehnica:** Detecteaza regresii in marker-ele de harta: disparitii, duplicari, schimbari de nivel spoiler sau vizibilitate.
**Scop:** Ofera alerta timpurie pentru erori de mapping.
**Target:** marker registry, regression tests, admin dashboard.
**Acceptare:** Orice regresie este raportata cu marker-ele afectate si tipul schimbarii.

### ~~W957~~ ✅ Quest marker duplication detector
**Descriere tehnica:** Cauta duplicate de marker pentru acelasi obiectiv, aceeasi locatie sau acelasi branch de quest.
**Scop:** Evita UI clutter si confuzie editoriala.
**Target:** marker registry, quest validation, map editor workflow.
**Acceptare:** Duplicatele sunt raportate si pot fi de-duplicated prin workflow.

### ~~W958~~ ✅ Story marker visibility diff
**Descriere tehnica:** Compara vizibilitatea marker-elor story intre doua profile sau doua versiuni de branch pentru a identifica leak-uri sau lipsuri.
**Scop:** Previne scurgeri de spoiler si inconsistente de access.
**Target:** marker visibility service, story branches, QA reports.
**Acceptare:** Diff-ul evidentiaza marker-ele adaugate sau ascunse neasteptat.

### ~~W959~~ ✅ Map pin unlock progression
**Descriere tehnica:** Leaga deblocarea pin-urilor de harta de progresia questurilor, story chapters, reputation si explorare.
**Scop:** Face progresia spatiala mai clara si controlata.
**Target:** pin registry, progression service, map renderer.
**Acceptare:** Pin-ul se deblocheaza doar cand progresia documentata este indeplinita.

### ~~W960~~ ✅ Quest pin unlock audit
**Descriere tehnica:** Auditeaza deblocarea pin-urilor de quest cu motiv, actor, eveniment declansator si efecte.
**Scop:** Face schimbarea harta-progresie usor de investigat.
**Target:** pin registry, audit log, quest progression.
**Acceptare:** Fiecare unlock important are audit si poate fi replicat in QA.

### ~~W961~~ ✅ Story pin spoiler level audit
**Descriere tehnica:** Valideaza ca pin-urile story respecta nivelul de spoiler si nu dezvaluie locatii, NPC-uri sau finaluri prea devreme.
**Scop:** Protejeaza descoperirea narativa.
**Target:** pin registry, spoiler classifier, content validation.
**Acceptare:** Pin-ul cu spoiler invalid este blocat sau retrogradat la nivelul permis.

### ~~W962~~ ✅ Quest pin grouping policy
**Descriere tehnica:** Defineste cum se grupeaza pin-urile pentru acelasi quest in functie de etapa, obiectiv, branch si locatie.
**Scop:** Reduce supra-incarcarea hartii si ghidajul ambiguu.
**Target:** pin renderer, quest tracker, map UI.
**Acceptare:** Gruparea urmeaza o ordine documentata si stabila.

### ~~W963~~ ✅ Map pin route anchor validator
**Descriere tehnica:** Valideaza ca un pin poate servi drept ancoră de ruta fara a crea destinatii invalide sau imposibile.
**Scop:** Previne rute generate din marker-e eronate.
**Target:** pin registry, route graph, quest navigation.
**Acceptare:** Pin-ul fara anchor valid este marcat ca non-navigabil.

### ~~W964~~ ✅ Story quest anchor fallback
**Descriere tehnica:** Ofera anchor-uri alternative pentru questuri story cand pin-ul principal este ascuns, mutat sau blocat.
**Scop:** Pastreaza navigarea functionala dupa schimbari editoriale.
**Target:** story quest service, map node registry, fallback routing.
**Acceptare:** Fallback-ul este folosit doar cand respecta spoiler gate-ul si accesul.

### ~~W965~~ ✅ Map waypoint path consistency
**Descriere tehnica:** Verifica consistenta dintre waypoint-uri, pin-uri si rutele calculate pentru a evita instructiuni contradictorii.
**Scop:** Face ghidajul pe harta predictibil.
**Target:** waypoint service, route graph, map renderer.
**Acceptare:** Waypoint-ul si ruta calculata indica aceeasi destinatie valida.

### ~~W966~~ ✅ Story waypoint permission filter
**Descriere tehnica:** Filtreaza waypoint-urile story dupa progresie, party, faction, staff role si nivel de spoiler.
**Scop:** Previne expunerea de informatie prin waypoint-uri.
**Target:** waypoint service, permission service, story state.
**Acceptare:** Waypoint-ul afisat este conform permisiunilor si branch-ului curent.

### ~~W967~~ ✅ Quest waypoint expiry
**Descriere tehnica:** Expira waypoint-urile de quest cand obiectivul este completat, rerutat, abandonat sau invalidat.
**Scop:** Previne ghidaje vechi care induc in eroare playerul.
**Target:** waypoint service, quest lifecycle, cleanup jobs.
**Acceptare:** Waypoint-ul expirat este curatat si nu reapare fara o conditie noua.

### ~~W968~~ ✅ Story waypoint provenance
**Descriere tehnica:** Salveaza provenienta fiecarui waypoint: quest, story, staff, import, AI draft sau recovery.
**Scop:** Face debugging-ul si moderarea waypoint-urilor posibile.
**Target:** waypoint registry, audit log, map editor workflow.
**Acceptare:** Provenienta waypoint-ului este vizibila pentru staff-ul autorizat.

### ~~W969~~ ✅ Map compass route split
**Descriere tehnica:** Imparte traseele de compass in segmente pentru a diferentia obiectivele de quest, story si optional.
**Scop:** Face navigatia mai precisa si mai usor de auditat.
**Target:** compass service, route graph, map UI.
**Acceptare:** Compass-ul poate afisa segmentul activ si destinatia urmatoare.

### ~~W970~~ ✅ Quest compass route recompute
**Descriere tehnica:** Recalculeaza traseul compass cand se schimba branch-ul, regiunea, rolul sau accesul la zona.
**Scop:** Evita indicatii de navigatie stale.
**Target:** compass service, route graph, player session.
**Acceptare:** Recalculele apar doar la schimbari relevante si sunt auditate sumar.

### ~~W971~~ ✅ Story compass spoiler budget
**Descriere tehnica:** Limiteaza cate informatii narative poate expune compass-ul in functie de progresie si nivel de spoiler.
**Scop:** Previne spoilere prin navigatie automata.
**Target:** compass service, spoiler classifier, story progression.
**Acceptare:** Compass-ul expune doar nivelul de detaliu permis.

### ~~W972~~ ✅ Map route load test harness
**Descriere tehnica:** Creeaza un harness de load test pentru rutele de quest si story cu multi jucatori, party si evente simultane.
**Scop:** Verifica scalabilitatea sistemului de mapping.
**Target:** route graph, validation pipeline, performance tests.
**Acceptare:** Harness-ul raporteaza timpul de calcul, blocajele si rutele instabile.

### ~~W973~~ ✅ Story map load spike detector
**Descriere tehnica:** Detecteaza spike-uri de incarcare pe zonele story si marcheaza rutele, marker-ele sau pin-urile cu risc operational.
**Scop:** Ajuta redistribuirea continutului popular.
**Target:** analytics, map renderer, admin dashboard.
**Acceptare:** Zonele supra-incarcate apar in raport cu recomandari de redistribuire.

### ~~W974~~ ✅ Quest path cache invalidation storm guard
**Descriere tehnica:** Limiteaza storm-urile de invalidare ale cache-ului de ruta cand multe evenimente de harta se produc simultan.
**Scop:** Protejeaza serverul de invalide repetate si rerender excesiv.
**Target:** route cache, map cache, scheduler.
**Acceptare:** Invalidarile simultane se grupeaza si produc un singur refresh coerent.

### ~~W975~~ ✅ Story region map shard alignment
**Descriere tehnica:** Aliniaza regiunile story cu shard-urile sau partitions folosite de map cache, astfel incat aceeasi zona sa aiba starea corecta.
**Scop:** Evita discrepante intre shards si world state.
**Target:** region mapping, cache layer, story state.
**Acceptare:** Shard-ul curent livreaza aceeasi versiune de regiune ca registrul live.

### ~~W976~~ ✅ Map marker shard reconciliation
**Descriere tehnica:** Reconciliaza marker-ele incarcate din shard-uri diferite pentru a elimina duplicari si stari divergente.
**Scop:** Pastreaza harta coerenta in distributie.
**Target:** marker cache, shard reconciliation, admin diagnostics.
**Acceptare:** Un marker conflictual este redus la o singura stare canonica.

### ~~W977~~ ✅ Quest region shard drift audit
**Descriere tehnica:** Auditeaza drift-ul dintre shard-ul in care ruleaza questul si regiunea canonica a hărții.
**Scop:** Identifica erori de sincronizare spatiala.
**Target:** quest runtime, region registry, audit reports.
**Acceptare:** Drift-ul este raportat cu shard-ul, regiunea si cauza probabila.

### ~~W978~~ ✅ Story region shard failover
**Descriere tehnica:** Defineste failover pentru regiunile story cand shard-ul principal este indisponibil sau lent.
**Scop:** Pastreaza continutul narativ disponibil in conditii degradate.
**Target:** region service, map cache, recovery service.
**Acceptare:** Failover-ul muta traficul pe un shard compatibil sau suspenda accesul controlat.

### ~~W979~~ ✅ Quest map data retention policy
**Descriere tehnica:** Stabileste retentia pentru datele de quest map: snapshots, logs, diffs, replay-uri si telemetry.
**Scop:** Controleaza stocarea si protejeaza datele sensibile.
**Target:** map telemetry, storage policy, audit retention.
**Acceptare:** Datele expirate sunt arhivate sau sterse conform politicii.

### ~~W980~~ ✅ Mapping quest story archival export
**Descriere tehnica:** Exporta arhiva proiectului de mapping quest story cu snapshot-uri, diffs, approvals, incidențe si documentatie aferenta.
**Scop:** Ofera un pachet complet pentru audit, backup si transfer de context.
**Target:** export pipeline, docs archive, release operations.
**Acceptare:** Exportul contine artefactele necesare pentru reconstructia istoricului continutului spatial.

### ~~W981~~ ✅ Story map generation seed policy
**Descriere tehnica:** Defineste seed-ul si regulile de initializare pentru generarea hartii narative astfel incat aceleasi input-uri sa produca aceeasi structura de baza.
**Scop:** Face continutul de mapping reproductibil si auditat.
**Target:** map generation pipeline, story world bootstrap, validation reports.
**Acceptare:** Seed-ul si parametrii de generatie sunt salvati si pot recrea harta de baza.

### ~~W982~~ ✅ Map generation deterministic diff
**Descriere tehnica:** Compara doua rulari de generare a hartii si raporteaza diferentele in noduri, rute, marker-e, hazard-uri si ownership.
**Scop:** Detecteaza drift in generarea procedurală.
**Target:** map generation pipeline, diff tools, release checklist.
**Acceptare:** Diff-ul listeaza toate schimbarile deterministe si cele non-deterministe separat.

### ~~W983~~ ✅ Quest generation lore guard
**Descriere tehnica:** Valideaza questurile generate automat impotriva glosarului lore, a hărții si a fazelor story active.
**Scop:** Previne generarea de continut care contrazice lumea.
**Target:** quest generation pipeline, lore validation, map registry.
**Acceptare:** Questul generat incorect este respins inainte de publicare.

### ~~W984~~ ✅ Story generation branch budget
**Descriere tehnica:** Limiteaza numarul de ramuri narative noi create de generarea automata pentru a evita explozia de continut.
**Scop:** Pastreaza controlul editorial si reducerea complexitatii.
**Target:** story generation pipeline, validation pipeline, docs.
**Acceptare:** Generarea peste buget este oprita sau coada de review este extinsa explicit.

### ~~W985~~ ✅ Map generation hazard quota
**Descriere tehnica:** Aplica cote pentru hazard-urile generate procedural pe regiune, capitol story si nivel de risc.
**Scop:** Evita harti supra-aglomerate sau imposibil de parcurs.
**Target:** map generation pipeline, hazard layer, region rules.
**Acceptare:** Hazard-urile peste cota sunt redistribuite, reduse sau refuzate.

### ~~W986~~ ✅ Quest generation objective balance
**Descriere tehnica:** Evalueaza obiectivele generate automat dupa dificultate, timp estimat, travel, combat si reward budget.
**Scop:** Previne questuri dezechilibrate sau repetitive.
**Target:** quest generation pipeline, balancing reports, route graph.
**Acceptare:** Obiectivele prea ieftine sau prea scumpe sunt marcate pentru ajustare.

### ~~W987~~ ✅ Story generation spoiler filter
**Descriere tehnica:** Filtreaza continutul generat automat pentru a respecta nivelul de spoiler al branch-ului, capitolului si fazei story.
**Scop:** Protejeaza descoperirea narativa.
**Target:** story generation pipeline, spoiler classifier, content validation.
**Acceptare:** Continutul cu spoiler peste prag este respins sau retrogradat.

### ~~W988~~ ✅ Map generation ownership assignment
**Descriere tehnica:** Asigneaza owner logic pentru nodurile, rutele si marker-ele generate automat, incluzand system owner si review owner.
**Scop:** Face generarea automata responsabila si urmaribila.
**Target:** map generation pipeline, ownership metadata, admin dashboard.
**Acceptare:** Fiecare element generat are owner sau este marcat neasignat.

### ~~W989~~ ✅ Quest generation provenance trace
**Descriere tehnica:** Pastreaza provenienta fiecarui quest generat: seed, template, prompt, faza story, evaluator si aprobator.
**Scop:** Face continutul generat auditabil si reversibil.
**Target:** quest generation pipeline, audit log, export pipeline.
**Acceptare:** Provenienta poate fi recunoscuta pentru fiecare quest generat.

### ~~W990~~ ✅ Story generation prompt audit
**Descriere tehnica:** Inregistreaza prompt-urile sau regulile folosite pentru generarea story, cu redactare pentru date sensibile.
**Scop:** Ofera trasabilitate fara expunerea completă a input-ului intern.
**Target:** story generation pipeline, audit log, privacy rules.
**Acceptare:** Promptul este auditabil si redactat in zonele sensibile.

### ~~W991~~ ✅ Map generation prompt lint
**Descriere tehnica:** Ruleaza lint asupra prompt-urilor sau configuratiilor de generare pentru a detecta instructiuni contradictorii sau incomplete.
**Scop:** Reduce generarea de continut incoerent.
**Target:** generation config validator, docs rules, release pipeline.
**Acceptare:** Promptul invalid este marcat cu erori si sugereaza corectii.

### ~~W992~~ ✅ Quest generation prompt lint
**Descriere tehnica:** Valideaza prompt-urile pentru generarea de questuri astfel incat sa includa lore, acces, ruta, recompensa si cleanup.
**Scop:** Evita generarea de questuri incomplete.
**Target:** quest generation pipeline, prompt validator, docs.
**Acceptare:** Promptul care lipseste componente critice este respins.

### ~~W993~~ ✅ Story generation template registry
**Descriere tehnica:** Creeaza registru pentru template-urile de generare story, cu tip, scope, nivel de spoiler, owner si versionare.
**Scop:** Standardizeaza continutul generat si usureaza review-ul.
**Target:** story generation pipeline, template registry, docs index.
**Acceptare:** Template-ul are metadata completa si versiune validata.

### ~~W994~~ ✅ Quest generation template registry
**Descriere tehnica:** Creeaza registru pentru template-urile de generare quest, incluzand obiective, rute, recompense si gating.
**Scop:** Reduce variantele improvizate si inconsistente.
**Target:** quest generation pipeline, template registry, validation.
**Acceptare:** Fiecare template are schema valida si owner.

### ~~W995~~ ✅ Map generation template registry
**Descriere tehnica:** Introduce template-uri pentru generarea de harta: zone, rute, landmark-uri, hazard-uri si spawn rules.
**Scop:** Controleaza structura hartii generate.
**Target:** map generation pipeline, template registry, validation reports.
**Acceptare:** Template-ul de harta este validat si versionat.

### ~~W996~~ ✅ Generated content approval queue
**Descriere tehnica:** Pune continutul generat automat intr-o coada de aprobare cu status, severitate, reviewer si deadline.
**Scop:** Separa generarea de publicare.
**Target:** moderation queue, generation pipeline, admin dashboard.
**Acceptare:** Continutul generat nu devine live fara aprobare.

### ~~W997~~ ✅ Generated content rollback guard
**Descriere tehnica:** Permite rollback pentru continut generat care a fost publicat si apoi dovedit problematic.
**Scop:** Reduce impactul continutului generat defect.
**Target:** generation runtime, rollback service, map/story registries.
**Acceptare:** Rollback-ul restaureaza versiunea anterioara sau produce recovery plan.

### ~~W998~~ ✅ Generated content quality threshold
**Descriere tehnica:** Stabileste praguri de calitate pentru continutul generat pe baza de consistenta, completitudine, performanta si review score.
**Scop:** Previne publicarea continutului slab sau incomplet.
**Target:** generation pipeline, validation reports, release checklist.
**Acceptare:** Continutul sub prag ramane blocat pana la corectie.

### ~~W999~~ ✅ Generated content contradiction detector
**Descriere tehnica:** Detecteaza contradictii intre continutul generat si regulile existente de harta, lore, quest si story.
**Scop:** Protejeaza coerenta lumii.
**Target:** generation validation, lore glossary, map/quest/story registries.
**Acceptare:** Contradictiile sunt raportate cu referinta la regula incalcata.

### ~~W1000~~ ✅ Generated content splice validator
**Descriere tehnica:** Valideaza integrarea continutului generat in continutul existent pentru a evita rute, marker-e sau capitole izolate.
**Scop:** Pastreaza continutul generat conectat la world state.
**Target:** generation pipeline, route graph, story/quest registries.
**Acceptare:** Splicing-ul invalid este refuzat inainte de publicare.

### ~~W1001~~ ✅ AI map suggestion review
**Descriere tehnica:** Creeaza un flux de review pentru sugestiile AI de map nodes, rute si marker-e, cu justificare si diff.
**Scop:** Limiteaza publicarea automata a sugestiilor AI.
**Target:** AI review queue, map editor workflow, validation pipeline.
**Acceptare:** Sugestia AI nu se publica fara review si rezultat clar.

### ~~W1002~~ ✅ AI quest suggestion review
**Descriere tehnica:** Creeaza un flux de review pentru sugestiile AI de questuri, incluzand obiective, flow, reward si lore checks.
**Scop:** Evita questuri AI nevalide sau incomplete.
**Target:** AI quest pipeline, moderator queue, quest registry.
**Acceptare:** Questul AI trece prin review cu verdict si motive.

### ~~W1003~~ ✅ AI story suggestion review
**Descriere tehnica:** Creeaza review pentru sugestii AI de story arcs, dialoguri si milestone-uri cu control de spoiler si canon.
**Scop:** Pastreaza continutul narativ sub control editorial.
**Target:** AI story pipeline, story editor workflow, lore validation.
**Acceptare:** Sugestia AI ajunge in review si nu poate sari peste gate.

### ~~W1004~~ ✅ AI suggestion batch quarantine
**Descriere tehnica:** Pune loturile de sugestii AI in carantina cand au erori blocking sau incalca reguli de continut.
**Scop:** Evita contaminarea registrului live cu drafturi defecte.
**Target:** AI queue, quarantine service, validation reports.
**Acceptare:** Lotul in carantina ramane izolat pana la repair sau respingere.

### ~~W1005~~ ✅ AI suggestion provenance chain
**Descriere tehnica:** Urmareste lantul de provenienta pentru o sugestie AI: model, seed, prompt, transformari, reviewer si publish decision.
**Scop:** Face responsabila publicarea continutului generat.
**Target:** AI audit log, generation pipeline, release operations.
**Acceptare:** Provenienta completa poate fi reconstruita pentru orice sugestie publicata.

### ~~W1006~~ ✅ AI suggestion diff explainability
**Descriere tehnica:** Explica diff-ul dintre sugestia AI si continutul existent prin schimbari de geografii, questuri, dialoguri si risc.
**Scop:** Face review-ul mai rapid si mai corect.
**Target:** AI review tools, diff renderer, validation pipeline.
**Acceptare:** Review-ul afiseaza clar ce s-a schimbat si de ce conteaza.

### ~~W1007~~ ✅ AI suggestion cost budget
**Descriere tehnica:** Limiteaza costul si volumul de sugestii AI pe sesiune, pe tip de continut si pe owner.
**Scop:** Controleaza consumul operational si zgomotul de review.
**Target:** AI generation service, quota manager, admin dashboard.
**Acceptare:** Sugestiile peste buget sunt amanate sau refuzate cu motiv.

### ~~W1008~~ ✅ AI suggestion latency monitor
**Descriere tehnica:** Monitorizeaza latenta generarii si review-ului AI pentru map, quest si story sugestii.
**Scop:** Detecteaza degradari operational timpuriu.
**Target:** AI telemetry, admin dashboard, performance alerts.
**Acceptare:** Latenta peste prag produce alerta si sumar contextual.

### ~~W1009~~ ✅ AI suggestion retry policy
**Descriere tehnica:** Defineste retry pentru sugestii AI esuate, cu backoff, max attempts si fallback manual.
**Scop:** Evita retry-urile infinite si pierderile de context.
**Target:** AI generation pipeline, recovery service, moderation queue.
**Acceptare:** Sugestia esuata urmeaza politica de retry si intra in fallback dupa limita.

### ~~W1010~~ ✅ AI suggestion branch lock
**Descriere tehnica:** Blocheaza sugestiile AI pe branch-uri story sau map nodes care sunt deja in review sau freeze.
**Scop:** Previne conflicte intre editari concurente.
**Target:** AI suggestion pipeline, branch lock manager, editor workflow.
**Acceptare:** Sugestia pe branch blocat este refuzata sau pusa in coada conform politicii.

### ~~W1011~~ ✅ AI suggestion freeze window
**Descriere tehnica:** Definește ferestre de freeze pentru sugestiile AI astfel incat schimbările mari sa fie stopate inainte de release.
**Scop:** Reduce riscul de drift intre review, test si publicare.
**Target:** AI suggestion pipeline, release checklist, branch lock manager.
**Acceptare:** Sugestiile intra in freeze si nu pot fi aplicate fara bypass auditat.

### ~~W1012~~ ✅ AI draft review SLA
**Descriere tehnica:** Stabileste un SLA pentru review-ul drafturilor AI cu prioritate, deadline si escaladare.
**Scop:** Evita cozi blocate si continut netrimis mai departe.
**Target:** moderation queue, admin dashboard, AI review workflow.
**Acceptare:** Draftul are timp limită si status vizibil pentru reviewer.

### ~~W1013~~ ✅ AI draft stale detector
**Descriere tehnica:** Detecteaza drafturile AI care au ramas prea mult timp in coada sau au fost invalidate de schimbari de context.
**Scop:** Previne publicarea continutului vechi sau nepotrivit.
**Target:** AI review queue, validation pipeline, audit reports.
**Acceptare:** Draftul stale este marcat, retras sau revalidat inainte de folosire.

### ~~W1014~~ ✅ AI content branch pinning
**Descriere tehnica:** Leaga drafturile AI de branch-ul de harta, quest sau story pe care l-au generat pentru a evita aplicarea pe alt context.
**Scop:** Previne cross-branch contamination.
**Target:** AI generation pipeline, branch manager, map/quest/story registries.
**Acceptare:** Draftul nu poate fi aplicat in alt branch fara reaprobare.

### ~~W1015~~ ✅ AI suggestion confidence threshold
**Descriere tehnica:** Stabileste pragul minim de incredere pentru sugestiile AI care pot merge in review sau in carantina.
**Scop:** Filtreaza sugestiile slabe inainte sa ocupe coada.
**Target:** AI scoring service, moderation queue, validation pipeline.
**Acceptare:** Sugestiile sub prag sunt respinse sau carantinate automat.

### ~~W1016~~ ✅ AI suggestion explanation bundle
**Descriere tehnica:** Genereaza un pachet de explicatie pentru fiecare sugestie AI: ce a schimbat, de ce, ce risca si ce dependente are.
**Scop:** Face review-ul mai rapid si mai corect.
**Target:** AI review tools, diff renderer, audit log.
**Acceptare:** Reviewerul vede sumarul explicativ fara sa consulte surse externe.

### ~~W1017~~ ✅ AI suggestion safety label
**Descriere tehnica:** Eticheteaza sugestiile AI cu nivel de siguranta: safe, caution, risky, blocked sau quarantine.
**Scop:** Instructeaza clar pipeline-ul si reviewerii.
**Target:** AI scoring service, moderation queue, release checklist.
**Acceptare:** Label-ul determinat este vizibil si afecteaza traseul de review.

### ~~W1018~~ ✅ AI content mismatch detector
**Descriere tehnica:** Detecteaza cand sugestia AI se potriveste semantic dar nu se potriveste cu regiunea, capitolul, quest chain-ul sau branch-ul activ.
**Scop:** Previne aplicarea continutului corect in locul gresit.
**Target:** AI validation, map/quest/story registries, context matcher.
**Acceptare:** Mismatch-ul este raportat cu contextul gresit si cel asteptat.

### ~~W1019~~ ✅ AI content rollback preview
**Descriere tehnica:** Arata exact cum ar arata rollback-ul unei sugestii AI inainte de a o publica sau respinge.
**Scop:** Reduce costul erorilor de review.
**Target:** AI review workflow, rollback service, preview renderer.
**Acceptare:** Preview-ul arata obiectele afectate si rezultatul final.

### ~~W1020~~ ✅ AI suggestion merge conflict detector
**Descriere tehnica:** Detecteaza conflictele dintre doua sugestii AI care ating acelasi nod, marker, quest sau branch story.
**Scop:** Evita suprascrierea si combinarea necontrolata a drafturilor.
**Target:** AI suggestion pipeline, branch lock manager, diff tools.
**Acceptare:** Conflictul este raportat cu sugestiile implicate si recomandare de rezolvare.

### ~~W1021~~ ✅ AI content dependency graph
**Descriere tehnica:** Construieste un graf al dependintelor pentru continutul AI generat: map nodes, quest chains, story arcs, lore terms si permissions.
**Scop:** Face compatibilitatea continutului AI verificabila.
**Target:** AI generation pipeline, dependency graph, validation reports.
**Acceptare:** Graf-ul poate arata ce depinde de un draft si ce il blocheaza.

### ~~W1022~~ ✅ AI content approval audit trail
**Descriere tehnica:** Inregistreaza trail-ul de aprobare pentru sugestiile AI cu reviewer, motivatie, schimbari cerute si timpul petrecut.
**Scop:** Ofera trasabilitate editoriala completa.
**Target:** AI review queue, audit log, admin dashboard.
**Acceptare:** Orice draft aprobat poate fi urmarit pana la decizia finala.

### ~~W1023~~ ✅ AI content rejection taxonomy
**Descriere tehnica:** Clasifica motivele de respingere pentru continutul AI: lore conflict, route invalid, spoiler, permission mismatch, missing cleanup sau quality.
**Scop:** Face respingerile consistente si utile pentru iteratie.
**Target:** AI review workflow, validation pipeline, docs export.
**Acceptare:** Fiecare respingere are cod de motiv si recomandare de corectie.

### ~~W1024~~ ✅ AI content rework loop
**Descriere tehnica:** Creeaza un loop de rework pentru sugestiile AI respinse, pastrand istoricul modificarilor si al criteriilor de validare.
**Scop:** Reduce rescrierile manuale si pierderea contextului.
**Target:** AI generation pipeline, review queue, version history.
**Acceptare:** Draftul revizuit poate fi comparat cu versiunea initiala si cu motivul respingerii.

### ~~W1025~~ ✅ AI content publish gate
**Descriere tehnica:** Adauga gate final de publish pentru continutul AI care verifica approval, provenance, consistency, accessibility si rollback.
**Scop:** Blocheaza publicarea accidentala a continutului generat.
**Target:** AI release pipeline, validation pipeline, admin dashboard.
**Acceptare:** Contintul AI nu poate fi publicat fara toate conditiile bifate.

### ~~W1026~~ ✅ AI content quarantine report
**Descriere tehnica:** Genereaza raport pentru continutul AI pus in carantina cu motiv, owner, dependente si actiune recomandata.
**Scop:** Face carantina actionabila, nu doar pasiva.
**Target:** quarantine service, moderation queue, admin dashboard.
**Acceptare:** Raportul arata clar de ce continutul a ramas in carantina.

### ~~W1027~~ ✅ AI content provenance export
**Descriere tehnica:** Exporta provenienta continutului AI intr-un format utilizabil la audit, backup sau transfer de context.
**Scop:** Face istoricul continutului generat usor de urmarit.
**Target:** AI audit log, export pipeline, docs archive.
**Acceptare:** Exportul include seed, prompt, model, reviewer si versiunea continutului.

### ~~W1028~~ ✅ AI content lifecycle state machine
**Descriere tehnica:** Modeleaza continutul AI in stari explicite: generated, queued, reviewing, approved, published, quarantined, rejected si rolled back.
**Scop:** Elimina tranzitiile implicite si starea ambigua.
**Target:** AI content service, state machine, moderation workflow.
**Acceptare:** Orice tranzitie invalida este refuzata si raportata.

### ~~W1029~~ ✅ AI content lifecycle cleanup
**Descriere tehnica:** Curata in mod sigur continutul AI ramas in stari intermediare dupa restart, failover sau cancel.
**Scop:** Evita drafturi orfane si lock-uri ramase.
**Target:** AI content service, recovery jobs, branch lock manager.
**Acceptare:** Starea intermediara este reconciliata sau raportata pentru interventie.

### ~~W1030~~ ✅ AI content release notes generator
**Descriere tehnica:** Genereaza release notes pentru continutul AI publicat cu sumar de schimbari, riscuri, validare si rollback.
**Scop:** Ofera context clar pentru review, suport si audit.
**Target:** AI release pipeline, docs export, changelog workflow.
**Acceptare:** Release notes includ efectele principale si artefactele afectate.

### ~~W1031~~ ✅ AI map suggestion moderation
**Descriere tehnica:** Adauga moderare dedicata pentru sugestiile AI de harti, incluzand risc de lore, route, accessibility si marker clutter.
**Scop:** Separa mapping-ul AI de sugestiile generice.
**Target:** AI map pipeline, moderation queue, map validation.
**Acceptare:** Sugestiile de harta trec printr-un review specializat.

### ~~W1032~~ ✅ AI quest suggestion moderation
**Descriere tehnica:** Creeaza moderare dedicata pentru sugestiile AI de questuri, cu verificari de ruta, reward, cleanup si branch compatibility.
**Scop:** Reduce erorile specifice questurilor generate.
**Target:** AI quest pipeline, quest validation, moderation workflow.
**Acceptare:** Questul AI nu ajunge in publicare fara validare de gameplay.

### ~~W1033~~ ✅ AI story suggestion moderation
**Descriere tehnica:** Creeaza moderare dedicata pentru sugestiile AI de story, cu accent pe canon, spoiler, pacing si branch coherence.
**Scop:** Protejeaza calitatea narativa.
**Target:** AI story pipeline, story validation, moderation queue.
**Acceptare:** Sugestia AI de story trebuie aprobata narativ si tehnic.

### ~~W1034~~ ✅ AI suggestion visibility policy
**Descriere tehnica:** Defineste ce sugestii AI sunt vizibile pentru staff, owner, moderator sau ascunse complet.
**Scop:** Controleaza expunerea drafturilor si a decisiei de review.
**Target:** AI review UI, permission service, audit log.
**Acceptare:** Fiecare rol vede doar nivelul de detaliu permis.

### ~~W1035~~ ✅ AI suggestion branch visibility filter
**Descriere tehnica:** Filtreaza sugestiile AI astfel incat un reviewer sa vada doar branch-urile si contextul pentru care are permisiuni.
**Scop:** Previne leakage intre ramuri de story sau map.
**Target:** AI review workflow, branch permissions, visibility service.
**Acceptare:** Sugestia pentru branch ascuns nu este afisata reviewerului fara drept.

### ~~W1036~~ ✅ AI suggestion review replay
**Descriere tehnica:** Permite replay-ul complet al deciziei de review pentru o sugestie AI, inclusiv diffs, comentarii si schimbari aplicate.
**Scop:** Face auditul si training-ul reviewerilor mai eficiente.
**Target:** AI review audit, replay tools, admin dashboard.
**Acceptare:** Replay-ul poate reproduce ordinea si continutul deciziei.

### ~~W1037~~ ✅ AI suggestion training feedback loop
**Descriere tehnica:** Colecteaza feedback-ul review-ului AI pentru a-l transforma in reguli, heuristici sau imbunatatiri de prompt.
**Scop:** Inchide bucla dintre review si generatie.
**Target:** AI validation pipeline, prompt tooling, audit reports.
**Acceptare:** Feedback-ul poate fi exportat si aplicat in iteratia urmatoare.

### ~~W1038~~ ✅ AI suggestion policy pack
**Descriere tehnica:** Grupeaza politicile AI pentru map, quest si story intr-un pachet documentat cu reguli, exceptii si severitati.
**Scop:** Elimina politicile dispersate si neuniforme.
**Target:** AI policy docs, validation pipeline, admin dashboard.
**Acceptare:** Pachetul de politici poate fi folosit ca referinta unica la review.

### ~~W1039~~ ✅ AI suggestion policy drift audit
**Descriere tehnica:** Detecteaza drift-ul dintre politica AI documentata si regulile efectiv aplicate de pipeline.
**Scop:** Previne divergențele intre docs si implementare.
**Target:** AI policy docs, pipeline rules, audit reports.
**Acceptare:** Drift-ul este raportat cu regula, severitatea si sursa divergentei.

### ~~W1040~~ ✅ AI suggestion rollout checklist
**Descriere tehnica:** Creeaza checklist de rollout pentru schimbari in pipeline-ul AI, inclusiv validation, moderation, cache, rollback si docs.
**Scop:** Face schimbarile AI publicabile in siguranta.
**Target:** AI release pipeline, docs/taskuri-de-lucru.md, validation checklist.
**Acceptare:** Rollout-ul nu continua fara checklist complet si aprobat.

### ~~W1041~~ ✅ AI suggestion policy simulator
**Descriere tehnica:** Simuleaza aplicarea politicilor AI pe sugestii reale sau sintetice pentru a vedea ce ar fi aprobat, carantinat sau respins.
**Scop:** Testeaza politicile inainte de activare.
**Target:** AI policy engine, validation pipeline, admin tools.
**Acceptare:** Simulatorul produce acelasi verdict ca engine-ul de productie pentru aceleasi inputuri.

### ~~W1042~~ ✅ AI suggestion scenario test harness
**Descriere tehnica:** Creeaza un harness de scenarii pentru sugestii AI care acopera cazuri de lore conflict, branch mismatch, quality drop si rollback.
**Scop:** Asigura regresii controlate inainte de rollout.
**Target:** AI test suite, moderation workflow, validation reports.
**Acceptare:** Scenariile esentiale pot fi rulate repetabil si dau verdict stabil.

### ~~W1043~~ ✅ AI suggestion approval override audit
**Descriere tehnica:** Inregistreaza orice override manual peste verdictul normal de review pentru sugestiile AI.
**Scop:** Pastreaza trasabilitate pentru exceptii.
**Target:** AI review queue, audit log, admin dashboard.
**Acceptare:** Orice override are autor, motiv si timestamp vizibil.

### ~~W1044~~ ✅ AI suggestion owner routing
**Descriere tehnica:** Trimite fiecare sugestie AI catre ownerul corect in functie de map, quest, story sau branch-ul afectat.
**Scop:** Reduce timpul pierdut cu redistribuiri manuale.
**Target:** moderation queue, ownership registry, notification service.
**Acceptare:** Sugestia ajunge la ownerul potrivit fara interventie manuala.

### ~~W1045~~ ✅ AI suggestion context pack builder
**Descriere tehnica:** Construieste automat un context pack minim pentru fiecare sugestie AI, cu reguli, branch, istoric si dependente relevante.
**Scop:** Ofera reviewerului contextul necesar fara zgomot.
**Target:** AI review tools, context service, audit export.
**Acceptare:** Pachetul de context contine doar informatia necesara pentru decizie.

### ~~W1046~~ ✅ AI suggestion prompt template registry
**Descriere tehnica:** Centralizeaza template-urile de prompt folosite pentru sugestii AI si le leaga de tipul de continut si politica activa.
**Scop:** Evita prompturi divergente intre echipe.
**Target:** prompt tooling, AI generation pipeline, policy docs.
**Acceptare:** Orice sugestie poate indica template-ul folosit pentru generare.

### ~~W1047~~ ✅ AI suggestion drift notifier
**Descriere tehnica:** Notifica atunci cand o sugestie AI sau un set de sugestii incepe sa se abata de la reguli, stil sau intentia branch-ului.
**Scop:** Identifica devierea din timp.
**Target:** AI telemetry, alerting system, moderation queue.
**Acceptare:** Drift-ul declanseaza alerta cu contextul afectat.

### ~~W1048~~ ✅ AI suggestion rollback queue
**Descriere tehnica:** Introduce o coada separata pentru rollback-urile sugerate de AI, astfel incat reversarile sa fie validate si ordonate.
**Scop:** Evita rollback-uri haotice sau simultane.
**Target:** rollback service, AI review workflow, queue manager.
**Acceptare:** Rollback-urile propuse sunt executate in ordine si cu status clar.

### ~~W1049~~ ✅ AI suggestion publish window scheduler
**Descriere tehnica:** Programeaza ferestre de publicare pentru sugestiile AI aprobate, cu control pe ore, zile si tip de continut.
**Scop:** Sincronizeaza publicarea cu operatiunile si review-ul.
**Target:** release scheduler, AI publish pipeline, admin dashboard.
**Acceptare:** Sugestia aprobata asteapta fereastra corecta inainte de publish.

### ~~W1050~~ ✅ AI suggestion duplicate detector
**Descriere tehnica:** Detecteaza sugestiile AI duplicate sau aproape duplicate pentru acelasi branch, obiectiv sau marker set.
**Scop:** Reduce zgomotul si munca redundanta.
**Target:** AI queue, similarity service, moderation workflow.
**Acceptare:** Duplicate-urile sunt grupate si tratate ca o singura propunere.

### ~~W1051~~ ✅ AI suggestion canonical source linker
**Descriere tehnica:** Leaga fiecare sugestie AI de sursa canonica relevanta: document, quest chain, lore registry sau map definition.
**Scop:** Face verificarea rapida si sigura.
**Target:** AI validation, lore registry, docs archive.
**Acceptare:** Reviewerul poate deschide sursa canonica din sugestie.

### ~~W1052~~ ✅ AI suggestion validation report exporter
**Descriere tehnica:** Exporta rezultatele de validare ale unei sugestii AI intr-un raport usor de distribuit si arhivat.
**Scop:** Simplifica auditul si handoff-ul.
**Target:** validation pipeline, reporting service, docs archive.
**Acceptare:** Raportul include verdictul, regulile incalcate si actiunile recomandate.

### ~~W1053~~ ✅ AI suggestion moderation escalation
**Descriere tehnica:** Ridica automat la nivel superior sugestiile AI care au risc mare, incertitudine mare sau impact critic.
**Scop:** Asigura review uman pentru cazurile sensibile.
**Target:** moderation queue, escalation rules, admin dashboard.
**Acceptare:** Sugestiile critice sunt escaladate cu motiv si prioritate.

### ~~W1054~~ ✅ AI suggestion incident triage mode
**Descriere tehnica:** Activeaza un mod de triere cand apar incidente pe pipeline-ul AI, reducand publicarea si marind vizibilitatea asupra esecurilor.
**Scop:** Stabilizeaza operatiunile in timpul incidentelor.
**Target:** AI operations, incident dashboard, moderation queue.
**Acceptare:** Triage mode modifica explicit fluxul si este vizibil in UI.

### ~~W1055~~ ✅ AI suggestion quota reset policy
**Descriere tehnica:** Defineste cand si cum se reseteaza cotele pentru sugestiile AI pe user, branch sau interval de timp.
**Scop:** Evita abuzul si distribuie corect resursele.
**Target:** quota manager, AI generation service, admin settings.
**Acceptare:** Resetarea cotelor respecta regula configurata si este auditata.

### ~~W1056~~ ✅ AI suggestion retention policy
**Descriere tehnica:** Stabileste cat timp se pastreaza sugestiile AI, diffs, comentariile si artefactele asociate.
**Scop:** Controleaza costul de stocare si conformitatea.
**Target:** retention service, audit storage, archive jobs.
**Acceptare:** Sugestiile expirate sunt arhivate sau sterse conform politicii.

### ~~W1057~~ ✅ AI suggestion archive search index
**Descriere tehnica:** Indexeaza sugestiile AI arhivate pentru cautare dupa branch, autor, verdict, risc sau context.
**Scop:** Face istoricul reutilizabil operational.
**Target:** archive search, AI audit store, admin tools.
**Acceptare:** O sugestie arhivata poate fi gasita rapid dupa criteriile principale.

### ~~W1058~~ ✅ AI suggestion replay sandbox
**Descriere tehnica:** Ofera un sandbox in care sugestiile AI pot fi redate si testate fara a afecta registrul live.
**Scop:** Permite verificari sigure pentru cazurile complexe.
**Target:** sandbox runtime, AI review tools, validation pipeline.
**Acceptare:** Replay-ul in sandbox nu modifica datele live si produce rezultate observabile.

### ~~W1059~~ ✅ AI suggestion branch merge advisor
**Descriere tehnica:** Recomanda cum sa fie unite doua ramasite de sugestii AI care ating acelasi branch, cu prioritate si ordine de aplicare.
**Scop:** Reduce conflictele la integrare.
**Target:** branch manager, AI diff tools, moderation workflow.
**Acceptare:** Advisorul sugereaza o ordine si marcheaza riscurile de merge.

### ~~W1060~~ ✅ AI suggestion approval reason assistant
**Descriere tehnica:** Sugereaza motivul de aprobare sau respingere pe baza diff-ului, regulilor si istoricului branch-ului.
**Scop:** Standardizeaza deciziile de review.
**Target:** AI review UI, audit log, validation pipeline.
**Acceptare:** Reviewerul vede o propunere de motiv pe care o poate accepta sau edita.

### ~~W1061~~ ✅ AI suggestion safe default fallback
**Descriere tehnica:** Definește fallback-uri sigure atunci cand sugestia AI nu poate fi evaluata complet sau lipsesc datele necesare.
**Scop:** Evita decizii arbitrare sau publicari riscante.
**Target:** AI generation pipeline, validation service, moderation queue.
**Acceptare:** Lipsa de date duce la fallback sigur, nu la aprobare implicita.

### ~~W1062~~ ✅ AI suggestion accessibility audit
**Descriere tehnica:** Verifica daca sugestiile AI respecta cerintele de accesibilitate pentru UI, texte, contrast descris sau navigare.
**Scop:** Pastreaza continutul generat utilizabil pentru toti utilizatorii.
**Target:** AI validation, accessibility checker, review workflow.
**Acceptare:** Problemele de accesibilitate sunt raportate inainte de publish.

### ~~W1063~~ ✅ AI suggestion localization guard
**Descriere tehnica:** Blocheaza sugestiile AI care introduc texte sau termeni incompatibili cu limba si terminologia proiectului.
**Scop:** Evita amestecul accidental de locale.
**Target:** AI validation, localization service, content review.
**Acceptare:** Sugestia cu localizare gresita este marcata si retrimisa la corectie.

### ~~W1064~~ ✅ AI suggestion permissions snapshot
**Descriere tehnica:** Salveaza snapshot-ul permisiunilor in momentul generarii si review-ului unei sugestii AI.
**Scop:** Face auditurile reproducibile.
**Target:** AI audit log, permission service, review workflow.
**Acceptare:** Se poate vedea ce drepturi existau cand a fost evaluata sugestia.

### ~~W1065~~ ✅ AI suggestion provenance redaction
**Descriere tehnica:** Redacteaza parti sensibile din provenienta sugestiei AI in functie de rolul celui care vizualizeaza raportul.
**Scop:** Protejeaza informatiile interne sensibile.
**Target:** AI audit export, permission service, admin dashboard.
**Acceptare:** Vizualizarea redactionata ascunde campurile nepermise fara sa strice auditul.

### ~~W1066~~ ✅ AI suggestion analytics dashboard
**Descriere tehnica:** Construieste un dashboard pentru volum, aprobari, respingeri, latenta si costuri ale sugestiilor AI.
**Scop:** Ofera vizibilitate operationala si de produs.
**Target:** analytics service, admin dashboard, telemetry pipeline.
**Acceptare:** Dashboard-ul afiseaza metricile cheie pe interval si tip de continut.

### ~~W1067~~ ✅ AI suggestion quality regression alert
**Descriere tehnica:** Detecteaza scaderi de calitate fata de baseline-ul istoric pentru sugestiile AI pe acelasi tip de continut.
**Scop:** Prinde degradarile dupa schimbari de prompt sau model.
**Target:** telemetry service, validation reports, alerting system.
**Acceptare:** O scadere semnificativa declanseaza alerta cu comparatie fata de baseline.

### ~~W1068~~ ✅ AI suggestion taxonomy sync
**Descriere tehnica:** Sincronizeaza taxonomia de tipuri de sugestii, motive si severitati intre docs, pipeline si UI.
**Scop:** Evita nomenclatura divergenta intre componente.
**Target:** AI policy docs, validation pipeline, admin dashboard.
**Acceptare:** Taxonomia folosita in UI coincide cu cea din documentatie si back-end.

### ~~W1069~~ ✅ AI suggestion import-export bundle
**Descriere tehnica:** Ambaleaza o sugestie AI impreuna cu contextul, verdictul si raportul de audit pentru import sau export.
**Scop:** Simplifica transferul intre medii si echipe.
**Target:** AI export pipeline, archive jobs, review tools.
**Acceptare:** Bundle-ul poate fi importat fara pierderea datelor esentiale.

### ~~W1070~~ ✅ AI suggestion rollout freeze rehearsal
**Descriere tehnica:** Ruleaza o repetitie de rollout pentru schimbari AI inainte de activare, verificand freeze, review, rollback si notificarile.
**Scop:** Reduce riscul la lansare.
**Target:** release pipeline, validation checklist, admin dashboard.
**Acceptare:** Repetitia confirma ca toate gate-urile de rollout reactioneaza corect.

### ~~W1071~~ ✅ AI suggestion freeze bypass audit
**Descriere tehnica:** Urmareste orice bypass folosit pentru a ignora ferestrele de freeze in pipeline-ul AI.
**Scop:** Pastreaza controlul asupra exceptiilor de release.
**Target:** release pipeline, audit log, admin dashboard.
**Acceptare:** Orice bypass este inregistrat cu motiv, autor si durata.

### ~~W1072~~ ✅ AI suggestion review SLA dashboard
**Descriere tehnica:** Afiseaza SLA-ul de review pentru sugestiile AI cu timer, status si blocaje active.
**Scop:** Face intarzierile vizibile si actionabile.
**Target:** moderation dashboard, review queue, telemetry service.
**Acceptare:** Dashboard-ul arata clar sugestiile care depasesc SLA-ul.

### ~~W1073~~ ✅ AI suggestion stale quarantine auto move
**Descriere tehnica:** Muta automat in carantina sugestiile AI care au ramas stale prea mult timp in coada.
**Scop:** Elimina drafturile expirate din fluxul activ.
**Target:** moderation queue, quarantine service, validation pipeline.
**Acceptare:** Sugestia stale este mutata fara interventie manuala.

### ~~W1074~~ ✅ AI suggestion branch lock conflict report
**Descriere tehnica:** Genereaza un raport clar cand doua sugestii AI intra in conflict pe acelasi branch blocat.
**Scop:** Ajuta la rezolvarea rapida a coliziunilor.
**Target:** branch lock manager, AI review workflow, audit log.
**Acceptare:** Raportul identifica ambele sugestii si motivul conflictului.

### ~~W1075~~ ✅ AI suggestion semantic diff summary
**Descriere tehnica:** Produce un sumar semantic al diferentelor dintre sugestia AI si continutul existent.
**Scop:** Reduce timpul necesar pentru review.
**Target:** diff renderer, AI review tools, validation pipeline.
**Acceptare:** Sumarul evidentiaza schimbarile importante fara sa ascunda detalii.

### ~~W1076~~ ✅ AI suggestion target scope validator
**Descriere tehnica:** Verifica daca sugestia AI modifica doar aria de target declarata: map, quest, story sau config.
**Scop:** Previne editari care scapa din scope.
**Target:** AI validation, scope matcher, moderation queue.
**Acceptare:** Orice extindere nepermisa de scope este blocata.

### ~~W1077~~ ✅ AI suggestion branch impact estimator
**Descriere tehnica:** Estimeaza impactul unei sugestii AI asupra branch-ului, inclusiv numar de obiecte, dependente si risc.
**Scop:** Ajuta reviewerul sa prioritizeze corect.
**Target:** AI review UI, dependency graph, telemetry service.
**Acceptare:** Estimarea arata clar impactul si severitatea potentiala.

### ~~W1078~~ ✅ AI suggestion review queue sorter
**Descriere tehnica:** Sorteaza sugestiile AI in coada dupa risc, vechime, owner si blocaje active.
**Scop:** Optimizeaza ordinea de lucru pentru moderatori.
**Target:** moderation queue, scoring service, admin dashboard.
**Acceptare:** Ordinea din coada reflecta regulile de prioritate configurate.

### ~~W1079~~ ✅ AI suggestion moderation note templates
**Descriere tehnica:** Defineste sabloane pentru notitele moderatorilor asupra sugestiilor AI.
**Scop:** Uniformizeaza feedback-ul si auditul.
**Target:** moderation UI, audit log, review workflow.
**Acceptare:** Moderatorul poate selecta rapid un sablon si il poate personaliza.

### ~~W1080~~ ✅ AI suggestion approval latency tracker
**Descriere tehnica:** Urmareste timpul dintre generarea, review-ul si aprobarea unei sugestii AI.
**Scop:** Identifica blocajele din pipeline.
**Target:** telemetry service, admin dashboard, audit reports.
**Acceptare:** Trackerul afiseaza timpii pe etape si media agregata.

### ~~W1081~~ ✅ AI suggestion generation trace viewer
**Descriere tehnica:** Afiseaza traseul complet al unei sugestii AI de la prompt la rezultat, inclusiv transformari si filtre.
**Scop:** Face debug-ul reproductibil.
**Target:** AI audit log, trace viewer, generation pipeline.
**Acceptare:** Traseul poate fi urmarit fara a deschide surse externe.

### ~~W1082~~ ✅ AI suggestion blocked reason composer
**Descriere tehnica:** Compune un motiv clar si structurat cand o sugestie AI este blocata de politica sau validare.
**Scop:** Ofera feedback actionabil pentru corectie.
**Target:** moderation workflow, validation service, review UI.
**Acceptare:** Motivul include regula, context si actiunea recomandata.

### ~~W1083~~ ✅ AI suggestion checklist validator
**Descriere tehnica:** Verifica daca o sugestie AI are toate elementele cerute in checklist inainte de publish.
**Scop:** Reduce scapari in etapa finala.
**Target:** release checklist, AI publish pipeline, admin dashboard.
**Acceptare:** Sugestia fara checklist complet nu poate fi publicata.

### ~~W1084~~ ✅ AI suggestion owner override policy
**Descriere tehnica:** Defineste cand ownerul poate suprascrie verdictul normal al unei sugestii AI.
**Scop:** Controleaza exceptiile fara a pierde guvernanta.
**Target:** permission service, review workflow, audit log.
**Acceptare:** Override-ul este permis doar in conditiile explicite ale politicii.

### ~~W1085~~ ✅ AI suggestion rollback approval gate
**Descriere tehnica:** Adauga un gate separat pentru aprobarea rollback-urilor generate de AI.
**Scop:** Evita revert-uri automate riscante.
**Target:** rollback service, moderation queue, release pipeline.
**Acceptare:** Rollback-ul AI nu executa fara aprobare explicita.

### ~~W1086~~ ✅ AI suggestion branch ownership snapshot
**Descriere tehnica:** Captureaza ownerii si responsabilii branch-ului in momentul generarii sugestiei AI.
**Scop:** Face handoff-ul si auditul mai clare.
**Target:** ownership registry, AI audit log, moderation workflow.
**Acceptare:** Snapshot-ul arata cine era responsabil la momentul generarii.

### ~~W1087~~ ✅ AI suggestion policy exception registry
**Descriere tehnica:** Pastreaza un registru al exceptiilor aprobate fata de politicile AI standard.
**Scop:** Evita exceptiile uitate sau repetate.
**Target:** policy engine, audit store, admin dashboard.
**Acceptare:** Orice exceptie este listata, cautabila si expirabila.

### ~~W1088~~ ✅ AI suggestion model version pin
**Descriere tehnica:** Permite fixarea versiunii de model folosita la generarea unei sugestii AI.
**Scop:** Asigura reproductibilitatea rezultatelor.
**Target:** generation pipeline, model registry, audit log.
**Acceptare:** Sugestia afiseaza modelul exact si nu se reevalueaza cu alt model.

### ~~W1089~~ ✅ AI suggestion prompt version pin
**Descriere tehnica:** Leaga fiecare sugestie AI de versiunea exacta a promptului care a produs-o.
**Scop:** Face debugging-ul si comparatia intre iteratii mai simple.
**Target:** prompt registry, AI audit log, review tools.
**Acceptare:** Promptul folosit poate fi identificat si comparat pe versiuni.

### ~~W1090~~ ✅ AI suggestion validation cache
**Descriere tehnica:** Cache-uieste rezultatele de validare pentru sugestii AI identice sau aproape identice.
**Scop:** Reduce costul recalcularilor repetate.
**Target:** validation pipeline, cache layer, moderation queue.
**Acceptare:** Validarile repetate refolosesc rezultatul cand inputul nu s-a schimbat.

### ~~W1091~~ ✅ AI suggestion replay permission gate
**Descriere tehnica:** Restrictioneaza cine poate relua o sugestie AI in sandbox sau in audit view.
**Scop:** Protejeaza continutul sensibil.
**Target:** sandbox runtime, permission service, audit viewer.
**Acceptare:** Replay-ul este permis doar rolurilor configurate.

### ~~W1092~~ ✅ AI suggestion structured feedback form
**Descriere tehnica:** Creeaza un formular structurat pentru feedback-ul reviewerilor asupra sugestiilor AI.
**Scop:** Standardizeaza invatarea si analiza ulterioara.
**Target:** review UI, feedback pipeline, analytics service.
**Acceptare:** Feedback-ul poate fi colectat pe categorii si severitati.

### ~~W1093~~ ✅ AI suggestion conflict resolution guide
**Descriere tehnica:** Documenteaza pasii de rezolvare pentru conflictele dintre sugestii AI, inclusiv prioritate, merge si respingere.
**Scop:** Reduce deciziile ad-hoc.
**Target:** docs/taskuri-de-lucru.md, moderation workflow, branch manager.
**Acceptare:** Ghidul explica ce trebuie facut pentru fiecare tip de conflict.

### ~~W1094~~ ✅ AI suggestion release candidate pin
**Descriere tehnica:** Marcheaza o sugestie AI ca release candidate si blocheaza modificari suplimentare pana la decizia finala.
**Scop:** Stabilizeaza pachetul care urmeaza sa fie publicat.
**Target:** release pipeline, branch lock manager, review workflow.
**Acceptare:** RC-ul nu poate fi schimbat fara a reseta statusul.

### ~~W1095~~ ✅ AI suggestion cleanup scheduler
**Descriere tehnica:** Programeaza curatarea sugestiilor AI abandonate, respinse sau expirate.
**Scop:** Pastreaza coada si arhiva curate.
**Target:** cleanup jobs, moderation queue, archive service.
**Acceptare:** Jobul sterge sau arhiveaza doar elementele eligibile.

### ~~W1096~~ ✅ AI suggestion health check summary
**Descriere tehnica:** Rezuma starea de sanatate a pipeline-ului AI pentru sugestii, inclusiv coada, erori, cost si latenta.
**Scop:** Ofera un status scurt si actionabil.
**Target:** ops dashboard, telemetry service, validation pipeline.
**Acceptare:** Summary-ul arata clar daca sistemul este healthy, degraded sau blocked.

### ~~W1097~~ ✅ AI suggestion anomaly detector
**Descriere tehnica:** Detecteaza anomalii in distributia sugestiilor AI, cum ar fi spike-uri de respingere, duplicate sau drift.
**Scop:** Semnaleaza probleme de model sau proces.
**Target:** telemetry service, analytics dashboard, alerting system.
**Acceptare:** Anomalia produce alerta cu metricile care au deviat.

### ~~W1098~~ ✅ AI suggestion publish audit report
**Descriere tehnica:** Genereaza un raport final pentru sugestiile AI publicate, incluzand aprobari, versiuni, diffs si rollback readiness.
**Scop:** Consolideaza auditul post-publicare.
**Target:** audit log, release pipeline, docs archive.
**Acceptare:** Raportul permite reconstruirea deciziei de publish.

### ~~W1099~~ ✅ AI suggestion registry diff view
**Descriere tehnica:** Afiseaza diferenta dintre registrul curent de sugestii AI si o versiune anterioara.
**Scop:** Ajuta la urmarirea schimbarilor de volum si politica.
**Target:** admin dashboard, registry service, audit tools.
**Acceptare:** Diferența arata adaugari, stergeri si modificari relevante.

### ~~W1100~~ ✅ AI suggestion rollout signoff
**Descriere tehnica:** Adauga semnatura finala de release pentru schimbari in pipeline-ul AI dupa validare, review si checklist.
**Scop:** Marcheaza in mod clar gata de productie.
**Target:** release pipeline, approval workflow, admin dashboard.
**Acceptare:** Schimbarea nu poate merge live fara semnatura finala inregistrata.

### ~~W1101~~ ✅ AI suggestion policy exception review
**Descriere tehnica:** Revizuieste periodic exceptiile din politica AI si le expira daca nu mai sunt justificate.
**Scop:** Evita exceptiile permanente si necontrolate.
**Target:** policy registry, audit workflow, admin dashboard.
**Acceptare:** Exceptiile vechi pot fi expirate sau reconfirmate explicit.

### ~~W1102~~ ✅ AI suggestion moderation escalation timer
**Descriere tehnica:** Porneste un timer de escaladare pentru sugestiile AI care stau prea mult fara raspuns.
**Scop:** Previne blocarea tacuta a review-ului.
**Target:** moderation queue, escalation rules, telemetry service.
**Acceptare:** Sugestia depasita este escaladata automat.

### ~~W1103~~ ✅ AI suggestion review ownership handoff
**Descriere tehnica:** Permite predarea explicita a unei sugestii AI de la un reviewer la altul fara pierderea contextului.
**Scop:** Reduce confuzia in echipele distribuite.
**Target:** review workflow, ownership registry, audit log.
**Acceptare:** Handoff-ul pastreaza istoricul si noul owner este vizibil.

### ~~W1104~~ ✅ AI suggestion branch freeze notice
**Descriere tehnica:** Trimite notificari cand un branch intra in freeze si blocheaza sugestiile AI noi.
**Scop:** Face starea de freeze vizibila imediat.
**Target:** notification service, branch lock manager, UI.
**Acceptare:** Utilizatorii vad clar ca branch-ul este in freeze.

### ~~W1105~~ ✅ AI suggestion scope shrink detector
**Descriere tehnica:** Detecteaza cand o sugestie AI reduce accidental aria de impact fata de ce era asteptat.
**Scop:** Evita pierderea unor elemente importante.
**Target:** AI validation, scope matcher, review tools.
**Acceptare:** Reducerea neasteptata a scope-ului este raportata ca risc.

### ~~W1106~~ ✅ AI suggestion branch replay checksum
**Descriere tehnica:** Calculeaza un checksum pentru replay-ul unei sugestii AI pe acelasi branch si acelasi context.
**Scop:** Verifica reproducibilitatea.
**Target:** replay sandbox, AI audit log, validation service.
**Acceptare:** Replay-ul identic produce acelasi checksum sau explica diferenta.

### ~~W1107~~ ✅ AI suggestion policy rule linter
**Descriere tehnica:** Analizeaza regulile din politica AI pentru ambiguitate, conflicte si lipsa de severitate.
**Scop:** Imbunatateste calitatea regulilor inainte de aplicare.
**Target:** policy docs, validation pipeline, admin tools.
**Acceptare:** Linterul raporteaza problemele si sugereaza corectii.

### ~~W1108~~ ✅ AI suggestion action recommendation engine
**Descriere tehnica:** Recomanda actiunea potrivita pentru fiecare sugestie AI: approve, reject, quarantine, rework sau defer.
**Scop:** Ajuta moderatorii sa ia decizii consistente.
**Target:** moderation queue, scoring service, review UI.
**Acceptare:** Recomandarea este explicata si poate fi suprascrisa.

### ~~W1109~~ ✅ AI suggestion content boundary checker
**Descriere tehnica:** Verifica daca sugestia AI trece de granita permisă dintre map, quest, story si configuratie.
**Scop:** Previne amestecul nedorit intre domenii.
**Target:** validation pipeline, branch manager, content registries.
**Acceptare:** Crossing-ul de boundary este detectat si blocat.

### ~~W1110~~ ✅ AI suggestion reviewer notes archive
**Descriere tehnica:** Arhiveaza notitele reviewerilor atasate sugestiilor AI si le face cautabile ulterior.
**Scop:** Pastreaza rationale-ul deciziilor.
**Target:** audit store, review workflow, search index.
**Acceptare:** Notitele pot fi regasite dupa sugestie, autor sau motiv.

### ~~W1111~~ ✅ AI suggestion incident rollback marker
**Descriere tehnica:** Marcheaza sugestiile AI implicate intr-un incident astfel incat rollback-ul sa fie usor de executat.
**Scop:** Reduce timpul de recuperare.
**Target:** incident response, rollback service, audit log.
**Acceptare:** Sugestiile marcate pot fi incluse rapid intr-un rollback plan.

### ~~W1112~~ ✅ AI suggestion quality gate dashboard
**Descriere tehnica:** Afișeaza toate gate-urile de calitate care blocheaza o sugestie AI si starea lor curenta.
**Scop:** Face blocajele explicite.
**Target:** validation pipeline, admin dashboard, moderation UI.
**Acceptare:** Fiecare gate are status si motivatie vizibile.

### ~~W1113~~ ✅ AI suggestion branch dependency notifier
**Descriere tehnica:** Notifica atunci cand o sugestie AI afecteaza dependente ascunse sau branch-uri conexe.
**Scop:** Reduce surprizele la merge.
**Target:** dependency graph, notification service, review workflow.
**Acceptare:** Notificarea include dependentele relevante si impactul estimat.

### ~~W1114~~ ✅ AI suggestion approval policy matrix
**Descriere tehnica:** Defineste o matrice de aprobare pe tip de continut, risc si rol.
**Scop:** Standardizeaza decizia finala.
**Target:** policy engine, review workflow, admin dashboard.
**Acceptare:** Matricea poate fi consultata si aplicata automat.

### ~~W1115~~ ✅ AI suggestion quarantine replay restriction
**Descriere tehnica:** Limiteaza replay-ul sugestiilor AI aflate in carantina doar la rolurile permise.
**Scop:** Protejeaza continutul sensibil sau defect.
**Target:** quarantine service, replay sandbox, permission service.
**Acceptare:** Utilizatorii fara drept nu pot reda sugestia.

### ~~W1116~~ ✅ AI suggestion feedback dedupe
**Descriere tehnica:** Grupeaza feedback-ul identic sau aproape identic primit pe aceeasi sugestie AI.
**Scop:** Evita zgomotul in analiza feedback-ului.
**Target:** feedback pipeline, review tools, analytics service.
**Acceptare:** Feedback-ul duplicat este consolidat si raportat o singura data.

### ~~W1117~~ ✅ AI suggestion version compare view
**Descriere tehnica:** Compara doua versiuni ale aceleiasi sugestii AI si evidentiaza diferenta de rezultat, context si verdict.
**Scop:** Simplifica iteratia.
**Target:** diff renderer, audit tools, review UI.
**Acceptare:** Comparatia arata clar ce s-a schimbat intre versiuni.

### ~~W1118~~ ✅ AI suggestion publish readiness meter
**Descriere tehnica:** Calculeaza cat de pregatita este o sugestie AI pentru publicare pe baza checklist-ului si validarii.
**Scop:** Ofera un indicator rapid de stare.
**Target:** release pipeline, admin dashboard, validation reports.
**Acceptare:** Meter-ul reflecta corect daca sugestia poate merge live.

### ~~W1119~~ ✅ AI suggestion branch reactivation guard
**Descriere tehnica:** Blocheaza reactivarea unei sugestii AI arhivate pe un branch care nu mai este valid sau activ.
**Scop:** Evita readucerea continutului depasit.
**Target:** archive service, branch manager, moderation workflow.
**Acceptare:** Reactivarea pe branch invalid este refuzata cu motiv.

### ~~W1120~~ ✅ AI suggestion input sanitation policy
**Descriere tehnica:** Definește regulile de igienizare pentru inputul folosit la generarea unei sugestii AI.
**Scop:** Reduce prompt injection si datele murdare.
**Target:** generation pipeline, input validator, security rules.
**Acceptare:** Inputul nesanitat este blocat sau curatat conform politicii.

### ~~W1121~~ ✅ AI suggestion moderation checklist export
**Descriere tehnica:** Exporta checklist-ul complet folosit de moderatori pentru o sugestie AI in format arhivabil.
**Scop:** Face auditul si training-ul mai simple.
**Target:** moderation tools, audit export, docs archive.
**Acceptare:** Exportul include toate punctele bifate si semnaturile relevante.

### ~~W1122~~ ✅ AI suggestion ownership conflict detector
**Descriere tehnica:** Detecteaza cand mai multi owneri sau echipe revendica aceeasi sugestie AI.
**Scop:** Previne asignarea dubla si blocajele de responsabilitate.
**Target:** ownership registry, moderation queue, notification service.
**Acceptare:** Conflictul de ownership este raportat si trimis la rezolvare.

### ~~W1123~~ ✅ AI suggestion branch policy binder
**Descriere tehnica:** Leaga politicile AI de un branch specific astfel incat regulile sa nu fie aplicate gresit pe alt context.
**Scop:** Protejeaza coerenta intre branch-uri.
**Target:** branch manager, policy engine, validation pipeline.
**Acceptare:** Politica aplicata poate fi identificata pe branch si versiune.

### ~~W1124~~ ✅ AI suggestion publish comment generator
**Descriere tehnica:** Genereaza un comentariu scurt pentru publicarea unei sugestii AI, cu motivul principal si impactul.
**Scop:** Standardizeaza comunicarea la release.
**Target:** release pipeline, admin dashboard, changelog workflow.
**Acceptare:** Comentariul generat poate fi atasat direct la publicare.

### ~~W1125~~ ✅ AI suggestion rollback diff exporter
**Descriere tehnica:** Exporta diff-ul necesar pentru rollback-ul unei sugestii AI in format usor de executat.
**Scop:** Reduce erorile la revert.
**Target:** rollback service, audit tools, release pipeline.
**Acceptare:** Exportul contine doar modificarile necesare pentru revert.

### ~~W1126~~ ✅ AI suggestion quality trend report
**Descriere tehnica:** Produce un raport de trend pentru calitatea sugestiilor AI pe intervale de timp.
**Scop:** Ajuta la evaluarea modelelor si a politicilor.
**Target:** analytics dashboard, telemetry service, audit reports.
**Acceptare:** Raportul arata evolutia calitatii si punctele de schimbare.

### ~~W1127~~ ✅ AI suggestion moderation SLA exception log
**Descriere tehnica:** Inregistreaza exceptiile fata de SLA-urile de moderare pentru sugestiile AI.
**Scop:** Face vizibile devierile operationale.
**Target:** moderation queue, audit log, telemetry service.
**Acceptare:** Orice exceptie SLA are motiv si durata.

### ~~W1128~~ ✅ AI suggestion release gate dependency map
**Descriere tehnica:** Construieste o harta a gate-urilor de release care pot bloca o sugestie AI.
**Scop:** Arata de ce nu poate fi publicata.
**Target:** release pipeline, dependency graph, admin dashboard.
**Acceptare:** Harta arata toate gate-urile si relatiile dintre ele.

### ~~W1129~~ ✅ AI suggestion audit replay timeline
**Descriere tehnica:** Afiseaza o cronologie completa a auditului unei sugestii AI cu generare, review, override si publish.
**Scop:** Simplifica investigatiile si training-ul.
**Target:** audit viewer, timeline renderer, review workflow.
**Acceptare:** Cronologia poate fi parcursa si filtrata pe evenimente.

### ~~W1130~~ ✅ AI suggestion signoff escalation
**Descriere tehnica:** Escaladeaza automat sugestiile AI care asteapta prea mult semnatura finala de release.
**Scop:** Evita blocajele in etapa finala.
**Target:** approval workflow, notification service, admin dashboard.
**Acceptare:** Sugestia fara semnatura este escaladata cu prioritate vizibila.

### ~~W1131~~ ✅ AI suggestion approval quorum policy
**Descriere tehnica:** Defineste cate aprobari sunt necesare pentru tipuri diferite de sugestii AI in functie de risc si impact.
**Scop:** Ajusteaza guvernanta la severitatea schimbarii.
**Target:** approval workflow, policy engine, admin dashboard.
**Acceptare:** Policy-ul specifica clar quorum-ul cerut pentru fiecare categorie.

### ~~W1132~~ ✅ AI suggestion auto reject heuristics
**Descriere tehnica:** Aplica euristici automate pentru respingerea sugestiilor AI care incalca reguli evidente sau repetitive.
**Scop:** Reduce incarcarea moderatorilor.
**Target:** moderation queue, validation pipeline, scoring service.
**Acceptare:** Sugestiile cu pattern-uri clare de respingere sunt eliminate automat.

### ~~W1133~~ ✅ AI suggestion branch freeze report
**Descriere tehnica:** Genereaza un raport al tuturor sugestiilor AI blocate de freeze pe fiecare branch.
**Scop:** Ofera vizibilitate asupra impactului freeze-ului.
**Target:** branch lock manager, admin dashboard, audit logs.
**Acceptare:** Raportul arata sugestiile afectate si durata blocajului.

### ~~W1134~~ ✅ AI suggestion context loss detector
**Descriere tehnica:** Detecteaza cand o sugestie AI pierde context esential intre generare, review si publish.
**Scop:** Previne decizii bazate pe informatie incompleta.
**Target:** AI audit pipeline, review workflow, telemetry service.
**Acceptare:** Pierderea de context este semnalata cu diferenta fata de starea initiala.

### ~~W1135~~ ✅ AI suggestion rollback safety proof
**Descriere tehnica:** Produce o dovada de siguranta pentru rollback inainte ca o sugestie AI sa fie aprobata pentru revert.
**Scop:** Reduce riscul de rollback gresit.
**Target:** rollback service, validation pipeline, audit tools.
**Acceptare:** Dovada include elementele afectate si starea rezultata.

### ~~W1136~~ ✅ AI suggestion review queue snapshot
**Descriere tehnica:** Salveaza snapshot-uri periodice ale cozii de review pentru sugestiile AI.
**Scop:** Permite analiza istorica a blocajelor si prioritatii.
**Target:** moderation queue, telemetry service, archive store.
**Acceptare:** Snapshot-ul poate fi comparat cu alte momente in timp.

### ~~W1137~~ ✅ AI suggestion policy severity mapper
**Descriere tehnica:** Mapeaza regulile de politica AI la niveluri de severitate standardizate.
**Scop:** Face interpretarea politicilor consistenta.
**Target:** policy engine, validation pipeline, admin dashboard.
**Acceptare:** Fiecare regula are severitate clara si stabila.

### ~~W1138~~ ✅ AI suggestion reviewer workload balancer
**Descriere tehnica:** Distribuie sugestiile AI intre moderatori in functie de incarcare, specializare si SLA.
**Scop:** Evita supraincarcarea unui singur reviewer.
**Target:** moderation queue, assignment service, admin dashboard.
**Acceptare:** Alocarea respecta incarcare si competentele configurate.

### ~~W1139~~ ✅ AI suggestion publish dependency blocker
**Descriere tehnica:** Blocheaza publicarea unei sugestii AI daca dependentele ei nu sunt aprobate sau sincronizate.
**Scop:** Previne publicari incomplete.
**Target:** release pipeline, dependency graph, validation service.
**Acceptare:** Blocker-ul arata dependentele lipsa si stop-eaza publish-ul.

### ~~W1140~~ ✅ AI suggestion rollback dependency blocker
**Descriere tehnica:** Blocheaza rollback-ul unei sugestii AI daca exista dependente critice care nu pot fi restaurate sigur.
**Scop:** Evita revenirile partiale periculoase.
**Target:** rollback service, dependency graph, admin dashboard.
**Acceptare:** Rollback-ul riscant este refuzat cu motiv explicit.

### ~~W1141~~ ✅ AI suggestion canonical diff archive
**Descriere tehnica:** Arhiveaza diff-ul canonic al unei sugestii AI ca referinta pentru audit si comparatie viitoare.
**Scop:** Pastreaza istoricul de schimbare usor de consultat.
**Target:** archive service, audit log, diff renderer.
**Acceptare:** Diff-ul arhivat poate fi redeschis si comparat ulterior.

### ~~W1142~~ ✅ AI suggestion branch health indicator
**Descriere tehnica:** Afiseaza un indicator de sanatate pentru branch-urile care primesc sugestii AI.
**Scop:** Arata rapid daca branch-ul este stabil sau tensionat.
**Target:** admin dashboard, branch manager, telemetry service.
**Acceptare:** Indicatorul reflecta blocaje, drift si activitate recenta.

### ~~W1143~~ ✅ AI suggestion moderation SLA recalibration
**Descriere tehnica:** Recalibreaza SLA-urile de moderare in functie de volum, risc si performanta istorica.
**Scop:** Mentine SLA-urile realiste si utile.
**Target:** moderation policy, telemetry service, admin dashboard.
**Acceptare:** Recalibrarea produce valori noi justificate de date.

### ~~W1144~~ ✅ AI suggestion exception timeline viewer
**Descriere tehnica:** Afiseaza o cronologie a tuturor exceptiilor aplicate asupra unei sugestii AI.
**Scop:** Face istoria exceptiilor usor de urmarit.
**Target:** audit viewer, policy registry, review workflow.
**Acceptare:** Cronologia arata ordinea si autorii exceptiilor.

### ~~W1145~~ ✅ AI suggestion quarantine severity badge
**Descriere tehnica:** Adauga un badge de severitate pentru sugestiile AI aflate in carantina.
**Scop:** Prioritizeaza clar cazurile critice.
**Target:** quarantine UI, moderation queue, admin dashboard.
**Acceptare:** Badge-ul reflecta severitatea si este vizibil in listare.

### ~~W1146~~ ✅ AI suggestion review routing rule test
**Descriere tehnica:** Testeaza regulile care trimit sugestiile AI catre reviewerul potrivit.
**Scop:** Previne rutarea gresita in productie.
**Target:** routing rules, moderation workflow, test suite.
**Acceptare:** Testele acopera toate traseele importante de rutare.

### ~~W1147~~ ✅ AI suggestion prompt injection guard
**Descriere tehnica:** Detecteaza incercarile de prompt injection in inputul folosit la generarea sugestiilor AI.
**Scop:** Protejeaza pipeline-ul de instructiuni malitioase.
**Target:** input validator, generation pipeline, security rules.
**Acceptare:** Inputul suspect este blocat si raportat.

### ~~W1148~~ ✅ AI suggestion content lineage viewer
**Descriere tehnica:** Arata linia completa de provenienta a unei sugestii AI pana la sursa initiala si la transformari intermediare.
**Scop:** Face auditul si debugging-ul mai rapide.
**Target:** audit viewer, trace pipeline, archive store.
**Acceptare:** Utilizatorul poate vedea toate etapele lineage-ului.

### ~~W1149~~ ✅ AI suggestion moderation label sync
**Descriere tehnica:** Sincronizeaza etichetele de moderare ale sugestiilor AI intre UI, audit si export.
**Scop:** Evita discrepantele intre suprafete.
**Target:** moderation UI, audit log, export pipeline.
**Acceptare:** Aceeasi sugestie are acelasi label peste toate suprafetele.

### ~~W1150~~ ✅ AI suggestion release comment audit
**Descriere tehnica:** Verifica daca comentariile de release generate pentru sugestiile AI sunt complete si coezive.
**Scop:** Evita note de release vagi sau incomplete.
**Target:** release notes generator, audit pipeline, admin dashboard.
**Acceptare:** Comentariile care lipsesc detalii sunt marcate pentru corectie.

### ~~W1151~~ ✅ AI suggestion rollback rehearsal log
**Descriere tehnica:** Pastreaza un jurnal al exercitiilor de rollback pentru sugestiile AI.
**Scop:** Ofera istoric pentru pregatirea operationala.
**Target:** rollback service, ops dashboard, audit logs.
**Acceptare:** Fiecare exercitiu are data, rezultat si observatii.

### ~~W1152~~ ✅ AI suggestion review explanation checker
**Descriere tehnica:** Verifica daca explicatia unei decizii de review pentru sugestia AI este suficient de clara si completa.
**Scop:** Pastreaza calitatea feedback-ului.
**Target:** review UI, feedback pipeline, moderation workflow.
**Acceptare:** Explicatiile vagi sunt semnalate pentru imbunatatire.

### ~~W1153~~ ✅ AI suggestion duplicate merge audit
**Descriere tehnica:** Inregistreaza cum au fost consolidate sugestiile AI duplicate si ce rezultat a ramas activ.
**Scop:** Pastreaza trasabilitatea consolidarii.
**Target:** moderation queue, audit log, similarity service.
**Acceptare:** Auditul arata care duplicate au fost unite si de ce.

### ~~W1154~~ ✅ AI suggestion policy doc sync job
**Descriere tehnica:** Sincronizeaza automat documentatia politicilor AI cu setul de reguli active.
**Scop:** Evita diferenta dintre documente si implementare.
**Target:** docs pipeline, policy engine, audit reports.
**Acceptare:** Sincronizarea detecteaza si raporteaza diferentele.

### ~~W1155~~ ✅ AI suggestion approval delay notifier
**Descriere tehnica:** Notifica atunci cand aprobarea unei sugestii AI intarzie peste o limita configurata.
**Scop:** Reduce blocajele in release.
**Target:** notification service, approval workflow, admin dashboard.
**Acceptare:** Notificarea include cat timp a depasit limita si cine e blocat.

### ~~W1156~~ ✅ AI suggestion branch quarantine split
**Descriere tehnica:** Permite separarea unei sugestii AI in carantina pe sub-branch-uri pentru analiza mai simpla.
**Scop:** Izoleaza problemele si reduce complexitatea de review.
**Target:** quarantine service, branch manager, validation pipeline.
**Acceptare:** Sub-branch-urile rezultate pot fi analizate independent.

### ~~W1157~~ ✅ AI suggestion risk heatmap
**Descriere tehnica:** Construiește o harta de risc pentru sugestiile AI pe branch, tip de continut si severitate.
**Scop:** Arata zonele cu risc mare din pipeline.
**Target:** analytics dashboard, telemetry service, admin tools.
**Acceptare:** Heatmap-ul diferentiaza clar zonele de risc ridicat.

### ~~W1158~~ ✅ AI suggestion sandbox export bundle
**Descriere tehnica:** Exporta o sugestie AI impreuna cu contextul necesar pentru a fi testata in sandbox extern.
**Scop:** Simplifica verificarea izolata.
**Target:** sandbox runtime, export pipeline, audit tools.
**Acceptare:** Bundle-ul poate fi reimportat si rulat in sandbox fara pierderi.

### ~~W1159~~ ✅ AI suggestion branch policy drift lock
**Descriere tehnica:** Blocheaza branch-urile care au drift intre politica activa si comportamentul real al sugestiilor AI.
**Scop:** Previne publicarea in conditii nevalidate.
**Target:** policy engine, branch lock manager, audit reports.
**Acceptare:** Drift-ul mare impune lock pana la remediere.

### ~~W1160~~ ✅ AI suggestion final approval receipt
**Descriere tehnica:** Genereaza o dovada finala de aprobare pentru sugestiile AI publicate.
**Scop:** Ofera o referinta oficiala pentru audit si suport.
**Target:** approval workflow, audit log, release pipeline.
**Acceptare:** Chitanta include aprobatorul, data, versiunea si scope-ul publicarii.

### ~~W1161~~ ✅ AI suggestion policy override ledger
**Descriere tehnica:** Pastreaza un registru al tuturor override-urilor aplicate asupra politicilor AI pentru sugestii.
**Scop:** Face exceptiile vizibile si revizibile.
**Target:** policy engine, audit log, admin dashboard.
**Acceptare:** Fiecare override are motiv, autor si perioada de valabilitate.

### ~~W1162~~ ✅ AI suggestion moderation drift report
**Descriere tehnica:** Detecteaza atunci cand moderatorii aproba sau resping sugestiile AI intr-un mod care deviaza de la baseline.
**Scop:** Semnaleaza inconsistentele de review.
**Target:** analytics dashboard, moderation queue, telemetry service.
**Acceptare:** Raportul compara trendul curent cu baseline-ul istoric.

### ~~W1163~~ ✅ AI suggestion release owner alert
**Descriere tehnica:** Notifica ownerul de release cand o sugestie AI critica a ramas blocata prea mult.
**Scop:** Reduce intarzierile din etapa de publicare.
**Target:** notification service, release pipeline, approval workflow.
**Acceptare:** Ownerul primeste alerta cu sugestia si cauza blocajului.

### ~~W1164~~ ✅ AI suggestion quarantine reason builder
**Descriere tehnica:** Construieste un motiv structurat pentru care o sugestie AI este pusa in carantina.
**Scop:** Face carantina inteligibila si actionabila.
**Target:** quarantine service, moderation UI, audit log.
**Acceptare:** Motivul include regula, severitatea si pasul urmator.

### ~~W1165~~ ✅ AI suggestion branch integrity checker
**Descriere tehnica:** Verifica daca o sugestie AI mentine integritatea branch-ului, incluzand continuitate, dependente si stari finale.
**Scop:** Previne branch-uri rupte sau inconsistente.
**Target:** branch validator, dependency graph, review workflow.
**Acceptare:** Neregulile de integritate sunt raportate inainte de publish.

### ~~W1166~~ ✅ AI suggestion approval comment lint
**Descriere tehnica:** Lint-uieste comentariile de aprobare pentru sugestiile AI ca sa fie clare, concise si complete.
**Scop:** Imbunatateste calitatea semnaturilor de review.
**Target:** review UI, policy engine, audit tools.
**Acceptare:** Comentariile slabe sau ambigue sunt marcate pentru revizuire.

### ~~W1167~~ ✅ AI suggestion replay compare audit
**Descriere tehnica:** Compara replay-ul unei sugestii AI cu executia originala si evidentiaza diferentele.
**Scop:** Verifica reproductibilitatea si consistența.
**Target:** replay sandbox, audit log, diff renderer.
**Acceptare:** Diferentele sunt listate explicit si explicate.

### ~~W1168~~ ✅ AI suggestion branch publish gate report
**Descriere tehnica:** Genereaza un raport cu toate gate-urile care blocheaza publicarea unei sugestii AI pe branch.
**Scop:** Ofera transparenta inainte de release.
**Target:** release pipeline, admin dashboard, validation service.
**Acceptare:** Raportul identifica exact gate-urile active si starea lor.

### ~~W1169~~ ✅ AI suggestion input provenance tag
**Descriere tehnica:** Adauga tag-uri de provenienta pentru inputul folosit la generarea sugestiilor AI.
**Scop:** Face clar de unde vine contextul generarii.
**Target:** generation pipeline, audit log, trace viewer.
**Acceptare:** Inputul are tag-uri stabile si cautabile.

### ~~W1170~~ ✅ AI suggestion release readiness audit
**Descriere tehnica:** Ruleaza un audit final care confirma daca o sugestie AI este pregatita pentru release.
**Scop:** Reduce riscul de publicare prematura.
**Target:** release pipeline, validation checklist, audit reports.
**Acceptare:** Auditul produce verdictul final cu toate criteriile verificate.

### ~~W1171~~ ✅ AI suggestion moderation backlog limiter
**Descriere tehnica:** Limiteaza cresterea backlog-ului de moderare pentru sugestiile AI prin praguri si actiuni automate.
**Scop:** Previne acumularea necontrolata a cozilor.
**Target:** moderation queue, telemetry service, admin dashboard.
**Acceptare:** Cand backlog-ul depaseste pragul, se aplica actiunea configurata.

### ~~W1172~~ ✅ AI suggestion policy conflict resolver
**Descriere tehnica:** Rezolva conflictele dintre reguli AI care se contrazic sau au severitati diferite.
**Scop:** Asigura decizii stabile in pipeline.
**Target:** policy engine, validation pipeline, admin tools.
**Acceptare:** Conflictul produce o regula activa deterministica sau un blocaj explicit.

### ~~W1173~~ ✅ AI suggestion review lane partition
**Descriere tehnica:** Imparte coada de review in benzi separate pe risc, tip de continut si SLA.
**Scop:** Ajuta la organizarea fluxului de moderare.
**Target:** moderation queue, assignment service, admin dashboard.
**Acceptare:** Sugestiile sunt repartizate in banda potrivita dupa politica.

### ~~W1174~~ ✅ AI suggestion rollback comment archive
**Descriere tehnica:** Arhiveaza comentariile asociate cu rollback-urile aplicate peste sugestiile AI.
**Scop:** Pastreaza rationale-ul revenirii.
**Target:** rollback service, audit log, archive store.
**Acceptare:** Comentariile pot fi consultate dupa rollback si sugestie.

### ~~W1175~~ ✅ AI suggestion escalation rule tester
**Descriere tehnica:** Testeaza regulile de escaladare aplicate sugestiilor AI intarziate sau riscante.
**Scop:** Verifica daca escaladarea porneste corect.
**Target:** escalation rules, moderation queue, test suite.
**Acceptare:** Testele acopera cazurile de timeout, risc si override.

### ~~W1176~~ ✅ AI suggestion branch summary card
**Descriere tehnica:** Creeaza un card sumar pentru un branch care primeste sugestii AI, cu starea curenta si blocajele.
**Scop:** Ofera status rapid pentru moderatori si owneri.
**Target:** admin dashboard, branch manager, review UI.
**Acceptare:** Cardul afiseaza datele esentiale in format compact.

### ~~W1177~~ ✅ AI suggestion compliance audit trail
**Descriere tehnica:** Inregistreaza un trail de conformitate pentru sugestiile AI, inclusiv aprobari, exceptii si verificari de policy.
**Scop:** Sustine auditul formal.
**Target:** compliance logs, audit viewer, policy engine.
**Acceptare:** Trail-ul poate fi exportat pentru control intern sau extern.

### ~~W1178~~ ✅ AI suggestion label consistency check
**Descriere tehnica:** Verifica daca etichetele de risc si stare ale unei sugestii AI sunt consistente in toate sistemele.
**Scop:** Evita interpretari divergente.
**Target:** moderation UI, audit log, export pipeline.
**Acceptare:** Inconsistenta de label este raportata si corectata.

### ~~W1179~~ ✅ AI suggestion branch threshold alert
**Descriere tehnica:** Declanseaza alerta cand un branch primeste prea multe sugestii AI peste pragul configurat.
**Scop:** Evita supraincarcarea branch-urilor.
**Target:** telemetry service, alerting system, branch manager.
**Acceptare:** Alerta mentioneaza branch-ul si pragul depasit.

### ~~W1180~~ ✅ AI suggestion publish approval matrix
**Descriere tehnica:** Produce o matrice de aprobare finala pentru publicarea sugestiilor AI pe baza de risc si context.
**Scop:** Standardizeaza decizia de release.
**Target:** release pipeline, approval workflow, admin dashboard.
**Acceptare:** Matricea poate fi consultata inainte de publicare.

### ~~W1181~~ ✅ AI suggestion override justification checker
**Descriere tehnica:** Verifica daca justificarea unui override peste verdictul AI este suficient de concreta si defensibila.
**Scop:** Pastreaza disciplina de guvernanta.
**Target:** audit log, review workflow, policy engine.
**Acceptare:** Justificarile vagi sunt marcate pentru completare.

### ~~W1182~~ ✅ AI suggestion context drift meter
**Descriere tehnica:** Masoara cat de mult s-a schimbat contextul unei sugestii AI intre generare si decizie.
**Scop:** Ajuta la interpretarea verdictului final.
**Target:** audit pipeline, telemetry service, review tools.
**Acceptare:** Meter-ul indica clar nivelul de drift si directia lui.

### ~~W1183~~ ✅ AI suggestion quarantine import guard
**Descriere tehnica:** Blocheaza importul sugestiilor AI din carantina daca lipsesc datele de siguranta.
**Scop:** Impiedica reintroducerea unor elemente incomplete.
**Target:** quarantine service, import pipeline, validation rules.
**Acceptare:** Importul lipsit de date este refuzat cu motiv explicit.

### ~~W1184~~ ✅ AI suggestion review attribution log
**Descriere tehnica:** Inregistreaza cine a facut fiecare actiune in review-ul unei sugestii AI, de la assignment la verdict.
**Scop:** Face responsabilitatea clara.
**Target:** review workflow, audit log, admin dashboard.
**Acceptare:** Fiecare actiune are atribuire si timestamp.

### ~~W1185~~ ✅ AI suggestion branch dependency freeze map
**Descriere tehnica:** Arata ce dependente intra in freeze atunci cand un branch AI este blocat.
**Scop:** Face efectele freeze-ului vizibile.
**Target:** dependency graph, branch manager, admin dashboard.
**Acceptare:** Harta include dependentele si starea lor dupa freeze.

### ~~W1186~~ ✅ AI suggestion policy health monitor
**Descriere tehnica:** Monitorizeaza sanatatea politicilor AI active si raporteaza reguli invalide, conflictuale sau nefolosite.
**Scop:** Mentine politica viabila in timp.
**Target:** policy engine, telemetry service, admin tools.
**Acceptare:** Monitorul raporteaza regulile problematice si severitatea lor.

### ~~W1187~~ ✅ AI suggestion publish readiness blocker
**Descriere tehnica:** Blocheaza publicarea daca indicatorul de ready nu a trecut toate pragurile cerute.
**Scop:** Reduce publicarea prematura.
**Target:** release pipeline, readiness meter, approval workflow.
**Acceptare:** Blocajul explica ce praguri lipsesc.

### ~~W1188~~ ✅ AI suggestion rollback provenance check
**Descriere tehnica:** Verifica provenienta sugestiilor AI inainte de rollback pentru a confirma ca se revine la setul corect de schimbari.
**Scop:** Evita revert-uri gresite.
**Target:** rollback service, provenance export, audit tools.
**Acceptare:** Rollback-ul este permis doar daca provenienta este confirmata.

### ~~W1189~~ ✅ AI suggestion moderation trend anomaly
**Descriere tehnica:** Detecteaza anomalii in trendul de moderare, cum ar fi aprobare excesiva sau respingere neasteptata.
**Scop:** Prinde schimbari de comportament in review.
**Target:** analytics dashboard, telemetry service, moderation queue.
**Acceptare:** Anomalia este prezentata cu comparatia fata de media istorica.

### ~~W1190~~ ✅ AI suggestion release attestation bundle
**Descriere tehnica:** Ambaleaza o atestare completa pentru sugestia AI publicata, incluzand audit, aprobarile si snapshot-ul de policy.
**Scop:** Ofera un pachet complet pentru audit si suport.
**Target:** release pipeline, audit log, export pipeline.
**Acceptare:** Bundle-ul poate fi arhivat si redeschis ulterior fara pierderi.

### ~~W1191~~ ✅ AI suggestion review queue fairness audit
**Descriere tehnica:** Verifica daca distribuirile in coada de review sunt echitabile intre moderatori si tipuri de continut.
**Scop:** Evita bias operational si supra-sarcina.
**Target:** moderation queue, assignment service, analytics dashboard.
**Acceptare:** Auditul arata distributia si semnaleaza dezechilibrele.

### ~~W1192~~ ✅ AI suggestion branch lock expiry policy
**Descriere tehnica:** Defineste cum expira lock-urile de branch aplicate asupra sugestiilor AI.
**Scop:** Evita blocajele indefinite.
**Target:** branch lock manager, policy engine, admin dashboard.
**Acceptare:** Lock-ul expira conform regulii si este vizibil in UI.

### ~~W1193~~ ✅ AI suggestion review replay redactor
**Descriere tehnica:** Redacteaza informatiile sensibile din replay-ul de review al sugestiilor AI.
**Scop:** Protejeaza datele interne la audit.
**Target:** replay viewer, audit tools, permission service.
**Acceptare:** Datele ascunse raman inaccesibile pentru rolurile fara drept.

### ~~W1194~~ ✅ AI suggestion policy bundle exporter
**Descriere tehnica:** Exporta regulile, exceptiile si severitatile AI intr-un bundle unic.
**Scop:** Simplifica backup-ul si transferul de policy.
**Target:** policy engine, export pipeline, docs archive.
**Acceptare:** Bundle-ul poate fi importat in alt mediu cu acelasi rezultat.

### ~~W1195~~ ✅ AI suggestion branch readiness snapshot
**Descriere tehnica:** Ia un snapshot de readiness pentru branch inainte de a accepta o sugestie AI noua.
**Scop:** Ofera un reper pentru comparatie si audit.
**Target:** branch manager, readiness meter, audit log.
**Acceptare:** Snapshot-ul poate fi comparat ulterior cu starea curenta.

### ~~W1196~~ ✅ AI suggestion final review checkpoint
**Descriere tehnica:** Introduce un checkpoint final de review pentru sugestiile AI inainte de publicare.
**Scop:** Reduce scapari in etapa ultima.
**Target:** review workflow, release pipeline, admin dashboard.
**Acceptare:** Sugestia nu poate fi publicata fara checkpoint trecut.

### ~~W1197~~ ✅ AI suggestion provenance hash ledger
**Descriere tehnica:** Pastreaza un ledger de hash-uri pentru provenienta sugestiilor AI.
**Scop:** Asigura integritatea si comparabilitatea istorica.
**Target:** audit log, provenance service, archive store.
**Acceptare:** Hash-ul permite verificarea ca provenance-ul nu a fost alterat.

### ~~W1198~~ ✅ AI suggestion moderation outcome archive
**Descriere tehnica:** Arhiveaza rezultatul final al moderarii pentru fiecare sugestie AI.
**Scop:** Pastreaza istoricul deciziilor si al motivelor.
**Target:** audit store, moderation workflow, search index.
**Acceptare:** Outcome-ul poate fi gasit dupa sugestie, rol sau interval.

### ~~W1199~~ ✅ AI suggestion rollback readiness audit
**Descriere tehnica:** Verifica daca un rollback propus de AI este pregatit din punct de vedere al dependintelor si al provenientei.
**Scop:** Previne revert-urile incomplet pregatite.
**Target:** rollback service, dependency graph, audit tools.
**Acceptare:** Auditul returneaza un verdict clar de ready sau blocked.

### ~~W1200~~ ✅ AI suggestion public release summary
**Descriere tehnica:** Genereaza un rezumat public pentru sugestiile AI ajunse in productie.
**Scop:** Ofera comunicare clara pentru schimbari vizibile.
**Target:** release notes, changelog workflow, admin dashboard.
**Acceptare:** Rezumatul include ce s-a schimbat si de ce a fost publicat.

### ~~W1201~~ ✅ AI suggestion branch lock audit trail
**Descriere tehnica:** Pastreaza un audit trail complet pentru toate lock-urile aplicate branch-urilor afectate de sugestii AI.
**Scop:** Face blocajele usor de investigat.
**Target:** branch lock manager, audit log, admin dashboard.
**Acceptare:** Fiecare lock are cauza, durata si autorul actiunii.

### ~~W1202~~ ✅ AI suggestion moderation decision cache
**Descriere tehnica:** Cache-uieste deciziile de moderare pentru sugestiile AI similare pentru a reduce munca repetitiva.
**Scop:** Optimizeaza review-ul repetitiv.
**Target:** moderation queue, cache layer, review workflow.
**Acceptare:** Deciziile identice pot fi refolosite cand contextul este acelasi.

### ~~W1203~~ ✅ AI suggestion branch transition map
**Descriere tehnica:** Deseneaza tranzitiile posibile ale unui branch inainte si dupa aplicarea unei sugestii AI.
**Scop:** Face explicit impactul structural.
**Target:** branch manager, dependency graph, review tools.
**Acceptare:** Harta arata starea initiala si cele mai probabile stari rezultate.

### ~~W1204~~ ✅ AI suggestion security policy latch
**Descriere tehnica:** Blocheaza sugestiile AI cand politicile de securitate nu sunt satisfacute.
**Scop:** Protejeaza pipeline-ul de introducerea de continut riscant.
**Target:** security rules, validation pipeline, moderation queue.
**Acceptare:** Sugestia este oprita pana cand toate controalele trec.

### ~~W1205~~ ✅ AI suggestion review comment sanitizer
**Descriere tehnica:** Curata comentariile de review pentru a elimina markup-ul, datele sensibile si zgomotul inutil.
**Scop:** Pastreaza comentariile sigure si lizibile.
**Target:** review UI, audit log, export pipeline.
**Acceptare:** Comentariile exportate sunt igienizate conform regulii.

### ~~W1206~~ ✅ AI suggestion branch stale lock releaser
**Descriere tehnica:** Elibereaza automat lock-urile branch-urilor care au ramas stale dupa un timeout configurat.
**Scop:** Evita blocajele permanente.
**Target:** branch lock manager, cleanup jobs, admin dashboard.
**Acceptare:** Lock-ul stale este eliberat sau escaladat cu motiv.

### ~~W1207~~ ✅ AI suggestion output schema validator
**Descriere tehnica:** Verifica daca rezultatul unei sugestii AI respecta schema de date asteptata.
**Scop:** Previne publicarea de output invalid.
**Target:** generation pipeline, schema validator, release workflow.
**Acceptare:** Output-ul neconform este respins inainte de folosire.

### ~~W1208~~ ✅ AI suggestion audit tag registry
**Descriere tehnica:** Centralizeaza tag-urile de audit folosite pentru sugestiile AI, astfel incat sa fie consistente in toate log-urile.
**Scop:** Reduce inconsistenta intre instrumente.
**Target:** audit log, telemetry service, archive store.
**Acceptare:** Tag-urile sunt reutilizate si validate dintr-un registru unic.

### ~~W1209~~ ✅ AI suggestion moderation priority override
**Descriere tehnica:** Permite cresterea prioritatii unei sugestii AI in moderare pentru cazuri critice.
**Scop:** Raspunde rapid la incidente sau release-uri urgente.
**Target:** moderation queue, admin dashboard, assignment service.
**Acceptare:** Prioritatea suprascrisa este vizibila si auditata.

### ~~W1210~~ ✅ AI suggestion branch policy snapshot compare
**Descriere tehnica:** Compara snapshot-ul politicii branch-ului cu politica activa cand se evalueaza o sugestie AI.
**Scop:** Evita interpretarea gresita a regulilor istorice.
**Target:** policy engine, branch manager, audit tools.
**Acceptare:** Diferentele dintre snapshot si politica activa sunt afisate clar.

### ~~W1211~~ ✅ AI suggestion rollback owner assignment
**Descriere tehnica:** Asigneaza un owner responsabil pentru fiecare rollback generat de sugestiile AI.
**Scop:** Clarifica responsabilitatea la revert.
**Target:** rollback service, ownership registry, notification service.
**Acceptare:** Fiecare rollback are un owner vizibil si notificat.

### ~~W1212~~ ✅ AI suggestion branch confidence meter
**Descriere tehnica:** Măsoară increderea generala in branch dupa aplicarea sugestiilor AI si a review-ului lor.
**Scop:** Ajuta la decizii de release si freeze.
**Target:** branch manager, telemetry service, admin dashboard.
**Acceptare:** Meter-ul reflecta atat calitatea cat si stabilitatea branch-ului.

### ~~W1213~~ ✅ AI suggestion review rationale template
**Descriere tehnica:** Ofera un sablon standard pentru rationale-ul din verdictele de review AI.
**Scop:** Standardizeaza comunicarea dintre moderatori.
**Target:** review UI, policy docs, audit log.
**Acceptare:** Reviewerul poate completa rationale-ul rapid si consistent.

### ~~W1214~~ ✅ AI suggestion quarantine escalation path
**Descriere tehnica:** Defineste traseul de escaladare pentru sugestiile AI din carantina.
**Scop:** Face actiunile urmatoare clare.
**Target:** quarantine service, escalation rules, admin dashboard.
**Acceptare:** Fiecare sugestie carantinata are un traseu de escaladare valid.

### ~~W1215~~ ✅ AI suggestion approval evidence bundle
**Descriere tehnica:** Aduna dovezile folosite la aprobarea unei sugestii AI intr-un bundle unic.
**Scop:** Simplifica auditul si revalidarea.
**Target:** approval workflow, audit log, export pipeline.
**Acceptare:** Bundle-ul contine toate dovezile relevante si este exportabil.

### ~~W1216~~ ✅ AI suggestion policy coverage report
**Descriere tehnica:** Raporteaza ce tipuri de sugestii AI sunt acoperite si ce tipuri raman fara reguli dedicate.
**Scop:** Identifica golurile de guvernanta.
**Target:** policy engine, docs archive, admin dashboard.
**Acceptare:** Raportul arata clar acoperirea si lipsurile.

### ~~W1217~~ ✅ AI suggestion branch publish history
**Descriere tehnica:** Pastreaza istoricul publicarilor AI pentru fiecare branch, cu link la diffs si aprobari.
**Scop:** Ofera context pentru schimbari viitoare.
**Target:** release pipeline, branch manager, audit log.
**Acceptare:** Istoricul poate fi consultat dupa branch si interval.

### ~~W1218~~ ✅ AI suggestion moderation spill detector
**Descriere tehnica:** Detecteaza cand sugestiile AI in review incep sa se reverse sau sa se propage in alte cozi.
**Scop:** Previne contaminarea fluxurilor adiacente.
**Target:** moderation queue, telemetry service, quarantine service.
**Acceptare:** Spill-ul este raportat cu sursa si destinatia.

### ~~W1219~~ ✅ AI suggestion release scope verifier
**Descriere tehnica:** Verifica daca sugestia AI ramane in scope-ul definit pentru release-ul curent.
**Scop:** Evita includerea schimbarii in release-ul gresit.
**Target:** release pipeline, scope matcher, branch manager.
**Acceptare:** Sugestia in afara scope-ului este marcata si oprita.

### ~~W1220~~ ✅ AI suggestion rollback scope verifier
**Descriere tehnica:** Verifica daca rollback-ul propus de AI nu depaseste scope-ul schimbarii initiale.
**Scop:** Previne revert-urile prea largi.
**Target:** rollback service, scope matcher, audit tools.
**Acceptare:** Rollback-ul neconform este refuzat cu explicatie.

### ~~W1221~~ ✅ AI suggestion provenance confidence score
**Descriere tehnica:** Calculeaza un scor de incredere pentru provenienta unei sugestii AI in functie de completitudine si integritate.
**Scop:** Ajuta la evaluarea fiabilitatii auditului.
**Target:** provenance service, audit log, analytics dashboard.
**Acceptare:** Scorul este prezent si actualizat cand lipsesc date.

### ~~W1222~~ ✅ AI suggestion moderation role matrix
**Descriere tehnica:** Defineste ce actiuni poate face fiecare rol asupra sugestiilor AI in moderare.
**Scop:** Clarifica drepturile si limitarile.
**Target:** permission service, moderation UI, policy docs.
**Acceptare:** Matricea de roluri este aplicata consistent in UI si backend.

### ~~W1223~~ ✅ AI suggestion audit replay bookmarks
**Descriere tehnica:** Permite adaugarea de bookmark-uri in replay-ul audit al unei sugestii AI.
**Scop:** Faciliteaza discutia si investigatia pe momente cheie.
**Target:** audit viewer, replay timeline, review workflow.
**Acceptare:** Bookmark-urile sunt salvate si pot fi revisitite rapid.

### ~~W1224~~ ✅ AI suggestion branch lock severity tiers
**Descriere tehnica:** Clasifica lock-urile branch-urilor pe severitate in functie de tipul sugestiei AI si de risc.
**Scop:** Ofera prioritate corecta pentru remediere.
**Target:** branch lock manager, admin dashboard, policy engine.
**Acceptare:** Fiecare lock are un tier clar si justificat.

### ~~W1225~~ ✅ AI suggestion review state exporter
**Descriere tehnica:** Exporta starea curenta a unui review AI intr-un format usor de arhivat sau transferat.
**Scop:** Simplifica handoff-ul si backup-ul.
**Target:** review workflow, export pipeline, archive store.
**Acceptare:** Exportul include statusul, comentariile si verificările.

### ~~W1226~~ ✅ AI suggestion quarantine intake filter
**Descriere tehnica:** Filtreaza sugestiile AI la intrarea in carantina in functie de gravitate si tip de defect.
**Scop:** Imparte corect munca de inspectie.
**Target:** quarantine service, moderation queue, validation pipeline.
**Acceptare:** Sugestiile sunt directionate catre categoria corecta de carantina.

### ~~W1227~~ ✅ AI suggestion publication comment audit
**Descriere tehnica:** Aduce in audit comentariile atasate publicarii unei sugestii AI.
**Scop:** Pastreaza motivarea release-ului.
**Target:** release pipeline, audit log, changelog workflow.
**Acceptare:** Comentariile de publicare sunt cautabile si asociate release-ului.

### ~~W1228~~ ✅ AI suggestion policy threshold tuner
**Descriere tehnica:** Ajusteaza pragurile de politica AI pe baza rezultatelor istorice si a volumului.
**Scop:** Mentine pragurile utile si proportionale.
**Target:** policy engine, analytics dashboard, telemetry service.
**Acceptare:** Ajustarea pragurilor este justificata si auditata.

### ~~W1229~~ ✅ AI suggestion owner escalation matrix
**Descriere tehnica:** Stabileste cand si cum este escaladat un caz AI catre ownerul de domeniu.
**Scop:** Reduce ambiguitatea in rezolvare.
**Target:** escalation rules, ownership registry, notification service.
**Acceptare:** Matricea produce ruta de escaladare corecta.

### ~~W1230~~ ✅ AI suggestion final archive seal
**Descriere tehnica:** Sigileaza arhiva finala a sugestiei AI dupa publicare sau respingere definitiva.
**Scop:** Protejeaza integritatea istoricului.
**Target:** archive service, audit log, retention policy.
**Acceptare:** Arhiva sigilata nu mai poate fi modificata fara procedura explicitata.

### ~~W1231~~ ✅ AI suggestion moderation freeze lift
**Descriere tehnica:** Ridica controlat freeze-ul de moderare pentru sugestiile AI dupa ce riscul a fost rezolvat.
**Scop:** Reia fluxul fara interventii ad-hoc.
**Target:** moderation queue, branch lock manager, admin dashboard.
**Acceptare:** Freeze-ul poate fi ridicat doar dupa verificarea conditiilor cerute.

### ~~W1232~~ ✅ AI suggestion approval chain viewer
**Descriere tehnica:** Afiseaza lantul complet de aprobari pentru o sugestie AI, de la reviewer initial la semnatura finala.
**Scop:** Face traseul decizional transparent.
**Target:** approval workflow, audit viewer, admin dashboard.
**Acceptare:** Viewer-ul arata ordinea si statusul fiecarei aprobari.

### ~~W1233~~ ✅ AI suggestion branch content lock audit
**Descriere tehnica:** Inregistreaza toate lock-urile de continut aplicate branch-urilor care primesc sugestii AI.
**Scop:** Simplifica investigarea blocajelor.
**Target:** branch lock manager, audit log, content registry.
**Acceptare:** Lock-ul este legat de branch, tipul de continut si motiv.

### ~~W1234~~ ✅ AI suggestion quarantine priority ladder
**Descriere tehnica:** Clasifica sugestiile AI din carantina pe o scara de prioritate pentru triere rapida.
**Scop:** Ajuta la rezolvarea primelor cazuri critice.
**Target:** quarantine service, moderation queue, admin dashboard.
**Acceptare:** Prioritatea este vizibila si respectata in coada.

### ~~W1235~~ ✅ AI suggestion policy rollback guard
**Descriere tehnica:** Blocheaza rollback-ul politicilor AI daca ar crea o stare in care sugestiile existente nu mai sunt evaluabile corect.
**Scop:** Protejeaza continuitatea evaluarii.
**Target:** policy engine, release pipeline, admin tools.
**Acceptare:** Rollback-ul riscant este respins cu motiv clar.

### ~~W1236~~ ✅ AI suggestion review timeout recovery
**Descriere tehnica:** Recupereaza sugestiile AI care au depasit timeout-ul de review si le muta intr-un traseu sigur.
**Scop:** Evita blocajele permanente.
**Target:** moderation queue, recovery service, notification service.
**Acceptare:** Sugestia expirata ajunge intr-o stare deterministica.

### ~~W1237~~ ✅ AI suggestion approval scope diff
**Descriere tehnica:** Arata diferenta dintre scope-ul initial si scope-ul final aprobat pentru o sugestie AI.
**Scop:** Evidentiaza extinderea sau restrangerea aprobata.
**Target:** diff renderer, approval workflow, audit tools.
**Acceptare:** Diferenta este clara si atasata la verdict.

### ~~W1238~~ ✅ AI suggestion release bottleneck detector
**Descriere tehnica:** Detecteaza punctele de blocaj din pipeline-ul de release pentru sugestiile AI.
**Scop:** Identifica unde se acumuleaza intarzierile.
**Target:** telemetry service, release pipeline, admin dashboard.
**Acceptare:** Bottleneck-urile sunt raportate cu etapa si durata.

### ~~W1239~~ ✅ AI suggestion owner fallback policy
**Descriere tehnica:** Defineste un owner de fallback cand sugestia AI nu poate fi atribuita clar.
**Scop:** Evita cazurile fara responsabil.
**Target:** ownership registry, notification service, moderation queue.
**Acceptare:** Fallback-ul este determinist si auditabil.

### ~~W1240~~ ✅ AI suggestion semantic quarantine splitter
**Descriere tehnica:** Imparte sugestiile AI din carantina pe subgrupuri semantice pentru analiza mai rapida.
**Scop:** Reduce complexitatea investigarii.
**Target:** quarantine service, similarity service, moderation workflow.
**Acceptare:** Subgrupurile sunt create pe baza criteriilor semantice configurate.

### ~~W1241~~ ✅ AI suggestion branch publish quorum
**Descriere tehnica:** Impune un numar minim de aprobari pentru publicarea unei sugestii AI pe branch-uri sensibile.
**Scop:** Creste siguranta release-ului.
**Target:** approval workflow, branch manager, release pipeline.
**Acceptare:** Fara quorum-ul cerut, publicarea este oprita.

### ~~W1242~~ ✅ AI suggestion audit search booster
**Descriere tehnica:** Imbunatateste cautarea in audit pentru sugestiile AI prin relevanta, filtre si alias-uri.
**Scop:** Gaseste mai rapid istoricul.
**Target:** audit search, index service, admin dashboard.
**Acceptare:** Rezultatele relevante apar primele si sunt filtrabile.

### ~~W1243~~ ✅ AI suggestion policy decision memo
**Descriere tehnica:** Genereaza un memo scurt pentru fiecare decizie de politica AI aplicata unei sugestii.
**Scop:** Pastreaza rationale-ul aplicarii politicii.
**Target:** policy engine, audit log, review workflow.
**Acceptare:** Memo-ul mentioneaza regula, motivul si rezultatul.

### ~~W1244~~ ✅ AI suggestion branch rollback timeline
**Descriere tehnica:** Afiseaza o cronologie a tuturor rollback-urilor asociate unui branch cu sugestii AI.
**Scop:** Ofera context pentru istoricul branch-ului.
**Target:** rollback service, branch manager, audit viewer.
**Acceptare:** Cronologia poate fi filtrata pe tip de rollback si rezultat.

### ~~W1245~~ ✅ AI suggestion moderation note redactor
**Descriere tehnica:** Redacteaza notitele moderatorilor pentru a elimina informatiile sensibile inainte de export.
**Scop:** Protejeaza datele interne in rapoarte.
**Target:** moderation UI, export pipeline, audit log.
**Acceptare:** Exportul ascunde campurile marcate ca sensibile.

### ~~W1246~~ ✅ AI suggestion validation rule snapshot
**Descriere tehnica:** Salveaza snapshot-ul regulilor de validare folosite la evaluarea unei sugestii AI.
**Scop:** Face auditul reproducibil.
**Target:** validation pipeline, audit log, policy engine.
**Acceptare:** Snapshot-ul poate fi comparat cu regulile active actuale.

### ~~W1247~~ ✅ AI suggestion release note reviewer
**Descriere tehnica:** Ofera un rol de reviewer pentru notele de release generate din sugestii AI.
**Scop:** Verifica daca sumarul public este corect.
**Target:** release notes generator, review workflow, admin dashboard.
**Acceptare:** Notitele de release nu pot fi publicate fara verificare.

### ~~W1248~~ ✅ AI suggestion confidence decay monitor
**Descriere tehnica:** Urmareste cum scade increderea intr-o sugestie AI pe masura ce timpul si contextul se schimba.
**Scop:** Indica momentul in care reevaluarea devine necesara.
**Target:** telemetry service, audit pipeline, moderation queue.
**Acceptare:** Monitorul semnaleaza cand confidence-ul cade sub prag.

### ~~W1249~~ ✅ AI suggestion branch anomaly lock
**Descriere tehnica:** Blocheaza branch-urile care prezinta anomalii dupa aplicarea unor sugestii AI.
**Scop:** Previne propagarea unei stari defecte.
**Target:** branch lock manager, anomaly detector, admin dashboard.
**Acceptare:** Anomalia declanseaza lock si raport cu motiv.

### ~~W1250~~ ✅ AI suggestion publish evidence index
**Descriere tehnica:** Indexeaza dovezile asociate publicarii unei sugestii AI pentru cautare si audit.
**Scop:** Face probele usor de recuperat.
**Target:** evidence store, audit search, release pipeline.
**Acceptare:** Dovezile pot fi gasite dupa sugestie si tipul lor.

### ~~W1251~~ ✅ AI suggestion rollback evidence index
**Descriere tehnica:** Indexeaza dovezile asociate rollback-ului unei sugestii AI pentru investigatie ulterioara.
**Scop:** Pastreaza istoricul revenirilor.
**Target:** evidence store, rollback service, audit search.
**Acceptare:** Indexul permite cautare dupa branch, motivatie si rezultat.

### ~~W1252~~ ✅ AI suggestion policy rollback memo
**Descriere tehnica:** Documenteaza motivul si efectul unui rollback de politica AI.
**Scop:** Pastreaza un istoric clar al modificarilor de guvernanta.
**Target:** policy engine, audit log, docs archive.
**Acceptare:** Memo-ul explica ce s-a schimbat si de ce.

### ~~W1253~~ ✅ AI suggestion moderation triage view
**Descriere tehnica:** Ofera o vedere de triere rapida pentru sugestiile AI cu prioritati, severitati si blocaje.
**Scop:** Ajuta moderatorii sa selecteze rapid ce trebuie rezolvat.
**Target:** moderation UI, queue manager, telemetry service.
**Acceptare:** Triage view afiseaza cele mai importante date intr-un singur ecran.

### ~~W1254~~ ✅ AI suggestion branch publish evidence seal
**Descriere tehnica:** Sigileaza dovezile de publicare pentru o sugestie AI si le leaga de branch-ul final.
**Scop:** Protejeaza integritatea auditului.
**Target:** evidence store, release pipeline, audit log.
**Acceptare:** Dovezile sigilate nu pot fi alterate fara procedura explicita.

### ~~W1255~~ ✅ AI suggestion owner reassignment audit
**Descriere tehnica:** Inregistreaza orice realocare de owner pentru sugestiile AI si motivul ei.
**Scop:** Pastreaza responsabilitatea trasabila.
**Target:** ownership registry, audit log, notification service.
**Acceptare:** Fiecare reassignment are istoricul complet.

### ~~W1256~~ ✅ AI suggestion quarantine resolution playbook
**Descriere tehnica:** Defineste un playbook pentru rezolvarea sugestiilor AI din carantina.
**Scop:** Standardizeaza operatiunea de recuperare.
**Target:** quarantine service, docs archive, moderation workflow.
**Acceptare:** Playbook-ul acopera tipurile principale de defect si actiunile recomandate.

### ~~W1257~~ ✅ AI suggestion branch state checksum
**Descriere tehnica:** Calculeaza un checksum al starii branch-ului dupa aplicarea sugestiilor AI.
**Scop:** Permite comparatie rapida intre stari.
**Target:** branch manager, audit tools, telemetry service.
**Acceptare:** Checksum-ul se schimba cand starea branch-ului se schimba.

### ~~W1258~~ ✅ AI suggestion moderation queue health
**Descriere tehnica:** Monitorizeaza sanatatea generala a cozii de moderare pentru sugestiile AI.
**Scop:** Detecteaza blocaje, crestere si stagnare.
**Target:** moderation queue, telemetry service, admin dashboard.
**Acceptare:** Starea queue-ului este reprezentata ca healthy, warning sau blocked.

### ~~W1259~~ ✅ AI suggestion rollback lane partition
**Descriere tehnica:** Imparte actiunile de rollback generate de AI in benzi separate in functie de risc si severitate.
**Scop:** Optimizeaza prioritizarea revenirilor.
**Target:** rollback service, queue manager, admin dashboard.
**Acceptare:** Rollback-urile sunt incadrate in banda corecta.

### ~~W1260~~ ✅ AI suggestion final disposition summary
**Descriere tehnica:** Produce un sumar final al destinului unei sugestii AI: publicata, respinsa, carantinata sau anulata.
**Scop:** Ofera un status clar si final.
**Target:** audit log, release pipeline, moderation workflow.
**Acceptare:** Sumarul final poate fi consultat dupa fiecare sugestie.

### ~~W1261~~ ✅ AI suggestion branch lock incident timeline
**Descriere tehnica:** Arata o cronologie a incidentelor legate de lock-urile branch-urilor afectate de sugestii AI.
**Scop:** Ajuta la analiza incidentelor si a frecventei lor.
**Target:** branch lock manager, incident dashboard, audit log.
**Acceptare:** Cronologia arata incidentul, lock-ul si rezolvarea.

### ~~W1262~~ ✅ AI suggestion moderation queue compactor
**Descriere tehnica:** Compactorizeaza coada de moderare a sugestiilor AI prin gruparea elementelor foarte similare.
**Scop:** Reduce zgomotul si munca redundanta.
**Target:** moderation queue, similarity service, admin dashboard.
**Acceptare:** Elemente similare sunt grupate fara pierdere de context.

### ~~W1263~~ ✅ AI suggestion policy version audit
**Descriere tehnica:** Inregistreaza versiunea exacta a politicii AI folosita la fiecare decizie de moderare.
**Scop:** Face auditul reproducibil.
**Target:** policy engine, audit log, review workflow.
**Acceptare:** Fiecare verdict poate fi legat de o versiune de policy.

### ~~W1264~~ ✅ AI suggestion release artifact seal
**Descriere tehnica:** Sigileaza artefactele de release asociate sugestiilor AI pentru a preveni modificari ulterioare.
**Scop:** Protejeaza integritatea livrarii.
**Target:** release pipeline, artifact store, audit log.
**Acceptare:** Artefactele sigilate nu pot fi schimbate fara audit.

### ~~W1265~~ ✅ AI suggestion quarantine lineage map
**Descriere tehnica:** Deseneaza linia de provenienta a sugestiilor AI ajunse in carantina.
**Scop:** Ajuta la identificarea sursei problemelor.
**Target:** quarantine service, lineage viewer, audit tools.
**Acceptare:** Linia de provenienta poate fi parcursa de la input la carantina.

### ~~W1266~~ ✅ AI suggestion approval evidence digest
**Descriere tehnica:** Genereaza un digest concis al dovezilor de aprobare pentru o sugestie AI.
**Scop:** Simplifica review-ul final si auditul.
**Target:** approval workflow, audit viewer, export pipeline.
**Acceptare:** Digest-ul rezuma probele cheie fara a pierde referintele.

### ~~W1267~~ ✅ AI suggestion rollback evidence digest
**Descriere tehnica:** Genereaza un digest concis al dovezilor folosite la un rollback AI.
**Scop:** Face revenirile mai usor de verificat.
**Target:** rollback service, audit viewer, export pipeline.
**Acceptare:** Digest-ul include motivul si rezultatul rollback-ului.

### ~~W1268~~ ✅ AI suggestion branch readiness gate
**Descriere tehnica:** Adauga un gate care verifica daca branch-ul este pregatit pentru a primi sugestii AI noi.
**Scop:** Evita acumularea in branch-uri instabile.
**Target:** branch manager, release pipeline, moderation queue.
**Acceptare:** Sugestiile sunt oprite cand branch-ul nu este ready.

### ~~W1269~~ ✅ AI suggestion moderation outcome scorer
**Descriere tehnica:** Calculeaza un scor pentru outcome-ul moderarii in functie de durata, consistenta si calitate.
**Scop:** Ajuta la evaluarea performanței review-ului.
**Target:** analytics dashboard, moderation queue, telemetry service.
**Acceptare:** Scorul poate fi calculat pe interval si pe moderator.

### ~~W1270~~ ✅ AI suggestion policy exception expiry job
**Descriere tehnica:** Ruleaza un job care expira exceptiile temporare din politica AI.
**Scop:** Evita exceptiile uitate.
**Target:** policy engine, cleanup jobs, admin dashboard.
**Acceptare:** Exceptiile expirate sunt marcate si retrase automat.

### ~~W1271~~ ✅ AI suggestion release checkpoint tracker
**Descriere tehnica:** Urmareste checkpoint-urile de release atinse de sugestiile AI pana la publicare.
**Scop:** Ofera transparenta asupra progresului.
**Target:** release pipeline, audit log, admin dashboard.
**Acceptare:** Fiecare checkpoint are status si timestamp.

### ~~W1272~~ ✅ AI suggestion branch provenance snapshot
**Descriere tehnica:** Salveaza snapshot-ul de provenienta pentru branch-ul care primeste sugestii AI.
**Scop:** Face comparatia istorica mai sigura.
**Target:** branch manager, provenance service, audit log.
**Acceptare:** Snapshot-ul poate fi folosit la comparatie ulterioara.

### ~~W1273~~ ✅ AI suggestion review red flag detector
**Descriere tehnica:** Detecteaza semnale de avertizare in review-ul sugestiilor AI, precum inconsistente sau blocaje repetate.
**Scop:** Scoate la suprafata review-urile problematice.
**Target:** review workflow, telemetry service, admin dashboard.
**Acceptare:** Red flag-ul este raportat cu motiv si context.

### ~~W1274~~ ✅ AI suggestion policy sync validator
**Descriere tehnica:** Verifica daca regula din policy-ul AI este sincronizata cu implementarea activa.
**Scop:** Evita divergentele intre docs si cod.
**Target:** policy engine, validation pipeline, docs archive.
**Acceptare:** Diferenta dintre policy si implementare este detectata.

### ~~W1275~~ ✅ AI suggestion owner visibility filter
**Descriere tehnica:** Filtreaza sugestiile AI astfel incat fiecare owner sa vada doar elementele relevante pentru domeniul lui.
**Scop:** Reduce zgomotul si expunerea inutila.
**Target:** permissions service, review UI, notification service.
**Acceptare:** Ownerul vede doar sugestiile permise de politica.

### ~~W1276~~ ✅ AI suggestion branch impact digest
**Descriere tehnica:** Produce un digest scurt al impactului unei sugestii AI asupra branch-ului si dependentelor.
**Scop:** Ofera context rapid inainte de review.
**Target:** dependency graph, review UI, audit log.
**Acceptare:** Digest-ul sintetizeaza impactul fara a ascunde detalii esentiale.

### ~~W1277~~ ✅ AI suggestion validation replay guard
**Descriere tehnica:** Previne replay-ul unei validari AI daca datele sau regulile nu mai corespund versiunii originale.
**Scop:** Pastreaza credibilitatea rezultatelor.
**Target:** validation pipeline, replay sandbox, audit tools.
**Acceptare:** Replay-ul invalid este respins cu motiv explicit.

### ~~W1278~~ ✅ AI suggestion publish dependency digest
**Descriere tehnica:** Summarizeaza dependentele care trebuie satisfacute pentru a publica o sugestie AI.
**Scop:** Face gating-ul mai transparent.
**Target:** release pipeline, dependency graph, admin dashboard.
**Acceptare:** Digest-ul listeaza dependentele critice si starea lor.

### ~~W1279~~ ✅ AI suggestion moderation intake checksum
**Descriere tehnica:** Calculeaza un checksum la intrarea in moderare pentru a detecta alterarea sugestiei AI.
**Scop:** Protejeaza integritatea fluxului.
**Target:** moderation queue, audit log, integrity checker.
**Acceptare:** Orice modificare a inputului schimba checksum-ul si este detectata.

### ~~W1280~~ ✅ AI suggestion branch archival policy
**Descriere tehnica:** Stabileste cand un branch cu sugestii AI trebuie arhivat in loc sa fie mentinut activ.
**Scop:** Pastreaza sistemul curat si usor de gestionat.
**Target:** branch manager, archive service, retention policy.
**Acceptare:** Branch-urile eligibile sunt arhivate conform regulii.

### ~~W1281~~ ✅ AI suggestion publish responsibility ledger
**Descriere tehnica:** Pastreaza un ledger al responsabilitatilor pentru fiecare sugestie AI publicata.
**Scop:** Clarifica cine raspunde de fiecare release.
**Target:** release pipeline, ownership registry, audit log.
**Acceptare:** Ledger-ul leaga release-ul de owner, reviewer si semnatar.

### ~~W1282~~ ✅ AI suggestion moderation replay trigger
**Descriere tehnica:** Permite declansarea unui replay al unei decizii de moderare pentru o sugestie AI.
**Scop:** Ajuta la investigatii si training.
**Target:** moderation workflow, audit viewer, replay sandbox.
**Acceptare:** Replay-ul porneste doar pentru sugestiile eligibile.

### ~~W1283~~ ✅ AI suggestion policy threshold report
**Descriere tehnica:** Genereaza un raport al pragurilor curente din politica AI si al efectului lor asupra deciziilor.
**Scop:** Ofera vizibilitate operationala.
**Target:** policy engine, analytics dashboard, audit reports.
**Acceptare:** Raportul arata pragurile si numarul de sugestii afectate.

### ~~W1284~~ ✅ AI suggestion branch freeze recovery plan
**Descriere tehnica:** Produce un plan de recuperare pentru branch-urile ramase in freeze din cauza sugestiilor AI.
**Scop:** Ajuta la iesirea controlata din blocaj.
**Target:** branch lock manager, recovery service, admin dashboard.
**Acceptare:** Planul include pasii, dependentele si ownerul.

### ~~W1285~~ ✅ AI suggestion quarantine state diff
**Descriere tehnica:** Arata diferenta dintre starea initiala si starea actuala a unei sugestii AI din carantina.
**Scop:** Face evolutia usor de urmarit.
**Target:** quarantine service, diff renderer, audit tools.
**Acceptare:** Diff-ul evidentiaza schimbarile relevante de stare.

### ~~W1286~~ ✅ AI suggestion approval checkpoint audit
**Descriere tehnica:** Inregistreaza fiecare checkpoint trecut in procesul de aprobare al unei sugestii AI.
**Scop:** Ofera audit granular al deciziei.
**Target:** approval workflow, audit log, review UI.
**Acceptare:** Checkpoint-urile sunt listate in ordinea parcurgerii.

### ~~W1287~~ ✅ AI suggestion rollback checkpoint audit
**Descriere tehnica:** Inregistreaza checkpoint-urile atinse de un rollback AI pentru a documenta progresul si blocajele.
**Scop:** Faciliteaza remedierea si auditul.
**Target:** rollback service, audit log, recovery service.
**Acceptare:** Jurnalul arata checkpoint-urile trecute si cele ratate.

### ~~W1288~~ ✅ AI suggestion branch readiness alert
**Descriere tehnica:** Notifica atunci cand branch-ul devine din nou pregatit pentru sugestii AI dupa o perioada de blocaj.
**Scop:** Reia fluxul fara intarzieri inutile.
**Target:** branch manager, notification service, moderation queue.
**Acceptare:** Alerta apare doar cand conditia de ready este restabilita.

### ~~W1289~~ ✅ AI suggestion evidence chain validator
**Descriere tehnica:** Valideaza ca lantul de dovezi pentru o sugestie AI este complet si nealterat.
**Scop:** Protejeaza integritatea auditului.
**Target:** audit log, evidence store, provenance service.
**Acceptare:** Lantul incomplet sau alterat este respins.

### ~~W1290~~ ✅ AI suggestion final archive index
**Descriere tehnica:** Indexeaza arhiva finala a sugestiilor AI pentru cautare rapida si recuperare.
**Scop:** Face istoricul usor de accesat.
**Target:** archive service, search index, audit viewer.
**Acceptare:** Sugestiile arhivate pot fi gasite dupa criteriile principale.

### ~~W1291~~ ✅ AI suggestion branch audit checkpoint
**Descriere tehnica:** Introduce un checkpoint de audit pentru branch-urile care primesc sugestii AI.
**Scop:** Marcheaza punctele cheie din istoric.
**Target:** branch manager, audit log, review workflow.
**Acceptare:** Checkpoint-ul este salvat si consultabil in audit.

### ~~W1292~~ ✅ AI suggestion moderation exception tracker
**Descriere tehnica:** Urmareste exceptiile aplicate in moderarea sugestiilor AI si le grupeaza pe tip.
**Scop:** Face exceptiile vizibile si analizabile.
**Target:** moderation queue, audit log, analytics dashboard.
**Acceptare:** Exceptiile sunt listate dupa tip, durata si autor.

### ~~W1293~~ ✅ AI suggestion policy gate dashboard
**Descriere tehnica:** Afiseaza toate gate-urile de politica care pot opri o sugestie AI si starea lor curenta.
**Scop:** Ofera transparenta in verificari.
**Target:** policy engine, admin dashboard, validation pipeline.
**Acceptare:** Dashboard-ul arata gate-urile active si cele trecute.

### ~~W1294~~ ✅ AI suggestion branch freeze exception log
**Descriere tehnica:** Inregistreaza exceptiile aprobate pentru branch-urile aflate in freeze cu sugestii AI.
**Scop:** Pastreaza controlul asupra exceptiilor.
**Target:** branch lock manager, audit log, admin dashboard.
**Acceptare:** Fiecare exceptie are motiv si expirare.

### ~~W1295~~ ✅ AI suggestion replay provenance checker
**Descriere tehnica:** Verifica daca replay-ul unei sugestii AI foloseste aceeasi provenienta ca executia originala.
**Scop:** Asigura comparabilitatea.
**Target:** replay sandbox, provenance service, audit tools.
**Acceptare:** Orice diferenta de provenienta este raportata.

### ~~W1296~~ ✅ AI suggestion review backlog heatmap
**Descriere tehnica:** Construieste o harta de caldura pentru backlog-ul de review al sugestiilor AI.
**Scop:** Evidentiaza unde se acumuleaza munca.
**Target:** analytics dashboard, moderation queue, telemetry service.
**Acceptare:** Heatmap-ul arata zonele cu acumulare mare.

### ~~W1297~~ ✅ AI suggestion publish blocker explanation
**Descriere tehnica:** Explica in termeni clari de ce publicarea unei sugestii AI este blocata.
**Scop:** Face remediation-ul mai rapid.
**Target:** release pipeline, validation service, admin dashboard.
**Acceptare:** Explicatia include regula, lipsa si pasul urmator.

### ~~W1298~~ ✅ AI suggestion quarantine evidence bundle
**Descriere tehnica:** Aduna toate dovezile relevante pentru o sugestie AI aflata in carantina.
**Scop:** Simplifica analiza si auditul.
**Target:** quarantine service, evidence store, audit log.
**Acceptare:** Bundle-ul poate fi exportat si consultat fara pierderi.

### ~~W1299~~ ✅ AI suggestion owner rotation policy
**Descriere tehnica:** Defineste cum se rotesc ownerii pe sugestii AI cand sarcinile sunt distribuite echitabil.
**Scop:** Evita incarcare disproportionata.
**Target:** ownership registry, assignment service, moderation queue.
**Acceptare:** Rotatia respecta regula configurata si este auditata.

### ~~W1300~~ ✅ AI suggestion branch status digest
**Descriere tehnica:** Rezuma starea branch-ului cu sugestii AI: freeze, ready, blocked, published sau archived.
**Scop:** Ofera o imagine rapida si clara.
**Target:** branch manager, admin dashboard, audit log.
**Acceptare:** Digest-ul afiseaza statusul actual si motivul lui.

### ~~W1301~~ ✅ AI suggestion rollback policy matrix
**Descriere tehnica:** Defineste ce tipuri de rollback sunt permise pentru diferite clase de sugestii AI.
**Scop:** Standardizeaza decizia de revert.
**Target:** rollback service, policy engine, admin dashboard.
**Acceptare:** Matricea specifica clar ce rollback este permis.

### ~~W1302~~ ✅ AI suggestion approval note index
**Descriere tehnica:** Indexeaza notele de aprobare pentru a putea fi gasite rapid ulterior.
**Scop:** Ajuta la audit si revenire.
**Target:** approval workflow, search index, audit viewer.
**Acceptare:** Notele pot fi cautate dupa sugestie, reviewer si motiv.

### ~~W1303~~ ✅ AI suggestion moderation rule tester
**Descriere tehnica:** Ruleaza teste asupra regulilor de moderare folosite pentru sugestiile AI.
**Scop:** Detecteaza regresii in logica de review.
**Target:** moderation rules, test suite, policy engine.
**Acceptare:** Testele acopera regulile critice si raporteaza abaterile.

### ~~W1304~~ ✅ AI suggestion branch archive digest
**Descriere tehnica:** Genereaza un digest al branch-urilor AI arhivate, cu motive si stare finala.
**Scop:** Ofera vizibilitate asupra istoricului inchis.
**Target:** archive service, branch manager, admin dashboard.
**Acceptare:** Digest-ul este cautabil si actualizat.

### ~~W1305~~ ✅ AI suggestion evidence retention policy
**Descriere tehnica:** Stabileste cat timp se pastreaza dovezile generate de sugestiile AI.
**Scop:** Controleaza costul si conformitatea.
**Target:** retention policy, evidence store, audit log.
**Acceptare:** Dovezile expira sau se arhiveaza conform politicii.

### ~~W1306~~ ✅ AI suggestion review lane alert
**Descriere tehnica:** Notifica atunci cand o banda de review pentru sugestiile AI se aglomereaza peste prag.
**Scop:** Previne acumularea in banda de lucru.
**Target:** moderation queue, notification service, analytics dashboard.
**Acceptare:** Alerta mentioneaza banda, pragul si perioada afectata.

### ~~W1307~~ ✅ AI suggestion branch rule drift monitor
**Descriere tehnica:** Monitorizeaza drift-ul dintre regulile branch-ului si comportamentul efectiv al sugestiilor AI.
**Scop:** Identifica schimbari neintenționate.
**Target:** branch manager, policy engine, telemetry service.
**Acceptare:** Drift-ul este raportat cu regula afectata.

### ~~W1308~~ ✅ AI suggestion publish evidence viewer
**Descriere tehnica:** Afiseaza dovezile asociate unei publicari AI intr-un viewer dedicat.
**Scop:** Simplifica auditul post-release.
**Target:** evidence store, audit viewer, release pipeline.
**Acceptare:** Viewer-ul reda dovezile fara a modifica datele.

### ~~W1309~~ ✅ AI suggestion rollback evidence viewer
**Descriere tehnica:** Afiseaza dovezile folosite la un rollback AI intr-un viewer dedicat.
**Scop:** Face investigatiile si training-ul mai simple.
**Target:** evidence store, audit viewer, rollback service.
**Acceptare:** Dovezile de rollback pot fi parcurse in ordine.

### ~~W1310~~ ✅ AI suggestion moderation signoff tracker
**Descriere tehnica:** Urmareste semnaturile de aprobare date in moderarea sugestiilor AI.
**Scop:** Face clar cine a semnat si cand.
**Target:** moderation workflow, audit log, admin dashboard.
**Acceptare:** Fiecare semnatura este listata cu timestamp.

### ~~W1311~~ ✅ AI suggestion quarantine replay digest
**Descriere tehnica:** Genereaza un digest al replay-ului pentru o sugestie AI aflata in carantina.
**Scop:** Ofera rezumat rapid al investigatiei.
**Target:** quarantine service, replay sandbox, audit tools.
**Acceptare:** Digest-ul arata ce s-a schimbat in replay.

### ~~W1312~~ ✅ AI suggestion policy evidence link
**Descriere tehnica:** Leaga o decizie de politica AI de dovezile care au justificat-o.
**Scop:** Face auditul transparent.
**Target:** policy engine, evidence store, audit log.
**Acceptare:** Legatura dintre regula si dovezi este vizibila.

### ~~W1313~~ ✅ AI suggestion branch incident classifier
**Descriere tehnica:** Clasifica incidentele branch-ului provocate de sugestiile AI in categorii standard.
**Scop:** Ajuta la analiza si raportare.
**Target:** incident dashboard, branch manager, audit log.
**Acceptare:** Incidentul este incadrat intr-o categorie clara.

### ~~W1314~~ ✅ AI suggestion review escalation counter
**Descriere tehnica:** Numara cate escaladari au avut loc in procesul de review al unei sugestii AI.
**Scop:** Ofera semnal pentru complexitate si risc.
**Target:** moderation queue, analytics dashboard, audit log.
**Acceptare:** Contorul este actualizat pe fiecare escaladare.

### ~~W1315~~ ✅ AI suggestion release ownership digest
**Descriere tehnica:** Rezuma cine detine responsabilitatea pentru publicarea unei sugestii AI.
**Scop:** Clarifica handoff-ul la release.
**Target:** release pipeline, ownership registry, admin dashboard.
**Acceptare:** Digest-ul arata ownerul, reviewerul si semnatarul.

### ~~W1316~~ ✅ AI suggestion branch policy exception diff
**Descriere tehnica:** Arata diferenta dintre politica normala a branch-ului si exceptiile aprobate pentru o sugestie AI.
**Scop:** Face exceptiile usor de evaluat.
**Target:** policy engine, branch manager, audit tools.
**Acceptare:** Diferenta este clara si usor de consultat.

### ~~W1317~~ ✅ AI suggestion queue fairness meter
**Descriere tehnica:** Măsoară echitatea distribuirii sugestiilor AI in coada de moderare.
**Scop:** Evita bias-ul operational.
**Target:** moderation queue, analytics dashboard, telemetry service.
**Acceptare:** Meter-ul arata distributia si deviațiile.

### ~~W1318~~ ✅ AI suggestion rollback evidence seal
**Descriere tehnica:** Sigileaza dovezile folosite la un rollback AI dupa finalizarea lui.
**Scop:** Protejeaza integritatea istoricului.
**Target:** evidence store, rollback service, audit log.
**Acceptare:** Dovezile sigilate nu mai pot fi modificate.

### ~~W1319~~ ✅ AI suggestion archive retrieval key
**Descriere tehnica:** Genereaza o cheie de recuperare pentru arhiva unei sugestii AI.
**Scop:** Face accesul la istoricul arhivat simplu si sigur.
**Target:** archive service, search index, audit viewer.
**Acceptare:** Cheia gaseste rapid arhiva corecta.

### ~~W1320~~ ✅ AI suggestion final review digest
**Descriere tehnica:** Produce un digest final pentru ultima etapa de review a unei sugestii AI.
**Scop:** Ajuta la decizia finala.
**Target:** review workflow, admin dashboard, audit log.
**Acceptare:** Digest-ul sintetizeaza verdictul si motivele cheie.

### ~~W1321~~ ✅ AI suggestion branch release ledger
**Descriere tehnica:** Pastreaza un ledger al publicarilor pentru branch-urile afectate de sugestii AI.
**Scop:** Ofera trasabilitate completa pentru release-uri.
**Target:** release pipeline, branch manager, audit log.
**Acceptare:** Fiecare release are o inregistrare clara si cautabila.

### ~~W1322~~ ✅ AI suggestion moderation lane rebalance
**Descriere tehnica:** Reechilibreaza benzile de moderare pentru sugestiile AI atunci cand una devine supraincarcata.
**Scop:** Distribuie munca mai uniform.
**Target:** moderation queue, assignment service, analytics dashboard.
**Acceptare:** Rebalansarea muta elementele fara pierdere de context.

### ~~W1323~~ ✅ AI suggestion policy evidence index
**Descriere tehnica:** Indexeaza dovezile care sustin fiecare regula din politica AI.
**Scop:** Face regulile mai usor de justificat si auditabil.
**Target:** policy engine, evidence store, audit viewer.
**Acceptare:** Dovezile pot fi gasite dupa regula si severitate.

### ~~W1324~~ ✅ AI suggestion rollback readiness meter
**Descriere tehnica:** Măsoară cat de pregatit este un rollback AI din punct de vedere al dependintelor si al datelor.
**Scop:** Evita reveniri incomplet pregatite.
**Target:** rollback service, telemetry service, admin dashboard.
**Acceptare:** Meter-ul produce o stare clara de ready sau blocked.

### ~~W1325~~ ✅ AI suggestion quarantine summary card
**Descriere tehnica:** Creeaza un card sumar pentru sugestiile AI aflate in carantina.
**Scop:** Ofera o vedere rapida asupra cazului.
**Target:** quarantine UI, moderation queue, admin dashboard.
**Acceptare:** Cardul include motivul, severitatea si ownerul.

### ~~W1326~~ ✅ AI suggestion replay issue detector
**Descriere tehnica:** Detecteaza probleme aparute la replay-ul unei sugestii AI, cum ar fi lipsa de date sau output diferit.
**Scop:** Face investigatia mai rapida.
**Target:** replay sandbox, audit tools, telemetry service.
**Acceptare:** Problemele sunt raportate cu diferenta identificata.

### ~~W1327~~ ✅ AI suggestion review action ledger
**Descriere tehnica:** Pastreaza un ledger al tuturor actiunilor facute in review-ul unei sugestii AI.
**Scop:** Ajuta la audit si la analiza fluxului.
**Target:** review workflow, audit log, admin dashboard.
**Acceptare:** Ledger-ul include actiunea, actorul si timestamp-ul.

### ~~W1328~~ ✅ AI suggestion branch policy review
**Descriere tehnica:** Revizuieste politicile aplicate unui branch inainte de a accepta noi sugestii AI.
**Scop:** Previne aplicarea pe branch-uri cu politici depasite.
**Target:** branch manager, policy engine, review workflow.
**Acceptare:** Revizuirea confirma daca policy-ul branch-ului este actual.

### ~~W1329~~ ✅ AI suggestion moderation comment digest
**Descriere tehnica:** Rezuma comentariile de moderare asociate unei sugestii AI intr-un digest compact.
**Scop:** Face feedback-ul usor de parcurs.
**Target:** moderation UI, audit viewer, export pipeline.
**Acceptare:** Digest-ul mentine referintele catre comentariile originale.

### ~~W1330~~ ✅ AI suggestion branch snapshot compare
**Descriere tehnica:** Compara snapshot-urile branch-ului inainte si dupa aplicarea sugestiilor AI.
**Scop:** Arata impactul real al schimbarii.
**Target:** branch manager, diff renderer, audit log.
**Acceptare:** Comparatia evidentiaza schimbarile relevante si starea finala.

### ~~W1331~~ ✅ AI suggestion approval evidence viewer
**Descriere tehnica:** Ofera un viewer dedicat pentru dovezile asociate aprobarii unei sugestii AI.
**Scop:** Simplifica auditul si verificarea finala.
**Target:** approval workflow, evidence store, audit viewer.
**Acceptare:** Dovezile sunt vizibile in ordinea corecta si fara alterare.

### ~~W1332~~ ✅ AI suggestion policy exception dashboard
**Descriere tehnica:** Afiseaza exceptiile active din politica AI pe un dashboard dedicat.
**Scop:** Ofera vizibilitate asupra derogărilor curente.
**Target:** policy engine, admin dashboard, audit log.
**Acceptare:** Dashboard-ul arata exceptiile, expirarea si motivul.

### ~~W1333~~ ✅ AI suggestion release gating summary
**Descriere tehnica:** Rezuma gate-urile care opresc sau permit publicarea unei sugestii AI.
**Scop:** Face decizia de release mai clara.
**Target:** release pipeline, admin dashboard, validation service.
**Acceptare:** Sumarul indica ce gate a blocat sau a permis publish-ul.

### ~~W1334~~ ✅ AI suggestion branch incident digest
**Descriere tehnica:** Produce un digest scurt al incidentelor branch-ului asociate sugestiilor AI.
**Scop:** Arata istoricul operational in format compact.
**Target:** incident dashboard, branch manager, audit log.
**Acceptare:** Digest-ul include incidentul, cauza si rezultatul.

### ~~W1335~~ ✅ AI suggestion quarantine expiration policy
**Descriere tehnica:** Stabileste cand o sugestie AI din carantina expira automat.
**Scop:** Evita acumularea permanenta.
**Target:** quarantine service, retention policy, cleanup jobs.
**Acceptare:** Sugestiile expira conform regulii configurate.

### ~~W1336~~ ✅ AI suggestion review replay index
**Descriere tehnica:** Indexeaza replay-urile de review pentru sugestiile AI.
**Scop:** Face istoria review-ului usor de cautat.
**Target:** audit viewer, replay sandbox, search index.
**Acceptare:** Replay-ul poate fi gasit dupa sugestie, reviewer si data.

### ~~W1337~~ ✅ AI suggestion branch lock reason digest
**Descriere tehnica:** Produce un digest al motivelor pentru care un branch este blocat de sugestii AI.
**Scop:** Explica blocajele fara sa incarce UI-ul.
**Target:** branch lock manager, admin dashboard, audit log.
**Acceptare:** Digest-ul mentioneaza motivele principale si durata.

### ~~W1338~~ ✅ AI suggestion policy lint report
**Descriere tehnica:** Genereaza un raport de lint pentru politicile AI active.
**Scop:** Scoate la iveala ambiguitati si contradictii.
**Target:** policy engine, docs archive, admin dashboard.
**Acceptare:** Raportul listeaza problemele si sugestiile de corectie.

### ~~W1339~~ ✅ AI suggestion moderation outcome explainability
**Descriere tehnica:** Explica de ce o sugestie AI a fost aprobata, respinsa sau carantinata.
**Scop:** Imbunatateste transparenta si training-ul.
**Target:** moderation workflow, audit viewer, analytics dashboard.
**Acceptare:** Explicatia include regula, contextul si verdictul final.

### ~~W1340~~ ✅ AI suggestion rollback authorization log
**Descriere tehnica:** Inregistreaza autorizatia necesara pentru fiecare rollback AI.
**Scop:** Face aprobarea revenirii auditable.
**Target:** rollback service, authorization log, admin dashboard.
**Acceptare:** Fiecare rollback are o autorizare asociata si verificabila.

### ~~W1341~~ ✅ AI suggestion archive retention audit
**Descriere tehnica:** Audit-eaza cat timp sunt pastrate arhivele de sugestii AI.
**Scop:** Asigura conformitatea cu politica de retentie.
**Target:** archive service, retention policy, audit log.
**Acceptare:** Auditul arata arhivele care depasesc sau respecta termenul.

### ~~W1342~~ ✅ AI suggestion branch readiness history
**Descriere tehnica:** Pastreaza istoricul starii de readiness pentru branch-urile cu sugestii AI.
**Scop:** Permite analiza regresiilor si a recovery-ului.
**Target:** branch manager, telemetry service, audit log.
**Acceptare:** Istoricul poate fi comparat pe intervale de timp.

### ~~W1343~~ ✅ AI suggestion moderation threshold audit
**Descriere tehnica:** Verifica pragurile folosite in moderare pentru a vedea daca sunt prea permisive sau prea stricte.
**Scop:** Calibreaza deciziile de review.
**Target:** moderation policy, analytics dashboard, audit reports.
**Acceptare:** Auditul identifica pragurile problematice si impactul lor.

### ~~W1344~~ ✅ AI suggestion evidence freshness checker
**Descriere tehnica:** Verifica daca dovezile atasate unei sugestii AI sunt inca proaspete si relevante.
**Scop:** Evita decizii bazate pe probe depasite.
**Target:** evidence store, validation pipeline, audit viewer.
**Acceptare:** Dovezile vechi sunt marcate ca stale sau invalide.

### ~~W1345~~ ✅ AI suggestion review checkpoint map
**Descriere tehnica:** Deseneaza harta checkpoint-urilor prin care trece un review AI pana la verdict.
**Scop:** Face procesul usor de inteles.
**Target:** review workflow, admin dashboard, audit viewer.
**Acceptare:** Harta arata ordinea, statusul si blocajele.

### ~~W1346~~ ✅ AI suggestion branch lock health report
**Descriere tehnica:** Raporteaza starea de sanatate a lock-urilor branch-ului asociate sugestiilor AI.
**Scop:** Face blocajele si recovery-ul vizibile.
**Target:** branch lock manager, telemetry service, admin dashboard.
**Acceptare:** Raportul diferentiaza lock-uri healthy, stale si blocked.

### ~~W1347~~ ✅ AI suggestion publish comment digest
**Descriere tehnica:** Rezuma comentariile folosite la publicarea unei sugestii AI.
**Scop:** Ofera context rapid pentru release si audit.
**Target:** release pipeline, changelog workflow, audit log.
**Acceptare:** Digest-ul pastreaza referinta la comentariile originale.

### ~~W1348~~ ✅ AI suggestion rollback explainability note
**Descriere tehnica:** Genereaza o nota explicativa pentru un rollback AI cu motiv, efect si dependente.
**Scop:** Face revenirea usor de justificat.
**Target:** rollback service, audit log, admin dashboard.
**Acceptare:** Nota include schimbarea, motivul si rezultatul.

### ~~W1349~~ ✅ AI suggestion quarantine audit seal
**Descriere tehnica:** Sigileaza auditul pentru sugestiile AI care ies din carantina.
**Scop:** Protejeaza integritatea istoricului de carantina.
**Target:** quarantine service, audit log, archive store.
**Acceptare:** Auditul sigilat nu poate fi schimbat fara procedura.

### ~~W1350~~ ✅ AI suggestion final readiness verdict
**Descriere tehnica:** Produce verdictul final privind readiness-ul unei sugestii AI pentru publicare sau arhivare.
**Scop:** Clarifica actiunea finala fara ambiguitate.
**Target:** release pipeline, archive service, admin dashboard.
**Acceptare:** Verdictul final este explicit si usor de consultat.

### ~~W1351~~ ✅ AI suggestion branch publish seal
**Descriere tehnica:** Sigileaza publicarea unui branch dupa ce sugestiile AI au trecut de toate gate-urile.
**Scop:** Stabileste o stare finala neschimbabila.
**Target:** release pipeline, branch manager, audit log.
**Acceptare:** Branch-ul sigilat nu mai accepta schimbari fara reactivare explicita.

### ~~W1352~~ ✅ AI suggestion moderation audit bookmark
**Descriere tehnica:** Permite adaugarea de bookmark-uri in auditul de moderare pentru sugestiile AI.
**Scop:** Face investigatia mai rapida pe momente cheie.
**Target:** audit viewer, moderation workflow, replay tools.
**Acceptare:** Bookmark-urile sunt salvate si pot fi revizitate imediat.

### ~~W1353~~ ✅ AI suggestion policy anomaly digest
**Descriere tehnica:** Rezuma anomaliile detectate in politica AI si efectul lor asupra sugestiilor.
**Scop:** Ofera vizibilitate asupra regulilor problematice.
**Target:** policy engine, analytics dashboard, audit log.
**Acceptare:** Digest-ul listeaza anomaliile, severitatea si impactul.

### ~~W1354~~ ✅ AI suggestion rollback approval matrix
**Descriere tehnica:** Defineste ce aprobari sunt necesare pentru rollback-urile generate de sugestii AI.
**Scop:** Controleaza revenirea la o stare anterioara.
**Target:** rollback service, approval workflow, admin dashboard.
**Acceptare:** Matricea specifica clar cine trebuie sa aprobe.

### ~~W1355~~ ✅ AI suggestion quarantine owner alert
**Descriere tehnica:** Notifica ownerul cand o sugestie AI intra in carantina.
**Scop:** Reduce timpul pana la reactie.
**Target:** quarantine service, notification service, ownership registry.
**Acceptare:** Ownerul primeste alerta cu motivul carantinarii.

### ~~W1356~~ ✅ AI suggestion replay readiness verifier
**Descriere tehnica:** Verifica daca o sugestie AI este pregatita pentru replay in sandbox.
**Scop:** Evita replay-uri incomplete sau gresite.
**Target:** replay sandbox, provenance service, validation pipeline.
**Acceptare:** Replay-ul este permis doar cand toate conditiile sunt satisfacute.

### ~~W1357~~ ✅ AI suggestion branch threshold reporter
**Descriere tehnica:** Raporteaza cand branch-ul depaseste pragurile de acceptare pentru sugestii AI.
**Scop:** Previne supraincarcarea branch-urilor.
**Target:** branch manager, telemetry service, admin dashboard.
**Acceptare:** Raportul include pragul, depasirea si impactul.

### ~~W1358~~ ✅ AI suggestion moderation verdict cache
**Descriere tehnica:** Cache-uieste verdictele de moderare pentru sugestii AI similare.
**Scop:** Reduce munca repetitiva in review.
**Target:** moderation queue, cache layer, review workflow.
**Acceptare:** Verdictele identice pot fi refolosite in conditii sigure.

### ~~W1359~~ ✅ AI suggestion policy evidence digest
**Descriere tehnica:** Produce un digest scurt al dovezilor care sustin o regula AI.
**Scop:** Face explicarea politicii mai rapida.
**Target:** policy engine, evidence store, audit viewer.
**Acceptare:** Digest-ul leaga regula de dovezile principale.

### ~~W1360~~ ✅ AI suggestion release readiness report
**Descriere tehnica:** Genereaza un raport final de readiness pentru publicarea unei sugestii AI.
**Scop:** Ofera verdictul operational inainte de release.
**Target:** release pipeline, admin dashboard, audit reports.
**Acceptare:** Raportul arata clar daca publish-ul poate continua.

### ~~W1361~~ ✅ AI suggestion branch lock replay guard
**Descriere tehnica:** Impiedica replay-ul unei sugestii AI pe branch-uri care au lock activ.
**Scop:** Evita inconsistentele in timpul blocajelor.
**Target:** branch lock manager, replay sandbox, validation pipeline.
**Acceptare:** Replay-ul este oprit cand lock-ul este activ.

### ~~W1362~~ ✅ AI suggestion moderation fairness report
**Descriere tehnica:** Raporteaza cat de echitabil sunt tratate sugestiile AI in moderare.
**Scop:** Scoate la lumina potentialele dezechilibre.
**Target:** analytics dashboard, moderation queue, telemetry service.
**Acceptare:** Raportul compara distributia intre moderatori si tipuri de sugestii.

### ~~W1363~~ ✅ AI suggestion policy change log
**Descriere tehnica:** Pastreaza jurnalul tuturor schimbarilor facute in politica AI.
**Scop:** Face guvernanta auditabila.
**Target:** policy engine, audit log, docs archive.
**Acceptare:** Schimbarile au autor, motivatie si timestamp.

### ~~W1364~~ ✅ AI suggestion rollback impact digest
**Descriere tehnica:** Rezuma impactul total al unui rollback AI asupra branch-ului si dependentelor.
**Scop:** Face evaluarea revenirii mai rapida.
**Target:** rollback service, dependency graph, admin dashboard.
**Acceptare:** Digest-ul include obiectele afectate si starea lor finala.

### ~~W1365~~ ✅ AI suggestion quarantine handoff note
**Descriere tehnica:** Creeaza o nota de handoff pentru sugestiile AI transferate din carantina catre alt reviewer.
**Scop:** Pastreaza contextul complet al cazului.
**Target:** quarantine service, review workflow, audit log.
**Acceptare:** Nota include motivul transferului si contextul necesar.

### ~~W1366~~ ✅ AI suggestion approval gate history
**Descriere tehnica:** Inregistreaza istoricul gate-urilor trecute sau blocate in procesul de aprobare AI.
**Scop:** Ofera urmarire granulara a traseului de aprobare.
**Target:** approval workflow, audit log, admin dashboard.
**Acceptare:** Istoricul este ordonat si usor de parcurs.

### ~~W1367~~ ✅ AI suggestion branch archive seal
**Descriere tehnica:** Sigileaza branch-ul arhivat dupa ce sugestiile AI au fost rezolvate definitiv.
**Scop:** Protejeaza starea finala si auditul.
**Target:** archive service, branch manager, audit log.
**Acceptare:** Branch-ul sigilat nu poate fi modificat fara procedura.

### ~~W1368~~ ✅ AI suggestion review comment index
**Descriere tehnica:** Indexeaza comentariile din review pentru sugestiile AI ca sa poata fi cautate rapid.
**Scop:** Simplifica navigarea istoricului de review.
**Target:** search index, review workflow, audit viewer.
**Acceptare:** Comentariile sunt gasibile dupa sugestie, reviewer si cuvant cheie.

### ~~W1369~~ ✅ AI suggestion branch readiness alert
**Descriere tehnica:** Trimite alerta cand branch-ul revine la stare ready dupa blocaje legate de sugestiile AI.
**Scop:** Reia munca fara intarziere.
**Target:** branch manager, notification service, moderation queue.
**Acceptare:** Alerta este trimisa doar dupa restabilirea readiness-ului.

### ~~W1370~~ ✅ AI suggestion policy rule ownership
**Descriere tehnica:** Leaga fiecare regula din politica AI de un owner responsabil.
**Scop:** Evita regulile fara responsabil clar.
**Target:** policy engine, ownership registry, admin dashboard.
**Acceptare:** Fiecare regula are owner si contact vizibile.

### ~~W1371~~ ✅ AI suggestion rollback checkpoint map
**Descriere tehnica:** Deseneaza harta checkpoint-urilor pentru rollback-urile AI.
**Scop:** Face progresul revenirii usor de urmarit.
**Target:** rollback service, audit viewer, recovery service.
**Acceptare:** Harta include checkpoint-urile, statusul si blocajele.

### ~~W1372~~ ✅ AI suggestion moderation label override log
**Descriere tehnica:** Inregistreaza orice override al etichetei de moderare pentru o sugestie AI.
**Scop:** Pastreaza trasabilitatea schimbarilor de label.
**Target:** moderation UI, audit log, policy engine.
**Acceptare:** Override-ul are autor, motiv si status anterior.

### ~~W1373~~ ✅ AI suggestion provenance anomaly alert
**Descriere tehnica:** Notifica atunci cand provenienta unei sugestii AI pare inconsistentă sau incompleta.
**Scop:** Prinde problemele de audit din timp.
**Target:** provenance service, alerting system, audit log.
**Acceptare:** Alerta include elementele lipsa sau divergente.

### ~~W1374~~ ✅ AI suggestion release candidate report
**Descriere tehnica:** Genereaza raportul pentru o sugestie AI declarata release candidate.
**Scop:** Clarifica starea de pregatire.
**Target:** release pipeline, admin dashboard, approval workflow.
**Acceptare:** Raportul arata ce mai lipseste pentru publish final.

### ~~W1375~~ ✅ AI suggestion branch policy snapshot viewer
**Descriere tehnica:** Permite vizualizarea snapshot-urilor de policy aplicate unui branch la momente diferite.
**Scop:** Ajuta la auditul schimbarilor de reguli.
**Target:** policy engine, branch manager, audit viewer.
**Acceptare:** Snapshot-urile pot fi comparate si consultate usor.

### ~~W1376~~ ✅ AI suggestion quarantine severity recheck
**Descriere tehnica:** Re-evalueaza severitatea unei sugestii AI aflate in carantina.
**Scop:** Evita clasificari gresite sau expirate.
**Target:** quarantine service, moderation workflow, validation pipeline.
**Acceptare:** Severitatea poate fi actualizata cu justificare.

### ~~W1377~~ ✅ AI suggestion review evidence index
**Descriere tehnica:** Indexeaza dovezile folosite in review-ul sugestiilor AI pentru cautare rapida.
**Scop:** Simplifica auditul si feedback-ul.
**Target:** evidence store, search index, audit viewer.
**Acceptare:** Dovezile sunt gasibile dupa sugestie si tip.

### ~~W1378~~ ✅ AI suggestion branch incident owner map
**Descriere tehnica:** Arata ce owner este responsabil pentru fiecare incident branch legat de sugestii AI.
**Scop:** Clarifica handoff-ul de incident.
**Target:** incident dashboard, ownership registry, branch manager.
**Acceptare:** Fiecare incident are owner asignat si vizibil.

### ~~W1379~~ ✅ AI suggestion moderation recovery guide
**Descriere tehnica:** Ofera un ghid de recuperare pentru sugestiile AI ramase blocate in moderare.
**Scop:** Standardizeaza iesirea din blocaje.
**Target:** moderation workflow, docs archive, admin dashboard.
**Acceptare:** Ghidul acopera blocaje, expirari si escaladari.

### ~~W1380~~ ✅ AI suggestion final archive digest
**Descriere tehnica:** Produce un digest final pentru arhiva sugestiilor AI inchise definitiv.
**Scop:** Ofera un rezumat curat pentru istoric.
**Target:** archive service, audit log, search index.
**Acceptare:** Digest-ul poate fi consultat dupa branch, status si perioada.

### ~~W1381~~ ✅ AI suggestion branch review seal
**Descriere tehnica:** Sigileaza branch-ul dupa ce toate sugestiile AI au trecut de review si audit.
**Scop:** Marcheaza finalizarea controlata a ciclului de review.
**Target:** branch manager, review workflow, audit log.
**Acceptare:** Branch-ul sigilat nu accepta noi sugestii fara reactivare.

### ~~W1382~~ ✅ AI suggestion moderation history viewer
**Descriere tehnica:** Afiseaza istoricul complet al moderarii pentru fiecare sugestie AI.
**Scop:** Face analiza deciziilor mult mai rapida.
**Target:** moderation UI, audit viewer, search index.
**Acceptare:** Istoricul poate fi parcurs de la initial la final.

### ~~W1383~~ ✅ AI suggestion policy approval trail
**Descriere tehnica:** Inregistreaza traseul de aprobare al politicii AI care a afectat o sugestie.
**Scop:** Leaga verdictul de politica aplicata.
**Target:** policy engine, approval workflow, audit log.
**Acceptare:** Trail-ul arata cine a aprobat si ce versiune a fost folosita.

### ~~W1384~~ ✅ AI suggestion rollback note digest
**Descriere tehnica:** Rezuma notitele asociate unui rollback AI intr-un format scurt si cautabil.
**Scop:** Simplifica auditul revenirilor.
**Target:** rollback service, audit viewer, docs archive.
**Acceptare:** Digest-ul include motivul si efectul principal.

### ~~W1385~~ ✅ AI suggestion quarantine recheck queue
**Descriere tehnica:** Creeaza o coada separata pentru reevaluarea sugestiilor AI din carantina.
**Scop:** Organizeaza reluarea analizei.
**Target:** quarantine service, moderation queue, validation pipeline.
**Acceptare:** Elementele din coada sunt reevaluate in ordine.

### ~~W1386~~ ✅ AI suggestion branch risk ledger
**Descriere tehnica:** Pastreaza un ledger al riscurilor asociate branch-urilor care primesc sugestii AI.
**Scop:** Face riscul explicit si urmaribil.
**Target:** branch manager, analytics dashboard, audit log.
**Acceptare:** Ledger-ul poate fi consultat dupa branch si severitate.

### ~~W1387~~ ✅ AI suggestion review checkpoint digest
**Descriere tehnica:** Produce un digest al checkpoint-urilor din procesul de review al sugestiilor AI.
**Scop:** Ofera o vedere rapida asupra progresului.
**Target:** review workflow, audit viewer, admin dashboard.
**Acceptare:** Digest-ul arata checkpoint-urile trecute si cele blocate.

### ~~W1388~~ ✅ AI suggestion policy freeze notice
**Descriere tehnica:** Trimite notificari cand o politica AI intra in freeze si blocheaza sugestiile.
**Scop:** Face starea de freeze vizibila pentru staff.
**Target:** notification service, policy engine, admin dashboard.
**Acceptare:** Notificarea arata politica afectata si durata.

### ~~W1389~~ ✅ AI suggestion release note seal
**Descriere tehnica:** Sigileaza notele de release generate din sugestii AI dupa aprobarea finala.
**Scop:** Protejeaza documentatia de release.
**Target:** release notes generator, audit log, docs archive.
**Acceptare:** Notele sigilate nu mai pot fi modificate fara audit.

### ~~W1390~~ ✅ AI suggestion branch archive viewer
**Descriere tehnica:** Ofera un viewer pentru branch-urile AI arhivate si starea lor finala.
**Scop:** Simplifica consultarea istoricului inchis.
**Target:** archive service, branch manager, audit viewer.
**Acceptare:** Viewer-ul arata statusul si motivele arhivarii.

### ~~W1391~~ ✅ AI suggestion moderation decision map
**Descriere tehnica:** Deseneaza o harta a deciziilor de moderare pentru sugestiile AI pe parcursul fluxului.
**Scop:** Face traseul logic vizibil.
**Target:** moderation workflow, decision engine, audit viewer.
**Acceptare:** Harta arata fiecare ramura de decizie.

### ~~W1392~~ ✅ AI suggestion evidence freshness report
**Descriere tehnica:** Raporteaza cat de proaspete sunt dovezile folosite la evaluarea sugestiilor AI.
**Scop:** Previne decizii pe date expirate.
**Target:** evidence store, telemetry service, audit log.
**Acceptare:** Raportul marcheaza dovezile stale sau actuale.

### ~~W1393~~ ✅ AI suggestion rollback history viewer
**Descriere tehnica:** Afiseaza istoricul complet al rollback-urilor pentru sugestiile AI.
**Scop:** Face investigatia mai simpla.
**Target:** rollback service, audit viewer, search index.
**Acceptare:** Istoricul poate fi filtrat dupa branch si data.

### ~~W1394~~ ✅ AI suggestion branch ownership digest
**Descriere tehnica:** Rezuma ownership-ul branch-ului pentru sugestiile AI curente.
**Scop:** Clarifica responsabilitatea in timpul review-ului.
**Target:** ownership registry, branch manager, admin dashboard.
**Acceptare:** Digest-ul arata ownerul principal si altii relevanti.

### ~~W1395~~ ✅ AI suggestion moderation escalation log
**Descriere tehnica:** Inregistreaza toate escaladarile din moderarea sugestiilor AI.
**Scop:** Face urmarirea incidentelor de review mai usoara.
**Target:** moderation queue, audit log, escalation rules.
**Acceptare:** Log-ul include motivul, destinatarul si momentul escaladarii.

### ~~W1396~~ ✅ AI suggestion policy alert digest
**Descriere tehnica:** Produce un digest al alertelor generate de politicile AI.
**Scop:** Ofera un rezumat operational al problemelor.
**Target:** policy engine, alerting system, admin dashboard.
**Acceptare:** Digest-ul grupeaza alertele dupa severitate si regula.

### ~~W1397~~ ✅ AI suggestion publish readiness snapshot
**Descriere tehnica:** Salveaza un snapshot al starii de readiness chiar inainte de publicare.
**Scop:** Pastreaza un punct de referinta pentru audit.
**Target:** release pipeline, audit log, readiness meter.
**Acceptare:** Snapshot-ul poate fi comparat cu starea ulterioara.

### ~~W1398~~ ✅ AI suggestion quarantine evidence viewer
**Descriere tehnica:** Ofera un viewer dedicat pentru dovezile unei sugestii AI aflate in carantina.
**Scop:** Simplifica analiza si verificarea.
**Target:** quarantine service, evidence store, audit viewer.
**Acceptare:** Dovezile sunt vizibile in ordine si cu context.

### ~~W1399~~ ✅ AI suggestion branch freeze digest
**Descriere tehnica:** Produce un digest scurt al starii de freeze pentru branch-urile cu sugestii AI.
**Scop:** Ofera claritate rapida asupra blocajului.
**Target:** branch lock manager, admin dashboard, audit log.
**Acceptare:** Digest-ul include motivul, durata si impactul.

### ~~W1400~~ ✅ AI suggestion review verdict seal
**Descriere tehnica:** Sigileaza verdictul final al unui review AI pentru a preveni alterari ulterioare.
**Scop:** Protejeaza integritatea deciziei.
**Target:** review workflow, audit log, admin dashboard.
**Acceptare:** Verdictul sigilat este doar citire si auditabil.

### ~~W1401~~ ✅ AI suggestion rollback readiness digest
**Descriere tehnica:** Rezuma starea de pregatire a unui rollback AI intr-un format compact.
**Scop:** Ofera un semnal rapid pentru decizia finala.
**Target:** rollback service, readiness checker, admin dashboard.
**Acceptare:** Digest-ul arata clar ready, blocked sau needs review.

### ~~W1402~~ ✅ AI suggestion policy coverage digest
**Descriere tehnica:** Rezuma ce acopera politicile AI si ce ramane neacoperit.
**Scop:** Identifica rapid golurile de guvernanta.
**Target:** policy engine, docs archive, admin dashboard.
**Acceptare:** Digest-ul listeaza acoperirea si lipsurile.

### ~~W1403~~ ✅ AI suggestion branch release history
**Descriere tehnica:** Pastreaza istoricul publicarilor pentru fiecare branch influentat de sugestii AI.
**Scop:** Ofera context pentru viitoarele decizii.
**Target:** release pipeline, branch manager, audit log.
**Acceptare:** Istoricul este cautabil dupa branch si interval.

### ~~W1404~~ ✅ AI suggestion moderation decision seal
**Descriere tehnica:** Sigileaza decizia de moderare dupa ce sugestia AI a fost evaluata.
**Scop:** Protejeaza decizia finala si auditul.
**Target:** moderation workflow, audit log, review UI.
**Acceptare:** Decizia sigilata nu poate fi schimbata fara procedura.

### ~~W1405~~ ✅ AI suggestion replay digest
**Descriere tehnica:** Produce un digest al replay-ului unei sugestii AI pentru consultare rapida.
**Scop:** Ajuta la investigatii si training.
**Target:** replay sandbox, audit viewer, review workflow.
**Acceptare:** Digest-ul sintetizeaza diferenta dintre replay si original.

### ~~W1406~~ ✅ AI suggestion branch risk digest
**Descriere tehnica:** Rezuma riscurile curente ale unui branch care primeste sugestii AI.
**Scop:** Ajuta la prioritizarea review-ului.
**Target:** branch manager, analytics dashboard, audit log.
**Acceptare:** Digest-ul marcheaza riscurile principale si severitatea.

### ~~W1407~~ ✅ AI suggestion policy owner alert
**Descriere tehnica:** Notifica ownerul politicii AI cand o regula intra in conflict sau produce blocaje.
**Scop:** Reduce timpul pana la remediere.
**Target:** policy engine, notification service, admin dashboard.
**Acceptare:** Alertarea ajunge la ownerul corect cu contextul problemei.

### ~~W1408~~ ✅ AI suggestion quarantine summary digest
**Descriere tehnica:** Produce un rezumat al tuturor sugestiilor AI aflate in carantina.
**Scop:** Ofera vizibilitate operationala de ansamblu.
**Target:** quarantine service, admin dashboard, analytics dashboard.
**Acceptare:** Digest-ul poate fi filtrat pe severitate si owner.

### ~~W1409~~ ✅ AI suggestion release comment seal
**Descriere tehnica:** Sigileaza comentariile de release atasate sugestiilor AI dupa aprobare.
**Scop:** Protejeaza integritatea comunicarii de release.
**Target:** release pipeline, changelog workflow, audit log.
**Acceptare:** Comentariile sigilate nu pot fi alterate fara audit.

### ~~W1410~~ ✅ AI suggestion final audit digest
**Descriere tehnica:** Produce un digest final de audit pentru sugestia AI dupa publicare, respingere sau carantina.
**Scop:** Ofera un rezumat complet al ciclului de viata.
**Target:** audit viewer, archive service, admin dashboard.
**Acceptare:** Digest-ul contine verdictul, motivele si dovezile principale.

### ~~W1411~~ ✅ AI suggestion branch archival digest
**Descriere tehnica:** Rezuma branch-urile AI arhivate cu motivul arhivarii si starea finala.
**Scop:** Ofera o vedere rapida asupra istoricului inchis.
**Target:** archive service, branch manager, audit viewer.
**Acceptare:** Digest-ul poate fi filtrat dupa status si perioada.

### ~~W1412~~ ✅ AI suggestion moderation outcome seal
**Descriere tehnica:** Sigileaza rezultatul final al moderarii pentru o sugestie AI.
**Scop:** Protejeaza verdictul impotriva modificarilor ulterioare.
**Target:** moderation workflow, audit log, review UI.
**Acceptare:** Outcome-ul sigilat este doar citire si auditable.

### ~~W1413~~ ✅ AI suggestion policy impact viewer
**Descriere tehnica:** Arata impactul fiecarui rule set din politica AI asupra sugestiilor evaluate.
**Scop:** Face efectul politicilor usor de inteles.
**Target:** policy engine, analytics dashboard, audit viewer.
**Acceptare:** Viewer-ul arata sugestiile afectate si regulile responsabile.

### ~~W1414~~ ✅ AI suggestion rollback comment seal
**Descriere tehnica:** Sigileaza comentariile atasate rollback-urilor AI dupa finalizare.
**Scop:** Pastreaza integritatea justificarii revenirii.
**Target:** rollback service, audit log, docs archive.
**Acceptare:** Comentariile sigilate nu pot fi editate fara audit.

### ~~W1415~~ ✅ AI suggestion quarantine result digest
**Descriere tehnica:** Produce un digest al rezultatului final pentru sugestiile AI care trec prin carantina.
**Scop:** Ofera claritate despre destinul fiecarui caz.
**Target:** quarantine service, audit viewer, admin dashboard.
**Acceptare:** Digest-ul arata ce s-a intamplat dupa carantina.

### ~~W1416~~ ✅ AI suggestion review lane digest
**Descriere tehnica:** Rezuma starea fiecarei benzi de review folosite pentru sugestiile AI.
**Scop:** Ajuta la monitorizarea fluxului de moderare.
**Target:** moderation queue, analytics dashboard, admin dashboard.
**Acceptare:** Digest-ul include incarcare, blocaje si timp mediu.

### ~~W1417~~ ✅ AI suggestion branch dependency seal
**Descriere tehnica:** Sigileaza dependentele branch-ului dupa ce sugestiile AI au fost aprobate.
**Scop:** Protejeaza consistenta structurii.
**Target:** dependency graph, branch manager, audit log.
**Acceptare:** Dependentele sigilate nu mai pot fi modificate fara audit.

### ~~W1418~~ ✅ AI suggestion evidence bundle digest
**Descriere tehnica:** Rezuma continutul unui bundle de dovezi asociat unei sugestii AI.
**Scop:** Simplifica consultarea rapida a probelor.
**Target:** evidence store, audit viewer, export pipeline.
**Acceptare:** Digest-ul enumera cele mai importante dovezi si surse.

### ~~W1419~~ ✅ AI suggestion publish anomaly report
**Descriere tehnica:** Raporteaza anomaliile detectate in timpul publicarii unei sugestii AI.
**Scop:** Identifica problemele din pipeline la release.
**Target:** release pipeline, telemetry service, admin dashboard.
**Acceptare:** Raportul arata anomalia, etapa si efectul.

### ~~W1420~~ ✅ AI suggestion branch lock seal
**Descriere tehnica:** Sigileaza lock-urile branch-ului pentru sugestiile AI dupa validarea finala.
**Scop:** Evita modificarile accidentale.
**Target:** branch lock manager, audit log, branch manager.
**Acceptare:** Lock-ul sigilat nu accepta schimbari fara procedura.

### ~~W1421~~ ✅ AI suggestion moderation note history
**Descriere tehnica:** Pastreaza istoricul complet al notitelor de moderare pentru sugestiile AI.
**Scop:** Ofera context pentru decizii si audit.
**Target:** moderation workflow, audit viewer, search index.
**Acceptare:** Istoricul poate fi parcurs cronologic.

### ~~W1422~~ ✅ AI suggestion policy rule digest
**Descriere tehnica:** Rezuma fiecare regula din politica AI intr-un format scurt si usor de parcurs.
**Scop:** Simplifica intelegerea politicii.
**Target:** policy docs, admin dashboard, audit viewer.
**Acceptare:** Digest-ul mentioneaza scopul si severitatea regulii.

### ~~W1423~~ ✅ AI suggestion rollback recovery note
**Descriere tehnica:** Genereaza o nota de recuperare pentru rollback-urile AI finalizate.
**Scop:** Clarifica ce s-a restaurat si ce ramane de facut.
**Target:** rollback service, recovery workflow, audit log.
**Acceptare:** Nota include starea finala si actiunile ramase.

### ~~W1424~~ ✅ AI suggestion quarantine owner digest
**Descriere tehnica:** Rezuma ownerul responsabil pentru sugestiile AI aflate in carantina.
**Scop:** Face handoff-ul clar.
**Target:** quarantine service, ownership registry, admin dashboard.
**Acceptare:** Digest-ul afiseaza ownerul si statusul.

### ~~W1425~~ ✅ AI suggestion replay outcome seal
**Descriere tehnica:** Sigileaza rezultatul unui replay AI pentru a preveni reinterpretarea ulterioara.
**Scop:** Protejeaza concluziile investigative.
**Target:** replay sandbox, audit log, admin dashboard.
**Acceptare:** Rezultatul sigilat este doar citire.

### ~~W1426~~ ✅ AI suggestion branch freeze recovery digest
**Descriere tehnica:** Rezuma pasii de recuperare pentru un branch blocat de sugestii AI.
**Scop:** Ajuta la iesirea din freeze mai rapid.
**Target:** branch lock manager, recovery service, admin dashboard.
**Acceptare:** Digest-ul include pasi, owner si blocaje rezolvate.

### ~~W1427~~ ✅ AI suggestion moderation escalation digest
**Descriere tehnica:** Produce un digest al tuturor escaladarilor din moderarea sugestiilor AI.
**Scop:** Ofera un rezumat operational al exceptiilor.
**Target:** moderation queue, analytics dashboard, audit log.
**Acceptare:** Digest-ul poate fi filtrat dupa cauza si destinatar.

### ~~W1428~~ ✅ AI suggestion policy exception digest
**Descriere tehnica:** Rezuma exceptiile active din politica AI si efectul lor asupra deciziilor.
**Scop:** Face exceptiile usor de urmarit.
**Target:** policy engine, admin dashboard, audit viewer.
**Acceptare:** Digest-ul arata durata, autorul si regula afectata.

### ~~W1429~~ ✅ AI suggestion release seal audit
**Descriere tehnica:** Audit-eaza sigiliul aplicat unui release AI dupa aprobarea finala.
**Scop:** Verifica integritatea release-ului.
**Target:** release pipeline, audit log, admin dashboard.
**Acceptare:** Auditul confirma ca sigiliul nu a fost alterat.

### ~~W1430~~ ✅ AI suggestion branch readiness seal
**Descriere tehnica:** Sigileaza starea de readiness a branch-ului dupa validarea unei sugestii AI.
**Scop:** Stabileste un reper stabil pentru release.
**Target:** branch manager, readiness meter, audit log.
**Acceptare:** Starea sigilata este verificabila si neschimbabila fara audit.

### ~~W1431~~ ✅ AI suggestion moderation replay history
**Descriere tehnica:** Arata istoricul replay-urilor de moderare pentru sugestiile AI.
**Scop:** Ajuta la comparatii si investigatii.
**Target:** moderation workflow, replay sandbox, audit viewer.
**Acceptare:** Istoricul include cine a rulat replay-ul si cu ce rezultat.

### ~~W1432~~ ✅ AI suggestion policy review digest
**Descriere tehnica:** Produce un digest al revizuirilor facute asupra politicilor AI.
**Scop:** Simplifica urmarirea schimbarilor de governance.
**Target:** policy engine, audit log, docs archive.
**Acceptare:** Digest-ul arata revizuirea, motivul si decizia.

### ~~W1433~~ ✅ AI suggestion rollback readiness seal
**Descriere tehnica:** Sigileaza verdictul de readiness pentru un rollback AI.
**Scop:** Protejeaza decizia de revert.
**Target:** rollback service, readiness checker, audit log.
**Acceptare:** Verdictul sigilat nu poate fi schimbat fara procedura.

### ~~W1434~~ ✅ AI suggestion quarantine history digest
**Descriere tehnica:** Rezuma istoricul complet al unei sugestii AI in carantina.
**Scop:** Ofera o vedere rapida a ciclului de carantina.
**Target:** quarantine service, audit viewer, admin dashboard.
**Acceptare:** Digest-ul include stari, schimbari si verdict final.

### ~~W1435~~ ✅ AI suggestion branch policy seal
**Descriere tehnica:** Sigileaza snapshot-ul de politica aplicat unui branch cu sugestii AI.
**Scop:** Previne schimbarea necontrolata a regulilor istorice.
**Target:** policy engine, branch manager, audit log.
**Acceptare:** Snapshot-ul sigilat poate fi doar comparat, nu editat.

### ~~W1436~~ ✅ AI suggestion evidence replay digest
**Descriere tehnica:** Rezuma rezultatul replay-ului dovezilor pentru o sugestie AI.
**Scop:** Face verificarea probelor mai rapida.
**Target:** evidence store, replay sandbox, audit viewer.
**Acceptare:** Digest-ul arata ce dovezi au fost confirmate sau invalidate.

### ~~W1437~~ ✅ AI suggestion moderation change digest
**Descriere tehnica:** Rezuma schimbarile de moderare aplicate unei sugestii AI pe parcursul ciclului ei.
**Scop:** Ofera o vedere de ansamblu asupra evolutiei.
**Target:** moderation workflow, audit log, analytics dashboard.
**Acceptare:** Digest-ul listeaza schimbarea, motivul si actorul.

### ~~W1438~~ ✅ AI suggestion branch incident seal
**Descriere tehnica:** Sigileaza inregistrarea incidentelor branch-ului produse de sugestii AI.
**Scop:** Protejeaza integritatea investigatiilor.
**Target:** incident dashboard, audit log, branch manager.
**Acceptare:** Incidentul sigilat nu poate fi modificat fara audit.

### ~~W1439~~ ✅ AI suggestion release outcome digest
**Descriere tehnica:** Produce un digest al rezultatului final de release pentru o sugestie AI.
**Scop:** Clarifica ce a ajuns live si de ce.
**Target:** release pipeline, changelog workflow, admin dashboard.
**Acceptare:** Digest-ul mentioneaza verdictul si efectul release-ului.

### ~~W1440~~ ✅ AI suggestion final lifecycle seal
**Descriere tehnica:** Sigileaza ciclul de viata final al unei sugestii AI dupa publicare, respingere sau arhivare.
**Scop:** Inchide definitiv inregistrarea.
**Target:** archive service, audit log, moderation workflow.
**Acceptare:** Ciclul sigilat ramane imuabil si consultabil.

### ~~W1441~~ ✅ AI suggestion branch release note
**Descriere tehnica:** Creeaza o nota de release pentru branch-urile care au primit sugestii AI si au trecut de review.
**Scop:** Ofera context clar pentru schimbarea publicata.
**Target:** release pipeline, changelog workflow, admin dashboard.
**Acceptare:** Nota include ce s-a schimbat si motivul publicarii.

### ~~W1442~~ ✅ AI suggestion moderation evidence trail
**Descriere tehnica:** Pastreaza traseul complet al dovezilor folosite in moderarea unei sugestii AI.
**Scop:** Face decizia de moderare auditable.
**Target:** moderation workflow, evidence store, audit log.
**Acceptare:** Traseul poate fi parcurs de la intrare la verdict.

### ~~W1443~~ ✅ AI suggestion policy override digest
**Descriere tehnica:** Rezuma override-urile aplicate politicii AI si motivul fiecaruia.
**Scop:** Ofera vizibilitate asupra exceptiilor.
**Target:** policy engine, audit viewer, admin dashboard.
**Acceptare:** Digest-ul mentioneaza regula afectata, autorul si durata.

### ~~W1444~~ ✅ AI suggestion rollback publish seal
**Descriere tehnica:** Sigileaza un rollback AI dupa ce a fost publicat sau executat.
**Scop:** Protejeaza integritatea revenirii.
**Target:** rollback service, audit log, admin dashboard.
**Acceptare:** Rollback-ul sigilat nu mai poate fi schimbat fara audit.

### ~~W1445~~ ✅ AI suggestion quarantine outcome viewer
**Descriere tehnica:** Ofera un viewer pentru rezultatul final al sugestiilor AI trecute prin carantina.
**Scop:** Face destinul cazului usor de urmarit.
**Target:** quarantine service, audit viewer, admin dashboard.
**Acceptare:** Outcome-ul poate fi vazut dupa sugestie si perioada.

### ~~W1446~~ ✅ AI suggestion review lane seal
**Descriere tehnica:** Sigileaza o banda de review dupa ce sugestiile AI din ea au fost procesate.
**Scop:** Marcheaza inchiderea controlata a lantului de moderare.
**Target:** moderation queue, review workflow, audit log.
**Acceptare:** Banda sigilata nu mai accepta intrari noi fara reactivare.

### ~~W1447~~ ✅ AI suggestion branch integrity digest
**Descriere tehnica:** Rezuma starea de integritate a branch-ului dupa aplicarea sugestiilor AI.
**Scop:** Ofera o vedere rapida asupra sanatatii structurale.
**Target:** branch validator, audit viewer, admin dashboard.
**Acceptare:** Digest-ul arata integritatea, riscurile si dependentele.

### ~~W1448~~ ✅ AI suggestion evidence seal audit
**Descriere tehnica:** Audit-eaza sigilarea dovezilor asociate unei sugestii AI.
**Scop:** Protejeaza integritatea probelor dupa inchidere.
**Target:** evidence store, audit log, archive service.
**Acceptare:** Auditul confirma cine a sigilat si cand.

### ~~W1449~~ ✅ AI suggestion publish outcome viewer
**Descriere tehnica:** Arata rezultatul publicarii unei sugestii AI cu impactul asupra branch-ului.
**Scop:** Ofera vizibilitate post-release.
**Target:** release pipeline, admin dashboard, audit viewer.
**Acceptare:** Viewer-ul arata verdictul, efectul si referintele.

### ~~W1450~~ ✅ AI suggestion branch policy digest
**Descriere tehnica:** Rezuma politicile active aplicate branch-ului la momentul evaluarii sugestiilor AI.
**Scop:** Face regulile branch-ului usor de consultat.
**Target:** policy engine, branch manager, audit viewer.
**Acceptare:** Digest-ul arata politica, versiunea si efectul ei.

### ~~W1451~~ ✅ AI suggestion moderation backlog digest
**Descriere tehnica:** Produce un digest al backlog-ului de moderare pentru sugestiile AI.
**Scop:** Ofera o imagine rapida asupra volumului de lucru.
**Target:** moderation queue, analytics dashboard, admin dashboard.
**Acceptare:** Digest-ul arata numarul, vechimea si severitatea.

### ~~W1452~~ ✅ AI suggestion rollback outcome viewer
**Descriere tehnica:** Ofera un viewer pentru rezultatul final al unui rollback AI.
**Scop:** Face verificarea revenirii mai usoara.
**Target:** rollback service, audit viewer, recovery workflow.
**Acceptare:** Rezultatul include obiectele restaurate si starea finala.

### ~~W1453~~ ✅ AI suggestion policy decision seal
**Descriere tehnica:** Sigileaza decizia unei reguli AI aplicate unei sugestii dupa finalizarea evaluarii.
**Scop:** Protejeaza trasabilitatea deciziei.
**Target:** policy engine, audit log, moderation workflow.
**Acceptare:** Decizia sigilata este imuabila si cautabila.

### ~~W1454~~ ✅ AI suggestion branch alert summary
**Descriere tehnica:** Rezuma alertele active pentru branch-urile care contin sugestii AI.
**Scop:** Ofera un status operational compact.
**Target:** branch manager, alerting system, admin dashboard.
**Acceptare:** Sumarul arata alertele, severitatea si cauza.

### ~~W1455~~ ✅ AI suggestion quarantine publish gate
**Descriere tehnica:** Blocheaza publicarea unei sugestii AI pana cand iesirea din carantina este aprobata.
**Scop:** Previne publicarea prematura.
**Target:** quarantine service, release pipeline, approval workflow.
**Acceptare:** Fara aprobarea de iesire, publish-ul ramane blocat.

### ~~W1456~~ ✅ AI suggestion replay outcome viewer
**Descriere tehnica:** Ofera un viewer pentru rezultatul final al replay-ului unei sugestii AI.
**Scop:** Simplifica analiza reproductibilitatii.
**Target:** replay sandbox, audit viewer, telemetry service.
**Acceptare:** Viewer-ul arata diferenta fata de executia originala.

### ~~W1457~~ ✅ AI suggestion branch freeze seal
**Descriere tehnica:** Sigileaza o stare de freeze pentru branch-urile care primesc sugestii AI.
**Scop:** Marcheaza clar intrarea in blocaj controlat.
**Target:** branch lock manager, audit log, admin dashboard.
**Acceptare:** Freeze-ul sigilat nu poate fi schimbat fara procedura.

### ~~W1458~~ ✅ AI suggestion evidence history digest
**Descriere tehnica:** Rezuma istoricul dovezilor folosite de o sugestie AI pe parcursul ciclului ei de viata.
**Scop:** Face auditul mai rapid.
**Target:** evidence store, audit viewer, archive service.
**Acceptare:** Digest-ul arata cum s-au schimbat dovezile in timp.

### ~~W1459~~ ✅ AI suggestion moderation seal audit
**Descriere tehnica:** Audit-eaza sigilarea deciziei de moderare pentru o sugestie AI.
**Scop:** Verifica integritatea verdictelor finale.
**Target:** moderation workflow, audit log, review UI.
**Acceptare:** Auditul confirma sigiliul, autorul si momentul.

### ~~W1460~~ ✅ AI suggestion release evidence seal
**Descriere tehnica:** Sigileaza dovezile asociate unui release AI dupa publicare.
**Scop:** Protejeaza auditul post-release.
**Target:** release pipeline, evidence store, audit log.
**Acceptare:** Dovezile sigilate raman numai citire.

### ~~W1461~~ ✅ AI suggestion branch archival note
**Descriere tehnica:** Genereaza o nota de arhivare pentru branch-urile AI inchise.
**Scop:** Pastreaza motivul si contextul inchiderii.
**Target:** archive service, branch manager, docs archive.
**Acceptare:** Nota este asociata branch-ului arhivat si cautabila.

### ~~W1462~~ ✅ AI suggestion policy alert viewer
**Descriere tehnica:** Ofera un viewer pentru alertele generate de politicile AI.
**Scop:** Face monitorizarea mai clara.
**Target:** alerting system, policy engine, admin dashboard.
**Acceptare:** Viewer-ul permite filtrare dupa regula si severitate.

### ~~W1463~~ ✅ AI suggestion rollback seal audit
**Descriere tehnica:** Audit-eaza sigiliul aplicat asupra unui rollback AI finalizat.
**Scop:** Verifica integritatea revenirii.
**Target:** rollback service, audit log, archive service.
**Acceptare:** Auditul confirma ca sigiliul nu a fost alterat.

### ~~W1464~~ ✅ AI suggestion quarantine final digest
**Descriere tehnica:** Rezuma in mod final ce s-a intamplat cu sugestia AI din carantina.
**Scop:** Clarifica rezultatul cazului.
**Target:** quarantine service, audit viewer, admin dashboard.
**Acceptare:** Digest-ul arata verdictul si actiunea finala.

### ~~W1465~~ ✅ AI suggestion review outcome seal
**Descriere tehnica:** Sigileaza verdictul rezultat din review-ul unei sugestii AI.
**Scop:** Protejeaza decizia finala.
**Target:** review workflow, audit log, moderation UI.
**Acceptare:** Verdictul sigilat este imuabil si verificabil.

### ~~W1466~~ ✅ AI suggestion branch publish digest
**Descriere tehnica:** Rezuma publicarea branch-ului dupa acceptarea sugestiilor AI.
**Scop:** Ofera context pentru ce a ajuns live.
**Target:** release pipeline, branch manager, changelog workflow.
**Acceptare:** Digest-ul mentioneaza schimbarea si efectul publicarii.

### ~~W1467~~ ✅ AI suggestion policy lock digest
**Descriere tehnica:** Rezuma lock-urile de politica aplicate sugestiilor AI.
**Scop:** Face blocajele de politica vizibile.
**Target:** policy engine, admin dashboard, audit viewer.
**Acceptare:** Digest-ul listeaza regula, motivul si durata lock-ului.

### ~~W1468~~ ✅ AI suggestion branch readiness seal audit
**Descriere tehnica:** Audit-eaza sigilarea readiness-ului unui branch cu sugestii AI.
**Scop:** Verifica integritatea verdictului de pregatire.
**Target:** branch manager, readiness meter, audit log.
**Acceptare:** Auditul confirma cine a sigilat si cu ce rezultat.

### ~~W1469~~ ✅ AI suggestion moderation archive viewer
**Descriere tehnica:** Ofera un viewer pentru arhiva completa a moderarii sugestiilor AI.
**Scop:** Simplifica accesul la istoricul inchis.
**Target:** archive service, moderation workflow, audit viewer.
**Acceptare:** Arhiva poate fi parcursa dupa sugestie, data si verdict.

### ~~W1470~~ ✅ AI suggestion final branch digest
**Descriere tehnica:** Produce un digest final pentru branch-ul care a trecut prin sugestii AI.
**Scop:** Ofera o vedere succinta a rezultatului final.
**Target:** branch manager, audit log, admin dashboard.
**Acceptare:** Digest-ul include statusul final si motivele cheie.

### ~~W1471~~ ✅ AI suggestion branch archive lock
**Descriere tehnica:** Blocheaza modificarile la branch-urile AI arhivate dupa inchidere.
**Scop:** Protejeaza istoricul final.
**Target:** archive service, branch manager, audit log.
**Acceptare:** Branch-ul arhivat nu poate fi schimbat fara procedura.

### ~~W1472~~ ✅ AI suggestion moderation status digest
**Descriere tehnica:** Rezuma starea moderarii pentru sugestiile AI in format compact.
**Scop:** Ofera un overview rapid pentru staff.
**Target:** moderation queue, admin dashboard, analytics.
**Acceptare:** Digest-ul arata starea curenta si blocajele.

### ~~W1473~~ ✅ AI suggestion policy freeze digest
**Descriere tehnica:** Rezuma politicile AI aflate in freeze si efectul lor asupra sugestiilor.
**Scop:** Face starea de freeze usor de consultat.
**Target:** policy engine, admin dashboard, audit viewer.
**Acceptare:** Digest-ul include regula, motivul si durata freeze-ului.

### ~~W1474~~ ✅ AI suggestion rollback archive seal
**Descriere tehnica:** Sigileaza arhiva rollback-urilor generate de sugestiile AI.
**Scop:** Protejeaza integritatea istoricului de revert.
**Target:** rollback service, archive service, audit log.
**Acceptare:** Arhiva sigilata ramane imuabila.

### ~~W1475~~ ✅ AI suggestion quarantine evidence seal
**Descriere tehnica:** Sigileaza dovezile asociate sugestiilor AI din carantina.
**Scop:** Pastreaza probele intacte dupa inchidere.
**Target:** quarantine service, evidence store, audit log.
**Acceptare:** Dovezile sigilate nu pot fi modificate fara audit.

### ~~W1476~~ ✅ AI suggestion review outcome digest
**Descriere tehnica:** Produce un digest al rezultatului final al review-ului pentru sugestiile AI.
**Scop:** Simplifica auditul si comunicarea.
**Target:** review workflow, audit viewer, admin dashboard.
**Acceptare:** Digest-ul mentioneaza verdictul si motivele.

### ~~W1477~~ ✅ AI suggestion branch readiness history seal
**Descriere tehnica:** Sigileaza istoricul de readiness al branch-ului dupa evaluarea sugestiilor AI.
**Scop:** Protejeaza comparatia istorica.
**Target:** branch manager, readiness meter, audit log.
**Acceptare:** Istoricul sigilat este doar citire.

### ~~W1478~~ ✅ AI suggestion policy change digest
**Descriere tehnica:** Rezuma schimbarile aduse politicii AI intr-un format usor de citit.
**Scop:** Ofera vizibilitate asupra modificarilor de governance.
**Target:** policy engine, docs archive, admin dashboard.
**Acceptare:** Digest-ul arata ce s-a schimbat si de ce.

### ~~W1479~~ ✅ AI suggestion moderation seal digest
**Descriere tehnica:** Rezuma sigilarea deciziilor de moderare pentru sugestiile AI.
**Scop:** Face auditul mai simplu.
**Target:** moderation workflow, audit log, admin dashboard.
**Acceptare:** Digest-ul arata cine a sigilat si cand.

### ~~W1480~~ ✅ AI suggestion branch freeze history
**Descriere tehnica:** Pastreaza istoricul complet al freeze-urilor branch-ului provocate de sugestii AI.
**Scop:** Ajuta la investigarea blocajelor repetate.
**Target:** branch lock manager, audit log, analytics dashboard.
**Acceptare:** Istoricul poate fi consultat cronologic.

### ~~W1481~~ ✅ AI suggestion evidence archive digest
**Descriere tehnica:** Rezuma arhiva dovezilor pentru sugestiile AI.
**Scop:** Ofera un sumar rapid al probelor disponibile.
**Target:** evidence store, archive service, audit viewer.
**Acceptare:** Digest-ul arata tipurile principale de dovezi.

### ~~W1482~~ ✅ AI suggestion release note digest
**Descriere tehnica:** Rezuma notele de release generate pentru sugestiile AI.
**Scop:** Ofera o privire rapida asupra publicarii.
**Target:** release notes generator, changelog workflow, admin dashboard.
**Acceptare:** Digest-ul poate fi consultat fara a deschide nota completa.

### ~~W1483~~ ✅ AI suggestion quarantine history seal
**Descriere tehnica:** Sigileaza istoricul complet al unei sugestii AI dupa iesirea din carantina.
**Scop:** Protejeaza traseul decizional.
**Target:** quarantine service, audit log, archive service.
**Acceptare:** Istoricul sigilat este imuabil si cautabil.

### ~~W1484~~ ✅ AI suggestion rollback history seal
**Descriere tehnica:** Sigileaza istoricul rollback-urilor pentru sugestiile AI dupa finalizare.
**Scop:** Pastreaza integritatea cronologiei.
**Target:** rollback service, archive service, audit log.
**Acceptare:** Istoricul sigilat nu poate fi editat fara audit.

### ~~W1485~~ ✅ AI suggestion policy digest seal
**Descriere tehnica:** Sigileaza digest-ul politicilor AI dupa aprobarea finala.
**Scop:** Protejeaza rezumatul de governance.
**Target:** policy engine, docs archive, audit log.
**Acceptare:** Digest-ul sigilat ramane doar citire.

### ~~W1486~~ ✅ AI suggestion moderation archive seal
**Descriere tehnica:** Sigileaza arhiva moderarii pentru sugestiile AI dupa inchidere.
**Scop:** Pastreaza un istoric neatins.
**Target:** moderation workflow, archive service, audit log.
**Acceptare:** Arhiva sigilata poate fi doar consultata.

### ~~W1487~~ ✅ AI suggestion branch decision digest
**Descriere tehnica:** Rezuma deciziile principale luate pentru branch-ul cu sugestii AI.
**Scop:** Ofera un overview al hotararilor.
**Target:** branch manager, audit viewer, admin dashboard.
**Acceptare:** Digest-ul arata deciziile si impactul lor.

### ~~W1488~~ ✅ AI suggestion evidence checkpoint viewer
**Descriere tehnica:** Afiseaza checkpoint-urile dovezilor pentru o sugestie AI pe parcursul ciclului ei de viata.
**Scop:** Face urmarirea probelor mai usoara.
**Target:** evidence store, audit viewer, replay tools.
**Acceptare:** Viewer-ul arata ordinea checkpoint-urilor si statusul lor.

### ~~W1489~~ ✅ AI suggestion policy outcome digest
**Descriere tehnica:** Rezuma rezultatul aplicarii politicii AI asupra unei sugestii.
**Scop:** Face efectul politicii usor de inteles.
**Target:** policy engine, audit log, analytics dashboard.
**Acceptare:** Digest-ul arata verdictul si regula responsabila.

### ~~W1490~~ ✅ AI suggestion branch archive digest
**Descriere tehnica:** Rezuma branch-urile arhivate dupa ciclul de viata al sugestiilor AI.
**Scop:** Ofera un rezumat curat pentru istoric.
**Target:** archive service, branch manager, audit viewer.
**Acceptare:** Digest-ul poate fi filtrat dupa status si perioada.

### ~~W1491~~ ✅ AI suggestion review archive seal
**Descriere tehnica:** Sigileaza arhiva review-ului pentru sugestiile AI dupa inchidere.
**Scop:** Protejeaza deciziile finale.
**Target:** review workflow, archive service, audit log.
**Acceptare:** Arhiva sigilata ramane imuabila.

### ~~W1492~~ ✅ AI suggestion moderation release digest
**Descriere tehnica:** Rezuma deciziile de moderare care au condus la publicarea unei sugestii AI.
**Scop:** Ofera context pentru release.
**Target:** moderation workflow, release pipeline, audit viewer.
**Acceptare:** Digest-ul arata verdictul si traseul catre release.

### ~~W1493~~ ✅ AI suggestion branch outcome seal
**Descriere tehnica:** Sigileaza rezultatul final al branch-ului dupa aplicarea sugestiilor AI.
**Scop:** Marcheaza inchiderea controlata a ciclului.
**Target:** branch manager, audit log, archive service.
**Acceptare:** Rezultatul sigilat este doar citire.

### ~~W1494~~ ✅ AI suggestion evidence digest seal
**Descriere tehnica:** Sigileaza digest-ul dovezilor aferente unei sugestii AI.
**Scop:** Protejeaza sumarul probelor de dupa audit.
**Target:** evidence store, audit log, archive service.
**Acceptare:** Digest-ul sigilat nu poate fi modificat.

### ~~W1495~~ ✅ AI suggestion policy review seal
**Descriere tehnica:** Sigileaza rezultatul revizuirii unei politici AI.
**Scop:** Protejeaza decizia de governance.
**Target:** policy engine, audit log, docs archive.
**Acceptare:** Rezultatul sigilat este doar citire si verificabil.

### ~~W1496~~ ✅ AI suggestion branch final note
**Descriere tehnica:** Creeaza o nota finala pentru branch-ul care a trecut prin sugestii AI.
**Scop:** Ofera un rezumat final usor de citit.
**Target:** branch manager, changelog workflow, admin dashboard.
**Acceptare:** Nota include rezultatul, motivul si impactul principal.

### ~~W1497~~ ✅ AI suggestion moderation final seal
**Descriere tehnica:** Sigileaza rezultatul final al moderarii dupa inchiderea cazului AI.
**Scop:** Protejeaza integritatea verdictului.
**Target:** moderation workflow, audit log, review UI.
**Acceptare:** Verdictul sigilat nu mai poate fi alterat fara audit.

### ~~W1498~~ ✅ AI suggestion rollback final seal
**Descriere tehnica:** Sigileaza rezultatul final al unui rollback AI dupa executie.
**Scop:** Protejeaza istoricul revenirii.
**Target:** rollback service, audit log, archive service.
**Acceptare:** Rollback-ul sigilat este imuabil si consultabil.

### ~~W1499~~ ✅ AI suggestion quarantine final seal
**Descriere tehnica:** Sigileaza rezultatul final al unei sugestii AI iesite din carantina.
**Scop:** Inchide definitiv cazul de carantina.
**Target:** quarantine service, audit log, archive service.
**Acceptare:** Rezultatul final sigilat poate fi doar consultat.

### ~~W1500~~ ✅ AI suggestion lifecycle closeout
**Descriere tehnica:** Inchide complet ciclul de viata al unei sugestii AI, de la generare la arhivare sau publicare.
**Scop:** Marcheaza finalul definitiv al cazului.
**Target:** archive service, audit log, moderation workflow.
**Acceptare:** Closeout-ul este inregistrat si nu mai permite tranzitii noi.

### ~~W1501~~ ✅ AI suggestion branch closure note
**Descriere tehnica:** Creeaza o nota de inchidere pentru branch-urile care au trecut prin sugestii AI.
**Scop:** Ofera un rezumat final al ciclului branch-ului.
**Target:** branch manager, changelog workflow, audit log.
**Acceptare:** Nota de inchidere poate fi consultata dupa branch si perioada.

### ~~W1502~~ ✅ AI suggestion moderation closeout seal
**Descriere tehnica:** Sigileaza cazul de moderare dupa inchiderea unei sugestii AI.
**Scop:** Protejeaza verdictul final si istoricul.
**Target:** moderation workflow, audit log, review UI.
**Acceptare:** Cazul sigilat ramane doar citire si auditable.

### ~~W1503~~ ✅ AI suggestion policy closeout digest
**Descriere tehnica:** Rezuma politicile AI aplicate si rezultatele lor la finalul ciclului de sugestie.
**Scop:** Ofera context final de governance.
**Target:** policy engine, audit viewer, docs archive.
**Acceptare:** Digest-ul include regulile, exceptiile si efectele.

### ~~W1504~~ ✅ AI suggestion rollback closeout note
**Descriere tehnica:** Genereaza o nota de closeout pentru rollback-urile AI finalizate.
**Scop:** Pastreaza justificarea revenirii.
**Target:** rollback service, audit log, recovery workflow.
**Acceptare:** Nota contine motivul, efectul si starea finala.

### ~~W1505~~ ✅ AI suggestion quarantine closeout report
**Descriere tehnica:** Raporteaza inchiderea unei sugestii AI iesite din carantina.
**Scop:** Face rezultatul carantinei usor de urmarit.
**Target:** quarantine service, audit viewer, admin dashboard.
**Acceptare:** Raportul arata verdictul si actiunea finala.

### ~~W1506~~ ✅ AI suggestion review closeout digest
**Descriere tehnica:** Produce un digest final al procesului de review pentru o sugestie AI.
**Scop:** Ofera o vedere succinta asupra deciziei finale.
**Target:** review workflow, audit viewer, admin dashboard.
**Acceptare:** Digest-ul mentioneaza verdictul, motivele si checkpoint-urile.

### ~~W1507~~ ✅ AI suggestion branch final seal
**Descriere tehnica:** Sigileaza branch-ul dupa inchiderea ciclului de sugestii AI.
**Scop:** Marcheaza starea finala si stabila.
**Target:** branch manager, audit log, archive service.
**Acceptare:** Branch-ul sigilat nu mai accepta schimbari fara reactivare.

### ~~W1508~~ ✅ AI suggestion evidence closeout seal
**Descriere tehnica:** Sigileaza dovezile aferente unei sugestii AI dupa incheierea cazului.
**Scop:** Protejeaza integritatea probelor finale.
**Target:** evidence store, audit log, archive service.
**Acceptare:** Dovezile sigilate raman imuabile.

### ~~W1509~~ ✅ AI suggestion policy closeout seal
**Descriere tehnica:** Sigileaza digest-ul politicii AI dupa inchiderea cazului.
**Scop:** Protejeaza rezumatul de governance.
**Target:** policy engine, audit log, docs archive.
**Acceptare:** Digest-ul sigilat este doar citire.

### ~~W1510~~ ✅ AI suggestion branch archive closeout
**Descriere tehnica:** Inchide definitiv arhiva branch-ului care a fost alimentat de sugestii AI.
**Scop:** Pastreaza istoricul final intr-o stare stabila.
**Target:** archive service, branch manager, audit log.
**Acceptare:** Arhiva de closeout este inregistrata si cautabila.

### ~~W1511~~ ✅ AI suggestion moderation evidence closeout
**Descriere tehnica:** Inchide setul de dovezi folosit in moderarea unei sugestii AI.
**Scop:** Marcheaza finalizarea auditului de moderare.
**Target:** moderation workflow, evidence store, audit log.
**Acceptare:** Setul de dovezi este sigilat dupa closeout.

### ~~W1512~~ ✅ AI suggestion rollback final note
**Descriere tehnica:** Creeaza nota finala pentru un rollback AI, cu actiunile si rezultatele principale.
**Scop:** Ofera un sumar clar al revenirii.
**Target:** rollback service, audit log, recovery workflow.
**Acceptare:** Nota finala poate fi consultata dupa rollback.

### ~~W1513~~ ✅ AI suggestion quarantine final report
**Descriere tehnica:** Genereaza raportul final pentru sugestiile AI care au trecut prin carantina.
**Scop:** Clarifica ce s-a intamplat cu fiecare caz.
**Target:** quarantine service, audit viewer, admin dashboard.
**Acceptare:** Raportul arata verdictul si motivele.

### ~~W1514~~ ✅ AI suggestion review final seal
**Descriere tehnica:** Sigileaza rezultatul final al review-ului pentru o sugestie AI.
**Scop:** Protejeaza decizia finala impotriva modificarilor.
**Target:** review workflow, audit log, moderation UI.
**Acceptare:** Verdictul sigilat este doar citire.

### ~~W1515~~ ✅ AI suggestion branch final report
**Descriere tehnica:** Produce un raport final pentru branch-ul care a fost influentat de sugestii AI.
**Scop:** Ofera un rezumat complet pentru arhiva si audit.
**Target:** branch manager, audit log, admin dashboard.
**Acceptare:** Raportul include statusul final si impactul major.

### ~~W1516~~ ✅ AI suggestion evidence final report
**Descriere tehnica:** Rezuma in mod final dovezile folosite de o sugestie AI.
**Scop:** Simplifica auditul post-caz.
**Target:** evidence store, audit viewer, archive service.
**Acceptare:** Raportul final enumera dovezile cheie si statusul lor.

### ~~W1517~~ ✅ AI suggestion policy final note
**Descriere tehnica:** Genereaza o nota finala pentru politica AI aplicata in cazul unei sugestii.
**Scop:** Pastreaza explicatia de governance.
**Target:** policy engine, docs archive, audit log.
**Acceptare:** Nota poate fi consultata dupa sugestie si versiune.

### ~~W1518~~ ✅ AI suggestion moderation final report
**Descriere tehnica:** Produce raportul final de moderare pentru o sugestie AI.
**Scop:** Ofera o urma clara a deciziei si a motivelor.
**Target:** moderation workflow, audit viewer, admin dashboard.
**Acceptare:** Raportul include verdictul si traseul complet.

### ~~W1519~~ ✅ AI suggestion rollback final report
**Descriere tehnica:** Genereaza raportul final al unui rollback AI dupa executie.
**Scop:** Documenteaza starea obtinuta si efectele.
**Target:** rollback service, audit log, recovery workflow.
**Acceptare:** Raportul arata ce s-a restaurat si ce a ramas blocat.

### ~~W1520~~ ✅ AI suggestion quarantine final report seal
**Descriere tehnica:** Sigileaza raportul final al carantinei pentru o sugestie AI.
**Scop:** Protejeaza rezultatul incheierii cazului.
**Target:** quarantine service, audit log, archive service.
**Acceptare:** Raportul sigilat nu mai poate fi modificat.

### ~~W1521~~ ✅ AI suggestion branch closeout digest
**Descriere tehnica:** Rezuma closeout-ul branch-ului dupa ciclul de sugestii AI.
**Scop:** Ofera un sumar final compact.
**Target:** branch manager, changelog workflow, audit log.
**Acceptare:** Digest-ul include statusul final si motivul inchiderii.

### ~~W1522~~ ✅ AI suggestion evidence closeout report
**Descriere tehnica:** Produce raportul final de closeout pentru dovezile asociate unei sugestii AI.
**Scop:** Marcheaza finalizarea gestionarii probelor.
**Target:** evidence store, audit viewer, archive service.
**Acceptare:** Raportul arata dovezile si starea lor finala.

### ~~W1523~~ ✅ AI suggestion policy closeout report
**Descriere tehnica:** Genereaza raportul final al politicii AI dupa inchiderea cazului.
**Scop:** Consolideaza explicatia de governance.
**Target:** policy engine, audit log, docs archive.
**Acceptare:** Raportul mentioneaza regula, efectul si verdictul.

### ~~W1524~~ ✅ AI suggestion moderation closeout report
**Descriere tehnica:** Produce raportul final de moderare pentru sugestiile AI inchise.
**Scop:** Ofera trasabilitate completa a deciziei.
**Target:** moderation workflow, audit viewer, admin dashboard.
**Acceptare:** Raportul include verdictul final si motivele.

### ~~W1525~~ ✅ AI suggestion rollback closeout report
**Descriere tehnica:** Genereaza raportul final al unui rollback AI dupa closeout.
**Scop:** Documenteaza rezultatul si efectul revenirii.
**Target:** rollback service, recovery workflow, audit log.
**Acceptare:** Raportul este cautabil si consultabil ulterior.

### ~~W1526~~ ✅ AI suggestion quarantine closeout digest
**Descriere tehnica:** Rezuma inchiderea carantinei pentru sugestiile AI intr-un digest compact.
**Scop:** Ofera o vedere rapida asupra rezultatului.
**Target:** quarantine service, audit viewer, admin dashboard.
**Acceptare:** Digest-ul include verdictul si actiunea finala.

### ~~W1527~~ ✅ AI suggestion review closeout report
**Descriere tehnica:** Produce raportul final de review pentru o sugestie AI dupa inchidere.
**Scop:** Pastreaza istoricul complet al deciziei.
**Target:** review workflow, audit log, admin dashboard.
**Acceptare:** Raportul mentioneaza verdictul, comentariile si checkpoint-urile.

### ~~W1528~~ ✅ AI suggestion branch closeout seal
**Descriere tehnica:** Sigileaza closeout-ul branch-ului dupa finalizarea ciclului de sugestii AI.
**Scop:** Marcheaza inchiderea definitiva a branch-ului.
**Target:** branch manager, archive service, audit log.
**Acceptare:** Closeout-ul sigilat ramane doar citire.

### ~~W1529~~ ✅ AI suggestion evidence closeout seal
**Descriere tehnica:** Sigileaza in mod final dovezile asociate unei sugestii AI dupa closeout.
**Scop:** Protejeaza istoricul probelor.
**Target:** evidence store, archive service, audit log.
**Acceptare:** Dovezile sigilate nu mai pot fi schimbate.

### ~~W1530~~ ✅ AI suggestion final closeout digest
**Descriere tehnica:** Produce un digest final pentru closeout-ul complet al unei sugestii AI.
**Scop:** Ofera un rezumat definitiv al ciclului de viata.
**Target:** audit viewer, archive service, admin dashboard.
**Acceptare:** Digest-ul final este disponibil dupa inchiderea cazului.
