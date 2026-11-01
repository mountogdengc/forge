"""Combine the verified Windows base and Bellwake update into one portable ZIP.

The base inputs are immutable, pinned archives. An optional tested full client
assembly replaces the old JAR for compatible playtest fixes. The completed
archive is extracted and every file verified in a NEW disposable test folder.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path, PurePosixPath
import subprocess
import zipfile

from package_windows_test import LAUNCHER, check_jar, test_evidence

ROOT = Path(__file__).resolve().parents[2]
BASE_SHA = "c417bc4d7b61b6f57ddbf56da8aaca8646a35cf224d3b07c80fd2da93551f75f"
UPDATE_SHA = "4fb223c4256253cbeeeed499b1c49848e5900800f1149968e9b7d6e6648c9278"
JAR_SHA = "bbc669f3447fd34fe258164ce3ee2a39bb69817c16f460ac46576c50076abced"
NAME = "Bellwake-Windows-0.3.0"


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def safe_name(name):
    path = PurePosixPath(name)
    if not name or path.is_absolute() or ".." in path.parts or "\\" in name or ":" in name:
        raise ValueError("Unsafe archive path: " + name)
    return path


def entries(archive, prefix=""):
    result = {}
    windows_names = set()
    for entry in archive.infolist():
        if entry.is_dir():
            continue
        if not entry.filename.startswith(prefix):
            raise ValueError("Entry outside expected archive root")
        name = entry.filename[len(prefix):]
        safe_name(name)
        if name.casefold() in windows_names:
            raise ValueError("Duplicate Windows archive path: " + name)
        windows_names.add(name.casefold())
        result[name] = entry
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-zip", type=Path, required=True)
    parser.add_argument("--update-zip", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--verify-dir", type=Path, required=True)
    parser.add_argument("--client-jar", type=Path,
                        help="Fresh tested full reactor assembly, plus current compatible Bellwake enemy definitions")
    args = parser.parse_args()
    client_tests = None
    if args.client_jar:
        if subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip():
            raise ValueError("Commit reviewed sources before recording the replacement client's identity")
        check_jar(args.client_jar)
        client_tests = test_evidence(ROOT)
    for path, expected in ((args.base_zip, BASE_SHA), (args.update_zip, UPDATE_SHA)):
        if digest(path) != expected:
            raise ValueError("Input archive checksum mismatch: " + str(path))
    output, verify = args.output.resolve(), args.verify_dir.resolve()
    if output.exists() or verify.exists():
        raise ValueError("Choose NEW output and verification folders; existing evidence is never overwritten")
    output.mkdir(parents=True)
    verify.mkdir(parents=True)
    archive_path = output / (NAME + ".zip")
    with zipfile.ZipFile(args.base_zip) as base, zipfile.ZipFile(args.update_zip) as update:
        base_entries = entries(base, "Forge-Adventure-Windows-Test-6747fa4da56e/")
        update_entries = entries(update)
        mapping = {name: (base, entry) for name, entry in base_entries.items()}
        mapping.update({name: (update, entry) for name, entry in update_entries.items()})
        # The update manifest is the complete base-plus-update inventory.
        inherited = {}
        for line in update.read("SHA256SUMS.txt").decode("utf-8").splitlines():
            checksum, name = line.split("  ", 1)
            safe_name(name)
            if name in inherited:
                raise ValueError("Duplicate checksum entry")
            inherited[name] = checksum
        if set(inherited) != set(mapping) - {"SHA256SUMS.txt"}:
            raise ValueError("Base-plus-update inventory disagrees with its checksum manifest")
        if inherited["forge-adventure-test.jar"] != JAR_SHA:
            raise ValueError("Unexpected client JAR")
        info = json.loads(update.read("BUILD-INFO.json"))
        info.update({"distribution": "Standalone portable Bellwake Windows x64 playtest build",
                     "packaged_at_utc": datetime.now(timezone.utc).isoformat(),
                     "packaging_source_commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
                     "input_base_zip_sha256": BASE_SHA, "input_update_zip_sha256": UPDATE_SHA,
                     "launcher": "Start-Bellwake.cmd", "default_campaign_plane": "Bellwake",
                     "package_verification": "All input entries hashed; output ZIP CRC and complete extraction hashes checked. Native packaged-runtime results recorded separately in Progression-Validation.md."})
        if args.client_jar:
            info["previous_update_source_commit"] = info["source_commit"]
            info["source_commit"] = info["packaging_source_commit"]
            info["jar_sha256"] = digest(args.client_jar)
            info["tests"] = client_tests
            info["client_build"] = "Full clean Maven reactor assembly; playtest fixes, not a one-class patch"
            info["verification"] = "Fresh reactor test results above; see Progression-Validation.md for native/UI proof."
            info.pop("changed_client_entries", None)
            info.pop("class_sha256", None)
        checklist = (ROOT / "docs/Adventure/Bellwake-Manual-Playtests.md").read_text(encoding="utf-8")
        start = checklist.index("1. Keep your existing")
        end = checklist.index("5. In Settings")
        checklist = checklist[:start] + """1. Extract this entire ZIP to a NEW writable folder, for example `C:/Games/Bellwake-0.3-Test`. Do not run inside the ZIP or merge with another installation. Java and the art/audio update are included.
2. Double-click **Start-Bellwake.cmd**. It uses this folder's isolated test profile and cache; your normal Forge profile is separate.
3. Choose **New Game**. **The Last Fare of Bellwake** is preselected; choose **Create character**, then the starter required below. Use a fresh run; older 0.1.0/0.2.0 saves need their matching older installation.
4. Use English for the first report so the labels below match. Name each run as specified in the matrix.
""" + checklist[end:]
        readme = """BELLWAKE 0.3.0 - WINDOWS x64 PLAYTEST BUILD

1. Extract the ENTIRE ZIP into a new writable folder (e.g. C:/Games/Bellwake-Test).
2. Double-click Start-Bellwake.cmd. Java, art and ambient audio are included.
3. Choose New Game. The Last Fare of Bellwake is preselected. Choose Create
   character, then Lantern Watch for test A or Ember Crew for test B.
4. Follow Bellwake-Manual-Playtests.md. Start with runs A and B.

No separate base package, patch or Java installation is needed. Do not run from
inside the ZIP or install over another Forge folder. Keep the launcher window
open while playing. Card images may download on demand.

Saving works during stable exploration, including interiors. Finish battles,
conversations, rewards and transitions before saving/loading. Old 0.1/0.2 saves
need their matching older installation; create fresh Bellwake 0.3 runs here.

The test-profile and test-cache folders keep this build separate from your normal
Forge saves. Back up test-profile before removing the extracted folder. Preserve
BUILD-INFO.json, logs/launcher.log and test-profile/forge.log when reporting a bug.

Automated/assisted checks do not establish real-match balance, listening quality
or controller acceptance. Those are the manual playtests in the included guide.
SHA256SUMS.txt covers pristine package files; settings/saves/logs change in play.
Preserve all included license notices. Client source identities are recorded in
BUILD-INFO.json; modified client sources accompany the build under source/.
"""
        replacement = {
            "Start-Bellwake.cmd": LAUNCHER.replace("Starting Forge Adventure", "Starting Bellwake").replace("\n", "\r\n").encode("ascii"),
            "READ-ME-FIRST.txt": readme.encode("utf-8"),
            "Bellwake-Manual-Playtests.md": checklist.encode("utf-8"),
            "BUILD-INFO.json": (json.dumps(info, indent=2) + "\n").encode("utf-8"),
            "test-profile/adventure/settings.json": b'{"plane":"Bellwake","width":1280,"height":720}\n',
            "source/tools/adventure/package_bellwake_windows.py": Path(__file__).read_bytes(),
        }
        if args.client_jar:
            replacement["forge-adventure-test.jar"] = args.client_jar.read_bytes()
            replacement["res/adventure/Bellwake/world/enemies.json"] = (ROOT / "forge-gui/res/adventure/Bellwake/world/enemies.json").read_bytes()
            readme += "\nPLAYTEST FIXES: named draws/discards in postgame log only; match-menu Help and\ncontrol tips; menu loading fixed; losing to Tor stays in the quay.\n\nKEEPING YOUR 0.3.0 RUNS: close both clients, back up the old test-profile folder,\nthen copy test-profile from your previous Bellwake 0.3.0 installation into this\nfresh extraction before launching. Keep the old folder too. No geometry, campaign\nversion or save-format changes were made. Never copy your normal Forge profile.\n"
            replacement["READ-ME-FIRST.txt"] = readme.encode("utf-8")
            # Include precisely the changed client sources, not unrelated local files.
            changed = subprocess.check_output(["git", "diff", "--name-only", "6747fa4da56e", "--", "*.java"], cwd=ROOT, text=True).splitlines()
            for name in changed:
                path = ROOT / name
                if path.is_file():
                    replacement["source/" + name] = path.read_bytes()
        hashes = {}
        print("Writing single Windows ZIP and validating all inherited bytes...", flush=True)
        with zipfile.ZipFile(archive_path, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as target:
            for name in sorted(set(mapping) | set(replacement)):
                if name == "SHA256SUMS.txt":
                    continue
                if name in mapping:
                    source, entry = mapping[name]
                    data = source.read(entry)  # Also validates the input entry CRC.
                    if hashlib.sha256(data).hexdigest() != inherited[name]:
                        raise ValueError("Inherited file checksum mismatch: " + name)
                if name in replacement:
                    data = replacement[name]
                hashes[name] = hashlib.sha256(data).hexdigest()
                target.writestr(NAME + "/" + name, data)
            manifest = "".join(f"{checksum}  {name}\n" for name, checksum in sorted(hashes.items())).encode("utf-8")
            target.writestr(NAME + "/SHA256SUMS.txt", manifest)
    print("Extracting a disposable copy and verifying every output file...", flush=True)
    with zipfile.ZipFile(archive_path) as package:
        if package.testzip():
            raise ValueError("Combined ZIP CRC check failed")
        for entry in package.infolist():
            safe_name(entry.filename)
        package.extractall(verify)
    extracted = verify / NAME
    actual_files = {p.relative_to(extracted).as_posix() for p in extracted.rglob("*") if p.is_file()}
    if actual_files != set(hashes) | {"SHA256SUMS.txt"}:
        raise ValueError("Extracted package inventory differs")
    for name, checksum in hashes.items():
        if digest(extracted / name) != checksum:
            raise ValueError("Extracted file checksum mismatch: " + name)
    check_jar(extracted / "forge-adventure-test.jar")
    version = subprocess.run([str(extracted / "runtime/bin/java.exe"), "-version"], capture_output=True, text=True, check=True)
    checksum = digest(archive_path)
    archive_path.with_suffix(".zip.sha256").write_text(f"{checksum}  {archive_path.name}\n", encoding="ascii")
    result = {"archive": str(archive_path), "bytes": archive_path.stat().st_size, "sha256": checksum,
              "files_verified": len(hashes) + 1, "extracted_for_test": str(extracted),
              "bundled_java": (version.stdout + version.stderr).strip()}
    (output / "package-verification.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2), flush=True)


if __name__ == "__main__":
    main()
