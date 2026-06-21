# D7-D9: Dialog, Restart, Final â€” Verificari pe Server Paper

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## D7: Dialog & AI Fallback

### T041: Dialog cu NPC
Click dreapta pe NPC, scrieti un mesaj scurt. Inchideti cu "pa".
**Gate:** Conversatia functioneaza fara stacktrace.

### T042: Fallback AI
Oprire server, stergeti OPENAI_API_KEY, porniti. Repetati dialogul.
**Gate:** Fallback-ul e clar, serverul stabil.

### T043: Debug OpenAI
```powershell
./scripts/debug-openai.ps1
./scripts/debug-openai.ps1 -IncludeResponseTest
```
**Gate:** Scriptul de debug OpenAI functioneaza.

## D8: Restart & Persistenta

### T044: Smoke test mapping
```powershell
./scripts/smoke-paper-mapping.ps1 -ServerDir <path> -SkipWandFlow
```
**Gate:** Smoke-mapping trece.

### T045: Smoke test quests
```powershell
./scripts/smoke-paper-quests.ps1 -ServerDir <path> -PlayerName <nume> -RegionId demo_sat
```
**Gate:** Smoke-quests trece.

### T046: Comenzi demo
```
/ainpc demo definition
/ainpc demo status demo_sat
/ainpc demo next demo_sat
/ainpc demo phases demo_sat <player>
/ainpc demo script demo_sat <player>
/ainpc demo evidence demo_sat <player>
/ainpc demo runbook demo_sat <player>
/ainpc demo smoke demo_sat <player>
/ainpc demo summary demo_sat <player>
/ainpc demo commands demo_sat <player>
/ainpc demo restart demo_sat
```
**Gate:** Toate raspund fara erori.

### T047: Restart gate
```
/ainpc demo restart demo_sat
```
Apoi: oprire, pornire, re-audit.
**Gate:** Verificarile inainte/dupa restart trec.

## D9: Demo Script Final

### T048: Audit final
```
/ainpc audit all
/ainpc debugdump all
```
**Gate:** Audit complet fara secrete expuse.

### T049: Script demo complet
20 de pasi: join, status, definition, places, spawn, list, routine, dialog, quest, quest accept, status, progression, story, audit, debugdump.

**Gate:** Testerul poate urma pasii fara sa editeze DB.

### T050: Documentare concluzie
Documentati: ce a mers, ce n-a mers, ce taskuri au ramas, ce urmeaza dupa D9.

