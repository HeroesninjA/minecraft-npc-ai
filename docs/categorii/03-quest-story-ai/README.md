# Quest, Story si AI

Actualizat: 2026-04-30

Aceasta categorie acopera questurile, story state-ul, contextul AI si authoring-ul asistat.

## Documente

| Document | Rol |
|---|---|
| `../../questuri-avansate.md` | Evolutia questurilor pe obiective, etape si progres |
| `../../quest-anchor-bindings.md` | Contract DB pentru ancore semantice de quest |
| `../../story-context-service.md` | Context narativ read-only pentru prompt si debugging |
| `../../story-si-context-ai.md` | Legatura `mapping -> indexare -> quest -> story -> AI` |
| `../../betonquest-directii-potrivite-pentru-ainpc.md` | Inspiratie pentru quest runtime matur |
| `../../generare-ai-si-constructie-automata.md` | AI ca tool de authoring validat |
| `../../reactie-npc-jucator.md` | Reactii NPC legate de questuri si interactiuni |

## Status scurt

- Questurile de baza sunt functionale.
- `visit_place`, `inspect_node` si `QuestAnchorResolver` exista initial.
- Ancorele semantice sunt persistate initial in `quest_anchor_bindings`.
- `/ainpc quest anchors` si `/ainpc audit quest` exista initial pentru inspectie read-only.
- `StoryContextService` si `/ainpc story context` exista initial ca strat read-only peste mapping si quest anchors.
- Story state persistent pe regiune/place nu este inca implementat.

## Urmatoarele documente utile

- Contract pentru `StoryDraft` si `QuestDraft`.
- Ghid de prompturi sigure pentru generare quest/story.
- Specificatie pentru story state persistent si events.
