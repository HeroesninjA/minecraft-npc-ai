# Stage Metadata — Contract

## Accepted Keys

| Key | Type | Required | Description | Validation |
|-----|------|----------|-------------|------------|
| `description` | string | no | Human-readable stage description | — |
| `completion_mode` | string | no (default: `all_objectives`) | How the stage completes | Must be `all_objectives`, `any_objective`, or `manual_turn_in` |
| `next_stage` | string | no | ID of the next stage | Must reference an existing phase; cannot self-reference |
| `objectives` | list of strings | no | Objective IDs belonging to this stage | Each must reference an existing objective key |
| `spawn_actors` | string (comma-sep) | no | Actor IDs to spawn when stage activates | Unknown actors are logged as warning |
| `despawn_actors` | string (comma-sep) | no | Actor IDs to despawn when stage ends | Unknown actors are logged as warning |
| `on_stage_enter` | string (comma-sep) | no | Actor trigger IDs for stage enter | Must match known trigger ID |
| `on_stage_complete` | string (comma-sep) | no | Actor trigger IDs for stage complete | Must match known trigger ID |
| `on_stage_exit` | string (comma-sep) | no | Actor trigger IDs for stage exit | Must match known trigger ID |

## Load Behavior

All keys from the stage YAML section are loaded as metadata via `loadQuestStageMetadata()`.
Unknown keys are **silently accepted** (no warning). This allows future extensions without breaking existing packs.

Keys that are ConfigurationSection objects are **skipped** (not loaded into flat metadata).

## Runtime Usage

| Stage Key | Where It's Used |
|-----------|----------------|
| `completion_mode` | `ScenarioQuestPhase.getStageCompletionMode()` — determines progression logic |
| `next_stage` | `QuestStageDefinition.getNextStageId()` — transitions after stage complete |
| `objectives` | `FeaturePackLoader.QuestStageDefinition.objectiveIds` — links objectives |
| `spawn_actors`, `despawn_actors` | `ScenarioEngine.getScenarioPhaseActorRule()` — spawn/despawn on phase change |
| `on_stage_enter`, `on_stage_complete`, `on_stage_exit` | `ScenarioEngine.readScenarioActorTriggerList()` — actor triggers |
