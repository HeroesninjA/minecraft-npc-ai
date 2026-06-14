import subprocess, re, os

for root, dirs, files in os.walk(r'ainpc-core-plugin/build'):
    for f in files:
        if f == 'ScenarioEngine.class': cf = os.path.join(root, f); break

r = subprocess.run([r'C:\Program Files\Java\jdk-25.0.2\bin\javap.exe', '-p', cf], capture_output=True, text=True)

pat = r'^\s*(?:(private|public|protected)\s+)?(?:(static)\s+)?(<[^>]+>\s+)?((?:[\w.<>\[\],\s])+?)\s+(\w+)\(([^)]*)\)\s*;'
methods = []

def split_params(ps_raw):
    """Split parameter string by commas, respecting angle brackets."""
    if not ps_raw.strip(): return []
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
    return parts

def split_type_name(pt):
    """Split 'type name' respecting angle brackets. Returns (type, name_or_None)."""
    pt = pt.strip()
    # Find last space that's NOT inside angle brackets
    depth = 0
    last_space = -1
    for i, c in enumerate(pt):
        if c == '<': depth += 1
        elif c == '>': depth -= 1
        elif c == ' ' and depth == 0:
            last_space = i
    if last_space >= 0:
        return pt[:last_space].strip(), pt[last_space+1:].strip()
    return pt, None

for line in r.stdout.splitlines():
    line = line.strip()
    if not line or '{' in line or '}' in line or 'Compiled' in line: continue
    m = re.match(pat, line)
    if not m: continue
    vis = m.group(1) or 'public'
    gen = m.group(3)  # <T> or None
    ret = m.group(4).strip()
    name = m.group(5)
    ps_raw = m.group(6)
    if name == 'ScenarioEngine': continue
    if 'lambda' in name: continue
    
    params = []
    for i, pt in enumerate(split_params(ps_raw)):
        ptype, pname = split_type_name(pt)
        if pname is None:
            pname = 'p' + str(i)
        params.append((ptype, pname))
    
    methods.append(dict(name=name, ret=ret, gen=gen, params=params, vis=vis))

def j2k(t):
    t = t.replace('$', '.')
    # Fix wildcards
    t = t.replace('?', '*')
    # Handle java.lang types
    t = t.replace('java.lang.String', 'String')
    t = t.replace('java.lang.Integer', 'Int')
    t = t.replace('java.lang.Long', 'Long')
    t = t.replace('java.lang.Boolean', 'Boolean')
    t = t.replace('java.lang.Double', 'Double')
    t = t.replace('java.lang.Float', 'Float')
    # Only strip lowercase package prefixes, keep ClassName.InnerClass
    parts = t.split('.')
    result_parts = []
    i = 0
    while i < len(parts):
        if parts[i] and parts[i][0].islower() and i < len(parts) - 1:
            i += 1
        else:
            result_parts.extend(parts[i:])
            break
    t = '.'.join(result_parts) if result_parts else parts[-1] if parts else t
    for a, b in [('boolean','Boolean'),('int','Int'),('long','Long'),('double','Double'),('float','Float'),('void','')]:
        t = t.replace(a, b)
    return t.strip()

out = []
out.append('package ro.ainpc.engine')
out.append('')
out.append('import com.google.gson.Gson')
out.append('import org.bukkit.Location')
out.append('import org.bukkit.Material')
out.append('import org.bukkit.configuration.ConfigurationSection')
out.append('import org.bukkit.entity.Entity')
out.append('import org.bukkit.entity.Player')
out.append('import ro.ainpc.AINPCPlugin')
out.append('import ro.ainpc.npc.AINPC')
out.append('import ro.ainpc.engine.FeaturePackLoader.ProfessionDefinition')
out.append('import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition')
out.append('import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition')
out.append('import ro.ainpc.engine.QuestAnchorResolver.ResolvedQuestAnchors')
out.append('import ro.ainpc.engine.QuestScenarioContract.Category')
out.append('import ro.ainpc.story.StoryContextService')
out.append('import ro.ainpc.world.WorldNode')
out.append('import ro.ainpc.world.WorldPlace')
out.append('import ro.ainpc.world.WorldRegion')
out.append('import java.lang.reflect.Type')
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
    gen = (m['gen'] or '').strip()
    params = [(pn, j2k(pt)) for pt, pn in m['params']]
    ps = ', '.join(n + ': ' + t for n, t in params)
    name = m['name']
    
    sig = name + '(' + ps + ')'
    if sig in seen: continue
    seen.add(sig)
    
    gen_prefix = (gen + ' ') if gen else ''
    
    if not ret:
        out.append('    ' + vis + 'fun ' + gen_prefix + name + '(' + ps + ') {')
        out.append('        TODO()')
        out.append('    }')
    else:
        out.append('    ' + vis + 'fun ' + gen_prefix + name + '(' + ps + '): ' + ret + ' = TODO()')

out.append('}')

path = r'ainpc-core-plugin\src\main\kotlin\ro\ainpc\engine\ScenarioEngine.kt'
with open(path, 'w', encoding='utf-8', newline='\r\n') as f:
    f.write('\n'.join(out))
print(str(len(methods)) + ' methods, ' + str(len(seen)) + ' unique, ' + str(len(out)) + ' lines')
