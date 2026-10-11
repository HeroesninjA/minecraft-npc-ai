# Plan de remediere și verificare

## Etapa 1 — Stabilitate locală

1. Repară testul MCP dependent de mtime/Clock.
2. Validează durata investiției și adaugă teste pentru zero/negativ.
3. Inițializează corect handler-ele economy sau elimină globalul `lateinit`.

## Etapa 2 — Persistență și reload

1. Definește contractul: progresul se recuperează din DB la reconnect.
2. Separă cleanup-ul cache-ului de ștergerea datelor persistente.
3. Implementează reload atomic cu păstrarea ultimului snapshot valid.
4. Clarifică dialectele DB înainte de a adăuga MySQL/MariaDB.

## Etapa 3 — AI

1. Decide providerul real al versiunii curente.
2. Leagă validarea de fluxul efectiv și testează provider fake, răspuns gol și răspuns invalid.
3. Mută providerul într-o abstracție injectată, nu într-o configurație citită fără efect.

## Etapa 4 — Hardening înainte de lansare

1. Restricționează comenzile din feature-pack prin allowlist și deny-by-default.
2. Verifică permisiunile și sursa fiecărui pack.
3. Repară smoke deploy, logarea rezultatelor și parserul RCON.
4. Elimină `|| true` din verificările obligatorii CI.

## Etapa 5 — Design și feature-uri incomplete

1. Definește scenariul shop înainte de implementarea finală.
2. Separă clasele monolitice pe responsabilități.
3. Decide dacă undo WorldEdit trebuie persistent după restart; apoi proiectează stocarea necesară.

## Criteriu de închidere

O constatare se marchează rezolvată numai după schimbarea codului, test automat
relevant și, când este necesar, test end-to-end pe Paper sau infrastructura țintă.
