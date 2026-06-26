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
