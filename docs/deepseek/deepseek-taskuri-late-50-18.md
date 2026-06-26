# DeepSeek Taskuri - Batch 20 (L1001-L1050)

Actualizat: 2026-06-25

Acest document continua seria DeepSeek dupa implementarea Batch 19 (L951-L1000).

Status: draft incomplet. Nu executa automat acest batch pana cand intervalul L1004-L1050 este expandat in taskuri individuale cu campurile obligatorii.

---

### L1001 /ainpc quest metrics command
**Descriere:** Adauga comanda standalone /ainpc quest metrics care afiseaza numarul de questuri active, completate si esuate.
**Target:** AINPCCommandQuest.kt, AINPCCommand.kt
**Acceptare:** Comanda afiseaza aceleasi metrici ca /ainpc overview dar numai pentru questuri.

### L1002 Add failure reasons to quest status
**Descriere:** Cand un quest are status FAILED, afiseaza motivele colectate de collectFailureReasons() in output-ul de status.
**Target:** AINPCCommandQuest.kt, ScenarioEngine.kt
**Acceptare:** Statusul contine informatii de diagnostic pentru questuri esuate.

### L1003 Tests for cleanup + stale cache
**Descriere:** Teste pentru cleanupStaleTemplateProgress, isCacheStale, loadPlayerQuests.
**Target:** QuestEngineRegressionTest.kt
**Acceptare:** Testele verifica comportamentul de cleanup si staleness.

## Rezervare L1004-L1050

Interval rezervat, nu task executabil. Expandarea trebuie facuta ulterior in taskuri individuale `### L1004`, `### L1005` etc., fiecare cu campurile obligatorii.

(Rezervat)
