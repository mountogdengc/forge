param([string]$JavaHome = 'C:/Program Files/Java/jdk-26')

# Owner-invoked visual acceptance launcher. Uses current source classes/assets
# and a separate persistent review profile; never copies over a normal save.
$ErrorActionPreference = 'Stop'
$artRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$artEvidence = Join-Path $artRoot '.build-cache/barovia-gothic'
$artDependencyFile = Join-Path $artRoot 'forge-gui-mobile-dev/target/smoke-classpath.txt'
if (!(Test-Path -LiteralPath $artDependencyFile)) {
    throw 'Build and prepare the current source classpath first; see docs/Adventure/Barovia-Gothic-Art.md.'
}
$artJava = Join-Path $JavaHome 'bin/java.exe'
if (!(Test-Path -LiteralPath $artJava)) { throw 'Pass -JavaHome pointing to a Windows JDK.' }
foreach ($artGuard in @('forge.profile.properties', 'forge-gui/forge.profile.properties', 'forge-gui-mobile-dev/forge.profile.properties')) {
    if (Test-Path -LiteralPath (Join-Path $artRoot $artGuard)) { throw "Remove or relocate the profile override $artGuard before using this isolated review launcher." }
}
$artProfile = Join-Path $artEvidence 'manual-profile'
$artAdventure = Join-Path $artProfile 'roaming/Forge/adventure'
New-Item -ItemType Directory -Force -Path $artAdventure, (Join-Path $artProfile 'roaming/Forge/preferences'), (Join-Path $artProfile 'local') | Out-Null
$artPrefs = Join-Path $artProfile 'roaming/Forge/preferences/forge.preferences'
if (!(Test-Path -LiteralPath $artPrefs)) {
    [IO.File]::WriteAllText($artPrefs, "UI_SELECTOR_MODE=Adventure`nUI_ENABLE_ONLINE_IMAGE_FETCHER=false`n")
}
$artSettings = Join-Path $artAdventure 'settings.json'
if (!(Test-Path -LiteralPath $artSettings)) {
    [IO.File]::WriteAllText($artSettings, '{"width":1920,"height":1080,"fullScreen":false,"videomode":"1080p","plane":"CurseOfStrahd"}')
}
$artReactor = @('forge-core', 'forge-game', 'forge-ai', 'forge-gui', 'forge-gui-mobile', 'forge-gui-mobile-dev' | ForEach-Object {
    Join-Path $artRoot ($_ + '/target/classes')
}) -join ';'
$artClasspath = $artReactor + ';' + (Get-Content -LiteralPath $artDependencyFile -Raw).Trim()
[xml]$artPom = Get-Content -LiteralPath (Join-Path $artRoot 'forge-gui-mobile-dev/pom.xml') -Raw
$artArguments = @($artPom.project.properties.'mandatory.java.args'.Split(' ', [StringSplitOptions]::RemoveEmptyEntries))
$artArguments += @('-Xms128m', '-Xmx1g', '-XX:ActiveProcessorCount=2', ('-Duser.home=' + $artProfile), '-cp', $artClasspath, 'forge.app.Main', 'width=1920', 'height=1080', 'landscape')
$artPriorAppData = $env:APPDATA
$artPriorLocalAppData = $env:LOCALAPPDATA
Push-Location (Join-Path $artRoot 'forge-gui-mobile-dev')
try {
    $env:APPDATA = Join-Path $artProfile 'roaming'
    $env:LOCALAPPDATA = Join-Path $artProfile 'local'
    & $artJava @artArguments *> (Join-Path $artEvidence 'manual-playtest.log')
    if ($LASTEXITCODE -ne 0) { throw "Game exited with $LASTEXITCODE; see .build-cache/barovia-gothic/manual-playtest.log." }
} finally {
    $env:APPDATA = $artPriorAppData
    $env:LOCALAPPDATA = $artPriorLocalAppData
    Pop-Location
}
