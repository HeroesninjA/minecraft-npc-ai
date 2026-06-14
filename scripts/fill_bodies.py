#!/usr/bin/env python3
"""Extract Java method bodies, convert to Kotlin, fill into stub."""
import subprocess, re

# 1. Read Java source
with open(r'C:\Users\HeroesninjA\AppData\Local\Temp\ScenarioEngine_java.java', 'r', encoding='utf-16') as f:
    java_source = f.read()
java_lines = java_source.split('\n')

# 2. Find all method declarations with their braces
def find_method_body(lines, start_idx):
    """Find the body of a method starting at start_idx (line with '{')."""
    depth = 0
    in_block = False
    body_start = start_idx
    for i in range(start_idx, len(lines)):
        for c in lines[i]:
            if c == '{':
                depth += 1
                in_block = True
            elif c == '}':
                depth -= 1
        if in_block and depth == 0:
            return body_start, i  # return (start_line, end_line)
    return start_idx, len(lines) - 1

# Method regex
method_pat = re.compile(
    r'^\s*(?:public|private|protected)?\s*(?:static\s+)?(?:final\s+)?(?:<[^>]+>\s+)?'
    r'([\w.<>\[\],\s]+?)\s+'  # return type
    r'(\w+)\s*'                # method name
    r'\(([^)]*)\)\s*'          # params
    r'(?:throws\s+[\w\s,]+)?\s*'
    r'\{'
)

methods = []
for i, line in enumerate(java_lines):
    m = method_pat.match(line.strip())
    if not m: continue
    ret_type = m.group(1).strip()
    name = m.group(2)
    params_str = m.group(3)
    if name == 'ScenarioEngine': continue
    if 'class' in line.strip().split()[:2]: continue
    
    # Count params
    param_count = 0
    if params_str.strip():
        param_count = len([p for p in params_str.split(',') if p.strip()])
    
    start, end = find_method_body(java_lines, i)
    body_lines = java_lines[start:end+1]
    body_text = '\n'.join(body_lines)
    
    methods.append({
        'name': name,
        'param_count': param_count,
        'body': body_text,
        'ret_type': ret_type,
    })

print(f'Found {len(methods)} methods in Java source')

# 3. Conversion patterns
def convert_body(b):
    b = b.replace('plugin.getDatabaseManager()', 'plugin.databaseManager')
    b = b.replace('plugin.getNpcManager()', 'plugin.npcManager')
    b = b.replace('plugin.getMessageUtils()', 'plugin.messageUtils')
    b = b.replace('plugin.getProgressionService()', 'plugin.progressionService')
    b = b.replace('plugin.getConfig()', 'plugin.config')
    b = b.replace('plugin.getLogger()', 'plugin.logger')
    b = b.replace('plugin.getServer()', 'plugin.server')
    b = b.replace('plugin.getDecisionEngine()', 'plugin.decisionEngine')
    b = b.replace('plugin.getStoryStateService()', 'plugin.storyStateService')
    b = b.replace('plugin.getFeaturePackLoader()', 'plugin.featurePackLoader')
    b = b.replace('plugin.getQuestDirector()', 'plugin.questDirector')
    b = b.replace('plugin.getFamilyManager()', 'plugin.familyManager')
    b = b.replace('plugin.getMemoryManager()', 'plugin.memoryManager')
    b = b.replace('plugin.getDialogueEngine()', 'plugin.dialogueEngine')
    b = b.replace('plugin.getEmotionManager()', 'plugin.emotionManager')
    b = b.replace('plugin.getNpcWorldBindingService()', 'plugin.npcWorldBindingService')
    b = b.replace('plugin.getPlatform()', 'plugin.platform')
    b = b.replace('plugin.getQuestConfig()', 'plugin.questConfig')
    b = b.replace('plugin.getHouseholdPersistenceService()', 'plugin.householdPersistenceService')
    b = b.replace('plugin.getAddonRegistry()', 'plugin.addonRegistry')
    
    # Common patterns
    b = re.sub(r'instanceof (\w+)', r'is \1', b)
    b = re.sub(r'catch\s*\(\s*(\w+)\s+(\w+)\s*\)', r'catch (\2: \1)', b)
    b = b.replace('throw new ', 'throw ')
    b = b.replace('this.', '')
    b = b.replace('@Override\n', '')
    b = b.replace('@Override', '')
    b = b.replace('List.copyOf(', 'toList(')
    b = b.replace('List.of(', 'listOf(')
    b = re.sub(r'new\s+([A-Z]\w+)\(', r'\1(', b)
    
    # Enhanced for
    b = re.sub(r'for\s*\(\s*(?:final\s+)?[\w.<>\[\]]+\s+(\w+)\s*:\s*(.+?)\)', r'for (\1 in \2)', b)
    
    # Try with resources: try (Type var = expr) { -> expr.use { var ->
    b = re.sub(r'try\s*\(\s*[\w.<>\[\]]+\s+(\w+)\s*=\s*(.+?)\)\s*\{', r'\2.use { \1 ->', b)
    
    return b

# 4. Read Kotlin stub
with open(r'ainpc-core-plugin\src\main\kotlin\ro\ainpc\engine\ScenarioEngine.kt', 'r', encoding='utf-8') as f:
    kotlin_source = f.read()
kotlin_lines = kotlin_source.split('\n')

# 5. Match methods and replace bodies
# Find all Kotlin method declarations
kt_method_pat = re.compile(r'^\s*(?:private\s+)?fun\s+(?:<[^>]+>\s+)?(\w+)\s*\(([^)]*)\)')
kt_methods = []
for i, line in enumerate(kotlin_lines):
    m = kt_method_pat.match(line)
    if m:
        name = m.group(1)
        params = m.group(2).strip()
        param_count = len([p for p in params.split(',') if p.strip()]) if params else 0
        kt_methods.append({'line': i, 'name': name, 'param_count': param_count})

print(f'Found {len(kt_methods)} methods in Kotlin stub')

# 6. For each Kotlin method, find matching Java body and replace
replacements = 0
for kt_m in kt_methods:
    # Find matching Java method by name and param count
    matches = [jm for jm in methods if jm['name'] == kt_m['name'] and jm['param_count'] == kt_m['param_count']]
    if not matches:
        # Try just by name (different overload)
        matches = [jm for jm in methods if jm['name'] == kt_m['name']]
    
    if matches:
        jm = matches[0]
        converted = convert_body(jm['body'])
        # Replace the TODO() with the actual body
        # Find the TODO() line after this method declaration
        for j in range(kt_m['line'] + 1, min(kt_m['line'] + 5, len(kotlin_lines))):
            if 'TODO()' in kotlin_lines[j]:
                indent = '        '  # 8 spaces for body
                # Convert the body to properly indented lines
                body_lines = converted.split('\n')
                # First line is the method declaration (skip it)
                # Last line is the closing } (skip it)
                inner_lines = body_lines[1:-1] if len(body_lines) > 2 else []
                
                # Fix indentation: original Java uses 8 spaces, keep as-is
                # Replace the TODO() line with the body content
                if inner_lines:
                    kotlin_lines[j] = inner_lines[0] if inner_lines else kotlin_lines[j]
                    # Insert remaining body lines
                    for k, bl in enumerate(inner_lines[1:], 1):
                        kotlin_lines.insert(j + k, bl)
                replacements += 1
                break

print(f'Replaced {replacements} method bodies')

# 7. Write updated Kotlin file
with open(r'ainpc-core-plugin\src\main\kotlin\ro\ainpc\engine\ScenarioEngine.kt', 'w', encoding='utf-8', newline='\r\n') as f:
    f.write('\n'.join(kotlin_lines))

print(f'Written {len(kotlin_lines)} lines')
