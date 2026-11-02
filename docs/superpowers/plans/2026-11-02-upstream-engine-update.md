# Upstream engine integration, November 2, 2026

The user authorized integrating latest upstream in an isolated checkout while preserving all local work. Engine updates remain the priority; upstream contribution readiness is not required.

## Global constraints
- Keep F:/Projects-PCGames/forge and its branch, index, and all local source bytes unchanged.
- Preserve the 103 changed/new paths in a local snapshot commit on codex/upstream-engine-update-20261102.
- Integrate the fetched upstream/master revision with a real merge; retain fork campaign/save behavior and private match-log filtering.
- No pushes, publishing, gameplay additions, or replacement of the original checkout.
- Use existing meaningful regression suites. Add a test only for an uncovered integration failure.
- Run capped noninteractive builds through the shared runner, with the actual isolated checkout as working directory. Keep logs and failures.

## Task 1: Preserve work and establish baseline
Copy and hash-verify all local changed/new source files, then create a local preservation commit. Run the existing focused catalog/loading, match-event/privacy, progression, campaign, save/restoration, Strahd-content and editor checks against this snapshot. Expected: compilation and selected checks pass, or a precisely documented pre-existing failure.

## Task 2: Merge upstream
Record exact upstream SHA. Merge upstream/master, resolve GameEventForwarder imports by retaining GameEventAddLog and accepting upstream removal of the unused zone helper/import. Inspect all seven overlapping files. Expected: no conflict markers, private filter retained, upstream reward-edition and AI initialization changes retained.

## Task 3: Verify integration
Run full default Maven clean test reactor; run Python Adventure tooling tests. Build the Adventure desktop client/editor and produce fresh classpaths. Exercise hidden, isolated-profile Bellwake and Strahd runtime save/Continue and match-help/practice-loss probes if the native runtime is available; these are assisted checks, not human gameplay acceptance. Compare selected regression coverage with baseline. Repair actual integration failures, with discriminating regression evidence.

## Task 4: Review and handoff
Review the final integration delta, record exact revisions, commands, totals, skips and limits in docs/Adventure/Upstream-Integration-2026-11-02.md. Verify the original source manifest, branch, HEAD and staging are unchanged. Keep the verified local integration branch/worktree for the user; do not merge it into the original checkout or push it.

## Review focus
- Preserve private log filtering while accepting upstream event/prompt synchronization changes.
- Explicit-edition named rewards and selected acquisition pools remain correct under deferred loading.
- Upstream AI configuration is retained; engine/AI tests cover imported fixes.
- Campaign versions, legacy imports, independent runs, rollback and interior restoration remain covered.
- All original uncommitted work is present in the isolated preservation commit and unchanged at its source.
