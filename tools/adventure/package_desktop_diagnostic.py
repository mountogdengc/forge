"""Package an opt-in diagnostic add-on; never launches or changes an installed game."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--client-jar', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True).strip():
        raise ValueError('Record reviewed sources before packaging the diagnostic.')
    with zipfile.ZipFile(args.client_jar) as jar:
        if jar.testzip() or 'forge/app/DesktopGraphicsPolicy.class' not in jar.namelist():
            raise ValueError('Missing or corrupt diagnostic client.')
        manifest = jar.read('META-INF/MANIFEST.MF')
        if b'Main-Class: forge.app.Main' not in manifest or b'GITBellwakeAudioDiagnostic' not in manifest:
            raise ValueError('Wrong client entry point or revision.')
    cases = []
    for report in (ROOT / 'forge-gui-mobile-dev/target/surefire-reports').glob('TEST-*.xml'):
        cases.extend(ET.parse(report).getroot().iter('testcase'))
    cases = [case for case in cases if case.get('classname') == 'forge.app.DesktopGraphicsPolicyTest']
    if len(cases) != 3 or any(case.find(tag) is not None for case in cases for tag in ('failure', 'error', 'skipped')):
        raise ValueError('Require all three passing desktop configuration tests.')
    output = args.output.resolve()
    if output.exists():
        raise ValueError('Choose a new output directory; preserve earlier evidence.')
    output.mkdir(parents=True)
    files = {
        'forge-bellwake-diagnostic.jar': args.client_jar.read_bytes(),
        'Run-Bellwake-Diagnostic.ps1': (ROOT / 'tools/adventure/run_bellwake_diagnostic.ps1').read_bytes(),
        'Start-Bellwake-Diagnostic.cmd': b'@echo off\r\nsetlocal EnableExtensions DisableDelayedExpansion\r\npushd "%~dp0" || exit /b 1\r\npowershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Run-Bellwake-Diagnostic.ps1" -PackageRoot "%CD%" -Seconds 30\r\nset "result=%ERRORLEVEL%"\r\necho Keep the newest logs\\diagnostic-* folder for your report.\r\npause\r\npopd\r\nexit /b %result%\r\n',
        'DIAGNOSTIC-README.txt': b'''BELLWAKE DESKTOP-SLOWDOWN DIAGNOSTIC - NOT A CONFIRMED FIX

No game has been launched with this candidate. A driver/kernel stall can delay
its watchdog. Use only when you are ready for one controlled diagnostic run.

1. Copy these add-on files beside your EXISTING forge-adventure-test.jar, in:
   C:\\tmp\\Bellwake-Windows-0.3.0\\Bellwake-Windows-0.3.0
   The same folder must contain runtime, res and forge.profile.properties.
   Do not replace/delete the original JAR, profile, cache or ordinary launchers.
2. Run Start-Bellwake-Diagnostic.cmd. Keep its console OPEN until finished.
   It uses the separately named JAR, 30 FPS, no audio, 960x540 window, 1 GB heap
   and two reported processors. It requests termination after 30 seconds,
   including loading. Closing the console can cancel the watchdog.
3. STAY AT THE MENU. Do not load/create a character or enter a match.
   Observe whether the desktop/cursor remain responsive outside the game.
4. Keep the newest logs/diagnostic-* folder. Report: menu reached or not;
   when slowdown started; cursor/desktop responsiveness; auto-close outcome;
   responsiveness after closing. Initial loading may consume the whole test.

AUDIO COMPARISON (only after agreeing to another timed test):
Start-Bellwake-Diagnostic-Audio.cmd enables audio while retaining the exact same
30 FPS, 960x540 window, 1 GB heap, two reported processors and 30-second deadline.
It may reproduce the desktop slowdown. Do not use the ordinary launcher for
this comparison. The first no-audio run was responsive; cause remains unknown.

This changes multiple settings for low-load triage. Success alone cannot tell
which subsystem caused the problem. Logs record game output, CPU/memory samples,
optional bounded NVIDIA GPU queries and termination status. The original game
JAR is left intact. No installer, driver update or global system setting change.
''',
    }
    files['Start-Bellwake-Diagnostic-Audio.cmd'] = files['Start-Bellwake-Diagnostic.cmd'].replace(
        b'-Seconds 30\r\n', b'-Seconds 30 -AudioEnabled\r\n')
    sources = ['forge-gui-mobile-dev/src/forge/app/GameLauncher.java',
               'forge-gui-mobile-dev/src/forge/app/DesktopGraphicsPolicy.java',
               'forge-gui-mobile-dev/test-unit/forge/app/DesktopGraphicsPolicyTest.java',
               'forge-gui-mobile-dev/pom.xml', 'tools/adventure/package_desktop_diagnostic.py',
               'docs/Adventure/Desktop-Slowdown-Diagnosis.md']
    for name in sources:
        files['diagnostic-source/' + name] = (ROOT / name).read_bytes()
    info = {
        'source_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
        'jar_sha256': hashlib.sha256(files['forge-bellwake-diagnostic.jar']).hexdigest(),
        'configuration_tests_passed': 3,
        'game_run': False,
        'status': 'Unconfirmed cause; opt-in low-load diagnostic only',
    }
    files['DIAGNOSTIC-BUILD-INFO.json'] = (json.dumps(info, indent=2) + '\n').encode()
    files['DIAGNOSTIC-SHA256SUMS.txt'] = ''.join(hashlib.sha256(data).hexdigest() + '  ' + name + '\n'
        for name, data in sorted(files.items())).encode()
    archive = output / 'Bellwake-Desktop-Diagnostic.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as target:
        for name, data in sorted(files.items()): target.writestr(name, data)
    with zipfile.ZipFile(archive) as target:
        if target.testzip() or set(target.namelist()) != set(files): raise ValueError('ZIP verification failed')
        for name, data in files.items():
            if target.read(name) != data: raise ValueError('Diagnostic payload differs: ' + name)
    result = {'archive': str(archive), 'bytes': archive.stat().st_size,
              'sha256': hashlib.sha256(archive.read_bytes()).hexdigest(), 'files_verified': len(files), **info}
    (output / 'verification.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))

if __name__ == '__main__': main()
