# DeepSeek Batch Guide — Standards & Process

Actualizat: 2026-06-24

Acest document unifica regulile pentru toate batch-urile DeepSeek (L001-L400+). 
Înlocuiește regulile duplicate din fișiere individuale de batch.

## Structura unui task

Fiecare task trebuie să aibă:

| Câmp | Obligatoriu | Descriere |
|------|:-----------:|-----------|
| `Prompt AI` | da | Instrucțiunea executabilă de model |
| `Target` | da | Zona tehnică afectată (ex: parser, runtime, docs) |
| `Scop` | da | Rezultatul urmărit, verificabil |
| `Acceptare` | da | Criterii clare, măsurabile |
| `Descriere tehnica` | da | Context tehnic și abordare |

## Reguli de format

- Prefix: `L` + număr secvențial pe 3 cifre (L001, L042, L150)
- Batch-urile sunt consecutive, fără gap-uri
- Maximum **50 taskuri per batch**
- Fiecare task = o singură schimbare mică

## Categorii standard

| Categorie | Prefix | Exemple |
|-----------|--------|---------|
| Contract, schema | L15x | scope registry, warnings |
| Runtime, progres | L16x | debounce, atomic save |
| Admin, diagnostic | L17x | snapshot, filter, backup |
| Persistenta, migrare | L18x | backup, diff, version |
| Teste, hardening | L19x | unit tests, integration |
| Prompt, orchestrare | L20x | templates, fallback |
| Debug, audit | L21x | duplicate detection |
| Infrastructura docs | L25x | indexing, archive policy |
| Control de calitate | L30x | guards, lint |
| Audit, guvernare | L35x | rule registry, governance |

## Reguli de validare (guard-uri)

1. **Fără `Prompt AI`** → task respins
2. **Fără `Target`** → task respins
3. **Fără `Scop`** → task respins
4. **Acceptare neclară** → task marcat pentru rescriere
5. **Task prea mare** (>1 zonă) → trebuie împărțit
6. **Redundanță** → referință la batch-ul anterior
7. **Contradicție** → raportată cu referință la ambele locații

## Fallback pattern pentru răspunsuri incomplete

Când AI produce un răspuns parțial:
1. Marchează taskul ca `PARTIAL` în audit
2. Returnează ce s-a putut genera
3. Loghează motivul întreruperii
4. Nu bloca pipeline-ul — continuă cu taskul următor

## Model selection

| Tip task | Model recomandat | Token budget |
|----------|-----------------|:------------:|
| Cod: schimbare mică | DeepSeek v4 Flash | 900/8 |
| Cod: cross-modul | DeepSeek v4 Flash | 1400/12 |
| Documentație | DeepSeek v4 Flash | 600/6 |
| Audit / read-only | DeepSeek v4 Flash | 900/8 |
| Planificare | DeepSeek v4 Flash | 2200/14 |

## Index batch-uri

| Batch | Fișier | Taskuri | Tema principală |
|-------|--------|:-------:|-----------------|
| 1 | `deepseek-taskuri-late-25.md` | L001-L030 | Fundație, bază |
| 2 | `deepseek-taskuri-late-25.md` | L031-L150 | Quest, runtime, NPC |
| 3 | `deepseek-taskuri-late-50.md` | L151-L200 | Contract, admin, persist |
| 4 | `deepseek-taskuri-late-50-2.md` | L201-L250 | Prompt, orchestrare |
| 5 | `deepseek-taskuri-late-50-3.md` | L251-L300 | Docs infrastructure |
| 6 | `deepseek-taskuri-late-50-4.md` | L301-L350 | Quality control |
| 7 | `deepseek-taskuri-late-50-5.md` | L351-L400 | Audit, governance |

## Politica de arhivare

Un batch poate fi arhivat când:
- Toate taskurile sunt marcate `DONE` sau `CANCELLED`
- A trecut cel puțin o lună de la ultima modificare
- Există un batch ulterior care acoperă aceeași zonă

## Etichete standard

| Eticheta | Folosită pentru |
|----------|----------------|
| `code:runtime` | Schimbări în gameplay runtime |
| `code:parser` | YAML/JSON parsing |
| `code:gui` | Inventare și ecrane |
| `code:api` | ainpc-api |
| `docs:canonic` | Documentație canonică |
| `docs:process` | Reguli de proces |
| `test:unit` | Teste unitare |
| `test:integration` | Teste de integrare |
| `meta:task` | Meta-taskuri despre taskuri |
