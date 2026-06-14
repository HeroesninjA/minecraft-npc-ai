#!/usr/bin/env python3
"""Parse Java with javalang, convert method by method."""
import javalang, re, sys

def java_to_kotlin_type(jtype_str):
    """Convert Java type string to Kotlin."""
    if jtype_str is None or jtype_str == 'void':
        return None
    s = str(jtype_str)
    return s.replace('boolean', 'Boolean').replace('int', 'Int').replace('long', 'Long').replace('double', 'Double').replace('float', 'Float')

FIXED_GETTERS = {
    'getTemplateId':'templateId','getDisplayName':'displayName','getDescription':'description',
    'getHint':'hint','getSourcePackId':'sourcePackId','getProgressionEnabled':'progressionEnabled',
    'getProgressionKind':'progressionKind','getProgressionMechanicId':'progressionMechanicId',
    'getProgressionLabel':'progressionLabel','getProgressionSingularLabel':'progressionSingularLabel',
    'getProgressionPluralLabel':'progressionPluralLabel','getProgressionMaxActive':'progressionMaxActive',
    'getQuestCode':'questCode','getQuestGiverProfession':'questGiverProfession',
    'getQuestPrerequisites':'questPrerequisites','getQuestRepeatable':'questRepeatable',
    'getQuestCooldownSeconds':'questCooldownSeconds','getQuestDialogues':'questDialogues',
    'getQuestStages':'questStages','getObjectives':'objectives','getRewards':'rewards',
    'getQuestContract':'questContract','getTriggerProbability':'triggerProbability',
    'getMinimumNpcCount':'minimumNpcCount','getRequiresPlayer':'requiresPlayer',
    'getNarrativeHints':'narrativeHints','getPreferredTopologies':'preferredTopologies',
    'getPhases':'phases','getRoles':'roles','getNpcRoles':'npcRoles','getPlayerRoles':'playerRoles',
    'getId':'id','getName':'name','getType':'type','getBaseType':'baseType','getAmount':'amount',
    'getItemId':'itemId','getCompletionMode':'completionMode','getObjectiveIds':'objectiveIds',
    'getPayload':'payload','getVariables':'variables','getBounds':'bounds',
    'getOccupation':'occupation','getDatabaseId':'databaseId','getUuid':'uuid',
    'getLocation':'location','getMaxActive':'maxActive','getActionBarMessage':'actionBarMessage',
    'getWorldAdmin':'worldAdmin','getWorldAdminService':'worldAdminService',
    'getScenarios':'scenarios','getValues':'values',
}

FIXED_SETTERS = {
    'setTemplateId':'templateId','setDisplayName':'displayName','setDescription':'description',
    'setHint':'hint','setSourcePackId':'sourcePackId','setProgressionEnabled':'progressionEnabled',
    'setProgressionMechanicId':'progressionMechanicId','setProgressionKind':'progressionKind',
    'setProgressionLabel':'progressionLabel','setProgressionSingularLabel':'progressionSingularLabel',
    'setProgressionPluralLabel':'progressionPluralLabel','setProgressionMaxActive':'progressionMaxActive',
    'setQuestCode':'questCode','setQuestGiverProfession':'questGiverProfession',
    'setQuestPrerequisites':'questPrerequisites','setQuestRepeatable':'questRepeatable',
    'setQuestCooldownSeconds':'questCooldownSeconds','setQuestDialogues':'questDialogues',
    'setQuestStages':'questStages','setObjectives':'objectives','setRewards':'rewards',
    'setQuestContract':'questContract','setTriggerProbability':'triggerProbability',
    'setMinimumNpcCount':'minimumNpcCount','setRequiresPlayer':'requiresPlayer',
    'setNarrativeHints':'narrativeHints','setPreferredTopologies':'preferredTopologies',
    'setRequiredProfessions':'requiredProfessions','setPreferredProfessions':'preferredProfessions',
    'setRequiredTraits':'requiredTraits','setPreferredTraits':'preferredTraits',
    'setId':'id','setOccupation':'occupation','setEnginePlugin':'enginePlugin',
}

PLUGIN_GETTERS = {
    'getDatabaseManager':'databaseManager','getNpcManager':'npcManager',
    'getMessageUtils':'messageUtils','getProgressionService':'progressionService',
    'getConfig':'config','getLogger':'logger','getServer':'server',
    'getDecisionEngine':'decisionEngine','getStoryStateService':'storyStateService',
    'getFeaturePackLoader':'featurePackLoader','getQuestDirector':'questDirector',
    'getFamilyManager':'familyManager','getMemoryManager':'memoryManager',
    'getDialogueEngine':'dialogueEngine','getEmotionManager':'emotionManager',
    'getNpcWorldBindingService':'npcWorldBindingService','getPlatform':'platform',
    'getQuestConfig':'questConfig','getHouseholdPersistenceService':'householdPersistenceService',
    'getAddonRegistry':'addonRegistry','getScenarioNpcMatcher':'scenarioNpcMatcher',
    'getQuestAnchorResolver':'questAnchorResolver','getDecisionIntentResolver':'decisionIntentResolver',
    'getDependencyValidator':'dependencyValidator','getMetadataValidator':'metadataValidator',
}

def convert_body(body_text):
    """Convert Java method body to Kotlin using regex patterns."""
    b = body_text
    
    # Plugin getters
    for j, k in PLUGIN_GETTERS.items():
        b = b.replace(f'plugin.{j}()', f'plugin.{k}')
    
    # Fixed getters
    for j, k in FIXED_GETTERS.items():
        b = b.replace(f'.{j}()', f'.{k}')
    
    # Fixed setters
    for j, k in FIXED_SETTERS.items():
        b = b.replace(f'.{j}(', f'.{k} = (')
    
    # Boolean isXxx()
    b = re.sub(r'\.(is\w+)\(\)', lambda m: f'.{m.group(1)[2].lower()}{m.group(1)[3:]}', b)
    
    # instanceof
    b = re.sub(r'instanceof (\w+)', r'is \1', b)
    
    # catch
    b = re.sub(r'catch\s*\(\s*(\w+)\s+(\w+)\s*\)', r'catch (\2: \1)', b)
    
    # throw new
    b = b.replace('throw new ', 'throw ')
    
    # this.
    b = b.replace('this.', '')
    
    # List.copyOf, List.of
    b = b.replace('List.copyOf(', 'toList(')
    b = b.replace('List.of(', 'listOf(')
    
    # Enhanced for: for (Type var : collection) -> for (var in collection)
    b = re.sub(r'for\s*\(\s*(?:final\s+)?[\w.<>\[\]]+\s+(\w+)\s*:\s*(.+?)\)',
               r'for (\1 in \2)', b)
    
    # For loop: for (int i = 0; i < N; i++) -> for (i in 0 until N)
    b = re.sub(r'for\s*\(\s*int\s+(\w+)\s*=\s*0\s*;\s*\1\s*<\s*(\w+)\s*;\s*\1\+\+\)',
               r'for (\1 in 0 until \2)', b)
    
    # Try with resources: try (Type var = expr) { -> expr.use { var ->
    b = re.sub(r'try\s*\(\s*[\w.<>\[\]]+\s+(\w+)\s*=\s*(.+?)\)\s*\{',
               r'\2.use { \1 ->', b)
    
    # lambda: (args) -> expr -> { args -> expr }
    b = re.sub(r'\((\w*)\)\s*->\s*', r'{ \1 -> ', b)
    
    # new Type() -> Type()
    b = re.sub(r'new\s+([A-Z]\w+)\(', r'\1(', b)
    
    # Remove @Override
    b = b.replace('@Override\n', '')
    
    return b

def convert_file(input_path, output_path):
    with open(input_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Parse with javalang
    try:
        tree = javalang.parse.parse(content)
    except:
        print("Parse failed, falling back to line-based")
        # Line-based fallback - just do regex on the whole content
        result = []
        result.append("package ro.ainpc.engine\n")
        result.append("import com.google.gson.Gson; import org.bukkit.Location; import org.bukkit.entity.Player; import ro.ainpc.AINPCPlugin; import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition; import ro.ainpc.story.StoryContextService; import java.sql.SQLException; import java.util.*; import java.util.concurrent.ConcurrentHashMap\n")
        result.append("class ScenarioEngine(private val plugin: AINPCPlugin) {\n")
        result.append("private val scenarioTemplates = LinkedHashMap<String, ScenarioTemplate>(); private val questTemplates = LinkedHashMap<String, ScenarioTemplate>(); private val gson = Gson(); private val activePlayerQuests = ConcurrentHashMap<UUID, MutableMap<String, PlayerQuestProgress>>(); val questDefinitions = ArrayList<ScenarioDefinition>(); var storyContextService: StoryContextService? = null; init {}\n")
        
        body = convert_body(content)
        result.append(body)
        result.append("}\n")
        
        with open(output_path, 'w', encoding='utf-8', newline='\r\n') as f:
            f.write('\n'.join(result))
        print("Fallback conversion done")
        return
    
    # Process AST
    lines = content.split('\n')
    result = []
    
    # Header
    result.append("package ro.ainpc.engine")
    result.append("")
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
    
    # Class
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
    result.append("    init {}")
    result.append("")
    
    # Process each method
    for path, node in tree.filter(javalang.tree.MethodDeclaration):
        if hasattr(node, '_position') and node._position:
            start_line = node._position.line - 1
            # Find the body
            if node.body:
                # Get the first line of the body
                body_text = None
                for stmt_line in range(start_line, min(start_line + 5, len(lines))):
                    if '{' in lines[stmt_line]:
                        # Find matching closing brace by counting
                        depth = 0
                        body_start = stmt_line
                        for close_line in range(body_start, len(lines)):
                            for c in lines[close_line]:
                                if c == '{': depth += 1
                                elif c == '}': depth -= 1
                            if depth == 0:
                                body_lines = lines[body_start:close_line+1]
                                body_text = '\n'.join(body_lines)
                                break
                        break
                
                if body_text:
                    # Convert body
                    converted_body = convert_body(body_text)
                    
                    # Build method declaration
                    modifiers = 'private ' if 'private' in (node.modifiers or []) else ''
                    name = node.name
                    params = []
                    for p in node.parameters:
                        ptype = java_to_kotlin_type(p.type) or 'Any'
                        params.append(f"{p.name}: {ptype}")
                    param_str = ', '.join(params)
                    ret = java_to_kotlin_type(node.return_type)
                    
                    if ret:
                        result.append(f"    {modifiers}fun {name}({param_str}): {ret} {converted_body}")
                    else:
                        result.append(f"    {modifiers}fun {name}({param_str}) {converted_body}")
                    result.append("")
    
    result.append("}")
    
    with open(output_path, 'w', encoding='utf-8', newline='\r\n') as f:
        f.write('\n'.join(result))
    print(f"AST conversion done: {len(result)} lines")

if __name__ == '__main__':
    convert_file(sys.argv[1], sys.argv[2])
