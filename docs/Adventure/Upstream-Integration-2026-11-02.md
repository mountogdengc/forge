# Upstream engine integration — November 2, 2026

## Result and revisions

Latest fetched upstream `Card-Forge/forge` master: `0a8c52d00cb1d8822fb5bb36a08fede4f79ed75f`. All 75 upstream-only commits were integrated into local branch `codex/upstream-engine-update-20261102` in the managed checkout:

`C:/Users/david/.codex/worktrees/forge-upstream-engine-update/forge`

- Original branch and HEAD: `codex/bellwake-chapter-polish`, `34876cb2970e8197bf3a359be2922c5b02474718`.
- Preservation commit: `9641022c0fac32fb4dd0d8440c03d0d68357c1a3`, capturing all 103 changed/new local paths.
- Edition regression: `dfad36f6cb39fe8f3c0d666ea81e4aa62e4a3edb`.
- Upstream merge: `271605b8511707038eb7382789b3f1a34273abf3`.

The original checkout at `F:/Projects-PCGames/forge` retains its branch, HEAD, full porcelain status, tracked diff, staged diff, and all 103 original file SHA-256 hashes. All 103 copied files also retain their exact original bytes in the integrated checkout. The preservation commit stores the same contents with Git's normal line-ending normalization. No branch was pushed or merged into the original working checkout.

## Resolution and compatibility

There was one text conflict, in `forge-gui/src/main/java/forge/gui/control/GameEventForwarder.java`. The resolution retains the fork's `GameEventAddLog` import and private postgame-log filter, and accepts upstream removal of the obsolete zone-change helper/import. The resulting difference from upstream is just the private-log import/filter.

The six other overlapping files merge automatically: `GameAction`, `Player`, `RewardData`, `AdventurePlayer`, `CardUtil`, and `FModel`. Review confirmed upstream AI-list initialization and the imported AI code/resources/tests are retained, while deferred card loading, campaign-specific pool invalidation, independent runs, and save restoration remain in place.

A new focused regression checks fixed and Union rewards in both unique-printing and all-variants modes, explicit editions, embedded-edition precedence, and absence of unrelated eager card loads. It failed before the merge with **expected AAA, found BBB** and passes with the upstream fix. Independent review found no Critical or Important integration issues; its test-strengthening suggestion was applied by asserting that the embedded-edition result contains two rewards.

This integration demonstrates a working path for current engine updates. The fork's shared card-catalog/loading and match-log changes remain the principal areas to review on future updates. Campaign/run envelopes and interior snapshots remain fork extensions: supported legacy import is covered, but reverse compatibility of new fork saves with upstream is not promised.

## Verification

| Check | Result |
| --- | --- |
| Pre-merge focused baseline | 146 passed, zero failures/errors/skips; all nine selected reactor modules succeeded |
| Full default clean Maven test reactor | All 12 modules succeeded; 1,826 reported cases: 1,820 passed, zero failures/errors, six optional network skips |
| Adventure client/editor package and final affected checks | Eight reactor modules succeeded; 175 passed, zero failures/errors/skips |
| Adventure Python tooling | 20 passed |
| Baseline coverage retained | All 146 selected baseline cases are represented in the full merged run |
| Built-client Bellwake runtime | Passed interior save/load, failed cross-campaign rollback, assisted endings/revisit/reload, and fresh-process Continue |
| Built-client actual match opening | Passed deferred catalog assertions, named-draw privacy, three Help dialogs, and penalty-free practice loss |
| Built-client Strahd runtime | Passed both fates, late briefing, early camp, friendly loss, reward-once behavior, checkpoints/campaign isolation, fresh-process Continue and completed-actor reconciliation |
| Original source preservation | Branch/HEAD/status/diffs and all 103 original hashes match; integrated copies match exact bytes |

The six skipped methods belong to the existing optional `forge.net.NetworkPlayIntegrationTest`: `analyzeLog`, `runComprehensiveDeltaSyncTest`, `runQuickDeltaSyncTest`, `testConfigurableParallel`, `testConfigurableSequential`, and `testUnifiedHarnessLocalMode`. These skips do not establish network interoperability.

Builds and native checks used JDK 26 with bounded memory and two reported processors. The sampled compiled core, engine, AI, GUI and desktop-client classes are Java 17 class-file version 61. Execution on an actual Java 17 runtime, iOS-native packaging, Android device execution, audible audio, physical input responsiveness, unassisted chapter completion and balance were not assessed. Hidden native checks disable audio and supply story victories; they are compatibility probes rather than human gameplay acceptance.

The first baseline attempt failed with a Windows directory-rename `AccessDeniedException` in `CampaignRegistryTest.explicitIdentitySurvivesFolderAndTitleRename`. No upstream code had been applied. The same source passed on repeat with temporary files outside the managed checkout. The exact cause is unproven; the failed attempt and reports remain retained. No test was removed or disabled. An upstream-only extra blank line at EOF in `FlatMonteCarloComboTest.java` was retained rather than adding unrelated formatting cleanup.

## Evidence and reproduction

Evidence inside the isolated checkout: `.superpowers/upstream-engine-update/` contains build scripts/logs, baseline/full report copies and JSON totals, source-preservation checks, independent-review notes and three native test profiles/logs. Original byte backup and manifest: `F:/Projects-PCGames/forge/.build-cache/upstream-integration-20261102/`.

Shared-queue jobs under `F:/Tools/CodexTestRunner/state/results/forge/`:

- Initial baseline failure: `ee13b86cb3b448a886699f49898035ec`.
- Passing baseline: `90e4d1675ab7437eb920e0f6701f8043`.
- Expected pre-merge regression failure: `4c69afd214334eecbb9171c72f29fed7`.
- Full merged suite: `355252a7e9944f2aaada05ce75220789`.
- Python checks: `498312ea360649559b66af48200c97b4`.
- Final package: `09b63aacbfca4e2b85b3c390e411ff2e`.

From the isolated checkout, the retained `.superpowers/upstream-engine-update/build.ps1` accepts `baseline`, `merged`, and `package`; invoke through the shared runner's `exec --project forge` command. `python-checks.ps1` runs all Adventure Python tests. `native-checks.ps1` directly invokes the existing hidden, bounded, isolated-profile native smoke harness after noninteractive build work has finished; it does not control the host desktop. Native output directories must be new for a rerun, preserving previous profiles/logs.

Built client: `forge-gui-mobile-dev/target/forge-gui-mobile-dev-2.0.16-GITUpstreamIntegration-jar-with-dependencies.jar`. This development artifact uses resources from the isolated checkout; no portable release ZIP or publication was produced.

Client SHA-256: `d001164328bae6b3e6b9b3c21155502979056d4ace5ab87d5d3537dd9907af31`; bytes: 57642267.
