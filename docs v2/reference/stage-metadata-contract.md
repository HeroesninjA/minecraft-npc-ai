# Stage Metadata Contract

Acesta este contractul pentru metadatele YAML ale etapelor de quest.

## Chei acceptate

- `description`;
- `completion_mode`;
- `next_stage`;
- `objectives`;
- `spawn_actors`;
- `despawn_actors`;
- `on_stage_enter`;
- `on_stage_complete`;
- `on_stage_exit`.

## Reguli

- cheile necunoscute sunt acceptate fara warning;
- cheile de tip sectiune sunt ignorate la flatten;
- metadatele sunt consumate de `ScenarioQuestPhase`, `QuestStageDefinition` si `ScenarioEngine`.
