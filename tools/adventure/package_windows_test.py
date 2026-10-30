#!/usr/bin/env python3
"""Package a tested Adventure assembly with a Windows JDK and isolated profile.

Run after a clean Maven verify. No downloads, source edits, or existing-profile
writes occur here. Existing output is never overwritten.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

REPOSITORY_URL = "https://github.com/mountogdengc/forge"
REQUIRED_CLASSES = (
    "forge/app/Main.class",
    "forge/adventure/scene/CampaignSelectionScene.class",
    "forge/adventure/campaign/CampaignRegistry.class",
    "forge/adventure/world/PlaythroughRepository.class",
)
REQUIRED_RESOURCES = (
    "adventure/common/ui/campaign_select.json",
    "adventure/common/ui/campaign_saves.json",
    "adventure/common/ui/campaign_select_portrait.json",
    "adventure/common/ui/campaign_saves_portrait.json",
    "skins/default/adv_bg_texture.jpg",
)
PROFILE = (
    "# Portable test profile. Never redirect this to a production profile.\n"
    "userDir=./test-profile\ncacheDir=./test-cache\n"
    "cardPicsDir=./test-cache/pics/cards\n"
    "decksDir=./test-profile/decks\n"
    "decksConstructedDir=./test-profile/decks/constructed\n"
)
LAUNCHER = r'''@echo off
setlocal EnableExtensions DisableDelayedExpansion
pushd "%~dp0" || exit /b 1
if not exist "runtime\bin\java.exe" goto incomplete
if not exist "forge-adventure-test.jar" goto incomplete
if not exist "java-options.txt" goto incomplete
if not exist "res\adventure\common\ui\campaign_select.json" goto incomplete
if not exist "forge.profile.properties" goto badprofile
findstr /x /c:"userDir=./test-profile" "forge.profile.properties" >nul
if errorlevel 1 goto badprofile
findstr /x /c:"cacheDir=./test-cache" "forge.profile.properties" >nul
if errorlevel 1 goto badprofile
if not exist "logs" mkdir "logs"
if exist "logs\launcher.log" copy /y "logs\launcher.log" "logs\launcher.previous.log" >nul
set "APPDATA=%CD%\test-platform\roaming"
set "LOCALAPPDATA=%CD%\test-platform\local"
echo Starting Forge Adventure with a separate test profile.
echo Keep this window open while playing. Errors go to logs\launcher.log.
"runtime\bin\java.exe" @java-options.txt -Xms256m -Xmx4g "-Duser.home=%CD%\test-home" -jar "forge-adventure-test.jar" landscape 1>"logs\launcher.log" 2>&1
set "result=%ERRORLEVEL%"
if not "%result%"=="0" (
  echo The game exited with an error. See logs\launcher.log and test-profile\forge.log.
  pause
)
popd
exit /b %result%
:badprofile
echo STOPPED: The isolated profile file is missing or changed.
echo Extract a fresh test package. This launcher will not use your normal Forge profile.
pause
popd
exit /b 1
:incomplete
echo STOPPED: The test package is incomplete. Extract the whole ZIP before launching.
pause
popd
exit /b 1
'''
README = r'''FORGE ADVENTURE -- WINDOWS x64 TEST BUILD
Source commit: {sha}
Source: {source_url}

START
1. Extract the entire ZIP into a new writable folder, such as C:\Games\Forge-Adventure-Test.
   Do not extract over an existing Forge installation or run from inside the ZIP.
2. Double-click Start-Adventure-Test.cmd. Java is included; no system Java is needed.
3. This package starts with a fresh Adventure profile. If a mode menu appears, choose
   Adventure. Do not install upstream updates over this fork. The GIT build revision
   skips the upstream startup updater.
4. Return to the overworld before saving or switching playthroughs.

YOUR NORMAL SAVES
The package uses test-profile/ and test-cache/ inside its extracted folder. The launcher
checks the expected profile paths; APPDATA, LOCALAPPDATA and Java user.home are also
redirected for this process only. It does not import or copy your normal Forge profile.
Do not edit these settings during initial testing. Deleting this folder deletes its
TEST saves too. Copy test-profile/ before removing a test installation you wish to keep.

FIRST TEST
[ ] New Game shows the campaign picker before character creation.
[ ] Create Shandalar Alpha. Reach the overworld and save a named checkpoint.
[ ] Preview a different campaign and cancel. Alpha is unchanged.
[ ] Create Shandalar Beta. Both Shandalar runs appear in Load Game.
[ ] Create Old Border Gamma in Shandalar Old Border. All three runs remain available.
[ ] Alternate between runs. Character, deck, gold, map and quests match each run.
[ ] In each run, make a manual save, press F5 to quicksave, and trigger an autosave by
    entering a location. Return to the overworld before loading another run.
[ ] Press F8 on the overworld. It restores the CURRENT run's quicksave.
[ ] Exit normally after Gamma, relaunch, and Continue. It restores Gamma.

LIMITS
Test build, not a stable release. Saving/switching inside interiors remains blocked.
Campaign content versions must match exactly; there is no content-version migration.
Historical-save import, New Game+ gameplay and a complete campaign need further testing.
No Curse of Strahd campaign is included. Start with fresh characters for this test.
Automated build/package checks are not a Windows graphical gameplay pass.

REPORTING A PROBLEM
Keep BUILD-INFO.json, logs/launcher.log and test-profile/forge.log. Record the campaign,
run, last action, expected result and actual result. A relevant test-save copy and a
screenshot help. Review logs/saves for character names and local paths before sharing.
Card images may download on demand into the test cache.

PROVENANCE
BUILD-INFO.json records source commit, Java version, test totals and the JAR checksum.
SHA256SUMS.txt covers initial package files; mutable profiles/logs change during play.
The sibling .zip.sha256 file verifies the ZIP. Preserve all license notices. Source
and build scripts are available at the exact source URL above.
'''


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def test_evidence(root: Path) -> dict:
    reports = sorted(root.glob("*/target/surefire-reports/TEST-*.xml"))
    if not reports:
        raise RuntimeError("No Surefire reports: run clean verify with tests enabled.")
    totals = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    classes: set[str] = set()
    for report in reports:
        doc = ET.parse(report).getroot()
        suites = [doc] if doc.tag == "testsuite" else list(doc.findall("testsuite"))
        for suite in suites:
            for key in totals:
                totals[key] += int(suite.get(key, "0"))
            for case in suite.findall("testcase"):
                if not any(case.find(tag) is not None for tag in ("skipped", "error", "failure")):
                    classes.add(case.get("classname", ""))
    required = {"forge.adventure.campaign.CampaignRegistryTest", "forge.adventure.world.PlaythroughRepositoryTest"}
    if totals["tests"] == 0 or totals["failures"] or totals["errors"]:
        raise RuntimeError(f"Tests did not pass: {totals}")
    if not required.issubset(classes):
        raise RuntimeError(f"Missing passing campaign/save suites: {sorted(required - classes)}")
    return {**totals, "report_files": len(reports), "scope": "forge-gui-mobile-dev and required reactor modules"}


def check_jar(jar: Path) -> None:
    with zipfile.ZipFile(jar) as archive:
        if archive.testzip():
            raise RuntimeError("JAR failed CRC validation.")
        manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8").replace("\r\n ", "").replace("\n ", "")
        attributes = dict(line.split(": ", 1) for line in manifest.splitlines() if ": " in line)
        if attributes.get("Main-Class") != "forge.app.Main":
            raise RuntimeError("The JAR does not start the Adventure desktop client.")
        if "GIT" not in attributes.get("Implementation-Version", ""):
            raise RuntimeError("Use a GIT test revision to disable the upstream startup updater.")
        names = set(archive.namelist())
        missing = set(REQUIRED_CLASSES) - names
        if missing:
            raise RuntimeError(f"Missing Adventure implementation: {sorted(missing)}")
        dlls = [name.lower() for name in names if name.lower().endswith(".dll")]
        for family in ("gdx", "lwjgl", "glfw"):
            if not any(family in name for name in dlls):
                raise RuntimeError(f"Missing Windows native library family: {family}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=Path.cwd())
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if sys.platform != "win32" or platform.machine().lower() not in ("amd64", "x86_64"):
        raise RuntimeError("Package on Windows x64 using the Windows JDK from the build.")
    root = args.repository.resolve()
    sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    if len(sha) != 40 or any(c not in "0123456789abcdef" for c in sha):
        raise RuntimeError("Cannot establish the source commit.")
    if os.environ.get("GITHUB_SHA") and os.environ["GITHUB_SHA"] != sha:
        raise RuntimeError("Checked-out source does not match the workflow commit.")
    if not os.environ.get("JAVA_HOME"):
        raise RuntimeError("JAVA_HOME is required.")
    jdk = Path(os.environ["JAVA_HOME"]).resolve()
    java = jdk / "bin/java.exe"
    if not java.is_file() or not (jdk / "legal").is_dir():
        raise RuntimeError("Windows JDK and its license notices are required.")
    version = subprocess.run([str(java), "-version"], capture_output=True, text=True, check=True)
    jars = list((root / "forge-gui-mobile-dev/target").glob("*-jar-with-dependencies.jar"))
    if len(jars) != 1:
        raise RuntimeError(f"Expected one clean Adventure assembly JAR, found {len(jars)}.")
    check_jar(jars[0])
    evidence = test_evidence(root)
    resources = root / "forge-gui/res"
    for name in REQUIRED_RESOURCES:
        if not (resources / name).is_file():
            raise RuntimeError(f"Required resource is missing: {name}")
    for folder in ("cardsfolder", "editions", "tokenscripts"):
        if not (resources / folder).is_dir():
            raise RuntimeError(f"Required game data is missing: {folder}")
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    pom = ET.parse(root / "forge-gui-mobile-dev/pom.xml")
    options = pom.findtext("m:properties/m:mandatory.java.args", namespaces=ns)
    if not options or "${" in options:
        raise RuntimeError("Cannot resolve mandatory launcher Java options.")
    license_file = next((p for p in (root / "LICENSE", root / "LICENSE.txt") if p.is_file()), None)
    if license_file is None:
        raise RuntimeError("Repository license file is missing.")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    package = output / f"Forge-Adventure-Windows-Test-{sha[:12]}"
    archive_path = package.with_suffix(".zip")
    if package.exists() or archive_path.exists():
        raise RuntimeError("Output exists; refusing to overwrite a package or test profile.")
    package.mkdir()
    shutil.copy2(jars[0], package / "forge-adventure-test.jar")
    shutil.copytree(resources, package / "res")
    shutil.copytree(jdk, package / "runtime")
    shutil.copy2(license_file, package / "LICENSE.txt")
    for name in ("NOTICE", "NOTICE.txt", "CHANGES.txt"):
        if (root / name).is_file():
            shutil.copy2(root / name, package / name)
    (package / "forge.profile.properties").write_text(PROFILE, encoding="ascii")
    preferences = package / "test-profile/preferences"
    preferences.mkdir(parents=True)
    (preferences / "forge.preferences").write_text("UI_SELECTOR_MODE=Adventure\nUI_LANDSCAPE_MODE=true\n", encoding="ascii")
    (package / "java-options.txt").write_text(options.strip() + "\n", encoding="ascii")
    (package / "Start-Adventure-Test.cmd").write_bytes(LAUNCHER.replace("\n", "\r\n").encode("ascii"))
    source_url = f"{REPOSITORY_URL}/tree/{sha}"
    (package / "READ-ME-FIRST.txt").write_text(README.format(sha=sha, source_url=source_url), encoding="utf-8")
    info = {
        "source_repository": REPOSITORY_URL, "source_commit": sha, "source_url": source_url,
        "built_at_utc": datetime.now(timezone.utc).isoformat(), "target": "Windows x64",
        "java": (version.stderr + version.stdout).strip(), "tests": evidence,
        "jar_sha256": sha256(package / "forge-adventure-test.jar"),
        "profile": "test-profile", "cache": "test-cache",
        "windows_graphical_playtest": "Not performed by this packaging workflow",
    }
    (package / "BUILD-INFO.json").write_text(json.dumps(info, indent=2) + "\n", encoding="utf-8")
    with (package / "SHA256SUMS.txt").open("w", encoding="utf-8", newline="\n") as stream:
        for path in sorted(package.rglob("*")):
            if path.is_file() and path.name != "SHA256SUMS.txt":
                stream.write(f"{sha256(path)}  {path.relative_to(package).as_posix()}\n")
    with zipfile.ZipFile(archive_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as archive:
        for path in sorted(package.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(output))
    with zipfile.ZipFile(archive_path) as archive:
        if archive.testzip():
            raise RuntimeError("Package ZIP failed CRC validation.")
    checksum = sha256(archive_path)
    archive_path.with_suffix(".zip.sha256").write_text(f"{checksum}  {archive_path.name}\n", encoding="ascii")
    print(json.dumps({"archive": str(archive_path), "bytes": archive_path.stat().st_size, "sha256": checksum, "tests": evidence}, indent=2))


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.CalledProcessError, ET.ParseError, zipfile.BadZipFile, KeyError) as exc:
        print(f"Windows test packaging failed: {exc}", file=sys.stderr)
        sys.exit(1)
