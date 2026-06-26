[CmdletBinding()]
param(
    [string]$ProjectRoot = "",
    [string]$CursorFile = "docs/deepseek/deepseek-execution-cursor.md",
    [string]$LedgerFile = "docs/deepseek/deepseek-execution-ledger.json",
    [string]$OutFile = "",
    [string]$Mark = "",
    [ValidateSet("DONE", "CANCELLED", "BLOCKED", "PARTIAL", "NEEDS_REVIEW")]
    [string]$MarkStatus = "DONE",
    [string]$Reason = "",
    [string]$Evidence = "",
    [switch]$FailOnBlocked
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-ProjectPath {
    param([string]$Path)
    if ([string]::IsNullOrWhiteSpace($Path)) {
        return (Get-Location).Path
    }
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return (Join-Path (Get-Location).Path $Path)
}

function Resolve-RepoPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return (Join-Path $Root $Path)
}

function Format-TaskId {
    param([int]$Number)
    return ("L{0:d3}" -f $Number)
}

function Normalize-TaskId {
    param([string]$Value)
    $match = [regex]::Match($Value, "L?(?<number>\d+)", "IgnoreCase")
    if (-not $match.Success) {
        throw "Invalid task id: $Value"
    }
    return Format-TaskId ([int]$match.Groups["number"].Value)
}

function Get-CurrentTaskNumber {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return 1
    }

    $raw = Get-Content -LiteralPath $Path -Raw -ErrorAction Stop
    $match = [regex]::Match($raw, 'Current task:\s*`?L(?<number>\d+)`?')
    if (-not $match.Success) {
        return 1
    }

    return [int]$match.Groups["number"].Value
}

function Set-CurrentTaskNumber {
    param(
        [string]$Path,
        [int]$Number
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    $taskId = Format-TaskId $Number
    $line = "- Current task: ``$taskId``"
    $raw = Get-Content -LiteralPath $Path -Raw -ErrorAction Stop
    if ($raw -match '(?m)^- Current task:\s*`?L\d+`?') {
        $updated = $raw -replace '(?m)^- Current task:\s*`?L\d+`?', $line
        Set-Content -LiteralPath $Path -Value $updated -Encoding utf8
    }
}

function Read-StatusMap {
    param([string]$Path)

    $map = [ordered]@{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $map
    }

    $raw = Get-Content -LiteralPath $Path -Raw -ErrorAction Stop
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return $map
    }

    $ledger = $raw | ConvertFrom-Json -ErrorAction Stop
    if ($null -eq $ledger.PSObject.Properties["statuses"]) {
        return $map
    }

    foreach ($property in $ledger.statuses.PSObject.Properties) {
        $map[$property.Name] = $property.Value
    }
    return $map
}

function Save-StatusMap {
    param(
        [string]$Path,
        [System.Collections.Specialized.OrderedDictionary]$StatusMap
    )

    $ledger = [ordered]@{
        version = 1
        updated_utc = (Get-Date).ToUniversalTime().ToString("o")
        policy = "strict_ascending"
        statuses = $StatusMap
    }

    $json = $ledger | ConvertTo-Json -Depth 10
    Set-Content -LiteralPath $Path -Value $json -Encoding utf8
}

function Get-TaskStatus {
    param([string]$Section)

    $statusMatch = [regex]::Match($Section, "(?mi)^\s*(?:\*\*Status:\*\*|Status:)\s*(?<status>[A-Z_]+)")
    if (-not $statusMatch.Success) {
        return "READY"
    }

    return $statusMatch.Groups["status"].Value.ToUpperInvariant()
}

function Get-LedgerStatus {
    param(
        [System.Collections.Specialized.OrderedDictionary]$StatusMap,
        [string]$TaskId
    )

    if (-not $StatusMap.Contains($TaskId)) {
        return $null
    }

    $entry = $StatusMap[$TaskId]
    if ($entry -is [System.Collections.IDictionary]) {
        if ($entry.Contains("status")) {
            return [string]$entry["status"]
        }
        return $null
    }

    if ($null -eq $entry.PSObject.Properties["status"]) {
        return $null
    }
    return [string]$entry.status
}

function Test-RequiredFields {
    param([string]$Section)

    $missing = @()
    foreach ($field in @("Descriere tehnica", "Scop", "Target", "Prompt AI", "Acceptare")) {
        $marker = "**$($field):**"
        if ($Section -notmatch [regex]::Escape($marker)) {
            $missing += $field
        }
    }
    return $missing
}

function Get-DeepSeekTasks {
    param(
        [string]$DocsDir,
        [System.Collections.Specialized.OrderedDictionary]$StatusMap
    )

    $tasks = @()
    $issues = @()
    $files = Get-ChildItem -LiteralPath $DocsDir -Filter "deepseek-taskuri-late-50*.md" -File |
        Sort-Object Name

    foreach ($file in $files) {
        $raw = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction Stop
        $documentStatus = if ($raw -match "(?mi)^Status:\s*draft incomplet") { "DRAFT_INCOMPLETE" } else { "ACTIVE" }

        $headingMatches = [regex]::Matches($raw, "(?m)^### L(?<start>\d+)(?:-L(?<end>\d+))?\s*(?<title>.*)$")
        for ($index = 0; $index -lt $headingMatches.Count; $index++) {
            $heading = $headingMatches[$index]
            $startNumber = [int]$heading.Groups["start"].Value
            $taskId = Format-TaskId $startNumber
            $endValue = $heading.Groups["end"].Value
            $title = $heading.Groups["title"].Value.Trim()
            $sectionStart = $heading.Index
            $sectionEnd = if ($index + 1 -lt $headingMatches.Count) { $headingMatches[$index + 1].Index } else { $raw.Length }
            $section = $raw.Substring($sectionStart, $sectionEnd - $sectionStart)
            $line = ($raw.Substring(0, $heading.Index) -split "`r?`n").Count

            if (-not [string]::IsNullOrWhiteSpace($endValue)) {
                $issues += [pscustomobject]@{
                    type = "range_heading"
                    task = "L$startNumber-L$endValue"
                    file = $file.Name
                    line = $line
                    message = "Range headings are reservations, not executable tasks."
                }
            }

            $missingFields = Test-RequiredFields $section
            if ($documentStatus -eq "ACTIVE" -and @($missingFields).Count -gt 0) {
                $issues += [pscustomobject]@{
                    type = "missing_required_fields"
                    task = $taskId
                    file = $file.Name
                    line = $line
                    missing = @($missingFields)
                }
            }

            $sectionStatus = Get-TaskStatus $section
            $ledgerStatus = Get-LedgerStatus $StatusMap $taskId
            $effectiveStatus = if ([string]::IsNullOrWhiteSpace($ledgerStatus)) { $sectionStatus } else { $ledgerStatus.ToUpperInvariant() }

            $tasks += [pscustomobject]@{
                number = $startNumber
                id = $taskId
                title = $title
                file = $file.Name
                path = $file.FullName
                line = $line
                status = $effectiveStatus
                status_source = if ([string]::IsNullOrWhiteSpace($ledgerStatus)) { "task" } else { "ledger" }
                document_status = $documentStatus
                range_end = if ([string]::IsNullOrWhiteSpace($endValue)) { $null } else { [int]$endValue }
                missing_fields = @($missingFields)
            }
        }
    }

    return [pscustomobject]@{
        tasks = @($tasks)
        issues = @($issues)
    }
}

function Get-DeepSeekReservations {
    param([string]$DocsDir)

    $reservations = @()
    $files = Get-ChildItem -LiteralPath $DocsDir -Filter "deepseek-taskuri-late-50*.md" -File |
        Sort-Object Name

    foreach ($file in $files) {
        $raw = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction Stop
        $documentStatus = if ($raw -match "(?mi)^Status:\s*draft incomplet") { "DRAFT_INCOMPLETE" } else { "ACTIVE" }
        if ($documentStatus -ne "DRAFT_INCOMPLETE") {
            continue
        }

        $matches = [regex]::Matches($raw, "(?m)^##\s+Rezervare\s+L(?<start>\d+)-L(?<end>\d+)\b")
        foreach ($match in $matches) {
            $startNumber = [int]$match.Groups["start"].Value
            $endNumber = [int]$match.Groups["end"].Value
            $line = ($raw.Substring(0, $match.Index) -split "`r?`n").Count
            $reservations += [pscustomobject]@{
                start = $startNumber
                end = $endNumber
                file = $file.Name
                line = $line
            }
        }
    }

    return @($reservations)
}

function Get-ArchiveIssues {
    param([string]$DocsDir)

    $issues = @()
    $archiveDir = Join-Path $DocsDir "arhiva"
    if (-not (Test-Path -LiteralPath $archiveDir)) {
        return $issues
    }

    $activeFiles = Get-ChildItem -LiteralPath $DocsDir -Filter "deepseek-taskuri*.md" -File |
        Select-Object -ExpandProperty Name
    $archiveFiles = Get-ChildItem -LiteralPath $archiveDir -Filter "deepseek-taskuri*.md" -File |
        Select-Object -ExpandProperty Name

    foreach ($archiveName in $archiveFiles) {
        if ($activeFiles -contains $archiveName) {
            $issues += [pscustomobject]@{
                type = "duplicate_active_archive"
                task = ""
                file = $archiveName
                line = 0
                message = "A DeepSeek batch with the same file name exists in both active docs and archive."
            }
        }
    }

    $archivePrefix = (Get-Item -LiteralPath $archiveDir).FullName.TrimEnd('\', '/')
    $docsFiles = Get-ChildItem -LiteralPath $DocsDir -Recurse -Filter "*.md" -File |
        Where-Object { -not $_.FullName.StartsWith($archivePrefix, [System.StringComparison]::OrdinalIgnoreCase) }

    foreach ($archiveName in $archiveFiles) {
        foreach ($file in $docsFiles) {
            $raw = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction Stop
            if ($raw -notmatch [regex]::Escape($archiveName)) {
                continue
            }

            $archiveForward = "docs/deepseek/arhiva/$archiveName"
            $archiveBackslash = "docs\deepseek\arhiva\$archiveName"
            $archiveRelative = "arhiva/$archiveName"
            $archiveRelativeBackslash = "arhiva\$archiveName"
            if ($raw -match [regex]::Escape($archiveForward) -or
                $raw -match [regex]::Escape($archiveRelative) -or
                $raw -match [regex]::Escape($archiveBackslash) -or
                $raw -match [regex]::Escape($archiveRelativeBackslash)) {
                continue
            }

            $legacyLinkFound = $false
            $linkMatches = [regex]::Matches($raw, '\[[^\]]+\]\((?<target>[^)]+)\)')
            foreach ($link in $linkMatches) {
                $target = $link.Groups["target"].Value.Trim()
                if ($target.StartsWith("http", [System.StringComparison]::OrdinalIgnoreCase) -or
                    $target.StartsWith("#") -or
                    $target.StartsWith("mailto:", [System.StringComparison]::OrdinalIgnoreCase)) {
                    continue
                }

                $target = ($target -split "#")[0]
                if ([string]::IsNullOrWhiteSpace($target)) {
                    continue
                }

                if ([System.IO.Path]::GetFileName($target) -eq $archiveName -and
                    $target -notmatch [regex]::Escape($archiveForward) -and
                    $target -notmatch [regex]::Escape($archiveBackslash)) {
                    $legacyLinkFound = $true
                    break
                }
            }

            if ($legacyLinkFound) {
                $issues += [pscustomobject]@{
                    type = "legacy_archive_link"
                    task = ""
                    file = $file.Name
                    line = 0
                    message = "Legacy link target for archived batch '$archiveName' must use the archive path."
                }
                continue
            }

            $issues += [pscustomobject]@{
                type = "stale_archive_reference"
                task = ""
                file = $file.Name
                line = 0
                message = "Reference to archived batch '$archiveName' does not include the archive path."
            }
        }
    }

    return $issues
}

function Get-NumberingIssues {
    param(
        [array]$Tasks,
        [array]$Reservations = @()
    )

    $issues = @()
    $ordered = $Tasks | Sort-Object number, file
    if (@($ordered).Count -eq 0) {
        return $issues
    }

    $expected = $ordered[0].number
    foreach ($task in $ordered) {
        $taskEnd = if ($null -eq $task.range_end) { $task.number } else { [int]$task.range_end }

        if ($task.number -gt $expected) {
            while ($task.number -gt $expected) {
                $reservation = $Reservations |
                    Where-Object { $_.start -eq $expected -and $_.end -lt $task.number } |
                    Sort-Object end -Descending |
                    Select-Object -First 1

                if ($null -eq $reservation) {
                    break
                }

                $expected = [int]$reservation.end + 1
            }

            if ($task.number -gt $expected) {
                $issues += [pscustomobject]@{
                    type = "numbering_gap"
                    task = "$(Format-TaskId $expected)-$(Format-TaskId ($task.number - 1))"
                    file = $task.file
                    line = $task.line
                    message = "DeepSeek numbering has a gap before $($task.id)."
                }
            }
        } elseif ($task.number -lt $expected) {
            $issues += [pscustomobject]@{
                type = "numbering_overlap"
                task = $task.id
                file = $task.file
                line = $task.line
                message = "DeepSeek numbering overlaps or repeats an earlier range."
            }
        }

        $nextExpected = $taskEnd + 1
        if ($nextExpected -gt $expected) {
            $expected = $nextExpected
        }
    }

    return $issues
}

function Get-BatchOrdinal {
    param([string]$Name)

    $match = [regex]::Match($Name, '^deepseek-taskuri-late-50-(?<number>\d+)\.md$')
    if ($match.Success) {
        return [int]$match.Groups["number"].Value
    }
    if ($Name -eq "deepseek-taskuri-late-50.md") {
        return 1
    }
    return $null
}

function Get-IndexIssues {
    param([string]$DocsDir)

    $issues = @()
    $readmePath = Join-Path $DocsDir "README.md"
    if (-not (Test-Path -LiteralPath $readmePath)) {
        return $issues
    }

    $activeFiles = Get-ChildItem -LiteralPath $DocsDir -Filter "deepseek-taskuri-late-50*.md" -File |
        Select-Object -ExpandProperty Name
    $raw = Get-Content -LiteralPath $readmePath -Raw -ErrorAction Stop

    foreach ($activeName in $activeFiles) {
        if ($raw -notmatch [regex]::Escape($activeName)) {
            $issues += [pscustomobject]@{
                type = "missing_active_index_entry"
                task = ""
                file = "README.md"
                line = 0
                message = "Active DeepSeek batch '$activeName' is not listed in docs/README.md."
            }
        }
    }

    $lines = Get-Content -LiteralPath $readmePath -ErrorAction Stop
    $listed = @()
    for ($index = 0; $index -lt $lines.Count; $index++) {
        $line = $lines[$index]
        $matches = [regex]::Matches($line, 'deepseek-taskuri-late-50(?:-\d+)?\.md')
        foreach ($match in $matches) {
            $name = $match.Value
            if ($activeFiles -contains $name) {
                $listed += [pscustomobject]@{
                    name = $name
                    ordinal = Get-BatchOrdinal $name
                    line = $index + 1
                }
            }
        }
    }

    $previous = $null
    foreach ($entry in $listed) {
        if ($null -eq $entry.ordinal) {
            continue
        }
        if ($null -ne $previous -and $entry.ordinal -lt $previous.ordinal) {
            $issues += [pscustomobject]@{
                type = "chronological_order"
                task = ""
                file = "README.md"
                line = $entry.line
                message = "DeepSeek active batch '$($entry.name)' appears before an earlier batch order."
            }
        }
        $previous = $entry
    }

    return $issues
}

function Get-MetadataIssues {
    param([string]$DocsDir)

    $issues = @()
    $files = @()
    $files += Get-ChildItem -LiteralPath $DocsDir -Filter "deepseek-taskuri-late-50*.md" -File
    $archiveDir = Join-Path $DocsDir "arhiva"
    if (Test-Path -LiteralPath $archiveDir) {
        $files += Get-ChildItem -LiteralPath $archiveDir -Filter "deepseek-taskuri*.md" -File
        $files += Get-Item -LiteralPath (Join-Path $archiveDir "README.md")
    }

    foreach ($file in $files) {
        $raw = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction Stop
        $firstLine = ($raw -split "`r?`n" | Select-Object -First 1)
        if ($file.Name -like "deepseek-taskuri*.md" -and $firstLine -notmatch "^# DeepSeek Taskuri") {
            $issues += [pscustomobject]@{
                type = "title_consistency"
                task = ""
                file = $file.Name
                line = 1
                message = "DeepSeek task batch title does not use the standard '# DeepSeek Taskuri' prefix."
            }
        }
        if ($file.Name -eq "README.md" -and $file.FullName -like "*\arhiva\*" -and $firstLine -ne "# Arhiva DeepSeek") {
            $issues += [pscustomobject]@{
                type = "title_consistency"
                task = ""
                file = "docs/deepseek/arhiva/README.md"
                line = 1
                message = "DeepSeek archive README title is not canonical."
            }
        }
        if ($raw -notmatch "(?mi)^Actualizat:\s*\d{4}-\d{2}-\d{2}") {
            $issues += [pscustomobject]@{
                type = "metadata_date"
                task = ""
                file = $file.Name
                line = 0
                message = "Document is missing an 'Actualizat: YYYY-MM-DD' metadata line."
            }
        }
    }

    return $issues
}

function Get-MarkdownLinkIssues {
    param([string]$DocsDir)

    $issues = @()
    $pathsToCheck = @(
        (Join-Path $DocsDir "README.md"),
        (Join-Path $DocsDir "index-arhiva.md"),
        (Join-Path $DocsDir "deepseek-active-series-summary.md"),
        (Join-Path $DocsDir "deepseek-batch-guide.md"),
        (Join-Path $DocsDir "arhiva\README.md")
    )

    foreach ($path in $pathsToCheck) {
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }

        $raw = Get-Content -LiteralPath $path -Raw -ErrorAction Stop
        $matches = [regex]::Matches($raw, '\[[^\]]+\]\((?<target>[^)]+)\)')
        foreach ($match in $matches) {
            $target = $match.Groups["target"].Value.Trim()
            if ($target.StartsWith("http", [System.StringComparison]::OrdinalIgnoreCase) -or
                $target.StartsWith("#") -or
                $target.StartsWith("mailto:", [System.StringComparison]::OrdinalIgnoreCase)) {
                continue
            }

            $target = $target.Trim("<", ">")
            $target = ($target -split "#")[0]
            if ([string]::IsNullOrWhiteSpace($target)) {
                continue
            }

            $normalized = $target.Replace("/", [System.IO.Path]::DirectorySeparatorChar)
            $resolved = if ([System.IO.Path]::IsPathRooted($normalized)) {
                $normalized
            } else {
                Join-Path (Split-Path -Parent $path) $normalized
            }

            if (-not (Test-Path -LiteralPath $resolved)) {
                $relativeFile = Resolve-Path -LiteralPath $path -Relative
                $issues += [pscustomobject]@{
                    type = "broken_markdown_link"
                    task = ""
                    file = $relativeFile
                    line = 0
                    message = "Markdown link target '$target' does not exist."
                }
            }
        }
    }

    return $issues
}

function Get-GuideFileIssues {
    param([string]$DocsDir)

    $issues = @()
    $guidePath = Join-Path $DocsDir "deepseek-batch-guide.md"
    if (-not (Test-Path -LiteralPath $guidePath)) {
        return $issues
    }

    $raw = Get-Content -LiteralPath $guidePath -Raw -ErrorAction Stop
    $matches = [regex]::Matches($raw, '`(?<path>(?:docs[/\\]deepseek[/\\])?(?:arhiva[/\\])?deepseek-taskuri[^`]+\.md|(?:docs[/\\]deepseek[/\\])?arhiva[/\\]README\.md|(?:docs[/\\]deepseek[/\\])?deepseek-[^`]+\.md)`')
    foreach ($match in $matches) {
        $relative = $match.Groups["path"].Value
        $relative = $relative -replace '^(?:docs[/\\]deepseek[/\\])', ''
        $resolved = Join-Path $DocsDir ($relative.Replace("/", [System.IO.Path]::DirectorySeparatorChar))
        if (-not (Test-Path -LiteralPath $resolved)) {
            $issues += [pscustomobject]@{
                type = "guide_missing_file"
                task = ""
                file = "deepseek-batch-guide.md"
                line = 0
                message = "Guide references missing file '$relative'."
            }
        }
    }

    return $issues
}

function Get-ArchiveInventoryIssues {
    param([string]$DocsDir)

    $issues = @()
    $archiveDir = Join-Path $DocsDir "arhiva"
    $readmePath = Join-Path $archiveDir "README.md"
    if (-not (Test-Path -LiteralPath $readmePath)) {
        return $issues
    }

    $raw = Get-Content -LiteralPath $readmePath -Raw -ErrorAction Stop
    $listed = [regex]::Matches($raw, '`(?<file>deepseek-taskuri[^`]+\.md)`') |
        ForEach-Object { $_.Groups["file"].Value }

    foreach ($name in $listed) {
        if (-not (Test-Path -LiteralPath (Join-Path $archiveDir $name))) {
            $issues += [pscustomobject]@{
                type = "archive_inventory_missing_file"
                task = ""
                file = "docs/deepseek/arhiva/README.md"
                line = 0
                message = "Archive inventory lists missing file '$name'."
            }
        }
    }

    $archiveFiles = Get-ChildItem -LiteralPath $archiveDir -Filter "deepseek-taskuri*.md" -File |
        Select-Object -ExpandProperty Name
    foreach ($name in $archiveFiles) {
        if ($listed -notcontains $name) {
            $issues += [pscustomobject]@{
                type = "archive_inventory_unlisted_file"
                task = ""
                file = "docs/deepseek/arhiva/README.md"
                line = 0
                message = "Archive file '$name' is not listed in the archive README."
            }
        }
    }

    return $issues
}

function Get-ActiveSeriesBoundaryIssues {
    param([string]$DocsDir)

    $issues = @()
    $archiveDir = Join-Path $DocsDir "arhiva"
    if (-not (Test-Path -LiteralPath $archiveDir)) {
        return $issues
    }

    $archiveTasks = @()
    $archiveFiles = Get-ChildItem -LiteralPath $archiveDir -Filter "deepseek-taskuri*.md" -File |
        Sort-Object Name
    foreach ($file in $archiveFiles) {
        $raw = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction Stop
        $matches = [regex]::Matches($raw, "(?m)^### L(?<start>\d+)(?:-L(?<end>\d+))?\b")
        foreach ($match in $matches) {
            $startNumber = [int]$match.Groups["start"].Value
            $endNumber = if ([string]::IsNullOrWhiteSpace($match.Groups["end"].Value)) {
                $startNumber
            } else {
                [int]$match.Groups["end"].Value
            }
            $archiveTasks += [pscustomobject]@{
                number = $startNumber
                end = $endNumber
                file = "docs/deepseek/arhiva/$($file.Name)"
            }
        }
    }

    $activeTasks = @()
    $activeFiles = Get-ChildItem -LiteralPath $DocsDir -Filter "deepseek-taskuri-late-50*.md" -File |
        Sort-Object Name
    foreach ($file in $activeFiles) {
        $raw = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction Stop
        $matches = [regex]::Matches($raw, "(?m)^### L(?<start>\d+)(?:-L(?<end>\d+))?\b")
        foreach ($match in $matches) {
            $startNumber = [int]$match.Groups["start"].Value
            $activeTasks += [pscustomobject]@{
                number = $startNumber
                file = $file.Name
                line = ($raw.Substring(0, $match.Index) -split "`r?`n").Count
            }
        }
    }

    if (@($archiveTasks).Count -eq 0 -or @($activeTasks).Count -eq 0) {
        return $issues
    }

    $lastArchiveNumber = ($archiveTasks | Measure-Object -Property end -Maximum).Maximum
    $firstActive = $activeTasks | Sort-Object number, file | Select-Object -First 1
    $expectedFirstActive = [int]$lastArchiveNumber + 1

    if ($firstActive.number -le $lastArchiveNumber) {
        $issues += [pscustomobject]@{
            type = "active_archive_boundary_overlap"
            task = $firstActive.file
            file = $firstActive.file
            line = $firstActive.line
            message = "Active DeepSeek series overlaps archived tasks. Last archived task is $(Format-TaskId $lastArchiveNumber)."
        }
    } elseif ($firstActive.number -ne $expectedFirstActive) {
        $issues += [pscustomobject]@{
            type = "active_archive_boundary_gap"
            task = "$(Format-TaskId $expectedFirstActive)-$(Format-TaskId ($firstActive.number - 1))"
            file = $firstActive.file
            line = $firstActive.line
            message = "Active DeepSeek series does not start immediately after the archive boundary."
        }
    }

    $referenceFiles = @(
        "README.md",
        "deepseek-batch-guide.md",
        "deepseek-active-series-summary.md"
    )
    $firstActiveId = Format-TaskId $firstActive.number
    foreach ($referenceName in $referenceFiles) {
        $referencePath = Join-Path $DocsDir $referenceName
        if (-not (Test-Path -LiteralPath $referencePath)) {
            continue
        }

        $raw = Get-Content -LiteralPath $referencePath -Raw -ErrorAction Stop
        if ($raw -notmatch [regex]::Escape($firstActive.file) -or $raw -notmatch [regex]::Escape($firstActiveId)) {
            $issues += [pscustomobject]@{
                type = "active_start_reference_missing"
                task = $firstActiveId
                file = $referenceName
                line = 0
                message = "Active start '$firstActiveId' in '$($firstActive.file)' is not referenced unambiguously."
            }
        }
    }

    return $issues
}

function Get-NextCandidate {
    param(
        [array]$Tasks,
        [int]$CurrentNumber
    )

    return $Tasks |
        Sort-Object number, file |
        Where-Object { $_.number -ge $CurrentNumber -and $_.status -notin @("DONE", "CANCELLED") } |
        Select-Object -First 1
}

function New-Result {
    param(
        [string]$Status,
        [int]$CurrentNumber,
        $Candidate,
        [string]$Message,
        [array]$Issues,
        [string]$LedgerPath
    )

    return [pscustomobject]@{
        status = $Status
        current_cursor = (Format-TaskId $CurrentNumber)
        ledger_file = $LedgerPath
        next_task = $Candidate
        message = $Message
        issues = @($Issues)
    }
}

function Get-ExecutionResult {
    param(
        [int]$CurrentNumber,
        [array]$Tasks,
        [array]$Issues,
        [string]$LedgerPath
    )

    $candidate = Get-NextCandidate $Tasks $CurrentNumber
    if ($null -eq $candidate) {
        return New-Result "complete" $CurrentNumber $null "No open DeepSeek task found at or after cursor." $Issues $LedgerPath
    }
    if ($candidate.document_status -eq "DRAFT_INCOMPLETE") {
        return New-Result "blocked" $CurrentNumber $candidate "Next task is inside a draft-incomplete batch. Expand and normalize the batch before execution." $Issues $LedgerPath
    }
    if ($candidate.range_end -ne $null) {
        return New-Result "blocked" $CurrentNumber $candidate "Next entry is a reserved range, not an executable task." $Issues $LedgerPath
    }
    if ($candidate.status -in @("BLOCKED", "PARTIAL", "NEEDS_REVIEW")) {
        return New-Result "blocked" $CurrentNumber $candidate "Next task is not closed and cannot be skipped without a human decision." $Issues $LedgerPath
    }
    if (@($candidate.missing_fields).Count -gt 0) {
        return New-Result "blocked" $CurrentNumber $candidate "Next task is missing required fields." $Issues $LedgerPath
    }

    return New-Result "ready" $CurrentNumber $candidate "Execute this task next. Do not skip forward." $Issues $LedgerPath
}

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Get-Location).Path
}

$root = Resolve-ProjectPath $ProjectRoot
$docsDir = Join-Path $root "docs\deepseek"
$cursorPath = Resolve-RepoPath $root $CursorFile
$ledgerPath = Resolve-RepoPath $root $LedgerFile
$currentNumber = Get-CurrentTaskNumber $cursorPath
$statusMap = Read-StatusMap $ledgerPath
$scan = Get-DeepSeekTasks $docsDir $statusMap
$reservations = Get-DeepSeekReservations $docsDir
$scan.issues += @(Get-ArchiveIssues $docsDir)
$scan.issues += @(Get-NumberingIssues $scan.tasks $reservations)
$scan.issues += @(Get-IndexIssues $docsDir)
$scan.issues += @(Get-MetadataIssues $docsDir)
$scan.issues += @(Get-MarkdownLinkIssues $docsDir)
$scan.issues += @(Get-GuideFileIssues $docsDir)
$scan.issues += @(Get-ArchiveInventoryIssues $docsDir)
$scan.issues += @(Get-ActiveSeriesBoundaryIssues $docsDir)
$result = Get-ExecutionResult $currentNumber $scan.tasks $scan.issues $ledgerPath

if (-not [string]::IsNullOrWhiteSpace($Mark)) {
    $taskIdToMark = Normalize-TaskId $Mark
    if ($null -eq $result.next_task) {
        throw "Cannot mark $taskIdToMark because there is no next task."
    }
    if ($result.next_task.id -ne $taskIdToMark) {
        throw "Refusing to mark $taskIdToMark. Next eligible task is $($result.next_task.id)."
    }

    $statusMap[$taskIdToMark] = [ordered]@{
        status = $MarkStatus.ToUpperInvariant()
        updated_utc = (Get-Date).ToUniversalTime().ToString("o")
        reason = $Reason
        evidence = $Evidence
        file = $result.next_task.file
        line = $result.next_task.line
        title = $result.next_task.title
    }
    Save-StatusMap $ledgerPath $statusMap

    $scan = Get-DeepSeekTasks $docsDir $statusMap
    $reservations = Get-DeepSeekReservations $docsDir
    $scan.issues += @(Get-ArchiveIssues $docsDir)
    $scan.issues += @(Get-NumberingIssues $scan.tasks $reservations)
    $scan.issues += @(Get-IndexIssues $docsDir)
    $scan.issues += @(Get-MetadataIssues $docsDir)
    $scan.issues += @(Get-MarkdownLinkIssues $docsDir)
    $scan.issues += @(Get-GuideFileIssues $docsDir)
    $scan.issues += @(Get-ArchiveInventoryIssues $docsDir)
    $scan.issues += @(Get-ActiveSeriesBoundaryIssues $docsDir)
    $result = Get-ExecutionResult $currentNumber $scan.tasks $scan.issues $ledgerPath
    if ($MarkStatus -in @("DONE", "CANCELLED") -and $null -ne $result.next_task) {
        Set-CurrentTaskNumber $cursorPath $result.next_task.number
        $currentNumber = Get-CurrentTaskNumber $cursorPath
        $result = Get-ExecutionResult $currentNumber $scan.tasks $scan.issues $ledgerPath
    }
}

$json = $result | ConvertTo-Json -Depth 10
if (-not [string]::IsNullOrWhiteSpace($OutFile)) {
    $outPath = Resolve-RepoPath $root $OutFile
    Set-Content -LiteralPath $outPath -Value $json -Encoding utf8
}

$json

if ($FailOnBlocked -and $result.status -eq "blocked") {
    exit 2
}





