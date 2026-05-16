# Quest, Story si AI

Actualizat: 2026-05-08

Aceasta categorie acopera questurile, story state-ul, contextul AI si authoring-ul asistat.

## Documente

| Document | Rol |
|---|---|
| `../../questuri-faza-1-stabilizare.md` | Faza Q1 pentru stabilizarea Q01-Q05, audit template si smoke test |
| `../../pregatire-questuri-avansate.md` | Pregatiri si status pentru Q06-Q08, stages liniare, ID-uri stabile, audit si smoke test |
| `../../questuri-avansate-v2.md` | Faze V2 pentru diversitate de questuri, mapping, progres generic si mecanici non-quest |
| `../../progression-service.md` | Directie pentru motor generic de progres peste questuri, contracte, datorii, evenimente, tutoriale si ritualuri |
| `../../lucru-alternat-quest-mapping-progression.md` | Protocol de lucru alternat intre mapping, questuri concrete, GUI system si extractii mici spre `ProgressionService` |
| `../../dialog-si-conversatii.md` | Evolutia dialogului pe masura ce avanseaza quest, story, environment si reputatie |
| `../../interactiuni.md` | Fluxul click/chat/sesiune, ascultare pasiva si intentii de quest inaintea dialogului liber |
| `../../gui-interfete.md` | Directie pentru Quest GUI, NPC interaction GUI si suprafete vizuale peste quest/story |
| `../../ai-orchestrare-si-mecanici.md` | AI transversal peste dialog, questuri, story, environment, reactii si tool calls validate |
| `../../quest-anchor-bindings.md` | Contract DB pentru ancore semantice de quest |
| `../../story-context-service.md` | Context narativ read-only pentru prompt si debugging |
| `../../story-si-context-ai.md` | Legatura `mapping -> indexare -> quest -> story -> AI` |
| `../../environment-context-si-engine.md` | Context read-only de lume pentru questuri si EnvironmentEngine viitor |
| `../../betonquest-directii-potrivite-pentru-ainpc.md` | Inspiratie pentru quest runtime matur |
| `../../generare-ai-si-constructie-automata.md` | AI ca tool de authoring validat |
| `../../reactie-npc-jucator.md` | Reactii NPC legate de questuri si interactiuni |

## Status scurt

- Questurile de baza sunt functionale.
- `ProgressionService` este directia recomandata pentru a folosi runtime-ul de quest ca motor generic de progres in addonuri, inclusiv pentru mecanici care nu se numesc quest.
- Pregatirea pentru questuri avansate a inceput: `objective_id` stabil exista initial, cu fallback pentru progres legacy.
- Auditul strict pentru continut avansat exista initial: obiective/reward-uri necunoscute si story actions incomplete sunt raportate ca erori.
- Q06 medieval exista initial ca primul quest pe mapping: `visit_place`, `inspect_node`, `talk_to_npc`, stages `INVESTIGATION/RETURN` si `record_story_event`.
- Q07 medieval exista initial ca quest social/delivery: `collect_item`, `talk_to_npc`, `deliver_to_npc`, stages `CONTACT/RETURN` si `record_story_event`.
- Q08 medieval exista initial ca hunt contextual: `visit_region`, `kill_mob`, `talk_to_npc`, stages `PATROL/RETURN` si `record_story_event`.
- D01 medieval exista initial ca sarcina NPC non-quest: `base_type: DUTY`, mecanica `npc_duties`, stages `PATROL/RETURN`, obiective mapate si `record_story_event`.
- B01/B02 medievale exista initial ca bounty-uri locale non-quest: `base_type: BOUNTY`, mecanica `local_bounties`, stages `HUNT/RETURN`, `kill_mob`, ancore pe regiune/place si `record_story_event`.
- E01 medieval exista initial ca eveniment local non-quest: `base_type: WORLD_EVENT`, mecanica `village_events`, stages `CHECK/RETURN`, obiective mapate si `record_story_event`.
- T01 medieval exista initial ca tutorial non-quest: `base_type: TUTORIAL`, mecanica `onboarding`, stages `LEARN/RETURN`, obiective mapate si `record_story_event`.
- R01 medieval exista initial ca ritual local non-quest: `base_type: RITUAL`, mecanica `village_rituals`, stages `PREPARE/CEREMONY/RETURN`, altar mapat si `record_story_event`.
- `quest.stages` exista initial in loader/runtime/audit/debugdump pentru flux liniar, cu `all_objectives`, `any_objective`, `manual_turn_in` si `next_stage` explicit.
- Fluxul de interactiuni exista initial pentru click dreapta, chat privat, passive listen, intentii de quest si fallback la dialog liber.
- `visit_place`, `inspect_node` si `QuestAnchorResolver` exista initial.
- Ancorele semantice sunt persistate initial in `quest_anchor_bindings`.
- `/ainpc quest anchors` si `/ainpc audit quest` exista initial pentru inspectie read-only.
- `StoryContextService` si `/ainpc story context` exista initial ca strat read-only peste mapping, quest anchors si story state persistent.
- `StoryStateService` exista initial pentru `region_story_state`, `place_story_state` si `story_events`.
- `/ainpc story region`, `/ainpc story place` si `/ainpc story events` exista initial pentru inspectia read-only a story state-ului persistent.
- Actiunile de quest `set_story_state` si `record_story_event` exista initial.
- `/ainpc audit quest` verifica initial si quest templates, nu doar binding-uri.
- `/ainpc audit quest`, `/ainpc debugdump quest` si `/ainpc debugdump story` acopera initial story state/events persistente.
- Quest GUI consuma snapshot-ul generic de progres si are filtre interactive pentru questuri, contracte, sarcini, bounty-uri, evenimente, tutoriale si ritualuri.
- `EnvironmentContextService` este directia recomandata pentru context read-only de lume; `EnvironmentEngine` complet ramane feature viitor.
- Dialogul trebuie sa devina quest-aware, story-aware si environment-aware, dar sa ramana prezentare peste runtime validat.
- `AIOrchestrationService` exista initial pentru politici, fallback determinist si output-uri non-executabile direct; integrarea cu dialog/quest/story ramane urmatorul pas.
- AI-ul trebuie orchestratat central printr-un serviciu dedicat, dar executia ramane in serviciile deterministe.
- Lipsesc inca branching, hook-uri intermediare pe stage si validator complet pentru story actions complexe.

## Urmatoarele documente utile

- Contract pentru `StoryDraft` si `QuestDraft`.
- Ghid de prompturi sigure pentru generare quest/story.
- Specificatie completa si validator pentru actiunile `set_story_state` si `record_story_event`.
- Contract complet pentru `DialogueContextSnapshot`, dialogue nodes si choices validate.
