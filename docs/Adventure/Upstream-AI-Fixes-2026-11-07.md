# Upstream AI fixes — third integration batch

## Scope

This batch continues `codex/upstream-adventure-fixes-20261107` from `4c01f0049f9b051dcc67f9bb5ca4ccc10066fb18`, selectively porting the coherent AI work in the reviewed upstream range ending at `7acb3dbb61baac5ece0f609a9507804909cb9f97`. Implementation and adaptations were assisted by OpenAI Codex.

The changes improve the computer opponent used by Adventure: preserving combo pieces, refusing unsafe Pact payments, named discard based on revealed cards, Standard/Vintage combo planning, draw/scry/surveil choices, flash interaction, mana/color choices, activation guards and planeswalker/X decisions. They also include the paired role/combo lists, shared card-hint hooks, two existing card-script hints and three existing AI profiles. No new cards/sets, campaign content, save format, background fight worker, sideboarding default, Two-Headed Giant feature or unrelated engine cleanup is imported. Master and GitHub are outside this local integration.

## Sources and adaptations

| Selected upstream changes | Included work |
| --- | --- |
| [7c34365c749](https://github.com/Card-Forge/forge/commit/7c34365c749), [a6fe7c40c5e](https://github.com/Card-Forge/forge/commit/a6fe7c40c5e) | Sacrifice/named-discard/Pact safeguards, dredge lines and Standard combo support; Cabal Therapy hint. |
| [af7df3488ea](https://github.com/Card-Forge/forge/commit/af7df3488ea), [45572700ec7](https://github.com/Card-Forge/forge/commit/45572700ec7) | Vintage combo plans, data-driven catalog, library-out/Oracle/reanimation lines, role-aware deck eligibility and Cautious/Reckless/Experimental profile settings. |
| [eb40bd24de1](https://github.com/Card-Forge/forge/commit/eb40bd24de1), [a04b54df781](https://github.com/Card-Forge/forge/commit/a04b54df781) | Timing discovery and additional Pact checks. |
| [79f6fc2ec91](https://github.com/Card-Forge/forge/commit/79f6fc2ec91), [fcfdc73aeff](https://github.com/Card-Forge/forge/commit/fcfdc73aeff), [6d4a175d9ef](https://github.com/Card-Forge/forge/commit/6d4a175d9ef) | Doomsday piles, Donate/Illusions and Channel lines; dedicate Channel mana to its intended payoff; Channel hint. |
| [1747b2c7472](https://github.com/Card-Forge/forge/commit/1747b2c7472), [d3f975437b8](https://github.com/Card-Forge/forge/commit/d3f975437b8), [cfc0752f7a5](https://github.com/Card-Forge/forge/commit/cfc0752f7a5) | Flash interaction valuation, hidden-origin discovery and deck-plan-aware library choices. |
| Only `AiCardLists` from [8360afa305b](https://github.com/Card-Forge/forge/commit/8360afa305b), plus [62ffaaedebc](https://github.com/Card-Forge/forge/commit/62ffaaedebc), [d47f567004e](https://github.com/Card-Forge/forge/commit/d47f567004e), [98d79231ccb](https://github.com/Card-Forge/forge/commit/98d79231ccb) | Cached role lookup/ASCII normalization, generic Any-mana color preferences, activation guards, loyalty and X choices. The fight worker remains deferred. |

All 16 source patches were checked in an ignored source mirror before production application. They applied without context conflicts. Compilation exposed one additional API dependency: ScryAi used a later turn helper. The fork retains its existing `getNextTurn().equals(ai)` expression, which has the same behavior here, instead of importing the helper's unrelated 42-file refactor.

Adventure's enemy-pool cache is adapted rather than replaced. It captures the Flat Monte Carlo preference as part of both the snapshot identity and supported-role eligibility calculation. Changing that preference refreshes enemy eligibility. Existing snapshots remain immutable; player pools, campaign edition/card restrictions, non-Commander filtering and the fork's other legality filters still apply before AI support is considered. Unsupported marked cards remain excluded. The global shared card-hint predicate defaults to strict behavior before model configuration and when the preference is disabled.

## Defects caught during integration

Independent review identified five defects in the imported upstream logic, despite its selected regression suite passing. Real-card failing regressions were added before accepting the corrections:

- Paradoxical Outcome's shortcut also selected Rescue and Disentomb and assigned illegal battlefield targets. Recognition is now limited to the intended spell and battlefield origin; target legality/count are checked. Shortcut candidates honor the existing failed-cast circuit breaker, preventing repeated rejected casts in the priority loop.
- Pact checks counted the same dual land against multiple colored obligations. Colored pips now receive distinct source assignments, with reassignment of flexible sources rather than independent per-color totals.
- Pact checks counted empty Pentad Prisms, tapped Mana Vaults and sources with unmet activation conditions as future mana. The forecast now uses free/tap, unrestricted, repeatable output with valid activation conditions; sources that cannot untap are excluded. Pending delayed obligations remain counted after the casting-turn history resets, with deduplication against that history and payment triggers already on the stack. A new read-only `TriggerHandler` list snapshot supports the forecast without adding saved state. Unreadable output cannot promise a payment that prevents losing the game. This remains a conservative forecast, not a full future resource planner.
- Oath timing inspected the exact first creature and its unrevealed position. The decision now uses remaining composition and library size, so rearranging the same hidden deck cannot change it. That heuristic does not guarantee which creature Oath will reveal or how many cards it will mill.
- The self-mill fallback accepted Dread Return when the sole Oracle was stranded in hand, or Reanimate in hand without mana left after the mill. It now requires a reachable winner and a payable follow-up: a winner cast from hand, an actual flashback line with sacrifice resources, or an affordable compatible reanimation spell. The payment forecast includes both the outlet and its follow-up; a sacrificed outlet is removed from the available fodder.

The previous batch's forced-target copy/Ivy protection and shared stack/reveal/zone/waterbending fixes remain unchanged. The fork's lazy loading, character previews, separate saves, themes, menus and private battle logs are preserved.

## Verification

Evidence is retained under `.build-cache/upstream-ai-fixes-20261107/`. The initial checks used the shared queue. That voluntary choice caused an avoidable wait: the user's queue rule applies to five other named projects, not Forge. Final Forge verification ran directly with the existing resource limits; no other project's job was stopped. The queued focused checks had already completed before an identity-guarded cancellation check, so no requests needed cancellation. Java source remains compatible with 17, built using the installed JDK 26. Desktop AI tests use disposable offline preferences/caches, headless mode, a 2 GiB heap and two reported processors; the ordinary client/editor build uses the established 1 GiB setting.

- Confirmed pre-import Pact baseline: three intended safety failures and one payable control, using the unchanged preceding assembly. The corrected fixture loads the actual type lists and asserts its basic lands' intrinsic colored mana; earlier incomplete fixture and command-quoting failures are retained separately.
- Adventure eligibility baseline: importing shared AI changes without the cache adaptation still fails supported-card admission. Focused policy/cache checks then pass with the adaptation, including preference toggling, immutable old snapshots, unchanged player pools and campaign restrictions.
- Initial selected upstream AI suite: 504 cases passed, zero failures/skips, across 33 selected classes including two bounded real-turn controls. Diagnostic-only printing methods are excluded from acceptance evidence. This result precedes the independently discovered corrections above.
- Review regression proof: six additional failures reproduced tapped Vaults, cumulative dual-source pips, empty Prisms, hidden Oath order, Rescue recognition and self-mill without a reachable winner. Expanded checks cover Disentomb separately and failed-cast retry behavior.
- Final corrected focused suite: 28/28 passed, including all 20 new policy cases and eight campaign-loading cases. The expanded uncorrected run produced 11 intended assertion failures with 17 passing controls. The separate lightweight policy-only comparison has 11 failures/nine controls before correction and 20/20 passes after correction.
- Final clean `mvn.cmd -B -ntp -pl forge-gui-mobile-dev,adventure-editor -am clean verify -Drevision=2.0.16-GITAiFixes`: all eight reactor modules succeeded, 338 reported cases, 337 passed, zero failures/errors and the existing Windows symbolic-link privilege skip (`ThemeResolverTest.symbolicLinkEscapeRejected`). Automatic discovery includes all 20 new GUI policy cases and the new Adventure cache case. A first clean run caught one unused import left by the Pact correction; that failure remains in `final/`, and the passing retry is recorded separately.
- Fresh desktop test compilation followed by the final selected AI suite: 504/504 passed, zero failures/skips, against the current compiled production sources. The two seeded ordinary-turn controls pass in both baseline and final code and assert that the requested heuristic/Flat Monte Carlo mode really runs.
- Authoring/content/package checks: 42/42 passed in the established Python/zlib environment with task-local Pillow. No tests or byte-exact map checks were relaxed.
- Final verification completed November 8. `final-verified/results.json` combines only inspected final results; `final-verified/reports/` and `desktop-testng-results.xml` archive the XML evidence. The complete build, desktop compilation and authoring logs are alongside them. `final-verified/source-audit.json` records hashes for all 84 changed Java files and seven changed resources; 80 files match the ordered source-patch mirror exactly. The eight manual Java adaptations and all five local plan/report/test additions are enumerated by the audit.
- Fresh production assembly SHA-256: `7ce5fc34792bfdfacd64f672a99d3d59498723a48790f919d8afe61faaeca86b`, for `forge-gui-mobile-dev/target/forge-gui-mobile-dev-2.0.16-GITAiFixes-jar-with-dependencies.jar`. Final verification commands and their successful exit are retained through `verify-complete.ps1` and the execution evidence. The earlier queued focused job is `1091938e2eba47ad92023b9679719ebc`; the initial passing AI suite is `9d1c4e0a72984a2680ed4c9eee470e8b`, not a substitute for the final rerun.

| Seeded ordinary-turn control | Baseline method duration | Final method duration |
| --- | ---: | ---: |
| Heuristic mode | 679 ms | 1,854 ms |
| Flat Monte Carlo mode | 721 ms | 77 ms |

These are single samples from TestNG method timing, including game setup but excluding one-time card database loading. They demonstrate bounded completion, not a statistically established speed change. Warm-up, garbage collection, suite order and other machine work can affect these numbers; the final run includes the larger AI suite.

The profiles carry different bounded search settings; these are intended behavior changes, not evidence of balanced campaign difficulty. A small seeded ordinary-turn comparison checks bounded completion in both heuristic and Flat Monte Carlo modes against the preceding assembly. It does not establish performance on mobile hardware, long matches, arbitrary decks or low-memory devices. No new graphical/native run or complete human playthrough is claimed by this batch.

The source audit compares non-adapted files directly with the selected-patch mirror and records tested source/resource hashes. It also checks that unrelated battle fixes and sideboarding preferences are unchanged. Only Cabal Therapy and Channel receive card-script edits; their existing card implementations are retained.

## Remaining work

New cards/sets and their data, captured-context background enemy generation, additional focused engine/trigger/undo work, and any separate decision on Two-Headed Giant/shared turns remain outstanding. Selective ports do not reduce an ancestry-based 548-commit behind count in the same way as a complete upstream merge.
