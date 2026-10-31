param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$OutputPath = "",
    [ValidateRange(320, 3840)][int]$Width = 1280,
    [ValidateRange(240, 2160)][int]$Height = 720,
    [ValidateSet('W', 'R')][string]$StarterColor = 'W'
)

$ErrorActionPreference = 'Stop'
$repository = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome 'bin/javac.exe'))) {
    throw 'Set JAVA_HOME or pass -JavaHome with a Windows JDK (17 or newer).'
}
$dependencyFile = Join-Path $repository 'forge-gui-mobile-dev/target/smoke-classpath.txt'
if (-not (Test-Path $dependencyFile)) {
    throw 'First run: mvn -B -ntp -pl forge-gui-mobile-dev -am test dependency:build-classpath -Dmdep.outputFile=target/smoke-classpath.txt'
}
if (Test-Path (Join-Path $repository 'forge-gui/forge.profile.properties')) {
    throw 'A resource-directory profile override exists. Use a clean checkout for this isolated test; it must not load a normal profile.'
}
if (-not $OutputPath) {
    $OutputPath = Join-Path $repository ('dist/bellwake-smoke/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6))
}
$output = [IO.Path]::GetFullPath($OutputPath)
if (Test-Path $output) { throw 'Output already exists; choose a new directory to preserve previous test evidence.' }
$classes = Join-Path $output 'classes'
$profile = Join-Path $output 'profile'
New-Item -ItemType Directory -Force $classes, "$profile/roaming/Forge/preferences", "$profile/local" | Out-Null
[IO.File]::WriteAllText((Join-Path $profile 'roaming/Forge/preferences/forge.preferences'), "UI_SELECTOR_MODE=Adventure`n")
$reactor = @('forge-core', 'forge-game', 'forge-ai', 'forge-gui', 'forge-gui-mobile', 'forge-gui-mobile-dev' | ForEach-Object {
    Join-Path $repository "$_/target/classes"
}) -join ';'
$classpath = ($reactor + ';' + (Get-Content $dependencyFile -Raw).Trim()).Replace('\', '/')
$sources = Get-ChildItem (Join-Path $repository 'forge-gui-mobile-dev/test/forge/app') -Filter '*.java' | ForEach-Object {
    '"' + $_.FullName.Replace('\', '/') + '"'
}
$compileArgs = "--release 17`n-cp`n`"$classpath`"`n-d`n`"$($classes.Replace('\', '/'))`"`n" + ($sources -join "`n")
$compileFile = Join-Path $output 'compile.args'
[IO.File]::WriteAllText($compileFile, $compileArgs)
& (Join-Path $JavaHome 'bin/javac.exe') "@$compileFile"
if ($LASTEXITCODE -ne 0) { throw "Graphical fixture compilation failed; see $output" }
[xml]$pom = Get-Content (Join-Path $repository 'forge-gui-mobile-dev/pom.xml')
$mandatory = $pom.project.properties.'mandatory.java.args'.Split(' ', [StringSplitOptions]::RemoveEmptyEntries)
$runtimeArgs = ($mandatory -join "`n") + "`n-Xmx4g`n-Dbellwake.isolatedProfile=true`n-Dbellwake.width=$Width`n-Dbellwake.height=$Height`n-Dbellwake.starter=$StarterColor`n" +
    '"-Dbellwake.expected=' + (Join-Path $output 'expected.properties').Replace('\', '/') + '"' + "`n" +
    '"-Duser.home=' + $profile.Replace('\', '/') + '"' + "`n" +
    '"-Dbellwake.screenshot=' + (Join-Path $output 'quay-restored.png').Replace('\', '/') + '"' + "`n-cp`n" +
    '"' + $classpath + ';' + $classes.Replace('\', '/') + '"' + "`nforge.app.InteriorRestorationSmoke`n"
$runtimeFile = Join-Path $output 'runtime.args'
[IO.File]::WriteAllText($runtimeFile, $runtimeArgs)
$priorAppData = $env:APPDATA
$priorLocalAppData = $env:LOCALAPPDATA
$log = Join-Path $output 'smoke.log'
Push-Location (Join-Path $repository 'forge-gui-mobile-dev')
try {
    $env:APPDATA = Join-Path $profile 'roaming'
    $env:LOCALAPPDATA = Join-Path $profile 'local'
    & (Join-Path $JavaHome 'bin/java.exe') "@$runtimeFile" *> $log
    $result = $LASTEXITCODE
    if ($result -eq 0 -and (Get-Content $log -Raw) -match 'BELLWAKE_ASSISTED_STORY_PASS') {
        $resumeFile = Join-Path $output 'resume.args'
        [IO.File]::WriteAllText($resumeFile, "-Dbellwake.resume=true`n" + $runtimeArgs)
        & (Join-Path $JavaHome 'bin/java.exe') "@$resumeFile" *> (Join-Path $output 'resume.log')
        $result = $LASTEXITCODE
    }
} finally {
    $env:APPDATA = $priorAppData
    $env:LOCALAPPDATA = $priorLocalAppData
    Pop-Location
}
$text = Get-Content $log -Raw
if ($result -ne 0 -or $text -notmatch 'BELLWAKE_INTERIOR_SMOKE_PASS' -or $text -notmatch 'BELLWAKE_ASSISTED_STORY_PASS') {
    Get-Content $log -Tail 25
    throw "Graphical smoke did not pass. Evidence: $output"
}
if (-not (Test-Path (Join-Path $output 'resume.log')) -or (Get-Content (Join-Path $output 'resume.log') -Raw) -notmatch 'BELLWAKE_FRESH_CONTINUE_PASS') {
    throw "Fresh-process Continue did not pass. Evidence: $output"
}
Write-Output "Graphical restoration and ASSISTED story checks passed. Evidence: $output"
Write-Output 'This supplies victories. It does not establish played match wins, balance, or unassisted gameplay acceptance.'
