# Changelog

Toate schimbarile notabile ale proiectului trebuie documentate aici.

Formatul este orientat pe intrari scurte, verificabile:

- `Added` pentru functionalitate noua
- `Changed` pentru schimbari de comportament sau arhitectura
- `Fixed` pentru bugfix-uri
- `Docs` pentru documentatie
- `Tests` pentru verificari adaugate sau rulate

## [Unreleased]

### Added

- Persistenta story initiala prin tabelele DB `region_story_state`, `place_story_state` si `story_events`.
- `StoryStateService` pentru citirea/scrierea starii story pe regiune/place si pentru inregistrarea evenimentelor story.
- Modele runtime pentru story persistent: `RegionStoryState`, `PlaceStoryState` si `StoryEvent`.
- Integrare initiala a story state-ului persistent in `StoryContextSnapshot` si in blocul `STORY_CONTEXT`.
- Comenzi read-only pentru story state persistent: `/ainpc story region <regionId>`, `/ainpc story place <placeId>` si `/ainpc story events <regionId|placeId> [limit]`.
- Actiuni de quest `set_story_state` si `record_story_event`, executate controlat la finalizarea questului.
- `QuestEntryDefinition` pastreaza acum metadata, `variables` si `payload` din YAML pentru actiuni non-item.

### Changed

- `AINPCPlugin` initializeaza si expune `StoryStateService` prin `getStoryStateService()`.
- `StoryContextService` include, cand exista mapping curent, starea persistenta a regiunii/place-ului si evenimentele story recente.
- Tab completion-ul pentru `/ainpc story` include acum `context`, `region`, `place` si `events`.
- Verificarea si acordarea recompenselor pe iteme ignora actiunile story, ca sa nu fie tratate ca materiale Minecraft invalide.

### Docs

- Actualizat `TODO.md` pentru a muta persistenta story initiala la functionalitati existente.
- Actualizate documentele de stare pentru story: `docs/story-context-service.md`, `docs/story-si-context-ai.md`, `docs/implementat-deja.md`, `docs/faze-observatii-avertizari.md`, `docs/README.md` si `docs/categorii/03-quest-story-ai/README.md`.

### Tests

- Rulat `mvn -pl ainpc-core-plugin -am test`.
- Rezultat: `27` teste trecute, `0` esecuri.

### Remaining

- Audit/debugdump dedicat pentru story state si story events.
- Validator dedicat pentru actiunile story din feature packs.
