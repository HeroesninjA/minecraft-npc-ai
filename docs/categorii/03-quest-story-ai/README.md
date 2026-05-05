# Quest, Story si AI

Actualizat: 2026-05-05

Aceasta categorie acopera questurile, story state-ul, contextul AI si authoring-ul asistat.

## Documente

| Document | Rol |
|---|---|
| `../../questuri-faza-1-stabilizare.md` | Faza Q1 pentru stabilizarea Q01-Q05, audit template si smoke test |
| `../../pregatire-questuri-avansate.md` | Pregatiri minime pentru Q06-Q08, ID-uri stabile, audit si smoke test |
| `../../questuri-avansate.md` | Evolutia questurilor pe obiective, etape si progres |
| `../../dialog-si-conversatii.md` | Evolutia dialogului pe masura ce avanseaza quest, story, environment si reputatie |
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
- Pregatirea pentru questuri avansate a inceput: `objective_id` stabil exista initial, cu fallback pentru progres legacy.
- Auditul strict pentru continut avansat exista initial: obiective/reward-uri necunoscute si story actions incomplete sunt raportate ca erori.
- Q06 medieval exista initial ca primul quest pe mapping: `visit_place`, `inspect_node`, `talk_to_npc` si `record_story_event`.
- Q07 medieval exista initial ca quest social/delivery: `collect_item`, `talk_to_npc`, `deliver_to_npc` si `record_story_event`.
- Q08 medieval exista initial ca hunt contextual: `visit_region`, `kill_mob`, `talk_to_npc` si `record_story_event`.
- `visit_place`, `inspect_node` si `QuestAnchorResolver` exista initial.
- Ancorele semantice sunt persistate initial in `quest_anchor_bindings`.
- `/ainpc quest anchors` si `/ainpc audit quest` exista initial pentru inspectie read-only.
- `StoryContextService` si `/ainpc story context` exista initial ca strat read-only peste mapping, quest anchors si story state persistent.
- `StoryStateService` exista initial pentru `region_story_state`, `place_story_state` si `story_events`.
- `/ainpc story region`, `/ainpc story place` si `/ainpc story events` exista initial pentru inspectia read-only a story state-ului persistent.
- Actiunile de quest `set_story_state` si `record_story_event` exista initial.
- `/ainpc audit quest` verifica initial si quest templates, nu doar binding-uri.
- `EnvironmentContextService` este directia recomandata pentru context read-only de lume; `EnvironmentEngine` complet ramane feature viitor.
- Dialogul trebuie sa devina quest-aware, story-aware si environment-aware, dar sa ramana prezentare peste runtime validat.
- AI-ul trebuie orchestratat central printr-un serviciu dedicat, dar executia ramane in serviciile deterministe.
- Lipsesc inca audit/debugdump dedicat si validator complet pentru story actions.

## Urmatoarele documente utile

- Contract pentru `StoryDraft` si `QuestDraft`.
- Ghid de prompturi sigure pentru generare quest/story.
- Specificatie completa si validator pentru actiunile `set_story_state` si `record_story_event`.
- Contract complet pentru `DialogueContextSnapshot`, dialogue nodes si choices validate.
