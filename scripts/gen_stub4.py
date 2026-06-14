import subprocess, re, os

javap = r'C:\Program Files\Java\jdk-25.0.2\bin\javap.exe'
for root, dirs, files in os.walk(r'ainpc-core-plugin\build'):
    for f in files:
        if f == 'ScenarioEngine.class':
            classfile = os.path.join(root, f)
            break

result = subprocess.run([javap, '-p', classfile], capture_output=True, text=True)
pat = r'(?:(private|public|protected)\s+)?(?:(static)\s+)?((?:[\w.<>\[\],\s])+?)\s+(\w+)\(([^)]*)\)\s*;'
all_methods = []

for line in result.stdout.split('\n'):
    line = line.strip()
    if not line or '{' in line or '}' in line or 'Compiled from' in line:
        continue
    m = re.match(pat, line)
    if not m: continue
    vis = m.group(1) or 'public'
    ret = m.group(3).strip()
    name = m.group(4)
    ps = m.group(5)
    if name == 'ScenarioEngine': continue
    
    params = []
    if ps.strip():
        for i, pt in enumerate(ps.split(',')):
            pt = pt.strip()
            parts = pt.rsplit(' ', 1)
            if len(parts) == 2 and '.' not in parts[1] and '$' not in parts[1]:
                pname = parts[1]
            else:
                pname = f'p{i}'
            params.append((parts[0].strip(), pname))
    all_methods.append({'name':name,'ret':ret,'params':params,'vis':vis})

def j2k(t):
    t = re.sub(r'[\w.\$]+[.\$](\w+)', r'\1', t)
    t = t.replace('boolean','Boolean').replace('int','Int').replace('long','Long')
    t = t.replace('double','Double').replace('float','Float').replace('void','Unit')
    return t

lines = []
lines.append('package ro.ainpc.engine')
lines.append('')
lines.append('import com.google.gson.Gson')
lines.append('import org.bukkit.Location')
lines.append('import org.bukkit.entity.Entity')
lines.append('import org.bukkit.entity.Player')
lines.append('import ro.ainpc.AINPCPlugin')
lines.append('import ro.ainpc.npc.AINPC')
lines.append('import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition')
lines.append('import ro.ainpc.story.StoryContextService')
lines.append('import java.util.*')
lines.append('import java.util.concurrent.ConcurrentHashMap')
lines.append('')

lines.append('class ScenarioEngine(private val plugin: AINPCPlugin) {')
lines.append('    private val scenarioTemplates = LinkedHashMap<String, ScenarioTemplate>()')
lines.append('    private val questTemplates = LinkedHashMap<String, ScenarioTemplate>()')
lines.append('    private val gson = Gson()')
lines.append('    private val activePlayerQuests = ConcurrentHashMap<UUID, MutableMap<String, PlayerQuestProgress>>()')
lines.append('    private val archivedPlayerQuests = ConcurrentHashMap<UUID, MutableMap<String, PlayerQuestProgress>>()')
lines.append('    private val questCompletionLocks = ConcurrentHashMap<UUID, String>()')
lines.append('    val questDefinitions = ArrayList<ScenarioDefinition>()')
lines.append('    var storyContextService: StoryContextService? = null')
lines.append('    private val trackedQuestPlayers = HashSet<UUID>()')
lines.append('    private val trackedQuestTemplates = ConcurrentHashMap<UUID, String>()')
lines.append('    init { loadScenarioTemplates() }')
lines.append('')

seen = set()
for m in all_methods:
    ret = j2k(m['ret'])
    vis = 'private ' if m['vis'] == 'private' else ''
    params = [(pn, j2k(pt)) for pt, pn in m['params']]
    ps = ', '.join(f'{n}: {t}' for n, t in params)
    name = m['name']
    sig = f'{name}({ps})'
    if sig in seen: continue
    seen.add(sig)
    if ret == 'Unit':
        lines.append(f'    {vis}fun {name}({ps}) { TODO() }')
    else:
        lines.append(f'    {vis}fun {name}({ps}): {ret} { TODO() }')

lines.append('}')

output_path = r'ainpc-core-plugin\src\main\kotlin\ro\ainpc\engine\ScenarioEngine.kt'
with open(output_path, 'w', encoding='utf-8', newline='\r\n') as f:
    f.write('\n'.join(lines))
print(f'{len(all_methods)} methods, {len(seen)} unique, {len(lines)} lines')
