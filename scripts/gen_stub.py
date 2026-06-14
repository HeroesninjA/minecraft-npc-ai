#!/usr/bin/env python3
"""Generate Kotlin stub from javap output."""
import subprocess, re, sys

# Run javap
javap = r"C:\Program Files\Java\jdk-25.0.2\bin\javap.exe"
classfile = None
import glob, os
for root, dirs, files in os.walk(r"ainpc-core-plugin\build"):
    for f in files:
        if f == "ScenarioEngine.class":
            classfile = os.path.join(root, f)
            break
    if classfile:
        break

if not classfile:
    print("Class file not found!")
    sys.exit(1)

# Get all methods
result = subprocess.run([javap, "-p", classfile], capture_output=True, text=True)
output = result.stdout

# Parse methods
public_methods = []
private_methods = []

for line in output.split('\n'):
    line = line.strip()
    if not line or 'ScenarioEngine(' in line or '{' in line or '}' in line or 'Compiled from' in line:
        continue
    
    # Parse: [private] return_type name(params);
    match = re.match(r'(private\s+)?((?:static\s+)?[\w.<>\[\],\s]+?)\s+(\w+)\(([^)]*)\)\s*;', line)
    if match:
        is_private = bool(match.group(1))
        ret_type = match.group(2).strip()
        name = match.group(3)
        params_str = match.group(4)
        
        # Parse parameters
        params = []
        if params_str.strip():
            for p in params_str.split(','):
                p = p.strip()
                parts = p.rsplit(' ', 1)
                if len(parts) == 2:
                    ptype, pname = parts
                    params.append((ptype.strip(), pname.strip()))
        
        method_info = {
            'name': name,
            'return_type': ret_type,
            'params': params,
            'private': is_private,
        }
        
        if is_private:
            private_methods.append(method_info)
        else:
            public_methods.append(method_info)

# Convert Java types to Kotlin
def j2k_type(t):
    t = t.replace('boolean', 'Boolean').replace('int', 'Int').replace('long', 'Long')
    t = t.replace('double', 'Double').replace('float', 'Float').replace('void', 'Unit')
    # Remove fully qualified names, keep simple names
    t = re.sub(r'[\w.]+\.(\w+)', r'\1', t)
    return t

# Generate Kotlin
lines = []
lines.append("package ro.ainpc.engine")
lines.append("")
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
lines.append("    private val gson: Gson = com.google.gson.Gson()")
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

# Generate public methods
for m in public_methods:
    vis = ""
    name = m['name']
    ret = j2k_type(m['return_type'])
    params = [f"{pn}: {j2k_type(pt)}" for pt, pn in m['params']]
    param_str = ", ".join(params)
    
    # Handle method name conflicts with Kotlin keywords
    if name == 'when':
        name = '`when`'
    
    if ret == 'Unit':
        lines.append(f"    {vis}fun {name}({param_str}) {{")
    else:
        lines.append(f"    {vis}fun {name}({param_str}): {ret} {{")
    
    lines.append(f"        TODO(\"Not yet converted from Java\")")
    lines.append(f"    }}")
    lines.append("")

# Generate private methods  
for m in private_methods:
    name = m['name']
    ret = j2k_type(m['return_type'])
    params = [f"{pn}: {j2k_type(pt)}" for pt, pn in m['params']]
    param_str = ", ".join(params)
    
    if ret == 'Unit':
        lines.append(f"    private fun {name}({param_str}) {{")
    else:
        lines.append(f"    private fun {name}({param_str}): {ret} {{")
    
    lines.append(f"        TODO(\"Not yet converted from Java\")")
    lines.append(f"    }}")
    lines.append("")

lines.append("}")

# Write
output_path = r"ainpc-core-plugin\src\main\kotlin\ro\ainpc\engine\ScenarioEngine.kt"
with open(output_path, 'w', encoding='utf-8', newline='\r\n') as f:
    f.write('\n'.join(lines))

print(f"Generated {len(public_methods)} public + {len(private_methods)} private methods")
print(f"Written to {output_path}")
