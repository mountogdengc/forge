# Adventure progression validation

Use this checklist with the [fork roadmap](Fork-Roadmap.md). It distinguishes a code/data review, an automated check, and an actual game run. Record evidence for each result; an unchecked case remains unverified.

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

## Verification record

| Evidence category | Initial record |
| --- | --- |
| Source and issue review | Baseline review completed; see the roadmap and linked upstream reports. |
| Candidate compilation and automated checks | Full default Maven reactor: all 12 modules succeeded. Surefire reported 1,363 cases, zero failures/errors, and 6 skipped existing desktop cases. Adventure module: all 18 new TestNG cases passed with zero skips (10 progression cases and 8 content cases). |
| Changed resource parsing | All six changed quest definitions and both changed Library maps passed libGDX parsing and the focused content checks. The map cases cover 144 combinations across difficulty, story progress, research, and recorded defeat. |
| Regression discrimination | The same eight content tests failed against isolated copies of the baseline assets for the original missing-prerequisite and missing/overlapping-dialog defects. |
| Serialized quest compatibility | A quest serialized by the baseline compiled classes loaded in the candidate classes with metadata, stage state, flag names/thresholds, and displayed epilogue preserved. Serialization identifiers for `AdventureQuestData`, `AdventureQuestStage`, and `AdventureQuestController` matched the baseline. |
| Manual Adventure playthrough | Not performed for this candidate. |
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
