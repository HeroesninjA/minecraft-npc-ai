import subprocess, re, os

for root, dirs, files in os.walk(r'ainpc-core-plugin/build'):
    for f in files:
        if f == 'ScenarioEngine.class': cf = os.path.join(root, f); break

r = subprocess.run([r'C:\Program Files\Java\jdk-25.0.2\bin\javap.exe', '-p', cf], capture_output=True, text=True)

pat = r'^\s*(?:(private|public|protected)\s+)?(?:(static)\s+)?((?:[\w.<>\[\],\s])+?)\s+(\w+)\(([^)]*)\)\s*;'
methods = []

for line in r.stdout.splitlines():
    line = line.strip()
    if not line or '{' in line or '}' in line or 'Compiled' in line: continue
    m = re.match(pat, line)
    if not m: continue
    vis = m.group(1) or 'public'
    ret = m.group(3).strip()
    name = m.group(4)
    ps_raw = m.group(5)
    if name == 'ScenarioEngine': continue
    if 'lambda' in name: continue  # skip synthetic lambda methods
    
    # Parse params handling nested generics
    params = []
    if ps_raw.strip():
        # Split by comma but respect angle bracket nesting
        parts = []
        depth = 0
        current = ''
        for c in ps_raw:
            if c == '<': depth += 1
            elif c == '>': depth -= 1
            elif c == ',' and depth == 0:
                parts.append(current.strip())
                current = ''
                continue
            current += c
        if current.strip():
            parts.append(current.strip())
        
        for i, pt in enumerate(parts):
            pt = pt.strip()
            sp = pt.rsplit(' ', 1)
            pname = sp[1] if len(sp) == 2 else 'p' + str(i)
            ptype = sp[0].strip()
            params.append((ptype, pname))
    
    methods.append(dict(name=name, ret=ret, params=params, vis=vis))

def j2k(t):
    # Replace $ with . for inner classes
    t = t.replace('$', '.')
    # Remove package prefixes: foo.bar.Baz -> Baz
    t = re.sub(r'[\w.]+\$?\.(\w+)', r'\1', t)
    # Java -> Kotlin types
    for a, b in [('boolean','Boolean'),('int','Int'),('long','Long'),('double','Double'),('float','Float'),('void','')]:
        t = t.replace(a, b)
    return t.strip()

out = []
out.append('package ro.ainpc.engine')
out.append('')
out.append('import com.google.gson.Gson')
out.append('import org.bukkit.Location')
out.append('import org.bukkit.entity.Entity')
out.append('import org.bukkit.entity.Player')
out.append('import ro.ainpc.AINPCPlugin')
out.append('import ro.ainpc.npc.AINPC')
out.append('import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition')
out.append('import ro.ainpc.story.StoryContextService')
out.append('import java.util.*')
out.append('import java.util.concurrent.ConcurrentHashMap')
out.append('')
out.append('class ScenarioEngine(private val plugin: AINPCPlugin) {')
out.append('    private val scenarioTemplates = LinkedHashMap<String, ScenarioTemplate>()')
out.append('    private val questTemplates = LinkedHashMap<String, ScenarioTemplate>()')
out.append('    private val gson = Gson()')
out.append('    private val activePlayerQuests = ConcurrentHashMap<UUID, MutableMap<String, PlayerQuestProgress>>()')
out.append('    private val archivedPlayerQuests = ConcurrentHashMap<UUID, MutableMap<String, PlayerQuestProgress>>()')
out.append('    private val questCompletionLocks = ConcurrentHashMap<UUID, String>()')
out.append('    val questDefinitions = ArrayList<ScenarioDefinition>()')
out.append('    var storyContextService: StoryContextService? = null')
out.append('    private val trackedQuestPlayers = HashSet<UUID>()')
out.append('    private val trackedQuestTemplates = ConcurrentHashMap<UUID, String>()')
out.append('')
out.append('    init { loadScenarioTemplates() }')
out.append('')

seen = set()
for m in methods:
    ret = j2k(m['ret'])
    vis = 'private ' if m['vis'] == 'private' else ''
    params = [(pn, j2k(pt)) for pt, pn in m['params']]
    ps = ', '.join(n + ': ' + t for n, t in params)
    name = m['name']
    
    sig = name + '(' + ps + ')'
    if sig in seen: continue
    seen.add(sig)
    
    if not ret:
        out.append('    ' + vis + 'fun ' + name + '(' + ps + ') {')
        out.append('        TODO()')
        out.append('    }')
    else:
        out.append('    ' + vis + 'fun ' + name + '(' + ps + '): ' + ret + ' = TODO()')

out.append('}')

path = r'ainpc-core-plugin\src\main\kotlin\ro\ainpc\engine\ScenarioEngine.kt'
with open(path, 'w', encoding='utf-8', newline='\r\n') as f:
    f.write('\n'.join(out))
print(str(len(methods)) + ' methods, ' + str(len(seen)) + ' unique, ' + str(len(out)) + ' lines')
