param([string]$InputFile, [string]$OutputFile)

$c = Get-Content -LiteralPath $InputFile -Raw

# 1. Package (remove semicolon)
$c = $c -replace 'package\s+([\w.]+)\s*;', 'package $1'

# 2. Remove all import lines (Kotlin uses same-package resolution; explicit imports added later)
$c = $c -replace '^import\s+.*$', ''

# 3. Remove blank lines left by import removal (keep at most 2 consecutive)
$c = $c -replace "`r`n`r`n`r`n+", "`r`n`r`n"

# 4. Class declaration and constructor
$c = $c -replace 'public class (\w+) \{', 'class $1(private val plugin: AINPCPlugin) {'

# 5. Remove old constructor
$c = $c -replace '(?s)\s{4}public \w+\(AINPCPlugin plugin\) \{.*?\n    \}', ''

# 6. Remove field declarations
$c = $c -replace '\s{4}private final .*?;\s*\n', ''

# 7. Add properties after class opening
$initBlock = @'

    private val gson: Gson = Gson()
    private val npcsByUuid: MutableMap<UUID, AINPC> = ConcurrentHashMap()
    private val npcsById: MutableMap<Int, AINPC> = ConcurrentHashMap()
    private val npcsByEntityId: MutableMap<UUID, AINPC> = ConcurrentHashMap()
    private val npcsBySourceKey: MutableMap<String, AINPC> = ConcurrentHashMap()
    private val villagePopulationCooldowns: MutableMap<String, Long> = ConcurrentHashMap()

    init {
        NPCManagerVillagerLookup.initVillagerLookupPlugin(plugin)
        NPCManagerDB.initNpcManagerDbPlugin(plugin)
        NPCManagerAnchors.initNpcManagerAnchorsPlugin(plugin)
    }

'@
$c = $c -replace '(class NPCManager\(private val plugin: AINPCPlugin\) \{)\s*\n', "`$1`n$initBlock"

# 8. Remove visibility modifiers (Kotlin default is public)
$c = $c -replace '(?m)^    public ', '    '
$c = $c -replace '(?m)^    private ', '    private '  # keep private

# 9. Convert method declarations: void -> fun, Type -> fun ... : Type
# Pattern: Type methodName( -> fun methodName(): Type
$c = $c -replace '(?m)^    ([\w.<>\[\]]+) (\w+)\((.*?)\) \{', '    fun $2($3): $1 {'

# 10. Convert void methods
$c = $c -replace '(?m)^    void (\w+)\((.*?)\) \{', '    fun $1($2) {'

# 11. Convert private methods
$c = $c -replace '(?m)^    private ([\w.<>\[\]]+) (\w+)\((.*?)\) \{', '    private fun $2($3): $1 {'
$c = $c -replace '(?m)^    private void (\w+)\((.*?)\) \{', '    private fun $2($3) {'

# 12. Remove semicolons from end of lines (but keep in for-loops which need special handling)
$c = $c -replace '(?m);$', ''

# 13. new Type() -> Type()
$c = $c -replace 'new ArrayList<>\(', 'ArrayList('
$c = $c -replace 'new ArrayList<', 'ArrayList('  # shouldn't hit due to diamond
$c = $c -replace 'new HashMap<>\(', 'HashMap('
$c = $c -replace 'new HashSet<>\(', 'HashSet('
$c = $c -replace 'new ConcurrentHashMap<>\(', 'ConcurrentHashMap('
$c = $c -replace 'new Random\(\)', 'Random()'
$c = $c -replace 'new Gson\(\)', 'Gson()'

# 14. new Type(arg1, arg2, ...) -> Type(arg1, arg2, ...)
$c = $c -replace 'new ([\w.]+)\(', '$1('

# 15. Try-with-resources -> use { }
$c = $c -replace 'try \(([\w.<>\[\] ]+) (\w+) = (.*?)\) \{', '$3.use { $2 ->'

# 16. catch (Exception e) -> catch (e: Exception)
$c = $c -replace 'catch \(([\w.]+) (\w+)\)', 'catch ($2: $1)'

# 17. Enhanced for loops: for (Type var : collection) -> for (var in collection)
$c = $c -replace 'for \(([\w.<>\[\]]+) (\w+) : (.+?)\) \{', 'for ($2 in $3) {'

# 18. Map.Entry iteration
$c = $c -replace 'Map\.Entry<([\w, ]+)> (\w+)', '$2  # Map.Entry conversion needed'

# 19. String concatenation (simple cases)
$c = $c -replace '" \+ (\w+) \+ "', '" + $1 + "'

# 20. Remove @Override annotations
$c = $c -replace '(?m)^    @Override\s*\n', ''

# 21. Remove static import reference from methods (since same-package)
# Already handled by import removal

# 22. List.copyOf() -> toList()
$c = $c -replace 'List\.copyOf\(', 'listOf('

# 23. Collections.unmodifiableCollection() -> toList()
$c = $c -replace 'Collections\.unmodifiableCollection\(', ''

# 24. int -> Int in type declarations (but not in method calls)
$c = $c -replace ': int ', ': Int '
$c = $c -replace 'Map<Integer,', 'Map<Int,'

# 25. boolean -> Boolean in type declarations
$c = $c -replace ': boolean ', ': Boolean '

# 26. .equals() -> == (for strings where safe)
# Skipping this - too dangerous for automated replacement

# 27. throw new -> throw
$c = $c -replace 'throw new ([\w.]+)\(', 'throw $1('

# 28. instanceof -> is
$c = $c -replace 'instanceof (\w+) (\w+)', 'is $1'

# 29. null checks: == null -> == null (same), != null -> != null (same)

# 30. String.isBlank() -> isBlank() (same)

Set-Content -LiteralPath $OutputFile -Value $c -NoNewline
Write-Host "Conversion written to $OutputFile"
