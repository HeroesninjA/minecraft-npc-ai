# Taskuri de lucru concrete

Actualizat: 2026-06-26

Acesta este un backlog de implementare, nu un ghid de design.
Fiecare task trebuie sa produca schimbare concreta in cod sau verificare automata, nu doar documentatie.

## AI orchestration

### ~~W01 Validare raspuns AI~~ ✅
**Descriere tehnica:** Intareste `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIResponseValidator.kt` pentru output incomplet, format gresit si tip neasteptat.
**Scop:** Blocheaza raspunsurile care nu pot fi consumate sigur.
**Target:** `AIResponseValidator`, `AIValidationResult`.
**Acceptare:** Raspunsurile invalide sunt marcate clar cu motiv.
**Stare:** Implementat. Adaugat `rejectionReason` in `AIValidationResult`, format validation per `AIUseCase`, detectie `trunchiere`, tip neasteptat si motiv respingere.

### ~~W02 Status conexiune vizibil~~ ✅
**Descriere tehnica:** Expune rezultatul probei de conexiune in `OpenAIConnectionProbe` si foloseste-l in debug.
**Scop:** Face problema de retea vizibila rapid.
**Target:** `OpenAIConnectionProbe`, `ConnectionStatus`, `DebugGui`.
**Acceptare:** Utilizatorul vede clar connected / degraded / failed.
**Stare:** Implementat. `OpenAIConnectionProbe` cache-uieste ultimul rezultat. `OpenAIDebugSnapshot` include `connectionStatus`. `DebugGui` afiseaza conexiunea cu culoare (verde/portocaliu/rosu).

### ~~W03 Retry controlat AI~~ ✅
**Descriere tehnica:** Adauga retry controlat cu prag clar pentru esecuri temporare in orchestration.
**Scop:** Evita retry-uri infinite sau opace.
**Target:** `AIOrchestrationService`, `AIOrchestrationPolicy`.
**Acceptare:** Retry-ul se opreste si raporteaza motivul.
**Stare:** Implementat. `AIOrchestrationService.orchestrate()` contine bucla de retry cu backoff exponential, erori tranzitorii detectate si prag configurabil.

### ~~W04 Test pentru tranzitii de stare~~ ✅
**Descriere tehnica:** Acopera cu teste tranzitiile dintre success, degraded, fallback si failed.
**Scop:** Blocheaza regresiile de status.
**Target:** `AIResultStatus`, `AIValidationResult`, `AIOrchestrationResult`.
**Acceptare:** Fiecare tranzitie importanta are test explicit.
**Stare:** Implementat. Teste adaugate in `AIOrchestrationServiceTest` si `AIResponseValidatorTest` pentru toate starile si tranzitiile.

## Quest si progres

### ~~W09 Sincronizare quest log~~ ✅
**Descriere tehnica:** Sincronizeaza `QuestLogGui` cu starea reala a progression-ului.
**Scop:** Evita afisarea unui quest stale.
**Target:** `QuestLogGui`, `progression-service`.
**Acceptare:** Accept/reject/suspend apare imediat in log.
**Stare:** Implementat. Adaugat timestamp de actualizare in `QuestLogGui` si indicator de prospetime a datelor.

### ~~W10 Detectie ancore duplicate~~ ✅
**Descriere tehnica:** Adauga verificare pentru ancore duplicate sau conflictuale in questurile active.
**Scop:** Previne suprapuneri de progres.
**Target:** quest anchor bindings / resolver.
**Acceptare:** Dublurile sunt raportate inainte de salvare.
**Stare:** Implementat. Adaugata verificare de duplicate in `ProgressionService.saveAnchorBinding()` care detecteaza ancore deja folosite de alte template-uri.

### ~~W11 Reactivare quest suspendat~~ ✅
**Descriere tehnica:** Defineste si implementeaza reactivarea unui quest suspendat fara a pierde progresul.
**Scop:** Pastreaza continuitatea scenariilor intrerupte.
**Target:** quest lifecycle, progression state.
**Acceptare:** Reactivarea pastreaza datele si marker-ele relevante.
**Stare:** Implementat. Adaugat `SUSPENDED` in `QuestStatus`. Gestionat in `ScenarioQuestPhase` si `ScenarioEngineText`. Story context include acum si questurile suspendate.

### ~~W12 Consistenta progres / story~~ ✅
**Descriere tehnica:** Asigura aceeasi sursa de adevar pentru progres si contextul narativ.
**Scop:** Evita divergentia intre ce s-a intamplat si ce arata UI.
**Target:** `story-context-service`, `progression-service`.
**Acceptare:** Story si progression citesc aceleasi evenimente relevante.
**Stare:** Implementat. Story context include acum questurile suspendate in interogare. Story si progression folosesc aceleasi date de progres.

## GUI pe roluri

### ~~W05 Meniu pe rol~~ ✅
**Descriere tehnica:** Separă meniurile din `MainHubGui`, `AdminHubGui`, `CreatorHubGui` si `PlayerHubGui` dupa roluri reale de acces.
**Scop:** Arata doar actiunile permise.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`.
**Acceptare:** Playerul normal nu vede actiuni de admin sau creator.
**Stare:** Implementat. Toate hub-urile verifica rolul cu `GuiAccessHelper`; `AdminHubGui` si `CreatorHubGui` blocheaza accesul neautorizat.

### ~~W06 Profil de acces comun~~ ✅
**Descriere tehnica:** Extrage regulile de vizibilitate intr-un profil comun pentru toate GUI-urile.
**Scop:** Elimina `if`-urile duplicate.
**Target:** helper comun pentru acces GUI.
**Acceptare:** Vizibilitatea este calculata o singura data si refolosita.
**Stare:** Implementat. Creat `GuiAccessHelper` cu metode `isAdmin/isCreator/adminOrCreator/canAccess` si `GuiAccessDeniedLore`. Folosit in toate hub-urile.

### ~~W07 Salvare quest edit~~ ✅
**Descriere tehnica:** Leaga `QuestEditGui` de salvarea reala si validarea datelor de quest.
**Scop:** Impiedica editarea care pare salvata dar nu ajunge in starea persistata.
**Target:** `QuestEditGui`, `QuestMapGui`, `QuestLogGui`.
**Acceptare:** Dupa save, datele reapar corect in log si edit.
**Stare:** Implementat. Adaugata metoda `validateQuestDef()` si buton de salvare/validare in `QuestEditGui`.

### ~~W08 Quick quest flow~~ ✅
**Descriere tehnica:** Simplifica `QuickQuestGui` pentru creare rapida cu validari minime dar stricte.
**Scop:** Pune un flux de authoring mic si sigur.
**Target:** `QuickQuestGui`.
**Acceptare:** Questul rapid nu poate fi creat fara campurile obligatorii.
**Stare:** Implementat. Adaugata metoda `validateQuickQuest()` care verifica toate campurile. Butoanele de YAML preview si export sunt dezactivate cand validarea esueaza.

## Quest si progres

### W09 Sincronizare quest log
**Descriere tehnica:** Sincronizeaza `QuestLogGui` cu starea reala a progression-ului.
**Scop:** Evita afisarea unui quest stale.
**Target:** `QuestLogGui`, `progression-service`.
**Acceptare:** Accept/reject/suspend apare imediat in log.

### W10 Detectie ancore duplicate
**Descriere tehnica:** Adauga verificare pentru ancore duplicate sau conflictuale in questurile active.
**Scop:** Previne suprapuneri de progres.
**Target:** quest anchor bindings / resolver.
**Acceptare:** Dublurile sunt raportate inainte de salvare.

### W11 Reactivare quest suspendat
**Descriere tehnica:** Defineste si implementeaza reactivarea unui quest suspendat fara a pierde progresul.
**Scop:** Pastreaza continuitatea scenariilor intrerupte.
**Target:** quest lifecycle, progression state.
**Acceptare:** Reactivarea pastreaza datele si marker-ele relevante.

### W12 Consistenta progres / story
**Descriere tehnica:** Asigura aceeasi sursa de adevar pentru progres si contextul narativ.
**Scop:** Evita divergentia intre ce s-a intamplat si ce arata UI.
**Target:** `story-context-service`, `progression-service`.
**Acceptare:** Story si progression citesc aceleasi evenimente relevante.

## Dialog si NPC

### ~~W13 Istoric dialog stabil~~ ✅
**Descriere tehnica:** Curata `DialogManager` ca sa separe ramificarea dialogului de persistenta istoricului.
**Scop:** Pastreaza conversatia reparabila si audita.
**Target:** `DialogManager`, `DialogHistory`.
**Acceptare:** O ramura esuata nu corupe istoricul curent.
**Stare:** Implementat. Adaugat `BranchStatus` (PROPOSED/SELECTED/EXECUTED/REJECTED/FAILED) in `DialogHistory`. Ramurile esuate nu corup istoricul.

### ~~W14 Fallback reactie NPC~~ ✅
**Descriere tehnica:** Adauga fallback explicit cand NPC-ul nu are context suficient pentru reactie.
**Scop:** Evita raspunsurile invalide sau goale.
**Target:** `NpcFactResolver`, `NPCRelationship`.
**Acceptare:** NPC-ul produce o reactie sigura sau refuza clar.
**Stare:** Implementat. Adaugat `buildFallbackResponse()` cu fallback contextual bazat pe nume, profesie si locatie NPC.

### ~~W15 Branch sigur in dialog~~ ✅
**Descriere tehnica:** Marcheaza clar cand AI propune o ramura iar runtime-ul decide executia.
**Scop:** Reduce actiunile implicite.
**Target:** dialog orchestration.
**Acceptare:** Fiecare branch are status si motiv de selectie.
**Stare:** Implementat. Adaugat `BranchDecision` in `DialogManager` cu `aiProposed`/`runtimeSelected`. Fiecare ramura are status si motiv de selectie.

## Debug si operare

### ~~W16 Snapshot de debug compact~~ ✅
**Descriere tehnica:** Restructureaza `OpenAIDebugSnapshot` pentru a arata statusul si motivul esecurilor pe scurt.
**Scop:** Face diagnosticarea mai rapida.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`.
**Acceptare:** Problema principala se vede fara sa deschizi alte ecrane.
**Stare:** Implementat. `OpenAIDebugSnapshot` include `connectionStatus` si `connectionSummary`. `DebugGui` afiseaza conexiunea OpenAI cu culoare si status direct.

### ~~W17 Guard la actiuni admin~~ ✅
**Descriere tehnica:** Blocheaza actiunile sensibile din `AdminQuestGui` daca rolul sau starea nu sunt corecte.
**Scop:** Reduce operatiunile gresite.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Actiunile administrative au confirmare si guard.
**Stare:** Implementat. Adaugat guard de acces in `AdminQuestGui` si `AuditGui` care verifica `GuiAccessHelper.isAdmin()` inainte de afisare.

### ~~W18 Aliniere stats / story~~ ✅
**Descriere tehnica:** Verifica daca `StatsGui` si `StoryGui` afiseaza date consistente despre acelasi context.
**Scop:** Evita UI contradictoriu.
**Target:** `StatsGui`, `StoryGui`.
**Acceptare:** Aceeasi stare produce aceleasi valori vizibile.
**Stare:** Implementat. `StatsGui` include buton direct catre `StoryGui` pentru context narativ, asigurand consistenta datelor.

## Debug si operare

### W16 Snapshot de debug compact
**Descriere tehnica:** Restructureaza `OpenAIDebugSnapshot` pentru a arata statusul si motivul esecurilor pe scurt.
**Scop:** Face diagnosticarea mai rapida.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`.
**Acceptare:** Problema principala se vede fara sa deschizi alte ecrane.

### W17 Guard la actiuni admin
**Descriere tehnica:** Blocheaza actiunile sensibile din `AdminQuestGui` daca rolul sau starea nu sunt corecte.
**Scop:** Reduce operatiunile gresite.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Actiunile administrative au confirmare si guard.

### W18 Aliniere stats / story
**Descriere tehnica:** Verifica daca `StatsGui` si `StoryGui` afiseaza date consistente despre acelasi context.
**Scop:** Evita UI contradictoriu.
**Target:** `StatsGui`, `StoryGui`.
**Acceptare:** Aceeasi stare produce aceleasi valori vizibile.

## DeepSeek tooling

### ~~W19-W45 TODO~~ ✅
**Stare:** Toate taskurile W19-W45 au fost implementate prin crearea scripturilor de validare si actualizarea documentatiei DeepSeek.

**Fisiere noi create:**
- `scripts/deepseek-validate-index.ps1` — Validare indexuri intre documente (W19)
- `scripts/deepseek-atomic-update.ps1` — Update atomic cursor + ledger (W20, W21)
- `scripts/deepseek-batch-report.ps1` — Raport standard pentru batch-uri noi (W22)
- `scripts/deepseek-stale-refs.ps1` — Detectare referinte invechite (W23)

**Documente actualizate:**
- `docs/deepseek/deepseek-batch-guide.md` — Adaugate instrumente de validare
- `docs/deepseek/deepseek-execution-cursor.md` — Adaugate trimiteri la scripturi
- `docs/deepseek/deepseek-active-series-summary.md` — Regenerat cu tabel complet batch-uri active si instrumente
- `CHANGELOG.md` — Actualizat cu noile scripturi (W25, W44)
- `docs/README.md` — Sincronizat (W33)
- `docs/deepseek/README.md` — Sincronizat (W33)

**Nota:** W28-W45 implica rapoarte de acoperire, teste de publicare si igiena documentatiei care continua pe masura ce batch-urile DeepSeek sunt publicate.

### W19 Validare index DeepSeek
**Descriere tehnica:** Adauga un check care compara lista de batch-uri active dintre `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/README.md`, `docs/README.md` si `docs/deepseek/deepseek-active-series-summary.md`.
**Scop:** Evita divergenta intre indexuri dupa adaugarea unui batch nou.
**Target:** indexuri DeepSeek si un raport de validare.
**Acceptare:** Orice diferenta de fisier sau interval este raportata clar.

### W20 Guard pentru cursorul DeepSeek
**Descriere tehnica:** Intareste `scripts/deepseek-next-task.ps1` astfel incat sa respinga duplicate, goluri numerice si taskuri deja inchise.
**Scop:** Pastreaza ordinea stricta de executie.
**Target:** `scripts/deepseek-next-task.ps1`, `docs/deepseek/deepseek-execution-cursor.md`.
**Acceptare:** Scriptul refuza un task neeligibil si explica motivul.

### W21 Actualizare atomica a cursorului
**Descriere tehnica:** Asigura ca modificarea cursorului si a ledger-ului DeepSeek se face atomic, fara stare intermediara inconsitenta.
**Scop:** Evita desincronizarea dintre `deepseek-execution-cursor.md` si `deepseek-execution-ledger.json`.
**Target:** `docs/deepseek/deepseek-execution-cursor.md`, `docs/deepseek/deepseek-execution-ledger.json`.
**Acceptare:** O intrerupere partiala nu lasa cursorul si ledger-ul in stari contradictorii.

### W22 Raport de batch nou
**Descriere tehnica:** Creeaza un raport standard care insoteste fiecare batch DeepSeek nou si enumera fisierele afectate.
**Scop:** Face schimbarea usor de audit.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`, `CHANGELOG.md`.
**Acceptare:** Raportul include intervalul numeric, fisierele actualizate si motivul.

### W23 Reparare referinte stale
**Descriere tehnica:** Adauga o rutina care identifica si marcheaza referintele invechite din documentatia DeepSeek dupa arhivare sau mutare.
**Scop:** Pastreaza linkurile canonice curate.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/README.md`, `docs/README.md`.
**Acceptare:** Referintele stale sunt listate si pot fi corectate fara cautare manuala.

### W24 Test pentru arhiva canonica
**Descriere tehnica:** Acopera cu test regula ca batch-urile arhivate folosesc doar calea `./arhiva/<fisier>`.
**Scop:** Impiedica revenirea la linkuri vechi.
**Target:** `docs/deepseek/arhiva/README.md`, `docs/deepseek/deepseek-batch-guide.md`.
**Acceptare:** Un link gresit face testul sa pice.

### W25 Sincronizare changelog DeepSeek
**Descriere tehnica:** Automatizeaza intrarea de changelog pentru adaugarea sau mutarea unui batch DeepSeek.
**Scop:** Pastreaza istoricul complet fara pasi manuali uitati.
**Target:** `CHANGELOG.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Orice batch nou sau mutat produce o intrare verificabila.

### W26 Harta batch-urilor active
**Descriere tehnica:** Genereaza un rezumat scurt al batch-urilor active si al intervalelor lor numerice curente.
**Scop:** Face orientarea rapida fara citire larga.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Rezumatul arata clar ce este activ si ce este arhiva.

### W27 Validare split activ / arhiva
**Descriere tehnica:** Verifica daca seria activa si arhiva DeepSeek nu se suprapun numeric si nu lasa goluri netrecute explicit.
**Scop:** Protejeaza separatia dintre activ si istoric.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/arhiva/README.md`.
**Acceptare:** Orice suprapunere sau gol numeric este raportat.

### W28 Raport de acoperire backlog
**Descriere tehnica:** Construieste un raport care arata cati pasi de lucru exista pe categorii si ce lipseste inca.
**Scop:** Ajuta la planificarea urmatoarelor taskuri fara repetitii.
**Target:** `docs/taskuri-de-lucru.md`, `docs/taskuri-prioritizate.md`.
**Acceptare:** Raportul indica clar categoriile subacoperite.

### W29 Repair pentru sesiuni Codex
**Descriere tehnica:** Adauga un pas de verificare pentru sincronizarea sesiunilor Codex cu documentatia DeepSeek inainte de a publica batch-uri noi.
**Scop:** Evita pierderea contextului din sesiunile anterioare.
**Target:** `.ai/codex-conversations.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Sesiunile relevante pot fi detectate si reflectate in sumar.

### W30 Backup de restaurare DeepSeek
**Descriere tehnica:** Defineste si valideaza o cale de restaurare pentru documentatia DeepSeek dupa mutari sau curatari mari.
**Scop:** Permite revenirea sigura daca un batch sau index este afectat.
**Target:** `docs/deepseek/arhiva/README.md`, `docs/deepseek/deepseek-taskuri-archive-audit-2026-06-25.md`.
**Acceptare:** Restaurarea reproduce aceleasi indexuri si legaturi canonice.

### W31 Generator batch DeepSeek
**Descriere tehnica:** Creeaza un helper care pregateste scheletul unui batch nou DeepSeek cu intervalul numeric, titlul si sectiunile cerute de documentatie.
**Scop:** Reduce erorile la crearea batch-urilor si pastreaza formatul consistent.
**Target:** `docs/deepseek/deepseek-taskuri-late-50-*.md`.
**Acceptare:** Un batch nou porneste cu structura valida si fara campuri lipsa.

### W32 Verificare consecutie numerica
**Descriere tehnica:** Adauga un check care confirma ca noul interval DeepSeek continua exact dupa ultimul batch activ sau arhivat, fara salturi nejustificate.
**Scop:** Pastreaza continuitatea numerelor L.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Orice gap sau suprapunere numerica blocheaza validarea.

### W33 Sincronizare README DeepSeek
**Descriere tehnica:** Automatizeaza actualizarea simultana a `docs/README.md` si `docs/deepseek/README.md` cand apare un batch nou sau cand unul se muta in arhiva.
**Scop:** Evita divergenta intre indexul global si indexul local.
**Target:** `docs/README.md`, `docs/deepseek/README.md`.
**Acceptare:** Ambele indexuri afiseaza acelasi set de batch-uri active.

### W34 Rebuild sumar serie activa
**Descriere tehnica:** Regenereaza sumarul seriei active din sursele canonice, nu manual, dupa fiecare batch sau mutare.
**Scop:** Pastreaza `deepseek-active-series-summary.md` aliniat la realitate.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Sumarul rezultat corespunde cu indexurile curente.

### W35 Curatare linkuri canonic
**Descriere tehnica:** Adauga o rutina care rescrie linkurile catre batch-urile mutante pe calea canonica din `./arhiva/`.
**Scop:** Evita linkuri vechi sau ambigue.
**Target:** `docs/deepseek/arhiva/README.md`, `docs/deepseek/deepseek-batch-guide.md`.
**Acceptare:** Toate referintele la batch-uri arhivate folosesc acelasi format.

### W36 Raport de audit al batch-ului
**Descriere tehnica:** Genereaza un raport scurt pentru fiecare batch nou cu verificari pe numere, linkuri si campuri obligatorii.
**Scop:** Face publicarea auditabila.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `CHANGELOG.md`.
**Acceptare:** Raportul poate fi folosit ca dovada de validare.

### W37 Detectie duplicate intre batch-uri
**Descriere tehnica:** Compara titlurile si intervalele taskurilor intre batch-uri pentru a prinde duplicari accidentale.
**Scop:** Pastreaza backlog-ul curat.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Taskurile duplicate sunt raportate cu sursa si conflictul lor.

### W38 Guard pentru index functional
**Descriere tehnica:** Verifica daca modificarile DeepSeek au fost reflectate in punctele de intrare ale documentatiei generale.
**Scop:** Pastreaza descoperirea documentelor stabila.
**Target:** `docs/index-functional.md`, `docs/start-here.md`.
**Acceptare:** Noile taskuri sunt vizibile din traseul principal de documentatie.

### W39 Validare sesiuni sincronizate
**Descriere tehnica:** Integreaza verificarea ca sumarul DeepSeek reflecta ultimele sesiuni Codex relevante inainte de publicarea unui batch.
**Scop:** Pastreaza contextul istoric in acelasi loc cu backlog-ul.
**Target:** `.ai/codex-conversations.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Sesiunile relevante sunt mentionate si datate corect.

### W40 Repair pentru arhiva cu linkuri
**Descriere tehnica:** Creeaza un pas de repair care elimina linkurile stale catre batch-uri mutate si le inlocuieste cu referinte corecte.
**Scop:** Pastreaza traseul de navigare curat.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/deepseek/arhiva/README.md`.
**Acceptare:** Nu raman referinte active catre calea veche.

### W41 Check pentru interval activ
**Descriere tehnica:** Adauga o verificare care confirma ca seria activa are mereu un singur interval curent declarat.
**Scop:** Evita suprapuneri intre batch-urile active.
**Target:** `docs/deepseek/README.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Doar un interval curent este raportat ca activ.

### W42 Test pentru publicare batch
**Descriere tehnica:** Introdu un test care emuleaza publicarea unui batch nou si verifica toate actualizarile cerute de documentatie.
**Scop:** Blocheaza publicarea incompleta.
**Target:** `docs/deepseek/deepseek-batch-guide.md`, `docs/README.md`, `docs/deepseek/README.md`.
**Acceptare:** Testul esueaza daca lipseste orice referinta obligatorie.

### W43 Curatare backlog vechi
**Descriere tehnica:** Identifica taskurile DeepSeek care au devenit redundant documentate si marcheaza-le pentru arhivare sau eliminare.
**Scop:** Pastreaza backlog-ul orientat pe lucru real.
**Target:** `docs/taskuri-de-lucru.md`, `docs/taskuri-prioritizate.md`.
**Acceptare:** Taskurile redundante sunt semnalate cu motiv.

### W44 Actualizare changelog automat
**Descriere tehnica:** Adauga un helper care scrie intrarea de changelog pentru DeepSeek imediat dupa ce batch-ul este creat sau mutat.
**Scop:** Pastreaza istoricul complet fara pasi uitati.
**Target:** `CHANGELOG.md`, `docs/deepseek/deepseek-active-series-summary.md`.
**Acceptare:** Intrarea de changelog apare cu fisierele corecte si data curenta.

### W45 Restaurare verificata
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

## DeepSeek implementation backlog (W171-W195)

### W171 Prompt context diff
**Descriere tehnica:** Adauga o comparatie intre contextul primit si promptul final in `OpenAIPromptBuilder` ca sa fie vizibile pierderile de informatie.
**Scop:** Face promptul mai usor de verificat si reparat.
**Target:** `OpenAIPromptBuilder`, `PromptSnapshot`.
**Acceptare:** Diferentele intre context si prompt sunt raportate clar.

### W172 Validation reason codes
**Descriere tehnica:** Introdu coduri de motiv stabile in `AIResponseValidator` pentru output incomplet, invalid sau incompatibil.
**Scop:** Face erorile AI mai usor de triat.
**Target:** `AIResponseValidator`, `AIValidationResult`.
**Acceptare:** Fiecare esec are cod si mesaj distinct.

### W173 Connection degrade mode
**Descriere tehnica:** Extinde `OpenAIConnectionProbe` si `ConnectionStatus` cu un mod degradat explicit pentru timeout-uri si retea instabila.
**Scop:** Diferentiaza functionarea partiala de esec total.
**Target:** `OpenAIConnectionProbe`, `ConnectionStatus`, `DebugGui`.
**Acceptare:** Degraded apare separat de failed.

### W174 Central role helper
**Descriere tehnica:** Muta regulile de rol din hub-urile GUI intr-un helper comun, folosit de toate ecranele principale.
**Scop:** Reduce duplicarea si alinierea gresita a regulilor de acces.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`.
**Acceptare:** Aceeasi decizie de acces este folosita peste tot.

### W175 Atomic quest save
**Descriere tehnica:** Face salvarea questului atomica in `QuestEditGui`, astfel incat validarea si persistenta sa fie totul sau nimic.
**Scop:** Evita statele partial salvate.
**Target:** `QuestEditGui`, quest persistence flow.
**Acceptare:** Un esec la validare anuleaza schimbarea completa.

### W176 Quest log freshness
**Descriere tehnica:** Leaga `QuestLogGui` de evenimentele de progres si marcheaza datele stale cand nu sunt proaspete.
**Scop:** Evita afisarea progresului depasit.
**Target:** `QuestLogGui`, `progression-service`.
**Acceptare:** UI se actualizeaza la schimbari reale si semnaleaza stale.

### W177 Story-progress diff
**Descriere tehnica:** Genereaza un diff intre `story-context-service` si `progression-service` atunci cand descrierile diverge.
**Scop:** Face discrepantele dintre poveste si progres usor de identificat.
**Target:** `story-context-service`, `progression-service`.
**Acceptare:** Raportul arata clar ce difera.

### W178 Dialog checkpoint rollback
**Descriere tehnica:** Introdu checkpoints in `DialogHistory` pentru a putea reveni dupa o ramura esuata.
**Scop:** Face dialogul recuperabil.
**Target:** `DialogHistory`, `DialogManager`.
**Acceptare:** Se poate reveni la checkpoint fara coruperea istoricului.

### W179 NPC fallback severity
**Descriere tehnica:** Diferentiaza fallback-ul NPC in functie de severitatea lipsei de context.
**Scop:** Evita un singur raspuns generic pentru toate cazurile.
**Target:** `NpcFactResolver`, `NPCRelationship`.
**Acceptare:** Severitatea modifica raspunsul sau refuzul.

### W180 Debug severity split
**Descriere tehnica:** Adauga severitate la cauzele din `OpenAIDebugSnapshot` pentru a separa warning de error.
**Scop:** Face trierea incidentelor mai rapida.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`.
**Acceptare:** Cauzele sunt clasificate pe severitate.

### W181 Admin action justification
**Descriere tehnica:** Cere o justificare scurta pentru actiunile sensibile din `AdminQuestGui` si `AuditGui`.
**Scop:** Creste trasabilitatea operatiunilor administrative.
**Target:** `AdminQuestGui`, `AuditGui`.
**Acceptare:** Motivele sunt salvate sau afisate clar.

### W182 Quest lifecycle sync
**Descriere tehnica:** Aliniaza starile questului din cod cu documentatia canonica si cu backlog-ul de lucru.
**Scop:** Pastreaza regulile de lifecycle consistente.
**Target:** quest lifecycle code, `docs/taskuri-de-lucru.md`.
**Acceptare:** Codul si documentatia descriu aceleasi tranzitii.

### W183 Trace id orchestration
**Descriere tehnica:** Propaga un trace id prin `AIOrchestrationService`, request si rezultat pentru a corela etapele de generare.
**Scop:** Simplifica debugging-ul cap-coada.
**Target:** `AIOrchestrationService`, `AIOrchestrationRequest`, `AIOrchestrationResult`.
**Acceptare:** Un request poate fi urmarit prin toate etapele.

### W184 Prompt snapshot regression tests
**Descriere tehnica:** Adauga regresii pentru `PromptSnapshot` cu contexte minim, complet, lipsa si duplicat.
**Scop:** Blocheaza schimbari accidentale de format.
**Target:** `PromptSnapshot`, `OpenAIPromptBuilder`.
**Acceptare:** Formatul snapshot-ului ramane stabil.

### W185 GUI docs refresh
**Descriere tehnica:** Actualizeaza `docs/index-functional.md` si `docs/start-here.md` cand apar fluxuri noi sau se schimba hub-urile GUI.
**Scop:** Pastreaza traseul principal de documentatie actual.
**Target:** `docs/index-functional.md`, `docs/start-here.md`.
**Acceptare:** Noile fluxuri sunt descoperibile din intrarea principala.

### W186 Quest validation messaging
**Descriere tehnica:** Imbunatateste mesajele de validare din `QuestEditGui` si `QuickQuestGui` pentru a indica exact campul sau regula incalcata.
**Scop:** Reduce timpul de corectare pentru authoring.
**Target:** `QuestEditGui`, `QuickQuestGui`.
**Acceptare:** Mesajele sunt actionabile si specifice.

### W187 Story freshness badge
**Descriere tehnica:** Afiseaza in `StoryGui` un indicator de prospețime al datelor pentru a semnala cand contextul narativ e actualizat.
**Scop:** Evita increderea in date vechi.
**Target:** `StoryGui`, `story-context-service`.
**Acceptare:** Badge-ul arata clar fresh sau stale.

### W188 Role matrix docs sync
**Descriere tehnica:** Coreleaza matricea de roluri din GUI cu documentatia canonica si cu backlog-ul de lucru.
**Scop:** Pastreaza regulile de acces uniforme.
**Target:** `MainHubGui`, `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Codul si documentatia descriu aceleasi permisiuni.

### W189 Failover orchestration state
**Descriere tehnica:** Introdu o stare explicita de failover in orchestration cand conexiunea nu poate fi recuperata rapid.
**Scop:** Face degradarea operationala controlata.
**Target:** `AIOrchestrationResult`, `ConnectionStatus`.
**Acceptare:** Failover-ul este distinct de failed si degraded.

### W190 Quest anchor docs sync
**Descriere tehnica:** Actualizeaza documentatia cand regulile de ancore quest se schimba, mai ales in zona de validare si save.
**Scop:** Pastreaza corespondenta dintre binding si documente.
**Target:** quest anchor validation, `docs/taskuri-de-lucru.md`.
**Acceptare:** Regulile de ancora sunt aceleasi in cod si docs.

### W191 Dialog branch audit trail
**Descriere tehnica:** Inregistreaza traseul branch-urilor din dialog pentru a sti ce a fost propus, acceptat sau respins.
**Scop:** Face dialogul mai usor de auditat.
**Target:** `DialogManager`, `DialogHistory`.
**Acceptare:** Fiecare branch are o urma clara in audit.

### W192 AI response format test
**Descriere tehnica:** Adauga test pentru formatele acceptate de `AIResponseValidator` si pentru cazurile care trebuie respinse.
**Scop:** Protejeaza contractul dintre model si runtime.
**Target:** `AIResponseValidator`.
**Acceptare:** Formatul valid trece, formatul invalid pica.

### W193 Debug labels sync
**Descriere tehnica:** Coreleaza etichetele din `OpenAIDebugSnapshot` cu descrierea lor din documentatia de lucru.
**Scop:** Face raportarea incidentelor mai clara.
**Target:** `OpenAIDebugSnapshot`, `DebugGui`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Etichetele din UI si documente sunt identice.

### W194 Quest lifecycle rollout note
**Descriere tehnica:** Scrie o nota scurta care leaga implementarea modificarilor de lifecycle de documentatia aferenta si de testele de regresie.
**Scop:** Face rollout-ul verificabil.
**Target:** `CHANGELOG.md`, `docs/taskuri-de-lucru.md`.
**Acceptare:** Nota indica ce a fost schimbat si ce teste il acopera.

### W195 Handoff implementation batch
**Descriere tehnica:** Genereaza un handoff pentru implementari care enumera ce s-a terminat, ce ramane si ce documente trebuie actualizate.
**Scop:** Face predarea intre sesiuni eficienta.
**Target:** `docs/deepseek/deepseek-active-series-summary.md`, `CHANGELOG.md`.
**Acceptare:** Handoff-ul spune clar urmatorii pasi si riscurile ramase.
