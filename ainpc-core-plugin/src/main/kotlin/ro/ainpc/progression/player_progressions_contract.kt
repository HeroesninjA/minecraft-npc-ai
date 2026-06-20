package ro.ainpc.progression

/**
 * Contract pentru viitoarea tabelă `player_progressions`.
 *
 * Scop: unifică quest, contract, duty, bounty, event, tutorial, ritual
 * într-o singură tabelă cu metadata generică, înlocuind `player_quests`.
 *
 * Reguli de migrare (activă doar după stabilizarea modelului):
 * 1. `player_quests` rămâne tabela sursă până la migrare.
 * 2. `player_progressions` NU se creează încă în DB.
 * 3. Acest contract definește doar structura și regulile de mapare.
 *
 * Migrarea activă va necesita:
 * - Script DDL pentru crearea tabelei
 * - Script DML pentru migrarea datelor existente
 * - Comutare repository de la `player_quests` la `player_progressions`
 * - Backward compatibility pentru datele vechi
 */
data class PlayerProgressionRecord(
    val playerUuid: String,
    val progressionId: String,
    val packId: String,
    val mechanicId: String,
    val kind: String,
    val category: String,
    val scenarioKind: String,
    val baseType: String,
    val definitionId: String,
    val templateId: String,
    val code: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long,
    val currentPhase: String,
    val currentStageId: String,
    val objectiveProgressJson: String,
    val variablesJson: String,
    val updatedAt: Long,
    val tracked: Boolean
)

/**
 * Regulile de mapare între `PlayerProgressionRecord` și definiții.
 */
object PlayerProgressionContract {
    const val TABLE_NAME = "player_progressions"
    const val COMPATIBILITY_SOURCE = "player_quests"

    val SUPPORTED_KINDS: Set<String> = setOf(
        "quest", "contract", "duty", "bounty",
        "event", "tutorial", "ritual"
    )

    val DDL_TEMPLATE: String
        get() = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                player_uuid        VARCHAR(36)  NOT NULL,
                progression_id     VARCHAR(255) NOT NULL,
                pack_id            VARCHAR(128) NOT NULL DEFAULT '',
                mechanic_id        VARCHAR(128) NOT NULL DEFAULT '',
                kind               VARCHAR(64)  NOT NULL DEFAULT 'quest',
                category           VARCHAR(64)  NOT NULL DEFAULT '',
                scenario_kind      VARCHAR(64)  NOT NULL DEFAULT '',
                base_type          VARCHAR(64)  NOT NULL DEFAULT '',
                definition_id      VARCHAR(255) NOT NULL DEFAULT '',
                template_id        VARCHAR(255) NOT NULL DEFAULT '',
                code               VARCHAR(255) NOT NULL DEFAULT '',
                status             VARCHAR(32)  NOT NULL DEFAULT 'offered',
                started_at         BIGINT       NOT NULL DEFAULT 0,
                completed_at       BIGINT       NOT NULL DEFAULT 0,
                current_phase      VARCHAR(128) NOT NULL DEFAULT '',
                current_stage_id   VARCHAR(128) NOT NULL DEFAULT '',
                objective_progress TEXT         NOT NULL DEFAULT '{}',
                variables_json     TEXT         NOT NULL DEFAULT '{}',
                updated_at         BIGINT       NOT NULL DEFAULT 0,
                tracked            INT          NOT NULL DEFAULT 0,
                PRIMARY KEY (player_uuid, progression_id)
            );
        """.trimIndent()

    val COMPATIBILITY_VIEW: String
        get() = """
            -- View logic pentru compatibilitate cu player_quests
            -- Creat doar la migrare, nu înainte
            SELECT player_uuid, template_id AS quest_template, code AS quest_code,
                   status, started_at, completed_at, current_phase, current_stage_id,
                   objective_progress, variables_json, updated_at, tracked
            FROM $TABLE_NAME
            WHERE kind = 'quest'
        """.trimIndent()
}
