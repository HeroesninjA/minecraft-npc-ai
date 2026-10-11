# Arhitectură și implementare grăbită

## ARCH-001 — Clase cu prea multe responsabilități

- `AINPCCommand.kt`: aproximativ 5.175 linii non-goale și 220 funcții.
- `AINPCCommandText.kt`: aproximativ 2.886 linii și 211 funcții.
- `ScenarioEngine.kt`: aproximativ 2.974 linii și 134 funcții.
- `NPCManager.kt`: aproximativ 1.815 linii și 85 funcții.
- **Impact:** routing, validare, DB, mesaje și efecte Bukkit sunt amestecate; testarea și refactorizarea devin riscante.
- **Remediere:** extrage servicii pe responsabilități și păstrează comenzile ca adaptoare subțiri.
- **Status:** măsurătoare statică.

## ARCH-002 — Executorul WorldGen pare configurabil, dar este fixat la construcție

- `WorldGenService` calculează o preferință, dar păstrează executorul injectat; parametrul `executorPreference` nu schimbă executorul fizic.
- **Impact:** API-ul poate induce în eroare și poate ascunde faptul că preferința nu are efect.
- **Remediere:** elimină parametrul nefuncțional sau construiește explicit executorul selectat.
- **Status:** static.

## ARCH-003 — Undo WorldEdit este volatil și are lifecycle neuniform

- Sesiunile sunt ținute în memorie și nu supraviețuiesc restartului.
- `WorldEditBlockExecutor.undo` șterge sesiunea înainte de finalizare și nu are același flush/close ca implementarea alternativă.
- **Impact:** undo poate deveni imposibil după restart sau incomplet după eroare.
- **Remediere:** definește contractul persistent pentru lumi dependente de WorldEdit și unifică lifecycle-ul sesiunilor.
- **Status:** static; comportamentul permanent trebuie confirmat prin scenariul de utilizare.

## ARCH-004 — Shop-ul este feature incomplet, nu flux universal

- Nu există o sursă clară de producție pentru înregistrarea shop-urilor.
- GUI-ul afișează oferte, dar fluxul de cumpărare/vânzare și intenția clickului nu sunt definite complet.
- `ShopService.clear()` nu golește indexul de roluri.
- **Impact:** feature-ul poate afișa elemente fără comportament coerent și poate păstra date vechi după reload.
- **Remediere:** decide întâi scenariul de utilizare; apoi definește sursa ofertelor, confirmarea și lifecycle-ul reloadului.
- **Status:** incomplet funcțional; clasificarea finală depinde de scenariul shop.
