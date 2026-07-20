function Get-AinpcRequiredJsonProperty {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Object,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        throw "Contract audit invalid: lipseste $Path.$Name."
    }
    return $property
}

function Assert-AinpcJsonObject {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if ($null -eq $Value -or $Value -isnot [System.Management.Automation.PSCustomObject]) {
        throw "Contract audit invalid: $Path trebuie sa fie obiect JSON."
    }
}

function Assert-AinpcJsonArray {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if ($null -eq $Value -or $Value -isnot [System.Array]) {
        throw "Contract audit invalid: $Path trebuie sa fie array JSON."
    }
}

function Assert-AinpcJsonString {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [switch]$AllowEmpty
    )

    if ($Value -isnot [string]) {
        throw "Contract audit invalid: $Path trebuie sa fie string."
    }
    if (-not $AllowEmpty -and [string]::IsNullOrWhiteSpace([string]$Value)) {
        throw "Contract audit invalid: $Path nu poate fi gol."
    }
}

function Assert-AinpcJsonBoolean {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if ($Value -isnot [bool]) {
        throw "Contract audit invalid: $Path trebuie sa fie boolean."
    }
}

function Test-AinpcJsonInteger {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value
    )

    return $Value -is [int] -or $Value -is [long]
}

function Assert-AinpcJsonInteger {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [long]$Minimum = [long]::MinValue,
        [long]$Maximum = [long]::MaxValue
    )

    if (-not (Test-AinpcJsonInteger -Value $Value)) {
        throw "Contract audit invalid: $Path trebuie sa fie numar intreg."
    }
    $number = [long]$Value
    if ($number -lt $Minimum -or $number -gt $Maximum) {
        throw "Contract audit invalid: $Path=$number este in afara intervalului [$Minimum, $Maximum]."
    }
}

function Assert-AinpcJsonStringEquals {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [string]$Expected,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Assert-AinpcJsonString -Value $Value -Path $Path
    if ([string]$Value -cne $Expected) {
        throw "Contract audit invalid: $Path trebuie sa fie '$Expected', nu '$Value'."
    }
}

function Assert-AinpcJsonBooleanEquals {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [bool]$Expected,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Assert-AinpcJsonBoolean -Value $Value -Path $Path
    if ([bool]$Value -ne $Expected) {
        throw "Contract audit invalid: $Path trebuie sa fie $Expected."
    }
}

function Assert-AinpcJsonIntegerEquals {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [long]$Expected,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Assert-AinpcJsonInteger -Value $Value -Path $Path
    if ([long]$Value -ne $Expected) {
        throw "Contract audit invalid: $Path trebuie sa fie $Expected, nu $Value."
    }
}

function Assert-AinpcJsonNullableIntegerEquals {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Expected,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if ($null -eq $Expected) {
        if ($null -ne $Value) {
            throw "Contract audit invalid: $Path trebuie sa fie null."
        }
        return
    }
    Assert-AinpcJsonIntegerEquals -Value $Value -Expected ([long]$Expected) -Path $Path
}

function Assert-AinpcJsonStringArray {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Assert-AinpcJsonArray -Value $Value -Path $Path
    $items = @($Value)
    for ($index = 0; $index -lt $items.Count; $index++) {
        Assert-AinpcJsonString -Value $items[$index] -Path "$Path[$index]"
    }
}

function Assert-AinpcExactStringArray {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Value,
        [Parameter(Mandatory = $true)]
        [string[]]$Expected,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Assert-AinpcJsonStringArray -Value $Value -Path $Path
    $actual = @($Value)
    if ($actual.Count -ne $Expected.Count) {
        throw "Contract audit invalid: $Path are $($actual.Count) elemente, asteptat $($Expected.Count)."
    }
    for ($index = 0; $index -lt $Expected.Count; $index++) {
        if ([string]$actual[$index] -cne $Expected[$index]) {
            throw "Contract audit invalid: $Path[$index] trebuie sa fie '$($Expected[$index])'."
        }
    }
}

function Assert-AinpcStringSetEquals {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [string[]]$Actual,
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [string[]]$Expected,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $actualSet = @{}
    foreach ($item in $Actual) {
        if ($actualSet.ContainsKey($item)) {
            throw "Contract audit invalid: $Path contine duplicatul '$item'."
        }
        $actualSet[$item] = $true
    }
    $expectedSet = @{}
    foreach ($item in $Expected) {
        if ($expectedSet.ContainsKey($item)) {
            throw "Contract audit intern invalid: setul asteptat pentru $Path contine '$item' de doua ori."
        }
        $expectedSet[$item] = $true
    }
    if ($actualSet.Count -ne $expectedSet.Count) {
        throw "Contract audit invalid: $Path nu are cardinalitatea asteptata."
    }
    foreach ($item in $expectedSet.Keys) {
        if (-not $actualSet.ContainsKey($item)) {
            throw "Contract audit invalid: $Path nu contine '$item'."
        }
    }
}

function Get-AinpcExpectedAuditPlan {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Mode,
        [Parameter(Mandatory = $true)]
        [string]$Profile
    )

    $requestedSections = @(switch ($Mode) {
        "all" { @("npc", "world", "database", "spawn", "quest", "wand") }
        "npc" { @("npc") }
        "world" { @("world") }
        "db" { @("database") }
        "spawn" { @("spawn") }
        "quest" { @("quest") }
        "wand" { @("wand") }
        default { throw "Contract audit intern invalid: mod necunoscut '$Mode'." }
    })
    $sectionNames = @(switch ($Mode) {
        "all" { @("NPCs", "World Mapping", "Database", "Spawn Order", "Quest Anchors", "Wand") }
        "npc" { @("NPCs") }
        "world" { @("World Mapping") }
        "db" { @("Database") }
        "spawn" { @("Spawn Order") }
        "quest" { @("Quest Anchors") }
        "wand" { @("Wand") }
    })
    if ($Profile -notin @("standard", "strict", "full", "offline")) {
        throw "Contract audit intern invalid: profil necunoscut '$Profile'."
    }
    if ($Profile -ne "standard" -and $Mode -notin @("all", "quest")) {
        throw "Contract audit intern invalid: profilul '$Profile' nu este valid pentru modul '$Mode'."
    }

    $completePersistentScan = $Profile -in @("full", "offline")
    $completeSpawnHistory = $completePersistentScan -and $Mode -eq "all"
    return [pscustomobject]@{
        RequestedSections = $requestedSections
        SectionNames = $sectionNames
        LiveRuntimeChecks = $Profile -ne "offline"
        FailOnWarnings = $Profile -eq "strict"
        QuestAnchorLimit = if ($completePersistentScan) { $null } else { 500L }
        SpawnBatchLimit = if ($Profile -eq "standard") { 10L } else { 50L }
        ScanCompleteSpawnHistory = $completeSpawnHistory
        SpawnHistoryPageSize = if ($completeSpawnHistory) { 200L } else { $null }
    }
}

function Assert-AinpcDatabaseSchemaInventory {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Inventory,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Assert-AinpcJsonObject -Value $Inventory -Path $Path
    Assert-AinpcJsonStringEquals -Value (Get-AinpcRequiredJsonProperty $Inventory "source" $Path).Value -Expected "jdbc_metadata" -Path "$Path.source"

    $countNames = @("expected_tables", "discovered_tables", "mapped_tables", "row_counted_tables", "total_rows")
    $counts = @{}
    foreach ($name in $countNames) {
        $value = (Get-AinpcRequiredJsonProperty $Inventory $name $Path).Value
        Assert-AinpcJsonInteger -Value $value -Path "$Path.$name" -Minimum 0
        $counts[$name] = [long]$value
    }
    $coverageComplete = (Get-AinpcRequiredJsonProperty $Inventory "coverage_complete" $Path).Value
    $rowCountsComplete = (Get-AinpcRequiredJsonProperty $Inventory "row_counts_complete" $Path).Value
    Assert-AinpcJsonBoolean -Value $coverageComplete -Path "$Path.coverage_complete"
    Assert-AinpcJsonBoolean -Value $rowCountsComplete -Path "$Path.row_counts_complete"

    $missingTablesValue = (Get-AinpcRequiredJsonProperty $Inventory "missing_tables" $Path).Value
    $unmappedTablesValue = (Get-AinpcRequiredJsonProperty $Inventory "unmapped_tables" $Path).Value
    Assert-AinpcJsonStringArray -Value $missingTablesValue -Path "$Path.missing_tables"
    Assert-AinpcJsonStringArray -Value $unmappedTablesValue -Path "$Path.unmapped_tables"
    $missingTables = @($missingTablesValue)
    $unmappedTables = @($unmappedTablesValue)

    $domainsValue = (Get-AinpcRequiredJsonProperty $Inventory "domains" $Path).Value
    Assert-AinpcJsonArray -Value $domainsValue -Path "$Path.domains"
    $domains = @($domainsValue)
    $expectedDomainNames = @("npc", "dialog", "quest", "world", "spawn", "story", "progression", "economy", "system")
    if ($domains.Count -ne $expectedDomainNames.Count) {
        throw "Contract audit invalid: $Path.domains trebuie sa contina cele noua domenii."
    }

    $domainRecords = @()
    $allExpectedTables = @()
    $allMissingTables = @()
    for ($index = 0; $index -lt $domains.Count; $index++) {
        $domainPath = "$Path.domains[$index]"
        $domain = $domains[$index]
        Assert-AinpcJsonObject -Value $domain -Path $domainPath
        $name = (Get-AinpcRequiredJsonProperty $domain "name" $domainPath).Value
        Assert-AinpcJsonStringEquals -Value $name -Expected $expectedDomainNames[$index] -Path "$domainPath.name"

        $expectedValue = (Get-AinpcRequiredJsonProperty $domain "expected_tables" $domainPath).Value
        $presentValue = (Get-AinpcRequiredJsonProperty $domain "present_tables" $domainPath).Value
        $missingValue = (Get-AinpcRequiredJsonProperty $domain "missing_tables" $domainPath).Value
        Assert-AinpcJsonStringArray -Value $expectedValue -Path "$domainPath.expected_tables"
        Assert-AinpcJsonStringArray -Value $presentValue -Path "$domainPath.present_tables"
        Assert-AinpcJsonStringArray -Value $missingValue -Path "$domainPath.missing_tables"
        $expected = @($expectedValue)
        $present = @($presentValue)
        $missing = @($missingValue)
        Assert-AinpcStringSetEquals -Actual @($present + $missing) -Expected $expected -Path "$domainPath.present_tables+missing_tables"

        $rowCounted = (Get-AinpcRequiredJsonProperty $domain "row_counted_tables" $domainPath).Value
        $totalRows = (Get-AinpcRequiredJsonProperty $domain "total_rows" $domainPath).Value
        Assert-AinpcJsonInteger -Value $rowCounted -Path "$domainPath.row_counted_tables" -Minimum 0
        Assert-AinpcJsonInteger -Value $totalRows -Path "$domainPath.total_rows" -Minimum 0
        if ([long]$rowCounted -gt $present.Count) {
            throw "Contract audit invalid: $domainPath.row_counted_tables depaseste tabelele prezente."
        }

        $domainRecords += [pscustomobject]@{
            Name = [string]$name
            Expected = $expected
            Present = $present
            Missing = $missing
            RowCounted = [long]$rowCounted
            TotalRows = [long]$totalRows
        }
        $allExpectedTables += $expected
        $allMissingTables += $missing
    }
    Assert-AinpcStringSetEquals -Actual $missingTables -Expected $allMissingTables -Path "$Path.missing_tables"
    Assert-AinpcStringSetEquals -Actual $allExpectedTables -Expected $allExpectedTables -Path "$Path.domains.expected_tables"

    $tablesValue = (Get-AinpcRequiredJsonProperty $Inventory "tables" $Path).Value
    Assert-AinpcJsonArray -Value $tablesValue -Path "$Path.tables"
    $tables = @($tablesValue)
    $tableRecords = @()
    $tableNames = @()
    for ($index = 0; $index -lt $tables.Count; $index++) {
        $tablePath = "$Path.tables[$index]"
        $table = $tables[$index]
        Assert-AinpcJsonObject -Value $table -Path $tablePath
        $name = (Get-AinpcRequiredJsonProperty $table "name" $tablePath).Value
        Assert-AinpcJsonString -Value $name -Path "$tablePath.name"
        $tableNames += [string]$name

        $domain = (Get-AinpcRequiredJsonProperty $table "domain" $tablePath).Value
        if ($null -ne $domain) {
            Assert-AinpcJsonString -Value $domain -Path "$tablePath.domain"
            if ([string]$domain -notin $expectedDomainNames) {
                throw "Contract audit invalid: $tablePath.domain='$domain' este necunoscut."
            }
        }
        $expected = (Get-AinpcRequiredJsonProperty $table "expected" $tablePath).Value
        Assert-AinpcJsonBoolean -Value $expected -Path "$tablePath.expected"
        if ([bool]$expected -and $null -eq $domain) {
            throw "Contract audit invalid: $tablePath.domain lipseste pentru un tabel asteptat."
        }
        if (-not [bool]$expected -and $null -ne $domain) {
            throw "Contract audit invalid: $tablePath.domain trebuie sa fie null pentru un tabel nemapat."
        }

        $rowCount = (Get-AinpcRequiredJsonProperty $table "row_count" $tablePath).Value
        $rowCountError = (Get-AinpcRequiredJsonProperty $table "row_count_error" $tablePath).Value
        if ($null -ne $rowCount) {
            Assert-AinpcJsonInteger -Value $rowCount -Path "$tablePath.row_count" -Minimum 0
        }
        if ($null -ne $rowCountError) {
            Assert-AinpcJsonString -Value $rowCountError -Path "$tablePath.row_count_error"
        }
        if (($null -eq $rowCount) -eq ($null -eq $rowCountError)) {
            throw "Contract audit invalid: $tablePath trebuie sa aiba exact unul dintre row_count si row_count_error."
        }

        $tableRecords += [pscustomobject]@{
            Name = [string]$name
            Domain = if ($null -eq $domain) { $null } else { [string]$domain }
            Expected = [bool]$expected
            RowCount = if ($null -eq $rowCount) { $null } else { [long]$rowCount }
        }
    }
    Assert-AinpcStringSetEquals -Actual $tableNames -Expected $tableNames -Path "$Path.tables.name"

    $derivedUnmapped = @($tableRecords | Where-Object { -not $_.Expected } | ForEach-Object { $_.Name })
    Assert-AinpcStringSetEquals -Actual $unmappedTables -Expected $derivedUnmapped -Path "$Path.unmapped_tables"
    foreach ($domainRecord in $domainRecords) {
        $domainTables = @($tableRecords | Where-Object { $_.Expected -and $_.Domain -eq $domainRecord.Name })
        Assert-AinpcStringSetEquals -Actual @($domainTables | ForEach-Object { $_.Name }) -Expected $domainRecord.Present -Path "$Path.domains.$($domainRecord.Name).present_tables"
        $countedDomainTables = @($domainTables | Where-Object { $null -ne $_.RowCount })
        $domainRows = [long]0
        foreach ($tableRecord in $countedDomainTables) {
            $domainRows += [long]$tableRecord.RowCount
        }
        if ($domainRecord.RowCounted -ne $countedDomainTables.Count -or $domainRecord.TotalRows -ne $domainRows) {
            throw "Contract audit invalid: totalurile domeniului '$($domainRecord.Name)' nu se reconciliaza."
        }
    }

    $mappedTables = @($tableRecords | Where-Object { $_.Expected })
    $rowCountedTables = @($tableRecords | Where-Object { $null -ne $_.RowCount })
    $totalRows = [long]0
    foreach ($tableRecord in $rowCountedTables) {
        $totalRows += [long]$tableRecord.RowCount
    }
    if ($counts.expected_tables -ne $allExpectedTables.Count -or
        $counts.discovered_tables -ne $tableRecords.Count -or
        $counts.mapped_tables -ne $mappedTables.Count -or
        $counts.row_counted_tables -ne $rowCountedTables.Count -or
        $counts.total_rows -ne $totalRows) {
        throw "Contract audit invalid: totalurile $Path nu se reconciliaza."
    }
    $derivedCoverageComplete = $missingTables.Count -eq 0 -and $unmappedTables.Count -eq 0
    $derivedRowCountsComplete = $rowCountedTables.Count -eq $tableRecords.Count
    if ([bool]$coverageComplete -ne $derivedCoverageComplete -or [bool]$rowCountsComplete -ne $derivedRowCountsComplete) {
        throw "Contract audit invalid: flag-urile de completitudine $Path nu se reconciliaza."
    }
}

function Assert-AinpcSpawnHistory {
    param(
        [Parameter(Mandatory = $true)]
        [object]$History,
        [Parameter(Mandatory = $true)]
        [long]$ExpectedPageSize,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Assert-AinpcJsonObject -Value $History -Path $Path
    $complete = (Get-AinpcRequiredJsonProperty $History "complete" $Path).Value
    Assert-AinpcJsonBoolean -Value $complete -Path "$Path.complete"
    $numberNames = @("page_size", "pages_read", "total_batches", "scanned_batches", "problematic_batches", "unknown_status_batches")
    $numbers = @{}
    foreach ($name in $numberNames) {
        $value = (Get-AinpcRequiredJsonProperty $History $name $Path).Value
        Assert-AinpcJsonInteger -Value $value -Path "$Path.$name" -Minimum 0
        $numbers[$name] = [long]$value
    }
    if ($numbers.page_size -ne $ExpectedPageSize) {
        throw "Contract audit invalid: $Path.page_size trebuie sa fie $ExpectedPageSize."
    }
    if ($numbers.problematic_batches -gt $numbers.total_batches -or
        $numbers.unknown_status_batches -gt $numbers.total_batches) {
        throw "Contract audit invalid: totalurile problematice din $Path depasesc total_batches."
    }

    $statusCounts = (Get-AinpcRequiredJsonProperty $History "status_counts" $Path).Value
    Assert-AinpcJsonObject -Value $statusCounts -Path "$Path.status_counts"
    $statusTotal = [long]0
    $derivedProblematic = [long]0
    $derivedUnknown = [long]0
    foreach ($property in @($statusCounts.PSObject.Properties)) {
        if ([string]::IsNullOrWhiteSpace($property.Name)) {
            throw "Contract audit invalid: $Path.status_counts contine un status gol."
        }
        Assert-AinpcJsonInteger -Value $property.Value -Path "$Path.status_counts.$($property.Name)" -Minimum 0
        $count = [long]$property.Value
        $statusTotal += $count
        if ($property.Name.ToUpperInvariant() -in @("RUNNING", "FAILED", "ROLLED_BACK")) {
            $derivedProblematic += $count
        }
        if ($property.Name -ceq "<UNKNOWN>") {
            $derivedUnknown = $count
        }
    }
    if ($statusTotal -ne $numbers.total_batches -or
        $derivedProblematic -ne $numbers.problematic_batches -or
        $derivedUnknown -ne $numbers.unknown_status_batches) {
        throw "Contract audit invalid: status_counts din $Path nu se reconciliaza."
    }

    $derivedComplete = $numbers.scanned_batches -eq $numbers.total_batches
    if ([bool]$complete -ne $derivedComplete) {
        throw "Contract audit invalid: $Path.complete nu corespunde randurilor scanate."
    }
    $expectedPages = if ($numbers.scanned_batches -eq 0) {
        0L
    } else {
        [long][Math]::Ceiling([double]$numbers.scanned_batches / [double]$numbers.page_size)
    }
    if ($numbers.pages_read -ne $expectedPages) {
        throw "Contract audit invalid: $Path.pages_read nu corespunde marimii paginii."
    }
}

function ConvertFrom-AinpcAuditResponse {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyString()]
        [string]$Response,
        [Parameter(Mandatory = $true)]
        [string]$ExpectedMode,
        [Parameter(Mandatory = $true)]
        [string]$ExpectedProfile
    )

    if ([string]::IsNullOrWhiteSpace($Response)) {
        throw "Contract audit invalid: raspunsul este gol."
    }
    try {
        $document = ConvertFrom-Json -InputObject $Response -ErrorAction Stop
    } catch {
        throw "Contract audit invalid: raspunsul nu este un singur document JSON valid: $($_.Exception.Message)"
    }
    Assert-AinpcJsonObject -Value $document -Path "root"
    $plan = Get-AinpcExpectedAuditPlan -Mode $ExpectedMode -Profile $ExpectedProfile

    Assert-AinpcJsonIntegerEquals -Value (Get-AinpcRequiredJsonProperty $document "schema_version" "root").Value -Expected 1 -Path "root.schema_version"
    Assert-AinpcJsonStringEquals -Value (Get-AinpcRequiredJsonProperty $document "document_type" "root").Value -Expected "ainpc-audit-report" -Path "root.document_type"
    $generatedAt = (Get-AinpcRequiredJsonProperty $document "generated_at" "root").Value
    Assert-AinpcJsonString -Value $generatedAt -Path "root.generated_at"
    if ([string]$generatedAt -notmatch '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z$') {
        throw "Contract audit invalid: root.generated_at nu este un instant UTC ISO-8601."
    }
    $parsedTimestamp = [DateTimeOffset]::MinValue
    $timestampValid = [DateTimeOffset]::TryParse(
        [string]$generatedAt,
        [Globalization.CultureInfo]::InvariantCulture,
        [Globalization.DateTimeStyles]::RoundtripKind,
        [ref]$parsedTimestamp
    )
    if (-not $timestampValid) {
        throw "Contract audit invalid: root.generated_at nu este o data calendaristica valida."
    }
    Assert-AinpcJsonStringEquals -Value (Get-AinpcRequiredJsonProperty $document "mode" "root").Value -Expected $ExpectedMode -Path "root.mode"
    Assert-AinpcJsonStringEquals -Value (Get-AinpcRequiredJsonProperty $document "profile" "root").Value -Expected $ExpectedProfile -Path "root.profile"

    $execution = (Get-AinpcRequiredJsonProperty $document "execution" "root").Value
    Assert-AinpcJsonObject -Value $execution -Path "root.execution"
    Assert-AinpcExactStringArray -Value (Get-AinpcRequiredJsonProperty $execution "sections_requested" "root.execution").Value -Expected $plan.RequestedSections -Path "root.execution.sections_requested"
    Assert-AinpcJsonBooleanEquals -Value (Get-AinpcRequiredJsonProperty $execution "live_runtime_checks" "root.execution").Value -Expected $plan.LiveRuntimeChecks -Path "root.execution.live_runtime_checks"
    Assert-AinpcJsonBooleanEquals -Value (Get-AinpcRequiredJsonProperty $execution "fail_on_warnings" "root.execution").Value -Expected $plan.FailOnWarnings -Path "root.execution.fail_on_warnings"
    Assert-AinpcJsonNullableIntegerEquals -Value (Get-AinpcRequiredJsonProperty $execution "quest_anchor_limit" "root.execution").Value -Expected $plan.QuestAnchorLimit -Path "root.execution.quest_anchor_limit"
    Assert-AinpcJsonIntegerEquals -Value (Get-AinpcRequiredJsonProperty $execution "spawn_batch_limit" "root.execution").Value -Expected $plan.SpawnBatchLimit -Path "root.execution.spawn_batch_limit"
    Assert-AinpcJsonBooleanEquals -Value (Get-AinpcRequiredJsonProperty $execution "scan_complete_spawn_history" "root.execution").Value -Expected $plan.ScanCompleteSpawnHistory -Path "root.execution.scan_complete_spawn_history"
    Assert-AinpcJsonNullableIntegerEquals -Value (Get-AinpcRequiredJsonProperty $execution "spawn_history_page_size" "root.execution").Value -Expected $plan.SpawnHistoryPageSize -Path "root.execution.spawn_history_page_size"

    $summary = (Get-AinpcRequiredJsonProperty $document "summary" "root").Value
    Assert-AinpcJsonObject -Value $summary -Path "root.summary"
    $summaryCounts = @{}
    foreach ($name in @("errors", "warnings", "infos", "total_findings", "retained_findings", "omitted_findings")) {
        $value = (Get-AinpcRequiredJsonProperty $summary $name "root.summary").Value
        Assert-AinpcJsonInteger -Value $value -Path "root.summary.$name" -Minimum 0
        $summaryCounts[$name] = [long]$value
    }
    $truncated = (Get-AinpcRequiredJsonProperty $summary "truncated" "root.summary").Value
    Assert-AinpcJsonBoolean -Value $truncated -Path "root.summary.truncated"
    Assert-AinpcJsonIntegerEquals -Value (Get-AinpcRequiredJsonProperty $summary "retained_findings_per_section_limit" "root.summary").Value -Expected 100 -Path "root.summary.retained_findings_per_section_limit"
    if ($summaryCounts.total_findings -ne ($summaryCounts.errors + $summaryCounts.warnings + $summaryCounts.infos) -or
        $summaryCounts.omitted_findings -ne ($summaryCounts.total_findings - $summaryCounts.retained_findings) -or
        $summaryCounts.retained_findings -gt $summaryCounts.total_findings -or
        [bool]$truncated -ne ($summaryCounts.omitted_findings -gt 0)) {
        throw "Contract audit invalid: totalurile root.summary nu se reconciliaza."
    }

    $sectionsValue = (Get-AinpcRequiredJsonProperty $document "sections" "root").Value
    Assert-AinpcJsonArray -Value $sectionsValue -Path "root.sections"
    $sections = @($sectionsValue)
    if ($sections.Count -ne $plan.SectionNames.Count) {
        throw "Contract audit invalid: root.sections are $($sections.Count) sectiuni, asteptat $($plan.SectionNames.Count)."
    }
    $sectionTotal = [long]0
    $sectionRetained = [long]0
    $sectionOmitted = [long]0
    $retainedSeverityCounts = @{ INFO = 0L; WARN = 0L; ERROR = 0L }
    for ($sectionIndex = 0; $sectionIndex -lt $sections.Count; $sectionIndex++) {
        $sectionPath = "root.sections[$sectionIndex]"
        $section = $sections[$sectionIndex]
        Assert-AinpcJsonObject -Value $section -Path $sectionPath
        Assert-AinpcJsonStringEquals -Value (Get-AinpcRequiredJsonProperty $section "name" $sectionPath).Value -Expected $plan.SectionNames[$sectionIndex] -Path "$sectionPath.name"
        $total = (Get-AinpcRequiredJsonProperty $section "total_findings" $sectionPath).Value
        $retained = (Get-AinpcRequiredJsonProperty $section "retained_findings" $sectionPath).Value
        $omitted = (Get-AinpcRequiredJsonProperty $section "omitted_findings" $sectionPath).Value
        Assert-AinpcJsonInteger -Value $total -Path "$sectionPath.total_findings" -Minimum 0
        Assert-AinpcJsonInteger -Value $retained -Path "$sectionPath.retained_findings" -Minimum 0 -Maximum 100
        Assert-AinpcJsonInteger -Value $omitted -Path "$sectionPath.omitted_findings" -Minimum 0
        $total = [long]$total
        $retained = [long]$retained
        $omitted = [long]$omitted
        if ($retained -gt $total -or $omitted -ne ($total - $retained)) {
            throw "Contract audit invalid: totalurile $sectionPath nu se reconciliaza."
        }

        $findingsValue = (Get-AinpcRequiredJsonProperty $section "findings" $sectionPath).Value
        Assert-AinpcJsonArray -Value $findingsValue -Path "$sectionPath.findings"
        $findings = @($findingsValue)
        if ($findings.Count -ne $retained) {
            throw "Contract audit invalid: $sectionPath.findings nu corespunde retained_findings."
        }
        for ($findingIndex = 0; $findingIndex -lt $findings.Count; $findingIndex++) {
            $findingPath = "$sectionPath.findings[$findingIndex]"
            $finding = $findings[$findingIndex]
            Assert-AinpcJsonObject -Value $finding -Path $findingPath
            $severity = (Get-AinpcRequiredJsonProperty $finding "severity" $findingPath).Value
            Assert-AinpcJsonString -Value $severity -Path "$findingPath.severity"
            if ([string]$severity -notin @("INFO", "WARN", "ERROR")) {
                throw "Contract audit invalid: $findingPath.severity='$severity' este necunoscuta."
            }
            $retainedSeverityCounts[[string]$severity] = [long]$retainedSeverityCounts[[string]$severity] + 1L
            Assert-AinpcJsonString -Value (Get-AinpcRequiredJsonProperty $finding "message" $findingPath).Value -Path "$findingPath.message"
        }
        $sectionTotal += $total
        $sectionRetained += $retained
        $sectionOmitted += $omitted
    }
    if ($sectionTotal -ne $summaryCounts.total_findings -or
        $sectionRetained -ne $summaryCounts.retained_findings -or
        $sectionOmitted -ne $summaryCounts.omitted_findings) {
        throw "Contract audit invalid: sectiunile nu se reconciliaza cu root.summary."
    }
    if ($retainedSeverityCounts.ERROR -gt $summaryCounts.errors -or
        $retainedSeverityCounts.WARN -gt $summaryCounts.warnings -or
        $retainedSeverityCounts.INFO -gt $summaryCounts.infos) {
        throw "Contract audit invalid: severitatile retinute depasesc root.summary."
    }
    if ($summaryCounts.omitted_findings -eq 0 -and
        ($retainedSeverityCounts.ERROR -ne $summaryCounts.errors -or
            $retainedSeverityCounts.WARN -ne $summaryCounts.warnings -or
            $retainedSeverityCounts.INFO -ne $summaryCounts.infos)) {
        throw "Contract audit invalid: severitatile complete nu se reconciliaza cu root.summary."
    }

    $expectedVerdict = if ($summaryCounts.errors -gt 0 -or ($summaryCounts.warnings -gt 0 -and $plan.FailOnWarnings)) {
        "FAIL"
    } elseif ($summaryCounts.warnings -gt 0) {
        "WARN"
    } else {
        "PASS"
    }
    $expectedExitCode = switch ($expectedVerdict) {
        "PASS" { 0L }
        "WARN" { 1L }
        "FAIL" { 2L }
    }
    Assert-AinpcJsonStringEquals -Value (Get-AinpcRequiredJsonProperty $document "verdict" "root").Value -Expected $expectedVerdict -Path "root.verdict"
    Assert-AinpcJsonIntegerEquals -Value (Get-AinpcRequiredJsonProperty $document "exit_code" "root").Value -Expected $expectedExitCode -Path "root.exit_code"

    $databaseSchema = (Get-AinpcRequiredJsonProperty $document "database_schema" "root").Value
    $databaseRequested = $plan.RequestedSections -contains "database"
    if ($null -ne $databaseSchema) {
        if (-not $databaseRequested) {
            throw "Contract audit invalid: root.database_schema este prezent fara sectiunea database."
        }
        Assert-AinpcDatabaseSchemaInventory -Inventory $databaseSchema -Path "root.database_schema"
    }
    $spawnHistory = (Get-AinpcRequiredJsonProperty $document "spawn_history" "root").Value
    if ($null -ne $spawnHistory) {
        if (-not $plan.ScanCompleteSpawnHistory) {
            throw "Contract audit invalid: root.spawn_history este prezent fara scanare completa."
        }
        Assert-AinpcSpawnHistory -History $spawnHistory -ExpectedPageSize $plan.SpawnHistoryPageSize -Path "root.spawn_history"
    }

    return $document
}
