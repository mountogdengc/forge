@echo off
setlocal EnableExtensions DisableDelayedExpansion
pushd "%~dp0" || exit /b 1
if not exist "runtime\bin\java.exe" goto incomplete
if not exist "forge-bellwake-diagnostic.jar" goto incomplete
if not exist "java-options.txt" goto incomplete
if not exist "forge.profile.properties" goto badprofile
findstr /x /c:"userDir=./test-profile" "forge.profile.properties" >nul
if errorlevel 1 goto badprofile
findstr /x /c:"cacheDir=./test-cache" "forge.profile.properties" >nul
if errorlevel 1 goto badprofile
powershell.exe -NoProfile -Command "if ((Get-FileHash -LiteralPath 'forge-bellwake-diagnostic.jar' -Algorithm SHA256).Hash -ne '018a851ba19ce1c8756036f0708b25b64510d73f5c34496f3d1e0e5e73d88075') { exit 1 }"
if errorlevel 1 goto badclient
if not exist "logs" mkdir "logs"
if exist "logs\capped-launcher.log" copy /y "logs\capped-launcher.log" "logs\capped-launcher.previous.log" >nul
set "APPDATA=%CD%\test-platform\roaming"
set "LOCALAPPDATA=%CD%\test-platform\local"
echo Bellwake playtest: 30 FPS, audio on, 1920x1080, 1 GB Java heap, two reported processors.
echo There is NO automatic timeout.
echo Keep this console open while playing. Errors go to logs\capped-launcher.log.
"runtime\bin\java.exe" @java-options.txt -Xms128m -Xmx1g -XX:ActiveProcessorCount=2 -Dforge.desktop.diagnostic=true -Dforge.desktop.diagnosticAudio=true "-Duser.home=%CD%\test-home" -jar "forge-bellwake-diagnostic.jar" landscape width=1920 height=1080 1>"logs\capped-launcher.log" 2>&1
set "result=%ERRORLEVEL%"
if not "%result%"=="0" (
  echo The game exited with an error. Keep logs\capped-launcher.log.
  pause
)
popd
exit /b %result%
:badclient
echo STOPPED: This launcher requires the exact audio-capable client used in the successful timed test.
pause
popd
exit /b 1
:badprofile
echo STOPPED: Portable profile settings are missing or changed.
pause
popd
exit /b 1
:incomplete
echo STOPPED: Place this file beside the installed audio diagnostic JAR and bundled runtime.
pause
popd
exit /b 1
