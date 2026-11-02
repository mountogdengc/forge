param(
    [string]$PackageRoot = $PSScriptRoot,
    [ValidateRange(5, 30)][int]$Seconds = 30,
    [switch]$AudioEnabled
)
$ErrorActionPreference = 'Stop'

# Owns only the process passed by this launcher and its optional GPU-query child.
# Termination is best effort: a stalled driver/kernel can delay any user-space watchdog.
function Stop-BellwakeOwnedProcess {
    param([Diagnostics.Process]$Process)
    if ($null -eq $Process) { return }
    if (-not $Process.HasExited) {
        try { $Process.Kill() }
        catch [InvalidOperationException] { if (-not $Process.HasExited) { throw } }
    }
    if (-not $Process.WaitForExit(5000)) { throw 'The owned process did not confirm termination.' }
}

function Watch-BellwakeProcess {
    param([Diagnostics.Process]$Process, [int]$LimitSeconds, [string]$LogDirectory, [string]$GpuTool = '')
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $writer = $null
    $lastSample = -1000L
    $lastGpu = -5000L
    $gpu = $null
    $gpuStarted = 0L
    $timedOut = $false
    try {
        $writer = [IO.StreamWriter]::new((Join-Path $LogDirectory 'process.csv'))
        $writer.AutoFlush = $true
        $writer.WriteLine('elapsed_ms,cpu_ms,working_set_bytes,private_bytes')
        while (-not $Process.HasExited) {
            if ($timer.Elapsed.TotalSeconds -ge $LimitSeconds) {
                $timedOut = $true
                Stop-BellwakeOwnedProcess $Process
                break
            }
            if ($timer.ElapsedMilliseconds - $lastSample -ge 1000) {
                $Process.Refresh()
                if ($Process.HasExited) { break }
                $writer.WriteLine(('{0},{1},{2},{3}' -f $timer.ElapsedMilliseconds,
                    $Process.TotalProcessorTime.TotalMilliseconds, $Process.WorkingSet64, $Process.PrivateMemorySize64))
                $lastSample = $timer.ElapsedMilliseconds
            }
            if ($null -ne $gpu -and -not $gpu.HasExited -and $timer.ElapsedMilliseconds - $gpuStarted -ge 2000) {
                Stop-BellwakeOwnedProcess $gpu
            }
            if ($GpuTool -and ($null -eq $gpu -or $gpu.HasExited) -and $timer.ElapsedMilliseconds - $lastGpu -ge 5000) {
                $lastGpu = $timer.ElapsedMilliseconds
                $gpu = Start-Process -FilePath $GpuTool -WindowStyle Hidden -PassThru `
                    -ArgumentList '--query-gpu=utilization.gpu,memory.used,power.draw,temperature.gpu --format=csv' `
                    -RedirectStandardOutput (Join-Path $LogDirectory "gpu-$lastGpu.csv") `
                    -RedirectStandardError (Join-Path $LogDirectory "gpu-$lastGpu.err")
                $gpuStarted = $timer.ElapsedMilliseconds
            }
            Start-Sleep -Milliseconds 100
        }
        $Process.WaitForExit()
        return @{ elapsed_ms = $timer.ElapsedMilliseconds; stopped_at_limit = $timedOut; exit_code = $Process.ExitCode }
    } finally {
        # Terminating Java must not depend on a successful telemetry flush or GPU-query cleanup.
        try { Stop-BellwakeOwnedProcess $Process }
        finally {
            try { Stop-BellwakeOwnedProcess $gpu }
            finally { if ($null -ne $writer) { $writer.Dispose() } }
        }
    }
}

# Allows window-free tests of the watchdog without executing the launcher.
if ($MyInvocation.InvocationName -eq '.') { return }
$root = (Resolve-Path -LiteralPath $PackageRoot).Path
$profile = Join-Path $root 'forge.profile.properties'
if (-not (Test-Path $profile) -or (Get-Content $profile) -notcontains 'userDir=./test-profile' -or
        (Get-Content $profile) -notcontains 'cacheDir=./test-cache') { throw 'This requires the portable Bellwake package.' }
$java = Join-Path $root 'runtime/bin/java.exe'
if (-not (Test-Path $java) -or -not (Test-Path (Join-Path $root 'forge-bellwake-diagnostic.jar'))) { throw 'Extract the diagnostic add-on beside the existing game JAR first.' }
$logs = Join-Path $root ('logs/diagnostic-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6))
New-Item -ItemType Directory -Force $logs | Out-Null
$options = (Get-Content (Join-Path $root 'java-options.txt') -Raw).Trim() + "`n-Xms128m`n-Xmx1g`n-XX:ActiveProcessorCount=2`n-Dforge.desktop.diagnostic=true`n" +
    '"-Duser.home=' + $root.Replace('\', '/') + '/test-home"' + "`n-jar`nforge-bellwake-diagnostic.jar`nlandscape`nwidth=960`nheight=540`n"
$argumentFile = Join-Path $logs 'runtime.args'
if ($AudioEnabled) { $options = "-Dforge.desktop.diagnosticAudio=true`n" + $options }
[IO.File]::WriteAllText($argumentFile, $options)
Write-Output "Diagnostic: 30 FPS, audio enabled=$AudioEnabled, windowed. Stops after $Seconds seconds including loading."
Write-Output "Driver/kernel stalls can delay the watchdog. Logs: $logs"
Write-Output 'Keep this diagnostic console open until the test finishes; closing it can cancel the watchdog.'
$previousAppData = $env:APPDATA
$previousLocal = $env:LOCALAPPDATA
$process = $null
try {
    $env:APPDATA = Join-Path $root 'test-platform/roaming'
    $env:LOCALAPPDATA = Join-Path $root 'test-platform/local'
    $process = Start-Process -FilePath $java -WorkingDirectory $root -WindowStyle Hidden -PassThru `
        -ArgumentList ('"@' + $argumentFile + '"') -RedirectStandardOutput (Join-Path $logs 'game.log') `
        -RedirectStandardError (Join-Path $logs 'game.err')
    $gpuTool = Join-Path ([Environment]::GetFolderPath('System')) 'nvidia-smi.exe'
    if (-not (Test-Path $gpuTool)) { $gpuTool = '' }
    $result = Watch-BellwakeProcess -Process $process -LimitSeconds $Seconds -LogDirectory $logs -GpuTool $gpuTool
    $result.game_pid = $process.Id
    $result.diagnostic = $true
    $result.audio_enabled = [bool]$AudioEnabled
    $result | ConvertTo-Json | Set-Content (Join-Path $logs 'result.json')
    Write-Output "Diagnostic finished. Keep this log folder: $logs"
} finally {
    try { Stop-BellwakeOwnedProcess $process }
    finally {
        $env:APPDATA = $previousAppData
        $env:LOCALAPPDATA = $previousLocal
    }
}
