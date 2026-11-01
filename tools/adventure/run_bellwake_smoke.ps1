param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$OutputPath = "",
    [string]$AssemblyJar = "",
    [string]$PackageRoot = "",
    [switch]$PresentationProbe,
    [ValidateRange(320, 3840)][int]$Width = 1280,
    [ValidateRange(240, 2160)][int]$Height = 720,
    [ValidateSet('W', 'R')][string]$StarterColor = 'W'
)

$ErrorActionPreference = 'Stop'
$repository = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$launchDirectory = Join-Path $repository 'forge-gui-mobile-dev'
$runtimeJava = Join-Path $JavaHome 'bin/java.exe'
if ($PackageRoot) {
    $launchDirectory = (Resolve-Path -LiteralPath $PackageRoot).Path
    $AssemblyJar = Join-Path $launchDirectory 'forge-adventure-test.jar'
    $runtimeJava = Join-Path $launchDirectory 'runtime/bin/java.exe'
    $portableProfile = Join-Path $launchDirectory 'forge.profile.properties'
    if (-not (Test-Path $runtimeJava) -or -not (Test-Path $portableProfile) -or
        (Get-Content $portableProfile) -notcontains 'userDir=./test-profile' -or
        (Get-Content $portableProfile) -notcontains 'cacheDir=./test-cache') {
        throw 'PackageRoot must be a disposable extracted portable test build with an isolated profile.'
    }
    if (Get-ChildItem -LiteralPath (Join-Path $launchDirectory 'test-profile/adventure/campaign-saves') -Recurse -File -ErrorAction SilentlyContinue) {
        throw 'PackageRoot already has playthroughs. Extract a NEW disposable verification copy.'
    }
}
if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome 'bin/javac.exe'))) {
    throw 'Set JAVA_HOME or pass -JavaHome with a Windows JDK (17 or newer).'
}
$dependencyFile = Join-Path $repository 'forge-gui-mobile-dev/target/smoke-classpath.txt'
if (-not $AssemblyJar -and -not (Test-Path $dependencyFile)) {
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
if ($AssemblyJar) {
    $classpath = (Resolve-Path -LiteralPath $AssemblyJar).Path.Replace('\', '/')
} else {
    $classpath = ($reactor + ';' + (Get-Content $dependencyFile -Raw).Trim()).Replace('\', '/')
}
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
if ($PresentationProbe) { $runtimeArgs = "-Dbellwake.presentationProbe=true`n" + $runtimeArgs }
if ($PackageRoot) { $runtimeArgs = "-Dbellwake.packagedProbe=true`n" + $runtimeArgs }
[IO.File]::WriteAllText($runtimeFile, $runtimeArgs)
$priorAppData = $env:APPDATA
$priorLocalAppData = $env:LOCALAPPDATA
$log = Join-Path $output 'smoke.log'
Push-Location $launchDirectory
try {
    $env:APPDATA = Join-Path $profile 'roaming'
    $env:LOCALAPPDATA = Join-Path $profile 'local'
    & $runtimeJava "@$runtimeFile" *> $log
    $result = $LASTEXITCODE
    if ($result -eq 0 -and (Get-Content $log -Raw) -match 'BELLWAKE_ASSISTED_STORY_PASS') {
        $resumeFile = Join-Path $output 'resume.args'
        [IO.File]::WriteAllText($resumeFile, "-Dbellwake.resume=true`n" + $runtimeArgs)
        & $runtimeJava "@$resumeFile" *> (Join-Path $output 'resume.log')
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
if ($PresentationProbe -and ($text -notmatch 'BELLWAKE_PRESENTATION_AUDIO_PASS' -or ([regex]::Matches($text, 'BELLWAKE_PRESENTATION_GEOMETRY_PASS')).Count -ne 2)) {
    throw "Presentation audio/geometry markers missing. Evidence: $output"
}
if ($PackageRoot -and $text -notmatch 'BELLWAKE_PORTABLE_START_PASS') {
    throw "Portable startup marker missing. Evidence: $output"
}
Write-Output "Graphical restoration and ASSISTED story checks passed. Evidence: $output"
Write-Output 'This supplies victories. It does not establish played match wins, balance, or unassisted gameplay acceptance.'
