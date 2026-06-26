# DeepSeek Taskuri - Batch 19 (L951-L1000)

Actualizat: 2026-06-25

Acest document extinde seria DeepSeek dupa implementarea tuturor taskurilor de cod din L001-L200 si acoperirea de audit din L851-L900.

Status: draft incomplet. Nu executa automat acest batch pana cand intervalul L961-L1000 este expandat in taskuri individuale cu campurile obligatorii.

---

### L951 Tab-complete pentru comenzi noi
**Descriere:** Adauga tab-completion pentru /ainpc quest summary, /ainpc quest warnings, /ainpc quest diff, /ainpc quest metrics.
**Target:** AINPCTabCompleter.kt
**Acceptare:** Tab-completion sugereaza noile subcomenzi.

### L952 Adauga quest metrics si summary in tab-complete
**Descriere:** Extinde tab-complete pentru /ainpc cu "warnings" ca subcomanda.
**Target:** AINPCTabCompleter.kt
**Acceptare:** /ainpc warnings apare in tab-complete.

### L953 Debugdump pentru scenarii active
**Descriere:** Adauga /ainpc debugdump scenario care exporta starea scenariilor active.
**Target:** DebugDumpService.kt, AINPCCommand.kt
**Acceptare:** Dump-ul contine templateId, player UUID, faza curenta, actorii spawnati si warning-uri.

### L954 Tab-complete pentru debugdump scenario
**Descriere:** Adauga "scenario" in lista de scope-uri disponibile pentru debugdump.
**Target:** AINPCTabCompleter.kt
**Acceptare:** /ainpc debugdump scenario apare in tab-complete si functioneaza.

### L955 Test pentru import_objectives_from in YAML
**Descriere:** Test care verifica ca import_objectives_from copie objectivele dintr-un alt scenariu.
**Target:** FeaturePackYamlSupportActorTest.kt sau test dedicat
**Acceptare:** Testul verifica ca obiectivele importate corespund cu cele din sursa.

### L956 Test pentru fallback-uri de campuri optionale in quest YAML
**Descriere:** Test care verifica default-urile pentru questCategory, completion_mode, tracking_mode, acceptance_mode.
**Target:** QuestYamlFallbackTest.kt
**Acceptare:** Campurile optionale au valori implicite sane cand lipsesc.

### L957 Test pentru cleanupStaleTemplateProgress
**Descriere:** Test care verifica ca progresul pentru template-uri sterse este curatat.
**Target:** QuestEngineRegressionTest.kt
**Acceptare:** Dupa stergerea unui template, progresul asociat dispare.

### L958 Adauga fail-reasons in /ainpc quest status output
**Descriere:** Cand un quest e esuat, afiseaza motivele colectate de collectFailureReasons().
**Target:** AINPCCommandQuest.kt
**Acceptare:** Output-ul de status include motive explicite.

### L959 Adauga /ainpc quest cache-clean command
**Descriere:** Comanda admin care forteaza cleanup-ul de cache stale.
**Target:** AINPCCommandQuest.kt
**Acceptare:** Curata progresul pentru template-uri sterse si tracking-ul orfan.

### L960 Extinde deepseek-batch-guide.md cu batch-ul 19
**Descriere:** Adauga randul 19 in indexul batch-urilor din ghid.
**Target:** ./deepseek-batch-guide.md
**Acceptare:** Batch 19 apare in index.

## Rezervare L961-L1000

Interval rezervat, nu task executabil. Expandarea trebuie facuta ulterior in taskuri individuale `### L961`, `### L962` etc., fiecare cu campurile obligatorii.

(Rezervat pentru extinderi viitoare, in functie de prioritati)

