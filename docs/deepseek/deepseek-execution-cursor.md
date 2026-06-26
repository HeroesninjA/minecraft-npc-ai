# DeepSeek Execution Cursor

Actualizat: 2026-06-25

Status: document activ de control.

Acest document este sursa de adevar pentru ordinea de executie DeepSeek. Scopul lui este sa opreasca saritul peste taskuri, alegerea arbitrara a unui task mai nou si continuarea dintr-un interval gresit.

## Cursor curent

- Current task: `L455`
- Policy: strict ascending
- Active source: `./deepseek-taskuri-late-50-7.md` si urmatoarele batch-uri active
- Checker: `../../scripts/deepseek-next-task.ps1`
- Ledger: `./deepseek-execution-ledger.json`

Regula: pana cand un status audit marcheaza explicit taskurile anterioare ca `DONE` sau `CANCELLED`, cursorul ramane la cel mai mic task activ neinchis.

## Reguli de executie

1. DeepSeek executa doar taskul indicat de cursor sau de checker.
2. Dupa executie, taskul primeste `**Status:** DONE` si dovada minima de verificare.
3. Daca taskul nu poate fi facut, primeste `**Status:** BLOCKED` si motivul blocajului.
4. Daca taskul este invalid sau nu mai merita facut, primeste `**Status:** CANCELLED` si motivul.
5. Cursorul avanseaza numai la urmatorul numar dupa ce taskul curent este `DONE` sau `CANCELLED`.
6. Un task `BLOCKED`, `PARTIAL` sau `NEEDS_REVIEW` opreste ordinea; nu se sare peste el fara decizie umana.
7. Batch-urile marcate `Status: draft incomplet` nu sunt executabile.

## Cum se foloseste

Ruleaza:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deepseek-next-task.ps1 -ProjectRoot "."
```

Sau, daca esti deja intr-o consola PowerShell:

```powershell
& .\scripts\deepseek-next-task.ps1 -ProjectRoot "."
```

Rezultatul acceptabil pentru executie este `status: ready`. Daca rezultatul este `blocked`, problema raportata trebuie rezolvata inainte de a continua.

## Cum se inchide taskul curent

Dupa ce taskul returnat de checker a fost executat si verificat, marcheaza-l in ledger:

```powershell
& .\scripts\deepseek-next-task.ps1 -ProjectRoot "." -Mark L401 -MarkStatus DONE -Evidence "docs/check sau test relevant"
```

Statusuri permise:

- `DONE`: task executat si verificat;
- `CANCELLED`: task invalid sau inlocuit;
- `BLOCKED`: task blocat si nu poate fi sarit fara decizie umana;
- `PARTIAL`: task inceput, dar incomplet;
- `NEEDS_REVIEW`: cere decizie umana inainte de executie.

Regula: `-Mark` accepta doar taskul curent returnat de checker. Daca incerci sa marchezi un task mai mare, scriptul refuza operatia.

## Instrumente conexe

- `scripts/deepseek-validate-index.ps1` — verifica consistenta intre indexuri
- `scripts/deepseek-atomic-update.ps1` — update atomic cursor + ledger cu backup
- `scripts/deepseek-batch-report.ps1` — genereaza raport standard pentru batch nou
- `scripts/deepseek-stale-refs.ps1` — detecteaza referinte invechite

## Blocaje cunoscute

- `deepseek-taskuri-late-50-16.md` pana la `deepseek-taskuri-late-50-21.md` sunt drafturi incomplete.
- Intervalele rezervate `L916-L950`, `L961-L1000`, `L1004-L1050`, `L1061-L1100`, `L1106-L1150`, `L1158-L1200` trebuie expandate in taskuri individuale inainte de executie automata.
- Rezervarile se scriu ca heading-uri non-task `## Rezervare Lx-Ly`, nu ca `### Lx-Ly`.



























































