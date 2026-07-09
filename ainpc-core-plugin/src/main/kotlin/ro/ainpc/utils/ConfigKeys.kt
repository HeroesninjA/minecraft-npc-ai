package ro.ainpc.utils

object ConfigKeys {
    const val ROUTINE_ENABLED = "routine.enabled"
    const val ROUTINE_TICK_SECONDS = "routine.tick_seconds"
    const val ROUTINE_ARRIVAL_RADIUS = "routine.arrival_radius"
    const val ROUTINE_MIN_TELEPORT = "routine.min_teleport_distance"
    const val ROUTINE_FORCE_TELEPORT = "routine.force_teleport_distance"
    const val ROUTINE_NATURAL_MOVEMENT = "routine.natural_movement.enabled"
    const val ROUTINE_NATURAL_MAX_DIST = "routine.natural_movement.max_distance"
    const val ROUTINE_NATURAL_SPEED = "routine.natural_movement.speed"
    const val ROUTINE_MOVE_COOLDOWN = "routine.move_cooldown_seconds"
    const val ROUTINE_TELEPORT_ENABLED = "routine.teleport_enabled"
    const val ROUTINE_COORDINATOR_ONLY = "routine.coordinator_only"
    const val ROUTINE_SYNC_SOCIAL = "routine.sync_social_movement"
    const val ROUTINE_BATCH_SIZE = "routine.batch_size"

    const val ECONOMY_INTEREST_RATE = "economy.interest_rate"
    const val ECONOMY_INTERVAL_HOURS = "economy.interest_interval_hours"
    const val ECONOMY_INTEREST_ENABLED = "economy.interest_enabled"
    const val ECONOMY_INTEREST_CHECK = "economy.interest_check_seconds"
    const val ECONOMY_SALARIES_ENABLED = "economy.npc_salaries_enabled"
    const val ECONOMY_SALARY_INTERVAL = "economy.salary_interval_seconds"

    const val MCP_ENABLED = "mcp.enabled"
    const val MCP_WRITE_TOOLS = "mcp.write_tools_enabled"
    const val MCP_COMMAND_PATH = "mcp.command.path"
    const val MCP_SNAPSHOT_AUTO = "mcp.snapshot.auto"

    const val SEASONAL_BEHAVIOR = "seasonal.behavior_enabled"

    const val STORY_REACTIONS = "story.npc_reactions_enabled"
    const val STORY_RANDOM_EVENTS = "story.random_events_enabled"
    const val STORY_RANDOM_INTERVAL = "story.random_events_interval_seconds"

    const val NPC_MEMORY_DECAY_DAYS = "npc.memory_decay_days"

    const val FEATURE_AI = "features.ai"
    const val FEATURE_ROUTINE = "features.routine"
    const val FEATURE_QUEST = "features.quest"
    const val FEATURE_STORY = "features.story"
    const val FEATURE_MAPPING = "features.mapping"
    const val FEATURE_SIMULATION = "features.simulation"
    const val FEATURE_GENERATION = "features.generation"
    const val FEATURE_GUI = "features.gui"
    const val FEATURE_MCP = "features.mcp"

    const val AI_PROVIDER = "ai_provider"
    const val AI_ORCHESTRATION = "ai.orchestration.enabled"

    const val HOT_RELOAD = "feature_packs.hot_reload"
    const val EVENTS_PUBLIC_API = "events.public_api_enabled"
    const val DEBUG_ENABLED = "debug.enabled"
}
