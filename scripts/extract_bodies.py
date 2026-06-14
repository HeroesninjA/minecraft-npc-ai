#!/usr/bin/env python3
"""Extract and convert method bodies from Java source."""
import subprocess, re

# Get Java source from git
result = subprocess.run(['git', 'show', 'HEAD:ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java'],
                       capture_output=True, text=True, encoding='utf-8')
java_source = result.stdout
lines = java_source.split('\n')

# Find all method declarations with their line numbers
method_starts = []
for i, line in enumerate(lines):
    stripped = line.strip()
    # Match method declaration: visibility? return_type name(params) {
    m = re.match(r'^\s*(?:public|private|protected)?\s*(?:static\s+)?(?:<[^>]+>\s+)?[\w.<>\[\],\s]+\s+(\w+)\s*\([^)]*\)\s*(?:throws\s+[\w\s,]+)?\s*\{', stripped)
    if m and not stripped.startswith('//') and not stripped.startswith('/*'):
        name = m.group(1)
        # Skip class declaration and constructor
        if name != 'ScenarioEngine' and not stripped.startswith('class '):
            method_starts.append((i, name, stripped))

# For each method, extract its body (from { to matching })
def find_matching_brace(lines, start_line):
    """Find the line with the matching closing brace."""
    depth = 0
    for i in range(start_line, len(lines)):
        for c in lines[i]:
            if c == '{': depth += 1
            elif c == '}': depth -= 1
        if depth == 0:
            return i
    return len(lines) - 1

method_bodies = {}
for start_line, name, decl in method_starts:
    end_line = find_matching_brace(lines, start_line)
    # Extract body lines (excluding the opening { line and closing } line)
    body_lines = lines[start_line:end_line+1]
    body_text = '\n'.join(body_lines)
    method_bodies[(start_line, name)] = body_text

print(f'Found {len(method_bodies)} methods')
# Show first few
for (line_no, name), body in list(method_bodies.items())[:10]:
    print(f'  Line {line_no}: {name} ({len(body.splitlines())} lines)')

# Now convert each body
def convert_body(body):
    """Apply Java->Kotlin conversions to method body."""
    b = body
    
    # Plugin getters (preserve case sensitivity)
    plugin_getters = {
        'getDatabaseManager': 'databaseManager', 'getNpcManager': 'npcManager',
        'getMessageUtils': 'messageUtils', 'getProgressionService': 'progressionService',
        'getConfig': 'config', 'getLogger': 'logger', 'getServer': 'server',
        'getDecisionEngine': 'decisionEngine', 'getStoryStateService': 'storyStateService',
        'getFeaturePackLoader': 'featurePackLoader', 'getQuestDirector': 'questDirector',
        'getFamilyManager': 'familyManager', 'getMemoryManager': 'memoryManager',
        'getDialogueEngine': 'dialogueEngine', 'getEmotionManager': 'emotionManager',
        'getNpcWorldBindingService': 'npcWorldBindingService', 'getPlatform': 'platform',
        'getQuestConfig': 'questConfig', 'getHouseholdPersistenceService': 'householdPersistenceService',
        'getAddonRegistry': 'addonRegistry', 'getScenarioNpcMatcher': 'scenarioNpcMatcher',
        'getQuestAnchorResolver': 'questAnchorResolver',
    }
    for j, k in plugin_getters.items():
        b = b.replace(f'plugin.{j}()', f'plugin.{k}')
    
    # Known getters -> properties
    known_getters = [
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
    for g in known_getters:
        b = b.replace(f'.{g}()', f'.{g[3].lower()}{g[4:]}')
    
    # Known setters -> properties
    known_setters = [
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
    for s in known_setters:
        prop = s[3].lower() + s[4:]
        b = b.replace(f'.{s}(', f'.{prop} = (')
    
    # Boolean isXxx() -> .xxx
    b = re.sub(r'\.is(\w+)\(\)', lambda m: f'.{m.group(1)[0].lower()}{m.group(1)[1:]}', b)
    
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
    
    # Enhanced for
    b = re.sub(r'for\s*\(\s*(?:final\s+)?[\w.<>\[\]]+\s+(\w+)\s*:\s*(.+?)\)',
               r'for (\1 in \2)', b)
    
    # Integer for loop: for (int i = 0; i < N; i++)
    b = re.sub(r'for\s*\(\s*int\s+(\w+)\s*=\s*0\s*;\s*\1\s*<\s*(\w+)\s*;\s*\1\+\+\)',
               r'for (\1 in 0 until \2)', b)
    
    # Try with resources
    b = re.sub(r'try\s*\(\s*[\w.<>\[\]]+\s+(\w+)\s*=\s*(.+?)\)\s*\{',
               r'\2.use { \1 ->', b)
    
    # Lambda: (args) -> expr
    b = re.sub(r'\((\w*)\)\s*->\s*', r'{ \1 -> ', b)
    
    # new Type()
    b = re.sub(r'new\s+([A-Z]\w+)\(', r'\1(', b)
    
    # new package.Type()
    b = re.sub(r'new\s+[\w.]+\.(\w+)\(', r'\1(', b)
    
    # Remove @Override
    b = b.replace('@Override\n', '')
    b = b.replace('@Override', '')
    
    return b

# Save all converted bodies
output = []
output.append('# Converted method bodies from Java')
output.append('')

for (line_no, name), body in method_bodies.items():
    converted = convert_body(body)
    output.append(f'# === {name} (line {line_no}) ===')
    output.append(converted)
    output.append('')

with open(r'C:\Users\HeroesninjA\AppData\Local\Temp\bodies.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(output))

print(f'\nSaved {len(method_bodies)} method bodies')
