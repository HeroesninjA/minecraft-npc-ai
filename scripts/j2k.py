#!/usr/bin/env python3
"""Java to Kotlin converter - handles the most common patterns."""
import re, sys

def convert_java_to_kotlin(content):
    # Package
    content = re.sub(r'package\s+([\w.]+)\s*;', r'package \1', content)
    
    # Remove ALL import lines (add fresh ones later)
    content = re.sub(r'^import\s+.*$', '', content, flags=re.MULTILINE)
    
    # Remove @Override
    content = re.sub(r'@Override\s*\n', '', content)
    
    # Class declaration with fields
    old_class = 'public class ScenarioEngine {'
    new_class = '''class ScenarioEngine(
    private val plugin: AINPCPlugin
) {
    private val scenarioTemplates: MutableMap<String, ScenarioTemplate> = LinkedHashMap()
    private val questTemplates: MutableMap<String, ScenarioTemplate> = LinkedHashMap()
    private val gson: Gson = Gson()
    private val activePlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>> = ConcurrentHashMap()
    private val archivedPlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>> = ConcurrentHashMap()
    private val questCompletionLocks: MutableMap<UUID, String> = ConcurrentHashMap()
    val questDefinitions: MutableList<ScenarioDefinition> = ArrayList()
    var storyContextService: StoryContextService? = null
    private val trackedQuestPlayers: MutableSet<UUID> = HashSet()
    private val trackedQuestTemplates: MutableMap<UUID, String> = ConcurrentHashMap()

    init {
    }
'''
    content = content.replace(old_class, new_class)
    
    # Remove old field declarations
    content = re.sub(r'^\s{4}private final .*?;\s*$', '', content, flags=re.MULTILINE)
    content = re.sub(r'^\s{4}private final .*?=.*?;\s*$', '', content, flags=re.MULTILINE)
    content = re.sub(r'^\s{4}private .*? = .*?;\s*$', '', content, flags=re.MULTILINE)
    
    # Remove old constructor
    content = re.sub(
        r'\s{4}public ScenarioEngine\(AINPCPlugin plugin\) \{.*?\n    \}\s*\n',
        '', content, flags=re.DOTALL
    )
    
    # Method signatures: public Type name(args) { -> fun name(args): Type {
    content = re.sub(
        r'^\s{4}public ([\w.<>\[\],\s]+?) (\w+)\((.*?)\) \{',
        r'    fun \2(\3): \1 {', content, flags=re.MULTILINE
    )
    content = re.sub(
        r'^\s{4}public void (\w+)\((.*?)\) \{',
        r'    fun \1(\2) {', content, flags=re.MULTILINE
    )
    content = re.sub(
        r'^\s{4}private ([\w.<>\[\],\s]+?) (\w+)\((.*?)\) \{',
        r'    private fun \2(\3): \1 {', content, flags=re.MULTILINE
    )
    content = re.sub(
        r'^\s{4}private void (\w+)\((.*?)\) \{',
        r'    private fun \1(\2) {', content, flags=re.MULTILINE
    )
    
    # Fix return types
    content = content.replace(': boolean {', ': Boolean {')
    content = content.replace(': int {', ': Int {')
    content = content.replace(': long {', ': Long {')
    content = content.replace(': double {', ': Double {')
    content = content.replace(': float {', ': Float {')
    
    # Remove redundant : void (from bad matches)
    content = content.replace(': void {', ' {')
    
    # === GETTERS ===
    getters = [
        'templateId', 'displayName', 'description', 'hint', 'sourcePackId',
        'progressionEnabled', 'progressionKind', 'progressionMechanicId', 'progressionLabel',
        'progressionSingularLabel', 'progressionPluralLabel', 'progressionMaxActive',
        'questCode', 'questGiverProfession', 'questPrerequisites', 'questRepeatable',
        'questCooldownSeconds', 'questDialogues', 'questStages', 'objectives', 'rewards',
        'questContract', 'triggerProbability', 'minimumNpcCount', 'requiresPlayer',
        'narrativeHints', 'preferredTopologies', 'phases', 'roles', 'playerRoles', 'npcRoles',
        'id', 'name', 'type', 'baseType', 'amount', 'itemId', 'completionMode',
        'objectiveIds', 'payload', 'variables', 'bounds', 'occupation', 'databaseId',
        'uuid', 'location', 'maxActive', 'actionBarMessage', 'worldAdmin', 'worldAdminService',
        'scenarios', 'values'
    ]
    for g in getters:
        content = re.sub(r'\.get' + re.escape(g) + r'\(\)', '.' + g, content)
    
    # Plugin getters: plugin.getXxx() -> plugin.xxx
    content = re.sub(r'plugin\.get(\w+)\(\)', r'plugin.\1', content)
    # Second pass for nested
    content = re.sub(r'plugin\.get(\w+)\(\)', r'plugin.\1', content)
    
    # === SETTERS ===
    setter_map = {
        'TemplateId': 'templateId', 'DisplayName': 'displayName', 'Description': 'description',
        'Hint': 'hint', 'SourcePackId': 'sourcePackId', 'ProgressionEnabled': 'progressionEnabled',
        'ProgressionMechanicId': 'progressionMechanicId', 'ProgressionKind': 'progressionKind',
        'ProgressionLabel': 'progressionLabel', 'ProgressionSingularLabel': 'progressionSingularLabel',
        'ProgressionPluralLabel': 'progressionPluralLabel', 'ProgressionMaxActive': 'progressionMaxActive',
        'QuestCode': 'questCode', 'QuestGiverProfession': 'questGiverProfession',
        'QuestPrerequisites': 'questPrerequisites', 'QuestRepeatable': 'questRepeatable',
        'QuestCooldownSeconds': 'questCooldownSeconds', 'QuestDialogues': 'questDialogues',
        'QuestStages': 'questStages', 'Objectives': 'objectives', 'Rewards': 'rewards',
        'QuestContract': 'questContract', 'TriggerProbability': 'triggerProbability',
        'MinimumNpcCount': 'minimumNpcCount', 'RequiresPlayer': 'requiresPlayer',
        'NarrativeHints': 'narrativeHints', 'PreferredTopologies': 'preferredTopologies',
        'RequiredProfessions': 'requiredProfessions', 'PreferredProfessions': 'preferredProfessions',
        'RequiredTraits': 'requiredTraits', 'PreferredTraits': 'preferredTraits',
        'EnginePlugin': 'enginePlugin', 'Id': 'id', 'Occupation': 'occupation'
    }
    for old, new in setter_map.items():
        content = content.replace(f'.set{old}(', f'.{new} = (')
    
    # === BOOLEAN isXxx() -> property ===
    booleans = [
        'isQuestRepeatable', 'isProgressionEnabled', 'isOptional', 'isPlayerRole',
        'isRequiresPlayer', 'isReplaceBaseType', 'isProgressEnabled',
        'isCompleted', 'isOffered', 'isActive', 'isCurrent', 'isArchived',
        'isSpawned', 'isValid', 'isEnabled', 'isBlank', 'isAdult'
    ]
    for b in booleans:
        content = content.replace(f'.{b}()', f'.{b[:-2]}')  # Remove ()
        # Also bare calls
        content = content.replace(f'{b}()', f'{b[:-2]}')
    
    # === OTHER PATTERNS ===
    content = re.sub(r'instanceof (\w+)', r'is \1', content)
    content = re.sub(r'catch\s*\(\s*(\w+)\s+(\w+)\s*\)', r'catch (\2: \1)', content)
    content = re.sub(r'throw new ', 'throw ', content)
    content = re.sub(r'this\.', '', content)
    content = re.sub(r'List\.copyOf\(', 'toList(', content)
    content = re.sub(r'List\.of\(', 'listOf(', content)
    content = re.sub(r'Collections\.unmodifiableCollection\(', '', content)
    content = re.sub(r'Objects\.equals\(', '', content)
    
    # Remove ; at end of lines (but keep in for loops with ; separator)
    content = re.sub(r';(\s*)$', r'\1', content, flags=re.MULTILINE)
    
    # Remove trailing semicolons from .apply { } blocks
    content = re.sub(r'apply \{([^}]*?);\}', r'apply {\1}', content)
    
    # === ADD IMPORTS ===
    imports = '''package ro.ainpc.engine

import com.google.gson.Gson
import org.bukkit.Location
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition
import ro.ainpc.story.StoryContextService
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

'''
    content = content.replace('package ro.ainpc.engine\n', imports)
    
    # Clean up excessive blank lines
    content = re.sub(r'\n\n\n+', '\n\n', content)
    
    return content

if __name__ == '__main__':
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    result = convert_java_to_kotlin(content)
    
    with open(output_file, 'w', encoding='utf-8', newline='\r\n') as f:
        f.write(result)
    
    print(f"Converted: {len(result.splitlines())} lines")
