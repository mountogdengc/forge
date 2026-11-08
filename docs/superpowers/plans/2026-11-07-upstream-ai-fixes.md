# Upstream AI integration plan — third batch

> **Execution:** superpowers:executing-plans, continuing in the existing local integration checkout.

**Goal:** Adapt the reviewed Flat Monte Carlo AI improvements and their coherent data/engine dependencies for Adventure battles, from local base `4c01f0049f9b051dcc67f9bb5ca4ccc10066fb18`.

**Sources:** `7c34365c749`, `a6fe7c40c5e`, `af7df3488ea`, `45572700ec7`, `eb40bd24de1`, `a04b54df781`, `79f6fc2ec91`, `1747b2c7472`, `fcfdc73aeff`, `d3f975437b8`, `cfc0752f7a5`, `6d4a175d9ef`, `62ffaaedebc`, `d47f567004e`, `98d79231ccb`, plus only `AiCardLists` from `8360afa305b`, in their upstream chronological order. Reviewed upstream head remains `7acb3dbb61baac5ece0f609a9507804909cb9f97`.

**Scope:** Combo/role catalogs, named discard, sacrifice/Pact/timing/activation guards, Standard/Vintage combo lines, library choices, flash interaction, mana/color decisions, planeswalker/X choices, three existing AI profile budgets, paired existing-card hints and conditional Flat MC deck eligibility. Take the upstream regression coverage with the changes. Background fight generation, human sideboarding defaults, new cards/sets, Two-Headed Giant and unrelated engine cleanup remain separate.

**Preservation:** Existing fork campaigns, allowed/restricted editions/cards, non-Commander filtering, player reward pools, independent saves, character previews, lazy loading, themes and private battle logs. Existing forced-target copy guard remains. No master merge, push or publication. Use the existing branch and checkout; reusing the preceding batch's freshly verified baseline avoids another full unchanged build.

**Files:** Selected patches determine the production file set under `forge-ai`, their paired shared `forge-core`/`forge-game`/`forge-gui` dependencies, `forge-gui/res/ai`, `forge-gui/res/lists`, two existing card scripts, and upstream tests under `forge-gui-desktop/src/test/java/forge/ai`. Manually adapt `RewardData` and extend `CampaignCardLoadingTest` for the fork's immutable cached pools. Add lightweight `UpstreamAiPolicyShould` in the GUI module so core safety checks are automatically discovered by its `*Should.java` convention.

## Tasks and completion contracts

- [x] **1. Safety regressions and dependency preflight.** Add real inline-game checks for unpayable Pact refusal and positive payable controls using `ComboGuard.problem`. Observe the intended failures against the unchanged assembled baseline. Preflight ordered patches in an ignored source mirror; record conflicts/dependencies before adapting production code.
- [x] **2. Coherent upstream port.** Apply the selected patches, retaining the fork's previous adaptations. Resolve context conflicts narrowly, without importing whole-file unrelated changes. Run the safety regressions and the upstream focused AI tests against freshly compiled sources. Record failures individually; diagnostic-only methods and early-return tests are not acceptance evidence.
- [x] **3. Adventure eligibility/cache adaptation.** Extend the tiny real-catalog fixture: a marked AI card is excluded with Flat MC off, a supported role card is admitted with it on, an unsupported marked card stays excluded, and campaign restrictions still win. Toggling off/on must refresh enemy eligibility while old snapshots remain immutable and player pools unchanged. Add normalization/reload/anti-combo-only boundaries for the optimized role lookup. Observe failures before the adaptation, then pass.
- [x] **4. Verify and assess performance.** Use the shared queue in the actual checkout for a clean Adventure client/editor reactor and selected desktop AI regression suites, including real phase-loop scenarios. Isolate all desktop test preferences/caches in disposable directories, headless and offline. Run 42 authoring/content checks in the established task-local dependency environment. Compare selected representative deterministic scenarios on baseline and new code, recording elapsed time and bounded completion without claiming mobile-device timing or gameplay balance.
- [x] **5. Review and commit.** Obtain one fresh independent whole-batch review, correct important introduced defects with failing-before/passing-after coverage, verify final sources and automatic test discovery, record results/source hashes/known skips/limits, then create a local commit. Verify a clean checkout and unchanged master; leave GitHub untouched.

## Review focus

- Generated enemy eligibility must not bypass campaign legality, Commander restrictions or become stale after changing Flat MC preference.
- New global card-hint predicates must be safe before model initialization and preserve strict behavior with Flat MC disabled.
- Pact guards must distinguish future renewable colored mana from one-shot or unavailable sources.
- Named discard and predictive simulations must not expose hidden opponent information or mutate the live game/RNG state.
- Combo-specific mana, X, trigger timing and activation guards must retain ordinary heuristic AI behavior and bounded search.

**Evidence:** `.build-cache/upstream-ai-fixes-20261107/`; final integration report under `docs/Adventure/`. Failed runs remain separate from final passing evidence. The existing shared-battle and Adventure test suites remain part of final verification.

**Review-driven adaptations:** Narrow Paradoxical Outcome recognition and legal targets; apply failed-cast memory before deterministic shortcuts; forecast Pact payments using distinct renewable colored sources, actual activation conditions and pending delayed obligations across turn changes; make Oath timing independent of hidden order; require reachable and affordable post-self-mill wins, including reanimation costs. `TriggerHandler.getDelayedTriggers()` supplies a read-only list snapshot for the pending-obligation forecast without changing saved state. Real-card regressions cover each reproduced defect and payable/ordinary controls.

Final evidence: docs/Adventure/Upstream-AI-Fixes-2026-11-07.md and .build-cache/upstream-ai-fixes-20261107/final-verified/results.json. Clean reactor337passes/oneknownskip; desktop504passes; authoring42passes; focused28passes. Final review found no further actionable issues. Final capped Forge verification runs directly: the user queue rule applies only to the five named other projects.
