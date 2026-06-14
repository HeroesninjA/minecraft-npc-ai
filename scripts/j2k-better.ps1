param([string]$InputFile, [string]$OutputFile)

$raw = Get-Content -LiteralPath $InputFile -Raw
$lines = $raw -split "`r`n"

# We'll build the output line by line, applying transformations
$out = @()
$inClass = $false
$inMethod = $false
$skipUntilBrace = 0
$javaFields = @()
$i = 0

while ($i -lt $lines.Count) {
    $line = $lines[$i]
    $trim = $line.Trim()
    
    # === Package ===
    if ($trim -match '^package\s+(.+);$') {
        $out += "package $($matches[1])"
        $out += ""
        $out += "// AUTO-CONVERTED from Java - review required"
        $out += ""
        $i++; continue
    }
    
    # === Skip imports (will add needed ones manually) ===
    if ($trim -match '^import\s+') {
        $i++; continue
    }
    
    # === Javadoc comments ===
    if ($trim -match '^/\*\*$' -or $trim -match '^\s*\*') {
        $out += $line
        $i++; continue
    }
    if ($trim -match '^\s*\*/$') {
        $out += $line
        $i++; continue
    }
    
    # === Class declaration ===
    if ($trim -match '^public class (\w+) \{') {
        $className = $matches[1]
        $out += "class $className("
        $out += "    private val plugin: AINPCPlugin"
        $out += ") {"
        $out += ""
        $out += "    private val gson: Gson = Gson()"
        $out += "    private val npcsByUuid: MutableMap<UUID, AINPC> = ConcurrentHashMap()"
        $out += "    private val npcsById: MutableMap<Int, AINPC> = ConcurrentHashMap()"
        $out += "    private val npcsByEntityId: MutableMap<UUID, AINPC> = ConcurrentHashMap()"
        $out += "    private val npcsBySourceKey: MutableMap<String, AINPC> = ConcurrentHashMap()"
        $out += "    private val villagePopulationCooldowns: MutableMap<String, Long> = ConcurrentHashMap()"
        $out += ""
        $out += "    init {"
        $out += "        NPCManagerVillagerLookup.initVillagerLookupPlugin(plugin)"
        $out += "        NPCManagerDB.initNpcManagerDbPlugin(plugin)"
        $out += "        NPCManagerAnchors.initNpcManagerAnchorsPlugin(plugin)"
        $out += "    }"
        $inClass = $true
        $i++; continue
    }
    
    # === Skip old constructor ===
    if ($trim -match '^public (\w+)\(AINPCPlugin plugin\) \{') {
        # Find matching closing brace
        $depth = 1
        $i++
        while ($i -lt $lines.Count -and $depth -gt 0) {
            $l = $lines[$i].Trim()
            if ($l -eq '}') { $depth-- }
            if ($l -match '\{$') { $depth += ([regex]::Matches($l, '\{')).Count }
            if ($l -match '\}$') { $depth -= ([regex]::Matches($l, '\}')).Count }
            $i++
        }
        continue
    }
    
    if ($inClass -and $trim -eq '') {
        $out += ""
        $i++; continue
    }
    
    # === Skip field declarations ===
    if ($trim -match '^private final .+;$') {
        $i++; continue
    }
    
    # === Method declarations ===
    if ($trim -match '^((public|private)\s+)?((static\s+)?[\w.<>\[\],\s]+)\s+(\w+)\(([^)]*)\)\s*\{') {
        $vis = if ($matches[2] -eq 'private') { 'private ' } else { '' }
        $retType = $matches[3].Trim()
        $methodName = $matches[5]
        $params = $matches[6]
        
        if ($retType -eq 'void') {
            $out += "    ${vis}fun $methodName($params) {"
        } else {
            # Convert Java types to Kotlin types in return position
            $retType = $retType -replace 'boolean', 'Boolean'
            $retType = $retType -replace '\bint\b', 'Int'
            $retType = $retType -replace '\blong\b', 'Long'
            $retType = $retType -replace '\bdouble\b', 'Double'
            $retType = $retType -replace '\bfloat\b', 'Float'
            $retType = $retType -replace 'List<', 'List<'
            $retType = $retType -replace 'Map<', 'Map<'
            $retType = $retType -replace 'Collection<', 'Collection<'
            $out += "    ${vis}fun $methodName($params): $retType {"
        }
        $i++; continue
    }
    
    # === Lines inside methods: convert Java-specific patterns ===
    if ($inClass) {
        $modified = $line
        
        # Try-with-resources: try (Type var = expr) {
        if ($modified -match 'try\s*\(\s*([\w.<>\[\]]+)\s+(\w+)\s*=\s*(.+)\)\s*\{') {
            # Complex: use .use {}
            $varType = $matches[1]
            $varName = $matches[2]
            $expr = $matches[3]
            $modified = $modified -replace "try\s*\(\s*$([regex]::Escape($varType))\s+$varName\s*=\s*$([regex]::Escape($expr))\)\s*\{", "$expr.use { $varName ->"
        }
        
        # instanceof
        $modified = $modified -replace 'instanceof (\w+)', 'is $1'
        
        # new Type() -> Type()
        $modified = $modified -replace 'new\s+([A-Z]\w+)\(', '$1('
        $modified = $modified -replace 'new\s+([A-Z]\w+)\.([A-Z]\w+)\(', '$1.$2('
        $modified = $modified -replace 'new\s+([a-z]\w+)\.([A-Z]\w+)\(', '$1.$2('
        
        # Enhanced for: for (Type var : expr) -> for (var in expr)
        if ($modified -match 'for\s*\(\s*(?:final\s+)?([\w.<>\[\]]+)\s+(\w+)\s*:\s*(.+)\)\s*\{') {
            $modified = $modified -replace "for\s*\(\s*(?:final\s+)?$([regex]::Escape($matches[1]))\s+$([regex]::Escape($matches[2]))\s*:\s*$([regex]::Escape($matches[3]))\)\s*\{", "for ($($matches[2]) in $($matches[3])) {"
        }
        
        # Map.Entry<A, B> entry -> entry
        $modified = $modified -replace 'Map\.Entry<[^>]+>\s+(\w+)', '$1'
        
        # Type declarations in local variables: Type name = -> val name: Type =
        if ($modified -match '^\s{8,}((?:final\s+)?[\w.<>\[\]]+)\s+(\w+)\s*=\s*') {
            # Only convert if it looks like a local variable declaration (not a method call)
            $varType = $matches[1]
            $varName = $matches[2]
            if ($varName -ne 'class' -and $varType -notmatch '^\s*(return|if|for|while|catch|throw|new)\b') {
                $varTypeClean = $varType -replace '^final\s+', ''
                $varTypeClean = $varTypeClean -replace '\bint\b', 'Int'
                $varTypeClean = $varTypeClean -replace '\bboolean\b', 'Boolean'
                $varTypeClean = $varTypeClean -replace '\blong\b', 'Long'
                $varTypeClean = $varTypeClean -replace '\bdouble\b', 'Double'
                $varTypeClean = $varTypeClean -replace '\bfloat\b', 'Float'
                $modified = $modified -replace "$([regex]::Escape($varType))\s+$varName\s*=", "val $varName: $varTypeClean ="
            }
        }
        
        # throw new -> throw
        $modified = $modified -replace 'throw new ', 'throw '
        
        # @Override - already handled by import skipping
        if ($trim -eq '@Override') { $i++; continue }
        
        # try { -> try {
        # catch (Exception e) -> catch (e: Exception)
        $modified = $modified -replace 'catch\s*\(\s*([\w.]+)\s+(\w+)\s*\)', 'catch ($2: $1)'
        
        # Collections.unmodifiableCollection( -> 
        $modified = $modified -replace 'Collections\.unmodifiableCollection\(', ''
        # Fix resulting double paren
        $modified = $modified -replace 'ArrayList\(\(', 'ArrayList('
        
        # List.copyOf( -> listOf(
        $modified = $modified -replace 'List\.copyOf\(', 'listOf('
        
        # List.of( -> listOf(
        $modified = $modified -replace 'List\.of\(', 'listOf('
        
        # Objects::nonNull -> { it != null }
        $modified = $modified -replace 'Objects::nonNull', '{ it != null }'
        
        # Type::method -> Type::method (keep - Kotlin also uses ::)
        # Villager::isValid -> Villager::isValid (keep)
        
        # .stream() -> (keep - can be called from Kotlin)
        # .toList() -> (keep)
        # .sorted(comparator) -> .sortedWith(Comparator { a, b -> ... })
        # Complex, skip for now
        
        # ;$ -> (remove trailing semicolon)
        if ($modified -match ';$') {
            $modified = $modified -replace ';$', ''
        }
        
        $out += $modified
    } else {
        $out += $line
    }
    
    $i++
}

# Add required imports at the top
$imports = @(
    "package ro.ainpc.managers",
    "",
    "import com.google.gson.Gson",
    "import com.google.gson.JsonObject",
    "import org.bukkit.Chunk",
    "import org.bukkit.Location",
    "import org.bukkit.NamespacedKey",
    "import org.bukkit.World",
    "import org.bukkit.entity.Entity",
    "import org.bukkit.entity.Villager",
    "import ro.ainpc.AINPCPlugin",
    "import ro.ainpc.api.WorldAdminApi",
    "import ro.ainpc.npc.AINPC",
    "import ro.ainpc.npc.NPCEmotions",
    "import ro.ainpc.npc.NPCPersonality",
    "import ro.ainpc.spawn.NpcSpawnPlan",
    "import ro.ainpc.spawn.ResolvedNpcSpawnPlan",
    "import ro.ainpc.world.NpcWorldBinding",
    "import ro.ainpc.world.NpcWorldBindingService",
    "import ro.ainpc.world.WorldNodeInfo",
    "import ro.ainpc.world.WorldPlaceInfo",
    "import java.sql.PreparedStatement",
    "import java.sql.ResultSet",
    "import java.sql.SQLException",
    "import java.sql.Statement",
    "import java.util.*",
    "import java.util.concurrent.ConcurrentHashMap"
)

$finalContent = ($imports -join "`r`n") + "`r`n`r`n" + ($out -join "`r`n")
Set-Content -LiteralPath $OutputFile -Value $finalContent -NoNewline
Write-Host "Conversion written to $OutputFile ($($finalContent.Split("`n").Count) lines)"
