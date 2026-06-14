#!/usr/bin/env python3
"""Generate Kotlin stub from javap output - fixed version."""
import subprocess, re, sys, os, glob

javap = r"C:\Program Files\Java\jdk-25.0.2\bin\javap.exe"
classfile = None
for root, dirs, files in os.walk(r"ainpc-core-plugin\build"):
    for f in files:
        if f == "ScenarioEngine.class":
            classfile = os.path.join(root, f)
            break
    if classfile: break

if not classfile:
    print("Class file not found!"); sys.exit(1)

result = subprocess.run([javap, "-p", classfile], capture_output=True, text=True)
output = result.stdout

# Parse methods
all_methods = []
for line in output.split('\n'):
    line = line.strip()
    if not line or '{' in line or '}' in line or 'Compiled from' in line:
        continue
    
    # Parse: [private|public|protected] [static] return_type name(params);
    match = re.match(
        r'(?:(private|public|protected)\s+)?'  # visibility
        r'(?:(static)\s+)?'                     # static
        r'((?:[\w.<>\[\],\s])+?)\s+'           # return type
        r'(\w+)\('                              # name
        r'([^)]*)'                              # params
        r'\)\s*;', 
        line
    )
    if not match:
        continue
    
    vis = match.group(1) or 'public'
    is_static = bool(match.group(2))
    ret_type_raw = match.group(3).strip()
    name = match.group(4)
    params_str = match.group(5)
    
    # Skip constructor and static initializer
    if name == 'ScenarioEngine': continue
    
    # Parse parameters
    params = []
    if params_str.strip():
        for p in params_str.split(','):
            p = p.strip()
            parts = p.rsplit(' ', 1)
            if len(parts) == 2:
                ptype, pname = parts
                params.append((ptype.strip(), pname.strip()))
    
    all_methods.append({
        'name': name,
        'return_type': ret_type_raw,
        'params': params,
        'visibility': vis,
        'static': is_static,
    })

# Convert Java type to Kotlin
def j2k_type(t):
    # Remove fully qualified package names, keep class names
    # e.g., "org.bukkit.entity.Player" -> "Player"
    # e.g., "java.util.List<ro.ainpc.npc.AINPC>" -> "List<AINPC>"
    t = re.sub(r'[\w.]+\.(\w+)', r'\1', t)
    t = t.replace('boolean', 'Boolean').replace('int', 'Int').replace('long', 'Long')
    t = t.replace('double', 'Double').replace('float', 'Float').replace('void', 'Unit')
    # Fix [] arrays
    t = t.replace('[]', '')
    return t

# Generate Kotlin
lines = []
lines.append("package ro.ainpc.engine")
lines.append("")
lines.append("import com.google.gson.Gson")
lines.append("import org.bukkit.Location")
lines.append("import org.bukkit.entity.Entity")
lines.append("import org.bukkit.entity.Player")
lines.append("import ro.ainpc.AINPCPlugin")
lines.append("import ro.ainpc.npc.AINPC")
lines.append("import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition")
lines.append("import ro.ainpc.story.StoryContextService")
lines.append("import java.util.*")
lines.append("import java.util.concurrent.ConcurrentHashMap")
lines.append("")

lines.append("class ScenarioEngine(")
lines.append("    private val plugin: AINPCPlugin")
lines.append(") {")
lines.append("    private val scenarioTemplates: MutableMap<String, ScenarioTemplate> = LinkedHashMap()")
lines.append("    private val questTemplates: MutableMap<String, ScenarioTemplate> = LinkedHashMap()")
lines.append("    private val gson: Gson = Gson()")
lines.append("    private val activePlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>> = ConcurrentHashMap()")
lines.append("    private val archivedPlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>> = ConcurrentHashMap()")
lines.append("    private val questCompletionLocks: MutableMap<UUID, String> = ConcurrentHashMap()")
lines.append("    val questDefinitions: MutableList<ScenarioDefinition> = ArrayList()")
lines.append("    var storyContextService: StoryContextService? = null")
lines.append("    private val trackedQuestPlayers: MutableSet<UUID> = HashSet()")
lines.append("    private val trackedQuestTemplates: MutableMap<UUID, String> = ConcurrentHashMap()")
lines.append("")
lines.append("    init {")
lines.append("        loadScenarioTemplates()")
lines.append("    }")
lines.append("")

# Track seen method signatures to avoid conflicting overloads
seen = set()

for m in all_methods:
    vis = m['visibility']
    name = m['name']
    ret = j2k_type(m['return_type'])
    params = [(pn, j2k_type(pt)) for pt, pn in m['params']]
    param_str = ", ".join(f"{pn}: {pt}" for pn, pt in params)
    
    # Build unique signature key
    sig = f"{name}({', '.join(pt for _, pt in params)})"
    
    # Skip duplicates (javap sometimes shows synthetic bridge methods)
    if sig in seen and m['visibility'] != 'public':
        continue
    seen.add(sig)
    
    vis_kw = ""
    if vis == 'private':
        vis_kw = "private "
    
    if ret == 'Unit':
        method_line = f"    {vis_kw}fun {name}({param_str}) {{"
    else:
        method_line = f"    {vis_kw}fun {name}({param_str}): {ret} {{"
    
    lines.append(method_line)
    lines.append(f"        TODO(\"Not yet converted from Java\")")
    lines.append(f"    }}")
    lines.append("")

lines.append("}")

output_path = r"ainpc-core-plugin\src\main\kotlin\ro\ainpc\engine\ScenarioEngine.kt"
with open(output_path, 'w', encoding='utf-8', newline='\r\n') as f:
    f.write('\n'.join(lines))

print(f"Generated {len(all_methods)} methods ({len(seen)} unique)")
