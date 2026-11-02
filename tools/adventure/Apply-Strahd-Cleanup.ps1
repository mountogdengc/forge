param([string]$GameFolder = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $GameFolder).Path.TrimEnd('\','/')
$manifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'PATCH-MANIFEST.json') -Raw -Encoding UTF8 | ConvertFrom-Json
function Inside([string]$base, [string]$relative) {
    $path = [IO.Path]::GetFullPath((Join-Path $base $relative))
    if (!$path.StartsWith($base + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Update path escapes its folder: $relative"
    }
    return $path
}
function Hash([string]$path) {
    $algorithm = [Security.Cryptography.SHA256]::Create()
    $stream = [IO.File]::OpenRead($path)
    try { return [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-','').ToLowerInvariant() }
    finally { $stream.Dispose(); $algorithm.Dispose() }
}
$jar = Inside $root 'forge-adventure-test.jar'
$campaign = Get-Content -LiteralPath (Inside $root 'res/adventure/CurseOfStrahd/campaign.json') -Raw -Encoding UTF8 | ConvertFrom-Json
if ($campaign.id -ne 'fork.curse-of-strahd' -or $campaign.version -ne '0.1.0') { throw 'This update requires Strahd opening chapter 0.1.0.' }
$profile = Get-Content -LiteralPath (Inside $root 'forge.profile.properties')
if ($profile -notcontains 'userDir=./test-profile' -or $profile -notcontains 'cacheDir=./test-cache') { throw 'The portable profile guard does not match.' }
$oldHash = Hash $jar
if ($oldHash -notin (@($manifest.base_jar_sha256, $manifest.jar_sha256) + @($manifest.compatible_base_jars))) { throw 'This client is not a compatible Strahd build. No files changed.' }
$runtime = Inside $root 'runtime/bin/java.exe'
$windowRuntime = Inside $root 'runtime/bin/javaw.exe'
foreach ($process in @([Diagnostics.Process]::GetProcessesByName('java')) + @([Diagnostics.Process]::GetProcessesByName('javaw'))) {
    $executable = $null
    try { $executable = $process.MainModule.FileName } catch { }
    if ($executable -and [IO.Path]::GetFullPath($executable) -in @($runtime,$windowRuntime)) { throw 'Close the game and Campaign Editor before applying the update.' }
}
$handle = [IO.File]::Open($jar, [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
$handle.Dispose()
$payloadRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'payload')).Path
$files = @($manifest.files.psobject.Properties)
foreach ($file in $files) {
    if ($file.Name -match '^(test-profile|test-cache|test-platform|test-home)[\\/]' -or $file.Name -eq 'forge.profile.properties') { throw 'Update cannot modify player data.' }
    $source = Inside $payloadRoot $file.Name
    if ((Hash $source) -ne $file.Value) { throw "Update file is damaged: $($file.Name)" }
    $null = Inside $root $file.Name
}
$patchName = if ($manifest.id) { $manifest.id -replace '[^A-Za-z0-9._-]','_' } else { 'strahd-cleanup' }
$backup = Inside $root ('.updates/' + $patchName + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,6))
New-Item -ItemType Directory -Path $backup | Out-Null
$records = @()
foreach ($name in @($files.Name) + @('SHA256SUMS.txt')) {
    $target = Inside $root $name
    $record = [pscustomobject]@{name=$name; existed=(Test-Path -LiteralPath $target)}
    $records += $record
    if ($record.existed) {
        $copy = Inside $backup $name
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $copy) | Out-Null
        Copy-Item -LiteralPath $target -Destination $copy
    }
}
$records | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backup 'restore-record.json') -Encoding UTF8
try {
    foreach ($file in $files) {
        $target = Inside $root $file.Name
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
        Copy-Item -LiteralPath (Inside $payloadRoot $file.Name) -Destination $target -Force
        if ((Hash $target) -ne $file.Value) { throw "Copy verification failed: $($file.Name)" }
    }
    $sums = @{}
    foreach ($line in Get-Content -LiteralPath (Inside $root 'SHA256SUMS.txt') -Encoding UTF8) {
        $parts = $line -split '  ',2
        if ($parts.Count -eq 2) { $sums[$parts[1]] = $parts[0] }
    }
    foreach ($file in $files) { $sums[$file.Name] = $file.Value }
    $lines = @($sums.Keys | Sort-Object | ForEach-Object { $sums[$_] + '  ' + $_ })
    [IO.File]::WriteAllLines((Inside $root 'SHA256SUMS.txt'), $lines, (New-Object Text.UTF8Encoding $false))
} catch {
    $failure = $_
    $rollbackErrors = @()
    foreach ($record in $records) {
        try {
            $target = Inside $root $record.name
            if ($record.existed) { Copy-Item -LiteralPath (Inside $backup $record.name) -Destination $target -Force }
            elseif (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target }
        } catch { $rollbackErrors += $record.name }
    }
    if ($rollbackErrors.Count) { throw "Update failed. Backup: $backup. Close the game and restore these files: $rollbackErrors. Original error: $failure" }
    throw "Update failed and original files were restored. $failure"
}
$displayName = if ($manifest.display_name) { $manifest.display_name } else { 'Strahd encounter cleanup' }
Write-Output "$displayName installed. Your saves and display/audio settings were preserved."
Write-Output "Backup: $backup"
Write-Output 'Launch normally with Start-CurseOfStrahd.cmd and choose Continue.'
