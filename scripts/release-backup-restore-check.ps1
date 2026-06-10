param(
    [Parameter(Mandatory = $true)]
    [string]$ServerDir,

    [string]$BackupRoot = "",

    [string]$ReleaseId = "",

    [switch]$IncludeWorlds,

    [string[]]$WorldNames = @("world", "world_nether", "world_the_end"),

    [switch]$SkipRestoreCheck,

    [switch]$KeepRestoreCheckDir
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-RequiredDirectory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    $resolved = Resolve-Path -LiteralPath $Path -ErrorAction Stop
    if (-not (Test-Path -LiteralPath $resolved.Path -PathType Container)) {
        throw "$Label nu este director: $Path"
    }
    return $resolved.Path
}

function New-SafeReleaseId {
    param([string]$Value)

    $raw = if ($Value -and $Value.Trim().Length -gt 0) {
        $Value.Trim()
    } else {
        Get-Date -Format "yyyyMMdd-HHmmss"
    }

    $safe = $raw -replace "[^A-Za-z0-9._-]", "-"
    $safe = $safe.Trim("-")
    if ($safe.Length -eq 0) {
        return Get-Date -Format "yyyyMMdd-HHmmss"
    }
    return $safe
}

function Get-RelativePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BasePath,

        [Parameter(Mandatory = $true)]
        [string]$FullPath
    )

    $base = [System.IO.Path]::GetFullPath($BasePath).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $full = [System.IO.Path]::GetFullPath($FullPath)
    $baseUri = [Uri]::new($base + [System.IO.Path]::DirectorySeparatorChar)
    $fullUri = [Uri]::new($full)
    return [Uri]::UnescapeDataString($baseUri.MakeRelativeUri($fullUri).ToString()).Replace("/", "\")
}

function Copy-PathToPayload {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SourcePath,

        [Parameter(Mandatory = $true)]
        [string]$RelativeDestination,

        [Parameter(Mandatory = $true)]
        [string]$PayloadRoot
    )

    $target = Join-Path $PayloadRoot $RelativeDestination
    $parent = Split-Path -Parent $target
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    if (Test-Path -LiteralPath $SourcePath -PathType Container) {
        Copy-Item -LiteralPath $SourcePath -Destination $target -Recurse -Force
    } else {
        Copy-Item -LiteralPath $SourcePath -Destination $target -Force
    }
}

function New-ManifestEntries {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PayloadRoot
    )

    $entries = New-Object System.Collections.Generic.List[object]
    $files = Get-ChildItem -LiteralPath $PayloadRoot -Recurse -File |
        Where-Object { $_.Name -ne "backup-manifest.json" } |
        Sort-Object FullName

    foreach ($file in $files) {
        $relativePath = Get-RelativePath -BasePath $PayloadRoot -FullPath $file.FullName
        $hash = Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256
        $entries.Add([pscustomobject]@{
            path = $relativePath
            bytes = $file.Length
            sha256 = $hash.Hash
            last_write_time_utc = $file.LastWriteTimeUtc.ToString("o")
        })
    }
    return @($entries.ToArray())
}

function Test-RestorePayload {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ZipPath,

        [Parameter(Mandatory = $true)]
        [string]$RestoreRoot
    )

    [System.IO.Compression.ZipFile]::ExtractToDirectory($ZipPath, $RestoreRoot)
    $manifestPath = Join-Path $RestoreRoot "backup-manifest.json"
    if (-not (Test-Path -LiteralPath $manifestPath)) {
        throw "Restore check a esuat: lipseste backup-manifest.json."
    }

    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    $issues = New-Object System.Collections.Generic.List[string]
    $checked = 0
    foreach ($entry in @($manifest.entries)) {
        $restoredPath = Join-Path $RestoreRoot $entry.path
        if (-not (Test-Path -LiteralPath $restoredPath -PathType Leaf)) {
            $issues.Add("Lipseste fisier restaurat: $($entry.path)")
            continue
        }
        $file = Get-Item -LiteralPath $restoredPath
        if ($file.Length -ne [int64]$entry.bytes) {
            $issues.Add("Dimensiune diferita pentru $($entry.path): $($file.Length) != $($entry.bytes)")
            continue
        }
        $hash = Get-FileHash -LiteralPath $restoredPath -Algorithm SHA256
        if ($hash.Hash -ne $entry.sha256) {
            $issues.Add("SHA256 diferit pentru $($entry.path)")
            continue
        }
        $checked++
    }

    return [pscustomobject]@{
        ok = $issues.Count -eq 0
        checked_files = $checked
        issue_count = $issues.Count
        issues = @($issues.ToArray())
        restore_root = $RestoreRoot
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

$serverDirFull = Resolve-RequiredDirectory -Path $ServerDir -Label "ServerDir"
$safeReleaseId = New-SafeReleaseId -Value $ReleaseId
$backupRootFull = if ($BackupRoot -and $BackupRoot.Trim().Length -gt 0) {
    $BackupRoot
} else {
    Join-Path $serverDirFull "ainpc-release-backups"
}
if (-not (Test-Path -LiteralPath $backupRootFull)) {
    New-Item -ItemType Directory -Path $backupRootFull -Force | Out-Null
}
$backupRootFull = Resolve-RequiredDirectory -Path $backupRootFull -Label "BackupRoot"

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupName = "ainpc-release-backup-$safeReleaseId-$timestamp"
$backupZip = Join-Path $backupRootFull "$backupName.zip"
$reportPath = Join-Path $backupRootFull "$backupName-report.json"
$restoreReportPath = Join-Path $backupRootFull "$backupName-restore-check.json"
$workspaceRoot = Join-Path $env:TEMP ("ainpc-release-backup-" + [Guid]::NewGuid().ToString("N"))
$payloadRoot = Join-Path $workspaceRoot "payload"
$restoreRoot = Join-Path $workspaceRoot "restore-check"

$warnings = New-Object System.Collections.Generic.List[string]
$selected = New-Object System.Collections.Generic.List[object]
$missing = New-Object System.Collections.Generic.List[object]

try {
    New-Item -ItemType Directory -Path $payloadRoot -Force | Out-Null

    $pluginData = Join-Path $serverDirFull "plugins\AINPC"
    if (Test-Path -LiteralPath $pluginData -PathType Container) {
        Copy-PathToPayload -SourcePath $pluginData -RelativeDestination "plugins\AINPC" -PayloadRoot $payloadRoot
        $selected.Add([pscustomobject]@{ role = "plugin-data"; source = $pluginData; destination = "plugins\AINPC" })
    } else {
        $missing.Add([pscustomobject]@{ role = "plugin-data"; source = $pluginData })
        $warnings.Add("plugins/AINPC lipseste; backup-ul poate fi doar pre-install sau incomplet.")
    }

    $pluginsDir = Join-Path $serverDirFull "plugins"
    if (Test-Path -LiteralPath $pluginsDir -PathType Container) {
        $pluginJars = @(Get-ChildItem -LiteralPath $pluginsDir -File -Filter "*.jar" |
            Where-Object { $_.Name -match "ainpc" } |
            Sort-Object Name)
        foreach ($jar in $pluginJars) {
            $destination = Join-Path "plugins" $jar.Name
            Copy-PathToPayload -SourcePath $jar.FullName -RelativeDestination $destination -PayloadRoot $payloadRoot
            $selected.Add([pscustomobject]@{ role = "plugin-jar"; source = $jar.FullName; destination = $destination })
        }
        if ($pluginJars.Count -eq 0) {
            $warnings.Add("Nu am gasit JAR-uri AINPC in plugins/.")
        }
    } else {
        $missing.Add([pscustomobject]@{ role = "plugins-dir"; source = $pluginsDir })
        $warnings.Add("plugins/ lipseste.")
    }

    $serverProperties = Join-Path $serverDirFull "server.properties"
    if (Test-Path -LiteralPath $serverProperties -PathType Leaf) {
        Copy-PathToPayload -SourcePath $serverProperties -RelativeDestination "server.properties" -PayloadRoot $payloadRoot
        $selected.Add([pscustomobject]@{ role = "server-config"; source = $serverProperties; destination = "server.properties" })
    } else {
        $missing.Add([pscustomobject]@{ role = "server-config"; source = $serverProperties })
    }

    if ($IncludeWorlds) {
        foreach ($worldName in $WorldNames) {
            if (-not $worldName -or $worldName.Trim().Length -eq 0) {
                continue
            }
            $worldPath = Join-Path $serverDirFull $worldName
            if (Test-Path -LiteralPath $worldPath -PathType Container) {
                Copy-PathToPayload -SourcePath $worldPath -RelativeDestination $worldName -PayloadRoot $payloadRoot
                $selected.Add([pscustomobject]@{ role = "world"; source = $worldPath; destination = $worldName })
            } else {
                $missing.Add([pscustomobject]@{ role = "world"; source = $worldPath })
                $warnings.Add("Lume lipsa, sarita: $worldName")
            }
        }
    }

    $entries = @(New-ManifestEntries -PayloadRoot $payloadRoot)
    if ($entries.Count -eq 0) {
        throw "Nu exista fisiere de inclus in backup. Verifica ServerDir si plugins/."
    }

    $manifest = [pscustomobject]@{
        schema = "ainpc.release-backup.v1"
        generated_at = (Get-Date).ToString("o")
        release_id = $safeReleaseId
        server_dir = $serverDirFull
        include_worlds = [bool]$IncludeWorlds
        world_names = @($WorldNames)
        selected_sources = @($selected.ToArray())
        missing_sources = @($missing.ToArray())
        warnings = @($warnings.ToArray())
        entries = @($entries)
    }
    $manifestPath = Join-Path $payloadRoot "backup-manifest.json"
    $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8

    if (Test-Path -LiteralPath $backupZip) {
        throw "Backup zip exista deja: $backupZip"
    }
    [System.IO.Compression.ZipFile]::CreateFromDirectory($payloadRoot, $backupZip, [System.IO.Compression.CompressionLevel]::Optimal, $false)
    $zipHash = Get-FileHash -LiteralPath $backupZip -Algorithm SHA256
    $zipItem = Get-Item -LiteralPath $backupZip

    $restoreCheck = [pscustomobject]@{
        requested = -not [bool]$SkipRestoreCheck
        ok = $null
        checked_files = 0
        issue_count = 0
        issues = @()
        restore_root = $null
    }
    if (-not $SkipRestoreCheck) {
        New-Item -ItemType Directory -Path $restoreRoot -Force | Out-Null
        $restoreCheck = Test-RestorePayload -ZipPath $backupZip -RestoreRoot $restoreRoot
        $restoreCheck | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $restoreReportPath -Encoding UTF8
        if (-not $restoreCheck.ok) {
            throw "Restore check a esuat. Raport: $restoreReportPath"
        }
    }

    $report = [pscustomobject]@{
        ok = $true
        generated_at = (Get-Date).ToString("o")
        release_id = $safeReleaseId
        server_dir = $serverDirFull
        backup_zip = $backupZip
        backup_zip_sha256 = $zipHash.Hash
        backup_zip_bytes = $zipItem.Length
        report_path = $reportPath
        restore_report_path = if ($SkipRestoreCheck) { $null } else { $restoreReportPath }
        entry_count = $entries.Count
        selected_source_count = $selected.Count
        missing_source_count = $missing.Count
        warnings = @($warnings.ToArray())
        restore_check = $restoreCheck
    }
    $report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportPath -Encoding UTF8

    Write-Host "Backup release creat: $backupZip"
    Write-Host "Raport: $reportPath"
    if (-not $SkipRestoreCheck) {
        Write-Host "Restore check: $($restoreCheck.ok)"
        Write-Host "Raport restore: $restoreReportPath"
    }
    $report | ConvertTo-Json -Depth 8
} finally {
    if (Test-Path -LiteralPath $workspaceRoot) {
        if ($KeepRestoreCheckDir) {
            $keptRoot = Join-Path $backupRootFull "$backupName-restore-dir"
            if (Test-Path -LiteralPath $restoreRoot) {
                Move-Item -LiteralPath $restoreRoot -Destination $keptRoot -Force
            }
        }
        Remove-Item -LiteralPath $workspaceRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
