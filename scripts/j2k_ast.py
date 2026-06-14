#!/usr/bin/env python3
"""Java to Kotlin AST-level converter using javalang."""
import javalang
import sys, re

def java_type_to_kotlin(jtype):
    """Convert Java type string to Kotlin type string."""
    if jtype is None:
        return "Unit"
    s = str(jtype)
    s = s.replace("boolean", "Boolean")
    s = s.replace("int", "Int")
    s = s.replace("long", "Long")
    s = s.replace("double", "Double")
    s = s.replace("float", "Float")
    s = s.replace("void", "Unit")
    return s

def convert_method(method, is_private=False):
    """Convert a Java method declaration to Kotlin."""
    vis = "private " if is_private else ""
    name = method.name
    params = []
    for p in method.parameters:
        ptype = java_type_to_kotlin(p.type)
        params.append(f"{p.name}: {ptype}")
    param_str = ", ".join(params)
    
    ret = java_type_to_kotlin(method.return_type)
    
    if ret == "Unit":
        return f"    {vis}fun {name}({param_str}) {{"
    else:
        return f"    {vis}fun {name}({param_str}): {ret} {{"

def getter_to_property(name):
    """Convert getXxx() to property name xxx."""
    if name.startswith("get"):
        rest = name[3:]
        return rest[0].lower() + rest[1:]
    return name

def setter_to_property(name):
    """Convert setXxx() to xxx."""
    if name.startswith("set"):
        rest = name[3:]
        return rest[0].lower() + rest[1:]
    return name

# === COMMON KOTLIN PROPERTIES FOR KNOWN TYPES ===
KOTLIN_PROPERTIES = {
    # ScenarioTemplate
    'getTemplateId': 'templateId', 'setTemplateId': 'templateId',
    'getDisplayName': 'displayName', 'setDisplayName': 'displayName',
    'getDescription': 'description', 'setDescription': 'description',
    'getHint': 'hint', 'setHint': 'hint',
    'getTriggerProbability': 'triggerProbability', 'setTriggerProbability': 'triggerProbability',
    'getMinimumNpcCount': 'minimumNpcCount', 'setMinimumNpcCount': 'minimumNpcCount',
    'getRequiresPlayer': 'requiresPlayer', 'setRequiresPlayer': 'requiresPlayer',
    'getQuestCode': 'questCode', 'setQuestCode': 'questCode',
    'getObjectives': 'objectives', 'setObjectives': 'objectives',
    'getQuestStages': 'questStages', 'setQuestStages': 'questStages',
    'getRewards': 'rewards', 'setRewards': 'rewards',
    'getProgressionEnabled': 'progressionEnabled', 'setProgressionEnabled': 'progressionEnabled',
    'getProgressionKind': 'progressionKind', 'setProgressionKind': 'progressionKind',
    'getProgressionMechanicId': 'progressionMechanicId', 'setProgressionMechanicId': 'progressionMechanicId',
    'getProgressionLabel': 'progressionLabel', 'setProgressionLabel': 'progressionLabel',
    'isQuestRepeatable': 'questRepeatable', 'setQuestRepeatable': 'questRepeatable',
    'getQuestRepeatable': 'questRepeatable',
    'isProgressionEnabled': 'progressionEnabled',
    'isOptional': 'optional', 'isPlayerRole': 'playerRole',
    'getId': 'id', 'setId': 'id',
    'getName': 'name', 'getType': 'type',
    'getAmount': 'amount', 'getItemId': 'itemId',
    'getUuid': 'uuid', 'getLocation': 'location',
    'getDatabaseId': 'databaseId', 'getOccupation': 'occupation',
    # Plugin getters
    'getDatabaseManager': 'databaseManager',
    'getNpcManager': 'npcManager',
    'getMessageUtils': 'messageUtils',
    'getProgressionService': 'progressionService',
    'getConfig': 'config',
    'getLogger': 'logger',
    'getServer': 'server',
}

# === PLUGIN PROPERTY FIX ===
PLUGIN_PREFIX = "plugin.get"
PLUGIN_PROPERTIES = {
    'DatabaseManager': 'databaseManager',
    'NpcManager': 'npcManager',
    'MessageUtils': 'messageUtils',
    'ProgressionService': 'progressionService',
    'Config': 'config',
    'Logger': 'logger',
    'Server': 'server',
    'DecisionEngine': 'decisionEngine',
    'StoryStateService': 'storyStateService',
    'FeaturePackLoader': 'featurePackLoader',
    'QuestDirector': 'questDirector',
    'FamilyManager': 'familyManager',
    'MemoryManager': 'memoryManager',
    'DialogueEngine': 'dialogueEngine',
    'EmotionManager': 'emotionManager',
    'NpcWorldBindingService': 'npcWorldBindingService',
    'Platform': 'platform',
    'QuestConfig': 'questConfig',
    'HouseholdPersistenceService': 'householdPersistenceService',
    'AddonRegistry': 'addonRegistry',
    'ScenarioNpcMatcher': 'scenarioNpcMatcher',
    'QuestAnchorResolver': 'questAnchorResolver',
    'DecisionIntentResolver': 'decisionIntentResolver',
    'DependencyValidator': 'dependencyValidator',
    'MetadataValidator': 'metadataValidator',
}

def convert_file(input_path, output_path):
    with open(input_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    result = []
    
    # Package
    result.append("package ro.ainpc.engine")
    result.append("")
    
    # Imports
    result.append("import com.google.gson.Gson")
    result.append("import org.bukkit.Location")
    result.append("import org.bukkit.entity.Player")
    result.append("import ro.ainpc.AINPCPlugin")
    result.append("import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition")
    result.append("import ro.ainpc.story.StoryContextService")
    result.append("import java.sql.SQLException")
    result.append("import java.util.*")
    result.append("import java.util.concurrent.ConcurrentHashMap")
    result.append("")
    
    # Class declaration with fields
    result.append("class ScenarioEngine(")
    result.append("    private val plugin: AINPCPlugin")
    result.append(") {")
    result.append("    private val scenarioTemplates: MutableMap<String, ScenarioTemplate> = LinkedHashMap()")
    result.append("    private val questTemplates: MutableMap<String, ScenarioTemplate> = LinkedHashMap()")
    result.append("    private val gson: Gson = Gson()")
    result.append("    private val activePlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>> = ConcurrentHashMap()")
    result.append("    private val archivedPlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>> = ConcurrentHashMap()")
    result.append("    private val questCompletionLocks: MutableMap<UUID, String> = ConcurrentHashMap()")
    result.append("    val questDefinitions: MutableList<ScenarioDefinition> = ArrayList()")
    result.append("    var storyContextService: StoryContextService? = null")
    result.append("    private val trackedQuestPlayers: MutableSet<UUID> = HashSet()")
    result.append("    private val trackedQuestTemplates: MutableMap<UUID, String> = ConcurrentHashMap()")
    result.append("")
    result.append("    init {")
    result.append("    }")
    result.append("")
    
    # Process the Java content line by line with smart replacements
    lines = content.split('\n')
    i = 0
    in_class = False
    skip_until_class_end = False
    
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        
        # Skip package, imports
        if stripped.startswith('package ') or stripped.startswith('import '):
            i += 1
            continue
        
        # Skip empty lines before class
        if not in_class and not stripped:
            i += 1
            continue
        
        # Class declaration - skip, we already have it
        if 'public class ScenarioEngine' in stripped:
            in_class = True
            i += 1
            # Skip fields
            while i < len(lines):
                sl = lines[i].strip()
                if sl.startswith('private final ') or sl.startswith('private ') or sl.startswith('public final ') or sl.startswith('public '):
                    if '=' in sl and ';' in sl and not '(' in sl:
                        i += 1
                        continue
                    else:
                        break
                else:
                    break
            continue
        
        # Skip constructor
        if 'public ScenarioEngine(AINPCPlugin plugin)' in stripped:
            depth = 1
            i += 1
            while i < len(lines) and depth > 0:
                for c in lines[i]:
                    if c == '{': depth += 1
                    elif c == '}': depth -= 1
                i += 1
            continue
        
        if not in_class:
            i += 1
            continue
        
        # Process method-like lines
        # Remove @Override
        if stripped == '@Override':
            i += 1
            continue
        
        # Process the line with replacements
        processed = line
        
        if in_class:
            # Method declarations
            processed = re.sub(
                r'^\s{4}public ([\w.<>\[\],\s]+?) (\w+)\((.*?)\) \{',
                lambda m: f"    fun {m.group(2)}({m.group(3)}): {java_type_to_kotlin(m.group(1))} {{",
                processed
            )
            processed = re.sub(
                r'^\s{4}public void (\w+)\((.*?)\) \{',
                r'    fun \1(\2) {',
                processed
            )
            processed = re.sub(
                r'^\s{4}private ([\w.<>\[\],\s]+?) (\w+)\((.*?)\) \{',
                lambda m: f"    private fun {m.group(2)}({m.group(3)}): {java_type_to_kotlin(m.group(1))} {{",
                processed
            )
            processed = re.sub(
                r'^\s{4}private void (\w+)\((.*?)\) \{',
                r'    private fun \1(\2) {',
                processed
            )
            
            # Getters
            for java_name, kt_name in KOTLIN_PROPERTIES.items():
                processed = processed.replace(f'.{java_name}()', f'.{kt_name}')
            
            # Plugin getters
            for java_name, kt_name in PLUGIN_PROPERTIES.items():
                processed = processed.replace(f'plugin.get{java_name}()', f'plugin.{kt_name}')
            
            # Setters
            for java_name, kt_name in KOTLIN_PROPERTIES.items():
                if java_name.startswith('set'):
                    processed = processed.replace(f'.{java_name}(', f'.{kt_name} = (')
            
            # Boolean isXxx
            processed = re.sub(r'\.(is\w+)\(\)', lambda m: f'.{m.group(1)[2].lower()}{m.group(1)[3:]}', processed)
            
            # Other patterns
            processed = re.sub(r'instanceof (\w+)', r'is \1', processed)
            processed = re.sub(r'catch\s*\(\s*(\w+)\s+(\w+)\s*\)', r'catch (\2: \1)', processed)
            processed = processed.replace('throw new ', 'throw ')
            processed = processed.replace('this.plugin', 'plugin')
            processed = processed.replace('List.copyOf(', 'toList(')
            processed = processed.replace('List.of(', 'listOf(')
            
            # Remove ;
            processed = processed.rstrip()
            if processed.endswith(';'):
                processed = processed[:-1]
        
        result.append(processed)
        i += 1
    
    # Close class
    result.append("}")
    
    # Write
    with open(output_path, 'w', encoding='utf-8', newline='\r\n') as f:
        f.write('\n'.join(result))
    
    print(f"Converted: {len(result)} lines")

if __name__ == '__main__':
    convert_file(sys.argv[1], sys.argv[2])
