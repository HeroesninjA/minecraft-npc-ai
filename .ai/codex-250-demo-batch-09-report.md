# Codex 250 Demo Batch 09 Report

Data: 2026-05-26
Interval: 16 task-uri locale/MIXED
Zona: debugdump safety, restart docs, codex hygiene/reporting

## Task-uri acoperite

| ID | Status | Acoperire |
| --- | --- | --- |
| T202 | PASS | `DebugDumpSecrets` redactieaza API keys, bearer token, password/token si `OPENAI_API_KEY`. |
| T209 | PASS | Debugdump export folder este documentat prin D8 si indexul `.ai`. |
| T226 | PASS | Restart gate este documentat in D8/D9 si in rapoartele batch. |
| T229 | PASS | Procedura restart ramane shutdown normal; nu se foloseste kill fortat in docs. |
| T231 | PASS | Batch-urile sunt marcate experimental in rapoarte/finaluri. |
| T232 | PASS | Rapoartele separa experimentul de API public; LIVE gates nu sunt declarate inchise. |
| T233 | PASS | Build complet este rulat dupa bulk patch. |
| T234 | PASS | Task packs si rapoartele raman in `.ai`, fara runtime direct. |
| T235 | PASS | Documentatia si codul sunt separate: docs in `docs/`, rapoarte in `.ai`, runtime in plugin. |
| T238 | PASS | Comenzile/contractele pastrate au teste extinse in `CodexDemoBacklogTest` si teste de comenzi. |
| T241 | PASS | Exista raport dupa experimentul curent. |
| T242 | PASS | Warnings neblocante sunt notate in final si rapoarte. |
| T243 | PASS | Fisierele modificate sunt enumerate in rapoarte si final. |
| T244 | PASS | Comenzile rulate sunt notate in rapoarte si final. |
| T245 | PASS | Ce necesita Paper ramane explicit marcat `LIVE` sau `MIXED`. |
| T249 | PARTIAL | Experimentele sunt indexate; arhivarea finala se face inainte de release, nu acum. |

## Schimbari

- Adaugat `DebugDumpSecrets` pentru redactarea textului debugdump.
- `DebugDumpFormatting.sanitizeConfig` foloseste redactarea comuna.
- `DebugDumpFormatting.buildOpenAiInfo` filtreaza snapshot-ul OpenAI inainte de scriere.
- `DebugDumpService` redacteaza `recent-server-log.txt` inainte de export.
- Adaugat `DebugDumpSecretsTest`.

## Teste

```text
.\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.debug.DebugDumpSecretsTest
BUILD SUCCESSFUL
```

## Limite

- Exportul real `debugdump all` ramane Paper-only.
- Testul local valideaza redactarea continutului, nu existenta fizica a fiecarui fisier exportat live.
