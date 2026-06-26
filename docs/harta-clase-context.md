# Harta claselor pentru context

Actualizat: 2026-06-25

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `context`: constructia snapshot-urilor de context pentru AI, serviciul de context si redactarea datelor sensibile.

## Noduri principale

- `ContextService` -> serviciul care construieste `ContextSnapshot` pentru player-only sau player+NPC si genereaza blocuri de prompt compacte
- `ContextSnapshot` -> snapshot comprehensiv al contextului (player stats, NPC stare/ancore, regiune/place, vreme, economie, questuri, story events); include `build()` factory si `toPromptBlock()` pentru injectie in prompt LLM
- `ContextRedactor` -> obiect utilitar care redacteaza date sensibile (chei API, UUID-uri, emailuri, IPv4) din string-uri de context

## Flux principal

`ContextService.buildSnapshot(player, npc?)` -> colecteaza date din toate serviciile -> `ContextSnapshot` -> `toPromptBlock()` -> trimis catre AI

`ContextRedactor.redact(text)` -> elimina datele sensibile inainte de iesirea de pe server

## Relatii utile

- `ContextService` este stratul de agregare; citeste din `WorldAdminService`, `StoryContextService`, `ProgressionService`, `EconomyService`, `NPCManager`
- `ContextSnapshot` este un model de date read-only; nu modifica stare
- `ContextRedactor` este folosit inainte de orice output extern (loguri, prompt AI, debug dump)

## Cum se citeste

1. Incepe cu `ContextService` (punctul de intrare)
2. Continua cu `ContextSnapshot` (modelul de date)
3. Consulta `ContextRedactor` pentru securitate
