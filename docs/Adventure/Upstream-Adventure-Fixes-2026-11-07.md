# Upstream Adventure fixes — first batch, November 7, 2026

## Scope and integration status

Local branch: `codex/upstream-adventure-fixes-20261107`, based on fork master `6f5ceb18f17c2f15f6d227e90a175b0e70c287c3`.

The reviewed upstream range contains 548 commits, from shared baseline `0a8c52d00cb1d8822fb5bb36a08fede4f79ed75f` through `7acb3dbb61baac5ece0f609a9507804909cb9f97`. This batch selectively ports useful changes; it does not merge that entire history. An ancestry-based "behind" count will therefore still include the original upstream commits even after their changes are adapted here.

The user's instruction to start working through the review authorizes this first integration batch. Existing fork menus, themes, navigation, campaign content, independent playthrough saves, character identity/imported portraits, card-pool policy, deferred loading and private battle logs remain the preservation requirements. No changes to campaign deck lists, gameplay content, Java requirements, GitHub branches or frozen playtest distributions are part of this batch. Implementation and adaptations were assisted by OpenAI Codex.

## Imported changes and adaptations

| Upstream source | What is included | Fork adaptation |
| --- | --- | --- |
| [7acb3dbb61 / #12745](https://github.com/Card-Forge/forge/pull/12745) | Capture previews immediately on the graphics thread; forced quicksave previews bypass cooldown. | Remove the obsolete `firstCapture` reset from `beginPlaythrough`; reset its cooldown instead. Queued captures retain both header and playthrough activation identity. A request sequence rejects callbacks superseded by a newer forced preview. |
| [6264f0292f / #12595](https://github.com/Card-Forge/forge/pull/12595) | Print sheets reject null/missing/unsupported card scripts through weighted insertion, bulk insertion and construction. | Constructor owns a filtered copy; supported weights and the caller's pool are preserved. |
| [1f8a571cab / #12687](https://github.com/Card-Forge/forge/pull/12687) | Inventory description bounds, wrapping, scroll hint, explicit focus and null-safe booster access. | Applied onto the existing themed scene, retaining fork controls and appearance behavior. |
| [5d892f0559 / #12678](https://github.com/Card-Forge/forge/pull/12678) | Notifications draw above the minimap; portrait width uses the available HUD area. | Applied onto the fork's existing navigation/HUD additions. |
| [8360afa305 / #12730](https://github.com/Card-Forge/forge/pull/12730), partial | Enemy-card filtering is materialized once instead of rerunning for each template scan. | Publish immutable player/enemy pools together, with the fork's Config identity and scoped Commander-mode keys. Reward generation retains one complete snapshot across invalidation. Existing AI eligibility remains unchanged. |

The rest of #12730 is deliberately deferred: its AI-list optimization depends on the subsequent AI bundle, and its background worker reads mutable live campaign/player/world state. That worker requires captured generation context and cancellation coverage before it can safely coexist with campaign switching and character previews. Fight deck generation remains synchronous in this batch.

Native validation additionally found that the fork's themed label styling resets an empty label's explicit JSON width to zero. The resulting inventory description viewport was -5 pixels wide. UIActor now preserves declared TextraLabel width across presentation styling; labels without a declared width retain their automatic sizing. This is an integration adaptation for the fork's theme/LabelFix combination, covered by the failing native viewport check.

## Verification

Evidence is retained under `.build-cache/upstream-adventure-fixes-20261107/`. Heavy checks use the shared runner in the actual active checkout, with JDK 26, Java 17 source compatibility, a 1 GiB heap and two reported processors.

- Clean baseline: eight Adventure client/editor reactor modules succeeded; 286 reported cases, 285 passed, zero failures/errors and one existing Windows symbolic-link privilege skip.
- Regression proof: preview/booster assertions failed before the imports. The enemy-filter regression measured three redundant rule inspections across three scans before caching. A later ordering regression reproduced an old queued capture overwriting a newer forced capture.
- Focused final checks: 16 passed, zero failures or skips.
- Final clean `mvn.cmd -B -ntp -pl forge-gui-mobile-dev,adventure-editor -am clean verify -Drevision=2.0.16-GITAdventureFixes`, including the native-discovered width adaptation: eight modules succeeded; 297 reported cases, 296 passed, zero failures/errors and the same one skip (`ThemeResolverTest.symbolicLinkEscapeRejected`). Queue job: `2a9a75a19b2e46ce8b0917630511d6d8`. Per-module reports and final tested Java source hashes are retained in `final/results.json` and `final/reports/`; full build log is `layout-final/build.log`.
- Independent source review: no Critical or Important introduced regressions found. The reviewer examined related callers and lifecycle behavior, rather than only changed lines.

Six final native scenarios passed against the same freshly assembled production JAR (no production bytecode overlays): landscape character creation/new save; fresh-process Continue after source portrait deletion; portrait/inherited character UI; landscape and portrait preview/HUD/inventory checks; copied pre-art Barovia Continue and four animated map visits. The inventory viewport now measures 305x82 in landscape and 241x78 in portrait. The probes exercise item selection, scroll focus, hint cancellation, and scrolling, and assert that their windows remain invisible and unfocused. Original source-fixture preservation: all 174 files retained their SHA-256 hashes.

Native evidence: `native-layout-final/` and `native-remaining/`. Their production assembly SHA-256 agrees: `D06A7B70660E570E39FC70315D4B04D6514D01C41E3448A402D210D80A70693F`. Relevant queue jobs: `607a0e694cb3495a9c7d8611c470259b` and `7e9caf882c85455c86d9877f56947683`. Those composite jobs also retain failures described below; individual passing native markers are checked explicitly in the final aggregate, rather than treating a nonzero composite exit as success.

Authoring/content/package checks: 42 passed with `python -m unittest discover -s tools/adventure -p test_*.py`, Python 3.14.4, task-local Pillow 13.0.0 and zlib `1.3.1.zlib-ng` (queue `d971f6977420405bb5d0fadd9dc7e837`). `final/results.json` combines only inspected final evidence and includes tested Java source hashes, per-module counts, native markers, assembly hash, authoring count and fixture preservation.

The first native attempt found an obsolete fixture that reflected the removed `commanderCardPool` field. The fixture now checks that the actual generated preview pool excludes non-Commander-ineligible cards, without depending on the private cache representation. Failed evidence is retained.

The first copied-save portrait probe exceeded its 180-second startup allowance following the previously recorded FSkinFont null-font fallback. A read-only thread dump on retry showed the graphics thread waiting in generated font-cache PNG writes. With the same 300-second allowance used by the existing character probe, the portrait inventory/preview/HUD checks passed. This does not establish fast cold startup or correct CJK font generation; the inherited fallback and initial timed-out run remain visible limits. No font or startup production code was changed.

The default Python initially lacked Pillow. The bundled runtime then ran all 42 tests but its zlib 1.3.2 generated different compressed map bytes, failing the existing byte-exact idempotence assertion. Re-running with the original Python/zlib-ng environment and Pillow installed only under the task's ignored dependency folder passed all 42. No map, authoring assertion or system Python installation was changed to make the tests pass.

## Remaining work and limits

- Coherent AI/card-role/combo changes, their engine dependencies and difficulty/performance assessment.
- Safe background enemy generation, with captured campaign/player/RNG context and stale-result cancellation tests.
- New Star Trek/Alchemy implementations, script migration and associated edition/deck/booster data.
- Additional focused battle-rule fixes, trigger processing and undo-history changes.
- Separate decision about the Two-Headed Giant/shared-turn engine changes.

An upstream inventory limitation remains: its extra four pixels of bottom glyph padding are set through actor height, which ScrollPane later replaces with preferred height. This is incomplete mitigation of existing icon clipping, not a confirmed new regression. The current batch retains upstream's implementation; physical-input and subjective last-line readability remain separate acceptance concerns.

Automated hidden-window probes do not establish physical controller behavior, audio, mobile device compatibility, long-form campaign balance or a complete human playthrough. All native profiles are disposable copies; original player profiles, source fixtures and prior packaged builds must remain unchanged.
