"""Build a small Bellwake update against the verified Windows base client.

Only MusicPlaylist.class changes inside the compatible base JAR. No runtime,
launcher, normal profiles or unrelated game resources are copied/modified.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]
BASE_COMMIT = "6747fa4da56e340e8b6031f5000f994c536ac293"
BASE_JAR_SHA = "d63e21139095faec06d4f12c1083c3bf27f166a2bebff526ccdb230e0ca33236"
CLASS = "forge/sound/MusicPlaylist.class"
JAVA_SOURCE = "forge-gui/src/main/java/forge/sound/MusicPlaylist.java"


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def safe_name(name):
    path = PurePosixPath(name)
    if path.is_absolute() or ".." in path.parts or "\\" in name or ":" in name:
        raise ValueError("Unsafe package path: " + name)
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", type=Path, required=True, help="Extracted verified 6747fa4da56e Windows client")
    parser.add_argument("--output", type=Path, required=True, help="New evidence/output folder; never overwritten")
    parser.add_argument("--java-home", type=Path, default=Path(os.environ.get("JAVA_HOME", "")))
    args = parser.parse_args()
    base, output = args.base.resolve(), args.output.resolve()
    jar = base / "forge-adventure-test.jar"
    if not jar.is_file() or digest(jar) != BASE_JAR_SHA:
        raise RuntimeError("Use the verified 6747fa4da56e base client; its JAR checksum does not match.")
    if output.exists():
        raise RuntimeError("Output already exists; choose a new folder to preserve evidence.")
    manifest = json.loads((ROOT / "forge-gui/res/adventure/Bellwake/campaign.json").read_text(encoding="utf-8"))
    if manifest["version"] != "0.3.0":
        raise RuntimeError("This update/checklist is specifically for Bellwake 0.3.0.")
    # A class-level update is only safe while every other application source is unchanged.
    changed = subprocess.check_output(["git", "diff", "--name-only", BASE_COMMIT, "--", "*.java"], cwd=ROOT, text=True).splitlines()
    production = {p for p in changed if "/src/" in p and "/test/" not in p}
    if production != {JAVA_SOURCE}:
        raise RuntimeError("A full client build is required for other Java changes: " + str(sorted(production)))
    if subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip():
        raise RuntimeError("Commit the reviewed update sources before recording artifact identity.")
    source_commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    javac = args.java_home / "bin/javac.exe"
    if not javac.is_file():
        raise RuntimeError("Pass --java-home with a Windows JDK (17 or newer).")
    stage, classes = output / "update", output / "compiler-classes"
    stage.mkdir(parents=True)
    classes.mkdir()
    subprocess.run([str(javac), "--release", "17", "-cp", str(jar), "-d", str(classes), str(ROOT / JAVA_SOURCE)], check=True)
    compiled = list(classes.rglob("*.class"))
    if {p.relative_to(classes).as_posix() for p in compiled} != {CLASS}:
        raise RuntimeError("Unexpected compiler output; refusing an incomplete class patch.")
    patch = (classes / CLASS).read_bytes()
    print("Compiling and validating the compatible client update...", flush=True)
    with zipfile.ZipFile(jar) as source, zipfile.ZipFile(stage / jar.name, "w") as target:
        entries = source.infolist()
        if sum(entry.filename == CLASS for entry in entries) != 1:
            raise RuntimeError("Expected exactly one base MusicPlaylist class.")
        if any(entry.filename.upper().startswith("META-INF/") and entry.filename.upper().endswith((".SF", ".RSA", ".DSA")) for entry in entries):
            raise RuntimeError("Refusing to modify a signed JAR.")
        for entry in entries:
            target.writestr(entry, patch if entry.filename == CLASS else source.read(entry))
    with zipfile.ZipFile(stage / jar.name) as result:
        if result.testzip() or result.read(CLASS) != patch:
            raise RuntimeError("Updated JAR integrity check failed.")
    print("Copying the chapter-only assets and manual checklist...", flush=True)
    resources = [ROOT / "forge-gui/res/adventure/Bellwake/campaign.json"]
    resources += list((ROOT / "forge-gui/res/adventure/Bellwake/music").rglob("*"))
    resources += [ROOT / ("forge-gui/res/adventure/common/maps/map/bellwake/" + name + ".tmx") for name in ("quay", "toll")]
    for path in resources:
        if path.is_file():
            destination = stage / path.relative_to(ROOT / "forge-gui")
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, destination)
    shutil.copyfile(ROOT / "docs/Adventure/Bellwake-Manual-Playtests.md", stage / "Bellwake-Manual-Playtests.md")
    sources = [JAVA_SOURCE, "tools/adventure/build_bellwake_presentation.py", "tools/adventure/test_bellwake_presentation.py",
               "tools/adventure/package_bellwake_update.py", "tools/adventure/run_bellwake_smoke.ps1",
               "forge-gui-mobile-dev/test/forge/app/InteriorRestorationSmoke.java"]
    for name in sources:
        destination = stage / "source" / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / name, destination)
    (stage / "READ-ME-FIRST.txt").write_text("""BELLWAKE 0.3.0 - ART AND AMBIENCE TEST UPDATE

Requires the complete Forge-Adventure-Windows-Test-6747fa4da56e base client.
Keep the old installation for old saves. Extract the full base ZIP into a NEW
writable folder, close the client, then copy this update's contents beside
Start-Adventure-Test.cmd, merging res and replacing the included files.
Launch Start-Adventure-Test.cmd and create a FRESH Bellwake run.
The Java runtime and launcher come from the base client. No Java install is needed.
0.1.0/0.2.0 chapter saves are not migrated into the changed 0.3.0 geometry.

Read Bellwake-Manual-Playtests.md for numbered actions and expected results.
Start with runs A and B, then the remaining acceptance cases. Automated checks
supply victories; they do not establish real-match balance or a human finish.

The update includes original procedural music beds, native tile-map composition,
and a compatible JAR with only MusicPlaylist.class changed to fix cross-campaign
cache invalidation. Modified sources are included under source/ for review; use
a complete matching Forge checkout to regenerate or run developer checks.
Existing Forge rules, AI, decks, rewards and save format are retained.

BUILD-INFO.json records identity. SHA256SUMS.txt describes the complete pristine
base-plus-update installation; it supersedes the base manifest. Mutable settings,
saves and logs will change after play. Preserve all base license/credit notices.
The sibling update ZIP .sha256 file verifies the downloaded update archive.
""", encoding="utf-8")
    base_info = json.loads((base / "BUILD-INFO.json").read_text(encoding="utf-8"))
    info = {"source_commit": source_commit, "built_at_utc": datetime.now(timezone.utc).isoformat(),
            "target": "Windows x64", "bellwake_content_version": "0.3.0",
            "base_client_source_commit": BASE_COMMIT, "base_jar_sha256": BASE_JAR_SHA,
            "jar_sha256": digest(stage / jar.name), "changed_client_entries": [CLASS],
            "class_sha256": hashlib.sha256(patch).hexdigest(), "compiler_release": 17,
            "java": base_info["java"], "profile": "test-profile", "cache": "test-cache",
            "base_client_test_evidence": base_info["tests"],
            "verification": "Update packager checks JAR/ZIP integrity. New-source/native and manual evidence is recorded separately in docs/Adventure/Progression-Validation.md.",
            "manual_playtest": "Pending: see Bellwake-Manual-Playtests.md"}
    (stage / "BUILD-INFO.json").write_text(json.dumps(info, indent=2) + "\n", encoding="utf-8")
    hashes = {}
    for line in (base / "SHA256SUMS.txt").read_text(encoding="utf-8").splitlines():
        checksum, name = line.split("  ", 1)
        safe_name(name)
        if len(checksum) != 64 or any(c not in "0123456789abcdef" for c in checksum):
            raise ValueError("Invalid base checksum")
        inherited = base / name
        if not inherited.is_file() or digest(inherited) != checksum:
            raise RuntimeError("Base installation differs from its pristine manifest: " + name)
        hashes[name] = checksum
    if hashes.get(jar.name) != BASE_JAR_SHA:
        raise RuntimeError("Base checksum manifest disagrees with the verified client.")
    for path in stage.rglob("*"):
        if path.is_file():
            hashes[path.relative_to(stage).as_posix()] = digest(path)
    (stage / "SHA256SUMS.txt").write_text("".join(f"{checksum}  {name}\n" for name, checksum in sorted(hashes.items())), encoding="utf-8")
    archive = output / "Bellwake-Art-Ambience-0.3.0.zip"
    print("Writing and verifying the update ZIP...", flush=True)
    with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as target:
        for path in sorted(stage.rglob("*")):
            if path.is_file():
                target.write(path, path.relative_to(stage))
    with zipfile.ZipFile(archive) as verify:
        if verify.testzip():
            raise RuntimeError("Update ZIP failed CRC validation.")
        if any(name.startswith(("test-profile/", "test-cache/", "runtime/")) for name in verify.namelist()):
            raise RuntimeError("Update must not contain user data or replace the runtime.")
    checksum = digest(archive)
    archive.with_suffix(".zip.sha256").write_text(f"{checksum}  {archive.name}\n", encoding="ascii")
    print(json.dumps({"archive": str(archive), "bytes": archive.stat().st_size,
                      "sha256": checksum, "jar_sha256": info["jar_sha256"], "source_commit": source_commit}, indent=2))


if __name__ == "__main__":
    main()
