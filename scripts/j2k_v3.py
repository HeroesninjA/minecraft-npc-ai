#!/usr/bin/env python3
"""Minimal Java-to-Kotlin converter - only handle patterns we KNOW work."""
import re, sys

def safe_convert(content):
    # Package
    content = re.sub(r'^package\s+([\w.]+)\s*;', r'package \1', content, flags=re.MULTILINE)
    
    # Remove static imports and regular imports (add fresh ones)
    content = re.sub(r'^import static .*$', '', content, flags=re.MULTILINE)
    content = re.sub(r'^import (?!static).*$', '', content, flags=re.MULTILINE)
    
    # Remove @Override
    content = re.sub(r'@Override\s*\n', '', content)
    
    # Class declaration
    content = content.replace('public class ScenarioEngine {', '''class ScenarioEngine(
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
    init {}''')
    
    # Remove field declarations (they appear before constructor)
    content = re.sub(r'^\s{4}private final .*;\s*$', '', content, flags=re.MULTILINE)
    content = re.sub(r'^\s{4}private final .*=.*;\s*$', '', content, flags=re.MULTILINE)
    content = re.sub(r'^\s{4}private .*?= .*?;\s*$', '', content, flags=re.MULTILINE)
    
    # Remove constructor
    content = re.sub(
        r'\s{4}public ScenarioEngine\(AINPCPlugin plugin\).*?\n    \}',
        '', content, flags=re.DOTALL
    )
    
    # Method signatures  
    content = re.sub(
        r'^(\s{4})public ([\w.<>\[\],\s]+?) (\w+)\((.*?)\) \{',
        r'\1fun \3(\4): \2 {', content, flags=re.MULTILINE
    )
    content = re.sub(
        r'^(\s{4})public void (\w+)\((.*?)\) \{',
        r'\1fun \2(\3) {', content, flags=re.MULTILINE
    )
    content = re.sub(
        r'^(\s{4})private ([\w.<>\[\],\s]+?) (\w+)\((.*?)\) \{',
        r'\1private fun \3(\4): \2 {', content, flags=re.MULTILINE
    )
    content = re.sub(
        r'^(\s{4})private void (\w+)\((.*?)\) \{',
        r'\1private fun \2(\3) {', content, flags=re.MULTILINE
    )
    
    # Fix basic types
    content = content.replace(': boolean {', ': Boolean {')
    content = content.replace(': int {', ': Int {')
    content = content.replace(': long {', ': Long {')
    content = content.replace(': double {', ': Double {')
    content = content.replace(': float {', ': Float {')
    
    # Plugin getters (SPECIFIC list, avoid regex to prevent false matches)
    plugin_getters = {
        'getDatabaseManager': 'databaseManager',
        'getNpcManager': 'npcManager',
        'getMessageUtils': 'messageUtils',
        'getProgressionService': 'progressionService',
        'getConfig': 'config',
        'getLogger': 'logger',
        'getServer': 'server',
        'getDecisionEngine': 'decisionEngine',
        'getStoryStateService': 'storyStateService',
        'getFeaturePackLoader': 'featurePackLoader',
        'getQuestDirector': 'questDirector',
        'getFamilyManager': 'familyManager',
        'getMemoryManager': 'memoryManager',
        'getDialogueEngine': 'dialogueEngine',
        'getEmotionManager': 'emotionManager',
        'getNpcWorldBindingService': 'npcWorldBindingService',
        'getPlatform': 'platform',
        'getQuestConfig': 'questConfig',
        'getHouseholdPersistenceService': 'householdPersistenceService',
        'getAddonRegistry': 'addonRegistry',
        'getScenarioNpcMatcher': 'scenarioNpcMatcher',
        'getQuestAnchorResolver': 'questAnchorResolver',
    }
    for java_name, kt_name in plugin_getters.items():
        content = content.replace(f'plugin.{java_name}()', f'plugin.{kt_name}')
    
    # Known Kotlin getters -> properties
    getters = [
        'getTemplateId', 'getDisplayName', 'getDescription', 'getHint', 'getSourcePackId',
        'getProgressionEnabled', 'getProgressionKind', 'getProgressionMechanicId',
        'getProgressionLabel', 'getProgressionSingularLabel', 'getProgressionPluralLabel',
        'getProgressionMaxActive', 'getQuestCode', 'getQuestGiverProfession',
        'getQuestPrerequisites', 'getQuestRepeatable', 'getQuestCooldownSeconds',
        'getQuestDialogues', 'getQuestStages', 'getObjectives', 'getRewards',
        'getQuestContract', 'getTriggerProbability', 'getMinimumNpcCount', 'getRequiresPlayer',
        'getNarrativeHints', 'getPreferredTopologies', 'getPhases', 'getRoles', 'getNpcRoles',
        'getId', 'getName', 'getType', 'getBaseType', 'getAmount', 'getItemId',
        'getCompletionMode', 'getObjectiveIds', 'getPayload', 'getVariables', 'getBounds',
        'getOccupation', 'getDatabaseId', 'getUuid', 'getLocation', 'getMaxActive',
        'getActionBarMessage', 'getWorldAdmin', 'getWorldAdminService',
    ]
    for g in getters:
        content = content.replace(f'.{g}()', f'.{g[3].lower()}{g[4:]}')
    
    # Known Kotlin setters -> property assignment
    setters = [
        'setTemplateId', 'setDisplayName', 'setDescription', 'setHint', 'setSourcePackId',
        'setProgressionEnabled', 'setProgressionMechanicId', 'setProgressionKind',
        'setProgressionLabel', 'setProgressionSingularLabel', 'setProgressionPluralLabel',
        'setProgressionMaxActive', 'setQuestCode', 'setQuestGiverProfession',
        'setQuestPrerequisites', 'setQuestRepeatable', 'setQuestCooldownSeconds',
        'setQuestDialogues', 'setQuestStages', 'setObjectives', 'setRewards',
        'setQuestContract', 'setTriggerProbability', 'setMinimumNpcCount', 'setRequiresPlayer',
        'setNarrativeHints', 'setPreferredTopologies', 'setRequiredProfessions',
        'setPreferredProfessions', 'setRequiredTraits', 'setPreferredTraits',
    ]
    for s in setters:
        prop = s[3].lower() + s[4:]
        content = content.replace(f'.{s}(', f'.{prop} = (')
    
    # Boolean isXxx() -> .xxx
    content = re.sub(r'\.is(\w+)\(\)', lambda m: f'.{m.group(1)[0].lower()}{m.group(1)[1:]}', content)
    
    # Basic patterns
    content = re.sub(r'instanceof (\w+)', r'is \1', content)
    content = re.sub(r'catch\s*\(\s*(\w+)\s+(\w+)\s*\)', r'catch (\2: \1)', content)
    content = content.replace('throw new ', 'throw ')
    content = content.replace('this.', '')
    content = content.replace('List.copyOf(', 'toList(')
    content = content.replace('List.of(', 'listOf(')
    
    # For each line: remove trailing semicolon (only if it's just a semicolon, not part of for(;;))
    lines = content.split('\n')
    result_lines = []
    for line in lines:
        if line.rstrip().endswith(';') and 'for (' not in line:
            line = line.rstrip()[:-1]
        result_lines.append(line)
    content = '\n'.join(result_lines)
    
    # Clean up blank lines
    content = re.sub(r'\n\n\n+', '\n\n', content)
    
    # Add imports at top
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
    
    return content

if __name__ == '__main__':
    with open(sys.argv[1], 'r', encoding='utf-8') as f:
        content = f.read()
    result = safe_convert(content)
    with open(sys.argv[2], 'w', encoding='utf-8', newline='\r\n') as f:
        f.write(result)
    print(f"Done: {len(result.splitlines())} lines")
