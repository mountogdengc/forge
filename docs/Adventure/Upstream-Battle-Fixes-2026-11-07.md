# Upstream battle fixes — second batch, November 7, 2026

## Scope and integration status

This batch continues the local `codex/upstream-adventure-fixes-20261107` branch from `3a1fa7c7864feb43fb3ddaa23988148ba6f8666a`. It selectively adapts four changes from the pinned upstream review ending at `7acb3dbb61baac5ece0f609a9507804909cb9f97`. It does not merge the full 548-commit history, so ancestry-based behind counts do not measure which fixes have been ported.

The shared engine changes apply to both ordinary matches and Adventure battles. The fork's campaigns, menus, themes, character identity, separate playthrough saves, deferred loading, card-pool policy and private battle logs remain preservation requirements. No master merge, push, new card content, frozen playtest package update or Java requirement change is included. Implementation and adaptations were assisted by OpenAI Codex.

## Imported changes and fork adaptations

| Upstream source | Included behavior | Adaptation or boundary |
| --- | --- | --- |
| [409689c3464](https://github.com/Card-Forge/forge/commit/409689c3464a2672bd6868392fb02e182a4df703) | Adding a mana tax preserves the original waterbending limit. | An explicitly supplied incoming limit still takes precedence; ordinary costs remain ordinary. |
| [08b825d1e95](https://github.com/Card-Forge/forge/commit/08b825d1e95c01e6263bfb86bf3e5029f2fe26df) | Extrapolate-style revealed sideboard choices work without a destination; an opponent choosing a card to hand over favors the worse card. | Retains existing Learn/deck-plan behavior for choices with a destination. |
| [1e8c35fca3e](https://github.com/Card-Forge/forge/commit/1e8c35fca3e930e0c4f25dec5f3cebf067cc98e1) | Optional copies consider spare mana and spells affordable from hand; threat prediction uses the copy's targets and the actual resolving ability. | Manually adds resolving-ability tracking alongside the existing resolving-card lifecycle, without importing unrelated stack/end-turn changes. The new generic optional-copy acceptance excludes `DefinedTarget` copies, which need their own redirected-effect evaluation. |
| [45cd35c4af9](https://github.com/Card-Forge/forge/commit/45cd35c4af9fd49f6a7c1a7ef021b7a629118a80) | No-match Dig effects still show looked-at cards to their chooser; mobile multi-card selection reveals before choosing. Graveyard activation filtering recognizes Beam me up, Harmonize and current-turn discarded Mayhem; exile filtering recognizes plotted cards. | Paired engine/UI changes and removal of obsolete forced-reveal flags from 115 existing scripts. The absent `rutherford_upgraded_ensign.txt` and `spock_always_your_friend.txt` remain deferred to the new-card batch. Opponent ownership filtering is retained. |

Independent review found an upstream AI regression before integration: Ivy, Gleeful Spellthief could accept a free copy of the AI's own Murder and force that copy onto herself. The real Ivy script reproduces this failure. The narrow `DefinedTarget` guard restores the previous conservative behavior for forced-target copies while retaining harmless free subability copies and ordinary paid optional copies. A follow-up review found no further actionable issues in that correction or the mobile dialog test.

## Verification

Evidence is retained under `.build-cache/upstream-battle-fixes-20261107/`. Heavy checks run through the shared runner in this active checkout with JDK 26, Java 17 source compatibility, a 1 GiB heap and two reported processors.

- Baseline regression proof: the initial real-game fixture reported 10 failures out of 12 cases before importing production changes. Two cases were passing controls. Corrected setup errors are retained separately from the confirmed failing baseline.
- Expanded engine checks: 18 pass, covering waterbending boundaries, destination-free choices, optional-copy ownership/mana/instant-speed boundaries, unsupported and recursive copies, copied targets, resolving-ability lifecycle, graveyard/exile eligibility and chooser-only Dig privacy. The Ivy regression failed before its guard and passed afterward; the harmless free `DB$ CopySpellAbility` control passes.
- Mobile controller contract: the previous controller fails reveal-before-selection while its ordinary-choice control passes. Both pass with the imported change. The test invokes the actual controller through a substituted dialog service and verifies that all looked-at cards are presented but only eligible cards can be selected.
- Script audit: every one of the 115 modified resource scripts changes only by removing `ForceRevealToController$ True`; all other pipe-delimited script tokens are unchanged. Per-script hashes are recorded in `resource-audit.json`.
- Final clean `mvn.cmd -B -ntp -pl forge-gui-mobile-dev,adventure-editor -am clean verify -Drevision=2.0.16-GITBattleFixes`: all eight reactor modules succeeded; 317 reported cases, 316 passed, zero failures/errors and one existing Windows symbolic-link privilege skip (`ThemeResolverTest.symbolicLinkEscapeRejected`). All 18 new engine cases and both mobile cases are included in those totals. The engine fixture follows this module's `*Should.java` discovery convention; an evidence audit caught and corrected its initial exclusion from Maven, while the direct TestNG run had already passed it. Earlier evidence remains in separate directories.
- Authoring/content/package checks: 42 passed with the original Python/zlib environment and task-local Pillow, without modifying the system installation or relaxing byte-exact map checks.
- Final queue job: `b4aca71e1c244594940f51d253c82da4`. Exact per-module results, the known skip, tested Java source hashes and archived XML reports are in `final-registered/results.json` and `final-registered/reports/`; the complete build log and authoring log are alongside them. The aggregate asserts the expected test counts and discovery of both new fixtures.
- Fresh production assembly SHA-256: `ae470ea2c574de5649455e0fdcaa4bcd767d050561b2e513371894d77a60cba6` (`forge-gui-mobile-dev/target/forge-gui-mobile-dev-2.0.16-GITBattleFixes-jar-with-dependencies.jar`).

No new full graphical/native run is claimed for this batch. The mobile change is a dialog sequencing correction exercised at the actual controller boundary; layout/rendering code is unchanged. The preceding Adventure batch's six native scenarios remain evidence for that earlier assembly, not the newly built assembly. These checks do not establish physical input behavior, mobile device compatibility, long-form AI balance or a complete campaign playthrough. The optional-copy budgeting remains upstream's approximate mana/CMC heuristic.

## Next work

The next reviewed batch is the coherent AI/card-role/combo work and its dependencies. New cards/sets, safe background enemy generation, broader engine/trigger/undo changes and Two-Headed Giant/shared-turn changes remain separate work. Background generation still requires captured campaign/player/RNG context and stale-result cancellation coverage before it can safely coexist with campaign switching.
