param([string]$InputFile, [string]$OutputFile)

$raw = Get-Content -LiteralPath $InputFile -Raw

# ==== SECTION 1: Package and imports ====
$raw = $raw -replace 'package ro\.ainpc\.engine;', 'package ro.ainpc.engine'

# Strip static imports (resolved by Kotlin same-package)
$raw = $raw -replace '^import static .*;\s*$', ''
# Strip regular imports (will add specific ones)
$raw = $raw -replace '^import (?!static).*;\s*$', ''

# Clean up blank lines
$raw = $raw -replace "`r`n`r`n`r`n+", "`r`n`r`n"

# ==== SECTION 2: Class declaration and fields ====
$classMatch = [regex]::Match($raw, 'public class ScenarioEngine \{([\s\S]*?)public ScenarioEngine\(', [System.Text.RegularExpressions.RegexOptions]::Singleline)
if ($classMatch.Success) {
    $fieldSection = $classMatch.Groups[1].Value
    
    # Extract field declarations
    $fields = @()
    $fieldRegex = [regex]::Matches($fieldSection, '(private|public)\s+(final\s+)?([\w.<>,\s\[\]]+)\s+(\w+)\s*=', [System.Text.RegularExpressions.RegexOptions]::Singleline)
    foreach ($m in $fieldRegex) {
        $vis = if ($m.Groups[1].Value -eq 'private') { 'private' } else { '' }
        $type = $m.Groups[3].Value.Trim()
        $name = $m.Groups[4].Value
        $fields += "    ${vis} val $name: $type"
        if ($name -eq 'storyContextService') {
            $fields += "        get() = null"  # initialized later
        }
        if ($name -eq 'questDefinitions') {
            $fields += "        get() = ArrayList()"
        }
    }
}

# ==== SECTION 3: Replace class declaration ====
$escapedFieldSection = [regex]::Escape($classMatch.Value)
$raw = $raw -replace $escapedFieldSection, ''

# Build new class header
$classHeader = @'
class ScenarioEngine(
    private val plugin: AINPCPlugin
) {
    private val scenarioTemplates: MutableMap<String, ScenarioTemplate> = LinkedHashMap()
    private val questTemplates: MutableMap<String, ScenarioTemplate> = LinkedHashMap()
    private val questOffers: MutableMap<UUID, ScenarioQuestOffer> = HashMap()
    private val activeQuests: MutableMap<UUID, ScenarioQuestPhase> = LinkedHashMap()
    private val completedQuests: MutableMap<String, MutableList<PlayerQuestProgress>> = LinkedHashMap()
    private val gson: Gson = Gson()
    private val activePlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>> = ConcurrentHashMap()
    private val archivedPlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>> = ConcurrentHashMap()
    private val questCompletionLocks: MutableMap<UUID, String> = ConcurrentHashMap()
    val questDefinitions: MutableList<FeaturePackLoader.ScenarioDefinition> = ArrayList()
    var storyContextService: StoryContextService? = null
    private val trackedQuestPlayers: MutableSet<UUID> = HashSet()
    private val trackedQuestTemplates: MutableMap<UUID, String> = ConcurrentHashMap()
    private val activeScenarios: MutableMap<UUID, ActiveScenario> = HashMap()

    init {
    }

'@

$raw = $raw -replace 'public class ScenarioEngine \{', $classHeader

# ==== SECTION 4: Remove old constructor ====
$raw = $raw -replace '(?s)\s{4}public ScenarioEngine\(AINPCPlugin plugin\) \{[^}]*?\n    \}\s*\n', ''

# ==== SECTION 5: Method conversions ====

# 5a. Method signatures
$raw = $raw -replace '(?m)^    public ([\w.<>\[\],\s]+) (\w+)\((.*?)\) \{', '    fun $2($3): $1 {'
$raw = $raw -replace '(?m)^    public void (\w+)\((.*?)\) \{', '    fun $1($2) {'
$raw = $raw -replace '(?m)^    private ([\w.<>\[\],\s]+) (\w+)\((.*?)\) \{', '    private fun $2($3): $1 {'
$raw = $raw -replace '(?m)^    private void (\w+)\((.*?)\) \{', '    private fun $1($2) {'
$raw = $raw -replace '(?m)^    protected ([\w.<>\[\],\s]+) (\w+)\((.*?)\) \{', '    protected fun $2($3): $1 {'
$raw = $raw -replace '(?m)^    protected void (\w+)\((.*?)\) \{', '    protected fun $1($2) {'

# 5b. Fix return types
$raw = $raw -replace ': boolean \{', ': Boolean {'
$raw = $raw -replace ': int \{', ': Int {'
$raw = $raw -replace ': long \{', ': Long {'
$raw = $raw -replace ': double \{', ': Double {'
$raw = $raw -replace ': float \{', ': Float {'
$raw = $raw -replace ': void \{', ' {'

# ==== SECTION 6: Comprehensive getter replacement ====
# Java-style getters on Kotlin types → property access
$raw = $raw -replace '\.getTemplateId\(\)', '.templateId'
$raw = $raw -replace '\.getDisplayName\(\)', '.displayName'
$raw = $raw -replace '\.getDescription\(\)', '.description'
$raw = $raw -replace '\.getHint\(\)', '.hint'
$raw = $raw -replace '\.getSourcePackId\(\)', '.sourcePackId'
$raw = $raw -replace '\.getProgressionEnabled\(\)', '.progressionEnabled'
$raw = $raw -replace '\.getProgressionKind\(\)', '.progressionKind'
$raw = $raw -replace '\.getProgressionMechanicId\(\)', '.progressionMechanicId'
$raw = $raw -replace '\.getProgressionLabel\(\)', '.progressionLabel'
$raw = $raw -replace '\.getProgressionSingularLabel\(\)', '.progressionSingularLabel'
$raw = $raw -replace '\.getProgressionPluralLabel\(\)', '.progressionPluralLabel'
$raw = $raw -replace '\.getProgressionMaxActive\(\)', '.progressionMaxActive'
$raw = $raw -replace '\.getQuestCode\(\)', '.questCode'
$raw = $raw -replace '\.getQuestGiverProfession\(\)', '.questGiverProfession'
$raw = $raw -replace '\.getQuestPrerequisites\(\)', '.questPrerequisites'
$raw = $raw -replace '\.getQuestRepeatable\(\)', '.questRepeatable'
$raw = $raw -replace '\.getQuestCooldownSeconds\(\)', '.questCooldownSeconds'
$raw = $raw -replace '\.getQuestDialogues\(\)', '.questDialogues'
$raw = $raw -replace '\.getQuestStages\(\)', '.questStages'
$raw = $raw -replace '\.getObjectives\(\)', '.objectives'
$raw = $raw -replace '\.getRewards\(\)', '.rewards'
$raw = $raw -replace '\.getQuestContract\(\)', '.questContract'
$raw = $raw -replace '\.getTriggerProbability\(\)', '.triggerProbability'
$raw = $raw -replace '\.getMinimumNpcCount\(\)', '.minimumNpcCount'
$raw = $raw -replace '\.getRequiresPlayer\(\)', '.requiresPlayer'
$raw = $raw -replace '\.getNarrativeHints\(\)', '.narrativeHints'
$raw = $raw -replace '\.getPreferredTopologies\(\)', '.preferredTopologies'
$raw = $raw -replace '\.getPhases\(\)', '.phases'
$raw = $raw -replace '\.getRoles\(\)', '.roles'
$raw = $raw -replace '\.getPlayerRoles\(\)', '.playerRoles'
$raw = $raw -replace '\.getNpcRoles\(\)', '.npcRoles'
$raw = $raw -replace '\.getId\(\)', '.id'
$raw = $raw -replace '\.getName\(\)', '.name'
$raw = $raw -replace '\.getType\(\)', '.type'
$raw = $raw -replace '\.getBaseType\(\)', '.baseType'
$raw = $raw -replace '\.getAmount\(\)', '.amount'
$raw = $raw -replace '\.getItemId\(\)', '.itemId'
$raw = $raw -replace '\.getCompletionMode\(\)', '.completionMode'
$raw = $raw -replace '\.getObjectiveIds\(\)', '.objectiveIds'
$raw = $raw -replace '\.getPayload\(\)', '.payload'
$raw = $raw -replace '\.getVariables\(\)', '.variables'
$raw = $raw -replace '\.getBounds\(\)', '.bounds'
$raw = $raw -replace '\.getOccupation\(\)', '.occupation'
$raw = $raw -replace '\.getDatabaseId\(\)', '.databaseId'
$raw = $raw -replace '\.getUuid\(\)', '.uuid'
$raw = $raw -replace '\.getLocation\(\)', '.location'
$raw = $raw -replace '\.getMaxActive\(\)', '.maxActive'
$raw = $raw -replace '\.getActionBarMessage\(\)', '.actionBarMessage'
$raw = $raw -replace '\.getWorldAdmin\(\)', '.worldAdmin'
$raw = $raw -replace '\.getWorldAdminService\(\)', '.worldAdminService'

# Plugin getters
$raw = $raw -replace 'plugin\.getDatabaseManager\(\)', 'plugin.databaseManager'
$raw = $raw -replace 'plugin\.getNpcManager\(\)', 'plugin.npcManager'
$raw = $raw -replace 'plugin\.getMessageUtils\(\)', 'plugin.messageUtils'
$raw = $raw -replace 'plugin\.getProgressionService\(\)', 'plugin.progressionService'
$raw = $raw -replace 'plugin\.getConfig\(\)', 'plugin.config'
$raw = $raw -replace 'plugin\.getLogger\(\)', 'plugin.logger'
$raw = $raw -replace 'plugin\.getServer\(\)', 'plugin.server'
$raw = $raw -replace 'plugin\.getDecisionEngine\(\)', 'plugin.decisionEngine'
$raw = $raw -replace 'plugin\.getStoryStateService\(\)', 'plugin.storyStateService'
$raw = $raw -replace 'plugin\.getFeaturePackLoader\(\)', 'plugin.featurePackLoader'
$raw = $raw -replace 'plugin\.getQuestDirector\(\)', 'plugin.questDirector'
$raw = $raw -replace 'plugin\.getFamilyManager\(\)', 'plugin.familyManager'
$raw = $raw -replace 'plugin\.getMemoryManager\(\)', 'plugin.memoryManager'
$raw = $raw -replace 'plugin\.getDialogueEngine\(\)', 'plugin.dialogueEngine'
$raw = $raw -replace 'plugin\.getEmotionManager\(\)', 'plugin.emotionManager'
$raw = $raw -replace 'plugin\.getNpcWorldBindingService\(\)', 'plugin.npcWorldBindingService'
$raw = $raw -replace 'plugin\.getPlatform\(\)', 'plugin.platform'
$raw = $raw -replace 'plugin\.getQuestConfig\(\)', 'plugin.questConfig'
$raw = $raw -replace 'plugin\.getHouseholdPersistenceService\(\)', 'plugin.householdPersistenceService'
$raw = $raw -replace 'plugin\.getAddonRegistry\(\)', 'plugin.addonRegistry'
$raw = $raw -replace 'plugin\.getScenarioNpcMatcher\(\)', 'plugin.scenarioNpcMatcher'
$raw = $raw -replace 'plugin\.getQuestAnchorResolver\(\)', 'plugin.questAnchorResolver'
$raw = $raw -replace 'plugin\.getDecisionIntentResolver\(\)', 'plugin.decisionIntentResolver'
$raw = $raw -replace 'plugin\.getDependencyValidator\(\)', 'plugin.dependencyValidator'
$raw = $raw -replace 'plugin\.getMetadataValidator\(\)', 'plugin.metadataValidator'

# ==== SECTION 7: Comprehensive setter replacement ====
$raw = $raw -replace '\.setTemplateId\(', '.templateId = ('
$raw = $raw -replace '\.setDisplayName\(', '.displayName = ('
$raw = $raw -replace '\.setDescription\(', '.description = ('
$raw = $raw -replace '\.setHint\(', '.hint = ('
$raw = $raw -replace '\.setSourcePackId\(', '.sourcePackId = ('
$raw = $raw -replace '\.setProgressionEnabled\(', '.progressionEnabled = ('
$raw = $raw -replace '\.setProgressionMechanicId\(', '.progressionMechanicId = ('
$raw = $raw -replace '\.setProgressionKind\(', '.progressionKind = ('
$raw = $raw -replace '\.setProgressionLabel\(', '.progressionLabel = ('
$raw = $raw -replace '\.setProgressionSingularLabel\(', '.progressionSingularLabel = ('
$raw = $raw -replace '\.setProgressionPluralLabel\(', '.progressionPluralLabel = ('
$raw = $raw -replace '\.setProgressionMaxActive\(', '.progressionMaxActive = ('
$raw = $raw -replace '\.setQuestCode\(', '.questCode = ('
$raw = $raw -replace '\.setQuestGiverProfession\(', '.questGiverProfession = ('
$raw = $raw -replace '\.setQuestPrerequisites\(', '.questPrerequisites = ('
$raw = $raw -replace '\.setQuestRepeatable\(', '.questRepeatable = ('
$raw = $raw -replace '\.setQuestCooldownSeconds\(', '.questCooldownSeconds = ('
$raw = $raw -replace '\.setQuestDialogues\(', '.questDialogues = ('
$raw = $raw -replace '\.setQuestStages\(', '.questStages = ('
$raw = $raw -replace '\.setObjectives\(', '.objectives = ('
$raw = $raw -replace '\.setRewards\(', '.rewards = ('
$raw = $raw -replace '\.setQuestContract\(', '.questContract = ('
$raw = $raw -replace '\.setTriggerProbability\(', '.triggerProbability = ('
$raw = $raw -replace '\.setMinimumNpcCount\(', '.minimumNpcCount = ('
$raw = $raw -replace '\.setRequiresPlayer\(', '.requiresPlayer = ('
$raw = $raw -replace '\.setNarrativeHints\(', '.narrativeHints = ('
$raw = $raw -replace '\.setPreferredTopologies\(', '.preferredTopologies = ('
$raw = $raw -replace '\.setRequiredProfessions\(', '.requiredProfessions = ('
$raw = $raw -replace '\.setPreferredProfessions\(', '.preferredProfessions = ('
$raw = $raw -replace '\.setRequiredTraits\(', '.requiredTraits = ('
$raw = $raw -replace '\.setPreferredTraits\(', '.preferredTraits = ('
$raw = $raw -replace '\.setEnginePlugin\(', '.enginePlugin = ('
$raw = $raw -replace '\.setId\(', '.id = ('
$raw = $raw -replace '\.setOccupation\(', '.occupation = ('

# ==== SECTION 8: Boolean isXxx() → property access ====
$raw = $raw -replace '\.isQuestRepeatable\(\)', '.questRepeatable'
$raw = $raw -replace '\.isProgressionEnabled\(\)', '.progressionEnabled'
$raw = $raw -replace '\.isOptional\(\)', '.optional'
$raw = $raw -replace '\.isPlayerRole\(\)', '.playerRole'
$raw = $raw -replace '\.isRequiresPlayer\(\)', '.requiresPlayer'
$raw = $raw -replace '\.isReplaceBaseType\(\)', '.isReplaceBaseType'
$raw = $raw -replace '\.isProgressEnabled\(\)', '.isProgressEnabled'
$raw = $raw -replace '\.isCompleted\(\)', '.isCompleted'
$raw = $raw -replace '\.isOffered\(\)', '.isOffered'
$raw = $raw -replace '\.isActive\(\)', '.isActive'
$raw = $raw -replace '\.isCurrent\(\)', '.isCurrent'
$raw = $raw -replace '\.isArchived\(\)', '.isArchived'
$raw = $raw -replace '\.isSpawned\(\)', '.isSpawned'
$raw = $raw -replace '\.isValid\(\)', '.isValid'
$raw = $raw -replace '\.isEnabled\(\)', '.isEnabled'
$raw = $raw -replace '\.isBlank\(\)', '.isBlank'

# ==== SECTION 9: Other Java patterns ====
$raw = $raw -replace 'instanceof (\w+)', 'is $1'
$raw = $raw -replace 'new\s+([A-Z]\w+)\(', '$1('
$raw = $raw -replace 'new\s+([a-z]\w+)\.([A-Z]\w+)\(', '$1.$2('
$raw = $raw -replace 'catch\s*\(\s*(\w+)\s+(\w+)\s*\)', 'catch ($2: $1)'
$raw = $raw -replace '@Override\s*\n', ''
$raw = $raw -replace 'throw new ', 'throw '
$raw = $raw -replace 'this\.', ''
$raw = $raw -replace ';(\s*)$', '$1'
$raw = $raw -replace 'List\.copyOf\(', 'listOf('
$raw = $raw -replace 'List\.of\(', 'listOf('
$raw = $raw -replace 'Collections\.unmodifiable', ''
$raw = $raw -replace 'Objects\.equals\(', ''

# ==== SECTION 10: Add imports at top ====
$imports = @"
package ro.ainpc.engine

import com.google.gson.Gson
import org.bukkit.Location
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition
import ro.ainpc.story.StoryContextService
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

"@

# Find the package line and replace everything before the class
$raw = $raw -replace '^package ro.ainpc.engine\s*$', $imports

# Remove any remaining import lines
$raw = $raw -replace '(?m)^import\s+.*\s*$', ''

# Clean up extra blank lines
$raw = $raw -replace "`r`n`r`n`r`n+", "`r`n`r`n"

Set-Content -LiteralPath $OutputFile -Value $raw -NoNewline
Write-Host "Conversion complete: $(($raw -split "`r`n").Count) lines"
