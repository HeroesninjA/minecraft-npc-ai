# GUI Stack

Actualizat: 2026-06-29

Acesta este punctul de intrare pentru documentele de UI:

- `gui-interfete.md` descrie stratul GUI principal pentru quest, world, stats, shop, debug, audit, story (inclusiv cardul de mediu la slot 5) si interactiune NPC
- `gui-admin-mapping-quest.md` descrie suprafata de administrare pentru mapping si quest, inclusiv AdminMappingGui cu identitate regiune (slot 6), patch buttons (slots 22/23) si comenzi create simplificate
- `playable-village-ux.md` descrie criteriile de lizibilitate si jucabilitate pentru primul sat

## Noutati

- `gui.skip_confirmations` in config.yml — skip toate confirmarile GUI pentru admini experimentati
- StoryGui arata card de mediu (slot 5) cu timp, vreme, anotimp, temperatura si evenimente speciale
- QuestLogGui are sumar progres (slot 2), context poveste (slot 6) si filtre avansate inline (slots 14-18)
- AdminMappingGui are identitate regiune, butoane patch analyze/plan si comenzi corecte pentru create place/node

Ordinea recomandata de citire:

1. `gui-interfete.md`
2. `gui-admin-mapping-quest.md`
3. `playable-village-ux.md`

Documentul exista ca strat scurt de navigare si reduce suprapunerea dintre UI-ul principal, UI-ul admin si UX-ul de playability.
