# Adventure progression validation

Use this checklist with the [fork roadmap](Fork-Roadmap.md). It distinguishes a code/data review, an automated check, and an actual game run. Record evidence for each result; an unchecked case remains unverified.

This checklist validates the current progression patch and the inherited content it changes. It does not require future campaigns to retain that content. A replacement campaign or Adventure system needs acceptance cases for its chosen design and an explicit policy for existing saves. Apply these regressions where their systems or content are retained, and keep unrun legacy cases marked unverified; retiring content does not turn an unrun case into a pass.

The campaign picker and independent-playthrough architecture are implemented as described in [Campaigns and saves](Campaigns-and-Saves.md). The campaign gate below records their automated and interactive coverage separately from the earlier progression fixes. Partial coverage does not turn an entire acceptance case into a pass.

## Bottom prompt clipping, November 1, 2026

The reported bottom corners used legacy FButton corner artwork drawn at 150% of
the control height, with a downward caption offset and a forced break in End Turn.
VPrompt now uses contained regular buttons, inset from every edge of the prompt
bar. Normal/reversed placement and the message/menu/Undo reserved areas remain.
No input commands, gameplay rules, campaign data or save format changed.

Native RED: `dist/bellwake-smoke/20261101-104723-573e9a` rejected the oversized
corner renderer in an actual match. The affected reactor package passed 91 cases,
zero failures/errors/skips; log `.bellwake-prompt-fix.log`, queue job
`d3f1fcd37e40493285e4e8d3d6bfa4bc`, both exit codes 0. Unchanged desktop test
reports remain the earlier 1,328-case run, including its six existing skips.

Native GREEN: `dist/bellwake-smoke/20261101-105646-561e26`, 1280x720, actual
OK/End Turn priority prompt after keeping the opening hand. Button bounds are
strictly inside the bar, the cropped renderer is absent, and normal/reversed
positions are asserted. `prompt-normal.png` and `prompt-reversed.png` were
inspected: complete borders and captions are visible. The wrapper exited 0.
An earlier geometry-only capture used the starting-hand labels; the final probe
waits for the real End Turn prompt instead of substituting labels.

Final standalone artifact identity and extracted viewport proof follow packaging.

## First manual playtest fixes, November 1, 2026

The user supplied a real Tor loss and reported successful saving, an
unfinished-action error when loading, an unwanted exit from town after practice,
and missing explanations of phases, stops and match controls. No deck rebalance
was inferred from that first loss.

The menu's own loading overlay was tripping the unchanged restore guard. Menu
loads now check outgoing actions before creating the overlay, then remove that
owned overlay before synchronous restoration. Actual battle/reward/dialogue and
stage-action guards remain. The shipped JAR failed the real SaveLoadScene callback
at phase 3 in `dist/bellwake-smoke/20261101-101457-d05b08`. A load-only patched
probe passed that path and then reproduced practice ejection at phase 11 in
`20261101-101646-58baaf`. The final client passed both paths.

Tor explicitly declares friendly practice. A loss restores idle movement and
keeps the player and tutor inside the quay without applying defeat penalties;
ordinary enemy defeat remains unchanged. The enemy-data copy policy and content
are tested. No campaign version, actor geometry or save-format change was made.

Named actual draws/discards and additional zone transfers now use a private
postgame log channel. Every ordinary getter filters that channel, including
maximum/custom logging, reconnect history and the live dropdown. Private entries
cannot carry card links. Local desktop/mobile results screens explicitly request
the detailed history. Network forwarding omits private named events; remote
postgame/replay history therefore remains redacted for these details. Tests cover
privacy, duplicate suppression, undo truncation and forwarding. The old forwarding
test initially failed because it expected all raw events; it now requires every
public event exactly once/in order, requires private events in its workload, and
rejects any private delivery.

Final full Maven reactor package: 1,419 cases, zero failures/errors, six existing
desktop optional skips. Queue job `cb7053017d414d87891f26bc774f170a` returned both
exit codes 0; log `.bellwake-playtest-fixes-final.log`. The earlier full gate and
failure are preserved in `.bellwake-playtest-fixes-reactor.log`, job
`54fcf469e6444a80abc32493c7e5537a`. The new GUI forwarding test follows that module's
`*Should.java` discovery convention.

Native Help probes: `dist/bellwake-smoke/20261101-103334-0c34ac` (1280x720) and
`20261101-103452-6770a1` (720x1280), both exited 0. They exercised menu loading,
practice loss, actual match startup and named opening-hand draws hidden from live
queries, rendered all three Help topics and checked tooltip removal after a new
dialog changes the active container. The phase-stop captures were inspected;
landscape fits and portrait uses the existing scrollable text panel. Hover/hold
physical input and broader controller acceptance remain manual. The first Help
probe waited at the real Play/Draw prompt; its helper now selects only that named
startup choice before checking actual draw events. This does not supply a played
match win.

Older-save compatibility: `dist/bellwake-smoke/20261101-103542-846abd` copied the
completed 0.3.0 profile from `20261031-215018-e58d78`, preserving the original. New
client Continue restored the old interior, position, reduced life, gold, deck,
ending and one-time reward, kept the Keeper removed, and applied the friendly Tor
policy. The wrapper exited 0. This tests the earlier client's saved payload, not
the user's particular private save file.

Fresh review found no critical/important issues. The tooltip-container lifecycle
concern was fixed by pinning the screen while visible and covered in the native
Help probe. New Java sources are tracked before standalone packaging. Updated
manual follow-up steps are in [Bellwake manual playtests](Bellwake-Manual-Playtests.md).
Final Windows artifact:
`dist/adventure-windows/bellwake-playtest-fixes-ee8431f7625-ready/Bellwake-Windows-0.3.0.zip`.
Source `ee8431f7625c2b4505240c21aae07df29e404a94`; 483,515,349 bytes; SHA256
`69598169cfc4f1c267f067f0cd7e01194f00b7efdf7ae759612b1cca695012da`.
New full client JAR SHA256:
`645e802a0ca66956c31299996b602e56134505c61294d6a7e751ac9d2996c80a`.
Packaging job `04128b3864d147c1a274d0d9e2f015c7` returned both exit codes 0.
ZIP CRCs and every one of the 56,412 extracted files passed verification; bundled
Java 26 ran. Use `source_commit`, `jar_sha256` and `packaged_at_utc` for this
artifact's identity; the inherited `built_at_utc` describes the earlier art update.
The actual launcher loaded Adventure menu resources:
`dist/bellwake-smoke/playtest-fixes-launcher-ee8431f7625`, exit 0.

Final extracted runtime: `dist/bellwake-smoke/20261101-104310-b82a0c`, Ember Crew,
1280x720. Wrapper exit 0. It used the ZIP's bundled runtime, JAR, resources and
portable profile, and passed actual menu-load restoration, friendly practice loss,
five audio routes/decoding, both map collision probes, connected-room/rollback
fixtures, both assisted ending branches and fresh-process Continue. The shipped
ZIP remains pristine; only the separate verification extraction has fixture saves.

## Standalone Bellwake Windows package, November 1, 2026

Ready-to-extract build:
`dist/adventure-windows/bellwake-0.3.0-ready/Bellwake-Windows-0.3.0.zip`.
Size: 483,085,243 bytes. SHA256:
`5e7cb8f090d5c0cd543f61eae4ffca0fceb6d9e8d69d201f736c4111ec5d94db`.
Extract the entire ZIP to a new writable folder, open its Bellwake-Windows-0.3.0
folder and run `Start-Bellwake.cmd`. New Game preselects The Last Fare of
Bellwake. Java, art/audio, isolated profile settings and the standalone manual
playtest guide are included; no base-plus-update installation is needed.

This combines the exact pinned base and update archives recorded below. The
client JAR remains `bbc669f3447fd34fe258164ce3ee2a39bb69817c16f460ac46576c50076abced`;
no gameplay code is rebuilt. Packaging adds a named launcher and fresh Bellwake
settings, and updates handoff/identity/checksum files. The original archives and
installations remain unchanged. Queue job `75ce02cc0f024aeca9fa0e38b8ad4645`
returned both exit codes 0. Every inherited entry was checked against the merged
manifest, ZIP CRCs passed, and all 56,387 files in a fresh extraction were
verified. The bundled Java 26 executable ran successfully. Evidence:
`dist/adventure-windows/bellwake-0.3.0-ready/package-verification.json`.

The actual CMD launcher started its own bundled Java and loaded Adventure start
menu resources. Evidence: `dist/bellwake-smoke/standalone-launcher-0.3.0-fixed`.
The startup-only probe then terminated only its owned client process; it does not
claim a played match or normal shutdown. The first probe failed because its log
reader did not share the active Windows log for writing. The reader was fixed;
the package bytes were unchanged, and the repeated probe passed.

Extracted native story/Continue proof:
`dist/bellwake-smoke/20261101-002731-5d438f`, 1280x720, Lantern Watch. The wrapper
exited 0. Bellwake default selection and the extracted portable profile passed;
all five campaign audio routes/decoding and both map entry/river collision probes
passed. Interior restoration, failed-load rollback, connected-room/shop fixtures,
both assisted ending branches and fresh-process Continue passed. The screenshot
`quay-restored.png` was inspected. Runtime, JAR and resources came from the fresh
ZIP extraction, with no dependency on the source checkout's resource directory.
The extraction was deliberately used only for verification; distribute the
unchanged ZIP so each tester gets fresh settings and no fixture playthroughs.

Reproduce on a NEW disposable extraction (never a player's test folder):

```powershell
./tools/adventure/verify_bellwake_launcher.ps1 -PackageRoot '<fresh-extracted-folder>' -EvidencePath '<new-evidence-folder>'
./tools/adventure/run_bellwake_smoke.ps1 -JavaHome 'C:/Program Files/Java/jdk-26' -PackageRoot '<fresh-extracted-folder>' -PresentationProbe -StarterColor W -Width 1280 -Height 720
```

The native fixture disables hardware audio and supplies victories. Actual matches,
subjective listening, physical controller input and measured pacing remain the
manual checklist; no new gameplay acceptance is claimed by packaging.

Fresh packaging review found no critical/important defects. Optional generalized
Windows path hardening (reserved names/trailing spaces and combined case checks)
is deferred: this helper accepts only two exact SHA-pinned archives and verifies
the complete output inventory and extracted bytes. It is not an arbitrary ZIP
merger. Real-launch coverage is separate from the native fixture's direct entry
point, as recorded above.

## Bellwake 0.3.0 art/ambient candidate, October 31, 2026

Source/art snapshot: `3c8c94ba7ce` on `codex/bellwake-chapter-polish`.
The waterfront and tollhouse now use authored 30x30 tile compositions, preserving
actor world positions and IDs; five original 48-second ambient beds use the
existing music playlists. A reproduced shared playlist invalidation bug is fixed
with independent per-playlist generation tracking. Content version 0.3.0 requires
a fresh run; preserve the matching old installation for older saves.

Checks actually run:

- Five Python asset contracts passed, including reachable interaction/exit paths,
  blocked water, unchanged actor coordinates, portrait map dimensions and WAV
  format/levels. A sealed-landing negative fixture proves the path check rejects
  blocked routes. Final run includes the paper-covered ledger tile.
- Eight focused chapter/content-validator cases passed. The affected Maven reactor
  passed 87 cases (70 Adventure), zero failures/errors/skips. Queue job
  `f934d7c6e4e64215b3ed9c9e6f857d3d` returned both exit codes 0; full log is
  `.bellwake-presentation-reactor.log` (2026-10-31 21:43 America/Denver).
- Native audio regression RED: `dist/bellwake-smoke/20261031-212727-019ea8`
  warmed common playlists, activated Bellwake, refreshed BLACK, then incorrectly
  received common TOWN music. Source GREEN:
  `dist/bellwake-smoke/20261031-213313-404a01` resolved all five campaign tracks,
  decoded their full PCM data with libGDX, completed assisted story/restoration
  and fresh-process Continue. Hardware audio was disabled; this is no listening
  or mix-quality claim.
- Fresh-context review reported no critical/important defects. Its optional
  manifest concern was addressed: packaging verifies every inherited base file
  against its pristine checksum before borrowing any manifest entry.

Windows update: `dist/bellwake-presentation/3c8c94ba7ce/Bellwake-Art-Ambience-0.3.0.zip`
(64,979,672 bytes), SHA256
`4fb223c4256253cbeeeed499b1c49848e5900800f1149968e9b7d6e6648c9278`.
The updated JAR SHA256 is
`bbc669f3447fd34fe258164ce3ee2a39bb69817c16f460ac46576c50076abced`.
The package contains only chapter resources, the compatible one-class JAR update,
source copies and handoff/identity/checksum files; the verified base ZIP supplies
the launcher and runtime. The packager compiled with release 17, validated every
inherited base file, checked JAR/ZIP CRCs, and rejected profile/cache/runtime
entries. Queue job `e8c989c2311d445baa9e86dbaf724df6` returned both exit codes 0.
Keep the base installation/archive unchanged; apply the update to a fresh copy.

Final delivery-JAR native proof: `dist/bellwake-smoke/20261031-215018-e58d78`,
720x1280, Ember Crew, same updated JAR SHA256 as above. The wrapper exited 0.
Both map geometry markers passed (entry clear, river blocked), as did the five
playlist routing/WAV decoding marker, interior restoration, both assisted endings,
reward persistence and fresh-process Continue. `quay-restored.png` was visually
inspected: river/shoreline fills portrait rather than an empty map region.
Reproduce with:

```powershell
./tools/adventure/run_bellwake_smoke.ps1 -JavaHome 'C:/Program Files/Java/jdk-26' -AssemblyJar 'dist/bellwake-presentation/3c8c94ba7ce/update/forge-adventure-test.jar' -PresentationProbe -StarterColor R -Width 720 -Height 1280
```

This uses matching checkout resources and an isolated disposable profile. It does
not play real matches, exercise physical controller input, or listen to hardware
audio. Full match, balance, listening and controller gates remain manual.
Manual acceptance remains pending. Use [Bellwake manual playtests](Bellwake-Manual-Playtests.md)
for exact prerequisites, four numbered runs, save checkpoints, presentation/input
checks, expected results and report fields. Real match balance, losses/recovery,
controller focus, subjective art/audio quality and measured pacing require those
runs; supplied victories and WAV decoding do not establish them.

## Bellwake 0.2.0 candidate, October 31, 2026

Implementation is on `codex/bellwake-chapter-polish`, based on
`22f5f1f8b7c3089978014b2e405f90377eceb184`. The snapshot model/version change
was committed as `6f650c675a255b0e8985f03d6d1ed15a5573c323`; integration evidence
below includes subsequent source and content changes. Final package identity belongs
in its `BUILD-INFO.json`.

Runtime: Windows 11 amd64, Oracle JDK 26 compiling with Java release 17, Maven
3.9.9, native GLFW/libGDX client. Tests use disposable profiles and never import
normal player saves. The focused gate passed 27 save/restoration/content cases
with zero failures or skips; the starter reward-copy regression additionally
failed before adjustment and passed afterward (4 content cases).

| Gate | Evidence/status |
| --- | --- |
| Interior reconstruction | Native quay/tollhouse saves restore pose, life without entry healing, currency, and encounter state; F5 preserves the overworld return position. |
| Restore failure | A valid payload referencing a missing map in another campaign fails after activation and restores the outgoing interior, run and Continue selection. |
| Connected rooms | Disposable map fixture checks return exits, per-visit defeated ordinary enemies, respawn on a later visit, anonymous interactive IDs, two signed shops, rotating identity, opened stock, and the saved random stream. |
| Story interactions | Assisted real dialogue/reward actions cover first/repeat practice rewards, ticket acquisition, both ending branches, no partial ending checkpoints, reload/revisit without duplicate cards, and the persistent completed journal. Victories are supplied. |
| Fresh process | Second application process loads Continue and checks saved position, currency, life, deck, ending, reward count, and absent Keeper. The actual StartScene Continue action passes, including clearing its loading overlay. |
| Write failure / async isolation | Repository fixtures inject interrupted writes and invalid payloads; retain previous snapshots/Continue; test out-of-order saves and retired-run publication. This is not a native full-disk/permissions UI test. |
| Presentation | 1280x720 and 1920x1080 quay screenshots inspected. Existing town/dungeon art remains; a stronger ferry/quay identity and ambient polish remain open. Portrait functional regression is recorded separately when run. |
| Real gameplay acceptance | Pending: played practice and Keeper matches with both starters; practice skipped; deliberate loss/rematch; physical controller focus; dialogue/card inspection readability; measured chapter duration and confusion. No unassisted finish or balance claim. |

Reproducible runner: [Windows test build](Windows-Test-Build.md). Initial full
assisted evidence is `dist/bellwake-smoke/20261031-150838-59f1a4/` (local logs,
screenshot and isolated profile). The runner requires separate first-process and
fresh-process PASS markers. Failed earlier attempts are retained separately;
they are not counted as passing acceptance. Final 1920x1080 evidence is
`dist/bellwake-smoke/20261031-151527-c454cf/`: both first-process PASS markers
and `BELLWAKE_FRESH_CONTINUE_PASS` passed after fixing the menu-during-action
and stale loading-overlay regressions.

### Automated integration gate

The full default reactor completed `mvn -U -B -ntp clean test` at
`2026-10-31T15:24:33-06:00`, exit 0, **BUILD SUCCESS** in 7 minutes 15 seconds.
All 12 modules succeeded. Surefire reported **1,415 cases, zero failures/errors,
and six existing desktop network-harness skips**. Adventure contributed **70
passing cases with no skips**. No modules were excluded. Log:
`.bellwake-full-reactor.log` (local).

The six skipped `NetworkPlayIntegrationTest` methods are `analyzeLog`,
`runComprehensiveDeltaSyncTest`, `runQuickDeltaSyncTest`,
`testConfigurableParallel`, `testConfigurableSequential`, and
`testUnifiedHarnessLocalMode`.

During the full run, a native probe reproduced the shared New Game+ API retaining
its source interior. The final fix clears that context before generation and uses
normal overworld entry for NG+. Its native regression went RED then GREEN and
reloads the original source checkpoint. Bellwake itself does not offer NG+.
The full reactor compiled before that small compatibility fix; the final affected
clean verify below rebuilds it. Other modules were unchanged. Complete inherited
campaign NG+ gameplay remains unverified.

### Windows assembly gate

The committed application source `6747fa4da56e340e8b6031f5000f994c536ac293`
completed the final affected build with **BUILD SUCCESS** at
`2026-10-31T15:38:21-06:00` in 12 minutes 53 seconds. All seven affected reactor
modules succeeded, including **87 passing cases and no skips** (70 Adventure,
14 GUI, 3 game cases). The Windows launcher and the
`2.0.16-GITWindowsTest-jar-with-dependencies.jar` were produced. Command:

```powershell
mvn -U -B -ntp -pl forge-gui-mobile-dev -am clean verify dependency:build-classpath `
  "-Drevision=2.0.16-GITWindowsTest" "-Dmdep.outputFile=target/smoke-classpath.txt"
```

The session also supplied a workspace Maven dependency cache. This rebuild
includes the final New Game+ context fix. The later packaging/test-only commit
`df6180c96bfb09a565bd6fea01b5f3d808e96016` adds an assembled-JAR smoke option,
step diagnostics, a bounded ten-minute comprehensive test budget, and faster
byte-only Windows copying. The application source is identical to the assembly
commit. Metadata-preserving copying of 20 card scripts took 0.367 seconds;
content-only copying took 0.017 seconds. All package content hashes and archive
integrity checks remain enabled.

Earlier portrait attempts reached the connected-room checks and then the first
red-starter ending, but exceeded three-/five-minute global test deadlines while
asset loading and packaging competed for I/O. They remain failed attempts, not
passing portrait acceptance. The final assembled-JAR run is recorded separately.

### Assembled-JAR native gate

`tools/adventure/run_bellwake_smoke.ps1 -JavaHome "C:/Program Files/Java/jdk-26"`
with `-AssemblyJar forge-gui-mobile-dev/target/forge-gui-mobile-dev-2.0.16-GITWindowsTest-jar-with-dependencies.jar`
and `-Width 720 -Height 1280 -StarterColor R` passed both full first-process
markers and `BELLWAKE_FRESH_CONTINUE_PASS`, exit 0. Evidence:
`dist/bellwake-smoke/20261031-155452-fb272e/`. This uses the merged distribution
JAR, matching checkout resources, and an isolated profile. It exercises Ember
Crew, both copies of the Lightning Strike preparation reward, both endings,
repeat interactions, connected-room/shop restoration, and the actual Continue
menu action. Expected life was deliberately reduced to **17** before exit;
Continue retained **17**, so town healing was not replayed. The source-context
New Game+ probe and reloading its source checkpoint passed as well.

The 720x1280 quay screenshot was inspected. The short landscape map leaves
unused vertical space in portrait. This functional pass is not approval of the
portrait presentation; Windows landscape remains the first acceptance target.
Wins were supplied. No actual match, balance, physical-controller or unassisted
chapter acceptance is inferred from these passes.

### Local Windows candidate

Final archive:
`dist/adventure-windows/ready/Forge-Adventure-Windows-Test-6747fa4da56e.zip`
(**476,490,133 bytes**, approximately 454 MiB). Packaging exited 0 after JAR
validation, required class/resource/native-library checks, full content checksums,
and ZIP CRC validation. SHA-256:

```text
c417bc4d7b61b6f57ddbf56da8aaca8646a35cf224d3b07c80fd2da93551f75f
```

`BUILD-INFO.json` records application source `6747fa4da56e340e8b6031f5000f994c536ac293`,
Bellwake content `0.2.0`, Windows x64, the copied Oracle JDK 26, and the final
available Surefire totals (1,415 cases, zero failures/errors, six existing skips).
The JAR SHA-256 is
`d63e21139095faec06d4f12c1083c3bf27f166a2bebff526ccdb230e0ca33236`.
The packaged test profile remains fresh; native fixtures use their own disposable
profiles. Earlier incomplete package directories were retained separately and
are not deliverables; use the `ready/` archive above.

A further process using **the copied runtime executable and packaged JAR**
passed the actual Continue menu action and all fresh-load assertions, exit 0
(`.bellwake-bundled-runtime.log`). It used the previously verified disposable
Ember Crew profile and matching checkout resources, preserving the deliverable's
fresh profile and content hashes. This is a packaged-binary/runtime check, not a
played match or approval of every launcher/input/presentation path.

The candidate is ready for Windows player testing. Remaining acceptance is the
actual practice/Keeper matches with both starters, practice skipped, deliberate
loss/rematch, physical controller focus, dialogue/card readability, measured
chapter duration/confusion, stronger quay art and ambient treatment, and the
portrait space issue above. Bellwake 0.1.0 runs are not migrated. No completed
polished-release acceptance or unassisted finish is claimed.

## Build and save identity

The foundation baseline is upstream commit `4fcb76673693cb21fd0e31e50b72774fc02053a1`, whose parent POM specifies `2.0.16-SNAPSHOT` and Java release 17. The working branch is `adventure/progression-foundation`. These identify the starting point, not a tested finished build.

For every validation run, record:

| Field | Value to record |
| --- | --- |
| Tested code | Exact commit SHA; whether uncommitted changes were included |
| Packaged build | In-game version string, artifact name, and resource version/source |
| Platform | OS, architecture, Java version, Maven version, and launch method |
| Adventure configuration | Plane, difficulty, starting mode/deck, and world seed or identifying save |
| Save provenance | Fresh save, baseline save, previously affected save, or save from another version |
| Comparison | Baseline result, candidate result, and relevant logs/screenshots |
| Test isolation | Copy of the save/profile used; original retained without overwriting |

Use the Adventure application. Desktop development for its libGDX client is provided by `forge-gui-mobile-dev`; `forge-gui-desktop` is the separate Swing client. Repository build requirements and the general snapshot command are documented in [CONTRIBUTING.md](../../CONTRIBUTING.md). The repository's test-build workflow runs `mvn -U -B clean test` with a virtual framebuffer. Record the command actually used and its result rather than inferring that a broader workflow passed.

## Verification record: earlier progression patch

| Evidence category | Initial record |
| --- | --- |
| Source and issue review | Baseline review completed; see the roadmap and linked upstream reports. |
| Candidate compilation and automated checks | Full default Maven reactor: all 12 modules succeeded. Surefire reported 1,363 cases, zero failures/errors, and 6 skipped existing desktop cases. Adventure module: all 18 new TestNG cases passed with zero skips (10 progression cases and 8 content cases). |
| Changed resource parsing | All six changed quest definitions and both changed Library maps passed libGDX parsing and the focused content checks. The map cases cover 144 combinations across difficulty, story progress, research, and recorded defeat. |
| Regression discrimination | The same eight content tests failed against isolated copies of the baseline assets for the original missing-prerequisite and missing/overlapping-dialog defects. |
| Serialized quest compatibility | A quest serialized by the baseline compiled classes loaded in the candidate classes with metadata, stage state, flag names/thresholds, and displayed epilogue preserved. Serialization identifiers for `AdventureQuestData`, `AdventureQuestStage`, and `AdventureQuestController` matched the baseline. |
| Manual Adventure playthrough | Not performed for the original progression patch. The later campaign UI smoke below does not complete its long-form progression cases. |
| Existing-save recovery | Not established until the relevant cases below pass. |

Update this table when evidence is available, including any environmental blocker. Do not replace a failed or unrun case with an assumed pass because an adjacent test succeeded.

### Automated gate, October 28, 2026

The candidate working tree based on `4fcb76673693cb21fd0e31e50b72774fc02053a1` completed the full default reactor at `2026-10-28T17:55:58-07:00` (`2026-10-29T00:55:58Z`). The command exited 0 with `BUILD SUCCESS` in 3 minutes 25 seconds. Runtime: Linux x86_64, OpenJDK `17.0.20.1+1`, Maven `3.9.9`, and Xvfb display `:97` at 1280 by 1024. The runner supplied proxy settings and a local dependency cache; no modules or tests were excluded.

Exact command, from the repository root (line breaks added):

```sh
DISPLAY=:97 \
JAVA_HOME=/workspace/scratch/e50234835600/build-tools/xvfb/root/usr/lib/jvm/java-17-openjdk-amd64 \
LD_LIBRARY_PATH=/workspace/scratch/e50234835600/build-tools/xvfb/root/usr/lib/x86_64-linux-gnu \
/workspace/scratch/e50234835600/build-tools/apache-maven-3.9.9/bin/mvn \
  -s /workspace/scratch/e50234835600/build-tools/maven-settings.xml \
  -Dmaven.repo.local=/workspace/scratch/e50234835600/build-tools/m2 \
  -U -B -ntp clean test
```

An earlier full run stopped because the environment lacked a display for existing Swing tests. Supplying Xvfb and the headful Java runtime resolved that environment failure. This successful gate covers compilation and automated tests, including the new Adventure checks; it does not constitute packaging, an iOS profile build, or an interactive Adventure playthrough.

## Campaign and save gate, October 29, 2026

The implementation was built from the working tree based on `b376401100fac7d664473b9742924b27e8d5e48e`, then committed as [`8435941c0c41e16987d3dcc46f16ba13216bf620`](https://github.com/mountogdengc/forge/commit/8435941c0c41e16987d3dcc46f16ba13216bf620). Its source/test/resource tree is `af3d5060774a572510273b6e6d9718645d64650d`; documentation was added afterward. Runtime: Linux x86_64, OpenJDK `17.0.20.1+1`, Maven `3.9.9`, and Xvfb `:97`.

The full default reactor completed `-U -B -ntp clean test` with **BUILD SUCCESS** in 4 minutes 31 seconds. All 12 modules succeeded. Surefire reported **1,385 cases, zero failures/errors, and six skipped existing desktop cases**. Adventure contributed 40 passing cases: the earlier 18 progression/content cases plus 11 campaign and 11 persistence cases. No reactor modules or tests were excluded.

After small UI layout/copy and New Game+ controller-reset corrections, `-B -ntp -pl forge-gui-mobile -am test` passed all 57 cases in the affected reactor, including all 40 Adventure cases, with zero failures/errors/skips. The final initial-save location label and wrapped-error dialog width were subsequently compiled with `-B -ntp -pl forge-gui-mobile -am compile` and checked in the live game. Initial snapshots now say `Campaign opening`, since the world-stage sprite has not entered the world when the first checkpoint is captured. Wrapped error messages receive an explicit width so the dialog does not collapse around its OK button.

The focused suites use temporary filesystem fixtures. Campaign checks cover stable explicit IDs, exact version lookup, duplicate IDs, descriptor validation, opening quest declarations, explicit common world/quest inheritance, legacy adapters, developer filtering, preview isolation, and repeated A–B–A catalog selection. Persistence checks cover two campaigns and three same-named runs, independent checkpoints, repository restart/Continue, captured asynchronous destinations, request/commit/callback ordering, interrupted writes, failed validation, corruption, identity/version/path rejection, retention, manual replacement, and unchanged legacy-copy sources. These tests do not restore a historical player-save fixture through graphics.

The interactive run uses `forge.app.Main` from the desktop libGDX backend with repository resources, an isolated profile, and copied compiled classes. The development UI reports `v.GIT`; no packaged installer was produced. No existing player profile was used. Running the game alongside the full test suite exceeded the environment's 8 GB memory limit and the operating system killed the test game; Maven completed successfully. The initial checkpoint remained intact. Restarting the game separately with a 2 GB heap allowed the smoke checks to continue. This environment failure is not counted as a product crash or an intentionally injected save-write failure.

### Interactive fixtures and results

| Playthrough | Campaign | Playthrough ID |
| --- | --- | --- |
| Shandalar Alpha / character Alpha | Shandalar | `eb5b99ca-f8c0-4132-bd20-66b50122418b` |
| Shandalar Beta / character Beta | Shandalar | `969cd25e-7888-451c-90e3-c3d672a923ac` |
| Old Border Gamma / character Gamma | Shandalar Old Border | `fcb1e7a5-8aa4-4f19-9147-36b46ca9e110` |

These are fresh, generated test characters. The third run is **Shandalar Old Border**, not Realm of Legends. The two Shandalar runs share campaign ID `legacy.6e73b6e5-0387-3fba-83cf-3d3e41f8b75d`; Gamma uses `legacy.22ce7be7-a503-3517-aa18-580ace007c76`. Both adapters use content version `legacy-1`.

| Case | Evidence and result |
| --- | --- |
| C01: picker and preview | Passed for the installed legacy campaigns. Campaign selection precedes character creation; cancelling an initial preview created no run. Selecting another campaign and cancelling while Alpha was active returned to Alpha. The final picker layout was visually checked without clipping. |
| C02: independent runs | Alpha and Beta coexist as separate Shandalar playthroughs; Gamma adds a second campaign. Identical run/character names are covered by the repository tests, rather than these differently named live fixtures. New Game+ creation was not played through. |
| C03: switching | Loaded Gamma, Alpha, and Beta through the browser and restored their distinct characters/maps. Loaded Gamma again after Beta. No global World setting change or application restart was needed for those switches. |
| C04: checkpoints | Named manual checkpoints and the run/snapshot browser were exercised in the live game. Alpha's quicksave/quickload path was exercised. Automated cases establish manual/auto/quick isolation in each of three runs and retention of more than ten manual saves. Every quick/auto path was not manually repeated in every run. |
| C05: restart/Continue | Passed. After a normal confirmed application exit and fresh launch, Continue restored Old Border Gamma's map, red-dragon avatar, 8 life, 0 shards, and 125 gold. Its locator remained snapshot `df24bdee-126c-46d2-815b-55e65c20dee1` in Gamma's run/campaign. |
| C06: delayed writes | Automated coverage for captured destinations and delayed older callbacks, including an older autosave finishing after a newer manual save. No timing-controlled background-write race was injected through the live UI. |
| C07: missing version | Passed with a disposable copy of a valid run whose campaign version was changed to an unavailable value. The run/snapshot stayed visible, the required version was explained, and Load was disabled. No real campaign directory was removed. |
| C08: failure handling | A second disposable copy with a corrupted payload was rejected for checksum mismatch. The repaired dialog displayed its complete error and usable OK button. Resume returned to Gamma with the same map/avatar/stats. SHA-256 comparisons found zero changes to the 24 original campaign-save files, including Continue, across both negative checks. Mid-restoration rollback and disk-full behavior were not injected in the live game; storage write/validation failures are covered by the repository tests. |
| C09: labels and IDs | Explicit campaign title/folder rename is automated. Storage tests establish names are not destinations. A run-renaming UI is not implemented. |
| C10: legacy import | Automated copy-preservation coverage only. A real historical binary player save has not been imported and restored through the live UI. |
| C11: versions | Exact campaign-version and unsupported metadata-format rejection are automated; the missing-version UI was exercised in C07. No content or format migration is implemented. |
| C12: invalid packages | Automated descriptor, missing world/quest catalog, opening quest, and duplicate-ID coverage. Exhaustive validation of every map/dialog/asset dependency is not claimed. |

The two disposable negative-case directories were removed after verification; all three original runs and Gamma's Continue locator remained. Representative local captures are `33-final-campaign-picker.png`, `49-three-playthroughs.png`, `51-alpha-restored-from-gamma.png`, `52-beta-restored.png`, `62-gamma-continued.png`, `63-missing-version.png`, `69-corrupt-rejection-readable.png`, and `70-final-gamma-resume.png` in the runner's `build-tools/ui-evidence/` directory. Those local screenshots are supplemental evidence, not bundled product resources.

The live checks cover Linux landscape mode, the opening, menus, and save transitions. They do not establish a complete campaign playthrough, a real legacy-player-save migration, New Game+ gameplay, portrait/mobile behavior, or macOS/iOS/Windows compatibility. Long-form progression cases F01–F12 remain separate and unverified unless explicitly recorded.

## Foundation acceptance matrix

Every row starts **Not run**. For each executed case, record the tested build, plane/save, observed behavior, and result. Run affected resource cases in each changed plane or override; do not assume a common-map pass covers a plane-specific copy.

Content coverage for this change series: Drunken Plea in Shandalar, Shandalar Old Border, Realm of Legends, Crystal Kingdoms, Innistrad, and the Shandalar Spanish override; Library of Varsil in the common and Old Border maps, across the edited difficulty variants. Check that only the intended story or fallback defeat dialog runs, particularly in Old Border.

| ID | Scenario | Expected result | Initial status |
| --- | --- | --- | --- |
| F01 | Complete the captive-rescue quest in its ordinary order. | Each eligible stage completes once; the return/report stage waits for its prerequisites. Story dialogue and rewards remain reachable. | Not run |
| F02 | Complete the relevant castle action before accepting the quest, then accept it and reach the corresponding stage. Include the reported green-castle case. | A real saved rescue/completion fact satisfies the stage when it becomes eligible. No second boss kill or impossible interaction is required. | Not run |
| F03 | Complete castle objectives in a different order, including more than one fact recorded before their stages activate. | Newly eligible stages reconcile their saved facts, including a chain of already-satisfied stages, without completing unrelated objectives. | Not run |
| F04 | Activate a flag objective when its flag is absent, below the required threshold, or belongs to another objective. | The stage remains incomplete. An absent flag does not become a completed action through a default value. | Not run |
| F05 | Load a baseline save with an active objective whose required saved quest flag is already set. Inspect its state immediately after loading, then follow the normal quest dialogue path. | The eligible objective reconciles during loading without editing the player's save. Dialogue and rewards remain available through their normal path and occur once. Record any further interaction required. | Not run |
| F06 | Save and reload before objective activation, after its completion, and before final quest turn-in. | Progress survives each boundary. Completed stages, completion dialogue, and one-time rewards do not repeat. | Not run |
| F07 | Defeat the Library of Varsil boss before the related story stage, while that stage is active, and after it. | Each legitimate victory records defeat and removes the boss. Relevant dialogue remains appropriate; the player can move on rather than immediately refighting the same boss. | Not run |
| F08 | Re-enter and reload a cleared Library boss room; repeat the procedure for any other boss whose defeat dialog changes. | The boss remains defeated and the encounter does not grant another one-time reward merely from re-entry or load. Intended repeatable encounters remain repeatable. | Not run |
| F09 | Accept a newly generated Drunken Plea quest, clear the target location, and return to the specified town. | Clearing the location activates the return objective. Completion text and quest rewards occur only on the required return. | Not run |
| F10 | Revisit the Drunken Plea quest-giver's town before clearing the target; save/load between steps. | Visiting early does not satisfy the later return objective. The required sequence survives reload. | Not run |
| F11 | Load a save containing a Drunken Plea instance created before the data correction. | Record whether the serialized instance retains its old prerequisite data. If so, report the limitation explicitly; do not infer migration from a new-quest pass. | Not run |
| F12 | Open the quest list during an ordinary campaign and with multiple active stages, including recovered objectives. | The list and tracking controls work. This is a smoke check, not proof that the unidentified cause of #12296 is fixed. | Not run |

For cases F02, F03, and F05, first verify the relevant action was actually recorded in that save. Possessing a key or having a deleted map object is not automatically equivalent to every captive-rescue flag. If the required fact is missing, document that distinct recovery problem rather than granting unearned progress.

## Targeted follow-up checks

| ID | Issue or risk | Check and decision |
| --- | --- | --- |
| R01 | Flower Fetching, [#9141](https://github.com/Card-Forge/forge/issues/9141) | Collect the flowers before accepting the quest, then accept it and revisit the interior cave. Repeat with a save/load boundary and a baseline save. Verify that an already-persisted `hasAFlower` flag in the selected cave's saved root/floor records advances an eligible stage without collecting another flower. Separately determine whether the existing recursive object reset restores the required interaction when needed. If either path fails, capture the map IDs, deleted-object state, quest flags, and exact build before changing reset logic. |
| R02 | Quest-list crash, [#12296](https://github.com/Card-Forge/forge/issues/12296) | Obtain a reproducing save and exception trace. Compare the same save on baseline and candidate builds; identify the failing quest or UI path. Generic UI smoke tests do not close this issue. |
| R03 | Existing hunt repair, [#5440](https://github.com/Card-Forge/forge/issues/5440) / [#7516](https://github.com/Card-Forge/forge/pull/7516) | Accept On the Hunt, leave town without moving, save/quit, and reload. Track the quest and move. Confirm the target and navigation recover; also check an untracked quest after tracking it. The baseline already contains the repair. |
| R04 | Existing dialog repairs, [#11670](https://github.com/Card-Forge/forge/issues/11670) / [#12128](https://github.com/Card-Forge/forge/pull/12128) | Open the Demon Tower portal dialog and complete the Skep reward interaction. Confirm readable choices, a reachable exit from the dialog, and the intended reward. Do not count strict-parser warnings elsewhere as runtime failures without checking Forge's parser. |
| R05 | New Game+ and intended resets | When a change affects completion/defeat flags or reset paths, verify a New Game+ run can obtain and complete the relevant quests again, while a normal reload retains completion. Record any differences by plane. |
| R06 | Candidate one-time quest support, [#12260](https://github.com/Card-Forge/forge/pull/12260) | Before adopting the PR, test competing offers from two towns, acceptance after a boss was defeated early, abandonment/retry, completion/re-offer, saved navigation, existing duplicates, and New Game+. Check its explicitly excluded old-save cases before making a compatibility claim. |

## Evidence and release decision

Record a short result for each executed case: steps, save/build identity, expected versus actual outcome, and the evidence location. Prefer a small set of meaningful regression tests for the changed state transitions, supported by actual Adventure playthroughs for dialogs, map navigation, and rewards.

A foundation build is ready for a campaign trial when its changed paths compile and parse successfully, the affected foundation cases pass on the tested configuration, and remaining limitations are stated. Readiness for a fresh save does not establish readiness for an existing campaign. Keep unresolved crash reports, untested platforms, and old-save recovery gaps visible in the decision.
