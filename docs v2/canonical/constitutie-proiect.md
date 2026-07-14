# Constitutie Proiect AINPC

Status: canonical in `docs v2`.
Actualizat: 2026-07-14.

Acesta este documentul constitutional al proiectului. Stabileste directia, limitele si regulile care au prioritate peste backlog, idei temporare si implementari incomplete.

## Identitate

AINPC este un plugin Paper modular pentru Minecraft, orientat pe NPC-uri persistente, sate lizibile, questuri, rutina, context narativ si integrare AI controlata.

Obiectivul nu este doar functionalitate izolata. Obiectivul este un server in care playerul intelege rapid cine sunt NPC-urile, ce rol au, unde locuiesc si de ce conteaza actiunile sale.

## Principii obligatorii

1. Playability inainte de volum.
2. Date validate inainte de automatizare.
3. Core determinist, AI asistiv.
4. Mapping semantic inainte de gameplay avansat.
5. Modularizare fara fragmentare prematura.
6. Compatibilitate pentru addonuri.
7. Test si audit pentru schimbari riscante.
8. Documentatia descrie starea reala.
9. Configurabilitate explicita.
10. Dezactivare completa pentru feature-uri majore.
11. Core neutru.
12. Feature-uri neperturbatoare.
13. Core rigid, addonuri libere.

## Structura canonica

| Zona | Rol |
|---|---|
| `ainpc-api` | Contract public stabil pentru addonuri |
| `ainpc-core-plugin` | Implementarea Paper: comenzi, NPC, quest, story, mapping, GUI, AI, debug |
| `ainpc-scenario-medieval` | Addon exemplar de scenariu |
| Addonuri de scenariu/story/resurse | Continut si reguli specifice |
| Datapack-uri compatibile | Interoperabilitate optionala |
| `docs v2` | Documentatia activa: reguli, design, roadmap si runbook |
| `docs` | Redirecturi de compatibilitate catre `docs v2` |
| `scripts` | Automatizari locale |
| `data`, `.ai`, `.codex` | Context si artefacte operationale |

Regula de baza: `ainpc-api` nu depinde de `ainpc-core-plugin`, iar core-ul nu trebuie sa forteze addonurile sa cunoasca internals.

## Directia de dezvoltare

1. Baseline verificabil: build, audit, debugdump, documentatie, context MCP, teste.
2. Sat jucabil: mapping semantic, regiuni, places, nodes, spawn controlat.
3. Demo intern: onboarding, dialog, questuri cap-coada, progression, story, GUI.
4. Modularizare publica: API stabil, addon registry, contracte compatibile.
5. Runtime extensibil: scenario runtime, triggers, conditions, actions, generare asistata.
6. Generare de harti si structuri.
7. Infrastructura server si date.
8. AI specializat si productie.

Aceasta ordine nu interzice explorarea, dar interzice tratarea explorarii ca functionalitate stabila.

## Reguli de prioritate

- Fix pentru build/test blocant.
- Protectie date, backup, migration sau rollback.
- Smoke test si observabilitate pentru functionalitate existenta.
- Functionalitate necesara primului demo intern.
- Stabilizare API sau addonuri.
- Generare automata, AI avansat si simulare mare.
- Refactor cosmetic.

## AI, date si gameplay

- AI-ul propune, rezuma si completeaza continut validat, dar nu decide progresul sau persistenta.
- Datele critice cer backup, inspectie si reparare.
- Gameplay-ul trebuie sa fie inteligibil pentru player si operabil pentru admin.
- Feature-urile noi intra initial ca `preview`, `dryrun`, `read-only`, `experimental` sau `disabled by default` daca pot afecta lumea, datele sau experienta playerului.

## API si addonuri

- API-ul public trebuie sa fie mic, stabil si documentat.
- Tipurile expuse din `ainpc-api` trebuie sa fie Java-friendly pana exista motiv explicit pentru altfel.
- Addonurile livreaza continut, tema, reguli speciale si pack-uri proprii.
- Core-ul ofera infrastructura si contracte, nu concureaza cu addonurile la expresivitate.
- Breaking changes in API cer documentare, test de compatibilitate si motiv clar.
