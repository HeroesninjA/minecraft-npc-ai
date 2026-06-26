# DeepSeek Taskuri - Audit si Remediere

Actualizat: 2026-06-25

Acest document verifica daca seria de taskuri DeepSeek este suficient de buna pentru lucru curent sau trebuie arhivata/remediata.

## Verdict

Nu se arhiveaza batch-urile active acum.

Motiv: politica din `deepseek-batch-guide.md` cere arhivare doar cand taskurile sunt `DONE` sau `CANCELLED`, au vechime si exista acoperire ulterioara. Batch-urile active nu au status complet de executie si unele sunt drafturi incomplete.

Decizie:

- pastreaza arhiva existenta din `./arhiva/`;
- pastreaza active batch-urile bine formate `deepseek-taskuri-late-50-7.md` pana la `deepseek-taskuri-late-50-15.md` si `deepseek-taskuri-late-50-24.md`;
- remediaza `deepseek-taskuri-late-50-16.md` pana la `deepseek-taskuri-late-50-21.md` inainte de folosire operationala.

## Ce este bine facut

| Zona | Evaluare | Observatie |
|---|---|---|
| Arhiva DeepSeek | OK | Primele batch-uri sunt separate in `./arhiva/` si au README propriu. |
| Ghidul de batch-uri | OK | `deepseek-batch-guide.md` defineste structura, validarea si politica de arhivare. |
| Batch 8-17 | OK ca format | Fisierele `late-50-6` pana la `late-50-15` au cate 50 taskuri si campurile obligatorii. |
| Continuitate numerica | OK | Seria merge pana la L1250 in ghid, cu batch-ul 24 adaugat dupa continuitatea curenta. |

## Probleme gasite

| Fisier | Problema | Impact |
|---|---|---|
| `../README.md` | Lista rapida a fost extinsa pana la `deepseek-taskuri-late-50-24.md`. | Problema de vizibilitate este remediata. |
| `deepseek-taskuri-late-50-16.md` | Are 15 taskuri individuale si rezervarea non-task `L916-L950`. | Nu respecta standardul de batch complet pana la expandare. |
| `deepseek-taskuri-late-50-17.md` | Are 10 taskuri individuale si rezervarea non-task `L961-L1000`. Lipsesc `Prompt AI` si `Descriere tehnica` la taskurile existente. | Nu poate fi consumat consecvent de DeepSeek. |
| `deepseek-taskuri-late-50-18.md` | Are 3 taskuri individuale si rezervarea non-task `L1004-L1050`. | Batch incomplet; nu este pregatit pentru executie. |
| `deepseek-taskuri-late-50-19.md` | Are 10 taskuri individuale si rezervarea non-task `L1061-L1100`. | Batch arhitectural incomplet si insuficient verificabil. |
| `deepseek-taskuri-late-50-20.md` | Are 5 taskuri individuale si rezervarea non-task `L1106-L1150`. | Batch incomplet; nu respecta structura standard. |
| `deepseek-taskuri-late-50-21.md` | Are 7 taskuri individuale si rezervarea non-task `L1158-L1200`. | Batch incomplet; nu respecta structura standard. |

## Reguli constitutionale relevante

Seria este acceptabila doar daca ramane aliniata la aceste reguli:

- AI-ul propune drafturi si planuri validate, nu executa direct modificari fara control;
- schimbarile riscante au test, audit sau demonstratie;
- documentatia trebuie sa descrie starea reala;
- taskurile trebuie sa fie slice-uri mici, verificabile;
- arhivarea nu trebuie folosita pentru a ascunde backlog incomplet.

## Propunere de remediere

### R1 - Corecteaza indexul

Status: aplicat in `../README.md`.

Actiune:

- listeaza `deepseek-batch-guide.md`;
- listeaza acest audit;
- listeaza toate batch-urile active pana la `deepseek-taskuri-late-50-24.md`.

### R2 - Marcheaza batch-urile incomplete ca draft

Status: aplicat.

Actiune:

- in fiecare fisier `late-50-16` pana la `late-50-21`, adauga status clar: `Status: draft incomplet`;
- mentioneaza ca nu trebuie executate automat pana la expandare.

Acceptare:

- cititorul vede imediat ca batch-ul nu este operational complet.

### R3 - Expandeaza intervalele rezervate in taskuri individuale

Actiune:

- transforma `L916-L950`, `L961-L1000`, `L1004-L1050`, `L1061-L1100`, `L1106-L1150`, `L1158-L1200` in taskuri individuale;
- fiecare task trebuie sa aiba `Descriere tehnica`, `Scop`, `Target`, `Prompt AI`, `Acceptare`.

Acceptare:

- fiecare fisier are 50 taskuri individuale sau este redenumit explicit ca draft partial.

### R3b - Normalizeaza rezervarile ca non-task

Status: aplicat.

Actiune:

- inlocuieste heading-urile `### Lx-Ly` cu `## Rezervare Lx-Ly`;
- pastreaza textul de blocaj in fiecare batch draft;
- actualizeaza `../../scripts/deepseek-next-task.ps1` ca aceste rezervari sa fie acceptate doar in batch-uri `Status: draft incomplet`.

Acceptare:

- checker-ul nu mai raporteaza `range_heading` sau `numbering_gap` pentru rezervarile declarate.

### R4 - Normalizeaza campurile lipsa

Actiune:

- completeaza `Prompt AI` unde lipseste;
- completeaza `Descriere tehnica` unde lipseste;
- pastreaza `Target` si `Acceptare` concrete.

Acceptare:

- validatorul de campuri obligatorii raporteaza zero lipsuri pentru batch-urile active.

### R5 - Nu arhiva pana la status real

Actiune:

- nu muta `late-50-16` pana la `late-50-21` in `./arhiva/` in starea curenta;
- arhiveaza doar dupa ce taskurile sunt `DONE` sau `CANCELLED`, conform ghidului.

Acceptare:

- arhiva ramane istoric curat, iar backlog-ul incomplet ramane vizibil ca lucru de remediat.

### R6 - Blocheaza saritul peste taskuri

Status: aplicat.

Actiune:

- foloseste `deepseek-execution-cursor.md` ca sursa de adevar pentru taskul curent;
- foloseste `deepseek-execution-ledger.json` pentru statusurile persistente ale taskurilor executate;
- foloseste `../../scripts/deepseek-next-task.ps1` pentru a calcula urmatorul task eligibil;
- nu permite executia unui task mai mare pana cand cel mai mic task activ este `DONE` sau `CANCELLED`;
- opreste executia daca urmatorul task este `BLOCKED`, `PARTIAL`, `NEEDS_REVIEW` sau se afla intr-un batch `draft incomplet`.

Acceptare:

- DeepSeek primeste un singur task urmator, in ordine crescatoare, si nu mai poate sari arbitrar la un batch mai nou.

## Ordine recomandata

1. Repara indexul din `../README.md`.
2. Adauga status `draft incomplet` la batch-urile `-16` pana la `-21`.
3. Expandeaza `deepseek-taskuri-late-50-16.md` la 50 taskuri complete.
4. Repeta pentru `-17`, `-18`, `-19`, `-20`, `-21`.
5. Ruleaza auditul de campuri obligatorii si numerotare.
6. Abia dupa executie reala si status `DONE`/`CANCELLED`, muta batch-uri in arhiva.




