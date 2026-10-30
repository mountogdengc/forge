# Isolated Windows Adventure test build

The `Adventure Windows test build` workflow builds this fork's libGDX Adventure
client on Windows x64 with Temurin JDK 17. It does not build the separate Swing
client, publish an upstream release, or merge the draft pull request.

## Download and run

Open the successful workflow run in this fork's Actions tab and download the
`forge-adventure-windows-test-<commit>` artifact. Extract that artifact ZIP, then
extract the contained `Forge-Adventure-Windows-Test-<commit>.zip` into a new writable
folder, such as `C:\Games\Forge-Adventure-Test`. Run `Start-Adventure-Test.cmd`.
Java is included. The package includes `READ-ME-FIRST.txt`, the three-playthrough
test checklist, `BUILD-INFO.json`, license notices, and SHA-256 checksums.

The launcher uses the package-local `test-profile` and `test-cache` directories.
It refuses to launch when its expected profile paths are missing and redirects
APPDATA, LOCALAPPDATA, and Java user.home for the child process. Do not install
over a normal Forge folder, edit the profile paths, or import important saves
for this first test. The GIT build revision skips the upstream startup updater;
the packager checks the manifest to avoid accidentally shipping an ordinary
upstream-updating snapshot.

## Build and verification scope

The workflow runs:

```powershell
mvn -U -B -ntp -pl forge-gui-mobile-dev -am clean verify "-Drevision=2.0.16-GITWindowsTest"
python tools/adventure/package_windows_test.py --output dist/adventure-windows
```

The single `GITWindowsTest` qualifier also satisfies Launch4j's version parser;
additional hyphens in the qualifier are rejected during EXE generation.

This runs tests for the Adventure client and required reactor modules, not the
full 12-module default reactor. Packaging requires passing campaign and save
regression suites, the Adventure main class, campaign/persistence classes,
Windows native library families, and required game resources. It verifies both
the assembled JAR and the resulting ZIP. Action dependencies are pinned to commit
SHAs. The job has read-only repository permissions and does not need publishing
credentials. Artifacts are retained for 14 days.

Build success does not establish graphical compatibility. Initial player testing
should use two Shandalar runs and one Shandalar Old Border run, named checkpoints,
F5/F8, campaign switching, and a full exit/restart/Continue. Return to the overworld
before saving or switching. Historical save import, New Game+, interior saving,
and a full campaign remain separate acceptance items.

The workflow is triggered by relevant pushes to `adventure/progression-foundation`.
A manual dispatch is also declared; GitHub's web Run workflow control normally
requires the workflow to exist on the default branch. Do not merge merely to
obtain that control: the branch's push event is sufficient when Actions is enabled.

If the build fails, inspect its failed step and the separate test-reports artifact.
No playable ZIP is claimed until a successful workflow actually uploads it.

## Provenance

OpenAI ChatGPT substantially assisted with the packaging helper, workflow, and
this documentation. Synthetic helper checks are not a Forge build or a Windows
playthrough. Runtime test counts and source identity belong in BUILD-INFO.json.
