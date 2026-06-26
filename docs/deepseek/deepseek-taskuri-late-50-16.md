# DeepSeek Taskuri - Batch 18 (L901-L950)

Actualizat: 2026-06-25

Acest document marcheaza revenirea de la audit de documentatie la implementare cod. Taskurile selectate sunt extrase din arhiva (L031-L200) si actualizate pentru starea curenta a proiectului (Kotlin, Gradle, feature flags).

Status: draft incomplet. Nu executa automat acest batch pana cand intervalul L916-L950 este expandat in taskuri individuale cu campurile obligatorii.

Reguli:
- fiecare task produce o schimbare mica, verificabila in cod
- daca atinge runtime, adauga test
- nu introduce mecanici noi fara validare

## Status implementare preexistenta

Multe taskuri din arhiva (L031-L200) sunt deja rezolvate in codul curent:
- L031: ObjectiveTypeAliasRegistry ✓
- L032-L036: Audit + teste regresie ✓
- L047-L055: Debug dump + UI + compatibilitate ✓
- L071-L080: Normalizare + validare ✓
- L151-L158: Scope registry + teste ✓
- L161-L162: Debounce ✓

Taskurile de mai jos sunt cele ramase de implementat.

---

## Persistence si save (prioritate critica)

### L901 Quest progress persistence - save path
**Descriere tehnica:** Implementeaza metodele stub din ScenarioEngine: persistQuestProgress(), persistQuestProgressAsync(), persistQuestTrackingPreferenceAsync(), deleteQuestProgress().
**Scop:** Quest progress supravietuieste restart-ului de server.
**Target:** `ScenarioEngine.kt`, `DatabaseManager.kt` (tabela player_quests exista deja).
**Prompt AI:** Implementeaza salvarea progresului de quest in tabela player_quests (INSERT OR REPLACE) si stergerea la abandon/complete.
**Acceptare:** Progress-ul e salvat in DB si poate fi citit inapoi.

### L902 Atomic save wrapper
**Descriere tehnica:** Adauga o metoda care face save atomic (transactional) pentru progres: scrie progresul intr-o singura tranzactie DB.
**Scop:** Evita stari partiale la crash in timpul salvarii.
**Target:** DatabaseManager.kt, ScenarioEngine.kt.
**Prompt AI:** Adauga o metoda executeTransaction() in DatabaseManager si foloseste-o pentru salvarea progresului.
**Acceptare:** Un save intrerupt nu lasa date partiale.

### L903 Quest progress resume after restart
**Descriere tehnica:** Incarca quest progress-ul din DB la startup si restauraza starile active.
**Scop:** Questurile active continua dupa restart.
**Target:** ScenarioEngine.kt, SchedulerCoordinator.kt.
**Prompt AI:** Adauga loadPlayerQuests() care citeste din player_quests si populeaza activePlayerQuests.
**Acceptare:** Dupa restart, questurile active isi pastreaza stadiul.

### L904 Cleanup pentru obiective orfane
**Descriere tehnica:** Detecteaza si curata obiectivele care nu mai au owner valid (player offline > N zile, template sters).
**Scop:** Evita scurgeri de state.
**Target:** ScenarioEngine.kt, cleanup routine.
**Prompt AI:** Adauga un cleanup periodic care elimina progresul pentru playeri fara template valid.
**Acceptare:** Nu raman obiective blocate dupa stergere template.

---

## Admin commands

### L905 Admin quest summary command
**Descriere tehnica:** Adauga /ainpc quest summary [player] care afiseaza numarul de questuri active, completate, esuate.
**Scop:** Vizibilitate operationala rapida.
**Target:** AINPCCommandQuest.kt.
**Acceptare:** Comanda afiseaza counts per status per player.

### L906 Warnings report command
**Descriere tehnica:** Adauga /ainpc warnings cu sumar consolidat de warnings pe quest.
**Scop:** Simplifica diagnosticarea problemelor de configurare.
**Target:** AINPCCommand.kt.
**Acceptare:** Output-ul grupeaza warnings pe fisier si tip.

---

## Debug dumps

### L907 Debug dump pentru arbore de objective
**Descriere tehnica:** Extinde debug dump-ul cu un JSON care contine ierarhia completa de obiective per stage.
**Scop:** Diagnostica rapida a structurii de quest.
**Target:** DebugDumpService.kt.
**Acceptare:** Fisierul exportat arata clar relatia stage -> objective.

### L908 Snapshot stare quest activ
**Descriere tehnica:** Adauga o comanda care captureaza starea curenta a unui quest pentru un player.
**Scop:** Debug reproductibil.
**Target:** AINPCCommandQuest.kt.
**Acceptare:** Snapshot-ul include quest, stage, obiective si progress.

### L909 Filtru debug per player
**Descriere tehnica:** Adauga un parametru de filtru pe player la debug dump.
**Scop:** Reduce zgomotul pe servere cu multi jucatori.
**Target:** DebugDumpService.kt.
**Acceptare:** Dump-ul poate fi limitat la un singur player.

---

## Metrici si telemetrie

### L910 Metrici simple pentru progres
**Descriere tehnica:** Expune numarul de questuri active, completate si esuate prin /ainpc overview.
**Scop:** Vizibilitate operationala.
**Target:** AINPCCommandMisc.kt.
**Acceptare:** Metricile apar in overview.

### L911 Telemetrie minima pentru interactiuni NPC
**Descriere tehnica:** Adauga contor de interactiuni si rezultate per NPC in debug dump.
**Scop:** Debugging fara logging excesiv.
**Target:** DebugDumpService.kt, NPCManager.kt.
**Acceptare:** Contorul e vizibil in dump fara a afecta gameplay.

---

## Migrare si backup

### L912 Backup configuratii quest
**Descriere tehnica:** Adauga /ainpc quest backup care copiaza quests.yml in backup cu timestamp.
**Scop:** Reduce riscul pierderii configuratiei.
**Target:** AINPCCommandQuest.kt.
**Acceptare:** Backup-ul e restaurabil.

### L913 Tool de diff pentru questuri
**Descriere tehnica:** Adauga un diff text intre doua versiuni ale aceluiasi quest intre YAML si runtime.
**Scop:** Simplifica review-ul modificarilor.
**Target:** AINPCCommandQuest.kt.
**Acceptare:** Diferentele apar clar.

---

## Contract si documentatie

### L914 Documentatie de schema cu exemple minime
**Descriere tehnica:** Adauga un ghid de schema in docs/ cu exemple minimale si complete.
**Scop:** Face contractul usor de urmat.
**Target:** docs/json-yaml-contract-exemple.md.
**Acceptare:** Documentatia arata clar obligatoriu vs optional.

### L915 Changelog pentru extinderea contractului
**Descriere tehnica:** Noteaza schimbarile de schema in ../../CHANGELOG.md.
**Scop:** Istoric al deciziilor.
**Target:** ../../CHANGELOG.md.
**Acceptare:** Fiecare schimbare de contract are motiv tehnic.

---

## Tests

## Rezervare L916-L950 - teste de regresie pentru persistence, atomicity, resume

Interval rezervat, nu task executabil. Expandarea trebuie facuta ulterior in taskuri individuale `### L916`, `### L917` etc., fiecare cu campurile obligatorii.

(testele vor fi adaugate incremental pe masura ce taskurile de mai sus sunt implementate)



