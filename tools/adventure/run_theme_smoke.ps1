param(
    [string]$SourceRoot = (Join-Path $PSScriptRoot '../..'),
    [string]$JavaHome = 'C:\Program Files\Java\jdk-26',
    [string]$OutputPath = '',
    [string]$ProfileRoot = '',
    [string]$ThemeId = 'folio',
    [ValidateRange(320,3840)][int]$Width = 1280,
    [ValidateRange(240,2160)][int]$Height = 720,
    [switch]$FullScenes,
    [switch]$Bespoke,
    [ValidateSet('','back','slider','keyboard','cache','assets')][string]$InputProbe='',
    [switch]$Resume
)
$ErrorActionPreference='Stop'
$themeSource=(Resolve-Path -LiteralPath $SourceRoot).Path
if($ThemeId -notmatch '^[a-z][a-z0-9._-]{0,79}$'){throw 'Invalid theme ID'}
foreach($themeGuard in @('forge.profile.properties','forge-gui/forge.profile.properties','forge-gui-mobile-dev/forge.profile.properties')){
    if(Test-Path -LiteralPath (Join-Path $themeSource $themeGuard)){throw "Profile override $themeGuard exists; do not probe a normal profile."}
}
if(!$OutputPath){$OutputPath=Join-Path $themeSource ('.build-cache/adventure-themes/'+$ThemeId+'-'+$Width+'x'+$Height+'-'+[guid]::NewGuid().ToString('N').Substring(0,8))}
$themeOutput=[IO.Path]::GetFullPath($OutputPath)
if(Test-Path -LiteralPath $themeOutput){throw 'Choose a fresh evidence directory'}
New-Item -ItemType Directory -Path $themeOutput | Out-Null
$themeClasses=Join-Path $themeOutput 'classes';New-Item -ItemType Directory -Path $themeClasses | Out-Null
if(!$ProfileRoot){$ProfileRoot=Join-Path $themeOutput 'profile'}
$themeProfile=[IO.Path]::GetFullPath($ProfileRoot)
$themeMarker=Join-Path $themeProfile 'THEME-DISPOSABLE-PROFILE'
if($Resume){if(!(Test-Path -LiteralPath $themeMarker)){throw 'Resume requires a profile created by this script'}}
else{
    if(Test-Path -LiteralPath $themeProfile){throw 'First run requires a new disposable profile directory'}
    New-Item -ItemType Directory -Path (Join-Path $themeProfile 'roaming/Forge/preferences'),(Join-Path $themeProfile 'local') | Out-Null
    [IO.File]::WriteAllText($themeMarker,$themeSource)
    [IO.File]::WriteAllText((Join-Path $themeProfile 'roaming/Forge/preferences/forge.preferences'),"UI_SELECTOR_MODE=Adventure`nUI_ENABLE_ONLINE_IMAGE_FETCHER=false`n")
    $themeSettings=Join-Path $themeProfile 'roaming/Forge/adventure/settings.json'
    New-Item -ItemType Directory -Path (Split-Path -Parent $themeSettings) | Out-Null
    $themeInitial=@{adventureThemeId=$ThemeId;width=$Width;height=$Height;fullScreen=$false;plane='Bellwake'}
    if($Bespoke){
        if($ThemeId -ne 'campaign'){throw 'Bespoke compatibility expects -ThemeId campaign'}
        $themeInitial.Remove('adventureThemeId');$themeInitial.plane='<user>ThemeBespoke'
        $themeSkin=Join-Path $themeProfile 'roaming/Forge/adventure/userplanes/ThemeBespoke/skin'
        New-Item -ItemType Directory -Path $themeSkin | Out-Null
        Copy-Item -Path (Join-Path $themeSource 'forge-gui/res/adventure/common/skin/*') -Destination $themeSkin
        $themeWorld=Join-Path $themeProfile 'roaming/Forge/adventure/userplanes/ThemeBespoke/world'
        New-Item -ItemType Directory -Path $themeWorld | Out-Null
        Copy-Item -LiteralPath (Join-Path $themeSource 'forge-gui/res/adventure/Bellwake/world/quests.json') -Destination (Join-Path $themeWorld 'quests.json')
        $themeSkinJson=Get-Content -LiteralPath (Join-Path $themeSkin 'ui_skin.json') -Raw | ConvertFrom-Json -AsHashtable
        $themeSkinJson.LabelStyle['custom-theme-probe']=@{font='default';fontColor='RGBA_255_255_255_255'}
        $themeSkinJson | ConvertTo-Json -Depth 15 | Set-Content -LiteralPath (Join-Path $themeSkin 'ui_skin.json')
    }
    [IO.File]::WriteAllText($themeSettings,($themeInitial | ConvertTo-Json))
}
$themeReport=Get-ChildItem -LiteralPath (Join-Path $themeSource 'forge-gui-mobile-dev/target/surefire-reports') -Filter 'TEST*.xml' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if(!$themeReport){throw 'Build the current Adventure reactor first; its exact native dependency classpath is required'}
$themeXml=[xml](Get-Content -LiteralPath $themeReport.FullName -Raw)
$themeCp=($themeXml.testsuite.properties.property | Where-Object name -EQ 'surefire.test.class.path').value
if(!$themeCp){throw 'Missing verified native classpath in Surefire report'}
$themeCp=$themeCp.Replace('\','/')
$themeReactor=@('forge-core','forge-game','forge-ai','forge-gui','forge-gui-mobile','forge-gui-mobile-dev' | ForEach-Object {
    (Join-Path $themeSource ($_+'/target/classes')).Replace('\','/')
}) -join ';'
$themeCp=$themeReactor+';'+$themeCp
$themeFixture=Join-Path $themeSource 'forge-gui-mobile-dev/test-unit/forge/app/AdventureThemeSmoke.java'
$themeArgs=Join-Path $themeOutput 'compile.args'
$themeCompile=@('--release','17','-encoding','UTF-8','-cp',('"'+$themeCp+'"'),'-d',('"'+$themeClasses.Replace('\','/')+'"'),('"'+$themeFixture.Replace('\','/')+'"'))
[IO.File]::WriteAllLines($themeArgs,[string[]]$themeCompile)
& (Join-Path $JavaHome 'bin/javac.exe') ('@'+$themeArgs) *> (Join-Path $themeOutput 'compile.log')
if($LASTEXITCODE -ne 0){Get-Content -LiteralPath (Join-Path $themeOutput 'compile.log') -Tail 30;throw 'Native theme fixture compilation failed'}
if(!(Test-Path -LiteralPath (Join-Path $themeClasses 'forge/app/AdventureThemeSmoke.class'))){throw 'The current source did not produce its native fixture class'}
Copy-Item -LiteralPath $themeFixture -Destination (Join-Path $themeOutput 'AdventureThemeSmoke.java')
[xml]$themePom=Get-Content -LiteralPath (Join-Path $themeSource 'forge-gui-mobile-dev/pom.xml')
$themeRuntime=@($themePom.project.properties.'mandatory.java.args'.Split(' ',[StringSplitOptions]::RemoveEmptyEntries))
$themeRuntime+=@('-Xms128m','-Xmx1536m','-XX:ActiveProcessorCount=2','-Dtheme.isolatedProfile=true',('-Dtheme.width='+$Width),('-Dtheme.height='+$Height),('-Dtheme.expected='+$ThemeId),
    ('"-Dtheme.source='+$themeSource.Replace('\','/')+'"'),('"-Dtheme.output='+$themeOutput.Replace('\','/')+'"'),('"-Duser.home='+$themeProfile.Replace('\','/')+'"'))
if($FullScenes){$themeRuntime+='-Dtheme.fullScenes=true'}
if($Bespoke){$themeRuntime+='-Dtheme.bespoke=true'}
if($InputProbe){$themeRuntime+='-Dtheme.inputProbe='+$InputProbe}
if($Resume){$themeRuntime+='-Dtheme.resume=true'}
$themeRuntime+=@('-cp',('"'+$themeClasses.Replace('\','/')+';'+$themeCp+'"'),'forge.app.AdventureThemeSmoke')
$themeArgs=Join-Path $themeOutput 'runtime.args';[IO.File]::WriteAllLines($themeArgs,[string[]]$themeRuntime)
$themeIdentity=@{sourceRoot=$themeSource;head=(git -C $themeSource rev-parse HEAD);dirtyPaths=@(git -C $themeSource status --short);theme=$ThemeId;width=$Width;height=$Height;profile=$themeProfile;fullScenes=[bool]$FullScenes;resume=[bool]$Resume}
$themeHashPaths=@(git -C $themeSource diff --name-only;git -C $themeSource ls-files --others --exclude-standard) | Sort-Object -Unique
$themeIdentity.sourceFiles=@($themeHashPaths | ForEach-Object {Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $themeSource $_) | Select-Object Path,Hash})
$themeIdentity | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $themeOutput 'source-identity.json')
$themeOldAppData=$env:APPDATA;$themeOldLocal=$env:LOCALAPPDATA
Push-Location $themeSource
try{
    $env:APPDATA=Join-Path $themeProfile 'roaming';$env:LOCALAPPDATA=Join-Path $themeProfile 'local'
    & (Join-Path $JavaHome 'bin/java.exe') ('@'+$themeArgs) *> (Join-Path $themeOutput 'smoke.log')
    $themeExit=$LASTEXITCODE
}finally{$env:APPDATA=$themeOldAppData;$env:LOCALAPPDATA=$themeOldLocal;Pop-Location}
if($themeExit -ne 0 -or !(Test-Path -LiteralPath (Join-Path $themeOutput 'PASS'))){
    Get-Content -LiteralPath (Join-Path $themeOutput 'smoke.log') -Tail 45
    throw "Native theme validation failed; evidence: $themeOutput"
}
Get-Content -LiteralPath (Join-Path $themeOutput 'PASS')
Write-Output "Evidence: $themeOutput"
