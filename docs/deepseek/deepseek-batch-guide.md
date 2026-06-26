# DeepSeek Batch Guide — Standards & Process

Actualizat: 2026-06-26

Acest document unifica regulile pentru toate batch-urile DeepSeek (L001-L400+). 
Înlocuiește regulile duplicate din fișiere individuale de batch.

## Structura unui task

Fiecare task trebuie să aibă:

- `Prompt AI` — da; instrucțiunea executabilă de model.
- `Target` — da; zona tehnică afectată, de exemplu parser, runtime sau docs.
- `Scop` — da; rezultatul urmărit, verificabil.
- `Acceptare` — da; criterii clare, măsurabile.
- `Descriere tehnica` — da; context tehnic și abordare.

## Reguli de format

- Prefix: `L` + număr secvențial pe 3 cifre (L001, L042, L150)
- Batch-urile sunt consecutive, fără gap-uri
- Maximum **50 taskuri per batch**
- Fiecare task = o singură schimbare mică

## Categorii standard

- `Contract, schema` — prefix `L15x`; exemple: scope registry, warnings.
- `Runtime, progres` — prefix `L16x`; exemple: debounce, atomic save.
- `Admin, diagnostic` — prefix `L17x`; exemple: snapshot, filter, backup.
- `Persistenta, migrare` — prefix `L18x`; exemple: backup, diff, version.
- `Teste, hardening` — prefix `L19x`; exemple: unit tests, integration.
- `Prompt, orchestrare` — prefix `L20x`; exemple: templates, fallback.
- `Debug, audit` — prefix `L21x`; exemple: duplicate detection.
- `Infrastructura docs` — prefix `L25x`; exemple: indexing, archive policy.
- `Control de calitate` — prefix `L30x`; exemple: guards, lint.
- `Audit, guvernare` — prefix `L35x`; exemple: rule registry, governance.

## Instrumente de validare

Urmatoarele scripturi ajuta la mentinerea consistentei:

- `scripts/deepseek-validate-index.ps1` — verifica ca toate batch-urile active sunt mentionate in toate indexurile
- `scripts/deepseek-atomic-update.ps1` — update atomic al cursorului si ledger-ului cu backup automat
- `scripts/deepseek-batch-report.ps1` — raport standard pentru fiecare batch nou
- `scripts/deepseek-stale-refs.ps1` — detecteaza linkuri invechite catre arhiva

## Reguli de validare (guard-uri)

1. **Fără `Prompt AI`** → task respins
2. **Fără `Target`** → task respins
3. **Fără `Scop`** → task respins
4. **Acceptare neclară** → task marcat pentru rescriere
5. **Task prea mare** (>1 zonă) → trebuie împărțit
6. **Redundanță** → referință la batch-ul anterior
7. **Contradicție** → raportată cu referință la ambele locații

## Ordine stricta de executie

Seria DeepSeek nu se executa prin alegerea libera a unui task din backlog. Ordinea este controlata de `deepseek-execution-cursor.md` si verificata cu `../../scripts/deepseek-next-task.ps1`.

Reguli:

1. Se executa doar cel mai mic task activ care nu este `DONE` sau `CANCELLED`.
2. Daca taskul curent este `BLOCKED`, `PARTIAL` sau `NEEDS_REVIEW`, executia se opreste si cere decizie umana.
3. Un batch cu `Status: draft incomplet` nu este eligibil pentru executie.
4. Rezervarile de forma `L916-L950` se scriu ca `## Rezervare L916-L950`, nu ca task `### L916-L950`.
5. Dupa fiecare task, se adauga `**Status:** DONE`, `**Status:** BLOCKED` sau `**Status:** CANCELLED` in sectiunea taskului.
6. Cursorul avanseaza doar dupa dovada minima de verificare.

Comanda recomandata:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deepseek-next-task.ps1 -ProjectRoot "."
```

Alternativ, dintr-o consola PowerShell deja deschisa:

```powershell
& .\scripts\deepseek-next-task.ps1 -ProjectRoot "."
```

Dupa executie, inchide doar taskul curent in ledger:

```powershell
& .\scripts\deepseek-next-task.ps1 -ProjectRoot "." -Mark L401 -MarkStatus DONE -Evidence "verificare"
```

Scriptul refuza marcarea unui task care nu este urmatorul eligibil.

## Verificare limita activ/arhiva

Seria activa trebuie sa inceapa imediat dupa ultimul task arhivat local. La starea curenta, arhiva locala se termina la `L450`, iar seria activa incepe cu `L451` in `deepseek-taskuri-late-50-7.md`.

`../../scripts/deepseek-next-task.ps1` verifica aceasta limita si raporteaza:

- `active_archive_boundary_overlap`, daca primul task activ se suprapune cu arhiva;
- `active_archive_boundary_gap`, daca intre arhiva si seria activa exista un gol numeric;
- `active_start_reference_missing`, daca indexul principal, ghidul sau sumarul activ nu mentioneaza clar taskul si fisierul de start.

## Fallback pattern pentru răspunsuri incomplete

Când AI produce un răspuns parțial:
1. Marchează taskul ca `PARTIAL` în audit
2. Returnează ce s-a putut genera
3. Loghează motivul întreruperii
4. Nu bloca pipeline-ul — continuă cu taskul următor

## Model selection

- Cod: schimbare mică — `DeepSeek v4 Flash`, `900/8`.
- Cod: cross-modul — `DeepSeek v4 Flash`, `1400/12`.
- Documentație — `DeepSeek v4 Flash`, `600/6`.
- Audit / read-only — `DeepSeek v4 Flash`, `900/8`.
- Planificare — `DeepSeek v4 Flash`, `2200/14`.

## Index batch-uri

- Batch 1 — sursa locală indisponibilă — `L001-L030` — Fundație, bază; referință istorică neverificată.
- Batch 2 — `./arhiva/deepseek-taskuri-late-25.md` — `L031-L150` — Quest, runtime, NPC.
- Batch 3 — `./arhiva/deepseek-taskuri-late-50.md` — `L151-L200` — Contract, admin, persist.
- Batch 4 — `./arhiva/deepseek-taskuri-late-50-2.md` — `L201-L250` — Prompt, orchestrare.
- Batch 5 — `./arhiva/deepseek-taskuri-late-50-3.md` — `L251-L300` — Docs infrastructure.
- Batch 6 — `./arhiva/deepseek-taskuri-late-50-4.md` — `L301-L350` — Quality control.
- Batch 7 — `./arhiva/deepseek-taskuri-late-50-5.md` — `L351-L400` — Audit, governance.
- Batch 8 — `./arhiva/deepseek-taskuri-late-50-6.md` — `L401-L450` — Arhivare, continuitate.
- Batch 9 — `deepseek-taskuri-late-50-7.md` — `L451-L500` — Automatizare, verificare.
- Batch 10 — `deepseek-taskuri-late-50-8.md` — `L501-L550` — Curatare, audit, stabilizare.
- Batch 11 — `deepseek-taskuri-late-50-9.md` — `L551-L600` — Audit, arhitectura, continuitate.
- Batch 12 — `deepseek-taskuri-late-50-10.md` — `L601-L650` — Control, audit, continuitate.
- Batch 13 — `deepseek-taskuri-late-50-11.md` — `L651-L700` — Tooling, rapoarte, executie controlata.
- Batch 14 — `deepseek-taskuri-late-50-12.md` — `L701-L750` — Executie, dovezi, prioritizare.
- Batch 15 — `deepseek-taskuri-late-50-13.md` — `L751-L800` — Implementare ghidata si validare practica.
- Batch 16 — `deepseek-taskuri-late-50-14.md` — `L801-L850` — Acoperire echilibrata pe documentatie.
- Batch 17 — `deepseek-taskuri-late-50-15.md` — `L851-L900` — Audit granular al documentatiei.
- Batch 18 — `deepseek-taskuri-late-50-16.md` — `L901-L950` — Revenire la cod: parser, runtime, teste.
- Batch 19 — `deepseek-taskuri-late-50-17.md` — `L951-L1000` — Refinements: tab-complete, debugdump, teste.
- Batch 20 — `deepseek-taskuri-late-50-18.md` — `L1001-L1050` — Metrics command, failure reasons, cleanup tests.
- Batch 21 — `deepseek-taskuri-late-50-19.md` — `L1051-L1100` — Arhitectural: runtime extensibil, reputatie, chain quests.
- Batch 22 — `deepseek-taskuri-late-50-20.md` — `L1101-L1150` — Migrare obiective la handler-e.
- Batch 23 — `deepseek-taskuri-late-50-21.md` — `L1151-L1200` — Completare handlere + teste.
- Batch 24 — `deepseek-taskuri-late-50-24.md` — `L1201-L1250` — Consolidare docs, guarduri, mentenanta.
- Batch 25 — `deepseek-taskuri-late-50-25.md` — `L1251-L1300` — Indexare, arhivare, audit si guarduri.
- Batch 26 — `deepseek-taskuri-late-50-26.md` — `L1301-L1350` — Harta, audit si automatizare.
- Batch 27 — `deepseek-taskuri-late-50-27.md` — `L1351-L1400` — Compatibilitate, integrare si audit.
- Batch 28 — `deepseek-taskuri-late-50-28.md` — `L1401-L1450` — Robustete operationala, backup si guarduri.
- Batch 29 — `deepseek-taskuri-late-50-29.md` — `L1451-L1500` — Operare, persistenta si consistenta.
- Batch 30 — `deepseek-taskuri-late-50-30.md` — `L1501-L1550` — Sinteza, control si audit.
- Batch 31 — `deepseek-taskuri-late-50-31.md` — `L1551-L1600` — Mentenanta, persistenta si conformitate.

## Politica de arhivare

Un batch poate fi arhivat când:
- Toate taskurile sunt marcate `DONE` sau `CANCELLED`
- A trecut cel puțin o lună de la ultima modificare
- Există un batch ulterior care acoperă aceeași zonă

Dupa mutarea in arhiva:
- pastreaza numele original al fisierului;
- actualizeaza `./arhiva/README.md` cu intervalul numeric si motivul mutarii;
- actualizeaza `../index-arhiva.md` daca apare o arhiva noua;
- actualizeaza `../README.md` ca batch-ul mutat sa nu mai apara ca activ;
- ruleaza `../../scripts/deepseek-next-task.ps1` si verifica sa nu raporteze duplicate sau referinte invechite.
- foloseste doar calea `./arhiva/<fisier>` in linkurile catre batch-uri arhivate; checker-ul marcheaza ca legacy orice referinta directa la calea veche.

Arhivarea poate fi facuta progresiv, pe loturi mici, daca fiecare batch mutat este deja inchis si ramane trasabil din index. Nu muta partial taskuri dintr-un batch fara o nota explicita de tranzitie.

Template mesaj arhivare:

```text
Arhivat <fisier> (<interval>) in <destinatie>.
Motiv: <DONE/CANCELLED/inlocuit de batch activ>.
Verificare: index actualizat, link arhiva valid, checker fara duplicate.
```

## Template de start batch

```text
# DeepSeek Taskuri - Batch <N> (L<start>-L<end>)

Actualizat: YYYY-MM-DD

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## <Tema principala>

### L<start> <titlu scurt>
**Descriere tehnica:** <context tehnic si abordare>
**Scop:** <rezultat urmarit, verificabil>
**Target:** <zona tehnica afectata>
**Prompt AI:** <instrucțiunea executabila>
**Acceptare:** <criterii clare, masurabile>
```

## Etichete standard

- `code:runtime` — schimbări în gameplay runtime.
- `code:parser` — YAML/JSON parsing.
- `code:gui` — inventare și ecrane.
- `code:api` — ainpc-api.
- `docs:canonic` — documentație canonică.
- `docs:process` — reguli de proces.
- `test:unit` — teste unitare.
- `test:integration` — teste de integrare.
- `meta:task` — meta-taskuri despre taskuri.




