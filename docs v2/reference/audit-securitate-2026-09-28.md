# Securitate și hardening

## SEC-001 — Comenzi de consolă definite în feature-pack

- **Dovadă:** `FeaturePackYamlSupport` încarcă `runtime_actions`; `ExecuteCommandAction` acceptă `as_console` și folosește `Bukkit.getConsoleSender()`; recompensa `command` execută `itemId` ca text de consolă.
- **Context:** pack-urile sunt administrate de persoane de încredere în etapa curentă.
- **Impact condiționat:** dacă sursa pack-ului devine accesibilă unui jucător, AI sau operator limitat, conținutul poate obține privilegii de consolă.
- **Remediere înainte de lansare:** deny-by-default, allowlist de comenzi/argumente, interzicerea implicită a `as_console`, validare și audit al sursei pack-ului.
- **Status:** risc static de hardening, nu vulnerabilitate publică confirmată în configurația actuală.

## SEC-002 — Substituțiile din comenzi nu au test de injecție

- **Dovadă:** comenzile înlocuiesc `{player}`/`{uuid}` sau `${player}` înainte de dispatch.
- **Risc:** dacă valorile nu sunt normalizate și ajung să conțină separatori acceptați de parserul Bukkit, rezultatul poate diferi de intenția pack-ului.
- **Remediere:** validează identificatorii, testează caractere speciale și nu construi comenzi arbitrare din input extern.
- **Status:** ipoteză statică; necesită test Paper și confirmarea regulilor parserului Bukkit.

## SEC-003 — Contextul global al cererilor OpenAI poate fi suprascris

- **Dovadă:** `OpenAIService` păstrează numele NPC-ului și al jucătorului în
  câmpuri mutabile, apoi le citește în callback-ul asincron.
- **Impact:** două cereri simultane pot atribui istoricul de debug unei alte
  combinații NPC/jucător.
- **Remediere:** păstrează contextul ca valoare locală per request și propagă-l
  prin `CompletableFuture`.
- **Status:** risc static; necesită test concurent cu cel puțin două requesturi.
