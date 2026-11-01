param(
    [Parameter(Mandatory)][string]$PackageRoot,
    [Parameter(Mandatory)][string]$EvidencePath
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $PackageRoot).Path
$evidence = [IO.Path]::GetFullPath($EvidencePath)
if (Test-Path $evidence) { throw 'Choose a NEW evidence folder.' }
if (Test-Path (Join-Path $root 'test-profile/adventure/campaign-saves')) {
    throw 'Use a fresh disposable extraction, not a player installation.'
}
$expectedJava = Join-Path $root 'runtime/bin/java.exe'
if (-not (Test-Path $expectedJava) -or -not (Test-Path (Join-Path $root 'Start-Bellwake.cmd'))) {
    throw 'Incomplete portable build.'
}
$profileText = Get-Content (Join-Path $root 'forge.profile.properties')
if ($profileText -notcontains 'userDir=./test-profile' -or $profileText -notcontains 'cacheDir=./test-cache') {
    throw 'Portable profile guard failed.'
}
New-Item -ItemType Directory $evidence | Out-Null
$log = Join-Path $root 'logs/launcher.log'
if (Test-Path $log) { throw 'Use a fresh disposable extraction with no launcher log.' }
$launcher = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d', '/c', 'Start-Bellwake.cmd') -WorkingDirectory $root -WindowStyle Hidden -PassThru
$ownedJava = $null
$passed = $false
try {
    $deadline = (Get-Date).AddMinutes(6)
    while ((Get-Date) -lt $deadline) {
        if (-not $ownedJava) {
            $children = @(Get-CimInstance Win32_Process -Filter "ParentProcessId=$($launcher.Id)")
            $ownedJava = $children | Where-Object { $_.ExecutablePath -and [IO.Path]::GetFullPath($_.ExecutablePath) -eq [IO.Path]::GetFullPath($expectedJava) } | Select-Object -First 1
        }
        if ($ownedJava -and (Test-Path $log)) {
            $stream = [IO.FileStream]::new($log, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::ReadWrite)
            $reader = [IO.StreamReader]::new($stream)
            try { $text = $reader.ReadToEnd() } finally { $reader.Dispose() }
            if ($text -match 'Looking for resource ui/start_menu.json\.\.\. Found!') {
                if (-not (Get-Process -Id $ownedJava.ProcessId -ErrorAction SilentlyContinue)) { throw 'Client exited during startup.' }
                [IO.File]::WriteAllText((Join-Path $evidence 'launcher-startup.log'), $text)
                $passed = $true
                break
            }
        }
        if ($launcher.HasExited) { throw 'Launcher exited before reaching Adventure menu resources.' }
        Start-Sleep -Seconds 1
    }
    if (-not $passed) { throw 'Launcher did not reach Adventure menu resources within six minutes.' }
} finally {
    # This is a startup-only probe. Stop only the Java child launched by this CMD,
    # checking its executable again; leave every other Forge/Java process alone.
    if ($ownedJava) {
        $current = Get-CimInstance Win32_Process -Filter "ProcessId=$($ownedJava.ProcessId)"
        if ($current -and $current.ParentProcessId -eq $launcher.Id -and
            [IO.Path]::GetFullPath($current.ExecutablePath) -eq [IO.Path]::GetFullPath($expectedJava)) {
            Stop-Process -Id $ownedJava.ProcessId -ErrorAction SilentlyContinue
        }
    }
    if (-not $launcher.HasExited) { Stop-Process -Id $launcher.Id -ErrorAction SilentlyContinue }
}
if ($passed) {
    [IO.File]::WriteAllText((Join-Path $evidence 'result.txt'), "BELLWAKE_LAUNCHER_STARTUP_PASS`nBundled Java launched by Start-Bellwake.cmd loaded Adventure start-menu resources.`nStartup-only process terminated after probe; no gameplay/shutdown acceptance implied.`n")
    Write-Output "BELLWAKE_LAUNCHER_STARTUP_PASS. Evidence: $evidence"
}
