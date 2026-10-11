# Defecte confirmate prin teste locale

## DEF-001 — Test MCP dependent de timpul fișierului

- **Dovadă:** `:ainpc-mcp-service:test` a avut 1 eșec din 53; `AinpcDebugHealthToolsTest.debugHealthIncludesRuntimeHealthSnapshot` așteaptă `FRESH`, dar primește `STALE`.
- **Cauză observată:** copia fixture-ului păstrează mtime-ul vechi, iar `SnapshotReader` compară mtime-ul cu pragul de 3600 secunde.
- **Impact:** suita completă Gradle nu este verde, deși modulele core, API și scenario trec.
- **Reproducere:** `:ainpc-mcp-service:test` cu JDK 25 local și proiect configurat pentru Java 21.
- **Remediere:** setează explicit mtime-ul fixture-ului sau injectează un `Clock` controlabil.
- **Status:** confirmat; codul aplicației nu a fost modificat.

## DEF-002 — Ruta economy folosește un plugin neinițializat

- **Dovadă:** `AINPCCommandEconomy.kt` declară un `lateinit` global; inițializarea nu este apelată din constructorul principal, iar ruta este folosită de `AINPCCommand`.
- **Impact:** comenzile `balance`, `pay`, `set` și `top` pot produce `UninitializedPropertyAccessException`.
- **Reproducere:** necesită dispatch pe server Paper; analiza statică a confirmat calea de acces.
- **Remediere:** injectează pluginul în handler și adaugă teste de dispatch.
- **Status:** confirmat static; nu a fost executat pe Paper.

## DEF-003 — Investițiile acceptă durată zero sau negativă

- **Dovadă:** parserul acceptă orice integer, iar `BankingService.invest` nu validează durata; `Investment.matured` consideră imediat maturată o durată `<= 0`.
- **Impact:** o investiție poate fi colectată instant și poate acorda randament fără așteptare.
- **Reproducere:** `/ainpc economy invest 100 0`, apoi `collect`, pe un server Paper.
- **Remediere:** respinge `durationHours <= 0` și adaugă limită maximă configurabilă.
- **Status:** confirmat static; nu a fost executat pe Paper.
