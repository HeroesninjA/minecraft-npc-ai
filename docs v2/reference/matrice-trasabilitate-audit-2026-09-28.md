# Matrice de trasabilitate a auditului

Scopul acestei matrice este să prevină pierderea sau duplicarea constatărilor
atunci când raportul sursă este împărțit în documente tematice.

| Constatare din raportul sursă | Document nou | ID | Tratament editorial |
|---|---|---|---|
| Economy neinițializat | Defecte confirmate | DEF-002 | Păstrat; severitatea P1 este provizorie |
| Investiție cu durată zero/negativă | Defecte confirmate | DEF-003 | Păstrat; comportamentul nu este intenționat |
| Test MCP `FRESH`/`STALE` | Defecte confirmate | DEF-001 | Păstrat ca defect de testare/reproductibilitate |
| Cleanup quest la offline | Riscuri statice | RISK-001 | Păstrat separat de reload |
| Reload pack invalid | Riscuri statice | RISK-002 | Păstrat separat de cleanup offline |
| SQL SQLite în calea generică | Riscuri statice | RISK-003 | Condiționat de suportul DB viitor |
| Smoke transfer/restart fără exit code | Riscuri statice | RISK-004 | Comasat cu problema logului smoke în planul de verificare |
| Lint CI cu `|| true` | Riscuri statice | RISK-005 | Păstrat separat; afectează feedback-ul CI |
| Parser RCON incomplet | Riscuri statice | RISK-006 | Păstrat; necesită mediu RCON |
| Provider Ollama + validator AI | Plan de remediere | — | Comasate ca flux AI incomplet, conform deciziei editoriale |
| Race condition OpenAI | Securitate / următoarea revizie | SEC-003 | Necesită documentare separată după verificarea concurenței |
| Comenzi din feature-pack | Securitate | SEC-001 | Hardening condiționat; pack-urile actuale sunt administrative |
| Injectare prin substituții | Securitate | SEC-002 | Ipoteză neconfirmată; necesită test Paper |
| Clase monolitice | Arhitectură | ARCH-001 | Păstrat ca măsurătoare statică |
| Executor WorldGen | Arhitectură | ARCH-002 | Păstrat; API aparent configurabil |
| Undo WorldEdit | Arhitectură | ARCH-003 | Păstrat; cerința este persistență permanentă dacă lumea depinde de WorldEdit |
| Shop incomplet și `clear()` | Arhitectură | ARCH-004 | Comasate; feature-ul trebuie definit înainte de clasificare finală |

## Elemente care nu trebuie confundate

- `RISK-001` se referă la cleanup-ul jucătorilor offline.
- `RISK-002` se referă la reload-ul unui snapshot de pack incomplet sau invalid.
- `DEF-001` este un test instabil/ dependent de timp, nu un defect al statusului runtime Paper.
- `SEC-001` nu este tratat ca exploatare publică în configurația actuală deoarece
  pack-urile sunt administrate de persoane de încredere.
- `ARCH-004` nu trebuie declarat defect final până când scenariul de utilizare
  al shop-ului nu este definit.

## Elemente rămase pentru următoarea revizie

1. Adăugarea câmpului `Status de verificare` pentru fiecare constatare.
2. Verificarea race condition-ului OpenAI cu un test concurent.
3. Confirmarea comportamentului shop-ului pe baza unui scenariu concret.
4. Reconcilierea raportului istoric din 2026-09-13 cu documentele noi.
