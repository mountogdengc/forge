param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$OutputPath = "",
    [string]$AssemblyJar = "",
    [string]$PackageRoot = "",
    [string]$ResumeFrom = "",
    [string]$CampaignProjectRoot = "",
    [switch]$PresentationProbe,
    [switch]$HelpProbe,
    [switch]$PromptProbe,
    [switch]$Hidden,
    [switch]$RequireDeferred,
    [switch]$StrahdProbe,
    [switch]$EditorButtonProbe,
    [ValidateRange(320, 3840)][int]$Width = 1280,
    [ValidateRange(240, 2160)][int]$Height = 720,
    [ValidateSet('W', 'R', 'B')][string]$StarterColor = 'W'
)

$ErrorActionPreference = 'Stop'
$repository = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if ($HelpProbe -and $PromptProbe) { throw 'Choose either HelpProbe or PromptProbe.' }
if ($ResumeFrom -and ($PackageRoot -or $HelpProbe -or $PromptProbe)) { throw 'ResumeFrom uses an AssemblyJar and its own disposable copied profile.' }
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
[IO.File]::WriteAllText((Join-Path $profile 'roaming/Forge/preferences/forge.preferences'), "UI_SELECTOR_MODE=Adventure`nUI_ENABLE_ONLINE_IMAGE_FETCHER=false`n")
if ($ResumeFrom) {
    $prior = (Resolve-Path -LiteralPath $ResumeFrom).Path
    if (-not (Test-Path (Join-Path $prior 'expected.properties')) -or -not (Test-Path (Join-Path $prior 'profile/roaming/Forge/adventure'))) {
        throw 'ResumeFrom must be a completed older smoke run with expectations and its disposable profile.'
    }
    Copy-Item -LiteralPath (Join-Path $prior 'expected.properties') -Destination (Join-Path $output 'expected.properties')
    Get-ChildItem -LiteralPath (Join-Path $prior 'profile/roaming/Forge') -Force | Copy-Item -Destination (Join-Path $profile 'roaming/Forge') -Recurse -Force
}
$authoringCampaignId = ""
if ($CampaignProjectRoot) {
    if ($PackageRoot -or $ResumeFrom) { throw 'CampaignProjectRoot requires a new isolated source run.' }
    $projectSource = (Resolve-Path -LiteralPath $CampaignProjectRoot).Path
    $projectManifest = Get-Content -LiteralPath (Join-Path $projectSource 'campaign.json') -Raw | ConvertFrom-Json
    $authoringCampaignId = $projectManifest.id
    if ($authoringCampaignId -notmatch '^user\.[A-Za-z0-9._-]+$') { throw 'Expected a newly authored campaign identity.' }
    $projectParent = Join-Path $profile 'roaming/Forge/adventure/userplanes'
    New-Item -ItemType Directory -Force $projectParent | Out-Null
    Copy-Item -LiteralPath $projectSource -Destination (Join-Path $projectParent 'Authoring-Test') -Recurse
}
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
$runtimeArgs = ($mandatory -join "`n") + "`n-Xms128m`n-Xmx1g`n-XX:ActiveProcessorCount=2`n-Dbellwake.isolatedProfile=true`n-Dbellwake.width=$Width`n-Dbellwake.height=$Height`n-Dbellwake.starter=$StarterColor`n" +
    '"-Dbellwake.expected=' + (Join-Path $output 'expected.properties').Replace('\', '/') + '"' + "`n" +
    '"-Duser.home=' + $profile.Replace('\', '/') + '"' + "`n" +
    '"-Dbellwake.screenshot=' + (Join-Path $output 'quay-restored.png').Replace('\', '/') + '"' + "`n-cp`n" +
    '"' + $classpath + ';' + $classes.Replace('\', '/') + '"' + "`nforge.app.InteriorRestorationSmoke`n"
$runtimeFile = Join-Path $output 'runtime.args'
if ($PresentationProbe) { $runtimeArgs = "-Dbellwake.presentationProbe=true`n" + $runtimeArgs }
if ($HelpProbe) { $runtimeArgs = "-Dbellwake.helpProbe=true`n" + $runtimeArgs }
if ($PromptProbe) { $runtimeArgs = "-Dbellwake.promptProbe=true`n" + $runtimeArgs }
if ($ResumeFrom) { $runtimeArgs = "-Dbellwake.resume=true`n" + $runtimeArgs }
if ($PackageRoot) { $runtimeArgs = "-Dbellwake.packagedProbe=true`n" + $runtimeArgs }
if ($Hidden) { $runtimeArgs = "-Dbellwake.hidden=true`n" + $runtimeArgs }
if ($RequireDeferred) { $runtimeArgs = "-Dbellwake.requireDeferred=true`n" + $runtimeArgs }
if ($StrahdProbe) {
    $runtimeArgs = "-Dstrahd.chapterProbe=true`n" + $runtimeArgs
    if ($ResumeFrom) { $runtimeArgs = "-Dstrahd.resume=true`n" + $runtimeArgs }
}
if ($EditorButtonProbe) { $runtimeArgs = "-Deditor.buttonProbe=true`n" + $runtimeArgs }
if ($authoringCampaignId) { $runtimeArgs = "-Dbellwake.campaignId=$authoringCampaignId`n" + $runtimeArgs }
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
    if ($StrahdProbe -and !$ResumeFrom -and $result -eq 0 -and (Get-Content $log -Raw) -match 'STRAHD_ASSISTED_CHAPTER_PASS') {
        $resumeFile = Join-Path $output 'strahd-resume.args'
        [IO.File]::WriteAllText($resumeFile, "-Dstrahd.resume=true`n" + $runtimeArgs)
        & $runtimeJava "@$resumeFile" *> (Join-Path $output 'resume.log')
        $result = $LASTEXITCODE
    }
} finally {
    $env:APPDATA = $priorAppData
    $env:LOCALAPPDATA = $priorLocalAppData
    Pop-Location
}
$text = Get-Content $log -Raw
if ($EditorButtonProbe) {
    if ($result -ne 0 -or $text -notmatch 'CAMPAIGN_EDITOR_BUTTON_PASS') {
        Get-Content $log -Tail 35
        throw "Campaign Editor startup button did not pass. Evidence: $output"
    }
    Write-Output "Campaign Editor startup-menu integration passed. Evidence: $output"
    return
}
if ($StrahdProbe) {
    if ($ResumeFrom -and $result -eq 0 -and $text -match 'STRAHD_COMPLETED_ACTOR_RECONCILIATION_PASS') {
        Write-Output "Older Strahd profile restored and completed actors removed. Evidence: $output"
        return
    }
    if ($result -ne 0 -or $text -notmatch 'STRAHD_ASSISTED_CHAPTER_PASS' -or
        !(Test-Path (Join-Path $output 'resume.log')) -or
        (Get-Content (Join-Path $output 'resume.log') -Raw) -notmatch 'STRAHD_FRESH_CONTINUE_PASS') {
        Get-Content $log -Tail 35
        throw "Strahd assisted chapter check did not pass. Evidence: $output"
    }
    Write-Output "Strahd ASSISTED chapter checks passed. Evidence: $output"
    return
}
if ($PromptProbe) {
    if ($result -ne 0 -or $text -notmatch 'BELLWAKE_PROMPT_LAYOUT_PASS') {
        Get-Content $log -Tail 25
        throw "Prompt layout probe did not pass. Evidence: $output"
    }
    Write-Output "Bottom prompt layout passed, normal and reversed. Evidence: $output"
    return
}
if ($ResumeFrom) {
    if ($result -ne 0 -or $text -notmatch 'BELLWAKE_FRESH_CONTINUE_PASS') {
        Get-Content $log -Tail 25
        throw "Older-profile Continue did not pass. Evidence: $output"
    }
    Write-Output "Older client profile restored in the updated client. Evidence: $output"
    return
}
if ($HelpProbe) {
    if ($result -ne 0 -or $text -notmatch 'BELLWAKE_MATCH_HELP_PASS' -or $text -notmatch 'BELLWAKE_PRACTICE_LOSS_PASS') {
        Get-Content $log -Tail 25
        throw "Match Help probe did not pass. Evidence: $output"
    }
    Write-Output "Actual match Help dialogs rendered. Evidence: $output"
    return
}
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
