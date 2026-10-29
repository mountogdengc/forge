# Adventure progression validation

Use this checklist with the [fork roadmap](Fork-Roadmap.md). It distinguishes a code/data review, an automated check, and an actual game run. Record evidence for each result; an unchecked case remains unverified.

This checklist validates the current progression patch and the inherited content it changes. It does not require future campaigns to retain that content. A replacement campaign or Adventure system needs acceptance cases for its chosen design and an explicit policy for existing saves. Apply these regressions where their systems or content are retained, and keep unrun legacy cases marked unverified; retiring content does not turn an unrun case into a pass.

The campaign picker and independent-playthrough architecture are implemented as described in [Campaigns and saves](Campaigns-and-Saves.md). The campaign gate below records their automated and interactive coverage separately from the earlier progression fixes. Partial coverage does not turn an entire acceptance case into a pass.

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
