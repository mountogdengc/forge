param(
    [Parameter(Mandatory=$true)][string]$AssemblyJar,
    [Parameter(Mandatory=$true)][string]$AssetsRoot,
    [string]$JavaHome = 'C:/Program Files/Java/jdk-26',
    [string]$OutputPath = '',
    # A previous benchmark run-N directory; copies only its disposable name-index file.
    [string]$IndexCacheFrom = '',
    # Separate from the card index: use the same converted-font cache in both old/new comparisons.
    [string]$FontCacheFrom = '',
    [switch]$RequireDeferred,
    [ValidateRange(1, 5)][int]$Runs = 1,
    [ValidateRange(30, 600)][int]$TimeoutSeconds = 180
)
$ErrorActionPreference = 'Stop'
$repository = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$jar = (Resolve-Path -LiteralPath $AssemblyJar).Path
$assets = (Resolve-Path -LiteralPath $AssetsRoot).Path
if (-not (Test-Path (Join-Path $assets 'res'))) { throw 'AssetsRoot must contain res.' }
$portable = Join-Path $assets 'forge.profile.properties'
if (-not (Test-Path $portable) -or (Get-Content $portable) -notcontains 'userDir=./test-profile' -or
    (Get-Content $portable) -notcontains 'cacheDir=./test-cache') {
    throw 'AssetsRoot must use the disposable portable test-profile/test-cache convention.'
}
if (-not $OutputPath) { $OutputPath = Join-Path $repository ('.superpowers/card-loading/startup-' + [guid]::NewGuid().ToString('N')) }
$output = [IO.Path]::GetFullPath($OutputPath)
if (Test-Path -LiteralPath $output) { throw 'Output already exists; preserve prior evidence.' }
$classes = Join-Path $output 'classes'
$seedIndex = $null
if ($IndexCacheFrom) {
    $seedRun = (Resolve-Path -LiteralPath $IndexCacheFrom).Path
    $seedIndex = Join-Path $seedRun 'test-cache/card-index/common/lazy-card-name-index.bin'
    if (-not (Test-Path -LiteralPath $seedIndex -PathType Leaf)) { throw 'IndexCacheFrom must be a prior run directory containing the common lazy-card name index.' }
}
New-Item -ItemType Directory -Path $classes -Force | Out-Null
& (Join-Path $JavaHome 'bin/javac.exe') -J-Xmx256m --release 17 -cp $jar -d $classes (Join-Path $PSScriptRoot 'StartupBenchmark.java')
if ($LASTEXITCODE -ne 0) { throw 'Startup benchmark compilation failed.' }
if (-not ('ForgeStartupMemoryInfo' -as [type])) {
    Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class ForgeStartupMemoryInfo {
    [StructLayout(LayoutKind.Sequential)] public struct Status {
        public uint length, load;
        public ulong totalPhysical, availablePhysical, totalPageFile, availablePageFile, totalVirtual, availableVirtual, availableExtendedVirtual;
    }
    [DllImport("kernel32.dll", SetLastError=true)] public static extern bool GlobalMemoryStatusEx(ref Status status);
    public static ulong CommitHeadroom() {
        Status s = new Status(); s.length = (uint)Marshal.SizeOf<Status>();
        if (!GlobalMemoryStatusEx(ref s)) throw new System.ComponentModel.Win32Exception();
        return s.availablePageFile;
    }
}
'@
}
[xml]$pom = Get-Content (Join-Path $repository 'forge-gui-mobile-dev/pom.xml')
$mandatory = $pom.project.properties.'mandatory.java.args'.Split(' ', [StringSplitOptions]::RemoveEmptyEntries)
for ($runNumber = 1; $runNumber -le $Runs; $runNumber++) {
    $headroom = [ForgeStartupMemoryInfo]::CommitHeadroom()
    if ($headroom -lt 3.5GB) { throw "Insufficient commit headroom ($headroom bytes); no JVM was started." }
    $runRoot = Join-Path $output "run-$runNumber"
    New-Item -ItemType Directory -Path "$runRoot/test-profile/preferences", "$runRoot/test-profile/adventure", "$runRoot/test-cache", "$runRoot/home", "$runRoot/roaming", "$runRoot/local", "$runRoot/temp" -Force | Out-Null
    # Config.resPath probes the working directory independently of Forge's assets path.
    # A disposable junction supplies that read-only resource view without copying the database.
    New-Item -ItemType Junction -Path (Join-Path $runRoot 'res') -Target (Join-Path $assets 'res') | Out-Null
    if ($seedIndex) {
        $indexTarget = Join-Path $runRoot 'test-cache/card-index/common'
        New-Item -ItemType Directory -Path $indexTarget -Force | Out-Null
        Copy-Item -LiteralPath $seedIndex -Destination (Join-Path $indexTarget 'lazy-card-name-index.bin')
    }
    if ($FontCacheFrom) {
        $fontSource = Join-Path (Resolve-Path -LiteralPath $FontCacheFrom).Path 'test-cache/fonts'
        if (-not (Test-Path -LiteralPath $fontSource)) { throw 'Missing prior converted-font cache.' }
        Copy-Item -LiteralPath $fontSource -Destination (Join-Path $runRoot 'test-cache/fonts') -Recurse
    }
    [IO.File]::WriteAllText("$runRoot/test-profile/preferences/forge.preferences", "UI_SELECTOR_MODE=Adventure`nUI_ENABLE_SOUNDS=false`nUI_ENABLE_MUSIC=false`n")
    [IO.File]::WriteAllText("$runRoot/test-profile/adventure/settings.json", '{"plane":"Bellwake","width":1280,"height":720,"videomode":"720p","enableExtraBattleBackgrounds":false}')
    $json = Join-Path $runRoot 'startup.json'
    $arguments = @($mandatory) + @('-Xms128m', '-Xmx2g', '-XX:ActiveProcessorCount=4', '-Dforge.startup.isolated=true',
        "-Dforge.startup.timeoutSeconds=$TimeoutSeconds", "-Duser.home=$runRoot/home", "-Djava.io.tmpdir=$runRoot/temp",
        '-cp', "$classes;$jar", 'forge.app.StartupBenchmark', $assets, $json, $runRoot)
    if ($RequireDeferred) { $arguments = @('-Dforge.startup.requireDeferred=true') + $arguments }
    $argumentFile = Join-Path $runRoot 'runtime.args'
    [IO.File]::WriteAllLines($argumentFile, ($arguments | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }))
    $priorAppData = $env:APPDATA
    $priorLocal = $env:LOCALAPPDATA
    $process = $null
    try {
        $env:APPDATA = Join-Path $runRoot 'roaming'
        $env:LOCALAPPDATA = Join-Path $runRoot 'local'
        $process = Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -ArgumentList ('"@' + $argumentFile + '"') -WorkingDirectory $runRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput "$runRoot/stdout.log" -RedirectStandardError "$runRoot/stderr.log"
        if (-not $process.WaitForExit(($TimeoutSeconds + 15) * 1000)) { throw "Startup benchmark timed out; stopping PID $($process.Id)." }
        if ($process.ExitCode -ne 0 -or -not (Test-Path $json)) { throw "Startup benchmark failed ($($process.ExitCode)); inspect $runRoot logs." }
        $result = Get-Content $json -Raw | ConvertFrom-Json
        if ($result.status -ne 'ready' -or -not $result.hidden -or $result.focused) { throw "Startup did not pass readiness/isolation checks; inspect $runRoot." }
        [IO.File]::WriteAllText((Join-Path $runRoot 'launch.json'), (@{ commit_headroom_before_bytes = $headroom; jar = $jar; jar_sha256 = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash; assets = $assets; jvm_pid = $process.Id; index_cache_from = $seedIndex; font_cache_from = $FontCacheFrom } | ConvertTo-Json))
        Get-Content $json
    } finally {
        if ($null -ne $process -and -not $process.HasExited) { $process.Kill($true); $process.WaitForExit() }
        $env:APPDATA = $priorAppData
        $env:LOCALAPPDATA = $priorLocal
    }
}
Write-Output "Evidence: $output"
